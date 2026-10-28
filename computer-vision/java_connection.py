"""Nonblocking live client for the SAME Phase 4B server and protocol."""
import math
from pathlib import Path
import sys
import threading
import time
import uuid

# Both the standalone test client and this module use the shared root transport.
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from java_protocol import JsonConnection


def server_viewport(info, expected_fit):
    if info.get('status') != 'info' or info.get('fit') != expected_fit:
        raise ValueError(f'Java must return INFO with fit={expected_fit}; check --viewport-fit')
    size = (info.get('viewport_width'), info.get('viewport_height'))
    if any(type(v) is not int or not 2 <= v <= 16384 for v in size):
        raise ValueError('Invalid Java viewport dimensions')
    return size


def scale_viewport(point, source_size, target_size):
    if any(type(v) is not int or v < 2 for v in (*source_size, *target_size)):
        raise ValueError('Invalid viewport dimensions')
    if len(point) != 2 or not all(math.isfinite(v) and 0 <= v <= n-1 for v, n in zip(point, source_size)):
        raise ValueError('Point outside calibrated viewport')
    return tuple(v*(target-1)/(source-1) for v, source, target in zip(point, source_size, target_size))


def calibrated_selection(mapper, raw_point, event_point):
    """Never turn missing calibration or an edge-clamped outside finger into a click."""
    if mapper is None or raw_point is None or event_point is None:
        return None
    if mapper.map_point(raw_point) is None:
        return None
    return mapper.map_point(event_point)


class JavaConnection:
    def __init__(self, port=5050, timeout=1.0, fit='stretch', retry_seconds=1.0,
                 factory=JsonConnection, max_age=0.25):
        self.port, self.timeout, self.fit = port, timeout, fit
        self.factory, self.retry_seconds, self.max_age = factory, retry_seconds, max_age
        self.lock = threading.Lock()
        self.wake = threading.Event()
        self.stop = threading.Event()
        self.pending = None
        self.context = 0
        self.state = dict(ready=False, busy=False, epoch=0, size=None,
                          status='Connecting', latest='No SELECT sent')
        self.thread = None

    def start(self):
        self.thread = threading.Thread(target=self._worker, name='java-control', daemon=True)
        self.thread.start()
        return self

    def snapshot(self):
        with self.lock:
            return dict(self.state)

    def invalidate(self):
        """Cancel any not-yet-sent click after calibration/camera state changes."""
        with self.lock:
            self.context += 1
            if self.pending is not None:
                self.pending = None
                self.state.update(busy=False, latest='SELECT canceled: calibration/tracking changed')

    def submit(self, mapper, raw_point, event_point):
        mapped = calibrated_selection(mapper, raw_point, event_point)
        if mapped is None:
            with self.lock:
                self.state['latest'] = 'Not sent: missing/invalid calibration or outside region'
            return False
        with self.lock:
            if not self.state['ready'] or self.state['busy']:
                self.state['latest'] = 'Not sent: Java unavailable or previous request busy; release and pinch again'
                return False
            coords = scale_viewport(mapped, mapper.viewport_size, self.state['size'])
            message = dict(type='SELECT', x=coords[0], y=coords[1], id=str(uuid.uuid4()))
            self.pending = (message, time.monotonic(), self.context)
            self.state.update(busy=True, latest=f"SELECT {message['id'][:8]} pending (not sent)")
        self.wake.set()
        return True

    def _set(self, **values):
        with self.lock:
            self.state.update(values)

    def _worker(self):
        while not self.stop.is_set():
            try:
                with self.factory(self.port, self.timeout) as connection:
                    size = server_viewport(connection.request({'type': 'INFO'}), self.fit)
                    with self.lock:
                        self.state.update(ready=True, size=size, status=f'Connected: {size[0]}x{size[1]} {self.fit}',
                                          epoch=self.state['epoch']+1)
                    next_info = time.monotonic()+1
                    while not self.stop.is_set():
                        self.wake.wait(0.05)
                        self.wake.clear()
                        with self.lock:
                            pending, self.pending = self.pending, None
                        if pending:
                            message, created, context = pending
                            with self.lock:
                                valid = not self.stop.is_set() and context == self.context and time.monotonic()-created <= self.max_age
                            if not valid:
                                self._set(busy=False, latest='SELECT dropped before send: expired or context changed')
                                continue
                            # Never retry this message, even if its ACK is lost.
                            self._set(latest=f"SELECT {message['id'][:8]} sending; outcome pending")
                            reply = connection.request(message, on_sent=lambda: self._set(
                                latest=f"SELECT {message['id'][:8]} sent; awaiting ACK"))
                            print('Java ACK:', reply, flush=True)
                            self._set(busy=False, latest=f"ACK {reply['status']}; executed={reply['executed']} "
                                      + str(reply.get('control', reply.get('error', ''))))
                        elif time.monotonic() >= next_info:
                            refreshed = server_viewport(connection.request({'type': 'INFO'}), self.fit)
                            if refreshed != size:
                                raise ValueError('Java viewport changed; reconnecting')
                            next_info = time.monotonic()+1
            except (OSError, ValueError, TypeError) as error:
                with self.lock:
                    was_busy = self.state['busy']
                    self.pending = None
                    self.state.update(ready=False, busy=False, size=None, status=f'Disconnected: {error}')
                    if was_busy:
                        self.state['latest'] = 'SELECT not retried; execution unknown if transmission began'
                print('Java connection:', error, flush=True)
            if not self.stop.is_set():
                self.stop.wait(self.retry_seconds)
        self._set(ready=False, status='Closed')

    def close(self):
        self.invalidate()
        self.stop.set()
        self.wake.set()
        if self.thread:
            self.thread.join(timeout=self.timeout+0.5)

    def __enter__(self):
        return self.start()

    def __exit__(self, *_):
        self.close()
