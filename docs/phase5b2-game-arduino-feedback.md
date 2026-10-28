# Phase 5B-2 — automatic Arduino feedback from Guess Who

This phase connects the existing game to the **ELEGOO Mega 2560** using the existing `phase5b1.ArduinoSerialController` and **jSerialComm 2.11.4**. It uses the existing firmware at **9600 baud, 8 data bits, no parity, one stop bit**, with no flow control. The onboard L LED (`LED_BUILTIN`, D13) blinks three times per completed feedback command.

## Files created and modified

Modified:

- `src/groupDGuessWho/Main.java`: optional `--arduino-port` setting and application startup/shutdown lifecycle.
- `src/groupDGuessWho/Board.java`: small callback after the existing character-card action completes and changes the card state. Original action bodies are preserved.

Created:

- `src/groupDGuessWho/ArduinoFeedback.java`: single background worker, bounded request queue, logging, cleanup, and adapter to the existing controller.
- `tests/java/groupDGuessWho/ArduinoFeedbackTest.java`: fake-Arduino tests, including the actual Board and TCP/Swing selection route.
- `build-phase5b2.ps1`: reproducible Java 17 build and Java tests; reuses the Phase 5B-1 dependency downloader/checksum and tests.
- `docs/phase5b2-game-arduino-feedback.md`: this guide.

Generated locally: `build-phase5b2/` class files, `phase5b2-test-results.txt`, `phase5b2-feedback-test-results.txt` (final feedback-fixture rerun), and `phase5b2-protocol-test-results.txt`. The existing Phase 5B-1 build/test outputs are regenerated too.

The Arduino firmware, existing serial controller/CLI/build script, `ControlServer.java`, `SwingSelectionController.java`, and Python/OpenCV sources are unchanged. Images, sounds, GUI layout, and game rules are unchanged. No Phase 5C work is included.

## Which actions blink?

The hook lives at the end of the **existing character-card branch of `Board.actionPerformed`**, after the original code returns successfully. It checks that the card was visible/enabled and that its image state changed. The existing game uses image filenames to distinguish default, excluded, and targeted states.

- Clicking a card to exclude it or restore it triggers one feedback request. Clicking again to reverse exclusion is a **new valid action**, not a duplicate.
- Selecting a card as a guess triggers feedback when it becomes targeted, whether the guess is correct or incorrect. Incorrect guessing is still a successfully handled game action.
- Selecting a card in the existing AI retrace/reveal flow triggers feedback when that action is allowed and changes its target state.
- Re-selecting a card that is already targeted produces no new feedback.
- Background clicks, disabled/hidden controls, ignored retrace actions, INFO requests, rejected coordinates, and duplicate TCP event IDs produce no feedback.
- Menu, question, continue, and other non-card buttons do not blink in this implementation.

Mouse input invokes the original button listeners. Webcam input passes through the unchanged TCP server and `SwingSelectionController`, which invokes those same listeners with `doClick`. There is no Arduino trigger in the TCP message-receipt code, and no second listener that could double-count a selection.

## Preparation

1. Leave the working Phase 5A sketch uploaded. No firmware update is needed.
2. Connect the Mega using its USB data cable. No external electronics are needed.
3. **Close Arduino IDE Serial Monitor/Serial Plotter and stop the Phase 5B-1 CLI tester.** Only the game should own the serial port. Also close any other game instance using COM5 or TCP port 5050.
4. Open PowerShell. Run from the project root so the original relative image/audio paths work.
5. Check `java -version` and `javac -version` both resolve to JDK 17. The commands below use the installed JDK path explicitly; change it if your installation moves.

## Compile and launch Java (terminal 1)

Recommended build, including hardware-free tests (test game windows briefly appear and may play the existing sounds):

```powershell
Set-Location 'C:\path\to\Interactive-Projection-Guess-Who'
.\build-phase5b2.ps1 -Test
```

For a build without running tests, omit `-Test`. The script uses `java` and `javac` on PATH. It calls the existing Phase 5B-1 script to download/check the exact JAR if necessary. The pinned JAR and SHA-256 are documented in [Phase 5B-1](phase5b1-java-arduino-test.md).

The complete manual equivalent for dependency setup and game compilation is below. Use this if script execution is blocked by PowerShell; no execution-policy change is required:

```powershell
Set-Location 'C:\path\to\Interactive-Projection-Guess-Who'
$ErrorActionPreference = 'Stop'
$jdk17 = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
$serialJar = 'arduino\java\lib\jSerialComm-2.11.4.jar'
New-Item -ItemType Directory -Force 'arduino\java\lib', 'build-phase5b2' | Out-Null
if (-not (Test-Path -LiteralPath $serialJar)) {
    Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/com/fazecast/jSerialComm/2.11.4/jSerialComm-2.11.4.jar' -OutFile $serialJar
}
if ((Get-FileHash -LiteralPath $serialJar -Algorithm SHA256).Hash -ne 'B5F7EC40D622864B92DC679159CDBAA3030D37884F0AF2DEEF45B56C16BDA7A6') {
    throw 'jSerialComm checksum mismatch. Remove that JAR and download again.'
}
& "$jdk17\bin\javac.exe" -encoding UTF-8 --release 17 -cp "$serialJar" -d build-phase5b2 src\groupDGuessWho\*.java arduino\java\src\phase5b1\ArduinoSerialController.java
if ($LASTEXITCODE -ne 0) { throw 'Game compilation failed' }
```

Launch with Arduino on COM5 and the existing gesture server:

```powershell
$jdk17 = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
& "$jdk17\bin\java.exe" -cp 'build-phase5b2;arduino\java\lib\jSerialComm-2.11.4.jar' groupDGuessWho.Main --arduino-port COM5 --control-port 5050 --viewport-width 1280 --viewport-height 720 --viewport-fit stretch
```

COM5 is configurable: replace it with the board's current Device Manager port. Windows classpath entries use **semicolons**, inside quotes. The old Phase 4B **compilation command is insufficient** for these updated sources; compile the existing serial controller too and include the JAR. Use the new runtime classpath shown here.

To play without Arduino support, omit `--arduino-port`:

```powershell
& "$jdk17\bin\java.exe" -cp 'build-phase5b2;arduino\java\lib\jSerialComm-2.11.4.jar' groupDGuessWho.Main --control-port 5050 --viewport-width 1280 --viewport-height 720 --viewport-fit stretch
```

The game also remains playable if you enable an absent or busy port: it logs an error and continues without LED feedback. `--no-control` still disables the gesture TCP server independently of Arduino support.

## Start existing webcam program (terminal 2)

```powershell
Set-Location 'C:\path\to\Interactive-Projection-Guess-Who'
.\computer-vision\.venv\Scripts\python.exe .\computer-vision\main.py --connect-java --camera 0 --width 640 --height 480
```

This uses your existing saved calibration. Keep the same camera, resolution, mirror mode, viewport, and paper position used during calibration. If needed, follow [Phase 4C calibration instructions](phase4c-live-control.md). To calibrate explicitly:

```powershell
.\computer-vision\.venv\Scripts\python.exe .\computer-vision\main.py --connect-java --calibrate --camera 0 --width 640 --height 480
```

The Python process talks only to Java over localhost TCP; it never opens COM5. No Python changes or new Python dependencies are required.

## Physical verification: perform these checks yourself

1. Launch Java with `--arduino-port COM5`. The game window should remain responsive during connection. Wait for `ARDUINO connected ... response received: PONG`. The existing controller waits 2.5 seconds for reset, then allows up to five seconds for a reply. Bootloader flicker during initial reset is normal and is separate from interaction feedback.
2. Also confirm `CONTROL listening on 127.0.0.1:5050`. If CONTROL is disabled, mouse play still works, but resolve the port conflict before testing webcam input.
3. Advance through the existing title/instruction screens until character cards appear. Menu clicks should not trigger a BLINK command.
4. Click one visible card with the mouse. It should perform its existing exclusion/selection action immediately. Observe one three-flash sequence on the onboard **L** LED. The power LED is independent.
5. Check the console sequence: `GAME action completed`, `ARDUINO sending BLINK`, then `ARDUINO BLINK command sent; response received: ACK BLINK`. The acknowledgement arrives after approximately 1.75 seconds. Continue interacting with the game during the sequence to confirm the GUI is responsive.
6. Click an empty region and verify there is no new action/BLINK log. In the game's guessing mode, select a card, then click that same already-targeted card again: the second click should not produce new feedback. In ordinary exclusion mode, clicking twice changes the card twice, so two feedback requests are expected.
7. Start the Python webcam program. For exact current control positions, run in a third terminal:

   ```powershell
   Set-Location 'C:\path\to\Interactive-Projection-Guess-Who'
   .\computer-vision\.venv\Scripts\python.exe .\test_java_connection.py --info
   ```

   Use a visible `character[...]` control's viewport `x`/`y`, not its `java_x`/`java_y`. Move your open hand so the calibrated preview coordinates align with that card, then pinch. Expect the normal card change, a TCP ACK, one game-action log, and one three-flash sequence. Hold the pinch: no second gesture action should occur. Open your fingers before selecting again.
8. Test offline play: close the game, disconnect USB, and relaunch the same Java command. It should log Arduino unavailable while mouse/gesture gameplay continues normally.
9. Test loss during play: reconnect and restart the game, wait for PONG, then unplug the Mega. Make a valid card action. Gameplay should still change immediately. Expect a serial error/unavailable message, usually immediately or within the existing five-second command timeout. Queued feedback is discarded. Reconnecting USB does not automatically reconnect this session; restart the game.
10. Stop safely: press **Q** in the webcam preview, then close the game window. The background worker is interrupted, queued feedback is discarded, and the serial port is closed. The JVM shutdown hook waits off the EDT for cleanup. Ctrl+C also invokes shutdown cleanup. Do not force-kill unless necessary. After Java exits, reopen Arduino Serial Monitor and send PING to confirm port release; close it before launching Java again.

## Concurrency and limitations

- The game opens one connection per enabled run. The existing controller owns all low-level serial framing, reset handling, timeouts, and validation. `ArduinoFeedback` only schedules calls to it.
- Connection, BLINK transmission, reply waiting, and port closing run on a dedicated worker, never on Swing's Event Dispatch Thread. The EDT only submits a request; TCP ACKs do not wait for the Arduino animation.
- The queue holds at most **16 pending requests**, plus one in flight. Each valid action normally gets one BLINK. Sustained fast interaction can build a backlog (roughly 1.75 seconds per animation); further requests are dropped with a queue-full log. Gameplay is never delayed to wait for queue space.
- Actions while connecting/unavailable are skipped, not replayed later. Errors clear pending requests. There are no automatic serial retries or reconnects, preventing accidental duplicate animations or repeated board resets. Restart the game to reconnect.
- There is no idle heartbeat. An unplug is noticed on the next BLINK; an idle disconnected board may remain logged as connected until then.
- The existing controller and jSerialComm retain normal exclusive port access. A second game/CLI or Serial Monitor cannot share the same port; the failed opener reports unavailable and does not interfere with gameplay.
- Shutdown can interrupt an in-flight acknowledgement wait. The Arduino may finish an already-received BLINK after Java closes; closing a port cannot retract a transmitted command.
- Feedback tracks the existing default/excluded/target image transitions. It does not change or add game rules, and it is not an indication that a guess was correct. A future redesign of card rendering would require revisiting this small hook.

## Automated validation

Results from this implementation:

- Java 17 production and test compilation passed with jSerialComm 2.11.4.
- **20 existing Phase 5B-1 controller checks passed.**
- Existing `ControlTestHarness --self-test` and `--swing-test` passed (mapping, strict JSON, real Swing hit testing, disabled/hidden/covered controls, and EDT routing).
- **19 Phase 5B-2 checks passed**, using a fake Arduino and the actual Board/Swing/TCP path. Covers one request per action, shared mouse/webcam handler, actual simulated mouse events, duplicate IDs, invalid coordinates, background clicks, INFO, disabled cards, rejected retrace selection, already-targeted cards, failed feedback isolation, missing hardware, serial errors, one connection per run, startup-event skipping, cleanup, and EDT responsiveness while the fake serial operation is blocked.
- **All 16 existing Python-to-Java protocol tests passed** against the new compiled classes, including the background gesture client and duplicate-ID handling.
- Compared the existing source snapshots: only `Main.java` and `Board.java` were modified. The existing serial controller, firmware, and Python sources are unchanged.
- The first integration-test attempt was blocked by sandbox localhost socket permissions; rerunning with permitted localhost access passed. All serial connections in the Phase 5B-2 integration tests were fake. **No physical Arduino, webcam/pinch, or projector validation was performed during this implementation.** Your prior successful hardware tests were Phase 5B-1; perform the integrated manual checks above.

To reproduce Java tests, run `./build-phase5b2.ps1 -Test`. Then run existing protocol tests from the project root:

```powershell
$env:JAVA17_EXE = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe'
$env:JAVA_TEST_CLASSPATH = 'build-phase5b2;arduino\java\lib\jSerialComm-2.11.4.jar'
.\computer-vision\.venv\Scripts\python.exe -m unittest discover -s tests -v
```

See `phase5b2-test-results.txt` and `phase5b2-protocol-test-results.txt` for recorded output. Test windows briefly open and close; run tests separately from live gameplay.

## Troubleshooting

| Symptom | Action |
| --- | --- |
| Cannot open COM5 | Close Serial Monitor/Plotter, standalone CLI, or another game instance. Check current COM port in Device Manager. |
| PONG/BLINK timeout | Verify the working Phase 5A sketch and USB data cable. Stop Java before checking with Serial Monitor. Restart Java after fixing the issue. |
| No LED but game works | Check `--arduino-port`, wait for connected/PONG, and use a card action that changes state. Inspect unavailable or queue-full logs. |
| Serial error after unplugging | Expected graceful degradation. Reconnect and restart Java; the game does not retry automatically. |
| Gesture changes game but no immediate LED | Wait for queued animations, check serial status, and check whether the card action was a no-op. TCP `executed=true` means listeners returned, not necessarily a card state change. |
| Gesture does nothing | Check CONTROL listener, saved calibration, open-hand rearming, and current INFO coordinates. Python must use the same viewport fit/port as Java. |
| Missing classes / compile errors with old command | Use the new commands including `ArduinoSerialController.java` and the JAR at compile time, and the JAR plus `build-phase5b2` at runtime. |
| Missing images/sounds | Run from the project root, not from `build-phase5b2`. |

Stop after Phase 5B-2. No strips, buzzer, PCB, or Phase 5C behavior is implemented.
