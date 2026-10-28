"""Command-line settings; paths work even when launched from another folder."""
import argparse
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parent
DEFAULT_MODEL = ROOT / "models" / "hand_landmarker.task"
MODEL_URL = "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task"


def positive_int(value):
    number = int(value)
    if number <= 0:
        raise argparse.ArgumentTypeError("must be greater than zero")
    return number


def camera_index(value):
    number = int(value)
    if number < 0:
        raise argparse.ArgumentTypeError("camera index must be zero or greater")
    return number


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description="Hand tracking, pinch selection, and optional Phase 3 calibration")
    parser.add_argument("--camera", type=camera_index, default=0)
    parser.add_argument("--width", type=positive_int, default=640)
    parser.add_argument("--height", type=positive_int, default=480)
    parser.add_argument("--model", type=Path, default=DEFAULT_MODEL)
    parser.add_argument("--no-mirror", action="store_true", help="Disable selfie-style mirroring")
    parser.add_argument("--download-model", action="store_true", help="Download model and exit; no camera access")
    parser.add_argument("--simulate", action="store_true", help="Enable camera calibration and simulated viewport")
    parser.add_argument("--calibrate", action="store_true", help="Start a fresh four-corner calibration (implies --simulate)")
    parser.add_argument("--load-calibration", action="store_true", help="Load saved calibration on first frame (implies --simulate)")
    parser.add_argument("--calibration-file", type=Path, default=ROOT / "calibration_config.json")
    parser.add_argument("--viewport-width", type=positive_int, default=1280)
    parser.add_argument("--viewport-height", type=positive_int, default=720)
    parser.add_argument("--pinch-threshold", type=float, default=0.30)
    parser.add_argument("--release-threshold", type=float, default=0.45)
    parser.add_argument("--debounce-ms", type=float, default=100,
                        help="Stable duration required for pinch AND release (default 100)")
    parser.add_argument("--cooldown-ms", type=float, default=400,
                        help="Minimum time between selections (default 400)")
    parser.add_argument('--connect-java', action='store_true', help='Enable calibrated live Java control')
    parser.add_argument('--java-port', type=int, default=5050)
    parser.add_argument('--java-timeout', type=float, default=1.0)
    parser.add_argument('--java-fit', choices=('stretch', 'contain'), default='stretch')
    args = parser.parse_args(argv)
    if not 1 <= args.java_port <= 65535 or not math.isfinite(args.java_timeout) or not 0 < args.java_timeout <= 10:
        parser.error('Java port must be 1..65535 and timeout > 0 and <= 10 seconds')
    if args.connect_java and not args.calibrate:
        args.load_calibration = True
    if args.calibrate and args.load_calibration:
        parser.error("Choose --calibrate OR --load-calibration")
    if not (2 <= args.viewport_width <= 4096 and 2 <= args.viewport_height <= 4096):
        parser.error("Viewport dimensions must be between 2 and 4096")
    args.simulate = args.simulate or args.calibrate or args.load_calibration or args.connect_java
    if not all(math.isfinite(v) for v in (args.pinch_threshold, args.release_threshold,
                                         args.debounce_ms, args.cooldown_ms)):
        parser.error("Gesture settings must be finite")
    if not 0 < args.pinch_threshold < args.release_threshold:
        parser.error("Require 0 < --pinch-threshold < --release-threshold")
    if args.debounce_ms < 0 or args.cooldown_ms < 0:
        parser.error("Debounce and cooldown must be nonnegative")
    return args
