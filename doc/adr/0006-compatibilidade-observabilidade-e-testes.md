# ADR-0006: Compatibilidade, observabilidade e testes

- **Status:** Aceito
- **Decisão em uma frase:** contratos públicos, configuração e sinais observáveis só mudam de
  forma explícita e são protegidos por caracterização, TDD em fatias e guardrails arquiteturais.
- **Quando consultar:** mudanças de path, status, JSON, validação, configuração, OpenAPI, span,
  log, atributo, package ou estratégia de testes.

## Contexto

Uma reorganização interna pode alterar comportamento sem mudar a intenção do autor: nomes de
classes podem alimentar spans por reflexão; um mapper pode perder campos de erro; validação pode
falhar antes do Resource; configuração pode escolher outro adapter. Testes apenas unitários não
capturam todas essas bordas.

## Decisão

Cada incremento relevante começa pela caracterização do comportamento atual e segue uma fatia
vertical pequena. Conforme o risco, testes cobrem:

- método, path, status, JSON e Jakarta Validation;
- mapeamento REST -> interno -> MTR e retorno;
- payload real do REST Client, headers e providers;
- simulador habilitado/desabilitado e bootstrap CDI;
- tradução de erros ponta a ponta;
- matriz de timeout, retry e circuit breaker;
- propriedades, defaults e profiles;
- nomes de spans, eventos e atributos contratuais;
- regras ArchUnit de dependência.

OpenAPI é gerado pelo Quarkus a partir do código; testes protegem annotations e contratos Java, não
um artefato estático manipulado. Mudança de contrato, arquitetura, segurança ou comportamento
observável exige checkpoint humano adicional.

## Consequências

- refactors e features distinguem mudança interna de mudança observável;
- regressões são localizadas por borda e capacidade;
- sinais operacionais são tratados como compatibilidade, não detalhe cosmético;
- testes não são removidos ou desabilitados para fazer uma mudança passar.

## Alternativas rejeitadas

- **Confiar apenas na suíte final:** descobre divergências tarde e sem localização.
- **Testar somente simulador:** não prova contrato, providers ou FT do MTR.
- **Snapshot estático de OpenAPI como fonte:** duplica a geração oficial do Quarkus.
- **Renomear sinais junto com feature:** mistura comportamento operacional e funcional.
