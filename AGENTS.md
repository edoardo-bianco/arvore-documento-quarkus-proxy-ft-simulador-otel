# Orientacao para agentes

Antes de alterar a arquitetura ou implementar uma etapa da refatoracao DDD:

1. Leia `doc/arquitetura-ddd-integracoes-atomicas.md`.
2. Leia `tasks/plan.md` e retome somente o proximo item pendente em `tasks/todo.md`.
3. Confirme que a branch atual corresponde a fase em execucao, no formato
   `refactor/ddd-fase-<numero>-baseline`; nao execute nenhuma fase em `main`. A branch
   atual e `refactor/ddd-fase-10-baseline`.
4. Para iniciar a Fase 0, use a regra de bootstrap de `tasks/plan.md`. Depois dela, nao avance alem
   de um checkpoint sem GO humano registrado na tabela de `tasks/todo.md`.
5. Preserve paths, JSON, validacoes, erros, simulador, fault tolerance e observabilidade.
6. Nao implemente novos endpoints, workflows, upload, cache de SAS ou Quarkus Flow neste escopo.
7. A refatoracao de producao foi encerrada com GO humano no checkpoint C6. As Fases 7 a 9 sao
   exclusivamente documentais e nao autorizam endpoint, contrato ou alteracao de producao.
8. A Fase 9 foi encerrada com GO humano no C9. Qualquer novo trabalho exige novo plano, nova fase
   e nova branch; nao reutilize a branch documental de fechamento.
9. A Fase 10 limita-se a explicitar o uso pragmatico do Quarkus, ajustar os guardrails ArchUnit e
   alinhar a documentacao. Ela nao autoriza mudanca de producao, endpoint, contrato ou comportamento.
10. A Fase 10 foi encerrada com GO humano no C10. Qualquer novo trabalho exige nova fase e nova
    branch; nao reutilize a branch de fechamento.
