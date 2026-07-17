# Plano de resolução das issues críticas do Sonar oficial

Status: **EM EXECUÇÃO — C16-B aprovado; bloco C16-C autorizado**.

- Fonte exclusiva: `doc/sonar/sonar-issues-staging.csv`.
- Branch de trabalho: `refactor/sonar-quality-fase-16-baseline`.
- Commit-base: `59952b89c587e07611e285ed7682883135388ed2`.
- Data do relatório: issues criadas ou atualizadas até 2026-07-17.

## Objetivo

Resolver somente as issues cujo campo `severity` seja exatamente `CRITICAL` no CSV exportado do
Sonar oficial do projeto, preservando comportamento, contratos, cobertura, observabilidade e as
fronteiras DDD existentes.

Este plano não autoriza correções de issues `MAJOR`, `MINOR` ou `INFO`, nem incorpora as issues do
projeto Sonar local `simtr-hub-local` que não estejam presentes no CSV oficial.

## Inventário reconciliado

| Recorte | Issues | Arquivos | Esforço informado pelo Sonar |
|---|---:|---:|---:|
| Total do CSV | 138 | — | — |
| `CRITICAL` | 100 | 34 | 1.204 min (20h04) |
| `CRITICAL` em arquivos existentes na branch | 74 | 31 | 726 min (12h06) |
| `CRITICAL` em paths ausentes na branch | 26 | 3 | 478 min (7h58) |
| Fora do plano: `MAJOR` | 22 | — | — |
| Fora do plano: `MINOR` | 15 | — | — |
| Fora do plano: `INFO` | 1 | — | — |

### Regras críticas

| Regra | Total no CSV | Em arquivos existentes | Em paths ausentes | Tratamento planejado |
|---|---:|---:|---:|---|
| `java:S1192` — literais duplicados | 81 | 74 | 7 | constantes locais e semanticamente nomeadas nos testes |
| `java:S2696` — escrita em campo `static` por método de instância | 19 | 0 | 19 | reconciliar código residual; não recriar arquivos ausentes |

### Distribuição das 74 issues acionáveis

| Lote lógico | Issues | Arquivos | Esforço Sonar |
|---|---:|---:|---:|
| Arquitetura, guardrails, exceções e observabilidade | 23 | 6 | 270 min |
| Testes e contratos compartilhados | 12 | 5 | 116 min |
| Árvore documental | 14 | 6 | 118 min |
| Conformidade | 9 | 4 | 78 min |
| Dossiê de produto | 9 | 7 | 82 min |
| Gestão de documentos | 7 | 3 | 62 min |

Todas as 74 issues acionáveis estão em `src/test/java`. Portanto, este plano não prevê alteração em
`src/main/java`, contratos públicos ou configuração de produção.

## Decisões de execução

1. O CSV oficial é a fonte de escopo. Uma issue só entra em um lote quando possui
   `severity=CRITICAL` no arquivo fornecido.
2. Constantes de teste permanecem na classe que possui o cenário. Não será criado catálogo global,
   helper compartilhado entre domínios ou abstração de produção apenas para satisfazer `S1192`.
3. Nomes de constantes devem registrar o papel do valor no teste, e não apenas repetir o literal.
4. Quando já existir constante canônica, ela deve ser reutilizada. O caso explícito do CSV é
   `PACOTE_DTO_ERRO_REST` em `ArchUnitProgressivoTest`.
5. Literais contratuais permanecem idênticos: paths, JSON, nomes de campos, códigos, mensagens,
   atributos de spans e eventos de log não podem mudar.
6. Não serão usados `@SuppressWarnings`, exclusões do scanner, `NOSONAR` ou marcação automática de
   falso positivo.
7. Arquivos ausentes não serão reintroduzidos para corrigir issue. Primeiro será reconciliada a
   cópia analisada pelo Sonar oficial.
8. Nenhuma alteração será feita em `.ppt`, `.pptx`, `.pdf` ou `.html`.
9. Após cada task concluída, o projeto `simtr-hub-local` será analisado no Sonar Docker. O gate
   exige Quality Gate `OK`, zero chave de issue nova e zero `java:S1192` nos arquivos do lote. O
   token será fornecido somente por variável de ambiente temporária e nunca será persistido.

## Ordem e dependências

1. Reconciliar o snapshot oficial e os três paths ausentes.
2. Tratar guardrails e contratos arquiteturais, porque protegem os demais lotes.
3. Tratar testes compartilhados e, depois, cada domínio de forma isolada.
4. Executar checkpoints com suíte completa e cobertura após cada dois ou três lotes.
5. Publicar uma análise no Sonar oficial e encerrar somente com zero `CRITICAL`.

## Tasks

### Task 16.0 — Reconciliar os 26 apontamentos de paths ausentes

**Descrição:** confirmar qual revisão e working tree foram analisados no Sonar oficial. Os três
paths abaixo não existem nem possuem histórico na branch atual:

- `src/test/java/br/gov/caixa/simtr/dossie/DossieContractTest.java` — 10 `S2696`;
- `src/test/java/br/gov/caixa/simtr/dossie/recurso/rest/DossieResourceTest.java` — 9 `S2696`;
- `src/test/java/br/gov/caixa/simtr/dossie/servico/DossieConsultaServicoTest.java` — 7 `S1192`.

**Critérios de aceite:**

- [ ] registrar branch, SHA e data da análise oficial que gerou o CSV;
- [ ] confirmar se os três arquivos são resíduos da versão inicial na máquina de trabalho;
- [ ] antes de removê-los, comprovar com `rg` que nenhuma produção ou teste vigente depende deles;
- [ ] não reintroduzir package ou teste legado na branch atual;
- [ ] após sincronização e nova análise, as 26 issues deixam de existir por ausência dos arquivos.

**Verificação:**

- [ ] `git status --short` contém somente mudanças conhecidas;
- [ ] `rg -n "br\.gov\.caixa\.simtr\.dossie" src/main src/test` não encontra dependência vigente
      não justificada;
- [ ] `mvn -q clean test` passa após a limpeza da cópia oficial;
- [ ] análise oficial não lista os três paths.

**Dependências:** nenhuma.

**Escopo estimado:** pequeno; reconciliação e limpeza da cópia, sem implementação substituta.

### Task 16.1 — Eliminar 11 `S1192` dos guardrails ArchUnit

**Arquivo:**

- `src/test/java/br/gov/caixa/simtr/hub/arquitetura/guardrails/ArchUnitProgressivoTest.java`.

**Descrição:** declarar constantes locais para padrões de package e nomes dos quatro domínios,
reutilizar `PACOTE_DTO_ERRO_REST` e preservar integralmente a semântica das regras negativas e
positivas.

**Critérios de aceite:**

- [ ] as 11 issues `S1192` do arquivo são eliminadas;
- [ ] nenhum pattern de package muda de valor;
- [ ] nenhuma regra ArchUnit é removida, relaxada ou marcada com `allowEmptyShould` adicional.

**Verificação:**

- [ ] `mvn -q -Dtest=ArchUnitProgressivoTest test` passa;
- [ ] scanner oficial não reporta `CRITICAL` no arquivo.

**Dependências:** Task 16.0.

**Escopo estimado:** pequeno, 1 arquivo.

### Task 16.2 — Tratar 12 `S1192` de exceções e observabilidade

**Arquivos:**

- `arquitetura/excecao/ExcecoesHighSerializacaoCaracterizacaoTest.java` — 5;
- `arquitetura/excecao/ExceptionMapperTest.java` — 2;
- `arquitetura/observabilidade/ObservabilidadeSpansContratoTest.java` — 2;
- `arquitetura/observabilidade/ObservabilityLogTest.java` — 2;
- `arquitetura/observabilidade/RestClientObservabilityFilterTest.java` — 1.

Todos os paths são relativos a `src/test/java/br/gov/caixa/simtr/hub/`.

**Descrição:** extrair somente constantes locais para payloads de erro, nomes de sistemas, campos
de log e atributos de spans. Os valores literais e asserções permanecem idênticos.

**Critérios de aceite:**

- [ ] as 12 issues críticas dos cinco arquivos são eliminadas;
- [ ] serialização, mensagens de erro, mascaramento e atributos observáveis não mudam;
- [ ] não surge helper compartilhado de observabilidade ou erro para uso exclusivo de testes.

**Verificação:**

- [ ] executar os cinco testes focados;
- [ ] executar `git diff --check`;
- [ ] scanner oficial não reporta `CRITICAL` nesses arquivos.

**Dependências:** Task 16.1.

**Escopo estimado:** médio, 5 arquivos.

### Checkpoint C16-A — Guardrails preservados

- [ ] Tasks 16.0 a 16.2 concluídas;
- [ ] 23 issues acionáveis de arquitetura eliminadas;
- [ ] 26 issues de paths ausentes reconciliadas;
- [ ] suíte completa verde;
- [ ] GO humano antes de continuar para contratos e domínios.

### Task 16.3 — Tratar 12 `S1192` dos testes compartilhados

**Arquivos:**

- `TestFixtures.java` — 2;
- `contrato/DossieProdutoErroApiContractTest.java` — 4;
- `contrato/DossieProdutoApiContractTest.java` — 1;
- `contrato/JsonContractAssertions.java` — 1;
- `recurso/ResourceEndpointTest.java` — 4.

Todos os paths são relativos a `src/test/java/br/gov/caixa/simtr/hub/`.

**Descrição:** nomear fixtures e valores contratuais repetidos sem alterar CPF/CNPJ fictícios,
paths, campos JSON, mensagens ou a independência dos oráculos de contrato.

**Critérios de aceite:**

- [ ] as 12 issues críticas dos cinco arquivos são eliminadas;
- [ ] requests e respostas esperadas permanecem byte a byte equivalentes quando contratuais;
- [ ] constantes compartilhadas ficam limitadas à classe que já é fixture canônica.

**Verificação:**

- [ ] executar os testes focados dos cinco arquivos;
- [ ] contratos dos oito endpoints continuam verdes.

**Dependências:** C16-A.

**Escopo estimado:** médio, 5 arquivos.

### Task 16.4 — Tratar 10 `S1192` da árvore documental: modelo e REST

**Arquivos:**

- `arvoredocumento/dominio/modelo/ProcessoParametrizadoModeloTest.java` — 5;
- `arvoredocumento/adaptador/entrada/rest/v1/ProcessoParametrizadoRestMapperTest.java` — 4;
- `arvoredocumento/adaptador/entrada/rest/v1/ProcessoResourceQuarkusTest.java` — 1.

**Descrição:** criar constantes locais para códigos, nomes, data fixa, metadados de erro e media
type, mantendo a leitura DAMP dos cenários.

**Critérios de aceite:**

- [ ] as 10 issues críticas dos três arquivos são eliminadas;
- [ ] mapeamento REST, JSON e status HTTP permanecem inalterados.

**Verificação:**

- [ ] executar os três testes focados;
- [ ] contrato HTTP de processo continua verde.

**Dependências:** Task 16.3.

**Escopo estimado:** médio, 3 arquivos.

### Task 16.5 — Tratar 4 `S1192` da árvore documental: MTR e caracterização

**Arquivos:**

- `arvoredocumento/adaptador/saida/mtr/adapter/ProcessoParametrizadoMtrAdapterTest.java` — 1;
- `arvoredocumento/adaptador/saida/mtr/client/ParametrizacaoProcessoClientTest.java` — 2;
- `arvoredocumento/caracterizacao/ProcessoParametrizadoNovoClientQuarkusTest.java` — 1.

**Critérios de aceite:**

- [ ] as 4 issues críticas são eliminadas com constantes locais;
- [ ] path MTR, classificação de erro e conteúdo do payload permanecem idênticos.

**Verificação:**

- [ ] executar os três testes focados;
- [ ] matriz de erro e fault tolerance de processo continua verde.

**Dependências:** Task 16.4.

**Escopo estimado:** médio, 3 arquivos.

### Checkpoint C16-B — Contratos compartilhados e árvore documental

- [x] Tasks 16.3 a 16.5 concluídas;
- [x] 26 issues críticas deste bloco corrigidas localmente;
- [x] `mvn -q clean test` passa;
- [x] JaCoCo não fica abaixo da baseline registrada;
- [x] GO humano antes dos demais domínios.

### Task 16.6 — Tratar 9 `S1192` de conformidade

**Arquivos:**

- `conformidade/adaptador/entrada/rest/v1/ChecklistRestMapperTest.java` — 5;
- `conformidade/adaptador/entrada/rest/v1/ChecklistResourceQuarkusTest.java` — 1;
- `conformidade/adaptador/saida/mtr/client/ParametrizacaoChecklistClientTest.java` — 2;
- `conformidade/caracterizacao/ChecklistNovoClientQuarkusTest.java` — 1.

**Critérios de aceite:**

- [ ] as 9 issues críticas são eliminadas;
- [ ] apontamentos, metadados MTR, media type, status e mensagens não mudam.

**Verificação:**

- [ ] executar os quatro testes focados;
- [ ] contrato HTTP, MTR, simulador e erros de checklist continuam verdes.

**Dependências:** C16-B.

**Escopo estimado:** médio, 4 arquivos.

### Task 16.7 — Tratar 4 `S1192` nos adapters MTR de dossiê de produto

**Arquivos:**

- `dossieproduto/adaptador/saida/mtr/adapter/CriacaoDossieProdutoMtrAdapterTest.java`;
- `dossieproduto/adaptador/saida/mtr/adapter/DocumentoDossieProdutoMtrAdapterTest.java`;
- `dossieproduto/adaptador/saida/mtr/adapter/FormularioDossieProdutoMtrAdapterTest.java`;
- `dossieproduto/adaptador/saida/mtr/adapter/ValidacaoNegocialDossieProdutoMtrAdapterTest.java`.

**Critérios de aceite:**

- [ ] as 4 issues críticas são eliminadas;
- [ ] identificador do sistema MTR e tradução lossless de erros permanecem inalterados.

**Verificação:**

- [ ] executar os quatro testes focados;
- [ ] nenhuma alteração em código de produção.

**Dependências:** Task 16.6.

**Escopo estimado:** médio, 4 arquivos.

### Task 16.8 — Tratar 5 `S1192` nos contratos MTR de dossiê de produto

**Arquivos:**

- `dossieproduto/integracao/DocumentoDossieProdutoMtrContractTest.java` — 3;
- `dossieproduto/integracao/FormularioDossieProdutoMtrContractTest.java` — 1;
- `dossieproduto/integracao/ValidacaoNegocialDossieProdutoMtrContractTest.java` — 1.

**Critérios de aceite:**

- [ ] as 5 issues críticas são eliminadas;
- [ ] JSON, IDs, media type, paths e wire MTR permanecem idênticos.

**Verificação:**

- [ ] executar os três contratos focados;
- [ ] confirmar igualdade dos payloads literais antes e depois.

**Dependências:** Task 16.7.

**Escopo estimado:** médio, 3 arquivos.

### Task 16.9 — Tratar 7 `S1192` de gestão de documentos

**Arquivos:**

- `gestaodocumento/adaptador/entrada/rest/v1/GestaoDocumentoResourceQuarkusTest.java` — 3;
- `gestaodocumento/adaptador/saida/mtr/client/GestaoDocumentoClientTest.java` — 2;
- `gestaodocumento/caracterizacao/GestaoDocumentoNovoClientQuarkusTest.java` — 2.

**Critérios de aceite:**

- [ ] as 7 issues críticas são eliminadas;
- [ ] media type, campos de credencial, sistema MTR, status e container permanecem idênticos;
- [ ] nenhuma lógica de upload, cache ou renovação de SAS é introduzida.

**Verificação:**

- [ ] executar os três testes focados;
- [ ] contrato de credencial e os dois modos de simulador continuam verdes.

**Dependências:** Task 16.8.

**Escopo estimado:** médio, 3 arquivos.

### Checkpoint C16-C — Todas as 74 issues acionáveis tratadas

- [ ] Tasks 16.6 a 16.9 concluídas;
- [ ] 25 issues críticas deste bloco eliminadas;
- [ ] nenhum arquivo de `src/main/java` alterado;
- [ ] `mvn -q clean test` passa sem testes ignorados;
- [ ] cobertura geral, de linhas e de branches não fica abaixo da baseline;
- [ ] GO humano para a análise oficial final.

### Task 16.10 — Publicar análise final no Sonar oficial

**Descrição:** executar o scanner oficial sobre uma cópia Git limpa da branch, importando o XML do
JaCoCo e registrando revisão, task da análise e Quality Gate.

**Critérios de aceite:**

- [ ] `CRITICAL=0` no Sonar oficial;
- [ ] as 74 `S1192` acionáveis estão fechadas;
- [ ] as 26 issues de paths ausentes não aparecem na análise;
- [ ] nenhuma nova issue crítica foi introduzida;
- [ ] as 38 issues não críticas do CSV inicial permanecem fora deste escopo;
- [ ] Quality Gate aprovado e token não persistido.

**Verificação:**

- [ ] `mvn -q clean test`;
- [ ] relatório `target/jacoco-report/jacoco.xml` existente e importado;
- [ ] `git diff --check`;
- [ ] `git status --short` revisado;
- [ ] consulta ao Sonar oficial por severidade e regra anexada ao checkpoint final.

**Dependências:** C16-C.

**Escopo estimado:** pequeno, sem alteração adicional de código.

## Baseline e limites de não regressão

A execução local da `main` em `59952b8` registrou 348 testes, zero falhas, zero erros e zero
ignorados. O JaCoCo/Sonar registrou 80,0% de cobertura geral, 88,5% de linhas e 60,2% de branches.
Esses valores são guardrails mínimos; antes da implementação deve ser capturada também a baseline
do Sonar oficial, pois o escopo analisado pode ser diferente.

Não são aceitos como solução:

- redução de cobertura ou remoção de teste para diminuir issues;
- alteração de payload, path, mensagem, log, span ou comportamento observado;
- supressão de regra ou exclusão de arquivo no scanner;
- nova abstração compartilhada entre domínios apenas para deduplicar constantes de teste;
- reintrodução dos três arquivos ausentes.

## Riscos e mitigações

| Risco | Impacto | Mitigação |
|---|---|---|
| Sonar oficial analisou working tree com arquivos residuais | Alto | usar checkout limpo e reconciliar os três paths antes de editar |
| Constantes tornam testes menos descritivos | Médio | constantes locais, nomes semânticos e manutenção do estilo DAMP |
| Alteração acidental de literal contratual | Alto | diff focado e comparação de payload/path antes e depois |
| Guardrail ArchUnit é relaxado durante extração | Alto | teste focado e proibição explícita de remover ou flexibilizar regra |
| Escopo cresce para MAJOR/MINOR/INFO | Médio | reconciliar cada lote pelo CSV e aceitar somente `CRITICAL` |
| Scanner não importa cobertura | Alto | validar e informar explicitamente `target/jacoco-report/jacoco.xml` |

## Condição para início

O GO humano para o plano foi registrado em 2026-07-17. Executar somente um lote por vez e parar
nos checkpoints C16-A, C16-B e C16-C.

## Evidências locais do checkpoint C16-A

- Branch: `refactor/sonar-quality-fase-16-baseline`.
- Commit-base: `59952b89c587e07611e285ed7682883135388ed2`.
- O CSV data as issues em 2026-07-17, mas não exporta a branch nem o SHA da análise oficial.
- Os três paths legados não existem na branch, não possuem histórico Git local e não são
  referenciados pelo código vigente. Nenhum arquivo foi removido ou reintroduzido.
- As 11 issues de `ArchUnitProgressivoTest` e as 12 issues de exceções/observabilidade foram
  tratadas com constantes privadas e semanticamente nomeadas.
- Cada literal apontado pelo CSV aparece uma única vez no respectivo arquivo, em sua declaração.
- `ArchUnitProgressivoTest` e os cinco testes focados passaram.
- `mvn -q clean test` passou com 348 testes, zero falhas, zero erros e zero ignorados.
- JaCoCo permaneceu na baseline: 80,02% combinado, 88,48% de linhas e 60,16% de branches.
- Nenhum arquivo de produção foi alterado; `git diff --check` ficou limpo e nenhum token Sonar foi
  persistido.
- Sonar Docker local no commit `9acafa2`: analysis
  `9560384d-9d08-42dd-a9de-3b9a7803eb77`, Quality Gate `OK`, 214 issues antes e depois, zero
  issue nova, zero `java:S1192`, cobertura 80,0% e duplicação 5,9%.
- A eliminação no servidor das 23 issues tratadas e das 26 issues dos paths ausentes permanece
  pendente da análise oficial. Não se declara fechamento remoto antecipadamente.

O C16-A recebeu GO humano em 2026-07-17. As Tasks 16.3 a 16.5 estão autorizadas, com parada
obrigatória no C16-B.

## Evidências do bloco C16-B

### Task 16.3

- Commit: `b19ad9c5f57ac75ebe32612f8aa1667e496f96c8`.
- Os 12 `java:S1192` dos cinco testes compartilhados foram tratados; cada literal do CSV ficou
  com uma única ocorrência no arquivo correspondente.
- Testes focados e `mvn -q clean test` passaram.
- Sonar Docker analysis `0890b302-6e13-491a-8658-4e982278426b`: Quality Gate `OK`, 214 issues
  antes e depois, zero issue nova, zero `java:S1192`, cobertura 80,0% e duplicação 5,9%.
- Nenhum token foi persistido e nenhum arquivo de produção foi alterado.

### Task 16.4

- Commit: `8185c52bdcb08067cd4d2be0976f49804fa347d6`.
- Os 10 `java:S1192` dos três testes de modelo/REST de árvore documental foram tratados; cada
  literal do CSV ficou com uma única ocorrência no arquivo correspondente.
- Testes focados e `mvn -q clean test` passaram.
- Sonar Docker analysis `9f311fb0-aa7b-4197-9d87-80d257b52c3c`: Quality Gate `OK`, 214 issues
  antes e depois, zero issue nova, zero `java:S1192`, cobertura 80,0% e duplicação 5,9%.
- Nenhum token foi persistido e nenhum arquivo de produção foi alterado.

### Task 16.5

- Commit: `9e8805d0bdbac629b6e7c9976dded67bf8bb2637`.
- Os 4 `java:S1192` dos três testes MTR/caracterização foram tratados com constantes privadas;
  o payload JSON e os valores contratuais permaneceram inalterados.
- Os três testes focados passaram. `mvn -q clean test` executou 348 testes, sem falhas, erros ou
  testes ignorados.
- JaCoCo: 91,12% de instruções, 88,48% de linhas, 60,16% de branches, 96,58% de métodos e
  93,75% de classes.
- Sonar Docker analysis `a9ae4b61-9110-4505-914b-0250983e2098`: Quality Gate `OK`, 214 issues
  antes e depois, zero issue nova, zero `java:S1192`, cobertura 80,0% e duplicação 5,9%.
- Nenhum token foi persistido e nenhum arquivo de produção foi alterado.

### Checkpoint C16-B

As Tasks 16.3 a 16.5 corrigiram localmente os 26 `java:S1192` previstos para o bloco. O
fechamento no Sonar oficial permanece pendente da análise oficial e não é antecipado por este
registro. O C16-B recebeu GO humano em 2026-07-17; as Tasks 16.6 a 16.9 estão autorizadas em
ordem, com parada obrigatória no C16-C.
