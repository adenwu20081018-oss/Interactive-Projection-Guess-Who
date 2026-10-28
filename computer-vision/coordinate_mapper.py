"""Map one flat camera-image quadrilateral into a virtual pixel viewport."""
import cv2
import numpy as np


def dimensions(value, name):
    if not isinstance(value, (list, tuple)) or len(value) != 2:
        raise ValueError(f"{name} must contain width and height")
    if any(type(v) is not int or not 2 <= v <= 16384 for v in value):
        raise ValueError(f"{name} dimensions must be integers between 2 and 16384")
    return tuple(value)


class CoordinateMapper:
    def __init__(self, points, camera_size, viewport_size):
        self.camera_size = dimensions(camera_size, "Camera")
        self.viewport_size = dimensions(viewport_size, "Viewport")
        try:
            source = np.asarray(points, dtype=np.float64)
        except (TypeError, ValueError) as error:
            raise ValueError("Corners must be four numeric x/y pairs") from error
        if source.shape != (4, 2) or not np.isfinite(source).all():
            raise ValueError("Select four finite corners: TL, TR, BR, BL")
        w, h = self.camera_size
        if np.any(source < 0) or np.any(source[:, 0] > w-1) or np.any(source[:, 1] > h-1):
            raise ValueError("Calibration corners must lie inside the camera image")
        edges = np.roll(source, -1, axis=0) - source
        crosses = np.cross(edges, np.roll(edges, -1, axis=0))
        # Positive cross products are clockwise in image coordinates (y downward).
        if np.any(crosses <= 1e-6) or np.any(np.linalg.norm(edges, axis=1) < 5):
            raise ValueError("Corners must form a convex clockwise quadrilateral, without crossing or duplicate points")
        self.source = source.astype(np.float32)
        if cv2.contourArea(self.source) < 100:
            raise ValueError("Interaction area is too small; select a larger rectangle")
        # Catch a rotated click sequence instead of silently rotating the viewport.
        if not (source[0, 0] < source[1, 0] and source[3, 0] < source[2, 0]
                and source[0, 1] < source[3, 1] and source[1, 1] < source[2, 1]):
            raise ValueError("Click the displayed top-left, top-right, bottom-right, bottom-left in order")
        tw, th = self.viewport_size
        target = np.float32([[0, 0], [tw-1, 0], [tw-1, th-1], [0, th-1]])
        self.matrix = cv2.getPerspectiveTransform(self.source, target)
        if not np.isfinite(self.matrix).all() or np.linalg.matrix_rank(self.matrix) != 3:
            raise ValueError("Calibration cannot produce a stable perspective matrix")
        mapped = cv2.perspectiveTransform(self.source[None, :, :], self.matrix)[0]
        if not np.allclose(mapped, target, atol=0.01):
            raise ValueError("Calibration corner mapping is unstable; choose a clearer rectangle")

    def contains(self, point):
        p = np.asarray(point, dtype=float)
        return (p.shape == (2,) and np.isfinite(p).all()
                and cv2.pointPolygonTest(self.source, tuple(map(float, p)), False) >= 0)

    def map_point(self, point):
        """Return viewport float coordinates, or None for outside/invalid points.

        Never project an outside point and silently clamp it to an edge.
        """
        if not self.contains(point):
            return None
        p = np.asarray(point, dtype=np.float64)
        if abs(self.matrix[2] @ np.array([p[0], p[1], 1])) < 1e-10:
            return None
        result = cv2.perspectiveTransform(p.reshape(1, 1, 2), self.matrix)[0, 0]
        w, h = self.viewport_size
        if not np.isfinite(result).all() or not (-0.01 <= result[0] <= w-1+0.01 and -0.01 <= result[1] <= h-1+0.01):
            return None
        # Only remove floating-point roundoff for already-inside points.
        return float(np.clip(result[0], 0, w-1)), float(np.clip(result[1], 0, h-1))

    def selection_message(self, camera_point):
        mapped = self.map_point(camera_point)
        if mapped is None:
            return None
        # Round half-pixels consistently despite tiny homography roundoff.
        x, y = (int(np.floor(v + 0.5 + 1e-7)) for v in mapped)
        return f"SELECT screen_x={x} screen_y={y}"
