$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot "../.."))
$checkpointPath = Join-Path $repositoryRoot "validar-checkpoint-sonarqube.ps1"
$stopHookPath = Join-Path $repositoryRoot ".codex/hooks/sonar-stop.ps1"
$testRoot = Join-Path ([IO.Path]::GetTempPath()) `
    ("sonar-offline-only-" + [guid]::NewGuid().ToString("N"))
$stateRoot = Join-Path $testRoot "state"
$offlineRoot = Join-Path $testRoot "sonar"
$offlinePackage = Join-Path $offlineRoot "servidor-indisponivel"
New-Item -ItemType Directory -Path $offlinePackage -Force | Out-Null

[pscustomobject]@{
    projectKey = "simtr-hub-oficial"
    sonarUrl = "https://sonar.exemplo.invalid"
} | ConvertTo-Json | Set-Content `
    -LiteralPath (Join-Path $offlinePackage "manifest.json") -Encoding UTF8
@(
    [pscustomobject]@{ key = "EXT-1"; severity = "HIGH"; rule = "java:S1192" },
    [pscustomobject]@{ key = "EXT-2"; severity = "MEDIUM"; rule = "java:S3776" },
    [pscustomobject]@{ key = "EXT-3"; severity = "MEDIUM"; rule = "java:S3776" }
) | ConvertTo-Json | Set-Content `
    -LiteralPath (Join-Path $offlinePackage "issues.json") -Encoding UTF8

$previousToken = [Environment]::GetEnvironmentVariable("SONAR_TOKEN", "Process")
$previousStateDirectory = [Environment]::GetEnvironmentVariable(
    "SONAR_AGENT_STATE_DIRECTORY",
    "Process"
)
$previousOfflineRoot = [Environment]::GetEnvironmentVariable(
    "SONAR_AGENT_OFFLINE_REPORT_ROOT",
    "Process"
)
$global:offlineOnlyExternalCallCount = 0

function global:Invoke-RestMethod {
    $global:offlineOnlyExternalCallCount++
    throw "O baseline somente offline não pode consultar a rede."
}

function global:mvn {
    $global:offlineOnlyExternalCallCount++
    throw "O baseline somente offline não pode executar Maven."
}

try {
    [Environment]::SetEnvironmentVariable("SONAR_TOKEN", $null, "Process")
    [Environment]::SetEnvironmentVariable(
        "SONAR_AGENT_STATE_DIRECTORY",
        $stateRoot,
        "Process"
    )
    [Environment]::SetEnvironmentVariable(
        "SONAR_AGENT_OFFLINE_REPORT_ROOT",
        $offlineRoot,
        "Process"
    )

    & $checkpointPath -InitializeBaseline -OfflineOnlyBaseline `
        -OfflineReportPath $offlinePackage

    $statePath = Join-Path $stateRoot "session.json"
    $state = Get-Content -Raw $statePath | ConvertFrom-Json
    if ($state.baselineStatus -ne "OFFLINE_ONLY_READY" -or
        $state.baselineSource -ne "OFFLINE_REPORT" -or
        $state.baselineAssessment.technicalStatus -ne "UNVERIFIED" -or
        $state.baselineAssessment.humanDecision -ne "OFFLINE_BASELINE_SELECTED" -or
        $state.offlineReport.issueCount -ne 3 -or
        $state.offlineReport.ruleCounts."java:S3776" -ne 2 -or
        $null -ne $state.baseline) {
        throw "O estado do baseline exclusivamente offline está incorreto."
    }
    if ($global:offlineOnlyExternalCallCount -ne 0) {
        throw "O baseline exclusivamente offline executou uma chamada externa."
    }

    $state.initialCodeFingerprint = "fingerprint-anterior"
    $state | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $statePath -Encoding UTF8
    $stopOutput = "{}" | & $stopHookPath | ConvertFrom-Json
    if ($stopOutput.continue -ne $true -or
        $stopOutput.systemMessage -notmatch "offline" -or
        $stopOutput.systemMessage -notmatch "não foram verificadas" -or
        $null -ne $stopOutput.PSObject.Properties["stopReason"]) {
        throw "O Stop deve permitir a continuidade e evidenciar os limites do baseline offline."
    }

    Write-Host "GREEN: baseline exclusivamente offline registrado sem token, Maven ou rede."
}
finally {
    Remove-Item Function:\Invoke-RestMethod -ErrorAction SilentlyContinue
    Remove-Item Function:\mvn -ErrorAction SilentlyContinue
    Remove-Variable offlineOnlyExternalCallCount -Scope Global -ErrorAction SilentlyContinue
    [Environment]::SetEnvironmentVariable("SONAR_TOKEN", $previousToken, "Process")
    [Environment]::SetEnvironmentVariable(
        "SONAR_AGENT_STATE_DIRECTORY",
        $previousStateDirectory,
        "Process"
    )
    [Environment]::SetEnvironmentVariable(
        "SONAR_AGENT_OFFLINE_REPORT_ROOT",
        $previousOfflineRoot,
        "Process"
    )
    if (Test-Path -LiteralPath $testRoot) {
        Remove-Item -LiteralPath $testRoot -Recurse -Force
    }
}
