# Planejamento e execução de 10.1 — correlação OpenTelemetry

## 10.1-B1.4 — concluída tecnicamente — 2026-09-11

Eventos confirmada/falhou da publicação inicial entregues em cinco arquivos executáveis,
com contexto do PRODUCER, campos seguros e política de logging aprovada em CP-B1.
**92 focados, 2 integrações A2 e 1.403 padrão/194 classes** aprovados.
Após **ContinuarAjustes** humano, extraída a finalização privada do publisher;
**45 focados e o checkpoint completo** passaram novamente. S3776 CLOSED/FIXED.

Sonar **COMPLIANT / NOT_REQUIRED**: nenhuma issue nova ou grave, cobertura 88,3%,
duplicação 4,4%. Baseline/assessment originais preservados; fingerprint conferido.
A2: exatamente 3/6 spans e 3/16 logs, uma confirmação inicial correlacionada ao PRODUCER.
[Execução, ajuste, inventários e checkpoint](execucao-10-1-b1-4.md).

**Próximo item: 10.1-B1.5**, prova POST → broker e consolidação de B1, com GO
já recebido para o desenho/CP-B1. B1.5 e B2–B5 permanecem pendentes;
C0.4/C9.1-L preservados. Sem staging/commit/push; C3 e encerramento humano da feature pendentes.
Os registros abaixo conservam os marcos anteriores.

## 10.1-B1.2 — concluída tecnicamente — 2026-09-11

Pedido humano **"10.1-B1.2"**, executado sob o GO **"go"** do desenho/CP-B1.
SERVER existente renomeado e INTERNAL de iniciação correlacionado, lazy e único
por invocação. Erros sanitizados, memorização, cancelamento e restauração de Context
verificados; refinamentos locais de rota e callbacks documentados.

**41 testes focados** (17 HTTP, 15 caso de uso e 9 publisher), **2 integrações A2** e
**1.358 testes padrão/192 classes** passaram; build aprovado. A2 exige 2/5 spans e
2/15 logs, preservando as relações dos logs e os carriers vazios neste recorte.
Sonar **COMPLIANT / NOT_REQUIRED**: 213 issues, zero novas/graves, cobertura 88,1%,
duplicação 4,4%. Baseline original de 217 issues e fingerprint final conferidos.
[Execução, diagnóstico, revisão e checkpoint](execucao-10-1-b1-2.md).

**Próximo item: 10.1-B1.3**, PRODUCER inicial e W3C, com GO já recebido para o desenho.
O caso de uso controla OTel manualmente dentro de sua política local; callbacks
externos continuam sujeitos à limitação preexistente do provider MP registrada.
B1.3–B1.5 e B2–B5 permanecem pendentes. C0.4/C9.1-L preservados.
Sem staging/commit/push; C3 e encerramento humano da feature pendentes.
Os registros abaixo conservam os marcos anteriores.

## 10.1-B1.1 — concluída tecnicamente — 2026-09-11

Pedidos humanos "10.1-B1" e "go" registrados. GO para o desenho B1/CP-B1; executado
somente B1.1, com cinco arquivos de teste. Cancelamento de um/todos os assinantes,
conclusão/falha tardia memorizada e invocações intercaladas comprovados. Regressões A2
preparadas com contagens e relações exatas, sem instrumentar produção.

Passaram **18 testes focados**, **2 integrações A2** e **1.352 testes padrão em 192
classes**, sem falhas/erros/ignorados; build aprovado. Sonar **COMPLIANT / NOT_REQUIRED**:
213 issues, nenhuma nova ou grave, cobertura 88,1%, duplicação 4,4%.
Baseline/assessment originais de 217 issues preservados; fingerprint final conferido.
[Execução, revisão, inventário e checkpoint](execucao-10-1-b1-1.md).

**Próximo incremento: 10.1-B1.2**, correlação HTTP/iniciação, com GO já recebido para
o desenho/CP-B1. C0.4/C9.1-L preservados; B1.2–B1.5 não implementadas.
C3 e encerramento humano da feature pendentes. Sem staging/commit/push ou derivados.
Os registros abaixo conservam os marcos anteriores.

## Desenho de 10.1-B1 — 2026-09-11

[Desenho concreto registrado](desenho-10-1-b1.md): cadeia HTTP → iniciação → envio
inicial, W3C do PRODUCER e logs de publicação. O desenho substitui a estimativa de B1
por cinco incrementos, incluindo regressões A2 em cada alteração de observabilidade.
O cancelamento de assinantes deve preservar a operação upstream memorizada; B1.1
confirma esse contrato antes de instrumentar produção.

**Próximo item: 10.1-B1.1, somente testes.** CP-B1 contém propostas concretas de carrier,
atributos/erros e falha dos novos logs; decisão humana ainda não registrada.
Não reabre os nomes C0.4 ou o aceite C9.1-L. Implementação B1 não iniciada.
Entrega exclusivamente Markdown, sem Maven/Sonar; números de A2 continuam históricos.
Os registros de execução abaixo preservam a evidência e a sequência anterior.

## Fechamento técnico de 10.1-A2 — 2026-09-11

**10.1-A2 concluída tecnicamente.** Pedido humano "Próxima subfatia: 10.1-A2" atendido
com três arquivos de teste novos, sem alterações de produção ou dependências.
Caracterização comprovada: POST com parent remoto correto; consultas reais do wrapper
Hub em três traces independentes; logs Hub correlacionados a cada consulta, decisões
sem contexto, settlement referenciando HTTP e log final sem traceId/spanId.
Resultados CONCLUSIVO e QUARENTENA, mensagens agendadas e conclusão das duas filas verificados.

Os **40 cenários de integração em 10 classes** foram validados pela suíte completa
(39 passaram; uma falha na contagem 14/15 do teste novo) e repetição recompilada da
classe Hub após corrigir exclusivamente a contagem. Não houve erro ou teste ignorado.
A execução completa não foi repetida após esse ajuste de asserção.
Revisão independente encerrada sem Critical/Required pendentes.

O checkpoint executou clean verify, scanner e Compute Engine: **1.345 testes padrão
em 192 classes**, todos aprovados; build aprovado. Sonar **COMPLIANT / NOT_REQUIRED**:
**213 issues**, nenhuma nova ou HIGH/BLOCKER/CRITICAL, cobertura **88,1%**,
duplicação **4,4%**. Baseline e assessment originais de **217 issues** comparados
integralmente com a referência de 9.1-C e preservados, sem reinicialização.

Checkpoint: 2026-09-11T12:25:38.8265063+00:00 (09:25:38 em São Paulo).
Análise: 94394293-0594-42bc-bd5b-2d387195bac7.
Compute Engine: 4c3a0bec-c544-4e9d-a1a5-adc29718db7e.
Fingerprint final conferido: c82d12e2cdc14d7afaa4ab828690898b4eca95cdff4d4784327ccc980a768ee1.
Snapshot: .codex/.state/session-after-10-1-a2-compliant-20260911.json.

A matriz executada abaixo orienta os ajustes. **Próximo item: desenho concreto de
10.1-B1**, ainda não iniciado. A cadeia de C0.4 ainda não está completa; A2 apenas
caracteriza suas lacunas. C0.4/doctree e C9.1-L preservados; C3 e encerramento humano
da feature pendentes. Sem staging/commit/push, mudança de .env/patch local ou derivados.


## Evidência executada de 10.1-A2 — 2026-09-11

Dois cenários opt-in novos e um suporte compartilhado, somente em src/test.
Produção e dependências intactas. Exporter CDI existente, controle positivo e forceFlush;
POST com traceparent sintético conhecido; dois listeners reais habilitados no profile.
O spy da porta final do orquestrador delega ao adapter real, sem stub. No cenário da
fixture, a porta pública do Hub, seu wrapper CDI e o simulador permanecem reais.

### Efeitos e inventário validado

- Terminal: POST 202, uma consulta controlada, resultado CONCLUSIVO /
  SITUACAO_CONCLUSIVA_MTR / uma tentativa / CONFORME, log real único e ambas as filas vazias.
  O callback da porta Hub recebeu SpanContext inválido; o teste também não tinha contexto
  ativo ao liberar a resposta. O mock não serve como prova dos spans internos do Hub.
- Fixture 4324680: três consultas Rascunho, dois reagendamentos e resultado QUARENTENA /
  MAXIMO_TENTATIVAS / três tentativas. Modelo capturado no registrador real após o fluxo;
  não inferir quarentena apenas da quantidade de spans ou do log que contém somente IDs.
- Peek com sequência inicial explícita 0, sem consumidor concorrente. Terminal observou
  tentativa 1 ACTIVE/sequência 1. Fixture observou tentativa 2 SCHEDULED/sequência 2,
  tentativa 3 SCHEDULED/sequência 4 e ACTIVE/sequência 5. Identidades AMQP conferidas;
  application properties inteiras vazias em todas essas observações.
- Cada cenário exige exatamente um log final INFO com IDs técnicos próprios, sem exception
  ou stacktrace; confirma a remoção das mensagens nas duas filas. Isso não altera nem
  amplia a garantia best-effort aceita em C9.1-L.

Inventários técnicos gerados somente ao final de todas as asserções, com estado VALIDADO,
em target/provas-10-1-a2/terminal.json e hub-real.json. O arquivo exato do cenário é
removido no início para não reaproveitar prova antiga. O clean do checkpoint remove target;
a matriz abaixo preserva a evidência relevante dessas execuções aprovadas, sem payload
ou dump de propriedades AMQP.

| Captura | Spans exportados | Logs de negócio/Hub selecionados |
|---|---|---|
| Terminal | 1 HTTP SERVER | 2: resultado final + settlement |
| Fixture real | 1 HTTP SERVER + 3 Hub INTERNAL | 15: 9 Hub + 2 decisões + 3 settlements + 1 resultado |

Span HTTP observado: `POST /simtr-hub/v1/monitoramentos-dossie`, com parent remoto
igual ao spanId sintético enviado. Cada consulta real usa
`simtr-hub.service.dossie-produto.consultar`, com trace próprio e parent inválido.
Toda a captura foi inspecionada; não há spans extras de mensageria ou do controle após reset.
Todos os cinco spans da matriz têm scope `io.quarkus.opentelemetry`, versão de scope
não informada, status UNSET sem descrição, links e eventos vazios. Não há duplicação
HTTP/Hub observada; as três consultas correspondem a três operações reais.

| Origem | traceId | spanId | parentSpanId |
|---|---|---|---|
| Terminal HTTP | `d31a51d181b347a3987984560c8bda6e` | `92aa2b84b184c13d` | `2222222222222222` |
| Fixture HTTP | `b3ddebbc7e434bb0a04e367fc284ef39` | `7b71f2920fe1347e` | `2222222222222222` |
| Fixture Hub 1 | `e76a68968ecc66487e92ce4fc382de28` | `e89938740a565147` | `0000000000000000` |
| Fixture Hub 2 | `0ab2969c0b17971d393325022c18fb5f` | `abac005973d03070` | `0000000000000000` |
| Fixture Hub 3 | `2dc47c333f8b29090c279e039200b7c9` | `255e610be43bf1c6` | `0000000000000000` |

| Evento real | Terminal | Fixture | Relação traceId/spanId comprovada |
|---|---|---|---|
| simtr-hub.dossie-produto.consulta.service.iniciada | 0 | 3 | Um por span Hub, mesmo par da consulta |
| simtr-hub.dossie-produto.consulta.simulador.usado | 0 | 3 | Mesmo par do respectivo span Hub |
| simtr-hub.dossie-produto.consulta.service.concluida | 0 | 3 | Mesmo par do respectivo span Hub |
| doctree.monitoramento-mtr.decisao.tomada | 0 | 2 | Ambos os campos vazios; decisão REAGENDAR |
| doctree.monitoramento-mtr.settlement.executado | 1 complete | 2 complete_transacional + 1 complete | Mesmo par do HTTP do cenário |
| orquestrador.monitoramento-dossie.resultado.registrado | 1 | 1 | Ambos os campos vazios |

As asserções relacionam cada trio do Hub ao seu par exportado, verificam todos os pares
dos logs do listener e o par vazio do log final. A referência ao HTTP no settlement é
correlação parcial observada; não comprova contexto ativo em toda a operação nem
continuidade do processamento. O callback inválido, os roots do Hub e o log final sem
contexto impedem declarar a cadeia distribuída completa. Causa e correção das pontes
reativas ficam para os recortes B; IDs funcionais/AMQP não são usados como prova de trace.

Chaves observadas nos spans HTTP: `client.address`, `code.function.name`, `http.request.body.size`, `http.request.method`, `http.response.body.size`, `http.response.status_code`, `http.route`, `server.address`, `server.port`, `url.path`, `url.scheme`, `user_agent.original`.
Chaves nos spans Hub: `code.function.name`, `dossie_produto.clientes.quantidade`, `dossie_produto.id`, `dossie_produto.produtos_contratados.quantidade`, `dossie_produto.unidades_tratamento.quantidade`, `simtr_hub.origem_dados`, `simtr_hub.simulador_dossie_produto_habilitado`.
Resource comum: `host.name`, `service.name`, `service.version`, `telemetry.sdk.language`, `telemetry.sdk.name`, `telemetry.sdk.version`, `webengine.name`, `webengine.version`.

O Hub existente contém identificador e contagens; A2 não remove nem filtra esses sinais.
O inventário persistido contém chaves, contexto e tipos técnicos, sem valores desses
atributos/resource. Logs selecionados são os eventos doctree.*, orquestrador.* e
simtr-hub.dossie-produto.* da janela do cenário; não é auditoria global de logs do SDK
ou de todos os campos sensíveis. A consulta usa fixture local: não houve chamada HTTP
a MTR externo nem Azure gerenciado. O teste de contrato MTR existente continua sendo
evidência separada do trecho Hub → cliente MTR.

### Lacunas frente a C0.4 e recortes prováveis

| Recorte | Estado observado / lacuna | Local provável do ajuste, sem implementação em A2 |
|---|---|---|
| B1: POST até entrada | SERVER automático existe com nome HTTP; faltam nome aprovado, iniciação, PRODUCER de envio e evento de publicação confirmada | Resource, IniciarMonitoramentoUseCase e MonitoramentoEntradaPublisher |
| B2: entrada até consulta/decisão | Sem CONSUMER, pré-validação ou avaliação; cada span Hub é root separado; decisão sem contexto | MonitoramentoEntradaListener, ProcessarTentativaMonitoramentoUseCase e PreValidacaoSimuladaAdapter |
| B3: resultado/Complete da entrada | Sem PRODUCER da saída ou CLIENT de settlement; só o log de settlement referencia HTTP | MonitoramentoResultadoPublisher e listener/log da entrada |
| B4: reagendamento | Sem span schedule/transação; próximas mensagens observadas sem carrier e novas consultas com traces próprios | MonitoramentoReagendamentoAdapter e listener |
| B5: saída até log | Sem CONSUMER, registro ou CLIENT do Complete; log final sem trace/span | MonitoramentoResultadoListener, ReceberResultadoMonitoramentoUseCase e adapter de log |

A ausência de eventos de falha nesses cenários de sucesso não prova lacuna de tratamento
de erro. A1 já inventariou ausência de provider/spans SDK nas operações reais.
C0.4, nomes doctree e C9.1-L preservados. Próximo item após o fechamento técnico A2:
desenho concreto da B1, com decisões de propagação/carrier e checkpoints ainda aplicáveis,
antes de alterar produção. A2 não instala provider nem antecipa B1–B5.

### Verificações e revisão

1. Controle negativo com exporter none: 1 teste, 1 falha esperada (captura 1 / observada 0),
   zero erros/ignorados. Executado antes do POST, comprovando que captura vazia não passa.
2. Primeira execução dos dois fluxos: 2 erros no polling bloqueante em thread parallel.
   Suporte corrigido para boundedElastic; ambos os cenários passaram (39,74s e 35,31s).
   Inventários parciais daquela execução com erro foram descartados como evidência.
3. Suíte opt-in completa com reforços de revisão: 40 testes em 10 classes, 39 passaram
   e somente a contagem literal incorreta de logs do Hub falhou (esperado 14 / real 15).
   Terminal reforçado passou em 28,89s. Não houve erro nem teste ignorado.
4. Corrigido exclusivamente 14 para 15; repetida a classe Hub inteira após recompilação:
   1 teste, zero falhas/erros/ignorados, 32,79s. Resultado de quarentena e inventário
   VALIDADO conferidos. Os 40 cenários estão validados pela execução completa mais essa
   repetição; não afirmar que a execução completa original passou ou que foi repetida.
5. Revisão independente concluída sem Critical/Required pendentes. Corrigidos polling,
   inventário parcial/antigo, prova do modelo final, relações de logs e contagem total.
   Sugestões fora do objetivo de tracing não ampliaram o recorte.

Comandos: `mvn -q -Pservicebus-integration test` e repetição
`mvn -q -Pservicebus-integration "-Dtest=MonitoramentoTelemetriaHubEmuladorTest" test`.
O checkpoint padrão/build/Sonar concluiu após a validação, conforme fechamento registrado
acima. Baseline original 217 preservado.


## Refinamento da prova A2 após execução e revisão — 2026-09-11

O controle negativo falhou com 1/0 spans; primeira execução dos fluxos teve dois erros
de suporte: block() após repetição no scheduler parallel. Corrigido o callable com
subscribeOn(Schedulers.boundedElastic()), conforme o padrão documentado do
[Reactor 3.4](https://projectreactor.io/docs/core/3.4.14/reference/index.html#faq.wrap-blocking);
a versão efetiva 3.4.41 foi confirmada na A1 e ambos os testes passaram após a correção.
Inventários das execuções com erro são parciais e não constituem evidência de conclusão.

Antes de fechar, reforçar somente os três testes/suporte já previstos:
spy observador na porta RegistrarResultadoMonitoramento, sem stub, delegando ao adapter
real de log. Capturar o modelo próprio do orquestrador depois do settlement para comprovar
resultado CONCLUSIVO/QUARENTENA, motivo e consultas, preservando log real/C9.1-L.
A porta pública do Hub continua sem mock/spy no cenário da fixture.
Relacionar cada trio de logs do Hub ao par trace/span exportado; congelar também pares
dos logs de decisão/settlement e ausência de trace e span no log final.
Inventário será gravado explicitamente só depois de todas as asserções; remover somente
o arquivo técnico do cenário em target no início, evitando sobra válida de execução antiga.
A revisão independente identificou esses reforços; não há mudança de produção ou escopo B.


## Execução autorizada de 10.1-A2 — 2026-09-11

Pedido humano: "Próxima subfatia: 10.1-A2". Executar somente caracterização do fluxo real.
Baseline original/assessment de 217 issues conferidos; partida COMPLIANT em A1,
fingerprint 626ff8925037d4bb9916307e3c7d64915a69d3ea8a5d779f2d14b39f220bd200.
Não reinicializar baseline nem instalar provider/instrumentação.

Desenho: até três arquivos de teste opt-in no pacote orquestrador.integracao.
MonitoramentoTelemetriaEmuladorTest cobre POST terminal com Hub controlado e observação
do contexto no callback, sem ativá-lo artificialmente. MonitoramentoTelemetriaHubEmuladorTest
usa fixture real 4324680 e wrapper CDI do Hub, sem mock/spy da porta, com política local
de três consultas e intervalos PT2S até quarentena. Ambos ativam os dois listeners.
SuporteTelemetriaMonitoramento concentra captura CDI/forceFlush/controle positivo,
JSON de arquivo real, espera limitada e inventário sanitizado. Reutiliza proteção do
ServiceBusEmuladorTestProfile, dados sintéticos e caminhos de log exclusivos por profile.

Provar POST 202 com traceparent sintético conhecido; terminal/3 consultas; reagendamento
observável por peek com sequência explícita; log final único e filas vazias depois do
settlement. Nunca adicionar consumidor concorrente das filas principais.
Capturar todos os spans da janela; observar nome/kind/scope/parent/links/status e chaves
de atributos/eventos/resource em memória, preservando somente inventário técnico sanitizado.
Comparar trace/span de logs de negócio e do Hub com a captura; IDs funcionais iguais não
substituem correlação. Campos antigos do Hub continuam preservados e delimitados no relatório.

Primeiro validar o suporte (controle negativo por exporter none e positivo com CDI),
depois fixar a caracterização observada e confrontar C0.4. Não ajustar ausência de tracing
de produção nesta subfatia. Integração opt-in completa, revisão independente e checkpoint
padrão/build/Sonar ao fechar. B1–B5 exigem desenho concreto posterior; A2 não os inicia.


## Fechamento técnico de 10.1-A1 — 2026-09-11

Decisão humana **ContinuarAjustes** registrada em 11/09/2026 às 08:19:51.
Aplicadas somente as duas correções no teste: referência de método para S1612 e recurso
sem nome no try-with-resources para S1481. Comparação integral com a versão anterior
confirmou somente essas duas substituições. Revisão independente confirmou equivalência
e preservação do fechamento de Scope antes do encerramento do span.

Após os ajustes, **11 cenários focados passaram**, sem falhas/erros/ignorados, às 08:23.
O novo checkpoint executou clean verify, scanner e Compute Engine:
**1.345 testes padrão em 192 classes**, sem falhas/erros/ignorados; build aprovado.
Sonar **COMPLIANT / NOT_REQUIRED**: **213 issues abertas**, baseline original **217**,
**zero issues novas ou graves**, cobertura **88,1%**, duplicação **4,4%**.
A suíte opt-in completa anterior aos dois ajustes de estilo passou com **38 testes em
oito classes**. Após as substituições, foi repetida a classe afetada inteira (11 cenários);
a suíte completa do emulador não foi repetida, pois o comportamento permaneceu igual.

Checkpoint: 2026-09-11T11:32:09.7129252Z (08:32:09 em São Paulo).
Análise: 2aef7b2d-0400-4831-a4ee-1b1cded783b9.
Compute Engine: 1d52ed90-c9c2-41e7-960a-29f7d142a660.
Fingerprint: 626ff8925037d4bb9916307e3c7d64915a69d3ea8a5d779f2d14b39f220bd200.
Baseline/assessment originais comparados integralmente com a referência de 9.1-C e
preservados. Fingerprint final conferido contra o checkpoint. Snapshot final:
.codex/.state/session-after-10-1-a1-compliant-20260911.json.
Decisão anterior preservada em .codex/.state/session-10-1-a1-continuar-ajustes-20260911.json.

**A1 concluída tecnicamente. Próxima subfatia: 10.1-A2**, ainda não iniciada.
O resultado da caracterização permanece: provider Azure ausente, zero spans SDK e
carrier recebido vazio nas operações observadas. O trecho Hub/MTR já instrumentado
ainda precisa ser relacionado ao trace anterior às filas. Não declarar 10.1/C3 completos.
Produção/dependências, C0.4/doctree, C9.1-L e baseline preservados; encerramento humano
da feature pendente. Sem staging/commit/push ou formatos derivados.



## Continuação autorizada após Sonar — 2026-09-11

Usuário escolheu explicitamente "ContinuarAjustes". Decisão registrada pelo script
validar-checkpoint-sonarqube.ps1 -HumanDecision ContinuarAjustes; estado
CONTINUE_ADJUSTMENTS preservado em .codex/.state/session-10-1-a1-continuar-ajustes-20260911.json.
Executar apenas as duas substituições propostas para S1612/S1481 no teste da A1.
Verificar os 11 cenários focados, revisar o diff e repetir o checkpoint padrão/build/Sonar.
A2 e mudanças de produção permanecem fora desta continuação. Baseline original preservado.


## Evidência da caracterização de 10.1-A1 — 2026-09-11

Único arquivo executável novo: `ServiceBusTelemetriaEmuladorTest`, 11 cenários opt-in.
Clientes principais injetados da fábrica; cliente auxiliar de DLQ fechado pelo teste;
proteção do profile do emulador reutilizada e dois listeners desabilitados. Sem alteração
em produção, dependências, exporter/sampler de produção ou testes existentes do Hub.

### Captura e provider

O Tracer CDI grava um span INTERNAL raiz, sampled, com contexto válido. Cada observação
ativa esse contexto somente no chamador, fecha o scope/span em finally, aguarda
`forceFlush().join(10, SECONDS)` com resultado conferido e examina a captura inteira,
sem filtro por nome/trace. Exige exatamente o controle, sem atributos/eventos/links,
e nenhum span adicional. A operação real precisa concluir para essa verificação passar.
Não há injeção de carrier nem scope artificial nos callbacks.

A primeira consulta imediata ao exporter retornou zero tanto com exporter none quanto
com cdi. Esse resultado inicial não era evidência válida de ausência do SDK. O suporte foi
corrigido para aguardar forceFlush, seguindo os testes existentes de telemetria.
O controle positivo então passou. Com esse suporte corrigido, o controle negativo com exporter none falhou exatamente em
expected 1 / actual 0 (1 teste, 1 falha, sem erros), e a repetição com cdi passou (1/0/0/0).

Resolução efetiva: Quarkus **3.33.2.1**, Azure Services **1.2.5**,
Service Bus **7.17.12**, Azure Core **1.55.5**, Reactor **3.4.41**, OTel API/sdk-testing
**1.57.0**. A árvore Maven não inclui `azure-core-tracing-opentelemetry`.
As duas pesquisas SPI em runtime (TracerProvider e Tracer legado) retornaram zero;
o tracer criado pelo provider padrão do Azure apresentou `isEnabled() == false`.
A fonte de DefaultTracerProvider confirma o fallback NoopTracer quando não há provider.
O Tracer CDI funcionar não implica que esse SPI Azure esteja instalado.

### Inventário observado no emulador

| Operação real | Escopo e efeito conferido | Spans SDK exportados |
|---|---|---|
| send | Entrada e saída; ACK, identidade/corpo sintéticos conferidos no recebimento | 0 |
| receive/process | Entregas das duas filas; callback sem SpanContext válido | 0 |
| Complete | Duas filas, mensagem removida após confirmação | 0 |
| Abandon | Duas filas, reentrega com mesma sequência, novo lock e delivery count maior; Complete final | 0 |
| DeadLetter | Duas filas, principal vazia e mensagem própria na DLQ; recebimento/Complete auxiliar | 0 |
| schedule sem transação | Entrada, sequência agendada visível; mensagem ativada e completada | 0 |
| createTransaction + schedule + Complete + commit | Entrada, estado após commit contém somente a próxima mensagem; ativação e Complete final | 0 |
| createTransaction + schedule + Complete + rollback | Entrada, mensagem original preservada/reentregue; próxima ausente após seu horário | 0 |
| peek e limpeza das mensagens próprias | Filas principais/DLQ; não descartar resíduos alheios | 0 |

Sem spans SDK não há nomes, kinds, scopes, atributos, status, parent ou links exportados
a inventariar para essas operações. As application properties recebidas foram verificadas
por inteiro e ficaram vazias, incluindo ausência de traceparent, tracestate e Diagnostic-Id.
O objeto enviado por send também permaneceu com application properties vazias.
Não se atribuiu propagação aos IDs AMQP. A captura não representa a aplicação completa,
o POST, o Hub nem seus logs; tampouco é prova de ausência de dados em logs internos do SDK.
As provas de horário/atomicidade detalhadas já existentes continuam na suíte de reagendamento.

### Confronto com as fontes da versão instalada

Fontes oficiais locais dos source-jars 7.17.12/1.55.5 foram extraídas somente em target,
sem dependência acrescentada nem implementação interna importada pelo teste.
ServiceBusTracer condiciona criação/injeção a isEnabled; possui ServiceBus.message,
ServiceBus.send/process/complete/abandon/deadLetter e scheduleMessage, além das operações
transacionais. Essas possibilidades estáticas não foram emitidas na captura.

Se habilitado, o código usa convenção anterior (messaging.system=servicebus,
messaging.operation e host/entity path), injeta contexto de mensagem e compatibilidade
Diagnostic-Id; a extração prioriza Diagnostic-Id sobre traceparent. Isso difere de C0.4
e demanda avaliação antes de qualquer habilitação. Também foi observado na fonte que
createTransaction do receiver chama traceMono com o nome ServiceBus.commitTransaction.
Nada disso foi corrigido ou tratado como comportamento exportado nesta A1.

**Consequência:** há lacuna demonstrada de instrumentação/propagação automática no SDK
instalado. A1 não escolhe instalar provider nem antecipar propagação manual.
A próxima subfatia é **10.1-A2**, que caracteriza POST/fluxo/Hub/log antes do desenho dos
ajustes. C0.4, nomes doctree e aceite C9.1-L preservados; C3/encerramento humano pendentes.

### Verificações executadas

- Focado: `mvn -q -Pservicebus-integration "-Dtest=ServiceBusTelemetriaEmuladorTest" test`:
  **11 testes, zero falhas/erros/ignorados**.
- Integração completa: `mvn -q -Pservicebus-integration test`:
  **38 testes em oito classes, zero falhas/erros/ignorados** (terminou às 08:02 de 11/09).
- Controle negativo final: comando focado no método
  `ServiceBusTelemetriaEmuladorTest#deveCapturarControlePositivoDoTracerCdi`,
  com `-Dquarkus.otel.traces.exporter=none`: **1 falha esperada**, zero erros/ignorados,
  especificamente captura esperada 1 / observada 0 após forceFlush.
- Mesmo método sem override de exporter: **1 teste passou**, às 08:03.
- Revisão independente de correção, arquitetura, segurança, simplicidade e desempenho:
  sem achados Critical/Required. Versão respaldada pela árvore Maven/source-jars;
  o literal fornecido ao createTracer é somente metadado, não prova a resolução.

### Primeiro checkpoint Sonar — NON_COMPLIANT (histórico)

Em 11/09/2026 às 08:06, `./validar-checkpoint-sonarqube.ps1` concluiu clean verify,
scanner e Compute Engine. **1.345 testes padrão em 192 classes, zero falhas/erros/ignorados**;
build aprovado. Estado técnico **NON_COMPLIANT / PENDING** por duas issues novas.
215 issues abertas contra baseline original 217; nenhuma HIGH/BLOCKER/CRITICAL,
cobertura **88,1%**, duplicação **4,4%**.

| Issue | Regra e severidade | Local no teste novo | Correção proposta, não aplicada |
|---|---|---|---|
| 017d8338-93d7-42bd-91b2-340a949d2c52 | java:S1612, MINOR / LOW manutenção | linha 88 | Usar `azureTracer::isEnabled` no lugar da lambda. |
| 00c3e560-fc7d-4dd0-bf01-b17eb19a802e | java:S1481, MINOR / LOW manutenção | linha 241 | Usar `try (var _ = controle.makeCurrent())`, mantendo fechamento automático. |

Issues consultadas pela API local e confrontadas com o código. O recurso sem nome segue
o padrão existente em ConsultaDossieProdutoObservabilidadeTest e outros testes do Hub.
Não remover makeCurrent/try-with-resources nem suprimir regras.

Análise: `adac179f-2ce1-44ba-ab85-d93651d154af`.
Compute Engine: `7b41c502-e2b5-4da2-b664-3a40a834b476`.
Fingerprint: `ed66f1694804a2ee4ff6ba0c797900b76e7e8c3c7c812e7b1347b43184e0b717`.
Baseline/assessment originais preservados, sem reinicialização. Snapshot:
`.codex/.state/session-after-10-1-a1-non-compliant-20260911.json`.

**Próxima ação naquele checkpoint:** decisão humana `Reprovar`, `AceitarExcepcionalmente` ou
`ContinuarAjustes`, conforme AGENTS.md. Recomendação: **ContinuarAjustes** para as duas
substituições acima, teste focado e novo checkpoint. Registrar a decisão escolhida com
`-HumanDecision` antes de prosseguir. Nenhuma decisão humana foi inferida.
Naquele checkpoint, caracterização entregue e fechamento de A1 pendente. A2 não iniciada. Sem commit/push.

### Esclarecimento sobre o rastreamento Hub/MTR

Pergunta humana em 11/09: o rastreamento inclui a consulta do dossiê no MTR?
O código existente já possui `simtr-hub.service.dossie-produto.consultar` (INTERNAL)
e `mtr.dossie-produto.consultar` (CLIENT). ConsultaDossieProdutoMtrContractTest verifica
mesmo traceId e parentage API → aplicação → cliente, usando stub HTTP do MTR.
Isso comprova esse trecho isolado, não a ligação com o POST antes do Service Bus.

O monitoramento chama a porta pública do Hub por SituacaoDossieHubAcl, via CDI no mesmo
processo. Com `simtr-hub.simulador.dossie-produto.habilitado=true` (padrão dev), usa fixture
local; com false, seleciona o adapter MTR. Exportação depende do profile configurado:
none no padrão, cdi nos profiles jaeger/grafana. A1 não executou MTR externo nem POST/Hub.
A2 continua responsável por caracterizar a ligação do fluxo real com o wrapper do Hub,
sem tratar mock dessa porta como prova da cadeia.


## Execução de 10.1-A1 autorizada — 2026-09-11

Pedido humano: "10.1-A1". Executar somente caracterização SDK/captura; A2 e produção
fora deste incremento. Baseline original de 217 issues recuperado da cópia final de 9.1-C
e comparado integralmente, inclusive baselineAssessment. Análise original
f6183a72-a2ea-44bc-9374-b2b064bdad55, READY/LOCAL_SONAR; sem InitializeBaseline.
Sessão anterior preservada em .codex/.state/session-before-10-1-a1-recovery-20260911.json.
Checkpoint corrente vazio até nova execução, sem atribuir análise antiga à A1.
Fingerprint antes do código: 5d84ce454d3e4ab5ba5aabb5b8148e0076ab4ef3a22a0c91c873a211719afd0d.
Nenhum pacote offline localizado; token herdado disponível sem exposição. Docker estava
parado e foi iniciado para a prova. Documentação e patch anteriores preservados.

Desenho: novo teste opt-in ServiceBusTelemetriaEmuladorTest, profile que reutiliza proteção
do emulador e fixa ambos os listeners false. Reutilizar exporter CDI existente.
Controle positivo; RED por exporter desabilitado apenas no comando, GREEN com cdi.
Depois caracterizar send/receive/Complete/Abandon/DeadLetter nas duas filas e
schedule/Complete/commit/rollback na entrada, com efeitos reais conferidos.
Fontes locais do SDK e árvore efetiva confrontadas com a captura. Não instalar provider,
agente, propagação ou span em produção. Dados sintéticos e diagnóstico seguro.
Suíte opt-in completa, revisão e checkpoint padrão/build/Sonar fecham o incremento.
Lacuna funcional que exija produção volta ao plano antes da alteração.


## Estado e intenção do planejamento inicial — 2026-09-11 (histórico)

Pedido humano: "retomamos pelo planejamento da 10.1". Esta entrega é exclusivamente
documental; caracterização executável e instrumentação não iniciadas. Próxima ação
proposta: **10.1-A1**, após solicitação de continuidade da execução.

HEAD e referência local de origin conferidos em `1f3246620821a4a3b3aad66a380c64b927d27e3a`,
na branch `feature/orquestrador-monitoramento-service-bus`. Não houve consulta ao remoto.
Preservar pausa local, `.codex-doc-alignment.patch`, `.env` e baseline original.
GO da feature e C0.4 continuam registrados; este planejamento não os revoga nem infere
novo GO. C9.1-L permanece aceito; C3 e encerramento humano continuam pendentes.

Objetivo: demonstrar, com spans exportados e logs reais, a correlação do POST até entrada,
consulta, reagendamento ou resultado, saída e submissão do log final. Cada operação deve
ter contexto/duração corretos, sem duplicação nem dados proibidos nos novos sinais.

## Contexto confirmado por leitura

Lidos: consolidado arquitetural, índice de ADRs, ADRs 0006/0010/0011/0012,
`tasks/README.md`, templates, plano/checklist e retomada. C0.4 foi aprovado em 2026-09-04
e exige caracterização do SDK antes de instrumentação manual.

| Evidência inspecionada | Consequência para 10.1 |
|---|---|
| `pom.xml`: Quarkus 3.33.2.1, BOM Azure Services 1.2.5, OpenTelemetry e sdk-testing | Reutilizar dependências. A matriz histórica registra SDK 7.17.12, Azure Core 1.55.5, Reactor 3.4.41 e OTel 1.57.0; reconfirmar resolução na etapa executável. |
| POMs locais da extensão 1.2.5 e SDK 7.17.12 | `azure-core-tracing-opentelemetry` aparece no POM do SDK com scope test; seu diretório não foi localizado no cache. Investigar provider efetivo, sem afirmar ausência de tracing no runtime. |
| Configuração de produção/teste e `InMemorySpanExporterProducer` | Produção usa exporter none por padrão; testes usam cdi/simple e já possuem exporter em memória. Injetar esse tipo sem alterar/importar o producer do Hub nem criar outro concorrente. |
| Publishers, mapper inicial, dois listeners e reagendador | Não há propagação/extração explícita pela aplicação; há pontes Reactor → CompletionStage → Mutiny. Memorização, transação e cancelamento já têm comportamento protegido. |
| Resource e casos de uso novos | Sem spans próprios declarados; isso não prova ausência do span HTTP automático. |
| Helpers de erro, log do listener e adapter de log final | Helpers consultam Span.current; logs copiam MDC. Testes de IDs/MDC existentes não comprovam cadeia distribuída. |
| `MonitoramentoResultadoEmuladorTest` | Prova fluxo funcional, log e Complete, sem inspecionar spans. O mock de ConsultarDossieProduto substitui também o wrapper observável do Hub. |
| `ConsultaDossieProdutoObservabilidade` e seu teste | Hub já emite `simtr-hub.service.dossie-produto.consultar`, identificador e contagens. Preservar seu código e sinais; registrar o limite da proibição de dados nos sinais novos. |

São evidências estáticas, não resultados de captura. OpenTelemetry presente no projeto
não comprova propagação AMQP, parentesco ou restauração de contexto em callbacks.

## Escopo e fora de escopo

Incluído: caracterizar SDK/fluxo, preencher somente lacunas comprovadas, testar correlação,
isolamento, erros, cancelamento e ausência de duplicação/dados proibidos nos novos sinais.

Preservar JSON v1, IDs AMQP, validações, respostas REST, políticas, ordem dos efeitos,
transação, memorização, flags false e fechamento dos clientes. Propagação de transporte
pertence aos adapters; SDK não entra no núcleo. A fábrica permanece técnica. OpenTelemetry
pode apoiar aplicação/domínio conforme os ADRs. Nomes aprovados com `doctree` permanecem.

Fora de escopo: Hub/dossie e seus testes existentes, novo serviço/dependência/upgrade,
Java agent, hooks globais Reactor, mudança de sampler/exporter de produção, novos destinos
de log, retry/timeout/readiness, persistência/Outbox, métricas/dashboards, Azure gerenciado,
reabrir C9.1-L, publicação Git e formatos derivados. Necessidade concreta fora desses
limites atualiza plano/checklist e recebe checkpoint correspondente antes da alteração.

## Contrato observável preservado de C0.4

| Operação | Nome e kind aprovados |
|---|---|
| HTTP | `simtr-hub.api.monitoramento-dossie.iniciar` — SERVER |
| Iniciação | `orquestrador.service.monitoramento-dossie.iniciar` — INTERNAL |
| Pré-validação | `doctree.service.prevalidacao.dossie.consultar` — INTERNAL |
| Avaliação | `doctree.service.monitoramento-mtr.avaliar` — INTERNAL |
| Registro | `orquestrador.service.monitoramento-dossie.resultado-registrar` — INTERNAL |
| Publicação / consumo | `send {fila}` — PRODUCER / `process {fila}` — CONSUMER |
| Reagendamento | `schedule q.prevalidacao.monitoramento-mtr.in` — PRODUCER |
| Settlement | `complete {fila}`, `abandon {fila}`, `dead_letter {fila}` — CLIENT |

Filas padrão: `q.prevalidacao.monitoramento-mtr.in` e `q.prevalidacao.monitoramento-mtr.out`.
Aproveitar o span HTTP existente sem adicionar outro SERVER equivalente. Inventariar
create/send e receive/process separadamente: funções distintas não significam duplicação.

Preservar os sete eventos previstos no [plano](plan.md): publicação confirmada/falhou;
decisão/processamento falhou/settlement; resultado registrado/falhou. Completar somente
ausências comprovadas no incremento correspondente, sem renomear eventos implementados.

Campos permitidos: IDs técnicos, tentativa funcional, delivery count, sequence number,
decisão, settlement, tipo técnico de erro, camada/componente/operação. Atributos de
mensageria seguem C0.4, incluindo `messaging.system`, `messaging.destination.name`,
`messaging.operation.name`, `messaging.operation.type` e `error.type`, quando compatíveis
com o SDK. Fixar nomes/tipos concretos após a caracterização; não converter DTO em atributos.

Contexto W3C manual em application properties somente se a propagação automática não for
comprovada. Observar eventual carrier legado, como Diagnostic-Id, sem presumir seu uso.
Definir precedência antes de alterar carriers; não duplicar nem sobrescrever contexto
silenciosamente. correlationId/IDs de negócio não substituem traceId/spanId. Não copiar
baggage ou propriedades arbitrárias para novos sinais/mensagens.

## Subfatias propostas

Cada subfatia fecha tecnicamente antes da seguinte. A estimativa antiga de até cinco
arquivos não descreve a Task 10 inteira: aplicar o limite por incremento executável.
Atualizações Markdown acompanham a evidência; helper necessário conta no limite.

### 10.1-A1 — caracterizar SDK e validar captura

**Entrega:** teste opt-in isolado com clientes reais da fábrica, listeners desabilitados,
emulador protegido por ServiceBusEmuladorTestProfile e exporter CDI existente.

**Aceitação:**

- Controle positivo captura span do Tracer CDI. Operação SDK confirmada sem spans pode
  resultar em "ausência observada"; lista vazia sozinha não valida o harness.
- Inventário de send, receive, schedule, Complete, Abandon e DeadLetter: quantidade,
  nomes/kinds/scopes, atributos/status, parent/links e chaves de propagação. Incluir o
  caminho transacional utilizado, distinguindo ACK de commit.
- Confrontar resolução/provider/fontes efetivos; não instalar provider, agente ou
  instrumentação manual de produção para melhorar o resultado inicial.

**Verificação:** dados sintéticos, operações realmente confirmadas, captura antes/depois
e esperas limitadas. Inspecionar spans completos em memória; persistir somente evidência
sanitizada, sem dump de propriedades AMQP. Controle positivo falho exige corrigir captura
antes de concluir ausência de tracing. Suíte opt-in e checkpoint conforme seção Sonar.

**Dependências:** continuidade executável solicitada e início Sonar seguro.
**Arquivos prováveis:** novo
`src/test/java/br/gov/caixa/simtr/arquitetura/infraestrutura/servicebus/ServiceBusTelemetriaEmuladorTest.java`,
com profile/helpers locais; no máximo um suporte adicional. Produção intacta.

### 10.1-A2 — caracterizar fluxo real e identificar lacunas

**Entrega:** POST com traceparent sintético conhecido, ambos os listeners ativados pelo
profile, entrada, consulta, reagendamento, saída e log.

**Aceitação:**

- Matriz relaciona spans exportados/logs por traceId/spanId/parent/links, explicitando
  perdas ou duplicações. IDs de negócio iguais não bastam.
- Hub controlado cobre terminal direto; prova separada usa fixture real 4324680 do
  simulador pela porta pública, sem mock dessa porta, para observar o wrapper do Hub.
  Política curta local permite Rascunho até limite/quarentena e log final.
- Inventário confrontado com C0.4, com localização provável dos ajustes. Caracterização
  do estado anterior fica separada dos critérios finais de correlação.

**Verificação:** exporter e JSON real, peek com sequência explícita, sem consumidor
concorrente das filas principais. Observar callbacks/contexto sem ativá-lo artificialmente
no teste. Identidades/capturas próprias por cenário; verificar suite opt-in e checkpoint.

**Dependências:** A1. **Arquivos prováveis:** novo
`src/test/java/br/gov/caixa/simtr/orquestrador/integracao/MonitoramentoTelemetriaEmuladorTest.java`
e, se necessário, teste separado para o profile da fixture real; até três arquivos.
Reutilizar o desenho de 9.1 sem modificar seus cenários existentes.

### Checkpoint após A1/A2

C0.4 já autoriza preencher lacunas; não pedir reaprovação dos mesmos nomes/limites.
Convenção incompatível, dependência nova, mudança de contrato/carrier, capacidade
transversal ou exceção de segurança exigem proposta concreta e decisão humana
**antes do ajuste afetado**. ADR novo somente para decisão arquitetural nova, como Proposto.

### 10.1-B — preencher somente lacunas demonstradas

As linhas indicam ordem e candidatos, não obrigação de criar spans em todos os pontos.
Após A2, registrar o desenho concreto de cada subfatia antes do RED. Ponto já coberto
automaticamente é preservado e testado.

| Subfatia | Entrega e aceitação | Arquivos executáveis candidatos / dependência |
|---|---|---|
| B1 — POST até entrada | HTTP aproveitado, iniciação/envio correlacionados, logs seguros e 202/falhas preservados. | Estimativa refinada em [B1.1–B1.5](desenho-10-1-b1.md): até cinco arquivos executáveis por incremento, incluindo regressões A2; A2/CP-B1 aplicável. |
| B2 — entrada até consulta/decisão | Contexto alcança pré-validação/porta pública do Hub; nomes internos e decisões preservados. | MonitoramentoEntradaListener, ProcessarTentativaMonitoramentoUseCase, PreValidacaoSimuladaAdapter e até dois testes; B1. |
| B3 — resultado até Complete da entrada | Contexto na saída, duração/status corretos de envio/settlement e efeitos únicos. | MonitoramentoResultadoPublisher, listener/log da entrada e até dois testes; B2. |
| B4 — reagendamento | Contexto segue na próxima tentativa; schedule/Complete/commit/cancelamento preservados, sem span aberto esperando o horário do broker. | MonitoramentoReagendamentoAdapter, listener e até dois testes; B3. |
| B5 — saída até log | Contexto alcança registro/settlement, trace válido no JSON final e limite C9.1-L preservado. | MonitoramentoResultadoListener, ReceberResultadoMonitoramentoUseCase, adapter de log e até dois testes; B4. |

**Verificação por linha:** RED da lacuna, GREEN mínimo, revisão/refactor, testes focados sem
broker e checkpoint ao fechar incremento. Subdividir se ultrapassar cinco arquivos.
Não criar helper transversal antecipadamente nem importar o helper do Hub.

**Lifecycle:** contexto por operação/entrega, nunca no singleton/startup. Scopes fechados
no callback/thread correspondente. Provar término em sucesso/erro/cancelamento e
reassinaturas sem vazamento, span pendente, segundo envio ou segundo settlement.
Serialização deve receber contexto para correlacionar seu erro. Não registrar
Throwable/mensagem externa automaticamente por annotation ou recordException.

### 10.1-C — regressão integrada e evidência para C3

**Entrega/aceitação:** matriz abaixo passa com instrumentação final e clientes/listeners
reais; suite padrão continua sem broker. Relatório distingue emulador de Azure gerenciado,
sem registrar aceite de C3 ou encerramento humano.

**Verificação:** integração opt-in completa, suite padrão/build/Sonar e revisão de
correção, simplicidade, arquitetura, segurança, desempenho, testes e escopo.
Guias/consolidado atualizados somente conforme estado implementado.
**Dependências:** B1–B5 aplicáveis. **Arquivos prováveis:** testes A1/A2 e até dois novos
testes de contrato/isolamento. Defeito de produção exige recorte próprio antes da correção.

## Matriz mínima de aceitação final

| Cenário | Evidência exigida |
|---|---|
| POST terminal | Trace alcança duas filas/log; parent/links demonstram a cadeia, com kinds aprovados. |
| Reagendamento até conclusão/limite | Span por entrega, IDs/prazo/tentativa preservados, continuidade após ativação e nenhum span aberto durante espera no broker. |
| No-op/quarentena anterior ao Hub | Só operações executadas geram spans; decisão funcional não vira erro técnico. |
| Abandon/redelivery | Nova entrega observável sem trocar tentativa funcional; distinguir reentrega de retry SDK, sem conclusão antecipada. |
| Contrato inválido nas duas filas | Diagnóstico/DeadLetter correlacionados quando há trace válido, sem corpo ou valor rejeitado. |
| Falha de envio/transação/settlement | ERROR/error.type controlados na operação falha, sem Throwable externo, falso sucesso, rollback indevido ou segundo settlement. |
| Contexto ausente/inválido e flags | Não herdar trace anterior nem criar motivo funcional novo de DLQ; preservar contexto/flags válidos conforme sampler vigente, sem mudar configuração. |
| Mensagens intercaladas/cancelamento | Isolar traces/IDs/MDC nas duas bordas e após troca de thread; terminar spans e preservar memorização/cancelamento. |
| Dados proibidos | Examinar atributos/eventos/status/resource/links e logs completos com sentinelas. Baggage, payload, credencial, namespace e IDs de negócio ausentes nos novos sinais. |
| Hub real via simulador | Wrapper existente pertence à cadeia; sinais anteriores iguais. Mock da porta pública não substitui a prova. |

A cadeia contínua de C0.4 permanece exigida. Se o SDK produzir apenas links entre traces
distintos, apresentar a diferença; não chamar esse resultado de mesmo trace nem impor
outro modelo silenciosamente.

## Riscos e divergências

| Risco | Controle |
|---|---|
| Provider ausente confundido com perda de contexto | Controle positivo/resolução/SDK em A1; dependência nova somente com proposta aprovada. |
| Convenção corrente diverge do SDK fixado/C0.4 | Registrar versões/operações; manter nomes históricos e submeter incompatibilidade concreta. |
| Perda de contexto ou fim prematuro nas pontes reativas | Testar outra thread, confirmação pendente, cancelamento e reassinatura. |
| Duplicação de spans HTTP/SDK | Comparar inventário anterior/posterior e distinguir operações técnicas diferentes. |
| Sinais existentes do Hub versus proibição nos sinais novos | Hub já emite identificador/contagens. Não remover/filtrar esses sinais nem afirmar ausência desses campos no trace completo; alcance global exigiria mudança explícita de escopo/segurança. |
| SDK emite namespace ou exceção original | Auditar sinais automáticos; achado exige proposta antes de habilitar/corrigir emissão. Não resolver por filtro global no Hub. |
| Captura filtrada esconde ausência/vazamento | Inventariar captura inteira por origem; selecionar trace/cenário depois de verificar controle e campos proibidos. |
| Consolidado tem frase antiga sobre listener futuro na seção do contrato de entrada | Código e seção específica confirmam implementação. Registrar inconsistência; alinhar pontualmente na consolidação posterior, sem reabrir implementação. |

## SonarQube e comandos previstos

No planejamento inicial exclusivamente Markdown: não inspecionar sonar/, credencial/estado Sonar nem executar
Maven/baseline/checkpoint. Números de 9.1 são evidência histórica.

Na continuidade executável, conferir/preservar a referência original registrada na
retomada, sem reinicializar. A1 altera testes e está sujeita ao fluxo de código/tooling do
AGENTS.md. Se a sessão perdeu a referência, recuperar somente evidência verificável
conforme histórico; nunca atribuir análise antiga a código novo. Se for necessária
inicialização nova e existirem pacotes offline, fonte escolhida pelo usuário.
Token somente herdado do launcher, nunca no chat/arquivo/argumento/log.
Indisponibilidade permanece limitação técnica.

Comandos previstos no planejamento inicial; resultados executados em A1 registrados acima:

```powershell
mvn -q -Pservicebus-integration "-Dtest=ServiceBusTelemetriaEmuladorTest" test
mvn -q -Pservicebus-integration "-Dtest=MonitoramentoTelemetriaEmuladorTest" test
mvn -q -Pservicebus-integration test
./validar-checkpoint-sonarqube.ps1
```

Definir testes focados sem broker pelo conjunto realmente alterado em cada B.
Suites/checkpoint sequenciais; análise completa por incremento coerente, não por edição.
Checkpoint executa suite padrão/build. Manter cobertura mínima 85%, duplicação máxima 5%,
análise de issues novas/graves e decisão humana se NON_COMPLIANT conforme AGENTS.md.
Não prometer COMPLIANT novo com base no marco de 9.1.

## Referências oficiais consultadas em 2026-09-11

- [Quarkus Azure Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/index.html)
  como entrada e [Service Bus](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html)
  para builder CDI/Dev Services; exemplos não substituem PEEK_LOCK/adapters aprovados.
- [Quarkus OpenTelemetry 3.33](https://quarkus.io/version/3.33/guides/opentelemetry-tracing/):
  instrumentação HTTP/CDI/Mutiny e propagação; conferir comportamento das pontes no runtime efetivo.
- [Tracing no Azure SDK Java](https://learn.microsoft.com/en-us/azure/developer/java/sdk/tracing):
  provider/agent e contexto explícito assíncrono orientam investigação; não autorizam nova dependência.
- [Convenções Azure Messaging](https://opentelemetry.io/docs/specs/semconv/messaging/azure-messaging/):
  identificadas como Development na consulta. Comparar com SDK/C0.4 sem atualizar nomes/kinds automaticamente.

## Resultado do planejamento inicial (histórico)

Planejamento registrado; caracterização executável/implementação não iniciadas. Somente
Markdown da feature alterado. Próxima ação proposta: 10.1-A1. C0.4/C9.1-L preservados;
C3/encerramento humano pendentes. Sem novo commit/push.
