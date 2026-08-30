# simtr-hub: arquitetura, observabilidade e operacao

## Autoridade documental

Este documento descreve o estado operacional atual do `simtr-hub`.

- decisao arquitetural: `arquitetura-ddd-integracoes-atomicas.md`;
- indice de ADRs: `adr/README.md`;
- especificacao funcional de referencia: `api-integracao-mtr-pre-validacao-v1.md`;
- configuracao executavel: `../src/main/resources/application.properties`;
- catalogo contratual de sinais: `catalogo-observabilidade.md`.

Em caso de divergencia, código, contratos executáveis e testes representam o comportamento atual;
a divergência documental deve ser registrada no plano da próxima feature.

## Fluxo atual

```text
Resource REST
  -> mapper da borda publica
  -> porta de entrada
  -> caso de uso atomico
  -> porta de saida
      |-> adapter MTR -> REST Client
      `-> adapter simulador -> fixture Markdown
```

Cada dominio possui modelos, falhas, portas e adapters proprios. DTO REST, DTO MTR e DTO do
simulador nao sao compartilhados. Producers CDI selecionam o adapter de saida e o caso de uso nao
conhece a property do simulador.

A organizacao hexagonal e pragmatica: Quarkus pode ser usado em qualquer componente, incluindo
dominio, aplicacao, portas e casos de uso, sem bloqueio arquitetural por framework. ArchUnit
protege a direcao das dependencias, o isolamento dos dominios e o confinamento dos contratos de
borda; ele nao rejeita uma classe somente por usar Quarkus, Jakarta, MicroProfile, Mutiny, Jackson
ou OpenTelemetry. Restricoes de papeis especificos, como manter REST Clients no adapter MTR,
continuam validas.

O package interno `parametrizacao` nao existe mais. `parametrizacao` continua aparecendo em nomes
externos que nao podem ser renomeados silenciosamente: URLs, config keys, fixtures, spans e logs.

## Dominios

### `arvoredocumento`

Dono de `ConsultarProcessoParametrizado`. Nao calcula arvore e nao executa IA.

### `conformidade`

Dono de `ConsultarChecklist`. Nao analisa documentos nem orquestra conformidade.

### `dossieproduto`

Dono das capacidades atomicas de consulta por identificador, consulta de documentos vinculados,
criacao, formulario, documento, validacao negocial, alteracao de produtos contratados, captura
para edicao e avanco de workflow. A borda REST segue o package canonico
`adaptador.entrada.rest.v1`; os demais componentes seguem `dominio`, `aplicacao` e `adaptador`.

### `gestaodocumento`

Dono de `ObterCredencialContainer`. O Hub somente devolve a SAS e a validade opacas recebidas do
MTR. Nao ha Azure Storage SDK no nucleo, upload, cache, renovacao ou reutilizacao de credencial.

## Endpoints locais

```http
GET /simtr-hub/v1/processo/identificador-negocial/{identificador}
GET /simtr-hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}
GET /simtr-hub/v1/dossie-produto/{id}
GET /simtr-hub/v1/dossie-produto/{id}/documentos
POST /simtr-hub/v1/dossie-produto
PATCH /simtr-hub/v1/dossie-produto/{id}/formulario
POST /simtr-hub/v1/dossie-produto/{id}/documento
PATCH /simtr-hub/v1/dossie-produto/{id}/validacao-negocial
PATCH /simtr-hub/v1/dossie-produto/{id}/produto
POST /simtr-hub/v1/dossie-produto/{id}/capturar
POST /simtr-hub/v1/dossie-produto/{id}/workflow
POST /simtr-hub/v1/storage/container/credencial
```

- Swagger UI: `/simtr-hub/doc`;
- OpenAPI Quarkus: `/simtr-hub/openapi`.

O documento OpenAPI e gerado exclusivamente pelo Quarkus. Nao existe arquivo estatico, filtro ou
complemento; os testes protegem o comportamento HTTP e os contratos Java que alimentam a geracao,
sem inspecionar o artefato gerado.

## Limite frente aos endpoints da especificacao de pre-validacao

Os endpoints locais acima correspondem as doze capacidades implementadas. A especificacao
`api-integracao-mtr-pre-validacao-v1.md` tambem cataloga duas operacoes do ciclo de vida do
dossie que **NAO EXISTEM NESTE HUB**:

| Endpoint MTR descrito na especificacao | Estado operacional no Hub |
|---|---|
| `PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/garantia` | Nao implementado |
| `POST /simtr-dossie-produto/v1/dossie-produto/{id}/cancelar` | Nao implementado |

Para essas operacoes nao ha Resource, rota `/simtr-hub`, porta ou caso de uso, REST Client,
adapter MTR, simulador, configuracao, fault tolerance ou sinais de observabilidade no Hub. A
ausencia e somente desta solucao; o documento funcional pode descrever uma API existente no MTR.
Elas nao sao usadas nos diagramas de sequencia principais da pre-validacao e nao existe endpoint
unico de pre-validacao ou orquestrador local.

Os prefixos `/simtr-parametrizacao`, `/simtr-dossie-produto` e `/simtr-gestao-documento` usados
pela especificacao representam os servicos MTR. Nesta implantacao, o gateway e configurado com
base `/simtr`, e cada REST Client acrescenta seu segmento de servico. A matriz completa, incluindo
as doze operacoes implementadas, esta em `arquitetura-ddd-integracoes-atomicas.md`.

## Configuracao de integracoes

Config keys dos REST Clients:

```text
parametrizacao-processo
parametrizacao-checklist
dossie-produto
gestao-documento
```

As URLs e os timeouts de conexao sao configurados em `application.properties` e podem ser
sobrescritos por ambiente. `SIMTR_API_KEY` e os secrets OIDC nunca devem ser versionados.

### Selecao do simulador

```properties
simtr-hub.simulador.parametrizacao-processo.habilitado=false
simtr-hub.simulador.parametrizacao-checklist.habilitado=false
simtr-hub.simulador.dossie-produto.habilitado=false
simtr-hub.simulador.gestao-documento.habilitado=false
```

O profile `dev` habilita os simuladores. O profile padrao de testes usa fixtures e stubs localhost,
sem Docker, Dev Services ou rede externa.

A captura e a consulta de documentos reutilizam
`simtr-hub.simulador.dossie-produto.habilitado`: desabilitada, a property seleciona os adapters
MTR; habilitada, cada capacidade usa fixture, DTO e mapper proprios sem chamada de rede. A consulta
de documentos entrega o cenario deterministico do identificador `4081899` e nao reproduz os
filtros ou projecoes do MTR.

## Fault tolerance e erros

As annotations de timeout, retry e circuit breaker ficam somente nos REST Clients MTR. Erros
negociais nao sao tratados como falhas transitorias; erros de servidor, comunicacao e timeout
seguem a matriz congelada de cada capacidade.

A captura aplica timeout e circuit breaker, mas nao possui retry automatico. Como a operacao altera
estado e o contrato MTR nao comprova idempotencia, erros `500` e timeout geram uma unica chamada.

A consulta de documentos chama o GET idempotente
`/simtr/dossie-produto/v4/dossie-produto/{id}/documentos`, encaminha somente os 12 filtros
opcionais informados e aplica timeout, retry apenas para falhas transitorias e circuit breaker.

A ordem e contratual:

1. o REST Client classifica o erro de protocolo dentro da chamada interceptada;
2. a politica de fault tolerance termina;
3. o adapter MTR converte a falha para o tipo interno lossless;
4. o mapper REST converte a falha interna para o status e JSON publicos.

O DTO tecnico compartilhado de erro REST fica em `arquitetura.excecao.dto`. ArchUnit impede seu
uso no dominio, na aplicacao e nos adapters de saida.

## Observabilidade

### Logs

Logs estruturados sao escritos no console e em:

```text
target/logs/simtr-hub.json
```

O filtro compartilhado de REST Client registra metodo, URL, status, duracao, classe e operacao nos
clients que o utilizam. Payloads sao truncados e mascarados para campos sensiveis. SAS e validade
de credencial nao sao registradas. Os clients da captura e da consulta de documentos nao registram
esse filtro; usam eventos proprios e providers locais para não publicar payload, URL interna
completa ou query string.

### Traces

Cada capacidade preserva spans nas fronteiras REST, caso de uso e adapter MTR. Os nomes e
atributos completos ficam em `catalogo-observabilidade.md` e sao protegidos por
`ObservabilidadeSpansContratoTest` e pelos contratos ponta a ponta.

No modo MTR, a captura produz exatamente os spans
`simtr-hub.api.dossie-produto.capturar` (`SERVER`),
`simtr-hub.service.dossie-produto.capturar` (`INTERNAL`) e
`mtr.dossie-produto.capturar` (`CLIENT`). Somente nesse client, `TracingPolicy.IGNORE` suprime o
span HTTP automatico com `url.full`; o contexto e reinjetado pelo propagador OpenTelemetry
configurado, mantendo o `traceparent` ligado ao CLIENT proprio. Os demais REST Clients nao sao
afetados.

No modo MTR, a consulta de documentos produz exatamente
`simtr-hub.api.dossie-produto.documentos.consultar` (`SERVER`),
`simtr-hub.service.dossie-produto.documentos.consultar` (`INTERNAL`) e
`mtr.dossie-produto.documentos.consultar` (`CLIENT`), no mesmo trace e com parentage
`SERVER -> INTERNAL -> CLIENT`. O filtro de entrada esvazia `url.query`; no outbound, o provider
exclusivo usa `TracingPolicy.IGNORE` e reinjeta o contexto no `traceparent`, preservando um unico
CLIENT proprio. Os sinais mantêm rota parametrizada, versao v4, origem, flag do simulador,
identificador, quantidade e tipo tecnico de erro, sem filtros, identidade, URL de documento,
storage, payload ou credenciais. No simulador permanecem apenas SERVER e INTERNAL, sem rede.

Por padrao:

```properties
quarkus.otel.traces.exporter=none
quarkus.otel.logs.enabled=false
quarkus.otel.logs.exporter=none
```

Os profiles opcionais `jaeger` e `grafana` apontam para OTLP em `localhost:4317` e so devem ser
usados quando o coletor correspondente estiver disponivel.

## Execucao e diagnostico

Executar em dev mode:

```bash
mvn quarkus:dev -Ddebug=false
```

Executar todos os gates locais:

```bash
mvn -q clean test
```

Cobertura:

```text
target/jacoco-report/index.html
```

Se uma chamada MTR falhar, verificar nesta ordem:

1. property do simulador e profile ativo;
2. URL do REST Client e credenciais por ambiente;
3. logs `mtr.*.chamada.iniciada`, `concluida` ou `falhou`;
4. trace, span CLIENT da capacidade e, quando aplicável, atributos do filtro REST Client;
5. classificacao do erro e tentativas previstas pela matriz FT.

## Limites e dividas conhecidas

- retry em operacoes mutaveis exige prova de idempotencia antes de orquestracao futura;
- o package tecnico compartilhado de erro `arquitetura.excecao.dto` permanece como desvio interno
  documentado e confinado as bordas REST permitidas;
- Quarkus Flow, persistencia de workflow, os dois endpoints ausentes listados acima, quaisquer
  outros endpoints novos, upload e lifecycle de SAS permanecem fora do escopo.

As listas REST de formulario e documento usam `List<@Valid T>`; contratos executaveis preservam
mensagens e nulabilidade. Resource, mappers, DTOs e testes de dossie residem na borda REST canônica
do domínio, sem alterar os contratos HTTP.
