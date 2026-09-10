# Continuidade de 8.2 — ativação controlada da entrada

## Autorização e ponto de partida — 2026-09-10

O usuário confirmou "vamos fazer isso" para a fatia proposta antes de 9.1.
[Plano](plan.md#82--ativação-controlada-da-entrada--2026-09-10) registrado antes de produção.
Branch feature/orquestrador-monitoramento-service-bus; marco b886bdb publicado e confirmado
no remoto. Patch temporário preservado. Baseline READY/LOCAL_SONAR original de 217 issues,
análise f6183a72-a2ea-44bc-9374-b2b064bdad55, mantido sem InitializeBaseline.
Checkpoint anterior: 79215d12-7a59-4071-b60a-08d491e8815b, COMPLIANT / NOT_REQUIRED,
1.293 testes/189 classes, 87,9% cobertura e 4,3% duplicação. Isso é evidência anterior.

## Execução

- RED: mvn -q "-Dtest=MonitoramentoEntradaListenerTest" test falhou por ausência de
  iniciarNoStartup(StartupEvent, boolean), antes da edição de produção.
- GREEN: observer no próprio listener e propriedade runtime explícita; default e %test=false.
  mvn -q "-Dtest=MonitoramentoEntradaListenerTest,MonitoramentoEntradaLogTest,ClientesServiceBusTest" test
  terminou com exit 0; listener com 43 testes, sem falhas/erros/ignorados.
- Prova focada: mvn -q -Pservicebus-integration "-Dtest=MonitoramentoAtivacaoEmuladorTest" test
  terminou com exit 0; dois casos sem falhas/erros/ignorados em 2026-09-10.
  O override de profile venceu %test=false e o bootstrap assinou o receiver sem chamada
  Java manual. POST real e cenário de segunda consulta após reagendamento foram verificados.
- Revisão independente da produção e do teste novo concluída sem findings/bloqueadores.
- Regressão completa: mvn -q -Pservicebus-integration clean test terminou com exit 0,
  23 testes em seis classes, sem falhas/erros/ignorados, em 2026-09-10.
  Infraestrutura 1; entrada REST/publicação 3; saída/publicação 2; transação 3;
  terminal/reagendamento 12; ativação por startup 2.
- Conferência documental: oito documentos, 249 links locais e 30 âncoras válidos;
  diff e whitespace dos arquivos novos sem erros.
- Checkpoint completo atual concluído no mesmo baseline; métricas e IDs no fechamento abaixo.

O novo observer não muda classificação, settlement, retry ou fechamento. Erro assíncrono
continua encerrando o consumidor sem derrubar HTTP; requer reinício da aplicação.
O profile opt-in rejeita configurações externas do Service Bus antes do bootstrap.
O harness usa peek na entrada e recebe/confirma somente a saída, que ainda não tem listener.
Guia do dev e guia Service Bus incluem comando completo, request e limites. Dev mode
interativo e Azure gerenciado não foram executados. 9.1/10.1 não iniciadas.

## Fechamento técnico — 2026-09-10

Checkpoint completo concluído em 2026-09-10T12:37:20.6956985+00:00 (09:37:20 -03:00).
**1.297 testes em 189 classes**, sem falhas, erros ou ignorados e nenhuma classe de integração
na suíte padrão; build aprovado. **23 integrações em seis classes** passaram separadamente
nesta mesma data. A ativação por startup foi provada em dois desses casos, sem chamar iniciar()
no teste. Os quatro novos testes sem broker integram os 43 casos do listener.

Sonar **COMPLIANT / NOT_REQUIRED**: cobertura **87,9%**, duplicação **4,3%**,
213 issues abertas, nenhuma nova ou HIGH/BLOCKER/CRITICAL.
Análise: `4eb0718a-d90c-4155-8a7f-a5118f3d16c6`.
Compute Engine: `3935ac03-dee8-4462-9528-82040065a932`.
Fingerprint: `9b21bd4c4a82e385f1191e905f40a916e6f95055bb74a59f3385a8109d5fcd27`.
Baseline e baselineAssessment comparados integralmente com o estado anterior: idênticos.
Não houve InitializeBaseline nem nova decisão Sonar necessária.
Cópia completa da sessão final: `.codex/.state/session-after-8-2-checkpoint-20260910.json`;
cópias anteriores preservadas fora do Git.

Revisões de produção e integração sem findings. A revisão documental identificou referências
antigas ao início manual em fluxos/diagramas e à contagem operacional 21/5; corrigidas para
opt-in de startup e 23/6, preservando os marcos históricos. Documentação verificada com links/âncoras
e diff sem erros; código permanece igual ao fingerprint analisado.
8.2 concluída tecnicamente. Próximo item funcional: 9.1, consumo/log da saída, ainda não iniciado.
Não houve staging, novo commit/push ou encerramento humano da feature nesta fatia.
Comandos de trabalho concluídos; serviços preexistentes e patch temporário preservados.

## Consolidação documental e preparação da publicação — 2026-09-10

Após a leitura inicial de 9.1, o usuário pediu consolidar toda a entrega, atualizar o guia
para retomada, organizar commit e publicar antes de continuar. Nenhum código/teste/configuração
da etapa 9 foi alterado. A consolidação preserva o executável analisado no fechamento acima.

Revisão independente confirmou ausência de novo bloqueador na produção de 8.2 e identificou
o exemplo manual incorreto: 0007 era usado apenas na prova com Hub controlado, enquanto o
simulador dev procura uma fixture pelo ID e só possui 4324680/Rascunho. O guia foi corrigido,
incluindo comandos Dev Services/Azure completos, POST, separação dos simuladores e política curta
opcional. O 202 confirma publicação inicial; a saída não possui consumidor/log de produção.

O consolidado arquitetural foi alinhado ao estado implementado dos packages irmãos sem mudar
as capacidades do Hub. O manifesto histórico de 42 arquivos de 8.1 foi separado; o pacote atual
reúne 13 arquivos. A pendência de compatibilidade do adapter de log com as categorias do
formatter foi registrada para 9.1, sem implementar solução ou alterar ADRs.

Fingerprint reconferido: 9b21bd4c4a82e385f1191e905f40a916e6f95055bb74a59f3385a8109d5fcd27,
igual ao checkpoint COMPLIANT. Evidências Maven/Sonar acima continuam válidas para o código
selecionado. Não houve nova execução de dev mode, Azure real ou análise completa nesta
consolidação exclusivamente documental. Commit e push normais foram autorizados pelo usuário.
A conferência documental passou com nove documentos, 263 links locais e 31 âncoras válidos,
diff e whitespace sem erros. A revisão final apontou duas frases antigas no guia Service Bus
sobre reagendamento pendente; ambas foram alinhadas à implementação e prova de 8.1.
