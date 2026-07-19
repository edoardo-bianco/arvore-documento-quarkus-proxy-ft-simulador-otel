# Plano: guia permanente para novas features

## Status

- **Estado:** encerrado com GO humano no C1
- **Data:** 2026-07-19
- **Branch:** `docs/agent-guidance-features`

## Intenção confirmada

Transformar a orientação de agentes e a documentação arquitetural em material permanente para
planejar e implementar novas features com segurança. O histórico da refatoração e as tasks já
encerradas deixam de ser contexto obrigatório. Permanecem o estado arquitetural atual, as decisões
relevantes e o protocolo completo de checkpoint SonarQube.

## Escopo

- reescrever `AGENTS.md` como guia operacional sem referências a fases encerradas;
- condensar `doc/arquitetura-ddd-integracoes-atomicas.md` para descrever somente a arquitetura
  implementada e as restrições vigentes;
- criar `doc/adr/README.md` como índice de leitura rápida;
- extrair decisões vigentes para ADRs individuais, com resumo e indicação de aplicabilidade no
  início de cada arquivo;
- substituir as tasks históricas da raiz por instruções e templates para
  `tasks/features/<nome>/plan.md` e `todo.md`;
- migrar os sinais contratuais ainda vigentes de `tasks/inventario-observabilidade.md` para um
  catálogo operacional em `doc/` e corrigir referências no README/documentação de operação;
- preservar integralmente o fluxo de baseline, checkpoint e decisão humana do SonarQube.

## Fora de escopo

- alterar `src/`, `pom.xml`, dependências ou configuração Quarkus;
- alterar paths, contratos JSON, validações, erros ou comportamento da aplicação;
- alterar scripts e hooks SonarQube já aprovados;
- apagar `doc/sonar/`, contratos OpenAPI/Postman ou documentação funcional;
- atualizar formatos derivados `.ppt`, `.pptx`, `.pdf` ou `.html`.

## Estrutura documental alvo

```text
AGENTS.md
doc/
|-- arquitetura-ddd-integracoes-atomicas.md
|-- catalogo-observabilidade.md
`-- adr/
    |-- README.md
    |-- 0001-monolito-modular-e-hexagonal.md
    |-- 0002-limites-por-dominio-e-capacidade.md
    |-- 0003-orquestracao-e-colaboracao-por-portas.md
    |-- 0004-contratos-independentes-por-borda.md
    |-- 0005-integracoes-mtr-simulador-e-fault-tolerance.md
    |-- 0006-compatibilidade-observabilidade-e-testes.md
    |-- 0007-idempotencia-antes-de-fluxos-mutaveis.md
    `-- 0008-mcp-como-borda-futura.md
tasks/
|-- README.md
|-- templates/
|   |-- plan.md
|   `-- todo.md
`-- features/
    `-- <nome>/
        |-- plan.md
        `-- todo.md
```

O índice de ADRs deve informar número, título, status, resumo da decisão e quando consultar o ADR.
Cada ADR deve começar com esses mesmos dados em linguagem suficiente para orientar o agente sem
leitura integral. O conteúdo completo só é obrigatório quando o ADR for aplicável ou houver dúvida.

## Ordem de execução

### Task 1 — Consolidar a arquitetura atual

**Descrição:** reescrever o documento canônico para apresentar o sistema implementado, seus
domínios, capacidades, fluxo, fronteiras e riscos vigentes, sem narrativa de migração ou fases.

**Critérios de aceitação:**

- descreve as oito capacidades atuais e os cinco endpoints ainda ausentes sem sugerir que já
  existem;
- preserva direção de dependências, contratos de borda, erros, FT, observabilidade e estratégia de
  testes;
- não contém plano executado, checkpoints históricos ou relato da refatoração.

**Verificação:** revisão de headings, links e comparação com código/contratos atuais.

### Task 2 — Extrair e indexar as decisões arquiteturais

**Descrição:** criar o índice e os oito ADRs aceitos listados na estrutura alvo.

**Critérios de aceitação:**

- o índice permite entender todas as decisões e selecionar ADRs sem abrir cada arquivo;
- cada ADR começa com status, decisão em uma frase e quando consultar;
- contexto, decisão, consequências e alternativas rejeitadas permanecem disponíveis no corpo.

**Dependência:** Task 1.

**Verificação:** todos os links do índice existem e as decisões não entram em conflito com o
consolidado.

### Checkpoint A — Coerência arquitetural documental

- consolidado e índice descrevem o estado vigente sem mudar a arquitetura;
- decisões relevantes foram preservadas antes da remoção da narrativa histórica;
- revisão humana é solicitada se surgir nova decisão, e não apenas extração documental.

### Task 3 — Reescrever o guia permanente dos agentes

**Descrição:** substituir regras de fases por um fluxo reutilizável para qualquer feature.

**Critérios de aceitação:**

- exige leitura do consolidado e do índice, com ADR completo apenas quando aplicável ou duvidoso;
- exige inspeção do código, contratos e testes relacionados antes do plano;
- exige pasta própria, plano registrado e GO humano antes da primeira alteração de produção;
- exige checkpoints adicionais para contrato, arquitetura, segurança ou comportamento observável;
- mantém integralmente o protocolo SonarQube e a regra de formatos derivados.

**Dependência:** Tasks 1 e 2.

**Verificação:** nenhuma referência a fases, branches encerradas ou retomada histórica permanece.

### Task 4 — Substituir o planejamento histórico

**Descrição:** criar o README e os templates permanentes, migrar para `doc/` o catálogo vigente de
observabilidade e remover os 19 arquivos históricos hoje existentes diretamente em `tasks/`.

**Critérios de aceitação:**

- `tasks/README.md` explica criação, ciclo de vida, GO e checkpoints de uma feature;
- templates possuem intenção, escopo, critérios de aceitação, verificação, riscos e decisões
  humanas;
- README e documentação operacional apontam para fontes permanentes, e o catálogo preserva nomes
  de spans, eventos, atributos e configuração ainda vigentes sem narrativa de fases;
- os planos, baselines, inventários e especificações das fases encerradas deixam de existir em
  `tasks/`;
- este plano e seu checklist permanecem em `tasks/features/agent-guidance-features/`.

**Dependência:** Tasks 1 a 3, para que nenhuma decisão seja perdida antes das exclusões.

**Verificação:** a raiz de `tasks/` contém somente `README.md`, `templates/` e `features/`.

### Checkpoint B — Validação final

- somente Markdown foi alterado ou removido;
- `doc/sonar/`, código, build, hooks, contratos e derivados permanecem intocados;
- links relativos apontam para arquivos existentes;
- busca por referências a `tasks/plan.md`, `tasks/todo.md` e fases encerradas não encontra uso
  operacional residual;
- `git diff --check` passa;
- Maven e SonarQube não são executados, conforme isenção para mudança exclusivamente documental;
- usuário revisa a evidência e decide o fechamento.

## Riscos e controles

| Risco | Controle |
|---|---|
| Apagar uma decisão junto com a narrativa histórica | Extrair e revisar ADRs antes de remover tasks e seções antigas |
| Índice superficial obrigar leitura de todos os ADRs | Incluir resumo e aplicabilidade no índice e no topo de cada ADR |
| Simplificação alterar a arquitetura por acidente | Comparar consolidado e ADRs com código, contratos e testes atuais |
| Quebrar o protocolo Sonar | Preservar a seção operacional e validar scripts/paths citados |
| Atualizar derivados sem autorização | Alterar apenas Markdown; deixar PDF, HTML e PowerPoint intocados |

## GO necessário

Nenhuma exclusão ou reescrita será iniciada até o usuário registrar GO no checklist desta feature.
