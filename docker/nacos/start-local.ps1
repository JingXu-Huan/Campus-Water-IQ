[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$composeFile = Join-Path $projectRoot 'docker-compose.nacos.yml'
$seedScript = Join-Path $PSScriptRoot 'seed-nacos.ps1'
$healthUrl = 'http://127.0.0.1:8848/nacos/v1/console/health/readiness'

if (-not (Test-Path $composeFile)) {
    throw "Compose file does not exist: $composeFile"
}

Push-Location $projectRoot
try {
    & docker compose -f $composeFile config --quiet
    if ($LASTEXITCODE -ne 0) {
        throw 'Nacos compose configuration is invalid.'
    }

    & docker compose -f $composeFile up -d
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker Compose failed to start Nacos.'
    }
}
finally {
    Pop-Location
}

$ready = $false
for ($attempt = 1; $attempt -le 60; $attempt++) {
    try {
        $response = Invoke-WebRequest -Uri $healthUrl -TimeoutSec 3 -UseBasicParsing
        if ($response.StatusCode -eq 200) {
            $ready = $true
            break
        }
    }
    catch {
        # Nacos needs a few seconds to initialize its embedded database.
    }
    Start-Sleep -Seconds 2
}

if (-not $ready) {
    & docker compose -f $composeFile logs --tail=80 nacos
    throw "Nacos did not become ready: $healthUrl"
}

& $seedScript
Write-Host 'Local Nacos is ready at http://127.0.0.1:8848/nacos'
