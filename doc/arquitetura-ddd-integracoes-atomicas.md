# Arquitetura atual do simtr-hub

## Como usar este documento

- **Status:** aceito
- **Última consolidação:** 2026-08-09
- **Objetivo:** explicar rapidamente a arquitetura implementada e as restrições que novas features
  devem respeitar.

Leia este consolidado antes de planejar uma feature. Em seguida, consulte o
[índice de ADRs](adr/README.md): a descrição do índice deve bastar para identificar quais decisões
se aplicam. Leia o ADR completo somente quando ele for aplicável à mudança ou quando houver dúvida.

O código, os contratos executáveis e os testes são a fonte de verdade do comportamento atual. Se
este documento divergir deles, registre a divergência no plano da feature antes de propor uma
correção.

## Visão do sistema

O `simtr-hub` é um monólito modular Quarkus organizado por domínios de negócio. Ele expõe oito
capacidades atômicas por REST e integra cada uma ao MTR ou ao simulador por adapters de saída
intercambiáveis. Além delas, a PoC de análise de conformidade expõe três endpoints sobre estado
documental, exige um identificador negocial do documento, gera uma correlação estável e inicia um
workflow que consulta e congela o checklist, executa um agente sequencial no Ollama local e produz
um resultado preliminar validado.

```text
cliente HTTP
    -> adapter REST de entrada
        -> porta de entrada
            -> caso de uso atômico
                -> porta de saída
                    -> adapter selecionado
                        |-- MTR
                        `-- simulador
```

O caso de uso não conhece Resource, REST Client, URL, DTO MTR, fixture nem o mecanismo CDI que
seleciona o adapter. Não existem atualmente endpoint único de pré-validação, MCP Server ou
comunicação distribuída entre os domínios. O runtime do Quarkus Flow, sua ponte de Messaging
interna e o `AnaliseConformidadeFlow` já estão presentes para a PoC. O workflow executável alcança
o resultado preliminar do agente ou do fallback técnico, pausa para a revisão humana correlacionada
e retoma até a conclusão.

## Domínios e capacidades implementadas

| Domínio | Responsabilidade | Capacidades atuais |
|---|---|---|
| `arvoredocumento` | Dados parametrizados usados por uma futura árvore documental | `ConsultarProcessoParametrizado` |
| `conformidade` | Consulta de checklist e PoC de análise/revisão humana | `ConsultarChecklist`; modelos, validação determinística, projeção documental, API e workflow HITL com checkpoint Redis/Valkey até o resultado final revisado |
| `dossieproduto` | Operações atômicas do ciclo de vida do dossiê no MTR | `CriarDossieProduto`, `AtualizarFormularioDossieProduto`, `IncluirDocumentoDossieProduto`, `RegistrarValidacaoNegocialDossieProduto`, `IniciarOuAvancarWorkflowDossieProduto` |
| `gestaodocumento` | Obtenção de credencial para o container documental | `ObterCredencialContainer` |

`parametrizacao` é o nome de um sistema/contrato upstream, não um domínio interno compartilhado.
As consultas de processo e checklist pertencem a consumidores diferentes e mantêm modelos e
mapeamentos próprios.

`prevalidacao` é um domínio futuro. Nenhum fluxo, estado ou aggregate deve ser inventado antes de
existirem requisitos, contratos e autorização próprios.

## API pública atual

| Método | Path público |
|---|---|
| `GET` | `/simtr-hub/v1/processo/identificador-negocial/{identificador}` |
| `GET` | `/simtr-hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}` |
| `POST` | `/simtr-hub/v1/dossie-produto` |
| `PATCH` | `/simtr-hub/v1/dossie-produto/{id}/formulario` |
| `POST` | `/simtr-hub/v1/dossie-produto/{id}/documento` |
| `PATCH` | `/simtr-hub/v1/dossie-produto/{id}/validacao-negocial` |
| `POST` | `/simtr-hub/v1/dossie-produto/{id}/workflow` |
| `POST` | `/simtr-hub/v1/storage/container/credencial` |
| `POST` | `/simtr-hub/v1/conformidade/analises` |
| `GET` | `/simtr-hub/v1/conformidade/analises/{instanceId}` |
| `PUT` | `/simtr-hub/v1/conformidade/analises/{instanceId}/revisao` |

Cinco operações descritas na especificação de pré-validação ainda não existem no Hub:

- alterar garantia do dossiê;
- alterar produto do dossiê;
- capturar dossiê;
- cancelar dossiê;
- consultar dossiê por identificador.

A existência dessas operações no MTR não autoriza endpoint, capacidade, adapter ou simulador no
Hub. Cada implementação futura exige feature, contrato, plano e GO próprios.

## Organização interna

O código usa **package by domain** e arquitetura hexagonal pragmática. Pastas e abstrações só são
criadas quando existe uma capacidade e um consumidor reais.

```text
<dominio>/
|-- dominio/
|   |-- modelo/
|   `-- erro/
|-- aplicacao/
|   |-- porta/
|   |   |-- entrada/
|   |   `-- saida/
|   `-- casodeuso/
`-- adaptador/
    |-- entrada/rest/v1/
    `-- saida/
        |-- mtr/
        `-- simulador/
```

Quarkus, Jakarta, MicroProfile, Mutiny, Jackson e OpenTelemetry podem apoiar qualquer camada. O
guardrail protege responsabilidades e direção das dependências, não uma pureza artificial de
framework.

### Regras de dependência

1. `dominio` não depende de aplicação, Resources, adapters ou contratos de borda.
2. `aplicacao` não importa adapters, DTOs de borda, REST Clients ou outro domínio.
3. Adapter de entrada traduz seu contrato para uma porta de entrada.
4. Adapter de saída implementa uma porta de saída e traduz tipos internos para sua borda.
5. REST, MTR, simulador e um eventual MCP possuem DTOs independentes.
6. Mappers não convertem diretamente DTO de uma borda em DTO de outra.
7. `arquitetura` não contém regra, modelo ou erro específico de negócio.
8. Colaboração entre domínios atravessa uma porta do consumidor e uma camada anticorrupção.
9. A API pública de aplicação de um domínio contém somente portas de entrada e os tipos
   semânticos referenciados por elas.

ArchUnit protege essas fronteiras. Uma feature que precise alterar uma regra deve explicar a
necessidade no plano e obter checkpoint humano de arquitetura.

## Portas, casos de uso e colaboração

- cada capacidade atômica possui sua porta de entrada;
- casos de uso usam linguagem do negócio e tipos internos;
- portas de saída representam a necessidade do consumidor, não uma API genérica do fornecedor;
- um adapter MTR pode implementar várias portas pequenas do mesmo domínio;
- não existe `Service` genérico que exponha operações de todos os contextos.

### PoC de análise de conformidade implementada

O estado implementado e comprovado até a Task 9.2 inclui:

- canais internos `flow-in` e `flow-out` com CloudEvent v1, sem connector ou broker;
- shim de Messaging confinado ao adapter e à versão Flow `0.10.2`;
- modelos imutáveis de solicitação, resultado, revisão e visão da análise;
- validação determinística de cobertura, identidade, nomes e confiança;
- porta documental neutra e seleção explícita de CouchDB em `dev/test` ou Cosmos DB for NoSQL
  nos demais ambientes;
- CouchDB como sistema de registro local da projeção e dos documentos de solicitação, checklist,
  resultados, revisão, falha e emissões referenciais;
- adapter Cosmos coberto por contrato determinístico com SDK mockado; a integração real permanece
  gate externo obrigatório antes de promoção para PRD;
- reserva interna compare-and-set para aceitar somente uma revisão por instância;
- três portas e casos de uso de entrada para iniciar, consultar e revisar;
- adapter REST v1 com DTOs próprios, JSON camelCase e `Location` relativo;
- `identificadorDocumento` obrigatório no POST e `correlationId` gerado pelo Hub;
- respostas do POST e GET com `correlationId`, `instanceId`, `identificadorDocumento`,
  `identificadorChecklist` e `versaoChecklist`, mantendo `instanceId` nos paths;
- identidades preservadas pela visão imutável em todas as transições e conferidas contra os
  resultados do checklist antes da revisão, sem campos de identidade no contrato do PUT;
- validação de borda do texto em até 20.000 caracteres e da lista completa de revisão;
- tradução de falhas para o contrato `ErroPadraoDto` com
  `400`/`404`/`409`/`422`/`503`, sem stack trace ou detalhe interno;
- `AnaliseConformidadeFlow` registrado como `WorkflowDefinition`, com o identificador nativo da
  instância usado também pela projeção e pelo contrato REST;
- início assíncrono sem espera bloqueante e etapa que acessa exclusivamente a porta
  `ConsultarChecklist`;
- contexto do workflow limitado às cinco identidades e à referência documental do checklist;
  item nulo, checklist vazio e falha técnica encerram o workflow e transitam a projeção para
  `FALHOU`;
- etapas do workflow recarregam solicitação, checklist, resultado preliminar e revisão somente pela
  porta documental e validam referência, SHA-256 e versão de schema antes de usar o conteúdo;
- porta `AnalisarTextoComChecklist` e task `agent(...)` no workflow, com o identificador raiz da
  instância usado como `memoryId`;
- capacidade Agentic sequencial `AplicadorChecklistAgent -> RevisorCoberturaAgent`, executada com
  `llama3.2:3b` no Ollama local e retorno em records exclusivos do adapter;
- projeção JSON mínima do checklist, cláusulas contra prompt injection e validação Java que rejeita
  identificadores extras/duplicados e completa item ausente como `NAO_ANALISADO`;
- timeout HTTP de 60 s e política MicroProfile FT no adapter Ollama, com timeout de 65 s, até duas
  novas tentativas, circuit breaker e fallback completo para revisão humana;
- retry interno do provider desabilitado por `ModelBuilderCustomizer`, mantendo a política FT como
  fonte única de repetição;
- tracing textual integral do Flow desabilitado por padrão; spans próprios do workflow e do agente
  carregam somente identificadores, versão, quantidade, modelo, origem e estado;
- eventos internos carregam somente `documentoRef`, `hashConteudo` e `versaoSchema`; o adapter de
  Messaging complementa `source`, `time` e correlação sem inserir payload negocial;
- `EventPublisher` documental persiste as emissões referenciais antes da confirmação do
  `flow-out`;
- feed nativo selecionado pelo backend: `_changes` com cursor persistido no CouchDB ou
  `ChangeFeedProcessor` com container de leases no Cosmos, ambos entregando CloudEvents
  referenciais validados ao `EventConsumer` do Flow;
- na reserva da revisão, a projeção persiste primeiro `revisaoRef` e o fato imutável que dispara o
  feed é criado depois; assim, o workflow só recebe uma referência já carregável, e uma repetição
  pode completar o fato caso haja falha entre as duas escritas;
- `quarkus-flow-redis` persiste checkpoints técnicos no Redis/Valkey; testes com Valkey comprovam
  que texto, checklist, resultado e revisão completos não entram nos hashes e que uma instância
  `WAITING` pode ser reconstruída após descarte do estado volátil;
- uma prova automatizada com duas invocações Maven/JVM separadas e os mesmos CouchDB e Valkey
  confirmou que a segunda JVM restaura uma instância `WAITING`, recebe a revisão e a conclui como
  `COMPLETED`; essa evidência não representa execução simultânea nem failover entre pods;
- um ambiente kind local executa duas réplicas da aplicação com CouchDB em `StatefulSet`/PVC e
  Valkey compartilhado; cada réplica adquire uma Lease de membro do pool
  `simtr-hub-conformidade`, readiness exige Lease, o controle sem RBAC permanece fora do Service e
  o rolling restart preserva os nomes das Leases;
- a prova cross-pod inicia a análise diretamente no owner identificado pelo `holderIdentity`,
  envia a revisão por outra réplica e substitui o owner tanto antes quanto imediatamente depois do
  aceite da revisão; a mesma Lease é assumida pelo pod substituto, o checkpoint e os dados
  compartilhados sobrevivem, as duas réplicas consultam `CONCLUIDA` e cada correlação produz um
  único fato de conclusão;
- página estática em `/poc-conformidade/`, sem dependência de framework ou CDN, com polling único
  a cada 1.500 ms, cinco identidades somente leitura e revisão limitada a parecer, justificativa,
  evidência e observação geral, sem `localStorage` ou `sessionStorage`;
- spans e logs estruturados da persistência documental e dos feeds CouchDB/Cosmos limitados a IDs,
  backend, operação, resultado, replay, cursor/lease e contexto de trace; texto, prompt, resposta,
  revisão, evidência, credencial e documento não entram nesses sinais;
- readiness que distingue backend documental, Redis/Valkey e Lease Kubernetes, preservando o
  estado específico de cada dependência;
- guardrails ArchUnit que impedem o núcleo de conformidade de depender de DTOs Ollama, SDK Cosmos
  ou CloudEvent, além das regras gerais de direção das dependências;
- pausa real no `listen`, publicação assíncrona da revisão pelo `flow-in` e retomada somente da
  instância correlacionada;
- dupla validação da revisão, antes da publicação e dentro do workflow retomado;
- evento final e projeção `CONCLUIDA` com origem `REVISAO_HUMANA`.

A projeção possui os estados `EM_PROCESSAMENTO`, `AGUARDANDO_REVISAO`, `CONCLUIDA` e `FALHOU`.
Ela pertence ao backend documental selecionado, não substitui o estado do Flow e preserva o
conteúdo de negócio fora do Redis. O POST cria a projeção `EM_PROCESSAMENTO` com as cinco
identidades e inicia o Flow sem
aguardar a consulta; o resultado preliminar publicado em `flow-out` projeta
`AGUARDANDO_REVISAO`; o PUT localiza por `instanceId`, valida as identidades persistidas, reserva
atomicamente e publica somente sua referência em `flow-in`; e o workflow correlacionado recarrega
o documento, retoma e projeta `CONCLUIDA` sem trocar as identidades. A espera do Flow possui
checkpoint Redis/Valkey e a entrega usa o feed documental nativo, com cursor ou lease persistente
conforme o backend. A página acompanha essa projeção sem criar estado adicional no navegador.

### Evolução durável implementada, com aceitação pendente

O ADR-0010 permanece `Proposto`, embora os checkpoints C4 a C9 já tenham autorizado sua
implementação incremental. As Tasks 7.3 a 7.6 implementaram a porta documental neutra, a seleção
por ambiente, CouchDB local com Compose Dev Services, adapter Cosmos contratualmente mockado,
`EventPublisher` referencial, checkpoint Redis/Valkey mínimo, feed documental nativo e prova de
restart sequencial entre JVMs. A advertência de configuração desconhecida para
`quarkus.flow.persistence.auto-restore` ainda é emitida pela versão Flow `0.10.2`, embora a
restauração padrão tenha sido comprovada funcional. O empacotamento local, as duas réplicas e a
prova cross-pod/failover foram concluídos nas Tasks 8.1 a 8.3; a página e os guardrails operacionais
foram concluídos nas Tasks 9.1 e 9.2. Permanecem pendentes o gate Cosmos real pré-PRD e a decisão
humana posterior; por isso o ADR ainda não é tratado como aceito e o ADR-0009 não foi marcado como
substituído.

C6 estabelece Quarkus reativo sempre que a API suportar:

- escritas da porta documental retornam `Uni<Void>`;
- a leitura documental retorna `Uni<Optional<VisaoAnaliseConformidade>>`;
- `IniciarAnaliseConformidade` e `ConsultarAnaliseConformidade` retornam
  `Uni<VisaoAnaliseConformidade>`;
- recursos REST e projeção interna propagam `Uni`;
- callbacks do Flow retornam `Uni` diretamente.

O Quarkus Flow `0.10.2` registra `Uni2CompletableFuture` como conversor do runtime. Portanto, a
aplicação não converte manualmente `Uni` para `CompletionStage` nessa borda; uma conversão explícita
só é aceita quando uma API externa comprovadamente não suportar `Uni`. `await`, `join` e bloqueio do
event loop permanecem proibidos.

A mesma evolução prevê CouchDB `3.5.2` iniciado automaticamente por
`compose-devservices.yml` no `quarkus:dev`, Azure Cosmos DB for NoSQL em PRD autenticado por
Microsoft Entra ID com Managed Identity ou Workload Identity e RBAC de plano de dados de menor
privilégio, sem chave ou connection string. O ambiente kind local com duas réplicas, Leases,
readiness e rolling restart foi validado; a retomada cross-pod e o failover do pod owner também
foram comprovados com CouchDB e Valkey compartilhados continuamente disponíveis. Essa prova local
não amplia o escopo para perda, reinício ou alta disponibilidade dos próprios backends.

Um futuro orquestrador do mesmo domínio pode compor portas de entrada atômicas. Ao atravessar um
domínio, usa uma porta de saída do consumidor e uma camada anticorrupção. Dentro do mesmo processo,
não chama endpoints REST locais.

## Contratos das bordas

### REST público

- DTOs pertencem ao adapter REST do domínio e da operação;
- paths, verbos, status, JSON e validações são contratos observáveis;
- OpenAPI é gerado pelo Quarkus a partir do código;
- o contrato técnico compartilhado de erro REST em `arquitetura.excecao.dto` é uma exceção
  arquitetural explícita e não pode vazar para domínio, aplicação, MTR, simulador ou MCP.

### MTR

- DTOs, mappers, REST Clients e annotations de fault tolerance pertencem ao adapter MTR;
- contratos são separados por versão e operação quando evoluem independentemente;
- falhas externas são traduzidas para falhas internas somente depois da política de fault
  tolerance.

### Simulador

- implementa as mesmas portas de saída do adapter MTR;
- usa DTO e mapper próprios para ler fixtures;
- não reutiliza DTO REST ou MTR;
- seleção MTR/simulador usa qualifiers ou producer CDI explícitos.

### MCP futuro

MCP pode ser uma nova borda de entrada para portas existentes. Não é domínio, regra de negócio ou
atalho para Resources REST/adapters de saída. DTOs, schemas, autorização, transporte e erros são
exclusivos dessa borda. Nenhum componente MCP está implementado ou autorizado apenas por estar
descrito aqui.

## Erros

- exceções HTTP, MCP e tipos de protocolo não atravessam portas;
- cada domínio classifica falhas relevantes para seus casos de uso;
- o adapter MTR preserva dados necessários à resposta pública sem transportar seu DTO até REST;
- o adapter REST traduz falhas internas para o status e corpo públicos;
- validação e desserialização anteriores ao Resource permanecem em mappers técnicos REST;
- stack, URL interna, token, credencial e estado de circuit breaker não são dados públicos.

## Assincronicidade e chamadas bloqueantes

`Uni` representa operações assíncronas com zero ou um resultado. Casos de uso não chamam `await`,
não bloqueiam event loop e não criam threads. Adapters bloqueantes deslocam o trabalho para worker
thread sem expor esse detalhe ao domínio.

Na evolução da conformidade, a preferência é manter `Uni` ponta a ponta nas portas, casos de uso,
REST, projeção e callbacks Flow. Converter para outro tipo reativo é uma decisão da borda de
integração e exige evidência de que a API chamada não aceita `Uni`.

## Fault tolerance e idempotência

Timeout, retry, circuit breaker e classificação de exceções pertencem ao adapter da integração:
adapters MTR mantêm suas políticas atuais e o adapter Ollama possui a política específica aprovada
para a PoC. Essas políticas não pertencem ao domínio, ao workflow nem ao simulador.

Criação de dossiê, inclusão de documento e avanço de workflow são operações mutáveis. Antes de um
workflow, orquestrador ou agente repetir essas operações, deve existir evidência de idempotência do
MTR ou uma estratégia/chave idempotente aprovada. Sem essa evidência, a composição mutável fica
bloqueada.

## Observabilidade e segurança

- spans, eventos de log e atributos existentes são comportamento observável;
- renomes Java não podem alterar silenciosamente nomes derivados por reflexão;
- novas entradas preservam correlação até o MTR e identificam sua origem;
- tokens, credenciais, argumentos sensíveis, URLs internas e payloads protegidos não aparecem em
  respostas, logs, traces, relatórios ou memória de conversa;
- a PoC mantém request/response completos do LangChain4j desligados por padrão e só admite o
  perfil `%poc` com dados sintéticos; o tracing textual do Flow também permanece desligado;
- os spans `simtr-hub.flow.conformidade.analise` e
  `simtr-hub.agent.conformidade.analisar` não registram texto, prompt, resposta, evidência,
  credencial, detalhe de erro ou stack;
- os spans `simtr-hub.persistencia.conformidade.documento` e
  `simtr-hub.feed.conformidade.documento`, bem como seus logs estruturados, usam apenas IDs,
  backend, operação, resultado, replay, cursor/lease e trace; a operação reativa mantém o span até
  terminar sem anexar payload ou exceção ao sinal;
- readiness identifica separadamente backend documental, Redis/Valkey e Lease Kubernetes para que
  a dependência impeditiva seja observável sem revelar configuração sensível;
- exposição de `ObterCredencialContainer` a agentes exige decisão de segurança própria.

## Estratégia de testes e evolução

Uma feature segue fatias verticais pequenas:

1. caracterizar o comportamento atual relevante;
2. escrever ou ajustar testes que provem a mudança pretendida;
3. implementar o menor incremento coerente;
4. executar testes focados;
5. executar suíte, build e checkpoint Sonar conforme o guia de agentes.

Conforme a mudança, os testes cobrem contrato HTTP/JSON, Jakarta Validation, mapeamentos, payload
MTR, simulador, erros, fault tolerance, configuração, observabilidade e regras ArchUnit. Mudanças de
contrato, arquitetura, segurança ou comportamento observável exigem checkpoint humano adicional.

## Restrições vigentes

- o Hub não faz upload para Azure Blob Storage;
- não mantém cache nem renova SAS;
- possui runtime Flow, feed documental nativo, API, agente Ollama e pausa/retomada HITL; a espera
  possui checkpoint Redis/Valkey restaurável e a entrega possui cursor CouchDB ou leases Cosmos,
  com restart sequencial entre duas JVMs, identidade por Lease e readiness de duas réplicas no
  kind comprovados; no ambiente kind, revisão por outra réplica e substituição do owner antes ou
  depois do aceite também foram comprovadas enquanto os backends permaneceram disponíveis;
- não possui MCP Server ou tools;
- o runtime usa a projeção documental do backend selecionado; CouchDB é real no ambiente local e
  Cosmos permanece mockado com gate real obrigatório antes de PRD;
- não calcula árvore documental; a análise de conformidade ponta a ponta está implementada apenas
  como PoC, com página, revisão humana obrigatória e as limitações operacionais documentadas no
  [README](../README.md#poc-de-conformidade-durável);
- não há validação real do adapter Cosmos, recuperação ou HA dos próprios CouchDB/Valkey, nem
  suporte produtivo inferido a partir das provas locais;
- não implementa os cinco endpoints ausentes listados acima.

Essas restrições descrevem o estado atual, não uma proibição permanente. Uma feature pode mudá-las
somente com requisitos explícitos, análise de impacto, plano, testes e GO humano.

## Decisões arquiteturais

Consulte [doc/adr/README.md](adr/README.md) para o resumo e a aplicabilidade de cada decisão. O
índice é parte da leitura inicial; o texto completo de um ADR é leitura sob demanda.
