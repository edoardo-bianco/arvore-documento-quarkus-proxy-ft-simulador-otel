# Plano: rastreabilidade durável do fluxo de dossiê no Cosmos DB

## Estado e pedidos humanos

CP-COSMOS recebido em 2026-09-12; ADR-0013 Aceito. P1 iniciado, sem persistência operacional ainda.
Continuidade coordenada com orquestrador-monitoramento-service-bus/10.1, preservando sua
branch e todas as alterações locais. Sem commit, push ou formatos derivados.

Requisitos confirmados pelo usuário nesta sessão:

- completar o trace do POST ao log final, com Jaeger e investigação de DLQ;
- manter estado durável em banco próprio: Cosmos DB via extensão Azure do Quarkus;
- chave principal de negócio: ID do dossiê de produto do Pré-Valida, correspondente ao
  campo existente idDossiePreValidacao; container novo doctree;
- Dev Services local, com possibilidade de selecionar posteriormente Cosmos real de DES;
- situação e etapa do fluxo, histórico das mudanças e mensagem enviada à fila;
- fila de entrada/saída, reagendamento, situações de Pré-Valida/MTR, motivo da quarentena,
  DLQ de entrada/saída e motivo do broker.

Database local proposto: simtr-hub. Partition key: /idDossiePreValidacao. O usuário informou
que o container ainda não existe. Não confundir esse ID com idDossieMtr ou monitoramentoId.

## Intenção e resultado verificável

Partindo do ID do Pré-Valida, localizar as execuções, estado registrado, mensagens e
histórico, abrir o trace no Jaeger e confrontar a última localização observada no broker.
O modelo não declara estado físico atual sem fonte/data de confirmação e não transforma
ausência em peek em conclusão. Quarentena funcional e Dead Letter são dimensões distintas.

[Contrato de dados e transições aprovado](especificacao.md).
[Decisão arquitetural aceita](../../../doc/adr/0013-acompanhamento-dossie-cosmos.md).

## Escopo

1. Capacidade acompanhamento com modelos/portas próprios; orquestrador e monitoramento
   colaboram por portas consumidoras e ACLs. Regras de processamento continuam com seus donos.
2. Snapshot por execução e eventos imutáveis no container doctree, na partição do dossiê.
   Eventos incluem cópia da mensagem gerada para envio, envelope permitido e correlação.
3. Escrita atômica de evento/projeção dentro da partição Cosmos, com idempotência e ETag.
4. Intenção durável antes de efeitos de mensageria, confirmação após ACK/commit e indicação
   explícita de confirmação pendente quando a gravação posterior não for conhecida.
5. Spans/logs 10.1-B2–B5, confirmação de exportação real e navegação no Jaeger.
6. Observação não destrutiva das duas filas e DLQs, incluindo movimentos automáticos do broker.
7. Extensão quarkus-azure-cosmos alinhada ao BOM Azure 1.2.5; Dev Services local e perfil DES
   externo, sem fallback silencioso para memória/emulador.
8. Testes de concorrência, redelivery, falhas intermediárias, reinicialização do componente
   com banco ativo e confronto Cosmos/Service Bus/Jaeger.

## Fora de escopo

Criar recursos na conta Azure real nesta sessão; alterar regras funcionais, políticas,
prazos ou situações no sistema Pré-Valida/MTR; novo endpoint público; reprocessar/remover
DLQ; dispatcher/outbox de reenvio automático; garantia exactly-once entre Cosmos e Service
Bus; recuperação automática de confirmação perdida sem evidência; novos serviços Azure.
Mensagens são persistidas no Cosmos conforme pedido, mas payload e identificadores de
negócio não passam a ser exportados nos sinais novos de Jaeger/log.

## Decisões aprovadas no CP-COSMOS

- Nova capacidade acompanhamento e colaboração por portas/ACL, conforme ADR-0013.
- Partição por idDossiePreValidacao; execução por monitoramentoId; snapshots e eventos
  separados, sem histórico ilimitado em um documento e sem sobrescrever outra execução.
- Diário durável de intenção/confirmação, sem transação distribuída nem replay automático.
  Falha antes do efeito impede sua execução; falha de persistência depois do ACK preserva
  o retorno funcional confirmado (inclusive HTTP 202), sem segundo envio, Abandon ou
  settlement. O registro continua pendente de confirmação, com diagnóstico seguro.
- Persistência do corpo exato das mensagens geradas, envelope selecionado, situações e
  motivos solicitados; acesso pelos mecanismos existentes do banco, sem exposição REST nova.
- Cliente síncrono durável produzido pela extensão 1.2.5, com I/O no worker. P1 confirma
  ownership e fechamento no ciclo CDI; o adapter fecha somente recursos que criar e possuir,
  nunca arbitrariamente o cliente injetado compartilhado. Cancelamento não presume fim do I/O.
- Emulador/relaxamento de certificado confinados ao processo local. DES usa endpoint/chave
  externos e TLS válido, com Dev Services desabilitado; troca local→DES em novo processo.

As escolhas de produto acima estão confirmadas. Este checkpoint alcança os detalhes novos
arquiteturais, de falha e dados; não pede novamente a escolha de Cosmos ou do container.
Somente o usuário registra GO/aceitação do ADR.

## Ordem e incrementos

F0 consolidado tecnicamente em 2026-09-12: DevServicesTest e A1 corrigidos; 10 repetições,
13 focados e 41 integrações aprovados. Checkpoint do fingerprint atual COMPLIANT, com
1.403 testes padrão, cobertura 88,3%, duplicação 4,4% e nenhuma issue nova/grave.
Baseline original preservado. Evidência na execução 10.1-B1.5 da feature coordenada.

| Incremento | Entrega e arquivos prováveis | Aceitação/verificação |
|---|---|---|
| P0 | Este plano, especificação, ADR/índice e vínculos de retomada | Revisão e CP-COSMOS humano antes da produção afetada |
| P1 | Gate extensão/Dev Services; pom.xml, properties principal/teste, CosmosDevServicesTest e correção do harness HTTP descoberta no checkpoint (até 5) | Resolver stack efetivo; criar DB/container somente local; CRUD/partição/ETag/batch reais; TLS e lifecycle; testes padrão sem Docker |
| P1-R1 | Correção do harness terminal revelada pela regressão conjunta; MonitoramentoTerminalEmuladorTest (1 arquivo) | Peeks com observadores exclusivos; preservar consumo/Complete e asserções; 12 cenários e regressão conjunta |
| P1-R2 | Correção dos peeks externos no harness de telemetria SDK; ServiceBusTelemetriaEmuladorTest (1 arquivo) | Observação independente do cancelamento; preservar inventário/asserções; 11 cenários e regressão conjunta |
| P1-R3 | Correção do peek DLQ no harness do resultado; MonitoramentoResultadoEmuladorTest (1 arquivo) | Observador DLQ exclusivo; quatro cenários e regressão conjunta |
| P2 | Modelo de acompanhamento, dimensões e redução de eventos, testes (subfatias até 5) | Várias execuções por dossiê, evento repetido/atrasado, avanço sem regressão, situações observada/calculada distintas |
| P3 | Porta de persistência, documentos/mapper e adapter Cosmos, teste (até 5 por subfatia) | Evento+snapshot atômicos, ETag, falha/conflito/cancelamento, corpo preservado, nada em logs |
| P4 | Portas/ACLs de orquestrador e monitoramento, em subfatias próprias | Sem SDK/DTO Cosmos no núcleo dos consumidores; guardrails positivos/negativos |
| P5 | POST/publicação inicial: diário da mensagem e confirmação | Preservar 202 somente após ACK; não enviar se intenção não persistiu; não reenviar após ACK confirmado |
| P6 | Entrada/processamento e B2–B3, em subfatias | Situações consultadas, decisão, publicação de resultado, Complete e trace contínuo |
| P7 | Reagendamento e B4, em subfatias | Intenção, horário e payload da próxima tentativa; confirmação somente após commit; sem replay de transação ambígua |
| P8 | Saída/registro e B5, em subfatias | Resultado, log submetido e Complete diferenciados; preservação de C9.1-L |
| P9 | Observador de filas/DLQs e reconciliação somente por evidência | Paginação/limites/data; DLQ explícita/automática; nenhuma liquidação/republicação; identidade confiável |
| P10 | Provas integradas e roteiro local/DES/Jaeger | Conferir trace real, Cosmos e broker; falhas/isolamento/reinício; checkpoint e evidência para aceite humano |

Desenho detalhado de cada subfatia antes do RED; até cinco arquivos executáveis por
incremento, incluindo testes. Executar somente o próximo item pendente aprovado.
B2–B5 não estão cancelados: serão coordenados com P6–P8 para evitar instrumentar duas vezes
as mesmas pontes. Nenhum dado Cosmos será usado para fabricar o parentage de um trace.

## Critérios de aceitação finais

- Cada execução recuperável pelo ID original do Pré-Valida e monitoramentoId, após recriar
  o componente de aplicação mantendo o banco ativo. Nenhuma dependência de mapa em memória.
- Mensagens enviadas, reagendadas e de resultado iguais às cópias persistidas; confirmação
  distingue preparação, ACK de envio, ACK transacional e commit.
- Histórico preserva transições, situações Pré-Valida/MTR, quarentena/motivo e DLQ/motivo;
  evento atrasado/repetido não regride estado nem duplica a mudança lógica.
- Entrada, saída e respectivas DLQs identificadas por mensagem. Pode haver mais de uma
  mensagem/fila simultânea; a consulta nunca força uma localização única falsa.
- Estado pendente/indeterminado explícito nas janelas Cosmos/broker; ausência em scan limitado
  não vira concluído e indisponibilidade não aciona fallback em memória.
- Trace POST→entrada→processamento/Hub→agendamento ou resultado→saída→log/settlement
  comprovado no Jaeger; logs e registros Cosmos contêm links técnicos coerentes.
- Falha de telemetria/persistência posterior a ACK não reexecuta efeito confirmado.
- Testes padrão/build, integração opt-in e Sonar conforme AGENTS.md; revisão de arquitetura,
  correção, segurança, simplicidade, desempenho e escopo. Encerramento humano separado.

## Riscos, dependências e limites conhecidos

- A extensão 1.2.5 é construída contra Quarkus 3.37.4, enquanto o projeto usa 3.33.2.1/JDK25.
  Service Bus já tem gate próprio; Cosmos exige caracterização antes de afirmar compatibilidade.
- O producer 1.2.5 oferece CosmosClient síncrono, não CosmosAsyncClient/builder CDI público.
  Não inventar API de injeção nem bloquear event loop; não trocar a extensão silenciosamente.
- Dev Services 1.2.5 usa vnext-preview, sem configuração de volume na extensão, e fecha o
  emulador que criou. Durabilidade de DES é do Cosmos real; shutdown do ambiente local pode
  descartar dados de teste. Não prometer persistência local após remoção do container.
- Dev Services ativa propriedade JVM de bypass de certificado do emulador. Perfil DES
  deve rejeitar esse bypass e exigir novo processo ao trocar de ambiente.
- Container/partition key só são criados automaticamente no local; DES valida configuração
  e recursos preexistentes. Nenhuma credencial no chat, código, relatório ou argumento.
- Contrato atual aceita qualquer ID Pré-Valida não vazio. Preservar string/zeros/caracteres;
  id físico dos documentos usa prefixo técnico/UUID, enquanto a chave de negócio fica na
  partition key. Caracterizar limites de tamanho Cosmos e falhar explicitamente antes de
  efeito, sem truncar/hash silencioso ou acrescentar validação REST sem checkpoint próprio.
- Cosmos e Service Bus não compartilham commit. Após Complete confirmado e confirmação Cosmos
  perdida, ausência na fila não permite reconstruir o fato: pode exigir evidência do trace/log
  ou decisão operacional. Não prometer reconciliação integral automática.
- Retenção não usa TTL/descarte automático por padrão. Volume/hot partition e indexação do
  payload devem ser medidos; histórico em itens separados evita documento crescente.

## Fontes oficiais e código da versão

- [Quarkus Azure Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/index.html).
- [Extensão Cosmos](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-cosmos.html).
- [Producer 1.2.5](https://github.com/quarkiverse/quarkus-azure-services/blob/1.2.5/services/azure-cosmos/runtime/src/main/java/io/quarkiverse/azure/cosmos/runtime/CosmosClientProducer.java).
- [Dev Services 1.2.5](https://github.com/quarkiverse/quarkus-azure-services/blob/1.2.5/services/azure-cosmos/deployment/src/main/java/io/quarkiverse/azure/cosmos/deployment/DevServicesCosmosProcessor.java).
- [Batch Cosmos](https://learn.microsoft.com/en-us/azure/cosmos-db/transactional-batch)
  e [concorrência otimista](https://learn.microsoft.com/en-us/azure/cosmos-db/database-transactions-optimistic-concurrency).
- [Peek](https://learn.microsoft.com/en-us/azure/service-bus-messaging/message-browsing)
  e [Dead Letter](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-dead-letter-queues).

## Histórico anterior ao GO

Somente planejamento Cosmos. A dependência, container, adapters e persistência ainda não
foram adicionados. Verificações executáveis concluídas pertencem ao complemento F0 já autorizado.
Revisão independente somente leitura concluída, sem bloqueador para CP-COSMOS. Correções:
preservação explícita do retorno após ACK, ownership do cliente como gate e projeção limitada.
O GO humano posterior está registrado abaixo; testes e revisão não substituem decisões humanas.

## GO humano — 2026-09-12

O usuário respondeu "GO para o desenho e implementação incremental" e reiterou
"GO ao ADR-0013". O desenho está aprovado; iniciar pelo P1, conforme
[recorte e verificações](execucao-p1.md). Os registros anteriores de espera ficam superados.
Nenhum encerramento/aceite final inferido. Sem commit/push.
## Refinamento humano: observabilidade homogênea — 2026-09-12

O usuário determinou spans rastreáveis no Jaeger e referência ao Cosmos para os detalhes.
[Contrato de correlação e matriz por etapa](observabilidade-homogenea.md) aplica a mesma
estratégia a P3/P5–P10: traceId/spanId ligam operação, log e evento; monitoramentoId agrupa
a execução durável e a chave Pré-Valida continua na partição. Navegação nos dois sentidos
integra o aceite. Isso não cria novos endpoints nem copia payload/IDs de negócio para spans.
P1 inclui quatro arquivos Cosmos e a correção complementar do harness HTTP descoberta no checkpoint, conforme execucao-p1.md (cinco executáveis).