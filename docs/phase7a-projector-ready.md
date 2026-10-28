# Phase 7A — projector-ready software preparation

This phase adds an **optional resizable presentation window**. No projector or external webcam was connected or tested. Existing laptop/built-in-camera practice is sufficient for the software workflow below; it does not validate desk projection or optical calibration.

## What changed and why

The original `Board` creates a fixed 814×537 outer frame and positions panels in an 800×500 content layout. Merely enabling resize would leave those panels and images at their original sizes. `--presentation` installs a layout adapter after the original Board is constructed: it scales top-level panel bounds, icon painting and fonts while keeping the same Swing buttons, action listeners and mouse dispatch. Artwork stretches to fill the content area; fonts scale uniformly by the smaller factor. Use 1000×625 (8:5) for an undistorted version of the original layout. Raster artwork will not acquire more detail when enlarged.

The normal launch without presentation/window-size options retains the existing fixed window. Gameplay, TCP protocol, Python production code, Arduino serial controller/firmware and Phase 6A electrical documents were not changed. Icon wrappers preserve their original filename strings because existing LED feedback compares those strings to detect a changed card.

Presentation mode supports resizing and Windows **Maximize/Restore**. It deliberately uses a normal decorated window, not exclusive or borderless fullscreen. This is sufficient to move the game onto an extended display without changing resolution, hiding exit controls or recreating the serial-connected window. There is no F11/fullscreen shortcut.

## Build and launch on this laptop

Open PowerShell in the repository root. These commands use JDK 17 and the existing pinned jSerialComm 2.11.4 dependency; the build downloads/verifies that JAR if missing.

```powershell
Set-Location 'C:\path\to\Interactive-Projection-Guess-Who'
$env:Path = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin;' + $env:Path
.\build-phase7a.ps1 -Test
java -cp 'build-phase5b2;arduino\java\lib\jSerialComm-2.11.4.jar' groupDGuessWho.Main --presentation --window-width 1000 --window-height 625 --viewport-width 1280 --viewport-height 720 --viewport-fit stretch
```

The output folder remains `build-phase5b2`, reused by the Phase 7A build. Keep the working directory at the project root so original image/audio assets resolve. Close other game instances using port 5050 before launching. `CONTROL disabled` means gestures cannot reach that instance, even if mouse play works.

Optional existing LED feedback, if the Mega is connected and its correct port is COM5:

```powershell
java -cp 'build-phase5b2;arduino\java\lib\jSerialComm-2.11.4.jar' groupDGuessWho.Main --presentation --window-width 1000 --window-height 625 --viewport-fit stretch --arduino-port COM5
```

Replace COM5 with the actual port. Close Arduino Serial Monitor/Plotter first. Omitting `--arduino-port` opens no serial port. The game still sends BLINK only; BUZZ firmware and the untested transistor buzzer design remain unchanged. Do not energize the unverified buzzer circuit for these tests.

## Practice without purchasing equipment

### A. No camera required

1. Run the Java command above. Advance through the title/instructions using the mouse and reach the character board.
2. Resize by dragging the window edge, then Maximize and Restore. Panels, card artwork and text should scale. Verify the 24 cards, Continue, questions and menus remain usable. The minimum content size is 800×500.
3. In another PowerShell window at the root, inspect live coordinates:

   ```powershell
   .\computer-vision\.venv\Scripts\python.exe .\test_java_connection.py --info
   ```

4. Pick a visible enabled control and copy its reported **x/y**, not `java_x/java_y`. Send a test action (this really clicks that control):

   ```powershell
   # Example only: replace these coordinates with the current INFO result.
   .\computer-vision\.venv\Scripts\python.exe .\test_java_connection.py --x 220 --y 215
   ```

5. After each resize or game-state change, query INFO again before choosing another control. Expect `executed: true` for an eligible control and no action for background/disabled controls. Do not reuse coordinates from another screen or round.

The automated presentation and real-game tests also run without camera, projector or physical Arduino. They simulate serial feedback and use a local TCP connection.

### B. Built-in webcam gesture practice

Leave Java running. In a second terminal at the repository root:

```powershell
.\computer-vision\.venv\Scripts\python.exe .\computer-vision\main.py --camera 0 --width 640 --height 480 --connect-java --calibrate --calibration-file .\computer-vision\calibration-laptop-practice.json --viewport-width 1280 --viewport-height 720 --java-fit stretch
```

Use the existing Python 3.12 environment and model from earlier phases. If this environment is unavailable, follow `computer-vision/README.md`; do not install a second OpenCV distribution alongside the pinned contrib package.

For a laptop-facing camera, choose a comfortable rectangle **inside the camera image** where your hand can move. Click its top-left, top-right, bottom-right and bottom-left corners in that order. This is a virtual input surface: you do not have to point the built-in camera at its own screen or at the desk. Do not click the instruction footer. The calibration window preserves camera image pixels; leave its image sizing alone.

Open your fingers to arm, move the index fingertip around the rectangle, then pinch/release over the corresponding virtual button location. Use the simulated viewport cursor and Java ACK text as feedback. Keep Java visible beside the previews or use Alt+Tab. Only eligible card changes cause LED feedback when enabled; not every menu action blinks.

Press **S** with the OpenCV preview focused to save this separate practice calibration. **C/R** resets and disables selection until four new corners are supplied; **U** undoes a corner; **Q** quits the camera application. The practice file does not replace the original calibration file. The built-in webcam test is a **manual step for the user**, not a test performed by the assistant.

### Coordinate mapping after resize

The chain remains: displayed-camera pixels → calibrated logical viewport → Java's current client/content coordinates → actual visible Swing button. The Java adapter recalculates the mapping on every INFO/SELECT using the live content width/height, excluding window borders and title bar. Screen position and a secondary display's desktop origin are not added to the TCP coordinates. Keep both sides at logical 1280×720 and **stretch** for this workflow; this resolution is not a request to change the monitor resolution. `contain` has different margin semantics and is not used in this guide.

In laptop virtual-input practice, resizing Java changes the rendering size but the calibrated hand rectangle can remain the same virtual input surface. Test alignment again after resizing. For **physical projection**, resizing/moving/maximizing the game changes the image on the desk: stop Python first, arrange the final window, then start with `--calibrate` again. A logical coordinate transform cannot infer that the real projected corners moved. Do the same after moving the camera/projector, changing focus/zoom/keystone, changing Windows resolution/scaling or changing mirror mode. Do not resize during live physical interaction.

## Future HDMI projector: Windows 11 Extend mode

1. Connect the projector to the laptop's HDMI-capable output and select the matching HDMI input on the projector. Put it on a suitable stable support; this guide does not validate a mount or optical arrangement.
2. Press **Win+P → Extend**. Open **Settings → System → Display**, use **Identify**, and arrange the display rectangles to match your physical layout. If absent, check the connection/input and use **Multiple displays → Detect**. Set an appropriate supported resolution and Windows scale before calibration. [Microsoft's multi-display guide](https://support.microsoft.com/en-us/windows/hardware/display-graphics/how-to-use-multiple-monitors-in-windows).
3. Keep the Python camera/calibration windows and terminals on the laptop. Move only the Java game onto the projector, then maximize it using the title-bar button. Keep the title bar/taskbar outside the four calibrated **game-content** corners.
4. You can drag the game to the other display, or list Java's detected devices before launch:

   ```powershell
   java -cp 'build-phase5b2;arduino\java\lib\jSerialComm-2.11.4.jar' groupDGuessWho.Main --list-displays
   ```

   Then use a displayed zero-based index, for example **only if Display 1 exists**:

   ```powershell
   java -cp 'build-phase5b2;arduino\java\lib\jSerialComm-2.11.4.jar' groupDGuessWho.Main --presentation --display 1 --window-width 1000 --window-height 625 --viewport-fit stretch
   ```

   Java indices may differ from Windows Identify numbers and can change on reconnect. An unavailable index reports an error without opening the game. On the generation/test machine, Java enumerated only Display 0 at 1280×800; no secondary-display test was performed.
5. Adjust the final projected image, then run fresh camera calibration using the actual projected client-area corners visible in the camera image. Follow the established TL→TR→BR→BL convention in the displayed camera view. Verify corner orientation with the simulated cursor before sending gestures; if mirrored unexpectedly, restart with the appropriate mirror setting and recalibrate rather than reusing old points.
6. Test cursor alignment near all four corners, then a few central buttons, then mouse and pinch interactions. Check title/instructions, all three card rows, questions, Continue and result screens. Test any difference in Windows DPI scale between displays manually; this was not exercised with real mixed-DPI monitors.

An external projector cannot be emulated by enabling Extend on a single unconnected display. Laptop practice verifies software flow, not throw distance, visibility, shadows, latency, optical distortion or real display-driver behavior.

## Future external webcam selection and recalibration

`--camera` already accepts an OpenCV device index. Index 0 is commonly built-in, but indices are not stable identities. Close other camera applications and quit the current tracker before trying another index. First preview a candidate without Java control:

```powershell
.\computer-vision\.venv\Scripts\python.exe .\computer-vision\main.py --camera 1 --width 640 --height 480
```

Verify the actual image is from the intended camera, press Q, then start with a **new calibration file**:

```powershell
.\computer-vision\.venv\Scripts\python.exe .\computer-vision\main.py --camera 1 --width 640 --height 480 --connect-java --calibrate --calibration-file .\computer-vision\calibration-projector-external.json --viewport-width 1280 --viewport-height 720 --java-fit stretch
```

Replace 1 if required. Requested resolution may differ from the camera's delivered resolution. The existing program invalidates calibration when actual frame dimensions change; saved calibration also checks camera index, mirror setting and logical viewport. Two different cameras can reuse the same index and resolution, so those checks cannot identify the physical device: always recalibrate after replacing/reconnecting/repositioning equipment. Hot-switching cameras during a run is not implemented; stop and restart. A missing camera reports an error rather than inventing a connection.

## Tests actually performed

On 2026-10-28:

- JDK 17 compilation passed with pinned jSerialComm 2.11.4.
- 20 hardware-free serial-controller checks passed.
- Existing Java mapping/strict-JSON and Swing visibility/disabled/covered-control checks passed.
- 19 existing real-Board/fake-Arduino checks passed in normal mode; 22 passed in the final presentation-mode rerun, including all 24 cards reachable at 1200×675, 1024×768 and 1000×625.
- 32 new presentation checks passed: live coordinate hit testing, mouse event dispatch, icon/font scaling and filename identity, background rejection, logical viewport stability, across 800×500, 1280×720, 1024×768 and 1000×625 content sizes.
- 73 existing Python vision/calibration/client tests passed, including real MediaPipe inference on blank frames (no camera), camera failure handling and calibration compatibility checks.
- 16 Python-to-real-Java TCP protocol tests passed.
- Opened and inspected `tmp/phase7a/presentation-game.png`, generated by the real Swing game at 1000×625. This is an application render, not projector imagery.
- Display enumeration reported one display. No real secondary display, built-in/external camera stream, physical Arduino feedback, audible buzzer or real projector test was performed in Phase 7A. Maximize/restore and mixed-DPI monitor transitions remain manual checks.

Initial sandbox loopback permission errors were resolved by an approved local test run. Python temporary-directory permission errors were resolved using the project-local `.test-temp` directory. An initial Python test invocation from the wrong directory was corrected. Final logs: `phase7a-java-test-results.txt`, `phase7a-presentation-regression-results.txt`, `phase7a-python-test-results.txt`, `phase7a-protocol-test-results.txt`.

Repeat Python checks from the correct folder:

```powershell
Push-Location .\computer-vision
$env:TEMP = (Join-Path (Get-Location) '.test-temp')
$env:TMP = $env:TEMP
New-Item -ItemType Directory -Force $env:TEMP | Out-Null
.\.venv\Scripts\python.exe -m unittest discover -s tests -v
Pop-Location
$env:JAVA_TEST_CLASSPATH = 'build-phase5b2;arduino\java\lib\jSerialComm-2.11.4.jar'
.\computer-vision\.venv\Scripts\python.exe -m unittest discover -s tests -v
```

## Changes and remaining manual checklist

Modified: `src/groupDGuessWho/Main.java`, `tests/java/groupDGuessWho/ArduinoFeedbackTest.java`.
Added: `src/groupDGuessWho/PresentationLayout.java`, `tests/java/groupDGuessWho/PresentationTest.java`, `build-phase7a.ps1`, this guide, the four test logs and the generated Swing preview. Build outputs are regenerated in the existing build folders. No existing Phase 6A documentation or Arduino sketch was edited.

- On the laptop: verify resizing/maximize/restore, every game screen, mouse controls and builtin-camera virtual pinches.
- If using the existing Mega: close Serial Monitor, choose the correct COM port and confirm one LED feedback sequence per eligible card action; verify the game still works without the board.
- Later: test real Extend mode, monitor/DPI placement, external camera selection and fresh physical projection calibration.
- Keep the untested buzzer design unpowered. Its status remains **DESIGNED — HARDWARE VALIDATION PENDING**.

Stop at Phase 7A. No automatic buzzer feedback, WS2812B functionality or additional phase is implemented.
