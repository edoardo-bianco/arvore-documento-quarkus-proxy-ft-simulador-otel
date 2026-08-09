# Catálogo atual de observabilidade

## Como usar

Este catálogo registra nomes e atributos operacionais protegidos por testes. Consulte-o antes de
alterar logs, spans, REST Clients, nomes Java refletidos, configuração de telemetria ou tratamento
de dados sensíveis.

Os sinais permitem responder:

1. em qual camada uma operação está: API, aplicação, simulador ou MTR;
2. qual capacidade falhou e qual tipo de erro ocorreu;
3. se a resposta veio do simulador ou do MTR;
4. qual REST Client/método foi chamado, status e duração;
5. qual backend/operação documental falhou para uma análise;
6. se o feed está no cursor inicial, retomando cursor persistido ou sem lease Cosmos;
7. qual entre backend documental, Redis/Valkey e Lease impede readiness;
8. como seguir uma análise por trace sem expor seu conteúdo.

## Proteção executável

| Teste | Contrato protegido |
|---|---|
| `ObservabilidadeSpansContratoTest#preservaSpansDasOitoCapacidadesNoCaminhoSimulador` | 16 spans manuais de API/aplicação, `SpanKind`, rota, API, flags de simulador e origem mock |
| `ObservabilidadeSpansContratoTest#preservaDeclaracoesDosSpansDeIntegracaoMtr` | oito spans CLIENT dos gateways e atributos de processo/checklist |
| `RestClientObservabilityFilterTest#preservaEventosEAtributosDerivadosDaInvocacaoRestClient` | eventos request/response, método, path, status, payload e nomes derivados por reflexão |
| `ObservabilidadeLogsContratoTest#preservaEventosEstruturadosDasOitoCapacidadesNoCaminhoSimulador` | 40 eventos de sucesso e MDC comum de evento/camada/componente/operação/trace |
| `OllamaAnaliseConformidadeFtQuarkusTest` | spans de workflow/agente, eventos de fallback e ausência de texto/checklist nos sinais próprios |
| `RepositorioDocumentalObservavelTest` | span/log documental, IDs permitidos, backend/operação/resultado, trace e proibição de payload/exceção |
| `ObservabilidadeFeedDocumentalTest` | span/log do feed, replay, cursor/lease, IDs permitidos, propagação reativa e proibição do CloudEvent/payload |
| `AnaliseConformidadeFlowQuarkusTest#readinessDistingueBackendDocumentalRedisELeaseSemDadosSensiveis` | checks separados de backend documental, Redis/Valkey e Lease sem dados sensíveis |

## Convenções comuns

### Logs estruturados

`ObservabilityLog` administra temporariamente no MDC:

- `evento`, igual à mensagem estável;
- `traceId`, `spanId` e `traceSampled`, quando existe span válido;
- `camada`, `componente` e `operacao`;
- campos de baixa cardinalidade ou identificadores necessários ao diagnóstico;
- `resultado=sucesso|erro`, `erro_tipo` e `origem=mock`, quando aplicáveis.

Cada operação emite os passos de requisição recebida, caso de uso iniciado, simulador usado, caso
de uso concluído e resposta enviada. Eventos de falha mantêm os prefixos da mesma capacidade.

### Spans

- API: `SpanKind.SERVER`, prefixo `simtr-hub.api.*`;
- aplicação: `SpanKind.INTERNAL`, prefixo `simtr-hub.service.*`;
- integração MTR: `SpanKind.CLIENT`, prefixo `mtr.*`.

A instrumentação HTTP automática do Quarkus permanece ativa, mas não substitui os sinais manuais
que carregam semântica da capacidade.

## Sinais por capacidade

| Capacidade | Span API | Span aplicação | Prefixo dos eventos | Atributos estáveis |
|---|---|---|---|---|
| Consultar processo | `simtr-hub.api.processo.consultar` | `simtr-hub.service.processo.consultar` | `simtr-hub.processo` | rota, `simtr_hub.api=parametrizacao-processo-v1`, flag de simulador e origem |
| Consultar checklist | `simtr-hub.api.checklist.consultar` | `simtr-hub.service.checklist.consultar` | `simtr-hub.checklist` | rota, `simtr_hub.api=parametrizacao-checklist-v1`, flag e origem |
| Criar dossiê | `simtr-hub.api.dossie-produto.criar` | `simtr-hub.service.dossie-produto.criar` | `simtr-hub.dossie-produto` | rota, API v1, flag e origem |
| Atualizar formulário | `simtr-hub.api.dossie-produto.formulario.atualizar` | `simtr-hub.service.dossie-produto.formulario.atualizar` | `simtr-hub.dossie-produto.formulario` | rota, API v1, flag e origem |
| Incluir documento | `simtr-hub.api.dossie-produto.documento.incluir` | `simtr-hub.service.dossie-produto.documento.incluir` | `simtr-hub.dossie-produto.documento` | rota, `simtr_hub.api=dossie-produto-v2`, flag e origem |
| Registrar validação | `simtr-hub.api.dossie-produto.validacao-negocial.registrar` | `simtr-hub.service.dossie-produto.validacao-negocial.registrar` | `simtr-hub.dossie-produto.validacao-negocial` | rota, API v1, flag e origem |
| Avançar workflow | `simtr-hub.api.dossie-produto.workflow.avancar` | `simtr-hub.service.dossie-produto.workflow.avancar` | `simtr-hub.dossie-produto.workflow` | rota, API v1, flag e origem |
| Obter credencial | `simtr-hub.api.gestao-documento.credencial-container.gerar` | `simtr-hub.service.gestao-documento.credencial-container.gerar` | `simtr-hub.gestao-documento.credencial-container` | rota, `simtr_hub.api=gestao-documento-v1`, flag e origem |

## PoC de conformidade

| Fronteira | Span | Eventos de log | Atributos permitidos |
|---|---|---|---|
| Workflow | `simtr-hub.flow.conformidade.analise` | checklist obtido e fallback aplicado | `instance_id`, identificador/versão/quantidade do checklist, modelo, origem do resultado e status |
| Agente Ollama | `simtr-hub.agent.conformidade.analisar` | `conformidade.agente.chamada.iniciada`, `concluida` | os mesmos identificadores/contagens e modelo, sem prompt ou resposta |
| Persistência documental | `simtr-hub.persistencia.conformidade.documento` | `conformidade.persistencia.operacao.concluida`, `falhou` | backend, operação, resultado, `correlation_id`, `instance_id`, `documento_id` e trace |
| Feed documental | `simtr-hub.feed.conformidade.documento` | `conformidade.feed.operacao.concluida`, `falhou` | backend, operação, resultado, replay, `cursor_lease_id`, `evento_id`, `correlation_id`, `instance_id` e trace |

As operações documentais são `criar`, `substituir` e `consultar`. O resultado informa apenas
gravação/conflito ou presença/ausência. No feed, CouchDB usa `CURSOR_INICIAL` ou
`CURSOR_PERSISTIDO`; Cosmos usa `DESDE_INICIO_SEM_LEASE`. Os valores de cursor/lease são IDs
técnicos, nunca o documento ou corpo do CloudEvent.

Spans de persistência e feed permanecem abertos até a terminação do `Uni`. Em falha, recebem status
`ERROR` e resultado `FALHA`, mas não gravam evento de exceção, stack ou mensagem; o mesmo objeto de
falha continua propagado para a aplicação.

Texto, prompt, resposta, revisão, evidência, credencial, documento e corpo CloudEvent são proibidos
nesses spans e logs. O profile `%poc` é uma exceção operacional separada: habilita logs completos
do LangChain4j e, por isso, admite somente dados sintéticos.

## Borda MTR

| Operação | Span CLIENT | Serviço/API |
|---|---|---|
| Processo | `mtr.parametrizacao.processo.consultar` | `simtr-parametrizacao`, `patriarca-processo-v2` |
| Checklist | `mtr.parametrizacao.checklist.consultar` | `simtr-parametrizacao`, `cadastro-checklist-v1` |
| Criar dossiê | `mtr.dossie-produto.criar` | `simtr-dossie-produto`, v1 |
| Formulário | `mtr.dossie-produto.formulario.atualizar` | `simtr-dossie-produto`, v1 |
| Documento | `mtr.dossie-produto.documento.incluir` | `simtr-dossie-produto`, v2 |
| Validação | `mtr.dossie-produto.validacao-negocial.registrar` | `simtr-dossie-produto`, v1 |
| Workflow | `mtr.dossie-produto.workflow.avancar` | `simtr-dossie-produto`, v1 |
| Credencial | `mtr.gestao-documento.credencial-container.gerar` | `simtr-gestao-documento`, v1 |

Os spans MTR registram `mtr.servico`, `mtr.api`, `http.request.method`, `url.path`,
`mtr.resposta.sucesso` e `erro.tipo` no caminho de falha.

## Filtro REST Client

Eventos protegidos:

- `mtr.rest-client.request.enviada`;
- `mtr.rest-client.response.recebida`.

Atributos principais: `rest_client.request.method`, `rest_client.url`, `rest_client.url.path`,
`rest_client.class`, `rest_client.operation`, `rest_client.payload.enabled`, status, duração e
metadados de payload. Classe e operação dependem do método refletido; renome Java pode alterar
dashboards e exige checkpoint observável.

Payloads mascaram recursivamente SAS, tokens, secrets, API keys e passwords. Payload completo não
deve virar atributo novo nem label de métrica.

## Configuração

| Área | Estado atual |
|---|---|
| Payload REST Client | habilitado em produção; request 2000 caracteres; response 4000; desabilitado em teste |
| Traces | sampler `always_on`, argumento `1.0`; exporter padrão `none` |
| Testes | exporter CDI em memória e `quarkus.otel.simple=true`; nenhum backend externo |
| Perfis `jaeger` e `grafana` | endpoint OTLP local `4317`, exporters CDI e handler de logs habilitado |
| Console | JSON compacto, MDC em campos planos e `service.name` adicional |
| Arquivo | `target/logs/simtr-hub.json`, JSON compacto, MDC plano e `service.name` |

## Health e readiness da conformidade

| Dependência | Check | Informação exposta |
|---|---|---|
| Backend documental | `simtr-hub-conformidade-documental` | `UP/DOWN` e `backend=couchdb|cosmosdb` |
| Redis/Valkey | health fornecido pela extensão Redis | disponibilidade do checkpoint técnico |
| Lease Kubernetes | `Lease Acquisition` | `UP/DOWN` e `leaseAcquired`; exigido para readiness no profile Kubernetes |

Os três checks permanecem distintos. Agregar todos em um único estado impediria identificar qual
dependência retirou o pod do Service.

Não existe instrumentação explícita de métrica de aplicação. Métricas, alertas e dashboards novos
exigem feature e critérios operacionais próprios.
