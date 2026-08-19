[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://127.0.0.1:8848/nacos'
)

$ErrorActionPreference = 'Stop'

if ($env:NACOS_CONSOLE_URL) {
    $BaseUrl = $env:NACOS_CONSOLE_URL.TrimEnd('/')
}

$BaseUrl = $BaseUrl.TrimEnd('/')
$namespace = $env:NACOS_NAMESPACE
$configDirectory = Join-Path $PSScriptRoot 'config'

if (-not (Test-Path $configDirectory)) {
    throw "Nacos config directory does not exist: $configDirectory"
}

$files = Get-ChildItem -Path $configDirectory -Filter '*.yml' -File | Sort-Object Name
if ($files.Count -eq 0) {
    throw "No Nacos YAML config files found in $configDirectory"
}

foreach ($file in $files) {
    $dataId = $file.Name
    $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8
    $form = @{
        dataId  = $dataId
        group   = 'DEFAULT_GROUP'
        content = $content
        type    = 'yaml'
    }

    $query = "dataId=$([Uri]::EscapeDataString($dataId))&group=DEFAULT_GROUP"
    if (-not [string]::IsNullOrWhiteSpace($namespace)) {
        $form.tenant = $namespace
        $query += "&tenant=$([Uri]::EscapeDataString($namespace))"
    }

    $result = $null
    for ($attempt = 1; $attempt -le 10; $attempt++) {
        try {
            $result = Invoke-RestMethod -Method Post -Uri "$BaseUrl/v1/cs/configs" -Body $form -ContentType 'application/x-www-form-urlencoded'
            if ("$result" -eq 'true') {
                break
            }
        }
        catch {
            if ($attempt -eq 10) {
                throw
            }
        }
        Start-Sleep -Seconds 1
    }

    if ("$result" -ne 'true') {
        throw "Nacos rejected config $dataId. Response: $result"
    }

    $stored = $null
    for ($attempt = 1; $attempt -le 10; $attempt++) {
        try {
            $stored = Invoke-RestMethod -Method Get -Uri "$BaseUrl/v1/cs/configs?$query"
            if (-not [string]::IsNullOrWhiteSpace("$stored")) {
                break
            }
        }
        catch {
            if ($attempt -eq 10) {
                throw
            }
        }
        Start-Sleep -Seconds 1
    }

    if ([string]::IsNullOrWhiteSpace("$stored")) {
        throw "Nacos returned an empty config after writing $dataId"
    }

    Write-Host "Seeded Nacos config: $dataId"
}
