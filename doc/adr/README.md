# Índice de decisões arquiteturais

Leia este índice depois da [arquitetura atual](../arquitetura-ddd-integracoes-atomicas.md). As
descrições abaixo devem permitir identificar as decisões relevantes sem abrir todos os ADRs.
Leia o arquivo completo somente quando a coluna **Consultar quando** alcançar a mudança planejada
ou quando restar dúvida.

| ADR | Status | Descrição suficiente para triagem | Consultar quando |
|---|---|---|---|
| [0001 — Monólito modular e arquitetura hexagonal pragmática](0001-monolito-modular-e-hexagonal.md) | Aceito | O Hub permanece um monólito modular organizado por domínio; portas e adapters protegem responsabilidades e direção de dependência, mas não proíbem Quarkus no núcleo. | Alterar packages, camadas, dependências, modularização ou regras ArchUnit. |
| [0002 — Limites por domínio e capacidade](0002-limites-por-dominio-e-capacidade.md) | Aceito | Cada domínio é dono de capacidades atômicas e modelos próprios; nomes de sistemas externos, como `parametrizacao`, não criam domínios internos compartilhados. | Criar capacidade, mover tipo entre domínios, compartilhar modelo ou definir ownership. |
| [0003 — Orquestração e colaboração por portas](0003-orquestracao-e-colaboracao-por-portas.md) | Aceito | Entradas acionam portas; composição local usa casos de uso/portas, nunca REST local; colaboração entre domínios passa por porta do consumidor e camada anticorrupção. | Criar workflow, orquestrador, composição ou chamada entre domínios. |
| [0004 — Contratos independentes por borda](0004-contratos-independentes-por-borda.md) | Aceito | REST, MTR, simulador e MCP possuem DTOs e mappers próprios; a única exceção compartilhada é o contrato técnico de erro REST em `arquitetura.excecao.dto`. | Alterar DTO, JSON, mapper, erro público ou reutilização de contrato. |
| [0005 — Integrações MTR, simulador e fault tolerance](0005-integracoes-mtr-simulador-e-fault-tolerance.md) | Aceito | MTR e simulador implementam as mesmas portas com contratos próprios; timeout/retry/circuit breaker e classificação de falhas ficam na chamada MTR interceptada. | Alterar integração, seleção CDI, simulador, retry, timeout, circuit breaker ou tradução de erro. |
| [0006 — Compatibilidade, observabilidade e testes](0006-compatibilidade-observabilidade-e-testes.md) | Aceito | Contratos públicos e sinais observáveis só mudam explicitamente; caracterização, TDD em fatias, testes ponta a ponta e ArchUnit protegem a evolução. | Mudar path, JSON, validação, configuração, span, log, atributo ou estratégia de testes. |
| [0007 — Idempotência antes de fluxos mutáveis](0007-idempotencia-antes-de-fluxos-mutaveis.md) | Aceito | Operações MTR mutáveis com retry não podem ser compostas ou repetidas por workflows/agentes sem evidência ou estratégia de idempotência. | Criar fluxo, automação ou agente que use criação, inclusão de documento ou avanço de workflow. |
| [0008 — MCP como borda futura](0008-mcp-como-borda-futura.md) | Aceito | MCP, se aprovado, será adapter de entrada para portas existentes, com contrato e segurança próprios; não está implementado nem autorizado por este ADR. | Propor MCP Server, tool, agente consumidor ou exposição de capacidade a agentes. |

## Ciclo de vida

- ADR novo começa como **Proposto** e exige checkpoint humano de arquitetura.
- Depois do GO, muda para **Aceito**.
- Uma decisão substituída não é apagada: recebe status **Substituído por ADR-NNNN**.
- O índice deve ser atualizado no mesmo incremento do ADR.
