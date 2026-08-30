# Guia de verificação da PoC de conformidade

## Objetivo

Este guia permite verificar, de forma reproduzível, o que foi desenvolvido na PoC de análise de
conformidade com Quarkus Flow, Ollama e revisão humana. Ele separa as evidências em cinco níveis:

1. contratos e comportamento cobertos pela suíte automatizada;
2. fluxo funcional completo em uma réplica local;
3. recuperação depois do restart da aplicação;
4. duas réplicas, Leases e failover em Kubernetes local;
5. qualidade estática e gate externo do Azure Cosmos DB for NoSQL.

Executar apenas a suíte comprova o comportamento isolado e integrado coberto por testes. Para
comprovar a experiência completa, execute também a demonstração local pela página. As provas de
restart, Kubernetes e Cosmos são opt-in e devem ser executadas conforme o objetivo da validação.

## Escopo comprovável e limite da conclusão

O roteiro local pode comprovar:

- `POST -> polling GET -> PUT de revisão -> GET concluído`;
- preservação de `correlationId`, `instanceId`, `identificadorDocumento`,
  `identificadorChecklist` e `versaoChecklist`;
- consulta de checklist pelo simulador, análise no Ollama local e revisão humana obrigatória;
- persistência documental no CouchDB e checkpoint técnico no Valkey;
- retomada depois do restart somente da aplicação;
- duas réplicas, Leases estáveis, revisão cross-pod e substituição do pod owner;
- replay idempotente, telemetria sanitizada e fronteiras arquiteturais por testes.

O roteiro **não comprova prontidão produtiva**. A integração real com Azure Cosmos DB for NoSQL
continua sendo gate obrigatório antes de PRD. Perda, restart, backup ou alta disponibilidade dos
próprios CouchDB e Valkey também não fazem parte das provas atuais.

## Regras de segurança

- Use somente texto, nomes, documentos e credenciais sintéticos.
- O profile `%poc` registra requests e responses completos do LangChain4j; não envie dados reais.
- Crie `.env.poc` e `.env.poc-kubernetes` apenas a partir dos exemplos versionados. Esses arquivos
  locais são ignorados pelo Git e não devem ser anexados a relatórios.
- Não informe token Sonar, chave, connection string ou segredo em comandos versionados, prints ou
  mensagens. O Cosmos produtivo usa identidade do Microsoft Entra ID.
- Não execute `docker compose down -v` se quiser preservar os documentos do CouchDB.
- Excluir o cluster kind remove também o PVC e todos os dados locais mantidos nele.

## 1. Confirmar o código que será verificado

Execute a partir da raiz do repositório:

```powershell
git status -sb
git rev-parse HEAD
```

Resultado esperado:

- a branch exibida é a branch que se deseja avaliar;
- o commit é registrado junto com a evidência;
- o worktree está limpo, ou toda alteração local está identificada antes dos testes.

Um worktree modificado não invalida automaticamente o teste, mas impede atribuir o resultado sem
ambiguidade ao commit registrado.

## 2. Conferir os pré-requisitos

Para a suíte padrão:

```powershell
java -version
mvn -version
docker version
```

São esperados Java 25, Maven funcional e Docker acessível. A suíte usa containers efêmeros para os
contratos documentais que precisam de CouchDB ou Valkey.

Para a demonstração com o modelo real:

```powershell
ollama list
```

O resultado deve conter o modelo configurado, por padrão `llama3.2:3b`, e o Ollama deve responder
em `http://localhost:11434`.

Para a prova com duas réplicas:

```powershell
kubectl version --client
kind version
```

O script também aceita o executável kind em `.tools/kind.exe` quando ele não estiver no `PATH`.

## 3. Executar a verificação automatizada padrão

Execute:

```powershell
mvn -q clean test
mvn -q verify
```

Resultado esperado:

- ambos os comandos terminam com código 0;
- não há teste com falha ou erro;
- gates opt-in podem aparecer como ignorados quando Ollama real, Cosmos real ou restart entre JVMs
  não foram habilitados;
- o relatório de cobertura é criado em `target/jacoco-report/index.html`.

A suíte padrão não depende do MTR, Ollama ou Cosmos reais. Ela usa stubs HTTP, um agente falso e
containers efêmeros quando necessário. Entre as provas versionadas estão:

| Capacidade | Testes representativos |
|---|---|
| Contrato REST/JSON | `AnaliseConformidadeApiContractTest`, `AnaliseConformidadeResourceQuarkusTest` |
| Flow e HITL | `AnaliseConformidadeFlowQuarkusTest`, `FeedNativoFlowQuarkusTest` |
| Página | `AnaliseConformidadePaginaEstaticaQuarkusTest` |
| CouchDB | `CouchDbAnaliseConformidadeStoreIntegrationTest`, `CouchDbChangesHttpClientIntegrationTest` |
| Cosmos contratual | `CosmosDbAnaliseConformidadeStoreContractTest`, `CosmosSdkCompatibilidadeTest` |
| Checkpoint | `ValkeyQuarkusTestResourceTest`, testes do contexto Flow e de restart opt-in |
| Observabilidade | `RepositorioDocumentalObservavelTest`, `ObservabilidadeFeedDocumentalTest` |
| Arquitetura | `ArchUnitProgressivoTest`, `RepositorioDocumentalContratoArquiteturalTest` |

Ao final, confirme que os comandos não alteraram arquivos versionados:

```powershell
git status --short
git diff --check
```

## 4. Preparar uma réplica local completa

Crie o arquivo de ambiente local:

```powershell
Copy-Item poc-containers.env.example .env.poc
```

Preencha localmente `COUCHDB_USERNAME`, `COUCHDB_PASSWORD` e `SIMTR_API_KEY`. Para esta PoC, use
valores exclusivamente locais e sintéticos. Confirme novamente o Ollama e suba os serviços:

```powershell
ollama list
docker compose --env-file .env.poc -f compose-poc.yml up --build -d
docker compose --env-file .env.poc -f compose-poc.yml ps -a
```

Resultado esperado:

- `couchdb`, `valkey` e `simtr-hub` ficam `healthy`;
- `ollama-check` termina com código 0;
- a aplicação fica publicada somente em `127.0.0.1:8080` por padrão;
- CouchDB e Valkey ficam publicados somente no loopback.

Verifique a readiness:

```powershell
Invoke-RestMethod http://localhost:8080/q/health/ready | ConvertTo-Json -Depth 8
```

O status global deve ser `UP`. O check `simtr-hub-conformidade-documental` deve estar `UP` e
informar `backend=couchdb`; o health do Redis/Valkey também deve estar disponível. A Lease só é
obrigatória no profile Kubernetes.

## 5. Demonstrar o fluxo HITL pela página

Abra `http://localhost:8080/poc-conformidade/` e siga estas etapas:

1. Informe `DOC-VALIDACAO-001` como identificador do documento.
2. Mantenha o checklist `1000012583` e a versão `1`.
3. Use um texto totalmente sintético, por exemplo:

   > O cliente sintético João da Silva, CPF 123.456.789-00, apresentou comprovante de endereço
   > sintético e autorizou esta validação de demonstração.

4. Selecione **Iniciar análise**.
5. Confirme que a página exibe as cinco identidades e passa por `EM_PROCESSAMENTO`.
6. Aguarde `AGUARDANDO_REVISAO`. A chamada ao Ollama pode demorar; timeout, retries e fallback
   técnico podem levar aproximadamente 181 segundos.
7. Para cada apontamento, confira o parecer, informe uma justificativa e, opcionalmente, uma
   evidência sintética. Identificador, nome e confiança permanecem somente leitura.
8. Selecione **Enviar revisão**.
9. Confirme que o estado termina em `CONCLUIDA` e que o resultado final reflete a revisão humana.

Registre como evidência:

| Evidência | Valor observado |
|---|---|
| Commit | |
| `correlationId` | |
| `instanceId` | |
| `identificadorDocumento` | |
| `identificadorChecklist` / versão | |
| Estado antes da revisão | `AGUARDANDO_REVISAO` |
| Estado final | `CONCLUIDA` |
| Origem/modelo ou fallback observado | |

As cinco identidades devem permanecer iguais do início ao resultado final. A página consulta o GET
a cada 1,5 segundo e não deve criar dados em `localStorage` ou `sessionStorage`.

## 6. Confirmar o contrato diretamente pela API

Esta etapa é opcional quando a página já foi validada, mas ajuda a separar o backend da interface.
Inicie uma nova análise:

```powershell
$base = "http://localhost:8080/simtr-hub/v1/conformidade/analises"
$solicitacao = [ordered]@{
    identificadorDocumento = "DOC-API-001"
    texto = "Documento inteiramente sintético para validação da API."
    identificadorChecklist = 1000012583
    versaoChecklist = 1
} | ConvertTo-Json

$respostaInicio = Invoke-WebRequest `
    -UseBasicParsing `
    -Method Post `
    -Uri $base `
    -ContentType "application/json" `
    -Body $solicitacao
$inicio = $respostaInicio.Content | ConvertFrom-Json
$respostaInicio.StatusCode
$inicio
```

O POST deve retornar HTTP `202`, header `Location` relativo e as cinco identidades. Consulte até a
revisão ficar disponível:

```powershell
$limite = (Get-Date).AddMinutes(4)
do {
    Start-Sleep -Seconds 2
    $estado = Invoke-RestMethod -Method Get -Uri "$base/$($inicio.instanceId)"
    $estado.status
} while ($estado.status -eq "EM_PROCESSAMENTO" -and (Get-Date) -lt $limite)

if ($estado.status -ne "AGUARDANDO_REVISAO") {
    throw "Estado inesperado antes da revisão: $($estado.status)"
}
```

Monte a revisão copiando a identidade de todos os apontamentos preliminares:

```powershell
$apontamentos = @($estado.resultadoPreliminar.apontamentos | ForEach-Object {
    [ordered]@{
        identificadorApontamento = $_.identificadorApontamento
        nomeApontamento = $_.nomeApontamento
        parecer = $_.parecer
        justificativa = "Revisão humana sintética para $($_.nomeApontamento)."
        evidencia = "Evidência sintética"
        confianca = $_.confianca
    }
})
$revisao = [ordered]@{
    observacao = "Validação manual sintética"
    apontamentos = $apontamentos
} | ConvertTo-Json -Depth 8

$respostaRevisao = Invoke-WebRequest `
    -UseBasicParsing `
    -Method Put `
    -Uri "$base/$($inicio.instanceId)/revisao" `
    -ContentType "application/json" `
    -Body $revisao
$respostaRevisao.StatusCode
```

O PUT deve retornar HTTP `202`. Consulte novamente até a conclusão:

```powershell
$limite = (Get-Date).AddMinutes(2)
do {
    Start-Sleep -Seconds 2
    $final = Invoke-RestMethod -Method Get -Uri "$base/$($inicio.instanceId)"
    $final.status
} while ($final.status -notin @("CONCLUIDA", "FALHOU") -and (Get-Date) -lt $limite)

if ($final.status -ne "CONCLUIDA") {
    throw "Estado final inesperado: $($final.status)"
}

$final | Select-Object correlationId, instanceId, identificadorDocumento, `
    identificadorChecklist, versaoChecklist, status
```

Uma revisão repetida deve produzir HTTP `409`, e uma revisão que altere nome, identidade, confiança
ou omita apontamentos deve produzir HTTP `422`.

## 7. Validar os dados persistidos no CouchDB

### Onde os documentos são armazenados

O CouchDB organiza dados como **databases contendo documentos JSON**; ele não possui schema
relacional, tabelas ou linhas. A [documentação oficial do CouchDB](https://docs.couchdb.org/en/stable/intro/overview.html#document-storage)
explica que cada database nomeado armazena documentos identificados de forma única e que os campos
podem ter tipos JSON variados. Nesta PoC, a flexibilidade do servidor é limitada por um contrato
da aplicação com `tipo` e `versaoSchema`.

| Nível | Compose | Kubernetes kind |
|---|---|---|
| database lógico da aplicação | `${COUCHDB_DATABASE:-conformidade}` | `conformidade`, definido nos manifests |
| processo CouchDB | container `couchdb` | container `couchdb` no StatefulSet `couchdb` |
| diretório dentro do container | `/opt/couchdb/data` | `/opt/couchdb/data` |
| persistência física | volume nomeado `simtr-hub-poc_couchdb-conformidade-data` | PVC `couchdb-conformidade-data` de 1 GiB |
| porta dentro da rede | `couchdb:5984` | Service `couchdb:5984` |
| acesso pelo host | `127.0.0.1:15984` por padrão | port-forward para `127.0.0.1:15984` |

Todos os documentos negociais da análise ficam no database `conformidade`, salvo sobrescrita de
`COUCHDB_DATABASE` no Compose. Bancos internos como `_users` ou `_replicator`, quando exibidos pelo
CouchDB, não contêm o estado negocial da PoC. Os arquivos em `/opt/couchdb/data` pertencem ao
CouchDB e não devem ser abertos ou editados diretamente; consulte-os pela API HTTP, Fauxton ou uma
ferramenta compatível.

O Valkey não armazena esses documentos: ele contém somente checkpoints técnicos do Quarkus Flow.
Texto, checklist, resultado e revisão permanecem no CouchDB.

### Schema lógico aplicado pela PoC

O CouchDB não valida esse schema por conta própria. Quem cria e valida o contrato é
`DocumentoAnaliseConformidadeStore`. A versão implementada é `versaoSchema=1`.

Todo documento negocial possui esta base:

```json
{
  "_id": "<prefixo>-<sha256>",
  "_rev": "<revisão MVCC gerada pelo CouchDB>",
  "id": "<mesmo valor determinístico de _id>",
  "tipo": "<discriminador do documento>",
  "versaoSchema": 1,
  "correlationId": "<UUID da análise>",
  "instanceId": "<ID nativo da instância Flow>",
  "identificadorDocumento": "DOC-VALIDACAO-002",
  "identificadorChecklist": 1000012583,
  "versaoChecklist": 1
}
```

`_id` é a chave única no database e `_rev` é o token de concorrência otimista mantido pelo
CouchDB. O campo `id` é a identidade determinística preservada no contrato da aplicação. A
[API oficial de documentos](https://docs.couchdb.org/en/stable/api/document/common.html#get--db-docid)
define `_id` e `_rev`; `_rev` não representa histórico de negócio.

Os IDs são construídos por prefixo mais SHA-256 da correlação, instância ou evento:

| Tipo | Prefixo do `_id` | Campos específicos principais |
|---|---|---|
| `entrada-analise` | `entrada-` + SHA-256 de `correlationId` | `texto` |
| `projecao-analise` | `projecao-` + SHA-256 de `instanceId` | `status` e referências para os fatos |
| `checklist-analise` | `checklist-` + SHA-256 de `correlationId` | `hashConteudo`, `checklist` |
| `resultado-preliminar` | `resultado-preliminar-` + SHA-256 de `correlationId` | `hashConteudo`, `resultado` |
| `revisao-humana` | `revisao-` + SHA-256 de `correlationId` | `hashConteudo`, `revisao` |
| `resultado-final` | `resultado-final-` + SHA-256 de `correlationId` | `hashConteudo`, `resultado` |
| `falha-analise` | `falha-` + SHA-256 de `correlationId` | `mensagem` sanitizada |
| `emissao-cloud-event` | `emissao-` + SHA-256 de `eventoId` | `eventoId`, `eventoTipo`, `documentoRef`, `hashConteudo` |

A projeção é o único documento negocial mutável e usa `_rev` para compare-and-set. Os demais são
fatos imutáveis: uma repetição com o mesmo ID e conteúdo é idempotente; conteúdo diferente para o
mesmo ID é conflito. O cursor técnico do feed é uma exceção separada, armazenada como documento
local `_local/simtr-flow-revisao-v1`, com `tipo=cursor-feed-revisao`, `versaoSchema=1` e `lastSeq`.

Uma projeção concluída possui esta forma estrutural:

```json
{
  "tipo": "projecao-analise",
  "versaoSchema": 1,
  "correlationId": "<correlationId>",
  "instanceId": "<instanceId>",
  "identificadorDocumento": "DOC-VALIDACAO-002",
  "identificadorChecklist": 1000012583,
  "versaoChecklist": 1,
  "status": "CONCLUIDA",
  "checklistRef": "checklist-<sha256>",
  "resultadoPreliminarRef": "resultado-preliminar-<sha256>",
  "revisaoRef": "revisao-<sha256>",
  "resultadoFinalRef": "resultado-final-<sha256>"
}
```

As referências apontam para documentos separados no mesmo database. Assim, a projeção permanece
pequena enquanto os payloads completos ficam nos fatos correspondentes.

Execute esta validação depois de iniciar uma análise pela página ou pela API. Use os valores reais
de identidade devolvidos pelo POST ou exibidos na página:

```powershell
$CorrelationId = "<correlationId da análise>"
$InstanceId = "<instanceId da análise>"
$IdentificadorDocumento = "DOC-VALIDACAO-002" # substitua pelo valor da análise
[long]$IdentificadorChecklist = 1000012583     # substitua pelo valor da análise
[int]$VersaoChecklist = 1                      # substitua pelo valor da análise
```

Os comandos abaixo usam as credenciais já presentes dentro do container ou pod do CouchDB. Eles
não imprimem usuário ou senha e não exigem copiá-los do arquivo `.env` para o terminal.

> **Segurança:** os documentos podem conter o texto analisado, checklist, resultado e revisão.
> Faça esta inspeção somente com dados sintéticos. Não anexe a saída completa a logs, issues,
> mensagens ou commits.

### Consultar pelo ambiente Docker Compose

Confirme primeiro que o CouchDB está ativo:

```powershell
docker compose --env-file .env.poc -f compose-poc.yml ps couchdb
```

Carregue todos os documentos comuns do database configurado e filtre localmente pela correlação:

```powershell
$CouchDocsJson = docker compose --env-file .env.poc -f compose-poc.yml `
  exec -T couchdb sh -c `
  'curl --fail --silent --show-error --user "$COUCHDB_USER:$COUCHDB_PASSWORD" "http://127.0.0.1:5984/$COUCHDB_DATABASE/_all_docs?include_docs=true"'

if ($LASTEXITCODE -ne 0) {
    throw "Falha ao consultar os documentos do CouchDB"
}

$TodosDocumentos = @(($CouchDocsJson | ConvertFrom-Json).rows.doc)
$DocumentosAnalise = @($TodosDocumentos | Where-Object {
    $_.correlationId -eq $CorrelationId
})

if ($DocumentosAnalise.Count -eq 0) {
    throw "Nenhum documento encontrado para a correlationId informada"
}

$DocumentosAnalise |
  Select-Object _id, tipo, versaoSchema, correlationId, instanceId, status |
  Sort-Object tipo |
  Format-Table -AutoSize
```

### Consultar pelo cluster Kubernetes kind

No cluster criado pelos scripts da PoC, execute a mesma consulta dentro do pod do CouchDB:

```powershell
$CouchDocsJson = kubectl --context kind-simtr-hub-poc --namespace simtr-hub-poc `
  exec statefulset/couchdb -- sh -c `
  'curl --fail --silent --show-error --user "$COUCHDB_USER:$COUCHDB_PASSWORD" "http://127.0.0.1:5984/$COUCHDB_DATABASE/_all_docs?include_docs=true"'

if ($LASTEXITCODE -ne 0) {
    throw "Falha ao consultar os documentos do CouchDB no Kubernetes"
}

$TodosDocumentos = @(($CouchDocsJson | ConvertFrom-Json).rows.doc)
$DocumentosAnalise = @($TodosDocumentos | Where-Object {
    $_.correlationId -eq $CorrelationId
})

if ($DocumentosAnalise.Count -eq 0) {
    throw "Nenhum documento encontrado para a correlationId informada"
}
```

As validações das subseções seguintes funcionam com `$DocumentosAnalise` obtido em qualquer um dos
dois ambientes.

### Validar a projeção e as identidades

A projeção é o documento que representa o estado consultado pela API. Valide que existe exatamente
uma projeção para a instância e que suas identidades não mudaram:

```powershell
$Projecoes = @($DocumentosAnalise | Where-Object {
    $_.tipo -eq "projecao-analise" -and $_.instanceId -eq $InstanceId
})

if ($Projecoes.Count -ne 1) {
    throw "Era esperada exatamente uma projeção para a instanceId informada"
}

$Projecao = $Projecoes[0]
$Projecao | Select-Object _id, _rev, tipo, versaoSchema, correlationId, instanceId, `
    identificadorDocumento, identificadorChecklist, versaoChecklist, status, `
    checklistRef, resultadoPreliminarRef, revisaoRef, resultadoFinalRef

if ($Projecao.correlationId -ne $CorrelationId) {
    throw "A projeção não preservou a correlationId"
}
if ($Projecao.identificadorDocumento -ne $IdentificadorDocumento -or
    [long]$Projecao.identificadorChecklist -ne $IdentificadorChecklist -or
    [int]$Projecao.versaoChecklist -ne $VersaoChecklist) {
    throw "A projeção não preservou as identidades do documento ou checklist"
}

$VersoesInvalidas = @($DocumentosAnalise | Where-Object {
    [int]$_.versaoSchema -ne 1
})
if ($VersoesInvalidas.Count -gt 0) {
    throw "Há documentos com versaoSchema diferente de 1"
}
```

Durante a análise, `status` pode ser `EM_PROCESSAMENTO`. Antes da revisão, deve ser
`AGUARDANDO_REVISAO`; nesse ponto são esperados `checklistRef` e `resultadoPreliminarRef`. Depois da
revisão, deve ser `CONCLUIDA`, com `revisaoRef` e `resultadoFinalRef` também preenchidos. Em falha
terminal, o estado esperado é `FALHOU`.

### Conferir quais documentos foram persistidos

Agrupe os documentos por tipo:

```powershell
$DocumentosAnalise |
  Group-Object tipo |
  Sort-Object Name |
  Select-Object Name, Count |
  Format-Table -AutoSize
```

| Tipo | Conteúdo esperado |
|---|---|
| `entrada-analise` | identidades e texto sintético recebido no POST |
| `projecao-analise` | estado atual e referências para os demais documentos |
| `checklist-analise` | snapshot do checklist e `hashConteudo` |
| `resultado-preliminar` | resultado do agente ou fallback antes da revisão |
| `revisao-humana` | parecer, justificativas, evidências e observação da revisão |
| `resultado-final` | resultado consolidado com origem `REVISAO_HUMANA` |
| `falha-analise` | falha sanitizada, somente quando a análise termina em `FALHOU` |
| `emissao-cloud-event` | referência, hash e metadados do evento, sem payload negocial completo |

A combinação exata depende do estágio da análise. Por exemplo, `revisao-humana` e
`resultado-final` ainda não existem enquanto a instância está `AGUARDANDO_REVISAO`.

Confirme também que cada referência preenchida na projeção resolve exatamente um documento:

```powershell
$Referencias = @(
    $Projecao.checklistRef
    $Projecao.resultadoPreliminarRef
    $Projecao.revisaoRef
    $Projecao.resultadoFinalRef
) | Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_) }

foreach ($Referencia in $Referencias) {
    $Encontrados = @($DocumentosAnalise | Where-Object { $_.id -eq $Referencia })
    if ($Encontrados.Count -ne 1) {
        throw "A referência $Referencia não resolveu exatamente um documento"
    }
}
```

Para inspecionar um tipo específico, ainda usando somente dados sintéticos:

```powershell
$DocumentosAnalise |
  Where-Object tipo -eq "checklist-analise" |
  Select-Object -First 1 |
  ConvertTo-Json -Depth 20
```

Troque `checklist-analise` pelo tipo desejado. O campo `_rev` é a versão MVCC gerenciada pelo
CouchDB; `id` é a identidade determinística gravada pela aplicação e `_id` é a identidade exposta
pelo CouchDB.

### Validar o cursor do feed `_changes`

O cursor é um documento local e, por regra do CouchDB, não aparece em `_all_docs`. Consulte-o
separadamente depois que o feed tiver processado mudanças.

No Compose:

```powershell
$CursorJson = docker compose --env-file .env.poc -f compose-poc.yml `
  exec -T couchdb sh -c `
  'curl --silent --show-error --user "$COUCHDB_USER:$COUCHDB_PASSWORD" "http://127.0.0.1:5984/$COUCHDB_DATABASE/_local/simtr-flow-revisao-v1"'
$Cursor = $CursorJson | ConvertFrom-Json
```

No Kubernetes, substitua somente a coleta:

```powershell
$CursorJson = kubectl --context kind-simtr-hub-poc --namespace simtr-hub-poc `
  exec statefulset/couchdb -- sh -c `
  'curl --silent --show-error --user "$COUCHDB_USER:$COUCHDB_PASSWORD" "http://127.0.0.1:5984/$COUCHDB_DATABASE/_local/simtr-flow-revisao-v1"'
$Cursor = $CursorJson | ConvertFrom-Json
```

Valide o conteúdo:

```powershell
$Cursor | Select-Object _id, _rev, tipo, versaoSchema, lastSeq

if ($Cursor.error -eq "not_found") {
    throw "O cursor ainda não foi criado; aguarde o feed processar uma mudança"
}
if ($Cursor.tipo -ne "cursor-feed-revisao" -or
    [string]::IsNullOrWhiteSpace([string]$Cursor.lastSeq)) {
    throw "Cursor do feed CouchDB inválido"
}
```

Resultado esperado: `_id=_local/simtr-flow-revisao-v1`, `tipo=cursor-feed-revisao`,
`versaoSchema=1` e `lastSeq` preenchido.

### Inspeção opcional pelo Fauxton

No Compose, o Fauxton fica disponível por padrão em `http://127.0.0.1:15984/_utils/`. Se
`COUCHDB_HTTP_PORT` foi alterada, use a porta configurada. Entre com o usuário e a senha mantidos
localmente em `.env.poc`; não registre a credencial em capturas.

No Kubernetes, abra o encaminhamento em outro terminal:

```powershell
kubectl --context kind-simtr-hub-poc --namespace simtr-hub-poc `
  port-forward service/couchdb 15984:5984
```

Enquanto o comando estiver ativo, acesse `http://127.0.0.1:15984/_utils/` e use as credenciais de
`.env.poc-kubernetes`. Encerre o port-forward com `Ctrl+C` depois da inspeção.

### Posso usar DBeaver?

Sim, **desde que a edição ou distribuição instalada disponibilize o driver `CouchDB`**. O
[catálogo oficial do DBeaver](https://dbeaver.com/databases/) lista CouchDB entre os bancos
orientados a documentos, e a [comparação oficial de edições](https://dbeaver.com/edition/) inclui
suporte NoSQL/BigData nos produtos PRO. A documentação genérica de drivers não garante que toda
instalação Community contenha o conector; confirme se `CouchDB` aparece no assistente descrito em
[Create connection](https://dbeaver.com/docs/dbeaver/Create-Connection/).

Essa recomendação condicionada é uma inferência das fontes oficiais: o catálogo confirma CouchDB,
mas não existe na documentação pública consultada uma página atual de configuração específica do
driver. Por isso, os nomes dos campos na interface podem variar conforme produto e versão; os
valores abaixo vêm das portas e configurações efetivas desta PoC.

Não configure CouchDB como PostgreSQL, banco SQL genérico ou arquivo local. Ele é acessado pela API
HTTP e seus objetos são documentos JSON, não tabelas. Se `CouchDB` não aparecer no assistente,
essa instalação não fornece o driver necessário; use uma edição/distribuição compatível ou o
Fauxton e os comandos HTTP deste guia.

Para o Compose:

| Campo da conexão | Valor |
|---|---|
| Driver/tipo | `CouchDB` |
| Host | `127.0.0.1` |
| Porta | `15984`, ou `COUCHDB_HTTP_PORT` configurada |
| Database | `conformidade`, ou `COUCHDB_DATABASE` configurado |
| Usuário/senha | valores locais de `.env.poc` |
| TLS/SSL | desabilitado para esta PoC HTTP local |

Para o kind, mantenha este comando ativo em outro terminal:

```powershell
kubectl --context kind-simtr-hub-poc --namespace simtr-hub-poc `
  port-forward service/couchdb 15984:5984
```

No DBeaver, use `127.0.0.1`, porta `15984`, database `conformidade` e as credenciais de
`.env.poc-kubernetes`. Use **Test Connection** antes de abrir o database. Não salve senha em
arquivo versionado, não exporte a configuração com credenciais e não edite manualmente a projeção,
os fatos ou o cursor durante uma prova.

O DBeaver é uma opção de inspeção visual, não a evidência canônica desta PoC. Os comandos
PowerShell anteriores e o Fauxton continuam sendo o roteiro reproduzível porque usam diretamente
a API do CouchDB e independem de edição ou driver externo.

## 8. Provar recuperação depois do restart

### Prova manual no Compose

1. Inicie uma análise e espere `AGUARDANDO_REVISAO`.
2. Registre `instanceId` e `correlationId`.
3. Reinicie somente a aplicação:

```powershell
docker compose --env-file .env.poc -f compose-poc.yml restart simtr-hub
```

4. Aguarde a readiness voltar a `UP`.
5. Consulte a mesma `instanceId`; ela deve continuar aguardando revisão.
6. Envie a revisão e confirme `CONCLUIDA` com as mesmas identidades.

Não recrie CouchDB nem Valkey durante essa prova. A evidência comprova recuperação da aplicação,
não recuperação dos backends.

### Prova automatizada entre duas JVMs

```powershell
./validar-restart-conformidade.ps1
```

Resultado esperado:

```text
Prova de restart entre JVMs concluída com sucesso
```

O script inicia CouchDB e Valkey efêmeros, cria uma instância `WAITING` na primeira JVM, restaura e
conclui a mesma instância na segunda JVM e remove seus containers ao terminar.

## 9. Provar duas réplicas e failover

Prepare o Secret local:

```powershell
Copy-Item poc-kubernetes.env.example .env.poc-kubernetes
```

Preencha os três valores obrigatórios somente no arquivo local e execute:

```powershell
./validar-poc-kubernetes.ps1
./validar-failover-poc-kubernetes.ps1
```

O primeiro roteiro deve terminar com:

```text
GREEN: duas replicas adquiriram Leases estaveis, readiness exigiu Lease e o rollout preservou identidades.
```

O segundo deve terminar com:

```text
GREEN: cross-pod e failover preservaram Lease, dados e checkpoint; cada instancia concluiu uma unica vez.
```

Inspecione o estado deixado pelos roteiros:

```powershell
kubectl --context kind-simtr-hub-poc --namespace simtr-hub-poc get pods
kubectl --context kind-simtr-hub-poc --namespace simtr-hub-poc get leases
kubectl --context kind-simtr-hub-poc --namespace simtr-hub-poc get pvc
```

São esperadas duas réplicas Ready, duas Leases de membro associadas a pods Ready e o PVC do
CouchDB. Os scripts deixam o cluster ativo para inspeção. Quando os dados não forem mais
necessários, a remoção explícita é:

```powershell
kind delete cluster --name simtr-hub-poc
```

Esse comando elimina o cluster, o PVC e todos os dados locais nele contidos.

## 10. Verificar Ollama e observabilidade

A demonstração pelo Compose já usa o Ollama do host. Para provar isoladamente a saída estruturada
do adapter real, use somente dados sintéticos:

```powershell
$env:QUARKUS_LANGCHAIN4J_OLLAMA_BASE_URL = "http://localhost:11434/"
$env:QUARKUS_LANGCHAIN4J_OLLAMA_ENABLE_INTEGRATION = "true"
mvn -q `
    "-Dollama.integration=true" `
    "-Dtest=OllamaAnaliseConformidadeRealQuarkusTest" `
    test
```

O teste deve ser executado, não ignorado, e terminar com código 0. Remova as variáveis temporárias
depois da prova:

```powershell
Remove-Item Env:QUARKUS_LANGCHAIN4J_OLLAMA_BASE_URL
Remove-Item Env:QUARKUS_LANGCHAIN4J_OLLAMA_ENABLE_INTEGRATION
```

Para inspecionar os logs JSON da execução em containers:

```powershell
docker compose --env-file .env.poc -f compose-poc.yml logs --since 10m simtr-hub
```

Eventos esperados incluem `conformidade.agente.chamada.iniciada`,
`conformidade.agente.chamada.concluida`, `conformidade.persistencia.operacao.concluida` e
`conformidade.feed.operacao.concluida`. Os sinais próprios podem conter IDs, backend, operação,
resultado, replay, cursor/lease e trace. Eles não devem conter texto, checklist, revisão,
evidência, credencial ou documento completo. Os testes contratuais de observabilidade são a prova
automatizada dessa ausência.

O exporter OpenTelemetry padrão é `none`. Para inspeção externa, use somente os profiles opcionais
`dev,jaeger` ou `dev,grafana` e um collector local já preparado, conforme o
[catálogo de observabilidade](../arquitetura-distribuida/catalogo-observabilidade.md).

## 11. Executar o checkpoint de qualidade

Esta etapa exige uma sessão que já possua o token Sonar somente em memória e um baseline válido no
estado `READY`. Não recrie o baseline apenas para repetir a verificação e nunca informe o token em
chat, arquivo ou argumento.

```powershell
./validar-checkpoint-sonarqube.ps1
```

Para considerar o incremento conforme, o script exige:

- nenhuma issue nova;
- nenhuma issue `HIGH`, `BLOCKER` ou `CRITICAL`;
- cobertura mínima de 85%;
- duplicação máxima de 5%.

O checkpoint final registrado para a Task 9.4 foi `COMPLIANT`, com 240 issues contra 240 no
baseline, nenhuma nova ou severa, cobertura de 85,3% e duplicação de 2,6%. Uma nova execução deve
ser avaliada pelo resultado que ela produzir; o registro histórico não substitui a análise atual.

Se a sessão não possuir baseline ou acesso ao Sonar local, registre a indisponibilidade. Não trate
a ausência de execução como aprovação nem inicialize outro baseline por suposição.

## 12. Gate Cosmos antes de PRD

A suíte padrão prova o contrato do adapter Cosmos com SDK mockado. A integração real é separada e
usa `DefaultAzureCredential`. Ela só deve ser executada contra Emulator ou conta não produtiva
preparada e autorizada.

Defina no ambiente, sem registrar os valores:

```powershell
$env:COSMOS_INTEGRATION_ENABLED = "true"
$env:COSMOS_ENDPOINT = "<endpoint autorizado>"
$env:COSMOS_DATABASE = "<database de teste>"
$env:COSMOS_CONTAINER = "<container de teste>"
mvn -q "-Dtest=CosmosDbAnaliseConformidadeStoreOptInTest" test
```

Resultado esperado: um teste executado, nenhum ignorado e código 0. O teste cria dados sintéticos,
valida a sequência documental e remove os documentos criados. Falta de identidade/RBAC, endpoint
ou containers deve reprovar o gate, não ser interpretada como sucesso.

Este gate ainda não consta como comprovado na evidência final atual da PoC.

## 13. Encerrar a execução local

### Onde cada tipo de estado é mantido

| Backend | Conteúdo na PoC | Compose | Kubernetes local |
|---|---|---|---|
| CouchDB | documentos de entrada, checklist, resultados, revisão, projeção e cursor do feed | volume nomeado `simtr-hub-poc_couchdb-conformidade-data` | PVC `couchdb-conformidade-data` |
| Redis/Valkey | checkpoints técnicos do Quarkus Flow | somente memória, com persistência desabilitada por `--save ""` | somente memória, sem volume ou PVC |

O CouchDB sobrevive ao restart da aplicação e à recriação de seu container enquanto o volume/PVC
e as credenciais que inicializaram esse estado forem preservados. Alterar apenas o Secret ou o
arquivo `.env` não rotaciona a credencial já persistida no CouchDB. O Valkey só mantém checkpoints
enquanto o mesmo serviço continua em execução: reiniciar ou recriar seu container/pod perde todo o
conteúdo. Portanto, não existe nesta PoC um
encerramento completo que preserve checkpoints do Valkey para uma sessão futura.

### Preservar CouchDB e Valkey durante o teste de restart

Reinicie somente a aplicação e mantenha os dois backends continuamente disponíveis.

No Compose:

```powershell
docker compose --env-file .env.poc -f compose-poc.yml restart simtr-hub
```

No Kubernetes:

```powershell
kubectl --context kind-simtr-hub-poc --namespace simtr-hub-poc `
  rollout restart deployment/simtr-hub
kubectl --context kind-simtr-hub-poc --namespace simtr-hub-poc `
  rollout status deployment/simtr-hub --timeout=300s
```

Esses comandos preservam documentos e checkpoints e são o caminho correto para provar retomada de
uma instância `AGUARDANDO_REVISAO`. Não reinicie CouchDB nem Valkey durante essa prova.

### Encerrar o Compose preservando apenas os documentos do CouchDB

Para remover containers e rede, mas preservar o volume documental:

```powershell
docker compose --env-file .env.poc -f compose-poc.yml down
```

Ao executar `down`, o container Valkey é removido e seus checkpoints são perdidos. Na próxima
subida, os documentos e projeções concluídas continuam no CouchDB, mas uma instância em andamento
não deve ser considerada retomável sem o checkpoint correspondente. O cluster kind permanece
ativo até ser removido explicitamente; enquanto estiver ativo e seus backends não reiniciarem,
CouchDB e Valkey continuam disponíveis.

### Limpar somente os checkpoints do Valkey

Faça essa limpeza apenas quando nenhuma instância em andamento precisar ser retomada. No Compose,
pare a aplicação, reinicie o Valkey sem persistência e inicie novamente a aplicação:

```powershell
docker compose --env-file .env.poc -f compose-poc.yml stop simtr-hub
docker compose --env-file .env.poc -f compose-poc.yml restart valkey
docker compose --env-file .env.poc -f compose-poc.yml start simtr-hub
```

No Kubernetes:

```powershell
kubectl --context kind-simtr-hub-poc --namespace simtr-hub-poc `
  rollout restart deployment/valkey
kubectl --context kind-simtr-hub-poc --namespace simtr-hub-poc `
  rollout status deployment/valkey --timeout=180s
```

Nos dois casos, o CouchDB e seus documentos permanecem. Limpar somente CouchDB e manter Valkey não
é um procedimento operacional recomendado: os checkpoints podem referenciar documentos que já
não existem. Para começar uma prova coerente do zero, limpe os dois backends.

### Limpeza completa dos ambientes da PoC

Depois de concluir os testes, siga **todos os passos desta seção** quando não precisar preservar
nenhum dado da PoC.

> **Atenção:** Docker Compose e Kubernetes kind são ambientes independentes. O comando
> `docker compose ... down` remove somente o ambiente Compose. Ele **não remove** o cluster kind
> nem o container Docker `simtr-hub-poc-control-plane`. Da mesma forma, excluir o cluster kind não
> remove o volume CouchDB criado pelo Compose. A remoção total exige limpar os dois ambientes.

| Comando | Remove | Não remove |
|---|---|---|
| `docker compose ... down` | containers e rede do Compose | volume CouchDB, cluster kind e `simtr-hub-poc-control-plane` |
| `docker compose ... down --volumes` | containers, rede e volume CouchDB do Compose | cluster kind e `simtr-hub-poc-control-plane` |
| `kind delete cluster --name simtr-hub-poc` | cluster, `control-plane`, pods, Leases, PVC e dados Kubernetes | recursos e volume do Compose |

#### Passo 1 — remover o ambiente Docker Compose e seu volume CouchDB

Mesmo que um `docker compose ... down` sem `--volumes` já tenha sido executado, execute o comando
abaixo para remover também o volume documental preservado:

```powershell
docker compose --env-file .env.poc -f compose-poc.yml `
  down --volumes --remove-orphans
```

Esse comando não remove o cluster kind. Se `simtr-hub-poc-control-plane` ainda aparecer no Docker
Desktop, prossiga para o passo 2; isso é esperado.

#### Passo 2 — remover o cluster Kubernetes kind e o `control-plane`

Use uma das duas formas abaixo, conforme a instalação disponível:

```powershell
# Quando kind estiver no PATH:
kind delete cluster --name simtr-hub-poc

# Quando o projeto estiver usando a cópia em .tools:
.\.tools\kind.exe delete cluster --name simtr-hub-poc
```

Execute somente uma das formas. Esse passo remove o container
`simtr-hub-poc-control-plane`, todos os pods, Leases, PVC e dados do CouchDB Kubernetes.

#### Passo 3 — remover a imagem local e, opcionalmente, os artefatos Maven

```powershell
docker image rm simtr-hub-poc:local

# Opcional:
mvn clean
```

Se `SIMTR_HUB_IMAGE_TAG` tiver sido sobrescrita, substitua `local` pela tag utilizada. Os comandos
dos passos 1 a 3 são limitados à PoC e preservam `.env.poc`, `.env.poc-kubernetes`, o Ollama
instalado no host e recursos alheios, como SonarQube ou Jaeger. Não use
`docker system prune --all --volumes` como limpeza normal da PoC: ele atua globalmente e pode
remover dados, imagens e caches de outros projetos.

#### Passo 4 — confirmar a remoção total

Execute todas as consultas:

```powershell
docker ps -a --filter "label=com.docker.compose.project=simtr-hub-poc"
docker volume ls --filter "label=com.docker.compose.project=simtr-hub-poc"
docker images simtr-hub-poc
kind get clusters
# Se kind não estiver no PATH, use: .\.tools\kind.exe get clusters
```

As duas primeiras consultas e `docker images` devem exibir somente seus cabeçalhos, sem recursos da
PoC. A listagem do kind não deve conter `simtr-hub-poc`, e o Docker Desktop não deve mais mostrar
`simtr-hub-poc-control-plane`.

Para reconstruir uma réplica no Compose a partir do ambiente limpo:

```powershell
mvn -q -DskipTests package
docker compose --env-file .env.poc -f compose-poc.yml up --build --detach
```

Para reconstruir e validar o ambiente Kubernetes completo:

```powershell
./validar-poc-kubernetes.ps1
./validar-failover-poc-kubernetes.ps1
```

O primeiro script recria o cluster quando ausente. O segundo completa a prova de revisão cross-pod
e failover; execute-o quando esse nível de evidência fizer parte do objetivo da validação.

Confirme o estado final do repositório:

```powershell
git status -sb
```

Não inclua `.env.poc`, `.env.poc-kubernetes`, logs locais, relatórios `target/` ou credenciais no
commit de evidência.

## Checklist de aceite da verificação

| Verificação | Obrigatória para validar a PoC local | Resultado |
|---|---:|---|
| Commit e worktree identificados | Sim | |
| `mvn -q clean test` | Sim | |
| `mvn -q verify` | Sim | |
| Readiness local `UP` | Sim | |
| Fluxo pela página até `CONCLUIDA` | Sim | |
| Cinco identidades preservadas | Sim | |
| Documentos CouchDB correlacionados e projeção válida | Sim | |
| Ollama real ou fallback explicitamente identificado | Sim | |
| Restart da aplicação com retomada | Para validar durabilidade | |
| Duas réplicas, Leases e failover | Para validar multipod | |
| Logs e sinais sem conteúdo sensível | Sim | |
| Checkpoint Sonar atual | Sim quando o ambiente estiver disponível | |
| Cosmos real/emulator | Obrigatório somente antes de PRD | `PENDENTE` |

A PoC pode ser considerada **verificada localmente** quando as linhas obrigatórias locais estiverem
aprovadas e as limitações forem registradas. Isso não autoriza promoção para PRD nem muda o status
do ADR-0010: ambas dependem dos respectivos gates e de decisão humana explícita.

## Diagnóstico rápido

| Sintoma | Verificação |
|---|---|
| `ollama-check` falha | Execute `ollama list`; confirme `OLLAMA_MODEL` e acesso à porta 11434 do host. |
| `simtr-hub` não fica healthy | Consulte `/q/health/ready` e os logs; identifique se falhou CouchDB ou Valkey. |
| Análise demora | Aguarde o limite de FT; três tentativas podem se aproximar de 181 s antes do fallback. |
| PUT retorna 409 | A instância não aguarda mais revisão ou a revisão já foi aceita. |
| PUT retorna 422 | Reenvie todos os apontamentos sem alterar identificador, nome ou confiança. |
| Estado some após restart | Confirme que apenas a aplicação reiniciou e CouchDB/Valkey permaneceram ativos. |
| Consulta CouchDB não retorna documentos | Confirme o ambiente consultado, a `correlationId`, o database configurado e se o CouchDB está ativo. |
| Pod não fica Ready | Inspecione `Lease Acquisition`, RBAC, Leases e health do backend documental/Valkey. |
| Sonar não executa | Confirme a sessão segura e o baseline `READY`; não crie baseline novo por suposição. |

## Fontes versionadas

- [README da PoC](../../README.md#poc-de-conformidade-durável)
- [Arquitetura consolidada](../arquitetura-distribuida/arquitetura-ddd-integracoes-atomicas.md)
- [ADR-0010](../adr/0010-conformidade-couchdb-checkpoint-redis-e-hitl-multipod.md)
- [Especificação da PoC](../poc/especificacao-poc-conformidade-quarkus-flow-ollama-hitl-sem-broker.md)
- [Catálogo de observabilidade](../arquitetura-distribuida/catalogo-observabilidade.md)
- [Compose local](../../compose-poc.yml)
- [Script de restart](../../validar-restart-conformidade.ps1)
- [Script de Kubernetes](../../validar-poc-kubernetes.ps1)
- [Script de failover](../../validar-failover-poc-kubernetes.ps1)
