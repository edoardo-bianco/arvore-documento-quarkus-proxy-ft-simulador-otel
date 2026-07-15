# Plano de implementacao: refatoracao DDD das integracoes atomicas

## Status

- **Planejado:** 2026-07-11
- **Implementacao:** Fases 0 a 8 concluidas; C0, C1, C2, C3, C4, C5, C6, C7 e C8 em GO;
  consolidacao introdutoria encerrada em 2026-07-15
- **Branch de trabalho atual:** `refactor/ddd-fase-8-baseline`
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
