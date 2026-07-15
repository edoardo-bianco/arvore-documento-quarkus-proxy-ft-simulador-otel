# Manifesto de baseline dos contratos HTTP

> Documento historico da Fase 0. A decisao posterior removeu testes e fingerprints do OpenAPI:
> o artefato e gerado exclusivamente pelo Quarkus. Para o estado final, consulte
> `equivalencia-final.md`; evidencias quantitativas permanecem somente no JaCoCo.

## Escopo e evidencia

Este manifesto encerra somente a Task 0.1 da Fase 0. Ele descreve o comportamento observado na
implementacao original, antes de qualquer alteracao de codigo de producao. Os oraculos dos testes
nao serializam DTOs de producao nem leem os mocks usados em runtime.

- baseline anterior aos testes de contrato: 100 testes, zero falhas;
- testes HTTP/OpenAPI focados deste manifesto: 22;
- suite verificada apos a caracterizacao: 122 testes, zero falhas, zero erros e zero ignorados;
- perfil dos testes: simulador atual habilitado;
- codigo de producao alterado: nenhum.

Os testes executaveis estao em
`src/test/java/br/gov/caixa/simtr/hub/contrato`. `JsonContractAssertions` compara JSON estrutural
exato, valida o UUID dinamico dos erros e calcula fingerprints SHA-256 sobre JSON canonico. A ordem
dos objetos do array `erros` e normalizada porque variou entre os perfis dev e test; quantidade,
conteudo e todos os demais campos permanecem exatos.

## Capacidades protegidas

| Dominio alvo / capacidade | Contrato HTTP preservado | Resposta de sucesso | Validacoes e erros caracterizados | Testes executaveis |
|---|---|---|---|---|
| `arvoredocumento` / `ConsultarProcessoParametrizado` | `GET /simtr-hub/v1/processo/identificador-negocial/{identificador}` | `200 application/json`; arvore JSON completa, fingerprint `01d6ac0993410e4cb8edc9c338a333acd56fa93390054c31907351ca19d70d34` | identificador zero: `400` e erro publico completo | `ProcessoApiContractTest#preservaRespostaJsonCompletaDaConsultaDeProcesso`; `#preservaErroPublicoParaIdentificadorDeProcessoInvalido` |
| `conformidade` / `ConsultarChecklist` | `GET /simtr-hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}` | `200 application/json`; arvore JSON completa e omissao atual de campos nulos, fingerprint `fa497a3a7f7feea380be4312bb5ad8231c385c7a5aa10043b5092971fdb816aa` | identificador e versao zero: `400` e erros publicos completos | `ChecklistApiContractTest#preservaRespostaJsonCompletaDaConsultaDeChecklist`; `#preservaErroPublicoParaIdentificadorDeChecklistInvalido`; `#preservaErroPublicoParaVersaoDeChecklistInvalida` |
| `dossieproduto` / `CriarDossieProduto` | `POST /simtr-hub/v1/dossie-produto`; request JSON literal completo | `201 application/json`, `{"id":1}` | corpo ausente; campos obrigatorios; JSON malformado: `400` com corpo vazio | `DossieProdutoApiContractTest#preservaContratoDeCriacao`; `DossieProdutoErroApiContractTest#preservaErroParaCorpoDeCriacaoAusente`; `#preservaErrosCompletosParaCamposObrigatoriosDaCriacao`; `#preservaErroSemCorpoParaJsonMalformado` |
| `dossieproduto` / `AtualizarFormularioDossieProduto` | `PATCH /simtr-hub/v1/dossie-produto/{id}/formulario`; request JSON literal completo | `201 application/json`, `{"id":123}` | identificador zero: `400` e erro publico completo | `DossieProdutoApiContractTest#preservaContratoDeAtualizacaoDeFormulario`; `DossieProdutoErroApiContractTest#preservaErroParaIdInvalidoNoFormulario` |
| `dossieproduto` / `IncluirDocumentoDossieProduto` | `POST /simtr-hub/v1/dossie-produto/{id}/documento`; request JSON literal completo | `201 application/json`, `{"id_documento":456,"id_instancia_documento":789}` | identificador zero e validacao cascata de atributo: `400` e erros publicos completos | `DossieProdutoApiContractTest#preservaContratoDeInclusaoDeDocumento`; `DossieProdutoErroApiContractTest#preservaErroParaIdInvalidoNaInclusaoDeDocumento`; `#preservaErroDeValidacaoCascataDoAtributoDeDocumento` |
| `dossieproduto` / `RegistrarValidacaoNegocialDossieProduto` | `PATCH /simtr-hub/v1/dossie-produto/{id}/validacao-negocial`; request JSON literal completo | `200` com corpo vazio | identificador zero e validacao cascata da verificacao: `400` e erros publicos completos | `DossieProdutoApiContractTest#preservaContratoDeValidacaoNegocialSemCorpo`; `DossieProdutoErroApiContractTest#preservaErroParaIdInvalidoNaValidacaoNegocial`; `#preservaErroDeValidacaoCascataDaVerificacaoNegocial` |
| `dossieproduto` / `IniciarOuAvancarWorkflowDossieProduto` | `POST /simtr-hub/v1/dossie-produto/{id}/workflow` | `200 application/json`, `{"id":123}` | identificador zero: `400` e erro publico completo | `DossieProdutoApiContractTest#preservaContratoDeAvancoDeWorkflow`; `DossieProdutoErroApiContractTest#preservaErroParaIdInvalidoNoWorkflow` |
| `gestaodocumento` / `ObterCredencialContainer` | `POST /simtr-hub/v1/storage/container/credencial` | `200 application/json`; SAS, validade, URL e container comparados exatamente | respostas de erro declaradas no OpenAPI; comportamento runtime negativo sera ampliado nas Tasks 0.3 e 5.1 | `GestaoDocumentoApiContractTest#preservaRespostaJsonCompletaDaCredencialDeContainer` |

O erro de validacao caracterizado possui exatamente `codigo_http=400`, `recurso=simtr-hub`,
`codigo_erro=ARVDOCP0001`, `id_erro` UUID valido e o conjunto integral de mensagens esperado.

## OpenAPI protegido

`OpenApiContractTest` consulta `/simtr-hub/openapi?format=json` e protege:

- OpenAPI `3.1.0`, versao `1.0.0-SNAPSHOT` e ausencia de seguranca global;
- titulo do perfil de teste `simtr-hub-test API`;
- conjunto exato das oito operacoes publicas sob `/simtr-hub/v1`;
- tags, parametros, obrigatoriedade, tipos, formatos, limites, request bodies, media types,
  codigos de resposta e schemas referenciados dessas operacoes;
- 42 schemas e o documento completo por fingerprint canonico
  `85c9bab0170fec09ec0d76b483b468410b5d819ebf54827ddc9f2a5fde874e88`.

O documento tambem contem paths gerados para REST Clients externos. Eles permanecem cobertos pelo
fingerprint completo, mas nao sao contados como endpoints publicos do Hub. Em execucao dev foi
observado o titulo `simtr-hub API`; a diferenca de titulo e propria do profile e nao e uma mudanca
de contrato provocada pela refatoracao.

Testes: `OpenApiContractTest#preservaSemanticaDasOitoOperacoesPublicas` e
`#preservaDocumentoOpenApiCompletoSemDependerDaOrdemDasPropriedades`.

## Lacunas destinadas as proximas tasks

A Task 0.1 congela a borda HTTP executada com o simulador atual. Nao substitui as protecoes ja
planejadas:

- observabilidade e nomes de spans/logs: Task 0.2;
- wire real do MTR, headers, providers, simulador desabilitado e erros upstream: Task 0.3;
- regras de dependencia e bootstrap CDI progressivas: Task 0.4;
- configuracao/profiles e matriz de timeout, retry e circuit breaker: caracterizacao vertical de
  cada capacidade nas Fases 1 a 5;
- idempotencia das operacoes mutaveis: decisao funcional bloqueante antes de workflows futuros.

Nenhuma dessas lacunas autoriza migrar producao antes de C0 receber `GO` humano.
