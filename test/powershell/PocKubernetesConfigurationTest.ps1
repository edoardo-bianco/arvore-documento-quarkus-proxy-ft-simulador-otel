$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot "../.."))
$manifestRoot = Join-Path $repositoryRoot "k8s/poc"
$validationScriptPath = Join-Path $repositoryRoot "validar-poc-kubernetes.ps1"
$environmentExamplePath = Join-Path $repositoryRoot "poc-kubernetes.env.example"
$applicationPropertiesPath = Join-Path $repositoryRoot "src/main/resources/application.properties"
$pomPath = Join-Path $repositoryRoot "pom.xml"
$guidePath = Join-Path $repositoryRoot "doc/poc/especificacao-poc-conformidade-quarkus-flow-ollama-hitl-sem-broker.md"

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

foreach ($relativePath in @(
    "kind-cluster.yaml",
    "namespace.yaml",
    "rbac.yaml",
    "couchdb.yaml",
    "valkey.yaml",
    "simtr-hub.yaml",
    "kustomization.yaml"
)) {
    Assert-FileExists -Path (Join-Path $manifestRoot $relativePath)
}
Assert-FileExists -Path $validationScriptPath
Assert-FileExists -Path $environmentExamplePath
Assert-FileExists -Path $guidePath

[xml]$pom = Get-Content -LiteralPath $pomPath -Raw
$namespace = [Xml.XmlNamespaceManager]::new($pom.NameTable)
$namespace.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
$durableDependency = $pom.SelectSingleNode(
    "/m:project/m:dependencies/m:dependency[m:groupId='io.quarkiverse.flow' and m:artifactId='quarkus-flow-durable-kubernetes']",
    $namespace
)
if ($null -eq $durableDependency) {
    throw "quarkus-flow-durable-kubernetes deve ser dependencia normal da aplicacao."
}

$applicationProperties = Get-Content -LiteralPath $applicationPropertiesPath -Raw
Assert-Contains -Content $applicationProperties `
    -Expected "quarkus.flow.durable.kube.lease.member.enabled=false" `
    -Message "Leases devem permanecer desligadas fora do perfil Kubernetes."
Assert-Contains -Content $applicationProperties `
    -Expected "%kubernetes.quarkus.flow.durable.kube.lease.member.enabled=true" `
    -Message "Perfil Kubernetes deve habilitar a Lease de membro."
Assert-Contains -Content $applicationProperties `
    -Expected "%kubernetes.quarkus.flow.durable.kube.lease.leader.enabled=true" `
    -Message "Perfil Kubernetes deve habilitar a Lease de lider."
Assert-Contains -Content $applicationProperties `
    -Expected "%kubernetes.quarkus.flow.durable.kube.pool.name=simtr-hub-conformidade" `
    -Message "Perfil Kubernetes deve definir o pool estavel de Leases."
Assert-Contains -Content $applicationProperties `
    -Expected "%kubernetes.quarkus.flow.durable.kube.health.readiness.require-lease=true" `
    -Message "Readiness Kubernetes deve exigir a aquisicao de Lease."

$rendered = & kubectl kustomize $manifestRoot 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "kubectl kustomize falhou:`n$($rendered -join [Environment]::NewLine)"
}
$manifest = $rendered -join [Environment]::NewLine

foreach ($required in @(
    "kind: Namespace",
    "name: simtr-hub-poc",
    "kind: ServiceAccount",
    "kind: Role",
    "kind: RoleBinding",
    "coordination.k8s.io",
    "- leases",
    "- deployments",
    "- replicasets",
    "replicas: 2",
    "serviceAccountName: simtr-hub-durable-flow",
    "fieldPath: metadata.name",
    "fieldPath: metadata.namespace",
    "path: /q/health/ready",
    "maxUnavailable: 1",
    "maxSurge: 1",
    "kind: StatefulSet",
    "claimName: couchdb-conformidade-data",
    "value: +Q 65536",
    "image: valkey/valkey:7.2-alpine",
    "redis://valkey:6379",
    "http://host.docker.internal:11434/",
    "name: simtr-hub-poc-secrets"
)) {
    Assert-Contains -Content $manifest -Expected $required `
        -Message "Manifest Kubernetes renderizado deve conter: $required"
}

$rbac = Get-Content -LiteralPath (Join-Path $manifestRoot "rbac.yaml") -Raw
foreach ($verb in @("get", "list", "watch", "create", "update", "patch", "delete")) {
    Assert-Contains -Content $rbac -Expected "- $verb" `
        -Message "RBAC de Leases deve permitir o verbo $verb."
}

$kindConfig = Get-Content -LiteralPath (Join-Path $manifestRoot "kind-cluster.yaml") -Raw
Assert-Contains -Content $kindConfig -Expected "kind.x-k8s.io/v1alpha4" `
    -Message "Configuracao local deve usar a API oficial do kind."

$validationScript = Get-Content -LiteralPath $validationScriptPath -Raw
foreach ($operation in @(
    "kind create cluster",
    "kind load docker-image",
    "kubectl apply -k",
    "kubectl get leases",
    "/q/health/ready",
    "kubectl rollout restart",
    "kubectl rollout status"
)) {
    Assert-Contains -Content $validationScript -Expected $operation `
        -Message "Validacao executavel deve comprovar: $operation"
}

Assert-Contains -Content $validationScript `
    -Expected '"-Dquarkus.flow.durable.kube.pool.name=simtr-hub-conformidade"' `
    -Message "O nome do pool de Leases deve ser fixado durante a augmentacao da imagem Kubernetes."
Assert-Contains -Content $validationScript `
    -Expected '$null -eq $_.metadata.PSObject.Properties["deletionTimestamp"]' `
    -Message "A prova deve ignorar somente pods antigos que ainda estejam em terminacao apos o rollout."
Assert-Contains -Content $validationScript `
    -Expected "auth can-i create leases.coordination.k8s.io" `
    -Message "O controle negativo deve consultar o recurso de Lease com grupo qualificado aceito pelo kubectl."
Assert-Contains -Content $validationScript `
    -Expected 'if (($defaultCanCreateLease -join "").Trim() -ne "no")' `
    -Message "A resposta negativa esperada de kubectl auth can-i nao deve falhar apenas pelo exit code 1."
Assert-Contains -Content $validationScript `
    -Expected '$leaseCheck.status -ne "DOWN"' `
    -Message "O controle negativo deve comprovar que o health check da Lease ficou DOWN."
Assert-Contains -Content $validationScript `
    -Expected '$leaseCheck.data.leaseAcquired -ne $false' `
    -Message "O controle negativo deve comprovar que nenhuma Lease foi adquirida."
if ($validationScript.Contains("--api-group", [StringComparison]::Ordinal)) {
    throw "kubectl auth can-i nao deve usar a flag --api-group, indisponivel na CLI validada."
}
if ($validationScript.Contains("QUARKUS_FLOW_DURABLE_KUBE_POOL_NAME", [StringComparison]::Ordinal)) {
    throw "O controle negativo nao deve tentar sobrescrever em runtime o nome de pool fixado no build."
}

$applicationRolloutRestarts = [regex]::Matches(
    $validationScript,
    '"rollout", "restart", "deployment/simtr-hub"'
).Count
if ($applicationRolloutRestarts -lt 2) {
    throw "O roteiro deve implantar a imagem reconstruida antes das provas e repetir o rollout para validar a estabilidade."
}

foreach ($key in @("COUCHDB_USERNAME=", "COUCHDB_PASSWORD=", "SIMTR_API_KEY=", "OLLAMA_MODEL=")) {
    $environmentExample = Get-Content -LiteralPath $environmentExamplePath -Raw
    Assert-Contains -Content $environmentExample -Expected $key `
        -Message "Exemplo de ambiente Kubernetes deve documentar $key sem fixar segredo."
}

$guide = Get-Content -LiteralPath $guidePath -Raw
foreach ($guidance in @(
    "quarkus.flow.durable.kube.lease.member.enabled=false",
    "%kubernetes.quarkus.flow.durable.kube.lease.member.enabled=true",
    "-Dquarkus.flow.durable.kube.pool.name=simtr-hub-conformidade",
    "QUARKUS_PROFILE=poc,kubernetes",
    ".tools/kind.exe",
    "./validar-poc-kubernetes.ps1",
    "perda, reinício,",
    "fora do escopo",
    "host.docker.internal:11434"
)) {
    Assert-Contains -Content $guide -Expected $guidance `
        -Message "Guia operacional Kubernetes deve registrar: $guidance"
}

Write-Host "GREEN: contrato Kubernetes da PoC cobre Leases, readiness, duas replicas e rollout."
