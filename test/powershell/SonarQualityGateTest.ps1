$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot "../.."))
$modulePath = Join-Path $repositoryRoot ".codex/hooks/SonarQuality.psm1"
Import-Module $modulePath -Force

function New-Issue {
    param(
        [Parameter(Mandatory)][string]$Key,
        [string]$Severity = "MAJOR",
        [string[]]$ImpactSeverities = @("MEDIUM")
    )

    $impacts = @($ImpactSeverities | ForEach-Object {
        [pscustomobject]@{ softwareQuality = "MAINTAINABILITY"; severity = $_ }
    })
    return [pscustomobject]@{ key = $Key; severity = $Severity; impacts = $impacts }
}

$baseline = [pscustomobject]@{
    issueCount = 2
    issues = @(
        (New-Issue -Key "I1"),
        (New-Issue -Key "I2" -Severity "MINOR" -ImpactSeverities @("LOW"))
    )
    metrics = [pscustomobject]@{ coverage = 86.2; duplicatedLinesDensity = 4.8 }
}

$passing = Compare-SonarQualitySnapshot -Baseline $baseline -Current $baseline
if (-not $passing.Passed -or $passing.Violations.Count -ne 0) {
    throw "Um snapshot dentro dos limites não foi aprovado."
}

$current = [pscustomobject]@{
    issueCount = 4
    issues = @(
        (New-Issue -Key "I1"),
        (New-Issue -Key "I2" -Severity "MINOR" -ImpactSeverities @("LOW")),
        (New-Issue -Key "I3" -ImpactSeverities @("HIGH")),
        (New-Issue -Key "I4" -Severity "BLOCKER" -ImpactSeverities @())
    )
    metrics = [pscustomobject]@{ coverage = 84.9; duplicatedLinesDensity = 5.1 }
}

$failing = Compare-SonarQualitySnapshot -Baseline $baseline -Current $current
$violationCodes = @($failing.Violations | ForEach-Object Code)
foreach ($expectedCode in @("NEW_ISSUES", "HIGH_OR_BLOCKER", "COVERAGE", "DUPLICATION")) {
    if ($violationCodes -notcontains $expectedCode) {
        throw "Violação esperada ausente: $expectedCode"
    }
}
if ($failing.Passed -or $failing.NewIssueKeys.Count -ne 2 -or
    $failing.HighOrBlockerIssueKeys.Count -ne 2) {
    throw "O gate não contabilizou corretamente issues novas e severas."
}

$critical = [pscustomobject]@{
    issueCount = 2
    issues = @((New-Issue -Key "I1"), (New-Issue -Key "I2" -Severity "CRITICAL"))
    metrics = [pscustomobject]@{ coverage = 90; duplicatedLinesDensity = 1 }
}
$criticalResult = Compare-SonarQualitySnapshot -Baseline $baseline -Current $critical
if ($criticalResult.HighOrBlockerIssueKeys -notcontains "I2") {
    throw "Severidade CRITICAL legada deve ser bloqueante por compatibilidade."
}

Write-Host "GREEN: critérios do gate SonarQube aprovados."
