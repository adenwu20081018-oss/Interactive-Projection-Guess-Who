# Phase 4A — Original Java game build and launch verification

Verified October 26, 2026. No existing files were modified. No compatibility fix was needed. No Python communication or Arduino work was performed.

## Build

Read docs/existing-game-analysis.md and the Eclipse project metadata. Rechecked Java entry point, imports, Board startup, and resource references. Eclipse uses src as source, bin as output, UTF-8, and Java SE 17. All dependencies are standard Java APIs (Swing/AWT, Java Sound, collections and file I/O); no external JARs are required.

Installed JDK: C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot
Runtime reports Temurin OpenJDK 17.0.20.1+1. Java was not on PATH in the agent's existing shell, so full executable paths were used.

Compiled all six Java source files with javac --release 17 and UTF-8 into build-phase4a. Compiler exit code 0; no diagnostics. Generated 13 class files. Original bin files and all Java sources were preserved.

All 138 distinct PNG/WAV filenames referenced by Board exist. All 137 supplied PNG files decoded successfully with System.Drawing. All four supplied WAV files have RIFF/WAVE headers. Audio code produced no runtime errors during the observed startup and clicks; audible playback quality was not independently verified.

## Launch and GUI check

Launched the freshly built groupDGuessWho.Main from the project root. Visually confirmed the original introduction screens and the 24-character board with character art, health displays, and controls. Mouse clicks advanced the introduction. Clicking KE25 changed its exclusion appearance; clicking again restored it. Left the game open at the initial choose-your-agent board.

stdout.log and stderr.log in build-phase4a remained empty after this check. This was a startup/UI smoke check, not a complete game playthrough. No automated Java test suite is supplied.

## Exact PowerShell commands

```powershell
Set-Location 'C:\path\to\Interactive-Projection-Guess-Who'
$jdk17 = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
& "$jdk17\bin\javac.exe" -encoding UTF-8 --release 17 -d build-phase4a src\groupDGuessWho\*.java
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
& "$jdk17\bin\java.exe" -cp build-phase4a groupDGuessWho.Main
```

To launch again without recompiling, run Set-Location, assign $jdk17, and run only the final java command. Keep the working directory at this project root because images and audio use relative filenames. Close the already-open game first if you want to avoid two instances and overlapping music. If you update the JDK later, adjust the versioned installation path.

## Remaining limitations

No build or startup blocker was found. The pre-existing logic issues recorded in Phase 0 (including brown-hair capitalization and reset arrays that hide fields) remain unchanged, as requested. They do not prevent startup, but this check does not certify all rounds, win/lose paths, answer correction, or sound quality. Play a complete mouse-controlled game to verify those flows. Eclipse itself was not launched; its existing Java 17 configuration is consistent with the successful command-line build.

New artifacts: this report, build-phase4a/groupDGuessWho/*.class (13 files), build-phase4a/stdout.log and build-phase4a/stderr.log. No existing game, resource, Eclipse configuration, or computer-vision file was changed. Stopped after Phase 4A.
