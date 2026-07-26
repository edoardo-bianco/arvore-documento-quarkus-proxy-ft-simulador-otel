# ADR-0010: Persistir a conformidade no CouchDB e checkpoints Flow no Redis

- **Status:** Proposto
- **Decisão em uma frase:** evoluir a PoC de conformidade para armazenar dados de
  negócio no CouchDB, checkpoints técnicos do Quarkus Flow no Redis/Valkey e revisão
  HITL por CloudEvent sem Kafka, com identidade durável para múltiplos pods.
- **Quando consultar:** persistência da conformidade, contexto ou checkpoint do Flow,
  CouchDB, Redis/Valkey, CloudEvent sem broker, idempotência da revisão, Docker,
  Kubernetes, múltiplos pods ou identificadores expostos pela API e pela página.

## Contexto

O ADR-0009 autorizou uma PoC em memória, com `flow-in`/`flow-out` internos e sem
promessa de retomada depois de reinício. A pausa e a retomada HITL foram comprovadas,
mas instância, projeção, resultado preliminar e revisão ainda dependem da mesma JVM.

A evolução solicitada precisa:

- sobreviver a reinício da aplicação;
- manter os dados JSON de negócio fora do backend de checkpoint do Flow;
- correlacionar de forma explícita análise, instância técnica, documento/texto e
  checklist;
- aceitar uma única resposta humana para a solicitação correspondente, tolerando
  repetições técnicas;
- continuar sem Kafka, AMQP ou broker externo;
- ser empacotável em containers e evoluir para múltiplos pods.

Quarkus Flow `0.10.2` oferece `quarkus-flow-redis` para persistência de estado e
`quarkus-flow-durable-kubernetes` para associar a identidade lógica do worker a
Kubernetes Leases. A mesma versão permite substituir a ponte padrão por implementações
próprias de `EventConsumer` e `EventPublisher`.

O CouchDB armazena documentos JSON, usa MVCC por `_rev` e oferece o feed `_changes`.
O feed pode repetir mudanças; portanto, a entrega deve ser tratada como pelo menos uma
vez, nunca como exatamente uma vez.

## Decisão proposta

### Identidades

Usar quatro identidades com responsabilidades diferentes:

- `correlationId`: identificador estável, gerado pelo Hub, da análise ponta a ponta;
- `instanceId`: identificador técnico da instância do Quarkus Flow;
- `identificadorDocumento`: identificador negocial informado para o texto inicial e
  preservado quando a entrada evoluir para documento;
- `identificadorChecklist` e `versaoChecklist`: referência negocial do checklist
  efetivamente aplicado.

`correlationId` e `instanceId` não são intercambiáveis. A API continuará aceitando o
`instanceId` nos paths atuais por compatibilidade, e as respostas passarão a expor,
de forma aditiva, todas as identidades acima. A página exibirá esses valores como
somente leitura durante processamento, revisão, conclusão e falha.

### Dados de negócio no CouchDB

O CouchDB será o sistema de registro dos dados da análise:

- metadados e correlação da análise;
- texto inicial identificado por `identificadorDocumento`;
- snapshot imutável do checklist consultado;
- resultado preliminar;
- solicitação e resposta de revisão humana;
- resultado final, falha sanitizada e projeção usada pelo polling.

Documentos que representam fatos de integração serão imutáveis e usarão IDs
determinísticos. A resposta humana terá chave derivada de `correlationId`; uma repetição
com o mesmo conteúdo será aceita idempotentemente, enquanto conteúdo diferente para a
mesma chave produzirá conflito.

Transições mutáveis da projeção usarão `_rev`/MVCC. Não haverá lock de linha,
transação relacional ou compartilhamento de DTOs CouchDB com REST, Flow ou Ollama.

### Checkpoints técnicos no Redis/Valkey

Adicionar `quarkus-flow-redis` como único provider de persistência do Flow. O backend
será Redis ou, preferencialmente para a execução open source em container, Valkey
compatível com o protocolo Redis. A escolha do servidor e da imagem será fixada no
checkpoint humano e validada contra a versão `0.10.2`.

O contexto persistido pelo Flow conterá somente IDs técnicos, referências imutáveis e
hashes. Texto, checklist, revisão e resultados completos permanecerão no CouchDB e
serão carregados por portas de aplicação quando cada etapa precisar deles.

Redis/Valkey não será sistema de registro de dados de negócio nem será tratado como
transporte de eventos apenas por armazenar checkpoints.

### Revisão HITL sem Kafka

Manter `emitJson(...) -> listen(...)` e CloudEvent v1. O CloudEvent de retomada
conterá somente:

- `id` determinístico;
- `type`;
- `source` e `time`;
- `flowinstanceid`;
- `correlationId`;
- referência e hash do documento de revisão no CouchDB.

Os eventos emitidos pelo workflow também serão referenciais. Um adapter do SPI
`EventPublisher` persistirá no CouchDB o fato de solicitação de revisão e atualizará
a projeção; resultado preliminar e checklist já terão documentos próprios. A ponte
Reactive Messaging padrão será desabilitada depois que os adapters `EventPublisher`
e `EventConsumer` estiverem comprovados.

O endpoint de revisão gravará primeiro um documento imutável no CouchDB. Um adapter
de entrada baseado no `_changes`, implementado pelo SPI oficial `EventConsumer`,
converterá a mudança em CloudEvent e a entregará ao Flow. O `listen` filtrará a
instância e a correlação esperadas.

A idempotência terá duas camadas:

1. CouchDB impede respostas contraditórias para a mesma chave lógica;
2. o workflow valida correlação, estado e conteúdo antes de consolidar uma única
   revisão.

Documento repetido, reconexão do `_changes` ou failover de pod não poderá concluir a
análise duas vezes. Documento inválido deverá permanecer observável e não bloquear
indefinidamente as demais revisões.

### Containers e múltiplos pods

Fornecer execução local em containers para aplicação, CouchDB, Redis/Valkey e Ollama,
com volumes duráveis, health checks e segredos somente por configuração externa.
Docker Compose validará inicialmente uma réplica.

Para múltiplos pods, usar Kubernetes e `quarkus-flow-durable-kubernetes`:

- cada pod adquire um Lease estável;
- o Lease define o `WorkflowApplication` ID;
- readiness exige Lease adquirido;
- um novo pod que assuma o mesmo Lease restaura do Redis/Valkey as instâncias daquele
  worker;
- a estratégia de rolling update preserva pelo menos um Lease liberável.

Um teste de integração com duas réplicas deverá provar que uma revisão recebida por
réplica diferente daquela que iniciou a análise retoma exatamente a instância
correlacionada. A documentação oficial não define completamente o roteamento
cross-pod sem broker; se o spike falhar, este ADR permanece `Proposto` e a alternativa
de distribuição exige novo checkpoint humano, sem adoção automática de Kafka.

## Consequências

- reinícios deixam de apagar dados de negócio e checkpoints pausados;
- CouchDB e Redis/Valkey tornam-se dependências operacionais da PoC;
- o contexto do workflow fica menor, mas cada etapa passa a resolver referências
  externas;
- o `_changes` funciona como fonte durável de notificações e exige cursor,
  reconexão, reprocessamento e tratamento de documentos inválidos;
- a semântica é pelo menos uma vez; idempotência deixa de ser opcional mesmo havendo
  uma única resposta humana esperada;
- a aplicação assume parte das responsabilidades que um broker forneceria pronto,
  como replay, observação de falhas e descarte controlado;
- backup, retenção, compactação, credenciais, TLS e saúde de CouchDB/Redis precisam
  ser documentados antes de uso fora do ambiente local;
- a extensão de persistência Flow está em estágio preview e exige testes de restart,
  compatibilidade e carga proporcionais ao risco;
- os identificadores adicionais ampliam o contrato REST de forma aditiva, mas
  `identificadorDocumento` passa a ser entrada obrigatória e exige checkpoint de
  contrato;
- se aceito, este ADR substituirá a decisão de volatilidade e canais exclusivamente
  locais do ADR-0009; as decisões de agente, Ollama, FT e telemetria sanitizada
  permanecerão vigentes.

## Alternativas consideradas

- **PostgreSQL com `quarkus-flow-jpa`:** tecnicamente válido, mas rejeitado para esta
  evolução porque a decisão humana separa documentos de negócio no CouchDB e
  checkpoints no Redis/Valkey.
- **Kafka ou AMQP:** oferecem transporte e redelivery maduros, mas continuam fora do
  escopo explícito.
- **CloudEvent apenas em memória:** simples em uma JVM, porém pode perder a revisão
  aceita durante crash e não resolve roteamento entre pods.
- **Redis/Valkey também como banco de negócio:** reduziria componentes, mas misturaria
  checkpoint técnico e documentos de negócio.
- **Remover `listen` e chamar uma continuação diretamente pelo REST:** elimina o
  CloudEvent, mas abandona a pausa/retomada nativa e transfere correlação e lifecycle
  para código proprietário.
- **Polling periódico do workflow no CouchDB:** evita eventos, mas mantém execução e
  temporização customizadas; o `_changes` oferece uma fronteira reativa mais adequada.

## Evidências

- persistência do Quarkus Flow `0.10.2`:
  <https://github.com/quarkiverse/quarkus-flow/blob/0.10.2/docs/modules/ROOT/pages/persistence.adoc>;
- Messaging e SPI próprios do Flow `0.10.2`:
  <https://github.com/quarkiverse/quarkus-flow/blob/0.10.2/docs/modules/ROOT/pages/messaging.adoc>;
- coordenação durável com Kubernetes Leases:
  <https://github.com/quarkiverse/quarkus-flow/blob/0.10.2/docs/modules/ROOT/pages/concepts-durable-workflow-k8s.adoc>;
- feed `_changes` do CouchDB:
  <https://docs.couchdb.org/en/stable/api/database/changes.html>;
- MVCC e conflitos do CouchDB:
  <https://docs.couchdb.org/en/stable/replication/conflicts.html>;
- imagem oficial CouchDB:
  <https://hub.docker.com/_/couchdb/>.

## Critério de aceitação

O ADR permanece `Proposto` até o checkpoint C4 confirmar explicitamente:

1. CouchDB como único sistema de registro dos dados de negócio da análise;
2. `quarkus-flow-redis` com Redis ou Valkey para checkpoints técnicos;
3. `EventPublisher` para emissões referenciais e `_changes` -> `EventConsumer` ->
   CloudEvent como caminho de retomada sem Kafka;
4. `correlationId`, `instanceId`, `identificadorDocumento`,
   `identificadorChecklist` e `versaoChecklist` no contrato e na página;
5. Kubernetes Leases e teste cross-pod/failover antes de declarar suporte a múltiplos
   pods;
6. manutenção do ADR como `Proposto` se a entrega cross-pod não for comprovada.
