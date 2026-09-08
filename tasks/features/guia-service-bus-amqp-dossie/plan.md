# Plano: criar guia de filas do dossiê com Azure Service Bus e AMQP

## Referência histórica deste recorte

Este arquivo preserva o planejamento e as decisões da feature documental anterior, encerrada
em 2026-09-02. Seus contratos, packages e autenticação não são o plano da implementação atual.
O [guia Service Bus](../../../doc/guias/guia-service-bus-amqp-dossie.md) foi alinhado ao
[plano de orquestrador/monitoramento](../orquestrador-monitoramento-service-bus/plan.md) e ao
[ADR-0010](../../../doc/adr/0010-extensao-quarkus-service-bus-connection-string-dev-services.md).
A continuidade fica nessa feature, sem reabrir os itens/decisões históricas abaixo.

## Intenção

Criar um guia técnico, orientado a uma pessoa desenvolvedora experiente, que mostre como o package
`br.gov.caixa.simtr.dossie` poderá:

1. publicar uma mensagem JSON na fila de entrada do Azure Service Bus; e
2. consumir continuamente uma mensagem JSON da fila de saída do Azure Service Bus assim que o
   broker a entregar, sem polling.

O roteiro deverá usar Quarkus `3.33` LTS, o cliente Java nativo do Azure Service Bus
(`com.azure:azure-messaging-servicebus`), Azure Identity (`com.azure:azure-identity`) e AMQP 1.0.
A mensagem será deliberadamente mínima, com apenas `idDossiePreValidacao` e `idDossieMtr`, para
isolar a mecânica de transporte da regra de negócio descrita na análise de sincronização.

Este plano entrega somente a preparação do guia. Ele não implementa mensageria, não provisiona
recursos Azure e não afirma que a combinação Quarkus/Azure SDK já funciona no projeto sem a prova
de compatibilidade prevista abaixo.

Há uma restrição obrigatória de plataforma: o guia deve permanecer na linha Quarkus `3.33` LTS.
Ele não proporá migração para outra linha do Quarkus nem override isolado do SmallRye gerenciado
pelo BOM. Como o SmallRye `4.33.0` não oferece o CBS `put-token` exigido pelo Azure Service Bus, o
usuário escolheu em 2026-08-31 retirar o conector SmallRye dessa borda e usar o Azure SDK com
Microsoft Entra ID. O SDK será versionado pelo Azure SDK BOM e sua compatibilidade com o BOM do
Quarkus deverá ser comprovada antes de o guia apresentar snippets executáveis.

## Branch

- planejamento documental: `docs/planejar-guia-service-bus-amqp-dossie`;
- o guia futuro deve permanecer em branch documental própria ou continuar nesta branch somente
  após autorização humana;
- a implementação de produção deverá usar outra branch `feature/...`, outro plano e novo GO.

## Escopo

- analisar e recortar o documento
  `doc/feat/sincronizacao-dossies-mtr-pre-validacao-service-bus-processamento-paralelo.md`;
- definir a topologia mínima abaixo, sem implementar o processor de monitoramento:

  ```text
  br.gov.caixa.simtr.dossie
      |-- publisher -- AMQP 1.0 --> fila de entrada
      `-- consumer  <-- AMQP 1.0 -- fila de saída
  ```

- orientar a inclusão do Azure SDK BOM e das dependências
  `com.azure:azure-messaging-servicebus` e `com.azure:azure-identity`;
- orientar configuração do namespace totalmente qualificado, transporte AMQP 1.0 sobre
  WebSockets/TLS em TCP `443` e nomes das filas por propriedades e variáveis de ambiente;
- propor DTOs de borda independentes para entrada e saída, ainda que tenham o mesmo JSON inicial;
- demonstrar publicação com `ServiceBusSenderAsyncClient`, observando o `Mono<Void>` até a
  confirmação do broker e adaptando-o a `Uni<Void>` somente por uma forma comprovada;
- demonstrar consumo contínuo orientado a evento com o cliente Azure aprovado em C1, sem polling;
- explicar settlement, redelivery, back-pressure, execução assíncrona e concorrência no limite
  necessário ao exemplo;
- explicar a separação entre tentativa funcional, controlada pela aplicação, e tentativa técnica,
  controlada por `DeliveryCount`/`MaxDeliveryCount` no broker;
- registrar como extensão técnica o reagendamento progressivo de uma nova mensagem e a quarentena
  funcional explícita, sem confundi-los com redelivery ou DLQ;
- definir como o ciclo de vida e o health dos clientes Azure serão expostos explicitamente e
  incluir um roteiro de verificação manual em ambiente não produtivo;
- referenciar somente documentação oficial e compatível com a versão escolhida;
- registrar limitações da abordagem e os itens que exigirão uma feature de implementação.

## Fora de escopo

- implementar ou alterar `pom.xml`, `src/`, testes, scripts, hooks ou configuração executável;
- criar testes unitários nesta primeira versão do guia;
- provisionar namespace, filas, DLQ, RBAC, políticas SAS, rede privada ou infraestrutura Azure;
- implementar o processor que lê a fila de entrada e publica na fila de saída;
- implementar consulta à pré-validação ou ao MTR, transições de situação ou retomada de workflow;
- implementar Peek-Lock com renovação explícita, settlement por mensagem, agendamento,
  `SequenceNumber`, sessão, transação do Service Bus, Outbox, quarentena ou deduplicação;
- definir retry de negócio, idempotência funcional, timeout do monitoramento ou intervalos
  progressivos;
- criar endpoint REST para disparar a publicação;
- usar SmallRye Reactive Messaging, `quarkus-messaging-amqp`, `MutinyEmitter`, `@Incoming` ou
  propriedades `mp.messaging.*` no exemplo principal;
- registrar payload, token, chave, connection string ou credencial em log, trace ou documento;
- atualizar a arquitetura consolidada antes de existir estado implementado;
- gerar ou atualizar `.ppt`, `.pptx`, `.pdf` ou `.html` derivados.

## Contexto verificado

- arquitetura consolidada lida:
  `doc/arquitetura-distribuida/arquitetura-ddd-integracoes-atomicas.md`;
- índice e ADRs aplicáveis lidos: ADR-0001, ADR-0003, ADR-0004, ADR-0006 e ADR-0007;
- análise de origem lida integralmente:
  `doc/feat/sincronizacao-dossies-mtr-pre-validacao-service-bus-processamento-paralelo.md`;
- planejamento e templates lidos: `tasks/README.md`, `tasks/templates/plan.md` e
  `tasks/templates/todo.md`;
- código relacionado inspecionado:
  `src/main/java/br/gov/caixa/simtr/dossie/ConsultaDocumentosDossieProduto.java`;
- guardrail relacionado inspecionado:
  `src/test/java/br/gov/caixa/simtr/hub/arquitetura/guardrails/ArchUnitProgressivoTest.java`;
- configuração e build inspecionados: `pom.xml` e
  `src/main/resources/application.properties`;
- guias de estilo inspecionados:
  `doc/guias/guia-consumo-porta-cdi-consulta-documentos-dossie.md` e
  `doc/guias/guia-implementacao-nova-capacidade-mtr.md`.

### Estado atual confirmado

- o repositório usa Quarkus `3.33.2.1`, pertencente à linha `3.33` LTS, e Java `25`;
- o BOM do Quarkus `3.33.2.1` gerencia SmallRye Reactive Messaging `4.33.0`;
- não há extensão AMQP, Azure Service Bus SDK, `@Incoming`, `@Outgoing`, `Emitter` ou
  `MutinyEmitter` no código atual;
- `br.gov.caixa.simtr.dossie` contém hoje somente o consumidor CDI local
  `ConsultaDocumentosDossieProduto`;
- o guardrail permite ao package irmão depender de tipos externos ao Hub e somente da API pública
  permitida dentro de `br.gov.caixa.simtr.hub`;
- a aplicação já possui `quarkus-smallrye-health`, mas os clientes do Azure SDK não contribuirão
  automaticamente com readiness/liveness; o health da borda deverá ser desenhado explicitamente.

### Resultado do item 1.1 — compatibilidade de versões

- a linha Quarkus `3.33` LTS permanece requisito obrigatório do guia;
- o SmallRye Reactive Messaging `4.33.0`, gerenciado pelo BOM usado no projeto, não possui o
  suporte CBS `put-token` necessário ao Azure Service Bus;
- o recurso aparece no SmallRye Reactive Messaging `4.36.0`, cuja documentação o classifica como
  experimental;
- o `4.36.0` não será introduzido por override isolado, porque isso quebraria o alinhamento de
  dependências do BOM e violaria a restrição de plataforma;
- portanto, essa combinação não oferece uma receita SmallRye AMQP autenticada no Azure Service
  Bus; a decisão humana registrada na seção seguinte escolhe o Azure SDK como caminho alternativo.

### Decisão humana de plataforma — Azure SDK

- em 2026-08-31, o usuário decidiu permanecer no Quarkus `3.33` LTS e usar
  `com.azure:azure-messaging-servicebus` com `com.azure:azure-identity`;
- o Azure SDK passa a ser o cliente principal desta borda; SmallRye Reactive Messaging permanece
  apenas como alternativa rejeitada pela ausência de CBS na versão gerenciada;
- a decisão elimina a dependência do CBS experimental do SmallRye, pois o SDK executa internamente
  a aquisição, o envio e a renovação do token no protocolo do Service Bus;
- o C1 aprovou o Azure SDK BOM `1.3.8` como matriz documental, o Processor para a saída, os
  clientes assíncronos para publicação/retry e o ciclo de vida CDI explícito; resolução,
  compilação e comportamento real continuam pendentes de validação técnica;
- nenhuma dependência ou configuração executável foi adicionada ao projeto.

### Resultado do item 1.2 — autenticação e menor privilégio

- o usuário escolheu Microsoft Entra ID em 2026-08-31; SAS não será o mecanismo do guia;
- a identidade gerenciada do workload é a forma preferida de obter tokens quando a aplicação
  estiver hospedada no Azure, sem client secret, connection string ou chave SAS;
- o C1 escolheu identidade gerenciada atribuída pelo sistema para o runtime Azure;
- a identidade do runtime deverá receber `Azure Service Bus Data Sender` somente na fila de
  entrada e `Azure Service Bus Data Receiver` somente na fila de saída, no menor escopo de entidade
  suportado; `Azure Service Bus Data Owner` e permissão `Manage` não são necessárias neste recorte;
- o recurso OAuth 2.0 solicitado ao Microsoft Entra ID será
  `https://servicebus.azure.net`; ele não deve ser confundido com o campo `name`/audience da
  autorização CBS `put-token`, que identifica a entidade do Service Bus;
- a autenticação local/SAS só poderá ser desabilitada depois de uma prova bem-sucedida do caminho
  Entra ID no ambiente não produtivo;
- em produção, o cliente deverá receber `ManagedIdentityCredential` como `TokenCredential`, sem
  implementar um token provider CBS na aplicação; `DefaultAzureCredential` ficará restrito a
  profiles locais/de desenvolvimento;
- a escolha do Azure SDK resolve o bloqueio de desenho causado pela ausência de CBS no SmallRye,
  mas não substitui a prova real de autenticação e autorização do item 1.3.

### Estado do item 1.3 — prova de conexão ainda pendente

- situação técnica em 2026-08-31: `UNVERIFIED`;
- a parte documental 1.3a confirmou no repositório oficial da Microsoft o Azure SDK BOM `1.3.8`
  como candidato estável; o POM desse BOM gerencia `azure-messaging-servicebus:7.17.19` e
  `azure-identity:1.18.4`;
- as páginas estáveis do Microsoft Learn consultadas na mesma data exibem versões diferentes:
  Service Bus `7.17.17` e Azure Identity `1.18.5`; como as referências não estão sincronizadas,
  o guia não fixará versões individuais e a resolução efetiva do BOM será a evidência decisiva;
- a documentação oficial declara JDK 8 ou superior para essas bibliotecas; o repositório permanece
  em Java `25`, mas a combinação completa com Quarkus ainda precisa de compilação e testes;
- a inspeção segura confirmou que o projeto não contém Azure SDK, clientes, publisher ou
  consumidor que possam ser iniciados para a prova;
- o processo atual não recebeu, nas variáveis padronizadas verificadas, identidade Microsoft Entra
  ID, endpoint de identidade gerenciada, namespace não produtivo ou nomes das filas; somente a
  presença foi inspecionada, sem ler ou publicar valores;
- a escolha posterior do Azure SDK define um cliente capaz de autenticar com `TokenCredential`,
  mas o escopo documental atual não autoriza alterar o `pom.xml` ou criar código de prova nas
  fontes do projeto;
- por faltarem simultaneamente cliente compatível, destino não produtivo e identidade injetada de
  modo seguro, nenhuma conexão de rede ou solicitação de token foi tentada;
- permanecem sem prova: resolução conjunta dos BOMs Quarkus/Azure, negociação WebSockets/TLS na
  porta `443`, aquisição e renovação do token Entra ID, criação do sender da fila de entrada e do
  consumidor da fila de saída;
- o guia deverá identificar essa prova como não executada e não poderá ser apresentado como pronto
  para produção;
- a análise documental do item 1.3 está encerrada com resultado `UNVERIFIED`; a prova executável
  permanece como gate da futura feature e exige a versão efetiva do Azure SDK BOM validada,
  namespace, filas e identidade disponibilizados por canal seguro, nunca pelo chat;
- Maven não foi executado porque o escopo atual é exclusivamente documental e o `AGENTS.md`
  proíbe essa execução; a resolução/compilação ficará para a futura feature executável, depois do
  baseline Sonar aplicável.

### Resultado do item 1.4 — agendamento e atomicidade

- situação técnica em 2026-08-31: `SUPPORTED_BY_OFFICIAL_API; UNVERIFIED_IN_ENVIRONMENT`;
- o Azure SDK Java expõe `scheduleMessage(message, scheduledAt, transactionContext)` no
  `ServiceBusSenderAsyncClient` e `CompleteOptions.setTransactionContext(...)` para concluir uma
  mensagem recebida dentro da mesma transação;
- no retry funcional planejado, a próxima mensagem é agendada na mesma fila de entrada da
  mensagem atual. Trata-se, portanto, de transação de entidade única: sender e receiver devem ser
  criados a partir da mesma instância de `ServiceBusClientBuilder`, compartilhando conexão AMQP,
  e `enableCrossEntityTransactions()` não deve ser habilitado;
- a sequência segura é: receber em `PEEK_LOCK`, executar o trabalho funcional antes de abrir a
  transação, criar `ServiceBusTransactionContext`, agendar a próxima mensagem com esse contexto,
  concluir a atual com `CompleteOptions` ligado ao mesmo contexto e somente então fazer commit;
- se qualquer operação falhar antes do commit bem-sucedido, a aplicação deve tentar rollback e
  não pode considerar nem o agendamento nem o Complete confirmados. O SDK não repete
  automaticamente operações dentro de uma transação;
- o lock da mensagem atual precisa permanecer válido até o commit. Processamento demorado exige
  renovação de lock, enquanto a janela transacional do Service Bus é limitada a dois minutos a
  partir da primeira operação da transação;
- transações requerem Azure Service Bus Standard ou Premium; Basic não oferece esse recurso;
- a atomicidade cobre somente as operações do broker. Banco de dados, estado do workflow, chamada
  MTR e outros recursos não participam da transação do Service Bus; idempotência e eventual
  Outbox continuam obrigatórios no desenho de produção;
- se uma evolução agendar para outra fila ou tópico, o caminho documentado é uma transação entre
  entidades do mesmo namespace, com um builder compartilhado e
  `enableCrossEntityTransactions()`/send-via. Esse não é o fluxo atual;
- para compor a transação sem `subscribe()` solto ou bloqueio dentro de callback, o futuro
  processor de retry usará `ServiceBusReceiverAsyncClient` com
  `ServiceBusSenderAsyncClient`; o consumidor da fila de saída usará o Processor aprovado no C1;
- a identidade do futuro processor precisará de `Azure Service Bus Data Receiver` e
  `Azure Service Bus Data Sender` na fila de entrada e será separada da identidade do recorte
  básico;
- nenhuma prova foi executada contra namespace real. A futura feature deverá validar tier,
  conexão compartilhada, lock renewal, commit, rollback e redelivery antes de alegar garantia em
  produção.

### Resultado do item 1.5 — ADR proposto

- criado o ADR-0009, `Azure SDK na borda Service Bus do dossiê`, inicialmente com status
  `Proposto`;
- o índice de ADRs foi atualizado no mesmo incremento com descrição e aplicabilidade suficientes
  para triagem;
- o ADR registra as escolhas humanas já realizadas — Quarkus `3.33` LTS, Azure SDK e Microsoft
  Entra ID — sem inferir a aceitação das decisões ainda pendentes;
- versão efetiva do Azure SDK BOM, modelo de recepção, identidade gerenciada, cadeia de
  credenciais, RBAC, lifecycle, settlement, concorrência, health e observabilidade foram
  submetidos ao checkpoint C1 e estão registrados na seção seguinte;
- a skill de documentação e ADRs orientou a separação entre contexto, decisão, alternativas,
  consequências e gates, seguindo a convenção dos ADRs existentes;
- nenhuma arquitetura implementada mudou; por isso o consolidado arquitetural não foi alterado.

### Resultado do checkpoint C1 — aprovado

Em 2026-08-31, o usuário aprovou explicitamente o C1 e aceitou o ADR-0009, mantendo todos os gates
técnicos pendentes. As decisões humanas registradas são:

- JSON com `idDossiePreValidacao` e `idDossieMtr` como `String`, usando records distintos para
  entrada e saída;
- propriedades `simtr-hub.dossie.service-bus.namespace`,
  `simtr-hub.dossie.service-bus.fila-entrada` e
  `simtr-hub.dossie.service-bus.fila-saida`, sempre com valores externos;
- Azure SDK BOM `1.3.8` como matriz documental, sem versões individuais, ainda sujeito a
  resolução efetiva, compilação e testes;
- `ServiceBusSenderAsyncClient` na publicação e `ServiceBusProcessorClient` no consumo contínuo
  da saída; o futuro retry transacional usará Receiver e Sender assíncronos;
- Microsoft Entra ID com identidade gerenciada atribuída pelo sistema e
  `ManagedIdentityCredential` em produção; `DefaultAzureCredential` somente em profiles locais;
- Data Sender na entrada e Data Receiver na saída para o recorte básico; o futuro processor terá
  identidade separada com Sender + Receiver somente na entrada;
- clientes CDI de longa duração, inicialização e shutdown explícitos; liveness independente do
  broker e readiness limitada à inicialização dos clientes/subscription, sem round-trip sintético;
- `PEEK_LOCK`, auto-complete desabilitado, `Complete` após sucesso, `Abandon` para falha transitória
  e `DeadLetter` para contrato permanentemente inválido; `Defer` fica fora do exemplo básico;
- `tentativaAtual` como application property inteira iniciada em `1`, quarentena explícita e
  `DeliveryCount`/`MaxDeliveryCount` exclusivamente técnicos;
- intervalos de validação externos, crescentes e versionados. Cada dossiê preserva
  `limiteEm = enviadoAoMtrEm + prazoMaximoValidacao`; a próxima execução usa
  `min(agora + intervalo, limiteEm)`, sem reiniciar ou ampliar o prazo nas retentativas;
- ao alcançar o prazo máximo individual, não executar nova consulta funcional e produzir
  `QUARENTENA`; atraso de entrega do broker não estende esse prazo;
- `maxConcurrentCalls=1` no exemplo inicial, externalizado para ajuste posterior;
- nenhum span ou métrica novo no guia e nenhum payload, token, identidade ou namespace em log.

### Emenda do C1 — transporte AMQP sobre WebSockets

Em 2026-08-31, antes da execução do item 2.4, o usuário alterou explicitamente a decisão de
transporte:

- o `ServiceBusClientBuilder` usará `AmqpTransportType.AMQP_WEB_SOCKETS`;
- a comunicação AMQP 1.0 será encapsulada em WebSockets com TLS pela porta TCP `443`;
- a receita não exigirá saída em `5671` e não fará fallback automático para AMQP/TCP;
- namespace e filas permanecem iguais; o builder continuará recebendo somente o FQDN do namespace,
  sem `https://`, `wss://`, path de fila ou credencial;
- proxy não foi aprovado nem presumido. Se o ambiente exigir um, endereço, autenticação e
  `ProxyOptions` precisarão de configuração externa e checkpoint próprio;
- a futura prova deverá validar DNS, TLS, handshake WebSocket, aquisição do token Entra ID e
  conexão ao Service Bus em `443`;
- foi aceita a consequência documentada pela Microsoft de maior latência inicial e pequeno
  overhead adicional em comparação com AMQP/TCP.

Esta emenda altera configuração de transporte, não muda a seleção do Azure SDK, a autenticação
Entra ID, o contrato JSON, o RBAC ou os gates executáveis já pendentes.

O aceite do C1 não transforma `UNVERIFIED_IN_ENVIRONMENT` em prova de funcionamento e não autoriza
alteração de produção.

### Resultado do item 2.1 — estrutura inicial do guia

- criado `doc/guias/guia-service-bus-amqp-dossie.md` com status `em elaboração`;
- registrados objetivo, leitor, limite do recorte, decisões de partida e evidência executável
  ainda `UNVERIFIED_IN_ENVIRONMENT`;
- incluída a estrutura obrigatória de 18 seções para orientar os próximos itens do checklist;
- nenhum conteúdo de dependências, topologia, contrato, configuração ou snippet reservado aos
  itens 2.2 a 2.11 foi antecipado;
- a skill de documentação orientou o alinhamento com os guias existentes e a separação entre
  decisão aceita e comportamento comprovado;
- nenhuma fonte, configuração executável ou formato derivado foi criado ou alterado.

### Resultado do item 2.2 — versões, dependências, topologia e pré-condições

- documentada a topologia com sender do package `dossie` na entrada, Processor na saída e
  processor intermediário explicitamente fora do recorte;
- registradas as direções invariantes e os papéis Data Sender/Receiver no menor escopo por fila;
- registradas as pré-condições de namespace, duas filas, tier Standard/Premium, identidade
  gerenciada, RBAC, DNS, TLS e TCP `443` para AMQP sobre WebSockets, HTTPS e autenticação em
  ambiente não produtivo, conforme a emenda do C1;
- documentada a matriz Quarkus `3.33.2.1`, Java `25`, Azure SDK BOM `1.3.8`, Service Bus `7.17.19`
  e Azure Identity `1.18.4`, preservando a divergência das páginas estáveis de API;
- incluído fragmento Maven para importar o BOM Azure ao lado do BOM Quarkus e declarar Service Bus
  antes de Identity, sem versões individuais;
- a skill de desenvolvimento orientado por fontes vinculou cada decisão específica a
  documentação oficial Microsoft/GitHub e manteve conflitos explicitamente visíveis;
- resolução Maven, compilação e conexão continuam `UNVERIFIED_IN_ENVIRONMENT`; nenhum comando
  Maven, código de produção ou configuração executável foi alterado.

### Resultado do item 2.3 — contratos e JSON mínimo

- documentado o mesmo wire shape de duas propriedades para as duas filas, preservando
  `idDossiePreValidacao` e `idDossieMtr` como `String` e identificadores opacos;
- propostos os records `MensagemEntradaSincronizacaoDossie` e
  `MensagemSaidaSincronizacaoDossie`, em packages de adaptadores distintos, sem compartilhar DTO;
- explicitado que o nome do record acompanha a fila, enquanto a direção do package acompanha a
  arquitetura: publisher em adaptador de saída e consumidor em adaptador de entrada;
- documentados `@JsonProperty` e o JSON canônico, sem incluir status, tentativa, timestamp,
  versão, correlação ou metadado AMQP no body;
- registrada a ausência intencional de validação de presença, nulo, vazio e formato; tolerância a
  propriedades desconhecidas não foi tratada como ampliação do contrato;
- a skill de desenvolvimento orientado por fontes fundamentou os snippets nas documentações
  oficiais de records do Java 25, Jackson e JSON/Jackson do Quarkus;
- os snippets permanecem `DOCUMENTED; UNVERIFIED_BY_COMPILATION`; nenhum arquivo Java, Maven ou
  configuração executável foi criado ou alterado.

### Resultado do item 2.4 — configuração, AMQP WebSockets/TLS e Entra ID

- atualizadas as pré-condições anteriores conforme a emenda humana do C1: transporte
  `AMQP_WEB_SOCKETS` em TCP `443`, sem requisito de `5671` nem fallback automático para AMQP/TCP;
- documentados os três aliases obrigatórios de ambiente e o `DossieServiceBusConfig`, sem valores
  reais, defaults, connection string ou segredo;
- documentado `ManagedIdentityCredentialBuilder().build()` para a identidade atribuída pelo
  sistema em produção, sem client ID, tenant ou secret;
- restringido o `DefaultAzureCredential` local com `AZURE_TOKEN_CREDENTIALS=dev` e
  `requireEnvVars(...)`, APIs compatíveis com Azure Identity `1.18.4`;
- documentado o builder de topo compartilhado com `credential(fqdn, TokenCredential)` e
  `transportType(AmqpTransportType.AMQP_WEB_SOCKETS)`, deixando os sub-builders para 2.5 e 2.6;
- registrados TLS obrigatório, handshake WebSocket, recurso Entra ID, CBS interno ao SDK, RBAC
  mínimo e proibição de SAS, client secret, token materializado, proxy presumido e log de
  topologia;
- a skill de desenvolvimento orientado por fontes confrontou snippets e consequências com as
  APIs oficiais do Azure SDK/Identity, Service Bus e configuração do Quarkus `3.33`;
- o resultado permanece
  `DOCUMENTED_FROM_OFFICIAL_API; UNVERIFIED_BY_COMPILATION_AND_ENVIRONMENT`; nenhum código,
  `application.properties`, Maven ou configuração executável foi alterado.

### Resultado do item 2.5 — publisher e composição `Mono<Void>` -> `Uni<Void>`

- documentada a construção única do `ServiceBusSenderAsyncClient` para a fila de entrada a partir
  do builder compartilhado, deixando lifecycle CDI e fechamento para o item 2.7;
- documentado publisher no adaptador de saída com serialização explícita pelo `ObjectMapper`,
  `BinaryData.fromBytes(...)` e `content-type=application/json`, sem logar body ou topologia;
- adotada a ponte sem dependência adicional
  `Uni.createFrom().completionStage(() -> sender.sendMessage(message).toFuture())`, que posterga o
  envio até a assinatura e preserva sucesso vazio, falha e solicitação de cancelamento;
- registrado que cada nova assinatura cria novo envio, que cancelamento não desfaz mensagem já
  aceita e que retry/resubscription dependem de estratégia de idempotência futura;
- distinguida a conclusão do send — aceite e armazenamento pelo broker — de processamento por um
  consumidor, com proibição explícita de `subscribe()`, fire-and-forget, `block()` e `await()` no
  publisher;
- as skills de documentação e desenvolvimento orientado por fontes fundamentaram a receita nas
  APIs oficiais do Azure SDK, settlement do Service Bus e Mutiny `3.1.1`;
- o resultado permanece
  `DOCUMENTED_FROM_OFFICIAL_API; UNVERIFIED_BY_COMPILATION_AND_ENVIRONMENT`; nenhum código, Maven,
  teste ou configuração executável foi criado ou alterado.

### Resultado do item 2.6 — consumidor contínuo sem polling

- documentada a construção única do `ServiceBusProcessorClient` para a fila de saída, a partir do
  builder compartilhado, com `PEEK_LOCK`, auto-complete desabilitado e callbacks obrigatórios;
- registrado `start()` como chamada de inicialização que retorna após iniciar o receptor em
  background, sem endpoint, scheduler, timer, laço ou chamada periódica de recepção na aplicação;
- esclarecido que a interface é push para o código da aplicação, enquanto o SDK permanece
  responsável pelo link AMQP, créditos de recepção e entrega aos callbacks;
- documentada a leitura explícita de `contexto.getMessage().getBody().toBytes()` pelo
  `ObjectMapper` para o DTO exclusivo da fila de saída, sem deixar tipos do SDK atravessarem a
  borda do adaptador;
- preservada a separação de escopo: nenhuma retomada de workflow foi inventada, e settlement,
  falhas, redelivery, lock renewal, threads, back-pressure, concorrência, lifecycle completo e
  health continuam no item 2.7;
- as skills de documentação e desenvolvimento orientado por fontes mantiveram a receita alinhada
  às APIs oficiais do Processor e tornaram explícita a diferença entre push na aplicação e a
  recepção interna mantida pelo SDK;
- o resultado permanece
  `DOCUMENTED_FROM_OFFICIAL_API; UNVERIFIED_BY_COMPILATION_AND_ENVIRONMENT`; nenhum código, Maven,
  teste ou configuração executável foi criado ou alterado.

### Resultado do item 2.7 — settlement, capacidade, lifecycle e health

- externalizado `max-concurrent-calls` com valor inicial `1` aprovado no C1 e aplicado ao builder
  por `maxConcurrentCalls(...)`, sem tratar o default do SDK como requisito implícito;
- documentada a tabela de disposition manual: `Complete` somente após sucesso, `Abandon` para
  falha transitória, `DeadLetter` para contrato permanentemente inválido e nenhuma segunda
  disposition presumida quando o resultado do settlement for incerto;
- registrada a garantia `at-least-once`, incluindo lock perdido e resposta de `Complete` perdida,
  e transferida a estratégia concreta de idempotência para a futura implementação;
- documentados auto lock renewal, prefetch e timeouts como parâmetros separados a medir e provar;
  os defaults observados nas páginas `7.17.17` não foram promovidos a requisitos da matriz
  documental `7.17.19`;
- esclarecido que o Processor usa callback síncrono, threads Reactor `boundedElastic` e limites de
  concorrência/prefetch, sem demanda Reactive Streams nem garantia de ordenação global;
- documentados `StartupEvent`, `ShutdownEvent`, `start()`, `isRunning()` e `close()`, com fechamento
  do Processor antes do sender e sem promessa não comprovada de drenagem de callbacks em voo;
- definido readiness local para clientes inicializados e Processor em execução, sem round-trip ao
  broker, e mantida liveness independente do Azure Service Bus;
- limitado o diagnóstico a sinais técnicos sanitizados, sem payload, IDs contratuais, namespace,
  entidade, identidade ou segredo e sem criar spans ou métricas;
- as skills de documentação e desenvolvimento orientado por fontes confrontaram a receita com as
  APIs oficiais do Azure SDK, Service Bus, lifecycle e SmallRye Health;
- o resultado permanece
  `DOCUMENTED_FROM_OFFICIAL_API; UNVERIFIED_BY_COMPILATION_AND_ENVIRONMENT`; nenhum código, Maven,
  teste ou configuração executável foi criado ou alterado.

### Resultado do item 2.8 — `tentativaAtual` separada de `DeliveryCount`

- atualizado o publisher inicial para gravar `tentativaAtual=1` como `Integer` em
  `applicationProperties`, preservando o body JSON com somente as duas propriedades aprovadas;
- documentadas propriedade, localização, tipo, início, avanço, finalidade e limite distintos para
  `tentativaAtual` funcional e `DeliveryCount` técnico;
- registrada validação estrita da chave `tentativaAtual`: ausência, tipo diferente de `Integer` ou
  valor menor que `1` não recebem coerção nem default silencioso;
- mantido o estado persistido e idempotente como fonte de verdade, com mensagem menor que o
  esperado tratada como possível duplicata/obsolescência e valor maior como salto a classificar;
- documentado que `Abandon` ou lock expirado não incrementam `tentativaAtual`; o broker atualiza
  somente o `DeliveryCount` read-only da mesma mensagem;
- proibido usar `DeliveryCount` como tentativa funcional, limite de negócio, índice de intervalo
  ou valor para nova mensagem;
- mantidos para o item 2.9 o valor operacional de `MaxDeliveryCount`, a criação da nova mensagem,
  o reagendamento, a quarentena e a política completa de DLQ;
- as skills de documentação e desenvolvimento orientado por fontes confrontaram o contrato com
  as APIs oficiais de `ServiceBusMessage`, `ServiceBusReceivedMessage` e o modelo AMQP do Service
  Bus;
- o resultado permanece
  `DOCUMENTED_FROM_OFFICIAL_API; UNVERIFIED_BY_COMPILATION_AND_ENVIRONMENT`; nenhum código, Maven,
  teste ou configuração executável foi criado ou alterado.

### Resultado do item 2.9 — reagendamento, quarentena e DLQ técnica

- registrada política temporal imutável por monitoramento, com `limiteEm` calculado desde o envio
  confirmado ao MTR e versão/snapshot dos intervalos preservada no estado persistido;
- explicitado o mapeamento entre tentativa e intervalo: `N` intervalos permitem `N + 1` consultas
  funcionais, sempre com próxima ativação em `min(agora + intervalo, limiteEm)`;
- documentada avaliação do prazo antes da consulta e depois de resultado inconclusivo, impedindo
  que atraso do broker ou duração da consulta ampliem o prazo individual;
- definida nova `ServiceBusMessage` para a próxima tentativa, com o mesmo JSON mínimo,
  `tentativaAtual + 1` em application properties e nenhuma cópia de `DeliveryCount` ou metadado
  read-only;
- registrada composição `scheduleMessage(...) + Complete` no mesmo `ServiceBusTransactionContext`,
  usando Receiver e Sender assíncronos da fila de entrada derivados do builder compartilhado;
- usada `Mono.usingWhen(...)` para commit após sucesso, rollback em falha anterior ao commit e
  rollback em cancelamento, sem fire-and-forget;
- documentados resultado incerto de falha no commit, timeout transacional de dois minutos,
  ausência de retry cego, tier Standard/Premium e proibição de cross-entity nessa topologia;
- limitada a atomicidade às operações do Azure Service Bus; MTR, banco, workflow e eventual
  Outbox permanecem fora da transação e dependem de desenho próprio;
- definida `QUARENTENA` como resultado funcional explícito e idempotente, sem inventar destino,
  schema ou persistência e sem usar DLQ para prazo ou esgotamento da política;
- reservados `DeadLetter` explícito para contrato permanentemente inválido e DLQ automática para
  esgotamento técnico por `MaxDeliveryCount`, cujo valor continua decisão operacional;
- as skills de documentação e desenvolvimento orientado por fontes confrontaram o handoff com
  as APIs oficiais de sender/receiver, `CompleteOptions`, Reactor `Mono.usingWhen`, transações,
  agendamento e DLQ;
- registrada a divergência documental entre Learn `7.17.17` e a matriz BOM `7.17.19`; o
  changelog informa somente atualização de dependências, sem substituir a prova executável;
- o resultado permanece
  `DOCUMENTED_FROM_OFFICIAL_API; UNVERIFIED_BY_COMPILATION_AND_ENVIRONMENT`; nenhum código, Maven,
  teste ou configuração executável foi criado ou alterado.

### Resultado do item 2.10 — verificação manual sem testes unitários

- documentado um protocolo para a futura feature executável, restrito a ambiente não produtivo e
  registrado agora como `PROTOCOL_DOCUMENTED; NOT_EXECUTED`;
- definidas como pré-condições filas dedicadas ou vazias, WebSockets/TLS em TCP `443`, RBAC mínimo,
  matriz efetivamente resolvida e aplicação compilada antes da execução manual;
- exigido que a implementação nomeie seu gatilho técnico do publisher, seu mecanismo reversível
  de falha transitória e seu ponto sanitizado de inspeção, sem inventar endpoint ou test hook nesta
  feature documental;
- definidos registro mínimo e estados `NOT_EXECUTED`, `OBSERVED` e `NOT_OBSERVED`, proibindo
  copiar para a evidência FQDN, nomes reais de filas, IDs, payload, identidade, token ou segredo;
- descritos nove passos: lifecycle/health, chamada ao publisher, aceite do broker, envio externo à
  saída, callback acionado pela entrega, `Complete`, rejeições contratuais controladas, `Abandon`
  com redelivery e inspeção de logs;
- usado Service Bus Explorer com Microsoft Entra ID, **Send** e **Peek**; **ReceiveAndDelete** e
  **Purge** ficaram expressamente fora do roteiro;
- preservada a separação de tentativas: falha transitória usa JSON válido, pode aumentar apenas
  `DeliveryCount` e termina em `Complete`; JSON inválido pertence ao caminho de `DeadLetter`;
- registrados critérios de parada para settlement incerto, permissão excessiva, destino produtivo,
  segredo exposto, DLQ inesperada ou evidência não correlacionável, sem reenvio automático;
- explicitado que a observação manual não prova ausência universal de polling, concorrência,
  carga, reconexão, idempotência, modo nativo nem a extensão transacional do item 2.9 e não
  substitui testes da futura implementação;
- as skills de documentação e desenvolvimento orientado por fontes mantiveram a fronteira entre
  protocolo e evidência, confrontando health, autenticação, Explorer e settlement com as fontes
  oficiais;
- nenhum teste unitário, Maven, Sonar, cliente Azure, configuração executável ou conexão externa
  foi criado, alterado ou executado.

### Resultado do item 2.11 — limitações e próximos incrementos

- atualizado o estado do guia para conteúdo planejado concluído, depois complementado pelas
  correções C2.2 e C2.4, com revisão focal C2.5 e evidência executável ainda pendentes;
- separadas, por dimensão, as decisões documentadas e as provas ausentes de plataforma, contrato,
  publicação, consumo, segurança/rede, transação, workflow e operação;
- proibidas alegações de prontidão produtiva, integração validada, exactly-once, ordenação global,
  atomicidade externa, alta disponibilidade ou health ponta a ponta;
- organizados os gates futuros de build, autenticação/transporte, publisher, consumidor,
  duplicatas/idempotência, transação e operação segura;
- documentado, a partir das fontes oficiais, que `PEEK_LOCK` admite redelivery e requer consumidor
  idempotente, enquanto a DLQ não possui limpeza automática e exige runbook próprio;
- ordenados os próximos incrementos ainda não autorizados: transporte básico executável, prova da
  fatia básica, processor/workflow da entrada e robustez/operação produtiva;
- mantidos modo nativo, Outbox, sessions/ordenação, duplicate detection e endpoint público fora do
  caminho implícito; somente necessidade concreta, plano e checkpoints poderão incluí-los;
- preservada a fronteira com os itens 3.1 e 3.2: branch, plano, GO e dívida detalhada de testes da
  implementação serão formalizados somente depois do checkpoint C2;
- as skills de documentação e desenvolvimento orientado por fontes fizeram o fechamento refletir
  contexto, consequências e evidência oficial sem criar nova decisão arquitetural ou ADR;
- o resultado corrente é
  `GUIDE_COMPLETE; C2_APPROVED; GUIDE_ACCEPTED; FEATURE_CLOSED; IMPLEMENTATION_NOT_REQUESTED; EXECUTION_UNVERIFIED`; nenhum código, teste,
  Maven, Sonar, configuração executável ou conexão externa foi criado, alterado ou executado.

### Divergências que o guia deve explicitar

| Origem | Divergência | Tratamento no guia |
|---|---|---|
| análise, seção 18 | escolhe `azure-messaging-servicebus` por recursos avançados | alinhado pela decisão humana de usar o Azure SDK como cliente principal |
| análise | considera o fluxo completo, Outbox, agendamento, transações e settlement explícito | manter a implementação fora do exemplo básico e documentar a extensão suportada pelo SDK somente após prova |
| análise, seção 5.3 | coloca `tentativaAtual` no body da entrada | C1 aprovou application property inteira para preservar o JSON mínimo |
| análise | menciona Quarkus `3.33.3.1` e Java `17` | usar o estado real: Quarkus `3.33.2.1` e Java `25` |
| repositório | não existe mensageria | tratar dependências, clientes e configuração como adições futuras, não como componentes existentes |
| Azure Service Bus | exige TLS e autorização por token no AMQP | usar `TokenCredential` no Azure SDK; não configurar usuário/senha genéricos |
| SmallRye `4.33.0` | não possui CBS `put-token` | não publicar uma receita de autenticação Azure com a versão gerenciada pelo BOM |
| SmallRye `4.36.0` | introduz CBS `put-token` como recurso experimental | C1 preservou Quarkus `3.33` LTS e rejeitou upgrade ou override |
| escopo original | previa `MutinyEmitter`, canais e listener `@Incoming` | C1 escolheu Azure Processor para a saída e clientes assíncronos para publicação/retry |

## Recorte funcional do guia

O package `dossie` será apresentado como o lado orquestrador do recorte:

- ele **escreve** na fila de entrada;
- ele **escuta e consome** a fila de saída quando uma mensagem chega;
- ele não lê a fila de entrada;
- ele não produz a mensagem real de resultado na fila de saída;
- ele não executa monitoramento nem regra de negócio entre as filas.

Essa direção deve aparecer no início do guia para evitar a interpretação de que o exemplo é um
processor de monitoramento completo.

## Contrato ilustrativo aprovado

O JSON dos dois exemplos terá somente:

```json
{
  "idDossiePreValidacao": "c5a13bd2-fc7e-45cd-9c47-8a79b5926c86",
  "idDossieMtr": "123456789"
}
```

Decisões aprovadas no checkpoint C1:

- ambos os campos serão `String`, sem inferir formato numérico para o identificador MTR;
- nomes JSON permanecerão exatamente `idDossiePreValidacao` e `idDossieMtr`, alinhados à análise;
- o corpo não conterá status, tentativa, timestamps, versão, correlação ou regra de negócio;
- metadados técnicos AMQP não serão misturados ao JSON;
- entrada e saída usarão records Java distintos, apesar do wire shape idêntico, para preservar a
  evolução independente das duas bordas conforme ADR-0004;
- nomes de filas reais não serão inventados: o guia usará placeholders e exigirá valores externos;
- propriedades aprovadas: `simtr-hub.dossie.service-bus.namespace`,
  `simtr-hub.dossie.service-bus.fila-entrada` e
  `simtr-hub.dossie.service-bus.fila-saida`.

## Decisões e impactos

| Dimensão | Situação | Checkpoint humano adicional? |
|---|---|---|
| Contrato | dois contratos de fila com o mesmo JSON mínimo | C1 aprovado |
| Arquitetura | Quarkus `3.33` + Azure SDK com `AMQP_WEB_SOCKETS`; Processor na saída e clientes assíncronos na publicação/retry | C1 aprovado e emendado |
| Segurança | Entra ID, identidade gerenciada atribuída pelo sistema e RBAC mínimo separado por fila | C1 aprovado |
| Comportamento observável | settlement manual, concorrência inicial 1 e health explícito, sem novos spans/métricas | C1 aprovado |

Todas as decisões desta tabela foram aprovadas pelo usuário no C1 e estão registradas no
`todo.md`. Os gates executáveis permanecem pendentes e não foram convertidos em aprovação técnica.

## Estratégia técnica que o guia deverá ensinar

### Dependência e serialização

- importar `com.azure:azure-sdk-bom:1.3.8` como matriz documental no `dependencyManagement`, sem
  remover o BOM do Quarkus, e validar a resolução efetiva antes de apresentar o snippet como
  executável;
- adicionar `com.azure:azure-messaging-servicebus` e `com.azure:azure-identity` sem versões
  individuais, conforme a recomendação do Azure SDK BOM;
- executar `mvn dependency:tree` e teste de compilação na futura feature de implementação para
  detectar conflitos de Reactor, Netty, Jackson ou Azure Core entre os dois BOMs;
- não adicionar `quarkus-messaging-amqp` nem sobrescrever o SmallRye `4.33.0`;
- serializar explicitamente o record de entrada para bytes JSON e criar `ServiceBusMessage` com
  `content-type=application/json`; não depender de conversão implícita do conector;
- desserializar explicitamente o `BinaryData` recebido para o DTO exclusivo da saída;
- documentar requisito de reflexão somente se o modo nativo fizer parte da implementação aprovada.

### Publicação reativa na fila de entrada

- criar um `ServiceBusSenderAsyncClient` de longa duração para a fila de entrada, usando
  `ServiceBusClientBuilder`;
- expor um método técnico que componha o `Mono<Void>` retornado por `sendMessage(...)` e só
  conclua depois da confirmação do serviço;
- se a borda interna exigir `Uni<Void>`, validar e documentar uma adaptação Reactor/Mutiny que
  preserve erro e cancelamento;
- não chamar `subscribe()` dentro do publisher, não fazer fire-and-forget e não bloquear o event
  loop;
- não acoplar o exemplo a Resource REST ou a caso de uso ainda inexistente.

### Consumo contínuo da fila de saída

- não usar `@Incoming`: essa anotação pertence ao SmallRye Reactive Messaging, que saiu do
  desenho desta borda;
- usar `ServiceBusProcessorClient`, aprovado no C1 e recomendado pela Microsoft para consumo
  contínuo e recuperação automática de falhas transitórias;
- reservar `ServiceBusReceiverAsyncClient` para o futuro processor de retry transacional, que
  precisará compor Receiver e Sender assíncronos;
- criar clientes de longa duração em bean CDI, iniciá-los depois da construção da aplicação e
  encerrá-los de modo determinístico no shutdown;
- receber em `PEEK_LOCK`, desabilitar auto-complete quando houver regra de processamento e
  concluir a mensagem somente após sucesso;
- não criar polling, scheduler, timer, endpoint de consulta ou loop de recepção;
- mapear o corpo para o DTO exclusivo da saída e deixar um ponto técnico mínimo de processamento;
- não inventar a regra que retomará o workflow.

### Configuração do Azure SDK para Service Bus

O guia deverá mostrar, depois da validação de dependências:

- `fullyQualifiedNamespace=<namespace>.servicebus.windows.net`;
- `AmqpTransportType.AMQP_WEB_SOCKETS`, com AMQP sobre WebSockets/TLS na porta `443`;
- nome relativo da fila configurado em `queueName(...)` para cada cliente;
- valores externos por environment variables, sem segredo literal;
- `ManagedIdentityCredential` de identidade atribuída pelo sistema entregue como
  `TokenCredential` em produção; `DefaultAzureCredential` ficará restrito ao desenvolvimento;
- `Azure Service Bus Data Sender` limitado à fila de entrada e
  `Azure Service Bus Data Receiver` limitado à fila de saída;
- token Microsoft Entra ID solicitado para o recurso `https://servicebus.azure.net`;
- liveness independente do broker e readiness representando inicialização dos clientes e da
  subscription, sem round-trip sintético;
- um alerta de que o SDK realiza CBS `put-token` e renovação internamente; a aplicação não
  implementará nem configurará CBS diretamente.

Não será aceito um exemplo com connection string, SAS, client secret literal ou token materializado
em propriedade. O snippet só será apresentado como executável depois de compilar com a combinação
de BOMs aprovada e de a prova 1.3 ter as pré-condições seguras disponíveis.

### Ack, falha e concorrência

- explicar `PEEK_LOCK`, `Complete` após sucesso, `Abandon` em falha transitória e `DeadLetter` para
  contrato permanentemente inválido; `Defer` ficará fora do exemplo básico;
- usar `disableAutoComplete()` no `ServiceBusProcessorClient`;
- registrar que a entrega do Service Bus é `at-least-once` e que idempotência será necessária na
  implementação real, embora esteja fora deste guia de transporte;
- documentar `maxConcurrentCalls=1` como início seguro e externalizá-lo para ajuste posterior;
- validar lock renewal, prefetch e timeout separadamente; não adotar defaults como requisito.

### Tentativa funcional, reagendamento, quarentena e DLQ

Se o guia for estendido ao processor da fila de entrada, deverá preservar a separação já definida
na análise. No recorte básico, esta seção funciona como decisão e handoff, não como implementação:

| Controle | Quem mantém | Quando avança | Destino ao esgotar |
|---|---|---|---|
| `tentativaAtual` | aplicação | uma consulta válida termina sem resultado conclusivo | quarentena funcional explícita |
| `DeliveryCount` | Azure Service Bus, somente leitura | lock expira ou a entrega é abandonada | DLQ ao exceder `MaxDeliveryCount` |

`tentativaAtual` pertence ao futuro processor que monitora a fila de entrada. O listener da fila
de saída não incrementa esse contador; se ele falhar tecnicamente ao tratar um resultado, usa
nack/redelivery e o `DeliveryCount` da própria mensagem de saída.

Decisão aprovada para manter o JSON ilustrativo com apenas dois campos:

- transportar `tentativaAtual` como **AMQP application property** tipada, não como
  `DeliveryCount` e não como parte do payload JSON;
- tratar essa propriedade como parte obrigatória e versionada do contrato, mesmo estando fora do
  body;
- validar ausência, tipo, faixa e regressão do contador antes de usá-lo;
- em produção, usar estado persistido e idempotente do monitoramento como fonte de verdade; o
  valor da mensagem representa a tentativa que está sendo executada;
- incrementar o contador somente depois de uma consulta funcional concluída com estado ainda não
  conclusivo;
- para falha técnica, não incrementar `tentativaAtual`: produzir nack/Abandon e permitir que o
  broker incremente `DeliveryCount`;
- quando houver nova tentativa funcional, não abandonar a mensagem esperando um atraso. Criar uma
  **nova mensagem agendada** com o próximo contador e concluir a mensagem atual somente depois da
  garantia de continuidade definida;
- quando o limite funcional for atingido, produzir/persistir explicitamente o resultado
  `QUARENTENA` e concluir a mensagem atual; não enviá-la à DLQ por motivo funcional;
- manter `MaxDeliveryCount` configurado como rede de segurança para falhas técnicas repetidas e
  mensagens tecnicamente não processáveis; o valor concreto será decisão operacional, não o limite
  de negócio;
- reservar DeadLetter explícito para contrato permanentemente inválido e a DLQ automática para
  esgotamento das entregas técnicas.

O reagendamento funcional também seguirá a política aprovada no C1:

- `tentativaAtual` começa em `1`;
- intervalos formam uma lista externa, crescente e versionada; valores como
  `PT30M,PT3H,PT4H,PT6H` são apenas ilustrativos;
- cada dossiê recebe prazo próprio contado do envio confirmado ao MTR:
  `limiteEm = enviadoAoMtrEm + prazoMaximoValidacao`;
- a próxima ativação usa `min(agora + intervalo, limiteEm)` e preserva o mesmo `limiteEm` em todas
  as retentativas;
- alteração posterior da configuração não muda a política de um monitoramento já iniciado;
- ao alcançar o prazo, inclusive após atraso do broker, não executar nova consulta funcional e
  produzir `QUARENTENA`.

Uma mensagem reagendada é uma nova mensagem do ponto de vista do broker e não herda o
`DeliveryCount` técnico da mensagem anterior. Por isso `MaxDeliveryCount` não substitui o contador
funcional.

O Azure SDK oferece suporte oficial à transação `agendar próxima + Complete atual`. Na topologia
proposta, ambas as operações recaem sobre a mesma fila de entrada e devem usar clientes criados
pelo mesmo `ServiceBusClientBuilder`, sem `enableCrossEntityTransactions()`. O guia deverá separar
essa capacidade confirmada na API da prova ainda pendente em ambiente e limitar a garantia às
operações do Service Bus.

## Tarefas

### Task 1 — Fechar a matriz de compatibilidade com Azure Service Bus

**Descrição:** determinar a combinação suportada de Quarkus, Azure SDK, Azure Identity e modelo de
consumo que permitirá ao guia apresentar uma receita executável para Azure Service Bus.

**Critérios de aceitação:**

- registrar a versão atual (`Quarkus 3.33.2.1` / `SmallRye 4.33.0`) e o SmallRye `4.36.0` como a
  primeira versão confirmada com CBS `put-token`, sem adotá-la;
- preservar obrigatoriamente a linha Quarkus `3.33` LTS;
- não sobrescrever isoladamente a versão SmallRye gerenciada pelo BOM;
- usar o Azure SDK como cliente principal, conforme decisão humana de 2026-08-31;
- escolher uma versão estável do Azure SDK BOM, registrar as versões efetivas de
  `azure-messaging-servicebus` e `azure-identity` e provar resolução sem conflitos com o BOM do
  Quarkus;
- usar Microsoft Entra ID, conforme decisão humana de 2026-08-31, sem apresentar SAS como
  alternativa equivalente no guia;
- preferir identidade gerenciada no runtime Azure e submeter o tipo concreto a C1;
- documentar o recurso OAuth `https://servicebus.azure.net` e as permissões mínimas por fila;
- nunca materializar access token, connection string, client secret, key ou token SAS no
  repositório ou no chat;
- provar conexão TLS, criação do sender da entrada e consumo contínuo da saída em namespace não
  produtivo, quando ambiente e credencial segura estiverem disponíveis;
- comparar `ServiceBusProcessorClient` e `ServiceBusReceiverAsyncClient` contra o requisito de
  consumo contínuo, recuperação, composição assíncrona, settlement e ciclo de vida;
- registrar que o SDK oferece agendamento e a transação
  `agendar próxima mensagem + concluir mensagem atual` na mesma fila, usando um único builder e
  contexto transacional, e manter a execução real como gate da futura feature;
- se a prova não puder ser executada, marcar a seção de autenticação como não verificada e impedir
  que o guia seja apresentado como pronto para produção.

**Verificação:**

- conferir documentação do Azure SDK Java, Azure Identity e protocolo do Service Bus;
- conferir compatibilidade de dependências com Quarkus `3.33.2.1`;
- opcionalmente compilar e executar um projeto descartável fora das fontes do repositório;
- registrar versões, data, configuração não secreta e resultado da conexão, sem token ou payload.

**Dependências:** checkpoint humano C0; para a parte local, acesso às dependências Maven; para a
prova real, namespace e filas não produtivos, credencial injetada fora do repositório e
conectividade de saída em `443`, inclusive handshake WebSocket. Na execução de 2026-08-31, as
pré-condições Azure não estavam disponíveis e a prova permaneceu `UNVERIFIED`. O C1 aprovou o
desenho, mas não dispensou essas
provas.

**Arquivos prováveis:**

- `doc/adr/0009-azure-sdk-service-bus-dossie.md`;
- `doc/adr/README.md`;
- `tasks/features/guia-service-bus-amqp-dossie/plan.md`;
- `tasks/features/guia-service-bus-amqp-dossie/todo.md`;
- nenhuma fonte do projeto.

### Checkpoint C1 — Contrato, plataforma, arquitetura, segurança e observabilidade

**Status:** `APROVADO` pelo usuário em 2026-08-31.

O checkpoint aprovou as decisões consolidadas em `Resultado do checkpoint C1 — aprovado`, aceitou
o ADR-0009 e manteve como pendentes todas as provas executáveis de dependências, ambiente,
autenticação, conexão e comportamento transacional.

### Task 2 — Escrever o guia passo a passo

**Descrição:** criar `doc/guias/guia-service-bus-amqp-dossie.md` com passos pequenos, completos e
ordenados, sem implementar a feature no projeto.

**Estrutura obrigatória do guia:**

1. objetivo, leitor e limite do recorte;
2. topologia e direção das duas filas;
3. pré-condições de Azure, rede e autenticação;
4. versões validadas, Azure SDK BOM e dependências Maven;
5. árvore proposta dentro de `br.gov.caixa.simtr.dossie`;
6. contratos Java independentes e JSON de duas propriedades;
7. configuração do sender da fila de entrada;
8. publisher com `ServiceBusSenderAsyncClient` e composição do resultado;
9. configuração do consumidor da fila de saída;
10. consumo contínuo pelo cliente Azure aprovado, acionado na chegada e sem polling;
11. settlement, redelivery, lock renewal, threads, back-pressure e concorrência;
12. separação entre contador funcional, `DeliveryCount`, quarentena e DLQ;
13. reagendamento progressivo como extensão condicionada à compatibilidade comprovada;
14. health e diagnóstico seguro;
15. verificação manual ponta a ponta sem testes unitários;
16. falhas esperadas e como diagnosticá-las sem expor segredo;
17. limites do exemplo e próximos incrementos de produção;
18. referências oficiais versionadas.

**Critérios de aceitação:**

- uma pessoa experiente consegue identificar todos os arquivos futuros e a ordem de criação;
- snippets usam APIs e imports válidos para a plataforma aprovada;
- o producer observa o resultado assíncrono do envio;
- o consumidor recebe continuamente, não faz polling e deixa clara a semântica de settlement;
- o corpo JSON contém somente as duas propriedades aprovadas;
- fila de entrada é somente escrita e fila de saída é somente lida pelo package `dossie`;
- configuração não contém credenciais, URLs completas com segredo ou valores reais de ambiente;
- o texto não apresenta `@Incoming`, `MutinyEmitter` ou properties SmallRye como parte do Azure
  SDK;
- tentativa funcional e `DeliveryCount` aparecem como contadores distintos, com quarentena e DLQ
  em destinos semanticamente diferentes;
- ausência de testes unitários é declarada como decisão temporária, não como evidência de correção;
- documentação consolidada não é alterada por um estado apenas hipotético;
- nenhum formato derivado é criado ou atualizado.

**Verificação:**

- conferir todos os imports, métodos e properties em documentação da versão aprovada;
- conferir que cada bloco de código possui contexto, pré-condição e resultado esperado;
- executar `rg` para garantir ausência de segredo/connection string e nomes descontinuados;
- executar `git diff --check` e revisar links Markdown;
- não executar Maven ou SonarQube enquanto o diff permanecer exclusivamente documental.

**Dependências:** Task 1 e checkpoint C1.

**Arquivos prováveis:**

- `doc/guias/guia-service-bus-amqp-dossie.md`;
- `tasks/features/guia-service-bus-amqp-dossie/plan.md`;
- `tasks/features/guia-service-bus-amqp-dossie/todo.md`.

### Checkpoint C2 — Revisão técnica do guia

- confrontar snippets com Quarkus, Azure SDK e Azure Identity das versões escolhidas;
- confrontar TLS, endereços, autenticação, settlement e transações com documentação Microsoft;
- confirmar que não há regra de negócio ou infraestrutura fora do recorte;
- confirmar que exemplos não bloqueiam, não fazem fire-and-forget e não vazam segredo;
- apresentar limitações e evidência de conexão, ou a ausência dela, ao usuário.

#### Ajustes solicitados na primeira revisão C2

Em 2026-09-01, a revisão técnica inicial não encontrou bloqueio nas APIs documentadas, na
composição reativa, no consumo sem polling, no settlement, na transação, em TLS/RBAC, no escopo ou
na ausência de segredos. Ela encontrou, porém, lacunas na fronteira de mensagens externas:

- o consumidor materializa o body com `toBytes()` antes de aplicar limite de tamanho da aplicação;
- o consumidor não valida `content-type` nem o DTO antes do efeito e do settlement;
- o publisher não impede mensagem nula nem exige que o DTO tenha sido validado antes do envio;
- a validação planejada do namespace ainda não restringe o host ao domínio Azure público aprovado.

O usuário decidiu **Continuar ajustes**. C2 permanece pendente e será retomado em três fatias:

1. **C2.1 — decisão de contrato e segurança:** obter decisão humana explícita sobre o limite máximo
   do body, media types e parâmetros aceitos, presença/faixa dos campos e restrição do FQDN;
2. **C2.2 — correção documental:** atualizar configuração, publisher, consumidor, settlement,
   verificação manual e limites sem criar código executável;
3. **C2.3 — nova revisão:** repetir a conferência técnica e submeter novamente C2 à decisão humana.

A correção deverá rejeitar antes de materialização ou efeito qualquer entrada fora da política,
usar motivo de falha sanitizado, não registrar body nem identificadores e não escolher defaults
silenciosos. O valor do limite e as regras exatas de compatibilidade não serão inferidos porque
alteram validação de contrato, segurança e comportamento observável. Nenhum ADR novo é necessário:
a direção arquitetural do ADR-0009 permanece a mesma.

##### Decisão humana C2.1 — contrato e segurança

Em 2026-09-01, o usuário aprovou o pacote de remediação:

- body máximo de 64 KiB nas duas filas, validado antes de `toBytes()`;
- somente `application/json`, opcionalmente com `charset=utf-8`; ausência ou outro tipo é
  contrato inválido;
- mensagem e os dois campos obrigatórios, não nulos, não vazios e com até 256 caracteres; campos
  desconhecidos são rejeitados;
- FQDN restrito a um único namespace sob `.servicebus.windows.net`, sem scheme, porta, path ou
  trailing dot;
- publisher inválido falha antes do envio; mensagem recebida inválida segue para `DeadLetter`
  com motivo fixo e sanitizado, sem body ou identificadores em logs.

C2.1 está concluído. Essa aprovação autoriza somente a correção documental C2.2; implementação,
configuração executável e prova de ambiente continuam fora desta branch.

##### Resultado C2.2 — correção documental

Em 2026-09-01, o guia foi corrigido conforme o pacote aprovado em C2.1:

- os dois records de borda passaram a rejeitar campos nulos, em branco ou acima de 256 pontos de
  código, sem normalizar, truncar ou incluir valores nas mensagens de erro;
- o publisher passou a rejeitar mensagem nula, falha de serialização e corpo acima de 65.536
  bytes antes de `sendMessage(...)`, mantendo o `Uni` lazy e a falha observável;
- o consumidor passou a aceitar somente `application/json`, com `charset=utf-8` opcional, a
  rejeitar parâmetros adicionais e a aplicar `FAIL_ON_UNKNOWN_PROPERTIES`;
- a leitura do body passou a verificar `BinaryData.getLength()` e a materializar no máximo 65.537
  bytes por `toStream().readNBytes(...)`, sem `toBytes()` ilimitado;
- o namespace foi restringido a um único label DNS sob `.servicebus.windows.net`, sem label
  adicional, scheme, porta, path, credencial ou trailing dot;
- mensagens recebidas permanentemente inválidas passaram a usar uma nova instância mutável de
  `DeadLetterOptions`, com razão `CONTRATO_MENSAGEM_INVALIDO` e descrição fixa sanitizada; falha de
  leitura do stream permanece técnica;
- o roteiro manual passou a nove passos e incluiu rejeições controladas do publisher e do
  consumidor, inspeção não destrutiva da DLQ e critérios de parada para disposition divergente;
- limites e gates futuros foram alinhados às mesmas decisões, preservando
  `UNVERIFIED_IN_ENVIRONMENT` e a ausência de alegação executável;
- as APIs usadas na correção foram confrontadas com a documentação oficial de Java, Azure SDK e
  Jackson; os snippets continuam `UNVERIFIED_BY_COMPILATION_AND_ENVIRONMENT` porque esta branch é
  exclusivamente documental;
- nenhum ADR, Markdown arquitetural consolidado, código, teste, build, script, configuração
  executável ou formato derivado foi alterado; Maven, Sonar e conexão Azure não foram executados.

C2.2 está concluído. O próximo item permitido é C2.3, que repetirá a revisão técnica completa e
submeterá novamente C2 à decisão humana; esta correção não registra aprovação final do checkpoint.

##### Resultado C2.3 — segunda revisão técnica

Em 2026-09-01, a revisão técnica foi repetida sobre o guia integral e as correções C2.2. A
conferência confirmou, nas APIs e referências oficiais selecionadas, a composição lazy
`Mono<Void>` para `Uni<Void>`, o Processor contínuo sem polling, settlement manual, separação
entre tentativa funcional e `DeliveryCount`, leitura limitada do body, validação fechada do JSON,
DeadLetter sanitizado, credenciais por ambiente e transação `schedule + Complete` na mesma fila.
Não foram encontrados novos desvios de arquitetura, contrato, segurança, escopo ou exposição de
segredo nesses pontos.

Permaneceu um achado documental obrigatório antes da aprovação final de C2: o guia afirma que o
Service Bus deve preceder Azure Identity no `pom.xml` por um alerta vigente. O próprio README
oficial informa que esse problema foi resolvido em `azure-identity:1.2.1`; a matriz documental usa
`1.18.4`. A ordem mostrada é inofensiva, mas não pode ser apresentada como requisito atual. Uma
correção futura deverá remover essa justificativa obsoleta ou qualificá-la apenas como ordem do
exemplo, sem alterar dependências ou versões.

Como melhoria de rastreabilidade não bloqueante, os links de Config Mappings e lifecycle podem
ser fixados na documentação Quarkus `3.33`, em coerência com os demais links versionados. A fonte
atual continua oficial e não foi identificada divergência de API nesses dois trechos.

As verificações locais confirmaram 48 cercas de código em pares, cinco links relativos existentes,
ausência de valores com formato de private key, connection string ou JWT, e `git diff --check`
limpo tanto no diff comum quanto no stage. A branch permanece exclusivamente Markdown; snippets,
build e ambiente continuam não verificados, e Maven, SonarQube e conexão Azure não foram
executados.

C2.3 está concluído como atividade de revisão e o parecer foi submetido ao checkpoint humano. C2
permanece sem aprovação até decisão explícita do usuário sobre o achado obrigatório; nenhuma
correção do guia foi antecipada neste item.

##### Decisão humana após C2.3

Em 2026-09-01, o usuário declarou **Continuar ajustes**. A decisão não aprova C2 nem autoriza
implementação executável; ela mantém o checkpoint em ajuste e abre duas fatias documentais:

1. **C2.4 — correção focal do guia:** remover a justificativa obsoleta sobre a ordem das
   dependências Maven, sem mudar a matriz nem fixar versões individuais, e versionar em `3.33` os
   links Quarkus de Config Mappings e lifecycle;
2. **C2.5 — revisão focal:** confirmar a remoção da alegação, validar os links e a integridade
   Markdown e submeter C2 novamente à decisão humana.

O próximo item permitido é somente C2.4. Nenhum ADR novo é necessário porque a correção não muda
a escolha do Azure SDK, a autenticação, o transporte ou outra decisão do ADR-0009.

##### Resultado C2.4 — correção focal do guia

Em 2026-09-02, o guia recebeu somente as correções autorizadas para C2.4:

- a ordem de Service Bus antes de Azure Identity no fragmento Maven passou a ser apresentada como
  escolha de leitura, não como requisito de compatibilidade;
- o texto passou a registrar que o problema histórico de ordem foi resolvido em
  `azure-identity:1.2.1`, anterior à versão `1.18.4` gerenciada pela matriz documental, sem alterar
  o Azure SDK BOM `1.3.8` nem fixar versões individuais;
- os links de Config Mappings e lifecycle foram fixados na documentação Quarkus `3.33`;
- o estado corrente do guia passou a indicar C2.4 corrigido e C2.5 pendente;
- nenhum ADR, código, teste, build, script, configuração executável ou formato derivado foi
  alterado; Maven, SonarQube e conexão Azure não foram executados.

C2.4 está concluído. O próximo item permitido é C2.5, que fará a revisão focal, validará os links e
a integridade Markdown e submeterá C2 novamente à decisão humana. Esta correção não aprova C2 nem
antecipa o handoff da implementação.

##### Resultado C2.5 — revisão focal e nova submissão de C2

Em 2026-09-02, a revisão focal conferiu somente os ajustes de C2.4 e sua integridade documental:

- a alegação de que Service Bus deve preceder Azure Identity foi removida do guia; o texto atual
  trata a ordem apenas como apresentação e registra a resolução do problema histórico em
  `azure-identity:1.2.1`, anterior à matriz documental `1.18.4`;
- a página oficial do Azure Service Bus respondeu HTTP `200` e mantém a evidência da resolução em
  `azure-identity:1.2.1`;
- as páginas Quarkus `3.33` de Config Mappings e lifecycle responderam HTTP `200`;
- o anchor de lifecycle `#listening-for-startup-and-shutdown-events` existe no HTML oficial;
- foi encontrado um desvio focal: o guia usa `#configmapping`, mas o HTML oficial expõe somente
  `id="config-mappings"`; o link abre a página correta, porém não posiciona a seção pretendida;
- o link sem fragmento de Config Mappings mantido nas referências do plano permanece válido;
- as 48 cercas de código do guia estão pareadas, os cinco links Markdown relativos apontam para
  quatro arquivos existentes e `git diff --cached --check` está limpo;
- nenhum código, teste, build, script, configuração executável, ADR ou formato derivado foi
  alterado; Maven, SonarQube e conexão Azure não foram executados.

C2.5 está concluído como atividade de revisão e C2 foi submetido novamente à decisão humana. A
recomendação técnica é **Continuar ajustes** para trocar somente o fragmento por
`#config-mappings`; C2 não foi aprovado e nenhuma correção foi antecipada.

##### Decisão humana após C2.5

Em 2026-09-02, o usuário declarou **Continuar ajustes**. A decisão não aprova C2 nem autoriza
implementação executável; ela abre somente estas duas fatias documentais sequenciais:

1. **C2.6 — correção focal do anchor:** no guia, substituir apenas `#configmapping` por
   `#config-mappings`, sem alterar texto técnico, página base, versão, dependência ou decisão;
2. **C2.7 — revisão focal final:** confirmar HTTP `200`, existência do anchor no HTML Quarkus
   `3.33`, ausência da referência obsoleta, integridade Markdown e `git diff --check`, e submeter
   C2 novamente à decisão humana.

O critério de aceitação de C2.6 é o único link de Config Mappings do guia apontar para o anchor
oficial já observado. Sua verificação é uma busca focal no Markdown; a validação externa e o novo
checkpoint pertencem a C2.7. O próximo item permitido é somente C2.6.

##### Resultado C2.6 — correção focal do anchor

Em 2026-09-02, o único link de Config Mappings do guia passou de `#configmapping` para
`#config-mappings`. Página base, versão Quarkus, texto técnico, dependências e decisões foram
preservados. A busca focal confirmou a nova referência e a ausência do anchor antigo no guia; a
validação externa e a nova submissão de C2 permanecem reservadas a C2.7.

Nenhum ADR, código, teste, build, script, configuração executável ou formato derivado foi alterado;
Maven, SonarQube e conexão Azure não foram executados. C2.6 está concluído e o próximo item
permitido é somente C2.7.

##### Resultado C2.7 — revisão focal final e nova submissão de C2

Em 2026-09-02, a revisão focal final confirmou:

- Quarkus `3.33.2.1` e Java `25` permanecem as versões declaradas no `pom.xml`;
- as páginas oficiais do Azure Service Bus, Config Mappings e lifecycle responderam HTTP `200`;
- a página Azure mantém a evidência de resolução do problema histórico em
  `azure-identity:1.2.1`;
- os anchors `#config-mappings` e `#listening-for-startup-and-shutdown-events` existem no HTML
  oficial do Quarkus `3.33`;
- o guia contém uma única referência ao novo anchor, nenhuma ao antigo e nenhuma alegação de que
  Service Bus deve preceder Azure Identity;
- as 48 cercas de código permanecem pareadas, todos os destinos relativos existem e
  `git diff --cached --check` está limpo;
- nenhum desvio novo de link, Markdown, escopo ou rastreabilidade foi encontrado.

Nenhum Maven, SonarQube, teste, build ou conexão Azure foi executado porque a branch permanece
exclusivamente documental. C2.7 está concluído e C2 foi submetido novamente à decisão humana, com
recomendação técnica de aprovação; somente o usuário pode registrar essa decisão.

##### Decisão humana após C2.7

Em 2026-09-02, o usuário declarou **Continuar ajustes**. C2 não foi aprovado, e os itens 3.1,
3.2 e CF continuam não autorizados. Como C2.7 não registrou achado técnico pendente e a decisão
não identificou uma correção adicional, nenhuma nova fatia C2.x será aberta por suposição.

O próximo item permitido é somente a definição humana do ajuste adicional pretendido. Depois
dessa definição, plano e checklist deverão ser atualizados antes de qualquer alteração no guia.

##### Decisão humana final de C2

Em 2026-09-02, depois de esclarecer que não havia ajuste adicional, o usuário declarou
explicitamente **aprova C2**. Essa decisão substitui o estado intermediário de ajuste, conclui C2
e não autoriza nenhuma implementação executável. O próximo item permitido é somente 3.1; os itens
3.2 e CF permanecem pendentes e devem respeitar a ordem do checklist.

### Task 3 — Preparar o handoff para a implementação futura

**Descrição:** encerrar o guia com a sequência de implementação real, sem executá-la.

**Critérios de aceitação:**

- indicar nova branch `feature/...` e novo plano para produção;
- exigir baseline SonarQube antes da primeira alteração executável, conforme `AGENTS.md`;
- exigir GO e checkpoints humanos novamente, pois o guia não autoriza código;
- registrar testes unitários, teste de contrato do cliente Azure, integração com namespace não
  produtivo, ArchUnit e checkpoint Sonar como dívida intencional da implementação futura;
- registrar como próximos escopos independentes: idempotência, lock renewal, DLQ, observabilidade,
  retry, concorrência dimensionada e fluxo de negócio.

**Verificação:** revisão do checklist final e dos links para `AGENTS.md`, arquitetura e ADRs.

**Dependências:** Task 2 e checkpoint C2.

**Arquivos prováveis:** os mesmos da Task 2.

#### Resultado 3.1 — handoff para a implementação futura

Em 2026-09-02, ficou registrado o seguinte handoff, sem criar a futura feature:

- a branch proposta é `feature/implementar-service-bus-amqp-dossie`, a ser criada a partir da
  linha principal atualizada somente depois do encerramento e da integração desta branch
  documental;
- o planejamento futuro deverá usar os templates vigentes em
  `tasks/features/implementar-service-bus-amqp-dossie/plan.md` e `todo.md`; a pasta e os arquivos
  não são criados neste item;
- o plano deverá reler a arquitetura e os ADRs aplicáveis, reinspecionar código, contratos e testes
  no estado então vigente e tratar este guia e o ADR-0009 como entradas, não como prova executável;
- a primeira feature executável deverá recortar o transporte básico e sua prova como fatias
  pequenas e verificáveis, sem incorporar implicitamente processor da entrada, workflow,
  idempotência, Outbox, operação produtiva ou os demais incrementos posteriores do guia;
- antes da primeira alteração de código ou tooling, a futura feature deverá aplicar o fluxo de
  baseline SonarQube do `AGENTS.md`, sem escolher fonte ou pacote por suposição e sem solicitar ou
  registrar token no chat;
- o novo `todo.md` deverá iniciar com GO pendente. Os GO e checkpoints desta feature documental
  não autorizam produção; contrato, arquitetura, segurança e comportamento observável deverão ser
  novamente apresentados nos checkpoints aplicáveis antes das respectivas mudanças.

A consulta focal confirmou que a branch e a pasta propostas ainda não existem localmente. Nenhuma
branch, pasta de tasks, código, configuração executável ou formato derivado foi criado; Maven e
SonarQube não foram executados. O item 3.1 está concluído, e o próximo item permitido é somente 3.2.

#### Resultado 3.2 — não aplicável por decisão humana

Em 2026-09-02, o usuário esclareceu que deseja somente o guia e que não haverá prosseguimento para
a implementação. Em seguida, autorizou explicitamente registrar 3.2 como não aplicável. Os testes
e controles descritos no guia e no handoff permanecem como condicionantes informativos caso uma
nova feature seja solicitada no futuro, mas não constituem trabalho ativo desta feature.

### Checkpoint CF — Aceitação e encerramento humanos

- apresentar o guia, a evidência técnica e as limitações;
- somente o usuário registra aceitação, exceção ou encerramento;
- não iniciar implementação de produção como continuação implícita.

#### Resultado CF — guia aceito e feature documental encerrada

Em 2026-09-02, o usuário autorizou o encerramento da feature documental após confirmar que deseja
somente o guia. O conteúdo está completo, C2 está aprovado e a ausência de compilação, testes,
Maven, SonarQube ou conexão Azure permanece explicitamente registrada como
`EXECUTION_UNVERIFIED`, sem impedir a aceitação do artefato documental. Nenhuma implementação será
iniciada por esta feature.

## Verificação manual que o guia deverá descrever

Sem criar testes unitários, o roteiro futuro deverá permitir verificar em ambiente não produtivo:

1. aplicação inicia, cria os clientes de longa duração e health representa seu estado;
2. chamada técnica ao publisher envia o JSON conhecido para a fila de entrada;
3. o resultado assíncrono conclui somente após o broker aceitar o envio;
4. uma mensagem conhecida é colocada externamente na fila de saída;
5. o consumidor Azure aprovado é acionado pela entrega e converte as duas propriedades;
6. a mensagem é aceita após sucesso;
7. uma falha controlada produz a disposition aprovada e o comportamento de redelivery esperado;
8. nenhum segredo ou payload completo aparece nos logs.

Essa verificação não substitui os testes que deverão ser planejados na feature de implementação.

## SonarQube

- escopo atual: exclusivamente Markdown;
- baseline: não se aplica;
- Maven, API Sonar e checkpoint: não executar;
- se o escopo mudar para `pom.xml`, `src/`, teste, script, hook ou configuração executável,
  interromper antes da primeira alteração, verificar `sonar/` e cumprir a escolha de baseline do
  `AGENTS.md`;
- um projeto descartável fora das fontes do repositório não pode alterar o fingerprint executável
  nem produzir arquivos versionados.

## Riscos e controles

| Risco | Impacto | Controle |
|---|---|---|
| publicar receita incompatível com Azure | alto | Task 1 e evidência de dependências, Entra ID e TLS antes da seção executável |
| conflito entre BOMs Quarkus e Azure | alto | Azure SDK BOM explícito, dependency tree, compilação e testes na implementação |
| reintroduzir CBS experimental do SmallRye | alto | manter SmallRye fora desta borda e registrar a alternativa rejeitada |
| vazar token, SAS key ou connection string | alto | env/secret store; revisão textual; nunca registrar valores |
| ampliar privilégios da identidade | alto | Data Sender apenas na entrada e Data Receiver apenas na saída; não usar Data Owner |
| confundir direção das filas | alto | topologia no início e critérios de aceitação por direção |
| implementar polling em vez de consumo contínuo | alto | cliente Azure iniciado no ciclo de vida CDI, sem scheduler/loop |
| acoplar entrada e saída pelo mesmo DTO | médio | records de borda distintos com wire shape inicialmente igual |
| confirmar envio antes do broker | alto | compor o `Mono<Void>` de `sendMessage(...)` sem assinatura solta |
| bloquear event loop | alto | sender assíncrono e modelo de consumo aprovado; proibir espera síncrona |
| complete prematuro ou settlement incorreto | alto | `PEEK_LOCK`, auto-complete desabilitado e política explícita |
| usar `MaxDeliveryCount` como limite funcional | alto | contador contratual separado e quarentena explícita |
| extrapolar a atomicidade do broker ou configurar cross-entity sem necessidade | alto | limitar a garantia a schedule + Complete na mesma fila, com builder compartilhado, tier Standard/Premium e prova futura de commit/rollback |
| escolher Receiver assíncrono sem recuperação | alto | decisão C1 explícita entre Processor recomendado e Receiver reativo |
| supor paralelismo pelo cliente assíncrono | médio | validar `maxConcurrentCalls` ou operadores Reactor |
| ausência temporária de testes unitários virar permanente | médio | registrar dívida no handoff da implementação |
| documentação divergir após upgrade | médio | links versionados e matriz de versões no guia |
| expandir para o workflow completo | médio | checklist de fora de escopo e revisão C2 |

## Dependências

- revisão e autorização humanas deste plano;
- permanência obrigatória na linha Quarkus `3.33` LTS, sem override isolado do SmallRye;
- decisão humana sobre contrato, plataforma, segurança e comportamento observável em C1;
- documentação oficial versionada do Azure SDK Java e Azure Identity;
- documentação oficial do Azure Service Bus sobre AMQP, autenticação, settlement e transações;
- tier Standard ou Premium para a futura prova de transação; Basic não oferece transações;
- para prova real: namespace e duas filas não produtivas, rede `443` apta a WebSockets/TLS e
  identidade/token injetado de modo seguro;
- para implementação futura: plano próprio, GO, baseline Sonar e testes.

## Fontes oficiais para elaboração do guia

- [Quarkus — histórico e suporte das releases](https://quarkus.io/releases/);
- [Quarkus 3.33 — referência de configuração](https://quarkus.io/version/3.33/guides/config-reference);
- [Quarkus 3.33 — Config Mappings](https://quarkus.io/version/3.33/guides/config-mappings);
- [Quarkus 3.33 — Getting Started with AMQP 1.0](https://quarkus.io/version/3.33/guides/amqp);
- [Quarkus 3.33 — AMQP connector reference](https://quarkus.io/version/3.33/guides/amqp-reference);
- [Quarkus 3.33 — Messaging, execution model and emitters](https://quarkus.io/version/3.33/guides/messaging);
- [SmallRye Reactive Messaging 4.33.0 — AMQP connector](https://smallrye.io/smallrye-reactive-messaging/4.33.0/amqp/amqp/);
- [SmallRye Reactive Messaging 4.36.0 — AMQP receiving/configuration reference](https://smallrye.io/smallrye-reactive-messaging/4.36.0/amqp/receiving-amqp-messages/);
- [SmallRye Reactive Messaging — CBS authentication experimental](https://smallrye.io/smallrye-reactive-messaging/latest/amqp/cbs-authentication/),
  consultada em 2026-08-31 para identificar o suporte `put-token` introduzido após `4.33.0`;
- [Azure SDK for Java — Service Bus client library](https://learn.microsoft.com/en-us/java/api/overview/azure/messaging-servicebus-readme?view=azure-java-stable);
- [Azure SDK for Java — `ServiceBusSenderAsyncClient`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebussenderasyncclient?view=azure-java-stable);
- [Azure SDK for Java — `ServiceBusReceiverAsyncClient`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusreceiverasyncclient?view=azure-java-stable);
- [Azure SDK for Java — `ServiceBusClientBuilder`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusclientbuilder?view=azure-java-stable);
- [Azure SDK for Java — `ServiceBusProcessorClient`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusprocessorclient?view=azure-java-stable);
- [Azure SDK for Java — Processor builder](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusclientbuilder.servicebusprocessorclientbuilder?view=azure-java-stable);
- [Azure SDK for Java — contexto e settlement da mensagem](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusreceivedmessagecontext?view=azure-java-stable);
- [Azure SDK for Java — `ServiceBusMessage`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusmessage?view=azure-java-stable);
- [Azure SDK for Java — `ServiceBusReceivedMessage`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusreceivedmessage?view=azure-java-stable);
- [Project Reactor — `Mono`](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html);
- [Azure SDK for Java — troubleshooting de concorrência](https://learn.microsoft.com/en-us/azure/developer/java/sdk/troubleshooting-messaging-service-bus-overview#concurrency-in-servicebusprocessorclient);
- [Azure SDK for Java — `AmqpTransportType`](https://learn.microsoft.com/en-us/java/api/com.azure.core.amqp.amqptransporttype?view=azure-java-stable);
- [Azure SDK for Java — `CompleteOptions`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.models.completeoptions?view=azure-java-stable);
- [Azure SDK for Java — `ServiceBusTransactionContext`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebustransactioncontext?view=azure-java-stable);
- [Azure SDK for Java — guia de migração do Service Bus](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/servicebus/azure-messaging-servicebus/migration-guide.md);
- [Azure SDK for Java — Azure Identity](https://learn.microsoft.com/en-us/java/api/overview/azure/identity-readme?view=azure-java-stable);
- [Azure Identity for Java — cadeias de credenciais](https://learn.microsoft.com/en-us/azure/developer/java/sdk/authentication/credential-chains);
- [Azure-hosted Java apps — identidade gerenciada atribuída pelo sistema](https://learn.microsoft.com/en-us/azure/developer/java/sdk/authentication/azure-hosted-apps);
- [Azure SDK for Java — Maven e Azure SDK BOM](https://learn.microsoft.com/en-us/azure/developer/java/sdk/get-started-maven);
- [Azure SDK for Java — POM do Azure SDK BOM `1.3.8`](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/boms/azure-sdk-bom/pom.xml);
- [Azure SDK for Java — changelog do Service Bus](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/servicebus/azure-messaging-servicebus/CHANGELOG.md);
- [Azure Service Bus — AMQP 1.0 protocol guide](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-amqp-protocol-guide);
- [Azure Service Bus — FAQ de portas e AMQP sobre WebSockets](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-faq#what-ports-do-i-need-to-open-on-the-firewall);
- [Azure Service Bus — authentication and authorization](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-authentication-and-authorization);
- [Azure Service Bus — autenticar aplicação com Microsoft Entra ID](https://learn.microsoft.com/en-us/azure/service-bus-messaging/authenticate-application);
- [Azure Service Bus — usar identidades gerenciadas](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-managed-service-identity);
- [Azure Service Bus — desabilitar autenticação local/SAS](https://learn.microsoft.com/en-us/azure/service-bus-messaging/disable-local-authentication);
- [Azure Service Bus — Explorer no portal](https://learn.microsoft.com/en-us/azure/service-bus-messaging/explorer);
- [Azure Service Bus — message transfers, locks and settlement](https://learn.microsoft.com/en-us/azure/service-bus-messaging/message-transfers-locks-settlement);
- [Azure Service Bus — prevenção de perda e processamento duplicado](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-message-loss-and-duplicates);
- [Azure Service Bus — prefetch](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-prefetch);
- [Azure Service Bus — transactions overview](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-transactions);
- [Azure Service Bus — scheduled messages](https://learn.microsoft.com/en-us/azure/service-bus-messaging/message-sequencing#scheduled-messages);
- [Azure Service Bus — quotas and limits](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-quotas);
- [Azure Service Bus — messages and application properties](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-messages-payloads);
- [Azure Service Bus — dead-letter queues](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-dead-letter-queues);
- [Quarkus 3.33 — lifecycle da aplicação](https://quarkus.io/version/3.33/guides/lifecycle#listening-for-startup-and-shutdown-events);
- [Quarkus 3.33 — SmallRye Health](https://quarkus.io/version/3.33/guides/smallrye-health);
- [Quarkus 3.33 — testes da aplicação](https://quarkus.io/version/3.33/guides/getting-started-testing).

## GO registrado

O checkpoint C0 foi aprovado pelo usuário em 2026-08-30 e está registrado no `todo.md`. A
autorização cobre somente a elaboração documental deste guia e não autoriza qualquer mudança de
produção.
