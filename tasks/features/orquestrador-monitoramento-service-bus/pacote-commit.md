# Pacote de commit — ativação controlada da entrada (8.2)

## Publicação confirmada — 2026-09-10

O incremento de 8.2 e sua consolidação foram publicados em
`a797114d1dc833499dcc957a881e83f431cae7b9` (`feat: ativar listener de monitoramento por configuracao`).
O push normal para `origin/feature/orquestrador-monitoramento-service-bus` foi concluído
e `git ls-remote` confirmou o mesmo hash do HEAD local. São 13 arquivos, sem alterações
de implementação em 9.1. Este complemento documental registra o resultado já confirmado.

Seleção/índice/workspace idênticos, diff sem erros e varredura de padrões de segredo aprovada.
O fingerprint executável e o baseline permaneceram iguais à evidência de 8.2.
`.codex-doc-alignment.patch` permaneceu fora do Git; artefatos locais foram preservados.
A etapa 9.1 continua pendente; não houve encerramento humano da feature.

## Escopo autorizado em 2026-09-10

O usuário pediu consolidar tudo até 8.2, atualizar o guia do desenvolvedor para retomada,
organizar o commit do que foi realizado e publicar antes de prosseguir com a etapa 9.
Branch: `feature/orquestrador-monitoramento-service-bus`.
Base publicada: `b886bdb8bb079eadcba2ed21c68739cf62ff3356` (até 8.1).
O [manifesto anterior](pacote-commit-8-1.md) conserva a seleção/evidência histórica de 42 arquivos.

Este pacote reúne **13 arquivos**: dois de produção/configuração, dois de teste e nove Markdown.
A ativação é opt-in no startup pela propriedade
`monitoramento.service-bus.entrada.consumo-habilitado=true`, com default false.
O código da etapa 9 não foi alterado; a leitura inicial foi interrompida para esta consolidação.
O guia corrige o ID manual para a fixture real 4324680 e explica os dois modos de broker.

## Manifesto exato

Selecionar somente estes caminhos, nunca `git add .`:

```text
doc/arquitetura-distribuida/arquitetura-ddd-integracoes-atomicas.md
doc/guias/guia-service-bus-amqp-dossie.md
src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/entrada/servicebus/MonitoramentoEntradaListener.java
src/main/resources/application.properties
src/test/java/br/gov/caixa/simtr/monitoramento/adaptador/entrada/servicebus/MonitoramentoEntradaListenerTest.java
src/test/java/br/gov/caixa/simtr/monitoramento/integracao/MonitoramentoAtivacaoEmuladorTest.java
tasks/features/orquestrador-monitoramento-service-bus/continuidade-8-2.md
tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md
tasks/features/orquestrador-monitoramento-service-bus/pacote-commit.md
tasks/features/orquestrador-monitoramento-service-bus/pacote-commit-8-1.md
tasks/features/orquestrador-monitoramento-service-bus/plan.md
tasks/features/orquestrador-monitoramento-service-bus/retomada.md
tasks/features/orquestrador-monitoramento-service-bus/todo.md
```

`.codex-doc-alignment.patch`, `.codex/.state/`, `sonar/`, `target/`, credenciais,
ambiente e formatos derivados permanecem fora do commit e preservados localmente.
A cópia do manifesto antigo é somente Markdown histórico, sem nova alteração executável.

## Mensagem do incremento

```text
feat: ativar listener de monitoramento por configuracao

Permite iniciar o consumidor da entrada no startup por opt-in, com
default desabilitado e lifecycle existente preservado. Prova o fluxo
POST -> processamento -> reagendamento/resultado sem inicio Java manual.

Consolida guias de Dev Services/Azure, corrige a fixture do roteiro
manual e prepara a retomada pelo consumo/log da saida em 9.1.

Validacao: 1297 testes sem broker e build; 23 integracoes com emulador.
Sonar COMPLIANT: 87,9% cobertura, 4,3% duplicacao, zero issues novas/graves.
```

## Verificações associadas ao código

| Verificação | Evidência em 10/09/2026 |
|---|---|
| Teste focado de lifecycle/ativação | 43 testes do listener, mais regressões focadas de log e fábrica; sem falhas |
| Suíte padrão e build | 1.297 testes em 189 classes, zero falhas/erros/ignorados |
| Integração completa com emulador | 23 testes em seis classes, zero falhas/erros/ignorados |
| Startup configurado e POST reais | Dois casos nas 23 integrações: conclusão na primeira consulta e após reagendamento |
| Sonar | COMPLIANT / NOT_REQUIRED; 87,9% cobertura, 4,3% duplicação, 213 issues, nenhuma nova/grave |
| Baseline | READY / LOCAL_SONAR original de 217 issues, integralmente preservado |

Análise `4eb0718a-d90c-4155-8a7f-a5118f3d16c6`;
Compute Engine `3935ac03-dee8-4462-9528-82040065a932`.
Fingerprint executável:
`9b21bd4c4a82e385f1191e905f40a916e6f95055bb74a59f3385a8109d5fcd27`.
As correções desta consolidação são Markdown; comparar esse fingerprint antes do commit.
Não repetir Maven/Sonar somente pelas correções documentais.

Comandos de reprodução, executados sequencialmente na raiz:

```powershell
mvn clean verify
mvn -q -Pservicebus-integration clean test
mvn -q -Pservicebus-integration "-Dtest=MonitoramentoAtivacaoEmuladorTest" test
```

O último é a alternativa focada, já incluída na suíte de integração completa.
As provas com broker exigem Docker e sessão sem configuração externa de Service Bus.
A porta pública do Hub é controlada nesses testes; não se trata de consulta MTR real.
Detalhes do RED/GREEN, revisões e checkpoint na [continuidade de 8.2](continuidade-8-2.md).

## Execução manual e limites

O [guia do desenvolvedor](guia-desenvolvimento.md#verificação-rápida-do-marco-até-82) contém
comandos completos para Dev Services/Azure, POST PowerShell/Swagger, fixture correta,
overrides locais de política curta e interpretação do resultado.
O [guia Service Bus](../../../doc/guias/guia-service-bus-amqp-dossie.md) explica o fluxo por package.

O padrão PT30M/PT24H permanece. Com a fixture Rascunho, o fluxo reagenda até o limite.
202 confirma a publicação inicial. O monitoramento já pode publicar na saída; consumo e log
da saída ainda pertencem a 9.1. Publicação terminal + Complete não são atômicos e podem duplicar
saída. Dev mode interativo e Azure gerenciado não foram executados nesta entrega.

## Conferência e publicação

Revisar `git diff --cached`, comparar seleção e workspace, conferir links/âncoras e
`git diff --cached --check`. As revisões do código de 8.2 não encontraram bloqueadores;
a revisão operacional detectou a fixture inexistente do exemplo, corrigida no guia.
O commit e o push autorizados foram concluídos; o hash remoto foi confirmado conforme o registro
no início deste manifesto. A main permanece preservada; 9.1 não foi antecipada.

[Retomada do desenvolvimento](retomada.md).
