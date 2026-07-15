# Checklist de execucao - refatoracao DDD

## Regra de retomada

Antes de executar qualquer item:

- [ ] ler `../doc/arquitetura-ddd-integracoes-atomicas.md`;
- [ ] confirmar com `git rev-parse --abbrev-ref HEAD` a branch da fase atual no formato
  `refactor/ddd-fase-<numero>-baseline`;
- [ ] se a Fase 0 ainda nao terminou, aplicar a regra de bootstrap de `plan.md` e seguir as
  dependencias 0.1–0.4; a exigencia de checkpoint `GO` comeca somente ao entrar na Fase 1;
- [ ] verificar `git diff` e preservar alteracoes do usuario;
- [ ] nao iniciar a fase seguinte sem GO humano.
- [ ] remover sem nova microaprovacao somente o legado de uma capacidade ja migrada quando a
  cobertura equivalente estiver verde e `rg` comprovar que nao restam referencias; preservar os
  contratos externos e continuar respeitando os checkpoints formais. Autorizacao humana
  registrada em 2026-07-12.

## Pre-condicao Git

- [x] G0 Criar a branch local `refactor/ddd-fase-0-baseline` a partir de `main` atualizado.
- [x] G1 Branch `refactor/ddd-fase-0-baseline` confirmada; working tree continha somente a
  atualizacao documental da branch; baseline `mvn -q test`: 100 testes, zero falhas.
- [x] G2 Branch `refactor/ddd-fase-3-baseline` criada a partir do commit `4d24167`, que recebeu
  GO em C2, publicada e configurada para rastrear `origin/refactor/ddd-fase-3-baseline`.
- [x] G3 Branch `refactor/ddd-fase-4-baseline` criada a partir do commit `7613fed`, que recebeu
  GO em C3, publicada e configurada para rastrear `origin/refactor/ddd-fase-4-baseline`.
- [x] G4 Branch `refactor/ddd-fase-5-baseline` criada a partir do commit `1f414fa`, que recebeu
  GO em C4, publicada e configurada para rastrear `origin/refactor/ddd-fase-5-baseline`.

## Ponto de retomada

- **Ultima tarefa concluida:** C5 - Fase 5 encerrada com GO humano.
- **Concluido:** baseline inicial com 100 testes e zero falhas; 22 testes focados de
  caracterizacao HTTP/OpenAPI aprovados para processo, checklist, cinco operacoes de dossie
  produto e credencial de gestao de documento; suite completa com 122 testes, zero falhas, zero
  erros e nenhum teste ignorado.
- **Comportamento registrado:** a resposta publica de checklist omite campos nulos, mesmo que
  esses campos existam no mock interno.
- **Evidencia HTTP:** `baseline-contratos.md` relaciona as oito capacidades aos oraculos
  independentes, erros completos, validacoes e semantica OpenAPI protegidos.
- **Evidencia de observabilidade:** `inventario-observabilidade.md`; 12 testes focados aprovados;
  suite completa com 126 testes, zero falhas, zero erros e zero ignorados.
- **Evidencia da borda MTR:** `baseline-borda-mtr.md`; cinco testes focados percorrem Resource ate
  stub localhost com simulador desabilitado, wire/headers/OIDC/observabilidade, erro lossless,
  retry, timeout e circuit breaker; suite completa com 131 testes, zero falhas, zero erros e zero
  ignorados.
- **Codigo de producao:** nucleo e bordas MTR/simulador do piloto em uso pelo Resource; cadeia
  legada exclusiva do workflow removida depois da comprovacao de cobertura equivalente.
- **Codigo de producao Java:** REST Client/DTO/erro/adapter MTR e adapter/DTO simulador exclusivos
  do workflow preservam wire, providers, FT e observabilidade; qualifiers e producer selecionam a
  porta de saida sem condicional no caso de uso; Resource usa a porta de entrada e nao restam
  referencias ao workflow na fachada, service, gateway, client ou mock legados.
- **Checkpoint:** C0 aprovado apos `mvn -q test` verde em 2026-07-12; Fase 1 desbloqueada.
- **Evidencia do piloto:** `baseline-workflow-dossie-produto.md`; 5 testes novos completam wire,
  headers, resposta, erro lossless, retry, timeout e matriz FT do workflow; conjunto focado com 39
  testes e suite completa com 136 testes, zero falhas, zero erros e zero ignorados.
- **Evidencia do nucleo:** 2 testes unitarios com fake da porta de saida; guardrails progressivos
  impedem imports de bordas na aplicacao e exposicao de implementacao pela porta de entrada; suite
  completa com 138 testes, zero falhas, zero erros e zero ignorados.
- **Evidencia do adapter MTR:** testes unitarios cobrem resposta, erro lossless, fallback de corpo
  invalido, timeout e matriz FT; `@QuarkusTest` percorre o novo client ate o stub com
  wire/headers/OIDC/trace, erro sem retry e retry apos 500; as oito operacoes publicas permanecem
  semanticamente equivalentes e a exposicao preexistente dos REST Clients no OpenAPI esta
  registrada como advertencia, sem contorno por `@Operation(hidden = true)`;
  `mvn -q clean test` com 153 testes, zero falhas, zero erros e zero ignorados.
- **Evidencia do adapter simulador e CDI:** `@QuarkusTest` prova bootstrap e selecao com simulador
  ligado e desligado; a fixture e lida por DTO proprio da borda; todos os testes Quarkus usam o
  perfil padrao `test`, sem `QuarkusTestProfile`/`@TestProfile`; `mvn -q clean test` com 155 testes,
  zero falhas, zero erros e zero ignorados.
- **Evidencia do adapter REST:** teste RED/GREEN prova que o metodo de workflow usa a porta de
  entrada; resultado e falha interna sao traduzidos para o contrato publico; testes HTTP, erro
  lossless, simulador/MTR, OpenAPI, observabilidade e ArchUnit permanecem verdes. A cadeia legada
  substituida e tres testes redundantes foram removidos com autorizacao humana; a suite passa de
  156 para 153 testes sem perda de cenarios. `mvn -q clean test`: 153 elementos `testcase`, zero
  falhas, zero erros e zero ignorados. O atributo agregado do Surefire informa 147 porque registra
  incorretamente zero na suite ArchUnit, apesar dos seis casos JUnit presentes e verdes no XML.
- **Revisao de C1:** concluida em 2026-07-12; branch correta, `diff --check` sem erros,
  suite/build verdes, guardrails carregados, contratos publicos preservados, cadeia legada do
  workflow sem referencias e nenhum profile proprio ou `@Operation(hidden = true)`.
- **Checkpoint C1:** GO humano confirmado em 2026-07-12; Fase 2 desbloqueada.
- **Evidencia da criacao:** `baseline-criacao-dossie-produto.md`; contrato HTTP/JSON, validacao,
  OpenAPI, wire/headers/OIDC, simulador, erros lossless, observabilidade, configuracao e matriz FT
  congelados; `mvn -q clean test` com 154 elementos `testcase`, zero falhas, zero erros e zero
  ignorados.
- **Evidencia do novo client de criacao:** quatro testes percorrem DTOs exclusivos ate o stub,
  preservando wire, headers/OIDC/trace, erro de negocio lossless sem retry, retry apos 500 e matriz
  FT; adapters de saida continuam sem dependencia de DTO publico; `mvn -q clean test` verde com
  quatro novos testes.
- **Evidencia do adapter MTR da criacao:** tres testes unitarios cobrem mapeamento completo,
  traducao lossless e timeout; qualifier proprio prepara a selecao CDI sem expor implementacao;
  suite limpa verde com CDI, ArchUnit, OpenAPI e contratos legados preservados.
- **Evidencia da borda simulador da criacao:** dois `@QuarkusTest` comprovam o bootstrap CDI e a
  selecao pelo ambiente padrao `test`: fixture quando habilitado e adapter MTR com OIDC/stub HTTP
  quando desabilitado; `mvn -q clean test` terminou com codigo 0.
- **Evidencia da borda REST da criacao:** request/response Java explicitos, mapper REST e Resource
  usam `CriarDossieProduto`; HTTP, JSON, validacao, erros, simulador/MTR, fault tolerance e
  observabilidade permanecem cobertos. A cadeia legada exclusiva da criacao foi removida sem
  referencias remanescentes.
- **Decisao OpenAPI:** por orientacao humana registrada em 2026-07-13, o documento e gerado
  exclusivamente pelo Quarkus e nao deve ser testado, filtrado, complementado ou manipulado. Os
  dois testes de fingerprint/semantica do artefato gerado foram removidos; nao existe arquivo
  OpenAPI estatico, filtro ou `@Operation(hidden = true)`.
- **Evidencia final da 2.1e:** scanner de spans focado verde; todos os testes Quarkus usam o profile
  padrao `test`, sem Docker, Dev Services, `QuarkusTestProfile` ou `@TestProfile`;
  `mvn -q clean test` com 163 elementos `testcase`, zero falhas, zero erros e zero ignorados. O teste
  HTTP da criacao tambem cobre a resposta nula preservada do contrato legado.
- **Checkpoint C2.1:** GO humano confirmado em 2026-07-13; formulario desbloqueado.
- **Evidencia da caracterizacao do formulario:** `baseline-formulario-dossie-produto.md`; cinco
  cenarios MTR novos congelam wire/headers/resposta, listas e nulos, erro lossless, retry e matriz
  FT; o corpo REST obrigatorio ganhou teste dedicado. `mvn -q clean test` com 169 elementos
  `testcase`, zero falhas, zero erros e zero ignorados; nenhuma classe de producao foi alterada.
- **Evidencia do nucleo do formulario:** tipos internos preservam listas, elementos e campos nulos
  sem copia ou normalizacao; portas de entrada/saida e caso de uso delegador usam somente `Uni` na
  aplicacao. Teste unitario e ArchUnit passaram; `mvn -q clean test` executou 171 casos, com zero
  falhas, zero erros e zero ignorados, no profile padrao `test`, sem Docker ou Dev Services.
- **Evidencia da nova borda MTR do formulario:** DTOs, client, erro, mapper e adapter exclusivos;
  testes unitarios e stub localhost comprovam wire completo, listas/elementos/campos nulos,
  headers/OIDC/trace, resposta, erro lossless sem retry, retry de 500 e matriz FT. O guardrail foi
  convertido para JUnit puro com ArchUnit core, eliminando a corrida de discovery com
  `@QuarkusTest` sem reduzir as 13 regras/provas. `mvn -q clean test` executou 188 casos em 41
  relatorios, com zero falhas, zero erros e zero ignorados, sem Docker ou Dev Services.
- **Evidencia da borda simulador do formulario:** DTO de resposta, qualifier, adapter e producer
  exclusivos leem a fixture existente sem DTO publico e preservam a precedencia do id informado.
  Teste unitario prova a selecao do simulador quando ligado e do MTR quando desligado; testes CDI
  confirmam ambos os estados e o bootstrap no profile padrao `test`. As properties permaneceram
  inalteradas. ArchUnit passou e `mvn -q clean test` executou 191 casos em 43 relatorios, com zero
  falhas, zero erros e zero ignorados, sem Docker ou Dev Services.
- **Evidencia da borda REST do formulario:** mapper REST explicito e Resource usam somente
  `AtualizarFormularioDossieProduto`; HTTP/JSON/validacao, erros lossless, simulador/MTR, matriz
  FT, observabilidade e ArchUnit permanecem verdes. A cadeia legada exclusiva foi removida de
  fachada, service, gateway, REST Client, mapper, mock factory e VOs, sem referencias
  remanescentes. O teste do endpoint OpenAPI gerado foi removido conforme a decisao humana; o
  Quarkus continua responsavel por gera-lo sem filtro ou manipulacao. O profile padrao `test`
  agora declara `quarkus.devservices.enabled=false`. `mvn -q clean test` executou 191 casos em 45
  relatorios, com zero falhas, zero erros e zero ignorados; `git diff --check` passou.
- **Checkpoint C2.2:** GO humano confirmado em 2026-07-13; inclusao de documento desbloqueada.
- **Evidencia da caracterizacao da inclusao de documento:**
  `baseline-documento-dossie-produto.md`; contrato HTTP/JSON/validacao, nulos aceitos, wire MTR v2,
  headers/OIDC/trace, resposta, erro lossless sem retry, retry apos 500, matriz FT, simulador,
  configuracao e observabilidade congelados. O OpenAPI permaneceu sob geracao exclusiva do
  Quarkus, sem teste ou manipulacao. `mvn -q clean test` executou 197 casos em 46 relatorios, com
  zero falhas, zero erros e zero ignorados, sem Docker ou Dev Services; nenhuma classe de producao
  foi alterada.
- **Evidencia do nucleo da inclusao de documento:** comando, seis tipos aninhados/resultado,
  portas de entrada e saida e caso de uso usam nomes internos; dominio permanece Java puro e a
  aplicacao usa somente tipos internos e `Uni`. O teste JUnit puro preserva comando, listas,
  elementos nulos, resultado e falha; os 13 guardrails ArchUnit passaram. `mvn -q clean test`
  executou 199 casos em 47 relatorios, com zero falhas, zero erros e zero ignorados, sem Docker ou
  Dev Services.
- **Evidencia da borda MTR v2 da inclusao de documento:** request/response e erro de protocolo,
  REST Client, mapper, falha interna, qualifier e adapter sao exclusivos da capacidade e nao
  reutilizam DTO REST publico. Stub HTTP local comprovou path, JSON, headers, OIDC local,
  nulabilidade, ids de resposta, erro 400 sem retry e retry 500 com o mesmo wire. Matriz FT,
  declaracao do span e 13 guardrails ArchUnit passaram. `mvn -q clean test` executou 209 casos em
  49 relatorios, com zero falhas, zero erros e zero ignorados, no profile `test`, sem Docker ou Dev
  Services.
- **Evidencia da borda simulador da inclusao de documento:** DTO proprio le
  `id_documento`/`id_instancia_documento` da fixture snake_case e o adapter retorna somente o
  resultado interno. Qualifier e producer selecionam simulador quando a property existente esta
  ligada e MTR quando desligada; testes CDI provam ambos os estados sem profile adicional. A
  configuracao permaneceu inalterada, os 13 guardrails ArchUnit passaram e `mvn -q clean test`
  executou 213 casos em 52 relatorios, com zero falhas, zero erros e zero ignorados, sem Docker ou
  Dev Services.
- **Evidencia da borda REST da inclusao de documento:** request/response explicitos, mapper
  lossless e observabilidade ligam o Resource somente a porta de entrada. Contratos HTTP, JSON,
  validacao, nulos, erros, MTR v2, simulador, FT, spans e logs passaram sem testar ou manipular o
  OpenAPI gerado pelo Quarkus. A cadeia legada exclusiva foi removida de Fachada, Service,
  Gateway, REST Client, MapStruct e mock factory, junto dos DTOs/VOs sem uso; `rg` confirmou
  ausencia de referencias. `mvn -q clean test` executou 215 casos em 55 relatorios, com zero
  falhas, zero erros e zero ignorados, no profile `test`, sem Docker ou Dev Services;
  `git diff --check` passou.
- **Checkpoint C2.3:** GO humano confirmado em 2026-07-13; validacao negocial desbloqueada.
- **Checkpoint temporario de sessao (2026-07-13):** Task 2.4a iniciada apenas em documentacao e
  inventario. Foram localizados o endpoint `PATCH /simtr-hub/v1/dossie-produto/{id}/validacao-negocial`,
  o REST Client MTR v1, a cadeia legada, a fixture do simulador, a property de selecao, a matriz
  de timeout/retry/circuit breaker, os sinais de observabilidade e o stub HTTP local reutilizavel.
  A cobertura atual ja protege HTTP 200 sem corpo, id invalido e uma validacao em cascata, mas
  ainda nao existe contrato MTR dedicado da capacidade. Nenhum arquivo de producao ou teste foi
  alterado; OpenAPI, Docker e Dev Services nao foram usados.
- **Evidencia HTTP da validacao negocial:** `DossieProdutoApiContractTest` e
  `DossieProdutoErroApiContractTest` agora protegem HTTP 200 sem corpo, corpo ausente, todas as dez
  mensagens obrigatorias aninhadas, listas de topo nulas e elementos, objetos e campos nulos
  aceitos. As duas classes focadas passaram em 2026-07-14; nenhuma classe de producao mudou e o
  OpenAPI nao foi testado ou manipulado.
- **Evidencia MTR da validacao negocial:** `ValidacaoNegocialDossieProdutoMtrContractTest` percorre
  o Resource legado ate o stub localhost com simulador desabilitado e protege path PATCH v1, wire
  completo, nulos e omissao `NON_NULL`, headers, OIDC, `traceparent`, resposta 200 vazia, erro 400
  lossless sem retry, retry de 500 com wire identico e a matriz FT declarada. Os cinco cenarios
  focados passaram em 2026-07-14 sem rede externa, Docker ou Dev Services.
- **Evidencia final da caracterizacao da validacao negocial:**
  `baseline-validacao-negocial-dossie-produto.md` registra HTTP/JSON/validacao/nulabilidade, MTR
  v1, simulador, configuracao, erros, observabilidade e matriz FT. Os testes focados de selecao,
  fixture, spans e logs passaram; `mvn -q clean test` executou 224 casos em 56 relatorios, com zero
  falhas, zero erros e zero ignorados, no profile `test`, sem Docker ou Dev Services. Nenhuma classe
  de producao mudou e a Task 2.4b nao foi iniciada.
- **Inicio da Task 2.4b (2026-07-14):** a arvore interna exige dez arquivos de producao; a task foi
  subdividida antes da primeira edicao de producao para manter cada incremento em ate cinco
  arquivos nao mecanicos. As seis alteracoes pendentes da Task 2.4a foram preservadas.
- **Evidencia dos tipos folha da validacao negocial:** parecer de apontamento, cliente avalista e
  produto foram criados como records Java puros, sem annotations ou imports de framework. Os 13
  testes de `ArchUnitProgressivoTest` passaram.
- **Evidencia dos tipos compostos da validacao negocial:** garantia, verificacao, resposta de
  formulario e comando foram criados sem copia, normalizacao ou nova invariante. Listas, elementos
  e campos nulos continuam representaveis; os 13 testes de `ArchUnitProgressivoTest` passaram.
- **Evidencia das portas e caso de uso da validacao negocial:** o teste unitario registrou RED de
  compilacao pela ausencia da porta de saida e passou a GREEN apos as duas portas e o caso de uso
  minimo. O delegador preserva a mesma instancia do comando, listas, elementos nulos, resultado
  `Void` e a mesma falha da porta de saida.
- **Evidencia final do nucleo da validacao negocial:** sete tipos internos, comando, portas e caso
  de uso permanecem sem DTOs ou frameworks, salvo `Uni` na aplicacao. Os dois testes unitarios e
  os 13 guardrails ArchUnit passaram; `mvn -q clean test` executou 226 casos em 57 relatorios, com
  zero falhas, zero erros e zero ignorados, sem Docker ou Dev Services. Nenhuma borda foi ligada.
- **Inicio da Task 2.4c (2026-07-14):** a borda MTR exige sete arquivos de producao e foi
  subdividida antes da primeira edicao de producao. O primeiro incremento cria
  `ValidacaoNegocialDossieProdutoMtrRequest`, `ValidacaoNegocialDossieProdutoMtrException` e
  `ValidacaoNegocialDossieProdutoMtrClient`; o segundo cria
  `FalhaRegistroValidacaoNegocialDossieProduto`, `ValidacaoNegocialDossieProdutoMtrMapper`,
  `ValidacaoNegocialMtr` e `ValidacaoNegocialDossieProdutoMtrAdapter`; o terceiro adiciona os
  testes unitarios, amplia o contrato contra stub local e executa ArchUnit/suite. As alteracoes
  locais das Tasks 2.4a e 2.4b serao preservadas.
- **Evidencia do contrato e client MTR da validacao negocial:** request v1 com records aninhados,
  excecao de protocolo e REST Client exclusivos preservam `PATCH`, path, omissao `NON_NULL`,
  resposta vazia, providers de headers/OIDC/trace e a matriz de timeout/retry/circuit breaker.
  O primeiro RED falhou pela ausencia dos tipos e os tres testes do client passaram a GREEN.
- **Evidencia do adapter MTR da validacao negocial:** falha interna Java pura, mapper, qualifier e
  adapter implementam a porta de saida sem DTO REST ou tipo legado. O segundo RED falhou pela
  ausencia do mapper/falha/adapter; os tres testes passaram a GREEN preservando a arvore completa,
  listas e elementos nulos, erro lossless e timeout. A traducao ocorre depois da politica FT.
- **Evidencia final da borda MTR da validacao negocial:** o novo client e adapter percorreram o
  stub localhost com wire completo e nulos, API key sintetica, OIDC local, `traceparent`, erro 400
  sem retry e retry 500 com wire identico. A declaracao do span CLIENT e os mesmos nomes de
  atributos/eventos do legado foram preservados; os 13 guardrails ArchUnit passaram.
  `mvn -q clean test` executou 236 casos em 59 relatorios, com zero falhas, zero erros e zero
  ignorados, no profile `test`, sem Docker ou Dev Services. Nenhum producer, simulador, caso de
  uso ou Resource foi ligado.
- **Inicio da Task 2.4d (2026-07-14):** a borda simulador cabe em cinco arquivos de producao, sem
  subdivisao adicional: `ValidacaoNegocialDossieProdutoSimuladorResponse`,
  `ValidacaoNegocialDossieProdutoSimuladorMapper`, `ValidacaoNegocialSimulador`,
  `ValidacaoNegocialDossieProdutoSimuladorAdapter` e
  `ValidacaoNegocialDossieProdutoPortasProducer`. O incremento preservara a fixture `{}`, o
  retorno `Void`, a property existente e a observabilidade contratual; caso de uso, Resource e
  Task 2.4e permanecerao desligados.
- **Evidencia final da borda simulador da validacao negocial:** DTO e mapper exclusivos leem a
  fixture real `{}` sem reutilizar DTO REST ou MTR e preservam o retorno `Void`. Qualifier, adapter
  e producer selecionam simulador com a property ligada e MTR com ela desligada, sem ambiguidade
  CDI; evento, campos de log e `simtr_hub.origem_dados=mock` permanecem equivalentes. O RED falhou
  somente pelos cinco componentes ausentes; 14 testes focados, contratos de spans/logs e 13
  guardrails passaram. `mvn -q clean test` executou 241 casos em 62 relatorios, com zero falhas,
  zero erros e zero ignorados, no profile `test`, sem Docker ou Dev Services. Properties, fixture,
  caso de uso e Resource nao foram alterados; a Task 2.4e nao foi iniciada.
- **Inicio da Task 2.4e (2026-07-14):** a migracao foi subdividida antes da primeira edicao de
  producao. A 2.4e.1 cria `ValidacaoNegocialDossieProdutoRequest`,
  `ValidacaoNegocialDossieProdutoRestMapper` e
  `ValidacaoNegocialDossieProdutoObservabilidade`; a 2.4e.2 altera somente
  `DossieProdutoResource` para usar a porta de entrada e comprova equivalencia HTTP, MTR,
  simulador, erro e telemetria; a 2.4e.3 remove, somente depois de cobertura verde e `rg` sem
  consumidores, a cadeia exclusiva formada por `DossieProdutoFachada`, `DossieProdutoService`,
  `DossieProdutoGateway`, `DossieProdutoClient`, `DossieProdutoClientExceptionMapper`,
  `DossieProdutoMockFactory`, `DossieProdutoMapper`, os sete
  `DossieProdutoValidacaoNegocial*Vo` e o DTO de topo legado
  `DossieProdutoValidacaoNegocialDto`. Os seis DTOs REST aninhados, contratos externos,
  properties, fixture e matriz FT permanecem.
- **Evidencia final da borda REST da validacao negocial:** request publico com `List<@Valid T>`,
  mapper REST completo e fronteira de observabilidade ligam o Resource a porta de entrada sem
  alterar path, JSON, mensagens, nulabilidade, resposta, erro, simulador, FT ou telemetria. A
  matriz HTTP/MTR/simulador/logs/spans e os 13 guardrails passou antes da remocao. `rg` confirmou
  exclusividade e a cadeia legada, o DTO de topo e os sete VOs foram removidos; os seis DTOs REST
  aninhados permanecem. O RED de compilacao e cinco testes novos substituem dez testes exclusivos
  do legado removido. `mvn -q clean test` executou 236 casos em 62 relatorios, com zero falhas,
  zero erros e zero ignorados, no profile `test`, sem Docker ou Dev Services. O warning `HV000271`
  nao aparece mais para a capacidade; warnings restantes pertencem a formulario/documento. Nao
  houve alteracao de OpenAPI, properties ou fixture.
- **Checkpoint C2.4:** GO humano confirmado em 2026-07-14; consolidacao de `dossieproduto`
  desbloqueada.
- **Inicio da Task 2.5 (2026-07-14):** inventario global cruzou todas as classes de producao de
  `dossieproduto` com referencias em producao e testes. Os unicos artefatos legados sem
  consumidores sao `DossieProdutoClienteVo`, `DossieProdutoClienteRelacionadoVo` e
  `DossieProdutoClienteAvalistaVo`; eles serao removidos em um unico incremento. Beans com uma
  unica referencia textual, mas selecionados pelo CDI (`Observabilidade`, `PortasProducer` e
  adapters de simulador), foram identificados como falsos positivos e permanecerao. A verificacao
  sera feita com `rg`, ArchUnit e `mvn -q clean test`.
- **Evidencia final da consolidacao de `dossieproduto`:** os tres VOs orfaos foram removidos sem
  ajuste de consumidor. A auditoria posterior confirmou zero referencias, zero arquivos nos
  packages raiz legados `fachada`, `servico`, `mapeamento` e `integracao`, e zero VOs no package
  raiz `dominio`. Os sete falsos positivos restantes sao beans `@ApplicationScoped`, producers ou
  adapters qualificados usados pelo CDI e foram preservados. Os 13 casos de
  `ArchUnitProgressivoTest` passaram; `mvn -q clean test` executou 236 testes em 62 relatorios,
  com zero falhas, zero erros e zero ignorados, no profile `test`, sem Docker ou Dev Services.
  Nenhum endpoint, JSON, validacao, erro, simulador, FT, observabilidade, property, fixture,
  OpenAPI ou dependencia foi alterado.
- **Checkpoint C2:** GO humano confirmado em 2026-07-14; Fase 3 desbloqueada.
- **Task 3.1 concluida:** contratos da consulta legada registrados em
  `tasks/baseline-consulta-processo-parametrizado.md`; testes focados e suite completa verdes,
  com evidencia de cobertura em `target/jacoco-report`; producao permaneceu inalterada.
- **Task 3.2 concluida:** nucleo semantico, portas, caso de uso e guardrails criados sob
  `arvoredocumento`, sem ligar bordas ou alterar contratos observaveis.
- **Task 3.3 concluida:** borda MTR exclusiva criada sob `arvoredocumento`, preservando contratos,
  erros, fault tolerance e telemetria; verificacoes focadas, ArchUnit e suite completa verdes.
- **Task 3.4 concluida:** DTO/mapper exclusivos, fallback de fixture, qualifiers e producer
  preservam simulador, selecao por property e telemetria; verificacoes focadas, CDI, ArchUnit e
  suite completa verdes.
- **Task 3.5 concluida:** Resource, DTOs REST, mapper de resposta/erro e observabilidade pertencem
  a `arvoredocumento`; path, JSON, validacao, erros e telemetria permanecem equivalentes.
- **Task 3.6a concluida:** inventario da cadeia legada registrado no proprio recorte operacional;
  guardrail explicito impede `arvoredocumento` de depender de outros dominios e possui prova
  controlada de deteccao. ArchUnit focado verde e `rg` sem imports cruzados em producao.
- **Task 3.6b concluida:** consumidores remanescentes foram migrados ou removidos com cobertura
  equivalente; cadeia legada exclusiva de processo removida de `parametrizacao`, que preserva
  integralmente checklist. `rg` sem referencias de codigo e verificacao focada verde.
- **Task 3.6c concluida:** `rg`, ArchUnit, build, suite completa, JaCoCo e revisao do diff verdes;
  README e documentacao consolidada permanecem adiados para o fim do plano.
- **Checkpoint C3:** GO humano confirmado em 2026-07-14; Fase 3 concluida.
- **Bootstrap da Fase 4:** `refactor/ddd-fase-4-baseline` criada no commit `7613fed`, publicada e
  com upstream remoto configurado antes da primeira task de implementacao.
- **Task 4.1 concluida:** `baseline-consulta-checklist.md` e caracterizacao executavel congelam
  HTTP/JSON/validacao, identificador/versao, nulabilidade, wire MTR, resposta sem conteudo, erros,
  retry, FT, simulador, configuracao e observabilidade sem alterar producao. Evidencia numerica
  permanece somente no JaCoCo; OpenAPI segue gerado exclusivamente pelo Quarkus e nao foi testado
  ou manipulado.
- **Task 4.2 concluida:** comando, modelos, portas e caso de uso reativo pertencem a
  `conformidade`; teste unitario e ArchUnit focados verdes; guardrails impedem compartilhamento de
  modelo com outros dominios, dependencia de bordas e exposicao da porta de saida. REST, MTR,
  simulador e CDI ainda nao foram ligados.
- **Task 4.3 concluida:** DTO/mapper, cliente, erros, qualifier e adapter MTR pertencem a
  `conformidade`; wire, nulabilidade, erros, ordem FT, headers, OIDC e observabilidade foram
  preservados sem alterar a borda REST publica ou o simulador.
- **Task 4.4 concluida:** DTO/mapper, qualifier, adapter e producer do simulador pertencem a
  `conformidade`; fixture, fallback, property, telemetria e selecao CDI ligada/desligada foram
  preservados sem alterar o caso de uso ou a borda REST publica.
- **Task 4.5 concluida:** DTOs e mapper REST exclusivos, fronteira de observabilidade e Resource
  pertencem a `conformidade`; path, JSON, validacao, nulabilidade, resposta sem conteudo, erros,
  selecao MTR/simulador, FT e telemetria foram preservados. O legado remanescente sera inventariado
  apenas na Task 4.6.
- **Task 4.6 iniciada:** o inventario nao encontrou consumidor de producao da cadeia legada de
  checklist. Testes historicos ainda referenciam client, gateway, service, mapper, mock factory,
  DTOs e VOs antigos; eles serao migrados ou removidos somente quando houver cobertura equivalente.
  A property e a fixture compartilhadas pela implementacao nova permanecem.
- **Task 4.6a concluida:** o isolamento explicito de `conformidade` foi ativado e sua
  eficacia foi provada por fixture arquitetural controlada. Nao ha import de outro dominio
  no codigo de producao de `conformidade`.
- **Task 4.6b.1 concluida:** caracterizacao HTTP/FT, classificacao de erro e contrato de spans
  agora exercitam o client e o adapter ativos de `conformidade`.
- **Task 4.6b.2 concluida:** os testes unitarios duplicados do legado foram removidos somente
  apos a cobertura equivalente da implementacao nova passar. A fixture publica sem consumidores
  tambem foi eliminada, e os testes compartilhados de gestao documental foram preservados.
- **Task 4.6c concluida:** fachada, service, mapper, VOs, DTOs, gateway, client, exception mapper
  e mock factory exclusivos do checklist legado foram removidos apos o inventario ficar sem
  consumidores externos. A property do client e a fixture compartilhada pelo simulador novo
  foram preservadas.
- **Task 4.6d concluida:** ausencia de referencias, isolamento ArchUnit, build limpo, suite
  completa e revisao multi-eixo passaram sem bloqueios. Contratos HTTP/JSON, validacao, wire MTR,
  nulabilidade, erros, simulador, fault tolerance e observabilidade permanecem cobertos; nenhuma
  manipulacao ou teste do OpenAPI gerado pelo Quarkus foi introduzido.
- **Checkpoint C4:** GO humano registrado em 2026-07-14. A consulta de checklist pertence
  integralmente a `conformidade`, e a cadeia legada exclusiva foi removida sem referencias.
- **Bootstrap da Fase 5:** `refactor/ddd-fase-5-baseline` criada no commit `1f414fa`, publicada e
  com upstream remoto configurado antes da primeira task de implementacao.
- **Task 5.1 concluida:** `baseline-obter-credencial-container.md` e caracterizacao executavel
  congelam HTTP/JSON/nulabilidade, wire MTR, validade opaca, erros, fault tolerance, simulador,
  configuracao e observabilidade sem alterar producao. A evidencia de cobertura permanece no
  JaCoCo; OpenAPI segue gerado exclusivamente pelo Quarkus e nao foi testado ou manipulado.
- **Task 5.2 concluida:** modelo, portas e caso de uso reativo pertencem ao novo nucleo de
  `gestaodocumento`; SAS, validade e nulos sao preservados sem regra funcional nova. A aplicacao
  nao importa bordas, e a porta de entrada nao expoe implementacao nem porta de saida.
- **Task 5.2a concluida:** modelo, portas e caso de uso reativo foram criados sem framework no
  dominio, sem DTOs ou adapters na aplicacao e sem interpretar SAS ou validade.
- **Task 5.2b concluida:** os guardrails progressivos agora incluem a aplicacao e a porta de entrada
  de `gestaodocumento`.
- **Task 5.3 concluida:** a borda MTR exigiu sete arquivos de producao e foi subdividida antes da
  primeira edicao. A 5.3a cria DTO, erro protocolar e REST Client; a 5.3b cria falha interna e
  mapper; a 5.3c cria qualifier e adapter com guardrail; a 5.3d consolida o stub real e a
  observabilidade.
- **Task 5.3a concluida:** DTO de resposta, erro protocolar e REST Client MTR exclusivos preservam
  paths, providers, classificacao de status e matriz de fault tolerance do legado.
- **Task 5.3b concluida:** falha interna e mapper MTR preservam classificacao, payload, causa,
  validade textual ou estruturada e nulabilidade sem conversao ou copia.
- **Task 5.3c concluida:** qualifier e adapter MTR implementam a porta de saida, traduzem falhas
  somente apos o client e preservam spans, atributos e eventos legados. O guardrail de adapters
  inclui a nova borda.
- **Task 5.3d concluida:** o stub localhost prova POST sem corpo, JSON e validade opacos, headers,
  OIDC, `traceparent`, erro negocial sem retry, recuperacao de erro de servidor com retry e
  observabilidade da nova borda. A evidencia de cobertura permanece exclusivamente no JaCoCo.
- **Task 5.4 concluida:** `GestaoDocumentoSimuladorResponse`, `GestaoDocumentoSimuladorMapper`,
  `GestaoDocumentoSimulador`, `GestaoDocumentoSimuladorAdapter` e
  `GestaoDocumentoPortasProducer` formam a borda simulador exclusiva. A fixture e as properties
  existentes permanecem inalteradas; SAS, validade e nulos atravessam sem conversao, a ausencia de
  JSON continua falhando sem fallback, e a porta seleciona simulador ou MTR sem ambiguidade CDI.
  Origem `mock` e evento estruturado legado foram preservados. A evidencia de cobertura permanece
  exclusivamente no JaCoCo.
- **Inicio da Task 5.5 (2026-07-14):** a migracao da borda REST foi subdividida antes da primeira
  edicao de producao. A 5.5a cria response e mapper REST exclusivos com teste unitario; a 5.5b
  cria a fronteira de observabilidade, substitui a Resource legada por uma Resource que injeta
  somente `ObterCredencialContainer` e adiciona testes focados; a 5.5c comprova equivalencia HTTP,
  MTR/simulador, erros e telemetria, executa ArchUnit e a suite completa. Fachada, service, mapper,
  DTO, VO, gateway, client e mock factory legados permanecem para o inventario controlado da
  Task 5.6. Nenhum contrato, property, fixture ou OpenAPI sera manipulado.
- **Task 5.5a concluida:** response e mapper REST exclusivos preservam os quatro campos JSON,
  inclusive nulos, e atravessam SAS e validade sem interpretacao ou copia. Falhas internas sao
  traduzidas para as mesmas excecoes publicas com status, mensagens e payload externo lossless.
  O teste unitario registrou RED de compilacao pela ausencia do mapper e passou a GREEN.
- **Refinamento da Task 5.5b:** o primeiro GREEN localizou `ResourceBeanCoverageTest` como
  consumidor de teste da package e da cadeia REST legadas. A 5.5b foi separada em 5.5b.1 para a
  nova observabilidade/Resource e 5.5b.2 para migrar esse consumidor compartilhado para a porta de
  entrada e o response novos, antes de repetir os testes. Nenhum consumidor de producao adicional
  foi encontrado.
- **Task 5.5b concluida:** a fronteira de observabilidade instancia o caso de uso sobre a porta de
  saida selecionada e preserva span, flag do simulador, origem, nome de container e eventos
  estruturados sem registrar SAS ou validade. A nova Resource substituiu a rota legada e injeta
  somente `ObterCredencialContainer`; o teste compartilhado agora usa a mesma porta e o response
  novos. Os testes registraram RED somente pela ausencia das duas classes e passaram a GREEN apos
  o ajuste do consumidor estrutural.
- **Task 5.5 concluida:** a matriz focada comprovou HTTP/JSON/nulabilidade, validade opaca, erro
  lossless, caminho MTR pelo stub, retry, simulador e contratos de spans/logs; ArchUnit e a suite
  completa passaram. A evidencia de cobertura permanece exclusivamente no JaCoCo. Nao houve
  dependencia nova, registro de SAS/validade, alteracao de property/fixture ou teste/manipulacao
  do OpenAPI gerado pelo Quarkus. A cadeia legada restante nao foi removida nesta task.
- **Inicio da Task 5.6 (2026-07-14):** o inventario encontrou nove artefatos de producao legados,
  quatro testes exclusivamente legados e tres consumidores compartilhados ainda acoplados a
  tipos antigos. Como a task ultrapassa cinco arquivos nao mecanicos, ela foi subdividida antes
  da primeira alteracao: a 5.6a prova por guardrails que o nucleo nao aceita Azure Blob SDK,
  cache/renovacao de SAS ou upload; a 5.6b migra os contratos compartilhados de fault tolerance e
  observabilidade para a borda nova; a 5.6c remove testes redundantes e a cadeia legada somente
  depois de `rg` sem consumidores; a 5.6d executa busca, ArchUnit, testes focados, suite completa,
  JaCoCo e revisao do diff. README e documentacao consolidada permanecem adiados para a Fase 6.
- **Task 5.6a concluida:** guardrails do nucleo bloqueiam dependencias de Azure Storage e
  bibliotecas de cache, alem de tipos, metodos e campos associados a cache, renovacao, upload ou
  Blob. Fixtures negativas controladas comprovam que as regras falham diante dessas violacoes, e
  o teste ArchUnit focado passou para o codigo de producao atual.
- **Task 5.6b concluida:** a caracterizacao de fault tolerance referencia o REST Client MTR novo
  e suas falhas protocolares; o contrato global de spans inspeciona
  `GestaoDocumentoMtrAdapter#obter`; e a fixture negativa de aplicacao usa o adapter novo. A
  matriz focada registrou RED na regra negativa ainda limitada a `integracao`, foi corrigida para
  cobrir tambem `adaptador` e passou a GREEN sem alteracao de producao.
- **Task 5.6c concluida:** quatro testes exclusivamente legados e a fixture compartilhada sem
  outros consumidores foram removidos. A busca seguinte encontrou apenas as nove definicoes de
  producao antigas e strings contratuais de telemetria; fachada, service, mapper, VO, DTO,
  gateway, REST Client, exception mapper e mock factory foram entao removidos. Nova busca ficou
  sem imports ou tipos antigos; os nomes legados restantes sao somente valores de telemetria
  preservados e suas assercoes. A matriz focada da capacidade, ArchUnit, spans globais e CDI
  passou apos a remocao.
- **Task 5.6d concluida:** buscas finais confirmaram ausencia de imports e tipos antigos, de
  dependencias Azure/cache e de operacoes de cache, renovacao ou upload no nucleo. ArchUnit,
  matriz focada e suite Maven completa passaram; JaCoCo foi regenerado no artefato local. A
  revisao de correcao, legibilidade, arquitetura, seguranca e desempenho nao encontrou bloqueios.
  Nao houve mudanca de dependencia/configuracao, acesso a SAS/validade em telemetria, nem teste,
  filtro ou manipulacao do OpenAPI gerado pelo Quarkus.
- **Task 5.6 concluida:** o Hub apenas obtem e devolve a credencial opaca; a cadeia legada foi
  removida sem consumidores, e guardrails impedem cache, renovacao, upload e Azure Storage no
  nucleo. A evidencia numerica de cobertura permanece exclusivamente no JaCoCo.
- **Checkpoint C5 (2026-07-15):** GO humano registrado. A Fase 5 foi encerrada com contratos,
  configuracao, wire MTR, simulador, erros, fault tolerance, observabilidade, ArchUnit, suite,
  build, JaCoCo e diff revisados sem bloqueios. O Maven nao foi repetido no fechamento porque
  somente documentacao mudou desde a ultima suite completa verde.
- **Proximo item futuro:** 6.1, deliberadamente nao iniciado. A Fase 6 foi adiada por decisao
  humana e devera comecar em `refactor/ddd-fase-6-baseline`, criada a partir do checkpoint
  publicado da Fase 5, somente apos nova instrucao.

## Fase 0 - Baseline e guardrails

- [x] 0.1 Congelar contratos HTTP dos oito endpoints.
- [x] 0.1a Auditar e tornar independentes os oraculos HTTP dos 16 testes existentes.
- [x] 0.1a.1 Congelar processo, checklist e credencial sem ler mocks de runtime no oraculo.
- [x] 0.1a.2 Substituir serializacao de DTOs por requests JSON literais nos testes de dossie.
- [x] 0.1b Fortalecer erros completos e validacoes de maior risco, incluindo cascata e JSON invalido.
- [x] 0.1c Caracterizar semanticamente o OpenAPI completo e o conjunto exato de oito operacoes.
- [x] 0.1d Criar manifesto por capacidade, executar testes focados/suite e fechar somente 0.1.
- [x] 0.2 Inventariar e proteger observabilidade contratual.
- [x] 0.2a Configurar exporter de spans em memoria somente no teste e provar a captura.
- [x] 0.2b Proteger nomes, kinds e atributos de spans das oito capacidades e bordas MTR.
- [x] 0.2c Proteger eventos e campos estruturados de log considerados contratuais.
- [x] 0.2d Criar inventario, executar testes focados/suite e fechar somente 0.2.
- [x] 0.3 Criar stub MTR local com simulador desabilitado.
- [x] 0.4 Adicionar ArchUnit progressivo.
- [x] C0 Executar suite/build e obter GO humano.

## Fase 1 - Piloto workflow de dossie

- [x] 1.1 Caracterizar REST, MTR, simulador, erros e fault tolerance.
- [x] 1.2 Criar tipos internos, portas e caso de uso.
- [x] 1.3 Criar adapter MTR e traducao de erros.
- [x] 1.4 Criar adapter simulador e selecao CDI explicita.
- [x] 1.5 Migrar Resource para a porta de entrada.
- [x] C1 Executar suite/build/ArchUnit, revisar diff e obter GO humano.

## Fase 2 - `dossieproduto`

- [x] 2.1a Caracterizar criacao.
- [x] 2.1b.1 Criar comando, tipos aninhados e resultado internos da criacao.
- [x] 2.1b.2 Criar portas e caso de uso da criacao com fake da porta de saida.
- [x] 2.1b Criar nucleo da criacao.
- [x] 2.1c.1 Criar DTOs, erro e REST Client MTR exclusivos da criacao.
- [x] 2.1c.2 Criar mapper e adapter MTR com traducao lossless.
- [x] 2.1c Criar borda MTR da criacao.
- [x] 2.1d Criar borda simulador da criacao.
- [x] 2.1e Migrar borda REST da criacao.
- [x] C2.1 Registrar evidencias e obter GO humano.
- [x] 2.2a Caracterizar formulario.
- [x] 2.2b.1 Criar tipos internos de cliente, produto, garantia, avalista e resposta.
- [x] 2.2b.2 Criar comando, item, vinculo e resultado internos do formulario.
- [x] 2.2b.3 Criar portas e caso de uso do formulario com fake da porta de saida.
- [x] 2.2b Criar nucleo do formulario.
- [x] 2.2c.1 Criar DTOs, erro e REST Client MTR exclusivos do formulario.
- [x] 2.2c.2 Criar falha interna, mapper, qualifier e adapter MTR do formulario.
- [x] 2.2c.3 Provar wire, nulabilidade, erros e matriz FT no novo client/adapter.
- [x] 2.2c Criar borda MTR do formulario.
- [x] 2.2d Criar borda simulador do formulario.
- [x] 2.2e.1 Criar mapper REST, observabilidade e ligar o Resource a porta de entrada.
- [x] 2.2e.2 Remover a cadeia legada exclusiva do formulario apos equivalencia verde.
- [x] 2.2e.3 Provar HTTP, erros, observabilidade, ArchUnit e ausencia de referencias legadas.
- [x] 2.2e Migrar borda REST do formulario.
- [x] C2.2 Registrar evidencias e obter GO humano.
- [x] 2.3a.1 Congelar contrato HTTP, validacao e nulabilidade da inclusao de documento.
- [x] 2.3a.2 Congelar wire MTR v2, erros e matriz FT contra stub HTTP local.
- [x] 2.3a.3 Registrar simulador, observabilidade, configuracao e evidencias da suite.
- [x] 2.3a Caracterizar inclusao de documento.
- [x] 2.3b.1 Criar comando, tipos aninhados e resultado internos da inclusao de documento.
- [x] 2.3b.2 Criar portas e caso de uso da inclusao de documento com fake da porta de saida.
- [x] 2.3b.3 Provar isolamento do nucleo com teste unitario e ArchUnit.
- [x] 2.3b Criar nucleo da inclusao de documento.
- [x] 2.3c.1 Criar DTOs v2, erro e REST Client MTR exclusivos da inclusao de documento.
- [x] 2.3c.2 Criar falha interna, mapper, qualifier e adapter MTR do documento.
- [x] 2.3c.3 Provar wire, nulabilidade, erros, observabilidade e matriz FT no novo client/adapter.
- [x] 2.3c Criar borda MTR v2 da inclusao de documento.
- [x] 2.3d Criar borda simulador da inclusao de documento.
- [x] 2.3e.1 Criar request/response explicitos, mapper REST e observabilidade do caso de uso.
- [x] 2.3e.2 Ligar o Resource a porta de entrada e comprovar equivalencia HTTP, de erros, MTR e simulador.
- [x] 2.3e.3 Remover a cadeia legada exclusiva da inclusao de documento e provar ausencia de referencias.
- [x] 2.3e Migrar borda REST da inclusao de documento.
- [x] C2.3 Registrar evidencias e obter GO humano.
- [x] 2.4a.1 Congelar contrato HTTP, validacao e nulabilidade da validacao negocial.
- [x] 2.4a.2 Congelar wire MTR, erros e matriz FT contra stub HTTP local.
- [x] 2.4a.3 Registrar simulador, observabilidade, configuracao e evidencias da suite.
- [x] 2.4a Caracterizar validacao negocial.
- [x] 2.4b.1 Criar tipos folha de parecer, cliente avalista e produto da validacao negocial.
- [x] 2.4b.2 Criar garantia, verificacao, resposta de formulario e comando internos.
- [x] 2.4b.3 Criar portas e caso de uso da validacao negocial com fake da porta de saida.
- [x] 2.4b.4 Provar nulabilidade, falha e isolamento com JUnit, ArchUnit e suite.
- [x] 2.4b Criar nucleo da validacao negocial.
- [x] 2.4c.1 Criar request, erro de protocolo e REST Client MTR v1 exclusivos da validacao negocial.
- [x] 2.4c.2 Criar falha interna, mapper, qualifier e adapter MTR da validacao negocial.
- [x] 2.4c.3 Provar wire, nulabilidade, erros, observabilidade e matriz FT no novo client/adapter.
- [x] 2.4c Criar borda MTR da validacao negocial.
- [x] 2.4d Criar borda simulador da validacao negocial.
- [x] 2.4e.1 Criar request/mapper REST e observabilidade da validacao negocial.
- [x] 2.4e.2 Ligar o Resource e comprovar equivalencia HTTP, MTR, simulador, erros e telemetria.
- [x] 2.4e.3 Remover a cadeia legada exclusiva e provar ausencia de referencias.
- [x] 2.4e Migrar borda REST da validacao negocial.
- [x] C2.4 Registrar evidencias e obter GO humano.
- [x] 2.5 Remover artefatos legados sem referencias.
- [x] C2 Executar suite/build/ArchUnit, revisar diff e obter GO humano.

## Fase 3 - `arvoredocumento`

- [x] 3.1 Caracterizar consulta de processo.
- [x] 3.2 Criar nucleo da consulta de processo.
- [x] 3.2a Criar identificador, referencia de checklist, opcao e tipo documental do agregado de leitura.
- [x] 3.2b Criar funcao documental, documento e campo de formulario do agregado de leitura.
- [x] 3.2c Criar macroprocesso, garantia e produto do agregado de leitura.
- [x] 3.2d Criar relacionamento, fase e raiz do processo parametrizado.
- [x] 3.2e Criar portas, caso de uso e teste unitario da consulta de processo.
- [x] 3.2f Ativar o guardrail ArchUnit da nova fatia e concluir a verificacao do nucleo.
- [x] 3.3 Criar borda MTR de processo.
- [x] 3.3a Criar response, deserializer tolerante, erro de protocolo e REST Client MTR com teste.
- [x] 3.3b Criar falha interna, mapper e adapter MTR com teste unitario.
- [x] 3.3c Provar wire, erros, configuracao, observabilidade e matriz FT no stub local e ArchUnit.
- [x] 3.4 Criar borda simulador de processo.
- [x] 3.4a Criar DTO, deserializer e mapper exclusivos da fixture de processo com teste.
- [x] 3.4b Criar qualifiers, adapter simulador e producer de selecao com teste unitario.
- [x] 3.4c Provar fixture, fallback, selecao CDI, observabilidade e ArchUnit.
- [x] 3.5 Migrar borda REST de processo.
- [x] 3.5a Criar DTOs REST exclusivos e mapper de resposta/erro com testes.
- [x] 3.5b Migrar observabilidade e Resource para a porta de entrada de `arvoredocumento`.
- [x] 3.5c Provar HTTP, MTR/simulador, erros, telemetria, ArchUnit e suite completa.
- [x] 3.6 Ativar guardrails e remover legado sem uso.
- [x] 3.6a Inventariar legado e ativar isolamento arquitetural de `arvoredocumento`.
- [x] 3.6b Remover a cadeia legada exclusiva de processo, preservando checklist.
- [x] 3.6c Provar ausencia de referencias, ArchUnit, build, suite e revisar diff.
- [x] C3 Executar suite/build/ArchUnit, revisar diff e obter GO humano.

## Fase 4 - `conformidade`

- [x] 4.1 Caracterizar consulta de checklist.
- [x] 4.1a Congelar HTTP, JSON, validacao, identificador/versao e nulabilidade.
- [x] 4.1b Congelar wire MTR, erros e matriz de fault tolerance contra stub localhost.
- [x] 4.1c Congelar simulador, observabilidade e configuracao; fechar manifesto e verificacao.
- [x] 4.2 Criar nucleo da consulta de checklist.
- [x] 4.2a Criar comando e modelos proprios de checklist e apontamento em `conformidade`.
- [x] 4.2b Criar portas de entrada/saida e caso de uso reativo com teste unitario.
- [x] 4.2c Incluir `conformidade` nos guardrails progressivos e executar ArchUnit.
- [x] 4.3 Criar borda MTR de checklist.
- [x] 4.3a Criar DTO MTR v1 e mapper exclusivo com prova de nulabilidade/listas.
- [x] 4.3b Criar erro tecnico e REST Client exclusivos com classificacao e matriz FT.
- [x] 4.3c Criar erro interno, qualifier e adapter MTR com traducao lossless e telemetria.
- [x] 4.3d Provar wire, headers, OIDC, retry e observabilidade contra stub localhost.
- [x] 4.4 Criar borda simulador de checklist.
- [x] 4.4a Criar DTO e mapper exclusivos do simulador com prova de nulabilidade/listas.
- [x] 4.4b Criar qualifier e adapter simulador preservando fixture, fallback e telemetria.
- [x] 4.4c Criar producer e provar selecao CDI com simulador ligado/desligado.
- [x] 4.5 Migrar borda REST de checklist.
- [x] 4.5a Criar DTOs e mapper REST exclusivos preservando JSON, nulabilidade e erros.
- [x] 4.5b Ligar nova Resource a porta de entrada com observabilidade equivalente.
- [x] 4.5c Provar HTTP e equivalencia ponta a ponta e executar a suite.
- [x] 4.6 Ativar guardrails e remover legado sem uso.
- [x] 4.6a Inventariar legado e ativar isolamento arquitetural explicito de `conformidade`.
- [x] 4.6b Migrar consumidores de teste remanescentes para `conformidade`.
- [x] 4.6b.1 Atualizar caracterizacao, erro e contrato de spans para a borda nova.
- [x] 4.6b.2 Remover cenarios unitarios redundantes do legado e a fixture de teste sem uso.
- [x] 4.6c Remover a cadeia legada exclusiva de checklist apos `rg` sem consumidores.
- [x] 4.6d Provar ausencia de referencias, ArchUnit, build, suite e revisar o diff.
- [x] C4 Executar suite/build/ArchUnit, revisar diff e obter GO humano.

## Fase 5 - `gestaodocumento`

- [x] 5.1 Caracterizar obtencao de credencial.
- [x] 5.2 Criar nucleo de credencial.
- [x] 5.2a Criar modelo, portas e caso de uso de obtencao da credencial com teste unitario.
- [x] 5.2b Ativar guardrails do nucleo de `gestaodocumento` e executar ArchUnit.
- [x] 5.3 Criar borda MTR de credencial.
- [x] 5.3a Criar DTO de resposta, erro protocolar e REST Client MTR com teste.
- [x] 5.3b Criar falha interna e mapper MTR preservando validade, nulos e erros.
- [x] 5.3c Criar qualifier e adapter MTR com traducao, telemetria e guardrail.
- [x] 5.3d Provar wire, headers, OIDC, retry e observabilidade contra stub localhost.
- [x] 5.4 Criar borda simulador de credencial.
- [x] 5.5a Criar response e mapper REST exclusivos preservando JSON, validade, nulos e erros.
- [x] 5.5b.1 Criar observabilidade e substituir a Resource legada preservando rota e telemetria.
- [x] 5.5b.2 Migrar o teste compartilhado da Resource para a porta de entrada e o response novos.
- [x] 5.5b Ligar nova Resource somente a porta de entrada com observabilidade equivalente.
- [x] 5.5c Provar HTTP e equivalencia ponta a ponta e executar os gates da task.
- [x] 5.5 Migrar borda REST de credencial.
- [x] 5.6a Impedir Azure Blob SDK, cache/renovacao de SAS e upload no nucleo por guardrails.
- [x] 5.6b Migrar contratos compartilhados de FT e observabilidade para a borda nova.
- [x] 5.6c Remover testes redundantes e cadeia legada apos `rg` sem consumidores.
- [x] 5.6d Provar ausencia de referencias, ArchUnit, build, suite e revisar o diff.
- [x] 5.6 Provar ausencia de cache, renovacao e upload.
- [x] C5 Executar suite/build/ArchUnit, revisar diff e obter GO humano.

## Fase 6 - Consolidacao

- [ ] 6.1 Remover package legado `parametrizacao` sem referencias.
- [ ] 6.2 Endurecer ArchUnit para todo o codigo migrado.
- [ ] 6.3 Executar verificacao completa de equivalencia.
- [ ] 6.4 Atualizar documentacao e dividas adiadas.
- [ ] C6 Obter aceite humano final.

## Registro de checkpoints

Esta tabela e a fonte autoritativa. Valores validos de status: `PENDENTE`, `GO` e `NO-GO`. Um
checkbox de checkpoint acima so pode ser marcado depois que a linha correspondente tiver `GO`,
data, evidencias verificaveis e aprovador humano.

| Checkpoint | Status | Data | Evidencias | Aprovador |
|---|---|---|---|---|
| C0 | GO | 2026-07-12 | G1: branch confirmada; baseline 100/0; Task 0.1: manifesto HTTP/OpenAPI e 22 focados; Task 0.2: inventario de observabilidade e 12 focados; Task 0.3: baseline MTR com cinco focados; Task 0.4: guardrails ArchUnit progressivos; `mvn -q test` executado novamente com codigo 0 e suite/build verdes | Usuario, GO registrado em conversa |
| C1 | GO | 2026-07-12 | Tasks 1.1-1.5 concluidas; `mvn -q clean test` com 153 elementos `testcase`, zero falhas, zero erros e zero ignorados; oito operacoes publicas e 43 schemas OpenAPI preservados; FT, wire, OIDC de teste, simulador, observabilidade e ArchUnit verdes; legado controlado do workflow removido sem referencias; `diff --check` limpo | Usuario, GO registrado em conversa |
| C2.1 | GO | 2026-07-13 | Tasks 2.1a-2.1e concluidas; `mvn -q clean test` com 163 elementos `testcase`, zero falhas, zero erros e zero ignorados; contratos HTTP/JSON/validacao, wire MTR, simulador, erros lossless, matriz FT, observabilidade e ArchUnit verdes; cadeia legada da criacao removida sem referencias; `diff --check` limpo | Usuario, GO registrado em conversa |
| C2.2 | GO | 2026-07-13 | Tasks 2.2a-2.2e concluidas; `mvn -q clean test` com 191 elementos `testcase` em 45 relatorios, zero falhas, zero erros e zero ignorados; contratos HTTP/JSON/validacao, wire MTR, simulador, erros lossless, matriz FT, observabilidade e ArchUnit verdes; cadeia legada do formulario removida sem referencias; `diff --check` limpo; commit `05571a1` publicado | Usuario, GO registrado em conversa |
| C2.3 | GO | 2026-07-13 | Tasks 2.3a-2.3e concluidas; `mvn -q clean test` com 215 elementos `testcase` em 55 relatorios, zero falhas, zero erros e zero ignorados; contratos HTTP/JSON/validacao, wire MTR v2, simulador, erros lossless, matriz FT, observabilidade e ArchUnit verdes; cadeia legada da inclusao de documento removida sem referencias; `diff --check` limpo; commit `099dd25` publicado | Usuario, GO registrado em conversa |
| C2.4 | GO | 2026-07-14 | Tasks 2.4a-2.4e concluidas; `mvn -q clean test` com 236 testes em 62 relatorios, zero falhas, zero erros e zero ignorados; contratos HTTP/JSON/validacao, wire MTR v1, simulador, erros lossless, matriz FT, observabilidade e ArchUnit verdes; warning depreciado removido da capacidade; cadeia legada da validacao negocial removida sem referencias; `diff --check` limpo | Usuario, GO registrado em conversa |
| C2 | GO | 2026-07-14 | Fase 2 consolidada; suite, build e ArchUnit verdes; diff revisado sem bloqueios | Usuario, GO registrado em conversa |
| C3 | GO | 2026-07-14 | Fase 3 concluida; consulta de processo integralmente em `arvoredocumento`; legado removido sem referencias; suite, build, ArchUnit, JaCoCo e diff verdes | Usuario, GO registrado em conversa |
| C4 | GO | 2026-07-14 | Fase 4 concluida; consulta de checklist integralmente em `conformidade`; legado removido sem referencias; contratos, configuracao, wire MTR, simulador, erros, fault tolerance, observabilidade, suite, build, ArchUnit e diff revisados sem bloqueios | Usuario, GO registrado em conversa |
| C5 | GO | 2026-07-15 | Fase 5 concluida; `gestaodocumento` integralmente migrado; cadeia legada removida sem consumidores; guardrails impedem Azure Storage, cache, renovacao e upload no nucleo; contratos, configuracao, wire MTR, simulador, erros, FT, observabilidade, suite, build, ArchUnit, JaCoCo e diff verdes | Usuario, GO registrado em conversa |
| C6 | PENDENTE | - | - | - |

## Bloqueios que nao podem ser resolvidos por suposicao

- [ ] Idempotencia das operacoes mutaveis do MTR antes de workflows futuros.
- [ ] Inclusao de qualquer endpoint ainda nao implementado.
- [ ] Escolha de engine, persistencia ou desenho de workflow.
- [ ] Mudanca de contrato publico, observabilidade ou comportamento do simulador.
