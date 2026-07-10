# Cenários de testes unitários dos endpoints implementados

Este documento define os cenários antes da implementação da cobertura automatizada.

## Escopo

Endpoints cobertos:

```http
GET /arvore-documento/v1/processo/identificador-negocial/{identificador}
GET /arvore-documento/v1/checklist/identificador-negocial/{identificador}/versao/{versao}
POST /arvore-documento/v1/dossie-produto
PATCH /arvore-documento/v1/dossie-produto/{id}/formulario
```

## Estratégia

Os testes unitários devem validar as camadas isoladamente, sem rede externa:

- `Resource`: valida status HTTP, entrada mínima, propagação para o service e corpo de resposta.
- `Service`: valida decisão entre simulador e gateway real conforme propriedade.
- `Mapper`: valida ida e volta `DTO -> VO -> DTO` nos contratos de dossiê produto.
- `MockFactory`: valida leitura dos mocks Markdown e fallback esperado.

Testes que sobem HTTP real com Quarkus devem ficar em uma etapa separada de regressão/smoke, não como teste unitário puro.

## Pré-condições de teste

Para os testes unitários, usar `src/test/resources/application.properties`, sem depender do profile `%dev` nem de variáveis de ambiente locais:

```properties
simtr.apikey=test-apikey
arvore-documento.simulador.parametrizacao-processo.habilitado=true
arvore-documento.simulador.parametrizacao-checklist.habilitado=true
arvore-documento.simulador.dossie-produto.habilitado=true
quarkus.rest-client.parametrizacao-processo.url=http://localhost:1
quarkus.rest-client.parametrizacao-checklist.url=http://localhost:1
quarkus.rest-client.dossie-produto.url=http://localhost:1
quarkus.oidc-client.enabled=false
```

Para cenários de caminho MTR real, usar mock do gateway/client, não chamada externa.

O OIDC client não deve ser exercitado em teste unitário. Quando um teste precisar validar o caminho de integração real, substituir `DossieProdutoGateway`, `ParametrizacaoProcessoGateway`, `ParametrizacaoChecklistGateway` ou os REST clients por mocks. Testes específicos do REST client com token fake devem ficar em uma camada posterior de integração controlada, com servidor local de mock para token e API.

## GET Processo por identificador negocial

Endpoint:

```http
GET /arvore-documento/v1/processo/identificador-negocial/{identificador}
```

### Cenários

| ID | Camada | Cenário | Entrada | Esperado |
|---|---|---|---|---|
| PROC-001 | Resource | Identificador válido retorna `200` | `identificador=1000016487` | `ProcessoService.consultar...` chamado e DTO retornado |
| PROC-002 | Resource | Identificador inválido falha validação | `identificador=0` | erro `400` com `ErroPadraoDto` |
| PROC-003 | Service | Simulador habilitado usa mock | simulador `true` | não chama gateway, chama `ProcessoMockFactory` |
| PROC-004 | Service | Simulador desabilitado usa gateway | simulador `false` | chama `ParametrizacaoProcessoGateway` |
| PROC-005 | Service | Erro do gateway é propagado | gateway lança `MtrRestClientException` | exceção não é engolida |
| PROC-006 | Mapper | Mapper preserva campos principais do processo | DTO mockado | VO/DTO mantém `identificador_negocial`, `nome`, fases, produtos e relacionamentos |

## GET Checklist por identificador negocial e versão

Endpoint:

```http
GET /arvore-documento/v1/checklist/identificador-negocial/{identificador}/versao/{versao}
```

### Cenários

| ID | Camada | Cenário | Entrada | Esperado |
|---|---|---|---|---|
| CHECK-001 | Resource | Identificador e versão válidos retornam `200` | `identificador=1000012583`, `versao=1` | `ChecklistService` chamado e DTO retornado |
| CHECK-002 | Resource | Identificador inválido falha validação | `identificador=0`, `versao=1` | erro `400` com `ErroPadraoDto` |
| CHECK-003 | Resource | Versão inválida falha validação | `identificador=1000012583`, `versao=0` | erro `400` com `ErroPadraoDto` |
| CHECK-004 | Service | Simulador habilitado usa mock | simulador `true` | não chama gateway, chama `ChecklistMockFactory` |
| CHECK-005 | Service | Simulador desabilitado usa gateway | simulador `false` | chama `ParametrizacaoChecklistGateway` |
| CHECK-006 | Mapper | Mapper preserva apontamentos | DTO mockado | VO/DTO mantém dados do checklist e apontamentos |

## POST Dossiê Produto

Endpoint:

```http
POST /arvore-documento/v1/dossie-produto
```

### Cenários

| ID | Camada | Cenário | Entrada | Esperado |
|---|---|---|---|---|
| DOSSIE-POST-001 | Resource | Corpo válido retorna `201` | payload de criação básica | resposta `DossieProdutoCriadoDto` com `id` |
| DOSSIE-POST-002 | Resource | Corpo nulo falha validação | `null` | erro `400` com mensagem de corpo obrigatório |
| DOSSIE-POST-003 | Resource | `processo` ausente falha validação | payload sem `processo` | erro `400` |
| DOSSIE-POST-004 | Resource | `chave_correlacao_canal` ausente falha validação | payload sem chave | erro `400` |
| DOSSIE-POST-005 | Service | Simulador habilitado usa mock | simulador `true` | não chama gateway, retorna mock |
| DOSSIE-POST-006 | Service | Simulador desabilitado usa gateway | simulador `false` | chama `DossieProdutoGateway.criarDossieProduto` |
| DOSSIE-POST-007 | Mapper | Mapper preserva cliente relacionado | payload com `cliente_relacionado` | DTO/VO mantém CPF, CNPJ, tipo de vínculo e titularidade |
| DOSSIE-POST-008 | MockFactory | Mock de criação é lido | `criacao-basica-dossie-produto.md` | retorna `id=1` |

## PATCH Formulário do Dossiê Produto

Endpoint:

```http
PATCH /arvore-documento/v1/dossie-produto/{id}/formulario
```

### Cenários

| ID | Camada | Cenário | Entrada | Esperado |
|---|---|---|---|---|
| DOSSIE-PATCH-001 | Resource | Corpo válido retorna `201` | `id=1`, payload de formulário | resposta `DossieProdutoCriadoDto` com `id` |
| DOSSIE-PATCH-002 | Resource | `id` inválido falha validação | `id=0` | erro `400` |
| DOSSIE-PATCH-003 | Resource | Corpo nulo falha validação | `id=1`, corpo `null` | erro `400` |
| DOSSIE-PATCH-004 | Service | Simulador habilitado usa mock | simulador `true` | não chama gateway, chama `DossieProdutoMockFactory.atualizarFormulario...` |
| DOSSIE-PATCH-005 | Service | Simulador desabilitado usa gateway | simulador `false` | chama `DossieProdutoGateway.atualizarFormulario...` |
| DOSSIE-PATCH-006 | Service | Mock retorna o `id` do path quando informado | `id=123` | resposta com `id=123` |
| DOSSIE-PATCH-007 | Mapper | Mapper preserva vínculo completo | payload com cliente, produto, garantia e respostas | DTO/VO mantém todos os campos snake_case/camelCase |
| DOSSIE-PATCH-008 | Mapper | Mapper preserva exclusão de resposta | `excluir=true` | valor mantido no DTO final |
| DOSSIE-PATCH-009 | MockFactory | Mock de formulário é lido | `formulario-dossie-produto.md` | retorna DTO com `id` |

## Cenários transversais

| ID | Camada | Cenário | Esperado |
|---|---|---|---|
| TRANS-001 | Exception mapper | Erro de negócio MTR é convertido | status original e `ErroPadraoDto` preservados |
| TRANS-002 | Exception mapper | Erro inesperado é padronizado | `500`, `ARVDOCP9999`, `id_erro` gerado |
| TRANS-003 | Observabilidade | Erro registrado contém correlação | log com `traceId`, `spanId`, `evento`, `erro_tipo` quando houver span ativo |
| TRANS-004 | REST client | Paths relativos permanecem via API Manager | dossiê produto usa `/dossie-produto/v1/dossie-produto` sobre base `/simtr` |
| TRANS-005 | Configuração de teste | Testes não dependem de OIDC/API Manager | `src/test/resources/application.properties` usa simuladores, URLs sentinela e OIDC client desabilitado |

## Critérios de aceite da implementação dos testes

- Rodar com `mvn test`.
- Não depender de rede, OIDC real nem API Manager.
- Não depender do profile `%dev`.
- Usar mocks para gateway/client quando validar caminho de MTR real.
- Cobrir pelo menos um caminho feliz e um caminho de validação por endpoint.
- Cobrir a decisão simulador versus gateway para cada service.
- Cobrir mapper dos contratos de dossiê produto, incluindo o PATCH de formulário.
