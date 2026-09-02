# ADR-0009: Azure SDK na borda Service Bus do dossiê

- **Status:** Aceito
- **Emenda aceita em 2026-08-31:** usar AMQP 1.0 sobre WebSockets/TLS em TCP `443` por meio de
  `AmqpTransportType.AMQP_WEB_SOCKETS`, sem fallback automático para AMQP/TCP.
- **Decisão em uma frase:** manter Quarkus `3.33` LTS e usar o Azure SDK Java com Microsoft Entra
  ID na futura borda Azure Service Bus do dossiê, sem SmallRye AMQP nessa integração.
- **Quando consultar:** propostas de Azure Service Bus, AMQP, autenticação Entra ID, clientes de
  mensageria do package `br.gov.caixa.simtr.dossie`, mensagens agendadas, settlement ou transações
  do broker.

## Contexto

O guia planejado precisa orientar o package `br.gov.caixa.simtr.dossie` a publicar na fila de
entrada e consumir continuamente a fila de saída do Azure Service Bus. A plataforma deve
permanecer na linha Quarkus `3.33` LTS e a autenticação escolhida é Microsoft Entra ID,
preferencialmente por identidade gerenciada no runtime Azure.

O Quarkus `3.33.2.1` usado no projeto gerencia SmallRye Reactive Messaging `4.33.0`. Essa versão
não oferece o CBS `put-token` necessário para apresentar uma receita SmallRye AMQP autenticada no
Azure Service Bus. O suporte aparece no SmallRye `4.36.0` como experimental; introduzi-lo por
override isolado quebraria o alinhamento do BOM e migrar o Quarkus violaria a restrição de
plataforma.

O Azure SDK Java oferece `TokenCredential`, clientes assíncronos, consumo contínuo, settlement,
mensagens agendadas e transações do Service Bus. Porém, o projeto ainda não contém essas
dependências ou clientes, e não houve prova com namespace, filas e identidade reais. A situação
permanece `UNVERIFIED_IN_ENVIRONMENT`.

## Decisão

Quando uma feature de implementação for autorizada:

- a borda usará `com.azure:azure-messaging-servicebus` e `com.azure:azure-identity`, gerenciados
  pelo Azure SDK BOM `1.3.8` ao lado do BOM do Quarkus, sem versões individuais. Essa é a matriz
  documental aceita; resolução efetiva, compilação e testes continuam como gates;
- o projeto permanecerá no Quarkus `3.33` LTS e não adicionará
  `quarkus-messaging-amqp`, canais `mp.messaging.*`, `@Incoming`, `@Outgoing` ou override isolado
  do SmallRye para essa borda;
- o JSON básico terá somente `idDossiePreValidacao` e `idDossieMtr`, ambos `String`, em DTOs
  distintos de entrada e saída. Namespace e filas usarão as propriedades externas
  `simtr-hub.dossie.service-bus.namespace`, `simtr-hub.dossie.service-bus.fila-entrada` e
  `simtr-hub.dossie.service-bus.fila-saida`;
- em produção Azure, o `ServiceBusClientBuilder` receberá `ManagedIdentityCredential` de uma
  identidade gerenciada atribuída pelo sistema. `DefaultAzureCredential` ficará restrito a
  profiles locais/de desenvolvimento, sem credencial de desenvolvimento como fallback produtivo;
- o builder compartilhado usará `AmqpTransportType.AMQP_WEB_SOCKETS`, com AMQP 1.0 sobre
  WebSockets/TLS em TCP `443`. Não haverá requisito de `5671`, fallback automático para AMQP/TCP
  ou proxy presumido;
- a aplicação manterá clientes de longa duração com ciclo de vida, inicialização e shutdown
  explícitos. O envio para a fila de entrada usará `ServiceBusSenderAsyncClient`; o consumo
  contínuo da saída usará `ServiceBusProcessorClient`, com recuperação de falhas transitórias;
- o listener da saída usará `PEEK_LOCK`, auto-complete desabilitado e `Complete` somente depois do
  sucesso. Falha transitória produzirá `Abandon`; contrato permanentemente inválido poderá usar
  `DeadLetter`; `Defer` não fará parte do exemplo básico;
- o exemplo começará com `maxConcurrentCalls=1`, externalizado para ajuste posterior. Liveness
  não dependerá do broker; readiness representará clientes e subscription inicializados, sem
  round-trip sintético. O guia não criará spans ou métricas e não registrará payload, token,
  identidade ou namespace em log;
- DTOs, serialização e mapeamento pertencerão à borda de mensageria e não reutilizarão contratos
  REST, MTR ou simulador, conforme o ADR-0004;
- a identidade do recorte básico terá Data Sender na fila de entrada e Data Receiver na fila de
  saída, no menor escopo suportado. Um futuro processor de retry usará identidade separada, com
  Data Receiver e Data Sender somente na fila de entrada;
- no retry funcional futuro, `tentativaAtual` será uma application property inteira iniciada em
  `1`. Somente consulta funcional concluída sem resultado conclusivo incrementará o contador;
  falha técnica fará `Abandon` sem alterá-lo. `DeliveryCount` e `MaxDeliveryCount` permanecerão
  exclusivamente técnicos, e o esgotamento funcional produzirá `QUARENTENA` explícita;
- os intervalos entre validações formarão uma lista externa, crescente e versionada. Para cada
  dossiê, `limiteEm = enviadoAoMtrEm + prazoMaximoValidacao` e
  `proximaExecucao = min(agora + intervalo, limiteEm)`. Retentativas preservarão o mesmo limite e
  mudanças de configuração não alterarão políticas já iniciadas; ao atingir o prazo, não haverá
  nova consulta funcional e o monitoramento seguirá para `QUARENTENA`;
- no retry funcional futuro, `agendar próxima + Complete atual` poderá usar uma transação de
  entidade única na mesma fila, com sender e receiver derivados do mesmo
  `ServiceBusClientBuilder`, sem `enableCrossEntityTransactions()`. Esse processor usará
  `ServiceBusReceiverAsyncClient` com `ServiceBusSenderAsyncClient` para compor a operação;
- a transação do Service Bus ficará limitada às operações do broker. Banco de dados, estado do
  workflow e chamadas MTR não serão tratados como participantes atômicos; idempotência e eventual
  Outbox exigirão desenho próprio;
- health, recuperação do fluxo, back-pressure, concorrência, renovação de lock, settlement e
  observabilidade serão responsabilidades explícitas da integração, não capacidades presumidas do
  Quarkus ou do SDK.

Esta decisão não implementa a borda, não provisiona Azure e não autoriza o processor de
monitoramento descrito como extensão no plano do guia.

## Consequências

- o projeto preserva a linha Quarkus exigida e usa um cliente com suporte oficial a Entra ID e às
  operações avançadas necessárias ao Service Bus;
- o CBS fica encapsulado no cliente Azure em vez de ser implementado pela aplicação;
- o adapter de mensageria dependerá diretamente do SDK do fornecedor, enquanto aplicação e
  domínio permanecerão protegidos por portas e contratos internos;
- integração Reactor/Mutiny, lifecycle CDI e health exigirão código e testes explícitos;
- AMQP sobre WebSockets concentra o transporte em TCP `443`, com maior latência inicial e pequeno
  overhead adicional de handshake em comparação com AMQP/TCP;
- os BOMs Quarkus e Azure podem introduzir conflitos em Azure Core, Reactor, Netty ou Jackson e
  deverão ser validados por dependency tree, compilação e testes;
- falhas dentro de transação, perda de lock, redelivery e retomada exigirão idempotência; a
  atomicidade do broker não resolve consistência com recursos externos;
- o consolidado arquitetural não muda enquanto não existir estado implementado.

## Gates técnicos para implementação

- o aceite arquitetural não altera a situação `UNVERIFIED_IN_ENVIRONMENT` registrada no plano;
- a futura feature executável deverá provar resolução conjunta dos BOMs com Java `25`, conexão
  AMQP sobre WebSockets/TLS em TCP `443`, autenticação Entra ID, envio, consumo contínuo e os
  cenários transacionais aplicáveis;
- a implementação deverá validar lifecycle, lock renewal, settlement, redelivery, concorrência,
  back-pressure, health, recuperação, prazos progressivos e idempotência;
- qualquer implementação exigirá branch e plano próprios, novo GO, baseline SonarQube, testes e
  checkpoints humanos correspondentes.

## Alternativas rejeitadas

- **SmallRye AMQP `4.33.0`:** não oferece CBS `put-token` para autenticação no Azure Service Bus.
- **Override isolado para SmallRye `4.36.0`:** usa recurso experimental e rompe o alinhamento da
  plataforma gerenciada pelo BOM.
- **Migrar o Quarkus para obter SmallRye mais recente:** viola a exigência de permanecer na linha
  `3.33` LTS.
- **SAS/connection string no lugar de Entra ID:** contraria a escolha humana de autenticação e
  amplia o manejo de segredos de longa duração.
- **Implementar CBS na aplicação:** transfere complexidade de protocolo, renovação e segurança
  para código próprio sem necessidade.

## Relação com decisões existentes

- ADR-0001: mantém o SDK na borda e preserva a direção das dependências;
- ADR-0003: mensageria aciona portas, não casos de uso concretos ou REST local;
- ADR-0004: contratos da borda Service Bus permanecem independentes;
- ADR-0006: configuração, settlement, health e sinais observáveis exigem mudança explícita e
  testes;
- ADR-0007: retry e retomada não autorizam repetição de operações MTR mutáveis sem idempotência.

## Referências oficiais

- [Azure SDK for Java — Service Bus](https://learn.microsoft.com/en-us/java/api/overview/azure/messaging-servicebus-readme?view=azure-java-stable);
- [Azure SDK for Java — Azure Identity](https://learn.microsoft.com/en-us/java/api/overview/azure/identity-readme?view=azure-java-stable);
- [Azure Identity for Java — cadeias de credenciais](https://learn.microsoft.com/en-us/azure/developer/java/sdk/authentication/credential-chains);
- [Azure-hosted Java apps — identidade gerenciada atribuída pelo sistema](https://learn.microsoft.com/en-us/azure/developer/java/sdk/authentication/azure-hosted-apps);
- [Azure Service Bus — autenticação e autorização](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-authentication-and-authorization);
- [Azure Service Bus — portas e AMQP sobre WebSockets](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-faq#what-ports-do-i-need-to-open-on-the-firewall);
- [Azure Service Bus — transações](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-transactions).
