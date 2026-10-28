"""Real OpenCV mapping plus synthetic gesture/UI tests. No camera is opened."""
import contextlib
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import cv2
import numpy as np

from calibration import CalibrationSession, load_calibration, save_calibration
from config import parse_args
from coordinate_mapper import CoordinateMapper
from gesture_detector import GestureDetector
from projection_preview import ProjectionPreview
from main import run
from test_gesture_detector import hand

RECT = [(100, 100), (500, 100), (500, 400), (100, 400)]


class MappingTests(unittest.TestCase):
    def test_identity(self):
        mapper = CoordinateMapper([(0,0),(639,0),(639,479),(0,479)], (640,480), (640,480))
        np.testing.assert_allclose(mapper.map_point((250,300)), (250,300), atol=1e-6)

    def test_scaling(self):
        mapper = CoordinateMapper(RECT, (640,480), (1280,720))
        np.testing.assert_allclose(mapper.map_point((300,250)), (639.5,359.5), atol=1e-5)

    def test_perspective_four_corners_and_interior(self):
        points = [(120,80),(530,130),(480,420),(70,350)]
        mapper = CoordinateMapper(points, (640,480), (1280,720))
        for source, target in zip(points, [(0,0),(1279,0),(1279,719),(0,719)]):
            np.testing.assert_allclose(mapper.map_point(source), target, atol=1e-4)
        # Known projective matrix gives an independent interior correspondence.
        h = np.array([[1., .1, 100], [.05, 1, 70], [.0005, .0002, 1]])
        target = np.float32([[[0,0],[299,0],[299,199],[0,199]]])
        source = cv2.perspectiveTransform(target, h)[0]
        mapper = CoordinateMapper(source, (640,480), (300,200))
        camera = cv2.perspectiveTransform(np.float64([[[123,87]]]), h)[0,0]
        np.testing.assert_allclose(mapper.map_point(camera), (123,87), atol=1e-4)

    def test_invalid_layouts(self):
        cases = [[], [(0,0)]*4, [(0,0),(20,0),(40,0),(60,0)],
                 [RECT[i] for i in (0,2,1,3)], list(reversed(RECT)),
                 [(100,100),(500,100),(200,200),(100,400)],
                 [(0,0),(2,0),(2,2),(0,2)],
                 [(0,0),(700,0),(500,400),(0,400)],
                 [(float('nan'),0),(500,0),(500,400),(0,400)],
                 [RECT[i] for i in (1,2,3,0)]]
        for points in cases:
            with self.subTest(points=points), self.assertRaises(ValueError):
                CoordinateMapper(points, (640,480), (1280,720))

    def test_outside_and_boundary(self):
        mapper = CoordinateMapper(RECT, (640,480), (1280,720))
        for p in [(99,200),(501,200),(200,99),(200,401),(float('inf'),0)]:
            self.assertIsNone(mapper.map_point(p))
            self.assertIsNone(mapper.selection_message(p))
        self.assertIsNotNone(mapper.map_point((100,200)))

    def test_camera_mirror_convention(self):
        mapper = CoordinateMapper(RECT, (640,480), (1280,720))
        flipped = [(639-x,y) for x,y in RECT]
        flipped = [flipped[i] for i in (1,0,3,2)]
        mirrored = CoordinateMapper(flipped, (640,480), (1280,720))
        a = mapper.map_point((250,200))
        b = mirrored.map_point((639-250,200))
        np.testing.assert_allclose(b, (1279-a[0], a[1]), atol=1e-5)


class PersistenceTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.path = Path(self.directory.name)/'calibration.json'
        self.mapper = CoordinateMapper(RECT, (640,480), (1280,720))
        save_calibration(self.path, self.mapper, True, 0)

    def load(self, size=(640,480), mirror=True, viewport=(1280,720), camera=0):
        return load_calibration(self.path, size, mirror, viewport, camera)

    def test_round_trip_and_metadata(self):
        loaded = self.load()
        np.testing.assert_allclose(loaded.matrix, self.mapper.matrix)
        data = json.loads(self.path.read_text())
        self.assertIn('created_utc', data)
        self.assertEqual(data['camera_resolution'], [640,480])

    def test_resolution_mismatch(self):
        with self.assertRaisesRegex(ValueError, 'resolution mismatch'):
            self.load(size=(1280,720))

    def test_mirror_viewport_and_camera_mismatch(self):
        for kw in ({'mirror':False}, {'viewport':(800,500)}, {'camera':1}):
            with self.subTest(kw=kw), self.assertRaises(ValueError):
                self.load(**kw)

    def test_bad_files_and_tampered_matrix(self):
        original = json.loads(self.path.read_text())
        for key, value in [('schema_version',99), ('mirrored','true'),
                           ('source_points',[]), ('transformation_matrix',[[0]*3]*3),
                           ('coordinate_convention','raw'), ('viewport_dimensions',[0,720])]:
            data = dict(original)
            data[key] = value
            self.path.write_text(json.dumps(data))
            with self.subTest(key=key), self.assertRaises(ValueError):
                self.load()
        for text in ('broken', '{}', '[]'):
            self.path.write_text(text)
            with self.assertRaises(ValueError):
                self.load()
        self.path.unlink()
        with self.assertRaisesRegex(ValueError, 'Cannot load'):
            self.load()

    def test_reset_undo_save_load(self):
        session = CalibrationSession((640,480),(1280,720),True,0,self.path)
        for p in RECT:
            session.click(*p)
        self.assertIsNotNone(session.mapper)
        session.undo()
        self.assertIsNone(session.mapper)
        self.assertEqual(len(session.points), 3)
        session.click(*RECT[3])
        session.key(ord('s'))
        session.key(ord('r'))
        self.assertEqual(session.points, [])
        self.assertIsNone(session.mapper)
        session.key(ord('l'))
        self.assertIsNotNone(session.mapper)
        self.path.write_text('{}')
        session.key(ord('l'))
        self.assertIsNone(session.mapper)

    def test_invalid_fourth_corner_remains_editable(self):
        session = CalibrationSession((640,480),(1280,720),True,0,self.path)
        for p in RECT[:3]+[RECT[0]]:
            session.click(*p)
        self.assertEqual(len(session.points),3)
        self.assertIsNone(session.mapper)
        session.click(*RECT[3])
        self.assertIsNotNone(session.mapper)


class IntegrationTests(unittest.TestCase):
    def test_complete_simulation_loop_outputs_converted_event(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)/'test.json'
            mapper = CoordinateMapper(RECT, (640,480), (1280,720))
            save_calibration(path, mapper, False, 0)
            args = parse_args(['--load-calibration','--calibration-file',str(path),
                               '--no-mirror','--debounce-ms','0'])
            with (patch('main.HandTracker') as tracker, patch('main.Camera') as camera,
                  patch('main.time.perf_counter',side_effect=[0,0,.1,.1,.2,.2]),
                  patch.object(cv2,'namedWindow'), patch.object(cv2,'resizeWindow'),
                  patch.object(cv2,'setMouseCallback'), patch.object(cv2,'imshow'),
                  patch.object(cv2,'waitKey',side_effect=[-1,-1,ord('q')]),
                  patch.object(cv2,'getWindowProperty',return_value=1),
                  contextlib.redirect_stdout(io.StringIO()) as output):
                camera.return_value.__enter__.return_value.read.side_effect = [np.zeros((480,640,3),np.uint8) for _ in range(3)]
                tracker.return_value.__enter__.return_value.detect.side_effect = [hand(r) for r in (.8,.1,.1)]
                run(args,cv2,np)
            self.assertEqual([line for line in output.getvalue().splitlines() if line.startswith('SELECT')],
                             ['SELECT screen_x=640 screen_y=240'])

    def preview(self):
        args = parse_args(['--simulate'])
        with patch('projection_preview.cv2.namedWindow'), patch('projection_preview.cv2.resizeWindow'), patch('projection_preview.cv2.setMouseCallback'):
            preview = ProjectionPreview(args, 'camera')
        preview.begin_frame((640,480))
        return preview

    def test_phase2_select_maps_once(self):
        detector = GestureDetector(debounce_ms=0)
        mapper = CoordinateMapper(RECT, (640,480), (1280,720))
        messages = []
        for i,r in enumerate([0.8,0.1,0.1,0.1]):
            event = detector.update(hand(r),640,480,i*0.1)
            if event.selection:
                messages.append(mapper.selection_message(event.selection))
        self.assertEqual(messages, ['SELECT screen_x=640 screen_y=240'])

    def test_ui_select_and_outside_suppression(self):
        preview = self.preview()
        for p in RECT:
            preview.session.click(*p)
        for point, event, expected in [((300,200),(300,200),True), ((99.9,200),(100,200),False), (None,None,False)]:
            with patch('projection_preview.cv2.imshow'), contextlib.redirect_stdout(io.StringIO()) as output:
                frame, accepted = preview.render(np.zeros((480,640,3),np.uint8),point,event,1)
            self.assertEqual(accepted,expected)
            self.assertEqual('SELECT screen_x=' in output.getvalue(),expected)
            self.assertGreater(frame.shape[0],480)

    def test_uncalibrated_and_resolution_change_block_selection(self):
        preview = self.preview()
        with patch('projection_preview.cv2.imshow'), contextlib.redirect_stdout(io.StringIO()) as output:
            _, accepted = preview.render(np.zeros((480,640,3),np.uint8),(300,200),(300,200),1)
        self.assertFalse(accepted)
        self.assertEqual(output.getvalue(),'')
        for p in RECT:
            preview.session.click(*p)
        preview.begin_frame((1280,720))
        self.assertIsNone(preview.session.mapper)

    def test_mouse_order_footer_and_camera_failure(self):
        preview = self.preview()
        preview.mouse(cv2.EVENT_LBUTTONDOWN, 100, 490, 0, None)
        self.assertEqual(preview.session.points,[])
        preview.allow_clicks = False
        preview.mouse(cv2.EVENT_LBUTTONDOWN,100,100,0,None)
        self.assertEqual(preview.session.points,[])
        preview.allow_clicks = True
        for x,y in RECT:
            preview.mouse(cv2.EVENT_LBUTTONDOWN,x,y,0,None)
        self.assertIsNotNone(preview.session.mapper)

    def test_config_modes(self):
        self.assertFalse(parse_args([]).simulate)
        self.assertTrue(parse_args(['--calibrate']).simulate)
        self.assertTrue(parse_args(['--load-calibration']).simulate)
        for args in (['--calibrate','--load-calibration'], ['--viewport-width','1']):
            with contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
                parse_args(args)


if __name__ == '__main__':
    unittest.main()
