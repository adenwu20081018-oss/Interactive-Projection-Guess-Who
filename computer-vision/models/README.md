# Hand Landmarker model

Run `python main.py --download-model` from computer-vision to download Google's
version 1 float16 Hand Landmarker bundle into this folder. This setup command
needs only Python's standard library and does not open the camera.

Source: https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task

The download is written to a temporary file and checked for the two expected
TFLite entries and valid ZIP CRCs before replacing the destination. This checks
bundle integrity, not model inference. Runtime tracking uses the local model
and does not download anything or save webcam images.

Official model information: https://ai.google.dev/edge/mediapipe/solutions/vision/hand_landmarker#models
