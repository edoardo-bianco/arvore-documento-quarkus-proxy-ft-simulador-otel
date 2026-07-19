$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot "../.."))
$sessionHookPath = Join-Path $repositoryRoot ".codex/hooks/sonar-session-start.ps1"
$stopHookPath = Join-Path $repositoryRoot ".codex/hooks/sonar-stop.ps1"
$testRoot = Join-Path ([IO.Path]::GetTempPath()) `
    ("sonar-documentation-only-" + [guid]::NewGuid().ToString("N"))
$stateRoot = Join-Path $testRoot "state"

$previousToken = [Environment]::GetEnvironmentVariable("SONAR_TOKEN", "Process")
$previousStateDirectory = [Environment]::GetEnvironmentVariable(
    "SONAR_AGENT_STATE_DIRECTORY",
    "Process"
)

try {
    [Environment]::SetEnvironmentVariable("SONAR_TOKEN", $null, "Process")
    [Environment]::SetEnvironmentVariable(
        "SONAR_AGENT_STATE_DIRECTORY",
        $stateRoot,
        "Process"
    )

    $sessionInput = [pscustomobject]@{
        session_id = "session-documentation-only"
        cwd = $repositoryRoot
        hook_event_name = "SessionStart"
        source = "startup"
    } | ConvertTo-Json -Compress
    $sessionOutput = $sessionInput | & $sessionHookPath | ConvertFrom-Json

    if ($sessionOutput.systemMessage -notmatch "documenta") {
        throw "SessionStart deve informar que tarefas documentais dispensam o Sonar."
    }

    $stopInput = [pscustomobject]@{
        session_id = "session-documentation-only"
        cwd = $repositoryRoot
        hook_event_name = "Stop"
    } | ConvertTo-Json -Compress
    $stopOutput = $stopInput | & $stopHookPath | ConvertFrom-Json

    if ($null -ne $stopOutput.PSObject.Properties["systemMessage"]) {
        throw "Stop não deve cobrar baseline quando o fingerprint de código não mudou."
    }

    Write-Host "GREEN: tarefa exclusivamente documental dispensada do fluxo Sonar."
}
finally {
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
