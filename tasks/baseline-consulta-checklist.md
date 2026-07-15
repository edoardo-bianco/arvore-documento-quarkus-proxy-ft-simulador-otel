# Baseline de `ConsultarChecklist`

> Documento historico da capacidade antes e durante a migracao. O estado final pertence a
> `conformidade` e esta em `equivalencia-final.md`; evidencias quantitativas permanecem somente no
> JaCoCo.

## Escopo

Este manifesto caracteriza a implementacao legada de `parametrizacao` exigida pela Task 4.1,
antes da criacao do nucleo em `conformidade`. Nenhuma classe, configuracao ou fixture de producao
foi alterada. O fluxo observado e:

`ChecklistResource -> ParametrizacaoFachada -> ChecklistService -> ChecklistMockFactory ou
ParametrizacaoChecklistGateway -> ParametrizacaoChecklistClient`.

Nao fazem parte desta etapa endpoint novo, workflow, upload, cache de SAS, calculo de arvore, IA ou
Quarkus Flow. A evidencia numerica de cobertura permanece somente no relatorio JaCoCo; este arquivo
registra os contratos e riscos necessarios para a migracao.

## HTTP, JSON, validacao e OpenAPI

| Aspecto | Contrato legado |
|---|---|
| Operacao publica | `GET /simtr-hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}` |
| Midia | resposta `application/json`; a operacao nao possui corpo de entrada |
| Identificador | `Long`, obrigatorio pelo path e maior ou igual a `1` |
| Versao | `Integer`, obrigatoria pelo path e maior ou igual a `1` |
| Validacao do identificador | `400`, `ARVDOCP0001` e mensagem exata `O identificador negocial deve ser maior que zero.` |
| Validacao da versao | `400`, `ARVDOCP0001` e mensagem exata `A versão do checklist deve ser maior que zero.` |
| Sucesso de referencia | `200`; fixture `1000012583`, versao `1`; fingerprint JSON `fa497a3a7f7feea380be4312bb5ad8231c385c7a5aa10043b5092971fdb816aa` |
| Sem conteudo | resposta MTR `204` permanece `204` na borda publica |
| Respostas OpenAPI declaradas | `200` com `ChecklistDto`; `204`; `400` e `500` com `ErroPadraoDto` |
| Geracao OpenAPI | exclusivamente pelo Quarkus a partir das annotations Java; sem teste, filtro, complemento ou artefato estatico |

`ChecklistDto` preserva `nome`, `identificador_negocial`, `versao`, `data_hora_criacao`,
`data_hora_ultima_alteracao`, `verificacao_previa`, `orientacao_operador` e `apontamentos`. Cada
apontamento preserva `identificador_negocial`, `nome`, `descricao`, `orientacao_operador`,
`indicador_reanalise` e `sequencia_apresentacao`.

## Nulabilidade

Os DTOs de checklist e apontamento usam `JsonInclude.Include.NON_NULL`. Portanto:

- campos escalares nulos sao omitidos do JSON publico;
- lista nula e omitida, enquanto lista vazia continua como `[]`;
- elemento nulo dentro de `apontamentos` e preservado;
- campos nulos de um apontamento nao removem o objeto nem seus demais campos;
- o mapper legado preserva listas, elementos e campos nulos entre DTO e VO.

Identificador e versao da rota sao encaminhados sem transformacao ao MTR. O corpo de sucesso
publico reflete os valores devolvidos pela dependencia; o legado nao substitui esses campos pelos
parametros recebidos.

## Wire MTR e headers

O REST Client usa `configKey=parametrizacao-checklist`, path de classe
`/parametrizacao/v1/cadastro/checklist` e path de metodo
`/identificador-negocial/{identificador}/versao/{versao}`. Com a URL configurada atual, cujo
sufixo e `/simtr`, o wire materializado e protegido pelo stub local e:

`GET /simtr/parametrizacao/v1/cadastro/checklist/identificador-negocial/{identificador}/versao/{versao}`.

A requisicao nao envia corpo nem `Content-Type`; envia `Accept: application/json`, `apikey`,
`Authorization: Bearer <token OIDC>` e `traceparent` W3C. O client registra
`RequestHeaderFactory`, `OidcClientRequestReactiveFilter` e `RestClientObservabilityFilter`. O stub
escuta somente em `127.0.0.1`, porta efemera, e fornece token e respostas falsos de teste.

O atributo manual `url.path` do gateway permanece, por legado,
`/simtr-parametrizacao/v1/cadastro/checklist/identificador-negocial/{identificador}/versao/{versao}`.
Ele nao e o mesmo texto do path materializado via API Manager; os dois contratos devem ser
preservados em suas respectivas funcoes durante a migracao.

## Erros e fault tolerance

`ParametrizacaoClientExceptionMapper` usa `ClientErrorBodyReader` com recurso
`simtr-parametrizacao` e classifica:

| Status MTR | Excecao |
|---|---|
| menor que `400` | nenhuma |
| `400`, `404`, `409`, `422` | `MtrBusinessErrorException` |
| demais `4xx` | `MtrClientTechnicalException` |
| `5xx` | `MtrServerErrorException` |

O payload de erro valido e devolvido sem perda pela borda publica. Payload ausente, incompleto ou
ilegivel e normalizado pelo leitor compartilhado. O contrato vertical protege `404` sem retry e
preservacao integral do corpo; tambem protege a recuperacao de `500` seguida de `200` com duas
requisicoes GET identicas.

| Politica | Matriz declarada |
|---|---|
| Timeout | `2000 ms` |
| Retry | `maxRetries=3`, `delay=300 ms`, `jitter=100 ms` |
| Retry em | `MtrServerErrorException`, `ProcessingException`, `TimeoutException` |
| Abortar retry em | `MtrBusinessErrorException`, `MtrClientTechnicalException` |
| Circuit breaker | volume `10`, razao `0.5`, atraso `10000 ms`, limiar de sucesso `2` |
| Falha/skip do circuito | `failOn` igual a `retryOn`; `skipOn` igual a `abortOn` |

## Simulador, properties e profiles

| Contexto | URL/timeouts e selecao |
|---|---|
| padrao | URL `https://api.des.caixa:8443/simtr`; connect `3000 ms`; read `10000 ms`; simulador `false` |
| `dev` | simulador `true`; OIDC Client configurado pelo profile `dev` |
| `test` | URL segura `http://localhost:1`; connect/read `100 ms`; simulador `true`; sem Docker, Dev Services ou backend externo |
| contrato MTR local | URL dinamica em `127.0.0.1`; read `5000 ms`; simulador `false`; OIDC e token falsos locais |

Com simulador ativo, `ChecklistService` nao chama o gateway. `ChecklistMockFactory` procura
`mock/parametrizacao/{identificador}-v1-checklist-parametrizacao-versao-{versao}.md`; identificador
ou versao nulos usam `1000012583` e `1`. Arquivo inexistente usa silenciosamente essa mesma
fixture padrao. Com simulador inativo, o service chama o gateway.

## Observabilidade contratual

| Camada | Span e sinais estaveis |
|---|---|
| API | `simtr-hub.api.checklist.consultar`, `SERVER`; `http.route`, `simtr_hub.api=parametrizacao-checklist-v1`, identificador, versao, encontrado e nome quando presente |
| Aplicacao | `simtr-hub.service.checklist.consultar`, `INTERNAL`; flag `simtr_hub.simulador_parametrizacao_checklist_habilitado`, `simtr_hub.origem_dados=mock|mtr`, identificador, versao, encontrado e nome |
| Gateway MTR | `mtr.parametrizacao.checklist.consultar`, `CLIENT`; servico, API, metodo, path logico, sucesso e tipo de erro |
| Provider REST Client | classe `ParametrizacaoChecklistClient`, operacao `consultarPorIdentificadorNegocialEVersao`, metodo/URL/status/duracao e eventos request/response |

Eventos de log que devem sobreviver:

- API: `simtr-hub.checklist.requisicao.recebida`, `resposta.enviada`, `requisicao.falhou`;
- aplicacao: `simtr-hub.checklist.service.iniciado`, `simulador.usado`, `service.concluido`,
  `service.falhou`;
- gateway: `mtr.parametrizacao.checklist.chamada.iniciada`, `chamada.concluida`, `chamada.falhou`;
- provider: `mtr.rest-client.request.enviada`, `mtr.rest-client.response.recebida`.

No fluxo reativo legado, atributos e eventos do provider podem ficar distribuidos entre spans
ativos diferentes da mesma trace. Classe/operacao, request, status e response devem permanecer
presentes, sem exigir que todos estejam no mesmo span.

## Condicoes para a migracao

As Tasks 4.2 a 4.5 devem tratar checklist como agregado de leitura independente em `conformidade` e
preservar tipos wrapper nulaveis, nomes JSON, identificador e versao, paths, classificacao de
erros, selecao por property e telemetria. Nao ha autorizacao nesta caracterizacao para compartilhar
o modelo de processo, corrigir o fallback silencioso de mock, mudar o path logico de observabilidade
ou interpretar as strings de data como tipos temporais.
