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
