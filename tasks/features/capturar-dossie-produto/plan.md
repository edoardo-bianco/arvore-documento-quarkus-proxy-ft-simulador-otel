# Plano: capturar dossiê de produto

## Intenção

Disponibilizar no `simtr-hub` a capacidade atômica de capturar um dossiê de produto para edição,
consumindo o contrato MTR `POST /simtr-dossie-produto/v1/dossie-produto/{id}/capturar` e oferecendo
um simulador equivalente. Segundo o contrato de origem, a operação altera o estado do dossiê para
`Em Alimentação`.

O resultado deve seguir os limites de domínio, portas e adapters já adotados em `dossieproduto`,
sem introduzir workflow, orquestração ou compartilhamento de DTOs entre REST, MTR e simulador.

## Resultado esperado

- expor `POST /simtr-hub/v1/dossie-produto/{id}/capturar` sem corpo de requisição;
- validar o identificador pelo mesmo contrato Jakarta Validation usado nas rotas existentes;
- devolver `200` e JSON `{ "id": <identificador> }` quando a captura for concluída;
- chamar o wire MTR v1 exato, com API key, token OIDC e propagação de trace já configurados;
- preservar e classificar erros MTR sem publicar payload externo inválido como sucesso;
- selecionar MTR ou simulador pela property existente de `dossieproduto`;
- produzir logs e spans coerentes com as capacidades atuais, sem payload, PII, segredos ou URL
  interna completa;
- manter contrato, arquitetura, testes e documentação sincronizados.

## Escopo

- nova capacidade atômica `CapturarDossieProduto` dentro do domínio `dossieproduto`;
- porta de entrada, porta de saída, caso de uso e modelo/falha internos próprios;
- endpoint REST público v1, DTO e mapper próprios da borda REST;
- REST Client MTR v1, DTO, mapper, erro protocolar e adapter próprios;
- adapter simulador, DTO, mapper e fixture sintética próprios;
- qualifiers, producer CDI e wrapper de observabilidade da capacidade;
- política de tracing exclusiva do REST Client MTR da captura para suprimir o span HTTP
  automático, preservando a propagação do contexto configurado e o span CLIENT próprio;
- contratos públicos, unitários, integração MTR, seleção do simulador, ArchUnit, logs e spans;
- atualização dos Markdown fonte que descrevem capacidades, arquitetura, ADR aplicável e
  observabilidade.

## Fora de escopo

- implementar `alterar garantia` ou `cancelar dossiê`;
- alterar a semântica do endpoint MTR ou criar nova versão do contrato externo;
- criar workflow, saga, orquestrador, agregado futuro ou dependência entre capacidades;
- refatorar a capacidade preexistente de workflow, inclusive seu mapeamento em linha e retry;
- alterar autenticação de entrada, providers OIDC/API key ou configuração global de segurança;
- desabilitar instrumentação OpenTelemetry globalmente, alterar exporters/semântica global ou o
  tracing dos demais REST Clients;
- adicionar dependência, REST Client config key ou property de simulador;
- persistir localmente a mudança de situação do dossiê;
- manter snapshot estático do OpenAPI gerado, conforme ADR-0006;
- atualizar coleção Postman, salvo nova solicitação explícita do responsável;
- gerar ou atualizar `.ppt`, `.pptx`, `.pdf` ou `.html` derivados.

## Contexto verificado

- arquitetura consolidada: `doc/arquitetura-ddd-integracoes-atomicas.md`;
- índice e decisões aplicáveis: ADR-0002, ADR-0004, ADR-0005, ADR-0006 e ADR-0007;
- contrato primário: `doc/swagger-mtr/simtr-dossie-produto-openapi- 2.20.0.8`;
- contrato complementar: `doc/api-integracao-mtr-pre-validacao-v1.md`;
- código análogo inspecionado: capacidade `IniciarOuAvancarWorkflowDossieProduto`, consulta por
  identificador, producers, observabilidade e `DossieProdutoResource`;
- testes análogos inspecionados: contratos REST, wire MTR, seleção de simulador, cobertura de
  Resources, ArchUnit e contratos transversais de observabilidade;
- configuração verificada: o config key `dossie-produto`, a base MTR terminada em `/simtr`, os
  providers de segurança/trace e `simtr-hub.simulador.dossie-produto.habilitado` já existem;
- a busca no código confirmou que a captura ainda não possui endpoint público, capacidade,
  client, adapter, simulador ou testes;
- `.tools/` já estava não rastreado antes deste plano e permanece fora do escopo;
- a referência geral `C:/Users/edoar/.agents/references/definition-of-done.md`, indicada pelas
  skills aplicadas, não está disponível; os critérios deste repositório e os templates de
  `tasks/` formam a definição de conclusão aplicável.

### Divergências registradas

- o Swagger publica o path com prefixo `/simtr-dossie-produto`, enquanto o wire local compõe a
  base configurada `/simtr` com `@Path("/dossie-produto")`; portanto, o stub deve receber
  `/simtr/dossie-produto/v1/dossie-produto/{id}/capturar`;
- o Swagger descreve corpo padronizado para `400`, `403` e `409`, mas apenas descrição para `404`
  e `500`; a borda deve tolerar corpo ausente ou malformado e produzir o erro público padronizado;
- o adapter de workflow análogo converte respostas MTR e simulador diretamente, embora a
  arquitetura consolidada exija contrato e mapper próprios por borda; a captura seguirá a decisão
  arquitetural sem refatorar o legado fora do escopo;
- o client de workflow análogo possui retry em POST mutável. O ADR-0007 exige idempotência
  documentada antes de retry; essa decisão não será copiada automaticamente para captura.

## Contrato MTR verificado

| Item | Contrato |
|---|---|
| Método e path | `POST /simtr-dossie-produto/v1/dossie-produto/{id}/capturar` |
| Semântica | captura para edição e altera a situação para `Em Alimentação` |
| Path param | `id`, obrigatório, `int64` |
| Corpo de entrada | inexistente |
| Sucesso | `200 application/json` |
| Corpo de sucesso | objeto com `id` obrigatório, `int64` |
| Erros declarados | `400`, `401`, `403`, `404`, `409` e `500` |
| Segurança de saída | `apikey`, complementada pelo provider OIDC já usado pelo Hub |

O fluxo de estados da documentação complementar confirma
`PENDENTE_INFORMACAO -> EM_ALIMENTACAO` pela captura. O Hub apenas solicita a transição e retorna o
resultado; ele não manterá uma máquina de estados duplicada.

## Contrato público proposto

| Item | Proposta para checkpoint C1 |
|---|---|
| Método e path | `POST /simtr-hub/v1/dossie-produto/{id}/capturar` |
| Entrada | somente `id` no path; sem corpo; aceitar requisição sem `Content-Type` |
| Validação | `@NotNull` e `@Min(1)` com as mensagens já usadas por `dossieproduto` |
| Sucesso | `200 application/json` com `{ "id": 123 }` |
| Erros públicos | `400`, `401`, `403`, `404`, `409` e `500`, no `ErroPadraoDto` existente |
| OpenAPI | gerado por annotations; nenhum snapshot estático |

O DTO REST proposto é `CapturaDossieProdutoResponse`, próprio da operação. Ele não reutiliza DTO
MTR/simulador nem o DTO de criação, pois captura não é criação e deve poder evoluir sem acoplar os
contratos.

## Organização arquitetural proposta

```text
POST /simtr-hub/v1/dossie-produto/{id}/capturar
  -> DossieProdutoResource
  -> CapturarDossieProduto                         (porta de entrada)
  -> CapturarDossieProdutoCasoDeUso
  -> SolicitarCapturaDossieProduto                 (porta de saída)
       -> CapturaDossieProdutoMtrAdapter            [modo MTR]
       -> CapturaDossieProdutoSimuladorAdapter      [modo simulador]
```

- domínio: reutilizar `IdentificadorDossieProduto` e criar
  `ResultadoCapturaDossieProduto(Long identificadorDossieProduto)`;
- falha: `FalhaCapturaDossieProduto` com as classificações `NEGOCIO`, `TECNICA_CLIENTE`,
  `DEPENDENCIA_INDISPONIVEL` e `TIMEOUT`;
- aplicação: uma porta por direção e caso de uso que apenas delega, sem conhecer HTTP, DTO ou
  configuração;
- MTR: DTO `CapturaDossieProdutoMtrResponse`, mapper e erro protocolar exclusivos;
- simulador: DTO, mapper, fixture e adapter exclusivos, sem imports da borda MTR/REST;
- seleção: qualifiers `CapturaMtr` e `CapturaSimulador`, com producer que reutiliza a property
  existente de `dossieproduto`;
- REST: `CapturaDossieProdutoResponse` e `CapturaDossieProdutoRestMapper` próprios;
- nenhuma nova dependência entre domínios e nenhum acesso bloqueante.

## Regras das bordas

### REST MTR

- config key: `dossie-produto`;
- client base: `@Path("/dossie-produto")`;
- operação: `POST /v1/dossie-produto/{id}/capturar`;
- retorno reativo: `Uni<CapturaDossieProdutoMtrResponse>`;
- sem parâmetro de corpo e sem `@Consumes` que obrigue JSON;
- `Accept: application/json`;
- manter `RequestHeaderFactory` e `OidcClientRequestReactiveFilter`; não registrar
  `RestClientObservabilityFilter` neste client, pois ele publica payload e URL concreta fora da
  allowlist aprovada para a captura;
- registrar somente neste client um provider local que forneça `HttpClientOptions` com
  `TracingPolicy.IGNORE`, impedindo a criação do span HTTP automático. Como essa política também
  desabilita a propagação automática do Vert.x, o mesmo provider deve injetar explicitamente o
  contexto corrente com o propagador configurado do OpenTelemetry, sem montar `traceparent`
  manualmente;
- classificar `400`, `404`, `409` e o eventual `422` pelo padrão atual como negócio; demais `4xx`
  como técnica cliente; `5xx` como indisponibilidade da dependência;
- preservar status e campos protocolares válidos; usar fallback seguro para erro sem corpo ou
  fora do contrato;
- rejeitar resposta nula, `id` nulo ou `id` diferente do solicitado como falha de contrato da
  dependência antes do mapper REST.

### Simulador

- reutilizar `simtr-hub.simulador.dossie-produto.habilitado`;
- fixture Markdown/JSON sintética em `src/main/resources/mock/dossieproduto/`;
- cenário de sucesso retorna o mesmo identificador solicitado/registrado na fixture;
- identificador ausente na fixture produz `404` controlado no contrato público;
- não chamar rede, não registrar PII e não receber annotations de fault tolerance;
- DTO e mapper do simulador não serão compartilhados com REST ou MTR.

## Fault tolerance e idempotência

Capturar altera estado. O Swagger informa a transição, mas não fornece chave de idempotência,
garantia de repetição segura nem semântica para resposta perdida após commit. Assim, a proposta
segura para C1 é:

- `@Timeout` de 2.000 ms, coerente com o client do mesmo serviço;
- `@CircuitBreaker` com os parâmetros vigentes (`10`, `0,5`, `10.000 ms`, `2`), contando somente
  falhas técnicas/timeout e ignorando falhas de negócio/cliente;
- **sem `@Retry` automático** até existir garantia oficial e testável de idempotência do MTR;
- se o usuário aprovar retry com nova evidência, atualizar primeiro este plano e o checklist,
  registrar a política exata e criar teste que prove repetição somente para falhas autorizadas;
- nunca repetir `400`, `401`, `403`, `404`, `409` ou outro erro de negócio/cliente.

## Segurança proposta

- preservar o controle de autenticação da API pública já aplicado ao Hub;
- preservar API key, OIDC e propagação de trace somente no client MTR;
- fixture apenas com dados sintéticos e sem dados pessoais;
- não registrar corpo, headers de autenticação, token, API key, mensagens/stacktrace do MTR ou URL
  interna completa;
- usar path templated nos atributos de telemetria;
- não ampliar permissões nem criar endpoint anônimo.

## Observabilidade proposta

| Camada | Span | Eventos principais |
|---|---|---|
| API | `simtr-hub.api.dossie-produto.capturar` (SERVER) | `simtr-hub.dossie-produto.captura.recebida/concluida/falhou` |
| Aplicação | `simtr-hub.service.dossie-produto.capturar` (INTERNAL) | `simtr-hub.dossie-produto.captura.processamento.*` |
| MTR | `mtr.dossie-produto.capturar` (CLIENT) | `mtr.dossie-produto.captura.chamada.*` |
| Simulador | sem CLIENT | `simtr-hub.dossie-produto.captura.simulador.*` |

Atributos permitidos: rota pública templated, API `dossie-produto-v1`, operação, origem
`mtr|mock`, método, path MTR templated, `dossie_produto.id`, resultado e classificação técnica.
Payload, PII, segredos e mensagens externas ficam proibidos.

No modo MTR, a captura deve produzir exatamente um span `CLIENT`: o span próprio
`mtr.dossie-produto.capturar`. O span HTTP automático do Vert.x não integra o contrato da
capacidade, pois adiciona `url.full` com a URL interna concreta. A correlação outbound deve sair do
span próprio e continuar usando os propagadores configurados pela aplicação.

## Decisões e impactos

| Dimensão | Situação | Checkpoint humano adicional? |
|---|---|---|
| Contrato | nova rota pública POST, resposta e matriz de status | sim, C1 |
| Arquitetura | nova capacidade atômica dentro de `dossieproduto`, sem novo ADR | sim, C1 |
| Segurança | nova superfície mutável; reutiliza autenticação e credenciais existentes | sim, C1 |
| Comportamento observável | novos logs, spans, atributos, timeout e circuit breaker | sim, C1 |
| Replanejamento C3 | política local remove o span HTTP automático e preserva propagação explícita | sim, C3-TRACE |
| Idempotência | contrato externo não comprova repetição segura; proposta sem retry | sim, C1 |

C0 autoriza iniciar a feature executável. C1 aprova explicitamente contrato, desenho, segurança,
telemetria e política de fault tolerance. Nenhuma aprovação é inferida deste documento.

## Estratégia de implementação e verificação

- implementar em fatias pequenas com RED -> GREEN -> REFACTOR;
- manter cada task limitada aos arquivos listados ou atualizar plano/checklist antes de ampliar;
- executar testes focados em cada task e suíte/ArchUnit nos checkpoints coerentes;
- provar o wire HTTP com stub local, inclusive método, path, corpo vazio, headers, trace e número de
  chamadas;
- provar que o simulador selecionado não acessa o MTR;
- provar a ausência de retry no modo inicialmente proposto, inclusive para `500` e timeout;
- inspecionar todos os spans do trace da captura, não apenas os três spans próprios conhecidos, e
  comparar o `traceparent` recebido no stub com o trace/span id do CLIENT próprio;
- revisar correção, simplicidade, arquitetura, segurança, desempenho, testes e escopo antes de
  cada checkpoint;
- não criar teste/snapshot do OpenAPI gerado.

## Tarefas

### Task 1 — Inicializar o baseline SonarQube autorizado

**Descrição:** após C0/C1, verificar se existem pacotes em `sonar/`, perguntar qual fonte deve
formar o baseline e executar somente o modo escolhido antes da primeira alteração executável.

**Critérios de aceitação:**

- fonte do baseline e eventual pacote registrados no `todo.md`;
- baseline inicializado pelo script oficial sem expor ou persistir token;
- limitações de um baseline exclusivamente offline registradas como `UNVERIFIED`.

**Verificação:** comando oficial correspondente à escolha humana.

**Dependências:** C0 e C1.

**Arquivos prováveis:** estado operacional gerado pelo script; nenhum arquivo de produção.

### Task 2 — Congelar o contrato público em RED

**Descrição:** adicionar cenários de sucesso, ausência de corpo, validação de `id` e documentação
dos endpoints antes da implementação da rota.

**Critérios de aceitação:**

- sucesso espera exatamente `200` e `{ "id": 123 }`;
- `id` igual a zero ou negativo espera `400` e as mensagens existentes;
- requisição sem `Content-Type` é aceita pelo contrato desejado;
- o RED falha somente pela ausência da nova rota/capacidade.

**Verificação:** testes focados de contrato REST e endpoints.

**Dependências:** Task 1.

**Arquivos prováveis:**

- `src/test/java/br/gov/caixa/simtr/hub/contrato/DossieProdutoApiContractTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/contrato/DossieProdutoErroApiContractTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/recurso/ResourceEndpointTest.java`.

### Task 3 — Criar resultado e falha internos

**Descrição:** modelar somente o resultado da captura e suas categorias de falha.

**Critérios de aceitação:**

- resultado contém apenas o identificador do dossiê;
- falha preserva status e dados protocolares necessários sem depender de DTOs externos;
- tipos internos não importam adapters ou Jakarta REST.

**Verificação:** testes unitários de modelo/falha e ArchUnit.

**Dependências:** Task 2.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/dominio/modelo/ResultadoCapturaDossieProduto.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/dominio/erro/FalhaCapturaDossieProduto.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/dominio/erro/FalhasDossieProdutoTest.java`.

### Task 4 — Criar portas e caso de uso atômico

**Descrição:** criar a entrada `CapturarDossieProduto`, a saída `SolicitarCapturaDossieProduto` e
o caso de uso de delegação reativa.

**Critérios de aceitação:**

- uma única delegação com `IdentificadorDossieProduto`;
- resultado e falha são propagados sem transformação ou bloqueio;
- aplicação permanece independente de REST, MTR e simulador.

**Verificação:** teste unitário do caso de uso e ArchUnit.

**Dependências:** Task 3.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/aplicacao/porta/entrada/CapturarDossieProduto.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/aplicacao/porta/saida/SolicitarCapturaDossieProduto.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/aplicacao/casodeuso/CapturarDossieProdutoCasoDeUso.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/aplicacao/casodeuso/CapturarDossieProdutoCasoDeUsoTest.java`.

### Checkpoint CA — Núcleo isolado

- evidenciar testes do domínio/aplicação e ArchUnit verdes;
- manter o contrato público em RED apenas pela rota ainda ausente;
- revisar direção de dependência, simplicidade e ausência de comportamento futuro.

### Task 5 — Criar DTO e mapper MTR da captura

**Descrição:** desserializar `{ "id": ... }` em contrato próprio da borda MTR e mapear para o
resultado interno.

**Critérios de aceitação:**

- `id` possui `@JsonProperty` explícito e tipo compatível com `int64`;
- mapper não depende de REST ou simulador;
- resposta nula, id nulo e divergente permanecem identificáveis para validação do adapter.

**Verificação:** teste unitário de desserialização/mapeamento e ArchUnit.

**Dependências:** Checkpoint CA.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/dto/v1/captura/CapturaDossieProdutoMtrResponse.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/mapper/CapturaDossieProdutoMtrMapper.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/mapper/CapturaDossieProdutoMtrMapperTest.java`.

### Task 6 — Declarar REST Client e erro protocolar

**Descrição:** declarar o POST v1, providers, timeout/circuit breaker e classificação de respostas
HTTP, sem retry na política inicial.

**Critérios de aceitação:**

- método, path, ausência de body e retorno reativo coincidem com o Swagger;
- timeout e circuit breaker coincidem com C1;
- não existe `@Retry` enquanto não houver garantia aprovada de idempotência;
- erro com corpo válido é preservado e corpo ausente/malformado recebe fallback seguro;
- erros de negócio/cliente não contam para o circuit breaker.

**Verificação:** testes reflexivos e unitários do client/erro protocolar.

**Dependências:** Task 5.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/client/CapturaDossieProdutoMtrClient.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/erro/CapturaDossieProdutoMtrException.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/client/CapturaDossieProdutoMtrClientTest.java`.

### Task 7 — Implementar adapter MTR

**Descrição:** implementar a porta de saída, validar integridade da resposta, traduzir falhas após
o client interceptado e instrumentar o span CLIENT.

**Critérios de aceitação:**

- resposta válida produz o resultado interno pelo mapper dedicado;
- resposta nula, id nulo ou divergente produz falha de dependência;
- negócio, técnica cliente, servidor e timeout são classificados sem perda protocolar;
- sinais não registram payload, segredos nem mensagens externas.

**Verificação:** teste unitário do adapter e ArchUnit.

**Dependências:** Task 6.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/adapter/CapturaDossieProdutoMtrAdapter.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/configuracao/qualificador/CapturaMtr.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/adapter/CapturaDossieProdutoMtrAdapterTest.java`.

### Checkpoint CB — Borda MTR

- evidenciar DTO/mapper/client/adapter e ArchUnit verdes;
- confirmar a matriz de status e a ausência de retry;
- revisar propagação de credenciais, correlação, timeout e circuit breaker.

### Task 8 — Criar contrato e fixture do simulador

**Descrição:** criar fixture sintética, DTO e mapper independentes para a captura simulada.

**Critérios de aceitação:**

- fixture contém apenas identificador sintético e documenta o contrato MTR simulado;
- mapper não importa tipos REST/MTR;
- id ausente ou divergente não é aceito como sucesso.

**Verificação:** teste do reader real, desserialização, mapper e ArchUnit.

**Dependências:** Checkpoint CB.

**Arquivos prováveis:**

- `src/main/resources/mock/dossieproduto/captura-dossie-produto.md`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/simulador/dto/CapturaDossieProdutoSimuladorResponse.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/simulador/mapper/CapturaDossieProdutoSimuladorMapper.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/simulador/mapper/CapturaDossieProdutoSimuladorMapperTest.java`.

### Task 9 — Implementar adapter simulador

**Descrição:** resolver a fixture, implementar a mesma porta de saída e produzir `404` controlado
quando o identificador não existir.

**Critérios de aceitação:**

- sucesso retorna o identificador esperado sem rede;
- ausente retorna falha de negócio compatível com `404`;
- não há annotations de fault tolerance nem dependência da borda MTR.

**Verificação:** teste unitário do adapter e ArchUnit.

**Dependências:** Task 8.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/simulador/adapter/CapturaDossieProdutoSimuladorAdapter.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/configuracao/qualificador/CapturaSimulador.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/simulador/adapter/CapturaDossieProdutoSimuladorAdapterTest.java`.

### Task 10 — Selecionar MTR ou simulador por CDI

**Descrição:** criar producer próprio que reutiliza a property de simulador de `dossieproduto`.

**Critérios de aceitação:**

- `false` seleciona somente o adapter MTR;
- `true` seleciona somente o adapter simulador;
- não surge nova property nem ambiguidade CDI.

**Verificação:** teste unitário do producer.

**Dependências:** Tasks 7 e 9.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/configuracao/CapturaDossieProdutoPortasProducer.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/configuracao/CapturaDossieProdutoPortasProducerTest.java`.

### Task 11 — Criar contrato e mapper REST

**Descrição:** mapear resultado/falha internos para resposta e exceções públicas padronizadas.

**Critérios de aceitação:**

- sucesso serializa exatamente a chave `id`;
- status e corpo MTR válidos são preservados;
- timeout/falha sem status usam `500` e corpo público seguro;
- nenhum DTO de outra borda é importado.

**Verificação:** teste unitário do mapper REST.

**Dependências:** Task 10.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/entrada/rest/v1/dto/CapturaDossieProdutoResponse.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/entrada/rest/v1/CapturaDossieProdutoRestMapper.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/entrada/rest/v1/CapturaDossieProdutoRestMapperTest.java`.

### Task 12 — Expor rota e instrumentar API/aplicação

**Descrição:** adicionar a operação no Resource e o wrapper observável da porta de entrada.

**Critérios de aceitação:**

- rota, validação, `Consumes` e OpenAPI coincidem com C1;
- fluxo completo usa a porta de entrada e o mapper REST;
- spans SERVER/INTERNAL e eventos aprovados são emitidos;
- contratos públicos da Task 2 ficam verdes sem relaxamento.

**Verificação:** contratos REST, teste do Resource, observabilidade da aplicação e cobertura direta.

**Dependências:** Task 11.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/entrada/rest/v1/DossieProdutoResource.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/configuracao/CapturaDossieProdutoObservabilidade.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/entrada/rest/v1/CapturaDossieProdutoResourceQuarkusTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/configuracao/CapturaDossieProdutoObservabilidadeTest.java`.

### Checkpoint C2 — Fatia vertical

- executar testes focados, suíte completa, ArchUnit e checkpoint SonarQube do incremento;
- apresentar qualquer situação `NON_COMPLIANT` para decisão humana pelo fluxo oficial;
- revisar o diff nos eixos exigidos pelo repositório antes da integração detalhada.

### Task 13 — Provar wire MTR e seleção ponta a ponta

**Descrição:** exercitar a rota pública contra o stub local e provar a seleção sem rede.

**Critérios de aceitação:**

- stub recebe POST no wire `/simtr/dossie-produto/v1/dossie-produto/123/capturar` com corpo vazio;
- `Accept`, API key, bearer e `traceparent` são propagados;
- `200`, `400`, `401`, `403`, `404`, `409`, `500`, timeout e corpo de erro inválido são cobertos;
- `500` e timeout resultam em uma única chamada enquanto a política for sem retry;
- modo simulador funciona com o endpoint MTR indisponível.

**Verificação:** testes Quarkus de integração com stub e seleção do simulador.

**Dependências:** Checkpoint C2.

**Arquivos prováveis:**

- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/integracao/DossieProdutoMtrStubTestResource.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/integracao/CapturaDossieProdutoMtrContractTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/integracao/CapturaDossieProdutoSelecaoSimuladorQuarkusTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/integracao/CapturaDossieProdutoNovoClientQuarkusTest.java`.

### Task 14 — Congelar contratos transversais

**Descrição:** incorporar a nova capacidade às provas globais de endpoints, beans, arquitetura,
logs e spans.

**Critérios de aceitação:**

- hierarquia SERVER -> INTERNAL -> CLIENT no MTR e SERVER -> INTERNAL no simulador;
- rota e paths são templated e o trace é contínuo;
- payload, PII, token, API key, mensagem e stacktrace externos não aparecem nos sinais;
- contagem/cobertura de Resources e guardrails arquiteturais incluem a nova operação.

**Resultado de 14.1 e limite residual:** o ajuste aprovado retirou a autodiscovery global do
`RestClientObservabilityFilter`, manteve o comportamento dos clients que o registram explicitamente
e saneou somente as exceções observáveis da captura. Os três spans próprios e os logs da capacidade
ficaram sem conteúdo proibido. A inspeção do trace completo, porém, revelou um quarto span CLIENT,
criado automaticamente pelo Vert.x, com o atributo semântico `url.full` contendo a URL interna
concreta. Essa limitação motivou a reprovação humana do C3 e é tratada separadamente na Task 14.2.

**Verificação:** contratos de observabilidade, Resources e ArchUnit.

**Dependências:** Task 13.

**Arquivos prováveis:**

- `src/test/java/br/gov/caixa/simtr/hub/arquitetura/observabilidade/ObservabilidadeSpansContratoTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/arquitetura/observabilidade/ObservabilidadeLogsContratoTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/recurso/ResourceBeanCoverageTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/arquitetura/guardrails/ArchUnitProgressivoTest.java`.

### Checkpoint C3-TRACE — Política local de tracing da captura

- aprovar a supressão, somente no REST Client da captura, do span HTTP automático do Vert.x por
  `HttpClientOptions` com `TracingPolicy.IGNORE`;
- aprovar a injeção explícita do contexto corrente pelo propagador OpenTelemetry configurado,
  necessária porque `IGNORE` também interrompe a propagação automática;
- confirmar que configuração global, outros REST Clients, span CLIENT próprio, API key, OIDC,
  fault tolerance, wire, contrato público e dependências permanecem inalterados;
- nenhuma mudança executável da Task 14.2 começa antes do `GO C3-TRACE`.

### Task 14.2 — Eliminar `url.full` do trace da captura

**Descrição:** registrar exclusivamente no `CapturaDossieProdutoMtrClient` um provider local, não
global, que desabilite o tracing automático do `HttpClient` Vert.x e injete o contexto corrente com
o propagador OpenTelemetry já configurado. Preservar o span CLIENT próprio do adapter e todos os
contratos funcionais aprovados.

**Critérios de aceitação:**

- todo o trace da captura fica sem `url.full` e sem URL interna completa, contendo exatamente um
  span CLIENT, `mtr.dossie-produto.capturar`;
- o stub recebe `traceparent` cujo trace id e parent id correspondem ao span CLIENT próprio, além
  dos mesmos API key, bearer, método, path, corpo vazio e número de chamadas já aprovados;
- a mudança fica limitada ao client/provider/testes da captura, sem `@Provider` global,
  configuração, dependência, alteração de outros clients, retry ou novo comportamento futuro.

**Verificação:** RED -> GREEN no contrato runtime, fazendo a asserção percorrer todos os spans do
mesmo trace; contrato reflexivo do registro local do provider; bateria focada de wire, seleção sem
rede, logs/spans, segurança, zero retry e ArchUnit. Suíte completa e SonarQube ficam no C3
revalidado, depois de um incremento coerente.

**Dependências:** Task 14.1 concluída e Checkpoint C3-TRACE aprovado.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/client/CapturaDossieProdutoMtrClient.java`;
- novo `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/client/CapturaDossieProdutoMtrTracingProvider.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/client/CapturaDossieProdutoMtrClientTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/integracao/CapturaDossieProdutoMtrContractTest.java`.

**Tamanho estimado:** M, restrito a quatro arquivos.

**Base técnica oficial:** o [guia do REST Client do Quarkus](https://quarkus.io/guides/rest-client)
documenta a customização por `HttpClientOptions`; a
[especificação MicroProfile REST Client](https://download.eclipse.org/microprofile/microprofile-rest-client-4.0/microprofile-rest-client-spec-4.0.html)
define `@RegisterProvider` por client; o
[Vert.x `TracingPolicy`](https://vertx.io/docs/4.5.28/apidocs/io/vertx/core/tracing/TracingPolicy.html)
define `IGNORE` como desativação de tracing e propagação; e a
[API Java do OpenTelemetry](https://opentelemetry.io/docs/languages/java/api/#context-propagation)
define a injeção pelo `TextMapPropagator` configurado. A inspeção das fontes instaladas do Quarkus
3.33.2.1 e Vert.x 4.5.28 confirmou esses pontos na versão efetivamente usada pelo projeto.

### Checkpoint C3 — Integração e sinais

- executar bateria focada, suíte completa e checkpoint SonarQube;
- revisar wire, número de chamadas, seleção sem rede e sigilo dos sinais;
- comprovar que nenhum span do trace da captura contém `url.full`/URL interna completa e que o
  `traceparent` continua vinculado ao CLIENT próprio;
- não iniciar documentação final até a autorização do próximo item pendente.

### Task 15 — Atualizar arquitetura e ADR aplicável

**Descrição:** registrar o estado implementado como a décima primeira capacidade do Hub e a oitava
de `dossieproduto`, reduzindo para duas as operações MTR ainda ausentes.

**Critérios de aceitação:**

- README e arquitetura registram rota pública, consumo MTR e simulador;
- ADR-0002 e índice recebem somente atualização factual, permanecendo `Aceito`;
- nenhum ADR novo é criado sem decisão arquitetural nova;
- nenhum formato derivado é alterado.

**Verificação:** buscas de consistência e `git diff --check`.

**Dependências:** Checkpoint C3.

**Arquivos prováveis:**

- `README.md`;
- `doc/arquitetura-ddd-integracoes-atomicas.md`;
- `doc/adr/0002-limites-por-dominio-e-capacidade.md`;
- `doc/adr/README.md`.

### Task 16 — Atualizar documentação de observabilidade

**Descrição:** registrar spans, eventos, atributos permitidos e proibições da captura, removendo a
indicação de operação não implementada dos Markdown fonte.

**Critérios de aceitação:**

- catálogo confere com os nomes efetivamente implementados/testados;
- documentação deixa de marcar captura como ausente;
- Postman e formatos derivados permanecem intactos.

**Verificação:** comparação com código/testes, buscas de segurança e `git diff --check`.

**Dependências:** Task 15.

**Arquivos prováveis:**

- `doc/catalogo-observabilidade.md`;
- `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`.

### Checkpoint C4 — Documentação fonte

- confirmar contagens, rotas, nomes de sinais e lista de operações ausentes;
- provar que nenhum `.ppt`, `.pptx`, `.pdf`, `.html` ou Postman foi alterado;
- não executar Maven/Sonar se o incremento permanecer exclusivamente documental.

### Task 17 — Validar e revisar integralmente

**Descrição:** executar a regressão completa, checkpoint final e revisão multi-eixo antes de
solicitar encerramento humano.

**Critérios de aceitação:**

- testes focados, suíte completa e ArchUnit verdes;
- checkpoint Sonar sem decisão pendente ou com decisão humana registrada;
- `git diff --check` sem erro;
- revisão de correção, legibilidade, arquitetura, segurança, desempenho, testes e escopo sem
  apontamento obrigatório;
- somente arquivos previstos ou previamente autorizados no diff.

**Verificação:** `mvn -q clean test`, script oficial de checkpoint, inspeções do diff e testes
manuais locais proporcionais ao risco.

**Dependências:** Checkpoint C4.

**Arquivos prováveis:** nenhum novo; eventuais correções devem atualizar plano/checklist antes.

### Ajuste 17.1-A — Consolidar constantes locais nos testes da captura

**Descrição:** remover repetição de literais semânticos nos dois testes identificados pela revisão
final, preservando integralmente cenários, dados, asserts e comportamento executável.

**Critérios de aceitação:**

- nomes dos spans, serviço MTR e sentinelas do contrato runtime usam constantes privadas locais;
- mensagem observável e sentinelas internas do mapper REST usam constantes privadas locais;
- chaves JSON permanecem explícitas nos asserts para documentar o contrato;
- não é criado helper compartilhado nem alterado código de produção;
- testes focados, suíte completa e checkpoint SonarQube permanecem verdes.

**Verificação:** executar cada teste após sua pequena refatoração, depois `mvn -q clean test`, script
oficial de checkpoint e inspeção do diff/whitespace.

**Dependências:** Task 17.1 concluída e `GO ajuste 17.1` explícito do usuário.

**Arquivos prováveis:**

- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/integracao/CapturaDossieProdutoMtrContractTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/entrada/rest/v1/CapturaDossieProdutoRestMapperTest.java`;
- `tasks/features/capturar-dossie-produto/plan.md`;
- `tasks/features/capturar-dossie-produto/todo.md`.

### Checkpoint CF — Aceitação humana

- apresentar contrato entregue, testes, integração, observabilidade, Sonar e diff;
- somente o usuário registra CF/encerramento da feature.

## SonarQube

- a elaboração inicial foi exclusivamente documental; `sonar/`, Maven, API Sonar e baseline não
  foram inspecionados ou executados antes do GO;
- fonte do baseline da implementação: somente SonarQube Docker local, pois a Task 1 confirmou que
  não existe diretório/pacote `sonar/` disponível para escolha;
- pacote autorizado: nenhum;
- baseline obrigatório: concluído na Task 1, após C0/C1 e antes da primeira alteração de código,
  testes ou tooling;
- estado inicial: `READY`, fonte `LOCAL_SONAR`, análise
  `9056ed2e-9910-4ed7-b6b6-33371ca35e64`, revisão `d40999f`, 213 issues, cobertura de 87,8%,
  duplicação de 3,5%, avaliação `COMPLIANT` e decisão `NOT_REQUIRED`;
- checkpoints esperados: C2, C3 e validação final, quando houver alteração do fingerprint;
- `NON_COMPLIANT` exige apresentação de evidências e decisão explícita
  `Reprovar`, `AceitarExcepcionalmente` ou `ContinuarAjustes`.

## Riscos e controles

| Risco | Impacto | Controle |
|---|---|---|
| retry duplicar uma transição mutável | alto | política inicial sem retry; mudar somente com idempotência oficial/testável e C1 atualizado |
| resposta perdida após captura concluída | alto | timeout limitado, correlação e erro explícito; não repetir cegamente |
| id de resposta nulo ou divergente | alto | validar no adapter antes de mapear/publicar sucesso |
| path Swagger divergir da composição local | alto | teste de wire completo com base `/simtr` e path do client |
| erro MTR sem corpo padronizado | médio | fallback seguro, preservação de status e testes para corpo vazio/malformado |
| acoplamento de DTOs entre bordas | médio | DTO e mapper próprios para REST, MTR e simulador, protegidos por ArchUnit |
| simulator mascarar falha de seleção | alto | teste com MTR indisponível e verificação de zero chamadas de rede |
| vazamento de credenciais ou mensagem externa | alto | allowlist de atributos e contratos negativos de logs/spans |
| `url.full` escapar por span automático fora da seleção nominal dos testes | alto | inspecionar todos os spans do mesmo trace e exigir exatamente um CLIENT próprio |
| `TracingPolicy.IGNORE` interromper a correlação outbound | alto | injetar o contexto pelo propagador configurado e comparar `traceparent` com trace/span id do CLIENT próprio |
| provider local afetar outros REST Clients | alto | registrar somente no client da captura, sem `@Provider`, property ou customizer global |
| crescimento do `DossieProdutoResource` | médio | mudança mínima, revisão estrutural; extração maior somente por plano/GO próprios |
| documentação e implementação divergirem | médio | atualizar somente após código estável e comparar nomes/contagens com testes |
| alteração preexistente ser incluída | médio | manter `.tools/` fora do diff/staging e revisar `git status` em checkpoints |

## Dependências

- contrato MTR `simtr-dossie-produto-openapi- 2.20.0.8` disponível no repositório;
- configuração e providers atuais do REST Client `dossie-produto`;
- autenticação pública já configurada no Hub;
- infraestrutura existente de `Uni`, CDI, MicroProfile REST Client/Fault Tolerance e OpenTelemetry;
- APIs já presentes de `HttpClientOptions`, `TracingPolicy`, `ClientRequestFilter` e propagação
  OpenTelemetry; nenhuma dependência nova é prevista;
- `MarkdownJsonMockReader` e property de simulador existentes;
- decisão humana C0/C1 e escolha posterior da fonte SonarQube.

## GO necessário

O `GO replanejar C3` autoriza somente esta atualização documental. Nenhuma alteração de produção,
teste executável ou tooling da Task 14.2 começa antes do `GO C3-TRACE` registrado no `todo.md`.
Depois desse GO, será executado somente o próximo item pendente do checklist. Qualquer mudança de
contrato, arquitetura, segurança, observabilidade ou política de retry atualiza este plano e o
checklist antes da implementação.
