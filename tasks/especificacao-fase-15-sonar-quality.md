# Especificação — Fase 15: fechamento de qualidade SonarQube

Status: C15-SPEC aprovado pelo usuário. Plano e execução ainda aguardam GO.

Branch dedicada: `refactor/sonar-quality-fase-15-baseline`.

## Objetivo

Resolver de forma incremental o blocker, reduzir as 175 issues HIGH abertas e eliminar o
warning de blame em dez arquivos, sem comprometer comportamento funcional, contratos JSON,
endpoints, simulador, fault tolerance ou observabilidade.

## Baseline confirmado

- 1 BLOCKER (`java:S2699`) em `RestClientObservabilityFilterTest` por teste sem asserção.
- 175 issues com impacto HIGH.
- Cobertura: 80,0%, importando `target/jacoco-report/jacoco.xml` explicitamente.
- Duplicação: 5,8%; 25 blocos duplicados.
- Complexidade: 1356; complexidade cognitiva: 829.
- Warning: `Missing blame information for 10 files`.

## Escopo autorizado

1. Investigar e corrigir o blocker com uma asserção comportamental.
2. Identificar os dez arquivos sem blame e corrigir a causa SCM sem desabilitar o blame.
3. Classificar as HIGH por regra e componente.
4. Corrigir lotes pequenos e independentes, com caracterização e checkpoint humano.
5. Repetir o scanner com o caminho JaCoCo explícito e comparar os indicadores.

## Fora de escopo

- Novos endpoints, workflows, upload, cache SAS ou Quarkus Flow.
- Mudança de contratos públicos, paths, JSON, erros ou semântica de fault tolerance.
- Extração de utilitário compartilhado somente para reduzir duplicação.
- Alteração do package técnico compartilhado `arquitetura.excecao.dto` sem autorização específica.
- Desabilitar SCM/blame ou ignorar arquivos para mascarar o warning.

## Critérios de aceite

- Zero BLOCKER.
- Cada HIGH resolvida ou documentada com justificativa técnica e risco aceito.
- Cobertura permanece igual ou superior a 80%.
- Duplicação e complexidade não aumentam.
- Warning de blame eliminado ou explicado por arquivos comprovadamente fora do histórico.
- Suíte completa, testes focados e ArchUnit aprovados.
- Nenhum token Sonar persistido no repositório.

## Riscos

| Risco | Mitigação |
|---|---|
| Refatoração altera logs/spans | testes de contrato de observabilidade antes e depois |
| Redução artificial de HIGH | não usar supressão sem justificativa e revisão |
| Cobertura volta a zero | validar existência do XML e passar `sonar.coverage.jacoco.xmlReportPaths` |
| Blame continua ausente | verificar rastreamento Git e histórico antes do scanner |

