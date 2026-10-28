# Vision subsystem — live Java gesture control

Tracking, pinch detection and calibration preview can run independently. Add `--connect-java` to send calibrated selections to the localhost Java server. The current [projector-ready guide](../docs/phase7a-projector-ready.md) covers presentation mode; the [Phase 4C technical record](../docs/phase4c-live-control.md) describes the original live-control integration. This Python subsystem does not communicate directly with Arduino; the Java game owns optional serial LED feedback.

The preview detects at most one hand, draws its 21 landmarks and 21 connections, highlights index fingertip 8 in yellow, and shows its pixel X/Y coordinates and smoothed processing FPS. Press Q (or close the preview window) to exit. Focus the preview window for keyboard input.

## Windows installation (PowerShell)

Use **64-bit Python 3.12**. The pinned MediaPipe release below does not support Python 3.14. MediaPipe 0.10.21 is a deliberate stable package pin, not a claim to be the newest release; the implementation uses the supported Tasks Hand Landmarker API, not legacy `mp.solutions.hands`.

If you need Python 3.12, install it with the following command, then reopen your terminal:

```powershell
winget install --exact --id Python.Python.3.12
```

Alternatively, use the Python 3.12 Windows installer from https://www.python.org/downloads/windows/ and include the Python launcher. Check availability with `py -3.12 --version`.

For a fresh installation:

```powershell
Set-Location 'C:\path\to\Interactive-Projection-Guess-Who\computer-vision'
py -3.12 -m venv .venv
.\.venv\Scripts\python.exe -m pip install --upgrade pip
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m pip check
```

A local `.venv` was created during development using the app's bundled Python 3.12. It can be used immediately on this machine. Virtual environments are machine-specific; recreate one with the commands above when moving the project. You do not need to activate the environment or change PowerShell execution policy when using its Python executable directly.

The requirements pin MediaPipe 0.10.21, NumPy 1.26.4, and OpenCV-contrib 4.11.0.86. OpenCV-contrib provides `cv2` and includes the camera/drawing functionality needed here. Do not additionally install opencv-python or a headless OpenCV package in this environment: multiple distributions share the same `cv2` namespace. Transitive dependencies are resolved by pip.

## Model setup

`models/hand_landmarker.task` is downloaded locally and excluded from Git. After cloning, obtain the official version 1 float16 Hand Landmarker bundle:

```powershell
.\.venv\Scripts\python.exe main.py --download-model
```

This explicitly downloads from Google's model storage and exits without opening a camera. Interrupted or invalid downloads do not replace an existing model. The downloader checks ZIP CRCs and both expected TFLite entries; the runtime performs actual model loading. No network connection is needed for normal tracking once the packages and model are present. Frames are processed locally and are not recorded by this program.

## Start the program

From the computer-vision folder:

```powershell
.\.venv\Scripts\python.exe main.py
```

To choose another camera or request a different resolution:

```powershell
.\.venv\Scripts\python.exe main.py --camera 1 --width 1280 --height 720
```

Options: `--camera` (default 0), `--width` (640), `--height` (480), `--no-mirror`, `--model PATH`, and `--download-model`. Use `main.py --help` for details. Default model paths are relative to the script, not the terminal working directory; a custom relative `--model` path is relative to the terminal.

The camera may not support the requested resolution. The console prints the actual frame dimensions, and coordinates use that actual size. The preview is mirrored by default; X increases to the right and Y downward from its top-left corner. These are **image pixels**, not physical screen/projector coordinates or centimetres. `--no-mirror` disables mirroring. FPS measures processed frame throughput, including the loop's display/capture work, and is smoothed; the first value is zero until a second frame arrives.

## VS Code

1. Open this `computer-vision` folder in VS Code and install Microsoft's Python extension if needed.
2. Run **Python: Select Interpreter** from Ctrl+Shift+P. Choose `.venv\Scripts\python.exe` (use Enter interpreter path if it is not listed).
3. Open `main.py` and choose **Run Python File in Terminal**, or run the exact terminal command above in VS Code's integrated PowerShell terminal.
4. Click the camera preview to focus it. Press Q to quit. Ctrl+C in the terminal also cleans up.

## Failure and tracking-loss behavior

- No visible hand: clears old points and coordinates, shows `No hand detected`, and keeps looking. Move your hand back into view to resume automatically.
- Occasional failed camera frame: shows a blank retry screen rather than stale coordinates. After 30 consecutive failed reads, exits with an explanatory message and releases resources.
- Camera cannot open: check Settings > Privacy & security > Camera, allow desktop apps, close other camera applications, and try `--camera 1` or another index.
- Unsupported requested resolution: use `--width 640 --height 480`; inspect the actual dimensions printed at startup.
- Missing/corrupt model: rerun the model setup command.
- Missing package or native DLL error: verify Python 3.12 x64 and the selected `.venv`; reinstall requirements. Do not name your own files `cv2.py`, `numpy.py`, or `mediapipe.py`.
- Fatal driver or inference errors: report a short error and close the camera/tracker/window. Restart after addressing the cause. A camera driver that blocks indefinitely inside its native read cannot be recovered by Python's failed-frame counter.

## How the files work

| File | Responsibility |
| --- | --- |
| `main.py` | Preview loop, drawing, coordinates/FPS overlay, Q/window-close handling, cleanup and readable errors. |
| `camera.py` | Camera opening, resolution requests, frame reads, retry limit, and release. |
| `hand_tracker.py` | Tasks Hand Landmarker, model download/validation, coordinate mapping, connections, and timing helpers. |
| `config.py` | Defaults and validated command-line arguments. |
| `requirements.txt` | Compatible direct dependency pins for Python 3.12 x64. |
| `models/hand_landmarker.task` | Official pretrained model bundle. |
| `models/README.md` | Model source and setup details. |
| `tests/test_phase1.py` | Camera-independent unit tests, using fake camera/UI objects where needed. |
| `tests/test_inference.py` | Actual model loading and two blank-frame inference checks. |
| `.gitignore` | Keeps local environment, bytecode caches, and incomplete downloads out of Git. |
| `README.md` | Installation, usage, test instructions, and manual checklist. |

VIDEO mode processes each webcam frame synchronously with a strictly increasing timestamp, so the results belong to the displayed frame. MediaPipe performs detection/tracking internally with `num_hands=1`. RGB conversion is explicit; NumPy makes the converted frame contiguous. A separate GestureDetector handles pinch selection after landmark tracking.

## Automated checks

```powershell
.\.venv\Scripts\python.exe -m unittest discover -s tests -v
```

Core tests need only the Python standard library and the supplied model. The inference test is skipped if the vision packages are absent. Tests cover coordinate conversion/clamping/nonfinite values, 21-point handling, lost tracking, CLI validation, timestamp ordering, FPS, camera startup/read failure and recovery, resource cleanup, drawing calls, Q, model integrity, and failed-download preservation. Fake-camera tests do not prove that physical webcam hardware works.

## Manual webcam acceptance checklist

- Launch with camera 0; confirm a live, correctly oriented feed and the reported actual resolution.
- Hold one open hand in view under good lighting: all 21 points and finger/palm connections should follow it.
- Move the index finger: the yellow circle should stay on its tip and X/Y should update plausibly in image pixels.
- Remove the hand: old coordinates must disappear. Bring it back: tracking should return automatically.
- Confirm FPS updates and motion feels usable. Try lower resolution if slow.
- Try another supported resolution and camera index, if available.
- Press Q in the focused preview; confirm the window closes and camera indicator turns off. Relaunch to confirm the camera was released. Also try window-close and Ctrl+C.
- If using a removable USB camera, unplug it while running: confirm the retry/error behavior and restart after reconnecting.

## References

- Official Tasks API guide: https://ai.google.dev/edge/mediapipe/solutions/vision/hand_landmarker/python
- Model and tracking options: https://ai.google.dev/edge/mediapipe/solutions/vision/hand_landmarker
- Pinned MediaPipe package: https://pypi.org/project/mediapipe/0.10.21/

Phase 3 calibration instructions are below.

## Historical Phase 1 verification (October 26, 2026)

- Installed requirements into a project-local `.venv` with Python 3.12.14 x64.
- Downloaded the official model and validated its bundle entries and CRCs.
- Ran `.venv\Scripts\python.exe -m unittest discover -s tests -v`: **24 tests passed, zero skipped**. This includes actual Tasks inference on two blank 640x480 frames, confirming empty hand results.
- Ran `.venv\Scripts\python.exe -m pip check`: **No broken requirements found**.
- Ran `main.py --help`: argument help displayed successfully.
- An earlier core test run on Python 3.14 encountered three sandbox temporary-file permission errors; rerunning with normal filesystem access passed all 23 core tests. The final full suite used Python 3.12.
- MediaPipe emitted native feedback-tensor/projection warnings and protobuf deprecation warnings during inference; inference completed successfully. These logs were not hidden.
- No physical webcam session, live hand accuracy check, displayed FPS measurement, or physical Q/reconnect test was performed. Those remain the manual checklist above.
- No existing Java sources, binaries, images, sound files, or Phase 0 documentation were modified.


## Phase 2 behavior (uncalibrated mode)

No new dependencies or model downloads are needed. The Phase 1 camera, landmarks,
index highlight, coordinates, FPS, and Q cleanup remain in place. The overlay now
also shows READY/PINCHED, the normalized tip distance, a thumb-to-index line, and
SELECTED for 0.6 seconds after an accepted selection.

Start with an open hand (thumb and index separated). Hold that position briefly
until the `Open fingers to arm` hint disappears, then bring thumb and index together.
Every accepted pinch prints exactly one line such as:

```text
SELECT x=450 y=300
```

Coordinates come from index landmark 8 on the frame where the pinch passes debounce,
not from the first frame of finger movement. They remain camera-image coordinates
in the displayed orientation (mirrored by default), with no projector mapping.
Normal startup diagnostics may also appear in the terminal.

### Detection rules

- Measure the 2D distance between thumb tip 4 and index tip 8.
- Divide it by the distance from wrist 0 to middle-finger knuckle 9. Both distances
  use floating-point image pixels, with width/height applied before measuring.
  This makes the ratio insensitive to uniform hand scaling and avoids rectangular
  image distortion. Drawing-coordinate rounding/clamping does not affect detection.
- Ratio at or below 0.30 begins a pinch candidate. It must stay there for 100 ms.
- Ratio at or above 0.45 begins a release candidate, also requiring 100 ms.
- The band between those thresholds retains the stable READY/PINCHED state and
  cancels any pending debounce. These separate thresholds are hysteresis: small
  movements near one boundary do not repeatedly click.
- Once PINCHED, no additional event occurs until a stable release and new pinch.
- Minimum interval between emitted selections is 400 ms. A pinch accepted during
  cooldown enters PINCHED but produces no event. It is consumed, not queued: holding
  it past the cooldown does not click later. Release and pinch again.
- Startup, missing/invalid landmarks, failed camera frames, and sample gaps over
  250 ms require a stable open hand before selection can resume. Pending debounce
  is canceled and the previous pinch latch is retained. The status is the last
  stable state; the extra arming hint explains why selection is currently blocked.
  Losing and regaining a closed hand cannot produce another SELECT.
- Degenerate palm length (under one pixel), nonfinite coordinates, or an off-image
  wrist/knuckle/tip measurement is treated as lost tracking rather than a pinch.

### PowerShell commands

```powershell
Set-Location 'C:\path\to\Interactive-Projection-Guess-Who\computer-vision'
.\.venv\Scripts\python.exe main.py
.\.venv\Scripts\python.exe -m unittest discover -s tests -v
```

Optional tuning, with the default values shown explicitly:

```powershell
.\.venv\Scripts\python.exe main.py --camera 0 --width 640 --height 480 --pinch-threshold 0.30 --release-threshold 0.45 --debounce-ms 100 --cooldown-ms 400
```

Thresholds are dimensionless ratios; times are milliseconds. Release must exceed
pinch, and pinch must be positive. Timing values must be finite and nonnegative.
A larger pinch threshold accepts a wider fingertip gap; a larger release threshold
requires wider separation. Increase debounce/cooldown if accidental selections
occur. Zero timings are allowed for experiments but remove those protections.
VS Code uses the same interpreter and Run Python File instructions above.

### Phase 2 files and verification

New: `gesture_detector.py`, `tests/test_gesture_detector.py`.
Modified: `main.py`, `config.py`, `README.md`.
Camera/tracker code, dependency pins, model, original tests, and Java game were unchanged.

Executed the complete suite with the existing Python 3.12 environment:
**44 passed, zero skipped** (24 existing tests + 20 new tests). This includes real
blank-frame MediaPipe inference, synthetic gesture state transitions, normalized
sizes/aspect ratios, debounce, hysteresis, cooldown, loss/reacquisition, event-time
coordinates, mocked preview console output, and overlay feedback. Existing MediaPipe
native warnings and protobuf deprecation warnings appeared; inference passed.
No physical Phase 2 gesture testing was performed by the assistant.

### Manual Phase 2 acceptance checklist

1. Confirm the working Phase 1 display still shows all landmarks, index coordinates,
   and FPS. Open your thumb/index until the arming hint disappears.
2. Pinch and hold: expect PINCHED, a brief SELECTED overlay, and one console SELECT.
   Keep holding several seconds: no further SELECT lines should appear.
3. Separate the fingers clearly for at least 100 ms, then pinch again after cooldown:
   expect exactly one more SELECT with the current index coordinates.
4. Try a very brief pinch and a brief release: neither should bypass debounce.
   Move around the threshold gap and check for absence of repeated selections.
5. Pinch again very quickly after a release: during cooldown it may show PINCHED
   without SELECT. Continue holding: it must not emit a delayed event.
6. Move nearer/farther while keeping the whole hand visible; check normalized pinch
   behavior and tune thresholds if necessary.
7. Hide your hand while pinched, then return still pinched: no new event. Open fully,
   then pinch again to resume. Repeat a loss while READY or mid-pinch.
8. Press Q and confirm the camera releases normally.

### Known limitations

Normalization compensates for uniform scale, not extreme hand rotation, perspective,
foreshortening, or depth overlap: tips that overlap in the image may appear pinched
without touching physically. Low light, occlusion, very small hands, and noisy
landmarks can still cause missed or false detections. Defaults need manual tuning
for your camera/hand. The conservative loss rule can require extra opening after
brief occlusion; processing gaps above 250 ms also disarm, so very low FPS can
prevent gesture acceptance. Only one hand is tracked, with no persistent identity
check if another hand replaces it. No OS mouse click or game action is performed.

The following section describes the optional Phase 3 extension.


## Phase 3: calibration and projector-free simulation

No new packages are required. Your existing `.venv`, MediaPipe model, tracking,
pinch thresholds, debounce, and cooldown are reused. Plain `main.py` still runs
uncalibrated Phase 2. Use simulation mode to enable viewport-coordinate output.

### Exact Windows PowerShell commands

```powershell
Set-Location 'C:\path\to\Interactive-Projection-Guess-Who\computer-vision'
.\.venv\Scripts\python.exe main.py --simulate --calibrate
```

The default target is 1280 x 720. To specify it explicitly:

```powershell
.\.venv\Scripts\python.exe main.py --simulate --calibrate --camera 0 --width 640 --height 480 --viewport-width 1280 --viewport-height 720
```

Load a saved calibration on a later run:

```powershell
.\.venv\Scripts\python.exe main.py --simulate --load-calibration
```

Use the same camera, actual capture resolution, mirror setting, and viewport size
as when you saved. Add `--no-mirror` to BOTH calibration and later runs if desired.
`--calibrate` and `--load-calibration` each imply simulation; they cannot be combined.
`--simulate` alone starts fresh corner selection, without automatically loading a file.
A custom path can be supplied with `--calibration-file 'C:\path\my-calibration.json'`.

Run all tests:

```powershell
.\.venv\Scripts\python.exe -m unittest discover -s tests -v
```

VS Code: select the existing `.venv\Scripts\python.exe` interpreter, open an
integrated PowerShell terminal, and use the commands above. A plain Run Python File
without arguments remains uncalibrated mode.

### Paper-target procedure

1. Draw four clear corner marks on a rectangular piece of paper or another flat
   rectangle. Put it on a stable surface, fully visible to the camera. A landscape
   16:9 rectangle is convenient for a 1280 x 720 viewport, though any rectangular
   aspect ratio works and will be stretched to the requested viewport dimensions.
2. Secure the camera and paper so neither moves. Start the calibration command.
   Two windows appear: the camera preview and a simulated viewport grid. This grid
   is a test canvas, not the Java game. You do not need a projector.
3. In the CAMERA preview, click the corners as they appear on screen:
   **top-left, top-right, bottom-right, bottom-left**. This is the displayed
   (normally mirrored) image, not necessarily the labels as seen on the paper.
   Orange marks and edges show the selected area. Avoid extreme rotation; the
   target's top/bottom and left/right edges should remain clearly identifiable.
4. A valid fourth point enables mapping. Invalid, crossing, concave, duplicate,
   extremely small, off-image, or reversed-order layouts are rejected with an
   explanation. The invalid fourth point is removed so you can retry or undo.
5. Press **S** to save after checking the outline. Open your thumb/index to arm
   selection, move your fingertip across the rectangle, and pinch. A virtual
   cursor follows on the separate canvas. Both camera and viewport coordinates
   are displayed. Accepted pinches show SELECTED briefly and print one line:

   ```text
   SELECT screen_x=620 screen_y=340
   ```

6. Move outside the orange polygon: the preview and canvas say OUTSIDE, the virtual
   cursor disappears, and a pinch is explicitly ignored with a diagnostic instead
   of a SELECT. Boundary points are allowed. An ignored pinch is still consumed by
   Phase 2; release and pinch again inside rather than dragging a held pinch inward.
7. Verify each corner maps near its matching viewport corner and that the centre
   tracks plausibly. Test a held pinch, release/re-pinch, tracking loss, and Q as in
   Phase 2. Quit and reload, then check the cursor again before using the calibration.

Keep the camera preview at its automatic image size and visible on the desktop.
Instructions are appended below the image so its original pixel origin stays fixed;
clicks on that footer are ignored. Use 640 x 480 capture if a larger preview does not
fit your display. The simulated canvas can be resized for viewing without changing
its logical viewport coordinates. Closing either window or pressing Q quits.

### Controls and persistence

| Key | Action |
| --- | --- |
| U or Backspace | Undo last corner; disables mapping until four valid corners exist again. |
| R or C | Reset active calibration / recalibrate from scratch. Saved file stays unchanged. |
| S | Save the active valid calibration, replacing the configured JSON file. |
| L | Load the configured JSON file. Invalid/incompatible load disables mapping and explains why. |
| Q | Exit and release resources. |

Calibration edits/load/save disarm pinch input; open your fingers before selecting
again. Failed camera reads clear the cursor and disable corner clicks until a valid
frame returns. A change in actual capture resolution resets calibration immediately.
No SELECT events are emitted before valid calibration in simulation mode.

`calibration_config.json` is **generated when you press S**, next to `main.py` by
default. No pretend hardware calibration is shipped. It contains schema version,
UTC creation time, method, camera index, actual camera resolution, mirror flag,
displayed-image coordinate convention, corner order, four source points, target
viewport dimensions, and the 3 x 3 transformation matrix. Writes use a temporary
file followed by replacement to avoid half-written JSON. Loading rebuilds the
matrix from the saved points and rejects a matrix that does not agree, malformed
JSON, invalid geometry, unsupported schema/convention, or mismatched settings.

Reset clears active calibration only. To discard a saved calibration permanently,
remove that specific JSON file yourself or overwrite it with a new valid calibration
using S. A matching camera index does not identify a physical device permanently;
check your camera if Windows changes device enumeration.

### What the coordinates mean

- **Camera image coordinates:** pixel X/Y in the actual displayed webcam image,
  origin at its top-left. Mirroring happens before detection and clicking, so both
  use the same convention. Actual frame dimensions are used, not merely requested ones.
- **Calibrated viewport coordinates:** logical pixels of the selected target size.
  They are not Windows desktop mouse coordinates. TL maps to (0,0), TR to
  (width-1,0), BR to (width-1,height-1), BL to (0,height-1).
- **Physical tabletop coordinates:** real lengths and positions on a surface.
  This program does not measure centimetres, depth, or physical touch.

`cv2.getPerspectiveTransform()` constructs the homography (a perspective mapping)
from four point pairs. `cv2.perspectiveTransform()` maps each inside fingertip
position. Polygon containment is checked before mapping. Outside points are not
silently snapped onto the viewport. Floating-point mapping is retained for display;
SELECT coordinates are rounded to the nearest viewport pixel, with half-pixel
roundoff handled consistently. Event mapping uses the index coordinates from the
confirmed Phase 2 event, with the current raw fingertip also required to be inside.

References: [OpenCV transforms](https://docs.opencv.org/4.12.0/da/d54/group__imgproc__transform.html)
and [planar homography explanation](https://docs.opencv.org/4.10.0/d9/dab/tutorial_homography.html).

### Using a real projector later

Project a clearly outlined rectangle or the simulated grid onto a flat surface.
Keep its aspect ratio and intended viewport dimensions consistent. Lock the camera,
projector position, zoom/keystone settings, and surface. Run fresh calibration and
click the four corners of the **visible projected interaction rectangle** in the
camera preview; save it under a new calibration filename if retaining the paper
setup. Check corner and interior alignment. If default mirroring makes physical
left/right confusing, use `--no-mirror` and recalibrate; never reuse a mirrored file
with the opposite setting. Software maps the displayed TL/TR/BR/BL order, not printed
labels. This stage does not place or control a Java window on a projector. Future
integration must use the same game content viewport, not window borders or desktop offsets.

Recalibrate whenever the camera, target, projector geometry, crop, resolution, or
mirror convention changes. The program can detect metadata mismatches, but cannot
detect an unnoticed physical movement or camera zoom change.

### Accuracy and limitations

A homography maps points on a common plane. A fingertip above the paper/tabletop
can have substantial **parallax error**, especially with an angled camera. Keep the
finger close to the plane and use a more perpendicular camera view for better
approximation. This is pointing plus pinch confirmation, **not physical touch detection**.
Lens distortion is not corrected. Corner-click error, depth, lighting, occlusion,
extreme perspective, and MediaPipe jitter limit accuracy. Four corners can align
while interior points are still inaccurate due to these effects. The displayed
READY/PINCHED status remains the gesture state, independent of whether a location
is permitted for selection. Viewports are limited to 2–4096 pixels per dimension;
a larger canvas increases memory/rendering cost and can lower FPS.

### Phase 3 files and test results

Created: `coordinate_mapper.py`, `calibration.py`, `projection_preview.py`,
`tests/test_calibration.py`. Modified: `main.py`, `config.py`, `README.md`.
The calibration JSON is created by your first successful S save. No dependencies,
Phase 1 camera/tracker modules, Phase 2 detector, or Java files were modified.

**62 tests passed, zero skipped** in the existing Python 3.12 environment: all 44
Phase 1/2 tests plus 18 calibration tests. Tests cover identity, scaling, perspective
and four-corner correspondences, invalid layouts, outside/boundary checks, JSON
round-trip/tampering/mismatch rejection, mirrored conventions, undo/reset/load,
failed-frame click protection, resolution changes, converted pinch events, and the
complete simulated main loop with fake camera/landmark inputs. Real blank-frame
MediaPipe inference also passed; its existing native/deprecation warnings remain.
An initial test exposed half-pixel rounding sensitivity; it was corrected and the
full suite rerun successfully.

No physical paper, projector, or live-camera Phase 3 accuracy testing was performed
by the assistant. Run the paper checklist above on your setup. Phase 3 is complete;
no Java connection, Arduino functionality, or Phase 4 work was started.
