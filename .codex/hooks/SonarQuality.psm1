Set-StrictMode -Version Latest

function Get-ObjectProperty {
    param(
        [AllowNull()][object]$InputObject,
        [Parameter(Mandatory)][string]$Name,
        [AllowNull()][object]$Default = $null
    )

    if ($null -eq $InputObject) {
        return $Default
    }
    $property = $InputObject.PSObject.Properties[$Name]
    if ($null -eq $property) {
        return $Default
    }
    return $property.Value
}

function ConvertTo-SonarQueryString {
    param([Parameter(Mandatory)][hashtable]$Parameters)

    return (($Parameters.GetEnumerator() | Sort-Object Key | ForEach-Object {
        "{0}={1}" -f
            [Uri]::EscapeDataString([string]$_.Key),
            [Uri]::EscapeDataString([string]$_.Value)
    }) -join "&")
}

function Get-SonarAuthenticationHeaders {
    $token = [Environment]::GetEnvironmentVariable("SONAR_TOKEN", "Process")
    if ([string]::IsNullOrWhiteSpace($token)) {
        throw "SONAR_TOKEN não está disponível no processo do agente."
    }
    return @{ Authorization = "Bearer $token" }
}

function Assert-SonarUrl {
    param([Parameter(Mandatory)][string]$SonarUrl)

    $parsed = $null
    if (-not [Uri]::TryCreate($SonarUrl, [UriKind]::Absolute, [ref]$parsed) -or
        $parsed.Scheme -notin @("http", "https")) {
        throw "SonarUrl deve ser uma URL absoluta HTTP ou HTTPS."
    }
    if (-not [string]::IsNullOrWhiteSpace($parsed.UserInfo)) {
        throw "SonarUrl não pode conter usuário ou senha."
    }
    return $parsed.AbsoluteUri.TrimEnd('/')
}

function Invoke-SonarQualityApi {
    param(
        [Parameter(Mandatory)][string]$SonarUrl,
        [Parameter(Mandatory)][string]$Path,
        [hashtable]$Parameters = @{},
        [int]$TimeoutSec = 30
    )

    $baseUrl = Assert-SonarUrl -SonarUrl $SonarUrl
    $query = ConvertTo-SonarQueryString -Parameters $Parameters
    $uri = "$baseUrl/$($Path.TrimStart('/'))"
    if (-not [string]::IsNullOrWhiteSpace($query)) {
        $uri = "$uri`?$query"
    }
    return Invoke-RestMethod -Method Get -Uri $uri `
        -Headers (Get-SonarAuthenticationHeaders) -TimeoutSec $TimeoutSec
}

function ConvertTo-QualityIssue {
    param([Parameter(Mandatory)][object]$Issue)

    $normalizedImpacts = @()
    foreach ($impact in @(Get-ObjectProperty -InputObject $Issue -Name "impacts" -Default @())) {
        $normalizedImpacts += [pscustomobject]@{
            softwareQuality = [string](Get-ObjectProperty $impact "softwareQuality" "")
            severity = [string](Get-ObjectProperty $impact "severity" "")
        }
    }
    return [pscustomobject]@{
        key = [string](Get-ObjectProperty $Issue "key" "")
        rule = [string](Get-ObjectProperty $Issue "rule" "")
        severity = [string](Get-ObjectProperty $Issue "severity" "")
        impacts = $normalizedImpacts
    }
}

function Get-SonarMeasureValue {
    param(
        [Parameter(Mandatory)][object[]]$Measures,
        [Parameter(Mandatory)][string]$Metric
    )

    $measure = @($Measures | Where-Object { $_.metric -eq $Metric } | Select-Object -First 1)
    if ($measure.Count -eq 0 -or [string]::IsNullOrWhiteSpace([string]$measure[0].value)) {
        throw "A métrica '$Metric' não foi retornada pelo SonarQube."
    }
    $value = 0.0
    if (-not [double]::TryParse(
        [string]$measure[0].value,
        [Globalization.NumberStyles]::Float,
        [Globalization.CultureInfo]::InvariantCulture,
        [ref]$value
    )) {
        throw "Valor inválido para a métrica '$Metric': '$($measure[0].value)'."
    }
    return $value
}

function Get-SonarQualitySnapshot {
    param(
        [Parameter(Mandatory)][string]$SonarUrl,
        [Parameter(Mandatory)][string]$ProjectKey
    )

    $analyses = Invoke-SonarQualityApi -SonarUrl $SonarUrl `
        -Path "api/project_analyses/search" -Parameters @{ project = $ProjectKey; ps = 1 }
    $latestAnalysis = @((Get-ObjectProperty $analyses "analyses" @()) | Select-Object -First 1)
    if ($latestAnalysis.Count -eq 0) {
        throw "O projeto '$ProjectKey' ainda não possui análise no SonarQube."
    }

    $measureResponse = Invoke-SonarQualityApi -SonarUrl $SonarUrl `
        -Path "api/measures/component" `
        -Parameters @{
            component = $ProjectKey
            metricKeys = "coverage,duplicated_lines_density"
        }
    $component = Get-ObjectProperty $measureResponse "component"
    $measures = @((Get-ObjectProperty $component "measures" @()))

    $issues = [System.Collections.Generic.List[object]]::new()
    $page = 1
    do {
        $response = Invoke-SonarQualityApi -SonarUrl $SonarUrl `
            -Path "api/issues/search" `
            -Parameters @{ componentKeys = $ProjectKey; resolved = "false"; p = $page; ps = 500 }
        $pageIssues = @((Get-ObjectProperty $response "issues" @()))
        foreach ($issue in $pageIssues) {
            $issues.Add((ConvertTo-QualityIssue -Issue $issue))
        }
        $paging = Get-ObjectProperty $response "paging"
        $total = [int](Get-ObjectProperty $paging "total" $issues.Count)
        if ($pageIssues.Count -eq 0 -and $issues.Count -lt $total) {
            throw "A paginação de issues terminou antes da contagem informada pelo SonarQube."
        }
        $page++
    } while ($issues.Count -lt $total)

    return [pscustomobject]@{
        capturedAt = [DateTimeOffset]::UtcNow.ToString("o")
        analysisKey = [string](Get-ObjectProperty $latestAnalysis[0] "key" "")
        analysisRevision = [string](Get-ObjectProperty $latestAnalysis[0] "revision" "")
        issueCount = $issues.Count
        issues = @($issues)
        metrics = [pscustomobject]@{
            coverage = Get-SonarMeasureValue -Measures $measures -Metric "coverage"
            duplicatedLinesDensity = Get-SonarMeasureValue `
                -Measures $measures -Metric "duplicated_lines_density"
        }
    }
}

function Test-BlockingSonarIssue {
    param([Parameter(Mandatory)][object]$Issue)

    $severities = [System.Collections.Generic.List[string]]::new()
    $legacySeverity = [string](Get-ObjectProperty $Issue "severity" "")
    if (-not [string]::IsNullOrWhiteSpace($legacySeverity)) {
        $severities.Add($legacySeverity.ToUpperInvariant())
    }
    foreach ($impact in @(Get-ObjectProperty $Issue "impacts" @())) {
        $impactSeverity = [string](Get-ObjectProperty $impact "severity" "")
        if (-not [string]::IsNullOrWhiteSpace($impactSeverity)) {
            $severities.Add($impactSeverity.ToUpperInvariant())
        }
    }
    return $null -ne ($severities | Where-Object { $_ -in @("HIGH", "BLOCKER", "CRITICAL") } |
        Select-Object -First 1)
}

function Compare-SonarQualitySnapshot {
    param(
        [Parameter(Mandatory)][object]$Baseline,
        [Parameter(Mandatory)][object]$Current,
        [double]$MinimumCoverage = 85.0,
        [double]$MaximumDuplication = 5.0
    )

    $baselineKeys = @((Get-ObjectProperty $Baseline "issues" @()) | ForEach-Object {
        [string](Get-ObjectProperty $_ "key" "")
    } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    $currentIssues = @((Get-ObjectProperty $Current "issues" @()))
    $currentKeys = @($currentIssues | ForEach-Object {
        [string](Get-ObjectProperty $_ "key" "")
    } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    $newIssueKeys = @($currentKeys | Where-Object { $_ -notin $baselineKeys } | Sort-Object -Unique)
    $blockingKeys = @($currentIssues | Where-Object { Test-BlockingSonarIssue $_ } |
        ForEach-Object { [string](Get-ObjectProperty $_ "key" "") } | Sort-Object -Unique)

    $currentMetrics = Get-ObjectProperty $Current "metrics"
    $coverage = [double](Get-ObjectProperty $currentMetrics "coverage" 0)
    $duplication = [double](Get-ObjectProperty $currentMetrics "duplicatedLinesDensity" 100)
    $baselineCount = [int](Get-ObjectProperty $Baseline "issueCount" $baselineKeys.Count)
    $currentCount = [int](Get-ObjectProperty $Current "issueCount" $currentKeys.Count)
    $violations = [System.Collections.Generic.List[object]]::new()

    if ($currentCount -gt $baselineCount -or $newIssueKeys.Count -gt 0) {
        $violations.Add([pscustomobject]@{
            Code = "NEW_ISSUES"
            Message = "Issues abertas: $currentCount (baseline: $baselineCount); novas: $($newIssueKeys.Count)."
        })
    }
    if ($blockingKeys.Count -gt 0) {
        $violations.Add([pscustomobject]@{
            Code = "HIGH_OR_BLOCKER"
            Message = "Foram encontradas $($blockingKeys.Count) issues HIGH/BLOCKER/CRITICAL."
        })
    }
    if ($coverage -lt $MinimumCoverage) {
        $violations.Add([pscustomobject]@{
            Code = "COVERAGE"
            Message = "Cobertura $coverage% abaixo do mínimo de $MinimumCoverage%."
        })
    }
    if ($duplication -gt $MaximumDuplication) {
        $violations.Add([pscustomobject]@{
            Code = "DUPLICATION"
            Message = "Duplicação $duplication% acima do máximo de $MaximumDuplication%."
        })
    }

    return [pscustomobject]@{
        Passed = $violations.Count -eq 0
        Violations = @($violations)
        NewIssueKeys = $newIssueKeys
        HighOrBlockerIssueKeys = $blockingKeys
        Coverage = $coverage
        DuplicatedLinesDensity = $duplication
        IssueCount = $currentCount
        BaselineIssueCount = $baselineCount
    }
}

function Get-SonarCodeFingerprint {
    param([Parameter(Mandatory)][string]$RepositoryRoot)

    $root = [IO.Path]::GetFullPath($RepositoryRoot)
    $files = [System.Collections.Generic.List[IO.FileInfo]]::new()
    $sourceRoot = Join-Path $root "src"
    if (Test-Path -LiteralPath $sourceRoot -PathType Container) {
        foreach ($file in @(Get-ChildItem -LiteralPath $sourceRoot -Recurse -File | Sort-Object FullName)) {
            $files.Add($file)
        }
    }

    foreach ($relativePath in @(
        "pom.xml",
        "mvnw",
        "mvnw.cmd",
        "Dockerfile",
        "docker-compose.yml",
        "docker-compose.yaml",
        "compose.yml",
        "compose.yaml",
        ".codex/hooks.json"
    )) {
        $candidate = Join-Path $root $relativePath
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            $files.Add((Get-Item -LiteralPath $candidate))
        }
    }
    foreach ($file in @(Get-ChildItem -LiteralPath $root -File -Filter "*.ps1")) {
        $files.Add($file)
    }
    foreach ($relativeDirectory in @(".mvn", ".codex/hooks", "test/powershell")) {
        $directory = Join-Path $root $relativeDirectory
        if (Test-Path -LiteralPath $directory -PathType Container) {
            foreach ($file in @(Get-ChildItem -LiteralPath $directory -Recurse -File)) {
                $files.Add($file)
            }
        }
    }

    $lines = foreach ($file in @($files | Sort-Object FullName -Unique)) {
        $relativePath = $file.FullName.Substring($root.Length).TrimStart('\', '/')
        "$relativePath=$((Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash)"
    }
    $bytes = [Text.Encoding]::UTF8.GetBytes(($lines -join "`n"))
    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        return ([BitConverter]::ToString($sha.ComputeHash($bytes))).Replace("-", "").ToLowerInvariant()
    }
    finally {
        $sha.Dispose()
    }
}

function Get-SonarAgentStateDirectory {
    param([Parameter(Mandatory)][string]$RepositoryRoot)

    $override = [Environment]::GetEnvironmentVariable("SONAR_AGENT_STATE_DIRECTORY", "Process")
    if (-not [string]::IsNullOrWhiteSpace($override)) {
        return [IO.Path]::GetFullPath($override)
    }
    return Join-Path ([IO.Path]::GetFullPath($RepositoryRoot)) ".codex/.state"
}

function Get-SonarOfflineReportRoot {
    param([Parameter(Mandatory)][string]$RepositoryRoot)

    $override = [Environment]::GetEnvironmentVariable(
        "SONAR_AGENT_OFFLINE_REPORT_ROOT",
        "Process"
    )
    if (-not [string]::IsNullOrWhiteSpace($override)) {
        return [IO.Path]::GetFullPath($override)
    }
    return Join-Path ([IO.Path]::GetFullPath($RepositoryRoot)) "sonar"
}

function Get-SonarOfflineReportCandidates {
    param([Parameter(Mandatory)][string]$RepositoryRoot)

    $reportRoot = Get-SonarOfflineReportRoot -RepositoryRoot $RepositoryRoot
    if (-not (Test-Path -LiteralPath $reportRoot -PathType Container)) {
        return @()
    }

    $directories = [System.Collections.Generic.List[string]]::new()
    foreach ($manifest in @(Get-ChildItem -LiteralPath $reportRoot -Recurse -File `
        -Filter "manifest.json")) {
        $directories.Add($manifest.Directory.FullName)
    }
    if ((Test-Path -LiteralPath (Join-Path $reportRoot "issues.json") -PathType Leaf) -or
        (Test-Path -LiteralPath (Join-Path $reportRoot "issues.csv") -PathType Leaf)) {
        $directories.Add($reportRoot)
    }

    return @($directories | Sort-Object -Unique | ForEach-Object {
        $relative = $_.Substring($reportRoot.TrimEnd('\', '/').Length).TrimStart('\', '/')
        $candidate = if ([string]::IsNullOrWhiteSpace($relative)) {
            "sonar"
        } else {
            "sonar/$($relative -replace '\\', '/')"
        }
        if ($candidate -match '^[A-Za-z0-9._/-]+$') { $candidate }
    })
}

function Get-SonarOfflineReportSummary {
    param(
        [Parameter(Mandatory)][string]$ReportPath,
        [Parameter(Mandatory)][string]$RepositoryRoot
    )

    $allowedRoot = [IO.Path]::GetFullPath(
        (Get-SonarOfflineReportRoot -RepositoryRoot $RepositoryRoot)
    ).TrimEnd('\', '/')
    $resolvedReportPath = [IO.Path]::GetFullPath($ReportPath).TrimEnd('\', '/')
    $allowedPrefix = $allowedRoot + [IO.Path]::DirectorySeparatorChar
    if ($resolvedReportPath -ne $allowedRoot -and
        -not $resolvedReportPath.StartsWith($allowedPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "OfflineReportPath deve estar dentro da pasta 'sonar/' do projeto."
    }
    if (-not (Test-Path -LiteralPath $resolvedReportPath -PathType Container)) {
        throw "Pacote offline não encontrado: '$resolvedReportPath'."
    }

    $manifestPath = Join-Path $resolvedReportPath "manifest.json"
    $issuesJsonPath = Join-Path $resolvedReportPath "issues.json"
    $issuesCsvPath = Join-Path $resolvedReportPath "issues.csv"
    if (-not (Test-Path -LiteralPath $issuesJsonPath -PathType Leaf) -and
        -not (Test-Path -LiteralPath $issuesCsvPath -PathType Leaf)) {
        throw "O pacote offline deve conter issues.json ou issues.csv."
    }
    if ((Test-Path -LiteralPath $manifestPath -PathType Leaf) -and
        (Get-Item -LiteralPath $manifestPath).Length -gt 5MB) {
        throw "manifest.json excede o limite de 5 MB."
    }
    $selectedIssuesPath = if (Test-Path -LiteralPath $issuesJsonPath -PathType Leaf) {
        $issuesJsonPath
    } else {
        $issuesCsvPath
    }
    if ((Get-Item -LiteralPath $selectedIssuesPath).Length -gt 100MB) {
        throw "O arquivo de issues excede o limite de 100 MB."
    }

    $manifest = if (Test-Path -LiteralPath $manifestPath -PathType Leaf) {
        Get-Content -Raw -LiteralPath $manifestPath | ConvertFrom-Json
    } else {
        [pscustomobject]@{}
    }
    $issues = if (Test-Path -LiteralPath $issuesJsonPath -PathType Leaf) {
        $json = Get-Content -Raw -LiteralPath $issuesJsonPath | ConvertFrom-Json
        $issuesProperty = $json.PSObject.Properties["issues"]
        if ($null -ne $issuesProperty) { @($issuesProperty.Value) } else { @($json) }
    } else {
        @(Import-Csv -LiteralPath $issuesCsvPath)
    }

    $severityCounts = [ordered]@{}
    $ruleCounts = [ordered]@{}
    $blockingCount = 0
    foreach ($issue in $issues) {
        $severity = [string](Get-ObjectProperty $issue "severity" "UNKNOWN")
        if ([string]::IsNullOrWhiteSpace($severity)) { $severity = "UNKNOWN" }
        $severity = $severity.ToUpperInvariant()
        if (-not $severityCounts.Contains($severity)) { $severityCounts[$severity] = 0 }
        $severityCounts[$severity]++
        $rule = [string](Get-ObjectProperty $issue "rule" "UNKNOWN")
        if ([string]::IsNullOrWhiteSpace($rule)) { $rule = "UNKNOWN" }
        if (-not $ruleCounts.Contains($rule)) { $ruleCounts[$rule] = 0 }
        $ruleCounts[$rule]++
        if (Test-BlockingSonarIssue -Issue $issue) { $blockingCount++ }
    }

    $evidenceFiles = @($manifestPath, $issuesJsonPath, $issuesCsvPath) | Where-Object {
        Test-Path -LiteralPath $_ -PathType Leaf
    }
    $fingerprintLines = @($evidenceFiles | Sort-Object | ForEach-Object {
        "$(Split-Path -Leaf $_)=$((Get-FileHash -LiteralPath $_ -Algorithm SHA256).Hash)"
    })
    $bytes = [Text.Encoding]::UTF8.GetBytes(($fingerprintLines -join "`n"))
    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        $fingerprint = ([BitConverter]::ToString($sha.ComputeHash($bytes))).Replace("-", "").ToLowerInvariant()
    } finally {
        $sha.Dispose()
    }

    $relativeSuffix = $resolvedReportPath.Substring($allowedRoot.Length).TrimStart('\', '/') `
        -replace '\\', '/'
    $relativePath = if ([string]::IsNullOrWhiteSpace($relativeSuffix)) {
        "sonar"
    } else {
        "sonar/$relativeSuffix"
    }
    return [pscustomobject]@{
        path = $relativePath
        fingerprint = $fingerprint
        issueCount = $issues.Count
        blockingIssueCount = $blockingCount
        severityCounts = [pscustomobject]$severityCounts
        ruleCounts = [pscustomobject]$ruleCounts
        projectKey = [string](Get-ObjectProperty $manifest "projectKey" "")
        sonarUrl = [string](Get-ObjectProperty $manifest "sonarUrl" "")
        importedAt = [DateTimeOffset]::UtcNow.ToString("o")
        immutableEvidence = $true
    }
}

function Write-SonarAgentState {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][object]$State
    )

    $directory = Split-Path -Parent $Path
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
    $State | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $Path -Encoding UTF8
}

function Wait-SonarComputeEngine {
    param(
        [Parameter(Mandatory)][string]$SonarUrl,
        [Parameter(Mandatory)][string]$TaskUrl,
        [int]$TimeoutSec = 180
    )

    $base = [Uri](Assert-SonarUrl -SonarUrl $SonarUrl)
    $task = $null
    if (-not [Uri]::TryCreate($TaskUrl, [UriKind]::Absolute, [ref]$task) -or
        $task.Scheme -ne $base.Scheme -or $task.Host -ne $base.Host -or $task.Port -ne $base.Port) {
        throw "A URL da tarefa Compute Engine não pertence ao servidor SonarQube configurado."
    }

    $deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSec)
    do {
        $response = Invoke-RestMethod -Method Get -Uri $task.AbsoluteUri `
            -Headers (Get-SonarAuthenticationHeaders) -TimeoutSec 30
        $ceTask = Get-ObjectProperty $response "task"
        $status = [string](Get-ObjectProperty $ceTask "status" "")
        if ($status -eq "SUCCESS") {
            return $ceTask
        }
        if ($status -in @("FAILED", "CANCELED")) {
            throw "A tarefa Compute Engine terminou com status '$status'."
        }
        Start-Sleep -Seconds 2
    } while ([DateTimeOffset]::UtcNow -lt $deadline)
    throw "Tempo limite excedido aguardando a tarefa Compute Engine."
}

Export-ModuleMember -Function @(
    "Assert-SonarUrl",
    "Compare-SonarQualitySnapshot",
    "Get-SonarAgentStateDirectory",
    "Get-SonarCodeFingerprint",
    "Get-SonarOfflineReportCandidates",
    "Get-SonarOfflineReportSummary",
    "Get-SonarQualitySnapshot",
    "Wait-SonarComputeEngine",
    "Write-SonarAgentState"
)
