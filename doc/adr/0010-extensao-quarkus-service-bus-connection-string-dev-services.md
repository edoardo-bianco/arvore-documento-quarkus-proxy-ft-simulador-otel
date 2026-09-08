# ADR-0010: extensão Quarkus para Azure Service Bus no monitoramento de dossiê

- **Status:** Aceito
- **Aceito em:** 2026-09-04, por decisão humana explícita no item 1.2 da feature.
- **Estrutura alvo:** `br.gov.caixa.simtr.orquestrador` e `br.gov.caixa.simtr.monitoramento`,
  conforme revisão humana dos packages registrada no checklist da feature. Migração do código
  anterior e implementação funcional permanecem incrementais.
- **Decisão em uma frase:** no recorte de monitoramento com duas filas, manter Quarkus
  `3.33.2.1` e JDK `25`, conforme confirmado no item E0.1, usar exclusivamente a extensão
  `io.quarkiverse.azureservices:quarkus-azure-servicebus:1.2.5`, autenticar ambientes reais por
  connection string externa e usar Dev Services quando essa configuração estiver ausente em
  dev/test.
- **Quando consultar:** implementação ou alteração do orquestrador, processor de monitoramento,
  Azure Service Bus, extensão Quarkus, autenticação SAS/connection string, Dev Services,
  transporte AMQP, settlement, agendamento, transações ou telemetria de mensageria.
- **Relação com ADR-0009:** esta decisão substitui a escolha de SDK direto e Entra ID do ADR-0009
  apenas para o recorte descrito. O ADR-0009 permanece preservado como registro histórico, com
  status `Substituído pelo ADR-0010`.

## Contexto

O novo recorte precisa receber por REST os identificadores de um dossiê, publicar uma mensagem em
uma fila de entrada, consultar a situação simulada da pré-validação e a porta local de consulta do
simtr-hub, publicar o resultado em uma fila de saída e registrar esse resultado no orquestrador.

O pedido restringe a implementação à versão mais recente da extensão Quarkus mantida para Azure
Service Bus. Em 2026-09-04, o catálogo oficial apresenta a versão `1.2.5`, com status `preview`,
construída com Quarkus `3.37.4` e Java `17`. O projeto usa Quarkus `3.33.2.1` e JDK `25`; portanto,
na proposição deste ADR, essa combinação exigia um gate de compatibilidade antes da
implementação funcional. As evidências de execução e os limites atuais ficam no checklist da feature.

A documentação da extensão oferece Dev Services: sem namespace ou connection string em dev/test,
ela inicia o emulador do Service Bus e seu SQL Server. O emulador aceita somente AMQP sobre TCP,
não persiste dados após reinício e não representa autenticação, disponibilidade ou comportamento
completo do serviço Azure. Ele não é evidência suficiente para prontidão produtiva.

O ADR-0009, aceito à época do desenho anterior e agora substituído, definia Azure SDK Java direto,
Microsoft Entra ID e AMQP sobre WebSockets/TLS em TCP `443`. Adotar extensão e connection string muda dependência pública da borda,
modelo de credencial, configuração, superfície de segredo e estratégia local. A mudança exige
decisão arquitetural, de segurança e de observabilidade explícita.

## Decisão

Fica decidido que:

- o projeto importará o BOM da família Quarkiverse Azure Services na versão `1.2.5` e dependerá de
  `io.quarkiverse.azureservices:quarkus-azure-servicebus` sem versão individual;
- nenhuma outra extensão ou cliente Service Bus será introduzido como alternativa silenciosa;
  falha da versão `1.2.5` no stack atual bloqueará o incremento para decisão humana;
- o `ServiceBusClientBuilder` produzido pela extensão será encapsulado na fábrica CDI técnica
  `arquitetura.infraestrutura.servicebus`, conforme detalhado no
  [ADR-0011](0011-composicao-local-monitoramento-e-fabrica-service-bus.md). Ela criará clientes
  assíncronos de longa duração e controlará seu shutdown; somente adapters Service Bus a acessam;
- `br.gov.caixa.simtr.orquestrador` receberá a entrada REST, publicará na fila de entrada e
  consumirá a fila de saída por listener para registrar o resultado. Esse log encerra o
  demonstrador; não representa continuidade durável de workflow;
- `br.gov.caixa.simtr.monitoramento` consumirá a fila de entrada, consultará as fontes, aplicará
  processamento e políticas, reagendará quando necessário e publicará o resultado na fila de
  saída. A política ficará em `monitoramento.dominio.politica`, sem packages intermediários;
- `br.gov.caixa.simtr.dossie` e o Hub permanecem preservados: o monitoramento acessa somente a
  porta pública de consulta do Hub por ACL própria, sem mover a mensageria para esses packages;
- os dois componentes adotarão DDD e arquitetura hexagonal pragmática com domínio, aplicação,
  portas de entrada/saída e adapters próprios. Listeners e REST traduzirão contratos para portas
  de entrada; casos de uso coordenarão domínio e portas de saída; SDK Azure, serialização e
  settlement permanecerão nas bordas. Configuração tipada e producer CDI da política pertencerão
  a `monitoramento.adaptador.configuracao`;
- Quarkus, Jakarta, MicroProfile, Mutiny, Jackson e OpenTelemetry poderão apoiar domínio e
  aplicação nos dois componentes, conforme ADR-0001. O uso de CDI, escopos e injeção no domínio é
  permitido; não se criarão abstrações apenas para remover o framework. As restrições protegem
  responsabilidades, contratos de borda e direção de dependências, sem impor domínio puro;
- o acesso ao dossiê MTR ocorrerá localmente pela porta pública
  `ConsultarDossieProduto`, por meio de adapter anticorrupção; não haverá chamada HTTP para o
  próprio Hub nem importação de Resource, DTO de borda ou caso de uso concreto;
- a pré-validação será representada por porta própria e adapter simulado determinístico, ativado
  explicitamente apenas nos profiles autorizados. Não haverá fallback produtivo para o mock;
- os contratos REST, fila de entrada e fila de saída permanecerão independentes e versionados,
  conforme aprovado no checkpoint C0.2. A
  ausência de persistência será explícita: a situação da pré-validação na saída é calculada, não
  duravelmente persistida;
- ambientes reais receberão a connection string somente por configuração externa. O valor não
  será gravado em source, arquivos, argumentos, logs, spans ou relatórios;
- Microsoft Entra ID não será usado nesta feature. A autenticação dos ambientes reais ocorrerá
  exclusivamente pela chave SAS contida na connection string externa, enquanto dev/test usarão a
  connection string gerada pelo emulador via Dev Services;
- a política SAS não usará `RootManageSharedAccessKey` nem permissão `Manage`. Como o mesmo runtime
  publica e consome nas duas filas, a credencial proposta terá somente `Send + Listen`, no menor
  escopo operacional permitido pelo provisionamento;
- produção desabilitará Dev Services e falhará na inicialização se a configuração externa estiver
  ausente. A ausência em dev/test permitirá a detecção automática do Dev Services; não será
  declarada uma property global com placeholder obrigatório que impeça essa detecção;
- as imagens do emulador e do SQL serão fixadas em versões documentadas, e a aceitação de suas
  licenças foi registrada no checkpoint C0.3 em 2026-09-04; a configuração executável continua
  condicionada ao GO da feature;
- dev/test com emulador usarão AMQP/TCP. O profile Azure usará AMQP sobre WebSockets/TLS em TCP
  `443`, sem fallback automático de transporte, até que evidência ou decisão posterior determine
  outra opção;
- os listeners usarão `PEEK_LOCK`, auto-complete desabilitado e settlement explícito. Sucesso gera
  `Complete`, falha recuperável gera `Abandon` e contrato permanentemente inválido pode ir para
  `DeadLetter`;
- o reagendamento funcional tentará a transação de entidade única `schedule + Complete` somente
  após prova com a API efetivamente resolvida e o emulador. Se a prova falhar, o plano volta para
  decisão; não haverá alegação de atomicidade por suposição;
- a telemetria aproveitará OpenTelemetry e logs JSON existentes. Antes de adicionar propagação ou
  spans manuais, testes caracterizarão o que a versão resolvida do Azure SDK já produz, evitando
  spans duplicados, conforme aprovado no checkpoint C0.4;
- logs e spans poderão correlacionar IDs técnicos e decisões, mas nunca conterão payload,
  connection string, chave SAS, namespace, documentos ou PII;
- a feature permanecerá classificada como demonstrador não produtivo enquanto não houver estado
  durável, idempotência, Outbox e integração real com a pré-validação.

## Consequências

- o desenvolvimento local passa a reproduzir filas e operações principais sem exigir um
  namespace Azure, desde que Docker e as licenças necessárias estejam disponíveis;
- a aplicação assume uma extensão `preview` e uma matriz não testada oficialmente com o stack do
  projeto; dependency tree, augmentation, JDK `25` e runtime tornam-se gates bloqueantes;
- connection string/SAS simplifica o primeiro recorte, mas reintroduz segredo de longa duração e
  substitui a autenticação sem segredo aprovada no ADR-0009;
- uma credencial `Send + Listen` reúne permissões que, em uma topologia distribuída posterior,
  devem ser separadas por identidade e papel;
- diferenças entre emulador e Azure real exigem uma validação posterior do mesmo fluxo no serviço
  gerenciado, sem reinterpretar o teste local como aprovação produtiva;
- o uso da extensão fica confinado à borda; aplicação, domínio, contratos internos e a API pública
  do Hub não passam a depender do SDK;
- o consolidado arquitetural somente será atualizado quando houver estado implementado e aceito.

## Gates para aceitação e implementação

Antes da primeira alteração executável:

1. aprovar este ADR, o ownership dos packages e a relação de substituição com o ADR-0009;
2. aprovar contrato REST e contratos v1 das filas;
3. aprovar SAS/connection string, autorização do endpoint, regras do mock e licenças;
4. aprovar spans, atributos, logs e propagação;
5. registrar GO da feature e inicializar o baseline SonarQube escolhido pelo usuário.

Em 2026-09-04, os checkpoints C0.1 a C0.4, o item E0.1, o GO e o baseline B0.2 foram
concluídos. Na mesma data, o usuário aceitou explicitamente este ADR no item 1.2 do checklist.
Com isso, este ADR passa a `Aceito` e o ADR-0009 passa a `Substituído pelo ADR-0010`; os gates
técnicos posteriores continuam obrigatórios.

Depois do GO, o primeiro incremento executável deverá provar isoladamente:

- resolução do BOM/extensão `1.2.5` com Quarkus `3.33.2.1` e JDK `25`;
- compilação, augmentation e injeção do `ServiceBusClientBuilder`;
- ausência de conflitos não explicados em Azure Core, Vert.x, Reactor, Netty, Jackson e
  OpenTelemetry;
- inicialização sem namespace real, sem expor segredo e sem downgrade ou override automático.

## Alternativas consideradas

- **Manter o ADR-0009:** preservaria Entra ID e o SDK direto, mas não atenderia à restrição atual
  de usar a extensão e seu Dev Services; a alternativa não foi escolhida para este recorte.
- **Usar a extensão com Microsoft Entra ID:** tecnicamente suportado e recomendado pela
  documentação da extensão, mas rejeitado por decisão humana para esta feature, que adotará
  connection string nos ambientes reais e emulador em dev/test.
- **Atualizar Quarkus para a versão usada pela extensão:** reduz uma dimensão do risco, mas amplia o
  escopo da feature; permanece fora do plano atual.
- **Usar uma versão anterior da extensão:** rejeitada pelo requisito de usar apenas a versão mais
  recente.
- **Fallback automático entre emulador e Azure:** rejeitado porque pode ocultar erro de
  configuração e executar contra ambiente não intencional.
- **Mock produtivo da pré-validação:** rejeitado porque produz decisão baseada em dado não
  persistido nem autoritativo.

## Relação com decisões existentes

- ADR-0001: aplica domínio, aplicação, portas e adapters a `orquestrador` e `monitoramento`,
  preserva dependências externas nas bordas e exige provas ArchUnit para os dois componentes;
- ADR-0003: o processor aciona a porta pública do Hub, sem REST local ou dependência em caso de uso
  concreto;
- ADR-0004: mantém DTOs e mappers nas respectivas bordas;
- ADR-0006: trata configuração, settlement, logs, spans e transporte como contratos observáveis;
- ADR-0007: retry funcional não autoriza repetir mutações externas sem idempotência;
- ADR-0009: substituído por este ADR após aceitação humana explícita em 2026-09-04, sem remoção do
  registro histórico.

## Referências oficiais

- [Catálogo da extensão Quarkus Azure Service Bus](https://quarkus.io/extensions/io.quarkiverse.azureservices/quarkus-azure-servicebus/);
- [Documentação da extensão e Dev Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html);
- [Matriz de compatibilidade Quarkiverse Azure Services](https://github.com/quarkiverse/quarkus-azure-services#compatibility-matrix);
- [Versões suportadas do Quarkus](https://quarkus.io/releases/);
- [Azure Service Bus Emulator — visão geral e limitações](https://learn.microsoft.com/en-us/azure/service-bus-messaging/overview-emulator);
- [Azure Service Bus — perda e duplicação de mensagens](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-message-loss-and-duplicates);
- [Quarkus OpenTelemetry tracing](https://quarkus.io/guides/opentelemetry-tracing/);
- [OpenTelemetry semantic conventions para Azure messaging](https://opentelemetry.io/docs/specs/semconv/messaging/azure-messaging/).
