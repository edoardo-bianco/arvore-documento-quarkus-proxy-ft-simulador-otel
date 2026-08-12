# Catálogo atual de observabilidade

## Como usar

Este catálogo registra nomes e atributos operacionais protegidos por testes. Consulte-o antes de
alterar logs, spans, REST Clients, nomes Java refletidos, configuração de telemetria ou tratamento
de dados sensíveis.

Os sinais permitem responder:

1. em qual camada uma operação está: API, aplicação, simulador ou MTR;
2. qual capacidade falhou e qual tipo de erro ocorreu;
3. se a resposta veio do simulador ou do MTR;
4. qual REST Client/método foi chamado, status e duração.

## Proteção executável

| Teste | Contrato protegido |
|---|---|
| `ObservabilidadeSpansContratoTest#preservaSpansDasDezCapacidadesNoCaminhoSimulador` | 20 spans manuais de API/aplicação, `SpanKind`, parentage, rota, API, flags de simulador e origem mock |
| `ObservabilidadeSpansContratoTest#preservaDeclaracoesDosSpansDeIntegracaoMtr` | dez spans CLIENT dos gateways, incluindo `mtr.dossie-produto.consultar` |
| `RestClientObservabilityFilterTest#preservaEventosEAtributosDerivadosDaInvocacaoRestClient` | eventos request/response, método, path, status, payload e nomes derivados por reflexão |
| `ObservabilidadeLogsContratoTest#preservaEventosEstruturadosDasDezCapacidadesNoCaminhoSimulador` | 50 eventos de sucesso e MDC comum de evento/camada/componente/operação/trace |
| `ObservabilidadeLogsContratoTest#registraFalhaDeProdutoComCamposEstaveisSemDadosSensiveis` | eventos de início/falha da alteração de produtos, id, quantidade, resultado e tipo de erro |
| `ObservabilidadeLogsContratoTest#registraFalhaDaConsultaComOrigemEClassificacaoSemDadosSensiveis` | eventos da falha simulada da consulta, id, origem mock, resultado e classificação sem dados sensíveis |
| `ProdutoDossieProdutoMtrContractTest#propagaContextoEntreSpansApiAplicacaoEClientSemDadosSensiveis` | encadeamento real API → aplicação → CLIENT, rota, atributos da capacidade e ausência de dados sensíveis novos |
| `ConsultaDossieProdutoMtrContractTest#propagaContextoEntreApiAplicacaoEClientSemDadosSensiveis` | encadeamento real SERVER → INTERNAL → CLIENT, sinais de sucesso/contagens e ausência de payload, PII e segredos |
| `ConsultaDossieProdutoMtrContractTest#preservaErro404CompletoSemRetry` | seis eventos do caminho MTR de falha, classificação por camada, uma chamada e corpo HTTP preservado sem mensagem externa na telemetria |

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
| Consultar dossiê | `simtr-hub.api.dossie-produto.consultar` | `simtr-hub.service.dossie-produto.consultar` | `simtr-hub.dossie-produto.consulta` | rota, `simtr_hub.api=dossie-produto-v2`, flag, origem, id e contagens de clientes, unidades e produtos |
| Criar dossiê | `simtr-hub.api.dossie-produto.criar` | `simtr-hub.service.dossie-produto.criar` | `simtr-hub.dossie-produto` | rota, API v1, flag e origem |
| Atualizar formulário | `simtr-hub.api.dossie-produto.formulario.atualizar` | `simtr-hub.service.dossie-produto.formulario.atualizar` | `simtr-hub.dossie-produto.formulario` | rota, API v1, flag e origem |
| Incluir documento | `simtr-hub.api.dossie-produto.documento.incluir` | `simtr-hub.service.dossie-produto.documento.incluir` | `simtr-hub.dossie-produto.documento` | rota, `simtr_hub.api=dossie-produto-v2`, flag e origem |
| Registrar validação | `simtr-hub.api.dossie-produto.validacao-negocial.registrar` | `simtr-hub.service.dossie-produto.validacao-negocial.registrar` | `simtr-hub.dossie-produto.validacao-negocial` | rota, API v1, flag e origem |
| Alterar produtos contratados | `simtr-hub.api.dossie-produto.produto.alterar` | `simtr-hub.service.dossie-produto.produto.alterar` | `simtr-hub.dossie-produto.produto` | rota, `simtr_hub.api=dossie-produto-v1`, flag, origem, `dossie_produto.id` e `dossie_produto.produtos.quantidade` |
| Avançar workflow | `simtr-hub.api.dossie-produto.workflow.avancar` | `simtr-hub.service.dossie-produto.workflow.avancar` | `simtr-hub.dossie-produto.workflow` | rota, API v1, flag e origem |
| Obter credencial | `simtr-hub.api.gestao-documento.credencial-container.gerar` | `simtr-hub.service.gestao-documento.credencial-container.gerar` | `simtr-hub.gestao-documento.credencial-container` | rota, `simtr_hub.api=gestao-documento-v1`, flag e origem |

### Consulta de dossiê por identificador

- rota pública: `GET /simtr-hub/v1/dossie-produto/{id}`;
- spans: `simtr-hub.api.dossie-produto.consultar` (`SERVER`),
  `simtr-hub.service.dossie-produto.consultar` (`INTERNAL`) e, no caminho MTR,
  `mtr.dossie-produto.consultar` (`CLIENT`);
- parentage: simulador `SERVER → INTERNAL`; MTR `SERVER → INTERNAL → CLIENT`, sempre no mesmo
  trace;
- eventos REST/aplicação de sucesso: `simtr-hub.dossie-produto.consulta.requisicao.recebida`,
  `simtr-hub.dossie-produto.consulta.service.iniciada`,
  `simtr-hub.dossie-produto.consulta.service.concluida` e
  `simtr-hub.dossie-produto.consulta.resposta.enviada`;
- evento do simulador: `simtr-hub.dossie-produto.consulta.simulador.usado`;
- eventos MTR: `mtr.dossie-produto.consulta.chamada.iniciada`,
  `mtr.dossie-produto.consulta.chamada.concluida` e
  `mtr.dossie-produto.consulta.chamada.falhou`;
- eventos REST/aplicação de falha: `simtr-hub.dossie-produto.consulta.service.falhou` e
  `simtr-hub.dossie-produto.consulta.requisicao.falhou`;
- campos estáveis de log: `operacao`, `dossie_produto_id`, `origem`,
  `simulador_habilitado`, `clientes_quantidade`, `unidades_tratamento_quantidade`,
  `produtos_contratados_quantidade`, `resultado` e `erro_tipo`, quando aplicáveis;
- valores de operação: `consultar-dossie-produto` nas camadas REST/aplicação e
  `consultar-dossie-produto-v2` na integração MTR;
- atributos do span API: `http.route`, `simtr_hub.api=dossie-produto-v2`,
  `dossie_produto.id` e as três contagens;
- atributos do span de aplicação: `simtr_hub.simulador_dossie_produto_habilitado`,
  `simtr_hub.origem_dados`, `dossie_produto.id`, `dossie_produto.clientes.quantidade`,
  `dossie_produto.unidades_tratamento.quantidade` e
  `dossie_produto.produtos_contratados.quantidade`;
- atributos do span MTR: `mtr.servico=simtr-dossie-produto`, `mtr.api=dossie-produto-v2`,
  `http.request.method=GET`, `url.path=/simtr/dossie-produto/v2/dossie-produto/{id}`,
  `dossie_produto.id`, as três contagens, `mtr.resposta.sucesso` e `erro.tipo` na falha.

Os sinais específicos da consulta não registram payload nem os campos `cpf`, `cnpj`, `nome`,
`matricula` ou `chave_correlacao_canal`; token, API key, mensagem/detalhe de erro externo e URL
interna completa também permanecem ausentes. O atributo `rest_client.url` continua pertencendo ao
filtro compartilhado preexistente descrito abaixo. O mapper transversal conserva o corpo de erro
na resposta HTTP, mas não copia `detalhe` nem mensagens do payload MTR para logs.

### Alteração de produtos contratados

- rota pública: `PATCH /simtr-hub/v1/dossie-produto/{id}/produto`;
- prefixos de eventos REST/aplicação: `simtr-hub.dossie-produto.produto`;
- prefixo de eventos MTR: `mtr.dossie-produto.produto`;
- campos estáveis de log: `operacao`, `dossie_produto_id`, `produtos_quantidade`,
  `origem_dados`, `simulador_habilitado`, `resultado` e `erro_tipo`, quando aplicáveis;
- atributos estáveis dos spans: `http.route`, `simtr_hub.api`,
  `simtr_hub.simulador_dossie_produto_habilitado`, `simtr_hub.origem_dados`,
  `dossie_produto.id` e `dossie_produto.produtos.quantidade`;
- o caminho MTR acrescenta `mtr.servico=simtr-dossie-produto`,
  `mtr.api=dossie-produto-v1`, `http.request.method=PATCH`, `url.path`,
  `mtr.resposta.sucesso` e `erro.tipo` na falha.

Os sinais específicos da capacidade não adicionam payload, API key, token ou URL interna. O
atributo `rest_client.url` continua pertencendo ao filtro compartilhado preexistente descrito
abaixo; sua eventual alteração exige escopo e checkpoint observável próprios.

## Borda MTR

| Operação | Span CLIENT | Serviço/API |
|---|---|---|
| Processo | `mtr.parametrizacao.processo.consultar` | `simtr-parametrizacao`, `patriarca-processo-v2` |
| Checklist | `mtr.parametrizacao.checklist.consultar` | `simtr-parametrizacao`, `cadastro-checklist-v1` |
| Consultar dossiê | `mtr.dossie-produto.consultar` | `simtr-dossie-produto`, v2 |
| Criar dossiê | `mtr.dossie-produto.criar` | `simtr-dossie-produto`, v1 |
| Formulário | `mtr.dossie-produto.formulario.atualizar` | `simtr-dossie-produto`, v1 |
| Documento | `mtr.dossie-produto.documento.incluir` | `simtr-dossie-produto`, v2 |
| Validação | `mtr.dossie-produto.validacao-negocial.registrar` | `simtr-dossie-produto`, v1 |
| Produtos contratados | `mtr.dossie-produto.produto.alterar` | `simtr-dossie-produto`, v1 |
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

Não existe instrumentação explícita de métrica de aplicação. Métricas, alertas e dashboards novos
exigem feature e critérios operacionais próprios.
