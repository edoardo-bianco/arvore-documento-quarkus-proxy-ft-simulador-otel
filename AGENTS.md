# Orientacao para agentes

Antes de alterar a arquitetura ou implementar uma etapa da refatoracao DDD:

1. Leia `doc/arquitetura-ddd-integracoes-atomicas.md`.
2. Leia `tasks/plan.md` e retome somente o proximo item pendente em `tasks/todo.md`.
3. Confirme que a branch atual corresponde a fase em execucao, no formato
   `refactor/ddd-fase-<numero>-baseline`; nao execute nenhuma fase em `main`. A branch atual da
   Fase 3 e `refactor/ddd-fase-3-baseline`.
4. Para iniciar a Fase 0, use a regra de bootstrap de `tasks/plan.md`. Depois dela, nao avance alem
   de um checkpoint sem GO humano registrado na tabela de `tasks/todo.md`.
5. Preserve paths, JSON, validacoes, erros, simulador, fault tolerance e observabilidade.
6. Nao implemente novos endpoints, workflows, upload, cache de SAS ou Quarkus Flow neste escopo.
