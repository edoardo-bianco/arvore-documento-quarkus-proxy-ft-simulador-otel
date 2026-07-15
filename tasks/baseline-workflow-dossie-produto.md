# Baseline do workflow de dossie produto

> Documento historico da capacidade antes e durante a migracao. Referencias a protecao ou
> fingerprint OpenAPI foram substituidas pela geracao exclusiva do Quarkus. O estado final esta em
> `equivalencia-final.md`; evidencias quantitativas permanecem somente no JaCoCo.

## Escopo

Manifesto de caracterizacao da Task 1.1 para a capacidade
`IniciarOuAvancarWorkflowDossieProduto`. Este documento congela o comportamento legado antes da
migracao estrutural e nao autoriza novo workflow, endpoint ou mudanca funcional.

## Contrato REST publico

- Metodo e path: `POST /simtr-hub/v1/dossie-produto/{id}/workflow`.
- Entrada: identificador de dossie no path; nao existe corpo de requisicao.
- Sucesso: `200 application/json` com `{"id":123}` no simulador atual.
- Validacao: identificador zero retorna `400` e a mensagem
  `O identificador do dossie produto deve ser maior que zero.` no erro publico completo.
- OpenAPI: operacao POST, parametro de path e respostas permanecem protegidos semanticamente.

## Borda MTR

- Simulador desabilitado seleciona o REST Client `dossie-produto`.
- Wire: `POST /simtr/dossie-produto/v1/dossie-produto/{id}/workflow`, corpo vazio.
- Headers protegidos: `Accept: application/json`, `apikey`, `Authorization: Bearer` e
  `traceparent` W3C.
- A resposta MTR `{"id":987}` atravessa Resource, service, gateway e client sem alteracao do JSON.
- Erro de negocio `400` preserva todos os campos publicos e nao executa retry.

## Simulador e configuracao

- `simtr-hub.simulador.dossie-produto.habilitado=true` seleciona o mock e devolve o identificador
  recebido no path.
- `false` seleciona o adapter MTR; o recurso de teste prova a chamada ao stub localhost com OIDC
  ativo.
- O default da configuracao principal e `false`; os profiles `%dev` e de teste habilitam o
  simulador, enquanto o recurso do stub sobrescreve explicitamente para `false` nos contratos MTR.
- REST Client: config key `dossie-produto`, URL configuravel e timeouts HTTP atuais preservados.

## Fault tolerance congelada

Aplicada ao metodo interceptado do REST Client:

- timeout: 2.000 ms;
- retry: 3 retries, delay 300 ms e jitter 100 ms;
- retry/fail: `WorkflowDossieProdutoMtrException.Servidor`, `ProcessingException` e
  `TimeoutException`;
- abort/skip: `WorkflowDossieProdutoMtrException.Negocio` e
  `WorkflowDossieProdutoMtrException.TecnicaCliente`;
- circuit breaker: volume 10, ratio 0,5, delay 10.000 ms e success threshold 2.

O stub prova retry apos `500`, retry apos timeout e abertura do circuit breaker depois das chamadas
remotas previstas. A operacao e mutavel: a refatoracao preserva o retry atual, mas nao resolve o
risco de duplicidade. Idempotencia continua bloqueante antes de workflows futuros.

## Observabilidade

- spans: `simtr-hub.api.dossie-produto.workflow.avancar`,
  `simtr-hub.service.dossie-produto.workflow.avancar` e
  `mtr.dossie-produto.workflow.avancar`;
- rota: `/simtr-hub/v1/dossie-produto/{id}/workflow`;
- logs contratuais cobrem requisicao, service, simulador, resposta e falha;
- atributos protegem identificador, resposta, origem MTR/mock, API e operacao.

## Evidencias executaveis

- `WorkflowDossieProdutoMtrContractTest`: cinco testes de wire, headers, resposta, erro lossless,
  retry, timeout e matriz declarada.
- `DossieProdutoMtrContractTest#circuitBreakerAbertoInterrompeNovasChamadasAoStub`.
- `DossieProdutoApiContractTest#preservaContratoDeAvancoDeWorkflow`.
- `DossieProdutoErroApiContractTest#preservaErroParaIdInvalidoNoWorkflow`.
- `WorkflowDossieProdutoSelecaoMtrQuarkusTest` e
  `WorkflowDossieProdutoSelecaoSimuladorQuarkusTest`: selecao CDI com simulador
  desabilitado/habilitado.
- `OpenApiContractTest`: equivalencia semantica do documento e conjunto exato de operacoes.
- `ObservabilidadeSpansContratoTest` e `ObservabilidadeLogsContratoTest`.
- `WorkflowDossieProdutoResourceQuarkusTest`: prova que o Resource usa a porta de entrada e mapeia
  o resultado interno para o JSON publico.

Verificacao em 2026-07-12:

- conjunto focado: 39 testes, zero falhas, zero erros e zero ignorados;
- `mvn -q test`: 136 testes, zero falhas, zero erros e zero ignorados;
- nenhum arquivo de producao alterado.

Verificacao apos migrar o piloto em 2026-07-12:

- `mvn -q clean test`: 156 testes, zero falhas, zero erros e zero ignorados;
- Resource percorre porta de entrada, caso de uso e adapter selecionado;
- erros MTR continuam lossless e os sinais REST/service/MTR permanecem equivalentes;
- todos os testes Quarkus usam o perfil padrao `test`, sem profile proprio.

Consolidacao do legado controlado em 2026-07-12:

- removidos o metodo de workflow da fachada, service, gateway, REST Client e mock factory legados;
- removidos tres testes redundantes somente depois de comprovada a cobertura equivalente pelos
  testes do nucleo, selecao CDI, adapter MTR, simulador e Resource;
- `rg` nao encontra referencias ao workflow na cadeia legada;
- `mvn -q clean test`: 153 elementos `testcase`, zero falhas, zero erros e zero ignorados;
- o atributo agregado do Surefire soma 147 porque atribui zero a suite ArchUnit, embora o XML dessa
  suite contenha seis casos JUnit verdes; a contagem por elementos `testcase` evita esse erro.

## Fechamento para C1

- branch revisada: `refactor/ddd-fase-0-baseline`;
- `git diff --check` sem erros;
- nenhuma referencia ao workflow na fachada, service, gateway, REST Client ou mock factory legados;
- nenhum `QuarkusTestProfile`, `@TestProfile` ou `@Operation(hidden = true)` no projeto;
- documento OpenAPI integral rebaselined somente depois de as oito operacoes publicas e os 43
  schemas permanecerem semanticamente verdes;
- C1 recebeu GO humano em 2026-07-12; a Task 2.1a e a proxima etapa autorizada.

## Advertencia OpenAPI

O scanner SmallRye OpenAPI do Quarkus inclui as interfaces `@RegisterRestClient` anotadas com
Jakarta REST porque o projeto nao restringe `mp.openapi.scan.packages` nem configura exclusoes.
Esse comportamento preexistente permanece fora do escopo desta refatoracao: o documento contem os
oito paths publicos e oito paths internos dos REST Clients. O client MTR dedicado adiciona
`WorkflowDossieProdutoMtrResponse` aos componentes, totalizando 43 schemas, sem alterar a semantica
das oito operacoes publicas. Nao usar `@Operation(hidden = true)` nos REST Clients como contorno;
uma eventual limpeza deve ser decisao separada, com nova baseline OpenAPI.
