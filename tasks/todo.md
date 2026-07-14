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

## Ponto de retomada

- **Ultima tarefa concluida:** 3.5 - migrar borda REST de processo.
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
- **Proximo item pendente:** criar `refactor/ddd-fase-4-baseline` antes de iniciar a Task 4.1.

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

- [ ] 4.1 Caracterizar consulta de checklist.
- [ ] 4.2 Criar nucleo da consulta de checklist.
- [ ] 4.3 Criar borda MTR de checklist.
- [ ] 4.4 Criar borda simulador de checklist.
- [ ] 4.5 Migrar borda REST de checklist.
- [ ] 4.6 Ativar guardrails e remover legado sem uso.
- [ ] C4 Executar suite/build/ArchUnit, revisar diff e obter GO humano.

## Fase 5 - `gestaodocumento`

- [ ] 5.1 Caracterizar obtencao de credencial.
- [ ] 5.2 Criar nucleo de credencial.
- [ ] 5.3 Criar borda MTR de credencial.
- [ ] 5.4 Criar borda simulador de credencial.
- [ ] 5.5 Migrar borda REST de credencial.
- [ ] 5.6 Provar ausencia de cache, renovacao e upload.
- [ ] C5 Executar suite/build/ArchUnit, revisar diff e obter GO humano.

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
| C4 | PENDENTE | - | - | - |
| C5 | PENDENTE | - | - | - |
| C6 | PENDENTE | - | - | - |

## Bloqueios que nao podem ser resolvidos por suposicao

- [ ] Idempotencia das operacoes mutaveis do MTR antes de workflows futuros.
- [ ] Inclusao de qualquer endpoint ainda nao implementado.
- [ ] Escolha de engine, persistencia ou desenho de workflow.
- [ ] Mudanca de contrato publico, observabilidade ou comportamento do simulador.
