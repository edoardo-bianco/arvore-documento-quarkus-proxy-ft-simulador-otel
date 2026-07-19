[CmdletBinding()]
param(
    [string]$ProjectKey = "simtr-hub-local",
    [string]$SonarUrl = "http://localhost:9000",
    [string]$Branch = "",
    [string]$OutputDirectory = "",
    [switch]$IncludeResolved,
    [switch]$NoArchive
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

function Write-JsonFile {
    param(
        [Parameter(Mandatory)][AllowNull()][object]$Value,
        [Parameter(Mandatory)][string]$Path,
        [int]$Depth = 30
    )

    ConvertTo-Json -InputObject $Value -Depth $Depth |
        Set-Content -LiteralPath $Path -Encoding utf8
}

function Export-CsvFile {
    param(
        [Parameter(Mandatory)][AllowEmptyCollection()][object[]]$Rows,
        [Parameter(Mandatory)][string[]]$Columns,
        [Parameter(Mandatory)][string]$Path
    )

    if ($Rows.Count -gt 0) {
        $Rows |
            Select-Object -Property $Columns |
            Export-Csv -LiteralPath $Path -NoTypeInformation -Encoding utf8
        return
    }

    '"{0}"' -f ($Columns -join '","') |
        Set-Content -LiteralPath $Path -Encoding utf8
}

function ConvertTo-QueryString {
    param([Parameter(Mandatory)][hashtable]$Parameters)

    return ($Parameters.GetEnumerator() |
        Sort-Object Key |
        ForEach-Object {
            "$([Uri]::EscapeDataString([string]$_.Key))=$([Uri]::EscapeDataString([string]$_.Value))"
        }) -join "&"
}

function Invoke-SonarApi {
    param(
        [Parameter(Mandatory)][string]$Path,
        [hashtable]$Parameters = @{}
    )

    $uri = "$($script:NormalizedSonarUrl)/$($Path.TrimStart('/'))"
    $query = ConvertTo-QueryString -Parameters $Parameters
    if (-not [string]::IsNullOrWhiteSpace($query)) {
        $uri = "${uri}?$query"
    }

    return Invoke-RestMethod `
        -Method Get `
        -Uri $uri `
        -Headers $script:SonarHeaders `
        -TimeoutSec 60
}

function Get-SonarPagedCollection {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][hashtable]$Parameters,
        [Parameter(Mandatory)][string]$CollectionProperty,
        [Parameter(Mandatory)][string]$RawFilePrefix
    )

    $page = 1
    $total = 0
    $items = [System.Collections.Generic.List[object]]::new()

    do {
        $pageParameters = $Parameters.Clone()
        $pageParameters["p"] = $page
        $pageParameters["ps"] = 500

        $response = Invoke-SonarApi -Path $Path -Parameters $pageParameters
        Write-JsonFile `
            -Value $response `
            -Path (Join-Path $script:OutputPath "$RawFilePrefix-page-$('{0:D4}' -f $page).json")

        $pageItems = @($response.$CollectionProperty)
        foreach ($item in $pageItems) {
            $items.Add($item)
        }

        if ($null -ne $response.PSObject.Properties["paging"]) {
            $total = [int]$response.paging.total
        }
        elseif ($null -ne $response.PSObject.Properties["total"]) {
            $total = [int]$response.total
        }
        else {
            throw "A resposta de '$Path' não contém total de paginação."
        }

        $page++
    } while ($pageItems.Count -gt 0 -and $items.Count -lt $total)

    if ($items.Count -ne $total) {
        throw "Exportação incompleta em '$Path': $($items.Count) de $total registros."
    }

    return [pscustomobject]@{
        Items = $items.ToArray()
        Total = $total
        Pages = $page - 1
    }
}

function Get-MeasureValue {
    param(
        [Parameter(Mandatory)][object]$Component,
        [Parameter(Mandatory)][string]$Metric
    )

    $measure = $Component.measures |
        Where-Object { $_.metric -eq $Metric } |
        Select-Object -First 1

    if ($null -eq $measure) {
        return $null
    }

    return $measure.value
}

function Get-PropertyValue {
    param(
        [AllowNull()][object]$InputObject,
        [Parameter(Mandatory)][string]$Name
    )

    if ($null -eq $InputObject) {
        return $null
    }

    $property = $InputObject.PSObject.Properties[$Name]
    if ($null -eq $property) {
        return $null
    }

    return $property.Value
}

function Get-LocalGitRevision {
    try {
        $revision = & git -C $PSScriptRoot rev-parse HEAD 2>$null
        if ($LASTEXITCODE -eq 0) {
            return [string]$revision
        }
    }
    catch {
        return $null
    }

    return $null
}

Write-Step "Validando configuração"

if ([string]::IsNullOrWhiteSpace($ProjectKey) -or
    $ProjectKey -notmatch '^[A-Za-z0-9_.:-]+$' -or
    $ProjectKey -match '^\d+$') {
    throw "ProjectKey deve conter letras e apenas letras, números, '-', '_', '.' ou ':'."
}

$parsedSonarUrl = $null
if (-not [Uri]::TryCreate($SonarUrl, [UriKind]::Absolute, [ref]$parsedSonarUrl) -or
    $parsedSonarUrl.Scheme -notin @("http", "https")) {
    throw "SonarUrl deve ser uma URL absoluta HTTP ou HTTPS."
}

if (-not [string]::IsNullOrWhiteSpace($parsedSonarUrl.UserInfo)) {
    throw "SonarUrl não pode conter usuário ou senha."
}

if ($parsedSonarUrl.Scheme -eq "http" -and
    $parsedSonarUrl.Host -notin @("localhost", "127.0.0.1", "::1")) {
    Write-Warning "O servidor remoto usa HTTP. Prefira HTTPS para proteger o token em trânsito."
}

$sonarToken = [Environment]::GetEnvironmentVariable("SONAR_TOKEN", "Process")
if ([string]::IsNullOrWhiteSpace($sonarToken)) {
    throw @"
Token do SonarQube não informado.

Use um User token com Browse e defina-o somente na sessão atual:

PowerShell 7.1 ou superior:

    `$env:SONAR_TOKEN = Read-Host -Prompt "Cole o User token" -MaskInput

Windows PowerShell 5.1:

    `$tokenSeguro = Read-Host -Prompt "Cole o User token" -AsSecureString
    `$env:SONAR_TOKEN = [System.Net.NetworkCredential]::new("", `$tokenSeguro).Password
    Remove-Variable tokenSeguro

Não coloque o token no texto de -Prompt.
"@
}

$script:NormalizedSonarUrl = $parsedSonarUrl.AbsoluteUri.TrimEnd('/')
$script:SonarHeaders = @{
    Authorization = "Bearer $sonarToken"
    Accept = "application/json"
}

if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $safeProjectKey = $ProjectKey -replace '[^A-Za-z0-9_.-]', '_'
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $OutputDirectory = Join-Path $PSScriptRoot "target/sonar-export/$safeProjectKey-$timestamp"
}

$script:OutputPath = [IO.Path]::GetFullPath($OutputDirectory)
if (Test-Path -LiteralPath $script:OutputPath) {
    throw "O diretório de saída já existe: '$($script:OutputPath)'."
}

New-Item -ItemType Directory -Path $script:OutputPath | Out-Null

Write-Host "Project Key: $ProjectKey"
Write-Host "SonarQube:   $script:NormalizedSonarUrl"
Write-Host "Branch:      $(if ($Branch) { $Branch } else { '<principal>' })"
Write-Host "Saída:       $script:OutputPath"

Write-Step "Exportando metadados"

$serverVersion = Invoke-SonarApi -Path "api/server/version"
$webApiCatalog = Invoke-SonarApi -Path "api/webservices/list"
$analysisParameters = @{ project = $ProjectKey; ps = 1 }
if ($Branch) {
    $analysisParameters["branch"] = $Branch
}
$latestAnalysis = Invoke-SonarApi `
    -Path "api/project_analyses/search" `
    -Parameters $analysisParameters
$localGitRevision = Get-LocalGitRevision

Write-JsonFile `
    -Value $webApiCatalog `
    -Path (Join-Path $script:OutputPath "web-api-catalog.json")
Write-JsonFile `
    -Value $latestAnalysis `
    -Path (Join-Path $script:OutputPath "latest-analysis.json")

Write-Step "Exportando issues"

$issueParameters = @{ componentKeys = $ProjectKey }
if (-not $IncludeResolved) {
    $issueParameters["resolved"] = "false"
}
if ($Branch) {
    $issueParameters["branch"] = $Branch
}

$issueResult = Get-SonarPagedCollection `
    -Path "api/issues/search" `
    -Parameters $issueParameters `
    -CollectionProperty "issues" `
    -RawFilePrefix "issues"

$issuesDocument = [ordered]@{
    projectKey = $ProjectKey
    branch = $Branch
    includesResolved = [bool]$IncludeResolved
    totalReportedByServer = $issueResult.Total
    exportedCount = $issueResult.Items.Count
    pages = $issueResult.Pages
    issues = @($issueResult.Items)
}
Write-JsonFile `
    -Value $issuesDocument `
    -Path (Join-Path $script:OutputPath "issues.json")

$issueRows = @($issueResult.Items | ForEach-Object {
    $textRange = Get-PropertyValue -InputObject $_ -Name "textRange"
    [pscustomobject]@{
        key = Get-PropertyValue -InputObject $_ -Name "key"
        rule = Get-PropertyValue -InputObject $_ -Name "rule"
        severity = Get-PropertyValue -InputObject $_ -Name "severity"
        type = Get-PropertyValue -InputObject $_ -Name "type"
        status = Get-PropertyValue -InputObject $_ -Name "status"
        resolution = Get-PropertyValue -InputObject $_ -Name "resolution"
        component = Get-PropertyValue -InputObject $_ -Name "component"
        project = Get-PropertyValue -InputObject $_ -Name "project"
        line = Get-PropertyValue -InputObject $_ -Name "line"
        startLine = Get-PropertyValue -InputObject $textRange -Name "startLine"
        endLine = Get-PropertyValue -InputObject $textRange -Name "endLine"
        startOffset = Get-PropertyValue -InputObject $textRange -Name "startOffset"
        endOffset = Get-PropertyValue -InputObject $textRange -Name "endOffset"
        message = Get-PropertyValue -InputObject $_ -Name "message"
        effort = Get-PropertyValue -InputObject $_ -Name "effort"
        creationDate = Get-PropertyValue -InputObject $_ -Name "creationDate"
        updateDate = Get-PropertyValue -InputObject $_ -Name "updateDate"
    }
})
$issueColumns = @(
    "key", "rule", "severity", "type", "status", "resolution", "component", "project",
    "line", "startLine", "endLine", "startOffset", "endOffset", "message", "effort",
    "creationDate", "updateDate"
)
Export-CsvFile `
    -Rows $issueRows `
    -Columns $issueColumns `
    -Path (Join-Path $script:OutputPath "issues.csv")

Write-Step "Exportando métricas de duplicação"

$measureParameters = @{
    component = $ProjectKey
    qualifiers = "FIL"
    metricKeys = "duplicated_lines,duplicated_blocks,duplicated_lines_density,ncloc"
}
if ($Branch) {
    $measureParameters["branch"] = $Branch
}

$measureResult = Get-SonarPagedCollection `
    -Path "api/measures/component_tree" `
    -Parameters $measureParameters `
    -CollectionProperty "components" `
    -RawFilePrefix "duplication-measures"

$duplicationFiles = @($measureResult.Items | ForEach-Object {
    [pscustomobject]@{
        key = $_.key
        path = $_.path
        language = $_.language
        ncloc = Get-MeasureValue -Component $_ -Metric "ncloc"
        duplicatedLines = Get-MeasureValue -Component $_ -Metric "duplicated_lines"
        duplicatedBlocks = Get-MeasureValue -Component $_ -Metric "duplicated_blocks"
        duplicatedLinesDensity = Get-MeasureValue `
            -Component $_ `
            -Metric "duplicated_lines_density"
    }
})

Write-JsonFile `
    -Value $duplicationFiles `
    -Path (Join-Path $script:OutputPath "duplication-files.json")
$duplicationFileColumns = @(
    "key", "path", "language", "ncloc", "duplicatedLines", "duplicatedBlocks",
    "duplicatedLinesDensity"
)
Export-CsvFile `
    -Rows $duplicationFiles `
    -Columns $duplicationFileColumns `
    -Path (Join-Path $script:OutputPath "duplication-files.csv")

Write-Step "Exportando blocos de duplicação"

$pathByKey = @{}
foreach ($file in $duplicationFiles) {
    $pathByKey[$file.key] = $file.path
}

$duplicationDetails = [System.Collections.Generic.List[object]]::new()
$duplicationRows = [System.Collections.Generic.List[object]]::new()
$fileIndex = 0

foreach ($file in @($duplicationFiles | Where-Object { [int]$_.duplicatedLines -gt 0 })) {
    $fileIndex++
    $duplicationParameters = @{ key = $file.key }
    if ($Branch) {
        $duplicationParameters["branch"] = $Branch
    }

    $duplicationResponse = Invoke-SonarApi `
        -Path "api/duplications/show" `
        -Parameters $duplicationParameters
    Write-JsonFile `
        -Value $duplicationResponse `
        -Path (Join-Path $script:OutputPath "duplications-file-$('{0:D4}' -f $fileIndex).json")

    $duplicationDetails.Add([pscustomobject]@{
        requestedFileKey = $file.key
        requestedFilePath = $file.path
        response = $duplicationResponse
    })

    $groupIndex = 0
    foreach ($group in @($duplicationResponse.duplications)) {
        $groupIndex++
        foreach ($block in @($group.blocks)) {
            $reference = [string]$block._ref
            $fileProperty = $null
            if ($null -ne $duplicationResponse.PSObject.Properties["files"]) {
                $fileProperty = $duplicationResponse.files.PSObject.Properties[$reference]
            }

            $blockFileKey = $file.key
            $blockPath = $file.path
            if ($null -ne $fileProperty) {
                $blockFileKey = $fileProperty.Value.key
                $blockPath = $fileProperty.Value.name
            }
            if ($pathByKey.ContainsKey($blockFileKey)) {
                $blockPath = $pathByKey[$blockFileKey]
            }

            $fromLine = [int]$block.from
            $lineCount = [int]$block.size
            $duplicationRows.Add([pscustomobject]@{
                requestedFileKey = $file.key
                group = $groupIndex
                reference = $reference
                fileKey = $blockFileKey
                filePath = $blockPath
                fromLine = $fromLine
                toLine = $fromLine + $lineCount - 1
                lineCount = $lineCount
            })
        }
    }
}

Write-JsonFile `
    -Value $duplicationDetails.ToArray() `
    -Path (Join-Path $script:OutputPath "duplication-blocks.json") `
    -Depth 40
$duplicationLineColumns = @(
    "requestedFileKey", "group", "reference", "fileKey", "filePath", "fromLine",
    "toLine", "lineCount"
)
Export-CsvFile `
    -Rows $duplicationRows.ToArray() `
    -Columns $duplicationLineColumns `
    -Path (Join-Path $script:OutputPath "duplication-lines.csv")

Write-Step "Criando manifesto"

$manifest = [ordered]@{
    schemaVersion = 1
    exportedAtUtc = (Get-Date).ToUniversalTime().ToString("o")
    sonarUrl = $script:NormalizedSonarUrl
    sonarServerVersion = [string]$serverVersion
    projectKey = $ProjectKey
    branch = $Branch
    localGitRevision = $localGitRevision
    includesResolvedIssues = [bool]$IncludeResolved
    issueTotalReportedByServer = $issueResult.Total
    issueExportedCount = $issueResult.Items.Count
    duplicationFileCount = $duplicationFiles.Count
    duplicationBlockRowCount = $duplicationRows.Count
    authenticationIncluded = $false
    endpoints = @(
        "api/server/version",
        "api/webservices/list",
        "api/project_analyses/search",
        "api/issues/search",
        "api/measures/component_tree",
        "api/duplications/show"
    )
}
Write-JsonFile `
    -Value $manifest `
    -Path (Join-Path $script:OutputPath "manifest.json")

$archivePath = $null
if (-not $NoArchive) {
    $archivePath = "$($script:OutputPath).zip"
    Compress-Archive -Path (Join-Path $script:OutputPath "*") -DestinationPath $archivePath
}

Write-Step "Exportação concluída"
Write-Host "Diretório: $script:OutputPath"
if ($archivePath) {
    Write-Host "Pacote:    $archivePath"
}
Write-Host "Issues:    $($issueResult.Items.Count)"
Write-Host "Arquivos:  $($duplicationFiles.Count)"
Write-Host "Blocos:    $($duplicationRows.Count)"
