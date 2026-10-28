"""MediaPipe Tasks adapter and camera-independent coordinate helpers."""
import math
import zipfile
from pathlib import Path
from urllib.request import urlopen

from config import MODEL_URL

# Wrist, thumb, four fingers, and palm connections (21 landmark indices).
CONNECTIONS = (
    (0, 1), (1, 2), (2, 3), (3, 4), (0, 5),
    (5, 6), (6, 7), (7, 8), (5, 9), (9, 10), (10, 11),
    (11, 12), (9, 13), (13, 14), (14, 15), (15, 16),
    (13, 17), (0, 17), (17, 18), (18, 19), (19, 20),
)


def to_pixel(x, y, width, height):
    """Convert normalized coordinates to pixels, clamped to the visible image."""
    if width <= 0 or height <= 0 or not (math.isfinite(x) and math.isfinite(y)):
        raise ValueError("Coordinates must be finite and image dimensions positive")
    return (min(width - 1, max(0, int(x * width))),
            min(height - 1, max(0, int(y * height))))


def landmark_pixels(landmarks, width, height):
    # An empty result is ordinary tracking loss, not an exception.
    if not landmarks or len(landmarks) != 21:
        return []
    try:
        return [to_pixel(point.x, point.y, width, height) for point in landmarks]
    except (ValueError, OverflowError):
        return []


class FrameClock:
    def __init__(self):
        self.last_timestamp = -1
        self.previous_time = None
        self.fps = 0.0

    def timestamp_ms(self, now):
        # Tasks VIDEO mode requires strictly increasing timestamps.
        self.last_timestamp = max(self.last_timestamp + 1, int(now * 1000))
        return self.last_timestamp

    def update_fps(self, now):
        if self.previous_time is not None and now > self.previous_time:
            current = 1.0 / (now - self.previous_time)
            self.fps = current if self.fps == 0 else 0.9 * self.fps + 0.1 * current
        self.previous_time = now
        return self.fps


def validate_model(path):
    path = Path(path)
    if not path.is_file():
        raise ValueError(f"Missing model: {path}. Run: python main.py --download-model")
    try:
        with zipfile.ZipFile(path) as bundle:
            required = {"hand_detector.tflite", "hand_landmarks_detector.tflite"}
            if not required.issubset(bundle.namelist()) or bundle.testzip() is not None:
                raise ValueError("Incomplete hand model bundle")
    except (zipfile.BadZipFile, OSError) as error:
        raise ValueError(f"Invalid model: {path}. Download it again.") from error
    return path


def download_model(path):
    """Explicit download only. An interrupted download never replaces a model."""
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(".download")
    try:
        with urlopen(MODEL_URL, timeout=60) as response, temporary.open("wb") as output:
            while chunk := response.read(1024 * 1024):
                output.write(chunk)
        validate_model(temporary)
        temporary.replace(path)
    finally:
        temporary.unlink(missing_ok=True)


class HandTracker:
    def __init__(self, model_path):
        validate_model(model_path)
        import mediapipe as mp
        from mediapipe.tasks import python
        from mediapipe.tasks.python import vision
        self.mp = mp
        options = vision.HandLandmarkerOptions(
            base_options=python.BaseOptions(model_asset_path=str(model_path)),
            running_mode=vision.RunningMode.VIDEO,
            num_hands=1,
            min_hand_detection_confidence=0.5,
            min_hand_presence_confidence=0.5,
            min_tracking_confidence=0.5,
        )
        self.detector = vision.HandLandmarker.create_from_options(options)

    def detect(self, bgr_frame, timestamp_ms):
        import cv2
        import numpy as np
        rgb = np.ascontiguousarray(cv2.cvtColor(bgr_frame, cv2.COLOR_BGR2RGB))
        image = self.mp.Image(image_format=self.mp.ImageFormat.SRGB, data=rgb)
        result = self.detector.detect_for_video(image, timestamp_ms)
        return result.hand_landmarks[0] if result.hand_landmarks else []

    def __enter__(self):
        return self

    def __exit__(self, *_):
        self.detector.close()
