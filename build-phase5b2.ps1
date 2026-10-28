param([switch]$Test)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    # Reuse the pinned dependency download/check and standalone tests from 5B-1.
    & .\arduino\java\build.ps1 -Test:$Test
    $serialJar = 'arduino\java\lib\jSerialComm-2.11.4.jar'
    New-Item -ItemType Directory -Force 'build-phase5b2' | Out-Null
    $gameSources = @(Get-ChildItem 'src\groupDGuessWho\*.java' | ForEach-Object FullName)
    & javac -encoding UTF-8 --release 17 -cp $serialJar -d build-phase5b2 @gameSources arduino\java\src\phase5b1\ArduinoSerialController.java
    if ($LASTEXITCODE -ne 0) { throw 'Game compilation failed.' }
    if ($Test) {
        $testSources = @(Get-ChildItem 'tests\java\groupDGuessWho\*.java' | ForEach-Object FullName)
        & javac -encoding UTF-8 --release 17 -cp "build-phase5b2;$serialJar" -d build-phase5b2 @testSources
        if ($LASTEXITCODE -ne 0) { throw 'Game test compilation failed.' }
        foreach ($mode in @('--self-test', '--swing-test')) {
            & java -cp "build-phase5b2;$serialJar" groupDGuessWho.ControlTestHarness $mode
            if ($LASTEXITCODE -ne 0) { throw "Existing Java test failed: $mode" }
        }
        & java -cp "build-phase5b2;$serialJar" groupDGuessWho.ArduinoFeedbackTest
        if ($LASTEXITCODE -ne 0) { throw 'Arduino feedback tests failed.' }
    }
    Write-Host 'Phase 5B-2 build complete. See docs/phase5b2-game-arduino-feedback.md.'
} finally { Pop-Location }
