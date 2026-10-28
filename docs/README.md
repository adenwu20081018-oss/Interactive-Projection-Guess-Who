# Engineering documentation

Use the root [project README](../README.md) for the portfolio overview and quick start. The guides below provide implementation details and verification evidence.

| Start here | Purpose |
| --- | --- |
| [Projector-ready setup](phase7a-projector-ready.md) | Current Windows launch commands, laptop practice, resizing and future Extend-mode setup |
| [Vision installation](../computer-vision/README.md) | Python environment, model and camera troubleshooting |
| [Test evidence](testing.md) | Recorded automated checks and physical-test boundaries |
| [Publishing](publishing.md) | Review the proposed tracking set and publish using Git or GitHub Desktop |
| [Credits](credits.md) | Original game attribution and dependency provenance |

## Hardware and drawings

- [Architecture](images/system-architecture.svg)
- [BOM](hardware-bom.md), [pin connections](pin-connections.md), [validation status](hardware-validation-status.md)
- [Electrical design](images/buzzer-schematic.svg), [wiring plan](images/arduino-wiring.svg)
- [Editable KiCad files](../hardware/kicad/README.md)
- [Concept CAD and four views](../hardware/concept-cad/README.md)
- [Actual game screenshots](images/screenshots/README.md)

## Implementation history

These phase documents record work at the time it was performed; their scope statements and local-machine paths are historical, not replacements for the current quick start.

| Area | Technical record |
| --- | --- |
| Original game | [Analysis](existing-game-analysis.md), [Java verification](phase4a-java-verification.md) |
| TCP and gestures | [Python-to-Java protocol](phase4b-communication.md), [live control](phase4c-live-control.md) |
| USB serial | [Arduino upload/test](phase5a-arduino-test.md), [Java serial utility](phase5b1-java-arduino-test.md) |
| Game feedback | [Java/Arduino integration](phase5b2-game-arduino-feedback.md) |
| Buzzer design | [Retained transistor design](phase5c1-active-buzzer.md); [archived ULN2003 assessment](phase5c1-uln2003-review.md) |

Documentation is organized through this index without moving files that existing guides reference. Root-level artwork and audio remain in place because the Java game loads those paths directly.
