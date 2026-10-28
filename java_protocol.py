"""Shared Phase 4B NDJSON transport. No camera imports or automatic click retries."""
import json
import socket


class JsonConnection:
    def __init__(self, port=5050, timeout=1.0):
        self.socket = socket.create_connection(('127.0.0.1', port), timeout=timeout)
        self.socket.settimeout(timeout)
        self.reader = self.socket.makefile('rb')

    def request(self, message, on_sent=None):
        wire = (json.dumps(message, allow_nan=False) + '\n').encode('utf-8')
        self.socket.sendall(wire)
        if on_sent:
            on_sent()
        line = self.reader.readline(65537)
        if not line or not line.endswith(b'\n') or len(line) > 65536:
            raise ValueError('Missing, incomplete, or oversized acknowledgement; execution outcome is unknown')
        reply = json.loads(line)
        if not isinstance(reply, dict) or not isinstance(reply.get('executed'), bool) or not isinstance(reply.get('status'), str):
            raise ValueError('Invalid acknowledgement; execution outcome is unknown')
        if message.get('id') and reply.get('id') != message['id'] and reply.get('status') != 'rejected':
            raise ValueError('Acknowledgement ID mismatch; execution outcome is unknown')
        return reply

    def close(self):
        self.reader.close()
        self.socket.close()

    def __enter__(self):
        return self

    def __exit__(self, *_):
        self.close()
