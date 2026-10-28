param([switch]$Test)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $jar = Join-Path $PSScriptRoot 'lib\jSerialComm-2.11.4.jar'
    $expectedHash = 'B5F7EC40D622864B92DC679159CDBAA3030D37884F0AF2DEEF45B56C16BDA7A6'
    New-Item -ItemType Directory -Force 'lib', 'build' | Out-Null
    if (-not (Test-Path -LiteralPath $jar)) {
        Write-Host 'Downloading jSerialComm 2.11.4 from Maven Central...'
        Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/com/fazecast/jSerialComm/2.11.4/jSerialComm-2.11.4.jar' -OutFile $jar
    }
    if ((Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash -ne $expectedHash) {
        throw "Dependency checksum mismatch: $jar. Remove that JAR and rerun to download a fresh copy."
    }
    & javac --release 17 -cp $jar -d build src\phase5b1\ArduinoSerialController.java src\phase5b1\ArduinoSerialTest.java
    if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
    if ($Test) {
        & javac --release 17 -cp "build;$jar" -d build tests\phase5b1\ArduinoSerialControllerTest.java
        if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
        & java -cp "build;$jar" phase5b1.ArduinoSerialControllerTest
        if ($LASTEXITCODE -ne 0) { throw 'Hardware-free tests failed.' }
    }
    Write-Host 'Build complete. See docs/phase5b1-java-arduino-test.md for run commands.'
} finally {
    Pop-Location
}
