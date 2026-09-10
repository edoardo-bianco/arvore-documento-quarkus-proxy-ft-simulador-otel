# Histórico do pacote até 8.1 — publicado em b886bdb

Este documento preserva a preparação e publicação anteriores. Para o incremento atual de 8.2,
consultar o [manifesto vigente](pacote-commit.md). Comandos e contagens abaixo são históricos.

## Publicação confirmada em 2026-09-10

Após a autorização humana "proceder com commit e publicação", os 42 arquivos deste manifesto
foram publicados em b886bdb8bb079eadcba2ed21c68739cf62ff3356, com push normal para
origin/feature/orquestrador-monitoramento-service-bus e hash confirmado no remoto.
O patch temporário e os artefatos locais permaneceram fora do commit.
Este manifesto conserva o retrato da preparação até 8.1. A 8.2 posterior tem escopo e evidências
na [continuidade própria](continuidade-8-2.md) e não faz parte daquele commit.

## Seleção preparada em 2026-09-10

Pedido humano: preparar um commit seguro e esclarecer execução, testes e alinhamento dos guias.
Este pacote reúne o incremento local **7.1-B/C/D e 8.1**, a partir do HEAD publicado
`d83b6895a196718880e8e3d23519702f6eb77682`, na branch
`feature/orquestrador-monitoramento-service-bus`.

A preparação seleciona **42 arquivos** explicitamente para o índice Git, incluindo os novos
arquivos de código/testes e esta documentação. Commit e push não foram executados.
Não se trata de encerramento da feature nem de entrega do consumo/log da saída (9.1).

| Grupo | Arquivos | Conteúdo |
|---|---:|---|
| Produção/configuração | 17 | Catálogo e política, processamento terminal, listener de início explícito, logs mínimos, reagendamento transacional e contratos/Javadocs relacionados |
| Testes | 14 | Limites, catálogo, CDI, listener, adapter, contratos, guardrails e duas classes novas de integração |
| Arquitetura e guia Service Bus | 4 | ADR-0011 e índice, consolidado e guia de execução |
| Tasks e guia do dev | 7 | Plano, checklist, retomada, continuidades 7.1/8.1, guia e manifesto |

É um pacote acumulado maior que uma fatia ideal. Os incrementos compartilham listener, decisões,
política e tipos de reagendamento; a seleção mantém juntos o estado integrado já testado e sua
documentação. Não foi reconstruído um estado intermediário sem evidência de compilação/testes.

## Mensagem preparada

```text
feat: processar e reagendar monitoramento via Service Bus

Conecta o listener de inicio explicito ao processamento terminal e ao
reagendamento transacional da entrada. Preserva prazo, IDs e versao;
resolve politicas recebidas com fallback v1 e limites de tentativas.

Inclui provas de commit, rollback/redelivery, cancelamento e fluxos
terminais com emulador. Mantem o consumidor da saida e telemetria final
pendentes, com guias de execucao e continuidade alinhados.

Validacao: 1293 testes sem broker e build em 10/09; 21 integracoes no
emulador em 09/09. Sonar COMPLIANT, 87,9% cobertura, 4,3% duplicacao,
nenhuma issue nova/grave.
```

A mensagem descreve o resultado funcional desde d83b689. Consultas, REST/publicação inicial e
publisher da saída já estavam publicados até 7.1-A; não são apresentados como novidades isoladas.

## Manifesto exato dos arquivos

Caminhos relativos à raiz. A seleção inclui somente os arquivos abaixo; não usar `git add .`.

```text
doc/adr/0011-composicao-local-monitoramento-e-fabrica-service-bus.md
doc/adr/README.md
doc/arquitetura-distribuida/arquitetura-ddd-integracoes-atomicas.md
doc/guias/guia-service-bus-amqp-dossie.md
src/main/java/br/gov/caixa/simtr/arquitetura/infraestrutura/servicebus/ClientesServiceBus.java
src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/configuracao/PoliticaMonitoramentoProducer.java
src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/entrada/servicebus/LogListenerMonitoramento.java
src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/entrada/servicebus/MonitoramentoEntradaListener.java
src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/servicebus/MonitoramentoReagendamentoAdapter.java
src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/servicebus/MonitoramentoReagendamentoServiceBusMapper.java
src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/servicebus/dto/ResultadoMonitoramentoDossieMtrV1.java
src/main/java/br/gov/caixa/simtr/monitoramento/aplicacao/casodeuso/ProcessarTentativaMonitoramentoUseCase.java
src/main/java/br/gov/caixa/simtr/monitoramento/aplicacao/porta/entrada/ProcessarTentativaMonitoramento.java
src/main/java/br/gov/caixa/simtr/monitoramento/aplicacao/porta/saida/ReagendarTentativaMonitoramento.java
src/main/java/br/gov/caixa/simtr/monitoramento/dominio/modelo/DecisaoProcessamento.java
src/main/java/br/gov/caixa/simtr/monitoramento/dominio/modelo/ReagendamentoMonitoramento.java
src/main/java/br/gov/caixa/simtr/monitoramento/dominio/politica/CatalogoPoliticasMonitoramento.java
src/main/java/br/gov/caixa/simtr/monitoramento/dominio/politica/PoliticaMonitoramento.java
src/main/java/br/gov/caixa/simtr/monitoramento/dominio/politica/PoliticaMonitoramentoProgressiva.java
src/main/java/br/gov/caixa/simtr/orquestrador/adaptador/entrada/servicebus/dto/ResultadoMonitoramentoDossieMtrV1.java
src/main/resources/application.properties
src/test/java/br/gov/caixa/simtr/arquitetura/guardrails/EstruturaPlanejada.java
src/test/java/br/gov/caixa/simtr/arquitetura/infraestrutura/servicebus/ClientesServiceBusTest.java
src/test/java/br/gov/caixa/simtr/monitoramento/adaptador/configuracao/PoliticaMonitoramentoProducerTest.java
src/test/java/br/gov/caixa/simtr/monitoramento/adaptador/configuracao/PoliticasMonitoramentoConfigTest.java
src/test/java/br/gov/caixa/simtr/monitoramento/adaptador/entrada/servicebus/MonitoramentoEntradaListenerTest.java
src/test/java/br/gov/caixa/simtr/monitoramento/adaptador/entrada/servicebus/MonitoramentoEntradaLogTest.java
src/test/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/servicebus/MonitoramentoReagendamentoAdapterTest.java
src/test/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/servicebus/ResultadoMonitoramentoContratoTest.java
src/test/java/br/gov/caixa/simtr/monitoramento/aplicacao/casodeuso/ProcessarTentativaMonitoramentoUseCaseTest.java
src/test/java/br/gov/caixa/simtr/monitoramento/dominio/modelo/ReagendamentoMonitoramentoTest.java
src/test/java/br/gov/caixa/simtr/monitoramento/dominio/politica/CatalogoPoliticasMonitoramentoTest.java
src/test/java/br/gov/caixa/simtr/monitoramento/dominio/politica/PoliticaMonitoramentoProgressivaTest.java
src/test/java/br/gov/caixa/simtr/monitoramento/integracao/MonitoramentoTerminalEmuladorTest.java
src/test/java/br/gov/caixa/simtr/monitoramento/integracao/ReagendamentoTransacionalEmuladorTest.java
tasks/features/orquestrador-monitoramento-service-bus/continuidade-7-1.md
tasks/features/orquestrador-monitoramento-service-bus/continuidade-8-1.md
tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md
tasks/features/orquestrador-monitoramento-service-bus/pacote-commit.md
tasks/features/orquestrador-monitoramento-service-bus/plan.md
tasks/features/orquestrador-monitoramento-service-bus/retomada.md
tasks/features/orquestrador-monitoramento-service-bus/todo.md
```

Fora do pacote: `.codex-doc-alignment.patch`, `.codex/.state/`, `sonar/`, `target/`,
credenciais e arquivos locais de ambiente. Todos permanecem preservados no workspace.
Nenhum formato derivado PDF/PPTX/HTML é atualizado nesta preparação.

## O que deve funcionar

| Verificação | Comando na raiz | Resultado já comprovado |
|---|---|---|
| Suíte padrão e build | `mvn clean verify` | 1.293 testes em 189 classes, sem falhas/erros/ignorados, mais build; 10/09 |
| Somente suíte padrão | `mvn test` | Mesmo conjunto de testes, sem emulador nem filas Azure |
| Regressão focada dos ajustes 8.1 | Comando abaixo | 68 testes, sem falhas/erros/ignorados; 10/09 |
| Integração completa com emulador | `mvn -q -Pservicebus-integration clean test` | 21 testes em cinco classes; 09/09 |
| Fluxos terminais e reagendamento pelo listener | `mvn -q -Pservicebus-integration "-Dtest=MonitoramentoTerminalEmuladorTest" test` | 12 cenários incluídos nas 21 integrações |

```powershell
mvn -q "-Dtest=ReagendamentoMonitoramentoTest,MonitoramentoReagendamentoAdapterTest,MonitoramentoEntradaListenerTest,PoliticaMonitoramentoProgressivaTest" test
```

Executar as suítes sequencialmente: compartilham `target/` e a porta HTTP de teste.
Java 25 e Maven são necessários; integração exige Docker operacional e sessão sem configuração
externa de Service Bus. O profile de integração valida essa condição e habilita emulador/SQL
explicitamente. Seus 21 testes são: infraestrutura 1, entrada 3, publicação de resultado 2,
transação 3 e fluxo terminal/reagendamento 12. As duas suítes são distintas: ativar
`servicebus-integration` seleciona somente as provas com broker.

Os testes de fluxo controlam a porta pública de consulta do Hub, mantendo SDK, emulador,
listener, caso de uso, ACL e publishers reais. Verificam conclusivo, no-op, quarentena por prazo
ou máximo, versão removida, Abandon/redelivery, DLQ, progressão de intervalos e reagendamento.
Não comprovam consulta ao MTR real nem consumo/log da saída de 9.1.

## Executar a aplicação

Com Java 25, Maven, Docker e a configuração de desenvolvimento descrita no
[README](../../../README.md#execucao-local):

```powershell
mvn quarkus:dev -Ddebug=false
```

Para emulador, a sessão não deve fornecer connection string/namespace externo de Service Bus.
As configurações existentes do Hub continuam necessárias, incluindo as variáveis de ambiente
`SIMTR_API_KEY`, `SIMTR_OIDC_CLIENT_SECRET` e `SIMTR_OIDC_INTERNET_CLIENT_SECRET`
conforme o README; não copiar seus valores para comandos, documentos ou Git.

Swagger UI: `http://localhost:8080/simtr-hub/doc`.
O POST `/simtr-hub/v1/monitoramentos-dossie` já publica a primeira tentativa e retorna 202
depois da confirmação do broker. Iniciar a aplicação **não inicia o listener**:
`MonitoramentoEntradaListener.iniciar()` só é chamado explicitamente nas provas controladas.
Não existe flag ou endpoint de ativação. Por isso, a demonstração reproduzível do processamento
e do reagendamento neste marco é a integração acima, não apenas o POST após subir o dev mode.

A flag `monitoramento.simulador.prevalidacao.habilitado=true` habilita o mock, mas também não
inicia o consumo. Para usar filas Azure configuradas, o comando continua
`mvn quarkus:dev "-Dquarkus.profile=dev,azure"`, com SAS/filas fornecidas externamente.
Azure real e o startup interativo de dev não foram executados nesta preparação do commit.

## Evidências e limites do commit

- 68 testes focados e 1.293 testes padrão/build aprovados em 10/09.
- 21 integrações aprovadas em 09/09; os cinco ajustes posteriores preservaram o comportamento
  e não exigiram repetição das provas com broker.
- Checkpoint COMPLIANT / NOT_REQUIRED: cobertura 87,9%, duplicação 4,3%, 213 issues abertas,
  nenhuma nova ou HIGH/BLOCKER/CRITICAL; cinco correções CLOSED/FIXED.
- Análise anterior à preparação `73b3dcd4-87ef-4158-a283-c6ba7195cf86`, em 10/09; detalhes e recuperação do baseline
  na [continuidade de 8.1](continuidade-8-1.md). O estado da sessão não integra o commit.
- O consumo exige início explícito. 9.1 e 10.1/C3 continuam pendentes.
- Transação de reagendamento comprovada localmente no emulador. Publicação terminal +
  Complete ainda não é atômica; não há Outbox/idempotência durável.

A preparação começou em Markdown e no índice Git. A conferência dos arquivos novos no índice
revelou linhas vazias extras no fim de seis testes; somente essas linhas foram removidas,
com escopo registrado no plano. O diff confirmou ausência de alteração lógica. Por mudar o
fingerprint, o checkpoint completo foi repetido no mesmo baseline e passou: 1.293 testes/189
classes, build aprovado e COMPLIANT / NOT_REQUIRED, 87,9% de cobertura e 4,3% de duplicação.
A integração com emulador não foi repetida para esse ajuste de formatação.

Checkpoint final de preparação: 2026-09-10 08:25:06 -03:00, análise `79215d12-7a59-4071-b60a-08d491e8815b`,
Compute Engine `ad62cf3a-a7c6-439d-8c22-eeaef0e20743`; fingerprint `3871c60389fe2d7425a3c22d8c74e0c14146088e4501d8f8ba61bacf7f1482f7`.
O baseline original foi comparado integralmente e permaneceu idêntico.

## Guias alinhados e conferência

O [guia do dev](guia-desenvolvimento.md#aplicação-local-e-testes-sem-broker) e o
[guia Service Bus](../../../doc/guias/guia-service-bus-amqp-dossie.md#escolha-local-do-desenvolvedor-e-execução-dos-testes)
explicam os comandos e a limitação de início explícito. Plano, checklist, retomada,
arquitetura e continuidades descrevem 8.1 implementada/concluída tecnicamente e 9.1 pendente.
As contagens de etapas antigas permanecem identificadas como históricas.

A seleção final foi conferida: os 42 caminhos deste manifesto estão no índice, sem artefatos
locais e sem diferenças entre índice/workspace nos arquivos selecionados. O diff preparado
passou em `git diff --cached --check`; links locais e âncoras foram conferidos.
Antes de criar o commit, o conteúdo continua revisável por `git diff --cached --stat` e
`git diff --cached`. Revisão independente da produção acumulada concluída sem bloqueadores.
Não atribuir hash novo, publicação ou encerramento sem o resultado real dessas operações.

## Referências históricas publicadas

A base até 4.1 foi publicada em 84fca5c, com complemento documental em 266092f.
O marco até 7.1-A está em d83b689: à época, 1.172 testes padrão, seis integrações,
87,4% de cobertura e 4,3% de duplicação. Essas contagens e a antiga proposta de commit até
7.1-A não descrevem o pacote atual. Execução e decisões detalhadas permanecem nas
[continuidades de 7.1](continuidade-7-1.md) e [8.1](continuidade-8-1.md).
