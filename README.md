# arvore-documento

Microsserviço Quarkus usado como proxy/adapter para a árvore de documento do MTR.

## Endpoint implementado

### API exposta pelo `arvore-documento`

```http
GET /arvore-documento/v1/processo/identificador-negocial/{identificador}
```

### API consumida no MTR

```http
GET /simtr-parametrizacao/v2/patriarca/processo/identificador-negocial/{identificador}
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
```

## Organização de pacotes

```text
br.gov.caixa.arvoredocumento
├── api
│   ├── dto
│   │   ├── erro
│   │   └── parametrizacao.processo
│   ├── exception
│   └── parametrizacao
├── application
│   └── parametrizacao
├── domain
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
```

Em ambiente real, configurar por variável de ambiente:

```bash
export QUARKUS_REST_CLIENT_PARAMETRIZACAO_PROCESSO_URL=https://simtr-parametrizacao-des.apps.nprd.caixa
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
GET /arvore-documento/openapi
```

## Chamada de exemplo

```bash
curl -X GET \
  'http://localhost:8080/arvore-documento/v1/processo/identificador-negocial/202114235' \
  -H 'Accept: application/json'
```


## Fault Tolerance e simulador

A chamada para o MTR Parametrização é feita pelo `ParametrizacaoProcessoClient` com:

- `@Timeout` de 2 segundos;
- `@Retry` com 3 retentativas para erro 5xx, timeout e falha de comunicação;
- `@CircuitBreaker` para abertura do circuito quando houver falhas recorrentes na integração;
- `@ClientExceptionMapper` para converter erro 4xx em `MtrClientErrorException` e erro 5xx em `MtrServerErrorException`.

Erros 4xx não entram em retry nem contam como falha do circuito, porque representam erro funcional ou de contrato.

O simulador é controlado por propriedade:

```properties
arvore-documento.simulador.parametrizacao-processo.habilitado=false
%dev.arvore-documento.simulador.parametrizacao-processo.habilitado=true
```

Quando habilitado, o `ProcessoService` não chama o REST Client do MTR e retorna um `ProcessoDto` mockado pelo `ProcessoMockFactory`.

## Observabilidade

O projeto inclui observabilidade no fluxo do endpoint de processo:

1. `ProcessoResource` registra o recebimento da requisição e o retorno da resposta.
2. `ProcessoService` registra a execução do caso de uso e a decisão entre simulador e MTR real.
3. `ParametrizacaoProcessoGateway` registra a chamada externa ao `simtr-parametrizacao`.
4. O `ParametrizacaoProcessoClient` mantém `@Timeout`, `@Retry` e `@CircuitBreaker`.

Spans explícitos criados:

- `arvore-documento.api.processo.consultar`
- `arvore-documento.service.processo.consultar`
- `mtr.parametrizacao.processo.consultar`

Logs estruturados JSON ficam habilitados no console com MDC achatado, incluindo campos como:

- `evento`
- `traceId`
- `spanId`
- `camada`
- `componente`
- `operacao`
- `identificador_negocial`
- `resultado`
- `erro_tipo`

Configurações principais:

```properties
quarkus.otel.exporter.otlp.endpoint=http://localhost:4317
quarkus.otel.traces.sampler=always_on
quarkus.otel.traces.sampler.arg=1.0
%dev.quarkus.otel.traces.exporter=logging

quarkus.log.console.json.enabled=true
quarkus.log.console.json.mdc.flat-fields=true
quarkus.log.console.json.exception-output-type=formatted
```
