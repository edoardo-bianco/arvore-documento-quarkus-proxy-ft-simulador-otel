# Plano de implementacao: refatoracao DDD das integracoes atomicas

## Status

- **Planejado:** 2026-07-11
- **Implementacao:** Fase 0 em andamento; Task 0.1 concluida
- **Branch de trabalho:** `refactor/ddd-fase-0-baseline`
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
7. Nenhum teste preexistente pode ser removido, desabilitado ou substituido sem evidencia e GO
   humano registrados.

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

### Checkpoint C2.1 - Criacao

- [ ] Evidencias de todas as bordas e matriz FT anexadas.
- [ ] ArchUnit da capacidade ativo; suite/build verdes.
- [ ] GO humano registrado antes de formulario.

### Subfase 2.2 - `AtualizarFormularioDossieProduto`

#### Task 2.2a - Caracterizar formulario

**Criterios de aceite:** HTTP/JSON/validacao/OpenAPI, wire MTR, simulador, erros, observabilidade,
configuracao e matriz FT estao congelados, incluindo listas e nulos atuais.

**Verificacao:** testes focados contra o legado.

**Dependencias:** C2.1. **Escopo:** M.

#### Task 2.2b - Criar nucleo do formulario

**Criterios de aceite:** tipos semanticos, portas e caso de uso sao framework-free salvo `Uni` na
aplicacao; nenhuma imutabilidade/regra funcional nova e introduzida.

**Verificacao:** teste unitario e ArchUnit.

**Dependencias:** 2.2a. **Escopo:** M.

#### Task 2.2c - Criar borda MTR do formulario

**Criterios de aceite:** DTO/mapper exclusivos preservam nulabilidade e wire; erro/FT mantem ordem e
classificacao atuais.

**Verificacao:** stub MTR, erros e matriz FT.

**Dependencias:** 2.2b. **Escopo:** M, subdividir tipos aninhados se necessario.

#### Task 2.2d - Criar borda simulador do formulario

**Criterios de aceite:** fixture e modelos proprios; properties e comportamento atual preservados.

**Verificacao:** simulador ligado/desligado e bootstrap CDI.

**Dependencias:** 2.2b. **Escopo:** S/M.

#### Task 2.2e - Migrar borda REST do formulario

**Criterios de aceite:** path/status/JSON/validacao/OpenAPI/observabilidade equivalentes; Resource
usa somente porta de entrada.

**Verificacao:** HTTP, equivalencia completa e suite.

**Dependencias:** 2.2c e 2.2d. **Escopo:** M.

### Checkpoint C2.2 - Formulario

- [ ] Nulos/listas e validacao permanecem equivalentes.
- [ ] Todas as bordas, ArchUnit, suite e build verdes.
- [ ] GO humano registrado antes de documento.

### Subfase 2.3 - `IncluirDocumentoDossieProduto`

#### Task 2.3a - Caracterizar inclusao de documento

**Criterios de aceite:** contrato publico, OpenAPI, MTR v2, simulador, erros, observabilidade,
configuracao e matriz FT estao congelados, incluindo nulos aceitos.

**Verificacao:** testes focados contra o legado.

**Dependencias:** C2.2. **Escopo:** M.

#### Task 2.3b - Criar nucleo da inclusao

**Criterios de aceite:** tipos, portas e caso de uso nao carregam Jackson/Jakarta; ids de documento
e instancia sao resultados internos sem nomes de protocolo.

**Verificacao:** teste unitario e ArchUnit.

**Dependencias:** 2.3a. **Escopo:** M.

#### Task 2.3c - Criar borda MTR v2

**Criterios de aceite:** DTO/mapper v2 exclusivos; `id_documento`/`id_instancia_documento`, erro e
matriz FT permanecem equivalentes.

**Verificacao:** stub MTR, erros e FT.

**Dependencias:** 2.3b. **Escopo:** M, subdividir arvore DTO em grupos de ate cinco arquivos.

#### Task 2.3d - Criar borda simulador da inclusao

**Criterios de aceite:** JSON snake_case e lido por DTO proprio; selecao/configuracao preservadas.

**Verificacao:** fixture real, simulador ligado/desligado e CDI.

**Dependencias:** 2.3b. **Escopo:** M.

#### Task 2.3e - Migrar borda REST da inclusao

**Criterios de aceite:** request/response explicitos; path/status/JSON/validacao/OpenAPI/logs iguais;
Resource usa a porta de entrada.

**Verificacao:** HTTP, equivalencia e suite.

**Dependencias:** 2.3c e 2.3d. **Escopo:** M.

### Checkpoint C2.3 - Documento

- [ ] Contratos publico, MTR v2 e simulador equivalentes.
- [ ] Todas as bordas, ArchUnit, suite e build verdes.
- [ ] GO humano registrado antes de validacao negocial.

### Subfase 2.4 - `RegistrarValidacaoNegocialDossieProduto`

#### Task 2.4a - Caracterizar validacao negocial

**Criterios de aceite:** HTTP 200 sem corpo, JSON/validacao/OpenAPI, MTR, simulador, erros,
observabilidade, configuracao e matriz FT estao congelados.

**Verificacao:** testes positivos/negativos contra o legado.

**Dependencias:** C2.3. **Escopo:** M.

#### Task 2.4b - Criar nucleo da validacao

**Criterios de aceite:** tipos, portas e caso de uso nao importam DTOs/frameworks; nulabilidade atual
e preservada sem inventar invariantes.

**Verificacao:** teste unitario e ArchUnit.

**Dependencias:** 2.4a. **Escopo:** M.

#### Task 2.4c - Criar borda MTR da validacao

**Criterios de aceite:** arvore de DTOs/mapper exclusiva; wire, erros e ordem FT equivalentes.

**Verificacao:** stub MTR, erros e matriz FT.

**Dependencias:** 2.4b. **Escopo:** M, subdividir DTOs aninhados em grupos de ate cinco arquivos.

#### Task 2.4d - Criar borda simulador da validacao

**Criterios de aceite:** fixture usa DTO/mapper proprio; properties e retorno atuais preservados.

**Verificacao:** simulador ligado/desligado e CDI.

**Dependencias:** 2.4b. **Escopo:** M.

#### Task 2.4e - Migrar borda REST da validacao

**Criterios de aceite:** `List<@Valid T>` remove warning sem alterar mensagens/paths; contrato,
OpenAPI e observabilidade permanecem iguais.

**Verificacao:** HTTP positivo/negativo, equivalencia e suite.

**Dependencias:** 2.4c e 2.4d. **Escopo:** M.

### Checkpoint C2.4 - Validacao negocial

- [ ] Validacao cascata equivalente e sem uso depreciado na capacidade.
- [ ] Todas as bordas, ArchUnit, suite e build verdes.
- [ ] GO humano registrado antes da consolidacao.

### Task 2.5 - Consolidar `dossieproduto`

**Descricao:** remover fachada, service, mapper e DTOs legados que nao possuem mais referencias.

**Criterios de aceite:** todas as cinco capacidades usam a arquitetura alvo; propriedades existentes
continuam validas; nenhuma classe morta permanece.

**Verificacao:** `rg` de imports legados, ArchUnit e `mvn -q test`.

**Dependencias:** C2.4.

**Escopo estimado:** M.

### Checkpoint C2 - GO/NO-GO de `dossieproduto`

- [ ] Cinco capacidades atomicas migradas e verdes.
- [ ] Nao existe orquestracao ou endpoint novo.
- [ ] Retry de operacoes mutaveis continua documentado como risco, nao corrigido silenciosamente.
- [ ] Contrato publico e MTR equivalentes ao baseline.
- [ ] Revisao humana autoriza migrar os outros dominios.

## Fase 3 - `arvoredocumento`

### Task 3.1 - Caracterizar `ConsultarProcessoParametrizado`

**Criterios de aceite:** HTTP/JSON/validacao/OpenAPI, wire MTR, simulador, erros, nulabilidade,
observabilidade, properties/profiles e matriz FT estao no manifesto.

**Verificacao:** testes focados contra `parametrizacao` legado.

**Dependencias:** C2. **Escopo:** M.

### Task 3.2 - Criar nucleo da consulta de processo

**Criterios de aceite:** modelo semantico, porta de entrada, porta de saida e caso de uso pertencem a
`arvoredocumento` e nao importam `parametrizacao` ou adapters.

**Verificacao:** teste unitario e ArchUnit.

**Dependencias:** 3.1. **Escopo:** M; subdividir a arvore de modelo por agregado de leitura.

### Task 3.3 - Criar borda MTR de processo

**Criterios de aceite:** DTO/mapper/client/adapter ficam sob `arvoredocumento.adaptador.saida.mtr`;
wire, erros e ordem FT permanecem iguais.

**Verificacao:** stub MTR, erros, configuracao e matriz FT.

**Dependencias:** 3.2. **Escopo:** M, grupos de ate cinco arquivos.

### Task 3.4 - Criar borda simulador de processo

**Criterios de aceite:** fixture usa DTO/mapper proprio; properties atuais selecionam o adapter sem
condicional no caso de uso.

**Verificacao:** fixture real, simulador ligado/desligado e CDI.

**Dependencias:** 3.2. **Escopo:** M.

### Task 3.5 - Migrar borda REST de processo

**Criterios de aceite:** path atual, JSON, validacao, OpenAPI e observabilidade sao equivalentes;
Resource chama somente a porta de entrada de `arvoredocumento`.

**Verificacao:** HTTP, equivalencia ponta a ponta e suite.

**Dependencias:** 3.3 e 3.4. **Escopo:** M.

### Task 3.6 - Ativar guardrails e remover legado de processo

**Criterios de aceite:** ArchUnit proibe dependencias internas para `parametrizacao`/outros dominios;
`rg` prova ausencia de referencias antes de remover artefatos de processo.

**Verificacao:** `rg`, ArchUnit, build e suite.

**Dependencias:** 3.5. **Escopo:** M.

### Checkpoint C3

- [ ] Consulta de processo pertence integralmente a `arvoredocumento`.
- [ ] Nao existe calculo de arvore ou IA nesta fase.
- [ ] Manifesto comprova OpenAPI, config/profiles, wire, FT, simulador, erros e observabilidade.
- [ ] Suite, build e ArchUnit estao verdes; GO humano registrado.

## Fase 4 - `conformidade`

### Task 4.1 - Caracterizar `ConsultarChecklist`

**Criterios de aceite:** HTTP/JSON/validacao/OpenAPI, wire MTR, simulador, erros,
identificador/versao, observabilidade, configuracao e matriz FT estao no manifesto.

**Verificacao:** testes focados contra `parametrizacao` legado.

**Dependencias:** C3. **Escopo:** M.

### Task 4.2 - Criar nucleo da consulta de checklist

**Criterios de aceite:** modelo, portas e caso de uso pertencem a `conformidade`, sem compartilhar
modelo de processo ou importar adapters.

**Verificacao:** teste unitario e ArchUnit.

**Dependencias:** 4.1. **Escopo:** M.

### Task 4.3 - Criar borda MTR de checklist

**Criterios de aceite:** DTO/mapper/client/adapter exclusivos preservam wire, erros e ordem FT.

**Verificacao:** stub MTR, erros, configuracao e matriz FT.

**Dependencias:** 4.2. **Escopo:** M, grupos de ate cinco arquivos.

### Task 4.4 - Criar borda simulador de checklist

**Criterios de aceite:** fixture usa DTO/mapper proprio; properties atuais sao preservadas.

**Verificacao:** fixture, simulador ligado/desligado e CDI.

**Dependencias:** 4.2. **Escopo:** M.

### Task 4.5 - Migrar borda REST de checklist

**Criterios de aceite:** path, JSON, validacao, OpenAPI e observabilidade equivalentes; Resource usa
somente a porta de entrada de `conformidade`.

**Verificacao:** HTTP, equivalencia ponta a ponta e suite.

**Dependencias:** 4.3 e 4.4. **Escopo:** M.

### Task 4.6 - Ativar guardrails e remover legado de checklist

**Criterios de aceite:** ArchUnit proibe dependencias internas para `parametrizacao`/outros dominios;
`rg` prova ausencia de referencias antes da remocao.

**Verificacao:** `rg`, ArchUnit, build e suite.

**Dependencias:** 4.5. **Escopo:** M.

### Checkpoint C4

- [ ] Consulta de checklist pertence integralmente a `conformidade`.
- [ ] Nao existe analise documental ou workflow nesta fase.
- [ ] Manifesto comprova OpenAPI, config/profiles, wire, FT, simulador, erros e observabilidade.
- [ ] Suite, build e ArchUnit estao verdes; GO humano registrado.

## Fase 5 - `gestaodocumento`

### Task 5.1 - Caracterizar `ObterCredencialContainer`

**Criterios de aceite:** HTTP/JSON/OpenAPI, wire MTR, simulador, erros, expiracao, observabilidade,
configuracao e matriz FT estao no manifesto sem assumir duracao fixa.

**Verificacao:** testes focados contra o legado.

**Dependencias:** C4. **Escopo:** M.

### Task 5.2 - Criar nucleo de credencial

**Criterios de aceite:** modelo, portas e caso de uso representam somente obtencao de credencial;
nao importam Azure SDK, cache ou adapters.

**Verificacao:** teste unitario e ArchUnit.

**Dependencias:** 5.1. **Escopo:** S/M.

### Task 5.3 - Criar borda MTR de credencial

**Criterios de aceite:** DTO/mapper/client/adapter preservam wire, expiracao, erros e ordem FT.

**Verificacao:** stub MTR, erros, configuracao e matriz FT.

**Dependencias:** 5.2. **Escopo:** M.

### Task 5.4 - Criar borda simulador de credencial

**Criterios de aceite:** fixture usa DTO/mapper proprio e properties atuais sao preservadas.

**Verificacao:** simulador ligado/desligado e CDI.

**Dependencias:** 5.2. **Escopo:** S/M.

### Task 5.5 - Migrar borda REST de credencial

**Criterios de aceite:** path, JSON, OpenAPI e observabilidade equivalentes; Resource usa somente a
porta de entrada e devolve expiracao contratual.

**Verificacao:** HTTP, equivalencia ponta a ponta e suite.

**Dependencias:** 5.3 e 5.4. **Escopo:** S/M.

### Task 5.6 - Provar limites de escopo

**Criterios de aceite:** ArchUnit/testes impedem Azure Blob SDK, cache/renovacao de SAS e upload no
nucleo; artefatos legados so saem depois de `rg` sem referencias.

**Verificacao:** `rg`, ArchUnit, build e suite.

**Dependencias:** 5.5. **Escopo:** S/M.

### Checkpoint C5

- [ ] Hub apenas obtem e devolve a credencial.
- [ ] Nao existe cache, renovacao ou upload.
- [ ] Manifesto comprova OpenAPI, config/profiles, wire, FT, simulador, erros e observabilidade.
- [ ] Suite, build e ArchUnit estao verdes; GO humano registrado.

## Fase 6 - Consolidacao final

### Task 6.1 - Remover o dominio legado `parametrizacao`

**Criterios de aceite:** `rg` prova ausencia de imports/referencias de producao, teste, config e
documentacao ativa; somente entao o package e removido.

**Verificacao:** `rg`, build e suite.

**Dependencias:** C5. **Escopo:** S.

### Task 6.2 - Endurecer ArchUnit para todo o codigo migrado

**Criterios de aceite:** regras finais cobrem dependencia por camada, DTOs por borda, excecao formal
do erro REST, REST Clients, API publica de aplicacao e imports cross-domain apenas em adapters ACL.

**Verificacao:** ArchUnit inclui provas negativas controladas e suite verde.

**Dependencias:** 6.1. **Escopo:** M.

### Task 6.3 - Executar verificacao de equivalencia completa

**Criterios de aceite:** manifesto comprova todos os contratos; nenhum teste baseline foi removido ou
desabilitado sem GO; OpenAPI, configs/profiles, wire, FT, simulador, erros e observabilidade batem.

**Verificacao:** `mvn -q test`, build, testes ponta a ponta locais e revisao do diff.

**Dependencias:** 6.2. **Escopo:** M.

### Task 6.4 - Atualizar documentacao de retomada

**Criterios de aceite:** packages finais, desvios, dividas e proximo checkpoint estao documentados;
`AGENTS.md`, plano e checklist apontam para estado concluido sem instrucoes obsoletas.

**Verificacao:** links locais, busca por documentos substituidos e revisao humana.

**Dependencias:** 6.3. **Escopo:** S.

### Checkpoint C6 - Conclusao

- [ ] Oito capacidades existentes migradas.
- [ ] Nenhum comportamento externo mudou sem decisao explicita.
- [ ] Nenhum endpoint, workflow ou upload novo foi implementado.
- [ ] `mvn -q test` e build passam sem testes desabilitados.
- [ ] ArchUnit protege as fronteiras finais.
- [ ] Revisao humana confirma a conclusao.

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
