# Plano: PoC de conformidade com Quarkus Flow, Ollama e Human-in-the-Loop

## Intenção

Validar, dentro do domínio `conformidade` do `simtr-hub`, uma análise documental
orquestrada por Quarkus Flow que reutiliza o checklist existente, usa uma capacidade
agentic do Quarkus LangChain4j com Ollama local, pausa de fato para revisão humana e
retoma por CloudEvent sem broker. A evolução aprovada para planejamento separa dados
de negócio JSON, acessados por uma porta documental neutra, de checkpoints técnicos
do Flow no Redis/Valkey. CouchDB será usado em DES e Azure Cosmos DB for NoSQL em
PRD; a evolução deve provar equivalência contratual, reinício e roteamento entre pods
antes de declarar suporte multipod.

O resultado esperado é uma PoC demonstrável por API e página estática, com limites e
riscos explícitos. Este plano não autoriza implementação: o primeiro item de produção
continua condicionado ao GO humano e aos checkpoints adicionais.

## Escopo

- manter Java 25 e Quarkus `3.33.2.1`, salvo incompatibilidade demonstrada e nova
  decisão humana;
- criar a PoC no domínio `br.gov.caixa.simtr.hub.conformidade`;
- reutilizar a porta de entrada `ConsultarChecklist` e seus modelos de domínio, sem
  chamada REST local e sem acesso direto ao cliente ou DTO do MTR;
- importar os BOMs companion de Quarkus Flow e Quarkus LangChain4j compatíveis com a
  plataforma atual;
- usar Quarkus Flow como orquestrador da camada de aplicação;
- usar LangChain4j Agentic preferencialmente como sequência
  `AplicadorChecklistAgent -> RevisorCoberturaAgent`, exposta como uma única
  capacidade ao workflow;
- usar `quarkus-langchain4j-ollama` com o modelo local `llama3.2:3b`;
- gerar saída tipada, validar e normalizar o resultado em Java;
- aplicar SmallRye Fault Tolerance somente no adaptador de IA;
- implementar pausa real `emitJson(...) -> listen(...)`;
- correlacionar análise, revisão e Flow por `correlationId`, `instanceId`,
  `identificadorDocumento`, `identificadorChecklist` e `versaoChecklist`;
- manter CloudEvent v1 e `listen(...)`, sem Kafka, usando o feed nativo do backend:
  `_changes` do CouchDB em DES e Change Feed Processor do Cosmos em PRD;
- expor a persistência por portas neutras e usar CouchDB em DES e Azure Cosmos DB
  for NoSQL em PRD como sistemas de registro dos dados de negócio;
- aplicar o mesmo contrato executável aos dois adapters, usando CouchDB real em
  DES e repositório determinístico + SDK mockado para Cosmos na validação local;
- manter a integração contra Cosmos real ou Emulator como gate externo antes da
  promoção para PRD, sem bloquear a conclusão das tasks locais;
- usar Quarkus reativo sempre que a API permitir: porta documental com
  `Uni<Void>` nas escritas e
  `Uni<Optional<VisaoAnaliseConformidade>>` nas leituras, portas de entrada e REST
  com `Uni`, projeção interna com `Uni<Void>` e callbacks Flow retornando `Uni`
  diretamente;
- iniciar automaticamente o CouchDB pelo Compose Dev Services no
  `mvn quarkus:dev`, com volume nomeado preservado entre reinícios do Quarkus;
- autenticar o Cosmos em PRD por Microsoft Entra ID com Managed Identity ou
  Workload Identity e RBAC de plano de dados com menor privilégio;
- usar `quarkus-flow-redis` com Redis/Valkey somente para checkpoints técnicos;
- limitar o contexto persistido do Flow a IDs, referências e hashes;
- tratar a entrega como pelo menos uma vez, com IDs determinísticos, concorrência
  otimista nativa (`_rev` ou `_etag`) e idempotência no workflow;
- expor os endpoints POST, GET e PUT especificados;
- fornecer página HTML/CSS/JavaScript estática com polling de 1.500 ms e todas as
  identidades visíveis como somente leitura;
- fornecer containers de DES para aplicação, CouchDB, Redis/Valkey e Ollama; a
  validação Cosmos real permanece separada e opt-in antes da promoção para PRD;
- validar duas réplicas em Kubernetes com identidade durável por Leases;
- ampliar contratos de arquitetura, API, logs e spans;
- documentar execução local, restart, replay, limitações e resultados da PoC.

## Fora de escopo

- Kafka, AMQP, broker externo ou qualquer `mp.messaging.*.connector`;
- WebSocket, Server-Sent Events ou push para a página;
- PostgreSQL, JPA, MongoDB, banco relacional, MVStore ou dados de negócio no
  Redis/Valkey;
- tratar MongoDB como emulador ou protocolo do Azure Cosmos DB for NoSQL;
- alta disponibilidade produtiva, backup corporativo ou retenção regulatória;
- perda, reinicialização, alta disponibilidade ou recuperação do Redis/Valkey;
- autenticação ou autorização nova para a API ou a página;
- chave ou connection string do Cosmos em PRD;
- Cosmos Emulator no caminho normal de DES ou nos pods locais;
- exigir Cosmos Emulator ou conta Cosmos real para concluir as tasks locais; essa
  integração permanece um gate externo de pré-promoção para PRD;
- alteração dos contratos existentes de consulta de checklist ou MTR;
- DTO compartilhado entre REST, MTR, mensageria e IA;
- retry adicional ao redor de `ConsultarChecklist`;
- nova inferência após a revisão humana;
- alteração automática de Quarkus, Java, LangChain4j ou Flow para a versão do projeto
  de referência;
- logging integral de prompt/resposta fora de perfil local controlado;
- teste obrigatório dependente de um Ollama real na suíte padrão;
- declarar suporte multipod sem a prova cross-pod/failover;
- formatos derivados `.ppt`, `.pptx`, `.pdf` ou `.html` de documentação.

## Contexto verificado

- arquitetura consolidada lida:
  `doc/arquitetura-ddd-integracoes-atomicas.md`;
- índice de ADRs lido: `doc/adr/README.md`;
- ADRs aplicáveis lidos integralmente:
  - ADR-0001 — monólito modular e arquitetura hexagonal;
  - ADR-0002 — ownership por domínio;
  - ADR-0003 — orquestração local por portas de aplicação;
  - ADR-0004 — contratos independentes por borda;
  - ADR-0005 — DTO MTR e Fault Tolerance nos adaptadores;
  - ADR-0006 — evolução compatível de contrato, configuração e observabilidade;
  - ADR-0008 — limites de exposição de dados a capacidades agentic;
- ADR-0007 foi classificado como não aplicável: a PoC consulta checklist e produz
  estado próprio, sem operação mutável no MTR;
- ADR-0009 registra a arquitetura em memória comprovada nos incrementos 1 a 6;
- ADR-0010 foi criado como `Proposto` para a evolução documental + Redis/Valkey +
  feeds nativos + Kubernetes; C4 autorizou a base e C5 autorizou CouchDB em DES,
  Cosmos DB for NoSQL em PRD, a porta neutra e o gate Cosmos;
- especificação integral lida:
  `doc/poc/especificacao-poc-conformidade-quarkus-flow-ollama-hitl-sem-broker.md`;
- templates e regras de tarefas lidos:
  `tasks/README.md`, `tasks/templates/plan.md` e `tasks/templates/todo.md`;
- código diretamente relacionado inspecionado:
  - `pom.xml`;
  - `src/main/resources/application.properties`;
  - `src/test/resources/application.properties`;
  - `src/main/java/br/gov/caixa/simtr/hub/conformidade/aplicacao/porta/entrada/ConsultarChecklist.java`;
  - `src/main/java/br/gov/caixa/simtr/hub/conformidade/aplicacao/casodeuso/ConsultarChecklistCasoDeUso.java`;
  - modelos `Checklist`, `ApontamentoChecklist` e `ComandoConsultaChecklist`;
  - `ChecklistResource`, `ChecklistRestMapper`, `ChecklistPortasProducer`,
    `ChecklistObservabilidade` e `ParametrizacaoChecklistClient`;
  - contrato público de erro `ErroPadraoDto`/`ErroMensagemDto` e exception mappers;
- testes diretamente relacionados inspecionados:
  - `ChecklistApiContractTest`;
  - `ChecklistResourceQuarkusTest`;
  - `ChecklistPortasProducerTest`;
  - `ChecklistSelecaoSimuladorQuarkusTest`;
  - `ArchUnitProgressivoTest`;
  - `ObservabilidadeLogsContratoTest`;
  - `ObservabilidadeSpansContratoTest`.

### Estado atual relevante

- o projeto usa o BOM principal do Quarkus `3.33.2.1` e Java 25;
- o POM ainda não importa BOMs de Flow ou LangChain4j e não possui dependências de
  Flow, Agentic, Ollama ou Messaging;
- `ConsultarChecklist` retorna `Uni<Checklist>` e já isola seleção entre MTR e
  simulador;
- a política de resiliência da consulta de checklist já existe no cliente MTR e não
  deve ser duplicada;
- não existe workflow, agente, canal interno, store de análise ou recurso estático;
- o domínio e a aplicação não dependem de adaptadores, e os testes ArchUnit
  fiscalizam essa direção;
- DTOs são confinados às bordas; somente os DTOs de erro REST são compartilhados
  dentro da borda REST;
- logs e spans existentes são contratos testados e não carregam payloads completos;
- a aplicação já usa Mutiny, OpenTelemetry, logs JSON, Bean Validation e SmallRye
  Fault Tolerance.

### Incompatibilidades, divergências e premissas

1. O projeto conceitual de referência usa Quarkus mais novo. Seu POM e sua sintaxe
   Flow são somente referência; não serão copiados sem validação.
2. Não existe `io.quarkus.platform:quarkus-flow-bom:3.33.2.1` no Maven Central.
   Portanto, não é possível repetir literalmente os três BOMs de plataforma usados
   pelo `newsletter-drafter` sem misturar versões de plataforma.
3. O spike confirmou a combinação explícita Flow `0.10.2` e Quarkus LangChain4j
   `1.11.2`. Ela é a mesma combinação resolvida pelos BOMs do projeto de referência,
   e ambas as extensões foram construídas sobre Quarkus `3.33.2`.
4. A configuração inicial da seção 21 da especificação usa chaves diferentes das
   fornecidas depois pelo usuário. A decisão vigente para a PoC é:

   ```properties
   quarkus.langchain4j.ollama.base-url=http://localhost:11434/
   quarkus.langchain4j.ollama.chat-model.model-id=llama3.2:3b
   quarkus.langchain4j.ollama.chat-model.temperature=0.2
   quarkus.langchain4j.ollama.chat-model.model-options.num-ctx=2048
   quarkus.langchain4j.ollama.timeout=60s
   ```

   O perfil local controlado poderá habilitar logs de request/response e categorias
   DEBUG. Outros perfis devem mantê-los desabilitados.
5. A ponte de `Uni<Checklist>` para a API do Flow será assíncrona, preferencialmente
   por `subscribeAsCompletionStage()` ou API equivalente da versão selecionada.
   `await().indefinitely()` e bloqueio de event loop não são aceitos.
6. A confiança será exibida como somente leitura. A revisão poderá alterar apenas
   parecer, justificativa e evidência; identificadores, nomes e confiança deverão ser
   preservados.
7. O fallback técnico seguirá para revisão humana. `503` fica reservado a falha
   técnica anterior à criação da espera quando nem mesmo um fallback coerente puder
   ser construído.
8. Como Flow, canais internos e estado volátil são decisões arquiteturais materiais,
   a implementação deverá propor um ADR antes de consolidá-los como estado aceito.
9. Não existe diretório `sonar/`. O baseline no SonarQube Docker local foi
   inicializado em 2026-07-24 antes do spike.
10. Flow `0.10.2` só registra sua ponte padrão de Messaging quando encontra
    `mp.messaging.incoming.flow-in.connector` e
    `mp.messaging.outgoing.flow-out.connector`. O bootstrap sem connector confirmou
    os canais desconectados. O spike registrou as classes da ponte como beans da
    aplicação e então iniciou sem warnings de topologia. Esse shim limitado à versão
    precisa de aprovação em C1.

## Evidências técnicas de versão

- a documentação oficial de integração Flow–LangChain4j lista
  `quarkus-flow-langchain4j`, `quarkus-langchain4j-agentic` e o provider Ollama, além
  das construções de agente e HITL:
  <https://docs.quarkiverse.io/quarkus-flow/dev/langchain4j.html>;
- a documentação oficial de Messaging confirma que uma cadeia completa da aplicação
  pode funcionar internamente sem connector:
  <https://quarkus.io/guides/messaging>;
- a referência oficial do Flow para Messaging define `flow-in`, `flow-out`,
  `flowinstanceid` e também documenta que a ponte padrão é ativada por connector:
  <https://docs.quarkiverse.io/quarkus-flow/dev/messaging.html>;
- a referência oficial do provider Ollama confirma a extensão e as propriedades
  geradas de configuração:
  <https://docs.quarkiverse.io/quarkus-langchain4j/dev/ollama-chat-model.html> e
  <https://docs.quarkiverse.io/quarkus-langchain4j/dev/includes/quarkus-all-config.html>;
- a referência oficial do Azure Cosmos DB Java SDK v4 confirma um único artefato
  Maven com APIs síncrona e assíncrona:
  <https://learn.microsoft.com/en-us/azure/cosmos-db/sdk-java-v4>;
- a API Java estável registra `com.azure:azure-cosmos:4.80.0` e
  `CosmosItemRequestOptions.setIfMatchETag(String)`:
  <https://learn.microsoft.com/en-us/java/api/com.azure.cosmos.models.cosmositemrequestoptions>;
- a documentação de concorrência confirma `_etag`, `If-Match` e resposta `412` para
  versão obsoleta:
  <https://learn.microsoft.com/en-us/azure/cosmos-db/database-transactions-optimistic-concurrency>;
- o Change Feed Processor do Java SDK v4 possui entrega pelo menos uma vez e
  coordenação por container de leases:
  <https://learn.microsoft.com/en-us/azure/cosmos-db/change-feed-processor>;
- o Cosmos DB Emulator é adequado a desenvolvimento, mas não reproduz todas as
  características do serviço:
  <https://learn.microsoft.com/en-us/azure/cosmos-db/emulator>;
- o relatório executável do spike está em
  `tasks/features/poc-conformidade-flow-ollama-hitl/compatibilidade.md`.

## Decisão de dependências e versões

### Estratégia preferencial

Manter:

```xml
<quarkus.platform.version>3.33.2.1</quarkus.platform.version>
<maven.compiler.release>25</maven.compiler.release>
```

Importar em `dependencyManagement`:

```text
io.quarkus.platform:quarkus-bom:3.33.2.1
io.quarkiverse.langchain4j:quarkus-langchain4j-bom:1.11.2
io.quarkiverse.flow:quarkus-flow-bom:0.10.2
```

As versões explícitas dos dois BOMs de extensão são necessárias porque o BOM Flow da
plataforma `3.33.2.1` não existe. Declarar as dependências sem versão individual:

```text
io.quarkiverse.flow:quarkus-flow
io.quarkiverse.flow:quarkus-flow-langchain4j
io.quarkiverse.flow:quarkus-flow-messaging
io.quarkiverse.flow:quarkus-flow-redis
io.quarkiverse.flow:quarkus-flow-durable-kubernetes
io.quarkiverse.langchain4j:quarkus-langchain4j-agentic
io.quarkiverse.langchain4j:quarkus-langchain4j-ollama
io.quarkus:quarkus-messaging
com.azure:azure-cosmos:4.80.0
```

`quarkus-langchain4j-agentic` e as extensões Flow usadas diretamente deverão
continuar explícitas. `quarkus-flow-redis` e
`quarkus-flow-durable-kubernetes` só entram depois do spike da Task 7.1. Não adicionar
`quarkus-messaging-kafka`. O SDK v4 do Cosmos será usado pela API assíncrona, com um
cliente singleton por processo. A autenticação Azure usará `azure-identity` com
`DefaultAzureCredential`: Managed Identity na implantação Azure ou Workload
Identity no Kubernetes, sempre com RBAC de plano de dados de menor privilégio. Chave
e connection string ficam proibidas em PRD.

### Gate de compatibilidade

O spike de 2026-07-24 registrou:

- Quarkus `3.33.2.1`, Flow `0.10.2`, Quarkus LangChain4j `1.11.2` e
  LangChain4j `1.16.2`/Agentic `1.16.2-beta26`;
- ausência de conflito de versão LangChain4j na dependency tree;
- compilação e bootstrap com Quarkus `3.33.2.1` e Java 25;
- disponibilidade compilável de `agent`, `emitJson`, `listen`,
  `extensionByInstanceId`, `@RegisterAiService`, `@SequenceAgent` e structured
  output;
- bootstrap com canais internos completos, sem Kafka, após registrar a ponte Flow
  como beans da aplicação;
- chaves Flow/Ollama confirmadas nos metadados dos JARs;
- somente um conflito não relacionado: `jackson-jq` 1.6.1 omitido em favor de 1.6.2.

C1 escolheu a primeira alternativa para os incrementos 1 a 6:

1. **recomendada:** manter Quarkus `3.33.2.1`, fixar somente os BOMs de extensão em
   Flow `0.10.2`/LangChain4j `1.11.2` e registrar um shim interno, versionado e
   testado para a ponte Flow;
2. implementar integralmente o SPI `EventConsumer`/`EventPublisher` documentado pelo
   Flow, com maior quantidade de código;
3. permitir connector/Kafka, contrariando o escopo atual;
4. atualizar Quarkus, como mudança de escopo não recomendada.

C4 propõe evoluir da alternativa 1 para adapters próprios dos SPIs da alternativa 2,
mantendo as versões já comprovadas e sem adotar as alternativas 3 ou 4. A Task 7.1
deve demonstrar compatibilidade antes da primeira substituição do shim.

## Configuração planejada

Configuração base já comprovada nos incrementos 1 a 6 e alvo da evolução:

```properties
# Quarkus Flow: desabilitar a ponte padrão somente depois de os SPIs próprios passarem
quarkus.flow.messaging.defaults-enabled=false
quarkus.flow.messaging.lifecycle-enabled=false
quarkus.flow.tracing.enabled=true
quarkus.flow.devui.backend.storage.enabled=true

# Persistência Flow Redis/Valkey e Durable Kubernetes:
# nomes exatos serão registrados pelo spike da versão 0.10.2

# Seleção textual obrigatória fora de dev/test; valores permitidos: couchdb, cosmosdb
conformidade.persistencia.backend=${CONFORMIDADE_PERSISTENCIA_BACKEND}
%dev.conformidade.persistencia.backend=couchdb
%test.conformidade.persistencia.backend=couchdb

# CouchDB — DES; compose-devservices.yml mapeia host/porta descobertos
conformidade.couchdb.host=${COUCHDB_HOST:localhost}
conformidade.couchdb.port=${COUCHDB_PORT:5984}
conformidade.couchdb.database=${COUCHDB_DATABASE:conformidade}
conformidade.couchdb.username=${COUCHDB_USERNAME:}
conformidade.couchdb.password=${COUCHDB_PASSWORD:}

# Compose Dev Services — somente DES; preservar o volume nomeado do CouchDB
%dev.quarkus.compose.devservices.remove-volumes=false
%test.quarkus.compose.devservices.enabled=false

# Cosmos DB for NoSQL — PRD; credencial resolvida por DefaultAzureCredential
conformidade.cosmos.endpoint=${COSMOS_ENDPOINT:}
conformidade.cosmos.database=${COSMOS_DATABASE:conformidade}
conformidade.cosmos.container=${COSMOS_CONTAINER:analises}
conformidade.cosmos.lease-container=${COSMOS_LEASE_CONTAINER:analises-leases}

# Ollama local ou container explicitamente iniciado; Dev Services continua desligado
quarkus.langchain4j.ollama.devservices.enabled=false
quarkus.langchain4j.ollama.base-url=${OLLAMA_BASE_URL:http://localhost:11434/}
quarkus.langchain4j.ollama.chat-model.model-id=${OLLAMA_MODEL:llama3.2:3b}
quarkus.langchain4j.ollama.chat-model.temperature=0.2
quarkus.langchain4j.ollama.chat-model.model-options.num-ctx=2048
quarkus.langchain4j.ollama.chat-model.format=json
quarkus.langchain4j.ollama.timeout=60s

# Logs integrais somente no perfil explicitamente selecionado para a PoC
%poc.quarkus.langchain4j.log-requests=true
%poc.quarkus.langchain4j.log-responses=true
%poc.quarkus.log.category."dev.langchain4j".level=DEBUG
%poc.quarkus.log.category."io.quarkiverse.langchain4j".level=DEBUG

# Reutiliza o checklist simulado no perfil da PoC
%poc.simtr-hub.simulador.parametrizacao-checklist.habilitado=true
```

Regras:

- não adicionar nenhuma propriedade `mp.messaging.*.connector`;
- obter credenciais de CouchDB/Redis por variável ou Secret, nunca por valor
  versionado;
- falhar no startup fora de dev/test quando o backend não estiver explicitamente
  selecionado ou sua configuração obrigatória estiver ausente;
- proibir chave e connection string do Cosmos em PRD; usar Microsoft Entra ID com
  Managed Identity ou Workload Identity e RBAC de plano de dados de menor
  privilégio;
- limitar credenciais locais do CouchDB ao ambiente de DES e nunca versionar valor
  produtivo;
- manter a API assíncrona do Cosmos e um cliente singleton, sem bloquear event loop;
- definir a porta documental com `Uni<Void>` para escrita e
  `Uni<Optional<VisaoAnaliseConformidade>>` para leitura;
- usar `compose-devservices.yml` somente no `quarkus:dev`, com CouchDB `3.5.2`,
  health check, inicialização idempotente do banco e volume nomeado preservado;
- manter os testes isolados em containers efêmeros próprios, sem reutilizar o
  volume do `quarkus:dev`;
- validar nomes e defaults de Redis/Valkey, cursor, Leases, health, timeouts e
  reconexão antes de alterar propriedades de produção;
- desabilitar chamadas reais ao Ollama nos testes padrão e substituir a porta de IA;
- se o formato JSON for uma opção dentro de `model-options` na versão efetiva,
  ajustar somente após evidência do configuration reference/bootstrap;
- não registrar texto, checklist, evidência, prompt, resposta, credencial ou stack
  trace nos eventos estruturados da aplicação;
- documentar que `%poc` pode registrar conteúdo sensível e deve ser usado apenas com
  dados sintéticos.

### Modelo de ameaças da persistência por ambiente

- endpoints dos bancos vêm somente de configuração confiável; nenhum valor de
  request pode selecionar host ou URL, reduzindo SSRF;
- texto, checklist, revisão e resultado são dados protegidos: podem ser persistidos,
  mas não entram em logs, spans, CloudEvents, checkpoint Redis ou diagnóstico do SDK;
- IDs de documento são derivados no servidor e validados antes de compor paths,
  partições ou point reads;
- JSON lido dos dois bancos é entrada não confiável e deve ter schema, identidades,
  hash e `versaoSchema` validados antes de chegar à aplicação;
- `_rev` e `_etag` ficam nos adapters e impedem lost update; conflito é traduzido
  para erro interno estável;
- `_changes` e Change Feed são pelo menos uma vez; replay, duplicidade, poison
  document e perda de cursor/lease são tratados sem dupla conclusão;
- credenciais nunca são versionadas, retornadas ou registradas; o mecanismo de
  autenticação Cosmos e seu menor privilégio exigem decisão humana própria;
- timeouts, retry e diagnósticos do SDK serão configurados e testados sem incluir
  conteúdo negocial ou segredo.

## Modelo, fronteiras e responsabilidades

### Domínio

Os modelos de análise ficarão em
`conformidade.dominio.modelo.analise`, sem imports de Quarkus, Flow, LangChain4j,
Jackson, REST ou Messaging:

- `SolicitacaoAnaliseConformidade`: `identificadorDocumento`, texto e referência do
  checklist;
- `ParecerConformidade`: `CONFORME`, `INCONFORME`, `INCONCLUSIVO` e
  `NAO_ANALISADO`;
- `OrigemResultado`: `AGENTE`, `FALLBACK_TECNICO` e `REVISAO_HUMANA`;
- `ResultadoApontamentoConformidade`: resultado completo por apontamento;
- `ResultadoAnaliseConformidade`: checklist congelado, resumo e resultados;
- `RevisaoHumanaConformidade`: observação e lista completa revisada;
- `StatusAnaliseConformidade`: `EM_PROCESSAMENTO`, `AGUARDANDO_REVISAO`,
  `CONCLUIDA` e `FALHOU`;
- `VisaoAnaliseConformidade`: projeção consultável pela API;
- erros específicos para instância ausente, transição inválida, checklist vazio,
  resultado do agente inválido e revisão inconsistente.

### Aplicação

Portas de entrada:

- `IniciarAnaliseConformidade`;
- `ConsultarAnaliseConformidade`;
- `RevisarAnaliseConformidade`.

Portas de saída:

- `AnalisarTextoComChecklist`;
- `ArmazenarEstadoAnaliseConformidade`;
- portas de leitura/gravação de documentos da análise;
- a porta temporária `PublicarRevisaoNoWorkflow` será substituída pela gravação
  idempotente da revisão.

O workflow pode chamar a porta de entrada existente `ConsultarChecklist`, por ser
composição interna no mesmo domínio. Nenhuma classe de aplicação conhece o store
concreto, `MutinyEmitter`, CloudEvent, Ollama ou DTO de borda.

### Adaptadores

- REST v1 traduz JSON público para modelos/portas de entrada;
- Ollama implementa a porta de análise e encapsula LangChain4j Agentic;
- uma porta documental neutra expressa documentos, snapshots, projeção,
  idempotência e concorrência otimista sem tipos de fornecedor;
- CouchDB implementa a porta em DES com `_rev`/MVCC e `_changes`;
- Cosmos DB for NoSQL implementa a porta em PRD com Azure Cosmos DB Java SDK v4,
  `_etag`/`If-Match` e Change Feed Processor;
- `quarkus-flow-redis` implementa checkpoint técnico sem conteúdo de negócio;
- `EventPublisher` e adapters `EventConsumer` por backend traduzem CloudEvents
  referenciais;
- Durable Kubernetes associa workers a Leases;
- recursos estáticos consomem somente a API pública.

## Fluxo de dados

```text
POST /conformidade/analises
  -> validar DTO e mapear solicitação
  -> gerar correlationId e persistir documento/projeção pela porta neutra
  -> iniciar instância Flow com IDs/referências e registrar EM_PROCESSAMENTO
  -> ConsultarChecklist.executar(comando) [Uni, sem bloqueio]
  -> congelar Checklist pelo contrato documental; contexto guarda referência/hash
  -> montar EntradaAnaliseAgente
  -> AnalisarTextoComChecklist [adapter Ollama + FT]
  -> validar/normalizar deterministicamente
       -> sucesso AGENTE
       -> ou FALLBACK_TECNICO por falha técnica esgotada
  -> persistir resultado preliminar
  -> emitJson(revisao.solicitada.v1 com referência)
  -> EventPublisher -> backend documental -> AGUARDANDO_REVISAO
  -> listen(revisao.concluida.v1 por flowinstanceid/correlationId)
  -> checkpoint WAITING no Redis/Valkey

PUT /conformidade/analises/{instanceId}/revisao
  -> exigir AGUARDANDO_REVISAO
  -> validar lista completa contra checklist congelado
  -> persistir documento imutável com ID/hash determinísticos
  -> _changes (DES) ou Change Feed (PRD) -> EventConsumer -> CloudEvent referencial
  -> retomar instância
  -> carregar documentos por referência
  -> validar novamente a revisão
  -> resultado REVISAO_HUMANA
  -> emitJson(analise.concluida.v1 com referência)
  -> EventPublisher -> backend documental -> CONCLUIDA
```

Falhas não recuperáveis atualizam a projeção para `FALHOU` com mensagem pública
sanitizada. A página consulta a projeção; ela não é o mecanismo de pausa do Flow.

## Desenho da entrega e dos CloudEvents

| Caminho | Produtor | Consumidor | Payload |
|---|---|---|---|
| emissão Flow | `emitJson(...)` | `EventPublisher` próprio que persiste pela porta neutra | CloudEvent v1 com referência/hash |
| retomada DES | CouchDB `_changes` | adapter `EventConsumer` CouchDB | CloudEvent v1 com referência/hash |
| retomada PRD | Cosmos Change Feed Processor | adapter `EventConsumer` Cosmos | CloudEvent v1 com referência/hash |

Tipos:

```text
br.gov.caixa.simtr.conformidade.revisao.solicitada.v1
br.gov.caixa.simtr.conformidade.revisao.concluida.v1
br.gov.caixa.simtr.conformidade.analise.concluida.v1
```

Contrato mínimo:

- `specversion=1.0`;
- `id` único;
- `source` estável da capacidade;
- `type` versionado;
- `time`;
- `datacontenttype=application/json`;
- `flowinstanceid` obrigatório;
- `correlationid` obrigatório somente como extensão do envelope CloudEvent;
- `flowtaskid` preservado quando fornecido pelo Flow;
- `data` limitado a `documentoRef` (`String` opaca com o ID determinístico do
  documento), `hashConteudo` (`String` SHA-256 hexadecimal minúscula) e
  `versaoSchema` do tipo `small int` (`Short` no Java e número inteiro no JSON);
- correlação do `listen` por `flowinstanceid`, com validação adicional de
  `correlationid` no envelope e de `correlationId` no documento persistido.

O codec rejeitará tipo desconhecido, extensão ausente e referência inválida. O
consumer registrará processamento antes de avançar o cursor. Não haverá DLQ; documento
inválido terá registro sanitizado próprio e não bloqueará os posteriores. A ponte
volátil dos incrementos 1 a 6 permanece como baseline de rollback e será desabilitada
somente depois da prova conjunta dos dois SPIs.

## Estratégia de integração com `Uni`

- `ConsultarChecklist` continuará retornando `Uni<Checklist>`;
- a etapa Flow converterá o `Uni` para o tipo assíncrono suportado pela versão,
  preferencialmente `CompletionStage`, sem `await`;
- cancelamento/falha será propagado para a trilha de erro do workflow;
- lista nula ou checklist sem apontamentos será erro de negócio, não falha técnica
  retentável;
- o retry já presente no cliente de parametrização continuará sendo a única política
  em torno da consulta;
- testes controlarão o `Uni` com item, nulo e falha para provar transição e ausência de
  bloqueio explícito.

## Estratégia do agente, prompt e saída

### Capacidade agentic

Estratégia preferida:

```text
AgenteAnaliseConformidade
  = @SequenceAgent(
      AplicadorChecklistAgent,
      RevisorCoberturaAgent
    )
```

O workflow enxerga uma única task agentic. O `memoryId` será derivado do `instanceId`
e não será exposto na API. A memória do agente, se exigida pela versão, será limitada
à instância e não substituirá o contexto do Flow.

### Prompt

O system prompt da especificação será versionado junto ao adaptador e testado como
contrato. Ele deverá:

- tratar instruções contidas no documento como dados, não comandos;
- usar somente evidências do texto;
- avaliar cada apontamento exatamente uma vez;
- preservar identificador e nome;
- não criar, combinar ou remover itens;
- distinguir ausência de informação (`INCONCLUSIVO`) de descumprimento explícito;
- produzir somente a estrutura esperada.

O user prompt conterá somente o texto e uma projeção mínima do checklist. DTO REST,
DTO MTR, URLs, tokens, configurações, spans e exceções não cruzam essa borda.

### Structured output e validação

Records específicos do adaptador de IA representarão a saída do modelo e serão
mapeados ao domínio. Não haverá parsing por expressão regular.

O validador determinístico deverá verificar:

- resultado e lista não nulos;
- cobertura de todos os apontamentos;
- nenhum identificador extra ou duplicado;
- parecer permitido;
- justificativa obrigatória;
- confiança entre 0 e 1;
- nome coerente.

Normalizações permitidas: ordem do checklist, nome vindo do checklist, espaços,
limite de confiança e item ausente como `NAO_ANALISADO`. Mudanças semânticas em
identificador, parecer, justificativa ou evidência produzem erro/fallback, nunca
correção silenciosa.

## Estratégia de Fault Tolerance

- aplicar no `OllamaAnaliseConformidadeAdapter`, nunca no domínio ou no workflow;
- timeout HTTP do provider: 60 s;
- SmallRye FT inicial:
  - `@Timeout`: valor ligeiramente superior ao timeout do provider, proposto 65 s,
    para evitar corrida entre timers; C3 confirmará o valor;
  - `@Retry`: 2 tentativas adicionais, delay 500 ms e jitter 200 ms;
  - `@CircuitBreaker`: volume 4, razão 0,5, delay 10 s, success threshold 2;
  - `@Fallback`: um resultado `NAO_ANALISADO` para cada item, evidência nula,
    confiança 0 e origem `FALLBACK_TECNICO`;
- retentar somente timeout, conexão recusada, indisponibilidade, resposta truncada e
  parsing potencialmente transitório;
- não retentar validação da solicitação, checklist vazio, identificador desconhecido,
  revisão inválida ou regra determinística não transitória;
- não incluir texto, prompt, resposta, stack trace ou credenciais no fallback público;
- testar número de chamadas, abertura/recuperação do circuito e classificação das
  exceções com agente falso.

## Estratégia Human-in-the-Loop e de estado

- `emitJson(revisao.solicitada.v1)` disponibiliza o resultado preliminar;
- `listen(revisao.concluida.v1)` pausa realmente a instância;
- o endpoint só persiste revisão se a projeção no backend documental estiver em
  `AGUARDANDO_REVISAO`;
- a revisão transporta a lista completa;
- identificador e nome são imutáveis;
- confiança é somente leitura e deve ser preservada;
- o servidor valida a revisão antes de publicar e o workflow valida novamente após
  retomar;
- documento de revisão com ID determinístico e concorrência otimista nativa impede
  respostas contraditórias; repetição idêntica é idempotente;
- o adapter do feed selecionado produz CloudEvent referencial e tolera redelivery;
- o workflow revalida estado, correlação, hash e lista antes de concluir;
- após a revisão não há nova inferência;
- o resultado final tem origem `REVISAO_HUMANA`;
- o backend documental selecionado é o sistema de registro da projeção e dos
  documentos de negócio;
- Redis/Valkey guarda somente checkpoints técnicos do Flow;
- restart deve preservar a revisão e restaurar uma espera pausada;
- a garantia de suporte multipod depende de teste Kubernetes com duas réplicas.

## Contrato REST e polling propostos

| Operação | Sucesso | Erros mínimos |
|---|---|---|
| `POST /simtr-hub/v1/conformidade/analises` | `202`, `Location`, cinco valores de identidade, `EM_PROCESSAMENTO` | `400`, `409`, `503` |
| `GET /simtr-hub/v1/conformidade/analises/{instanceId}` | `200` com visão atual e cinco valores de identidade | `404` |
| `PUT /simtr-hub/v1/conformidade/analises/{instanceId}/revisao` | `202` | `400`, `404`, `409`, `422` |

Os erros usarão `ErroPadraoDto`/`ErroMensagemDto` somente na borda REST. C2 deverá
confirmar:

- nomes JSON camelCase definidos pela especificação;
- `identificadorDocumento` obrigatório no POST;
- `correlationId`, `instanceId`, `identificadorDocumento`,
  `identificadorChecklist` e `versaoChecklist` em todas as respostas de visão;
- limite máximo do texto, proposto em 20.000 caracteres;
- exigência da lista completa;
- confiança somente leitura e rejeição com `422` se alterada;
- inexistência de alteração de autenticação/autorização;
- mensagem pública sanitizada para `503`.

A página ficará em `META-INF/resources/poc-conformidade/`, fará polling a cada
1.500 ms somente enquanto `EM_PROCESSAMENTO` ou `AGUARDANDO_REVISAO`, cancelará o
timer em estado terminal e permitirá editar apenas parecer, justificativa e evidência.
Ela mostrará permanentemente as cinco informações de identidade e o aviso de que a
PoC depende do backend documental e do Redis/Valkey e só declara suporte multipod
depois da prova Kubernetes.
Não armazenará texto ou revisão no browser além do necessário à tela atual.

## Arquivos prováveis e responsabilidades

### Arquivos a modificar

| Arquivo | Responsabilidade da alteração |
|---|---|
| `pom.xml` | manter BOMs companion e acrescentar Azure Cosmos SDK, Flow Redis e Durable Kubernetes conforme os gates |
| `src/main/resources/application.properties` | selecionar backend, configurar Flow, Ollama, CouchDB/Cosmos, Redis/Valkey, durable Kubernetes e health sem segredo versionado |
| `src/test/resources/application.properties` | impedir Ollama real, selecionar CouchDB em testes padrão e estabilizar integrações |
| `src/test/java/br/gov/caixa/simtr/hub/arquitetura/ArchUnitProgressivoTest.java` | preservar direção de dependência e confinamento dos novos DTOs de borda |
| `src/test/java/br/gov/caixa/simtr/hub/arquitetura/observabilidade/ObservabilidadeLogsContratoTest.java` | incluir eventos estruturados da nova capacidade sem payload sensível |
| `src/test/java/br/gov/caixa/simtr/hub/arquitetura/observabilidade/ObservabilidadeSpansContratoTest.java` | incluir spans e atributos de API, workflow, IA e HITL |
| `doc/arquitetura-ddd-integracoes-atomicas.md` | consolidar o estado efetivamente implementado |
| `doc/adr/README.md` | indexar o novo ADR e explicar sua aplicabilidade |

### Arquivos a criar

Os nomes definitivos podem ser simplificados durante o incremento sem mudar a
responsabilidade:

| Área | Arquivos prováveis | Responsabilidade |
|---|---|---|
| ADR | `doc/adr/0010-conformidade-couchdb-checkpoint-redis-e-hitl-multipod.md` | propor persistência documental por ambiente, checkpoint Redis/Valkey, feeds nativos, idempotência e multipod |
| Domínio | `.../conformidade/dominio/modelo/analise/SolicitacaoAnaliseConformidade.java` | entrada validada do caso de uso |
| Domínio | `.../ParecerConformidade.java`, `.../OrigemResultado.java` | vocabulário fechado |
| Domínio | `.../ResultadoApontamentoConformidade.java`, `.../ResultadoAnaliseConformidade.java` | resultado tipado |
| Domínio | `.../RevisaoHumanaConformidade.java` | revisão autocontida |
| Domínio | `.../StatusAnaliseConformidade.java`, `.../VisaoAnaliseConformidade.java` | projeção de estado |
| Domínio | `.../dominio/erro/*AnaliseConformidade.java` | falhas de regra sem tipos REST |
| Aplicação | `.../aplicacao/porta/entrada/IniciarAnaliseConformidade.java` | iniciar instância |
| Aplicação | `.../ConsultarAnaliseConformidade.java` | consultar projeção |
| Aplicação | `.../RevisarAnaliseConformidade.java` | validar e publicar revisão |
| Aplicação | `.../aplicacao/porta/saida/AnalisarTextoComChecklist.java` | isolar IA |
| Aplicação | `.../ArmazenarEstadoAnaliseConformidade.java` | isolar store e transições |
| Aplicação | `.../PublicarRevisaoNoWorkflow.java` | isolar `flow-in` |
| Aplicação | `.../aplicacao/casodeuso/*AnaliseConformidadeCasoDeUso.java` | implementar as três portas de entrada |
| Aplicação | `.../aplicacao/workflow/AnaliseConformidadeFlow.java` | declarar a sequência Flow |
| Aplicação | `.../ConsultarChecklistEtapa.java` | adaptar `Uni` sem bloqueio e congelar checklist |
| Aplicação | `.../ValidarResultadoAgenteEtapa.java` | validação determinística |
| Aplicação | `.../ValidarRevisaoHumanaEtapa.java` | validação pós-retomada |
| IA | `.../adaptador/saida/ia/ollama/AplicadorChecklistAgent.java` | primeira análise tipada |
| IA | `.../RevisorCoberturaAgent.java` | revisar cobertura sem mudar identidade |
| IA | `.../AgenteAnaliseConformidade.java` | capacidade sequencial única |
| IA | `.../OllamaAnaliseConformidadeAdapter.java` | mapear porta, FT e fallback |
| IA | `.../EntradaAnaliseAgente.java` e records de saída | contrato privado da borda IA |
| Porta documental | `.../aplicacao/porta/saida/*Documento*` | necessidades neutras de persistência, consulta, idempotência e concorrência |
| CouchDB/DES | `.../adaptador/saida/couchdb/*` | documentos, projeção, snapshots, `_rev`, `_changes` e IDs determinísticos |
| Cosmos/PRD | `.../adaptador/saida/cosmos/*` | itens, projeção, snapshots, `_etag`, Change Feed e IDs determinísticos |
| Persistência Flow | configuração/adapter do `quarkus-flow-redis` | checkpoint mínimo no Redis/Valkey |
| Eventos | `.../adaptador/saida/evento/*EventPublisher*` | persistir emissões referenciais pela porta neutra e atualizar projeção |
| Eventos DES | `.../adaptador/entrada/couchdb/*Changes*` | cursor `_changes`, validação e adapter `EventConsumer` |
| Eventos PRD | `.../adaptador/entrada/cosmos/*ChangeFeed*` | leases do Change Feed, validação e adapter `EventConsumer` |
| Eventos | `.../adaptador/saida/messaging/interno/CloudEventMapper.java` | CloudEvent v1 referencial e migração da ponte volátil |
| REST | `.../adaptador/entrada/rest/v1/AnaliseConformidadeResource.java` | POST, GET e PUT |
| REST | `.../AnaliseConformidadeRestMapper.java` | mapear domínio, DTO e erro público |
| REST | `.../rest/v1/dto/*.java` | contratos JSON exclusivos da borda |
| Observabilidade | `.../adaptador/configuracao/AnaliseConformidadeObservabilidade.java` | eventos, MDC e spans sem payload |
| UI | `src/main/resources/META-INF/resources/poc-conformidade/index.html` | estrutura acessível |
| UI | `src/main/resources/META-INF/resources/poc-conformidade/app.js` | API, polling e revisão |
| UI | `src/main/resources/META-INF/resources/poc-conformidade/styles.css` | apresentação responsiva mínima |
| Containers | `Dockerfile`, Compose e configuração associada | DES com app, CouchDB, Redis/Valkey e Ollama; Cosmos opt-in |
| Kubernetes | manifests locais da PoC | Leases, readiness, duas réplicas e failover |
| Documentação | `doc/poc/README.md` | pré-requisitos, execução, teste opcional e limitações |
| Testes | classes espelhadas sob `src/test/java/.../conformidade` e `.../contrato` | domínio, aplicação, Flow, canais, API, agente falso e guardrails |

## Decisões e impactos

| Dimensão | Situação | Checkpoint humano adicional? |
|---|---|---|
| Contrato | três endpoints, novos JSONs, `Location` e status 202/400/404/409/422/503 | sim, C2 |
| Arquitetura | Quarkus Flow, Agentic, canais internos, nova porta IA e estado volátil | sim, C1 |
| Segurança | texto não confiável enviado ao modelo, prompt injection, página de revisão e logs potencialmente sensíveis | sim, C2 e C3 |
| Comportamento observável | novas propriedades, timeout/retry/circuit breaker, eventos, logs e spans | sim, C1 e C3 |
| Contrato da evolução | `identificadorDocumento` obrigatório e cinco valores de identidade nas respostas/página | sim, C4 |
| Arquitetura da evolução | porta neutra, CouchDB/DES, Cosmos/PRD, Redis/Valkey, feeds nativos, contexto referencial, containers e Leases | sim, C4 e C5 |
| Segurança da evolução | credenciais externas, dados persistidos e conteúdo fora do checkpoint/log | C4 e C6; Cosmos por Entra ID, identidade gerenciada/federada e RBAC de plano de dados |
| Comportamento observável da evolução | backend, cursor/lease, reconexão, health/readiness, replay, restart, timeouts e failover | sim, C4/C5 e checkpoint antes da configuração final |

## Ordem de implementação e tarefas

Cada incremento termina compilável, com testes focados e rollback próprio. Depois do
GO, somente o próximo item pendente do `todo.md` será executado.

### Pré-implementação — baseline e decisão de compatibilidade

#### Task 0.5 — Selecionar e inicializar o baseline SonarQube

**Descrição:** após C0, verificar se existem pacotes em `sonar/`, apresentar as
fontes permitidas e pedir ao usuário a escolha exigida pelo `AGENTS.md`. Inicializar
o baseline escolhido antes de alterar POM, propriedades ou código.

**Critérios de aceitação:**

- fonte e eventual pacote registrados no `todo.md`;
- baseline inicializado pelo script oficial;
- limitações de baseline offline registradas sem alegar aprovação.

**Verificação:**

- saída de `./validar-checkpoint-sonarqube.ps1 -InitializeBaseline` com os parâmetros
  autorizados;
- estado do checkpoint registrado pelo próprio script.

**Dependências:** C0.

**Tamanho estimado:** S.

**Rollback:** nenhuma alteração de produção; remover somente registros temporários não
versionados criados pelo script, se aplicável e autorizado.

#### Task 0.6 — Resolver a matriz de compatibilidade

**Descrição:** usar POM temporário fora da árvore de produção ou comandos read-only
para resolver os BOMs companion, árvore de dependências e APIs exigidas. Não alterar
o POM do repositório antes de C1.

**Critérios de aceitação:**

- tabela com versões efetivas e convergência LangChain4j;
- confirmação ou incompatibilidade demonstrada de cada API do gate;
- recomendação única para C1, sem upgrade automático.

**Verificação:**

- `mvn help:effective-pom`;
- `mvn dependency:tree -Dincludes=io.quarkiverse.flow:*,io.quarkiverse.langchain4j:*,dev.langchain4j:*`;
- inspeção das assinaturas nos artefatos resolvidos;
- compilação de spike descartável quando a assinatura não puder ser comprovada por
  metadados.

**Dependências:** Task 0.5.

**Tamanho estimado:** M.

**Rollback:** apagar somente o spike temporário validado; nenhum arquivo do
repositório é alterado.

### Checkpoint C1 — Arquitetura, versões e configuração

- apresentar versões efetivas, APIs disponíveis, dependency tree e resultado do
  spike;
- confirmar BOMs/dependências, estratégia agentic, canais sem connector, estado
  volátil e necessidade do ADR-0009;
- confirmar propriedades Flow/Ollama e que logs integrais ficam restritos a `%poc`;
- decisão humana: `GO`, `NO-GO` ou pedido de ajuste para o incremento 1.

### Incremento 1 — Compatibilidade e dependências

#### Task 1.1 — Integrar extensões e provar bootstrap mínimo

**Descrição:** importar BOMs, declarar dependências sem versão manual, aplicar a
configuração mínima e criar somente o bootstrap/prova de API necessário. Propor o
ADR-0009 com status `Proposto`; ele não passa a `Aceito` sem decisão humana.

**Critérios de aceitação:**

- POM convergente em Quarkus `3.33.2.1`/Java 25;
- aplicação inicia sem Kafka, broker ou Dev Service Ollama;
- chaves de configuração reconhecidas;
- APIs de Flow, Agentic e Ollama compilam;
- testes existentes continuam verdes;
- ADR proposto e índice coerente.

**Verificação:**

- `mvn -q dependency:tree`;
- `mvn -q -DskipTests compile`;
- teste de bootstrap Quarkus;
- `mvn -q test`;
- checkpoint Sonar do incremento.

**Dependências:** C1.

**Arquivos prováveis:** `pom.xml`, `application.properties`,
`src/test/resources/application.properties`, ADR-0009 e teste de bootstrap.

**Tamanho estimado:** M.

**Rollback:** remover imports, dependências, propriedades e spike versionado; restaurar
o POM anterior e confirmar que a suíte existente inicia.

### Checkpoint CA — ADR arquitetural proposto

- revisar o texto efetivamente criado para o ADR-0009 e sua entrada no índice;
- confirmar que a decisão se limita à PoC, explicita volatilidade e não cria
  precedente automático para produção;
- decisão humana: manter `Proposto`, ajustar, rejeitar ou mudar para `Aceito`;
- o incremento 2 não começa antes desse checkpoint, pois passa a implementar os
  canais e limites descritos no ADR.

### Incremento 2 — Canais internos e contrato CloudEvent

#### Task 2.1 — Fechar as cadeias `flow-in` e `flow-out`

**Descrição:** em RED-GREEN-REFACTOR, implementar o shim aprovado em C1, codec
CloudEvent, publisher de revisão, consumidor local de eventos de saída e a menor
porta/store necessários para processamento significativo, sem connector.

**Critérios de aceitação:**

- cadeia completa em ambos os canais;
- `byte[]` contém CloudEvent v1 estruturado;
- `flowinstanceid` é obrigatório e preservado;
- tipo desconhecido/payload inválido falha de modo controlado;
- confirmação ocorre após processamento;
- ponte Flow registrada sem depender de `mp.messaging.*.connector`;
- nenhum Kafka é iniciado ou adicionado.

**Verificação:**

- testes unitários do codec;
- `@QuarkusTest` publicando e consumindo nos canais internos normais;
- busca negativa por `quarkus-messaging-kafka` e `.connector=`;
- `mvn -q -Dtest=*CloudEvent*,*Messaging* test`;
- checkpoint Sonar.

**Dependências:** Task 1.1 e CA.

**Arquivos prováveis:** portas de publicação/store, shim CDI da ponte Flow,
`CloudEventMapper`, `RevisaoHumanaCloudEventPublisher`,
`FlowOutCloudEventConsumer` e testes.

**Tamanho estimado:** M.

**Rollback:** remover adapters, portas e configuração exclusiva dos canais; manter
somente o incremento 1 compilável.

### Checkpoint C2 — Contrato público e segurança da entrada

- apresentar OpenAPI/JSON/status propostos, erros públicos e `Location`;
- confirmar limite do texto, imutabilidade dos campos, confiança somente leitura e
  semântica 400/404/409/422/503;
- confirmar que a PoC não altera autenticação/autorização existente;
- confirmar exposição da página estática e aviso de volatilidade;
- decisão humana antes de criar os endpoints.

### Incremento 3 — API, domínio e estado em memória

#### Task 3.1 — Implementar regras puras e transições atômicas

**Descrição:** criar modelos, erros e validadores de domínio; completar o store de
memória com transições atômicas e testes concorrentes.

**Critérios de aceitação:**

- invariantes de resultado e revisão cobertas;
- estados só seguem transições permitidas;
- duas revisões concorrentes não são aceitas;
- domínio permanece sem imports de adaptadores/frameworks.

**Verificação:**

- testes unitários RED-GREEN-REFACTOR;
- teste concorrente do store;
- `mvn -q -Dtest=*AnaliseConformidade*Test test`;
- ArchUnit focado.

**Dependências:** C2 e Task 2.1.

**Arquivos prováveis:** modelos/erros de domínio, validador e
`AnaliseConformidadeMemoryStore`.

**Tamanho estimado:** M.

**Rollback:** remover o pacote de análise e voltar ao store mínimo do incremento 2.

#### Task 3.2 — Expor POST, GET e PUT sobre portas de entrada

**Descrição:** implementar casos de uso iniciais, recurso REST, DTOs e mapper sem
ligar ainda a análise real do modelo.

**Critérios de aceitação:**

- contrato aprovado em C2 implementado exatamente;
- REST depende somente de portas de entrada;
- `ErroPadraoDto` fica confinado à borda REST;
- validações e status possuem teste de contrato;
- instância desconhecida e transição inválida produzem 404 e 409;
- revisão inconsistente produz 422.

**Verificação:**

- `AnaliseConformidadeResourceQuarkusTest`;
- `AnaliseConformidadeApiContractTest` com fingerprint JSON quando estável;
- Bean Validation e erros negativos;
- ArchUnit;
- `mvn -q test`;
- checkpoint Sonar.

**Dependências:** Task 3.1.

**Arquivos prováveis:** três portas/casos de uso, resource, mapper, DTOs e testes.

**Tamanho estimado:** M.

**Rollback:** remover recurso/DTOs/casos de uso novos; preservar regras puras se
úteis ao próximo ajuste, ou reverter todo o incremento 3.

### Incremento 4 — Workflow e consulta do checklist

#### Task 4.1 — Orquestrar até o ponto anterior ao agente

**Descrição:** declarar o Flow e integrar `ConsultarChecklist` por sua porta
existente, congelando o checklist no contexto e propagando falhas sem bloqueio.

**Critérios de aceitação:**

- POST cria instância e retorna rapidamente;
- Flow registra `EM_PROCESSAMENTO`;
- a única integração de checklist é `ConsultarChecklist`;
- não há `await`, REST local, MTR client ou DTO de adaptador;
- checklist nulo/vazio falha de forma determinística;
- o retry existente não é duplicado.

**Verificação:**

- teste do workflow com `ConsultarChecklist` falso e `Uni` controlado;
- testes de item, nulo e falha;
- busca por dependências proibidas/`await`;
- ArchUnit;
- `mvn -q test`;
- checkpoint Sonar.

**Dependências:** Task 3.2.

**Arquivos prováveis:** `AnaliseConformidadeFlow`, `ConsultarChecklistEtapa`,
ajustes no caso de uso de início e testes.

**Tamanho estimado:** M.

**Rollback:** retirar a ligação do Flow e restaurar o início controlado do incremento
3; nenhum contrato de checklist é alterado.

### Checkpoint C3 — Agente, Fault Tolerance e observabilidade sensível

- apresentar prompt, projeção enviada ao modelo, structured output e defesa contra
  prompt injection;
- confirmar estratégia sequencial ou fallback demonstrado para único AI service;
- confirmar timeout HTTP 60 s, timeout FT proposto 65 s, retry, circuit breaker,
  exceções retentáveis e fallback;
- aprovar nomes de spans, eventos e atributos sem conteúdo sensível;
- confirmar que request/response DEBUG existe somente em `%poc` com dados sintéticos.

**Decisão humana registrada em 2026-07-24: `C3 GO`.**

Ficaram aprovados:

- uma única capacidade externa `AgenteAnaliseConformidade`, implementada pela
  sequência `AplicadorChecklistAgent -> RevisorCoberturaAgent` já comprovada no
  spike e acessada pelo workflow somente pela porta
  `AnalisarTextoComChecklist`;
- `memoryId` derivado do `instanceId`, sem exposição na API ou substituição do
  contexto mantido pelo Flow;
- system prompt versionado com as regras da especificação e instrução explícita
  para tratar qualquer comando encontrado no documento como conteúdo não
  confiável;
- user prompt limitado ao texto e a uma projeção JSON do checklist com
  identificador, versão, nome, orientação e os campos funcionais dos
  apontamentos; DTOs REST/MTR, datas técnicas, URLs, credenciais, configuração,
  spans e exceções ficam excluídos;
- structured output por records próprios do adaptador e validação determinística
  em Java; identificador extra ou duplicado produz fallback, item ausente é
  completado como `NAO_ANALISADO` e nenhuma alteração semântica é silenciosa;
- timeout HTTP do Ollama de 60 s; `@Timeout` de 65 s por tentativa;
  `@Retry` com 2 tentativas adicionais, delay de 500 ms e jitter de 200 ms;
  `@CircuitBreaker` com volume 4, razão 0,5, delay de 10 s e success threshold 2;
- retry somente para timeout, conexão recusada, indisponibilidade, resposta
  truncada e parsing potencialmente transitório; entrada, checklist e regras
  determinísticas inválidas não são retentadas;
- fallback técnico completo com um `NAO_ANALISADO` por apontamento, evidência
  nula, confiança 0, justificativa sanitizada e origem `FALLBACK_TECNICO`;
- aceitação de que três tentativas podem aproximar o tempo total de 181 s;
- nenhum truncamento ou particionamento silencioso para conciliar a entrada
  pública de até 20.000 caracteres com `num-ctx=2048`; falha ou resposta
  truncada segue para fallback humano como limitação explícita da PoC;
- `quarkus.flow.tracing.enabled=false` por padrão, pois o listener da versão
  `0.10.2` foi observado registrando payload e causa completa em `INFO`;
- request/response completos do LangChain4j somente no perfil local `%poc`,
  destinado exclusivamente a dados sintéticos; logs e spans próprios não
  carregam texto, prompt, resposta, evidência, credencial ou stack trace;
- spans:
  `simtr-hub.api.conformidade.analise.iniciar`,
  `simtr-hub.api.conformidade.analise.consultar`,
  `simtr-hub.api.conformidade.analise.revisar`,
  `simtr-hub.flow.conformidade.analise`,
  `simtr-hub.agent.conformidade.analisar`,
  `simtr-hub.flow.conformidade.revisao.aguardar` e
  `simtr-hub.flow.conformidade.revisao.retomar`; a consulta reutiliza
  `simtr-hub.service.checklist.consultar`;
- atributos aprovados:
  `conformidade.analise.instance_id`,
  `conformidade.checklist.identificador`,
  `conformidade.checklist.versao`,
  `conformidade.checklist.quantidade_apontamentos`,
  `conformidade.agente.modelo`,
  `conformidade.agente.origem_resultado`,
  `conformidade.analise.status` e
  `conformidade.revisao.humana`;
- eventos aprovados:
  `conformidade.analise.iniciada`,
  `conformidade.checklist.obtido`,
  `conformidade.agente.chamada.iniciada`,
  `conformidade.agente.chamada.concluida`,
  `conformidade.agente.fallback.aplicado`,
  `conformidade.revisao.solicitada`,
  `conformidade.revisao.recebida`,
  `conformidade.analise.concluida` e
  `conformidade.analise.falhou`.

### Incremento 5 — Ollama e capacidade agentic

#### Task 5.1 — Implementar agente tipado, validação e fallback

**Descrição:** criar os AI services, adaptador Ollama, prompts, mappers, validador
determinístico e política FT aprovada; integrar a task agentic ao workflow.

**Critérios de aceitação:**

- capacidade agentic real é exercida;
- modelo padrão é `llama3.2:3b`, temperatura 0,2 e `num-ctx=2048`;
- saída é tipada e validada em Java;
- prompt injection é tratada no system prompt;
- falha técnica esgotada produz fallback completo;
- falha de negócio não é retentada;
- suíte padrão não exige Ollama em execução.

**Verificação:**

- testes dos prompts e mappers;
- agente falso determinístico;
- testes de FT, retry e fallback;
- teste do workflow até emissão do resultado preliminar;
- teste real Ollama somente por perfil/tag opt-in;
- `mvn -q test`;
- checkpoint Sonar.

**Dependências:** C3 e Task 4.1.

**Arquivos prováveis:** AI services, records de IA,
`OllamaAnaliseConformidadeAdapter`, validador e testes.

**Tamanho estimado:** L.

**Rollback:** substituir a porta IA pelo falso do teste e remover integração Ollama,
mantendo o workflow/checklist compilável.

### Incremento 6 — Human-in-the-Loop completo

#### Task 6.1 — Pausar, revisar, correlacionar e retomar

**Descrição:** completar `emitJson`/`listen`, ligar os canais ao PUT, validar duas
vezes a revisão e concluir a instância.

**Critérios de aceitação:**

- Flow fica realmente aguardando sem ocupar thread;
- `revisao.solicitada.v1` atualiza a projeção para `AGUARDANDO_REVISAO`;
- CloudEvent de revisão retoma somente a instância correlacionada;
- evento com outro `flowinstanceid` não a libera;
- revisão duplicada recebe 409;
- revisão inconsistente recebe 422 e não retoma;
- resultado final tem origem `REVISAO_HUMANA`;
- reinício/volatilidade está documentado.

**Verificação:**

- teste end-to-end com agente falso: POST -> GET -> PUT -> GET;
- teste do workflow ainda parado antes do PUT;
- teste de correlação cruzada entre duas instâncias;
- teste concorrente de revisão;
- teste de falha/ack dos canais;
- `mvn -q test`;
- checkpoint Sonar.

**Dependências:** Task 5.1.

**Arquivos prováveis:** Flow, etapas de validação, publisher/consumer, casos de uso,
store e testes.

**Tamanho estimado:** L.

**Rollback:** remover `emit/listen` e a publicação do PUT, retornando ao resultado
preliminar controlado do incremento 5.

#### Task 6.2 — Fechar suíte e Sonar do incremento HITL volátil

**Descrição:** executar a suíte completa e o checkpoint Sonar do incremento 6 antes
de misturar a mudança de persistência com a ponte HITL já comprovada.

**Critérios de aceitação:**

- suíte completa verde;
- checkpoint Sonar registrado;
- qualquer `NON_COMPLIANT` recebe decisão humana pelo script;
- baseline funcional da ponte volátil fica isolado para comparação.

**Verificação:** `mvn -q test` e `./validar-checkpoint-sonarqube.ps1`.

**Dependências:** Task 6.1.

**Tamanho estimado:** S.

**Rollback:** corrigir somente regressões do incremento 6; não iniciar a evolução
durável enquanto este item estiver pendente.

### Checkpoint C4 — Evolução durável, contrato e implantação

C4 foi aprovado em 2026-07-25 para a arquitetura então centrada no CouchDB:

- CouchDB como único sistema de registro dos dados de negócio;
- Redis ou Valkey com `quarkus-flow-redis` somente para checkpoints;
- `_changes` -> `EventConsumer` -> CloudEvent como retomada sem Kafka;
- `identificadorDocumento` obrigatório e cinco valores de identidade na API/UI;
- contexto do Flow somente por referências e hashes;
- Kubernetes Leases e prova com duas réplicas;
- manutenção do ADR como `Proposto` se a prova cross-pod falhar.

O checkpoint C5 substitui somente a escolha universal do backend e do feed. Redis,
contexto referencial, identidades, ausência de Kafka, Leases e condição de aceitação
do ADR permanecem vigentes. O checkpoint C8 altera somente a evidência exigida para
concluir localmente a Task 7.3; o gate Cosmos real de pré-promoção continua vigente.

### Checkpoint C5 — Persistência documental por ambiente

C5 foi aprovado em 2026-07-26 antes da continuação da Task 7.3:

- porta documental neutra, sem `_rev`, `_etag`, `_changes`, Change Feed ou DTO de
  fornecedor fora dos adapters;
- Apache CouchDB em DES;
- Azure Cosmos DB for NoSQL em PRD, pelo Java SDK v4;
- contrato executável compartilhado para os dois adapters;
- `_changes` em DES e Change Feed Processor em PRD, ambos pelo menos uma vez;
- gate opt-in contra Cosmos Emulator ou conta não produtiva antes da promoção;
- autenticação do Cosmos excluída de C5 e sujeita a checkpoint de segurança próprio.

### Checkpoint C6 — Assincronia, autenticação e ambientes locais

C6 foi aprovado em 2026-07-26 antes da continuação da Task 7.3:

- porta documental com `Uni<Void>` nas escritas e
  `Uni<Optional<VisaoAnaliseConformidade>>` nas leituras;
- `IniciarAnaliseConformidade` e `ConsultarAnaliseConformidade` com
  `Uni<VisaoAnaliseConformidade>`;
- POST com `Uni<Response>`, GET com
  `Uni<VisaoAnaliseConformidadeResponse>` e projeção interna de eventos com
  `Uni<Void>`;
- callbacks do Flow retornando `Uni` diretamente; o conversor
  `Uni2CompletableFuture` da versão `0.10.2` faz a adaptação interna, sem conversão
  manual na aplicação;
- Azure Cosmos DB for NoSQL em PRD autenticado por Microsoft Entra ID com
  `DefaultAzureCredential`, usando Managed Identity ou Workload Identity e RBAC de
  plano de dados de menor privilégio;
- chave e connection string do Cosmos proibidas em PRD;
- `mvn quarkus:dev` inicia automaticamente CouchDB `3.5.2` por
  `compose-devservices.yml`, com health check, inicialização idempotente e volume
  nomeado preservado entre reinícios do Quarkus;
- testes continuam isolados em containers efêmeros, sem compartilhar o volume de
  DES;
- o Compose completo de uma réplica permanece na Task 8.1;
- o ambiente Kubernetes local posterior usará kind ou k3d, duas réplicas da
  aplicação, CouchDB em `StatefulSet` de um pod com PVC, além de Valkey/Redis e
  Ollama; Cosmos/Emulator não fará parte desses pods.

### Checkpoint C7 — Nome da extensão de correlação no CloudEvent

Durante a preparação do `EventPublisher` da Task 7.3, a inspeção da dependência
efetivamente resolvida encontrou uma incompatibilidade no contrato aprovado:
`io.cloudevents:cloudevents-core:4.1.0` valida nomes de extensões como
`[a-z0-9]+`. Por isso, `correlationId`, com `I` maiúsculo, é rejeitado pelo
`CloudEventBuilder` e não pode formar um CloudEvent v1 válido.

A decisão técnica aprovada é:

- usar `correlationid` somente como nome da extensão no envelope CloudEvent;
- manter `correlationId` nos modelos Java, documentos, API REST e interface;
- manter `flowinstanceid` e `flowtaskid` como já definidos pelo Flow;
- atualizar codec, testes, especificação, arquitetura e ADR de forma atômica;
- não implementar a mudança executável antes do GO deste checkpoint de contrato.

C7 foi aprovado pelo usuário em 2026-08-02 com `C7 GO`, autorizando a mudança
executável e documental acima sem alterar o casing das demais bordas.

Em 2026-08-02, o usuário confirmou também o contrato do `data` referencial da
Task 7.3: `documentoRef` é `String` opaca gerada pelo servidor e validada contra o
ID determinístico; `hashConteudo` é `String` SHA-256 hexadecimal minúscula validada
contra o conteúdo canônico; `versaoSchema` permanece `Short`, inicialmente `1`.

### Checkpoint C8 — Validação local do adapter Cosmos

Em 2026-08-03, o usuário confirmou que o ambiente local usa CouchDB e não terá
Cosmos DB disponível nesta etapa. A decisão aprovada é:

- manter CouchDB real como backend e integração local;
- validar o adapter Cosmos hermeticamente pelo contrato compartilhado com repositório
  determinístico e por mocks das APIs do Azure Cosmos DB Java SDK v4;
- manter o teste contra Cosmos Emulator ou conta não produtiva como opt-in opcional,
  sem bloquear a Task 7.3 ou a suíte local;
- não interpretar os mocks como prova de conectividade, autenticação, RBAC ou
  compatibilidade do serviço real;
- exigir a integração Cosmos real antes de qualquer promoção para PRD.

### Incremento 7 — Persistência durável e contrato

#### Task 7.1 — Provar compatibilidade e roteamento mínimo

**Descrição:** executar spike sem alterar o comportamento público para validar
`quarkus-flow-redis`, Redis/Valkey, APIs CouchDB/`_changes`,
`quarkus-flow-durable-kubernetes`, SPI `EventConsumer` e a hipótese de retomada
cross-pod.

**Critérios de aceitação:**

- dependency tree compatível com Quarkus `3.33.2.1` e Flow `0.10.2`;
- checkpoint mínimo é gravado/restaurado no Redis/Valkey;
- um documento CouchDB gera CloudEvent referencial repetível;
- Leases e identidade do `WorkflowApplication` são compreendidos por teste;
- limitação de roteamento cross-pod é registrada sem adotar fallback.

**Verificação:** testes de compatibilidade focados, `dependency:tree`, restart mínimo
e experimento local de duas réplicas quando a infraestrutura estiver disponível.

**Dependências:** Task 6.2 e C4.

**Arquivos prováveis:** POM, propriedades de teste e classes de spike sob testes.

**Tamanho estimado:** M.

**Rollback:** remover dependências e testes de spike, mantendo a baseline do
incremento 6.

#### Task 7.2 — Evoluir o contrato de identidades

**Descrição:** acrescentar `correlationId`, `identificadorDocumento`,
`identificadorChecklist` e `versaoChecklist` à visão, mantendo `instanceId` nos paths
atuais.

**Critérios de aceitação:**

- POST exige `identificadorDocumento`;
- Hub gera `correlationId` estável;
- POST e GET expõem os cinco valores;
- revisão valida a correlação persistida e não aceita troca de identidades;
- DTOs continuam exclusivos da borda REST.

**Verificação:** contract tests/OpenAPI, casos de uso, mapper e ArchUnit.

**Dependências:** Task 7.1 e C4.

**Arquivos prováveis:** modelos/portas, casos de uso, DTOs REST, mapper, Resource e
testes.

**Tamanho estimado:** M.

**Rollback:** remover campos aditivos e voltar ao contrato aprovado em C2.

#### Task 7.3 — Persistir agregado e projeção por adapters documentais

**Descrição:** implementar porta documental neutra e os adapters CouchDB/DES e Azure
Cosmos DB for NoSQL/PRD para documento inicial, snapshot do checklist, resultados,
revisão, falha e projeção, substituindo o store em memória. Persistir emissões
referenciais do Flow por um `EventPublisher` que dependa somente da porta.

**Critérios de aceitação:**

- texto e `identificadorDocumento` são persistidos sem aparecer em logs;
- snapshot do checklist é imutável, possui hash `String` SHA-256 em hexadecimal
  minúsculo e `versaoSchema` `Short`;
- `emitJson` publica `documentoRef` `String`, `hashConteudo` `String` e
  `versaoSchema` `Short` por `EventPublisher`, sem payload negocial completo,
  usando `correlationid` somente como extensão do envelope CloudEvent;
- projeção é consultável por `instanceId` e correlacionada por `correlationId`;
- porta de aplicação não expõe token, DTO ou exceção de fornecedor;
- CouchDB traduz concorrência para `_rev`/MVCC;
- Cosmos traduz concorrência para `_etag`/`If-Match` pela API assíncrona do Java SDK
  v4 e usa `correlationId` como chave lógica de partição;
- revisão idêntica é idempotente e revisão contraditória recebe conflito;
- a mesma suíte de contrato passa para CouchDB real e para o repositório Cosmos
  determinístico;
- mocks do Java SDK v4 comprovam partition key, `_etag`/`If-Match`, consulta,
  conflitos e ausência sem acesso de rede;
- nenhum DTO CouchDB ou Cosmos atravessa a borda do adapter;
- configuração fora de dev/test falha se o backend não estiver selecionado;
- porta documental usa `Uni<Void>` nas escritas e
  `Uni<Optional<VisaoAnaliseConformidade>>` nas leituras, sem `await`, `join` ou
  bloqueio do event loop;
- portas de entrada, recursos REST, projeção de eventos e callbacks Flow propagam
  `Uni` até a borda suportada;
- Cosmos usa `DefaultAzureCredential` e RBAC de plano de dados; chave e connection
  string são rejeitadas em PRD;
- `mvn quarkus:dev` sobe CouchDB automaticamente e preserva seu volume entre
  reinícios do Quarkus;
- testes CouchDB permanecem isolados em containers efêmeros.

**Verificação:** RED/GREEN por fatias; contrato compartilhado; CouchDB real em
container; repositório Cosmos determinístico e Java SDK v4 mockado; concorrência,
conflitos, restart da aplicação, REST, ArchUnit e busca negativa por tipos de
fornecedor fora dos adapters. O teste contra Cosmos Emulator ou conta não produtiva
permanece opt-in não bloqueante e será obrigatório somente antes da promoção para
PRD.

**Dependências:** Task 7.2, C5, C6, C7 e C8.

**Arquivos prováveis:** portas de persistência, contratos de teste, documentos e
mappers dos adapters CouchDB/Cosmos, casos de uso, seleção de backend e testes.

**Tamanho estimado:** L.

**Rollback:** selecionar o adapter em memória pela mesma porta sem remover documentos
ou itens já gravados.

#### Task 7.4 — Reduzir contexto e habilitar checkpoint Redis/Valkey

**Descrição:** transformar o contexto do workflow em envelope de referências e
habilitar o provider Redis do Flow.

**Critérios de aceitação:**

- checkpoint não contém texto, checklist, resultados ou revisão completos;
- etapas carregam conteúdo do backend documental por portas;
- pausa em `WAITING` é restaurada após reinício;
- projeção continua vindo do backend selecionado;
- indisponibilidade de cada backend produz falha/health sanitizados e distintos.

**Verificação:** inspeção serializada do checkpoint, restart durante processamento e
espera, testes de falha do backend documental/Redis e ausência de payload sensível.

**Dependências:** Task 7.3.

**Arquivos prováveis:** contexto/etapas Flow, portas de leitura, POM, propriedades e
testes de restart.

**Tamanho estimado:** L.

**Rollback:** desabilitar provider e restaurar contexto anterior somente no perfil de
rollback, sem misturar os dados persistidos.

#### Task 7.5 — Entregar revisão pelo feed nativo com idempotência

**Descrição:** completar a substituição da ponte Reactive Messaging padrão: a revisão
REST é gravada pelo contrato documental e o adapter `EventConsumer` do backend
selecionado entrega CloudEvent referencial ao `listen(...)`.

**Critérios de aceitação:**

- `202` ocorre depois da persistência da revisão;
- cursor CouchDB e leases do Change Feed sobrevivem a reconexão/restart;
- evento possui ID determinístico, `flowinstanceid`, `correlationId`, referência e
  hash;
- replay e dois consumers não concluem duas vezes;
- documento inválido fica observável e não bloqueia o feed;
- nenhuma dependência Kafka/AMQP é adicionada.
- a ponte padrão só é desabilitada depois de `EventPublisher` e `EventConsumer`
  passarem em conjunto.

**Verificação:** E2E POST -> GET -> PUT -> restart/replay -> GET, duplicidade,
correlação cruzada, falha antes/depois da entrega e busca negativa por broker.

**Dependências:** Task 7.4.

**Arquivos prováveis:** endpoint/caso de uso de revisão, adapters `_changes` e Change
Feed Processor, CloudEvent mapper, idempotência e testes.

**Tamanho estimado:** L.

**Rollback:** reativar temporariamente a ponte volátil isolada e manter documentos de
revisão sem reprocessamento automático.

#### Task 7.6 — Fechar suíte e Sonar da persistência

**Descrição:** consolidar os testes do incremento 7, tratar os dois riscos encontrados
no fechamento da Task 7.5 e executar o checkpoint Sonar. O teste de cursor CouchDB
será executado sob o mesmo harness `@QuarkusTest` dos contextos Quarkus para impedir
que providers do MicroProfile Context sejam carregados por classloaders
incompatíveis. Também será feito um spike pareado de Flow `0.13.0` e Quarkus
LangChain4j `1.12.0`, mantendo Quarkus `3.33.2.1` e Java 25: a atualização só será
retida se dependency tree, compilação, bootstrap, matriz focada, E2E e configuração
de auto-restore forem compatíveis; em caso contrário, as versões aprovadas
`0.10.2`/`1.11.2` serão restauradas e a limitação será registrada. A restauração será
comprovada ainda em uma única réplica, mas entre dois processos JVM distintos que
reutilizem os mesmos backends; cross-pod, Lease e failover continuam exclusivamente
na Task 8.3. A suíte completa também deverá eliminar a janela de consistência
encontrada no E2E: o feed não pode publicar o documento da revisão antes de a
projeção tornar sua referência carregável pelo workflow. O ajuste fica limitado à
ordenação recuperável da reserva documental, sem novo contrato, backend ou
abstração de mensageria.

**Critérios de aceitação:**

- o teste de cursor passa junto dos contextos `@QuarkusTest` no mesmo fork, sem
  `ServiceConfigurationError`;
- o spike registra por evidência se Flow `0.13.0`/LangChain4j `1.12.0` pode ser
  adotado sem mudar Quarkus, Java ou contratos da aplicação;
- nenhuma chave de configuração desconhecida de auto-restore permanece se o upgrade
  for retido; se o defeito persistir ou houver regressão, as versões anteriores são
  mantidas e o risco fica explícito;
- uma instância `WAITING` criada em um processo é restaurada e retomada por outro
  processo usando o mesmo CouchDB e Valkey, sem declarar suporte multipod;
- duas revisões sequenciais de instâncias distintas são entregues somente depois de
  suas referências estarem carregáveis, sem perda do segundo evento nem conclusão
  cruzada;
- a suíte completa e o checkpoint Sonar atendem aos gates vigentes, ou eventual
  `NON_COMPLIANT` é submetido à decisão humana prevista no processo.

**Verificação:** `mvn -q test`, testes de integração com containers,
matriz de compatibilidade/versionamento, roteiro automatizado de restart entre JVMs,
`git diff --check` e `./validar-checkpoint-sonarqube.ps1`.

**Dependências:** Task 7.5.

**Tamanho estimado:** M.

### Incremento 8 — Containers e múltiplos pods

#### Task 8.1 — Empacotar execução local de uma réplica

**Descrição:** estender o `compose-devservices.yml` já usado para iniciar
automaticamente o CouchDB no `quarkus:dev` e fornecer imagem da aplicação e Compose
de DES para aplicação, CouchDB e Redis/Valkey, reutilizando o Ollama e os modelos já
instalados no host por `host.docker.internal`; incluir health checks e preflight do
modelo, manter volume durável para CouchDB e preservar o Cosmos Emulator como
integração opt-in separada. O Redis/Valkey permanece disponível durante as provas da
PoC e não recebe garantia de recuperação própria neste escopo.

**Critérios de aceitação:** restart do container da aplicação, mantendo CouchDB,
Redis/Valkey e o Ollama do host disponíveis, preserva análise/revisão; o preflight
confirma que o modelo configurado já existe; credenciais vêm de configuração externa;
nenhum segredo entra na imagem ou no Git. Perda ou restart do próprio Redis/Valkey
não faz parte da prova. Exclusivamente neste Compose local, o tenant OIDC e o cliente
OIDC padrão ficam desabilitados; os perfis `dev` e produtivo permanecem inalterados.

**Verificação:** build de imagem, `docker compose up`, health, E2E e restart.

**Dependências:** Task 7.6.

**Tamanho estimado:** M.

#### Task 8.2 — Configurar identidade durável em Kubernetes

**Descrição:** preparar ambiente kind ou k3d e manifests locais com duas réplicas da
aplicação, `quarkus-flow-durable-kubernetes`, Leases, readiness, CouchDB em
`StatefulSet` de um pod com PVC, Redis/Valkey e Ollama compartilhados. Cosmos e seu
emulador não serão implantados nesse ambiente local.

**Critérios de aceitação:** cada pod adquire identidade estável; pod sem Lease não
fica ready; rolling restart preserva Lease recuperável.

**Verificação:** kind/k3d, inspeção de Leases, readiness e restart controlado.

**Dependências:** Task 8.1.

**Tamanho estimado:** M.

#### Task 8.3 — Provar cross-pod e failover

**Descrição:** iniciar uma análise em um pod, enviar revisão por outro e interromper
o owner em diferentes pontos.

**Critérios de aceitação:** exatamente a instância correlacionada retoma uma vez,
dados e checkpoint sobrevivem à substituição do pod da aplicação enquanto os
backends compartilhados permanecem disponíveis, e o resultado fica consultável por
qualquer pod. Se a prova falhar, parar, manter ADR-0010 `Proposto` e solicitar nova
decisão arquitetural.

**Verificação:** teste automatizado ou roteiro reproduzível com evidências de pod,
Lease, IDs, restart e resultado.

**Dependências:** Task 8.2.

**Tamanho estimado:** L.

#### Task 8.4 — Fechar suíte e Sonar da infraestrutura

**Descrição:** executar testes e checkpoint do fingerprint executável depois da
configuração de containers/Kubernetes.

**Dependências:** Task 8.3.

**Verificação:** suíte aplicável, validação de manifests, `git diff --check` e
checkpoint Sonar.

**Tamanho estimado:** S.

### Incremento 9 — Página, guardrails, observabilidade e documentação

#### Task 9.1 — Entregar a página estática com identidades e polling

**Descrição:** criar interface acessível e responsiva que inicia, acompanha e revisa
uma análise, sem framework frontend.

**Critérios de aceitação:**

- formulário exige `identificadorDocumento`, checklist, versão e texto;
- polling de 1.500 ms para em estados terminais;
- as cinco informações de identidade permanecem visíveis e somente leitura;
- campos imutáveis e confiança não são editáveis;
- erros 400/404/409/422/503 são apresentados sem stack trace;
- dependências operacionais e limite multipod são visíveis.

**Verificação:** navegador, rede, acessibilidade por teclado/labels e ausência de
`localStorage`/`sessionStorage`.

**Dependências:** Task 8.4.

**Arquivos prováveis:** `index.html`, `app.js`, `styles.css`.

**Tamanho estimado:** M.

#### Task 9.2 — Fechar observabilidade e guardrails arquiteturais

**Descrição:** adicionar telemetria dos adapters CouchDB/Cosmos, Redis/Valkey,
cursor/leases, replay, checkpoint, Lease Kubernetes e failover sem payload sensível.

**Critérios de aceitação:**

- logs/spans carregam IDs, backend, operação, replay e trace context;
- nenhum texto, prompt, resposta, revisão, evidência, credencial ou documento aparece;
- DTO REST/IA/CouchDB/Cosmos/CloudEvent permanece em sua borda;
- aplicação/domínio não importam adaptadores;
- health/readiness distinguem dependências e Lease.

**Verificação:** contratos de observabilidade, ArchUnit e testes negativos de
conteúdo sensível.

**Dependências:** Task 9.1 e checkpoint humano dos valores observáveis.

**Tamanho estimado:** M.

#### Task 9.3 — Consolidar documentação e ADRs

**Descrição:** atualizar README da PoC e, somente agora, todos os documentos de
arquitetura para refletir o estado efetivamente implementado.

**Critérios de aceitação:**

- comandos de containers e Kubernetes, restart, replay e limites documentados;
- consolidado arquitetural descreve apenas comportamento comprovado;
- ADR-0009 não é apagado e só é marcado substituído depois da aceitação humana;
- ADR-0010 só muda de `Proposto` para `Aceito` por decisão humana explícita;
- nenhum formato derivado é gerado.

**Verificação:** links/índice Markdown, comparação com código/testes e
`git diff --check`.

**Dependências:** Task 9.2 e prova Task 8.3.

**Tamanho estimado:** M.

#### Task 9.4 — Fechar suíte, verify e Sonar final

**Descrição:** executar a matriz completa e preparar as evidências de encerramento.

**Verificação:** `mvn -q test`, `mvn -q verify`, integrações opt-in documentadas,
`git diff --check` e checkpoint Sonar final.

**Dependências:** Task 9.3.

**Tamanho estimado:** M.

#### Task 9.5 — Documentar a verificação reproduzível da PoC

**Descrição:** criar um guia operacional em Markdown que permita a outra pessoa verificar, passo a
passo, as capacidades implementadas e distinguir a suíte padrão, a demonstração HITL local e as
provas opt-in de restart e múltiplos pods.

**Critérios de aceitação:**

- pré-requisitos, preparação segura e comandos exatos documentados;
- cada etapa informa o resultado esperado e a evidência que comprova;
- fluxo manual pela página e verificações automatizadas são explicados separadamente;
- restart entre JVMs, Compose, kind/failover, observabilidade e Sonar possuem rotas de validação;
- limites não comprovados, especialmente Cosmos real e HA dos backends, permanecem explícitos;
- nenhum formato derivado é criado ou atualizado.

**Verificação:** comparação com código, DTOs, testes, properties, Compose e scripts versionados;
links Markdown e `git diff --check`. Por ser documentação exclusiva, não executar Maven ou
checkpoint Sonar nesta task.

**Dependências:** Task 9.4.

**Tamanho estimado:** S.

#### Task 9.6 — Documentar limpeza completa dos ambientes locais da PoC

**Descrição:** complementar o guia operacional e a seção da PoC no README com um encerramento
destrutivo, explícito e limitado aos recursos da PoC, permitindo reconstruir Compose e kind sem
estado persistido anterior.

**Critérios de aceitação:**

- distinguir o encerramento comum, que preserva o volume documental, da limpeza completa;
- remover containers, rede e volume do projeto Compose `simtr-hub-poc`, o cluster kind
  `simtr-hub-poc`, sua imagem local e, opcionalmente, os artefatos Maven;
- documentar comandos de inspeção que confirmem a ausência desses recursos e os comandos para
  reconstruir Compose ou Kubernetes;
- preservar arquivos `.env` e recursos Docker alheios à PoC, com advertência explícita contra
  `docker system prune --all --volumes` como procedimento normal.

**Verificação:** comparação com `compose-poc.yml`, `validar-poc-kubernetes.ps1`, manifests
`k8s/poc`, links Markdown e `git diff --check`. Por ser documentação exclusiva, não executar Maven
ou checkpoint Sonar nesta task.

**Dependências:** Task 9.5.

**Tamanho estimado:** S.

#### Task 9.7 — Separar retenção e descarte de CouchDB e Valkey

**Descrição:** refinar o encerramento operacional no guia e no README para distinguir a retenção
dos documentos no CouchDB da retenção dos checkpoints técnicos no Redis/Valkey, considerando os
limites reais de persistência dos ambientes Compose e kind.

**Critérios de aceitação:**

- explicar que CouchDB usa volume/PVC persistente e Valkey não possui volume nem persistência
  própria na PoC;
- documentar como preservar os dois backends durante restart somente da aplicação;
- documentar como encerrar preservando documentos do CouchDB, mas descartando checkpoints do
  Valkey, e como limpar todo o estado;
- disponibilizar comandos separados para limpar somente Valkey em Compose e Kubernetes;
- advertir que limpar apenas um backend pode deixar documentos ou checkpoints órfãos e não é uma
  prova válida de retomada de instâncias em andamento.

**Verificação:** comparação com `compose-poc.yml`, `k8s/poc/couchdb.yaml`,
`k8s/poc/valkey.yaml`, comandos documentados, links Markdown e `git diff --check`. Por ser
documentação exclusiva, não executar Maven ou checkpoint Sonar nesta task.

**Dependências:** Task 9.6.

**Tamanho estimado:** S.

#### Task 9.8 — Explicitar a remoção total de Compose e kind

**Descrição:** tornar inequívoco no guia que Docker Compose e o cluster Kubernetes kind são
ambientes independentes e que a remoção total exige executar os comandos de limpeza dos dois.

**Critérios de aceitação:**

- explicar que `docker compose down` não remove o container `simtr-hub-poc-control-plane`;
- explicar que `down` sem `--volumes` preserva o volume do CouchDB do Compose;
- organizar a remoção total em passos separados para Compose, kind, imagem local e artefatos
  opcionais;
- registrar o efeito e o limite de cada comando e disponibilizar verificações após a limpeza;
- manter a limpeza restrita aos recursos da PoC, sem recomendar comandos Docker globais.

**Verificação:** comparação com `compose-poc.yml`, `validar-poc-kubernetes.ps1`, nomes observados
no Docker e `git diff --check`. Por ser documentação exclusiva, não executar Maven ou checkpoint
Sonar nesta task.

**Dependências:** Task 9.7.

**Tamanho estimado:** S.

#### Task 9.9 — Documentar a inspeção dos dados persistidos no CouchDB

**Descrição:** acrescentar ao guia um roteiro PowerShell para consultar o banco CouchDB da PoC,
correlacionar os documentos com a análise executada e validar projeção, fatos e cursor do feed.

**Critérios de aceitação:**

- usar as credenciais já injetadas no container/pod, sem copiá-las para argumentos, saída ou
  documentação;
- oferecer comandos equivalentes para o ambiente Compose e o cluster kind;
- listar os documentos do database configurado e filtrá-los por `correlationId`;
- validar identidades, status e referências da projeção usando a `instanceId` observada na API;
- explicar os tipos documentais esperados em cada estágio do fluxo e consultar o cursor local
  `_local/simtr-flow-revisao-v1` separadamente;
- alertar que a inspeção de payload completo deve usar somente dados sintéticos e não deve ser
  anexada a logs ou commits.

**Verificação:** comparação com `compose-poc.yml`, manifests `k8s/poc`,
`DocumentoAnaliseConformidadeStore`, `IdsDocumentoAnaliseConformidade`,
`CouchDbChangesHttpClient`, comandos PowerShell e `git diff --check`. Por ser documentação
exclusiva, não executar Maven ou checkpoint Sonar nesta task.

**Dependências:** Task 9.8.

**Tamanho estimado:** S.

### Checkpoint CF — Aceitação técnica e encerramento

- apresentar diff, comandos, testes, dependency tree, demonstração HITL e resultado
  Sonar;
- apresentar recuperação por restart, replay idempotente, teste cross-pod, backends,
  dependências operacionais, Ollama local e logs `%poc`;
- se houver situação Sonar `NON_COMPLIANT`, pedir ao usuário
  `Reprovar`, `AceitarExcepcionalmente` ou `ContinuarAjustes` e registrar pelo script;
- somente o usuário registra aceitação, exceção ou encerramento.

## Estratégia de testes

| Camada/capacidade | Teste obrigatório |
|---|---|
| Domínio | pareceres, cobertura, duplicidade, confiança, nomes, revisão completa |
| Contrato documental | mesma semântica para CouchDB e Cosmos, sem tipo nativo na porta |
| CouchDB | documentos, snapshots, `_rev`, consulta, concorrência, conflito e revisão idempotente |
| Cosmos DB for NoSQL | itens, partição, `_etag`, consulta, concorrência, conflito e revisão idempotente |
| Redis/Valkey | checkpoint mínimo, ausência de payload negocial e restauração |
| Casos de uso | iniciar/consultar/revisar por portas falsas |
| Checklist | `Uni` item/nulo/falha, sem bloqueio e sem retry duplicado |
| Agente | prompt, structured output, mapeamento, normalização e fallback |
| FT | exceções retentáveis/não retentáveis, tentativas, circuito e fallback |
| Flow | ordem de etapas, pausa real, retomada e contexto preservado |
| Entrega | `_changes`/Change Feed, cursor/leases, replay, CloudEvent referencial, correlação e documento inválido |
| REST | identidades, JSON, `Location`, 202/400/404/409/422/503 e contrato de erro |
| E2E falso | POST -> polling -> PUT -> conclusão |
| Concorrência | duas instâncias, duas revisões/consumers e nenhuma conclusão cruzada |
| Restart | processamento, espera, revisão persistida e resultado |
| Multipod | duas réplicas, Leases, cross-routing e failover |
| Arquitetura | imports, ownership e confinamento de DTOs |
| Observabilidade | eventos/spans/atributos e ausência de payload sensível |
| UI | estados, timer, cinco valores de identidade, campos editáveis e erros |
| Ollama real | opt-in, fora da suíte padrão |

Meta de cobertura do checkpoint Sonar: pelo menos 85%; duplicação: no máximo 5%.

## Comandos de validação por incremento

| Momento | Comandos/evidências |
|---|---|
| Pré-implementação | script de baseline conforme escolha humana; effective POM; dependency tree/spike |
| Incremento 1 | `mvn -q dependency:tree`, `mvn -q -DskipTests compile`, bootstrap, `mvn -q test`, Sonar |
| Incremento 2 | testes CloudEvent/Messaging, busca negativa por connector/Kafka, suíte, Sonar |
| Incremento 3 | testes domínio/store/API/contrato/ArchUnit, suíte, Sonar |
| Incremento 4 | testes Flow/checklist/Uni e ArchUnit, suíte, Sonar |
| Incremento 5 | testes agente/FT/fallback e workflow, suíte, Sonar |
| Incremento 6 | testes HITL/E2E/correlação/concorrência/canais, suíte, Sonar |
| Incremento 7 | contrato CouchDB/Cosmos, Redis, restart, replay, E2E, suíte e Sonar |
| Incremento 8 | containers, manifests, Leases, duas réplicas, failover e Sonar |
| Incremento 9 | UI/observabilidade/ArchUnit/docs, `mvn -q test`, `mvn -q verify`, Sonar final |

O checkpoint Sonar ocorre ao final de cada incremento executável coerente, não a cada
edição isolada. Se o baseline autorizado for exclusivamente offline, executar os
testes locais e registrar que o estado Sonar atual permanece `UNVERIFIED`.

## SonarQube

- fonte do baseline: **SonarQube Docker local**, escolhida pelo usuário;
- pacote autorizado: **nenhum**, pois não existe diretório `sonar/`;
- baseline inicializado em 2026-07-24 com servidor `UP`, build e análise completos;
- checkpoint esperado: antes da primeira alteração executável e depois de cada
  incremento coerente que mude o fingerprint;
- qualquer violação será apresentada como `NON_COMPLIANT`; o agente não infere
  reprovação nem aceitação excepcional.

## Riscos e controles

| Risco | Impacto | Controle |
|---|---|---|
| BOM Flow ausente/incompatível com 3.33.2.1 | alto | spike fail-fast, C1 e nenhum upgrade automático |
| Versões divergentes de LangChain4j | alto | BOMs companion, dependency tree e proibição de pin sem evidência |
| API Flow/Agentic diferente da documentação corrente | alto | compilar cada construção mínima antes do fluxo completo |
| Canal sem connector sem cadeia completa | alto | produtor e consumidor internos para ambos os canais e teste normal sem in-memory connector |
| Bloqueio do event loop ao consumir `Uni` | alto | ponte assíncrona e busca/teste contra `await` |
| Retry multiplicado sobre checklist | médio | reutilizar somente a política existente |
| Duração máxima excessiva com retry Ollama | médio | timeout provider/FT distintos, limites testados e documentação |
| Prompt injection | alto | system prompt, projeção mínima, structured output e validação Java |
| Vazamento de texto/prompt/resposta em logs | alto | logs integrais só em `%poc`, dados sintéticos e contratos negativos |
| Saída alucinada ou incompleta | alto | validador determinístico, fallback e revisão humana obrigatória |
| Correlação de revisão com instância errada | alto | `flowinstanceid` obrigatório e testes cruzados |
| Revisões simultâneas ou repetidas | alto | ID determinístico, `_rev`/`_etag`, hash e validação idempotente no workflow |
| Perda de dado de negócio em restart | alto | persistência durável e teste de restart por backend |
| DES divergir semanticamente de PRD | alto | porta neutra, contrato compartilhado, testes Cosmos herméticos locais e gate Cosmos real antes da promoção |
| Configuração iniciar com backend incorreto | alto | seleção explícita fora de dev/test e falha rápida por configuração ausente |
| Credencial Cosmos configurada por chave/connection string ou com privilégio excessivo | alto | `DefaultAzureCredential`, Managed/Workload Identity, RBAC de plano de dados mínimo e testes negativos |
| Checkpoint conter payload negocial | alto | contexto referencial e inspeção serializada |
| Cursor/lease do feed perdido ou mudança repetida | alto | retomada persistida, replay e duas camadas de idempotência |
| Documento inválido bloquear o feed | alto | validação, registro observável e avanço controlado |
| Redis/Valkey indisponível | alto | health/readiness, falha sanitizada e teste independente do backend documental |
| Roteamento cross-pod não funcionar sem broker | alto | spike e prova obrigatória; ADR permanece `Proposto` se falhar |
| Lease causar instância órfã em rollout | alto | readiness, identidade estável e teste de failover |
| Credenciais persistidas no repositório/log | alto | secrets/variáveis e testes negativos |
| Payload grande esgotar contexto/recursos | médio | limite de entrada aprovado em C2 e `num-ctx=2048` |
| Contrato REST divergir da convenção atual | médio | C2 e contract tests com erro público existente |
| Novo ADR/consolidado descrever intenção como estado | médio | ADR começa `Proposto`; consolidado só muda após implementação |
| Teste depender de Ollama local | médio | porta falsa na suíte padrão e teste real opt-in |
| Cobertura abaixo de 85% ou duplicação acima de 5% | médio | testes por incremento e decisão humana em `NON_COMPLIANT` |

## Decisões que precisam ser confirmadas

1. GO para iniciar qualquer alteração executável;
2. fonte do baseline Sonar e eventual pacote offline;
3. combinação efetiva de BOMs e APIs em C1;
4. aceitação, manutenção como `Proposto` ou rejeição do ADR-0009 no checkpoint CA;
5. contrato público, limite de texto e campos imutáveis em C2;
6. política de segurança do texto/prompt e logging `%poc`;
7. valores finais de FT e telemetria em C3;
8. eventual fallback de `@SequenceAgent` para um único `@RegisterAiService`;
9. C4 para ADR-0010, Redis/Valkey, identidades, contexto referencial e Kubernetes;
10. C5 para porta neutra, CouchDB/DES, Cosmos/PRD, feeds e gate de promoção;
11. C6 para assincronia da porta, autenticação Cosmos e ambientes locais;
12. valores finais de cursor/leases/reconexão/health/readiness e telemetria;
13. decisão arquitetural nova se o teste cross-pod falhar;
14. mudança de ADR-0010 para `Aceito` e ADR-0009 para `Substituído` somente após
    evidência e decisão humana;
15. decisão sobre qualquer `NON_COMPLIANT`;
16. aceitação e encerramento final.

## Cobertura da saída exigida pela especificação

1. resumo do repositório: `Contexto verificado`;
2. classes reutilizadas: `Contexto verificado` e `Modelo, fronteiras e responsabilidades`;
3. incompatibilidades: `Incompatibilidades, divergências e premissas`;
4. decisão de versões: `Decisão de dependências e versões`;
5. POM: `Decisão de dependências e versões` e Task 1.1;
6. properties: `Configuração planejada`;
7. arquivos a criar: `Arquivos prováveis e responsabilidades`;
8. arquivos a modificar: mesma seção;
9. responsabilidade por arquivo: mesma seção;
10. fluxo de dados: `Fluxo de dados`;
11. canais internos: `Desenho dos canais e CloudEvents`;
12. CloudEvent/correlação: mesma seção;
13. integração com `Uni`: seção própria;
14. agente: `Estratégia do agente, prompt e saída`;
15. prompt/saída: mesma seção;
16. Fault Tolerance: seção própria;
17. Human-in-the-Loop: seção própria;
18. polling: `Contrato REST e polling propostos`;
19. testes: `Estratégia de testes`;
20. comandos: `Comandos de validação por incremento`;
21. riscos: `Riscos e controles`;
22. decisões: `Decisões que precisam ser confirmadas`;
23. ordem: `Ordem de implementação e tarefas`.

## GO necessário

Nenhuma alteração de produção começa antes do GO humano registrado no `todo.md`.
Após o GO, a execução começa por Task 0.5, uma única pendência por vez.
