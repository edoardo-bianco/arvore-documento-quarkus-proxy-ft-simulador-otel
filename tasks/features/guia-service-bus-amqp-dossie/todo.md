# Checklist: criar guia de filas do dossiê com Azure Service Bus e AMQP

## Referência histórica deste recorte

Este arquivo preserva o planejamento e as decisões da feature documental anterior, encerrada
em 2026-09-02. Seus contratos, packages e autenticação não são o plano da implementação atual.
O [guia Service Bus](../../../doc/guias/guia-service-bus-amqp-dossie.md) foi alinhado ao
[plano de orquestrador/monitoramento](../orquestrador-monitoramento-service-bus/plan.md) e ao
[ADR-0010](../../../doc/adr/0010-extensao-quarkus-service-bus-connection-string-dev-services.md).
A continuidade fica nessa feature, sem reabrir os itens/decisões históricas abaixo.

## Estado

- **Branch:** `docs/planejar-guia-service-bus-amqp-dossie`
- **Escopo:** planejamento documental; nenhum código ou configuração executável
- **Próximo item:** nenhum — feature documental encerrada; implementação não solicitada

## Checklist

- [x] 0.1 Confirmar intenção: escrita na fila de entrada e consumo contínuo da fila de saída,
  acionado na chegada, no package `br.gov.caixa.simtr.dossie`;
- [x] 0.2 Ler arquitetura consolidada, índice e ADRs aplicáveis;
- [x] 0.3 Ler integralmente a análise da sincronização;
- [x] 0.4 Inspecionar Quarkus/Java/BOM, package `dossie`, configuração, guardrail e testes
  relacionados;
- [x] 0.5 Confirmar ausência de mensageria no estado atual;
- [x] 0.6 Verificar Quarkus/SmallRye AMQP, Azure SDK Java e Azure Service Bus em fontes oficiais;
- [x] 0.7 Registrar recorte, divergências, riscos, tarefas, verificações e checkpoints;
- [x] 0.8 Separar `tentativaAtual` funcional de `DeliveryCount` técnico e quarentena de DLQ;
- [x] C0 Revisar o plano e autorizar humanamente a criação do guia;
- [x] 1.1 Confirmar versão suportada de Quarkus/SmallRye para CBS `put-token`;
- [x] 1.2 Escolher autenticação Entra ID/Managed Identity ou SAS com permissões mínimas;
- [x] 1.2a Escolher cliente compatível mantendo Quarkus `3.33` — Azure SDK Java com
  `azure-messaging-servicebus` e `azure-identity`;
- [x] 1.3a Registrar a matriz candidata somente por fontes oficiais: Azure SDK BOM `1.3.8`,
  Service Bus `7.17.19` e Azure Identity `1.18.4`, com divergência das páginas de referência;
- [x] 1.3b Registrar a impossibilidade de provar resolução/compilação nesta branch documental e
  transferir a prova para a futura feature executável, depois do baseline Sonar aplicável;
- [x] 1.3c Registrar autenticação, TLS, sender e consumidor como `UNVERIFIED` por ausência de
  cliente instalado, namespace, filas e identidade disponibilizada por canal seguro;
- [x] 1.4 Verificar suporte real a mensagem agendada e à garantia
  `agendar próxima + Complete atual` na combinação de clientes Azure escolhida —
  `SUPPORTED_BY_OFFICIAL_API; UNVERIFIED_IN_ENVIRONMENT`;
- [x] 1.5 Criar ADR `Proposto` para a seleção do Azure SDK nesta borda e submetê-lo a C1;
- [x] C1 Aprovar contrato JSON, DTOs, configuração, plataforma, segurança, settlement,
  concorrência e health;
- [x] 2.1 Criar `doc/guias/guia-service-bus-amqp-dossie.md`;
- [x] 2.2 Documentar Azure SDK BOM, dependências, versões, topologia e pré-condições;
- [x] 2.3 Documentar DTOs de entrada/saída e JSON de duas propriedades;
- [x] 2.4 Documentar configuração dos clientes Azure, AMQP WebSockets/TLS e Entra ID sem segredo;
- [x] 2.5 Documentar publisher com `ServiceBusSenderAsyncClient` e composição do resultado;
- [x] 2.6 Documentar consumidor contínuo pelo cliente Azure aprovado, acionado na chegada e sem
  polling;
- [x] 2.7 Documentar settlement, redelivery, lock renewal, threads, back-pressure, concorrência e
  health;
- [x] 2.8 Documentar `tentativaAtual` como application property, separada de `DeliveryCount`;
- [x] 2.9 Documentar reagendamento progressivo, quarentena explícita e DLQ apenas técnica;
- [x] 2.10 Documentar verificação manual sem testes unitários;
- [x] 2.11 Documentar limitações e próximos incrementos;
- [x] C2 Revisar APIs, imports, properties, links, segurança e escopo do guia;
  - [x] C2.0 Executar a primeira revisão e apresentar os achados ao usuário;
  - [x] C2.1 Obter decisão humana sobre tamanho máximo, media type, validação dos campos e FQDN;
  - [x] C2.2 Corrigir o guia conforme as decisões aprovadas;
  - [x] C2.3 Repetir a revisão técnica e submeter novamente o checkpoint humano;
  - [x] C2.4 Corrigir a alegação obsoleta sobre ordem Maven e versionar os links Quarkus indicados;
  - [x] C2.5 Executar revisão focal e submeter C2 novamente à decisão humana;
  - [x] C2.6 Corrigir no guia `#configmapping` para `#config-mappings`;
  - [x] C2.7 Executar revisão focal final e submeter C2 novamente à decisão humana;
- [x] 3.1 Registrar handoff para branch/plano/GO da implementação futura;
- [x] 3.2 Não aplicável — usuário decidiu não prosseguir com a implementação;
- [x] CF Apresentar evidências e obter aceitação/encerramento humanos.

## Decisões humanas

| Checkpoint | Status | Data | Evidência | Aprovador |
|---|---|---|---|---|
| C0 — autorização do guia | APROVADO | 2026-08-30 | Usuário declarou `go` | usuário |
| C1 — contrato | APROVADO | 2026-08-31 | JSON mínimo com dois campos String, DTOs distintos, `tentativaAtual` fora do body e prazo individual desde o envio ao MTR | usuário |
| C1 — arquitetura/plataforma | APROVADO | 2026-08-31 | Quarkus 3.33 + Azure SDK BOM 1.3.8 documental; Processor na saída, clientes assíncronos na publicação/retry e ADR-0009 aceito | usuário |
| C1 — autenticação | APROVADO | 2026-08-31 | Usuário escolheu Microsoft Entra ID; SAS não será o mecanismo do guia | usuário |
| C1 — segurança restante | APROVADO | 2026-08-31 | Identidade atribuída pelo sistema com `ManagedIdentityCredential` em produção; RBAC mínimo por fila e identidade separada para o futuro processor | usuário |
| C1 — observabilidade/settlement | APROVADO | 2026-08-31 | Settlement manual, concorrência inicial 1, health explícito, intervalos progressivos, prazo individual, quarentena funcional e ausência de novos spans/métricas | usuário |
| C1 — emenda de transporte | APROVADO | 2026-08-31 | Usuário escolheu `AMQP_WEB_SOCKETS` em TCP 443, sem fallback automático para AMQP/TCP e sem proxy presumido | usuário |
| C2 — revisão técnica | APROVADO | 2026-09-02 | Após esclarecer que não havia ajuste adicional, o usuário declarou explicitamente `aprova C2`; C2 concluído e 3.1 liberado | usuário |
| C2.1 — contrato e segurança | APROVADO | 2026-09-01 | Body máximo 64 KiB; JSON com charset UTF-8 opcional; dois campos obrigatórios de até 256 caracteres e sem campos desconhecidos; FQDN Azure público restrito; falha fechada e DeadLetter sanitizado | usuário |
| CF — encerramento | ENCERRADO | 2026-09-02 | Usuário confirmou que deseja somente o guia, autorizou 3.2 como não aplicável e encerrou a feature documental | usuário |

## Evidências do planejamento

| Item | Data | Evidência |
|---|---|---|
| 0.1 | 2026-08-30 | Usuário delimitou Azure Service Bus via AMQP, escrita da entrada e consumo acionado na chegada da saída |
| 0.2 | 2026-08-30 | Arquitetura, `doc/adr/README.md` e ADRs 0001, 0003, 0004, 0006 e 0007 lidos |
| 0.3 | 2026-08-30 | Análise de 1.697 linhas lida; seção 18 usa Azure SDK e foi registrada como divergência |
| 0.4 | 2026-08-30 | Projeto confirmado em Quarkus 3.33.2.1, Java 25 e SmallRye Reactive Messaging 4.33.0 no BOM |
| 0.5 | 2026-08-30 | Nenhuma dependência/canal/anotação de mensageria encontrada no código atual |
| 0.6 | 2026-08-31 | SmallRye 4.33 não oferece CBS; Azure SDK Java documenta `TokenCredential`, sender assíncrono, Processor e Receiver assíncrono |
| 0.7 | 2026-08-30 | `plan.md` registra recorte documental, contrato proposto, compatibilidade como gate e ausência temporária de testes unitários |
| 0.8 | 2026-08-30 | Tentativa funcional separada de `DeliveryCount`; quarentena é resultado explícito e DLQ fica reservada a falhas técnicas |
| 1.1 | 2026-08-31 | Quarkus 3.33 LTS gerencia SmallRye 4.33.0 sem CBS `put-token`; o recurso surge no 4.36.0 como experimental, sem autorização para upgrade ou override |
| 1.2 | 2026-08-31 | Usuário escolheu Microsoft Entra ID; identidade atribuída pelo sistema, credencial produtiva e papéis mínimos foram posteriormente aprovados no C1 |
| 1.2a | 2026-08-31 | Usuário escolheu manter Quarkus 3.33 e usar `azure-messaging-servicebus` + `azure-identity`; SmallRye saiu desta borda |
| 1.3 — tentativa anterior | 2026-08-31 | Sem SDK, identidade injetada, namespace ou filas; nenhuma conexão/token foi tentado e o item continua pendente |
| 1.3a | 2026-08-31 | Repositório oficial aponta BOM 1.3.8 gerindo Service Bus 7.17.19 e Identity 1.18.4; páginas Learn mostram 7.17.17/1.18.5, portanto versões individuais não serão fixadas |
| 1.4 | 2026-08-31 | API oficial suporta schedule + Complete no mesmo `ServiceBusTransactionContext`; a fila única exige builder compartilhado e não usa cross-entity. Tier Standard/Premium, timeout de dois minutos e prova real continuam como gates; resultado `SUPPORTED_BY_OFFICIAL_API; UNVERIFIED_IN_ENVIRONMENT` |
| 1.5 | 2026-08-31 | ADR-0009 criado como `Proposto` e incluído no índice; registra Quarkus 3.33 + Azure SDK + Entra ID, alternativas rejeitadas, consequências e gates ainda submetidos ao C1 |
| C1 | 2026-08-31 | Usuário aprovou explicitamente a recomendação consolidada, acrescentou intervalos progressivos e prazo máximo individual desde o envio ao MTR, aceitou o ADR-0009 e manteve os gates técnicos pendentes |
| 2.1 | 2026-08-31 | Guia criado com estado `em elaboração`, objetivo, limites, decisões de partida e roteiro das 18 seções; conteúdo técnico dos itens 2.2 a 2.11 não foi antecipado |
| 2.2 | 2026-08-31 | Guia recebeu topologia, direções, RBAC, pré-condições Azure/rede, matriz BOM 1.3.8 e fragmentos Maven citados em fontes oficiais; Maven e ambiente permanecem não verificados |
| 2.3 | 2026-08-31 | Guia recebeu dois records independentes, JSON canônico com duas propriedades String, packages de borda e limites explícitos de validação; snippets permanecem não compilados |
| 2.4 | 2026-08-31 | Guia recebeu ConfigMapping, aliases externos, credenciais distintas por ambiente e builder compartilhado com `AMQP_WEB_SOCKETS` em TCP 443; compilação e ambiente permanecem não verificados |
| 2.5 | 2026-09-01 | Guia recebeu sender assíncrono da entrada, serialização JSON explícita e ponte lazy `Mono<Void>` -> `Uni<Void>` por `CompletionStage`, preservando resultado, falha e solicitação de cancelamento; compilação e ambiente permanecem não verificados |
| 2.6 | 2026-09-01 | Guia recebeu `ServiceBusProcessorClient` de longa duração na fila de saída, callbacks push, `start()` no ciclo de vida e leitura explícita do DTO sem polling na aplicação; settlement, concorrência e ambiente permanecem para 2.7 e para a futura prova executável |
| 2.7 | 2026-09-01 | Guia recebeu settlement manual, garantia `at-least-once`, renovação de lock, limites de prefetch/timeout, concorrência inicial `1`, threads/back-pressure, lifecycle CDI, readiness local e liveness independente do broker; compilação, carga e ambiente permanecem não verificados |
| 2.8 | 2026-09-01 | Guia passou a publicar `tentativaAtual=1` como `Integer` em application properties e separou contrato, validação e avanço funcional de `DeliveryCount`/`MaxDeliveryCount` técnicos e read-only; compilação e ambiente permanecem não verificados |
| 2.9 | 2026-09-01 | Guia recebeu política temporal versionada, nova mensagem funcional e composição transacional `schedule + Complete` na mesma fila; `QUARENTENA` ficou funcional e explícita, enquanto `DeadLetter`/DLQ/`MaxDeliveryCount` permaneceram técnicos; compilação e ambiente continuam não verificados |
| 2.10 | 2026-09-01 | Guia recebeu protocolo manual futuro em nove passos, incluindo rejeições contratuais controladas, health, aceite do envio, Service Bus Explorer, consumo acionado pela chegada, settlement/redelivery e inspeção segura de logs; não houve testes, conexão ou execução, portanto o resultado permanece `PROTOCOL_DOCUMENTED; NOT_EXECUTED` |
| 2.11 | 2026-09-01 | Guia separou decisões documentadas de provas ausentes, proibiu alegações produtivas, organizou gates e quatro incrementos futuros sem antecipar o handoff; conteúdo recebeu as correções C2.2, mas C2.3 e a execução permanecem pendentes |
| C2.0 | 2026-09-01 | Revisão inicial confirmou APIs e composição, mas encontrou materialização sem limite, ausência de validação de media type/DTO, publisher sem precondição explícita e FQDN sem restrição ao domínio aprovado; usuário escolheu Continuar ajustes |
| C2.1 | 2026-09-01 | Usuário aprovou limites e validações da borda; o próximo incremento permitido é a correção exclusivamente documental C2.2 |
| C2.2 | 2026-09-01 | Guia corrigido com records estritos, limite de 64 KiB, media type/JSON fechados, leitura limitada antes da materialização, FQDN Azure público, DeadLetter sanitizado, roteiro negativo e gates alinhados; snippets e ambiente permanecem não verificados |
| C2.3 | 2026-09-01 | Segunda revisão confirmou as correções de contrato e segurança, as APIs centrais e a integridade estrutural; identificou como correção obrigatória remover a alegação obsoleta de que Service Bus deve preceder Azure Identity no POM; C2 foi submetido novamente e aguarda decisão humana |
| decisão após C2.3 | 2026-09-01 | Usuário declarou `Continuar ajustes`; C2 não foi aprovado e o próximo incremento permitido é a correção exclusivamente documental C2.4 |
| C2.4 | 2026-09-02 | Guia passou a tratar a ordem Maven apenas como apresentação, registrou que o problema histórico foi resolvido em `azure-identity:1.2.1`, fixou em `3.33` os links Quarkus de Config Mappings e lifecycle e atualizou o estado para C2.5 pendente; nenhuma versão, dependência ou decisão arquitetural mudou |
| C2.5 | 2026-09-02 | Revisão focal confirmou a remoção da alegação Maven, HTTP 200 nas páginas oficiais e anchor de lifecycle válido; encontrou `#configmapping` inexistente, sendo `#config-mappings` o identificador oficial; 48 cercas pareadas, cinco links relativos válidos e diff check limpo; C2 submetido novamente sem aprovação inferida |
| decisão após C2.5 | 2026-09-02 | Usuário declarou `Continuar ajustes`; C2 permanece em ajuste e o próximo incremento permitido é a correção documental focal C2.6, seguida da revisão C2.7 |
| C2.6 | 2026-09-02 | Único link de Config Mappings do guia corrigido de `#configmapping` para `#config-mappings`; página, versão, texto técnico e decisões preservados; validação externa reservada a C2.7 |
| C2.7 | 2026-09-02 | Páginas oficiais responderam HTTP 200; evidência Azure e anchors Quarkus confirmados; guia sem alegação Maven ou anchor antigos, com 48 cercas pareadas, destinos relativos existentes e `git diff --cached --check` limpo; C2 submetido sem aprovação inferida |
| decisão após C2.7 | 2026-09-02 | Usuário declarou `Continuar ajustes`; C2 não foi aprovado e, sem novo achado técnico ou correção indicada, nenhuma fatia adicional foi aberta por suposição; aguarda-se definição humana do ajuste pretendido |
| decisão final de C2 | 2026-09-02 | Após esclarecer que não havia ajuste adicional, o usuário declarou `aprova C2`; a decisão substituiu o estado intermediário de ajuste, concluiu C2 e liberou somente o item 3.1 |
| 3.1 | 2026-09-02 | Handoff aponta `feature/implementar-service-bus-amqp-dossie` e `tasks/features/implementar-service-bus-amqp-dossie/`, ambos ainda inexistentes; exige plano pelos templates, revalidação do estado, baseline Sonar aplicável e novo GO/checkpoints antes de produção; nenhuma feature futura foi criada |
| 3.2 | 2026-09-02 | Usuário decidiu não prosseguir com implementação e autorizou registrar o item como não aplicável; testes e controles futuros permanecem apenas como condicionantes informativos no guia e no handoff |
| CF | 2026-09-02 | Usuário confirmou que deseja somente o guia e autorizou o encerramento; conteúdo completo e C2 aprovado, com execução não solicitada e ainda não verificada |

## Regra de execução

Depois de C0, executar somente o próximo item pendente. As provas executáveis adiadas no item 1.3
pertencem à futura feature porque esta branch é exclusivamente documental; não executar Maven
aqui. C1, C2 e CF foram concluídos; 3.2 foi encerrado como não aplicável por decisão humana e não
há próximo incremento nesta feature. Qualquer
alteração em `pom.xml`, `src/`, testes, scripts, hooks ou configuração executável muda o escopo e
exige cumprir o baseline SonarQube antes da primeira alteração.
