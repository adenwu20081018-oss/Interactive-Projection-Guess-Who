# Hardware validation status — Phase 6A

## Evidence and boundaries

| Subsystem | Status | Evidence / limitation |
| --- | --- | --- |
| Webcam gestures controlling Java | Physically tested, user reported | User confirmed mouse and pinch control in the existing game |
| USB serial and onboard Mega LED | Physically tested, user reported | User confirmed commands and automatic LED feedback from game actions |
| BUZZ firmware | Implemented; software checks previously passed | Earlier Mega compile and 32 hardware-free logic checks; not repeated in Phase 6A |
| Transistor active-buzzer assembly | **DESIGNED — HARDWARE VALIDATION PENDING** | No physical pinout verification, electrical measurements or audible-operation validation |
| HDMI projector | Planned / untested | Display path documented; physical projection not demonstrated |
| WS2812B strip | Planned / not purchased | No implemented circuit, allocated pin or firmware |

The selected buzzer design retains Q1, 470Ω R1, 10kΩ R2, 1N4007 D1 and the active buzzer. The marking `2N2222A 331` does not establish physical E/B/C order. Diagrams use functional terminals only and may accompany an **unpowered** display. The ULN2003 alternative is archived and is not the selected circuit. No further component investigation is part of this phase.

## Phase 6A checks

- Application/firmware source hashes compared before and after generation: unchanged.
- Three SVG documents parsed as XML and rendered to PNG; exported previews visually inspected.
- Generated KiCad S-expression delimiters checked and intended wire graph checked for five separate nets with all listed terminals attached. These checks are not KiCad parsing or ERC.
- KiCad executable/CLI was not found on PATH or in the standard Program Files location. **Native opening, KiCad rendering and ERC were not performed. No ERC pass is claimed.**
- No hardware was powered, uploaded, measured or tested. No Java, Python/OpenCV or Arduino firmware change was made. No PCB was designed.

## Deliverables

| File | Format / purpose |
| --- | --- |
| `hardware/kicad/guess-who-hardware.kicad_sch` | Editable modern KiCad schematic with embedded functional symbols; native validation pending |
| `hardware/kicad/intended-nets.json` | Reference connectivity, not a KiCad-exported netlist |
| `hardware/kicad/README.md` | Opening instructions, modeling limits and future ERC procedure |
| `hardware/kicad/validation-report.txt` | Actual tool/check results |
| `docs/images/system-architecture.svg` and `.png` | System/protocol drawing, scalable source and raster preview |
| `docs/images/arduino-wiring.svg` and `.png` | Color breadboard connection plan, functional transistor terminals |
| `docs/images/buzzer-schematic.svg` and `.png` | Electrical design illustration, not a KiCad export |
| `docs/hardware-bom.md` | Parts and validation classification |
| `docs/pin-connections.md` | Functional connections, interfaces and polarity |
| `docs/hardware-validation-status.md` | This status and deliverables record |

Remaining verification: open the schematic in KiCad, inspect its layout, run ERC and record findings; independently establish the exact transistor's physical terminal mapping before any future powered assembly; validate polarity, electrical operation, beep timing and existing command regression. Software ACK BUZZ establishes command acceptance, not sound production. Projector and strip validation belongs to later work.

Documentation scope stops at Phase 6A. Hardware validation is deferred; no Phase 6B work is included.
