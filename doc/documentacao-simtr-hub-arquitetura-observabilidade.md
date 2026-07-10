# SIMTR Hub - Arquitetura e observabilidade

## Indice

- [Objetivo](#objetivo)
- [Cenario de uso](#cenario-de-uso)
- [Contrato exposto](#contrato-exposto)
- [Arquitetura](#arquitetura)
- [Pacotes](#pacotes)
- [Configuracao atual](#configuracao-atual)
- [Mock da parametrizacao](#mock-da-parametrizacao)
- [Mock do dossie produto](#mock-do-dossie-produto)
- [Como chamar o MTR real em dev mode](#como-chamar-o-mtr-real-em-dev-mode)
- [Perfis de observabilidade](#perfis-de-observabilidade)
- [Execucao local](#execucao-local)
- [Tratamento de erro](#tratamento-de-erro)
- [Resiliencia](#resiliencia)
- [Logs de REST Client](#logs-de-rest-client)
- [Observabilidade do projeto](#observabilidade-do-projeto)
- [Jaeger](#jaeger)
- [Grafana](#grafana)
- [Como correlacionar logs e traces](#como-correlacionar-logs-e-traces)
- [Troubleshooting](#troubleshooting)
- [Proximos passos](#proximos-passos)

## Objetivo

O `SIMTR Hub` e um microsservico Quarkus criado para atuar como proxy/adapter entre consumidores internos e servicos MTR, iniciando por parametrizacao e dossie produto.

O projeto nao faz apenas repasse HTTP. Ele cria uma fronteira controlada para:

- expor um contrato proprio do `SIMTR Hub`;
- encapsular o endpoint interno do `simtr-parametrizacao`;
- padronizar erros;
- aplicar timeout, retry e circuit breaker;
- permitir desenvolvimento local com simulador;
- registrar logs JSON estruturados;
- exportar traces e logs por OpenTelemetry quando um backend for ativado;
- visualizar a execucao em Jaeger ou, como alternativa mais completa, Grafana.

### Nomenclatura

O nome funcional da solucao e `SIMTR Hub`.

Alguns exemplos, paths, propriedades, span names, arquivos de log e packages ainda usam o identificador tecnico legado `arvore-documento`, porque a aplicacao ainda nao passou pela migracao completa de nomenclatura no codigo. Esses identificadores devem ser tratados como legado tecnico temporario.

A migracao futura deve substituir o root tecnico `br.gov.caixa.simtr.arvoredocumento` por um root coerente com `simtr-hub`, em momento proprio e com testes de regressao completos.

Tecnologias principais:

- Quarkus `3.33.2.1`;
- Java `25`, conforme `maven.compiler.release` configurado no `pom.xml`;
- `quarkus-rest-jackson`;
- `quarkus-rest-client-jackson`;
- `quarkus-smallrye-openapi`;
- `quarkus-smallrye-health`;
- `quarkus-smallrye-fault-tolerance`;
- `quarkus-opentelemetry`;
- `quarkus-logging-json`.

## Cenario de uso

O consumidor precisa consultar documentos e parametros MTR vinculados a um processo negocial.

Sem este proxy, o consumidor chamaria diretamente o MTR:

```http
GET /simtr-parametrizacao/v2/patriarca/processo/identificador-negocial/{identificador}
```

Com o `SIMTR Hub`, o consumidor passa a chamar uma API de dominio:

```http
GET /arvore-documento/v1/processo/identificador-negocial/{identificador}
```

Beneficios praticos:

- o endpoint do MTR fica isolado;
- o contrato externo pode evoluir sem expor detalhes do MTR;
- erros do MTR sao convertidos para o padrao esperado pelo `SIMTR Hub`;
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

```http
GET http://localhost:8080/arvore-documento/v1/checklist/identificador-negocial/1000012583/versao/1
Accept: application/json
```

```http
POST http://localhost:8080/arvore-documento/v1/dossie-produto
Content-Type: application/json
Accept: application/json
```

Corpo para criacao basica de dossie produto em modo rascunho:

```json
{
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
}
```

Para inclusao de documento, `objeto` em `atributos` e `propriedades` e opcional no contrato aceito pelo SIMTR Hub. O Hub valida `id` do path e corpo informado, mas nao deve rejeitar documento apenas pela ausencia desse campo.

Resposta de sucesso:

```http
HTTP/1.1 201 Created
```

```json
{
  "id": 0
}
```

```http
PATCH http://localhost:8080/arvore-documento/v1/dossie-produto/{id}/formulario
Content-Type: application/json
Accept: application/json
```

Corpo para inclusao ou edicao de respostas de formulario no dossie produto:

```json
[
  {
    "vinculo_dossie": {
      "fase": 0,
      "cliente": {
        "cpf": "string",
        "cnpj": "string",
        "tipo_vinculo": 0
      },
      "produto": {
        "codigo_operacao": 0,
        "codigo_modalidade": 0
      },
      "garantia": {
        "codigo_bacen": 0,
        "produto_operacao": 0,
        "produto_modalidade": 0,
        "clientes_avalistas": [
          {
            "cpf": "string",
            "cnpj": "string"
          }
        ]
      },
      "respostas_formulario": [
        {
          "campo_formulario": 0,
          "resposta": "string",
          "opcoes_selecionadas": [
            "string"
          ],
          "excluir": true
        }
      ]
    }
  }
]
```

Resposta de sucesso:

```http
HTTP/1.1 201 Created
```

```json
{
  "id": 0
}
```

```http
POST http://localhost:8080/arvore-documento/v1/dossie-produto/{id}/documento
Content-Type: application/json
Accept: application/json
```

Corpo para inclusao de documento no dossie produto:

```json
{
  "id": 0,
  "path_storage": "{{nome_container}}/{{nome_arquivo}}.{{extensao}}",
  "codigo_ged": "string",
  "object_store_ged": "string",
  "tipo_documento": "string",
  "vinculo_dossie": {
    "cliente": {
      "cpf": "string",
      "cnpj": "string",
      "tipo_vinculo": 0
    },
    "elemento_conteudo": 0,
    "garantia": {
      "codigo_bacen": 0,
      "produto_operacao": 0,
      "produto_modalidade": 0,
      "cliente_avalista": [
        {
          "cpf": "string",
          "cnpj": "string"
        }
      ]
    }
  },
  "atributos": [
    {
      "chave": "string",
      "valor": "string",
      "objeto": "string",
      "opcoes_selecionadas": [
        "string"
      ]
    }
  ],
  "propriedades": [
    {
      "chave": "string",
      "valor": "string",
      "objeto": "string"
    }
  ]
}
```

Resposta de sucesso:

```http
HTTP/1.1 201 Created
```

```json
{
  "id_documento": 0,
  "id_instancia_documento": 0
}
```

```http
PATCH http://localhost:8080/arvore-documento/v1/dossie-produto/{id}/validacao-negocial
Content-Type: application/json
Accept: application/json
```

Corpo para registro de validacao negocial no dossie produto, seguindo o OpenAPI implementado no MTR:

```json
{
  "verificacoes": [
    {
      "identificador_instancia_documento": 0,
      "identificador_checklist": 0,
      "versao_checklist": 0,
      "analise_realizada": true,
      "parecer_apontamentos": [
        {
          "identificador_apontamento": 0,
          "resultado": "APROVADO",
          "comentario": "string",
          "necessidade_reanalise": true,
          "indice_ia": 0.99
        }
      ],
      "garantia": {
        "codigo_bacen": 0,
        "clientes_avalistas": [
          {
            "cpf": "string",
            "cnpj": "string"
          }
        ]
      },
      "produto": {
        "codigo_operacao": 0,
        "codigo_modalidade": 0
      },
      "previo": true
    }
  ],
  "respostas_formulario": [
    {
      "campo_formulario": 29848450,
      "resposta": "string",
      "opcoes_selecionadas": [
        "string"
      ]
    }
  ]
}
```

Observacao sobre `previo`: o campo e aceito quando informado, mas nao e obrigatorio na validacao local do Hub. Payloads reais da integracao de pre-validacao podem omitir esse indicador; nesse caso o Hub deve encaminhar a requisicao ao MTR sem bloquear com `400`.

Resposta de sucesso:

```http
HTTP/1.1 200 OK
```

Sem corpo de resposta.

```http
POST http://localhost:8080/arvore-documento/v1/dossie-produto/{id}/workflow
Accept: application/json
```

Esse endpoint nao recebe corpo de requisicao e aceita chamada sem `Content-Type`.

Resposta de sucesso:

```http
HTTP/1.1 200 OK
```

```json
{
  "id": 0
}
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
GET http://localhost:8080/arvore-documento/openapi
```

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

ChecklistResource
  -> ChecklistService
      -> se simulador=true
          -> ChecklistMockFactory
      -> se simulador=false
          -> ParametrizacaoChecklistGateway
              -> ParametrizacaoChecklistClient
                  -> simtr-parametrizacao
      -> ChecklistMapper.toVo(...)
  -> ChecklistMapper.toDto(...)
  -> ChecklistDto

DossieProdutoResource
  -> DossieProdutoService
      -> se simulador=true
          -> DossieProdutoMockFactory
      -> se simulador=false
          -> DossieProdutoGateway
              -> DossieProdutoClient
                  -> simtr-dossie-produto
  -> DossieProdutoMapper.toVo(...)
  -> DossieProdutoMapper.toDto(...)
  -> DossieProdutoCriadoDto, DossieProdutoDocumentoCriadoDto ou resposta sem corpo
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

HTTP GET /arvore-documento/v1/checklist/identificador-negocial/{id}/versao/{versao}
  -> ChecklistResource
  -> ChecklistService
  -> ChecklistMockFactory
  -> ChecklistMapper
  -> resposta mockada

HTTP POST /arvore-documento/v1/dossie-produto
  -> DossieProdutoResource
  -> DossieProdutoService
  -> DossieProdutoMockFactory
  -> DossieProdutoMapper
  -> resposta mockada com id do dossie produto

HTTP PATCH /arvore-documento/v1/dossie-produto/{id}/formulario
  -> DossieProdutoResource
  -> DossieProdutoService
  -> DossieProdutoMockFactory
  -> DossieProdutoMapper
  -> resposta mockada com id do dossie produto

HTTP POST /arvore-documento/v1/dossie-produto/{id}/documento
  -> DossieProdutoResource
  -> DossieProdutoService
  -> DossieProdutoMockFactory
  -> DossieProdutoMapper
  -> resposta mockada com id_documento e id_instancia_documento

HTTP PATCH /arvore-documento/v1/dossie-produto/{id}/validacao-negocial
  -> DossieProdutoResource
  -> DossieProdutoService
  -> DossieProdutoMockFactory
  -> DossieProdutoMapper
  -> resposta 200 OK sem corpo

HTTP POST /arvore-documento/v1/dossie-produto/{id}/workflow
  -> DossieProdutoResource
  -> DossieProdutoService
  -> DossieProdutoMockFactory
  -> DossieProdutoMapper
  -> resposta mockada com id do dossie produto
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
  -> resposta do SIMTR Hub

HTTP GET /arvore-documento/v1/checklist/identificador-negocial/{id}/versao/{versao}
  -> ChecklistResource
  -> ChecklistService
  -> ParametrizacaoChecklistGateway
  -> ParametrizacaoChecklistClient
  -> GET /simtr-parametrizacao/v1/cadastro/checklist/identificador-negocial/{id}/versao/{versao}
  -> ChecklistMapper
  -> resposta do SIMTR Hub

HTTP POST /arvore-documento/v1/dossie-produto
  -> DossieProdutoResource
  -> DossieProdutoService
  -> DossieProdutoGateway
  -> DossieProdutoClient
  -> POST /simtr/dossie-produto/v1/dossie-produto
  -> DossieProdutoMapper
  -> resposta do SIMTR Hub com HTTP 201

HTTP PATCH /arvore-documento/v1/dossie-produto/{id}/formulario
  -> DossieProdutoResource
  -> DossieProdutoService
  -> DossieProdutoGateway
  -> DossieProdutoClient
  -> PATCH /simtr/dossie-produto/v1/dossie-produto/{id}/formulario
  -> DossieProdutoMapper
  -> resposta do SIMTR Hub com HTTP 201

HTTP POST /arvore-documento/v1/dossie-produto/{id}/documento
  -> DossieProdutoResource
  -> DossieProdutoService
  -> DossieProdutoGateway
  -> DossieProdutoClient
  -> POST /simtr/dossie-produto/v2/dossie-produto/{id}/documento
  -> DossieProdutoMapper
  -> resposta do SIMTR Hub com HTTP 201

HTTP PATCH /arvore-documento/v1/dossie-produto/{id}/validacao-negocial
  -> DossieProdutoResource
  -> DossieProdutoService
  -> DossieProdutoGateway
  -> DossieProdutoClient
  -> PATCH /simtr/dossie-produto/v1/dossie-produto/{id}/validacao-negocial
  -> resposta do SIMTR Hub com HTTP 200 sem corpo

HTTP POST /arvore-documento/v1/dossie-produto/{id}/workflow
  -> DossieProdutoResource
  -> DossieProdutoService
  -> DossieProdutoGateway
  -> DossieProdutoClient
  -> POST /simtr/dossie-produto/v1/dossie-produto/{id}/workflow
  -> DossieProdutoMapper
  -> resposta do SIMTR Hub com HTTP 200
```

## Pacotes

```text
br.gov.caixa.simtr.arvoredocumento
|-- api
|   |-- dev
|   |-- dto
|   |   |-- erro
|   |   |-- dossieproduto
|   |   |-- parametrizacao.checklist
|   |   `-- parametrizacao.processo
|   |-- exception
|   |-- dossieproduto
|   `-- parametrizacao
|-- application
|   |-- dossieproduto
|   `-- parametrizacao
|-- domain
|   |-- dossieproduto
|   |-- parametrizacao.checklist
|   `-- parametrizacao.processo
|-- infrastructure
|   `-- client
|       |-- dossieproduto
|       |-- mock
|       `-- parametrizacao
|-- mapper
|   |-- dossieproduto
|   `-- parametrizacao
`-- shared
    |-- exception
    `-- observability
```

| Pacote | Responsabilidade |
|---|---|
| `api.parametrizacao` | Endpoint REST exposto pelo `SIMTR Hub` |
| `api.dossieproduto` | Endpoint REST de negocio para dossies produto |
| `api.dev` | Redirect de `/` para Dev UI em dev mode |
| `api.dto` | Contratos REST de sucesso e erro |
| `api.exception` | Conversao de excecoes para resposta HTTP |
| `application.parametrizacao` | Caso de uso e escolha entre simulador e MTR |
| `application.dossieproduto` | Caso de uso de dossie produto e escolha entre simulador e MTR |
| `domain.parametrizacao.processo` | Modelo interno em VOs |
| `domain.dossieproduto` | Modelo interno em VOs para dossie produto |
| `infrastructure.client.parametrizacao` | Gateway e REST Client do MTR |
| `infrastructure.client.dossieproduto` | Gateway e REST Client do `simtr-dossie-produto` |
| `infrastructure.client.mock` | Leitor comum de mocks em Markdown com corpo JSON |
| `mapper.parametrizacao` | Conversao DTO/VO |
| `mapper.dossieproduto` | Conversao DTO/VO de dossie produto |
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

quarkus.smallrye-openapi.path=/arvore-documento/openapi
quarkus.swagger-ui.always-include=true
quarkus.swagger-ui.path=/arvore-documento/doc
%dev.quarkus.smallrye-openapi.path=/arvore-documento/openapi
%dev.quarkus.swagger-ui.path=/arvore-documento/doc

quarkus.rest-client.parametrizacao-processo.url=https://api.des.caixa:8443/simtr
quarkus.rest-client.parametrizacao-processo.connect-timeout=3000
quarkus.rest-client.parametrizacao-processo.read-timeout=10000

quarkus.rest-client.parametrizacao-checklist.url=https://api.des.caixa:8443/simtr
quarkus.rest-client.parametrizacao-checklist.connect-timeout=3000
quarkus.rest-client.parametrizacao-checklist.read-timeout=10000

quarkus.rest-client.dossie-produto.url=https://api.des.caixa:8443/simtr
quarkus.rest-client.dossie-produto.connect-timeout=3000
quarkus.rest-client.dossie-produto.read-timeout=10000

arvore-documento.simulador.parametrizacao-processo.habilitado=false
%dev.arvore-documento.simulador.parametrizacao-processo.habilitado=true
arvore-documento.simulador.parametrizacao-checklist.habilitado=false
%dev.arvore-documento.simulador.parametrizacao-checklist.habilitado=true
arvore-documento.simulador.dossie-produto.habilitado=false
%dev.arvore-documento.simulador.dossie-produto.habilitado=true
```

O REST Client de dossie produto usa base versionavel `@Path("/dossie-produto")`. As versoes ficam nos metodos (`/v1/dossie-produto...` e `/v2/dossie-produto...`) para manter um unico client do servico e permitir endpoints v1 e v2 sem duplicar `/simtr`. A validacao negocial usa `PATCH /v1/dossie-produto/{id}/validacao-negocial` e retorna sucesso sem corpo. O workflow usa `POST /v1/dossie-produto/{id}/workflow` e nao recebe request body.

Configuracao de observabilidade atual:

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

%jaeger.quarkus.otel.exporter.otlp.endpoint=http://localhost:4317
%jaeger.quarkus.otel.traces.exporter=cdi
%jaeger.quarkus.otel.logs.enabled=true
%jaeger.quarkus.otel.logs.handler.enabled=true
%jaeger.quarkus.otel.logs.exporter=cdi

%grafana.quarkus.otel.exporter.otlp.endpoint=http://localhost:4317
%grafana.quarkus.otel.traces.exporter=cdi
%grafana.quarkus.otel.logs.enabled=true
%grafana.quarkus.otel.logs.handler.enabled=true
%grafana.quarkus.otel.logs.exporter=cdi

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

- O modo padrao nao tenta conectar em Jaeger, Grafana ou OpenTelemetry Collector.
- `quarkus.otel.traces.exporter=none` evita exportacao de traces quando nao ha backend local.
- `quarkus.otel.logs.enabled=false` e `quarkus.otel.logs.handler.enabled=false` evitam exportacao de logs OpenTelemetry quando nao ha backend local.
- Os logs JSON continuam sendo escritos no console e em `target/logs/arvore-documento.json`.
- O profile `jaeger` habilita a exportacao OTLP para Jaeger em `localhost:4317`.
- O profile `grafana` habilita a exportacao OTLP para um OpenTelemetry Collector em `localhost:4317`.
- `target/logs/arvore-documento.json` recebe uma copia local dos logs JSON.
- Jaeger e bom para traces; para consultar logs em UI, use Grafana/Loki ou outro backend de logs.

Em ambiente real, configure a URL do MTR por variavel de ambiente:

```bash
export QUARKUS_REST_CLIENT_PARAMETRIZACAO_PROCESSO_URL=https://api.des.caixa:8443/simtr
export QUARKUS_REST_CLIENT_PARAMETRIZACAO_CHECKLIST_URL=https://api.des.caixa:8443/simtr
export QUARKUS_REST_CLIENT_DOSSIE_PRODUTO_URL=https://api.des.caixa:8443/simtr
```

## Mock da parametrizacao

O projeto possui um simulador local para permitir desenvolvimento sem chamar o MTR Parametrizacao real.

O simulador e controlado pela propriedade:

```properties
arvore-documento.simulador.parametrizacao-processo.habilitado=false
%dev.arvore-documento.simulador.parametrizacao-processo.habilitado=true
arvore-documento.simulador.parametrizacao-checklist.habilitado=false
%dev.arvore-documento.simulador.parametrizacao-checklist.habilitado=true
```

Com isso:

- fora do dev mode, o padrao e chamar o REST Client real do MTR;
- em dev mode, o padrao e usar o mock local;
- quando o mock esta ativo, o fluxo nao chama o REST Client correspondente;
- o `ProcessoService` chama `ProcessoMockFactory`;
- o `ChecklistService` chama `ChecklistMockFactory`;
- o retorno mockado continua passando pelo mapper correspondente, mantendo o mesmo fluxo DTO -> VO -> DTO da API.

### Arquivos de mock

O `ProcessoMockFactory` le os dados de mock a partir de arquivos Markdown versionados em:

```text
src/main/resources/mock/parametrizacao
```

Os arquivos de processo usados pelo simulador atual sao:

```text
src/main/resources/mock/parametrizacao/1000016487-consulta-processo-parametrizacao-v2-identificador-negocial.md
src/main/resources/mock/parametrizacao/1000009990-consulta-processo-parametrizacao-v2-identificador-negocial.md
```

O arquivo de checklist usado pelo simulador atual e:

```text
src/main/resources/mock/parametrizacao/1000012583-v1-checklist-parametrizacao-versao-1.md
```

Cada arquivo documenta a chamada original e contem o corpo JSON retornado pelo MTR na secao:

```markdown
## dados do mock corpo do retorno json
```

O leitor comum `MarkdownJsonMockReader` extrai o primeiro objeto JSON `{ ... }` depois dessa secao e converte para o DTO esperado pelo factory (`ProcessoDto` ou `ChecklistDto`) usando Jackson.

### Convencao de nomes

O identificador deve ficar no inicio do nome para facilitar ordenacao, busca no IntelliJ/Explorer e comparacao entre cenarios de mock.

Para mock de processo, use:

```text
{identificador}-consulta-processo-parametrizacao-v2-identificador-negocial.md
```

Exemplo:

```text
1000016487-consulta-processo-parametrizacao-v2-identificador-negocial.md
```

Para mock de checklist, use:

```text
{identificador}-v{versao-api}-checklist-parametrizacao-versao-{versao-checklist}.md
```

Exemplo:

```text
1000012583-v1-checklist-parametrizacao-versao-1.md
```

Esse nome representa o endpoint:

```http
GET /simtr-parametrizacao/v1/cadastro/checklist/identificador-negocial/1000012583/versao/1
```

Para endpoints com mais parametros, coloque os parametros relevantes no inicio, antes da descricao do endpoint. Isso deixa mocks relacionados ao mesmo identificador agrupados na listagem de arquivos.

### Identificador e fallback

Quando a API e chamada com um identificador, o factory procura primeiro o arquivo correspondente ao identificador informado.

Exemplo:

```http
GET /arvore-documento/v1/processo/identificador-negocial/1000016487
```

Procura:

```text
mock/parametrizacao/1000016487-consulta-processo-parametrizacao-v2-identificador-negocial.md
```

Se nao existir um arquivo especifico para o identificador consultado, o simulador usa como fallback o arquivo `1000016487`.

Isso permite chamar outros ids em dev mode sem quebrar a execucao, mas o conteudo retornado sera o processo padrao do mock.

Para checklist, o factory procura:

```text
mock/parametrizacao/{identificador}-v1-checklist-parametrizacao-versao-{versao}.md
```

Se nao encontrar, usa o fallback:

```text
mock/parametrizacao/1000012583-v1-checklist-parametrizacao-versao-1.md
```

### Dados atuais do mock

O mock atual representa o processo:

| Campo | Valor |
|---|---|
| `identificador_negocial` | `1000016487` |
| `nome` | `Concessao Habitacional` |
| `indicador_produto_obrigatorio` | `false` |
| `relacionamentos` | `9` itens |
| `produtos` | `16` itens |
| `fases` | `3` itens |

O DTO/VO atual preserva os campos do contrato v2 usados nos mocks, incluindo `indicador_produto_obrigatorio` na raiz do processo e `permite_multiplo` nos objetos `tipo_documento` e em `funcao_documental.tipos_documento[]`.

No swagger `simtr-parametrizacao-openapi-2.12.2.4`, o atributo `indicador_multiplos` aparece em outras respostas de tipo documento por id. Para o endpoint `GET /simtr-parametrizacao/v2/patriarca/processo/identificador-negocial/{identificador}`, o atributo correspondente exposto nos tipos de documento aninhados e `permite_multiplo`.

O campo `checklist` dentro de `fases[]` segue o contrato de `ProcessoFaseDTO1`: pode vir `null`, como lista vazia `[]` ou como lista de objetos `ChecklistDTO`.

### Como adicionar outro mock

Para criar um novo cenario de mock de processo:

1. Crie um arquivo em `src/main/resources/mock/parametrizacao`.
2. Use o padrao de nome:

```text
{identificador}-consulta-processo-parametrizacao-v2-identificador-negocial.md
```

3. Inclua a secao:

```markdown
## dados do mock corpo do retorno json
```

4. Cole abaixo dela o JSON completo do retorno do MTR.
5. Rode a aplicacao em dev mode e chame o identificador novo.

Exemplo:

```powershell
mvn quarkus:dev -Ddebug=false
```

```http
GET http://localhost:8080/arvore-documento/v1/processo/identificador-negocial/{identificador}
```

Para criar um novo cenario de mock de checklist:

1. Crie um arquivo em `src/main/resources/mock/parametrizacao`.
2. Use o padrao:

```text
{identificador}-v{versao-api}-checklist-parametrizacao-versao-{versao-checklist}.md
```

3. Mantenha a chamada original no inicio do Markdown.
4. Inclua a secao `## dados do mock corpo do retorno json`.
5. Cole abaixo dela o JSON completo do retorno do checklist.

Exemplo ja versionado:

```text
1000012583-v1-checklist-parametrizacao-versao-1.md
```

## Mock do dossie produto

O simulador de dossie produto permite desenvolver os endpoints de criacao basica, inclusao ou edicao de respostas de formulario, inclusao de documento, validacao negocial e workflow sem chamar o MTR real.

O simulador e controlado pela propriedade:

```properties
arvore-documento.simulador.dossie-produto.habilitado=false
%dev.arvore-documento.simulador.dossie-produto.habilitado=true
```

Com isso:

- fora do dev mode, o padrao e chamar o REST Client real do `simtr-dossie-produto`;
- em dev mode, o padrao e usar o mock local;
- quando o mock esta ativo, o fluxo nao chama o REST Client de dossie produto;
- o `DossieProdutoService` chama `DossieProdutoMockFactory`;
- o retorno mockado continua passando pelo mapper correspondente, mantendo o fluxo DTO -> VO -> DTO da API.

### Arquivos de mock do dossie produto

Os arquivos usados em runtime ficam em:

```text
src/main/resources/mock/dossieproduto/criacao-basica-dossie-produto.md
src/main/resources/mock/dossieproduto/formulario-dossie-produto.md
src/main/resources/mock/dossieproduto/documento-dossie-produto.md
src/main/resources/mock/dossieproduto/validacao-negocial-dossie-produto.md
src/main/resources/mock/dossieproduto/workflow-dossie-produto.md
```

As copias documentais ficam em:

```text
doc/mock/dossie-produto/criacao-basica-dossie-produto.md
doc/mock/dossie-produto/formulario-dossie-produto.md
doc/mock/dossie-produto/documento-dossie-produto.md
doc/mock/dossie-produto/validacao-negocial-dossie-produto.md
doc/mock/dossie-produto/workflow-dossie-produto.md
```

Cada arquivo documenta a chamada original e contem o corpo JSON retornado pelo MTR na secao:

```markdown
## dados do mock corpo do retorno json
```

Resposta mockada atual para criacao, formulario e workflow:

```json
{
  "id": 1
}
```

Para validacao negocial, o mock runtime le o arquivo Markdown correspondente e usa `{}` apenas para representar sucesso sem corpo de resposta.

Resposta mockada atual para inclusao de documento:

```json
{
  "id_documento": 456,
  "id_instancia_documento": 789
}
```

Para adicionar outro cenario de mock de dossie produto, crie um novo arquivo Markdown em `src/main/resources/mock/dossieproduto`, mantenha a secao `## dados do mock corpo do retorno json` e coloque abaixo dela o JSON de resposta esperado.

## Como chamar o MTR real em dev mode

Se precisar testar a integracao real mesmo em dev mode, desabilite o simulador:

```powershell
mvn quarkus:dev -Ddebug=false "-Darvore-documento.simulador.parametrizacao-processo.habilitado=false"
```

Para desabilitar apenas o simulador de checklist:

```powershell
mvn quarkus:dev -Ddebug=false "-Darvore-documento.simulador.parametrizacao-checklist.habilitado=false"
```

Para desabilitar apenas o simulador de dossie produto:

```powershell
mvn quarkus:dev -Ddebug=false "-Darvore-documento.simulador.dossie-produto.habilitado=false"
```

Ou configure por variavel de ambiente:

```powershell
$env:ARVORE_DOCUMENTO_SIMULADOR_PARAMETRIZACAO_PROCESSO_HABILITADO="false"
$env:ARVORE_DOCUMENTO_SIMULADOR_PARAMETRIZACAO_CHECKLIST_HABILITADO="false"
$env:ARVORE_DOCUMENTO_SIMULADOR_DOSSIE_PRODUTO_HABILITADO="false"
```

Nesse caso, configure tambem a URL real do MTR:

```powershell
$env:QUARKUS_REST_CLIENT_PARAMETRIZACAO_PROCESSO_URL="https://api.des.caixa:8443/simtr"
$env:QUARKUS_REST_CLIENT_PARAMETRIZACAO_CHECKLIST_URL="https://api.des.caixa:8443/simtr"
$env:QUARKUS_REST_CLIENT_DOSSIE_PRODUTO_URL="https://api.des.caixa:8443/simtr"
```

## Perfis de observabilidade

O projeto foi configurado para nao depender de Docker, Jaeger ou Grafana no modo padrao. Isso evita erro em maquina de trabalho sem backend de observabilidade local.

Os profiles sao:

| Cenario | Profile Quarkus | Exporta OpenTelemetry | Destino esperado | Quando usar |
|---|---|---|---|---|
| Sem Jaeger/Grafana | `dev` | Nao | Nenhum backend externo | Maquina sem Docker ou sem collector |
| Jaeger local | `dev,jaeger` | Sim | Jaeger/OTLP em `localhost:4317` | Desenvolvimento local com Jaeger |
| Jaeger remoto | `dev,jaeger` + variavel de endpoint | Sim | Jaeger/OTLP remoto | Maquina sem Docker usando servidor compartilhado |
| Grafana local | `dev,grafana` | Sim | OpenTelemetry Collector em `localhost:4317` | Stack Grafana/Tempo/Loki local |
| Grafana remoto | `dev,grafana` + variavel de endpoint | Sim | OpenTelemetry Collector remoto | Ambiente corporativo com Grafana centralizado |

Importante: o profile `grafana` nao aponta para a URL da UI do Grafana. A aplicacao envia dados para um OpenTelemetry Collector. O Collector encaminha traces para Tempo e logs para Loki. A UI do Grafana apenas consulta Tempo/Loki.

### Modo sem Jaeger/Grafana

Use quando nao houver Docker, Jaeger, Grafana, Tempo, Loki ou OpenTelemetry Collector disponivel.

```powershell
mvn quarkus:dev -Ddebug=false
```

Nesse modo:

- nao existe tentativa de conexao em `localhost:4317`;
- os logs JSON aparecem no console;
- os logs JSON tambem sao gravados em `target/logs/arvore-documento.json`;
- os campos `traceId` e `spanId` podem aparecer quando houver contexto de span local, mas nenhum trace e enviado para fora.

### Jaeger local

Use quando o Jaeger estiver rodando na propria maquina.

```powershell
mvn quarkus:dev -Ddebug=false "-Dquarkus.profile=dev,jaeger"
```

Endpoint usado pela aplicacao:

```text
http://localhost:4317
```

UI do Jaeger:

```text
http://localhost:16686
```

### Jaeger remoto

Use quando nao houver Docker local, mas existir um Jaeger ou collector OTLP em outro servidor.

No PowerShell:

```powershell
$env:QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT="http://servidor-jaeger:4317"
mvn quarkus:dev -Ddebug=false "-Dquarkus.profile=dev,jaeger"
```

No IntelliJ, configure:

| Campo | Valor |
|---|---|
| `Run` | `quarkus:dev -Ddebug=false "-Dquarkus.profile=dev,jaeger"` |
| `Profiles` | vazio |
| `Environment variables` | `QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT=http://servidor-jaeger:4317` |

Troque `servidor-jaeger` pelo host real. A porta `4317` precisa estar acessivel pela sua maquina via rede, VPN ou firewall. A UI do Jaeger, se estiver publicada, normalmente fica em:

```text
http://servidor-jaeger:16686
```

Se o endpoint remoto usar HTTPS, configure o endpoint com `https://`:

```powershell
$env:QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT="https://servidor-jaeger:4317"
```

Se o servidor remoto exigir header de autenticacao, configure tambem os headers OTLP conforme orientacao da equipe responsavel pelo collector:

```powershell
$env:QUARKUS_OTEL_EXPORTER_OTLP_HEADERS="Authorization=Bearer <token>"
```

### Grafana local

Use quando existir uma stack local com OpenTelemetry Collector, Tempo, Loki e Grafana. A aplicacao deve apontar para o Collector, nao para o Grafana.

```powershell
mvn quarkus:dev -Ddebug=false "-Dquarkus.profile=dev,grafana"
```

Endpoint usado pela aplicacao:

```text
http://localhost:4317
```

UI comum do Grafana, quando publicada localmente:

```text
http://localhost:3000
```

### Grafana remoto

Use quando a empresa disponibilizar um OpenTelemetry Collector remoto integrado ao Grafana, Tempo e Loki.

No PowerShell:

```powershell
$env:QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT="http://servidor-otel-collector:4317"
mvn quarkus:dev -Ddebug=false "-Dquarkus.profile=dev,grafana"
```

No IntelliJ, configure:

| Campo | Valor |
|---|---|
| `Run` | `quarkus:dev -Ddebug=false "-Dquarkus.profile=dev,grafana"` |
| `Profiles` | vazio |
| `Environment variables` | `QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT=http://servidor-otel-collector:4317` |

Troque `servidor-otel-collector` pelo host real do Collector. A URL da UI do Grafana e separada, por exemplo:

```text
http://servidor-grafana:3000
```

Se o Collector remoto exigir HTTPS:

```powershell
$env:QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT="https://servidor-otel-collector:4317"
```

Se o Collector remoto exigir token ou outro header:

```powershell
$env:QUARKUS_OTEL_EXPORTER_OTLP_HEADERS="Authorization=Bearer <token>"
```

### Configuracao no IntelliJ

Crie uma configuracao Maven em `Run > Edit Configurations`.

Para rodar sem backend externo:

| Campo | Valor |
|---|---|
| `Name` | `arvore-documento [dev]` |
| `Run` | `quarkus:dev -Ddebug=false` |
| `Working directory` | raiz do projeto, onde esta o `pom.xml` |
| `Profiles` | vazio |

Para rodar com Jaeger local:

| Campo | Valor |
|---|---|
| `Name` | `arvore-documento [dev jaeger]` |
| `Run` | `quarkus:dev -Ddebug=false "-Dquarkus.profile=dev,jaeger"` |
| `Working directory` | raiz do projeto, onde esta o `pom.xml` |
| `Profiles` | vazio |

Para rodar com Grafana/Collector local:

| Campo | Valor |
|---|---|
| `Name` | `arvore-documento [dev grafana]` |
| `Run` | `quarkus:dev -Ddebug=false "-Dquarkus.profile=dev,grafana"` |
| `Working directory` | raiz do projeto, onde esta o `pom.xml` |
| `Profiles` | vazio |

O campo `Profiles` do IntelliJ e para Maven profiles (`-P`). Ele nao deve ser usado para `dev,jaeger` ou `dev,grafana`, porque estes sao Quarkus profiles e ficam em `-Dquarkus.profile`.

## Execucao local

### Compilar

```powershell
mvn -q -DskipTests compile
```

### Rodar em dev mode sem Docker/Jaeger

Este e o modo recomendado para maquina de trabalho sem Docker. A aplicacao nao tenta exportar OpenTelemetry para `localhost:4317`; os logs ficam no console e em `target/logs/arvore-documento.json`.

```powershell
mvn quarkus:dev -Ddebug=false
```

### Rodar em dev mode com Jaeger

Use este modo somente quando houver um backend OTLP disponivel, por exemplo Jaeger rodando localmente com a porta `4317` publicada.

```powershell
mvn quarkus:dev -Ddebug=false "-Dquarkus.profile=dev,jaeger"
```

### Chamar a API

```powershell
Invoke-WebRequest `
  -Uri "http://localhost:8080/arvore-documento/v1/processo/identificador-negocial/1" `
  -Headers @{ Accept = "application/json" }

Invoke-WebRequest `
  -Uri "http://localhost:8080/arvore-documento/v1/checklist/identificador-negocial/1000012583/versao/1" `
  -Headers @{ Accept = "application/json" }

$body = @{
  processo = 0
  chave_correlacao_canal = 0
  numero_negocio = 0
  clientes = @(
    @{
      cpf = "string"
      cnpj = "string"
      tipo_vinculo = 0
      cliente_relacionado = @{
        cpf = "string"
        cnpj = "string"
      }
      sequencia_titularidade = 0
    }
  )
} | ConvertTo-Json -Depth 5

Invoke-WebRequest `
  -Uri "http://localhost:8080/arvore-documento/v1/dossie-produto" `
  -Method POST `
  -ContentType "application/json" `
  -Headers @{ Accept = "application/json" } `
  -Body $body

$formularioBody = @(
  @{
    vinculo_dossie = @{
      fase = 0
      cliente = @{
        cpf = "string"
        cnpj = "string"
        tipo_vinculo = 0
      }
      produto = @{
        codigo_operacao = 0
        codigo_modalidade = 0
      }
      garantia = @{
        codigo_bacen = 0
        produto_operacao = 0
        produto_modalidade = 0
        clientes_avalistas = @(
          @{
            cpf = "string"
            cnpj = "string"
          }
        )
      }
      respostas_formulario = @(
        @{
          campo_formulario = 0
          resposta = "string"
          opcoes_selecionadas = @("string")
          excluir = $true
        }
      )
    }
  }
) | ConvertTo-Json -Depth 8

Invoke-WebRequest `
  -Uri "http://localhost:8080/arvore-documento/v1/dossie-produto/1/formulario" `
  -Method PATCH `
  -ContentType "application/json" `
  -Headers @{ Accept = "application/json" } `
  -Body $formularioBody
```

### Conferir log JSON local

```powershell
Get-Content -Tail 20 target/logs/arvore-documento.json
```

### Testes, Mockito e JaCoCo

A suite usa `quarkus-junit-mockito`/Mockito e `quarkus-jacoco`.

Para evitar o auto-anexo dinamico do Mockito/Byte Buddy em JDKs recentes, o `pom.xml` configura:

- `maven-dependency-plugin` copiando `org.mockito:mockito-core` resolvido pelo Maven para `target/test-agents/mockito-core.jar`;
- `maven-surefire-plugin` iniciando a JVM de testes com `@{argLine} -javaagent:${project.build.directory}/test-agents/mockito-core.jar`;
- propriedade vazia `<argLine></argLine>` para preservar compatibilidade com outros produtores de `argLine`.

Essa configuracao substitui o self-attach dinamico do Mockito por agente Java carregado no startup da JVM de testes. Ela preserva `@InjectMock`, `quarkus-junit-mockito` e o relatorio do `quarkus-jacoco`.

Validacao padrao:

```powershell
mvn -q test
```

Resultado esperado:

- a suite passa;
- `target/test-agents/mockito-core.jar` e gerado;
- `target/jacoco-report/index.html` e gerado;
- nao aparecem os warnings `Mockito is currently self-attaching...`, `A Java agent has been loaded dynamically` e `Dynamic loading of agents will be disallowed...`.

O aviso abaixo pode aparecer mesmo com a solucao correta, porque ele esta relacionado ao class sharing da JVM com bootstrap classpath alterado por agente Java:

```text
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```

## Tratamento de erro

O REST Client usa `@ClientExceptionMapper` para converter respostas HTTP de erro do MTR em excecoes controladas.

Fluxo:

```text
MTR retorna 4xx/5xx
  -> ParametrizacaoClientExceptionMapper.toException(...) ou DossieProdutoClientExceptionMapper.toException(...)
  -> ClientErrorBodyReader
  -> MtrBusinessErrorException, MtrClientTechnicalException ou MtrServerErrorException
  -> MtrRestClientExceptionMapper
  -> ErroPadraoDto retornado ao chamador
```

| Condicao | Comportamento |
|---|---|
| `400`, `404`, `409`, `422` do MTR | `MtrBusinessErrorException`; sem retry; nao conta como falha do circuito |
| Demais `4xx` do MTR | `MtrClientTechnicalException`; sem retry; nao conta como falha do circuito |
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

Erros de negocio (`400`, `404`, `409`, `422`) abortam retry e sao ignorados pelo circuit breaker, porque representam erro funcional retornado pelo MTR. Demais erros `4xx`, como autenticacao/autorizacao, tambem abortam retry, mas sao classificados como erro tecnico de cliente. Erros `5xx`, timeout e falha de comunicacao sao tratados como falha potencialmente transitoria.

## Logs de REST Client

As chamadas dos REST Clients de parametrizacao e dossie produto passam pelo `RestClientObservabilityFilter`. O filtro registra dois eventos por chamada:

- `mtr.rest-client.request.enviada`
- `mtr.rest-client.response.recebida`

Esses logs incluem URL real montada pelo client, metodo HTTP, classe e operacao do client, status HTTP, duracao e payload truncado de entrada/saida. Headers nao sao logados para evitar exposicao de `apikey` ou token.

As mesmas informacoes principais tambem sao adicionadas ao span corrente como atributos/eventos, incluindo `rest_client.url`, `rest_client.request.body`, `rest_client.response.body`, tamanhos e marcadores de truncamento.

Controle por propriedade:

```properties
arvore-documento.observabilidade.rest-client.payload.habilitado=true
arvore-documento.observabilidade.rest-client.payload.input.max-length=2000
arvore-documento.observabilidade.rest-client.payload.output.max-length=4000
```

Se `payload.habilitado=false`, o filtro continua registrando URL, metodo, operacao, status e duracao, mas omite os corpos de entrada e saida.

## Observabilidade do projeto

A observabilidade atual tem duas saidas sempre ativas e duas saidas opcionais:

1. Logs JSON no console.
2. Logs JSON em arquivo local.
3. Traces OpenTelemetry exportados por OTLP quando o profile `jaeger` ou outro profile equivalente estiver ativo.
4. Logs OpenTelemetry exportados por OTLP quando o profile `jaeger` ou outro profile equivalente estiver ativo.

### Spans criados

| Camada | Span |
|---|---|
| API | `arvore-documento.api.processo.consultar` |
| Aplicacao | `arvore-documento.service.processo.consultar` |
| Integracao MTR | `mtr.parametrizacao.processo.consultar` |
| API | `arvore-documento.api.checklist.consultar` |
| Aplicacao | `arvore-documento.service.checklist.consultar` |
| Integracao MTR | `mtr.parametrizacao.checklist.consultar` |
| API | `arvore-documento.api.dossie-produto.criar` |
| Aplicacao | `arvore-documento.service.dossie-produto.criar` |
| Integracao MTR | `mtr.dossie-produto.criar` |
| API | `arvore-documento.api.dossie-produto.formulario.atualizar` |
| Aplicacao | `arvore-documento.service.dossie-produto.formulario.atualizar` |
| Integracao MTR | `mtr.dossie-produto.formulario.atualizar` |
| API | `arvore-documento.api.dossie-produto.documento.incluir` |
| Aplicacao | `arvore-documento.service.dossie-produto.documento.incluir` |
| Integracao MTR | `mtr.dossie-produto.documento.incluir` |
| API | `arvore-documento.api.dossie-produto.validacao-negocial.registrar` |
| Aplicacao | `arvore-documento.service.dossie-produto.validacao-negocial.registrar` |
| Integracao MTR | `mtr.dossie-produto.validacao-negocial.registrar` |
| API | `arvore-documento.api.dossie-produto.workflow.avancar` |
| Aplicacao | `arvore-documento.service.dossie-produto.workflow.avancar` |
| Integracao MTR | `mtr.dossie-produto.workflow.avancar` |

Os spans de integracao MTR aparecem quando o simulador correspondente esta desabilitado.

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
dossie_produto_id
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

Importante: Jaeger nao e a melhor ferramenta para consultar logs. A UI do Jaeger e focada em traces. Os logs ficam disponiveis no console, no arquivo JSON e, quando o profile `jaeger` esta ativo, tambem podem ser enviados por OTLP para um backend que aceite logs.

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
mvn quarkus:dev -Ddebug=false "-Dquarkus.profile=dev,jaeger"
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

### Opcao com Docker: Grafana LGTM via Quarkus Dev Services

Esta opcao e util em maquina de desenvolvimento com Docker. Ela nao e necessaria para a maquina sem Docker e nao substitui a configuracao remota via OpenTelemetry Collector.

Adicionar a dependencia:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-observability-devservices-lgtm</artifactId>
    <scope>provided</scope>
</dependency>
```

Para usar Dev Services LGTM, mantenha a aplicacao sem endpoint OTLP fixo ou crie um profile especifico para LGTM. O projeto ja evita endpoint fixo no modo padrao:

```properties
quarkus.otel.traces.exporter=none
quarkus.otel.logs.enabled=false
quarkus.otel.logs.handler.enabled=false
quarkus.otel.logs.exporter=none
```

Motivo: o Dev Service sobe seu proprio stack e pode configurar o endpoint OTLP automaticamente. Se a aplicacao apontar fixamente para `localhost:4317`, ela pode enviar para outro backend local, como Jaeger, e nao para o collector do stack LGTM.

Na pratica, para este projeto, prefira o profile `grafana` quando ja existir um OpenTelemetry Collector local ou remoto. Use Dev Services LGTM apenas quando quiser que o Quarkus suba a stack local via Docker.

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

Nesse modelo, a aplicacao usa um profile especifico para apontar para o OpenTelemetry Collector:

```properties
%grafana.quarkus.otel.exporter.otlp.endpoint=http://localhost:4317
%grafana.quarkus.otel.traces.exporter=cdi
%grafana.quarkus.otel.logs.enabled=true
%grafana.quarkus.otel.logs.handler.enabled=true
%grafana.quarkus.otel.logs.exporter=cdi
```

Neste caso, o `localhost:4317` deve ser um OpenTelemetry Collector, nao o Jaeger.

Execucao em dev mode:

```powershell
mvn quarkus:dev -Ddebug=false "-Dquarkus.profile=dev,grafana"
```

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

### Warning do Mockito sobre self-attach durante testes

Se `mvn -q test` voltar a mostrar:

```text
Mockito is currently self-attaching...
WARNING: A Java agent has been loaded dynamically
WARNING: Dynamic loading of agents will be disallowed by default in a future release
```

Verifique primeiro o `pom.xml`:

- `maven-dependency-plugin` deve copiar `org.mockito:mockito-core` para `target/test-agents/mockito-core.jar`;
- `maven-surefire-plugin` deve conter `@{argLine} -javaagent:${project.build.directory}/test-agents/mockito-core.jar`;
- `systemPropertyVariables` do Surefire deve continuar preservando `java.util.logging.manager=org.jboss.logmanager.LogManager`.

Nao tratar esse caso com `-XX:+EnableDynamicAgentLoading` como solucao principal. Essa flag apenas reduz ruido do JDK e mantem o comportamento que sera restringido em versoes futuras.

### Como verificar erros internos

Quando a API retorna erro `500`, a resposta HTTP fica propositalmente generica para nao expor detalhes internos:

```json
{
  "codigo_http": 500,
  "recurso": "arvore-documento",
  "id_erro": "cbcc0f91-0f3d-4d7c-8c8d-fdfeb6c14049",
  "codigo_erro": "ARVDOCP9999",
  "erros": [
    {
      "mensagem": "Erro interno ao processar a requisição."
    }
  ]
}
```

O detalhe tecnico da excecao, incluindo stacktrace, deve ser consultado apenas nos logs.

Use o `id_erro` retornado na resposta para localizar a linha correspondente no arquivo JSON:

```powershell
Select-String `
  -Path target/logs/arvore-documento.json `
  -Pattern "cbcc0f91-0f3d-4d7c-8c8d-fdfeb6c14049"
```

Para acompanhar os erros em tempo real:

```powershell
Get-Content -Tail 50 -Wait target/logs/arvore-documento.json
```

O log de erro nao tratado e emitido pelo `GenericExceptionMapper` com o evento:

```text
arvore-documento.erro.nao-tratado
```

Campos importantes para diagnostico:

| Campo | Uso |
|---|---|
| `id_erro` | Mesmo identificador retornado na resposta HTTP |
| `codigo_erro` | Codigo funcional do erro, por exemplo `ARVDOCP9999` |
| `erro_tipo` | Classe da excecao original |
| `erro_mensagem` | Mensagem tecnica da excecao |
| `traceId` | Correlacao com trace, quando houver span ativo |
| `spanId` | Span onde o erro foi registrado |

O stacktrace fica no campo de excecao do log JSON e tambem no console, conforme configurado em:

```properties
quarkus.log.console.json.exception-output-type=formatted
quarkus.log.file.json.exception-output-type=formatted
```

Se o erro acontecer durante o uso do simulador, verifique principalmente:

- se existe arquivo `.md` para o identificador consultado;
- se o arquivo possui a secao `## dados do mock corpo do retorno json`;
- se o JSON dessa secao e valido;
- se algum campo do JSON real possui formato diferente do DTO esperado.

### Maquina sem Docker ou sem Jaeger

Use o modo padrao:

```powershell
mvn quarkus:dev -Ddebug=false
```

Nesse modo, o projeto nao tenta conectar em `localhost:4317`. A execucao fica observavel pelo console JSON e pelo arquivo:

```powershell
Get-Content -Tail 20 target/logs/arvore-documento.json
```

### API retorna sempre o processo `1000016487` no dev mode

Isso e esperado quando o simulador esta habilitado e nao existe um arquivo de mock especifico para o identificador consultado.

O factory procura:

```text
mock/parametrizacao/{identificador}-consulta-processo-parametrizacao-v2-identificador-negocial.md
```

Se nao encontrar, usa o fallback:

```text
mock/parametrizacao/1000016487-consulta-processo-parametrizacao-v2-identificador-negocial.md
```

Para retornar outro processo, crie um arquivo `.md` com o identificador desejado e coloque o JSON na secao `## dados do mock corpo do retorno json`.

### Erro ao converter JSON do arquivo de mock

Confirme se o arquivo Markdown contem a secao:

```markdown
## dados do mock corpo do retorno json
```

O primeiro `{` depois dessa secao deve ser o inicio do corpo JSON. Evite colocar exemplos com `{` antes do JSON nessa mesma secao.

### Quero chamar o MTR real em dev mode

Desabilite o simulador:

```powershell
mvn quarkus:dev -Ddebug=false "-Darvore-documento.simulador.parametrizacao-processo.habilitado=false"
```

E configure a URL real:

```powershell
$env:QUARKUS_REST_CLIENT_PARAMETRIZACAO_PROCESSO_URL="https://api.des.caixa:8443/simtr"
```

### Servidor remoto nao recebe traces ou logs

Confirme se a aplicacao foi iniciada com o profile correto.

Para Jaeger remoto:

```powershell
mvn quarkus:dev -Ddebug=false "-Dquarkus.profile=dev,jaeger"
```

Para Grafana remoto via OpenTelemetry Collector:

```powershell
mvn quarkus:dev -Ddebug=false "-Dquarkus.profile=dev,grafana"
```

Confirme tambem a variavel de endpoint:

```powershell
$env:QUARKUS_OTEL_EXPORTER_OTLP_ENDPOINT
```

Teste conectividade de rede com a porta OTLP gRPC:

```powershell
Test-NetConnection servidor-otel-collector -Port 4317
```

Pontos de verificacao:

- Para Jaeger, a aplicacao envia OTLP para `servidor-jaeger:4317`; a UI, quando publicada, fica em `servidor-jaeger:16686`.
- Para Grafana, a aplicacao nao envia dados para `servidor-grafana:3000`; ela envia para o OpenTelemetry Collector, normalmente em `servidor-otel-collector:4317`.
- Se houver proxy, VPN, firewall ou rede corporativa, a porta `4317` precisa estar liberada.
- Se o endpoint remoto exigir token, configure `QUARKUS_OTEL_EXPORTER_OTLP_HEADERS`.
- Depois de subir a aplicacao, gere uma chamada HTTP real e aguarde alguns segundos, porque o exporter envia dados em lote.

### `/q/swagger-ui/` retorna 404

O Swagger deste projeto nao esta no path padrao do Quarkus.

Use:

```text
http://localhost:8080/arvore-documento/doc/
```

### `/arvore-documento/openai` retorna 404

O path OpenAPI configurado e:

```text
http://localhost:8080/arvore-documento/openapi
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

Confirme primeiro se a aplicacao foi iniciada com o profile `jaeger`:

```powershell
mvn quarkus:dev -Ddebug=false "-Dquarkus.profile=dev,jaeger"
```

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

Isso e esperado. Jaeger e uma ferramenta de traces. Mesmo com o profile `jaeger`, a consulta principal na UI do Jaeger e para traces. Para logs, use:

- console JSON;
- `target/logs/arvore-documento.json`;
- Grafana/Loki;
- outro backend compativel com logs OpenTelemetry.

### Erro `OpenTelemetry exporter set to 'otlp' but upstream dependencies not found`

Nao use:

```properties
%dev.quarkus.otel.traces.exporter=otlp
```

Nesta versao do Quarkus, use `none` quando nao quiser exportar e `cdi` quando quiser usar o exporter fornecido pelo Quarkus. O projeto usa `none` por padrao:

```properties
quarkus.otel.traces.exporter=none
quarkus.otel.logs.exporter=none
```

E usa `cdi` apenas nos profiles opcionais:

```properties
%jaeger.quarkus.otel.traces.exporter=cdi
%jaeger.quarkus.otel.logs.exporter=cdi
%grafana.quarkus.otel.traces.exporter=cdi
%grafana.quarkus.otel.logs.exporter=cdi
```

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

1. Criar um `docker-compose-jaeger.yml` para padronizar a subida do Jaeger.
2. Adicionar configuracao versionada de OpenTelemetry Collector para enviar traces ao Tempo e logs ao Loki.
3. Adicionar testes unitarios para `ProcessoService` cobrindo simulador e MTR real.
4. Adicionar stub/WireMock para respostas `200`, `404`, `500` e timeout do `simtr-parametrizacao`.
5. Adicionar stub/WireMock para respostas `201`, `400`, `403`, `404`, `409`, `500` e timeout do `simtr-dossie-produto`.
6. Criar dashboard Grafana com latencia, erro, volume e correlacao por `traceId`.
