# Credits and provenance

## Original game

The existing Java Guess Who source credits **James Chen, Edward Li, Aden Wu and Lucas Chua** (2025). The portfolio preserves those source headers and builds on the original Swing game, artwork, sound and gameplay. Integration features are documented separately in the phase guides; this repository should not be presented as a claim that one person authored every original asset or game component.

## Dependencies and external references

- Python vision dependencies and exact pins: [`requirements.txt`](../computer-vision/requirements.txt).
- MediaPipe Hand Landmarker model: Google's version 1 float16 bundle, downloaded by the existing setup command. [Model source and download notes](../computer-vision/models/README.md).
- Java serial dependency: jSerialComm 2.11.4, fetched from Maven Central by [`build.ps1`](../arduino/java/build.ps1) with a pinned SHA-256 check.
- Arduino platform: Arduino AVR Boards; ELEGOO Mega 2560 hardware.
- Hardware tutorial/datasheet sources are cited in the relevant [buzzer design guide](phase5c1-active-buzzer.md) and KiCad documentation.

No new blanket license has been assigned during portfolio preparation. Before choosing a repository license, confirm the permissions for the original collaborators' code, artwork, audio and included third-party documents. Dependency licenses remain their respective authors' licenses; a new repository license must not imply ownership of those materials.
