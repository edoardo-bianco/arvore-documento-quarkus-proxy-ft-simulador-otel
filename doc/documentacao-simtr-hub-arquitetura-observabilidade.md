# simtr-hub: arquitetura, observabilidade e operacao

## Autoridade documental

Este documento descreve o estado operacional depois da refatoracao DDD concluida em 2026-07-15.

- decisao arquitetural: `arquitetura-ddd-integracoes-atomicas.md`;
- capacidades e equivalencia: `../tasks/equivalencia-final.md`;
- configuracao executavel: `../src/main/resources/application.properties`;
- inventario contratual de sinais: `../tasks/inventario-observabilidade.md`.

Documentos historicos de baseline mostram o comportamento antes e durante a migracao. Em caso de
divergencia estrutural, esta documentacao e a decisao arquitetural acima prevalecem.

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

O package interno `parametrizacao` nao existe mais. `parametrizacao` continua aparecendo em nomes
externos que nao podem ser renomeados silenciosamente: URLs, config keys, fixtures, spans e logs.

## Dominios

### `arvoredocumento`

Dono de `ConsultarProcessoParametrizado`. Nao calcula arvore e nao executa IA.

### `conformidade`

Dono de `ConsultarChecklist`. Nao analisa documentos nem orquestra conformidade.

### `dossieproduto`

Dono das capacidades atomicas de criacao, formulario, documento, validacao negocial e avanco de
workflow. A borda REST permanece no package de compatibilidade `recurso.rest.v1`; os demais
componentes seguem `dominio`, `aplicacao` e `adaptador`.

### `gestaodocumento`

Dono de `ObterCredencialContainer`. O Hub somente devolve a SAS e a validade opacas recebidas do
MTR. Nao ha Azure Storage SDK no nucleo, upload, cache, renovacao ou reutilizacao de credencial.

## Endpoints locais

```http
GET /simtr-hub/v1/processo/identificador-negocial/{identificador}
GET /simtr-hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}
POST /simtr-hub/v1/dossie-produto
PATCH /simtr-hub/v1/dossie-produto/{id}/formulario
POST /simtr-hub/v1/dossie-produto/{id}/documento
PATCH /simtr-hub/v1/dossie-produto/{id}/validacao-negocial
POST /simtr-hub/v1/dossie-produto/{id}/workflow
POST /simtr-hub/v1/storage/container/credencial
```

- Swagger UI: `/simtr-hub/doc`;
- OpenAPI Quarkus: `/simtr-hub/openapi`.

O documento OpenAPI e gerado exclusivamente pelo Quarkus. Nao existe arquivo estatico, filtro,
complemento ou teste do artefato gerado.

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

## Fault tolerance e erros

As annotations de timeout, retry e circuit breaker ficam somente nos REST Clients MTR. Erros
negociais nao sao tratados como falhas transitorias; erros de servidor, comunicacao e timeout
seguem a matriz congelada de cada capacidade.

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

O filtro de REST Client registra metodo, URL, status, duracao, classe e operacao. Payloads sao
truncados e mascarados para campos sensiveis. SAS e validade de credencial nao sao registradas.

### Traces

Cada capacidade preserva spans nas fronteiras REST, caso de uso e adapter MTR. Os nomes e
atributos completos ficam em `../tasks/inventario-observabilidade.md` e sao protegidos por
`ObservabilidadeSpansContratoTest` e pelos contratos ponta a ponta.

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
4. trace e atributos do REST Client;
5. classificacao do erro e tentativas previstas pela matriz FT.

## Limites e dividas conhecidas

- retry em operacoes mutaveis exige prova de idempotencia antes de orquestracao futura;
- warnings Jakarta Validation remanescentes de formulario e documento precisam de mudanca
  separada e testes de contrato;
- o package REST de `dossieproduto` e o package tecnico de erro sao desvios internos documentados;
- Quarkus Flow, persistencia de workflow, novos endpoints, upload e lifecycle de SAS permanecem
  fora do escopo.
