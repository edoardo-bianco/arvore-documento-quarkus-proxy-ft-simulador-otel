$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot "../.."))
$composePath = Join-Path $repositoryRoot "compose-poc.yml"
$dockerfilePath = Join-Path $repositoryRoot "src/main/docker/Dockerfile.jvm"
$dockerignorePath = Join-Path $repositoryRoot ".dockerignore"
$environmentExamplePath = Join-Path $repositoryRoot "poc-containers.env.example"

function Assert-FileExists {
    param([Parameter(Mandatory)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Arquivo obrigatorio ausente: $Path"
    }
}

function Assert-Contains {
    param(
        [Parameter(Mandatory)][string]$Content,
        [Parameter(Mandatory)][string]$Expected,
        [Parameter(Mandatory)][string]$Message
    )

    if (-not $Content.Contains($Expected, [StringComparison]::Ordinal)) {
        throw $Message
    }
}

function Assert-NotContains {
    param(
        [Parameter(Mandatory)][string]$Content,
        [Parameter(Mandatory)][string]$Unexpected,
        [Parameter(Mandatory)][string]$Message
    )

    if ($Content.Contains($Unexpected, [StringComparison]::Ordinal)) {
        throw $Message
    }
}

Assert-FileExists -Path $composePath
Assert-FileExists -Path $dockerfilePath
Assert-FileExists -Path $dockerignorePath
Assert-FileExists -Path $environmentExamplePath

$compose = Get-Content -LiteralPath $composePath -Raw
$dockerfile = Get-Content -LiteralPath $dockerfilePath -Raw
$dockerignore = Get-Content -LiteralPath $dockerignorePath -Raw
$environmentExample = Get-Content -LiteralPath $environmentExamplePath -Raw

foreach ($service in @("simtr-hub", "couchdb", "valkey", "ollama-check")) {
    Assert-Contains -Content $compose -Expected "  ${service}:" `
        -Message "Compose deve declarar o servico $service."
}

Assert-Contains -Content $dockerfile `
    -Expected "registry.access.redhat.com/ubi9/openjdk-25-runtime:1.24" `
    -Message "A imagem da aplicacao deve executar bytecode Java 25."
Assert-Contains -Content $dockerfile -Expected "target/quarkus-app/lib/" `
    -Message "Dockerfile deve copiar a camada de dependencias do fast-jar."
Assert-Contains -Content $dockerfile -Expected "target/quarkus-app/quarkus/" `
    -Message "Dockerfile deve copiar a camada Quarkus do fast-jar."
Assert-Contains -Content $dockerfile -Expected 'USER 185' `
    -Message "A aplicacao deve executar como usuario nao root."
Assert-Contains -Content $dockerfile -Expected 'JAVA_APP_JAR="/deployments/quarkus-run.jar"' `
    -Message "Dockerfile deve iniciar o fast-jar produzido pelo Quarkus."

foreach ($configuration in @(
    'QUARKUS_PROFILE: "poc"',
    'CONFORMIDADE_PERSISTENCIA_BACKEND: "couchdb"',
    'COUCHDB_HOST: "couchdb"',
    'QUARKUS_REDIS_HOSTS: "redis://valkey:6379"',
    'OLLAMA_BASE_URL: "http://host.docker.internal:11434/"',
    'QUARKUS_OIDC_TENANT_ENABLED: "false"',
    'QUARKUS_OIDC_CLIENT_CLIENT_ENABLED: "false"'
)) {
    Assert-Contains -Content $compose -Expected $configuration `
        -Message "Configuracao interna ausente no Compose: $configuration"
}

foreach ($dependencyCondition in @(
    'condition: service_healthy',
    'condition: service_completed_successfully'
)) {
    Assert-Contains -Content $compose -Expected $dependencyCondition `
        -Message "Compose deve aguardar saude e inicializacao: $dependencyCondition"
}

foreach ($volume in @("couchdb-conformidade-data")) {
    Assert-Contains -Content $compose -Expected $volume `
        -Message "Volume obrigatorio ausente: $volume"
}

foreach ($externalValue in @(
    '${COUCHDB_USERNAME:?',
    '${COUCHDB_PASSWORD:?',
    '${SIMTR_API_KEY:?'
)) {
    Assert-Contains -Content $compose -Expected $externalValue `
        -Message "Credencial deve ser exigida externamente: $externalValue"
}

foreach ($healthTarget in @(
    'http://127.0.0.1:8080/q/health/ready',
    'http://127.0.0.1:5984/_up',
    'valkey-cli'
)) {
    Assert-Contains -Content $compose -Expected $healthTarget `
        -Message "Health check obrigatorio ausente: $healthTarget"
}

foreach ($loopbackBinding in @(
    '127.0.0.1:${SIMTR_HUB_HTTP_PORT:-8080}:8080',
    '127.0.0.1:${COUCHDB_HTTP_PORT:-15984}:5984',
    '127.0.0.1:${VALKEY_PORT:-16379}:6379'
)) {
    Assert-Contains -Content $compose -Expected $loopbackBinding `
        -Message "Porta local da PoC nao pode ser publicada fora do loopback: $loopbackBinding"
}

Assert-Contains -Content $compose -Expected 'host.docker.internal:host-gateway' `
    -Message "Containers devem resolver explicitamente o Ollama instalado no host."
Assert-Contains -Content $compose -Expected 'http://host.docker.internal:11434/api/tags' `
    -Message "Preflight deve consultar os modelos disponibilizados pelo Ollama local."
Assert-Contains -Content $compose -Expected 'condition: service_completed_successfully' `
    -Message "Aplicacao deve aguardar o preflight do modelo local."
Assert-NotContains -Content $compose -Unexpected 'docker.io/ollama/ollama' `
    -Message "Task 8.1 nao deve baixar uma segunda instalacao do Ollama."
Assert-NotContains -Content $compose -Unexpected 'ollama-models' `
    -Message "Task 8.1 deve reutilizar os modelos ja instalados no host."
Assert-Contains -Content $dockerignore -Expected "target/quarkus-app" `
    -Message ".dockerignore deve permitir somente o artefato Quarkus necessario."

foreach ($key in @("COUCHDB_USERNAME=", "COUCHDB_PASSWORD=", "SIMTR_API_KEY=")) {
    Assert-Contains -Content $environmentExample -Expected $key `
        -Message "Exemplo de ambiente deve documentar $key sem fixar segredo."
}

$composeOutput = & docker compose -f $composePath config --no-interpolate 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "docker compose config falhou:`n$($composeOutput -join [Environment]::NewLine)"
}

Write-Host "GREEN: empacotamento local da PoC possui contrato coerente e Compose valido."
