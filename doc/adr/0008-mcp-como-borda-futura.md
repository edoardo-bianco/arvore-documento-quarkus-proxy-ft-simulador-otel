# ADR-0008: MCP como borda futura

- **Status:** Aceito
- **Decisão em uma frase:** MCP, se futuramente aprovado, será adapter de entrada para portas
  existentes, com contratos e segurança próprios; nada de MCP está implementado por esta decisão.
- **Quando consultar:** propostas de MCP Server, tools, agentes consumidores, exposição de
  credenciais ou nova forma de entrada automatizada.

## Contexto

Agentes podem consumir capacidades do Hub sem que o protocolo defina o domínio. Acoplar MCP a
casos de uso ou fazer uma tool chamar Resource REST reutiliza transporte em vez de capacidade e
cria dependências laterais. Um serviço MCP separado também adiciona rede e operação antes de haver
necessidade comprovada.

## Decisão

Quando uma feature concreta for autorizada:

```text
tools/call
    -> adapter MCP
        -> mapper MCP
            -> comando interno
                -> porta de entrada existente
```

DTOs, schemas, nomes de tools, transporte, autenticação, autorização e tradução de erros pertencem
à borda MCP. A tool não chama Resource REST, REST Client, simulador ou adapter de saída. Uma tool
composta aciona a porta de um fluxo da aplicação, não duplica coordenação no adapter.

Cada capacidade exige decisão de exposição e menor privilégio. `ObterCredencialContainer` requer
avaliação de segurança própria. Tokens, credenciais, URLs internas, stacks e payloads sensíveis não
aparecem em respostas, logs, traces ou memória de conversa.

## Consequências

- REST e MCP podem evoluir independentemente sobre a mesma capacidade;
- regras de negócio corrigidas no caso de uso protegem todas as entradas;
- nova superfície exige testes de schema, autorização, erros, trace e dados sensíveis;
- um serviço MCP separado futuro será consumidor externo, não adapter interno com acesso ao núcleo.

## Alternativas rejeitadas

- **MCP no domínio/aplicação:** tecnologia de entrada passa a definir negócio.
- **Tool chamando REST local:** duplica protocolo e validação dentro do processo.
- **Serviço MCP separado desde já:** adiciona deployment e falhas distribuídas sem demanda.
- **Reutilizar DTO REST/MTR:** acopla contratos com ciclos de vida diferentes.
