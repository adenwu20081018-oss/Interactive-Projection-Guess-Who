# Hardware bill of materials — Phase 6A

This BOM distinguishes user-reported physical tests, an unvalidated design, and future hardware. It does not certify electrical ratings of an unidentified specimen.

| Ref / item | Qty | Specification / role | Status |
| --- | --- | --- | --- |
| U1 | 1 | ELEGOO Mega 2560; USB serial at 9600 baud, 8N1 | User physically tested for onboard LED feedback |
| USB data cable | 1 | PC to Mega; USB data and board power | User physically tested |
| USB webcam | 1 | Camera input to Python/OpenCV/MediaPipe | User physically tested for gesture control |
| Onboard L LED | Included | LED_BUILTIN / D13; no external LED or resistor purchase | User physically tested |
| BZ1 | 1 | Two-pin kit active buzzer; published nominal 5V, 3.5–5.5V, less than 25mA | Owned; designed, not physically validated |
| Q1 | 1 | NPN PN2222/2N2222A-family function; actual marking `2N2222A 331` | Owned; physical E/B/C pinout unverified |
| R1 | 1 | 470Ω, at least ¼W; D8-to-base resistor | Retained unvalidated design |
| R2 | 1 | 10kΩ, at least ¼W; base-to-emitter/GND pull-down | Retained unvalidated design |
| D1 | 1 | 1N4007; banded cathode to buzzer + | Retained unvalidated design |
| Breadboard / insulated jumpers | As needed | Separate electrical nodes; functional wiring plan only | Unpowered display / future validation |
| HDMI projector and cable | 1 each planned | PC graphics output over HDMI | Planned; projection untested |
| WS2812B strip | TBD | Future separately powered/addressed lighting extension | Planned, not purchased; no wiring designed |

The buzzer, Q1, resistors and protection diode are **DESIGNED — HARDWARE VALIDATION PENDING** as an assembled circuit. A component being owned is not a successful circuit test.

The published buzzer ratings come from the [previously extracted ELEGOO kit datasheet](phase5c1-elegoo-active-buzzer-datasheet.pdf). They do not establish a ≤20mA direct GPIO current limit; the retained design uses a transistor. No further component investigation is part of Phase 6A. No external supply, WS2812B power system or PCB is specified here.
