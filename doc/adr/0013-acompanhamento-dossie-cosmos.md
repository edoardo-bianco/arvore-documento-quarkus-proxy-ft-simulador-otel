# ADR-0013: acompanhamento durável do fluxo de dossiê no Cosmos DB

- **Status:** Aceito; CP-COSMOS humano recebido em 2026-09-12. Implementação incremental.
- **Decisão em uma frase:** acompanhamento mantém estado por execução e histórico por
  dossiê Pré-Valida no Cosmos DB, acessado pelos componentes por portas/ACLs, com diário
  de intenção/confirmação e localização baseada em evidência do broker.
- **Quando consultar:** persistência do fluxo, ownership de acompanhamento, Cosmos,
  mensagens/estado duráveis, concorrência, confirmação distribuída ou observação de DLQ.
- **Relação:** complementa ADR-0002/0003/0006/0010/0011; não substitui as políticas ou
  responsabilidades de orquestrador e monitoramento nem generaliza infraestrutura do Hub.

## Contexto

O fluxo existente inicia por POST, publica na entrada, consulta/reagenda ou publica
resultado, consome a saída e submete log. Sua evolução exige estado durável por dossiê,
histórico das situações, mensagens enviadas, filas/DLQs e motivos, além de trace no Jaeger.
Os dois componentes produzem fatos diferentes do mesmo fluxo; colocar suas regras e
modelos de estado na fábrica técnica Service Bus confundiria ownership.

Um dossiê Pré-Valida pode ter várias execuções. Cosmos e Service Bus não compartilham
transação; uma escrita de confirmação pode falhar depois de um efeito confirmado no broker.
O acompanhamento não pode tornar essa janela um falso sucesso/falha nem provocar outro efeito.

## Decisão

### Ownership e colaboração

Criar a capacidade de negócio br.gov.caixa.simtr.acompanhamento, com portas de entrada
para registrar fatos e consultar acompanhamento, domínio próprio e porta de persistência.
Orquestrador e monitoramento usam suas portas de saída e ACLs locais separadas para essa
API pública, conforme ADR-0003. DTOs Cosmos/SDK e repositórios permanecem no adapter Cosmos
no acompanhamento. Nenhum núcleo importa SDK ou a infraestrutura Service Bus.

Acompanhamento projeta fatos; não decide elegibilidade, política, resultado funcional,
prazo, routing ou retry do monitoramento. A fábrica Service Bus permanece exclusivamente
técnica. A integração Cosmos não cria barramento/event sourcing genérico.

### Chave e dados

Container doctree, database configurável (local simtr-hub), partição pelo valor original
de idDossiePreValidacao. O snapshot por monitoramentoId preserva múltiplas execuções.
Eventos imutáveis guardam mudanças e a mensagem gerada para envio, incluindo corpo exato,
envelope selecionado e referências técnicas. O snapshot guarda dimensões fixas e resumo de
pendências (quantidade/indicador/última referência por dimensão); listas completas ficam nos
eventos. Último evento registrado e último evento aplicado são referências distintas: gravar
um evento atrasado não significa avanço do fluxo. Não existe histórico ilimitado em um item.

Dimensões independentes representam entrada, processamento, reagendamento e saída; pode
haver mensagens em filas diferentes simultaneamente. Situação observada no Pré-Valida/MTR,
situação calculada, motivo de quarentena e localização/motivo DLQ permanecem distintos.
Localização tem fonte e instante; ausência em peek não confirma remoção ou conclusão.

### Consistência e efeitos

Evento e projeção são atualizados atomicamente na mesma partição Cosmos, com idempotência
por operação/fase e ETag. Eventos atrasados ficam no histórico sem regredir projeção.
A ordem considera identidade, tentativa e fase por dimensão, não só timestamp.

Intenção durável é pré-condição do efeito protegido; confirmação é registrada depois do
ACK/commit. Falha após ACK preserva o efeito confirmado e deixa confirmação pendente no
diário. Preserva também o retorno funcional: POST continua 202 após ACK; publicação de saída
não provoca Abandon por falha posterior do Cosmos; Complete/commit conhecido permanece sucesso.
Registrar diagnóstico seguro sem alegar confirmação persistida. Não dispara segundo envio ou
settlement. Falha de intenção anterior ao efeito pode propagar e impede o efeito protegido.
Não há dispatcher de replay, transação
Cosmos+Service Bus ou promessa exactly-once. Confirmação perdida pode exigir reconciliação
operacional; não é reconstruída a partir da ausência da mensagem.

### Runtime, ambiente e segurança

Usar quarkus-azure-cosmos alinhado ao BOM Azure 1.2.5, sujeito a gate no stack existente.
A versão oferece CosmosClient síncrono: adapter executa I/O no worker, com cliente CDI durável,
sem bloquear event loop e sem presumir cancelamento do I/O em voo. O gate confirma quem possui
e fecha o cliente no ciclo da extensão. O adapter fecha somente recursos que criar e possuir;
não fecha arbitrariamente o cliente injetado compartilhado.

Local usa Dev Services e bootstrap idempotente do database/container. Emulador descartável
não equivale à durabilidade do ambiente DES. Perfil remoto configura endpoint/database/chave
externos, desabilita Dev Services e rejeita bypass TLS local. Troca local→DES em novo processo.
Não provisionar conta/recursos Azure automaticamente nem expor novo endpoint REST.

O corpo persistido atende ao requisito de guardar a mensagem. Isso não autoriza corpo,
novos IDs de negócio, credencial, lock token, baggage ou Throwable nos spans/logs novos.
TraceId/spanId persistidos são a referência comum entre operação no Jaeger, log e evento
no Cosmos; parentage continua vindo do carrier W3C. O registro conserva o contexto da
operação acompanhada; spans CLIENT da persistência são seus filhos. Consultar pelo par
localiza detalhes e permite retornar ao trace sem exportar a chave de negócio nos spans.
A mesma convenção vale em todas as etapas, inclusive observação posterior de DLQ.

## Consequências e verificações

- Nova capacidade e ACLs exigem guardrails de direção de dependência e contratos próprios.
- Cosmos passa a integrar o caminho dos efeitos protegidos; falhas devem ser ensaiadas
  antes/depois de gravação, envio, commit, Complete e shutdown.
- Batch/ETag e ordenação são testados no emulador; versão preview e compatibilidade são gates.
- Observador de DLQ usa somente peek, com identidade confiável, paginação e limites explícitos.
- Prova integrada confronta documentos, mensagens e trace real Jaeger, incluindo eventos
  concorrentes/atrasados e registro final best-effort de C9.1-L.

## Alternativas consideradas

- Estado apenas em Jaeger/log: não satisfaz persistência/consulta de fluxo solicitadas.
- Estado de negócio na infraestrutura compartilhada: contraria ownership do ADR-0011.
- Um snapshot único substituído a cada POST do dossiê: perde execuções e histórico.
- Histórico crescente embutido: aumenta conflitos e risco de limite de tamanho por item.
- Outbox com reenvio automático: muda recuperação/semântica de transação; não integra esta decisão.
- CosmosAsyncClient injetado pela extensão 1.2.5: não é oferecido pelo producer inspecionado.

## Referências

- [ADR-0002](0002-limites-por-dominio-e-capacidade.md), [ADR-0003](0003-orquestracao-e-colaboracao-por-portas.md)
  e [ADR-0011](0011-composicao-local-monitoramento-e-fabrica-service-bus.md).
- [Quarkus Azure Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/index.html)
  e [Cosmos](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-cosmos.html).
- [Producer 1.2.5](https://github.com/quarkiverse/quarkus-azure-services/blob/1.2.5/services/azure-cosmos/runtime/src/main/java/io/quarkiverse/azure/cosmos/runtime/CosmosClientProducer.java).
- [Dev Services 1.2.5](https://github.com/quarkiverse/quarkus-azure-services/blob/1.2.5/services/azure-cosmos/deployment/src/main/java/io/quarkiverse/azure/cosmos/deployment/DevServicesCosmosProcessor.java).
- [Batch Cosmos](https://learn.microsoft.com/en-us/azure/cosmos-db/transactional-batch)
  e [concorrência](https://learn.microsoft.com/en-us/azure/cosmos-db/database-transactions-optimistic-concurrency).
