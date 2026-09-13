# Execução de 10.1-B1.3 — envio inicial e W3C

## Estado final — 2026-09-11

**B1.3 concluída tecnicamente.** Após ContinuarAjustes humano, S1130 corrigida e
checkpoint COMPLIANT / NOT_REQUIRED. Próximo item: B1.4.

Pedido humano **"10.1-B1.3"**, sob o GO **"go"** do
[desenho/CP-B1](desenho-10-1-b1.md). Execução somente desta subfatia.
Baseline LOCAL_SONAR existente e assessment comparados integralmente ao snapshot
COMPLIANT B1.2; fingerprint inicial
`4d2504a12063a58bcfa25d77af573501b942c2e248b6c0a30dd1a447f6016dd3`.
Sem pacotes em sonar/; credencial herdada, sem reinicialização do baseline.

## Recorte e verificações

Quatro arquivos executáveis: MonitoramentoEntradaPublisher, seu teste funcional,
novo MonitoramentoEntradaPublisherSpansTest e SuporteTelemetriaMonitoramento.
Produção reutiliza Tracer CDI, configuração input-queue, W3CTraceContextPropagator
e política local de callbacks caracterizada em B1.2. Scope por bloco síncrono,
sem alteração global ou extração no consumidor. Span PRODUCER antes de serializar,
fim no ACK/falha e operação upstream memorizada por invocação.

RED/GREEN de parent, timing, carrier exato, tracestate/flags, invalidade e ausência
de gravação; erros de preparação/serialização/SDK; cancelamento e contextos distintos.
Regressão OrquestradorEntradaLogTest e B1.2; A2 com 3/6 spans, logs 2/15,
primeira mensagem com W3C e reagendamentos sem carrier. Revisão e checkpoint final.
Novos eventos de publicação pertencem a B1.4. Sem mudanças de contrato JSON/AMQP
preexistente, fábrica, mapper, dependências, configuração operacional ou Hub.

Limitações B1.2 de callbacks externos e divergência documental sobre listener
futuro preservadas e registradas; não ampliar a correção neste recorte.
Implementação, testes, revisão e checkpoint final concluídos; o primeiro checkpoint
NON_COMPLIANT e a decisão humana estão preservados no histórico abaixo.

## RED, GREEN e revisão

- RED inicial: novo teste compilou e falhou na ausência de PRODUCER gravando na chamada
  SDK, antes de qualquer alteração de produção.
- GREEN inicial: prova do PRODUCER e os nove testes funcionais do publisher passaram.
- Matriz final focada: **66 testes** — 19 PublisherSpans, 9 publisher funcional,
  6 OrquestradorEntradaLog, 15 iniciação e 17 HTTP; zero falhas/erros/ignorados.
- As provas adicionais de erro, contexto e cancelamento passaram com a produção
  estabilizada; não houve alteração de contrato para acomodar testes.
- Revisão independente pela skill code-review-and-quality e revisão local:
  nenhum Critical/Required na solução final. Os gaps provisórios de testes e suporte
  foram preenchidos. O revisor não executou Maven nem alterou arquivos.

### Implementação conferida

PRODUCER `send {fila}` recebe pai na invocação e nasce somente na primeira assinatura,
antes de serializar. Contexto explícito na preparação, chamada/assinatura SDK e término.
A política local ThreadContext envolve a construção inteira do Uni e sua memoização,
como em B1.2. Um future local permite fechar o Scope antes do callback terminal,
inclusive no ACK síncrono; não bloqueia nem cancela o upstream quando observadores cancelam.

Os quatro atributos aprovados são os únicos no sucesso. Falhas acrescentam somente
error.type controlado e status ERROR: SERIALIZACAO_ENTRADA,
FALHA_PREPARACAO_PUBLICACAO ou FALHA_PUBLICACAO. Tipos/identidade de erros de preparação
são preservados; SDK mantém IllegalStateException de mensagem fixa sem causa/suppressed.
Serialização mantém seu único evento e id/código, agora correlacionados ao PRODUCER,
conferidos no JSON real. Sucesso e demais falhas ainda não emitem logs B1.4.

W3CTraceContextPropagator injeta exclusivamente traceparent/tracestate do span criado,
sem baggage/Diagnostic-Id. Testes verificam contexto válido não gravável com sampler
local alwaysOff (sem modificar o CDI), inválido com tracer noop, flags do always_on
CDI inclusive com parent 00, tracestate presente/ausente e invocações intercaladas.
Scopes restauram o contexto gravável/não gravável e o contexto inválido nas provas locais.

### Fontes verificadas

- [Quarkus Azure Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/index.html)
  e [Service Bus](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html):
  extensão 1.2.5 / SDK 7.17.12 e fábrica existentes, sem mudanças.
- [API Java de propagação](https://opentelemetry.io/docs/languages/java/api/#contextpropagators).
  Fonte instalado de W3CTraceContextPropagator em opentelemetry-api 1.57.0 conferido:
  valida SpanContext, usa suas flags e omite tracestate vazio; não exige isRecording.
- Quarkus 3.33.2.1, Mutiny 3.1.1 e SmallRye Context Propagation 2.3.0 preservados.
  O diagnóstico local e o limite dos callbacks externos permanecem em
  [B1.2](execucao-10-1-b1-2.md); não há correção global do provider nesta subfatia.

## Integração A1/A2 — validada

Comando: `mvn -q -Pservicebus-integration "-Dtest=ServiceBusTelemetriaEmuladorTest,MonitoramentoTelemetriaEmuladorTest,MonitoramentoTelemetriaHubEmuladorTest" test`.
**13 testes, zero falhas/erros/ignorados**: A1 11 (64,31s), terminal A2 1 (31,58s),
fixture Hub A2 1 (34,02s). Captura CDI positiva/flush e inventários VALIDADO,
com resultados esperados e filas vazias ao concluir. A1 preserva a caracterização
do SDK/provider sem spans/carrier automáticos, sem alteração do teste.

| Cenário A2 | Spans / logs | Resultado |
|---|---|---|
| Terminal | 3 / 2 | CONCLUSIVO / CONFORME; uma tentativa |
| Fixture Hub real | 6 / 15 | QUARENTENA / MAXIMO_TENTATIVAS; três consultas |

Terminal: trace `efd917faf39342cda6a80d607d043c9c`;
SERVER `12d9d42a63d67d91` → INTERNAL `ba6e2d40843393d7` →
PRODUCER `aaa3c453c9beb4ad`. Mensagem inicial ACTIVE, sequência 1, somente
`traceparent=00-efd917faf39342cda6a80d607d043c9c-aaa3c453c9beb4ad-01`.
O spanId do carrier corresponde ao PRODUCER, não ao SERVER/INTERNAL.

Fixture: trace `bdc2508d22f44a81a27875228eb6309d`;
SERVER `f49392769822eaee` → INTERNAL `30dc4602418b2da8` →
PRODUCER `cf5999b7a5de6899`. Parent remoto de ambos os SERVER:
`2222222222222222`. Spans manuais UNSET, sem eventos/links/descrição,
PRODUCER com os quatro atributos aprovados e duração contida no INTERNAL.

Três consultas Hub permanecem raízes em traces distintos, com seus logs próprios.
Settlement mantém o par HTTP; decisões e log final continuam sem contexto.
Nenhuma alteração inesperada nas relações dos logs. Na fixture foram observadas
tentativa 2 SCHEDULED (seq.2), tentativa 3 SCHEDULED (seq.4) e ACTIVE (seq.5),
todas sem carrier. A primeira mensagem da fixture não foi observada pelo peek;
a prova do carrier inicial no broker é a terminal. Não ampliar essa evidência
para a futura prova dedicada B1.5 nem para Azure gerenciado.

Suíte opt-in completa reservada à B1.5; esta execução repetiu A1 e as duas A2 afetadas.
Os inventários em target foram conferidos antes do clean do checkpoint; seus resultados
e relações relevantes estão preservados acima.

## Primeiro checkpoint do incremento — histórico NON_COMPLIANT

`./validar-checkpoint-sonarqube.ps1` concluiu Maven/build/Compute Engine.
**1.377 testes em 193 classes**, zero falhas/erros/ignorados; BUILD SUCCESS.
Situação técnica **NON_COMPLIANT**, decisão **PENDING**.
214 issues abertas / baseline original 217; uma nova, nenhuma HIGH/BLOCKER/CRITICAL.
Cobertura **88,2%** e duplicação **4,4%** cumprem os limites.

Única issue nova: `04ac90dc-eb34-4d89-ae9a-ac0e0074c751`,
`java:S1130`, MINOR / maintainability LOW, no método
`deveAbrirProducerAntesDoEnvioEPropagarSeuContextoAteOAck`,
MonitoramentoEntradaPublisherSpansTest.java:113. Declara `throws Exception`,
mas o corpo não lança checked exception. Ajuste proposto: remover somente a declaração,
rodar o teste focado e repetir o checkpoint, sem alterar produção ou critérios.

- checkedAt: `2026-09-11T18:57:14.0320913+00:00` (15:57:14 em São Paulo).
- Analysis key: `5be1a812-b64c-4325-9d7f-7c8b7e623594`.
- Compute Engine: `e07bd4d4-40a7-41fb-bfa1-7d2198271ff5`.
- Fingerprint: `8aed5d485743cbeeead02e6e14ebe00579c3eecfe96b0820afe356e0174ac3fb`.

Após a apresentação da issue e do ajuste concreto, o usuário respondeu
**"ContinuarAjustes"**. Registrado por
`./validar-checkpoint-sonarqube.ps1 -HumanDecision ContinuarAjustes`.
Removida somente a declaração throws Exception do teste apontado; corpo, asserções,
produção e integração preservados. Os 19 testes focados passaram em 2,694s;
o checkpoint completo também passou novamente, conforme fechamento abaixo.
Baseline e assessment comparados integralmente com B1.2 e preservados; fingerprint
corrente igual ao checkpoint. Snapshot:
`.codex/.state/session-after-10-1-b1-3-noncompliant-pending-20260911.json`.
Esse primeiro checkpoint ficou aberto até o ajuste autorizado; o fechamento final
está registrado abaixo. B1.4 não foi iniciada.
Sem staging/commit/push; C3 e encerramento humano da feature permanecem pendentes.

## Fechamento técnico após ContinuarAjustes

- Correção restrita a retirar throws Exception de um método de teste; corpo,
  asserções e os três demais arquivos executáveis da B1.3 preservados.
- **19 testes focados** aprovados; **1.377 padrão/193 classes** aprovados novamente,
  zero falhas/erros/ignorados e BUILD SUCCESS.
- A evidência anterior de **66 focados e 13 integrações A1/A2** permanece válida:
  produção, suporte e cenários de integração não mudaram neste ajuste.
- Revisão final da alteração de uma linha e diff sem erros de whitespace;
  revisão independente da implementação já concluída, sem achados obrigatórios.
- S1130 `04ac90dc-eb34-4d89-ae9a-ac0e0074c751` consultada: **CLOSED / FIXED**,
  atualizada em `2026-09-11T16:28:51-03:00`.

| Evidência final | Resultado |
|---|---|
| checkedAt | `2026-09-11T16:29:23.6972697-03:00` |
| Situação / decisão | COMPLIANT / NOT_REQUIRED |
| Issues abertas / baseline | 213 / 217 |
| Novas / HIGH, BLOCKER, CRITICAL | 0 / 0 |
| Cobertura / duplicação | 88,2% / 4,4% |
| Analysis key | `4e421065-520a-48b8-93c0-2f9f30ecc741` |
| Compute Engine | `0e50a705-af93-4e6c-b295-3bf26c03d88c` |
| Fingerprint | `ab2d6fa84161732355b713c3809bc5594e61d4fb91850e2e04a4790c566f9cca` |

Baseline e assessment originais comparados integralmente com B1.2 e preservados.
Fingerprint do código igual ao checkpoint; snapshot final:
`.codex/.state/session-after-10-1-b1-3-compliant-20260911.json`.
As atualizações Markdown posteriores não alteram o fingerprint executável.

**B1.3 concluída tecnicamente; próximo item B1.4.** GO do desenho/CP-B1 já recebido.
Sem staging/commit/push. C3 e encerramento humano da feature permanecem pendentes;
não houve aceite excepcional nem conclusão humana inferida.
