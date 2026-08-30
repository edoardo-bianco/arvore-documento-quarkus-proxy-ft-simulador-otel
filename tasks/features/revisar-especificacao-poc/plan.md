# Plano: revisar especificação da PoC de conformidade

## Intenção

Transformar a especificação da PoC em uma referência fiel ao estado implementado, tornando
explícitas as decisões arquiteturais, os componentes C4, o schema lógico dos documentos e a
localização lógica e física dos dados persistidos.

## Escopo

- revisar integralmente o Markdown fonte da especificação da PoC;
- distinguir decisão aceita, proposta implementada localmente e pendência pré-PRD;
- substituir o diagrama genérico atual por um diagrama C4 de componentes;
- documentar o schema lógico compartilhado por CouchDB e Cosmos DB for NoSQL;
- identificar database, containers, partição, concorrência, feeds, volume/PVC e checkpoints;
- remover instruções de planejamento que ficaram obsoletas depois da implementação.

## Fora de escopo

- alterar código, contratos REST, configuração executável, scripts ou infraestrutura;
- aceitar ou substituir ADRs por inferência do agente;
- executar o gate real do Cosmos, alterar backends ou promover a PoC para produção;
- gerar formatos derivados `.ppt`, `.pptx`, `.pdf` ou `.html`.

## Contexto verificado

- arquitetura consolidada lida:
  `doc/arquitetura-distribuida/arquitetura-ddd-integracoes-atomicas.md`;
- ADRs aplicáveis: 0001, 0002, 0003, 0004, 0005, 0006, 0009 e 0010;
- código, contratos e testes inspecionados: domínio e aplicação de `conformidade`, adapters
  documentais CouchDB/Cosmos, feeds nativos, Flow, REST, propriedades, Compose, manifests kind e
  contratos de teste relacionados;
- divergências: a especificação mistura projeto futuro e estado implementado, chama um flowchart
  de diagrama C4, não consolida o schema documental e não deixa explícita a localização física dos
  documentos; o ADR-0010 permanece `Proposto` apesar das provas locais, pois o gate Cosmos real e
  sua aceitação humana continuam pendentes.

## Decisões e impactos

| Dimensão | Situação | Checkpoint humano adicional? |
|---|---|---|
| Contrato | somente documentação do contrato implementado, sem mudança de path, status ou JSON | não |
| Arquitetura | explicitação de decisões já registradas e implementadas; nenhum novo status de ADR | não |
| Segurança | documentação de credenciais externas, dados sensíveis e Entra ID já decididos | não |
| Comportamento observável | documentação da separação entre documentos, checkpoints e feeds | não |

## Tarefas

### Task 1 — Consolidar a especificação implementada

**Descrição:** reescrever a especificação como documento de estado atual, preservando requisitos
úteis e registrando de forma verificável decisões, componentes, schema e armazenamento.

**Critérios de aceitação:**

- o documento diferencia ADR-0009 `Aceito` de ADR-0010 `Proposto`;
- existe catálogo explícito de decisões com motivação, consequência e evidência;
- existe diagrama C4 Component coerente com packages, portas e adapters atuais;
- existe schema lógico com tipos documentais, IDs, campos, mutabilidade e referências;
- CouchDB, Cosmos, Redis/Valkey, `_changes`, Change Feed e localização física são distinguidos;
- limitações de Cosmos real e disponibilidade dos próprios backends permanecem visíveis;
- linguagem obsoleta de primeira fase de planejamento é removida.

**Verificação:**

- inspeção dos links, headings e blocos Mermaid/JSON;
- buscas por formulações obsoletas e decisões contraditórias;
- `git diff --check` e revisão integral do diff.

**Dependências:** nenhuma.

**Arquivos prováveis:**

- `doc/poc/especificacao-poc-conformidade-quarkus-flow-ollama-hitl-sem-broker.md`;
- `tasks/features/revisar-especificacao-poc/plan.md`;
- `tasks/features/revisar-especificacao-poc/todo.md`.

### Checkpoint — revisão documental

- confirmar que todas as afirmações arquiteturais possuem base no código, no consolidado ou nos
  ADRs aplicáveis;
- não alterar o status do ADR-0010 sem decisão humana explícita.

## SonarQube

- fonte do baseline: não aplicável a escopo exclusivamente documental;
- pacote autorizado: nenhum;
- checkpoint esperado: nenhum, conforme `AGENTS.md`.

## Riscos e controles

| Risco | Impacto | Controle |
|---|---|---|
| descrever intenção antiga como estado atual | alto | confrontar cada seção com código e testes atuais |
| inferir prontidão produtiva | alto | destacar gate Cosmos real e limites das provas locais |
| confundir documento negocial com checkpoint Flow | alto | mapas lógico e físico separados |
| tratar CouchDB como banco relacional | médio | usar schema documental, fatos e projeção, sem tabelas |
| promover ADR-0010 sem autorização | alto | preservar status `Proposto` em todos os trechos |

## GO necessário

O escopo é exclusivamente documental e foi solicitado explicitamente pelo usuário. Não haverá
alteração de produção nem checkpoint SonarQube nesta task.
