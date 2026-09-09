# Arquitetura atual do simtr-hub

## Como usar este documento

- **Status:** aceito
- **Última consolidação:** 2026-09-09
- **Objetivo:** explicar rapidamente a arquitetura implementada e as restrições que novas features
  devem respeitar.

Leia este consolidado antes de planejar uma feature. Em seguida, consulte o
[índice de ADRs](../adr/README.md): a descrição do índice deve bastar para identificar quais decisões
se aplicam. Leia o ADR completo somente quando ele for aplicável à mudança ou quando houver dúvida.

O código, os contratos executáveis e os testes são a fonte de verdade do comportamento atual. Se
este documento divergir deles, registre a divergência no plano da feature antes de propor uma
correção.

## Visão do sistema

O `simtr-hub` é um monólito modular Quarkus organizado por domínios de negócio. Ele expõe doze
capacidades atômicas por REST e integra cada uma ao MTR ou ao simulador por adapters de saída
intercambiáveis.

```text
cliente HTTP
    -> adapter REST de entrada
        -> porta de entrada
            -> caso de uso atômico
                -> porta de saída
                    -> adapter selecionado
                        |-- MTR
                        `-- simulador
```

O caso de uso não conhece Resource, REST Client, URL, DTO MTR, fixture nem o mecanismo CDI que
seleciona o adapter. Não existem atualmente endpoint único de pré-validação, orquestrador local,
motor de workflow, MCP Server ou comunicação distribuída entre os domínios.

Além da borda REST, a consulta de documentos do dossiê pode ser iniciada pelo consumidor CDI local
`br.gov.caixa.simtr.dossie.ConsultaDocumentosDossieProduto`, no mesmo artifact e runtime Quarkus.
Esse caminho entra pela mesma porta de aplicação e não cria chamada HTTP local nem nova superfície
externa.

## Domínios e capacidades implementadas

| Domínio | Responsabilidade | Capacidades atuais |
|---|---|---|
| `arvoredocumento` | Dados parametrizados usados por uma futura árvore documental | `ConsultarProcessoParametrizado` |
| `conformidade` | Consulta de checklist por identificador e versão | `ConsultarChecklist` |
| `dossieproduto` | Operações atômicas do ciclo de vida do dossiê no MTR | `ConsultarDossieProduto`, `ConsultarDocumentosDossieProduto`, `CriarDossieProduto`, `AtualizarFormularioDossieProduto`, `IncluirDocumentoDossieProduto`, `RegistrarValidacaoNegocialDossieProduto`, `AlterarProdutosContratadosDossieProduto`, `CapturarDossieProduto`, `IniciarOuAvancarWorkflowDossieProduto` |
| `gestaodocumento` | Obtenção de credencial para o container documental | `ObterCredencialContainer` |

`parametrizacao` é o nome de um sistema/contrato upstream, não um domínio interno compartilhado.
As consultas de processo e checklist pertencem a consumidores diferentes e mantêm modelos e
mapeamentos próprios.

`prevalidacao` é um domínio futuro. Nenhum fluxo, estado ou aggregate deve ser inventado antes de
existirem requisitos, contratos e autorização próprios.

## API pública atual

| Método | Path público |
|---|---|
| `GET` | `/simtr-hub/v1/processo/identificador-negocial/{identificador}` |
| `GET` | `/simtr-hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}` |
| `GET` | `/simtr-hub/v1/dossie-produto/{id}` |
| `GET` | `/simtr-hub/v1/dossie-produto/{id}/documentos` |
| `POST` | `/simtr-hub/v1/dossie-produto` |
| `PATCH` | `/simtr-hub/v1/dossie-produto/{id}/formulario` |
| `POST` | `/simtr-hub/v1/dossie-produto/{id}/documento` |
| `PATCH` | `/simtr-hub/v1/dossie-produto/{id}/validacao-negocial` |
| `PATCH` | `/simtr-hub/v1/dossie-produto/{id}/produto` |
| `POST` | `/simtr-hub/v1/dossie-produto/{id}/capturar` |
| `POST` | `/simtr-hub/v1/dossie-produto/{id}/workflow` |
| `POST` | `/simtr-hub/v1/storage/container/credencial` |

Duas operações descritas na especificação de pré-validação ainda não existem no Hub:

- alterar garantia do dossiê;
- cancelar dossiê.

A existência dessas operações no MTR não autoriza endpoint, capacidade, adapter ou simulador no
Hub. Cada implementação futura exige feature, contrato, plano e GO próprios.

## Organização interna

O código usa **package by domain** e arquitetura hexagonal pragmática. Pastas e abstrações só são
criadas quando existe uma capacidade e um consumidor reais.

```text
<dominio>/
|-- dominio/
|   |-- modelo/
|   `-- erro/
|-- aplicacao/
|   |-- porta/
|   |   |-- entrada/
|   |   `-- saida/
|   `-- casodeuso/
`-- adaptador/
    |-- entrada/rest/v1/
    `-- saida/
        |-- mtr/
        `-- simulador/
```

Quarkus, Jakarta, MicroProfile, Mutiny, Jackson e OpenTelemetry podem apoiar qualquer camada. O
guardrail protege responsabilidades e direção das dependências, não uma pureza artificial de
framework.

Isso inclui Quarkus reativo, Mutiny (`Uni` e `Multi` quando necessários) e CDI no domínio e na
aplicação. A execução assíncrona não muda o componente responsável pela regra de negócio nem
justifica movê-la para infraestrutura. Essa permissão não leva SDKs de integração ou contratos
de transporte ao núcleo e não autoriza bloquear o event loop.

### Regras de dependência

1. `dominio` não depende de aplicação, Resources, adapters ou contratos de borda.
2. `aplicacao` não importa adapters, DTOs de borda, REST Clients ou outro domínio.
3. Adapter de entrada traduz seu contrato para uma porta de entrada.
4. Adapter de saída implementa uma porta de saída e traduz tipos internos para sua borda.
5. REST, MTR, simulador e um eventual MCP possuem DTOs independentes.
6. Mappers não convertem diretamente DTO de uma borda em DTO de outra.
7. `arquitetura` não contém regra, modelo ou erro específico de negócio.
8. Colaboração entre domínios atravessa uma porta do consumidor e uma camada anticorrupção.
9. A API pública de aplicação de um domínio contém somente portas de entrada e os tipos
   semânticos referenciados por elas.

ArchUnit protege essas fronteiras. Uma feature que precise alterar uma regra deve explicar a
necessidade no plano e obter checkpoint humano de arquitetura.

### Escopos da infraestrutura

`br.gov.caixa.simtr.hub` permanece inalterado: `hub.arquitetura` mantém as capacidades
arquiteturais de suporte próprias do Hub. A feature de monitoramento não migra nem generaliza
essas capacidades, incluindo segurança, erros e observabilidade.

Para a composição técnica compartilhada do Service Bus, o destino definido é
`br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus`, com fábrica e qualifiers implementados. Esse package
não recebe todo código assíncrono: políticas, tentativas, prazo e decisões de processamento
continuam em `monitoramento`; regras de orquestração continuam em `orquestrador`. Listeners,
publishers, DTOs e mappers pertencem aos adapters de cada componente.

A infraestrutura transversal não importa os componentes de negócio nem o Hub. Domínio e aplicação
não importam a fábrica ou o SDK Service Bus; somente as bordas Service Bus acessam os clientes.
Não se cria um framework assíncrono genérico. A composição aprovada e seus limites estão no
[ADR-0011](../adr/0011-composicao-local-monitoramento-e-fabrica-service-bus.md).

### Logs JSON dos novos adapters Service Bus

A composição técnica de `arquitetura.infraestrutura.observabilidade` instala um decorator
nos formatters JSON já configurados pelo Quarkus e restaura os delegates no encerramento.
Somente registros com marcador `CamposLogJson` de adapters Service Bus de `monitoramento`
e `orquestrador` recebem campos JSON tipados. Categorias do Hub são sempre excluídas,
inclusive quando recebem o marcador; os demais registros mantêm a saída do formatter original.

O marcador transporta somente campos técnicos previamente sanitizados; DTOs, códigos, mensagens
e classificação continuam nas bordas. Não há novo destino de log, alteração de configuração,
dependência ou migração de `hub.arquitetura`. A decisão e os limites estão no
[ADR-0012](../adr/0012-campos-json-tipados-logs-service-bus.md).

O mapper consumidor da entrada registra falhas de parsing, envelope AMQP e contrato com
`evento`, `recurso`, `id_erro`, `codigo_erro`, array `erros` e `detalhe`, sem `codigo_http`.
Propaga uma exceção própria com o mesmo id/código. Falhas técnicas acrescentam classe/frames
sanitizados e localização JSON numérica quando disponível; trace é incluído somente se válido.
Payload e mensagens originais da exceção não entram no registro. DTO e classificação pertencem
à borda de `monitoramento`.

O mapper produtor da entrada em `orquestrador` registra falhas de serialização com o mesmo
formato e DTO próprio. Usa o evento `orquestrador.servicebus.entrada.falhou` e o código
`ORQUESTRADOR_ENTRADA_SERIALIZACAO_FALHOU`; propaga `SerializacaoEntradaException`, subtipo
de `RuntimeException`, com id/código e sem a causa original. O mapper captura especificamente
`JsonProcessingException` e a traduz para essa falha local unchecked, sem exigir declaração
checked do chamador. Preserva o JSON e as propriedades AMQP de sucesso, sem realizar publicação.
O mapper de reagendamento do monitoramento aplica o mesmo formato com DTO/helper próprios.
Usa `monitoramento.servicebus.reagendamento.falhou` e os códigos
`MONITORAMENTO_REAGENDAMENTO_CONTRATO_INVALIDO` e
`MONITORAMENTO_REAGENDAMENTO_SERIALIZACAO_FALHOU`. Propaga
`MapeamentoReagendamentoException extends RuntimeException` com o id/código registrado.
Validação não expõe valores rejeitados ou stack; serialização registra somente tipo/frames,
sem causa, mensagem original ou processor. Erros inesperados não são reclassificados.
O mapeamento e os logs das bordas de resultado estão descritos abaixo; a validação do caso de quarentena permanece nas tasks.

### Monitoramento: política e configuração implementadas

O componente `br.gov.caixa.simtr.monitoramento` possui a política em `dominio.politica` e sua
configuração em `adaptador.configuracao`. `PoliticasMonitoramentoConfig` mapeia
`monitoramento.politicas.ativa` e o mapa `monitoramento.politicas.definicoes` de
`application.properties`. O producer CDI inicializa a política selecionada no bootstrap e rejeita
seleção ou definições inválidas, inclusive definições inativas.

A classe produtora usa `@Startup` e valida no construtor com injeção da configuração tipada.
O método `@Produces @Singleton` fornece a política já validada; o bootstrap não depende da
existência de um consumidor que a injete.

Os intervalos são ordenados e o último se repete; lista unitária produz intervalo fixo. Máximo de
tentativas é opcional e duração máxima é obrigatória. A política não usa `DeliveryCount`.
Quarkus reativo/Mutiny e CDI continuam permitidos no domínio e na aplicação; SDK e contratos de
borda não migram para o núcleo por essa permissão.

A extensão Quarkus Azure Service Bus e o Dev Services são exercitados em integração explícita
nas duas filas e no caminho REST → publicação inicial. O emulador conserva
`src/main/azure/servicebus-emulator/config.json`. O endpoint e a preparação local pela ACL já
funcionam; listeners e processamento das filas permanecem pendentes. O restante está no
[plano da feature](../../tasks/features/orquestrador-monitoramento-service-bus/plan.md).

### Monitoramento: consultas implementadas

As portas de saída `ConsultarPreValidacao` e `ConsultarSituacaoDossie` já possuem adapters
CDI funcionais. Ambas devolvem `Uni` com modelos próprios; a execução ocorre na assinatura,
sem bloqueio, retry ou log adicional. Essa colaboração concretiza os
[ADRs 0003](../adr/0003-orquestracao-e-colaboracao-por-portas.md) e
[0011](../adr/0011-composicao-local-monitoramento-e-fabrica-service-bus.md).

`PreValidacaoSimuladaAdapter` usa DTO/mapper exclusivos da borda, conforme
[ADR-0004](../adr/0004-contratos-independentes-por-borda.md), e retorna
`PreValidacaoConsultada(String situacao, boolean simulada)`. A situação não é vazia e a
origem simulada fica explícita. A flag `monitoramento.simulador.prevalidacao.habilitado`
é `false` por padrão; ativação explícita libera três cenários imutáveis:
`pre-em-analise → EM_ANALISE_ENVIO_MTR`, `pre-conforme → CONFORME` e
`pre-nao-conforme → NAO_CONFORME`. Desativação ou ausência falha, sem fallback ou resultado
funcional fictício; a flag não impede a inicialização das capacidades atuais do Hub.

`SituacaoDossieHubAcl` injeta somente a porta pública `ConsultarDossieProduto`.
Converte a string MTR para `IdentificadorDossieProduto(Long)`, aceita zeros à esquerda e
valida o intervalo positivo antes de chamar o Hub. A seleção MTR/simulador permanece no
fornecedor; não há acesso a HTTP local, Resource, DTO de borda ou implementação interna.

O resultado `SituacaoDossieConsultada(Integer id, String nome)` preserva id, inclusive nulo,
e nome original não vazio; não transporta data/matrícula nem normaliza ou classifica a
situação. Resposta/situação ausente ou nome vazio falha explicitamente. Falhas locais têm
texto fixo sem o valor rejeitado; falhas do Hub seguem pelo mesmo `Uni`, sem nova interpretação.
O mapeamento entre id/nome do Hub e estados conclusivos precisa ser confirmado antes do
processamento terminal; as consultas prontas não resolvem essa regra.

### Fluxo aprovado das duas filas

Os componentes novos são `orquestrador` e `monitoramento`; `br.gov.caixa.simtr.dossie`
e o Hub permanecem preservados. O orquestrador já publica a primeira tentativa na fila de entrada. O listener do
monitoramento acionará a aplicação/política para consultar as fontes, decidir no-op,
reagendar na entrada ou publicar resultado terminal/quarentena na saída. O listener do
orquestrador consumirá a saída e concluirá o demonstrador após o log do resultado.

A autenticação aprovada é connection string/SAS externa nos ambientes reais e connection string
do emulador via Dev Services em dev/test habilitados. O builder pertence à extensão Quarkus
Azure Service Bus; a fábrica técnica do ADR-0011 já cria os quatro clientes compartilhados.
Entra ID/SDK direto do ADR-0009 são históricos. O
[guia Service Bus](../guias/guia-service-bus-amqp-dossie.md) detalha fluxo, critérios,
contratos e configuração. O trecho inicial REST → entrada está funcional; o diagrama do fluxo
completo não implica que listeners e processamento estejam prontos. O publisher de resultado
foi implementado em 7.1-A; ainda não há caso de uso de processamento conectado a ele.

### Orquestrador: iniciação REST e publicação inicial implementadas

`br.gov.caixa.simtr.orquestrador` possui os tipos semânticos `SolicitacaoMonitoramento` e
`MonitoramentoIniciado` em `dominio.modelo`. A borda `adaptador.entrada.rest.v1` contém request,
response e mapper próprios, sem reutilizar DTOs do Hub ou do monitoramento. O request valida os
identificadores obrigatórios e o MTR como inteiro decimal no intervalo `1..9223372036854775807`,
com `@DecimalMin`/`@DecimalMax` e formato decimal. Preserva o JSON string, zeros à esquerda e a
tolerância Jackson existente a campos desconhecidos. O response contém somente os dois IDs
técnicos. `POST /simtr-hub/v1/monitoramentos-dossie` injeta a porta `IniciarMonitoramento` e
responde `202` somente após confirmação da publicação. O caso de uso gera UUIDs e instante no
servidor, obtém os parâmetros e monta a tentativa inicial 1. Reassinaturas do mesmo `Uni`
compartilham o resultado, sem novo envio; novas requisições são novas iniciações, sem idempotência
durável entre elas.

Validação mantém `400/ARVDOCP0001` pelo mapper global existente. Falhas da iniciação são
traduzidas localmente para `500/ARVDOCP9999`, com mensagem genérica e ID técnico, sem causa do
broker. O DTO de erro pertence à borda REST do orquestrador: mantém o formato existente sem
importar o DTO atualmente localizado no Hub. O Hub e sua exceção arquitetural permanecem intactos.

A composição dos parâmetros iniciais implementa o
[ADR-0011 aceito](../adr/0011-composicao-local-monitoramento-e-fabrica-service-bus.md):
`ObterParametrosMonitoramento` → `ParametrosMonitoramentoAcl` → `PrepararMonitoramento`.
O monitoramento recebe `iniciadoEm` e calcula limite/versão pela política CDI; a ACL traduz
para o record próprio do orquestrador. Não consulta fontes nem publica nessa colaboração.
O limite superior do identificador MTR foi aprovado e aplicado no request REST, compatível com
a conversão ao `Long` do Hub, agora implementada na ACL de consulta. O contrato REST permanece string.

### Clientes Service Bus e escolha do ambiente

Somente `ClientesServiceBus` injeta/configura o builder da extensão. Cria senders e receivers
assíncronos duradouros das duas filas, com qualifiers `FilaEntrada`/`FilaSaida`; os receivers
usam `PEEK_LOCK` e auto-complete desabilitado. Não inicia consumo ao criar os clientes.
A fábrica fecha todos em ordem inversa, inclusive após falha parcial, sem repetir fechamento.
O observer de shutdown usa `PLATFORM_AFTER`; futuros listeners devem cancelar suas assinaturas
antes dele. `@PreDestroy` usa a mesma rotina idempotente.

A fábrica acompanha a ativação da extensão por `IfBuildProperty` e exige connection string,
fornecida por Dev Services ou pelo ambiente externo. O publisher resolve o cliente qualificado
por `Instance` apenas ao enviar. Com extensão desabilitada, o Resource permanece registrado
e a publicação falha explicitamente. Não há fallback Entra ou cliente por mensagem.

Testes padrão (`mvn test`) não usam emulador nem fila Azure; extensão e Dev Services ficam
desabilitados. Mocks/stubs verificam parâmetros, envio, falhas, confirmação e lifecycle.
A integração com broker possui tag/profile explícitos: `mvn -Pservicebus-integration test`
executa somente os testes de emulador. Seu profile fixa `test` e reconstrói as fontes de
configuração antes do bootstrap, rejeitando conexão/namespace efetivos, inclusive por arquivo
ou `%test`, sem expandir expressões. Falha de leitura impede a execução com erro fixo sem causa.

O desenvolvedor escolhe emulador com `mvn quarkus:dev`, em sessão sem configuração externa,
ou filas Azure com `mvn quarkus:dev "-Dquarkus.profile=dev,azure"` e connection string SAS/
nomes de filas fornecidos pelo ambiente. Essa escolha não muda a execução padrão dos testes.
O profile Azure mantém Dev Services desabilitado e AMQP sobre WebSockets; versões,
credenciais e configuração do emulador permanecem conforme ADR-0010.

### Fila de entrada: contratos e publisher implementados

O orquestrador possui `TentativaMonitoramento` em seu domínio e um DTO v1 próprio em
`adaptador.saida.servicebus.dto`. Seu mapper monta o JSON e as propriedades AMQP da mensagem
de entrada, sem publicar, criar clientes ou agendar. O SDK fica restrito a essa borda.

O monitoramento possui outro `TentativaMonitoramento` e outro DTO v1, pertencentes ao próprio
componente. O mapper em `adaptador.entrada.servicebus` recebe corpo e propriedades como strings,
valida envelope, tipos JSON, campos obrigatórios, versão de schema, tentativa positiva, intervalo
MTR aprovado e coerência de datas antes de produzir o modelo interno. A falha de contrato é
tipada, sem SDK, payload ou causa do parser. Não representa execução de DeadLetter.

Identificadores e zeros à esquerda são preservados; campos desconhecidos não são incorporados.
`DeliveryCount` não participa da tentativa funcional. Prazo original e versão da política são
preservados, sem substituição pela configuração ativa. A compatibilidade ocorre por JSON, não
por compartilhamento de DTOs entre componentes. O publisher inicial reutiliza o mapper e o
sender compartilhado, concluindo seu `Uni` após confirmação e traduzindo falhas do SDK para
mensagem fixa sem causa externa. Não adiciona retry ou bloqueio. Listener, settlement e limites
operacionais de recebimento continuam dependentes das próximas etapas.

### Reagendamento: contrato preparado

`monitoramento.adaptador.saida.servicebus` possui mapper CDI e DTO v1 próprios.
A validação equivalente à entrada precede a serialização; inclui obrigatoriedade, limite MTR,
contador positivo e limite temporal posterior ao início. O JSON produzido é compatível com o
consumidor independente. Identificadores, zeros à esquerda, tentativa recebida, início, limite
e versão permanecem intactos. O envelope é determinístico e não contém agendamento.
Esse mapper não calcula a próxima tentativa, publica ou agenda mensagens.

### Resultado: duas bordas independentes

O produtor do monitoramento e o consumidor do orquestrador possuem modelos de resultado,
DTOs v1, mappers CDI e DTOs/helpers de erro próprios. O produtor monta JSON e envelope
determinístico; o consumidor rejeita envelope divergente, JSON inválido, tipos incorretos,
overflow, ausência dos campos já definidos e conclusão anterior ao início. IDs, zeros à
esquerda, sequência técnica e situações original/calculada permanecem separados e preservados.
Não há publicação, listener, settlement ou persistência nesse mapeamento.

Os erros usam os eventos `monitoramento.servicebus.resultado.falhou` e
`orquestrador.servicebus.resultado.falhou`, com códigos locais e RuntimeException própria
em cada borda. O consumidor diferencia parsing, envelope e contrato; o produtor diferencia
contrato e serialização. Logs incluem somente diagnóstico sanitizado e trace válido.
A validação local das duas bordas admite `situacaoMtr` nula e contador zero em QUARENTENA
antes de consulta. Contadores negativos são rejeitados; CONCLUSIVO exige pelo menos uma
tentativa e situação MTR CONFORME, NAO_CONFORME ou PENDENTE_INFORMACAO. A propriedade
auxiliar de validação não integra o JSON. O mapper não recalcula nem persiste as situações.

Os guardrails agora verificam isolamento de DTOs por componente/borda, domínio sem dependência
de aplicação, portas de entrada sem acesso a implementações/portas de saída e ACLs limitadas
às portas de entrada/modelos públicos do fornecedor. Fixtures positivas e negativas exercitam
as regras; a permissão de Quarkus/Mutiny/CDI continua protegida pelos testes existentes.

### Publicação de resultado implementada em 7.1-A

A porta `PublicarResultadoMonitoramento` resolve por CDI para
`MonitoramentoResultadoPublisher`. O adapter usa seu mapper e o sender compartilhado
`FilaSaida`; não cria clientes, classifica situações nem persiste transições. O `Uni`
é preguiçoso e compartilha uma publicação por invocação; só conclui após confirmação do
broker. Falhas síncronas/assíncronas do SDK são traduzidas para mensagem fixa sem causa
externa; a exceção própria do mapper é preservada.

Testes sem broker cobrem confirmação, falhas e composição CDI. Dois casos de integração
explícita provam a publicação de resultado conclusivo/quarentena e a leitura pelo contrato
independente do orquestrador. Nessa prova, Complete ocorre antes do cancelamento da
assinatura de recebimento. A evidência não representa processamento completo nem
atomicidade entre publicar a saída e concluir a entrada. O caso de uso e os listeners
continuam pendentes; ver [continuidade de 7.1](../../tasks/features/orquestrador-monitoramento-service-bus/continuidade-7-1.md).

### Estrutura inativa de orquestrador e monitoramento

Os packages definitivos também contêm a representação antecipada das capacidades planejadas:
11 portas de aplicação (sete com implementação conectada), dois records independentes de
parâmetros iniciais funcionais e oito classes ainda pendentes. Modelos de decisão, casos de uso
e adapters futuros possuem Javadoc com
responsabilidade e dependências previstas. As classes pendentes usam `@Vetoed`; não são beans,
não expõem endpoint e não executam operações. Referências `@see` descrevem ligações previstas,
sem constituir injeção ou implementação de interface.

Essa representação foi antecipada explicitamente para revisão da arquitetura com desenvolvedores.
Ela não constitui entrega das capacidades nem regra geral de criação de abstrações futuras.
Tipos vazios ainda precisam de campos e invariantes; os records de parâmetros já participam
do cálculo e tradução locais. Os contratos e a política funcionais permanecem preservados.

Testes verificam a inatividade no CDI e as fronteiras entre núcleo, bordas, SDK e infraestrutura,
com provas positiva de Quarkus/Mutiny/CDI e negativa de SDK no núcleo. As regras de acesso
público pelas ACLs já verificam a consulta funcional ao Hub; o isolamento de DTOs inclui a borda
do simulador de pré-validação. Novas provas positivas/negativas restringem o builder à fábrica
e o acesso à infraestrutura Service Bus às bordas correspondentes. Preservar essas regras.
O [guia de desenvolvimento](../../tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md)
mapeia cada arquivo para o item que implementará seu comportamento.

### Consumidor CDI local da consulta de documentos

`br.gov.caixa.simtr.dossie` é um package consumidor irmão de `br.gov.caixa.simtr.hub`, não um novo
domínio nem uma borda de rede. Seu bean `@ApplicationScoped` injeta a porta de entrada
`ConsultarDocumentosDossieProduto` e delega a ela os critérios, preservando o `Uni`, a lista, a
lista vazia e a falha. O CDI resolve `ConsultaDocumentosDossieProdutoObservabilidade`, que mantém o
caso de uso concreto e a porta de saída selecionada encapsulados no Hub.

```text
br.gov.caixa.simtr.dossie.ConsultaDocumentosDossieProduto
    -> porta de entrada ConsultarDocumentosDossieProduto
        -> wrapper observável existente
            -> caso de uso
                -> adapter MTR ou simulador selecionado
```

O guardrail importa o código de produção de todo `br.gov.caixa.simtr`. Para o package irmão, a
allowlist permite dependências no Hub somente para a porta de entrada de `dossieproduto` e para o
package de modelos semânticos usado por sua assinatura. Caso de uso concreto, porta de saída,
adapter, Resource e REST Client permanecem fora desse limite.

## Portas, casos de uso e colaboração

- cada capacidade atômica possui sua porta de entrada;
- casos de uso usam linguagem do negócio e tipos internos;
- portas de saída representam a necessidade do consumidor, não uma API genérica do fornecedor;
- um adapter MTR pode implementar várias portas pequenas do mesmo domínio;
- não existe `Service` genérico que exponha operações de todos os contextos.

Um futuro orquestrador do mesmo domínio pode compor portas de entrada atômicas. Ao atravessar um
domínio, usa uma porta de saída do consumidor e uma camada anticorrupção. Dentro do mesmo processo,
não chama endpoints REST locais.

## Contratos das bordas

### REST público

- DTOs pertencem ao adapter REST do domínio e da operação;
- paths, verbos, status, JSON e validações são contratos observáveis;
- OpenAPI é gerado pelo Quarkus a partir do código;
- o contrato técnico compartilhado de erro REST em `arquitetura.excecao.dto` é uma exceção
  arquitetural explícita e não pode vazar para domínio, aplicação, MTR, simulador ou MCP.

### MTR

- DTOs, mappers, REST Clients e annotations de fault tolerance pertencem ao adapter MTR;
- contratos são separados por versão e operação quando evoluem independentemente;
- falhas externas são traduzidas para falhas internas somente depois da política de fault
  tolerance.

A captura consome `POST /simtr/dossie-produto/v1/dossie-produto/{id}/capturar` sem corpo. Seu
REST Client mantém API key e OIDC, aplica timeout e circuit breaker, mas não aplica retry porque o
contrato externo não comprova idempotência. Um provider registrado somente nesse client suprime o
span HTTP automático que publicaria a URL interna completa e reinjeta o contexto usando o
propagador OpenTelemetry configurado; os demais REST Clients não são afetados.

A consulta de documentos consome
`GET /simtr/dossie-produto/v4/dossie-produto/{id}/documentos` sem corpo e encaminha os 12 filtros
opcionais somente quando informados. Por ser leitura idempotente, aplica timeout, retry apenas a
falhas transitórias e circuit breaker. Um provider exclusivo também suprime o span HTTP automático
que publicaria a query string e reinjeta o contexto corrente; o adapter publica um único span
CLIENT próprio e traduz a falha protocolar somente depois da política de fault tolerance.

### Simulador

- implementa as mesmas portas de saída do adapter MTR;
- usa DTO e mapper próprios para ler fixtures;
- não reutiliza DTO REST ou MTR;
- seleção MTR/simulador usa qualifiers ou producer CDI explícitos.

`CapturarDossieProduto` e `ConsultarDocumentosDossieProduto` reutilizam a property
`simtr-hub.simulador.dossie-produto.habilitado` em producers explícitos para selecionar seus
adapters MTR ou simulador. Cada capacidade mantém DTO, mapper e fixture próprios. A consulta de
documentos resolve o cenário determinístico do identificador `4081899`, não reproduz filtros ou
projeções do MTR e, como a captura, não aplica fault tolerance nem realiza chamada de rede no modo
simulador.

### MCP futuro

MCP pode ser uma nova borda de entrada para portas existentes. Não é domínio, regra de negócio ou
atalho para Resources REST/adapters de saída. DTOs, schemas, autorização, transporte e erros são
exclusivos dessa borda. Nenhum componente MCP está implementado ou autorizado apenas por estar
descrito aqui.

## Erros

- exceções HTTP, MCP e tipos de protocolo não atravessam portas;
- cada domínio classifica falhas relevantes para seus casos de uso;
- o adapter MTR preserva dados necessários à resposta pública sem transportar seu DTO até REST;
- o adapter REST traduz falhas internas para o status e corpo públicos;
- validação e desserialização anteriores ao Resource permanecem em mappers técnicos REST;
- stack, URL interna, token, credencial e estado de circuit breaker não são dados públicos.

## Assincronicidade e chamadas bloqueantes

`Uni` representa operações assíncronas com zero ou um resultado. Casos de uso não chamam `await`,
não bloqueiam event loop e não criam threads. Adapters bloqueantes deslocam o trabalho para worker
thread sem expor esse detalhe ao domínio.

## Fault tolerance e idempotência

Timeout, retry, circuit breaker e classificação de exceções pertencem ao adapter MTR. As políticas
não são aplicadas automaticamente ao simulador.

Criação de dossiê, inclusão de documento, alteração de produtos contratados, captura e avanço de
workflow são operações mutáveis. Antes de um workflow, orquestrador ou agente repetir essas
operações, deve existir evidência de idempotência do MTR ou uma estratégia/chave idempotente
aprovada. Sem essa evidência, a composição mutável fica bloqueada.

## Observabilidade e segurança

- spans, eventos de log e atributos existentes são comportamento observável;
- renomes Java não podem alterar silenciosamente nomes derivados por reflexão;
- novas entradas preservam correlação até o MTR e identificam sua origem;
- tokens, credenciais, argumentos sensíveis, URLs internas e payloads protegidos não aparecem em
  respostas, logs, traces, relatórios ou memória de conversa;
- exposição de `ObterCredencialContainer` a agentes exige decisão de segurança própria.

`ConsultarDocumentosDossieProduto` publica os spans
`simtr-hub.api.dossie-produto.documentos.consultar` (SERVER),
`simtr-hub.service.dossie-produto.documentos.consultar` (INTERNAL) e, somente no modo MTR,
`mtr.dossie-produto.documentos.consultar` (CLIENT). Seus sinais registram rota parametrizada,
versão v4, origem, flag do simulador, identificador, quantidade e tipo técnico de erro, sem query
string, filtros, payload, identidade, URL de documento, path de storage ou credenciais.

## Estratégia de testes e evolução

Uma feature segue fatias verticais pequenas:

1. caracterizar o comportamento atual relevante;
2. escrever ou ajustar testes que provem a mudança pretendida;
3. implementar o menor incremento coerente;
4. executar testes focados;
5. executar suíte, build e checkpoint Sonar conforme o guia de agentes.

Conforme a mudança, os testes cobrem contrato HTTP/JSON, Jakarta Validation, mapeamentos, payload
MTR, simulador, erros, fault tolerance, configuração, observabilidade e regras ArchUnit. Mudanças de
contrato, arquitetura, segurança ou comportamento observável exigem checkpoint humano adicional.
O OpenAPI é gerado pelo Quarkus a partir das annotations e contratos Java; os testes não mantêm
snapshot nem inspecionam o documento gerado, conforme o ADR-0006.

O escopo ArchUnit de produção abrange todo `br.gov.caixa.simtr`, inclusive packages consumidores
irmãos. A fronteira de `br.gov.caixa.simtr.dossie` possui verificação positiva sobre o código real e
prova negativa que rejeita dependência no caso de uso concreto.

## Restrições vigentes

- o Hub não faz upload para Azure Blob Storage;
- não mantém cache nem renova SAS;
- não possui workflow ou orquestrador local;
- não possui MCP Server ou tools;
- não possui persistência de estado de fluxo;
- não calcula árvore documental nem executa análise de conformidade;
- não implementa os dois endpoints ausentes listados acima.

Essas restrições descrevem o estado atual, não uma proibição permanente. Uma feature pode mudá-las
somente com requisitos explícitos, análise de impacto, plano, testes e GO humano.

## Decisões arquiteturais

Consulte [doc/adr/README.md](../adr/README.md) para o resumo e a aplicabilidade de cada decisão. O
índice é parte da leitura inicial; o texto completo de um ADR é leitura sob demanda.
