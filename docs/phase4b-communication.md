# Phase 4B — Simulated Python-to-Java communication

Phase 4A was checked before integration: JDK 17 compilation and the original Swing GUI had passed. Phase 4B also compiled and ran successfully. No webcam, gesture, calibration, Arduino, graphics, audio resources, or game rules were changed.

## Compile and launch in PowerShell

Close any previous integrated game using port 5050 before launching another. The Phase 4A build has no control listener.

Terminal 1:

```powershell
Set-Location 'C:\path\to\Interactive-Projection-Guess-Who'
$jdk17 = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
& "$jdk17\bin\javac.exe" -encoding UTF-8 --release 17 -d build-phase4b src\groupDGuessWho\*.java
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
& "$jdk17\bin\java.exe" -cp build-phase4b groupDGuessWho.Main --control-port 5050 --viewport-width 1280 --viewport-height 720 --viewport-fit stretch
```

These are the default server settings. Keep the working directory at the project root so relative image/audio paths work. No external Java libraries are needed. Eclipse's existing src configuration includes the new files automatically.

Use `--no-control` for mouse-only operation. If the port is occupied, Java prints `CONTROL disabled` and keeps the mouse game running; this is not a successful network launch. Choose another port with `--control-port 5051` and pass `--port 5051` to Python as well.

## Python test client and finding coordinates

In Terminal 2, from the same project root:

```powershell
python test_java_connection.py --info
```

INFO performs no click. It returns live viewport/content dimensions and visible, enabled, uncovered button centres. Each entry contains a readable control name, viewport `x`/`y`, and diagnostic `java_x`/`java_y`. Send the listed **x/y**, not java_x/java_y. Requery after a screen or round changes.

On the title screen in the tested layout, the centre was approximately (639,359):

```powershell
python test_java_connection.py --x 639 --y 359 --id my-title-001
```

General example:

```powershell
python test_java_connection.py --x 620 --y 340
```

The selected control depends on the current screen. Without `--id`, Python creates a UUID. Every intentional click should use a fresh ID. Repeat `--id my-title-001` to test duplicate suppression: no second click should occur.

The script uses only Python's standard library and never imports camera/gesture code. If needed, substitute `computer-vision\.venv\Scripts\python.exe` for `python`; the script itself remains at the project root. Do not start the webcam program for Phase 4B.

## Protocol and acknowledgements

Java binds specifically to **127.0.0.1**, never a public interface, and checks that accepted clients are loopback peers. Each UTF-8 request must end with a newline:

```json
{"type":"SELECT","x":620,"y":340,"id":"test-001"}
```

SELECT requires finite numeric coordinates within 0..viewportWidth-1 and 0..viewportHeight-1. Fractions are allowed. Optional IDs are 1–128 ASCII letters, digits, underscore, dot, colon, or hyphen. Unknown fields/types, duplicate JSON keys, trailing JSON, missing coordinates, strings/booleans/null in place of numbers, and malformed JSON are rejected. The strict decoder supports this flat string/number protocol, not arbitrary nested JSON.

The read-only request is `{"type":"INFO"}`.

Network reads run on background threads. Validated GUI requests are queued with `SwingUtilities.invokeLater()`. On the Swing event-dispatch thread, the adapter reads current layout bounds, finds the topmost component under the coordinate, and activates an eligible JButton using `doClick(0)`. This reuses all existing action and sound listeners. It does not move the OS mouse or directly invoke Board.actionPerformed.

Python prints a delivery message after sending bytes, then waits for Java's final acknowledgement. Delivery alone does not prove execution. Example acknowledgement:

```json
{"received_x":219.0,"received_y":215.0,"java_x":137,"java_y":150,"control":"character[0]: GE19","status":"executed","executed":true,"received":true,"id":"test-001"}
```

- `received: true`: Java received the framed request; rejected requests can also have this field.
- `executed: true`: the existing button listeners returned without an uncaught exception. This does not guarantee a particular game outcome: some original buttons intentionally do nothing in some states.
- `status`: executed, info, rejected, duplicate, unused_margin, no_control, or action_error.
- Duplicates return executed=false and original_status, without repeating the action.
- An action_error may follow partial listener work. Its ID stays consumed.

Java logs connections, received coordinates, converted coordinates, selected control, execution result, and disconnections. The client exits 0 for successful execution/INFO, 2 for a reply without a new executed action, and 1 for connection/protocol errors.

If the connection fails after sending, execution is **unknown**. There are no automatic retries. Retry with the same ID against the same Java process; a new ID could click twice.

### Duplicate handling and bounds

Valid SELECT IDs are remembered across connections for the Java process lifetime, including no_control and margin outcomes. Actions and duplicate checks are serialized, including simultaneous requests. The cache holds 10,000 IDs; new IDs are rejected at capacity rather than evicting old ones and risking replays. Restart clears it. Events without IDs cannot be deduplicated.

Requests are limited to 8192 bytes per line, four concurrent clients, and a 10-second idle read timeout. Oversized, invalid-UTF-8, incomplete, or excess connections can close without a JSON acknowledgement. Unterminated messages never execute. Client disconnection does not terminate the game. The listener and clients close with the game window.

## Coordinate mapping

1280 x 720 is the configured input viewport, not the Java window size. The actual content area measured **802 x 502** on this machine; most original panels occupy 800 x 500. The adapter queries actual content dimensions on every request, excludes title bar/borders, and uses Swing logical coordinates so no manual Windows DPI multiplier is applied. Screen position is irrelevant.

- **stretch** (default): the whole viewport maps onto the current content rectangle. Endpoint mapping is x * (contentWidth-1)/(viewportWidth-1), likewise for y. This suits calibration of the entire game-content rectangle to the entire virtual viewport.
- **contain**: content is aspect-preserving and centred in the virtual viewport, using scale=min(viewportWidth/contentWidth, viewportHeight/contentHeight). Remaining side/top margins are rejected as unused_margin. INFO reports offsets. Use only if the actual presentation follows this letterbox convention.

The modes specify a coordinate contract; they do not redraw the game or detect a projector layout. The original window remains non-resizable. Mapping still recomputes from live dimensions if the platform or a future approved change alters them. Original fixed-position panels are not redesigned, and unused content areas return no_control. Arbitrary crops, desktop offsets, or projection margins must not be assumed to match either mode. Actual Phase 3 input remains disconnected.

INFO uses live hierarchy/z-order hit testing so hidden or covered buttons are not offered through their centre points. A screen can still change between INFO and SELECT; there is no screen-version transaction in this phase.

## Automated tests

From the project root:

```powershell
$jdk17 = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
& "$jdk17\bin\javac.exe" -encoding UTF-8 --release 17 -d build-phase4b-tests src\groupDGuessWho\*.java tests\java\groupDGuessWho\*.java
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed' }
$env:JAVA17_EXE = "$jdk17\bin\java.exe"
$env:JAVA_TEST_CLASSPATH = 'build-phase4b-tests'
python -m unittest discover -s tests -v
```

**15 tests passed.** They cover real Python-to-Java messaging, malformed JSON, missing/invalid coordinates, duplicate IDs across reconnects and concurrent requests, no-ID behavior, coordinate endpoints/scaling/letterboxing/resizing, fragmented/multiple messages, partial disconnects, oversized messages, reconnects, real connection refusal, readable client errors, and actual Swing hit testing/EDT execution. A brief separate Swing fixture tests hidden, disabled, and covered buttons. The test endpoint uses an ephemeral port and is terminated afterward; it does not control your running game.

Initial compilation found the project's existing Character class shadowed java.lang.Character; the new serializer was qualified without changing the original class. An initial network test run lacked sandbox socket permission; the successful run used permitted localhost access.

An optional repeat of the unchanged Phase 1–3 suite was blocked before execution because automatic approval review hit a usage limit. That suite was not rerun in this phase; its prior 62-test result is recorded in the Phase 3 documentation.

## Actual GUI verification

Launched the new build with its server on port 5050. INFO returned the actual live layout. Simulated SELECTs advanced the original introduction to the 24-character board. A SELECT at viewport (219,215) mapped to Java (137,150) and visibly excluded GE19. Repeating the ID returned duplicate and left the card excluded. A normal mouse click restored it. The running game's stderr log remained empty.

This verifies real original-game activation, not every gameplay path or a full round. Sound listeners/resources were preserved and no audio errors appeared, but audible playback quality was not independently checked. Phase 0's pre-existing game logic issues were not changed. The integrated game was left open on the choose-your-agent board after verification.

## Manual checks

1. Launch Java and confirm `CONTROL listening`, not `CONTROL disabled`.
2. Query INFO and send a listed coordinate with a fresh ID. Observe both the GUI and execution acknowledgement.
3. Repeat the same ID: no second action. Use a new ID for the next intended click.
4. On the board, query INFO again and toggle a character. Confirm ordinary mouse clicks still work.
5. Try a non-button area: expect no_control. Try out-of-range coordinates: expect rejection, not a crash.
6. Close Java, run Python, and verify the readable connection error. Restart and query INFO again. IDs do not survive restart.
7. Optionally launch with `--viewport-fit contain` and test a letterbox margin, then a valid INFO coordinate.

## Files and remaining limitations

Modified:
- `src/groupDGuessWho/Main.java`: EDT startup, server options and lifecycle.
- `src/groupDGuessWho/Board.java`: window access and readable control names only; existing gameplay methods unchanged.

Created:
- `src/groupDGuessWho/ControlJson.java`
- `src/groupDGuessWho/ControlServer.java`
- `src/groupDGuessWho/ViewportMapping.java`
- `src/groupDGuessWho/SwingSelectionController.java`
- `test_java_connection.py`
- `tests/test_java_protocol.py`
- `tests/java/groupDGuessWho/ControlTestHarness.java`
- `docs/phase4b-communication.md`

Generated: build-phase4b and build-phase4b-tests class files, plus build-phase4b/stdout.log and stderr.log. Original bin files, Eclipse configuration, images, sounds, and all Phase 1–3 computer-vision code/tests were preserved.

Limitations: deduplication is in memory; local processes are not authenticated; UI state can change between requests; execution confirms listeners ran rather than guaranteeing a semantic game result. Do not forward the port externally. No webcam, Arduino, or Phase 4C work was started. Stop after Phase 4B.
