"""Run this file to show one hand's landmarks. Q exits the preview."""
import sys
import time
from contextlib import nullcontext

from camera import Camera
from config import parse_args
from gesture_detector import GestureDetector
from hand_tracker import CONNECTIONS, FrameClock, HandTracker, download_model, landmark_pixels

WINDOW = "Guess Who - Hand Tracking"


def draw_overlay(cv, frame, points, fps, status=None, gesture=None, selected=False):
    for start, end in CONNECTIONS if points else ():
        cv.line(frame, points[start], points[end], (0, 220, 0), 2)
    for point in points:
        cv.circle(frame, point, 4, (255, 180, 0), -1)
    if points:
        cv.circle(frame, points[8], 12, (0, 255, 255), 3)
        position = f"Index tip: X={points[8][0]}  Y={points[8][1]}"
    else:
        position = "No hand detected - X: --  Y: --"
    lines = [f"FPS: {fps:.1f} | Q: quit", status or position]
    if gesture is not None:
        hint = " | Open fingers to arm" if not gesture.armed else ""
        lines.append(f"Gesture: {gesture.status}{hint}")
        if gesture.ratio is not None:
            lines.append(f"Tip distance / palm: {gesture.ratio:.2f}")
            if points:
                cv.line(frame, points[4], points[8], (255, 0, 255), 2)
        if selected:
            lines.append("SELECTED")
    for i, text in enumerate(lines):
        origin = (10, 25 + i * 28)
        cv.putText(frame, text, origin, cv.FONT_HERSHEY_SIMPLEX, 0.6, (0, 0, 0), 4)
        cv.putText(frame, text, origin, cv.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 1)


def run(args, cv, np):
    clock = FrameClock()
    gestures = GestureDetector(args.pinch_threshold, args.release_threshold,
                               args.debounce_ms, args.cooldown_ms)
    selected_until = 0.0
    java = None
    java_epoch = 0
    if args.connect_java:
        from java_connection import JavaConnection
        java = JavaConnection(args.java_port, args.java_timeout, args.java_fit)
    # Both context managers release their resources, including on errors or Q.
    with HandTracker(args.model) as tracker, Camera(args.camera, args.width, args.height, cv=cv) as camera, (java if java else nullcontext()):
        cv.namedWindow(WINDOW, cv.WINDOW_AUTOSIZE)
        projection = None
        if args.simulate:
            from projection_preview import ProjectionPreview, CANVAS
            projection = ProjectionPreview(args, WINDOW)
        size_reported = False
        while True:
            frame = camera.read()
            if frame is None:
                if java:
                    java.invalidate()
                # Do not display stale fingertip coordinates when the camera fails.
                frame = np.zeros((args.height, args.width, 3), dtype=np.uint8)
                gesture = gestures.update([], args.width, args.height, time.perf_counter())
                selected_until = 0.0
                draw_overlay(cv, frame, [], 0, "Camera frame unavailable - retrying", gesture)
                if projection:
                    projection.allow_clicks = False
                    extra = java_feedback(java, None, None) if java else ()
                    frame, _ = projection.render(frame, None, None, time.perf_counter(), extra)
                delay = 30
            else:
                if not size_reported:
                    print(f"Camera frame size: {frame.shape[1]} x {frame.shape[0]} (requested {args.width} x {args.height})")
                    size_reported = True
                if not args.no_mirror:
                    frame = cv.flip(frame, 1)
                if projection:
                    projection.begin_frame((frame.shape[1], frame.shape[0]))
                landmarks = tracker.detect(frame, clock.timestamp_ms(time.perf_counter()))
                points = landmark_pixels(landmarks, frame.shape[1], frame.shape[0])
                now = time.perf_counter()
                if projection and (projection.changed or projection.session.mapper is None):
                    if java:
                        java.invalidate()
                    gestures.update([], frame.shape[1], frame.shape[0], now)
                    projection.changed = False
                if java:
                    state = java.snapshot()
                    # A reconnect requires an open hand before another pinch.
                    if not state['ready'] or state['epoch'] != java_epoch:
                        gestures.update([], frame.shape[1], frame.shape[0], now)
                    java_epoch = state['epoch']
                    if not points:
                        java.invalidate()
                gesture = gestures.update(landmarks if points else [], frame.shape[1], frame.shape[0], now)
                if gesture.selection is not None and not projection:
                    x, y = gesture.selection
                    print(f"SELECT x={x} y={y}", flush=True)
                    selected_until = now + 0.6
                if gesture.ratio is None:
                    if java:
                        java.invalidate()
                    selected_until = 0.0
                draw_overlay(cv, frame, points, clock.update_fps(now),
                             gesture=gesture, selected=now < selected_until)
                if projection:
                    # Raw image coordinates keep outside points outside; drawing
                    # helpers clamp to image edges and must not decide validity.
                    point = (landmarks[8].x * frame.shape[1], landmarks[8].y * frame.shape[0]) if points else None
                    if java and gesture.selection is not None:
                        java.submit(projection.session.mapper, point, gesture.selection)
                    extra = java_feedback(java, projection.session.mapper, point) if java else ()
                    frame, _ = projection.render(frame, point, gesture.selection, now, extra)
                delay = 1
            cv.imshow(WINDOW, frame)
            key = cv.waitKey(delay) & 0xFF
            if key in (ord("q"), ord("Q")):
                break
            if projection:
                projection.key(key)
                if java and projection.changed:
                    java.invalidate()
                if cv.getWindowProperty(CANVAS, cv.WND_PROP_VISIBLE) < 1:
                    break
            if cv.getWindowProperty(WINDOW, cv.WND_PROP_VISIBLE) < 1:
                break


def java_feedback(java, mapper, point):
    from java_connection import scale_viewport
    state = java.snapshot()
    lines = ['Java: ' + state['status'], state['latest']]
    mapped = mapper.map_point(point) if mapper and point is not None else None
    if mapped is not None and state['size']:
        x, y = scale_viewport(mapped, mapper.viewport_size, state['size'])
        lines.append(f'Java viewport: X={x:.1f} Y={y:.1f}')
    return lines


def main(argv=None):
    args = parse_args(argv)
    cv = None
    try:
        if args.download_model:
            download_model(args.model)
            print(f"Model ready: {args.model}")
            return 0
        import cv2 as cv
        import numpy as np
        run(args, cv, np)
        return 0
    except KeyboardInterrupt:
        return 0
    except Exception as error:
        # Hardware/driver/model failures should give a useful message, not a traceback.
        print(f"Unable to continue: {error}", file=sys.stderr)
        print("See README.md for dependency, model, and camera troubleshooting.", file=sys.stderr)
        return 1
    finally:
        if cv is not None:
            cv.destroyAllWindows()


if __name__ == "__main__":
    raise SystemExit(main())
