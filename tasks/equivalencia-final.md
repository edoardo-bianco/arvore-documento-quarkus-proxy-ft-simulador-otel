# Verificacao final de equivalencia da refatoracao DDD

## Escopo e resultado

Verificacao executada em 2026-07-15 na branch `refactor/ddd-fase-6-baseline`, depois da remocao do
package Java legado `parametrizacao` e do endurecimento final dos guardrails ArchUnit.

As oito capacidades existentes continuam protegidas por contratos executaveis de HTTP/JSON,
validacao, wire MTR, simulador, erros, fault tolerance e observabilidade. Nenhum endpoint,
workflow, upload, cache ou renovacao de SAS foi adicionado. A evidencia quantitativa de cobertura
permanece exclusivamente em `target/jacoco-report/index.html`.

O OpenAPI continua gerado exclusivamente pelo Quarkus a partir do codigo Java. Nao existe teste,
filtro, complemento, artefato estatico ou ocultacao de operacao introduzido pela refatoracao.

## Matriz por capacidade

| Dominio e capacidade | Manifesto da capacidade | Contrato HTTP | Wire MTR e FT | Simulador e configuracao |
|---|---|---|---|---|
| `dossieproduto` / `IniciarOuAvancarWorkflowDossieProduto` | `baseline-workflow-dossie-produto.md` | `DossieProdutoApiContractTest` e `DossieProdutoErroApiContractTest` | `WorkflowDossieProdutoMtrContractTest`, `WorkflowDossieProdutoNovoClientQuarkusTest` e testes do client/adapter | `WorkflowDossieProdutoSelecaoSimuladorQuarkusTest` |
| `dossieproduto` / `CriarDossieProduto` | `baseline-criacao-dossie-produto.md` | `DossieProdutoApiContractTest` e `DossieProdutoErroApiContractTest` | `CriacaoDossieProdutoNovoClientQuarkusTest` e testes do client/adapter | `CriacaoDossieProdutoSelecaoSimuladorQuarkusTest` |
| `dossieproduto` / `AtualizarFormularioDossieProduto` | `baseline-formulario-dossie-produto.md` | `DossieProdutoApiContractTest` e `DossieProdutoErroApiContractTest` | `FormularioDossieProdutoMtrContractTest` e testes do client/adapter | `FormularioDossieProdutoSelecaoSimuladorQuarkusTest` |
| `dossieproduto` / `IncluirDocumentoDossieProduto` | `baseline-documento-dossie-produto.md` | `DossieProdutoApiContractTest` e `DossieProdutoErroApiContractTest` | `DocumentoDossieProdutoMtrContractTest` e testes do client/adapter | `DocumentoDossieProdutoSelecaoSimuladorQuarkusTest` |
| `dossieproduto` / `RegistrarValidacaoNegocialDossieProduto` | `baseline-validacao-negocial-dossie-produto.md` | `DossieProdutoApiContractTest` e `DossieProdutoErroApiContractTest` | `ValidacaoNegocialDossieProdutoMtrContractTest` e testes do client/adapter | `ValidacaoNegocialDossieProdutoSelecaoSimuladorQuarkusTest` |
| `arvoredocumento` / `ConsultarProcessoParametrizado` | `baseline-consulta-processo-parametrizado.md` | `ProcessoApiContractTest` | `ProcessoParametrizadoNovoClientQuarkusTest` e testes do client/adapter | `ProcessoParametrizadoSelecaoSimuladorQuarkusTest` |
| `conformidade` / `ConsultarChecklist` | `baseline-consulta-checklist.md` | `ChecklistApiContractTest` | `ChecklistNovoClientQuarkusTest` e testes do client/adapter | `ChecklistSelecaoSimuladorQuarkusTest` |
| `gestaodocumento` / `ObterCredencialContainer` | `baseline-obter-credencial-container.md` | `GestaoDocumentoApiContractTest` | `GestaoDocumentoNovoClientQuarkusTest` e testes do client/adapter | `GestaoDocumentoSelecaoSimuladorQuarkusTest` |

Erros publicos lossless e validacoes permanecem nos contratos HTTP e nos mappers REST. Os sinais
transversais permanecem protegidos por `ObservabilidadeSpansContratoTest`,
`ObservabilidadeLogsContratoTest` e pelos contratos locais contra os stubs MTR.

## Gates transversais executados

- `mvn -q clean test` concluiu com codigo `0`; os relatorios Surefire nao registram falha, erro ou
  teste pulado.
- `ArchUnitProgressivoTest` protege dominio, aplicacao, adapters REST, DTOs por borda, erro REST
  tecnico, REST Clients, API publica de entrada e dependencias cross-domain via ACL.
- Provas negativas controladas confirmam que cada nova regra detecta a violacao correspondente.
- `rg` nao encontra arquivo, declaracao de package ou import Java do dominio legado
  `br.gov.caixa.simtr.hub.parametrizacao`.
- Os nomes `parametrizacao` preservados pertencem ao sistema externo: paths, config keys, fixtures
  MTR e sinais de telemetria contratuais.
- Properties e profiles de MTR/simulador permanecem sem alteracao; a branch da Fase 6 nao modifica
  producao nem configuracao.
- O diff final da Fase 6 fica restrito a guardrails, fixtures arquiteturais, documentacao e
  colecao Postman; nao altera producao nem configuracao funcional.

## Dividas mantidas fora do escopo

- validar idempotencia das operacoes mutaveis antes de qualquer workflow futuro;
- decidir separadamente eventual adocao de Quarkus Flow e persistencia de orquestracao.

## Evolucao posterior

A Fase 11 tratou separadamente os warnings Jakarta Validation de formulario e documento. A
colocacao de `@Valid` foi modernizada com contratos proprios, sem mudar o comportamento publico
consolidado neste documento.
