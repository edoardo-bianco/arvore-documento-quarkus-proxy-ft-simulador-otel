# Plano: <nome da feature>

## Intenção

<Resultado desejado, usuário beneficiado e motivo da mudança.>

## Escopo

- <entrega incluída>;

## Fora de escopo

- <não objetivo explícito>;

## Contexto verificado

- arquitetura consolidada lida;
- ADRs aplicáveis: <números ou “nenhum”>;
- código, contratos e testes inspecionados: <paths>;
- divergências ou suposições: <itens ou “nenhuma”>.

## Decisões e impactos

| Dimensão | Situação | Checkpoint humano adicional? |
|---|---|---|
| Contrato | <impacto> | <sim/não> |
| Arquitetura | <impacto> | <sim/não> |
| Segurança | <impacto> | <sim/não> |
| Comportamento observável | <impacto> | <sim/não> |

## Tarefas

### Task 1 — <resultado pequeno e verificável>

**Descrição:** <o que será entregue e por quê>.

**Critérios de aceitação:**

- <condição observável>;

**Verificação:**

- <teste, build, análise ou inspeção>;

**Dependências:** <tasks ou “nenhuma”>.

**Arquivos prováveis:**

- `<path>`;

### Checkpoint — <nome>

- <evidência necessária>;
- <decisão humana, quando aplicável>.

## SonarQube

- fonte do baseline: <local, local + pacote ou offline>;
- pacote autorizado: <path ou “nenhum”>;
- checkpoint esperado: <momento do incremento>.

## Riscos e controles

| Risco | Impacto | Controle |
|---|---|---|
| <risco> | <alto/médio/baixo> | <mitigação> |

## GO necessário

Nenhuma alteração de produção começa antes do GO humano registrado no `todo.md`.
