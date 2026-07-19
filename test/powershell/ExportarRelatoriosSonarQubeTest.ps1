$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot "../.."))
$scriptPath = Join-Path $repositoryRoot "exportar-relatorios-sonarqube.ps1"
$parseErrors = $null
[System.Management.Automation.Language.Parser]::ParseFile(
    $scriptPath,
    [ref]$null,
    [ref]$parseErrors
) | Out-Null
if ($parseErrors.Count -ne 0) {
    throw "Falha de sintaxe no exportador."
}

$testBase = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$testRoot = Join-Path $testBase ("sonar-export-test-" + [guid]::NewGuid().ToString("N"))
$outputPath = Join-Path $testRoot "output"
New-Item -ItemType Directory -Path $testRoot | Out-Null

$previousToken = [Environment]::GetEnvironmentVariable("SONAR_TOKEN", "Process")
[Environment]::SetEnvironmentVariable("SONAR_TOKEN", $null, "Process")
$missingTokenFailed = $false
try {
    & $scriptPath -OutputDirectory (Join-Path $testRoot "missing-token") -NoArchive
}
catch {
    $missingTokenFailed = $_.Exception.Message -match "Token do SonarQube não informado"
}
if (-not $missingTokenFailed) {
    throw "O exportador não falhou de forma segura sem SONAR_TOKEN."
}

[Environment]::SetEnvironmentVariable("SONAR_TOKEN", "credencial-sintetica", "Process")
$credentialInUrlFailed = $false
try {
    & $scriptPath `
        -SonarUrl "http://usuario:senha@localhost:9000" `
        -OutputDirectory (Join-Path $testRoot "credential-in-url") `
        -NoArchive
}
catch {
    $credentialInUrlFailed = $_.Exception.Message -match "não pode conter usuário ou senha"
}
if (-not $credentialInUrlFailed) {
    throw "O exportador aceitou credencial embutida em SonarUrl."
}

$global:capturedSonarRequests = [System.Collections.Generic.List[object]]::new()

function global:git {
    param([Parameter(ValueFromRemainingArguments = $true)][object[]]$Arguments)

    $global:LASTEXITCODE = 0
    return "abc123def456"
}

function global:Invoke-RestMethod {
    param($Method, $Uri, $Headers, $TimeoutSec)

    if ($Headers.Authorization -ne "Bearer credencial-sintetica") {
        throw "Header de autenticação incorreto."
    }
    if ([string]$Uri -match "credencial-sintetica") {
        throw "Credencial encontrada na URL."
    }

    $global:capturedSonarRequests.Add([pscustomobject]@{ Uri = [string]$Uri })

    if ($Uri -match "/api/server/version") {
        return "2026.1"
    }
    if ($Uri -match "/api/webservices/list") {
        return [pscustomobject]@{ webServices = @() }
    }
    if ($Uri -match "/api/project_analyses/search") {
        return [pscustomobject]@{
            paging = [pscustomobject]@{ total = 1 }
            analyses = @([pscustomobject]@{
                key = "analysis-1"
                revision = "abc123def456"
            })
        }
    }
    if ($Uri -match "/api/issues/search") {
        return ConvertFrom-Json @'
{"paging":{"pageIndex":1,"pageSize":500,"total":2},"issues":[{"key":"I1","rule":"java:S1","severity":"MAJOR","type":"CODE_SMELL","status":"OPEN","component":"simtr-hub-local:src/A.java","project":"simtr-hub-local","line":10,"textRange":{"startLine":10,"endLine":10,"startOffset":1,"endOffset":4},"message":"Issue A","effort":"5min","creationDate":"2026-07-18T00:00:00+0000","updateDate":"2026-07-18T00:00:00+0000"},{"key":"I2","rule":"java:S2","severity":"CRITICAL","type":"BUG","status":"OPEN","component":"simtr-hub-local:src/B.java","project":"simtr-hub-local","line":20,"textRange":{"startLine":20,"endLine":21,"startOffset":0,"endOffset":2},"message":"Issue B","effort":"10min","creationDate":"2026-07-18T00:00:00+0000","updateDate":"2026-07-18T00:00:00+0000"}]}
'@
    }
    if ($Uri -match "/api/measures/component_tree") {
        return ConvertFrom-Json @'
{"paging":{"pageIndex":1,"pageSize":500,"total":2},"components":[{"key":"simtr-hub-local:src/A.java","path":"src/A.java","language":"java","measures":[{"metric":"ncloc","value":"100"},{"metric":"duplicated_lines","value":"5"},{"metric":"duplicated_blocks","value":"1"},{"metric":"duplicated_lines_density","value":"5.0"}]},{"key":"simtr-hub-local:src/B.java","path":"src/B.java","language":"java","measures":[{"metric":"ncloc","value":"80"},{"metric":"duplicated_lines","value":"0"},{"metric":"duplicated_blocks","value":"0"},{"metric":"duplicated_lines_density","value":"0.0"}]}]}
'@
    }
    if ($Uri -match "/api/duplications/show") {
        return ConvertFrom-Json @'
{"duplications":[{"blocks":[{"from":10,"size":5,"_ref":"1"},{"from":30,"size":5,"_ref":"2"}]}],"files":{"1":{"key":"simtr-hub-local:src/A.java","name":"src/A.java","projectName":"simtr-hub-local"},"2":{"key":"simtr-hub-local:src/B.java","name":"src/B.java","projectName":"simtr-hub-local"}}}
'@
    }

    throw "Endpoint simulado ausente: $Uri"
}

try {
    & $scriptPath -OutputDirectory $outputPath -NoArchive

    $expectedFiles = @(
        "manifest.json",
        "latest-analysis.json",
        "web-api-catalog.json",
        "issues.json",
        "issues.csv",
        "issues-page-0001.json",
        "duplication-files.json",
        "duplication-files.csv",
        "duplication-measures-page-0001.json",
        "duplication-blocks.json",
        "duplication-lines.csv",
        "duplications-file-0001.json"
    )
    foreach ($file in $expectedFiles) {
        if (-not (Test-Path -LiteralPath (Join-Path $outputPath $file))) {
            throw "Arquivo ausente: $file"
        }
    }

    $manifest = Get-Content -Raw (Join-Path $outputPath "manifest.json") |
        ConvertFrom-Json
    if ($manifest.issueExportedCount -ne 2 -or
        $manifest.duplicationFileCount -ne 2 -or
        $manifest.duplicationBlockRowCount -ne 2) {
        throw "Contagens incorretas no manifesto."
    }
    if ($manifest.authenticationIncluded -ne $false) {
        throw "Manifesto indicou autenticação incluída."
    }

    $duplicationRows = @(Import-Csv (Join-Path $outputPath "duplication-lines.csv"))
    if ($duplicationRows.Count -ne 2 -or
        $duplicationRows[0].fromLine -ne "10" -or
        $duplicationRows[0].toLine -ne "14") {
        throw "Intervalos de duplicação incorretos."
    }
    if ($global:capturedSonarRequests.Count -ne 6) {
        throw "Quantidade inesperada de requests: $($global:capturedSonarRequests.Count)"
    }

    $secretInFiles = Get-ChildItem -LiteralPath $outputPath -File |
        Select-String -SimpleMatch "credencial-sintetica"
    if ($secretInFiles) {
        throw "Credencial encontrada no pacote exportado."
    }

    Write-Host "GREEN: exportação simulada e contrato de segurança aprovados."
}
finally {
    Remove-Item Function:\Invoke-RestMethod -ErrorAction SilentlyContinue
    Remove-Item Function:\git -ErrorAction SilentlyContinue
    Remove-Variable capturedSonarRequests -Scope Global -ErrorAction SilentlyContinue
    [Environment]::SetEnvironmentVariable("SONAR_TOKEN", $previousToken, "Process")

    $resolvedTestRoot = [IO.Path]::GetFullPath($testRoot)
    if ($resolvedTestRoot.StartsWith($testBase, [StringComparison]::OrdinalIgnoreCase) -and
        (Test-Path -LiteralPath $resolvedTestRoot)) {
        Remove-Item -LiteralPath $resolvedTestRoot -Recurse -Force
    }
}
