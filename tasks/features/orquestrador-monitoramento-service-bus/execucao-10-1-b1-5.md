# Execução de 10.1-B1.5 — prova POST até broker

## Início — 2026-09-11

Pedido humano **10.1-B1.5**, execução somente do próximo item sob o GO do desenho/CP-B1.
Branch feature/orquestrador-monitoramento-service-bus; HEAD 1f32466 e alterações
locais anteriores preservados. Sem staging/commit/push ou formatos derivados.

Baseline LOCAL_SONAR e assessment comparados ao snapshot B1.4, íntegros.
Checkpoint inicial COMPLIANT / NOT_REQUIRED, 213 issues / baseline 217, nenhuma nova/grave;
88,3% cobertura, 4,4% duplicação e 1.403 testes padrão/194 classes aprovados.
Fingerprint inicial b77639406f1d5e434ce682712060a635b3a770e670c11f843fe88580d331939d.
Sem pasta sonar/; credencial herdada disponível sem exposição. Não reinicializar baseline.

## Recorte e critérios concretos

Um único arquivo executável novo:
src/test/java/br/gov/caixa/simtr/orquestrador/integracao/MonitoramentoInicioTelemetriaEmuladorTest.java.
Profile próprio reutiliza integralmente a proteção ServiceBusEmuladorTestProfile e
desabilita os dois listeners; JSON real em arquivo dedicado. Não altera produção,
mapper/contrato, dependências, configuração operacional nem os cenários/suporte A2.

- POST real com parent W3C sintético, tracestate e sentinelas de negócio/baggage.
- 202 com somente os dois UUIDs técnicos; mensagem própria disponível no broker.
- Peek com sequência inicial explícita 0 e quantidade exata; conferir IDs/correlation
  e sequência antes de Complete. Receber e concluir somente a própria mensagem.
- Filas vazias antes/depois; sem purge ou consumidor adicional. Se surgir mensagem
  alheia, falhar antes de seu settlement. Não usar Complete como limpeza indiscriminada.
- Exporter CDI com controle positivo gravável/exportado e forceFlush; capturar toda
  a operação, inclusive receive/Complete/peek de limpeza, antes de selecionar spans.
- Inventariar todos os spans/logs capturados (metadados sanitizados, sem payload)
  antes das asserções de seleção; total exato de três spans SERVER → INTERNAL →
  PRODUCER, um evento confirmado e nenhum evento adicional das famílias do fluxo.
- Verificar JSON/envelope AMQP existentes, carrier W3C exato do PRODUCER, par do log,
  campos/tipos/chaves permitidos, atributos/status/lifecycle e ausência de sentinelas.
- Regressão opt-in completa inclui A1/A2. Depois, revisão e checkpoint Sonar completo.

A prova inicia pelo contrato real HTTP/broker e cresce para a captura/asserções de
telemetria. O comportamento já foi implementado em B1.2–B1.4; não inventar RED de
produção correta. Qualquer defeito encontrado será reproduzido antes de corrigir,
respeitando o recorte e os checkpoints.

## Referências e limites

Arquitetura atual, índice e decisões aplicáveis (0006/0010/0011/0012) já lidos nesta
continuidade; contratos, profile seguro, fábrica e testes A1/A2 confirmados no código.
[Azure Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/index.html) e
[Service Bus Extension](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html)
consultados. Versões preservadas: Quarkus 3.33.2.1, extensão 1.2.5, SDK 7.17.12.
SDK async/peek/settlement seguem os padrões já caracterizados em A1.

O emulador e as fixtures locais não comprovam Azure gerenciado nem MTR externo.
B1.5 conclui tecnicamente o trecho inicial B1, se todas as verificações passarem;
B2–B5, 10.1-C/C3 e encerramento humano da feature permanecem pendentes.
Divergência documental preexistente sobre listener futuro continua registrada na A2;
não será corrigida fora do escopo.

## Estado

Pausada por solicitação humana em 2026-09-11; B1.5 permanece em andamento.

- Primeira versão HTTP/broker: 1 teste aprovado, sem erros/falhas/ignorados (29,16 s).
- Segunda versão, com telemetria: Maven terminou com exit code 1; 1 teste, 0 falhas,
  1 erro, 0 ignorados (68,24 s). Timeout de 40 s no peek da entrada após Complete,
  linha 91 → exigirFilaVazia:232. A causa permanece em investigação.
- Evidências preservadas em target/b15-inicial.log, target/b15-telemetria.log e
  target/surefire-reports/br.gov.caixa.simtr.orquestrador.integracao.MonitoramentoInicioTelemetriaEmuladorTest.txt.
  Arquivos em target são temporários; esta descrição preserva o resultado da pausa.
- Nenhum processo Java encontrado na conferência da pausa. Não há Maven pendente.
- Não houve novo checkpoint B1.5. O COMPLIANT anterior pertence exclusivamente à B1.4.

Próxima ação: reproduzir/localizar o timeout pós-settlement no teste, sem mudar produção
nem concluir mensagens cuja identidade não esteja comprovada. A revisão parcial
sugeriu Complete por sequência mesmo com divergência de IDs; essa sugestão não foi
adotada, pois contraria o critério aprovado de identidade. Se divergir, falhar sem
settlement e registrar possível resíduo como limitação; não limpar mensagem alheia.

Depois do diagnóstico, completar a prova: nomes/tipos exatos do JSON e envelope,
datas/política, schema permitido do log, igualdade peek/recebida e marcação VALIDADO
somente após todas as asserções. Inventariar a captura inteira após a limpeza e antes
da seleção dos spans. Não introduzir contexto artificial para obter correlação.
A janela inclui POST/peek/receive/Complete/peeks finais, mas não shutdown do profile.

Executar a suíte opt-in completa (incluindo A1/A2), revisão independente final e
checkpoint Sonar. Só depois consolidar tecnicamente B1 e apontar o desenho B2.
Sem staging/commit/push; alterações anteriores, .env e patch local preservados.

## Retomada e diagnóstico — 2026-09-12

Pedido humano: retomar B1.5 pelo timeout no peek após Complete, preservando alterações
locais e sem commit/push. Escopo executável mantido no novo teste B1.5.

O hook sonar-session-start.ps1 regravou session.json com baseline/assessment nulos.
Antes da edição, preservado esse estado em
.codex/.state/session-before-10-1-b1-5-recovery-20260912.json e recuperado integralmente
o snapshot session-after-10-1-b1-4-compliant-20260911.json. Igualdade byte a byte
conferida; READY/LOCAL_SONAR, baseline e checkpoint históricos preservados.
Não houve InitializeBaseline; token herdado disponível, sem exposição. Não existe sonar/.
A falha recorrente de inicialização do sandbox exigiu execução escalada das leituras,
edições e comandos; não houve alteração do sandbox nem dos hooks.

### Reprodução e ajuste do harness

1. Teste original, sem alterações: 1 aprovado em 35,42 s.
2. Dez repetições temporárias da mesma prova: 10 testes, 0 falhas, 1 erro, 0 ignorados,
   87,09 s. A primeira repetição reproduziu o timeout de 40 s no peek pós-Complete;
   as nove seguintes passaram. Evidência: target/b15-repeticoes-diagnostico.log.
3. Ajuste mínimo: dentro de concatMap, manter identidade/igualdade antes de Complete,
   aguardar Complete e os peeks de entrada/saída antes de emitir para next().
   O helper de peek é adiado com Mono.defer, sem aumentar os 40 s, retry, sleep,
   novo cliente ou consumidor. Não alterar produção nem concluir mensagem alheia.
4. Dez repetições após ajuste: 10 aprovadas, 0 falhas/erros/ignorados, 28,05 s.
   Evidência: target/b15-lifecycle-green.log.
5. Restaurado @Test permanente e completados nomes/tipos do response e JSON, envelope,
   datas/política, igualdade peek/recebida, carrier exato, atributos HTTP e schema do log.
   Inventário inicia INICIADO, grava CAPTURADO antes da seleção e só marca VALIDADO
   após todas as asserções. Prova final: 1 aprovado, 36,32 s,
   target/b15-prova-final.log.

SDK local azure-messaging-servicebus 7.17.12:
ServiceBusReceiverAsyncClient.java, linhas 894–906, passa getLinkName ao peek de management;
1792–1800 obtém esse nome do consumer; 935–981 documenta stream infinito e cancelamento.
azure-core-amqp 2.10.0, MessageFlux.java 357–375, cancela o upstream e congela o mediator.
Os sources.jar instalados foram consultados, sem alterar dependências.

O log da falha original mostra cancelamento às 21:26:38.040, espera por atualização
pendente às .041 e fechamento remoto às .046, imediatamente antes do peek que expirou.
A hipótese sustentada por fontes/logs é a corrida entre teardown do receive link e
peek usando o link associado. Reordenar o harness elimina essa janela. As evidências
não identificam conclusivamente o estado interno do emulador que reteve a chamada
por 40 s e não validam o serviço Azure gerenciado.

### Revisão e limites atuais

Revisão independente confirmou a ordem Complete → peeks → emissão/cancelamento,
com identidade antes de qualquer settlement. A sugestão preliminar de completar
depois de next() foi descartada; violaria o lifecycle já caracterizado.

Revisão final apontou reforço necessário na seleção de logs: considerar message e evento,
incluindo a família monitoramento.*, para impedir evento adicional fora da contagem.
A correção e a verificação final desse ponto ainda estão em andamento.

A suíte opt-in completa foi iniciada. Encontrou erro de ACK por fechamento do receive
link em ServiceBusDevServicesTest, arquivo preexistente e não alterado em B1.5.
Registrar como risco da regressão; não corrigir silenciosamente fora do recorte.
Resultado final da suíte e checkpoint B1.5 ainda pendentes neste registro.

Na resposta sobre observabilidade, foi esclarecido ao usuário que somente POST →
iniciação → publicação inicial possui cadeia correlacionada validada. B2–B5 continuam
pendentes; consultas Hub ainda abrem traces separados e log final não tem trace/span.
O profile jaeger configura exportação, desabilitada por padrão; esta prova usa exporter
em memória e não comprova visualização real no Jaeger.

### Resultado da regressão e revisão final — 2026-09-12

A suíte ampla executou 41 testes em 11 classes: 0 falhas, 1 erro, 0 ignorados.
A1, A2 e B1.5 passaram nessa execução. O erro foi exclusivamente
ServiceBusDevServicesTest.assertFilaDisponivel:70, que recebe via next().block()
e tenta Complete depois do cancelamento. O arquivo não foi alterado por B1.5.
A repetição focada passou (1 teste, 28,06 s), mas não elimina essa corrida preexistente.

O filtro de logs da B1.5 foi reforçado para verificar message e evento nas quatro
famílias: orquestrador.*, monitoramento.*, doctree.* e simtr-hub.*. Continua exigindo
exatamente um evento e seu schema completo. Prova repetida após o ajuste:
1 teste aprovado, 32,61 s; junto da repetição DevServices, Maven terminou com exit code 0.

Inventário final B1.5 VALIDADO: 3 spans, 18 registros totais na janela, exatamente
1 confirmação inicial. Os demais 17 são registros técnicos SDK/AMQP; todos foram
inventariados por logger/chaves antes da seleção. O total de logs técnicos pode variar
com abertura/fechamento assíncronos de links; não é contagem contratual do fluxo.
Carrier contém somente traceparent/tracestate do PRODUCER; filas vazias comprovadas
após Complete e antes de cancelar o consumo. A janela termina na captura/forceFlush
após o retorno do receive; não abrange shutdown nem promete aguardar todo log tardio
de teardown do SDK.

Cópias preservadas antes do clean em .codex/.state/b15-evidencias-20260912:
inicio.json, terminal.json, hub-real.json e relatórios textuais das duas classes repetidas.
Logs brutos em target foram transitórios; os resultados relevantes estão registrados aqui.

Checkpoint completo iniciado com baseline original. Não declarar que a execução ampla
passou sem erros ou que a repetição corrigiu ServiceBusDevServicesTest. A correção desse
harness fica como tarefa explícita de estabilização da regressão, fora do arquivo B1.5.
B2–B5 e validação real do trace completo no Jaeger continuam pendentes.

## Checkpoint final e pendência explícita — 2026-09-12

./validar-checkpoint-sonarqube.ps1 concluiu Maven clean verify, scanner e Compute Engine.
1.403 testes padrão em 194 classes, zero falhas/erros/ignorados; build SUCCESS.
Situação COMPLIANT / NOT_REQUIRED: 213 issues frente ao baseline original de 217,
zero novas ou HIGH/BLOCKER/CRITICAL, cobertura 88,3%, duplicação 4,4%.

- Analysis key: b3e9f089-c04f-4ed8-8d57-0096cc2d8b56.
- Compute Engine: 890ac9ed-7c12-4da9-90e1-6f15f5626517.
- Fingerprint: 82859a92070f0c9da1b8908bbf8ad5e996405ea2a98ad3d0f5e74bc65f1ea1a2.
- Snapshot final: .codex/.state/session-after-10-1-b1-5-compliant-20260912.json.

Revisão independente final confirmou o filtro corrigido e não deixou achados
obrigatórios no arquivo B1.5. Produção, A1/A2 e o teste DevServices não mudaram
nesta retomada. Permanecem preservados .env, patch local e alterações anteriores.

**Prova B1.5 validada; consolidação B1 pendente da estabilização da regressão.**
O rerun aprovado de ServiceBusDevServicesTest não constitui correção da corrida.
Próxima tarefa proposta no checklist: delimitar essa correção no harness preexistente,
mantendo a validação de messageId e Complete dentro de concatMap, com thenReturn
apenas após ACK e next() por último. Não receber/cancelar antes de fazer Complete.
Não precisa alterar produção, clientes operacionais, timeout ou dependências.
A ampliação para esse segundo arquivo não foi implementada nesta retomada.

O checkpoint padrão não inclui os testes opt-in e não substitui a evidência ampla
41/0/1/0 mais as duas repetições aprovadas descritas acima. Não há aceite excepcional
ou encerramento humano inferidos. B2–B5, 10.1-C/C3 e prova visual Jaeger pendentes.

## Complemento final da regressão e consolidação técnica — 2026-09-12

O pedido humano de completar a rastreabilidade ampliou a continuidade registrada em
rastreabilidade-completa-jaeger.md. O complemento foi delimitado antes dos ajustes:
ServiceBusDevServicesTest e ServiceBusTelemetriaEmuladorTest (A1), somente testes.
Produção, timeout, retry, clientes operacionais e dependências não mudaram neste complemento.

### Diagnóstico e correção

ServiceBusDevServicesTest cancelava receive com next() antes de Complete. O teste agora
confere messageId e aguarda Complete dentro de concatMap(..., 0), emitindo a mensagem
somente depois do ACK; next() permanece por último. Dez repetições passaram (37,41 s).
A anotação temporária de repetição foi restaurada para o teste permanente.

A regressão ampla seguinte revelou um segundo ponto no A1: o cenário de rollback fazia
peek final depois de cancelar receive com take(observacao). A pilha localizou o timeout
no peek; a mensagem do SDK sobre idle não basta para afirmar uma causa interna do broker.
O teste agora verifica fila vazia no término da janela, enquanto receive permanece ativo,
antes do cancelamento. O helper de recebimento também verifica Complete → peek vazio
antes de emitir/cancelar. Identidade e asserções de resíduos permanecem obrigatórias.
Não se acrescentou retry, timeout, descarte de mensagens ou receiver alternativo.

### Provas finais

- Dez repetições DevServices: aprovadas.
- Regressão focada A1/DevServices/B1.5: 13 testes, zero falhas/erros/ignorados.
- Suíte opt-in completa posterior às duas correções: 41 testes em 11 classes,
  zero falhas/erros/ignorados; Maven exit code 0. Esta execução supera a pendência ampla anterior.
- Checkpoint padrão completo: Maven clean verify, scanner e Compute Engine aprovados;
  1.403 testes em 194 classes, zero falhas/erros/ignorados.
- Situação COMPLIANT / NOT_REQUIRED em 2026-09-12T15:31:47.8342667-03:00:
  213 issues / baseline 217, nenhuma nova ou HIGH/BLOCKER/CRITICAL, cobertura 88,3%,
  duplicação 4,4%. Baseline e avaliação original comparados e preservados.

Identificadores do checkpoint deste complemento:

- Compute Engine: 178145b4-8f04-4ff1-b64b-799e30fd6c86.
- Analysis key: 44183a0d-1cc5-4c72-b5e6-bc6d70d7d76d.
- Fingerprint: c6cb108f9ab162e4154e140c3cb4f2229193be14e84f1ccb8c67d1d5f17da488.
- Snapshot: .codex/.state/session-after-10-1-b1-complemento-compliant-20260912.json.
- Relatórios textuais das 11 classes opt-in e provas terminal/hub-real preservados em
  .codex/.state/b15-complemento-evidencias-20260912 antes do clean.
- Prova inicial B1.5 e evidências do marco anterior continuam em
  .codex/.state/b15-evidencias-20260912. Logs em target são transitórios.

### Estado de continuidade

B1.5 e a consolidação B1 estão concluídas tecnicamente. Isso não registra encerramento
humano da feature nem prova a cadeia inteira no Jaeger. B2–B5 e 10.1-C/C3 permanecem pendentes.
Jaeger local respondeu em localhost:16686; ainda falta confrontar o trace completo exportado.
O novo pedido Cosmos possui plano próprio rastreabilidade-fluxo-dossie-cosmos e ADR-0013
Proposto, revisado e aguardando CP-COSMOS humano antes da produção afetada. B2–B5 serão
coordenados com P6–P8. Nenhum commit, push, staging ou formato derivado foi produzido.