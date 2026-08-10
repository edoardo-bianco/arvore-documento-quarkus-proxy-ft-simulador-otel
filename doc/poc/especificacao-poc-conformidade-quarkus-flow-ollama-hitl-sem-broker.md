# Especificação da PoC de conformidade com Quarkus Flow, Ollama e HITL sem broker

## Estado deste documento

- **Revisão:** 2026-08-10.
- **Natureza:** especificação do estado implementado e dos limites ainda vigentes.
- **Escopo técnico:** domínio de conformidade do monólito modular <code>simtr-hub</code>.
- **Decisão histórica aceita:** [ADR-0009](../adr/0009-poc-conformidade-quarkus-flow-hitl-em-memoria.md).
- **Evolução durável:** [ADR-0010](../adr/0010-conformidade-couchdb-checkpoint-redis-e-hitl-multipod.md),
  ainda com status **Proposto**.

O código e os testes implementam localmente a evolução durável do ADR-0010. Isso não muda seu
status: a integração real contra Cosmos DB for NoSQL e uma decisão humana explícita de aceitação
continuam pendentes. Portanto, este documento não declara prontidão para produção.

Esta revisão substitui a redação anterior orientada a planejamento. Verbos no presente descrevem
o estado implementado; expressões como “pendente”, “proposto” e “gate” identificam trabalho ou
decisão ainda não concluídos.

## Sumário

1. [Objetivo e resultado da PoC](#1-objetivo-e-resultado-da-poc)
2. [Escopo e limites](#2-escopo-e-limites)
3. [Visão arquitetural](#3-visão-arquitetural)
4. [Decisões arquiteturais explícitas](#4-decisões-arquiteturais-explícitas)
5. [C4 Component](#5-c4-component)
6. [Fluxo ponta a ponta](#6-fluxo-ponta-a-ponta)
7. [Identidades e correlação](#7-identidades-e-correlação)
8. [Modelo de domínio persistido](#8-modelo-de-domínio-persistido)
9. [Schema lógico do backend documental](#9-schema-lógico-do-backend-documental)
10. [Onde os documentos são gravados](#10-onde-os-documentos-são-gravados)
11. [Concorrência, imutabilidade e idempotência](#11-concorrência-imutabilidade-e-idempotência)
12. [CloudEvents, feed nativo e ausência de broker](#12-cloudevents-feed-nativo-e-ausência-de-broker)
13. [Quarkus Flow e checkpoint técnico](#13-quarkus-flow-e-checkpoint-técnico)
14. [Agente Ollama, validação e fallback](#14-agente-ollama-validação-e-fallback)
15. [Human-in-the-Loop](#15-human-in-the-loop)
16. [API REST e página estática](#16-api-rest-e-página-estática)
17. [Configuração e seleção por ambiente](#17-configuração-e-seleção-por-ambiente)
18. [Segurança e dados sensíveis](#18-segurança-e-dados-sensíveis)
19. [Observabilidade e saúde](#19-observabilidade-e-saúde)
20. [Containers, Kubernetes e failover](#20-containers-kubernetes-e-failover)
21. [Testes e evidências](#21-testes-e-evidências)
22. [Limitações e gates pendentes](#22-limitações-e-gates-pendentes)
23. [Mapa para o código](#23-mapa-para-o-código)
24. [Referências](#24-referências)

---

## 1. Objetivo e resultado da PoC

A PoC demonstra uma análise documental de conformidade que:

1. recebe um texto identificado e a referência de um checklist;
2. consulta o checklist pela capacidade existente do domínio de conformidade;
3. congela o checklist consultado em um documento imutável;
4. aplica o checklist ao texto com um agente sequencial LangChain4j sobre Ollama local;
5. valida deterministicamente a resposta do modelo;
6. persiste o resultado preliminar;
7. pausa uma instância Quarkus Flow em espera por revisão humana;
8. recebe a revisão por REST, persiste-a e a entrega ao Flow por feed nativo;
9. consolida e persiste o resultado final;
10. disponibiliza o estado por polling HTTP.

O fluxo usa CloudEvents v1 e Human-in-the-Loop real com <code>emitJson</code> e
<code>listen</code>, sem Kafka, AMQP ou outro broker externo.

## 2. Escopo e limites

### 2.1 Incluído

- Quarkus 3.33.2.1 e Java 25;
- Quarkus Flow 0.10.2;
- Quarkus LangChain4j 1.11.2;
- agente sequencial local com Ollama;
- consulta de checklist por porta existente;
- saída estruturada e validação Java;
- SmallRye Fault Tolerance no adapter Ollama;
- revisão humana obrigatória;
- persistência documental reativa;
- CouchDB no desenvolvimento, testes de integração e ambientes locais da PoC;
- adapter Azure Cosmos DB for NoSQL destinado a produção;
- Redis/Valkey como provider de checkpoint do Flow;
- feed <code>_changes</code> no CouchDB e Change Feed Processor no Cosmos;
- Compose de uma réplica e kind com duas réplicas;
- página HTML/CSS/JavaScript sem framework e polling HTTP;
- telemetria sanitizada e health/readiness por dependência.

### 2.2 Não incluído

- Kafka, AMQP, WebSocket ou broker externo;
- JPA, banco relacional, MongoDB ou MVStore;
- upload binário, OCR, chunking, vetorização ou RAG;
- autenticação específica nova para os três endpoints da PoC;
- edição do checklist pelo workflow;
- criação de apontamentos pelo modelo;
- garantia de alta disponibilidade do CouchDB ou do Valkey;
- recuperação depois da perda dos próprios backends;
- implantação do Cosmos ou do Cosmos Emulator no kind local;
- aprovação produtiva do adapter Cosmos sem o gate real;
- retenção, arquivamento ou descarte regulatório além da ausência de TTL automático nesta PoC.

## 3. Visão arquitetural

A capacidade pertence ao bounded context <code>conformidade</code> dentro do monólito modular.
REST e página são adapters de entrada. Os casos de uso e o Flow ficam na aplicação. Domínio,
portas e modelos não dependem de CouchDB, Cosmos, Redis, CloudEvents, Flow, LangChain4j ou Ollama.

Há três categorias de estado, intencionalmente separadas:

| Categoria | Conteúdo | Sistema responsável |
|---|---|---|
| documentos de negócio | texto, checklist congelado, resultados, revisão, falha, emissões e projeção | CouchDB ou Cosmos DB for NoSQL |
| checkpoint técnico | estado serializado e espera da instância Quarkus Flow | Redis/Valkey |
| identidade do worker | ownership e readiness de workers em múltiplos pods | Kubernetes Leases |

O backend documental é o sistema de registro consultado pela API. Redis/Valkey não é usado pela
API de polling e não contém os documentos de negócio.

## 4. Decisões arquiteturais explícitas

### 4.1 Catálogo

| ID | Decisão | Motivação | Consequência | Estado e origem |
|---|---|---|---|---|
| DA-01 | manter monólito modular, package by domain e hexagonal pragmática | proteger responsabilidades sem criar rede interna | adapters apontam para portas; núcleo não conhece fornecedores | aceita, ADR-0001 |
| DA-02 | a análise pertence a <code>conformidade</code> | checklist é linguagem e responsabilidade desse domínio | não se cria domínio técnico <code>parametrizacao</code> | aceita, ADR-0002 |
| DA-03 | Flow orquestra portas na camada de aplicação | REST é borda, não API interna | workflow não chama Resource nem REST Client MTR diretamente | aceita, ADR-0003 |
| DA-04 | cada borda mantém DTO e mapper próprios | REST, MTR, simulador, Ollama e persistência evoluem separadamente | não há DTO único atravessando as camadas | aceita, ADR-0004 |
| DA-05 | reutilizar <code>ConsultarChecklist</code> e a FT existente do MTR | evitar duplicação e preservar classificação de falhas | workflow depende da porta; seleção MTR/simulador permanece configurável | aceita, ADR-0005 |
| DA-06 | preservar contratos e sinais por testes | mudanças internas podem alterar API e telemetria | REST, propriedades, spans e ArchUnit são guardrails | aceita, ADR-0006 |
| DA-07 | usar Quarkus Flow e agente sequencial sobre Ollama, sem broker | comprovar Flow, agentic e HITL localmente | versões Quarkiverse ficam explicitamente fixadas | aceita, ADR-0009 |
| DA-08 | Human-in-the-Loop é obrigatório | resultado do modelo não é decisão final | toda conclusão normal exige uma revisão humana persistida | aceita, ADR-0009 |
| DA-09 | usar <code>EventPublisher</code> documental e <code>EventConsumer</code> por feed nativo | obter entrega durável sem Kafka | semântica pelo menos uma vez e idempotência obrigatória | proposta implementada localmente, ADR-0010 |
| DA-10 | separar documentos de negócio de checkpoints do Flow | o formato técnico do Flow não é modelo de negócio | CouchDB/Cosmos guardam documentos; Redis/Valkey guarda apenas checkpoint | proposta implementada localmente, ADR-0010 |
| DA-11 | usar uma porta documental reativa e neutra | evitar vazamento de fornecedor e bloqueio do event loop | escritas retornam <code>Uni&lt;Void&gt;</code>; consulta retorna <code>Uni&lt;Optional&lt;...&gt;&gt;</code> | proposta implementada localmente, ADR-0010 |
| DA-12 | CouchDB em DES e Cosmos DB for NoSQL em PRD | JSON, MVCC e feed nativo existem nos dois backends | mesmo contrato lógico, dois adapters e gate Cosmos real pré-PRD | proposta, ADR-0010 |
| DA-13 | modelar uma projeção mutável e fatos imutáveis | polling precisa de visão atual; auditoria precisa de fatos estáveis | somente a projeção é substituída por compare-and-set | proposta implementada localmente, ADR-0010 |
| DA-14 | particionar Cosmos por <code>correlationId</code> | manter documentos da mesma análise na mesma chave lógica | infraestrutura Cosmos deve criar <code>/correlationId</code> como partition key | proposta, ADR-0010 |
| DA-15 | reduzir contexto Flow a identidades, referências e hashes | evitar duplicar texto e resultados no checkpoint | etapas recarregam conteúdo pelo backend documental | proposta implementada localmente, ADR-0010 |
| DA-16 | usar SHA-256 canônico e schema versionado | validar integridade de referências e evolução do JSON | hash é string hexadecimal minúscula de 64 caracteres; <code>versaoSchema</code> é <code>Short</code> no Java e inteiro no JSON | decisão humana registrada |
| DA-17 | Cosmos produtivo usa Entra ID e RBAC de dados | eliminar segredo estático de banco | chave e connection string provocam falha de startup | proposta implementada no adapter, gate operacional pendente |
| DA-18 | múltiplos pods usam Redis compartilhado e Kubernetes Leases | restaurar o worker lógico correto após troca de pod | readiness exige Lease; failover não torna os próprios backends altamente disponíveis | proposta comprovada localmente, ADR-0010 |

### 4.2 Relação entre ADR-0009 e ADR-0010

O ADR-0009 continua <code>Aceito</code> como decisão histórica que autorizou a PoC inicialmente
volátil. O ADR-0010 implementou localmente uma evolução que removeu a volatilidade da aplicação,
mas permanece <code>Proposto</code> porque:

- o contrato Cosmos foi comprovado com SDK mockado, não com serviço real;
- a integração opt-in contra Emulator ou conta não produtiva ainda é gate pré-PRD;
- nenhuma decisão humana alterou o ADR-0010 para <code>Aceito</code>;
- as provas mantiveram CouchDB e Valkey disponíveis, sem testar falha desses serviços.

Não se deve marcar o ADR-0009 como substituído nem o ADR-0010 como aceito por inferência.

## 5. C4 Component

O diagrama abaixo é C4 nível 3 para o container Quarkus <code>simtr-hub</code>. CouchDB e Cosmos
aparecem simultaneamente apenas para documentar as alternativas; um único backend é selecionado no
startup.

~~~mermaid
C4Component
    title PoC de conformidade — C4 nível 3 (componentes)

    Person(operador, "Pessoa revisora", "Inicia a análise, acompanha o polling e revisa o resultado")
    System_Ext(mtr, "MTR", "Fornece o checklist quando a integração real está habilitada")
    System_Ext(ollama, "Ollama local", "Executa o modelo llama3.2:3b")
    System_Ext(kubeapi, "API Kubernetes", "Mantém Leases de worker no perfil kubernetes")

    Container_Boundary(hub, "simtr-hub — aplicação Quarkus") {
        Component(pagina, "Página estática", "HTML, CSS e JavaScript", "Formulário, polling e revisão; não persiste documentos no navegador")
        Component(rest, "AnaliseConformidadeResource", "Quarkus REST", "POST, GET e PUT; DTOs e mapper da borda REST")
        Component(casos, "Casos de uso de análise", "Aplicação / Mutiny", "Inicia, consulta e reserva a revisão por portas")
        Component(flow, "AnaliseConformidadeFlow", "Quarkus Flow", "Consulta checklist, chama agente, emite, espera e consolida")
        Component(checklist, "ConsultarChecklist", "Porta de aplicação", "Reutiliza a capacidade MTR/simulador existente")
        Component(checklistadapter, "Adapters de checklist", "REST Client MTR / simulador local", "Seleciona a origem por configuração e traduz contratos")
        Component(agentport, "AnalisarTextoComChecklist", "Porta de saída", "Contrato único da análise agentic")
        Component(agent, "OllamaAnaliseConformidadeAdapter", "LangChain4j Agentic + SmallRye FT", "Agente sequencial, saída tipada, validação e fallback")
        Component(docport, "ArmazenarEstadoAnaliseConformidade", "Porta reativa", "Contrato documental neutro")
        Component(docstore, "DocumentoAnaliseConformidadeStore", "Aplicação documental", "Schema, hashes, fatos imutáveis e projeção")
        Component(couchadapter, "Adapter CouchDB", "HTTP/JSON", "MVCC por _rev e feed _changes")
        Component(cosmosadapter, "Adapter Cosmos DB", "Azure SDK v4", "Concorrência por _etag e Change Feed Processor")
        Component(publisher, "EventPublisher documental", "SPI Quarkus Flow", "Persiste emissões referenciais e atualiza a projeção")
        Component(feed, "Feed nativo + EventConsumer", "SPI Quarkus Flow", "Transforma revisão persistida em CloudEvent e entrega ao listen")
    }

    ContainerDb(couchdb, "CouchDB", "JSON document database", "Backend DES/local; documentos, projeção e cursor _changes")
    ContainerDb(cosmos, "Azure Cosmos DB for NoSQL", "JSON document database", "Backend PRD proposto; container de dados e container de leases")
    ContainerDb(valkey, "Redis/Valkey", "Redis protocol", "Checkpoint técnico opaco do Quarkus Flow")

    Rel(operador, pagina, "Usa")
    Rel(pagina, rest, "POST/GET/PUT", "HTTP/JSON")
    Rel(rest, casos, "Aciona portas de entrada", "Uni")
    Rel(casos, docport, "Persiste e consulta estado")
    Rel(casos, flow, "Cria e inicia instância")
    Rel(flow, checklist, "Consulta checklist")
    Rel(checklist, checklistadapter, "Usa a implementação selecionada")
    Rel(checklistadapter, mtr, "Consulta quando o simulador está desabilitado", "HTTP/JSON")
    Rel(flow, agentport, "Solicita análise")
    Rel(agentport, agent, "Implementado por")
    Rel(agent, ollama, "Prompt e structured output", "HTTP/JSON")
    Rel(flow, docport, "Carrega e grava referências")
    Rel(docport, docstore, "Implementado por")
    Rel(docstore, couchadapter, "Seleciona em DES/dev/test")
    Rel(docstore, cosmosadapter, "Seleciona em PRD")
    Rel(couchadapter, couchdb, "Cria, lê e substitui", "HTTP/JSON")
    Rel(cosmosadapter, cosmos, "Cria, lê e substitui", "Azure SDK v4")
    Rel(flow, publisher, "Publica CloudEvents referenciais")
    Rel(publisher, docport, "Registra emissão e projeta estado")
    Rel(couchdb, feed, "Mudanças de revisão", "_changes")
    Rel(cosmos, feed, "Mudanças de revisão", "Change Feed")
    Rel(feed, flow, "CloudEvent revisão.concluida.v1")
    Rel(flow, valkey, "Checkpoint e restauração", "Redis protocol")
    Rel(flow, kubeapi, "Adquire identidade de worker", "Kubernetes Leases")
~~~

### 5.1 Responsabilidades e fronteiras

| Componente | Responsabilidade | Não faz |
|---|---|---|
| página estática | interação, polling e edição humana | não usa LocalStorage como sistema de registro |
| Resource REST | valida contrato HTTP e mapeia DTOs | não chama CouchDB, Cosmos, Ollama ou MTR diretamente |
| casos de uso | coordenam portas e transições de entrada | não dependem de DTO de fornecedor |
| Flow | orquestra etapas e espera HITL | não guarda texto/checklist/resultados completos no contexto |
| adapter Ollama | agente, FT, classificação de falhas e fallback | não persiste documentos |
| porta/store documental | contrato canônico, fatos, projeção e integridade | não expõe <code>_rev</code>, <code>_etag</code> ou SDK ao núcleo |
| EventPublisher | persiste fatos de emissão e atualiza projeção | não usa connector ou broker |
| feed/EventConsumer | observa revisão persistida e retoma o Flow | não decide o resultado de negócio |
| Redis/Valkey | checkpoint técnico do Flow | não é fonte do polling nem repositório de documentos |

## 6. Fluxo ponta a ponta

~~~mermaid
sequenceDiagram
    actor Pessoa
    participant UI as Página estática
    participant API as API de conformidade
    participant Docs as Backend documental
    participant Flow as Quarkus Flow
    participant Redis as Redis/Valkey
    participant Checklist as ConsultarChecklist
    participant MTR as MTR ou simulador
    participant Agente as LangChain4j/Ollama
    participant Publisher as EventPublisher
    participant Feed as _changes ou Change Feed

    Pessoa->>UI: informa documento, texto e checklist
    UI->>API: POST /analises
    API->>Docs: cria entrada-analise e projecao-analise
    API->>Flow: cria e inicia a instância
    Flow->>Redis: checkpoint técnico
    API-->>UI: 202 + cinco identidades

    Flow->>Checklist: executar comando
    Checklist->>MTR: consulta configurada
    MTR-->>Checklist: checklist
    Checklist-->>Flow: checklist
    Flow->>Docs: grava checklist-analise e referência na projeção

    Flow->>Docs: recarrega texto e checklist por referência
    Flow->>Agente: entrada estruturada
    Agente-->>Flow: resultado ou fallback tipado
    Flow->>Docs: grava resultado-preliminar
    Flow->>Publisher: emite revisao.solicitada.v1
    Publisher->>Docs: grava emissao-cloud-event e muda projeção para AGUARDANDO_REVISAO
    Flow->>Redis: checkpoint WAITING

    loop polling
        UI->>API: GET /analises/{instanceId}
        API->>Docs: consulta projeção e fatos referenciados
        API-->>UI: estado atual
    end

    Pessoa->>UI: revisa e confirma
    UI->>API: PUT /analises/{instanceId}/revisao
    API->>Docs: reserva referência e grava revisao-humana imutável
    API-->>UI: 202
    Docs-->>Feed: mudança persistida
    Feed->>Flow: CloudEvent revisao.concluida.v1 com referência
    Flow->>Docs: carrega revisão e consolida resultado
    Flow->>Docs: grava resultado-final
    Flow->>Publisher: emite analise.concluida.v1
    Publisher->>Docs: grava emissão e muda projeção para CONCLUIDA
    Flow->>Redis: checkpoint terminal

    UI->>API: GET /analises/{instanceId}
    API-->>UI: CONCLUIDA + resultado final
~~~

Uma entrega repetida pelo feed é esperada. A conclusão lógica permanece única porque documento,
projeção e workflow validam identidade, conteúdo e estado.

## 7. Identidades e correlação

| Identidade | Tipo | Origem | Uso |
|---|---|---|---|
| <code>correlationId</code> | <code>String</code>, UUID textual | gerado pelo Hub | correlação ponta a ponta, IDs documentais e partition key do Cosmos |
| <code>instanceId</code> | <code>String</code> | Quarkus Flow | path da API, checkpoint e correlação do <code>listen</code> |
| <code>identificadorDocumento</code> | <code>String</code> | informado no POST | identidade negocial do texto/documento analisado |
| <code>identificadorChecklist</code> | <code>Long</code> | informado e confirmado pelo checklist | identidade negocial do checklist |
| <code>versaoChecklist</code> | <code>Integer</code> | informado e confirmado pelo checklist | versão aplicada |

As cinco identidades aparecem na resposta do POST, no GET e na página em todos os estados. Elas
também estão em todos os documentos negociais.

No CloudEvent, os nomes de extensões são minúsculos conforme o SDK:

- <code>flowinstanceid</code>;
- <code>flowtaskid</code>, quando fornecido pelo Flow;
- <code>correlationid</code>.

<code>correlationId</code> continua em camelCase nos JSONs da API e do backend documental.

## 8. Modelo de domínio persistido

### 8.1 Solicitação

~~~text
SolicitacaoAnaliseConformidade
├── correlationId: String
├── identificadorDocumento: String
├── texto: String (1..20000 caracteres)
├── identificadorChecklist: Long positivo
└── versaoChecklist: Integer positivo
~~~

### 8.2 Checklist congelado

~~~text
Checklist
├── nome: String
├── identificadorNegocial: Long
├── versao: Integer
├── dataHoraCriacao: String
├── dataHoraUltimaAlteracao: String
├── verificacaoPrevia: Boolean
├── orientacaoOperador: String
└── apontamentos: List<ApontamentoChecklist>
    ├── identificadorNegocial: Long
    ├── nome: String
    ├── descricao: String
    ├── orientacaoOperador: String
    ├── indicadorReanalise: Boolean
    └── sequenciaApresentacao: Integer
~~~

O snapshot é a cópia efetivamente analisada; alterações posteriores no MTR não mudam uma análise
já iniciada.

### 8.3 Resultado

~~~text
ResultadoAnaliseConformidade
├── identificadorChecklist: Long
├── versaoChecklist: Integer
├── nomeChecklist: String
├── resumo: String
├── origem: AGENTE | FALLBACK_TECNICO | REVISAO_HUMANA
└── apontamentos: List<ResultadoApontamentoConformidade>
    ├── identificadorApontamento: Long
    ├── nomeApontamento: String
    ├── parecer: CONFORME | INCONFORME | INCONCLUSIVO | NAO_ANALISADO
    ├── justificativa: String
    ├── evidencia: String ou null
    └── confianca: Double entre 0.0 e 1.0
~~~

### 8.4 Revisão humana

~~~text
RevisaoHumanaConformidade
├── observacao: String ou null
└── apontamentos: List<ResultadoApontamentoConformidade>
~~~

Na revisão, IDs, nomes e confiança permanecem imutáveis. A pessoa pode alterar parecer,
justificativa e evidência.

### 8.5 Projeção consultável

~~~text
VisaoAnaliseConformidade
├── cinco identidades
├── status: EM_PROCESSAMENTO | AGUARDANDO_REVISAO | CONCLUIDA | FALHOU
├── resultadoPreliminar: Resultado ou null
├── resultadoFinal: Resultado ou null
└── mensagemErro: String sanitizada ou null
~~~

## 9. Schema lógico do backend documental

O schema é documental, não relacional. Não existem tabelas nem foreign keys físicas. O
<code>DocumentoAnaliseConformidadeStore</code> cria e valida o contrato
<code>versaoSchema = 1</code>; CouchDB e Cosmos armazenam o mesmo JSON canônico.

### 9.1 Relações lógicas

~~~mermaid
erDiagram
    ENTRADA_ANALISE ||--|| PROJECAO_ANALISE : "inicia"
    PROJECAO_ANALISE ||--o| CHECKLIST_ANALISE : "checklistRef"
    PROJECAO_ANALISE ||--o| RESULTADO_PRELIMINAR : "resultadoPreliminarRef"
    PROJECAO_ANALISE ||--o| REVISAO_HUMANA : "revisaoRef"
    PROJECAO_ANALISE ||--o| RESULTADO_FINAL : "resultadoFinalRef"
    PROJECAO_ANALISE ||--o| FALHA_ANALISE : "falhaRef"
    PROJECAO_ANALISE ||--o{ EMISSAO_CLOUD_EVENT : "correlationId"

    ENTRADA_ANALISE {
        string id PK
        string correlationId
        string instanceId
        string identificadorDocumento
        long identificadorChecklist
        int versaoChecklist
        short versaoSchema
        string texto
    }

    PROJECAO_ANALISE {
        string id PK
        string correlationId
        string instanceId
        string identificadorDocumento
        long identificadorChecklist
        int versaoChecklist
        short versaoSchema
        string status
        string checklistRef FK
        string checklistHash
        string resultadoPreliminarRef FK
        string revisaoRef FK
        string resultadoFinalRef FK
        string falhaRef FK
    }

    CHECKLIST_ANALISE {
        string id PK
        string correlationId
        string instanceId
        short versaoSchema
        string hashConteudo
        object checklist
    }

    RESULTADO_PRELIMINAR {
        string id PK
        string correlationId
        string instanceId
        short versaoSchema
        string hashConteudo
        object resultado
    }

    REVISAO_HUMANA {
        string id PK
        string correlationId
        string instanceId
        short versaoSchema
        string hashConteudo
        object revisao
    }

    RESULTADO_FINAL {
        string id PK
        string correlationId
        string instanceId
        short versaoSchema
        string hashConteudo
        object resultado
    }

    FALHA_ANALISE {
        string id PK
        string correlationId
        string instanceId
        short versaoSchema
        string mensagem
    }

    EMISSAO_CLOUD_EVENT {
        string id PK
        string correlationId
        string instanceId
        short versaoSchema
        string eventoId
        string eventoTipo
        string documentoRef
        string hashConteudo
    }
~~~

Os campos marcados como FK são referências lógicas. O backend não aplica integridade referencial;
o store a valida ao gravar e carregar.

### 9.2 Envelope comum

Todo documento negocial contém:

~~~json
{
  "id": "<prefixo>-<sha256>",
  "tipo": "<discriminador>",
  "versaoSchema": 1,
  "correlationId": "<UUID>",
  "instanceId": "<ID da instância Flow>",
  "identificadorDocumento": "DOC-2026-000123",
  "identificadorChecklist": 1000012583,
  "versaoChecklist": 1
}
~~~

No CouchDB, o servidor acrescenta <code>_id</code> e <code>_rev</code>. O <code>_id</code>
corresponde ao campo canônico <code>id</code>. No Cosmos, o servidor acrescenta metadados como
<code>_etag</code>, <code>_rid</code>, <code>_ts</code> e <code>_self</code>. Esses metadados são
removidos antes de o documento atravessar o adapter.

### 9.3 Catálogo de documentos

| <code>tipo</code> | ID determinístico | Conteúdo específico | Mutabilidade |
|---|---|---|---|
| <code>entrada-analise</code> | <code>entrada-</code> + SHA-256 de <code>correlationId</code> | <code>texto</code> completo recebido | imutável |
| <code>projecao-analise</code> | <code>projecao-</code> + SHA-256 de <code>instanceId</code> | status, hashes e referências | mutável por compare-and-set |
| <code>checklist-analise</code> | <code>checklist-</code> + SHA-256 de <code>correlationId</code> | <code>hashConteudo</code> e <code>checklist</code> | imutável |
| <code>resultado-preliminar</code> | <code>resultado-preliminar-</code> + SHA-256 de <code>correlationId</code> | <code>hashConteudo</code> e <code>resultado</code> | imutável |
| <code>revisao-humana</code> | <code>revisao-</code> + SHA-256 de <code>correlationId</code> | <code>hashConteudo</code> e <code>revisao</code> | imutável |
| <code>resultado-final</code> | <code>resultado-final-</code> + SHA-256 de <code>correlationId</code> | <code>hashConteudo</code> e <code>resultado</code> | imutável |
| <code>falha-analise</code> | <code>falha-</code> + SHA-256 de <code>correlationId</code> | <code>mensagem</code> sanitizada | imutável |
| <code>emissao-cloud-event</code> | <code>emissao-</code> + SHA-256 de <code>eventoId</code> | tipo do evento e referência do documento | imutável |

Todos os IDs usam SHA-256 hexadecimal minúsculo. O hash de conteúdo é calculado sobre a
serialização JSON do objeto de negócio, não sobre o envelope completo.

### 9.4 Exemplo de projeção concluída

~~~json
{
  "id": "projecao-<sha256-instanceId>",
  "tipo": "projecao-analise",
  "versaoSchema": 1,
  "correlationId": "<correlationId>",
  "instanceId": "<instanceId>",
  "identificadorDocumento": "DOC-2026-000123",
  "identificadorChecklist": 1000012583,
  "versaoChecklist": 1,
  "status": "CONCLUIDA",
  "checklistRef": "checklist-<sha256-correlationId>",
  "checklistHash": "<sha256-conteudo-checklist>",
  "resultadoPreliminarRef": "resultado-preliminar-<sha256-correlationId>",
  "revisaoRef": "revisao-<sha256-correlationId>",
  "resultadoFinalRef": "resultado-final-<sha256-correlationId>"
}
~~~

A resposta do GET materializa a visão consultando a projeção e carregando os fatos referenciados.
Os payloads completos não são copiados para a projeção.

### 9.5 Cursor e leases do feed

O cursor CouchDB é técnico e separado dos documentos negociais:

~~~json
{
  "_id": "_local/simtr-flow-revisao-v1",
  "tipo": "cursor-feed-revisao",
  "versaoSchema": 1,
  "lastSeq": "<sequência do _changes>"
}
~~~

Por ser um documento local do CouchDB, ele não aparece em <code>_all_docs</code>. No Cosmos, o
Change Feed Processor usa o container de leases <code>analises-leases</code> e o prefixo
<code>simtr-conformidade-revisao-v1</code>. Esses leases também são técnicos e não fazem parte do
schema negocial.

## 10. Onde os documentos são gravados

### 10.1 Mapa lógico e físico

| Ambiente | Documentos de negócio | Local físico | Checkpoint Flow | Observação |
|---|---|---|---|---|
| <code>quarkus:dev</code> | database CouchDB configurado, padrão <code>conformidade</code> | volume lógico <code>couchdb-conformidade-data</code> do Compose Dev Services, montado em <code>/opt/couchdb/data</code> | Redis/Valkey externo quando necessário à prova durável | o Docker acrescenta o prefixo do projeto Compose; testes não reutilizam esse volume |
| testes CouchDB | database de container efêmero | filesystem descartável do container de teste | provider de teste conforme cenário | isolado do desenvolvimento |
| Docker Compose da PoC | database CouchDB <code>conformidade</code> | volume Docker <code>simtr-hub-poc_couchdb-conformidade-data</code> em <code>/opt/couchdb/data</code> | serviço <code>valkey:6379</code>, sem snapshot em disco | host acessa CouchDB em <code>127.0.0.1:15984</code> por padrão |
| Kubernetes kind | database CouchDB <code>conformidade</code> | PVC <code>couchdb-conformidade-data</code>, 1 GiB, montado em <code>/opt/couchdb/data</code> | Service <code>valkey:6379</code>, sem persistência própria | CouchDB é StatefulSet de uma réplica |
| PRD proposto | database Cosmos <code>conformidade</code>, container <code>analises</code> | armazenamento gerenciado pelo Azure Cosmos DB for NoSQL | Redis compatível e durável conforme plataforma de PRD, ainda não especificada nesta PoC | partition key obrigatória <code>/correlationId</code> |

Os nomes Cosmos podem ser sobrescritos por configuração, mas os defaults implementados são:

- database: <code>conformidade</code>;
- container de dados: <code>analises</code>;
- container técnico do Change Feed: <code>analises-leases</code>.

Nenhum documento é salvo como arquivo JSON no repositório ou no diretório de trabalho. No
ambiente local, os bytes ficam em volume administrado pelo Docker ou no PVC do kind; em PRD,
ficariam no serviço gerenciado do Cosmos. O kind não publica o CouchDB no host por padrão: a
inspeção exige <code>kubectl port-forward</code>.

### 10.2 Resposta direta: onde fica cada conteúdo

| Conteúdo | Onde é gravado |
|---|---|
| texto enviado no POST | documento <code>entrada-analise</code> no CouchDB ou Cosmos selecionado |
| checklist retornado pelo MTR/simulador | documento <code>checklist-analise</code> no mesmo backend |
| resultado do agente ou fallback | documento <code>resultado-preliminar</code> |
| resposta da pessoa revisora | documento <code>revisao-humana</code> |
| resultado consolidado | documento <code>resultado-final</code> |
| estado mostrado pelo polling | documento <code>projecao-analise</code>, com referências |
| erro terminal | documento <code>falha-analise</code> |
| fatos de emissão do Flow | documentos <code>emissao-cloud-event</code> |
| cursor CouchDB | documento local <code>_local/simtr-flow-revisao-v1</code> no mesmo database |
| leases do Change Feed Cosmos | container <code>analises-leases</code> |
| espera e execução do Flow | Redis/Valkey, em formato técnico pertencente ao Quarkus Flow |
| identidade de worker multipod | objetos Lease na API Kubernetes |

### 10.3 Onde os documentos não são gravados

- não são gravados no browser ou em LocalStorage;
- não são gravados no Redis/Valkey;
- não são gravados em banco relacional;
- não são enviados a Kafka ou outro broker;
- não são gravados como payload completo nos CloudEvents;
- não são gravados nos logs por desenho da aplicação;
- não ficam no contexto do Flow, que carrega o conteúdo por referência quando necessário.

O Ollama recebe temporariamente texto e checklist para inferência. A aplicação não usa o Ollama
como repositório documental.

### 10.4 Acesso e inspeção

Os arquivos sob <code>/opt/couchdb/data</code> pertencem ao CouchDB e não devem ser editados
diretamente. A inspeção deve usar API HTTP, Fauxton ou ferramenta compatível, sempre com dados
sintéticos. O procedimento operacional está em
[guia-verificacao-poc-conformidade.md](guia-verificacao-poc-conformidade.md).

## 11. Concorrência, imutabilidade e idempotência

### 11.1 Projeção

A projeção é o único documento negocial substituído:

- CouchDB usa <code>_rev</code>;
- Cosmos usa <code>_etag</code> com <code>If-Match</code>;
- conflito 409/412 é traduzido para conflito de transição;
- versões nativas permanecem opacas fora do adapter.

### 11.2 Fatos

Fatos são criados com IDs determinísticos:

- primeira criação grava o documento;
- repetição do mesmo ID e mesmo JSON é idempotente;
- mesmo ID com conteúdo diferente é conflito;
- revisão contraditória não sobrescreve a anterior;
- emissão repetida não duplica o efeito lógico.

### 11.3 Semântica do feed

<code>_changes</code> e Change Feed possuem entrega pelo menos uma vez. A aplicação não promete
exactly-once físico. Ela garante conclusão lógica única pela combinação de:

1. ID determinístico;
2. fato imutável;
3. compare-and-set da projeção;
4. validação de <code>instanceId</code>, <code>correlationId</code>, referência e hash;
5. estado esperado do Flow;
6. emissão final idempotente.

Documento de revisão inválido é ignorado de forma observável e não bloqueia mudanças posteriores.

## 12. CloudEvents, feed nativo e ausência de broker

### 12.1 Tipos

| Evento | Direção | Documento referenciado |
|---|---|---|
| <code>br.gov.caixa.simtr.conformidade.revisao.solicitada.v1</code> | Flow para EventPublisher | resultado preliminar |
| <code>br.gov.caixa.simtr.conformidade.revisao.concluida.v1</code> | feed para <code>listen</code> | revisão humana |
| <code>br.gov.caixa.simtr.conformidade.analise.concluida.v1</code> | Flow para EventPublisher | resultado final |

### 12.2 Envelope referencial

O <code>data</code> contém somente:

~~~json
{
  "documentoRef": "revisao-<sha256>",
  "hashConteudo": "<sha256 hexadecimal minúsculo>",
  "versaoSchema": 1
}
~~~

O envelope usa CloudEvents 1.0, <code>source=urn:simtr-hub:conformidade</code>,
<code>datacontenttype=application/json</code>, <code>time</code> UTC e extensões de correlação.

### 12.3 Caminhos por backend

- CouchDB: <code>_changes</code> em long polling com documento incluído e cursor persistido;
- Cosmos: Change Feed Processor sobre o container <code>analises</code>, com leases separados;
- Flow: <code>FeedNativoEventConsumer</code> entrega o CloudEvent diretamente ao runtime;
- nenhum <code>mp.messaging.*.connector</code> é necessário;
- nenhum tópico, fila ou broker é criado.

## 13. Quarkus Flow e checkpoint técnico

O descriptor implementado possui estas tarefas:

~~~text
consultarChecklist
  -> analisarConformidade
  -> emitirSolicitacaoRevisao
  -> aguardarRevisaoHumana
  -> consolidarRevisaoHumana
  -> emitirAnaliseConcluida
~~~

O <code>listen</code> correlaciona <code>flowinstanceid</code> com o ID da instância esperada.

O contexto persistido pelo Flow contém:

~~~text
ContextoAnaliseConformidadeFlow
├── correlationId
├── identificadorDocumento
├── identificadorChecklist
├── versaoChecklist
└── checklistRef
    ├── documentoRef
    ├── hashConteudo
    └── versaoSchema
~~~

Texto, checklist, resultados e revisão não fazem parte desse contexto. O Flow recarrega a
solicitação e os fatos pelo backend documental.

<code>quarkus-flow-redis</code> é o único provider de checkpoint. No Compose e no kind, o servidor
é Valkey compatível com Redis. A configuração local usa <code>--save ""</code>; portanto, a prova
local cobre restart/failover da aplicação enquanto Valkey permanece disponível, não perda ou
restart do Valkey.

## 14. Agente Ollama, validação e fallback

### 14.1 Composição

A capacidade externa ao workflow é única: <code>AnalisarTextoComChecklist</code>. O adapter usa um
<code>@SequenceAgent</code>:

~~~text
AplicadorChecklistAgent
  -> RevisorCoberturaAgent
~~~

O primeiro agente aplica todos os apontamentos. O segundo revisa cobertura e fundamentação sem
inventar identificadores.

### 14.2 Proteções de prompt

Os prompts:

- tratam texto, checklist e resultado anterior como dados não confiáveis;
- ignoram instruções inseridas dentro do documento;
- proíbem invenção de fatos, IDs e evidências;
- exigem um resultado por apontamento;
- exigem justificativa não vazia;
- limitam evidência a trecho literal do documento ou <code>null</code>;
- exigem JSON estruturado.

DTOs REST, DTOs MTR, credenciais, headers, spans e objetos do Flow não são enviados ao modelo.

### 14.3 Validação determinística

Depois da inferência, o Java valida:

- checklist válido e não vazio;
- identidade e versão do checklist;
- cobertura exata dos apontamentos;
- ausência de IDs extras ou duplicados;
- nomes iguais aos do checklist;
- parecer pertencente ao enum;
- justificativa não vazia;
- confiança finita entre 0 e 1;
- ordem final conforme <code>sequenciaApresentacao</code>.

Na revisão humana, também valida nomes e confiança imutáveis e cobertura exata do resultado
preliminar.

### 14.4 Fault Tolerance efetivo

| Política | Valor implementado |
|---|---|
| timeout | 65 segundos por tentativa |
| retry | até 2 tentativas adicionais |
| delay/jitter | 500 ms / 200 ms |
| circuit breaker | volume 4, razão 0,5, atraso 10 s, 2 sucessos para fechar |
| fallback | resultado completo com <code>NAO_ANALISADO</code>, confiança 0 e origem <code>FALLBACK_TECNICO</code> |

Falhas transitórias de conexão, timeout, resposta incompleta, parsing e HTTP 408/425/429/5xx podem
ser repetidas. Falhas de negócio não são repetidas. O fallback mantém o HITL disponível mesmo sem
Ollama.

### 14.5 Configuração do modelo

- modelo padrão: <code>llama3.2:3b</code>;
- temperatura: <code>0.2</code>;
- contexto: <code>2048</code>;
- formato: JSON;
- Ollama Dev Service desabilitado;
- processo e modelo administrados externamente.

A entrada pública aceita até 20.000 caracteres, maior que o contexto configurado. Não existe
chunking ou truncamento silencioso; falha/truncamento segue a política de FT e fallback.

## 15. Human-in-the-Loop

O fluxo normal não conclui antes da revisão. A máquina de estados é:

~~~mermaid
stateDiagram-v2
    [*] --> EM_PROCESSAMENTO
    EM_PROCESSAMENTO --> AGUARDANDO_REVISAO: resultado preliminar + emissão persistidos
    AGUARDANDO_REVISAO --> CONCLUIDA: revisão válida + resultado final
    EM_PROCESSAMENTO --> FALHOU: falha terminal
    AGUARDANDO_REVISAO --> FALHOU: falha terminal
    CONCLUIDA --> [*]
    FALHOU --> [*]
~~~

Regras:

- a página só habilita revisão em <code>AGUARDANDO_REVISAO</code>;
- a API valida todos os apontamentos;
- IDs, nomes, checklist, documento, correlação e instância não mudam;
- confiança é somente leitura;
- repetição contraditória retorna conflito;
- uma revisão persistida vira CloudEvent apenas pelo feed nativo;
- o resultado final sempre tem origem <code>REVISAO_HUMANA</code>.

## 16. API REST e página estática

Base: <code>/simtr-hub/v1/conformidade/analises</code>.

### 16.1 Iniciar análise

~~~http
POST /simtr-hub/v1/conformidade/analises
Content-Type: application/json
~~~

~~~json
{
  "identificadorDocumento": "DOC-2026-000123",
  "texto": "Texto a ser analisado...",
  "identificadorChecklist": 1000012583,
  "versaoChecklist": 1
}
~~~

Resposta <code>202 Accepted</code>, com <code>Location</code> para o GET:

~~~json
{
  "correlationId": "<UUID>",
  "instanceId": "<ID Flow>",
  "identificadorDocumento": "DOC-2026-000123",
  "identificadorChecklist": 1000012583,
  "versaoChecklist": 1,
  "status": "EM_PROCESSAMENTO"
}
~~~

### 16.2 Consultar

~~~http
GET /simtr-hub/v1/conformidade/analises/{instanceId}
~~~

Retorna <code>200</code> com todas as identidades, status, resultado preliminar, resultado final e
mensagem de erro, incluindo campos nulos explicitamente.

### 16.3 Revisar

~~~http
PUT /simtr-hub/v1/conformidade/analises/{instanceId}/revisao
Content-Type: application/json
~~~

~~~json
{
  "observacao": "Revisão realizada pelo operador.",
  "apontamentos": [
    {
      "identificadorApontamento": 1,
      "nomeApontamento": "Apontamento",
      "parecer": "CONFORME",
      "justificativa": "Justificativa revisada.",
      "evidencia": "Trecho revisado.",
      "confianca": 1.0
    }
  ]
}
~~~

Resposta normal: <code>202 Accepted</code>.

### 16.4 Erros

| Status | Situação |
|---|---|
| 400 | payload inválido |
| 404 | instância inexistente |
| 409 | transição inválida ou revisão contraditória |
| 422 | checklist, resultado ou revisão inconsistente |
| 503 | indisponibilidade técnica |

O corpo segue o contrato de erro REST compartilhado autorizado pela arquitetura.

### 16.5 Página

A página está em
<code>src/main/resources/META-INF/resources/poc-conformidade/index.html</code>. Ela usa Fetch API,
polling e JavaScript puro, sem CDN. Não persiste conteúdo no browser. Estados visuais:

1. formulário;
2. processamento;
3. revisão humana;
4. conclusão ou falha.

## 17. Configuração e seleção por ambiente

### 17.1 Backend documental

~~~properties
conformidade.persistencia.backend=<couchdb|cosmosdb>
%dev.conformidade.persistencia.backend=couchdb
%test.conformidade.persistencia.backend=couchdb
~~~

Fora de dev/test, a seleção é obrigatória. Valor ausente ou desconhecido falha no startup.

### 17.2 CouchDB

~~~properties
conformidade.couchdb.host=<host>
conformidade.couchdb.port=5984
conformidade.couchdb.database=conformidade
conformidade.couchdb.username=<externo>
conformidade.couchdb.password=<externo>
~~~

### 17.3 Cosmos DB for NoSQL

~~~properties
conformidade.cosmos.endpoint=<https obrigatório>
conformidade.cosmos.database=conformidade
conformidade.cosmos.container=analises
conformidade.cosmos.lease-container=analises-leases
~~~

O adapter não cria database, container ou partition key. A infraestrutura deve provisionar:

- container <code>analises</code> com partition key <code>/correlationId</code>;
- container <code>analises-leases</code> compatível com Change Feed Processor;
- RBAC de plano de dados para a identidade da aplicação.

### 17.4 Redis/Valkey

Compose e kind usam:

~~~text
QUARKUS_REDIS_HOSTS=redis://valkey:6379
~~~

Essa configuração atende ao provider do Flow, não à porta documental.

## 18. Segurança e dados sensíveis

### 18.1 Documentos

O backend documental contém texto potencialmente sensível, checklist, pareceres, justificativas e
evidências. Portanto:

- credenciais não podem ser versionadas;
- inspeção operacional deve usar somente dados sintéticos;
- saídas completas não devem ser anexadas a logs, issues, chats ou commits;
- CouchDB deve ficar restrito à rede interna;
- documentos não recebem TTL silencioso nesta PoC;
- retenção produtiva exige decisão própria.

### 18.2 Cosmos

Em PRD:

- autenticação por <code>DefaultAzureCredential</code>;
- Managed Identity ou Workload Identity;
- RBAC de plano de dados de menor privilégio;
- endpoint HTTPS;
- chave e connection string proibidas;
- presença de <code>COSMOS_KEY</code>, <code>AZURE_COSMOS_KEY</code> ou connection string provoca
  falha.

### 18.3 Prompt injection e telemetria

O texto e o checklist são dados não confiáveis. Prompts deixam explícito que instruções internas
ao documento não alteram o papel do agente. Texto, prompt, resposta, credenciais e headers de
autenticação não entram na telemetria padrão.

O perfil <code>poc</code> pode habilitar logs integrais do LangChain4j e deve ser usado apenas com
dados sintéticos em ambiente controlado.

## 19. Observabilidade e saúde

São registrados spans, logs estruturados e eventos com:

- <code>instanceId</code> e <code>correlationId</code>;
- identificador e versão do checklist;
- quantidade de apontamentos;
- modelo e origem do resultado;
- status;
- backend e operação documental;
- resultado do feed, replay e evento;
- worker lógico.

Não são registrados texto, prompt, resposta completa, revisão, credenciais ou segredo.

Readiness separa:

- aplicação iniciada;
- backend documental acessível;
- Redis/Valkey disponível ao Flow;
- Lease adquirida no perfil Kubernetes.

Um pod sem Lease não recebe tráfego.

## 20. Containers, Kubernetes e failover

### 20.1 Docker Compose

<code>compose-poc.yml</code> executa:

- uma réplica do <code>simtr-hub</code>;
- CouchDB 3.5.2 com volume durável;
- Valkey 7.2 sem snapshot próprio;
- verificação do Ollama no host.

O Compose prova restart da aplicação enquanto CouchDB e Valkey permanecem ativos.

### 20.2 Kubernetes kind

Os manifests em <code>k8s/poc</code> executam:

- duas réplicas da aplicação;
- CouchDB em StatefulSet de uma réplica e PVC;
- Valkey compartilhado;
- Ollama no host;
- ServiceAccount, Role e RoleBinding namespaced;
- Leases do Quarkus Flow;
- readiness condicionada à Lease.

As provas locais cobrem:

- revisão recebida por réplica diferente;
- substituição do owner enquanto a instância espera;
- substituição do owner depois do aceite;
- mesma Lease assumida pelo sucessor;
- checkpoint preservado;
- consulta do resultado concluído pelas duas réplicas;
- conflito em repetição de revisão;
- uma única emissão documental de conclusão.

Isso prova failover da aplicação, não alta disponibilidade do CouchDB, Valkey ou Ollama.

## 21. Testes e evidências

| Capacidade | Evidência principal | Situação |
|---|---|---|
| domínio e validação | <code>ModelosAnaliseConformidadeTest</code>, <code>ValidadorAnaliseConformidadeTest</code> | automatizada |
| API e OpenAPI | <code>AnaliseConformidadeResourceQuarkusTest</code>, <code>AnaliseConformidadeApiContractTest</code> | automatizada |
| página | <code>AnaliseConformidadePaginaEstaticaQuarkusTest</code> | automatizada |
| Flow/HITL | <code>AnaliseConformidadeFlowQuarkusTest</code> | automatizada |
| contrato documental | <code>ArmazenarEstadoAnaliseConformidadeContractTest</code> | compartilhada |
| CouchDB real | <code>CouchDbAnaliseConformidadeStoreIntegrationTest</code> | automatizada com container efêmero |
| schema Cosmos | <code>CosmosDbAnaliseConformidadeStoreContractTest</code> e testes do SDK | determinística com SDK mockado |
| Cosmos real | <code>CosmosDbAnaliseConformidadeStoreOptInTest</code> | disponível, gate ainda pendente |
| feeds | testes CouchDB, Cosmos e <code>FeedNativoFlowQuarkusTest</code> | automatizada |
| restart entre JVMs | <code>AnaliseConformidadeRestartEntreJvmTest</code> | comprovada mantendo backends ativos |
| cross-pod/failover | <code>validar-poc-kubernetes.ps1</code> e <code>validar-failover-poc-kubernetes.ps1</code> | comprovada localmente |
| fronteiras | ArchUnit e <code>RepositorioDocumentalContratoArquiteturalTest</code> | automatizada |

Esta revisão altera somente Markdown fonte. Conforme o <code>AGENTS.md</code>, não executa Maven,
SonarQube nem gera PDF, HTML ou apresentações.

## 22. Limitações e gates pendentes

### 22.1 Bloqueia promoção para PRD

- executar o contrato opt-in contra Cosmos Emulator ou conta não produtiva;
- validar a partition key <code>/correlationId</code>;
- validar criação, leitura, replace condicional e conflitos reais;
- validar Change Feed Processor e container de leases reais;
- validar <code>DefaultAzureCredential</code>, identidade gerenciada/federada e RBAC real;
- registrar decisão humana sobre o ADR-0010.

### 22.2 Limites das provas locais

- CouchDB é uma única réplica;
- Valkey é uma única réplica e usa <code>--save ""</code>;
- Ollama permanece no host;
- perda, restart e recuperação dos backends não foram testados;
- a entrada máxima pode exceder o contexto do modelo;
- não há chunking, RAG ou upload binário;
- o backend documental não possui validação nativa de JSON Schema; o contrato é aplicado pela
  aplicação e pelos testes;
- não há política produtiva de retenção ou descarte.

### 22.3 O que pode ser afirmado

Pode-se afirmar que a PoC comprovou localmente persistência documental, retomada entre JVMs,
revisão cross-pod e conclusão idempotente sem broker, desde que CouchDB, Valkey e Ollama continuem
disponíveis.

Não se pode afirmar prontidão produtiva, equivalência completa CouchDB/Cosmos em operação real ou
alta disponibilidade de toda a solução.

## 23. Mapa para o código

| Tema | Fonte principal |
|---|---|
| API REST | <code>src/main/java/br/gov/caixa/simtr/hub/conformidade/adaptador/entrada/rest/v1</code> |
| domínio | <code>src/main/java/br/gov/caixa/simtr/hub/conformidade/dominio</code> |
| casos de uso e portas | <code>src/main/java/br/gov/caixa/simtr/hub/conformidade/aplicacao</code> |
| Flow e contexto referencial | <code>aplicacao/workflow</code> |
| schema e IDs documentais | <code>adaptador/saida/documento</code> e <code>aplicacao/documento</code> |
| CouchDB | <code>adaptador/saida/couchdb</code> e <code>adaptador/entrada/couchdb</code> |
| Cosmos | <code>adaptador/saida/cosmosdb</code> e <code>adaptador/entrada/cosmosdb</code> |
| CloudEvents e SPI | <code>adaptador/saida/messaging/interno</code> e <code>adaptador/entrada/documento</code> |
| agente Ollama | <code>adaptador/saida/ollama</code> |
| propriedades | <code>src/main/resources/application.properties</code> |
| Compose | <code>compose-devservices.yml</code> e <code>compose-poc.yml</code> |
| Kubernetes | <code>k8s/poc</code> |
| guia operacional | [guia-verificacao-poc-conformidade.md](guia-verificacao-poc-conformidade.md) |
| execução e decisões da feature | <code>tasks/features/poc-conformidade-flow-ollama-hitl</code> |

## 24. Referências

### 24.1 Arquitetura do repositório

- [Arquitetura DDD e integrações atômicas](../arquitetura-ddd-integracoes-atomicas.md)
- [Índice de ADRs](../adr/README.md)
- [ADR-0001 — monólito modular e arquitetura hexagonal](../adr/0001-monolito-modular-e-hexagonal.md)
- [ADR-0002 — limites por domínio e capacidade](../adr/0002-limites-por-dominio-e-capacidade.md)
- [ADR-0003 — orquestração e colaboração por portas](../adr/0003-orquestracao-e-colaboracao-por-portas.md)
- [ADR-0004 — contratos independentes por borda](../adr/0004-contratos-independentes-por-borda.md)
- [ADR-0005 — MTR, simulador e Fault Tolerance](../adr/0005-integracoes-mtr-simulador-e-fault-tolerance.md)
- [ADR-0006 — compatibilidade, observabilidade e testes](../adr/0006-compatibilidade-observabilidade-e-testes.md)
- [ADR-0009 — PoC inicial em memória](../adr/0009-poc-conformidade-quarkus-flow-hitl-em-memoria.md)
- [ADR-0010 — backend documental e checkpoint Redis](../adr/0010-conformidade-couchdb-checkpoint-redis-e-hitl-multipod.md)

### 24.2 Referências técnicas externas

- Quarkus Flow: <https://docs.quarkiverse.io/quarkus-flow/dev/>
- Quarkus Flow Durable Kubernetes:
  <https://docs.quarkiverse.io/quarkus-flow/dev/concepts-durable-workflow-k8s.html>
- Quarkus LangChain4j: <https://docs.quarkiverse.io/quarkus-langchain4j/dev/>
- CloudEvents 1.0: <https://github.com/cloudevents/spec>
- Apache CouchDB documents: <https://docs.couchdb.org/en/stable/api/document/common.html>
- Apache CouchDB <code>_changes</code>:
  <https://docs.couchdb.org/en/stable/api/database/changes.html>
- Azure Cosmos DB Java SDK v4:
  <https://learn.microsoft.com/en-us/azure/cosmos-db/sdk-java-v4>
- Azure Cosmos DB optimistic concurrency:
  <https://learn.microsoft.com/en-us/azure/cosmos-db/database-transactions-optimistic-concurrency>
- Azure Cosmos DB Change Feed Processor:
  <https://learn.microsoft.com/en-us/azure/cosmos-db/change-feed-processor>
- Azure Cosmos DB RBAC:
  <https://learn.microsoft.com/en-us/azure/cosmos-db/how-to-connect-role-based-access-control>
