"""Pure pinch state machine: no camera, drawing, MediaPipe, or console access."""
from dataclasses import dataclass
import math


@dataclass(frozen=True)
class GestureResult:
    status: str
    ratio: float | None
    armed: bool
    selection: tuple[int, int] | None = None


def pinch_ratio(landmarks, width, height):
    """Tip distance / wrist-to-middle-knuckle distance, in image-plane pixels.

    Scale x and y by actual image dimensions before measuring: normalized
    MediaPipe x/y axes do not have the same pixel scale on a rectangular image.
    Use floating-point coordinates, not rounded/clamped drawing positions.
    """
    if not landmarks or len(landmarks) != 21 or width <= 0 or height <= 0:
        return None
    points = []
    for index in (0, 9, 4, 8):
        point = landmarks[index]
        if not (math.isfinite(point.x) and math.isfinite(point.y)
                and 0 <= point.x <= 1 and 0 <= point.y <= 1):
            return None
        points.append((point.x * width, point.y * height))
    palm = math.dist(points[0], points[1])
    if palm < 1.0:
        return None
    return math.dist(points[2], points[3]) / palm


class GestureDetector:
    def __init__(self, pinch_threshold=0.30, release_threshold=0.45,
                 debounce_ms=100, cooldown_ms=400):
        values = (pinch_threshold, release_threshold, debounce_ms, cooldown_ms)
        if not all(math.isfinite(v) for v in values):
            raise ValueError("Gesture settings must be finite")
        if not 0 < pinch_threshold < release_threshold:
            raise ValueError("Require 0 < pinch threshold < release threshold")
        if debounce_ms < 0 or cooldown_ms < 0:
            raise ValueError("Gesture timing must be nonnegative")
        self.pinch_threshold = pinch_threshold
        self.release_threshold = release_threshold
        self.debounce = debounce_ms / 1000
        self.cooldown = cooldown_ms / 1000
        self.status = "READY"
        # Require a deliberate open hand at startup and after tracking loss.
        self.armed = False
        self.candidate = None
        self.candidate_since = None
        self.last_selection = -math.inf
        self.last_update = None

    def _cancel_tracking(self):
        self.armed = False
        self.candidate = None
        self.candidate_since = None

    def update(self, landmarks, width, height, now):
        """Return one selection only at the debounced pinch onset.

        now is monotonic seconds. Missing data cancels pending transitions but
        preserves the pinch latch. Reacquisition must show a stable release.
        Gaps over 250 ms also count as lost continuity (e.g. a stalled camera).
        """
        if not math.isfinite(now) or (self.last_update is not None and now < self.last_update):
            raise ValueError("Gesture timestamps must be finite and nondecreasing")
        if self.last_update is not None and now - self.last_update > 0.25:
            self._cancel_tracking()
        self.last_update = now
        ratio = pinch_ratio(landmarks, width, height)
        if ratio is None:
            self._cancel_tracking()
            return GestureResult(self.status, None, False)

        target = None
        if ratio >= self.release_threshold:
            if self.status == "PINCHED" or not self.armed:
                target = "READY"
        elif ratio <= self.pinch_threshold and self.armed and self.status == "READY":
            target = "PINCHED"
        # In the hysteresis band retain the stable state, cancel pending debounce.
        if target is None:
            self.candidate = None
            self.candidate_since = None
        elif target != self.candidate:
            self.candidate = target
            self.candidate_since = now

        selection = None
        if target is not None and now - self.candidate_since + 1e-9 >= self.debounce:
            self.status = target
            self.candidate = None
            self.candidate_since = None
            if target == "READY":
                self.armed = True
            else:
                # A pinch during cooldown is consumed, not queued for later.
                # Holding it beyond cooldown must never produce a delayed click.
                if now - self.last_selection + 1e-9 >= self.cooldown:
                    tip = landmarks[8]
                    selection = (min(width - 1, int(tip.x * width)),
                                 min(height - 1, int(tip.y * height)))
                    self.last_selection = now
        return GestureResult(self.status, ratio, self.armed, selection)
