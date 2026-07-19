# ADR-0007: Idempotência antes de fluxos mutáveis

- **Status:** Aceito
- **Decisão em uma frase:** operações MTR mutáveis com retry não podem ser repetidas ou compostas
  por workflows/agentes sem evidência ou estratégia explícita de idempotência.
- **Quando consultar:** automação de criação de dossiê, inclusão de documento, avanço de workflow,
  retries externos, orquestração, MCP ou retomada de fluxo.

## Contexto

Uma chamada mutável pode concluir no MTR e perder a resposta por timeout. O retry então repete uma
operação já realizada e pode duplicar efeitos. Esse risco aumenta quando um workflow ou agente
coordena etapas, repete comandos ou retoma execução após falha.

## Decisão

Antes de compor ou expor uma operação mutável com repetição automática, a feature deve apresentar
uma das evidências:

- garantia documentada de idempotência pelo MTR;
- chave idempotente aceita e persistida pela operação;
- estratégia equivalente, testável e aprovada para detectar/reconciliar repetição.

Sem essa evidência, o checkpoint de arquitetura/segurança fica bloqueado. A política existente de
retry não é removida silenciosamente como atalho, pois também é comportamento operacional atual.

## Consequências

- workflows e agentes não amplificam duplicidade conhecida;
- requisito de idempotência aparece no plano antes da implementação;
- testes precisam cobrir timeout depois de possível sucesso e retomada/repetição;
- uma correção pode exigir contrato ou persistência e, portanto, GO próprio.

## Alternativas rejeitadas

- **Assumir que POST/PATCH é seguro para repetir:** verbo HTTP não comprova idempotência upstream.
- **Desabilitar retry sem análise:** pode reduzir resiliência e mudar comportamento existente.
- **Resolver duplicidade só no orquestrador:** outros consumidores continuariam expostos.
