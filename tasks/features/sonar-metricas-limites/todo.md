# Checklist: adequar métricas SonarQube aos limites do agente

## Estado

- **Branch:** `refactor/sonar-metricas-limites`
- **Escopo:** cobertura >= 85% e duplicação <= 5%; encerrar e publicar ao comprovar ambas
- **Próximo item:** nenhum; planejamento encerrado

## Checklist

- [x] 0.1 Confirmar intenção e limites de sucesso;
- [x] 0.2 Ler arquitetura, índice e ADRs 0001, 0002, 0004 e 0006;
- [x] 0.3 Inspecionar código, contratos, hotspots e testes relacionados;
- [x] 0.4 Executar `clean verify`/JaCoCo e baseline no SonarQube Docker local;
- [x] 0.5 Confirmar ausência de `sonar/` e registrar fonte local sem pacote offline;
- [x] S0 Registrar `ContinuarAjustes` para o baseline `NON_COMPLIANT`;
- [x] 0.6 Registrar plano, riscos, lotes adaptativos e checkpoints;
- [x] C0 Registrar GO humano antes da primeira alteração em `src/`;
- [x] 1.1 Caracterizar as cinco falhas de domínio de dossiê;
- [x] 1.2 Completar cobertura nos adapters Documento/Formulario/Criacao;
- [x] S1.2 Executar o checkpoint Sonar intermediário da Task 1.2;
- [x] 1.3 Completar cobertura nos adapters Processo/Checklist/GestaoDocumento;
- [x] 1.4 Executar lote de cobertura contingencial somente se necessário;
- [x] SCOV Validar cobertura com suíte, JaCoCo e checkpoint Sonar;
- [x] 2.1 Preparar desenho mínimo de deduplicação dentro de `dossieproduto`;
- [x] C1 Obter GO arquitetural antes da deduplicação de produção;
- [x] 2.2 Extrair estrutura comum e migrar criação/formulário;
- [x] S2.2 Executar checkpoint Sonar da primeira fatia;
- [x] 2.3 Migrar documento/validação/workflow e manter visível a `java:S1185` intencional;
- [x] SDUP Validar duplicação, cobertura, issues e Quality Gate;
- [x] C2 Avaliar e autorizar contingência MTR somente se SDUP não bastar — dispensada;
- [x] 3.1 Executar contingência aprovada, se aplicável — não aplicável;
- [x] CMET Confirmar cobertura >= 85% e duplicação <= 5% no mesmo snapshot;
- [x] CF.1 Revisar o diff e finalizar `plan.md`/`todo.md` com as evidências;
- [x] CF Registrar GO humano de encerramento;
- [x] CF.2 Criar o commit final;
- [x] CF.3 Enviar a branch ao remoto.

## Evidência inicial

| Métrica | Baseline | Limite | Situação |
|---|---:|---:|---|
| Cobertura | 80,0% | >= 85,0% | fora do limite |
| Cobertura de linhas | 88,5% | informativo | — |
| Cobertura de branches | 60,2% | informativo | principal déficit |
| Duplicação | 5,9% | <= 5,0% | fora do limite |
| Issues novas | 0 | 0 | conforme |
| Issues severas | 0 | 0 | conforme |
| Issues MEDIUM | 137 (82 teste; 55 produção) | informativo | fora do escopo revisado |
| Testes | 348 aprovados | todos aprovados | conforme |
| Quality Gate | OK | OK | conforme |

## Evidências de execução

| Item | Data | Evidência |
|---|---|---|
| 1.1 | 2026-07-19 | `FalhasDossieProdutoTest`: 5 testes aprovados, sem falhas, erros ou skips; nenhuma alteração de produção |
| 1.2 | 2026-07-19 | `clean verify`: 372 testes aprovados; JaCoCo reduziu branches ausentes de Documento 28→0, Formulário 24→0 e Criação 23→0; nenhuma alteração de produção |
| S1.2 | 2026-07-20 | Análise `54103af7-494f-4548-937d-6bc021166f70`: 372 testes aprovados; cobertura 83,1%; duplicação 5,9%; zero issue nova/severa; Quality Gate `OK`; `ContinuarAjustes` registrado |
| 1.3 | 2026-07-20 | 30 testes focados aprovados; branches ausentes de Processo 24→0, Checklist 24→0 e Gestão de Documento 21→0; revisão multi-eixo e `git diff --check` aprovados; nenhuma alteração de produção |
| 1.4 | 2026-07-20 | Lote contingencial dispensado: `mvn -q clean verify` aprovou 390 testes, sem falhas, erros ou skips; JaCoCo registrou 3.321/3.534 linhas e 1.098/1.506 branches cobertos, equivalentes a 87,7% pela fórmula de cobertura do Sonar; nenhuma alteração adicional necessária |
| SCOV | 2026-07-20 | Análise `188f1958-e94b-451e-ada3-3b0c1adb5fcd`: 390 testes aprovados; cobertura 87,7%; duplicação 5,9%; 214 issues, igual ao baseline; zero issue nova/severa; Quality Gate `OK`; situação `NON_COMPLIANT` exclusivamente pela duplicação; `ContinuarAjustes` registrado |
| 2.1 | 2026-07-20 | Desenho restrito a `dossieproduto.dominio.erro`: superclasse package-private não genérica com dados internos (`Throwable` não admite subclasse genérica), cinco classes públicas finais preservadas e migração em duas fatias; protótipo confirmou compatibilidade fonte/binária dos métodos; CPD atual confirmado em 298 linhas nas candidatas e redução estimada de cerca de 175 linhas; nenhuma alteração de produção |
| 2.2 | 2026-07-20 | Criada `FalhaDossieProduto` package-private e migradas somente criação/formulário; testes focados aprovados; `clean verify` aprovou 390 testes, sem falhas, erros ou skips; JaCoCo permaneceu em 87,7% (3.308/3.520 linhas e 1.092/1.498 branches); `javap` confirmou construtores, retornos, descritores e `mensagens(): List<String>`; revisão multi-eixo sem achados pendentes |
| S2.2 | 2026-07-20 | Análise `e53aa580-fa75-4c9f-9b64-610fe25b712e`: 390 testes aprovados; cobertura 87,7%; duplicação 5,1%; 216 issues, sendo 2 novas `MINOR`/`java:S1185` nos overrides de `mensagens()` mantidos para preservar a assinatura genérica pública; zero issue severa; Quality Gate `ERROR` exclusivamente por `new_violations=2`; situação `NON_COMPLIANT`; `ContinuarAjustes` registrado |
| 2.3 | 2026-07-20 | Migradas inclusão de documento, validação negocial e workflow para `FalhaDossieProduto`; os cinco overrides de `mensagens()` foram mantidos para preservar a compatibilidade genérica e, por decisão posterior do usuário, permanecem sem supressão de `java:S1185`; testes focados aprovados; `clean verify` aprovou 390 testes, sem falhas, erros ou skips, em 134 s após um primeiro ensaio encerrado apenas pelo limite operacional de 180 s; JaCoCo registrou 3.257/3.465 linhas e 1.078/1.474 branches, equivalentes a 87,8% pela fórmula do Sonar; `javap` confirmou construtores, descritores, accessors e `mensagens(): List<String>` nas cinco classes; revisão de correção, simplicidade, arquitetura, segurança e desempenho sem achados pendentes |
| SDUP-A | 2026-07-20 | Análise `3078c6be-5189-4035-a854-3ec180df0432`: cobertura 87,8%; duplicação 3,7%; 214 issues, zero nova/severa; situação `COMPLIANT`. Snapshot descartado como resultado final porque continha cinco `@SuppressWarnings("java:S1185")`, proibidos pelo usuário; SDUP será repetido sem supressões |
| SDUP | 2026-07-20 | Análise `b57f8a93-f6e7-4553-bcd8-c382bbd8a908`: 390 testes aprovados; cobertura 87,8%; duplicação 3,7%; 219 issues, sendo 5 novas `MINOR` com impacto `MAINTAINABILITY: LOW`, todas `java:S1185` nos overrides de `mensagens()` mantidos por compatibilidade; zero issue severa; Quality Gate `ERROR` exclusivamente por `new_violations=5`; situação `NON_COMPLIANT`; `AceitarExcepcionalmente` registrado |
| 3.1 | 2026-07-20 | Contingência MTR não executada: C2 foi dispensado porque o SDUP já comprovou duplicação de 3,7%, abaixo do limite de 5%; nenhuma alteração adicional de código ou teste |
| CMET | 2026-07-20 | Análise `aa8083cf-27f1-4b54-80c2-96efd1b75ffe`, fingerprint `5b82c48cc25fb4edac21905926dd60fa5e6b6bacca28372de6b1a5991f1d32b7`: 390 testes aprovados; cobertura 87,8% e duplicação 3,7% no mesmo snapshot, ambas conformes; 219 issues, sendo as mesmas 5 novas `java:S1185` `MINOR`/`MAINTAINABILITY: LOW` aceitas no SDUP; zero issue severa; Quality Gate `ERROR` exclusivamente por `new_violations=5`; situação `NON_COMPLIANT`; `AceitarExcepcionalmente` registrado |
| CF.1 | 2026-07-20 | Revisão final de correção, simplicidade, arquitetura, segurança, desempenho e escopo sem achados pendentes; `javap` confirmou construtores, tipos de retorno e descritores públicos das cinco falhas; nenhuma supressão `java:S1185`; `git diff --check` aprovado, somente paths autorizados no worktree e nenhum derivado, relatório, estado local ou credencial incluído; arquitetura consolidada permanece válida porque a extração é package-private no domínio existente e não muda responsabilidade nem fronteira |

## Decisões humanas

| Checkpoint | Status | Data | Evidência | Aprovador |
|---|---|---|---|---|
| S0 | CONTINUAR_AJUSTES | 2026-07-19 | Baseline `faca645b-5b1c-4e6c-9ee9-c19d9da04d43`: cobertura 80,0%, duplicação 5,9%, zero issue nova/severa e Quality Gate OK | Usuário, decisão registrada pelo script |
| S0-R | CONTINUAR_AJUSTES | 2026-07-20 | Baseline reconstruído no mesmo commit, análise `488c3c0f-d18c-491c-b6d0-dd6c69546400`, com métricas idênticas | Usuário, decisão registrada pelo script |
| C0 | GO | 2026-07-19 | Plano aprovado; alterações em `src/` autorizadas a partir do próximo item pendente | Usuário, GO registrado em conversa |
| ESCOPO | REVISADO | 2026-07-20 | Encerrar quando cobertura e duplicação estiverem conformes; issues preexistentes ficam fora do escopo | Usuário, orientação registrada em conversa |
| S1.2 | CONTINUAR_AJUSTES | 2026-07-20 | Cobertura ainda abaixo de 85% e duplicação ainda acima de 5%; zero issue nova/severa | Usuário, decisão registrada pelo script |
| SCOV | CONTINUAR_AJUSTES | 2026-07-20 | Cobertura conforme em 87,7%; duplicação 5,9%; zero issue nova/severa; Quality Gate `OK` | Usuário, decisão registrada pelo script |
| C1 | GO | 2026-07-20 | Aprovada a superclasse package-private não genérica, com preservação dos contratos descritos e riscos reflexivos/serialização explicitados | Usuário, GO registrado em conversa |
| S2.2 | CONTINUAR_AJUSTES | 2026-07-20 | Cobertura conforme em 87,7%; duplicação reduzida a 5,1%, ainda 0,1 p.p. acima do limite; duas issues novas `MINOR` e Quality Gate `ERROR` | Usuário, decisão registrada pelo script |
| SDUP | ACEITAR_EXCEPCIONALMENTE | 2026-07-20 | Cobertura e duplicação conformes; aceitas excepcionalmente cinco issues novas `java:S1185`, todas `MINOR`/`MAINTAINABILITY: LOW`, mantidas visíveis e sem supressão; Quality Gate `ERROR` | Usuário, decisão registrada pelo script |
| C2 | DISPENSADO | 2026-07-20 | SDUP comprovou duplicação de 3,7%, abaixo do limite de 5%; a condição para autorizar contingência MTR não ocorreu | Usuário autorizou prosseguir; dispensa registrada por condição objetiva do plano |
| CMET | ACEITAR_EXCEPCIONALMENTE | 2026-07-20 | Cobertura 87,8% e duplicação 3,7% conformes no mesmo snapshot; aceitas novamente as mesmas cinco `java:S1185` LOW, mantidas visíveis e sem supressão | Usuário, decisão registrada pelo script |
| CF | GO | 2026-07-20 | Revisão CF.1 concluída sem achados pendentes; commit e push autorizados | Usuário, GO registrado em conversa |
