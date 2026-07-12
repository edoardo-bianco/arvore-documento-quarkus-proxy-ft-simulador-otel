# Checklist de execucao - refatoracao DDD

## Regra de retomada

Antes de executar qualquer item:

- [ ] ler `../doc/arquitetura-ddd-integracoes-atomicas.md`;
- [ ] confirmar `refactor/ddd-fase-0-baseline` com `git rev-parse --abbrev-ref HEAD`;
- [ ] se a Fase 0 ainda nao terminou, aplicar a regra de bootstrap de `plan.md` e seguir as
  dependencias 0.1–0.4; a exigencia de checkpoint `GO` comeca somente ao entrar na Fase 1;
- [ ] verificar `git diff` e preservar alteracoes do usuario;
- [ ] nao iniciar a fase seguinte sem GO humano.

## Pre-condicao Git

- [x] G0 Criar a branch local `refactor/ddd-fase-0-baseline` a partir de `main` atualizado.
- [x] G1 Branch `refactor/ddd-fase-0-baseline` confirmada; working tree continha somente a
  atualizacao documental da branch; baseline `mvn -q test`: 100 testes, zero falhas.

## Ponto de retomada

- **Tarefa atual:** 0.1 - EM ANDAMENTO.
- **Concluido:** baseline com 100 testes e zero falhas; 16 testes focados de caracterizacao
  REST aprovados para processo, checklist, cinco operacoes de dossie produto e credencial de
  gestao de documento; suite completa do checkpoint com 116 testes, zero falhas, zero erros e
  nenhum teste ignorado.
- **Comportamento registrado:** a resposta publica de checklist omite campos nulos, mesmo que
  esses campos existam no mock interno.
- **Pendente em 0.1:** caracterizacao semantica do OpenAPI e manifesto dos contratos HTTP.
- **Codigo de producao:** nenhuma alteracao.
- **Proximo passo:** concluir 0.1, executar novamente a suite completa e somente entao avaliar
  o item 0.1 para fechamento.

## Fase 0 - Baseline e guardrails

- [ ] 0.1 Congelar contratos HTTP dos oito endpoints.
- [ ] 0.2 Inventariar e proteger observabilidade contratual.
- [ ] 0.3 Criar stub MTR local com simulador desabilitado.
- [ ] 0.4 Adicionar ArchUnit progressivo.
- [ ] C0 Executar suite/build e obter GO humano.

## Fase 1 - Piloto workflow de dossie

- [ ] 1.1 Caracterizar REST, MTR, simulador, erros e fault tolerance.
- [ ] 1.2 Criar tipos internos, portas e caso de uso.
- [ ] 1.3 Criar adapter MTR e traducao de erros.
- [ ] 1.4 Criar adapter simulador e selecao CDI explicita.
- [ ] 1.5 Migrar Resource para a porta de entrada.
- [ ] C1 Executar suite/build/ArchUnit, revisar diff e obter GO humano.

## Fase 2 - `dossieproduto`

- [ ] 2.1a Caracterizar criacao.
- [ ] 2.1b Criar nucleo da criacao.
- [ ] 2.1c Criar borda MTR da criacao.
- [ ] 2.1d Criar borda simulador da criacao.
- [ ] 2.1e Migrar borda REST da criacao.
- [ ] C2.1 Registrar evidencias e obter GO humano.
- [ ] 2.2a Caracterizar formulario.
- [ ] 2.2b Criar nucleo do formulario.
- [ ] 2.2c Criar borda MTR do formulario.
- [ ] 2.2d Criar borda simulador do formulario.
- [ ] 2.2e Migrar borda REST do formulario.
- [ ] C2.2 Registrar evidencias e obter GO humano.
- [ ] 2.3a Caracterizar inclusao de documento.
- [ ] 2.3b Criar nucleo da inclusao de documento.
- [ ] 2.3c Criar borda MTR v2 da inclusao de documento.
- [ ] 2.3d Criar borda simulador da inclusao de documento.
- [ ] 2.3e Migrar borda REST da inclusao de documento.
- [ ] C2.3 Registrar evidencias e obter GO humano.
- [ ] 2.4a Caracterizar validacao negocial.
- [ ] 2.4b Criar nucleo da validacao negocial.
- [ ] 2.4c Criar borda MTR da validacao negocial.
- [ ] 2.4d Criar borda simulador da validacao negocial.
- [ ] 2.4e Migrar borda REST da validacao negocial.
- [ ] C2.4 Registrar evidencias e obter GO humano.
- [ ] 2.5 Remover artefatos legados sem referencias.
- [ ] C2 Executar suite/build/ArchUnit, revisar diff e obter GO humano.

## Fase 3 - `arvoredocumento`

- [ ] 3.1 Caracterizar consulta de processo.
- [ ] 3.2 Criar nucleo da consulta de processo.
- [ ] 3.3 Criar borda MTR de processo.
- [ ] 3.4 Criar borda simulador de processo.
- [ ] 3.5 Migrar borda REST de processo.
- [ ] 3.6 Ativar guardrails e remover legado sem uso.
- [ ] C3 Executar suite/build/ArchUnit, revisar diff e obter GO humano.

## Fase 4 - `conformidade`

- [ ] 4.1 Caracterizar consulta de checklist.
- [ ] 4.2 Criar nucleo da consulta de checklist.
- [ ] 4.3 Criar borda MTR de checklist.
- [ ] 4.4 Criar borda simulador de checklist.
- [ ] 4.5 Migrar borda REST de checklist.
- [ ] 4.6 Ativar guardrails e remover legado sem uso.
- [ ] C4 Executar suite/build/ArchUnit, revisar diff e obter GO humano.

## Fase 5 - `gestaodocumento`

- [ ] 5.1 Caracterizar obtencao de credencial.
- [ ] 5.2 Criar nucleo de credencial.
- [ ] 5.3 Criar borda MTR de credencial.
- [ ] 5.4 Criar borda simulador de credencial.
- [ ] 5.5 Migrar borda REST de credencial.
- [ ] 5.6 Provar ausencia de cache, renovacao e upload.
- [ ] C5 Executar suite/build/ArchUnit, revisar diff e obter GO humano.

## Fase 6 - Consolidacao

- [ ] 6.1 Remover package legado `parametrizacao` sem referencias.
- [ ] 6.2 Endurecer ArchUnit para todo o codigo migrado.
- [ ] 6.3 Executar verificacao completa de equivalencia.
- [ ] 6.4 Atualizar documentacao e dividas adiadas.
- [ ] C6 Obter aceite humano final.

## Registro de checkpoints

Esta tabela e a fonte autoritativa. Valores validos de status: `PENDENTE`, `GO` e `NO-GO`. Um
checkbox de checkpoint acima so pode ser marcado depois que a linha correspondente tiver `GO`,
data, evidencias verificaveis e aprovador humano.

| Checkpoint | Status | Data | Evidencias | Aprovador |
|---|---|---|---|---|
| C0 | PENDENTE | 2026-07-11 | G1: branch confirmada; baseline 100/0; demais evidencias pendentes | - |
| C1 | PENDENTE | - | - | - |
| C2.1 | PENDENTE | - | - | - |
| C2.2 | PENDENTE | - | - | - |
| C2.3 | PENDENTE | - | - | - |
| C2.4 | PENDENTE | - | - | - |
| C2 | PENDENTE | - | - | - |
| C3 | PENDENTE | - | - | - |
| C4 | PENDENTE | - | - | - |
| C5 | PENDENTE | - | - | - |
| C6 | PENDENTE | - | - | - |

## Bloqueios que nao podem ser resolvidos por suposicao

- [ ] Idempotencia das operacoes mutaveis do MTR antes de workflows futuros.
- [ ] Inclusao de qualquer endpoint ainda nao implementado.
- [ ] Escolha de engine, persistencia ou desenho de workflow.
- [ ] Mudanca de contrato publico, observabilidade ou comportamento do simulador.
