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

## 7. Provar recuperação depois do restart

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

## 8. Provar duas réplicas e failover

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

## 9. Verificar Ollama e observabilidade

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
[catálogo de observabilidade](../catalogo-observabilidade.md).

## 10. Executar o checkpoint de qualidade

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

## 11. Gate Cosmos antes de PRD

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

## 12. Encerrar a execução local

Para encerrar o Compose preservando o volume documental:

```powershell
docker compose --env-file .env.poc -f compose-poc.yml down
```

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
| Pod não fica Ready | Inspecione `Lease Acquisition`, RBAC, Leases e health do backend documental/Valkey. |
| Sonar não executa | Confirme a sessão segura e o baseline `READY`; não crie baseline novo por suposição. |

## Fontes versionadas

- [README da PoC](../../README.md#poc-de-conformidade-durável)
- [Arquitetura consolidada](../arquitetura-ddd-integracoes-atomicas.md)
- [ADR-0010](../adr/0010-conformidade-couchdb-checkpoint-redis-e-hitl-multipod.md)
- [Especificação da PoC](especificacao-poc-conformidade-quarkus-flow-ollama-hitl-sem-broker.md)
- [Catálogo de observabilidade](../catalogo-observabilidade.md)
- [Compose local](../../compose-poc.yml)
- [Script de restart](../../validar-restart-conformidade.ps1)
- [Script de Kubernetes](../../validar-poc-kubernetes.ps1)
- [Script de failover](../../validar-failover-poc-kubernetes.ps1)
