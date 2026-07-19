# Planejamento de features

`tasks/` contém somente planejamento e decisões de trabalho atuais ou futuras. Contexto
arquitetural permanente pertence a `doc/arquitetura-ddd-integracoes-atomicas.md` e `doc/adr/`.

## Estrutura

```text
tasks/
|-- README.md
|-- templates/
|   |-- plan.md
|   `-- todo.md
`-- features/
    `-- <nome-curto>/
        |-- plan.md
        `-- todo.md
```

Use um nome curto em kebab-case e a mesma identificação na branch, quando possível.

## Início de uma feature

1. Leia o consolidado arquitetural e o índice de ADRs.
2. Inspecione código, contratos e testes relacionados ao pedido.
3. Crie uma branch específica; não trabalhe em `main`.
4. Copie os templates para `tasks/features/<nome>/`.
5. Preencha intenção, escopo, critérios, riscos, verificações e checkpoints.
6. Registre no `todo.md` que o plano aguarda GO.
7. Obtenha GO humano antes da primeira alteração de produção.

Planejamento e documentação podem anteceder o GO de produção. Exclusões, decisões arquiteturais e
outras mudanças materiais devem aparecer explicitamente no plano para que a autorização seja
informada.

## Execução

- retome apenas o próximo item pendente;
- mantenha no máximo um incremento coerente em andamento;
- atualize o checklist e a tabela de decisões com evidência verificável;
- execute checkpoints adicionais antes de mudar contrato, arquitetura, segurança ou comportamento
  observável;
- se o escopo mudar, atualize o plano antes de continuar;
- aplique o fluxo SonarQube definido em `AGENTS.md` para código e tooling executável.

## Encerramento

Uma feature só termina quando critérios e verificações estão concluídos, decisões técnicas
pendentes receberam resposta humana e o checkpoint final foi registrado. O diretório da feature
permanece como evidência concisa; não replique nele documentação arquitetural ou logs extensos.
