$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot "../.."))
$hooksPath = Join-Path $repositoryRoot ".codex/hooks.json"
$sessionHookPath = Join-Path $repositoryRoot ".codex/hooks/sonar-session-start.ps1"
$stopHookPath = Join-Path $repositoryRoot ".codex/hooks/sonar-stop.ps1"
$qualityModulePath = Join-Path $repositoryRoot ".codex/hooks/SonarQuality.psm1"
$checkpointPath = Join-Path $repositoryRoot "validar-checkpoint-sonarqube.ps1"
$launcherPath = Join-Path $repositoryRoot "iniciar-codex-com-sonar.ps1"
Import-Module $qualityModulePath -Force

$hooks = Get-Content -Raw $hooksPath | ConvertFrom-Json
foreach ($eventName in @("SessionStart", "Stop")) {
    $groups = @($hooks.hooks.$eventName)
    if ($groups.Count -ne 1 -or @($groups[0].hooks).Count -ne 1) {
        throw "Configuração inesperada para o evento $eventName."
    }
    if ([string]::IsNullOrWhiteSpace($groups[0].hooks[0].commandWindows)) {
        throw "O evento $eventName não possui comando Windows."
    }
}

foreach ($scriptPath in @($sessionHookPath, $stopHookPath, $checkpointPath, $launcherPath)) {
    $parseErrors = $null
    [System.Management.Automation.Language.Parser]::ParseFile(
        $scriptPath,
        [ref]$null,
        [ref]$parseErrors
    ) | Out-Null
    if ($parseErrors.Count -ne 0) {
        throw "Falha de sintaxe em $scriptPath."
    }
}

$testRoot = Join-Path ([IO.Path]::GetTempPath()) ("sonar-hook-test-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $testRoot | Out-Null
$previousToken = [Environment]::GetEnvironmentVariable("SONAR_TOKEN", "Process")
$previousStateDirectory = [Environment]::GetEnvironmentVariable("SONAR_AGENT_STATE_DIRECTORY", "Process")
try {
    [Environment]::SetEnvironmentVariable("SONAR_TOKEN", $null, "Process")
    [Environment]::SetEnvironmentVariable("SONAR_AGENT_STATE_DIRECTORY", $testRoot, "Process")

    $hookInput = [pscustomobject]@{
        session_id = "session-test"
        cwd = $repositoryRoot
        hook_event_name = "SessionStart"
        source = "startup"
    } | ConvertTo-Json -Compress

    $hookOutput = $hookInput | & $sessionHookPath | ConvertFrom-Json
    if (-not $hookOutput.continue -or
        $hookOutput.systemMessage -notmatch "documenta" -or
        $hookOutput.systemMessage -notmatch "iniciar-codex-com-sonar") {
        throw "O hook não diferenciou documentação da inicialização segura para código."
    }
    if ($hookOutput.systemMessage -match "sqp_[A-Za-z0-9]+") {
        throw "O hook expôs um token na saída."
    }

    $state = Get-Content -Raw (Join-Path $testRoot "session.json") | ConvertFrom-Json
    if ($state.sessionId -ne "session-test" -or $state.tokenAvailable -ne $false) {
        throw "O estado de sessão sem token está incorreto."
    }

    $state.initialCodeFingerprint = "fingerprint-diferente"
    $state | ConvertTo-Json -Depth 20 | Set-Content `
        -LiteralPath (Join-Path $testRoot "session.json") -Encoding UTF8
    $firstStop = "{}" | & $stopHookPath | ConvertFrom-Json
    if ($firstStop.continue -ne $true -or
        $firstStop.systemMessage -notmatch "não foi concluído" -or
        $null -ne $firstStop.PSObject.Properties["stopReason"]) {
        throw "O Stop deve evidenciar a ausência do baseline sem reprovar automaticamente."
    }

    $launcherText = Get-Content -Raw $launcherPath
    if ($launcherText -match "param\s*\([^)]*SONAR_TOKEN" -or
        $launcherText -notmatch "AsSecureString") {
        throw "O inicializador deve solicitar o token de modo seguro e não aceitá-lo por parâmetro."
    }

    $fingerprintRoot = Join-Path $testRoot "fingerprint"
    $fingerprintSource = Join-Path $fingerprintRoot "src/main"
    $fingerprintHooks = Join-Path $fingerprintRoot ".codex/hooks"
    $fingerprintTests = Join-Path $fingerprintRoot "test/powershell"
    New-Item -ItemType Directory `
        -Path $fingerprintSource,$fingerprintHooks,$fingerprintTests -Force | Out-Null
    Set-Content -LiteralPath (Join-Path $fingerprintSource "codigo.txt") -Value "codigo"
    Set-Content -LiteralPath (Join-Path $fingerprintRoot "pom.xml") -Value "pom"
    Set-Content -LiteralPath (Join-Path $fingerprintRoot "executar.ps1") -Value "script-1"
    Set-Content -LiteralPath (Join-Path $fingerprintHooks "hook.ps1") -Value "hook-1"
    Set-Content -LiteralPath (Join-Path $fingerprintTests "verificar.ps1") -Value "teste-1"
    Set-Content -LiteralPath (Join-Path $fingerprintRoot ".codex/hooks.json") -Value '{"v":1}'

    $initialFingerprint = Get-SonarCodeFingerprint $fingerprintRoot
    Set-Content -LiteralPath (Join-Path $fingerprintRoot "executar.ps1") -Value "script-2"
    $scriptFingerprint = Get-SonarCodeFingerprint $fingerprintRoot
    if ($scriptFingerprint -eq $initialFingerprint) {
        throw "O fingerprint não detectou alteração em script executável da raiz."
    }

    Set-Content -LiteralPath (Join-Path $fingerprintHooks "hook.ps1") -Value "hook-2"
    $hookFingerprint = Get-SonarCodeFingerprint $fingerprintRoot
    if ($hookFingerprint -eq $scriptFingerprint) {
        throw "O fingerprint não detectou alteração em hook executável."
    }

    Set-Content -LiteralPath (Join-Path $fingerprintRoot ".codex/hooks.json") -Value '{"v":2}'
    $configurationFingerprint = Get-SonarCodeFingerprint $fingerprintRoot
    if ($configurationFingerprint -eq $hookFingerprint) {
        throw "O fingerprint não detectou alteração na configuração executável dos hooks."
    }

    Set-Content -LiteralPath (Join-Path $fingerprintTests "verificar.ps1") -Value "teste-2"
    $testFingerprint = Get-SonarCodeFingerprint $fingerprintRoot
    if ($testFingerprint -eq $configurationFingerprint) {
        throw "O fingerprint não detectou alteração em verificador de test/powershell."
    }

    Write-Host "GREEN: configuração e segurança dos hooks do agente aprovadas."
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
