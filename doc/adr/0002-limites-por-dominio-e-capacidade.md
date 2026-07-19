# ADR-0002: Limites por domínio e capacidade

- **Status:** Aceito
- **Decisão em uma frase:** cada domínio é dono de capacidades atômicas e contratos internos
  próprios; o nome de um fornecedor ou sistema upstream não define domínio compartilhado.
- **Quando consultar:** criação ou movimentação de capacidade, definição de ownership,
  compartilhamento de modelo ou colaboração entre `arvoredocumento`, `conformidade`,
  `dossieproduto` e `gestaodocumento`.

## Contexto

APIs externas podem agrupar operações sob um nome técnico que não representa responsabilidade de
negócio no Hub. O MTR chama de `parametrizacao` tanto a consulta usada pela árvore documental
quanto a consulta de checklist, mas os consumidores, ritmos de mudança e modelos são diferentes.

## Decisão

Os limites atuais são:

- `arvoredocumento`: consulta de processo parametrizado;
- `conformidade`: consulta de checklist;
- `dossieproduto`: cinco operações atômicas do ciclo do dossiê;
- `gestaodocumento`: obtenção da credencial de container.

`parametrizacao` permanece somente como nome de integração na borda MTR. Modelos e mappers não são
compartilhados automaticamente entre consumidores. Pequena duplicação de contrato na borda é
preferível a um acoplamento que confunda ownership ou impeça evolução independente.

Novas capacidades recebem um domínio responsável, uma porta de entrada e tipos semânticos. Não se
criam aggregates, estados ou serviços genéricos antes de requisitos concretos.

## Consequências

- mudanças do fornecedor ficam confinadas à borda de cada consumidor;
- ownership e linguagem do negócio permanecem explícitos;
- colaboração entre domínios exige contrato e tradução deliberados;
- duplicação estrutural entre fronteiras não é removida só por semelhança textual.

## Alternativas rejeitadas

- **Domínio compartilhado `parametrizacao`:** modela o fornecedor em vez do negócio.
- **Modelo comum para operações semelhantes:** cria mudança coordenada entre contextos distintos.
- **Aggregate preventivo:** inventa invariantes e estados ainda desconhecidos.
