# Checklist: guia permanente para novas features

## Estado

- **Branch:** `docs/agent-guidance-features`
- **Escopo:** exclusivamente documental
- **Próximo item:** nenhum; feature encerrada com GO humano no C1

## Checklist

- [x] 0.1 Confirmar a intenção por entrevista;
- [x] 0.2 Consolidar requisitos, restrições e fora de escopo;
- [x] 0.3 Inventariar `AGENTS.md`, arquitetura e tasks históricas;
- [x] 0.4 Registrar plano, estrutura alvo, critérios e riscos;
- [x] C0 Registrar GO humano para iniciar a execução;
- [x] 1.1 Consolidar a arquitetura atual;
- [x] 2.1 Criar índice e ADRs com descrição de leitura rápida;
- [x] C-A Validar coerência arquitetural documental;
- [x] 3.1 Reescrever `AGENTS.md` como guia permanente;
- [x] 4.1 Criar README e templates permanentes de features;
- [x] 4.2 Remover tasks históricas somente após preservar decisões;
- [x] C-B Executar validação documental final;
- [x] C1 Registrar decisão humana de encerramento.

## Decisões humanas

| Checkpoint | Status | Data | Evidência | Aprovador |
|---|---|---|---|---|
| INTENÇÃO | CONFIRMADA | 2026-07-19 | Guia permanente; tasks e narrativa histórica removíveis; consolidado e ADRs preservam decisões; índice e topo dos ADRs orientam sem leitura integral; Sonar mantido | Usuário, confirmação explícita em conversa |
| C0 | GO | 2026-07-19 | Plano, estrutura alvo, exclusões e preservação do protocolo Sonar revisados | Usuário, GO registrado em conversa |
| C-A | REGISTRADO | 2026-07-19 | Consolidado sem narrativa de fases; oito ADRs com status, resumo e aplicabilidade; nove links do índice válidos; nenhuma nova decisão arquitetural introduzida | Gate documental |
| C-B | REGISTRADO | 2026-07-19 | Exatamente 19 tasks históricas removidas após migração dos sinais vigentes; raiz de `tasks/` contém apenas README, templates e feature atual; 19 documentos com links locais válidos; nenhuma referência operacional residual; protocolo Sonar preservado; diff exclusivamente Markdown e derivados intocados | Gate documental |
| C1 | GO | 2026-07-19 | Evidência final revisada; guia permanente, consolidado, oito ADRs, catálogo de observabilidade e estrutura de tasks aprovados; feature documental encerrada | Usuário, GO registrado em conversa |
