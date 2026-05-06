param(
    [string]$Version = "",
    [ValidateSet("AUTOMATIC", "USER_MANAGED")]
    [string]$PublishingType = "AUTOMATIC",
    [string]$DeploymentName = "",
    [int]$PollSeconds = 10,
    [int]$MaxPolls = 90,
    [switch]$SkipPublishSigned
)

$ErrorActionPreference = "Stop"

function Get-BuildVersion {
    $buildSbt = Join-Path $PSScriptRoot "..\\build.sbt"
    $match = Select-String -Path $buildSbt -Pattern 'ThisBuild\s*/\s*version\s*:=\s*"([^"]+)"' | Select-Object -First 1
    if (-not $match) {
        throw "Konnte Version aus build.sbt nicht lesen."
    }
    return $match.Matches[0].Groups[1].Value
}

function Get-CentralCredentialValue {
    param(
        [string]$Key,
        [string]$EnvName
    )

    $envValue = [Environment]::GetEnvironmentVariable($EnvName)
    if ($envValue) {
        return $envValue
    }

    $credFile = Join-Path $HOME ".sbt\\sonatype_central_credentials"
    if (-not (Test-Path $credFile)) {
        throw "Credentials-Datei nicht gefunden: $credFile"
    }

    $match = Select-String -Path $credFile -Pattern "^$Key=(.*)$" | Select-Object -First 1
    if (-not $match) {
        throw "Eintrag '$Key' fehlt in $credFile"
    }
    return $match.Matches[0].Groups[1].Value
}

function Invoke-Curl {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $result = & curl.exe @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "curl fehlgeschlagen mit Exit-Code $LASTEXITCODE"
    }
    return ($result | Out-String).Trim()
}

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repoRoot

if (-not $Version) {
    $Version = Get-BuildVersion
}

if (-not $DeploymentName) {
    $DeploymentName = "com.anjunar:scalajs-jfx2:$Version"
}

$bundleDir = Join-Path $repoRoot "target\\sona-staging"
$bundleZip = Join-Path $repoRoot "target\\central-bundle-$Version.zip"

if (-not $SkipPublishSigned) {
    & sbt --batch "publishSigned"
    if ($LASTEXITCODE -ne 0) {
        throw "sbt publishSigned ist fehlgeschlagen."
    }
}

if (-not (Test-Path $bundleDir)) {
    throw "Bundle-Verzeichnis nicht gefunden: $bundleDir"
}

if (Test-Path $bundleZip) {
    Remove-Item $bundleZip -Force
}

Compress-Archive -Path (Join-Path $bundleDir "*") -DestinationPath $bundleZip -CompressionLevel Optimal

$user = Get-CentralCredentialValue -Key "user" -EnvName "SONATYPE_CENTRAL_USERNAME"
$password = Get-CentralCredentialValue -Key "password" -EnvName "SONATYPE_CENTRAL_PASSWORD"
$token = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${user}:${password}"))
$encodedName = [uri]::EscapeDataString($DeploymentName)
$uploadUrl = "https://central.sonatype.com/api/v1/publisher/upload?name=$encodedName&publishingType=$PublishingType"

Write-Host "Lade Bundle hoch: $bundleZip"
$deploymentId = Invoke-Curl -Arguments @(
    "--silent",
    "--show-error",
    "--fail",
    "--request", "POST",
    "--header", "Authorization: Bearer $token",
    "--form", "bundle=@$bundleZip",
    $uploadUrl
)

if (-not $deploymentId) {
    throw "Sonatype hat keine Deployment-ID zurueckgegeben."
}

Write-Host "Deployment-ID: $deploymentId"

for ($attempt = 1; $attempt -le $MaxPolls; $attempt++) {
    Start-Sleep -Seconds $PollSeconds

    $statusJson = Invoke-Curl -Arguments @(
        "--silent",
        "--show-error",
        "--fail",
        "--request", "POST",
        "--header", "Authorization: Bearer $token",
        "https://central.sonatype.com/api/v1/publisher/status?id=$deploymentId"
    )

    $status = $statusJson | ConvertFrom-Json
    Write-Host "[$attempt/$MaxPolls] Status: $($status.deploymentState)"

    if ($status.deploymentState -eq "PUBLISHED") {
        Write-Host "Maven Central Publish abgeschlossen."
        $status | ConvertTo-Json -Depth 10
        exit 0
    }

    if ($status.deploymentState -in @("FAILED", "VALIDATED")) {
        Write-Host "Deployment beendet mit Status $($status.deploymentState)."
        $status | ConvertTo-Json -Depth 10
        exit 1
    }
}

throw "Timeout beim Warten auf den Sonatype-Status. Deployment-ID: $deploymentId"
