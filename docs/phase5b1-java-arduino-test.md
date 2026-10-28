# Phase 5B-1 — standalone Java USB serial test

Hardware: **ELEGOO MEGA 2560**, USB data cable, Windows 11, **JDK 17**. Keep the working `arduino/serial_test/serial_test.ino` uploaded. This utility uses its existing `PING`, `LED_ON`, `LED_OFF`, and `BLINK` protocol. The built-in L LED is `LED_BUILTIN` / digital pin 13. No external wiring is needed.

No game or Python/OpenCV files are changed. The Java sources live outside the game's `src` directory, so its existing compilation remains independent. No GUI, Maven, Gradle, Arduino sketch changes, or Phase 5B-2 integration is required.

## Added files

| File | Purpose |
| --- | --- |
| `arduino/java/src/phase5b1/ArduinoSerialController.java` | Discovery, opening, reset wait, handshake, commands, response validation, timeout handling, cleanup |
| `arduino/java/src/phase5b1/ArduinoSerialTest.java` | Standalone console test and interactive port selection |
| `arduino/java/tests/phase5b1/ArduinoSerialControllerTest.java` | Hardware-free tests against an in-memory serial connection |
| `arduino/java/build.ps1` | Download/check dependency, compile, optionally run tests |
| `arduino/java/.gitignore` | Excludes generated JAR and compiled classes from Git |
| `docs/phase5b1-java-arduino-test.md` | This guide |

The dependency is downloaded into `arduino/java/lib/`; compiled classes go into `arduino/java/build/`. Neither folder is used by the game.

## Dependency

Pinned library: **jSerialComm 2.11.4**, coordinates `com.fazecast:jSerialComm:2.11.4`. Its JAR includes the native libraries; do not separately install a DLL. This project uses its [official SerialPort API](https://fazecast.github.io/jSerialComm/javadoc/com/fazecast/jSerialComm/SerialPort.html) for port discovery and nonblocking reads/writes.

- Download: [versioned JAR on Maven Central](https://repo.maven.apache.org/maven2/com/fazecast/jSerialComm/2.11.4/jSerialComm-2.11.4.jar)
- SHA-256: `B5F7EC40D622864B92DC679159CDBAA3030D37884F0AF2DEEF45B56C16BDA7A6`

The build script downloads the JAR if absent and checks this digest on every run. The downloaded JAR is already present in this workspace; the procedure below also works on a fresh copy. Internet access is needed for the first download only. JDK 17 needs no additional native-access JVM flags for this utility.

## Quick start in Windows PowerShell

**Close Arduino IDE Serial Monitor and Serial Plotter before Java opens the COM port.** You may close Arduino IDE entirely to avoid a port conflict. Stop any other serial terminal using the board. Do not upload a sketch while Java holds the port.

Open PowerShell and run (change the project path if you moved the folder):

```powershell
Set-Location 'C:\path\to\Interactive-Projection-Guess-Who'
java -version
javac -version
.\arduino\java\build.ps1 -Test
if ($LASTEXITCODE -ne 0) { throw 'Build/tests failed; resolve the error before running.' }
```

Both Java commands should report version 17. If PowerShell blocks running the script, use the manual commands in the next section; no execution-policy change is required.

List ports only (does not open them):

```powershell
java -cp 'arduino\java\build;arduino\java\lib\jSerialComm-2.11.4.jar' phase5b1.ArduinoSerialTest --list
```

Start with an interactive port choice:

```powershell
java -cp 'arduino\java\build;arduino\java\lib\jSerialComm-2.11.4.jar' phase5b1.ArduinoSerialTest
```

Or supply the port explicitly:

```powershell
java -cp 'arduino\java\build;arduino\java\lib\jSerialComm-2.11.4.jar' phase5b1.ArduinoSerialTest COM5
```

Replace `COM5` with the actual discovered name. It is an example, not a hardcoded default. Windows classpath entries are separated by a **semicolon**; keep the entire classpath quoted. These commands run from the project root and never launch Guess Who.

## Complete manual download, compilation, and execution commands

Use these instead of `build.ps1` if desired. Run from the project root. They work without Maven/Gradle or a preinstalled JAR.

```powershell
$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Force 'arduino\java\lib', 'arduino\java\build' | Out-Null
$serialJar = Join-Path (Get-Location) 'arduino\java\lib\jSerialComm-2.11.4.jar'
if (-not (Test-Path -LiteralPath $serialJar)) {
    Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/com/fazecast/jSerialComm/2.11.4/jSerialComm-2.11.4.jar' -OutFile $serialJar
}
if ((Get-FileHash -LiteralPath $serialJar -Algorithm SHA256).Hash -ne 'B5F7EC40D622864B92DC679159CDBAA3030D37884F0AF2DEEF45B56C16BDA7A6') {
    throw 'JAR checksum mismatch. Remove the downloaded JAR and download it again.'
}

javac --release 17 -cp "$serialJar" -d 'arduino\java\build' 'arduino\java\src\phase5b1\ArduinoSerialController.java' 'arduino\java\src\phase5b1\ArduinoSerialTest.java'
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }

javac --release 17 -cp "arduino\java\build;$serialJar" -d 'arduino\java\build' 'arduino\java\tests\phase5b1\ArduinoSerialControllerTest.java'
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
java -cp "arduino\java\build;$serialJar" phase5b1.ArduinoSerialControllerTest
if ($LASTEXITCODE -ne 0) { throw 'Hardware-free tests failed.' }

java -cp "arduino\java\build;$serialJar" phase5b1.ArduinoSerialTest --list
java -cp "arduino\java\build;$serialJar" phase5b1.ArduinoSerialTest COM5
```

## Physical test: perform these steps yourself

1. Connect your Mega using the USB data cable. Leave the working Phase 5A sketch on it; no re-upload is needed.
2. Close Arduino Serial Monitor/Plotter and any other program using the port.
3. Run the `--list` command above. COM5 was previously detected and was also found during implementation, but Windows may assign another number later. In Device Manager, compare **Ports (COM & LPT)** before and after unplugging/reconnecting the board if unsure. Avoid selecting Bluetooth ports.
4. Start the interactive tester and type your port name, or run with `COM5` as shown above. The program waits **2.5 seconds** for reset and sends a startup `PING`. It must receive `PONG` before reporting readiness. Allow up to approximately **7.5 seconds** for either readiness or a handshake timeout. Bootloader LED flicker during reset is normal.
5. At the `>` prompt, enter each command below and wait for its reply. Java appends the newline automatically; do not type literal `\n` characters.

| Enter | Expected Arduino output | Expected L LED |
| --- | --- | --- |
| `PING` | `Arduino -> PONG` | No change |
| `LED_ON` | `Arduino -> ACK LED_ON` | On continuously |
| `LED_OFF` | `Arduino -> ACK LED_OFF` | Off |
| `BLINK` | `Arduino -> ACK BLINK` after completion | Three flashes, then off; about 1.75 seconds total |

6. Optional: enter `ALL`. It runs those four commands sequentially, holding LED_ON for one second before LED_OFF so you can see it. No next command is sent until the preceding acknowledgement arrives.
7. Enter an unsupported word such as `HELLO`. The console explains the valid commands and stays open; it does not send unsupported input to the board. Phase 5A's Arduino error replies, if received, are displayed as communication errors.
8. Enter `QUIT`. Expect `Serial port closed.` Reopen Arduino Serial Monitor at 9600 baud with New Line and send `PING` to confirm the port was released. Close the monitor again before rerunning Java. EOF also exits, and Ctrl+C triggers shutdown cleanup. Force-killing the process or powering off Windows cannot guarantee Java cleanup callbacks.
9. Test disconnection: run Java again, wait for Ready, unplug USB, and enter `PING`. Expect a disconnect/read/write error or a timeout within approximately five seconds of the command. The tester exits with a nonzero status. Disconnecting while idle is noticed on the next command; there is no background hardware monitor. Reconnect, list ports again, and restart Java.
10. Optional busy-port check: open Arduino Serial Monitor and try Java. Expect a clear cannot-open error. Close the monitor and rerun Java successfully.

Example session after opening the selected port:

```text
Arduino: PONG (startup handshake). Ready at 9600 baud, 8N1.
Commands: PING, LED_ON, LED_OFF, BLINK, ALL, QUIT.
ALL runs the four commands in that order; wait for each response.
> PING
Java -> PING
Arduino -> PONG
> LED_ON
Java -> LED_ON
Arduino -> ACK LED_ON
> LED_OFF
Java -> LED_OFF
Arduino -> ACK LED_OFF
> BLINK
Java -> BLINK
Arduino -> ACK BLINK
> QUIT
Serial port closed.
```

## Controller behavior

- `listPorts()` discovers names and descriptions; no port is automatically chosen.
- `open(portName)` configures **9600 baud, 8 data bits, no parity, 1 stop bit**, flow control disabled. It waits for reset and validates a PING/PONG handshake. A failed open/handshake reports an error and cleans up any opened connection.
- `send(Command)` serializes each request/response pair, sends ASCII with LF termination, accepts CRLF or LF replies, and validates the expected acknowledgement. BLINK has the same five-second transaction deadline, comfortably above its 1.75-second sequence.
- Reads and writes use nonblocking library operations with a monotonic five-second deadline and short polling sleeps. Partial writes are handled. Replies are limited to 128 printable characters; malformed or unexpected responses fail clearly.
- Timeout, bad reply, and I/O failure invalidate and close the session. No automatic command retries are made: an LED command could have executed even if its acknowledgement was lost. Reopen/restart to retry deliberately.
- Try-with-resources closes the port after normal exit or an exception. A shutdown hook handles console interruption after connection. Closing does not deliberately send LED_OFF, so use LED_OFF before quitting if desired; USB disconnect or the next open/reset can change the LED state.

## Validation performed during implementation

- Downloaded the actual jSerialComm **2.11.4** JAR from Maven Central and recorded/checked its SHA-256.
- Compiled both production classes and the test harness using **JDK 17.0.20** with `--release 17` against that JAR.
- **20 hardware-free checks passed**: startup handshake and four exact command frames; normal close; partial writes; LF/CRLF handling; delayed BLINK response; no-reply and partial-reply timeouts; Arduino error reporting; wrong acknowledgement; oversized and invalid input; simulated disconnection; native read/write error returns; stalled writes; failed-handshake cleanup; reset delay ordering; idempotent close; rejecting sends after close; and interruption cleanup. These exercise the real controller through an in-memory connection, not a physical USB device.
- Ran the real CLI with `--list` successfully. Native discovery reported COM3/COM4 Bluetooth ports and **COM5 — Arduino Mega 2560**. Discovery did not open the board.
- Ran the real CLI with `COM_DOES_NOT_EXIST`: it printed a missing-port error and exited with status 1.
- Compared SHA-256 snapshots of 10,390 existing game/OpenCV/sketch/classpath files: all unchanged.
- Physical Java-to-board command execution, LED observation, busy-port behavior, unplug/replug behavior, and hardware port release remain for you to verify with the steps above. Your successful Phase 5A Arduino IDE tests do not substitute for these Java tests.

## Troubleshooting

| Problem | Action |
| --- | --- |
| `java` or `javac` not found | Install/use JDK 17 and ensure its `bin` directory is on PATH; open a new PowerShell window. Both commands are required. |
| Download fails | Check internet/proxy access to Maven Central, then rerun. A partial file can cause checksum mismatch; remove only that JAR and retry. |
| Script execution blocked | Use the complete manual commands above. |
| `ClassNotFoundException` / `NoClassDefFoundError` | Compile first; run from project root; include both `arduino\java\build` and the exact JAR in the quoted semicolon-separated classpath. |
| Native library load error | Use the complete official JAR, compatible JDK 17, and a writable temporary directory. Check antivirus quarantine messages. |
| No board listed / missing COM5 | Reconnect a data-capable USB cable, compare Device Manager entries, and pass the current port name. Do not select a Bluetooth port. |
| Cannot open port | Close Arduino IDE Serial Monitor/Plotter and other serial programs. Unplug/reconnect and relist ports if needed. |
| Handshake timeout | Confirm selected port and working Phase 5A sketch at 9600. Close Java, verify PING in Arduino Serial Monitor, close the monitor, then rerun Java. |
| Unexpected or malformed response | Check sketch, baud, and selected device. The error includes printable unexpected replies. Restart the session after fixing the cause. |
| Timeout during BLINK | Wait for ACK; do not reset or unplug during the sequence. Check the USB cable. The standard Phase 5A sequence fits within the five-second deadline. |
| Port unavailable after exit | Check for another terminal/Java process, close it, reconnect the board, and list ports again. |

Stop after these standalone tests. Gameplay feedback, external LED strips, and Phase 5B-2 are intentionally not implemented.
