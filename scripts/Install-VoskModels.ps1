[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$GameDirectory,
    [ValidateSet('es','de','ja')][string[]]$Languages = @('es','de','ja')
)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$models = @{
    es = 'vosk-model-small-es-0.42'
    de = 'vosk-model-small-de-0.15'
    ja = 'vosk-model-small-ja-0.22'
}
$gameRoot = [IO.Path]::GetFullPath($GameDirectory)
if (-not (Test-Path -LiteralPath $gameRoot -PathType Container)) { throw "No existe la instancia de Minecraft: $gameRoot" }
$modelRoot = Join-Path $gameRoot 'config\speakcraft\models'
New-Item -ItemType Directory -Force -Path $modelRoot | Out-Null
foreach ($language in $Languages) {
    $modelName = $models[$language]
    $destination = Join-Path $modelRoot $language
    if (Test-Path -LiteralPath $destination) {
        Write-Host "Se conserva el modelo existente: $destination"
        continue
    }
    $url = "https://alphacephei.com/vosk/models/$modelName.zip"
    Write-Host "Descarga solicitada: $url (modelo oficial, licencia Apache-2.0)"
    $staging = Join-Path $modelRoot ('.install-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $staging | Out-Null
    $archive = Join-Path $staging 'model.zip'
    Invoke-WebRequest -Uri $url -OutFile $archive -UseBasicParsing
    Expand-Archive -LiteralPath $archive -DestinationPath $staging
    $source = [IO.Path]::GetFullPath((Join-Path $staging $modelName))
    $safeRoot = [IO.Path]::GetFullPath($modelRoot).TrimEnd('\') + '\'
    $destination = [IO.Path]::GetFullPath($destination)
    if (-not $source.StartsWith($safeRoot,[StringComparison]::OrdinalIgnoreCase) -or
        -not $destination.StartsWith($safeRoot,[StringComparison]::OrdinalIgnoreCase)) { throw 'Ruta fuera de models' }
    if (-not (Test-Path -LiteralPath (Join-Path $source 'am\final.mdl'))) { throw 'El ZIP no contiene un modelo Vosk válido' }
    Move-Item -LiteralPath $source -Destination $destination
    # Leave the archive and its local fingerprint for auditing; no recursive deletion.
    $hash = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash
    "$url`nSHA256 descargado: $hash" | Set-Content -LiteralPath (Join-Path $destination 'download-source.txt') -Encoding UTF8
    Write-Host "Instalado: $destination"
}
Write-Host 'En Minecraft: F8, elegir idioma y micrófono, Probar voz, Guardar y reanudar.'
