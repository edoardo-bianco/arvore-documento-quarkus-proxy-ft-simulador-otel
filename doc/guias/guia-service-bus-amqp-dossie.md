# Guia para integrar o dossiê ao Azure Service Bus com Azure SDK

## Estado do documento

- **Status:** correções C2.2 aplicadas; nova revisão técnica C2.3 pendente;
- **Escopo desta revisão:** conteúdo dos itens 2.1 a 2.11 e decisões de segurança aprovadas em C2.1;
- **Plataforma aprovada:** Quarkus `3.33` LTS, Azure SDK Java e Microsoft Entra ID;
- **Decisão arquitetural:** [ADR-0009](../adr/0009-azure-sdk-service-bus-dossie.md), aceito;
- **Evidência executável:** `UNVERIFIED_IN_ENVIRONMENT`;
- **Próximo incremento:** repetir a revisão técnica do guia e apresentar o checkpoint C2.3.

Este documento foi elaborado incrementalmente conforme o
[plano da feature](../../tasks/features/guia-service-bus-amqp-dossie/plan.md). O conteúdo previsto
está completo e as correções aprovadas em C2.1 foram incorporadas, mas a nova revisão C2.3, o
handoff da implementação e a aceitação final permanecem pendentes. Esta versão não é uma receita
executável e não deve ser usada para produção.

## Objetivo, leitor e limite do recorte

Este guia será um roteiro técnico para uma pessoa desenvolvedora experiente integrar o package
`br.gov.caixa.simtr.dossie` a duas filas do Azure Service Bus:

- publicar uma mensagem JSON mínima na fila de entrada; e
- consumir continuamente a fila de saída quando o broker entregar uma mensagem, sem polling.

O guia ensinará a borda de transporte aprovada, sem implementar código no repositório. Ele não
provisionará Azure, não implementará o processor que monitora a fila de entrada, não criará regra
de negócio ou workflow e não apresentará os gates técnicos pendentes como evidência de
funcionamento.

A análise funcional mais ampla permanece como referência de contexto em
[sincronização assíncrona dos dossiês](../feat/sincronizacao-dossies-mtr-pre-validacao-service-bus-processamento-paralelo.md),
mas somente o recorte aprovado no plano pertence a este guia.

## Decisões de partida

O checkpoint C1 aprovou o contrato mínimo, Azure SDK BOM documental, Microsoft Entra ID, modelo de
consumo, RBAC, settlement, concorrência inicial, health, intervalos progressivos e prazo máximo
individual contado desde o envio ao MTR. As decisões detalhadas e os gates ainda pendentes estão
registrados no [plano](../../tasks/features/guia-service-bus-amqp-dossie/plan.md) e no
[checklist](../../tasks/features/guia-service-bus-amqp-dossie/todo.md).

O aceite arquitetural não substitui resolução de dependências, compilação, autenticação, conexão
AMQP sobre WebSockets/TLS, teste com filas não produtivas ou validação transacional.

## Topologia e direção das duas filas

O recorte usa um namespace Azure Service Bus e duas filas com responsabilidades diferentes:

```text
br.gov.caixa.simtr.dossie
    |-- ServiceBusSenderAsyncClient -- send --> [fila de entrada]
    `-- ServiceBusProcessorClient  <-- receive-- [fila de saída]

[fila de entrada]
    -> processor de monitoramento fora deste guia
        -> [fila de saída]
```

Para o package `dossie`, as direções são invariantes:

| Entidade | Papel do package `dossie` | Papel RBAC mínimo |
|---|---|---|
| fila de entrada | somente publicar | `Azure Service Bus Data Sender` |
| fila de saída | somente receber e concluir | `Azure Service Bus Data Receiver` |

O exemplo não lê a fila de entrada, não produz resultados na fila de saída e não implementa o
processor intermediário. O futuro processor de retry terá identidade separada e os papéis Data
Receiver e Data Sender somente na fila de entrada; esse fluxo permanece uma extensão documental.

O nome usado por `queueName(...)` é relativo ao namespace. O endereço do namespace será o FQDN
`<namespace>.servicebus.windows.net`, sem path de fila e sem credencial embutida. Essa separação
segue o [modelo de nós AMQP do Service Bus](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-amqp-protocol-guide).

## Pré-condições de Azure, rede e autenticação

Antes de tratar qualquer snippet deste guia como executável, o ambiente não produtivo deverá
atender a todas as pré-condições abaixo:

| Área | Pré-condição | Evidência futura esperada |
|---|---|---|
| namespace | um namespace não produtivo, com FQDN conhecido e tier Standard ou Premium | recurso identificado sem connection string |
| filas | fila de entrada e fila de saída existentes no mesmo namespace | nomes relativos fornecidos por configuração externa |
| identidade | identidade gerenciada atribuída pelo sistema habilitada no compute Azure | principal identificado por canal seguro |
| autorização | Data Sender somente na entrada e Data Receiver somente na saída | assignments no menor escopo de entidade |
| rede | resolução DNS e saída TCP `443` para AMQP sobre WebSockets/TLS, HTTPS e autenticação | teste de conectividade e handshake WebSocket sem publicar segredo |
| TLS | trust store capaz de validar o certificado do namespace | handshake real bem-sucedido |
| prova | massa e filas descartáveis para envio/recepção controlados | mensagem de teste sem dado real ou sensível |

O tier Basic não atende ao desenho completo porque não suporta transações; Standard e Premium
suportam a extensão `agendar próxima + Complete atual`. A escolha entre Standard e Premium é
operacional e de capacidade, não será inferida por este guia. Consulte a
[visão de tiers](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-premium-messaging)
e a [documentação de transações](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-transactions).

Uma emenda do C1 escolheu AMQP 1.0 sobre WebSockets/TLS. A Microsoft documenta que esse transporte
usa TCP `443`, é funcionalmente equivalente ao AMQP direto para o Service Bus e possui maior
latência inicial e pequeno overhead adicional de handshake. A receita não exige `5671` e não faz
fallback automático para AMQP/TCP. A futura prova deverá confirmar também que firewall ou
intermediário de rede permite o handshake WebSocket. Consulte a
[FAQ de portas do Service Bus](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-faq#what-ports-do-i-need-to-open-on-the-firewall).

A identidade gerenciada evita segredo no código, mas ainda exige RBAC. A atribuição deve ser feita
na fila sempre que o ambiente suportar esse escopo, não no subscription, resource group ou
namespace por conveniência. A propagação de um assignment pode levar alguns minutos. Consulte
[managed identity e RBAC no Service Bus](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-managed-service-identity).

Autenticação local/SAS só deverá ser desabilitada depois de a autenticação Entra ID funcionar no
ambiente não produtivo. Namespace, nomes das filas e identidade serão entregues por configuração;
token, client secret, chave SAS e connection string não pertencem ao repositório, ao guia ou ao
chat.

## Versões validadas documentalmente

O estado observado no projeto e a matriz aprovada no C1 são:

| Componente | Versão | Estado da evidência |
|---|---:|---|
| Quarkus | `3.33.2.1` | confirmado no `pom.xml` atual |
| Java | `25` | confirmado por `maven.compiler.release` |
| SmallRye Mutiny | `3.1.1` | gerenciado pelo BOM Quarkus; usado na ponte `Mono<Void>` -> `Uni<Void>` |
| SmallRye Reactive Messaging | `4.33.0` | gerenciado pelo BOM Quarkus; não será usado nesta borda |
| Azure SDK BOM | `1.3.8` | confirmado no POM/README oficial do BOM e aprovado no C1 |
| `azure-messaging-servicebus` | `7.17.19` | versão gerenciada pelo BOM `1.3.8` |
| `azure-identity` | `1.18.4` | versão gerenciada pelo BOM `1.3.8` |

O [POM oficial do Azure SDK BOM](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/boms/azure-sdk-bom/pom.xml)
é a fonte da matriz `1.3.8`/`7.17.19`/`1.18.4`. As páginas estáveis de API consultadas em
2026-08-31 exibiam [Service Bus `7.17.17`](https://learn.microsoft.com/en-us/java/api/overview/azure/messaging-servicebus-readme?view=azure-java-stable)
e [Identity `1.18.5`](https://learn.microsoft.com/en-us/java/api/com.azure.identity.defaultazurecredential?view=azure-java-stable);
essa divergência não autoriza misturar versões individuais. O BOM permanece a fonte documental e
a resolução Maven da futura feature será a prova efetiva.

A [documentação Microsoft para Maven](https://learn.microsoft.com/en-us/azure/developer/java/sdk/get-started-maven)
declara JDK 8 ou superior e recomenda uma versão LTS para produção. Isso não comprova
automaticamente a combinação específica Java `25` + Quarkus `3.33.2.1` + Azure SDK; ela permanece
`UNVERIFIED_IN_ENVIRONMENT` até compilação e testes.

## Azure SDK BOM e dependências Maven

O futuro `pom.xml` deverá manter o BOM Quarkus existente e acrescentar o BOM Azure. Os fragmentos
abaixo mostram somente as inclusões planejadas; não substituem os blocos atuais:

```xml
<!-- Dentro de <properties> -->
<azure.sdk.bom.version>1.3.8</azure.sdk.bom.version>

<!-- Dentro de <dependencyManagement><dependencies>, mantendo o BOM Quarkus -->
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-sdk-bom</artifactId>
    <version>${azure.sdk.bom.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>

<!-- Dentro de <dependencies>; sem versões individuais -->
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-messaging-servicebus</artifactId>
</dependency>
<dependency>
    <groupId>com.azure</groupId>
    <artifactId>azure-identity</artifactId>
</dependency>
```

No fragmento acima, Service Bus aparece antes de Azure Identity apenas como ordem de apresentação,
não como requisito de compatibilidade. O
[README oficial do cliente Java](https://learn.microsoft.com/en-us/java/api/overview/azure/messaging-servicebus-readme?view=azure-java-stable)
registra que o problema histórico de ordem foi resolvido em `azure-identity:1.2.1`, anterior à
versão `1.18.4` gerenciada pela matriz documental adotada.
O BOM gerencia versões, mas não adiciona bibliotecas automaticamente; cada artifact necessário
continua declarado em `<dependencies>`. Esse é o padrão documentado em
[Azure SDK com Maven](https://learn.microsoft.com/en-us/azure/developer/java/sdk/get-started-maven)
e no [README oficial do BOM](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/boms/azure-sdk-bom/README.md).

Não adicionar:

- versão individual em `azure-messaging-servicebus` ou `azure-identity`;
- `quarkus-messaging-amqp` para esta borda;
- override de SmallRye Reactive Messaging;
- biblioteca beta ou cliente Service Bus legado.

Na futura feature executável, `dependency:tree`, compilação e testes deverão verificar conflitos
de Azure Core, Reactor, Netty, Jackson e SLF4J. A própria Microsoft alerta que frameworks e SDKs
podem resolver versões únicas incompatíveis dessas dependências transitivas; consulte o
[diagnóstico de conflitos do Azure SDK](https://learn.microsoft.com/en-us/azure/developer/java/sdk/troubleshooting-dependency-version-conflict).
Nenhum desses comandos foi executado nesta branch documental.

## Contratos Java independentes e JSON de duas propriedades

As mensagens da fila de entrada e da fila de saída possuem, neste primeiro recorte, o mesmo wire
shape. Isso não as transforma no mesmo contrato: a primeira solicita o processamento iniciado
pelo package `dossie`; a segunda identifica a mensagem devolvida ao package `dossie`. Cada borda
deverá, portanto, ter seu próprio tipo Java e poderá evoluir separadamente.

O corpo JSON aprovado para as duas direções contém exatamente estas propriedades:

```json
{
  "idDossiePreValidacao": "c5a13bd2-fc7e-45cd-9c47-8a79b5926c86",
  "idDossieMtr": "123456789"
}
```

| Propriedade JSON | Tipo Java | Regra deste recorte |
|---|---|---|
| `idDossiePreValidacao` | `String` | identificador opaco; o exemplo semelhante a UUID não autoriza conversão automática para `UUID` |
| `idDossieMtr` | `String` | identificador opaco; não converter para número nem remover zeros à esquerda |

Status, tentativa, timestamp, versão, correlação e regra de negócio não pertencem ao body. Em
particular, `tentativaAtual` é a AMQP application property definida no item 2.8. Os demais
metadados técnicos do Service Bus também permanecem fora destes records.

### Posicionamento futuro dos tipos

Os arquivos ainda não existem. A futura feature de implementação deverá criá-los nas bordas que
possuem os dados, sem reutilizar DTO REST, MTR, simulador ou um tipo compartilhado:

```text
src/main/java/br/gov/caixa/simtr/dossie/
`-- adaptador/
    |-- saida/servicebus/dto/
    |   `-- MensagemEntradaSincronizacaoDossie.java
    `-- entrada/servicebus/dto/
        `-- MensagemSaidaSincronizacaoDossie.java
```

Os nomes `entrada` e `saida` dos records indicam as filas. A direção dos packages segue a
arquitetura: publicar na fila de entrada é um adaptador de saída; consumir da fila de saída é um
adaptador de entrada.

### Record exclusivo da fila de entrada

```java
package br.gov.caixa.simtr.dossie.adaptador.saida.servicebus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MensagemEntradaSincronizacaoDossie(
        @JsonProperty("idDossiePreValidacao") String idDossiePreValidacao,
        @JsonProperty("idDossieMtr") String idDossieMtr
) {
    private static final int LIMITE_IDENTIFICADOR_CARACTERES = 256;

    public MensagemEntradaSincronizacaoDossie {
        idDossiePreValidacao = validar("idDossiePreValidacao", idDossiePreValidacao);
        idDossieMtr = validar("idDossieMtr", idDossieMtr);
    }

    private static String validar(String nome, String valor) {
        if (valor == null || valor.isBlank()
                || valor.codePointCount(0, valor.length()) > LIMITE_IDENTIFICADOR_CARACTERES) {
            throw new IllegalArgumentException("Campo contratual inválido: " + nome);
        }
        return valor;
    }
}
```

### Record exclusivo da fila de saída

```java
package br.gov.caixa.simtr.dossie.adaptador.entrada.servicebus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MensagemSaidaSincronizacaoDossie(
        @JsonProperty("idDossiePreValidacao") String idDossiePreValidacao,
        @JsonProperty("idDossieMtr") String idDossieMtr
) {
    private static final int LIMITE_IDENTIFICADOR_CARACTERES = 256;

    public MensagemSaidaSincronizacaoDossie {
        idDossiePreValidacao = validar("idDossiePreValidacao", idDossiePreValidacao);
        idDossieMtr = validar("idDossieMtr", idDossieMtr);
    }

    private static String validar(String nome, String valor) {
        if (valor == null || valor.isBlank()
                || valor.codePointCount(0, valor.length()) > LIMITE_IDENTIFICADOR_CARACTERES) {
            throw new IllegalArgumentException("Campo contratual inválido: " + nome);
        }
        return valor;
    }
}
```

Records são apropriados aqui por representarem portadores transparentes de um conjunto fixo de
valores, com componentes finais e membros básicos gerados pela linguagem, conforme a
[documentação de records do Java 25](https://docs.oracle.com/en/java/javase/25/language/records.html).
O `@JsonProperty` explicita o nome externo de cada propriedade no próprio contrato, seguindo a
[documentação oficial do Jackson](https://github.com/FasterXML/jackson-annotations#annotations-for-renaming-properties).
O projeto já possui `quarkus-rest-jackson`; a futura serialização usará o `ObjectMapper` do
Quarkus, cuja configuração é descrita no
[guia JSON do Quarkus 3.33](https://quarkus.io/version/3.33/guides/rest-json#configuring-json-support).

`@JsonProperty` fixa o nome no wire; os construtores compactos aplicam a decisão C2.1: os dois
campos são obrigatórios, não nulos, não vazios e limitados a 256 caracteres Unicode. O cálculo por
`codePointCount(...)` evita contar um par substituto como dois caracteres. O valor não é
normalizado nem truncado; texto inválido falha com uma mensagem que contém apenas o nome público do
campo, nunca o identificador recebido. Consulte a
[API de `String.codePointCount(...)`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/String.html#codePointCount(int,int)).

A desserialização manual de `BinaryData` pelo `ObjectMapper` não aciona Jakarta Validation. Por
isso a validação permanece nos records de cada borda e a leitura da fila de saída habilitará
explicitamente `FAIL_ON_UNKNOWN_PROPERTIES`. Não será criado validador compartilhado entre os
adaptadores: a pequena duplicação preserva a propriedade de cada DTO por sua fila. Qualquer
evolução de campos, tamanho ou normalização do body exigirá atualização do plano e novo checkpoint
humano. Os snippets desta seção estão `DOCUMENTED; UNVERIFIED_BY_COMPILATION`.

## Configuração externa, builder compartilhado, AMQP WebSockets/TLS e Entra ID

Esta seção define a configuração-base dos futuros clientes, mas ainda não cria o sender nem o
Processor. Os arquivos e snippets são propostas para a feature executável e permanecem fora das
fontes desta branch documental.

### Três propriedades de topologia e uma operacional

O contrato aprovado contém três valores obrigatórios de topologia e o limite operacional de
concorrência do Processor. Nenhum deles é segredo:

| Propriedade Quarkus | Variável de ambiente | Formato esperado |
|---|---|---|
| `simtr-hub.dossie.service-bus.namespace` | `SIMTR_HUB_DOSSIE_SERVICE_BUS_NAMESPACE` | `<namespace>.servicebus.windows.net` |
| `simtr-hub.dossie.service-bus.fila-entrada` | `SIMTR_HUB_DOSSIE_SERVICE_BUS_FILA_ENTRADA` | nome relativo da fila de entrada |
| `simtr-hub.dossie.service-bus.fila-saida` | `SIMTR_HUB_DOSSIE_SERVICE_BUS_FILA_SAIDA` | nome relativo da fila de saída |
| `simtr-hub.dossie.service-bus.max-concurrent-calls` | `SIMTR_HUB_DOSSIE_SERVICE_BUS_MAX_CONCURRENT_CALLS` | inteiro positivo; valor inicial `1` |

O futuro `application.properties` deverá declarar os aliases sem valor real de topologia. Somente
a concorrência terá o default `1` aprovado no C1:

```properties
simtr-hub.dossie.service-bus.namespace=${SIMTR_HUB_DOSSIE_SERVICE_BUS_NAMESPACE}
simtr-hub.dossie.service-bus.fila-entrada=${SIMTR_HUB_DOSSIE_SERVICE_BUS_FILA_ENTRADA}
simtr-hub.dossie.service-bus.fila-saida=${SIMTR_HUB_DOSSIE_SERVICE_BUS_FILA_SAIDA}
simtr-hub.dossie.service-bus.max-concurrent-calls=${SIMTR_HUB_DOSSIE_SERVICE_BUS_MAX_CONCURRENT_CALLS:1}
```

O Quarkus `3.33` lê variáveis de ambiente com precedência maior que o `application.properties` e
converte pontos e hífens em sublinhados, conforme a
[referência de configuração 3.33](https://quarkus.io/version/3.33/guides/config-reference#environment-variables).
Não há valor padrão: expressão ausente deve impedir a inicialização em vez de selecionar um
destino implícito.

A interface futura agrupa essas propriedades e mantém todos os membros obrigatórios:

```java
package br.gov.caixa.simtr.dossie.adaptador.configuracao;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "simtr-hub.dossie.service-bus")
public interface DossieServiceBusConfig {

    String namespace();

    String filaEntrada();

    String filaSaida();

    int maxConcurrentCalls();
}
```

O naming strategy padrão converte `filaEntrada` e `filaSaida` para kebab-case. Membros não
opcionais exigem valor, como documentado em
[Config Mappings do Quarkus 3.33](https://quarkus.io/version/3.33/guides/config-mappings#config-mappings).
Antes de construir
qualquer cliente, a futura implementação deverá validar que `namespace` contém exatamente um
label de namespace seguido por `.servicebus.windows.net`, sem label adicional, scheme,
`https://`, `wss://`, porta, path, credencial ou trailing dot. Os dois nomes de fila devem ser
relativos e `maxConcurrentCalls` deve ser maior ou igual a `1`. O default `1` é a decisão
inicial do C1, não o default implícito do SDK adotado como requisito; a propriedade permanece
externalizada para ajuste futuro.

A forma estrutural é, sem diferença entre maiúsculas e minúsculas,
`[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.servicebus\.windows\.net`: um único label DNS não vazio,
sem ponto adicional e sem hífen nas extremidades. Regras operacionais adicionais do nome criado
no Azure continuam sujeitas à validação do ambiente; elas não ampliam esse sufixo público.

Esses valores não são segredos, mas identificam a topologia. Não registrar a configuração inteira,
o namespace, os nomes reais das filas ou a representação da interface em log.

### `TokenCredential` exclusivo por ambiente

Em runtime Azure, a identidade atribuída pelo sistema é selecionada explicitamente, sem client ID,
tenant, secret ou cadeia de fallback:

```java
import com.azure.core.credential.TokenCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;

TokenCredential credentialProducao = new ManagedIdentityCredentialBuilder().build();
```

O construtor sem `.clientId(...)` corresponde à identidade atribuída pelo sistema. A
[API de `ManagedIdentityCredentialBuilder`](https://learn.microsoft.com/en-us/java/api/com.azure.identity.managedidentitycredentialbuilder?view=azure-java-stable)
documenta esse caminho sem senha ou chave. A identidade do workload terá Data Sender somente na
fila de entrada e Data Receiver somente na fila de saída.

Somente em profile local/de desenvolvimento, o guia usará `DefaultAzureCredential`. A cadeia será
restrita a credenciais de ferramentas de desenvolvimento por uma variável não secreta:

```text
AZURE_TOKEN_CREDENTIALS=dev
```

```java
import com.azure.core.credential.TokenCredential;
import com.azure.identity.AzureIdentityEnvVars;
import com.azure.identity.DefaultAzureCredentialBuilder;

TokenCredential credentialDesenvolvimento = new DefaultAzureCredentialBuilder()
        .requireEnvVars(AzureIdentityEnvVars.AZURE_TOKEN_CREDENTIALS)
        .build();
```

`AZURE_TOKEN_CREDENTIALS=dev` existe desde Azure Identity `1.16.1`, e `requireEnvVars(...)` desde
`1.18.0`; portanto, ambas as APIs pertencem à matriz documental `1.18.4`. A
[documentação de cadeias do Azure Identity](https://learn.microsoft.com/en-us/azure/developer/java/sdk/authentication/credential-chains#exclude-a-credential-type-category)
explica a restrição. O principal de desenvolvimento deverá autenticar fora da aplicação, por uma
ferramenta suportada, e possuir os mesmos papéis mínimos nas filas não produtivas.

A futura configuração CDI deverá expor exatamente um `TokenCredential` por ambiente. Produção não
tentará `DefaultAzureCredential` se `ManagedIdentityCredential` falhar, e desenvolvimento não
usará identidade, tenant ou client secret materializados no repositório.

### Builder-base compartilhado

O método futuro recebe a configuração e a credencial já escolhida. Ele seleciona explicitamente
WebSockets; o default do SDK é AMQP/TCP e, portanto, não atende à emenda do C1:

```java
import com.azure.core.amqp.AmqpTransportType;
import com.azure.core.credential.TokenCredential;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;

static ServiceBusClientBuilder novoBuilderCompartilhado(
        DossieServiceBusConfig config,
        TokenCredential credential) {
    return new ServiceBusClientBuilder()
            .credential(config.namespace(), credential)
            .transportType(AmqpTransportType.AMQP_WEB_SOCKETS);
}
```

A [API de `ServiceBusClientBuilder`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusclientbuilder?view=azure-java-stable)
confirma as assinaturas `credential(String, TokenCredential)` e
`transportType(AmqpTransportType)`. A
[API de `AmqpTransportType`](https://learn.microsoft.com/en-us/java/api/com.azure.core.amqp.amqptransporttype?view=azure-java-stable)
distingue `AMQP_WEB_SOCKETS` de `AMQP` sobre TCP.

Uma única instância desse builder de topo deverá originar os clientes de longa duração para
compartilhar a conexão subjacente. Os próximos itens completarão os sub-builders sem inverter as
filas:

| Próximo cliente | Sub-builder | Entidade configurada |
|---|---|---|
| publisher da entrada | `sender()` | `queueName(config.filaEntrada())` |
| consumidor da saída | `processor()` | `queueName(config.filaSaida())` |

O ciclo de vida CDI, início, shutdown e health estão detalhados no item 2.7. Nenhum cliente deverá
ser criado por mensagem ou por callback.

### TLS, token e limites de configuração

O Azure Service Bus exige TLS em todos os transportes AMQP. Com
`AMQP_WEB_SOCKETS`, o SDK estabelece AMQP 1.0 dentro de WebSockets/TLS em TCP `443`; o FQDN passado
ao builder continua sem scheme. O trust store deve validar a cadeia do namespace e a receita não
desabilitará hostname verification ou validação de certificado.

O `TokenCredential` entrega ao SDK a aquisição e renovação do token para o recurso
`https://servicebus.azure.net`. O próprio SDK apresenta esse token no endpoint CBS; a aplicação
não cria `TokenRequestContext`, não materializa o access token e não implementa `put-token`.
Consulte a
[autenticação Entra ID do Service Bus](https://learn.microsoft.com/en-us/azure/service-bus-messaging/authenticate-application)
e o [guia do protocolo AMQP](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-amqp-protocol-guide).

Não configurar neste recorte:

- `connectionString(...)`, `AzureSasCredential` ou `AzureNamedKeyCredential`;
- `ClientSecretCredential`, `AZURE_CLIENT_SECRET` ou token em property;
- `customEndpointAddress(...)`, proxy ou credencial de proxy;
- fallback automático para `AmqpTransportType.AMQP`/TCP;
- `retryOptions(...)`, timeout, prefetch ou concorrência antes dos itens correspondentes;
- `enableCrossEntityTransactions()`, pois a extensão aprovada usa transação de entidade única.

Caso um proxy corporativo seja obrigatório, o uso de `proxyOptions(...)`, seu endereço e eventual
autenticação constituem nova configuração de rede e segurança. Devem ser externalizados e
submetidos a checkpoint; não serão inferidos apenas porque o transporte usa WebSockets.

O resultado deste item é
`DOCUMENTED_FROM_OFFICIAL_API; UNVERIFIED_BY_COMPILATION_AND_ENVIRONMENT`. Ainda faltam resolução
conjunta dos BOMs, compilação, seleção CDI por profile, DNS, TLS, handshake WebSocket, aquisição de
token, autorização RBAC e conexão real.

## Publisher assíncrono da fila de entrada

O publisher usa um `ServiceBusSenderAsyncClient` direcionado exclusivamente à fila de entrada. O
cliente deve ser construído uma vez a partir do builder compartilhado da seção anterior e mantido
durante o ciclo de vida da aplicação:

```java
ServiceBusSenderAsyncClient senderEntrada = builderCompartilhado
        .sender()
        .queueName(config.filaEntrada())
        .buildAsyncClient();
```

`sender()` e `buildAsyncClient()` pertencem ao sub-builder oficial do Azure SDK. O cliente não deve
ser criado por mensagem; sua inicialização, exposição CDI e fechamento determinístico são
completados pelo desenho do item 2.7. Consulte a
[API do sub-builder de sender](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusclientbuilder.servicebussenderclientbuilder?view=azure-java-stable)
e a
[API de `ServiceBusSenderAsyncClient`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebussenderasyncclient?view=azure-java-stable).

### Serialização explícita e composição `Mono<Void>` -> `Uni<Void>`

O publisher futuro fica no adaptador de saída `servicebus`. Ele recebe o sender de longa duração e
o `ObjectMapper` configurado pelo Quarkus, serializa somente o DTO da fila de entrada e preserva o
resultado assíncrono do Azure SDK:

```java
package br.gov.caixa.simtr.dossie.adaptador.saida.servicebus;

import java.util.concurrent.CompletableFuture;

import br.gov.caixa.simtr.dossie.adaptador.saida.servicebus.dto.MensagemEntradaSincronizacaoDossie;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.mutiny.Uni;

public final class PublicadorFilaEntradaServiceBus {

    private static final String APPLICATION_JSON = "application/json";
    private static final String TENTATIVA_ATUAL = "tentativaAtual";
    private static final int PRIMEIRA_TENTATIVA = 1;
    private static final int LIMITE_CORPO_BYTES = 64 * 1024;

    private final ServiceBusSenderAsyncClient sender;
    private final ObjectMapper objectMapper;

    public PublicadorFilaEntradaServiceBus(
            ServiceBusSenderAsyncClient sender,
            ObjectMapper objectMapper) {
        this.sender = sender;
        this.objectMapper = objectMapper;
    }

    public Uni<Void> publicar(MensagemEntradaSincronizacaoDossie mensagem) {
        return Uni.createFrom().completionStage(() -> {
            if (mensagem == null) {
                return CompletableFuture.<Void>failedFuture(
                        new IllegalArgumentException("Mensagem de entrada obrigatória."));
            }

            try {
                byte[] json = objectMapper.writeValueAsBytes(mensagem);
                if (json.length > LIMITE_CORPO_BYTES) {
                    return CompletableFuture.<Void>failedFuture(
                            new IllegalArgumentException(
                                    "Corpo da mensagem de entrada excede 65536 bytes."));
                }

                ServiceBusMessage mensagemServiceBus = new ServiceBusMessage(BinaryData.fromBytes(json))
                        .setContentType(APPLICATION_JSON);
                mensagemServiceBus.getApplicationProperties()
                        .put(TENTATIVA_ATUAL, PRIMEIRA_TENTATIVA);

                return sender.sendMessage(mensagemServiceBus).toFuture();
            } catch (JsonProcessingException erro) {
                return CompletableFuture.<Void>failedFuture(
                        new IllegalArgumentException(
                                "Mensagem de entrada não pôde ser serializada."));
            }
        });
    }
}
```

O `BinaryData` recebe os bytes produzidos pelo Jackson somente depois das validações do record e
do limite aprovado de 64 KiB (65.536 bytes). Entrada nula, campo inválido, falha de serialização ou
corpo acima do limite conclui o `Uni` com falha antes de construir a mensagem AMQP ou chamar
`sendMessage(...)`. As mensagens de erro são sanitizadas: não contêm o DTO nem seus identificadores.
`setContentType("application/json")` explicita o formato do corpo. O exemplo não usa a serialização
implícita de `BinaryData.fromObject` porque o contrato JSON pertence à borda e deverá continuar sob
o `ObjectMapper` do projeto.
`tentativaAtual=1` segue fora desses bytes: é um `Integer` no mapa AMQP de application properties
da primeira mensagem funcional.
Consulte as APIs oficiais de
[`BinaryData.fromBytes(...)`](https://learn.microsoft.com/en-us/java/api/com.azure.core.util.binarydata?view=azure-java-stable)
e
[`ServiceBusMessage`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusmessage?view=azure-java-stable).

A composição possui a seguinte semântica:

1. criar o `Uni` não valida, serializa nem envia a mensagem;
2. a primeira assinatura executa o supplier, valida a entrada, serializa o DTO e, se o corpo
   couber em 64 KiB, chama `sendMessage(...)`;
3. `Mono.toFuture()` assina o `Mono<Void>` do Azure SDK sem bloquear a thread;
4. conclusão vazia do `Mono` torna-se o item `null` de sucesso do `Uni<Void>`;
5. falha de serialização, rejeição do broker ou outra falha do envio termina o `Uni` com falha;
6. cancelamento do `Uni` cancela o `CompletionStage`, que solicita o cancelamento da assinatura
   Reactor ainda em andamento.

A variante com supplier é deliberada. A documentação do Mutiny `3.1.1` recomenda
`completionStage(() -> ...)` para adiar a criação do stage até a assinatura e criar um stage novo
para cada assinatura; também documenta que resultado `null` vira item do `Uni` e conclusão
excepcional vira falha. Consulte
[Mutiny 3.1.1 — integração com `CompletionStage`](https://smallrye.io/smallrye-mutiny/3.1.1/guides/completion-stage/).

O cancelamento é uma solicitação de interrupção da cadeia local, não uma operação compensatória no
broker. Se o Service Bus já tiver aceitado a transferência, cancelar o observador não remove a
mensagem. Essa corrida deverá ser testada com as versões efetivamente resolvidas na futura feature.
Pelo mesmo motivo, cada nova assinatura no `Uni` cria um novo envio: o chamador deve compor e
assinar o resultado uma única vez, e não usar `repeat`, resubscription ou retry funcional antes da
estratégia de idempotência correspondente.

### Quando o envio pode ser considerado concluído

O `Uni<Void>` conclui somente junto com a operação `sendMessage(...)`; não significa que algum
consumidor já processou a mensagem. Os clientes suportados do Service Bus fazem settlement
explícito do envio: a operação aguarda o aceite ou a rejeição do serviço, e o envio aceito conclui
quando a mensagem foi aceita e armazenada pelo broker. Consulte
[transferência e settlement de mensagens](https://learn.microsoft.com/en-us/azure/service-bus-messaging/message-transfers-locks-settlement#settling-send-operations).

Não fazer dentro do publisher:

- chamar `subscribe()`, pois isso separaria o envio do resultado retornado e criaria
  fire-and-forget;
- chamar `block()`, `await()` ou usar o sender síncrono;
- construir o `CompletionStage` fora do supplier de `completionStage(...)`;
- aceitar mensagem nula, contornar os construtores dos records ou enviar corpo acima de 64 KiB;
- recuperar falha com `null`, ocultar rejeição do broker ou registrar body, token, namespace ou
  nomes reais de filas;
- criar o sender por chamada, adicionar retry funcional, repetir a assinatura ou inventar
  `messageId`/deduplicação sem o desenho de idempotência aprovado;
- aceitar `tentativaAtual` arbitrária no método de publicação inicial ou usar `DeliveryCount` como
  seu valor.

A Microsoft alerta que fire-and-forget impede observar erros e pode acumular operações pendentes
em memória. Back-pressure, limite de envios em voo, retry técnico, timeout e concorrência serão
tratados nos itens próprios; este incremento documenta somente uma publicação e seu resultado.

O resultado deste item é
`DOCUMENTED_FROM_OFFICIAL_API; UNVERIFIED_BY_COMPILATION_AND_ENVIRONMENT`. Os imports, a ponte de
cancelamento e o settlement ainda deverão ser provados por testes com as versões resolvidas e um
namespace não produtivo na futura feature executável.

## Consumidor contínuo da fila de saída, sem polling

O consumidor aprovado é um `ServiceBusProcessorClient` de longa duração, direcionado
exclusivamente à fila de saída. A API o define como um mecanismo push que mantém o receptor em
background e chama os handlers quando há mensagem disponível. A aplicação inicia o Processor;
ela não agenda consultas à fila nem chama uma operação de recepção repetidamente. Consulte as APIs
oficiais do
[`ServiceBusProcessorClient`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusprocessorclient?view=azure-java-stable)
e do
[`ServiceBusProcessorClientBuilder`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusclientbuilder.servicebusprocessorclientbuilder?view=azure-java-stable).

O futuro componente de configuração deverá construir uma única instância a partir do builder de
topo compartilhado:

```java
import java.util.function.Consumer;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;

static ServiceBusProcessorClient novoProcessorSaida(
        ServiceBusClientBuilder builderCompartilhado,
        DossieServiceBusConfig config,
        Consumer<ServiceBusReceivedMessageContext> aoReceber,
        Consumer<ServiceBusErrorContext> aoErro) {
    return builderCompartilhado
            .processor()
            .queueName(config.filaSaida())
            .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
            .disableAutoComplete()
            .maxConcurrentCalls(config.maxConcurrentCalls())
            .processMessage(aoReceber)
            .processError(aoErro)
            .buildProcessorClient();
}
```

`processMessage(...)` e `processError(...)` são callbacks obrigatórios do builder. Recebê-los como
dependências no fragmento evita inventar aqui uma retomada de workflow ou uma regra funcional. O
`PEEK_LOCK` e o único `disableAutoComplete()` preservam a decisão C1 de não concluir uma mensagem
antes do processamento aprovado; `Complete`, `Abandon`, `DeadLetter` e o tratamento de exceções
estão definidos em conjunto no item 2.7.

### Início acionado pelo ciclo de vida

Depois que a aplicação e suas dependências estiverem prontas, o ciclo de vida CDI chamará:

```java
processorSaida.start();
```

`start()` inicia o processamento em background e retorna, sem aguardar uma mensagem. A chamada
pertence à inicialização da aplicação, não a um endpoint, callback, timer ou scheduler. O item 2.7
define o evento CDI concreto, o fechamento determinístico e a representação em health.

“Sem polling” é uma propriedade do código da aplicação: não haverá `while`, `sleep`,
`ScheduledExecutorService`, `@Scheduled` nem chamada periódica a `receiveMessages(...)`. O SDK
continua responsável pelo link AMQP, pelos créditos de recepção e pela entrega aos callbacks. O
termo “acionado na chegada” descreve essa interface push do Processor, não a ausência de atividade
interna de recepção no cliente.

### Corpo restrito à borda de saída

Dentro do callback de mensagem, o adaptador deverá obter o `ServiceBusReceivedMessage` por
`contexto.getMessage()`, validar metadados e tamanho e somente então desserializar os bytes com o
`ObjectMapper` do projeto:

```java
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

final class LeitorMensagemSaidaServiceBus {

    private static final int LIMITE_CORPO_BYTES = 64 * 1024;
    private static final Pattern CONTENT_TYPE_JSON = Pattern.compile(
            "^application/json(?:[ \\t]*;[ \\t]*charset[ \\t]*=[ \\t]*utf-8)?[ \\t]*$",
            Pattern.CASE_INSENSITIVE);

    private final ObjectReader leitor;

    LeitorMensagemSaidaServiceBus(ObjectMapper objectMapper) {
        this.leitor = objectMapper
                .readerFor(MensagemSaidaSincronizacaoDossie.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    MensagemSaidaSincronizacaoDossie ler(ServiceBusReceivedMessage mensagem)
            throws IOException {
        validarContentType(mensagem.getContentType());
        byte[] json = lerCorpoLimitado(mensagem.getBody());

        try {
            return leitor.readValue(json);
        } catch (IOException | IllegalArgumentException erro) {
            throw ContratoMensagemInvalidoException.payloadInvalido();
        }
    }

    private static void validarContentType(String contentType) {
        if (contentType == null || !CONTENT_TYPE_JSON.matcher(contentType).matches()) {
            throw ContratoMensagemInvalidoException.contentTypeInvalido();
        }
    }

    private static byte[] lerCorpoLimitado(BinaryData corpo) throws IOException {
        if (corpo == null) {
            throw ContratoMensagemInvalidoException.payloadInvalido();
        }

        Long tamanhoConhecido = corpo.getLength();
        if (tamanhoConhecido != null && tamanhoConhecido > LIMITE_CORPO_BYTES) {
            throw ContratoMensagemInvalidoException.payloadExcedeLimite();
        }

        try (InputStream stream = corpo.toStream()) {
            byte[] json = stream.readNBytes(LIMITE_CORPO_BYTES + 1);
            if (json.length > LIMITE_CORPO_BYTES) {
                throw ContratoMensagemInvalidoException.payloadExcedeLimite();
            }
            return json;
        }
    }
}

final class ContratoMensagemInvalidoException extends RuntimeException {

    private ContratoMensagemInvalidoException(String mensagem) {
        super(mensagem);
    }

    static ContratoMensagemInvalidoException contentTypeInvalido() {
        return new ContratoMensagemInvalidoException("Content-Type contratual inválido.");
    }

    static ContratoMensagemInvalidoException payloadExcedeLimite() {
        return new ContratoMensagemInvalidoException("Corpo excede o limite contratual.");
    }

    static ContratoMensagemInvalidoException payloadInvalido() {
        return new ContratoMensagemInvalidoException("Payload contratual inválido.");
    }
}
```

São aceitos somente `application/json` e, sem outros parâmetros, a variante opcional
`application/json; charset=utf-8`; caixa e espaços horizontais em torno dos separadores não mudam
a semântica do media type. Ausência, outro tipo ou qualquer parâmetro adicional é contrato
inválido. `getLength()` rejeita antecipadamente um tamanho conhecido acima de 64 KiB, mas não é a
única defesa: `toStream()` combinado a `readNBytes(65_537)` limita a materialização mesmo quando o
tamanho for desconhecido ou incorreto. O adaptador nunca chama `toBytes()` antes dessa barreira.
Consulte as APIs oficiais de
[`BinaryData.getLength()` e `toStream()`](https://learn.microsoft.com/en-us/java/api/com.azure.core.util.binarydata?view=azure-java-stable)
e
[`InputStream.readNBytes(int)`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/io/InputStream.html#readNBytes(int)).

O `ObjectReader` próprio da borda rejeita propriedades desconhecidas independentemente da
configuração global do Jackson. O construtor compacto do record rejeita campo ausente, nulo, em
branco ou acima de 256 caracteres. JSON malformado, propriedade desconhecida e violação desses
campos tornam-se `ContratoMensagemInvalidoException` com texto fixo. Uma `IOException` ocorrida ao
ler o stream limitado é propagada separadamente e continua sendo falha técnica; não é classificada
automaticamente como contrato permanentemente inválido. Consulte
[`ObjectReader`](https://fasterxml.github.io/jackson-databind/javadoc/2.13/com/fasterxml/jackson/databind/ObjectReader.html)
e
[`FAIL_ON_UNKNOWN_PROPERTIES`](https://fasterxml.github.io/jackson-databind/javadoc/2.13/com/fasterxml/jackson/databind/DeserializationFeature.html#FAIL_ON_UNKNOWN_PROPERTIES).

O fragmento pertence ao `try` do futuro callback; a disposition sanitizada está definida no item
2.7. O tipo
`ServiceBusReceivedMessageContext` não atravessa a borda do adaptador: somente o DTO próprio da
fila de saída poderá ser entregue ao ponto de processamento que vier a ser aprovado. As APIs de
[`ServiceBusReceivedMessageContext.getMessage()`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusreceivedmessagecontext?view=azure-java-stable)
e
[`ServiceBusReceivedMessage.getBody()`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusreceivedmessage?view=azure-java-stable)
fundamentam essa leitura.

Não fazer neste recorte:

- usar `@Incoming`, canal SmallRye, `ServiceBusReceiverAsyncClient` ou um receptor criado por
  mensagem;
- chamar `processorSaida.start()` a cada requisição ou a cada entrega;
- criar laço, scheduler, timer ou endpoint de consulta para simular consumo contínuo;
- chamar `subscribe()`, `block()` ou `await()` dentro do callback para adaptar uma operação de
  negócio ainda não desenhada;
- chamar `getBody().toBytes()` antes de provar o limite, aceitar media type ausente ou tolerar
  propriedades JSON desconhecidas;
- contradizer settlement, redelivery, lock renewal, prefetch, concorrência, back-pressure, retry,
  timeout, logs ou health definidos no item 2.7;
- registrar body, token, namespace ou nomes reais das filas.

O Processor é a opção recomendada pela documentação do Azure SDK para produção porque recupera
automaticamente falhas transitórias de recepção. Essa capacidade não autoriza retry funcional nem
substitui idempotência e settlement explícito. O resultado deste item é
`DOCUMENTED_FROM_OFFICIAL_API; UNVERIFIED_BY_COMPILATION_AND_ENVIRONMENT`: a resolução da versão
`7.17.19`, os imports, o ciclo de vida, a entrega real e a recuperação ainda dependem da futura
feature executável e de um namespace não produtivo.

## Settlement manual, redelivery e locks

Com `PEEK_LOCK` e `disableAutoComplete()`, o callback é responsável por uma única disposition
explícita. A
[`ServiceBusReceivedMessageContext`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusreceivedmessagecontext?view=azure-java-stable)
expõe `complete()`, `abandon()`, `deadLetter()` e `defer()`; o último permanece fora do exemplo
básico por decisão do C1.

| Resultado classificado pelo adaptador | Disposition | Consequência no broker |
|---|---|---|
| contrato válido e processamento aprovado concluído | `contexto.complete()` | confirma sucesso e remove a mensagem da fila |
| falha transitória, para a qual nova entrega pode ajudar | `contexto.abandon()` | libera o lock e torna a mensagem elegível para redelivery |
| contrato permanentemente inválido, segundo regra explícita e testada | `contexto.deadLetter(novasOpcoesContratoInvalido())` | move a mensagem para a DLQ técnica da fila com motivo sanitizado |
| resultado desconhecido ou falha ao executar settlement | nenhuma segunda disposition por suposição | tratar o resultado como incerto e admitir redelivery |

As chamadas acima deverão ocorrer somente depois da classificação correspondente. Um
`catch (RuntimeException)` genérico não pode transformar toda falha em `Abandon`, nem tentar `Abandon`
depois que `Complete` falhou: o broker pode já ter aplicado a primeira operação e a resposta ter se
perdido. A futura implementação deverá representar falha transitória e contrato inválido de modo
explícito, sem inferi-los apenas pela classe base da exceção.

A disposition de contrato inválido usa uma instância nova de opções em cada chamada, porque
`DeadLetterOptions` é mutável, e nunca copia texto da exceção, body, content type recebido ou
identificadores para a DLQ:

```java
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.models.DeadLetterOptions;

private static final String MOTIVO_CONTRATO_INVALIDO = "CONTRATO_MENSAGEM_INVALIDO";
private static final String DESCRICAO_CONTRATO_INVALIDO =
        "Mensagem rejeitada pela validação da borda.";

private static DeadLetterOptions novasOpcoesContratoInvalido() {
    return new DeadLetterOptions()
            .setDeadLetterReason(MOTIVO_CONTRATO_INVALIDO)
            .setDeadLetterErrorDescription(DESCRICAO_CONTRATO_INVALIDO);
}

private static void rejeitarContratoInvalido(
        ServiceBusReceivedMessageContext contexto) {
    contexto.deadLetter(novasOpcoesContratoInvalido());
}
```

O callback chama `rejeitarContratoInvalido(...)` somente ao capturar a
`ContratoMensagemInvalidoException` definida na borda. Isso cobre media type ausente ou não
permitido, corpo acima de 64 KiB, JSON malformado, propriedade desconhecida e campo ausente, nulo,
em branco ou acima de 256 caracteres. Uma `IOException` da leitura limitada e falhas de negócio,
rede ou settlement seguem a classificação técnica; não recebem `DeadLetter` por este atalho. A
[API de `DeadLetterOptions`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.models.deadletteroptions?view=azure-java-stable)
confirma os setters de razão e descrição, e a
[API do contexto recebido](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusreceivedmessagecontext?view=azure-java-stable)
confirma `deadLetter(DeadLetterOptions)`.

O `processError(...)` do Processor é outro canal. Ele recebe `ServiceBusErrorContext` para erros de
recepção, callback, renovação de lock e settlement; não recebe uma mensagem para substituir a
disposition do callback. `ServiceBusErrorSource` distingue, entre outras, `RECEIVE`,
`USER_CALLBACK`, `RENEW_LOCK`, `COMPLETE` e `ABANDON`. Se a exceção for
`ServiceBusException`, `isTransient()` informa se repetir a operação técnica pode funcionar, mas
isso não autoriza retry funcional da mensagem pela aplicação. Consulte as APIs oficiais de
[`ServiceBusErrorContext`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebuserrorcontext?view=azure-java-stable),
[`ServiceBusErrorSource`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebuserrorsource?view=azure-java-stable)
e
[`ServiceBusException`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusexception?view=azure-java-stable).

### Garantia `at-least-once`

Em Peek-Lock, o broker mantém a mensagem e concede um lock exclusivo temporário. `Complete` é o
ack positivo; `Abandon`, expiração do lock, perda do link ou encerramento antes do settlement
permitem nova entrega. Uma falha depois do efeito externo e antes da confirmação de `Complete`
pode fazer a mesma mensagem chegar novamente. Portanto:

- o exemplo não oferece exactly-once;
- sucesso local sem `Complete` confirmado continua sendo resultado incerto;
- o consumidor real deverá ser idempotente mesmo com concorrência `1`; essa propriedade precisará
  estar provada antes de aumentar concorrência ou adicionar retry;
- redelivery técnico não incrementa o contador funcional criado pela aplicação; essa separação
  está definida no item 2.8.

A documentação de
[transferência, locks e settlement](https://learn.microsoft.com/en-us/azure/service-bus-messaging/message-transfers-locks-settlement)
confirma essas transições e a possibilidade de redelivery. A estratégia concreta de idempotência
depende do efeito futuro e permanece fora deste guia de transporte.

### Renovação de lock, prefetch e timeouts

A API pública atual do builder informa renovação automática de lock por cinco minutos e
`prefetchCount` igual a zero por default. Esses valores são observações da página estável
`7.17.17`, não requisitos aceitos para a matriz documental `7.17.19`. O método
`maxAutoLockRenewDuration(Duration)` define por quanto tempo o cliente tentará renovar; `ZERO` ou
`null` desabilitam essa renovação. Um valor positivo de `prefetchCount(...)` coloca mensagens já
travadas em buffer local.

O fragmento inicial não habilita prefetch positivo nem fixa uma duração de renovação. Na feature
executável será obrigatório:

1. obter a duração de lock configurada na fila não produtiva;
2. medir tempo normal e pior caso do callback;
3. escolher e externalizar uma duração de renovação maior que o processamento esperado;
4. provar expiração, perda do link e falha de settlement;
5. manter prefetch em zero ou escolher valor que caiba no lock e na capacidade medida;
6. alinhar timeout do SDK, prazo de shutdown e timeout funcional sem tratá-los como o mesmo valor.

O lock começa quando a mensagem é adquirida pelo cliente, inclusive no buffer de prefetch. Um
buffer grande com `maxConcurrentCalls=1` pode consumir o tempo de lock antes do callback e causar
redelivery ou falha em `Complete`. Consulte a
[API do Processor builder](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusclientbuilder.servicebusprocessorclientbuilder?view=azure-java-stable)
e a orientação oficial sobre
[prefetch](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-prefetch).

## Concorrência, threads e back-pressure

O builder do item 2.6 usa `maxConcurrentCalls(config.maxConcurrentCalls())`; a configuração inicia
em `1`. Isso limita a uma chamada concorrente do callback por instância do Processor. Duas réplicas
da aplicação ainda podem processar duas mensagens ao mesmo tempo, e redelivery impede tratar esse
limite como garantia de ordenação global.

O callback é um `Consumer<ServiceBusReceivedMessageContext>` síncrono. A documentação de
troubleshooting da Microsoft informa que o Processor invoca handlers em daemon threads do Reactor
`boundedElastic`; isso é comportamento do SDK a validar, não afinidade de thread que a aplicação
possa assumir. O handler não roda por contrato no event loop do Quarkus e não deverá depender de
request context, nome de thread ou estado em `ThreadLocal`.

Não há demanda Reactive Streams exposta entre o Processor e o callback. Neste desenho,
back-pressure significa limitar trabalho admitido por:

- `maxConcurrentCalls`, que limita callbacks simultâneos;
- `prefetchCount`, que controla o buffer local e permanece sem valor positivo neste recorte;
- duração do callback e capacidade real das dependências acionadas;
- número de réplicas, que multiplica a concorrência total.

O callback não poderá iniciar `Uni`/`Mono` com `subscribe()` e retornar antes do resultado, pois
isso dissocia efeito, lock e settlement. Também não deverá usar `block()`/`await()` como ponte
improvisada. Se o futuro ponto de processamento for assíncrono, a feature executável deverá
desenhar uma composição compatível ou reabrir a escolha do cliente em checkpoint arquitetural.

Antes de aumentar o valor inicial `1`, testes de carga deverão provar idempotência, capacidade do
downstream, uso do pool Reactor, tempo de lock, renovação, prefetch, memória, shutdown e
distribuição entre réplicas. Consulte a orientação oficial sobre
[concorrência e threads do Processor](https://learn.microsoft.com/en-us/azure/developer/java/sdk/troubleshooting-messaging-service-bus-overview#concurrency-in-servicebusprocessorclient).

## Ciclo de vida CDI e health

Os clientes são recursos de longa duração. O Quarkus oferece `StartupEvent` e `ShutdownEvent` para
inicialização e encerramento; o Processor oferece `isRunning()`, `start()` e `close()`, e o sender
assíncrono oferece `close()`. O desenho futuro será equivalente a:

```java
import java.util.concurrent.atomic.AtomicBoolean;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class CicloVidaServiceBusDossie {

    private final ServiceBusProcessorClient processorSaida;
    private final ServiceBusSenderAsyncClient senderEntrada;
    private final AtomicBoolean clientesInicializados = new AtomicBoolean();

    public CicloVidaServiceBusDossie(
            ServiceBusProcessorClient processorSaida,
            ServiceBusSenderAsyncClient senderEntrada) {
        this.processorSaida = processorSaida;
        this.senderEntrada = senderEntrada;
    }

    void iniciar(@Observes StartupEvent evento) {
        processorSaida.start();
        clientesInicializados.set(processorSaida.isRunning());
    }

    void encerrar(@Observes ShutdownEvent evento) {
        clientesInicializados.set(false);
        try {
            processorSaida.close();
        } finally {
            senderEntrada.close();
        }
    }

    public boolean pronto() {
        return clientesInicializados.get() && processorSaida.isRunning();
    }
}
```

`close()` já interrompe o processamento e fecha links e sessões; não é necessário chamar
`stop()` imediatamente antes. Marcar o estado como não pronto antes do fechamento impede que o
health anuncie nova admissão durante o shutdown. A
[documentação de lifecycle do Quarkus 3.33](https://quarkus.io/version/3.33/guides/lifecycle#listening-for-startup-and-shutdown-events),
a
[API do Processor](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusprocessorclient?view=azure-java-stable)
e a
[API do sender assíncrono](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebussenderasyncclient?view=azure-java-stable)
confirmam essas operações.

O Quarkus já possui `quarkus-smallrye-health`. A integração acrescentará somente readiness local:

```java
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;

@Readiness
@ApplicationScoped
public class DossieServiceBusReadiness implements HealthCheck {

    private final CicloVidaServiceBusDossie cicloVida;

    public DossieServiceBusReadiness(CicloVidaServiceBusDossie cicloVida) {
        this.cicloVida = cicloVida;
    }

    @Override
    public HealthCheckResponse call() {
        return cicloVida.pronto()
                ? HealthCheckResponse.up("dossie-service-bus-inicializado")
                : HealthCheckResponse.down("dossie-service-bus-inicializado");
    }
}
```

Esse check informa somente que os clientes CDI foram construídos e que o Processor está no estado
`running`. Ele não faz send, receive, management query, DNS ou round-trip ao broker e não prova que
o namespace está acessível. Nenhum `@Liveness` específico consultará o Service Bus: indisponibilidade
do broker não deve provocar restart permanente da aplicação enquanto o Processor tenta recuperar.
A
[documentação do SmallRye Health no Quarkus 3.33](https://quarkus.io/version/3.33/guides/smallrye-health)
distingue readiness, liveness e seus endpoints.

No diagnóstico, serão permitidos apenas o nome fixo do check, `ServiceBusErrorSource`, classe da
exceção e, após sanitização, `ServiceBusFailureReason`/indicador transitório. Não registrar
`getFullyQualifiedNamespace()`, `getEntityPath()`, body, IDs do contrato, token, identidade ou
configuração. O guia não acrescenta span nem métrica.

O shutdown gracioso do Quarkus não garante drenagem de callbacks desse SDK: a documentação limita
o suporte atual às extensões que o implementam e destaca HTTP. A matriz `7.17.19` também não possui
`drainTimeout`, introduzido somente na linha `7.18.0-beta` conforme o
[changelog oficial](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/servicebus/azure-messaging-servicebus/CHANGELOG.md).
A futura feature deverá testar callbacks
em voo, tempo de `close()`, redelivery após interrupção e a janela de término do orquestrador; este
guia não promete drenagem não comprovada.

O resultado do item 2.7 é
`DOCUMENTED_FROM_OFFICIAL_API; UNVERIFIED_BY_COMPILATION_AND_ENVIRONMENT`. Settlement, lock loss,
threads, carga, shutdown, health e recuperação ainda deverão ser provados com as dependências
resolvidas e um namespace não produtivo.

## `tentativaAtual` funcional versus `DeliveryCount` técnico

Os dois valores contam fenômenos diferentes e nunca serão copiados, comparados como equivalentes
ou usados como fallback um do outro:

| Dimensão | `tentativaAtual` | `DeliveryCount` |
|---|---|---|
| proprietário | aplicação | Azure Service Bus |
| localização | AMQP `application-properties` da mensagem de entrada | header/propriedade de sistema somente leitura |
| tipo observado no Java | `Integer`, por contrato desta integração | `long`, retornado por `getDeliveryCount()` |
| início | `1` na primeira publicação funcional | atribuído e mantido pelo broker durante as entregas da mensagem |
| avanço | somente após consulta funcional válida e inconclusiva | quando o lock expira ou o receiver executa `Abandon` |
| finalidade | posição na política funcional de monitoramento | diagnóstico e proteção contra falhas técnicas repetidas |
| limite | lista versionada de intervalos e prazo individual | `MaxDeliveryCount` configurado na entidade, não na mensagem |

A documentação do Service Bus separa user/application properties das propriedades predefinidas
do broker e classifica `DeliveryCount` como read-only. No Azure SDK Java,
`ServiceBusMessage.getApplicationProperties()` permite publicar metadados livres, enquanto
`ServiceBusReceivedMessage.getDeliveryCount()` apenas lê o número de entregas. Consulte
[`ServiceBusMessage`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusmessage?view=azure-java-stable),
[`ServiceBusReceivedMessage`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusreceivedmessage?view=azure-java-stable)
e a
[estrutura oficial das mensagens](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-messages-payloads).

### Contrato da application property

Para a fila de entrada, a chave exata `tentativaAtual` é metadado contratual obrigatório e
versionado, mesmo fora do JSON. A primeira publicação do item 2.5 grava o `Integer` `1`; o body
continua contendo somente `idDossiePreValidacao` e `idDossieMtr`.

Na futura recepção da fila de entrada, a validação mínima será equivalente a:

```java
import java.util.OptionalInt;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;

private static final String TENTATIVA_ATUAL = "tentativaAtual";

static OptionalInt lerTentativaAtual(ServiceBusReceivedMessage mensagem) {
    Object valor = mensagem.getApplicationProperties().get(TENTATIVA_ATUAL);
    if (!(valor instanceof Integer tentativaAtual) || tentativaAtual < 1) {
        return OptionalInt.empty();
    }
    return OptionalInt.of(tentativaAtual);
}
```

`OptionalInt.empty()` representa metadado ausente, tipo diferente de `Integer` ou valor menor que
`1`; o chamador deverá classificá-lo como contrato permanentemente inválido e aplicar a política
de settlement do item 2.7. Não converter `String`, `Long`, número decimal ou `Number.intValue()`:
isso esconderia produtores incompatíveis e poderia truncar valores.

O valor recebido também não é fonte de verdade suficiente. A implementação de produção deverá
compará-lo com o estado persistido e idempotente do monitoramento:

- sem estado anterior, a tentativa esperada é `1`;
- valor menor que o esperado pode representar mensagem duplicada ou obsoleta;
- valor maior que o esperado representa salto ou processamento fora do estado conhecido;
- nenhum desses casos autoriza sobrescrever silenciosamente o estado persistido.

A decisão concreta para duplicata, salto e retomada depende do workflow futuro. Este guia registra
o guardrail, mas não inventa a transição.

### Efeito de falha técnica e redelivery

O listener da fila de saída não inicia nem incrementa `tentativaAtual`. Se o tratamento técnico de
uma mensagem de saída falhar e executar `Abandon`, a application property — caso exista — não é
alterada; na próxima entrega, apenas o `DeliveryCount` dessa mesma mensagem pode aumentar:

```java
long deliveryCount = contexto.getMessage().getDeliveryCount();
```

O valor é diagnóstico técnico. Ele não seleciona intervalo funcional, não decide nova consulta ao
MTR e não produz quarentena. Da mesma forma, código da aplicação não tenta definir ou reiniciar
`DeliveryCount`: a API recebida o expõe somente para leitura.

`MaxDeliveryCount` é configuração da fila usada pelo broker como rede de segurança. Ao exceder o
limite após abandonos ou expirações de lock, a mensagem segue para a DLQ com razão técnica
`MaxDeliveryCountExceeded`. O valor concreto permanece uma decisão operacional; a nova mensagem
funcional, o reagendamento e os destinos de quarentena/DLQ são detalhados na seção seguinte.
Consulte a documentação oficial de
[dead-letter queues](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-dead-letter-queues).

Não fazer:

- acrescentar `tentativaAtual` aos records ou ao JSON de duas propriedades;
- usar `DeliveryCount` como tentativa funcional, limite de negócio ou índice de intervalo;
- incrementar `tentativaAtual` em falha técnica, `Abandon`, lock perdido ou redelivery;
- aceitar application property ausente ou incompatível usando default silencioso;
- copiar `DeliveryCount` para uma mensagem nova ou tentar persistir alteração no objeto recebido;
- exigir `tentativaAtual` no contrato da fila de saída sem novo checkpoint de contrato.

O resultado do item 2.8 é
`DOCUMENTED_FROM_OFFICIAL_API; UNVERIFIED_BY_COMPILATION_AND_ENVIRONMENT`. A codificação AMQP do
`Integer`, a leitura, os casos inválidos, o `DeliveryCount` e a comparação com estado persistido
ainda deverão ser provados na futura feature executável.

## Reagendamento progressivo, quarentena funcional e DLQ técnica

Esta seção é o handoff para um futuro processor da fila de entrada. Ela não adiciona esse
processor ao recorte básico, não define o workflow persistente e não transforma redelivery em
temporizador funcional. O listener da fila de saída continua com o desenho dos itens 2.6 e 2.7.

### Política temporal preservada por monitoramento

Cada monitoramento deverá persistir `enviadoAoMtrEm`, `limiteEm`, a versão — ou o snapshot — da
lista de intervalos e a tentativa esperada. O prazo nasce uma única vez:

```text
limiteEm = enviadoAoMtrEm + prazoMaximoValidacao
```

Uma mudança posterior na configuração afeta somente monitoramentos novos. Não recalcular
`limiteEm`, não trocar a lista de um monitoramento em curso e não usar o horário de redelivery
como novo início.

A lista é externa, positiva, estritamente crescente e versionada. Valores como
`PT30M,PT3H,PT4H,PT6H` são apenas ilustrativos, não defaults. O mapeamento não pode ficar
implícito:

- `intervalos[0]` separa a tentativa `1` da tentativa `2`;
- `intervalos[n]` separa a tentativa `n + 1` da tentativa `n + 2`;
- uma lista com `N` intervalos permite no máximo `N + 1` consultas funcionais;
- após uma consulta inconclusiva, a próxima ativação é
  `min(agora + intervalo, limiteEm)`.

Em cada entrega válida da fila de entrada, a futura implementação deverá avaliar os casos nesta
ordem:

| Situação | Ação funcional | Settlement da mensagem atual |
|---|---|---|
| `agora >= limiteEm`, inclusive por atraso do broker | produzir/persistir `QUARENTENA` sem consultar o MTR | `Complete` somente após tornar o resultado durável |
| consulta funcional conclusiva | persistir o resultado conclusivo | `Complete` após a garantia de continuidade do workflow |
| falha técnica antes de conclusão funcional | não alterar `tentativaAtual` | `Abandon` da mesma mensagem |
| consulta válida e inconclusiva, com intervalo seguinte e antes do prazo | criar nova mensagem com contador incrementado e horário calculado | agendar a próxima e concluir a atual na transação do broker |
| consulta válida e inconclusiva, sem intervalo seguinte ou já no prazo | produzir/persistir `QUARENTENA` | `Complete` somente após tornar o resultado durável |

A verificação do prazo ocorre antes de cada consulta e novamente depois de um resultado
inconclusivo. Assim, uma ativação tardia não amplia o prazo e uma consulta que termine depois de
`limiteEm` não cria outra tentativa.

### Nova mensagem funcional

Reagendamento cria uma nova `ServiceBusMessage`; não usa `Abandon` como relógio. A nova mensagem:

- mantém o body JSON com somente `idDossiePreValidacao` e `idDossieMtr`, serializado pelo
  `ObjectMapper` da borda;
- grava `tentativaAtual + 1` como `Integer` em `applicationProperties`, depois de validar estado,
  faixa e disponibilidade do próximo intervalo;
- não copia `DeliveryCount`, lock token, sequence number, enqueue time nem outra propriedade
  somente leitura da mensagem recebida;
- usa o mesmo `limiteEm` e a mesma política versionada do estado persistido, sem acrescentá-los ao
  JSON aprovado.

O `SequenceNumber` retornado por `scheduleMessage(...)` identifica o agendamento enquanto ele
permanece nesse estado; não é identificador funcional nem substitui idempotência. O broker torna a
mensagem disponível somente no horário agendado, sujeito à carga da fila, e não oferece agenda
recorrente. Consulte a documentação de
[mensagens agendadas](https://learn.microsoft.com/en-us/azure/service-bus-messaging/message-sequencing#scheduled-messages)
e a API de
[`ServiceBusSenderAsyncClient`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebussenderasyncclient?view=azure-java-stable).

### `schedule + Complete` na mesma transação do broker

A extensão aprovada usará `ServiceBusReceiverAsyncClient` e `ServiceBusSenderAsyncClient` de longa
duração, derivados do mesmo `ServiceBusClientBuilder`, apontando para a mesma fila de entrada. A
identidade separada desse processor terá somente Data Receiver e Data Sender nessa fila. Como a
transação envolve uma única entidade, o builder não chama `enableCrossEntityTransactions()`.

Depois que a consulta e a decisão funcional terminarem e a nova mensagem estiver pronta, a
composição de broker será equivalente a:

```java
import java.time.OffsetDateTime;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.azure.messaging.servicebus.models.CompleteOptions;

import reactor.core.publisher.Mono;

final class ReagendadorTransacionalServiceBus {

    private final ServiceBusReceiverAsyncClient receiverEntrada;
    private final ServiceBusSenderAsyncClient senderEntrada;

    ReagendadorTransacionalServiceBus(
            ServiceBusReceiverAsyncClient receiverEntrada,
            ServiceBusSenderAsyncClient senderEntrada) {
        this.receiverEntrada = receiverEntrada;
        this.senderEntrada = senderEntrada;
    }

    Mono<Void> reagendarEConcluir(
            ServiceBusReceivedMessage atual,
            ServiceBusMessage proxima,
            OffsetDateTime proximaExecucao) {
        return Mono.usingWhen(
                receiverEntrada.createTransaction(),
                transacao -> senderEntrada
                        .scheduleMessage(proxima, proximaExecucao, transacao)
                        .then(receiverEntrada.complete(
                                atual,
                                new CompleteOptions().setTransactionContext(transacao))),
                receiverEntrada::commitTransaction,
                (transacao, falhaOriginal) -> receiverEntrada
                        .rollbackTransaction(transacao)
                        .onErrorResume(falhaRollback -> {
                            if (falhaRollback != falhaOriginal) {
                                falhaOriginal.addSuppressed(falhaRollback);
                            }
                            return Mono.empty();
                        }),
                receiverEntrada::rollbackTransaction);
    }
}
```

O `Mono.usingWhen(...)` limita o recurso transacional à composição: sucesso das duas operações
leva ao commit; falha anterior ao commit preserva a falha original e tenta rollback; cancelamento
também tenta rollback. O `Mono<Long>` do agendamento só é descartado por `then(...)` depois de
concluir aquela operação no contexto transacional. A sobrecarga com limpezas distintas segue a
[API oficial de `Mono.usingWhen(...)`](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html);
a versão efetiva de Reactor continua sujeita à futura resolução conjunta dos BOMs.

Falha ou timeout na resposta de commit tem resultado incerto. Nesse caso, não executar
automaticamente `Abandon`, novo commit ou nova transação por suposição: a mensagem atual pode ter
sido concluída e a próxima pode ter sido agendada. A retomada deverá reconciliar o estado
persistido e processar duplicatas de forma idempotente.

A garantia é estritamente do Azure Service Bus. Chamada MTR, banco, estado do workflow e eventual
publicação externa não participam da transação; Outbox ou outra coordenação exigem desenho e GO
próprios. A transação também:

- requer namespace Standard ou Premium; Basic não oferece transações;
- expira em dois minutos contados da primeira operação;
- não deve envolver a consulta MTR nem trabalho funcional demorado;
- não recebe retry funcional ou repetição cega quando o resultado é incerto.

As APIs oficiais expõem `createTransaction()`, `scheduleMessage(..., transactionContext)`,
`complete(..., CompleteOptions)` e commit/rollback. Consulte
[`ServiceBusReceiverAsyncClient`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusreceiverasyncclient?view=azure-java-stable),
[`CompleteOptions`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.models.completeoptions?view=azure-java-stable),
[`ServiceBusTransactionContext`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebustransactioncontext?view=azure-java-stable),
[`ServiceBusClientBuilder`](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusclientbuilder?view=azure-java-stable)
e a visão oficial de
[transações](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-transactions).

### Quarentena funcional não é DLQ

`QUARENTENA` é resultado terminal do monitoramento quando o prazo individual ou a política de
intervalos se esgota. O workflow futuro deverá produzi-lo ou persistir sua transição explicitamente
e de modo idempotente. Se essa continuidade falhar tecnicamente, a mensagem atual é abandonada sem
incrementar `tentativaAtual`; se o estado já estiver em `QUARENTENA` numa redelivery, ele deverá ser
reconhecido antes de repetir efeitos.

Este guia não inventa fila, endpoint, tabela, evento ou schema para quarentena. Essa definição
pertence à futura feature de workflow e exigirá os checkpoints correspondentes. O que fica
proibido é usar `deadLetter()` ou esperar `MaxDeliveryCount` por causa de prazo funcional,
quantidade de consultas ou resultado inconclusivo.

A DLQ permanece exclusivamente técnica:

| Causa | Mecanismo | Tratamento |
|---|---|---|
| contrato permanentemente inválido | `DeadLetter` explícito, conforme item 2.7 | investigação/correção do produtor ou contrato |
| falhas técnicas repetidas, abandonos ou locks expirados | DLQ automática ao exceder `MaxDeliveryCount` | diagnóstico e recuperação operacional controlada |
| prazo funcional ou lista de intervalos esgotados | não usar DLQ | `QUARENTENA` explícita |

O valor de `MaxDeliveryCount` não é escolhido por este guia: é decisão operacional da entidade,
não limite de negócio nem configuração da mensagem. A DLQ não é limpa nem reprocessada
automaticamente; monitoramento, alerta, retenção e replay precisam de runbook e autorização
próprios. Consulte
[dead-letter queues](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-dead-letter-queues).

Não fazer:

- reagendar com `Abandon`, `sleep`, scheduler local ou polling;
- incrementar `tentativaAtual` antes de uma consulta válida terminar inconclusiva;
- calcular novo prazo a partir de `agora` ou da mensagem reagendada;
- agendar a próxima mensagem e executar `Complete` como operações independentes;
- incluir MTR, banco ou outro serviço na suposta atomicidade do Service Bus;
- habilitar cross-entity, fixar `MaxDeliveryCount` ou criar destino de quarentena sem decisão
  própria;
- reenviar automaticamente depois de commit incerto.

As páginas Learn consultadas identificam o artifact `7.17.17`, enquanto a matriz documental do
Azure SDK BOM `1.3.8` aponta `7.17.19`. O
[changelog oficial](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/servicebus/azure-messaging-servicebus/CHANGELOG.md)
registra apenas atualizações de dependências em `7.17.19`; isso não substitui resolução,
compilação nem prova transacional. O resultado do item 2.9 é
`DOCUMENTED_FROM_OFFICIAL_API; UNVERIFIED_BY_COMPILATION_AND_ENVIRONMENT`.

## Verificação manual em ambiente não produtivo

Este roteiro pertence à futura feature executável. Ele descreve como produzir evidência manual
sem criar testes unitários, mas não foi executado nesta branch documental. A situação atual é
`PROTOCOL_DOCUMENTED; NOT_EXECUTED`. A verificação não substitui compilação, testes automatizados
nem os checkpoints da implementação.

### Pré-condições e registro da execução

Usar namespace e filas dedicados ou vazios em ambiente não produtivo, sem dados reais. A máquina
de execução deverá alcançar o namespace por AMQP sobre WebSockets/TLS em TCP `443`; as identidades
deverão possuir somente os papéis aprovados para cada fila. Alterações recentes de RBAC precisam
estar propagadas antes do início. O portal poderá usar Microsoft Entra ID e os papéis Data Sender
ou Data Receiver no menor escopo necessário.

A feature executável deverá ainda fornecer e nomear:

- o comando aprovado de inicialização, depois de resolução de dependências e compilação;
- um gatilho técnico aprovado para chamar o publisher, sem que este guia invente endpoint público;
- uma forma reversível e exclusiva de não produção para provocar falha transitória antes do
  settlement, sem alterar o JSON;
- um efeito técnico sanitizado ou ponto de inspeção que permita confirmar a conversão dos dois
  campos sem registrá-los em log.

Se qualquer um desses meios não existir, o passo correspondente fica `NOT_EXECUTED`; ausência de
pré-condição não pode ser convertida em sucesso. O registro mínimo será:

| Campo | Regra de registro |
|---|---|
| execução | identificador local, revisão do código e instante UTC |
| ambiente | alias não sensível; nunca FQDN, nome real de fila ou identificador da identidade |
| matriz | versões efetivamente resolvidas de Quarkus, Java, Azure SDK BOM, Service Bus e Identity |
| autorização | nomes dos papéis e escopos descritos por alias, sem token ou credencial |
| passo | `NOT_EXECUTED`, `OBSERVED` ou `NOT_OBSERVED`, com referência à evidência sanitizada |
| dados de ensaio | apenas `JSON válido conhecido` ou o nome fixo do caso inválido; não copiar payload nem IDs para o relatório |

### Roteiro básico ponta a ponta

1. **Inicialização e health.** Iniciar a aplicação uma única vez pelo comando aprovado. Depois do
   lifecycle, consultar os endpoints padrão:

   ```powershell
   Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/q/health/live'
   Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/q/health/ready'
   ```

   Se a implementação mover health para interface, porta ou root path de gerenciamento, usar a
   URL aprovada daquela feature. Esperar liveness `UP` sem round-trip ao broker e readiness `UP`
   somente com os clientes inicializados e o Processor em execução. Os nomes dos checks devem
   identificar componentes por aliases sanitizados. Os paths e o formato de `status`/`checks`
   seguem o
   [SmallRye Health do Quarkus 3.33](https://quarkus.io/version/3.33/guides/smallrye-health).

2. **Chamada técnica ao publisher.** Acionar o gatilho aprovado com o JSON conhecido de duas
   propriedades. Não criar endpoint ad hoc para esta verificação. Registrar o início e o término
   da chamada, sem registrar body ou identificadores contratuais.

3. **Aceite do envio pelo broker.** Confirmar que o resultado assíncrono da chamada só conclui
   com sucesso depois do aceite pelo Service Bus; falha e cancelamento não podem aparecer como
   sucesso. Com nenhum consumidor da entrada retirando a mensagem, usar **Peek** no Service Bus
   Explorer para confirmar uma mensagem ativa com `content-type=application/json`, body de duas
   propriedades e application property `tentativaAtual` inteira igual a `1`. Não usar
   **ReceiveAndDelete**. A conclusão de `sendMessage(...)` representa o aceite da transferência
   pelo broker conforme
   [message transfers, locks and settlement](https://learn.microsoft.com/en-us/azure/service-bus-messaging/message-transfers-locks-settlement).

4. **Injeção conhecida na saída.** No
   [Service Bus Explorer do portal](https://learn.microsoft.com/en-us/azure/service-bus-messaging/explorer),
   autenticado por Microsoft Entra ID, selecionar a fila de saída não produtiva, escolher
   `Application/Json`, inserir o mesmo wire shape de duas propriedades e executar **Send**. Não
   acrescentar `tentativaAtual` à saída. Manter a aplicação já iniciada; não fazer nova chamada,
   varredura ou temporização na aplicação para provocar o consumo.

5. **Entrega acionada pela chegada.** Observar uma única invocação do callback do Processor e a
   conversão para o DTO da saída pelo efeito sanitizado ou ponto de inspeção definido na feature
   executável. A mensagem deve chegar sem chamada HTTP, scheduler ou loop de consulta após o
   `start()`. Complementar a observação com revisão de código que confirme a ausência de polling;
   uma execução manual isolada não prova essa ausência para todos os caminhos.

6. **Settlement de sucesso.** Confirmar que o efeito aprovado termina antes de `Complete` e que a
   mensagem deixa a contagem ativa, não aparece na DLQ e não recebe uma segunda disposition. Usar
   **Peek** e as contagens do Explorer como observação não destrutiva; considerar a atualização
   assíncrona das métricas e não inferir causalidade em fila compartilhada.

7. **Rejeição contratual controlada.** Primeiro, pelo gatilho técnico aprovado do publisher,
   tentar publicar entrada nula, campo em branco e campo com 257 caracteres sintéticos. Cada
   tentativa deve falhar antes do envio e não alterar a contagem da fila de entrada. Se o gatilho
   não permitir distinguir essa borda, registrar `NOT_EXECUTED`, sem criar endpoint ad hoc.

   Depois, enviar separadamente à fila de saída, sempre com dados sintéticos e sem copiar o body
   para o relatório: mensagem sem content type; com content type diferente de `application/json`;
   com `application/json` e corpo de 65.537 bytes; com propriedade JSON desconhecida; e com campo
   ausente, nulo, em branco ou de 257 caracteres. Cada entrega deve ir diretamente para a DLQ com
   `DeadLetterReason=CONTRATO_MENSAGEM_INVALIDO` e
   `DeadLetterErrorDescription=Mensagem rejeitada pela validação da borda.`, sem body, content type
   recebido, exceção ou identificador incorporado ao motivo/descrição. Observar por **Peek**; não
   executar **Purge**, replay ou `ReceiveAndDelete`.

8. **Falha transitória e redelivery.** Em fila dedicada, habilitar o mecanismo reversível de falha
   antes do efeito/settlement e enviar outro JSON válido conhecido. Esperar `Abandon`, redelivery
   da mesma mensagem e eventual aumento de `DeliveryCount`, sem mudar `tentativaAtual`. Desabilitar
   a falha e confirmar processamento seguido de `Complete`. Não usar JSON malformado neste passo:
   contrato permanentemente inválido segue `DeadLetter`, não o caminho de redelivery transitório.
   Sem injetor seguro e aprovado, registrar `NOT_EXECUTED`.

9. **Logs seguros.** Inspecionar localmente os logs capturados durante todos os passos e confirmar
   que não contêm body, valores dos dois IDs, namespace/FQDN, nomes reais de filas, identificador de
   identidade, token ou credencial. Não colar no relatório linhas que revelem os próprios valores
   procurados; registrar somente o resultado e uma referência sanitizada à captura protegida.

### Critérios de parada e limite da evidência

Parar a execução e preservar a evidência, sem reenvio automático, quando houver settlement
incerto, permissão mais ampla que a aprovada, destino produtivo, segredo exposto, mensagem
inesperada na DLQ, mensagem inválida fora da DLQ, razão/descrição diferente dos valores fixos ou
resultado não correlacionável. O roteiro não autoriza **Purge**,
**ReceiveAndDelete** nem limpeza destrutiva; mensagens de ensaio remanescentes seguem o runbook e
a autorização do ambiente não produtivo.

Considerar o roteiro observado somente quando os nove passos tiverem `OBSERVED`. Qualquer passo
`NOT_EXECUTED` ou `NOT_OBSERVED` mantém o resultado global sem comprovação. Mesmo quando observado,
o roteiro não prova concorrência, carga, reconexão, lock perdido, duplicatas em todas as janelas,
idempotência, modo nativo ou compatibilidade transacional do item 2.9. Esses casos, inclusive
commit/rollback e recuperação de resultado incerto, pertencem aos testes e controles da futura
feature de implementação.

## Limites do exemplo e próximos incrementos

### O que está documentado e o que continua sem prova

O guia fecha o desenho do recorte básico, não a implementação. No estado atual do repositório não
há dependências Azure Service Bus, configuração executável, clientes, publisher, consumidor ou
health dessa integração. Nenhum namespace, fila, identidade ou credencial foi disponibilizado e
nenhuma conexão foi tentada.

| Dimensão | O guia define | Limite atual |
|---|---|---|
| plataforma | Quarkus `3.33` LTS, Java `25` e matriz documental do Azure SDK BOM `1.3.8` | resolução conjunta dos BOMs, compilação e runtime continuam sem prova |
| contrato | dois records de borda; duas `String` obrigatórias, não nulas, não brancas e de até 256 caracteres; propriedades desconhecidas rejeitadas; body de até 64 KiB; `tentativaAtual` fora do body | não há produtor/consumidor compilado nem teste de compatibilidade com mensagens reais |
| publicação | sender assíncrono e composição `Mono<Void>` para `Uni<Void>`, com validação anterior ao envio e sem fire-and-forget | não existe gatilho técnico aprovado nem envio aceito ou rejeição local observado |
| consumo | Processor de longa duração, acionado pela entrega, media type restrito, leitura limitada e settlement manual com DLQ sanitizada | reconexão, lock renewal, prefetch, concorrência, back-pressure e shutdown em voo não foram exercitados |
| segurança e rede | Entra ID, RBAC mínimo, `AMQP_WEB_SOCKETS` em TCP `443` e um namespace sob `.servicebus.windows.net` | identidade, escopos efetivos, propagação de RBAC, DNS, firewall, proxy e handshake não foram validados |
| reagendamento | política funcional e `schedule + Complete` na mesma transação do broker | tier, commit, rollback, timeout e resultado incerto não foram testados em ambiente |
| workflow | fronteira entre `tentativaAtual`, `DeliveryCount`, quarentena e DLQ | MTR, estado durável, idempotência, destino da quarentena, Outbox e continuidade do processo não possuem implementação neste recorte |
| operação | health local e diagnóstico sem segredo | métricas, spans, alertas, capacidade, runbooks de DLQ/replay e suporte operacional não foram definidos |

Também permanecem fora de qualquer alegação deste guia: modo nativo/reflection, ordenação global,
exactly-once, throughput, alta disponibilidade, recuperação de desastre, provisionamento Azure e
atomicidade entre Service Bus, MTR, banco ou workflow. Readiness indica o estado local aprovado;
não prova disponibilidade ponta a ponta do broker.

Portanto, não descrever este material como aplicação pronta, integração validada, garantia de
produção ou exemplo copiável sem adaptação. O ADR-0009 aceita a direção arquitetural; ele não
converte os gates executáveis em evidência.

### Gates antes de qualquer alegação executável

| Gate | Evidência mínima futura |
|---|---|
| build | versões efetivamente resolvidas, `dependency:tree`, compilação no Java `25` e ausência de conflito entre Reactor, Netty, Jackson e Azure Core |
| autenticação e transporte | token obtido por credencial aprovada, RBAC no menor escopo e conexão WebSockets/TLS `443` com aliases não sensíveis |
| publisher | rejeição anterior ao send de entrada nula/inválida ou acima de 64 KiB, serialização exata, `tentativaAtual=1` como `Integer` e resultado concluído somente após aceite do broker |
| consumidor | entrega sem polling, media type, limite anterior à materialização, JSON/campos estritos, `Complete`, `Abandon`, `DeadLetter` sanitizado, lock renewal, reconexão e encerramento exercitados |
| duplicatas | processamento idempotente e recuperação de lock perdido/restart comprovados, pois `PEEK_LOCK` oferece entrega `at-least-once` |
| transação | namespace Standard/Premium e casos de commit, rollback, cancelamento, timeout e resposta incerta de `schedule + Complete` |
| operação segura | logs sanitizados, capacidade dimensionada, alertas e runbooks autorizados para DLQ e replay |

A documentação oficial confirma que `PEEK_LOCK` admite redelivery e exige processamento
idempotente no consumidor; detecção de duplicatas no envio não substitui essa proteção. Consulte
[perda e duplicatas no Service Bus](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-message-loss-and-duplicates).
A DLQ não possui limpeza automática, por isso alerta, investigação, retenção e replay precisam de
runbook explícito antes de produção. Consulte
[dead-letter queues](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-dead-letter-queues).

### Próximos incrementos, ainda não autorizados por este guia

Depois do checkpoint C2, os próximos trabalhos deverão ser recortados em features ou fatias
aprovadas, nesta ordem de dependência:

1. **Transporte básico executável:** adicionar dependências, configuração, DTOs, clientes de longa
   duração, publisher, consumidor da saída e health, preservando exatamente o recorte aprovado.
2. **Prova da fatia básica:** criar testes automatizados, validar arquitetura e segurança, compilar
   a matriz efetiva e executar o roteiro do item 2.10 contra filas não produtivas. A estratégia
   detalhada de testes será registrada no handoff posterior ao C2; o
   [guia de testes do Quarkus 3.33](https://quarkus.io/version/3.33/guides/getting-started-testing)
   será uma das fontes oficiais.
3. **Processor da fila de entrada e workflow:** integrar MTR e estado durável, implementar a
   política de intervalos, idempotência, transação `schedule + Complete` e destino explícito da
   quarentena. Esse incremento altera arquitetura e comportamento e exige seus próprios
   checkpoints.
4. **Robustez e operação produtiva:** dimensionar concorrência/prefetch, provar lock renewal e
   reconexão, tratar duplicatas, definir DLQ/replay, observabilidade, capacidade, infraestrutura,
   rede e rollout. Cada mudança pública, arquitetural, de segurança ou observável exige o
   checkpoint correspondente.

Modo nativo, Outbox, sessions/ordenação, duplicate detection do broker e qualquer endpoint público
somente entram se uma necessidade concreta for planejada e aprovada; não são continuação
implícita. O item 2.11 encerra apenas o conteúdo técnico do guia. Branch, plano, GO e dívida de
testes da implementação não são criados implicitamente: o handoff de branch, plano e novo GO está
registrado no item 3.1 da feature documental. Em 2026-09-02, o usuário decidiu não prosseguir com
a implementação; por isso, o item 3.2 foi encerrado como não aplicável. Qualquer implementação
futura dependerá de um novo pedido e continuará sujeita ao handoff e aos gates documentados.

O resultado corrente do guia é
`GUIDE_COMPLETE; C2_APPROVED; GUIDE_ACCEPTED; FEATURE_CLOSED; IMPLEMENTATION_NOT_REQUESTED; EXECUTION_UNVERIFIED`.

## Cobertura do guia

O conteúdo dos itens 2.1 a 2.11 cobre, na ordem:

1. objetivo, leitor e limite do recorte;
2. topologia e direção das duas filas;
3. pré-condições de Azure, rede e autenticação;
4. versões validadas, Azure SDK BOM e dependências Maven;
5. árvore proposta dentro de `br.gov.caixa.simtr.dossie`;
6. contratos Java independentes e JSON de duas propriedades;
7. configuração do sender da fila de entrada;
8. publisher com `ServiceBusSenderAsyncClient` e composição do resultado;
9. configuração do consumidor da fila de saída;
10. consumo contínuo pelo cliente Azure aprovado, acionado na chegada e sem polling;
11. settlement, redelivery, lock renewal, threads, back-pressure e concorrência;
12. separação entre contador funcional, `DeliveryCount`, quarentena e DLQ;
13. reagendamento progressivo como extensão condicionada à compatibilidade comprovada;
14. health e diagnóstico seguro;
15. verificação manual ponta a ponta sem testes unitários;
16. falhas esperadas e diagnóstico sem exposição de segredo;
17. limites do exemplo e próximos incrementos de produção;
18. referências oficiais versionadas.

## Regra de evolução deste guia

Qualquer evolução deverá manter explícita a diferença entre decisão documentada e comportamento
comprovado. Os snippets foram confrontados com as APIs e imports documentados, mas permanecem sem
compilação; somente os gates da futura feature poderão torná-los evidência executável. Nenhum
formato `.ppt`, `.pptx`, `.pdf` ou `.html` será gerado a partir deste Markdown.
