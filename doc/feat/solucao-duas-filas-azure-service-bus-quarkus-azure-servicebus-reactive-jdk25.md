# Sincronização de Dossiês com Duas Filas do Azure Service Bus, Quarkus Reactive e JDK 25

Solução técnica para monitorar a situação de um Dossiê de Produto no MTR por meio de duas filas do Azure Service Bus, utilizando Quarkus 3.33 LTS, JDK 25, a extensão `quarkus-azure-servicebus`, `ServiceBusReceiverAsyncClient`, `ServiceBusSenderAsyncClient`, AMQP 1.0 e autenticação por connection string.

## Sumário

- [1. Objetivo](#1-objetivo)
- [2. Decisões consolidadas](#2-decisões-consolidadas)
- [3. Arquitetura da solução](#3-arquitetura-da-solução)
- [4. Responsabilidades dos componentes](#4-responsabilidades-dos-componentes)
- [5. Fila de entrada](#5-fila-de-entrada)
- [6. Processamento da fila de entrada](#6-processamento-da-fila-de-entrada)
- [7. Paralelismo, antiguidade e SequenceNumber](#7-paralelismo-antiguidade-e-sequencenumber)
- [8. Intervalos progressivos e novas tentativas](#8-intervalos-progressivos-e-novas-tentativas)
- [9. Fila de saída e retomada da orquestração](#9-fila-de-saída-e-retomada-da-orquestração)
- [10. Regras de transição](#10-regras-de-transição)
- [11. Peek-Lock, settlement e idempotência](#11-peek-lock-settlement-e-idempotência)
- [12. Consistência entre transição e publicação do resultado](#12-consistência-entre-transição-e-publicação-do-resultado)
- [13. Segurança com connection string](#13-segurança-com-connection-string)
- [14. Configuração das filas](#14-configuração-das-filas)
- [15. Observabilidade](#15-observabilidade)
- [16. Sugestão de implementação com quarkus-azure-servicebus](#16-sugestão-de-implementação-com-quarkus-azure-servicebus)
- [17. Testes mínimos](#17-testes-mínimos)
- [18. Conclusão](#18-conclusão)
- [19. Referências oficiais](#19-referências-oficiais)

---

## 1. Objetivo

Implementar uma solução assíncrona para:

1. receber do orquestrador a solicitação de monitoramento de um dossiê específico;
2. consultar periodicamente a situação efetiva do Dossiê de Produto no MTR;
3. atualizar a situação correspondente na pré-validação quando o resultado for conclusivo;
4. reagendar uma nova tentativa quando o resultado ainda não for conclusivo;
5. interromper o monitoramento por quantidade máxima de tentativas ou por prazo máximo;
6. colocar o dossiê em `QUARENTENA` quando um dos limites for atingido;
7. publicar o resultado em uma fila de saída;
8. permitir que o orquestrador retome ou finalize seu fluxo a partir desse resultado.

Cada mensagem identifica diretamente o dossiê da pré-validação e o Dossiê de Produto correspondente no MTR. Não haverá listagem de dossiês, filtro por período ou scheduler local que percorra uma coleção.

---

## 2. Decisões consolidadas

A implementação utilizará:

```text
Quarkus 3.33.x LTS
+
JDK 25
+
io.quarkiverse.azureservices:quarkus-azure-servicebus
+
ServiceBusClientBuilder injetado por CDI
+
ServiceBusReceiverAsyncClient para os listeners
+
ServiceBusSenderAsyncClient para publicação e agendamento
+
Reactor Mono/Flux na borda do Azure SDK
+
Mutiny Uni nas portas e casos de uso da aplicação
+
AMQP 1.0 sobre TLS
+
connection string recebida por configuração externa
```

A extensão `quarkus-azure-servicebus` não é um connector do MicroProfile Reactive Messaging. Portanto, o listener não será implementado com `@Incoming`.

O listener será um bean CDI que mantém uma assinatura reativa sobre:

```java
ServiceBusReceiverAsyncClient.receiveMessages()
```

O método retorna um `Flux<ServiceBusReceivedMessage>` contínuo. O processamento e o settlement permanecem não bloqueantes.

Não será utilizada a extensão `quarkus-messaging-amqp` para essas mesmas filas. Misturar os dois modelos criaria duas formas diferentes de conexão, acknowledgement, tratamento de erros e configuração AMQP para o mesmo fluxo.

---

## 3. Arquitetura da solução

Nomes ilustrativos:

```text
q.prevalidacao.monitoramento-mtr.in
q.prevalidacao.monitoramento-mtr.out
```

Fluxo consolidado:

```text
Orquestrador
    |
    | 1. envia o Dossiê de Produto ao MTR
    | 2. coloca o dossiê em EM_ANALISE_ENVIO_MTR
    | 3. publica o comando de monitoramento
    v
Fila de entrada
q.prevalidacao.monitoramento-mtr.in
    |
    | AMQP 1.0 / Peek-Lock
    v
Processor Quarkus do doctree
    |
    | consulta a situação atual na pré-validação
    | consulta o Dossiê de Produto no MTR
    |
    +-- situação ainda não conclusiva
    |       |
    |       v
    |   agenda nova mensagem na fila de entrada
    |   com intervalo progressivo
    |
    +-- situação conclusiva
    |       |
    |       v
    |   aplica a transição na pré-validação
    |   registra o resultado para publicação
    |
    +-- prazo ou tentativas esgotados
            |
            v
        aplica QUARENTENA
        registra o resultado para publicação
            |
            v
Fila de saída
q.prevalidacao.monitoramento-mtr.out
    |
    | AMQP 1.0 / Peek-Lock
    v
Listener reativo do orquestrador
    |
    +-- CONFORME
    |       -> finaliza o fluxo
    |
    +-- PENDENTE_INFORMACAO
    |       -> retoma o fluxo de complementação
    |
    +-- NAO_CONFORME
    |       -> aplica a regra definida pela orquestração
    |
    +-- QUARENTENA
            -> suspende a automação
            -> sinaliza ação humana
```

A fila de saída é consumida pelo orquestrador. O `doctree` é responsável por aplicar a transição na pré-validação e produzir o resultado que permite ao orquestrador continuar.

---

## 4. Responsabilidades dos componentes

| Componente | Responsabilidade |
|---|---|
| Orquestrador | Enviar o dossiê ao MTR, alterar para `EM_ANALISE_ENVIO_MTR`, publicar o primeiro comando e consumir o resultado |
| Fila de entrada | Manter comandos ativos e tentativas futuras agendadas |
| Processor do `doctree` | Validar a mensagem, consultar a situação atual, consultar o MTR, controlar limites, aplicar transições e decidir o próximo passo |
| Sender da fila de entrada | Agendar a próxima tentativa quando a situação ainda não for conclusiva |
| Fila de saída | Transportar resultados conclusivos ou de quarentena |
| Listener do orquestrador | Correlacionar o resultado e retomar, finalizar ou suspender o fluxo |

O processor deverá permanecer modular dentro do `doctree`, separado por portas e adaptadores. Essa delimitação permite extraí-lo futuramente para um worker dedicado sem alterar os contratos das filas nem as regras do monitoramento.

---

## 5. Fila de entrada

### 5.1 Responsabilidade

A fila de entrada recebe comandos para monitorar um único dossiê por mensagem.

Produtores:

```text
orquestrador
processor do doctree, ao agendar a próxima tentativa
```

Consumidor:

```text
processor do doctree
```

### 5.2 Contrato da mensagem

```json
{
  "schemaVersion": 1,
  "monitoramentoId": "MON-6ec73bf9-b390-417c-b2c2-f34fd7f68a63",
  "orquestracaoId": "ORQ-25c62d41-2305-4661-a3c8-cdbdf681c1f2",
  "idDossiePreValidacao": "c5a13bd2-fc7e-45cd-9c47-8a79b5926c86",
  "idDossieMtr": "123456789",
  "tentativaAtual": 1,
  "iniciadoEm": "2026-09-04T12:00:00Z",
  "limiteEm": "2026-09-05T12:00:00Z",
  "politicaMonitoramentoVersao": "v1"
}
```

### 5.3 Campos

| Campo | Uso |
|---|---|
| `monitoramentoId` | Identifica um ciclo de monitoramento |
| `orquestracaoId` | Permite ao orquestrador localizar o fluxo a ser retomado |
| `idDossiePreValidacao` | Identifica o dossiê da pré-validação |
| `idDossieMtr` | Identifica o Dossiê de Produto no MTR |
| `tentativaAtual` | Controla tentativas funcionais de consulta |
| `iniciadoEm` | Registra o início do monitoramento |
| `limiteEm` | Define o prazo absoluto de encerramento |
| `politicaMonitoramentoVersao` | Identifica a política de intervalos utilizada |

### 5.4 Propriedades do Service Bus

| Propriedade | Valor |
|---|---|
| `MessageId` | `<monitoramentoId>:tentativa:<tentativaAtual>` |
| `CorrelationId` | `orquestracaoId` |
| `Subject` | `MONITORAR_DOSSIE_MTR` |
| `ContentType` | `application/json` |
| `ScheduledEnqueueTime` | Definido apenas nas tentativas futuras |
| `SequenceNumber` | Atribuído pelo Service Bus |

O produtor não define `SequenceNumber`.

O `MessageId` deve ser determinístico para permitir detecção de duplicidade de envios dentro da janela configurada na fila.

---

## 6. Processamento da fila de entrada

### 6.1 Recebimento

O receiver será configurado com:

```java
.receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
.disableAutoComplete()
```

O processor decide explicitamente entre:

```text
Complete
Abandon
DeadLetter
```

### 6.2 Fluxo de decisão

```text
receber mensagem
    |
    v
validar contrato
    |
    v
consultar situação atual da pré-validação
    |
    +-- situação diferente de EM_ANALISE_ENVIO_MTR
    |       |
    |       v
    |   concluir como no-op idempotente
    |
    +-- situação = EM_ANALISE_ENVIO_MTR
            |
            v
    verificar prazo e tentativas
            |
            +-- limite atingido
            |       |
            |       v
            |   aplicar QUARENTENA
            |   registrar resultado terminal
            |
            +-- limite não atingido
                    |
                    v
            consultar Dossiê de Produto no MTR
                    |
        +-----------+-------------------+
        |                               |
        v                               v
situação conclusiva             situação não conclusiva
        |                               |
        v                               v
aplicar transição                calcular nova tentativa
registrar resultado              agendar nova mensagem
```

### 6.3 Resultado terminal

Depois de aplicar uma transição conclusiva ou `QUARENTENA`, o `doctree` deve registrar o resultado que será publicado na fila de saída.

A mensagem de entrada somente pode receber `Complete` depois que o estado terminal e o resultado a publicar estiverem confirmados pela aplicação.

### 6.4 Falha técnica

Exemplos:

- falha na consulta da situação atual;
- timeout ou indisponibilidade do `simtr-hub`;
- falha na atualização da pré-validação;
- falha ao agendar a próxima tentativa;
- falha de comunicação com o Service Bus.

Essas falhas não representam conformidade ou não conformidade.

Para falha recuperável:

```text
Abandon
```

O `DeliveryCount` será incrementado e a mesma mensagem poderá ser entregue novamente.

### 6.5 Falha permanente do contrato

Exemplos:

- JSON inválido;
- campo obrigatório ausente;
- `schemaVersion` não suportada;
- tentativa com valor inválido.

Resultado:

```text
DeadLetter
```

A mensagem deve seguir para a DLQ com `DeadLetterReason` e `DeadLetterErrorDescription`.

---

## 7. Paralelismo, antiguidade e `SequenceNumber`

### 7.1 Política adotada

Não haverá FIFO global e não será usada uma sessão global.

A fila de entrada será consumida com:

```text
Sessions desabilitadas
+
competing consumers
+
Peek-Lock
+
PrefetchCount inicial igual a zero
+
concorrência limitada
```

As queues do Service Bus normalmente entregam mensagens na ordem em que foram adicionadas. Assim, as mensagens ativas mais antigas tendem a ocupar primeiro os slots disponíveis de processamento.

Como várias mensagens são processadas em paralelo, não existe garantia de ordem de conclusão.

Exemplo:

```text
Fila ativa: M1 M2 M3 M4 M5 M6
Concorrência: 4

Slots iniciais:
slot 1 <- M1
slot 2 <- M2
slot 3 <- M3
slot 4 <- M4

Possível ordem de conclusão:
M3, M1, M4, M2
```

A garantia adotada é:

> As mensagens ativas mais antigas são normalmente recebidas antes das mais novas e ocupam primeiro a capacidade disponível, mas são processadas em paralelo e podem terminar fora de ordem.

Não se declara FIFO estrito.

### 7.2 Uso correto do `SequenceNumber`

O `SequenceNumber` será usado para:

- auditoria;
- logs;
- tracing;
- diagnóstico de ordem;
- cancelamento de uma mensagem ainda agendada;
- recuperação de uma mensagem explicitamente diferida.

Ele não será usado para localizar e consumir diretamente a menor mensagem ativa.

`peekMessage(sequenceNumber)` apenas visualiza mensagens e não aplica lock. O recebimento direto por número de sequência é destinado a mensagens previamente colocadas em estado `Deferred`.

### 7.3 Mensagens agendadas

Enquanto uma tentativa estiver agendada para o futuro, ela não faz parte do conjunto de mensagens ativas.

Quando chega o horário agendado, a mensagem torna-se disponível para recepção. O número retornado por `scheduleMessage(...)` permite cancelar aquele agendamento enquanto ele ainda estiver pendente.

O identificador funcional permanece:

```text
monitoramentoId
+
orquestracaoId
+
MessageId determinístico
```

---

## 8. Intervalos progressivos e novas tentativas

### 8.1 Regra

Quando o MTR responder corretamente, mas ainda não apresentar uma situação conclusiva, o processor deverá:

1. incrementar a tentativa funcional;
2. calcular o próximo intervalo;
3. criar uma nova mensagem de entrada;
4. agendar a nova mensagem no Service Bus;
5. concluir a mensagem atual.

Não será utilizado:

```text
Thread.sleep
scheduler local
Uni mantido em memória durante horas
lock da mensagem mantido durante o intervalo
Abandon como mecanismo de espera funcional
```

### 8.2 Configuração ilustrativa

```properties
monitoramento.service-bus.progressive-delays=PT30M,PT3H,PT4H,PT6H
monitoramento.service-bus.max-attempts=5
monitoramento.service-bus.max-duration=PT24H
```

Interpretação:

| Depois da tentativa | Espera antes da próxima |
|---:|---:|
| 1 | 30 minutos |
| 2 | 3 horas |
| 3 | 4 horas |
| 4 | 6 horas |

Os valores finais são parâmetros de negócio e operação.

### 8.3 Condições de encerramento

O dossiê será colocado em `QUARENTENA` quando ocorrer primeiro:

```text
número máximo de tentativas
OU
prazo máximo do monitoramento
```

Antes de aplicar `QUARENTENA`, o processor deve confirmar novamente que o dossiê ainda está em `EM_ANALISE_ENVIO_MTR`.

### 8.4 Tentativa funcional versus redelivery técnica

| Controle | Significado |
|---|---|
| `tentativaAtual` | Consultas ao MTR concluídas corretamente, mas sem resultado terminal |
| `DeliveryCount` | Entregas malsucedidas da mesma mensagem por falha técnica |

Um timeout não deve incrementar `tentativaAtual`. Ele deve provocar `Abandon`.

### 8.5 Atomicidade no reagendamento

As operações abaixo devem ser executadas na mesma transação do Service Bus:

```text
scheduleMessage da próxima tentativa
+
Complete da mensagem atual
```

Isso evita dois cenários:

```text
Complete sem criar a próxima tentativa
```

ou:

```text
criar a próxima tentativa sem concluir a mensagem atual
```

A transação cobre somente operações internas do Service Bus.

---

## 9. Fila de saída e retomada da orquestração

### 9.1 Responsabilidade

A fila de saída recebe apenas resultados que encerram o ciclo atual de monitoramento:

```text
CONFORME
NAO_CONFORME
PENDENTE_INFORMACAO
QUARENTENA
```

Produtor:

```text
doctree
```

Consumidor:

```text
orquestrador
```

### 9.2 Contrato da mensagem de saída

```json
{
  "schemaVersion": 1,
  "monitoramentoId": "MON-6ec73bf9-b390-417c-b2c2-f34fd7f68a63",
  "orquestracaoId": "ORQ-25c62d41-2305-4661-a3c8-cdbdf681c1f2",
  "idDossiePreValidacao": "c5a13bd2-fc7e-45cd-9c47-8a79b5926c86",
  "idDossieMtr": "123456789",
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

### 9.3 Propriedades

| Propriedade | Valor |
|---|---|
| `MessageId` | `<monitoramentoId>:resultado:v1` |
| `CorrelationId` | `orquestracaoId` |
| `Subject` | `RESULTADO_MONITORAMENTO_DOSSIE_MTR` |
| `ContentType` | `application/json` |

### 9.4 Comportamento do orquestrador

| Situação original no MTR | Ação do orquestrador |
|---|---|
| `CONFORME` | Finaliza o fluxo |
| `PENDENTE_INFORMACAO` | Retoma a complementação de informações ou documentos |
| `NAO_CONFORME` | Executa a regra correspondente da orquestração |
| `QUARENTENA` | Suspende a automação e sinaliza ação humana |

A mensagem de saída deve manter separadamente:

```text
situacaoMtr
situacaoPreValidacao
```

Isso é necessário porque `PENDENTE_INFORMACAO` no MTR é persistido como `NAO_CONFORME` na pré-validação, mas o orquestrador precisa conhecer o resultado original para retomar o fluxo correto.

O listener do orquestrador deve executar `Complete` somente depois de registrar de forma durável a retomada, finalização ou suspensão da orquestração.

---

## 10. Regras de transição

| Situação retornada pelo MTR | Situação na pré-validação | Resultado na fila de saída |
|---|---|---|
| `CONFORME` | `CONFORME` | `CONFORME` |
| `NAO_CONFORME` | `NAO_CONFORME` | `NAO_CONFORME` |
| `PENDENTE_INFORMACAO` | `NAO_CONFORME` | `PENDENTE_INFORMACAO` |
| Outra situação | Sem transição | Nova tentativa agendada |
| Máximo de tentativas | `QUARENTENA` | `QUARENTENA` |
| Prazo máximo | `QUARENTENA` | `QUARENTENA` |

As transições somente podem partir de:

```text
EM_ANALISE_ENVIO_MTR
```

Se a mensagem for entregue novamente depois de a transição já ter sido aplicada, o processor deve reconhecer o estado atual e concluir como no-op idempotente.

---

## 11. Peek-Lock, settlement e idempotência

### 11.1 Semântica de entrega

O Service Bus opera com entrega `at-least-once` no modo `PEEK_LOCK`.

Isso significa:

- a mensagem fica bloqueada para um receiver enquanto o lock estiver válido;
- `Complete` remove a mensagem;
- `Abandon` libera a mensagem e incrementa `DeliveryCount`;
- perda ou expiração do lock permite redelivery;
- uma mensagem pode ser recebida novamente.

Não existe garantia de leitura física exatamente uma vez.

### 11.2 Idempotência do processor

O efeito funcional deve ser idempotente:

```text
se o dossiê já não está em EM_ANALISE_ENVIO_MTR
    -> não repetir transição
    -> não duplicar efeitos
    -> concluir a mensagem conforme o estado já registrado
```

Também devem ser idempotentes:

- publicação do resultado;
- retomada do orquestrador;
- finalização da orquestração;
- transição para `QUARENTENA`;
- início de um novo ciclo após `PENDENTE_INFORMACAO`.

### 11.3 Identificadores determinísticos

Fila de entrada:

```text
MessageId = <monitoramentoId>:tentativa:<tentativaAtual>
```

Fila de saída:

```text
MessageId = <monitoramentoId>:resultado:v1
```

A detecção de duplicidade do Service Bus reduz duplicações de envio dentro da janela configurada, mas não substitui a idempotência do consumidor.

### 11.4 Duração do lock

O tempo máximo de renovação automática deve superar o tempo normal de processamento de uma mensagem.

O processamento não deve incluir a espera progressiva. A espera é representada por uma nova mensagem agendada.

`PrefetchCount=0` será o ponto inicial para evitar que mensagens permaneçam bloqueadas em memória aguardando um slot por tempo excessivo.

---

## 12. Consistência entre transição e publicação do resultado

A atualização da pré-validação e o envio ao Service Bus não fazem parte de uma única transação distribuída.

Sem uma proteção, poderia ocorrer:

```text
transição aplicada na pré-validação
    -> aplicação cai antes da publicação
    -> orquestrador não recebe o resultado
```

A solução deve utilizar um registro durável de resultado pendente de publicação, seguindo o padrão Outbox:

```text
transação local da aplicação
    |
    +-- aplicar a transição do dossiê
    |
    +-- registrar evento de resultado pendente
```

Depois:

```text
publisher do doctree
    -> lê resultado pendente
    -> envia para a fila de saída
    -> aguarda confirmação do Service Bus
    -> marca como publicado
```

A mensagem de entrada pode ser concluída depois que a transição e o evento de resultado estiverem persistidos. Se houver redelivery, o processor encontra o resultado já registrado e não repete a transição.

O mecanismo de persistência do Outbox não altera o contrato das duas filas.

---

## 13. Segurança com connection string

### 13.1 Configuração da extensão

A extensão aceita diretamente:

```properties
quarkus.azure.servicebus.connection-string=${QUARKUS_AZURE_SERVICEBUS_CONNECTION_STRING}
```

A connection string não será decomposta manualmente em host, usuário e chave. A extensão usa o valor para produzir um `ServiceBusClientBuilder` configurado.

### 13.2 Escopo necessário

Como o `doctree` precisa:

- receber da fila de entrada;
- enviar uma tentativa agendada para a fila de entrada;
- enviar resultados para a fila de saída;

sua connection string deve ser de namespace, sem `EntityPath`, e baseada em uma política SAS dedicada com:

```text
Listen
+
Send
```

O direito `Manage` não é necessário.

O orquestrador deve usar outra política e outra connection string, também de namespace, porque precisa enviar para a entrada e receber da saída.

Não utilizar `RootManageSharedAccessKey` em runtime.

### 13.3 Gestão do segredo

A connection string deverá:

- ser mantida em Secret do ambiente ou em referência ao Azure Key Vault;
- ser injetada como variável de ambiente;
- não ser versionada em Git, Helm values ou `application.properties` com valor real;
- não aparecer em logs, traces, mensagens de erro ou endpoints de configuração;
- possuir processo de rotação pelas chaves primária e secundária da política SAS.

### 13.4 Transporte

O transporte adotado será:

```java
AmqpTransportType.AMQP
```

A comunicação ocorre por AMQP 1.0 protegido por TLS.

Quando a rede corporativa não permitir AMQP direto, poderá ser usado `AMQP_WEB_SOCKETS` pela porta 443 sem alterar os contratos das filas.

---

## 14. Configuração das filas

### 14.1 Fila de entrada

| Propriedade | Configuração |
|---|---|
| Tier | Standard ou Premium |
| Receive mode | Peek-Lock |
| Sessions | Desabilitadas |
| Duplicate detection | Habilitada |
| Partitioning | Conforme decisão de infraestrutura; não é usado para FIFO estrito |
| Lock duration | Compatível com o tempo normal de processamento |
| Auto lock renewal | Configurado no receiver |
| Max delivery count | Definido operacionalmente |
| Dead-letter on expiration | Habilitado |
| TTL | Superior ao prazo máximo do monitoramento e à margem operacional |

### 14.2 Fila de saída

| Propriedade | Configuração |
|---|---|
| Tier | Standard ou Premium |
| Receive mode | Peek-Lock |
| Sessions | Desabilitadas, salvo requisito futuro de ordenação por orquestração |
| Duplicate detection | Habilitada |
| Max delivery count | Definido operacionalmente |
| Dead-letter on expiration | Habilitado |
| TTL | Compatível com o prazo de retomada do orquestrador |

---

## 15. Observabilidade

Cada mensagem deverá produzir logs estruturados e spans com:

```text
monitoramentoId
orquestracaoId
idDossiePreValidacao
idDossieMtr
MessageId
CorrelationId
SequenceNumber
EnqueuedTime
ScheduledEnqueueTime
DeliveryCount
tentativaAtual
limiteEm
situacaoMtr
situacaoPreValidacao
resultadoMonitoramento
motivoQuarentena
```

Spans mínimos:

```text
servicebus.input.receive
prevalidacao.dossie.read
mtr.dossie.read
prevalidacao.dossie.transition
servicebus.input.schedule
servicebus.input.complete
servicebus.input.abandon
servicebus.output.publish
servicebus.output.receive
orquestracao.resume
```

Indicadores mínimos:

- mensagens ativas, agendadas e na DLQ da fila de entrada;
- mensagens ativas e na DLQ da fila de saída;
- idade da mensagem ativa mais antiga;
- tempo total do monitoramento;
- quantidade de tentativas por dossiê;
- falhas técnicas por integração;
- dossiês enviados para `QUARENTENA`;
- resultados pendentes de publicação;
- tempo entre publicação do resultado e retomada do orquestrador.

---

# 16. Sugestão de implementação com `quarkus-azure-servicebus`

## 16.1 Compatibilidade de versões

A plataforma deverá utilizar a última manutenção homologada da linha Quarkus 3.33 LTS, atualmente `3.33.3.1`, com **JDK 25** tanto no build quanto na execução em modo JVM.

O Quarkus passou a oferecer suporte integral ao Java 25 a partir da versão 3.31, incluindo execução em JVM, geração de projetos com alvo Java 25 e build nativo com Mandrel. A linha Quarkus 3.33 LTS foi construída sobre o Quarkus 3.32 e incorpora esse suporte. Portanto, o alvo Java adotado por esta solução será:

```text
Quarkus 3.33.x LTS
+
JDK 25
+
bytecode release 25
```

O `maven.compiler.release=25` implica que o artefato JVM deverá ser executado em runtime Java 25 ou superior; não haverá compatibilidade de execução com JDK 17 ou 21. A imagem de build e a imagem de runtime do container deverão usar JDK 25.

Caso seja exigido build nativo, deverá ser utilizada uma distribuição GraalVM/Mandrel compatível com Java 25. O Mandrel/GraalVM 25 é o padrão da linha Quarkus 3.33 para esse cenário.

A extensão `quarkus-azure-servicebus` está em status `preview`. A matriz publicada do projeto registra a versão `1.2.5` construída com Quarkus `3.37.4` e a versão `1.2.4` construída com Quarkus `3.31.3`; ambas foram construídas e testadas pelo projeto da extensão com Java 17. Isso representa a baseline usada pelo projeto da extensão, não uma comprovação de incompatibilidade com JDK 25. Entretanto, também não constitui homologação explícita da combinação **Quarkus 3.33 + JDK 25**.

Por isso, a versão da extensão deverá permanecer parametrizada e ser fixada somente após teste de compatibilidade com o BOM real do projeto, executado obrigatoriamente em JDK 25.

## 16.2 Dependências Maven

```xml
<properties>
    <maven.compiler.release>25</maven.compiler.release>
    <quarkus.platform.version>3.33.3.1</quarkus.platform.version>
    <quarkus-azure-services.version>VERSAO_HOMOLOGADA_COM_QUARKUS_3_33</quarkus-azure-services.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.quarkus.platform</groupId>
            <artifactId>quarkus-bom</artifactId>
            <version>${quarkus.platform.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <dependency>
            <groupId>io.quarkiverse.azureservices</groupId>
            <artifactId>quarkus-azure-services-bom</artifactId>
            <version>${quarkus-azure-services.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.quarkiverse.azureservices</groupId>
        <artifactId>quarkus-azure-servicebus</artifactId>
    </dependency>

    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-arc</artifactId>
    </dependency>

    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-jackson</artifactId>
    </dependency>

    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-opentelemetry</artifactId>
    </dependency>

    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-smallrye-health</artifactId>
    </dependency>
</dependencies>
```

Não adicionar `quarkus-messaging-amqp` para este fluxo.

O build deverá ser executado com um JDK 25 real. A propriedade `maven.compiler.release=25` define o nível da linguagem, das APIs e do bytecode, mas não substitui a instalação/configuração do JDK 25 no Maven, no pipeline e na imagem de build.

## 16.3 `application.properties`

```properties
# ============================================================
# Azure Service Bus
# O valor real é fornecido por Secret/Key Vault.
# ============================================================
quarkus.azure.servicebus.connection-string=${QUARKUS_AZURE_SERVICEBUS_CONNECTION_STRING}

# Em produção não iniciar o emulador por Dev Services.
%prod.quarkus.azure.servicebus.devservices.enabled=false

# ============================================================
# Filas
# ============================================================
monitoramento.service-bus.input-queue=${SERVICE_BUS_INPUT_QUEUE:q.prevalidacao.monitoramento-mtr.in}
monitoramento.service-bus.output-queue=${SERVICE_BUS_OUTPUT_QUEUE:q.prevalidacao.monitoramento-mtr.out}

# ============================================================
# Listener
# ============================================================
monitoramento.service-bus.consumer.concurrency=${SERVICE_BUS_CONSUMER_CONCURRENCY:4}
monitoramento.service-bus.consumer.prefetch=${SERVICE_BUS_CONSUMER_PREFETCH:0}
monitoramento.service-bus.consumer.max-auto-lock-renew=${SERVICE_BUS_MAX_AUTO_LOCK_RENEW:PT5M}

# ============================================================
# Política funcional de monitoramento
# Valores ilustrativos.
# ============================================================
monitoramento.service-bus.progressive-delays=${SERVICE_BUS_PROGRESSIVE_DELAYS:PT30M,PT3H,PT4H,PT6H}
monitoramento.service-bus.max-attempts=${SERVICE_BUS_MAX_ATTEMPTS:5}
monitoramento.service-bus.max-duration=${SERVICE_BUS_MAX_DURATION:PT24H}
```

## 16.4 Configuração tipada

```java
package br.gov.caixa.simtr.doctree.monitoramentomtr.infraestrutura.servicebus;

import io.smallrye.config.ConfigMapping;

import java.time.Duration;
import java.util.List;

@ConfigMapping(prefix = "monitoramento.service-bus")
public interface MonitoramentoServiceBusConfig {

    String inputQueue();

    String outputQueue();

    Consumer consumer();

    List<Duration> progressiveDelays();

    int maxAttempts();

    Duration maxDuration();

    interface Consumer {
        int concurrency();

        int prefetch();

        Duration maxAutoLockRenew();
    }
}
```

## 16.5 Contratos Java

```java
package br.gov.caixa.simtr.mensageria.contrato;

import java.time.Instant;

public record MonitoramentoDossieEntrada(
        int schemaVersion,
        String monitoramentoId,
        String orquestracaoId,
        String idDossiePreValidacao,
        String idDossieMtr,
        int tentativaAtual,
        Instant iniciadoEm,
        Instant limiteEm,
        String politicaMonitoramentoVersao
) {
}
```

```java
package br.gov.caixa.simtr.mensageria.contrato;

import java.time.Instant;

public record MonitoramentoDossieSaida(
        int schemaVersion,
        String monitoramentoId,
        String orquestracaoId,
        String idDossiePreValidacao,
        String idDossieMtr,
        String resultadoMonitoramento,
        String situacaoMtr,
        String situacaoPreValidacao,
        String motivo,
        int tentativasRealizadas,
        Instant iniciadoEm,
        Instant concluidoEm,
        long inputSequenceNumber
) {
}
```

## 16.6 Criação dos clients

A extensão injeta um `ServiceBusClientBuilder` já configurado pela connection string.

```java
package br.gov.caixa.simtr.doctree.monitoramentomtr.infraestrutura.servicebus;

import com.azure.core.amqp.AmqpTransportType;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceiveMode;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MonitoramentoServiceBusClients {

    @Inject
    ServiceBusClientBuilder clientBuilder;

    @Inject
    MonitoramentoServiceBusConfig config;

    private ServiceBusReceiverAsyncClient inputReceiver;
    private ServiceBusSenderAsyncClient inputSender;
    private ServiceBusSenderAsyncClient outputSender;

    @PostConstruct
    void inicializar() {
        clientBuilder.transportType(AmqpTransportType.AMQP);

        inputReceiver = clientBuilder
                .receiver()
                .queueName(config.inputQueue())
                .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                .disableAutoComplete()
                .prefetchCount(config.consumer().prefetch())
                .maxAutoLockRenewDuration(
                        config.consumer().maxAutoLockRenew()
                )
                .buildAsyncClient();

        inputSender = clientBuilder
                .sender()
                .queueName(config.inputQueue())
                .buildAsyncClient();

        outputSender = clientBuilder
                .sender()
                .queueName(config.outputQueue())
                .buildAsyncClient();
    }

    public ServiceBusReceiverAsyncClient inputReceiver() {
        return inputReceiver;
    }

    public ServiceBusSenderAsyncClient inputSender() {
        return inputSender;
    }

    public ServiceBusSenderAsyncClient outputSender() {
        return outputSender;
    }

    @PreDestroy
    void encerrar() {
        if (inputReceiver != null) {
            inputReceiver.close();
        }
        if (inputSender != null) {
            inputSender.close();
        }
        if (outputSender != null) {
            outputSender.close();
        }
    }
}
```

Os clients são long-lived. Não criar sender ou receiver por mensagem.

## 16.7 Publicação inicial pelo orquestrador

```java
package br.gov.caixa.simtr.orquestrador.infraestrutura.servicebus;

import com.azure.core.amqp.AmqpTransportType;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MonitoramentoEntradaPublisher {

    @Inject
    ServiceBusClientBuilder clientBuilder;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "monitoramento.service-bus.input-queue")
    String inputQueue;

    private ServiceBusSenderAsyncClient sender;

    @PostConstruct
    void inicializar() {
        sender = clientBuilder
                .transportType(AmqpTransportType.AMQP)
                .sender()
                .queueName(inputQueue)
                .buildAsyncClient();
    }

    public Uni<Void> publicar(MonitoramentoDossieEntrada comando) {
        ServiceBusMessage message = new ServiceBusMessage(serializar(comando))
                .setMessageId(
                        comando.monitoramentoId()
                                + ":tentativa:"
                                + comando.tentativaAtual()
                )
                .setCorrelationId(comando.orquestracaoId())
                .setSubject("MONITORAR_DOSSIE_MTR")
                .setContentType("application/json");

        return Uni.createFrom().completionStage(
                () -> sender.sendMessage(message).toFuture()
        );
    }

    private String serializar(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Não foi possível serializar a mensagem de monitoramento",
                    e
            );
        }
    }

    @PreDestroy
    void encerrar() {
        if (sender != null) {
            sender.close();
        }
    }
}
```

O `Uni<Void>` somente termina quando o Azure SDK confirma o envio.

## 16.8 Porta de aplicação

```java
package br.gov.caixa.simtr.doctree.monitoramentomtr.aplicacao.porta.entrada;

import io.smallrye.mutiny.Uni;

public interface MonitorarDossieMtrUseCase {

    Uni<DecisaoMonitoramento> executar(
            MonitorarDossieMtrCommand command
    );
}
```

```java
public sealed interface DecisaoMonitoramento {

    record Reagendar(
            MonitoramentoDossieEntrada proximaMensagem,
            java.time.OffsetDateTime executarEm
    ) implements DecisaoMonitoramento {
    }

    record ResultadoTerminalPersistido(
            String eventId
    ) implements DecisaoMonitoramento {
    }

    record EncerrarIdempotente()
            implements DecisaoMonitoramento {
    }
}
```

A aplicação não conhece `ServiceBusReceivedMessage`, settlement, AMQP ou `SequenceNumber`.

## 16.9 Listener reativo da fila de entrada

```java
package br.gov.caixa.simtr.doctree.monitoramentomtr.infraestrutura.servicebus;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@ApplicationScoped
public class MonitoramentoEntradaListener {

    @Inject
    MonitoramentoServiceBusClients clients;

    @Inject
    MonitoramentoServiceBusConfig config;

    @Inject
    MonitoramentoEntradaMapper mapper;

    @Inject
    MonitorarDossieMtrUseCase useCase;

    @Inject
    DecisaoServiceBusHandler decisaoHandler;

    private Disposable subscription;

    void iniciar(@Observes StartupEvent event) {
        int concorrencia = config.consumer().concurrency();

        subscription = clients.inputReceiver()
                .receiveMessages()

                // Várias mensagens em paralelo, com capacidade limitada.
                // Prefetch do flatMap igual a 1 evita demanda excessiva por slot.
                .flatMap(
                        this::processarMensagem,
                        concorrencia,
                        1
                )

                // Falhas individuais são resolvidas dentro de processarMensagem.
                // Este retry é destinado ao fluxo de recepção.
                .retryWhen(
                        Retry.backoff(
                                        Long.MAX_VALUE,
                                        Duration.ofSeconds(2)
                                )
                                .maxBackoff(Duration.ofMinutes(1))
                )

                .subscribe(
                        ignored -> {
                        },
                        failure -> Log.error(
                                "Fluxo reativo da fila de entrada encerrado",
                                failure
                        ),
                        () -> Log.warn(
                                "Fluxo reativo da fila de entrada foi concluído"
                        )
                );
    }

    private Mono<Void> processarMensagem(
            ServiceBusReceivedMessage message
    ) {
        Log.infof(
                "Mensagem recebida. messageId=%s, sequenceNumber=%d, "
                        + "enqueuedTime=%s, deliveryCount=%d",
                message.getMessageId(),
                message.getSequenceNumber(),
                message.getEnqueuedTime(),
                message.getDeliveryCount()
        );

        return Mono.fromCallable(() -> mapper.mapearEValidar(message))

                // Conversão restrita à borda: Mutiny -> CompletionStage -> Reactor.
                .flatMap(command ->
                        Mono.fromCompletionStage(
                                useCase.executar(command)
                                        .subscribeAsCompletionStage()
                        )
                )

                .flatMap(decisao ->
                        decisaoHandler.aplicar(message, decisao)
                )

                .onErrorResume(
                        MensagemMonitoramentoInvalidaException.class,
                        failure -> clients.inputReceiver().deadLetter(
                                message,
                                new DeadLetterOptions()
                                        .setDeadLetterReason("PAYLOAD_INVALIDO")
                                        .setDeadLetterErrorDescription(
                                                failure.getMessage()
                                        )
                        )
                )

                .onErrorResume(failure -> {
                    Log.errorf(
                            failure,
                            "Falha no processamento. messageId=%s, "
                                    + "sequenceNumber=%d, deliveryCount=%d",
                            message.getMessageId(),
                            message.getSequenceNumber(),
                            message.getDeliveryCount()
                    );

                    return clients.inputReceiver().abandon(message);
                });
    }

    void encerrar(@Observes ShutdownEvent event) {
        if (subscription != null) {
            subscription.dispose();
        }
    }
}
```

Não utilizar no listener:

```java
.block();
.await().indefinitely();
Thread.sleep(...);
```

## 16.10 Reagendamento transacional

```java
package br.gov.caixa.simtr.doctree.monitoramentomtr.infraestrutura.servicebus;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.models.CompleteOptions;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@ApplicationScoped
public class ReagendamentoServiceBus {

    @Inject
    MonitoramentoServiceBusClients clients;

    public Mono<Void> reagendarEConcluir(
            ServiceBusReceivedMessage mensagemAtual,
            ServiceBusMessage proximaMensagem,
            OffsetDateTime executarEm
    ) {
        return clients.inputReceiver()
                .createTransaction()
                .flatMap(transaction ->
                        clients.inputSender()
                                .scheduleMessage(
                                        proximaMensagem,
                                        executarEm,
                                        transaction
                                )
                                .then(
                                        clients.inputReceiver().complete(
                                                mensagemAtual,
                                                new CompleteOptions()
                                                        .setTransactionContext(
                                                                transaction
                                                        )
                                        )
                                )
                                .then(
                                        clients.inputReceiver()
                                                .commitTransaction(transaction)
                                )
                                .onErrorResume(failure ->
                                        clients.inputReceiver()
                                                .rollbackTransaction(transaction)
                                                .onErrorResume(rollbackFailure -> {
                                                    Log.warn(
                                                            "Falha ao executar rollback "
                                                                    + "da transação do Service Bus",
                                                            rollbackFailure
                                                    );
                                                    return Mono.empty();
                                                })
                                                .then(Mono.error(failure))
                                )
                );
    }
}
```

A próxima mensagem deve usar:

```java
new ServiceBusMessage(payload)
        .setMessageId(
                monitoramentoId
                        + ":tentativa:"
                        + proximaTentativa
        )
        .setCorrelationId(orquestracaoId)
        .setSubject("MONITORAR_DOSSIE_MTR")
        .setContentType("application/json");
```

## 16.11 Handler de decisão

```java
package br.gov.caixa.simtr.doctree.monitoramentomtr.infraestrutura.servicebus;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;

@ApplicationScoped
public class DecisaoServiceBusHandler {

    @Inject
    MonitoramentoServiceBusClients clients;

    @Inject
    ReagendamentoServiceBus reagendamento;

    @Inject
    MonitoramentoMessageMapper mapper;

    public Mono<Void> aplicar(
            ServiceBusReceivedMessage mensagemAtual,
            DecisaoMonitoramento decisao
    ) {
        if (decisao instanceof DecisaoMonitoramento.Reagendar valor) {
            return reagendamento.reagendarEConcluir(
                    mensagemAtual,
                    mapper.paraMensagemEntrada(valor.proximaMensagem()),
                    valor.executarEm()
            );
        }

        if (decisao instanceof
                DecisaoMonitoramento.ResultadoTerminalPersistido) {
            return clients.inputReceiver().complete(mensagemAtual);
        }

        if (decisao instanceof
                DecisaoMonitoramento.EncerrarIdempotente) {
            return clients.inputReceiver().complete(mensagemAtual);
        }

        return Mono.error(
                new IllegalStateException(
                        "Decisão não suportada: "
                                + decisao.getClass().getName()
                )
        );
    }
}
```

`ResultadoTerminalPersistido` significa que a transição e o evento a publicar já foram registrados de forma durável pela aplicação.

## 16.12 Publicação na fila de saída

```java
package br.gov.caixa.simtr.doctree.monitoramentomtr.infraestrutura.servicebus;

import com.azure.messaging.servicebus.ServiceBusMessage;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MonitoramentoResultadoPublisher {

    @Inject
    MonitoramentoServiceBusClients clients;

    @Inject
    MonitoramentoMessageMapper mapper;

    public Uni<Void> publicar(MonitoramentoDossieSaida resultado) {
        ServiceBusMessage message = mapper.paraMensagemSaida(resultado);

        return Uni.createFrom().completionStage(
                () -> clients.outputSender()
                        .sendMessage(message)
                        .toFuture()
        );
    }
}
```

Mapeamento:

```java
public ServiceBusMessage paraMensagemSaida(
        MonitoramentoDossieSaida resultado
) {
    return new ServiceBusMessage(serializar(resultado))
            .setMessageId(
                    resultado.monitoramentoId()
                            + ":resultado:v1"
            )
            .setCorrelationId(resultado.orquestracaoId())
            .setSubject("RESULTADO_MONITORAMENTO_DOSSIE_MTR")
            .setContentType("application/json");
}
```

O registro Outbox somente pode ser marcado como publicado depois da conclusão desse `Uni`.

## 16.13 Listener da fila de saída no orquestrador

O orquestrador utiliza o mesmo padrão de client assíncrono:

```java
outputReceiver.receiveMessages()
        .flatMap(
                message -> processarResultado(message)
                        .then(outputReceiver.complete(message))
                        .onErrorResume(failure ->
                                outputReceiver.abandon(message)
                        ),
                concorrencia,
                1
        )
        .subscribe();
```

`processarResultado(message)` deve:

1. validar o contrato;
2. localizar a execução por `orquestracaoId`;
3. aplicar o resultado de forma idempotente;
4. persistir a retomada, finalização ou suspensão;
5. somente então permitir `Complete`.

## 16.14 Estrutura sugerida

### Orquestrador

```text
infraestrutura/servicebus
├── OrquestradorServiceBusClients.java
├── MonitoramentoEntradaPublisher.java
├── MonitoramentoResultadoListener.java
├── MonitoramentoMessageMapper.java
└── MonitoramentoServiceBusConfig.java
```

### `doctree`

```text
monitoramentomtr
├── aplicacao
│   ├── comando
│   ├── porta
│   │   ├── entrada
│   │   └── saida
│   └── servico
├── dominio
└── infraestrutura
    └── servicebus
        ├── MonitoramentoServiceBusClients.java
        ├── MonitoramentoEntradaListener.java
        ├── MonitoramentoEntradaMapper.java
        ├── MonitoramentoMessageMapper.java
        ├── DecisaoServiceBusHandler.java
        ├── ReagendamentoServiceBus.java
        ├── PoliticaReagendamento.java
        └── MonitoramentoResultadoPublisher.java
```

---

## 17. Testes mínimos

1. Confirmar `java -version` e `mvn -version` usando JDK 25 no ambiente local, pipeline e imagem de build.
2. Executar `mvn clean verify` com `maven.compiler.release=25` e validar a inicialização do Quarkus em modo JVM com JDK 25.
3. Injetar o `ServiceBusClientBuilder` usando a connection string externa.
4. Publicar o primeiro comando na fila de entrada.
5. Confirmar recepção contínua por `ServiceBusReceiverAsyncClient.receiveMessages()`.
6. Confirmar `PEEK_LOCK` e auto-complete desabilitado.
7. Processar com sucesso e validar `Complete`.
8. Simular falha recuperável e validar `Abandon` e incremento de `DeliveryCount`.
9. Enviar contrato inválido e validar DLQ com motivo.
10. Retornar situação não conclusiva e validar, na mesma transação:
    - agendamento da próxima tentativa;
    - `Complete` da mensagem atual.
11. Confirmar que a mensagem agendada somente fica disponível no horário definido.
12. Publicar várias mensagens e validar concorrência limitada sem FIFO global.
13. Confirmar que as mensagens ativas mais antigas tendem a ocupar primeiro os slots disponíveis.
14. Confirmar que a ordem de conclusão pode variar.
15. Confirmar que `SequenceNumber` é registrado, mas não utilizado para selecionar a mensagem ativa.
16. Validar transição terminal e criação do resultado pendente.
17. Publicar o resultado na fila de saída com `MessageId` determinístico.
18. Repetir a publicação e validar duplicate detection dentro da janela configurada.
19. Consumir o resultado no orquestrador e validar retomada idempotente.
20. Reiniciar o pod durante o processamento e validar redelivery sem duplicação de efeito.
21. Validar rotação da connection string sem expor o segredo.
22. Executar dependency tree e, se aplicável, build nativo com GraalVM/Mandrel para Java 25, usando a versão selecionada da extensão.

---

## 18. Conclusão

A solução utiliza duas filas do Azure Service Bus para separar o orquestrador do processamento de monitoramento executado pelo `doctree`.

A implementação fica definida sobre **Quarkus 3.33 LTS e JDK 25** da seguinte forma:

```text
orquestrador
    -> ServiceBusSenderAsyncClient
    -> fila de entrada

doctree
    -> ServiceBusReceiverAsyncClient.receiveMessages()
    -> processamento reativo e paralelo
    -> Peek-Lock e settlement explícito
    -> nova tentativa por scheduleMessage
    -> resultado terminal por fila de saída

orquestrador
    -> ServiceBusReceiverAsyncClient.receiveMessages()
    -> retoma, finaliza ou suspende o fluxo
```

A connection string será entregue à extensão por:

```properties
quarkus.azure.servicebus.connection-string=${QUARKUS_AZURE_SERVICEBUS_CONNECTION_STRING}
```

A solução não usa `@Incoming`, não usa FIFO global e não usa `SequenceNumber` para buscar uma mensagem ativa específica.

A garantia é:

```text
entrega at-least-once
+
mensagens ativas mais antigas normalmente recebidas primeiro
+
processamento paralelo com concorrência limitada
+
mensagens agendadas para espera progressiva
+
settlement explícito
+
processamento idempotente
+
resultado durável e correlacionado
```

---

## 19. Referências oficiais

### Quarkus

- [Quarkus Azure Service Bus Extension](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html)
- [Quarkus Azure Service Bus no catálogo de extensões](https://quarkus.io/extensions/io.quarkiverse.azureservices/quarkus-azure-servicebus/)
- [Quarkus 3.31 — suporte integral ao Java 25](https://quarkus.io/blog/quarkus-3-31-released/)
- [Quarkus 3.33 LTS](https://quarkus.io/blog/quarkus-3-33-released/)
- [Quarkus 3.33.3.1 — LTS emergency release](https://quarkus.io/blog/quarkus-3-33-3-1-released/)
- [GraalVM/Mandrel 25 e suporte de JDKs no Quarkus](https://quarkus.io/blog/mandrel-25-minimum-version/)
- [Quarkus releases](https://quarkus.io/releases/)

### Azure Service Bus e Azure SDK for Java

- [ServiceBusClientBuilder](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusclientbuilder)
- [ServiceBusReceiverAsyncClient](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusreceiverasyncclient)
- [ServiceBusSenderAsyncClient](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebussenderasyncclient)
- [Queues, topics and subscriptions](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-queues-topics-subscriptions)
- [Message sequencing and timestamps](https://learn.microsoft.com/en-us/azure/service-bus-messaging/message-sequencing)
- [Prefetch](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-prefetch)
- [Duplicate detection](https://learn.microsoft.com/en-us/azure/service-bus-messaging/duplicate-detection)
- [Transactions](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-transactions)
- [AMQP 1.0 protocol guide](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-amqp-protocol-guide)
- [Authentication and authorization](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-authentication-and-authorization)
- [Shared Access Signatures](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-sas)
