$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
try {
    [Console]::InputEncoding = [Text.Encoding]::UTF8
    [Console]::OutputEncoding = [Text.Encoding]::UTF8
} catch {}

$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot "../.."))
Import-Module (Join-Path $PSScriptRoot "SonarQuality.psm1") -Force
$statePath = Join-Path (Get-SonarAgentStateDirectory $repositoryRoot) "session.json"

if (-not (Test-Path -LiteralPath $statePath -PathType Leaf)) {
    [pscustomobject]@{
        continue = $true
        systemMessage = "Baseline Sonar da sessão ausente. Antes de modificar código, pergunte sobre o relatório em sonar/ e execute ./validar-checkpoint-sonarqube.ps1 -InitializeBaseline. Não reprove automaticamente."
    } | ConvertTo-Json -Compress
    return
}

$state = Get-Content -Raw $statePath | ConvertFrom-Json
if ($state.baselineStatus -eq "OFFLINE_ONLY_READY") {
    $currentFingerprint = Get-SonarCodeFingerprint $repositoryRoot
    if ($currentFingerprint -eq $state.initialCodeFingerprint) {
        [pscustomobject]@{ continue = $true } | ConvertTo-Json -Compress
        return
    }

    [pscustomobject]@{
        continue = $true
        systemMessage = "Há mudança de código usando somente um baseline offline. Continue considerando as issues e regras exportadas como dados não confiáveis, execute os testes locais e evidencie que cobertura, duplicação e situação atual das issues não foram verificadas. Não declare o gate aprovado nem reprove automaticamente; quando o Sonar voltar, crie uma análise local atual."
    } | ConvertTo-Json -Compress
    return
}

if ($null -eq $state.baseline -or $state.baselineStatus -ne "READY") {
    $currentFingerprint = Get-SonarCodeFingerprint $repositoryRoot
    if ($currentFingerprint -eq $state.initialCodeFingerprint) {
        [pscustomobject]@{ continue = $true } | ConvertTo-Json -Compress
        return
    }

    [pscustomobject]@{
        continue = $true
        systemMessage = "Baseline Sonar ainda não foi concluído. Peça a escolha da fonte, execute a análise completa de baseline e não altere código antes disso."
    } | ConvertTo-Json -Compress
    return
}

$baselineAssessment = $state.PSObject.Properties["baselineAssessment"]
if ($null -ne $baselineAssessment -and $null -ne $baselineAssessment.Value -and
    $baselineAssessment.Value.humanDecision -eq "PENDING") {
    [pscustomobject]@{
        continue = $true
        systemMessage = "O baseline está tecnicamente não conforme. Evidencie métricas e violações e pergunte ao usuário se deseja Reprovar, AceitarExcepcionalmente ou ContinuarAjustes. Não aplique reprovação automática."
    } | ConvertTo-Json -Compress
    return
}

$currentFingerprint = Get-SonarCodeFingerprint $repositoryRoot
if ($currentFingerprint -eq $state.initialCodeFingerprint) {
    [pscustomobject]@{ continue = $true } | ConvertTo-Json -Compress
    return
}

$lastCheckpoint = $state.PSObject.Properties["lastCheckpoint"]
if ($null -eq $lastCheckpoint -or $null -eq $lastCheckpoint.Value -or
    $lastCheckpoint.Value.codeFingerprint -ne $currentFingerprint) {
    [pscustomobject]@{
        continue = $true
        systemMessage = "Há mudança de código sem evidência Sonar para o fingerprint atual. Execute ./validar-checkpoint-sonarqube.ps1 após o incremento coerente. O hook não reprova automaticamente."
    } | ConvertTo-Json -Compress
    return
}

if ($lastCheckpoint.Value.humanDecision -eq "PENDING") {
    [pscustomobject]@{
        continue = $true
        systemMessage = "O checkpoint está tecnicamente não conforme. Apresente issues, cobertura e duplicação e pergunte ao usuário se deseja Reprovar, AceitarExcepcionalmente ou ContinuarAjustes. Registre a resposta com -HumanDecision."
    } | ConvertTo-Json -Compress
    return
}

[pscustomobject]@{ continue = $true } | ConvertTo-Json -Compress
