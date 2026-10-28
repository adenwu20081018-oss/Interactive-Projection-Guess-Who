"""Real Tasks inference on blank frames; no camera or display is opened."""
import importlib.util
import unittest

from config import DEFAULT_MODEL
from hand_tracker import HandTracker

HAS_VISION = all(importlib.util.find_spec(name) for name in ('cv2', 'numpy', 'mediapipe'))


@unittest.skipUnless(HAS_VISION, 'Install requirements.txt to run real inference')
class InferenceTests(unittest.TestCase):
    def test_blank_frames_have_no_hand(self):
        import numpy as np
        with HandTracker(DEFAULT_MODEL) as tracker:
            frame = np.zeros((480, 640, 3), dtype=np.uint8)
            self.assertEqual(tracker.detect(frame, 0), [])
            self.assertEqual(tracker.detect(frame, 33), [])


if __name__ == '__main__':
    unittest.main()
