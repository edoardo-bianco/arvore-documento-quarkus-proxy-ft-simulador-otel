# Checklist: rastreabilidade durável do fluxo de dossiê

- [x] P0.1: registrar requisitos humanos, conferir arquitetura/ADRs/código e fontes da versão.
- [x] P0.2: documentar plano, dados, ownership e tratamento das janelas Cosmos/Service Bus.
- [x] P0.3: revisão independente do desenho concluída, com correções incorporadas e sem bloqueadores.
- [x] P0.4: CP-COSMOS humano: "GO para o desenho e implementação incremental", reiterado "GO ao ADR-0013" em 2026-09-12; ADR Aceito.
- [x] F0: correções do harness consolidadas; 41 integrações e 1.403 testes padrão aprovados, checkpoint atual COMPLIANT.
- [x] P1: gate de compatibilidade Cosmos/Dev Services local e configuração DES; suíte padrão e perfis isolados verificados, checkpoint COMPLIANT.
- [ ] P2: modelo, eventos e projeção sem regressão (subfatias detalhadas antes do RED).
- [ ] P3: persistência Cosmos com idempotência/ETag/batch, lifecycle, spans CLIENT e consulta pelas referências técnicas.
- [ ] P4: portas/ACLs e guardrails de colaboração.
- [ ] P5: diário e confirmação da mensagem inicial, preservando contrato POST.
- [ ] P6: entrada, situações, resultado e Complete; rastreamento 10.1-B2/B3.
- [ ] P7: reagendamento transacional e rastreamento 10.1-B4.
- [ ] P8: saída, log, Complete e rastreamento 10.1-B5.
- [ ] P9: observação de filas/DLQs e reconciliação por evidência.
- [ ] P10: provas integradas Cosmos/Service Bus/Jaeger, navegação trace/span ↔ evento/execução, roteiro e checkpoint.
- [ ] Aceitação/encerramento humano.

Próximo item: P2, conforme execucao-p1.md e guia-desenvolvimento.md. P1-R1/R2/R3 corrigiram o lifecycle dos peeks dos testes de integração. GO humano recebido para o desenho e implementação incremental. Escolha de Cosmos,
container doctree, chave Pré-Valida e uso de Dev Services já vieram do usuário.
Sem commit/push. [Plano](plan.md) e [dados/transições](especificacao.md).

Regra obrigatória: [observabilidade homogênea](observabilidade-homogenea.md), conforme refinamento humano após o GO.
