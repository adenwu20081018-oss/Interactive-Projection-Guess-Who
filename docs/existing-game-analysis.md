# Existing Guess Who game analysis

Inspected on October 26, 2026. This stage is analysis only. No Java source, compiled classes, graphics, sounds, or gameplay were changed. No integration was implemented.

## Project and inspection scope

Project root: `C:\path\to\Interactive-Projection-Guess-Who`.

All six Java source files in `src/groupDGuessWho/` were read, including their character data, event handlers, question selection/filtering, presentation, and audio code:

| File | Purpose |
| --- | --- |
| `Main.java` | Main GUI entry point. `main()` constructs `new Board()`. |
| `Board.java` | Main window and game controller together. Owns buttons, characters, turns, health, menus, results, answer correction, graphics, and sound. Includes the inner music class `Player`. |
| `Character1.java` | Character data used by Board: ID, gender, eye/hair colours, and five yes/no traits, with getters and setters. `getName()` returns the ID. |
| `AIQuestionSelector.java` | Separate console AI game with its own character list, `selectQuestion()`, `filterCharacters()`, `playGame()`, and incorrect-answer comparison. Has its own `main()`. Board does not call this class. |
| `Main2.java` | Console demonstration: constructs AIQuestionSelector and prints one selected question. It is not an automated test. |
| `CharacterGuessingGame.java` | Separate console game with its own `main()` and `startGame()`. Also declares the package-private `Character` data class. Board does not use it. |

Also inspected `.project`, `.classpath`, `.settings/org.eclipse.jdt.core.prefs`, and `.settings/org.eclipse.core.resources.prefs`. Inventoried all 13 supplied class files in `bin/groupDGuessWho/` and checked their class-version headers; they were not decompiled or verified against source.

All 137 PNG resources were opened with System.Drawing and their dimensions read successfully. All four WAV files were read and checked for RIFF/WAVE headers. All literal PNG/WAV references in Board were checked for existence. These are file checks, not a visual gameplay review or audio playback test. The full original-file inventory is at the end of this document.

## Main game and screen

The GUI uses Java Swing and AWT: JFrame, JPanel, JButton, JLabel, JProgressBar, layouts, and ActionListener. It does not use JavaFX or a browser. Sound uses `javax.sound.sampled`.

The window is fixed at 814 by 537 pixels. Panels use an approximately 800 by 500 content area and fixed positions. The character board is a 3-row, 8-column grid at content position (100,100), size 600 by 300; each character card occupies 75 by 100 pixels. Agency FB is requested for text; it is not bundled.

`Board.actionPerformed(ActionEvent)` (line 550) is the central input handler. It checks which actual button object fired the event using `event.getSource()`. Game state is held in fields such as `panelOnScreen`, `continueStatus`, `continueSmallStatus`, `retrace`, `AIGuessStatus`, `roundWinner`, and the health values. There is no separate controller class or external-input API.

### Existing controls

| Control | Existing action |
| --- | --- |
| `startGame` | Clickable title and introduction screen. Successive clicks show Intro1 through Intro4, then the board. |
| `returnToMainMenu` | Clickable win/lose screen that returns to the menu and resets the round. |
| `characterDisplay[24]` | Toggle a card's exclusion mark, guess that character in guess mode, or identify your own agent during answer correction. |
| `playerAction[0..1]` | Guess A Character; Check A Characteristic. |
| `characteristics[0..7]` | Gender, Eyes, Hair, Beard, Moustache, Big Nose, Glasses, Hat. First three open submenus. |
| `gender[0..1]` | Male, Female. |
| `eyes[0..2]` | Brown, Blue, Green. |
| `hair[0..5]` | Black, White, Dark Brown, Light Brown, Blonde, Highlights. |
| `playerAnswer[0..1]` | YES and NO for the AI's question or guess. |
| `revoke` | Return from guess/trait selection or a trait submenu. It is navigation, not a general undo. |
| `continues` | Wide confirmation/continue control; initially confirms that you have chosen an agent. |
| `continueSmall` | Small CONTINUE control for dialogue, AI turns, round outcomes, and answer correction. |
| `healthDisplayImage_button` | Final green virus-upload button, effective when `virusOn` is true. |

`setupButtons()` configures general controls; `setUpButtons()` is a different, case-sensitive method that creates character buttons. `JButtonSetUp()` attaches both gameplay and click-sound listeners. Character, title, and result controls have their own character-click sound listeners.

## Character selection and gameplay

1. `initializeCharacters()` (line 193) creates 37 characters for the GUI.
2. `selectRandomCharacters()` (237) chooses 24 distinct character objects. It copies this selection into `allCharacters` (AI possibilities) and `charactersOnScreen` (the displayed board).
3. `selectRandomCharacter()` (257) chooses the AI's secret agent from those 24. The field `selectedCharacter` means the AI's secret, not the player's choice.
4. The player chooses an agent mentally and confirms. There is no initial stored player-character selection in Board. Clicking a card normally marks/unmarks it as excluded; it does not register a secret agent.
5. In guess mode (`panelOnScreen == 0`), clicking a card compares it with the AI's secret. Outside this mode, exclusion marks only change `characterStatus` and the image; they do not filter the AI's candidate list.
6. Trait buttons answer questions about the AI's secret. `playerQuestionDisplay()` presents the question and continuation control.
7. `AITurn()` (1396) asks a question or guesses. `selectQuestion()` (1190) chooses an unasked question that divides remaining possibilities as evenly as possible. `AITracker()` records the human's answers; `AIElimination()` and `filterCharacters()` reduce AI possibilities.
8. Two explicitly handled indistinguishable pairs are AA04/KE25 and BY08/LZ29. When one of these pairs is all that remains, the AI makes a random guess between them.
9. `AIRetrace1()` and `AIRetrace2()` handle inconsistent answers and ask the player to identify their agent for comparison.
10. `winCheck()` deducts 20 health from the round loser and calls `resetRound()`. Health starts at 100. Player defeat shows the loss screen; defeating the AI leads to the green virus-upload control before the win screen.

## Graphics, sound, and transitions

There are 111 character PNGs: 37 IDs, each with normal, `exclude`, and `target` variants. `setUpImage()` explicitly maps all 37 IDs to these files. The remaining 26 PNGs include title, four introduction screens, win/lose screens, profiles, dialogue backgrounds, button backgrounds, battery, and health images.

`AIDisguise.png`, `buttonBase.png`, and `healthDisplay.png` are present but not referenced by Board's resource literals. They were preserved.

The WAV files are `Intro.wav` (1,802,062 bytes), `Suspicion_Matrix.wav` (18,018,126), `buttonClick.wav` (80,718), and `characterClick.wav` (184,740). `Board.Player.playMusic()` loops music; `stopMusic()` stops it. `playSound()` starts click clips.

The inspected GUI implements presentation changes through image swaps, text changes, panel visibility, and progress-bar values. No active timer-based animation loop was found. The commented-out sleep experiment is inactive. Actual timing, rendering, sound quality, and user-visible transitions could not be verified because launch failed.

## Compilation and launch

This is an Eclipse Java project named `groupDGuessWho`, configured for Java SE 17, UTF-8, source folder `src`, and output folder `bin`. No Maven/Gradle build, wrapper, JAR, launch script, or automated test suite was supplied. Imports use standard Java libraries only.

With a JDK 17 available, these PowerShell commands describe a source build into a separate output directory, preserving the supplied bin files. They were NOT executed because no javac was found:

```powershell
Set-Location 'C:\path\to\Interactive-Projection-Guess-Who'
javac -encoding UTF-8 --release 17 -d build-analysis src\groupDGuessWho\*.java
java -cp build-analysis groupDGuessWho.Main
```

The supplied binaries can instead be launched with Java 17 using `java -cp bin groupDGuessWho.Main`. In Eclipse, import the existing project, configure a Java 17 JDK, and run Main as a Java Application. Keep the working directory at the project root: resources are loaded by relative filenames such as `title.png`, not packaged classpath lookups.

Other entry points are `groupDGuessWho.Main2`, `groupDGuessWho.AIQuestionSelector`, and `groupDGuessWho.CharacterGuessingGame`; these run console programs, not the graphical game.

### Checks actually performed

| Check | Result |
| --- | --- |
| Recursive source/configuration/resource inventory | Completed: 6 Java, 137 PNG, 4 WAV, 13 class files, and 4 Eclipse configuration files. |
| PNG decoding/dimensions using System.Drawing | Passed for all 137 files. Not a visual comparison of art against character traits. |
| WAV signature inspection | All 4 have RIFF/WAVE headers. No playback or full audio-decoder test. |
| Board PNG/WAV reference existence | No missing referenced files. |
| Class-version inspection | All 13 binaries have major version 61 (Java 17). Binary/source equivalence not established. |
| Java discovery | Neither java nor javac was on PATH. Checked common Java/JDK directories and searched Program Files and Program Files (x86) for java.exe/javac.exe. Found MATLAB's bundled Java 8 runtime, no compiler. This is not proof no other runtime exists elsewhere. |
| Runtime version command | MATLAB runtime reports Java 1.8.0_202. |
| Actual GUI launch attempt | Failed before opening a window: UnsupportedClassVersionError, Main requires class version 61.0; available runtime supports up to 52.0. |
| Compilation | Not run: no Java compiler found. |
| Existing automated tests | None found; zero game tests ran. Console demonstrations were not treated as tests. |
| Manual gameplay | Not run: incompatible runtime prevented startup. |

Exact executable used for the version check and launch attempt:

```powershell
& 'C:\Program Files\MATLAB\R2026a\sys\java\jre\win64\jre\bin\java.exe' -version
& 'C:\Program Files\MATLAB\R2026a\sys\java\jre\win64\jre\bin\java.exe' -cp bin groupDGuessWho.Main
```

No software was installed and no replacement binaries were generated.

## Where future gesture input belongs

This section identifies future connection points only; nothing here was implemented.

The natural destination is the existing Board instance and its buttons. A future input adapter should translate a gesture into one deliberate activation of an appropriate existing button. Activating a button through `doClick()` on Swing's event-dispatch thread would retain its gameplay and sound listeners. Calling `actionPerformed()` directly with a text command would not work as-is: it checks button identity, and a direct call would bypass the separately attached sound listener.

| Future intent | Existing destination and downstream methods |
| --- | --- |
| Point at/activate a character | `characterDisplay[index]` -> `actionPerformed()` -> existing exclusion/guess/retrace branch, `setUpImage()`, possibly `AIRetrace2()`. |
| Choose a question | `playerAction`, `characteristics`, `gender`, `eyes`, `hair` buttons -> `actionPerformed()` -> trait comparison and `playerQuestionDisplay()`. |
| Answer yes/no | `playerAnswer` -> `actionPerformed()` -> `AITracker()`, `AIElimination()`, `winCheck()`, or `AIRetrace1()`, depending on phase. |
| Continue, back, start, or finish | Existing `continues`, `continueSmall`, `revoke`, `startGame`, `returnToMainMenu`, or health button. |

The controls are private, so a narrowly scoped Board entry point may eventually be needed to find and activate valid controls. `Main.main()` is the possible future startup/lifecycle connection point. Character1 is a data object and should not receive device events. The console classes are not the GUI controller and should not be used as integration targets for this game.

Do not send raw gesture input directly into `filterCharacters()`, `winCheck()`, or `resetRound()`: that would skip parts of the existing user interaction. A new initial player-selection mechanic would change the game and is outside this stage.

## Existing issues found by source inspection

These are source findings, not reproduced gameplay test failures. They were left unchanged.

- **Brown-hair answers are incorrect:** in Board's `hair[2]`/`hair[3]` handlers, comparisons use `Dark Brown` and `Light Brown`. Character data uses `Dark brown` and `Light brown`. Java's equals comparison is case-sensitive, so those player questions report NO even for matching agents.
- **Some round state does not reset:** `resetRound()` declares local arrays named `retraceResultDisplay`, `genderAIGuess`, `eyesAIGuess`, and `hairAIGuess`. These temporary variables hide the fields rather than resetting them. Answers/correction flags can persist between rounds.
- **Return-to-menu reset ordering:** the result button calls `mainMenuOn()` followed by `resetRound()`. The latter overwrites first-round flags and continuation state just initialized by the former. A replay may skip the original first-round sequence.
- **Correction sequence can stall at the hat item:** the small-continue correction branch requires `continueSmallStatus < 10`, but includes an item for status 10. If advancement leaves status at 10, the next click reaches the later fallback and can finish the round instead of displaying the hat correction.
- **Empty candidate correction is overwritten:** `AIElimination()` calls `AIRetrace1()` when no candidates remain, then unconditionally replaces its AI message with ELIMINATION IN PROGRESS and shows the wide Continue panel. This creates conflicting correction/continuation state.
- **Input depends heavily on UI visibility:** many action branches do not verify whether their source button is currently shown or appropriate for the turn. Cards also remain actionable in several phases. An external adapter must not activate hidden controls or bypass the current phase; repeated guesses can change the pending round outcome before continuation.
- **Swing startup is on the main thread:** Main constructs Board directly, without scheduling it on Swing's event-dispatch thread. Future background input must not introduce further unsynchronized Swing updates.
- **Audio resource lifetime:** `playSound()` opens a stream and clip for each click without explicitly closing them. Music streams also lack explicit closure. Rapid repeated input could amplify resource use; playback behavior was not tested.
- **Console versions differ from GUI data and rules:** AIQuestionSelector has 47 characters rather than Board's 37 and uses OA35 rather than QA35; traits also differ, for example FH17/GE20 gender. CharacterGuessingGame also has differing traits and whitespace in JE23's values. These are separate implementations, not interchangeable controllers.
- **Console logic limitations:** AIQuestionSelector replaces the player's comparison character with blank traits before identifying wrong answers, so it does not actually look up the supplied ID's traits. Its question selection can exhaust questions with multiple indistinguishable candidates. CharacterGuessingGame filters a single shared board for both players, has unchecked numeric input, and breaks after a round outcome despite the five-point loop condition. These do not establish bugs in Board's separate implementations.
- **Packaging/runtime limitations:** relative resource paths, a non-bundled font, fixed screen geometry, and no reproducible automated tests complicate deployment and verification. Supplied binaries could be stale; their Java version alone cannot establish that they match the source.

## Potential Python and Arduino integration issues

- There is currently no socket, serial-port, process-message, camera, or Arduino code and no external integration dependency. A communication format and ownership of the serial port would need to be chosen in a later stage; two processes should not assume simultaneous ownership of one port.
- Blocking camera, serial, or network reads on Swing's event thread would freeze buttons and screen updates. Future device work would need to run separately, with UI actions scheduled on the event thread.
- Camera frames or noisy physical switches can repeat a single intended action many times. That would toggle cards back and forth, skip introduction screens, or submit extra answers. Future input needs one action per intentional gesture, plus handling for delayed or duplicate messages.
- Character positions are randomized each round. An old card index may refer to a different person after reset. Inputs need the current board/phase context rather than a permanently assigned character position.
- Projector coordinates, camera coordinates, window decorations, monitor placement, and Windows scaling are different coordinate systems. The fixed 800-by-500 content layout and 75-by-100 cards provide a reference, but eventual mapping must use the actual displayed geometry. No calibration was attempted.
- Future launchers must keep the Java version and working directory correct to preserve every image and sound. Disconnect/reconnect handling must avoid replaying stale actions. No Python packages, Arduino libraries, serial drivers, or hardware were tested.

Stage complete: documentation and inspection only. Runtime gameplay verification remains blocked by the available Java version. No fixes, redesign, computer vision, or hardware integration were started.

## Complete original-file inventory

The following paths are relative to the project root. They include every supplied file; the newly created analysis document is excluded.

```text
.classpath
.project
.settings\org.eclipse.core.resources.prefs
.settings\org.eclipse.jdt.core.prefs
100x100Button.png
166x100Button.png
250x100Button.png
300x100Button.png
600x100Button.png
62x100Button.png
83x100Button.png
AA04.png
AA04exclude.png
AA04target.png
AD01.png
AD01exclude.png
AD01target.png
AE05.png
AE05exclude.png
AE05target.png
AIDisguise.png
AIProfile.png
AIText.png
AX02.png
AX02exclude.png
AX02target.png
AY03.png
AY03exclude.png
AY03target.png
battery.png
BD07.png
BD07exclude.png
BD07target.png
bin\groupDGuessWho\AIQuestionSelector.class
bin\groupDGuessWho\Board.class
bin\groupDGuessWho\Board$1.class
bin\groupDGuessWho\Board$2.class
bin\groupDGuessWho\Board$3.class
bin\groupDGuessWho\Board$4.class
bin\groupDGuessWho\Board$5.class
bin\groupDGuessWho\Board$Player.class
bin\groupDGuessWho\Character.class
bin\groupDGuessWho\Character1.class
bin\groupDGuessWho\CharacterGuessingGame.class
bin\groupDGuessWho\Main.class
bin\groupDGuessWho\Main2.class
BL09.png
BL09exclude.png
BL09target.png
BN06.png
BN06exclude.png
BN06target.png
buttonBase.png
buttonClick.wav
BY08.png
BY08exclude.png
BY08target.png
CE12.png
CE12exclude.png
CE12target.png
characterClick.wav
CN10.png
CN10exclude.png
CN10target.png
CS11.png
CS11exclude.png
CS11target.png
DD14.png
DD14exclude.png
DD14target.png
DL13.png
DL13exclude.png
DL13target.png
EA15.png
EA15exclude.png
EA15target.png
EC16.png
EC16exclude.png
EC16target.png
FH17.png
FH17exclude.png
FH17target.png
FS18.png
FS18exclude.png
FS18target.png
GE19.png
GE19exclude.png
GE19target.png
GE20.png
GE20exclude.png
GE20target.png
healthDisplay.png
healthDisplayOff.png
healthDisplayOn.png
HN21.png
HN21exclude.png
HN21target.png
HY22.png
HY22exclude.png
HY22target.png
Intro.wav
Intro1.png
Intro2.png
Intro3.png
Intro4.png
JE23.png
JE23exclude.png
JE23target.png
KE25.png
KE25exclude.png
KE25target.png
LA26.png
LA26exclude.png
LA26target.png
LO27.png
LO27exclude.png
LO27target.png
lose.png
LY28.png
LY28exclude.png
LY28target.png
LZ29.png
LZ29exclude.png
LZ29target.png
MA30.png
MA30exclude.png
MA30target.png
MA32.png
MA32exclude.png
MA32target.png
ME33.png
ME33exclude.png
ME33target.png
MX31.png
MX31exclude.png
MX31target.png
NK34.png
NK34exclude.png
NK34target.png
PL36.png
PL36exclude.png
PL36target.png
playerProfile.png
playerTextLong.png
playerTextShort.png
PR37.png
PR37exclude.png
PR37target.png
QA35.png
QA35exclude.png
QA35target.png
RC24.png
RC24exclude.png
RC24target.png
revoke.png
src\groupDGuessWho\AIQuestionSelector.java
src\groupDGuessWho\Board.java
src\groupDGuessWho\Character1.java
src\groupDGuessWho\CharacterGuessingGame.java
src\groupDGuessWho\Main.java
src\groupDGuessWho\Main2.java
Suspicion_Matrix.wav
title.png
win.png
```
