# Compatibilidade: Quarkus Flow, LangChain4j e Ollama

## Resultado

O spike de 2026-07-24 comprovou que é possível manter Quarkus `3.33.2.1` e Java 25
usando a mesma combinação efetiva do projeto `newsletter-drafter`:

```text
Quarkus Flow             0.10.2
Quarkus LangChain4j      1.11.2
LangChain4j core         1.16.2
LangChain4j Agentic      1.16.2-beta26
```

O POM de produção não foi alterado. O spike foi criado sob `target/`, compilado,
executado e removido após a coleta das evidências.

## Fontes verificadas

- POM local de referência:
  `C:/desenvolvimento/repositorio/edo-quarkus-flow-prj/apps/newsletter-drafter/pom.xml`;
- configuração agentic/workflow local:
  `NewsletterWorkflow.java` e `AutoDraftCriticAgent.java` do mesmo projeto;
- documentação oficial Flow–LangChain4j:
  <https://docs.quarkiverse.io/quarkus-flow/dev/langchain4j.html>;
- documentação oficial Flow Messaging:
  <https://docs.quarkiverse.io/quarkus-flow/dev/messaging.html>;
- documentação oficial Quarkus Messaging:
  <https://quarkus.io/guides/messaging>;
- documentação oficial Ollama:
  <https://docs.quarkiverse.io/quarkus-langchain4j/dev/ollama-chat-model.html>;
- POMs e metadados publicados no Maven Central:
  <https://central.sonatype.com/artifact/io.quarkiverse.flow/quarkus-flow> e
  <https://central.sonatype.com/artifact/io.quarkiverse.langchain4j/quarkus-langchain4j-ollama>.

## Matriz de versões

| Componente | Projeto atual | `newsletter-drafter` efetivo | Spike aprovado tecnicamente |
|---|---:|---:|---:|
| Java | 25 | 17 | 25 |
| Quarkus | 3.33.2.1 | 3.37.0 | 3.33.2.1 |
| Flow | ausente | 0.10.2 | 0.10.2 |
| Quarkus LangChain4j | ausente | 1.11.2 | 1.11.2 |
| LangChain4j core | ausente | 1.16.2 | 1.16.2 |

Flow `0.10.2` e Quarkus LangChain4j `1.11.2` foram ambos construídos contra Quarkus
`3.33.2`. A plataforma principal do spike manteve todos os artefatos
`io.quarkus:*` em `3.33.2.1`.

## BOMs

Não existe:

```text
io.quarkus.platform:quarkus-flow-bom:3.33.2.1
```

Por isso, repetir literalmente o POM do `newsletter-drafter` não é possível. A
combinação compilada foi:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.quarkus.platform</groupId>
            <artifactId>quarkus-bom</artifactId>
            <version>3.33.2.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>io.quarkiverse.langchain4j</groupId>
            <artifactId>quarkus-langchain4j-bom</artifactId>
            <version>1.11.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>io.quarkiverse.flow</groupId>
            <artifactId>quarkus-flow-bom</artifactId>
            <version>0.10.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Dependências compiladas sem versão individual:

```text
io.quarkiverse.flow:quarkus-flow
io.quarkiverse.flow:quarkus-flow-langchain4j
io.quarkiverse.flow:quarkus-flow-messaging
io.quarkiverse.langchain4j:quarkus-langchain4j-agentic
io.quarkiverse.langchain4j:quarkus-langchain4j-ollama
io.quarkus:quarkus-messaging
```

`quarkus-messaging-kafka` não apareceu na árvore.

## APIs compiladas

O spike reutilizou os mesmos imports e formas do `newsletter-drafter`:

- `FuncDSL.agent`;
- `FuncDSL.emitJson`;
- `FuncDSL.listen`;
- `FuncDSL.consumed(...).extensionByInstanceId("flowinstanceid")`;
- `@RegisterAiService`;
- `@SequenceAgent`;
- `@Agent`;
- retorno estruturado em record;
- `Uni.subscribeAsCompletionStage()`.

`mvn -q -DskipTests package` terminou com código 0.

## Configuração verificada nos JARs

Os metadados `quarkus-config-model.json` dos artefatos resolvidos confirmaram:

```properties
quarkus.flow.messaging.defaults-enabled
quarkus.flow.messaging.lifecycle-enabled
quarkus.flow.tracing.enabled
quarkus.flow.devui.backend.storage.enabled

quarkus.langchain4j.ollama.devservices.enabled
quarkus.langchain4j.ollama.base-url
quarkus.langchain4j.ollama.chat-model.model-id
quarkus.langchain4j.ollama.chat-model.temperature
quarkus.langchain4j.ollama.chat-model.model-options.num-ctx
quarkus.langchain4j.ollama.chat-model.format
quarkus.langchain4j.ollama.timeout
quarkus.langchain4j.log-requests
quarkus.langchain4j.log-responses
```

## Divergência de Messaging

O `newsletter-drafter` usa `quarkus-messaging-kafka`. Esse connector ativa
automaticamente os beans:

```text
FlowMessagingConsumer
FlowDomainEventsPublisher
```

Em Flow `0.10.2`, `FlowMessagingProcessor` só registra esses beans quando estão
presentes:

```text
mp.messaging.incoming.flow-in.connector
mp.messaging.outgoing.flow-out.connector
```

O primeiro bootstrap, sem connector, iniciou a aplicação mas registrou:

```text
Emitter flow-in has no downstream
Subscriber flow-out has no upstream
```

O segundo bootstrap registrou subclasses CDI mínimas dos dois componentes Flow. O
resultado:

- aplicação iniciada com Quarkus `3.33.2.1`;
- Flow aqueceu duas definições;
- recursos `flow`, `flow-langchain4j`, `flow-messaging`, `langchain4j-ollama` e
  `messaging` instalados;
- nenhuma advertência de canal desconectado;
- nenhuma dependência ou tentativa de iniciar Kafka.

## Dependency tree

Versões relevantes:

```text
io.quarkiverse.flow:quarkus-flow:0.10.2
io.quarkiverse.flow:quarkus-flow-langchain4j:0.10.2
io.quarkiverse.flow:quarkus-flow-messaging:0.10.2
io.quarkiverse.langchain4j:quarkus-langchain4j-agentic:1.11.2
io.quarkiverse.langchain4j:quarkus-langchain4j-ollama:1.11.2
dev.langchain4j:langchain4j:1.16.2
dev.langchain4j:langchain4j-agentic:1.16.2-beta26
dev.langchain4j:langchain4j-ollama:1.16.2
```

Não houve conflito de LangChain4j. O único `omitted for conflict` foi
`net.thisptr:jackson-jq:1.6.1`, resolvido pelo BOM para `1.6.2`.

## Recomendação para C1

Manter:

```text
Quarkus 3.33.2.1
Java 25
Flow 0.10.2
Quarkus LangChain4j 1.11.2
```

Usar os BOMs de extensão explícitos, declarar `quarkus-flow-messaging` diretamente e
criar no adaptador de messaging um shim CDI mínimo, limitado a essa versão, que
registra `FlowMessagingConsumer` e `FlowDomainEventsPublisher`. O shim deve:

- permanecer fora de domínio/aplicação;
- ter teste de bootstrap/topologia sem connector;
- ter teste de correlação e ack;
- ser documentado no ADR como limitação da PoC;
- ser removido quando uma versão futura oferecer ponte interna oficial sem connector.

Alternativa mais desacoplada, porém maior, é implementar os SPIs
`EventConsumer`/`EventPublisher` documentados pelo Flow. Kafka, connector ou upgrade
do Quarkus continuam fora do escopo.

## Decisão pendente

C1 deve aprovar ou rejeitar:

1. os BOMs explícitos Flow `0.10.2` e Quarkus LangChain4j `1.11.2`;
2. a dependência direta `quarkus-flow-messaging`;
3. o shim CDI interno sem connector;
4. a permanência em Quarkus `3.33.2.1` e Java 25.

## Spike durável da Task 7.1 — 2026-07-26

### Isolamento

O profile Maven `spike-persistencia-duravel` usa
`src/spike-test/java` como source set exclusivo de testes. As dependências
`quarkus-flow-redis` e `quarkus-flow-durable-kubernetes` só existem quando o
profile é ativado. O build padrão continuou sem esses recursos instalados e
`mvn -q test` terminou com código 0.

O spike não alterou `src/main`, configuração de produção, endpoint, DTO, JSON,
OpenAPI, ADR ou consolidado arquitetural.

### Grafo resolvido

```text
io.quarkiverse.flow:quarkus-flow-redis:0.10.2
\- io.quarkus:quarkus-redis-client:3.33.2.1

io.quarkiverse.flow:quarkus-flow-durable-kubernetes:0.10.2
\- io.quarkus:quarkus-kubernetes-client:3.33.2.1

io.serverlessworkflow:serverlessworkflow-persistence-tests:7.22.2.Final:test
org.testcontainers:testcontainers:2.0.4:test
```

### Contratos oficiais confirmados

- [Flow persistence 0.10.2](https://github.com/quarkiverse/quarkus-flow/blob/0.10.2/docs/modules/ROOT/pages/persistence.adoc):
  um provider por aplicação, extensão Redis e restauração associada à identidade
  da aplicação;
- [Flow messaging 0.10.2](https://github.com/quarkiverse/quarkus-flow/blob/0.10.2/docs/modules/ROOT/pages/messaging.adoc):
  um bean CDI `EventConsumer`, zero ou mais `EventPublisher` e substituição da
  ponte default por beans próprios;
- [Flow Durable Kubernetes 0.10.2](https://github.com/quarkiverse/quarkus-flow/blob/0.10.2/docs/modules/ROOT/pages/concepts-durable-workflow-k8s.adoc):
  Lease de membro como identidade da `WorkflowApplication`, pool de réplicas,
  RBAC e cuidados de rollout;
- [CouchDB `_changes`](https://docs.couchdb.org/en/stable/api/database/changes.html):
  cursor `since`, feeds repetíveis e necessidade de consumidor idempotente.

As assinaturas efetivas dos JARs confirmaram:

```text
EventConsumer.listen(EventFilter, WorkflowApplication)
EventPublisher.publish(CloudEvent) -> CompletableFuture<Void>
WorkflowApplication.Builder.withId(String)
MemberLeaseCoordinator.awaitLease(Duration)
```

### Evidências executadas

- RED do mapper: falha de compilação por `CouchDbChangeEventMapper` ausente;
- GREEN unitário: replay da mesma mudança gera o mesmo CloudEvent sem `data`;
- CouchDB `3.5.2`: documento real consultado duas vezes desde `since=0`;
- Valkey `7.2-alpine`: contrato oficial de writer/reader/scan/restauração;
- Durable Kubernetes: `LeaseStartupEvent`, espera de 30 segundos e
  `Builder.withId(leaseName)` verificados sem cluster;
- suíte opt-in: 6 testes, 0 falhas e 0 erros;
- suíte padrão: código 0;
- Sonar: `COMPLIANT`, 0 issues novas, cobertura 86,2%, duplicação 3,1%.

### Limites da evidência

O contrato Redis comprovou gravação e restauração pelos handlers no mesmo processo;
não comprovou restart da aplicação. O teste Kubernetes comprovou o vínculo de
identidade, mas não Lease real, readiness, duas réplicas, roteamento cross-pod ou
failover. Nenhum fallback foi implementado.

O bootstrap do profile exibiu duas limitações adicionais:

- sem o producer OpenTelemetry in-memory da suíte padrão, tentou exportar para
  `localhost:4317`, que estava indisponível;
- Quarkus advertiu que `quarkus.flow.persistence.auto-restore` não foi reconhecida,
  apesar de `FlowPersistenceConfig` expor o prefixo
  `quarkus.flow.persistence` e o método `autoRestore()` no JAR `0.10.2`.

Essas ocorrências foram registradas como risco de integração; não justificam mudar
produção, adicionar fallback, antecipar a Task 7.2 ou aceitar o ADR-0010.

## Spike de atualização da Task 7.6 — 2026-08-05

O checkpoint C9 autorizou avaliar Flow `0.13.0` com Quarkus LangChain4j `1.12.0`,
mantendo Quarkus `3.33.2.1` e Java 25. A combinação candidata resolveu e compilou:

```text
io.quarkiverse.flow:*                         0.13.0
io.serverlessworkflow:*                       7.25.1.Final
io.cloudevents:cloudevents-core               4.1.1
io.quarkiverse.langchain4j:*                  1.12.0
```

O bootstrap Quarkus, porém, falhou antes dos testes por ambiguidade CDI na injeção de
`AgenteAnaliseConformidade`: a combinação registrou simultaneamente o bean de classe
`AgenteAnaliseConformidade$$QuarkusImpl` e um bean sintético com o mesmo tipo e
qualificador `@Default`. Resolver essa divergência exigiria mudar o desenho agentic
ou a integração Flow–LangChain4j, sem relação direta e comprovada com o warning de
auto-restore.

Conforme o rollback previsto na Task 7.6, o POM voltou a Flow `0.10.2` e Quarkus
LangChain4j `1.11.2`; a compilação de retorno terminou com código 0. A versão
candidata não foi retida.

## Prova de restart entre JVMs da Task 7.6 — 2026-08-05

A prova opt-in executou duas invocações Maven, portanto duas JVMs distintas, contra
os mesmos containers efêmeros CouchDB e Valkey. A primeira JVM iniciou uma análise,
persistiu a projeção documental e encerrou com a instância Flow em `WAITING`. A
segunda JVM restaurou essa instância, correlacionou o CloudEvent da revisão pelo
`flowinstanceid` e concluiu o fluxo em `COMPLETED`, com origem
`REVISAO_HUMANA`.

O warning de configuração desconhecida para
`quarkus.flow.persistence.auto-restore` foi emitido nas duas inicializações. Mesmo
assim, a restauração automática padrão funcionou sem configurar a propriedade nem
desabilitar a validação global. O warning permanece como incompatibilidade de
metadados/configuração conhecida em Flow `0.10.2`, não como falha funcional de
restore no cenário comprovado.

O `ValkeyQuarkusTestResource` permanece global na suíte normal, porque a persistência
Flow precisa de Redis ativo em todos os contextos Quarkus. Quando
`restart.proof.enabled=true`, o recurso não inicia um container próprio nem
sobrescreve o endpoint Valkey externo compartilhado pelas duas JVMs. O script
removeu seus containers ao terminar. A evidência cobre restart sequencial de uma
réplica e não cobre Lease, duas réplicas simultâneas, roteamento cross-pod ou
failover, que permanecem nas Tasks 8.2/8.3.
