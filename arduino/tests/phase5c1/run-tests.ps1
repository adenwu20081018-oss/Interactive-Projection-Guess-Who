param(
    [string]$AvrBin = (Join-Path $env:LOCALAPPDATA "Arduino15\packages\arduino\tools\avr-gcc\7.3.0-atmel3.6.1-arduino7\bin"),
    [string]$Node = 'node'
)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path "$PSScriptRoot\..\..\..").Path
$work = Join-Path $projectRoot 'tmp\phase5c1'
New-Item -ItemType Directory -Force $work | Out-Null
$archive = Join-Path $work 'avr8js.tgz'
if (-not (Test-Path $archive)) {
    Invoke-WebRequest 'https://registry.npmjs.org/avr8js/-/avr8js-0.21.0.tgz' -OutFile $archive
}
if ((Get-FileHash $archive -Algorithm SHA256).Hash -ne '4DCAF64ABFC5A3AD39BB03472810E1AE8914914EDD424623428DCAECA597731D') {
    throw 'Simulator archive hash mismatch.'
}
tar -xf $archive -C $work
if ($LASTEXITCODE -ne 0) { throw 'Simulator extraction failed.' }
& "$AvrBin\avr-g++.exe" -std=gnu++11 -Os -mmcu=atmega328p "-I$PSScriptRoot" "$PSScriptRoot\test_sketch.cpp" -o "$work\test_sketch.elf"
if ($LASTEXITCODE -ne 0) { throw 'Test harness compilation failed.' }
& "$AvrBin\avr-objcopy.exe" -O binary "$work\test_sketch.elf" "$work\test_sketch.bin"
if ($LASTEXITCODE -ne 0) { throw 'Test binary extraction failed.' }
& $Node "$PSScriptRoot\run-simulation.cjs" "$work\package" "$work\test_sketch.bin"
if ($LASTEXITCODE -ne 0) { throw 'Sketch tests failed.' }
