[CmdletBinding()]
param(
    [string]$ClusterName = "simtr-hub-poc",
    [string]$Namespace = "simtr-hub-poc",
    [string]$EnvironmentFile = ".env.poc-kubernetes",
    [string]$Image = "simtr-hub-poc:local",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repositoryRoot = $PSScriptRoot
$manifestRoot = Join-Path $repositoryRoot "k8s/poc"
$kindConfigPath = Join-Path $manifestRoot "kind-cluster.yaml"
$dockerfilePath = Join-Path $repositoryRoot "src/main/docker/Dockerfile.jvm"
$environmentPath = [IO.Path]::GetFullPath((Join-Path $repositoryRoot $EnvironmentFile))
$context = "kind-$ClusterName"
$kindOnPath = Get-Command kind -ErrorAction SilentlyContinue
if ($null -eq $kindOnPath) {
    $kindCommand = Join-Path $repositoryRoot ".tools/kind.exe"
}
else {
    $kindCommand = $kindOnPath.Source
}

function Invoke-Checked {
    param(
        [Parameter(Mandatory)][string]$FilePath,
        [Parameter(Mandatory)][string[]]$Arguments
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Comando falhou ($LASTEXITCODE): $FilePath $($Arguments -join ' ')"
    }
}

function Get-ClusterJson {
    param(
        [Parameter(Mandatory)][string]$Resource,
        [string[]]$AdditionalArguments = @()
    )

    $arguments = @("--context", $context, "--namespace", $Namespace, "get", $Resource) +
        $AdditionalArguments + @("-o", "json")
    $output = & kubectl @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Nao foi possivel consultar $Resource no cluster $ClusterName."
    }
    return (($output -join [Environment]::NewLine) | ConvertFrom-Json)
}

function Get-MemberLeaseSnapshot {
    $leases = Get-ClusterJson -Resource "leases" -AdditionalArguments @(
        "--selector", "io.quarkiverse.flow.durable.k8s/pool=simtr-hub-conformidade"
    )
    $members = @($leases.items | Where-Object { $_.metadata.name -like "*-member-*" })
    if ($members.Count -ne 2) {
        throw "Esperadas 2 Leases de membro; encontradas $($members.Count)."
    }
    if (@($members | Where-Object { [string]::IsNullOrWhiteSpace($_.spec.holderIdentity) }).Count -gt 0) {
        throw "Toda Lease de membro deve possuir holderIdentity."
    }
    return $members
}

function Assert-ApplicationReady {
    $pods = Get-ClusterJson -Resource "pods" -AdditionalArguments @(
        "--selector", "app.kubernetes.io/name=simtr-hub"
    )
    $activePods = @($pods.items | Where-Object {
        $null -eq $_.metadata.PSObject.Properties["deletionTimestamp"]
    })
    if ($activePods.Count -ne 2) {
        throw "Esperados 2 pods ativos da aplicacao; encontrados $($activePods.Count)."
    }

    foreach ($pod in $activePods) {
        $readyCondition = $pod.status.conditions |
            Where-Object { $_.type -eq "Ready" } |
            Select-Object -First 1
        if ($null -eq $readyCondition -or $readyCondition.status -ne "True") {
            throw "Pod $($pod.metadata.name) nao esta Ready."
        }

        $podName = $pod.metadata.name
        $healthOutput = & kubectl --context $context --namespace $Namespace exec $podName -- `
            curl --fail --silent --show-error http://127.0.0.1:8080/q/health/ready
        if ($LASTEXITCODE -ne 0) {
            throw "Readiness HTTP falhou no pod $($pod.metadata.name)."
        }
        $health = ($healthOutput -join [Environment]::NewLine) | ConvertFrom-Json
        $leaseCheck = $health.checks |
            Where-Object { $_.name -eq "Lease Acquisition" } |
            Select-Object -First 1
        if ($null -eq $leaseCheck -or $leaseCheck.status -ne "UP" -or
            $leaseCheck.data.leaseAcquired -ne $true) {
            throw "Pod $($pod.metadata.name) ficou Ready sem comprovar a Lease."
        }
    }
    return @($activePods | ForEach-Object { $_.metadata.name } | Sort-Object)
}

function Assert-PodWithoutLeaseIsNotReady {
    $deploymentName = "simtr-hub-sem-lease"
    $defaultCanCreateLease = & kubectl --context $context --namespace $Namespace `
        auth can-i create leases.coordination.k8s.io `
        --as "system:serviceaccount:${Namespace}:default"
    if (($defaultCanCreateLease -join "").Trim() -ne "no") {
        throw "ServiceAccount default deve permanecer sem permissao para criar Leases."
    }

    $deployment = @{
        apiVersion = "apps/v1"
        kind = "Deployment"
        metadata = @{ name = $deploymentName; namespace = $Namespace }
        spec = @{
            replicas = 1
            selector = @{ matchLabels = @{ app = $deploymentName } }
            template = @{
                metadata = @{ labels = @{ app = $deploymentName } }
                spec = @{
                    serviceAccountName = "default"
                    containers = @(@{
                        name = "simtr-hub"
                        image = $Image
                        imagePullPolicy = "Never"
                        envFrom = @(@{ secretRef = @{ name = "simtr-hub-poc-secrets" } })
                        env = @(
                            @{ name = "QUARKUS_PROFILE"; value = "poc,kubernetes" },
                            @{ name = "CONFORMIDADE_PERSISTENCIA_BACKEND"; value = "couchdb" },
                            @{ name = "COUCHDB_HOST"; value = "couchdb" },
                            @{ name = "COUCHDB_PORT"; value = "5984" },
                            @{ name = "COUCHDB_DATABASE"; value = "conformidade" },
                            @{ name = "QUARKUS_REDIS_HOSTS"; value = "redis://valkey:6379" },
                            @{ name = "OLLAMA_BASE_URL"; value = "http://host.docker.internal:11434/" },
                            @{ name = "QUARKUS_OIDC_TENANT_ENABLED"; value = "false" },
                            @{ name = "QUARKUS_OIDC_CLIENT_CLIENT_ENABLED"; value = "false" },
                            @{ name = "QUARKUS_LOG_FILE_ENABLED"; value = "false" },
                            @{ name = "POD_NAME"; valueFrom = @{ fieldRef = @{ fieldPath = "metadata.name" } } },
                            @{ name = "POD_NAMESPACE"; valueFrom = @{ fieldRef = @{ fieldPath = "metadata.namespace" } } }
                        )
                        readinessProbe = @{
                            httpGet = @{ path = "/q/health/ready"; port = 8080 }
                            periodSeconds = 2
                            failureThreshold = 1
                        }
                    })
                }
            }
        }
    }

    try {
        $deploymentJson = $deployment | ConvertTo-Json -Depth 20 -Compress
        $deploymentJson | & kubectl --context $context apply -f -
        if ($LASTEXITCODE -ne 0) {
            throw "Nao foi possivel criar o pod de controle sem RBAC de Lease."
        }

        $podName = $null
        for ($attempt = 0; $attempt -lt 20 -and $null -eq $podName; $attempt++) {
            $podList = Get-ClusterJson -Resource "pods" -AdditionalArguments @(
                "--selector", "app=$deploymentName"
            )
            $podName = @($podList.items | Select-Object -First 1).metadata.name
            if ($null -eq $podName) {
                Start-Sleep -Seconds 1
            }
        }
        if ($null -eq $podName) {
            throw "Pod de controle sem Lease nao foi criado."
        }

        $leaseCheck = $null
        for ($attempt = 0; $attempt -lt 30 -and $null -eq $leaseCheck; $attempt++) {
            $healthOutput = & kubectl --context $context --namespace $Namespace exec $podName -- `
                curl --silent --show-error http://127.0.0.1:8080/q/health/ready
            if ($LASTEXITCODE -eq 0) {
                try {
                    $health = ($healthOutput -join [Environment]::NewLine) | ConvertFrom-Json
                    $leaseCheck = $health.checks |
                        Where-Object { $_.name -eq "Lease Acquisition" } |
                        Select-Object -First 1
                }
                catch {
                    $leaseCheck = $null
                }
            }
            if ($null -eq $leaseCheck) {
                Start-Sleep -Seconds 1
            }
        }
        if ($null -eq $leaseCheck -or $leaseCheck.status -ne "DOWN" -or
            $leaseCheck.data.leaseAcquired -ne $false) {
            throw "Health do pod sem RBAC deve comprovar Lease Acquisition DOWN e nao adquirida."
        }

        $pod = Get-ClusterJson -Resource "pod/$podName"
        $readyCondition = $pod.status.conditions |
            Where-Object { $_.type -eq "Ready" } |
            Select-Object -First 1
        if ($null -ne $readyCondition -and $readyCondition.status -eq "True") {
            throw "Pod sem permissao e sem Lease nao pode ficar Ready."
        }
    }
    finally {
        & kubectl --context $context --namespace $Namespace delete deployment $deploymentName `
            --ignore-not-found=true --wait=true | Out-Null
    }
}

foreach ($tool in @("docker", "kubectl", "mvn")) {
    if ($null -eq (Get-Command $tool -ErrorAction SilentlyContinue)) {
        throw "Ferramenta obrigatoria nao encontrada no PATH: $tool"
    }
}
if (-not (Test-Path -LiteralPath $kindCommand -PathType Leaf)) {
    throw "kind nao encontrado no PATH nem em .tools/kind.exe. Instale a versao indicada no guia."
}
if (-not (Test-Path -LiteralPath $environmentPath -PathType Leaf)) {
    throw "Arquivo de ambiente ausente: $environmentPath. Copie poc-kubernetes.env.example e preencha-o."
}

$environmentContent = Get-Content -LiteralPath $environmentPath
foreach ($key in @("COUCHDB_USERNAME", "COUCHDB_PASSWORD", "SIMTR_API_KEY", "OLLAMA_MODEL")) {
    $line = $environmentContent | Where-Object { $_ -match "^$([regex]::Escape($key))=(.+)$" } |
        Select-Object -First 1
    if ($null -eq $line) {
        throw "Valor obrigatorio ausente no arquivo de ambiente: $key"
    }
}

if (-not $SkipBuild) {
    Invoke-Checked -FilePath "mvn" -Arguments @(
        "-q",
        "-DskipTests",
        "-Dquarkus.flow.durable.kube.pool.name=simtr-hub-conformidade",
        "package"
    )
    Invoke-Checked -FilePath "docker" -Arguments @(
        "build", "--tag", $Image, "--file", $dockerfilePath, $repositoryRoot
    )
}

$existingClusters = @(& $kindCommand get clusters)
if ($LASTEXITCODE -ne 0) {
    throw "Nao foi possivel consultar os clusters kind."
}
if ($existingClusters -notcontains $ClusterName) {
    Invoke-Checked -FilePath $kindCommand -Arguments @(
        "create", "cluster", "--name", $ClusterName, "--config", $kindConfigPath
    )
}

# Mantidos de forma literal para tornar o roteiro operacional facilmente auditavel:
# kind create cluster; kind load docker-image; kubectl apply -k; kubectl get leases;
# kubectl rollout restart; kubectl rollout status; /q/health/ready.
Invoke-Checked -FilePath $kindCommand -Arguments @(
    "load", "docker-image", $Image, "--name", $ClusterName
)
Invoke-Checked -FilePath "kubectl" -Arguments @(
    "--context", $context, "apply", "-f", (Join-Path $manifestRoot "namespace.yaml")
)

$secretYaml = & kubectl --context $context --namespace $Namespace create secret generic `
    simtr-hub-poc-secrets --from-env-file=$environmentPath --dry-run=client -o yaml
if ($LASTEXITCODE -ne 0) {
    throw "Nao foi possivel montar o Secret a partir do arquivo de ambiente."
}
$secretYaml | & kubectl --context $context apply -f - | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Nao foi possivel aplicar o Secret no cluster."
}

Invoke-Checked -FilePath "kubectl" -Arguments @(
    "--context", $context, "apply", "-k", $manifestRoot
)
Invoke-Checked -FilePath "kubectl" -Arguments @(
    "--context", $context, "--namespace", $Namespace, "rollout", "status",
    "statefulset/couchdb", "--timeout=180s"
)
Invoke-Checked -FilePath "kubectl" -Arguments @(
    "--context", $context, "--namespace", $Namespace, "rollout", "status",
    "deployment/valkey", "--timeout=180s"
)
Invoke-Checked -FilePath "kubectl" -Arguments @(
    "--context", $context, "--namespace", $Namespace, "rollout", "restart", "deployment/simtr-hub"
)
Invoke-Checked -FilePath "kubectl" -Arguments @(
    "--context", $context, "--namespace", $Namespace, "rollout", "status",
    "deployment/simtr-hub", "--timeout=300s"
)

$memberLeasesBefore = Get-MemberLeaseSnapshot
$leaseNamesBefore = @($memberLeasesBefore.metadata.name | Sort-Object)
$podNamesBefore = Assert-ApplicationReady
Assert-PodWithoutLeaseIsNotReady

Invoke-Checked -FilePath "kubectl" -Arguments @(
    "--context", $context, "--namespace", $Namespace, "rollout", "restart", "deployment/simtr-hub"
)
Invoke-Checked -FilePath "kubectl" -Arguments @(
    "--context", $context, "--namespace", $Namespace, "rollout", "status",
    "deployment/simtr-hub", "--timeout=300s"
)

$memberLeasesAfter = Get-MemberLeaseSnapshot
$leaseNamesAfter = @($memberLeasesAfter.metadata.name | Sort-Object)
$podNamesAfter = Assert-ApplicationReady
if (@(Compare-Object -ReferenceObject $leaseNamesBefore -DifferenceObject $leaseNamesAfter).Count -ne 0) {
    throw "Rolling restart alterou as identidades estaveis das Leases."
}
if (@(Compare-Object -ReferenceObject $podNamesBefore -DifferenceObject $podNamesAfter -IncludeEqual |
        Where-Object { $_.SideIndicator -eq "==" }).Count -ne 0) {
    throw "Rolling restart deveria substituir os dois pods efemeros."
}
foreach ($holder in @($memberLeasesAfter.spec.holderIdentity)) {
    if ($podNamesAfter -notcontains $holder) {
        throw "Lease foi preservada, mas seu holderIdentity nao corresponde a um pod atual: $holder"
    }
}

Write-Host "GREEN: duas replicas adquiriram Leases estaveis, readiness exigiu Lease e o rollout preservou identidades."
