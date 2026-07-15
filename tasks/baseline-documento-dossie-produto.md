# Baseline - Incluir documento no dossie de produto

> Documento historico da capacidade antes e durante a migracao. O estado final esta em
> `equivalencia-final.md`; evidencias quantitativas permanecem somente no JaCoCo.

## Escopo

Manifesto executavel da Task 2.3a para a capacidade atomica
`IncluirDocumentoDossieProduto`. A caracterizacao observa a cadeia legada ainda em producao sem
alterar classes de `src/main` e sem introduzir contrato, regra funcional ou endpoint novo.

O OpenAPI permanece integralmente gerado pelo Quarkus. Por decisao humana de 2026-07-13, o
artefato gerado nao e testado, filtrado, complementado ou manipulado; o contrato publico desta
capacidade e protegido no codigo Java e nos testes HTTP/JSON/validacao.

## Contrato REST publico

| Item | Baseline observado |
|---|---|
| Metodo e path | `POST /simtr-hub/v1/dossie-produto/{id}/documento` |
| Sucesso | HTTP `201`, `Content-Type: application/json` e `{"id_documento":456,"id_instancia_documento":789}` no simulador |
| Identificador | obrigatorio e maior que zero |
| Corpo | objeto obrigatorio; corpo ausente retorna HTTP `400` com `O corpo da requisicao deve ser informado.` |
| JSON | `path_storage`, `codigo_ged`, `object_store_ged`, `tipo_documento`, `vinculo_dossie`, `elemento_conteudo`, `cliente_avalista`, `opcoes_selecionadas`, `id_documento` e `id_instancia_documento` em snake_case |
| Validacao cascata | atributo e propriedade nao nulos exigem `chave` e `valor`; a mensagem observada para chave de atributo ausente e preservada |
| Nulabilidade | campos e objetos internos nulos, listas nulas e elementos nulos nas listas sao aceitos; campos nulos sao omitidos no wire MTR, mas elementos nulos permanecem |
| Campos internos | nao possuem novas obrigatoriedades; a refatoracao nao deve inventar invariantes |

Oraculos independentes:

- `DossieProdutoApiContractTest#preservaContratoDeInclusaoDeDocumento` protege request literal,
  status e response publicos;
- `DossieProdutoErroApiContractTest#preservaErroParaIdInvalidoNaInclusaoDeDocumento` protege a
  mensagem do identificador;
- `DossieProdutoErroApiContractTest#preservaErroParaCorpoDeDocumentoAusente` protege o corpo
  obrigatorio;
- `DossieProdutoErroApiContractTest#preservaErroDeValidacaoCascataDoAtributoDeDocumento` protege a
  validacao cascata;
- `DocumentoDossieProdutoMtrContractTest#preservaListasElementosECamposNulosAceitosNoWireDoDocumento`
  protege campos, objetos, listas e elementos nulos pela travessia REST ate o stub MTR.

## Contrato MTR v2

| Item | Baseline observado |
|---|---|
| Metodo e path | `POST /simtr/dossie-produto/v2/dossie-produto/{id}/documento` |
| Request | mesma estrutura JSON publica apos o mapeamento legado REST -> VO -> DTO; propriedades nulas sao omitidas por `NON_NULL` |
| Response | `id_documento` e `id_instancia_documento`; o Resource mantem HTTP `201` |
| Headers | `Content-Type` e `Accept` JSON, `apikey`, `Authorization: Bearer` e `traceparent` |
| Erro de negocio | resposta MTR `400` volta lossless e nao sofre retry |
| Erro recuperavel | resposta MTR `500` sofre retry e reenvia o mesmo wire |

Para o request composto somente por nulos aceitos, o wire observado e:

```json
{
  "vinculo_dossie": {
    "garantia": {"cliente_avalista": [null]}
  },
  "atributos": [null],
  "propriedades": [null]
}
```

`DocumentoDossieProdutoMtrContractTest` executa cinco cenarios contra
`DossieProdutoMtrStubTestResource`, somente em localhost e com simulador desabilitado:

1. wire MTR v2 completo, headers e resposta;
2. listas, elementos, objetos e campos nulos aceitos;
3. erro de negocio completo sem retry;
4. retry apos erro recuperavel com wire identico;
5. matriz de fault tolerance declarada no REST Client legado.

## Matriz de fault tolerance congelada

Metodo legado: `DossieProdutoClient#incluirDocumentoDossieProduto`.

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
- `MockFactoryTest` comprova a fixture `mock/dossieproduto/documento-dossie-produto.md` com ids
  `456` e `789`;
- a configuracao de producao do REST Client permanece com connect timeout `3000 ms` e read timeout
  `10000 ms`;
- o stub de contrato troca somente a URL por localhost, aumenta o read timeout para `5000 ms` e
  desliga o simulador durante sua classe.

O profile padrao `test` declara `quarkus.devservices.enabled=false`. Os testes nao usam Docker,
Dev Services, rede externa, `QuarkusTestProfile` ou `@TestProfile`.

## Observabilidade congelada

Spans protegidos:

- `simtr-hub.api.dossie-produto.documento.incluir`;
- `simtr-hub.service.dossie-produto.documento.incluir`;
- `mtr.dossie-produto.documento.incluir`.

Eventos de log protegidos incluem:

- `simtr-hub.dossie-produto.documento.requisicao.recebida`;
- `simtr-hub.dossie-produto.documento.service.iniciado`;
- `simtr-hub.dossie-produto.documento.simulador.usado`;
- `simtr-hub.dossie-produto.documento.service.concluido`;
- `simtr-hub.dossie-produto.documento.resposta.enviada`;
- prefixo `mtr.dossie-produto.documento.*` na chamada externa;
- eventos comuns `mtr.rest-client.request.enviada` e `mtr.rest-client.response.recebida`.

As evidencias executaveis permanecem em `ObservabilidadeSpansContratoTest`,
`ObservabilidadeLogsContratoTest` e no teste MTR contra o stub local.

## Divida observada na task, resolvida na Fase 11

Naquele momento, o Hibernate Validator avisava sobre `@Valid` aplicado aos containers `List`. A
borda REST continuava usando Jakarta Validation para rejeitar entradas invalidas antes do caso de
uso e preservar status, mensagens e paths do contrato. A migracao posterior deveria avaliar
`List<@Valid T>` somente com equivalencia comprovada.

Esse registro descreve o estado historico da task. A Fase 11 adotou `List<@Valid T>` depois de
proteger as quatro mensagens de atributos/propriedades e a nulabilidade, eliminando o aviso sem
alterar o comportamento publico.

## Evidencia de execucao

- testes focados de contrato HTTP, MTR v2, simulador, mapper e observabilidade: codigo 0;
- `mvn -q clean test`: codigo 0 em 2026-07-13;
- 197 elementos `testcase` em 46 relatorios Surefire, zero falhas, zero erros e zero ignorados;
- profile padrao `test`, com Docker e Dev Services desabilitados;
- nenhuma classe de producao alterada pela Task 2.3a.

O proximo item e a Task 2.3b, que criara o nucleo interno da inclusao de documento. Nenhuma
implementacao dessa task foi iniciada durante a caracterizacao.

## Evidencia do nucleo DDD

A Task 2.3b adicionou o nucleo interno sem ligar nenhuma borda:

- `ComandoInclusaoDocumentoDossieProduto` carrega o identificador do dossie e os dados do
  documento com nomes Java sem annotations de protocolo;
- cliente, garantia, vinculo, atributo e propriedade sao tipos internos separados;
- `ResultadoInclusaoDocumentoDossieProduto` representa os dois ids como
  `identificadorDocumento` e `identificadorInstanciaDocumento`, sem nomes JSON;
- `IncluirDocumentoDossieProduto` e a porta de entrada atomica;
- `SolicitarInclusaoDocumentoDossieProduto` representa a necessidade da aplicacao na saida;
- `IncluirDocumentoDossieProdutoCasoDeUso` delega reativamente sem copiar, validar, normalizar ou
  bloquear.

O dominio usa somente Java. Mutiny `Uni` aparece apenas na aplicacao, conforme a regra
arquitetural. Jackson, Jakarta, Quarkus, REST, OpenTelemetry e contratos das bordas nao entram no
nucleo.

`IncluirDocumentoDossieProdutoCasoDeUsoTest` e JUnit puro e comprova:

1. delegacao da mesma instancia do comando;
2. preservacao das mesmas listas e de elementos nulos em atributos, propriedades e avalistas;
3. retorno da mesma instancia do resultado interno;
4. propagacao da mesma falha da porta de saida.

O ciclo TDD registrou falha de compilacao RED pela ausencia dos novos tipos e passou a GREEN apos
a implementacao minima. O teste unitario e os 13 casos de `ArchUnitProgressivoTest` passaram no
foco. `mvn -q clean test` terminou com codigo 0 em 2026-07-13: 199 elementos `testcase` em 47
relatorios, zero falhas, zero erros e zero ignorados, sem Docker ou Dev Services.

O proximo item e a Task 2.3c, que criara a borda MTR v2. REST Client, DTO MTR, mapper, erro,
qualifier e adapter dessa borda nao foram iniciados na Task 2.3b.

## Evidencias da Task 2.3c - borda MTR v2

A borda externa passou a ter tipos exclusivos da capacidade:

- `DocumentoDossieProdutoMtrRequest` representa o wire v2 e mantem os nomes JSON
  `path_storage`, `codigo_ged`, `object_store_ged`, `tipo_documento`, `vinculo_dossie`,
  `elemento_conteudo`, `codigo_bacen`, `produto_operacao`, `produto_modalidade`,
  `cliente_avalista` e `opcoes_selecionadas`;
- `DocumentoDossieProdutoMtrResponse` preserva `id_documento` e
  `id_instancia_documento`;
- `DocumentoDossieProdutoMtrException` le o corpo externo completo e classifica negocio,
  tecnica de cliente e indisponibilidade do servidor;
- `DocumentoDossieProdutoMtrClient` chama
  `POST /dossie-produto/v2/dossie-produto/{id}/documento` e conserva timeout, retry e circuit
  breaker da cadeia legada;
- `DocumentoDossieProdutoMtrMapper` converte somente entre modelo interno e protocolo MTR,
  preservando campos, listas e elementos nulos;
- `DocumentoDossieProdutoMtrAdapter`, qualificado por `@DocumentoMtr`, implementa
  `SolicitarInclusaoDocumentoDossieProduto` e traduz a falha de protocolo para
  `FalhaInclusaoDocumentoDossieProduto` sem perder status, recurso, id, codigo, mensagens,
  detalhe ou stacktrace externo.

Os testes JUnit puros do client e do adapter cobrem seis cenarios: classificacao e fallback de
erro, matriz FT declarada, mapeamento completo com nulos, os dois ids de resposta, traducao
lossless e timeout apos a politica do client. O ciclo TDD registrou RED de compilacao antes de
cada implementacao e passou a GREEN com a borda minima.

`DocumentoDossieProdutoMtrContractTest` passou a executar nove cenarios. Os quatro cenarios da
nova borda comprovaram contra `HttpServer` local:

1. metodo, path, JSON, `Content-Type`, `Accept`, `apikey`, token OIDC e `traceparent`;
2. omissao de campos nulos com preservacao de listas e elementos nulos;
3. corpo 400 completo, classificado como negocio e sem retry;
4. retry de 500 para 201 repetindo exatamente o mesmo wire.

O adapter preserva o span `mtr.dossie-produto.documento.incluir`, seus atributos e os eventos de
log existentes. `ObservabilidadeSpansContratoTest` reconhece o adapter novo e continua
reconhecendo temporariamente o gateway legado, que somente sera removido na Task 2.3e. Os 13
casos de `ArchUnitProgressivoTest` passaram e confirmaram que a nova borda nao reutiliza DTO REST
publico.

A primeira tentativa de `mvn -q clean test` encontrou a instabilidade intermitente ja registrada
do discovery Quarkus/MapStruct, sem executar a suite. A repeticao do mesmo comando terminou com
codigo 0 em 2026-07-13: 209 elementos `testcase` em 49 relatorios, zero falhas, zero erros e zero
ignorados. O profile permaneceu `test`, `quarkus.devservices.enabled=false`, sem Docker,
`QuarkusTestProfile` ou `@TestProfile`.

O proximo item e a Task 2.3d, que criara a borda de simulador da inclusao de documento. O caso de
uso e o endpoint REST ainda nao selecionam a nova borda MTR nesta task.

## Evidencias da Task 2.3d - borda simulador

`DocumentoDossieProdutoSimuladorResponse` e exclusivo da borda simulador e declara explicitamente
os campos snake_case `id_documento` e `id_instancia_documento`. A fixture existente
`mock/dossieproduto/documento-dossie-produto.md` nao foi alterada e continua fornecendo os ids
456 e 789. `DocumentoDossieProdutoSimuladorAdapter` realiza o fluxo
`JSON -> DTO do simulador -> ResultadoInclusaoDocumentoDossieProduto`, sem importar DTO publico
ou DTO MTR, e preserva o evento `simtr-hub.dossie-produto.documento.simulador.usado` e o atributo
de span `simtr_hub.origem_dados=mock`.

`@DocumentoSimulador` identifica o novo adapter. `DocumentoDossieProdutoPortasProducer` recebe as
portas qualificadas por `@DocumentoMtr` e `@DocumentoSimulador` e publica uma unica porta de saida
nao qualificada. A selecao usa somente
`simtr-hub.simulador.dossie-produto.habilitado`, preservando os valores existentes: `false` na
configuracao principal, `true` em desenvolvimento e no profile padrao `test`. Nenhuma property ou
dependencia Maven foi adicionada ou alterada.

O ciclo TDD registrou RED de compilacao antes da criacao dos componentes. Na fase GREEN:

- `DocumentoDossieProdutoSimuladorAdapterTest` le a fixture real por DTO proprio, comprova os dois
  ids internos e a falha explicita quando a fixture nao existe;
- `DocumentoDossieProdutoPortasProducerTest` comprova a mesma instancia de simulador quando ligado
  e de MTR quando desligado;
- `DocumentoDossieProdutoSelecaoSimuladorQuarkusTest` comprova bootstrap CDI e fixture com a
  property ligada no profile padrao `test`;
- `DocumentoDossieProdutoMtrContractTest`, cujo stub local sobrescreve a property para `false`,
  comprova que a porta nao qualificada seleciona MTR e preserva o wire nulo caracterizado.

Os 13 casos de `ArchUnitProgressivoTest` passaram. `mvn -q clean test` terminou com codigo 0 em
2026-07-13: 213 elementos `testcase` em 52 relatorios, zero falhas, zero erros e zero ignorados,
sem Docker, Dev Services, `QuarkusTestProfile` ou `@TestProfile`.

O proximo item e a Task 2.3e, que ligara o Resource a porta de entrada, adicionara o mapper REST e
a observabilidade do caso de uso e removera a cadeia legada exclusiva da inclusao de documento
somente depois da equivalencia verde.

## Evidencias da Task 2.3e - borda REST

`InclusaoDocumentoDossieProdutoRequest` e `InclusaoDocumentoDossieProdutoResponse` tornam
explicita a fronteira publica da operacao e preservam os mesmos nomes JSON, inclusao `NON_NULL`,
validacoes aninhadas e nulabilidade. Os DTOs aninhados publicos continuam exclusivos da borda
REST. `DocumentoDossieProdutoRestMapper` converte todos os campos, listas e elementos nulos para
o comando interno, converte os dois identificadores do resultado para a resposta publica e traduz
`FalhaInclusaoDocumentoDossieProduto` para as excecoes REST existentes sem perder status,
recurso, identificadores, mensagens, detalhe ou stacktrace externo.

`DocumentoDossieProdutoObservabilidade` implementa a porta de entrada e envolve
`IncluirDocumentoDossieProdutoCasoDeUso`, preservando o span de service, atributos, origem MTR ou
mock e os eventos de inicio, conclusao e falha. `DossieProdutoResource` injeta somente
`IncluirDocumentoDossieProduto` para esta operacao; path, metodo, status 201, corpo JSON,
validacoes, span e eventos da API permanecem iguais. O Resource nao conhece a porta de saida, o
REST Client MTR nem o simulador. O OpenAPI permaneceu integralmente sob geracao do Quarkus, sem
teste, filtro, arquivo estatico ou manipulacao.

O ciclo TDD registrou RED antes da criacao dos contratos, mapper, observabilidade e ligacao do
Resource. Na fase GREEN:

- `DocumentoDossieProdutoRestMapperTest` cobre mapeamento completo, campos/listas/elementos nulos,
  resposta nula e traducao lossless de todas as categorias de falha;
- `DocumentoDossieProdutoObservabilidadeTest` comprova delegacao do mesmo comando, retorno
  inalterado e propagacao da mesma falha;
- `DocumentoDossieProdutoResourceQuarkusTest` comprova pelo endpoint real o mapeamento
  REST-interno-resposta e a resposta nula legada;
- `DossieProdutoApiContractTest` e `DossieProdutoErroApiContractTest` preservam contrato HTTP,
  validacoes e erros;
- `DocumentoDossieProdutoMtrContractTest` comprova o fluxo ponta a ponta contra stub HTTP local
  com a property desligada, incluindo wire, headers, OIDC local, erro sem retry e retry recuperavel;
- `DocumentoDossieProdutoSelecaoSimuladorQuarkusTest`, spans e logs comprovam o modo simulador e
  a observabilidade preservada no profile padrao `test`.

Somente depois dessa matriz verde, o metodo exclusivo foi removido de `DossieProdutoFachada`,
`DossieProdutoService`, `DossieProdutoGateway`, `DossieProdutoClient`, `DossieProdutoMapper` e
`DossieProdutoMockFactory`. Os DTOs `DossieProdutoDocumentoInclusaoDto` e
`DossieProdutoDocumentoCriadoDto` e os sete VOs `DossieProdutoDocumento*Vo`, sem consumidores,
tambem foram removidos. Os testes compartilhados mantiveram integralmente a cobertura da
validacao negocial ainda legada, e `rg` nao encontrou referencia aos tipos ou metodos removidos.

`mvn -q clean test` terminou com codigo 0 em 2026-07-13: 215 elementos `testcase` em 55 relatorios,
zero falhas, zero erros e zero ignorados, no profile `test`, com
`quarkus.devservices.enabled=false` e sem Docker. Nao existem `QuarkusTestProfile`, `@TestProfile`,
`@QuarkusIntegrationTest`, teste de OpenAPI ou `System.out.println`; os 13 casos de
`ArchUnitProgressivoTest` passaram e `git diff --check` terminou sem erro. Permanecem somente os
warnings deprecatórios conhecidos do Hibernate Validator sobre `@Valid` em containers, mantidos
para preservar a validacao REST publicada.

O proximo item e o checkpoint C2.3: revisao humana das evidencias e registro de GO antes de iniciar
a Task 2.4a de validacao negocial.
