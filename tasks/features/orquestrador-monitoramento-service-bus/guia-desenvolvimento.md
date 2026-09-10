# Guia de desenvolvimento — orquestrador e monitoramento Service Bus

**9.1-A/B/C publicadas: implementação `fcbe38a` e integração `2c6c4cd`.**
O [manifesto de 9.1](pacote-commit-9-1.md) reúne seleção, evidências e registro de publicação.
Próximo item de desenvolvimento: **10.1**, correlação OpenTelemetry; começar pelo
[roteiro para assumir a entrega](#roteiro-para-assumir-a-entrega).

Passaram **1.345 testes padrão em 192 classes** e **27 integrações em sete classes**, sem
falhas, erros ou ignorados. Build e Sonar **COMPLIANT / NOT_REQUIRED**: cobertura **88,1%**,
duplicação **4,4%**, nenhuma issue nova ou grave. Baseline original preservado.
As provas automatizadas usam o emulador com a porta do Hub controlada. Após a publicação,
duas inicializações em dev mode e um POST até o log final foram verificados com a fixture real
do simulador. Os avisos JDBC de prelogin foram transitórios nessas reproduções; o bloqueio
persistente original não foi reproduzido. [Diagnóstico e operação](diagnostico-devmode-20260910.md).
Azure gerenciado não foi executado. O [manifesto de 8.2](pacote-commit.md) preserva o marco publicado anterior
a797114; a [continuidade de 9.1](continuidade-9-1.md) conserva os checkpoints de A/B/C.

O caso de uso, o adapter de log e o listener da saída estão conectados por CDI.
C9.1-L aceito: Complete ocorre após submissão ao logger, sem confirmação de escrita no destino.
Uma falha interna pode perder o log e não gera reentrega para recuperá-lo.
Campos do evento final ficam em mdc, preservando o formato padrão.

## Prova integrada da saída em 9.1-C

[MonitoramentoResultadoEmuladorTest](../../../src/test/java/br/gov/caixa/simtr/orquestrador/integracao/MonitoramentoResultadoEmuladorTest.java)
acrescenta quatro cenários com ambos os listeners ativados pelo startup:

- POST terminal até log real com os mesmos IDs e remoção da saída por Complete;
- reagendamento até conclusão na segunda consulta;
- Rascunho até o máximo de três consultas e registro final de encerramento;
- contrato inválido da saída na DLQ, seguido de novo POST válido até log/Complete.

Somente a consulta do Hub é controlada. REST, ACLs, políticas, publishers, mappers, listeners,
caso de uso e logger são reais. O profile reutiliza a proteção contra conexão Azure externa,
habilita pré-validação simulada e usa PT1S/PT1M/max3 somente no teste. Esses parâmetros não
alteram os defaults nem a fixture manual 4324680, que continua Rascunho.

A prova confere o JSON final em mdc e, separadamente, a remoção das mensagens com peek de
sequência explícita. Ela não disputa a fila principal com outro consumidor. A DLQ exige
esperar a própria mensagem visível e a principal vazia antes de receber/concluir a subfila.
Duas execuções anteriores sem essa sincronização perderam o ACK do receiver auxiliar após
detach remoto; o ajuste e a continuidade do listener foram verificados, sem alteração do SDK
ou da produção. A causa interna do emulador não foi estabelecida.

```powershell
mvn -q -Pservicebus-integration "-Dtest=MonitoramentoResultadoEmuladorTest" test
mvn -q -Pservicebus-integration clean test
```

Os quatro cenários novos e as 23 integrações anteriores passaram: 27 testes em sete classes.
Passaram também 1.345 testes padrão/192 classes sem broker, com build SUCCESS.
Sonar COMPLIANT / NOT_REQUIRED: cobertura 88,1%, duplicação 4,4%, nenhuma issue nova/grave;
baseline original preservado. A suíte padrão continua sem broker. C9.1-L permanece válido; log e Complete não são atômicos
e a confirmação da porta não garante escrita no destino. Falhas técnicas/Abandon estão
caracterizadas sem broker na 9.1-B. Azure gerenciado e dev mode interativo não foram executados.
9.1-A/B/C concluídas tecnicamente; próximo item: 10.1, correlação/telemetria, seguido de C3.
[Evidências e diagnóstico](continuidade-9-1.md).

## Listener da saída em 9.1-B

MonitoramentoResultadoListener está conectado ao mapper, à porta e ao log. Ativação independente:

```properties
monitoramento.service-bus.entrada.consumo-habilitado=false
monitoramento.service-bus.saida.consumo-habilitado=false
```

Para executar os dois consumidores com Dev Services, mantendo os pré-requisitos de ambiente
da seção de execução manual:

```powershell
mvn quarkus:dev -Ddebug=false "-Dmonitoramento.service-bus.entrada.consumo-habilitado=true" "-Dmonitoramento.service-bus.saida.consumo-habilitado=true" "-Dmonitoramento.simulador.prevalidacao.habilitado=true"
```

Para filas Azure, acrescentar "-Dquarkus.profile=dev,azure" e fornecer a conexão/filas externas
descritas adiante. Escolher o broker não ativa nenhum listener. A flag da saída também pode
ser habilitada sozinha para consumir resultados já presentes, sem iniciar a entrada.
Usar o POST e a fixture 4324680 do roteiro manual; os intervalos/limites continuam iguais.

Quando houver resultado, o orquestrador valida a entrega e submete o evento
orquestrador.monitoramento-dossie.resultado.registrado, com IDs técnicos em mdc, antes de Complete.
Falhas anteriores à submissão propagadas pela porta geram Abandon; contrato inválido do mapper
vai à DLQ com MONITORAMENTO_SAIDA_INVALIDA. Falha no settlement encerra o consumo sem tentar outro.
Reinício exige nova instância/runtime; HTTP ativo não garante que o consumidor está ativo.
Shutdown cancela a assinatura antes da fábrica e não fecha o cliente compartilhado.
C9.1-L permanece: o registro pode faltar por falha interna/filtro do logger, mesmo após Complete.

Verificação local: 123 testes focados, incluindo 36 do listener e três provas CDI, passaram.
As 23 integrações anteriores passaram com a saída desabilitada. A prova nova de startup
com a flag da saída, POST, log e fila concluída foi acrescentada na 9.1-C, descrita acima.
Correlação/telemetria e revisão final do demonstrador permanecem nas etapas seguintes.
Fechamento da 9.1-B: 1.345 testes padrão/192 classes, zero falhas/erros/ignorados, build SUCCESS;
Sonar COMPLIANT / NOT_REQUIRED, 88,1% cobertura, 4,4% duplicação e nenhuma issue nova/grave.
Baseline preservado. Evidências completas e limite da integração na continuidade.

## O que já pode ser demonstrado

Os packages abaixo estão no **mesmo artifact e runtime Quarkus**, simulando responsabilidades
de microsserviços. Ainda não são aplicações implantadas separadamente.

| Responsável | Comportamento disponível no workspace |
|---|---|
| `br.gov.caixa.simtr.orquestrador` | POST `/simtr-hub/v1/monitoramentos-dossie` valida e publica a tentativa na fila de entrada. Retorna `202` com `monitoramentoId` e `orquestracaoId` após confirmação do broker. |
| `br.gov.caixa.simtr.monitoramento` | Listener inicia no startup quando `monitoramento.service-bus.entrada.consumo-habilitado=true`. Valida a entrega, consulta pré-validação/Hub, aplica política e limites; ignora, reagenda na entrada ou publica resultado na saída. |
| `br.gov.caixa.simtr.orquestrador` — resultado | Caso de uso e adapter registram log estruturado por CDI desde 9.1-A. Listener da saída conectado em 9.1-B com opt-in, validação, chamada e Complete/Abandon/DLQ; prova integrada no emulador concluída em 9.1-C. |

Filas padrão: `q.prevalidacao.monitoramento-mtr.in` e `q.prevalidacao.monitoramento-mtr.out`.
O consumo da entrada é **false por padrão**. A escolha entre Dev Services e Azure não o ativa.
A publicação de resultado não ocorre em todo POST: pré-validação inelegível vira no-op;
situação não conclusiva reagenda até limite; conclusão ou esgotamento publica na saída.
Com a flag da saída false, a fila aguarda consumidor e os testes anteriores a leem pelo harness.
Com a flag true, o listener do orquestrador recebe e conclui as mensagens após submissão do log.

## Roteiro para assumir a entrega

**Próximo item: 10.1**, correlação OpenTelemetry ponta a ponta.
C9.1-L foi aceito: submissão best-effort ao logger, sem confirmação do destino.
9.1-A/B/C concluídas tecnicamente, com ativação real da saída e fluxo até log/Complete
verificados no emulador. [Evidência e decisão](continuidade-9-1.md).
10.1/C3 e o encerramento humano da feature permanecem pendentes.
O [manifesto anterior](pacote-commit-8-1.md) registra os 42 arquivos publicados em b886bdb;
o [manifesto de 8.2](pacote-commit.md) preserva esse marco histórico. A seleção atual e a
publicação de 9.1 estão no [manifesto de 9.1](pacote-commit-9-1.md).

1. Conferir a branch `feature/orquestrador-monitoramento-service-bus` e os hashes local/remoto.
   Preservar alterações locais e baseline. Em outro checkout, seguir [AGENTS.md](../../../AGENTS.md)
   para inicializar a própria sessão; não copiar credenciais ou supor que existe estado Sonar local.
2. Ler [retomada](retomada.md), [plano](plan.md) e a primeira pendência do [checklist](todo.md).
   Registros de pausas e checkpoints antigos são históricos.
3. Executar os [roteiros de aplicação e testes](#verificação-rápida-do-marco-até-82).
   As suítes são distintas e sequenciais; `mvn test` continua sem broker.
4. Preservar o fluxo de 9.1 e detalhar 10.1: caracterizar a instrumentação efetiva do SDK antes
   de preencher lacunas de propagação/spans; consultar as
   [pendências](#pontos-a-fechar-antes-da-lógica-correspondente).
5. Atualizar plano/checklist antes da implementação e observar os checkpoints humanos aplicáveis.
   10.1 não foi executada durante o fechamento de 9.1.

| Marco | Uso da entrega |
|---|---|
| Até 8.1 — b886bdb | Processamento e reagendamento com início explícito; provas sem broker e com emulador |
| 8.2 — a797114 | Startup da entrada por configuração e POST até a fila de resultado, incluindo reagendamento |
| 9.1 — pacote atual | Fluxo funcional incluindo consumo/log do resultado, verificado no emulador com o Hub controlado |
| Após 10.1/C3 | Correlação de telemetria e revisão ponta a ponta de confirmação, falha e redelivery |

## Decisões para navegar e implementar este código

O [guia Service Bus](../../../doc/guias/guia-service-bus-amqp-dossie.md) é a leitura principal
para a conversa entre desenvolvedores: explica a arquitetura no mesmo runtime, os contratos,
os dois fluxos de colaboração e as decisões aceitas. Este inventário permite localizar os arquivos.

| Decisão aplicada | Consequência ao implementar | Referência |
|---|---|---|
| Orquestrador e monitoramento são componentes do monólito modular | Regras ficam em seus componentes; Hub/dossie existentes permanecem preservados | [ADR-0010](../../../doc/adr/0010-extensao-quarkus-service-bus-connection-string-dev-services.md) |
| Hexagonal pragmática permite CDI/Quarkus/Mutiny no núcleo | Não criar wrappers para esconder o framework; SDK, handles e DTOs de borda continuam fora do núcleo | [ADR-0001](../../../doc/adr/0001-monolito-modular-e-hexagonal.md), [ADR-0011](../../../doc/adr/0011-composicao-local-monitoramento-e-fabrica-service-bus.md) |
| Cada borda possui contratos próprios | Compatibilidade por JSON, sem DTO compartilhado entre REST, produtor, consumidor ou Hub | [ADR-0004](../../../doc/adr/0004-contratos-independentes-por-borda.md) |
| Colaboração local usa porta e ACL | Consultar o Hub pela API pública e obter parâmetros do monitoramento sem importar implementação/configuração | [ADR-0003](../../../doc/adr/0003-orquestracao-e-colaboracao-por-portas.md), [ADR-0011](../../../doc/adr/0011-composicao-local-monitoramento-e-fabrica-service-bus.md) |
| Factory única cria os clientes da extensão | Implementada em 6.1 com qualifiers e shutdown; adapters não criam clientes por mensagem | [ADR-0011](../../../doc/adr/0011-composicao-local-monitoramento-e-fabrica-service-bus.md) |
| SAS externa, extensão 1.2.5 e Dev Services local | Preservar Quarkus 3.33.2.1/JDK 25, profiles e config.json; não introduzir Entra/SDK direto alternativo | [ADR-0010](../../../doc/adr/0010-extensao-quarkus-service-bus-connection-string-dev-services.md) |
| Infraestrutura de logging transporta campos já sanitizados | DTO, classificação, identidade e sanitização continuam na borda; Hub é delegado sem alteração | [ADR-0012](../../../doc/adr/0012-campos-json-tipados-logs-service-bus.md) |
| Settlement e telemetria são comportamento observável | Provar operação real e instrumentação do SDK antes de declarar atomicidade ou acrescentar spans | [ADR-0006](../../../doc/adr/0006-compatibilidade-observabilidade-e-testes.md), [ADR-0010](../../../doc/adr/0010-extensao-quarkus-service-bus-connection-string-dev-services.md) |

A política, os contratos/mappers, os erros tipados e os guardrails de 4.1 estão prontos.
O mock de pré-validação e a ACL do Hub foram concluídos em 5.1. Factory, Resource, parâmetros,
iniciação e publisher da entrada funcionam desde 6.1. O publisher de resultado está implementado
em 7.1-A e é acionado pelo caso de uso de 7.1-B. O listener da entrada funciona em 7.1-C,
com início explícito e integração terminal verificada em 7.1-D. A 8.1 conecta o reagendamento
transacional. O log final está implementado em 9.1-A e conectado ao listener da saída em 9.1-B.
O checklist mostra dependências e checkpoints.

## Objetivo e ponto de partida

O [guia Service Bus](../../../doc/guias/guia-service-bus-amqp-dossie.md) apresenta o fluxo e os
contratos vigentes. Este arquivo complementa o guia com inventário Java e roteiro manual.
O nome histórico do guia não autoriza mudança no package `br.gov.caixa.simtr.dossie`.

**Estado da continuidade:** as duas consultas são beans CDI funcionais, com 70 testes novos
e 100% das linhas/condições das seis classes implementadas. Reagendamento e resultado mantêm
seus contratos/mappers verificados. As etapas em RED da pausa são históricas; regressão e
checkpoint atuais estão no checklist.

Este guia permite discutir a arquitetura olhando os arquivos Java e continuar a implementação
sem reconstruir o histórico da conversa. A feature está na branch
`feature/orquestrador-monitoramento-service-bus`.

Leia primeiro [plan.md](plan.md) para escopo e decisões e [todo.md](todo.md) para andamento,
verificações e decisões humanas. Este guia orienta o trabalho; o checklist continua sendo a fonte
do progresso. A antecipação estrutural foi solicitada pelo usuário no C4.4.

A estrutura contém 11 portas conectadas, incluindo reagendamento por entrega e as duas portas
de recebimento/registro implementadas em 9.1-A. MonitoramentoResultadoListener foi conectado
em 9.1-B; não restam esqueletos @Vetoed nos dois componentes.
Parâmetros, decisão, modelo de reagendamento e listener da entrada permanecem funcionais.

| Estado | O que significa |
|---|---|
| Implementado e verificado | Política, configuração/producer CDI, contratos REST e da fila de entrada, mappers e logs de erro dessas bordas, incluindo reagendamento e resultado, e as consultas de 5.1, publicação inicial de 6.1 e publisher da saída de 7.1-A já possuem implementação e testes |
| Implementação conectada | Consultas, parâmetros, iniciação, ambos os publishers e processamento resolvem por CDI; listener da entrada CDI aceita início explícito ou opt-in no startup |
| Interface declarada | A porta usa tipos do próprio componente; as onze possuem implementação conectada no estado atual |
| Parâmetros funcionais | Records independentes com `limiteEm` e `politicaMonitoramentoVersao`, calculados no monitoramento e traduzidos na ACL |
| Estrutura inativa (histórico) | A antecipação reservava nomes e responsabilidades; o último esqueleto foi substituído em 9.1-B |
| Pendente no checklist | Ter um arquivo Java não conclui o item funcional; isso exige comportamento, testes, revisão e checkpoint |

A antecipação estrutural é histórica: no estado atual, as onze portas têm implementação
funcional conectada. Para próximas etapas, acrescentar comportamento com testes e respeitar
os contratos existentes, sem introduzir retornos fictícios de sucesso.

## Arquitetura para a conversa com a equipe

O diagrama mostra o fluxo planejado completo. REST, iniciação, publicações, consultas,
política, processamento e listener da entrada estão implementados, incluindo no-op/settlement.
O listener aceita início explícito e, desde 8.2, ativação por configuração no startup.
Reagendamento está conectado em 8.1; caso de uso/log final em 9.1-A e listener/settlement da saída
em 9.1-B. Verde identifica implementação disponível; a saída foi verificada no emulador em 9.1-C.

```mermaid
flowchart TD
    REST["POST REST: orquestrador"] --> INICIAR["IniciarMonitoramentoUseCase"]
    INICIAR --> PUBIN["MonitoramentoEntradaPublisher"]
    PUBIN --> QIN[("q.prevalidacao.monitoramento-mtr.in")]
    QIN --> LIN["MonitoramentoEntradaListener: opt-in no startup"]
    LIN --> PROCESSAR["ProcessarTentativaMonitoramentoUseCase"]
    PROCESSAR --> PRE["Consultar pré-validação simulada"]
    PRE --> ELEGIVEL{"EM_ANALISE_ENVIO_MTR?"}
    ELEGIVEL -->|Não| NOOP["Registrar no-op e concluir entrada"]
    ELEGIVEL -->|Sim| LIMITE{"Prazo ou tentativas esgotados?"}
    LIMITE -->|Sim| QUARENTENA["Calcular resultado QUARENTENA"]
    LIMITE -->|Não| HUB["Consultar situação pela porta pública do Hub"]
    HUB --> CONCLUSIVO{"Situação conclusiva?"}
    CONCLUSIVO -->|Não| POLITICA["Política calcula próxima tentativa e intervalo"]
    POLITICA --> REAGENDAR["MonitoramentoReagendamentoAdapter"]
    REAGENDAR -->|"Agendar próxima e concluir atual na mesma transação"| QIN
    CONCLUSIVO -->|Sim| RESULTADO["Calcular resultado terminal"]
    RESULTADO --> PUBOUT["MonitoramentoResultadoPublisher"]
    QUARENTENA --> PUBOUT
    PUBOUT --> QOUT[("q.prevalidacao.monitoramento-mtr.out")]
    PUBOUT -->|"Após confirmação do broker"| COMPLETEIN["Concluir entrada"]
    QOUT --> LOUT["MonitoramentoResultadoListener: opt-in no startup"]
    LOUT --> RECEBER["ReceberResultadoMonitoramentoUseCase"]
    RECEBER --> LOG["ResultadoMonitoramentoLogAdapter"]
    LOG --> FIM["Concluir saída: fim do demonstrador"]
    classDef implementado fill:#dcfce7,stroke:#15803d,color:#14532d;
    classDef pendente fill:#f3f4f6,stroke:#6b7280,color:#374151;
    class REST,INICIAR,PUBIN,PRE,HUB,POLITICA,PUBOUT,LIN,PROCESSAR,ELEGIVEL,NOOP,LIMITE,QUARENTENA,CONCLUSIVO,RESULTADO,COMPLETEIN,REAGENDAR implementado;
    class LOUT,RECEBER,LOG,FIM implementado;
```

A fábrica `arquitetura.infraestrutura.servicebus.ClientesServiceBus` já fornece suporte técnico aos
adapters Service Bus. Ela não é um componente de negócio, um barramento genérico ou o destino de
toda lógica assíncrona.

| Componente | Responsabilidade | Dependências permitidas |
|---|---|---|
| `orquestrador.dominio` | Solicitação, identificadores e resultado próprio | Tipos do próprio domínio e suporte permitido pelo ADR-0001 |
| `orquestrador.aplicacao` | Iniciar e receber resultado | Domínio e portas próprios |
| `monitoramento.dominio` | Política, tentativa, prazo, situações e decisões | Tipos próprios e suporte permitido pelo ADR-0001 |
| `monitoramento.aplicacao` | Preparar parâmetros e coordenar processamento | Política, domínio e portas próprios |
| Adapters de entrada | Validar/mapear contrato e chamar porta de entrada | API do próprio componente; transporte apenas na borda |
| Adapters de saída | Implementar portas e traduzir integração | Porta/tipos do consumidor e API externa explicitamente permitida |
| ACL de parâmetros | Traduzir resultado do monitoramento | `PrepararMonitoramento` e seus tipos públicos, sem importar política/configuração |
| ACL do Hub | Traduzir identificador e situação mínima | `ConsultarDossieProduto` e tipos públicos referenciados, sem Resource/DTO MTR/caso de uso concreto |
| Infraestrutura Service Bus | Builder da extensão, clientes e lifecycle | SDK e suporte técnico; nenhum domínio ou componente de negócio |

Quarkus, CDI e Mutiny são permitidos no núcleo pela hexagonal pragmática. SDK Azure, DTO de
transporte, client, connection string e handles de settlement permanecem nas bordas.
Cada componente e borda conserva modelos/DTOs próprios, mesmo quando o JSON precisa ser compatível.

Referências: [arquitetura consolidada](../../../doc/arquitetura-distribuida/arquitetura-ddd-integracoes-atomicas.md),
[índice dos ADRs](../../../doc/adr/README.md),
[ADR-0003](../../../doc/adr/0003-orquestracao-e-colaboracao-por-portas.md),
[ADR-0004](../../../doc/adr/0004-contratos-independentes-por-borda.md),
[ADR-0011](../../../doc/adr/0011-composicao-local-monitoramento-e-fabrica-service-bus.md) e
[ADR-0012](../../../doc/adr/0012-campos-json-tipados-logs-service-bus.md).

## Plataforma e fluxo aprovados

A implementação usa a extensão Quarkus Azure Service Bus `1.2.5` com Quarkus `3.33.2.1`
e JDK 25. Ambientes reais usam `QUARKUS_AZURE_SERVICEBUS_CONNECTION_STRING` externa com SAS;
não implementar Entra ID. Dev Services fornece emulador/SQL e connection string local em dev
e nos testes de integração habilitados. Manter
`src/main/azure/servicebus-emulator/config.json` e os simuladores existentes.

O orquestrador escreve na entrada; o listener do monitoramento aciona a aplicação/política,
que decide no-op, resultado/quarentena na saída ou reagendamento na entrada. O orquestrador
consome a saída e conclui o demonstrador após o log. A preparação local de limite/versão do
ADR-0011 ocorre antes da primeira publicação e não substitui esse fluxo assíncrono.
Critérios, confirmação e contratos estão detalhados no guia Service Bus.
`dossie` e Hub permanecem preservados.

## Configuração v1 já implementada

A configuração pertence a `monitoramento` e está pronta desde 4.1.
Em [application.properties](../../../src/main/resources/application.properties):

```properties
monitoramento.politicas.ativa=padrao
monitoramento.politicas.definicoes.padrao.versao=v1
monitoramento.politicas.definicoes.padrao.tipo=progressiva
monitoramento.politicas.definicoes.padrao.intervalos=PT30M
monitoramento.politicas.definicoes.padrao.duracao-maxima=PT24H
# monitoramento.politicas.definicoes.padrao.max-tentativas=5
```

`padrao` é o nome da definição selecionada; `v1` é sua versão. O máximo de tentativas
está comentado: o valor 5 é exemplo, não um limite ativo. A duração máxima de 24 horas
continua obrigatória. O padrão usa somente 30 minutos e repete esse intervalo até o prazo.
A ausência de teto por contagem vale para o padrão; max-tentativas permanece opcional
para outras configurações, conforme confirmação humana. `DeliveryCount` não é tentativa funcional.

| Lista de intervalos configurada | Intervalos entre tentativas |
|---|---|
| `PT30M` (padrão) | 30 min → 30 min → 30 min → ... |
| `PT3H,PT4H,PT6H` | 3 h → 4 h → 6 h → 6 h → ... |

A primeira tentativa continua imediata. Os valores da lista são esperas entre tentativas,
não horários absolutos desde o início. O prazo original encerra o monitoramento; a lista
não reinicia essa janela. Para outra política, configurar a definição e sua seleção/versão
nos campos existentes. Nenhuma operação no broker é executada apenas por alterar os intervalos.

| Arquivo | Responsabilidade implementada |
|---|---|
| [PoliticasMonitoramentoConfig](../../../src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/configuracao/PoliticasMonitoramentoConfig.java) | `ConfigMapping` lê seleção, definições, versão, tipo, lista de `Duration`, limite opcional e duração máxima |
| [PoliticaMonitoramentoProducer](../../../src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/configuracao/PoliticaMonitoramentoProducer.java) | Valida todas as definições no bootstrap e fornece a política ativa e o catálogo por CDI |
| [CatalogoPoliticasMonitoramento](../../../src/main/java/br/gov/caixa/simtr/monitoramento/dominio/politica/CatalogoPoliticasMonitoramento.java) | Resolve a versão recebida ou fornece v1 padrão, expondo versão solicitada, política efetiva e indicador de recuperação |
| [PoliticaMonitoramentoProgressiva](../../../src/main/java/br/gov/caixa/simtr/monitoramento/dominio/politica/PoliticaMonitoramentoProgressiva.java) | Calcula limite inicial e decisão de encerrar ou próxima tentativa/intervalo |
| [PrepararMonitoramentoUseCase](../../../src/main/java/br/gov/caixa/simtr/monitoramento/aplicacao/casodeuso/PrepararMonitoramentoUseCase.java) | Usa a política para devolver limite e versão à composição inicial do orquestrador |

A seleção por nome continua preparando novos monitoramentos. O catálogo resolve a versão
de uma tentativa em curso independentemente dessa seleção. A definição recebida prevalece
quando configurada, mesmo inativa. Quando ausente, usa v1 interna com PT30M repetido,
PT24H e sem teto opcional de tentativas, conforme a direção humana atualizada em 2026-09-09. Não há quarentena
por versão ausente nem escrita automática em application.properties.

A v1 interna mantém valores fixos, mesmo se uma outra definição v1 personalizada existir.
Ela não recupera valores antigos que foram removidos. A resolução informa
`versaoSolicitada`, `politica` efetiva e `padraoAplicado`; o boolean diferencia recuperação
inclusive quando a mensagem também pede v1. Versões duplicadas e definições presentes
inválidas falham no bootstrap, evitando uma seleção ambígua.

Ao conectar o caso de uso, injetar o catálogo, chamar
`resolver(tentativa.politicaMonitoramentoVersao())` e avaliar a política com o limite recebido.
Preservar IDs, contador, início, limite e versão da mensagem; não chamar calcularLimite
para uma tentativa em curso. A duração padrão não reinicia nem amplia o prazo recebido.
Calcular o intervalo não agenda uma mensagem: a borda de 8.1 confirma schedule + Complete na mesma transação.
A decisão está no [ADR-0011](../../../doc/adr/0011-composicao-local-monitoramento-e-fabrica-service-bus.md).

## Inventário da estrutura antecipada

Todos os caminhos abaixo apontam para arquivos Java. A coluna final indica onde os campos,
a lógica ou a conexão deverão ser implementados. O Javadoc de cada classe informa sua função.

| Arquivo | Package após `br.gov.caixa.simtr` | Estado | Item funcional |
|---|---|---|---|
| [ParametrosMonitoramento](../../../src/main/java/br/gov/caixa/simtr/orquestrador/dominio/modelo/ParametrosMonitoramento.java) | `orquestrador.dominio.modelo` | Parâmetros funcionais | 6.1 |
| [ResultadoMonitoramento](../../../src/main/java/br/gov/caixa/simtr/orquestrador/dominio/modelo/ResultadoMonitoramento.java) | `orquestrador.dominio.modelo` | Implementado e verificado; regra de quarentena/conclusivo confirmada | 4.1 |
| [ParametrosMonitoramento](../../../src/main/java/br/gov/caixa/simtr/monitoramento/dominio/modelo/ParametrosMonitoramento.java) | `monitoramento.dominio.modelo` | Parâmetros funcionais | 6.1 |
| [ResultadoMonitoramento](../../../src/main/java/br/gov/caixa/simtr/monitoramento/dominio/modelo/ResultadoMonitoramento.java) | `monitoramento.dominio.modelo` | Implementado e verificado; regra de quarentena/conclusivo confirmada | 4.1 |
| [PreValidacaoConsultada](../../../src/main/java/br/gov/caixa/simtr/monitoramento/dominio/modelo/PreValidacaoConsultada.java) | `monitoramento.dominio.modelo` | Implementado e verificado | 5.1 |
| [SituacaoDossieConsultada](../../../src/main/java/br/gov/caixa/simtr/monitoramento/dominio/modelo/SituacaoDossieConsultada.java) | `monitoramento.dominio.modelo` | Implementado e verificado | 5.1 |
| [DecisaoProcessamento](../../../src/main/java/br/gov/caixa/simtr/monitoramento/dominio/modelo/DecisaoProcessamento.java) | `monitoramento.dominio.modelo` | Modelo funcional com decisões imutáveis | 7.1-B |
| [ReagendamentoMonitoramento](../../../src/main/java/br/gov/caixa/simtr/monitoramento/dominio/modelo/ReagendamentoMonitoramento.java) | `monitoramento.dominio.modelo` | Implementado | 8.1 |
| [IniciarMonitoramento](../../../src/main/java/br/gov/caixa/simtr/orquestrador/aplicacao/porta/entrada/IniciarMonitoramento.java) | `orquestrador.aplicacao.porta.entrada` | Implementação conectada | 6.1 |
| [ReceberResultadoMonitoramento](../../../src/main/java/br/gov/caixa/simtr/orquestrador/aplicacao/porta/entrada/ReceberResultadoMonitoramento.java) | `orquestrador.aplicacao.porta.entrada` | Conectada por CDI | 9.1-A |
| [ObterParametrosMonitoramento](../../../src/main/java/br/gov/caixa/simtr/orquestrador/aplicacao/porta/saida/ObterParametrosMonitoramento.java) | `orquestrador.aplicacao.porta.saida` | Implementação conectada | 6.1 |
| [PublicarTentativaMonitoramento](../../../src/main/java/br/gov/caixa/simtr/orquestrador/aplicacao/porta/saida/PublicarTentativaMonitoramento.java) | `orquestrador.aplicacao.porta.saida` | Implementação conectada | 6.1 |
| [RegistrarResultadoMonitoramento](../../../src/main/java/br/gov/caixa/simtr/orquestrador/aplicacao/porta/saida/RegistrarResultadoMonitoramento.java) | `orquestrador.aplicacao.porta.saida` | Conectada por CDI | 9.1-A |
| [PrepararMonitoramento](../../../src/main/java/br/gov/caixa/simtr/monitoramento/aplicacao/porta/entrada/PrepararMonitoramento.java) | `monitoramento.aplicacao.porta.entrada` | Implementação conectada | 6.1 |
| [ProcessarTentativaMonitoramento](../../../src/main/java/br/gov/caixa/simtr/monitoramento/aplicacao/porta/entrada/ProcessarTentativaMonitoramento.java) | `monitoramento.aplicacao.porta.entrada` | Implementação conectada por CDI | 7.1-B |
| [ConsultarPreValidacao](../../../src/main/java/br/gov/caixa/simtr/monitoramento/aplicacao/porta/saida/ConsultarPreValidacao.java) | `monitoramento.aplicacao.porta.saida` | Implementação conectada | 5.1 |
| [ConsultarSituacaoDossie](../../../src/main/java/br/gov/caixa/simtr/monitoramento/aplicacao/porta/saida/ConsultarSituacaoDossie.java) | `monitoramento.aplicacao.porta.saida` | Implementação conectada | 5.1 |
| [PublicarResultadoMonitoramento](../../../src/main/java/br/gov/caixa/simtr/monitoramento/aplicacao/porta/saida/PublicarResultadoMonitoramento.java) | `monitoramento.aplicacao.porta.saida` | Implementação conectada por CDI | 7.1-A |
| [ReagendarTentativaMonitoramento](../../../src/main/java/br/gov/caixa/simtr/monitoramento/aplicacao/porta/saida/ReagendarTentativaMonitoramento.java) | `monitoramento.aplicacao.porta.saida` | Implementação associada por entrega | 8.1 |
| [IniciarMonitoramentoUseCase](../../../src/main/java/br/gov/caixa/simtr/orquestrador/aplicacao/casodeuso/IniciarMonitoramentoUseCase.java) | `orquestrador.aplicacao.casodeuso` | Implementado e verificado | 6.1 |
| [ReceberResultadoMonitoramentoUseCase](../../../src/main/java/br/gov/caixa/simtr/orquestrador/aplicacao/casodeuso/ReceberResultadoMonitoramentoUseCase.java) | `orquestrador.aplicacao.casodeuso` | Delega à porta e aguarda submissão ao logger | 9.1-A |
| [MonitoramentoDossieResource](../../../src/main/java/br/gov/caixa/simtr/orquestrador/adaptador/entrada/rest/v1/MonitoramentoDossieResource.java) | `orquestrador.adaptador.entrada.rest.v1` | Implementado e verificado | 6.1 |
| [MonitoramentoResultadoListener](../../../src/main/java/br/gov/caixa/simtr/orquestrador/adaptador/entrada/servicebus/MonitoramentoResultadoListener.java) | `orquestrador.adaptador.entrada.servicebus` | Consumo serial, settlement e startup por opt-in | 9.1-B |
| [MonitoramentoEntradaPublisher](../../../src/main/java/br/gov/caixa/simtr/orquestrador/adaptador/saida/servicebus/MonitoramentoEntradaPublisher.java) | `orquestrador.adaptador.saida.servicebus` | Implementado e verificado | 6.1 |
| [ResultadoMonitoramentoLogAdapter](../../../src/main/java/br/gov/caixa/simtr/orquestrador/adaptador/saida/log/ResultadoMonitoramentoLogAdapter.java) | `orquestrador.adaptador.saida.log` | Log estruturado best-effort, uma vez por invocação | 9.1-A |
| [ParametrosMonitoramentoAcl](../../../src/main/java/br/gov/caixa/simtr/orquestrador/adaptador/saida/acl/monitoramento/ParametrosMonitoramentoAcl.java) | `orquestrador.adaptador.saida.acl.monitoramento` | Implementado e verificado | 6.1 |
| [PrepararMonitoramentoUseCase](../../../src/main/java/br/gov/caixa/simtr/monitoramento/aplicacao/casodeuso/PrepararMonitoramentoUseCase.java) | `monitoramento.aplicacao.casodeuso` | Implementado e verificado | 6.1 |
| [ProcessarTentativaMonitoramentoUseCase](../../../src/main/java/br/gov/caixa/simtr/monitoramento/aplicacao/casodeuso/ProcessarTentativaMonitoramentoUseCase.java) | `monitoramento.aplicacao.casodeuso` | Implementação conectada por CDI | 7.1-B |
| [MonitoramentoEntradaListener](../../../src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/entrada/servicebus/MonitoramentoEntradaListener.java) | `monitoramento.adaptador.entrada.servicebus` | Listener CDI funcional, opt-in no startup | 7.1-C / 8.2 |
| [PreValidacaoSimuladaAdapter](../../../src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/simulador/prevalidacao/PreValidacaoSimuladaAdapter.java) | `monitoramento.adaptador.saida.simulador.prevalidacao` | Implementado e verificado | 5.1 |
| [PreValidacaoSimuladaDto](../../../src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/simulador/prevalidacao/dto/PreValidacaoSimuladaDto.java) | `monitoramento.adaptador.saida.simulador.prevalidacao.dto` | Implementado e verificado; exclusivo da borda | 5.1 |
| [PreValidacaoSimuladaMapper](../../../src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/simulador/prevalidacao/PreValidacaoSimuladaMapper.java) | `monitoramento.adaptador.saida.simulador.prevalidacao` | Implementado e verificado; situação original e origem simulada | 5.1 |
| [SituacaoDossieHubAcl](../../../src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/acl/simtrhub/SituacaoDossieHubAcl.java) | `monitoramento.adaptador.saida.acl.simtrhub` | Implementado e verificado | 5.1 |
| [MonitoramentoResultadoPublisher](../../../src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/servicebus/MonitoramentoResultadoPublisher.java) | `monitoramento.adaptador.saida.servicebus` | Implementado; confirmação, falhas e integração verificadas | 7.1-A |
| [MonitoramentoReagendamentoAdapter](../../../src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/servicebus/MonitoramentoReagendamentoAdapter.java) | `monitoramento.adaptador.saida.servicebus` | Implementado | 8.1 |
| [ClientesServiceBus](../../../src/main/java/br/gov/caixa/simtr/arquitetura/infraestrutura/servicebus/ClientesServiceBus.java) | `arquitetura.infraestrutura.servicebus` | Implementado e verificado | 6.1 |
| [MonitorarDossieMtrV1](../../../src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/servicebus/dto/MonitorarDossieMtrV1.java) | `monitoramento.adaptador.saida.servicebus.dto` | Implementado; testes de reagendamento em GREEN | 4.1 |
| [MonitoramentoReagendamentoServiceBusMapper](../../../src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/servicebus/MonitoramentoReagendamentoServiceBusMapper.java) | `monitoramento.adaptador.saida.servicebus` | Implementado; testes de reagendamento em GREEN | 4.1 |
| [ResultadoMonitoramentoDossieMtrV1](../../../src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/servicebus/dto/ResultadoMonitoramentoDossieMtrV1.java) | `monitoramento.adaptador.saida.servicebus.dto` | Implementado e verificado; regra de quarentena/conclusivo confirmada | 4.1 |
| [MonitoramentoResultadoServiceBusMapper](../../../src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/servicebus/MonitoramentoResultadoServiceBusMapper.java) | `monitoramento.adaptador.saida.servicebus` | Implementado e verificado; regra de quarentena/conclusivo confirmada | 4.1 |
| [ResultadoMonitoramentoDossieMtrV1](../../../src/main/java/br/gov/caixa/simtr/orquestrador/adaptador/entrada/servicebus/dto/ResultadoMonitoramentoDossieMtrV1.java) | `orquestrador.adaptador.entrada.servicebus.dto` | Implementado e verificado; regra de quarentena/conclusivo confirmada | 4.1 |
| [MonitoramentoResultadoServiceBusMapper](../../../src/main/java/br/gov/caixa/simtr/orquestrador/adaptador/entrada/servicebus/MonitoramentoResultadoServiceBusMapper.java) | `orquestrador.adaptador.entrada.servicebus` | Implementado e verificado; regra de quarentena/conclusivo confirmada | 4.1 |

Os mappers, DTOs de entrada, política e configuração anteriores a C4.4 permanecem implementados.
Não substituí-los por esqueletos. Os futuros listeners/publishers devem usar o padrão de erro das bordas
já verificado em 4.1, com testes do JSON emitido.

Qualifiers implementados: [FilaEntrada](../../../src/main/java/br/gov/caixa/simtr/arquitetura/infraestrutura/servicebus/FilaEntrada.java)
e [FilaSaida](../../../src/main/java/br/gov/caixa/simtr/arquitetura/infraestrutura/servicebus/FilaSaida.java).
O [ErroInicioMonitoramentoDto](../../../src/main/java/br/gov/caixa/simtr/orquestrador/adaptador/entrada/rest/v1/dto/ErroInicioMonitoramentoDto.java)
pertence exclusivamente à borda REST e mantém o formato de erro existente sem importar o Hub.

## Ordem de continuidade

| Passo | Trabalho | Evidência para avançar |
|---|---|---|
| 4.1-E1 | Estrutura Java, portas e proteção de inatividade | Compilação, ArchUnit, CDI, regressão e checkpoint do incremento |
| 4.1-E2 | Conferir este guia e o manifesto do pacote de commit | Links, inventário e estado alinhados ao código/checklist |
| 4.1 — reagendamento | DTO/mapper implementados na saída do monitoramento, sem agendar | JSON/AMQP compatível com a entrada; valores iniciais preservados; erro JSON |
| 4.1 — resultado | Modelos, DTOs/mappers, logs e validação confirmada implementados | 120 testes de contrato, 14 de logs; JSON e quarentena null/zero verificados |
| 4.1 — guardrails | Isolamento de bordas e acesso público pelas ACLs implementados | 19 testes de fronteira após inclusão da borda do simulador em 5.1; provas positivas/negativas e regressão estrutural aprovadas |
| 5.1 — concluído | Consulta simulada da pré-validação e ACL do Hub | 70 testes novos, tradução mínima, cenários controlados, CDI e guardrails |
| 6.1 — implementado | Parâmetros locais, fábrica, POST, caso de uso e publisher | `202` após confirmação, contrato REST, erros, testes sem broker e integração explícita aprovados |
| **C2 — aceito em 2026-09-09** | Revisão humana da publicação inicial e correção C2-R1 | Aceite explícito do usuário; próximo item funcional: 7.1 |
| 7.1-A — concluída | Publisher de resultado pela porta CDI | Confirmação, falhas, testes sem broker e integração explícita; checkpoint COMPLIANT |
| **7.1-B — implementada** | Caso de uso CDI, catálogo, classificação e publicação terminal confirmada | No-op, limites, sequência, falhas, prazo original e intenção de reagendamento verificados sem broker |
| **7.1-C — implementada** | Listener com início explícito, settlement e lifecycle | 99 testes focados sem broker; publicação antes de Complete, falhas sem segundo settlement, shutdown, logs mínimos e consumo automático inativo |
| **7.1-D — concluída tecnicamente** | Integração do processamento terminal | Nove cenários novos, 15 integrações no perfil completo; 1.268 testes sem broker e Sonar COMPLIANT |
| **8.1 — concluída tecnicamente** | Reagendamento transacional | Cinco correções Sonar verificadas, 68 testes focados e 1.293 padrão; COMPLIANT, evidências na continuidade |
| 8.2 | Ativação da entrada no startup por configuração | Default inativo, POST até resultado/reagendamento, falhas e shutdown |
| 9.1 — concluída tecnicamente | Listener de resultado e registro | Provas sem broker, ativação real até log/Complete e DLQ no emulador; limites em C9.1-L |
| 10.1/C3 | Correlação e fluxo ponta a ponta | Propagação, spans e emulador, com limites registrados |
| 11.1–CF | Consolidação e revisão final | Suíte, Sonar, documentação, revisão e encerramento humano |

O [manifesto](pacote-commit.md) descreve o marco publicado de 8.2; 9.1 permanece local. A [evidência de 6.1](continuidade-6-1.md) e a
[revisão C2](revisao-c2.md) fundamentam o aceite já registrado. A
[continuidade de 8.1](continuidade-8-1.md) registra o fechamento e o próximo item 9.1.
A atualização dos documentos não executa publicação Git.
O [roteiro principal](../../../doc/guias/guia-service-bus-amqp-dossie.md#ponto-de-partida-para-o-desenvolvedor)
detalha a sequência de processamento, as dependências de negócio e as verificações para 7.1.

## Como ler e manter o Javadoc

O Javadoc dos tipos antecipados diferencia o estado atual da implementação esperada. Nas classes
inativas, descreve responsabilidade, item funcional, dependências permitidas e cenários a testar.
Nos métodos das portas, `@param` identifica a entrada e `@return` explica a conclusão esperada,
incluindo confirmação/falha nas operações assíncronas. `@see` e `{@link ...}` ligam tipos.

Exemplos para abrir na IDE:
[IniciarMonitoramento](../../../src/main/java/br/gov/caixa/simtr/orquestrador/aplicacao/porta/entrada/IniciarMonitoramento.java)
e [MonitoramentoReagendamentoAdapter](../../../src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/servicebus/MonitoramentoReagendamentoAdapter.java).

As classes vazias ainda não possuem métodos declarados para documentar: sua orientação fica no
Javadoc da classe. Não criar métodos fictícios, construtores vazios explícitos ou exceções
contratuais apenas para preencher a documentação. Ao implementar a fatia, documentar métodos e
construtores reais e atualizar o texto que identificava a pendência. Usar `@throws` somente para
exceções efetivamente previstas pelo contrato; falha emitida por `Uni` deve ser explicada no
retorno/comportamento, sem inventar declaração checked.

Seguir a [especificação Javadoc do JDK 25](https://docs.oracle.com/en/java/javase/25/docs/specs/javadoc/doc-comment-spec.html):
descrição antes dos block tags, nomes de parâmetros iguais aos da assinatura e referências Java
resolvíveis. Não usar tags personalizadas sem suporte configurado.

O complemento de comentários foi conferido com DocLint e saída temporária, sem HTML: nenhum erro;
27 avisos apenas de construtores implícitos sem comentário. Esses construtores foram preservados,
e deverão ser documentados quando forem explicitamente necessários na implementação.
A comparação antes/depois confirmou preservação de todo conteúdo Java fora dos Javadocs.
Nesta edição exclusivamente documental, não houve Maven, baseline, API ou checkpoint Sonar;
a evidência de 859 testes refere-se à estrutura antes desse complemento.

## Como implementar uma classe pendente

1. Conferir o próximo item do `todo.md`, os critérios do `plan.md` e os ADRs aplicáveis.
   Registrar qualquer alteração de contrato, responsabilidade, segurança ou observabilidade antes
   do código, com o checkpoint humano exigido por [AGENTS.md](../../../AGENTS.md).
2. Selecionar uma fatia pequena, com arquivos e comportamento concretos. Não preencher todos os
   esqueletos de uma camada em uma única alteração.
3. Definir cenários e testes de regressão do comportamento que falta. Nesta feature, a orientação
   humana prioriza implementação e cobertura sem exigir uma ordem rígida de TDD.
4. Completar os tipos/DTOs apenas na sua borda. Para casos de uso e adapters de saída, implementar
   a interface indicada no Javadoc. Usar injeção pelas portas; não pelo caso de uso concreto.
5. Implementar o mínimo necessário. Manter SDK/serialização/settlement nos adapters, sem bloqueio
   do event loop. A fábrica usa exclusivamente o builder fornecido pela extensão.
6. Retirar `@Vetoed` somente da classe cuja ativação faz parte da fatia. Aplicar CDI/REST/observer
   somente quando o fluxo correspondente estiver pronto e autorizado. Modelos e DTOs não precisam
   tornar-se beans.
7. Atualizar o Javadoc e as provas de comportamento/injeção real. O inventário temporário de
   esqueletos encerrou em 9.1-B; ComponentesResultadoMonitoramento e ResultadoMonitoramentoCdiTest
   agora verificam os três beans de resultado. Preservar as provas e os guardrails existentes.
8. Executar testes focados, revisar o diff e concluir o checkpoint coerente. Atualizar o checklist,
   este inventário e o consolidado quando o estado implementado mudar.

`@Vetoed` impede que beans e observers da classe sejam instalados pelo CDI; não é controle de
autorização e não impede instanciação manual. A proteção anterior verificava esqueletos sem
métodos de execução ou anotações REST; esses componentes agora têm implementação e provas CDI.
Fonte: [Jakarta CDI — Vetoed](https://jakarta.ee/specifications/cdi/4.1/apidocs/jakarta/enterprise/inject/vetoed).

## Contratos e tratamento de erros a preservar

- `idDossieMtr` continua string decimal positiva no intervalo de `Long`, com zeros à esquerda
  preservados. A conversão para o identificador do Hub está implementada na ACL de 5.1.
- Reagendamento conserva `iniciadoEm`, `limiteEm` e versão da política; não recalcular a janela.
- Não reutilizar DTO REST, MTR, simulador ou DTO Java do outro componente para reduzir duplicação.
  A compatibilidade deve ser provada pelo JSON produzido/consumido.
- A falha de contrato/serialização deve produzir o log JSON aprovado, com diagnóstico sanitizado,
  código estável e mesmo id/código propagado pela exceção. O produtor mantém RuntimeException.
- As bordas futuras devem seguir o padrão das bordas de entrada existentes. Não introduzir
  `catch` que engole a falha nem emissão duplicada do mesmo erro em todas as camadas.
- Registro para ação posterior continua sendo log. Persistência, ação automática, Outbox e
  generalização dos erros/logs do Hub permanecem fora do recorte.

## Pontos a fechar antes da lógica correspondente

**Resultado e guardrails de 4.1 já estão concluídos.** Os modelos, DTOs e mappers de resultado
estão implementados, incluindo a validação de quarentena/conclusivo.
[FronteirasMonitoramentoArchUnitTest](../../../src/test/java/br/gov/caixa/simtr/arquitetura/guardrails/FronteirasMonitoramentoArchUnitTest.java)
já verifica acesso das ACLs somente às portas/modelos públicos, isolamento dos DTOs por borda
e direção das dependências, com provas positivas e negativas. Preservar essas regras e
verificá-las sobre as implementações funcionais introduzidas nas próximas fatias.

1. **Classificação implementada em 7.1-B:** reutilizar o caso de uso e a tabela literal abaixo.
   A situação MTR é preservada; pré-validação é calculada separadamente. A ACL preserva
   id/nome; a fixture `1 / Rascunho` não fornece IDs para os estados conclusivos.
2. **Lifecycle preservado:** o listener da entrada cancela antes da factory de 6.1, como
   verificado em 7.1-C/D. O listener da saída preserva a mesma ordem desde 9.1-B; a factory
   continua dona dos quatro clientes e de seu fechamento idempotente.
3. **Versão resolvida em 7.1-B:** reutilizar a definição recebida quando configurada e v1 padrão
   quando ausente. O caso de uso mantém a tentativa original e evidencia versão efetiva/padrão;
   não reapresentar essa decisão nem recalcular a janela.
4. **Transação implementada em 8.1:** ReagendamentoMonitoramento limita o horário ao prazo
   original. O listener associa a entrega a uma implementação da porta no adapter, sem SDK
   no núcleo nem contexto mutável em singleton. A prova confirma schedule + Complete,
   rollback/redelivery e o fluxo até teto/prazo. Falha de commit não provoca outra liquidação.
5. **Saída em 9.1:** os três componentes estão implementados em 9.1-A/B. A flag da saída é
   independente; Complete sucede a submissão do log. Campos do evento final usam mdc e o erro
   do listener usa a composição já elegível do ADR-0012, sem alterar o Hub/formatter.
   Startup, log, settlement e DLQ foram verificados no emulador em 9.1-C.
6. **Telemetria em 10.1:** caracterizar a instrumentação efetiva do SDK, revisar os nomes
   observáveis planejados e preencher apenas lacunas de propagação/spans, conforme o guia principal.

### Situações informadas e diferença para o contrato atual

Direção literal do usuário em 2026-09-09:

| Situação original informada pelo Hub | Situação de pré-validação a calcular |
|---|---|
| `FINALIZADO_CONFORME` | `CONFORME` |
| `FINALIZADO_INCONFORME` | `INCONFORME` |
| `PENDENTE_INFORMACA` | `INCONFORME` |

A situação MTR recebida deve ser preservada. A retomada aplica literalmente a direção
humana registrada no [plano](plan.md#direção-humana-para-situações-do-hub--2026-09-09):
`INCONFORME` não foi trocado por `NAO_CONFORME`, nem `PENDENTE_INFORMACA` por
`PENDENTE_INFORMACAO`. Não foi inferida uma nova grafia ou identificação numérica.

Os dois DTOs agora aceitam em CONCLUSIVO os três nomes originais da tabela. Também
preservam `CONFORME`, `NAO_CONFORME` e `PENDENTE_INFORMACAO` por compatibilidade com
o contrato publicado. A prova entre produtor/consumidor verifica os seis pares sem
recalcular valores, mantendo exigência de consulta/contador positivo e quarentena null/zero.
O contrato passou de 120 para 123 cenários; a regressão focada passou com 181 testes sem broker.

O caso de uso e seu Javadoc implementam a classificação funcional de 7.1-B.
Os testes de contrato transportam situações já calculadas pela fixture; os testes do caso
de uso provam a regra de negócio. O listener da entrada executa settlement e, desde 8.2, admite opt-in no startup.

A regra de versão já foi definida: usar a definição recebida quando configurada e v1
padrão quando ausente, sem quarentena por esse motivo. Catálogo e producer implementam
a resolução, já conectada ao processamento. Não reapresentar a escolha.
O consumo da entrada permanece desabilitado por padrão; 8.2 permite ativação explícita por configuração no startup.
ReagendamentoPendente executa a transação de 8.1: agendar próxima e concluir atual.
Abandon continua restrito à falha técnica anterior à liquidação.

## Aplicação local e testes sem broker

A escolha local segue o [guia principal](../../../doc/guias/guia-service-bus-amqp-dossie.md#escolha-local-do-desenvolvedor-e-execução-dos-testes):

| Uso | Comando |
|---|---|
| Testes padrão, sem emulador nem fila Azure | `mvn test` |
| Somente integração explícita com emulador | `mvn -Pservicebus-integration test` |
| Aplicação local no emulador | `mvn quarkus:dev` |
| Aplicação local nas filas Azure configuradas | `mvn quarkus:dev "-Dquarkus.profile=dev,azure"` |

Emulador exige Docker e sessão sem connection string/namespace externo. Para Azure, fornecer
externamente `QUARKUS_AZURE_SERVICEBUS_CONNECTION_STRING`, `SERVICE_BUS_INPUT_QUEUE` e
`SERVICE_BUS_OUTPUT_QUEUE`; Dev Services fica desabilitado, com `AMQP_WEB_SOCKETS`.
Credenciais nunca entram nos argumentos, arquivos ou logs. O uso de Azure real não foi testado
nesta entrega; os 27 casos de integração, em sete classes, usam exclusivamente o emulador.

Surefire exclui a tag `servicebus-integration` por padrão; o profile Maven seleciona apenas
essa tag. Testes locais usam mocks/stubs, mesmo quando usam `@QuarkusTest` para CDI/cobertura.
A extensão e Dev Services permanecem desabilitados na configuração comum de testes.
O profile de integração fixa `test` e verifica as fontes efetivas do Quarkus antes do bootstrap,
incluindo arquivos e `%test`. Conexão/namespace presentes são rejeitados sem expandir expressões;
falha de leitura impede a execução com erro fixo sem causa. A correção C2-R1 possui 11 testes
puros, sem iniciar Quarkus ou broker. O checkpoint Sonar verifica a suíte padrão; a integração
tem evidência separada. Ver [revisão e correção C2](revisao-c2.md).

### Verificação rápida do marco até 8.2

Com Java 25 e Maven, executar as suítes sequencialmente na raiz:

```powershell
mvn clean verify
mvn -q -Pservicebus-integration clean test
```

A primeira executa a suíte padrão sem broker e o build. A segunda seleciona somente as
integrações e exige Docker, sem configuração externa de Service Bus. Contagens, datas e
checkpoint atual ficam na [continuidade de 8.2](continuidade-8-2.md).

Para provar especificamente a ativação pelo startup e os dois fluxos via POST:

```powershell
mvn -q -Pservicebus-integration "-Dtest=MonitoramentoAtivacaoEmuladorTest" test
```

O teste habilita a configuração antes do bootstrap, usa o REST real e controla somente
a porta pública do Hub. Não importa, injeta ou inicia o listener. SDK, emulador, caso de uso,
mappers e publishers são reais. Um cenário conclui na primeira consulta; o outro reagenda
e conclui na segunda, com intervalo curto exclusivo do profile. O harness lê/confirma a
saída para verificar o resultado; isso não implementa o consumidor de 9.1.
Os 12 cenários de MonitoramentoTerminalEmuladorTest continuam disponíveis com início Java
controlado e cobrem também no-op, prazo, máximo, versão removida, DLQ e Abandon/redelivery.

### Executar manualmente com Dev Services

Pré-requisitos: Java 25, Maven, Docker e as configurações de desenvolvimento do
[README](../../../README.md#execucao-local), incluindo `SIMTR_API_KEY`,
`SIMTR_OIDC_CLIENT_SECRET` e `SIMTR_OIDC_INTERNET_CLIENT_SECRET` fornecidas externamente.
Essas variáveis configuram o runtime e as integrações do Hub; não são credenciais do POST.
O Resource atual de monitoramento não exige Bearer.

Na sessão do emulador, não definir connection string/namespace externos de Service Bus.
As filas padrão são criadas pelo `config.json` existente. Executar:

```powershell
mvn quarkus:dev -Ddebug=false "-Dmonitoramento.service-bus.entrada.consumo-habilitado=true" "-Dmonitoramento.simulador.prevalidacao.habilitado=true"
```

A primeira flag ativa o listener da entrada no startup; a segunda ativa somente o mock de
pré-validação. O Hub tem configuração separada: no profile dev, o simulador de dossiê já está
habilitado por `%dev.simtr-hub.simulador.dossie-produto.habilitado=true`.

Em outro terminal PowerShell, publicar a solicitação:

```powershell
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/simtr-hub/v1/monitoramentos-dossie' -ContentType 'application/json' -Body '{"idDossiePreValidacao":"pre-em-analise","idDossieMtr":"4324680"}'
```

Também é possível usar o Swagger em `http://localhost:8080/simtr-hub/doc`.
Esperar status `202` com dois IDs gerados pelo servidor. O exemplo usa **4324680**, a fixture
existente no simulador do Hub, cuja situação é `Rascunho`. O ID `0007` dos testes de integração
é próprio da prova com a consulta do Hub controlada; não possui fixture para este roteiro manual.

Com os defaults `PT30M/PT24H`, esperar decisão `REAGENDAR`, confirmação
`complete_transacional` e uma próxima tentativa agendada. Não esperar saída terminal imediata.
Para observar a saída por limite de tentativas mais rapidamente, iniciar a aplicação com
estes overrides locais já suportados pela política:

```powershell
mvn quarkus:dev -Ddebug=false "-Dmonitoramento.service-bus.entrada.consumo-habilitado=true" "-Dmonitoramento.simulador.prevalidacao.habilitado=true" "-Dmonitoramento.politicas.definicoes.padrao.intervalos=PT5S" "-Dmonitoramento.politicas.definicoes.padrao.duracao-maxima=PT1M" "-Dmonitoramento.politicas.definicoes.padrao.max-tentativas=3"
```

Executar o mesmo POST depois do novo startup. Mantidas as respostas `Rascunho` e o serviço
operacional, a terceira consulta deve publicar `QUARENTENA/MAXIMO_TENTATIVAS` na saída,
com `tentativasRealizadas=3`, seguida do Complete da entrada. Os intervalos são mínimos
de agendamento; o tempo real inclui processamento e entrega do broker. Se o prazo vencer
antes, o motivo será `PRAZO_MAXIMO`. Os overrides não alteram os defaults versionados.
Este roteiro até 8.2 habilita somente a entrada; para consumir/logar a saída, usar a flag
adicional indicada na seção de 9.1-B. Não há endpoint de leitura da saída.
A comprovação automatizada
do conteúdo da saída é a integração com emulador indicada acima.

### Executar manualmente apontando para filas Azure

Fornecer pelo ambiente `QUARKUS_AZURE_SERVICEBUS_CONNECTION_STRING` (SAS com permissões
de envio/consumo nas filas), `SERVICE_BUS_INPUT_QUEUE` e `SERVICE_BUS_OUTPUT_QUEUE`.
As duas filas devem existir; o Dev Services não as provisiona no Azure. Manter os demais
pré-requisitos de runtime do roteiro anterior e não copiar segredos para argumentos ou Git.

```powershell
mvn quarkus:dev -Ddebug=false "-Dquarkus.profile=dev,azure" "-Dmonitoramento.service-bus.entrada.consumo-habilitado=true" "-Dmonitoramento.simulador.prevalidacao.habilitado=true"
```

O profile azure desabilita Dev Services e usa `AMQP_WEB_SOCKETS`; o consumo usa as filas
fornecidas. O POST e as regras são os mesmos. Os três overrides de política curta também
podem ser acrescentados a esse comando para a demonstração controlada.

Escolher Azure para o broker **mantém os simuladores de negócio do profile dev**. Para consultar
um Hub/MTR real, configurar essa integração separadamente e desabilitar explicitamente
`simtr-hub.simulador.dossie-produto.habilitado`; isso não foi verificado neste marco.
A configuração Azure está disponível, mas as 23 integrações executadas usam apenas o emulador.

### Como interpretar as evidências e falhas

O `202` comprova publicação inicial, não conclusão do monitoramento. No-op não publica saída;
reagendamento confirma schedule + Complete na mesma transação. Resultado terminal/quarentena
é publicado antes do Complete, mas essas duas operações não são atômicas e podem gerar
duplicidade após redelivery. Ainda não há Outbox ou idempotência durável.

Falha síncrona ao obter o cliente impede o startup com diagnóstico fixo. Falha assíncrona
de consumo/settlement encerra a assinatura e requer reinício da aplicação; HTTP pode continuar
respondendo. Não há retry, reconexão ou health check adicionais do listener. O shutdown cancela
a assinatura antes de fechar os clientes. O profile test fixa consumo false; somente o profile
opt-in da integração de ativação substitui por true.

O teste Quarkus com emulador verifica startup e HTTP reais, controla a porta pública do Hub
e lê/confirma a saída no harness. O roteiro manual foi conferido contra código e configuração;
dev mode interativo, Azure gerenciado e consulta MTR real não foram executados nesta consolidação.

## Publicação inicial implementada em 6.1

O Resource valida o request e chama somente `IniciarMonitoramento`. Na assinatura do `Uni`,
o caso de uso gera UUIDs/instante, obtém limite/versão pela ACL e monta tentativa 1. O publisher
usa o mapper existente e o sender qualificado da entrada. O `202` depende da confirmação;
falha retorna `500/ARVDOCP9999` com mensagem genérica e DTO próprio, sem causa do broker.
Validação mantém `400/ARVDOCP0001`; nenhuma validação ou capacidade do Hub foi alterada.

O mesmo `Uni` compartilha IDs/confirmação entre assinantes, sem novo envio. Uma nova requisição
é uma nova iniciação; não há idempotência durável entre requisições nem Outbox.
Só a fábrica depende da flag de ativação da extensão. Sem cliente disponível, o Resource
continua registrado e a publicação falha explicitamente; não simula sucesso.
Logs/spans manuais e caracterização da telemetria automática permanecem na Task 10.

## Publicação de resultado implementada em 7.1-A

[MonitoramentoResultadoPublisher](../../../src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/saida/servicebus/MonitoramentoResultadoPublisher.java)
implementa `PublicarResultadoMonitoramento` por CDI. Reutiliza seu mapper e o sender
compartilhado `FilaSaida`; conclui após confirmação do broker. O `Uni` é preguiçoso e
compartilha um envio por invocação; invocações independentes podem publicar novamente.
Falhas do SDK são sanitizadas e a falha própria do mapper é preservada.

O caso de uso de 7.1-B chama essa porta. A prova isolada de 7.1-A publica resultados
conclusivo/quarentena construídos pelo teste e lê pelo contrato independente do orquestrador.
A integração de 7.1-D acrescenta listener e classificação reais com resposta do Hub controlada.
Nenhuma dessas provas estabelece atomicidade entre publicar a saída e concluir a entrada.

Para reproduzir as verificações sem broker de política/configuração e publisher:

```powershell
mvn -q "-Dtest=CatalogoPoliticasMonitoramentoTest,PoliticaMonitoramentoProgressivaTest,PoliticasMonitoramentoConfigTest,PoliticaMonitoramentoProducerTest" test
mvn -q "-Dtest=MonitoramentoResultadoPublisherTest,PublicarResultadoMonitoramentoQuarkusTest,ResultadoMonitoramentoContratoTest" test
```

Para a prova com broker, usar o profile de integração da seção anterior. O fechamento da
última execução está na [continuidade de 7.1](continuidade-7-1.md). As seis provas anteriores
foram repetidas junto das nove de 7.1-D; a 8.1 acrescenta provas de transação e do fluxo reagendado.

## Caso de uso de processamento implementado em 7.1-B

A porta ProcessarTentativaMonitoramento recebe a tentativa e inputSequenceNumber escalar.
O caso de uso consulta a pré-validação primeiro; fora de EM_ANALISE_ENVIO_MTR devolve Ignorar.
Se elegível, resolve a versão recebida e verifica o prazo original e a quantidade já realizada
(tentativaAtual - 1), sem consultar o Hub quando esgotados. A consulta de limites aceita zero;
max-tentativas=1 permite a primeira consulta e encerra após ela se não conclusiva.

O Hub é classificado pelos nomes exatos: FINALIZADO_CONFORME -> CONFORME,
FINALIZADO_INCONFORME -> INCONFORME e PENDENTE_INFORMACA -> INCONFORME.
O resultado preserva a situação MTR, IDs, início e sequência; situação calculada não comprova
persistência. Uma consulta iniciada no prazo pode concluir terminal após o prazo.
Para resposta não conclusiva, o caso de uso relê o relógio e verifica prazo/contador novamente.

DecisaoProcessamento distingue Ignorar, ResultadoPublicado e ReagendamentoPendente.
ResultadoPublicado só é emitido após a confirmação da porta de publicação. Quarentena anterior
ao Hub usa contador anterior e MTR ausente; posterior ao Hub usa tentativaAtual e MTR consultado.
O motivo é PRAZO_MAXIMO ou MAXIMO_TENTATIVAS; falta de versão continua usando v1 padrão.
O Uni é adiado e memorizado por invocação; falhas propagam sem retry adicional.

ReagendamentoPendente contém a tentativa original, contador/intervalo calculados e instante
da avaliação. PoliticaAplicada registra versões solicitada/efetiva e indicador de padrão,
sem transportar a estratégia executável. Essa decisão não agenda nem conclui entregas.
ReagendamentoMonitoramento transforma essa intenção em próxima tentativa e horário limitado
ao prazo original. O listener executa a porta associada à entrega no adapter transacional.
O contador é verificado antes de incrementar, incluindo o maior inteiro representável.
A última consulta pode concluir normalmente; se não concluir, o teto gera QUARENTENA.

Para reproduzir os testes da fatia sem broker:

```powershell
mvn -q "-Dtest=ProcessarTentativaMonitoramentoUseCaseTest,PoliticaMonitoramentoProgressivaTest,CatalogoPoliticasMonitoramentoTest,ResultadoMonitoramentoCdiTest,EstruturaMonitoramentoArchUnitTest,FronteirasMonitoramentoArchUnitTest" test
```

## Listener da entrada de 7.1-C

[MonitoramentoEntradaListener](../../../src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/entrada/servicebus/MonitoramentoEntradaListener.java)
resolve por CDI e usa a fábrica existente. Seu método Java iniciar() continua disponível em
provas controladas. Desde 8.2, o observer StartupEvent chama esse mesmo método quando
monitoramento.service-bus.entrada.consumo-habilitado=true; o default é false. Escolher
emulador ou Azure não ativa o consumidor por si só. Não existe endpoint de ativação.

| Resultado da etapa | Ação da borda |
|---|---|
| Ignorar | Log de decisão e Complete |
| ResultadoPublicado | Complete após confirmação da publicação pelo caso de uso |
| Contrato inválido no mapper | DeadLetter com motivo MONITORAMENTO_ENTRADA_INVALIDA e descrição fixa |
| Falha técnica antes do settlement | Abandon |
| Falha de Complete, Abandon ou DeadLetter | Encerrar a assinatura; nenhuma segunda liquidação |
| ReagendamentoPendente | Schedule + Complete transacionais; só avançar após commit confirmado |
| Falha transacional/commit incerto | Encerrar assinatura; nenhuma segunda liquidação |

O processamento é serial e aguarda o settlement antes da próxima entrega. O receiver da
entrada mantém PEEK_LOCK e auto-complete desabilitado, agora com prefetchCount(0) explícito;
concatMap(..., 0) evita antecipação pelo operador no Reactor efetivo 3.4.41.
Há uma única inicialização por instância. Após encerramento, iniciar() é rejeitado.
ShutdownEvent PLATFORM_BEFORE e PreDestroy cancelam idempotentemente a assinatura;
somente a fábrica fecha os clientes, em PLATFORM_AFTER. O cancelamento é best effort:
os testes provam interrupção da cadeia local, sem comprovar interrupção de efeitos remotos
já iniciados. Liberação do lock e redelivery dependem do SDK/broker.

[LogListenerMonitoramento](../../../src/main/java/br/gov/caixa/simtr/monitoramento/adaptador/entrada/servicebus/LogListenerMonitoramento.java)
usa os eventos aprovados doctree.monitoramento-mtr.decisao.tomada, processamento.falhou e
settlement.executado, com campos constantes e sem Throwable/payload/IDs externos.
A prova JSON verifica o registro real. Propagação/spans e caracterização do SDK ficam em 10.1.

Reproduzir as verificações focadas do listener, sempre sem broker:

```powershell
mvn -q "-Dtest=MonitoramentoEntradaListenerTest,MonitoramentoEntradaLogTest,ClientesServiceBusTest,ResultadoMonitoramentoCdiTest,EstruturaMonitoramentoArchUnitTest,FronteirasMonitoramentoArchUnitTest" test
```

7.1-D comprovou integração terminal no emulador com cenários controlados e início explícito.
O ramo não conclusivo está conectado em 8.1; desde 8.2, o startup pode ativar a entrada por configuração.
Publicação da saída e Complete da entrada ainda não são atômicos e podem duplicar saída
após redelivery. Os critérios de política, prazo original e situações MTR permanecem os
já implementados em 7.1-B.

## Integração terminal verificada em 7.1-D

[MonitoramentoTerminalEmuladorTest](../../../src/test/java/br/gov/caixa/simtr/monitoramento/integracao/MonitoramentoTerminalEmuladorTest.java)
acrescenta nove cenários com filas/SDK e composição CDI reais. Somente a porta pública do Hub
é controlada pelo teste. Os três cenários via REST comprovam os nomes originais do MTR,
CONFORME/INCONFORME separados, v1 ativa e sequência real da mensagem. Os demais cobrem
no-op, prazo original expirado, fallback v1 para versão removida, política inativa com max=1,
Abandon/redelivery sem incrementar tentativa funcional e contrato inválido na DLQ.

O TerminalProfile compõe ServiceBusEmuladorTestProfile: preserva a proteção contra
configuração Azure externa e habilita simulador/política limitada apenas nesse teste.
A classe executa serialmente; exige filas dedicadas vazias, sem purga de resíduos.
Cada cenário obtém um listener CDI, chama iniciar() explicitamente e destrói o handle
contextual ao terminar. Somente o cliente adicional de DLQ é fechado pelo teste.

O controle negativo deixou a publicação na entrada sem resultado até o timeout ao omitir
o início do listener. Com o acionamento ligado, os nove cenários passaram. A execução
completa passou com 15 testes em quatro classes, incluindo as seis provas anteriores:

```powershell
mvn -q -Pservicebus-integration "-Dtest=MonitoramentoTerminalEmuladorTest" test
mvn -q -Pservicebus-integration clean test
```

A saída é lida pelo contrato independente do orquestrador. Complete da saída/DLQ ocorre
antes de cancelar a recepção; a remoção da entrada é conferida pela sequência explícita,
sem depender do cursor de peek. No redelivery, mesma sequência/corpo e DeliveryCount
crescente são observados enquanto a segunda consulta está pendente; a resposta final
preserva a tentativa funcional e o inputSequenceNumber.

Esses testes não comprovam atomicidade de publicação/Complete nem repetição segura diante
de perda de conexão. A ativação por configuração no startup foi acrescentada em 8.2. A prova adicional de 8.1
cobre o reagendamento transacional. Essas 23 integrações existentes não exercitam o listener
ou a flag da saída nem o fluxo completo até o log; a 9.1-C acrescentou quatro provas desse consumo.
O listener já tem evidência sem broker em 9.1-B. Azure gerenciado não foi executado.
Resultados históricos ficam na [continuidade de 8.1](continuidade-8-1.md); o fechamento atual
sem broker e Sonar está na [continuidade de 9.1](continuidade-9-1.md).

## Reagendamento transacional de 8.1

O listener converte ReagendamentoPendente em ReagendamentoMonitoramento e chama
MonitoramentoReagendamentoAdapter.associar(receiver, mensagem). Essa associação devolve
ReagendarTentativaMonitoramento para a entrega corrente; handles Azure ficam na borda.
O mapper próprio preserva IDs, início, prazo e versão e monta o envelope da próxima tentativa.

O adapter serializa antes de abrir a transação. Em seguida executa createTransaction,
scheduleMessage e Complete com o mesmo contexto, e commit. Falha em schedule/Complete
tenta rollback; erro no commit é resultado incerto e não tenta rollback, Abandon,
Complete comum ou novo envio. O listener só avança após a confirmação transacional.

A próxima data é min(processadoEm + intervalo, limiteEm). Faltando cinco minutos para
o prazo e com intervalo de trinta minutos, agenda para o prazo original; a entrega final
publica QUARENTENA/PRAZO_MAXIMO sem outra consulta. Atingir max-tentativas após consulta
não conclusiva também publica QUARENTENA e encerra. Nenhum desses caminhos renova a janela.

A invocação compartilha um único CompletableFuture. Cancelamento alcança o SDK e reassinar
não repete a transação; assinantes da mesma invocação compartilham também o cancelamento.
Cancelar na fronteira de commit não comprova rollback remoto. O sucesso registra
settlement=complete_transacional; falha não registra sucesso de liquidação.

A prova opt-in cobre commit, rollback/reentrega e perda deliberada da confirmação local
após commit confirmado, sem simular uma falha real de rede. O fluxo real CDI cobre
progressão e repetição, máximo configurado e prazo original com fallback v1.
A sequência muda ao ativar um agendamento; rollback não exige DeliveryCount crescente.

```powershell
mvn -q "-Dtest=ReagendamentoMonitoramentoTest,MonitoramentoReagendamentoAdapterTest,MonitoramentoEntradaListenerTest,PoliticaMonitoramentoProgressivaTest,ProcessarTentativaMonitoramentoUseCaseTest" test
mvn -q -Pservicebus-integration clean test
```

A aplicação admite emulador ou filas Azure conforme configuração do dev. O perfil de integração
acima é exclusivamente do emulador e rejeita configuração externa antes do bootstrap.
A suíte padrão não inicia broker. Evidências finais ficam na [continuidade](continuidade-8-1.md).

## Verificação e acompanhamento

Stack preservado: Quarkus `3.33.2.1`, JDK 25, extensão Azure Services `1.2.5`, SDK Service Bus
`7.17.12` e ArchUnit `1.4.2`. A versão efetiva está no `pom.xml`/árvore já registrada no plano;
não atualizar dependências junto da estrutura.
Consultar a [entrada Quarkus Azure Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/index.html)
e a [guia Service Bus](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html)
e confrontar exemplos com a versão efetiva. A guia `dev` pode evoluir.

As consultas concluídas seguem o [C5.1 aprovado](preparacao-consultas-5-1.md): modelo da
pré-validação com situação não vazia e origem; modelo do Hub com id/nome originais, sem
classificação. O mock possui DTO/mapper próprios e só responde com a flag
`monitoramento.simulador.prevalidacao.habilitado=true`; o padrão é `false`. Os cenários
`pre-em-analise`, `pre-conforme` e `pre-nao-conforme` são determinísticos.
Ausência, desativação e entrada inválida falham no `Uni`; falhas do Hub são propagadas,
sem retry ou log novo. O [guia principal](../../../doc/guias/guia-service-bus-amqp-dossie.md#consultas-implementadas-em-51)
explicita os modelos, respostas e ativação local.

Para verificar os 70 casos de consulta:

```powershell
mvn -q "-Dtest=PreValidacaoSimuladaMapperTest,PreValidacaoSimuladaAdapterTest,SituacaoDossieHubAclTest,ConsultasMonitoramentoQuarkusTest,PreValidacaoHabilitadaQuarkusTest" test
```

Esses testes usam `@QuarkusTest` para que o `quarkus-jacoco` existente meça também os caminhos
de erro. A ativação do mock fica restrita ao profile específico. Nenhum plugin, exclusão de
cobertura ou dependência foi acrescentado. A limitação da instrumentação automática está na
[guia oficial de cobertura](https://quarkus.io/guides/tests-with-coverage/#coverage-for-tests-not-using-quarkustest).

Na raiz do repositório, os comandos abaixo verificam a estrutura. Contratos/mappers e
consultas possuem API exercitada pelos testes; os três componentes de resultado têm provas positivas de CDI:

```powershell
mvn -q compile
mvn -q "-Dtest=EstruturaMonitoramentoArchUnitTest,FronteirasMonitoramentoArchUnitTest,ResultadoMonitoramentoCdiTest,ArchUnitProgressivoTest" test
```

Para uma fatia funcional, selecionar os testes pertinentes ao comportamento alterado. Depois de um
incremento coerente que mude o fingerprint executável:

```powershell
.\validar-checkpoint-sonarqube.ps1
```

O checkpoint executa a verificação completa. Não repetir Maven completo após ele sem mudança ou
falha que justifique. Preservar o baseline existente; não executar `-InitializeBaseline` nesta
retomada. Nunca copiar token para chat, argumentos, arquivos ou logs. Seguir o launcher e o fluxo
de credencial em memória de `AGENTS.md`. Indisponibilidade não equivale a aprovação.

Em `NON_COMPLIANT`, registrar as issues e métricas e obter decisão humana conforme o processo.
Somente a decisão efetivamente dada pelo usuário deve ser registrada pelo script. Os números
anteriores de testes/cobertura estão no `todo.md`; não tratá-los como medição após uma nova edição.

A cada entrega, registrar no checklist: arquivos, requisito atendido, cenários de regressão, testes focados,
checkpoint/fingerprint, riscos remanescentes e próximo item. Manter a distinção entre arquivo
criado, contrato implementado, integração ligada e fluxo verificado.

## Preservação do workspace e conversa com a equipe

Preservar todas as alterações rastreadas e não rastreadas. Não usar `git reset`, `git clean` nem
trocar branch descartando mudanças. O package `dossie`, o Hub, seus logs/erros, extensão, simuladores e
`src/main/azure/servicebus-emulator/config.json` permanecem preservados.

Para a reunião: percorrer o diagrama, abrir as portas e as classes pelos links do inventário,
revisar os limites preservados e detalhar 10.1/C3 pelo checklist.
Apresentar o fluxo funcional verificado: entrada e saída habilitadas por configuração independente,
log best-effort após processamento/reagendamento e ausência de persistência durável.

## Referência histórica da revisão do guia Service Bus e orientação Java

O guia técnico principal foi alinhado às decisões vigentes de fluxo, packages, connection
string/SAS, extensão e Dev Services. Nas onze portas, o Javadoc agora descreve o comportamento
de cada método, sua conclusão e falha; foram corrigidos os trechos `undefined`.
Nove classes do fluxo receberam a posição exata nas duas filas, critérios e responsabilidades.
Naquela revisão, a sintaxe foi conferida com DocLint (sem erros; nove avisos de construtores
implícitos), sem alteração fora dos comentários. O RED de reagendamento pertencia à pausa
daquele momento e foi superado pela implementação. O estado vigente inclui 6.1 e 7.1-A implementados, com C2 aceito; a evidência executável e o histórico completo estão no checklist.
