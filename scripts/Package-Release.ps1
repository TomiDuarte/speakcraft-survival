[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$versionLine = Get-Content -LiteralPath (Join-Path $root 'gradle.properties') | Where-Object { $_ -match '^mod_version=' }
$version = ($versionLine -split '=',2)[1].Trim()
if ($version -notmatch '^\d+\.\d+\.\d+([-.][a-zA-Z0-9.]+)?$') { throw 'Invalid release version' }
$jarName = "speakcraft-survival-$version.jar"
$jar = Join-Path $root "build\libs\$jarName"
if (-not (Test-Path -LiteralPath $jar)) { throw 'Run gradlew.bat build first' }
$out = Join-Path $root "dist\$version"
New-Item -ItemType Directory -Force -Path $out | Out-Null
Copy-Item -LiteralPath $jar -Destination (Join-Path $out $jarName) -Force
$zipPath = Join-Path $out "SpeakCraftSurvival-$version-windows.zip"
$files = @('README.md','INSTRUCCIONES_INSTALACION.md','LICENSE','THIRD_PARTY_NOTICES.md','CHANGELOG.md','VERIFICACION.md','CONTRIBUTING.md','docs/JAPONES.md','docs/images/japanese-guide.png','verification/README.md','verification/language-sources.json','scripts/readings/coverage.json','scripts/Install-VoskModels.ps1','licenses/Apache-2.0.txt','licenses/SudachiDict-LEGAL.txt')
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$stream = [IO.File]::Open($zipPath,[IO.FileMode]::Create,[IO.FileAccess]::Write)
$zip = New-Object IO.Compression.ZipArchive($stream,[IO.Compression.ZipArchiveMode]::Create)
try {
    [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$jar,$jarName) | Out-Null
    foreach ($file in $files) {
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,(Join-Path $root $file),$file) | Out-Null
    }
} finally { $zip.Dispose(); $stream.Dispose() }
$lines = foreach ($file in @((Join-Path $out $jarName),$zipPath)) {
    $hash = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  $([IO.Path]::GetFileName($file))"
}
[IO.File]::WriteAllLines((Join-Path $out 'SHA256SUMS.txt'),$lines,[Text.UTF8Encoding]::new($false))
Write-Output "Release files: $out"
Get-ChildItem -LiteralPath $out -File | Select-Object Name,Length
