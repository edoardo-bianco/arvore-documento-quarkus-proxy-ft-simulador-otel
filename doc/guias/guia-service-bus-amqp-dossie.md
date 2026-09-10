# Guia Service Bus — orquestrador e monitoramento de dossiê

## O que os desenvolvedores recebem nesta branch

**A entrega está consolidada até 8.2: POST, consumo opt-in da entrada, processamento e
reagendamento/publicação de resultado funcionam. O consumidor/log da saída ainda falta em 9.1.**
Os parágrafos abaixo situam a evolução já implementada.
Política/configuração CDI, consultas de pré-validação/Hub, contratos/mappers e logs de erro
já estavam prontos. Agora também funcionam parâmetros pela ACL, fábrica de clientes, iniciação
e publisher inicial. Em 7.1-A foi implementado o publisher de resultado na saída.
O caso de uso de processamento está implementado em 7.1-B e o listener da entrada em 7.1-C,
com início explícito. A 8.1 conecta o reagendamento transacional e foi exercitada com o fluxo
até conclusão, teto ou prazo original. A 8.2 acrescenta ativação da entrada por configuração
no startup, desabilitada por padrão. O consumo do resultado continua pendente.

| Parte | Estado atual | O que a evidência comprova |
|---|---|---|
| Extensão Azure Service Bus e Dev Services | Implementados | Builder CDI e envio/recebimento/Complete nas duas filas em teste de infraestrutura |
| Política progressiva, configuração e catálogo por versão | Implementados | Intervalos, limites, resolução da versão recebida com v1 padrão se ausente e rejeição de configuração inválida/ambígua |
| REST e publicação inicial | Implementados em 6.1 | Validação, parâmetros pela política, IDs no servidor e `202` somente após confirmação da publicação |
| Reagendamento | Implementado em 8.1 | Próxima tentativa dentro do prazo original; schedule + Complete na mesma transação e preservação de IDs/versão |
| Resultado | Modelos, DTOs, mappers, erros e publisher implementados | Compatibilidade entre bordas, validação e publicação confirmada de conclusivo/quarentena, acionada pelo caso de uso de 7.1-B |
| Logging técnico e fronteiras | Implementados | Campos JSON tipados, diagnóstico seguro, isolamento de bordas e ACLs |
| Consultas de 5.1 | Implementadas e injetáveis | Mock com ativação explícita, DTO/mapper próprios e ACL pela porta pública do Hub; resultados mínimos e falhas verificados |
| Clientes Service Bus | Implementados em 6.1 | Quatro clientes duradouros, qualifiers por fila e fechamento completo/idempotente |
| Conexões restantes | 8.1 implementada | Das 11 portas, nove estão conectadas (reagendamento por entrega); três esqueletos da saída permanecem inativos |

As evidências da suíte padrão sem broker e do checkpoint vigente estão na
[continuidade de 8.2](../../tasks/features/orquestrador-monitoramento-service-bus/continuidade-8-2.md).
Os números históricos de 7.1/8.1 abaixo permanecem como marcos anteriores.
7.1-B conecta contrato, catálogo e portas no caso de uso: no-op, limites, classificação literal,
publicação confirmada e intenção de reagendamento. 7.1-C acrescenta listener CDI com início
explícito, settlement serial, falhas sem segunda liquidação e shutdown antes da fábrica.
7.1-D acrescenta nove cenários terminais com emulador; o perfil completo passou com 15 testes,
incluindo as seis provas anteriores. Somente a porta pública do Hub é controlada nesses nove
cenários. A 8.1 acrescenta provas de transação, progressão/repetição, máximo de tentativas e
prazo original com fallback v1, elevando o total a 21 integrações em cinco classes.
Fluxo com consumo/log da saída e Azure gerenciado ainda não foram verificados.

**C2 aceito, 8.1 publicada e 8.2 concluída tecnicamente. Próximo item: 9.1.**
A prova de startup passou nos dois cenários via POST; regressão com 1.297 testes padrão e
23 integrações aprovada, build/Sonar COMPLIANT. Evidências e métricas completas nas tasks.
Para assumir o trabalho, começar pelo
[roteiro do desenvolvedor](../../tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md#roteiro-para-assumir-a-entrega).
O [pacote de commit](../../tasks/features/orquestrador-monitoramento-service-bus/pacote-commit.md)
descreve o incremento de 8.2 autorizado para commit/publicação. O manifesto histórico de
b886bdb está separado; o guia do dev contém os roteiros completos de Dev Services e filas Azure,
com fixture 4324680, comandos de ativação e política curta opcional.

## Escopo e decisões vigentes

Este guia descreve a feature `orquestrador-monitoramento-service-bus`, na branch
`feature/orquestrador-monitoramento-service-bus`. O nome histórico do arquivo foi preservado
para manter os links, mas esta implementação **não altera o package `br.gov.caixa.simtr.dossie`**.
O Hub, suas capacidades, contratos, simuladores, logs e erros também permanecem preservados.

Os componentes novos são `br.gov.caixa.simtr.orquestrador` e
`br.gov.caixa.simtr.monitoramento`. O orquestrador publica na fila de entrada e consome a fila
de saída no desenho final. O monitoramento já consome a entrada, executa os critérios e reagenda
na própria entrada ou publica um resultado na saída. Os dois packages estão no mesmo artifact e
runtime, simulando responsabilidades de microsserviços. O consumo da saída e o encerramento por
log no orquestrador ainda serão implementados em 9.1.

A plataforma e a autenticação já estão decididas no
[ADR-0010 aceito](../adr/0010-extensao-quarkus-service-bus-connection-string-dev-services.md):

- extensão `io.quarkiverse.azureservices:quarkus-azure-servicebus:1.2.5`;
- BOM Quarkiverse Azure Services `1.2.5`, Quarkus `3.33.2.1` e JDK `25`;
- ambientes reais autenticados por SAS na connection string externa;
- desenvolvimento local com Service Bus Emulator e SQL Server iniciados por Dev Services;
- Microsoft Entra ID não é a autenticação desta feature.

O [ADR-0009](../adr/0009-azure-sdk-service-bus-dossie.md) preserva a decisão histórica de SDK
direto/Entra ID e foi substituído. Seus exemplos e os antigos contratos de dois campos não
orientam esta implementação. O histórico documental anterior permanece nas
[tasks do guia antigo](../../tasks/features/guia-service-bus-amqp-dossie/todo.md).

Para implementar e acompanhar o trabalho, consultar o
[plano vigente](../../tasks/features/orquestrador-monitoramento-service-bus/plan.md),
[checklist vigente](../../tasks/features/orquestrador-monitoramento-service-bus/todo.md) e
[inventário Java com roteiro de continuidade](../../tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md).
Este guia explica o fluxo; o inventário aponta cada arquivo e seu estado.

## Arquitetura e decisões para implementar

### Componentes no mesmo runtime

Este recorte usa o monólito modular Quarkus existente. Os packages abaixo delimitam
responsabilidades; não representam três aplicações implantadas separadamente.
A arquitetura segue os [ADRs 0001](../adr/0001-monolito-modular-e-hexagonal.md),
[0003](../adr/0003-orquestracao-e-colaboracao-por-portas.md) e
[0010](../adr/0010-extensao-quarkus-service-bus-connection-string-dev-services.md):

| Área | Responsabilidade | Limite de implementação |
|---|---|---|
| `orquestrador` | Receber solicitação, preparar a primeira mensagem e receber o resultado | Não é dono da política nem consulta configuração interna do monitoramento |
| `monitoramento` | Política, prazo, tentativa funcional, consultas, decisão terminal e reagendamento | Não incorpora contratos nem implementações internas do Hub |
| `hub.dossieproduto` | Fornecer a capacidade atômica existente de consulta por identificador | Mantém REST/MTR/simulador, regras, logs e erros existentes |
| `arquitetura.infraestrutura.servicebus` | Criar e encerrar os quatro clientes compartilhados | Factory implementada; não recebe regra, DTO ou modelo de negócio |
| `arquitetura.infraestrutura.observabilidade` | Acrescentar campos JSON tipados a novos registros elegíveis | Já implementada; não centraliza classificação, identidade ou sanitização de erros |
| `dossie` e `hub.arquitetura` existentes | Manter suas responsabilidades atuais | Não são destino dos novos listeners/publishers nem alvo de migração nesta feature |

```mermaid
flowchart LR
    subgraph APP["Mesmo runtime Quarkus"]
        ORQ["orquestrador"]
        MON["monitoramento"]
        HUB["hub.dossieproduto existente"]
        SB["infraestrutura.servicebus — pronta"]
        OBS["infraestrutura.observabilidade — pronta"]
        ORQ -->|"porta + ACL de parâmetros pronta (6.1)"| MON
        MON -->|"porta + ACL de consulta pronta (5.1)"| HUB
        ORQ -->|"publisher da entrada pronto (6.1)"| SB
        MON -->|"publisher da saída e listener com opt-in no startup"| SB
        ORQ -->|"logs novos das bordas"| OBS
        MON -->|"logs novos das bordas"| OBS
    end
```

O diagrama mostra as conexões locais implementadas. O fluxo assíncrono pelas duas filas está
no diagrama seguinte; a colaboração local não substitui esse fluxo nem ativa o listener.

### Hexagonal pragmática e contratos por borda

Domínio contém política e significado dos dados; aplicação coordena casos de uso e portas;
adapters traduzem REST/JSON/AMQP, acessam fontes e executam efeitos externos. SDK Azure e
handles de entrega ficam nas bordas. Domínio não depende de aplicação; contratos das portas
de entrada não expõem casos de uso concretos nem portas de saída.

**Quarkus, CDI, Jakarta, MicroProfile, Mutiny, Jackson e OpenTelemetry podem apoiar domínio e
aplicação.** Não criar wrappers apenas para esconder o framework. Essa permissão não leva
clientes Azure, DTOs de transporte ou infraestrutura técnica ao núcleo, nem permite bloquear
o event loop. Essa escolha está explícita nos ADRs 0001, 0010 e 0011.

Pelo [ADR-0004](../adr/0004-contratos-independentes-por-borda.md), cada borda tem DTO e mapper
próprios. O caminho é DTO → mapper → tipo interno → mapper da outra borda → DTO. A mensagem
JSON compatível é o contrato entre produtor e consumidor; igualdade de campos não autoriza
compartilhar a classe Java. Os erros Service Bus também têm DTO e RuntimeException próprios.
Não importar o DTO técnico de erro REST do Hub para economizar essas classes.

### Colaboração com o Hub e preparação inicial

O [ADR-0011](../adr/0011-composicao-local-monitoramento-e-fabrica-service-bus.md) fixa dois
caminhos implementados: consulta do Hub em 5.1 e preparação inicial em 6.1.

- **Implementado em 5.1:** `ConsultarSituacaoDossie` → `SituacaoDossieHubAcl` → porta pública
  `hub.dossieproduto.aplicacao.porta.entrada.ConsultarDossieProduto`. A ACL converte o
  identificador validado para Long positivo e traduz a situação para modelo do monitoramento.
  Ela não chama HTTP local, Resource, caso de uso concreto, DTO REST/MTR ou porta de saída do Hub.
- **Implementado em 6.1:** `ObterParametrosMonitoramento` → `ParametrosMonitoramentoAcl` →
  `PrepararMonitoramento`. A operação local recebe `iniciadoEm` e devolve
  `limiteEm`/`politicaMonitoramentoVersao`; a ACL traduz para o record do orquestrador.
  A política e sua configuração continuam no monitoramento. Esse cálculo não consulta fontes,
  gera IDs, envia mensagens ou processa tentativas.

IDs e instante inicial pertencem ao início da orquestração; prazo/versão vêm da política.
Esses valores devem ser preservados nos futuros reagendamentos. Os dois records de parâmetros
já têm cálculo/tradução conectados por CDI; o orquestrador não importa política ou configuração.

### Consultas implementadas em 5.1

A colaboração segue os ADRs 0003/0011: a ACL injeta somente `ConsultarDossieProduto` e
usa os modelos públicos dessa assinatura. `idDossieMtr` permanece string nos contratos;
a conversão para `Long` positivo ocorre na ACL, aceita zeros à esquerda e rejeita formato
inválido, zero e overflow antes de chamar o Hub. A seleção MTR/simulador permanece no Hub.

O retorno é `SituacaoDossieConsultada(Integer id, String nome)`: preserva id, inclusive
nulo, e nome original não vazio, sem normalizar ou transportar data, matrícula ou objeto
completo do Hub. A fixture existente `4324680` retorna `1 / Rascunho`; não fornece IDs
para os nomes conclusivos informados. Esses nomes já são aceitos literalmente no contrato
de resultado. A ACL preserva situações desconhecidas; a classificação do caso de uso
continua pendente e não é inferida da fixture.

Pelo ADR-0004, o simulador possui `PreValidacaoSimuladaDto` e
`PreValidacaoSimuladaMapper` próprios. Sua porta retorna
`PreValidacaoConsultada(String situacao, boolean simulada)`, com situação não vazia
e `simulada=true` nos cenários abaixo:

| `idDossiePreValidacao` | Situação retornada |
|---|---|
| `pre-em-analise` | `EM_ANALISE_ENVIO_MTR` |
| `pre-conforme` | `CONFORME` |
| `pre-nao-conforme` | `NAO_CONFORME` |

A configuração é `monitoramento.simulador.prevalidacao.habilitado=false`.
Para habilitar explicitamente no desenvolvimento:

```powershell
mvn quarkus:dev "-Dmonitoramento.simulador.prevalidacao.habilitado=true"
```

O comando habilita os dados simulados. O endpoint de 6.1 publica a tentativa inicial, mas ainda
não consulta pré-validação: essa etapa pertence ao processamento de 7.1.
Os testes habilitam a flag somente em profile específico. Com a flag desabilitada, a
consulta falha e as capacidades atuais do Hub continuam inicializando normalmente.

As duas portas retornam `Uni` e executam na assinatura, sem bloqueio. Ausência da
pré-validação produz falha, distinta de situação não elegível; resposta/situação do Hub
ausente ou nome vazio também falha. Não há retorno fictício, retry ou log adicional.
Falhas locais usam mensagens fixas sem o valor rejeitado; falhas do Hub seguem pelo mesmo
fluxo assíncrono, preservando o comportamento existente. Classificação de erro e settlement
são tratados na borda consumidora implementada em 7.1-C, com início explícito.

Esses detalhes foram aprovados no [C5.1](../../tasks/features/orquestrador-monitoramento-service-bus/preparacao-consultas-5-1.md).
Os 70 testes novos cobrem comportamento e CDI; as seis classes implementadas têm 100% das
linhas e condições medidas pelo JaCoCo. O guardrail também inclui a borda do simulador.

### Fábrica e ciclo de vida dos clientes

Também pelo ADR-0011, somente `ClientesServiceBus`, implementada em 6.1, injeta e configura o
`ServiceBusClientBuilder` da extensão. Ela cria sender/receiver assíncronos duradouros para
cada fila, diferenciados por qualifiers CDI de entrada/saída. Os adapters recebem clientes,
nunca o builder; não há cliente por mensagem. Os quatro produtores CDI entregam singletons
com `@FilaEntrada`/`@FilaSaida`; receivers usam `PEEK_LOCK` e auto-complete desabilitado.
Criar os receivers não inicia consumo.

A fábrica fecha todos os clientes em ordem inversa, mesmo após falha parcial na criação ou
no fechamento. O encerramento é idempotente: observer de `ShutdownEvent` com prioridade
`PLATFORM_AFTER` e `@PreDestroy` usam a mesma rotina. Os futuros listeners devem cancelar suas
assinaturas em prioridade anterior; essa coordenação com listeners ainda será verificada.

Somente a fábrica é condicionada por `quarkus.azure.servicebus.enabled`. O Resource e o caso
de uso permanecem disponíveis com a extensão desabilitada; o publisher resolve o cliente
qualificado via `Instance` ao enviar e falha explicitamente quando indisponível. A fábrica
exige a connection string da extensão, fornecida por Dev Services ou pelo ambiente. Não há
fallback para Entra ID.
A infraestrutura não importa Hub, orquestrador ou monitoramento; somente os adapters
Service Bus acessam a fábrica. Ela não é barramento genérico, dona do retry funcional,
reagendamento, validação, DTO ou settlement. Essas responsabilidades continuam nos componentes.

## Fluxo completo a implementar

O diagrama representa o fluxo aprovado. REST, iniciação, publicações, consultas, processamento
e listener da entrada estão implementados; a 8.2 permite opt-in no startup. Integração
terminal no emulador está verificada em 7.1-D; a 8.1 conecta reagendamento. Consumo/log da saída continuam pendentes.

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
    QOUT --> LOUT["MonitoramentoResultadoListener do orquestrador"]
    LOUT --> RECEBER["ReceberResultadoMonitoramentoUseCase"]
    RECEBER --> LOG["ResultadoMonitoramentoLogAdapter"]
    LOG --> FIM["Concluir saída: fim do demonstrador"]
```

O listener inicia o processamento pela porta de entrada. A aplicação do monitoramento coordena
as consultas e a política; o listener não passa a ser dono das regras de negócio. A decisão
semântica orienta os efeitos das portas de saída. Serialização, clientes e settlement ficam
nos adapters. Em 8.1, o listener chama associar(receiver, mensagem) no adapter sem estado;
a porta devolvida captura os handles somente na borda. O domínio fornece próxima tentativa
e horário, sem SDK ou contexto transacional. A fábrica do [ADR-0011](../adr/0011-composicao-local-monitoramento-e-fabrica-service-bus.md)
continua dona dos clientes.

### Quem escreve e quem lê

| Fila | Quem escreve | Quem lê | Finalidade |
|---|---|---|---|
| `q.prevalidacao.monitoramento-mtr.in` | Orquestrador, na tentativa inicial; monitoramento, no reagendamento | Listener de entrada do monitoramento | Executar uma tentativa do monitoramento |
| `q.prevalidacao.monitoramento-mtr.out` | Publisher de resultado do monitoramento | Listener de resultado do orquestrador | Informar resultado terminal ou quarentena e encerrar o demonstrador com log |

A direção de um package é relativa ao componente: o orquestrador escreve na fila de entrada
por `adaptador.saida.servicebus` e lê a fila de saída por `adaptador.entrada.servicebus`.

### Caminhos de aplicação e arquivos

Todos os nomes abaixo existem no código. Consultas de 5.1 e iniciação/publicação de 6.1
estão conectadas; os demais caminhos funcionais permanecem pendentes.

| Etapa | Caminho no componente | Item |
|---|---|---|
| Iniciar | `MonitoramentoDossieResource` → `IniciarMonitoramento.executar` → `IniciarMonitoramentoUseCase` | 6.1 implementado |
| Publicar entrada | `PublicarTentativaMonitoramento.executar` → `MonitoramentoEntradaPublisher` → mapper próprio → sender da entrada | 6.1 implementado |
| Consumir entrada | `MonitoramentoEntradaListener` → mapper de entrada → `ProcessarTentativaMonitoramento.executar` → caso de uso | Implementado; reagendamento em 8.1 e opt-in no startup em 8.2 |
| Consultar fontes | `ConsultarPreValidacao` → `PreValidacaoSimuladaAdapter`; `ConsultarSituacaoDossie` → `SituacaoDossieHubAcl` → porta pública `ConsultarDossieProduto` | 5.1 implementado |
| Reagendar | Decisão da política → modelo limita horário → listener associa entrega → porta no adapter → mapper próprio → fila de entrada | Implementado em 8.1; schedule + Complete com commit confirmado |
| Publicar resultado | `PublicarResultadoMonitoramento.executar` → `MonitoramentoResultadoPublisher` → mapper próprio → fila de saída | Publisher implementado em 7.1-A e acionado pelo caso de uso de 7.1-B |
| Consumir resultado | `MonitoramentoResultadoListener` → mapper próprio → `ReceberResultadoMonitoramento.executar` → caso de uso | 9.1 |
| Finalizar recorte | `RegistrarResultadoMonitoramento.executar` → `ResultadoMonitoramentoLogAdapter` → conclusão da saída pelo listener | 9.1 |

O acesso ao Hub é local por porta pública e ACL do monitoramento. Não há chamada HTTP ao
próprio Hub nem implementação de mensageria dentro de `dossie`. A factory técnica
`arquitetura.infraestrutura.servicebus.ClientesServiceBus` concentra apenas criação e
lifecycle dos clientes da extensão; não contém política, DTO de negócio ou processamento.

## Critérios de monitoramento

A coordenação funcional abaixo está implementada no caso de uso de 7.1-B.
A política calcula limites/intervalos; o caso de uso consulta e publica pelas portas.
Registro do no-op e settlement estão implementados em 7.1-C. Agendamento efetivo está conectado em 8.1;
a intenção pendente encerra a assinatura atual sem settlement, mantendo o consumo geral inativo.

| Condição, na ordem de avaliação | Comportamento esperado |
|---|---|
| Envelope/JSON inválido | Mapper registra erro de contrato; listener executa DeadLetter com motivo/descrição fixos |
| Pré-validação diferente de `EM_ANALISE_ENVIO_MTR` | Registrar no-op e concluir a entrada; não consultar MTR, reagendar ou produzir novo resultado |
| Prazo ou máximo de tentativas atingido | Calcular `QUARENTENA`, publicar resultado e concluir entrada após confirmação |
| Situação do Hub conclusiva conforme a direção humana de 2026-09-09 | Resultado terminal preserva `situacaoMtr` e calcula `situacaoPreValidacao` separadamente; contrato e classificação funcional de 7.1-B usam os nomes originais |
| Outra situação do MTR, ainda dentro dos limites | Calcular próxima tentativa/intervalo; limitar horário ao prazo original e executar a transação de reagendamento |
| Falha técnica recuperável | Propagar/classificar falha para Abandon/redelivery; não incrementar tentativa funcional |

A [tabela informada pelo usuário e a diferença para o contrato atual](../../tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md#situações-informadas-e-diferença-para-o-contrato-atual)
orientam 7.1-B: FINALIZADO_CONFORME → CONFORME, FINALIZADO_INCONFORME → INCONFORME e
PENDENTE_INFORMACA → INCONFORME, preservando a situação MTR. As duas bordas já aceitam
esses nomes exatos por direção humana, sem corrigir grafias nem inferir IDs numéricos.
Os valores anteriormente publicados continuam aceitos por compatibilidade.

`QUARENTENA` é resultado funcional na saída. DLQ é tratamento técnico de uma entrega e não
substitui a quarentena. Esta feature não persiste a situação da pré-validação: ela é calculada
para o resultado. O log do orquestrador encerra o recorte; não retoma, suspende ou finaliza um
workflow durável.

A [política implementada](../../src/main/java/br/gov/caixa/simtr/monitoramento/dominio/politica/PoliticaMonitoramentoProgressiva.java)
usa intervalos ordenados; após a lista, repete o último. Lista unitária equivale a intervalo
fixo. A configuração padrão atual é `PT30M`, duração máxima `PT24H`, versão
`v1` e máximo de tentativas opcional, ausente por padrão. O prazo encerra quando o instante
de processamento é igual ou posterior a `limiteEm`; tem precedência sobre o máximo de
tentativas. Uma lista configurada como `PT3H,PT4H,PT6H` produz esperas de 3 h, 4 h, 6 h
e repete 6 h enquanto o monitoramento estiver dentro do prazo. São intervalos entre tentativas,
não horários desde o início. A ausência de teto por contagem vale para o padrão;
`max-tentativas` permanece disponível para outras configurações. A política fornece a próxima
tentativa e o intervalo, não agenda a mensagem.
Todas as definições são validadas no bootstrap, inclusive as não selecionadas; o producer
entrega a instância escolhida por nome. A [configuração v1 e seus arquivos](../../tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md#configuração-v1-já-implementada)
estão prontos desde 4.1. A resolução por versão da mensagem está conectada ao caso de uso em 7.1-B.

No início, o orquestrador gera IDs e `iniciadoEm` no servidor. Antes de publicar, obtém
`limiteEm` e `politicaMonitoramentoVersao` por `ObterParametrosMonitoramento` →
`ParametrosMonitoramentoAcl` → `PrepararMonitoramento`. Essa colaboração síncrona calcula
parâmetros em memória; não substitui a fila para processar tentativas.
Reagendamentos preservam instante inicial, prazo, versão e IDs.
`CatalogoPoliticasMonitoramento` resolve a definição da versão recebida, inclusive inativa.
Se ausente, fornece v1 padrão em memória: PT30M repetido, PT24H e máximo de tentativas ausente. Não gera QUARENTENA por falta de definição.
A seleção ativa e as propriedades permanecem iguais; novos monitoramentos usam a ativa.

A resolução diferencia versão solicitada, política efetiva e padrão aplicado. O caso de uso
avalia a política com o limite recebido, preservando a versão e sem reiniciar as 24 horas.
Definições presentes inválidas, seleção ativa inválida e versões duplicadas falham no bootstrap.
O catálogo, a composição CDI e sua conexão ao caso de uso estão implementados.
Antes da consulta são contadas as tentativas anteriores; após resposta não conclusiva, a atual.
max-tentativas=1 permite uma consulta. Prazo tem precedência sobre contagem no encerramento;
resposta conclusiva de consulta iniciada no prazo prevalece sobre expiração durante a consulta.
O próximo horário é min(processadoEm + intervalo, limiteEm). A entrega no prazo original
publica QUARENTENA/PRAZO_MAXIMO sem nova consulta. O teto de tentativas e o maior inteiro
representável são verificados antes do incremento; o padrão segue sem teto operacional configurado.
`DeliveryCount` não é `tentativaAtual`; rollback não promete seu incremento.
Ver [ADR-0011](../adr/0011-composicao-local-monitoramento-e-fabrica-service-bus.md).

## Contratos de borda

### REST inicial e fila de entrada

O contrato REST aprovado é `POST /simtr-hub/v1/monitoramentos-dossie`, com
`idDossiePreValidacao` e `idDossieMtr`. O response contém `monitoramentoId` e
`orquestracaoId`, com `202 Accepted` somente após confirmação da publicação inicial.
Resource, caso de uso e publisher estão implementados em 6.1. Autenticação e autorização
preservam a postura vigente do serviço. A iniciação é assíncrona, sem bloqueio: gera dois UUIDs
e `iniciadoEm`, obtém prazo/versão pela ACL e envia a tentativa 1. Reassinaturas do mesmo `Uni`
compartilham o resultado e não reenviam; requisições HTTP diferentes geram novas iniciações.
Isso não fornece idempotência durável nem garantia de entrega exatamente uma vez.

A validação mantém `400/ARVDOCP0001` no mapper global existente. Falha na iniciação/publicação
retorna `500/ARVDOCP9999`, mensagem genérica e ID de erro, sem causa ou dados do broker.
`ErroInicioMonitoramentoDto` pertence à borda REST do orquestrador e preserva o formato
existente, sem importar o DTO do Hub. Não foram acrescentados logs manuais ou spans neste item;
os logs de erro dos mappers permanecem. Campos desconhecidos não controlam IDs ou tentativa.

A fila de entrada usa os nove campos v1 abaixo. Exemplo sintético compatível com os mappers
já implementados:

```json
{
  "schemaVersion": 1,
  "monitoramentoId": "MON-1",
  "orquestracaoId": "ORQ-1",
  "idDossiePreValidacao": "pre-externa",
  "idDossieMtr": "0007",
  "tentativaAtual": 1,
  "iniciadoEm": "2026-09-04T12:00:00Z",
  "limiteEm": "2026-09-05T12:00:00Z",
  "politicaMonitoramentoVersao": "v1"
}
```

| Propriedade AMQP | Entrada inicial e reagendamento |
|---|---|
| `MessageId` | `<monitoramentoId>:tentativa:<tentativaAtual>` |
| `CorrelationId` | `<orquestracaoId>` |
| `Subject` | `MONITORAR_DOSSIE_MTR` |
| `ContentType` | `application/json` |

`tentativaAtual` é inteiro positivo no **corpo JSON**. Identificadores permanecem strings;
`idDossieMtr` deve representar inteiro decimal de `1` a `9223372036854775807`,
preservando zeros à esquerda. Campos obrigatórios não podem ser nulos/brancos;
`limiteEm` deve ser posterior a `iniciadoEm`. Campos JSON desconhecidos são tolerados
conforme o contrato aprovado. Não importar limites de 256 caracteres/64 KiB ou rejeição de
campos desconhecidos dos antigos exemplos do ADR-0009 como requisitos já implementados.

Cada borda possui DTO e mapper próprios: produtor inicial no orquestrador, consumidor no
monitoramento e produtor de reagendamento no monitoramento. A compatibilidade é demonstrada
pelo JSON; os tipos Java não são compartilhados entre essas bordas.
O mapper monta a mensagem; o publisher inicial já a envia. O agendamento permanece responsabilidade
do adapter futuro de reagendamento.

### Fila de saída

Os modelos, DTOs e mappers independentes das duas bordas de resultado estão **implementados
e verificados em 4.1**. O produtor serializa e monta o envelope; o consumidor valida e produz
o modelo próprio do orquestrador. O publisher de 7.1-A é acionado pelo caso de uso de 7.1-B;
o listener da entrada de 7.1-C aguarda essa confirmação antes de Complete. O consumo da saída
continua pendente em 9.1.

Exemplo conclusivo com os 13 campos do contrato v1 anteriormente publicado, que continua
aceito pelos mappers. As duas bordas também aceitam os três nomes originais informados
pelo Hub, com situação de pré-validação independente. O caso de uso de 7.1-B classifica
somente os três nomes originais do Hub; os nomes antigos abaixo são compatibilidade de transporte:

```json
{
  "schemaVersion": 1,
  "monitoramentoId": "MON-1",
  "orquestracaoId": "ORQ-1",
  "idDossiePreValidacao": "pre-externa",
  "idDossieMtr": "0007",
  "resultadoMonitoramento": "CONCLUSIVO",
  "situacaoMtr": "PENDENTE_INFORMACAO",
  "situacaoPreValidacao": "NAO_CONFORME",
  "motivo": "SITUACAO_CONCLUSIVA_MTR",
  "tentativasRealizadas": 3,
  "iniciadoEm": "2026-09-04T12:00:00Z",
  "concluidoEm": "2026-09-04T18:30:00Z",
  "inputSequenceNumber": 13527
}
```

| Campo/regra | Comportamento implementado |
|---|---|
| `schemaVersion` | Exatamente 1 |
| Identificadores | Strings obrigatórias; MTR no mesmo intervalo decimal positivo da entrada, preservando zeros |
| `tentativasRealizadas` | Inteiro não negativo; em CONCLUSIVO, pelo menos 1 |
| `situacaoMtr` em CONCLUSIVO | FINALIZADO_CONFORME, FINALIZADO_INCONFORME ou PENDENTE_INFORMACA; também CONFORME, NAO_CONFORME e PENDENTE_INFORMACAO por compatibilidade |
| QUARENTENA sem consulta | Admite `situacaoMtr=null` e zero tentativas; consumidor também aceita ausência do campo de situação |
| Datas | Ambas obrigatórias; `concluidoEm` pode coincidir com `iniciadoEm`, mas não precedê-lo |
| `inputSequenceNumber` | Inteiro representável como long, inclusive zero/negativo; sequência técnica não é contador funcional |
| Envelope/JSON | Rejeita divergências, tipos incorretos, overflow e conteúdo adicional depois do objeto; campos desconhecidos são ignorados |

A nulidade e o contador da quarentena foram confirmados pelo usuário e estão registrados no
[plano/checklist](../../tasks/features/orquestrador-monitoramento-service-bus/todo.md).
Exemplo de quarentena antes da primeira consulta: no objeto acima, o resultado e a situação
da pré-validação são `QUARENTENA`, `situacaoMtr` é `null`,
`tentativasRealizadas` é `0` e o motivo descreve o limite atingido. O prazo pode vencer
antes da primeira consulta; o mapper não inventa uma situação MTR para preencher o campo.

A validação adicional exige evidência de consulta para CONCLUSIVO, mas o mapper não calcula
a correspondência entre situação MTR e situação de pré-validação: recebe e preserva esses
valores. A decisão de negócio da tabela de critérios será executada em 7.1. A propriedade
auxiliar usada na validação não aparece no JSON.

| Propriedade AMQP | Resultado |
|---|---|
| `MessageId` | `<monitoramentoId>:resultado:v1` |
| `CorrelationId` | `<orquestracaoId>` |
| `Subject` | `RESULTADO_MONITORAMENTO_DOSSIE_MTR` |
| `ContentType` | `application/json` |

O produtor pertence a `monitoramento.adaptador.saida.servicebus`; o consumidor pertence a
`orquestrador.adaptador.entrada.servicebus`. Situação original MTR e situação calculada da
pré-validação permanecem separadas. O demonstrador não persiste essas situações nem retoma
workflow durável.

## Extensão, connection string e Dev Services

O [pom.xml](../../pom.xml) já importa `quarkus-azure-services-bom:1.2.5` e declara
`quarkus-azure-servicebus` sem versão individual. O SDK resolvido e caracterizado na feature
é `azure-messaging-servicebus:7.17.12`. Usar o `ServiceBusClientBuilder` produzido pela
extensão, encapsulado pela fábrica técnica implementada conforme o ADR-0011. Não copiar o antigo BOM
Azure `1.3.8`, adicionar Azure Identity ou construir uma cadeia de credenciais Entra.

| Ambiente | Credencial e infraestrutura | Transporte |
|---|---|---|
| `dev` local sem configuração externa de Service Bus | Dev Services inicia emulador e SQL e fornece a connection string local | `AMQP` sobre TCP |
| Testes padrão | Extensão/Dev Services desabilitados; mocks/stubs, sem broker | Nenhum |
| Integração explícita com Dev Services | Profile Maven `servicebus-integration` habilita extensão/Dev Services e recusa configuração externa | `AMQP` sobre TCP |
| Azure real; no dev usar `dev,azure` | `QUARKUS_AZURE_SERVICEBUS_CONNECTION_STRING` fornecida externamente; Dev Services desabilitado | `AMQP_WEB_SOCKETS` |
| `prod` | Dev Services desabilitado; connection string externa obrigatória no desenho aprovado | A propriedade base é `AMQP`; a seleção WebSockets está no profile `azure` |

A property da extensão é `quarkus.azure.servicebus.connection-string`. Seu valor real não
é gravado no repositório, argumentos, logs, spans ou relatórios. O
[application.properties](../../src/main/resources/application.properties) deixa essa property
sem placeholder global obrigatório para permitir a detecção de Dev Services em dev/test.
Não configurar apenas namespace/credencial Entra como alternativa de autenticação.
A política SAS real usa `Send + Listen`, sem `Manage` ou `RootManageSharedAccessKey`,
no menor escopo operacional permitido para as duas filas do mesmo runtime.

A configuração executável atual mantém:

- `monitoramento.service-bus.input-queue`, com alias `SERVICE_BUS_INPUT_QUEUE`;
- `monitoramento.service-bus.output-queue`, com alias `SERVICE_BUS_OUTPUT_QUEUE`;
- `quarkus.azure.servicebus.devservices.emulator.config-file-path=config.json`;
- arquivo em [src/main/azure/servicebus-emulator/config.json](../../src/main/azure/servicebus-emulator/config.json);
- emulador `1.1.2` e SQL Server `2022-CU14-ubuntu-22.04`, com imagens fixadas.

A property contém `config.json`; o arquivo continua no diretório exigido pela extensão.
Não movê-lo para outra pasta. Os testes comuns desabilitam a extensão e Dev Services. O
[ServiceBusDevServicesTest](../../src/test/java/br/gov/caixa/simtr/monitoramento/adaptador/configuracao/ServiceBusDevServicesTest.java)
prova envio/recebimento nas duas filas, e o
[MonitoramentoEntradaEmuladorTest](../../src/test/java/br/gov/caixa/simtr/orquestrador/integracao/MonitoramentoEntradaEmuladorTest.java)
prova REST → publicação inicial. Ambos exigem seleção explícita; não equivalem ao processamento
completo. O recebimento e Complete nesses testes são apenas limpeza/verificação da integração.

Há três recursos diferentes: o **emulador Service Bus** atende ao transporte; o
**adapter simulado de pré-validação**, implementado em 5.1 com ativação explícita, fornece cenários de negócio;
o **simulador existente do Hub** atende à consulta de dossiê quando seu profile/property já
existente o habilita. Eles permanecem com responsabilidades separadas.

### Escolha local do desenvolvedor e execução dos testes

A escolha do broker para executar a aplicação é independente da suíte unitária/de contratos:

| Objetivo | Comando na raiz | Condição |
|---|---|---|
| Testes padrão, sem broker | `mvn test` | Não exige emulador nem acesso a filas Azure |
| Somente integração com emulador | `mvn -Pservicebus-integration test` | Docker disponível; sem configuração externa de Service Bus |
| Aplicação local com emulador | `mvn quarkus:dev` | Sem connection string/namespace externo; Dev Services fornece emulador/SQL |
| Aplicação local com filas Azure | `mvn quarkus:dev "-Dquarkus.profile=dev,azure"` | Connection string SAS e nomes das filas fornecidos pelo ambiente |

Para Azure, configurar externamente `QUARKUS_AZURE_SERVICEBUS_CONNECTION_STRING`,
`SERVICE_BUS_INPUT_QUEUE` e `SERVICE_BUS_OUTPUT_QUEUE`. O profile `azure` desabilita Dev Services
e seleciona `AMQP_WEB_SOCKETS`; os nomes devem apontar para as filas de desenvolvimento.
Não colocar credenciais nos argumentos Maven, no repositório ou no guia.

Para emulador, usar uma sessão sem connection string/namespace externo. O profile de integração
fixa `test` e verifica as fontes efetivas do Quarkus antes do bootstrap: ambiente, propriedades
JVM, arquivos e configurações `%test`. Conexão/namespace presentes são rejeitados sem expandir
expressões; falha de leitura também impede a execução, com erro fixo sem causa externa.
Isso evita que a prova local publique acidentalmente no Azure. Os testes padrão usam dependências substituídas;
o teste de configuração Azure usa uma connection string sintética e não envia mensagens.

No `pom.xml`, Surefire usa `!servicebus-integration` por padrão; o profile Maven seleciona apenas
a tag `servicebus-integration`. O checkpoint Sonar usa a suíte padrão. Não habilitar Dev Services
em testes unitários para aumentar cobertura; integração e sua evidência são registradas à parte.
A flag do mock de pré-validação é uma escolha separada e não é necessária para publicar em 6.1.

Para validar o estado atual, executar `mvn clean verify` e, separadamente,
`mvn -q -Pservicebus-integration clean test`. A suíte padrão continua sem broker; datas,
contagens e checkpoint estão na [continuidade de 8.2](../../tasks/features/orquestrador-monitoramento-service-bus/continuidade-8-2.md).
Não executar as suítes em paralelo.

A propriedade `monitoramento.service-bus.entrada.consumo-habilitado` tem default false.
Com true, o observer StartupEvent chama iniciar() no listener da entrada, uma vez por instância.
O shutdown continua cancelando a assinatura antes de fechar os clientes. Falha síncrona ao
obter cliente propaga diagnóstico sanitizado; falha assíncrona encerra o consumo sem retry
adicional. Reiniciar a aplicação após corrigir a falha. Isso não fornece readiness do broker.

Para executar com emulador e mock explícito, conforme pré-requisitos do [README](../../README.md#execucao-local):

```powershell
mvn quarkus:dev -Ddebug=false "-Dmonitoramento.service-bus.entrada.consumo-habilitado=true" "-Dmonitoramento.simulador.prevalidacao.habilitado=true"
```

A escolha Azure/emulador e as credenciais permanecem externas. A flag não muda conexão nem
habilita o mock automaticamente. Para Azure já configurado, acrescentar
"-Dquarkus.profile=dev,azure". O consumo usa as filas desse ambiente.

A prova `mvn -q -Pservicebus-integration "-Dtest=MonitoramentoAtivacaoEmuladorTest" test`
inicia o runtime por configuração, chama o POST e verifica terminal/reagendamento sem acesso
ao listener pelo teste. O harness lê/confirma a saída; consumo/log dessa fila continuam em 9.1.
O [roteiro de execução](../../tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md#verificação-rápida-do-marco-até-82)
traz o request, os cenários, os limites e os demais testes. Dev mode interativo e Azure real
não foram executados nesta fatia.

Pontos de entrada oficiais: [Quarkus Azure Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/index.html)
e [extensão Service Bus/Dev Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html).
Conferir exemplos contra as versões fixadas no projeto; este guia não autoriza upgrade.

## Consumo, confirmação e tratamento de erro

Os listeners planejados usam `ServiceBusReceiverAsyncClient`, prefetch inicial zero, concorrência limitada e consumo contínuo com
`PEEK_LOCK` e auto-complete desabilitado. Os publishers usam
`ServiceBusSenderAsyncClient`; a aplicação compõe a conclusão em Mutiny sem bloquear o
event loop. Não criar cliente por mensagem nem usar scheduler local para simular agendamento.

| Caminho | Quando concluir a mensagem atual |
|---|---|
| Resultado terminal/quarentena | Depois da confirmação da publicação na saída |
| Reagendamento | No mesmo efeito transacional que agenda a próxima mensagem na entrada, após prova de API/emulador |
| No-op | Depois de confirmar a condição e registrar a decisão |
| Consumo da saída | Depois que o registro do resultado pelo orquestrador concluir |
| Falha recuperável | Abandon/redelivery conforme classificação; não incrementar tentativa funcional |
| Contrato permanentemente inválido | DeadLetter com diagnóstico controlado, sem payload ou segredo |

A transação de entidade única `schedule + Complete` está implementada em 8.1 e foi provada
no SDK resolvido e no emulador, incluindo commit, rollback/redelivery e cancelamento.
Essa garantia não alcança publicar a saída e concluir a entrada: sem Outbox, uma falha
nessa janela pode repetir o resultado. Não criar contexto
mutável de entrega em singleton nem transportar handles Azure ao domínio/aplicação.

O log final previsto usa o evento `orquestrador.monitoramento-dossie.resultado.registrado`
e correlação por `monitoramentoId`/`orquestracaoId`. Erros do novo desenvolvimento usam
o padrão JSON aprovado no [ADR-0012](../adr/0012-campos-json-tipados-logs-service-bus.md):
`recurso`, `id_erro`, `codigo_erro`, `erros: [{mensagem}]`, `detalhe` e,
quando técnico, `stacktrace` sanitizado. `codigo_http` é omitido em AMQP.
O array deve ser JSON real no formatter, não texto escapado em `message`.

A falha reconhecida é registrada uma vez em ERROR e propagada com o mesmo id/código.
Preservar RuntimeException própria e não ignorar a exceção. Diagnóstico contém tipo e frames
sem mensagem original, causas, suppressed, processor ou payload. Trace/span só são acrescentados
quando o contexto é válido. O Hub conserva seus logs, erros e comportamento atuais.
Armazenamento e ações automáticas a partir desses registros continuam fora do incremento.

A validação local das duas bordas admite `situacaoMtr` nula e contador zero em QUARENTENA
antes de consulta. Contadores negativos são rejeitados; CONCLUSIVO exige pelo menos uma
tentativa e aceita os nomes originais FINALIZADO_CONFORME, FINALIZADO_INCONFORME e
PENDENTE_INFORMACA, preservados literalmente. CONFORME, NAO_CONFORME e PENDENTE_INFORMACAO
continuam aceitos por compatibilidade com o contrato publicado. A propriedade auxiliar
de validação não integra o JSON. O mapper não recalcula nem persiste as situações; a
classificação funcional é executada pelo caso de uso de 7.1-B.

### Como o erro JSON é emitido sem alterar o Hub

O [ADR-0012](../adr/0012-campos-json-tipados-logs-service-bus.md) decidiu uma composição
técnica já implementada em `arquitetura.infraestrutura.observabilidade`:

1. A borda classifica a falha, gera a identidade, monta seu DTO e sanitiza o diagnóstico.
2. `CamposLogJson` transporta somente o objeto JSON imutável já sanitizado; não recebe Throwable.
3. `FormatoLogJsonServiceBus` delega ao formatter JSON existente e acrescenta campos tipados
   apenas quando a categoria é uma das novas bordas Service Bus e o registro possui o marcador.
4. `ConfiguracaoLogJsonServiceBus` instala a decoração no início e restaura os delegates
   no encerramento, inclusive quando há handlers intermediários.

O formatter preserva metadados e rejeita sobrescrita de campos do logger. Categorias do Hub e
registros sem marcador mantêm a saída original. Não se cria writer, destino, rotação ou nível
novo, nem se usa MDC para representar arrays/números como texto. DTOs, eventos, códigos,
mensagens, identidade e sanitização continuam locais; a infraestrutura não trata o negócio.

A repetição entre helpers é um ponto de manutenção registrado: a medição atual é 4,3%, abaixo
do limite de 5%. Reduzi-la por extração transversal exige decisão arquitetural concreta;
não compartilhar DTOs ou sanitização nem excluir arquivos da análise para reduzir o indicador.

### Telemetria do fluxo ainda a implementar

Pelos [ADRs 0006](../adr/0006-compatibilidade-observabilidade-e-testes.md) e
[0010](../adr/0010-extensao-quarkus-service-bus-connection-string-dev-services.md), a Task 10
primeiro caracteriza o que o SDK efetivo já emite. Propagação W3C nas application properties
e spans manuais só preenchem lacunas comprovadas; não duplicar instrumentação automática.

| Contrato planejado no C0.4 | Nome/kind |
|---|---|
| Entrada REST | `simtr-hub.api.monitoramento-dossie.iniciar` — SERVER |
| Início da orquestração | `orquestrador.service.monitoramento-dossie.iniciar` — INTERNAL |
| Consulta de pré-validação | `doctree.service.prevalidacao.dossie.consultar` — INTERNAL |
| Avaliação do monitoramento | `doctree.service.monitoramento-mtr.avaliar` — INTERNAL |
| Registro do resultado | `orquestrador.service.monitoramento-dossie.resultado-registrar` — INTERNAL |
| Enviar/agendar | `send <fila>` e `schedule <fila>` — PRODUCER |
| Processar entrega | `process <fila>` — CONSUMER |
| Settlement | `complete <fila>`, `abandon <fila>`, `dead_letter <fila>` — CLIENT |

Os prefixos `doctree` acima são nomes observáveis do planejamento original, não packages
Java atuais. A revisão humana dos packages não os renomeou automaticamente. Conferir o
contrato de sinais antes da implementação; eventual alteração exige checkpoint próprio.

Os eventos planejados incluem publicação confirmada/falha e resultado registrado/falha no
orquestrador, e `doctree.monitoramento-mtr.decisao.tomada`,
`doctree.monitoramento-mtr.processamento.falhou` e
`doctree.monitoramento-mtr.settlement.executado`. Eles são distintos dos erros de mapper
já implementados com prefixos `monitoramento.servicebus`/`orquestrador.servicebus`.

Os sinais podem usar IDs técnicos validados, tentativa funcional, delivery count, sequência,
decisão e settlement. Nomes/atributos de mensageria devem ser conferidos com a versão efetiva
do SDK. Payload, connection string, SAS, namespace, documentos e PII ficam fora da telemetria.
IDs de alta cardinalidade não viram labels de métrica; novas métricas/dashboards estão fora
desta feature. Os logs atuais só acrescentam trace/span quando já existe contexto válido.
Na integração de 6.1, os logs automáticos do SDK exibiram metadados de conexão/entidade do
emulador. Sua caracterização e adequação ao contrato de telemetria continuam na Task 10;
6.1 não comprova a segurança/correlação de todos os sinais de um ambiente Azure real.

## Continuidade manual e estado real

Na estrutura existente, Javadoc de classe informa responsabilidade, fluxo, item pendente e
verificação. Javadoc dos métodos das portas descreve entrada, resultado, conclusão assíncrona
e falha. Classes vazias não ganham métodos fictícios para ilustrar o fluxo; o desenvolvedor
deverá declarar os métodos/implementações ao executar o item correspondente.

| Estado | Entrega |
|---|---|
| Implementado e verificado anteriormente | Extensão, configuração/Dev Services, política, configuração tipada/producer CDI, contratos REST e da entrada, mappers e logs das bordas da entrada |
| Estrutura restante | Onze portas (nove conectadas, incluindo reagendamento por entrega), parâmetros/decisão e listener da entrada funcionais, com três classes `@Vetoed`, ainda sem fluxo completo |
| Reagendamento implementado | DTO/mapper v1 próprios, validação, envelope e log de erro JSON; sem publicar ou agendar |
| 4.1 tecnicamente concluído | Contratos/mappers de resultado, validação confirmada de quarentena/conclusivo e guardrails verificados |
| 5.1 tecnicamente concluído | Consulta simulada de pré-validação, DTO/mapper próprios e ACL do Hub com testes de comportamento, CDI e fronteiras |
| 6.1 implementado | Parâmetros locais, factory, POST, caso de uso e publisher inicial; integração explícita comprovada |
| 7.1-A implementado | Publisher da saída com confirmação, falhas seguras e testes sem broker/integração explícita |
| 7.1-B implementado | Caso de uso CDI, no-op, limites, classificação, confirmação da publicação e intenção pendente de reagendamento |
| 7.1-C implementado | Listener da entrada CDI com início explícito, settlement serial, logs mínimos e lifecycle; testes sem broker |
| 7.1-D verificado | Nove cenários novos com emulador, incluídos no perfil completo de 15 integrações; checkpoint nas tasks |
| 8.1 implementada | Agendamento transacional ligado ao listener; cancelamento, teto e prazo original verificados |
| 8.2 implementada | Ativação da entrada por configuração no startup; evidências atuais nas tasks |
| 9.1 pendente | Listener da saída e log final |
| 10.1 em diante pendente | Correlação ponta a ponta, cenários integrados e fechamento do demonstrador |

O mapper de reagendamento já está implementado. Os testes preservados provaram JSON/envelope,
validação equivalente à entrada, compatibilidade com o consumidor e erro JSON sanitizado.
O RED da pausa permanece registrado como etapa anterior no checklist, junto das evidências atuais.

O item 6.1 está tecnicamente concluído, incluindo o ajuste Sonar autorizado.
C2 foi aceito pelo usuário em 2026-09-09. 7.1 está concluída tecnicamente, com publisher da
saída, caso de uso/listener da entrada e integração terminal local verificados. A 8.1 concluiu
o reagendamento transacional e a 8.2 acrescentou ativação da entrada no startup por opt-in.
O consumo/log da saída permanece pendente em 9.1. Preservar as entregas e seguir o checklist.
Os comandos e critérios de verificação estão no guia de desenvolvimento e no plano.

O [manifesto de commit](../../tasks/features/orquestrador-monitoramento-service-bus/pacote-commit.md)
descreve o pacote proposto até 7.1-A, incluindo 5.1, 6.1 e C2-R1 desde a base publicada de 4.1.
O checklist registra a evidência executável e as pendências; a preparação documental não executa publicação Git.
Preservar arquivos rastreados e não rastreados ao materializar novas entregas.

A [solução ampla de origem](../feat/solucao-duas-filas-azure-service-bus-quarkus-azure-servicebus-reactive-jdk25.md)
inclui persistência e continuidade durável que não pertencem a este recorte. Para esta feature,
prevalecem as adaptações explícitas do plano e dos ADRs aceitos: sem alteração em `dossie`
ou Hub, sem persistência nova, e encerramento do demonstrador por log.

## Roteiro para continuar a implementação

### Ponto de partida para o desenvolvedor

1. Trabalhar na branch `feature/orquestrador-monitoramento-service-bus` e consultar
   [plano](../../tasks/features/orquestrador-monitoramento-service-bus/plan.md),
   [checklist](../../tasks/features/orquestrador-monitoramento-service-bus/todo.md) e
   [evidência de 6.1](../../tasks/features/orquestrador-monitoramento-service-bus/continuidade-6-1.md).
   C2 está aceito e 7.1 concluída tecnicamente; consultar também a
   [continuidade de 7.1](../../tasks/features/orquestrador-monitoramento-service-bus/continuidade-7-1.md).
2. Conferir o fechamento de [8.1](../../tasks/features/orquestrador-monitoramento-service-bus/continuidade-8-1.md)
   e a ativação de 8.2 antes de seguir para 9.1 quando autorizado. Reutilizar classificação e resolução de versão de 7.1-B:
   definição recebida quando disponível, v1 padrão quando ausente, sempre com prazo original.
   A fixture `1 / Rascunho` não fornece IDs para os nomes conclusivos.
3. Ler `ProcessarTentativaMonitoramentoUseCase` e `DecisaoProcessamento` no
   [inventário Java](../../tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md).
   A porta recebe tentativa e sequência escalar. Ignorar e ResultadoPublicado resultam em
   Complete pelo listener; ReagendamentoPendente executa schedule + Complete com o mesmo
   contexto na borda. Situação original e calculada permanecem separadas.
4. Exercitar `MonitoramentoEntradaListener`, já conectado ao receiver qualificado e à porta,
   em integração com cenários controlados. As provas antigas chamam iniciar(); a prova
   de 8.2 ativa o consumo por configuração no startup e entra pelo POST. Não há endpoint de ativação.
   O reagendamento só permite avançar após commit confirmado e nunca gera Complete simples
   adicional. O shutdown cancela a operação pendente antes da fábrica; cancelar a espera na
   fronteira de commit não comprova reversão remota nem autoriza segunda liquidação.
5. Usar `MonitoramentoResultadoPublisher`, já implementado pela porta e pelo sender da
   saída. O adapter reutiliza seu mapper; concluir a entrada somente após confirmação do envio.
   Provar no-op, limites, falha, contratos inválidos e ausência de Complete antecipado. Sem Outbox,
   publicação na saída e Complete da entrada não são uma operação atômica.
6. Executar testes locais sem broker e integração explícita para o trecho conectado.
   Atualizar inventário de inatividade, Javadocs, documentação e checkpoint Sonar com o baseline
   original. Preservar prazo/versão/IDs e as provas de `schedule + Complete`, rollback,
   redelivery e cancelamento; falha de commit nunca tenta rollback ou nova publicação.

**6.1 entrega a publicação inicial confirmada e C2 está aceito.** 7.1-A entrega o publisher
da saída; 7.1-B/C conectam o processamento e o listener com início explícito, verificados no
emulador em 7.1-D. A 8.1 conecta o reagendamento. Consumo da saída e verificação do demonstrador completo permanecem pendentes.

### Ordem das entregas restantes

| Item | Entrega a implementar | Verificação para considerar pronta |
|---|---|---|
| 7.1-B — implementada | Caso de uso conectado ao catálogo, consultas e publisher | Ver evidência local sem broker e checkpoint nas tasks |
| 7.1-C — implementada | Listener da entrada, settlement e lifecycle, sem início automático | Saída confirmada antes de Complete, falhas sem segundo settlement, encerramento antes da fábrica e logs mínimos; evidência sem broker nas tasks |
| 7.1-D — concluída tecnicamente | Integração terminal e fechamento do item | 15 integrações, 1.268 testes sem broker, revisão e Sonar COMPLIANT; evidências nas tasks |
| 8.1 — implementada | Reagendamento de situação não conclusiva | Política/versão, prazo, teto, schedule + Complete, rollback/redelivery e cancelamento; fechamento nas tasks |
| 8.2 | Ativação da entrada por configuração no startup | Default inativo, início único, falhas/shutdown e POST real no emulador |
| 9.1 | Listener da saída, caso de uso e log final | Registro do resultado e Complete/Abandon/DeadLetter coerentes com a falha |
| 10.1 e C3 | Correlação e fluxo integrado no emulador | Propagação, sinais sem duplicação, caminhos de sucesso, falha e quarentena |
| 11.1–CF | Verificação final e revisão do demonstrador | Suíte/checkpoint, documentação e decisão humana de encerramento |

Ao assumir uma fatia, seguir o [inventário Java](../../tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md):
implementar a porta no adapter/caso de uso correspondente, retirar `@Vetoed` apenas quando
funcional e remover o tipo do inventário de inatividade junto dos testes. Não ligar todos os
esqueletos ao CDI para aparentar fluxo pronto. Reutilizar contratos/mappers já verificados.

As definições de arquitetura já estão aceitas, mas permanecem pontos técnicos a resolver nas
fatias correspondentes: tratamento do intervalo/prazo restante e do contador no limite inteiro;
acoplamento da entrega à transação de reagendamento; comprovação de atomicidade no SDK/emulador;
integração do encerramento antes da factory e caracterização da telemetria automática.
A recuperação v1 autorizada é explícita no modelo de resolução; não introduzir outras recuperações.

O histórico de execução e publicação permanece nas tasks. O estado preparado inclui a base até 7.1-A;
o demonstrador completo e a entrega produtiva dependem das etapas restantes.
Persistência, Cosmos DB, Outbox, idempotência durável, workflow durável, métricas e novas
integrações ficam fora deste recorte.
