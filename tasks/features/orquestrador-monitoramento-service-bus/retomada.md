# Retomada — orquestrador e monitoramento Service Bus

## Diagnóstico de dev mode verificado — 2026-09-10

Após a publicação 0ec09ac, o usuário pediu verificar o startup e a emulação do Service Bus.
Duas reproduções iniciaram em 24,416 s e 27,213 s. SQL/emulador criaram as duas filas,
health/Swagger responderam 200 e o POST com fixture real 4324680 retornou 202 e chegou ao
log final com IDs correspondentes. Avisos JDBC foram transitórios; o bloqueio persistente
original não foi reproduzido. [Evidências, limites e operação](diagnostico-devmode-20260910.md).

Nenhuma mudança executável ou nova análise Sonar. .env, baseline, aceite C9.1-L e patch local
preservados. Aplicações e containers do diagnóstico encerrados; infraestrutura preexistente
preservada. Somente Markdown alterado; commit e push deste diagnóstico foram autorizados pelo
usuário após a verificação, sem nova mudança executável.
Próximo item funcional continua **10.1**, seguido dos checkpoints restantes.
Os registros abaixo descrevem o estado antes desta verificação operacional.

## Marco publicado de 9.1 e retomada — 2026-09-10

9.1-A/B/C publicada na branch `feature/orquestrador-monitoramento-service-bus`:
`fcbe38a2a8a857668f0d6fc1377c02783c17ff4c` (implementação/regressões) e
`2c6c4cde881c3b8e2261e3a615123d58290fe5c4` (integração).
Push normal e hash remoto confirmados. O [manifesto de 9.1](pacote-commit-9-1.md)
registra seleção, evidências e limites; o [guia do desenvolvedor](guia-desenvolvimento.md#roteiro-para-assumir-a-entrega)
permite executar os dois listeners e retomar manualmente o desenvolvimento.

O código continua idêntico ao checkpoint final: 1.345 testes sem broker, 27 integrações,
build SUCCESS e Sonar COMPLIANT. Baseline e aceite C9.1-L preservados.
A consolidação acrescenta somente Markdown. Preservar .codex-doc-alignment.patch fora do Git.

O usuário pediu terminar a publicação antes de analisar o problema do startup Quarkus em
dev mode (avisos JDBC de prelogin do SQL Server). Diagnóstico operacional pendente após
esta publicação; não declarar execução manual validada nem aplicar correção por suposição.
**Próximo item funcional: 10.1**; C3 e encerramento humano continuam pendentes.
Os registros abaixo são históricos dos fechamentos técnicos anteriores à publicação.

## 9.1 concluída tecnicamente — 2026-09-10

9.1-A/B/C implementadas e verificadas no workspace: listener da saída, portas/caso de uso,
registro final e integração com emulador. Ativação independente por
`monitoramento.service-bus.saida.consumo-habilitado=true`; ambos os listeners permanecem
false por padrão. O [guia do desenvolvedor](guia-desenvolvimento.md#prova-integrada-da-saída-em-91-c)
traz o comando de integração e o roteiro para executar os dois consumidores.

Quatro cenários novos: POST terminal; reagendamento até conclusão; Rascunho até máximo3;
inválido na DLQ e novo POST válido depois dela. Hub controlado, demais componentes reais.
Passaram 27 integrações/7 classes e 1.345 testes padrão/192 classes sem broker, zero
falhas/erros/ignorados. Build SUCCESS; Sonar COMPLIANT / NOT_REQUIRED:
88,1% cobertura, 4,4% duplicação, 213 issues, nenhuma nova ou HIGH/BLOCKER/CRITICAL.

A prova DLQ teve duas falhas de ACK do receiver auxiliar por detach remoto. Após aguardar
a própria mensagem visível por peek e a principal vazia, passaram o isolado e a suíte completa,
incluindo o POST posterior. Produção/SDK não alterados; causa interna do emulador não demonstrada.
[Diagnóstico e evidências](continuidade-9-1.md#91-concluída-tecnicamente--2026-09-10).
C9.1-L preservado: Complete após submissão ao logger; falhas internas podem perder o log sem
Abandon. Não há atomicidade log/Complete. Azure gerenciado e dev interativo não executados.

Análise 3546ec18-65c4-4e5b-a382-ed897f69f7b5; checkpoint 2026-09-10T15:27:27.592153-03:00.
Fingerprint 5d84ce454d3e4ab5ba5aabb5b8148e0076ab4ef3a22a0c91c873a211719afd0d.
Baseline original e baselineAssessment integralmente preservados, sem reinicialização.
Cópia final: .codex/.state/session-after-9-1-c-compliant-20260910.json.
Não reabrir C9.1-L nem reinicializar baseline.

**Próximo item: 10.1**, detalhar/caracterizar correlação OpenTelemetry antes de implementar.
10.1/C3 não iniciados; encerramento da feature continua humano. Guias, arquitetura e checklist
alinhados. Sem novo staging/commit/push; branch feature/orquestrador-monitoramento-service-bus
e marco publicado d5df6b6 preservados. Conservar alterações locais e .codex-doc-alignment.patch.
Nenhum comando do agente em execução ao concluir a subfatia. Registros abaixo são históricos.

## 9.1-B concluída tecnicamente — 2026-09-10

Listener da saída implementado e conectado por CDI ao mapper, ao caso de uso e ao log.
Ativar com `monitoramento.service-bus.saida.consumo-habilitado=true`, independente da entrada;
ambos os listeners permanecem false por padrão. O [guia de execução](guia-desenvolvimento.md#listener-da-saída-em-91-b)
mostra o comando dos dois consumidores para Dev Services e a variante Azure.

Complete após conclusão da porta/submissão ao logger; falha técnica propagada gera Abandon,
contrato inválido gera DLQ. Falha de settlement encerra a assinatura sem segundo settlement
ou reinício automático. Shutdown cancela antes da factory, sem fechar o cliente compartilhado.
C9.1-L já aceito: falhas internas de escrita/filtro podem perder o log e não geram Abandon.
HTTP disponível não confirma consumidor ativo.

Passaram 123 testes focados, incluindo 36 do listener e três provas CDI; 23 integrações
existentes/6 classes com saída desabilitada; 1.345 testes padrão/192 classes sem broker.
Zero falhas/erros/ignorados, build SUCCESS. Sonar COMPLIANT / NOT_REQUIRED:
88,1% cobertura, 4,4% duplicação, 213 issues, nenhuma nova ou HIGH/BLOCKER/CRITICAL.
Análise 358f8528-6bde-4c8c-92bd-0d27dc8a50aa; checkpoint 2026-09-10T14:20:35.1083771-03:00;
fingerprint 17fea85859935f12aed57ec6a83070b9c5f80696ea281add4bd74003047e1ac5.
Baseline original e baselineAssessment integralmente preservados, sem reinicialização.
Cópia final local: .codex/.state/session-after-9-1-b-compliant-20260910.json.
[Comandos, revisão e evidências](continuidade-9-1.md#verificação-e-fechamento-técnico-de-91-b).

**Próxima subfatia: 9.1-C**, nova prova no emulador com ativação da saída, POST, log e Complete,
seguida de fechamento de 9.1. Essa prova integrada ainda não foi executada; 10.1 não iniciada.
Guias, arquitetura e checklist alinhados. Nenhum comando do agente em execução.
Branch feature/orquestrador-monitoramento-service-bus; HEAD e origin preservados em
d5df6b6e309842460427df290895f5064d00b5d1. 9.1-A/B permanecem locais, sem novo staging/commit/push.
Preservar alterações locais e .codex-doc-alignment.patch. Não reabrir C9.1-L nem reinicializar baseline.

Os registros abaixo são históricos; a retomada atual é a indicada nesta seção.

## 9.1-A concluída tecnicamente — Sonar conforme

O usuário aceitou Complete após submissão ao logger. Foi esclarecido que falha interna de
escrita/filtro pode deixar o resultado sem log e não gera Abandon/reentrega após Complete.
[Evidência, decisão e limites](continuidade-9-1.md).

Caso de uso e adapter de log implementados localmente, com duas portas conectadas por CDI.
A emissão é lazy, uma vez por invocação, com campos textuais em mdc e contexto preservado.
O listener da saída continua inativo nesta subfatia. Passaram 42 testes focados e 1.307 testes
padrão/191 classes, sem falhas, erros ou ignorados; build SUCCESS.
O usuário decidiu ContinuarAjustes. Variável renomeada para registroJson e S1117 confirmada
CLOSED/FIXED; novo checkpoint COMPLIANT / NOT_REQUIRED, cobertura 88%, duplicação 4,3%,
nenhuma issue nova/grave. Baseline original e baselineAssessment integralmente preservados.
Análise a99ebf50-6eed-4f06-b02b-ff3375817eed; fingerprint
0e6e88d3f5c2f750ef0957596002fa3e2a52f4bfb69002d1d3679887a6b561ef.
Sem novo commit/push ou início de 10.1. Próximo passo: 9.1-B, listener/ativação da saída,
seguido de integração/fechamento em 9.1-C. A definição de submissão best-effort já foi aceita;
não reabrir esse checkpoint nem reinicializar baseline.
Guias, arquitetura e links alinhados. Cópia final local:
.codex/.state/session-after-9-1-a-compliant-20260910.json. Nenhum comando do agente em execução.

## Marco publicado para retomada

8.2 publicada em `a797114d1dc833499dcc957a881e83f431cae7b9`; push normal confirmado em origin na branch
`feature/orquestrador-monitoramento-service-bus`. O [registro de publicação](pacote-commit.md#publicação-confirmada--2026-09-10)
identifica o pacote e as verificações. A primeira pendência funcional continua sendo 9.1.

## Retomada consolidada antes da etapa 9 — 2026-09-10

**8.2 concluída tecnicamente.** O usuário pediu consolidar, atualizar o guia do desenvolvedor,
organizar commit e publicar antes de prosseguir com 9. A leitura inicial de 9.1 não produziu
alterações de código, testes ou configuração. Não executar 9.1 durante esta consolidação.

Branch: `feature/orquestrador-monitoramento-service-bus`. Base publicada até 8.1:
`b886bdb8bb079eadcba2ed21c68739cf62ff3356`. O [manifesto de 8.2](pacote-commit.md)
identifica os arquivos deste incremento; o [manifesto anterior](pacote-commit-8-1.md) é histórico.
Consultar `git log -3 --oneline` e `git status -sb` para os hashes e o estado de publicação.

### O que está disponível

- `orquestrador`: POST publica a primeira tentativa na entrada e retorna 202 após confirmação.
- `monitoramento`: startup ativa a entrada quando
  `monitoramento.service-bus.entrada.consumo-habilitado=true` (default false); processa,
  aplica política e limites, reagenda transacionalmente ou publica resultado na saída.
- Saída do `orquestrador`: listener, caso de uso e adapter de log ainda são estruturas inativas.
  Implementá-los em 9.1, com validação, registro e settlement. A telemetria ponta a ponta é 10.1.
- Os packages simulam responsabilidades de microsserviços no mesmo artifact/runtime Quarkus.
  Hub e `dossie` permanecem preservados.

O [guia do desenvolvedor](guia-desenvolvimento.md#verificação-rápida-do-marco-até-82) tem
comandos completos de Dev Services/Azure, POST manual com a fixture **4324680/Rascunho**,
política curta opcional e expectativas. 0007 permanece apenas nos testes com Hub controlado.
Escolher Azure para o broker não desliga os simuladores do profile dev. O 202 não confirma
conclusão; a saída ainda não é consumida/logada pela aplicação.

### Evidência preservada e próximos passos

Passaram 1.297 testes padrão/189 classes e 23 integrações/6 classes, sem falhas/erros/ignorados.
Build e Sonar COMPLIANT / NOT_REQUIRED: 87,9% cobertura, 4,3% duplicação, zero issues novas/graves.
Código da consolidação idêntico ao checkpoint; detalhes na [continuidade de 8.2](continuidade-8-2.md).
As correções posteriores são documentais; não foi necessário repetir Maven/Sonar.

1. Conferir branch, Git e alterações locais; preservar `.codex-doc-alignment.patch` e artefatos.
2. Ler arquitetura/índice de ADRs, [plano](plan.md), [checklist](todo.md) e guia do dev.
3. No workspace atual, preservar baseline READY/LOCAL_SONAR original; não reinicializar.
   Cópia final: `.codex/.state/session-after-8-2-checkpoint-20260910.json`.
   Em checkout novo, seguir AGENTS.md para a própria sessão; esse estado local não vai ao Git.
4. Retomar **9.1 — consumir saída e registrar resultado**, detalhando sua próxima fatia antes
   da produção. Reutilizar os contratos/portas existentes, preservar lifecycle e provar falhas,
   redelivery e conclusão do log antes do Complete. A seleção de categorias do formatter tipado
   frente ao adapter `adaptador.saida.log` é uma pendência registrada no plano, sem solução aplicada.
5. Depois: 10.1, revisão C3 e consolidação final 11.1/CF. Não há encerramento humano da feature.

Os registros abaixo conservam a evolução anterior. Seus hashes, contagens e menções de trabalho
local ou início exclusivamente manual pertencem ao momento em que foram escritos.

## 8.2 — ativação da entrada concluída tecnicamente — 2026-09-10

Pedido humano "vamos fazer isso" registrado para a ativação antes de 9.1.
Marco publicado: b886bdb8bb079eadcba2ed21c68739cf62ff3356, confirmado em origin.
A 8.2 está local: observer StartupEvent aciona o listener da entrada quando
monitoramento.service-bus.entrada.consumo-habilitado=true; default e %test=false.
O padrão funcional PT30M/PT24H e o baseline local original permanecem preservados.

RED confirmado por ausência do observer; 43 testes do listener e regressões focadas passaram.
Os dois casos novos de startup → POST → resultado no emulador passaram, incluindo reagendamento,
sem chamada manual ao listener. Revisões independentes de produção e integração sem findings.
Regressão completa: 23 integrações/6 classes e 1.297 testes padrão/189 classes, sem falhas,
erros ou ignorados. Build e Sonar COMPLIANT / NOT_REQUIRED: 87,9% cobertura,
4,3% duplicação, nenhuma issue nova/grave. Baseline integralmente idêntico.
Análise 4eb0718a-d90c-4155-8a7f-a5118f3d16c6; fingerprint 9b21bd4c4a82e385f1191e905f40a916e6f95055bb74a59f3385a8109d5fcd27.
Cópia final: `.codex/.state/session-after-8-2-checkpoint-20260910.json`.
Próximo item funcional: 9.1. Nenhum comando de trabalho ficou em execução.

[Execução e comando](guia-desenvolvimento.md#verificação-rápida-do-marco-até-82),
[plano](plan.md#82--ativação-controlada-da-entrada--2026-09-10) e
[evidências](continuidade-8-2.md). 9.1/10.1 não iniciadas; sem novo commit/push de 8.2.
As seções abaixo registram estados anteriores, inclusive a preparação anterior à publicação.

## Preparação do commit — 2026-09-10

O pedido posterior é preparar um commit seguro e esclarecer execução/testes/guias.
O [manifesto atual](pacote-commit.md) descreve os 42 arquivos de 7.1-B/C/D + 8.1
já selecionados no índice desde d83b689, com mensagem e roteiro de reprodução.
Índice/workspace conferidos e diff preparado sem erros; commit/push não executados.
Foram removidas somente linhas vazias finais de seis testes novos e da continuidade,
com novo checkpoint completo no mesmo baseline: 1.293 testes/189 classes, build e
COMPLIANT / NOT_REQUIRED, 87,9% cobertura, 4,3% duplicação, nenhuma issue nova/grave.
Análise `79215d12-7a59-4071-b60a-08d491e8815b`, fingerprint `3871c60389fe2d7425a3c22d8c74e0c14146088e4501d8f8ba61bacf7f1482f7`.
Cópia final de sessão: `.codex/.state/session-after-commit-preparation-20260910.json`. O patch temporário e os
artefatos locais ficam preservados fora do pacote.

Guia do dev e guia Service Bus explicitam 1.293 testes padrão, 21 integrações e início
obrigatoriamente explícito do listener. A prova do fluxo é a integração com emulador;
subir dev mode não ativa consumo. Código, baseline e checkpoint de 8.1 permanecem preservados.
O registro de fechamento abaixo descreve o estado anterior à preparação do índice.

## Ponto seguro — 8.1 concluída tecnicamente em 2026-09-10

Este registro documenta a conclusão de 8.1 antes da preparação do commit descrita acima.

- As cinco issues autorizadas foram corrigidas e confirmadas CLOSED/FIXED.
- Passaram **68 testes focados** e **1.293 testes padrão em 189 classes**, sem falhas,
  erros ou ignorados. Build e checkpoint completos concluídos.
- Sonar **COMPLIANT / NOT_REQUIRED**: cobertura **87,9%**, duplicação **4,3%**,
  213 issues abertas, nenhuma nova ou HIGH/BLOCKER/CRITICAL.
- As **21 integrações em cinco classes** passaram em 09/09; não foram repetidas para
  estas correções de sintaxe/testes, que preservam o comportamento.
- Revisão independente dos ajustes concluída sem apontamentos.
- Branch `feature/orquestrador-monitoramento-service-bus`; HEAD publicado
  `d83b6895a196718880e8e3d23519702f6eb77682`, até 7.1-A. Trabalho posterior segue local.
  Todos os arquivos, inclusive `.codex-doc-alignment.patch`, foram preservados.
- Baseline READY original de 217 issues recuperado após o hook apagar o estado de sessão,
  conferido integralmente idêntico; nenhuma reinicialização.
- Sem staging, commit ou push. Comandos de trabalho concluídos; serviços preexistentes preservados.
- **9.1 e 10.1 permanecem pendentes e não foram iniciadas.** Não houve encerramento humano da feature.

Evidências, IDs das análises e recuperação do baseline na
[continuidade de 8.1](continuidade-8-1.md).

### Próximo passo ao retomar

1. Conferir branch e alterações com `git status -sb`; preservar arquivos rastreados e não rastreados.
2. Ler continuidade, checklist, plano, guia do dev e referências exigidas por AGENTS.md.
3. Conferir `.codex/.state/session.json` antes de qualquer edição executável. O hook de
   abertura pode apagá-lo; a cópia final validada após 8.2 é
   `.codex/.state/session-after-8-2-checkpoint-20260910.json`.
   Usar essa evidência para recuperar o mesmo baseline, sem InitializeBaseline.
4. Próximo item funcional: **9.1 — consumo e log da saída**, a detalhar e executar em fatia própria.
   Os ajustes Sonar de 8.1 já terminaram; não repetir decisões humanas anteriores.

### Decisões que continuam válidas

Padrão v1 PT30M repetido até o prazo original, normalmente PT24H, sem max-tentativas
operacional. Outras políticas podem configurar máximo e intervalos como PT3H/PT4H/PT6H,
repetindo o último. Versão recebida configurada prevalece; versão removida usa v1 interna,
preservando versão, início e prazo recebidos. O agendamento é limitado ao prazo original.
Prazo vencido encerra por QUARENTENA/PRAZO_MAXIMO antes do Hub; máximo esgotado após consulta
não conclusiva encerra por QUARENTENA/MAXIMO_TENTATIVAS. Proteção contra overflow preservada.

Situações originais: FINALIZADO_CONFORME → CONFORME, FINALIZADO_INCONFORME → INCONFORME,
PENDENTE_INFORMACA → INCONFORME, preservando situacaoMtr.
Desde 8.2, o consumo pode ser ativado por configuração no startup ou iniciar() explícito.
O dev escolhe emulador ou Azure para a aplicação;
testes padrão continuam sem broker. Provas transacionais são locais no emulador.
Publicação terminal + Complete ainda não é atômica; não há Outbox/idempotência durável.

## Estado anterior — 7.1-D concluída; 7.1 encerrada tecnicamente

Base até 7.1-A publicada em d83b689; alterações posteriores de 7.1-B/C/D permanecem locais.
Integração terminal verificada com listener/caso de uso/ACL/publishers reais CDI e somente
a porta pública do Hub controlada. Situações originais, no-op, prazo, versão removida,
política inativa max=1, Abandon/redelivery e DLQ cobertos. Consumo automático segue inativo.

15 testes de integração/4 classes passaram no emulador, incluindo as seis provas anteriores.
Suíte padrão: 1.268 testes/187 classes sem broker. Sonar COMPLIANT / NOT_REQUIRED:
cobertura 87,8%, duplicação 4,3%, zero issues novas/graves. Baseline original integralmente
preservado. Evidências e IDs na [continuidade de 7.1](continuidade-7-1.md).

Próximo item funcional: **8.1 — reagendamento transacional**. Antes de implementar, detalhar
intervalo versus prazo original e contador no limite inteiro; provar schedule + Complete,
rollback e redelivery no SDK/emulador. Não ativar consumo geral antes da capacidade segura.
9.1 (consumo/log da saída) e 10.1 (telemetria) seguem pendentes; o demonstrador não está completo.

Preservar alterações rastreadas/não rastreadas, inclusive .codex-doc-alignment.patch e baseline.
Defaults PT30M repetido, prazo original, max-tentativas opcional nas outras políticas e
situações MTR permanecem preservados. O dev escolhe emulador ou Azure para a aplicação;
testes padrão continuam sem broker. Sem staging/commit/push adicional.
Nenhum comando desta execução ficou em andamento; containers temporários dos testes
encerrados, containers preexistentes preservados. Os estados abaixo são históricos.

## Estado anterior — 7.1-C tecnicamente concluída

Base até 7.1-A publicada em d83b689; 7.1-B/C concluídas localmente. Listener da entrada CDI
com início explícito, processamento serial, settlement único, logs mínimos e shutdown antes
da fábrica. Consumo automático permanece inativo; ReagendamentoPendente encerra a assinatura
sem settlement, aguardando 8.1. Publicação e Complete ainda não são atômicos.

Os 13 apontamentos java:S8924 MINOR foram corrigidos após ContinuarAjustes humano.
1.268 testes padrão/187 classes passaram sem broker; COMPLIANT / NOT_REQUIRED:
cobertura 87,8%, duplicação 4,3%, zero issues novas ou graves. Baseline READY original
integralmente preservado. Evidência e IDs do checkpoint na [continuidade de 7.1](continuidade-7-1.md).

Próxima fatia: **7.1-D**, integração terminal real e fechamento técnico de 7.1.
Não ativar consumo geral antes do reagendamento seguro de 8.1. Manter testes padrão sem
broker; o dev escolhe emulador ou filas Azure para a aplicação.
Preservar todas as alterações locais, inclusive arquivos não rastreados e o baseline.
Padrão PT30M repetido, prazo original e max-tentativas opcional nas outras políticas
permanecem preservados, assim como as situações originais do Hub.
Sem novo staging/commit/push; nenhum comando desta execução ficou em andamento.

## Estado anterior — 7.1-B tecnicamente concluída

Base até 7.1-A publicada em d83b689; 7.1-B concluída localmente após GO e ContinuarAjustes.
Caso de uso CDI aplica pré-validação, catálogo, limites, três situações originais do Hub e
publicação terminal confirmada. Versão ausente usa v1 interna PT30M, sem teto de tentativas
e sem renovar o prazo recebido; outras configurações mantêm max-tentativas opcional.

1.232 testes padrão/186 classes passaram sem broker. Checkpoint COMPLIANT / NOT_REQUIRED:
cobertura 87,6%, duplicação 4,3%, nenhuma issue nova ou grave. Duas S6878 corrigidas após
a decisão humana; baseline original integralmente preservado. Evidência e IDs na
[continuidade de 7.1](continuidade-7-1.md).

Próximo trabalho: detalhar e executar 7.1-C (listener, settlement e lifecycle), seguido da
integração terminal de 7.1-D. Consumo geral permanece inativo até a prova de reagendamento
de 8.1. ReagendamentoPendente é intenção, não confirmação de agendamento nem Complete.
Em 8.1, tratar intervalo que alcança/ultrapassa limiteEm e tentativaAtual no limite inteiro.

Preservar todas as alterações locais, o patch temporário e o baseline. Nenhum novo staging,
commit ou push; não reinicializar baseline, não repetir as decisões de versão/situação/padrão.
Testes padrão sem emulador; o dev escolhe emulador ou filas Azure para a aplicação.
Nenhum comando do agente ficou em execução. Os registros abaixo são históricos.

## Estado anterior — contrato e resolução de políticas de 7.1-B concluídos

Base até 7.1-A publicada em d83b689. O contrato aceita os nomes originais informados pelo usuário.
Decisão humana posterior: versão recebida configurada usa sua definição; versão ausente usa
v1 padrão, sem quarentena por esse motivo. A direção atual usa PT30M repetido até o prazo;
max-tentativas fica ausente no padrão e disponível para outras configurações.
Catálogo e producer CDI implementam essa resolução.
Preservar início/prazo/versão da tentativa; padrões não abrem uma nova janela de 24 horas.

1.200 testes padrão/185 classes e 96 testes focados passaram sem broker após o ajuste de PT30M.
Checkpoint COMPLIANT: cobertura 87,4%, duplicação 4,3%, nenhuma issue nova ou grave.
Baseline original integralmente preservado. Evidência em [continuidade de 7.1](continuidade-7-1.md).

Próximo trabalho: conectar catálogo e classificação ao caso de uso terminal de 7.1-B,
usando o GO vigente. A decisão de versão já foi dada; não reapresentar a pergunta.
Listeners e agendamento de 8.1 permanecem inativos. Alterações locais, sem novo
staging/commit/push; patch temporário preservado. Nenhum comando do agente ficou em execução.
Os registros abaixo representam a entrega publicada e as preparações anteriores.


## Material preparado para continuidade pelo dev — 2026-09-09

Atualizados o [pacote de commit até 7.1-A](pacote-commit.md) e o
[roteiro do desenvolvedor](guia-desenvolvimento.md#roteiro-para-assumir-a-entrega), incluindo
o guia Service Bus. Começar por esse roteiro e pela [continuidade de 7.1](continuidade-7-1.md):
C2 aceito, 7.1-A concluída, próxima subfatia 7.1-B após as definições pendentes.
O manifesto anterior até 4.1 foi atualizado; o histórico de execução permanece preservado.

Esta preparação altera somente Markdown. Não executa staging, commit, push, Maven ou Sonar
e não altera código, tooling, baseline ou formatos derivados. O dev escolhe emulador ou
filas Azure; a suíte padrão continua sem broker. O futuro hash de entrega deve corresponder
ao resultado real da publicação, sem inferência a partir destes registros.

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


## C2 aceito; explicação antes de 7.1 — 2026-09-09

C2-R1 foi corrigido após o pedido explícito do usuário. O profile de integração verifica as
fontes efetivas do Quarkus antes do bootstrap e mantém o emulador isolado de conexão externa.
11 regressões sem broker e quatro testes explícitos no emulador passaram; checkpoint final
registrado em [revisao-c2.md](revisao-c2.md). Baseline original e alterações anteriores
preservados, sem staging, commit ou push. O usuário declarou "c2 aceito" em 2026-09-09
e pediu a explicação do trabalho restante e sua estimativa antes de prosseguir à 7.1.
7.1 não iniciado. Estimativa preliminar: 12–20 horas de trabalho assistido, incluindo
testes/checkpoints e excluindo espera por decisões, conforme o
[plano](plan.md#aceite-c2-e-estimativa-do-trabalho-restante--2026-09-09).
Os registros de pausa e revisão abaixo são históricos; o aceite acima é o estado vigente.


## Pausa segura em 2026-09-08 — retomar em 2026-09-09

Pausa solicitada pelo usuário ao encerrar o dia. **Estado atual GREEN: 6.1 tecnicamente
concluído, testes passando e Sonar COMPLIANT.** O RED citado no histórico de 2026-09-07
não descreve o workspace atual.

- Mesma branch: `feature/orquestrador-monitoramento-service-bus`.
- 1155 testes padrão aprovados, sem emulador/fila real; quatro testes de integração explícita
  aprovados separadamente. Cobertura 87,4%, duplicação 4,3%, nenhuma issue nova ou grave.
- Baseline original preservado; último fingerprint:
  `37d69b34ee96e8dd3161397bb8e30c88e85ed3e8036669419b75358e7ad39fb2`.
- Alterações de 5.1/6.1 e documentação salvas localmente, **sem novo commit/push e sem staging**.
  HEAD e referência local do remoto permanecem em `266092f`. Preservar todos os arquivos
  rastreados e não rastreados, inclusive `.codex-doc-alignment.patch`.
- Comandos Maven/Sonar do agente concluídos; nenhum comando do agente mantido em segundo plano.
  O Java preexistente PID 325464, iniciado às 08:43:45, foi preservado. Não encerrar serviços
  ou processos preexistentes para fazer esta pausa.
- Não iniciar novos testes, servidores, análise, commit ou implementação durante a pausa.

### Como retomar amanhã

1. Abrir este mesmo repositório e conferir a branch/alterações com `git status -sb`.
   Não fazer reset, clean, stash ou trocar branch descartando o trabalho.
2. Ler este registro, [plano](plan.md), [checklist](todo.md),
   [continuidade de 6.1](continuidade-6-1.md) e
   [guia de desenvolvimento](guia-desenvolvimento.md).
   Consultar arquitetura e ADRs conforme AGENTS.md.
3. **Retomar pela revisão humana C2**, usando a evidência final de 6.1. C2 não foi aceito
   nem inferido pelo agente. O próximo item funcional é 7.1, ainda não iniciado.
4. Antes de implementar a classificação em 7.1, confirmar a tabela oficial de id/nome do Hub
   para códigos conclusivos e definir tratamento da versão de política. Não inventar a
   correspondência da fixture `1 / Rascunho`. Detalhar a próxima fatia e obter o GO aplicável.
5. Se houver continuidade executável, usar a sessão iniciada pelo launcher
   `./iniciar-codex-com-sonar.ps1`, com token somente em memória. Reutilizar o baseline;
   não executar InitializeBaseline nem repetir análise completa apenas por reabrir a sessão.
6. Preservar a escolha do dev: aplicação no emulador ou nas filas Azure por configuração.
   `mvn test` permanece sem broker; emulador somente na integração explícita
   `mvn -Pservicebus-integration test`.

Texto para iniciar a conversa:

> Retomar tasks/features/orquestrador-monitoramento-service-bus/retomada.md.
> Preservar a branch e todas as alterações locais. O item 6.1 está concluído, com testes
> passando e Sonar COMPLIANT. Retomar pela revisão C2, sem iniciar 7.1 antes das decisões
> pendentes. Testes unitários continuam sem emulador; o dev escolhe emulador ou fila Azure.

**Continuidade atual:** comportamento de 6.1 implementado conforme o GO humano:
REST → parâmetros locais pela ACL/política → publicação inicial confirmada. Testes padrão
sem emulador nem fila Azure; integração com emulador somente por profile explícito.
Item 6.1 tecnicamente concluído após o ajuste Sonar autorizado por ContinuarAjustes.
C2 e 7.1 permanecem pendentes. Ver [continuidade de 6.1](continuidade-6-1.md).

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

## Registro histórico da pausa em 2026-09-07

O texto abaixo descreve o ponto de partida anterior à implementação de 2026-09-08.
As menções a RED, próxima subfatia e checkpoint de 859 testes são históricas.

Pausa solicitada pelo usuário em 2026-09-07, para continuar em 2026-09-08.
Branch: `feature/orquestrador-monitoramento-service-bus`. Nenhum commit ou staging realizado.

### Estado da pausa histórica de 2026-09-07

Todas as alterações rastreadas e não rastreadas estão preservadas no workspace.
Não executar reset, clean, checkout destrutivo ou stash para reconstruir um estado anterior.
Os comandos iniciados pelo agente terminaram; não há agente auxiliar trabalhando.
Não iniciar tarefa em segundo plano durante a pausa.

**O workspace está em RED de compilação conhecido, não em GREEN.** Dois testes novos e uma
fixture de reagendamento exigem uma API ainda ausente. Não apagar esses arquivos nem atribuir
ao estado atual os 859 testes do checkpoint anterior.

### Leitura registrada na pausa histórica

1. `AGENTS.md`, arquitetura consolidada e índice de ADRs, respeitando o procedimento do projeto.
2. [Guia Service Bus corrigido](../../../doc/guias/guia-service-bus-amqp-dossie.md).
3. [Guia de desenvolvimento e inventário Java](guia-desenvolvimento.md).
4. [Plano](plan.md) e [checklist](todo.md), principalmente o estado atual e as últimas evidências.
5. [Manifesto de commit](pacote-commit.md), ainda somente proposta de revisão.

As decisões vigentes são ADRs 0010, 0011 e 0012; o ADR-0009 é histórico/substituído.
Não reabrir escolhas já aprovadas nem inferir novas decisões humanas.

## Decisões a preservar

- Orquestrador publica na fila de entrada; monitoramento consome por listener, aplica critérios
  pela aplicação/política e reagenda na entrada ou publica resultado na saída.
- Orquestrador consome a saída e encerra o demonstrador registrando log.
- Packages `br.gov.caixa.simtr.orquestrador` e `br.gov.caixa.simtr.monitoramento`.
  Não alterar `br.gov.caixa.simtr.dossie`, Hub, seus contratos, código, logs ou erros.
- Extensão Quarkus Azure Service Bus `1.2.5`, Quarkus `3.33.2.1`, JDK 25,
  connection string/SAS; não usar Entra ID nesta feature.
- Emulador por Dev Services, simuladores preservados e arquivo em
  `src/main/azure/servicebus-emulator/config.json`.
- DTOs/mappers independentes por borda. Erros novos em JSON tipado aprovado, diagnóstico
  sanitizado, identidade/código propagados em RuntimeException própria.
- Permanecer exclusivamente em 4.1. Não reiniciar itens concluídos nem avançar para 5.1.

## O que ficou concluído

Base de extensão/configuração/Dev Services; política e producer CDI; contratos/mappers REST
e da entrada; validação/compatibilidade e logs JSON das duas bordas da entrada.
Estrutura antecipada: 40 tipos, incluindo 11 portas, dois records de parâmetros e 27 classes
inativas. Guia de desenvolvimento e manifesto de commit preparados.

O guia técnico antigo `doc/guias/guia-service-bus-amqp-dossie.md` foi reescrito conforme
o fluxo e a plataforma vigentes. Plano, checklist, ADRs inconsistentes e referências foram
alinhados. Javadocs de 20 tipos, incluindo 11 métodos, corrigidos/complementados.
213 links locais verificados; DocLint sem erros, com 9 avisos de construtores implícitos.
Código fora dos Javadocs preservado. Essa revisão não executou Maven/Sonar nem gerou derivados.

## Próximo passo funcional exato

Completar o GREEN da subfatia DTO/mapper de reagendamento e erro JSON em 4.1.
O RED já foi executado com:

```powershell
mvn -q "-Dtest=MonitoramentoReagendamentoContratoTest,MonitoramentoReagendamentoLogTest" test
```

O comando terminou com falha de compilação esperada por API ausente. Foram preservados:

- `src/test/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/servicebus/MonitoramentoReagendamentoContratoTest.java`;
- `src/test/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/servicebus/MonitoramentoReagendamentoLogTest.java`;
- `src/test/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/servicebus/ReagendamentoFixture.java`.

Os dois arquivos de produção continuam como esqueletos:
`monitoramento/adaptador/saida/servicebus/MonitoramentoReagendamentoServiceBusMapper.java`
e `monitoramento/adaptador/saida/servicebus/dto/MonitorarDossieMtrV1.java`, sob
`src/main/java/br/gov/caixa/simtr/`.

Os testes esperam mapper com `paraMensagem(TentativaMonitoramento)`, validação/serialização
próprias e `MapeamentoReagendamentoException extends RuntimeException`. A tentativa de criar
a produção foi interrompida antes de gravar novos arquivos. Conferir o código antes de editar;
não presumir que rascunhos de ferramentas tenham sido aplicados.

Montar JSON/envelope compatível, preservando IDs, tentativa recebida, início, limite e versão.
Não publicar, calcular a próxima tentativa ou agendar de fato: esses efeitos permanecem nos
itens posteriores. Aplicar o log JSON previsto no plano. Depois do GREEN, atualizar Javadoc,
inventário de esqueletos, guia e checklist; executar regressão focada e checkpoint do incremento.
Contratos de resultado e guardrails restantes continuam pendentes em 4.1.

## Baseline e evidência técnica anterior

Preservar o baseline já escolhido/inicializado; **não executar `-InitializeBaseline`**.
O estado existente fica em `.codex/.state/session.json`; a referência recuperada está em
`.codex/.state/session-restored-s7467-20260907.json`. Conferir preservação antes de retomar
alteração executável, sem recriar baseline ou retirar código do workspace.

Último checkpoint completo registrado: `COMPLIANT`, 859 testes, cobertura 86,4%, duplicação
3,7%, 213 issues, nenhuma nova ou HIGH/BLOCKER/CRITICAL, em
`2026-09-07T17:13:24.4462907-03:00`.
Esse checkpoint antecede os complementos Javadoc e os novos testes em RED.
Ainda não existe checkpoint verde para o workspace atual.

Para nova sessão de código, seguir o launcher `iniciar-codex-com-sonar.ps1` e a credencial
herdada em memória conforme AGENTS.md. Nunca solicitar ou copiar token no chat, arquivos,
argumentos ou logs. Se houver indisponibilidade, registrar a limitação; não inferir aprovação.
A pausa de hoje é apenas documental e não reinspeciona credencial/baseline nem executa Sonar.

## Preparação da classificação em 7.1 — 2026-09-09

O usuário informou o mapeamento por nomes do Hub, preservando a situação original do MTR.
Direção literal, confirmações pendentes e impacto nas duas bordas do contrato registrados no
[plano](plan.md#direção-humana-para-situações-do-hub--2026-09-09).
