# Arquitetura atual do simtr-hub

## Como usar este documento

- **Status:** aceito
- **Última consolidação:** 2026-08-25
- **Objetivo:** explicar rapidamente a arquitetura implementada e as restrições que novas features
  devem respeitar.

Leia este consolidado antes de planejar uma feature. Em seguida, consulte o
[índice de ADRs](adr/README.md): a descrição do índice deve bastar para identificar quais decisões
se aplicam. Leia o ADR completo somente quando ele for aplicável à mudança ou quando houver dúvida.

O código, os contratos executáveis e os testes são a fonte de verdade do comportamento atual. Se
este documento divergir deles, registre a divergência no plano da feature antes de propor uma
correção.

## Visão do sistema

O `simtr-hub` é um monólito modular Quarkus organizado por domínios de negócio. Ele expõe doze
capacidades atômicas por REST e integra cada uma ao MTR ou ao simulador por adapters de saída
intercambiáveis. Além delas, a PoC de análise de conformidade expõe três endpoints sobre estado
volátil e inicia um workflow que consulta e congela o checklist, executa um agente sequencial no
Ollama local e produz um resultado preliminar validado.

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
seleciona o adapter. Não existem atualmente endpoint único de pré-validação, MCP Server ou
comunicação distribuída entre os domínios. O runtime do Quarkus Flow, sua ponte de Messaging
interna e o `AnaliseConformidadeFlow` já estão presentes para a PoC. O workflow executável alcança
o resultado preliminar do agente ou do fallback técnico, pausa para a revisão humana correlacionada
e retoma até a conclusão.

Além da borda REST, a consulta de documentos do dossiê pode ser iniciada pelo consumidor CDI local
`br.gov.caixa.simtr.dossie.ConsultaDocumentosDossieProduto`, no mesmo artifact e runtime Quarkus.
Esse caminho entra pela mesma porta de aplicação e não cria chamada HTTP local nem nova superfície
externa.

## Domínios e capacidades implementadas

| Domínio | Responsabilidade | Capacidades atuais |
|---|---|---|
| `arvoredocumento` | Dados parametrizados usados por uma futura árvore documental | `ConsultarProcessoParametrizado` |
| `conformidade` | Consulta de checklist e PoC de análise/revisão humana | `ConsultarChecklist`; modelos, validação determinística, projeção volátil, API e workflow HITL até o resultado final revisado |
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
| `POST` | `/simtr-hub/v1/conformidade/analises` |
| `GET` | `/simtr-hub/v1/conformidade/analises/{instanceId}` |
| `PUT` | `/simtr-hub/v1/conformidade/analises/{instanceId}/revisao` |

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

### PoC de análise de conformidade em implementação

O estado implementado até a Task 6.1 inclui:

- canais internos `flow-in` e `flow-out` com CloudEvent v1, sem connector ou broker;
- shim de Messaging confinado ao adapter e à versão Flow `0.10.2`;
- modelos imutáveis de solicitação, resultado, revisão e visão da análise;
- validação determinística de cobertura, identidade, nomes e confiança;
- porta de estado e store `ConcurrentHashMap` com transições atômicas;
- reserva interna compare-and-set para aceitar somente uma revisão por instância;
- três portas e casos de uso de entrada para iniciar, consultar e revisar;
- adapter REST v1 com DTOs próprios, JSON camelCase e `Location` relativo;
- validação de borda do texto em até 20.000 caracteres e da lista completa de revisão;
- tradução de falhas para o contrato `ErroPadraoDto` com
  `400`/`404`/`409`/`422`/`503`, sem stack trace ou detalhe interno;
- `AnaliseConformidadeFlow` registrado como `WorkflowDefinition`, com o identificador nativo da
  instância usado também pela projeção e pelo contrato REST;
- início assíncrono sem espera bloqueante e etapa que acessa exclusivamente a porta
  `ConsultarChecklist`;
- checklist copiado para o contexto do workflow com lista imutável; item nulo, checklist vazio e
  falha técnica encerram o workflow e transitam a projeção para `FALHOU`;
- porta `AnalisarTextoComChecklist` e task `agent(...)` no workflow, com o identificador raiz da
  instância usado como `memoryId`;
- capacidade Agentic sequencial `AplicadorChecklistAgent -> RevisorCoberturaAgent`, executada com
  `llama3.2:3b` no Ollama local e retorno em records exclusivos do adapter;
- projeção JSON mínima do checklist, cláusulas contra prompt injection e validação Java que rejeita
  identificadores extras/duplicados e completa item ausente como `NAO_ANALISADO`;
- timeout HTTP de 60 s e política MicroProfile FT no adapter Ollama, com timeout de 65 s, até duas
  novas tentativas, circuit breaker e fallback completo para revisão humana;
- retry interno do provider desabilitado por `ModelBuilderCustomizer`, mantendo a política FT como
  fonte única de repetição;
- tracing textual integral do Flow desabilitado por padrão; spans próprios do workflow e do agente
  carregam somente identificadores, versão, quantidade, modelo, origem e estado;
- evento `flow-out` complementado no adapter de Messaging com `source` estável e `time` UTC, sem
  remover as extensões de correlação nativas do Flow;
- pausa real no `listen`, publicação assíncrona da revisão pelo `flow-in` e retomada somente da
  instância correlacionada;
- dupla validação da revisão, antes da publicação e dentro do workflow retomado;
- evento final e projeção `CONCLUIDA` com origem `REVISAO_HUMANA`.

A projeção possui os estados `EM_PROCESSAMENTO`, `AGUARDANDO_REVISAO`, `CONCLUIDA` e `FALHOU`.
Ela é exclusivamente volátil, não substitui o estado do Flow e não oferece recuperação após
reinício. O POST cria a projeção `EM_PROCESSAMENTO` e inicia o Flow sem aguardar a consulta; o
resultado preliminar publicado em `flow-out` projeta `AGUARDANDO_REVISAO`; o PUT valida, reserva
atomicamente e publica a revisão em `flow-in`; e o workflow correlacionado retoma até projetar
`CONCLUIDA`. A entrega interna continua sem garantia durável ou recuperação após falha, e a página
da PoC ainda não está implementada.

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

Timeout, retry, circuit breaker e classificação de exceções pertencem ao adapter da integração:
adapters MTR mantêm suas políticas atuais e o adapter Ollama possui a política específica aprovada
para a PoC. Essas políticas não pertencem ao domínio, ao workflow nem ao simulador.

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
- a PoC mantém request/response completos do LangChain4j desligados por padrão e só admite o
  perfil `%poc` com dados sintéticos; o tracing textual do Flow também permanece desligado;
- os spans `simtr-hub.flow.conformidade.analise` e
  `simtr-hub.agent.conformidade.analisar` não registram texto, prompt, resposta, evidência,
  credencial, detalhe de erro ou stack;
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
- possui runtime Flow, ponte interna, API, agente Ollama e pausa/retomada HITL completa no mesmo
  processo, mas sem entrega durável ou recuperação após falha;
- não possui MCP Server ou tools;
- possui somente projeção volátil da PoC, sem persistência ou recuperação de estado;
- não calcula árvore documental nem executa ainda a análise de conformidade ponta a ponta;
- não implementa os dois endpoints ausentes listados acima.

Essas restrições descrevem o estado atual, não uma proibição permanente. Uma feature pode mudá-las
somente com requisitos explícitos, análise de impacto, plano, testes e GO humano.

## Decisões arquiteturais

Consulte [doc/adr/README.md](adr/README.md) para o resumo e a aplicabilidade de cada decisão. O
índice é parte da leitura inicial; o texto completo de um ADR é leitura sob demanda.
