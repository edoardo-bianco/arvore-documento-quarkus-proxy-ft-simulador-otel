# Baseline de `ObterCredencialContainer`

## Escopo

Este manifesto caracteriza a implementacao legada de `gestaodocumento` exigida pela Task 5.1,
antes da criacao do novo nucleo. Nenhuma classe, property ou fixture de producao foi alterada. O
fluxo observado e:

`GestaoDocumentoResource -> GestaoDocumentoFachada -> GestaoDocumentoService ->
GestaoDocumentoMockFactory ou GestaoDocumentoGateway -> GestaoDocumentoClient`.

A capacidade somente obtem e devolve uma credencial de container. Nao fazem parte desta etapa
upload, Azure Blob SDK, cache, reutilizacao ou renovacao de SAS, endpoint ou workflow novo e
Quarkus Flow. A cobertura automatizada e sua evolucao permanecem no codigo de teste e no relatorio
JaCoCo, sem contagens neste documento.

## HTTP, JSON e OpenAPI

| Aspecto | Contrato legado |
|---|---|
| Operacao publica | `POST /simtr-hub/v1/storage/container/credencial` |
| Entrada | sem corpo; `Consumes` aceita qualquer tipo de midia |
| Sucesso | `200` com `Content-Type: application/json` e corpo de credencial |
| Campos | `sas`, `validade`, `url_storage` e `nome_container` |
| Tipos Java atuais | `sas`, `urlStorage` e `nomeContainer` sao `String`; `validade` e `Object` |
| Nulabilidade | os quatro campos aceitam nulo e continuam presentes como `null` no JSON publico |
| Respostas OpenAPI declaradas | `200` com `GestaoDocumentoCredencialContainerDto`; `401`, `403`, `500` e `503` com `ErroPadraoDto` |
| Geracao OpenAPI | exclusivamente pelo Quarkus a partir das annotations Java; sem teste, filtro, complemento ou artefato estatico |

O endpoint nao recebe nome de container nem qualquer parametro de duracao. O nome, a URL, a
credencial e sua validade sao integralmente fornecidos pela origem selecionada.

## SAS e validade contratual

`sas` e um valor opaco: o hub nao interpreta seus parametros. `validade` tambem atravessa o fluxo
sem conversao e hoje pode ser uma string, um objeto JSON ou nulo. O mapper legado preserva tanto
uma validade textual quanto uma validade estruturada.

A caracterizacao usa um valor de validade deliberadamente distinto e exige igualdade exata entre
MTR e resposta publica. Isso protege o contrato sem inferir instante de emissao, unidade, fuso,
duracao ou politica de expiracao. A migracao nao pode introduzir duracao fixa, calculo de validade,
cache, renovacao, reutilizacao ou upload.

## Wire MTR e headers

O REST Client usa `configKey=gestao-documento`, path de classe `/gestao-documento` e path de metodo
`/v1/storage/container/credencial`. Com a URL configurada, cujo sufixo e `/simtr`, o wire
materializado e:

`POST /simtr/gestao-documento/v1/storage/container/credencial`.

A requisicao nao possui corpo nem `Content-Type`; envia `Accept: application/json`, `apikey`,
`Authorization: Bearer <token OIDC>` e `traceparent` W3C. O client registra
`RequestHeaderFactory`, `OidcClientRequestReactiveFilter` e `RestClientObservabilityFilter`. A
caracterizacao do caminho MTR usa somente um stub em `127.0.0.1`, porta efemera, com token e dados
sinteticos.

O atributo manual `url.path` do gateway usa o mesmo path materializado:
`/simtr/gestao-documento/v1/storage/container/credencial`.

## Erros e fault tolerance

`GestaoDocumentoClientExceptionMapper` usa `ClientErrorBodyReader` com recurso
`simtr-gestao-documento` e classifica:

| Status MTR | Excecao |
|---|---|
| menor que `400` | nenhuma |
| `400`, `404`, `409`, `422` | `MtrBusinessErrorException` |
| demais `4xx` | `MtrClientTechnicalException` |
| `5xx` | `MtrServerErrorException` |

O payload de erro valido e devolvido sem perda pela borda publica. Payload ausente, incompleto ou
ilegivel e normalizado pelo leitor compartilhado. Erro de negocio aborta retry; erro de servidor
pode ser repetido preservando o mesmo POST sem corpo.

| Politica | Matriz declarada |
|---|---|
| Timeout | `2000 ms` |
| Retry | `maxRetries=3`, `delay=300 ms`, `jitter=100 ms` |
| Retry em | `MtrServerErrorException`, `ProcessingException`, `TimeoutException` |
| Abortar retry em | `MtrBusinessErrorException`, `MtrClientTechnicalException` |
| Circuit breaker | volume `10`, razao `0.5`, atraso `10000 ms`, limiar de sucesso `2` |
| Falha/skip do circuito | `failOn` igual a `retryOn`; `skipOn` igual a `abortOn` |

O retry de POST e comportamento existente e deve ser preservado nesta refatoracao, sem ser
interpretado como autorizacao para novos fluxos mutaveis.

## Simulador, properties e profiles

| Contexto | URL/timeouts e selecao |
|---|---|
| padrao | URL `https://api.des.caixa:8443/simtr`; connect `3000 ms`; read `10000 ms`; simulador `false` |
| `dev` | simulador `true`; OIDC Client configurado pelo profile `dev` |
| `test` | URL segura `http://localhost:1`; connect/read `100 ms`; simulador `true`; sem Docker, Dev Services ou backend externo |
| contrato MTR local | URL dinamica em `127.0.0.1`; read `5000 ms`; simulador `false`; OIDC e token falsos locais |

Com simulador ativo, `GestaoDocumentoService` nao chama o gateway e le o primeiro objeto JSON de
`mock/gestaodocumento/credencial-container.md`. Fixture ausente ou sem objeto JSON causa
`IllegalStateException`; nao existe fallback silencioso. Com simulador inativo, o service chama o
gateway. A data presente na fixture e apenas dado simulado e nao define duracao contratual.

## Observabilidade contratual

| Camada | Span e sinais estaveis |
|---|---|
| API | `simtr-hub.api.gestao-documento.credencial-container.gerar`, `SERVER`; `http.route`, `simtr_hub.api=gestao-documento-v1` e nome do container quando presente |
| Aplicacao | `simtr-hub.service.gestao-documento.credencial-container.gerar`; flag do simulador, origem `mock|mtr` e nome do container quando presente |
| Gateway MTR | `mtr.gestao-documento.credencial-container.gerar`, `CLIENT`; servico, API, metodo, path, sucesso, nome do container e tipo de erro |
| Provider REST Client | classe `GestaoDocumentoClient`, operacao `gerarCredencialContainer`, metodo/URL/status/duracao e eventos request/response |

Eventos de log que devem sobreviver:

- API: `simtr-hub.gestao-documento.credencial-container.requisicao.recebida`,
  `resposta.enviada`, `requisicao.falhou`;
- aplicacao: `simtr-hub.gestao-documento.credencial-container.service.iniciado`,
  `simulador.usado`, `service.concluido`, `service.falhou`;
- gateway: `mtr.gestao-documento.credencial-container.chamada.iniciada`,
  `chamada.concluida`, `chamada.falhou`;
- provider: `mtr.rest-client.request.enviada`, `mtr.rest-client.response.recebida`.

No fluxo reativo, atributos e eventos do provider podem ficar distribuidos entre spans ativos da
mesma trace. Classe/operacao, request, status e response devem permanecer presentes, sem exigir que
todos estejam no mesmo span.

## Condicoes para a migracao

As Tasks 5.2 a 5.6 devem preservar o endpoint publico, nomes e nulabilidade JSON, wire MTR,
headers, classificacao de erros, ordem de fault tolerance, selecao por property, fixture e
telemetria. O novo nucleo representa somente a obtencao da credencial e deve tratar SAS e validade
como dados contratuais opacos, sem dependencia de SDK de storage ou politica de lifecycle.
