# arvore-documento - Proxy Quarkus com observabilidade

## Indice

- [Objetivo](#objetivo)
- [Cenario de uso](#cenario-de-uso)
- [Contrato exposto](#contrato-exposto)
- [Arquitetura](#arquitetura)
- [Pacotes](#pacotes)
- [Configuracao atual](#configuracao-atual)
- [Mock da parametrizacao](#mock-da-parametrizacao)
- [Perfis de observabilidade](#perfis-de-observabilidade)
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
- exportar traces e logs por OpenTelemetry quando um backend for ativado;
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

```http
GET http://localhost:8080/arvore-documento/v1/checklist/identificador-negocial/1000012583/versao/1
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

HTTP GET /arvore-documento/v1/checklist/identificador-negocial/{id}/versao/{versao}
  -> ChecklistResource
  -> ChecklistService
  -> ParametrizacaoChecklistGateway
  -> ParametrizacaoChecklistClient
  -> GET /simtr-parametrizacao/v1/cadastro/checklist/identificador-negocial/{id}/versao/{versao}
  -> ChecklistMapper
  -> resposta do arvore-documento
```

## Pacotes

```text
br.gov.caixa.simtr.arvoredocumento
|-- api
|   |-- dev
|   |-- dto
|   |   |-- erro
|   |   |-- parametrizacao.checklist
|   |   `-- parametrizacao.processo
|   |-- exception
|   `-- parametrizacao
|-- application
|   `-- parametrizacao
|-- domain
|   |-- parametrizacao.checklist
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

quarkus.rest-client.parametrizacao-checklist.url=http://localhost:8081
quarkus.rest-client.parametrizacao-checklist.connect-timeout=3000
quarkus.rest-client.parametrizacao-checklist.read-timeout=10000

arvore-documento.simulador.parametrizacao-processo.habilitado=false
%dev.arvore-documento.simulador.parametrizacao-processo.habilitado=true
arvore-documento.simulador.parametrizacao-checklist.habilitado=false
%dev.arvore-documento.simulador.parametrizacao-checklist.habilitado=true
```

Configuracao de observabilidade atual:

```properties
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
export QUARKUS_REST_CLIENT_PARAMETRIZACAO_PROCESSO_URL=https://simtr-parametrizacao-des.apps.nprd.caixa
export QUARKUS_REST_CLIENT_PARAMETRIZACAO_CHECKLIST_URL=https://simtr-parametrizacao-des.apps.nprd.caixa
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

### Como chamar o MTR real em dev mode

Se precisar testar a integracao real mesmo em dev mode, desabilite o simulador:

```powershell
mvn quarkus:dev -Ddebug=false "-Darvore-documento.simulador.parametrizacao-processo.habilitado=false"
```

Para desabilitar apenas o simulador de checklist:

```powershell
mvn quarkus:dev -Ddebug=false "-Darvore-documento.simulador.parametrizacao-checklist.habilitado=false"
```

Ou configure por variavel de ambiente:

```powershell
$env:ARVORE_DOCUMENTO_SIMULADOR_PARAMETRIZACAO_PROCESSO_HABILITADO="false"
$env:ARVORE_DOCUMENTO_SIMULADOR_PARAMETRIZACAO_CHECKLIST_HABILITADO="false"
```

Nesse caso, configure tambem a URL real do MTR:

```powershell
$env:QUARKUS_REST_CLIENT_PARAMETRIZACAO_PROCESSO_URL="https://simtr-parametrizacao-des.apps.nprd.caixa"
$env:QUARKUS_REST_CLIENT_PARAMETRIZACAO_CHECKLIST_URL="https://simtr-parametrizacao-des.apps.nprd.caixa"
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
  -> ParametrizacaoClientExceptionMapper.toException(...)
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
$env:QUARKUS_REST_CLIENT_PARAMETRIZACAO_PROCESSO_URL="https://simtr-parametrizacao-des.apps.nprd.caixa"
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

1. Decidir se o path OpenAPI deve continuar `/arvore-documento/openai` ou mudar para `/arvore-documento/openapi`.
2. Criar um `docker-compose-jaeger.yml` para padronizar a subida do Jaeger.
3. Adicionar configuracao versionada de OpenTelemetry Collector para enviar traces ao Tempo e logs ao Loki.
4. Adicionar testes unitarios para `ProcessoService` cobrindo simulador e MTR real.
5. Adicionar stub/WireMock para respostas `200`, `404`, `500` e timeout do `simtr-parametrizacao`.
6. Criar dashboard Grafana com latencia, erro, volume e correlacao por `traceId`.
