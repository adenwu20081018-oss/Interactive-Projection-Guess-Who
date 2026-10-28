"""Synthetic landmark sequences; no webcam or physical gesture claims."""
import contextlib
import io
import unittest
from types import SimpleNamespace
from unittest.mock import MagicMock, patch

from config import parse_args
from gesture_detector import GestureDetector, pinch_ratio
from main import run, draw_overlay


def hand(ratio, scale=100, width=640, height=480, tip_x=300):
    points = [SimpleNamespace(x=0.5, y=0.5) for _ in range(21)]
    points[0] = SimpleNamespace(x=200 / width, y=300 / height)
    points[9] = SimpleNamespace(x=200 / width, y=(300-scale) / height)
    points[8] = SimpleNamespace(x=tip_x / width, y=200 / height)
    points[4] = SimpleNamespace(x=(tip_x-ratio*scale) / width, y=200 / height)
    return points


class GestureTests(unittest.TestCase):
    def setUp(self):
        self.detector = GestureDetector()

    def update(self, ratio, now, **kwargs):
        return self.detector.update(hand(ratio, **kwargs) if ratio is not None else [], 640, 480, now)

    def arm(self):
        self.update(0.8, 0)
        self.assertTrue(self.update(0.8, 0.1).armed)

    def click(self):
        self.arm()
        self.update(0.1, 0.2)
        return self.update(0.1, 0.3)

    def test_open_to_pinch(self):
        self.assertEqual(self.click().selection, (300, 200))

    def test_starting_closed_does_not_select(self):
        self.assertIsNone(self.update(0.1, 0).selection)
        self.assertIsNone(self.update(0.1, 0.1).selection)

    def test_held_pinch_emits_once(self):
        self.click()
        for i in range(4, 50):
            result = self.update(0.1, i / 10)
            self.assertEqual(result.status, 'PINCHED')
            self.assertIsNone(result.selection)

    def test_release_and_reactivation(self):
        self.click()
        self.update(0.8, 0.4)
        self.assertEqual(self.update(0.8, 0.5).status, 'READY')
        self.update(0.8, 0.6)
        self.update(0.1, 0.7)
        self.assertIsNotNone(self.update(0.1, 0.8).selection)

    def test_hysteresis_retains_pinch(self):
        self.click()
        self.assertEqual(self.update(0.38, 0.4).status, 'PINCHED')
        self.assertEqual(self.update(0.38, 0.5).status, 'PINCHED')

    def test_hysteresis_retains_ready(self):
        self.arm()
        self.update(0.38, 0.2)
        self.assertEqual(self.update(0.38, 0.3).status, 'READY')

    def test_short_pinch_and_band_interrupt_debounce(self):
        self.arm()
        self.update(0.1, 0.2)
        self.update(0.38, 0.25)
        self.assertIsNone(self.update(0.1, 0.3).selection)
        self.assertIsNotNone(self.update(0.1, 0.4).selection)

    def test_short_release_does_not_rearm(self):
        self.click()
        self.update(0.8, 0.4)
        self.update(0.1, 0.45)
        self.assertIsNone(self.update(0.1, 0.6).selection)
        self.assertEqual(self.detector.status, 'PINCHED')

    def test_cooldown_consumes_pinch_without_delayed_click(self):
        self.detector = GestureDetector(debounce_ms=0, cooldown_ms=400)
        self.update(0.8, 0)
        self.assertIsNotNone(self.update(0.1, 0.01).selection)
        self.update(0.8, 0.1)
        self.assertIsNone(self.update(0.1, 0.2).selection)
        for t in (0.3, 0.4, 0.5, 0.6):
            self.assertIsNone(self.update(0.1, t).selection)
        self.update(0.8, 0.7)
        self.assertIsNotNone(self.update(0.1, 0.8).selection)

    def test_loss_during_hold_requires_release(self):
        self.click()
        self.update(None, 0.4)
        for t in (0.5, 0.6, 0.7):
            self.assertIsNone(self.update(0.1, t).selection)
        self.update(0.8, 0.8)
        self.assertTrue(self.update(0.8, 0.9).armed)
        self.update(0.1, 1.0)
        self.assertIsNotNone(self.update(0.1, 1.1).selection)

    def test_loss_cancels_pending_pinch(self):
        self.arm()
        self.update(0.1, 0.2)
        self.update(None, 0.25)
        self.assertIsNone(self.update(0.1, 0.4).selection)
        self.assertFalse(self.detector.armed)

    def test_long_frame_gap_requires_rearm(self):
        self.arm()
        self.update(0.1, 0.2)
        self.assertIsNone(self.update(0.1, 1.0).selection)
        self.assertFalse(self.detector.armed)

    def test_event_uses_confirmation_frame_coordinates(self):
        self.arm()
        self.update(0.1, 0.2, tip_x=250)
        self.assertEqual(self.update(0.1, 0.3, tip_x=450).selection, (450, 200))

    def test_different_hand_sizes_and_aspect_ratios(self):
        for scale in (20, 50, 150):
            for width, height in ((640,480), (1280,720)):
                self.assertAlmostEqual(pinch_ratio(hand(0.2, scale, width, height), width, height), 0.2)
                detector = GestureDetector(debounce_ms=0)
                detector.update(hand(0.8, scale, width, height), width, height, 0)
                self.assertIsNotNone(detector.update(hand(0.2, scale, width, height), width, height, 0.1).selection)

    def test_invalid_geometry_is_tracking_loss(self):
        points = hand(0.1)
        points[9] = points[0]
        self.assertIsNone(pinch_ratio(points, 640,480))
        points = hand(0.1)
        points[4].x = float('nan')
        self.assertIsNone(pinch_ratio(points, 640,480))
        points[4].x = -0.1
        self.assertIsNone(pinch_ratio(points, 640,480))

    def test_threshold_boundaries(self):
        self.detector = GestureDetector(debounce_ms=0)
        self.assertTrue(self.update(0.45, 0).armed)
        self.assertIsNotNone(self.update(0.30, 0.1).selection)

    def test_invalid_config(self):
        for args in (['--pinch-threshold','0.6'], ['--cooldown-ms','-1'],
                     ['--debounce-ms','nan'], ['--release-threshold','inf']):
            with contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
                parse_args(args)
        for kwargs in ({'pinch_threshold': 0.5}, {'debounce_ms': -1}, {'cooldown_ms': float('nan')}):
            with self.assertRaises(ValueError):
                GestureDetector(**kwargs)

    def test_bad_timestamp_rejected(self):
        self.update(0.8, 1)
        with self.assertRaises(ValueError):
            self.update(0.8, 0)


class IntegrationTests(unittest.TestCase):
    def test_console_one_line_per_pinch_and_release(self):
        cv, np = MagicMock(), MagicMock()
        cv.waitKey.side_effect = [-1, -1, -1, -1, -1, ord('q')]
        cv.getWindowProperty.return_value = 1
        frame = SimpleNamespace(shape=(480, 640, 3))
        args = parse_args(['--no-mirror','--debounce-ms','0','--cooldown-ms','0'])
        times = [v for i in range(6) for v in (i*0.1, i*0.1)]
        with patch('main.HandTracker') as tracker, patch('main.Camera') as camera, patch('main.time.perf_counter', side_effect=times):
            camera.return_value.__enter__.return_value.read.return_value = frame
            tracker.return_value.__enter__.return_value.detect.side_effect = [hand(r) for r in (0.8,0.1,0.1,0.8,0.1,0.1)]
            with contextlib.redirect_stdout(io.StringIO()) as output:
                run(args, cv, np)
        self.assertEqual([s for s in output.getvalue().splitlines() if s.startswith('SELECT ')],
                         ['SELECT x=300 y=200', 'SELECT x=300 y=200'])

    def test_overlay_status_and_selected(self):
        detector = GestureDetector(debounce_ms=0)
        detector.update(hand(0.8),640,480,0)
        result = detector.update(hand(0.1),640,480,0.1)
        cv = MagicMock()
        draw_overlay(cv, object(), [(1,1)]*21, 30, gesture=result, selected=True)
        texts = [c.args[1] for c in cv.putText.call_args_list]
        self.assertIn('Gesture: PINCHED', texts)
        self.assertIn('SELECTED', texts)


if __name__ == '__main__':
    unittest.main()
