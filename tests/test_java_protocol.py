"""Runs against a real Java TCP server. Requires precompiled ControlTestHarness.
Set JAVA17_EXE and JAVA_TEST_CLASSPATH, then run unittest discover -s tests.
"""
import contextlib
import io
import json
import os
import queue
import socket
import subprocess
import threading
import unittest
import uuid
from concurrent.futures import ThreadPoolExecutor
from unittest.mock import patch

from test_java_connection import main, send_request


class JavaProtocolTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.java = os.environ.get('JAVA17_EXE', r'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe')
        cls.classpath = os.environ.get('JAVA_TEST_CLASSPATH', 'build-phase4b-tests')
        cls.process = subprocess.Popen([cls.java, '-Djava.awt.headless=true', '-cp', cls.classpath,
                                        'groupDGuessWho.ControlTestHarness'], stdout=subprocess.PIPE,
                                       stderr=subprocess.STDOUT, text=True)
        cls.lines = queue.Queue()
        def drain():
            for line in cls.process.stdout:
                cls.lines.put(line)
        cls.thread = threading.Thread(target=drain, daemon=True)
        cls.thread.start()
        try:
            line = cls.lines.get(timeout=10)
            cls.port = int(line.split('127.0.0.1:')[1].split()[0])
        except Exception:
            cls.process.terminate()
            cls.process.wait(timeout=5)
            raise RuntimeError('Compile the Java test harness first; server startup failed')

    @classmethod
    def tearDownClass(cls):
        cls.process.terminate()
        cls.process.wait(timeout=5)
        cls.thread.join(timeout=2)
        cls.process.stdout.close()

    def request(self, message):
        with contextlib.redirect_stdout(io.StringIO()):
            return send_request(message, self.port)

    def raw(self, wire):
        with socket.create_connection(('127.0.0.1', self.port), timeout=5) as sock:
            sock.sendall(wire)
            with sock.makefile('rb') as reader:
                return json.loads(reader.readline())

    def test_successful_python_java_delivery_and_execution(self):
        reply = self.request({'type':'SELECT','x':620,'y':340,'id':str(uuid.uuid4())})
        self.assertTrue(reply['received'])
        self.assertTrue(reply['executed'])

    def test_live_background_client_uses_java_server(self):
        import sys
        import time
        from pathlib import Path
        from types import SimpleNamespace
        sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'computer-vision'))
        from java_connection import JavaConnection
        mapper = SimpleNamespace(viewport_size=(640,360), map_point=lambda p: p)
        with JavaConnection(self.port) as client:
            deadline = time.monotonic()+5
            while not client.snapshot()['ready'] and time.monotonic() < deadline:
                time.sleep(.01)
            self.assertTrue(client.snapshot()['ready'])
            self.assertTrue(client.submit(mapper,(310,170),(310,170)))
            while client.snapshot()['busy'] and time.monotonic() < deadline:
                time.sleep(.01)
            self.assertIn('ACK executed; executed=True',client.snapshot()['latest'])

    def test_malformed_json(self):
        for wire in [b'{bad}\n', b'{}{}\n', b'{"type":"SELECT","x":1,"x":2,"y":3}\n']:
            self.assertEqual(self.raw(wire)['status'],'rejected')

    def test_missing_coordinates(self):
        self.assertFalse(self.request({'type':'SELECT','x':3})['executed'])

    def test_invalid_coordinates_and_types(self):
        for x in [-1,1280,'620',True,None]:
            self.assertEqual(self.request({'type':'SELECT','x':x,'y':0})['status'],'rejected')
        for y in [-1,720]:
            self.assertEqual(self.request({'type':'SELECT','x':0,'y':y})['status'],'rejected')

    def test_duplicate_id_across_reconnections(self):
        event={'type':'SELECT','x':2,'y':3,'id':str(uuid.uuid4())}
        first=self.request(event)
        second=self.request(event)
        self.assertTrue(first['executed'])
        self.assertEqual(second['status'],'duplicate')
        self.assertFalse(second['executed'])
        self.assertEqual(self.request({'type':'INFO'})['count'],first['count'])

    def test_no_id_events_are_independent(self):
        a=self.request({'type':'SELECT','x':2,'y':3})
        b=self.request({'type':'SELECT','x':2,'y':3})
        self.assertEqual(b['count'],a['count']+1)

    def test_fragmented_and_multiple_messages(self):
        with socket.create_connection(('127.0.0.1',self.port),timeout=5) as sock:
            sock.sendall(b'{"ty')
            sock.sendall(b'pe":"INFO"}\n{"type":"INFO"}\n')
            with sock.makefile('rb') as reader:
                self.assertEqual(json.loads(reader.readline())['status'],'info')
                self.assertEqual(json.loads(reader.readline())['status'],'info')

    def test_partial_disconnect_does_not_execute(self):
        before=self.request({'type':'INFO'})['count']
        with socket.create_connection(('127.0.0.1',self.port),timeout=5) as sock:
            sock.sendall(b'{"type":"SELECT","x":1,"y":2}')
            sock.shutdown(socket.SHUT_WR)
            self.assertEqual(sock.recv(1),b'')
        self.assertEqual(self.request({'type':'INFO'})['count'],before)

    def test_oversized_message_disconnect_and_reconnect(self):
        with socket.create_connection(('127.0.0.1',self.port),timeout=5) as sock:
            sock.sendall(b' '*8193)
            self.assertEqual(sock.recv(1),b'')
        self.assertEqual(self.request({'type':'INFO'})['status'],'info')

    def test_unknown_fields_and_bad_id(self):
        for request in [{'type':'OTHER'}, {'type':'INFO','x':2},
                        {'type':'SELECT','x':1,'y':2,'id':'bad\nid'},
                        {'type':'SELECT','x':1,'y':2,'extra':1}]:
            self.assertEqual(self.request(request)['status'],'rejected')

    def test_java_mapping_and_json_assertions(self):
        result=subprocess.run([self.java,'-cp',self.classpath,'groupDGuessWho.ControlTestHarness','--self-test'],capture_output=True,text=True,timeout=10)
        self.assertEqual(result.returncode,0,result.stdout+result.stderr)

    def test_real_swing_hit_testing_and_edt(self):
        result=subprocess.run([self.java,'-cp',self.classpath,'groupDGuessWho.ControlTestHarness','--swing-test'],capture_output=True,text=True,timeout=15)
        self.assertEqual(result.returncode,0,result.stdout+result.stderr)

    def test_simultaneous_duplicate_ids(self):
        wire=(json.dumps({'type':'SELECT','x':2,'y':3,'id':str(uuid.uuid4())})+'\n').encode()
        with ThreadPoolExecutor(max_workers=2) as pool:
            results=list(pool.map(lambda _: self.raw(wire),range(2)))
        self.assertEqual(sorted(r['status'] for r in results),['duplicate','executed'])

    def test_real_connection_refusal(self):
        # Hold a local port without listening so no other process can take it.
        with socket.socket() as unused:
            unused.bind(('127.0.0.1',0))
            with contextlib.redirect_stdout(io.StringIO()), self.assertRaises(OSError):
                send_request({'type':'INFO'}, unused.getsockname()[1], timeout=1)

    def test_client_connection_refusal_is_readable(self):
        with patch('test_java_connection.socket.create_connection',side_effect=ConnectionRefusedError('refused')):
            with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()) as output:
                self.assertEqual(main(['--x','1','--y','2']),1)
        self.assertIn('Check Java is running',output.getvalue())


if __name__ == '__main__':
    unittest.main()
