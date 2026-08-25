# Checklist: implementar consulta de documentos do dossiê via porta CDI

## Estado

- **Branch:** `feature/implementar-consulta-dossie-via-porta`
- **Escopo:** código, testes, guardrail e arquitetura consolidada
- **Próximo item:** nenhum — feature encerrada pelo usuário

## Checklist

- [x] 0.1 Confirmar mesmo artifact/runtime e package `br.gov.caixa.simtr.dossie`;
- [x] 0.2 Ler arquitetura, índice e ADRs aplicáveis;
- [x] 0.3 Inspecionar porta, modelos, CDI, producer, observabilidade e testes;
- [x] 0.4 Separar implementação da PR documental #17;
- [x] 0.5 Atualizar plano, riscos, verificações e arquivos prováveis;
- [x] C0.1 Aprovar o contrato CDI local proposto;
- [x] C0.2 Aprovar dependência arquitetural somente pela porta de entrada;
- [x] C0.3 Aprovar postura de segurança sem nova borda ou telemetria com PII;
- [x] C0.4 Aprovar wrapper observável existente, sem novo span/log;
- [x] GO Registrar autorização humana antes de `src/`;
- [x] B0.1 Verificar pacotes em `sonar/` e registrar a fonte quando aplicável;
- [x] B0.2 Inicializar baseline SonarQube antes do teste RED;
- [x] 1.1 Escrever e executar teste unitário RED;
- [x] 1.2 Implementar bean CDI mínimo e obter GREEN;
- [x] C1 Revisar a primeira fatia;
- [x] 2.1 Escrever e obter GREEN no teste de wiring CDI;
- [x] 2.2 Ampliar ArchUnit para `br.gov.caixa.simtr`;
- [x] 2.3 Adicionar regra e prova negativa do limite do package irmão;
- [x] C2 Executar testes focados e revisar os limites;
- [x] 3.1 Executar suíte Maven completa;
- [x] 3.2 Executar checkpoint SonarQube;
- [x] 3.3 Atualizar arquitetura consolidada sem formatos derivados;
- [x] C3 Revisar diff final e apresentar evidências;
- [x] CF Registrar encerramento decidido pelo usuário.

## Decisões humanas

| Checkpoint | Status | Data | Evidência | Aprovador |
|---|---|---|---|---|
| Premissa | CONFIRMADO | 2026-08-24 | Mesmo runtime; package `br.gov.caixa.simtr.dossie` | usuário |
| Contrato CDI | APROVADO | 2026-08-25 | Bean e método propostos no plano | usuário |
| Arquitetura | APROVADO | 2026-08-25 | Dependência somente da porta e tipos da assinatura | usuário |
| Segurança | APROVADO | 2026-08-25 | Sem nova borda e sem PII em telemetria | usuário |
| Observabilidade | APROVADO | 2026-08-25 | Wrapper existente; sem novo span/log | usuário |
| GO | APROVADO | 2026-08-25 | GO informado explicitamente | usuário |
| Sonar 3.2 | CONTINUE_ADJUSTMENTS | 2026-08-25 | Primeira execução com 1 issue nova `java:S5778`; ajuste solicitado explicitamente | usuário |
| CF | APROVADO | 2026-08-25 | Encerramento aprovado explicitamente após a apresentação das evidências de C3 | usuário |

## Evidências

| Item | Data | Evidência |
|---|---|---|
| 0.3 | 2026-08-24 | A porta correta é `ConsultarDocumentosDossieProduto`; o bean observável implementa a interface e usa a saída produzida |
| 0.4 | 2026-08-24 | Branch de implementação separada; PR #17 continua isolada |
| 0.5 | 2026-08-24 | Plano cobre TDD, wiring CDI, ArchUnit, Maven e SonarQube |
| B0.1 | 2026-08-25 | Diretório `sonar/` inexistente; baseline formado somente pelo SonarQube Docker local |
| B0.2 | 2026-08-25 | Baseline local registrado: 645 testes verdes; 213 issues abertas incorporadas, 0 novas, 0 HIGH/BLOCKER/CRITICAL, cobertura 86,7% e duplicação 4% |
| 1.1 | 2026-08-25 | `mvn -q -Dtest=ConsultaDocumentosDossieProdutoTest test` confirmou RED apenas por `ConsultaDocumentosDossieProduto` ainda não existir |
| 1.2 | 2026-08-25 | Teste focado GREEN: 3 executados, 0 falhas, 0 erros e 0 ignorados; bean delega diretamente à porta sem bloquear ou transformar o `Uni` |
| C1 | 2026-08-25 | Revisão em correção, simplicidade, arquitetura, segurança e desempenho sem achados; suíte completa GREEN com 648 testes; checkpoint SonarQube conforme com 0 issues novas, 0 HIGH/BLOCKER/CRITICAL, cobertura 86,6% e duplicação 4% |
| 2.1 | 2026-08-25 | `mvn -q -Dtest=ConsultaDocumentosDossieProdutoQuarkusTest test` GREEN: 1 executado, 0 falhas, 0 erros e 0 ignorados; Quarkus resolveu o consumidor e a porta observável sem ambiguidade e sem HTTP |
| 2.2 | 2026-08-25 | Prova de escopo RED com 34 testes e falha única em `escopoIncluiPackageIrmao`; após importar `br.gov.caixa.simtr`, `mvn -q -Dtest=ArchUnitProgressivoTest test` ficou GREEN com 34 executados, 0 falhas, 0 erros e 0 ignorados |
| 2.3 | 2026-08-25 | Testes RED por ausência da nova regra; allowlist limita `br.gov.caixa.simtr.dossie..` à porta de entrada e aos modelos de `dossieproduto` dentro do Hub; GREEN com 36 testes ArchUnit, 0 falhas, 0 erros e 0 ignorados, incluindo prova negativa do caso de uso concreto |
| C2 | 2026-08-25 | Testes focados de consumidor, CDI, observabilidade e arquitetura GREEN: 44 executados, 0 falhas, 0 erros e 0 ignorados; revisão de correção, simplicidade, arquitetura, segurança e desempenho sem achados obrigatórios ou críticos |
| 3.1 | 2026-08-25 | `mvn -q test` GREEN: 151 conjuntos e 652 testes executados, 0 falhas, 0 erros e 0 ignorados |
| 3.2 | 2026-08-25 | Primeira execução `NON_COMPLIANT` por 1 issue nova `java:S5778`; decisão `ContinuarAjustes` registrada; lambda refatorada sem alterar a prova negativa; teste ArchUnit GREEN com 36 testes; repetição do checkpoint `COMPLIANT` com 652 testes, 213 issues abertas iguais ao baseline, 0 novas, 0 HIGH/BLOCKER/CRITICAL, cobertura 86,6% e duplicação 4% |
| 3.3 | 2026-08-25 | Arquitetura consolidada atualizada para registrar o consumidor CDI local, sua entrada pela porta observável e a allowlist ArchUnit do package irmão; nenhum `.ppt`, `.pptx`, `.pdf` ou `.html` foi alterado; Maven e SonarQube não foram repetidos porque o incremento é exclusivamente Markdown |
| C3 | 2026-08-25 | Revisão final de correção, simplicidade, arquitetura, segurança, desempenho, testes e escopo sem achados `Critical` ou obrigatórios; bean delega diretamente à porta e preserva `Uni`, lista, vazio e falha; CDI resolve o wrapper observável; ArchUnit cobre o package irmão com prova negativa; nenhuma nova borda, telemetria, dependência ou operação bloqueante; 151 relatórios e 652 testes com 0 falhas/erros/ignorados; SonarQube `COMPLIANT` com 0 issues novas, cobertura 86,6% e duplicação 4%; `git diff --check` sem erro |
| CF | 2026-08-25 | Usuário declarou explicitamente: “Aprovo o encerramento da feature.” |
