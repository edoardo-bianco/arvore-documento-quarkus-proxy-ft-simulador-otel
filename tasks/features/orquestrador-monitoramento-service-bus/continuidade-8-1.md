# Continuidade de 8.1 — reagendamento transacional

## Checkpoint da preparação do commit — 2026-09-10

A preparação posterior do commit reuniu 42 arquivos no índice Git, incluindo novos testes
e documentação. A revisão da produção acumulada não encontrou bloqueadores. Commit/push
não executados; o patch temporário e o estado Sonar continuam fora da seleção.

git diff --cached --check revelou linhas vazias extras no EOF de seis testes antes não
rastreados e deste Markdown. O plano foi atualizado antes da edição; foram removidas apenas
essas linhas. Diff conferido sem mudanças de lógica, assertions, imports ou configuração.
O fingerprint mudou e o checkpoint obrigatório foi repetido no baseline original.

**1.293 testes padrão/189 classes e build aprovados; COMPLIANT / NOT_REQUIRED**.
Cobertura **87,9%**, duplicação **4,3%**, 213 issues abertas, nenhuma nova ou
HIGH/BLOCKER/CRITICAL. As 21 integrações em cinco classes de 09/09 permanecem evidência
separada; não foram repetidas para a formatação. Os 68 testes focados de 8.1 já estavam
aprovados e estão incluídos na suíte completa.

Checkpoint 2026-09-10 08:25:06 -03:00; análise `79215d12-7a59-4071-b60a-08d491e8815b`;
Compute Engine `ad62cf3a-a7c6-439d-8c22-eeaef0e20743`.
Fingerprint: `3871c60389fe2d7425a3c22d8c74e0c14146088e4501d8f8ba61bacf7f1482f7`.
Baseline de 217 issues comparado integralmente com a evidência anterior: idêntico.
Cópia completa da sessão final em
`.codex/.state/session-after-commit-preparation-20260910.json`; cópias anteriores preservadas.

Links locais e âncoras conferidos. Diff preparado sem erros, 42 caminhos iguais ao
[manifesto](pacote-commit.md) e nenhum arquivo selecionado com alterações fora do índice.
O estado executável após a documentação corresponde ao fingerprint deste checkpoint.
Guias alinham os comandos e explicitam que subir a aplicação não inicia o listener.
9.1 e 10.1 permanecem pendentes. Nenhum comando de trabalho ficou em execução.

## 8.1 concluída tecnicamente — 2026-09-10

As cinco correções autorizadas por ContinuarAjustes foram aplicadas e confirmadas
como CLOSED/FIXED na API local. Passaram 68 testes focados e 1.293 testes padrão em
189 classes, sem falhas, erros ou ignorados. Build e checkpoint completos concluídos:
**COMPLIANT / NOT_REQUIRED**, cobertura **87,9%**, duplicação **4,3%**, 213 issues
abertas, nenhuma nova ou HIGH/BLOCKER/CRITICAL.

As 21 integrações em cinco classes passaram em 2026-09-09; não foram repetidas nesta
retomada, pois os ajustes preservam o comportamento. Revisão independente dos cinco
ajustes concluída sem apontamentos. 9.1 e 10.1 permanecem pendentes e não foram iniciadas.
Não houve encerramento humano da feature, staging, commit ou push.

### Correções e verificação

- S6878: record pattern `Agendar(var pedido)` no listener, preservando o pedido e o settlement.
- S1905: removido somente o cast à esquerda; `(long) atual.tentativaAtual() + 1`
  continua evitando overflow antes da comparação.
- Três S5778: `operacao.await()` extraído antes de `assertThrows`; somente
  `espera.atMost(ESPERA)` permanece na lambda. A assinatura segue ocorrendo em atMost.

Regressão focada: modelo 6, adapter 14, listener 39 e política 9 testes.
Foram mantidos os testes de limites, falhas síncronas/assíncronas, ordem transacional,
cancelamento e liquidação única; não houve mudança de comportamento para exigir novo RED.

Checkpoint em `2026-09-10T10:40:27.2869134+00:00` (07:40:27 de Brasília).
Análise `73b3dcd4-87ef-4158-a283-c6ba7195cf86`;
Compute Engine `301a31b8-9ff3-47d0-adbe-09d8da8a0040`.
Fingerprint executável:
`df66ee16e9f1f1de22ebbef4a6d013d3b3d84baceef7faea941f8b92b96c2e0d`.
Os cinco IDs listados no histórico abaixo foram consultados individualmente e estão
CLOSED/FIXED, sem supressão ou aceite excepcional.

### Recuperação do baseline e risco de retomada

Na abertura desta sessão, `.codex/hooks/sonar-session-start.ps1` sobrescreveu
`.codex/.state/session.json` com baseline nulo. Antes de editar código, foi recuperado
o baseline READY/LOCAL_SONAR da cópia `session-restored-s7467-20260907.json`,
na mesma pasta, preservando integralmente seus 217 registros, severidades, regras e métricas.
A análise original continua `f6183a72-a2ea-44bc-9374-b2b064bdad55`.
Não foi executado InitializeBaseline nem escolhido outro pacote/fonte.

A API local confirmou a análise de 09/09 e as mesmas cinco issues. O fingerprint local conferiu:
`44db361ec0b1271c298c840727390754fe6d964b3b66436d24927803a1050ee3`.
O registro vazio foi salvo em `session-before-8-1-sonar-recovery-20260910.json`.
O checkpoint antigo de 06/09 da cópia não foi reapresentado como atual: o campo foi limpo
até a conclusão do novo checkpoint. A evidência de 09/09 e a decisão ContinuarAjustes
permaneceram registradas nesta pasta, sem inferir nova decisão humana.

Baseline novamente comparado integralmente após o checkpoint: idêntico.
Cópia final completa em `.codex/.state/session-after-8-1-checkpoint-20260910.json`,
com conteúdo idêntico a session.json. Em nova sessão, conferir o estado antes de editar
código e usar essa cópia validada se o hook voltar a apagá-lo. A correção do hook é risco
de tooling separado, fora das cinco correções autorizadas; nenhum hook foi alterado.

Branch, HEAD d83b689, arquivos rastreados/não rastreados e
`.codex-doc-alignment.patch` preservados. Nenhum comando de trabalho ficou em execução.
O próximo item funcional é 9.1, a detalhar e executar em sua própria fatia.

## Histórico — pausa de 2026-09-09

**Implementação e testes concluídos; fechamento técnico pendente de cinco ajustes Sonar.**
O usuário escolheu ContinuarAjustes, registrado com sucesso por
`./validar-checkpoint-sonarqube.ps1 -HumanDecision ContinuarAjustes`.
Em seguida pediu o ponto seguro para amanhã. Nenhuma das cinco correções foi aplicada.

Branch `feature/orquestrador-monitoramento-service-bus`; HEAD publicado
`d83b6895a196718880e8e3d23519702f6eb77682`, até 7.1-A.
Alterações posteriores de 7.1-B/C/D e 8.1 continuam locais, sem novo staging/commit/push.
Todos os arquivos rastreados/não rastreados e o baseline original estão preservados.
Comandos Maven/Sonar concluídos; nenhum comando de trabalho mantido em execução.
Containers temporários dos testes encerrados; serviços preexistentes preservados.
Esta pausa altera somente Markdown, sem repetir testes, análise ou implementação.

O [ponto de retomada](retomada.md) contém o texto para iniciar amanhã;
o [checklist](todo.md) identifica a próxima fatia já autorizada.
9.1 e 10.1 permanecem pendentes.

## Escopo autorizado e estado implementado

Pedido humano: "Próxima etapa: 8.1 — reagendamento transacional".
O [plano](plan.md) registrou o escopo antes da primeira alteração executável.
Prova com o broker precedeu a implementação produtiva.

- ReagendamentoMonitoramento passou a representar próxima tentativa e instante.
  Preserva IDs, início, prazo e versão recebida; incrementa somente o contador decidido.
  O horário é o menor entre processamento + intervalo e limite original; compara a
  duração restante antes da soma para evitar overflow de Instant.
- Política verifica o prazo original e o máximo aplicável antes de incrementar.
  Na tentativa Integer.MAX_VALUE, resultado conclusivo continua válido; ainda não conclusivo
  encerra por MAXIMO_TENTATIVAS. MAX_VALUE - 1 pode agendar MAX_VALUE.
- MonitoramentoReagendamentoAdapter CDI associa receiver/mensagem por entrega e devolve uma
  implementação local da porta. Handles SDK permanecem na borda, sem mapa singleton ou
  ThreadLocal. Usa mapper/DTO próprios e serializa antes de abrir a transação.
- Receiver cria a transação; sender da entrada agenda a próxima mensagem com o contexto;
  Complete da entrega atual usa o mesmo contexto; commit confirma ambos.
  Falha de schedule/Complete tenta rollback. Falha de commit representa resultado incerto,
  sem rollback, retry local ou segundo settlement.
- Listener executa a porta fora do recovery do processamento. Sucesso transacional não gera
  Complete simples adicional; falha encerra a assinatura sem avançar para outra entrega.
  Início segue explícito por iniciar(), processamento serial e shutdown preservados.
- Adapter compartilha o futuro da operação entre assinaturas, permitindo cancelamento chegar
  ao SDK sem criar outra transação em nova assinatura da mesma operação. Cancelamento é
  melhor esforço e não garante desfazer efeitos remotos já iniciados.
- Restam três esqueletos do consumo/log da saída, em 9.1. Nove das onze portas estão conectadas.
  Arquitetura consolidada, Javadocs e guias foram alinhados ao estado implementado.

## Limites reafirmados pelo usuário

O usuário confirmou que atingir o máximo encerra o monitoramento pela publicação
de QUARENTENA na fila de saída e reforçou o período máximo de XXH. Primeira consulta
imediata; resultado conclusivo encerra normalmente; ainda não conclusivo no máximo
encerra por MAXIMO_TENTATIVAS, sem novo agendamento. Prazo vencido encerra por PRAZO_MAXIMO
antes de consultar o Hub, com prioridade quando ambos os limites estiverem esgotados.

max-tentativas continua opcional nas políticas; o padrão v1 não ganha teto operacional.
Default: PT30M repetido, janela original normalmente PT24H. Lista PT3H/PT4H/PT6H significa
esses intervalos entre tentativas, repetindo PT6H depois. O prazo nunca é renovado.

Versão recebida configurada prevalece, mesmo inativa. Definição removida usa v1 interna,
preservando versão recebida, início e prazo; isso não gera quarentena por política ausente.
Um intervalo que ultrapassaria o prazo agenda no próprio prazo para encerramento.
O dev escolhe emulador ou Azure para a aplicação; testes padrão seguem sem broker.

Mapeamentos preservados: FINALIZADO_CONFORME → CONFORME,
FINALIZADO_INCONFORME → INCONFORME, PENDENTE_INFORMACA → INCONFORME;
situacaoMtr permanece original.

## Provas e regressão executadas

Versões efetivas: SDK Service Bus 7.17.12, extensão 1.2.5, Quarkus 3.33.2.1,
JDK 25, Reactor 3.4.41 e Mutiny 3.1.1. Nenhuma dependência foi atualizada.

RED/GREEN executado para limite inteiro, modelo, adapter e conexão ao listener.
Regressão de cancelamento inicialmente falhou em dois casos: memoizar o Uni final retinha
o upstream. Memoizar a referência do futuro e transformar para Uni fora da memoização
corrigiu os casos, incluindo cancelar e reassinar sem repetir a transação.
O teste do adapter cobre 14 cenários de ordem/contexto, falhas síncronas/assíncronas,
rollback, commit incerto, cancelamento e assinatura compartilhada.

Comando da integração completa, concluído com sucesso:

```powershell
mvn -q -Pservicebus-integration clean test
```

**21 testes em cinco classes**, sem falhas, erros ou ignorados:

| Classe | Testes | Evidência |
|---|---:|---|
| ServiceBusDevServicesTest | 1 | Infraestrutura explícita de integração |
| MonitoramentoTerminalEmuladorTest | 12 | Nove cenários anteriores e três fluxos completos de reagendamento |
| PublicarResultadoMonitoramentoEmuladorTest | 2 | Publicação da saída |
| ReagendamentoTransacionalEmuladorTest | 3 | Commit, rollback/redelivery e confirmação local perdida |
| MonitoramentoEntradaEmuladorTest | 3 | Entrada e contrato já existentes |

Os três novos fluxos completos cobrem PT1S/PT2S/PT3S repetindo PT3S com máximo 5,
encerrando em QUARENTENA/MAXIMO_TENTATIVAS ou CONCLUSIVO/CONFORME; e versão removida,
com default PT30M limitado ao prazo original de dez segundos, uma consulta ao Hub e
encerramento por PRAZO_MAXIMO sem nova consulta.

A prova transacional mantém a assinatura original até verificar os efeitos por peek com
cursor explícito. Rollback permite nova entrega com mesmo corpo/sequência e novo handle/lock,
sem mensagem futura, incluindo observação após o horário inicialmente agendado.
Filas isoladas e vazias verificadas, sem purge ou skip.

A suíte padrão do checkpoint passou: **1.293 testes sem broker**, sem falhas, erros ou
ignorados. Revisão independente encerrada sem bloqueadores; duas observações de Javadoc
foram corrigidas antes do checkpoint.

## Diagnósticos resolvidos da prova inicial

- A sequência de agendamento muda quando a mensagem é ativada; expectativa corrigida conforme
  [documentação oficial](https://learn.microsoft.com/en-us/azure/service-bus-messaging/message-sequencing).
- Abandon adicional após rollback falhava porque o SDK já considera o handle settled.
  A prova passou a exigir nova entrega, sem segunda ação nesse handle.
- DeliveryCount não aumentou no rollback. A
  [API Java](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusreceivedmessage?view=azure-java-stable)
  associa incremento a Abandon/expiração de lock; o teste verifica novo handle/lock.
- O timeout de peek observado durante a preparação foi resolvido mantendo a assinatura
  até conferir os efeitos. Os três casos transacionais e a integração completa passaram.
- Confirmação local perdida é exceção deliberada após commit real. Não representa uma
  falha real de rede, nem valida todas as condições do Azure gerenciado.

## Histórico — checkpoint de 09/09 e ajustes então pendentes

Checkpoint de `2026-09-09T21:03:03.546826-03:00`: **NON_COMPLIANT**.
Cobertura **87,9%**; duplicação **4,3%**; 218 issues atuais, **cinco novas**;
nenhuma HIGH/BLOCKER/CRITICAL. A contagem atual não é soma simples do baseline e das novas,
pois issues anteriores podem ter sido resolvidas.

Baseline READY original de 217 issues conferido integralmente igual ao início de 8.1;
nenhuma reinicialização. Análise: `d910434c-cb7c-4b77-815b-7d6f67c00c0f`.
Compute Engine: `03bb1a78-8e49-44fd-accf-d4588a49b5d5`.
Fingerprint executável:
`44db361ec0b1271c298c840727390754fe6d964b3b66436d24927803a1050ee3`.

**Decisão humana posterior: ContinuarAjustes, registrada com sucesso.**
Nenhum arquivo executável foi alterado depois desse checkpoint; a pausa só atualiza Markdown.

Os caminhos Java abaixo estão sob `src/main/java/br/gov/caixa/simtr/` ou
`src/test/java/br/gov/caixa/simtr/`, conforme a classe:

| Regra | Severidade | Local na pausa | Ajuste autorizado pendente |
|---|---|---|---|
| S6878 | MAJOR | monitoramento/adaptador/entrada/servicebus/MonitoramentoEntradaListener.java:113 | Usar record pattern no case Agendar e acessar o pedido capturado |
| S1905 | MINOR | monitoramento/dominio/modelo/ReagendamentoMonitoramento.java:23 | Remover somente o cast redundante à esquerda; manter o cast à direita antes da soma |
| S5778 | MAJOR | monitoramento/adaptador/saida/servicebus/MonitoramentoReagendamentoAdapterTest.java:82 | Extrair operacao.await() da lambda assertThrows |
| S5778 | MAJOR | Mesmo teste:93 | Mesmo ajuste, deixando apenas atMost(ESPERA) na lambda |
| S5778 | MAJOR | Mesmo teste:117 | Mesmo ajuste, deixando apenas atMost(ESPERA) na lambda |

IDs das issues, na ordem da tabela:

- `797371e2-3813-4af3-8930-811d5e792495`
- `5a88b011-82f9-42cd-bd5f-fce0012ab710`
- `2c2209f9-c3c0-4d3e-9924-a7c1e74abe21`
- `f3b9daf7-5120-4a6b-8129-de36e26d6132`
- `2614cd4c-7681-4e12-9ad6-b4ad6aa80bb4`

## Roteiro dos ajustes — executado em 2026-09-10

Não pedir novamente a autorização já registrada. Conferir as linhas atuais, aplicar os cinco
ajustes mantendo o comportamento e executar a regressão focada sem broker:

```powershell
mvn -q "-Dtest=ReagendamentoMonitoramentoTest,MonitoramentoReagendamentoAdapterTest,MonitoramentoEntradaListenerTest,PoliticaMonitoramentoProgressivaTest" test
```

Depois da fatia coerente, na sessão iniciada pelo launcher com token somente em memória:

```powershell
./validar-checkpoint-sonarqube.ps1
```

Usar o baseline existente; não executar InitializeBaseline. Registrar o resultado real e
atualizar retomada/checklist. Repetir integração somente se surgir alteração de comportamento,
falha ou preocupação não coberta; os 21 casos já passaram antes destas correções de sintaxe/teste.
Se houver indisponibilidade do servidor/token, registrar a limitação sem declarar aprovação.

Limitações preservadas: prova local no emulador, sem validação em Azure real; confirmação
incerta não equivale a rollback; cancelamento não desfaz efeitos remotos garantidamente.
Publicação terminal + Complete ainda não é atômica; não há Outbox/idempotência durável.
9.1 (consumo/log da saída), 10.1 (telemetria) e fechamento da feature permanecem pendentes.
