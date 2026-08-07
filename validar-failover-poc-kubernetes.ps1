[CmdletBinding()]
param(
    [string]$ClusterName = "simtr-hub-poc",
    [string]$Namespace = "simtr-hub-poc",
    [int]$TimeoutSeconds = 300
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$context = "kind-$ClusterName"
$basePath = "/simtr-hub/v1/conformidade/analises"
$applicationSelector = "app.kubernetes.io/name=simtr-hub"
$leaseSelector = "io.quarkiverse.flow.durable.k8s/pool=simtr-hub-conformidade"

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

function Get-ReadyApplicationPods {
    $pods = Get-ClusterJson -Resource "pods" -AdditionalArguments @(
        "--selector", $applicationSelector
    )
    return @($pods.items | Where-Object {
        $null -eq $_.metadata.PSObject.Properties["deletionTimestamp"] -and
        $null -ne ($_.status.conditions | Where-Object {
            $_.type -eq "Ready" -and $_.status -eq "True"
        } | Select-Object -First 1)
    } | ForEach-Object { $_.metadata.name } | Sort-Object)
}

function Get-MemberBindings {
    $readyPods = Get-ReadyApplicationPods
    if ($readyPods.Count -ne 2) {
        throw "A prova exige exatamente dois pods ativos e Ready; encontrados $($readyPods.Count)."
    }

    $leases = Get-ClusterJson -Resource "leases" -AdditionalArguments @(
        "--selector", $leaseSelector
    )
    $members = @($leases.items | Where-Object {
        $_.metadata.name -like "*-member-*"
    } | Sort-Object { $_.metadata.name })
    if ($members.Count -ne 2) {
        throw "A prova exige exatamente duas Leases de membro; encontradas $($members.Count)."
    }

    $bindings = foreach ($lease in $members) {
        $holderIdentity = [string]$lease.spec.holderIdentity
        if ([string]::IsNullOrWhiteSpace($holderIdentity) -or
            $readyPods -notcontains $holderIdentity) {
            throw "Lease $($lease.metadata.name) nao esta associada a um pod Ready."
        }
        [pscustomobject]@{
            memberLeaseName = [string]$lease.metadata.name
            holderIdentity = $holderIdentity
            leaseUid = [string]$lease.metadata.uid
            resourceVersion = [string]$lease.metadata.resourceVersion
        }
    }
    return @($bindings)
}

function Invoke-PodRequest {
    param(
        [Parameter(Mandatory)][string]$Pod,
        [Parameter(Mandatory)][ValidateSet("GET", "POST", "PUT")][string]$Method,
        [Parameter(Mandatory)][string]$Path,
        [string]$Body
    )

    $url = "http://127.0.0.1:8080$Path"
    $arguments = @(
        "--context", $context, "--namespace", $Namespace,
        "exec", $Pod, "--", "curl",
        "--silent", "--show-error", "--max-time", "90",
        "--request", $Method,
        "--header", "Accept: application/json",
        "--write-out", "`n%{http_code}"
    )
    if (-not [string]::IsNullOrWhiteSpace($Body)) {
        $arguments += @(
            "--header", "Content-Type: application/json",
            "--data-binary", $Body
        )
    }
    $arguments += $url

    # Chamada dirigida ao pod: kubectl exec ... localhost:8080/simtr-hub/v1/conformidade/analises
    $output = @(& kubectl @arguments)
    if ($LASTEXITCODE -ne 0 -or $output.Count -eq 0) {
        throw "Falha HTTP $Method dirigida ao pod $Pod."
    }
    $status = [int]$output[-1]
    $responseBody = if ($output.Count -gt 1) {
        ($output[0..($output.Count - 2)] -join [Environment]::NewLine)
    }
    else {
        ""
    }
    return [pscustomobject]@{ status = $status; body = $responseBody }
}

function Convert-ResponseJson {
    param(
        [Parameter(Mandatory)]$Response,
        [Parameter(Mandatory)][int]$ExpectedStatus,
        [Parameter(Mandatory)][string]$Operation
    )

    if ($Response.status -ne $ExpectedStatus) {
        throw "$Operation retornou HTTP $($Response.status), esperado $ExpectedStatus."
    }
    if ([string]::IsNullOrWhiteSpace($Response.body)) {
        throw "$Operation nao retornou JSON."
    }
    try {
        return ($Response.body | ConvertFrom-Json)
    }
    catch {
        throw "$Operation retornou JSON invalido."
    }
}

function Wait-AnalysisStatus {
    param(
        [Parameter(Mandatory)][string]$Pod,
        [Parameter(Mandatory)][string]$InstanceId,
        [Parameter(Mandatory)][string]$ExpectedStatus
    )

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        $response = Invoke-PodRequest -Pod $Pod -Method "GET" `
            -Path "$basePath/$InstanceId"
        if ($response.status -eq 200) {
            $view = Convert-ResponseJson -Response $response -ExpectedStatus 200 `
                -Operation "Consulta da analise $InstanceId no pod $Pod"
            if ($view.status -eq $ExpectedStatus) {
                return $view
            }
            if ($view.status -eq "FALHOU") {
                throw "A analise $InstanceId terminou em FALHOU."
            }
        }
        Start-Sleep -Seconds 2
    } while ([DateTime]::UtcNow -lt $deadline)

    throw "Timeout aguardando $ExpectedStatus para a analise $InstanceId no pod $Pod."
}

function New-AnalysisPayload {
    param([Parameter(Mandatory)][string]$Scenario)

    return (@{
        identificadorDocumento = "DOC-POC-FAILOVER-$Scenario-$([Guid]::NewGuid().ToString('N'))"
        texto = "Documento sintetico para a prova Kubernetes $Scenario."
        identificadorChecklist = 1000012583
        versaoChecklist = 1
    } | ConvertTo-Json -Compress)
}

function New-ReviewPayload {
    param([Parameter(Mandatory)]$WaitingView)

    $preliminary = @($WaitingView.resultadoPreliminar.apontamentos)
    if ($preliminary.Count -eq 0) {
        throw "Resultado preliminar sem apontamentos; nao e possivel montar revisao completa."
    }
    $reviewItems = @($preliminary | ForEach-Object {
        [ordered]@{
            identificadorApontamento = $_.identificadorApontamento
            nomeApontamento = $_.nomeApontamento
            parecer = $_.parecer
            justificativa = "Revisado na prova cross-pod Kubernetes."
            evidencia = "Evidencia sintetica da prova."
            confianca = $_.confianca
        }
    })
    return ([ordered]@{
        observacao = "Revisao sintetica da prova cross-pod."
        apontamentos = $reviewItems
    } | ConvertTo-Json -Depth 10 -Compress)
}

function Get-CheckpointKeys {
    param([Parameter(Mandatory)][string]$InstanceId)

    $output = @(& kubectl --context $context --namespace $Namespace `
        exec deployment/valkey -- valkey-cli --scan --pattern "*$InstanceId*")
    if ($LASTEXITCODE -ne 0) {
        throw "Nao foi possivel inspecionar o checkpoint Valkey da instancia $InstanceId."
    }
    return @($output | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}

function Replace-OwnerPod {
    param(
        [Parameter(Mandatory)][string]$OwnerPod,
        [Parameter(Mandatory)][string]$MemberLeaseName,
        [Parameter(Mandatory)][string]$LeaseUid,
        [Parameter(Mandatory)][string]$ResourceVersion
    )

    # Interrupcao dirigida: kubectl delete pod <owner> --wait=false
    & kubectl --context $context --namespace $Namespace delete pod $OwnerPod --wait=false | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Nao foi possivel interromper o owner $OwnerPod."
    }

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        try {
            $lease = Get-ClusterJson -Resource "lease/$MemberLeaseName"
            $replacementPod = [string]$lease.spec.holderIdentity
            $readyPods = Get-ReadyApplicationPods
            if ($lease.metadata.uid -ne $LeaseUid) {
                throw "A Lease $MemberLeaseName foi apagada e recriada durante o failover."
            }
            if ($replacementPod -ne $OwnerPod -and
                -not [string]::IsNullOrWhiteSpace($replacementPod) -and
                $readyPods.Count -eq 2 -and
                $readyPods -contains $replacementPod) {
                if ($lease.metadata.resourceVersion -eq $ResourceVersion) {
                    throw "A Lease $MemberLeaseName nao registrou a troca de holderIdentity."
                }
                return $replacementPod
            }
        }
        catch {
            if ($_.Exception.Message -like "*foi apagada e recriada*" -or
                $_.Exception.Message -like "*nao registrou a troca*") {
                throw
            }
        }
        Start-Sleep -Seconds 2
    } while ([DateTime]::UtcNow -lt $deadline)

    throw "Timeout aguardando substituicao de $OwnerPod na Lease $MemberLeaseName."
}

function Get-CompletionEmissionCount {
    param(
        [Parameter(Mandatory)][string]$Pod,
        [Parameter(Mandatory)][string]$InstanceId
    )

    $query = 'curl --fail --silent --show-error --user "$COUCHDB_USERNAME:$COUCHDB_PASSWORD" "http://couchdb:5984/conformidade/_all_docs?include_docs=true"'
    $output = @(& kubectl --context $context --namespace $Namespace exec $Pod -- sh -c $query)
    if ($LASTEXITCODE -ne 0) {
        throw "Nao foi possivel contar as emissoes documentais da instancia $InstanceId."
    }
    $allDocuments = (($output -join [Environment]::NewLine) | ConvertFrom-Json)
    return @($allDocuments.rows | Where-Object {
        $_.doc.tipo -eq "emissao-cloud-event" -and
        $_.doc.instanceId -eq $InstanceId -and
        $_.doc.eventoTipo -eq "br.gov.caixa.simtr.conformidade.analise.concluida.v1"
    }).Count
}

function Assert-QueryableFromEveryPod {
    param(
        [Parameter(Mandatory)][string]$InstanceId,
        [Parameter(Mandatory)][string]$CorrelationId
    )

    $pods = Get-ReadyApplicationPods
    if ($pods.Count -ne 2) {
        throw "A verificacao final exige duas replicas Ready."
    }
    foreach ($pod in $pods) {
        # Consulta dirigida: kubectl exec ... localhost:8080/simtr-hub/v1/conformidade/analises
        $view = Convert-ResponseJson `
            -Response (Invoke-PodRequest -Pod $pod -Method "GET" -Path "$basePath/$InstanceId") `
            -ExpectedStatus 200 `
            -Operation "Consulta final no pod $pod"
        if ($view.instanceId -ne $InstanceId -or
            $view.correlationId -ne $CorrelationId -or
            $view.status -ne "CONCLUIDA" -or
            $null -eq $view.resultadoFinal) {
            throw "Pod $pod nao retornou o resultado final correlacionado esperado."
        }
    }
}

function Invoke-FailoverScenario {
    param(
        [Parameter(Mandatory)][string]$Scenario,
        [Parameter(Mandatory)][bool]$InterruptAfterReview
    )

    $bindings = Get-MemberBindings
    $ownerBinding = $bindings[0]
    $ownerPod = $ownerBinding.holderIdentity
    $reviewerPod = $bindings[1].holderIdentity
    $memberLeaseName = $ownerBinding.memberLeaseName

    # POST dirigido: kubectl exec ... localhost:8080/simtr-hub/v1/conformidade/analises
    $started = Convert-ResponseJson `
        -Response (Invoke-PodRequest -Pod $ownerPod -Method "POST" -Path $basePath `
            -Body (New-AnalysisPayload -Scenario $Scenario)) `
        -ExpectedStatus 202 `
        -Operation "Inicio da analise no owner $ownerPod"
    $instanceId = [string]$started.instanceId
    $correlationId = [string]$started.correlationId
    if ([string]::IsNullOrWhiteSpace($instanceId) -or
        [string]::IsNullOrWhiteSpace($correlationId)) {
        throw "POST nao retornou instanceId e correlationId."
    }

    $waitingView = Wait-AnalysisStatus -Pod $reviewerPod -InstanceId $instanceId `
        -ExpectedStatus "AGUARDANDO_REVISAO"
    if ($waitingView.correlationId -ne $correlationId) {
        throw "Consulta cross-pod retornou correlacao diferente da iniciada."
    }
    $reviewPayload = New-ReviewPayload -WaitingView $waitingView
    $checkpointBefore = Get-CheckpointKeys -InstanceId $instanceId
    if ($checkpointBefore.Count -eq 0) {
        throw "Checkpoint da instancia em espera nao foi encontrado no Valkey."
    }

    if (-not $InterruptAfterReview) {
        $point = "owner-interrompido-antes-da-revisao"
        $replacementPod = Replace-OwnerPod -OwnerPod $ownerPod `
            -MemberLeaseName $memberLeaseName -LeaseUid $ownerBinding.leaseUid `
            -ResourceVersion $ownerBinding.resourceVersion
        $checkpointAfter = Get-CheckpointKeys -InstanceId $instanceId
        if ($checkpointAfter.Count -eq 0) {
            throw "Checkpoint nao sobreviveu a substituicao do owner antes da revisao."
        }
        $null = Wait-AnalysisStatus -Pod $replacementPod -InstanceId $instanceId `
            -ExpectedStatus "AGUARDANDO_REVISAO"
        # PUT dirigido: kubectl exec ... localhost:8080/simtr-hub/v1/conformidade/analises
        $reviewResponse = Invoke-PodRequest -Pod $reviewerPod -Method "PUT" `
            -Path "$basePath/$instanceId/revisao" -Body $reviewPayload
    }
    else {
        $point = "owner-interrompido-apos-a-revisao"
        # PUT dirigido: kubectl exec ... localhost:8080/simtr-hub/v1/conformidade/analises
        $reviewResponse = Invoke-PodRequest -Pod $reviewerPod -Method "PUT" `
            -Path "$basePath/$instanceId/revisao" -Body $reviewPayload
        if ($reviewResponse.status -ne 202) {
            throw "Revisao cross-pod retornou HTTP $($reviewResponse.status), esperado 202."
        }
        $replacementPod = Replace-OwnerPod -OwnerPod $ownerPod `
            -MemberLeaseName $memberLeaseName -LeaseUid $ownerBinding.leaseUid `
            -ResourceVersion $ownerBinding.resourceVersion
    }
    if ($reviewResponse.status -ne 202) {
        throw "Revisao cross-pod retornou HTTP $($reviewResponse.status), esperado 202."
    }

    $completedView = Wait-AnalysisStatus -Pod $replacementPod -InstanceId $instanceId `
        -ExpectedStatus "CONCLUIDA"
    if ($completedView.correlationId -ne $correlationId -or
        $null -eq $completedView.resultadoFinal) {
        throw "Failover concluiu uma correlacao ou um resultado diferente do esperado."
    }
    Assert-QueryableFromEveryPod -InstanceId $instanceId -CorrelationId $correlationId

    $revisaoRepetidaStatus = (Invoke-PodRequest -Pod $reviewerPod -Method "PUT" `
        -Path "$basePath/$instanceId/revisao" -Body $reviewPayload).status
    if ($revisaoRepetidaStatus -ne 409) {
        throw "Revisao repetida retornou HTTP $revisaoRepetidaStatus, esperado 409."
    }
    $completionEmissionCount = Get-CompletionEmissionCount -Pod $reviewerPod `
        -InstanceId $instanceId
    if ($completionEmissionCount -ne 1) {
        throw "Esperada uma emissao de conclusao; encontradas $completionEmissionCount."
    }

    return [pscustomobject]@{
        scenario = $Scenario
        point = $point
        ownerPod = $ownerPod
        reviewerPod = $reviewerPod
        memberLeaseName = $memberLeaseName
        replacementPod = $replacementPod
        instanceId = $instanceId
        correlationId = $correlationId
        resultadoFinal = $true
        revisaoRepetidaStatus = $revisaoRepetidaStatus
        completionEmissionCount = $completionEmissionCount
    }
}

if ($null -eq (Get-Command kubectl -ErrorAction SilentlyContinue)) {
    throw "kubectl nao encontrado no PATH."
}
if ($TimeoutSeconds -lt 60) {
    throw "TimeoutSeconds deve ser pelo menos 60."
}

$null = Get-MemberBindings
$beforeReview = Invoke-FailoverScenario `
    -Scenario "antes-revisao" -InterruptAfterReview $false
$afterReview = Invoke-FailoverScenario `
    -Scenario "apos-revisao" -InterruptAfterReview $true

@($beforeReview, $afterReview) |
    Select-Object scenario, point, ownerPod, reviewerPod, memberLeaseName,
        replacementPod, instanceId, correlationId, resultadoFinal,
        revisaoRepetidaStatus, completionEmissionCount |
    Format-List

Write-Host "GREEN: cross-pod e failover preservaram Lease, dados e checkpoint; cada instancia concluiu uma unica vez."
