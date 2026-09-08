# Plano: implementar orquestração de monitoramento com duas filas do Service Bus

**Continuidade atual:** item 4.1 tecnicamente concluído, incluindo política/configuração,
contratos, mappers, logs tipados, validação confirmada de resultado e guardrails.
Checkpoint COMPLIANT; evidências completas no checklist. Nenhum avanço para 5.1,
listener, publicação, settlement ou encerramento humano da feature.

## Estado atual da continuidade

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
Branch `feature/orquestrador-monitoramento-service-bus`; commit e push autorizados pelo usuário para revisão com os desenvolvedores.
Hub, dossiê, configurações e extensão/emulador preservados. Restam 19 esqueletos inativos,
correspondentes aos incrementos futuros. O próximo item é 5.1, ainda não iniciado.
O encerramento humano da feature não foi inferido; detalhes no [checklist](todo.md).

## Histórico da retomada e revisão de packages

Esta retomada incorpora a orientação humana de 2026-09-06, seguida da autorização para prosseguir.
As raízes são
`br.gov.caixa.simtr.orquestrador` e `br.gov.caixa.simtr.monitoramento`. O segundo nome substitui
`br.gov.caixa.simtr.doctree.monitoramentomtr`; não haverá um nível `simtr` abaixo de
`monitoramento`. Os dois componentes terão domínio, aplicação, portas e adapters próprios.

O item 3.1 continua concluído e o item 4.1 continua em andamento. A política pura, seu teste e os
dois testes de infraestrutura foram migrados para `monitoramento`, preservando o GREEN.
A configuração tipada e seu producer CDI passaram pelo RED/GREEN; seleção inválida também foi
rejeitada no bootstrap real do Quarkus, antes da execução de um teste sem injeção da política.
Os contratos/mappers restantes e a ampliação do ArchUnit continuam pendentes. O endpoint REST
pertence ao item 6.1 e não está implementado.

O primeiro checkpoint da subfatia migração/configuração concluiu 682 testes e build, mas ficou
`NON_COMPLIANT` por uma issue nova CRITICAL/HIGH `java:S8911` no uso de `@Startup` no producer.
A documentação Quarkus permite esse uso e o bootstrap foi comprovado no runtime efetivo;
a divergência e as métricas estão no checklist. O usuário decidiu `ContinuarAjustes`, e a decisão
foi registrada pelo script do checkpoint. O ajuste mantém o mesmo producer: `@Startup` passa para
a classe, o construtor valida todas as definições e guarda a política selecionada, e o método
`@Produces @Singleton` sem parâmetros a fornece. O RED/GREEN comprovou validação na construção;
a injeção CDI passou e a prova negativa confirmou falha no bootstrap sem consumidor da política.

O checkpoint do ajuste CDI concluiu 683 testes e build e ficou `COMPLIANT`: 213 issues abertas,
nenhuma nova, nenhuma HIGH/BLOCKER/CRITICAL, cobertura 86,3% e duplicação 3,9%. O baseline foi
preservado e não houve supressão de regra nem aceitação excepcional. O próximo trabalho continua
nos contratos/mappers e guardrails restantes do item 4.1; não houve avanço para 5.1.

Na continuidade, request/response REST, modelos semânticos próprios e mapper do orquestrador
passaram por RED/GREEN. O checkpoint deste incremento concluiu 706 testes sem falhas, mas ficou
`NON_COMPLIANT` por uma issue nova MINOR/LOW `java:S6353` na expressão `[0-9]+` do request.
São 214 issues, 1 nova, nenhuma HIGH/BLOCKER/CRITICAL, cobertura 86,3% e duplicação 3,9%.
O usuário decidiu `ContinuarAjustes`; a decisão foi registrada e a regex foi simplificada para
o literal Java `"\\d+"`, sem flags Unicode e sem modificar testes, mensagens ou validações.
O novo checkpoint concluiu 706 testes e ficou `COMPLIANT`: 213 issues abertas, nenhuma nova,
nenhuma HIGH/BLOCKER/CRITICAL, cobertura 86,3% e duplicação 3,9%. Não há decisão Sonar pendente.
A localização `br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus` e seus limites técnicos
foram definidos pelo usuário: regras assíncronas ficam nos respectivos componentes, Quarkus
reativo é permitido no domínio e `simtr.hub` permanece como está. Esta revisão é exclusivamente
documental, sem nova execução Maven/Sonar. Em seguida, o usuário aprovou explicitamente o
ADR-0011 em 2026-09-06, incluindo colaboração por porta/ACL e detalhes da composição CDI.
O usuário também aprovou o limite positivo compatível com `Long` e deu GO para prosseguir em
2026-09-06. A subfatia executável de 4.1 aplicou esse limite por RED/GREEN no contrato REST,
preservando JSON string e zeros à esquerda: 32 testes focados e 715 testes completos aprovados.
O checkpoint ficou `NON_COMPLIANT` por uma issue nova `java:S4144` no teste; cobertura 86,3%,
duplicação 3,9% e nenhuma HIGH/BLOCKER/CRITICAL. O usuário decidiu `ContinuarAjustes`; decisão
registrada e cenários de rejeição consolidados sem perda de casos. Os 32 testes focados e 715
testes completos passaram; novo checkpoint `COMPLIANT`, com 213 issues abertas, nenhuma nova,
nenhuma HIGH/BLOCKER/CRITICAL, cobertura 86,3% e duplicação 3,9%. S4144 confirmada como `FIXED`;
não há decisão Sonar pendente. O endpoint permanece em 6.1; esta subfatia não conclui 4.1.

Na subfatia seguinte de 4.1, produtor e consumidor da fila de entrada foram implementados com
modelos e DTOs independentes, mapeamento JSON/AMQP, validação e compatibilidade entre as bordas.
O produtor monta a mensagem sem publicar; o consumidor valida envelope, tipos, schema, campos
obrigatórios, tentativa positiva, limite MTR aprovado e coerência de datas. Identificadores,
zeros à esquerda, prazo original e versão da política são preservados; campos desconhecidos e
`DeliveryCount` não alteram a tentativa funcional. Falhas de contrato não expõem payload ou causa
do parser. Não há listener, settlement, publicação ou agendamento nesta subfatia.

O usuário confirmou 161 testes focados e 779 completos aprovados. Os relatórios Surefire
preservados confirmam 779 testes, sem falhas, erros ou ignorados. O último checkpoint foi
`NON_COMPLIANT` por uma issue nova `java:S7467`, com cobertura 86,5%, duplicação 3,9% e nenhuma
HIGH/BLOCKER/CRITICAL. `ContinuarAjustes` foi registrado em `2026-09-06T20:45:43-03:00`, conforme
confirmação humana da retomada. A atualização dessas evidências em tasks ficou pendente na pausa.

Em 2026-09-07, o usuário autorizou exclusivamente a retomada de 4.1 com atualização de tasks,
troca de `catch (JsonProcessingException ignored)` por `catch (JsonProcessingException _)` na
linha 60 de `monitoramento/adaptador/entrada/servicebus/MonitoramentoEntradaServiceBusMapper.java`,
preservação do corpo e dos testes, regressão focada e novo checkpoint com o baseline existente.
DTO/mapper de reagendamento, contratos de resultado e novos guardrails ArchUnit continuam
pendentes em 4.1; esta correção não os conclui nem autoriza avanço para 5.1.

Risco encontrado antes da edição: o `session.json` atual está em
`NOT_REQUIRED_UNTIL_CODE_CHANGE`, com `baseline=null` e `lastCheckpoint=null`, divergindo do
estado confirmado pelo usuário. O hook `sonar-session-start.ps1` escreve esses campos nulos ao
iniciar a sessão. A referência original deve ser recuperada sem reinicialização; enquanto não
for recuperada, o baseline preservado e o novo checkpoint não podem ser declarados verificados.
Essa divergência não autoriza corrigir hooks, substituir a referência ou reiniciar itens.

A referência original foi recuperada dos resultados completos das ferramentas da sessão anterior:
baseline capturado em `2026-09-06T11:34:28.1558818-03:00`, análise
`f6183a72-a2ea-44bc-9374-b2b064bdad55`, 217 issues. O objeto restaurado foi comparado integralmente
ao snapshot histórico; o fingerprint executável coincidiu com o checkpoint de S7467.
Também foram recuperados o último checkpoint e a decisão `CONTINUE_ADJUSTMENTS` de
`2026-09-06T20:45:43.39731-03:00`. Cópias dos estados anterior e restaurado ficaram em
`.codex/.state/`. Nenhum baseline foi reinicializado e nenhum hook foi alterado.

O ajuste autorizado de S7467 foi aplicado em 2026-09-07: somente `ignored` foi substituído por
`_`, preservando o corpo do catch, a tradução para `ContratoMonitoramentoInvalidoException` e
todos os testes. Passaram 161 testes focados e 779 completos, sem falhas, erros ou ignorados.
O checkpoint ficou `COMPLIANT`: 213 issues, nenhuma nova, nenhuma HIGH/BLOCKER/CRITICAL,
cobertura 86,5% e duplicação 3,9%; S7467 confirmada como `CLOSED`/`FIXED`. Baseline original
integralmente preservado; decisão Sonar `NOT_REQUIRED`. O registro operacional da rejeição
continua pertencendo ao futuro listener, conforme o plano; não foi adicionado log ao mapper.
Restam em 4.1 DTO/mapper de reagendamento, contratos de resultado e novos guardrails ArchUnit.

O workspace, a branch e as alterações em andamento serão preservados. A revisão não reinicia
itens concluídos nem antecipa 5.1. A migração de Java não altera o caminho exigido pela extensão:
`src/main/azure/servicebus-emulator/config.json`.

## C4.4 — estrutura no código e guia de continuidade — 2026-09-07

**Estado:** estrutura e guia concluídos, sem lógica nova conectada. 40 arquivos de produção
(11 portas, dois records e 27 esqueletos), 190 testes focados e 859 completos aprovados;
checkpoint `COMPLIANT` em `2026-09-07T17:13:24.4462907-03:00`, cobertura 86,4%, duplicação 3,7%,
nenhuma issue nova/HIGH/BLOCKER/CRITICAL. Baseline original e arquivos anteriores preservados.
[Guia de desenvolvimento](guia-desenvolvimento.md) e [manifesto do pacote](pacote-commit.md)
prontos para discussão/revisão; nenhum commit. Evidências detalhadas no checklist.

**Direção humana:** antecipar a estrutura de componentes e classes no código para discussão com os
desenvolvedores, antes de continuar a implementação efetiva; em seguida criar um guia para
implementar e acompanhar o trabalho conforme o plano. Esta direção substitui a proposta anterior
do agente de manter as classes futuras apenas em um mapa documental.

**Ordem atual:** 4.1-E1 estrutura inativa -> 4.1-E2 guia de continuidade -> preparação do pacote de
commit para revisão -> retomada das pendências funcionais de 4.1. O item 5.1 permanece pendente.
Não refazer política, configuração, contratos REST/entrada ou logs já verificados.

**Escopo estrutural autorizado:** declarar portas, tipos semânticos mínimos e arquivos Java dos
casos de uso, adapters, DTOs/mappers pendentes e fábrica técnica nas localizações dos ADRs
0001/0003/0004/0011/0012. Os nomes concretos, responsabilidades e itens de implementação serão
inventariados no guia desta feature. Tipos de cada componente/borda continuam independentes.

As classes ainda sem implementação serão `@Vetoed`, com Javadoc que identifica a pendência,
responsabilidade, dependências previstas e item do checklist. Não terão métodos de execução,
injeção, producers, observers, anotações REST ou clientes ativos. Portas com operações declaradas
não terão implementação CDI nem retorno fictício. Tipos ainda sem campos serão identificados
como estrutura pendente; não são contratos JSON aprovados nem modelos prontos para uso.
`@Vetoed` impede instalação de beans/observers da classe, conforme
[Jakarta CDI](https://jakarta.ee/specifications/cdi/4.1/apidocs/jakarta/enterprise/inject/vetoed).
Seu uso é temporário: na fatia funcional correspondente, implementar o contrato, testar e
habilitar a classe. Não criar hierarquia de herança apenas para representar código pendente.

**Limite da antecipação:** a estrutura mostra a arquitetura já aprovada; não implementa consulta,
publicação, consumo, geração de IDs, cálculo adicional de política, reagendamento, transação,
settlement, persistência ou ação automática sobre logs. Assinaturas semânticas ainda sujeitas à
implementação não autorizam alterar JSON, validação ou a API do Hub. Código, logs e erros do Hub,
extensão Quarkus, simuladores e `src/main/azure/servicebus-emulator/config.json` são preservados.

**Divergência de sequência registrada:** o plano e o consolidado previam criar arquivos conforme
seus consumidores fossem implementados. O pedido humano antecipa somente sua representação
estrutural nesta feature; não muda ownership, dependências permitidas ou os ADRs aceitos. A fábrica
executável continua em 6.1 e os fluxos continuam nos respectivos itens 5.1–10.1.

**4.1-E1 — critérios e verificação:** arquivos no package definitivo; distinção explícita entre
implementado e pendente; compilação preservada; guardrails de núcleo/borda e isolamento entre
componentes com provas positivas/negativas; prova CDI de que os esqueletos não são beans. Criar
em grupos de até cinco arquivos de produção, verificar compilação e revisar o conjunto. Após o
incremento coerente, regressão e checkpoint Sonar com o baseline original, sem reinicialização.

**4.1-E2 — critérios e verificação:** guia navegável com diagrama, inventário de classes/portas,
responsabilidades, dependências permitidas, estado real, próximos passos por item, comandos de
verificação, tratamento de falhas e roteiro de atualização do checklist. Links locais conferidos.
O guia não declara listeners/REST/integrações prontos nem substitui evidências do `todo.md`.

**Pacote de commit:** preparar manifesto de arquivos e mensagens para revisão, incluindo o trabalho
da feature já preservado e os esqueletos/guia; separar alterações preexistentes sem relação.
Não fazer staging global, commit, push ou descarte nesta antecipação. O manifesto deve registrar
que 4.1 ainda está incompleto e que o pacote representa base e estrutura, sem fluxo funcional.

**Riscos:** confundir esqueleto com implementação (Javadoc, inventário e prova de inatividade);
congelar campos/assinaturas cedo (pendências vinculadas às fatias funcionais); afirmar proteção
ArchUnit sem exercitar dependências (fixtures positivas/negativas); incluir mudanças alheias no
commit (manifesto seletivo e revisão do diff). O guia deve apontar a evolução desses controles.

**Arquivos prováveis:** `src/main/java/br/gov/caixa/simtr/{orquestrador,monitoramento,arquitetura}`,
testes correspondentes fora do Hub, consolidado arquitetural e esta pasta de tasks.

### Complemento C4.4 — Javadoc para continuidade manual

**Estado:** concluído em 40 tipos e 11 métodos declarados. DocLint sem erros; 27 avisos
somente de construtores implícitos preservados. Conteúdo fora dos comentários comparado e
preservado; guia atualizado, sem implementação de lógica.

O usuário solicitou complementar os comentários das classes e métodos pendentes conforme Javadoc.
O recorte é exclusivamente documental: ampliar a orientação das 27 classes inativas, documentar
os métodos das 11 portas com parâmetros e retorno esperado e esclarecer os dois records de
parâmetros. Preservar assinaturas, imports, anotações, campos, corpos e todo comportamento.

Usar a sintaxe padrão do JDK 25: descrição antes dos block tags, `@param`, `@return`,
`@see` e links/código inline. Não inventar métodos, exceções contratuais ou garantias já
implementadas. Identificar item funcional, dependências, pendências e verificações esperadas.
Atualizar o guia para orientar a leitura desses comentários.

Verificação documental: conferir referências, parâmetros/retornos e sintaxe com DocLint em
diretório temporário, sem gerar HTML; comparar o conteúdo fora dos Javadocs antes/depois.
Conforme a classificação de documentação em AGENTS.md, este complemento não executa Maven,
baseline, API ou checkpoint Sonar. O checkpoint de 859 testes pertence à estrutura anterior aos
novos comentários; não declarar seu fingerprint idêntico aos fontes após a edição documental.

## Intenção

Implementar no mesmo artifact e runtime Quarkus um primeiro recorte vertical executável para
monitorar um Dossiê de Produto:

```text
POST REST
    -> br.gov.caixa.simtr.orquestrador
    -> fila de entrada
    -> br.gov.caixa.simtr.monitoramento
        -> consulta situação simulada na pré-validação
        -> consulta dossiê pela porta CDI pública do simtr-hub
        -> reagenda ou publica resultado
    -> fila de saída
    -> br.gov.caixa.simtr.orquestrador
    -> log estruturado do resultado
```

O recorte comprovará a extensão Quarkus Azure Service Bus, a topologia das duas filas, os
contratos, o processamento reativo, o settlement explícito e a correlação OpenTelemetry. Como não
haverá Cosmos DB nem outro armazenamento durável nesta feature, ela não será apresentada como
implementação produtiva completa da solução descrita no documento de origem.

## Resultado observável

- uma requisição REST válida recebe `202 Accepted` somente depois que o Service Bus confirma a
  publicação inicial;
- a mensagem é consumida da fila de entrada com `PEEK_LOCK` e auto-complete desabilitado;
- a situação da pré-validação vem de um adapter simulado e a situação do MTR vem da capacidade
  existente `ConsultarDossieProduto`, chamada localmente por porta CDI, sem HTTP para o próprio Hub;
- situação não conclusiva produz nova mensagem agendada; situação conclusiva ou limite funcional
  produz mensagem na fila de saída;
- o listener do orquestrador registra um evento estruturado e somente então conclui a mensagem;
- uma única trilha correlaciona REST, publicação, processamento, consultas, reagendamento,
  publicação da saída e consumo final, sem registrar payload ou segredo.

## Natureza e limites do recorte

Este incremento é um demonstrador funcional de integração e arquitetura. A ausência de
persistência implica:

- a consulta simulada da pré-validação não comprova integração com Cosmos DB;
- `situacaoPreValidacao` na saída representa o estado calculado pelo processor, não uma transição
  duravelmente persistida;
- o log final não equivale à retomada durável de uma orquestração;
- não existe Outbox; uma falha entre publicar a saída e concluir a entrada pode repetir a saída;
- `MessageId` determinístico e duplicate detection reduzem duplicidade, mas não substituem
  idempotência durável;
- o recorte não pode receber classificação de pronto para produção.

Uma feature posterior deverá introduzir persistência, transição da pré-validação, Outbox e estado
idempotente do orquestrador antes de promover o fluxo a produção.

## Stack e versões verificadas

| Elemento | Estado e decisão proposta |
|---|---|
| Java | manter JDK `25` e `maven.compiler.release=25` |
| Quarkus | manter nesta feature o `3.33.2.1` atual; upgrade de manutenção fica fora de escopo |
| Quarkus LTS atual | a linha recomendada continua `3.33`; a manutenção mais recente consultada é `3.33.3.2` |
| Extensão | usar exclusivamente `io.quarkiverse.azureservices:quarkus-azure-servicebus:1.2.5` |
| Status da extensão | `preview`, construída/testada pelo projeto da extensão com Quarkus `3.37.4` e Java `17` |
| Compatibilidade alvo | C1 aceito em 2026-09-05 com provas locais registradas no checklist; permanece o risco da matriz não coberta oficialmente |
| Cliente | `ServiceBusClientBuilder` produzido pela extensão; sender/receiver assíncronos de longa duração |
| Telemetria | `quarkus-opentelemetry` e logs JSON já existentes; sem novo backend de observabilidade |

A exigência “somente a última versão da extensão” significa que falha de compatibilidade bloqueia
o incremento para decisão humana. O plano não autoriza downgrade silencioso da extensão, override
de dependências ou migração do Quarkus.

Fontes oficiais consultadas em 2026-09-04:

- catálogo da extensão, versão e status:
  <https://quarkus.io/extensions/io.quarkiverse.azureservices/quarkus-azure-servicebus/>;
- documentação, autenticação, builder e Dev Services:
  <https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html>;
- matriz de compatibilidade da extensão:
  <https://github.com/quarkiverse/quarkus-azure-services#compatibility-matrix>;
- versões suportadas do Quarkus:
  <https://quarkus.io/releases/>;
- limitações do emulador:
  <https://learn.microsoft.com/en-us/azure/service-bus-messaging/overview-emulator>;
- entrega, settlement e duplicidade:
  <https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-message-loss-and-duplicates>;
- tracing no Quarkus:
  <https://quarkus.io/guides/opentelemetry-tracing/>;
- configuração tipada, grupos, listas e mapas:
  <https://quarkus.io/version/3.33/guides/config-mappings>;
- convenções OpenTelemetry para Azure Service Bus:
  <https://opentelemetry.io/docs/specs/semconv/messaging/azure-messaging/>.

## Escopo

- organizar `br.gov.caixa.simtr.orquestrador` por domínio, aplicação, portas e adapters: receber
  REST, publicar na fila de entrada e consumir a fila de saída por listener;
- organizar `br.gov.caixa.simtr.monitoramento` pela mesma arquitetura: consumir a fila de entrada,
  consultar as fontes, aplicar políticas, reagendar e publicar o resultado na fila de saída;
- migrar código e testes de `doctree.monitoramentomtr` para `monitoramento` dentro do item 4.1,
  preservando comportamento e verificações já concluídas;
- definir contratos versionados e independentes para REST, fila de entrada e fila de saída;
- criar `POST /simtr-hub/v1/monitoramentos-dossie` com request contendo
  `idDossiePreValidacao` e `idDossieMtr` e response contendo `monitoramentoId` e
  `orquestracaoId`;
- retornar `202 Accepted` após confirmação assíncrona do envio; preservar o contrato técnico de
  erro REST já existente;
- gerar `monitoramentoId`, `orquestracaoId`, tentativa inicial e limites no servidor;
- usar `ServiceBusSenderAsyncClient` na publicação e `ServiceBusReceiverAsyncClient` no consumo;
- configurar `PEEK_LOCK`, auto-complete desabilitado, prefetch inicial zero e concorrência limitada;
- aplicar `Complete`, `Abandon` e `DeadLetter` conforme sucesso, falha recuperável e contrato
  permanentemente inválido;
- reagendar tentativa não conclusiva com `scheduleMessage`; tentar transação de entidade única
  para `agendar próxima + Complete atual`, condicionada à prova no emulador e no SDK resolvido;
- consumir a fila de saída no orquestrador e registrar o resultado em log estruturado;
- acessar `ConsultarDossieProduto` por um adapter anticorrupção local, sem chamar o Resource REST;
- implementar uma porta de consulta à pré-validação e um adapter simulado determinístico;
- usar o simulador existente do dossiê do Hub quando a property já existente estiver habilitada;
- iniciar Azure Service Bus Dev Services em dev/test somente quando namespace e connection string
  não estiverem definidos;
- criar `src/main/azure/servicebus-emulator/config.json` com as duas filas;
- fixar imagens do emulador e SQL em versões verificadas pela documentação da extensão, sem tag
  `latest`;
- usar connection string real somente por configuração externa e nunca registrar seu valor;
- instrumentar spans, contexto distribuído e logs estruturados, com testes dos sinais;
- ampliar ArchUnit para proteger os novos packages, adapters e acesso à API pública do Hub;
- atualizar o consolidado arquitetural e ADRs somente conforme decisões aprovadas e estado verde.

## Fora de escopo

- alterar `br.gov.caixa.simtr.dossie`, o código do Hub, seus contratos, logs, erros ou simuladores;
- Cosmos DB, banco relacional ou qualquer persistência real da pré-validação;
- transição durável de situação, Outbox, inbox, estado durável da orquestração ou exatamente uma vez;
- retomada, finalização ou suspensão real de workflow; a saída somente produz log;
- provisionamento de namespace, filas, políticas SAS, Key Vault, rede ou infraestrutura Azure;
- conexão obrigatória a um namespace Azure real nesta feature; ela será um gate posterior quando
  credencial e ambiente forem disponibilizados por canal seguro;
- usar `quarkus-messaging-amqp`, `@Incoming`, `@Outgoing` ou `mp.messaging.*`;
- usar outra versão da extensão se `1.2.5` falhar;
- atualizar Quarkus de `3.33.2.1` para `3.33.3.2` dentro desta feature;
- build nativo, métricas, dashboards e alertas;
- criar endpoint de consulta de estado da orquestração;
- registrar payload, connection string, chave SAS, identidade, namespace, CPF, CNPJ ou dados do
  dossiê em logs ou spans;
- alterar `.ppt`, `.pptx`, `.pdf` ou `.html` derivados.

## Contratos aprovados e implementação incremental

### REST de entrada

```http
POST /simtr-hub/v1/monitoramentos-dossie
Content-Type: application/json

{
  "idDossiePreValidacao": "c5a13bd2-fc7e-45cd-9c47-8a79b5926c86",
  "idDossieMtr": "4324680"
}
```

```http
HTTP/1.1 202 Accepted

{
  "monitoramentoId": "MON-<uuid>",
  "orquestracaoId": "ORQ-<uuid>"
}
```

Regras aprovadas para o contrato REST:

- os dois campos são obrigatórios e não vazios;
- `idDossieMtr` permanece `String` no contrato externo, com valor inteiro decimal no intervalo
  `1..9223372036854775807`, preservando zeros à esquerda, antes de ser traduzido para
  `IdentificadorDossieProduto`;
- campos desconhecidos seguem a política Jackson existente e são ignorados;
- falha de validação usa o erro REST atual; falha de publicação é traduzida sem expor broker,
  namespace ou credencial;
- autenticação/autorização seguem a postura existente do serviço; nenhum papel novo será inventado
  sem decisão humana.

### Fila de entrada v1

O JSON seguirá o contrato completo do documento de origem: `schemaVersion`, `monitoramentoId`,
`orquestracaoId`, `idDossiePreValidacao`, `idDossieMtr`, `tentativaAtual`, `iniciadoEm`,
`limiteEm` e `politicaMonitoramentoVersao`.

Propriedades:

```text
MessageId     = <monitoramentoId>:tentativa:<tentativaAtual>
CorrelationId = <orquestracaoId>
Subject       = MONITORAR_DOSSIE_MTR
ContentType   = application/json
```

### Fila de saída v1

O JSON seguirá o contrato completo do documento de origem, mas nesta feature
`situacaoPreValidacao` será o estado calculado, não persistido. Essa limitação deve aparecer na
documentação do contrato e nos testes.

```text
MessageId     = <monitoramentoId>:resultado:v1
CorrelationId = <orquestracaoId>
Subject       = RESULTADO_MONITORAMENTO_DOSSIE_MTR
ContentType   = application/json
```

DTOs REST, Service Bus, MTR e simuladores não serão reutilizados entre bordas. Produtor e
consumidor da mesma fila terão mapeamento explícito e teste de compatibilidade JSON; não será criado
um DTO global genérico em `br.gov.caixa.simtr.mensageria.contrato` sem aprovação arquitetural.

## Arquitetura alvo: DDD e hexagonal nos dois componentes

A orientação é hexagonal pragmática, não estrita, conforme ADR-0001. Quarkus, Jakarta,
MicroProfile, Mutiny, Jackson e OpenTelemetry podem ser usados no domínio e na aplicação quando
cumprirem uma responsabilidade real. CDI, escopos e injeção não são proibidos nessas camadas;
não criar wrappers ou portas apenas para esconder o framework. Preservar a política pura já
implementada é uma escolha adequada a essa regra, não uma exigência de domínio sem Quarkus.

Quarkus reativo e Mutiny (`Uni` e `Multi` quando necessários) também podem apoiar domínio e
aplicação. Uma regra de negócio continua pertencendo ao seu componente mesmo quando executada
assincronamente: políticas, tentativas funcionais, prazo e processamento em `monitoramento`;
início e tratamento de resultado da orquestração em `orquestrador`.

A capacidade transversal em `br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus` é somente
técnica, para composição compartilhada dos clientes Service Bus, não um destino para todo código
assíncrono. Listeners, publishers, DTOs e mappers continuam nos adapters dos respectivos componentes.
Não criar framework assíncrono genérico, barramento de negócio ou abstrações para necessidades futuras.

`br.gov.caixa.simtr.hub` permanece como está; `hub.arquitetura` conserva as capacidades de suporte
no âmbito do Hub. Não migrar nem generalizar segurança, erros, observabilidade ou outras capacidades
do Hub nesta feature. A nova infraestrutura não importa Hub, orquestrador ou monitoramento; os
núcleos não importam a fábrica ou o SDK. Quarkus reativo no núcleo não altera essas fronteiras.

As filas delimitam a colaboração assíncrona. Cada componente possui modelos internos e casos de
uso próprios; DTOs de transporte ficam nos respectivos adapters. O recorte não cria aggregates,
repositórios ou estado durável sem necessidade funcional aprovada.

| Responsabilidade | `orquestrador` | `monitoramento` |
|---|---|---|
| Entradas | REST para iniciar; listener da fila de saída para receber resultado | Listener da fila de entrada para processar tentativa |
| Aplicação | Iniciar solicitação e registrar resultado recebido | Consultar fontes, aplicar política e coordenar resultado ou reagendamento |
| Domínio | Identificadores, solicitação e resultado da orquestração | Situações, tentativas, prazo, decisões e política de monitoramento |
| Saídas | Publicar solicitação na entrada; registrar resultado em log | Consultar pré-validação e Hub; publicar resultado na saída; reagendar na entrada |

As entidades continuam `q.prevalidacao.monitoramento-mtr.in` e
`q.prevalidacao.monitoramento-mtr.out`. A mudança de package Java não renomeia filas, paths REST,
contratos JSON, properties ou sinais observáveis aprovados.

```text
br.gov.caixa.simtr.orquestrador
|-- dominio
|   |-- modelo
|   `-- erro
|-- aplicacao
|   |-- porta.entrada
|   |-- porta.saida
|   `-- casodeuso
`-- adaptador
    |-- configuracao
    |-- entrada.rest.v1
    |-- entrada.servicebus          # listener da fila de saida
    `-- saida
        |-- servicebus             # publisher da fila de entrada
        `-- log                    # registro do resultado

br.gov.caixa.simtr.monitoramento
|-- dominio
|   |-- modelo
|   |-- erro
|   `-- politica
|-- aplicacao
|   |-- porta.entrada
|   |-- porta.saida
|   `-- casodeuso
`-- adaptador
    |-- configuracao               # configuracao tipada e producer da politica
    |-- entrada.servicebus          # listener da fila de entrada
    `-- saida
        |-- servicebus             # resultado na saida; reagendamento na entrada
        |-- simulador.prevalidacao
        `-- acl.simtrhub
```

A antecipação estrutural autorizada no C4.4 cria os diretórios e classes previstos antes da lógica.
A implementação funcional continua seguindo o checklist. A política pura tem
o destino exato `src/main/java/br/gov/caixa/simtr/monitoramento/dominio/politica/`; os testes
espelharão essa estrutura em `src/test/java/br/gov/caixa/simtr/monitoramento/`.

### Portas e direção das dependências

- REST e listeners validam seu contrato, mapeiam para tipos internos e acionam portas de entrada
  do próprio componente. Não chamam publishers ou casos de uso concretos diretamente;
- casos de uso implementam portas de entrada e dependem de domínio e portas de saída próprios;
- no orquestrador, portas de saída representam publicar a solicitação e registrar o resultado;
- no monitoramento, portas de saída representam consultar pré-validação, consultar situação do
  dossiê, publicar resultado e reagendar tentativa;
- `PoliticaMonitoramento` e `PoliticaMonitoramentoProgressiva` pertencem a
  `monitoramento.dominio.politica`. A aplicação de monitoramento injeta essa política;
  configuração tipada e producer CDI ficam em `monitoramento.adaptador.configuracao`;
- domínio não depende de aplicação ou adapters; domínio e aplicação não recebem tipos do SDK
  Azure, DTOs de transporte, connection string nem handles de settlement;
- serialização, `Complete`, `Abandon`, `DeadLetter` e transação ficam nos adapters Service Bus;
  o SDK é restrito a essas bordas e à fábrica técnica compartilhada. A aplicação expressa a
  decisão sem conhecer o mecanismo de entrega;
- DTOs de produtor e consumidor pertencem às respectivas bordas; testes de compatibilidade JSON
  protegem a comunicação sem um DTO global compartilhado.

O adapter `monitoramento.adaptador.saida.acl.simtrhub` injeta somente
`br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDossieProduto` e traduz
`DossieProdutoConsultado` para um modelo mínimo do consumidor. O processor não importa Resource,
DTO REST/MTR, caso de uso concreto ou porta de saída do domínio `dossieproduto`.

### Extensão, simuladores e composição CDI

Os adapters dos dois componentes usarão a extensão
`io.quarkiverse.azureservices:quarkus-azure-servicebus:1.2.5`, com o builder fornecido por ela.
O SDK assíncrono utilizado através desse builder é parte da integração da extensão. Não será
introduzido cliente construído por fora dela nem mecanismo alternativo de mensageria.

Uma única fábrica CDI será dona do `ServiceBusClientBuilder` injetado e criará clientes de longa
duração. Isso evita mutações concorrentes do builder por múltiplos beans e centraliza lifecycle,
transport, filas e shutdown. Sua localização foi definida como
`br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus`, acessível somente pelos adapters
Service Bus. Os detalhes de injeção e lifecycle estão aprovados no ADR-0011, sem dependência
lateral entre núcleos nem DTOs ou regras de negócio compartilhados. A fábrica ainda não foi
implementada e permanece prevista para o item 6.1.

Dev Services continuará usando o emulador com as duas filas e o `config.json` no caminho atual.
A consulta simulada da pré-validação implementará uma porta de saída de `monitoramento`. A
consulta ao Hub continuará passando por ACL e pela porta pública `ConsultarDossieProduto`, cuja
seleção MTR/simulador pertence ao Hub e usa `simtr-hub.simulador.dossie-produto.habilitado`.
Nenhum dos novos núcleos acessará diretamente fixture, adapter MTR ou adapter simulador do Hub.

### Composição aprovada e pendência contratual do item 4.1

O contrato inicial exige `limiteEm` e `politicaMonitoramentoVersao` antes da publicação pelo
orquestrador, enquanto a política pertence ao monitoramento. A obtenção desses parâmetros foi
definida sem importar sua política ou configuração de borda no núcleo do orquestrador: porta de
saída do consumidor com ACL para uma porta pública local de monitoramento, conforme ADR-0011.
Essa colaboração calcula somente parâmetros iniciais, mantendo processamento e resultado nas
duas filas. Não duplicar a regra, criar DTO compartilhado ou mudar o JSON para essa colaboração.

Detalhamento registrado no [ADR-0011 aceito](../../../doc/adr/0011-composicao-local-monitoramento-e-fabrica-service-bus.md):
porta `ObterParametrosMonitoramento` do orquestrador, ACL local para `PrepararMonitoramento` e
fábrica técnica em `br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus`. O usuário definiu
a localização e os limites técnicos, o uso permitido de Quarkus reativo no domínio e a preservação
do Hub, e aprovou integralmente o ADR-0011. Nenhum desses beans, portas ou package técnico foi
implementado nesta atualização documental; o aceite não altera a ordem dos itens do checklist.

#### Checkpoint humano C4.1 — arquitetura e limite MTR aprovados

Decisão humana registrada em 2026-09-06: “ADR-0011 aprovado”. Isso aprova a colaboração local por
porta/ACL e a composição CDI, mantendo ambas as raízes hexagonais, a localização técnica definida,
Quarkus reativo permitido no núcleo e o Hub preservado.

Em seguida, o usuário declarou “Long do contrato MTR ok, aprovado vamos prosseguir go”. A validação
pública de `idDossieMtr` deve aceitar o intervalo `1..9223372036854775807` (`Long` positivo),
preservando o JSON string e zeros à esquerda. Valores acima desse limite devem ser rejeitados
como entrada inválida antes da publicação, sem alterar o tipo do Hub.

O aceite contratual é próprio e resolve a divergência observada no modelo real do Hub. A subfatia
imediata altera somente request REST, testes focados e documentação: RED para teto excedido,
GREEN com constraint Jakarta Validation, regressão de limites inclusivos e preservação textual,
seguido do checkpoint Sonar. Contratos Service Bus e guardrails restantes continuam em 4.1;
não executar 5.1/6.1 durante sua conclusão.

### Guardrails a implementar

ArchUnit deverá incluir explicitamente os dois novos packages nas regras de aplicação, portas,
isolamento de contratos e acesso entre componentes. A importação atual de todo
`br.gov.caixa.simtr` já alcança seus arquivos, mas várias regras selecionam somente os quatro
domínios internos do Hub; a cobertura genérica não comprova essas novas fronteiras.

Os testes deverão demonstrar os caminhos permitidos e a rejeição de dependência em adapter a
partir do núcleo, DTO vazando de borda, acesso ao caso de uso concreto de outro componente e SDK
Azure fora dos adapters de integração e da fábrica técnica delimitada. Proteger também a ausência
de dependências da infraestrutura nos componentes de negócio e no Hub, sem ampliar o acesso dos
núcleos à fábrica. Incluir provas positivas de uso permitido de Quarkus reativo/Mutiny e CDI no
domínio e na aplicação, sem blacklist genérica de frameworks. Essa proteção começa no item 4.1
e acompanha as fatias que introduzem as demais portas e adapters.

## Configuração e segurança

- decisão humana registrada em 2026-09-04: usar connection string externa nos ambientes reais e
  emulador via Dev Services em dev/test; Microsoft Entra ID fica fora desta feature;
- não declarar
  `quarkus.azure.servicebus.connection-string=${QUARKUS_AZURE_SERVICEBUS_CONNECTION_STRING}` com
  variável obrigatória global: a ausência real da configuração deve permitir que Dev Services
  seja detectado;
- em ambiente real, usar a variável padrão
  `QUARKUS_AZURE_SERVICEBUS_CONNECTION_STRING`; produção deve falhar na inicialização quando ela
  estiver ausente e Dev Services deve permanecer desabilitado;
- connection string deve ser de namespace, não usar `RootManageSharedAccessKey` e possuir apenas
  `Send + Listen`, pois este monólito publica e consome nas duas filas;
- não há privilégio `Manage`; provisionamento permanece externo;
- aceitar as EULAs do emulador e do SQL é checkpoint humano obrigatório. A aceitação será explícita
  por configuração aprovada, nunca inferida pelo agente;
- o emulador suporta somente AMQP/TCP e não WebSockets. Dev/test usarão AMQP/TCP; o ambiente Azure
  manterá `AMQP_WEB_SOCKETS`/443 conforme ADR-0010;
- ativar o mock de pré-validação explicitamente. Fora de dev/test, não haverá fallback silencioso
  para dados simulados;
- selecionar a política de monitoramento por configuração tipada, com uma propriedade para a
  política ativa e um mapa de definições; o producer CDI deve falhar no bootstrap quando a política
  ativa não existir, o tipo não for suportado ou os valores forem inválidos;
- manter todos os parâmetros da política em configuração: versão, tipo, intervalos progressivos,
  máximo opcional de tentativas e duração máxima obrigatória; nenhuma implementação contém valor
  operacional compilado;
- limitar tamanho do body REST e do corpo da mensagem, validar schema/subject/content type e nunca
  desserializar conteúdo não validado diretamente para modelo interno.

### Fronteiras de confiança e ameaças principais

| Fronteira | Risco | Controle planejado |
|---|---|---|
| HTTP -> orquestrador | spoofing, payload inválido, DoS | autenticação vigente, autorização aprovada, validação e limite de tamanho |
| Service Bus -> listener | adulteração lógica, contrato incompatível | validação completa, schema version, subject/content type e DLQ |
| configuração -> SDK | vazamento ou privilégio excessivo | segredo externo, fail-fast, Send+Listen, redaction e nenhum log do valor |
| mock -> regra | dado simulado usado fora do ambiente autorizado | flag explícita, origem no span e bloqueio de fallback produtivo |
| redelivery -> efeitos | logs/saídas duplicados | MessageId determinístico, duplicate detection e limitação documentada |

### C4.2 — log de erro padronizado aprovado nas bordas Service Bus

**Estado:** GO humano explícito recebido em 2026-09-07 para o formato apresentado,
inicialmente somente em logs. Consumidor e produtor da entrada implementados e verificados;
produtor propaga RuntimeException própria conforme autorização humana. Após S1130, nove testes
focados e 825 completos passaram; checkpoint `COMPLIANT`, zero issues novas, cobertura 86,7%,
duplicação 3,8%, nenhuma HIGH/BLOCKER/CRITICAL. S110, S5961 e S1130 `CLOSED/FIXED`; baseline
original preservado. Demais bordas/guardrails permanecem pendentes em 4.1, sem avanço para 5.1.

**Intenção:** toda falha dos contratos Service Bus desta feature deve deixar um registro de erro
JSON que identifique onde ocorreu, classifique o motivo por código estável e permita correlação
para uma ação posterior. A troca para `_` permanece uma correção de análise estática; não satisfaz
sozinha este novo requisito operacional.

**Escopo considerado para esta proposta:** contratos Service Bus de monitoramento/orquestrador,
começando pelo consumidor e produtor de entrada já existentes. Aplicar o mesmo formato às bordas
de reagendamento e resultado quando forem implementadas em 4.1. Hub e REST atuais permanecem
preservados. O GO aprovou registro inicialmente somente em logs. Armazenamento durável e
processamento automático permanecem fora deste incremento.

**Perguntas que o registro deve responder:**

1. Qual componente e operação identificaram a falha?
2. Qual código estável permite classificar o erro sem interpretar texto livre?
3. Qual ocorrência e contexto de trace correspondem à falha?
4. Em erro de JSON, qual a localização segura no documento, sem copiar seu conteúdo?

**Estado confirmado e divergências:**

- `ErroPadraoDto` do REST usa `codigo_http`, `recurso`, `id_erro`, `codigo_erro`,
  `erros: [{mensagem}]`, `detalhe` e `stacktrace`, omitindo propriedades nulas.
- A configuração atual já gera JSON no console e em `target/logs/simtr-hub.json`.
  Isso não comprova que um objeto de erro esteja presente no registro.
- `GenericExceptionMapper` e `MtrRestClientExceptionMapper` registram contexto próprio;
  `ObservabilityLog` converte valores em strings. Logo, reutilizar esse helper não garante
  `erros` como array JSON. O registro REST atual não é uma cópia integral do DTO de resposta.
- Na inspeção inicial, o mapper consumidor classificava e propagava falhas sem log e o produtor
  propagava `JsonProcessingException`. O consumidor agora registra o contrato abaixo e propaga
  id/código; o produtor é a próxima subfatia. Não existem listeners funcionais.
- Não corrigir nem generalizar a observabilidade do Hub silenciosamente. Essa eventual ampliação
  exige autorização própria. O padrão de campos JSON pode ser equivalente com DTOs independentes
  nas bordas, conforme ADR-0004; isso não autoriza importar o DTO/helper do Hub nesses componentes.

**Contrato proposto para o registro:**

O registro mantém os metadados do logger e os campos de contexto usados pelo projeto. O objeto
de erro usa os nomes e tipos do REST. Exemplo ilustrativo de uma falha de desserialização:

```json
{
  "level": "ERROR",
  "evento": "monitoramento.servicebus.entrada.falhou",
  "camada": "adaptador",
  "componente": "MonitoramentoEntradaServiceBusMapper",
  "operacao": "lerContrato",
  "recurso": "q.prevalidacao.monitoramento-mtr.in",
  "id_erro": "3e59459d-aaba-4a2e-a464-2222a5f65589",
  "codigo_erro": "MONITORAMENTO_ENTRADA_JSON_INVALIDO",
  "erros": [
    {"mensagem": "JSON invalido na mensagem de entrada."}
  ],
  "detalhe": "Falha de desserializacao.",
  "linha_json": 4,
  "coluna_json": 12,
  "traceId": "0123456789abcdef0123456789abcdef",
  "spanId": "0123456789abcdef"
}
```

- `id_erro` identifica a ocorrência, inclusive quando o corpo não fornece nenhum identificador
  confiável. Propagar a identificação de forma tipada para evitar registros duplicados depois.
- `codigo_http` é omitido em AMQP; não inventar status HTTP para uma operação de fila.
  Em uma borda HTTP, continua com seu significado e tipo atuais.
- Para exceções técnicas, `stacktrace` mantém somente o nome do tipo e os frames de execução
  (classe, método, arquivo e linha), sem mensagem original, causas ou objetos do parser.
  Componente/operação identificam a etapa; linha/coluna JSON são números extraídos com segurança,
  quando disponíveis. Rejeições de validação usam código/detalhe controlados, sem inventar
  exceção técnica. O teste deve provar localização útil e ausência de payload também nesse campo.
- `traceId`/`spanId` preservam o contexto válido existente. Sem span ativo, omitir esses
  campos e manter `id_erro`; não fabricar trace nem antecipar a Task 10.
- IDs técnicos da mensagem somente entram quando validados e disponíveis. Payload inválido,
  valor rejeitado, sourceRef, mensagem original do parser, credenciais e causas não entram no log.
- `erros` deve ser um array JSON real; um JSON escapado dentro de `message` não comprova
  esse contrato. O teste precisa observar a saída do formatter usado no runtime.
- Esta proposta usa `ERROR` para as falhas classificadas no recorte, como solicitado; sucesso
  não emite evento de erro. Um registro por falha reconhecida, seguido da propagação da falha.
  O log não executa settlement, DLQ, retry, persistência nem ação automática.

| Origem | Evento proposto | Código proposto | Classificação |
|---|---|---|---|
| Consumidor, parsing/desserialização | `monitoramento.servicebus.entrada.falhou` | `MONITORAMENTO_ENTRADA_JSON_INVALIDO` | JSON não interpretável pelo contrato |
| Consumidor, propriedades AMQP | `monitoramento.servicebus.entrada.falhou` | `MONITORAMENTO_ENTRADA_ENVELOPE_INVALIDO` | Envelope ausente ou divergente |
| Consumidor, tipos/schema/campos/valores/datas | `monitoramento.servicebus.entrada.falhou` | `MONITORAMENTO_ENTRADA_CONTRATO_INVALIDO` | Contrato v1 não atendido |
| Produtor, serialização | `orquestrador.servicebus.entrada.falhou` | `ORQUESTRADOR_ENTRADA_SERIALIZACAO_FALHOU` | Falha ao montar o JSON da entrada |

Os nomes/códigos acima integram o formato apresentado e aprovado no GO de C4.2. Detalhes de
mensagens são controlados pela aplicação. A ação posterior usa código e contexto; não dependerá
de regex sobre stacktrace ou texto de exception.

**Sequência, dependências e critérios de aceitação:**

1. C4.2 aprovado pelo usuário em 2026-09-07: escopo, JSON, códigos, nível e campos permitidos;
   inicialmente somente logs. Necessidade posterior de armazenamento/processamento revisa o plano.
2. Retomar o baseline original existente, sem reinicialização. Antes de produção, provar por
   RED a saída JSON real, identificação/localização, propagação e ausência de dados protegidos.
   Não alterar formatter global, log do Hub, dependência ou package transversal sem detalhar
   a necessidade e obter o checkpoint correspondente.
3. Implementar primeiro a fatia do consumidor, com tipos próprios da borda e um registro por
   falha de parsing, envelope ou validação. Preservar aceitação/rejeição dos contratos e os
   testes existentes; não antecipar listener.
4. Verificar o consumidor, revisar as sete dimensões e executar checkpoint Sonar do incremento.
   Em seguida, dentro de 4.1, aplicar o contrato ao produtor, preservando propagação de falha
   e ausência de publicação. Reagendamento/resultado seguem suas subfatias existentes.
5. Testar evento/código, estrutura e tipos JSON, correlação disponível, restauração de contexto,
   ausência de duplicação e vazamento, sucesso sem erro, entrada inválida sem sucesso e diagnóstico
   de localização. Testes de contrato de entrada/REST e ArchUnit existentes continuam verdes.

**Arquivos prováveis:** mapper e exceção em
`monitoramento/adaptador/entrada/servicebus/`, DTO e registro de erro locais nessa borda e
testes espelhados; depois mapper/DTO/registro de erro de
`orquestrador/adaptador/saida/servicebus/` e seus testes. Manter fatias de até cinco arquivos
Java; detalhar eventual adaptação de formatter antes de implementá-la. Atualizar arquitetura
somente quando o estado implementado mudar e registrar execução em tasks.

**Verificações:** RED/GREEN dos testes de log real e regressão das bordas; suíte completa e
`./validar-checkpoint-sonarqube.ps1` após cada incremento coerente, preservando a referência
recuperada. Esta revisão de planejamento não altera código/tooling e não executa Maven/Sonar.

**Riscos/fora de escopo:** o helper atual transforma objetos em strings, portanto a emissão do
array `erros` exige prova no formatter efetivo Quarkus `3.33.2.1`; a técnica não será declarada
verificada antecipadamente. Não usar captura genérica para esconder erro inesperado. Sem alteração
global do Hub/REST, biblioteca genérica de erro, backend novo, publicação de erro em fila,
persistência, ação automática, listener, 5.1 ou artefatos derivados.

Referência da plataforma: [Quarkus 3.33 — JSON logging e MDC](https://quarkus.io/version/3.33/guides/logging/).
Versão instalada conferida: `io.quarkus:quarkus-logging-json:3.33.2.1`.

### C4.3 — adaptação técnica do formatter (proposta após a prova)

**Estado:** autorizado pelo usuário em 2026-09-07, exclusivamente para o novo desenvolvimento,
preservando código, logs e erros de `simtr.hub`. C4.2 permanece aprovado; nenhuma repetição do GO.

**Necessidade comprovada:** uma prova Java temporária executada com os binários efetivos
Quarkus `3.33.2.1` e JBoss Log Manager `3.2.1.Final` confirmou que lista e número no MDC
saem como strings. Um decorator isolado produziu `erros` como array e localização numérica,
manteve `message` como evento e omitiu `codigo_http`; seis verificações de formato passaram.
Um registro de Hub sem marcador foi delegado com saída idêntica byte a byte. A prova não
instalou handlers nem verificou lifecycle, concorrência ou o mapper.

**Proposta concreta:** [ADR-0012](../../../doc/adr/0012-campos-json-tipados-logs-service-bus.md),
aceito com essa restrição. Criar composição exclusivamente técnica em
`arquitetura.infraestrutura.observabilidade`: marcador imutável de campos JSON já sanitizados,
decorator dos formatters JSON instalados e instalação/restauração CDI. DTOs, classificação e
sanitização ficam nas bordas. O decorator modifica somente registros com o marcador específico;
os demais são delegados integralmente. Categorias do Hub são sempre excluídas, mesmo com marcador;
provar saída idêntica e preservar todos os hashes de seus fontes/testes. Sem novo destino, alteração de extensão/dependência,
reabertura de arquivo, mudança de níveis/filtros/rotação ou migração do Hub.

**Motivo do checkpoint adicional:** a integração alcança handlers compartilhados e introduz uma
capacidade transversal. Isso está condicionado explicitamente no passo 2 de C4.2 e no ADR-0011.
A resposta humana autorizou a adaptação somente para o novo desenvolvimento; esse é o aceite de
C4.3, mantendo a condição de Hub inalterado.

**Próxima sequência dentro de 4.1:** C4.3/ADR-0012 autorizado com restrição; executar RED/GREEN da
composição técnica (até cinco arquivos Java), regressão/Sonar; em seguida consumidor (mapper,
exceção, representação/registro local e testes), com seu checkpoint; depois produtor.
As demais pendências de 4.1 e o impedimento de avançar para 5.1 continuam iguais.

**Critérios de aceitação/riscos:** observar JSON real nos destinos configurados, preservar logs
existentes por comparação de saída, suportar lifecycle/handlers intermediários, não duplicar
registro nem misturar contexto concorrente, não sobrescrever metadados, não incluir payload
ou Throwable original. Apenas erros marcados pagam o parse/serialização adicional.
A prova isolada não representa implementação nem aprovação técnica do runtime.

**Arquivos prováveis:** até três classes técnicas e dois testes na primeira subfatia;
nenhuma alteração em `hub/`, `pom.xml`, configuração Azure ou emulador.
Arquitetura consolidada será atualizada após implementação; ADR e índice registram apenas proposta.

**Evidência do incremento C4.3:** composição técnica implementada em três classes e dois testes;
184 testes focados e 799 completos aprovados. Checkpoint de 2026-09-07 `NON_COMPLIANT` por
S6878/S5786 novas; cobertura 86,5%, duplicação 3,9%, nenhuma HIGH/BLOCKER/CRITICAL.
Baseline original idêntico; 493 arquivos do Hub com hashes preservados. Usuário decidiu
`ContinuarAjustes`, registrado em `2026-09-07T09:53:45.1657041-03:00`. Corrigir as duas issues
e completar as provas do console e do observer CDI, preservando os demais componentes.
Após os ajustes, prova do console real JSON/textual e observer CDI concluída: 186 focados,
801 completos e checkpoint `COMPLIANT`, zero issues novas, cobertura 86,5%, duplicação 3,9%.
C4.3 concluído. O consumidor de C4.2 também foi concluído: RED comportamental confirmado,
204 testes focados e 819 completos aprovados; checkpoint `COMPLIANT`, zero issues novas,
cobertura 86,6%, duplicação 3,8%, baseline original idêntico. Erro JSON real classificado,
id/código propagados e aceitação/rejeição dos contratos preservada. Detalhes e IDs no `todo.md`.

**Próxima fatia — produtor da entrada:** preservar a assinatura checked `JsonProcessingException`
e o JSON/AMQP de sucesso. Capturar somente falha de serialização, registrar uma ocorrência com
DTO/helper próprios e propagar subtipo local com id/código e mensagem controlada, sem causa
original. Obter o recurso da configuração de fila existente; manter trace válido e stack seguro.
Até cinco arquivos Java: mapper, exceção, DTO, registro local e teste. Provar RED no arquivo JSON
real antes de produção, sanitização de mensagem/causas/suppressed, sucesso sem erro, propagação,
isolamento do trace e compatibilidade existente; executar regressão e checkpoint antes das
demais bordas. Sem publicação, agendamento, novos destinos ou configuração do Hub.

**Resultado do produtor e ajuste proposto:** implementação e regressão aprovadas; Sonar aponta
S110 na herança da exceção Jackson e S5961 por 29 asserções em um teste. Cobertura 86,7%,
duplicação 3,8%, duas issues novas MAJOR/MEDIUM, nenhuma HIGH/BLOCKER/CRITICAL; baseline original
idêntico. Situação `NON_COMPLIANT`, sem reprovação automática; decisão humana pendente.
Proposta para `ContinuarAjustes`: derivar a exceção local de `IOException` e declarar o subtipo
em `throws` do mapper package-private, mantendo falha checked, id/código e sanitização.
Trata-se de mudança explícita do tipo técnico desse mapper novo, sem alterar JSON/AMQP ou Hub.
Separar o teste entre contrato JSON e identidade/propagação, preservando todas as asserções.
Ajustes não aplicados; repetir regressão/checkpoint somente após registrar a decisão humana.
Evidência completa e IDs no `todo.md`.

**Revisão humana posterior:** autorização explícita limitada a S5961; dividir o teste mantendo
as asserções e executar regressão/checkpoint. Usuário quer RuntimeException e esclarecimento de
S110. A proposta de IOException acima fica substituída pela proposta de exceção local derivada
diretamente de RuntimeException, removendo a declaração checked do mapper e preservando captura
do Jackson, classificação, log sanitizado e propagação de id/código. Consumidor já usa
RuntimeException; produtor ainda é checked no estado atual. Não alterar a hierarquia de produção
neste incremento de S5961. Registrar a decisão humana sobre S110 antes de implementá-la.
A divisão S5961 foi concluída e revisada: todas as 29 verificações preservadas, nove testes
focados e 825 completos aprovados. Sonar confirma S5961 `CLOSED/FIXED`; permanece S110 como
única issue nova, MAJOR/MEDIUM, cobertura 86,7%, duplicação 3,8%, nenhuma HIGH/BLOCKER/CRITICAL.
Baseline e produção preservados. Evidências completas e IDs no checklist.

**Autorização S110 com RuntimeException:** usuário decidiu “ContinuarAjustes com RuntimeException”.
Aplicar herança direta de RuntimeException à exceção local do produtor e retirar a declaração
checked da assinatura do mapper. Preservar mensagem controlada, id/código, causa nula, catch
específico do Jackson, formato/log único e JSON/AMQP de sucesso. A proposta de IOException
permanece descartada. RED exigirá propagação RuntimeException; testes de metadata Jackson
serão adaptados à ausência de causa/suppressed e de valores sensíveis na exceção local.
Na prova de propriedades AMQP existente, remover apenas o `throws Exception` do teste,
validando compilação do chamador sem obrigação checked. Escopo: quatro arquivos Java do produtor
(dois de produção e dois de teste), docs da feature e consolidado após implementação.
Regressão/revisão e checkpoint com baseline original são obrigatórios antes de concluir a correção.
Demais pendências de 4.1 permanecem; 5.1 não será iniciado.

**Resultado da correção RuntimeException:** implementação, revisão e regressão concluídas;
nove focados e 825 completos passaram. Sonar confirmou S110/S5961 `CLOSED/FIXED`, mas apontou
S1130, MINOR/LOW, no `throws Exception` residual do teste de compatibilidade entre as bordas.
Cobertura 86,7%, duplicação 3,8%, nenhuma HIGH/BLOCKER/CRITICAL; baseline original idêntico.
Proposta restrita para a próxima decisão: remover somente essa declaração de
`deveSerCompativelComContratoIndependenteDoConsumidor()`, mantendo corpo/asserções, a declaração
necessária do teste de leitura JSON e toda produção. Usuário autorizou `ContinuarAjustes`;
registrar a decisão, aplicar somente essa remoção e executar regressão/checkpoint com baseline
original. Não introduzir testes, dependências ou alterações de comportamento. Evidências no checklist.

**Resultado S1130 e estado atual:** retirada somente a declaração desnecessária, com comparação
integral do arquivo e revisão independente. Nove testes focados e 825 completos passaram;
checkpoint `COMPLIANT` em `2026-09-07T16:06:58.5792654-03:00`, zero issues novas, cobertura
86,7%, duplicação 3,8%, nenhuma HIGH/BLOCKER/CRITICAL. S110/S5961/S1130 confirmadas `CLOSED/FIXED`;
baseline integralmente idêntico. Logs da entrada em produtor/consumidor verificados; aplicar o
formato aprovado às futuras bordas próprias quando suas subfatias de 4.1 forem implementadas.
Reagendamento, resultado e novos guardrails continuam pendentes; 5.1 não iniciado. IDs no checklist.

## Telemetria

Perguntas operacionais que os sinais devem responder:

1. A requisição foi aceita pelo broker ou falhou antes da publicação?
2. Em qual tentativa e decisão cada monitoramento está, e qual dependência falhou?
3. A mensagem foi concluída, abandonada, reagendada ou enviada à DLQ, e por quê?
4. O resultado chegou ao listener do orquestrador e foi registrado?

Regras:

- nomes de spans de negócio propostos para o checkpoint C0.4:
  - `simtr-hub.api.monitoramento-dossie.iniciar` (`SERVER`);
  - `orquestrador.service.monitoramento-dossie.iniciar` (`INTERNAL`);
  - `doctree.service.prevalidacao.dossie.consultar` (`INTERNAL`);
  - `doctree.service.monitoramento-mtr.avaliar` (`INTERNAL`);
  - `orquestrador.service.monitoramento-dossie.resultado-registrar` (`INTERNAL`);
- spans de mensageria seguirão o formato `{messaging.operation.name} {fila}`:
  - `send q.prevalidacao.monitoramento-mtr.in` (`PRODUCER`);
  - `process q.prevalidacao.monitoramento-mtr.in` (`CONSUMER`);
  - `schedule q.prevalidacao.monitoramento-mtr.in` (`PRODUCER`);
  - `send q.prevalidacao.monitoramento-mtr.out` (`PRODUCER`);
  - `process q.prevalidacao.monitoramento-mtr.out` (`CONSUMER`);
  - settlement usará `complete`, `abandon` ou `dead_letter` seguido do nome da fila, com
    `SpanKind.CLIENT`;
- eventos de log estáveis serão
  `orquestrador.monitoramento-dossie.publicacao.confirmada`,
  `orquestrador.monitoramento-dossie.publicacao.falhou`,
  `doctree.monitoramento-mtr.decisao.tomada`,
  `doctree.monitoramento-mtr.processamento.falhou`,
  `doctree.monitoramento-mtr.settlement.executado`,
  `orquestrador.monitoramento-dossie.resultado.registrado` e
  `orquestrador.monitoramento-dossie.resultado.falhou`;
- campos estruturados permitidos incluem `monitoramento_id`, `orquestracao_id`, `message_id`,
  `tentativa_atual`, `delivery_count`, `sequence_number`, `decisao`, `settlement`, `error_type`,
  `camada`, `componente` e `operacao`, além da correlação de trace já fornecida pelo runtime;
- spans de envio terão `SpanKind.PRODUCER`; processamento terá `SpanKind.CONSUMER`; unidades
  internas e consultas manterão os tipos já usados no projeto;
- atributos de mensageria seguirão, quando suportados pela versão resolvida, `messaging.system`,
  `messaging.destination.name`, `messaging.operation.name`, `messaging.operation.type` e
  `error.type`;
- `monitoramentoId`, `orquestracaoId`, `messageId`, `deliveryCount`, `tentativaAtual` e
  `sequenceNumber` podem estar em logs/spans, mas nunca como labels de métrica;
- o contexto W3C será propagado nas application properties da mensagem somente se a instrumentação
  do Azure SDK resolvida não comprovar a propagação automática; não haverá spans duplicados;
- a Task 10 caracterizará primeiro nomes, atributos e propagação realmente emitidos pelo SDK. Se a
  versão resolvida usar convenção legada incompatível, a divergência voltará a checkpoint humano;
  spans manuais preencherão somente lacunas comprovadas;
- payload, connection string, namespace, documentos e PII não entram na telemetria;
- os testes devem provar nomes, atributos permitidos, ausência de campos proibidos e uma cadeia de
  trace contínua no fluxo assíncrono.

## Estratégia de processamento

1. validar envelope e JSON;
2. consultar situação simulada da pré-validação;
3. se não for `EM_ANALISE_ENVIO_MTR`, concluir como no-op e registrar a decisão;
4. validar prazo e tentativas;
5. se limite atingido, calcular `QUARENTENA` e publicar a saída;
6. caso contrário, consultar o dossiê pela porta local do Hub;
7. mapear `CONFORME`, `NAO_CONFORME` e `PENDENTE_INFORMACAO` como terminais;
8. para situação diferente, agendar a próxima tentativa e concluir a atual atomicamente quando a
   API/emulador comprovarem suporte;
9. publicar o resultado terminal e somente depois concluir a entrada;
10. no listener de saída, validar, registrar log estruturado e concluir; falha recuperável abandona,
    contrato inválido vai para DLQ.

Falha técnica não incrementa `tentativaAtual`; redelivery técnica continua representada por
`DeliveryCount`.

## Comandos previstos

Os comandos abaixo pertencem à implementação futura, depois dos checkpoints e do baseline:

```powershell
java -version
mvn -version
mvn -q dependency:tree
mvn -q -Dtest=<teste-focado> test
mvn -q test
./validar-checkpoint-sonarqube.ps1
mvn quarkus:dev -Ddebug=false
```

O baseline e os checkpoints já executados estão registrados no checklist. A revisão exclusivamente
Markdown não inspeciona `sonar/`, não solicita token e não executa Maven/Sonar. Ao retomar código,
preservar as alterações existentes e tratar a continuidade do estado Sonar conforme `AGENTS.md`,
sem retirar código para refazer um baseline anterior.

## Estratégia de testes

- teste de resolução do BOM e bootstrap CDI no Quarkus `3.33.2.1`/JDK `25`;
- testes de contrato REST para path, validação, `202`, JSON, OpenAPI e erros;
- testes de serialização e compatibilidade dos contratos v1 das duas filas;
- testes unitários RED/GREEN da política de tentativas, prazo e mapeamento de situações;
- testes do mock de pré-validação e da ACL local de `ConsultarDossieProduto`;
- testes de publisher/listeners para Complete, Abandon e DeadLetter;
- teste de transação `schedule + Complete`, com skip proibido: indisponibilidade deve bloquear a
  alegação de atomicidade e ser apresentada ao usuário;
- teste Quarkus com Dev Services e duas filas reais no emulador;
- teste ponta a ponta do POST até o evento de log da fila de saída, com delays curtos apenas no
  profile de teste;
- testes OpenTelemetry com exporter em memória e inspeção dos logs estruturados;
- ArchUnit para os núcleos e as bordas de `orquestrador` e `monitoramento`, incluindo provas
  negativas das dependências proibidas e acesso ao Hub somente pela ACL;
- suíte completa e checkpoint SonarQube ao fim de cada incremento coerente que altere fingerprint.

## Tarefas

### Task 1 — Resolver a decisão arquitetural antes do código

**Descrição:** propor ADR sucessor do ADR-0009 para registrar extensão `1.2.5`, connection string,
Dev Services e transporte por profile, sem apagar o histórico da decisão anterior.

**Critérios de aceitação:**

- ADR novo começa como `Proposto` e referencia o ADR-0009;
- índice explica extensão, autenticação, emulador, limitação de compatibilidade e aplicabilidade;
- ADR-0009 somente recebe `Substituído por ADR-NNNN` depois de aprovação humana.

**Verificação:** inspeção do diff Markdown e links; nenhuma mudança derivada.

**Dependências:** checkpoint C0 de arquitetura.

**Arquivos prováveis:** `doc/adr/0010-*.md`, `doc/adr/README.md`,
`doc/adr/0009-azure-sdk-service-bus-dossie.md`.

**Escopo estimado:** médio, 3 arquivos.

### Task 2 — Provar compatibilidade da extensão 1.2.5

**Descrição:** após baseline, adicionar o BOM/extensão e um smoke test mínimo que prove resolução,
compilação, augmentation e injeção do `ServiceBusClientBuilder` no stack atual.

**Critérios de aceitação:**

- somente a versão `1.2.5` é usada;
- `mvn dependency:tree` não revela conflito não explicado entre Quarkus, Azure Core, Vert.x,
  Reactor, Netty, Jackson ou OpenTelemetry;
- JDK `25`, compilação e bootstrap Quarkus ficam verdes sem namespace Azure real.

**Verificação:** versões do Java/Maven, dependency tree, teste focado e checkpoint SonarQube.

**Dependências:** Task 1 aceita, GO e baseline SonarQube.

**Arquivos prováveis:** `pom.xml`, um teste de compatibilidade e configuração mínima de teste.

**Escopo estimado:** médio, até 3 arquivos.

### Checkpoint C1 — Compatibilidade

- apresentar árvore efetiva, versões e resultado de augmentation;
- falha exige decisão humana; não fazer downgrade, override ou upgrade por suposição.

### Task 3 — Configurar duas filas no Dev Services

**Descrição:** configurar o emulador com as filas de entrada/saída, imagens fixadas e profiles que
selecionam emulador ou conexão externa sem fallback produtivo.

**Critérios de aceitação:**

- ausência de namespace/connection string em dev/test inicia o emulador;
- presença de connection string usa o ambiente real e não inicia Dev Services;
- dev/test usa AMQP/TCP; profile Azure usa WebSockets/443;
- segredo não aparece em source, logs, erros ou relatório de teste.

**Verificação:** bootstrap com Docker, inspeção das entidades e teste de seleção por profile.

**Dependências:** C1 e aceitação humana das EULAs.

**Arquivos prováveis:** `src/main/azure/servicebus-emulator/config.json`,
`src/main/resources/application.properties`, `src/test/resources/application.properties` e até
dois testes/profiles.

**Escopo estimado:** médio, até 5 arquivos.

### Task 4 — Fixar contratos e política de monitoramento

**Descrição:** implementar primeiro os testes e depois os tipos/mappers mínimos dos contratos REST
e Service Bus, junto da política pura de tentativa/prazo/situação. A política será uma porta
injetável produzida por CDI a partir de um objeto de configuração que seleciona uma definição
nomeada. Retomar o GREEN existente e alinhar os packages conforme a revisão humana, sem refazer
a política nem reabrir os itens 2.1 e 3.1.

**Subfatias de retomada, na ordem:**

1. Migrar `PoliticaMonitoramento`, `PoliticaMonitoramentoProgressiva`, seu teste e os dois testes
   de infraestrutura Service Bus de `doctree.monitoramentomtr` para `monitoramento`. Atualizar
   declarações/imports, preservar os testes e verificar ausência de referências Java ao package
   anterior. Não mover `src/main/azure/servicebus-emulator/config.json`.
2. Criar e executar o RED da configuração tipada e do producer CDI; fazer o GREEN em
   `monitoramento.adaptador.configuracao`, com os valores em `application.properties`.
3. Completar contratos e mappers mínimos, respeitando o ownership de cada borda, e detalhar a
   obtenção dos parâmetros iniciais e a composição técnica CDI antes dos respectivos checkpoints.
4. Acrescentar as provas ArchUnit aplicáveis aos tipos introduzidos; executar testes focados,
   revisão do incremento e checkpoint Sonar do item 4.1. Manter 5.1 pendente.

**Critérios de aceitação:**

- JSON e propriedades AMQP coincidem com os contratos aprovados;
- contratos inválidos são classificados sem depender do SDK;
- política distingue tentativa funcional de redelivery técnica;
- política e testes residem na raiz `monitoramento`, sem `doctree`, `monitoramentomtr` ou um
  subpackage `monitoramento.simtr`; domínio, aplicação, portas e adapters têm limites testáveis,
  permitindo Quarkus no domínio e na aplicação conforme ADR-0001;
- política progressiva exige ao menos um intervalo e repete o último intervalo quando a tentativa
  ultrapassa a lista; uma lista com apenas PT30M agenda sempre a cada 30 minutos;
- máximo de tentativas é opcional e, quando ausente, não participa da decisão; duração máxima é
  obrigatória e permanece ativa;
- política ativa, versão, tipo, intervalos, máximo opcional e duração vêm integralmente de
  application.properties; seleção ou valores inválidos falham no bootstrap.

**Verificação:** regressão da migração, RED/GREEN de configuração e bootstrap CDI, testes
unitários/de serialização, ArchUnit e checkpoint Sonar do incremento.

**Dependências:** C0.1 de arquitetura, C0.2 de contrato, decisão de política configurável e
revisão humana dos packages; C2 continua posterior ao item 6.1.

**Arquivos prováveis:** até cinco arquivos por subfatia em packages de domínio, aplicação,
configuração e DTOs de borda; dividir a task em subfatias internas se o limite for excedido.

**Escopo estimado:** médio por subfatia, máximo 5 arquivos.

#### Contratos REST — recorte de continuidade de 4.1

Implementar em duas subfatias, sem Resource, caso de uso, publicação ou novo endpoint:

1. RED/GREEN do request, validação Jakarta, tipo semântico `SolicitacaoMonitoramento` próprio do
   orquestrador e mapper REST -> interno (quatro arquivos contando o teste);
2. RED/GREEN do response, tipo semântico `MonitoramentoIniciado` e retorno pelo mesmo mapper
   (quatro arquivos contando a ampliação do teste).

Os dois identificadores de dossiê permanecem strings também neste modelo inicial, sem normalização
ou importação de tipos do Hub. Aplicar apenas a validação aprovada: campos não vazios e MTR como
inteiro decimal positivo. Não impor UUID à pré-validação. Provar JSON camelCase, ausência de
parâmetros controlados pelo servidor no request e tolerância vigente a campos desconhecidos usando
Jackson e Validator reais do Quarkus. Geração de IDs, HTTP 202, erro REST e OpenAPI do endpoint
permanecem em 6.1.

Divergência contratual resolvida no C4.1: `IdentificadorDossieProduto` do Hub recebe `Long` e o
usuário aprovou o limite público `1..9223372036854775807`. O request REST já rejeita valores acima
do teto, com regressão de limites e zeros à esquerda. A futura conversão na ACL de 5.1 deve
respeitar esse intervalo; nem essa ACL nem o endpoint foram implementados nesta subfatia.

Após os testes focados, revisar e executar checkpoint Sonar do incremento. Contratos Service Bus,
guardrails dos novos componentes e detalhamento arquitetural continuam pendentes dentro de 4.1.

Estado desta subfatia: request/response, modelos próprios e mapper implementados por RED/GREEN;
23 testes de contrato e 36 testes ArchUnit existentes aprovados. O primeiro checkpoint completo
ficou `NON_COMPLIANT` por `java:S6353`; após `ContinuarAjustes` humano e simplificação da regex,
706 testes, build e Compute Engine passaram e o novo checkpoint ficou `COMPLIANT`, conforme
evidências do checklist. Isso não conclui 4.1 nem comprova o endpoint HTTP.

Referências conferidas no runtime `3.33.2.1`: [validação Quarkus](https://quarkus.io/guides/validation/)
e [Jackson no Quarkus REST](https://quarkus.io/guides/rest-json/#configuring-json-support).

#### Contratos Service Bus — continuidade de 4.1

Executar RED/GREEN em subfatias de até cinco arquivos Java, com registro das evidências:

1. Produtor da entrada no orquestrador: modelo semântico próprio `TentativaMonitoramento`,
   DTO v1 de saída e mapper para corpo JSON/propriedades AMQP, com teste usando o ObjectMapper
   real do Quarkus. Apenas montar a mensagem; não criar client, publicar nem agendar.
2. Consumidor da entrada no monitoramento: DTO próprio, validação de contrato antes do
   mapeamento e modelo semântico próprio. Provar compatibilidade do JSON do produtor e
   classificação de conteúdo inválido sem depender de tipos do SDK.
3. DTO/mapper de reagendamento na saída do monitoramento, preservando o contrato da entrada.
4. Contratos/mappers de resultado nas duas bordas, também independentes e com prova de
   compatibilidade JSON; não implementar processamento terminal ou settlement.
5. Guardrails ArchUnit aplicáveis, provas negativas e permissão positiva de Quarkus/Mutiny/CDI.

Os contratos mantêm os campos v1 aprovados, identificadores string e limite MTR já aceito.
Nenhum DTO é compartilhado entre bordas; SDK permanece nos adapters Service Bus. A montagem
de uma mensagem não é sua publicação: factory, clients, listeners e endpoint continuam nos
itens posteriores. Revisar e executar checkpoint Sonar após incremento coerente.

### Task 5 — Implementar consultas da pré-validação e do Hub

**Descrição:** criar em `monitoramento` a porta de consulta à pré-validação, seu adapter simulado
e a ACL local para a porta pública `ConsultarDossieProduto`.

**Critérios de aceitação:**

- mock retorna cenários determinísticos e identifica origem simulada;
- ACL traduz somente identificador e situação necessários ao consumidor;
- não há HTTP local nem dependência em Resource, DTO MTR/REST, caso de uso concreto ou porta de
  saída do Hub.

**Verificação:** testes unitários, teste CDI e ArchUnit RED/GREEN.

**Dependências:** Task 4 e checkpoint arquitetural.

**Arquivos prováveis:** `monitoramento.{dominio,aplicacao.porta.saida}`, adapters em
`monitoramento.adaptador.saida.{simulador.prevalidacao,acl.simtrhub}` e testes; máximo 5 por subfatia.

**Escopo estimado:** médio por subfatia.

### Task 6 — Entregar POST até a fila de entrada

**Descrição:** implementar a primeira fatia vertical do endpoint, caso de uso e publisher
assíncrono do orquestrador: REST -> porta de entrada -> caso de uso -> porta de saída -> adapter
Service Bus. A regra de monitoramento permanece no componente `monitoramento`.

**Critérios de aceitação:**

- request válido gera IDs e mensagem v1 determinística;
- resposta `202` ocorre depois da confirmação do broker;
- validação/falha usam o contrato de erro existente sem revelar detalhes do Service Bus.

**Verificação:** contrato REST, teste unitário do publisher e teste com fila do emulador.

**Dependências:** Tasks 3 e 4; C0.1 a C0.4 e definição da composição CDI/obtenção de parâmetros
iniciais. C2 revisa esta fatia depois de implementada.

**Arquivos prováveis:** até 5 por subfatia em
`br.gov.caixa.simtr.orquestrador.{dominio,aplicacao,adaptador}`.

**Escopo estimado:** médio por subfatia.

### Checkpoint C2 — Primeira fatia vertical

- POST publica exatamente uma mensagem válida na entrada;
- testes focados, arquitetura, segurança, telemetria e SonarQube sem decisão pendente.

### Task 7 — Processar mensagem terminal da entrada até a saída

**Descrição:** em `monitoramento`, consumir uma mensagem pelo adapter de entrada, acionar a porta
de aplicação, consultar as duas fontes por portas de saída, aplicar a política do domínio e
publicar o resultado pelo adapter Service Bus; concluir a entrada após confirmação.

**Critérios de aceitação:**

- processamento permanece não bloqueante e com concorrência limitada;
- falha recuperável abandona; contrato inválido envia à DLQ;
- `Complete` da entrada ocorre somente depois da confirmação da saída.

**Verificação:** testes RED/GREEN do caso de uso, listener e integração com emulador.

**Dependências:** Tasks 3, 4 e 5.

**Arquivos prováveis:** até 5 por subfatia em
`br.gov.caixa.simtr.monitoramento.{dominio,aplicacao,adaptador}`.

**Escopo estimado:** médio por subfatia.

### Task 8 — Reagendar situação não conclusiva

**Descrição:** em `monitoramento`, executar a decisão da política progressiva através da porta de
reagendamento. O adapter Service Bus realiza a transação de entidade única para agendar a próxima
mensagem e concluir a atual.

**Critérios de aceitação:**

- delay, tentativa e limite seguem a política versionada;
- falha técnica não incrementa tentativa funcional;
- commit confirma ambos os efeitos ou rollback mantém a mensagem atual disponível.

**Verificação:** testes da política, commit/rollback/redelivery no emulador e checkpoint técnico da
API efetivamente resolvida.

**Dependências:** Task 7.

**Arquivos prováveis:** serviço de reagendamento, handler de decisão e até três testes.

**Escopo estimado:** médio, até 5 arquivos.

### Task 9 — Consumir a saída e registrar o resultado

**Descrição:** implementar em `orquestrador.adaptador.entrada.servicebus` o listener da fila de
saída. Ele valida e mapeia a mensagem, aciona a porta de entrada e o caso de uso registra o
resultado por porta de saída implementada pelo adapter de log; o listener então faz o settlement.

**Critérios de aceitação:**

- evento estável permite localizar `monitoramentoId` e `orquestracaoId`;
- não registra body, PII, namespace ou segredo;
- log concluído permite `Complete`; falha recuperável gera `Abandon`; contrato inválido vai à DLQ.

**Verificação:** testes do listener, conteúdo do log e redelivery.

**Dependências:** Task 7 e checkpoint C0.4 de observabilidade.

**Arquivos prováveis:** listener/mapper, porta/caso de uso de recebimento, porta/adapter de log e
testes em `orquestrador`; dividir em subfatias de até 5 arquivos.

**Escopo estimado:** médio, até 5 arquivos.

### Task 10 — Fechar correlação OpenTelemetry ponta a ponta

**Descrição:** verificar instrumentação do SDK resolvido, adicionar apenas os spans/propagação que
faltarem e proteger o contrato observável.

**Critérios de aceitação:**

- trace do POST alcança o processamento da entrada e o consumo da saída;
- send/process/settle seguem semântica de mensageria aprovada e não duplicam spans do SDK;
- falhas têm status e `error.type` controlado; campos proibidos permanecem ausentes.

**Verificação:** exporter em memória, captura de logs e testes de contrato observável.

**Dependências:** Tasks 6 a 9.

**Arquivos prováveis:** helper de propagação somente se necessário e testes de observabilidade;
instrumentação funcional deve ser incorporada às classes das fatias anteriores.

**Escopo estimado:** médio, até 5 arquivos.

### Checkpoint C3 — Fluxo ponta a ponta

- executar POST e observar entrada, consulta simulada, consulta Hub, reagendamento/resultado e log;
- provar Complete, Abandon, DLQ e correlação sem segredo/PII;
- registrar limitações do emulador e o que ainda exige Azure real.

### Task 11 — Consolidar documentação e verificação final

**Descrição:** atualizar arquitetura, tarefas e documentação operacional somente após o estado
implementado ficar verde.

**Critérios de aceitação:**

- consolidado registra novos packages, portas, adapters, contratos e limitações;
- ADR aprovado possui status correto e ADR substituído permanece no histórico;
- nenhuma documentação afirma durabilidade ou prontidão produtiva inexistente.

**Verificação:** suíte completa, checkpoint SonarQube, `git diff --check` e revisão do diff.

**Dependências:** C3.

**Arquivos prováveis:** consolidado arquitetural, ADRs e esta pasta de tasks; sem derivados.

**Escopo estimado:** médio, até 5 arquivos.

## Checkpoints humanos obrigatórios antes de produção

| Checkpoint | Decisão necessária |
|---|---|
| C0 — arquitetura | novo package/orquestrador, ownership do processor, ADR sucessor, extensão e autenticação |
| C0 — contrato | path, verbo, `202`, requests/responses, mensagens v1, validação e OpenAPI |
| C0 — segurança | autorização REST, SAS `Send+Listen`, segredo externo, mock fora de produção e EULAs |
| C0 — observabilidade | nomes/kinds de spans, atributos, eventos de log e propagação |
| GO | autorização explícita antes de `pom.xml`, `src/`, testes ou configuração executável |
| C1 | aceitar ou bloquear a matriz efetiva de dependências |
| C2 | aceitar a primeira fatia POST -> fila de entrada |
| C3 | aceitar o fluxo end-to-end e suas limitações |
| CF | aceitar o encerramento da feature |

## SonarQube

- a revisão documental anterior não executou análise; a retomada de implementação foi autorizada
  pelo usuário em 2026-09-06;
- a sessão não conservava o baseline anterior. Antes desta migração/configuração, foi inicializada
  uma nova referência local sobre o workspace preservado, sem remover alterações: 660 testes,
  217 issues, cobertura 85,8% e duplicação 3,9%;
- essa referência já inclui a política pura da retomada e não substitui a evidência histórica do
  item 3.1 (213 issues). As quatro issues adicionais foram identificadas como `java:S5778` nas
  lambdas do teste da política; a revisão ajustou as lambdas sem alterar comportamento;
- depois de cada incremento coerente que altere o fingerprint: executar o checkpoint;
- `NON_COMPLIANT` exige evidência e decisão humana conforme `AGENTS.md`.
- primeiro checkpoint da migração/configuração: 214 issues, 1 nova CRITICAL/HIGH (`java:S8911`),
  cobertura 86,3%, duplicação 3,9%; os quatro `java:S5778` anteriores foram resolvidos;
- após decisão humana `ContinuarAjustes` e ajuste da inicialização CDI: 213 issues, 0 novas,
  0 HIGH/BLOCKER/CRITICAL, cobertura 86,3% e duplicação 3,9%; situação `COMPLIANT`, sem nova decisão
  Sonar pendente. Evidências completas e referência da análise estão no checklist.

## Riscos e controles

| Risco | Impacto | Controle |
|---|---|---|
| pedido contradiz ADR-0009 aceito | alto | ADR sucessor Proposto e checkpoint antes de dependência/código |
| extensão 1.2.5 não testada com Quarkus 3.33/JDK 25 | alto | spike inicial; sem downgrade/override automático |
| versão `preview` da extensão | alto | isolar na borda, testes de bootstrap/runtime e registrar suporte limitado |
| emulador não suporta WebSockets | alto | AMQP/TCP em dev/test; WebSockets/443 apenas no Azure |
| EULA e Docker obrigatórios no Dev Services | alto | aprovação explícita, imagens fixadas e diagnóstico de indisponibilidade |
| uma connection string para papéis combinados | médio | política dedicada `Send+Listen`, sem `Manage`, segredo externo e futura separação |
| mock de pré-validação usado indevidamente | alto | ativação explícita, origem observável e bloqueio de fallback produtivo |
| sem Outbox/estado durável | alto | classificar como demonstrador e separar feature de persistência |
| redelivery duplica saída/log | médio | IDs determinísticos, duplicate detection e limitação documentada |
| fixture atual retorna `Rascunho` | médio | delays curtos no teste e cenário de quarentena; não alterar semântica MTR silenciosamente |
| contexto de trace se perde na fila | médio | teste ponta a ponta e propagação manual somente se necessária |
| builder compartilhado mutado por múltiplos beans | médio | fábrica CDI única e inicialização centralizada |
| listeners mantêm event loop bloqueado | alto | SDK e adaptação Reactor nas bordas Service Bus; Quarkus/Mutiny permitidos no núcleo; não bloquear event loop com `block/await/sleep` |
| referências ao package anterior sobreviverem à migração | médio | cinco arquivos migrados, regressão aprovada e busca Java sem `doctree`/`monitoramentomtr`; manter esse controle nas demais subfatias |
| regras ArchUnit selecionam apenas domínios do Hub em parte dos checks | alto | ampliar seletores e provas negativas para os dois novos componentes nas respectivas fatias |
| orquestrador precisa dos parâmetros iniciais de uma política que pertence ao monitoramento | médio | porta/ACL aprovada no ADR-0011; provar tradução e preservação dos parâmetros nas fatias de implementação |
| implementação da composição CDI divergir do ADR-0011 aceito | médio | fábrica em `arquitetura.infraestrutura.servicebus`, acesso somente pelos adapters Service Bus; verificar injeção/lifecycle em 6.1, sem acoplar núcleos ou compartilhar contratos |
| infraestrutura transversal virar destino de todo código assíncrono | alto | manter regras em seus componentes mesmo com Quarkus reativo no domínio; limitar infraestrutura à composição técnica e preservar `hub.arquitetura` no Hub |
| nova sessão perder novamente o baseline | médio | nova referência local registrada sobre o workspace preservado; consultar checklist e estado da sessão, sem remover alterações nem inferir conformidade histórica |

## Dependências

- decisão humana sobre o ADR sucessor e os quatro checkpoints C0;
- GO humano explícito;
- fonte de baseline SonarQube escolhida depois do GO;
- JDK 25 e Docker disponíveis;
- aceitação explícita das licenças do emulador e SQL;
- para prova Azure posterior: namespace, filas e connection string fornecidos por canal seguro,
  nunca pelo chat.

## Decisões registradas antes do GO

O checkpoint C0.1 foi aprovado em 2026-09-04: `br.gov.caixa.simtr.orquestrador` será responsável
pela entrada REST, publicação inicial e consumo do resultado;
`br.gov.caixa.simtr.doctree.monitoramentomtr` foi o package originalmente definido para o processor,
substituído na revisão humana de 2026-09-06 por `br.gov.caixa.simtr.monitoramento`. Ambos serão
componentes irmãos fora de `hub`; o acesso ao dossiê ocorrerá somente pela porta pública e por ACL
local. O ADR-0010 foi aceito no item 1.2; o histórico de decisões permanece no checklist.

O checkpoint C0.2 foi aprovado em 2026-09-04: o path, o request, a validação, o `202 Accepted`
após confirmação do broker, o response, os contratos v1 independentes das filas, o erro REST
existente, a tolerância a campos JSON desconhecidos e o OpenAPI gerado pelo código foram aceitos.

O checkpoint C0.3 foi aprovado em 2026-09-04: connection string/SAS externa nos ambientes reais,
emulador em dev/test, ausência de Entra ID, permissões `Send + Listen` sem `Manage` ou
`RootManageSharedAccessKey`, segredo fora de código e telemetria, autorização vigente sem papel
novo, mock somente leitura explicitamente habilitado sem fallback produtivo, validação da
superfície de entrada e aceite explícito das EULAs do Service Bus Emulator e do SQL Server.

O checkpoint C0.4 foi aprovado em 2026-09-04: nomes e kinds dos spans, eventos de log estruturado,
atributos permitidos, proibições de dados sensíveis, propagação W3C e caracterização da
instrumentação efetiva do SDK antes de adicionar spans manuais foram aceitos.

O item E0.1 foi aprovado em 2026-09-04: o projeto permanecerá no Quarkus `3.33.2.1` nesta feature,
e o upgrade para `3.33.3.2` continuará fora do escopo.

Foi decidido em 2026-09-04 usar connection string externa nos ambientes reais e emulador em
dev/test, sem Microsoft Entra ID nesta feature. Essa decisão resolve a escolha do mecanismo de
autenticação, mas não implica aceite das EULAs nem conclui os demais itens de segurança.

## GO registrado

O usuário registrou GO em 2026-09-04 após aprovar os checkpoints de arquitetura, contrato,
segurança, observabilidade e escopo de plataforma. A primeira alteração de produção, teste, build
ou configuração executável continua condicionada à escolha e inicialização do baseline SonarQube
prevista nos itens B0.1 e B0.2 do `todo.md`.

## Retomada funcional de 4.1 — DTO/mapper de reagendamento (2026-09-07)

O usuário solicitou "vamos continuar agora com o plano", após a entrega estrutural C4.4 e
seu complemento Javadoc. Retomar somente a próxima subfatia pendente, sob os GOs existentes
de contratos e logs C4.2/C4.3. Resultado e guardrails restantes continuam pendentes; 5.1 não iniciado.

**Escopo:** substituir os dois esqueletos da saída do monitoramento por DTO v1 próprio e mapper
de JSON/AMQP, com validação equivalente à entrada. Receber a tentativa já decidida pelo domínio:
não incrementar contador, recalcular prazo/versão, criar IDs, publicar ou agendar. Reutilizar
somente o modelo semântico do próprio monitoramento e o marcador técnico do ADR-0012.

**Erro na borda:** completar a aplicação do padrão já aprovado em C4.2 ao reagendamento.
Evento local `monitoramento.servicebus.reagendamento.falhou`; códigos estáveis
`MONITORAMENTO_REAGENDAMENTO_CONTRATO_INVALIDO` e
`MONITORAMENTO_REAGENDAMENTO_SERIALIZACAO_FALHOU`, distinguindo validação de serialização.
DTO/helper próprios, nível ERROR, array JSON real, recurso da fila de entrada, id/código
propagados em `MapeamentoReagendamentoException extends RuntimeException`. Diagnóstico de
serialização contém apenas tipo/frames; validação não inventa stack técnico nem expõe valores.
Um registro por falha reconhecida. Erro inesperado não é capturado/reclassificado genericamente.

**Arquivos prováveis:** DTO/mapper existentes, helper de log, DTO de erro e exceção próprios
(cinco arquivos de produção); testes de contrato/compatibilidade/log e atualização pontual do
inventário de esqueletos. Atualizar Javadoc, guia, manifesto e arquitetura conforme o estado real.

**Critérios e verificação:** RED antes do GREEN; ObjectMapper/Validator reais no Quarkus;
JSON v1 exato e envelope determinístico; compatibilidade com consumidor independente;
limites inclusivos MTR e contador, obrigatoriedade e coerência temporal; erro JSON observado no
formatter efetivo, diagnóstico útil sem payload/causas e correlação válida; regressão focada
incluindo ArchUnit/CDI; checkpoint completo único do incremento com baseline original.

**Início seguro:** baseline integralmente idêntico à referência recuperada, credencial disponível
somente em memória; último checkpoint COMPLIANT de 859 testes. Fingerprint atual
`8d680e16a458d99d9b3beb6323a03d3818f0f46e33e23a0a52d360d96d6f22a2` inclui o
complemento Javadoc posterior ao checkpoint. Não reinicializar o baseline. Hub, simuladores,
extensão e `src/main/azure/servicebus-emulator/config.json` permanecem preservados.

## Revisão documental solicitada — guia Service Bus e Javadoc (2026-09-07)

O usuário identificou explicitamente `doc/guias/guia-service-bus-amqp-dossie.md` como o guia
desalinhado. Esse arquivo ainda descrevia o recorte histórico do ADR-0009: alteração de
`br.gov.caixa.simtr.dossie`, SDK direto, Entra ID, JSON de dois campos e monitor fora do guia.
A decisão vigente é ADR-0010/0011: orquestrador publica na entrada; monitoramento consome,
consulta/aplica critérios, reagenda na entrada ou publica resultado na saída; orquestrador
consome a saída e encerra o demonstrador registrando log. O package `dossie` e o Hub permanecem
inalterados. Autenticação por connection string/SAS, extensão Quarkus Azure Service Bus e
emulador via Dev Services já estão decididos; não reabrir essas escolhas.

**Prioridade atual:** pausar o GREEN do mapper de reagendamento e corrigir documentação e
Javadoc antes da continuidade funcional. Os três arquivos novos de testes de reagendamento
foram preservados em RED de compilação por API ainda ausente; a tentativa de criar a produção
foi interrompida e nenhum desses novos arquivos de produção foi gravado. Não apresentar o
workspace corrente como aprovado pelos 859 testes anteriores.

**Escopo documental autorizado:** reescrever o guia indicado conforme contrato/fluxo atuais;
alinhar guia de desenvolvimento, plano/checklist, manifesto, consolidado e ADRs somente onde
inconsistentes; contextualizar tasks históricas sem mudar suas decisões/encerramento.
Corrigir os onze Javadocs de portas com `undefined` e explicar etapas, filas, responsabilidades
e verificações nos tipos estruturais. Preservar todas as assinaturas, imports, anotações e corpos.

**Verificação:** links locais e referências Java; comparação do conteúdo fora de Javadoc;
DocLint isolado em diretório temporário sem gerar HTML; auditoria de preservação de arquivos.
Este recorte é exclusivamente documental: não inspecionar Sonar, credencial ou baseline, não
executar Maven nem checkpoint. O RED anterior permanece registrado, sem nova execução.
Resultado/reagendamento/guardrails de 4.1 continuam pendentes e 5.1 não será iniciado.

## Evidências da revisão documental do guia Service Bus (2026-09-07)

- Guia indicado reescrito para as duas filas e os componentes `orquestrador`/`monitoramento`;
  critérios de no-op, limites/quarentena, situação conclusiva e reagendamento descritos na ordem
  do plano. Connection string/SAS, extensão e Dev Services explícitos; `dossie`/Hub preservados.
- Guia de desenvolvimento, manifesto, consolidado, ADRs 0009/0010/0011 e índice alinhados.
  ADR-0012 conferido e preservado. Tasks do guia antigo receberam somente contexto histórico,
  sem mudar decisões ou encerramento. Documento amplo de origem preservado.
- Vinte tipos Java revisados exclusivamente em Javadoc, incluindo onze métodos de portas:
  removidos os onze `undefined`, com comportamento esperado, parâmetros/retorno, etapas e testes.
- Comparação antes/depois confirmou conteúdo idêntico fora de Javadoc nos vinte arquivos.
  DocLint isolado: exit 0, nenhum erro; nove avisos de construtores implícitos nas classes vazias,
  preservados para não criar código artificial. Saída apenas temporária, sem HTML.
- Doze documentos e 213 links locais conferidos, nenhum destino ausente. Nenhum arquivo original
  removido. Auditoria dos fontes apontou somente os vinte Javadocs alterados; código/configuração,
  `dossie`, Hub, simuladores, extensão e arquivo do emulador mantidos.
- `git diff --check` passou; apenas avisos preexistentes de LF/CRLF, sem normalização.
  Nenhum Maven, baseline, credencial, API ou checkpoint Sonar executado neste recorte documental.
- Próxima pendência funcional permanece em 4.1: completar o GREEN do DTO/mapper de reagendamento
  e seus logs/testes. Os três arquivos novos de teste do RED anterior foram preservados; a API
  esperada está ausente e não há novo resultado verde. Depois restam contratos de resultado e
  guardrails. Nenhum commit, mudança de branch ou avanço para 5.1.

## Continuidade de 4.1 — implementação e cobertura de resultado (2026-09-08)

Direção humana: "vamos fazer uma implementação vamos focar na implemntação e na cobertura
sem seguir exatamente o TDD". Para esta continuidade, implementar e verificar comportamento,
regressão e cobertura sem exigir a sequência RED anterior à produção. O histórico TDD das
subfatias concluídas permanece preservado.

Escopo: modelos próprios de resultado, DTOs/mappers produtor e consumidor independentes, JSON
v1 e envelope aprovados, logs locais tipados com diagnóstico seguro, testes reais de contrato,
cobertura e compatibilidade. Completar os guardrails previstos de isolamento por borda e acesso
das ACLs à API pública. Permanecer em 4.1, sem listeners, publicação, settlement ou 5.1.
Baseline original READY e checkpoint de 900 testes COMPLIANT conferidos; sem reinicialização.

Implementar em subfatias locais: modelos/DTOs; mapper/erro do produtor; mapper/erro do consumidor;
testes de contrato e logs; guardrails e regressão; checkpoint completo único do incremento.
A compatibilidade das duas bordas constitui a unidade de verificação; a orientação atual de
foco na implementação/cobertura substitui a ordem rígida de RED e o limite histórico de cinco
arquivos, sem autorizar compartilhamento de DTOs, mudança transversal ou ampliação funcional.

Contrato a confirmar: o fluxo aprovado verifica limites antes de consultar MTR; o texto v1 não
define nulidade de situacaoMtr nem contador zero na quarentena anterior à primeira consulta.
Proposta submetida: permitir null/zero nesse caso e exigir situação conclusiva e pelo menos
uma tentativa no resultado CONCLUSIVO. Não aplicar essa validação antes da resposta humana.
Os demais campos/tipos e propriedades seguem o contrato aprovado; motivo textual é preservado.
inputSequenceNumber conserva o long recebido, sem interpretar seus bits ou usá-lo como contador.
concluidoEm pode coincidir com iniciadoEm, mas não precedê-lo.

Arquivos: os dois modelos e quatro DTOs/mappers estruturais existentes; helpers, DTOs de erro e
exceções próprios de cada borda; testes espelhados, inventário de esqueletos e guardrails com
fixtures negativas/positivas. Atualização documental restrita ao estado e às evidências.

Verificações: ObjectMapper e Validator reais no Quarkus; JSON exato e envelope determinístico;
campos ausentes/nulos/tipos inválidos/overflow/datas; preservação de IDs e zeros; compatibilidade,
campos desconhecidos e rejeição de conteúdo extra; um erro sanitizado por falha reconhecida,
id/código propagados, trace válido e erro inesperado não reclassificado. Regra nova de arquitetura
deve rejeitar fixtures inválidas e aceitar as portas/modelos públicos e o framework já permitido.

## Ajuste Sonar do resultado — autorizado em 2026-09-08

O incremento de resultado e guardrails passou em 317 testes focados e no checkpoint completo
com 1021 testes em 171 classes, sem falhas, erros ou ignorados. Cobertura 87,0%, duplicação 4,4%.
O checkpoint de 2026-09-08T10:10:55.4993743-03:00 ficou NON_COMPLIANT: 215 issues abertas,
duas novas CRITICAL/HIGH de java:S1192 no mapper consumidor de resultado. São os literais
"paraResultado" (três ocorrências) e "validarTipos" (cinco ocorrências), sem falha funcional.
Issues: 1dfde59a-ce66-490f-97d7-7f227d974d46 e 1f4b5927-0402-44d5-b3fd-283dd75c5b7e.
Análise: ddc71af5-f271-48a7-8512-2ee6f7c61c7e; CE: c3b09788-5b67-4f89-b732-fa1d871fff40.
Fingerprint: f78cac3cee1c3f3ca65b857e9be8be403d14a3733809b2eed42b7facd267e975.

Direção humana: "vamos resolver os problemas sonar". Decisão ContinuarAjustes registrada
pelo script do checkpoint. Escopo do ajuste: extrair duas constantes no mapper consumidor,
preservar os textos emitidos e os testes existentes, executar regressão focada e novo checkpoint
com o baseline original. Sem aceitação excepcional, reinicialização de baseline ou avanço para 5.1.
A definição de nulidade/contador da quarentena anterior à primeira consulta MTR continua
aguardando resposta à pergunta de contrato; esta autorização trata das duas issues Sonar.

## Resultado e guardrails — evidência final do ajuste Sonar (2026-09-08)

- Modelos/DTOs independentes e mappers das duas bordas de resultado implementados, com JSON
  v1, envelope determinístico, validação estrutural/temporal e erros locais tipados e sanitizados.
  Sem envio, listener, settlement ou persistência. Validação especial da quarentena pendente.
- 98 testes de contrato de resultado, 11 de logs reais e 18 de fronteiras ArchUnit aprovados.
  Guardrails exercitam DTOs por borda, domínio/portas e acesso das ACLs à API pública, com
  fixtures positivas e negativas. Seis tipos implementados retirados do inventário: restam 19.
- Regressão após S1192: 317 testes em 11 classes, zero falhas/erros/ignorados.
  Comando: `mvn -q "-Dtest=ResultadoMonitoramentoContratoTest,ResultadoServiceBusLogTest,FronteirasMonitoramentoArchUnitTest,EstruturaMonitoramentoArchUnitTest,EsqueletosMonitoramentoCdiTest,ArchUnitProgressivoTest,MonitoramentoReagendamentoContratoTest,MonitoramentoReagendamentoLogTest,MonitoramentoEntradaContratoTest,MonitoramentoEntradaLogTest,OrquestradorEntradaLogTest" test`.
- O ajuste de produção extraiu somente OPERACAO_MAPEAMENTO e OPERACAO_VALIDACAO_TIPOS no
  mapper consumidor de resultado. Textos dos logs e comportamento preservados; comparação
  reversa da extração idêntica ao arquivo anterior. Nenhum teste foi alterado para esse ajuste.
- `./validar-checkpoint-sonarqube.ps1` concluiu build, scanner e Compute Engine em
  `2026-09-08T10:30:53.8267228-03:00`: **1021 testes em 171 classes**, sem falhas/erros/ignorados.
  **COMPLIANT**, decisão técnica `NOT_REQUIRED`, 213 issues (baseline 217), nenhuma nova
  ou HIGH/BLOCKER/CRITICAL, cobertura **87,0%**, duplicação **4,4%**.
- API local confirmou ambas as issues S1192 como CLOSED/FIXED, sem supressão ou exceção.
  CE: `22a6d6fe-92da-4f60-87cf-c1a4f2807310`; análise: `c86b89ae-ac8d-49d2-b540-439d06446b38`.
  Fingerprint: `13df928607bf0c276fecfbba4395ada0195fbc41f0ecf06c47fd6e4095f389d9`, idêntico ao código atual.
- JaCoCo: 157/157 linhas cobertas nos dois mappers, dois helpers de log e duas exceções de
  resultado. Essa cobertura não conclui a definição contratual de quarentena ainda pendente.
- Baseline comparado integralmente com a referência original, sem reinicialização.
  Auditoria SHA-256 do incremento de resultado: 598 arquivos preexistentes preservados,
  seis tipos de produção e o inventário alterados, seis novos arquivos de produção e nove
  de testes/fixtures, nenhum removido. Inclui preservação de Hub, dossiê, reagendamento,
  configurações, pom.xml e AGENTS.md no incremento.
- Revisão final: correção, simplicidade, arquitetura, segurança, desempenho, testes e escopo
  conferidos; extração local sem nova dependência ou efeito externo. Documentação de continuidade
  alinhada. Nenhum staging, commit, avanço para 5.1 ou encerramento humano da feature.


## Acompanhamento de duplicação — 2026-09-08

O usuário perguntou se a duplicação exige preocupação. O checkpoint atual mede 4,4%, abaixo
do limite de 5%, com margem de 0,6 ponto percentual; o incremento anterior media 3,6%.
Consulta aos blocos reais do Sonar local: os dois LogErroResultado têm 62 e 61 linhas marcadas
como duplicadas, incluindo montagem/emissão do JSON e diagnóstico; as duas
MapeamentoResultadoException têm 22 linhas cada. Há também blocos nos helpers anteriores.
Os DTOs novos não aparecem entre os arquivos com duplicação positiva nessa consulta.

Risco registrado: repetição técnica pode aumentar custo de manutenção e consumir a margem.
Priorizar avaliação da montagem/emissão técnica dos logs, preservando DTOs, exceções,
classificação e sanitização locais conforme ADR-0012. Uma extração transversal exige proposta
concreta e checkpoint arquitetural antes da implementação. Esta avaliação não alterou código,
exclusões Sonar, limiar, baseline ou Hub e não representa autorização para mudar a arquitetura.

## Preparação da continuidade de 4.1 — 2026-09-08

Usuário: "ok prosseguimos". Conferidos o estado atual, fluxo aprovado, DTOs/mappers, teste de
contrato, logs e limites do ADR-0012. A proposta de nulidade/contador da quarentena foi apresentada
novamente para resposta; não houve decisão inferida. A [matriz de validação preparada](preparacao-validacao-resultado.md)
identifica os casos e arquivos do próximo incremento. A avaliação da duplicação confirmou que
centralizar emissão exige proposta e checkpoint arquitetural; não houve extração transversal.

Neste recorte preparatório foram alterados somente documentos da feature. Código, testes,
baseline e último checkpoint permanecem preservados; sem Maven, API Sonar, commit ou 5.1.

## Validação final de resultado — confirmação humana em 2026-09-08

Usuário respondeu "confirmo" à proposta: QUARENTENA com situacaoMtr=null e zero tentativas
quando ainda não houve consulta; CONCLUSIVO com situação conclusiva e pelo menos uma tentativa.
Essa confirmação autoriza a validação local nas duas bordas de resultado em 4.1.

Implementação: manter DTOs independentes, rejeitar contador negativo e validar que CONCLUSIVO
possui tentativasRealizadas >= 1 e situacaoMtr em CONFORME, NAO_CONFORME ou PENDENTE_INFORMACAO.
QUARENTENA aceita null/zero e preserva o contador não negativo recebido; ausência de situação
não permite reconstruir consultas anteriores. Não recalcular situações, motivo ou contador.
A validação auxiliar não deve acrescentar propriedade ao JSON v1.

Verificações: produtor e consumidor reais, JSON exato com situação nula na quarentena,
rejeição de conclusivo sem situação/consulta, três situações conclusivas no limite mínimo,
contador negativo e compatibilidade dos limites numéricos; log real sanitizado da nova rejeição,
regressão e checkpoint completo. Preservar formato de erros, configurações, Hub e baseline.
A implementação segue o foco autorizado em cobertura sem ordem rígida de TDD.
O item 4.1 será marcado tecnicamente concluído somente após essas verificações; 5.1 não iniciado.
Duplicação permanece acompanhada; esta confirmação não altera a responsabilidade de logging.

## Conclusão técnica de 4.1 — validação de resultado em 2026-09-08

- Direção humana: "confirmo" à regra apresentada de quarentena null/zero antes de consulta
  e CONCLUSIVO com situação conclusiva e pelo menos uma tentativa. Confirmação registrada
  antes da produção; não foi inferida da mensagem anterior "ok prosseguimos".
- Os dois DTOs próprios agora rejeitam contador negativo e validam a evidência de consulta
  em CONCLUSIVO: CONFORME, NAO_CONFORME ou PENDENTE_INFORMACAO e contador >= 1.
  QUARENTENA preserva situação MTR nula e contador não negativo recebido; ausência de estado
  não é preenchida com consulta, valor inventado ou contador da sequência técnica.
- Jakarta Validation executa a condição local; JsonIgnore impede propriedade auxiliar no JSON.
  Mappers, helpers de log, exceções e contratos de outras bordas permaneceram idênticos.
  Não houve extração transversal ou mudança de logging para reduzir artificialmente duplicação.
- 25 casos novos: quarentena null/zero e limites, ausência de situação no consumidor, contador
  negativo nas duas bordas, conclusivo sem situação/consulta, tipos inválidos e logs reais.
  Os três estados conclusivos foram verificados no mínimo de uma tentativa.
  ResultadoMonitoramentoContratoTest: 120 casos; ResultadoServiceBusLogTest: 14 casos.
- Regressão focada: **342 testes em 11 classes**, zero falhas/erros/ignorados. Comando:
  `mvn -q "-Dtest=ResultadoMonitoramentoContratoTest,ResultadoServiceBusLogTest,FronteirasMonitoramentoArchUnitTest,EstruturaMonitoramentoArchUnitTest,EsqueletosMonitoramentoCdiTest,ArchUnitProgressivoTest,MonitoramentoReagendamentoContratoTest,MonitoramentoReagendamentoLogTest,MonitoramentoEntradaContratoTest,MonitoramentoEntradaLogTest,OrquestradorEntradaLogTest" test`.
- `./validar-checkpoint-sonarqube.ps1` concluiu build, scanner e Compute Engine em
  `2026-09-08T11:12:19.3296159-03:00`: **1046 testes em 171 classes**, sem falhas, erros ou ignorados.
  **COMPLIANT**, decisão `NOT_REQUIRED`, 213 issues (baseline 217), nenhuma nova/severa,
  cobertura **87,0%**, duplicação **4,4%**. A duplicação não aumentou.
  CE `c189234e-e9d0-4a30-a4e1-38f845c48484`; análise `f3b6720c-0b18-4713-8a16-22a030278153`.
  Fingerprint `13c39b71061152a7498dd7c83022afee3342e8eea8fe4f8fa6bbbe936bcf86fe`, conferido com o código atual.
- JaCoCo da nova validação: 4/4 linhas e 6/6 condições em cada DTO, sem trechos descobertos.
  Comparação de SHA-256: somente dois DTOs e três testes/fixture alterados; 615 arquivos
  preexistentes preservados, nenhum adicionado ou removido em src/. Baseline integralmente
  idêntico à referência original; token permaneceu somente em memória.
- Revisão de correção, simplicidade, arquitetura, segurança, desempenho, testes e escopo:
  condição pura e local, sem nova dependência, dados transportados ou operação externa;
  JSON exato, diagnóstico sanitizado e fronteiras arquiteturais preservados.
- Plano/checklist, retomada, guias, manifesto, preparação e consolidado alinhados.
  **4.1 está tecnicamente concluído**, com seus contratos e guardrails. 5.1 permanece não iniciado;
  nenhum staging, commit ou encerramento humano da feature.

Referências de validação consultadas e confirmadas no runtime efetivo:
[Quarkus Validation](https://quarkus.io/guides/validation/) e
[Jakarta AssertTrue](https://jakarta.ee/specifications/bean-validation/3.1/apidocs/jakarta/validation/constraints/asserttrue).


## Preparação de commit e push para revisão com desenvolvedores — 2026-09-08

O usuário autorizou organizar commit e push na branch atual, depois de revisar o guia para
explicitar arquitetura, decisões adotadas e o que está pronto ou pendente. A revisão altera
somente Markdown; o código permanece no estado verificado por 1046 testes e checkpoint
COMPLIANT (87,0% de cobertura e 4,4% de duplicação). Nenhum novo incremento funcional.

Escopo: guia Service Bus como leitura principal, guia de desenvolvimento como inventário,
manifesto do pacote e notas de continuidade. Explicar no próprio guia as decisões aceitas dos
ADRs 0001/0003/0004/0006/0010/0011/0012 e as definições de contrato/política das tasks, além
de manter seus links. Corrigir referências desatualizadas à fila de saída ainda pendente.

Revisar e incluir os fontes/configurações/testes existentes da feature, ADRs, documentação e
tasks relacionadas. A orientação Azure de AGENTS.md e o documento de origem serão incluídos
após revisão: o primeiro orienta a continuidade e o segundo é destino de links dos guias.
O patch temporário .codex-doc-alignment.patch permanece local. Não publicar estado de sessão,
credenciais, relatórios Sonar, saídas de build ou formatos derivados. Fazer push normal para
origin na mesma branch, sem force, merge ou encerramento da feature.

## Revisão do guia e pacote para desenvolvedores — 2026-09-08

O guia principal passou a abrir com o que está pronto e explicitar arquitetura do mesmo runtime,
responsabilidade de cada componente, hexagonal pragmática, contratos independentes, ACLs, factory,
SAS/Dev Services, logging tipado e telemetria planejada. A seção desatualizada da fila de saída
foi substituída pelo contrato implementado, JSON v1 e regras confirmadas. Adicionado roteiro 5.1–CF.

Os nomes de telemetria planejados com prefixo doctree foram preservados como contrato histórico,
com nota explícita de que a revisão dos packages não os renomeou. Essa diferença está documentada
para caracterização/checkpoint da Task 10; nenhum sinal ou código foi alterado nesta revisão.

Guia de desenvolvimento e manifesto foram alinhados. A seleção agora inclui AGENTS.md e o
documento amplo de origem após revisão, preservando as referências para os devs. O patch
temporário continua local. O usuário já autorizou commit e push nesta branch; não repetir
pedido de autorização para essas duas operações.

## Verificação do pacote antes do commit — 2026-09-08

Revisados escopo, comportamento já entregue, fronteiras, diagnóstico seguro, configuração,
testes e estruturas futuras. Os 620 arquivos conferidos de src/, pom.xml e AGENTS.md ficaram
idênticos durante esta revisão documental; a evidência executável anterior permanece aplicável.

Verificados 17 documentos e 146 referências locais, sem destino ausente; os dois exemplos JSON
do guia são sintaticamente válidos e os dois diagramas distinguem arquitetura e fluxo planejado.
Conferidos os valores contra DTOs, política/configuração, factory inativa e infraestrutura de logs.
A seleção contém 125 arquivos; a busca por formatos de credenciais privadas/Sonar/GitHub/SAS
não encontrou candidatos, sem expor valores. O diff documental passou após ajuste de linha final.

A branch remota ainda não existia na consulta de origem. Publicação autorizada: commit único
do estado acumulado até 4.1 e push com upstream para a mesma branch. Nenhum merge ou force.
O único arquivo de trabalho preservado fora do pacote é .codex-doc-alignment.patch.
