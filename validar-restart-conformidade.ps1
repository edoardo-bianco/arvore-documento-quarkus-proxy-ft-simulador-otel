[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$repositorio = (Resolve-Path $PSScriptRoot).Path
$diretorioProva = [System.IO.Path]::GetFullPath(
    (Join-Path $repositorio "target\restart-proof"))
$prefixoPermitido = $repositorio + [System.IO.Path]::DirectorySeparatorChar
if (-not $diretorioProva.StartsWith(
        $prefixoPermitido,
        [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Diretório da prova de restart fora do repositório"
}

$arquivoEstado = Join-Path $diretorioProva "instance-id.txt"
$sufixo = [Guid]::NewGuid().ToString("N")
$nomeCouchDb = "simtr-restart-couch-$sufixo"
$nomeValkey = "simtr-restart-valkey-$sufixo"
$usuarioCouchDb = "restart-test"
$senhaCouchDb = [Guid]::NewGuid().ToString()
$databaseCouchDb = "conformidade-restart"
$couchDbIniciado = $false
$valkeyIniciado = $false
$variaveis = @(
    "CONFORMIDADE_PERSISTENCIA_BACKEND",
    "COUCHDB_HOST",
    "COUCHDB_PORT",
    "COUCHDB_DATABASE",
    "COUCHDB_USERNAME",
    "COUCHDB_PASSWORD",
    "QUARKUS_REDIS_HOSTS"
)
$valoresAnteriores = @{}

foreach ($nomeVariavel in $variaveis) {
    $valoresAnteriores[$nomeVariavel] =
        [Environment]::GetEnvironmentVariable($nomeVariavel, "Process")
}

function Invoke-DockerChecked {
    param([Parameter(Mandatory)][string[]]$Argumentos)

    $saida = & docker @Argumentos
    if ($LASTEXITCODE -ne 0) {
        throw "Comando Docker da prova de restart falhou"
    }
    return $saida
}

function Get-PortaMapeada {
    param(
        [Parameter(Mandatory)][string]$Container,
        [Parameter(Mandatory)][string]$PortaInterna
    )

    $mapeamento = Invoke-DockerChecked @("port", $Container, $PortaInterna)
    $linha = @($mapeamento)[0]
    if ($linha -notmatch ":(?<porta>[0-9]+)$") {
        throw "Mapeamento de porta inválido para $Container"
    }
    return [int]$Matches.porta
}

function Wait-CouchDb {
    param(
        [Parameter(Mandatory)][int]$Porta,
        [Parameter(Mandatory)][hashtable]$Headers
    )

    for ($tentativa = 1; $tentativa -le 30; $tentativa++) {
        try {
            $resposta = Invoke-WebRequest `
                -Uri "http://127.0.0.1:$Porta/_up" `
                -Headers $Headers `
                -TimeoutSec 2
            if ($resposta.StatusCode -eq 200) {
                return
            }
        } catch {
            Start-Sleep -Seconds 1
        }
    }
    throw "CouchDB não ficou pronto para a prova de restart"
}

function Wait-Valkey {
    param([Parameter(Mandatory)][string]$Container)

    for ($tentativa = 1; $tentativa -le 30; $tentativa++) {
        $resposta = & docker exec $Container redis-cli ping 2>$null
        if ($LASTEXITCODE -eq 0 -and $resposta -eq "PONG") {
            return
        }
        Start-Sleep -Seconds 1
    }
    throw "Valkey não ficou pronto para a prova de restart"
}

function Invoke-FaseMaven {
    param([Parameter(Mandatory)][string]$Metodo)

    & mvn -q `
        "-Dtest=AnaliseConformidadeRestartEntreJvmTest#$Metodo" `
        "-Drestart.proof.enabled=true" `
        "-Drestart.proof.state-file=$arquivoEstado" `
        test
    if ($LASTEXITCODE -ne 0) {
        throw "A fase $Metodo da prova entre JVMs falhou"
    }
}

try {
    New-Item -ItemType Directory -Path $diretorioProva -Force | Out-Null
    Remove-Item -LiteralPath $arquivoEstado -ErrorAction SilentlyContinue

    Invoke-DockerChecked @(
        "run", "-d",
        "--name", $nomeCouchDb,
        "--label", "simtr.restart-proof=true",
        "-e", "COUCHDB_USER=$usuarioCouchDb",
        "-e", "COUCHDB_PASSWORD=$senhaCouchDb",
        "-p", "127.0.0.1::5984",
        "couchdb:3.5.2"
    ) | Out-Null
    $couchDbIniciado = $true

    Invoke-DockerChecked @(
        "run", "-d",
        "--name", $nomeValkey,
        "--label", "simtr.restart-proof=true",
        "-p", "127.0.0.1::6379",
        "valkey/valkey:7.2-alpine"
    ) | Out-Null
    $valkeyIniciado = $true

    $portaCouchDb = Get-PortaMapeada $nomeCouchDb "5984/tcp"
    $portaValkey = Get-PortaMapeada $nomeValkey "6379/tcp"
    $credencial = [Convert]::ToBase64String(
        [Text.Encoding]::UTF8.GetBytes("$usuarioCouchDb`:$senhaCouchDb"))
    $headers = @{ Authorization = "Basic $credencial" }

    Wait-CouchDb $portaCouchDb $headers
    Wait-Valkey $nomeValkey
    $criacao = Invoke-WebRequest `
        -Uri "http://127.0.0.1:$portaCouchDb/$databaseCouchDb" `
        -Method Put `
        -Headers $headers `
        -SkipHttpErrorCheck
    if ($criacao.StatusCode -notin @(201, 412)) {
        throw "Não foi possível criar o database CouchDB da prova"
    }

    [Environment]::SetEnvironmentVariable(
        "CONFORMIDADE_PERSISTENCIA_BACKEND", "couchdb", "Process")
    [Environment]::SetEnvironmentVariable("COUCHDB_HOST", "127.0.0.1", "Process")
    [Environment]::SetEnvironmentVariable(
        "COUCHDB_PORT", $portaCouchDb.ToString(), "Process")
    [Environment]::SetEnvironmentVariable(
        "COUCHDB_DATABASE", $databaseCouchDb, "Process")
    [Environment]::SetEnvironmentVariable(
        "COUCHDB_USERNAME", $usuarioCouchDb, "Process")
    [Environment]::SetEnvironmentVariable(
        "COUCHDB_PASSWORD", $senhaCouchDb, "Process")
    [Environment]::SetEnvironmentVariable(
        "QUARKUS_REDIS_HOSTS", "redis://127.0.0.1:$portaValkey", "Process")

    Push-Location $repositorio
    try {
        Write-Host "Fase 1/2: criando checkpoint WAITING na primeira JVM"
        Invoke-FaseMaven "criaInstanciaEmEsperaNaPrimeiraJvm"
        Write-Host "Fase 2/2: restaurando e retomando na segunda JVM"
        Invoke-FaseMaven "restauraERetomaNaSegundaJvm"
    } finally {
        Pop-Location
    }

    Write-Host "Prova de restart entre JVMs concluída com sucesso"
} finally {
    foreach ($nomeVariavel in $variaveis) {
        [Environment]::SetEnvironmentVariable(
            $nomeVariavel,
            $valoresAnteriores[$nomeVariavel],
            "Process")
    }
    if ($valkeyIniciado) {
        & docker rm -f $nomeValkey 2>$null | Out-Null
    }
    if ($couchDbIniciado) {
        & docker rm -f $nomeCouchDb 2>$null | Out-Null
    }
}
