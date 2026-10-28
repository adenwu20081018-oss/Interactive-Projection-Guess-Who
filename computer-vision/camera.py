"""Small camera wrapper with bounded recovery from missing frames."""


class CameraError(RuntimeError):
    pass


class Camera:
    def __init__(self, index=0, width=640, height=480, cv=None, max_failures=30):
        if cv is None:
            import cv2 as cv
        self.cv = cv
        self.capture = None
        self.failures = 0
        self.max_failures = max_failures
        try:
            self.capture = cv.VideoCapture(index)
            if not self.capture.isOpened():
                raise CameraError(f"Cannot open camera {index}. Check permissions, camera index, and other camera apps.")
            self.capture.set(cv.CAP_PROP_FRAME_WIDTH, width)
            self.capture.set(cv.CAP_PROP_FRAME_HEIGHT, height)
        except Exception as error:
            self.close()
            raise CameraError(f"Camera initialization failed: {error}") from error

    def read(self):
        try:
            ok, frame = self.capture.read()
        except self.cv.error:
            ok, frame = False, None
        if not ok or frame is None or frame.size == 0:
            self.failures += 1
            if self.failures >= self.max_failures:
                raise CameraError("Camera stopped delivering frames. Reconnect it and restart the program.")
            return None
        self.failures = 0
        return frame

    def close(self):
        if self.capture is not None:
            self.capture.release()
            self.capture = None

    def __enter__(self):
        return self

    def __exit__(self, *_):
        self.close()
