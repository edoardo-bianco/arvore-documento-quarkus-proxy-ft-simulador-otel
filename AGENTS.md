# Orientacao para agentes

Antes de alterar a arquitetura ou implementar uma etapa da refatoracao DDD:

1. Leia `doc/arquitetura-ddd-integracoes-atomicas.md`.
2. Leia `tasks/plan.md` e retome somente o proximo item pendente em `tasks/todo.md`.
3. Confirme que a branch atual corresponde a fase em execucao, no formato
   `refactor/sonar-quality-fase-<numero>-baseline`; nao execute nenhuma fase em `main`. A branch
   atual e `refactor/sonar-quality-fase-15-baseline`.
4. Para iniciar a Fase 0, use a regra de bootstrap de `tasks/plan.md`. Depois dela, nao avance alem
   de um checkpoint sem GO humano registrado na tabela de `tasks/todo.md`.
5. Preserve paths, JSON, validacoes, erros, simulador, fault tolerance e observabilidade.
6. Nao implemente novos endpoints, workflows, upload, cache de SAS ou Quarkus Flow neste escopo.
7. A refatoracao de producao foi encerrada com GO humano no checkpoint C6. As Fases 7 a 9 sao
   exclusivamente documentais e nao autorizam endpoint, contrato ou alteracao de producao.
8. As Fases 0 a 12 foram encerradas com GO humano. A Fase 13 permanece documental, com NO-GO
   recomendado para extracao de clones entre fronteiras. A Fase 14 foi encerrada. A Fase 15 trata
   o blocker, issues HIGH e warning de blame do SonarQube; somente a especificacao foi autorizada
   no C15-SPEC, e plano/tasks permanecem bloqueados ate GO posterior.
9. O package tecnico compartilhado de erro `arquitetura.excecao.dto` permanece como excecao
   arquitetural documentada; qualquer mudanca exige fase futura explicitamente autorizada.
10. A Fase 12 foi encerrada com GO humano no C12. A Fase 13 usa especificacao e branch proprias,
    e a Fase 14 usa branch propria. A Fase 15 usa nova especificacao e branch
    `refactor/sonar-quality-fase-15-baseline`; nao reutilize branches de fechamento. O proximo
    item permitido e a especificacao C15, seguida de GO para plano e tasks.

## Formatos derivados da documentacao

Ao alterar um documento fonte em Markdown (`.md`), nao gere, regenere nem atualize suas versoes
derivadas em PowerPoint (`.ppt` ou `.pptx`), PDF (`.pdf`) ou HTML (`.html`), mesmo que esses
arquivos fiquem desatualizados em relacao ao Markdown. Esses arquivos sao apenas outros formatos
do mesmo documento. As apresentacoes em PowerPoint sao atualizadas manualmente pelo responsavel.
Somente altere qualquer um desses formatos quando o usuario solicitar isso explicitamente.
