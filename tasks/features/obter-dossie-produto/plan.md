# Plano: consultar dossiê de produto por identificador

## Intenção

Expor no `simtr-hub` uma capacidade atômica de consulta do domínio `dossieproduto`, permitindo
obter um dossiê por seu identificador pela API pública do Hub, consumindo o endpoint
`GET /simtr-dossie-produto/v2/dossie-produto/{id}` do MTR e oferecendo a mesma porta de saída por
um simulador próprio.

## Resultado esperado

- o consumidor chama `GET /simtr-hub/v1/dossie-produto/{id}`;
- o Hub valida o identificador, aciona somente a porta de entrada da capacidade e responde `200`
  com um contrato público tipado em `snake_case`;
- em modo MTR, o REST Client existente `dossie-produto` executa `GET` no contrato v2, preservando
  autenticação, correlação, classificação de falhas e fault tolerance;
- em modo simulador, a mesma porta de saída lê uma fixture própria sem acesso de rede;
- DTOs e mappers de REST público, MTR e simulador permanecem independentes;
- logs e spans permitem diagnosticar a consulta sem registrar CPF, CNPJ, nome, matrícula ou o
  payload retornado.

## Premissas submetidas ao GO

1. O path público será `GET /simtr-hub/v1/dossie-produto/{id}`. O path informado no pedido,
   `GET /simtr-dossie-produto/v2/dossie-produto/{id}`, é o contrato upstream do MTR.
2. `id` será `Long`, obrigatório e maior que zero.
3. O corpo `200` seguirá a estrutura do Swagger 2.20.0.8 e do exemplo fornecido, preservando
   nomes `snake_case`, ordem das listas, listas vazias e as chaves nulas explicitamente aprovadas
   no contrato.
4. Datas serão transportadas como `String` opaca no formato recebido, sem parse, conversão de
   timezone ou reformatação. Essa escolha resolve a divergência registrada entre o Swagger e os
   exemplos, mas precisa ser aprovada em C1.
5. A fixture conservará a estrutura e os valores não pessoais do exemplo, mas substituirá CPF e
   nome por dados evidentemente sintéticos. Os valores pessoais literais não serão gravados no
   histórico Git sem confirmação explícita de que são fictícios ou autorizados.
6. A autenticação/autorização pública seguirá a política global já aplicada aos demais endpoints;
   os providers existentes continuarão enviando API key, token OIDC e correlação ao MTR.
7. Por ser uma leitura idempotente, a chamada MTR reutilizará a matriz de timeout, retry e circuit
   breaker das consultas atuais, condicionada ao checkpoint C1.

## Escopo

- expor `GET /simtr-hub/v1/dossie-produto/{id}` com contrato público próprio;
- criar a capacidade `ConsultarDossieProduto` no domínio `dossieproduto`, com modelo de leitura,
  falha, porta de entrada, caso de uso e porta de saída específicos;
- criar DTOs e mappers independentes para REST público, MTR v2 e simulador;
- consumir o contrato MTR descrito no Swagger local
  `doc/swagger-mtr/simtr-dossie-produto-openapi- 2.20.0.8`;
- reutilizar o config key REST Client `dossie-produto`, os providers de API key/OIDC/correlação e
  a property `simtr-hub.simulador.dossie-produto.habilitado`;
- selecionar MTR ou simulador por qualifiers e producer CDI explícitos;
- fornecer fixture de sucesso `200` para o identificador `4324680`, com dados pessoais
  sintetizados;
- classificar e traduzir falhas MTR sem transportar DTO externo pelo núcleo;
- cobrir contrato HTTP/JSON, validação, mapeamentos, REST Client, wire MTR, falhas, fault
  tolerance, simulador, seleção CDI, observabilidade e ArchUnit;
- atualizar README, arquitetura consolidada, ADR-0002, índice de ADRs e catálogo de
  observabilidade após o comportamento estar implementado;
- manter a coleção Postman fora do escopo automatizado desta feature; o responsável fará essa
  atualização manualmente depois.

## Fora de escopo

- expor o prefixo upstream `/simtr-dossie-produto` como path público do Hub;
- implementar alteração de garantia, captura ou cancelamento de dossiê;
- consultar dossiê por CPF, CNPJ, situação ou qualquer filtro diferente do identificador;
- criar listagem, paginação, persistência, cache, estado local, workflow ou orquestrador;
- reutilizar `ProdutoContratadoDossieProduto` da operação mutável na resposta de consulta;
- compartilhar DTO ou mapper entre REST, MTR e simulador;
- alterar o Swagger MTR de referência ou corrigir silenciosamente inconsistências nele;
- adicionar dependência, config key, property de seleção ou política de autenticação nova;
- refatorar o `DossieProdutoResource`, os REST Clients existentes ou filtros compartilhados;
- criar arquivo OpenAPI estático ou teste do documento OpenAPI gerado;
- alterar formatos derivados `.html`, `.pdf`, `.ppt` ou `.pptx`;
- incluir CPF, CNPJ, nome ou matrícula nos novos logs, spans ou atributos.

## Contexto verificado

- arquitetura consolidada lida em
  `doc/arquitetura-distribuida/arquitetura-ddd-integracoes-atomicas.md`;
- índice de decisões lido em `doc/adr/README.md`;
- ADRs aplicáveis lidos integralmente: ADR-0002, ADR-0004, ADR-0005 e ADR-0006;
- contrato fonte inspecionado no Swagger
  `doc/swagger-mtr/simtr-dossie-produto-openapi- 2.20.0.8`, versão `2.20.0.8`;
- especificação complementar inspecionada em
  `doc/arquitetura-distribuida/api-integracao-mtr-pre-validacao-v1.md`;
- ausência atual da capacidade confirmada no README, na arquitetura, no código e nos testes;
- fluxo de consulta comparado com `ConsultarChecklist`, incluindo Resource, portas, caso de uso,
  REST Client, adapter MTR, simulador, producer CDI, observabilidade e testes com stub local;
- fluxo MTR v2 do próprio domínio comparado com `IncluirDocumentoDossieProduto`;
- contratos transversais inspecionados em `DossieProdutoApiContractTest`,
  `DossieProdutoErroApiContractTest`, `ResourceEndpointTest`, `ResourceBeanCoverageTest`, testes de
  logs/spans e `ArchUnitProgressivoTest`;
- configuração atual confirmada: a base real termina em `/simtr`, o REST Client declara
  `/dossie-produto` e o wire esperado é
  `GET /simtr/dossie-produto/v2/dossie-produto/{id}`;
- `.tools/` já estava não rastreado antes deste planejamento e permanece fora do escopo;
- a referência compartilhada `definition-of-done.md` indicada pelos skills não está instalada no
  caminho declarado; este plano usa `AGENTS.md`, critérios por task e verificações executáveis do
  projeto como Definition of Done aplicável.

## Divergências registradas

| Fonte | Divergência | Tratamento proposto |
|---|---|---|
| Swagger 2.20.0.8 | `data_criacao` e datas internas aparecem como `Calendar`/objeto, mas o `example` usa texto | Representar datas como `String` opaca e congelar o wire com teste; decisão em C1 |
| Swagger 2.20.0.8 | Campos marcados `required` aparecem nulos no exemplo fornecido, inclusive dados de cliente e produto | Testar nulabilidade e presença de chaves no contrato público aprovado, sem inferir validações inexistentes |
| Swagger x exemplo fornecido | O Swagger permite `processo.data`; o exemplo não contém a chave | Manter o campo tipado, omitindo-o quando ausente; confirmar a semântica em C1 |
| Prefixo publicado x gateway configurado | A especificação usa `/simtr-dossie-produto`; o ambiente usa base `/simtr` + `/dossie-produto` | Documentar ambos e testar o wire efetivo sem alterar a configuração comum |
| Arquitetura atual | Registra nove capacidades, seis em `dossieproduto` e quatro operações ausentes | Após implementação, atualizar para dez capacidades, sete em `dossieproduto` e três operações ausentes |
| Exemplo do pedido | Contém CPF e nome possivelmente reais | Usar identidade sintética na fixture; literal somente com autorização explícita de segurança |

## Contrato público proposto

| Elemento | Proposta |
|---|---|
| Método e path | `GET /simtr-hub/v1/dossie-produto/{id}` |
| Path param | `id` obrigatório, `Long`, maior que zero |
| Corpo de requisição | nenhum |
| Sucesso | `200 OK`, `application/json` |
| Corpo `200` | objeto tipado com os campos descritos abaixo |
| Não encontrado | `404` com `ErroPadraoDto`, preservado da falha interna |
| Outros erros documentados | `400`, `401`, `403` e `500` com `ErroPadraoDto` |
| Resposta MTR `200` vazia | falha de contrato da dependência traduzida para `500`; não produzir `204` implícito |
| OpenAPI | gerado pelo Quarkus a partir de Resource e DTOs; nenhum artefato estático |

### Estrutura do `200`

| Campo | Tipo público |
|---|---|
| `id` | inteiro `int64` |
| `chave_correlacao_canal` | inteiro `int64` |
| `instancia_jbpm` | inteiro `int64` ou `null` |
| `numero_negocio` | inteiro `int64` ou `null` |
| `canal_criacao` | string |
| `unidade_criacao` | inteiro `int32` |
| `data_criacao` | string opaca ou `null` |
| `clientes` | lista de cliente, preservando ordem e elementos nulos se recebidos |
| `processo` | objeto com `id`, `nome`, `identificador_negocial`, `macroprocesso`, `data`, `tratamento_seletivo` e `complementacao_seletiva` |
| `fase_atual` | objeto com `id`, `nome`, `identificador_negocial` e `data` |
| `situacao_atual` | objeto com `id`, `nome`, `data` e `matricula` |
| `unidades_tratamento` | lista de inteiros `int32` |
| `produtos_contratados` | lista de produto com `id`, `codigo_operacao`, `codigo_modalidade` e `nome` |

O mapper REST será responsável pela presença/omissão de chaves nulas conforme o contrato aprovado.
O teste de contrato usará o exemplo estrutural do pedido com `cpf = "00000000000"` e
`nome = "CLIENTE SIMULADO"`, evitando persistir identidade possivelmente real.

## Contrato MTR proposto

| Elemento | Proposta |
|---|---|
| REST Client | `ConsultaDossieProdutoMtrClient`, config key `dossie-produto` |
| Base declarada | `@Path("/dossie-produto")` |
| Operação | `@GET @Path("/v2/dossie-produto/{id}")` |
| Wire testado | `GET /simtr/dossie-produto/v2/dossie-produto/4324680` |
| Request | sem corpo e sem `Content-Type`; `Accept: application/json` |
| Providers | `RequestHeaderFactory`, `OidcClientRequestReactiveFilter`, `RestClientObservabilityFilter` |
| Sucesso | DTO MTR v2 próprio, convertido ao modelo interno por mapper próprio |
| Regra mínima de integridade | resposta não nula, `id` não nulo e igual ao identificador solicitado |
| Erros | `400/404/409/422` como negócio; demais `4xx` como técnica cliente; `5xx` como servidor |
| Tradução | erro protocolar permanece até depois dos interceptors e então vira `FalhaConsultaDossieProduto` |

### Fault tolerance proposta

- `@Timeout` de 2.000 ms;
- `@Retry` com até 3 retries, atraso de 300 ms e jitter de 100 ms;
- retry apenas para erro servidor, `ProcessingException` e `TimeoutException`;
- abortar retry para erro de negócio e erro técnico cliente;
- `@CircuitBreaker` com volume 10, razão 0,5, atraso de 10.000 ms e limiar de 2 sucessos;
- nenhuma annotation de fault tolerance no simulador.

## Simulador proposto

- fixture por identificador em
  `mock/dossieproduto/4324680-v2-consulta-dossie-produto.md`;
- leitura `JSON -> ConsultaDossieProdutoSimuladorResponse -> DossieProdutoConsultado`;
- dados de identidade sintéticos, sem copiar CPF/nome potencialmente reais;
- solicitação de `4324680` retorna o exemplo `200` aprovado sem acesso de rede;
- identificador sem fixture retorna falha de negócio simulada `404`, em vez de responder com um
  dossiê de outro identificador;
- seleção pela property existente `simtr-hub.simulador.dossie-produto.habilitado`.

## Desenho arquitetural proposto

```text
GET /simtr-hub/v1/dossie-produto/{id}
    -> DossieProdutoResource
        -> ConsultarDossieProduto
            -> ConsultarDossieProdutoCasoDeUso
                -> ObterDossieProduto
                    |-- ConsultaDossieProdutoMtrAdapter
                    |     -> ConsultaDossieProdutoMtrClient
                    `-- ConsultaDossieProdutoSimuladorAdapter
                          -> fixture Markdown/JSON
```

- `IdentificadorDossieProduto` existente será reutilizado porque já é o tipo semântico do mesmo
  domínio e não pertence a uma borda;
- `DossieProdutoConsultado` será um modelo interno de leitura coeso, sem DTOs Jackson, annotations
  REST ou invariantes de aggregate inventadas;
- os subtipos de cliente, processo, fase, situação e produto poderão ser records semânticos
  aninhados ao modelo de leitura para manter coesão e limitar o número de arquivos;
- a operação mutável `ProdutoContratadoDossieProduto` não será reutilizada, pois seu campo
  `excluir` e sua finalidade não pertencem à consulta;
- o novo desenho aplica ADRs existentes e não requer ADR novo.

## Estilo e estrutura

```java
public interface ConsultarDossieProduto {

    Uni<DossieProdutoConsultado> executar(IdentificadorDossieProduto identificador);
}
```

- nomes Java usam linguagem do negócio; nomes JSON permanecem explícitos com `@JsonProperty`;
- operações assíncronas retornam `Uni`, sem `await`, `subscribe`, thread ou bloqueio no código de
  produção;
- mappers executam somente `borda -> interno` ou `interno -> borda`;
- DTO MTR v2 reside em `adaptador/saida/mtr/dto/v2/consulta`;
- DTO REST reside em `adaptador/entrada/rest/v1/dto` porque a API pública continua v1;
- DTO do simulador reside em `adaptador/saida/simulador/dto`;
- nenhum Map genérico ou DTO compartilhado substituirá os contratos tipados.

## Observabilidade proposta

| Camada | Sinal proposto |
|---|---|
| API | span SERVER `simtr-hub.api.dossie-produto.consultar` |
| Aplicação | span INTERNAL `simtr-hub.service.dossie-produto.consultar` |
| MTR | span CLIENT `mtr.dossie-produto.consultar` |
| Eventos REST/aplicação | prefixo `simtr-hub.dossie-produto.consulta` |
| Eventos MTR | prefixo `mtr.dossie-produto.consulta` |
| Atributos | rota, API v2, origem, flag do simulador, `dossie_produto.id` e contagens de listas |

O `url.path` do novo span CLIENT será a rota templated
`/simtr/dossie-produto/v2/dossie-produto/{id}`. Payload, CPF, CNPJ, nome, matrícula, API key,
token e URL interna não serão registrados nos novos sinais.

## Decisões e impactos

| Dimensão | Situação | Checkpoint humano adicional? |
|---|---|---|
| Contrato | Novo path, método, status, JSON, nulabilidade, validação e OpenAPI públicos | sim — C1 |
| Arquitetura | Nova capacidade, portas e adapters no domínio `dossieproduto`, aplicando ADRs aceitos | sim — C1 |
| Segurança | Nova superfície que devolve dados de cliente e nova chamada autenticada ao MTR | sim — C1 |
| Comportamento observável | Novos spans, eventos, atributos e matriz de fault tolerance | sim — C1 |
| Dependências | Nenhuma biblioteca, serviço ou property nova | não |

## Estratégia de testes

- RED -> GREEN -> REFACTOR para contrato público e cada comportamento novo;
- testes unitários para caso de uso, falha e mappers;
- testes reflexivos para path, providers e annotations de fault tolerance do REST Client;
- testes de adapter para tradução lossless, resposta vazia e divergência de identificador;
- testes Quarkus com stub HTTP local para método, path, ausência de corpo, headers, token,
  `traceparent`, sucesso, `404`, `500` com retry e timeout;
- teste de seleção CDI nos dois valores da property;
- contrato HTTP exato para o JSON `200`, erro de validação e erro MTR público;
- contratos transversais de logs e spans sem dados sensíveis;
- `ArchUnitProgressivoTest` para direção das dependências e isolamento de DTOs.

Comandos de referência:

```powershell
mvn -q "-Dtest=<classes-focadas>" test
mvn -q clean test
./validar-checkpoint-sonarqube.ps1
git diff --check
```

## Tarefas

### Task 1 — Inicializar o baseline técnico

**Descrição:** cumprir o fluxo SonarQube antes da primeira alteração em `src/`. Como esta sessão
altera somente documentação, `sonar/` não foi inspecionado. Na retomada, verificar se há pacotes e,
se houver, solicitar ao usuário a fonte do baseline antes de executar o script oficial.

**Critérios de aceitação:**

- C0 e C1 registrados pelo usuário;
- fonte do baseline escolhida pelo usuário quando houver pacote offline;
- baseline inicializado no modo autorizado e evidência registrada no `todo.md`;
- indisponibilidade registrada como limitação, nunca como aprovação.

**Verificação:**

- `./validar-checkpoint-sonarqube.ps1 -InitializeBaseline` com os parâmetros autorizados.

**Dependências:** C0 e C1 aprovados.

**Arquivos prováveis:** nenhum arquivo fonte.

### Task 2 — Congelar o contrato público em RED

**Descrição:** adicionar testes HTTP que descrevam a nova rota antes de criar qualquer classe de
produção.

**Critérios de aceitação:**

- teste exige `GET /simtr-hub/v1/dossie-produto/4324680`, `200`, JSON e nulabilidade aprovados;
- teste exige `400` para `id <= 0`, sem corpo de requisição;
- RED falha pela rota ausente, não por infraestrutura ou rede.

**Verificação:**

- `mvn -q "-Dtest=DossieProdutoApiContractTest,DossieProdutoErroApiContractTest,ResourceEndpointTest" test`;
- registrar a evidência RED no checklist.

**Dependências:** Task 1 concluída.

**Arquivos prováveis:**

- `src/test/java/br/gov/caixa/simtr/hub/contrato/DossieProdutoApiContractTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/contrato/DossieProdutoErroApiContractTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/recurso/ResourceEndpointTest.java`.

### Task 3 — Criar o modelo de leitura e a falha interna

**Descrição:** modelar o resultado da consulta e sua classificação de falhas sem annotations ou
tipos de borda.

**Critérios de aceitação:**

- modelo tipa todos os campos do contrato, listas e objetos aninhados;
- datas permanecem `String` opaca e valores nulos/listas vazias são preserváveis;
- falha classifica negócio, técnica cliente, dependência indisponível e timeout;
- nenhum tipo depende de REST, MTR, simulador ou outro domínio.

**Verificação:**

- `mvn -q "-Dtest=FalhasDossieProdutoTest,ArchUnitProgressivoTest" test`.

**Dependências:** Task 2 com RED caracterizado.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/dominio/modelo/DossieProdutoConsultado.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/dominio/erro/FalhaConsultaDossieProduto.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/dominio/erro/FalhasDossieProdutoTest.java`.

### Task 4 — Criar portas e caso de uso atômico

**Descrição:** implementar a coordenação mínima `entrada -> caso de uso -> saída` usando apenas
tipos internos.

**Critérios de aceitação:**

- porta de entrada `ConsultarDossieProduto` recebe `IdentificadorDossieProduto`;
- porta de saída `ObterDossieProduto` expressa a necessidade do consumidor;
- caso de uso delega exatamente uma vez e propaga item, nulo e falha sem bloqueio;
- teste comprova identidade do identificador e propagação do resultado/falha.

**Verificação:**

- `mvn -q "-Dtest=ConsultarDossieProdutoCasoDeUsoTest,ArchUnitProgressivoTest" test`.

**Dependências:** Task 3 concluída.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/aplicacao/porta/entrada/ConsultarDossieProduto.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/aplicacao/porta/saida/ObterDossieProduto.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/aplicacao/casodeuso/ConsultarDossieProdutoCasoDeUso.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/aplicacao/casodeuso/ConsultarDossieProdutoCasoDeUsoTest.java`.

### Checkpoint A — Núcleo isolado

- testes focados das Tasks 3 e 4 verdes;
- ArchUnit verde;
- RED público da Task 2 permanece explicado pela borda ainda ausente;
- revisão de simplicidade confirma que o modelo de consulta não virou aggregate ou contrato
  compartilhado.

### Task 5 — Traduzir a resposta MTR v2

**Descrição:** criar o DTO de resposta MTR e o mapper dedicado a partir do Swagger e do exemplo
aprovado.

**Critérios de aceitação:**

- DTO MTR possui campos e `@JsonProperty` completos, sem reutilizar DTO REST/simulador;
- mapper preserva nulos, listas vazias, ordem e todos os objetos aninhados;
- teste usa datas em string e cobre `processo.data` ausente;
- resposta externa nula permanece detectável pelo adapter, sem virar sucesso vazio.

**Verificação:**

- `mvn -q "-Dtest=ConsultaDossieProdutoMtrMapperTest,ArchUnitProgressivoTest" test`.

**Dependências:** Task 4 concluída.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/dto/v2/consulta/ConsultaDossieProdutoMtrResponse.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/mapper/ConsultaDossieProdutoMtrMapper.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/mapper/ConsultaDossieProdutoMtrMapperTest.java`.

### Task 6 — Declarar REST Client e erro protocolar

**Descrição:** implementar o contrato wire GET, providers, matriz de fault tolerance e
classificação de erros antes de traduzir para o núcleo.

**Critérios de aceitação:**

- método executa `GET /dossie-produto/v2/dossie-produto/{id}` e retorna o DTO MTR v2;
- API key, OIDC e filtro de observabilidade permanecem registrados;
- GET não produz corpo nem `Content-Type` de requisição e aceita JSON;
- erros e fallback malformado seguem a classificação aprovada;
- annotations de timeout/retry/circuit breaker correspondem exatamente a C1.

**Verificação:**

- `mvn -q "-Dtest=ConsultaDossieProdutoMtrClientTest" test`;
- inspeção reflexiva de método, path, providers e annotations.

**Dependências:** Task 5 concluída.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/client/ConsultaDossieProdutoMtrClient.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/erro/ConsultaDossieProdutoMtrException.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/client/ConsultaDossieProdutoMtrClientTest.java`.

### Task 7 — Implementar adapter MTR

**Descrição:** conectar o REST Client à porta de saída, validar a identidade mínima da resposta e
traduzir falhas somente depois da chamada interceptada.

**Critérios de aceitação:**

- adapter implementa `ObterDossieProduto` com qualifier MTR exclusivo;
- resposta `200` nula, sem `id` ou com `id` diferente do solicitado falha como contrato externo;
- falhas MTR são traduzidas losslessly após fault tolerance;
- span/log CLIENT registram somente identificador, contagens, resultado e tipos de erro.

**Verificação:**

- `mvn -q "-Dtest=ConsultaDossieProdutoMtrAdapterTest,ArchUnitProgressivoTest" test`.

**Dependências:** Task 6 concluída.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/configuracao/qualificador/ConsultaDossieProdutoMtr.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/adapter/ConsultaDossieProdutoMtrAdapter.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/adapter/ConsultaDossieProdutoMtrAdapterTest.java`.

### Checkpoint B — Borda MTR isolada

- testes de DTO, mapper, client, adapter e falhas verdes;
- ArchUnit verde;
- matriz de fault tolerance coincide com C1;
- contrato público continua RED apenas porque Resource/seleção ainda não existem.

### Task 8 — Criar contrato próprio do simulador

**Descrição:** preparar fixture, DTO e mapper independentes para o sucesso `200` do identificador
canônico.

**Critérios de aceitação:**

- fixture documenta o endpoint MTR v2 e contém todos os campos aprovados;
- CPF/nome são sintéticos e nenhuma identidade literal do pedido é persistida;
- DTO e mapper do simulador não dependem de MTR ou REST;
- teste preserva nulos, listas vazias, ordem e datas em string.

**Verificação:**

- `mvn -q "-Dtest=ConsultaDossieProdutoSimuladorMapperTest,ArchUnitProgressivoTest" test`;
- leitura da fixture por `MarkdownJsonMockReader`.

**Dependências:** Task 4 concluída.

**Arquivos prováveis:**

- `src/main/resources/mock/dossieproduto/4324680-v2-consulta-dossie-produto.md`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/simulador/dto/ConsultaDossieProdutoSimuladorResponse.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/simulador/mapper/ConsultaDossieProdutoSimuladorMapper.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/simulador/mapper/ConsultaDossieProdutoSimuladorMapperTest.java`.

### Task 9 — Implementar adapter simulador

**Descrição:** resolver fixture por identificador e implementar a mesma porta de saída sem fault
tolerance ou rede.

**Critérios de aceitação:**

- adapter possui qualifier simulador exclusivo e implementa `ObterDossieProduto`;
- `4324680` retorna a fixture e marca origem `mock`;
- identificador sem fixture produz falha controlada `404`, sem retornar dados de outro dossiê;
- nenhuma classe do simulador importa DTO/client/erro MTR.

**Verificação:**

- `mvn -q "-Dtest=ConsultaDossieProdutoSimuladorAdapterTest,ArchUnitProgressivoTest" test`.

**Dependências:** Task 8 concluída.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/configuracao/qualificador/ConsultaDossieProdutoSimulador.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/simulador/adapter/ConsultaDossieProdutoSimuladorAdapter.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/simulador/adapter/ConsultaDossieProdutoSimuladorAdapterTest.java`.

### Task 10 — Selecionar a porta de saída por CDI

**Descrição:** produzir uma única implementação de `ObterDossieProduto` usando a property já
existente do domínio.

**Critérios de aceitação:**

- `true` seleciona simulador e `false` seleciona MTR;
- bootstrap CDI não apresenta ambiguidade, recursão ou dependência circular;
- nenhuma property ou default existente é alterado.

**Verificação:**

- `mvn -q "-Dtest=ConsultaDossieProdutoPortasProducerTest" test`.

**Dependências:** Tasks 7 e 9 concluídas.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/configuracao/ConsultaDossieProdutoPortasProducer.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/configuracao/ConsultaDossieProdutoPortasProducerTest.java`.

### Task 11 — Criar DTO e mapper REST de resposta

**Descrição:** traduzir o modelo interno para o contrato público sem converter diretamente DTO
MTR ou simulador.

**Critérios de aceitação:**

- DTO público tipa todos os campos e usa nomes `snake_case` explícitos;
- mapper preserva valores, ordem, listas vazias e nulabilidade aprovada;
- o JSON estrutural do mock é reproduzido com identidade sintética;
- falha interna é convertida ao mecanismo REST público existente sem perda de dados.

**Verificação:**

- `mvn -q "-Dtest=ConsultaDossieProdutoRestMapperTest,ArchUnitProgressivoTest" test`.

**Dependências:** Task 4 concluída e contrato C1 aprovado.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/entrada/rest/v1/dto/ConsultaDossieProdutoResponse.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/entrada/rest/v1/ConsultaDossieProdutoRestMapper.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/entrada/rest/v1/ConsultaDossieProdutoRestMapperTest.java`.

### Task 12 — Expor a capacidade e torná-la observável

**Descrição:** adicionar o wrapper observável da aplicação e o método GET ao Resource existente,
fechando a primeira fatia vertical completa.

**Critérios de aceitação:**

- Resource valida `id`, chama somente `ConsultarDossieProduto` e retorna `200` com o DTO público;
- `404` interno e demais falhas são traduzidos para `ErroPadraoDto`;
- testes RED da Task 2 tornam-se GREEN sem relaxar o contrato;
- spans/logs da API e aplicação usam os nomes aprovados e não contêm dados pessoais;
- `DossieProdutoResource` não é refatorado fora do método/constructor necessários.

**Verificação:**

- `mvn -q "-Dtest=DossieProdutoApiContractTest,DossieProdutoErroApiContractTest,ResourceEndpointTest,ResourceBeanCoverageTest,ConsultaDossieProdutoRestMapperTest,ConsultaDossieProdutoObservabilidadeTest,ArchUnitProgressivoTest" test`.

**Dependências:** Tasks 10 e 11 concluídas.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/configuracao/ConsultaDossieProdutoObservabilidade.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/entrada/rest/v1/DossieProdutoResource.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/configuracao/ConsultaDossieProdutoObservabilidadeTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/recurso/ResourceBeanCoverageTest.java`.

### Checkpoint C2 — Fatia vertical funcional

- todos os testes focados das Tasks 2–12 verdes;
- `mvn -q clean test` verde;
- `ArchUnitProgressivoTest` verde;
- `./validar-checkpoint-sonarqube.ps1` executado para o incremento coerente;
- revisão de correção, simplicidade, arquitetura, segurança, desempenho e escopo concluída;
- qualquer situação `NON_COMPLIANT` apresentada ao usuário para decisão formal.

### Task 13 — Provar wire MTR e seleção ponta a ponta

**Descrição:** exercitar o caminho real com stub HTTP local e o caminho simulador pelo container
Quarkus.

**Critérios de aceitação:**

- stub recebe GET, path v2 exato, corpo vazio, `Accept`, API key, bearer token e `traceparent`;
- sucesso preserva o JSON completo até a API pública;
- `404` não sofre retry e preserva erro; `500` recuperável repete GET idêntico conforme C1;
- property seleciona MTR/simulador nos dois valores sem acesso externo.

**Verificação:**

- `mvn -q "-Dtest=ConsultaDossieProdutoMtrContractTest,ConsultaDossieProdutoSelecaoSimuladorQuarkusTest" test`.

**Dependências:** C2 concluído.

**Arquivos prováveis:**

- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/integracao/DossieProdutoMtrStubTestResource.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/integracao/ConsultaDossieProdutoMtrContractTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/integracao/ConsultaDossieProdutoSelecaoSimuladorQuarkusTest.java`.

### Task 14 — Congelar observabilidade transversal

**Descrição:** ampliar os contratos de spans e logs para a décima capacidade.

**Critérios de aceitação:**

- caminho simulador possui parentage SERVER -> INTERNAL e origem `mock`;
- caminho MTR possui SERVER -> INTERNAL -> CLIENT, rota e API v2;
- eventos de sucesso/falha registram identificador, origem, resultado e contagens aprovadas;
- sentinelas de CPF, CNPJ, nome, matrícula, token, API key e payload não aparecem nos novos sinais.

**Verificação:**

- `mvn -q "-Dtest=ObservabilidadeSpansContratoTest,ObservabilidadeLogsContratoTest,ConsultaDossieProdutoMtrContractTest" test`.

**Dependências:** Task 13 concluída.

**Arquivos prováveis:**

- `src/test/java/br/gov/caixa/simtr/hub/arquitetura/observabilidade/ObservabilidadeSpansContratoTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/arquitetura/observabilidade/ObservabilidadeLogsContratoTest.java`.

### Checkpoint C3 — Integração e sinais

- testes de integração, seleção, logs e spans verdes;
- suíte completa verde;
- checkpoint SonarQube executado porque `src/test` alterou o fingerprint;
- nenhuma regressão nos nove endpoints existentes;
- qualquer situação `NON_COMPLIANT` submetida à decisão humana prevista em `AGENTS.md`.

### Task 15 — Atualizar o estado arquitetural permanente

**Descrição:** refletir somente o estado implementado nas fontes Markdown permanentes.

**Critérios de aceitação:**

- README e arquitetura listam dez capacidades, sete em `dossieproduto` e o novo path público;
- lista de ausências passa de quatro para três operações;
- ADR-0002 e índice recebem atualização factual da capacidade, sem criar decisão nova;
- nenhuma afirmação de implementação antecede o código verde.

**Verificação:**

- buscas de consistência por contagens, capacidade e paths;
- `git diff --check`.

**Dependências:** C3 concluído.

**Arquivos prováveis:**

- `README.md`;
- `doc/arquitetura-distribuida/arquitetura-ddd-integracoes-atomicas.md`;
- `doc/adr/0002-limites-por-dominio-e-capacidade.md`;
- `doc/adr/README.md`.

### Task 16 — Atualizar documentação operacional

**Descrição:** documentar os sinais da nova capacidade no catálogo de observabilidade. A coleção
Postman será atualizada manualmente pelo responsável e não integra esta task.

**Critérios de aceitação:**

- catálogo registra spans, eventos, atributos e proibições de dados sensíveis;
- nenhum formato derivado é alterado.

**Verificação:**

- busca por segredo e pelos dados pessoais originais;
- consistência dos nomes e atributos com código e contratos executáveis;
- `git diff --check`.

**Dependências:** Task 15 concluída.

**Arquivos prováveis:**

- `doc/arquitetura-distribuida/catalogo-observabilidade.md`;

### Ajuste S1 — Eliminar cinco issues LOW `java:S1185`

**Descrição:** remover overrides redundantes de `mensagens()` identificados pelo SonarQube local
em cinco falhas de `dossieproduto`, preservando o método público herdado e todo o comportamento.

**Critérios de aceitação:**

- os overrides que apenas retornam `super.mensagens()` são removidos de
  `FalhaCriacaoDossieProduto`, `FalhaAtualizacaoFormularioDossieProduto`,
  `FalhaInclusaoDocumentoDossieProduto`, `FalhaRegistroValidacaoNegocialDossieProduto` e
  `FalhaWorkflowDossieProduto`;
- construtores, tipos, mensagens, causas e tradução REST/MTR permanecem inalterados;
- testes de domínio e ArchUnit permanecem verdes;
- o checkpoint SonarQube elimina as cinco issues e não introduz issue nova ou decisão pendente.

**Verificação:**

- `mvn -q "-Dtest=FalhasDossieProdutoTest,ArchUnitProgressivoTest" test`;
- `./validar-checkpoint-sonarqube.ps1`;
- `git diff --check`.

**Dependências:** solicitação explícita do usuário após a Task 16.1; baseline local `READY`.

**Arquivos prováveis:**

- as cinco classes de falha listadas acima;
- `tasks/features/obter-dossie-produto/plan.md`;
- `tasks/features/obter-dossie-produto/todo.md`.

### Checkpoint C4 — Documentação

- documentação fonte consistente com código e testes;
- nenhum `.html`, `.pdf`, `.ppt` ou `.pptx` alterado;
- Maven e SonarQube não são repetidos por este incremento exclusivamente documental.

### Task 17 — Validar e revisar a feature completa

**Descrição:** executar a verificação final e revisar o diff integral antes de solicitar
encerramento humano.

**Critérios de aceitação:**

- testes focados e `mvn -q clean test` passam sem falhas ou testes ignorados;
- checkpoint SonarQube final está atual e sem decisão pendente;
- contratos existentes permanecem inalterados;
- diff não contém segredo, PII real, formato derivado ou mudança fora do plano;
- documentação e checklist refletem a evidência final; a atualização manual do Postman permanece
  fora do critério de encerramento automatizado desta feature.

**Verificação:**

- `mvn -q clean test`;
- `./validar-checkpoint-sonarqube.ps1`, se houver fingerprint posterior ao C3;
- `git diff --check`;
- revisão do diff nos eixos exigidos por `AGENTS.md`.

**Dependências:** C4 concluído.

**Arquivos prováveis:** somente ajustes estritamente necessários encontrados pela validação.

## SonarQube

- esta sessão é exclusivamente documental: não solicitar token, não inspecionar `sonar/`, não
  executar Maven, baseline, API Sonar ou checkpoint;
- após C0 e C1, antes da primeira alteração em `src/`, verificar se existem pacotes em `sonar/`;
- se houver pacotes, o usuário escolherá entre baseline local, local + pacote específico ou
  exclusivamente pacote específico;
- checkpoint esperado no C2, no C3 e no final somente se houver novo fingerprint;
- baseline offline permanece `UNVERIFIED` e não autoriza afirmar cobertura, duplicação, issues
  atuais ou Quality Gate aprovado;
- `NON_COMPLIANT` exige evidência e decisão humana `Reprovar`, `AceitarExcepcionalmente` ou
  `ContinuarAjustes`.

## Riscos e controles

| Risco | Impacto | Controle |
|---|---|---|
| Datas divergentes entre Swagger e exemplos | alto | `String` opaca, teste de wire e decisão C1 |
| Nulabilidade/omissão virar contrato acidental | alto | contrato JSON exato antes da produção e mapper REST dedicado |
| Exposição ou persistência de PII | alto | fixture sintética, autenticação existente, nenhum payload em telemetria e checkpoint de segurança |
| CPF literal preexistente em `TestFixtures.java` | médio | não reutilizar nem ampliar nesta feature; tratar eventual saneamento em demanda própria para não alterar testes fora do escopo |
| Resposta MTR de outro identificador | alto | validar `id` não nulo e igual ao solicitado antes de devolver dados |
| `200` sem corpo virar `204` implícito | médio | tratar item nulo como falha de dependência e testar ausência de `204` |
| Retry alterar carga/comportamento | médio | aplicar apenas a GET idempotente, congelar annotations e wire, aprovar em C1 |
| Ambiguidade CDI entre adapters | médio | qualifiers exclusivos, producer explícito e teste nos dois modos |
| Reuso indevido do produto da operação mutável | médio | modelo de leitura próprio e ArchUnit |
| Crescimento do `DossieProdutoResource` | médio | alteração cirúrgica; refatoração fica fora de escopo e exige plano próprio |
| Documentação afirmar entrega antes do código | médio | atualizar estado permanente somente após C3 |
| Alteração acidental de `.tools/` ou derivados | baixo | excluir do diff e verificar paths finais |

## Dependências

- Swagger local `simtr-dossie-produto` 2.20.0.8;
- Quarkus 3.33.2.1, Java 25, Mutiny, REST Client Reactive, OIDC e MicroProfile Fault Tolerance já
  presentes no projeto;
- configuração `quarkus.rest-client.dossie-produto.*` existente;
- property `simtr-hub.simulador.dossie-produto.habilitado` existente;
- `MarkdownJsonMockReader`, contrato de erro REST, filtros de segurança/observabilidade e stub
  HTTP local existentes;
- nenhuma dependência externa nova prevista.

## GO necessário

Nenhuma alteração de produção ou teste executável começa antes de o usuário registrar C0 e C1 no
`todo.md`. O GO deve confirmar explicitamente:

- path público e contrato `200`/erros;
- datas como string e política de nulabilidade/omissão;
- capacidade/portas/adapters no domínio `dossieproduto`;
- exposição autenticada dos dados de cliente e uso de fixture sintética;
- nomes de telemetria e matriz de fault tolerance.
