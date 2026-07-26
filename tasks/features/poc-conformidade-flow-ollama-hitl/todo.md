# Checklist: PoC de conformidade com Quarkus Flow, Ollama e Human-in-the-Loop

## Estado

- **Branch:** `feature/poc-conformidade-flow-ollama-hitl`
- **Escopo:** concluir baseline HITL volátil e evoluir para CouchDB + Redis/Valkey +
  `_changes` + containers/Kubernetes após C4
- **Próximo item:** 7.2 — evoluir o contrato de identidades; permanece pendente e
  fora do escopo desta retomada
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
- [ ] 7.2 Evoluir contrato com `correlationId`, `identificadorDocumento`, checklist e
  versão;
- [ ] 7.3 Persistir documentos de negócio e projeção no CouchDB;
- [ ] 7.4 Reduzir contexto e habilitar checkpoint Redis/Valkey;
- [ ] 7.5 Completar `EventPublisher` + `_changes` -> `EventConsumer` -> CloudEvent
  com idempotência;
- [ ] 7.6 Executar suíte e checkpoint Sonar do incremento 7;
- [ ] 8.1 Empacotar app, CouchDB, Redis/Valkey e Ollama para uma réplica;
- [ ] 8.2 Configurar Kubernetes Leases, readiness e duas réplicas;
- [ ] 8.3 Provar retomada cross-pod e failover;
- [ ] 8.4 Executar suíte e checkpoint Sonar do incremento 8;
- [ ] 9.1 Criar página estática com polling e cinco valores de identidade;
- [ ] 9.2 Fechar observabilidade e guardrails arquiteturais;
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
- falha da prova cross-pod interrompe a evolução multipod e exige novo checkpoint,
  sem adoção automática de Kafka.
