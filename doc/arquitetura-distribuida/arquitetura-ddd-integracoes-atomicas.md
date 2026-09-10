# Arquitetura atual do simtr-hub

## Como usar este documento

- **Status:** aceito
- **Última consolidação:** 2026-09-10
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
seleciona o adapter. As capacidades do Hub continuam atômicas. No mesmo runtime, os packages
irmãos `orquestrador` e `monitoramento` implementam a demonstração assíncrona descrita abaixo:
POST, entrada Service Bus, processamento, reagendamento/publicação e consumo/log da saída.
Ambos os listeners têm ativação independente por configuração; a prova funcional integrada
até log/Complete e DLQ está implementada em 9.1-C com emulador e Hub controlado.
Não existem endpoint único de pré-validação, motor de workflow durável,
MCP Server ou implantação desses componentes como microsserviços separados.

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

Além das doze capacidades do Hub, a borda do package `orquestrador` expõe
`POST /simtr-hub/v1/monitoramentos-dossie` para iniciar a demonstração de monitoramento.
Esse endpoint publica a primeira tentativa e responde 202 após confirmação do broker.

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

O producer também fornece `CatalogoPoliticasMonitoramento`, imutável e indexado por versão.
A definição recebida tem precedência, inclusive inativa; versões repetidas falham no bootstrap.
Na ausência da definição, o catálogo resolve para v1 interna com intervalo único PT30M,
repetido até o prazo, duração PT24H e máximo de tentativas ausente. A configuração padrão
em application.properties também usa PT30M. A resolução não altera propriedades ou seleção ativa. A resolução expõe versão solicitada, política efetiva e indicador de padrão aplicado.

A recuperação por valores padrão está definida no
[ADR-0011](../adr/0011-composicao-local-monitoramento-e-fabrica-service-bus.md).
Não produz quarentena por ausência de versão. O chamador preserva os dados da tentativa e
usa o prazo recebido; os padrões não reabrem uma janela de 24 horas. Configuração presente
inválida continua rejeitada. O catálogo e sua composição CDI estão implementados e testados;
o caso de uso de processamento resolve e aplica esse catálogo em 7.1-B.

Os intervalos são ordenados e o último se repete; lista unitária produz intervalo fixo. Máximo de
tentativas é opcional e duração máxima é obrigatória. A política não usa `DeliveryCount`.
Quarkus reativo/Mutiny e CDI continuam permitidos no domínio e na aplicação; SDK e contratos de
borda não migram para o núcleo por essa permissão.

A extensão Quarkus Azure Service Bus e o Dev Services são exercitados em integração explícita
nas duas filas e no caminho REST → publicação inicial. O emulador conserva
`src/main/azure/servicebus-emulator/config.json`. O endpoint e a preparação local pela ACL já
funcionam; o processamento por porta e o listener da entrada estão implementados, com consumo
iniciado explicitamente ou por opt-in de configuração no startup (8.2). O restante está no
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
Os nomes originais informados pelo usuário já são aceitos nas duas bordas de resultado,
sem normalização ou IDs inventados. As consultas prontas e essa validação não executam
a classificação terminal, que pertence ao caso de uso de processamento implementado em 7.1-B.

### Fluxo aprovado das duas filas

Os componentes novos são `orquestrador` e `monitoramento`; `br.gov.caixa.simtr.dossie`
e o Hub permanecem preservados. O orquestrador já publica a primeira tentativa na fila de entrada. O listener do
monitoramento, iniciado explicitamente ou por opt-in no startup, aciona a aplicação/política para consultar as fontes,
decidir no-op, reagendar na entrada ou publicar resultado terminal/quarentena na saída.
O reagendamento e o Complete da entrega atual compartilham a transação da mesma fila.
O listener do orquestrador consome a saída quando habilitado e executa Complete depois da
submissão do log do resultado, sem garantia de escrita no destino.

A autenticação aprovada é connection string/SAS externa nos ambientes reais e connection string
do emulador via Dev Services em dev/test habilitados. O builder pertence à extensão Quarkus
Azure Service Bus; a fábrica técnica do ADR-0011 já cria os quatro clientes compartilhados.
Entra ID/SDK direto do ADR-0009 são históricos. O
[guia Service Bus](../guias/guia-service-bus-amqp-dossie.md) detalha fluxo, critérios,
contratos e configuração. O trecho inicial REST → entrada está funcional; o diagrama do fluxo
completo inclui o consumo da saída implementado e verificado no emulador em 9.1-C. O publisher de resultado
foi implementado em 7.1-A, é acionado pelo caso de uso de 7.1-B e pelo listener de 7.1-C;
a integração terminal local foi verificada em 7.1-D.

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
O observer de shutdown usa `PLATFORM_AFTER`; o listener da entrada cancela sua assinatura
em `PLATFORM_BEFORE`. O listener da saída usa a mesma prioridade e preserva essa ordem.
`@PreDestroy` usa a mesma rotina idempotente.

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
tentativa e aceita os nomes originais FINALIZADO_CONFORME, FINALIZADO_INCONFORME e
PENDENTE_INFORMACA, preservados literalmente. CONFORME, NAO_CONFORME e PENDENTE_INFORMACAO
continuam aceitos por compatibilidade com o contrato publicado. A propriedade auxiliar
de validação não integra o JSON. O mapper não recalcula nem persiste as situações; a
classificação funcional é executada pelo caso de uso de 7.1-B.

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
atomicidade entre publicar a saída e concluir a entrada. O caso de uso está conectado por
CDI em 7.1-B e o listener da entrada em 7.1-C; a 8.2 permite ativação por configuração no startup. Ver [continuidade de 7.1](../../tasks/features/orquestrador-monitoramento-service-bus/continuidade-7-1.md).

### Caso de uso de processamento implementado em 7.1-B

A porta ProcessarTentativaMonitoramento recebe a tentativa e inputSequenceNumber escalar.
O caso de uso consulta a pré-validação primeiro; fora de EM_ANALISE_ENVIO_MTR devolve Ignorar.
Se elegível, resolve a versão recebida e verifica o prazo original e a quantidade já realizada
(tentativaAtual - 1), sem consultar o Hub quando esgotados. A consulta de limites aceita zero;
max-tentativas=1 permite a primeira consulta e encerra após ela se não conclusiva.

O Hub é classificado pelos nomes exatos: FINALIZADO_CONFORME -> CONFORME,
FINALIZADO_INCONFORME -> INCONFORME e PENDENTE_INFORMACA -> INCONFORME.
O resultado preserva a situação MTR, IDs, início e sequência; situação calculada não comprova
persistência. Uma consulta iniciada no prazo pode concluir terminal após o prazo.
Para resposta não conclusiva, o caso de uso relê o relógio e verifica prazo/contador novamente.

DecisaoProcessamento distingue Ignorar, ResultadoPublicado e ReagendamentoPendente.
ResultadoPublicado só é emitido após a confirmação da porta de publicação. Quarentena anterior
ao Hub usa contador anterior e MTR ausente; posterior ao Hub usa tentativaAtual e MTR consultado.
O motivo é PRAZO_MAXIMO ou MAXIMO_TENTATIVAS; falta de versão continua usando v1 padrão.
O Uni é adiado e memorizado por invocação; falhas propagam sem retry adicional.

ReagendamentoPendente contém a tentativa original, contador/intervalo calculados e instante
da avaliação. PoliticaAplicada registra versões solicitada/efetiva e indicador de padrão,
sem transportar a estratégia executável. Essa decisão não agenda nem conclui entregas.
ReagendamentoMonitoramento transforma a decisão em próxima tentativa e horário limitado
ao prazo original. O listener associa a entrega ao adapter transacional, mantendo o SDK na borda.
O consumo da saída está conectado ao caso de uso de recebimento e ao log, com opt-in próprio.

### Listener da entrada implementado em 7.1-C

MonitoramentoEntradaListener é um bean CDI com início explícito por iniciar() e, desde 8.2,
ativação por configuração no startup. O observer StartupEvent chama iniciar() quando
monitoramento.service-bus.entrada.consumo-habilitado=true (default e %test=false).
O opt-in não muda emulador/Azure, credenciais ou simulador. Usa o receiver FilaEntrada
da fábrica em PEEK_LOCK, sem auto-complete
e com prefetchCount(0). concatMap com prefetch zero processa uma entrega por vez e aguarda
o settlement antes de solicitar a próxima. SDK e handles permanecem na borda.

Ignorar registra a decisão e executa Complete. ResultadoPublicado executa Complete após
a confirmação da publicação pelo caso de uso. Contrato inválido identificado pelo mapper
gera DeadLetter com motivo/descrição fixos e opções próprias por entrega. Falha técnica
anterior ao settlement gera Abandon. Falha de qualquer settlement encerra a assinatura;
não há segunda tentativa de liquidação nem reinício automático.

ReagendamentoPendente executa a porta associada à entrega atual. O commit confirma o
agendamento e o Complete juntos; não existe Complete simples adicional nesse ramo.
O consumo permanece inativo sem opt-in. Falha síncrona ao obter cliente impede o startup
com diagnóstico sanitizado; falha assíncrona mantém HTTP ativo e encerra a assinatura,
exigindo reinício da aplicação. Não foi acrescentado retry nem readiness do listener.
A prova de 8.2 usa startup configurado e POST até resultado/reagendamento no emulador,
sem iniciar() no teste e sem consumir a entrada pelo harness.
O listener admite uma única inicialização e cancela sua assinatura idempotentemente no
shutdown, antes da fábrica fechar os clientes. O cancelamento é best effort: interromper
a cadeia local não comprova a interrupção de uma publicação remota já iniciada.

Logs mínimos usam os eventos aprovados de decisão, falha de processamento e settlement,
com campos locais constantes, sem corpo, identificadores externos ou Throwable. Propagação
e caracterização completa do SDK permanecem em 10.1. Testes sem broker cobrem ordem,
falhas, concorrência de lifecycle, inatividade automática e JSON sanitizado. A integração
terminal com emulador foi verificada em 7.1-D. Publicação da saída e Complete ainda não são atômicos.

### Reagendamento transacional implementado em 8.1

ReagendamentoMonitoramento preserva IDs, iniciadoEm, limiteEm e versão recebida ao construir
a próxima tentativa. O horário é o menor entre processadoEm + intervalo e limiteEm, comparando
a duração restante antes de somar. A entrega no prazo publica QUARENTENA/PRAZO_MAXIMO sem
nova consulta ao Hub. max-tentativas, quando configurado, também encerra sem novo agendamento;
o maior inteiro representável é verificado antes do incremento. O padrão mantém PT30M/PT24H
sem teto operacional configurado. Resultado conclusivo da última consulta continua conclusivo.

MonitoramentoReagendamentoAdapter não guarda entrega no singleton. associar(receiver, mensagem)
devolve uma implementação local de ReagendarTentativaMonitoramento com handles capturados
somente na borda. Serializa pelo mapper próprio antes de abrir transação; o receiver cria
o contexto, o sender FilaEntrada agenda e o mesmo receiver executa Complete com esse contexto.
Falhas antes do commit tentam rollback; falha de commit não tenta rollback nem outra liquidação.
Qualquer falha transacional encerra a assinatura, mantendo recuperação/redelivery a cargo do broker.

Cada invocação compartilha o mesmo CompletableFuture, com a espera fora da memorização.
Cancelar alcança a operação pendente do SDK e uma nova assinatura não repete a transação.
Assinantes da mesma invocação compartilham o cancelamento. Na fronteira do commit, cancelar
a espera não prova reversão remota; não há rollback ou Complete/Abandon adicional.

A prova no emulador valida commit, rollback com nova entrega e ausência de ativação posterior,
além da perda controlada de confirmação local após commit real. Não simula falha real de rede.
As verificações de efeitos usam peek com cursor explícito antes de cancelar a assinatura.
Uma mensagem agendada ganha nova sequência ao ser ativada; rollback não promete incremento
de DeliveryCount. O fluxo CDI real também verifica progressão/repetição, teto e prazo original.
A suíte padrão continua sem broker. O consumo da saída é verificado separadamente em 9.1-C;
Azure gerenciado permanece sem validação.

### Integração terminal verificada em 7.1-D

A prova opt-in do emulador percorre REST → publicação da entrada → listener CDI iniciado
explicitamente → caso de uso/catálogo → ACL → publicação da saída. A pré-validação usa
o simulador existente habilitado apenas no profile do teste; somente a porta pública
ConsultarDossieProduto é controlada para fornecer os três nomes originais, sem IDs inventados.
O resultado é lido e validado pelo mapper independente do orquestrador.

Os cenários também verificam no-op, prazo recebido expirado, versão removida com recuperação
v1, política inativa com max=1, falha transitória com Abandon/redelivery e contrato inválido
na DLQ. IDs, situação MTR, contador funcional e sequência da entrada são preservados.
A prova de redelivery mantém a segunda consulta pendente para conferir mesma sequência/corpo,
crescimento de DeliveryCount e ausência de saída antecipada; só então libera a resposta terminal.

Cada cenário começa com filas do emulador vazias, usa identidades próprias e destrói apenas
o listener contextual ao terminar. Peek com sequência explícita comprova a remoção da entrada
sem confundir avanço de cursor com settlement. O teste conclui saída/DLQ antes de cancelar
a recepção. O profile reaproveita a proteção de configuração externa de C2-R1.
Nenhum caminho de produção foi alterado para realizar esta prova.

Essa evidência local não valida Azure gerenciado, perda de conexão durante publicação,
atomicidade entre output/Complete ou consumo automático da saída; este último tem prova própria em 9.1-C.
A prova adicional do reagendamento de 8.1 está descrita acima.
Desde 8.2, a aplicação pode iniciar automaticamente o listener da entrada por configuração
explícita, desabilitada por padrão; o dev escolhe emulador ou Azure. A suíte padrão permanece
sem broker. Execuções e checkpoint ficam nas tasks da feature.

### Recebimento e registro local do resultado

A porta ReceberResultadoMonitoramento resolve para ReceberResultadoMonitoramentoUseCase,
que delega a RegistrarResultadoMonitoramento e espera sua conclusão. ResultadoMonitoramentoLogAdapter
implementa a saída com o logger padrão. Cada invocação é lazy e compartilha a conclusão entre
assinantes; invocações distintas podem produzir novo registro, sem idempotência durável.

O evento INFO orquestrador.monitoramento-dossie.resultado.registrado mantém os campos textuais
evento, camada, componente, operacao, monitoramento_id e orquestracao_id no objeto mdc do JSON.
ExtLogRecord captura o contexto corrente e acrescenta IDs somente à cópia daquele registro.
Não emite payload, Throwable, status/motivo ou identificadores de negócio. O formatter original
é preservado; a categoria do adapter de log não usa a composição tipada de erros do ADR-0012.

A conclusão da porta representa submissão ao pipeline de logging, sem confirmação de escrita.
O consumo da saída executa Complete após essa submissão; falhas internas ou
filtros podem perder o log sem provocar Abandon/reentrega. Falhas anteriores propagadas pela
porta continuam sendo falhas da operação. Não há atomicidade entre logging e settlement.
O listener da saída aciona esse caso de uso quando habilitado; a integração com emulador foi verificada em 9.1-C.

### Listener da saída e guardrails

MonitoramentoResultadoListener recebe o cliente @FilaSaida pela fábrica e usa o mapper próprio
antes de chamar ReceberResultadoMonitoramento. O consumo inicia explicitamente ou no startup
quando monitoramento.service-bus.saida.consumo-habilitado=true; default false, independente da
flag da entrada. Cada instância inicia uma vez; conclusão ou falha da fonte impede reinício.

O fluxo serial espera a porta de recebimento e então executa Complete. Somente contrato
rejeitado pelo mapper vai à DLQ, com MONITORAMENTO_SAIDA_INVALIDA e descrição fixa.
Falhas técnicas da leitura/porta produzem Abandon, sem alterar o resultado na reentrega.
Falha de qualquer settlement encerra a assinatura sem executar outro; falha da fonte também
encerra o consumo. HTTP pode continuar ativo, sem readiness específica nem retry da aplicação.
Shutdown cancela a assinatura antes da fábrica; o listener não fecha clientes.

O erro orquestrador.monitoramento-dossie.resultado.falhou contém somente constantes técnicas
e não transporta Throwable, payload ou identificadores não validados. Contrato inválido
mantém o erro próprio já emitido pelo mapper, sem segundo erro de processamento.

As onze portas de aplicação estão conectadas, incluindo reagendamento por entrega e
recebimento/registro. O último esqueleto foi implementado: não restam classes @Vetoed nesses
dois componentes. ComponentesResultadoMonitoramento e ResultadoMonitoramentoCdiTest verificam
a resolução única dos três beans de resultado sem endpoint adicional.
As fronteiras de núcleo, bordas, SDK/fábrica e ACLs continuam protegidas por ArchUnit.
A prova integrada de ativação da saída e log final no emulador está implementada e verificada em 9.1-C.

### Integração da saída verificada em 9.1-C

O profile opt-in inicia os dois listeners pelo startup, sem chamada explícita no teste.
A prova percorre REST → entrada → processamento/publicação → saída → mapper → caso de uso
→ log real, conferindo depois a remoção das mensagens por Complete. Somente a porta pública
do Hub tem respostas controladas; consultas pendentes permitem observar por peek as sequências
reais e a progressão das tentativas antes de liberar conclusão ou reagendamento.

Os cenários cobrem terminal direto, reagendamento até conclusão e até o máximo de três
consultas, além de contrato inválido na DLQ da saída. Após a DLQ, um novo POST válido confirma
que o listener principal continua consumindo. A prova da subfila aguarda a mensagem própria
visível por peek e a ausência na principal antes de receber/concluir a DLQ. Não acrescenta
retry de settlement nem consumidor concorrente na saída principal.

O teste valida o JSON final e os IDs em mdc, sem payload. Cada cenário começa com filas
vazias; o receiver auxiliar pertence somente à DLQ da prova e é fechado pelo teste.
Reutiliza a proteção contra conexão externa do profile existente. Defaults de produção
permanecem false, com flags independentes. Esse resultado local não confirma escrita durável
do log, atomicidade log/Complete, idempotência ou comportamento no Azure gerenciado.
Falhas técnicas/Abandon permanecem caracterizadas sem broker; telemetria e revisão geral
de sinais/redelivery são etapas seguintes. Execuções, diagnóstico da DLQ e checkpoint ficam nas tasks.

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
- o Hub não possui workflow durável; a demonstração local nos packages irmãos possui orquestração
  de monitoramento com consumo/log da saída implementado e prova funcional integrada no emulador;
  correlação/telemetria e revisão final da demonstração permanecem nas etapas seguintes;
- não possui MCP Server ou tools;
- não possui persistência de estado de fluxo;
- não calcula árvore documental nem executa análise de conformidade;
- não implementa os dois endpoints ausentes listados acima.

Essas restrições descrevem o estado atual, não uma proibição permanente. Uma feature pode mudá-las
somente com requisitos explícitos, análise de impacto, plano, testes e GO humano.

## Decisões arquiteturais

Consulte [doc/adr/README.md](../adr/README.md) para o resumo e a aplicabilidade de cada decisão. O
índice é parte da leitura inicial; o texto completo de um ADR é leitura sob demanda.
