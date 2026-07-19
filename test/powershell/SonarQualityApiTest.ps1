$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot "../.."))
$modulePath = Join-Path $repositoryRoot ".codex/hooks/SonarQuality.psm1"
$sessionHookPath = Join-Path $repositoryRoot ".codex/hooks/sonar-session-start.ps1"
$testRoot = Join-Path ([IO.Path]::GetTempPath()) ("sonar-api-test-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $testRoot | Out-Null

$previousToken = [Environment]::GetEnvironmentVariable("SONAR_TOKEN", "Process")
$previousStateDirectory = [Environment]::GetEnvironmentVariable("SONAR_AGENT_STATE_DIRECTORY", "Process")
$global:capturedQualityRequests = [System.Collections.Generic.List[string]]::new()

function global:Invoke-RestMethod {
    param($Method, $Uri, $Headers, $TimeoutSec)

    if ($Headers.Authorization -ne "Bearer credencial-sintetica") {
        throw "Header de autenticação incorreto."
    }
    if ([string]$Uri -match "credencial-sintetica") {
        throw "Token encontrado na URL."
    }
    $global:capturedQualityRequests.Add([string]$Uri)

    if ($Uri -match "/api/project_analyses/search") {
        return [pscustomobject]@{
            analyses = @([pscustomobject]@{ key = "A1"; revision = "abc123" })
        }
    }
    if ($Uri -match "/api/measures/component") {
        return [pscustomobject]@{
            component = [pscustomobject]@{
                measures = @(
                    [pscustomobject]@{ metric = "coverage"; value = "86.5" },
                    [pscustomobject]@{ metric = "duplicated_lines_density"; value = "4.5" }
                )
            }
        }
    }
    if ($Uri -match "/api/issues/search") {
        return [pscustomobject]@{
            paging = [pscustomobject]@{ total = 1 }
            issues = @([pscustomobject]@{
                key = "I1"
                rule = "java:S1"
                severity = "MAJOR"
                impacts = @([pscustomobject]@{
                    softwareQuality = "MAINTAINABILITY"
                    severity = "MEDIUM"
                })
            })
        }
    }
    if ($Uri -match "/api/ce/task") {
        return [pscustomobject]@{ task = [pscustomobject]@{ id = "CE1"; status = "SUCCESS" } }
    }
    throw "Endpoint simulado ausente: $Uri"
}

try {
    [Environment]::SetEnvironmentVariable("SONAR_TOKEN", "credencial-sintetica", "Process")
    [Environment]::SetEnvironmentVariable("SONAR_AGENT_STATE_DIRECTORY", $testRoot, "Process")
    Import-Module $modulePath -Force

    $snapshot = Get-SonarQualitySnapshot `
        -SonarUrl "http://localhost:9000" -ProjectKey "simtr-hub-local"
    if ($snapshot.issueCount -ne 1 -or $snapshot.metrics.coverage -ne 86.5 -or
        $snapshot.metrics.duplicatedLinesDensity -ne 4.5) {
        throw "Snapshot Sonar incorreto."
    }
    if (($global:capturedQualityRequests -join " ") -notmatch "resolved=false") {
        throw "A consulta de issues não restringiu o snapshot a issues abertas."
    }

    $ceTask = Wait-SonarComputeEngine `
        -SonarUrl "http://localhost:9000" `
        -TaskUrl "http://localhost:9000/api/ce/task?id=CE1" `
        -TimeoutSec 5
    if ($ceTask.status -ne "SUCCESS") {
        throw "A espera do Compute Engine não retornou sucesso."
    }

    $sessionInput = [pscustomobject]@{
        session_id = "session-api-test"
        cwd = $repositoryRoot
        hook_event_name = "SessionStart"
        source = "startup"
    } | ConvertTo-Json -Compress
    $sessionOutput = $sessionInput | & $sessionHookPath | ConvertFrom-Json
    $sessionState = Get-Content -Raw (Join-Path $testRoot "session.json") | ConvertFrom-Json
    if (-not $sessionOutput.continue -or $null -ne $sessionState.baseline -or
        $sessionState.tokenAvailable -ne $true -or
        $sessionOutput.systemMessage -notmatch "InitializeBaseline") {
        throw "O SessionStart deve aguardar a escolha humana antes do baseline."
    }

    $stateText = Get-Content -Raw (Join-Path $testRoot "session.json")
    if ($stateText -match "credencial-sintetica") {
        throw "O token foi persistido no estado do agente."
    }

    Write-Host "GREEN: API, Compute Engine e seleção prévia do baseline aprovados."
}
finally {
    Remove-Item Function:\Invoke-RestMethod -ErrorAction SilentlyContinue
    Remove-Variable capturedQualityRequests -Scope Global -ErrorAction SilentlyContinue
    [Environment]::SetEnvironmentVariable("SONAR_TOKEN", $previousToken, "Process")
    [Environment]::SetEnvironmentVariable(
        "SONAR_AGENT_STATE_DIRECTORY",
        $previousStateDirectory,
        "Process"
    )
    if (Test-Path -LiteralPath $testRoot) {
        Remove-Item -LiteralPath $testRoot -Recurse -Force
    }
}
