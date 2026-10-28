"""Synthetic live integration tests; no camera or projector required."""
import time
import unittest
from config import parse_args
from coordinate_mapper import CoordinateMapper
from gesture_detector import GestureDetector
from test_gesture_detector import hand
from java_connection import JavaConnection, calibrated_selection, scale_viewport, server_viewport


def wait_for(predicate):
    deadline = time.monotonic()+3
    while time.monotonic() < deadline:
        if predicate():
            return
        time.sleep(.01)
    raise AssertionError('Worker did not reach expected state')


class FakeTransport:
    def __init__(self):
        self.messages = []
        self.fail_select = False
        self.refuse = False
        self.connections = 0

    def __call__(self, port, timeout):
        self.connections += 1
        if self.refuse:
            raise ConnectionRefusedError('test server absent')
        return self

    def __enter__(self):
        return self

    def __exit__(self, *_):
        pass

    def request(self, message, on_sent=None):
        if message['type'] == 'INFO':
            return dict(status='info', executed=False, viewport_width=1280, viewport_height=720, fit='stretch')
        self.messages.append(message)
        if on_sent:
            on_sent()
        if self.fail_select:
            self.fail_select = False
            raise ConnectionResetError('lost ACK')
        return dict(status='executed', executed=True, id=message['id'], control='fixture')


class LiveTests(unittest.TestCase):
    def setUp(self):
        self.mapper = CoordinateMapper([(100,100),(500,100),(500,400),(100,400)], (640,480), (640,360))
        self.transport = FakeTransport()
        self.client = JavaConnection(factory=self.transport, retry_seconds=.02)
        self.addCleanup(self.client.close)

    def connect(self):
        self.client.start()
        wait_for(lambda: self.client.snapshot()['ready'])

    def test_optional_cli(self):
        self.assertFalse(parse_args([]).connect_java)
        args = parse_args(['--connect-java'])
        self.assertTrue(args.simulate and args.load_calibration)
        self.assertFalse(parse_args(['--connect-java','--calibrate']).load_calibration)

    def test_coordinate_endpoints_and_center(self):
        self.assertEqual(scale_viewport((639,359),(640,360),(1280,720)), (1279,719))
        self.assertEqual(scale_viewport((319.5,179.5),(640,360),(1280,720)), (639.5,359.5))
        with self.assertRaises(ValueError):
            scale_viewport((-1,0),(640,360),(1280,720))

    def test_incompatible_info(self):
        with self.assertRaises(ValueError):
            server_viewport(dict(status='info',fit='contain'), 'stretch')

    def test_missing_invalid_calibration_and_outside(self):
        self.connect()
        for mapper, raw, event in [(None,(300,200),(300,200)),(self.mapper,(99,200),(100,200)),
                                   (self.mapper,None,(300,200)),(self.mapper,(300,200),(600,200))]:
            self.assertIsNone(calibrated_selection(mapper,raw,event))
            self.assertFalse(self.client.submit(mapper,raw,event))
        self.assertEqual(self.transport.messages, [])
        with self.assertRaises(ValueError):
            CoordinateMapper([(0,0)]*4,(640,480),(1280,720))

    def test_correct_events_unique_ids_and_ack(self):
        self.connect()
        for _ in range(2):
            self.assertTrue(self.client.submit(self.mapper,(300,250),(300,250)))
            wait_for(lambda: not self.client.snapshot()['busy'])
        a,b = self.transport.messages
        self.assertEqual(a['type'],'SELECT')
        self.assertAlmostEqual(a['x'],639.5)
        self.assertAlmostEqual(a['y'],359.5)
        self.assertNotEqual(a['id'],b['id'])
        self.assertIn('executed=True',self.client.snapshot()['latest'])

    def test_missing_server_reconnect_no_stale_click(self):
        self.transport.refuse = True
        self.client.start()
        wait_for(lambda: self.transport.connections >= 1)
        self.assertFalse(self.client.submit(self.mapper,(300,200),(300,200)))
        self.transport.refuse = False
        wait_for(lambda: self.client.snapshot()['ready'])
        self.assertEqual(self.transport.messages,[])

    def test_lost_ack_never_replayed(self):
        self.connect()
        self.transport.fail_select = True
        self.client.submit(self.mapper,(300,200),(300,200))
        wait_for(lambda: self.client.snapshot()['epoch'] >= 2)
        self.assertEqual(len(self.transport.messages),1)
        self.assertIn('unknown',self.client.snapshot()['latest'])

    def test_expired_click_dropped(self):
        self.client._set(ready=True,size=(1280,720))
        self.client.submit(self.mapper,(300,200),(300,200))
        message, created, context = self.client.pending
        self.client.pending = (message, created-1, context)
        self.connect()
        wait_for(lambda: not self.client.snapshot()['busy'])
        self.assertEqual(self.transport.messages,[])

    def test_busy_and_context_cancel(self):
        # Queue deterministically before starting the worker.
        self.client._set(ready=True,size=(1280,720))
        self.assertTrue(self.client.submit(self.mapper,(300,200),(300,200)))
        self.assertFalse(self.client.submit(self.mapper,(300,200),(300,200)))
        self.client.invalidate()
        self.connect()
        self.assertEqual(self.transport.messages,[])

    def test_one_network_event_for_held_pinch(self):
        self.connect()
        detector = GestureDetector()
        for i in range(20):
            result = detector.update(hand(.8 if i < 2 else .1),640,480,i/10)
            if result.selection:
                self.client.submit(self.mapper,(300,200),result.selection)
        wait_for(lambda: not self.client.snapshot()['busy'])
        self.assertEqual(len(self.transport.messages),1)

    def test_live_main_loop_routes_once_and_closes(self):
        from unittest.mock import patch, MagicMock
        import numpy as np
        from main import run
        args = parse_args(['--connect-java','--no-mirror','--debounce-ms','0'])
        cv = MagicMock()
        cv.waitKey.side_effect = [-1,-1,ord('q')]
        cv.getWindowProperty.return_value = 1
        with (patch('main.HandTracker') as tracker, patch('main.Camera') as camera,
              patch('main.time.perf_counter',side_effect=[0,0,.1,.1,.2,.2]),
              patch('projection_preview.ProjectionPreview') as preview,
              patch('java_connection.JavaConnection') as client_type):
            client = client_type.return_value
            client.snapshot.return_value = dict(ready=True,epoch=1,size=(1280,720),status='Connected',latest='ACK')
            ui = preview.return_value
            ui.changed = False
            ui.session.mapper = self.mapper
            ui.render.side_effect = lambda frame,*rest: (frame,True)
            camera.return_value.__enter__.return_value.read.side_effect = [np.zeros((480,640,3),np.uint8) for _ in range(3)]
            tracker.return_value.__enter__.return_value.detect.side_effect = [hand(r) for r in (.8,.1,.1)]
            run(args,cv,np)
            client.submit.assert_called_once_with(self.mapper,(300.,200.),(300,200))
            client.__exit__.assert_called_once()
