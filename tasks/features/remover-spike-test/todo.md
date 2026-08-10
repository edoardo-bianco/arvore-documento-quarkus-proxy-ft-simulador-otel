# Checklist: remover `src/spike-test`

## Estado

- **Branch:** `refactor/remover-spike-test`
- **Escopo:** testes e tooling Maven
- **Próximo item:** nenhum — feature encerrada pelo usuário

## Checklist

- [x] 0.1 Confirmar intenção e critérios de sucesso;
- [x] 0.2 Ler arquitetura, índice e ADRs 0006/0010;
- [x] 0.3 Inspecionar `src/spike-test`, consumidores, profile Maven e testes substitutos;
- [x] 0.4 Registrar plano, riscos e checkpoints;
- [x] 0.5 Confirmar ausência de pacotes em `sonar/` e selecionar baseline local;
- [x] C0 Registrar GO humano antes da primeira alteração executável;
- [x] 1.1 Inicializar baseline SonarQube local;
- [x] 1.2 Migrar as três provas úteis para `src/test/java`;
- [x] C1 Executar testes focados, suíte padrão, revisar e criar commit próprio;
- [x] 2.1 Remover `src/spike-test` e o profile Maven opt-in;
- [x] C2 Executar suíte padrão, checkpoint Sonar, revisar e criar commit próprio;
- [x] CF Validar ausência de referências e solicitar encerramento humano.

## Decisões humanas

| Checkpoint | Status | Data | Evidência | Aprovador |
|---|---|---|---|---|
| C0 | APROVADO | 2026-08-10 | GO explícito para baseline local, migração, remoção e verificações | Usuário |
| CF | ENCERRADO | 2026-08-10 | Usuário respondeu explicitamente `ENCERRAR` após revisar o resultado técnico | Usuário |

## Evidências técnicas

- 2026-08-10 — baseline SonarQube local inicializado pelo script oficial: `READY`, fonte
  `LOCAL_SONAR`, situação técnica `COMPLIANT`, 240 issues no baseline, cobertura 85,3%,
  duplicação 2,6% e nenhuma violação.
- 2026-08-10 — as três provas originais passaram no source set experimental com
  `mvn -q -Pspike-persistencia-duravel` e seleção focada dos testes.
- 2026-08-10 — as provas foram incorporadas a `src/test/java`; o teste Redis passou a reutilizar
  `ValkeyQuarkusTestResource` e a dependência `serverlessworkflow-persistence-tests` foi movida
  para as dependências normais de teste, sem alterar código de produção ou runtime.
- 2026-08-10 — os três testes migrados passaram pela suíte padrão com seleção focada e código 0.
- 2026-08-10 — `mvn -q test` concluiu a suíte padrão em 169 s com código 0; permaneceu somente
  o erro de formatação do JBoss LogManager já observado no baseline, sem falha da suíte.
- 2026-08-10 — revisão C1 sem achados bloqueantes: contratos preservados, dependência restrita a
  testes, suporte Valkey definitivo reutilizado, nenhum segredo ou mudança de produção/runtime e
  diff restrito ao plano, testes e configuração necessária para compilá-los.
- 2026-08-10 — Task 1 concluída no commit `f4cff00` (`test: incorporar provas de persistencia a
  suite padrao`).
- 2026-08-10 — removidos os sete arquivos de `src/spike-test` e o profile Maven
  `spike-persistencia-duravel`; busca em configuração e fontes executáveis não encontrou
  referências residuais ao source set, profile, recurso Valkey duplicado ou mapper experimental.
- 2026-08-10 — após a remoção, `mvn -q test` concluiu a suíte padrão em 214 s com código 0;
  permaneceu somente o erro de formatação do JBoss LogManager já observado no baseline.
- 2026-08-10 — checkpoint SonarQube final `COMPLIANT`: 240 issues totais iguais ao baseline,
  zero issues novas, nenhuma `HIGH`/`BLOCKER`, cobertura 85,3%, duplicação 2,6%, nenhuma
  violação e decisão humana técnica não requerida.
- 2026-08-10 — revisão final sem achados bloqueantes em correção, simplicidade, arquitetura,
  segurança, desempenho, testes ou escopo; a remoção não alterou produção, runtime ou contratos.
- 2026-08-10 — usuário aprovou o resultado e registrou explicitamente o encerramento da feature.
