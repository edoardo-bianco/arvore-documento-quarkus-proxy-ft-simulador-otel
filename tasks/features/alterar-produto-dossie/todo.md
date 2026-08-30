# Checklist: alterar produtos contratados do dossiê

## Estado

- **Branch:** `feature/alterar-produto-dossie`
- **Escopo:** produção, testes e documentação
- **Próximo item:** nenhum — feature revalidada e encerrada pelo usuário

## Checklist

- [x] 0.1 Confirmar intenção e critérios de sucesso;
- [x] 0.2 Ler arquitetura, índice e ADRs 0002, 0004, 0005 e 0006;
- [x] 0.3 Inspecionar Swagger 2.20.0.8, código, contratos, simulador e testes relacionados;
- [x] 0.4 Registrar plano, riscos, dependências e checkpoints;
- [x] C0 Registrar GO humano antes da primeira alteração executável;
- [x] C1 Aprovar contrato público, desenho da capacidade, segurança, observabilidade e decisão de retry;
- [x] 1.1 Verificar pacotes `sonar/`, obter escolha quando aplicável e inicializar baseline;
- [x] 2.1 Escrever testes RED do contrato HTTP, erros e validação;
- [x] 3.1 Criar comando, produto contratado e falha internos;
- [x] 4.1 Criar portas e caso de uso atômico;
- [x] 5.1 Criar DTO e mapper MTR v1;
- [x] 6.1 Criar REST Client e erro protocolar MTR com fault tolerance aprovada;
- [x] 7.1 Criar fixture, DTO, mapper do simulador e qualifiers;
- [x] 8.1 Criar adapters MTR/simulador e seleção CDI;
- [x] 9.1 Expor a capacidade pela borda REST e adicionar observabilidade da aplicação;
- [x] C2 Validar primeira fatia executável, revisar diff e executar checkpoint SonarQube;
- [x] 10.1 Provar wire, headers, erros e fault tolerance contra o stub MTR;
- [x] 11.1 Congelar spans, eventos e atributos novos sem alterar sinais existentes;
- [x] 12.1 Atualizar README, arquitetura consolidada, ADR-0002, índice e documentação operacional;
- [x] 13.1 Atualizar catálogo de observabilidade e coleção Postman;
- [x] 14.1 Executar testes focados, suíte Maven e checkpoint SonarQube final;
- [x] 15.1 Remover o teste do OpenAPI gerado e alinhar a verificação ao padrão existente;
- [x] C3 Aprovar `204 No Content` no MTR real, no Hub e no OpenAPI público do Hub;
- [x] 16.1 Alterar o sucesso MTR/Hub/OpenAPI para `204 No Content` e revalidar;
- [x] CF Revisar evidências e solicitar nova aceitação/encerramento humano.

## Decisões humanas

| Checkpoint | Status | Data | Evidência | Aprovador |
|---|---|---|---|---|
| C0 | GO | 2026-08-10 | Usuário respondeu explicitamente `go` após revisar o planejamento | usuário |
| C1 — contrato | APROVADO | 2026-08-10 | Corpo obrigatório, item não nulo, lista vazia aceita, `excluir` opcional e `200` vazio | usuário |
| C1 — arquitetura | APROVADO | 2026-08-10 | Capacidade `AlterarProdutosContratadosDossieProduto` no domínio `dossieproduto` | usuário |
| C1 — segurança | APROVADO | 2026-08-10 | Nova entrada reutiliza API key, OIDC e correlação existentes, sem payload em sinais | usuário |
| C1 — observabilidade | APROVADO | 2026-08-10 | Spans/eventos de `dossie-produto.produto.alterar` | usuário |
| C1 — retry | APROVADO | 2026-08-10 | Usuário confirmou idempotência e autorizou até 3 retries em falhas recuperáveis; remoção futura exige retirar `@Retry` e ajustar testes | usuário |
| CF — aceite inicial | ACEITO / ENCERRADO | 2026-08-11 | Usuário respondeu explicitamente `ok validado` e autorizou commit, push e PR para `main` | usuário |
| CF — pós-Task 15 | ACEITO / ENCERRADO | 2026-08-11 | Usuário respondeu `proceder` ao pedido de novo aceite após revisar as evidências da Task 15 | usuário |
| C3 — status de sucesso | APROVADO | 2026-08-11 | Usuário confirmou MTR real `204`, Hub `204` e OpenAPI público do Hub `204`, apesar do Swagger MTR declarar `200` | usuário |
| CF — pós-Task 16 | ACEITO / ENCERRADO | 2026-08-11 | Usuário autorizou preparar o PR com a correção após esclarecer que o RED foi resolvido e não testava o OpenAPI gerado | usuário |

## Evidências técnicas

- 2026-08-10 — Swagger local `simtr-dossie-produto-openapi- 2.20.0.8` confirmado na versão
  `2.20.0.8`: `PATCH`, `id int64`, lista de `ProdutoContratadoDTO1`, códigos obrigatórios,
  `excluir` opcional e sucesso `200` sem schema de resposta.
- 2026-08-10 — arquitetura consolidada, índice, ADRs 0002/0004/0005/0006, templates e fluxos
  análogos de formulário/validação negocial inspecionados.
- 2026-08-10 — inconsistência do Swagger registrada: descrições de `403/404` mencionam documento
  no endpoint de produto.
- 2026-08-10 — branch `feature/alterar-produto-dossie` criada; `.tools/` não rastreado foi
  preservado e permanece fora do escopo.
- 2026-08-10 — sessão classificada como exclusivamente documental; baseline, Maven e SonarQube
  não foram executados conforme `AGENTS.md`.
- 2026-08-10 — usuário registrou GO para C0/C1, confirmou a idempotência da alteração declarativa
  de produtos e autorizou a repetição automática de até 3 retries em falhas recuperáveis.
- 2026-08-10 — ajuste para desabilitar repetição documentado: remover `@Retry` do método MTR,
  preservar `@Timeout`/`@CircuitBreaker` e trocar os testes para exigir uma única chamada.
- 2026-08-10 — diretório `sonar/` inexistente; baseline exclusivamente local inicializado pelo
  script oficial antes de qualquer alteração executável.
- 2026-08-10 — baseline `READY`, fonte `LOCAL_SONAR`, situação técnica `COMPLIANT`, 219 issues,
  cobertura 87,8%, duplicação 3,7% e nenhuma violação; decisão humana não requerida.
- 2026-08-10 — RED executado com
  `mvn -q "-Dtest=DossieProdutoApiContractTest,DossieProdutoErroApiContractTest,DossieProdutoValidacaoJakartaContractTest,DossieProdutoOpenApiContractTest" test`:
  28 testes, 7 falhas e 1 erro esperados; os outros 20 testes passaram.
- 2026-08-10 — evidência RED localizada exclusivamente na capacidade ausente: rota pública
  retornou `404` nos cenários de sucesso/validação, operação não apareceu no OpenAPI e o método
  `alterarProdutosContratadosDossieProduto(Long, List)` não existe no Resource.
- 2026-08-10 — RED da tarefa 3.1 executado com
  `mvn -q "-Dtest=AlteracaoProdutosContratadosDossieProdutoModeloTest,FalhasDossieProdutoTest" test`:
  compilação de testes falhou pela ausência esperada de
  `FalhaAlteracaoProdutosContratadosDossieProduto`, antes da criação dos tipos de produção.
- 2026-08-10 — GREEN da tarefa 3.1 executado com
  `mvn -q "-Dtest=AlteracaoProdutosContratadosDossieProdutoModeloTest,FalhasDossieProdutoTest,ArchUnitProgressivoTest" test`:
  41 testes passaram, sem falhas, erros ou violações arquiteturais. Os testes RED do contrato
  público permanecem intencionalmente pendentes até a borda REST da tarefa 9.1.
- 2026-08-10 — RED da tarefa 4.1 executado com
  `mvn -q "-Dtest=AlterarProdutosContratadosDossieProdutoCasoDeUsoTest" test`: compilação de
  testes falhou pela ausência esperada das portas
  `AlterarProdutosContratadosDossieProduto` e
  `SolicitarAlteracaoProdutosContratadosDossieProduto`.
- 2026-08-10 — GREEN da tarefa 4.1 executado com
  `mvn -q "-Dtest=AlterarProdutosContratadosDossieProdutoCasoDeUsoTest,ArchUnitProgressivoTest" test`:
  35 testes passaram, sem falhas, erros ou violações arquiteturais; foram comprovados o mesmo
  comando delegado, o resultado vazio e a propagação da mesma falha, sem bloqueio ou dependência
  das bordas REST, MTR e simulador.
- 2026-08-10 — RED da tarefa 5.1 executado com
  `mvn -q "-Dtest=ProdutoDossieProdutoMtrMapperTest" test`: compilação de testes falhou pela
  ausência esperada de `ProdutoDossieProdutoMtrMapper`, antes da criação dos tipos MTR.
- 2026-08-10 — GREEN da tarefa 5.1 executado com
  `mvn -q "-Dtest=ProdutoDossieProdutoMtrMapperTest,ArchUnitProgressivoTest" test`: 35 testes
  passaram, sem falhas, erros ou violações arquiteturais; o JSON estrutural comprovou a ordem da
  lista, os nomes `codigo_operacao`/`codigo_modalidade`, `excluir` quando presente, sua omissão
  quando nulo e a preservação da lista vazia.
- 2026-08-10 — RED da tarefa 6.1 executado com
  `mvn -q "-Dtest=ProdutoDossieProdutoMtrClientTest" test`: compilação de testes falhou pela
  ausência esperada de `ProdutoDossieProdutoMtrException`, antes da criação do erro protocolar e
  do REST Client.
- 2026-08-10 — GREEN da tarefa 6.1 executado com
  `mvn -q "-Dtest=ProdutoDossieProdutoMtrClientTest,ArchUnitProgressivoTest" test`: 39 testes
  passaram, sem falhas, erros ou violações arquiteturais; foram comprovados `PATCH`, path MTR v1,
  corpo `List<ProdutoDossieProdutoMtrRequest>`, retorno `Uni<Void>`, headers/providers existentes,
  classificação `400/404/409/422` como negócio, demais 4xx como cliente técnico, 5xx como servidor,
  fallback público e a matriz C1 de timeout, até 3 retries e circuit breaker.
- 2026-08-10 — RED da tarefa 7.1 executado com
  `mvn -q "-Dtest=ProdutoDossieProdutoSimuladorMapperTest" test`: compilação de testes falhou
  pela ausência esperada de `ProdutoMtr`, `ProdutoSimulador` e
  `ProdutoDossieProdutoSimuladorResponse`, antes da criação do contrato simulado.
- 2026-08-10 — GREEN da tarefa 7.1 executado com
  `mvn -q "-Dtest=ProdutoDossieProdutoSimuladorMapperTest,ArchUnitProgressivoTest" test`: 37
  testes passaram, sem falhas, erros ou violações arquiteturais; a fixture documenta o endpoint
  MTR e fornece `{}`, o DTO/mapper próprios produzem `Void`, os qualifiers CDI são exclusivos e
  não há `@Timeout`, `@Retry` ou `@CircuitBreaker` no mapper do simulador.
- 2026-08-10 — RED da tarefa 8.1 executado com
  `mvn -q "-Dtest=ProdutoDossieProdutoMtrAdapterTest,ProdutoDossieProdutoSimuladorAdapterTest,ProdutoDossieProdutoPortasProducerTest" test`:
  compilação de testes falhou pela ausência esperada de `ProdutoDossieProdutoMtrAdapter`, antes
  da criação dos adapters e do producer.
- 2026-08-10 — GREEN da tarefa 8.1 executado com
  `mvn -q "-Dtest=ProdutoDossieProdutoMtrAdapterTest,ProdutoDossieProdutoSimuladorAdapterTest,ProdutoDossieProdutoPortasProducerTest,ArchUnitProgressivoTest" test`:
  45 testes passaram, sem falhas, erros ou violações arquiteturais; o adapter MTR preserva o wire
  e traduz negócio/cliente técnico/servidor/timeout losslessly depois do client, o simulador lê a
  fixture, retorna `Void`, marca origem `mock` sem depender de MTR ou fault tolerance, e o bootstrap
  Quarkus resolveu uma única porta com seleção pela property existente.
- 2026-08-11 — RED final da tarefa 9.1 reproduzido com
  `mvn -q "-Dtest=DossieProdutoOpenApiContractTest" test`: 1 teste executado e 1 falha esperada;
  embora o runtime já retornasse `200` vazio, o OpenAPI inferia conteúdo JSON para a resposta de
  sucesso.
- 2026-08-11 — GREEN da tarefa 9.1 executado com
  `mvn -q "-Dtest=DossieProdutoApiContractTest,DossieProdutoErroApiContractTest,DossieProdutoValidacaoJakartaContractTest,DossieProdutoOpenApiContractTest,ProdutoDossieProdutoRestMapperTest,ProdutoDossieProdutoObservabilidadeTest,ArchUnitProgressivoTest" test`:
  68 testes passaram, sem falhas, erros ou violações arquiteturais; `content = {}` suprimiu o
  conteúdo inferido no OpenAPI da resposta `200`, sem alterar o teste nem o retorno HTTP vazio.
  A revisão de correção, simplicidade, arquitetura, segurança, desempenho e escopo não encontrou
  bloqueios, e `git diff --check` passou com apenas avisos de normalização LF para CRLF. C2 e o
  checkpoint SonarQube não foram executados por orientação explícita do usuário.
- 2026-08-11 — validação executável do C2 executada com
  `mvn -q "-Dtest=AlteracaoProdutosContratadosDossieProdutoModeloTest,FalhasDossieProdutoTest,AlterarProdutosContratadosDossieProdutoCasoDeUsoTest,ProdutoDossieProdutoMtrMapperTest,ProdutoDossieProdutoMtrClientTest,ProdutoDossieProdutoSimuladorMapperTest,ProdutoDossieProdutoMtrAdapterTest,ProdutoDossieProdutoSimuladorAdapterTest,ProdutoDossieProdutoPortasProducerTest,ProdutoDossieProdutoRestMapperTest,ProdutoDossieProdutoObservabilidadeTest,DossieProdutoApiContractTest,DossieProdutoErroApiContractTest,DossieProdutoValidacaoJakartaContractTest,DossieProdutoOpenApiContractTest,ArchUnitProgressivoTest" test`:
  102 testes passaram, sem falhas, erros ou violações arquiteturais. A revisão completa das Tasks
  3–9.1 não encontrou achado `Critical` ou obrigatório; `git diff --check` e a inspeção de
  whitespace passaram. Como observação não bloqueante, o `DossieProdutoResource` aproxima-se de
  1.000 linhas, mas sua decomposição exigiria mudança arquitetural fora do C2.
- 2026-08-11 — checkpoint C2 iniciado com `./validar-checkpoint-sonarqube.ps1`, mas abortado antes
  da análise: `.codex/.state/session.json` havia sido recriado nesta sessão com
  `baselineStatus = NOT_REQUIRED_UNTIL_CODE_CHANGE` e `baseline = null`, apesar do baseline local
  registrado em 2026-08-10. Consulta somente leitura ao SonarQube local retornou
  `Insufficient privileges` para `simtr-hub-local`; o container estava operacional e recuperou os
  índices persistidos. Nenhum checkpoint atual foi produzido, nenhum baseline foi reinicializado
  sobre o código alterado e C2 permanece pendente.
- 2026-08-11 — baseline local legítimo recuperado sem incorporar o incremento: o script oficial
  foi executado sobre uma extração temporária do `HEAD` limpo `218e2e8`, confirmou 219 issues,
  cobertura 87,8%, duplicação 3,7% e situação técnica `COMPLIANT`; os temporários foram removidos
  e o worktree da feature permaneceu intacto.
- 2026-08-11 — checkpoint C2 executado com `./validar-checkpoint-sonarqube.ps1`: a suíte Maven
  completa passou com 433 testes, sem falhas, erros ou testes ignorados, e o Compute Engine
  concluiu a análise. A situação técnica é `NON_COMPLIANT`: 245 issues abertas contra 219 no
  baseline, 26 issues novas, 3 issues `CRITICAL` da regra `java:S1192`, cobertura 83,7% abaixo do
  mínimo de 85% e duplicação 3,7% dentro do máximo de 5%. As três issues críticas repetem o
  literal observável `produtos_quantidade` em `ProdutoDossieProdutoObservabilidade`,
  `DossieProdutoResource` e `ProdutoDossieProdutoMtrAdapter`; as outras 23 são de
  manutenibilidade/testes. A decisão humana permanece `PENDING`, portanto C2 continua aberto.
- 2026-08-11 — usuário decidiu explicitamente `ContinuarAjustes`; a decisão foi registrada pelo
  script oficial e o C2 permanece aberto para corrigir issues novas e recuperar a cobertura antes
  de um novo checkpoint.
- 2026-08-11 — ajustes eliminaram 25 das 26 issues novas e todas as 3 críticas; testes MTR
  existentes passaram a seguir o padrão `@QuarkusTest` das capacidades análogas para que o JaCoCo
  contabilize sua execução. Novo checkpoint executou 433 testes sem falhas, erros ou ignorados,
  elevou a cobertura para 87,1% e manteve a duplicação em 3,7%, mas permaneceu `NON_COMPLIANT`
  por uma única issue nova `MINOR` (`java:S7466`) em
  `ProdutoDossieProdutoSimuladorAdapterTest`: usar `var` na variável anônima do recurso
  `try-with-resources`. A nova decisão humana permanece `PENDING` e C2 continua aberto.
- 2026-08-11 — usuário decidiu novamente `ContinuarAjustes`; a decisão foi registrada pelo script
  oficial. O último achado `java:S7466` foi corrigido com `var` no `try-with-resources`, sem mudar
  o comportamento, e o teste focado `ProdutoDossieProdutoSimuladorAdapterTest` passou.
- 2026-08-11 — checkpoint final do C2 executado com `./validar-checkpoint-sonarqube.ps1`: situação
  técnica `COMPLIANT`, 219 issues abertas contra 219 no baseline, nenhuma issue nova, nenhuma issue
  `HIGH`, `BLOCKER` ou `CRITICAL`, cobertura 87,1%, duplicação 3,7% e nenhuma violação. A suíte
  Maven completa produziu 433 testes, sem falhas, erros ou ignorados; decisão humana adicional não
  foi requerida. C2 concluído, sem iniciar a tarefa 10.1.
- 2026-08-11 — tarefa 10.1 comprovada por
  `ProdutoDossieProdutoMtrContractTest` e
  `ProdutoDossieProdutoSelecaoSimuladorQuarkusTest`: o fluxo ponta a ponta preservou `PATCH`, path
  MTR v1, lista JSON, resposta `200` vazia, `Content-Type`, `Accept`, API key, bearer token e
  `traceparent`; erro de negócio `400` preservou o corpo sem retry; erro recuperável `500` repetiu
  exatamente o mesmo wire; a matriz C1 de timeout, até 3 retries e circuit breaker permaneceu
  idêntica; MTR e simulador foram selecionados nos dois valores da configuração sem rede externa.
- 2026-08-11 — conjunto focado da tarefa 10.1 executado com
  `mvn -q "-Dtest=ProdutoDossieProdutoMtrContractTest,ProdutoDossieProdutoSelecaoSimuladorQuarkusTest,ProdutoDossieProdutoMtrClientTest,ProdutoDossieProdutoMtrAdapterTest,ProdutoDossieProdutoSimuladorAdapterTest,ProdutoDossieProdutoPortasProducerTest" test`:
  24 testes passaram, sem falhas, erros ou ignorados. A revisão de correção, simplicidade,
  arquitetura, segurança, desempenho e escopo não encontrou bloqueios; nenhum arquivo de produção,
  dependência ou segredo real foi adicionado pela tarefa.
- 2026-08-11 — checkpoint SonarQube do incremento 10.1 executado com
  `./validar-checkpoint-sonarqube.ps1`: situação técnica `COMPLIANT`, 219 issues contra 219 no
  baseline, nenhuma issue nova, nenhuma issue `HIGH`, `BLOCKER` ou `CRITICAL`, cobertura 87,8%,
  duplicação 3,7% e nenhuma violação. A suíte completa produziu 439 testes, sem falhas, erros ou
  ignorados; decisão humana não requerida. Tarefa 10.1 concluída, sem iniciar 11.1.
- 2026-08-11 — caracterização anterior à tarefa 11.1 executada com
  `ObservabilidadeSpansContratoTest`, `ObservabilidadeLogsContratoTest` e
  `ProdutoDossieProdutoObservabilidadeTest`; os contratos existentes passaram antes da ampliação.
- 2026-08-11 — contratos da tarefa 11.1 congelaram a nona capacidade observável: spans `SERVER`
  da API e `INTERNAL` da aplicação no simulador, além do encadeamento real
  `SERVER -> INTERNAL -> CLIENT` na integração MTR. Foram comprovados trace e parentage, rota,
  método, operação, identificador, quantidade e payload REST desabilitado, sem dados sensíveis nos
  novos atributos da capacidade.
- 2026-08-11 — logs estruturados da capacidade foram congelados nos cinco eventos de sucesso
  (`requisicao.recebida`, `service.iniciado`, `simulador.usado`, `service.concluido` e
  `resposta.enviada`) e nos eventos de início/falha controlada, com operação, identificador,
  quantidade, resultado e tipo de erro quando aplicável. Sentinelas de payload, API key, token e
  URL interna não apareceram nos novos sinais.
- 2026-08-11 — divergência preexistente registrada: o filtro compartilhado
  `RestClientObservabilityFilter` mantém o atributo `rest_client.url` e o campo de log `url` com a
  URL completa da integração. Esses sinais antecedem a feature e foram excluídos apenas da
  asserção sobre atributos novos; corrigi-los alteraria observabilidade comum às capacidades e
  exige escopo e checkpoint humano próprios.
- 2026-08-11 — conjunto focado da tarefa 11.1 executado com
  `mvn -q "-Dtest=ObservabilidadeSpansContratoTest,ObservabilidadeLogsContratoTest,ProdutoDossieProdutoObservabilidadeTest,ProdutoDossieProdutoMtrContractTest" test`:
  13 testes passaram, sem falhas, erros ou ignorados. A revisão de correção, simplicidade,
  arquitetura, segurança, desempenho e escopo não encontrou bloqueios; a tarefa alterou somente
  testes e o checklist, preservando nomes e atributos observáveis existentes.
- 2026-08-11 — checkpoint SonarQube do incremento 11.1 executado com
  `./validar-checkpoint-sonarqube.ps1`: situação técnica `COMPLIANT`, 219 issues contra 219 no
  baseline, nenhuma issue nova, nenhuma issue `HIGH`, `BLOCKER` ou `CRITICAL`, cobertura 87,8%,
  duplicação 3,7% e nenhuma violação. A suíte completa produziu 441 testes, sem falhas, erros ou
  ignorados; decisão humana não requerida. Tarefa 11.1 concluída, sem iniciar 12.1.
- 2026-08-11 — tarefa 12.1 atualizou somente fontes Markdown: `README.md`, arquitetura
  consolidada, ADR-0002, índice de ADRs e documentação operacional. O inventário agora registra
  nove capacidades no Hub, seis em `dossieproduto`, o endpoint público
  `PATCH /simtr-hub/v1/dossie-produto/{id}/produto` e somente quatro operações ainda ausentes.
- 2026-08-11 — ADR-0002 permaneceu `Aceito` e recebeu apenas a atualização factual da sexta
  capacidade de `dossieproduto`; nenhuma decisão arquitetural ou ADR novo foi criado. Também foram
  corrigidas as afirmações obsoletas sobre ausência de endpoint novo e inexistência de teste do
  OpenAPI gerado, em conformidade com o código e os contratos executáveis atuais.
- 2026-08-11 — verificação documental da tarefa 12.1 confirmou a rota nas três visões de estado,
  as contagens nove/seis/quatro e nenhuma ocorrência residual de oito capacidades, cinco
  ausências ou produto não implementado. `git diff --check` não encontrou erro; nenhum formato
  derivado `.html`, `.pdf`, `.ppt` ou `.pptx` foi alterado. Por ser incremento exclusivamente
  documental, Maven e checkpoint SonarQube não foram executados, conforme `AGENTS.md`. Tarefa
  12.1 concluída, sem iniciar 13.1.
- 2026-08-11 — tarefa 13.1 atualizou
  `doc/arquitetura-distribuida/catalogo-observabilidade.md` com a nona capacidade:
  spans `simtr-hub.api.dossie-produto.produto.alterar`,
  `simtr-hub.service.dossie-produto.produto.alterar` e
  `mtr.dossie-produto.produto.alterar`, prefixos de eventos REST/aplicação/MTR, id, quantidade,
  origem, resultado e atributos de sucesso/falha. Os nomes dos testes e as contagens do catálogo
  foram alinhados a 18 spans manuais, nove declarações CLIENT e 45 eventos de sucesso.
- 2026-08-11 — a coleção Postman recebeu exatamente uma requisição
  `PATCH /simtr-hub/v1/dossie-produto/{{idDossie}}/produto`, com URL local e corpo de dois itens:
  um exemplo de inclusão (`excluir=false`) e outro de exclusão (`excluir=true`). A coleção v2.1 e
  o JSON interno do corpo foram parseados com sucesso; método, URL, linguagem JSON, dois itens e
  flags foram confirmados, sem padrão de token, API key, senha ou secret nas linhas adicionadas.
- 2026-08-11 — revisão do diff do item 13.1 confirmou somente o catálogo, a coleção e este
  checklist no incremento. `git diff --check` dos dois artefatos não encontrou erro; nenhum
  formato derivado foi alterado. Por permanecer documental, Maven e SonarQube não foram
  executados. Tarefa 13.1 concluída, sem iniciar 14.1.
- 2026-08-11 — suíte focada final executada com 20 classes de contrato HTTP, validação, OpenAPI,
  domínio, aplicação, mappers, adapters, REST Client, MTR, simulador, seleção CDI, observabilidade
  e ArchUnit: 113 testes passaram, sem falhas, erros ou ignorados. O contrato
  `DossieProdutoOpenApiContractTest#descreveContratoPublicoDeAlteracaoDeProdutos` passou e
  confirmou especificamente a operação no documento gerado.
- 2026-08-11 — `mvn -q test` passou com 441 testes em 110 relatórios, sem falhas, erros ou
  ignorados. A execução limpa realizada pelo checkpoint final repetiu os mesmos 441 testes com o
  mesmo resultado.
- 2026-08-11 — revisão final de correção, legibilidade, arquitetura, segurança, desempenho e
  escopo aprovada sem achados críticos ou obrigatórios: entrada validada no Resource, providers de
  API key/OIDC/observabilidade preservados, nenhuma dependência nova, nenhum bloqueio ou
  `subscribe` manual, três loops lineares sobre coleções recebidas, ArchUnit verde, documentação e
  Postman consistentes. O `rest_client.url` completo e o aviso de API deprecada no filtro
  compartilhado permanecem FYIs preexistentes, não introduzidos pela feature e já documentados
  para eventual escopo/checkpoint próprios.
- 2026-08-11 — checkpoint SonarQube final executado com
  `./validar-checkpoint-sonarqube.ps1`: situação técnica `COMPLIANT`, 219 issues contra 219 no
  baseline, nenhuma issue nova, nenhuma issue `HIGH`, `BLOCKER` ou `CRITICAL`, cobertura 87,8%,
  duplicação 3,7% e nenhuma violação; decisão técnica humana não requerida.
- 2026-08-11 — `git diff --check` final não encontrou erro e nenhum formato derivado `.html`,
  `.pdf`, `.ppt` ou `.pptx` foi alterado. `.tools/` permaneceu fora do escopo e intocado. Tarefa
  14.1 concluída; checkpoint CF permanece pendente para decisão explícita do usuário.
- 2026-08-11 — usuário validou explicitamente a entrega com `ok validado`, aceitou e encerrou a
  feature no checkpoint CF e autorizou commit, push da branch e abertura de PR para `main`.
- 2026-08-11 — após revisar a cobertura existente, o usuário solicitou explicitamente remover o
  teste sobre o OpenAPI gerado e manter o padrão anterior dos testes. O escopo foi atualizado com
  a Task 15, a feature foi reaberta e o checkpoint CF voltou a depender de nova validação humana.
- 2026-08-11 — Task 15 removeu `DossieProdutoOpenApiContractTest` sem substituição por snapshot ou
  outra inspeção de `/simtr-hub/openapi`. README e documentação operacional passaram a declarar o
  padrão do ADR-0006: contratos HTTP e Java alimentam a geração, sem testar o artefato gerado. O
  endpoint, DTO, annotations OpenAPI e todo o código de produção permaneceram inalterados.
- 2026-08-11 — testes focados do padrão restante passaram com
  `mvn -q "-Dtest=DossieProdutoApiContractTest,DossieProdutoErroApiContractTest,DossieProdutoValidacaoJakartaContractTest,ProdutoDossieProdutoRestMapperTest,ArchUnitProgressivoTest" test`.
  Em seguida, `mvn -q clean test` passou com 440 testes em 109 relatórios, sem falhas, erros ou
  ignorados; a redução exata de 441/110 para 440/109 confirma somente a retirada da classe pedida.
- 2026-08-11 — checkpoint SonarQube da Task 15 executado com
  `./validar-checkpoint-sonarqube.ps1`: situação técnica `COMPLIANT`, 219 issues contra 219 no
  baseline, nenhuma issue nova, nenhuma issue `HIGH`, `BLOCKER` ou `CRITICAL`, cobertura 87,8%,
  duplicação 3,7% e nenhuma violação; decisão técnica humana não requerida. A execução limpa do
  checkpoint repetiu os 440 testes aprovados.
- 2026-08-11 — busca em `src/test` não encontrou mais acesso a `/simtr-hub/openapi`;
  `git diff --check` passou, nenhum formato derivado foi alterado e `.tools/` permaneceu fora do
  escopo. Task 15.1 concluída; checkpoint CF pendente para nova decisão explícita do usuário.
- 2026-08-11 — usuário respondeu `proceder` ao pedido direto de validação da Task 15, aceitando e
  encerrando novamente a feature no checkpoint CF e autorizando a atualização do PR existente.
- 2026-08-11 — usuário confirmou explicitamente no checkpoint C3 que o MTR real responde
  `204 No Content`, que o Hub deve responder `204 No Content` e que o OpenAPI público do Hub deve
  documentar `204`, apesar de o Swagger MTR 2.20.0.8 declarar `200`. A feature foi reaberta com a
  Task 16 antes de qualquer alteração executável.
- 2026-08-11 — RED da Task 16 executado com
  `mvn -q "-Dtest=DossieProdutoApiContractTest,ProdutoDossieProdutoMtrContractTest" test`: 16
  testes, seis falhas esperadas, todas por o Hub ainda responder `200` quando o contrato passou a
  exigir `204`. O stub MTR já respondeu `204` e o cliente `Uni<Void>` aceitou a resposta.
- 2026-08-11 — `DossieProdutoResource` passou a construir `Response.noContent()` e sua
  `@APIResponse` pública passou de `200` para `204`; contratos HTTP, MTR, logs e spans foram
  alinhados. Erros, validações, payload, headers, retry e sinais observáveis permaneceram
  inalterados. Nenhum teste do OpenAPI gerado foi reintroduzido e o Swagger MTR permaneceu intacto.
- 2026-08-11 — busca de consistência confirmou que README, arquitetura consolidada, documentação
  operacional, catálogo de observabilidade e coleção Postman não afirmavam sucesso `200` para a
  rota. O GREEN focado passou para API, integração MTR, cliente/adapter, erros, validação,
  observabilidade e ArchUnit.
- 2026-08-11 — a primeira tentativa de `mvn -q clean test` encontrou o JAR bloqueado por um
  `quarkus:dev` deste workspace; somente os dois processos confirmados dessa execução foram
  encerrados. A suíte revelou e permitiu alinhar os dois contratos transversais de logs/spans;
  a repetição limpa final passou com 440 testes em 109 relatórios, sem falhas, erros ou ignorados.
- 2026-08-11 — checkpoint SonarQube da Task 16 executado com
  `./validar-checkpoint-sonarqube.ps1`: situação técnica `COMPLIANT`, 219 issues contra 219 no
  baseline, nenhuma issue nova, nenhuma issue `HIGH`, `BLOCKER` ou `CRITICAL`, cobertura 87,8%,
  duplicação 3,7% e nenhuma violação; decisão técnica humana não requerida.
- 2026-08-11 — revisão final nos eixos de correção, simplicidade, arquitetura, segurança e
  desempenho não encontrou achados bloqueantes; `git diff --check` passou, nenhum formato
  derivado foi alterado e `.tools/` permaneceu fora do escopo e intocado. Task 16.1 concluída;
  checkpoint CF pendente para nova decisão explícita do usuário.
- 2026-08-11 — após esclarecer que as seis falhas RED foram resolvidas pelo GREEN e pertenciam a
  contratos HTTP/MTR, sem inspecionar o OpenAPI gerado, o usuário autorizou explicitamente
  preparar o PR com a correção. Checkpoint CF aceito e feature encerrada novamente; commit, push e
  atualização do PR #11 autorizados.
