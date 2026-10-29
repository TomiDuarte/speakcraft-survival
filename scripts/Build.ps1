[CmdletBinding()]
param([ValidateSet('build','test','runGameTest','runClient','compileGametestJava')][string[]]$Tasks = @('build'))
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    $env:GRADLE_USER_HOME = Join-Path $projectRoot '.gradle-user'
    if (-not $env:JAVA_HOME) {
        $localJdk = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.tools') -Directory -ErrorAction SilentlyContinue |
            Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\javac.exe') } | Select-Object -First 1
        if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName }
    }
    if (-not $env:JAVA_HOME -and -not (Get-Command java -ErrorAction SilentlyContinue)) {
        throw 'Instalá un JDK 25 y configurá JAVA_HOME. Ver INSTRUCCIONES_INSTALACION.md.'
    }
    $env:DEBUG = ''
    & .\gradlew.bat @Tasks --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle terminó con código $LASTEXITCODE" }
} finally { Pop-Location }
