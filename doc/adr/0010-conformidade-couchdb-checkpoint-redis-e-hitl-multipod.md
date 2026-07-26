# ADR-0010: Persistir a conformidade em backend documental por ambiente e checkpoints Flow no Redis

- **Status:** Proposto
- **Decisão em uma frase:** evoluir a PoC para armazenar dados de negócio por uma
  porta documental neutra, implementada por Apache CouchDB em DES e Azure Cosmos DB
  for NoSQL em PRD, mantendo checkpoints técnicos do Flow no Redis/Valkey e revisão
  HITL por CloudEvent sem Kafka.
- **Quando consultar:** persistência da conformidade, contexto ou checkpoint do Flow,
  CouchDB, Cosmos DB for NoSQL, Redis/Valkey, Change Feed, CloudEvent sem broker,
  idempotência da revisão, Docker, Kubernetes, múltiplos pods ou identificadores
  expostos pela API e pela página.

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
Azure Cosmos DB for NoSQL armazena itens JSON, usa `_etag`/`If-Match` para
concorrência otimista e oferece Change Feed Processor no Java SDK v4. Os dois feeds
possuem semântica de entrega pelo menos uma vez.

O backend documental não pode vazar para aplicação, domínio, REST, Flow ou Ollama.
Sem essa fronteira, usar CouchDB em DES e Cosmos em PRD criaria dois comportamentos
de negócio e tornaria os testes locais pouco representativos.

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

### Contrato documental neutro

A persistência documental será expressa por portas pequenas orientadas às
necessidades da conformidade. O contrato lógico será o mesmo nos dois ambientes e
abrangerá:

- metadados e correlação da análise;
- texto inicial identificado por `identificadorDocumento`;
- snapshot imutável do checklist consultado;
- resultado preliminar;
- solicitação e resposta de revisão humana;
- resultado final, falha sanitizada e projeção usada pelo polling.

Documentos que representam fatos de integração serão imutáveis e usarão IDs
determinísticos. A resposta humana terá chave derivada de `correlationId`; repetição
com o mesmo conteúdo será aceita idempotentemente, enquanto conteúdo diferente para
a mesma chave produzirá conflito.

`correlationId`, já confirmado como texto, será também a chave lógica de partição dos
itens no Cosmos. A versão concorrente será opaca fora do adapter: CouchDB traduzirá
essa necessidade para `_rev`; Cosmos DB for NoSQL, para `_etag` e `If-Match`. Esses
campos nativos e os DTOs de cada banco não atravessarão a borda do adapter.

O mesmo contrato executável deverá ser aplicado aos dois adapters. Ele comprovará
documentos canônicos, projeção, imutabilidade, repetição idêntica, conflito
contraditório, concorrência otimista e tradução de falhas.
Hashes canônicos serão `String`, calculados com SHA-256 e representados por 64
caracteres hexadecimais minúsculos no Java e no JSON.

### Backend por ambiente

- **DES:** Apache CouchDB, acessado por sua API HTTP/JSON e limitado à rede interna;
- **PRD:** Azure Cosmos DB for NoSQL, acessado pelo Azure Cosmos DB Java SDK v4;
- **pré-promoção:** integração opt-in contra Azure Cosmos DB Emulator ou conta
  Cosmos não produtiva, obrigatória antes de promover a versão para PRD.

O emulador apoia desenvolvimento e CI, mas não substitui a validação das
características do serviço gerenciado que ele não reproduz. MongoDB não é adapter
desta decisão: seu protocolo corresponderia ao Azure Cosmos DB for MongoDB, enquanto
a API produtiva escolhida é Azure Cosmos DB for NoSQL.

A autenticação produtiva do Cosmos será definida em checkpoint de segurança próprio.
Até essa decisão, nenhuma chave, connection string ou mecanismo de identidade será
fixado silenciosamente em código ou configuração.

### Checkpoints técnicos no Redis/Valkey

Adicionar `quarkus-flow-redis` como único provider de persistência do Flow. O backend
será Redis ou, preferencialmente para a execução open source em container, Valkey
compatível com o protocolo Redis. A escolha do servidor e da imagem será fixada no
checkpoint humano e validada contra a versão `0.10.2`.

O contexto persistido pelo Flow conterá somente IDs técnicos, referências imutáveis e
hashes. Texto, checklist, revisão e resultados completos permanecerão no backend
documental selecionado e serão carregados por portas de aplicação quando cada etapa
precisar deles.

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
- referência e hash do documento de revisão.

Os eventos emitidos pelo workflow também serão referenciais. Um adapter do SPI
`EventPublisher` persistirá no backend documental o fato de solicitação de revisão e
atualizará a projeção; resultado preliminar e checklist já terão documentos próprios.
A ponte Reactive Messaging padrão será desabilitada depois que os adapters
`EventPublisher` e `EventConsumer` estiverem comprovados.

O endpoint de revisão gravará primeiro um documento imutável pelo contrato neutro.
O adapter de entrada converterá a mudança em CloudEvent e a entregará ao Flow:

- em DES, consumirá o `_changes` do CouchDB com cursor persistido;
- em PRD, usará o Change Feed Processor do Azure Cosmos DB Java SDK v4 e seu
  container de leases.

O `listen` filtrará a instância e a correlação esperadas.

A idempotência terá duas camadas:

1. o backend documental impede respostas contraditórias para a mesma chave lógica;
2. o workflow valida correlação, estado e conteúdo antes de consolidar uma única
   revisão.

Documento repetido, reconexão do feed ou failover de pod não poderá concluir a
análise duas vezes. Documento inválido deverá permanecer observável e não bloquear
indefinidamente as demais revisões. A semântica continuará sendo pelo menos uma vez
nos dois backends.

### Containers e múltiplos pods

Fornecer execução local de DES em containers para aplicação, CouchDB, Redis/Valkey e
Ollama, com volumes duráveis, health checks e segredos somente por configuração
externa. Docker Compose validará inicialmente uma réplica. O Cosmos Emulator será
opt-in e não substituirá o CouchDB como backend normal de DES.

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
- CouchDB em DES, Cosmos DB for NoSQL em PRD e Redis/Valkey tornam-se dependências
  operacionais da PoC;
- o contexto do workflow fica menor, mas cada etapa passa a resolver referências
  externas;
- `_changes` e Change Feed Processor funcionam como fontes duráveis de notificações
  e exigem retomada, reprocessamento e tratamento de documentos inválidos;
- a semântica é pelo menos uma vez; idempotência deixa de ser opcional mesmo havendo
  uma única resposta humana esperada;
- a aplicação assume parte das responsabilidades que um broker forneceria pronto,
  como replay, observação de falhas e descarte controlado;
- a diferença entre os backends cria risco de desvio semântico, controlado por
  contrato compartilhado e gate Cosmos antes da promoção;
- backup, retenção, compactação, credenciais, identidade, TLS e saúde dos backends
  precisam ser documentados antes de uso fora do ambiente local;
- a extensão de persistência Flow está em estágio preview e exige testes de restart,
  compatibilidade e carga proporcionais ao risco;
- os identificadores adicionais ampliam o contrato REST de forma aditiva, mas
  `identificadorDocumento` passa a ser entrada obrigatória e exige checkpoint de
  contrato;
- se aceito, este ADR substituirá a decisão de volatilidade e canais exclusivamente
  locais do ADR-0009; as decisões de agente, Ollama, FT e telemetria sanitizada
  permanecerão vigentes.

## Alternativas consideradas

- **CouchDB também em PRD:** reduziria o risco de desvio entre ambientes, mas foi
  substituído pela decisão humana de usar Azure Cosmos DB for NoSQL em PRD.
- **MongoDB em DES:** rejeitado porque corresponderia ao protocolo da API do Azure
  Cosmos DB for MongoDB, não à API for NoSQL escolhida para PRD; também não seria um
  substituto transparente do SDK, `_etag` e Change Feed do Cosmos.
- **PostgreSQL com `quarkus-flow-jpa`:** tecnicamente válido, mas rejeitado porque
  mistura dados documentais de negócio com uma escolha relacional não solicitada.
- **Kafka ou AMQP:** oferecem transporte e redelivery maduros, mas continuam fora do
  escopo explícito.
- **CloudEvent apenas em memória:** simples em uma JVM, porém pode perder a revisão
  aceita durante crash e não resolve roteamento entre pods.
- **Redis/Valkey também como banco de negócio:** reduziria componentes, mas misturaria
  checkpoint técnico e documentos de negócio.
- **Remover `listen` e chamar uma continuação diretamente pelo REST:** elimina o
  CloudEvent, mas abandona a pausa/retomada nativa e transfere correlação e lifecycle
  para código proprietário.
- **Polling periódico do workflow no banco:** evita feeds, mas mantém execução e
  temporização customizadas; `_changes` e Change Feed oferecem fronteiras reativas
  mais adequadas.

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
  <https://hub.docker.com/_/couchdb/>;
- Azure Cosmos DB Java SDK v4:
  <https://learn.microsoft.com/en-us/azure/cosmos-db/sdk-java-v4>;
- concorrência otimista por `_etag` e `If-Match`:
  <https://learn.microsoft.com/en-us/azure/cosmos-db/database-transactions-optimistic-concurrency>;
- Change Feed Processor e semântica pelo menos uma vez:
  <https://learn.microsoft.com/en-us/azure/cosmos-db/change-feed-processor>;
- Cosmos DB Emulator e suas diferenças para o serviço:
  <https://learn.microsoft.com/en-us/azure/cosmos-db/emulator>;
- RBAC nativo do plano de dados e Microsoft Entra ID, como fonte para o checkpoint
  de autenticação ainda pendente:
  <https://learn.microsoft.com/en-us/azure/cosmos-db/how-to-connect-role-based-access-control>.

## Critério de aceitação

Os checkpoints C4 e C5 já autorizaram a implementação desta proposta. O ADR permanece
`Proposto` até a prova cross-pod/failover e uma decisão humana posterior de aceitação.
As condições vigentes são:

1. porta documental neutra e contrato executável compartilhado;
2. CouchDB em DES e Azure Cosmos DB for NoSQL em PRD;
3. `_rev`/`_changes` confinados ao adapter CouchDB e `_etag`/Change Feed confinados
   ao adapter Cosmos;
4. validação opt-in contra Cosmos Emulator ou conta não produtiva como gate antes da
   promoção para PRD;
5. `quarkus-flow-redis` com Redis ou Valkey somente para checkpoints técnicos;
6. `EventPublisher` para emissões referenciais e feed nativo ->
   `EventConsumer` -> CloudEvent como caminho de retomada sem Kafka;
7. `correlationId`, `instanceId`, `identificadorDocumento`,
   `identificadorChecklist` e `versaoChecklist` no contrato e na página;
8. checkpoint de segurança antes de fixar a autenticação produtiva do Cosmos;
9. Kubernetes Leases e teste cross-pod/failover antes de declarar suporte a múltiplos
   pods;
10. manutenção do ADR como `Proposto` se a entrega cross-pod não for comprovada.
