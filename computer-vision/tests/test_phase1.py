"""Run from computer-vision: python -m unittest discover -s tests -v"""
import contextlib
import io
from pathlib import Path
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import MagicMock, patch
import zipfile

from camera import Camera, CameraError
from config import DEFAULT_MODEL, parse_args
from hand_tracker import FrameClock, download_model, landmark_pixels, to_pixel, validate_model
from main import draw_overlay, main, run


class CoordinatesTests(unittest.TestCase):
    def test_center(self):
        self.assertEqual(to_pixel(0.5, 0.5, 640, 480), (320, 240))

    def test_edges_and_outside(self):
        self.assertEqual(to_pixel(-0.2, 1.2, 640, 480), (0, 479))
        self.assertEqual(to_pixel(1, 1, 1, 1), (0, 0))

    def test_invalid_coordinates(self):
        for x, width in [(float('nan'), 640), (float('inf'), 640), (0, 0)]:
            with self.assertRaises(ValueError):
                to_pixel(x, 0, width, 480)

    def test_tracking_loss_clears_points(self):
        self.assertEqual(landmark_pixels([], 640, 480), [])
        self.assertEqual(landmark_pixels([SimpleNamespace(x=0, y=0)], 640, 480), [])

    def test_all_landmarks_and_tip(self):
        points = landmark_pixels([SimpleNamespace(x=i / 40, y=0.5) for i in range(21)], 640, 480)
        self.assertEqual(len(points), 21)
        self.assertEqual(points[8], (128, 240))

    def test_nonfinite_landmark_is_tracking_loss(self):
        self.assertEqual(landmark_pixels([SimpleNamespace(x=float('nan'), y=0)] * 21, 640, 480), [])


class ConfigurationAndClockTests(unittest.TestCase):
    def test_defaults(self):
        args = parse_args([])
        self.assertEqual((args.camera, args.width, args.height), (0, 640, 480))
        self.assertTrue(args.model.is_absolute())

    def test_overrides(self):
        args = parse_args(['--camera', '2', '--width', '1280', '--height', '720', '--no-mirror'])
        self.assertEqual((args.camera, args.width, args.height, args.no_mirror), (2, 1280, 720, True))

    def test_bad_arguments(self):
        for flag, value in [('--camera', '-1'), ('--width', '0'), ('--height', '-10'), ('--width', 'abc')]:
            with contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
                parse_args([flag, value])

    def test_timestamps_strictly_increase(self):
        clock = FrameClock()
        self.assertEqual([clock.timestamp_ms(t) for t in [1, 1, 0.9, 1.01]], [1000, 1001, 1002, 1010])

    def test_fps_and_repeated_time(self):
        clock = FrameClock()
        self.assertEqual(clock.update_fps(1), 0)
        self.assertAlmostEqual(clock.update_fps(1.1), 10)
        self.assertAlmostEqual(clock.update_fps(1.1), 10)


class CameraTests(unittest.TestCase):
    def make_cv(self):
        cv = MagicMock()
        cv.error = RuntimeError
        return cv

    def test_open_failure_releases_camera(self):
        cv = self.make_cv()
        cv.VideoCapture.return_value.isOpened.return_value = False
        with self.assertRaises(CameraError):
            Camera(cv=cv)
        cv.VideoCapture.return_value.release.assert_called_once()

    def test_temporary_loss_recovers_and_resets_counter(self):
        cv = self.make_cv()
        frame = SimpleNamespace(size=10)
        cv.VideoCapture.return_value.read.side_effect = [(False, None), (True, frame), (False, None)]
        with Camera(cv=cv, max_failures=2) as camera:
            self.assertIsNone(camera.read())
            self.assertIs(camera.read(), frame)
            self.assertIsNone(camera.read())
        cv.VideoCapture.return_value.release.assert_called_once()

    def test_persistent_loss_exits_and_releases(self):
        cv = self.make_cv()
        cv.VideoCapture.return_value.read.return_value = (True, SimpleNamespace(size=0))
        with self.assertRaises(CameraError):
            with Camera(cv=cv, max_failures=2) as camera:
                camera.read()
                camera.read()
        cv.VideoCapture.return_value.release.assert_called_once()

    def test_driver_read_exception_is_retried(self):
        cv = self.make_cv()
        cv.VideoCapture.return_value.read.side_effect = RuntimeError('driver')
        with Camera(cv=cv) as camera:
            self.assertIsNone(camera.read())


class ModelTests(unittest.TestCase):
    def test_supplied_model_bundle(self):
        self.assertEqual(validate_model(DEFAULT_MODEL), DEFAULT_MODEL)

    def test_missing_and_corrupt_model(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'model.task'
            with self.assertRaises(ValueError):
                validate_model(path)
            path.write_bytes(b'not a model')
            with self.assertRaises(ValueError):
                validate_model(path)

    def test_incomplete_model(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'model.task'
            with zipfile.ZipFile(path, 'w') as bundle:
                bundle.writestr('wrong.tflite', b'')
            with self.assertRaises(ValueError):
                validate_model(path)

    def test_failed_download_preserves_existing_model(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'model.task'
            path.write_bytes(b'existing')
            with patch('hand_tracker.urlopen', side_effect=OSError('offline')):
                with self.assertRaises(OSError):
                    download_model(path)
            self.assertEqual(path.read_bytes(), b'existing')
            self.assertFalse(path.with_suffix('.download').exists())


class ApplicationTests(unittest.TestCase):
    def test_draws_connections_landmarks_and_tip(self):
        cv = MagicMock()
        points = [(i, i) for i in range(21)]
        draw_overlay(cv, object(), points, 30)
        self.assertEqual(cv.line.call_count, 21)
        self.assertEqual(cv.circle.call_count, 22)
        self.assertEqual(cv.circle.call_args.args[1:3], ((8, 8), 12))

    def test_loss_overlay_has_no_stale_coordinates(self):
        cv = MagicMock()
        draw_overlay(cv, object(), [], 30)
        cv.circle.assert_not_called()
        self.assertTrue(any('X: --' in call.args[1] for call in cv.putText.call_args_list))

    def test_q_releases_tracker_and_camera(self):
        cv, np = MagicMock(), MagicMock()
        cv.waitKey.return_value = ord('q')
        with patch('main.HandTracker') as tracker, patch('main.Camera') as camera:
            camera.return_value.__enter__.return_value.read.return_value = None
            run(parse_args([]), cv, np)
            tracker.return_value.__exit__.assert_called_once()
            camera.return_value.__exit__.assert_called_once()

    def test_startup_error_is_reported_and_windows_closed(self):
        cv, np = MagicMock(), MagicMock()
        with patch.dict('sys.modules', {'cv2': cv, 'numpy': np}), patch('main.run', side_effect=CameraError('unavailable')):
            with contextlib.redirect_stderr(io.StringIO()) as output:
                self.assertEqual(main([]), 1)
            self.assertIn('unavailable', output.getvalue())
            cv.destroyAllWindows.assert_called_once()


if __name__ == '__main__':
    unittest.main()
