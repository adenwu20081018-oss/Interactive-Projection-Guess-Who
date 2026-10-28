"""Optional calibration UI and simulated viewport. No game communication."""
import textwrap
import cv2
import numpy as np

from calibration import CalibrationSession, ORDER

CANVAS = "Guess Who - Simulated viewport"


class ProjectionPreview:
    def __init__(self, args, camera_window):
        self.args = args
        self.session = None
        self.camera_window = camera_window
        self.changed = False
        self.allow_clicks = False
        self.selected_until = 0
        cv2.namedWindow(CANVAS, cv2.WINDOW_NORMAL)
        cv2.resizeWindow(CANVAS, min(args.viewport_width, 1280), min(args.viewport_height, 720))
        cv2.setMouseCallback(camera_window, self.mouse)

    def mouse(self, event, x, y, flags, userdata):
        if event == cv2.EVENT_LBUTTONDOWN and self.allow_clicks and self.session:
            self.session.click(x, y)
            self.changed = True

    def begin_frame(self, size):
        self.allow_clicks = True
        if self.session is None:
            self.session = CalibrationSession(size, (self.args.viewport_width, self.args.viewport_height),
                                              not self.args.no_mirror, self.args.camera, self.args.calibration_file)
            if self.args.load_calibration:
                self.session.key(ord('l'))
        elif self.session.camera_size != tuple(size):
            self.session.camera_size = tuple(size)
            self.session.reset()
            self.session.message = "Camera size changed. Recalibrate before selecting"
            self.changed = True

    def key(self, key):
        if self.session and key in (ord('u'), ord('U'), 8, ord('r'), ord('R'),
                                    ord('c'), ord('C'), ord('s'), ord('S'), ord('l'), ord('L')):
            self.session.key(key)
            self.changed = True
            self.selected_until = 0
            print(self.session.message)

    def render(self, frame, point, selection, now, extra_lines=()):
        """Draw current data only. Return augmented camera frame and accepted flag."""
        session = self.session
        mapper = session.mapper if session else None
        mapped = mapper.map_point(point) if mapper and point is not None else None
        accepted = False
        if selection is not None and mapper:
            message = mapper.selection_message(selection) if mapped is not None else None
            if message:
                print(message, flush=True)
                self.selected_until = now + 0.6
                accepted = True
            else:
                print("Selection ignored: outside calibrated interaction area", flush=True)
        if point is None or mapped is None:
            self.selected_until = 0
        if mapper is None:
            coordinate_text = "Viewport: NOT CALIBRATED - selection disabled"
        elif point is None:
            coordinate_text = "Viewport: no tracked fingertip"
        elif mapped is None:
            coordinate_text = "Viewport: OUTSIDE - selection disabled"
        else:
            coordinate_text = f"Viewport: X={mapped[0]:.1f} Y={mapped[1]:.1f}"
        if session:
            corners = session.points
            for i, p in enumerate(corners):
                xy = tuple(map(int, p))
                cv2.circle(frame, xy, 7, (0, 128, 255), -1)
                label = ("TL", "TR", "BR", "BL")[i]
                origin = (max(0, min(xy[0]+9, frame.shape[1]-30)), max(16, xy[1]-8))
                cv2.putText(frame, label, origin, cv2.FONT_HERSHEY_SIMPLEX, 0.45, (0, 128, 255), 1)
            if len(corners) >= 2:
                cv2.polylines(frame, [np.int32(corners)], len(corners) == 4, (0, 128, 255), 2)
        w, h = self.args.viewport_width, self.args.viewport_height
        canvas = np.full((h, w, 3), 32, dtype=np.uint8)
        for x in range(0, w, max(1, w//8)):
            cv2.line(canvas, (x, 0), (x, h-1), (80, 80, 80), 1)
        for y in range(0, h, max(1, h//4)):
            cv2.line(canvas, (0, y), (w-1, y), (80, 80, 80), 1)
        cv2.rectangle(canvas, (0, 0), (w-1, h-1), (180, 180, 180), 2)
        cv2.putText(canvas, f"Virtual {w} x {h} | {coordinate_text}", (15, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 1)
        if mapped is not None:
            cursor = tuple(int(np.floor(v + 0.5 + 1e-7)) for v in mapped)
            color = (0, 255, 0) if now < self.selected_until else (0, 255, 255)
            cv2.circle(canvas, cursor, 12, color, 2)
            if now < self.selected_until:
                cv2.putText(canvas, "SELECTED", (15, 60), cv2.FONT_HERSHEY_SIMPLEX, 0.8, color, 2)
        cv2.imshow(CANVAS, canvas)
        # Add instructions BELOW the image: original image pixels and mouse
        # positions stay unchanged. Clicks on this footer are rejected.
        lines = [coordinate_text, "Click TL > TR > BR > BL | U undo | R/C reset | S save | L load | Q quit"]
        if now < self.selected_until:
            lines.insert(0, "SELECTED")
        lines += [session.message if session else "Waiting for first camera frame"]
        lines += list(extra_lines)
        wrapped = [part for line in lines for part in textwrap.wrap(line, max(20, frame.shape[1]//8))]
        footer = np.full((24*len(wrapped)+12, frame.shape[1], 3), 24, dtype=np.uint8)
        for i, line in enumerate(wrapped):
            cv2.putText(footer, line, (8, 22+i*24), cv2.FONT_HERSHEY_SIMPLEX, 0.45, (255, 255, 255), 1)
        return np.vstack((frame, footer)), accepted
