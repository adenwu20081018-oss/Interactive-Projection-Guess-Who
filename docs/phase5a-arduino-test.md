# Phase 5A — Arduino USB serial test (Windows)

This standalone sketch tests an ELEGOO MEGA 2560 (Arduino Mega 2560 compatible) through USB at 9600 baud. It uses only the onboard **L** LED (`LED_BUILTIN`, D13). No external wiring, libraries, Python packages, or game integration is needed. Java and the OpenCV system are unchanged. Phase 5B is not included.

## Files

- `arduino/serial_test/serial_test.ino` — complete Arduino sketch.
- `docs/phase5a-arduino-test.md` — this upload, test, and troubleshooting guide.

## Upload with Arduino IDE 2

1. Download and install **Arduino IDE 2 for Windows** from [Arduino Software](https://www.arduino.cc/en/software). Launch it and allow its initial setup to finish.
2. Connect the ELEGOO MEGA 2560 to the PC with a USB **data** cable (the board end is USB Type-B). USB supplies power; leave the board pins unconnected.
3. Identify its port: right-click Windows Start, open **Device Manager**, and expand **Ports (COM & LPT)**. Note the entries, unplug the Mega 2560, then reconnect it. The entry that disappears and returns is the board, which may be labeled `Arduino Mega 2560 (COM3)`, `USB Serial Device (COM3)`, or another USB-serial name. Your number may differ; do not assume COM3. You can also compare **Tools > Port** in the IDE before and after reconnecting.
4. In the IDE's **Boards Manager**, search for **Arduino AVR Boards** by Arduino and install it if needed.
5. Choose **File > Open** and open `arduino/serial_test/serial_test.ino` inside this project. Keep the file inside its matching `serial_test` folder.
6. Select **Tools > Board > Arduino AVR Boards > Arduino Mega or Mega 2560**. Under **Tools > Processor**, select **ATmega2560 (Mega 2560)**. Use this Arduino target for the ELEGOO board. Select the port identified above under **Tools > Port**. Alternatively use the top board selector's **Select other board and port** dialog.
7. Click **Verify** (checkmark) and wait for successful compilation. Close other programs using the COM port.
8. Click **Upload** (right arrow) and wait for the IDE to report completion. This replaces the program currently stored on the Mega 2560. RX/TX LEDs may flicker during transfer. No programmer or bootloader operation is needed.
9. Open **Tools > Serial Monitor**. Use the settings below and wait about two seconds after opening it before sending commands. Opening the port may reset the Mega 2560. The sketch starts with L off and prints no startup banner.

These steps follow Arduino's [IDE 2 upload guide](https://docs.arduino.cc/software/ide-v2/tutorials/getting-started/ide-v2-uploading-a-sketch/) and [board/port selection guide](https://support.arduino.cc/hc/en-us/articles/4406856349970-Select-board-and-port-in-Arduino-IDE).

The Mega 2560 uses `LED_BUILTIN` on digital pin 13, as listed in the [Arduino Mega 2560 specifications](https://store.arduino.cc/products/arduino-mega-2560-rev3). The sketch continues to use USB-connected `Serial`; no change to `Serial1`, `Serial2`, or `Serial3` is needed.

## Exact settings

| Setting | Value |
| --- | --- |
| Hardware | ELEGOO MEGA 2560 |
| Board package | Arduino AVR Boards by Arduino |
| Board target | Arduino Mega or Mega 2560 (`arduino:avr:mega:cpu=atmega2560`) |
| Processor | ATmega2560 (Mega 2560) |
| Built-in LED | `LED_BUILTIN`, digital pin 13 (onboard L LED) |
| Port | The COM port that appears when your Mega 2560 is connected |
| Serial Monitor baud | **9600 baud** |
| Serial Monitor line ending | **New Line**; **Both NL & CR** also works |
| Serial format | 8 data bits, no parity, 1 stop bit (default 8N1) |
| Other terminal flow control | None |

Do not choose **No line ending** or **Carriage return** alone: a newline (`LF`) is required to execute a command. See Arduino's [Serial Monitor guide](https://docs.arduino.cc/software/ide-v2/tutorials/ide-v2-serial-monitor).

## Manual command tests

Type the command text without quotes into Serial Monitor and press Enter/send. Wait for each reply before sending the next command, especially during BLINK.

| Send | Expected serial reply | Expected L LED behavior |
| --- | --- | --- |
| `PING` | `PONG` | No change |
| `LED_ON` | `ACK LED_ON` | Turns on and stays on |
| `LED_OFF` | `ACK LED_OFF` | Turns off and stays off |
| `BLINK` | `ACK BLINK` after completion | Initial 250 ms off, then three flashes: 250 ms on / 250 ms off each; ends off |
| `HELLO` | `ERR UNKNOWN_COMMAND` | No change |

BLINK takes approximately **1.75 seconds**, including the initial off interval. Try it both after LED_OFF and after LED_ON; there should be three distinct flashes in either case. The power LED stays on; observe **L**, not ON, RX, or TX.

Additional checks:

1. Switch line endings to **Both NL & CR** and send `PING`, `LED_ON`, and `LED_OFF` again. Expect the same replies, once per command.
2. Send lowercase `ping` or `LED_ON ` with a trailing space. Expect `ERR UNKNOWN_COMMAND`; commands are uppercase and exact, with no space trimming.
3. Send an empty line. Expect no reply and no LED change.
4. Send `ABCDEFGHIJKLMNOPQRSTUVWXYZ123456` (32 characters). Expect `ERR LINE_TOO_LONG` once. Then send `PING` and expect `PONG`, confirming recovery. The limit is 31 characters excluding CR/LF.
5. Optional framing check: choose **No line ending**, send `PING`, and confirm there is no reply. Restore **New Line** and send an empty line to complete that pending command; expect `PONG`. If your IDE does not send an empty input, press the board's RESET button to clear the pending line, wait two seconds, then send a normal `PING`.
6. Press RESET. After any bootloader activity, L should be off. Wait two seconds and send `PING`; expect `PONG`.

## Input handling and limits

The sketch uses a fixed 32-byte buffer, reserving one byte for the string terminator. It does not allocate Arduino `String` objects. Oversized lines are discarded until LF, then return one error. CR bytes are ignored for CRLF compatibility. Non-printable input other than CR/LF invalidates the whole line and returns `ERR INVALID_CHARACTER` at LF. Blank lines are ignored. Replies use Arduino's normal CRLF output endings.

BLINK deliberately uses simple blocking delays. Serial input is not processed during those delays; continuous sending can overflow the Mega 2560's separate hardware serial receive buffer. This phase uses a **send one command, wait for its reply** workflow, not streaming or queued game control. A partial command waits for LF or reset; there is no timeout.

## Tests actually performed during implementation

- Inspected the source for command/reply mappings, LED initialization, three blink cycles, bounded writes, LF/CRLF handling, and discard/recovery paths.
- Ran eight local source/document checks: baud and LED setup; command/reply strings; buffer bound and terminator; LF/CR handling; bad-line rejection; three-cycle loop and ACK ordering; includes restricted to Arduino/C standard headers; and guide coverage. **8/8 passed.** These inspect source structure, not executable behavior.
- Hardware correction: checked that the hardware references in these two Phase 5A files match the Mega 2560, the Mega board/processor settings are documented, and the sketch executable code is unchanged (only its header comment changed).
- Arduino compilation, upload, physical COM-port communication, and visual LED checks have **not** been performed. No Arduino compiler was found on PATH or in the default Arduino AVR tools directory. Complete the IDE Verify, Upload, and manual checks above on your Windows PC with the board connected.

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| No new COM port | Try a known USB data cable, another direct PC USB port, and reconnect. Refresh Device Manager. A power light alone does not prove the cable carries data. |
| Unknown device / warning icon | Inspect Device Manager's device status and hardware ID. Install the matching driver from Arduino or the board/USB-chip manufacturer's official source; compatible boards may use a different USB chip. Reconnect and repeat port identification. |
| Mega board target missing | Install **Arduino AVR Boards** in Boards Manager, then select **Arduino Mega or Mega 2560**. |
| Access denied / port busy | Close other serial terminals and IDE instances using that COM port. Close Serial Monitor before retrying upload if necessary. |
| Upload timeout / `avrdude` error | Recheck **Arduino Mega or Mega 2560**, processor **ATmega2560 (Mega 2560)**, and current COM port, reconnect the data cable, and retry. Leave pins 0 and 1 unconnected. Consult Arduino's [upload troubleshooting](https://support.arduino.cc/hc/en-us/articles/4403365313810-If-your-sketch-doesn-t-upload). |
| Garbled output | Set Serial Monitor to **9600**, reopen it, wait two seconds, and send PING. |
| No reply | Confirm upload succeeded, select the correct port, use **New Line**, and send uppercase PING. The sketch has no startup message. |
| Unexpected unknown-command error | Remove spaces/quotes; use exact uppercase commands. Reset the board if a previous unterminated command remains pending. |
| LED appears unchanged | Observe the onboard **L** LED. Send LED_OFF, then LED_ON. The power LED is independent. |
| BLINK reply is delayed / commands disappear | Wait approximately 1.75 seconds for ACK BLINK. Send commands individually and wait for replies. |
| LED turns off when monitor opens | Opening the serial connection can reset the Mega 2560; off is the expected startup state. |

Phase 5A is manually verified when all four commands and the unknown-command test behave as described, CRLF works, and an oversized line is rejected with successful recovery. Stop here; no Java serial connection or external electronics are part of this phase.
