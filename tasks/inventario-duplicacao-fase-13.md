# Inventario de duplicacao - Fase 13

## Status

- **Estado:** Task 13.1 concluida; Task 13.2 concluida; C13-INVENTARIO pendente
- **Data da consulta:** 2026-07-16
- **Projeto SonarQube:** `simtr-hub-local`
- **Servidor:** SonarQube local 26.7
- **Escopo autorizado:** consulta somente leitura; nenhuma alteracao em producao ou testes

## Baseline revalidada

A Web API confirmou novamente as metricas de projeto da analise usada na especificacao:

| Metrica | Valor |
|---|---:|
| Cobertura geral | 80,0% |
| Cobertura de linhas | 88,4% |
| Cobertura de branches | 60,0% |
| Densidade de duplicacao | 6,0% |
| Linhas duplicadas | 735 |
| Blocos duplicados | 26 |
| Complexidade ciclomatica | 1.352 |
| Complexidade cognitiva | 837 |
| Linhas de codigo | 10.668 |

## Evidencia de acesso

- `GET /api/authentication/validate`: HTTP 200, token valido;
- `GET /api/measures/component`: HTTP 200, metricas acima reconciliadas;
- `GET /api/measures/component_tree`: HTTP 403, sem permissao para listar arquivos;
- o token fornecido era de analise de projeto, nao um User Token com permissao Browse;
- a credencial foi removida da memoria depois da verificacao e nao foi registrada neste arquivo;
- `target/sonar/report-task.txt` identifica a analise, mas o workspace nao preserva
  `.scannerwork/scanner-report` nem outro artefato local com os blocos detalhados.

## Reconciliacao dos 26 blocos

Os 26 blocos e as 735 linhas foram reconciliados com `api/measures/component_tree` e
`api/duplications/show`. Os grupos abaixo sao deduplicados por conjunto de arquivos e intervalo;
as ocorrencias listadas representam todos os blocos contados pelo SonarQube.

### Grupo 1 - excecoes de erro MTR, 8 ocorrencias, 214 linhas

**Ownership:** cada excecao pertence ao adapter MTR do dominio e da operacao indicada no nome.
**Classificacao:** intencional de contrato.
**Justificativa:** os tipos carregam a classificacao de falha externa usada por retry/circuit
breaker, alem de `Erro` e `Mensagem` proprios; unifica-los atravessaria dominios ou apagaria o
contrato da operacao.

| Arquivo | Inicio | Tamanho |
|---|---:|---:|
| `conformidade/adaptador/saida/mtr/erro/ChecklistMtrException.java` | 39 | 27 |
| `dossieproduto/adaptador/saida/mtr/erro/WorkflowDossieProdutoMtrException.java` | 31 | 25 |
| `arvoredocumento/adaptador/saida/mtr/erro/ProcessoParametrizadoMtrException.java` | 39 | 27 |
| `dossieproduto/adaptador/saida/mtr/erro/CriacaoDossieProdutoMtrException.java` | 40 | 27 |
| `dossieproduto/adaptador/saida/mtr/erro/DocumentoDossieProdutoMtrException.java` | 40 | 27 |
| `dossieproduto/adaptador/saida/mtr/erro/FormularioDossieProdutoMtrException.java` | 40 | 27 |
| `dossieproduto/adaptador/saida/mtr/erro/ValidacaoNegocialDossieProdutoMtrException.java` | 40 | 27 |
| `gestaodocumento/adaptador/saida/mtr/erro/GestaoDocumentoMtrException.java` | 40 | 27 |

### Grupo 2 - tipo e construcao de falhas de dominio, 8 ocorrencias, 120 linhas

**Ownership:** cada classe pertence ao dominio e a capacidade indicada no nome.
**Classificacao:** intencional de dominio.
**Justificativa:** cada falha possui `Tipo`, mensagens e nulabilidade sob ownership proprio; uma
hierarquia comum criaria modelo compartilhado entre `arvoredocumento`, `conformidade`,
`dossieproduto` e `gestaodocumento`.

| Arquivo | Inicio | Tamanho |
|---|---:|---:|
| `dossieproduto/dominio/erro/FalhaAtualizacaoFormularioDossieProduto.java` | 7 | 15 |
| `arvoredocumento/dominio/erro/FalhaConsultaProcessoParametrizado.java` | 7 | 15 |
| `conformidade/dominio/erro/FalhaConsultaChecklist.java` | 7 | 15 |
| `dossieproduto/dominio/erro/FalhaCriacaoDossieProduto.java` | 7 | 15 |
| `dossieproduto/dominio/erro/FalhaInclusaoDocumentoDossieProduto.java` | 7 | 15 |
| `dossieproduto/dominio/erro/FalhaRegistroValidacaoNegocialDossieProduto.java` | 7 | 15 |
| `dossieproduto/dominio/erro/FalhaWorkflowDossieProduto.java` | 7 | 15 |
| `gestaodocumento/dominio/erro/FalhaObtencaoCredencialContainer.java` | 7 | 15 |

### Grupo 3 - mensagem e causa das falhas de dominio, 8 ocorrencias, 367 linhas

**Ownership:** os mesmos oito tipos do Grupo 2, nos respectivos dominios.
**Classificacao:** intencional de dominio.
**Justificativa:** o trecho preserva a traducao de mensagens e causas de cada capacidade; a
semelhanca textual nao prova uma regra comum e a extracao cruzaria fronteiras de negocio.

| Arquivo | Inicio | Tamanho |
|---|---:|---:|
| `dossieproduto/dominio/erro/FalhaAtualizacaoFormularioDossieProduto.java` | 34 | 50 |
| `arvoredocumento/dominio/erro/FalhaConsultaProcessoParametrizado.java` | 34 | 48 |
| `conformidade/dominio/erro/FalhaConsultaChecklist.java` | 34 | 48 |
| `dossieproduto/dominio/erro/FalhaCriacaoDossieProduto.java` | 34 | 48 |
| `dossieproduto/dominio/erro/FalhaInclusaoDocumentoDossieProduto.java` | 34 | 50 |
| `dossieproduto/dominio/erro/FalhaRegistroValidacaoNegocialDossieProduto.java` | 34 | 50 |
| `dossieproduto/dominio/erro/FalhaWorkflowDossieProduto.java` | 34 | 25 |
| `gestaodocumento/dominio/erro/FalhaObtencaoCredencialContainer.java` | 34 | 48 |

### Grupo 4 - mapeamento de arvoredocumento MTR e simulador, 2 ocorrencias, 34 linhas

**Ownership:** dominio `arvoredocumento`, com um mapper por borda de saida.
**Classificacao:** intencional de contrato.
**Justificativa:** os mappers leem `ProcessoParametrizadoMtrResponse` e
`ProcessoParametrizadoSimuladorResponse` distintos e produzem modelos internos; compartilhar
mapper ou DTO violaria a independencia MTR/simulador.

| Arquivo | Inicio | Tamanho |
|---|---:|---:|
| `arvoredocumento/adaptador/saida/mtr/mapper/ProcessoParametrizadoMtrMapper.java` | 209 | 17 |
| `arvoredocumento/adaptador/saida/simulador/mapper/ProcessoParametrizadoSimuladorMapper.java` | 218 | 17 |

## Totais reconciliados

| Medicao | Soma dos grupos | Baseline Sonar |
|---|---:|---:|
| Blocos | 26 | 26 |
| Linhas | 735 | 735 |
| Arquivos com blocos | 18 | - |
| Grupos deduplicados | 4 | - |

## Resultado da classificacao e piloto

Nenhum dos quatro grupos e **removivel local** sob as regras aprovadas. O menor grupo e o Grupo
4, com dois arquivos, mas ele atravessa a borda MTR/simulador e usa DTOs independentes. Os Grupos
1 a 3 atravessam dominios, contratos ou capacidades e excedem o limite de cinco arquivos quando
considerados integralmente.

**Recomendacao para C13-INVENTARIO:** NO-GO para caracterizacao/refatoracao. Reduzir a metrica
exigiria mudar uma fronteira aprovada, compartilhar DTO/erro ou fazer uma alteracao assimetrica que
apenas enganaria a deteccao de clones. Isso contraria o objetivo de reduzir duplicacao sem
comprometer desacoplamento.

## Verificacoes executadas

- `mvn -q -Dtest=ArchUnitProgressivoTest test`: aprovado;
- nenhum arquivo em `src/main` ou `src/test` foi alterado;
- `git diff --check`: aprovado, com os avisos normais de conversao LF/CRLF;
- busca por token Sonar nos documentos alterados: limpa;
- User Token usado na coleta foi removido da memoria ao final.
