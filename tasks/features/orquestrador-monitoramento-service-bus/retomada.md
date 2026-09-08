# Retomada — orquestrador e monitoramento Service Bus

**Continuidade atual:** item 4.1 tecnicamente concluído, incluindo política/configuração,
contratos, mappers, logs tipados, validação confirmada de resultado e guardrails.
Checkpoint COMPLIANT; evidências completas no checklist. Nenhum avanço para 5.1,
listener, publicação, settlement ou encerramento humano da feature.

## Estado vigente em 2026-09-08

O usuário confirmou QUARENTENA com situacaoMtr=null e zero tentativas quando ainda não
houve consulta; CONCLUSIVO exige situação conclusiva e pelo menos uma tentativa.
A regra foi implementada e verificada nas duas bordas independentes. Contadores negativos
são rejeitados. O JSON v1 e as situações recebidas são preservados, sem recálculo ou efeito remoto.

**4.1 tecnicamente concluído.** A regressão focada passou em 342 testes, incluindo 25 casos
novos; a suíte completa passou em **1046 testes em 171 classes**, sem falhas, erros ou ignorados.
Checkpoint de `2026-09-08T11:12:19.3296159-03:00`: **COMPLIANT**, cobertura **87,0%**,
duplicação **4,4%**, 213 issues abertas, nenhuma nova ou HIGH/BLOCKER/CRITICAL.
A duplicação não aumentou neste incremento. A validação nova tem 100% das linhas e condições
cobertas em ambas as bordas. Análise `f3b6720c-0b18-4713-8a16-22a030278153`.
Os checkpoints de 859, 900 e 1021 testes continuam como evidências históricas.

Baseline original de 217 issues integralmente preservado, análise
`f6183a72-a2ea-44bc-9374-b2b064bdad55`, READY; nenhuma reinicialização.
Branch `feature/orquestrador-monitoramento-service-bus`; commit e push autorizados para revisão com desenvolvedores. Consultar o histórico Git e o manifesto do pacote.
Hub, dossiê, configurações e extensão/emulador preservados. Restam 19 esqueletos inativos,
correspondentes aos incrementos futuros. O próximo item é 5.1, ainda não iniciado.
O encerramento humano da feature não foi inferido; detalhes no [checklist](todo.md).

## Registro histórico da pausa em 2026-09-07

O texto abaixo descreve o ponto de partida anterior à implementação de 2026-09-08.
As menções a RED, próxima subfatia e checkpoint de 859 testes são históricas.

Pausa solicitada pelo usuário em 2026-09-07, para continuar em 2026-09-08.
Branch: `feature/orquestrador-monitoramento-service-bus`. Nenhum commit ou staging realizado.

## Estado seguro da pausa

Todas as alterações rastreadas e não rastreadas estão preservadas no workspace.
Não executar reset, clean, checkout destrutivo ou stash para reconstruir um estado anterior.
Os comandos iniciados pelo agente terminaram; não há agente auxiliar trabalhando.
Não iniciar tarefa em segundo plano durante a pausa.

**O workspace está em RED de compilação conhecido, não em GREEN.** Dois testes novos e uma
fixture de reagendamento exigem uma API ainda ausente. Não apagar esses arquivos nem atribuir
ao estado atual os 859 testes do checkpoint anterior.

## Leitura para continuar

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
