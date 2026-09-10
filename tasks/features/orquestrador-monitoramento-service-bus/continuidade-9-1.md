# Continuidade de 9.1 — consumo da saída e log final

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

## Verificação de 9.1-A — 2026-09-10

Os 42 testes focados passaram após corrigir a leitura dos campos em mdc.
O checkpoint completo executou clean verify, SonarScanner e Compute Engine:
1.307 testes padrão em 191 classes, zero falhas/erros/ignorados e nenhuma classe de
integração com broker. Build SUCCESS; cobertura 88,0%, duplicação 4,3%, 214 issues abertas.

Situação NON_COMPLIANT / decisão PENDING por uma issue nova, sem issue HIGH/BLOCKER/CRITICAL:
java:S1117, severidade legada MAJOR/impacto MEDIUM, em ResultadoMonitoramentoLogAdapterTest:156.
A variável local registro oculta o campo da classe; correção proposta: registroJson,
sem alteração de comportamento. Chave 4682c0b3-5b05-4a80-9813-9e8e836a86d1.
Decisão solicitada ao usuário conforme AGENTS.md: ContinuarAjustes, AceitarExcepcionalmente
ou Reprovar. Não registrar resposta por suposição; não avançar à 9.1-B antes deste fechamento.

Análise 66388963-15c3-4f03-90d6-89522b86c436; CE c11976c1-5033-43d5-bbf3-f8ec783d5d5d;
horário 2026-09-10T13:01:32.8311392-03:00; fingerprint
8c1658d23688371eb04e5a78c694fef3397a04d9763be3b92cc214360456c7f9.
Baseline e baselineAssessment comparados integralmente à cópia de 8.2 e idênticos.
Documentação alinhada em sete Markdown: 256 links locais e 28 âncoras válidos; diff e
whitespace dos arquivos novos aprovados. Fingerprint após documentação idêntico ao checkpoint.
Cópia de retomada: .codex/.state/session-after-9-1-a-checkpoint-20260910.json.
Todos os comandos do agente concluídos; nenhum consumidor novo ativado.
Integrações não repetidas nesta subfatia sem consumo: as 23 provas publicadas de 8.2 são
evidência anterior, não prova do futuro listener da saída. Sem novo commit/push.

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

Os registros de investigação abaixo descrevem o estado anterior ao aceite.

## GO e ponto de partida — 2026-09-10

O usuário autorizou "go para prosseguirmos" após confirmar commit/push do marco 8.2.
HEAD d5df6b6e309842460427df290895f5064d00b5d1, branch
feature/orquestrador-monitoramento-service-bus, remoto conferido na publicação.
Patch temporário preservado. Baseline READY/LOCAL_SONAR original idêntico à cópia de 8.2.
Checkpoint preservado COMPLIANT / NOT_REQUIRED, análise
4eb0718a-d90c-4155-8a7f-a5118f3d16c6; 1.297 testes padrão e 23 integrações são evidência
do marco 8.2, não validação da implementação ainda inexistente de 9.1.

## Contrato e limitação real do logger

O plano exige registro concluído antes de Complete e Abandon quando a porta de registro falhar.
As fontes efetivas de jboss-logmanager-3.2.1.Final-sources.jar mostram:

- LoggerNode.publish captura falhas de Handler.publish e chama ErrorManager.
- ExtHandler.publish captura falhas de doPublish e chama reportError.
- Logger.logRaw pode filtrar um registro e retornar normalmente.

Portanto, sucesso da chamada ao logger não comprova escrita bem-sucedida no destino.
A revisão independente chegou à mesma conclusão: percorrer handlers, trocar ErrorManager
ou acrescentar callbacks seria um mecanismo novo, afetaria infraestrutura compartilhada e
ainda precisaria tratar concorrência, filtros e destinos assíncronos.

A hipótese inicial de flat-fields não se confirmou no teste Quarkus: os campos ficam em mdc,
como registrado na seção atual. A categoria própria do adapter de log pode ser mantida,
preservando o Hub e os limites do ADR-0012.
Referência: [logging Quarkus 3.33](https://quarkus.io/version/3.33/guides/logging/).
As APIs de captura de falha foram confirmadas nas fontes da versão local efetiva.

## Resultado da prova isolada — 2026-09-10

Executada com exit 0 no Java instalado e classpath efetivo da suíte, em processo separado:
`java @target/provas-9-1/java-args.txt`.
Fonte temporária: `target/provas-9-1/ProvaLoggerSaida.java` (ignorada pelo Git).

```text
LOG_RETORNOU_NORMALMENTE=true; HANDLER_FALHOU=1; ESCRITA_CONCLUIDA=0
LOG_FILTRADO_RETORNOU_NORMALMENTE=true; NOVAS_ENTREGAS_AO_HANDLER=0
```

O handler de prova lança uma falha controlada; o ErrorManager conta essa falha. O logger real
retorna normalmente. A segunda chamada é filtrada e também retorna normalmente sem entregar
o registro ao handler. Isso confirma a ausência de confirmação do destino nessa API; não é
prova de I/O real, persistência ou settlement no broker. Nenhum cliente Service Bus foi usado.
A primeira tentativa da prova não iniciou a classe por composição incorreta do arquivo de
argumentos; o arquivo foi corrigido e a execução válida acima terminou sem erro.

**Checkpoint solicitado:** aprovar, para este demonstrador, Complete após submissão ao logger
padrão, aceitando que falhas/filtros internos dos destinos não provocarão Abandon. A alternativa
é exigir confirmação de destino e planejar mecanismo próprio antes de conectar o consumo.
A decisão humana ainda não foi recebida; não foi registrada aceitação por suposição.

## Prova isolada e checkpoint C9.1-L

Prova planejada em target/provas-9-1: logger real, handler que falha de forma controlada e
ErrorManager que conta a falha. Verificar se log retorna normalmente apesar da falha do handler.
Processo Java separado, sem broker, sem alterar os handlers do runtime da aplicação.
Não é teste adicionado à suíte padrão nem prova de settlement no Service Bus.

**Proposta para decisão humana:** aceitar logging best-effort no demonstrador. A porta
confirma submissão ao pipeline, e o listener pode dar Complete depois dessa conclusão.
Falhas internas anteriores à submissão ainda falham o Uni e permitem Abandon; falhas,
filtros ou descartes nos destinos não podem ser traduzidos em Abandon por essa API.
Redelivery pode repetir logs. Se confirmação do destino for obrigatória, definir escopo
e arquitetura próprios para um mecanismo confirmável antes da implementação.

**Estado:** C9.1-L pendente; nenhuma alteração de produção, teste da suíte ou configuração
foi realizada. Não houve decisão humana de aceitar o limite, novo checkpoint Sonar,
novo commit/push ou avanço para 10.1. Plano e checklist registram a investigação atual.
