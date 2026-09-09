# Retomada — orquestrador e monitoramento Service Bus

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
