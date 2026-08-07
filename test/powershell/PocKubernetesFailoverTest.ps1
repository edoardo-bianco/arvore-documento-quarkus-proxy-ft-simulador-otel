$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot "../.."))
$proofScriptPath = Join-Path $repositoryRoot "validar-failover-poc-kubernetes.ps1"

if (-not (Test-Path -LiteralPath $proofScriptPath -PathType Leaf)) {
    throw "Roteiro reproduzivel de cross-pod e failover ausente: $proofScriptPath"
}

$proofScript = Get-Content -LiteralPath $proofScriptPath -Raw

function Assert-Contains {
    param(
        [Parameter(Mandatory)][string]$Expected,
        [Parameter(Mandatory)][string]$Message
    )

    if (-not $proofScript.Contains($Expected, [StringComparison]::Ordinal)) {
        throw $Message
    }
}

foreach ($operation in @(
    "function Invoke-PodRequest",
    "function Get-MemberBindings",
    "function Replace-OwnerPod",
    "function Get-CompletionEmissionCount",
    "/simtr-hub/v1/conformidade/analises",
    "AGUARDANDO_REVISAO",
    "CONCLUIDA"
)) {
    Assert-Contains -Expected $operation `
        -Message "A prova de failover deve conter a operacao: $operation"
}

foreach ($invariant in @(
    "ownerPod",
    "reviewerPod",
    "memberLeaseName",
    "replacementPod",
    "instanceId",
    "correlationId",
    "resultadoFinal"
)) {
    Assert-Contains -Expected $invariant `
        -Message "A prova deve evidenciar o invariante: $invariant"
}

foreach ($directRequest in @(
    'Invoke-PodRequest -Pod $ownerPod -Method "POST"',
    'Invoke-PodRequest -Pod $reviewerPod -Method "PUT"',
    'Invoke-PodRequest -Pod $pod -Method "GET"'
)) {
    Assert-Contains -Expected $directRequest `
        -Message "POST, revisao e consulta devem ser dirigidos a pods identificados: $directRequest"
}

Assert-Contains -Expected 'holderIdentity' `
    -Message "O owner deve ser associado ao holderIdentity de sua Lease de membro."
Assert-Contains -Expected 'Get-ClusterJson -Resource "lease/$MemberLeaseName"' `
    -Message "A prova deve acompanhar diretamente a Lease estavel do owner."
Assert-Contains -Expected 'delete pod $OwnerPod --wait=false' `
    -Message "A prova deve interromper explicitamente o pod owner."
Assert-Contains -Expected 'resourceVersion' `
    -Message "A prova deve observar a troca do pod sem apagar ou recriar a Lease estavel."
Assert-Contains -Expected 'revisaoRepetidaStatus' `
    -Message "A prova deve verificar que repetir a revisao nao conclui a instancia novamente."
Assert-Contains -Expected 'owner-interrompido-antes-da-revisao' `
    -Message "A prova deve interromper o owner enquanto a instancia aguarda revisao."
Assert-Contains -Expected 'owner-interrompido-apos-a-revisao' `
    -Message "A prova deve interromper o owner imediatamente depois de a revisao ser aceita."

Write-Host "GREEN: roteiro Kubernetes cobre cross-pod, correlacao unica e failover em dois pontos."
