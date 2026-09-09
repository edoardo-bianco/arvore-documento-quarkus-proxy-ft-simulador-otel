# Continuidade de 7.1 — processamento terminal

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
