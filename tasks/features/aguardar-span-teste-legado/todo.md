# Checklist: aguardar span no teste legado

## Estado

- **Branch:** `fix/aguardar-span-teste-legado`
- **Escopo:** teste e documentação de execução
- **Próximo item:** nenhum — feature encerrada pelo usuário

## Checklist

- [x] 0.1 Confirmar intenção e critérios de sucesso;
- [x] 0.2 Ler arquitetura, índice e ADR-0006;
- [x] 0.3 Inspecionar código, contratos e testes relacionados;
- [x] 0.4 Registrar plano, riscos e checkpoints;
- [x] C0 Registrar GO humano antes da primeira alteração do teste;
- [x] 1.1 Inicializar baseline SonarQube local;
- [x] 2.1 Substituir a busca imediata por espera limitada do span;
- [x] 2.2 Executar teste focado e suíte Maven;
- [x] C1 Executar checkpoint SonarQube e tratar eventual `NON_COMPLIANT`;
- [x] 2.3 Remover `Thread.sleep()` do polling preservando retorno imediato e timeout;
- [x] 2.4 Executar novamente teste focado e suíte Maven;
- [x] C2 Executar novo checkpoint SonarQube e tratar eventual `NON_COMPLIANT`;
- [x] CF Revisar diff, registrar evidências e encerrar a correção.

## Decisões humanas

| Checkpoint | Status | Data | Evidência | Aprovador |
|---|---|---|---|---|
| C0 | GO | 2026-07-22 | Usuário respondeu explicitamente `GO` após revisar o plano | usuário |
| C1 | ContinuarAjustes | 2026-07-23 | Usuário escolheu explicitamente continuar os ajustes para remover a issue nova | usuário |
| CF | ENCERRADO | 2026-07-23 | Usuário respondeu explicitamente `encerrar` após revisar o resultado técnico | usuário |

## Evidências técnicas

- 2026-07-22 — baseline local não inicializado: o processo atual não herdou `SONAR_TOKEN`;
  nenhuma alteração foi feita em `src/test` e o próximo item permanece 1.1.
- 2026-07-23 — baseline local inicializado pelo script oficial: `READY`, fonte
  `LOCAL_SONAR`, situação técnica `COMPLIANT`, 219 issues no baseline, cobertura 87,8%,
  duplicação 3,7% e nenhuma violação.
- 2026-07-23 — busca imediata do span substituída no teste legado por polling de retorno
  imediato, limitado a cinco segundos e com nomes dos spans recebidos na mensagem de timeout;
  nenhum arquivo de produção foi alterado.
- 2026-07-23 — `mvn -q -Dtest=ConsultarProcessoParametrizadoLegadoContractTest test`
  concluído com código 0.
- 2026-07-23 — `mvn -q test` concluído com código 0; a saída manteve o erro de formatação
  do JBoss LogManager já observado no baseline, sem falha da suíte e fora do escopo desta
  correção.
- 2026-07-23 — checkpoint SonarQube concluído com código 0: situação técnica
  `NON_COMPLIANT`, 1 issue nova, nenhuma issue `HIGH`/`BLOCKER`, cobertura 87,8% e duplicação
  3,7%. Issue nova `java:S2925`, severidade `MAJOR`, impacto de manutenibilidade `MEDIUM`,
  em `ConsultarProcessoParametrizadoLegadoContractTest.java:209`: uso de `Thread.sleep()`.
  Decisão humana inicialmente `PENDING`.
- 2026-07-23 — usuário escolheu `ContinuarAjustes`; decisão registrada pelo script oficial.
  O próximo incremento permanece restrito ao polling do teste, sem dependência nova ou mudança
  em produção.
- 2026-07-23 — `Thread.sleep()` removido. O teste verifica primeiro o span já exportado e,
  somente quando necessário, usa polling agendado pelo JDK com `CompletableFuture`, intervalo
  de 10 ms, timeout de cinco segundos e encerramento automático do executor; nenhuma dependência
  foi adicionada.
- 2026-07-23 — após o ajuste, teste focado
  `mvn -q -Dtest=ConsultarProcessoParametrizadoLegadoContractTest test` concluído com código 0.
- 2026-07-23 — após o ajuste, `mvn -q test` concluído com código 0; permaneceu somente o erro
  de formatação do JBoss LogManager já registrado e fora do escopo.
- 2026-07-23 — primeira tentativa do C2 excedeu 120 segundos no wrapper e terminou sem
  atualizar o estado; o script foi repetido integralmente com limite ampliado.
- 2026-07-23 — C2 concluído pelo script oficial: situação técnica `COMPLIANT`, 219 issues
  totais iguais ao baseline, 0 issues novas, nenhuma issue `HIGH`/`BLOCKER`, cobertura 87,8%,
  duplicação 3,7% e nenhuma violação; decisão humana não requerida.
- 2026-07-23 — revisão final sem achados bloqueantes em correção, simplicidade, arquitetura,
  segurança, desempenho, testes ou escopo. O diff funcional altera somente o teste legado;
  `doc/sonar/sonar-issues-staging.csv` já estava modificado antes da implementação e permanece
  preservado fora do escopo.
- 2026-07-23 — usuário aprovou o resultado e registrou explicitamente o encerramento da feature.
