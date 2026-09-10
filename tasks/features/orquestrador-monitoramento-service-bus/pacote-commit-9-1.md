# Pacote de commit — consumo e registro da saída (9.1)

## Publicação de 9.1

9.1-A/B/C publicada por push normal na branch
`feature/orquestrador-monitoramento-service-bus`, partindo de
`d5df6b6e309842460427df290895f5064d00b5d1`:

| Commit | Conteúdo |
|---|---|
| `fcbe38a2a8a857668f0d6fc1377c02783c17ff4c` | Implementação de 9.1-A/B e regressões sem broker |
| `2c6c4cde881c3b8e2261e3a615123d58290fe5c4` | Quatro cenários integrados de 9.1-C |

`git ls-remote` confirmou `2c6c4cde881c3b8e2261e3a615123d58290fe5c4` no remoto,
igual ao HEAD após os dois commits. Esta consolidação documental acrescenta guias, evidências
e o registro desses marcos; seu hash pode ser consultado em `git log`.
O [manifesto de 8.2](pacote-commit.md) permanece histórico.

O usuário reafirmou concluir commit/publicação antes de analisar o problema relatado ao iniciar
Quarkus em dev mode. Esse diagnóstico operacional fica pendente após a publicação; não houve
correção de código/configuração nesta consolidação. A próxima etapa funcional continua 10.1.

## Organização e escopo

1. Implementação de 9.1-A/B e regressões sem broker: caso de uso, submissão do log, listener
   da saída, ativação independente, classificação/settlement e guardrails CDI.
2. Prova integrada de 9.1-C: startup dos dois listeners, POST, reagendamento, log/Complete
   e DLQ da saída seguida de POST válido.
3. Consolidação documental: arquitetura, guias, checklist, evidências e registro da publicação.

As subfatias já foram implementadas e verificadas. Esta preparação altera somente Markdown.
O guia permite executar o fluxo e assumir manualmente a próxima etapa, 10.1.
C3, etapas finais e encerramento humano da feature permanecem pendentes.

## Manifesto exato

São 22 caminhos: seis de produção/configuração, oito de testes (incluindo duas remoções
de guardrails substituídos) e oito Markdown. A seleção é explícita, sem `git add .`.

```text
src/main/java/br/gov/caixa/simtr/orquestrador/adaptador/entrada/servicebus/MonitoramentoResultadoListener.java
src/main/java/br/gov/caixa/simtr/orquestrador/adaptador/saida/log/ResultadoMonitoramentoLogAdapter.java
src/main/java/br/gov/caixa/simtr/orquestrador/aplicacao/casodeuso/ReceberResultadoMonitoramentoUseCase.java
src/main/java/br/gov/caixa/simtr/orquestrador/aplicacao/porta/entrada/ReceberResultadoMonitoramento.java
src/main/java/br/gov/caixa/simtr/orquestrador/aplicacao/porta/saida/RegistrarResultadoMonitoramento.java
src/main/resources/application.properties
src/test/java/br/gov/caixa/simtr/arquitetura/guardrails/EsqueletosMonitoramentoCdiTest.java
src/test/java/br/gov/caixa/simtr/arquitetura/guardrails/EstruturaPlanejada.java
src/test/java/br/gov/caixa/simtr/arquitetura/guardrails/ComponentesResultadoMonitoramento.java
src/test/java/br/gov/caixa/simtr/arquitetura/guardrails/ResultadoMonitoramentoCdiTest.java
src/test/java/br/gov/caixa/simtr/orquestrador/adaptador/entrada/servicebus/MonitoramentoResultadoListenerTest.java
src/test/java/br/gov/caixa/simtr/orquestrador/adaptador/saida/log/ResultadoMonitoramentoLogAdapterTest.java
src/test/java/br/gov/caixa/simtr/orquestrador/aplicacao/casodeuso/ReceberResultadoMonitoramentoUseCaseTest.java
src/test/java/br/gov/caixa/simtr/orquestrador/integracao/MonitoramentoResultadoEmuladorTest.java
doc/arquitetura-distribuida/arquitetura-ddd-integracoes-atomicas.md
doc/guias/guia-service-bus-amqp-dossie.md
tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md
tasks/features/orquestrador-monitoramento-service-bus/plan.md
tasks/features/orquestrador-monitoramento-service-bus/retomada.md
tasks/features/orquestrador-monitoramento-service-bus/todo.md
tasks/features/orquestrador-monitoramento-service-bus/continuidade-9-1.md
tasks/features/orquestrador-monitoramento-service-bus/pacote-commit-9-1.md
```

As remoções de EsqueletosMonitoramentoCdiTest e EstruturaPlanejada são intencionais:
ResultadoMonitoramentoCdiTest e ComponentesResultadoMonitoramento verificam os beans
agora funcionais. `.codex-doc-alignment.patch`, estado local, relatórios, credenciais,
`target/` e formatos derivados ficam fora do pacote e são preservados.

## Evidência associada ao código

| Verificação | Resultado de 10/09/2026 |
|---|---|
| Regressões focadas de 9.1-B | 123 testes, incluindo 36 do listener e três provas CDI; sem falhas |
| Suíte padrão e build finais | 1.345 testes em 192 classes, zero falhas/erros/ignorados; build SUCCESS |
| Perfil completo de integração | 27 testes em sete classes, zero falhas/erros/ignorados; quatro novos em 9.1-C |
| Sonar final | COMPLIANT / NOT_REQUIRED; 88,1% cobertura, 4,4% duplicação, 213 issues, nenhuma nova ou HIGH/BLOCKER/CRITICAL |
| Baseline | Original de 217 issues e baselineAssessment preservados, sem reinicialização |

Checkpoint: `2026-09-10T15:27:27.592153-03:00`.
Análise: `3546ec18-65c4-4e5b-a382-ed897f69f7b5`.
Compute Engine: `d4c8a25d-daee-4169-a736-59133f9e8d5f`.
Fingerprint executável:
`5d84ce454d3e4ab5ba5aabb5b8148e0076ab4ef3a22a0c91c873a211719afd0d`.

O checkpoint completo incluiu clean verify, SonarScanner e Compute Engine.
A preparação documental reutiliza essa evidência mediante comparação do fingerprint.
Não repetir Maven/Sonar somente por documentação. As
[evidências de A/B/C](continuidade-9-1.md) detalham RED/GREEN, correções e revisões independentes.

Comandos de reprodução, sequenciais na raiz:

```powershell
mvn clean verify
mvn -q -Pservicebus-integration clean test
```

Para executar somente os quatro novos cenários, usar como alternativa ao perfil completo:

```powershell
mvn -q -Pservicebus-integration "-Dtest=MonitoramentoResultadoEmuladorTest" test
```

Os testes com broker exigem Docker e sessão sem conexão Azure externa; somente a consulta
do Hub é controlada. A suíte padrão permanece sem broker. Não são provas de MTR/Azure reais.

## Guia manual e limites

O [guia do desenvolvedor](guia-desenvolvimento.md#roteiro-para-assumir-a-entrega) reúne pré-requisitos,
comandos Dev Services/Azure, POST PowerShell/Swagger, fixture 4324680, política curta opcional,
interpretação dos logs e próxima etapa. Para o fluxo completo, habilitar as duas propriedades:

```properties
monitoramento.service-bus.entrada.consumo-habilitado=true
monitoramento.service-bus.saida.consumo-habilitado=true
```

Ambas permanecem false por padrão. A fixture manual está em Rascunho, de modo que a demonstração
reagenda até o limite configurado. O evento final contém os IDs técnicos em mdc.
C9.1-L aceito: Complete após submissão ao logger; falha interna/filtro pode perder o log
sem Abandon. Log/Complete não são atômicos. Falha de settlement encerra o consumidor
sem segunda liquidação ou reinício pela aplicação. HTTP ativo não comprova consumo ativo.

A prova DLQ encontrou detach remoto e falha de ACK no receiver auxiliar. Passou após aguardar
a própria mensagem materializada e a principal vazia; o isolado e o perfil completo passaram
com Complete e POST posterior. A causa interna do emulador não foi demonstrada.
Não houve alteração de produção/SDK, retry ou supressão do settlement.
A validação em dev mode interativo continua pendente; Azure gerenciado não foi executado.

## Conferência final

Conferir manifesto, conteúdo do índice contra o workspace, diff sem erros, links/âncoras,
ausência de padrões de credenciais e fingerprint idêntico ao checkpoint final.
As revisões independentes de código não encontraram bloqueadores. A revisão de retomada
confirmou o roteiro e identificou uma contagem histórica de integração no roteiro manual,
alinhada ao total atual de 27 testes/sete classes nesta consolidação.

O push dos dois commits executáveis e a igualdade local/remoto foram confirmados conforme o
registro no início deste manifesto. Confirmar também o hash do complemento documental após seu push.
Nenhuma implementação de 10.1 nem encerramento humano da feature faz parte deste pacote.
