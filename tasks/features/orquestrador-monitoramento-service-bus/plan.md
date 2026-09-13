# Plano: implementar orquestração de monitoramento com duas filas do Service Bus

## CP-COSMOS recebido; P1 em validação — 2026-09-12

GO humano: "GO para o desenho e implementação incremental", reiterado "GO ao ADR-0013".
ADR-0013 Aceito; o registro anterior de espera está superado. P1 adicionou a extensão,
perfis local/DES e teste opt-in; quatro provas Cosmos aprovadas, incluindo ETag/batch/rollback.
Regressão conjunta e checkpoint em andamento. Ainda não existe persistência operacional
nem rastreamento completo. [Execução P1](../rastreabilidade-fluxo-dossie-cosmos/execucao-p1.md).
B2–B5 continuam coordenados com P6–P8. Sem staging, commit ou push.

## Ampliação humana: Cosmos DB, situações, mensagens e DLQ — 2026-09-12

O usuário confirmou estado durável em Cosmos DB, extensão Quarkus, Dev Services local e
configuração posterior do Cosmos real de DES. Container novo doctree; chave de negócio
idDossiePreValidacao. Incluir situação/etapa, mudanças, mensagens enviadas, filas, agendamento,
situações Pré-Valida/MTR, quarentena e DLQs com motivos.
[Plano específico](../rastreabilidade-fluxo-dossie-cosmos/plan.md),
[dados/transições](../rastreabilidade-fluxo-dossie-cosmos/especificacao.md) e ADR-0013 Aceito após o GO humano.
A alternativa inicial sem banco foi superada pelo pedido humano. O GO posterior aprovou
arquitetura e consistência; a extensão e o gate local foram implementados em P1.
Persistência operacional e trace completo continuam pendentes no checklist Cosmos.

Complemento B1.5: DevServicesTest e A1 ajustados para preservar lifecycle de receive/peek;
10 repetições DevServices e 13 focados aprovados. Regressão final: 41 testes/11 classes,
zero falhas/erros/ignorados. Checkpoint final COMPLIANT: 1.403 testes padrão/194 classes,
213 issues (0 novas/graves), cobertura 88,3% e duplicação 4,4%; baseline original preservado.
B1.5 e B1 consolidados tecnicamente. [Evidência final](execucao-10-1-b1-5.md#complemento-final-da-regressão-e-consolidação-técnica--2026-09-12).
A revisão do desenho Cosmos terminou sem bloqueadores e o CP-COSMOS humano foi recebido; seguir o checklist Cosmos vigente.
Os blocos posteriores preservam marcos históricos e suas pendências à época; este estado prevalece.
B2–B5 permanecem pendentes e serão coordenados com P6–P8 do novo plano. Sem commit/push.


## Continuidade solicitada: rastreabilidade completa e Jaeger — 2026-09-12

O usuário solicitou habilitar o Jaeger e completar a rastreabilidade, incluindo localização
nas filas e investigação de Dead Letter. [Escopo, ordem, critérios e limites](rastreabilidade-completa-jaeger.md).
Próximo incremento: estabilização complementar B1.5 em ServiceBusDevServicesTest;
depois B2–B5, prova real Jaeger e diagnóstico de leitura das filas/DLQs.
C0.4/CP-B1 preservados; detalhes novos passam pelo checkpoint aplicável. Sem commit/push.


## 10.1-B1.5 — prova validada; consolidação pendente — 2026-09-12

Timeout pós-Complete reproduzido e corrigido somente no harness B1.5: identidade e
igualdade antes de Complete; peeks finais com receive link ativo; cancelamento depois.
Dez repetições passaram após o ajuste. Prova permanente e revisão final aprovadas:
3 spans relacionados, W3C exato do PRODUCER, 1 log confirmado, contrato preservado.
[Diagnóstico, inventário, regressão e checkpoint](execucao-10-1-b1-5.md).

Checkpoint **COMPLIANT / NOT_REQUIRED**: 1.403 testes padrão/194 classes e build
aprovados; 213 issues, zero novas/graves, cobertura 88,3%, duplicação 4,4%.
Baseline original recuperado do snapshot B1.4 após reset pelo hook e preservado.

Suíte opt-in: 41 testes, 0 falhas, 1 erro em ServiceBusDevServicesTest preexistente;
A1/A2/B1.5 passaram. Repetição focada DevServices + B1.5 passou, mas não corrige a
corrida preexistente: esse teste cancela receive antes de Complete.
A estabilização dessa regressão fica registrada como pendência explícita de B1.5,
sem alterar outro arquivo de teste silenciosamente. Proposta mínima na execução.
Não declarar a execução ampla inteiramente aprovada nem encerrar B1 com base no rerun.

Rastreabilidade completa ainda ausente: só POST → iniciação → envio inicial está
correlacionado. B2–B5 e validação real no Jaeger permanecem pendentes, assim como
10.1-C/C3 e encerramento humano. Sem staging, commit, push ou derivados.

## 10.1-B1.5 — pausada, em andamento — 2026-09-11

Pedido humano **10.1-B1.5**, sob o GO do desenho/CP-B1.
Prova dedicada POST → broker em um novo teste de integração; produção e suporte A2
preservados. Baseline/checkpoint B1.4 conferidos antes de qualquer alteração executável.
[Recorte, critérios e verificações](execucao-10-1-b1-5.md).
Pausa humana solicitada; retomar pelo diagnóstico do timeout no peek após Complete.
Prova final, suíte opt-in completa (incluindo A1/A2), revisão e checkpoint pendentes.
B2–B5 e C3 continuam pendentes.

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

## 10.1-B1 — desenho registrado — 2026-09-11

Pedido humano "desenho da 10.1-B1" atendido em [desenho-10-1-b1.md](desenho-10-1-b1.md).
Recorte: SERVER HTTP existente, iniciação, PRODUCER do envio inicial, carrier W3C e
logs de publicação. Cinco incrementos, com até cinco arquivos executáveis por etapa
e evolução explícita das regressões A2. Implementação ainda não iniciada.

**Próximo item executável: 10.1-B1.1**, contratos do lifecycle atual e preparação das
regressões, somente testes. CP-B1 registra os detalhes novos de carrier, atributos/erros
e falha de logging para decisão humana antes dos ajustes de produção afetados.
C0.4 e C9.1-L preservados; nenhuma nova decisão humana inferida.

Esta entrega alterou somente Markdown da feature. A evidência técnica abaixo pertence
à A2; não houve Maven, inspeção Sonar ou novo checkpoint no desenho. C3 e encerramento
humano permanecem pendentes. Sem staging/commit/push ou formatos derivados.
Os registros de execução abaixo conservam os marcos anteriores.

## 10.1-A2 — concluída tecnicamente — 2026-09-11

Caracterização do POST, dois listeners, wrapper real do Hub, reagendamento e log final
entregue em três arquivos de teste. No cenário da fixture, cada consulta do Hub abre
um trace próprio; seus logs acompanham a consulta, mas o log final fica sem trace/span.
O settlement referencia o span HTTP: correlação parcial, sem cadeia distribuída completa.
Produção e dependências preservadas. [Matriz, inventário e verificações](continuidade-10-1.md).

Validados 40 cenários opt-in em 10 classes: execução completa com 39 aprovados e uma
falha na contagem do teste; correção exclusiva 14 → 15 e repetição da classe afetada aprovada.
Revisão independente sem pendências obrigatórias. Checkpoint: 1.345 testes padrão em
192 classes e build aprovados; Sonar **COMPLIANT / NOT_REQUIRED**, 213 issues,
nenhuma nova ou grave, cobertura 88,1%, duplicação 4,4%. Baseline original 217 preservado.
Fingerprint c82d12e2cdc14d7afaa4ab828690898b4eca95cdff4d4784327ccc980a768ee1 conferido.

**Próximo item: desenho concreto de 10.1-B1**, ainda não iniciado. C0.4/doctree e
C9.1-L preservados; C3 e encerramento humano pendentes. Branch
feature/orquestrador-monitoramento-service-bus; HEAD/origin local em 1f32466.
Pausa, patch e .env preservados; sem staging/commit/push ou formatos derivados.
Os registros abaixo são históricos.


## 10.1-A1 — concluída tecnicamente — 2026-09-11

Usuário escolheu ContinuarAjustes; decisão registrada. Aplicadas somente as duas
correções S1612/S1481 no novo teste, com comportamento e fechamento do scope preservados.
Após os ajustes: 11 focados e 1.345 testes padrão em 192 classes passaram, sem falhas,
erros ou ignorados; build aprovado. Sonar **COMPLIANT / NOT_REQUIRED**:
213 issues, nenhuma nova ou grave, cobertura 88,1%, duplicação 4,4%.
Baseline original 217 preservado. A suíte opt-in completa anterior aos ajustes de
estilo passou com 38 testes em oito classes; a classe afetada foi repetida após eles.
[Inventário, decisão, análise final e limitações](continuidade-10-1.md).

A caracterização confirma provider Azure ausente, zero spans SDK e carrier recebido vazio.
Próxima subfatia: **10.1-A2**, caracterizar o fluxo real e sua ligação com Hub/log; ainda
não iniciada. Produção/dependências, C0.4/doctree e C9.1-L preservados.
C3/encerramento humano da feature pendentes. Branch feature/orquestrador-monitoramento-service-bus;
HEAD/origin local em 1f32466. Pausa, patch e .env preservados. Sem staging/commit/push.
Registros abaixo conservam os marcos anteriores.


## Planejamento de 10.1 — 2026-09-11

Pedido humano: "retomamos pelo planejamento da 10.1". O
[detalhamento de 10.1](continuidade-10-1.md) registra contexto confirmado, contrato C0.4,
escopo, riscos, arquivos, critérios e verificações. A1 caracteriza SDK/captura; A2 caracteriza
o fluxo real; B1–B5 preenchem lacunas demonstradas; C reúne regressão/evidência para C3.
A previsão antiga de até cinco arquivos foi desdobrada por incremento antes de qualquer
alteração executável.

Próxima ação proposta: **10.1-A1**, após solicitação de continuidade executável.
GO/C0.4 existentes continuam válidos; incompatibilidade, dependência, contrato/carrier novo,
capacidade transversal ou exceção de segurança exigem proposta/decisão antes do ajuste.
Preservar nomes doctree e aceite C9.1-L. IDs de negócio ou mock da porta pública do Hub
não comprovam a cadeia de traces.

HEAD/referência local de origin: 1f3246620821a4a3b3aad66a380c64b927d27e3a.
Somente Markdown: sem caracterização executável, inspeção Sonar, Maven/checkpoint,
nova decisão humana, staging/commit/push. Pausa/patch preservados; C3/encerramento pendentes.
Registros abaixo conservam os marcos anteriores.

## Consolidação e publicação de 9.1 — 2026-09-10

Pedido humano: "podemos organizar commit e push? o guia do desnvolvedor foi atualizado para ele
poder retomar a implementação manual eventualmente?". Escopo: publicar 9.1-A/B/C já verificadas,
conferir o guia manual e preparar a retomada pela 10.1, sem executar o próximo item.

Organização: commit funcional de 9.1-A/B com regressões sem broker; commit da integração 9.1-C;
commit documental com guias, arquitetura, evidências e recibo de publicação. Seleção explícita no
[manifesto de 9.1](pacote-commit-9-1.md). O manifesto de 8.2 permanece histórico.
Nesta consolidação, editar somente Markdown; conferir links, diff, seleção e identidade do
fingerprint executável com o checkpoint final. Nenhuma nova análise Maven/Sonar é necessária
para documentação. Preservar baseline, .codex-doc-alignment.patch e artefatos locais.
Implementação/regressões publicadas em fcbe38a; integração publicada em 2c6c4cd.
Push normal concluído e hash 2c6c4cde881c3b8e2261e3a615123d58290fe5c4 confirmado no remoto.
Este complemento documental registra a publicação e o guia manual; conferir seu hash após push.
O usuário reafirmou terminar a publicação antes de analisar o startup Quarkus em dev mode.
Esse diagnóstico operacional fica pendente, sem alterar o escopo executável desta consolidação.
10.1/C3 e encerramento humano permanecem pendentes. Registros de execução abaixo são históricos.

## 9.1 concluída tecnicamente — 2026-09-10

9.1-A/B/C entregues localmente: duas portas CDI, log final, listener da saída com opt-in
independente e prova integrada no emulador. O pedido humano "9.1-C" foi executado sem alterar
produção, contratos, configuração padrão, dependências ou o aceite C9.1-L.

MonitoramentoResultadoEmuladorTest acrescentou quatro cenários: POST terminal; reagendamento
até conclusão na segunda consulta; Rascunho até o máximo de três consultas; contrato inválido
na DLQ, seguido de POST válido para comprovar continuidade do consumidor.
O controle negativo com saída false falhou por ausência do log, com um Complete da entrada.
Após habilitar a saída e sincronizar a observação da DLQ, passou a regressão completa:
27 testes/7 classes, zero falhas/erros/ignorados, incluindo os quatro novos.

A primeira execução dos quatro casos e a reprodução isolada falharam no ACK do Complete
auxiliar da DLQ após detach remoto. A prova passou a aguardar a própria mensagem visível
por peek e a ausência na fila principal antes de receber/concluir a subfila. Passaram o
isolado e a suíte completa, mantendo o Complete e o POST posterior. A causa interna do
emulador não foi demonstrada; não se declara correção de SDK. Não houve retry/supressão
de settlement, sleep fixo ou mudança de produção. Revisões independentes sem bloqueadores.

Checkpoint completo: clean verify, SonarScanner e Compute Engine concluídos; build SUCCESS.
1.345 testes padrão/192 classes, zero falhas/erros/ignorados e nenhuma classe com broker.
Sonar COMPLIANT / NOT_REQUIRED: 88,1% cobertura, 4,4% duplicação, 213 issues abertas,
nenhuma nova ou HIGH/BLOCKER/CRITICAL. Análise 3546ec18-65c4-4e5b-a382-ed897f69f7b5;
CE d4c8a25d-daee-4169-a736-59133f9e8d5f; checkpoint 2026-09-10T15:27:27.592153-03:00.
Fingerprint 5d84ce454d3e4ab5ba5aabb5b8148e0076ab4ef3a22a0c91c873a211719afd0d.
Baseline original de 217 issues e baselineAssessment integralmente idênticos ao fechamento
de 9.1-B, sem reinicialização. Cópia final local:
.codex/.state/session-after-9-1-c-compliant-20260910.json.

Comandos de regressão e checkpoint executados sequencialmente:

```powershell
mvn -q -Pservicebus-integration clean test
./validar-checkpoint-sonarqube.ps1
```

O teste isolado pode ser executado com:

```powershell
mvn -q -Pservicebus-integration "-Dtest=MonitoramentoResultadoEmuladorTest" test
```

Limites preservados: Hub controlado no teste, nenhuma execução de Azure gerenciado/dev
interativo, log sem confirmação de escrita e sem atomicidade com Complete. Abandon por
falha técnica permanece caracterizado sem broker na 9.1-B; revisão geral de retry/redelivery
e sinais continua em C3. Defaults dos dois listeners permanecem false.

Guias, arquitetura, checklist e retomada alinhados. Verificação documental: sete Markdown,
264 links locais e 34 âncoras válidos; diff/whitespace aprovados e fingerprint idêntico ao
checkpoint após a documentação. Referências antigas da introdução do guia principal também
foram alinhadas ao consumo da saída implementado e ao marco publicado de 8.2.
Próximo item: 10.1, ainda não iniciado.
O encerramento humano da feature continua pendente. Sem novo staging/commit/push; branch,
marco publicado d5df6b6 e alterações locais preservados. Nenhum comando do agente em execução
ao concluir esta subfatia. O plano e os diagnósticos abaixo preservam o histórico anterior.

## 9.1-C — prova integrada da saída e fechamento de 9.1 — 2026-09-10

Pedido humano: "9.1-C". Executar somente a próxima subfatia registrada, preservando o aceite
C9.1-L e as alterações locais de 9.1-A/B. Baseline READY/LOCAL_SONAR e baselineAssessment
comparados integralmente à cópia conforme de 9.1-B; idênticos. Token herdado disponível sem
exposição; fingerprint inicial 17fea85859935f12aed57ec6a83070b9c5f80696ea281add4bd74003047e1ac5.

**Intenção/escopo:** acrescentar MonitoramentoResultadoEmuladorTest no package de integração
do orquestrador. Ambos os listeners iniciam pelo profile de teste, sem injeção/chamada explícita.
REST, políticas, ACL, publishers, mappers, listeners, caso de uso, logger e SDK reais; somente
a porta pública de consulta do Hub tem resposta controlada, como nas integrações anteriores.
Reutilizar a proteção contra conexão externa do ServiceBusEmuladorTestProfile, filas e versões
existentes: Quarkus 3.33.2.1, extensão 1.2.5, SDK 7.17.12.

**Critérios:** POST terminal até um JSON real com os mesmos IDs e Complete da saída; POST com
reagendamento até conclusão e até quarentena por máximo; contrato inválido da saída na DLQ
com diagnóstico fixo. Pausar consultas do Hub para observar por peek as sequências reais de
entrada, conferir tentativa/intervalo e depois sua remoção. Confirmar log e, separadamente,
filas principais vazias por peek com sequência explícita, sem consumidor concorrente da saída.
Receiver adicional somente na DLQ do teste, com Complete da própria mensagem antes de cancelar.
Arquivo de log exclusivo, leitura de registros completos e checagem de IDs técnicos/ausência
de payload. Não afirmar exactly-once, confirmação de escrita pelo logger ou atomicidade log/Complete.

**RED/GREEN:** controle negativo temporário com saída false somente no profile novo, executando
o cenário terminal; deve falhar por ausência de log. Depois habilitar a saída nesse profile e
executar os quatro cenários. Defaults de produção permanecem false. Não há mudança funcional
planejada: caso a prova revele defeito, registrar o ajuste antes de alterar produção.

**Verificações/checkpoints:** revisão independente do desenho e do teste; suíte completa opt-in
com emulador; suíte padrão/build e checkpoint Sonar no mesmo baseline. Manter segregação da
suíte padrão sem broker. Atualizar guias, arquitetura, checklist e retomada com limites reais.
Arquivos prováveis: um novo teste Java (profile e helpers locais) e os sete Markdown da feature.

**Riscos/limites:** log pode preceder settlement, então ambos precisam de verificação; peek sem
sequência explícita pode avançar cursor; awaits têm limite e futuras consultas são liberadas
ao terminar. Revisão independente recomendou preservar o pipeline real do logger: falha técnica
antes da submissão/Abandon permanece caracterizada nos testes de 9.1-B; não simular falha de
handler como falha propagada. Revisão geral de redelivery/retry/sinais continua em C3.
A prova local não equivale a Azure gerenciado nem a dev mode interativo.

**Fora de escopo:** 10.1/C3, novas decisões/contratos, Hub/dossie de produção, novas dependências,
factory, formatter/handlers, persistência/Outbox, telemetria adicional e novo commit/push.
Próximo checkpoint humano somente se houver desvio do escopo aprovado ou violação Sonar.
9.1 pode fechar tecnicamente com A/B/C; encerramento da feature permanece humano.

Referências oficiais consultadas:
[Azure Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/index.html) e
[Service Bus/Dev Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html).
As APIs e a configuração foram conferidas nos testes existentes e nas dependências efetivas.

### Diagnóstico da integração — 2026-09-10

Controle negativo: saída false somente no novo profile; cenário terminal falhou por timeout
aguardando o log (30 segundos), com zero eventos finais e um Complete da entrada.
Com saída true, os três cenários via POST passaram. O cenário DLQ falhou no ACK do Complete do
receiver da prova: amqp:link:detach-forced / InnerMessageReceiver was closed. A reprodução isolada
também falhou. O log mostra detach remoto no receiver principal antes do erro da DLQ; a causa
ainda não está confirmada. Nenhuma produção foi alterada.

Experimento delimitado ao teste: aguardar via peek a própria mensagem na DLQ e a ausência na
principal antes de receber/concluir a DLQ; depois publicar novo POST válido para comprovar
continuidade do consumidor principal. Não acrescentar sleep fixo, retry de settlement ou suprimir
exceção para tornar a prova verde. Preservar evidência da falha e registrar o resultado.

Após a sincronização, o cenário DLQ isolado passou (1 teste, zero falhas/erros/ignorados),
incluindo Complete da DLQ e novo POST até log/Complete. Revisão final sem findings.
O resultado sustenta a observação explícita do estado pelo harness; a causa interna do emulador
não foi demonstrada, e não se declara correção ou validação geral do SDK. Regressão completa
com emulador em execução; produção, dependências e defaults preservados.

Na revisão documental foram identificadas referências antigas no guia do desenvolvedor ao
manifesto até 7.1-A e ao listener da saída ainda por implementar. Alinhar esses trechos aos
marcos reais (8.2 publicada, 9.1 local) no fechamento, sem alterar o manifesto histórico.

## 9.1-B — listener e ativação da saída — 2026-09-10

Pedido humano: "Próxima subfatia: 9.1-B", após fechamento conforme de 9.1-A.
Executar esta subfatia de listener/ativação dentro do fluxo de saída aprovado; preservar C9.1-L.
Baseline READY/LOCAL_SONAR e baselineAssessment idênticos à cópia final de 9.1-A; token herdado
disponível, sem exposição. Fingerprint inicial 0e6e88d3f5c2f750ef0957596002fa3e2a52f4bfb69002d1d3679887a6b561ef.

**Intenção/escopo:** tornar MonitoramentoResultadoListener um bean CDI e conectar receiver
@FilaSaida → mapper próprio → ReceberResultadoMonitoramento → log já implementado → settlement.
Ativação independente por monitoramento.service-bus.saida.consumo-habilitado=true,
default e %test=false. Startup e chamada explícita compartilham início único; encerramento
cancela assinatura antes da fábrica (PLATFORM_BEFORE), sem fechar o cliente compartilhado.

**Comportamento:** entrega serial por concatMap com demanda sem antecipação. Complete sucede
a conclusão da porta (submissão best-effort, sem ack de escrita). MapeamentoResultadoException
originada no mapper gera DeadLetter com reason MONITORAMENTO_SAIDA_INVALIDA e descrição fixa.
Falhas técnicas de leitura ou da porta, síncronas/assíncronas, geram Abandon. Uma nova entrega
pode registrar novamente; não alterar tentativas, contrato ou política.
Recuperação de falha fica antes da escolha de settlement: falha de complete/abandon/deadLetter
encerra a assinatura, sem segundo settlement ou reinício automático. Reinicialização exige
novo runtime/instância; HTTP ativo não comprova consumidor ativo. Cancelamento do efeito remoto
não comprova rollback. Erro mínimo aprovado orquestrador.monitoramento-dossie.resultado.falhou,
com valores técnicos locais, sem mensagem original/Throwable/payload. Nenhum evento novo.

**Arquivos funcionais:** listener, teste do listener, application.properties e substituição dos
dois arquivos do inventário inativo por ComponentesResultadoMonitoramento e
ResultadoMonitoramentoCdiTest (renomeações). O último esqueleto passa a implementação:
substituir a prova de inatividade por três provas positivas de resolução CDI, sem testes vazios.

**Verificações/checkpoints:** RED → GREEN com mapper real e SDK controlado; default/opt-in,
serialização, espera da porta, falhas, DLQ, redelivery, cancelamento/início concorrente e
prioridade de shutdown. Verificar evento de erro sem dados proibidos, guardrails e regressão da
entrada. Revisão independente, suíte padrão/build e checkpoint completo no baseline preservado.
Regressão opt-in com emulador existente para confirmar que default da saída não disputa o
consumo dos testes anteriores. A prova nova de startup → POST → log/saída concluída pertence
a 9.1-C; não declarar demonstração completa comprovada antes dela.

**Riscos/divergências:** factory da saída usa prefetch padrão do SDK; confirmar zero na versão
efetiva antes de preservar essa configuração. Guias ainda descrevem saída inativa: alinhar
somente ao que for implementado e verificado. Azure gerenciado não será usado.
**Fora de escopo:** 9.1-C/10.1, Hub/dossie, contratos/DTOs, novos clientes, transação de log,
persistência/Outbox, alterações de formatter/handlers, dependências e commit/push.
**Próximo checkpoint humano:** somente se houver desvio do escopo aprovado ou violação Sonar.

Referências conferidas com Quarkus 3.33.2.1, extensão 1.2.5 e SDK 7.17.12:
[Azure Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/index.html),
[Service Bus](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html),
[lifecycle Quarkus](https://quarkus.io/guides/lifecycle/).
Fontes locais e testes existentes confirmam as APIs usadas; não adotar APIs de versões futuras.

### Verificação e fechamento técnico de 9.1-B

RED de compilação confirmou APIs ausentes. GREEN: 123 testes em sete classes, sem falhas,
incluindo 36 do listener e três provas positivas de CDI. Revisão independente sem bloqueadores.
SDK 7.17.12 confirmou DEFAULT_PREFETCH_COUNT=0; factory preservada.
Regressão com emulador: 23 testes em seis classes, zero falhas/erros/ignorados, saída desabilitada.
A prova nova com a flag da saída habilitada, startup → POST → log → Complete permanece em 9.1-C;
essa regressão confirma os cenários anteriores, sem declarar o demonstrador completo validado.

Checkpoint completo: 1.345 testes padrão/192 classes, zero falhas/erros/ignorados, sem classes
de integração com broker; clean verify e build SUCCESS. Sonar COMPLIANT / NOT_REQUIRED:
88,1% de cobertura, 4,4% de duplicação, 213 issues abertas, nenhuma nova ou HIGH/BLOCKER/CRITICAL.
Análise 358f8528-6bde-4c8c-92bd-0d27dc8a50aa; CE b37adb59-aa6f-42fc-bed7-05d2e3711338;
checkpoint 2026-09-10T14:20:35.1083771-03:00; fingerprint
17fea85859935f12aed57ec6a83070b9c5f80696ea281add4bd74003047e1ac5.
Baseline original (217 issues) e baselineAssessment integralmente idênticos à cópia conforme
de 9.1-A, sem reinicialização. Cópia final local:
.codex/.state/session-after-9-1-b-compliant-20260910.json.

Comandos executados sequencialmente:

```powershell
mvn -q "-Dtest=MonitoramentoResultadoListenerTest,ResultadoMonitoramentoCdiTest,MonitoramentoEntradaListenerTest,ResultadoMonitoramentoLogAdapterTest,ReceberResultadoMonitoramentoUseCaseTest,EstruturaMonitoramentoArchUnitTest,FronteirasMonitoramentoArchUnitTest" test
mvn -q -Pservicebus-integration clean test
./validar-checkpoint-sonarqube.ps1
```

A revisão encontrou frases antigas no guia Service Bus sobre classificação ainda pendente,
intenção de reagendamento sem settlement, inventário de inatividade e pendências já resolvidas
em 7.1/8.1/8.2. Incluído no alinhamento documental corrigir esses trechos pelo código verificado,
sem alterar política, processamento ou reagendamento. Os comandos atuais do guia do dev usam
ResultadoMonitoramentoCdiTest; nomes em evidências históricas permanecem históricos.

Guias, consolidado arquitetural, checklist e retomada alinhados ao listener opt-in implementado.
Verificação documental final: sete Markdown, 259 links locais e 31 âncoras válidos;
diff e whitespace dos novos arquivos aprovados, fingerprint idêntico ao checkpoint.
Revisão final confirmou as três provas CDI; referências antigas dos guias foram corrigidas.
C9.1-L preservado: Complete após submissão ao logger; falhas internas de escrita/filtro podem
deixar o resultado sem log e não provocam Abandon. 9.1-B concluída tecnicamente; 9.1-C é a próxima
subfatia, sem início de 10.1 ou novo commit/push. Registros de 9.1-A abaixo são históricos.

## 9.1-A concluída tecnicamente — 2026-09-10

ContinuarAjustes registrado conforme resposta do usuário. S1117 corrigida renomeando a
variável local para registroJson; a API Sonar confirmou CLOSED/FIXED. Nenhuma lógica mudou.
Nova execução de validar-checkpoint-sonarqube.ps1: clean verify, SonarScanner e Compute Engine
concluídos; 1.307 testes padrão/191 classes, zero falhas/erros/ignorados, sem testes de broker.
Build SUCCESS; COMPLIANT / NOT_REQUIRED, cobertura 88,0%, duplicação 4,3%,
213 issues abertas, nenhuma nova ou HIGH/BLOCKER/CRITICAL.

Análise a99ebf50-6eed-4f06-b02b-ff3375817eed; CE 1bde1a6b-92eb-46de-be0c-32c1470dc8a1;
checkpoint 2026-09-10T13:25:58.0670135-03:00; fingerprint
0e6e88d3f5c2f750ef0957596002fa3e2a52f4bfb69002d1d3679887a6b561ef.
Baseline e baselineAssessment originais integralmente idênticos; sem reinicialização.
Cópia final: .codex/.state/session-after-9-1-a-compliant-20260910.json.
A revisão funcional anterior permanece válida para a renomeação local; regressão completa
e fechamento da issue confirmam o ajuste. Guias e retomada atualizados para o estado atual.

9.1-A entrega as duas portas de recebimento/registro conectadas por CDI e o log best-effort
aceito em C9.1-L. O listener da saída ainda é esqueleto; próximo item funcional é 9.1-B,
seguido das integrações de 9.1-C. 10.1 não iniciada; sem novo commit/push.
As 23 integrações publicadas em 8.2 não foram repetidas para a renomeação de variável.
Os registros abaixo preservam os estados anteriores, inclusive o checkpoint NON_COMPLIANT.

## Ajuste S1117 autorizado — 2026-09-10

O usuário decidiu ContinuarAjustes para o checkpoint de 9.1-A. Decisão registrada com
validar-checkpoint-sonarqube.ps1 -HumanDecision ContinuarAjustes.
Escopo: renomear somente a variável local registro para registroJson no leitor JSON do teste;
sem alteração de lógica, assertions ou produção. Reexecutar o checkpoint completo com
baseline original preservado e registrar a situação técnica resultante.

## C9.1-L aceito e início de 9.1-A — 2026-09-10

O usuário respondeu "ok" à proposta de Complete após submeter o log, aceitando que falhas
internas de escrita não provocarão Abandon. Foi esclarecido que o registro final pode faltar;
o resultado publicado pelo monitoramento não muda, mas a mensagem concluída não será entregue
novamente apenas para recuperar o log. Esse aceite autoriza a semântica best-effort proposta.

9.1-A conecta RegistrarResultadoMonitoramento ao adapter de log e ReceberResultadoMonitoramento
ao caso de uso. Os dois passam a ser beans CDI; o listener da saída continua inativo até 9.1-B.
Cada invocação é lazy e compartilha conclusão entre assinantes, sem repetir registro nessa
invocação; uma nova invocação/redelivery pode registrar novamente. MDC por ExtLogRecord,
campos textuais aprovados e categoria própria; nenhuma alteração no formatter ou no Hub.

Arquivos funcionais: caso de uso, adapter, seus dois testes e inventário de esqueletos (cinco).
Atualizar também o Javadoc das duas portas para a garantia aprovada. Critérios: esperar a porta,
propagar suas falhas síncronas/assíncronas, emitir JSON real com IDs técnicos, preservar contexto
e isolar invocações. RED/GREEN, revisão, suíte padrão/build e checkpoint no baseline original.
Não conectar receiver, ativação ou settlement nesta primeira subfatia.

O teste no runtime efetivo revelou uma divergência na investigação: apesar das propriedades
mdc.flat-fields presentes na configuração, o formatter manteve os campos textuais dentro de
mdc. C0.4 exige campos estruturados, sem impor raiz para este evento final; preservar o formato
original e verificar mdc.evento/monitoramento_id/orquestracao_id. A emissão JSON tipada de erros
das bordas Service Bus continua sob ADR-0012, sem expansão de categoria ou mudança no Hub.
A primeira execução GREEN teve 42 testes, quatro falhas por essa expectativa incorreta e
nenhum erro; teste ajustado ao JSON real, nova execução em andamento. Revisão independente
não encontrou bloqueadores de produção: lazy/memoize por invocação, CDI, MDC e campos seguros.

## 9.1 — investigação inicial anterior ao aceite de C9.1-L — 2026-09-10

O usuário autorizou prosseguir com "go para prosseguirmos", após a publicação de 8.2
(a797114) e do registro documental (d5df6b6). A próxima fatia é 9.1; 10.1 permanece fora
deste incremento. Branch e patch temporário preservados. Baseline READY/LOCAL_SONAR
integralmente idêntico à cópia de 8.2, fingerprint ainda
9b21bd4c4a82e385f1191e905f40a916e6f95055bb74a59f3385a8109d5fcd27.

**Intenção/escopo:** conectar o listener da saída ao mapper/porta/caso de uso existentes
e ao adapter de log próprio. Validar contrato, aguardar conclusão da porta, executar
Complete no sucesso, Abandon na falha recuperável da porta e DLQ no contrato inválido.
Preservar entrega serial, settlement único e cancelamento antes da fábrica.

**Pendência descoberta antes de produção (C9.1-L):** na versão efetiva JBoss Log Manager
3.2.1.Final, LoggerNode.publish e ExtHandler.publish capturam falhas de handlers e as
encaminham ao ErrorManager, sem propagá-las ao chamador. O retorno de Logger.log não
confirma que o registro chegou ao destino. Isso contradiz a leitura literal do critério
"log concluído/falha de registro → settlement" se a falha for de escrita/handler.
A revisão independente confirmou que não há solução mínima com as mesmas APIs que
observe essa falha sem novo mecanismo de confirmação ou alteração de infraestrutura.

**Próxima ação autorizada:** prova isolada da falha em target/provas-9-1, usando as
dependências efetivas, sem alterar produção, testes da suíte ou configuração. Registrar
evidência e proposta concreta antes do checkpoint humano exigido por AGENTS.md para
mudança de comportamento observável. Não inferir aceitação do limite a partir do GO geral.

**Proposta para C9.1-L:** usar o logger padrão como efeito de demonstração best-effort.
A porta conclui após construir e submeter o registro ao pipeline; falhas anteriores à
submissão propagam pelo Uni e permitem Abandon. Falhas, filtros ou descarte internos
dos handlers não são observáveis e podem ocorrer apesar de Complete. Não prometer
persistência nem atomicidade entre log e settlement; redelivery pode duplicar o evento.
Se for exigida confirmação do destino, elaborar escopo/ADR próprio antes de adicionar
sink, writer ou instrumentação de handlers.

**Desenho mínimo após a decisão aplicável:** adapter ApplicationScoped na categoria própria,
evento INFO orquestrador.monitoramento-dossie.resultado.registrado; ExtLogRecord com MDC
por registro, preservando o contexto corrente, campos somente string (evento, camada,
componente, operacao, monitoramento_id, orquestracao_id), sem payload/Throwable/IDs de negócio.
Hipótese inicial sobre mdc.flat-fields corrigida pela prova descrita no início: os campos
textuais ficam em mdc. Não expandir o decorator tipado do ADR-0012 nem usar helpers do Hub.
O caso de uso usa apenas a porta RegistrarResultadoMonitoramento; SDK e settlement ficam
no listener. Ativação/lifecycle da saída será detalhada na fatia correspondente, preservando
a ativação independente da entrada.

**Critérios/verificações após C9.1-L:** RED/GREEN de lazy/conclusão/falha da porta, campos reais
do JSON, isolamento/contexto entre mensagens, listener válido/inválido/redelivery/erro de
settlement e shutdown; integração com emulador; regressão sem broker e checkpoint Sonar
no baseline original. Não simular falha de handler como se o Logger a propagasse.

**Sequência planejada:** 9.1-A adapter/caso de uso e provas sem broker; 9.1-B listener/ativação;
9.1-C integração/fechamento. Delimitar cada subfatia antes da produção e concluir seu checkpoint;
não avançar para 10.1. Arquivos prováveis: os três esqueletos, seus testes, inventário CDI,
configuração de ativação da saída e guias/tasks. A investigação atual altera somente tasks
e arquivos temporários ignorados em target.

**Fora de escopo:** mudar Hub/dossie, DTO público, política/reagendamento, dependências,
formatter/handlers compartilhados, destino de log, persistência, Outbox, telemetria manual
de 10.1 ou publicar novo commit sem pedido correspondente. A decisão C9.1-L permanece pendente.

[Evidência e checkpoint de logging](continuidade-9-1.md).

## Consolidação e publicação até 8.2 — 2026-09-10

O usuário pediu consolidar a entrega antes da etapa 9, atualizar o guia para retomada,
organizar o commit do que foi realizado e publicar. Esse pedido autoriza documentação,
staging seletivo, commit e push normal nesta branch. A leitura inicial de 9.1 não alterou
código, testes ou configuração; sua implementação fica suspensa durante esta consolidação.

**Escopo:** publicar a ativação opt-in de 8.2 já testada e sua documentação. Explicar o fluxo
implementado por package, os comandos Dev Services/Azure, o POST e os resultados observáveis.
Separar o manifesto histórico de b886bdb do pacote atual e deixar um roteiro de retomada.

**Divergências a corrigir nesta consolidação:** o exemplo manual usa 0007, mas esse ID pertence
à prova com Hub controlado; a fixture do simulador do Hub é 4324680, com situação Rascunho.
Há frases antigas de ausência de startup no final dos guias/consolidado e de inexistência
geral de orquestrador local. Alinhar essas descrições ao código existente, distinguindo os
packages da demonstração das capacidades atômicas do Hub. Nenhum comportamento muda.

**Critérios/verificação:** guias com comandos completos e resposta esperada; início condicionado
a monitoramento.service-bus.entrada.consumo-habilitado=true; saída sem consumidor de 9.1.
Revisar diff, links/âncoras, seleção exata e ausência de artefatos/segredos. O fingerprint deve
continuar igual ao checkpoint de 8.2 (9b21bd4c4a82e385f1191e905f40a916e6f95055bb74a59f3385a8109d5fcd27).
Reutilizar suas evidências: 1.297 testes padrão, 23 integrações, build e Sonar COMPLIANT.
Não repetir Maven/Sonar para alterações exclusivamente Markdown com código idêntico.
Conferir o hash remoto depois do push; não registrar publicação antes de sua confirmação.

**Arquivos:** os quatro arquivos executáveis de 8.2 já validados, consolidado arquitetural,
guia Service Bus e tasks/guia/manifestos da feature. Preservar .codex-doc-alignment.patch,
baseline e artefatos locais fora do índice.

**Fora de escopo:** implementar 9.1/10.1, executar aplicação contra Azure, mudar contratos,
configuração executável, Hub, dependências, ADRs, formatos derivados ou encerrar a feature.

**Risco para a retomada de 9.1:** o adapter planejado em adaptador.saida.log ainda está inativo;
a composição JSON tipada atual seleciona somente categorias Service Bus (ADR-0012).
Definir e validar a emissão do evento aprovado, sua conclusão/falha e ativação da saída na
própria fatia, com o checkpoint aplicável se houver mudança do limite arquitetural. Nenhuma
solução ou expansão desse limite foi implementada nesta leitura.

## 8.2 — ativação controlada da entrada — 2026-09-10

O usuário pediu inserir 8.2 antes de 9.1 e confirmou: "vamos fazer isso", após a proposta
de configuração explícita, startup, validação de início único/falhas/shutdown e prova via POST.
Esse aceite autoriza a fatia e sua mudança observável de ativação. A 8.1 já está publicada
em b886bdb8bb079eadcba2ed21c68739cf62ff3356, confirmada em origin; a restrição anterior
de aguardar o reagendamento transacional foi satisfeita.

**Intenção:** permitir que o dev habilite o consumidor da entrada pela configuração e execute
REST → processamento → reagendamento/resultado sem uma chamada Java manual ao listener.

**Desenho:** propriedade runtime booleana
`monitoramento.service-bus.entrada.consumo-habilitado=false`. Um observer de StartupEvent
no próprio MonitoramentoEntradaListener chama iniciar() somente quando true. Reutilizar
os clientes da fábrica e seu encerramento existente; nenhuma nova porta ou camada.
Default e profile de teste ficam false; apenas o profile de integração de ativação usa true.
A chamada explícita existente continua disponível para as provas controladas de 7.1/8.1.

**Critérios de aceitação:**
- Desabilitado não resolve receiver nem assina consumo; habilitado inicia uma única vez,
  sem bloquear o startup para aguardar mensagem. Falha síncrona ao obter cliente mantém
  a exceção sanitizada existente; falha assíncrona encerra a assinatura e exige reinício
  da aplicação, sem retry/reconexão adicionais ou alegação de readiness do broker.
- POST no runtime do teste com emulador e ativação configurada produz resultado terminal
  e resultado após reagendamento, sem chamada direta/reflexiva a iniciar() no teste.
  SDK, broker, REST, listener e processamento reais; somente a porta pública do Hub controlada.
- Default sem broker e ciclo de vida preservados; documentação mostra comando de ativação,
  mock explícito, escolha emulador/Azure e limite: saída aguarda consumo/log de 9.1.

**Verificações:** RED/GREEN do observer/default/falha no teste do listener; regressão existente
de início duplicado, falha de consumo e shutdown; integração de startup opt-in em perfil
isolado e regressão completa com emulador; suíte padrão/build/Sonar no baseline original.
Revisar correção, simplicidade, arquitetura, segurança, desempenho, testes e escopo do diff.
Não adicionar dependência para uma nova ferramenta de testes; usar JUnit/Quarkus existentes.

**Arquivos prováveis:** MonitoramentoEntradaListener.java, application.properties,
MonitoramentoEntradaListenerTest.java, MonitoramentoAtivacaoEmuladorTest.java;
guia Service Bus, consolidado arquitetural e tasks/guias da feature.

**Fora de escopo:** 9.1/10.1, consumidor/flag da saída, novo endpoint de ativação, sinal de
readiness, retry, transporte, credenciais, contratos, política, Hub, hooks, dependências e
publicação de novo commit. Não alterar os defaults funcionais PT30M/PT24H.
A ativação usa a conexão já escolhida pelo dev; prova local não valida Azure gerenciado.

**Riscos/dependências:** emulador/Docker operacional; isolar o novo profile das provas que
iniciam manualmente o listener; não disputar o receiver de entrada para receber mensagens
no teste de startup. Observar remoção via peek e ler somente a saída no harness.
Configuração false preserva o comportamento anterior. Mudanças de conexão exigem reinício.
Falhas assíncronas já encerram o consumo sem derrubar o HTTP; esse limite permanece explícito.

**Divergências registradas:** os registros de preparação ainda descrevem ausência de commit,
mas b886bdb já foi publicado. Guias devem distinguir esse marco da implementação local de 8.2.
Há frases gerais antigas no consolidado sobre ausência de orquestração, apesar da seção
dedicada implementada; restringir o alinhamento desta fatia à descrição de ativação, sem
reescrever silenciosamente a arquitetura do Hub.

**Ordem/checkpoints:** 8.2 (RED → GREEN → integração → revisão/Sonar/documentação), depois 9.1,
10.1, C3 e 11.1–CF. Não avançar para 9.1 nesta fatia. Apresentar decisão humana somente se
houver violação Sonar ou necessidade concreta de escopo adicional.

Referências oficiais consultadas e conferidas com pom.xml (Quarkus 3.33.2.1, JDK 25,
extensão 1.2.5) e API local: [Azure Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/index.html),
[Service Bus/Dev Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html)
e [ciclo de vida Quarkus](https://quarkus.io/guides/lifecycle/). A documentação dev não
autoriza atualizar versões ou copiar os exemplos de autenticação/log fora do ADR vigente.
Evidências desta execução em [continuidade de 8.2](continuidade-8-2.md).

**Fechamento técnico:** 8.2 concluída; 1.297 testes padrão/189 classes e 23 integrações/6 classes
passaram, build aprovado e Sonar COMPLIANT / NOT_REQUIRED, 87,9% cobertura, 4,3% duplicação,
nenhuma issue nova/grave. Revisão sem findings e baseline integralmente idêntico.
Guias, arquitetura e retomada alinhados; sem novo commit/push. 9.1 permanece pendente.

## Preparação do commit e execução — 2026-09-10

Pedido humano: preparar commit seguro, esclarecer execução/testes e conferir alinhamento dos guias.
Escopo: manifesto do incremento 7.1-B/C/D + 8.1 desde d83b689, mensagem, seleção explícita de
42 arquivos no índice, revisão e orientação de execução. Corrigir referências documentais antigas
do manifesto/guia; preservar código e evidências já aprovados. Commit e push não são executados.

Critérios: incluir produção/testes/documentação correspondentes; excluir patch temporário,
estado Sonar, credenciais, build e formatos derivados; índice igual ao manifesto e conteúdo
de código igual ao verificado. Explicar início explícito e distinguir suíte padrão (1.293),
integração (21) e aplicação dev. Revisar diff e links; não repetir Maven/Sonar para Markdown.

Riscos: pacote acumulado amplo, ambiente dev exige configuração própria e startup não ativa
consumo. O guia ainda mencionava 15 integrações em uma seção atual, e o manifesto descrevia
a entrega antiga até 7.1-A; divergências tratadas nesta preparação documental.
9.1, 10.1, ativação automática, dependências, hooks e mudanças de comportamento ficam fora do escopo.
Resultado revisável no [pacote de commit](pacote-commit.md).

### Ajuste de formatação revelado pelo índice

A conferência de git diff --cached --check encontrou linhas vazias extras no EOF de seis
testes novos e da continuidade de 8.1, antes não rastreados. O preparo seguro inclui remover
somente essas linhas, sem alterar lógica, assertions, imports ou configuração. Antes da edição,
o baseline READY original e o checkpoint anterior foram conferidos. Não reinicializar baseline.

Ajustar os seis testes listados pela conferência e o Markdown; comparar conteúdos ignorando
apenas os terminadores finais para provar o escopo. A alteração de fingerprint exige novo
checkpoint completo no mesmo baseline; não adicionar testes para formatação nem repetir
integração de broker. Atualizar manifesto/índice e registrar o resultado real antes de concluir.

## Fechamento técnico de 8.1 — 2026-09-10

O pedido de retomada autorizou as cinco correções Sonar e o novo checkpoint antes de 9.1,
mantendo o ContinuarAjustes já registrado. S6878, S1905 e três S5778 corrigidas, com
equivalência de comportamento confirmada por revisão independente e testes existentes.

Passaram 68 testes focados e 1.293 testes padrão/189 classes sem broker.
Build e checkpoint **COMPLIANT / NOT_REQUIRED**: cobertura 87,9%, duplicação 4,3%,
213 issues abertas, nenhuma nova ou grave. As cinco issues estão CLOSED/FIXED.
As 21 integrações aprovadas em 09/09 não foram repetidas neste ajuste sem mudança funcional.

Divergência operacional encontrada: o hook de abertura havia apagado o baseline em session.json.
O baseline original foi recuperado de cópia validada antes da edição e comparado integralmente
após o checkpoint. A correção do hook fica fora do escopo; risco e cópia final de recuperação
estão na [continuidade de 8.1](continuidade-8-1.md). Nenhum InitializeBaseline foi executado.

8.1 concluída tecnicamente; 9.1 e 10.1 permanecem pendentes e não foram iniciadas.
Branch, alterações locais e HEAD d83b689 preservados, sem staging/commit/push.
Não há encerramento humano da feature. Evidências e próximo item no
[ponto seguro](retomada.md) e no [checklist](todo.md).

## Execução autorizada de 8.1 — reagendamento transacional

Pedido humano: "Próxima etapa: 8.1 — reagendamento transacional". Preservar branch,
alterações locais e baseline READY original. A evidência anterior permanece COMPLIANT
(1.268 testes padrão sem broker, 87,8% cobertura, 4,3% duplicação).

Primeira subfatia: prova obrigatória com SDK 7.17.12/extensão 1.2.5 e emulador do perfil
servicebus-integration, reutilizando os clientes CDI da mesma fila e do mesmo builder.
Verificar createTransaction, scheduleMessage com contexto, CompleteOptions com o mesmo
contexto, commit, rollback e redelivery da mensagem original. Incluir perda controlada
da confirmação local após commit real para distinguir resultado incerto de rollback
confirmado. Não apresentar essa simulação como falha real de rede.

Critérios da prova: commit remove a entrada e disponibiliza somente a próxima mensagem
no instante agendado; rollback não publica a próxima e permite reentrega com mesmo
corpo/identidade e novo lock. Rollback não promete incremento de DeliveryCount (a API
associa esse incremento a Abandon/expiração de lock). Manter assinatura durante a transação; leituras
com cursor explícito, filas vazias verificadas, esperas limitadas e sem purge/skip.
Os testes de integração continuam opt-in; suíte padrão não inicia broker.

A prova precede a implementação produtiva da porta/modelo/adapter e sua conexão ao
listener. Se a API ou o emulador rejeitar a transação, registrar causa/evidência e
apresentar a decisão concreta ao usuário conforme ADR-0010; não trocar tecnologia,
ativar consumo ou declarar atomicidade por suposição. O desenho subsequente detalhará
associação da entrega sem SDK no núcleo/mapa singleton, prazo original versus próximo
intervalo e contador Integer.MAX_VALUE antes da edição produtiva.

Verificações: teste opt-in focado, revisão independente, regressão adequada e checkpoint
completo no mesmo baseline após a subfatia coerente. Arquivos prováveis desta prova:
um teste de integração e tasks da feature. Riscos: diferenças emulador/Azure gerenciado;
confirmação local perdida não determina o resultado remoto. Fora de escopo: nova
dependência, contrato externo, startup automático, 9.1, 10.1 e commit/push.
Referências oficiais: Quarkiverse Azure Services/Service Bus, transações Service Bus,
API Java sender/receiver e visão geral do emulador; assinaturas conferidas por javap.
Evidências e continuidade serão registradas em [continuidade de 8.1](continuidade-8-1.md).

## Desenho de 8.1 após a prova do broker

Aplicar somente se a prova transacional passar. Revisão independente do desenho concluída.
A reafirmação humana dos limites está na [continuidade](continuidade-8-1.md).

- ReagendamentoMonitoramento representa próxima tentativa e instante. A construção a partir
  da decisão preserva IDs, início, prazo e versão recebida; altera somente o contador.
  Calcular min(processadoEm + intervalo, limiteEm), comparando primeiro a duração restante
  para evitar overflow. A entrega no prazo encerra por PRAZO_MAXIMO antes de consultar o Hub.
- A política mantém max-tentativas opcional e verifica o máximo configurado antes do incremento.
  Também encerra a tentativa não conclusiva no maior inteiro representável por MAXIMO_TENTATIVAS,
  preservando consulta conclusiva nessa tentativa e prioridade do prazo quando ambos vencem.
- MonitoramentoReagendamentoAdapter CDI sem estado de entrega; método técnico associar(receiver,
  mensagem) cria implementação local da porta ReagendarTentativaMonitoramento, capturando
  os handles somente na borda. Nenhum mapa singleton, ThreadLocal, callback SDK no domínio
  ou alteração do contrato da porta. Reutilizar mapper/DTO próprios já implementados.
- Serializar antes de abrir transação. No mesmo receiver da entrega: createTransaction;
  sender FilaEntrada agenda com contexto; Complete usa esse contexto; commit confirma ambos.
  Falha em schedule/Complete tenta rollback. O tratamento anterior ao commit não envolve
  commit: falha de commit é resultado incerto, sem rollback nem nova ação. Rollback confirmado
  ou incerto também propaga falha; não reaproveitar o handle settled em Abandon.
- O listener executa a porta associada no ramo de liquidação, fora do recovery de processamento.
  Sucesso transacional não gera Complete simples adicional. Falha encerra a assinatura,
  sem retry local, segundo settlement ou avanço para a próxima entrega. Cada entrega aguarda
  confirmação antes da seguinte; cancelamento é melhor esforço, sem prometer desfazer efeito remoto.
- Reutilizar logs mínimos sanitizados de decisão/settlement, confirmação transacional somente
  após commit; sem Throwable/payload/IDs não validados. Instrumentação completa fica em 10.1.
- Consumo segue com iniciar() explícito; nenhum startup automático ou flag nova. A aplicação
  continua admitindo emulador ou Azure conforme configuração, e testes padrão sem broker.

RED/GREEN por subfatia: modelo/política (limites, janela original, repetição);
adapter (ordem, contexto, confirmação, falhas síncronas/assíncronas e resultado incerto);
listener (mesma entrega, serialização, nenhuma liquidação adicional);
integração real do adapter/listener (reagendamento, prazo e teto sem consulta excedente).
Revisão final, suíte padrão, integração opt-in e checkpoint completo no mesmo baseline.

Arquivos prováveis: modelo, política, adapter, listener, Javadocs da porta/decisão,
inventário de esqueletos, testes diretamente relacionados, arquitetura/guias/tasks.
Entregar em subfatias verificáveis; não adicionar endpoint, dependência ou consumidor de saída.

## Estado anterior — 7.1 concluída tecnicamente após 7.1-D

Nove cenários terminais novos; perfil completo com 15 integrações/4 classes aprovado.
1.268 testes padrão/187 classes sem broker; Sonar COMPLIANT, 87,8% de cobertura,
4,3% de duplicação, nenhuma issue nova/grave e baseline original preservado.
Revisão sem findings; evidência completa na [continuidade](continuidade-7-1.md).
Próximo item 8.1; consumo geral segue inativo, sem transação ou execução da saída antecipados.
Sem novo staging/commit/push. A integração não exigiu alteração de produção.

## Execução autorizada de 7.1-D — integração terminal

Pedido humano: "7.1-D — integração terminal". Executar apenas a integração explícita e o
fechamento técnico de 7.1, preservando o baseline READY original e todas as alterações locais.

Intenção/escopo: provar o caminho terminal com SDK/emulador reais e listener/caso de uso,
catálogo, pré-validação simulada habilitada, ACL e publishers reais CDI. Controlar somente
a porta pública ConsultarDossieProduto nos testes; não alterar o Hub nem inventar IDs de
situação. Reutilizar ServiceBusEmuladorTestProfile por composição, preservando o bloqueio
de configuração externa; habilitar pré-validação e uma política com max=1 só nesse profile.

Critérios: três classificações originais via REST; no-op sem Hub/saída; prazo original
expirado (versão removida); política configurada inativa com max=1; versão ausente dentro
do prazo continua por v1; falha transitória com Abandon/redelivery e contagem funcional
preservada; contrato inválido na DLQ com motivo fixo. Validar saída pelo mapper independente
do orquestrador, IDs, MTR original, contador e sequência real da entrada.

Controles da prova: início explícito, um listener CDI por teste; encerramento pelo handle CDI
após settlement, sem modificar API de produção. Publicar antes de iniciar e observar via
peek com sequência explícita; não usar avanço implícito do cursor como prova de remoção.
Leitura da saída/DLQ confirma Complete antes de cancelar a assinatura. Esperas limitadas,
sem sleeps fixos ou descarte de mensagens estranhas. Somente clientes extras de teste são
fechados pelo teste; a fábrica continua dona dos clientes CDI.
Controle negativo inicial: sem iniciar listener, o resultado não deve aparecer; ativar o
acionamento no teste e executar a suíte de integração. Não retirar código funcional para
fabricar RED. Eventuais defeitos reais descobertos recebem regressão antes da correção.

Verificações: perfil opt-in servicebus-integration (incluindo seis provas anteriores),
suíte padrão sem broker, revisão independente e checkpoint completo no mesmo baseline.
Confirmados artefatos efetivos: extensão 1.2.5, SDK 7.17.12, Reactor 3.4.41 e Quarkus 3.33.2.1.
Referências: guia Quarkiverse Azure Services/Service Bus, API oficial do receiver e CDI/ArC,
conferidas contra os binários efetivos. Sem dependência/configuração produtiva nova.

Arquivos prováveis: novo teste de integração terminal, eventual apoio restrito a src/test,
guia do dev, guia Service Bus, arquitetura e tasks da feature.
Riscos: teste local não valida Azure gerenciado; output/Complete não são atômicos; efeito
remoto já iniciado pode continuar após cancelamento. Filas do emulador devem estar vazias
no início; resíduos falham a prova, não são purgados silenciosamente.
Fora de escopo: consumo automático/geral, transação de 8.1, listener da saída de 9.1,
instrumentação de 10.1, persistência, Outbox, contratos externos e commit/push.
Após 7.1-D, próximo item funcional: 8.1, com prazo/contador/transação ainda a detalhar.

## Estado anterior — 7.1-C concluída tecnicamente

Listener, settlement e lifecycle implementados no recorte autorizado. As 13 S8924 MINOR
foram corrigidas após ContinuarAjustes humano; 1.268 testes/187 classes sem broker,
COMPLIANT, cobertura 87,8%, duplicação 4,3% e nenhuma issue nova/grave. Baseline preservado.
Evidência completa na [continuidade](continuidade-7-1.md); próximo item funcional 7.1-D.
Consumo automático permanece inativo e 8.1 não foi antecipada. Sem commit/push adicional.

## Execução autorizada de 7.1-C — listener e settlement

Pedido humano: "7.1-C". Implementar somente o próximo item: listener da entrada, controle
da assinatura, ordem do settlement e shutdown. Preservar branch/alterações/baseline READY.
Não implementar 7.1-D, reagendamento/transação de 8.1, consumo da saída ou telemetria completa.

Desenho e critérios:
- Listener @ApplicationScoped funcional, com Instance do receiver FilaEntrada, mapper existente
  e porta ProcessarTentativaMonitoramento. iniciar() explícito, sem observer StartupEvent,
  configuração nova ou chamada automática. CDI ativo não significa consumo ativo.
- Uma única inicialização por instância; estado NOVO/INICIADO/ENCERRADO sincronizado e
  Disposable.Swap protegem emissão síncrona e disputa com shutdown. Reinício é rejeitado.
- Usar concatMap(..., 0): uma entrega processada/liquidada de cada vez e nenhum prefetch do
  operador. Confirmado no bytecode Reactor efetivo 3.4.41. Explicitar prefetchCount(0) somente
  no receiver de entrada; preservar PEEK_LOCK/disableAutoComplete e fábrica dona dos clientes.
- O mapper valida antes da porta; passar tentativa e inputSequenceNumber escalar.
  Ignorar registra decisão e gera Complete. ResultadoPublicado gera Complete somente após
  a porta confirmar a publicação. Contrato inválido no mapper gera DeadLetter com opções
  novas por entrega e motivo/descrição fixos. Falha técnica de leitura/mapper/porta gera Abandon.
- Separar escolha da ação de sua execução: handlers técnicos não podem capturar falha de
  Complete/Abandon/DeadLetter e tentar outro settlement. Falha de settlement termina assinatura.
- ReagendamentoPendente termina assinatura sem settlement ou reinício; não gerar sucesso,
  descarte ou Abandon repetido. Liberação/redelivery da entrega depende de SDK/broker; não
  prometer que o lock só será liberado ao expirar.
- ShutdownEvent PLATFORM_BEFORE e PreDestroy cancelam a assinatura idempotentemente, antes
  do fechamento da fábrica PLATFORM_AFTER. Cancelar espera corrente sem fechar o cliente.
- Logs mínimos reutilizam eventos aprovados doctree.monitoramento-mtr.decisao.tomada,
  processamento.falhou e settlement.executado; campos locais constantes, sem Throwable,
  mensagem original, corpo ou identificadores não validados. Não adicionar spans/propagação
  antes da caracterização de 10.1. Reutilizar o marcador JSON existente, sem alterar o Hub.

TDD/verificação: mapper real e SDK simulado; inatividade CDI; ordem da publicação/Complete;
contratos inválidos; falhas síncronas/assíncronas; ausência de segundo settlement; avanço da
segunda entrega só após a primeira; pending sem settlement; encerramento síncrono, cancelamento,
início duplicado/pós-shutdown e corrida de lifecycle. Provar JSON mínimo sanitizado em runtime.
Guardrails, revisão independente e checkpoint completo com testes padrão sem broker.
Integração terminal com fila fica em 7.1-D; aplicação futura admite emulador ou filas Azure.

Arquivos prováveis: listener, log local se necessário, ClientesServiceBus (prefetch de entrada),
testes da borda/fábrica/CDI e inventário de esqueletos; arquitetura/guias/tasks.
Riscos: Abandon técnico pode redeliver até MaxDeliveryCount; sem retry/backoff local novo.
Publicação confirmada e Complete não são atômicos; saída pode duplicar após redelivery.
Pendências de 8.1: data agendada versus prazo, contador limite e transação da mesma entrega.
Referências consultadas: Quarkiverse Azure Services/Service Bus, receiver SDK e Reactor;
binários efetivos confirmados: SDK 7.17.12, Reactor 3.4.41, Quarkus 3.33.2.1/extensão 1.2.5.
Sem mudança de dependências, configuração de conexão, profiles, contratos ou commit/push.

## Fechamento técnico de 7.1-B — caso de uso implementado

GO humano "go" executado nesta fatia. ProcessarTentativaMonitoramentoUseCase está conectado
por CDI, com catálogo por versão, consulta de pré-validação primeiro, limites, classificação
literal do Hub e publicação terminal confirmada. DecisaoProcessamento é funcional e distingue
Ignorar, ResultadoPublicado e ReagendamentoPendente. O núcleo recebe apenas a sequência escalar;
SDK/settlement permanecem na borda. Oito das onze portas estão conectadas; seis esqueletos inativos.

Mantidos: PT30M repetido sem teto no padrão; listas como PT3H,PT4H,PT6H com repetição de PT6H;
max-tentativas opcional em outras definições; versão configurada prevalece, ausente usa v1 interna.
Nenhum fallback renova iniciadoEm/limiteEm/versão ou IDs. Antes do Hub, contagem = atual - 1;
após consulta, atual. max-tentativas=1 permite uma consulta. Os três nomes originais do Hub
geram CONFORME/INCONFORME conforme a tabela humana, preservando situacaoMtr.
Consulta conclusiva iniciada no prazo prevalece sobre expiração durante a consulta.

RED confirmou porta/decisões/construtor ainda ausentes. GREEN: 128 testes focados em nove classes
sem broker. Revisão independente encontrou três lacunas de teste, corrigidas e reconferidas:
prazo recebido de 2 h diferente da v1 de 24 h, record completo/seqüência da quarentena e consulta
explícita no no-op. Nova execução dos 32 testes do caso de uso passou; revisão final sem findings.

Primeiro checkpoint: 1.232 testes/186 classes passaram, cobertura 87,7%, duplicação 4,3%;
NON_COMPLIANT somente por duas java:S6878 no switch, sem issues graves.
O usuário escolheu "ContinuarAjustes", registrado pelo script. Os dois ramos foram alterados
para record patterns sem mudar comportamento; checkpoint completo repetido.

**Evidência final:** 1.232 testes padrão/186 classes, zero falhas, erros ou ignorados, sem broker.
**COMPLIANT / NOT_REQUIRED**: cobertura 87,6%, duplicação 4,3%,
213 issues abertas, nenhuma nova ou HIGH/BLOCKER/CRITICAL.
Checkpoint: 2026-09-09T14:34:10.3274763-03:00.
Análise: 710fa75b-0379-4ad9-b0e2-5f5cbfaf955c; CE: 99069941-4ef8-45df-a97d-c40580442c78.
Fingerprint: 3e33f51117f8317730ddc5bcea590f8ebed8ff64327896b332d2f7adcaebeaaa.
Baseline READY original de 217 issues integralmente preservado, sem reinicialização.
As seis integrações de 7.1-A são evidência anterior e não foram repetidas nesta fatia.

7.1-B tecnicamente concluída; 7.1 completo permanece pendente. Próxima fatia: 7.1-C,
listener/settlement/lifecycle, com integração terminal em 7.1-D. Antes de ativar consumo geral,
8.1 deve coordenar reagendamento com a mesma entrega e provar transação/rollback/redelivery.
ReagendamentoPendente não comprova envio nem permite presumir Complete. Permanecem para 8.1
o intervalo que atinge/ultrapassa o prazo e o contador Integer.MAX_VALUE, cujo incremento
atual falha por overflow. Sem Outbox, publicação confirmada seguida de redelivery pode duplicar saída.

Arquitetura, guia Service Bus, guia do dev, inventário e retomada atualizados.
Mesma branch e alterações locais preservadas, inclusive .codex-doc-alignment.patch.
Sem novo staging/commit/push; d83b689 continua sendo o marco publicado até 7.1-A.
Nenhum comando do agente permanece em execução.
Os registros abaixo são históricos e não alteram este estado vigente.

## Caso de uso 7.1-B — execução autorizada

GO humano: "go", após revisão de alinhamento. Implementar o próximo recorte de 7.1-B:
conectar catálogo/portas existentes no caso de uso e tornar DecisaoProcessamento funcional.
Preservar alterações e baseline READY; listeners e transação de 8.1 continuam inativos.

Critérios e fluxo:
- Consultar pré-validação primeiro; fora de EM_ANALISE_ENVIO_MTR, Ignorar sem Hub/publicação.
- Resolver versão recebida; ausente usa v1 interna sem renovar janela ou substituir dados recebidos.
- Antes do Hub, avaliar prazo e quantidade já realizada (tentativaAtual - 1). Acrescentar à
  política consulta de motivo de encerramento que aceita zero, sem calcular reagendamento.
  max-tentativas=1 permite a primeira consulta. Após resposta não conclusiva, avaliar com
  tentativaAtual. Quarentena anterior ao Hub informa contador anterior e MTR ausente.
- Classificar somente FINALIZADO_CONFORME -> CONFORME, FINALIZADO_INCONFORME -> INCONFORME,
  PENDENTE_INFORMACA -> INCONFORME; preservar MTR original, IDs, início e sequência.
- Terminal publica motivo SITUACAO_CONCLUSIVA_MTR; quarentena publica PRAZO_MAXIMO ou
  MAXIMO_TENTATIVAS e situação calculada QUARENTENA. ResultadoPublicado exige confirmação
  da porta de publicação. Falhas técnicas propagam sem sucesso fictício ou retry adicional.
- Consulta iniciada dentro do prazo pode concluir terminal após o prazo. Para não conclusivo,
  reler relógio e avaliar prazo/contador: publicar quarentena se esgotado, preservando MTR
  consultado; senão devolver ReagendamentoPendente com tentativa original, resolução,
  contador/intervalo e instante da avaliação. A intenção não comprova agendamento.
- Porta recebe inputSequenceNumber escalar long; SDK/settlement ficam na borda. Uni adiado
  e memorizado por invocação; nova invocação representa novo processamento/redelivery.
- Relógio segue IniciarMonitoramentoUseCase, com instante controlado nos testes.

Verificar TDD: ordem, elegibilidade, três mapeamentos, versões presentes/inativas/ausentes,
limites, falhas por porta, confirmação pendente e assinaturas repetidas. Testes CDI/ArchUnit
sem broker, revisão independente e checkpoint Sonar completo ao final do incremento.

Arquivos prováveis: porta/caso de uso/modelo de decisão, política, testes/inventário de
esqueletos; arquitetura, guias e tasks. Dependências: consultas 5.1, publisher 7.1-A, catálogo.
Riscos mantidos: duplicação em redelivery após publicação; sem Outbox. Intervalo que alcança
ou ultrapassa o prazo continua pendente de decisão/teste em 8.1. Sem commit/push neste recorte.
Guias oficiais Quarkiverse Azure Services/Service Bus consultadas; stack existente confirmado
no pom: Quarkus 3.33.2.1 e extensão 1.2.5; SDK/transporte/profiles permanecem fora do recorte.

## Revisão de alinhamento dos critérios — 2026-09-09

Pedido humano: confirmar se tudo está alinhado à direção mais recente. Revisão por leitura
do código/configuração, testes e documentação vigente; sem alteração executável ou nova
execução de Maven/Sonar. A evidência anterior permanece 1.200 testes/185 classes e COMPLIANT,
checkpoint 2026-09-09T11:39:14.852554-03:00.

Confirmado: application.properties e recuperação interna usam PT30M, PT24H e sem teto
por contagem; lista unitária repete o intervalo; PT3H,PT4H,PT6H aplica a ordem e repete PT6H;
max-tentativas continua opcional para outras configurações; versão configurada prevalece,
e versão ausente resolve para v1 padrão sem reiniciar o prazo recebido.
Os guias/ADR descrevem essas regras e delimitam valores antigos como históricos.

Limite da confirmação: ProcessarTentativaMonitoramentoUseCase e
MonitoramentoReagendamentoAdapter continuam inativos. A política encerra quando processadoEm
atinge limiteEm, mas não compara processadoEm + intervalo com limiteEm. Por inspeção,
faltando 5 minutos e sendo o intervalo PT30M, a decisão ainda devolve PT30M. Portanto a
garantia operacional de encerramento/agendamento no prazo não está demonstrada.

Registrar no recorte de 8.1, antes de ativar consumo: definir e testar o tratamento quando
o próximo intervalo alcança ou ultrapassa limiteEm, preservando o prazo original e a
verificação de expiração no consumo. Não tratar o intervalo retornado como garantia de
que a próxima data agendada está dentro do prazo. Nenhuma solução desse limite foi
implementada ou registrada como decisão humana nesta revisão.

Divergência documental já prevista para 7.1-B: o Javadoc do caso de uso inativo ainda
pede definição de versão antiga; alinhar ao catálogo e à decisão recebida quando implementar
o caso de uso, junto da classificação já aprovada. Não modificar Java nesta revisão.


## Ajuste solicitado — intervalo padrão de 30 minutos

Direção humana de 2026-09-09: padrão com apenas 30 minutos repete esse intervalo até o
tempo máximo; uma lista configurada (por exemplo 3 h, 4 h e 6 h) aplica os períodos em ordem
e depois repete o último. Não aplicar teto adicional de tentativas no padrão.

Próximo recorte de 7.1-B: alterar application.properties e a v1 interna de recuperação
para uma lista unitária PT30M, mantendo duração PT24H e prazo original das tentativas.
A política já repete lista unitária/último elemento; confirmar a regra com testes da
configuração tipada, progressão PT3H/PT4H/PT6H e encerramento pelo prazo.
A primeira execução continua imediata; os períodos são intervalos entre tentativas,
não horários absolutos desde o início. Não antecipar listener/transação de 8.1.

Resposta humana: "Somente o padrão: manter max-tentativas como opção para outras configurações".
A opção e seu comportamento permanecem íntegros. Não há teto por contagem na v1 padrão;
outras definições podem configurá-lo explicitamente. Esta resposta encerra a clarificação.

Critérios: ativa e recuperação retornam 30 min nas tentativas 1, 2, 3, 4, 5 e 100;
lista PT3H,PT4H,PT6H retorna 3 h, 4 h, 6 h e repete 6 h; prazo máximo continua obrigatório;
IDs, contador, início, limite e versão preservados. Default sem teto por contagem.
Arquivos prováveis: application.properties, CatalogoPoliticasMonitoramento.java,
seus testes, testes de config/producer/política; ADR-0011/índice, arquitetura e guias/tasks.
Verificação: RED dos novos defaults, GREEN/regressão sem broker, revisão independente
e checkpoint Sonar completo. Baseline READY original integralmente preservado.
Guias oficiais Quarkus Azure Services/Service Bus e Config Mapping reconferidas.
Escopo externo inalterado: Hub, DTOs/JSON, SDK/dependências, profiles e broker.


### Verificação do ajuste de 30 minutos

Ajuste concluído: application.properties e a recuperação v1 interna usam somente PT30M,
repetido até o prazo máximo (PT24H no padrão), sem teto por contagem. O usuário confirmou
que max-tentativas permanece opcional para outras configurações. A política já aplica
listas ordenadas e repete o último intervalo; PT3H,PT4H,PT6H foi provado pela configuração.

RED: 48 testes, seis falhas esperadas nos defaults antigos. GREEN/regressão: 96 testes em
oito classes, sem falhas/erros/ignorados e sem broker. Revisão executável sem findings;
finding documental corrigido e reconferido, com valores anteriores delimitados como histórico.
Checkpoint completo: 1.200 testes em 185 classes, zero falhas/erros/ignorados, sem broker.
COMPLIANT / NOT_REQUIRED: cobertura 87,4%, duplicação 4,3%, 213 issues, nenhuma nova ou grave.
Data: 2026-09-09T11:39:14.852554-03:00.
Análise: 2e4040f9-882c-4634-8e86-e3ec246f3728; CE: cefd5805-df18-43ad-a76a-33a565855be2.
Fingerprint: 9ea895778d29140c6a585525f326bf19296dd5ff1fbdd9f9f1eafe84295d9c64.
Baseline READY original de 217 issues integralmente preservado, sem reinicialização.
As seis integrações de 7.1-A não foram repetidas; este ajuste não muda operação no broker.

7.1-B continua em andamento: conectar catálogo e classificação ao caso de uso terminal
é o próximo recorte. Listeners/8.1 continuam inativos; testes padrão seguem sem emulador.
Alterações locais preservadas, sem novo staging/commit/push depois de d83b689.

## Evidência anterior — recuperação da política por v1 padrão

Recorte de resolução de políticas concluído em 2026-09-09. Por decisão explícita do usuário,
uma definição ausente usa v1 padrão, sem QUARENTENA por esse motivo. A definição configurada
continua tendo precedência, inclusive inativa. Catálogo imutável e producer CDI implementados;
a política ativa segue destinada a novos inícios. A resolução distingue versão solicitada,
política efetiva e padrão aplicado. Não escreve propriedades nem altera a mensagem.

Padrões: PT30M, PT3H, PT4H, PT6H (último repetido), PT24H e sem máximo opcional de tentativas.
O processamento deve usar o prazo recebido, sem recalcular a janela. Configuração inválida
presente, seleção inválida e versões repetidas continuam falhando na inicialização.

RED comprovado por ausência do catálogo. GREEN/regressão: 89 testes em oito classes, zero
falhas/erros/ignorados, sem broker. Revisão independente sem findings.
Checkpoint completo: 1.193 testes em 185 classes, zero falhas/erros/ignorados; COMPLIANT /
NOT_REQUIRED, cobertura 87,4%, duplicação 4,3%, 213 issues, nenhuma nova ou HIGH/BLOCKER/CRITICAL.
Data: 2026-09-09T11:15:55.2842506-03:00.
Análise: 94d5935f-751f-4b82-acab-eb535d017fb4; CE: b2eab3ae-924a-4582-a219-2772bf2e4fb3.
Fingerprint: feb530374023662d511d1c84f455ac0c44c0fbc7487464463276bd4b930995fb.
Baseline READY original de 217 issues integralmente preservado, sem reinicialização.
Testes com broker não executados; as seis integrações anteriores são evidência de 7.1-A.

7.1-B permanece em andamento: próximo recorte é conectar catálogo e classificação ao caso
de uso terminal, usando o GO vigente. A decisão de versão está resolvida; não perguntar de novo.
Listeners e 8.1 não foram ativados. Alterações locais, sem novo commit/push após d83b689.

Os registros seguintes preservam o planejamento e as evidências anteriores.

## Decisão humana de recuperação da política ausente — 2026-09-09

O usuário definiu: "vamos evitar a quarantena e dexiar o monitoramento que tinhamos iniciado -
se a configuração não existe consiguramos uma configuração v1 com valores dadrões".
Esta decisão substitui a proposta de QUARENTENA por definição ausente. O GO de 7.1 continua
válido; não solicitar novamente a escolha de versão.

Próximo recorte de 7.1-B: resolução de políticas por versão, com recuperação por v1 padrão.
Quando a versão recebida estiver configurada, usar essa definição, mesmo inativa. Quando
ausente, fornecer uma política progressiva v1 interna: PT30M, PT3H, PT4H, PT6H (último
intervalo repetido), PT24H de duração e máximo de tentativas ausente. Esses padrões são os
valores atuais de application.properties; não são valores recuperados de uma configuração
removida. A aplicação deve preservar IDs, tentativa, iniciadoEm, limiteEm e versão recebida,
usando o limite original ao avaliar continuidade. A recuperação não reinicia as 24 horas.
A resolução expõe versão solicitada, política efetiva e indicador de padrão aplicado.

Escopo: catálogo imutável no domínio de monitoramento, composto pelo producer CDI existente,
e provas de resolução, configuração e injeção sem broker. A política ativa continua destinada
a novas iniciações. Definições presentes inválidas, seleção ativa inválida e versões duplicadas
falham no bootstrap; recuperação se aplica à versão ausente de uma mensagem válida.
Sem novas propriedades, logs, DTOs de transporte, SDK, dependências ou mudança do Hub.
Não ativar listeners nem implementar o agendamento transacional de 8.1 neste recorte.
A ligação ao caso de uso terminal é o recorte seguinte de 7.1-B, já autorizado pelo GO vigente.

Critérios: versão configurada tem precedência; ausente usa padrões fixos independentemente
da ativa; os cinco passos da progressão e ausência de teto opcional são demonstrados;
prazo recebido é respeitado sem mutação da tentativa; versões vazias não viram fallback.
Provar CDI e rejeição de ambiguidades, mantendo a regressão de configuração inválida.

Arquivos prováveis: CatalogoPoliticasMonitoramento.java e seu teste;
PoliticaMonitoramentoProducer.java, PoliticasMonitoramentoConfigTest.java e
PoliticaMonitoramentoProducerTest.java; ADR-0011/índice, arquitetura, guias e tasks.
Sequência: registrar plano/decisão; RED; GREEN; revisão independente; regressão focada;
checkpoint completo; atualizar evidência e guia do dev. Baseline READY original conferido
integralmente preservado, análise f6183a72-a2ea-44bc-9374-b2b064bdad55; não reinicializar.
Guias oficiais Quarkus Azure Services/Service Bus e Config Mapping reconferidas no stack
existente. Nenhuma alteração de extensão/configuração Azure entra no recorte.

Risco assumido pela direção humana: sem a definição antiga, não é possível reconstruir
eventuais intervalos e teto de tentativas personalizados; aplicam-se os padrões declarados.
O prazo já transportado permanece a autoridade para o monitoramento em curso.


## Evidência do recorte de contrato de 7.1-B

Recorte de contrato de 7.1-B concluído em 2026-09-09. As duas bordas de resultado aceitam
FINALIZADO_CONFORME, FINALIZADO_INCONFORME e PENDENTE_INFORMACA literalmente, preservando
os três valores anteriores por compatibilidade. A situação calculada é transportada em campo
independente; o caso de uso ainda não executa a classificação.

RED: três cenários novos rejeitados no produtor, 123 casos no total. GREEN e regressão:
181 testes em sete classes, zero falhas/erros/ignorados, sem broker. Revisão independente
sem bloqueadores. Checkpoint completo: 1.175 testes em 184 classes, zero falhas/erros/ignorados,
sem testes com broker; COMPLIANT / NOT_REQUIRED, cobertura 87,4%, duplicação 4,3%,
213 issues, nenhuma nova ou HIGH/BLOCKER/CRITICAL.

Checkpoint: 2026-09-09T10:16:03.4607778-03:00.
Análise: 32307360-2efd-4bea-a5e3-9cf559738ac6; CE: 37d9990a-47fd-407d-b3dd-abf9e64524a5.
Fingerprint: 365d3b7545e39eb6cad8ace405c3f1f9fbb22c74253efaefed30c308a1744f81.
Baseline READY original de 217 issues integralmente preservado, sem reinicialização.
As seis integrações anteriores pertencem à entrega 7.1-A; não foram repetidas neste recorte
de validação/JSON, que não modifica envio, recebimento ou agendamento no broker.

Código alterado somente nos dois DTOs ResultadoMonitoramentoDossieMtrV1 e no teste
ResultadoMonitoramentoContratoTest. Configuração/política v1, Hub, dossie, fábrica,
publishers e listeners preservados. Nenhum novo commit/push após d83b689.

**7.1-B permanece em andamento.** O caso de uso depende da escolha de tratamento para
versão divergente. A pergunta foi reapresentada; nenhuma alternativa foi escolhida ou
implementada por suposição. 7.1-C/D e 8.1 permanecem pendentes.

## Retomada de 7.1-B — compatibilidade das situações originais

O usuário solicitou "continuar desenvolvimento" após a publicação de d83b689, em 2026-09-09.
Retomar somente 7.1-B. A direção humana anterior já informa literalmente
FINALIZADO_CONFORME -> CONFORME, FINALIZADO_INCONFORME -> INCONFORME e
PENDENTE_INFORMACA -> INCONFORME, preservando a situação MTR. Usar esses textos exatos,
sem inferir IDs nem normalizar PENDENTE_INFORMACA. Não é necessária nova autorização
para a direção já dada; a estratégia de versão divergente permanece sem escolha.

Primeiro recorte independente: permitir esses três nomes originais em CONCLUSIVO nos
dois DTOs próprios de resultado. Preservar os três valores anteriormente aceitos por
compatibilidade com a entrega publicada, a exigência de consulta/contador positivo,
quarentena null/zero, JSON e erros existentes. O mapper continua sem classificar situações.

Arquivos executáveis: os dois ResultadoMonitoramentoDossieMtrV1.java e
ResultadoMonitoramentoContratoTest.java. RED: acrescentar os três pares informados à
prova entre produtor/consumidor independentes. GREEN: ampliar somente a validação
das situações conclusivas. Regressão: contrato, logs, publisher e fronteiras; depois
checkpoint completo com baseline preservado. Não ativar listener ou agendamento.

Baseline READY original de 217 issues conferido integralmente igual ao final de 7.1-A,
análise f6183a72-a2ea-44bc-9374-b2b064bdad55. Credencial herdada disponível, sem exposição.
Fingerprint anterior: 4d1adf3c64b2ac87ac3a8aa89c8a768d70fa8ba11a58fad8f7d92d2400a024a0.
As guias oficiais Quarkus Azure Services e Service Bus foram reconferidas; extensão 1.2.5,
builder da extensão e Dev Services permanecem iguais. Nenhum novo serviço Azure entra
neste recorte. A política/configuração v1 existente permanece preservada.

A pergunta sobre versão foi reapresentada: usar a definição recebida quando configurada
e quarentena se ausente, ou quarentena para qualquer divergência da ativa. A parte do
processamento dependente dessa decisão será implementada somente após a resposta.
Este recorte de contrato não conclui 7.1-B nem representa classificação funcional.


## Preparação documental da entrega até 7.1-A — 2026-09-09

Pedido humano: atualizar o pacote de commit e os guias que o desenvolvedor seguirá.
Escopo deste incremento: somente Markdown; alinhar manifesto, guia de desenvolvimento,
guia Service Bus, checklist e retomada ao ponto estável de C2 aceito/7.1-A concluída.
A preparação documenta as alterações acumuladas de 5.1, 6.1, C2-R1 e 7.1-A; não declara
um novo commit ou push executado.

Critérios: distinguir publicação inicial e publisher da saída do processamento ainda
pendente; indicar configuração v1 existente, próxima subfatia 7.1-B, decisões em aberto,
comandos sem broker/integração explícita e limites da evidência. Conferir links locais,
diff documental e preservação dos arquivos executáveis. Sem Maven, Sonar, alteração
de baseline, geração de formatos derivados ou mudança de contrato/arquitetura.

Divergência registrada para 7.1-B: o Javadoc dos esqueletos de processamento e a validação
dos DTOs ainda citam CONFORME/NAO_CONFORME/PENDENTE_INFORMACAO. A direção humana de
2026-09-09 usa nomes originais do Hub e INCONFORME. Os guias explicitam essa diferença;
a documentação Java e a validação serão ajustadas junto da lógica e regressão aplicáveis,
sem alterar código nesta preparação. As confirmações de grafia e versão permanecem
conforme o registro de 7.1; nenhuma nova decisão humana é inferida.


## Estado atual de 7.1-A — 2026-09-09

C2 aceito e 7.1 autorizado. **7.1-A concluída:** publisher de resultado implementado;
145 testes focados, 1.172 testes padrão/184 classes e seis testes de integração explícita
passaram. O usuário decidiu ContinuarAjustes; S5778 foi corrigida somente no teste CDI.
Checkpoint final COMPLIANT / NOT_REQUIRED: cobertura 87,4%, duplicação 4,3%, nenhuma
issue nova ou HIGH/BLOCKER/CRITICAL. 7.1 continua em andamento; caso de uso/listener
e 8.1 não foram implementados.
Baseline original e alterações locais preservados, sem staging, commit ou push.
Detalhes e próxima decisão em [continuidade-7-1.md](continuidade-7-1.md).
Os registros anteriores de retomada e C2 abaixo permanecem como histórico.


## Estado após a correção C2-R1 — 2026-09-09

C2-R1 corrigido após o pedido explícito do usuário: fontes efetivas do Quarkus verificadas
antes do bootstrap do emulador. **1.166 testes padrão/182 classes**, sem broker, e **quatro
testes de integração explícita** aprovados. Sonar **COMPLIANT**, cobertura **87,4%**,
duplicação **4,3%**, nenhuma issue nova ou grave. Baseline original preservado.
C2 aceito explicitamente pelo usuário em 2026-09-09; 7.1 não iniciado. Detalhes em [revisao-c2.md](revisao-c2.md).


**Continuidade atual:** comportamento de 6.1 implementado conforme o GO humano:
REST → parâmetros locais pela ACL/política → publicação inicial confirmada. Testes padrão
sem emulador nem fila Azure; integração com emulador somente por profile explícito.
Item 6.1 tecnicamente concluído após o ajuste Sonar autorizado por ContinuarAjustes.
C2 aceito; próximo item funcional: 7.1, ainda pendente. Ver [continuidade de 6.1](continuidade-6-1.md).

## Estado vigente em 2026-09-08

Fábrica CDI com quatro clientes duradouros/qualifiers e fechamento completo/idempotente;
preparação/ACL calculam limite/versão; iniciação gera UUIDs/instante no servidor e tentativa 1.
O POST responde 202 somente após confirmação; validação/falha preservam o formato REST.
Das 11 portas, seis possuem implementação conectada; restam nove esqueletos inativos.

**1155 testes padrão em 181 classes**, zero falhas/erros/ignorados, sem broker.
Separadamente, **quatro testes em duas classes de integração explícita** passaram no emulador,
incluindo REST → JSON/envelope v1. O profile Maven `servicebus-integration` seleciona somente
essas provas; não participam da execução padrão nem da cobertura do checkpoint.

Checkpoint de `2026-09-08T21:03:51.3982853-03:00`: **COMPLIANT**, decisão **NOT_REQUIRED**.
Cobertura **87,4%**, duplicação **4,3%**, 213 issues, nenhuma nova ou HIGH/BLOCKER/CRITICAL.
Análise `0b8a179d-1dd9-4e88-bbef-d1fefd914484`.
O usuário decidiu ContinuarAjustes sobre S1710; o ajuste removeu apenas o agrupador
`@APIResponses`, mantendo os três `@APIResponse` e o contrato existente.

Baseline READY/LOCAL_SONAR original de 217 issues preservado, análise
`f6183a72-a2ea-44bc-9374-b2b064bdad55`; nenhuma reinicialização.
Hub, `dossie`, testes/configurações dessas capacidades, dependências e emulador preservados.
A alteração de build de 6.1 somente separa a suíte padrão da integração com broker.

O dev escolhe emulador (`mvn quarkus:dev`) ou filas Azure externas
(`mvn quarkus:dev "-Dquarkus.profile=dev,azure"`), conforme o guia. Azure real não foi usado
na verificação; teste unitário não depende dessa escolha. Antes de 7.1, confirmar a tabela
id/nome do Hub → códigos conclusivos e o tratamento de versão de política.
Telemetria automática do SDK e encerramento das assinaturas dos futuros listeners ainda
precisam de caracterização nas etapas correspondentes.

Branch `feature/orquestrador-monitoramento-service-bus`. Base até 4.1 publicada em
`84fca5c`, complemento documental em `266092f`. Alterações de 5.1/6.1 no workspace,
sem novo commit/push. Não houve encerramento humano da feature.
Detalhes e histórico das verificações no [checklist](todo.md).

## Histórico da retomada e revisão de packages

Esta retomada incorpora a orientação humana de 2026-09-06, seguida da autorização para prosseguir.
As raízes são
`br.gov.caixa.simtr.orquestrador` e `br.gov.caixa.simtr.monitoramento`. O segundo nome substitui
`br.gov.caixa.simtr.doctree.monitoramentomtr`; não haverá um nível `simtr` abaixo de
`monitoramento`. Os dois componentes terão domínio, aplicação, portas e adapters próprios.

O item 3.1 continua concluído e o item 4.1 continua em andamento. A política pura, seu teste e os
dois testes de infraestrutura foram migrados para `monitoramento`, preservando o GREEN.
A configuração tipada e seu producer CDI passaram pelo RED/GREEN; seleção inválida também foi
rejeitada no bootstrap real do Quarkus, antes da execução de um teste sem injeção da política.
Os contratos/mappers restantes e a ampliação do ArchUnit continuam pendentes. O endpoint REST
pertence ao item 6.1 e não está implementado.

O primeiro checkpoint da subfatia migração/configuração concluiu 682 testes e build, mas ficou
`NON_COMPLIANT` por uma issue nova CRITICAL/HIGH `java:S8911` no uso de `@Startup` no producer.
A documentação Quarkus permite esse uso e o bootstrap foi comprovado no runtime efetivo;
a divergência e as métricas estão no checklist. O usuário decidiu `ContinuarAjustes`, e a decisão
foi registrada pelo script do checkpoint. O ajuste mantém o mesmo producer: `@Startup` passa para
a classe, o construtor valida todas as definições e guarda a política selecionada, e o método
`@Produces @Singleton` sem parâmetros a fornece. O RED/GREEN comprovou validação na construção;
a injeção CDI passou e a prova negativa confirmou falha no bootstrap sem consumidor da política.

O checkpoint do ajuste CDI concluiu 683 testes e build e ficou `COMPLIANT`: 213 issues abertas,
nenhuma nova, nenhuma HIGH/BLOCKER/CRITICAL, cobertura 86,3% e duplicação 3,9%. O baseline foi
preservado e não houve supressão de regra nem aceitação excepcional. O próximo trabalho continua
nos contratos/mappers e guardrails restantes do item 4.1; não houve avanço para 5.1.

Na continuidade, request/response REST, modelos semânticos próprios e mapper do orquestrador
passaram por RED/GREEN. O checkpoint deste incremento concluiu 706 testes sem falhas, mas ficou
`NON_COMPLIANT` por uma issue nova MINOR/LOW `java:S6353` na expressão `[0-9]+` do request.
São 214 issues, 1 nova, nenhuma HIGH/BLOCKER/CRITICAL, cobertura 86,3% e duplicação 3,9%.
O usuário decidiu `ContinuarAjustes`; a decisão foi registrada e a regex foi simplificada para
o literal Java `"\\d+"`, sem flags Unicode e sem modificar testes, mensagens ou validações.
O novo checkpoint concluiu 706 testes e ficou `COMPLIANT`: 213 issues abertas, nenhuma nova,
nenhuma HIGH/BLOCKER/CRITICAL, cobertura 86,3% e duplicação 3,9%. Não há decisão Sonar pendente.
A localização `br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus` e seus limites técnicos
foram definidos pelo usuário: regras assíncronas ficam nos respectivos componentes, Quarkus
reativo é permitido no domínio e `simtr.hub` permanece como está. Esta revisão é exclusivamente
documental, sem nova execução Maven/Sonar. Em seguida, o usuário aprovou explicitamente o
ADR-0011 em 2026-09-06, incluindo colaboração por porta/ACL e detalhes da composição CDI.
O usuário também aprovou o limite positivo compatível com `Long` e deu GO para prosseguir em
2026-09-06. A subfatia executável de 4.1 aplicou esse limite por RED/GREEN no contrato REST,
preservando JSON string e zeros à esquerda: 32 testes focados e 715 testes completos aprovados.
O checkpoint ficou `NON_COMPLIANT` por uma issue nova `java:S4144` no teste; cobertura 86,3%,
duplicação 3,9% e nenhuma HIGH/BLOCKER/CRITICAL. O usuário decidiu `ContinuarAjustes`; decisão
registrada e cenários de rejeição consolidados sem perda de casos. Os 32 testes focados e 715
testes completos passaram; novo checkpoint `COMPLIANT`, com 213 issues abertas, nenhuma nova,
nenhuma HIGH/BLOCKER/CRITICAL, cobertura 86,3% e duplicação 3,9%. S4144 confirmada como `FIXED`;
não há decisão Sonar pendente. O endpoint permanece em 6.1; esta subfatia não conclui 4.1.

Na subfatia seguinte de 4.1, produtor e consumidor da fila de entrada foram implementados com
modelos e DTOs independentes, mapeamento JSON/AMQP, validação e compatibilidade entre as bordas.
O produtor monta a mensagem sem publicar; o consumidor valida envelope, tipos, schema, campos
obrigatórios, tentativa positiva, limite MTR aprovado e coerência de datas. Identificadores,
zeros à esquerda, prazo original e versão da política são preservados; campos desconhecidos e
`DeliveryCount` não alteram a tentativa funcional. Falhas de contrato não expõem payload ou causa
do parser. Não há listener, settlement, publicação ou agendamento nesta subfatia.

O usuário confirmou 161 testes focados e 779 completos aprovados. Os relatórios Surefire
preservados confirmam 779 testes, sem falhas, erros ou ignorados. O último checkpoint foi
`NON_COMPLIANT` por uma issue nova `java:S7467`, com cobertura 86,5%, duplicação 3,9% e nenhuma
HIGH/BLOCKER/CRITICAL. `ContinuarAjustes` foi registrado em `2026-09-06T20:45:43-03:00`, conforme
confirmação humana da retomada. A atualização dessas evidências em tasks ficou pendente na pausa.

Em 2026-09-07, o usuário autorizou exclusivamente a retomada de 4.1 com atualização de tasks,
troca de `catch (JsonProcessingException ignored)` por `catch (JsonProcessingException _)` na
linha 60 de `monitoramento/adaptador/entrada/servicebus/MonitoramentoEntradaServiceBusMapper.java`,
preservação do corpo e dos testes, regressão focada e novo checkpoint com o baseline existente.
DTO/mapper de reagendamento, contratos de resultado e novos guardrails ArchUnit continuam
pendentes em 4.1; esta correção não os conclui nem autoriza avanço para 5.1.

Risco encontrado antes da edição: o `session.json` atual está em
`NOT_REQUIRED_UNTIL_CODE_CHANGE`, com `baseline=null` e `lastCheckpoint=null`, divergindo do
estado confirmado pelo usuário. O hook `sonar-session-start.ps1` escreve esses campos nulos ao
iniciar a sessão. A referência original deve ser recuperada sem reinicialização; enquanto não
for recuperada, o baseline preservado e o novo checkpoint não podem ser declarados verificados.
Essa divergência não autoriza corrigir hooks, substituir a referência ou reiniciar itens.

A referência original foi recuperada dos resultados completos das ferramentas da sessão anterior:
baseline capturado em `2026-09-06T11:34:28.1558818-03:00`, análise
`f6183a72-a2ea-44bc-9374-b2b064bdad55`, 217 issues. O objeto restaurado foi comparado integralmente
ao snapshot histórico; o fingerprint executável coincidiu com o checkpoint de S7467.
Também foram recuperados o último checkpoint e a decisão `CONTINUE_ADJUSTMENTS` de
`2026-09-06T20:45:43.39731-03:00`. Cópias dos estados anterior e restaurado ficaram em
`.codex/.state/`. Nenhum baseline foi reinicializado e nenhum hook foi alterado.

O ajuste autorizado de S7467 foi aplicado em 2026-09-07: somente `ignored` foi substituído por
`_`, preservando o corpo do catch, a tradução para `ContratoMonitoramentoInvalidoException` e
todos os testes. Passaram 161 testes focados e 779 completos, sem falhas, erros ou ignorados.
O checkpoint ficou `COMPLIANT`: 213 issues, nenhuma nova, nenhuma HIGH/BLOCKER/CRITICAL,
cobertura 86,5% e duplicação 3,9%; S7467 confirmada como `CLOSED`/`FIXED`. Baseline original
integralmente preservado; decisão Sonar `NOT_REQUIRED`. O registro operacional da rejeição
continua pertencendo ao futuro listener, conforme o plano; não foi adicionado log ao mapper.
Restam em 4.1 DTO/mapper de reagendamento, contratos de resultado e novos guardrails ArchUnit.

O workspace, a branch e as alterações em andamento serão preservados. A revisão não reinicia
itens concluídos nem antecipa 5.1. A migração de Java não altera o caminho exigido pela extensão:
`src/main/azure/servicebus-emulator/config.json`.

## C4.4 — estrutura no código e guia de continuidade — 2026-09-07

**Estado:** estrutura e guia concluídos, sem lógica nova conectada. 40 arquivos de produção
(11 portas, dois records e 27 esqueletos), 190 testes focados e 859 completos aprovados;
checkpoint `COMPLIANT` em `2026-09-07T17:13:24.4462907-03:00`, cobertura 86,4%, duplicação 3,7%,
nenhuma issue nova/HIGH/BLOCKER/CRITICAL. Baseline original e arquivos anteriores preservados.
[Guia de desenvolvimento](guia-desenvolvimento.md) e [manifesto do pacote](pacote-commit.md)
prontos para discussão/revisão; nenhum commit. Evidências detalhadas no checklist.

**Direção humana:** antecipar a estrutura de componentes e classes no código para discussão com os
desenvolvedores, antes de continuar a implementação efetiva; em seguida criar um guia para
implementar e acompanhar o trabalho conforme o plano. Esta direção substitui a proposta anterior
do agente de manter as classes futuras apenas em um mapa documental.

**Ordem atual:** 4.1-E1 estrutura inativa -> 4.1-E2 guia de continuidade -> preparação do pacote de
commit para revisão -> retomada das pendências funcionais de 4.1. O item 5.1 permanece pendente.
Não refazer política, configuração, contratos REST/entrada ou logs já verificados.

**Escopo estrutural autorizado:** declarar portas, tipos semânticos mínimos e arquivos Java dos
casos de uso, adapters, DTOs/mappers pendentes e fábrica técnica nas localizações dos ADRs
0001/0003/0004/0011/0012. Os nomes concretos, responsabilidades e itens de implementação serão
inventariados no guia desta feature. Tipos de cada componente/borda continuam independentes.

As classes ainda sem implementação serão `@Vetoed`, com Javadoc que identifica a pendência,
responsabilidade, dependências previstas e item do checklist. Não terão métodos de execução,
injeção, producers, observers, anotações REST ou clientes ativos. Portas com operações declaradas
não terão implementação CDI nem retorno fictício. Tipos ainda sem campos serão identificados
como estrutura pendente; não são contratos JSON aprovados nem modelos prontos para uso.
`@Vetoed` impede instalação de beans/observers da classe, conforme
[Jakarta CDI](https://jakarta.ee/specifications/cdi/4.1/apidocs/jakarta/enterprise/inject/vetoed).
Seu uso é temporário: na fatia funcional correspondente, implementar o contrato, testar e
habilitar a classe. Não criar hierarquia de herança apenas para representar código pendente.

**Limite da antecipação:** a estrutura mostra a arquitetura já aprovada; não implementa consulta,
publicação, consumo, geração de IDs, cálculo adicional de política, reagendamento, transação,
settlement, persistência ou ação automática sobre logs. Assinaturas semânticas ainda sujeitas à
implementação não autorizam alterar JSON, validação ou a API do Hub. Código, logs e erros do Hub,
extensão Quarkus, simuladores e `src/main/azure/servicebus-emulator/config.json` são preservados.

**Divergência de sequência registrada:** o plano e o consolidado previam criar arquivos conforme
seus consumidores fossem implementados. O pedido humano antecipa somente sua representação
estrutural nesta feature; não muda ownership, dependências permitidas ou os ADRs aceitos. A fábrica
executável continua em 6.1 e os fluxos continuam nos respectivos itens 5.1–10.1.

**4.1-E1 — critérios e verificação:** arquivos no package definitivo; distinção explícita entre
implementado e pendente; compilação preservada; guardrails de núcleo/borda e isolamento entre
componentes com provas positivas/negativas; prova CDI de que os esqueletos não são beans. Criar
em grupos de até cinco arquivos de produção, verificar compilação e revisar o conjunto. Após o
incremento coerente, regressão e checkpoint Sonar com o baseline original, sem reinicialização.

**4.1-E2 — critérios e verificação:** guia navegável com diagrama, inventário de classes/portas,
responsabilidades, dependências permitidas, estado real, próximos passos por item, comandos de
verificação, tratamento de falhas e roteiro de atualização do checklist. Links locais conferidos.
O guia não declara listeners/REST/integrações prontos nem substitui evidências do `todo.md`.

**Pacote de commit:** preparar manifesto de arquivos e mensagens para revisão, incluindo o trabalho
da feature já preservado e os esqueletos/guia; separar alterações preexistentes sem relação.
Não fazer staging global, commit, push ou descarte nesta antecipação. O manifesto deve registrar
que 4.1 ainda está incompleto e que o pacote representa base e estrutura, sem fluxo funcional.

**Riscos:** confundir esqueleto com implementação (Javadoc, inventário e prova de inatividade);
congelar campos/assinaturas cedo (pendências vinculadas às fatias funcionais); afirmar proteção
ArchUnit sem exercitar dependências (fixtures positivas/negativas); incluir mudanças alheias no
commit (manifesto seletivo e revisão do diff). O guia deve apontar a evolução desses controles.

**Arquivos prováveis:** `src/main/java/br/gov/caixa/simtr/{orquestrador,monitoramento,arquitetura}`,
testes correspondentes fora do Hub, consolidado arquitetural e esta pasta de tasks.

### Complemento C4.4 — Javadoc para continuidade manual

**Estado:** concluído em 40 tipos e 11 métodos declarados. DocLint sem erros; 27 avisos
somente de construtores implícitos preservados. Conteúdo fora dos comentários comparado e
preservado; guia atualizado, sem implementação de lógica.

O usuário solicitou complementar os comentários das classes e métodos pendentes conforme Javadoc.
O recorte é exclusivamente documental: ampliar a orientação das 27 classes inativas, documentar
os métodos das 11 portas com parâmetros e retorno esperado e esclarecer os dois records de
parâmetros. Preservar assinaturas, imports, anotações, campos, corpos e todo comportamento.

Usar a sintaxe padrão do JDK 25: descrição antes dos block tags, `@param`, `@return`,
`@see` e links/código inline. Não inventar métodos, exceções contratuais ou garantias já
implementadas. Identificar item funcional, dependências, pendências e verificações esperadas.
Atualizar o guia para orientar a leitura desses comentários.

Verificação documental: conferir referências, parâmetros/retornos e sintaxe com DocLint em
diretório temporário, sem gerar HTML; comparar o conteúdo fora dos Javadocs antes/depois.
Conforme a classificação de documentação em AGENTS.md, este complemento não executa Maven,
baseline, API ou checkpoint Sonar. O checkpoint de 859 testes pertence à estrutura anterior aos
novos comentários; não declarar seu fingerprint idêntico aos fontes após a edição documental.

## Intenção

Implementar no mesmo artifact e runtime Quarkus um primeiro recorte vertical executável para
monitorar um Dossiê de Produto:

```text
POST REST
    -> br.gov.caixa.simtr.orquestrador
    -> fila de entrada
    -> br.gov.caixa.simtr.monitoramento
        -> consulta situação simulada na pré-validação
        -> consulta dossiê pela porta CDI pública do simtr-hub
        -> reagenda ou publica resultado
    -> fila de saída
    -> br.gov.caixa.simtr.orquestrador
    -> log estruturado do resultado
```

O recorte comprovará a extensão Quarkus Azure Service Bus, a topologia das duas filas, os
contratos, o processamento reativo, o settlement explícito e a correlação OpenTelemetry. Como não
haverá Cosmos DB nem outro armazenamento durável nesta feature, ela não será apresentada como
implementação produtiva completa da solução descrita no documento de origem.

## Resultado observável

- uma requisição REST válida recebe `202 Accepted` somente depois que o Service Bus confirma a
  publicação inicial;
- a mensagem é consumida da fila de entrada com `PEEK_LOCK` e auto-complete desabilitado;
- a situação da pré-validação vem de um adapter simulado e a situação do MTR vem da capacidade
  existente `ConsultarDossieProduto`, chamada localmente por porta CDI, sem HTTP para o próprio Hub;
- situação não conclusiva produz nova mensagem agendada; situação conclusiva ou limite funcional
  produz mensagem na fila de saída;
- o listener do orquestrador registra um evento estruturado e somente então conclui a mensagem;
- uma única trilha correlaciona REST, publicação, processamento, consultas, reagendamento,
  publicação da saída e consumo final, sem registrar payload ou segredo.

## Natureza e limites do recorte

Este incremento é um demonstrador funcional de integração e arquitetura. A ausência de
persistência implica:

- a consulta simulada da pré-validação não comprova integração com Cosmos DB;
- `situacaoPreValidacao` na saída representa o estado calculado pelo processor, não uma transição
  duravelmente persistida;
- o log final não equivale à retomada durável de uma orquestração;
- não existe Outbox; uma falha entre publicar a saída e concluir a entrada pode repetir a saída;
- `MessageId` determinístico e duplicate detection reduzem duplicidade, mas não substituem
  idempotência durável;
- o recorte não pode receber classificação de pronto para produção.

Uma feature posterior deverá introduzir persistência, transição da pré-validação, Outbox e estado
idempotente do orquestrador antes de promover o fluxo a produção.

## Stack e versões verificadas

| Elemento | Estado e decisão proposta |
|---|---|
| Java | manter JDK `25` e `maven.compiler.release=25` |
| Quarkus | manter nesta feature o `3.33.2.1` atual; upgrade de manutenção fica fora de escopo |
| Quarkus LTS atual | a linha recomendada continua `3.33`; a manutenção mais recente consultada é `3.33.3.2` |
| Extensão | usar exclusivamente `io.quarkiverse.azureservices:quarkus-azure-servicebus:1.2.5` |
| Status da extensão | `preview`, construída/testada pelo projeto da extensão com Quarkus `3.37.4` e Java `17` |
| Compatibilidade alvo | C1 aceito em 2026-09-05 com provas locais registradas no checklist; permanece o risco da matriz não coberta oficialmente |
| Cliente | `ServiceBusClientBuilder` produzido pela extensão; sender/receiver assíncronos de longa duração |
| Telemetria | `quarkus-opentelemetry` e logs JSON já existentes; sem novo backend de observabilidade |

A exigência “somente a última versão da extensão” significa que falha de compatibilidade bloqueia
o incremento para decisão humana. O plano não autoriza downgrade silencioso da extensão, override
de dependências ou migração do Quarkus.

Fontes oficiais consultadas em 2026-09-04:

- catálogo da extensão, versão e status:
  <https://quarkus.io/extensions/io.quarkiverse.azureservices/quarkus-azure-servicebus/>;
- documentação, autenticação, builder e Dev Services:
  <https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html>;
- matriz de compatibilidade da extensão:
  <https://github.com/quarkiverse/quarkus-azure-services#compatibility-matrix>;
- versões suportadas do Quarkus:
  <https://quarkus.io/releases/>;
- limitações do emulador:
  <https://learn.microsoft.com/en-us/azure/service-bus-messaging/overview-emulator>;
- entrega, settlement e duplicidade:
  <https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-message-loss-and-duplicates>;
- tracing no Quarkus:
  <https://quarkus.io/guides/opentelemetry-tracing/>;
- configuração tipada, grupos, listas e mapas:
  <https://quarkus.io/version/3.33/guides/config-mappings>;
- convenções OpenTelemetry para Azure Service Bus:
  <https://opentelemetry.io/docs/specs/semconv/messaging/azure-messaging/>.

## Escopo

- organizar `br.gov.caixa.simtr.orquestrador` por domínio, aplicação, portas e adapters: receber
  REST, publicar na fila de entrada e consumir a fila de saída por listener;
- organizar `br.gov.caixa.simtr.monitoramento` pela mesma arquitetura: consumir a fila de entrada,
  consultar as fontes, aplicar políticas, reagendar e publicar o resultado na fila de saída;
- migrar código e testes de `doctree.monitoramentomtr` para `monitoramento` dentro do item 4.1,
  preservando comportamento e verificações já concluídas;
- definir contratos versionados e independentes para REST, fila de entrada e fila de saída;
- criar `POST /simtr-hub/v1/monitoramentos-dossie` com request contendo
  `idDossiePreValidacao` e `idDossieMtr` e response contendo `monitoramentoId` e
  `orquestracaoId`;
- retornar `202 Accepted` após confirmação assíncrona do envio; preservar o contrato técnico de
  erro REST já existente;
- gerar `monitoramentoId`, `orquestracaoId`, tentativa inicial e limites no servidor;
- usar `ServiceBusSenderAsyncClient` na publicação e `ServiceBusReceiverAsyncClient` no consumo;
- configurar `PEEK_LOCK`, auto-complete desabilitado, prefetch inicial zero e concorrência limitada;
- aplicar `Complete`, `Abandon` e `DeadLetter` conforme sucesso, falha recuperável e contrato
  permanentemente inválido;
- reagendar tentativa não conclusiva com `scheduleMessage`; tentar transação de entidade única
  para `agendar próxima + Complete atual`, condicionada à prova no emulador e no SDK resolvido;
- consumir a fila de saída no orquestrador e registrar o resultado em log estruturado;
- acessar `ConsultarDossieProduto` por um adapter anticorrupção local, sem chamar o Resource REST;
- implementar uma porta de consulta à pré-validação e um adapter simulado determinístico;
- usar o simulador existente do dossiê do Hub quando a property já existente estiver habilitada;
- iniciar Azure Service Bus Dev Services em dev/test somente quando namespace e connection string
  não estiverem definidos;
- criar `src/main/azure/servicebus-emulator/config.json` com as duas filas;
- fixar imagens do emulador e SQL em versões verificadas pela documentação da extensão, sem tag
  `latest`;
- usar connection string real somente por configuração externa e nunca registrar seu valor;
- instrumentar spans, contexto distribuído e logs estruturados, com testes dos sinais;
- ampliar ArchUnit para proteger os novos packages, adapters e acesso à API pública do Hub;
- atualizar o consolidado arquitetural e ADRs somente conforme decisões aprovadas e estado verde.

## Fora de escopo

- alterar `br.gov.caixa.simtr.dossie`, o código do Hub, seus contratos, logs, erros ou simuladores;
- Cosmos DB, banco relacional ou qualquer persistência real da pré-validação;
- transição durável de situação, Outbox, inbox, estado durável da orquestração ou exatamente uma vez;
- retomada, finalização ou suspensão real de workflow; a saída somente produz log;
- provisionamento de namespace, filas, políticas SAS, Key Vault, rede ou infraestrutura Azure;
- conexão obrigatória a um namespace Azure real nesta feature; ela será um gate posterior quando
  credencial e ambiente forem disponibilizados por canal seguro;
- usar `quarkus-messaging-amqp`, `@Incoming`, `@Outgoing` ou `mp.messaging.*`;
- usar outra versão da extensão se `1.2.5` falhar;
- atualizar Quarkus de `3.33.2.1` para `3.33.3.2` dentro desta feature;
- build nativo, métricas, dashboards e alertas;
- criar endpoint de consulta de estado da orquestração;
- registrar payload, connection string, chave SAS, identidade, namespace, CPF, CNPJ ou dados do
  dossiê em logs ou spans;
- alterar `.ppt`, `.pptx`, `.pdf` ou `.html` derivados.

## Contratos aprovados e implementação incremental

### REST de entrada

```http
POST /simtr-hub/v1/monitoramentos-dossie
Content-Type: application/json

{
  "idDossiePreValidacao": "c5a13bd2-fc7e-45cd-9c47-8a79b5926c86",
  "idDossieMtr": "4324680"
}
```

```http
HTTP/1.1 202 Accepted

{
  "monitoramentoId": "MON-<uuid>",
  "orquestracaoId": "ORQ-<uuid>"
}
```

Regras aprovadas para o contrato REST:

- os dois campos são obrigatórios e não vazios;
- `idDossieMtr` permanece `String` no contrato externo, com valor inteiro decimal no intervalo
  `1..9223372036854775807`, preservando zeros à esquerda, antes de ser traduzido para
  `IdentificadorDossieProduto`;
- campos desconhecidos seguem a política Jackson existente e são ignorados;
- falha de validação usa o erro REST atual; falha de publicação é traduzida sem expor broker,
  namespace ou credencial;
- autenticação/autorização seguem a postura existente do serviço; nenhum papel novo será inventado
  sem decisão humana.

### Fila de entrada v1

O JSON seguirá o contrato completo do documento de origem: `schemaVersion`, `monitoramentoId`,
`orquestracaoId`, `idDossiePreValidacao`, `idDossieMtr`, `tentativaAtual`, `iniciadoEm`,
`limiteEm` e `politicaMonitoramentoVersao`.

Propriedades:

```text
MessageId     = <monitoramentoId>:tentativa:<tentativaAtual>
CorrelationId = <orquestracaoId>
Subject       = MONITORAR_DOSSIE_MTR
ContentType   = application/json
```

### Fila de saída v1

O JSON seguirá o contrato completo do documento de origem, mas nesta feature
`situacaoPreValidacao` será o estado calculado, não persistido. Essa limitação deve aparecer na
documentação do contrato e nos testes.

```text
MessageId     = <monitoramentoId>:resultado:v1
CorrelationId = <orquestracaoId>
Subject       = RESULTADO_MONITORAMENTO_DOSSIE_MTR
ContentType   = application/json
```

DTOs REST, Service Bus, MTR e simuladores não serão reutilizados entre bordas. Produtor e
consumidor da mesma fila terão mapeamento explícito e teste de compatibilidade JSON; não será criado
um DTO global genérico em `br.gov.caixa.simtr.mensageria.contrato` sem aprovação arquitetural.

## Arquitetura alvo: DDD e hexagonal nos dois componentes

A orientação é hexagonal pragmática, não estrita, conforme ADR-0001. Quarkus, Jakarta,
MicroProfile, Mutiny, Jackson e OpenTelemetry podem ser usados no domínio e na aplicação quando
cumprirem uma responsabilidade real. CDI, escopos e injeção não são proibidos nessas camadas;
não criar wrappers ou portas apenas para esconder o framework. Preservar a política pura já
implementada é uma escolha adequada a essa regra, não uma exigência de domínio sem Quarkus.

Quarkus reativo e Mutiny (`Uni` e `Multi` quando necessários) também podem apoiar domínio e
aplicação. Uma regra de negócio continua pertencendo ao seu componente mesmo quando executada
assincronamente: políticas, tentativas funcionais, prazo e processamento em `monitoramento`;
início e tratamento de resultado da orquestração em `orquestrador`.

A capacidade transversal em `br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus` é somente
técnica, para composição compartilhada dos clientes Service Bus, não um destino para todo código
assíncrono. Listeners, publishers, DTOs e mappers continuam nos adapters dos respectivos componentes.
Não criar framework assíncrono genérico, barramento de negócio ou abstrações para necessidades futuras.

`br.gov.caixa.simtr.hub` permanece como está; `hub.arquitetura` conserva as capacidades de suporte
no âmbito do Hub. Não migrar nem generalizar segurança, erros, observabilidade ou outras capacidades
do Hub nesta feature. A nova infraestrutura não importa Hub, orquestrador ou monitoramento; os
núcleos não importam a fábrica ou o SDK. Quarkus reativo no núcleo não altera essas fronteiras.

As filas delimitam a colaboração assíncrona. Cada componente possui modelos internos e casos de
uso próprios; DTOs de transporte ficam nos respectivos adapters. O recorte não cria aggregates,
repositórios ou estado durável sem necessidade funcional aprovada.

| Responsabilidade | `orquestrador` | `monitoramento` |
|---|---|---|
| Entradas | REST para iniciar; listener da fila de saída para receber resultado | Listener da fila de entrada para processar tentativa |
| Aplicação | Iniciar solicitação e registrar resultado recebido | Consultar fontes, aplicar política e coordenar resultado ou reagendamento |
| Domínio | Identificadores, solicitação e resultado da orquestração | Situações, tentativas, prazo, decisões e política de monitoramento |
| Saídas | Publicar solicitação na entrada; registrar resultado em log | Consultar pré-validação e Hub; publicar resultado na saída; reagendar na entrada |

As entidades continuam `q.prevalidacao.monitoramento-mtr.in` e
`q.prevalidacao.monitoramento-mtr.out`. A mudança de package Java não renomeia filas, paths REST,
contratos JSON, properties ou sinais observáveis aprovados.

```text
br.gov.caixa.simtr.orquestrador
|-- dominio
|   |-- modelo
|   `-- erro
|-- aplicacao
|   |-- porta.entrada
|   |-- porta.saida
|   `-- casodeuso
`-- adaptador
    |-- configuracao
    |-- entrada.rest.v1
    |-- entrada.servicebus          # listener da fila de saida
    `-- saida
        |-- servicebus             # publisher da fila de entrada
        `-- log                    # registro do resultado

br.gov.caixa.simtr.monitoramento
|-- dominio
|   |-- modelo
|   |-- erro
|   `-- politica
|-- aplicacao
|   |-- porta.entrada
|   |-- porta.saida
|   `-- casodeuso
`-- adaptador
    |-- configuracao               # configuracao tipada e producer da politica
    |-- entrada.servicebus          # listener da fila de entrada
    `-- saida
        |-- servicebus             # resultado na saida; reagendamento na entrada
        |-- simulador.prevalidacao
        `-- acl.simtrhub
```

A antecipação estrutural autorizada no C4.4 cria os diretórios e classes previstos antes da lógica.
A implementação funcional continua seguindo o checklist. A política pura tem
o destino exato `src/main/java/br/gov/caixa/simtr/monitoramento/dominio/politica/`; os testes
espelharão essa estrutura em `src/test/java/br/gov/caixa/simtr/monitoramento/`.

### Portas e direção das dependências

- REST e listeners validam seu contrato, mapeiam para tipos internos e acionam portas de entrada
  do próprio componente. Não chamam publishers ou casos de uso concretos diretamente;
- casos de uso implementam portas de entrada e dependem de domínio e portas de saída próprios;
- no orquestrador, portas de saída representam publicar a solicitação e registrar o resultado;
- no monitoramento, portas de saída representam consultar pré-validação, consultar situação do
  dossiê, publicar resultado e reagendar tentativa;
- `PoliticaMonitoramento` e `PoliticaMonitoramentoProgressiva` pertencem a
  `monitoramento.dominio.politica`. A aplicação de monitoramento injeta essa política;
  configuração tipada e producer CDI ficam em `monitoramento.adaptador.configuracao`;
- domínio não depende de aplicação ou adapters; domínio e aplicação não recebem tipos do SDK
  Azure, DTOs de transporte, connection string nem handles de settlement;
- serialização, `Complete`, `Abandon`, `DeadLetter` e transação ficam nos adapters Service Bus;
  o SDK é restrito a essas bordas e à fábrica técnica compartilhada. A aplicação expressa a
  decisão sem conhecer o mecanismo de entrega;
- DTOs de produtor e consumidor pertencem às respectivas bordas; testes de compatibilidade JSON
  protegem a comunicação sem um DTO global compartilhado.

O adapter `monitoramento.adaptador.saida.acl.simtrhub` injeta somente
`br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDossieProduto` e traduz
`DossieProdutoConsultado` para um modelo mínimo do consumidor. O processor não importa Resource,
DTO REST/MTR, caso de uso concreto ou porta de saída do domínio `dossieproduto`.

### Extensão, simuladores e composição CDI

Os adapters dos dois componentes usarão a extensão
`io.quarkiverse.azureservices:quarkus-azure-servicebus:1.2.5`, com o builder fornecido por ela.
O SDK assíncrono utilizado através desse builder é parte da integração da extensão. Não será
introduzido cliente construído por fora dela nem mecanismo alternativo de mensageria.

Uma única fábrica CDI será dona do `ServiceBusClientBuilder` injetado e criará clientes de longa
duração. Isso evita mutações concorrentes do builder por múltiplos beans e centraliza lifecycle,
transport, filas e shutdown. Sua localização foi definida como
`br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus`, acessível somente pelos adapters
Service Bus. Os detalhes de injeção e lifecycle estão aprovados no ADR-0011, sem dependência
lateral entre núcleos nem DTOs ou regras de negócio compartilhados. A fábrica ainda não foi
implementada e permanece prevista para o item 6.1.

Dev Services continuará usando o emulador com as duas filas e o `config.json` no caminho atual.
A consulta simulada da pré-validação implementará uma porta de saída de `monitoramento`. A
consulta ao Hub continuará passando por ACL e pela porta pública `ConsultarDossieProduto`, cuja
seleção MTR/simulador pertence ao Hub e usa `simtr-hub.simulador.dossie-produto.habilitado`.
Nenhum dos novos núcleos acessará diretamente fixture, adapter MTR ou adapter simulador do Hub.

### Composição aprovada e pendência contratual do item 4.1

O contrato inicial exige `limiteEm` e `politicaMonitoramentoVersao` antes da publicação pelo
orquestrador, enquanto a política pertence ao monitoramento. A obtenção desses parâmetros foi
definida sem importar sua política ou configuração de borda no núcleo do orquestrador: porta de
saída do consumidor com ACL para uma porta pública local de monitoramento, conforme ADR-0011.
Essa colaboração calcula somente parâmetros iniciais, mantendo processamento e resultado nas
duas filas. Não duplicar a regra, criar DTO compartilhado ou mudar o JSON para essa colaboração.

Detalhamento registrado no [ADR-0011 aceito](../../../doc/adr/0011-composicao-local-monitoramento-e-fabrica-service-bus.md):
porta `ObterParametrosMonitoramento` do orquestrador, ACL local para `PrepararMonitoramento` e
fábrica técnica em `br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus`. O usuário definiu
a localização e os limites técnicos, o uso permitido de Quarkus reativo no domínio e a preservação
do Hub, e aprovou integralmente o ADR-0011. Nenhum desses beans, portas ou package técnico foi
implementado nesta atualização documental; o aceite não altera a ordem dos itens do checklist.

#### Checkpoint humano C4.1 — arquitetura e limite MTR aprovados

Decisão humana registrada em 2026-09-06: “ADR-0011 aprovado”. Isso aprova a colaboração local por
porta/ACL e a composição CDI, mantendo ambas as raízes hexagonais, a localização técnica definida,
Quarkus reativo permitido no núcleo e o Hub preservado.

Em seguida, o usuário declarou “Long do contrato MTR ok, aprovado vamos prosseguir go”. A validação
pública de `idDossieMtr` deve aceitar o intervalo `1..9223372036854775807` (`Long` positivo),
preservando o JSON string e zeros à esquerda. Valores acima desse limite devem ser rejeitados
como entrada inválida antes da publicação, sem alterar o tipo do Hub.

O aceite contratual é próprio e resolve a divergência observada no modelo real do Hub. A subfatia
imediata altera somente request REST, testes focados e documentação: RED para teto excedido,
GREEN com constraint Jakarta Validation, regressão de limites inclusivos e preservação textual,
seguido do checkpoint Sonar. Contratos Service Bus e guardrails restantes continuam em 4.1;
não executar 5.1/6.1 durante sua conclusão.

### Guardrails a implementar

ArchUnit deverá incluir explicitamente os dois novos packages nas regras de aplicação, portas,
isolamento de contratos e acesso entre componentes. A importação atual de todo
`br.gov.caixa.simtr` já alcança seus arquivos, mas várias regras selecionam somente os quatro
domínios internos do Hub; a cobertura genérica não comprova essas novas fronteiras.

Os testes deverão demonstrar os caminhos permitidos e a rejeição de dependência em adapter a
partir do núcleo, DTO vazando de borda, acesso ao caso de uso concreto de outro componente e SDK
Azure fora dos adapters de integração e da fábrica técnica delimitada. Proteger também a ausência
de dependências da infraestrutura nos componentes de negócio e no Hub, sem ampliar o acesso dos
núcleos à fábrica. Incluir provas positivas de uso permitido de Quarkus reativo/Mutiny e CDI no
domínio e na aplicação, sem blacklist genérica de frameworks. Essa proteção começa no item 4.1
e acompanha as fatias que introduzem as demais portas e adapters.

## Configuração e segurança

- decisão humana registrada em 2026-09-04: usar connection string externa nos ambientes reais e
  emulador via Dev Services em dev/test; Microsoft Entra ID fica fora desta feature;
- não declarar
  `quarkus.azure.servicebus.connection-string=${QUARKUS_AZURE_SERVICEBUS_CONNECTION_STRING}` com
  variável obrigatória global: a ausência real da configuração deve permitir que Dev Services
  seja detectado;
- em ambiente real, usar a variável padrão
  `QUARKUS_AZURE_SERVICEBUS_CONNECTION_STRING`; produção deve falhar na inicialização quando ela
  estiver ausente e Dev Services deve permanecer desabilitado;
- connection string deve ser de namespace, não usar `RootManageSharedAccessKey` e possuir apenas
  `Send + Listen`, pois este monólito publica e consome nas duas filas;
- não há privilégio `Manage`; provisionamento permanece externo;
- aceitar as EULAs do emulador e do SQL é checkpoint humano obrigatório. A aceitação será explícita
  por configuração aprovada, nunca inferida pelo agente;
- o emulador suporta somente AMQP/TCP e não WebSockets. Dev/test usarão AMQP/TCP; o ambiente Azure
  manterá `AMQP_WEB_SOCKETS`/443 conforme ADR-0010;
- ativar o mock de pré-validação explicitamente. Fora de dev/test, não haverá fallback silencioso
  para dados simulados;
- selecionar a política de monitoramento por configuração tipada, com uma propriedade para a
  política ativa e um mapa de definições; o producer CDI deve falhar no bootstrap quando a política
  ativa não existir, o tipo não for suportado ou os valores forem inválidos;
- manter todos os parâmetros da política em configuração: versão, tipo, intervalos progressivos,
  máximo opcional de tentativas e duração máxima obrigatória; nenhuma implementação contém valor
  operacional compilado;
- limitar tamanho do body REST e do corpo da mensagem, validar schema/subject/content type e nunca
  desserializar conteúdo não validado diretamente para modelo interno.

### Fronteiras de confiança e ameaças principais

| Fronteira | Risco | Controle planejado |
|---|---|---|
| HTTP -> orquestrador | spoofing, payload inválido, DoS | autenticação vigente, autorização aprovada, validação e limite de tamanho |
| Service Bus -> listener | adulteração lógica, contrato incompatível | validação completa, schema version, subject/content type e DLQ |
| configuração -> SDK | vazamento ou privilégio excessivo | segredo externo, fail-fast, Send+Listen, redaction e nenhum log do valor |
| mock -> regra | dado simulado usado fora do ambiente autorizado | flag explícita, origem no span e bloqueio de fallback produtivo |
| redelivery -> efeitos | logs/saídas duplicados | MessageId determinístico, duplicate detection e limitação documentada |

### C4.2 — log de erro padronizado aprovado nas bordas Service Bus

**Estado:** GO humano explícito recebido em 2026-09-07 para o formato apresentado,
inicialmente somente em logs. Consumidor e produtor da entrada implementados e verificados;
produtor propaga RuntimeException própria conforme autorização humana. Após S1130, nove testes
focados e 825 completos passaram; checkpoint `COMPLIANT`, zero issues novas, cobertura 86,7%,
duplicação 3,8%, nenhuma HIGH/BLOCKER/CRITICAL. S110, S5961 e S1130 `CLOSED/FIXED`; baseline
original preservado. Demais bordas/guardrails permanecem pendentes em 4.1, sem avanço para 5.1.

**Intenção:** toda falha dos contratos Service Bus desta feature deve deixar um registro de erro
JSON que identifique onde ocorreu, classifique o motivo por código estável e permita correlação
para uma ação posterior. A troca para `_` permanece uma correção de análise estática; não satisfaz
sozinha este novo requisito operacional.

**Escopo considerado para esta proposta:** contratos Service Bus de monitoramento/orquestrador,
começando pelo consumidor e produtor de entrada já existentes. Aplicar o mesmo formato às bordas
de reagendamento e resultado quando forem implementadas em 4.1. Hub e REST atuais permanecem
preservados. O GO aprovou registro inicialmente somente em logs. Armazenamento durável e
processamento automático permanecem fora deste incremento.

**Perguntas que o registro deve responder:**

1. Qual componente e operação identificaram a falha?
2. Qual código estável permite classificar o erro sem interpretar texto livre?
3. Qual ocorrência e contexto de trace correspondem à falha?
4. Em erro de JSON, qual a localização segura no documento, sem copiar seu conteúdo?

**Estado confirmado e divergências:**

- `ErroPadraoDto` do REST usa `codigo_http`, `recurso`, `id_erro`, `codigo_erro`,
  `erros: [{mensagem}]`, `detalhe` e `stacktrace`, omitindo propriedades nulas.
- A configuração atual já gera JSON no console e em `target/logs/simtr-hub.json`.
  Isso não comprova que um objeto de erro esteja presente no registro.
- `GenericExceptionMapper` e `MtrRestClientExceptionMapper` registram contexto próprio;
  `ObservabilityLog` converte valores em strings. Logo, reutilizar esse helper não garante
  `erros` como array JSON. O registro REST atual não é uma cópia integral do DTO de resposta.
- Na inspeção inicial, o mapper consumidor classificava e propagava falhas sem log e o produtor
  propagava `JsonProcessingException`. O consumidor agora registra o contrato abaixo e propaga
  id/código; o produtor é a próxima subfatia. Não existem listeners funcionais.
- Não corrigir nem generalizar a observabilidade do Hub silenciosamente. Essa eventual ampliação
  exige autorização própria. O padrão de campos JSON pode ser equivalente com DTOs independentes
  nas bordas, conforme ADR-0004; isso não autoriza importar o DTO/helper do Hub nesses componentes.

**Contrato proposto para o registro:**

O registro mantém os metadados do logger e os campos de contexto usados pelo projeto. O objeto
de erro usa os nomes e tipos do REST. Exemplo ilustrativo de uma falha de desserialização:

```json
{
  "level": "ERROR",
  "evento": "monitoramento.servicebus.entrada.falhou",
  "camada": "adaptador",
  "componente": "MonitoramentoEntradaServiceBusMapper",
  "operacao": "lerContrato",
  "recurso": "q.prevalidacao.monitoramento-mtr.in",
  "id_erro": "3e59459d-aaba-4a2e-a464-2222a5f65589",
  "codigo_erro": "MONITORAMENTO_ENTRADA_JSON_INVALIDO",
  "erros": [
    {"mensagem": "JSON invalido na mensagem de entrada."}
  ],
  "detalhe": "Falha de desserializacao.",
  "linha_json": 4,
  "coluna_json": 12,
  "traceId": "0123456789abcdef0123456789abcdef",
  "spanId": "0123456789abcdef"
}
```

- `id_erro` identifica a ocorrência, inclusive quando o corpo não fornece nenhum identificador
  confiável. Propagar a identificação de forma tipada para evitar registros duplicados depois.
- `codigo_http` é omitido em AMQP; não inventar status HTTP para uma operação de fila.
  Em uma borda HTTP, continua com seu significado e tipo atuais.
- Para exceções técnicas, `stacktrace` mantém somente o nome do tipo e os frames de execução
  (classe, método, arquivo e linha), sem mensagem original, causas ou objetos do parser.
  Componente/operação identificam a etapa; linha/coluna JSON são números extraídos com segurança,
  quando disponíveis. Rejeições de validação usam código/detalhe controlados, sem inventar
  exceção técnica. O teste deve provar localização útil e ausência de payload também nesse campo.
- `traceId`/`spanId` preservam o contexto válido existente. Sem span ativo, omitir esses
  campos e manter `id_erro`; não fabricar trace nem antecipar a Task 10.
- IDs técnicos da mensagem somente entram quando validados e disponíveis. Payload inválido,
  valor rejeitado, sourceRef, mensagem original do parser, credenciais e causas não entram no log.
- `erros` deve ser um array JSON real; um JSON escapado dentro de `message` não comprova
  esse contrato. O teste precisa observar a saída do formatter usado no runtime.
- Esta proposta usa `ERROR` para as falhas classificadas no recorte, como solicitado; sucesso
  não emite evento de erro. Um registro por falha reconhecida, seguido da propagação da falha.
  O log não executa settlement, DLQ, retry, persistência nem ação automática.

| Origem | Evento proposto | Código proposto | Classificação |
|---|---|---|---|
| Consumidor, parsing/desserialização | `monitoramento.servicebus.entrada.falhou` | `MONITORAMENTO_ENTRADA_JSON_INVALIDO` | JSON não interpretável pelo contrato |
| Consumidor, propriedades AMQP | `monitoramento.servicebus.entrada.falhou` | `MONITORAMENTO_ENTRADA_ENVELOPE_INVALIDO` | Envelope ausente ou divergente |
| Consumidor, tipos/schema/campos/valores/datas | `monitoramento.servicebus.entrada.falhou` | `MONITORAMENTO_ENTRADA_CONTRATO_INVALIDO` | Contrato v1 não atendido |
| Produtor, serialização | `orquestrador.servicebus.entrada.falhou` | `ORQUESTRADOR_ENTRADA_SERIALIZACAO_FALHOU` | Falha ao montar o JSON da entrada |

Os nomes/códigos acima integram o formato apresentado e aprovado no GO de C4.2. Detalhes de
mensagens são controlados pela aplicação. A ação posterior usa código e contexto; não dependerá
de regex sobre stacktrace ou texto de exception.

**Sequência, dependências e critérios de aceitação:**

1. C4.2 aprovado pelo usuário em 2026-09-07: escopo, JSON, códigos, nível e campos permitidos;
   inicialmente somente logs. Necessidade posterior de armazenamento/processamento revisa o plano.
2. Retomar o baseline original existente, sem reinicialização. Antes de produção, provar por
   RED a saída JSON real, identificação/localização, propagação e ausência de dados protegidos.
   Não alterar formatter global, log do Hub, dependência ou package transversal sem detalhar
   a necessidade e obter o checkpoint correspondente.
3. Implementar primeiro a fatia do consumidor, com tipos próprios da borda e um registro por
   falha de parsing, envelope ou validação. Preservar aceitação/rejeição dos contratos e os
   testes existentes; não antecipar listener.
4. Verificar o consumidor, revisar as sete dimensões e executar checkpoint Sonar do incremento.
   Em seguida, dentro de 4.1, aplicar o contrato ao produtor, preservando propagação de falha
   e ausência de publicação. Reagendamento/resultado seguem suas subfatias existentes.
5. Testar evento/código, estrutura e tipos JSON, correlação disponível, restauração de contexto,
   ausência de duplicação e vazamento, sucesso sem erro, entrada inválida sem sucesso e diagnóstico
   de localização. Testes de contrato de entrada/REST e ArchUnit existentes continuam verdes.

**Arquivos prováveis:** mapper e exceção em
`monitoramento/adaptador/entrada/servicebus/`, DTO e registro de erro locais nessa borda e
testes espelhados; depois mapper/DTO/registro de erro de
`orquestrador/adaptador/saida/servicebus/` e seus testes. Manter fatias de até cinco arquivos
Java; detalhar eventual adaptação de formatter antes de implementá-la. Atualizar arquitetura
somente quando o estado implementado mudar e registrar execução em tasks.

**Verificações:** RED/GREEN dos testes de log real e regressão das bordas; suíte completa e
`./validar-checkpoint-sonarqube.ps1` após cada incremento coerente, preservando a referência
recuperada. Esta revisão de planejamento não altera código/tooling e não executa Maven/Sonar.

**Riscos/fora de escopo:** o helper atual transforma objetos em strings, portanto a emissão do
array `erros` exige prova no formatter efetivo Quarkus `3.33.2.1`; a técnica não será declarada
verificada antecipadamente. Não usar captura genérica para esconder erro inesperado. Sem alteração
global do Hub/REST, biblioteca genérica de erro, backend novo, publicação de erro em fila,
persistência, ação automática, listener, 5.1 ou artefatos derivados.

Referência da plataforma: [Quarkus 3.33 — JSON logging e MDC](https://quarkus.io/version/3.33/guides/logging/).
Versão instalada conferida: `io.quarkus:quarkus-logging-json:3.33.2.1`.

### C4.3 — adaptação técnica do formatter (proposta após a prova)

**Estado:** autorizado pelo usuário em 2026-09-07, exclusivamente para o novo desenvolvimento,
preservando código, logs e erros de `simtr.hub`. C4.2 permanece aprovado; nenhuma repetição do GO.

**Necessidade comprovada:** uma prova Java temporária executada com os binários efetivos
Quarkus `3.33.2.1` e JBoss Log Manager `3.2.1.Final` confirmou que lista e número no MDC
saem como strings. Um decorator isolado produziu `erros` como array e localização numérica,
manteve `message` como evento e omitiu `codigo_http`; seis verificações de formato passaram.
Um registro de Hub sem marcador foi delegado com saída idêntica byte a byte. A prova não
instalou handlers nem verificou lifecycle, concorrência ou o mapper.

**Proposta concreta:** [ADR-0012](../../../doc/adr/0012-campos-json-tipados-logs-service-bus.md),
aceito com essa restrição. Criar composição exclusivamente técnica em
`arquitetura.infraestrutura.observabilidade`: marcador imutável de campos JSON já sanitizados,
decorator dos formatters JSON instalados e instalação/restauração CDI. DTOs, classificação e
sanitização ficam nas bordas. O decorator modifica somente registros com o marcador específico;
os demais são delegados integralmente. Categorias do Hub são sempre excluídas, mesmo com marcador;
provar saída idêntica e preservar todos os hashes de seus fontes/testes. Sem novo destino, alteração de extensão/dependência,
reabertura de arquivo, mudança de níveis/filtros/rotação ou migração do Hub.

**Motivo do checkpoint adicional:** a integração alcança handlers compartilhados e introduz uma
capacidade transversal. Isso está condicionado explicitamente no passo 2 de C4.2 e no ADR-0011.
A resposta humana autorizou a adaptação somente para o novo desenvolvimento; esse é o aceite de
C4.3, mantendo a condição de Hub inalterado.

**Próxima sequência dentro de 4.1:** C4.3/ADR-0012 autorizado com restrição; executar RED/GREEN da
composição técnica (até cinco arquivos Java), regressão/Sonar; em seguida consumidor (mapper,
exceção, representação/registro local e testes), com seu checkpoint; depois produtor.
As demais pendências de 4.1 e o impedimento de avançar para 5.1 continuam iguais.

**Critérios de aceitação/riscos:** observar JSON real nos destinos configurados, preservar logs
existentes por comparação de saída, suportar lifecycle/handlers intermediários, não duplicar
registro nem misturar contexto concorrente, não sobrescrever metadados, não incluir payload
ou Throwable original. Apenas erros marcados pagam o parse/serialização adicional.
A prova isolada não representa implementação nem aprovação técnica do runtime.

**Arquivos prováveis:** até três classes técnicas e dois testes na primeira subfatia;
nenhuma alteração em `hub/`, `pom.xml`, configuração Azure ou emulador.
Arquitetura consolidada será atualizada após implementação; ADR e índice registram apenas proposta.

**Evidência do incremento C4.3:** composição técnica implementada em três classes e dois testes;
184 testes focados e 799 completos aprovados. Checkpoint de 2026-09-07 `NON_COMPLIANT` por
S6878/S5786 novas; cobertura 86,5%, duplicação 3,9%, nenhuma HIGH/BLOCKER/CRITICAL.
Baseline original idêntico; 493 arquivos do Hub com hashes preservados. Usuário decidiu
`ContinuarAjustes`, registrado em `2026-09-07T09:53:45.1657041-03:00`. Corrigir as duas issues
e completar as provas do console e do observer CDI, preservando os demais componentes.
Após os ajustes, prova do console real JSON/textual e observer CDI concluída: 186 focados,
801 completos e checkpoint `COMPLIANT`, zero issues novas, cobertura 86,5%, duplicação 3,9%.
C4.3 concluído. O consumidor de C4.2 também foi concluído: RED comportamental confirmado,
204 testes focados e 819 completos aprovados; checkpoint `COMPLIANT`, zero issues novas,
cobertura 86,6%, duplicação 3,8%, baseline original idêntico. Erro JSON real classificado,
id/código propagados e aceitação/rejeição dos contratos preservada. Detalhes e IDs no `todo.md`.

**Próxima fatia — produtor da entrada:** preservar a assinatura checked `JsonProcessingException`
e o JSON/AMQP de sucesso. Capturar somente falha de serialização, registrar uma ocorrência com
DTO/helper próprios e propagar subtipo local com id/código e mensagem controlada, sem causa
original. Obter o recurso da configuração de fila existente; manter trace válido e stack seguro.
Até cinco arquivos Java: mapper, exceção, DTO, registro local e teste. Provar RED no arquivo JSON
real antes de produção, sanitização de mensagem/causas/suppressed, sucesso sem erro, propagação,
isolamento do trace e compatibilidade existente; executar regressão e checkpoint antes das
demais bordas. Sem publicação, agendamento, novos destinos ou configuração do Hub.

**Resultado do produtor e ajuste proposto:** implementação e regressão aprovadas; Sonar aponta
S110 na herança da exceção Jackson e S5961 por 29 asserções em um teste. Cobertura 86,7%,
duplicação 3,8%, duas issues novas MAJOR/MEDIUM, nenhuma HIGH/BLOCKER/CRITICAL; baseline original
idêntico. Situação `NON_COMPLIANT`, sem reprovação automática; decisão humana pendente.
Proposta para `ContinuarAjustes`: derivar a exceção local de `IOException` e declarar o subtipo
em `throws` do mapper package-private, mantendo falha checked, id/código e sanitização.
Trata-se de mudança explícita do tipo técnico desse mapper novo, sem alterar JSON/AMQP ou Hub.
Separar o teste entre contrato JSON e identidade/propagação, preservando todas as asserções.
Ajustes não aplicados; repetir regressão/checkpoint somente após registrar a decisão humana.
Evidência completa e IDs no `todo.md`.

**Revisão humana posterior:** autorização explícita limitada a S5961; dividir o teste mantendo
as asserções e executar regressão/checkpoint. Usuário quer RuntimeException e esclarecimento de
S110. A proposta de IOException acima fica substituída pela proposta de exceção local derivada
diretamente de RuntimeException, removendo a declaração checked do mapper e preservando captura
do Jackson, classificação, log sanitizado e propagação de id/código. Consumidor já usa
RuntimeException; produtor ainda é checked no estado atual. Não alterar a hierarquia de produção
neste incremento de S5961. Registrar a decisão humana sobre S110 antes de implementá-la.
A divisão S5961 foi concluída e revisada: todas as 29 verificações preservadas, nove testes
focados e 825 completos aprovados. Sonar confirma S5961 `CLOSED/FIXED`; permanece S110 como
única issue nova, MAJOR/MEDIUM, cobertura 86,7%, duplicação 3,8%, nenhuma HIGH/BLOCKER/CRITICAL.
Baseline e produção preservados. Evidências completas e IDs no checklist.

**Autorização S110 com RuntimeException:** usuário decidiu “ContinuarAjustes com RuntimeException”.
Aplicar herança direta de RuntimeException à exceção local do produtor e retirar a declaração
checked da assinatura do mapper. Preservar mensagem controlada, id/código, causa nula, catch
específico do Jackson, formato/log único e JSON/AMQP de sucesso. A proposta de IOException
permanece descartada. RED exigirá propagação RuntimeException; testes de metadata Jackson
serão adaptados à ausência de causa/suppressed e de valores sensíveis na exceção local.
Na prova de propriedades AMQP existente, remover apenas o `throws Exception` do teste,
validando compilação do chamador sem obrigação checked. Escopo: quatro arquivos Java do produtor
(dois de produção e dois de teste), docs da feature e consolidado após implementação.
Regressão/revisão e checkpoint com baseline original são obrigatórios antes de concluir a correção.
Demais pendências de 4.1 permanecem; 5.1 não será iniciado.

**Resultado da correção RuntimeException:** implementação, revisão e regressão concluídas;
nove focados e 825 completos passaram. Sonar confirmou S110/S5961 `CLOSED/FIXED`, mas apontou
S1130, MINOR/LOW, no `throws Exception` residual do teste de compatibilidade entre as bordas.
Cobertura 86,7%, duplicação 3,8%, nenhuma HIGH/BLOCKER/CRITICAL; baseline original idêntico.
Proposta restrita para a próxima decisão: remover somente essa declaração de
`deveSerCompativelComContratoIndependenteDoConsumidor()`, mantendo corpo/asserções, a declaração
necessária do teste de leitura JSON e toda produção. Usuário autorizou `ContinuarAjustes`;
registrar a decisão, aplicar somente essa remoção e executar regressão/checkpoint com baseline
original. Não introduzir testes, dependências ou alterações de comportamento. Evidências no checklist.

**Resultado S1130 e estado atual:** retirada somente a declaração desnecessária, com comparação
integral do arquivo e revisão independente. Nove testes focados e 825 completos passaram;
checkpoint `COMPLIANT` em `2026-09-07T16:06:58.5792654-03:00`, zero issues novas, cobertura
86,7%, duplicação 3,8%, nenhuma HIGH/BLOCKER/CRITICAL. S110/S5961/S1130 confirmadas `CLOSED/FIXED`;
baseline integralmente idêntico. Logs da entrada em produtor/consumidor verificados; aplicar o
formato aprovado às futuras bordas próprias quando suas subfatias de 4.1 forem implementadas.
Reagendamento, resultado e novos guardrails continuam pendentes; 5.1 não iniciado. IDs no checklist.

## Telemetria

Perguntas operacionais que os sinais devem responder:

1. A requisição foi aceita pelo broker ou falhou antes da publicação?
2. Em qual tentativa e decisão cada monitoramento está, e qual dependência falhou?
3. A mensagem foi concluída, abandonada, reagendada ou enviada à DLQ, e por quê?
4. O resultado chegou ao listener do orquestrador e foi registrado?

Regras:

- nomes de spans de negócio propostos para o checkpoint C0.4:
  - `simtr-hub.api.monitoramento-dossie.iniciar` (`SERVER`);
  - `orquestrador.service.monitoramento-dossie.iniciar` (`INTERNAL`);
  - `doctree.service.prevalidacao.dossie.consultar` (`INTERNAL`);
  - `doctree.service.monitoramento-mtr.avaliar` (`INTERNAL`);
  - `orquestrador.service.monitoramento-dossie.resultado-registrar` (`INTERNAL`);
- spans de mensageria seguirão o formato `{messaging.operation.name} {fila}`:
  - `send q.prevalidacao.monitoramento-mtr.in` (`PRODUCER`);
  - `process q.prevalidacao.monitoramento-mtr.in` (`CONSUMER`);
  - `schedule q.prevalidacao.monitoramento-mtr.in` (`PRODUCER`);
  - `send q.prevalidacao.monitoramento-mtr.out` (`PRODUCER`);
  - `process q.prevalidacao.monitoramento-mtr.out` (`CONSUMER`);
  - settlement usará `complete`, `abandon` ou `dead_letter` seguido do nome da fila, com
    `SpanKind.CLIENT`;
- eventos de log estáveis serão
  `orquestrador.monitoramento-dossie.publicacao.confirmada`,
  `orquestrador.monitoramento-dossie.publicacao.falhou`,
  `doctree.monitoramento-mtr.decisao.tomada`,
  `doctree.monitoramento-mtr.processamento.falhou`,
  `doctree.monitoramento-mtr.settlement.executado`,
  `orquestrador.monitoramento-dossie.resultado.registrado` e
  `orquestrador.monitoramento-dossie.resultado.falhou`;
- campos estruturados permitidos incluem `monitoramento_id`, `orquestracao_id`, `message_id`,
  `tentativa_atual`, `delivery_count`, `sequence_number`, `decisao`, `settlement`, `error_type`,
  `camada`, `componente` e `operacao`, além da correlação de trace já fornecida pelo runtime;
- spans de envio terão `SpanKind.PRODUCER`; processamento terá `SpanKind.CONSUMER`; unidades
  internas e consultas manterão os tipos já usados no projeto;
- atributos de mensageria seguirão, quando suportados pela versão resolvida, `messaging.system`,
  `messaging.destination.name`, `messaging.operation.name`, `messaging.operation.type` e
  `error.type`;
- `monitoramentoId`, `orquestracaoId`, `messageId`, `deliveryCount`, `tentativaAtual` e
  `sequenceNumber` podem estar em logs/spans, mas nunca como labels de métrica;
- o contexto W3C será propagado nas application properties da mensagem somente se a instrumentação
  do Azure SDK resolvida não comprovar a propagação automática; não haverá spans duplicados;
- a Task 10 caracterizará primeiro nomes, atributos e propagação realmente emitidos pelo SDK. Se a
  versão resolvida usar convenção legada incompatível, a divergência voltará a checkpoint humano;
  spans manuais preencherão somente lacunas comprovadas;
- payload, connection string, namespace, documentos e PII não entram na telemetria;
- os testes devem provar nomes, atributos permitidos, ausência de campos proibidos e uma cadeia de
  trace contínua no fluxo assíncrono.

## Estratégia de processamento

1. validar envelope e JSON;
2. consultar situação simulada da pré-validação;
3. se não for `EM_ANALISE_ENVIO_MTR`, concluir como no-op e registrar a decisão;
4. validar prazo e tentativas;
5. se limite atingido, calcular `QUARENTENA` e publicar a saída;
6. caso contrário, consultar o dossiê pela porta local do Hub;
7. mapear `CONFORME`, `NAO_CONFORME` e `PENDENTE_INFORMACAO` como terminais;
8. para situação diferente, agendar a próxima tentativa e concluir a atual atomicamente quando a
   API/emulador comprovarem suporte;
9. publicar o resultado terminal e somente depois concluir a entrada;
10. no listener de saída, validar, registrar log estruturado e concluir; falha recuperável abandona,
    contrato inválido vai para DLQ.

Falha técnica não incrementa `tentativaAtual`; redelivery técnica continua representada por
`DeliveryCount`.

## Comandos previstos

Os comandos abaixo pertencem à implementação futura, depois dos checkpoints e do baseline:

```powershell
java -version
mvn -version
mvn -q dependency:tree
mvn -q -Dtest=<teste-focado> test
mvn -q test
./validar-checkpoint-sonarqube.ps1
mvn quarkus:dev -Ddebug=false
```

O baseline e os checkpoints já executados estão registrados no checklist. A revisão exclusivamente
Markdown não inspeciona `sonar/`, não solicita token e não executa Maven/Sonar. Ao retomar código,
preservar as alterações existentes e tratar a continuidade do estado Sonar conforme `AGENTS.md`,
sem retirar código para refazer um baseline anterior.

## Estratégia de testes

- teste de resolução do BOM e bootstrap CDI no Quarkus `3.33.2.1`/JDK `25`;
- testes de contrato REST para path, validação, `202`, JSON, OpenAPI e erros;
- testes de serialização e compatibilidade dos contratos v1 das duas filas;
- testes unitários RED/GREEN da política de tentativas, prazo e mapeamento de situações;
- testes do mock de pré-validação e da ACL local de `ConsultarDossieProduto`;
- testes de publisher/listeners para Complete, Abandon e DeadLetter;
- teste de transação `schedule + Complete`, com skip proibido: indisponibilidade deve bloquear a
  alegação de atomicidade e ser apresentada ao usuário;
- teste Quarkus com Dev Services e duas filas reais no emulador;
- teste ponta a ponta do POST até o evento de log da fila de saída, com delays curtos apenas no
  profile de teste;
- testes OpenTelemetry com exporter em memória e inspeção dos logs estruturados;
- ArchUnit para os núcleos e as bordas de `orquestrador` e `monitoramento`, incluindo provas
  negativas das dependências proibidas e acesso ao Hub somente pela ACL;
- suíte completa e checkpoint SonarQube ao fim de cada incremento coerente que altere fingerprint.

## Tarefas

### Task 1 — Resolver a decisão arquitetural antes do código

**Descrição:** propor ADR sucessor do ADR-0009 para registrar extensão `1.2.5`, connection string,
Dev Services e transporte por profile, sem apagar o histórico da decisão anterior.

**Critérios de aceitação:**

- ADR novo começa como `Proposto` e referencia o ADR-0009;
- índice explica extensão, autenticação, emulador, limitação de compatibilidade e aplicabilidade;
- ADR-0009 somente recebe `Substituído por ADR-NNNN` depois de aprovação humana.

**Verificação:** inspeção do diff Markdown e links; nenhuma mudança derivada.

**Dependências:** checkpoint C0 de arquitetura.

**Arquivos prováveis:** `doc/adr/0010-*.md`, `doc/adr/README.md`,
`doc/adr/0009-azure-sdk-service-bus-dossie.md`.

**Escopo estimado:** médio, 3 arquivos.

### Task 2 — Provar compatibilidade da extensão 1.2.5

**Descrição:** após baseline, adicionar o BOM/extensão e um smoke test mínimo que prove resolução,
compilação, augmentation e injeção do `ServiceBusClientBuilder` no stack atual.

**Critérios de aceitação:**

- somente a versão `1.2.5` é usada;
- `mvn dependency:tree` não revela conflito não explicado entre Quarkus, Azure Core, Vert.x,
  Reactor, Netty, Jackson ou OpenTelemetry;
- JDK `25`, compilação e bootstrap Quarkus ficam verdes sem namespace Azure real.

**Verificação:** versões do Java/Maven, dependency tree, teste focado e checkpoint SonarQube.

**Dependências:** Task 1 aceita, GO e baseline SonarQube.

**Arquivos prováveis:** `pom.xml`, um teste de compatibilidade e configuração mínima de teste.

**Escopo estimado:** médio, até 3 arquivos.

### Checkpoint C1 — Compatibilidade

- apresentar árvore efetiva, versões e resultado de augmentation;
- falha exige decisão humana; não fazer downgrade, override ou upgrade por suposição.

### Task 3 — Configurar duas filas no Dev Services

**Descrição:** configurar o emulador com as filas de entrada/saída, imagens fixadas e profiles que
selecionam emulador ou conexão externa sem fallback produtivo.

**Critérios de aceitação:**

- ausência de namespace/connection string em dev/test inicia o emulador;
- presença de connection string usa o ambiente real e não inicia Dev Services;
- dev/test usa AMQP/TCP; profile Azure usa WebSockets/443;
- segredo não aparece em source, logs, erros ou relatório de teste.

**Verificação:** bootstrap com Docker, inspeção das entidades e teste de seleção por profile.

**Dependências:** C1 e aceitação humana das EULAs.

**Arquivos prováveis:** `src/main/azure/servicebus-emulator/config.json`,
`src/main/resources/application.properties`, `src/test/resources/application.properties` e até
dois testes/profiles.

**Escopo estimado:** médio, até 5 arquivos.

### Task 4 — Fixar contratos e política de monitoramento

**Descrição:** implementar primeiro os testes e depois os tipos/mappers mínimos dos contratos REST
e Service Bus, junto da política pura de tentativa/prazo/situação. A política será uma porta
injetável produzida por CDI a partir de um objeto de configuração que seleciona uma definição
nomeada. Retomar o GREEN existente e alinhar os packages conforme a revisão humana, sem refazer
a política nem reabrir os itens 2.1 e 3.1.

**Subfatias de retomada, na ordem:**

1. Migrar `PoliticaMonitoramento`, `PoliticaMonitoramentoProgressiva`, seu teste e os dois testes
   de infraestrutura Service Bus de `doctree.monitoramentomtr` para `monitoramento`. Atualizar
   declarações/imports, preservar os testes e verificar ausência de referências Java ao package
   anterior. Não mover `src/main/azure/servicebus-emulator/config.json`.
2. Criar e executar o RED da configuração tipada e do producer CDI; fazer o GREEN em
   `monitoramento.adaptador.configuracao`, com os valores em `application.properties`.
3. Completar contratos e mappers mínimos, respeitando o ownership de cada borda, e detalhar a
   obtenção dos parâmetros iniciais e a composição técnica CDI antes dos respectivos checkpoints.
4. Acrescentar as provas ArchUnit aplicáveis aos tipos introduzidos; executar testes focados,
   revisão do incremento e checkpoint Sonar do item 4.1. Manter 5.1 pendente.

**Critérios de aceitação:**

- JSON e propriedades AMQP coincidem com os contratos aprovados;
- contratos inválidos são classificados sem depender do SDK;
- política distingue tentativa funcional de redelivery técnica;
- política e testes residem na raiz `monitoramento`, sem `doctree`, `monitoramentomtr` ou um
  subpackage `monitoramento.simtr`; domínio, aplicação, portas e adapters têm limites testáveis,
  permitindo Quarkus no domínio e na aplicação conforme ADR-0001;
- política progressiva exige ao menos um intervalo e repete o último intervalo quando a tentativa
  ultrapassa a lista; uma lista com apenas PT30M agenda sempre a cada 30 minutos;
- máximo de tentativas é opcional e, quando ausente, não participa da decisão; duração máxima é
  obrigatória e permanece ativa;
- política ativa, versão, tipo, intervalos, máximo opcional e duração vêm integralmente de
  application.properties; seleção ou valores inválidos falham no bootstrap.

**Verificação:** regressão da migração, RED/GREEN de configuração e bootstrap CDI, testes
unitários/de serialização, ArchUnit e checkpoint Sonar do incremento.

**Dependências:** C0.1 de arquitetura, C0.2 de contrato, decisão de política configurável e
revisão humana dos packages; C2 continua posterior ao item 6.1.

**Arquivos prováveis:** até cinco arquivos por subfatia em packages de domínio, aplicação,
configuração e DTOs de borda; dividir a task em subfatias internas se o limite for excedido.

**Escopo estimado:** médio por subfatia, máximo 5 arquivos.

#### Contratos REST — recorte de continuidade de 4.1

Implementar em duas subfatias, sem Resource, caso de uso, publicação ou novo endpoint:

1. RED/GREEN do request, validação Jakarta, tipo semântico `SolicitacaoMonitoramento` próprio do
   orquestrador e mapper REST -> interno (quatro arquivos contando o teste);
2. RED/GREEN do response, tipo semântico `MonitoramentoIniciado` e retorno pelo mesmo mapper
   (quatro arquivos contando a ampliação do teste).

Os dois identificadores de dossiê permanecem strings também neste modelo inicial, sem normalização
ou importação de tipos do Hub. Aplicar apenas a validação aprovada: campos não vazios e MTR como
inteiro decimal positivo. Não impor UUID à pré-validação. Provar JSON camelCase, ausência de
parâmetros controlados pelo servidor no request e tolerância vigente a campos desconhecidos usando
Jackson e Validator reais do Quarkus. Geração de IDs, HTTP 202, erro REST e OpenAPI do endpoint
permanecem em 6.1.

Divergência contratual resolvida no C4.1: `IdentificadorDossieProduto` do Hub recebe `Long` e o
usuário aprovou o limite público `1..9223372036854775807`. O request REST já rejeita valores acima
do teto, com regressão de limites e zeros à esquerda. A futura conversão na ACL de 5.1 deve
respeitar esse intervalo; nem essa ACL nem o endpoint foram implementados nesta subfatia.

Após os testes focados, revisar e executar checkpoint Sonar do incremento. Contratos Service Bus,
guardrails dos novos componentes e detalhamento arquitetural continuam pendentes dentro de 4.1.

Estado desta subfatia: request/response, modelos próprios e mapper implementados por RED/GREEN;
23 testes de contrato e 36 testes ArchUnit existentes aprovados. O primeiro checkpoint completo
ficou `NON_COMPLIANT` por `java:S6353`; após `ContinuarAjustes` humano e simplificação da regex,
706 testes, build e Compute Engine passaram e o novo checkpoint ficou `COMPLIANT`, conforme
evidências do checklist. Isso não conclui 4.1 nem comprova o endpoint HTTP.

Referências conferidas no runtime `3.33.2.1`: [validação Quarkus](https://quarkus.io/guides/validation/)
e [Jackson no Quarkus REST](https://quarkus.io/guides/rest-json/#configuring-json-support).

#### Contratos Service Bus — continuidade de 4.1

Executar RED/GREEN em subfatias de até cinco arquivos Java, com registro das evidências:

1. Produtor da entrada no orquestrador: modelo semântico próprio `TentativaMonitoramento`,
   DTO v1 de saída e mapper para corpo JSON/propriedades AMQP, com teste usando o ObjectMapper
   real do Quarkus. Apenas montar a mensagem; não criar client, publicar nem agendar.
2. Consumidor da entrada no monitoramento: DTO próprio, validação de contrato antes do
   mapeamento e modelo semântico próprio. Provar compatibilidade do JSON do produtor e
   classificação de conteúdo inválido sem depender de tipos do SDK.
3. DTO/mapper de reagendamento na saída do monitoramento, preservando o contrato da entrada.
4. Contratos/mappers de resultado nas duas bordas, também independentes e com prova de
   compatibilidade JSON; não implementar processamento terminal ou settlement.
5. Guardrails ArchUnit aplicáveis, provas negativas e permissão positiva de Quarkus/Mutiny/CDI.

Os contratos mantêm os campos v1 aprovados, identificadores string e limite MTR já aceito.
Nenhum DTO é compartilhado entre bordas; SDK permanece nos adapters Service Bus. A montagem
de uma mensagem não é sua publicação: factory, clients, listeners e endpoint continuam nos
itens posteriores. Revisar e executar checkpoint Sonar após incremento coerente.

### Task 5 — Implementar consultas da pré-validação e do Hub

**Descrição:** criar em `monitoramento` a porta de consulta à pré-validação, seu adapter simulado
e a ACL local para a porta pública `ConsultarDossieProduto`.

**Critérios de aceitação:**

- mock retorna cenários determinísticos e identifica origem simulada;
- ACL traduz somente identificador e situação necessários ao consumidor;
- não há HTTP local nem dependência em Resource, DTO MTR/REST, caso de uso concreto ou porta de
  saída do Hub.

**Verificação:** testes unitários, teste CDI e ArchUnit RED/GREEN.

**Dependências:** Task 4 e checkpoint arquitetural.

**Arquivos prováveis:** `monitoramento.{dominio,aplicacao.porta.saida}`, adapters em
`monitoramento.adaptador.saida.{simulador.prevalidacao,acl.simtrhub}` e testes; máximo 5 por subfatia.

**Escopo estimado:** médio por subfatia.

### Task 6 — Entregar POST até a fila de entrada

**Descrição:** implementar a primeira fatia vertical do endpoint, caso de uso e publisher
assíncrono do orquestrador: REST -> porta de entrada -> caso de uso -> porta de saída -> adapter
Service Bus. A regra de monitoramento permanece no componente `monitoramento`.

**Critérios de aceitação:**

- request válido gera IDs e mensagem v1 determinística;
- resposta `202` ocorre depois da confirmação do broker;
- validação/falha usam o contrato de erro existente sem revelar detalhes do Service Bus.

**Verificação:** contrato REST, teste unitário do publisher e teste com fila do emulador.

**Dependências:** Tasks 3 e 4; C0.1 a C0.4 e definição da composição CDI/obtenção de parâmetros
iniciais. C2 revisa esta fatia depois de implementada.

**Arquivos prováveis:** até 5 por subfatia em
`br.gov.caixa.simtr.orquestrador.{dominio,aplicacao,adaptador}`.

**Escopo estimado:** médio por subfatia.

### Checkpoint C2 — Primeira fatia vertical

- POST publica exatamente uma mensagem válida na entrada;
- testes focados, arquitetura, segurança, telemetria e SonarQube sem decisão pendente.

### Task 7 — Processar mensagem terminal da entrada até a saída

**Descrição:** em `monitoramento`, consumir uma mensagem pelo adapter de entrada, acionar a porta
de aplicação, consultar as duas fontes por portas de saída, aplicar a política do domínio e
publicar o resultado pelo adapter Service Bus; concluir a entrada após confirmação.

**Critérios de aceitação:**

- processamento permanece não bloqueante e com concorrência limitada;
- falha recuperável abandona; contrato inválido envia à DLQ;
- `Complete` da entrada ocorre somente depois da confirmação da saída.

**Verificação:** testes RED/GREEN do caso de uso, listener e integração com emulador.

**Dependências:** Tasks 3, 4 e 5.

**Arquivos prováveis:** até 5 por subfatia em
`br.gov.caixa.simtr.monitoramento.{dominio,aplicacao,adaptador}`.

**Escopo estimado:** médio por subfatia.

### Task 8 — Reagendar situação não conclusiva

**Descrição:** em `monitoramento`, executar a decisão da política progressiva através da porta de
reagendamento. O adapter Service Bus realiza a transação de entidade única para agendar a próxima
mensagem e concluir a atual.

**Critérios de aceitação:**

- delay, tentativa e limite seguem a política versionada;
- falha técnica não incrementa tentativa funcional;
- commit confirma ambos os efeitos ou rollback mantém a mensagem atual disponível.

**Verificação:** testes da política, commit/rollback/redelivery no emulador e checkpoint técnico da
API efetivamente resolvida.

**Dependências:** Task 7.

**Arquivos prováveis:** serviço de reagendamento, handler de decisão e até três testes.

**Escopo estimado:** médio, até 5 arquivos.

### Task 9 — Consumir a saída e registrar o resultado

**Descrição:** implementar em `orquestrador.adaptador.entrada.servicebus` o listener da fila de
saída. Ele valida e mapeia a mensagem, aciona a porta de entrada e o caso de uso registra o
resultado por porta de saída implementada pelo adapter de log; o listener então faz o settlement.

**Critérios de aceitação:**

- evento estável permite localizar `monitoramentoId` e `orquestracaoId`;
- não registra body, PII, namespace ou segredo;
- C9.1-L: submissão ao logger permite `Complete`; falha anterior à submissão propagada pela porta
  gera `Abandon`; contrato inválido vai à DLQ. Falhas internas de escrita/filtros podem perder o log
  após Complete, sem reentrega para recuperá-lo.

**Verificação:** testes do listener, conteúdo do log e redelivery.

**Dependências:** Task 7 e checkpoint C0.4 de observabilidade.

**Arquivos prováveis:** listener/mapper, porta/caso de uso de recebimento, porta/adapter de log e
testes em `orquestrador`; dividir em subfatias de até 5 arquivos.

**Escopo estimado:** médio, até 5 arquivos.

### Task 10 — Fechar correlação OpenTelemetry ponta a ponta

**Descrição:** verificar instrumentação do SDK resolvido, adicionar apenas os spans/propagação que
faltarem e proteger o contrato observável.

**Critérios de aceitação:**

- trace do POST alcança o processamento da entrada e o consumo da saída;
- send/process/settle seguem semântica de mensageria aprovada e não duplicam spans do SDK;
- falhas têm status e `error.type` controlado; campos proibidos permanecem ausentes.

**Verificação:** exporter em memória, captura de logs e testes de contrato observável.

**Dependências:** Tasks 6 a 9.

**Arquivos prováveis:** helper de propagação somente se necessário e testes de observabilidade;
instrumentação funcional deve ser incorporada às classes das fatias anteriores.

**Escopo estimado:** médio, até 5 arquivos.

### Checkpoint C3 — Fluxo ponta a ponta

- executar POST e observar entrada, consulta simulada, consulta Hub, reagendamento/resultado e log;
- provar Complete, Abandon, DLQ e correlação sem segredo/PII;
- registrar limitações do emulador e o que ainda exige Azure real.

### Task 11 — Consolidar documentação e verificação final

**Descrição:** atualizar arquitetura, tarefas e documentação operacional somente após o estado
implementado ficar verde.

**Critérios de aceitação:**

- consolidado registra novos packages, portas, adapters, contratos e limitações;
- ADR aprovado possui status correto e ADR substituído permanece no histórico;
- nenhuma documentação afirma durabilidade ou prontidão produtiva inexistente.

**Verificação:** suíte completa, checkpoint SonarQube, `git diff --check` e revisão do diff.

**Dependências:** C3.

**Arquivos prováveis:** consolidado arquitetural, ADRs e esta pasta de tasks; sem derivados.

**Escopo estimado:** médio, até 5 arquivos.

## Checkpoints humanos obrigatórios antes de produção

| Checkpoint | Decisão necessária |
|---|---|
| C0 — arquitetura | novo package/orquestrador, ownership do processor, ADR sucessor, extensão e autenticação |
| C0 — contrato | path, verbo, `202`, requests/responses, mensagens v1, validação e OpenAPI |
| C0 — segurança | autorização REST, SAS `Send+Listen`, segredo externo, mock fora de produção e EULAs |
| C0 — observabilidade | nomes/kinds de spans, atributos, eventos de log e propagação |
| GO | autorização explícita antes de `pom.xml`, `src/`, testes ou configuração executável |
| C1 | aceitar ou bloquear a matriz efetiva de dependências |
| C2 | aceitar a primeira fatia POST -> fila de entrada |
| C3 | aceitar o fluxo end-to-end e suas limitações |
| CF | aceitar o encerramento da feature |

## SonarQube

- a revisão documental anterior não executou análise; a retomada de implementação foi autorizada
  pelo usuário em 2026-09-06;
- a sessão não conservava o baseline anterior. Antes desta migração/configuração, foi inicializada
  uma nova referência local sobre o workspace preservado, sem remover alterações: 660 testes,
  217 issues, cobertura 85,8% e duplicação 3,9%;
- essa referência já inclui a política pura da retomada e não substitui a evidência histórica do
  item 3.1 (213 issues). As quatro issues adicionais foram identificadas como `java:S5778` nas
  lambdas do teste da política; a revisão ajustou as lambdas sem alterar comportamento;
- depois de cada incremento coerente que altere o fingerprint: executar o checkpoint;
- `NON_COMPLIANT` exige evidência e decisão humana conforme `AGENTS.md`.
- primeiro checkpoint da migração/configuração: 214 issues, 1 nova CRITICAL/HIGH (`java:S8911`),
  cobertura 86,3%, duplicação 3,9%; os quatro `java:S5778` anteriores foram resolvidos;
- após decisão humana `ContinuarAjustes` e ajuste da inicialização CDI: 213 issues, 0 novas,
  0 HIGH/BLOCKER/CRITICAL, cobertura 86,3% e duplicação 3,9%; situação `COMPLIANT`, sem nova decisão
  Sonar pendente. Evidências completas e referência da análise estão no checklist.

## Riscos e controles

| Risco | Impacto | Controle |
|---|---|---|
| pedido contradiz ADR-0009 aceito | alto | ADR sucessor Proposto e checkpoint antes de dependência/código |
| extensão 1.2.5 não testada com Quarkus 3.33/JDK 25 | alto | spike inicial; sem downgrade/override automático |
| versão `preview` da extensão | alto | isolar na borda, testes de bootstrap/runtime e registrar suporte limitado |
| emulador não suporta WebSockets | alto | AMQP/TCP em dev/test; WebSockets/443 apenas no Azure |
| EULA e Docker obrigatórios no Dev Services | alto | aprovação explícita, imagens fixadas e diagnóstico de indisponibilidade |
| uma connection string para papéis combinados | médio | política dedicada `Send+Listen`, sem `Manage`, segredo externo e futura separação |
| mock de pré-validação usado indevidamente | alto | ativação explícita, origem observável e bloqueio de fallback produtivo |
| sem Outbox/estado durável | alto | classificar como demonstrador e separar feature de persistência |
| redelivery duplica saída/log | médio | IDs determinísticos, duplicate detection e limitação documentada |
| fixture atual retorna `Rascunho` | médio | delays curtos no teste e cenário de quarentena; não alterar semântica MTR silenciosamente |
| contexto de trace se perde na fila | médio | teste ponta a ponta e propagação manual somente se necessária |
| builder compartilhado mutado por múltiplos beans | médio | fábrica CDI única e inicialização centralizada |
| listeners mantêm event loop bloqueado | alto | SDK e adaptação Reactor nas bordas Service Bus; Quarkus/Mutiny permitidos no núcleo; não bloquear event loop com `block/await/sleep` |
| referências ao package anterior sobreviverem à migração | médio | cinco arquivos migrados, regressão aprovada e busca Java sem `doctree`/`monitoramentomtr`; manter esse controle nas demais subfatias |
| regras ArchUnit selecionam apenas domínios do Hub em parte dos checks | alto | ampliar seletores e provas negativas para os dois novos componentes nas respectivas fatias |
| orquestrador precisa dos parâmetros iniciais de uma política que pertence ao monitoramento | médio | porta/ACL aprovada no ADR-0011; provar tradução e preservação dos parâmetros nas fatias de implementação |
| implementação da composição CDI divergir do ADR-0011 aceito | médio | fábrica em `arquitetura.infraestrutura.servicebus`, acesso somente pelos adapters Service Bus; verificar injeção/lifecycle em 6.1, sem acoplar núcleos ou compartilhar contratos |
| infraestrutura transversal virar destino de todo código assíncrono | alto | manter regras em seus componentes mesmo com Quarkus reativo no domínio; limitar infraestrutura à composição técnica e preservar `hub.arquitetura` no Hub |
| nova sessão perder novamente o baseline | médio | nova referência local registrada sobre o workspace preservado; consultar checklist e estado da sessão, sem remover alterações nem inferir conformidade histórica |

## Dependências

- decisão humana sobre o ADR sucessor e os quatro checkpoints C0;
- GO humano explícito;
- fonte de baseline SonarQube escolhida depois do GO;
- JDK 25 e Docker disponíveis;
- aceitação explícita das licenças do emulador e SQL;
- para prova Azure posterior: namespace, filas e connection string fornecidos por canal seguro,
  nunca pelo chat.

## Decisões registradas antes do GO

O checkpoint C0.1 foi aprovado em 2026-09-04: `br.gov.caixa.simtr.orquestrador` será responsável
pela entrada REST, publicação inicial e consumo do resultado;
`br.gov.caixa.simtr.doctree.monitoramentomtr` foi o package originalmente definido para o processor,
substituído na revisão humana de 2026-09-06 por `br.gov.caixa.simtr.monitoramento`. Ambos serão
componentes irmãos fora de `hub`; o acesso ao dossiê ocorrerá somente pela porta pública e por ACL
local. O ADR-0010 foi aceito no item 1.2; o histórico de decisões permanece no checklist.

O checkpoint C0.2 foi aprovado em 2026-09-04: o path, o request, a validação, o `202 Accepted`
após confirmação do broker, o response, os contratos v1 independentes das filas, o erro REST
existente, a tolerância a campos JSON desconhecidos e o OpenAPI gerado pelo código foram aceitos.

O checkpoint C0.3 foi aprovado em 2026-09-04: connection string/SAS externa nos ambientes reais,
emulador em dev/test, ausência de Entra ID, permissões `Send + Listen` sem `Manage` ou
`RootManageSharedAccessKey`, segredo fora de código e telemetria, autorização vigente sem papel
novo, mock somente leitura explicitamente habilitado sem fallback produtivo, validação da
superfície de entrada e aceite explícito das EULAs do Service Bus Emulator e do SQL Server.

O checkpoint C0.4 foi aprovado em 2026-09-04: nomes e kinds dos spans, eventos de log estruturado,
atributos permitidos, proibições de dados sensíveis, propagação W3C e caracterização da
instrumentação efetiva do SDK antes de adicionar spans manuais foram aceitos.

O item E0.1 foi aprovado em 2026-09-04: o projeto permanecerá no Quarkus `3.33.2.1` nesta feature,
e o upgrade para `3.33.3.2` continuará fora do escopo.

Foi decidido em 2026-09-04 usar connection string externa nos ambientes reais e emulador em
dev/test, sem Microsoft Entra ID nesta feature. Essa decisão resolve a escolha do mecanismo de
autenticação, mas não implica aceite das EULAs nem conclui os demais itens de segurança.

## GO registrado

O usuário registrou GO em 2026-09-04 após aprovar os checkpoints de arquitetura, contrato,
segurança, observabilidade e escopo de plataforma. A primeira alteração de produção, teste, build
ou configuração executável continua condicionada à escolha e inicialização do baseline SonarQube
prevista nos itens B0.1 e B0.2 do `todo.md`.

## Retomada funcional de 4.1 — DTO/mapper de reagendamento (2026-09-07)

O usuário solicitou "vamos continuar agora com o plano", após a entrega estrutural C4.4 e
seu complemento Javadoc. Retomar somente a próxima subfatia pendente, sob os GOs existentes
de contratos e logs C4.2/C4.3. Resultado e guardrails restantes continuam pendentes; 5.1 não iniciado.

**Escopo:** substituir os dois esqueletos da saída do monitoramento por DTO v1 próprio e mapper
de JSON/AMQP, com validação equivalente à entrada. Receber a tentativa já decidida pelo domínio:
não incrementar contador, recalcular prazo/versão, criar IDs, publicar ou agendar. Reutilizar
somente o modelo semântico do próprio monitoramento e o marcador técnico do ADR-0012.

**Erro na borda:** completar a aplicação do padrão já aprovado em C4.2 ao reagendamento.
Evento local `monitoramento.servicebus.reagendamento.falhou`; códigos estáveis
`MONITORAMENTO_REAGENDAMENTO_CONTRATO_INVALIDO` e
`MONITORAMENTO_REAGENDAMENTO_SERIALIZACAO_FALHOU`, distinguindo validação de serialização.
DTO/helper próprios, nível ERROR, array JSON real, recurso da fila de entrada, id/código
propagados em `MapeamentoReagendamentoException extends RuntimeException`. Diagnóstico de
serialização contém apenas tipo/frames; validação não inventa stack técnico nem expõe valores.
Um registro por falha reconhecida. Erro inesperado não é capturado/reclassificado genericamente.

**Arquivos prováveis:** DTO/mapper existentes, helper de log, DTO de erro e exceção próprios
(cinco arquivos de produção); testes de contrato/compatibilidade/log e atualização pontual do
inventário de esqueletos. Atualizar Javadoc, guia, manifesto e arquitetura conforme o estado real.

**Critérios e verificação:** RED antes do GREEN; ObjectMapper/Validator reais no Quarkus;
JSON v1 exato e envelope determinístico; compatibilidade com consumidor independente;
limites inclusivos MTR e contador, obrigatoriedade e coerência temporal; erro JSON observado no
formatter efetivo, diagnóstico útil sem payload/causas e correlação válida; regressão focada
incluindo ArchUnit/CDI; checkpoint completo único do incremento com baseline original.

**Início seguro:** baseline integralmente idêntico à referência recuperada, credencial disponível
somente em memória; último checkpoint COMPLIANT de 859 testes. Fingerprint atual
`8d680e16a458d99d9b3beb6323a03d3818f0f46e33e23a0a52d360d96d6f22a2` inclui o
complemento Javadoc posterior ao checkpoint. Não reinicializar o baseline. Hub, simuladores,
extensão e `src/main/azure/servicebus-emulator/config.json` permanecem preservados.

## Revisão documental solicitada — guia Service Bus e Javadoc (2026-09-07)

O usuário identificou explicitamente `doc/guias/guia-service-bus-amqp-dossie.md` como o guia
desalinhado. Esse arquivo ainda descrevia o recorte histórico do ADR-0009: alteração de
`br.gov.caixa.simtr.dossie`, SDK direto, Entra ID, JSON de dois campos e monitor fora do guia.
A decisão vigente é ADR-0010/0011: orquestrador publica na entrada; monitoramento consome,
consulta/aplica critérios, reagenda na entrada ou publica resultado na saída; orquestrador
consome a saída e encerra o demonstrador registrando log. O package `dossie` e o Hub permanecem
inalterados. Autenticação por connection string/SAS, extensão Quarkus Azure Service Bus e
emulador via Dev Services já estão decididos; não reabrir essas escolhas.

**Prioridade atual:** pausar o GREEN do mapper de reagendamento e corrigir documentação e
Javadoc antes da continuidade funcional. Os três arquivos novos de testes de reagendamento
foram preservados em RED de compilação por API ainda ausente; a tentativa de criar a produção
foi interrompida e nenhum desses novos arquivos de produção foi gravado. Não apresentar o
workspace corrente como aprovado pelos 859 testes anteriores.

**Escopo documental autorizado:** reescrever o guia indicado conforme contrato/fluxo atuais;
alinhar guia de desenvolvimento, plano/checklist, manifesto, consolidado e ADRs somente onde
inconsistentes; contextualizar tasks históricas sem mudar suas decisões/encerramento.
Corrigir os onze Javadocs de portas com `undefined` e explicar etapas, filas, responsabilidades
e verificações nos tipos estruturais. Preservar todas as assinaturas, imports, anotações e corpos.

**Verificação:** links locais e referências Java; comparação do conteúdo fora de Javadoc;
DocLint isolado em diretório temporário sem gerar HTML; auditoria de preservação de arquivos.
Este recorte é exclusivamente documental: não inspecionar Sonar, credencial ou baseline, não
executar Maven nem checkpoint. O RED anterior permanece registrado, sem nova execução.
Resultado/reagendamento/guardrails de 4.1 continuam pendentes e 5.1 não será iniciado.

## Evidências da revisão documental do guia Service Bus (2026-09-07)

- Guia indicado reescrito para as duas filas e os componentes `orquestrador`/`monitoramento`;
  critérios de no-op, limites/quarentena, situação conclusiva e reagendamento descritos na ordem
  do plano. Connection string/SAS, extensão e Dev Services explícitos; `dossie`/Hub preservados.
- Guia de desenvolvimento, manifesto, consolidado, ADRs 0009/0010/0011 e índice alinhados.
  ADR-0012 conferido e preservado. Tasks do guia antigo receberam somente contexto histórico,
  sem mudar decisões ou encerramento. Documento amplo de origem preservado.
- Vinte tipos Java revisados exclusivamente em Javadoc, incluindo onze métodos de portas:
  removidos os onze `undefined`, com comportamento esperado, parâmetros/retorno, etapas e testes.
- Comparação antes/depois confirmou conteúdo idêntico fora de Javadoc nos vinte arquivos.
  DocLint isolado: exit 0, nenhum erro; nove avisos de construtores implícitos nas classes vazias,
  preservados para não criar código artificial. Saída apenas temporária, sem HTML.
- Doze documentos e 213 links locais conferidos, nenhum destino ausente. Nenhum arquivo original
  removido. Auditoria dos fontes apontou somente os vinte Javadocs alterados; código/configuração,
  `dossie`, Hub, simuladores, extensão e arquivo do emulador mantidos.
- `git diff --check` passou; apenas avisos preexistentes de LF/CRLF, sem normalização.
  Nenhum Maven, baseline, credencial, API ou checkpoint Sonar executado neste recorte documental.
- Próxima pendência funcional permanece em 4.1: completar o GREEN do DTO/mapper de reagendamento
  e seus logs/testes. Os três arquivos novos de teste do RED anterior foram preservados; a API
  esperada está ausente e não há novo resultado verde. Depois restam contratos de resultado e
  guardrails. Nenhum commit, mudança de branch ou avanço para 5.1.

## Continuidade de 4.1 — implementação e cobertura de resultado (2026-09-08)

Direção humana: "vamos fazer uma implementação vamos focar na implemntação e na cobertura
sem seguir exatamente o TDD". Para esta continuidade, implementar e verificar comportamento,
regressão e cobertura sem exigir a sequência RED anterior à produção. O histórico TDD das
subfatias concluídas permanece preservado.

Escopo: modelos próprios de resultado, DTOs/mappers produtor e consumidor independentes, JSON
v1 e envelope aprovados, logs locais tipados com diagnóstico seguro, testes reais de contrato,
cobertura e compatibilidade. Completar os guardrails previstos de isolamento por borda e acesso
das ACLs à API pública. Permanecer em 4.1, sem listeners, publicação, settlement ou 5.1.
Baseline original READY e checkpoint de 900 testes COMPLIANT conferidos; sem reinicialização.

Implementar em subfatias locais: modelos/DTOs; mapper/erro do produtor; mapper/erro do consumidor;
testes de contrato e logs; guardrails e regressão; checkpoint completo único do incremento.
A compatibilidade das duas bordas constitui a unidade de verificação; a orientação atual de
foco na implementação/cobertura substitui a ordem rígida de RED e o limite histórico de cinco
arquivos, sem autorizar compartilhamento de DTOs, mudança transversal ou ampliação funcional.

Contrato a confirmar: o fluxo aprovado verifica limites antes de consultar MTR; o texto v1 não
define nulidade de situacaoMtr nem contador zero na quarentena anterior à primeira consulta.
Proposta submetida: permitir null/zero nesse caso e exigir situação conclusiva e pelo menos
uma tentativa no resultado CONCLUSIVO. Não aplicar essa validação antes da resposta humana.
Os demais campos/tipos e propriedades seguem o contrato aprovado; motivo textual é preservado.
inputSequenceNumber conserva o long recebido, sem interpretar seus bits ou usá-lo como contador.
concluidoEm pode coincidir com iniciadoEm, mas não precedê-lo.

Arquivos: os dois modelos e quatro DTOs/mappers estruturais existentes; helpers, DTOs de erro e
exceções próprios de cada borda; testes espelhados, inventário de esqueletos e guardrails com
fixtures negativas/positivas. Atualização documental restrita ao estado e às evidências.

Verificações: ObjectMapper e Validator reais no Quarkus; JSON exato e envelope determinístico;
campos ausentes/nulos/tipos inválidos/overflow/datas; preservação de IDs e zeros; compatibilidade,
campos desconhecidos e rejeição de conteúdo extra; um erro sanitizado por falha reconhecida,
id/código propagados, trace válido e erro inesperado não reclassificado. Regra nova de arquitetura
deve rejeitar fixtures inválidas e aceitar as portas/modelos públicos e o framework já permitido.

## Ajuste Sonar do resultado — autorizado em 2026-09-08

O incremento de resultado e guardrails passou em 317 testes focados e no checkpoint completo
com 1021 testes em 171 classes, sem falhas, erros ou ignorados. Cobertura 87,0%, duplicação 4,4%.
O checkpoint de 2026-09-08T10:10:55.4993743-03:00 ficou NON_COMPLIANT: 215 issues abertas,
duas novas CRITICAL/HIGH de java:S1192 no mapper consumidor de resultado. São os literais
"paraResultado" (três ocorrências) e "validarTipos" (cinco ocorrências), sem falha funcional.
Issues: 1dfde59a-ce66-490f-97d7-7f227d974d46 e 1f4b5927-0402-44d5-b3fd-283dd75c5b7e.
Análise: ddc71af5-f271-48a7-8512-2ee6f7c61c7e; CE: c3b09788-5b67-4f89-b732-fa1d871fff40.
Fingerprint: f78cac3cee1c3f3ca65b857e9be8be403d14a3733809b2eed42b7facd267e975.

Direção humana: "vamos resolver os problemas sonar". Decisão ContinuarAjustes registrada
pelo script do checkpoint. Escopo do ajuste: extrair duas constantes no mapper consumidor,
preservar os textos emitidos e os testes existentes, executar regressão focada e novo checkpoint
com o baseline original. Sem aceitação excepcional, reinicialização de baseline ou avanço para 5.1.
A definição de nulidade/contador da quarentena anterior à primeira consulta MTR continua
aguardando resposta à pergunta de contrato; esta autorização trata das duas issues Sonar.

## Resultado e guardrails — evidência final do ajuste Sonar (2026-09-08)

- Modelos/DTOs independentes e mappers das duas bordas de resultado implementados, com JSON
  v1, envelope determinístico, validação estrutural/temporal e erros locais tipados e sanitizados.
  Sem envio, listener, settlement ou persistência. Validação especial da quarentena pendente.
- 98 testes de contrato de resultado, 11 de logs reais e 18 de fronteiras ArchUnit aprovados.
  Guardrails exercitam DTOs por borda, domínio/portas e acesso das ACLs à API pública, com
  fixtures positivas e negativas. Seis tipos implementados retirados do inventário: restam 19.
- Regressão após S1192: 317 testes em 11 classes, zero falhas/erros/ignorados.
  Comando: `mvn -q "-Dtest=ResultadoMonitoramentoContratoTest,ResultadoServiceBusLogTest,FronteirasMonitoramentoArchUnitTest,EstruturaMonitoramentoArchUnitTest,EsqueletosMonitoramentoCdiTest,ArchUnitProgressivoTest,MonitoramentoReagendamentoContratoTest,MonitoramentoReagendamentoLogTest,MonitoramentoEntradaContratoTest,MonitoramentoEntradaLogTest,OrquestradorEntradaLogTest" test`.
- O ajuste de produção extraiu somente OPERACAO_MAPEAMENTO e OPERACAO_VALIDACAO_TIPOS no
  mapper consumidor de resultado. Textos dos logs e comportamento preservados; comparação
  reversa da extração idêntica ao arquivo anterior. Nenhum teste foi alterado para esse ajuste.
- `./validar-checkpoint-sonarqube.ps1` concluiu build, scanner e Compute Engine em
  `2026-09-08T10:30:53.8267228-03:00`: **1021 testes em 171 classes**, sem falhas/erros/ignorados.
  **COMPLIANT**, decisão técnica `NOT_REQUIRED`, 213 issues (baseline 217), nenhuma nova
  ou HIGH/BLOCKER/CRITICAL, cobertura **87,0%**, duplicação **4,4%**.
- API local confirmou ambas as issues S1192 como CLOSED/FIXED, sem supressão ou exceção.
  CE: `22a6d6fe-92da-4f60-87cf-c1a4f2807310`; análise: `c86b89ae-ac8d-49d2-b540-439d06446b38`.
  Fingerprint: `13df928607bf0c276fecfbba4395ada0195fbc41f0ecf06c47fd6e4095f389d9`, idêntico ao código atual.
- JaCoCo: 157/157 linhas cobertas nos dois mappers, dois helpers de log e duas exceções de
  resultado. Essa cobertura não conclui a definição contratual de quarentena ainda pendente.
- Baseline comparado integralmente com a referência original, sem reinicialização.
  Auditoria SHA-256 do incremento de resultado: 598 arquivos preexistentes preservados,
  seis tipos de produção e o inventário alterados, seis novos arquivos de produção e nove
  de testes/fixtures, nenhum removido. Inclui preservação de Hub, dossiê, reagendamento,
  configurações, pom.xml e AGENTS.md no incremento.
- Revisão final: correção, simplicidade, arquitetura, segurança, desempenho, testes e escopo
  conferidos; extração local sem nova dependência ou efeito externo. Documentação de continuidade
  alinhada. Nenhum staging, commit, avanço para 5.1 ou encerramento humano da feature.


## Acompanhamento de duplicação — 2026-09-08

O usuário perguntou se a duplicação exige preocupação. O checkpoint atual mede 4,4%, abaixo
do limite de 5%, com margem de 0,6 ponto percentual; o incremento anterior media 3,6%.
Consulta aos blocos reais do Sonar local: os dois LogErroResultado têm 62 e 61 linhas marcadas
como duplicadas, incluindo montagem/emissão do JSON e diagnóstico; as duas
MapeamentoResultadoException têm 22 linhas cada. Há também blocos nos helpers anteriores.
Os DTOs novos não aparecem entre os arquivos com duplicação positiva nessa consulta.

Risco registrado: repetição técnica pode aumentar custo de manutenção e consumir a margem.
Priorizar avaliação da montagem/emissão técnica dos logs, preservando DTOs, exceções,
classificação e sanitização locais conforme ADR-0012. Uma extração transversal exige proposta
concreta e checkpoint arquitetural antes da implementação. Esta avaliação não alterou código,
exclusões Sonar, limiar, baseline ou Hub e não representa autorização para mudar a arquitetura.

## Preparação da continuidade de 4.1 — 2026-09-08

Usuário: "ok prosseguimos". Conferidos o estado atual, fluxo aprovado, DTOs/mappers, teste de
contrato, logs e limites do ADR-0012. A proposta de nulidade/contador da quarentena foi apresentada
novamente para resposta; não houve decisão inferida. A [matriz de validação preparada](preparacao-validacao-resultado.md)
identifica os casos e arquivos do próximo incremento. A avaliação da duplicação confirmou que
centralizar emissão exige proposta e checkpoint arquitetural; não houve extração transversal.

Neste recorte preparatório foram alterados somente documentos da feature. Código, testes,
baseline e último checkpoint permanecem preservados; sem Maven, API Sonar, commit ou 5.1.

## Validação final de resultado — confirmação humana em 2026-09-08

Usuário respondeu "confirmo" à proposta: QUARENTENA com situacaoMtr=null e zero tentativas
quando ainda não houve consulta; CONCLUSIVO com situação conclusiva e pelo menos uma tentativa.
Essa confirmação autoriza a validação local nas duas bordas de resultado em 4.1.

Implementação: manter DTOs independentes, rejeitar contador negativo e validar que CONCLUSIVO
possui tentativasRealizadas >= 1 e situacaoMtr em CONFORME, NAO_CONFORME ou PENDENTE_INFORMACAO.
QUARENTENA aceita null/zero e preserva o contador não negativo recebido; ausência de situação
não permite reconstruir consultas anteriores. Não recalcular situações, motivo ou contador.
A validação auxiliar não deve acrescentar propriedade ao JSON v1.

Verificações: produtor e consumidor reais, JSON exato com situação nula na quarentena,
rejeição de conclusivo sem situação/consulta, três situações conclusivas no limite mínimo,
contador negativo e compatibilidade dos limites numéricos; log real sanitizado da nova rejeição,
regressão e checkpoint completo. Preservar formato de erros, configurações, Hub e baseline.
A implementação segue o foco autorizado em cobertura sem ordem rígida de TDD.
O item 4.1 será marcado tecnicamente concluído somente após essas verificações; 5.1 não iniciado.
Duplicação permanece acompanhada; esta confirmação não altera a responsabilidade de logging.

## Conclusão técnica de 4.1 — validação de resultado em 2026-09-08

- Direção humana: "confirmo" à regra apresentada de quarentena null/zero antes de consulta
  e CONCLUSIVO com situação conclusiva e pelo menos uma tentativa. Confirmação registrada
  antes da produção; não foi inferida da mensagem anterior "ok prosseguimos".
- Os dois DTOs próprios agora rejeitam contador negativo e validam a evidência de consulta
  em CONCLUSIVO: CONFORME, NAO_CONFORME ou PENDENTE_INFORMACAO e contador >= 1.
  QUARENTENA preserva situação MTR nula e contador não negativo recebido; ausência de estado
  não é preenchida com consulta, valor inventado ou contador da sequência técnica.
- Jakarta Validation executa a condição local; JsonIgnore impede propriedade auxiliar no JSON.
  Mappers, helpers de log, exceções e contratos de outras bordas permaneceram idênticos.
  Não houve extração transversal ou mudança de logging para reduzir artificialmente duplicação.
- 25 casos novos: quarentena null/zero e limites, ausência de situação no consumidor, contador
  negativo nas duas bordas, conclusivo sem situação/consulta, tipos inválidos e logs reais.
  Os três estados conclusivos foram verificados no mínimo de uma tentativa.
  ResultadoMonitoramentoContratoTest: 120 casos; ResultadoServiceBusLogTest: 14 casos.
- Regressão focada: **342 testes em 11 classes**, zero falhas/erros/ignorados. Comando:
  `mvn -q "-Dtest=ResultadoMonitoramentoContratoTest,ResultadoServiceBusLogTest,FronteirasMonitoramentoArchUnitTest,EstruturaMonitoramentoArchUnitTest,EsqueletosMonitoramentoCdiTest,ArchUnitProgressivoTest,MonitoramentoReagendamentoContratoTest,MonitoramentoReagendamentoLogTest,MonitoramentoEntradaContratoTest,MonitoramentoEntradaLogTest,OrquestradorEntradaLogTest" test`.
- `./validar-checkpoint-sonarqube.ps1` concluiu build, scanner e Compute Engine em
  `2026-09-08T11:12:19.3296159-03:00`: **1046 testes em 171 classes**, sem falhas, erros ou ignorados.
  **COMPLIANT**, decisão `NOT_REQUIRED`, 213 issues (baseline 217), nenhuma nova/severa,
  cobertura **87,0%**, duplicação **4,4%**. A duplicação não aumentou.
  CE `c189234e-e9d0-4a30-a4e1-38f845c48484`; análise `f3b6720c-0b18-4713-8a16-22a030278153`.
  Fingerprint `13c39b71061152a7498dd7c83022afee3342e8eea8fe4f8fa6bbbe936bcf86fe`, conferido com o código atual.
- JaCoCo da nova validação: 4/4 linhas e 6/6 condições em cada DTO, sem trechos descobertos.
  Comparação de SHA-256: somente dois DTOs e três testes/fixture alterados; 615 arquivos
  preexistentes preservados, nenhum adicionado ou removido em src/. Baseline integralmente
  idêntico à referência original; token permaneceu somente em memória.
- Revisão de correção, simplicidade, arquitetura, segurança, desempenho, testes e escopo:
  condição pura e local, sem nova dependência, dados transportados ou operação externa;
  JSON exato, diagnóstico sanitizado e fronteiras arquiteturais preservados.
- Plano/checklist, retomada, guias, manifesto, preparação e consolidado alinhados.
  **4.1 está tecnicamente concluído**, com seus contratos e guardrails. 5.1 permanece não iniciado;
  nenhum staging, commit ou encerramento humano da feature.

Referências de validação consultadas e confirmadas no runtime efetivo:
[Quarkus Validation](https://quarkus.io/guides/validation/) e
[Jakarta AssertTrue](https://jakarta.ee/specifications/bean-validation/3.1/apidocs/jakarta/validation/constraints/asserttrue).


## Retomada autorizada do item 5.1 — 2026-09-08

O usuário declarou “vamos assim proseguir a implementação”, após confirmar a publicação de
`266092f`. A retomada passa ao próximo item pendente, 5.1; 4.1 permanece tecnicamente concluído.
Não se registra encerramento humano da feature nem avanço para 6.1.

A inspeção confirmou as portas, os modelos/esqueletos e a consulta pública do Hub. Foi
identificada uma divergência a resolver: a situação do Hub é id/nome (fixture 1 / Rascunho),
enquanto os critérios de monitoramento usam códigos conclusivos; não há tabela local validada.
Também continuam em aberto os campos mínimos, ativação concreta do mock e ausência/falha,
conforme já registrado no roteiro publicado.

A [proposta C5.1](preparacao-consultas-5-1.md) apresenta modelos mínimos, configuração false
por padrão, cenários, falhas, sequência de subfatias e critérios de cobertura para decisão
específica. Baseline original comparado e preservado; nenhuma alteração executável, teste ou
análise Sonar nesta preparação. C5.1 permanece PENDENTE, sem inferir aprovação da retomada.

## Complemento do roteiro para continuidade por desenvolvedores — 2026-09-08

Na conferência solicitada pelo usuário após a publicação de `84fca5c`, foram encontrados
trechos desatualizados no guia de desenvolvimento: resultado e guardrails de 4.1 ainda
apareciam como pendentes, e a nota final de uma revisão antiga podia ser lida como RED atual.
Risco: orientar outro desenvolvedor a refazer entregas já concluídas.

Escopo documental: corrigir essas referências, identificar notas históricas e acrescentar
ao guia principal o ponto de partida de 5.1, com arquivos, sequência e verificação.
Conferir portas, Javadocs, validação de resultado e testes ArchUnit existentes; revisar
links e diff. Não implementar 5.1 nem alterar contratos, ADRs ou arquivos executáveis.
Não executar Maven, Sonar ou baseline neste complemento exclusivamente Markdown.
A publicação segue a autorização de commit e push da revisão dos guias na mesma branch.

## Preparação de commit e push para revisão com desenvolvedores — 2026-09-08

O usuário autorizou organizar commit e push na branch atual, depois de revisar o guia para
explicitar arquitetura, decisões adotadas e o que está pronto ou pendente. A revisão altera
somente Markdown; o código permanece no estado verificado por 1046 testes e checkpoint
COMPLIANT (87,0% de cobertura e 4,4% de duplicação). Nenhum novo incremento funcional.

Escopo: guia Service Bus como leitura principal, guia de desenvolvimento como inventário,
manifesto do pacote e notas de continuidade. Explicar no próprio guia as decisões aceitas dos
ADRs 0001/0003/0004/0006/0010/0011/0012 e as definições de contrato/política das tasks, além
de manter seus links. Corrigir referências desatualizadas à fila de saída ainda pendente.

Revisar e incluir os fontes/configurações/testes existentes da feature, ADRs, documentação e
tasks relacionadas. A orientação Azure de AGENTS.md e o documento de origem serão incluídos
após revisão: o primeiro orienta a continuidade e o segundo é destino de links dos guias.
O patch temporário .codex-doc-alignment.patch permanece local. Não publicar estado de sessão,
credenciais, relatórios Sonar, saídas de build ou formatos derivados. Fazer push normal para
origin na mesma branch, sem force, merge ou encerramento da feature.

## Revisão do guia e pacote para desenvolvedores — 2026-09-08

O guia principal passou a abrir com o que está pronto e explicitar arquitetura do mesmo runtime,
responsabilidade de cada componente, hexagonal pragmática, contratos independentes, ACLs, factory,
SAS/Dev Services, logging tipado e telemetria planejada. A seção desatualizada da fila de saída
foi substituída pelo contrato implementado, JSON v1 e regras confirmadas. Adicionado roteiro 5.1–CF.

Os nomes de telemetria planejados com prefixo doctree foram preservados como contrato histórico,
com nota explícita de que a revisão dos packages não os renomeou. Essa diferença está documentada
para caracterização/checkpoint da Task 10; nenhum sinal ou código foi alterado nesta revisão.

Guia de desenvolvimento e manifesto foram alinhados. A seleção agora inclui AGENTS.md e o
documento amplo de origem após revisão, preservando as referências para os devs. O patch
temporário continua local. O usuário já autorizou commit e push nesta branch; não repetir
pedido de autorização para essas duas operações.

## Verificação do pacote antes do commit — 2026-09-08

Revisados escopo, comportamento já entregue, fronteiras, diagnóstico seguro, configuração,
testes e estruturas futuras. Os 620 arquivos conferidos de src/, pom.xml e AGENTS.md ficaram
idênticos durante esta revisão documental; a evidência executável anterior permanece aplicável.

Verificados 17 documentos e 146 referências locais, sem destino ausente; os dois exemplos JSON
do guia são sintaticamente válidos e os dois diagramas distinguem arquitetura e fluxo planejado.
Conferidos os valores contra DTOs, política/configuração, factory inativa e infraestrutura de logs.
A seleção contém 125 arquivos; a busca por formatos de credenciais privadas/Sonar/GitHub/SAS
não encontrou candidatos, sem expor valores. O diff documental passou após ajuste de linha final.

A branch remota ainda não existia na consulta de origem. Publicação autorizada: commit único
do estado acumulado até 4.1 e push com upstream para a mesma branch. Nenhum merge ou force.
O único arquivo de trabalho preservado fora do pacote é .codex-doc-alignment.patch.

## GO C5.1 e divisão executável — 2026-09-08

O usuário respondeu “go” à proposta C5.1. Registrar antes da primeira alteração de produção.
Preservar a arquitetura e o contrato aprovados; implementar somente 5.1.

- 5.1a: modelo PreValidacaoConsultada, DTO/mapper próprios do simulador, teste de contrato
  e retirada do modelo do inventário de inatividade (cinco arquivos).
- 5.1b: PreValidacaoSimuladaAdapter, teste unitário, nova property false por padrão,
  Javadoc da porta ConsultarPreValidacao e inventário (cinco arquivos).
- 5.1c: modelo SituacaoDossieConsultada, SituacaoDossieHubAcl, teste unitário,
  Javadoc de ConsultarSituacaoDossie e inventário (cinco arquivos).
- 5.1d: testes CDI com mock desabilitado/habilitado e Hub real/simulador, inclusão da
  borda própria do simulador na regra de isolamento de DTOs, regressão e checkpoint.

Escolhas locais de implementação: IllegalArgumentException para entrada/valor inválido;
IllegalStateException para simulador desabilitado/resposta do Hub ausente; NoSuchElementException
para pré-validação não cadastrada. Mensagens locais fixas sem dados rejeitados. Falhas emitidas
pela porta do Hub permanecem no Uni, sem interpretação, retry ou log novo. Não introduzir tipos
de erro compartilhados, helpers de logging ou normalização de nomes para esta consulta.
Modelo do Hub preserva id inclusive nulo e nome original não vazio; não inventa id.

Verificação: testes de comportamento por subfatia; CDI/ArchUnit e checkpoint Sonar único do
incremento coerente 5.1. Contratos das filas, classificação terminal e settlement ficam preservados.

## Cobertura mensurada de 5.1 — ajuste dos testes em 2026-09-08

Primeiro checkpoint completo de 5.1: 1113 testes/176 classes, zero falhas; COMPLIANT,
86,9% de cobertura, 4,4% de duplicação, 213 issues, nenhuma nova/severa.
Análise `1b45d25f-3fdd-4460-9456-1c3ad3df0eb1`, em `2026-09-08T15:04:13.1141977-03:00`.

A revisão JaCoCo revelou caminhos de erro não contabilizados nas seis classes de consulta,
apesar de exercitados pelos 63 testes de mapper/adapter/ACL. O pom possui quarkus-jacoco,
sem agente JaCoCo para JUnit fora de QuarkusTest. A documentação oficial confirma o limite:
https://quarkus.io/guides/tests-with-coverage/#coverage-for-tests-not-using-quarkustest

Ajuste dentro da cobertura autorizada: executar essas três classes com @QuarkusTest, mantendo
cenários, doubles da porta pública e asserções. Usar o classloader instrumentado já adotado
no projeto; não alterar pom, dependências, exclusões de cobertura ou produção. Verificar os
mesmos casos e as linhas/condições efetivamente medidas, depois concluir novo checkpoint.

A primeira tentativa operacional de checkpoint falhou no clean por log aberto em target;
o log foi movido para a pasta temporária. A execução seguinte encontrou uma expectativa nova
incorreta no teste de ausência: FalhaDossieProduto prioriza a primeira mensagem da lista.
Corrigida somente a asserção para a mensagem existente do simulador, conferida no código;
teste focado e a suíte de 1113 passaram. Nenhuma dessas tentativas alterou o baseline ou o Hub.

## Conclusão técnica de 5.1 — 2026-09-08

Consultas implementadas conforme C5.1, com modelos mínimos próprios, mock desabilitado por
padrão e ACL pela API pública do Hub. Subfatias, 70 casos novos e revisão descritos na
[evidência final do checklist](todo.md#51--implementação-e-verificação-final-em-2026-09-08).
Suíte: 1113 testes/176 classes; checkpoint COMPLIANT, cobertura 87,2%, duplicação 4,4%,
nenhuma issue nova ou HIGH/BLOCKER/CRITICAL. As seis classes têm 100% das linhas/condições.
Baseline preservado. Guias e consolidado atualizados; próximo item 6.1 ainda não iniciado.
As alterações de 5.1 permanecem sem novo commit/push.

## GO de 6.1 e separação dos testes — 2026-09-08

Usuário declarou “go” ao próximo item 6.1 e determinou testes unitários sem emulador,
com escolha local entre emulador e fila Azure. O [detalhamento de 6.1](continuidade-6-1.md)
registra essa alteração de escopo antes de produção/tooling, subfatias e critérios.
Implementar somente 6.1; manter 5.1 concluído e o baseline original preservado.

## Estado final de 6.1

Implementação e verificações concluídas, inclusive o ajuste autorizado de S1710.
Checkpoint COMPLIANT, 1155 testes padrão sem broker, cobertura 87,4% e duplicação 4,3%.
Quatro testes de integração explícita com emulador passaram separadamente.
Ver [evidência final](todo.md#61--conclusão-técnica-e-ajuste-sonar-em-2026-09-08).
C2 permanece pendente de decisão humana; 7.1 não foi iniciado.
Não houve novo commit/push. Baseline e capacidades existentes preservados.

## Pausa ao final de 2026-09-08

O usuário solicitou parar por hoje e retomar em 2026-09-09. Pausa registrada em
[retomada.md](retomada.md#pausa-segura-em-2026-09-08--retomar-em-2026-09-09), com instruções
para a próxima sessão. 6.1 concluído em GREEN e Sonar COMPLIANT; C2 pendente e 7.1 não iniciado.
Alterações de 5.1/6.1 preservadas na mesma branch, sem staging, novo commit ou push.
Comandos do agente concluídos; nenhum trabalho deixado em execução. Serviços preexistentes
preservados. A pausa não representa aceite de C2 nem encerramento humano da feature.

## Revisão C2 e correção proposta — 2026-09-09

Revisão registrada em [revisao-c2.md](revisao-c2.md), com achado P1 C2-R1.
A proteção do profile de integração não abrange connection string carregada de arquivo
por `quarkus.config.locations` ou outras fontes efetivas do Quarkus. A promessa documental
de rejeição de configuração externa excede o código: testes podem operar em filas Azure reais.
Não houve reprodução em Azure nem alteração do baseline. C2 segue sem aceite humano.

**Intenção/escopo proposto:** corrigir exclusivamente o isolamento do profile de integração,
garantindo conexão de origem controlada pelo emulador antes de qualquer envio, recebimento
ou settlement. Cobrir fontes de arquivo e ambiente/JVM sem ler, registrar ou usar segredo real.
Preferir solução na infraestrutura de testes, sem alterar a fábrica de produção.

**Fora de escopo:** 7.1, listeners, regras de negócio, contratos REST/JSON, dependências,
configuração de desenvolvimento, alteração dos dados locais, commit/push e novo baseline.
O dev mantém a escolha emulador/Azure; testes padrão continuam sem broker.

**Critérios de aceitação:** configuração externa efetiva é rejeitada antes de I/O; ausência
de configuração externa permite o emulador; erro não divulga valores; regressão negativa
usa valores sintéticos e nenhum broker; integração explícita preserva as quatro provas existentes.

**Verificações após GO:** reproduzir o caminho de configuração em regressão sem broker;
corrigir e confirmar a regressão; executar a integração explícita somente após garantir seu
isolamento; checkpoint único do incremento com a suíte padrão e o baseline original.
Registrar evidências e reapresentar C2, sem inferir aceitação.

**Arquivos prováveis:** `ServiceBusEmuladorTestProfile.java`, teste próprio do profile e,
se necessário para garantir a origem antes do runtime, recurso exclusivo de teste no mesmo
package. Atualizar os textos afetados da feature e do consolidado após a correção verificada.

**Risco de implementação:** validar configuração na fase errada pode rejeitar a conexão
gerada legitimamente pelo Dev Services ou ocorrer tarde demais. Provar a ordem antes do I/O;
não substituir por uma verificação posterior ao envio. Se exigir alteração de produção,
atualizar este recorte antes de prosseguir.

**Checkpoint C2-R1:** GO humano registrado abaixo pelo pedido explícito de correção.
O aceite de C2 permanece uma decisão humana separada.

## GO C2-R1 — 2026-09-09

O usuário solicitou explicitamente "corrigir C2-R1". Autorizado o recorte de isolamento
dos testes descrito na revisão/plano, com regressão sem broker e integração explícita após
a proteção. C2 permanece pendente de aceite humano; 7.1 não foi autorizado por esta decisão.
Token herdado disponível, sem exposição do valor. Fingerprint anterior à alteração:
37d69b34ee96e8dd3161397bb8e30c88e85ed3e8036669419b75358e7ad39fb2, igual ao final de 6.1.
O estado da sessão de 2026-09-09 estava sem baseline. Sua cópia foi preservada, e somente
a referência original de 217 issues e sua avaliação foram recuperadas do snapshot
session-restored-s7467-20260907.json; nenhuma análise/baseline novo foi inicializado.
A análise original permanece f6183a72-a2ea-44bc-9374-b2b064bdad55.
O checkpoint corrente será preenchido por execução real após o incremento, sem atribuir
à sessão a análise antiga contida no snapshot. Evidências de 6.1 permanecem no histórico.

## Correção C2-R1 — evidência final em 2026-09-09

- GO humano: pedido explícito "corrigir C2-R1", registrado antes da alteração executável.
- Alteração executável restrita a `ServiceBusEmuladorTestProfile.java` e ao novo
  `ServiceBusEmuladorTestProfileTest.java`, ambos na infraestrutura de testes.
- O profile de integração fixa `test` e reconstrói as fontes de configuração do Quarkus
  antes de cada bootstrap. Rejeita connection string/namespace efetivos, inclusive por arquivo
  ou `%test`, sem expandir expressões; falha de leitura produz mensagem fixa sem causa externa.
  A suíte padrão continua sem broker e a escolha emulador/Azure no desenvolvimento permanece igual.
- RED inicial: dois casos de configuração externa em arquivo falharam porque o profile
  antigo não lançava a rejeição. GREEN inicial: ambos passaram. A ampliação acrescentou
  RED para profile efetivo e falha de leitura; GREEN final: **11 casos**, sem Quarkus/broker.
- Regressão cobre connection string/namespace, arquivo comum e `%test`, expressão sem
  expansão, propriedades JVM, configuração ausente/inativa, releitura e erro de parsing.
  As propriedades JVM alteradas pelo teste são restauradas em `finally`.
- `mvn -q -Pservicebus-integration test`: **quatro testes/duas classes**, zero falhas,
  erros ou ignorados, concluídos em 2026-09-09 às 08:02:54 locais; emulador fornecido pelo
  Dev Services. SQL/emulador temporários encerrados; serviços preexistentes preservados.
- Limitação observada: SDK emitiu três registros de erro de settlement com link fechado
  durante o controle das mensagens pelos testes. O Surefire terminou sem falhas.
  Isso não valida settlement de listeners futuros; caracterizar nas etapas 7.1/8.1/C3.
  Nenhum listener ou controle de mensagens foi alterado no recorte C2-R1.
- `./validar-checkpoint-sonarqube.ps1` concluiu suíte padrão, build, scanner e Compute Engine:
  **1.166 testes em 182 classes**, zero falhas/erros/ignorados, sem classes de emulador.
- Sonar **COMPLIANT / NOT_REQUIRED**: cobertura **87,4%**, duplicação **4,3%**,
  213 issues, nenhuma nova ou HIGH/BLOCKER/CRITICAL.
  Data: `2026-09-09T11:11:54.9764280+00:00`; análise: `a6d671c0-5cb0-45ea-b404-6c95ebd16a5c`;
  CE: `85e9b139-9dab-47d9-9c0d-c1cc3883d1d4`;
  fingerprint: `d3b4a05d157224e099bee8320381e0e8708776161b87055a8aac79b50fa510eb`.
- Baseline original READY, 217 issues e análise `f6183a72-a2ea-44bc-9374-b2b064bdad55`
  conferidos integralmente iguais ao snapshot preservado após o checkpoint. Sem InitializeBaseline.
- Revisão independente sem achados adicionais; contexto de configuração reconstruído por
  chamada, sem ConfigProvider global, e proteção anterior à augmentation/início da aplicação.
  Fonte conferida nos artefatos Quarkus 3.33.2.1; versões e dependências preservadas.
- Contratos, fábrica de produção, Hub, escolha dev emulador/Azure e suíte padrão preservados.
  Guias/consolidado/tasks atualizados; sem formatos derivados, staging, commit ou push.
- **C2-R1 tecnicamente concluído. C2 permanece pendente de aceite humano; 7.1 não iniciado.**

## Aceite C2 e estimativa do trabalho restante — 2026-09-09

O usuário declarou "c2 aceito" e solicitou explicação do trabalho restante e estimativa
antes de prosseguir à 7.1. C2 aceito após a correção C2-R1; 7.1 permanece não iniciado.
Não há encerramento da feature nem decisão nova sobre as regras pendentes.

A infraestrutura, política, contratos/mappers, consultas e publicação REST → entrada estão
implementados. Faltam quatro etapas funcionais e a integração/consolidação final. A inspeção
confirmou que o caso de uso de processamento, listener da entrada e adapter de reagendamento
ainda são estruturas inativas; a política progressiva já possui lógica implementada.

| Etapa | Entrega restante | Estimativa de trabalho assistido |
|---|---|---|
| 7.1 | Consumir entrada, consultar fontes, decidir no-op/terminal/quarentena, publicar resultado e confirmar a entrada após o efeito | 3–5 h |
| 8.1 | Reagendar não conclusivos, provar agendamento + Complete na mesma transação, rollback e redelivery | 3–5 h |
| 9.1 | Consumir saída, registrar resultado estruturado e confirmar/tratar falhas | 1–2 h |
| 10.1 | Fechar propagação e spans do POST ao resultado, sem duplicação nem campos proibidos | 2–4 h |
| C3 e 11.1–CF | Integração ponta a ponta, suíte/Sonar finais, documentação, revisão e apresentação para aceite humano | 3–4 h |
| Total | Escopo atual da feature | 12–20 h |

Estimativa preliminar do agente, incluindo implementação, testes focados, checkpoints
coerentes e ajustes usuais, equivalente a cerca de 2–3 dias de 8 horas. Exclui espera por
decisões humanas ou indisponibilidade do ambiente; não representa prazo garantido.
Reestimar após 7.1 e a prova transacional de 8.1.

Dependências para detalhar 7.1: tabela oficial de id/nome do Hub para estados conclusivos
e tratamento de mensagem com versão de política diferente da selecionada. A coordenação
semântica de decisão e transação precisa ser detalhada nas respectivas subfatias.
Maiores incertezas: transação no SDK/emulador, erros de settlement observados na integração,
lifecycle dos listeners e instrumentação automática do SDK. Uma limitação dessas provas
pode exigir revisão da estimativa e do plano.

Esta atualização é exclusivamente documental: baseline e código preservados, sem execução
de Maven/Sonar, staging, commit ou push. Os registros anteriores de C2 pendente são históricos.

## Direção humana para situações do Hub — 2026-09-09

O usuário informou literalmente: "FINALIZADO_CONFORME -> CONFORME ,
FINALIZADO_INCONFORME -> INCONFORME, PENDENTE_INFORMACA - INCONFORME mantendo a situação do mtr".

Preservar a situação recebida no campo situacaoMtr e usar campo independente
situacaoPreValidacao para a classificação calculada. Não inventar IDs numéricos.
Foi solicitada confirmação dos códigos INCONFORME versus NAO_CONFORME (usado no
contrato/plano atual) e PENDENTE_INFORMACA versus PENDENTE_INFORMACAO. As respostas
ainda não foram recebidas neste registro; não normalizar silenciosamente esses nomes.

Divergência confirmada no código: isConsultaConclusivaValida dos dois DTOs de resultado
aceita apenas CONFORME, NAO_CONFORME e PENDENTE_INFORMACAO em situacaoMtr. Para preservar
os nomes originais informados, detalhar em 7.1 o ajuste da validação nas duas bordas,
com regressão de compatibilidade, sem substituir a situação MTR pela classificação.
Nenhum DTO ou outro código foi alterado nesta atualização documental. A decisão sobre
mensagens com versão antiga de política continua pendente.

## GO e execução de 7.1 — 2026-09-09

O usuário solicitou "7.1" após aceitar C2 e informar o mapeamento das situações do Hub.
Autorizada a execução do item 7.1; 8.1 e demais itens permanecem fora deste incremento.
Baseline original READY de 217 issues integralmente igual ao estado de C2; fingerprint
inicial d3b4a05d157224e099bee8320381e0e8708776161b87055a8aac79b50fa510eb.
Credencial herdada presente, sem exposição. Nenhum baseline novo.

Execução em subfatias coerentes:
1. Publisher da saída pela porta existente: mapper próprio, sender FilaSaida, confirmação
   assíncrona, falha segura e um envio por invocação compartilhada. Atualizar porta e
   inventário de inatividade junto com a implementação.
2. Decisão semântica e caso de uso terminal: ordem pré-validação/no-op, limites/quarentena,
   consulta Hub e situação conclusiva; preservar situação MTR, IDs, prazo e sequência.
   Detalhar a passagem de inputSequenceNumber sem SDK no núcleo.
3. Listener da entrada: PEEK_LOCK, concorrência limitada, publicação antes de Complete,
   Abandon/DeadLetter controlados, lifecycle das assinaturas antes da fábrica.
   Detalhar explicitamente o comportamento temporário do ramo não conclusivo antes de
   habilitar consumo, pois seu reagendamento pertence a 8.1.
4. Integração explícita com emulador, regressão padrão sem broker, revisão e checkpoint
   Sonar do incremento. Atualizar arquitetura/guia/tasks; sem commit/push.

A primeira subfatia independe das confirmações de códigos e da escolha de versão de política.
Pergunta enviada ao usuário sobre versão divergente: usar definição da versão recebida,
se configurada, com quarentena quando ausente, ou quarentena para toda divergência.
Nenhuma resposta foi inferida; implementar essa parte após a definição aplicável.

Critérios/verificações do publisher: teste sem Quarkus/broker em RED por API ausente;
GREEN mínimo; provar operação preguiçosa, confirmação compartilhada, falhas síncrona e
assíncrona sanitizadas, ausência de cliente, preservação da falha do mapper e invocações
independentes. Arquivos: publisher, porta, teste e inventário EstruturaPlanejada.

### Complemento de verificação de 7.1-A

Acrescentar prova CDI da porta de resultado sem broker e dois casos opt-in de publicação
real (conclusivo/quarentena) no profile protegido do emulador. Os resultados são construídos
pelo teste com o contrato atual: não implementam nem antecipam a classificação do Hub.
Na nova prova, Complete faz parte da cadeia de recebimento antes de next/cancelamento,
para verificar a publicação sem repetir a janela de link fechado observada em C2.
O teste não inicializa listener de produção nem valida o processamento terminal completo.

## Revisão e resultado parcial de 7.1-A

Ver [continuidade de 7.1](continuidade-7-1.md) para implementação, 145 focados,
seis integrações e 1.172 testes padrão. Checkpoint NON_COMPLIANT por java:S5778 no
teste CDI, com ajuste de uma chamada na lambda proposto e decisão humana pendente.
A configuração v1 já existe e foi explicada ao usuário; permanece pendente a regra
para versão divergente, não a criação da política configurada.

A revisão técnica recomenda não habilitar consumo geral antes de 8.1: mensagens não
conclusivas não podem ser descartadas nem entrar em Abandon repetido como substituto do
agendamento. Proposta de gate e desenho mínimo do listener registrados na continuidade;
nenhum deles foi aplicado ou tratado como decisão humana nesta subfatia.

## Fechamento técnico de 7.1-A após ContinuarAjustes — 2026-09-09

O usuário escolheu ContinuarAjustes; decisão registrada por
./validar-checkpoint-sonarqube.ps1 -HumanDecision ContinuarAjustes.
A correção extraiu Duration.ofSeconds(3) da lambda de assertThrows no teste CDI.
O teste focado passou; nova suíte/checkpoint: **1.172 testes em 184 classes**, zero falhas,
erros ou ignorados, sem broker. As seis provas de integração anteriores permanecem válidas;
não foram repetidas após alteração exclusiva na organização do teste sem broker.

Sonar **COMPLIANT / NOT_REQUIRED**, 213 issues, nenhuma nova ou HIGH/BLOCKER/CRITICAL,
cobertura **87,4%**, duplicação **4,3%**. Data: 2026-09-09T12:05:06.5544417+00:00;
análise: aab863f9-bbc0-4088-aace-e28abdf6382c; CE: ad5d8a85-bfce-440a-ac3c-2462dfbadd46;
fingerprint: 4d1adf3c64b2ac87ac3a8aa89c8a768d70fa8ba11a58fad8f7d92d2400a024a0.
Baseline original de 217 issues conferido integralmente igual ao início da subfatia.
Sem InitializeBaseline, staging, commit ou push.

7.1-A concluída; 7.1-B/C/D continuam pendentes. Não houve decisão sobre política de versão
divergente nem mudança nos códigos de negócio. A política v1, propriedades, config mapping
e producer CDI já existentes foram preservados. Ver [continuidade de 7.1](continuidade-7-1.md).
