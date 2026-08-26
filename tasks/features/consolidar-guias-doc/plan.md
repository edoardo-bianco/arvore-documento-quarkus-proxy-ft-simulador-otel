# Plano: consolidar guias comuns e exclusivos em `doc/guias`

## Intenção

Manter os guias na pasta `doc/guias/` sem duplicidade, respeitando a separação entre as branches:

- a `main` contém somente os guias das capacidades comuns;
- `feature/poc-conformidade-flow-ollama` contém exatamente a mesma documentação comum da `main`,
  acrescida dos guias e funcionalidades exclusivos da POC;
- nenhuma funcionalidade, documentação ou commit exclusivo da POC é integrado à `main`.

As branches continuam separadas enquanto a POC estiver em desenvolvimento. A atualização ocorre
somente no sentido `main -> POC`, por rebase da POC sobre a `main`.

## Escopo

### Parte comum — destinada à `main` e à POC

- consolidar em `doc/guias/`, sem cópias em `doc/`, os guias:
  - `guia-consumo-porta-cdi-consulta-documentos-dossie.md`;
  - `guia-implementacao-nova-capacidade-mtr.md`;
- preservar as correções locais de links relativos do guia de consumo CDI;
- atualizar o `README.md` e outras referências aos paths antigos;
- isolar essa mudança em um commit estritamente documental;
- levar somente esse commit para `docs/consolidar-guias-doc`, criada sobre a `main`, e abrir PR
  dessa branch para a `main`;
- depois do merge da PR, rebasear a POC sobre a `main`, tornando o commit comum ancestral da POC.

### Parte exclusiva — destinada somente à POC

- mover `doc/poc/guia-verificacao-poc-conformidade.md` para `doc/guias/`;
- ajustar os links do `README.md`, da especificação da POC, do próprio guia e dos registros da
  feature que apontam para o path antigo;
- manter essa alteração em commit posterior e exclusivo da branch POC;
- confirmar que o commit exclusivo não faz parte da branch nem da PR destinada à `main`.

## Fora de escopo

- fazer merge da branch POC na `main`;
- levar código, configuração, funcionalidade ou documentação exclusiva da POC para a `main`;
- alterar o conteúdo técnico dos guias além dos links exigidos pelas mudanças de diretório;
- alterar código, contratos, testes, tooling, ADRs ou arquitetura;
- gerar ou atualizar formatos derivados `.ppt`, `.pptx`, `.pdf` ou `.html`;
- executar Maven ou checkpoint SonarQube, pois o escopo é exclusivamente documental.

## Contexto verificado

- arquitetura consolidada e índice de ADRs lidos;
- ADRs aplicáveis: nenhum, pois não há decisão arquitetural ou contrato alterado;
- os dois guias comuns existem em `doc/` e possuem cópias locais staged em `doc/guias/`;
- os blobs staged dos guias comuns são idênticos aos arquivos em `doc/`; o guia CDI possui quatro
  correções unstaged de links relativos necessárias após a mudança de pasta;
- o `README.md` ainda aponta para o path antigo do guia de implementação comum;
- o guia exclusivo da POC está em `doc/poc/guia-verificacao-poc-conformidade.md` e é referenciado
  pelo `README.md`, pela especificação da POC e pelo checklist histórico da feature;
- mover o guia exclusivo para `doc/guias/` exige ajustar também o link de volta para a
  especificação, hoje relativo à pasta `doc/poc/`;
- divergência corrigida no plano: igualdade entre as branches aplica-se somente à parte comum; a
  POC permanece um superset da `main` e não será integrada nela.

## Estratégia de histórico

```text
main ─────────────── C  ← PR contém somente documentação comum
                       \
POC  ── commits POC ──── rebase sobre C ── P
                                         ↑
                                         documentação exclusiva da POC
```

- `C` é o commit documental comum, sem dependência de arquivos exclusivos da POC;
- a branch da PR é criada a partir da `main` e recebe somente `C`;
- a `main` nunca recebe um merge, rebase ou conjunto de commits da branch POC;
- após o merge de `C`, a POC é rebaseada sobre a nova `main`;
- `P` move e corrige somente o guia exclusivo, permanecendo na POC.

## Decisões e impactos

| Dimensão | Situação | Checkpoint humano adicional? |
|---|---|---|
| Contrato | Nenhum path HTTP, JSON, DTO ou contrato externo muda | não |
| Arquitetura | Nenhuma responsabilidade ou dependência muda | não |
| Segurança | Nenhuma superfície de entrada ou dado sensível muda | não |
| Comportamento observável | Nenhum log, span, métrica ou configuração muda | não |

## Tarefas

### Task 1 — preparar a alteração documental comum

**Descrição:** consolidar somente os dois guias comuns em `doc/guias/`, corrigir seus links e
referências e criar um commit documental que possa ser aplicado sobre a `main` sem depender da POC.

**Critérios de aceitação:**

- cada guia comum existe uma única vez, em `doc/guias/`;
- nenhum arquivo ou referência permanece nos dois paths antigos em `doc/`;
- o diff contém somente documentação comum e os registros desta task;
- nenhum arquivo, conteúdo ou referência exclusiva da POC é incluído no commit comum.

**Verificação:**

- `rg` não encontra referências para os dois paths comuns antigos;
- a listagem de `doc/` encontra somente uma cópia de cada guia comum;
- `git diff --check` não encontra erro de whitespace;
- a revisão do diff confirma que o commit é aplicável à `main` isoladamente.

**Dependências:** GO humano C0.

**Arquivos prováveis:**

- `README.md`;
- cópias correspondentes atualmente localizadas na raiz de `doc/`;
- `doc/guias/guia-consumo-porta-cdi-consulta-documentos-dossie.md`;
- `doc/guias/guia-implementacao-nova-capacidade-mtr.md`;
- `tasks/features/consolidar-guias-doc/plan.md`;
- `tasks/features/consolidar-guias-doc/todo.md`.

### Checkpoint C1 — PR exclusivamente comum para a `main`

- criar `docs/consolidar-guias-doc` a partir da `main`;
- aplicar somente o commit da Task 1 nessa branch;
- confirmar que `git diff main...docs/consolidar-guias-doc` não contém arquivos da POC;
- publicar e abrir PR de `docs/consolidar-guias-doc` para `main`;
- aguardar o merge da PR antes de atualizar a POC.

### Task 2 — atualizar a base comum da POC

**Descrição:** depois do merge da PR, rebasear a branch POC sobre a nova `main`, sem realizar merge
ou fluxo inverso da POC para a `main`.

**Critérios de aceitação:**

- `origin/main` é ancestral da branch POC;
- os dois guias comuns da POC são os mesmos herdados da `main`;
- o rebase preserva os commits e arquivos exclusivos da POC;
- nenhum commit exclusivo da POC aparece na `main`.

**Verificação:**

- comparação de ancestralidade e do diff `main...POC`;
- o diff POC contra `main` contém somente os acréscimos próprios da POC;
- busca pelos nomes e paths dos dois guias comuns confirma unicidade.

**Dependências:** merge da PR do Checkpoint C1.

### Task 3 — consolidar o guia exclusivo dentro da POC

**Descrição:** mover o guia de verificação da POC de `doc/poc/` para `doc/guias/`, corrigir suas
referências e registrar essa mudança em commit exclusivo da POC.

**Critérios de aceitação:**

- o guia exclusivo existe uma única vez em `doc/guias/` na POC;
- nenhuma referência ativa aponta para `doc/poc/guia-verificacao-poc-conformidade.md`;
- o guia e suas referências não existem na `main` nem na branch da PR comum;
- o conteúdo técnico é preservado, salvo links necessários ao move.

**Verificação:**

- `rg` não encontra o path antigo do guia exclusivo;
- `git diff --check` não encontra erro de whitespace;
- comparação com a `main` confirma que a mudança é exclusiva da POC.

**Dependências:** Task 2.

### Checkpoint CF — encerramento

- `main`: somente guias comuns, sem duplicidade, em `doc/guias/`;
- POC: mesmos guias comuns da `main` mais o guia exclusivo, todos em `doc/guias/`;
- nenhuma funcionalidade ou documentação exclusiva da POC foi integrada à `main`;
- evidências de unicidade, links, ancestralidade e sincronização registradas no checklist;
- encerramento permanece uma decisão humana.

## SonarQube

- fonte do baseline: não aplicável ao escopo exclusivamente documental;
- pacote autorizado: nenhum;
- checkpoint esperado: nenhum, conforme `AGENTS.md`.

## Riscos e controles

| Risco | Impacto | Controle |
|---|---|---|
| contaminar a PR comum com arquivos ou commits da POC | alto | criar a branch da PR sobre `main` e revisar seu diff completo antes da publicação |
| perder as correções locais ainda não commitadas | médio | preservar e comparar blobs/diff antes de trocar ou rebasear branches |
| manter links quebrados após os moves | médio | corrigir links relativos e buscar todos os paths antigos antes de cada commit |
| duplicar o commit comum durante o rebase | médio | identificar o patch comum e confirmar que a POC o herda da nova `main` |
| alterar acidentalmente código ou formato derivado | baixo | limitar staging a Markdown e revisar o diff final |

## GO necessário

Nenhuma alteração nos guias, referências ou branches começa antes do GO humano registrado no
`todo.md` para este plano corrigido.
