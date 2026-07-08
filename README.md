# arvore-documento

Microsserviço Quarkus usado como proxy/adapter para a árvore de documento do MTR.

## Endpoint implementado

### API exposta pelo `arvore-documento`

```http
GET /arvore-documento/v1/processo/identificador-negocial/{identificador}
GET /arvore-documento/v1/checklist/identificador-negocial/{identificador}/versao/{versao}
```

### API consumida no MTR

```http
GET /simtr-parametrizacao/v2/patriarca/processo/identificador-negocial/{identificador}
GET /simtr-parametrizacao/v1/cadastro/checklist/identificador-negocial/{identificador}/versao/{versao}
```

## Decisão arquitetural

Este projeto implementa um proxy com camada de serviço e mapeamento explícito, não um repasse direto de HTTP.

Fluxo:

```text
ProcessoResource
  -> ProcessoService
      -> ParametrizacaoProcessoClient
          -> ProcessoDto
      -> ProcessoMapper.toVo(...)
  -> ProcessoMapper.toDto(...)
  -> ProcessoDto

ChecklistResource
  -> ChecklistService
      -> ParametrizacaoChecklistClient
          -> ChecklistDto
      -> ChecklistMapper.toVo(...)
  -> ChecklistMapper.toDto(...)
  -> ChecklistDto
```

## Organização de pacotes

```text
br.gov.caixa.simtr.arvoredocumento
├── api
│   ├── dto
│   │   ├── erro
│   │   ├── parametrizacao.checklist
│   │   └── parametrizacao.processo
│   ├── exception
│   └── parametrizacao
├── application
│   └── parametrizacao
├── domain
│   ├── parametrizacao.checklist
│   └── parametrizacao.processo
├── infrastructure
│   └── client.parametrizacao
├── mapper
│   └── parametrizacao
└── shared
    └── exception
```

## Tratamento de erro

1. O REST Client chama o `simtr-parametrizacao`.
2. Para respostas HTTP `4xx` ou `5xx`, o método anotado com `@ClientExceptionMapper` lê o corpo de erro no padrão do MTR.
3. O client lança `MtrRestClientException` contendo status HTTP e `ErroPadraoDto`.
4. O `MtrRestClientExceptionMapper` da API REST do `arvore-documento` transforma a exceção novamente no objeto de erro padrão.
5. Se o MTR retornar erro fora do contrato esperado, a API gera um erro padronizado `ARVDOCP0002`.

## Configuração

```properties
quarkus.rest-client.parametrizacao-processo.url=http://localhost:8081
quarkus.rest-client.parametrizacao-processo.connect-timeout=3000
quarkus.rest-client.parametrizacao-processo.read-timeout=10000

quarkus.rest-client.parametrizacao-checklist.url=http://localhost:8081
quarkus.rest-client.parametrizacao-checklist.connect-timeout=3000
quarkus.rest-client.parametrizacao-checklist.read-timeout=10000
```

Em ambiente real, configurar por variável de ambiente:

```bash
export QUARKUS_REST_CLIENT_PARAMETRIZACAO_PROCESSO_URL=https://simtr-parametrizacao-des.apps.nprd.caixa
export QUARKUS_REST_CLIENT_PARAMETRIZACAO_CHECKLIST_URL=https://simtr-parametrizacao-des.apps.nprd.caixa
```

## Execução local

```bash
mvn quarkus:dev
```

Swagger UI:

```http
GET /arvore-documento/doc
```

OpenAPI:

```http
GET /arvore-documento/openai
```

## Chamada de exemplo

```bash
curl -X GET \
  'http://localhost:8080/arvore-documento/v1/processo/identificador-negocial/202114235' \
  -H 'Accept: application/json'

curl -X GET \
  'http://localhost:8080/arvore-documento/v1/checklist/identificador-negocial/1000012583/versao/1' \
  -H 'Accept: application/json'
```


## Fault Tolerance e simulador

A chamada para o MTR Parametrização é feita pelos REST Clients de parametrização (`ParametrizacaoProcessoClient` e `ParametrizacaoChecklistClient`) com:

- `@Timeout` de 2 segundos;
- `@Retry` com 3 retentativas para erro 5xx, timeout e falha de comunicação;
- `@CircuitBreaker` para abertura do circuito quando houver falhas recorrentes na integração;
- `@ClientExceptionMapper` para converter erro 4xx em `MtrClientErrorException` e erro 5xx em `MtrServerErrorException`.

Erros 4xx não entram em retry nem contam como falha do circuito, porque representam erro funcional ou de contrato.

O simulador é controlado por propriedade:

```properties
arvore-documento.simulador.parametrizacao-processo.habilitado=false
%dev.arvore-documento.simulador.parametrizacao-processo.habilitado=true
arvore-documento.simulador.parametrizacao-checklist.habilitado=false
%dev.arvore-documento.simulador.parametrizacao-checklist.habilitado=true
```

Quando habilitado, o service correspondente não chama o REST Client do MTR e retorna o DTO mockado pelo factory do domínio (`ProcessoMockFactory` ou `ChecklistMockFactory`).

## Observabilidade

O projeto inclui observabilidade no fluxo do endpoint de processo:

1. `ProcessoResource` registra o recebimento da requisição e o retorno da resposta.
2. `ProcessoService` registra a execução do caso de uso e a decisão entre simulador e MTR real.
3. `ParametrizacaoProcessoGateway` registra a chamada externa ao `simtr-parametrizacao`.
4. Os REST Clients mantêm `@Timeout`, `@Retry` e `@CircuitBreaker`.

O endpoint de checklist segue o mesmo padrão com `ChecklistResource`, `ChecklistService`, `ParametrizacaoChecklistGateway` e `ParametrizacaoChecklistClient`.

Spans explícitos criados:

- `arvore-documento.api.processo.consultar`
- `arvore-documento.service.processo.consultar`
- `mtr.parametrizacao.processo.consultar`
- `arvore-documento.api.checklist.consultar`
- `arvore-documento.service.checklist.consultar`
- `mtr.parametrizacao.checklist.consultar`

Logs estruturados JSON ficam habilitados no console e no arquivo `target/logs/arvore-documento.json` com MDC achatado, incluindo campos como:

- `evento`
- `traceId`
- `spanId`
- `camada`
- `componente`
- `operacao`
- `identificador_negocial`
- `resultado`
- `erro_tipo`

Por padrão, o projeto não exporta OpenTelemetry para fora. Isso evita erro ou ruído em máquinas sem Docker, Jaeger ou OpenTelemetry Collector. Nesse modo, a observabilidade fica disponível nos logs JSON locais.

Configuração padrão:

```properties
quarkus.otel.traces.sampler=always_on
quarkus.otel.traces.sampler.arg=1.0
quarkus.otel.traces.exporter=none
quarkus.otel.logs.enabled=false
quarkus.otel.logs.handler.enabled=false
quarkus.otel.logs.exporter=none

quarkus.log.console.json.enabled=true
quarkus.log.console.json.mdc.flat-fields=true
quarkus.log.console.json.exception-output-type=formatted
quarkus.log.file.enabled=true
quarkus.log.file.path=target/logs/arvore-documento.json
quarkus.log.file.json.enabled=true
```

Para enviar traces e logs OpenTelemetry ao Jaeger, suba o Jaeger/OTLP em `localhost:4317` e rode com o profile opcional:

```bash
mvn quarkus:dev -Ddebug=false "-Dquarkus.profile=dev,jaeger"
```
