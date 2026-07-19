# ADR-0004: Contratos independentes por borda

- **Status:** Aceito
- **Decisão em uma frase:** cada borda possui DTOs e mappers próprios; contratos REST, MTR,
  simulador e MCP não são reutilizados entre si nem atravessam portas.
- **Quando consultar:** mudanças de DTO, JSON, validação, mapper, versão MTR, fixture, erro público
  ou proposta de compartilhar contratos.

## Contexto

API pública, fornecedor, simulador e futuros protocolos evoluem por motivos diferentes. Reutilizar
o mesmo DTO reduz linhas no curto prazo, mas transforma qualquer mudança de uma borda em impacto
nas demais e leva detalhes de protocolo ao núcleo.

## Decisão

O fluxo de dados é sempre:

```text
DTO da borda -> mapper da borda -> tipo interno -> mapper da outra borda -> DTO da outra borda
```

Não existe conversão direta REST -> MTR ou MCP -> MTR. DTO MTR é separado por versão/operação
quando necessário; fixture passa por DTO do simulador; futuros schemas MCP pertencem ao adapter
MCP.

A única exceção compartilhada é o contrato técnico de erro REST em
`arquitetura.excecao.dto`. Essa exceção é explícita, protegida por ArchUnit e proibida em domínio,
aplicação, MTR, simulador e MCP.

## Consequências

- contratos externos evoluem independentemente;
- mappers tornam traduções e perdas visíveis e testáveis;
- alguma duplicação estrutural entre bordas é intencional;
- mudança no package técnico de erro exige checkpoint arquitetural.

## Alternativas rejeitadas

- **DTO único em todas as camadas:** acopla negócio e protocolos.
- **Mapper direto entre bordas:** pula o significado interno e cria dependência lateral.
- **Generalizar qualquer erro compartilhado:** dilui a exceção técnica e contamina domínios.
