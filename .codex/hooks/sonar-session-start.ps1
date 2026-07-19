$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
try {
    [Console]::InputEncoding = [Text.Encoding]::UTF8
    [Console]::OutputEncoding = [Text.Encoding]::UTF8
} catch {}

$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot "../.."))
Import-Module (Join-Path $PSScriptRoot "SonarQuality.psm1") -Force

$rawInput = [Console]::In.ReadToEnd()
if ([string]::IsNullOrWhiteSpace($rawInput)) {
    $rawInput = (@($input) -join [Environment]::NewLine)
}
$inputData = if ([string]::IsNullOrWhiteSpace($rawInput)) {
    [pscustomobject]@{}
} else {
    $rawInput | ConvertFrom-Json
}
$sessionIdProperty = $inputData.PSObject.Properties["session_id"]
$sessionId = if ($null -eq $sessionIdProperty) { "unknown" } else { [string]$sessionIdProperty.Value }
$statePath = Join-Path (Get-SonarAgentStateDirectory $repositoryRoot) "session.json"
$fingerprint = Get-SonarCodeFingerprint $repositoryRoot
$token = [Environment]::GetEnvironmentVariable("SONAR_TOKEN", "Process")

$state = [ordered]@{
    schemaVersion = 1
    sessionId = $sessionId
    projectKey = "simtr-hub-local"
    sonarUrl = "http://localhost:9000"
    startedAt = [DateTimeOffset]::UtcNow.ToString("o")
    initialCodeFingerprint = $fingerprint
    tokenAvailable = -not [string]::IsNullOrWhiteSpace($token)
    baseline = $null
    baselineAssessment = $null
    offlineReport = $null
    lastCheckpoint = $null
    baselineStatus = "NOT_REQUIRED_UNTIL_CODE_CHANGE"
}

if ([string]::IsNullOrWhiteSpace($token)) {
    Write-SonarAgentState -Path $statePath -State $state
    [pscustomobject]@{
        continue = $true
        systemMessage = "Tarefas exclusivamente documentais dispensam token, baseline e checkpoint Sonar. Se o pedido exigir código, verifique os pacotes em sonar/: com autorização humana, um pacote pode ser registrado sem token por -InitializeBaseline -OfflineOnlyBaseline -OfflineReportPath; caso contrário, não aceite token no chat e oriente uma nova sessão com ./iniciar-codex-com-sonar.ps1."
    } | ConvertTo-Json -Compress
    return
}

Write-SonarAgentState -Path $statePath -State $state

$message = "Tarefas exclusivamente documentais dispensam o fluxo Sonar. Se o pedido exigir código, verifique os pacotes em sonar/ e pergunte se deve usar Sonar local, local mais pacote ou somente pacote offline. O modo somente offline usa -InitializeBaseline -OfflineOnlyBaseline -OfflineReportPath e permanece UNVERIFIED."
[pscustomobject]@{ continue = $true; systemMessage = $message } | ConvertTo-Json -Compress
