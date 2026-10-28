# Phase 4C — Live webcam control

The existing pinch detector now optionally sends calibrated SELECT messages to the existing Java server. No production Java source, graphics, sounds, mouse controls, or gameplay were changed. No new Python dependencies are required. Phase 5 and Arduino are not included.

## Launch Java (PowerShell terminal 1)

Close any previous game using port 5050 first. Run from the project root so the original image/audio paths resolve:

```powershell
Set-Location 'C:\path\to\Interactive-Projection-Guess-Who'
$jdk17 = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
& "$jdk17\bin\javac.exe" -encoding UTF-8 --release 17 -d build-phase4b src\groupDGuessWho\*.java
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
& "$jdk17\bin\java.exe" -cp build-phase4b groupDGuessWho.Main --control-port 5050 --viewport-width 1280 --viewport-height 720 --viewport-fit stretch
```

Confirm Java reports a listener on 127.0.0.1:5050. A `CONTROL disabled` message means mouse play still works but this window cannot receive selections.

## Start the webcam (terminal 2)

Use the existing Python 3.12 environment from Phases 1–3; activation is unnecessary:

```powershell
Set-Location 'C:\path\to\Interactive-Projection-Guess-Who'
.\computer-vision\.venv\Scripts\python.exe .\computer-vision\main.py --connect-java --camera 0 --width 640 --height 480
```

This automatically loads `computer-vision/calibration_config.json`. Use the same camera index, actual camera resolution, mirror setting, and virtual viewport as your saved calibration. Add `--no-mirror` only if that was used when calibrating. A missing or incompatible file leaves selection disabled; the warning appears in the preview. A different file can be selected with `--calibration-file 'path\to\file.json'`.

To create a fresh calibration instead:

```powershell
.\computer-vision\.venv\Scripts\python.exe .\computer-vision\main.py --connect-java --calibrate --camera 0 --width 640 --height 480
```

Place your four-point paper in view. Click the displayed paper corners in this order: top-left, top-right, bottom-right, bottom-left. Mirroring means these are the corners as shown in the preview. Keep the camera and paper still. Press **S** to save, **L** to load, **U** to undo, or **R/C** to reset. New calibration becomes active after the fourth valid corner; keep fingers open while setting it up. Press **Q** in a preview window to quit cleanly. Saving replaces the chosen calibration file only when you press S.

Without Java control, all previous modes remain available:

```powershell
.\computer-vision\.venv\Scripts\python.exe .\computer-vision\main.py
.\computer-vision\.venv\Scripts\python.exe .\computer-vision\main.py --load-calibration
```

Use `--java-port 5051` if Java was launched with `--control-port 5051`. `--java-timeout 1` sets the socket timeout in seconds. If Java uses `--viewport-fit contain`, Python must use `--java-fit contain`; the default for both commands above is stretch.

## Find and test a real button without a projector

In a third terminal, from the project root:

```powershell
.\computer-vision\.venv\Scripts\python.exe .\test_java_connection.py --info
```

INFO is read-only. Look in `controls` for a currently visible control and its viewport **x/y** coordinates. Do not use `java_x/java_y`, which are internal GUI coordinates. Requery after the screen changes, a control moves, or a new round starts. Choose a harmless control you want to test; do not assume an arbitrary paper location is clickable.

1. Keep the Java game visible alongside the previews. With open fingers, move the index tip over the paper until the preview's **Java viewport X/Y** approximately matches that control's INFO x/y.
2. Bring thumb and index together steadily. Expect one local SELECTED indication, then `sent; awaiting ACK`, then `ACK executed; executed=True` and the control name. The game should respond using its existing button behavior.
3. Hold the pinch: no second action should occur. Open your fingers, then pinch again to select again.
4. If the reply says `no_control`, requery INFO and adjust your finger position. `unused_margin` means a letterbox area, not a button. Local SELECTED only means a valid calibrated gesture; it does not prove the game executed it.

For an independent communication check, substitute a current INFO coordinate in this command (the example numbers are not a guaranteed button):

```powershell
.\computer-vision\.venv\Scripts\python.exe .\test_java_connection.py --x 620 --y 340
```

For a rough paper position with the default 1280×720 viewport, x/1279 is the fraction from left to right and y/719 the fraction from top to bottom. Perspective can make this estimate inaccurate, so use the live coordinate readout to finish alignment. The paper represents the whole virtual viewport, not the laptop screen or window title bar. No projector is required.

## How it works and what the status means

The existing detector still handles hysteresis, debounce, cooldown, and one event per held pinch. On a selection, the saved homography converts the event's camera-image fingertip coordinates to the calibrated viewport. Both the raw fingertip and selected point must be inside. Missing calibration or incompatible camera metadata prevents sending.

A background worker reads the existing INFO response. If Java's viewport dimensions differ, each axis is converted with `javaCoordinate = calibratedCoordinate * (javaDimension - 1) / (calibratedDimension - 1)`. The corner pixels therefore match exactly. Java's unchanged adapter then maps into its current Swing content area, handles stretch/contain margins, and invokes the current eligible button on the EDT. No button positions are hardcoded and no OS mouse automation is used.

The wire message remains newline-delimited JSON: `{"type":"SELECT","x":620,"y":340,"id":"unique-uuid"}`. The standalone client and live client share `java_protocol.py`. Only 127.0.0.1 is used. The worker keeps a connection open, checks INFO periodically, and reconnects after errors. Network waits never run in the frame loop.

Only one selection may be pending or awaiting acknowledgement. Busy/disconnected clicks are dropped. Unsent events older than 250 ms are dropped; tracking loss or calibration changes cancel pending events. Reconnection requires opening the fingers before another pinch. Every accepted submission gets a new UUID; an uncertain event is never resent automatically.

`sent` confirms bytes were sent; `ACK executed=True` confirms Java's existing listeners returned successfully. A lost ACK means execution is unknown, because an already-transmitted action cannot be withdrawn. A game listener can legitimately produce no visible change in its current state even when it returns successfully. Connection and latest event state are shown separately.

## Automated verification

Run CV tests from their directory:

```powershell
Set-Location 'C:\path\to\Interactive-Projection-Guess-Who\computer-vision'
.\.venv\Scripts\python.exe -m unittest discover -s tests -v
```

Run Java/protocol tests from the root (a small Swing fixture briefly opens):

```powershell
Set-Location 'C:\path\to\Interactive-Projection-Guess-Who'
$jdk17 = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
& "$jdk17\bin\javac.exe" -encoding UTF-8 --release 17 -d build-phase4b-tests src\groupDGuessWho\*.java tests\java\groupDGuessWho\*.java
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
$env:JAVA17_EXE = "$jdk17\bin\java.exe"
.\computer-vision\.venv\Scripts\python.exe -m unittest discover -s tests -v
```

Actual results on 2026-10-26:

- JDK 17 compilation of all production sources plus the test harness passed.
- 73 CV tests passed: all 62 previous tests and 11 new live-control tests. Includes synthetic full frame-loop routing and cleanup, normalized mapping, unique IDs, missing/invalid calibration, outside points, missing server/reconnection, lost-ACK no replay, busy/context cancellation, expired events, held-pinch prevention, and CLI defaults. MediaPipe blank-frame inference also passed; its existing diagnostic warnings remain.
- 16 Java/protocol tests passed: all 15 previous tests plus the new background client talking to the real Java localhost test server. Includes malformed input, duplicates, disconnects, mapping, and a real Swing fixture checking button activation on the EDT.
- Initial sandbox runs encountered temporary-directory permissions and blocked sockets. CV tests passed with TEMP/TMP directed to a workspace test directory; protocol tests passed with approved execution outside the socket-restricted sandbox. A timing-sensitive new test was changed to inject an explicitly old event instead of depending on sub-millisecond clock resolution.
- No physical hand/webcam-to-game or projector test was performed for Phase 4C. The Java network fixture is not the original game's physical interaction test.

## Remaining manual checks and limitations

Test the real button procedure above; hold/release/repeat; move outside the paper; remove/reintroduce your hand; restart Java while holding a pinch; confirm no stale action occurs and an open hand rearms selection; check normal mouse controls; reset/reload calibration; and press Q. Requery INFO after resizing/layout changes. Check audio/animations in normal play.

Calibration cannot detect a physically moved camera or paper: recalibrate whenever either moves. The mapping assumes a flat interaction plane, so a finger held far above the paper can shift the apparent position. Tracking noise and lighting still affect precision. Slow game listeners can exceed the default ACK timeout; the preview stays responsive but reports an uncertain outcome without retrying. The unchanged Java server retains at most 10,000 event IDs per run; restart it if it reports capacity reached. Existing gameplay behavior and its limitations are unchanged.

## Files in this phase

New: `java_protocol.py`, `computer-vision/java_connection.py`, `computer-vision/tests/test_java_live.py`, and this document.

Modified: `test_java_connection.py` (shared transport), `computer-vision/main.py`, `computer-vision/config.py`, `computer-vision/projection_preview.py` (extra status lines), `computer-vision/README.md`, `tests/test_java_protocol.py`, and `tests/java/groupDGuessWho/ControlTestHarness.java` (test-only INFO metadata).

Generated verification artifacts: `computer-vision/phase4c-test-results.txt`, an empty `computer-vision/.test-temp/` directory, updated `build-phase4b-tests/` class files, and Python bytecode caches. Existing saved calibration was not overwritten.
