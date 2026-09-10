# Continuidade de 7.1 — processamento terminal

## Fechamento técnico de 7.1-D — integração terminal

Pedido humano "7.1-D — integração terminal" executado; 7.1 está concluída tecnicamente.
Novo MonitoramentoTerminalEmuladorTest com nove cenários opt-in, SDK/emulador e composição
CDI reais: REST/entrada/listener/caso de uso/catálogo/pré-validação/ACL/publicação da saída.
Somente a porta pública ConsultarDossieProduto é controlada para fornecer situações originais
com id nulo; nenhum código do Hub, contrato, dependência ou caminho de produção foi alterado.

Cenários verificados:
- três nomes originais via REST: FINALIZADO_CONFORME -> CONFORME,
  FINALIZADO_INCONFORME e PENDENTE_INFORMACA -> INCONFORME, preservando situacaoMtr;
- no-op sem Hub/saída e Complete da entrada;
- prazo recebido expirado com versão removida, sem renovar janela nem consultar o Hub;
- política configurada inativa v-limitada com max=1 permite consulta e depois encerra;
- versão removida ainda no prazo continua por v1, preservando tentativa funcional 4;
- falha transitória do Hub com Abandon e nova entrega: mesma sequência/corpo, DeliveryCount
  crescente, duas consultas e uma saída final com tentativa funcional 3;
- contrato inválido na DLQ real com motivo/descrição fixos, sem chamada ao Hub.

Controle negativo: com iniciarListener vazio apenas no teste, bootstrap/CDI/envio funcionaram
e a espera de resultado falhou por timeout de 30 s. Produção preservada. Com início explícito
ligado, os nove cenários passaram. A revisão independente pediu ausência de segunda saída
em quatro casos; verificações acrescentadas, execução serial explícita e v1 do REST conferida.
Revisão final sem findings ou bloqueadores.

**Integração final:** mvn -q -Pservicebus-integration clean test — 15 testes/4 classes,
zero falhas, erros ou ignorados: 9 terminais novos, 3 de entrada/REST, 2 de publicação de
resultado e 1 de infraestrutura. As seis provas anteriores foram repetidas nesta execução.
O profile composto reaproveita a proteção C2-R1 contra conexão Azure externa. Filas devem
estar vazias no início; resíduos falham a prova, sem purga silenciosa. Handles CDI são
destruídos por cenário; Complete de saída/DLQ ocorre antes de next/cancelamento. Remoção
da entrada é conferida com peek por sequência explícita, sem avançar cursor implicitamente.

**Suíte padrão:** 1.268 testes/187 classes, zero falhas, erros ou ignorados, sem broker.
**Sonar COMPLIANT / NOT_REQUIRED:** cobertura 87,8%, duplicação 4,3%, 213 issues abertas,
nenhuma nova ou HIGH/BLOCKER/CRITICAL.
Checkpoint: 2026-09-09T17:13:06.2689001-03:00.
Análise: e0c9cf64-a31c-4c52-8904-67fcf1f1de61; CE: c48e3332-015c-40b9-9c00-499f8583a189.
Fingerprint: 52a0d878f51874e79f33ad04798b5009a79a772bb1205892dad3cb4b08b57944.
Baseline READY original de 217 issues integralmente preservado, sem reinicialização.
Não houve mudança executável após esse checkpoint.

Guias e arquitetura alinhadas à prova. Corrigidas referências desatualizadas da própria
feature: caso de uso ainda sem publisher e inventário antigo de oito esqueletos; código
confirmado tem cinco. Histórico de execução permanece nas tasks e marco Git publicado
continua d83b689. Alterações de 7.1-B/C/D locais, sem staging/commit/push.
Comandos concluídos; containers temporários do emulador/SQL/Ryuk encerrados pelo teste,
preservados os containers preexistentes do Sonar e Kubernetes.

Próximo item funcional: **8.1 — reagendamento transacional**. Detalhar intervalo que alcança/
ultrapassa limiteEm e contador no limite inteiro; provar schedule + Complete na mesma entrega,
rollback e redelivery antes de afirmar atomicidade. Consumo geral permanece inativo até essa
capacidade. Azure gerenciado, listener da saída/9.1, telemetria/10.1 e fluxo completo não
fazem parte desta evidência. Publicação terminal/Complete continuam não atômicos; cancelamento
local não garante interrupção de efeito remoto já iniciado. O dev escolhe emulador ou
filas Azure para aplicação; testes padrão permanecem sem broker.

## Fechamento técnico de 7.1-C — listener da entrada

Pedido humano "7.1-C" executado na mesma branch, preservando alterações locais e baseline.
MonitoramentoEntradaListener saiu do inventário @Vetoed: agora é CDI, com início explícito
por iniciar(), sem StartupEvent, flag ou endpoint de ativação. Oito das onze portas continuam
conectadas; restam cinco esqueletos inativos. Nenhuma etapa de 7.1-D/8.1 foi antecipada.

A borda usa mapper existente e porta de processamento com sequência escalar. Processa uma
entrega por vez, com concatMap(..., 0) e prefetchCount(0) somente no receiver da entrada.
Ignorar registra decisão e executa Complete; ResultadoPublicado só permite Complete após
a confirmação da publicação. Contrato inválido no mapper gera DeadLetter com opções novas
e diagnóstico fixo; falha técnica anterior ao settlement gera Abandon. Falha de settlement
encerra a assinatura sem executar outro settlement. ReagendamentoPendente encerra sem
settlement ou reinício. O início é único; shutdown cancela a cadeia antes de a fábrica
fechar os clientes. Cancelamento local é best effort e não comprova interrupção do envio remoto.
Publicação na saída e Complete ainda não são atômicos; redelivery pode duplicar saída.

RED falhou pela API ausente. GREEN final: 99 testes focados em seis classes, sem broker,
incluindo 35 testes do listener e prova JSON de decisão/falha/settlement. Cobertos:
publicação antes de Complete, serialização das entregas, falhas síncronas/assíncronas,
classificação por fase, ausência de segundo settlement, cancelamento durante processamento
ou Complete, término síncrono, corrida de início/shutdown com latches, prioridade CDI real,
opções de DLQ independentes e ausência de payload/Throwable nos novos logs mínimos.
A revisão independente terminou sem findings ou bloqueadores.

Primeiro checkpoint: 1.268 testes passaram; NON_COMPLIANT por 13 java:S8924 MINOR,
todos referentes a imports estáticos de Mockito em ClientesServiceBusTest e
MonitoramentoEntradaLogTest. O usuário solicitou corrigir todos os novos; ContinuarAjustes
foi registrado pelo script. Imports corrigidos e checkpoint completo repetido.

**Evidência final:** 1.268 testes padrão/187 classes, zero falhas, erros ou ignorados, sem broker.
**COMPLIANT / NOT_REQUIRED**: cobertura 87,8%, duplicação 4,3%,
213 issues abertas, nenhuma nova ou HIGH/BLOCKER/CRITICAL.
Checkpoint: 2026-09-09T16:19:40.3504657-03:00.
Análise: 1c3dc85d-1a1d-40d0-b9f1-836f72bdd31a; CE: e604a6c6-5046-4e17-8438-2388d9e0722c.
Fingerprint: f1f839c71c98d0d2a7cbab62cc71601b26791ff2d41dbb033077232ddd494e71.
Baseline READY original de 217 issues integralmente preservado, sem reinicialização.
Nenhuma integração com broker foi executada nesta fatia; as seis de 7.1-A são históricas.
SDK efetivo 7.17.12 e Reactor 3.4.41 confirmados; dependências e perfis preservados.
Logs completos/propagação permanecem em 10.1.

Próxima fatia funcional: 7.1-D, integração terminal real com início explícito e cenários
controlados. Antes do consumo geral, 8.1 deve implementar reagendamento/transação e tratar
intervalo versus prazo original e contador no limite inteiro. O dev escolhe emulador ou
filas Azure para a aplicação; testes padrão continuam sem broker.
O commit publicado continua d83b689. Alterações de 7.1-B/C permanecem locais, sem novo
staging/commit/push. Nenhum comando desta execução ficou em andamento.

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

## Estado anterior — ajuste do intervalo padrão

A direção humana mais recente muda a v1 padrão configurada e a recuperação interna para
PT30M repetido, duração PT24H e sem teto adicional de tentativas. O usuário confirmou que
max-tentativas deve permanecer opcional para outras configurações. Uma lista como
PT3H,PT4H,PT6H aplica 3 h, 4 h, 6 h e repete 6 h até o prazo original.

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

## Estado anterior — resolução de políticas em 7.1-B

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

## Evidência anterior — contrato de 7.1-B

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

Os registros seguintes descrevem o fechamento de 7.1-A antes do commit/push d83b689.


## Estado em 2026-09-09

C2 aceito pelo usuário; pedido explícito "7.1" registrado. **7.1 está em andamento.**
A subfatia **7.1-A está concluída**, com publisher de resultado e testes aprovados.
Após a decisão humana ContinuarAjustes, S5778 foi corrigida no teste CDI e o checkpoint
final está **COMPLIANT / NOT_REQUIRED**. Não foi iniciado o item 8.1.
## Checkpoint final após ajuste autorizado

O usuário escolheu ContinuarAjustes e a decisão foi registrada pelo script. Duration foi
calculado antes do assertThrows; o teste CDI passou novamente. A política v1 e o código de
produção permaneceram iguais. O checkpoint completo posterior passou:

- 1.172 testes em 184 classes, zero falhas/erros/ignorados, sem broker.
- COMPLIANT / NOT_REQUIRED, cobertura 87,4%, duplicação 4,3%.
- 213 issues; nenhuma nova ou HIGH/BLOCKER/CRITICAL.
- Data: 2026-09-09T12:05:06.5544417+00:00; análise: aab863f9-bbc0-4088-aace-e28abdf6382c.
- CE: ad5d8a85-bfce-440a-ac3c-2462dfbadd46.
- Fingerprint: 4d1adf3c64b2ac87ac3a8aa89c8a768d70fa8ba11a58fad8f7d92d2400a024a0.
- Baseline original integralmente preservado; sem staging, commit ou push.

As seis integrações já passaram antes da alteração exclusiva na organização do teste CDI;
não foram repetidas. **7.1-A concluída; o item 7.1 completo permanece pendente.**


## Entrega implementada

- [MonitoramentoResultadoPublisher](../../../src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/servicebus/MonitoramentoResultadoPublisher.java)
  implementa a [porta de publicação](../../../src/main/java/br/gov/caixa/simtr/monitoramento/aplicacao/porta/saida/PublicarResultadoMonitoramento.java).
- Mapper próprio e sender compartilhado FilaSaida, sem criar clientes ou classificar situações.
- Uni preguiçoso; uma publicação por invocação compartilhada, concluída somente após confirmação.
  Invocações independentes podem reenviar; não há deduplicação durável.
- Falhas síncronas/assíncronas do SDK são traduzidas para mensagem fixa sem causa externa;
  falha própria do mapper é preservada. Não foi acrescentado retry.
- Porta/publisher retirados da condição de inatividade; sete portas com implementação conectada
  e oito classes ainda inativas. Caso de uso de processamento e listeners permanecem intactos.

## Verificações

1. RED de MonitoramentoResultadoPublisherTest: compilação falhou pela ausência esperada
   do construtor e de executar. GREEN: seis testes puros passaram, sem Quarkus/broker.
2. Regressão focada: **145 testes**, zero falhas/erros/ignorados:
   publisher 6; CDI 1; arquitetura 10; inatividade 8; contrato de resultado 120.
3. Integração explícita: **seis testes em três classes**, zero falhas/erros/ignorados,
   concluídos em 2026-09-09 às 08:42:45 locais. Quatro provas anteriores preservadas;
   duas novas provam publicação de conclusivo/quarentena e contrato consumidor independente.
   Na nova prova, Complete integra a cadeia antes do cancelamento do recebimento.
   Não representa processamento completo, settlement de listener ou atomicidade entre filas.
4. Checkpoint completo: **1.172 testes padrão em 184 classes**, zero falhas/erros/ignorados.
   As três classes com broker não participam dessa suíte; o teste puro do profile de
   emulador participa sem iniciar broker. Sete novos testes padrão substituem uma prova de
   inatividade do publisher, explicando o aumento líquido de seis.
5. Sonar inicial, antes do ajuste: **NON_COMPLIANT / PENDING**, cobertura **87,4%**, duplicação **4,3%**,
   214 issues, uma nova, nenhuma HIGH/BLOCKER/CRITICAL.
   Data: 2026-09-09T11:51:44.9594983+00:00; análise: ab6319c9-fe8c-42e5-8c7b-db445bcc5d2a;
   Compute Engine: df45c927-772c-415b-88c7-4ec554f6f549;
   fingerprint: 90802b1571571db5aa503395356d391b8fd7adffad61168482aa418780050c0e.
6. Baseline original integralmente preservado: 217 issues, análise
   f6183a72-a2ea-44bc-9374-b2b064bdad55. Sem InitializeBaseline.
7. Revisão independente somente leitura não encontrou bloqueador no publisher.
   git diff --check passou. Contêineres temporários encerrados;
   simtr-hub-poc-control-plane e sonarqube-simtr-local preexistentes preservados.

## Histórico da issue Sonar e decisão humana

- Chave: 06c3e2fe-65e5-465c-8fa7-809d7e913f50; regra java:S5778; severidade MAJOR
  (impacto de manutenibilidade MEDIUM).
- Arquivo: [PublicarResultadoMonitoramentoQuarkusTest.java](../../../src/test/java/br/gov/caixa/simtr/monitoramento/integracao/PublicarResultadoMonitoramentoQuarkusTest.java),
  linha 29.
- A lambda de assertThrows chama Duration.ofSeconds além de atMost.
  Proposta concreta: calcular Duration antes do assertThrows e deixar somente atMost na lambda.
- Pergunta enviada ao usuário com Reprovar, AceitarExcepcionalmente ou ContinuarAjustes,
  conforme AGENTS.md. O usuário escolheu ContinuarAjustes; decisão registrada e ajuste
  aplicado, com checkpoint final COMPLIANT conforme evidência acima.

## O que já existe para a política v1

A dúvida do usuário foi conferida diretamente no código:
[application.properties](../../../src/main/resources/application.properties) seleciona padrao,
versão v1, tipo progressiva, intervalos PT30M/PT3H/PT4H/PT6H e duração máxima PT24H.
O último intervalo se repete; max-tentativas está comentado e não limita a contagem atualmente.
[PoliticasMonitoramentoConfig](../../../src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/configuracao/PoliticasMonitoramentoConfig.java)
lê esses campos por ConfigMapping; o producer valida as definições e disponibiliza a
PoliticaMonitoramentoProgressiva selecionada por CDI. A política já calcula limite,
próxima tentativa e intervalo e decide encerramento por limites. O agendamento efetivo
no broker pertence à 8.1. Não é necessário refazer a configuração v1.

A definição pendente é o tratamento de uma mensagem com versão diferente da ativa.
Foi proposta a consulta da definição recebida, quando disponível, com quarentena quando
ausente; alternativa apresentada: quarentena para toda divergência. Nenhuma foi escolhida.

## Próximo trabalho e limites

- 7.1-A concluída após ajuste autorizado; usar o checkpoint final e o baseline preservado.
- Completar 7.1-B após as definições de versão e códigos literais do mapeamento informado.
  Preservar situacaoMtr e calcular situacaoPreValidacao separadamente.
- Detalhar 7.1-C antes de habilitar consumo. A revisão recomenda concorrência 1 e
  inputSequenceNumber como long separado na porta; SDK/handles permanecem na borda.
  Classificar primeiro uma única ação e executar um settlement; falha no próprio settlement
  não deve acionar um segundo settlement.
- Proposta técnica ainda não aplicada: manter o consumo geral inativo até a prova transacional
  de 8.1, com teste controlado de terminal em 7.1. Abandon repetido de não conclusivos pode
  esgotar DeliveryCount; não representa reagendamento funcional.
- Lifecycle proposto: listener cancela sua assinatura antes do fechamento da fábrica;
  não manter contexto mutável de mensagem no singleton.
- API local 7.17.12 possui assinaturas transacionais, mas schedule + Complete ainda precisa
  de prova no SDK/emulador em 8.1. Publicar saída e Complete da entrada podem duplicar resultado
  se a publicação confirmar e o settlement falhar.

Preservadas todas as alterações locais, branch e baseline. Sem staging, commit ou push.
Testes padrão sem broker; a escolha de emulador ou Azure continua pertencendo ao desenvolvedor.
