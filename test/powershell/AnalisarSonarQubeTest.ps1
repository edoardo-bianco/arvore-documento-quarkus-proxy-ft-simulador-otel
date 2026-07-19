$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot "../.."))
$scriptPath = Join-Path $repositoryRoot "analisar-sonarqube.ps1"
$parseErrors = $null
[System.Management.Automation.Language.Parser]::ParseFile(
    $scriptPath,
    [ref]$null,
    [ref]$parseErrors
) | Out-Null
if ($parseErrors.Count -ne 0) {
    throw "Falha de sintaxe no analisador SonarQube."
}

$previousToken = [Environment]::GetEnvironmentVariable("SONAR_TOKEN", "Process")
[Environment]::SetEnvironmentVariable("SONAR_TOKEN", "credencial-sintetica", "Process")
$global:capturedMavenArguments = @()

function global:Invoke-RestMethod {
    param($Method, $Uri, $TimeoutSec)

    if ($Uri -ne "http://localhost:9000/api/system/status") {
        throw "URL inesperada na validação do SonarQube: $Uri"
    }

    return [pscustomobject]@{ status = "UP" }
}

function global:mvn {
    param([Parameter(ValueFromRemainingArguments = $true)][object[]]$Arguments)

    $global:capturedMavenArguments = @($Arguments)
    $global:LASTEXITCODE = 0
    Write-Output "saida-maven-simulada"
}

try {
    $analysisOutput = @(& $scriptPath -UseGlobalMaven)

    if ($analysisOutput.Count -ne 0) {
        throw "A saída do Maven vazou para o retorno do analisador SonarQube."
    }

    if ($global:capturedMavenArguments -notcontains "-Dsonar.projectKey=simtr-hub-local") {
        throw "O default sonar.projectKey não é simtr-hub-local."
    }
    if ($global:capturedMavenArguments -notcontains "-Dsonar.projectName=simtr-hub-local") {
        throw "O default sonar.projectName não é simtr-hub-local."
    }
    if ($global:capturedMavenArguments -notcontains
        "-Dsonar.coverage.jacoco.xmlReportPaths=target/jacoco-report/jacoco.xml") {
        throw "O caminho do relatório JaCoCo não foi informado ao SonarScanner."
    }
    if ($global:capturedMavenArguments -notcontains
        "-Dsonar.scanner.metadataFilePath=target/sonar/report-task.txt") {
        throw "O caminho dos metadados da análise não foi informado ao SonarScanner."
    }
    if (($global:capturedMavenArguments -join " ") -match "credencial-sintetica") {
        throw "A credencial foi incluída nos argumentos Maven."
    }

    $credentialInUrlFailed = $false
    try {
        & $scriptPath -SonarUrl "http://usuario:senha@localhost:9000" -UseGlobalMaven
    }
    catch {
        $credentialInUrlFailed = $_.Exception.Message -match "não pode conter usuário ou senha"
    }
    if (-not $credentialInUrlFailed) {
        throw "O analisador aceitou credencial embutida em SonarUrl."
    }

    Write-Host "GREEN: defaults do projeto SonarQube e contrato de segurança aprovados."
}
finally {
    Remove-Item Function:\Invoke-RestMethod -ErrorAction SilentlyContinue
    Remove-Item Function:\mvn -ErrorAction SilentlyContinue
    Remove-Variable capturedMavenArguments -Scope Global -ErrorAction SilentlyContinue
    [Environment]::SetEnvironmentVariable("SONAR_TOKEN", $previousToken, "Process")
}
