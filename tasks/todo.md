# Checklist de execucao - refatoracao DDD

## Regra de retomada

Antes de executar qualquer item:

- [ ] ler `../doc/arquitetura-ddd-integracoes-atomicas.md`;
- [ ] confirmar `refactor/ddd-fase-0-baseline` com `git rev-parse --abbrev-ref HEAD`;
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

## Ponto de retomada

- **Ultima tarefa concluida:** 2.1e - borda REST da criacao.
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
- **Proximo item pendente:** C2.1 - revisar evidencias e obter GO humano.

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
- [ ] C2.1 Registrar evidencias e obter GO humano.
- [ ] 2.2a Caracterizar formulario.
- [ ] 2.2b Criar nucleo do formulario.
- [ ] 2.2c Criar borda MTR do formulario.
- [ ] 2.2d Criar borda simulador do formulario.
- [ ] 2.2e Migrar borda REST do formulario.
- [ ] C2.2 Registrar evidencias e obter GO humano.
- [ ] 2.3a Caracterizar inclusao de documento.
- [ ] 2.3b Criar nucleo da inclusao de documento.
- [ ] 2.3c Criar borda MTR v2 da inclusao de documento.
- [ ] 2.3d Criar borda simulador da inclusao de documento.
- [ ] 2.3e Migrar borda REST da inclusao de documento.
- [ ] C2.3 Registrar evidencias e obter GO humano.
- [ ] 2.4a Caracterizar validacao negocial.
- [ ] 2.4b Criar nucleo da validacao negocial.
- [ ] 2.4c Criar borda MTR da validacao negocial.
- [ ] 2.4d Criar borda simulador da validacao negocial.
- [ ] 2.4e Migrar borda REST da validacao negocial.
- [ ] C2.4 Registrar evidencias e obter GO humano.
- [ ] 2.5 Remover artefatos legados sem referencias.
- [ ] C2 Executar suite/build/ArchUnit, revisar diff e obter GO humano.

## Fase 3 - `arvoredocumento`

- [ ] 3.1 Caracterizar consulta de processo.
- [ ] 3.2 Criar nucleo da consulta de processo.
- [ ] 3.3 Criar borda MTR de processo.
- [ ] 3.4 Criar borda simulador de processo.
- [ ] 3.5 Migrar borda REST de processo.
- [ ] 3.6 Ativar guardrails e remover legado sem uso.
- [ ] C3 Executar suite/build/ArchUnit, revisar diff e obter GO humano.

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
| C2.1 | PENDENTE | - | - | - |
| C2.2 | PENDENTE | - | - | - |
| C2.3 | PENDENTE | - | - | - |
| C2.4 | PENDENTE | - | - | - |
| C2 | PENDENTE | - | - | - |
| C3 | PENDENTE | - | - | - |
| C4 | PENDENTE | - | - | - |
| C5 | PENDENTE | - | - | - |
| C6 | PENDENTE | - | - | - |

## Bloqueios que nao podem ser resolvidos por suposicao

- [ ] Idempotencia das operacoes mutaveis do MTR antes de workflows futuros.
- [ ] Inclusao de qualquer endpoint ainda nao implementado.
- [ ] Escolha de engine, persistencia ou desenho de workflow.
- [ ] Mudanca de contrato publico, observabilidade ou comportamento do simulador.
