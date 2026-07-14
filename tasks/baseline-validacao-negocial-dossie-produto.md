# Baseline - Registrar validacao negocial do dossie de produto

## Escopo

Manifesto executavel da Task 2.4a para a capacidade atomica
`RegistrarValidacaoNegocialDossieProduto`. A caracterizacao observa a cadeia legada ainda em
producao sem alterar classes de `src/main`, criar contratos internos ou iniciar a Task 2.4b.

O OpenAPI permanece integralmente gerado pelo Quarkus. Por decisao humana de 2026-07-13, o
artefato gerado nao foi testado, filtrado, complementado ou manipulado; o contrato publico foi
protegido no codigo Java e nos testes HTTP/JSON/validacao.

## Contrato REST publico

| Item | Baseline observado |
|---|---|
| Metodo e path | `PATCH /simtr-hub/v1/dossie-produto/{id}/validacao-negocial` |
| Sucesso | HTTP `200` sem corpo |
| Identificador | obrigatorio e maior que zero |
| Corpo | objeto obrigatorio; corpo ausente retorna HTTP `400` com `O corpo da requisicao deve ser informado.` |
| JSON | nomes snake_case preservados, incluindo ids, checklist, pareceres, garantia, produto e respostas de formulario |
| Validacao cascata | dez mensagens obrigatorias observadas em verificacao, parecer, produto e resposta de formulario |
| Nulabilidade | listas de topo, elementos de listas, objetos e campos sem `@NotNull` aceitam nulo; nenhuma normalizacao foi introduzida |

Oraculos independentes:

- `DossieProdutoApiContractTest` protege o request literal completo, HTTP 200 sem corpo e os dois
  cenarios de nulabilidade aceita;
- `DossieProdutoErroApiContractTest` protege identificador invalido, corpo ausente e todas as
  mensagens obrigatorias aninhadas;
- `ValidacaoNegocialDossieProdutoMtrContractTest` protege a travessia REST ate o stub MTR com
  simulador desabilitado.

## Contrato MTR v1

| Item | Baseline observado |
|---|---|
| Metodo e path | `PATCH /simtr/dossie-produto/v1/dossie-produto/{id}/validacao-negocial` |
| Request | mesma estrutura JSON publica apos o mapeamento legado REST -> VO -> DTO |
| Nulos no wire | propriedades nulas sao omitidas por `NON_NULL`; listas e elementos nulos permanecem |
| Response | HTTP `200` sem corpo; o Resource mantem HTTP `200` sem corpo |
| Headers | `Content-Type` e `Accept` JSON, `apikey`, `Authorization: Bearer` e `traceparent` |
| Erro de negocio | corpo MTR `400` volta lossless e nao sofre retry |
| Erro recuperavel | resposta MTR `500` sofre retry e reenvia o mesmo wire |

Para o request de nulabilidade caracterizado, o wire observado e:

```json
{
  "verificacoes": [
    null,
    {
      "identificador_checklist": 6592,
      "versao_checklist": 2,
      "analise_realizada": true,
      "parecer_apontamentos": [null],
      "garantia": {"clientes_avalistas": [null]}
    }
  ],
  "respostas_formulario": [
    null,
    {
      "campo_formulario": 1000011689,
      "opcoes_selecionadas": [null]
    }
  ]
}
```

`ValidacaoNegocialDossieProdutoMtrContractTest` executa cinco cenarios contra
`DossieProdutoMtrStubTestResource`, somente em localhost e com simulador desabilitado:

1. wire MTR v1 completo, headers, OIDC, trace e resposta vazia;
2. listas, elementos, objetos e campos nulos no wire;
3. erro de negocio completo sem retry;
4. retry depois de erro recuperavel com wire identico;
5. matriz de fault tolerance declarada no REST Client legado.

## Matriz de fault tolerance congelada

Metodo legado: `DossieProdutoClient#registrarValidacaoNegocialDossieProduto`.

| Politica | Valor atual |
|---|---|
| Timeout | `2000 ms` |
| Retry | `maxRetries=3`, `delay=300 ms`, `jitter=100 ms` |
| Retry on | `MtrServerErrorException`, `ProcessingException`, `TimeoutException` |
| Abort on | `MtrBusinessErrorException`, `MtrClientTechnicalException` |
| Circuit breaker | volume `10`, ratio `0.5`, delay `10000 ms`, sucessos `2` |
| Fail/skip | `failOn` igual a `retryOn`; `skipOn` igual a `abortOn` |

A operacao e mutavel. O retry foi preservado como risco conhecido; idempotencia nao foi corrigida
nem suposta nesta caracterizacao.

## Simulador e configuracao

- `simtr-hub.simulador.dossie-produto.habilitado=false` no default de producao;
- `%dev.simtr-hub.simulador.dossie-produto.habilitado=true` no profile `dev`;
- `simtr-hub.simulador.dossie-produto.habilitado=true` no profile padrao `test`;
- `DossieProdutoServiceTest` comprova selecao do mock quando habilitado e do gateway quando
  desabilitado;
- `MockFactoryTest` comprova a leitura da fixture
  `mock/dossieproduto/validacao-negocial-dossie-produto.md`, cujo JSON e `{}`;
- a configuracao principal do REST Client mantem connect timeout `3000 ms` e read timeout
  `10000 ms`;
- o stub troca somente a URL por localhost, aumenta o read timeout para `5000 ms` e desliga o
  simulador durante sua classe;
- o profile padrao `test` declara `quarkus.devservices.enabled=false`.

Os testes nao usaram Docker, Dev Services, rede externa, `QuarkusTestProfile` ou `@TestProfile`.

## Observabilidade congelada

Spans protegidos:

- `simtr-hub.api.dossie-produto.validacao-negocial.registrar`;
- `simtr-hub.service.dossie-produto.validacao-negocial.registrar`;
- `mtr.dossie-produto.validacao-negocial.registrar`.

Eventos de log protegidos incluem:

- `simtr-hub.dossie-produto.validacao-negocial.requisicao.recebida`;
- `simtr-hub.dossie-produto.validacao-negocial.service.iniciado`;
- `simtr-hub.dossie-produto.validacao-negocial.simulador.usado`;
- `simtr-hub.dossie-produto.validacao-negocial.service.concluido`;
- `simtr-hub.dossie-produto.validacao-negocial.resposta.enviada`;
- prefixo `mtr.dossie-produto.validacao-negocial.*` na chamada externa;
- eventos comuns `mtr.rest-client.request.enviada` e `mtr.rest-client.response.recebida`.

As evidencias executaveis permanecem em `ObservabilidadeSpansContratoTest`,
`ObservabilidadeLogsContratoTest` e no teste MTR contra o stub local.

## Validacao de containers

Os DTOs da validacao negocial ja usam `List<@Valid T>` para a cascata. Os warnings `HV000271`
observados na suite pertencem a contratos ainda legados de formulario e documento, nao a esta
capacidade. A futura migracao REST deve preservar as mesmas mensagens e nulabilidade sem inventar
novas restricoes.

## Evidencia de execucao

- testes HTTP focados: codigo 0;
- cinco testes MTR focados: codigo 0;
- testes focados de simulador, fixture, spans e logs: codigo 0;
- `mvn -q clean test`: codigo 0 em 2026-07-14;
- 224 elementos `testcase` em 56 relatorios Surefire, zero falhas, zero erros e zero ignorados;
- profile padrao `test`, sem Docker ou Dev Services;
- nenhuma classe de producao alterada pela Task 2.4a;
- `2.4b` ainda nao estava iniciada ao encerrar a caracterizacao.

Ao encerrar a Task 2.4a, o proximo item pendente era a Task 2.4b, que criaria o nucleo interno da
validacao negocial. Nenhum tipo, porta ou caso de uso dessa task foi criado durante a
caracterizacao.

## Evidencia do nucleo DDD

A Task 2.4b adicionou o nucleo interno sem ligar nenhuma borda:

- `ComandoRegistroValidacaoNegocialDossieProduto` carrega o identificador do dossie, verificacoes
  e respostas de formulario com nomes Java sem annotations de protocolo;
- parecer de apontamento, cliente avalista, produto, garantia, verificacao e resposta de formulario
  sao tipos internos separados;
- `RegistrarValidacaoNegocialDossieProduto` e a porta de entrada atomica;
- `SolicitarRegistroValidacaoNegocialDossieProduto` representa a necessidade da aplicacao na saida;
- `RegistrarValidacaoNegocialDossieProdutoCasoDeUso` delega reativamente e retorna `Uni<Void>` sem
  copiar, validar, normalizar, bloquear ou tratar a falha da porta.

O dominio usa somente Java. Mutiny `Uni` aparece apenas na aplicacao; Jackson, Jakarta, Quarkus,
REST, OpenTelemetry e contratos das bordas nao entram no nucleo. Listas, elementos, objetos e
campos nulos permanecem representaveis sem novas invariantes.

`RegistrarValidacaoNegocialDossieProdutoCasoDeUsoTest` comprova:

1. delegacao da mesma instancia do comando;
2. preservacao das mesmas listas e de elementos nulos em verificacoes, pareceres, avalistas,
   respostas e opcoes selecionadas;
3. conclusao com resultado `Void`;
4. propagacao da mesma falha da porta de saida.

O ciclo TDD registrou RED de compilacao pela ausencia da nova porta de saida e passou a GREEN apos
a implementacao minima. Os dois testes unitarios e os 13 casos de `ArchUnitProgressivoTest`
passaram no foco. `mvn -q clean test` terminou com codigo 0 em 2026-07-14: 226 elementos
`testcase` em 57 relatorios, zero falhas, zero erros e zero ignorados, sem Docker ou Dev Services.

Ao encerrar a Task 2.4b, o proximo item pendente era a Task 2.4c, que criaria a borda MTR da
validacao negocial. REST Client, DTO MTR, mapper, erro, qualifier e adapter exclusivos nao foram
iniciados durante a criacao do nucleo.

## Evidencia da borda MTR DDD

A Task 2.4c adicionou uma borda MTR v1 exclusiva, ainda sem liga-la ao caso de uso ou ao Resource:

- `ValidacaoNegocialDossieProdutoMtrRequest` representa a arvore externa com records aninhados e
  `NON_NULL`, sem reutilizar DTO REST publico;
- `ValidacaoNegocialDossieProdutoMtrClient` preserva `PATCH`, path v1, JSON, resposta vazia,
  headers, OIDC, propagacao de trace e a matriz FT congelada;
- `ValidacaoNegocialDossieProdutoMtrException` classifica negocio, tecnica cliente e servidor
  dentro da chamada interceptada pelo fault tolerance;
- `ValidacaoNegocialDossieProdutoMtrMapper` traduz tipos internos para o wire preservando listas,
  elementos, objetos e campos nulos;
- `FalhaRegistroValidacaoNegocialDossieProduto` conserva tipo, status, recurso, id, codigo,
  mensagens, detalhe e stacktrace externo sem depender de protocolo;
- `ValidacaoNegocialDossieProdutoMtrAdapter`, qualificado por `ValidacaoNegocialMtr`, implementa a
  porta de saida e traduz a falha somente depois da politica do client.

O adapter preserva o span `mtr.dossie-produto.validacao-negocial.registrar`, kind `CLIENT`, os
atributos MTR/HTTP/quantidades e os eventos
`mtr.dossie-produto.validacao-negocial.chamada.iniciada`, `.concluida` e `.falhou`. Logs nao
incluem payload, API key, token ou PII; usam somente os campos estruturados ja existentes.

Dois ciclos TDD registraram RED de compilacao antes dos tipos do client e antes do mapper/falha/
adapter. Depois do GREEN:

1. seis testes unitarios protegem classificacao/fallback do client, matriz FT, mapeamento
   completo, erro lossless e timeout;
2. quatro cenarios adicionais no contrato localhost exercitam diretamente o novo client/adapter,
   cobrindo wire completo, nulabilidade/`NON_NULL`, erro 400 sem retry e retry de 500 com wire
   identico;
3. API key sintetica, OIDC local, `traceparent`, resposta vazia e declaracao do span foram
   observados;
4. os 13 casos de `ArchUnitProgressivoTest` passaram;
5. `mvn -q clean test` terminou com codigo 0 em 2026-07-14: 236 elementos `testcase` em 59
   relatorios, zero falhas, zero erros e zero ignorados, sem Docker ou Dev Services.

O proximo item pendente e a Task 2.4d, que criara a borda de simulador e a selecao CDI. Nenhum
producer, DTO/mapper de simulador, fixture, caso de uso ou Resource foi ligado na Task 2.4c.

## Evidencia da borda simulador DDD

A Task 2.4d adicionou a borda de simulador e a selecao da porta de saida, sem migrar o Resource:

- `ValidacaoNegocialDossieProdutoSimuladorResponse` representa exclusivamente o objeto vazio `{}`
  da fixture atual;
- `ValidacaoNegocialDossieProdutoSimuladorMapper` converte essa resposta para o resultado interno
  `Void`, sem reutilizar DTO REST publico ou contrato MTR;
- `ValidacaoNegocialDossieProdutoSimuladorAdapter`, qualificado por
  `ValidacaoNegocialSimulador`, implementa `SolicitarRegistroValidacaoNegocialDossieProduto`;
- o adapter preserva `simtr_hub.origem_dados=mock`, o evento
  `simtr-hub.dossie-produto.validacao-negocial.simulador.usado` e seus campos estruturados;
- `ValidacaoNegocialDossieProdutoPortasProducer` preserva
  `simtr-hub.simulador.dossie-produto.habilitado` e seleciona simulador ou MTR sem condicional no
  caso de uso.

O primeiro teste focado registrou RED de compilacao somente pela ausencia dos cinco componentes.
Depois do incremento minimo, 14 casos focados comprovaram fixture real, resultado vazio, erro
explicito de fixture ausente, producer, bootstrap CDI e selecao com a property ligada e desligada.
Os contratos de spans/logs e os 13 guardrails ArchUnit passaram. `mvn -q clean test` terminou com
codigo 0 em 2026-07-14: 241 elementos `testcase` em 62 relatorios, zero falhas, zero erros e zero
ignorados, no profile `test`, sem Docker ou Dev Services. Nenhuma property ou fixture foi alterada;
o caso de uso concreto continua fora do CDI, o Resource continua na cadeia legada e a Task 2.4e
nao foi iniciada.

## Evidencia da borda REST DDD

A Task 2.4e concluiu a migracao vertical da validacao negocial:

- `ValidacaoNegocialDossieProdutoRequest` preserva o JSON publico e aplica `@Valid` nos tipos dos
  dois containers, sem novas restricoes;
- `ValidacaoNegocialDossieProdutoRestMapper` traduz toda a arvore REST para o comando interno e a
  falha interna para as excecoes publicas, preservando listas, elementos, objetos, campos nulos e
  todos os dados do erro;
- `ValidacaoNegocialDossieProdutoObservabilidade` liga a porta selecionada ao caso de uso e mantem
  os spans, eventos e campos estruturados ja contratados;
- `DossieProdutoResource` depende somente da porta de entrada para esta capacidade e continua
  respondendo HTTP `200` sem corpo no mesmo path;
- a cadeia exclusiva de fachada, service, gateway, client, exception mapper, mock factory e
  MapStruct mapper foi removida depois da matriz verde e de `rg` sem consumidores;
- o DTO REST de topo legado e os sete VOs antigos foram removidos; os seis DTOs REST aninhados,
  a fixture, as properties e a matriz FT foram preservados.

O ciclo TDD registrou RED de compilacao quando o teste passou a exigir o novo request e a nova
porta. Depois do GREEN, contratos HTTP positivos e negativos, wire MTR, erros lossless, retry,
simulador ligado/desligado, logs, spans e os 13 guardrails ArchUnit passaram. O warning
`HV000271` deixou de apontar a validacao negocial; os avisos restantes pertencem aos contratos de
formulario e documento.

`mvn -q clean test` terminou com codigo 0 em 2026-07-14: 236 testes em 62 relatorios, zero falhas,
zero erros e zero ignorados, no profile `test`, sem Docker ou Dev Services. Cinco testes novos de
mapper REST e observabilidade substituem dez testes exclusivos da cadeia removida. OpenAPI,
properties e fixture nao foram alterados.

O proximo item pendente e o Checkpoint C2.4. A Task 2.5 nao pode iniciar antes do GO humano ser
registrado na tabela autoritativa de `todo.md`.
