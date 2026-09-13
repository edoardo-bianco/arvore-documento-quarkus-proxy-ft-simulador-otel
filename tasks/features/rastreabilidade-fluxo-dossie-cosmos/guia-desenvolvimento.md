# Guia de desenvolvimento: acompanhamento Cosmos e rastreabilidade no Jaeger

## Ponto de partida

Este guia permite continuar a implementação aprovada no ADR-0013 sem depender do chat.
O estado detalhado e as decisões humanas ficam no [checklist](todo.md) e na
[execução P1](execucao-p1.md). Retome somente o próximo incremento pendente e registre
seu recorte antes do RED; até cinco arquivos executáveis por subfatia.

Já codificado e verificado:

- Fluxo funcional Service Bus até resultado/log/Complete, reagendamento, quarentena e DLQ.
- B1: correlação POST SERVER → iniciação INTERNAL → publicação inicial PRODUCER, carrier
  W3C e log após confirmação do envio. As provas A1/A2 caracterizam as lacunas seguintes.
- Extensão Azure Cosmos 1.2.5, SDK efetivo 4.73.1, Quarkus 3.33.2.1/JDK 25.
- Dev Services Cosmos local e configuração externa DES; gate opt-in cria `simtr-hub/doctree`
  e comprova corpo exato, partição, ETag e rollback de batch no emulador.
- Isolamento das suítes padrão/integração antes da descoberta JUnit. Correções de lifecycle
  nos testes preservam as consultas e usam observadores exclusivos onde o cancelamento
  do consumo interferia no peek.

Ainda não existe gravação operacional do fluxo no Cosmos. A aplicação em dev inicia o
emulador, mas o bootstrap de database/container implementado até aqui pertence ao teste.
Não há consulta pública de acompanhamento nem rastreamento completo comprovado no Jaeger.
O próximo incremento funcional é P2, após concluir o checkpoint de P1 registrado no checklist.

## Referências que governam a implementação

Ler a [arquitetura atual](../../../doc/arquitetura-distribuida/arquitetura-ddd-integracoes-atomicas.md)
e o [índice de ADRs](../../../doc/adr/README.md), depois o
[ADR-0013 aceito](../../../doc/adr/0013-acompanhamento-dossie-cosmos.md).
O [plano](plan.md) define escopo/ordem; a [especificação](especificacao.md) define dados,
transições e consistência; a [observabilidade homogênea](observabilidade-homogenea.md)
define a ligação Jaeger/log/Cosmos e as consultas de navegação.

Para o código já instrumentado, usar o
[desenho B1](../orquestrador-monitoramento-service-bus/desenho-10-1-b1.md).
A continuidade da entrada está no
[desenho B2](../orquestrador-monitoramento-service-bus/desenho-10-1-b2.md).
Não atualizar expectativas de A2 para esconder ausência de spans: mudar o inventário
somente junto à instrumentação correspondente e com prova de parentage.

## Ambiente e comandos

Executar na raiz, com JDK 25, Maven e Docker para integrações. A sessão local de testes
não deve conter endpoint/chave Cosmos nem connection string/namespace Service Bus externos;
os perfis recusam sua presença antes do bootstrap. Não imprimir valores para conferir isso.

| Objetivo | Comando |
|---|---|
| Testes padrão sem emuladores | `mvn -q clean test` |
| Gate Cosmos local | `mvn -q -Pcosmos-integration clean test` |
| Integração Service Bus | `mvn -q -Pservicebus-integration clean test` |
| Integração conjunta | `mvn -q -Pazure-integration clean test` |
| Uma classe Cosmos | `mvn -q -Pcosmos-integration "-Dtest=CosmosDevServicesTest" test` |
| Uma classe Service Bus | `mvn -q -Pservicebus-integration "-Dtest=MonitoramentoTerminalEmuladorTest" test` |
| Checkpoint do incremento | `./validar-checkpoint-sonarqube.ps1` |

Os comandos que usam `clean` removem relatórios anteriores de `target`; conservar antes
um resumo sanitizado nas tasks e as evidências locais necessárias. Não rodar Maven em paralelo
na mesma árvore. Tags sozinhas não impedem Dev Services da API antiga de iniciar durante
a descoberta: usar os perfis Maven, que também controlam as classes excluídas.
`-Dtest` substitui includes/excludes do Surefire; combiná-lo com o perfil correto.

Para executar a aplicação com os emuladores e exportação ao Jaeger já disponível em
`http://localhost:16686` (OTLP gRPC em `localhost:4317`):

```powershell
mvn quarkus:dev "-Ddebug=false" "-Dquarkus.profile=dev,jaeger"
```

Se não houver Jaeger local, este exemplo inicia o all-in-one v1 em outro terminal,
com as portas necessárias limitadas ao computador local:

```powershell
docker run --rm --name simtr-jaeger -p 127.0.0.1:16686:16686 -p 127.0.0.1:4317:4317 -p 127.0.0.1:4318:4318 jaegertracing/all-in-one:1.76.0
```

O exemplo segue a [distribuição oficial v1](https://www.jaegertracing.io/docs/1.76/getting-started/),
com armazenamento em memória para teste. Nesta sessão já havia um container `jaeger`
ativo da imagem `jaegertracing/all-in-one:latest`; não iniciar outro nas mesmas portas.
A imagem fixada do exemplo não foi executada nesta validação. A evolução do backend para
outra linha de versão deve ser validada no recorte correspondente. Consultar os logs da
aplicação em `target/logs/simtr-hub.json`; o aceite no Jaeger trata dos traces exportados.
Os listeners permanecem desligados por padrão. Para a prova local do fluxo funcional,
acrescentar as duas opções existentes ao comando, quando o cenário exigir consumo:

```text
-Dmonitoramento.service-bus.entrada.consumo-habilitado=true
-Dmonitoramento.service-bus.saida.consumo-habilitado=true
```

Usar o simulador/configuração de Pré-Valida e Hub conforme o
[guia Service Bus](../../../doc/guias/guia-service-bus-amqp-dossie.md); não assumir que
habilitar os listeners simula automaticamente todas as dependências.
A amostragem atual é integral, mas um Jaeger acessível não comprova que todos os spans
foram exportados. Não esperar eventos de negócio no Cosmos antes de P5–P8.

DES usa perfil `des`, `SIMTR_COSMOS_ENDPOINT`, `SIMTR_COSMOS_DATABASE` e, quando escolhida
chave, `QUARKUS_AZURE_COSMOS_KEY` fornecida externamente. Não combinar `dev` e `des`.
A extensão oferece autenticação padrão Azure quando não há chave; validar a opção do
ambiente no recorte de configuração. O bootstrap operacional, a validação do container
preexistente e a rejeição do bypass TLS local ainda são obrigatórios em P3 antes da ativação.
Trocar local → DES em processo novo. Não provisionar conta, gravar credenciais ou acessar
Cosmos real como parte destes testes. O emulador Dev Services é descartável.

## Identidades e limite entre dados e telemetria

| Identidade | Uso |
|---|---|
| `idDossiePreValidacao` | String original do dossiê, partition key `/idDossiePreValidacao` |
| `monitoramentoId` | Uma execução; vários POSTs do mesmo dossiê não se sobrescrevem |
| `orquestracaoId` | Correlação técnica existente |
| `messageId` / tentativa / fase | Identidade causal da mensagem e da operação |
| `traceId` / `spanId` | Referência da operação no Jaeger, log e EVENTO Cosmos |

No container `doctree`, `EXECUCAO` usa `id=execucao:<monitoramentoId>` e projeção limitada;
`EVENTO` usa identidade determinística de operação/fase e criação imutável. A confirmação
referencia a preparação que contém a mensagem exata. Não usar upsert para encobrir conteúdo
diferente com a mesma identidade. Histórico e lista completa de pendências não crescem
dentro do snapshot.

O Cosmos contém corpo/envelope permitido, situações, motivos e IDs de negócio. Os sinais
novos de log/Jaeger mantêm os campos técnicos aprovados. O evento referencia o span da
operação acompanhada; o acesso ao Cosmos recebe CLIENT filho. Domínio não importa SDK
Cosmos, Service Bus ou OpenTelemetry. MDC não é fonte de parentage.

## Ordem de implementação e prova de cada incremento

Nomes abaixo identificam pontos existentes de integração. Nomes de classes novas são
responsabilidade do recorte, respeitando os packages e portas do ADR; não criar todas
as abstrações antecipadamente.

| Incremento | Trabalho e pontos de integração | Prova necessária antes de avançar |
|---|---|---|
| P2 | `acompanhamento/dominio`: identidades, evento, dimensões/projeção e redução pura, em subfatias | Duas execuções do mesmo dossiê; evento repetido e conteúdo conflitante; evento atrasado registrado sem regressão; situações observada/calculada distintas; no-op sem saída fictícia |
| P3 | Portas próprias e adapter Cosmos: documentos/mapper, lifecycle, bootstrap local, DES, batch/ETag, leitura paginada e spans CLIENT | Atomicidade evento+snapshot, conflito concorrente, idempotência, corpo exato, falha/cancelamento, limites do serviço, consulta por trace/span e fechamento do cliente exclusivo |
| P4 | Portas de saída consumidoras em orquestrador/monitoramento e ACLs para acompanhamento | Guardrails positivos/negativos; nenhum SDK/DTO Cosmos vazando para o núcleo ou DTO entre bordas |
| P5 | `IniciarMonitoramentoUseCase`, `MonitoramentoEntradaPublisher` e mapper de entrada: intenção antes do send e confirmação depois do ACK | Mensagem persistida idêntica à enviada; falha anterior impede send; falha posterior preserva 202/ACK, sem duplicação; mesmo trace/span nos fatos |
| P6 | `MonitoramentoEntradaListener`, `ProcessarTentativaMonitoramentoUseCase`, consulta Pré-Valida/Hub, publisher de resultado e Complete; B2/B3 | Um CONSUMER por entrega, Hub no contexto causal, situações/origem, decisão e motivos, resultado e settlement separados; no-op; carrier inválido e callbacks intercalados |
| P7 | Adapter de reagendamento e mapper; B4 | Intenção da próxima mensagem, horário/seq, schedule+Complete ainda pendentes antes do commit; confirmação só após commit; rollback e resposta ambígua sem replay automático |
| P8 | `MonitoramentoResultadoListener`, `ReceberResultadoMonitoramentoUseCase`, `ResultadoMonitoramentoLogAdapter`; B5 | Leitura, log submetido e Complete distintos; preservar C9.1-L; falha posterior ao ACK não liquida novamente; trace até registro final |
| P9 | Observação própria de entrada, saída e suas DLQs | Peek paginado/limitado, fonte/data, DLQ explícita e automática, motivo do broker, mensagem sem identidade confiável e varredura parcial; nenhuma operação destrutiva |
| P10 | Provas integradas e roteiro de investigação | Confrontar broker, Cosmos e Jaeger real nos caminhos conclusivo/no-op/quarentena/reagendamento/DLQ; falhas intermediárias, reinício com banco ativo, navegação nos dois sentidos e checkpoint |

Para começar P2, delimitar primeiro a identidade de execução e o envelope de evento com
seus testes puros; em seguida a redução por dimensões. Inspecionar os modelos existentes
`TentativaMonitoramento`, `ResultadoMonitoramento`, `DecisaoProcessamento`,
`PreValidacaoConsultada` e `SituacaoDossieConsultada`. O acompanhamento registra decisões
prontas; não copia a política de monitoramento nem deduz situação observada a partir da calculada.

P3 deve conferir o producer real da extensão: em 1.2.5 o cliente síncrono tem escopo
Dependent e não possui disposer. Definir um único dono do recurso durável e o fechamento
correspondente; não fechar arbitrariamente um bean compartilhado. I/O síncrono fica em
worker, sem bloquear event loop. Cancelamento da assinatura não comprova término do I/O.
O gate P1 testou caracteres/zeros da chave e batch/ETag; concorrência do adapter, limites
de tamanho e persistência após recriar o componente com banco ativo ainda exigem provas P3.

B2–B5 devem ser implementados junto a P6–P8. Reutilizar o contexto W3C da mensagem,
começar/terminar spans na operação real e manter Scope lexical em cada callback. Não
manter span aberto durante a espera agendada, fabricar pai pelo Cosmos ou desenhar
retrospectivamente um span para um movimento automático à DLQ cujo instante é desconhecido.

## Contrato de falha que os testes precisam proteger

| Janela | Comportamento obrigatório |
|---|---|
| Cosmos falha antes do efeito protegido | Não enviar/agendar/liquidar esse efeito; propagar a falha conforme o contrato da borda |
| Broker confirma; confirmação Cosmos falha | Preservar o sucesso funcional, inclusive 202; manter intenção pendente e diagnóstico seguro; nenhum segundo envio/settlement |
| ACK de schedule/Complete transacional, commit pendente | Não afirmar agendamento definitivo nem remoção da entrada |
| Commit/settlement com resposta ambígua | Registrar confirmação pendente; não repetir efeito por dedução |
| Mesmo operationId/fase, mesmo conteúdo | Idempotência sem duplicar mudança lógica |
| Mesma identidade, conteúdo diferente | Conflito explícito, sem sobrescrever o evento |
| Evento atrasado | Conservar no histórico; avançar somente dimensões autorizadas pela causalidade |
| Peek não encontrou mensagem | Informar limite/fonte/data; não concluir que houve Complete |

Não existe transação distribuída Cosmos/Service Bus, dispatcher de outbox, replay automático
ou garantia exactly-once neste escopo. Entrada e saída podem coexistir; quarentena funcional
não implica DLQ. As duas situações externas continuam diferentes do resumo do fluxo.

## Investigação e aceite final

Partindo do Jaeger, copiar `traceId` e, se necessário, `spanId`; consultar EVENTO conforme
[SQL parametrizado e navegação](observabilidade-homogenea.md#navegação-jaeger--cosmos--jaeger).
O resultado identifica partição e execução. Ler `execucao:<monitoramentoId>` com a partition
key original e paginar o histórico daquela execução. Busca inicial por trace pode cruzar
partições e encontrar várias execuções; não escolher uma arbitrariamente.

No sentido inverso, abrir `/trace/<traceId>` na base Jaeger configurada e localizar `spanId`.
Não gravar URL de ambiente no documento. A persistência não depende de amostragem/exportação:
se o backend não tiver o trace, apresentar a indisponibilidade sem inventar evidência.

Antes de declarar concluído, provar mensagem exata, situações/motivos, localização com
fonte/data, pendências após falha e navegação real nas duas direções. Verificar também
que payload, credencial, lock token e mensagens brutas de exceção não aparecem nos sinais novos.
O aceite humano final é separado do commit de um incremento estável.

## Checkpoint, revisão e commits progressivos

Seguir o fluxo Sonar de `AGENTS.md`: conservar o baseline existente desta feature; uma
sessão nova deve conferir seu estado conforme o procedimento oficial. Não reconstruir
baseline a partir de totais deste guia. Token somente herdado pelo processo, nunca no chat
ou em arquivo. `NON_COMPLIANT` exige a decisão humana registrada pelo script.

Após cada recorte coerente: testes apropriados, revisão de correção/arquitetura/segurança,
checkpoint aplicável, atualização de execução/checklist e commit autorizado com escopo
explícito. O usuário solicitou um commit local estável deste marco; isso não registra
aceite final da feature nem autorização de push. Não versionar `.env`, `.codex/.state`,
logs brutos, `target` ou patches locais alheios ao incremento.

## Fontes oficiais da versão e comportamento

- [Quarkus Azure Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/index.html),
  [Cosmos](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-cosmos.html) e
  [Service Bus](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html).
- [Producer Cosmos 1.2.5](https://github.com/quarkiverse/quarkus-azure-services/blob/1.2.5/services/azure-cosmos/runtime/src/main/java/io/quarkiverse/azure/cosmos/runtime/CosmosClientProducer.java) e
  [Dev Services 1.2.5](https://github.com/quarkiverse/quarkus-azure-services/blob/1.2.5/services/azure-cosmos/deployment/src/main/java/io/quarkiverse/azure/cosmos/deployment/DevServicesCosmosProcessor.java).
- [Batch Cosmos](https://learn.microsoft.com/en-us/azure/cosmos-db/transactional-batch),
  [peek Service Bus](https://learn.microsoft.com/en-us/azure/service-bus-messaging/message-browsing) e
  [DLQ](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-dead-letter-queues).

Conferir cada API contra os artefatos resolvidos pelo Maven. A guia atual não substitui
a versão efetiva; nenhuma atualização de dependência está implícita na continuidade.