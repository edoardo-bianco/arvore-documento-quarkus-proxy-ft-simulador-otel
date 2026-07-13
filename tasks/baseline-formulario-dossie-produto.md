# Baseline - Atualizar formulario do dossie de produto

## Escopo

Manifesto executavel da Task 2.2a para a capacidade atomica
`AtualizarFormularioDossieProduto`. A caracterizacao observa o legado ainda em producao sem alterar
classes de `src/main` e sem introduzir contrato, regra funcional ou endpoint novo.

O OpenAPI permanece integralmente gerado pelo Quarkus. Por decisao humana de 2026-07-13, o
artefato gerado nao e testado, filtrado, complementado ou manipulado; o contrato publico desta
capacidade e protegido no codigo Java e nos testes HTTP/JSON/validacao.

## Contrato REST publico

| Item | Baseline observado |
|---|---|
| Metodo e path | `PATCH /simtr-hub/v1/dossie-produto/{id}/formulario` |
| Sucesso | HTTP `201`, `Content-Type: application/json` e `{"id":123}` no simulador |
| Identificador | obrigatorio e maior que zero |
| Corpo | lista obrigatoria; corpo ausente retorna HTTP `400` com `O corpo da requisicao deve ser informado.` |
| JSON | `vinculo_dossie`, `respostas_formulario`, `campo_formulario` e `opcoes_selecionadas` em snake_case |
| Nulabilidade | elementos nulos, objetos vazios e campos internos nulos sao aceitos; campos nulos sao omitidos na serializacao, mas elementos nulos das listas permanecem |
| Campos internos | nao possuem novas obrigatoriedades; a refatoracao nao deve inventar invariantes |

Oraculos independentes:

- `DossieProdutoApiContractTest#preservaContratoDeAtualizacaoDeFormulario` protege request literal,
  status e response publicos;
- `DossieProdutoErroApiContractTest#preservaErroParaIdInvalidoNoFormulario` protege a mensagem do
  identificador;
- `DossieProdutoErroApiContractTest#preservaErroParaCorpoDeFormularioAusente` protege o corpo
  obrigatorio;
- `FormularioDossieProdutoMtrContractTest#preservaListasElementosECamposNulosNoWireDoFormulario`
  protege listas, elementos e campos nulos pela travessia REST ate o stub MTR.

## Contrato MTR v1

| Item | Baseline observado |
|---|---|
| Metodo e path | `PATCH /simtr/dossie-produto/v1/dossie-produto/{id}/formulario` |
| Request | mesma estrutura JSON observavel do contrato atual, apos o mapeamento legado REST -> VO -> DTO |
| Response | JSON `{"id":<long>}`; o Resource converte o sucesso para HTTP `201` |
| Headers | `Content-Type` e `Accept` JSON, `apikey`, `Authorization: Bearer` e `traceparent` |
| Erro de negocio | resposta MTR `400` volta lossless e nao sofre retry |
| Erro recuperavel | resposta MTR `500` sofre retry e reenvia o mesmo wire |

`FormularioDossieProdutoMtrContractTest` executa cinco cenarios contra
`DossieProdutoMtrStubTestResource`, somente em localhost e com simulador desabilitado:

1. wire completo, headers e resposta;
2. listas, elementos e campos nulos;
3. erro de negocio completo sem retry;
4. retry apos erro recuperavel com wire identico;
5. matriz de fault tolerance declarada no REST Client legado.

## Matriz de fault tolerance congelada

Metodo legado: `DossieProdutoClient#atualizarFormularioDossieProduto`.

| Politica | Valor atual |
|---|---|
| Timeout | `2000 ms` |
| Retry | `maxRetries=3`, `delay=300 ms`, `jitter=100 ms` |
| Retry on | `MtrServerErrorException`, `ProcessingException`, `TimeoutException` |
| Abort on | `MtrBusinessErrorException`, `MtrClientTechnicalException` |
| Circuit breaker | volume `10`, ratio `0.5`, delay `10000 ms`, sucessos `2` |
| Fail/skip | `failOn` igual a `retryOn`; `skipOn` igual a `abortOn` |

A operacao e mutavel. O retry e preservado como risco conhecido; idempotencia nao e corrigida nem
suposta nesta refatoracao.

## Simulador e configuracao

- `simtr-hub.simulador.dossie-produto.habilitado=false` no default de producao;
- `%dev.simtr-hub.simulador.dossie-produto.habilitado=true` no profile `dev`;
- `simtr-hub.simulador.dossie-produto.habilitado=true` no profile padrao `test`;
- `DossieProdutoServiceTest` comprova selecao do mock quando habilitado e do gateway quando
  desabilitado;
- `MockFactoryTest` comprova que a fixture devolve o id do path;
- `quarkus.rest-client.dossie-produto.url`, connect timeout e read timeout permanecem inalterados.

Os testes usam somente o profile `test`, sem Docker, Dev Services, `QuarkusTestProfile` ou
`@TestProfile`.

## Observabilidade congelada

Spans:

- `simtr-hub.api.dossie-produto.formulario.atualizar`;
- `simtr-hub.service.dossie-produto.formulario.atualizar`;
- `mtr.dossie-produto.formulario.atualizar`.

Eventos de log protegidos incluem:

- `simtr-hub.dossie-produto.formulario.requisicao.recebida`;
- `simtr-hub.dossie-produto.formulario.service.iniciado`;
- `simtr-hub.dossie-produto.formulario.simulador.usado`;
- `simtr-hub.dossie-produto.formulario.service.concluido`;
- `simtr-hub.dossie-produto.formulario.resposta.enviada`;
- prefixo `mtr.dossie-produto.formulario.*` na chamada externa.

As evidencias executaveis permanecem em `ObservabilidadeSpansContratoTest` e
`ObservabilidadeLogsContratoTest`.

## Divida observada, sem correcao nesta task

O Hibernate Validator avisa sobre `@Valid` aplicado ao container `List`. A borda REST continua
usando Jakarta Validation para rejeitar entradas invalidas antes do caso de uso e preservar status,
mensagens e paths do contrato atual. A migracao futura deve avaliar `List<@Valid T>` preservando
esse comportamento; dominio e aplicacao permanecem sem annotations de validacao.

## Evidencia de execucao

- testes focados `FormularioDossieProdutoMtrContractTest,DossieProdutoErroApiContractTest`: codigo
  0;
- `mvn -q clean test`: codigo 0 em 2026-07-13;
- 169 elementos `testcase`, zero falhas, zero erros e zero ignorados em 38 relatórios Surefire;
- nenhuma classe de producao alterada pela Task 2.2a.

## Evidencia da borda MTR DDD

A Task 2.2c adicionou contratos MTR exclusivos sob
`dossieproduto.adaptador.saida.mtr.dto.v1.formulario`, REST Client e excecao protocolares proprios,
mapper para os tipos internos e adapter da porta `SolicitarAtualizacaoFormularioDossieProduto`.
Nenhum DTO REST publico ou artefato do simulador e reutilizado nessa borda.

Evidencias executaveis:

- `FormularioDossieProdutoMtrClientTest`: classificacao/fallback de erro e matriz FT;
- `FormularioDossieProdutoMtrAdapterTest`: mapeamento completo/nulos, resultado, erro lossless e
  timeout;
- `FormularioDossieProdutoMtrContractTest`: novo client/adapter contra o mesmo stub localhost,
  cobrindo wire, headers/OIDC/trace, listas e nulos, erro 400 sem retry e retry de 500 com wire
  identico;
- `ArchUnitProgressivoTest`: 13 regras e provas executadas como JUnit puro sobre ArchUnit core,
  sem o engine que disputava o classloader dos `@QuarkusTest`.

`mvn -q clean test` terminou com codigo 0 em 2026-07-13: 188 elementos `testcase` em 41 relatorios,
zero falhas, zero erros e zero ignorados, usando o profile padrao `test`, sem Docker ou Dev Services.

## Evidencia da borda simulador DDD

A Task 2.2d adicionou `FormularioDossieProdutoSimuladorResponse`, o adapter qualificado por
`@FormularioSimulador` e `FormularioDossieProdutoPortasProducer`. A borda le a fixture existente
`mock/dossieproduto/formulario-dossie-produto.md` com modelo proprio, sem importar DTO REST publico
ou a `DossieProdutoMockFactory`. Como no legado, a fixture e lida primeiro; seu `id=1` e usado
quando nao ha identificador na chamada, e o identificador informado prevalece quando presente.

A configuracao nao foi alterada:

- default de producao: simulador desabilitado;
- profile `dev`: simulador habilitado;
- profile padrao `test`: simulador habilitado.

Evidencias executaveis:

- `FormularioDossieProdutoPortasProducerTest` prova diretamente a selecao ligada/desligada;
- `FormularioDossieProdutoSelecaoSimuladorQuarkusTest` prova bootstrap CDI, selecao habilitada,
  leitura da fixture e precedencia do identificador no profile padrao `test`;
- `FormularioDossieProdutoMtrContractTest`, cujo recurso localhost desabilita o simulador, prova
  que a porta nao qualificada seleciona o novo adapter MTR, mantendo o adapter simulador acessivel
  somente pelo seu qualifier;
- `ArchUnitProgressivoTest` permaneceu verde.

`mvn -q clean test` terminou com codigo 0 em 2026-07-13: 191 elementos `testcase` em 43 relatorios,
zero falhas, zero erros e zero ignorados. Nao foram usados Docker, Dev Services,
`QuarkusTestProfile` ou `@TestProfile`; o OpenAPI continuou sob geracao exclusiva do Quarkus.

## Evidencia da borda REST DDD

A Task 2.2e ligou `DossieProdutoResource` diretamente a porta de entrada
`AtualizarFormularioDossieProduto`. `FormularioDossieProdutoRestMapper` traduz o DTO REST para o
comando interno, o resultado para `DossieProdutoCriadoDto` e as falhas internas para as excecoes
publicas existentes, preservando listas, elementos, objetos e campos nulos.

`FormularioDossieProdutoObservabilidade` decora a porta de entrada e preserva os spans, atributos
e eventos de log da antiga camada de servico. O scanner agora reconhece o span MTR em
`FormularioDossieProdutoMtrAdapter`; o Resource conserva path, metodo, status, validacoes e
anotacoes REST publicas. O OpenAPI permanece gerado exclusivamente pelo Quarkus: nao ha teste do
documento gerado, arquivo estatico, filtro, hash ou `@Operation(hidden = true)`.

A cadeia exclusiva substituida foi removida de `DossieProdutoFachada`, `DossieProdutoService`,
`DossieProdutoGateway`, `DossieProdutoClient`, `DossieProdutoMapper` e
`DossieProdutoMockFactory`. Os sete VOs antigos do formulario/criado tambem foram eliminados, e
busca estatica confirmou ausencia de referencias remanescentes. Documento e validacao negocial
continuam temporariamente na cadeia legada, fora do escopo desta capacidade.

Evidencias executaveis:

- `FormularioDossieProdutoRestMapperTest` cobre mapeamento completo, nulabilidade, resultado e
  erros lossless;
- `FormularioDossieProdutoObservabilidadeTest` cobre delegacao e propagacao da mesma falha;
- `DossieProdutoApiContractTest`, `DossieProdutoErroApiContractTest` e
  `FormularioDossieProdutoMtrContractTest` percorrem HTTP e MTR em localhost;
- `ObservabilidadeSpansContratoTest`, `ObservabilidadeLogsContratoTest` e
  `ArchUnitProgressivoTest` protegem observabilidade e limites arquiteturais;
- `ResourceBeanCoverageTest` prova que o formulario do Resource depende da porta de entrada.

O profile padrao `test` declara `quarkus.devservices.enabled=false`; simuladores e URLs localhost
continuam configurados em `src/test/resources/application.properties`. Assim os testes nao usam
Docker nem Dev Services e nao dependem de rede externa. Nao existem `QuarkusTestProfile` ou
`@TestProfile`.

`mvn -q clean test` terminou com codigo 0 em 2026-07-13: 191 elementos `testcase` em 45 relatorios,
zero falhas, zero erros e zero ignorados. A matriz focada da 2.2e executou 79 testes com codigo 0;
`git diff --check` passou. O checkpoint C2.2 permanece pendente de revisao e GO humano.
