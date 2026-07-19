[CmdletBinding()]
param(
    [string]$ProjectKey = "simtr-hub-local",
    [string]$ProjectName = "simtr-hub-local",
    [string]$SonarUrl = "http://localhost:9000",
    [string]$MavenProfile = "",
    [string]$MetadataFilePath = "target/sonar/report-task.txt",
    [switch]$UseGlobalMaven
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Write-Step {
    param([Parameter(Mandatory)][string]$Message)

    Write-Host ""
    Write-Host "============================================================"
    Write-Host $Message
    Write-Host "============================================================"
}

function Test-Command {
    param([Parameter(Mandatory)][string]$Command)

    return $null -ne (Get-Command $Command -ErrorAction SilentlyContinue)
}

function Invoke-CheckedCommand {
    param(
        [Parameter(Mandatory)][string]$Executable,
        [Parameter(Mandatory)][string[]]$Arguments
    )

    Write-Host ""
    Write-Host "Executando:"
    Write-Host "$Executable $($Arguments -join ' ')"
    Write-Host ""

    & $Executable @Arguments | Out-Host

    if ($LASTEXITCODE -ne 0) {
        throw "O comando Maven falhou com código de saída $LASTEXITCODE. Confirme o erro acima, a associação do token e o ProjectKey informado."
    }
}

Write-Step "Validando configuração"

if ([string]::IsNullOrWhiteSpace($ProjectKey) -or
    $ProjectKey -notmatch '^[A-Za-z0-9_.:-]+$' -or
    $ProjectKey -match '^\d+$') {
    throw "ProjectKey deve conter letras e apenas letras, números, '-', '_', '.' ou ':'."
}

if ([string]::IsNullOrWhiteSpace($ProjectName)) {
    throw "ProjectName é obrigatório."
}

$parsedSonarUrl = $null
if (-not [Uri]::TryCreate($SonarUrl, [UriKind]::Absolute, [ref]$parsedSonarUrl) -or
    $parsedSonarUrl.Scheme -notin @("http", "https")) {
    throw "SonarUrl deve ser uma URL absoluta HTTP ou HTTPS."
}
if (-not [string]::IsNullOrWhiteSpace($parsedSonarUrl.UserInfo)) {
    throw "SonarUrl não pode conter usuário ou senha."
}

if (-not [string]::IsNullOrWhiteSpace($MavenProfile) -and
    $MavenProfile -notmatch '^[A-Za-z0-9_.-]+(?:,[A-Za-z0-9_.-]+)*$') {
    throw "MavenProfile contém caracteres inválidos."
}
if ([string]::IsNullOrWhiteSpace($MetadataFilePath) -or
    $MetadataFilePath.IndexOfAny([IO.Path]::GetInvalidPathChars()) -ge 0) {
    throw "MetadataFilePath inválido."
}

$sonarToken = [Environment]::GetEnvironmentVariable("SONAR_TOKEN", "Process")
if ([string]::IsNullOrWhiteSpace($sonarToken)) {
    throw @"
Token do SonarQube não informado.

Defina-o somente na sessão atual do PowerShell:

    `$env:SONAR_TOKEN = "seu-token"

O script não recebe token por parâmetro e não o imprime.
"@
}

$projectPom = Join-Path $PSScriptRoot "pom.xml"
if (-not (Test-Path -LiteralPath $projectPom -PathType Leaf)) {
    throw "pom.xml não encontrado junto ao script em '$PSScriptRoot'."
}

$normalizedSonarUrl = $parsedSonarUrl.AbsoluteUri.TrimEnd('/')

Write-Host "Projeto:       $ProjectName"
Write-Host "Project Key:   $ProjectKey"
Write-Host "SonarQube URL: $normalizedSonarUrl"
Write-Host "Diretório:     $PSScriptRoot"

Write-Step "Verificando o SonarQube"

try {
    $status = Invoke-RestMethod `
        -Method Get `
        -Uri "$normalizedSonarUrl/api/system/status" `
        -TimeoutSec 15
}
catch {
    throw "Não foi possível acessar o SonarQube em '$normalizedSonarUrl': $($_.Exception.Message)"
}

if ($status.status -ne "UP") {
    throw "O SonarQube respondeu com status '$($status.status)'."
}

Write-Host "SonarQube disponível e com status UP."

Write-Step "Selecionando o Maven"

$mavenExecutable = $null
if (-not $UseGlobalMaven) {
    $windowsWrapper = Join-Path $PSScriptRoot "mvnw.cmd"
    $unixWrapper = Join-Path $PSScriptRoot "mvnw"

    if (Test-Path -LiteralPath $windowsWrapper -PathType Leaf) {
        $mavenExecutable = $windowsWrapper
        Write-Host "Usando Maven Wrapper: $mavenExecutable"
    }
    elseif (Test-Path -LiteralPath $unixWrapper -PathType Leaf) {
        $mavenExecutable = $unixWrapper
        Write-Host "Usando Maven Wrapper: $mavenExecutable"
    }
}

if ($null -eq $mavenExecutable) {
    if (-not (Test-Command "mvn")) {
        throw "Maven não encontrado no PATH e Maven Wrapper não disponível."
    }

    $mavenExecutable = "mvn"
    Write-Host "Usando Maven global."
}

Write-Step "Compilando e publicando a análise"

$mavenArguments = @(
    "clean",
    "verify",
    "org.sonarsource.scanner.maven:sonar-maven-plugin:sonar",
    "-Dsonar.projectKey=$ProjectKey",
    "-Dsonar.projectName=$ProjectName",
    "-Dsonar.host.url=$normalizedSonarUrl",
    "-Dsonar.coverage.jacoco.xmlReportPaths=target/jacoco-report/jacoco.xml",
    "-Dsonar.scanner.metadataFilePath=$MetadataFilePath"
)

if (-not [string]::IsNullOrWhiteSpace($MavenProfile)) {
    $mavenArguments += "-P$MavenProfile"
}

Push-Location -LiteralPath $PSScriptRoot
try {
    Invoke-CheckedCommand -Executable $mavenExecutable -Arguments $mavenArguments
}
finally {
    Pop-Location
}

Write-Step "Análise concluída"
Write-Host "A análise foi enviada ao SonarQube."
Write-Host "Dashboard: $normalizedSonarUrl/dashboard?id=$([Uri]::EscapeDataString($ProjectKey))"
