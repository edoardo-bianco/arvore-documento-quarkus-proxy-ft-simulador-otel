# Baseline de `ConsultarProcessoParametrizado`

> Documento historico da capacidade antes e durante a migracao. O estado final pertence a
> `arvoredocumento` e esta em `equivalencia-final.md`; evidencias quantitativas permanecem somente
> no JaCoCo.

## Escopo

Este manifesto caracteriza a implementação legada de `parametrizacao` exigida pela Task 3.1,
antes da criação do núcleo em `arvoredocumento`. Nenhuma classe, configuração ou fixture de
produção foi alterada. O fluxo observado é:

`ProcessoResource -> ParametrizacaoFachada -> ProcessoService -> ProcessoMockFactory ou
ParametrizacaoProcessoGateway -> ParametrizacaoProcessoClient`.

Não fazem parte desta etapa cálculo de árvore, IA, endpoint novo, workflow, upload, cache de SAS ou
Quarkus Flow. A evidência numérica de cobertura permanece somente no relatório JaCoCo; este arquivo
registra contratos e riscos necessários para a migração.

## HTTP, JSON, validação e OpenAPI

| Aspecto | Contrato legado |
|---|---|
| Operação pública | `GET /simtr-hub/v1/processo/identificador-negocial/{identificador}` |
| Mídia | resposta `application/json`; a operação não possui corpo de entrada |
| Parâmetro | `identificador` é `Long`, obrigatório pelo path e deve ser maior ou igual a `1` |
| Validação | zero e negativo retornam `400`, `ARVDOCP0001` e a mensagem exata `O identificador negocial deve ser maior que zero.` |
| Sucesso de referência | `200`; árvore completa do identificador `1000016487`; fingerprint JSON `01d6ac0993410e4cb8edc9c338a333acd56fa93390054c31907351ca19d70d34` |
| Respostas OpenAPI declaradas | `200` com `ProcessoDto`; `400`, `404` e `500` com `ErroPadraoDto` |
| Geração OpenAPI | exclusivamente pelo Quarkus a partir das annotations Java; sem teste, filtro, complemento ou artefato estático |

Os 12 schemas são `ProcessoDto`, `MacroprocessoDto`, `RelacionamentoDto`, `ProdutoDto`, `FaseDto`,
`GarantiaDto`, `DocumentoDto`, `FuncaoDocumentalDto`, `TipoDocumentoDto`, `CampoFormularioDto`,
`OpcaoDisponivelDto` e `ChecklistReferenciaDto`. O processo contém macroprocesso,
relacionamentos, produtos, fases, documentos e checklist; os ramos reutilizam campos de formulário,
documentos, garantias, tipos documentais e referências de checklist.

## Nulabilidade e desserialização tolerante

Todos os DTOs da árvore usam `JsonInclude.Include.NON_NULL`. Portanto:

- campo ou objeto nulo é omitido do JSON de saída;
- lista nula é omitida, enquanto lista vazia continua como `[]`;
- elemento nulo dentro de lista é preservado;
- objetos não nulos mantêm seus campos não nulos mesmo quando os demais são omitidos;
- o mapper legado preserva listas, elementos, objetos aninhados e campos nulos entre DTO e VO.

`ChecklistReferenciaDto` aceita no wire um objeto ou um array e, no segundo caso, usa o primeiro
elemento. Array vazio, JSON nulo ou valor que não seja objeto resultam em referência nula; números
e representações numéricas em texto são aceitos. Essa tolerância pertence ao contrato de entrada
MTR e não deve ser perdida na Task 3.3.

## Wire MTR e headers

O REST Client usa `configKey=parametrizacao-processo`, path de classe
`/parametrizacao/v2/patriarca/processo` e path de método
`/identificador-negocial/{identificador}`. Com a URL configurada atual, cujo sufixo é `/simtr`, o
wire materializado e protegido pelo stub local é:

`GET /simtr/parametrizacao/v2/patriarca/processo/identificador-negocial/{identificador}`.

A requisição não envia corpo nem `Content-Type`; envia `Accept: application/json`,
`apikey`, `Authorization: Bearer <token OIDC>` e `traceparent` W3C. O client registra
`RequestHeaderFactory`, `OidcClientRequestReactiveFilter` e `RestClientObservabilityFilter`. O stub
escuta somente em `127.0.0.1`, porta efêmera, e fornece token e respostas falsos de teste.

O atributo manual `url.path` do gateway permanece, por legado,
`/simtr-parametrizacao/v2/patriarca/processo/identificador-negocial/{identificador}`. Ele não é o
mesmo texto do path materializado via API Manager; os dois contratos devem ser preservados em suas
respectivas funções durante a migração.

## Erros e fault tolerance

`ParametrizacaoClientExceptionMapper` usa `ClientErrorBodyReader` com recurso
`simtr-parametrizacao` e classifica:

| Status MTR | Exceção |
|---|---|
| menor que `400` | nenhuma |
| `400`, `404`, `409`, `422` | `MtrBusinessErrorException` |
| demais `4xx` | `MtrClientTechnicalException` |
| `5xx` | `MtrServerErrorException` |

O payload de erro válido é devolvido sem perda pela borda pública. Payload ausente, incompleto ou
ilegível é normalizado pelo leitor compartilhado. O contrato vertical prova `404` sem retry e
preservação integral do corpo; também prova recuperação de `500` seguida de `200` com duas
requisições GET idênticas.

| Política | Matriz declarada |
|---|---|
| Timeout | `2000 ms` |
| Retry | `maxRetries=3`, `delay=300 ms`, `jitter=100 ms` |
| Retry em | `MtrServerErrorException`, `ProcessingException`, `TimeoutException` |
| Abortar retry em | `MtrBusinessErrorException`, `MtrClientTechnicalException` |
| Circuit breaker | volume `10`, razão `0.5`, atraso `10000 ms`, limiar de sucesso `2` |
| Falha/skip do circuito | `failOn` igual a `retryOn`; `skipOn` igual a `abortOn` |

## Simulador, properties e profiles

| Contexto | URL/timeouts e seleção |
|---|---|
| padrão | URL `https://api.des.caixa:8443/simtr`; connect `3000 ms`; read `10000 ms`; simulador `false` |
| `dev` | simulador `true`; OIDC Client configurado pelo profile `dev` |
| `test` | URL segura `http://localhost:1`; connect/read `100 ms`; simulador `true`; sem Docker, Dev Services ou backend externo |
| contrato MTR local | URL dinâmica em `127.0.0.1`; read `5000 ms`; simulador `false`; OIDC e token falsos locais |

Com simulador ativo, `ProcessoService` não chama o gateway. `ProcessoMockFactory` procura
`mock/parametrizacao/{identificador}-consulta-processo-parametrizacao-v2-identificador-negocial.md`;
identificador nulo ou arquivo inexistente usa o fixture padrão `1000016487`. O fixture específico
`1000009990` também faz parte do inventário. Com simulador inativo, o mesmo service chama o gateway.

## Observabilidade contratual

| Camada | Span e sinais estáveis |
|---|---|
| API | `simtr-hub.api.processo.consultar`, `SERVER`; `http.route`, `simtr_hub.api=parametrizacao-processo-v1`, identificador, encontrado e nome quando presente |
| Aplicação | `simtr-hub.service.processo.consultar`, `INTERNAL`; flag `simtr_hub.simulador_parametrizacao_processo_habilitado`, `simtr_hub.origem_dados=mock|mtr`, identificador, encontrado e nome |
| Gateway MTR | `mtr.parametrizacao.processo.consultar`, `CLIENT`; serviço, API, método, path lógico, sucesso e tipo de erro |
| Provider REST Client | classe `ParametrizacaoProcessoClient`, operação `consultarPorIdentificadorNegocial`, método/URL/status/duração e eventos request/response |

Eventos de log que devem sobreviver:

- API: `simtr-hub.processo.requisicao.recebida`, `resposta.enviada`, `requisicao.falhou`;
- aplicação: `simtr-hub.processo.service.iniciado`, `simulador.usado`, `service.concluido`,
  `service.falhou`;
- gateway: `mtr.parametrizacao.processo.chamada.iniciada`, `chamada.concluida`, `chamada.falhou`;
- provider: `mtr.rest-client.request.enviada`, `mtr.rest-client.response.recebida`.

No fluxo reativo legado, atributos e eventos do provider podem ficar distribuídos entre spans
ativos diferentes da mesma trace: classe/operação e request estão presentes, assim como status e
response, mas não se exige que estejam todos no mesmo span. A migração deve preservar nomes,
valores e correlação; consolidá-los no span correto não é proibido.

## Condições para a migração

As Tasks 3.2 a 3.5 devem tratar essa capacidade como agregado de leitura e preservar tipos wrapper
nuláveis, listas, nomes JSON, paths, classificação de erros, seleção por property e telemetria. Não
há autorização nesta caracterização para corrigir o fallback silencioso de mock, mudar o path
lógico de observabilidade ou interpretar `ultima_alteracao` como data.
