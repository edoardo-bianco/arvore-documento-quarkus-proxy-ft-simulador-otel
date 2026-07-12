# Inventario de observabilidade contratual

## Escopo

Este inventario registra os sinais existentes antes da refatoracao DDD. A Task 0.2 nao adiciona,
remove ou renomeia telemetria de producao; ela identifica o que precisa sobreviver a movimentos de
packages e separa sinais protegidos agora dos que dependem do stub MTR da Task 0.3.

Evidencia de fechamento: 12 testes focados de observabilidade e suite completa com 126 testes,
zero falhas, zero erros e zero ignorados.

Perguntas operacionais que os sinais atuais permitem responder:

1. Em qual camada uma operacao do Hub esta: API, aplicacao, simulador ou integracao MTR?
2. Qual capacidade e operacao falhou e qual foi o tipo do erro?
3. A resposta veio do mock ou do MTR e qual configuracao de simulador estava ativa?
4. Qual REST Client/metodo foi chamado, qual status retornou e quanto tempo levou?

## Protecao executavel

| Teste | Contrato protegido |
|---|---|
| `ObservabilidadeSpansContratoTest#preservaSpansDasOitoCapacidadesNoCaminhoSimulador` | conjunto exato de 16 spans manuais de API/aplicacao; `SpanKind`; `http.route`; `simtr_hub.api`; flags de simulador; `simtr_hub.origem_dados=mock` |
| `ObservabilidadeSpansContratoTest#preservaDeclaracoesDosSpansDeIntegracaoMtr` | oito nomes de spans CLIENT dos Gateways e os tres `@SpanAttribute` de processo/checklist |
| `RestClientObservabilityFilterTest#preservaEventosEAtributosDerivadosDaInvocacaoRestClient` | eventos request/response; method, path, status, payload flag; `rest_client.class` e `rest_client.operation` derivados de `org.eclipse.microprofile.rest.client.invokedMethod` |
| `ObservabilidadeLogsContratoTest#preservaEventosEstruturadosDasOitoCapacidadesNoCaminhoSimulador` | conjunto exato de 40 eventos de sucesso e MDC comum com evento/camada/componente/operacao/trace |

Os testes usam `InMemorySpanExporter` produzido por CDI somente no perfil de teste. O exporter e a
dependencia `opentelemetry-sdk-testing` seguem o mecanismo documentado oficialmente pelo Quarkus:
https://quarkus.io/guides/opentelemetry#using-cdi-to-produce-a-test-exporter. A captura de logs usa o
JBoss Log Manager ja configurado no Surefire, conforme
https://quarkus.io/guides/logging#configure-logging-for-quarkustest.

## Convencoes comuns

### Logs estruturados

`ObservabilityLog` coloca temporariamente no MDC e remove depois da emissao:

- `evento`: igual a mensagem estavel do log;
- `traceId`, `spanId`, `traceSampled`: presentes quando existe span valido;
- `camada`, `componente`, `operacao`: presentes nos eventos das oito capacidades;
- campos de negocio de baixa cardinalidade ou identificadores usados para diagnostico;
- `resultado=sucesso|erro`, `erro_tipo` e `origem=mock` onde aplicavel.

Todos os 40 eventos de sucesso abaixo sao protegidos pelo conjunto exato do teste. Cada operacao
emite cinco passos: requisicao API recebida, service iniciado, simulador usado, service concluido e
resposta API enviada.

### Spans

- API: `SpanKind.SERVER`, prefixo `simtr-hub.api.*`.
- Aplicacao: `SpanKind.INTERNAL`, prefixo `simtr-hub.service.*`.
- Integracao MTR: `SpanKind.CLIENT`, prefixo `mtr.*`.
- Instrumentacao HTTP automatica do Quarkus continua ativa, mas seus nomes nao sao duplicados como
  contrato manual nesta Task; paths e verbos ja sao protegidos pela Task 0.1.

## Sinais por capacidade

| Capacidade | Span API | Span aplicacao | Eventos de sucesso, prefixo na ordem | Atributos estaveis protegidos |
|---|---|---|---|---|
| Consultar processo | `simtr-hub.api.processo.consultar` | `simtr-hub.service.processo.consultar` | `simtr-hub.processo`: `requisicao.recebida`, `service.iniciado`, `simulador.usado`, `service.concluido`, `resposta.enviada` | rota publica; `simtr_hub.api=parametrizacao-processo-v1`; flag `simtr_hub.simulador_parametrizacao_processo_habilitado`; origem |
| Consultar checklist | `simtr-hub.api.checklist.consultar` | `simtr-hub.service.checklist.consultar` | `simtr-hub.checklist`: mesmos cinco passos | rota publica; `simtr_hub.api=parametrizacao-checklist-v1`; flag `simtr_hub.simulador_parametrizacao_checklist_habilitado`; origem |
| Criar dossie | `simtr-hub.api.dossie-produto.criar` | `simtr-hub.service.dossie-produto.criar` | `simtr-hub.dossie-produto`: mesmos cinco passos | rota; API v1; flag de simulador dossie; origem |
| Atualizar formulario | `simtr-hub.api.dossie-produto.formulario.atualizar` | `simtr-hub.service.dossie-produto.formulario.atualizar` | prefixo `simtr-hub.dossie-produto.formulario` | rota; API v1; flag de simulador dossie; origem |
| Incluir documento | `simtr-hub.api.dossie-produto.documento.incluir` | `simtr-hub.service.dossie-produto.documento.incluir` | prefixo `simtr-hub.dossie-produto.documento` | rota; `simtr_hub.api=dossie-produto-v2`; flag; origem |
| Registrar validacao | `simtr-hub.api.dossie-produto.validacao-negocial.registrar` | `simtr-hub.service.dossie-produto.validacao-negocial.registrar` | prefixo `simtr-hub.dossie-produto.validacao-negocial` | rota; API v1; flag; origem |
| Avancar workflow | `simtr-hub.api.dossie-produto.workflow.avancar` | `simtr-hub.service.dossie-produto.workflow.avancar` | prefixo `simtr-hub.dossie-produto.workflow` | rota; API v1; flag; origem |
| Obter credencial | `simtr-hub.api.gestao-documento.credencial-container.gerar` | `simtr-hub.service.gestao-documento.credencial-container.gerar` | prefixo `simtr-hub.gestao-documento.credencial-container` | rota; `simtr_hub.api=gestao-documento-v1`; flag `simtr_hub.simulador_gestao_documento_habilitado`; origem |

Os spans tambem registram identificadores, quantidades e dados de resposta quando nao nulos. Esses
atributos estao inventariados no codigo atual, mas o teste geral protege somente o subconjunto
estavel acima para nao transformar fixtures em contrato funcional duplicado.

## Borda MTR

| Operacao | Span CLIENT | Servico/API/metodo/path atuais |
|---|---|---|
| Processo | `mtr.parametrizacao.processo.consultar` | `simtr-parametrizacao`; `patriarca-processo-v2`; GET; `/simtr-parametrizacao/v2/patriarca/processo/identificador-negocial/{identificador}` |
| Checklist | `mtr.parametrizacao.checklist.consultar` | `simtr-parametrizacao`; `cadastro-checklist-v1`; GET; `/simtr-parametrizacao/v1/cadastro/checklist/identificador-negocial/{identificador}/versao/{versao}` |
| Criar dossie | `mtr.dossie-produto.criar` | `simtr-dossie-produto`; v1; POST; `/simtr/dossie-produto/v1/dossie-produto` |
| Formulario | `mtr.dossie-produto.formulario.atualizar` | `simtr-dossie-produto`; v1; PATCH; path com `{id}/formulario` |
| Documento | `mtr.dossie-produto.documento.incluir` | `simtr-dossie-produto`; v2; POST; path com `{id}/documento` |
| Validacao | `mtr.dossie-produto.validacao-negocial.registrar` | `simtr-dossie-produto`; v1; PATCH; path com `{id}/validacao-negocial` |
| Workflow | `mtr.dossie-produto.workflow.avancar` | `simtr-dossie-produto`; v1; POST; path com `{id}/workflow` |
| Credencial | `mtr.gestao-documento.credencial-container.gerar` | `simtr-gestao-documento`; v1; POST; `/simtr/gestao-documento/v1/storage/container/credencial` |

Todos registram `mtr.servico`, `mtr.api`, `http.request.method`, `url.path`,
`mtr.resposta.sucesso` e `erro.tipo` no caminho de falha. Os nomes/kinds sao protegidos agora; os
valores runtime de sucesso/falha e paths materializados serao exercitados com simulador desligado e
stub local na Task 0.3.

### Filtro REST Client

Eventos de span e log protegidos:

- `mtr.rest-client.request.enviada`;
- `mtr.rest-client.response.recebida`.

Atributos principais: `rest_client.request.method`, `rest_client.url`, `rest_client.url.path`,
`rest_client.class`, `rest_client.operation`, `rest_client.payload.enabled`, status, duracao e
metadados de payload (`present`, `truncated`, `length`, `limit`, `body`). Classe e operacao dependem
do metodo refletido; por isso uma renomeacao Java pode mudar dashboards e e testada explicitamente.

Payloads mascaram recursivamente `sas`, tokens, secrets, API keys e passwords. Payload completo nao
deve ser usado como atributo novo nem como label de metrica.

## Configuracao existente

| Area | Baseline |
|---|---|
| Payload REST Client | habilitado em producao; request 2000 caracteres; response 4000; desabilitado em teste |
| Traces | sampler `always_on`, argumento `1.0`; exporter default `none` |
| Testes | exporter `cdi` em memoria e `quarkus.otel.simple=true`; nenhum backend externo |
| Perfis `jaeger` e `grafana` | endpoint OTLP local `4317`, exporter de traces/logs `cdi`, handler de logs habilitado |
| Console | JSON compacto, MDC em campos planos e `service.name` adicional |
| Arquivo | `target/logs/simtr-hub.json`, JSON compacto, MDC plano e `service.name` |

Nao existe instrumentacao explicita de metrica de aplicacao no baseline. Esta Task nao cria
metricas, alertas ou dashboards.

## Sinais documentados e ainda nao executados ponta a ponta

- eventos `*.requisicao.falhou`, `*.service.falhou` e `*.mtr.falhou`;
- `mtr.resposta.sucesso=false`, `erro.tipo` e status de span ERROR dos Gateways;
- logs request/response produzidos por chamada HTTP MTR real com OIDC/provider ativos;
- equivalencia dos atributos MTR quando o simulador estiver desabilitado.

Esses sinais nao podem ser removidos ou renomeados. A protecao runtime sera adicionada pela Task
0.3 e pelas caracterizacoes verticais das Fases 1 a 5, quando o stub MTR permitir exercitar a borda
sem rede externa.
