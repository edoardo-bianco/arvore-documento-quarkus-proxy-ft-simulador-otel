# Orientacao para agentes

Antes de alterar a arquitetura ou implementar uma etapa da refatoracao DDD:

1. Leia `doc/arquitetura-ddd-integracoes-atomicas.md`.
2. Leia `tasks/plan.md` e retome somente o proximo item pendente em `tasks/todo.md`.
3. Confirme que a branch atual corresponde a fase em execucao, no formato
   `refactor/sonar-quality-fase-<numero>-baseline`; nao execute nenhuma fase em `main`. A branch
   atual e `refactor/sonar-quality-fase-16-baseline`.
4. Para iniciar a Fase 0, use a regra de bootstrap de `tasks/plan.md`. Depois dela, nao avance alem
   de um checkpoint sem GO humano registrado na tabela de `tasks/todo.md`.
5. Preserve paths, JSON, validacoes, erros, simulador, fault tolerance e observabilidade.
6. Nao implemente novos endpoints, workflows, upload, cache de SAS ou Quarkus Flow neste escopo.
7. A refatoracao de producao foi encerrada com GO humano no checkpoint C6. As Fases 7 a 9 sao
   exclusivamente documentais e nao autorizam endpoint, contrato ou alteracao de producao.
8. As Fases 0 a 12 foram encerradas com GO humano. A Fase 13 permanece documental, com NO-GO
   recomendado para extracao de clones entre fronteiras. As Fases 14 e 15 foram encerradas. A
   Fase 16 trata exclusivamente as issues `CRITICAL` do CSV oficial
   `doc/sonar/sonar-issues-staging.csv`; o plano foi autorizado no C16-PLAN.
9. O package tecnico compartilhado de erro `arquitetura.excecao.dto` permanece como excecao
   arquitetural documentada; qualquer mudanca exige fase futura explicitamente autorizada.
10. A Fase 12 foi encerrada com GO humano no C12. As Fases 13 a 16 usam especificacoes e branches
    proprias; nao reutilize branches de fechamento. O proximo item permitido e a Task 16.0,
    seguida das Tasks 16.1 e 16.2, com parada obrigatoria no checkpoint C16-A.

## Formatos derivados da documentacao

Ao alterar um documento fonte em Markdown (`.md`), nao gere, regenere nem atualize suas versoes
derivadas em PowerPoint (`.ppt` ou `.pptx`), PDF (`.pdf`) ou HTML (`.html`), mesmo que esses
arquivos fiquem desatualizados em relacao ao Markdown. Esses arquivos sao apenas outros formatos
do mesmo documento. As apresentacoes em PowerPoint sao atualizadas manualmente pelo responsavel.
Somente altere qualquer um desses formatos quando o usuario solicitar isso explicitamente.
