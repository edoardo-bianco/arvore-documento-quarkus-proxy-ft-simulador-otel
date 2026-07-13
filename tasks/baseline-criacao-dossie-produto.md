# Baseline da criacao de dossie produto

## Escopo

Manifesto da Task 2.1a para `CriarDossieProduto`. A caracterizacao observa a implementacao legada
antes da migracao estrutural e nao altera contrato, regra funcional, endpoint ou politica de
resiliencia.

## Contrato REST e validacao

- `POST /simtr-hub/v1/dossie-produto`.
- Request JSON protegido por literal independente, com `processo`, `chave_correlacao_canal`,
  `numero_negocio` e a lista aninhada de clientes.
- Sucesso: `201 application/json` com o identificador criado.
- Corpo ausente e campos obrigatorios invalidos retornam o erro publico caracterizado; JSON
  malformado retorna `400` com corpo vazio, conforme comportamento atual.
- O OpenAPI e gerado exclusivamente pelo Quarkus. Por decisao humana de 2026-07-13, o artefato
  gerado nao e testado, filtrado, complementado ou manipulado; o contrato publico permanece
  protegido pelos testes Java e HTTP/JSON/validacao dos Resources.

## Borda MTR

- Wire: `POST /simtr/dossie-produto/v1/dossie-produto`, preservando estruturalmente o JSON.
- Headers: `Content-Type` e `Accept` JSON, `apikey`, bearer token obtido pelo OIDC Client e
  `traceparent` W3C.
- Sucesso MTR `201 {"id":987}` atravessa a borda e permanece equivalente no REST publico.
- Erro de negocio `400` e lossless, com uma unica tentativa.
- Erro recuperavel `500` executa retry e reenvia exatamente o mesmo wire.
- Tentativa acima de 2.000 ms e cancelada e seguida por retry.

## Simulador e configuracao

- `simtr-hub.simulador.dossie-produto.habilitado=true` seleciona a mock factory; `false` seleciona
  o gateway MTR.
- Configuracao principal: `false`; `%dev`: `true`; ambiente padrao `test`: `true`.
- O `QuarkusTestResourceLifecycleManager` do contrato MTR sobrescreve a propriedade para `false` e
  fornece URLs localhost efemeras para REST Client e token OIDC.
- Todos os testes Quarkus usam o profile padrao `test`; nao existem `QuarkusTestProfile` ou
  `@TestProfile`.
- REST Client `dossie-produto`: connect timeout de 3.000 ms e read timeout de 10.000 ms na
  configuracao principal; os valores reduzidos do ambiente de teste nao acessam rede externa.

## Fault tolerance congelada

No metodo `DossieProdutoClient#criarDossieProduto`:

- timeout: 2.000 ms;
- retry: 3 retries, delay 300 ms e jitter 100 ms;
- retry/fail: `MtrServerErrorException`, `ProcessingException` e `TimeoutException`;
- abort/skip: `MtrBusinessErrorException` e `MtrClientTechnicalException`;
- circuit breaker: volume 10, failure ratio 0,5, delay 10.000 ms e success threshold 2.

A operacao e mutavel. A caracterizacao preserva o retry atual, mas nao assume idempotencia nem
autoriza novo workflow.

## Observabilidade

- spans protegidos: `simtr-hub.api.dossie-produto.criar`,
  `simtr-hub.service.dossie-produto.criar` e `mtr.dossie-produto.criar`;
- o span do REST Client protege classe `DossieProdutoClient`, operacao `criarDossieProduto`, status
  e eventos de request/response;
- logs protegem recebimento, inicio/conclusao/falha do service, uso do simulador e envio da
  resposta, com os campos estruturados atuais.

## Evidencias executaveis

- `DossieProdutoApiContractTest#preservaContratoDeCriacao`;
- testes de criacao em `DossieProdutoErroApiContractTest`;
- `DossieProdutoServiceTest#criacaoComSimuladorHabilitadoUsaMockFactory` e
  `#criacaoComSimuladorDesabilitadoUsaGateway`;
- seis testes em `DossieProdutoMtrContractTest`, incluindo a matriz declarada de fault tolerance;
- `ObservabilidadeSpansContratoTest` e `ObservabilidadeLogsContratoTest`.

Verificacao em 2026-07-12:

- `mvn -q clean test`: 154 elementos `testcase`, zero falhas, zero erros e zero ignorados;
- a tentativa anterior com filtro `-Dtest` falhou somente no discovery do engine ArchUnit com o
  classloader Quarkus; apos `clean`, a suite padrao passou integralmente;
- nenhum arquivo de producao foi alterado na Task 2.1a.

## Borda MTR exclusiva - Task 2.1c.1

- Request e response MTR proprios nao reutilizam DTO REST, DTO do simulador ou modelo interno.
- O novo client preserva config key, path, providers de headers/OIDC/observabilidade e matriz FT.
- O erro proprio classifica negocio, tecnico de cliente e servidor sem perder o payload externo.
- Quatro testes cobrem wire, bearer token, traceparent, sucesso, erro 400 e retry apos 500.
- `mvn -q clean test` passou; contratos Java, HTTP e de integracao permaneceram verdes.

## Adapter MTR - Task 2.1c.2

- O mapper converte comando e resultado internos sem importar DTO REST ou contrato do simulador.
- O adapter implementa `SolicitarCriacaoDossieProduto` com qualifier proprio e preserva spans,
  eventos e atributos da borda MTR existente.
- A excecao externa e traduzida depois da politica FT para `FalhaCriacaoDossieProduto`, mantendo
  status, recurso, identificador, codigo, mensagens, detalhe e stacktrace externo.
- Tres testes unitarios cobrem sucesso, traducao lossless e timeout; a suite limpa completa passou.

## Borda simulador - Task 2.1d

- O adapter simulador implementa `SolicitarCriacaoDossieProduto` com qualifier exclusivo e le o
  fixture `mock/dossieproduto/criacao-basica-dossie-produto.md` em DTO proprio.
- O comportamento legado foi preservado: a resposta usa o `id` do fixture, registra origem `mock`
  no span e emite `simtr-hub.dossie-produto.simulador.usado` com os campos estruturados existentes.
- Um producer CDI seleciona simulador ou MTR pela propriedade
  `simtr-hub.simulador.dossie-produto.habilitado`; o caso de uso permanece sem condicional.
- Dois `@QuarkusTest`, ambos no profile padrao `test`, comprovam os caminhos habilitado e
  desabilitado. O caminho desabilitado atravessa o adapter MTR, OIDC Client e stub HTTP.
- `mvn -q clean test` terminou com codigo 0 em 2026-07-12; CDI, ArchUnit e contratos legados
  permaneceram verdes.

## Borda REST - Task 2.1e

- `DossieProdutoResource` recebe `CriacaoDossieProdutoRequest`, chama somente a porta de entrada
  `CriarDossieProduto` e devolve `CriacaoDossieProdutoResponse`, preservando path, status, JSON,
  validacao, erros e sinais de observabilidade da criacao.
- `CriacaoDossieProdutoRestMapper` traduz request para comando, resultado para response e
  `FalhaCriacaoDossieProduto` para o erro REST lossless existente.
- A criacao foi removida de `DossieProdutoFachada`, `DossieProdutoService`,
  `DossieProdutoGateway`, `DossieProdutoClient`, `DossieProdutoMapper`, mock factory e dos DTO/VO
  antigos; buscas finais nao encontraram referencias remanescentes nesses componentes.
- O scanner de spans examina `CriacaoDossieProdutoMtrAdapter` e
  `WorkflowDossieProdutoMtrAdapter`; o teste focado passou no estado definitivo.
- A investigacao mostrou que o antigo fingerprint OpenAPI incluia uma operacao do REST Client
  interno. Por orientacao humana, `OpenApiContractTest` foi removido e o documento voltou a ficar
  integralmente sob responsabilidade do Quarkus, sem arquivo estatico, filtro,
  `@Operation(hidden = true)` ou atualizacao de hash.
- Os dois casos removidos testavam somente o artefato gerado pelo Quarkus. Contratos de codigo,
  HTTP/JSON/validacao, MTR/simulador, erros, fault tolerance, CDI, ArchUnit e observabilidade
  continuam cobertos e verdes.
- Todos os testes Quarkus usam o profile padrao `test`, sem Docker, Dev Services,
  `QuarkusTestProfile` ou `@TestProfile`.
- `mvn -q clean test` terminou com codigo 0 em 2026-07-13: 163 elementos `testcase`, zero falhas,
  zero erros e zero ignorados; o teste HTTP da criacao inclui a resposta nula preservada do contrato
  legado.
