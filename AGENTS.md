# Orientacao para agentes

Antes de alterar a arquitetura ou implementar uma etapa da refatoracao DDD:

1. Leia `doc/arquitetura-ddd-integracoes-atomicas.md`.
2. Leia `tasks/plan.md` e retome somente o proximo item pendente em `tasks/todo.md`.
3. Confirme que a branch atual corresponde a fase em execucao, no formato
   `refactor/ddd-fase-<numero>-baseline`; nao execute nenhuma fase em `main`. A branch de fechamento
   e `refactor/ddd-fase-6-baseline`.
4. Para iniciar a Fase 0, use a regra de bootstrap de `tasks/plan.md`. Depois dela, nao avance alem
   de um checkpoint sem GO humano registrado na tabela de `tasks/todo.md`.
5. Preserve paths, JSON, validacoes, erros, simulador, fault tolerance e observabilidade.
6. Nao implemente novos endpoints, workflows, upload, cache de SAS ou Quarkus Flow neste escopo.
7. A refatoracao planejada foi encerrada com GO humano no checkpoint C6. Qualquer novo trabalho
   arquitetural exige novo plano, nova fase e nova branch no formato definido acima; nao reutilize
   a branch de fechamento como baseline de uma fase futura.
