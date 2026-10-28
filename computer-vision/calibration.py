"""Calibration persistence and four-click state, independent of hand tracking."""
import json
from datetime import datetime, timezone
from pathlib import Path

import numpy as np

from coordinate_mapper import CoordinateMapper

ORDER = ["top-left", "top-right", "bottom-right", "bottom-left"]
CONVENTION = "displayed-camera-pixels"


def save_calibration(path, mapper, mirrored, camera_index):
    data = {
        "schema_version": 1,
        "created_utc": datetime.now(timezone.utc).isoformat(),
        "coordinate_convention": CONVENTION,
        "corner_order": ORDER,
        "camera_index": camera_index,
        "camera_resolution": list(mapper.camera_size),
        "mirrored": mirrored,
        "source_points": mapper.source.tolist(),
        "viewport_dimensions": list(mapper.viewport_size),
        "transformation_matrix": mapper.matrix.tolist(),
        "method": "opencv-four-point-homography",
    }
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    try:
        temporary.write_text(json.dumps(data, indent=2, allow_nan=False), encoding="utf-8")
        temporary.replace(path)
    finally:
        temporary.unlink(missing_ok=True)


def load_calibration(path, camera_size, mirrored, viewport_size, camera_index):
    try:
        data = json.loads(Path(path).read_text(encoding="utf-8"))
        if not isinstance(data, dict) or type(data.get("schema_version")) is not int or data["schema_version"] != 1:
            raise ValueError("Unsupported calibration schema; recalibrate")
        if data.get("coordinate_convention") != CONVENTION or data.get("corner_order") != ORDER:
            raise ValueError("Unsupported coordinate convention or corner order")
        if type(data.get("mirrored")) is not bool or data["mirrored"] != mirrored:
            raise ValueError("Mirror setting mismatch; use the original mirror setting or recalibrate")
        if type(data.get("camera_index")) is not int or data["camera_index"] != camera_index:
            raise ValueError("Camera index mismatch; select the original camera or recalibrate")
        mapper = CoordinateMapper(data["source_points"], data["camera_resolution"], data["viewport_dimensions"])
        if mapper.camera_size != tuple(camera_size):
            raise ValueError(f"Camera resolution mismatch: saved {mapper.camera_size}, actual {camera_size}; recalibrate")
        if mapper.viewport_size != tuple(viewport_size):
            raise ValueError("Viewport resolution mismatch; use the saved viewport dimensions or recalibrate")
        matrix = np.asarray(data["transformation_matrix"], dtype=float)
        if (matrix.shape != (3, 3) or not np.isfinite(matrix).all()
                or not np.allclose(matrix, mapper.matrix, rtol=1e-7, atol=1e-7)):
            raise ValueError("Saved matrix does not match its corners; recalibrate")
        return mapper
    except (OSError, ValueError, TypeError, KeyError, OverflowError) as error:
        raise ValueError(f"Cannot load calibration: {error}") from error


class CalibrationSession:
    def __init__(self, camera_size, viewport_size, mirrored, camera_index, path):
        self.camera_size = tuple(camera_size)
        self.viewport_size = tuple(viewport_size)
        self.mirrored = mirrored
        self.camera_index = camera_index
        self.path = Path(path)
        self.points = []
        self.mapper = None
        self.message = "Click top-left"

    def reset(self):
        self.points = []
        self.mapper = None
        self.message = "Calibration reset (saved file unchanged). Click top-left"

    def undo(self):
        self.mapper = None
        if self.points:
            self.points.pop()
        self.message = f"Click {ORDER[len(self.points)]}"

    def click(self, x, y):
        if len(self.points) == 4:
            return
        w, h = self.camera_size
        if not (0 <= x < w and 0 <= y < h):
            self.message = "Click inside the camera image"
            return
        self.points.append((x, y))
        if len(self.points) < 4:
            self.message = f"Click {ORDER[len(self.points)]}"
            return
        try:
            self.mapper = CoordinateMapper(self.points, self.camera_size, self.viewport_size)
            self.message = "Calibrated (unsaved). S: save | C: recalibrate"
        except ValueError as error:
            self.points.pop()
            self.message = f"Invalid: {error}. U: undo | R: reset"

    def key(self, key):
        if key in (ord('u'), ord('U'), 8):
            self.undo()
        elif key in (ord('r'), ord('R'), ord('c'), ord('C')):
            self.reset()
        elif key in (ord('s'), ord('S')):
            if self.mapper is None:
                self.message = "Select four valid corners before saving"
            else:
                try:
                    save_calibration(self.path, self.mapper, self.mirrored, self.camera_index)
                    self.message = "Calibration saved"
                except OSError as error:
                    self.message = f"Save failed: {error}"
        elif key in (ord('l'), ord('L')):
            # A failed load must not leave an old mapping active by accident.
            self.reset()
            try:
                self.mapper = load_calibration(self.path, self.camera_size, self.mirrored,
                                               self.viewport_size, self.camera_index)
                self.points = self.mapper.source.tolist()
                self.message = "Calibration loaded. Recalibrate if camera/target moved"
            except ValueError as error:
                self.message = str(error)
