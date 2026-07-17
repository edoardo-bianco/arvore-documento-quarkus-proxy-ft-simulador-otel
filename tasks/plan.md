# Plano de implementacao: refatoracao DDD das integracoes atomicas

## Status

- **Planejado:** 2026-07-11
- **Implementacao:** Fases 0 a 12 concluidas; C0, C1, C2, C3, C4, C5, C6, C7, C8, C9, C10,
  C11 e C12 em GO; C13 documental; C14-SPEC, C14-PLAN e C14-TASKS em GO; primeiro lote HIGH autorizado
- **Branch de trabalho atual:** `refactor/ddd-fase-13-baseline`
- **Documento arquitetural:** `../doc/arquitetura-ddd-integracoes-atomicas.md`
- **Checklist operacional:** `todo.md`

Depois da Fase 0, nenhuma etapa pode comecar sem que o checkpoint anterior esteja com `GO` na
tabela autoritativa de `todo.md`.

### Regra de bootstrap da Fase 0

As Tasks 0.1 a 0.4 podem executar em sua ordem de dependencias quando a intencao e este plano
estiverem aprovados pelo usuario, mesmo sem checkpoint anterior. Antes da primeira edicao de
producao, o executor deve registrar em `todo.md` que a Fase 0 foi iniciada, anexar `git diff`/estado
da arvore, confirmar `refactor/ddd-fase-0-baseline` como branch atual e confirmar a baseline
de testes. C0 encerra essa excecao: nenhuma Task da Fase 1 pode iniciar sem C0 em `GO`.

## Objetivo

Migrar as oito capacidades existentes para dominios hexagonais independentes, preservando todos os
contratos e comportamentos observaveis. O trabalho sera feito em fatias verticais pequenas, usando
TDD leve e sem implementar funcionalidades ou workflows novos.

## Pre-condicao Git obrigatoria

Antes da Task 0.1, o trabalho deve estar fora de `main` em uma branch dedicada. A branch inicial
aprovada e criada para esta fase e `refactor/ddd-fase-0-baseline`.

- Nao executar tasks de implementacao diretamente em `main`.
- No inicio de cada retomada, confirmar a branch com `git rev-parse --abbrev-ref HEAD`.
- A partir da Fase 3, cada nova fase deve usar uma branch propria no formato
  `refactor/ddd-fase-<numero>-baseline`.
- A branch da nova fase deve nascer do commit publicado que recebeu `GO` no checkpoint da fase
  anterior, ser publicada e ter upstream remoto configurado antes da primeira task de
  implementacao.
- Nao reutilizar a branch da fase anterior para implementar a fase seguinte.
- Preservar alteracoes do usuario e revisar o diff antes de cada checkpoint.
- Commit e push exigem solicitacao explicita do usuario e nao fazem parte automaticamente de um
  checkpoint.

## Grafo de dependencia

```text
documentacao aceita
    -> baseline e testes de caracterizacao
        -> guardrails ArchUnit e infraestrutura de stub MTR
            -> piloto dossieproduto/workflow
                -> demais capacidades dossieproduto
                    -> arvoredocumento/processo
                    -> conformidade/checklist
                    -> gestaodocumento/credencial
                        -> remocao do legado e verificacao final
```

Depois do piloto, a ordem de execucao permanece fixa: concluir `dossieproduto`, depois
`arvoredocumento`, `conformidade` e `gestaodocumento`. Alterar essa ordem exige atualizar
dependencias, checkpoints e obter GO humano antes da execucao.

## Regras de execucao

1. Antes de mudar producao, adicionar ou fortalecer um teste que caracterize a fatia.
2. Cada task deve manter o projeto compilavel; bridges temporarios sao permitidos se forem removidos
   na mesma fase.
3. Nao alterar simultaneamente contrato publico e contrato MTR sem teste de equivalencia.
4. Executar testes focados depois de cada task e `mvn -q test` apenas quando houver mudanca desde a
   ultima execucao.
5. Todo checkpoint e GO/NO-GO humano antes da fase seguinte.
6. Para toda capacidade, caracterizar tambem OpenAPI, propriedades/profiles, wire MTR e a matriz
   atual de timeout/retry/circuit breaker aplicavel.
7. Um teste preexistente so pode ser removido ou substituido com evidencia de cobertura equivalente
   verde. O usuario autorizou em 2026-07-12 a remocao, sem nova microaprovacao, do legado ja migrado
   e controlado; testes nao podem ser desabilitados, e checkpoints continuam exigindo GO humano.
8. O OpenAPI e gerado exclusivamente pelo Quarkus e nao deve ser testado, filtrado, complementado
   ou manipulado. A decisao humana de 2026-07-13 substitui as referencias anteriores a fingerprint
   ou caracterizacao do documento gerado; o contrato publico continua protegido no codigo Java e
   nos testes HTTP/JSON/validacao dos Resources.

### Estrategia de implementacao e testes

A execucao usa **refatoracao incremental orientada por caracterizacao**:

- testes de caracterizacao do legado podem iniciar verdes quando o valor esperado vem de contrato
  observado e independente; requests e respostas congelados nao podem ser derivados dos mesmos
  DTOs ou mocks de runtime usados pela implementacao;
- RED-GREEN-REFACTOR estrito aplica-se a logica nova, bugs, mapeamentos, invariantes, casos de uso e
  regras arquiteturais;
- mudancas que possam afetar CDI, Jackson, REST Client, OpenAPI, configuracao ou observabilidade nao
  sao classificadas como movimentos mecanicos;
- cada incremento executa testes focados; a suite completa roda sempre que a verificacao da task
  exigir e em todo checkpoint, sem repeticao quando nao houve mudanca;
- o manifesto relaciona cada capacidade aos testes/metodos exatos, contratos protegidos, lacunas
  conhecidas e evidencia OpenAPI, sem substituir protecao executavel.

## Mapa de arquivos por tipo de task

Este mapa vale para todas as fases e evita que uma task atravesse camadas sem declarar o motivo:

| Tipo de task | Arquivos provaveis |
|---|---|
| caracterizacao | `src/test/.../<dominio>`, `src/test/resources`, `tasks/baseline-contratos.md` |
| nucleo | `src/main/.../<dominio>/dominio`, `aplicacao/porta`, `aplicacao/casodeuso` |
| borda MTR | `adaptador/saida/mtr`, testes de stub em `src/test` |
| borda simulador | `adaptador/saida/simulador`, `src/main/resources/mock`, testes focados |
| borda REST | `adaptador/entrada/rest`, testes HTTP/OpenAPI |
| guardrails | `pom.xml`, `src/test/.../arquitetura`, remocoes comprovadas por `rg` |

O executor deve listar os arquivos exatos no inicio da task. Se houver mais de cinco arquivos nao
mecanicos, deve subdividir a task no checklist antes de editar.

## Fase 0 - Baseline e guardrails

### Task 0.1 - Congelar contratos HTTP existentes

**Descricao:** ampliar testes de caracterizacao dos oito endpoints atuais antes de mover classes.

**Criterios de aceite:**

- paths, verbos, status, JSON e validacoes relevantes estao cobertos;
- requests, respostas e erros congelados usam oraculos independentes dos DTOs e mocks de runtime;
- erros publicos atuais possuem exemplos completos de caracterizacao;
- o OpenAPI e comparado semanticamente, sem depender da ordem textual, cobrindo o conjunto exato de
  operacoes e a estrutura contratual completa;
- um manifesto lista por capacidade os testes e contratos protegidos;
- os testes passam contra a implementacao original.

**Verificacao:** testes focados de Resource e `mvn -q test`.

**Dependencias:** nenhuma.

**Escopo estimado:** M por Resource; executar como subtasks separadas se exceder cinco arquivos.

### Task 0.2 - Inventariar observabilidade contratual

**Descricao:** registrar spans, eventos e atributos atuais por capacidade e criar assercoes para os
sinais que nao podem mudar.

**Criterios de aceite:**

- inventario versionado no repositorio;
- atributos derivados de classe/metodo do REST Client estao identificados;
- testes falham quando um sinal protegido e renomeado.

**Verificacao:** testes de observabilidade focados.

**Dependencias:** Task 0.1.

**Escopo estimado:** M.

### Task 0.3 - Criar teste de borda MTR realista

**Descricao:** configurar stub HTTP local com simulador desabilitado para observar request, response,
headers e erros do REST Client sem acessar sistemas externos.

**Criterios de aceite:**

- pelo menos uma operacao de dossie percorre Resource ate o stub;
- o teste valida wire JSON e provider de seguranca/observabilidade sem credenciais reais;
- timeout, retry e circuit breaker podem ser exercitados deterministicamente;
- nenhum teste depende de rede externa.

**Verificacao:** teste de integracao focado e suite completa.

**Dependencias:** Task 0.1.

**Escopo estimado:** M.

### Task 0.4 - Adicionar guardrails ArchUnit progressivos

**Descricao:** incluir ArchUnit e regras inicialmente aplicadas somente a packages ja migrados.

**Criterios de aceite:**

- build executa as regras automaticamente;
- existe um teste negativo controlado ou prova equivalente de que cada regra detecta violacao;
- a API publica de aplicacao e limitada a portas de entrada e seus comandos/resultados;
- apenas adapters anticorrupcao podem importar a API publica de outro dominio;
- o DTO tecnico de erro REST possui package permitido explicito e e proibido nas demais bordas;
- legado nao vira excecao permanente silenciosa.

**Verificacao:** testes ArchUnit e `mvn -q test`.

**Dependencias:** Task 0.1.

**Escopo estimado:** S.

### Checkpoint C0 - GO/NO-GO de protecao

- [ ] Os 100 testes preexistentes continuam presentes e verdes; remocoes/skips exigem evidencia e GO.
- [ ] Manifesto por capacidade registra contratos/testes e o novo total da suite.
- [ ] Contratos REST, MTR, simulador, erros e observabilidade possuem protecao suficiente para o piloto.
- [ ] ArchUnit esta ativo sem exigir migracao big bang.
- [ ] Nenhum arquivo de producao mudou de comportamento.
- [ ] Revisao humana autoriza o piloto.

## Fase 1 - Piloto `IniciarOuAvancarWorkflowDossieProduto`

### Task 1.1 - Caracterizar a operacao de workflow

**Descricao:** cobrir REST, simulador, MTR, erros e fault tolerance da operacao de contrato pequeno.

**Criterios de aceite:**

- status e JSON publicos atuais estao congelados;
- request/response MTR e selecao do simulador estao cobertos;
- OpenAPI e propriedades/profiles estao congelados;
- matriz atual de timeout/retry/circuit breaker e observada sem ser alterada.

**Verificacao:** testes focados da operacao.

**Dependencias:** C0.

**Escopo estimado:** M.

### Task 1.2 - Introduzir contrato interno e caso de uso

**Descricao:** criar tipos semanticos, porta de entrada, porta de saida e caso de uso atomico para
iniciar ou avancar workflow.

**Criterios de aceite:**

- aplicacao nao importa DTOs ou adapters;
- tipos internos nao possuem annotations de framework;
- o caso de uso e testado com fake da porta de saida.

**Verificacao:** teste unitario focado e ArchUnit.

**Dependencias:** Task 1.1.

**Escopo estimado:** M.

### Task 1.3 - Criar adapter MTR da operacao

**Descricao:** separar DTO MTR, mapper, client e adapter mantendo a politica atual de fault tolerance.

**Criterios de aceite:**

- wire contract continua identico;
- erros HTTP sao traduzidos para falha interna lossless;
- nenhum DTO publico aparece no adapter MTR.

**Verificacao:** teste com stub MTR, testes de erro e ArchUnit.

**Dependencias:** Task 1.2.

**Escopo estimado:** M, quebrar client/erro em duas tasks se necessario.

### Task 1.4 - Criar adapter de simulador e selecao CDI

**Descricao:** mover o simulador para a mesma porta de saida, com DTO proprio se ler fixture JSON, e
selecionar adapter por qualifier/producer explicito.

**Criterios de aceite:**

- propriedades atuais habilitam os mesmos caminhos;
- bootstrap CDI nao possui ambiguidade ou recursao;
- caso de uso nao contem condicional de simulador.

**Verificacao:** testes com simulador ligado/desligado e bootstrap Quarkus.

**Dependencias:** Task 1.2.

**Escopo estimado:** M.

### Task 1.5 - Migrar o adapter REST da operacao

**Descricao:** fazer o Resource chamar a porta de entrada e mapear resultado/erro para o contrato
publico atual.

**Criterios de aceite:**

- path, status, JSON, OpenAPI e observabilidade permanecem iguais;
- Resource nao conhece porta de saida, MTR ou simulador;
- bridge legado da operacao e removido.

**Verificacao:** teste HTTP, teste MTR ponta a ponta e suite completa.

**Dependencias:** Tasks 1.3 e 1.4.

**Escopo estimado:** S.

### Checkpoint C1 - GO/NO-GO do padrao

- [ ] A capacidade percorre REST -> aplicacao -> porta -> MTR/simulador.
- [ ] Contratos, erros, fault tolerance e observabilidade permanecem equivalentes.
- [ ] Regras ArchUnit da capacidade estao ativas.
- [ ] Nao ha DTO compartilhado entre bordas.
- [ ] Revisao humana aprova o padrao antes de replica-lo.

## Fase 2 - Demais capacidades de `dossieproduto`

Cada capacidade repete seis tasks pequenas e termina em checkpoint proprio. Uma task que exceder
cinco arquivos deve ser subdividida no `todo.md` antes de editar, preservando os mesmos criterios.

### Subfase 2.1 - `CriarDossieProduto`

#### Task 2.1a - Caracterizar criacao

**Criterios de aceite:** HTTP/JSON/validacao/OpenAPI, wire MTR, simulador, erros, observabilidade,
propriedades/profiles e timeout/retry/circuit breaker atuais estao congelados.

**Verificacao:** testes focados contra a implementacao original.

**Dependencias:** C1. **Escopo:** M.

#### Task 2.1b - Criar nucleo da criacao

**Criterios de aceite:** comando/resultado, porta de entrada, porta de saida e caso de uso nao
importam DTOs/frameworks; teste unitario usa fake da porta.

**Verificacao:** teste unitario e ArchUnit.

**Dependencias:** 2.1a. **Escopo:** M.

#### Task 2.1c - Criar borda MTR da criacao

**Criterios de aceite:** DTO/mapper MTR sao exclusivos da operacao; wire e ordem de fault tolerance
permanecem iguais; erro externo vira falha interna lossless.

**Verificacao:** stub MTR, matriz de falhas e ArchUnit.

**Dependencias:** 2.1b. **Escopo:** M, subdividir DTOs aninhados se exceder cinco arquivos.

#### Task 2.1d - Criar borda simulador da criacao

**Criterios de aceite:** fixture usa DTO/mapper proprio; selecao CDI preserva properties; caso de uso
nao possui condicional.

**Verificacao:** simulador ligado/desligado e bootstrap CDI.

**Dependencias:** 2.1b. **Escopo:** M.

#### Task 2.1e - Migrar borda REST da criacao

**Criterios de aceite:** request/response Java recebem nomes explicitos; path, status, JSON,
validacao, OpenAPI e observabilidade nao mudam; bridge legado e removido.

**Verificacao:** HTTP, equivalencia REST-interno-MTR e suite completa.

**Dependencias:** 2.1c e 2.1d. **Escopo:** M.

**Status:** concluida em 2026-07-13; suite limpa verde e evidencias registradas em
`baseline-criacao-dossie-produto.md`. O OpenAPI permaneceu sob geracao exclusiva do Quarkus, sem
arquivo estatico, filtro ou annotation de ocultacao.

### Checkpoint C2.1 - Criacao

**Status:** `GO` humano registrado em 2026-07-13; formulario desbloqueado.

- [x] Evidencias de todas as bordas e matriz FT anexadas.
- [x] ArchUnit da capacidade ativo; suite/build verdes.
- [x] GO humano registrado antes de formulario.

### Subfase 2.2 - `AtualizarFormularioDossieProduto`

#### Task 2.2a - Caracterizar formulario

**Criterios de aceite:** HTTP/JSON/validacao/OpenAPI, wire MTR, simulador, erros, observabilidade,
configuracao e matriz FT estao congelados, incluindo listas e nulos atuais.

**Verificacao:** testes focados contra o legado.

**Dependencias:** C2.1. **Escopo:** M.

**Status:** concluida em 2026-07-13; contratos e evidencias registrados em
`baseline-formulario-dossie-produto.md`, sem mudanca de producao e sem testar ou manipular o
OpenAPI gerado pelo Quarkus.

#### Task 2.2b - Criar nucleo do formulario

**Criterios de aceite:** tipos semanticos, portas e caso de uso sao framework-free salvo `Uni` na
aplicacao; nenhuma imutabilidade/regra funcional nova e introduzida.

**Verificacao:** teste unitario e ArchUnit.

**Dependencias:** 2.2a. **Escopo:** M.

**Status:** concluida em 2026-07-13; tipos internos, portas e caso de uso criados sem regra
funcional nova. Testes unitarios, ArchUnit e `mvn -q clean test` passaram no profile padrao
`test`, sem Docker ou Dev Services.

#### Task 2.2c - Criar borda MTR do formulario

**Criterios de aceite:** DTO/mapper exclusivos preservam nulabilidade e wire; erro/FT mantem ordem e
classificacao atuais.

**Verificacao:** stub MTR, erros e matriz FT.

**Dependencias:** 2.2b. **Escopo:** M, subdividir tipos aninhados se necessario.

**Status:** concluida em 2026-07-13; DTOs/client/erro/mapper/adapter exclusivos preservam wire,
nulos, headers, traducao lossless e ordem/classificacao FT. Testes unitarios, stub localhost,
ArchUnit e `mvn -q clean test` passaram no profile padrao `test`, sem Docker ou Dev Services.

#### Task 2.2d - Criar borda simulador do formulario

**Criterios de aceite:** fixture e modelos proprios; properties e comportamento atual preservados.

**Verificacao:** simulador ligado/desligado e bootstrap CDI.

**Dependencias:** 2.2b. **Escopo:** S/M.

**Status:** concluida em 2026-07-13; DTO, qualifier, adapter e producer exclusivos preservam a
fixture, a precedencia do identificador informado e a property existente. Testes unitario e CDI
comprovam a selecao ligada/desligada no profile padrao `test`; ArchUnit e `mvn -q clean test`
passaram sem Docker ou Dev Services.

#### Task 2.2e - Migrar borda REST do formulario

**Criterios de aceite:** path/status/JSON/validacao/observabilidade equivalentes; anotacoes REST
publicas preservadas e OpenAPI integralmente gerado pelo Quarkus, sem teste ou manipulacao do
artefato; Resource usa somente porta de entrada.

**Verificacao:** HTTP, equivalencia completa e suite.

**Dependencias:** 2.2c e 2.2d. **Escopo:** M.

**Status:** concluida em 2026-07-13; mapper REST explicito e Resource usam a porta de entrada,
cadeia legada exclusiva removida sem referencias e contratos HTTP/JSON/validacao, erros, FT,
simulador/MTR e observabilidade preservados. O profile padrao `test` desabilita globalmente Dev
Services; `mvn -q clean test` e `git diff --check` passaram. C2.2 recebeu GO humano em
2026-07-13.

### Checkpoint C2.2 - Formulario

**Status:** `GO` humano registrado em 2026-07-13; inclusao de documento desbloqueada.

- [x] Nulos/listas e validacao permanecem equivalentes.
- [x] Todas as bordas, ArchUnit, suite e build verdes.
- [x] GO humano registrado antes de documento.

### Subfase 2.3 - `IncluirDocumentoDossieProduto`

#### Task 2.3a - Caracterizar inclusao de documento

**Criterios de aceite:** contrato publico, OpenAPI, MTR v2, simulador, erros, observabilidade,
configuracao e matriz FT estao congelados, incluindo nulos aceitos.

**Verificacao:** testes focados contra o legado.

**Dependencias:** C2.2. **Escopo:** M.

**Status:** concluida em 2026-07-13; contrato HTTP/JSON/validacao, nulos aceitos, wire MTR v2,
headers/OIDC/trace, resposta, erros lossless, retry, matriz FT, simulador, configuracao e
observabilidade congelados contra o legado. O OpenAPI permanece sob geracao exclusiva do Quarkus,
sem teste ou manipulacao do documento gerado. Testes focados e `mvn -q clean test` passaram sem
Docker ou Dev Services; nenhuma classe de producao foi alterada.

#### Task 2.3b - Criar nucleo da inclusao

**Criterios de aceite:** tipos, portas e caso de uso nao carregam Jackson/Jakarta; ids de documento
e instancia sao resultados internos sem nomes de protocolo.

**Verificacao:** teste unitario e ArchUnit.

**Dependencias:** 2.3a. **Escopo:** M.

**Status:** concluida em 2026-07-13; comando, tipos aninhados, resultado, portas e caso de uso
criados com nomes internos e sem dependencias de protocolo no dominio. Listas, elementos e campos
nulos permanecem sem copia ou normalizacao; o caso de uso delega de forma reativa e preserva a
mesma falha da porta de saida. Teste JUnit puro, ArchUnit e `mvn -q clean test` passaram sem Docker
ou Dev Services. Nenhuma borda foi ligada nesta task.

#### Task 2.3c - Criar borda MTR v2

**Criterios de aceite:** DTO/mapper v2 exclusivos; `id_documento`/`id_instancia_documento`, erro e
matriz FT permanecem equivalentes.

**Verificacao:** stub MTR, erros e FT.

**Dependencias:** 2.3b. **Escopo:** M, subdividir arvore DTO em grupos de ate cinco arquivos.

**Status:** concluida em 2026-07-13; DTOs de request/response v2, excecao de protocolo e REST
Client exclusivos foram criados sem reutilizar DTO REST publico. Mapper, falha interna, qualifier e
adapter implementam a porta de saida preservando path, wire, nulabilidade, os dois ids de resposta,
erros e matriz FT. Testes JUnit puros, stub HTTP local, observabilidade, ArchUnit e
`mvn -q clean test` passaram no profile `test`, sem Docker ou Dev Services. A borda ainda nao foi
selecionada pelo caso de uso nem ligada ao endpoint REST.

#### Task 2.3d - Criar borda simulador da inclusao

**Criterios de aceite:** JSON snake_case e lido por DTO proprio; selecao/configuracao preservadas.

**Verificacao:** fixture real, simulador ligado/desligado e CDI.

**Dependencias:** 2.3b. **Escopo:** M.

**Status:** concluida em 2026-07-13; DTO de resposta e adapter simulador exclusivos leem a
fixture snake_case existente e retornam os dois identificadores internos. Qualifier e producer
selecionam simulador ou MTR pela property existente, sem condicional no caso de uso. Testes JUnit
puros, CDI com a property ligada/desligada, fixture real, ArchUnit e `mvn -q clean test` passaram
no profile `test`, sem Docker ou Dev Services. Nenhuma property foi alterada e o endpoint REST
permanece na cadeia legada ate a Task 2.3e.

#### Task 2.3e - Migrar borda REST da inclusao

**Criterios de aceite:** request/response explicitos; path/status/JSON/validacao/logs iguais;
OpenAPI integralmente gerado pelo Quarkus, sem teste ou manipulacao do artefato; Resource usa a
porta de entrada.

**Verificacao:** HTTP, equivalencia e suite.

**Dependencias:** 2.3c e 2.3d. **Escopo:** M.

**Status:** concluida em 2026-07-13; request/response REST explicitos, mapper lossless e wrapper de
observabilidade ligam o Resource somente a porta de entrada. Testes Java puros e HTTP comprovaram
campos, listas, nulos, validacoes, erros, status, simulador e MTR via stub local. A cadeia legada
exclusiva foi removida de Fachada, Service, Gateway, REST Client, MapStruct e mock factory, junto
dos DTOs/VOs sem uso; `rg` nao encontrou referencias remanescentes. O OpenAPI permaneceu sob
geracao exclusiva do Quarkus, sem teste ou manipulacao. `mvn -q clean test` executou 215 casos em
55 relatorios, com zero falhas, zero erros e zero ignorados, no profile `test`, sem Docker ou Dev
Services; `git diff --check` passou.

### Checkpoint C2.3 - Documento

**Status:** `GO` humano registrado em 2026-07-13; validacao negocial desbloqueada.

- [x] Contratos publico, MTR v2 e simulador equivalentes.
- [x] Todas as bordas, ArchUnit, suite e build verdes.
- [x] GO humano registrado antes de validacao negocial.

### Subfase 2.4 - `RegistrarValidacaoNegocialDossieProduto`

#### Task 2.4a - Caracterizar validacao negocial

**Criterios de aceite:** HTTP 200 sem corpo, JSON/validacao, MTR, simulador, erros,
observabilidade, configuracao e matriz FT estao congelados.

**Status:** iniciada em 2026-07-13 e pausada em checkpoint temporario de sessao. C2.3 foi
registrado como GO e a task foi subdividida no checklist. O inventario parcial confirmou a cadeia
legada, a cobertura HTTP/validacao e observabilidade existente, a property do simulador, a matriz
FT do REST Client e o stub HTTP local reutilizavel; nenhum codigo de producao ou teste foi
alterado. Retomar pela caracterizacao executavel ainda ausente, sem testar ou manipular o OpenAPI.

**Conclusao:** concluida em 2026-07-14; HTTP 200 sem corpo, JSON, dez validacoes obrigatorias,
nulabilidade, wire MTR v1, headers/OIDC/trace, erros lossless, retry, matriz FT, simulador,
configuracao e observabilidade foram congelados contra o legado. Evidencias registradas em
`baseline-validacao-negocial-dossie-produto.md`. `mvn -q clean test` executou 224 casos em 56
relatorios, sem falhas, erros ou ignorados, no profile `test`, sem Docker ou Dev Services; nenhuma
classe de producao foi alterada e a Task 2.4b nao foi iniciada.

**Verificacao:** testes positivos/negativos contra o legado.

**Dependencias:** C2.3. **Escopo:** M.

#### Task 2.4b - Criar nucleo da validacao

**Criterios de aceite:** tipos, portas e caso de uso nao importam DTOs/frameworks; nulabilidade atual
e preservada sem inventar invariantes.

**Verificacao:** teste unitario e ArchUnit.

**Dependencias:** 2.4a. **Escopo:** M.

**Status:** concluida em 2026-07-14; sete tipos internos, comando, portas de entrada/saida e caso
de uso delegador foram criados sem dependencias de protocolo ou nova invariante. O teste unitario
registrou RED de compilacao e passou a GREEN preservando comando, listas, elementos nulos,
resultado `Void` e falha. Os 13 guardrails ArchUnit e `mvn -q clean test` com 226 casos em 57
relatorios passaram sem Docker ou Dev Services. Nenhuma borda foi ligada e a Task 2.4c nao foi
iniciada.

#### Task 2.4c - Criar borda MTR da validacao

**Criterios de aceite:** arvore de DTOs/mapper exclusiva; wire, erros e ordem FT equivalentes.

**Verificacao:** stub MTR, erros e matriz FT.

**Dependencias:** 2.4b. **Escopo:** M, subdividir DTOs aninhados em grupos de ate cinco arquivos.

**Status:** concluida em 2026-07-14; request MTR v1 com tipos aninhados exclusivos, excecao de
protocolo e REST Client preservam path, wire, omissao `NON_NULL`, headers, OIDC, trace e a matriz
FT. Mapper, falha interna, qualifier e adapter implementam a porta de saida com traducao lossless
depois da politica do client e com os mesmos spans, atributos e eventos de log do legado. Dois
ciclos RED/GREEN, testes unitarios, stub localhost, 13 guardrails ArchUnit e `mvn -q clean test`
com 236 casos em 59 relatorios passaram sem Docker ou Dev Services. Nenhuma borda foi selecionada
pelo caso de uso ou ligada ao Resource, e a Task 2.4d nao foi iniciada.

#### Task 2.4d - Criar borda simulador da validacao

**Criterios de aceite:** fixture usa DTO/mapper proprio; properties e retorno atuais preservados.

**Verificacao:** simulador ligado/desligado e CDI.

**Dependencias:** 2.4b. **Escopo:** M.

**Status:** concluida em 2026-07-14; DTO vazio e mapper exclusivos leem a fixture `{}` e convertem
para o resultado interno `Void`. Qualifier, adapter e producer preservam a property existente, o
evento de simulador e o atributo de origem. RED de compilacao, cinco casos novos, selecao CDI nos
dois estados, 13 guardrails e contratos de spans/logs passaram. `mvn -q clean test` executou 241
casos em 62 relatorios, sem falhas, erros ou ignorados, no profile `test`, sem Docker ou Dev
Services. Caso de uso e Resource permanecem desligados da nova borda.

#### Task 2.4e - Migrar borda REST da validacao

**Criterios de aceite:** `List<@Valid T>` remove warning sem alterar mensagens/paths; contrato,
OpenAPI e observabilidade permanecem iguais.

**Verificacao:** HTTP positivo/negativo, equivalencia e suite.

**Dependencias:** 2.4c e 2.4d. **Escopo:** M.

**Status:** concluida em 2026-07-14; request publico com `List<@Valid T>`, mapper REST completo e
fronteira de observabilidade ligam o Resource a porta de entrada sem alterar path, JSON,
validacoes, resposta, erros, simulador, FT ou telemetria. A cadeia legada exclusiva e seus sete
VOs foram removidos somente depois da matriz de equivalencia verde e de busca global sem
consumidores; os seis DTOs REST aninhados foram preservados. `mvn -q clean test` executou 236
casos em 62 relatorios, sem falhas, erros ou ignorados, no profile `test`, sem Docker ou Dev
Services. O warning `HV000271` nao aparece mais para a validacao negocial. C2.4 recebeu GO
humano em 2026-07-14.

### Checkpoint C2.4 - Validacao negocial

- [x] Validacao cascata equivalente e sem uso depreciado na capacidade.
- [x] Todas as bordas, ArchUnit, suite e build verdes.
- [x] GO humano registrado antes da consolidacao.

### Task 2.5 - Consolidar `dossieproduto`

**Descricao:** remover fachada, service, mapper e DTOs legados que nao possuem mais referencias.

**Criterios de aceite:** todas as cinco capacidades usam a arquitetura alvo; propriedades existentes
continuam validas; nenhuma classe morta permanece.

**Verificacao:** `rg` de imports legados, ArchUnit e `mvn -q test`.

**Dependencias:** C2.4.

**Escopo estimado:** M.

**Status:** concluida em 2026-07-14 apos GO humano de C2.4. O inventario global encontrou e removeu
somente `DossieProdutoClienteVo`, `DossieProdutoClienteRelacionadoVo` e
`DossieProdutoClienteAvalistaVo`, sem consumidores. Beans descobertos pelo CDI foram separados dos
falsos positivos textuais e preservados. Nao restam arquivos nos packages raiz legados
`fachada`, `servico`, `mapeamento` ou `integracao`, nem VOs no package raiz `dominio`.
`ArchUnitProgressivoTest` passou com 13 casos e `mvn -q clean test` executou 236 testes em 62
relatorios, sem falhas, erros ou ignorados. Nenhum contrato, property, fixture ou dependencia foi
alterado. C2 recebeu GO humano em 2026-07-14.

### Checkpoint C2 - GO/NO-GO de `dossieproduto`

**Status:** `GO` humano registrado em 2026-07-14; Fase 3 desbloqueada.

- [x] Cinco capacidades atomicas migradas e verdes.
- [x] Nao existe orquestracao ou endpoint novo.
- [x] Retry de operacoes mutaveis continua documentado como risco, nao corrigido silenciosamente.
- [x] Contrato publico e MTR equivalentes ao baseline.
- [x] Revisao humana autoriza migrar os outros dominios.

## Fase 3 - `arvoredocumento`

### Task 3.1 - Caracterizar `ConsultarProcessoParametrizado`

**Criterios de aceite:** HTTP/JSON/validacao/OpenAPI, wire MTR, simulador, erros, nulabilidade,
observabilidade, properties/profiles e matriz FT estao no manifesto.

**Verificacao:** testes focados contra `parametrizacao` legado.

**Dependencias:** C2. **Escopo:** M.

**Status:** concluida em 2026-07-14; HTTP, JSON, validacao, OpenAPI, nulabilidade, wire MTR,
headers/OIDC/trace, erros, retry, matriz FT, simulador, configuracao e observabilidade foram
caracterizados contra o legado. O manifesto esta em `baseline-consulta-processo-parametrizado.md`;
somente testes e documentacao foram alterados. A suite completa e o relatorio JaCoCo ficaram
verdes, sem Docker ou Dev Services; a Task 3.2 nao foi iniciada.

### Task 3.2 - Criar nucleo da consulta de processo

**Criterios de aceite:** modelo semantico, porta de entrada, porta de saida e caso de uso pertencem a
`arvoredocumento` e nao importam `parametrizacao` ou adapters.

**Verificacao:** teste unitario e ArchUnit.

**Dependencias:** 3.1. **Escopo:** M; subdividir a arvore de modelo por agregado de leitura.

**Status:** concluida em 2026-07-14; agregado de leitura semantico, identificador, portas de
entrada/saida e caso de uso delegador foram criados sob `arvoredocumento`, sem imports do legado
`parametrizacao` ou de adapters. Nulabilidade e referencias de listas permanecem sem validacao,
normalizacao ou copia nova. Testes unitarios, ArchUnit e suite completa passaram.

### Task 3.3 - Criar borda MTR de processo

**Criterios de aceite:** DTO/mapper/client/adapter ficam sob `arvoredocumento.adaptador.saida.mtr`;
wire, erros e ordem FT permanecem iguais.

**Verificacao:** stub MTR, erros, configuracao e matriz FT.

**Dependencias:** 3.2. **Escopo:** M, grupos de ate cinco arquivos.

**Status:** concluida em 2026-07-14; DTOs, deserializer, erro de protocolo, REST Client, mapper,
falha interna e adapter exclusivos preservam wire, nulabilidade, erros lossless, matriz FT,
headers e telemetria. Testes focados, stub local, ArchUnit e suite completa passaram. Properties,
caso de uso e Resource permaneceram inalterados; a Task 3.4 nao foi iniciada.

### Task 3.4 - Criar borda simulador de processo

**Criterios de aceite:** fixture usa DTO/mapper proprio; properties atuais selecionam o adapter sem
condicional no caso de uso.

**Verificacao:** fixture real, simulador ligado/desligado e CDI.

**Dependencias:** 3.2. **Escopo:** M.

**Status:** concluida em 2026-07-14; DTO, deserializer e mapper exclusivos leem as fixtures atuais
e preservam a arvore, nulabilidade e o fallback padrao. Qualifiers e producer selecionam simulador
ou MTR pela property existente, sem condicional no caso de uso e com telemetria equivalente.
Testes focados, CDI nos dois estados, ArchUnit e suite completa passaram. Properties, fixtures,
caso de uso e Resource permaneceram inalterados; a Task 3.5 nao foi iniciada.

### Task 3.5 - Migrar borda REST de processo

**Criterios de aceite:** path atual, JSON, validacao, OpenAPI e observabilidade sao equivalentes;
Resource chama somente a porta de entrada de `arvoredocumento`.

**Verificacao:** HTTP, equivalencia ponta a ponta e suite.

**Dependencias:** 3.3 e 3.4. **Escopo:** M.

**Status:** concluida em 2026-07-14; a Resource passou a depender somente da porta de entrada de
`arvoredocumento`, com DTOs e mapper REST exclusivos. Path, JSON, validacao, erros, selecao de
borda e telemetria foram preservados; o legado remanescente sera tratado apenas na Task 3.6.

### Task 3.6 - Ativar guardrails e remover legado de processo

**Criterios de aceite:** ArchUnit proibe dependencias internas para `parametrizacao`/outros dominios;
`rg` prova ausencia de referencias antes de remover artefatos de processo.

**Verificacao:** `rg`, ArchUnit, build e suite.

**Dependencias:** 3.5. **Escopo:** M.

#### Task 3.6a - Inventariar legado e ativar isolamento arquitetural

**Criterios de aceite:** inventario identifica consumidores remanescentes da cadeia legada de
processo; ArchUnit proibe dependencia de `arvoredocumento` para `parametrizacao` e outros dominios,
com prova controlada de deteccao.

**Verificacao:** `rg` e ArchUnit focado.

#### Task 3.6b - Remover a cadeia legada exclusiva de processo

**Criterios de aceite:** referencias remanescentes sao migradas para `arvoredocumento` ou removidas
somente quando houver cobertura equivalente; artefatos de checklist permanecem em `parametrizacao`.

**Verificacao:** `rg` sem referencias de codigo, compilacao e testes focados equivalentes.

#### Task 3.6c - Consolidar verificacoes da fase

**Criterios de aceite:** consulta de processo pertence integralmente a `arvoredocumento`, sem
alteracao de contrato observavel e sem iniciar a Fase 4.

**Verificacao:** `rg`, ArchUnit, build, suite completa e revisao do diff.

**Status:** concluida em 2026-07-14; guardrail explicito ativo, cadeia legada exclusiva de processo
removida sem referencias e checklist preservado em `parametrizacao`. Build, suite, ArchUnit,
JaCoCo e revisao do diff ficaram verdes; README e documentacao consolidada permanecem para o fim
do plano. O Checkpoint C3 recebeu GO humano em 2026-07-14 e a Fase 4 nao foi iniciada.

### Checkpoint C3

- [x] Consulta de processo pertence integralmente a `arvoredocumento`.
- [x] Nao existe calculo de arvore ou IA nesta fase.
- [x] Manifesto comprova OpenAPI, config/profiles, wire, FT, simulador, erros e observabilidade.
- [x] Suite, build e ArchUnit estao verdes; GO humano registrado.

## Fase 4 - `conformidade`

### Task 4.1 - Caracterizar `ConsultarChecklist`

**Criterios de aceite:** HTTP/JSON/validacao, wire MTR, simulador, erros, identificador/versao,
observabilidade, configuracao e matriz FT estao no manifesto. O OpenAPI permanece integralmente
gerado pelo Quarkus, sem teste, filtro ou manipulacao do artefato.

**Verificacao:** testes focados contra `parametrizacao` legado.

**Dependencias:** C3. **Escopo:** M.

**Status:** concluida em 2026-07-14; HTTP, JSON, validacao, nulabilidade, identificador/versao,
wire MTR, resposta sem conteudo, erros, retry, matriz FT, simulador, configuracao e observabilidade
foram congelados em `baseline-consulta-checklist.md` e em caracterizacao executavel. O OpenAPI
permaneceu sob geracao exclusiva do Quarkus, sem teste ou manipulacao do artefato. Nenhuma classe,
property ou fixture de producao foi alterada.

### Task 4.2 - Criar nucleo da consulta de checklist

**Criterios de aceite:** modelo, portas e caso de uso pertencem a `conformidade`, sem compartilhar
modelo de processo ou importar adapters.

**Verificacao:** teste unitario e ArchUnit.

**Dependencias:** 4.1. **Escopo:** M.

**Status:** concluida em 2026-07-14; comando semantico, modelos de checklist/apontamento, portas de
entrada/saida e caso de uso reativo pertencem exclusivamente a `conformidade`. O caso de uso
delega sem bloquear e preserva sucesso e falha; guardrails progressivos impedem dependencia entre
dominios, uso de bordas pela aplicacao e exposicao da porta de saida pela API de entrada. Teste
unitario e ArchUnit focados verdes, sem ligar REST, MTR, simulador ou CDI.

### Task 4.3 - Criar borda MTR de checklist

**Criterios de aceite:** DTO/mapper/client/adapter exclusivos preservam wire, erros e ordem FT.

**Verificacao:** stub MTR, erros, configuracao e matriz FT.

**Dependencias:** 4.2. **Escopo:** M, grupos de ate cinco arquivos.

**Status:** concluida em 2026-07-14; DTO e mapper MTR v1, erro tecnico, REST Client, qualifier,
adapter e erro interno pertencem exclusivamente a `conformidade`. Foram preservados o wire GET
por identificador/versao, nulabilidade, classificacao e payload dos erros, timeout, retry, circuit
breaker, headers, OIDC e observabilidade legada. A borda REST publica e o simulador permanecem no
legado ate as Tasks 4.4 e 4.5.

### Task 4.4 - Criar borda simulador de checklist

**Criterios de aceite:** fixture usa DTO/mapper proprio; properties atuais sao preservadas.

**Verificacao:** fixture, simulador ligado/desligado e CDI.

**Dependencias:** 4.2. **Escopo:** M.

**Status:** concluida em 2026-07-14; DTO, mapper, qualifier, adapter e producer exclusivos de
`conformidade` leem a fixture existente e preservam nulabilidade, fallback silencioso,
observabilidade e a property `simtr-hub.simulador.parametrizacao-checklist.habilitado`. A porta de
saida seleciona simulador quando habilitado e MTR quando desabilitado, sem condicional no caso de
uso. A fixture, as properties, o caso de uso e a borda REST publica permaneceram inalterados.

### Task 4.5 - Migrar borda REST de checklist

**Criterios de aceite:** path, JSON, validacao, OpenAPI e observabilidade equivalentes; Resource usa
somente a porta de entrada de `conformidade`.

**Verificacao:** HTTP, equivalencia ponta a ponta e suite.

**Dependencias:** 4.3 e 4.4. **Escopo:** M.

**Status:** concluida em 2026-07-14; DTOs e mapper REST exclusivos, fronteira de
observabilidade e Resource ligam o endpoint apenas a porta de entrada de `conformidade`. Path,
JSON, validacao, nulabilidade, resposta sem conteudo, erros, selecao MTR/simulador, fault tolerance
e telemetria permanecem equivalentes. O OpenAPI continua integralmente gerado pelo Quarkus, sem
teste, filtro ou manipulacao. Fachada, service, mapper, VOs e DTOs legados permanecem para o
inventario e a remocao controlada da Task 4.6.

#### Task 4.5a - Criar contrato e mapper REST de checklist

**Criterios de aceite:** DTOs pertencem a borda de entrada de `conformidade`; nomes JSON,
omissao de nulos, listas e elementos nulos permanecem iguais; falhas internas sao traduzidas para
o erro publico existente sem perda de payload.

**Verificacao:** teste unitario do mapper e serializacao JSON.

#### Task 4.5b - Ligar Resource e observabilidade pela porta de entrada

**Criterios de aceite:** a nova Resource substitui somente a rota legada e injeta apenas
`ConsultarChecklist`; path, validacoes, resposta sem conteudo, spans, atributos e logs permanecem
iguais. Fachada, service, mapper, VOs e DTOs legados permanecem para o inventario da Task 4.6.

**Verificacao:** teste HTTP com porta de entrada controlada e teste unitario da fronteira de
observabilidade.

#### Task 4.5c - Consolidar equivalencia da borda REST

**Criterios de aceite:** caminhos de simulador e MTR, erros, nulabilidade, headers, FT e telemetria
continuam cobertos sem teste, filtro ou manipulacao do OpenAPI gerado pelo Quarkus.

**Verificacao:** contratos HTTP, stub MTR, observabilidade, ArchUnit e suite completa.

### Task 4.6 - Ativar guardrails e remover legado de checklist

**Criterios de aceite:** ArchUnit proibe dependencias internas para `parametrizacao`/outros dominios;
`rg` prova ausencia de referencias antes da remocao.

**Verificacao:** `rg`, ArchUnit, build e suite.

**Status:** concluida. O dominio `conformidade` possui guardrail explicito contra dependencias de
outros dominios; contratos remanescentes usam a borda ativa; e a cadeia legada exclusiva foi
removida apos ausencia comprovada de consumidores. Configuracao do REST Client e fixture Markdown
compartilhadas foram preservadas. Build limpo, suite completa e revisao multi-eixo passaram sem
bloqueios; C4 recebeu GO humano em 2026-07-14.

**Dependencias:** 4.5. **Escopo:** M.

#### Task 4.6a - Inventariar legado e ativar isolamento de `conformidade`

**Criterios de aceite:** o inventario separa consumidores reais, testes historicos e artefatos
compartilhados; ArchUnit proibe explicitamente `conformidade` de depender de `parametrizacao` ou
de outros dominios, com prova controlada de deteccao.

**Verificacao:** `rg` e ArchUnit focado.

#### Task 4.6b - Migrar consumidores de teste remanescentes

**Criterios de aceite:** caracterizacao HTTP/MTR, erros, spans e guardrails apontam para a borda de
`conformidade`; cenarios exclusivos do legado so saem quando a cobertura nova equivalente estiver
identificada e verde.

**Verificacao:** testes focados da nova borda e busca sem imports dos tipos legados.

##### Task 4.6b.1 - Atualizar contratos para a borda nova

**Criterios de aceite:** caracterizacao ponta a ponta, classificacao de erro e declaracao de span
nao dependem mais de client ou gateway legados.

**Verificacao:** contratos de checklist, excecao e observabilidade focados.

##### Task 4.6b.2 - Remover testes redundantes e fixture sem uso

**Criterios de aceite:** testes exclusivos de service, mapper, gateway e mock factory legados sao
removidos somente depois de confirmar cobertura equivalente nas bordas MTR, simulador, REST e CDI;
`TestFixtures` deixa de expor DTO legado sem afetar outros dominios.

**Verificacao:** `rg`, compilacao e testes focados equivalentes.

#### Task 4.6c - Remover a cadeia legada exclusiva de checklist

**Criterios de aceite:** fachada, service, mapper, VOs, DTOs REST antigos, gateway, REST Client,
exception mapper e mock factory sao removidos somente depois de zero consumidores. Property do
REST Client e fixture Markdown permanecem porque pertencem ao contrato da borda nova.

**Verificacao:** `rg` sem referencias de codigo e compilacao.

#### Task 4.6d - Consolidar verificacoes da fase

**Criterios de aceite:** consulta de checklist pertence integralmente a `conformidade`, sem iniciar
C4 ou a Fase 5 e sem alterar contratos externos ou o OpenAPI gerado pelo Quarkus.

**Verificacao:** `rg`, ArchUnit, build, suite completa e revisao do diff.

### Checkpoint C4

- [x] Consulta de checklist pertence integralmente a `conformidade`.
- [x] Nao existe analise documental ou workflow nesta fase.
- [x] Manifesto comprova contratos externos, config/profiles, wire, FT, simulador, erros e observabilidade.
- [x] Suite, build e ArchUnit estao verdes; GO humano registrado.

## Fase 5 - `gestaodocumento`

### Task 5.1 - Caracterizar `ObterCredencialContainer`

**Criterios de aceite:** HTTP/JSON/OpenAPI, wire MTR, simulador, erros, expiracao, observabilidade,
configuracao e matriz FT estao no manifesto sem assumir duracao fixa.

**Verificacao:** testes focados contra o legado.

**Dependencias:** C4. **Escopo:** M.

**Status:** concluida em 2026-07-14; `baseline-obter-credencial-container.md` e caracterizacao
executavel congelam HTTP/JSON/nulabilidade, wire MTR, headers/OIDC/trace, validade opaca, erros,
retry, matriz FT, simulador, configuracao e observabilidade sem alterar producao. O OpenAPI segue
gerado exclusivamente pelo Quarkus, sem teste, filtro ou manipulacao. Nenhuma duracao de SAS foi
inferida.

### Task 5.2 - Criar nucleo de credencial

**Criterios de aceite:** modelo, portas e caso de uso representam somente obtencao de credencial;
nao importam Azure SDK, cache ou adapters.

**Verificacao:** teste unitario e ArchUnit.

**Dependencias:** 5.1. **Escopo:** S/M.

**Status:** concluida em 2026-07-14; `CredencialContainer`, portas de entrada/saida e caso de uso
reativo pertencem ao novo nucleo de `gestaodocumento`. SAS e validade permanecem opacos e
nulaveis, sem calculo, normalizacao ou copia; a aplicacao nao importa DTOs ou adapters. Os
guardrails do nucleo foram ativados.

### Task 5.3 - Criar borda MTR de credencial

**Criterios de aceite:** DTO/mapper/client/adapter preservam wire, expiracao, erros e ordem FT.

**Verificacao:** stub MTR, erros, configuracao e matriz FT.

**Dependencias:** 5.2. **Escopo:** M.

**Status:** concluida em 2026-07-14; DTO de resposta, erro protocolar, REST Client, falha interna,
mapper, qualifier e adapter MTR exclusivos de `gestaodocumento` preservam o POST sem corpo, JSON,
nulabilidade, validade opaca, headers, OIDC, propagacao de trace, classificacao lossless de erros,
ordem de fault tolerance e observabilidade contratual. A porta de saida ainda nao foi ligada ao
caso de uso, ao producer, ao Resource ou ao simulador; a Task 5.4 nao foi iniciada.

### Task 5.4 - Criar borda simulador de credencial

**Criterios de aceite:** fixture usa DTO/mapper proprio e properties atuais sao preservadas.

**Verificacao:** simulador ligado/desligado e CDI.

**Dependencias:** 5.2. **Escopo:** S/M.

**Status:** concluida em 2026-07-14; DTO, mapper, qualifier, adapter e producer exclusivos de
`gestaodocumento` leem a fixture existente sem reutilizar DTO publico ou MTR. SAS e validade
permanecem opacos, inclusive quando nulos ou estruturados; fixture ausente continua falhando sem
fallback. A property existente seleciona simulador ou MTR sem condicional no caso de uso, e os
mesmos sinais do legado — atributo de origem e evento estruturado — foram preservados. Fixture, properties,
caso de uso e Resource permaneceram inalterados; a Task 5.5 nao foi iniciada.

### Task 5.5 - Migrar borda REST de credencial

**Criterios de aceite:** path, JSON, OpenAPI e observabilidade equivalentes; Resource usa somente a
porta de entrada e devolve expiracao contratual.

**Verificacao:** HTTP, equivalencia ponta a ponta e suite.

**Dependencias:** 5.3 e 5.4. **Escopo:** S/M.

**Status:** concluida em 2026-07-14; response e mapper REST exclusivos, fronteira de
observabilidade e Resource ligam o endpoint somente a `ObterCredencialContainer`. Path, resposta,
campos JSON inclusive nulos, validade opaca, erros, selecao MTR/simulador, fault tolerance e
telemetria permanecem equivalentes. A Resource legada foi substituida, enquanto a cadeia restante
permanece para o inventario da Task 5.6. Testes RED/GREEN, contratos HTTP ponta a ponta, ArchUnit e
suite completa passaram; a evidencia de cobertura permanece exclusivamente no JaCoCo. O OpenAPI
continua gerado pelo Quarkus, sem teste, filtro ou manipulacao.

#### Task 5.5a - Criar contrato e mapper REST de credencial

**Criterios de aceite:** response e mapper pertencem a borda REST de `gestaodocumento`; os quatro
campos, nulos e validade opaca permanecem iguais; falhas internas viram os erros publicos atuais
sem perda de payload.

**Verificacao:** teste unitario de mapeamento, JSON e erros.

#### Task 5.5b - Ligar Resource e observabilidade pela porta de entrada

**Criterios de aceite:** a Resource substitui somente a rota legada e injeta apenas
`ObterCredencialContainer`; spans, atributos e logs permanecem iguais sem registrar SAS ou
validade. A cadeia legada restante permanece para a Task 5.6.

**Verificacao:** testes da fronteira de observabilidade, Resource e bean CDI compartilhado.

#### Task 5.5c - Consolidar equivalencia da borda REST

**Criterios de aceite:** HTTP, JSON, nulos, validade, erros, MTR, simulador, FT e telemetria
continuam cobertos sem teste, filtro ou manipulacao do OpenAPI gerado pelo Quarkus.

**Verificacao:** contratos focados, ArchUnit, suite completa e revisao do diff.

### Task 5.6 - Provar limites de escopo

**Criterios de aceite:** ArchUnit/testes impedem Azure Blob SDK, cache/renovacao de SAS e upload no
nucleo; artefatos legados so saem depois de `rg` sem referencias.

**Verificacao:** `rg`, ArchUnit, build e suite.

**Dependencias:** 5.5. **Escopo:** S/M.

**Status:** concluida em 2026-07-14; guardrails bloqueiam Azure Storage, bibliotecas de cache e
declaracoes de cache, renovacao, upload ou Blob no nucleo de `gestaodocumento`, com fixtures
negativas controladas. Contratos compartilhados de fault tolerance e spans foram migrados para o
client/adapter novos; testes exclusivamente legados e a cadeia antiga foram removidos somente
apos inventario e `rg` sem consumidores. Buscas finais, ArchUnit, matriz focada, suite Maven
completa, JaCoCo, seguranca e diff passaram sem dependencia, configuracao, OpenAPI ou telemetria
sensivel novos. O Checkpoint C5 recebeu GO humano em 2026-07-15 e encerrou a Fase 5.

### Checkpoint C5

**Status:** `GO` humano registrado em 2026-07-15; Fase 5 encerrada. A Fase 6 foi posteriormente
aberta em sua propria branch e permanece aguardando o Checkpoint C6.

- [x] Hub apenas obtem e devolve a credencial.
- [x] Nao existe cache, renovacao ou upload.
- [x] Manifesto comprova contratos externos, config/profiles, wire, FT, simulador, erros e observabilidade.
- [x] Suite, build e ArchUnit estao verdes; GO humano registrado.

## Fase 6 - Consolidacao final

### Task 6.1 - Remover o dominio legado `parametrizacao`

**Criterios de aceite:** `rg` prova ausencia de imports/referencias de producao, teste, config e
documentacao ativa; somente entao o package e removido.

**Verificacao:** `rg`, build e suite.

**Dependencias:** C5. **Escopo:** S.

**Status:** concluida em 2026-07-15. O inventario confirmou que nao existia mais package de
producao `parametrizacao`; restava somente uma fixture negativa de ArchUnit sob o package legado.
A prova foi migrada para uma dependencia controlada entre dominios ativos, sem reduzir os testes
de isolamento, e o ultimo arquivo Java do package foi removido. Nomes `parametrizacao` que fazem
parte dos contratos externos — paths, config keys, fixtures MTR e telemetria — foram
deliberadamente preservados. Buscas de package/import, ArchUnit focado, build e suite limpa
completa passaram; a evidencia quantitativa permanece exclusivamente no JaCoCo.

### Task 6.2 - Endurecer ArchUnit para todo o codigo migrado

**Criterios de aceite:** regras finais cobrem dependencia por camada, DTOs por borda, excecao formal
do erro REST, REST Clients, API publica de aplicacao e imports cross-domain apenas em adapters ACL.

**Verificacao:** ArchUnit inclui provas negativas controladas e suite verde.

**Dependencias:** 6.1. **Escopo:** M.

**Status:** concluida em 2026-07-15. Os guardrails finais cobrem dominio, aplicacao, adapters REST,
DTOs confinados por borda, excecao formal do erro REST tecnico, declaracao e consumo de REST
Clients, API publica de entrada e dependencias cross-domain somente por adapters ACL. ACLs podem
usar a porta de entrada e os tipos semanticos do fornecedor, mas nao seus casos de uso, portas de
saida ou bordas. Provas negativas controladas foram mantidas ou adicionadas para cada fronteira
nova. Ciclos RED/GREEN, ArchUnit focado, revisao multi-eixo e suite limpa completa passaram; a
evidencia quantitativa permanece exclusivamente no JaCoCo.

### Task 6.3 - Executar verificacao de equivalencia completa

**Criterios de aceite:** manifesto comprova todos os contratos; nenhum teste baseline foi removido ou
desabilitado sem GO; OpenAPI, configs/profiles, wire, FT, simulador, erros e observabilidade batem.

**Verificacao:** `mvn -q test`, build, testes ponta a ponta locais e revisao do diff.

**Dependencias:** 6.2. **Escopo:** M.

**Status:** concluida em 2026-07-15. `equivalencia-final.md` indexa as oito capacidades e seus
contratos executaveis de HTTP/JSON/validacao, wire MTR, FT, simulador, erros, configuracao e
observabilidade. A suite limpa completa e o build passaram; os relatorios Surefire nao registram
falha, erro ou teste pulado, e o JaCoCo foi regenerado. Buscas confirmaram ausencia de teste,
filtro ou artefato OpenAPI proibido, de package Java legado e de credencial no diff. Nenhum arquivo
de producao, property ou fixture funcional foi alterado na Fase 6 ate este ponto.

### Task 6.4 - Atualizar documentacao de retomada

**Criterios de aceite:** packages finais, desvios, dividas e proximo checkpoint estao documentados;
`AGENTS.md`, plano e checklist apontam para estado concluido sem instrucoes obsoletas.

**Verificacao:** links locais, busca por documentos substituidos e revisao humana.

**Dependencias:** 6.3. **Escopo:** S.

**Status:** concluida em 2026-07-15. README, documento operacional, `AGENTS.md`, plano e checklist
foram consolidados na arquitetura final; baselines substituidos foram identificados como
historicos e apontam para `equivalencia-final.md`. A colecao Postman ativa usa os paths publicos
`/simtr-hub` e agrupamentos por capacidade, com JSON valido. Links locais e referencias
obsoletas foram revisados; nenhuma mudanca de producao, configuracao funcional, OpenAPI, Docker
ou Dev Services foi introduzida. Como somente documentacao e a colecao Postman mudaram desde a
ultima suite limpa verde, o Maven nao foi repetido. A implementacao da Fase 6 foi encerrada e o
Checkpoint C6 recebeu GO humano em 2026-07-15.

### Checkpoint C6 - Conclusao

- [x] Oito capacidades existentes migradas.
- [x] Nenhum comportamento externo mudou sem decisao explicita.
- [x] Nenhum endpoint, workflow ou upload novo foi implementado.
- [x] `mvn -q test` e build passam sem testes desabilitados.
- [x] ArchUnit protege as fronteiras finais.
- [x] Revisao humana confirma a conclusao.

**Status:** `GO` humano registrado em 2026-07-15; Fase 6 e plano de refatoracao encerrados.

## Fase 7 - Alinhamento documental do escopo de endpoints

### Task 7.1 - Confrontar a especificacao de pre-validacao com a solucao existente

**Descricao:** inventariar todos os endpoints MTR descritos em
`../doc/api-integracao-mtr-pre-validacao-v1.md`, distinguir claramente os que possuem capacidade
e endpoint publico no Hub daqueles que ainda nao existem nesta solucao e alinhar README,
documento arquitetural e documento operacional ao codigo executavel.

**Criterios de aceite:**

- a matriz cobre todos os endpoints MTR da especificacao e indica, sem ambiguidade, implementado
  ou nao implementado no Hub;
- endpoints ausentes nao aparecem como capacidade pronta, promessa de entrega ou rota publica;
- paths publicos e MTR implementados, packages, limites de escopo e decisao OpenAPI correspondem
  ao codigo atual;
- a alteracao fica restrita a documentacao e governanca da fase.

**Verificacao:** inventario de Resources e REST Clients, comparacao com a especificacao, links
locais, busca por afirmacoes divergentes e revisao multi-eixo do diff.

**Dependencias:** C6. **Escopo:** S, subdividido por grupo documental.

**Status:** concluida em 2026-07-15. A especificacao contem treze endpoints MTR: oito possuem
capacidade e rota publica no Hub, enquanto garantia, produto, captura, cancelamento e consulta de
dossie por identificador nao estao implementados nesta solucao. README, decisao arquitetural e
documento operacional distinguem API upstream de rota do Hub, registram as cinco ausencias sem
promessa de entrega e explicam a montagem de paths pelo gateway. O documento canonico tambem foi
ajustado ao package real do erro REST, ao estado final do ArchUnit e a decisao de OpenAPI gerado
exclusivamente pelo Quarkus. Inventario do codigo, comparacao automatica dos endpoints, links,
buscas de divergencia, seguranca e `git diff --check` passaram. Nenhum arquivo de producao, teste
ou configuracao funcional mudou; por isso o Maven nao foi repetido. C7 recebeu GO humano em
2026-07-15.

### Checkpoint C7 - Alinhamento documental

- [x] Matriz diferencia todos os endpoints implementados e ausentes da especificacao.
- [x] README, decisao arquitetural e documento operacional descrevem a mesma solucao.
- [x] Nenhum arquivo de producao, teste ou configuracao funcional foi alterado.
- [x] Revisao humana confirma a clareza do escopo.

**Status:** `GO` humano registrado em 2026-07-15; Fase 7 documental encerrada.

## Fase 8 - Introducao conceitual da solucao

### Task 8.1 - Incorporar os fundamentos introdutorios ao documento canonico

**Descricao:** analisar `../doc/organizacao-arquitetural-da-solucao-simtr-hub-v2.md` e incorporar
ao inicio de `../doc/arquitetura-ddd-integracoes-atomicas.md` somente o contexto que explica a
organizacao por dominio, a separacao entre aplicacao e infraestrutura e o uso de Portas e
Adaptadores no `simtr-hub`.

**Criterios de aceite:**

- a introducao explica a solucao atual antes das regras detalhadas;
- conceitos futuros, como orquestradores, microsservicos e Quarkus Flow, nao sao apresentados
  como componentes existentes;
- a versao historica da arquitetura contida no documento-fonte nao substitui o estado canonico
  verificado depois das Fases 6 e 7;
- a alteracao fica restrita a documentacao e governanca da fase.

**Verificacao:** comparacao com o documento-fonte e com o estado canonico atual, links locais,
busca por afirmacoes divergentes, revisao multi-eixo e `git diff --check`.

**Dependencias:** C7. **Escopo:** S, exclusivamente documental.

**Status:** concluida em 2026-07-15. A introducao do documento canonico agora explica a solucao
implementada como monolito modular organizado por dominio, distingue aplicacao de infraestrutura
e descreve o fluxo atual por portas e adapters REST, MTR e simulador. Orquestradores,
microsservicos e motores de workflow permanecem identificados apenas como possibilidades futuras.
A copia historica da arquitetura presente no documento-fonte nao foi incorporada. Links, escopo
documental, consistencia das afirmacoes e `git diff --check` passaram; nenhum arquivo de producao,
teste ou configuracao funcional mudou, portanto o Maven nao foi repetido.

### Checkpoint C8 - Introducao arquitetural

- [x] A introducao explica objetivo, organizacao por dominio e separacao aplicacao/infraestrutura.
- [x] Portas e Adaptadores sao descritos como organizacao interna da solucao atual.
- [x] Elementos futuros permanecem explicitamente separados da implementacao existente.
- [x] Nenhum arquivo de producao, teste ou configuracao funcional foi alterado.
- [x] Revisao humana aceita a consolidacao introdutoria.

**Status:** `GO` humano registrado em 2026-07-15; Fase 8 documental encerrada.

## Fase 9 - Fundamentos hexagonais e papel do orquestrador

### Task 9.1 - Consolidar a explicacao geral antes da aplicacao ao Hub

**Descricao:** reorganizar o inicio de `../doc/arquitetura-ddd-integracoes-atomicas.md` para
apresentar a separacao entre aplicacao e infraestrutura, a Arquitetura Hexagonal em termos gerais,
a direcao das dependencias, sua relacao com microsservicos e seu uso pragmatico antes de explicar
como esses fundamentos aparecem no `simtr-hub`.

**Criterios de aceite:**

- a explicacao geral precede a descricao especifica da solucao;
- o texto distingue aplicacao de infraestrutura e registra os beneficios esperados da separacao;
- portas, adapters e direcao das dependencias sao explicados sem depender de tecnologia;
- o exemplo diferencia orquestrador externo de entrada, orquestrador local da camada de aplicacao
  e orquestrador externo consumido por uma porta de saida;
- o texto afirma que Arquitetura Hexagonal nao define a topologia de microsservicos e deve ser
  aplicada de forma pragmatica;
- todos os papeis de orquestracao permanecem conceituais e futuros; a solucao atual continua sem
  endpoint de pre-validacao, orquestrador local ou motor de workflow;
- a alteracao fica restrita a documentacao e governanca da fase.

**Verificacao:** comparacao com o documento-fonte, coerencia com as regras canonicas e o inventario
de endpoints, links locais, busca por afirmacoes divergentes, revisao multi-eixo e
`git diff --check`.

**Dependencias:** C8. **Escopo:** S, exclusivamente documental.

**Status:** concluida em 2026-07-15. O documento canonico apresenta primeiro organizacao por
dominio, separacao entre aplicacao e infraestrutura e Arquitetura Hexagonal em termos gerais;
depois explica sua aplicacao ao `simtr-hub`. A direcao da interacao distingue orquestrador externo
de entrada, coordenacao local futura na camada de aplicacao e orquestrador externo consumido por
porta de saida. Microsservicos e uso pragmatico foram contextualizados sem transformar evolucao
futura em componente atual. Busca no codigo confirmou ausencia de implementacao de orquestrador;
links, escopo documental, seguranca, revisao multi-eixo e `git diff --check` passaram. Producao,
testes e configuracao funcional nao mudaram, portanto o Maven nao foi repetido.

### Checkpoint C9 - Fundamentos hexagonais

- [x] Aplicacao e infraestrutura estao separadas antes da descricao da solucao.
- [x] Arquitetura Hexagonal, portas, adapters e direcao das dependencias estao explicados em geral.
- [x] Os tres papeis conceituais de orquestracao estao distintos e marcados como futuros.
- [x] Microsservicos e uso pragmatico estao contextualizados sem promessa de implementacao.
- [x] Nenhum arquivo de producao, teste ou configuracao funcional foi alterado.
- [x] Revisao humana aceita a consolidacao.

**Status:** `GO` humano registrado em 2026-07-15; Fase 9 documental encerrada.

## Fase 10 - Uso pragmatico do Quarkus

### Task 10.1 - Permitir framework em qualquer componente sem relaxar as fronteiras

**Descricao:** explicitar que a Arquitetura Hexagonal e uma orientacao de organizacao e direcao de
dependencias, nao uma politica de pureza tecnologica. Quarkus e suas APIs associadas podem ser
usados em qualquer componente quando forem uteis; nenhum guardrail deve rejeitar uma classe
somente pelo framework utilizado.

**Criterios de aceite:**

- Quarkus, Jakarta, MicroProfile, Mutiny, Jackson e OpenTelemetry nao sao proibidos por camada;
- ArchUnit continua impedindo dominio e aplicacao de depender de Resources, adapters, fachadas,
  mapeamentos e demais bordas proibidas;
- isolamento entre dominios, confinamento de DTOs, API publica de aplicacao e regras especificas
  de escopo permanecem ativos;
- uma prova executavel demonstra que o uso de Quarkus e aceito no dominio e na aplicacao;
- documento canonico, README e documento operacional descrevem a mesma decisao pragmatica;
- nenhum endpoint, contrato, comportamento ou arquivo de producao e alterado.

**Subtasks:**

- **10.1a:** registrar fase, branch, escopo e checkpoint na governanca;
- **10.1b:** criar a prova RED de que Quarkus deve ser aceito no dominio e na aplicacao;
- **10.1c:** remover dos guardrails somente as proibicoes por tecnologia e manter as fronteiras;
- **10.1d:** alinhar documento canonico, README e documento operacional;
- **10.1e:** executar ArchUnit, suite completa, buscas de contradicao e revisao multi-eixo.

**Verificacao:** ciclo RED/GREEN do `ArchUnitProgressivoTest`, `mvn -q clean test`, buscas de
afirmacoes divergentes, `git diff --check`, verificacao de segredos e revisao multi-eixo.

**Dependencias:** C9. **Escopo:** M, sem alteracao de producao.

**Status:** concluida em 2026-07-15 na branch `refactor/ddd-fase-10-baseline`, criada e publicada a
partir do commit `2e9641e` aceito em C9. O ciclo RED/GREEN confirmou a permissao de Quarkus e a
preservacao das fronteiras; ArchUnit, suite limpa completa, JaCoCo, buscas, seguranca e revisao
multi-eixo passaram. Referencias a codigo `framework-free` nas fases anteriores sao registro
historico dos criterios executados naquela data e ficam superadas pela decisao normativa desta
Fase 10. Nenhum arquivo de producao foi alterado. C10 recebeu GO humano em 2026-07-15.

### Checkpoint C10 - Pragmatismo de framework

- [x] Quarkus e suas APIs associadas podem ser usados em qualquer componente sem bloqueio por camada.
- [x] Fronteiras de dominio, aplicacao, adapters, DTOs e integracoes entre dominios permanecem protegidas.
- [x] Documento canonico, README e documento operacional estao alinhados.
- [x] ArchUnit, suite completa, build e revisao multi-eixo estao verdes.
- [x] Nenhum arquivo de producao, endpoint, contrato ou comportamento foi alterado.
- [x] Revisao humana aceita a decisao e autoriza o fechamento.

**Status:** `GO` humano registrado em 2026-07-15; Fase 10 encerrada.

## Fase 11 - Cascata Jakarta Validation de formulario e documento

### Task 11.1 - Remover o uso depreciado de `@Valid` em listas REST

**Descricao:** mover `@Valid` do container `List` para o argumento de tipo nas seis listas da
borda REST de formulario e documento. A mudanca deve eliminar `HV000271` sem alterar paths,
status, JSON, mensagens de validacao, nulabilidade ou propagacao para MTR/simulador.

**Criterios de aceite:**

- as seis declaracoes usam `List<@Valid T>` e nenhuma delas mantem `@Valid List<T>`;
- contratos HTTP protegem as mensagens de chave e valor de atributos e propriedades de documento;
- listas nulas, elementos nulos, objetos vazios e campos internos nulos continuam aceitos onde ja
  pertencem ao contrato;
- o teste focado inicializa o Hibernate Validator sem `HV000271` para formulario ou documento;
- a suite completa, ArchUnit e o build permanecem verdes;
- o package REST `dossieproduto.recurso.rest.v1` nao e renomeado nesta fase;
- o contrato tecnico compartilhado em `arquitetura.excecao.dto` permanece inalterado.

**Subtasks:**

- **11.1a:** registrar fase, branch, escopo e checkpoint na governanca;
- **11.1b:** adicionar uma prova RED da colocacao depreciada e completar os contratos HTTP de
  documento;
- **11.1c:** migrar as seis listas para anotacao no argumento de tipo e executar testes focados;
- **11.1d:** executar suite limpa, revisar warnings, diff, segredos e alinhamento documental;
- **11.1e:** publicar o checkpoint e aguardar GO humano.

**Verificacao:** ciclo RED/GREEN do contrato de metadados Jakarta Validation,
`DossieProdutoErroApiContractTest`, contratos MTR de formulario/documento,
`ArchUnitProgressivoTest`, `mvn -q clean test`, ausencia de `HV000271`, `git diff --check`,
verificacao de segredos e revisao do diff.

**Dependencias:** C10. **Escopo:** S, com alteracao restrita aos metadados de validacao REST,
testes contratuais e governanca.

**Status:** concluida em 2026-07-15 na branch `refactor/ddd-fase-11-baseline`, criada e publicada a
partir do commit `6d57872`, aceito em C10 e integrado a `main`. O ciclo RED/GREEN comprovou a
colocacao da cascata no tipo dos elementos; contratos HTTP e de nulabilidade permaneceram verdes,
a suite limpa terminou sem `HV000271` e os dois desvios de package ficaram inalterados.

### Checkpoint C11 - Cascata Jakarta Validation

- [x] As seis listas usam `List<@Valid T>` e o warning depreciado foi eliminado.
- [x] Mensagens, nulabilidade, paths, status e JSON permanecem protegidos e equivalentes.
- [x] Package REST e contrato tecnico compartilhado de erro permanecem fora do escopo.
- [x] Testes focados, suite completa, build, ArchUnit e revisao do diff estao verdes.
- [x] Revisao humana aceita a mudanca e autoriza o fechamento.

**Status:** `GO` humano registrado em 2026-07-15; Fase 11 encerrada. A renomeacao opcional do
package REST foi autorizada como trabalho separado para uma nova fase.

## Fase 12 - Alinhamento do package REST de `dossieproduto`

### Task 12.1 - Renomear `recurso.rest` para `adaptador.entrada.rest`

**Descricao:** mover integralmente a borda REST de `dossieproduto` e seus testes do package
historico `dossieproduto.recurso.rest` para `dossieproduto.adaptador.entrada.rest`. A mudanca e
organizacional: paths, verbos, status, JSON, validacoes, erros, observabilidade e integracoes nao
podem mudar.

**Criterios de aceite:**

- Resource, mappers e DTOs residem sob `dossieproduto.adaptador.entrada.rest.v1`;
- testes e fixtures arquiteturais usam o mesmo package de adapter de entrada;
- nao restam declaracoes, imports, paths Java ou allowlists para `dossieproduto.recurso.rest`;
- ArchUnit protege apenas o padrao `adaptador.entrada.rest` e impede regressao para o package antigo;
- endpoints HTTP e contratos de validacao permanecem equivalentes;
- o contrato tecnico compartilhado em `arquitetura.excecao.dto` permanece inalterado;
- README, documento operacional, AGENTS e governanca descrevem o estado novo;
- testes focados, suite limpa, build e diff permanecem verdes.

**Subtasks:**

- **12.1a:** registrar fase, branch, escopo e checkpoint C12;
- **12.1b:** criar guardrail RED que rejeita o package historico;
- **12.1c:** mover producao/testes e atualizar FQCNs, imports e guardrails;
- **12.1d:** alinhar documentacao e executar verificacoes focadas/completas;
- **12.1e:** publicar o checkpoint e aguardar GO humano.

**Verificacao:** ciclo RED/GREEN do `ArchUnitProgressivoTest`, busca global pelo FQCN antigo,
contratos REST de `dossieproduto`, `ResourceBeanCoverageTest`, `mvn -q clean test`,
`git diff --check`, verificacao de segredos e revisao do diff.

**Dependencias:** C11. **Escopo:** M, refatoracao mecanica de package sem mudanca funcional.

**Status:** concluida e aceita em 2026-07-15 na branch `refactor/ddd-fase-12-baseline`, criada e
publicada a partir do commit `54569d7`, aceito em C11. O ciclo RED/GREEN, os contratos focados, a
suite limpa, o build, a documentacao e o diff passaram; GO humano registrado no C12.

### Checkpoint C12 - Package REST alinhado

- [x] Borda REST e testes residem em `dossieproduto.adaptador.entrada.rest`.
- [x] Package/FQCN antigo nao possui declaracoes, imports ou allowlists e esta proibido por guardrail.
- [x] Contratos externos e package tecnico compartilhado de erro permanecem inalterados.
- [x] Documentacao, testes focados, suite limpa, build e diff estao verdes.
- [x] Revisao humana aceita a renomeacao e autoriza o fechamento.

**Status:** `GO` humano registrado em 2026-07-15; Fase 12 encerrada. Nenhuma nova fase foi iniciada.

## Riscos e mitigacoes

| Risco | Impacto | Mitigacao |
|---|---|---|
| Big bang estrutural | Alto | fatias verticais e GO/NO-GO |
| Testes atuais cobrirem apenas simulador | Alto | stub MTR com simulador desabilitado |
| Corpo de erro mudar | Alto | traducao lossless e teste ponta a ponta |
| CDI ambiguo | Alto | qualifiers/producer e teste de bootstrap |
| Retry duplicar mutacao | Alto | preservar agora e bloquear uso futuro sem idempotencia |
| Fixture depender de DTO publico | Medio | DTO/mapper do simulador |
| Logs/spans mudarem com nomes Java | Medio | inventario e assercoes contratuais |
| Tasks crescerem alem de cinco arquivos | Medio | quebrar subtask em caracterizacao, nucleo e adapters |

## Questoes explicitamente adiadas

- escolha e desenho de Quarkus Flow;
- estado e persistencia de orquestradores;
- contratos entre futuros microsservicos;
- idempotencia funcional no MTR;
- novos endpoints do ciclo de vida de dossie;
- modelo do calculo da arvore, IA e conformidade;
- upload e lifecycle de SAS no microsservico consumidor.

## Fase 13 - Reducao segura de duplicacao

**Status:** especificacao, plano tecnico e tasks aprovados no C13-SPEC, C13-PLAN e C13-TASKS;
inventario e classificacao concluidos; C13-INVENTARIO pendente; nenhuma alteracao de producao
autorizada.

**Documento de especificacao:** `especificacao-fase-13-reducao-duplicacao.md`.

**Objetivo aprovado:** reduzir somente duplicacao removivel, preservando contratos externos,
fronteiras de dominio e independencia entre REST, MTR e simulador. A baseline Sonar da abertura e
6,0% de densidade, 735 linhas e 26 blocos duplicados, com cobertura geral de 80,0%, complexidade
ciclomatica 1.352 e complexidade cognitiva 837.

### Decisoes arquiteturais do plano

1. A metrica identifica candidatos; ela nao determina ownership nem autoriza compartilhamento.
2. Os 26 blocos serao inventariados antes de escolher qualquer arquivo de producao.
3. DTOs e mapeamentos independentes entre REST, MTR e simulador permanecem separados.
4. Duplicacao entre dominios permanece quando a extracao criaria dependencia ou modelo comum.
5. O piloto fica no mesmo dominio e na mesma borda, toca no maximo cinco arquivos nao mecanicos e
   nao adiciona dependencia.
6. A fase para depois de um unico piloto e exige GO humano antes de considerar outro bloco.
7. Token Sonar com Browse permanece somente em memoria e deve ser revogado depois do inventario.

### Grafo de dependencias

```text
C13-SPEC GO
    -> C13-PLAN GO
        -> decomposicao das tasks
            -> C13-TASKS GO
                -> acesso Sonar somente leitura
                    -> inventario dos 26 blocos
                        -> classificacao arquitetural
                            -> C13-INVENTARIO GO e escolha do piloto
                                -> caracterizacao focada
                                    -> refatoracao de um bloco
                                        -> suite + ArchUnit + Sonar
                                            -> C13 GO/NO-GO
```

Nenhum passo de producao pode ser antecipado: os arquivos exatos dependem do inventario e a
caracterizacao depende do piloto escolhido.

### Pacote de trabalho A - Inventario somente leitura

**Descricao:** obter acesso Browse por User Token e registrar os 26 blocos reportados pelo
SonarQube, incluindo pares de arquivos, linhas, tamanho e componente.

**Criterios de aceite:**

- todos os 26 blocos e as 735 linhas da baseline estao reconciliados ou a diferenca esta explicada;
- cada bloco possui localizacao verificavel e nenhum arquivo de producao ou teste muda;
- o token nao aparece em arquivo, diff, log versionado ou comando persistido.

**Verificacao:** Web API da instancia local, `git status --short`, busca de segredos e revisao do
inventario.

**Arquivos previstos:** `tasks/inventario-duplicacao-fase-13.md` e governanca. **Escopo:** S,
1-3 arquivos documentais.

### Pacote de trabalho B - Classificacao e escolha do piloto

**Descricao:** classificar cada bloco como removivel local, intencional de contrato, intencional
de dominio, tecnico compartilhavel ou duvidoso. Selecionar o menor candidato local que ja possua
boa protecao comportamental.

**Criterios de aceite:**

- os 26 blocos possuem ownership e justificativa;
- o piloto pertence a um unico dominio e uma unica borda, sem `arquitetura.excecao.dto`;
- arquivos de producao e testes provaveis estao listados, limitados a cinco nao mecanicos.

**Verificacao:** regras do documento canonico, imports atuais, ArchUnit existente e revisao humana
no C13-INVENTARIO.

**Arquivos previstos:** inventario, `tasks/plan.md` e `tasks/todo.md`. Nenhum `src/main`.
**Escopo:** S.

### Pacote de trabalho C - Caracterizacao do piloto

**Descricao:** identificar a protecao existente do bloco escolhido e adicionar somente a lacuna
necessaria antes da refatoracao. Testes de caracterizacao podem iniciar verdes quando usam oraculo
independente, conforme a estrategia historica do projeto.

**Criterios de aceite:**

- sucesso, nulos, erros e efeitos relevantes do candidato estao protegidos;
- o teste nao deriva o esperado do mesmo mapper, DTO ou helper da implementacao;
- nenhuma classe de producao muda neste pacote.

**Verificacao:** testes focados, ArchUnit quando aplicavel e `git diff --check`.

**Arquivos previstos:** somente testes do dominio/borda escolhidos e governanca, definidos depois
do C13-INVENTARIO. **Escopo:** S/M, no maximo cinco arquivos.

### Pacote de trabalho D - Refatoracao de um bloco

**Descricao:** eliminar o bloco selecionado com a menor mudanca que expresse um conceito real.
Preferir helper privado ou consolidacao local; nao criar hierarquia, service, adapter ou modelo
generico para atender a metrica.

**Criterios de aceite:**

- a duplicacao deixa de existir sem atravessar dominio ou borda;
- comportamento, ordem de efeitos, erros e nulabilidade permanecem identicos;
- o diff de producao toca no maximo cinco arquivos e nao altera contrato ou configuracao.

**Verificacao:** testes focados do pacote C, ArchUnit e revisao multi-eixo do diff.

**Arquivos previstos:** definidos pelo C13-INVENTARIO; nao podem incluir arquivos fora do piloto
sem novo GO. **Escopo:** S/M.

### Pacote de trabalho E - Verificacao e encerramento

**Descricao:** executar a verificacao completa, repetir o SonarQube com plugin Maven fixado e
comparar valores absolutos e percentuais com a baseline.

**Criterios de aceite:**

- `mvn -q clean test`, build e ArchUnit passam sem teste removido ou ignorado;
- linhas duplicadas ficam abaixo de 735 e blocos abaixo de 26;
- cobertura nao cai abaixo de 80,0% geral, 88,4% de linhas e 60,0% de branches;
- complexidade cognitiva nao supera 837 e variacao ciclomatica possui explicacao por arquivo;
- contratos HTTP/MTR, simulador, FT, erros e observabilidade permanecem equivalentes;
- diff, segredos, documentacao e estado Git sao revisados antes do C13.

**Verificacao:** suite limpa, testes contratuais afetados, ArchUnit, SonarQube,
`git diff --check`, busca de segredos e revisao humana. **Escopo:** S documental.

### Checkpoints

| Checkpoint | Autoriza | Nao autoriza |
|---|---|---|
| C13-PLAN | decompor este plano em tasks executaveis | inventario ou producao |
| C13-TASKS | executar somente inventario e classificacao | escolher ou editar piloto |
| C13-INVENTARIO | caracterizar e refatorar o unico piloto aprovado | segundo bloco ou escopo cruzado |
| C13 | aceitar ou rejeitar o resultado do piloto | continuar automaticamente |

### Paralelizacao

Nao ha paralelizacao recomendada. Inventario, classificacao, caracterizacao, refatoracao e medicao
formam uma cadeia de dependencia. Executa-los em paralelo aumentaria o risco de escolher um helper
antes de estabelecer ownership.

### Estrategia de commits

Se o usuario solicitar commits, manter save points separados: governanca/especificacao, inventario,
caracterizacao, refatoracao e evidencia final. Commit e push continuam fora do fluxo automatico e
exigem solicitacao explicita.

### Riscos e mitigacoes da Fase 13

| Risco | Impacto | Mitigacao |
|---|---|---|
| Remover duplicacao intencional de borda | Alto | classificar ownership e preservar DTOs independentes |
| Criar helper cross-domain | Alto | piloto restrito ao mesmo dominio/borda e ArchUnit verde |
| Apenas deslocar complexidade | Alto | exigir conceito nomeado e comparar complexidade antes/depois |
| Alterar erro, FT ou observabilidade | Alto | caracterizacao focada e contratos existentes antes da edicao |
| Percentual Sonar arredondado ocultar resultado | Medio | comparar tambem linhas e blocos absolutos |
| Queda de cobertura por reorganizacao | Medio | pisos da baseline e JaCoCo regenerado |
| Vazamento de User Token | Alto | variavel de ambiente em memoria, busca de segredos e revogacao |
| Crescimento do piloto | Medio | maximo de cinco arquivos e novo GO para qualquer expansao |

### Fora de escopo

- segundo bloco duplicado sem novo GO;
- compartilhamento de DTOs ou modelos entre bordas ou dominios;
- alteracao de `arquitetura.excecao.dto`;
- novos endpoints, workflows, upload, cache de SAS ou Quarkus Flow;
- mudanca funcional, de contrato, configuracao, FT, simulador ou observabilidade;
- aumento de cobertura como objetivo independente;
- nova dependencia ou ferramenta de clone detection no build.

## Fase 14 - Remediacao segura de issues HIGH do SonarQube

**Status:** especificacao, plano e tasks aprovados no C14-SPEC, C14-PLAN e C14-TASKS; somente
baseline e primeiro lote S1192 autorizados; demais lotes bloqueados.

**Documento de especificacao:** `especificacao-fase-14-sonar-high.md`.

### Baseline e decisoes do plano

- 189 issues de impacto HIGH: 169 `java:S1192`, 9 `java:S1948`, 9 `java:S1186` e 2 `java:S3776`;
- S1192 sera tratado com constantes locais por classe, sem catalogo compartilhado;
- S1186 sera alterado somente quando o no-op for comprovadamente intencional;
- S3776 sera reduzido com metodos privados locais, preservando ordem de efeitos;
- S1948 exige caracterizacao de serializacao; `arquitetura.excecao.dto` nao muda sem GO especifico;
- cada lote toca no maximo cinco arquivos e para para revisao humana;
- C13 continua documental e seu NO-GO de extracao de clones nao e reaberto por esta fase.

### Grafo de dependencias

```text
C14-SPEC GO
    -> C14-PLAN GO
        -> decomposicao das tasks
            -> C14-TASKS GO
                -> baseline de issues e caracterizacao
                    -> Lote A: S1192 local
                        -> checkpoint humano
                    -> Lote B: S1186 intencional
                        -> checkpoint humano
                    -> Lote C: S3776 local
                        -> checkpoint humano
                    -> Lote D: S1948 serializacao
                        -> C14 final
```

### Pacote A - Baseline e caracterizacao

**Descricao:** registrar issue keys, regras, arquivos, linhas, testes e sinais observaveis antes de
qualquer mudanca.

**Criterios de aceite:** os 189 issues ficam reconciliados; contratos e cobertura baseline ficam
registrados; nenhum arquivo de producao muda.

**Verificacao:** API de issues, `mvn -q clean test`, ArchUnit e manifestos de observabilidade.
**Escopo:** S, documental e testes existentes.

### Pacote B - S1192 local

**Descricao:** corrigir uma classe por vez, iniciando por uma classe de observabilidade ou adapter
com strings repetidas, extraindo constantes privadas locais.

**Criterios de aceite:** valores literais permanecem identicos; logs, spans, atributos e contratos
nao mudam; nenhum utilitario cross-domain e criado.

**Verificacao:** testes focados da classe, contratos de observabilidade, ArchUnit e diff. Cada
incremento deve tocar no maximo cinco arquivos.

### Pacote C - S1186 intencional

**Descricao:** revisar os nove metodos vazios de fixtures e handlers; documentar no-ops reais sem
alterar setup, teardown ou comportamento de testes.

**Criterios de aceite:** nenhum metodo funcional e silenciado; comentarios explicam o motivo; suite
e testes de contrato continuam verdes.

**Verificacao:** testes focados, suite limpa e diff de testes separado de producao.

### Pacote D - S3776

**Descricao:** reduzir os dois metodos acima do limite extraindo captura de atributos e eventos de
observabilidade para helpers privados locais.

**Criterios de aceite:** complexidade cognitiva fica no limite; ordem de spans, logs, nulos e falhas
permanece equivalente; nenhum comportamento e deslocado para helper generico.

**Verificacao:** contratos de observabilidade, testes da capacidade, ArchUnit e suite limpa.

### Pacote E - S1948

**Descricao:** confirmar se excecoes sao serializadas fora do processo e preservar o grafo de erro
quando forem. A decisao de alterar o DTO tecnico compartilhado exige GO arquitetural separado.

**Criterios de aceite:** round-trip, status, erro, tipo, mensagens e causa permanecem equivalentes;
nenhuma perda e escondida com `transient`.

**Verificacao:** teste de serializacao, testes de traducao de erro, contratos MTR/REST, ArchUnit e
suite limpa.

### Pacote F - Verificacao e encerramento

**Descricao:** repetir scanner Maven fixado e comparar issues, cobertura, complexidades e CPD com a
baseline.

**Criterios de aceite:** issues tratados ou justificados com evidencia; cobertura minima preservada;
Quality Gate e suite verdes; nenhum segredo no diff.

**Verificacao:** `mvn -q clean test`, scanner, API de issues, `git diff --check` e revisao humana.

### Checkpoints

| Checkpoint | Autoriza | Nao autoriza |
|---|---|---|
| C14-PLAN | decompor tasks | alterar codigo ou executar lote |
| C14-TASKS | baseline e primeiro lote aprovado | alterar lote seguinte sem revisao |
| C14-A/B/C/D | prosseguir somente apos evidencia do lote anterior | ampliar escopo ou compartilhar contratos |
| C14 | aceitar resultado final | continuar automaticamente |

### Riscos e mitigacoes

| Risco | Impacto | Mitigacao |
|---|---|---|
| constante altera chave de observabilidade | Alto | preservar valor literal e testar spans/logs |
| `transient` perde erro | Alto | caracterizar e preferir serializacao completa |
| fixture vazio deixa de exercer guardrail | Alto | testes ArchUnit e comentarios sem lancar excecao |
| helper desloca complexidade | Medio | limite por lote e comparar complexidade por metodo |
| remediacao mistura C13 | Alto | branch Fase 14 e checkpoints independentes |

## Fase 15 - Fechamento de qualidade SonarQube

**Status:** concluida em 2026-07-16 no commit `1f0dca0`. A continuidade sem novas confirmações foi
autorizada pelo usuario no C15-CHECKPOINT-QUALITY-01; suite limpa e scanner final ficaram verdes,
com Quality Gate OK, zero HIGH, zero new violations, zero BLOCKER e warning de blame eliminado.

**Objetivo:** eliminar o blocker, reduzir as 175 HIGH e resolver o warning de blame, mantendo
cobertura >=80%, duplicação e complexidade sem regressão.

### Ordem

1. Congelar baseline Sonar/Git/JaCoCo e listar os dez arquivos sem blame.
2. Corrigir o blocker S2699 com asserção comportamental.
3. Reexecutar testes e scanner com XML JaCoCo explícito.
4. Diagnosticar o SCM/blame: confirmar `.git`, clone completo, ausência de shallow/partial clone,
   submódulos, line endings e arquivos rastreados; preservar SCM habilitado.
5. Classificar HIGH por regra/componente e aprovar lotes pequenos.
6. Corrigir lotes com caracterização e checkpoints humanos.
7. Reanalisar e fechar comparando os indicadores.

### Checkpoints

- **C15-A:** baseline, blocker e warning diagnosticados.
- **C15-B:** primeiro lote HIGH validado.
- **C15-C:** lotes restantes sem regressão.
- **C15-D:** análise final e zero BLOCKER.

### Resultado final

- 82 issues HIGH `java:S1192` no checkpoint de retomada reduzidas a zero.
- Cobertura 80,0%, duplicacao 5,9%, complexidade 1356 e complexidade cognitiva 829.
- Quality Gate OK e zero issues no periodo de codigo novo.
- SCM revision `1f0dca054eaf97b0215e3c50e697fa98b886e983`, com blame publicado sem warning.
- Nenhum endpoint, contrato, fronteira arquitetural, fault tolerance, simulador ou chave de
  observabilidade alterado.

### Fora de escopo

- novos endpoints, workflows, upload, cache de SAS ou Quarkus Flow;
- compartilhamento de DTOs REST/MTR/simulador;
- mudanca de contrato, observabilidade, fault tolerance ou simulador;
- fechamento artificial de issues sem correcao ou justificativa aprovada;
- commit, push ou alteracao de `src/main` antes dos gates seguintes.

## Fase 16 - Issues CRITICAL do Sonar oficial

**Status:** plano aprovado em 2026-07-17 no C16-PLAN. A execução fica restrita às issues com
`severity=CRITICAL` no arquivo `doc/sonar/sonar-issues-staging.csv` e deve seguir os lotes e
critérios detalhados em `../doc/sonar/plano-resolucao-issues-criticas-staging.md`.

**Objetivo:** eliminar 74 `java:S1192` acionáveis em 31 arquivos de teste e reconciliar 26 issues
associadas a três paths ausentes, sem alterar produção, contratos, cobertura, observabilidade ou
fronteiras DDD.

### Ordem autorizada

1. Tasks 16.0 a 16.2: reconciliação, ArchUnit, exceções e observabilidade.
2. C16-A: suíte completa e revisão humana obrigatória.
3. Tasks 16.3 a 16.5 somente após GO em C16-A.
4. Após cada Task 16.x: Sonar Docker local com comparação de issues antes/depois.
5. C16-B: suíte completa e JaCoCo.
6. Tasks 16.6 a 16.9 somente após GO em C16-B.
7. C16-C e análise oficial final somente após os respectivos GOs.

### Limites

- não corrigir `MAJOR`, `MINOR` ou `INFO` neste escopo;
- não alterar `src/main/java`, contratos ou configuração de produção;
- não usar supressões, `NOSONAR` ou exclusões do scanner;
- não reintroduzir os três testes legados ausentes;
- não alterar formatos derivados `.ppt`, `.pptx`, `.pdf` ou `.html`.

### Evidencia local do C16-A

- Tasks 16.0 a 16.2 executadas na branch `refactor/sonar-quality-fase-16-baseline` a partir de
  `59952b89c587e07611e285ed7682883135388ed2`.
- Os três paths legados não existem, não possuem histórico Git local e não têm referências no
  código vigente. O CSV não exporta branch nem SHA da análise oficial; a baixa das 26 issues será
  confirmada no scanner oficial.
- Os 23 `java:S1192` do bloco foram tratados com constantes privadas nos seis testes previstos.
- `mvn -q clean test`: 348 testes, zero falhas, zero erros e zero ignorados.
- JaCoCo: 91,12% de instruções, 88,48% de linhas, 60,16% de branches, 96,58% de métodos e
  93,75% de classes; cobertura combinada de linhas e branches em 80,02%.
- Nenhum arquivo de `src/main` foi alterado, nenhum token foi persistido e `git diff --check`
  ficou limpo.
- Sonar Docker local no commit `9acafa2`: analysis
  `9560384d-9d08-42dd-a9de-3b9a7803eb77`, Quality Gate OK, 214 issues antes/depois, zero nova,
  zero `java:S1192`, cobertura 80,0% e duplicação 5,9%.
- C16-A recebeu GO humano em 2026-07-17; Tasks 16.3 a 16.5 estão autorizadas, com parada
  obrigatória no C16-B.

### Evidencia tecnica do C16-B

- Tasks 16.3 a 16.5 concluídas: 26 `java:S1192` tratados nos 11 testes previstos, sem alteração
  em `src/main`.
- `mvn -q clean test`: 348 testes, zero falhas, zero erros e zero ignorados.
- JaCoCo preservado: 91,12% de instruções, 88,48% de linhas, 60,16% de branches, 96,58% de
  métodos e 93,75% de classes; cobertura combinada Sonar de 80,0%.
- Gate Sonar Docker da Task 16.5 no commit `9e8805d`: analysis
  `a9ae4b61-9110-4505-914b-0250983e2098`, Quality Gate OK, 214 issues antes/depois, zero nova,
  zero `java:S1192` e duplicação de 5,9%.
- Nenhum token foi persistido. O C16-B recebeu GO humano em 2026-07-17; as Tasks 16.6 a 16.9
  estão autorizadas em ordem, com parada obrigatória no C16-C.

### Evidencia tecnica para o C16-C - aguarda GO

- Tasks 16.6 a 16.9 concluídas: os 25 `java:S1192` finais foram tratados nos 14 testes previstos,
  sem alteração em `src/main`, dependências ou configuração.
- A reconciliação do CSV oficial contabiliza 100 issues CRITICAL: 74 `java:S1192` acionáveis em
  31 arquivos foram corrigidas localmente; as outras 26 apontam para três paths ausentes, sendo
  7 `java:S1192` e 19 `java:S2696`, e aguardam confirmação de baixa na análise oficial.
- Os testes focados de cada task passaram. A última execução de `mvn -q clean test` registrou 348
  testes, zero falhas, zero erros e zero ignorados.
- JaCoCo preservado: 91,12% de instruções, 88,48% de linhas, 60,16% de branches, 96,58% de
  métodos e 93,75% de classes; cobertura combinada Sonar de 80,0%.
- Gate Sonar Docker da Task 16.9 no commit `a46b465`: analysis
  `35ee26e9-71a3-4370-a7e2-83ba70f57c69`, Compute Engine `SUCCESS`, Quality Gate `OK`, 214
  issues antes/depois, zero nova, zero `java:S1192`, cobertura 80,0% e duplicação 5,9%.
- A revisão multi-eixo não encontrou findings Critical ou Required. Não houve alteração em
  produção, `pom.xml`, properties ou formatos derivados; nenhum token foi persistido.
- A execução está parada no C16-C. A Task 16.10 e a análise oficial permanecem bloqueadas até GO
  humano explícito.
