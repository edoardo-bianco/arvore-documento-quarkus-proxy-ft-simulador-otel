# arvore-documento - Proxy Quarkus com observabilidade

## Indice

- [Objetivo](#objetivo)
- [Cenario de uso](#cenario-de-uso)
- [Contrato exposto](#contrato-exposto)
- [Arquitetura](#arquitetura)
- [Pacotes](#pacotes)
- [Configuracao atual](#configuracao-atual)
- [Execucao local](#execucao-local)
- [Tratamento de erro](#tratamento-de-erro)
- [Resiliencia](#resiliencia)
- [Observabilidade do projeto](#observabilidade-do-projeto)
- [Jaeger](#jaeger)
- [Grafana](#grafana)
- [Como correlacionar logs e traces](#como-correlacionar-logs-e-traces)
- [Troubleshooting](#troubleshooting)
- [Proximos passos](#proximos-passos)

## Objetivo

O `arvore-documento` e um microsservico Quarkus criado para atuar como proxy/adapter entre consumidores internos e o servico MTR de parametrizacao.

O projeto nao faz apenas repasse HTTP. Ele cria uma fronteira controlada para:

- expor um contrato proprio do `arvore-documento`;
- encapsular o endpoint interno do `simtr-parametrizacao`;
- padronizar erros;
- aplicar timeout, retry e circuit breaker;
- permitir desenvolvimento local com simulador;
- registrar logs JSON estruturados;
- exportar traces e logs por OpenTelemetry;
- visualizar a execucao em Jaeger ou, como alternativa mais completa, Grafana.

Tecnologias principais:

- Quarkus `3.33.2.1`;
- Java `17`;
- `quarkus-rest-jackson`;
- `quarkus-rest-client-jackson`;
- `quarkus-smallrye-openapi`;
- `quarkus-smallrye-health`;
- `quarkus-smallrye-fault-tolerance`;
- `quarkus-opentelemetry`;
- `quarkus-logging-json`.

## Cenario de uso

O consumidor precisa consultar a arvore de documentos vinculada a um processo negocial.

Sem este proxy, o consumidor chamaria diretamente o MTR:

```http
GET /simtr-parametrizacao/v2/patriarca/processo/identificador-negocial/{identificador}
```

Com o `arvore-documento`, o consumidor passa a chamar uma API de dominio:

```http
GET /arvore-documento/v1/processo/identificador-negocial/{identificador}
```

Beneficios praticos:

- o endpoint do MTR fica isolado;
- o contrato externo pode evoluir sem expor detalhes do MTR;
- erros do MTR sao convertidos para o padrao esperado pelo `arvore-documento`;
- falhas transitorias recebem retry controlado;
- falhas recorrentes podem abrir circuit breaker;
- chamadas locais podem usar mock em dev mode;
- logs e traces permitem reconstruir a execucao completa.

## Contrato exposto

### API de negocio

```http
GET http://localhost:8080/arvore-documento/v1/processo/identificador-negocial/1
Accept: application/json
```

Parametro:

```java
@Min(value = 1, message = "O identificador negocial deve ser maior que zero.")
```

### Swagger UI

```http
GET http://localhost:8080/arvore-documento/doc/
```

### OpenAPI

O projeto esta configurado com o path abaixo:

```http
GET http://localhost:8080/arvore-documento/openai
```

Observacao: o nome do path esta como `openai`, mas o conteudo retornado e o documento OpenAPI gerado pelo Quarkus. Se o objetivo for usar o nome convencional, ajuste para `/arvore-documento/openapi` no `application.properties`.

### Health check

```http
GET http://localhost:8080/q/health
GET http://localhost:8080/q/health/live
GET http://localhost:8080/q/health/ready
GET http://localhost:8080/q/health/started
```

### Dev UI

Em dev mode, a raiz `/` redireciona para o Dev UI.

```http
GET http://localhost:8080/
GET http://localhost:8080/q/dev-ui/
```

## Arquitetura

Fluxo principal:

```text
ProcessoResource
  -> ProcessoService
      -> se simulador=true
          -> ProcessoMockFactory
      -> se simulador=false
          -> ParametrizacaoProcessoGateway
              -> ParametrizacaoProcessoClient
                  -> simtr-parametrizacao
      -> ProcessoMapper.toVo(...)
  -> ProcessoMapper.toDto(...)
  -> ProcessoDto
```

O fluxo `DTO -> VO -> DTO` e proposital. Mesmo que o contrato atual seja parecido com o retorno do MTR, o VO cria uma fronteira para regras futuras, enriquecimento e adaptacao de contrato.

### Fluxo com simulador

```text
HTTP GET /arvore-documento/v1/processo/identificador-negocial/{id}
  -> ProcessoResource
  -> ProcessoService
  -> ProcessoMockFactory
  -> ProcessoMapper
  -> resposta mockada
```

### Fluxo com MTR real

```text
HTTP GET /arvore-documento/v1/processo/identificador-negocial/{id}
  -> ProcessoResource
  -> ProcessoService
  -> ParametrizacaoProcessoGateway
  -> ParametrizacaoProcessoClient
  -> GET /simtr-parametrizacao/v2/patriarca/processo/identificador-negocial/{id}
  -> ProcessoMapper
  -> resposta do arvore-documento
```

## Pacotes

```text
br.gov.caixa.arvoredocumento
|-- api
|   |-- dev
|   |-- dto
|   |   |-- erro
|   |   `-- parametrizacao.processo
|   |-- exception
|   `-- parametrizacao
|-- application
|   `-- parametrizacao
|-- domain
|   `-- parametrizacao.processo
|-- infrastructure
|   `-- client.parametrizacao
|-- mapper
|   `-- parametrizacao
`-- shared
    |-- exception
    `-- observability
```

| Pacote | Responsabilidade |
|---|---|
| `api.parametrizacao` | Endpoint REST exposto pelo `arvore-documento` |
| `api.dev` | Redirect de `/` para Dev UI em dev mode |
| `api.dto` | Contratos REST de sucesso e erro |
| `api.exception` | Conversao de excecoes para resposta HTTP |
| `application.parametrizacao` | Caso de uso e escolha entre simulador e MTR |
| `domain.parametrizacao.processo` | Modelo interno em VOs |
| `infrastructure.client.parametrizacao` | Gateway e REST Client do MTR |
| `mapper.parametrizacao` | Conversao DTO/VO |
| `shared.observability` | Logs estruturados com contexto de trace |

## Configuracao atual

Arquivo:

```text
src/main/resources/application.properties
```

Conteudo funcional relevante:

```properties
quarkus.application.name=arvore-documento
quarkus.http.port=8080

quarkus.smallrye-openapi.path=/arvore-documento/openai
quarkus.swagger-ui.always-include=true
quarkus.swagger-ui.path=/arvore-documento/doc
%dev.quarkus.smallrye-openapi.path=/arvore-documento/openai
%dev.quarkus.swagger-ui.path=/arvore-documento/doc

quarkus.rest-client.parametrizacao-processo.url=http://localhost:8081
quarkus.rest-client.parametrizacao-processo.connect-timeout=3000
quarkus.rest-client.parametrizacao-processo.read-timeout=10000

arvore-documento.simulador.parametrizacao-processo.habilitado=false
%dev.arvore-documento.simulador.parametrizacao-processo.habilitado=true
```

Configuracao de observabilidade atual:

```properties
quarkus.otel.exporter.otlp.endpoint=http://localhost:4317
quarkus.otel.traces.sampler=always_on
quarkus.otel.traces.sampler.arg=1.0
quarkus.otel.logs.enabled=true
quarkus.otel.logs.handler.enabled=true
quarkus.otel.logs.exporter=cdi

quarkus.log.console.json.enabled=true
quarkus.log.console.json.pretty-print=false
quarkus.log.console.json.exception-output-type=formatted
quarkus.log.console.json.mdc.flat-fields=true
quarkus.log.console.json.additional-field."service.name".value=${quarkus.application.name}
quarkus.log.console.json.additional-field."service.name".type=string

quarkus.log.file.enabled=true
quarkus.log.file.path=target/logs/arvore-documento.json
quarkus.log.file.json.enabled=true
quarkus.log.file.json.pretty-print=false
quarkus.log.file.json.exception-output-type=formatted
quarkus.log.file.json.mdc.flat-fields=true
quarkus.log.file.json.additional-field."service.name".value=${quarkus.application.name}
quarkus.log.file.json.additional-field."service.name".type=string
```

Pontos importantes:

- `quarkus.otel.exporter.otlp.endpoint=http://localhost:4317` envia sinais OTLP para um backend local.
- `quarkus.otel.logs.enabled=true` habilita logs como sinal OpenTelemetry.
- `quarkus.otel.logs.handler.enabled=true` conecta o logging do Quarkus ao OpenTelemetry.
- `quarkus.otel.logs.exporter=cdi` usa o exporter OTLP padrao fornecido pelo Quarkus.
- `target/logs/arvore-documento.json` recebe uma copia local dos logs JSON.
- Jaeger e bom para traces; para consultar logs em UI, use Grafana/Loki ou outro backend de logs.

Em ambiente real, configure a URL do MTR por variavel de ambiente:

```bash
export QUARKUS_REST_CLIENT_PARAMETRIZACAO_PROCESSO_URL=https://simtr-parametrizacao-des.apps.nprd.caixa
```

## Execucao local

### Compilar

```powershell
mvn -q -DskipTests compile
```

### Rodar em dev mode

```powershell
mvn quarkus:dev -Ddebug=false
```

### Chamar a API

```powershell
Invoke-WebRequest `
  -Uri "http://localhost:8080/arvore-documento/v1/processo/identificador-negocial/1" `
  -Headers @{ Accept = "application/json" }
```

### Conferir log JSON local

```powershell
Get-Content -Tail 20 target/logs/arvore-documento.json
```

## Tratamento de erro

O REST Client usa `@ClientExceptionMapper` para converter respostas HTTP de erro do MTR em excecoes controladas.

Fluxo:

```text
MTR retorna 4xx/5xx
  -> ParametrizacaoProcessoClient.toException(...)
  -> ClientErrorBodyReader
  -> MtrClientErrorException ou MtrServerErrorException
  -> MtrRestClientExceptionMapper
  -> ErroPadraoDto retornado ao chamador
```

| Condicao | Comportamento |
|---|---|
| `4xx` do MTR | `MtrClientErrorException`; sem retry; nao conta como falha do circuito |
| `5xx` do MTR | `MtrServerErrorException`; com retry; pode abrir circuito |
| Timeout | Com retry; pode abrir circuito |
| Falha de comunicacao | Com retry; pode abrir circuito |
| Erro inesperado | Convertido para erro padronizado |

## Resiliencia

A resiliencia fica no REST Client, que e a fronteira com a dependencia externa.

```java
@Timeout(value = 2_000, unit = ChronoUnit.MILLIS)
@Retry(maxRetries = 3)
@CircuitBreaker(
        requestVolumeThreshold = 10,
        failureRatio = 0.5,
        delay = 10_000,
        successThreshold = 2
)
```

Erros `4xx` abortam retry e sao ignorados pelo circuit breaker, porque representam erro funcional ou de contrato. Erros `5xx`, timeout e falha de comunicacao sao tratados como falha potencialmente transitoria.

## Observabilidade do projeto

A observabilidade atual tem quatro saidas:

1. Traces OpenTelemetry exportados por OTLP.
2. Logs OpenTelemetry exportados por OTLP.
3. Logs JSON no console.
4. Logs JSON em arquivo local.

### Spans criados

| Camada | Span |
|---|---|
| API | `arvore-documento.api.processo.consultar` |
| Aplicacao | `arvore-documento.service.processo.consultar` |
| Integracao MTR | `mtr.parametrizacao.processo.consultar` |

O span `mtr.parametrizacao.processo.consultar` aparece quando o simulador esta desabilitado.

### Campos de log

`ObservabilityLog` coloca no MDC os campos funcionais e tecnicos da execucao:

```text
traceId
spanId
traceSampled
evento
camada
componente
operacao
identificador_negocial
resultado
erro_tipo
processo_nome
```

Exemplo de linha JSON:

```json
{
  "message": "arvore-documento.processo.requisicao.recebida",
  "traceId": "b45622a44293cf8847972f2553cad319",
  "spanId": "f4204827a0a232af",
  "evento": "arvore-documento.processo.requisicao.recebida",
  "camada": "api",
  "componente": "ProcessoResource",
  "identificador_negocial": "2",
  "service.name": "arvore-documento"
}
```

## Jaeger

Jaeger e a opcao principal ja validada neste projeto para visualizar traces.

Use Jaeger quando o objetivo for entender:

- quais spans foram criados;
- duracao da chamada;
- hierarquia API -> Service -> Gateway -> REST Client;
- erros registrados no trace;
- atributos como `identificador_negocial`, `origem_dados` e `processo.nome`.

Importante: Jaeger nao e a melhor ferramenta para consultar logs. Mesmo com `quarkus.otel.logs.enabled=true`, a UI do Jaeger e focada em traces. Os logs ficam disponiveis no console, no arquivo JSON e podem ser enviados por OTLP para outro backend que aceite logs.

### Subir Jaeger

```powershell
docker run -d --name jaeger `
  -e COLLECTOR_OTLP_ENABLED=true `
  -p 16686:16686 `
  -p 4317:4317 `
  -p 4318:4318 `
  jaegertracing/all-in-one:latest
```

Portas:

| Porta | Uso |
|---|---|
| `16686` | UI do Jaeger |
| `4317` | OTLP gRPC |
| `4318` | OTLP HTTP |

### Rodar a aplicacao

```powershell
mvn quarkus:dev -Ddebug=false
```

### Gerar trace

```powershell
Invoke-WebRequest `
  -Uri "http://localhost:8080/arvore-documento/v1/processo/identificador-negocial/1" `
  -Headers @{ Accept = "application/json" }
```

### Consultar pela UI

Abra:

```text
http://localhost:16686
```

Na tela:

1. Em `Service`, selecione `arvore-documento`.
2. Clique em `Find Traces`.
3. Abra o trace mais recente.

### Consultar pela API do Jaeger

Listar servicos:

```powershell
Invoke-WebRequest http://localhost:16686/api/services
```

Resultado esperado:

```json
{
  "data": [
    "jaeger-all-in-one",
    "arvore-documento"
  ]
}
```

Listar traces:

```powershell
Invoke-WebRequest "http://localhost:16686/api/traces?service=arvore-documento&limit=20"
```

### O que deve aparecer no Jaeger

Com simulador habilitado:

```text
HTTP GET /arvore-documento/v1/processo/identificador-negocial/{id}
  -> arvore-documento.api.processo.consultar
    -> arvore-documento.service.processo.consultar
```

Com simulador desabilitado:

```text
HTTP GET /arvore-documento/v1/processo/identificador-negocial/{id}
  -> arvore-documento.api.processo.consultar
    -> arvore-documento.service.processo.consultar
      -> mtr.parametrizacao.processo.consultar
        -> GET /simtr-parametrizacao/v2/patriarca/processo/identificador-negocial/{id}
```

## Grafana

Grafana e a alternativa recomendada quando voce quer uma visao mais completa do sistema, principalmente logs e traces juntos.

Use Grafana quando o objetivo for:

- consultar traces no Tempo;
- consultar logs no Loki;
- correlacionar `traceId` entre trace e log;
- montar dashboards;
- observar mais de um servico;
- evoluir para metricas.

### Opcao recomendada: Grafana LGTM via Quarkus Dev Services

Adicionar a dependencia:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-observability-devservices-lgtm</artifactId>
    <scope>provided</scope>
</dependency>
```

Para usar Dev Services LGTM, evite manter o endpoint fixo do Jaeger em dev:

```properties
# quarkus.otel.exporter.otlp.endpoint=http://localhost:4317
```

Motivo: o Dev Service sobe seu proprio stack e configura o endpoint OTLP automaticamente. Se a aplicacao continuar apontando para `localhost:4317`, ela vai enviar para o Jaeger, nao para o collector do stack LGTM.

Executar:

```powershell
mvn quarkus:dev
```

Abrir Dev UI:

```text
http://localhost:8080/q/dev-ui/
```

O Dev UI deve mostrar os servicos de observabilidade iniciados e o link do Grafana.

### Opcao manual: Grafana + Tempo + Loki + OpenTelemetry Collector

Nesse modelo, a aplicacao continua usando OTLP:

```properties
quarkus.otel.exporter.otlp.endpoint=http://localhost:4317
quarkus.otel.logs.enabled=true
quarkus.otel.logs.handler.enabled=true
quarkus.otel.logs.exporter=cdi
```

O `localhost:4317` deve ser um OpenTelemetry Collector, nao o Jaeger.

Fluxo recomendado:

```text
arvore-documento
  -> OTLP gRPC :4317
  -> OpenTelemetry Collector
      -> traces para Tempo
      -> logs para Loki
  -> Grafana
      -> datasource Tempo
      -> datasource Loki
```

No Grafana:

1. Abra a datasource `Tempo` para consultar traces.
2. Filtre pelo service `arvore-documento`.
3. Abra o trace desejado.
4. Copie o `traceId`.
5. Abra a datasource `Loki`.
6. Procure logs com o mesmo `traceId`.

Consulta LogQL tipica, dependendo dos labels configurados no Loki:

```logql
{service_name="arvore-documento"} |= "traceId"
```

Se os logs forem indexados com `traceId` como label:

```logql
{service_name="arvore-documento", traceId="b45622a44293cf8847972f2553cad319"}
```

## Como correlacionar logs e traces

1. Gere uma chamada:

```powershell
Invoke-WebRequest `
  -Uri "http://localhost:8080/arvore-documento/v1/processo/identificador-negocial/2" `
  -Headers @{ Accept = "application/json" }
```

2. Abra Jaeger ou Grafana Tempo e localize o trace.

3. Copie o `traceId`.

4. Procure no arquivo local:

```powershell
Select-String `
  -Path target/logs/arvore-documento.json `
  -Pattern "b45622a44293cf8847972f2553cad319"
```

5. Em Grafana/Loki, consulte logs pelo mesmo `traceId`.

Essa correlacao funciona porque `ObservabilityLog` copia o contexto do span atual para o MDC, e o JSON logger escreve o MDC no console e no arquivo.

## Troubleshooting

### `/q/swagger-ui/` retorna 404

O Swagger deste projeto nao esta no path padrao do Quarkus.

Use:

```text
http://localhost:8080/arvore-documento/doc/
```

### `/arvore-documento/openapi` retorna 404

O path configurado atualmente e:

```text
http://localhost:8080/arvore-documento/openai
```

Para trocar para o nome convencional:

```properties
quarkus.smallrye-openapi.path=/arvore-documento/openapi
%dev.quarkus.smallrye-openapi.path=/arvore-documento/openapi
```

### `/q/health/live` retorna 404

Confirme se existe no `pom.xml`:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-health</artifactId>
</dependency>
```

Depois reinicie o dev mode.

### Jaeger nao mostra `arvore-documento`

Verifique se o container esta rodando:

```powershell
docker ps --filter name=jaeger
```

Verifique se a porta OTLP esta livre e publicada:

```powershell
docker ps --filter name=jaeger --format "{{.Ports}}"
```

Gere uma chamada real:

```powershell
Invoke-WebRequest `
  -Uri "http://localhost:8080/arvore-documento/v1/processo/identificador-negocial/1" `
  -Headers @{ Accept = "application/json" }
```

Aguarde alguns segundos, porque o exporter envia dados em lote.

### Logs nao aparecem no Jaeger

Isso e esperado. Jaeger e uma ferramenta de traces. Para logs, use:

- console JSON;
- `target/logs/arvore-documento.json`;
- Grafana/Loki;
- outro backend compativel com logs OpenTelemetry.

### Erro `OpenTelemetry exporter set to 'otlp' but upstream dependencies not found`

Nao use:

```properties
%dev.quarkus.otel.traces.exporter=otlp
```

Nesta versao do Quarkus, o valor correto para o exporter padrao e `cdi`, que ja e o default. O projeto usa:

```properties
quarkus.otel.logs.exporter=cdi
```

E deixa os traces usarem o default do Quarkus.

### Arquivo de log nao foi criado

Confirme:

```properties
quarkus.log.file.enabled=true
quarkus.log.file.path=target/logs/arvore-documento.json
quarkus.log.file.json.enabled=true
```

Gere uma requisicao e leia:

```powershell
Get-Content -Tail 20 target/logs/arvore-documento.json
```

## Proximos passos

1. Decidir se o path OpenAPI deve continuar `/arvore-documento/openai` ou mudar para `/arvore-documento/openapi`.
2. Criar um `docker-compose-jaeger.yml` para padronizar a subida do Jaeger.
3. Criar um profile separado para Grafana LGTM, evitando conflito com Jaeger em `localhost:4317`.
4. Adicionar configuracao versionada de OpenTelemetry Collector para enviar traces ao Tempo e logs ao Loki.
5. Adicionar testes unitarios para `ProcessoService` cobrindo simulador e MTR real.
6. Adicionar stub/WireMock para respostas `200`, `404`, `500` e timeout do `simtr-parametrizacao`.
7. Criar dashboard Grafana com latencia, erro, volume e correlacao por `traceId`.
