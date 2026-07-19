[CmdletBinding()]
param(
    [string]$ProjectKey = "simtr-hub-local",
    [string]$ProjectName = "simtr-hub-local",
    [string]$SonarUrl = "http://localhost:9000",
    [string]$MavenProfile = "",
    [double]$MinimumCoverage = 85.0,
    [double]$MaximumDuplication = 5.0,
    [int]$ComputeEngineTimeoutSec = 180,
    [string]$OfflineReportPath = "",
    [ValidateSet("", "Reprovar", "AceitarExcepcionalmente", "ContinuarAjustes")]
    [string]$HumanDecision = "",
    [switch]$UseGlobalMaven,
    [switch]$InitializeBaseline,
    [switch]$OfflineOnlyBaseline
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$modulePath = Join-Path $PSScriptRoot ".codex/hooks/SonarQuality.psm1"
Import-Module $modulePath -Force
$stateDirectory = Get-SonarAgentStateDirectory $PSScriptRoot
$statePath = Join-Path $stateDirectory "session.json"

function Write-TechnicalEvidence {
    param(
        [Parameter(Mandatory)][object]$Result,
        [string]$Label = "Checkpoint"
    )

    Write-Host "$Label - issues abertas: $($Result.IssueCount) (baseline: $($Result.BaselineIssueCount))"
    Write-Host "$Label - issues novas: $($Result.NewIssueKeys.Count)"
    Write-Host "$Label - HIGH/BLOCKER/CRITICAL: $($Result.HighOrBlockerIssueKeys.Count)"
    Write-Host "$Label - cobertura: $($Result.Coverage)% (mínimo: $MinimumCoverage%)"
    Write-Host "$Label - duplicação: $($Result.DuplicatedLinesDensity)% (máximo: $MaximumDuplication%)"
}

function New-Assessment {
    param([Parameter(Mandatory)][object]$Result)

    return [pscustomobject]@{
        technicalStatus = if ($Result.Passed) { "COMPLIANT" } else { "NON_COMPLIANT" }
        humanDecision = if ($Result.Passed) { "NOT_REQUIRED" } else { "PENDING" }
        decidedAt = $null
        issueCount = $Result.IssueCount
        baselineIssueCount = $Result.BaselineIssueCount
        newIssueKeys = @($Result.NewIssueKeys)
        highOrBlockerIssueKeys = @($Result.HighOrBlockerIssueKeys)
        coverage = $Result.Coverage
        duplicatedLinesDensity = $Result.DuplicatedLinesDensity
        violations = @($Result.Violations)
    }
}

function Set-PendingHumanDecision {
    param(
        [Parameter(Mandatory)][object]$State,
        [Parameter(Mandatory)][string]$Decision
    )

    $target = $null
    $lastCheckpointProperty = $State.PSObject.Properties["lastCheckpoint"]
    if ($null -ne $lastCheckpointProperty -and $null -ne $lastCheckpointProperty.Value -and
        $lastCheckpointProperty.Value.humanDecision -eq "PENDING") {
        $target = $lastCheckpointProperty.Value
    } else {
        $baselineProperty = $State.PSObject.Properties["baselineAssessment"]
        if ($null -ne $baselineProperty -and $null -ne $baselineProperty.Value -and
            $baselineProperty.Value.humanDecision -eq "PENDING") {
            $target = $baselineProperty.Value
        }
    }
    if ($null -eq $target) {
        throw "Não existe evidência com decisão humana pendente."
    }

    $target.humanDecision = switch ($Decision) {
        "Reprovar" { "REJECTED_BY_USER" }
        "AceitarExcepcionalmente" { "ACCEPTED_EXCEPTION" }
        "ContinuarAjustes" { "CONTINUE_ADJUSTMENTS" }
    }
    $target.decidedAt = [DateTimeOffset]::UtcNow.ToString("o")
}

if (-not [string]::IsNullOrWhiteSpace($HumanDecision)) {
    if (-not (Test-Path -LiteralPath $statePath -PathType Leaf)) {
        throw "Não existe estado Sonar para registrar a decisão humana."
    }
    $decisionState = Get-Content -Raw $statePath | ConvertFrom-Json
    Set-PendingHumanDecision -State $decisionState -Decision $HumanDecision
    Write-SonarAgentState -Path $statePath -State $decisionState
    Write-Host "Decisão humana registrada: $HumanDecision."
    return
}

if ([string]::IsNullOrWhiteSpace($ProjectKey) -or
    $ProjectKey -notmatch '^[A-Za-z0-9_.:-]+$' -or $ProjectKey -match '^\d+$') {
    throw "ProjectKey inválido."
}
if ($MinimumCoverage -lt 0 -or $MinimumCoverage -gt 100) {
    throw "MinimumCoverage deve estar entre 0 e 100."
}
if ($MaximumDuplication -lt 0 -or $MaximumDuplication -gt 100) {
    throw "MaximumDuplication deve estar entre 0 e 100."
}
$normalizedSonarUrl = Assert-SonarUrl -SonarUrl $SonarUrl
$offlineOnlyRequested = [bool]$OfflineOnlyBaseline
if ($offlineOnlyRequested -and -not $InitializeBaseline) {
    throw "OfflineOnlyBaseline exige -InitializeBaseline."
}
if ($offlineOnlyRequested -and [string]::IsNullOrWhiteSpace($OfflineReportPath)) {
    throw "OfflineOnlyBaseline exige -OfflineReportPath com um pacote dentro de sonar/."
}
if (-not $InitializeBaseline -and -not [string]::IsNullOrWhiteSpace($OfflineReportPath)) {
    throw "OfflineReportPath só pode ser usado com -InitializeBaseline."
}

if ($offlineOnlyRequested) {
    $offlineReport = Get-SonarOfflineReportSummary `
        -ReportPath $OfflineReportPath -RepositoryRoot $PSScriptRoot
    $existingState = if (Test-Path -LiteralPath $statePath -PathType Leaf) {
        Get-Content -Raw $statePath | ConvertFrom-Json
    } else {
        $null
    }
    $sessionId = if ($null -ne $existingState -and
        $null -ne $existingState.PSObject.Properties["sessionId"]) {
        [string]$existingState.sessionId
    } else {
        "manual"
    }
    $tokenAvailable = -not [string]::IsNullOrWhiteSpace(
        [Environment]::GetEnvironmentVariable("SONAR_TOKEN", "Process")
    )
    $state = [ordered]@{
        schemaVersion = 3
        sessionId = $sessionId
        projectKey = $ProjectKey
        sonarUrl = $normalizedSonarUrl
        startedAt = [DateTimeOffset]::UtcNow.ToString("o")
        initialCodeFingerprint = Get-SonarCodeFingerprint $PSScriptRoot
        tokenAvailable = $tokenAvailable
        baselineStatus = "OFFLINE_ONLY_READY"
        baselineSource = "OFFLINE_REPORT"
        baseline = $null
        baselineAssessment = [pscustomobject]@{
            technicalStatus = "UNVERIFIED"
            humanDecision = "OFFLINE_BASELINE_SELECTED"
            decidedAt = [DateTimeOffset]::UtcNow.ToString("o")
            limitations = @(
                "O servidor SonarQube atual não foi consultado.",
                "Cobertura e duplicação atuais não foram verificadas.",
                "O pacote offline não comprova o estado atual das issues."
            )
        }
        offlineReport = $offlineReport
        lastCheckpoint = $null
    }
    Write-SonarAgentState -Path $statePath -State $state
    Write-Host "Baseline exclusivamente offline registrado: $($offlineReport.path)"
    Write-Host "Issues exportadas: $($offlineReport.issueCount); severas: $($offlineReport.blockingIssueCount)"
    $ruleCount = @($offlineReport.ruleCounts.PSObject.Properties).Count
    Write-Host "Regras encontradas: $ruleCount"
    Write-Warning "Status UNVERIFIED: o pacote orienta a codificação, mas não aprova cobertura, duplicação nem o estado atual das issues."
    return
}

$token = [Environment]::GetEnvironmentVariable("SONAR_TOKEN", "Process")
if ([string]::IsNullOrWhiteSpace($token)) {
    throw @"
SONAR_TOKEN não está disponível no processo atual.
Não informe o token no chat. Inicie uma nova sessão por:

    ./iniciar-codex-com-sonar.ps1
"@
}

function Invoke-AnalysisSnapshot {
    $metadataPath = Join-Path $stateDirectory "report-task.txt"
    if (Test-Path -LiteralPath $metadataPath -PathType Leaf) {
        Remove-Item -LiteralPath $metadataPath -Force
    }

    $analysisArguments = @{
        ProjectKey = $ProjectKey
        ProjectName = $ProjectName
        SonarUrl = $normalizedSonarUrl
        MetadataFilePath = $metadataPath
    }
    if (-not [string]::IsNullOrWhiteSpace($MavenProfile)) {
        $analysisArguments["MavenProfile"] = $MavenProfile
    }
    if ($UseGlobalMaven) {
        $analysisArguments["UseGlobalMaven"] = $true
    }

    & (Join-Path $PSScriptRoot "analisar-sonarqube.ps1") @analysisArguments
    if (-not (Test-Path -LiteralPath $metadataPath -PathType Leaf)) {
        throw "Metadados da análise não encontrados em '$metadataPath'."
    }

    $reportTask = @{}
    foreach ($line in Get-Content -LiteralPath $metadataPath) {
        $separator = $line.IndexOf('=')
        if ($separator -gt 0) {
            $reportTask[$line.Substring(0, $separator)] = $line.Substring($separator + 1)
        }
    }
    if (-not $reportTask.ContainsKey("ceTaskUrl")) {
        throw "ceTaskUrl ausente nos metadados da análise."
    }

    Write-Host "Aguardando o processamento da análise pelo Compute Engine..."
    $ceTask = Wait-SonarComputeEngine -SonarUrl $normalizedSonarUrl `
        -TaskUrl $reportTask.ceTaskUrl -TimeoutSec $ComputeEngineTimeoutSec
    $snapshot = Get-SonarQualitySnapshot `
        -SonarUrl $normalizedSonarUrl -ProjectKey $ProjectKey
    return [pscustomobject]@{ ComputeEngineTask = $ceTask; Snapshot = $snapshot }
}

if ($InitializeBaseline) {
    $offlineReport = if (-not [string]::IsNullOrWhiteSpace($OfflineReportPath)) {
        Get-SonarOfflineReportSummary `
            -ReportPath $OfflineReportPath -RepositoryRoot $PSScriptRoot
    } else {
        $null
    }
    Write-Host "Executando análise completa antes de registrar o baseline..."
    $analysis = Invoke-AnalysisSnapshot
    $snapshot = $analysis.Snapshot
    $baselineResult = Compare-SonarQualitySnapshot -Baseline $snapshot -Current $snapshot `
        -MinimumCoverage $MinimumCoverage -MaximumDuplication $MaximumDuplication
    $existingState = if (Test-Path -LiteralPath $statePath -PathType Leaf) {
        Get-Content -Raw $statePath | ConvertFrom-Json
    } else {
        $null
    }
    $sessionId = if ($null -ne $existingState -and
        $null -ne $existingState.PSObject.Properties["sessionId"]) {
        [string]$existingState.sessionId
    } else {
        "manual"
    }
    $state = [ordered]@{
        schemaVersion = 3
        sessionId = $sessionId
        projectKey = $ProjectKey
        sonarUrl = $normalizedSonarUrl
        startedAt = [DateTimeOffset]::UtcNow.ToString("o")
        initialCodeFingerprint = Get-SonarCodeFingerprint $PSScriptRoot
        tokenAvailable = $true
        baselineStatus = "READY"
        baselineSource = "LOCAL_SONAR"
        baseline = $snapshot
        baselineAssessment = New-Assessment -Result $baselineResult
        offlineReport = $offlineReport
        lastCheckpoint = $null
    }
    Write-SonarAgentState -Path $statePath -State $state
    Write-TechnicalEvidence -Result $baselineResult -Label "Baseline"
    if ($null -ne $offlineReport) {
        Write-Host "Relatório offline considerado: $($offlineReport.path)"
        Write-Host "Issues externas exportadas: $($offlineReport.issueCount); severas: $($offlineReport.blockingIssueCount)"
        Write-Warning "O relatório offline é evidência imutável; ele não comprova o estado atual do servidor externo."
    }
    if (-not $baselineResult.Passed) {
        Write-Warning "O baseline está tecnicamente fora dos critérios. Evidencie a situação e pergunte ao usuário se deseja reprovar, aceitar excepcionalmente ou continuar ajustes. Nenhuma reprovação foi aplicada automaticamente."
    } else {
        Write-Host "Baseline tecnicamente conforme e registrado."
    }
    return
}

if (-not (Test-Path -LiteralPath $statePath -PathType Leaf)) {
    throw "Baseline da sessão ausente. Execute este script com -InitializeBaseline antes de alterar código."
}
$state = Get-Content -Raw $statePath | ConvertFrom-Json
if ($state.baselineStatus -eq "OFFLINE_ONLY_READY") {
    throw "O baseline atual é exclusivamente offline. Quando o SonarQube estiver disponível, execute novamente -InitializeBaseline com o mesmo -OfflineReportPath, sem -OfflineOnlyBaseline, para criar evidência local atual."
}
if ($null -eq $state.baseline -or $state.baselineStatus -ne "READY") {
    throw "Baseline Sonar não foi concluído para esta sessão."
}
if ($state.projectKey -ne $ProjectKey -or
    ([Uri]$state.sonarUrl).AbsoluteUri.TrimEnd('/') -ne $normalizedSonarUrl) {
    throw "ProjectKey ou SonarUrl não correspondem ao baseline da sessão."
}

$analysis = Invoke-AnalysisSnapshot
$current = $analysis.Snapshot
$result = Compare-SonarQualitySnapshot -Baseline $state.baseline -Current $current `
    -MinimumCoverage $MinimumCoverage -MaximumDuplication $MaximumDuplication
$fingerprint = Get-SonarCodeFingerprint $PSScriptRoot
$assessment = New-Assessment -Result $result
$state.lastCheckpoint = [pscustomobject]@{
    checkedAt = [DateTimeOffset]::UtcNow.ToString("o")
    codeFingerprint = $fingerprint
    technicalStatus = $assessment.technicalStatus
    humanDecision = $assessment.humanDecision
    decidedAt = $assessment.decidedAt
    computeEngineTaskId = [string]$analysis.ComputeEngineTask.id
    analysisKey = $current.analysisKey
    issueCount = $assessment.issueCount
    baselineIssueCount = $assessment.baselineIssueCount
    newIssueKeys = @($assessment.newIssueKeys)
    highOrBlockerIssueKeys = @($assessment.highOrBlockerIssueKeys)
    coverage = $assessment.coverage
    duplicatedLinesDensity = $assessment.duplicatedLinesDensity
    violations = @($assessment.violations)
    offlineReportPath = if ($null -eq $state.offlineReport) { $null } else { $state.offlineReport.path }
    offlineEvidenceStatus = if ($null -eq $state.offlineReport) { "NOT_SELECTED" } else { "REFERENCE_ONLY" }
}
Write-SonarAgentState -Path $statePath -State $state
Write-TechnicalEvidence -Result $result

if ($result.Passed) {
    Write-Host "Situação técnica conforme. Nenhuma reprovação automática foi aplicada."
} else {
    $details = ($result.Violations | ForEach-Object {
        "[$($_.Code)] $($_.Message)"
    }) -join [Environment]::NewLine
    Write-Warning "Situação técnica não conforme:`n$details"
    Write-Warning "DECISÃO HUMANA PENDENTE: pergunte ao usuário se deseja Reprovar, AceitarExcepcionalmente ou ContinuarAjustes."
}
