param([switch]$Test)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    & .\build-phase5b2.ps1 -Test:$Test
    if ($Test) {
        $classpath = 'build-phase5b2;arduino\java\lib\jSerialComm-2.11.4.jar'
        & java -cp $classpath groupDGuessWho.PresentationTest
        if ($LASTEXITCODE -ne 0) { throw 'Presentation tests failed' }
        & java -cp $classpath groupDGuessWho.ArduinoFeedbackTest --presentation
        if ($LASTEXITCODE -ne 0) { throw 'Presentation game/LED regression failed' }
    }
    Write-Host 'Phase 7A build complete (classes in build-phase5b2).'
} finally { Pop-Location }
