# Checklist: PoC de conformidade com Quarkus Flow, Ollama e Human-in-the-Loop

## Estado

- **Branch:** `feature/poc-conformidade-flow-ollama-hitl`
- **Escopo:** concluir baseline HITL volátil e evoluir para persistência documental
  neutra, com CouchDB em DES, Azure Cosmos DB for NoSQL em PRD, Redis/Valkey para
  checkpoints e entrega referencial sem broker
- **Próximo item:** Task 9.3 — pendente, não iniciada
- **Especificação:** `doc/poc/especificacao-poc-conformidade-quarkus-flow-ollama-hitl-sem-broker.md`
- **Plano:** `tasks/features/poc-conformidade-flow-ollama-hitl/plan.md`
- **Baseline Sonar:** SonarQube Docker local, inicializado em 2026-07-24

## Checklist

- [x] 0.1 Confirmar intenção e critérios de sucesso;
- [x] 0.2 Ler arquitetura, índice e ADRs aplicáveis;
- [x] 0.3 Inspecionar código, contratos e testes relacionados;
- [x] 0.4 Registrar plano, riscos, checkpoints e rollback;
- [x] C0 Registrar GO humano antes da primeira alteração de produção;
- [x] 0.5 Verificar pacotes `sonar/`, pedir a fonte e inicializar o baseline autorizado;
- [x] 0.6 Resolver BOMs, versões, dependency tree e APIs em spike sem alterar produção;
- [x] C1 Aprovar arquitetura, versões, canais, estado volátil e configuração;
- [x] 1.1 Incremento 1 — integrar dependências/configuração e provar bootstrap;
- [x] 1.2 Executar testes e checkpoint Sonar do incremento 1;
- [x] CA Revisar e decidir o status do ADR arquitetural proposto;
- [x] 2.1 Incremento 2 — fechar canais internos e contrato CloudEvent;
- [x] 2.2 Executar testes e checkpoint Sonar do incremento 2;
- [x] C2 Aprovar contrato REST, segurança da entrada, campos imutáveis e UI;
- [x] 3.1 Incremento 3 — implementar domínio, validação e transições atômicas;
- [x] 3.2 Incremento 3 — expor POST, GET e PUT por portas de entrada;
- [x] 3.3 Executar testes e checkpoint Sonar do incremento 3;
- [x] 4.1 Incremento 4 — integrar Flow e `ConsultarChecklist` sem bloqueio;
- [x] 4.2 Executar testes e checkpoint Sonar do incremento 4;
- [x] C3 Aprovar agente, prompt, Fault Tolerance, logs e spans;
- [x] 5.1 Incremento 5 — implementar Agentic/Ollama, structured output e fallback;
- [x] 5.2 Executar testes e checkpoint Sonar do incremento 5;
- [x] 6.1 Incremento 6 — completar pausa, correlação e retomada HITL;
- [x] 6.2 Executar testes e registrar indisponibilidade do checkpoint Sonar do
  incremento 6;
- [x] C4 Aprovar arquitetura/contrato da ADR-0010 para implementação, mantendo-a
  `Proposto` até a prova multipod;
- [x] 7.1 Provar compatibilidade de CouchDB, Redis/Valkey, Flow persistence,
  `EventConsumer` e Durable Kubernetes;
- [x] 7.2 Evoluir contrato com `correlationId`, `identificadorDocumento`, checklist e
  versão;
- [x] C5 Aprovar persistência por ambiente: CouchDB em DES, Azure Cosmos DB for
  NoSQL em PRD, porta neutra, contrato compartilhado e gate Cosmos pré-promoção;
- [x] C6 Aprovar assincronia da porta documental, autenticação Cosmos por Entra ID,
  CouchDB automático no `quarkus:dev` e desenho do Kubernetes local;
- [x] C7 Aprovar `correlationid` como nome da extensão CloudEvent compatível com a
  especificação, preservando `correlationId` na API, nos documentos e nos modelos;
- [x] C8 Aprovar CouchDB como integração local e Cosmos validado por contrato
  determinístico/SDK mockado, mantendo o gate real antes da promoção para PRD;
- [x] 7.3 Persistir documentos de negócio e projeção pela porta neutra, com adapters
  CouchDB e Cosmos DB for NoSQL;
- [x] 7.4 Reduzir contexto e habilitar checkpoint Redis/Valkey;
- [x] 7.5 Completar `EventPublisher` + feed nativo do backend -> `EventConsumer` ->
  CloudEvent com idempotência;
- [x] 7.6 Executar suíte e checkpoint Sonar do incremento 7;
- [x] 8.1 Empacotar app, CouchDB, Redis/Valkey e Ollama para uma réplica;
- [x] 8.2 Configurar Kubernetes Leases, readiness e duas réplicas;
- [x] 8.3 Provar retomada cross-pod e failover;
- [x] 8.4 Executar suíte e checkpoint Sonar do incremento 8;
- [x] 9.1 Criar página estática com polling e cinco valores de identidade;
- [x] 9.2 Fechar observabilidade e guardrails arquiteturais;
- [ ] 9.3 Atualizar README, consolidado arquitetural e ADRs conforme estado comprovado;
- [ ] 9.4 Executar suíte, verify e checkpoint Sonar final;
- [ ] CF Apresentar evidências e solicitar aceitação/encerramento humano.

## Evidências de execução

### Task 1.1 — Compatibilidade e bootstrap

- RED: `mvn -q "-Dtest=FlowOllamaCompatibilidadeQuarkusTest" test` falhou porque
  Flow, Flow Messaging e LangChain4j Agentic ainda não existiam no classpath;
- GREEN: o mesmo teste iniciou Quarkus `3.33.2.1`, carregou os recursos Flow,
  Flow LangChain4j, Flow Messaging, LangChain4j Ollama e Messaging e terminou com
  código 0;
- o perfil de teste usa `base-url=http://localhost:1/`,
  `devservices.enabled=false` e `enable-integration=false`; portanto, o bootstrap
  passou sem Ollama, container ou acesso externo;
- `mvn -q -DskipTests compile` terminou com código 0;
- a dependency tree resolveu Flow `0.10.2`, Quarkus LangChain4j `1.11.2`,
  LangChain4j `1.16.2`, Agentic `1.16.2-beta26` e Quarkus Messaging `3.33.2.1`;
- a busca negativa não encontrou `quarkus-messaging-kafka` nem propriedade
  `mp.messaging.*.connector`;
- o único conflito omitido continua sendo `jackson-jq:1.6.1`, gerenciado para
  `1.6.2`; o bootstrap também emitiu avisos de reindexação Jandex desses artefatos,
  sem impedir compilação ou inicialização;
- ADR-0009 criado inicialmente como `Proposto`, indexado e posteriormente aceito
  no checkpoint CA.

### Task 1.2 — Suíte e checkpoint Sonar

- `mvn -q test` terminou com código 0 em 60,4 s;
- 82 relatórios Surefire registraram 325 testes, 0 falhas, 0 erros e 0 ignorados;
- `./validar-checkpoint-sonarqube.ps1` terminou com código 0 após executar
  `clean verify`, SonarScanner e aguardar o Compute Engine;
- estado técnico: `COMPLIANT`, com 219 issues atuais contra 219 no baseline,
  nenhuma issue nova e nenhuma issue `HIGH` ou `BLOCKER`;
- cobertura: 87,8%, acima da meta de 85%;
- duplicação: 3,7%, abaixo do limite de 5%;
- decisão humana Sonar: `NOT_REQUIRED`, pois não houve violação;
- próximo passo: checkpoint humano CA para revisar o ADR-0009 antes de qualquer
  implementação dos canais do incremento 2.

### Task 2.1 — Canais internos e contrato CloudEvent

- RED: `mvn -q "-Dtest=*CloudEvent*,*Messaging*" test` falhou na compilação
  porque codec, publisher, consumer, store e shims ainda não existiam;
- GREEN/REFACTOR:
  `mvn -q "-Dtest=*CloudEvent*,*Messaging*,ArchUnitProgressivoTest" test`
  terminou com código 0;
- 13 testes focados de CloudEvent/Messaging e 33 testes ArchUnit passaram sem
  falhas, erros ou ignorados;
- o codec produz CloudEvent v1 em JSON estruturado `byte[]`, com `source`
  estável, `time`, `application/json`, tipo versionado e `flowinstanceid`;
- o consumidor preserva `flowtaskid`, aceita os dois tipos de `flow-out`,
  rejeita tipo, correlação ou payload inválidos e só confirma depois do registro;
- o `@QuarkusTest` publicou de verdade em `flow-in`, publicou/consumiu em
  `flow-out` e registrou a projeção técnica em memória;
- os shims CDI confinam o acoplamento temporário ao Flow `0.10.2`; nenhum
  contrato REST ou modelo de domínio sujeito a C2 foi antecipado;
- a busca negativa em `pom.xml`, `src/main` e `src/test` não encontrou
  `quarkus-messaging-kafka`, `smallrye-kafka` nem atribuição `.connector=`;
- próximo passo: Task 2.2 para suíte completa e checkpoint Sonar do incremento 2.

### Task 2.2 — Suíte e checkpoint Sonar

- `mvn -q test` terminou com código 0 em 58,5 s;
- a suíte passou com os 325 testes já registrados no incremento 1 mais os 13
  testes do incremento 2, totalizando 338 testes sem falhas;
- `./validar-checkpoint-sonarqube.ps1` terminou com código 0 após `clean verify`,
  SonarScanner e Compute Engine;
- estado técnico: `NON_COMPLIANT`, com 220 issues atuais contra 219 no baseline
  e exatamente uma issue nova;
- não há issue nova `HIGH` ou `BLOCKER`;
- cobertura: 87,2%, acima da meta de 85%;
- duplicação: 3,6%, abaixo do limite de 5%;
- issue nova: `java:S5785`, severidade `MAJOR`, em
  `FlowOutCloudEventConsumerTest.java:71`, com orientação `Use assertNotNull
  instead.`;
- decisão humana Sonar: `ContinuarAjustes`, registrada pelo script;
- a asserção foi substituída por `assertNotNull` e o teste focado passou;
- o checkpoint repetido terminou com código 0 e estado técnico `COMPLIANT`:
  219 issues atuais contra 219 no baseline, nenhuma issue nova e nenhuma issue
  `HIGH` ou `BLOCKER`;
- métricas finais: cobertura de 87,2% e duplicação de 3,6%;
- decisão humana final não requerida pelo script, pois não restou violação;
- ao concluir a Task 2.2, o próximo passo passou a ser o checkpoint C2,
  posteriormente aprovado e registrado abaixo.

### Checkpoint C2 — Contrato público e segurança da entrada

- decisão humana explícita `registrar C2`, recebida em 2026-07-24;
- aprovados os três endpoints e a semântica 202/200/400/404/409/422/503
  descritos no plano, incluindo `Location` no início da análise;
- aprovado o limite máximo de 20.000 caracteres para o texto;
- aprovadas a lista completa na revisão, a imutabilidade dos campos de identidade
  e a confiança somente leitura;
- confirmada a manutenção da autenticação/autorização existente, sem ampliar a
  superfície de segurança;
- aprovada a página estática com polling, campos editáveis restritos, aviso de
  volatilidade e sem armazenamento adicional no navegador;
- próximo passo: Task 3.1, limitada a regras puras, validação e transições
  atômicas em memória.

### Task 3.1 — Domínio, validação e transições atômicas

- RED dos modelos: o teste focado falhou na compilação porque os modelos e o
  erro de domínio ainda não existiam;
- GREEN/REFACTOR dos modelos: foram implementados solicitação, apontamento,
  resultado, revisão, status e visão da análise como valores imutáveis, com
  cópias defensivas e validação do limite de 20.000 caracteres;
- RED do validador: o teste focado falhou na compilação porque a validação
  determinística ainda não existia;
- GREEN/REFACTOR do validador: cobertura exata do checklist, identidade e nomes
  imutáveis, confiança somente leitura, ordenação determinística e origem humana
  do resultado final passaram a ser invariantes de domínio;
- RED do armazenamento: o teste focado falhou na compilação porque a porta e o
  armazenamento em memória ainda não existiam;
- GREEN/REFACTOR do armazenamento: as transições
  `EM_PROCESSAMENTO -> AGUARDANDO_REVISAO -> CONCLUIDA` e a falha terminal foram
  implementadas por instância com `ConcurrentHashMap.compute`;
- o teste concorrente com duas revisões simultâneas confirmou exatamente uma
  reserva aceita e uma rejeição controlada por transição inválida;
- `mvn -q "-Dtest=*AnaliseConformidade*Test" test` terminou com código 0:
  19 testes focados, sem falhas, erros ou ignorados;
- `mvn -q "-Dtest=ArchUnitProgressivoTest" test` terminou com código 0:
  33 testes arquiteturais, sem falhas, erros ou ignorados;
- a busca negativa não encontrou imports de adaptadores ou frameworks no novo
  domínio e o consolidado arquitetural foi atualizado com o estado efetivamente
  implementado;
- a suíte completa e o checkpoint Sonar permanecem programados para a Task 3.3,
  após a fatia REST da Task 3.2;
- próximo passo: Task 3.2, limitada aos casos de uso e aos três endpoints
  aprovados no checkpoint C2.

### Task 3.2 — Portas de entrada e API REST

- RED dos casos de uso: o teste focado falhou na compilação porque as três portas
  e os casos de uso de iniciar, consultar e revisar ainda não existiam;
- GREEN dos casos de uso: o início cria apenas a projeção volátil
  `EM_PROCESSAMENTO`, a consulta exige instância existente e a revisão valida o
  resultado preliminar antes da reserva atômica;
- RED da borda REST: os testes demonstraram primeiro a ausência do Resource e,
  depois, a normalização indevida do `Location` para URL absoluta;
- GREEN/REFACTOR da borda REST: foram implementados POST, GET e PUT com JSON
  camelCase, `Location` relativo, DTOs exclusivos da borda e respostas
  `202`/`200`;
- os erros de domínio são traduzidos para `ErroPadraoDto` somente na borda REST:
  payload inválido `400`, instância ausente `404`, transição inválida `409`,
  revisão inconsistente `422` e indisponibilidade técnica sanitizada `503`;
- o texto possui validação redundante na borda e no domínio com limite de 20.000
  caracteres; o texto não é devolvido, persistido no store ou registrado em log;
- um RED de segurança comprovou que `apontamentos: [null]` produzia `500`; a
  validação de elemento da lista passou a rejeitá-lo com erro público `400`;
- identificadores, nomes e confiança permanecem obrigatórios na revisão; nomes e
  confiança são validados contra o resultado preliminar antes da reserva;
- os enums do contrato REST são próprios da borda e são traduzidos
  explicitamente pelo mapper, sem reutilizar os enums do domínio;
- `mvn -q "-Dtest=*AnaliseConformidade*Test,ArchUnitProgressivoTest" test`
  terminou com código 0: 36 testes focados de análise e 33 testes ArchUnit, sem
  falhas, erros ou ignorados;
- risco residual registrado: o contrato aprovado limita o texto, mas não define
  quantidade máxima de itens da revisão; nenhum limite público adicional foi
  inventado sem novo checkpoint;
- o POST ainda não inicia Flow e o PUT ainda não publica CloudEvent nem conclui
  a análise; essas ligações permanecem, respectivamente, nos incrementos 4 e 6;
- a suíte completa e o checkpoint Sonar são o próximo item isolado, Task 3.3.

### Task 3.3 — Suíte e checkpoint Sonar

- `mvn -q test` terminou com código 0: 107 relatórios Surefire registraram
  440 testes, 0 falhas, 0 erros e 0 ignorados;
- o primeiro `./validar-checkpoint-sonarqube.ps1` terminou tecnicamente
  `NON_COMPLIANT`: 225 issues atuais contra 219 no baseline, 6 issues novas,
  nenhuma issue `HIGH`, `BLOCKER` ou `CRITICAL`, cobertura de 81,8% e
  duplicação de 3,3%;
- as 6 issues novas eram 3 ocorrências `java:S1710` no Resource, 2 ocorrências
  `java:S5778` nos testes de casos de uso e 1 ocorrência `java:S7467` no teste
  de contrato;
- a decisão humana `ContinuarAjustes` foi registrada pelo script;
- os wrappers `@APIResponses` foram removidos sem alterar as respostas
  documentadas, as lambdas de `assertThrows` passaram a conter somente a
  invocação sob teste e a exceção ignorada passou a usar padrão sem nome;
- o diagnóstico da cobertura confirmou que os quatro testes unitários novos
  eram executados, mas suas classes não eram instrumentadas pelo
  `quarkus-jacoco`; eles passaram a executar com `@QuarkusTest`, preservando os
  cenários e contribuindo para o relatório consolidado;
- o teste focado de análise e ArchUnit terminou com código 0; a suíte completa
  repetida registrou novamente 440 testes, sem falhas, erros ou ignorados;
- o checkpoint Sonar repetido terminou com código 0 e estado técnico
  `COMPLIANT`: 219 issues atuais contra 219 no baseline, nenhuma issue nova e
  nenhuma issue `HIGH`, `BLOCKER` ou `CRITICAL`;
- métricas finais: cobertura de 86,7% e duplicação de 3,3%;
- decisão humana final `NOT_REQUIRED`, pois não restou violação;
- próximo passo: Task 4.1, limitada à integração do Flow e de
  `ConsultarChecklist` sem bloqueio.

### Task 4.1 — Workflow e consulta assíncrona do checklist

- RED da etapa: `mvn -q "-Dtest=ConsultarChecklistEtapaTest" test` falhou na
  compilação porque contexto e etapa do workflow ainda não existiam;
- GREEN da etapa: a porta `ConsultarChecklist` passou a ser convertida de `Uni`
  para `CompletableFuture` sem espera bloqueante, com cópia defensiva do
  checklist e de sua lista de apontamentos;
- RED da integração: `mvn -q "-Dtest=AnaliseConformidadeFlowQuarkusTest" test`
  falhou na compilação pela ausência de `AnaliseConformidadeFlow`;
- GREEN da integração: o Quarkus registrou o workflow
  `AnaliseConformidadeFlow`; o POST real devolveu `202`, `Location`, estado
  `EM_PROCESSAMENTO` e o identificador nativo da instância enquanto um
  `CompletableFuture` controlado permanecia pendente;
- a instância Flow e a projeção volátil compartilham o mesmo `instanceId`; após
  a conclusão controlada, o output do workflow contém texto e checklist
  congelado no contexto;
- checklist nulo ou sem apontamentos produz falha de negócio determinística;
  falha técnica assíncrona transita a projeção para `FALHOU` com mensagem
  sanitizada, e a falha síncrona de `start()` também deixa a projeção terminal e
  é traduzida como indisponibilidade técnica;
- os testes verificaram exatamente uma chamada à porta nos cenários de falha;
  não foi introduzido retry adicional;
- `mvn -q
  "-Dtest=AnaliseConformidadeFlowQuarkusTest,ConsultarChecklistEtapaTest,CasosDeUsoAnaliseConformidadeTest,ArchUnitProgressivoTest"
  test` terminou com código 0: 13 testes focados e 33 testes ArchUnit, sem
  falhas, erros ou ignorados;
- a busca negativa não encontrou `await`, acesso direto ao client MTR, DTO REST
  ou anotações `@Retry`/`@Timeout`/`@CircuitBreaker` na nova orquestração;
- o consolidado arquitetural foi atualizado com o workflow efetivamente
  implementado até a consulta do checklist;
- risco para C3: o `TraceLoggerExecutionListener` padrão do Flow `0.10.2`
  registrou input/output e causa completa em INFO durante os testes, incluindo
  o texto do documento e detalhe interno de falha. Nenhuma configuração de log
  foi alterada antes do checkpoint humano de observabilidade;
- a suíte completa e o checkpoint Sonar permanecem isolados no próximo item,
  Task 4.2.

### Task 4.2 — Suíte e checkpoint Sonar (decisão pendente)

- a primeira suíte completa expôs quatro falhas dependentes da ordem de
  execução em testes preexistentes dos adapters MTR: era esperado `[null]`, mas
  foi obtido `null`;
- os quatro testes MTR passaram isoladamente e voltaram a falhar quando
  executados depois do novo teste Quarkus do Flow, confirmando interferência de
  isolamento do mock CDI entre aplicações de teste;
- o teste do Flow passou a usar um `QuarkusTestProfile` próprio e uma alternativa
  CDI controlada para `ConsultarChecklist`, sem instalar mock Mockito global;
- a regressão mínima e a combinação do teste do Flow com os quatro testes MTR
  terminaram com código 0;
- `mvn -q test` repetido terminou com código 0: 109 relatórios Surefire
  registraram 447 testes, 0 falhas, 0 erros e 0 ignorados;
- `./validar-checkpoint-sonarqube.ps1` executou `clean verify`, SonarScanner e
  Compute Engine e terminou tecnicamente `NON_COMPLIANT`;
- o checkpoint registrou 223 issues atuais contra 219 no baseline, exatamente
  4 issues novas e nenhuma issue `HIGH`, `BLOCKER` ou `CRITICAL`;
- cobertura: 86,7%, acima da meta de 85%; duplicação: 3,3%, abaixo do limite de
  5%;
- três issues `java:S5778`, severidade `MAJOR`, estão em
  `ConsultarChecklistEtapaTest.java`, linhas 51, 76 e 85, por lambdas com mais
  de uma invocação capaz de lançar exceção;
- uma issue `java:S1128`, severidade `MINOR`, está em
  `AnaliseConformidadeFlowQuarkusTest.java:35`, por import estático não
  utilizado de `assertTrue`;
- a decisão humana `ContinuarAjustes` foi registrada pelo script;
- as lambdas de `assertThrows` passaram a conter somente a chamada sob teste e
  o import estático não utilizado foi removido, sem alteração de produção;
- os dois testes afetados passaram no teste focado e `mvn -q test` repetido
  terminou com código 0, preservando os 447 testes sem falhas;
- o checkpoint Sonar repetido terminou com código 0 e estado técnico
  `COMPLIANT`: 219 issues atuais contra 219 no baseline, nenhuma issue nova e
  nenhuma issue `HIGH`, `BLOCKER` ou `CRITICAL`;
- métricas finais: cobertura de 86,7% e duplicação de 3,3%;
- decisão humana final `NOT_REQUIRED`, pois não restou violação;
- próximo passo: checkpoint humano C3, sem antecipar mudanças de agente,
  prompt, Fault Tolerance, logs, spans ou configuração observável.

### Checkpoint C3 — Agente, Fault Tolerance e observabilidade sensível

- decisão humana explícita `C3 GO`, recebida em 2026-07-24;
- aprovada uma única capacidade agentic sequencial, protegida por porta de
  aplicação e com `memoryId` derivado do `instanceId`;
- aprovados prompt injection como conteúdo não confiável, projeção mínima do
  checklist, structured output tipado e validação Java determinística;
- aprovados timeout HTTP 60 s, timeout FT 65 s, 2 retries adicionais,
  delay/jitter 500/200 ms e circuit breaker 4/0,5/10 s/2;
- aprovado fallback humano completo, sanitizado e sem retry para falhas de
  negócio ou de regra determinística;
- aceita como limitação da PoC a possível duração próxima de 181 s e a ausência
  de truncamento/particionamento silencioso quando `num-ctx=2048` for
  insuficiente;
- aprovado desabilitar o tracing integral do Flow por padrão e manter logs
  completos do LangChain4j somente em `%poc` com dados sintéticos;
- aprovados os spans, atributos e eventos sem conteúdo sensível enumerados no
  plano;
- próximo passo: Task 5.1, limitada ao agente tipado, adaptador Ollama,
  validação, política FT, fallback e integração ao Flow.

### Task 5.1 — Agente sequencial, Ollama, FT e resultado preliminar

- RED/GREEN da fronteira tipada: foram criadas a porta
  `AnalisarTextoComChecklist`, a entrada de aplicação, os records exclusivos do
  adapter Ollama e o mapper de saída;
- a projeção JSON enviada ao modelo contém texto e somente os campos funcionais
  aprovados do checklist; datas e `verificacaoPrevia` não são expostas;
- `AplicadorChecklistAgent` e `RevisorCoberturaAgent` formam uma única
  `@SequenceAgent`, compartilham memória pelo `instanceId`, tratam entrada e
  saída anterior como dados não confiáveis e retornam structured output;
- a validação Java rejeita item extra, duplicado, nome divergente e conteúdo
  inválido; item ausente é completado deterministicamente como
  `NAO_ANALISADO`, sem inventar evidência;
- o adapter Ollama implementa timeout FT de 65 s, duas novas tentativas com
  delay/jitter 500/200 ms, circuit breaker 4/0,5/10 s/2 e fallback completo
  `FALLBACK_TECNICO`; falha de negócio não é retentada;
- o teste CDI controlado comprovou retry até sucesso, abertura do circuito,
  curto-circuito sem chamada ao agente, recuperação após 10 s e fallback de
  saída inválida;
- a inspeção da versão resolvida mostrou que a extensão fixava internamente
  `maxRetries(1)` no builder Ollama; um `ModelBuilderCustomizer` passou a fixar
  `maxRetries(0)`, preservando a política MicroProfile aprovada como única fonte
  de repetição;
- `AnaliseConformidadeFlow` passou a executar uma task `agent(...)` após a
  consulta, normaliza o identificador estrutural da task para o `instanceId`
  raiz e carrega o resultado preliminar no contexto;
- o tracing textual do Flow foi desabilitado em produção e teste após evidência
  de que expunha payload e stack; spans manuais
  `simtr-hub.flow.conformidade.analise` e
  `simtr-hub.agent.conformidade.analisar` foram verificados sem texto, prompt,
  resposta, evidência, credencial ou stack;
- o teste opt-in executou a sequência real no Ollama local com
  `llama3.2:3b` e terminou GREEN com origem `AGENTE`; após desabilitar o retry
  interno, o rerun terminou com código 0 em 28 s;
- os testes focados de mapper, prompts, adapter, FT, Flow, compatibilidade,
  validação e ArchUnit terminaram com código 0;
- a suíte padrão mantém a integração real desabilitada e o teste Ollama marcado
  como opt-in;
- próximo passo: Task 5.2, limitada à suíte completa e ao checkpoint Sonar do
  incremento 5.

### Task 5.2 — Suíte e checkpoint Sonar

- `mvn -q test` terminou com código 0: 115 relatórios Surefire registraram
  461 testes, 0 falhas, 0 erros e 1 ignorado; o único teste ignorado é a
  integração real com Ollama, mantida como opt-in;
- o primeiro `./validar-checkpoint-sonarqube.ps1` executou `clean verify`,
  SonarScanner e Compute Engine e terminou tecnicamente `NON_COMPLIANT`:
  230 issues atuais contra 219 no baseline e 11 issues novas;
- cobertura e duplicação já estavam conformes, respectivamente em 86,2% e
  3,1%; uma issue `java:S1192` possuía severidade `CRITICAL` e impacto `HIGH`;
- a primeira decisão humana `ContinuarAjustes` foi registrada pelo script;
- foram corrigidos o literal repetido, três recursos `Scope`, seis lambdas de
  `assertThrows` e a espera do circuit breaker, sem nova dependência ou
  alteração de contrato;
- o checkpoint repetido reduziu as issues novas de 11 para 3, eliminou toda
  severidade `HIGH`, `BLOCKER` ou `CRITICAL` e preservou as métricas, mas
  permaneceu `NON_COMPLIANT` por três ocorrências `java:S1481`;
- a segunda decisão humana `ContinuarAjustes` foi registrada pelo script;
- os três scopes passaram a ser fechados explicitamente antes do encerramento
  dos spans, preservando a ordem e a classificação de exceções existentes;
- os testes focados do adapter e da integração FT passaram, e o checkpoint
  final executou novamente a suíte completa com código 0;
- estado técnico final `COMPLIANT`: 219 issues atuais contra 219 no baseline,
  nenhuma issue nova e nenhuma issue `HIGH`, `BLOCKER` ou `CRITICAL`;
- métricas finais: cobertura de 86,2% e duplicação de 3,1%;
- decisão humana final `NOT_REQUIRED`, pois não restou violação;
- próximo passo: Task 6.1, sem antecipar a implementação de pausa, correlação
  ou retomada HITL.

### Task 6.1 — Pausa, correlação e retomada HITL

- o diagnóstico reproduziu a projeção presa antes de `AGUARDANDO_REVISAO` e
  confirmou que `emitJson` publicava no `flow-out`, mas o SDK efetivo não
  preenchia `time` e usava `source=reference-impl`;
- um `EmittedEventDecorator` registrado por ServiceLoader, confinado ao adapter
  de Messaging e ao workflow `analise-conformidade`, passou a preencher
  `source=urn:simtr-hub:conformidade` e `time` UTC sem alterar o payload;
- o teste RED/GREEN comprovou `revisao.solicitada.v1`, atualização da projeção
  para `AGUARDANDO_REVISAO` e a instância Flow em `WAITING`;
- a porta `PublicarRevisaoNoWorkflow` passou a isolar a publicação assíncrona
  do caso de uso, e o PUT entrega a revisão ao `flow-in` sem bloqueio;
- o workflow revalida a revisão após a retomada, emite
  `analise.concluida.v1` e conclui a projeção com origem
  `REVISAO_HUMANA`;
- o E2E falso cobriu POST -> GET aguardando -> PUT -> GET concluída, além de
  correlação cruzada entre duas instâncias, `422` sem retomada e duplicidade
  com `409`;
- o store preservou a prova concorrente de exatamente uma reserva entre duas
  revisões simultâneas, e o teste de canais passou a usar o resultado real
  exigido pela projeção;
- 34 testes focados de Flow, casos de uso, REST, canais, codec, ack e store
  passaram; ArchUnit, contrato API e compatibilidade Flow/Ollama também
  terminaram com código 0;
- nenhum log temporário de payload permaneceu habilitado;
- próximo passo: Task 6.2, limitada à suíte completa e ao checkpoint Sonar do
  incremento 6.

### Task 6.2 — Suíte e checkpoint Sonar indisponível

- `git diff --check` terminou com código 0; foram emitidos somente avisos de
  normalização futura de LF para CRLF, sem erro de whitespace;
- `mvn -q test` concluiu depois do timeout de acompanhamento do runner; os 115
  relatórios Surefire registraram 464 testes, 0 falhas, 0 erros e 1 ignorado;
- o único teste ignorado continua sendo a integração real com Ollama, mantida
  como opt-in;
- o processo Codex atual não herdou `SONAR_TOKEN`; conforme a regra de segurança,
  nenhum token foi solicitado, aceito ou exposto no chat e o checkpoint local
  não foi executado;
- o estado Sonar deste incremento permanece `UNVERIFIED`, sem ser transformado
  em aprovação, reprovação ou conformidade;
- decisão humana explícita `Pode continuar sem sonar`, recebida em 2026-07-25,
  autorizou avançar para a Task 7.1 com essa limitação registrada;
- em 2026-07-26, antes de iniciar a Task 7.1, o processo Codex herdou a credencial
  somente em memória e permitiu revalidar o incremento sem expor o token;
- a primeira tentativa de inicializar o baseline da nova sessão encontrou uma
  ocorrência intermitente em `ChecklistSelecaoSimuladorQuarkusTest`: o span manual
  não foi localizado; o teste isolado e a suíte completa subsequente terminaram com
  código 0, sem reprodução da falha e sem alteração especulativa de código;
- a segunda inicialização executou `clean verify`, SonarScanner e Compute Engine com
  código 0, e o checkpoint repetido terminou `COMPLIANT`;
- a revalidação registrou 219 issues atuais contra 219 no baseline, nenhuma issue
  nova ou `HIGH`, `BLOCKER` ou `CRITICAL`, cobertura de 86,2% e duplicação de 3,1%;
- a decisão técnica foi `NOT_REQUIRED`; o registro histórico de indisponibilidade e
  a autorização humana de 2026-07-25 foram preservados;
- próximo passo: Task 7.1, limitada ao spike de compatibilidade e roteamento
  mínimo, sem iniciar a evolução do contrato público da Task 7.2.

### Task 7.1 — Compatibilidade e roteamento mínimo durável

- as fontes oficiais do Flow foram fixadas na tag `0.10.2`; elas confirmaram um
  único provider de persistência, `quarkus-flow-redis`, os SPIs
  `EventConsumer`/`EventPublisher` e a associação da Lease ao ID da
  `WorkflowApplication`;
- foi criado o profile Maven opt-in `spike-persistencia-duravel`, que troca apenas
  o source set de testes por `src/spike-test/java`; sem o profile, Redis,
  Kubernetes e Testcontainers não entram no classpath e nenhuma classe de
  produção, contrato REST, OpenAPI ou DTO foi alterado;
- a árvore do profile resolveu `quarkus-flow-redis:0.10.2`,
  `quarkus-flow-durable-kubernetes:0.10.2`, clientes Quarkus `3.33.2.1`,
  `serverlessworkflow-persistence-tests:7.22.2.Final` e
  `testcontainers:2.0.4`, sem upgrade da plataforma;
- RED: o teste referencial falhou na compilação somente pela ausência de
  `CouchDbChangeEventMapper`; GREEN: o mapper test-only passou a produzir ID
  determinístico por documento/revisão, extensões de referência, hash, sequência
  e revisão, sem `data`;
- `CouchDbChangesIntegrationTest` subiu `couchdb:3.5.2`, criou um documento,
  repetiu `_changes?since=0&include_docs=true` e comprovou o mesmo CloudEvent
  referencial nas duas leituras, sem transportar o campo de negócio `parecer`;
- `RedisCheckpointCompatibilidadeQuarkusTest` subiu
  `valkey/valkey:7.2-alpine` e executou o contrato oficial
  `AbstractHandlerPersistenceTest`: writer, reader, scan e restauração de contexto
  terminaram com código 0;
- `LeaseWorkflowApplicationCompatibilidadeTest` comprovou que, fora da estratégia
  dev/test, a extensão dispara `LeaseStartupEvent`, espera até 30 segundos pela
  Lease de membro e usa o nome obtido em `WorkflowApplication.Builder.withId`;
- o teste de SPI confirmou `EventConsumer.listen(EventFilter,
  WorkflowApplication)`, `EventPublisher.publish(CloudEvent)` assíncrono e
  `WorkflowApplication.Builder.withId(String)` na versão efetiva;
- `mvn -q -Pspike-persistencia-duravel test` executou 6 testes com 0 falhas e
  0 erros; `mvn -q test` preservou a suíte padrão com código 0;
- o checkpoint executou `clean verify`, SonarScanner e Compute Engine e terminou
  `COMPLIANT`: 219 issues atuais contra 219 no baseline, nenhuma issue nova ou
  `HIGH`, `BLOCKER` ou `CRITICAL`, cobertura de 86,2%, duplicação de 3,1% e
  decisão `NOT_REQUIRED`;
- limitação observada: o profile isolado não possui o exporter OpenTelemetry
  in-memory da suíte padrão e tentou acessar o OTLP local indisponível; também
  registrou advertência para `quarkus.flow.persistence.auto-restore`, embora a
  interface de configuração `FlowPersistenceConfig` exponha esse prefixo e
  propriedade na versão `0.10.2`; nenhuma correção foi feita fora do spike;
- não foram comprovados restart entre processos, duas réplicas, Lease real,
  roteamento cross-pod ou failover. Nenhum fallback distribuído foi criado. Essas
  provas permanecem nas Tasks 8.2/8.3 e o ADR-0010 continua `Proposto`;
- próximo item formal: Task 7.2. Por instrução desta retomada, ela permanece
  pendente e nenhum contrato público foi antecipado.

### Task 7.2 — Contrato estável de identidades

- RED: os testes de contrato, domínio e store falharam na compilação pela ausência
  da fábrica que gera `correlationId`, dos campos de identidade na visão e da
  inicialização completa da projeção;
- o POST passou a exigir `identificadorDocumento`; o mapper cria a solicitação com
  `correlationId` UUID gerado pelo Hub, sem aceitar correlação fornecida pelo cliente;
- POST e GET expõem exatamente `correlationId`, `instanceId`,
  `identificadorDocumento`, `identificadorChecklist` e `versaoChecklist`, mantendo
  os paths atuais baseados em `instanceId`;
- a visão imutável preserva as cinco identidades nas transições e rejeita resultado
  cujo checklist/versão não corresponda à análise; o caso de uso de revisão valida
  novamente as identidades persistidas;
- o PUT continua aceitando somente observação e apontamentos; o teste OpenAPI
  comprova as cinco identidades nas respostas e a ausência delas no DTO de revisão;
- DTOs permaneceram exclusivos do adapter REST; `ArchUnitProgressivoTest` terminou
  com código 0;
- testes focados de contrato, domínio, casos de uso, mapper, store, Flow e Messaging
  terminaram com código 0; a suíte executada pelo checkpoint registrou 115 relatórios
  Surefire, 466 testes, 0 falhas, 0 erros e 1 teste opt-in ignorado;
- revisão nos eixos de correção, simplicidade, arquitetura, segurança e desempenho
  não encontrou bloqueadores; não foram adicionadas dependências, logs, spans,
  persistência ou mudança de autenticação;
- risco registrado sem alteração contratual: a especificação aprovada não define
  tamanho máximo para `identificadorDocumento`; o incremento valida somente
  obrigatoriedade/não vazio e não inventa um limite público;
- esclarecimento humano recebido em 2026-07-26: `identificadorDocumento` permanece
  `String`; um identificador originalmente numérico pode ser representado como texto,
  e nenhuma conversão para `Long` será feita nesta evolução;
- esclarecimento humano recebido em 2026-07-26 para a Task 7.3: `versaoSchema` é
  `small int`, representado por `Short` no Java e por número inteiro no JSON;
- o checkpoint executou `clean verify`, SonarScanner e Compute Engine e terminou
  `COMPLIANT`: 219 issues atuais contra 219 no baseline, nenhuma issue nova ou
  `HIGH`, `BLOCKER` ou `CRITICAL`, cobertura de 86,2%, duplicação de 3,0% e decisão
  `NOT_REQUIRED`;
- Task 7.3 permanece pendente; nenhum adapter CouchDB, persistência, `_changes` ou
  checkpoint Redis/Valkey foi iniciado.

### Task 7.3 — Persistência documental por ambiente

- esclarecimento humano recebido em 2026-07-26: hashes canônicos são `String`;
  adotada a representação recomendada SHA-256 com 64 caracteres hexadecimais
  minúsculos;
- RED/GREEN do contrato compartilhado comprovou sequência completa, revisão
  idêntica, conflito contraditório, concorrência e falha sanitizada nos stores em
  memória e CouchDB;
- RED/GREEN de recuperação parcial comprovou que o adapter CouchDB retoma a criação
  da projeção quando o documento inicial imutável já foi persistido;
- `mvn -q test` terminou com código 0 antes do primeiro checkpoint desta task;
- o checkpoint Sonar terminou `NON_COMPLIANT`: 21 issues novas, das quais 10
  `CRITICAL`, cobertura de 80,1% e duplicação de 2,9%;
- decisão humana `ContinuarAjustes` registrada pelo script em 2026-07-26; as issues
  e a cobertura foram corrigidas antes do novo checkpoint;
- RED/GREEN do hash comprovou snapshot com `hashConteudo` `String` SHA-256 em
  hexadecimal minúsculo e a mesma referência/hash na projeção;
- a suíte completa após os ajustes terminou com código 0 e o relatório JaCoCo local
  registrou 4.341 linhas cobertas e 361 não cobertas;
- o novo checkpoint Sonar terminou `COMPLIANT`: 219 issues atuais contra 219 no
  baseline, nenhuma issue nova ou `HIGH`, `BLOCKER` ou `CRITICAL`, cobertura de
  85,1%, duplicação de 2,9% e decisão `NOT_REQUIRED`.
- RED do contrato reativo falhou ao encontrar `void` nas escritas da porta
  documental; o GREEN converteu escritas para `Uni<Void>` e leitura para
  `Uni<Optional<VisaoAnaliseConformidade>>`;
- o store em memória, o adapter CouchDB, os casos de uso, a projeção interna, o
  consumidor Reactive Messaging e os callbacks Flow passaram a propagar `Uni`;
- o cliente CouchDB deixou de usar `HttpClient.send` e passou a usar
  `HttpClient.sendAsync`, sem `await`, `join` ou chamada HTTP bloqueante no código
  de produção da conformidade;
- os contratos compartilhados dos stores em memória e CouchDB real, os testes de
  casos de uso, Flow, Reactive Messaging e a suíte completa terminaram com código
  0;
- o checkpoint Sonar desta fatia foi tentado em 2026-07-26, mas encerrou antes de
  Maven/SonarScanner porque `SONAR_TOKEN` não estava disponível no processo;
  situação `UNVERIFIED`, sem aprovação ou reprovação técnica;
- RED de compatibilidade comprovou a ausência inicial do Azure Cosmos DB Java SDK;
  o GREEN adicionou o BOM oficial `azure-sdk-bom` 1.3.8, `azure-cosmos` 4.81.0,
  `azure-identity` 1.18.4 e o adaptador oficial Reactor/Java Flow do Mutiny;
- o núcleo documental foi extraído para um store compartilhado sem tipos de
  fornecedor; CouchDB e Cosmos DB implementam a mesma interface interna de
  criação, substituição otimista e consulta;
- o adapter Cosmos usa `correlationId` como chave lógica de partição, `_etag` com
  `If-Match` nas substituições e consulta parametrizada pelo `id` somente quando a
  partição ainda não é conhecida;
- um RED arquitetural expôs o token de concorrência dentro do conteúdo comum; o
  GREEN separou conteúdo e versão na interface neutra, deixando `_rev`, `_etag` e
  os demais metadados de sistema confinados aos adapters;
- o contrato compartilhado passou para CouchDB real e para um repositório Cosmos
  determinístico; os testes unitários do adapter Cosmos comprovaram `409`/`412`
  como conflito, `404` como ausência, partition key e propagação do `_etag`;
- `ArchUnitProgressivoTest` e a suíte completa terminaram com código 0; foram
  registrados 122 relatórios Surefire, 496 testes, 0 falhas, 0 erros e 1 teste
  opt-in ignorado;
- o checkpoint Sonar desta nova fatia foi tentado em 2026-07-26, mas novamente
  encerrou antes de Maven/SonarScanner porque o processo não herdou `SONAR_TOKEN`;
  situação `UNVERIFIED`, sem aprovação ou reprovação técnica;
- a integração opt-in contra Cosmos DB Emulator ou conta não produtiva, a seleção
  do adapter por ambiente e a inicialização automática do CouchDB em DES continuam
  pendentes nesta Task 7.3;
- RED/GREEN da seleção removeu o store em memória da descoberta CDI e passou a
  exigir `couchdb` ou `cosmosdb`; a ausência ou um valor desconhecido falha no
  startup fora de `dev/test`;
- o cliente Cosmos é singleton assíncrono, usa exclusivamente
  `DefaultAzureCredential`, é fechado no shutdown e rejeita propriedades ou
  variáveis de chave/connection string;
- `%dev` e `%test` selecionam CouchDB; os dois testes Quarkus que exercitam o store
  usam containers efêmeros próprios, senha aleatória e Compose Dev Services
  desabilitado;
- `compose-devservices.yml` usa `couchdb:3.5.2`, credenciais locais obrigatórias por
  variáveis, porta dinâmica mapeada, health check que cria `conformidade`
  idempotentemente e volume nomeado;
- a prova real com dois ciclos de `mvn quarkus:dev` criou um documento sintético,
  encerrou graciosamente, leu o mesmo documento após o restart e o removeu; container
  e rede foram removidos e o volume permaneceu nos dois shutdowns;
- o RED operacional mostrou que Ryuk removia o volume mesmo com
  `remove-volumes=false`; o GREEN desabilitou Ryuk somente em `%dev`, preservando
  Testcontainers/Ryuk nos testes; o volume sintético final foi removido para não
  deixar credenciais de prova na máquina;
- `ArchUnitProgressivoTest` e a suíte completa terminaram com código 0; foram
  registrados 124 relatórios Surefire, 502 testes, 0 falhas, 0 erros e 1 teste
  opt-in ignorado;
- o checkpoint Sonar desta fatia foi tentado em 2026-07-26 e encerrou antes de
  Maven/SonarScanner porque o processo não herdou `SONAR_TOKEN`; situação
  `UNVERIFIED`, sem aprovação ou reprovação técnica;
- a integração opt-in contra Cosmos DB Emulator ou conta não produtiva e os SPIs
  `EventPublisher`/feeds nativos continuam pendentes nesta Task 7.3.
- na retomada de 2026-08-02, o baseline local atualizado terminou
  `NON_COMPLIANT` somente por cobertura de 83,2%, com 237 issues atuais e no
  baseline, nenhuma issue nova ou `HIGH`, `BLOCKER` ou `CRITICAL` e duplicação de
  2,8%; a decisão humana `ContinuarAjustes` foi registrada pelo script;
- a inspeção do JaCoCo mostrou que os testes existentes do producer e dos adapters
  Cosmos passavam fora do classloader instrumentado do Quarkus; quatro testes
  passaram a executar com `@QuarkusTest`, sem alteração no código de produção;
- o RED que tentou construir um cliente Cosmos real comprovou dependência de
  autenticação/rede e foi substituído por uma matriz hermética dos guardrails de
  endpoint (scheme, host, userinfo, query, fragmento e sintaxe inválida);
- `mvn -q clean test` e o `clean verify` do checkpoint passaram; a falha
  intermitente anterior na captura do span de checklist não reapareceu e nenhuma
  espera ou enfraquecimento de asserção foi introduzido sem causa reproduzível;
- o checkpoint final de 2026-08-02 terminou `COMPLIANT`: 237 issues atuais contra
  237 no baseline, nenhuma issue nova ou `HIGH`, `BLOCKER` ou `CRITICAL`, cobertura
  de 85,3%, duplicação de 2,8% e decisão `NOT_REQUIRED`;
- a Task 7.3 permanece pendente pelos itens funcionais já registrados: integração
  Cosmos opt-in e `EventPublisher`/feeds nativos, estes bloqueados pelo C7.
- na preparação do `EventPublisher`, a árvore efetiva confirmou
  `cloudevents-core:4.1.0`; sua implementação aceita somente `[a-z0-9]+` em nomes
  de extensões e rejeita o `correlationId` aprovado por conter `I` maiúsculo;
- recomendação apresentada no checkpoint C7: usar `correlationid` somente no envelope
  CloudEvent, preservando `correlationId` na API REST, nos documentos e nos modelos;
  nenhuma mudança executável foi feita antes dessa decisão humana;
- `C7 GO` recebido do usuário em 2026-08-02 aprovou essa recomendação e liberou a
  continuação da Task 7.3, sem autorizar alteração de casing nas demais bordas.
- esclarecimento humano recebido em 2026-08-02 para o `data` referencial da Task
  7.3: `documentoRef` é `String` opaca gerada internamente e validada contra o ID
  determinístico; `hashConteudo` é `String` SHA-256 hexadecimal minúscula validada
  contra o documento canônico; `versaoSchema` permanece `Short`, inicialmente `1`.
- o `EventPublisher` referencial persiste somente emissões de conformidade pela
  porta documental neutra, propaga a conclusão assíncrona sem bloquear e ignora
  tipos de outros workflows;
- o bytecode efetivo do `WorkflowApplicationCreator` do Flow `0.10.2` confirmou a
  descoberta CDI de todos os beans `EventPublisher`; um teste de bootstrap Quarkus
  comprovou exatamente uma instância do publisher documental no runtime;
- os testes focados do mapper, decorator, publisher, canais internos e workflow
  terminaram com código 0; a suíte completa registrou 127 relatórios, 526 testes,
  0 falhas, 0 erros e 2 testes ignorados;
- o gate Cosmos opt-in permanece `UNVERIFIED`: `COSMOS_INTEGRATION_ENABLED`,
  `COSMOS_ENDPOINT`, `COSMOS_DATABASE` e `COSMOS_CONTAINER` não estavam presentes
  no processo de 2026-08-03; nenhum valor de credencial foi solicitado ou exibido;
- após a decisão `ContinuarAjustes`, o ambiente foi verificado novamente sem exibir
  valores e as quatro variáveis continuavam ausentes; a execução isolada de
  `CosmosDbAnaliseConformidadeStoreOptInTest` descobriu 1 teste e o ignorou pelo
  opt-in desabilitado, mantendo o gate `UNVERIFIED`;
- o checkpoint Sonar do publisher terminou `NON_COMPLIANT`: 242 issues atuais e no
  baseline, nenhuma issue nova, cobertura de 85,0%, duplicação de 2,7% e duas issues
  `CRITICAL`/impacto HIGH da regra `java:S1192` nas linhas 341 e 345 de
  `DocumentoAnaliseConformidadeStore`;
- a decisão humana `ContinuarAjustes` foi registrada pelo script em 2026-08-03; os
  dois literais passaram a reutilizar as constantes já existentes, sem alterar as
  chaves JSON ou o comportamento;
- os contratos dos stores em memória, CouchDB real e Cosmos determinístico passaram
  com código 0; a reexecução Sonar terminou `COMPLIANT`, com 240 issues atuais contra
  242 no baseline, nenhuma issue nova ou bloqueante, cobertura de 85,0%, duplicação
  de 2,7% e decisão `NOT_REQUIRED`;
- decisão humana C8 recebida em 2026-08-03: CouchDB permanece como integração local;
  Cosmos deve ser validado por contrato determinístico e SDK mockado nesta etapa;
  o teste real continua opt-in e não bloqueia a Task 7.3, mas permanece obrigatório
  antes de qualquer promoção para PRD;
- a matriz focada aprovada no C8 executou CouchDB `3.5.2` em container real, contrato
  Cosmos com repositório determinístico, APIs do Azure Cosmos DB Java SDK v4
  mockadas, seleção do backend e `EventPublisher`: 40 testes, 0 falhas, 0 erros e
  0 ignorados;
- a Task 7.3 foi concluída; a integração Cosmos real continua pendente somente como
  gate externo obrigatório antes de qualquer promoção para PRD e não foi declarada
  verificada por esses mocks.

### Task 7.4 — Contexto referencial e checkpoint Redis/Valkey

- `quarkus-flow-redis` foi habilitado e a integração usa Valkey `7.2-alpine` nos
  testes, mantendo os dados completos de negócio no backend documental;
- o contexto do workflow passou a carregar somente as cinco identidades e a
  referência documental do checklist; cada etapa recarrega e valida o conteúdo pela
  porta documental antes de usá-lo;
- o teste `checkpointRedisEhReferencialERestauraInstanciaEmEspera` comprovou que o
  hash persistido não contém texto, checklist, resultado ou revisão completos e que
  uma instância `WAITING` pode ser reconstruída pelos handlers após descarte do
  estado volátil no mesmo processo;
- a limitação já registrada em `compatibilidade.md` permanece: essa evidência não é
  restart real da aplicação nem prova cross-pod/failover, itens posteriores do plano;
- a primeira suíte completa registrou 542 testes aprovados; o checkpoint Sonar ficou
  `NON_COMPLIANT`, com cobertura de 84,9%, duplicação de 2,6% e duas issues novas
  (`java:S5778` e `java:S1612`); a decisão humana `ContinuarAjustes` foi registrada;
- o primeiro ajuste removeu `java:S1612`, mas os novos testes de validação ainda não
  contribuíam para o JaCoCo por executarem fora do classloader instrumentado; o novo
  checkpoint registrou 552 testes aprovados, 241 issues contra 242 no baseline,
  cobertura de 84,9% e duplicação de 2,6%; nova decisão humana
  `ContinuarAjustes` foi registrada em 2026-08-04;
- o ajuste final isolou o `Uni` fora da lambda indicada por `java:S5778` e executou
  `ContextoAnaliseConformidadeFlowTest` com `@QuarkusTest`, cobrindo integralmente as
  validações do envelope referencial;
- o checkpoint final terminou `COMPLIANT`: 240 issues atuais contra 242 no baseline,
  nenhuma issue nova ou bloqueante, cobertura de 85,1%, duplicação de 2,6% e decisão
  `NOT_REQUIRED`; 128 relatórios Surefire registraram 552 testes, 0 falhas, 0 erros e
  2 testes opt-in ignorados;
- a Task 7.4 foi concluída sem iniciar qualquer alteração da Task 7.5.

### Task 7.5 — Feed documental nativo e retomada correlacionada

- o runtime passou a registrar somente o `EventPublisher` documental e o
  `FeedNativoEventConsumer`; a ponte volátil padrão não permanece ativa;
- o feed CouchDB usa `_changes` com cursor persistido no próprio banco e só confirma
  a sequência após entrega aceita; o adapter reconstruído retomou do cursor salvo em
  teste com CouchDB `3.5.2` real;
- o feed Cosmos configura `ChangeFeedProcessor` com container de leases persistente;
  seu contrato mockado comprovou ciclo de vida, descarte observável de documento
  inválido e propagação de falha de entrega para retry;
- documentos de revisão persistidos são convertidos em CloudEvents referenciais com
  `id` determinístico, `flowinstanceid`, `flowtaskid`, `correlationid`,
  `documentoRef`, `hashConteudo` e `versaoSchema`, rejeitando adulteração antes da
  entrega;
- RED: o E2E completo executou 7 cenários, com 4 aprovados e 3 falhas de retomada; a
  causa foi `extensionByInstanceId`, cujo predicado tentava materializar a interface
  `CloudEvent` por conversão Jackson;
- GREEN: a espera passou a usar correlação declarativa entre `.flowinstanceid` e
  `$workflow.id`; os 7 cenários E2E aprovaram PUT após persistência, retomada HITL,
  isolamento entre instâncias, duplicação, restauração referencial e falhas
  sanitizadas;
- a matriz focada aprovou 35 testes do feed, runtime, mappers, publisher e E2E; o
  teste adicional de cursor CouchDB aprovou 1/1 em execução Maven isolada. Quando
  misturado aos dois contextos `@QuarkusTest` no mesmo fork, o ServiceLoader de
  MicroProfile Context apresentou conflito de classloader; a suíte completa deverá
  confirmar ou tratar essa interferência na Task 7.6;
- o stack trace diagnóstico temporário do retry de `_changes` foi removido, mantendo
  apenas mensagem operacional sanitizada; a busca negativa no `pom.xml` e no código
  de conformidade não encontrou Kafka, AMQP ou Reactive Messaging;
- a advertência já registrada para `quarkus.flow.persistence.auto-restore` permanece
  fora deste ajuste. A Task 7.6 e seu checkpoint Sonar ainda não foram iniciados.

### Task 7.6 — Suíte, compatibilidade e restart

- decisão humana C9 recebida em 2026-08-05: proceder com a recomendação de alinhar o
  teste de cursor ao harness `@QuarkusTest`, executar o spike pareado Flow
  `0.13.0`/LangChain4j `1.12.0` e comprovar restart entre processos JVM;
- a prova desta task continua limitada a uma réplica e aos mesmos backends; Lease,
  roteamento cross-pod e failover permanecem na Task 8.3;
- as versões candidatas só serão retidas se dependency tree, compilação, bootstrap,
  matriz focada, E2E e configuração de auto-restore forem compatíveis; caso
  contrário, o POM volta às versões aprovadas e a incompatibilidade fica registrada.
- o teste de cursor foi convertido para `@QuarkusTest`, reutiliza o
  `CouchDbQuarkusTestResource` e desabilita somente o feed de runtime no perfil
  próprio; a matriz mínima com três contextos passou sem `ServiceConfigurationError`;
- a matriz ampla confirmou o cursor sem erro de classloader, mas expôs novamente a
  intermitência da segunda instância correlacionada no E2E 0.10.2; a classe-base do
  Flow foi inspecionada e comprovou que ela já multiplexa os registros por tipo, de
  modo que nenhuma alteração especulativa foi feita no adapter;
- o spike Flow `0.13.0`/LangChain4j `1.12.0` compilou e resolveu
  Serverless Workflow `7.25.1.Final` e CloudEvents `4.1.1`, mas o bootstrap falhou
  por dois beans CDI `@Default` de `AgenteAnaliseConformidade`; o POM voltou a
  `0.10.2`/`1.11.2` e recompilou com sucesso;
- a prova opt-in `AnaliseConformidadeRestartEntreJvmTest`, orquestrada por
  `validar-restart-conformidade.ps1`, executou duas invocações Maven/JVM separadas
  contra o mesmo CouchDB e o mesmo Valkey efêmeros: a primeira persistiu uma
  instância `WAITING` e a segunda restaurou, recebeu a revisão e concluiu a mesma
  instância como `COMPLETED`;
- o `ValkeyQuarkusTestResource` permanece global para manter Redis ativo nos
  contextos Quarkus da suíte, mas não inicia container quando a prova opt-in informa
  `restart.proof.enabled=true`; assim, as duas JVMs preservam o endpoint Valkey
  externo compartilhado;
- a advertência de `quarkus.flow.persistence.auto-restore` continua sendo emitida
  por Flow `0.10.2`, mas a prova entre JVMs demonstrou que o auto-restore padrão
  funciona. A validação global de configuração não foi desabilitada;
- os containers da prova foram removidos ao final. Restart cross-pod, Lease e
  failover continuam fora desta evidência e permanecem nas Tasks 8.2/8.3;
- a primeira suíte completa revelou que restringir o recurso Valkey ao E2E
  desativava Redis nos demais contextos Quarkus; o recurso voltará a ser global na
  suíte normal e preservará o endpoint externo apenas na prova opt-in;
- a mesma execução reproduziu uma janela de consistência: o `_changes` pode observar
  o fato de revisão antes de a projeção persistir `revisaoRef`, fazendo o Flow
  consumir o evento antes de conseguir carregar sua referência. O plano foi
  atualizado antes do ajuste para ordenar a reserva de forma recuperável;
- GREEN: a projeção agora reserva `revisaoRef` antes da criação do fato imutável que
  dispara o `_changes`; repetição após falha intermediária completa o fato ausente e
  concorrência contraditória não cria evento órfão. A matriz focal, os contratos dos
  adapters CouchDB/Cosmos e a prova posterior entre duas JVMs passaram;
- `mvn -q test` aprovou a suíte completa com 562 testes, 0 falhas, 0 erros e 4
  ignorados; o teste de restart permanece opt-in na suíte padrão;
- o baseline da sessão foi inicializado somente no fechamento da task, depois das
  alterações, e por isso não foi usado isoladamente para afirmar ausência de
  regressão. O checkpoint oficial terminou `NON_COMPLIANT`, com 256 issues atuais
  e no baseline tardio, 0 issues novas nessa comparação, cobertura de 83,0%,
  duplicação de 2,5% e 3 issues `CRITICAL`/impacto HIGH da regra `java:S1192`;
- a comparação compensatória com a análise anterior de 2026-08-04 encontrou 240
  issues anteriores contra 256 atuais e 16 issues abertas criadas depois daquela
  análise. As três impeditivas pedem constantes para `"Cursor CouchDB inválido"`,
  `"versaoSchema"` e `"hashConteudo"`; a decisão humana `ContinuarAjustes` foi
  registrada pelo script;
- as 16 issues novas foram corrigidas e seis classes de teste foram alinhadas ao
  harness `@QuarkusTest`; antes do ajuste final de cobertura, a suíte aprovou 562
  testes, sem falhas ou erros e com 4 testes opt-in ignorados;
- oito cenários úteis foram acrescentados a `CloudEventMapperTest` e
  `CosmosChangeFeedTest`: integridade do documento referencial, tipo/source
  divergentes, envelope ausente/JSON inválido, recuperação após falha de início,
  `close` idempotente, filtragem de lotes e limites do hostname;
- a matriz focada final aprovou 23 testes, sem falhas, erros ou ignorados; a suíte
  completa final aprovou 570 testes, 0 falhas, 0 erros e 4 ignorados;
- `validar-restart-conformidade.ps1` repetiu com sucesso as duas fases em JVMs
  distintas contra os mesmos CouchDB e Valkey efêmeros, removidos ao final;
- a sessão de 2026-08-06 também precisou inicializar o baseline depois dos ajustes;
  por isso a comparação baseline/checkpoint desta sessão não é usada isoladamente
  para afirmar ausência de regressão. A comparação compensatória voltou de 256 para
  as 240 issues da análise de 2026-08-04, consistente com a correção líquida das 16
  issues sem acréscimo;
- o checkpoint formal terminou `COMPLIANT`: 240 issues atuais contra 240 no baseline
  tardio da sessão, 0 novas, nenhuma issue `HIGH`, `BLOCKER` ou `CRITICAL`, cobertura
  de 85,2%, duplicação de 2,5% e decisão `NOT_REQUIRED`;
- a Task 7.6 está tecnicamente concluída. O próximo item é a Task 8.1; cross-pod,
  Lease e failover continuam fora desta evidência e permanecem nas Tasks 8.2/8.3.
- a validação real posterior confirmou `llama3.2:3b` instalado no Ollama local. A
  integração opt-in da aplicação executou o adapter real, concluiu a chamada em
  aproximadamente 16,8 s, produziu `OrigemResultado.AGENTE` e passou sem fallback;
  uma inferência direta pela CLI, fora de JUnit e Quarkus, também terminou com código
  0. A resposta livre foi `OLLMAMOK` em vez do texto exato solicitado, registrando
  aderência textual imperfeita sem invalidar a prova estruturada da aplicação.
- decisão humana recebida em 2026-08-06: a PoC de containers e múltiplos pods assume
  Redis/Valkey compartilhado e disponível; perda, restart, alta disponibilidade ou
  recuperação do próprio Redis/Valkey ficam fora do escopo. As Tasks 8.1/8.3 devem
  provar restart/failover somente da aplicação, mantendo os backends disponíveis.
- decisão humana recebida em 2026-08-06 para a Task 8.1: reutilizar o Ollama e os
  modelos já instalados no host, sem baixar ou manter uma segunda cópia em volume
  Docker. A conectividade foi comprovada fora de testes: `ollama list` encontrou
  `llama3.2:3b` e a imagem da aplicação consultou com sucesso `/api/tags` por
  `host.docker.internal:11434`. A forma de acesso do Kubernetes fica para a Task 8.2.
- checkpoint humano de segurança aprovado em 2026-08-06: desabilitar somente no
  Compose local da PoC o tenant OIDC e o cliente OIDC padrão. A autorização não altera
  os perfis `dev` ou produtivo e não autoriza remover as extensões da aplicação.

### Task 8.1 — Empacotamento local de uma réplica

- foi criado um empacotamento JVM `fast-jar` sobre UBI 9/OpenJDK 25, executado como
  usuário não root, com `curl` disponível para healthcheck e preflight;
- `compose-poc.yml` sobe uma réplica da aplicação, CouchDB `3.5.2` e Valkey
  `7.2-alpine`; somente o CouchDB possui volume nomeado. O Valkey permanece sem
  persistência própria porque perda, restart e HA desse backend estão fora da PoC;
- o Compose reutiliza `llama3.2:3b` já instalado no Ollama do host por
  `host.docker.internal:11434`. O serviço `ollama-check` consulta `/api/tags` e
  impede o início da aplicação quando o modelo configurado não está disponível;
- credenciais CouchDB e API key são obrigatórias e permanecem somente no ambiente;
  o arquivo de exemplo não contém valores sensíveis. Tenant e cliente OIDC ficam
  desabilitados exclusivamente no perfil local `poc` configurado pelo Compose. A
  revisão final restringiu as portas da aplicação, CouchDB e Valkey a `127.0.0.1`,
  evitando exposição à rede local durante a execução sem OIDC;
- RED/GREEN do contrato `PocContainersConfigurationTest.ps1` cobriu Dockerfile,
  serviços, healthchecks, credenciais externas, ausência de imagem/volume Ollama e
  `docker compose config --no-interpolate`; a execução final terminou GREEN;
- a validação fora dos testes confirmou `ollama list`, acesso a `/api/tags` a partir
  da imagem da aplicação e duas respostas HTTP 200 do `llama3.2:3b` no fluxo real.
  O contrato dos prompts passou a exigir resumo/justificativas preenchidos, evidência
  literal ou nula e a chave canônica `confianca`; o DTO aceita também o alias
  `confiança` observado na borda do modelo;
- a saída real permaneceu segura: o revisor alterou nomes autoritativos de três
  apontamentos, a validação determinística recusou a saída e o fluxo produziu
  `FALLBACK_TECNICO` completo para revisão humana, sem corrigir semanticamente a
  resposta do modelo de forma silenciosa;
- o modelo também copiou descrições do checklist no campo de evidência, apesar da
  proibição do prompt. Nesta execução a divergência de nomes já forçou fallback,
  mas o mapper ainda não confronta evidência com o texto original; essa limitação
  fica registrada para os guardrails da Task 9.2 e não é tratada como evidência
  confiável desta PoC;
- a análise sintética `DOC-POC-CONTAINER-004`, instância
  `01KZC3JF8T9Q8SJ3SQ9MCFVH48` e correlação
  `05325a8c-f5a1-42f0-9e25-a4e8f614bbea`, percorreu
  `EM_PROCESSAMENTO -> AGUARDANDO_REVISAO -> CONCLUIDA`, com seis apontamentos
  finais. Depois do restart somente de `simtr-hub`, readiness voltou `UP` e a mesma
  análise concluída, correlação e revisão foram recuperadas, mantendo CouchDB e
  Valkey ativos. No encerramento, `docker compose down` removeu somente containers
  e rede; nenhum container da PoC permaneceu e o volume
  `simtr-hub-poc_couchdb-conformidade-data` foi preservado;
- `mvn -q test` aprovou 573 testes, 0 falhas, 0 erros e 4 ignorados. Os ignorados são
  gates explícitos: um Cosmos real, duas fases da prova entre JVMs e um Ollama real;
  a integração Ollama desta task foi comprovada externamente à suíte padrão;
- a primeira tentativa do checkpoint foi interrompida por dois testes intermitentes
  de captura do `InMemorySpanExporter`; ambos passaram juntos na repetição focada,
  sem alteração de produção. A repetição completa do checkpoint terminou
  `COMPLIANT`: 240 issues atuais contra 240 no baseline, 0 novas, nenhuma issue
  `HIGH`, `BLOCKER` ou `CRITICAL`, cobertura de 85,1%, duplicação de 2,5% e decisão
  `NOT_REQUIRED`. Depois da restrição das portas ao loopback, o checkpoint final
  repetiu a suíte e preservou integralmente essas métricas;
- a Task 8.1 está tecnicamente concluída. O próximo item é a Task 8.2; acesso do
  Kubernetes ao Ollama do host, Leases, duas réplicas e failover não fazem parte
  desta evidência.

### Task 8.2 — Identidade durável em Kubernetes

- o ambiente kind local executou duas réplicas da aplicação, CouchDB `3.5.2` em
  `StatefulSet` com PVC `Bound`, Valkey compartilhado e acesso ao Ollama/modelo do
  host pelo preflight de cada pod; Cosmos e seu emulador permaneceram ausentes;
- o limite de filas Erlang do CouchDB foi fixado em `ERL_FLAGS=+Q 65536`, conforme
  o troubleshooting oficial, evitando o OOM observado no nó kind sem alterar o
  volume persistente;
- build, contratos estáticos de Compose/Kubernetes e matriz Quarkus focada ficaram
  GREEN antes da prova final;
- o primeiro diagnóstico encontrou duas Leases de membro e uma de líder ainda no
  pool default `flow-pool`: o perfil `poc,kubernetes` habilitava Lease/readiness em
  runtime, mas o nome do pool da extensão Flow `0.10.2` já havia sido consumido na
  augmentação;
- RED/GREEN do `PocKubernetesConfigurationTest.ps1` passou a exigir
  `-Dquarkus.flow.durable.kube.pool.name=simtr-hub-conformidade` no `mvn package`;
  a imagem reconstruída recebeu o ID
  `sha256:7521234bef9546299c586ff9ac7a6237755c03e13d8eb6fe1b4746ad54d0b1a8`;
- a execução real revelou e protegeu três condições do roteiro: tag local fixa
  exige rollout inicial depois de `kind load docker-image`; pods antigos com
  `deletionTimestamp` podem coexistir brevemente depois de `rollout status`; e
  `kubectl auth can-i` usa o recurso qualificado
  `leases.coordination.k8s.io` e retorna exit code 1 para a resposta válida `no`;
- o roteiro final terminou GREEN: duas Leases de membro estáveis do pool
  `simtr-hub-conformidade` foram adquiridas, o pod com ServiceAccount `default` sem
  RBAC não ficou Ready e expôs `Lease Acquisition=DOWN` com
  `leaseAcquired=false`; o rolling restart substituiu os dois pods preservando os
  nomes das Leases e vinculando-as aos holders novos;
- o snapshot final confirmou duas réplicas `1/1 Ready`, CouchDB e Valkey `1/1`, PVC
  CouchDB `Bound`, duas Leases de membro e uma de líder com holders atuais. As três
  Leases antigas `flow-pool` ficaram sem holder e foram preservadas, sem exclusão
  destrutiva desnecessária;
- esta evidência comprova identidade, readiness e rolling restart, mas não roteia
  revisão entre réplicas nem interrompe o owner de uma instância. Retomada
  cross-pod e failover permanecem exclusivamente na Task 8.3; ADR-0010 continua
  `Proposto`;
- o baseline Sonar da sessão precisou ser inicializado depois das alterações desta
  retomada; por isso sua comparação isolada não prova ausência de regressão desde o
  início da Task 8.2. A suíte executada pelo baseline/checkpoint aprovou 573 testes,
  0 falhas, 0 erros e 4 gates opt-in ignorados;
- o checkpoint formal terminou `COMPLIANT`: 240 issues atuais contra 240 no
  baseline tardio, 0 novas, nenhuma issue `HIGH`, `BLOCKER` ou `CRITICAL`, cobertura
  de 85,1%, duplicação de 2,5% e decisão `NOT_REQUIRED`. Essas métricas coincidem
  com o último checkpoint registrado da Task 8.1 e constituem evidência
  compensatória consistente, sem eliminar a limitação temporal do baseline.

### Task 8.3 — Retomada cross-pod e failover

- RED: `PocKubernetesFailoverTest.ps1` falhou pela ausência de um roteiro
  reproduzível que identificasse owner/revisor por Lease, dirigisse POST/PUT/GET a
  pods distintos, substituísse o owner em dois pontos e comprovasse conclusão
  única;
- GREEN estático: `validar-failover-poc-kubernetes.ps1` passou a executar duas
  análises sintéticas e o contrato PowerShell confirmou chamadas dirigidas,
  `holderIdentity`, nome/UID/`resourceVersion` da Lease, checkpoint, correlações,
  revisão repetida e resultado final;
- a primeira invocação foi encerrada pelo timeout curto do executor antes de
  qualquer failover. A segunda caracterizou um defeito do próprio harness depois
  de alcançar `AGUARDANDO_REVISAO`: a inspeção usava `valkey-cli` no pod da
  aplicação. O alvo foi corrigido para `deployment/valkey`; nenhuma dessas duas
  tentativas interrompeu pods;
- a execução real final terminou GREEN em 84 segundos. No cenário
  `owner-interrompido-antes-da-revisao`, o POST entrou em
  `simtr-hub-cc7775678-w6nl8`, a revisão foi enviada por
  `simtr-hub-cc7775678-zdr9s` e `simtr-hub-cc7775678-l6lhn` reassumiu a mesma Lease;
  o checkpoint permaneceu presente no Valkey antes do PUT;
- no cenário `owner-interrompido-apos-a-revisao`, o POST entrou em
  `simtr-hub-cc7775678-l6lhn`, o PUT `202` entrou por
  `simtr-hub-cc7775678-zdr9s` e o owner foi removido imediatamente; o substituto
  `simtr-hub-cc7775678-sk78m` reassumiu a mesma Lease e concluiu a instância;
- as instâncias `01KZETFDYYT3RYM6K2Y0M3Y7J0`/
  `cbc2809b-05e9-4e81-87ea-d3fd65a4c959` e
  `01KZETRTA4GCVA95N73ZGAS04F`/
  `7425ff9e-96b3-44b9-ae44-787271ff7964` terminaram `CONCLUIDA` e ficaram
  consultáveis diretamente pelas duas réplicas. Em cada caso, repetir a revisão
  retornou `409` e o CouchDB continha exatamente uma emissão
  `br.gov.caixa.simtr.conformidade.analise.concluida.v1` para o `instanceId`;
- o snapshot final manteve duas réplicas Ready, vinculadas às Leases estáveis
  `flow-pool-member-simtr-hub-conformidade-00` e `-01`, com UIDs respectivamente
  `6b427657-351a-44e7-8c3a-2c4f4fcbc27b` e
  `a615f5e8-6136-47af-befb-45a8f9153ece`; CouchDB, PVC, Valkey e Ollama não foram
  reiniciados durante os failovers;
- as duas análises sintéticas deixadas em espera pelas invocações interrompidas
  foram concluídas pela própria API, sem apagar documentos, checkpoints, Leases ou
  PVC. O cluster kind permanece ativo e sem script ou teste em execução;
- ADR-0010 continua `Proposto` até decisão humana posterior. No encerramento da
  Task 8.3, a suíte completa e o checkpoint Sonar ainda não haviam sido executados;
  seus resultados posteriores estão registrados na Task 8.4 abaixo.

### Task 8.4 — Suíte e checkpoint Sonar do incremento 8

- os contratos `PocContainersConfigurationTest.ps1`,
  `PocKubernetesConfigurationTest.ps1` e `PocKubernetesFailoverTest.ps1` terminaram
  GREEN; `git diff --check` não encontrou erro, apenas os avisos LF/CRLF já
  conhecidos no worktree Windows;
- `mvn -q test` aprovou 573 testes, com 0 falhas, 0 erros e 4 gates opt-in
  ignorados; a execução independente durou 137,4 segundos;
- `validar-checkpoint-sonarqube.ps1` executou `clean verify`, SonarScanner e
  Compute Engine completos e terminou `COMPLIANT`: 240 issues atuais contra 240 no
  baseline, 0 novas, nenhuma issue `HIGH`, `BLOCKER` ou `CRITICAL`, cobertura de
  85,1%, duplicação de 2,5% e decisão `NOT_REQUIRED`;
- o baseline local capturado às 15:48 de 2026-08-07 continua tardio em relação às
  alterações históricas da Task 8.2, mas antecede o fingerprint executável criado
  na Task 8.3; por isso a comparação deste checkpoint é válida para o novo roteiro
  de failover e seu contrato;
- o cluster kind permanece ativo com duas réplicas Ready, CouchDB e Valkey Ready,
  PVC CouchDB `Bound` e Leases atuais; nenhum commit foi realizado. O próximo item
  formal é a Task 9.1.

### Task 9.1 — Página estática, polling e revisão humana

- RED: `mvn -q "-Dtest=AnaliseConformidadePaginaEstaticaQuarkusTest" test`
  executou 4 testes e falhou nos quatro porque os recursos estáticos não existiam e
  `/poc-conformidade/` devolvia `404`;
- GREEN: a página passou a ser servida em `/poc-conformidade/` com HTML semântico,
  CSS responsivo e JavaScript puro, sem framework, CDN, `localStorage` ou
  `sessionStorage`; o mesmo teste focado aprovou 4 testes sem falhas ou erros;
- o formulário exige documento, checklist, versão e texto com limite de 20.000
  caracteres; as cinco identidades permanecem visíveis em elementos somente leitura;
- a revisão permite alterar somente parecer, justificativa e evidência por
  apontamento, além da observação geral; identificador, nome e confiança são copiados
  do resultado preliminar e não são renderizados como controles editáveis;
- o cliente usa `POST`, `GET` e `PUT` no contrato existente, mantém um único timer,
  consulta a cada 1.500 ms, para ao abrir a revisão ou atingir estado terminal e
  reinicia o polling depois do `PUT`;
- os status HTTP `400`, `404`, `409`, `422` e `503` são convertidos em mensagens
  públicas fixas; o corpo de erro não é renderizado e detalhes internos não aparecem;
- como o MCP Chrome DevTools não estava configurado, a validação da skill de browser
  usou o Chrome do sistema em contexto Playwright isolado e um servidor HTTP efêmero
  local, ambos encerrados na própria execução;
- o roteiro real no navegador comprovou
  `POST -> GET -> GET -> PUT -> GET -> CONCLUIDA`, intervalo de 1.503 ms, as cinco
  identidades, campos imutáveis preservados no `PUT`, os cinco status de erro,
  árvore acessível, labels, ordem inicial de foco, console limpo no fluxo feliz e
  ausência de exceções JavaScript;
- screenshots desktop inicial/final e mobile foram inspecionados; a viewport de
  360 px não apresentou overflow horizontal e o carregamento local medido terminou
  em 39 ms;
- a sintaxe de `app.js` foi validada pelo parser JavaScript do Node; `git diff
  --check` terminou sem erro;
- `mvn -q test` terminou com código 0 em 128,9 s; o `clean verify` do checkpoint
  registrou 136 relatórios Surefire, 577 testes, 0 falhas, 0 erros e 4 gates opt-in
  ignorados;
- `validar-checkpoint-sonarqube.ps1` terminou com código 0 e situação técnica
  `COMPLIANT`: 240 issues atuais contra 240 no baseline, nenhuma nova ou bloqueante,
  cobertura de 85,1%, duplicação de 2,5% e decisão `NOT_REQUIRED`;
- todos os processos efêmeros de teste foram encerrados; o cluster kind permanece
  ativo. A Task 9.2 não foi iniciada porque depende do checkpoint humano dos valores
  observáveis.

### Checkpoint humano da Task 9.2 — valores observáveis

- GO humano explícito recebido em 2026-08-07 para iniciar a Task 9.2;
- a telemetria pode conter somente IDs, backend, operação, resultado, indicador de
  replay e contexto de trace;
- health/readiness devem distinguir backend documental, Redis/Valkey e Lease
  Kubernetes;
- texto, prompt, resposta, revisão, evidência, credenciais e documentos são
  absolutamente proibidos na nova telemetria;
- perguntas operacionais que os sinais devem responder: qual backend/operação
  falhou para uma análise; se o feed está em replay ou o cursor/checkpoint está
  paralisado; qual das três dependências está impedindo readiness; e como seguir a
  análise pelo trace sem expor payload.

### Task 9.2 — Observabilidade e guardrails arquiteturais

- a persistência documental e os feeds nativos CouchDB/Cosmos passaram a emitir
  spans e logs estruturados com somente IDs, backend, operação, resultado, replay,
  cursor/lease e contexto de trace; payload, texto, prompt, resposta, revisão,
  evidência, credencial e documento permanecem ausentes da telemetria;
- as operações reativas mantêm o span atual até a terminação do `Uni`, propagam a
  falha original e registram somente resultado sanitizado, sem anexar exceção ou
  conteúdo sensível ao log/span;
- readiness passou a identificar separadamente backend documental, Redis/Valkey e
  Lease Kubernetes, preservando o estado dos checks delegados;
- ArchUnit ganhou provas negativas para impedir que o núcleo de conformidade dependa
  de DTO do Ollama, SDK Cosmos ou CloudEvent; as violações sintéticas permanecem
  confinadas ao código de teste;
- um RED reproduzido em `RepositorioDocumentalObservavelTest` mostrou que `parentId`
  é MDC permitido, mas não obrigatório em span raiz; a asserção passou a separar
  campos permitidos de obrigatórios sem relaxar a proibição de dados sensíveis;
- o baseline limpo do `HEAD` foi produzido fora de `target/` e permaneceu `READY`:
  240 issues, cobertura 85,1% e duplicação 2,5%; o pacote inválido sob `target/`, que
  havia retornado métricas zero, não foi reutilizado;
- o primeiro checkpoint do incremento ficou `NON_COMPLIANT`: 257 issues, 17 novas,
  4 severas, cobertura 82,5% e duplicação 2,6%; a decisão humana
  `ContinuarAjustes` foi registrada pelo script;
- as 17 ocorrências foram corrigidas com constantes, nomes internos sem colisão,
  captura de exceções operacionais, lambdas simples e provas ArchUnit equivalentes;
  o déficit de cobertura foi rastreado à retirada de `@QuarkusTest` de cinco testes
  MTR, que passavam sem contribuir para o `quarkus-jacoco`;
- o RED conjunto desses cinco testes reproduziu cinco falhas de isolamento, enquanto
  cada classe passou sozinha; perfis Quarkus distintos preservaram os contratos,
  tornaram o conjunto determinístico e restauraram a instrumentação de cobertura;
- o segundo checkpoint reduziu o resultado a 242 issues, 2 novas `MINOR`
  `java:S1481`, cobertura 85,3% e duplicação 2,6%; uma nova decisão humana
  `ContinuarAjustes` foi registrada antes da correção;
- o checkpoint final terminou `COMPLIANT`: 240 issues contra 240 no baseline,
  nenhuma issue nova ou severa, cobertura 85,3%, duplicação 2,6% e decisão
  `NOT_REQUIRED`;
- o `clean verify` final registrou 138 relatórios Surefire, 585 testes, 0 falhas,
  0 erros e 4 gates opt-in ignorados; a Task 9.3 não foi iniciada.

### Planejamento da evolução durável

- o usuário autorizou planejar a mudança para CouchDB nos dados de negócio e
  Redis/Valkey nos checkpoints, sem Kafka;
- ADR-0010 foi criado como `Proposto` e indexado sem alterar o status do ADR-0009;
- a especificação foi emendada para separar sistema de registro, checkpoint e
  contrato CloudEvent;
- o plano preserva a Task 6.2 como próximo item e divide a evolução em contrato,
  CouchDB, checkpoint, `_changes`, containers, Kubernetes, UI e documentação final;
- o contrato proposto expõe `correlationId`, `instanceId`,
  `identificadorDocumento`, `identificadorChecklist` e `versaoChecklist`;
- decisão humana explícita `C4 GO`, recebida em 2026-07-25, autorizou a arquitetura,
  o contrato público, a segurança persistente e o comportamento observável descritos;
- conforme o critério aprovado, ADR-0010 continua `Proposto` até a Task 8.3 comprovar
  a retomada cross-pod/failover; C4 não foi interpretado como aceitação antecipada;
- nenhum código, POM, propriedade executável, script, consolidado arquitetural ou
  formato derivado foi alterado nesta etapa documental.

### Checkpoint C5 — Persistência documental por ambiente

- o usuário confirmou em 2026-07-26 a recomendação de manter Apache CouchDB em DES
  e usar Azure Cosmos DB for NoSQL em PRD;
- a aplicação dependerá de portas de persistência orientadas às necessidades do
  domínio; `_rev`, `_etag`, `_changes`, Change Feed e DTOs dos fornecedores ficarão
  confinados aos respectivos adapters;
- os dois adapters deverão cumprir o mesmo contrato executável para documentos,
  projeção, idempotência, concorrência otimista e falhas;
- CouchDB usará `_rev` e `_changes`; Cosmos DB for NoSQL usará `_etag`/`If-Match`,
  Azure Cosmos DB Java SDK v4 e Change Feed Processor;
- uma integração opt-in contra Cosmos DB Emulator ou conta Cosmos não produtiva será
  gate obrigatório antes da promoção para PRD; o emulador não será tratado como
  substituto completo do serviço;
- MongoDB não foi selecionado: seu protocolo corresponde ao Azure Cosmos DB for
  MongoDB, não ao Azure Cosmos DB for NoSQL escolhido;
- a forma de autenticação do adapter Cosmos em PRD não foi fixada por suposição e
  exigirá checkpoint de segurança antes da configuração executável;
- ADR-0010 permanece `Proposto`; C5 autoriza a implementação da arquitetura por
  ambiente, mas não antecipa sua aceitação final nem a prova multipod.

### Checkpoint C6 — Assincronia, autenticação e ambientes locais

- o usuário confirmou em 2026-07-26 `Uni<Void>` para as escritas e
  `Uni<Optional<VisaoAnaliseConformidade>>` para as leituras da porta documental;
- o usuário ampliou a decisão em 2026-07-26: usar Quarkus reativo sempre que
  possível, com `Uni<VisaoAnaliseConformidade>` nas portas de início/consulta,
  `Uni` nos recursos REST, `Uni<Void>` na projeção interna e callbacks Flow
  retornando `Uni` diretamente;
- a inspeção do artefato efetivo `quarkus-flow:0.10.2` comprovou o conversor
  `Uni2CompletableFuture`; a aplicação não fará conversão manual nesses callbacks;
- o adapter Cosmos em PRD usará Microsoft Entra ID por
  `DefaultAzureCredential`, com Managed Identity ou Workload Identity e RBAC de
  plano de dados de menor privilégio;
- chave e connection string do Cosmos ficam proibidas em PRD;
- `mvn quarkus:dev` deverá iniciar automaticamente CouchDB `3.5.2` por
  `compose-devservices.yml`, com health check, inicialização idempotente do banco e
  volume nomeado preservado entre reinícios do Quarkus;
- testes CouchDB permanecem isolados em containers efêmeros e não compartilham o
  volume de DES;
- o Compose completo de uma réplica permanece planejado para a Task 8.1;
- o ambiente Kubernetes local posterior usará kind ou k3d, duas réplicas da
  aplicação, CouchDB em `StatefulSet` de um pod com PVC, Redis/Valkey e Ollama;
  Cosmos e seu emulador não serão implantados nesses pods;
- ADR-0010 continua `Proposto` até a prova cross-pod/failover e decisão humana
  posterior.

## Decisões humanas

| Checkpoint | Status | Data | Evidência | Aprovador |
|---|---|---|---|---|
| C0 | APROVADO | 2026-07-24 | GO explícito registrado pelo usuário no chat | Usuário |
| Baseline Sonar | REGISTRADO | 2026-07-24 | Não existe `sonar/`; baseline local inicializado com servidor `UP` e script concluído com código 0 | — |
| C1 | APROVADO | 2026-07-24 | `C1 GO` explícito após revisão de `compatibilidade.md` | Usuário |
| Sonar incremento 1 | COMPLIANT | 2026-07-24 | 0 issues novas; cobertura 87,8%; duplicação 3,7%; decisão humana não requerida | — |
| CA | APROVADO | 2026-07-24 | `CA GO` explícito; ADR-0009 alterado de `Proposto` para `Aceito` | Usuário |
| Sonar incremento 2 | COMPLIANT | 2026-07-24 | `ContinuarAjustes` registrado; rerun com 0 issues novas, cobertura 87,2% e duplicação 3,6% | Usuário |
| C2 | APROVADO | 2026-07-24 | `registrar C2` explícito para o contrato, segurança da entrada, campos imutáveis e UI descritos no plano | Usuário |
| Sonar incremento 3 | COMPLIANT | 2026-07-24 | `ContinuarAjustes` registrado; rerun com 0 issues novas, cobertura 86,7% e duplicação 3,3% | Usuário |
| Sonar incremento 4 | COMPLIANT | 2026-07-24 | `ContinuarAjustes` registrado; rerun com 0 issues novas, cobertura 86,7% e duplicação 3,3% | Usuário |
| C3 | APROVADO | 2026-07-24 | `C3 GO` explícito para agente sequencial, prompt/structured output, FT, fallback e telemetria sanitizada descritos no plano | Usuário |
| Sonar incremento 5 | COMPLIANT | 2026-07-24 | Duas decisões `ContinuarAjustes` registradas; rerun final com 0 issues novas, cobertura 86,2% e duplicação 3,1% | Usuário |
| C4 | APROVADO | 2026-07-25 | `C4 GO` explícito; autoriza a implementação, mas ADR-0010 permanece `Proposto` até a prova cross-pod da Task 8.3 | Usuário |
| Sonar incremento 6 | UNVERIFIED | 2026-07-25 | Processo sem `SONAR_TOKEN`; usuário autorizou continuar sem checkpoint, sem aprovação ou reprovação técnica | Usuário |
| Revalidação Sonar incremento 6 | COMPLIANT | 2026-07-26 | Baseline da sessão e checkpoint completos; 0 issues novas, cobertura 86,2%, duplicação 3,1% e decisão `NOT_REQUIRED` | — |
| Sonar spike Task 7.1 | COMPLIANT | 2026-07-26 | 0 issues novas; cobertura 86,2%; duplicação 3,1%; decisão `NOT_REQUIRED` | — |
| Sonar Task 7.2 | COMPLIANT | 2026-07-26 | 0 issues novas; cobertura 86,2%; duplicação 3,0%; decisão `NOT_REQUIRED` | — |
| Tipo de `identificadorDocumento` | APROVADO | 2026-07-26 | Usuário confirmou manter `String`; tipos futuros ausentes ou ambíguos exigem pergunta e registro antes da especificação | Usuário |
| Tipo de `versaoSchema` | APROVADO | 2026-07-26 | Usuário definiu `small int`; representação `Short` no Java e número inteiro no JSON | Usuário |
| Tipo de hash canônico | APROVADO | 2026-07-26 | Usuário definiu `String`; adotada representação SHA-256 hexadecimal minúscula com 64 caracteres | Usuário |
| C5 | APROVADO | 2026-07-26 | Usuário aceitou a recomendação: CouchDB em DES, Azure Cosmos DB for NoSQL em PRD, porta neutra, contratos compartilhados e validação Cosmos obrigatória antes da promoção | Usuário |
| C6 | APROVADO | 2026-07-26 | Usuário confirmou Quarkus reativo/`Uni` em toda borda suportada, Cosmos por Entra ID com identidade gerenciada/federada e RBAC mínimo, CouchDB automático no `quarkus:dev` e Kubernetes local posterior | Usuário |
| Sonar Task 7.3 — primeira execução | CONTINUAR_AJUSTES | 2026-07-26 | 21 issues novas, 10 `CRITICAL`, cobertura 80,1%, duplicação 2,9%; decisão registrada pelo script | Usuário |
| Sonar Task 7.3 — reexecução | COMPLIANT | 2026-07-26 | 0 issues novas, cobertura 85,1%, duplicação 2,9% e decisão `NOT_REQUIRED` | — |
| Sonar Task 7.3 — propagação de `Uni` | UNVERIFIED | 2026-07-26 | Script encerrou antes da análise porque o processo atual não herdou `SONAR_TOKEN`; suíte local completa aprovada | — |
| Sonar Task 7.3 — adapter Cosmos | UNVERIFIED | 2026-07-26 | Script encerrou antes da análise porque o processo atual não herdou `SONAR_TOKEN`; 496 testes locais e ArchUnit aprovados | — |
| Sonar Task 7.3 — seleção e Dev Services | UNVERIFIED | 2026-07-26 | Script encerrou antes da análise porque o processo atual não herdou `SONAR_TOKEN`; 502 testes locais, ArchUnit e restart CouchDB aprovados | — |
| Sonar Task 7.3 — retorno | CONTINUAR_AJUSTES | 2026-08-02 | Baseline local atualizado com 0 issues novas, cobertura 83,2% e duplicação 2,8%; decisão registrada pelo script | Usuário |
| Sonar Task 7.3 — ajuste de cobertura | COMPLIANT | 2026-08-02 | 0 issues novas; cobertura 85,3%; duplicação 2,8%; decisão `NOT_REQUIRED` | — |
| C7 | APROVADO | 2026-08-02 | `C7 GO` explícito para usar `correlationid` somente na extensão CloudEvent e preservar `correlationId` nas demais bordas | Usuário |
| Contrato do `data` referencial | APROVADO | 2026-08-02 | Usuário confirmou `documentoRef` `String` opaca e determinística, `hashConteudo` `String` SHA-256 e `versaoSchema` `Short` inicialmente `1` | Usuário |
| Sonar Task 7.3 — `EventPublisher` referencial | CONTINUAR_AJUSTES | 2026-08-03 | 0 issues novas; cobertura 85,0%; duplicação 2,7%; 2 issues `CRITICAL`/HIGH `java:S1192` já presentes no baseline; decisão registrada pelo script | Usuário |
| Sonar Task 7.3 — ajuste de constantes | COMPLIANT | 2026-08-03 | 240 issues atuais contra 242 no baseline; 0 issues novas ou bloqueantes; cobertura 85,0%; duplicação 2,7%; decisão `NOT_REQUIRED` | — |
| C8 | APROVADO | 2026-08-03 | CouchDB real no ambiente local; Cosmos validado por contrato determinístico e SDK mockado; integração real preservada como gate externo pré-PRD | Usuário |
| C9 | APROVADO | 2026-08-05 | Usuário autorizou a recomendação para a Task 7.6: alinhar o teste de cursor ao harness Quarkus, executar spike pareado Flow `0.13.0`/LangChain4j `1.12.0` e provar restart entre JVMs, sem antecipar cross-pod/failover | Usuário |
| Escopo Redis/Valkey da PoC | APROVADO | 2026-08-06 | Redis/Valkey compartilhado permanece disponível; perda, restart, HA e recuperação do próprio serviço ficam fora das Tasks 8.1/8.3 | Usuário |
| Ollama local na Task 8.1 | APROVADO | 2026-08-06 | Compose reutiliza Ollama/modelos instalados no host por `host.docker.internal`; não baixa outra cópia. A integração Kubernetes será decidida na Task 8.2 | Usuário |
| Segurança OIDC da Task 8.1 | APROVADO | 2026-08-06 | Desabilitar tenant OIDC e cliente OIDC padrão somente no Compose local da PoC; `dev` e produção permanecem inalterados | Usuário |
| Sonar Task 8.2 — baseline tardio | REGISTRADO | 2026-08-07 | Baseline local inicializado depois das alterações da retomada; suíte com 573 testes, 0 falhas, 0 erros e 4 ignorados; não usado isoladamente para afirmar ausência de regressão | — |
| Sonar Task 8.2 — checkpoint | COMPLIANT | 2026-08-07 | 240 issues atuais contra 240 no baseline tardio, 0 novas ou bloqueantes, cobertura 85,1%, duplicação 2,5% e decisão `NOT_REQUIRED`; métricas idênticas às registradas na Task 8.1 | — |
| Sonar Task 8.4 — checkpoint | COMPLIANT | 2026-08-07 | 240 issues atuais contra 240 no baseline, 0 novas ou bloqueantes, cobertura 85,1%, duplicação 2,5% e decisão `NOT_REQUIRED`; baseline anterior ao roteiro executável da Task 8.3 | — |
| Sonar Task 9.1 — checkpoint | COMPLIANT | 2026-08-07 | 240 issues atuais contra 240 no baseline, 0 novas ou bloqueantes, cobertura 85,1%, duplicação 2,5% e decisão `NOT_REQUIRED`; 577 testes, 0 falhas, 0 erros e 4 ignorados | — |
| Checkpoint Task 9.2 — valores observáveis | APROVADO | 2026-08-07 | GO explícito para telemetria limitada a IDs, backend, operação, resultado, replay e trace; dados sensíveis absolutamente proibidos | Usuário |
| Sonar Task 9.2 — primeira execução | CONTINUAR_AJUSTES | 2026-08-09 | 257 issues atuais contra 240 no baseline, 17 novas, 4 severas, cobertura 82,5% e duplicação 2,6%; decisão registrada pelo script | Usuário |
| Sonar Task 9.2 — segundo checkpoint | CONTINUAR_AJUSTES | 2026-08-09 | 242 issues atuais contra 240 no baseline, 2 novas `MINOR`, cobertura 85,3% e duplicação 2,6%; decisão registrada pelo script | Usuário |
| Sonar Task 9.2 — ajuste final | COMPLIANT | 2026-08-09 | 240 issues atuais contra 240 no baseline, 0 novas ou severas, cobertura 85,3%, duplicação 2,6% e decisão `NOT_REQUIRED`; 585 testes, 0 falhas, 0 erros e 4 ignorados | — |
| Sonar Task 7.4 — primeira execução | CONTINUAR_AJUSTES | 2026-08-04 | 2 issues novas (`java:S5778` e `java:S1612`), cobertura 84,9% e duplicação 2,6%; decisão registrada pelo script | Usuário |
| Sonar Task 7.4 — primeiro ajuste | CONTINUAR_AJUSTES | 2026-08-04 | 241 issues atuais contra 242 no baseline; 0 issues novas ou bloqueantes; cobertura 84,9%; duplicação 2,6%; decisão registrada pelo script | Usuário |
| Sonar Task 7.4 — ajuste final | COMPLIANT | 2026-08-04 | 240 issues atuais contra 242 no baseline; 0 issues novas ou bloqueantes; cobertura 85,1%; duplicação 2,6%; decisão `NOT_REQUIRED` | — |
| Sonar Task 7.6 — primeira execução | CONTINUAR_AJUSTES | 2026-08-05 | Baseline da sessão inicializado tardiamente: 256 issues atuais, 3 `CRITICAL`/HIGH, cobertura 83,0% e duplicação 2,5%; comparação compensatória com a análise anterior de 240 issues identificou 16 issues abertas criadas desde 2026-08-04; decisão registrada pelo script | Usuário |
| Sonar Task 7.6 — ajuste final | COMPLIANT | 2026-08-06 | Baseline da sessão novamente tardio; checkpoint com 240 issues atuais contra 240 no baseline, 0 novas ou bloqueantes, cobertura 85,2%, duplicação 2,5% e decisão `NOT_REQUIRED`; comparação compensatória com 256/240 confirma correção líquida das 16 issues | — |
| CF | PENDENTE | — | Aguardará evidências finais | — |

## Regras de avanço

- somente o usuário altera um checkpoint humano para aprovado, rejeitado, aceito
  excepcionalmente ou encerrado;
- depois do GO, executar somente o próximo item pendente;
- mudança de escopo atualiza primeiro `plan.md` e este checklist;
- nenhuma incompatibilidade autoriza upgrade ou pin de dependência sem C1;
- qualquer `NON_COMPLIANT` Sonar exige decisão humana registrada pelo script;
- teste real com Ollama é opt-in; a suíte padrão usa agente falso.
- ADR-0010 permanece `Proposto` e ADR-0009 permanece `Aceito` até decisão humana;
- C6 proíbe chave/connection string do Cosmos em PRD e exige Entra ID,
  Managed/Workload Identity e RBAC de plano de dados de menor privilégio;
- falha da prova cross-pod interrompe a evolução multipod e exige novo checkpoint,
  sem adoção automática de Kafka.
