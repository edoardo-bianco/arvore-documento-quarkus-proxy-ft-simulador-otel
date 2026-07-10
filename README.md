# arvore-documento

Microsservico Quarkus usado como proxy/adapter entre consumidores internos e APIs MTR relacionadas a arvore documental, parametrizacao, dossie produto e gestao de documentos.

O projeto nao faz apenas repasse HTTP. Ele cria uma fronteira com DTOs, VOs, mappers, service, gateway, tratamento de erro, resiliencia, simulador e observabilidade.

## Endpoints implementados

### API exposta pelo `arvore-documento`

```http
GET /arvore-documento/v1/processo/identificador-negocial/{identificador}
GET /arvore-documento/v1/checklist/identificador-negocial/{identificador}/versao/{versao}
POST /arvore-documento/v1/dossie-produto
PATCH /arvore-documento/v1/dossie-produto/{id}/formulario
POST /arvore-documento/v1/dossie-produto/{id}/documento
PATCH /arvore-documento/v1/dossie-produto/{id}/validacao-negocial
POST /arvore-documento/v1/dossie-produto/{id}/workflow
POST /arvore-documento/v1/storage/container/credencial
```

### APIs consumidas no MTR

```http
GET /simtr/parametrizacao/v2/patriarca/processo/identificador-negocial/{identificador}
GET /simtr/parametrizacao/v1/cadastro/checklist/identificador-negocial/{identificador}/versao/{versao}
POST /simtr/dossie-produto/v1/dossie-produto
PATCH /simtr/dossie-produto/v1/dossie-produto/{id}/formulario
POST /simtr/dossie-produto/v2/dossie-produto/{id}/documento
PATCH /simtr/dossie-produto/v1/dossie-produto/{id}/validacao-negocial
POST /simtr/dossie-produto/v1/dossie-produto/{id}/workflow
POST /simtr/gestao-documento/v1/storage/container/credencial
```

Os endpoints de dossie produto implementam criacao basica, formulario, inclusao de documento, validacao negocial e workflow. Gestao de Documentos implementa a geracao de credencial SAS de container para upload documental.

Criacao basica e formulario retornam HTTP `201` com o id do dossie:

```json
{
  "id": 1
}
```

## Decisao arquitetural

Fluxo padrao:

```text
Resource
  -> Service
      -> se simulador=true
          -> MockFactory
      -> se simulador=false
          -> Gateway
              -> REST Client MTR
      -> Mapper.toVo(...)
  -> Mapper.toDto(...)
  -> DTO de resposta
```

Fluxos implementados:

```text
ProcessoResource
  -> ProcessoService
  -> ParametrizacaoProcessoGateway ou ProcessoMockFactory
  -> ProcessoMapper

ChecklistResource
  -> ChecklistService
  -> ParametrizacaoChecklistGateway ou ChecklistMockFactory
  -> ChecklistMapper

DossieProdutoResource
  -> DossieProdutoService
  -> DossieProdutoGateway ou DossieProdutoMockFactory
  -> DossieProdutoMapper

GestaoDocumentoResource
  -> GestaoDocumentoService
  -> GestaoDocumentoGateway ou GestaoDocumentoMockFactory
  -> GestaoDocumentoMapper
```

O ciclo `DTO -> VO -> DTO` e intencional: o VO cria uma fronteira para regras futuras, enriquecimento e adaptacao de contrato sem acoplar a API externa diretamente ao contrato MTR.

## Organizacao de pacotes

```text
br.gov.caixa.simtr.arvoredocumento
|-- api
|   |-- dossieproduto
|   |-- dto
|   |   |-- dossieproduto
|   |   |-- erro
|   |   |-- gestaodocumento
|   |   |-- parametrizacao.checklist
|   |   `-- parametrizacao.processo
|   |-- exception
|   |-- gestaodocumento
|   `-- parametrizacao
|-- application
|   |-- dossieproduto
|   |-- gestaodocumento
|   `-- parametrizacao
|-- domain
|   |-- dossieproduto
|   |-- gestaodocumento
|   |-- parametrizacao.checklist
|   `-- parametrizacao.processo
|-- infrastructure
|   `-- client
|       |-- dossieproduto
|       |-- gestaodocumento
|       |-- mock
|       `-- parametrizacao
|-- mapper
|   |-- dossieproduto
|   |-- gestaodocumento
|   `-- parametrizacao
`-- shared
    |-- exception
    `-- observability
```

Utilitarios compartilhados:

- `ClientErrorBodyReader`: leitura e normalizacao do corpo de erro retornado pelo MTR.
- `MarkdownJsonMockReader`: leitura de mocks Markdown com secao `## dados do mock corpo do retorno json`.
- `RestClientObservabilityFilter`: log e atributos de trace para chamadas REST Client.
- `RequestHeaderFactory`: propagacao do header `apikey`.

## Configuracao

As URLs atuais usam `https://api.des.caixa:8443/simtr` como base:

```properties
quarkus.rest-client.parametrizacao-processo.url=https://api.des.caixa:8443/simtr
quarkus.rest-client.parametrizacao-processo.connect-timeout=3000
quarkus.rest-client.parametrizacao-processo.read-timeout=10000

quarkus.rest-client.parametrizacao-checklist.url=https://api.des.caixa:8443/simtr
quarkus.rest-client.parametrizacao-checklist.connect-timeout=3000
quarkus.rest-client.parametrizacao-checklist.read-timeout=10000

quarkus.rest-client.dossie-produto.url=https://api.des.caixa:8443/simtr
quarkus.rest-client.dossie-produto.connect-timeout=3000
quarkus.rest-client.dossie-produto.read-timeout=10000

quarkus.rest-client.gestao-documento.url=https://api.des.caixa:8443/simtr
quarkus.rest-client.gestao-documento.connect-timeout=3000
quarkus.rest-client.gestao-documento.read-timeout=10000
```

Em ambiente real, as URLs podem ser sobrescritas por variaveis de ambiente:

```bash
export QUARKUS_REST_CLIENT_PARAMETRIZACAO_PROCESSO_URL=https://api.des.caixa:8443/simtr
export QUARKUS_REST_CLIENT_PARAMETRIZACAO_CHECKLIST_URL=https://api.des.caixa:8443/simtr
export QUARKUS_REST_CLIENT_DOSSIE_PRODUTO_URL=https://api.des.caixa:8443/simtr
export QUARKUS_REST_CLIENT_GESTAO_DOCUMENTO_URL=https://api.des.caixa:8443/simtr
```

Credenciais e apikey:

```properties
simtr.apikey=${SIMTR_API_KEY}
%dev.quarkus.oidc.credentials.secret=${SIMTR_OIDC_CLIENT_SECRET}
%dev.quarkus.oidc.internet.credentials.secret=${SIMTR_OIDC_INTERNET_CLIENT_SECRET}
```

## Simulador e mocks

Propriedades atuais:

```properties
arvore-documento.simulador.parametrizacao-processo.habilitado=false
%dev.arvore-documento.simulador.parametrizacao-processo.habilitado=true
arvore-documento.simulador.parametrizacao-checklist.habilitado=false
%dev.arvore-documento.simulador.parametrizacao-checklist.habilitado=true
arvore-documento.simulador.dossie-produto.habilitado=false
%dev.arvore-documento.simulador.dossie-produto.habilitado=true
arvore-documento.simulador.gestao-documento.habilitado=false
%dev.arvore-documento.simulador.gestao-documento.habilitado=true
```

Com isso, processo, checklist, dossie produto e gestao de documentos usam mock por padrao em dev mode. Para chamar o MTR real de dossie produto em dev mode, desabilite explicitamente o simulador:

```bash
mvn quarkus:dev -Ddebug=false "-Darvore-documento.simulador.dossie-produto.habilitado=false"
```

Para chamar o MTR real de Gestao de Documentos em dev mode:

```bash
mvn quarkus:dev -Ddebug=false "-Darvore-documento.simulador.gestao-documento.habilitado=false"
```

Mocks em runtime:

```text
src/main/resources/mock/parametrizacao/1000016487-consulta-processo-parametrizacao-v2-identificador-negocial.md
src/main/resources/mock/parametrizacao/1000009990-consulta-processo-parametrizacao-v2-identificador-negocial.md
src/main/resources/mock/parametrizacao/1000012583-v1-checklist-parametrizacao-versao-1.md
src/main/resources/mock/dossieproduto/criacao-basica-dossie-produto.md
src/main/resources/mock/dossieproduto/formulario-dossie-produto.md
src/main/resources/mock/dossieproduto/documento-dossie-produto.md
src/main/resources/mock/dossieproduto/validacao-negocial-dossie-produto.md
src/main/resources/mock/dossieproduto/workflow-dossie-produto.md
src/main/resources/mock/gestaodocumento/credencial-container.md
```

Copias documentais:

```text
doc/mock/parametrizacao
doc/mock/dossie-produto/criacao-basica-dossie-produto.md
doc/mock/dossie-produto/formulario-dossie-produto.md
doc/mock/dossie-produto/documento-dossie-produto.md
doc/mock/dossie-produto/validacao-negocial-dossie-produto.md
doc/mock/dossie-produto/workflow-dossie-produto.md
doc/mock/gestao-documento/credencial-container.md
```

## Execucao local

```bash
mvn quarkus:dev -Ddebug=false
```

Swagger UI:

```http
GET /arvore-documento/doc
```

OpenAPI:

```http
GET /arvore-documento/openapi
```

## Chamadas de exemplo

```bash
curl -X GET \
  'http://localhost:8080/arvore-documento/v1/processo/identificador-negocial/202114235' \
  -H 'Accept: application/json'

curl -X GET \
  'http://localhost:8080/arvore-documento/v1/checklist/identificador-negocial/1000012583/versao/1' \
  -H 'Accept: application/json'

curl -X POST \
  'http://localhost:8080/arvore-documento/v1/dossie-produto' \
  -H 'Accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "processo": 0,
    "chave_correlacao_canal": 0,
    "numero_negocio": 0,
    "clientes": [
      {
        "cpf": "string",
        "cnpj": "string",
        "tipo_vinculo": 0,
        "cliente_relacionado": {
          "cpf": "string",
          "cnpj": "string"
        },
        "sequencia_titularidade": 0
      }
    ]
  }'

curl -X POST \
  'http://localhost:8080/arvore-documento/v1/storage/container/credencial' \
  -H 'Accept: application/json'
```

## Tratamento de erro

1. O REST Client chama o MTR.
2. Para respostas HTTP `4xx` ou `5xx`, o metodo anotado com `@ClientExceptionMapper` le o corpo de erro no padrao MTR.
3. O client lanca `MtrRestClientException` contendo status HTTP e `ErroPadraoDto`.
4. O `MtrRestClientExceptionMapper` da API REST do `arvore-documento` transforma a excecao novamente no objeto de erro padrao.
5. Se o MTR retornar erro fora do contrato esperado, a API gera um erro padronizado `ARVDOCP0002`.

Erros de negocio (`400`, `404`, `409`, `422`) nao entram em retry nem contam como falha do circuito. Demais `4xx` sao tratados como erro tecnico de cliente. Erros `5xx`, timeout e falha de comunicacao sao tratados como falhas potencialmente transitorias.

## Fault tolerance

Os REST Clients usam:

- `@Timeout` de 2 segundos;
- `@Retry` com 3 retentativas para erro 5xx, timeout e falha de comunicacao;
- `@CircuitBreaker` para abertura do circuito quando houver falhas recorrentes;
- `@ClientExceptionMapper` para classificar erro de negocio, erro tecnico de cliente e erro tecnico de servidor.

## Observabilidade

O projeto registra logs estruturados JSON no console e em `target/logs/arvore-documento.json`.

Spans explicitos:

- `arvore-documento.api.processo.consultar`
- `arvore-documento.service.processo.consultar`
- `mtr.parametrizacao.processo.consultar`
- `arvore-documento.api.checklist.consultar`
- `arvore-documento.service.checklist.consultar`
- `mtr.parametrizacao.checklist.consultar`
- `arvore-documento.api.dossie-produto.criar`
- `arvore-documento.service.dossie-produto.criar`
- `mtr.dossie-produto.criar`
- `arvore-documento.api.dossie-produto.formulario.atualizar`
- `arvore-documento.service.dossie-produto.formulario.atualizar`
- `mtr.dossie-produto.formulario.atualizar`
- `arvore-documento.api.dossie-produto.documento.incluir`
- `arvore-documento.service.dossie-produto.documento.incluir`
- `mtr.dossie-produto.documento.incluir`
- `arvore-documento.api.dossie-produto.validacao-negocial.registrar`
- `arvore-documento.service.dossie-produto.validacao-negocial.registrar`
- `mtr.dossie-produto.validacao-negocial.registrar`
- `arvore-documento.api.dossie-produto.workflow.avancar`
- `arvore-documento.service.dossie-produto.workflow.avancar`
- `mtr.dossie-produto.workflow.avancar`
- `arvore-documento.api.gestao-documento.credencial-container.gerar`
- `arvore-documento.service.gestao-documento.credencial-container.gerar`
- `mtr.gestao-documento.credencial-container.gerar`

Campos comuns nos logs:

- `evento`
- `traceId`
- `spanId`
- `camada`
- `componente`
- `operacao`
- `identificador_negocial`
- `processo`
- `chave_correlacao_canal`
- `dossie_produto_id`
- `nome_container`
- `resultado`
- `erro_tipo`
- `url`
- `status_http`
- `request_body`
- `request_body_truncado`
- `response_body`
- `response_body_truncado`

Payloads de REST Client sao mascarados antes do log para campos sensiveis como `sas`, `token`, `client_secret`, `apikey` e `password`.

Por padrao, o projeto nao exporta OpenTelemetry para fora. Isso evita erro ou ruido em maquinas sem Docker, Jaeger ou OpenTelemetry Collector.

Configuracao padrao:

```properties
arvore-documento.observabilidade.rest-client.payload.habilitado=true
arvore-documento.observabilidade.rest-client.payload.input.max-length=2000
arvore-documento.observabilidade.rest-client.payload.output.max-length=4000

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

Para enviar traces e logs OpenTelemetry ao Jaeger, suba Jaeger/OTLP em `localhost:4317` e rode com o profile opcional:

```bash
mvn quarkus:dev -Ddebug=false "-Dquarkus.profile=dev,jaeger"
```

Para usar Grafana/Tempo/Loki via OpenTelemetry Collector em `localhost:4317`:

```bash
mvn quarkus:dev -Ddebug=false "-Dquarkus.profile=dev,grafana"
```

## Documentacao complementar

Detalhes completos de arquitetura, mock, observabilidade e troubleshooting ficam em:

```text
doc/documentacao-simtr-hub-arquitetura-observabilidade.md
```
