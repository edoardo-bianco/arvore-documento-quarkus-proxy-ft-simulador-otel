# Checklist: consolidar guias comuns e exclusivos em `doc/guias`

## Estado

- **Branch da PR comum:** `docs/consolidar-guias-doc`, baseada em `main`
- **Branch POC:** `feature/poc-conformidade-flow-ollama`
- **Fluxo permitido:** `main -> POC`; nunca `POC -> main`
- **Escopo:** exclusivamente documental
- **Próximo item:** 1.3 — aguardar o merge humano da PR documental `#19`

## Checklist

- [x] 0.1 Confirmar que a igualdade entre branches vale somente para a documentação comum;
- [x] 0.2 Confirmar que funcionalidades e documentação exclusivas permanecem somente na POC;
- [x] 0.3 Ler arquitetura, índice e ADRs aplicáveis;
- [x] 0.4 Inspecionar guias comuns, guia exclusivo, duplicidades, blobs e referências;
- [x] 0.5 Corrigir o plano, os riscos e a estratégia de histórico;
- [x] C0 Registrar GO humano antes da primeira alteração nos guias, referências ou branches;
- [x] 1.1 Consolidar os dois guias comuns em `doc/guias/` num commit documental isolado;
- [x] C1 Verificar unicidade, ausência de paths antigos, whitespace e ausência de conteúdo POC;
- [x] 1.2 Criar `docs/consolidar-guias-doc` sobre `main`, aplicar somente o commit comum e publicar;
- [ ] 1.3 Abrir PR de `docs/consolidar-guias-doc` para `main` e aguardar seu merge;
- [ ] 2.1 Rebasear a POC sobre a `main` que já contém a mudança comum;
- [ ] C2 Verificar ancestralidade e que o diff da POC contém somente seus acréscimos exclusivos;
- [ ] 3.1 Mover o guia exclusivo da POC para `doc/guias/` e corrigir suas referências;
- [ ] C3 Verificar unicidade e confirmar que o guia exclusivo não existe na `main`;
- [ ] 3.2 Commitar e publicar a alteração exclusiva somente na branch POC;
- [ ] CF Solicitar encerramento humano da feature.

## Decisões humanas

| Checkpoint | Status | Data | Evidência | Aprovador |
|---|---|---|---|---|
| C0 | APROVADO | 2026-08-26 | GO explícito no chat para o plano corrigido | usuário |
| CF | PENDENTE | — | Encerramento depende das verificações finais | — |

## Evidências técnicas

| Checkpoint | Status | Data | Evidência |
|---|---|---|---|
| C1 | CONFORME | 2026-08-26 | `eb11f2d`: somente `README.md` e dois renomes Markdown; R097/R100; paths antigos ausentes; `git diff --check` limpo; blobs de origem iguais aos da `origin/main` `6fd653d`; guia exclusivo da POC preservado byte a byte |
| 1.2 | PUBLICADO | 2026-08-26 | `docs/consolidar-guias-doc` criada sobre `origin/main` `6fd653d`; somente o commit comum foi aplicado como `48e2495`; branch publicada e sincronizada com `origin/docs/consolidar-guias-doc` |
| 1.3 | PR_ABERTA | 2026-08-26 | PR `#19` aberta de `docs/consolidar-guias-doc` para `main`; um commit e três arquivos documentais; estado remoto `CLEAN`, sem checks reportados; aguarda merge humano |
