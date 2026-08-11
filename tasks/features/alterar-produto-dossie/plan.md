# Plano: alterar produtos contratados do dossiê

## Intenção

Expor no `simtr-hub` uma capacidade atômica do domínio `dossieproduto` para incluir ou excluir
produtos contratados de um dossiê, encaminhando a operação ao endpoint
`PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/produto` do MTR e oferecendo a mesma porta de
saída por um simulador próprio.

## Escopo

- expor `PATCH /simtr-hub/v1/dossie-produto/{id}/produto` com contrato público próprio;
- criar a capacidade `AlterarProdutosContratadosDossieProduto` no domínio `dossieproduto`, com
  comando, porta de entrada, caso de uso e porta de saída específicos;
- criar DTOs e mappers independentes para as bordas REST pública, MTR e simulador;
- consumir o contrato MTR v1 descrito no Swagger `simtr-dossie-produto-openapi- 2.20.0.8`;
- selecionar adapter MTR ou simulador por qualifiers e producer CDI explícitos;
- preservar headers API key/OIDC/correlação, classificação de falhas e política de fault
  tolerance aprovada para a operação;
- adicionar sinais de observabilidade sem registrar o payload;
- cobrir contrato HTTP/OpenAPI, validação, mapeamentos, wire MTR, erros, fault tolerance,
  simulador, seleção CDI, observabilidade e guardrails arquiteturais;
- atualizar a documentação fonte e a coleção Postman para refletir a nova capacidade.

## Fora de escopo

- implementar alteração de garantia, captura, cancelamento ou consulta de dossiê;
- criar endpoint único de pré-validação, orquestrador, workflow local ou colaboração entre
  domínios;
- alterar o Swagger MTR de referência ou corrigir descrições incorretas dentro dele;
- reutilizar DTO de validação negocial, formulário, MTR ou simulador em outra borda/operação;
- criar persistência, cache, chave idempotente ou estado local do dossiê;
- mudar a property compartilhada `simtr-hub.simulador.dossie-produto.habilitado` ou o config key
  REST Client `dossie-produto`;
- refatorar as capacidades de `dossieproduto` já implementadas;
- alterar versões derivadas `.html`, `.pdf`, `.ppt` ou `.pptx` dos documentos Markdown.

## Contexto verificado

- arquitetura consolidada lida em `doc/arquitetura-ddd-integracoes-atomicas.md`;
- ADRs aplicáveis lidos integralmente: ADR-0002, ADR-0004, ADR-0005 e ADR-0006;
- contrato fonte inspecionado em
  `doc/swagger-mtr/simtr-dossie-produto-openapi- 2.20.0.8`, versão `2.20.0.8`;
- especificação complementar conferida em `doc/api-integracao-mtr-pre-validacao-v1.md`;
- código, contratos e testes inspecionados: fluxo completo de
  `AtualizarFormularioDossieProduto`, fluxo sem corpo de
  `RegistrarValidacaoNegocialDossieProduto`, `DossieProdutoResource`, DTOs REST,
  `DossieProdutoMtrStubTestResource`, testes de contrato, seleção CDI, configuração,
  observabilidade e fixtures do simulador;
- o Swagger define `id` como `int64`, corpo como lista de
  `v1.dossieproduto.ProdutoContratadoDTO1`, `codigo_operacao` e `codigo_modalidade` obrigatórios,
  `excluir` opcional e sucesso `200` sem schema de resposta;
- divergência documental esperada após a implementação: a arquitetura, o README, o ADR-0002 e a
  documentação de observabilidade ainda registram cinco operações ausentes e somente cinco
  capacidades em `dossieproduto`;
- inconsistência no contrato MTR: as descrições de `403` e `404` do endpoint de produto mencionam
  documento, aparentemente por cópia; o plano não replica essa redação na API pública;
- o Swagger não marca explicitamente `requestBody.required`; a proposta do Hub exige corpo, em
  conformidade com as demais operações mutáveis desta borda, e submete essa adaptação ao
  checkpoint de contrato;
- a Definition of Done referenciada por `using-agent-skills` não existe no caminho instalado; as
  verificações deste plano seguem `AGENTS.md`, os critérios por task e a suíte atual do projeto.

## Contrato público proposto

| Elemento | Proposta |
|---|---|
| Método e path | `PATCH /simtr-hub/v1/dossie-produto/{id}/produto` |
| Path param | `id` obrigatório, `Long`, maior que zero |
| Corpo | lista JSON obrigatória de produtos contratados |
| Item | objeto não nulo com `codigo_operacao` e `codigo_modalidade` obrigatórios; `excluir` opcional |
| Lista vazia | aceita, pois o contrato fonte não declara `minItems` |
| Sucesso | `200` sem corpo |
| Erros | contrato público existente `ErroPadraoDto`; `400`, `401`, `403`, `404`, `409` e `500` documentados |
| OpenAPI | gerado pelo Quarkus a partir do Resource e dos DTOs, sem arquivo estático novo |

Exemplo de corpo:

```json
[
  {
    "codigo_operacao": 100,
    "codigo_modalidade": 200,
    "excluir": false
  },
  {
    "codigo_operacao": 300,
    "codigo_modalidade": 400,
    "excluir": true
  }
]
```

## Desenho arquitetural proposto

```text
PATCH /simtr-hub/v1/dossie-produto/{id}/produto
    -> DossieProdutoResource
        -> AlterarProdutosContratadosDossieProduto
            -> AlterarProdutosContratadosDossieProdutoCasoDeUso
                -> SolicitarAlteracaoProdutosContratadosDossieProduto
                    |-- ProdutoDossieProdutoMtrAdapter
                    `-- ProdutoDossieProdutoSimuladorAdapter
```

- `ComandoAlteracaoProdutosContratadosDossieProduto` transporta o identificador e a lista de
  `ProdutoContratadoDossieProduto` no núcleo;
- porta de entrada e porta de saída retornam `Uni<Void>`, pois o `200` MTR não possui corpo;
- `FalhaAlteracaoProdutosContratadosDossieProduto` conserva os dados externos necessários para o
  erro público sem levar DTO MTR ao núcleo;
- REST, MTR e simulador possuem tipos próprios, mesmo quando os campos são estruturalmente iguais;
- o adapter MTR mantém erro protocolar durante a chamada interceptada e traduz a falha somente
  depois da política de fault tolerance;
- o simulador lê `JSON -> DTO do simulador -> Void` por fixture própria e não recebe annotations
  de fault tolerance;
- o novo desenho aplica os ADRs existentes e não propõe domínio, camada compartilhada ou ADR novo.

## Observabilidade proposta

| Camada | Sinal proposto |
|---|---|
| API | span `simtr-hub.api.dossie-produto.produto.alterar` |
| Aplicação | span `simtr-hub.service.dossie-produto.produto.alterar` |
| MTR | span CLIENT `mtr.dossie-produto.produto.alterar` |
| Logs API | prefixo `simtr-hub.dossie-produto.produto` |
| Logs MTR | prefixo `mtr.dossie-produto.produto` |
| Atributos | rota, API v1, origem, flag do simulador, `dossie_produto.id` e quantidade de produtos |

O payload completo, API key, token, URL interna e dados de autenticação não serão registrados em
logs ou atributos novos.

## Fault tolerance aprovada

O GO de 2026-08-10 confirmou que a inclusão/exclusão declarativa de produtos será tratada como
idempotente para esta integração. O método
`ProdutoDossieProdutoMtrClient.alterar` reutilizará a matriz já aplicada às operações v1
análogas:

- `@Timeout` de 2.000 ms;
- `@Retry` com até 3 retries, atraso de 300 ms e jitter de 100 ms, somente para falha de servidor,
  `ProcessingException` e `TimeoutException`;
- abortar retry em falha de negócio ou cliente técnico;
- `@CircuitBreaker` com volume 10, razão 0,5, atraso de 10.000 ms e limiar de 2 sucessos.

A confirmação de idempotência é uma decisão humana deste planejamento; o Swagger 2.20.0.8 não
declara formalmente essa propriedade.

Se futuramente for necessário **não repetir automaticamente esse PATCH**, o incremento deverá:

1. remover a annotation `@Retry` do método `ProdutoDossieProdutoMtrClient.alterar` e o import que
   ficar sem uso;
2. manter `@Timeout` e `@CircuitBreaker`, pois eles limitam/abrem o circuito, mas não repetem a
   mesma chamada por si próprios;
3. substituir o teste de retry por um teste que configure falha recuperável e comprove exatamente
   uma requisição ao stub;
4. ajustar o teste reflexivo da matriz para exigir ausência de `@Retry`, atualizar este plano e
   obter novo checkpoint humano de comportamento observável/fault tolerance antes da mudança.

## Decisões e impactos

| Dimensão | Situação | Checkpoint humano adicional? |
|---|---|---|
| Contrato | Novo path, verbo, status, JSON, validação e OpenAPI públicos | sim — C1 |
| Arquitetura | Nova capacidade atômica dentro do domínio e das camadas já aceitas | sim — C1 |
| Segurança | Nova superfície de entrada e nova chamada autenticada ao MTR, reutilizando providers existentes | sim — C1 |
| Comportamento observável | Novos spans, eventos e atributos; nenhum sinal existente será renomeado | sim — C1 |
| Fault tolerance | Timeout/circuit breaker necessários; retry da mutação depende de decisão explícita | sim — C1 |

## Tarefas

### Task 1 — Inicializar o baseline técnico

**Descrição:** cumprir o fluxo SonarQube antes da primeira alteração de código ou teste executável.
Como esta sessão altera somente documentação, nenhum pacote ou relatório Sonar foi usado para
selecionar baseline. Na retomada, verificar os pacotes e, se existirem, solicitar ao usuário a
fonte do baseline antes de executar o script oficial.

**Critérios de aceitação:**

- fonte do baseline escolhida pelo usuário quando houver pacote offline;
- baseline inicializado pelo modo autorizado e evidência registrada no `todo.md`;
- indisponibilidade de servidor/token registrada como limitação, nunca como aprovação.

**Verificação:**

- comando `./validar-checkpoint-sonarqube.ps1 -InitializeBaseline` com os parâmetros autorizados.

**Dependências:** C0 e C1 aprovados.

**Arquivos prováveis:** nenhum arquivo fonte.

### Task 2 — Congelar o contrato público em RED

**Descrição:** adicionar testes que descrevam o endpoint proposto antes de criar qualquer classe
de produção, comprovando inicialmente a ausência da rota.

**Critérios de aceitação:**

- teste exige `PATCH`, path público, `200` sem corpo e JSON snake_case exato;
- testes exigem corpo, item e campos obrigatórios, `id > 0`, aceitação de lista vazia e
  opcionalidade de `excluir`;
- teste do OpenAPI gerado exige a operação, array de itens e campos requeridos;
- RED falha pela capacidade ausente, não por erro de infraestrutura.

**Verificação:**

- testes focados de contrato HTTP, erro, Jakarta Validation e OpenAPI;
- inspeção da mensagem RED e registro da evidência no checklist.

**Dependências:** Task 1 concluída.

**Arquivos prováveis:**

- `src/test/java/br/gov/caixa/simtr/hub/contrato/DossieProdutoApiContractTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/contrato/DossieProdutoErroApiContractTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/contrato/DossieProdutoValidacaoJakartaContractTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/contrato/DossieProdutoOpenApiContractTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/recurso/ResourceEndpointTest.java`.

### Task 3 — Modelar comando, produto e falha internos

**Descrição:** introduzir apenas os tipos semânticos necessários à nova capacidade no domínio
`dossieproduto`, sem annotations ou DTOs das bordas.

**Critérios de aceitação:**

- comando contém `identificadorDossieProduto` e lista de produtos contratados;
- produto contém código da operação, código da modalidade e indicador de exclusão;
- falha específica classifica negócio, cliente técnico, dependência indisponível e timeout,
  preservando os dados de erro já suportados pelo domínio;
- domínio não importa Resource, REST Client, DTO de borda ou configuração CDI.

**Verificação:**

- testes unitários dos tipos e da falha;
- `ArchUnitProgressivoTest` focado.

**Dependências:** Task 2 com RED caracterizado.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/dominio/modelo/ProdutoContratadoDossieProduto.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/dominio/modelo/ComandoAlteracaoProdutosContratadosDossieProduto.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/dominio/erro/FalhaAlteracaoProdutosContratadosDossieProduto.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/dominio/erro/FalhasDossieProdutoTest.java`.

### Task 4 — Criar portas e caso de uso atômico

**Descrição:** implementar a coordenação mínima `entrada -> caso de uso -> saída`, sem conhecer
MTR, simulador ou REST.

**Critérios de aceitação:**

- porta de entrada `AlterarProdutosContratadosDossieProduto` retorna `Uni<Void>`;
- porta de saída `SolicitarAlteracaoProdutosContratadosDossieProduto` expressa a necessidade da
  aplicação;
- caso de uso apenas delega o comando à porta de saída e propaga sucesso/falha sem bloqueio;
- teste comprova identidade do comando, resultado vazio e propagação de falha.

**Verificação:**

- `AlterarProdutosContratadosDossieProdutoCasoDeUsoTest`;
- `ArchUnitProgressivoTest` focado.

**Dependências:** Task 3 concluída.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/aplicacao/porta/entrada/AlterarProdutosContratadosDossieProduto.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/aplicacao/porta/saida/SolicitarAlteracaoProdutosContratadosDossieProduto.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/aplicacao/casodeuso/AlterarProdutosContratadosDossieProdutoCasoDeUso.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/aplicacao/casodeuso/AlterarProdutosContratadosDossieProdutoCasoDeUsoTest.java`.

### Task 5 — Traduzir o contrato de produto para o MTR

**Descrição:** criar o DTO MTR v1 e o mapper dedicado, mantendo o wire snake_case do Swagger sem
reutilizar o DTO REST.

**Critérios de aceitação:**

- cada item serializa somente `codigo_operacao`, `codigo_modalidade` e `excluir` não nulos;
- lista, ordem dos itens e valores são preservados do comando ao wire;
- mapper não depende de DTO REST ou simulador;
- testes cobrem lista vazia, `excluir` ausente e valores completos.

**Verificação:**

- `ProdutoDossieProdutoMtrMapperTest`;
- comparação estrutural do JSON com o exemplo aprovado.

**Dependências:** Task 4 concluída.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/dto/v1/produto/ProdutoDossieProdutoMtrRequest.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/mapper/ProdutoDossieProdutoMtrMapper.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/mapper/ProdutoDossieProdutoMtrMapperTest.java`.

### Task 6 — Implementar REST Client e erro protocolar MTR

**Descrição:** declarar a chamada MTR v1, seus providers e sua classificação de respostas sem
traduzir a falha antes dos interceptors de fault tolerance.

**Critérios de aceitação:**

- REST Client chama `PATCH /dossie-produto/v1/dossie-produto/{id}/produto` e retorna `Uni<Void>`;
- `RequestHeaderFactory`, filtro OIDC e filtro de observabilidade permanecem registrados;
- `400/404/409/422` são classificados como negócio, demais 4xx como cliente técnico e 5xx como
  servidor, preservando o payload de erro quando válido;
- timeout, circuit breaker e eventual retry correspondem exatamente à decisão C1;
- corpo externo malformado usa fallback público existente sem vazar dados internos.

**Verificação:**

- `ProdutoDossieProdutoMtrClientTest`;
- reflexão das annotations de fault tolerance;
- testes de classificação e fallback de erro.

**Dependências:** Task 5 concluída e decisão de fault tolerance registrada em C1.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/client/ProdutoDossieProdutoMtrClient.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/erro/ProdutoDossieProdutoMtrException.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/client/ProdutoDossieProdutoMtrClientTest.java`.

### Task 7 — Criar contrato próprio do simulador e qualifiers

**Descrição:** preparar a borda simulada com fixture, DTO e mapper próprios e criar os dois
qualifiers exclusivos da capacidade.

**Critérios de aceitação:**

- fixture documenta o endpoint MTR e contém um objeto JSON vazio para o sucesso sem corpo;
- mapper traduz o DTO simulado para `Void` sem reutilizar tipos MTR/REST;
- qualifiers `ProdutoMtr` e `ProdutoSimulador` não conflitam com os das capacidades existentes;
- nenhuma annotation de fault tolerance é aplicada ao simulador.

**Verificação:**

- teste unitário do mapper e leitura da fixture pelo `MarkdownJsonMockReader`;
- inspeção dos qualifiers CDI.

**Dependências:** Task 4 concluída.

**Arquivos prováveis:**

- `src/main/resources/mock/dossieproduto/produto-dossie-produto.md`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/simulador/dto/ProdutoDossieProdutoSimuladorResponse.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/simulador/mapper/ProdutoDossieProdutoSimuladorMapper.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/configuracao/qualificador/ProdutoMtr.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/configuracao/qualificador/ProdutoSimulador.java`.

### Task 8 — Implementar adapters e seleção CDI

**Descrição:** conectar a porta de saída aos adapters MTR/simulador e selecionar uma única
implementação pela property já existente do domínio.

**Critérios de aceitação:**

- adapter MTR mapeia comando, chama o client, traduz falhas losslessly depois da chamada e retorna
  `Void` no sucesso;
- adapter simulador lê a fixture própria e marca origem `mock` sem chamar MTR;
- producer seleciona simulador quando
  `simtr-hub.simulador.dossie-produto.habilitado=true` e MTR quando `false`;
- bootstrap Quarkus não apresenta injeção ambígua ou recursiva.

**Verificação:**

- `ProdutoDossieProdutoMtrAdapterTest`;
- `ProdutoDossieProdutoSimuladorAdapterTest`;
- `ProdutoDossieProdutoPortasProducerTest`.

**Dependências:** Tasks 6 e 7 concluídas.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/adapter/ProdutoDossieProdutoMtrAdapter.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/simulador/adapter/ProdutoDossieProdutoSimuladorAdapter.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/configuracao/ProdutoDossieProdutoPortasProducer.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/mtr/adapter/ProdutoDossieProdutoMtrAdapterTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/saida/simulador/adapter/ProdutoDossieProdutoSimuladorAdapterTest.java`.

### Task 9 — Expor a capacidade pela borda REST observável

**Descrição:** criar o DTO e mapper REST próprios, adicionar o wrapper observável da aplicação e
implementar o método no `DossieProdutoResource` até tornar verdes os testes públicos da Task 2.

**Critérios de aceitação:**

- DTO REST expõe os três campos aprovados e valida item/campos conforme o contrato;
- mapper executa `REST -> interno` e `falha interna -> erro REST`, sem conversão REST -> MTR;
- Resource valida `id` e corpo, chama somente a porta de entrada e retorna `200` sem entidade;
- logs e spans usam os nomes aprovados, registram somente id/contagem/origem e preservam erro;
- testes RED da Task 2 ficam GREEN sem alterar os critérios aprovados.

**Verificação:**

- `ProdutoDossieProdutoRestMapperTest`;
- testes focados de contrato HTTP, erro, Jakarta Validation, OpenAPI e Resource;
- `ProdutoDossieProdutoObservabilidadeTest`.

**Dependências:** Tasks 4 e 8 concluídas.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/entrada/rest/v1/dto/AlteracaoProdutoDossieProdutoRequest.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/entrada/rest/v1/ProdutoDossieProdutoRestMapper.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/entrada/rest/v1/DossieProdutoResource.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/configuracao/ProdutoDossieProdutoObservabilidade.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/entrada/rest/v1/ProdutoDossieProdutoRestMapperTest.java`.

### Checkpoint C2 — Fim da primeira fatia executável

- contrato público e caminho completo pelo simulador verdes;
- testes de domínio, aplicação, mappers, adapters, CDI, Resource e ArchUnit verdes;
- diff revisado em correção, simplicidade, arquitetura, segurança, desempenho e escopo;
- checkpoint SonarQube executado para o fingerprint executável do incremento;
- eventual `NON_COMPLIANT` apresentado ao usuário para decisão explícita.

### Task 10 — Provar wire MTR, headers, erros e fault tolerance

**Descrição:** executar a nova capacidade ponta a ponta contra o stub MTR local e congelar o
comportamento da integração real.

**Critérios de aceitação:**

- wire usa `PATCH`, path v1 exato, lista JSON exata e resposta `200` sem corpo;
- `Content-Type`, `Accept`, API key, bearer token e `traceparent` chegam ao stub;
- erros de negócio preservam o corpo e não sofrem retry;
- falhas recuperáveis seguem a decisão C1 e, se retry estiver autorizado, repetem o mesmo wire;
- seleção MTR/simulador funciona nos dois valores de configuração sem rede externa.

**Verificação:**

- `ProdutoDossieProdutoMtrContractTest`;
- `ProdutoDossieProdutoSelecaoSimuladorQuarkusTest`;
- testes focados do client, adapters e producer.

**Dependências:** C2 tecnicamente concluído.

**Arquivos prováveis:**

- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/integracao/ProdutoDossieProdutoMtrContractTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/integracao/ProdutoDossieProdutoSelecaoSimuladorQuarkusTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/integracao/DossieProdutoMtrStubTestResource.java`, somente se o contexto genérico existente não cobrir o path novo;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/configuracao/ProdutoDossieProdutoPortasProducerTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/dossieproduto/adaptador/configuracao/ProdutoDossieProdutoObservabilidadeTest.java`.

### Task 11 — Congelar o novo comportamento observável

**Descrição:** proteger os spans, eventos e atributos novos sem alterar os sinais das oito
capacidades anteriores.

**Critérios de aceitação:**

- testes encontram os spans API, aplicação e CLIENT aprovados com parentage e rota corretos;
- logs de início, conclusão e falha incluem operação, id, contagem e resultado;
- payload, API key, token e URL interna não aparecem nos sinais adicionados;
- nomes e atributos existentes permanecem idênticos.

**Verificação:**

- `ObservabilidadeSpansContratoTest`;
- `ObservabilidadeLogsContratoTest`;
- testes focados de observabilidade da capacidade.

**Dependências:** Task 10 concluída.

**Arquivos prováveis:**

- `src/test/java/br/gov/caixa/simtr/hub/arquitetura/observabilidade/ObservabilidadeSpansContratoTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/arquitetura/observabilidade/ObservabilidadeLogsContratoTest.java`.

### Task 12 — Atualizar o estado arquitetural documentado

**Descrição:** retirar a operação de produto da lista de ausências e registrar a nona capacidade
do Hub e a sexta de `dossieproduto`, sem criar decisão arquitetural nova.

**Critérios de aceitação:**

- README e consolidado listam o endpoint público novo e mantêm somente quatro operações ausentes;
- ADR-0002 e seu resumo recebem apenas a atualização factual do inventário, preservando a decisão
  de limites por domínio/capacidade;
- documentação de arquitetura/observabilidade não afirma mais que produto está ausente;
- nenhum formato derivado é atualizado.

**Verificação:**

- busca textual por contagens e pela rota de produto em documentos fonte;
- inspeção do diff documental contra o comportamento implementado.

**Dependências:** Tasks 9 a 11 concluídas.

**Arquivos prováveis:**

- `README.md`;
- `doc/arquitetura-ddd-integracoes-atomicas.md`;
- `doc/adr/0002-limites-por-dominio-e-capacidade.md`;
- `doc/adr/README.md`;
- `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`.

### Task 13 — Atualizar catálogo observável e coleção de uso

**Descrição:** documentar os sinais novos e adicionar uma requisição exercitável da rota pública
na coleção Postman.

**Critérios de aceitação:**

- catálogo contém spans API/aplicação/MTR, prefixos de eventos e atributos estáveis aprovados;
- coleção Postman contém `PATCH /simtr-hub/v1/dossie-produto/{{idDossie}}/produto` com exemplo de
  inclusão e exclusão;
- JSON da coleção permanece válido;
- nenhum token, API key ou segredo real é adicionado.

**Verificação:**

- parse JSON de `doc/postman/simtr-hub.postman_collection.json`;
- inspeção do catálogo e da requisição Postman.

**Dependências:** Tasks 11 e 12 concluídas.

**Arquivos prováveis:**

- `doc/catalogo-observabilidade.md`;
- `doc/postman/simtr-hub.postman_collection.json`.

### Task 14 — Executar verificação final e preparar encerramento

**Descrição:** validar a feature completa, revisar o diff e registrar evidências sem inferir o
encerramento humano.

**Critérios de aceitação:**

- todos os testes focados e `mvn -q test` passam;
- OpenAPI gerado contém somente o contrato aprovado;
- cobertura permanece pelo menos 85%, duplicação no máximo 5% e não há issue nova no checkpoint,
  ou a situação `NON_COMPLIANT` recebe decisão humana;
- nenhum arquivo fora do escopo nem formato derivado foi alterado;
- documentação corresponde ao estado implementado.

**Verificação:**

- suíte Maven completa;
- checkpoint `./validar-checkpoint-sonarqube.ps1`;
- `git diff --check`, revisão do diff e `git status --short`;
- checklist e evidências atualizados para revisão humana final.

**Dependências:** Tasks 10 a 13 concluídas.

**Arquivos prováveis:**

- `tasks/features/alterar-produto-dossie/todo.md`.

### Checkpoint CF — Revisão e encerramento humano

- critérios de aceitação e verificações apresentados com evidências;
- qualquer `NON_COMPLIANT` resolvido por `ContinuarAjustes`, `AceitarExcepcionalmente` ou
  `Reprovar`, conforme decisão do usuário;
- usuário decide explicitamente se aceita e encerra a feature.

## SonarQube

- o planejamento inicial foi documental; após o GO, o baseline foi inicializado antes da primeira
  alteração executável;
- fonte do baseline de implementação: SonarQube Docker local (`LOCAL_SONAR`);
- pacote autorizado: nenhum — o diretório `sonar/` não existe;
- estado inicial: `READY` e `COMPLIANT`, 219 issues no baseline, cobertura 87,8%, duplicação 3,7%
  e nenhuma violação;
- checkpoints esperados: C2 após a primeira fatia executável coerente e Task 14 após integração,
  observabilidade e documentação finais;
- com baseline exclusivamente offline, executar testes locais e registrar que o estado Sonar
  atual permanece `UNVERIFIED`.

## Riscos e controles

| Risco | Impacto | Controle |
|---|---|---|
| Repetir uma mutação sem idempotência comprovada | alto | C1 decide explicitamente se a matriz de retry existente pode ser aplicada; sem decisão, não implementar a chamada |
| Acoplar contratos estruturalmente iguais | alto | DTO e mapper próprios para REST, MTR e simulador; testes de dependência e ArchUnit |
| Divergir do wire MTR 2.20.0.8 | alto | testes contra stub validam método, path, headers, JSON exato e resposta vazia |
| Propagar descrições incorretas de documento no endpoint de produto | médio | usar linguagem de produto no contrato público e registrar a divergência do Swagger como evidência |
| Tornar corpo opcional por omissão do Swagger | médio | contrato público propõe corpo obrigatório e exige aprovação em C1 |
| Aceitar item nulo incompatível com o schema | médio | validação de elemento não nulo e teste de erro público |
| Ambiguidade CDI entre MTR e simulador | alto | qualifiers exclusivos, producer explícito e testes nos dois modos |
| Vazar payload ou credenciais em logs/traces | alto | observar apenas id/contagens/origem e testar ausência de dados sensíveis |
| Renomear sinais existentes ao editar Resource compartilhado | alto | testes de caracterização preservam os oito fluxos anteriores |
| Documentação e artefatos derivados divergirem | baixo | atualizar somente Markdown/JSON fonte; `.html`, `.pdf`, `.ppt` e `.pptx` permanecem intocados |

## Decisões registradas no GO

- contrato público aprovado com corpo obrigatório, item não nulo, lista vazia aceita, `excluir`
  opcional e `200` sem corpo;
- capacidade `AlterarProdutosContratadosDossieProduto` aprovada no domínio `dossieproduto`;
- nova entrada e reutilização dos providers de API key, OIDC e correlação aprovadas, sem payload
  nos sinais;
- nomes de spans, eventos e atributos propostos aprovados;
- idempotência confirmada pelo usuário e matriz de até 3 retries autorizada para falhas
  recuperáveis;
- a remoção futura de retry seguirá o ajuste descrito em **Fault tolerance aprovada** e exigirá
  atualização de plano/checkpoint antes da alteração.

## GO necessário

O GO humano e as decisões de C1 foram registrados em 2026-08-10. A execução continua estritamente
pelo próximo item pendente do `todo.md`, começando pelo baseline SonarQube antes da primeira
alteração executável.
