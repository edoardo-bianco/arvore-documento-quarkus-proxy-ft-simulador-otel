# Sincronização Assíncrona da Situação dos Dossiês entre o MTR e a Pré-Validação com Azure Service Bus

Implementação com duas filas do Azure Service Bus, processamento paralelo das mensagens ativas mais antigas, monitoramento com intervalos progressivos, atualização da pré-validação pelo `doctree` e retomada do fluxo pelo orquestrador.

## Sumário

- [1. Objetivo](#1-objetivo)
- [2. Decisão arquitetural](#2-decisão-arquitetural)
- [3. Topologia da solução](#3-topologia-da-solução)
- [4. Responsabilidades dos componentes](#4-responsabilidades-dos-componentes)
- [5. Fila de entrada](#5-fila-de-entrada)
- [6. Fila de saída](#6-fila-de-saída)
- [7. Fluxo completo de monitoramento](#7-fluxo-completo-de-monitoramento)
- [8. Processamento paralelo com prioridade para mensagens mais antigas](#8-processamento-paralelo-com-prioridade-para-mensagens-mais-antigas)
- [9. Uso do `SequenceNumber`](#9-uso-do-sequencenumber)
- [10. Recebimento com Peek-Lock](#10-recebimento-com-peek-lock)
- [11. Processamento idempotente](#11-processamento-idempotente)
- [12. Intervalos progressivos com mensagens agendadas](#12-intervalos-progressivos-com-mensagens-agendadas)
- [13. Limites de tentativas e de tempo](#13-limites-de-tentativas-e-de-tempo)
- [14. Situações retornadas pelo MTR e regras de transição](#14-situações-retornadas-pelo-mtr-e-regras-de-transição)
- [15. Retomada do fluxo pelo orquestrador](#15-retomada-do-fluxo-pelo-orquestrador)
- [16. Tentativa funcional e falha técnica](#16-tentativa-funcional-e-falha-técnica)
- [17. Consistência entre a transição e a publicação do resultado](#17-consistência-entre-a-transição-e-a-publicação-do-resultado)
- [18. Implementação com Quarkus 3.33 LTS e Azure SDK assíncrono](#18-implementação-com-quarkus-333-lts-e-azure-sdk-assíncrono)
- [19. Organização modular no `doctree`](#19-organização-modular-no-doctree)
- [20. Configuração das filas](#20-configuração-das-filas)
- [21. Segurança](#21-segurança)
- [22. Observabilidade](#22-observabilidade)
- [23. Cenários mínimos de teste](#23-cenários-mínimos-de-teste)
- [24. Resultado arquitetural](#24-resultado-arquitetural)
- [25. Referências oficiais](#25-referências-oficiais)

---

## 1. Objetivo

Implementar uma solução assíncrona para acompanhar a situação de um Dossiê de Produto enviado ao MTR, atualizar a situação correspondente na pré-validação e devolver o resultado ao orquestrador responsável pela continuidade do fluxo.

A solução utilizará:

- uma fila de entrada para receber os comandos de monitoramento;
- um processor reativo no módulo `doctree`;
- consultas individuais ao dossiê da pré-validação e ao Dossiê de Produto no MTR;
- mensagens agendadas para realizar novas consultas em intervalos progressivos;
- limite máximo de tentativas;
- prazo máximo de monitoramento;
- transição para `QUARENTENA` quando um dos limites for atingido;
- uma fila de saída para devolver o resultado ao orquestrador;
- processamento paralelo das mensagens disponíveis, priorizando as mensagens ativas mais antigas;
- entrega `at-least-once`, Peek-Lock e processamento idempotente.

Não haverá:

- listagem periódica de dossiês;
- filtro de dossiês por período;
- scheduler que percorra uma coleção;
- bloqueio da mensagem durante horas;
- FIFO global de processamento;
- uso do `SequenceNumber` para localizar diretamente uma mensagem ativa.

---

## 2. Decisão arquitetural

O Azure Service Bus será utilizado para separar temporal e operacionalmente:

- o envio do dossiê ao MTR;
- o acompanhamento da análise realizada pelo MTR;
- a atualização da situação na pré-validação;
- a retomada posterior da orquestração.

O orquestrador não permanecerá bloqueado aguardando a conclusão da análise no MTR. Depois de enviar o dossiê e colocá-lo em `EM_ANALISE_ENVIO_MTR`, publicará um comando na fila de entrada. O `doctree` realizará o monitoramento e, quando houver uma saída conclusiva ou quarentena, publicará o resultado na fila de saída. O orquestrador consumirá esse resultado e retomará ou finalizará o fluxo.

O processor será inicialmente implementado de forma modular dentro do `doctree`. A responsabilidade de consumo e monitoramento deverá permanecer isolada para permitir sua futura extração para um worker dedicado, com ciclo de vida, implantação e escala próprios.

A fila oferece:

- desacoplamento temporal;
- buffer durável;
- distribuição de carga entre consumidores concorrentes;
- redelivery em falhas;
- mensagens agendadas;
- Dead-Letter Queue;
- detecção de duplicidade;
- transações entre operações internas do Service Bus.

---

## 3. Topologia da solução

Nomes ilustrativos:

```text
q.prevalidacao.monitoramento-mtr.in
q.prevalidacao.monitoramento-mtr.out
```

Fluxo:

```text
Orquestrador
    |
    | 1. envia o Dossiê de Produto ao MTR
    | 2. atualiza a pré-validação para EM_ANALISE_ENVIO_MTR
    | 3. publica o primeiro comando de monitoramento
    v
Fila de entrada
q.prevalidacao.monitoramento-mtr.in
    |
    | Peek-Lock
    | mensagens mais antigas disponibilizadas primeiro
    | múltiplas mensagens processadas em paralelo
    v
Processor Quarkus do doctree
    |
    | consulta a situação atual na pré-validação
    | consulta o Dossiê de Produto no MTR via simtr-hub
    |
    +-- situação ainda não conclusiva
    |       |
    |       v
    |   agenda nova mensagem na fila de entrada
    |   com intervalo progressivo
    |
    +-- CONFORME
    |       |
    |       v
    |   atualiza a pré-validação para CONFORME
    |   registra resultado para publicação
    |
    +-- NAO_CONFORME
    |       |
    |       v
    |   atualiza a pré-validação para NAO_CONFORME
    |   registra resultado para publicação
    |
    +-- PENDENTE_INFORMACAO
    |       |
    |       v
    |   atualiza a pré-validação para NAO_CONFORME
    |   preserva PENDENTE_INFORMACAO no resultado
    |
    +-- prazo ou tentativas esgotados
            |
            v
        atualiza para QUARENTENA
        registra resultado para publicação
            |
            v
Fila de saída
q.prevalidacao.monitoramento-mtr.out
    |
    | Peek-Lock
    v
Orquestrador
    |
    +-- CONFORME
    |       -> finaliza o fluxo
    |
    +-- PENDENTE_INFORMACAO
    |       -> retoma a complementação
    |
    +-- NAO_CONFORME
    |       -> aplica a regra correspondente
    |
    +-- QUARENTENA
            -> suspende a automação
            -> sinaliza ação humana
```

---

## 4. Responsabilidades dos componentes

| Componente | Responsabilidade |
|---|---|
| Orquestrador | Enviar o dossiê ao MTR, atualizar para `EM_ANALISE_ENVIO_MTR`, publicar o primeiro comando, consumir o resultado e retomar ou finalizar o fluxo |
| Fila de entrada | Manter comandos ativos e agendados de monitoramento |
| Processor do `doctree` | Validar a mensagem, consultar a pré-validação, consultar o MTR, aplicar as regras, reagendar ou concluir o monitoramento |
| `simtr-hub` | Disponibilizar a capacidade `ConsultarDossieProduto` por porta CDI |
| Persistência do `doctree` | Manter a situação do dossiê e o resultado pendente de publicação |
| Fila de saída | Transportar o resultado conclusivo ou de quarentena ao orquestrador |
| Consumer do orquestrador | Correlacionar o resultado, persistir a retomada e executar `Complete` somente depois do tratamento correto |

---

## 5. Fila de entrada

### 5.1 Responsabilidade

A fila de entrada receberá comandos para verificar um dossiê específico. Não será necessário buscar uma lista de dossiês pendentes.

Produtores:

```text
Orquestrador
Processor do doctree, ao agendar uma nova tentativa
```

Consumidor:

```text
Processor do doctree
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
  "iniciadoEm": "2026-08-29T15:00:00Z",
  "limiteEm": "2026-08-30T15:00:00Z",
  "politicaMonitoramentoVersao": "v1"
}
```

### 5.3 Campos

| Campo | Responsabilidade |
|---|---|
| `monitoramentoId` | Identificar unicamente um ciclo de monitoramento |
| `orquestracaoId` | Permitir que o orquestrador localize a execução a ser retomada |
| `idDossiePreValidacao` | Consultar e atualizar o dossiê na pré-validação |
| `idDossieMtr` | Consultar o Dossiê de Produto no MTR |
| `tentativaAtual` | Controlar tentativas funcionais de consulta |
| `iniciadoEm` | Registrar o início do monitoramento |
| `limiteEm` | Definir o prazo absoluto de monitoramento |
| `politicaMonitoramentoVersao` | Identificar a política de intervalos e limites aplicada |

### 5.4 Propriedades AMQP

| Propriedade | Valor |
|---|---|
| `MessageId` | `<monitoramentoId>:tentativa:<tentativaAtual>` |
| `CorrelationId` | `orquestracaoId` |
| `Subject` | `MONITORAR_DOSSIE_MTR` |
| `ContentType` | `application/json` |
| `ScheduledEnqueueTime` | Preenchido apenas nas tentativas posteriores |
| `SequenceNumber` | Atribuído pelo Service Bus |

A fila de entrada não utilizará uma sessão global. Isso permite que vários consumers processem mensagens diferentes em paralelo.

---

## 6. Fila de saída

### 6.1 Responsabilidade

A fila de saída será consumida pelo orquestrador e receberá somente resultados que encerrem o ciclo atual de monitoramento:

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

### 6.2 Contrato do resultado conclusivo

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
  "iniciadoEm": "2026-08-29T15:00:00Z",
  "concluidoEm": "2026-08-29T20:30:00Z",
  "inputSequenceNumber": 13527
}
```

### 6.3 Contrato de quarentena

```json
{
  "schemaVersion": 1,
  "monitoramentoId": "MON-6ec73bf9-b390-417c-b2c2-f34fd7f68a63",
  "orquestracaoId": "ORQ-25c62d41-2305-4661-a3c8-cdbdf681c1f2",
  "idDossiePreValidacao": "c5a13bd2-fc7e-45cd-9c47-8a79b5926c86",
  "idDossieMtr": "123456789",
  "resultadoMonitoramento": "QUARENTENA",
  "situacaoMtr": null,
  "situacaoPreValidacao": "QUARENTENA",
  "motivo": "MAXIMO_TENTATIVAS_ATINGIDO",
  "tentativasRealizadas": 5,
  "iniciadoEm": "2026-08-29T15:00:00Z",
  "concluidoEm": "2026-08-30T15:00:00Z",
  "inputSequenceNumber": 13980
}
```

### 6.4 Propriedades AMQP

| Propriedade | Valor |
|---|---|
| `MessageId` | `<monitoramentoId>:resultado:v1` |
| `CorrelationId` | `orquestracaoId` |
| `Subject` | `RESULTADO_MONITORAMENTO_DOSSIE_MTR` |
| `ContentType` | `application/json` |

A correlação funcional será realizada por `orquestracaoId` e `monitoramentoId`.

Se a fila de saída já utilizar sessões, poderá manter:

```text
SessionId = orquestracaoId
```

Nesse caso, o orquestrador deverá consumir várias sessões em paralelo. Isso preserva a ordem apenas dentro de uma mesma orquestração e não cria FIFO global.

Como o desenho atual produz uma única mensagem terminal por ciclo de monitoramento, sessões na fila de saída são opcionais.

---

## 7. Fluxo completo de monitoramento

1. O orquestrador envia o Dossiê de Produto ao MTR.
2. O orquestrador atualiza o dossiê da pré-validação para `EM_ANALISE_ENVIO_MTR`.
3. O orquestrador publica a primeira mensagem na fila de entrada.
4. O Service Bus entrega a mensagem a um consumer em Peek-Lock.
5. O processor valida o contrato e os identificadores.
6. O processor consulta a situação atual do dossiê da pré-validação.
7. Se o dossiê não estiver mais em `EM_ANALISE_ENVIO_MTR`, nenhuma nova transição será aplicada.
8. Se ainda estiver em `EM_ANALISE_ENVIO_MTR`, o processor verifica o prazo e o número de tentativas.
9. Se algum limite tiver sido atingido, o dossiê será encaminhado para `QUARENTENA`.
10. Se os limites não tiverem sido atingidos, o processor consulta `ConsultarDossieProduto` por meio do `simtr-hub`.
11. Se o MTR retornar situação conclusiva, o processor aplica a transição e registra o resultado para publicação.
12. Se o MTR retornar situação ainda não conclusiva, o processor calcula o próximo intervalo e agenda uma nova mensagem.
13. Falhas técnicas recuperáveis provocam `Abandon` da mensagem atual.
14. O publisher da Outbox envia o resultado conclusivo para a fila de saída.
15. O orquestrador consome o resultado e retoma, finaliza ou suspende o fluxo.

---

## 8. Processamento paralelo com prioridade para mensagens mais antigas

### 8.1 Decisão

A fila de entrada não utilizará FIFO global estrito.

O requisito será:

> Processar várias mensagens em paralelo, fazendo com que as mensagens ativas mais antigas sejam solicitadas e iniciadas antes das mensagens ativas mais novas, sem garantir a ordem de conclusão.

O Service Bus mantém as mensagens ordenadas e timestamped quando elas chegam. Em uma queue com competing consumers, os receivers normalmente recebem as mensagens na ordem em que foram adicionadas. Cada mensagem é entregue a somente um consumer enquanto seu lock estiver válido.

### 8.2 Modelo de concorrência

Exemplo com concorrência igual a três:

```text
Fila ativa:
M1  M2  M3  M4  M5  M6
^ mais antiga             mais nova ^

Slots de processamento:
slot 1 <- M1
slot 2 <- M2
slot 3 <- M3
```

As três mensagens mais antigas disponíveis são iniciadas antes de `M4`, `M5` e `M6`.

Entretanto, a conclusão pode ocorrer nesta ordem:

```text
M3 termina primeiro
M1 termina depois
M2 termina por último
```

Quando um slot é liberado, a próxima mensagem disponível é solicitada:

```text
slot liberado <- M4
```

### 8.3 Garantias e limites

| Comportamento | Garantia da solução |
|---|---|
| Mensagens disponíveis mais antigas são solicitadas antes das mais novas | Preferência de despacho da queue e controle de demanda do consumer |
| Várias mensagens são processadas ao mesmo tempo | Sim |
| Uma mensagem é processada por somente um receiver enquanto o lock estiver válido | Sim |
| A mensagem mais antiga sempre termina antes das demais | Não |
| Ordem global estrita entre início e conclusão | Não |
| Uma mensagem abandonada pode ser entregue novamente depois de mensagens mais novas | Sim |
| Uma mensagem agendada participa da ordem antes de seu horário | Não |
| Uma mensagem agendada recebe nova posição quando se torna ativa | Sim |

### 8.4 Configuração recomendada

Para favorecer as mensagens mais antigas sem serializar o processamento:

```text
Sessions na fila de entrada = desabilitadas
ReceiveMode                 = PEEK_LOCK
PrefetchCount               = 0 inicialmente
Concorrência por instância  = configurável
Número de réplicas          = configurável
AutoComplete                = desabilitado
```

O `PrefetchCount` inicial igual a zero evita que uma instância bloqueie antecipadamente um grande conjunto de mensagens antigas em memória, enquanto outras instâncias recebem mensagens posteriores. Também reduz o risco de expiração do lock dentro do buffer local.

Depois de testes de carga, poderá ser adotado um prefetch pequeno, limitado ao número de processamentos concorrentes ou a um múltiplo baixo desse valor. Não deverá ser utilizado um prefetch grande sem medir:

- idade da mensagem;
- tempo de lock restante;
- distribuição entre pods;
- taxa de redelivery;
- throughput;
- latência do MTR.

### 8.5 Concorrência total

```text
concorrência total aproximada
=
réplicas do processor
×
concorrência por instância
```

Exemplo:

```text
3 pods
×
4 mensagens por pod
=
até 12 processamentos simultâneos
```

O valor deverá respeitar a capacidade:

- do `doctree`;
- do `simtr-hub`;
- do MTR;
- do armazenamento da pré-validação;
- do namespace do Service Bus.

### 8.6 Limite conceitual

Não existe simultaneamente:

```text
paralelismo irrestrito
+
FIFO global estrito de conclusão
```

Ao escolher processamento paralelo, a solução preserva a prioridade de leitura das mensagens ativas mais antigas, mas aceita que mensagens posteriores possam terminar primeiro.

---

## 9. Uso do `SequenceNumber`

### 9.1 Responsabilidade

O `SequenceNumber`:

- é atribuído pelo Service Bus;
- é somente leitura;
- representa a posição atribuída pelo broker;
- será utilizado para logs, tracing, auditoria e diagnóstico;
- não substituirá `MessageId`, `monitoramentoId` ou `orquestracaoId`.

### 9.2 Não utilizar para receber diretamente uma mensagem ativa

Não deverá ser implementado:

```text
peek das mensagens
    -> localizar menor SequenceNumber
    -> tentar consumir diretamente aquela mensagem ativa
```

O `peekMessage(sequenceNumber)` apenas navega ou visualiza mensagens. Ele não aplica lock nem consome a mensagem.

O recebimento por `SequenceNumber` é aplicável a mensagens explicitamente colocadas em estado `Deferred`.

### 9.3 Mensagens agendadas

Ao agendar uma mensagem, o Service Bus retorna um `SequenceNumber` que pode ser utilizado para cancelar o agendamento enquanto a mensagem permanecer agendada.

Quando chega o horário de ativação:

1. a mensagem é anexada à fila ativa;
2. recebe um novo `SequenceNumber`;
3. passa a participar da ordem das mensagens disponíveis naquele momento.

Portanto, a ordem considerada pelo consumer será a das mensagens ativas, não a data em que o ciclo de monitoramento começou.

### 9.4 Particionamento

Para simplificar a auditoria de antiguidade por `SequenceNumber`, recomenda-se utilizar uma queue de entrada não particionada.

Se a entidade for particionada, deverá ser utilizado também `EnqueuedTime` para avaliar a idade, pois o `SequenceNumber` incorpora informação da partição e não deve ser tratado isoladamente como uma sequência global de negócio.

---

## 10. Recebimento com Peek-Lock

O processor utilizará:

```text
ServiceBusReceiveMode.PEEK_LOCK
```

Fluxo de sucesso:

```text
mensagem recebida
    -> lock aplicado pelo broker
    -> processamento concluído
    -> Complete
    -> mensagem removida
```

Fluxo de falha recuperável:

```text
mensagem recebida
    -> lock aplicado
    -> falha técnica
    -> Abandon
    -> mensagem volta a ficar disponível
    -> DeliveryCount incrementado
```

Fluxo de falha permanente do contrato:

```text
payload inválido
    -> DeadLetter
    -> motivo e descrição registrados
```

A solução não utilizará `RECEIVE_AND_DELETE`, porque uma queda depois da recepção e antes do processamento provocaria perda da mensagem.

O `Complete` somente ocorrerá depois que a decisão correspondente tiver sido executada corretamente.

---

## 11. Processamento idempotente

O Service Bus fornece entrega `at-least-once`. Uma mensagem pode ser redeliverada quando ocorrer:

- `Abandon`;
- expiração do lock;
- queda do pod;
- perda de conexão;
- falha no `Complete`;
- rolling update;
- falha depois de um efeito externo e antes do settlement.

O processor deverá ser idempotente.

### 11.1 Regra de origem

As transições deste monitoramento somente serão aplicadas quando o estado atual for:

```text
EM_ANALISE_ENVIO_MTR
```

Se o dossiê já estiver em `CONFORME`, `NAO_CONFORME`, `QUARENTENA` ou outro estado posterior, a mesma transição não deverá ser repetida.

### 11.2 Resultado pendente de publicação

O processor não deverá concluir silenciosamente uma mensagem redeliverada apenas porque a situação já mudou.

Ele deverá verificar se o resultado correspondente:

- já foi registrado;
- está pendente de publicação;
- já foi publicado.

Comportamento:

```text
estado já alterado + resultado pendente
    -> garantir publicação pela Outbox
    -> Complete idempotente

estado já alterado + resultado publicado
    -> no-op
    -> Complete
```

### 11.3 Identificadores determinísticos

Mensagem de tentativa:

```text
<monitoramentoId>:tentativa:<numero>
```

Mensagem de resultado:

```text
<monitoramentoId>:resultado:v1
```

Esses valores permitem detecção de duplicidade no broker e idempotência no consumer.

---

## 12. Intervalos progressivos com mensagens agendadas

O intervalo não será implementado com:

```java
Thread.sleep(...);
```

Também não será mantido o lock da mensagem durante horas.

Quando o MTR ainda não possuir uma situação conclusiva:

```text
mensagem atual processada
    -> incrementar tentativa funcional
    -> calcular próxima execução
    -> agendar nova mensagem
    -> Complete na mensagem atual
```

### 12.1 Configuração externa

Exemplo ilustrativo:

```properties
monitoramento.mtr.intervalos=PT30M,PT3H,PT4H,PT6H
monitoramento.mtr.maximo-tentativas=5
monitoramento.mtr.prazo-maximo=PT24H
```

Interpretação:

| Depois da tentativa | Intervalo antes da próxima tentativa |
|---:|---:|
| 1 | 30 minutos |
| 2 | 3 horas |
| 3 | 4 horas |
| 4 | 6 horas |

Os valores definitivos deverão ser definidos externamente.

### 12.2 Cálculo

```java
Duration intervalo = intervalos.get(
        Math.min(tentativaAtual - 1, intervalos.size() - 1)
);

Instant proximaExecucaoCalculada =
        Instant.now().plus(intervalo);

Instant proximaExecucao =
        proximaExecucaoCalculada.isAfter(limiteEm)
                ? limiteEm
                : proximaExecucaoCalculada;
```

Se a próxima ativação ocorrer exatamente em `limiteEm`, o processor deverá verificar o prazo antes de consultar o MTR novamente.

### 12.3 Operação transacional no Service Bus

Para situação não conclusiva, estas operações deverão ocorrer na mesma transação do Service Bus:

```text
agendar próxima mensagem na fila de entrada
+
Complete da mensagem atual
```

Isso evita:

```text
Complete sem próxima tentativa
```

ou:

```text
próxima tentativa criada sem concluir a atual
```

---

## 13. Limites de tentativas e de tempo

O monitoramento será encerrado quando ocorrer a primeira condição:

```text
situação conclusiva no MTR
OU
máximo de tentativas
OU
prazo máximo
OU
dossiê não está mais em EM_ANALISE_ENVIO_MTR
```

Regra:

```java
boolean limiteAtingido =
        tentativaAtual > maximoTentativas
        || !Instant.now().isBefore(limiteEm);
```

Depois de uma consulta não conclusiva, deverá ser verificado se a tentativa executada atingiu o máximo antes de criar a próxima mensagem.

### 13.1 Quarentena

Se o prazo ou o número de tentativas for atingido:

1. consultar novamente a situação atual;
2. confirmar que permanece em `EM_ANALISE_ENVIO_MTR`;
3. atualizar para `QUARENTENA`;
4. registrar o resultado com o motivo;
5. publicar o resultado na fila de saída;
6. permitir que o orquestrador suspenda a automação e sinalize ação humana.

Motivos mínimos:

```text
MAXIMO_TENTATIVAS_ATINGIDO
PRAZO_MAXIMO_ATINGIDO
```

---

## 14. Situações retornadas pelo MTR e regras de transição

O MTR permanece como fonte da verdade para a situação de conformidade.

| Situação no MTR | Situação aplicada na pré-validação | Resultado enviado ao orquestrador |
|---|---|---|
| `CONFORME` | `CONFORME` | `situacaoMtr=CONFORME` |
| `NAO_CONFORME` | `NAO_CONFORME` | `situacaoMtr=NAO_CONFORME` |
| `PENDENTE_INFORMACAO` | `NAO_CONFORME` | `situacaoMtr=PENDENTE_INFORMACAO` |
| Situação não conclusiva | Nenhuma transição | Nova tentativa agendada |
| Limite atingido | `QUARENTENA` | `resultadoMonitoramento=QUARENTENA` |

É obrigatório preservar na mensagem de saída:

```text
situacaoMtr
situacaoPreValidacao
```

Isso permite distinguir:

```text
MTR = NAO_CONFORME
```

 de:

```text
MTR = PENDENTE_INFORMACAO
```

mesmo que ambos resultem em `NAO_CONFORME` na pré-validação.

---

## 15. Retomada do fluxo pelo orquestrador

O orquestrador consumirá a fila de saída em Peek-Lock.

### 15.1 `CONFORME`

```text
receber resultado
    -> localizar orquestração por orquestracaoId
    -> persistir conclusão
    -> finalizar o fluxo
    -> Complete
```

### 15.2 `PENDENTE_INFORMACAO`

```text
receber resultado
    -> localizar orquestração
    -> reconhecer que a pré-validação está NAO_CONFORME
    -> retomar a complementação de informações ou documentos
    -> persistir retomada
    -> Complete
```

Quando houver novo envio ao MTR, deverá ser criado um novo `monitoramentoId`, com contadores e prazo reiniciados.

### 15.3 `NAO_CONFORME`

O orquestrador deverá aplicar a regra já definida para esse resultado. Esta solução apenas transporta a situação original do MTR e a situação aplicada na pré-validação.

### 15.4 `QUARENTENA`

```text
receber QUARENTENA
    -> suspender continuidade automática
    -> registrar o motivo
    -> sinalizar necessidade de ação humana
    -> Complete
```

O orquestrador somente deverá executar `Complete` depois de persistir corretamente a retomada, finalização ou suspensão.

---

## 16. Tentativa funcional e falha técnica

### 16.1 Tentativa funcional

Ocorre quando a consulta ao MTR foi concluída corretamente, mas a situação ainda não é conclusiva.

Resultado:

```text
incrementar tentativaAtual
agendar próxima mensagem
Complete na mensagem atual
```

### 16.2 Falha técnica

Exemplos:

- timeout no `simtr-hub`;
- indisponibilidade do MTR;
- falha de rede;
- falha transitória de persistência;
- falha ao criar o próximo agendamento;
- falha no settlement.

Resultado recuperável:

```text
não incrementar tentativa funcional
Abandon na mensagem atual
```

O prazo absoluto `limiteEm` continua avançando.

### 16.3 Contadores diferentes

```text
tentativaAtual
    -> número de consultas funcionais concluídas

DeliveryCount
    -> número de entregas técnicas malsucedidas da mesma mensagem
```

Quando `DeliveryCount` exceder o `MaxDeliveryCount`, a mensagem será movida para a DLQ.

### 16.4 Quarentena não é DLQ

| Destino | Significado |
|---|---|
| `QUARENTENA` | Resultado funcional: prazo ou tentativas de negócio esgotados |
| DLQ da entrada | Mensagem tecnicamente não processável ou falhas técnicas repetidas |
| DLQ da saída | Resultado não processado pelo orquestrador após redeliveries |

---

## 17. Consistência entre a transição e a publicação do resultado

Não existe uma transação distribuída única entre:

```text
persistência da pré-validação
+
Azure Service Bus
```

Pode ocorrer:

```text
situação atualizada
    -> processo cai antes de publicar resultado
    -> orquestrador não recebe retomada
```

A solução deverá utilizar Outbox.

### 17.1 Operação local atômica

Na mesma operação atômica do armazenamento do `doctree`:

```text
atualizar situação do dossiê
+
registrar evento de resultado pendente
```

Exemplo lógico:

```json
{
  "eventId": "MON-6ec73bf9-b390-417c-b2c2-f34fd7f68a63:resultado:v1",
  "tipo": "RESULTADO_MONITORAMENTO_DOSSIE_MTR",
  "monitoramentoId": "MON-6ec73bf9-b390-417c-b2c2-f34fd7f68a63",
  "orquestracaoId": "ORQ-25c62d41-2305-4661-a3c8-cdbdf681c1f2",
  "situacaoMtr": "PENDENTE_INFORMACAO",
  "situacaoPreValidacao": "NAO_CONFORME",
  "statusPublicacao": "PENDENTE"
}
```

### 17.2 Publisher

```text
Outbox pendente
    -> publicar na fila de saída
    -> marcar como publicada
```

Se o envio tiver sucesso e a marcação falhar, a mesma mensagem poderá ser reenviada. Por isso:

- o `MessageId` será determinístico;
- a fila de saída terá detecção de duplicidade;
- o orquestrador será idempotente.

### 17.3 Redelivery da entrada depois da transição

```text
mensagem de entrada redeliverada
    -> situação já alterada
    -> Outbox já registrada
    -> não repetir transição
    -> garantir publicação
    -> Complete
```

---

## 18. Implementação com Quarkus 3.33 LTS e Azure SDK assíncrono

### 18.1 Plataforma

Utilizar a manutenção atual da linha LTS:

```xml
<quarkus.platform.version>3.33.3.1</quarkus.platform.version>
```

Runtime:

```text
Java 17
Quarkus 3.33 LTS
Mutiny
CDI
OpenTelemetry
SmallRye Fault Tolerance
Azure Service Bus via AMQP 1.0
```

### 18.2 Escolha do cliente

O conector AMQP genérico do Quarkus é adequado para consumo e produção convencionais.

Esta solução depende, entretanto, de capacidades específicas do Azure Service Bus:

- Peek-Lock e settlement explícito;
- mensagens agendadas;
- `SequenceNumber`;
- transações no broker;
- renovação automática de lock;
- Dead Letter com motivo;
- controle explícito de prefetch e concorrência.

Por isso, o adaptador de infraestrutura deverá utilizar:

```text
com.azure:azure-messaging-servicebus
```

O Azure SDK assíncrono utiliza AMQP 1.0 e expõe `Mono` e `Flux`. A aplicação e as portas continuarão utilizando `Uni` e `Multi`; a conversão ficará restrita ao adaptador.

### 18.3 Dependências Maven

```xml
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
            <groupId>com.azure</groupId>
            <artifactId>azure-sdk-bom</artifactId>
            <version>${azure.sdk.bom.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
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
        <artifactId>quarkus-smallrye-fault-tolerance</artifactId>
    </dependency>

    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-opentelemetry</artifactId>
    </dependency>

    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-smallrye-health</artifactId>
    </dependency>

    <dependency>
        <groupId>com.azure</groupId>
        <artifactId>azure-messaging-servicebus</artifactId>
    </dependency>

    <dependency>
        <groupId>com.azure</groupId>
        <artifactId>azure-identity</artifactId>
    </dependency>
</dependencies>
```

### 18.4 Configurações

```properties
monitoramento.mtr.service-bus.namespace=${SERVICE_BUS_NAMESPACE}
monitoramento.mtr.service-bus.fila-entrada=${SERVICE_BUS_FILA_ENTRADA}
monitoramento.mtr.service-bus.fila-saida=${SERVICE_BUS_FILA_SAIDA}

monitoramento.mtr.consumer.concorrencia-por-instancia=${MONITORAMENTO_CONCORRENCIA:4}
monitoramento.mtr.consumer.prefetch=${MONITORAMENTO_PREFETCH:0}
monitoramento.mtr.consumer.max-auto-lock-renew=${MONITORAMENTO_LOCK_RENEW:PT5M}

monitoramento.mtr.intervalos=${MONITORAMENTO_INTERVALOS}
monitoramento.mtr.maximo-tentativas=${MONITORAMENTO_MAXIMO_TENTATIVAS}
monitoramento.mtr.prazo-maximo=${MONITORAMENTO_PRAZO_MAXIMO}
```

### 18.5 Clientes

```java
@ApplicationScoped
public class ServiceBusClientProvider {

    private ServiceBusReceiverAsyncClient inputReceiver;
    private ServiceBusSenderAsyncClient inputSender;
    private ServiceBusSenderAsyncClient outputSender;

    @Inject
    ServiceBusConfiguration configuration;

    @PostConstruct
    void inicializar() {
        TokenCredential credential =
                new DefaultAzureCredentialBuilder().build();

        ServiceBusClientBuilder builder =
                new ServiceBusClientBuilder()
                        .credential(
                                configuration.fullyQualifiedNamespace(),
                                credential
                        )
                        .transportType(AmqpTransportType.AMQP);

        inputReceiver = builder.receiver()
                .queueName(configuration.inputQueue())
                .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                .disableAutoComplete()
                .prefetchCount(configuration.prefetch())
                .maxAutoLockRenewDuration(
                        configuration.maxAutoLockRenewDuration()
                )
                .buildAsyncClient();

        inputSender = builder.sender()
                .queueName(configuration.inputQueue())
                .buildAsyncClient();

        outputSender = builder.sender()
                .queueName(configuration.outputQueue())
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
        inputReceiver.close();
        inputSender.close();
        outputSender.close();
    }
}
```

Os clients deverão ser criados uma única vez por processo e reutilizados durante todo o ciclo de vida da aplicação.

### 18.6 Listener reativo paralelo

```java
@ApplicationScoped
public class MonitoramentoInputListener {

    @Inject
    ServiceBusClientProvider clients;

    @Inject
    MonitorarDossieMtrUseCase useCase;

    @Inject
    ServiceBusConfiguration configuration;

    private Disposable subscription;

    void iniciar(@Observes StartupEvent event) {
        int concorrencia = configuration.concorrenciaPorInstancia();

        subscription = clients.inputReceiver()
                .receiveMessages()

                // Mantém a demanda próxima da capacidade real do processor.
                .limitRate(concorrencia)

                // As mensagens emitidas primeiro ocupam primeiro os slots,
                // mas cada processamento evolui de forma independente.
                .flatMap(
                        this::processarMensagem,
                        concorrencia,
                        1
                )

                // Recupera o fluxo de recepção quando a conexão AMQP falhar.
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
                                "Consumer da fila de monitoramento encerrado",
                                failure
                        )
                );
    }

    private Mono<Void> processarMensagem(
            ServiceBusReceivedMessage message
    ) {
        final MonitorarDossieMtrCommand command;

        try {
            command = mapearEValidar(message);
        } catch (RuntimeException failure) {
            return clients.inputReceiver().deadLetter(
                    message,
                    new DeadLetterOptions()
                            .setDeadLetterReason("PAYLOAD_INVALIDO")
                            .setDeadLetterErrorDescription(
                                    failure.getMessage()
                            )
            );
        }

        Log.infof(
                "Mensagem recebida. messageId=%s, sequenceNumber=%d, "
                        + "enqueuedTime=%s, deliveryCount=%d",
                message.getMessageId(),
                message.getSequenceNumber(),
                message.getEnqueuedTime(),
                message.getDeliveryCount()
        );

        return Mono.fromCompletionStage(
                        useCase.executar(command)
                                .subscribeAsCompletionStage()
                )
                .flatMap(decisao ->
                        executarDecisao(message, decisao)
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

                    return clients.inputReceiver()
                            .abandon(message);
                });
    }

    void encerrar(@Observes ShutdownEvent event) {
        if (subscription != null) {
            subscription.dispose();
        }
    }
}
```

O `flatMap` com concorrência limitada permite que as primeiras mensagens emitidas pelo receiver ocupem primeiro os slots de processamento. Como os slots executam em paralelo, a ordem de conclusão não é preservada.

### 18.7 Porta de entrada

```java
public interface MonitorarDossieMtrUseCase {

    Uni<DecisaoMonitoramento> executar(
            MonitorarDossieMtrCommand command
    );
}
```

Decisões:

```java
public sealed interface DecisaoMonitoramento {

    record Reagendar(
            MonitorarDossieMtrCommand proximoComando,
            Instant executarEm
    ) implements DecisaoMonitoramento {
    }

    record ResultadoConclusivo(
            String outboxEventId
    ) implements DecisaoMonitoramento {
    }

    record Quarentena(
            String outboxEventId
    ) implements DecisaoMonitoramento {
    }

    record EncerrarIdempotente()
            implements DecisaoMonitoramento {
    }
}
```

O caso de uso não conhecerá:

- `ServiceBusReceivedMessage`;
- `SequenceNumber`;
- `Complete`;
- `Abandon`;
- AMQP;
- nome de filas.

### 18.8 Reagendamento transacional

```java
private Mono<Void> reagendarEConcluir(
        ServiceBusReceivedMessage mensagemAtual,
        ServiceBusMessage proximaMensagem,
        OffsetDateTime executarEm
) {
    ServiceBusReceiverAsyncClient receiver =
            clients.inputReceiver();

    return receiver.createTransaction()
            .flatMap(transaction ->
                    clients.inputSender()
                            .scheduleMessage(
                                    proximaMensagem,
                                    executarEm,
                                    transaction
                            )
                            .then(
                                    receiver.complete(
                                            mensagemAtual,
                                            new CompleteOptions()
                                                    .setTransactionContext(
                                                            transaction
                                                    )
                                    )
                            )
                            .then(
                                    receiver.commitTransaction(
                                            transaction
                                    )
                            )
                            .onErrorResume(failure ->
                                    receiver.rollbackTransaction(transaction)
                                            .onErrorResume(
                                                    rollbackFailure ->
                                                            Mono.empty()
                                            )
                                            .then(Mono.error(failure))
                            )
            );
}
```

A mensagem agendada deverá usar:

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

### 18.9 Resultado conclusivo

Quando houver transição conclusiva ou quarentena:

```text
caso de uso
    -> atualiza situação
    -> grava Outbox
    -> retorna ResultadoConclusivo ou Quarentena
    -> listener executa Complete na entrada
    -> publisher da Outbox envia a saída
```

A publicação da saída não deverá ocorrer antes do registro durável do resultado.

---

## 19. Organização modular no `doctree`

```text
dossieproduto
├── aplicacao
│   ├── comando
│   │   └── MonitorarDossieMtrCommand.java
│   ├── porta
│   │   ├── entrada
│   │   │   └── MonitorarDossieMtrUseCase.java
│   │   └── saida
│   │       ├── ConsultarDossiePreValidacaoPort.java
│   │       ├── ConsultarDossieProdutoMtrPort.java
│   │       ├── AtualizarSituacaoDossiePort.java
│   │       └── RegistrarResultadoMonitoramentoPort.java
│   └── servico
│       └── MonitorarDossieMtrService.java
│
├── dominio
│   ├── PoliticaMonitoramento.java
│   ├── DecisaoMonitoramento.java
│   ├── SituacaoMtr.java
│   └── ResultadoMonitoramento.java
│
└── infraestrutura
    └── mensageria
        └── servicebus
            ├── MonitoramentoInputListener.java
            ├── MonitoramentoInputScheduler.java
            ├── ResultadoOutputPublisher.java
            ├── ServiceBusClientProvider.java
            └── ServiceBusMessageMapper.java
```

A lógica de negócio ficará nas portas, serviços e domínio. O Azure Service Bus permanecerá restrito aos adaptadores de infraestrutura.

---

## 20. Configuração das filas

### 20.1 Fila de entrada

| Propriedade | Configuração |
|---|---|
| Tier | Standard ou Premium |
| Sessions | Desabilitadas |
| Receive mode | Peek-Lock |
| Duplicate detection | Habilitada |
| Partitioning | Preferencialmente desabilitado para auditoria simples de ordem |
| Lock duration | Compatível com o tempo normal de uma consulta |
| Auto lock renewal | Habilitado no client |
| Max delivery count | Configurável |
| Dead-letter on expiration | Habilitado |
| TTL | Superior ao prazo máximo de monitoramento e à margem operacional |
| Prefetch inicial | Zero |

### 20.2 Fila de saída

| Propriedade | Configuração |
|---|---|
| Tier | Standard ou Premium |
| Receive mode | Peek-Lock |
| Duplicate detection | Habilitada |
| Sessions | Opcionais; se usadas, `SessionId=orquestracaoId` |
| Max delivery count | Configurável |
| Dead-letter on expiration | Habilitado |
| TTL | Compatível com o prazo operacional do orquestrador |

### 20.3 Detecção de duplicidade

A detecção de duplicidade deverá utilizar os `MessageId` determinísticos. Ela protege contra reenvios do produtor dentro da janela configurada, mas não substitui a idempotência do consumer.

---

## 21. Segurança

Em produção, utilizar Managed Identity e Azure RBAC.

| Componente | Permissão mínima |
|---|---|
| Orquestrador | `Azure Service Bus Data Sender` na fila de entrada e `Data Receiver` na fila de saída |
| Processor do `doctree` | `Data Receiver` e `Data Sender` na fila de entrada; `Data Sender` na fila de saída |
| Publisher da Outbox | `Data Sender` na fila de saída |

Diretrizes:

- não versionar connection strings ou chaves;
- preferir `DefaultAzureCredential` em código;
- utilizar Managed Identity no ambiente Azure;
- separar permissões por componente;
- usar Private Endpoint e DNS privado quando definido pela infraestrutura;
- utilizar AMQP sobre TLS;
- reutilizar clients em vez de criar conexão por mensagem.

---

## 22. Observabilidade

### 22.1 Logs e atributos de span

Registrar:

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
pod
concorrenciaConfigurada
```

### 22.2 Spans

```text
servicebus.receive.monitoramento
prevalidacao.consultar-dossie
mtr.consultar-dossie-produto
prevalidacao.atualizar-situacao
outbox.registrar-resultado
servicebus.schedule.next-attempt
servicebus.publish.resultado
servicebus.complete
servicebus.abandon
orquestrador.resume
```

### 22.3 Métricas

- mensagens ativas na entrada;
- mensagens agendadas;
- idade da mensagem ativa mais antiga;
- mensagens na DLQ;
- concorrência ativa por pod;
- tempo de processamento;
- tempo de espera até obter um slot;
- quantidade de consultas ao MTR;
- falhas técnicas;
- redeliveries;
- expirações de lock;
- dossiês enviados para `QUARENTENA`;
- resultados pendentes na Outbox;
- atraso até o consumo pelo orquestrador.

O `SequenceNumber` e o `EnqueuedTime` deverão ser utilizados para diagnosticar se mensagens antigas estão acumulando, não para realizar seleção manual de mensagens ativas.

---

## 23. Cenários mínimos de teste

### 23.1 Paralelismo e antiguidade

1. Publicar dez mensagens em sequência.
2. Configurar concorrência igual a três.
3. Validar que as três primeiras mensagens recebidas correspondem às mais antigas disponíveis.
4. Introduzir tempos de processamento diferentes.
5. Validar que a ordem de conclusão pode ser diferente da ordem de leitura.

### 23.2 Múltiplos pods

1. Executar duas ou mais réplicas.
2. Manter `PrefetchCount=0`.
3. Validar distribuição das mensagens entre as instâncias.
4. Confirmar que uma mensagem não é entregue simultaneamente a dois consumers enquanto o lock está válido.

### 23.3 Redelivery

1. Processar uma mensagem e provocar `Abandon`.
2. Confirmar incremento do `DeliveryCount`.
3. Confirmar que outras mensagens continuam sendo processadas em paralelo.
4. Confirmar idempotência quando a mensagem voltar.

### 23.4 Intervalo progressivo

1. Retornar situação não conclusiva.
2. Confirmar criação da próxima mensagem agendada.
3. Confirmar `Complete` da mensagem atual na mesma transação.
4. Confirmar novo `SequenceNumber` quando a mensagem se tornar ativa.

### 23.5 Resultado conclusivo

1. Retornar `CONFORME`, `NAO_CONFORME` e `PENDENTE_INFORMACAO`.
2. Validar transição da pré-validação.
3. Validar registro da Outbox.
4. Validar publicação na fila de saída.
5. Validar retomada correta do orquestrador.

### 23.6 Quarentena

1. Atingir o máximo de tentativas.
2. Atingir o prazo máximo em outro cenário.
3. Confirmar transição para `QUARENTENA`.
4. Confirmar motivo correto no resultado.
5. Confirmar suspensão do fluxo no orquestrador.

### 23.7 Falhas de infraestrutura

- queda do pod depois da atualização e antes do `Complete`;
- queda depois do agendamento e antes do commit;
- falha no publisher depois do envio e antes de marcar a Outbox;
- indisponibilidade temporária do MTR;
- expiração do lock;
- falha do orquestrador antes do `Complete` da saída.

---

## 24. Resultado arquitetural

A solução consolidada será:

```text
Orquestrador
    -> envia dossiê ao MTR
    -> atualiza para EM_ANALISE_ENVIO_MTR
    -> publica comando na fila de entrada

doctree
    -> consome várias mensagens em paralelo
    -> as mensagens ativas mais antigas ocupam primeiro os slots
    -> valida a situação atual
    -> consulta o MTR
    -> aplica a transição quando conclusiva
    -> registra resultado em Outbox
    -> agenda nova tentativa quando não conclusiva
    -> coloca em QUARENTENA por prazo ou tentativas

fila de saída
    -> entrega o resultado ao orquestrador

orquestrador
    -> CONFORME: finaliza
    -> PENDENTE_INFORMACAO: retoma complementação
    -> NAO_CONFORME: aplica a regra correspondente
    -> QUARENTENA: suspende e sinaliza ação humana
```

A política de ordenação será registrada assim:

> A fila de entrada utilizará competing consumers e processamento paralelo. As mensagens ativas mais antigas serão normalmente entregues antes das mensagens ativas mais novas e ocuparão primeiro os slots disponíveis. Não haverá FIFO global estrito, e a ordem de conclusão poderá ser diferente da ordem de leitura.

A política não deverá ser registrada como:

> Todas as mensagens serão processadas e concluídas exatamente na ordem global de chegada.

A garantia consolidada será:

```text
Azure Service Bus Queue
+
Peek-Lock
+
mensagens ativas mais antigas priorizadas no despacho
+
processamento paralelo com concorrência limitada
+
mensagens agendadas para intervalos progressivos
+
transações internas do Service Bus no reagendamento
+
processamento idempotente
+
Outbox para resultados
+
fila de saída consumida pelo orquestrador
+
DLQ para falhas técnicas não resolvidas
```

---

## 25. Referências oficiais

### Azure Service Bus

- [Introduction to Azure Service Bus Messaging](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-messaging-overview)
- [Service Bus queues, topics, and subscriptions](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-queues-topics-subscriptions)
- [Message sequencing and timestamps](https://learn.microsoft.com/en-us/azure/service-bus-messaging/message-sequencing)
- [Message browsing and peek](https://learn.microsoft.com/en-us/azure/service-bus-messaging/message-browsing)
- [Peek-Lock, settlement and message locks](https://learn.microsoft.com/en-us/azure/service-bus-messaging/message-transfers-locks-settlement)
- [Scheduled messages](https://learn.microsoft.com/en-us/azure/service-bus-messaging/message-sequencing#scheduled-messages)
- [Service Bus transactions](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-transactions)
- [Duplicate detection](https://learn.microsoft.com/en-us/azure/service-bus-messaging/duplicate-detection)
- [Dead-letter queues](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-dead-letter-queues)
- [Prefetch messages](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-prefetch)
- [Timeouts and retries](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-timeouts-retries)
- [Competing Consumers pattern](https://learn.microsoft.com/en-us/azure/architecture/patterns/competing-consumers)
- [Java native SDK versus JMS](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-jms-versus-native-sdk)
- [ServiceBusReceiverAsyncClient](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusreceiverasyncclient)
- [ServiceBusSenderAsyncClient](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebussenderasyncclient)

### Quarkus

- [Quarkus 3.33 LTS](https://quarkus.io/blog/quarkus-3-33-released/)
- [Quarkus 3.33.3.1](https://quarkus.io/blog/quarkus-3-33-3-1-released/)
- [Quarkus Messaging 3.33](https://quarkus.io/version/3.33/guides/messaging/)
- [Vert.x and Mutiny in Quarkus 3.33](https://quarkus.io/version/3.33/guides/vertx/)
