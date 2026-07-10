# Planejamento - Modulo Dossie Produto - Workflow v1

Data: 2026-07-10

Status: revisado, aprovado pelo usuario e implementado em 2026-07-10.

## Objetivo

Implementar no SIMTR Hub o proxy do endpoint de workflow do Dossie Produto:

```http
POST /arvore-documento/v1/dossie-produto/{id}/workflow
```

Esse endpoint deve chamar o MTR:

```http
POST /simtr-dossie-produto/v1/dossie-produto/{id}/workflow
```

No roteamento atual via API Manager, a chamada real deve continuar usando a base configurada em `application.properties`:

```properties
quarkus.rest-client.dossie-produto.url=https://api.des.caixa:8443/simtr
```

Portanto, o REST Client deve montar o path relativo:

```http
/dossie-produto/v1/dossie-produto/{id}/workflow
```

Nao deve duplicar `/simtr` e nao deve usar diretamente o prefixo bruto `/simtr-dossie-produto` no client.

## Fontes consultadas

- `doc/espaco-colaborativo-de-desenvolvimento.md`
- `doc/api-integracao-mtr-pre-validacao-v1.md`
- `doc/swagger-mtr/simtr-dossie-produto-openapi- 2.20.0.8`
- `doc/planejamento-dossie-produto-documento-v2.md`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/api/dossieproduto/DossieProdutoResource.java`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/application/dossieproduto/DossieProdutoService.java`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/dossieproduto/DossieProdutoClient.java`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/dossieproduto/DossieProdutoGateway.java`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/dossieproduto/mock/DossieProdutoMockFactory.java`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/mapper/dossieproduto/DossieProdutoMapper.java`
- Testes existentes de resource, service, gateway, mock factory e mapper.

## Contexto funcional

No fluxo de pre-validacao, o workflow e acionado depois que:

1. o Dossie Produto foi criado;
2. o formulario foi preenchido;
3. os documentos validados foram vinculados ao dossie;
4. o resultado de validacao negocial dos checklists foi registrado.

Depois da chamada de workflow, o Modulo Dossie Produto valida autorizacao, dados obrigatorios e regras de avanco, atualiza a situacao do dossie e publica mensagem na fila `DOSSIE_PRODUTO_SINALIZACAO_WORKFLOW` para processamento assincrono e sinalizacao ao SINDA.

O endpoint deve iniciar ou avancar o fluxo de um dossie que esteja em situacao `Rascunho`, `Em Alimentacao` ou `Em Complementacao`.

## Contrato externo

### Request

Path param:

- `id`: `integer int64`, obrigatorio, identificador do Dossie Produto cujo fluxo sera iniciado ou avancado.

Body:

- Sem corpo de requisicao.

### Response

HTTP `200 OK`.

Segundo o OpenAPI, o corpo de sucesso tem o mesmo formato simples usado para representar o Dossie Produto criado:

```json
{
  "id": 0
}
```

O campo `id` e obrigatorio no schema de resposta.

### Erros

Propagar o padrao ja existente para erros vindos do MTR por meio de `DossieProdutoClientExceptionMapper` e `MtrRestClientExceptionMapper`.

Status relevantes no OpenAPI:

- `400`: falha ao processar a requisicao por parametros incorretos.
- `401`: nao autorizado.
- `403`: canal ou usuario sem permissao.
- `404`: parametros nao localizados para o avanco de fluxo.
- `409`: conflito ao processar a requisicao.
- `500`: falha nao mapeada.

## Endpoint exposto pelo SIMTR Hub

Manter o padrao externo atual do Hub:

```http
POST /arvore-documento/v1/dossie-produto/{id}/workflow
```

Retornar `200 OK`, nao `201 Created`, porque o contrato do MTR para workflow indica avanco de fluxo, nao criacao de recurso.

## Decisoes tecnicas propostas

1. Manter o endpoint no resource existente:

```text
src/main/java/br/gov/caixa/simtr/arvoredocumento/api/dossieproduto/DossieProdutoResource.java
```

2. Nao criar DTO de request, porque o endpoint nao recebe body.

3. Reaproveitar `DossieProdutoCriadoDto` e `DossieProdutoCriadoVo` para a resposta `{ "id": ... }`, porque o schema de sucesso do OpenAPI tem a mesma forma e o mapper ja cobre esse par DTO/VO.

4. Adicionar metodo no `DossieProdutoClient`:

```java
@POST
@Path("/v1/dossie-produto/{id}/workflow")
Uni<DossieProdutoCriadoDto> iniciarOuAvancarWorkflowDossieProduto(@PathParam("id") Long id);
```

O metodo deve manter `@Timeout`, `@Retry`, `@CircuitBreaker` e o `@ClientExceptionMapper` ja existente no client.

5. Adicionar metodo no `DossieProdutoGateway` com span/logs de dependencia e path observado:

```text
/simtr/dossie-produto/v1/dossie-produto/{id}/workflow
```

6. Adicionar metodo no `DossieProdutoService` seguindo a decisao simulador vs MTR.

7. Adicionar metodo no `DossieProdutoMockFactory`. Como a resposta e apenas o id, o mock pode retornar o `id` recebido no path quando informado, preservando comportamento semelhante ao formulario.

8. Criar mock Markdown em:

```text
src/main/resources/mock/dossieproduto/workflow-dossie-produto.md
```

Opcionalmente criar copia documental em:

```text
doc/mock/dossie-produto/workflow-dossie-produto.md
```

9. Nao alterar o roteamento base do `DossieProdutoClient`, que ja esta consolidado como:

```java
@Path("/dossie-produto")
```

10. Nao implementar neste ciclo os endpoints de validacao negocial, captura, cancelamento, garantia, produto ou consulta. Este plano trata somente de workflow.

## Arquivos previstos

### Alterar classes existentes

- `src/main/java/br/gov/caixa/simtr/arvoredocumento/api/dossieproduto/DossieProdutoResource.java`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/application/dossieproduto/DossieProdutoService.java`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/dossieproduto/DossieProdutoClient.java`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/dossieproduto/DossieProdutoGateway.java`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/dossieproduto/mock/DossieProdutoMockFactory.java`
- `src/test/java/br/gov/caixa/simtr/arvoredocumento/api/ResourceEndpointTest.java`
- `src/test/java/br/gov/caixa/simtr/arvoredocumento/api/ResourceBeanCoverageTest.java`, se a cobertura/instanciacao exigir.
- `src/test/java/br/gov/caixa/simtr/arvoredocumento/application/dossieproduto/DossieProdutoServiceTest.java`
- `src/test/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/GatewayTest.java`
- `src/test/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/mock/MockFactoryTest.java`
- `src/test/java/br/gov/caixa/simtr/arvoredocumento/mapper/dossieproduto/DossieProdutoMapperTest.java`, se for util reforcar o reaproveitamento de `DossieProdutoCriadoDto/Vo`.
- `doc/espaco-colaborativo-de-desenvolvimento.md`
- `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`, se houver decisao consolidada relevante apos a implementacao.

### Criar mock

- `src/main/resources/mock/dossieproduto/workflow-dossie-produto.md`
- `doc/mock/dossie-produto/workflow-dossie-produto.md`, se mantida a copia documental dos mocks.

### Nao criar

- DTO de request de workflow.
- VO de request de workflow.
- Novo resource de Dossie Produto.
- Novo REST Client de Dossie Produto.

## Plano de implementacao apos aprovacao

1. Adicionar o mock Markdown de resposta do workflow com corpo:

```json
{
  "id": 123
}
```

2. Atualizar `DossieProdutoMockFactory` com metodo `iniciarOuAvancarWorkflowDossieProdutoMock(Long id)`, lendo o mock e retornando `new DossieProdutoCriadoDto(id)` quando `id` vier informado.

3. Atualizar `DossieProdutoClient` com `POST /v1/dossie-produto/{id}/workflow`, sem request body e com retorno `Uni<DossieProdutoCriadoDto>`.

4. Atualizar `DossieProdutoGateway` para chamar o client, registrar inicio/sucesso/falha e propagar excecoes sem mascarar.

5. Atualizar `DossieProdutoService` para escolher mock ou gateway conforme `arvore-documento.simulador.dossie-produto.habilitado`, mapear DTO -> VO na resposta e registrar observabilidade.

6. Atualizar `DossieProdutoResource` para expor `POST /{id}/workflow`, validar `id > 0`, nao exigir corpo, converter resposta VO -> DTO e retornar `200 OK`.

7. Atualizar testes de resource, service, gateway e mock factory.

8. Avaliar se `DossieProdutoMapperTest` precisa de novo caso dedicado ao retorno reutilizado; se o teste `devePreservarDossieProdutoCriado` ja cobrir a resposta, nao duplicar sem necessidade.

9. Rodar:

```powershell
mvn -q test
```

10. Atualizar `doc/espaco-colaborativo-de-desenvolvimento.md` com arquivos alterados, testes executados, resultado e pendencias.

11. Atualizar `doc/documentacao-simtr-hub-arquitetura-observabilidade.md` se a implementacao consolidar informacao de contrato ou observabilidade que deva entrar na documentacao principal.

## Observabilidade prevista

Resource:

- Span: `arvore-documento.api.dossie-produto.workflow.avancar`.
- `http.route`: `/arvore-documento/v1/dossie-produto/{id}/workflow`.
- Atributos: `dossie_produto.id`, `dossie_produto.workflow.id_resposta`.
- Logs de recebimento, sucesso e falha com `camada=api`.

Service:

- Span: `arvore-documento.service.dossie-produto.workflow.avancar`.
- Registrar `simulador_habilitado`, origem `mock` ou `mtr`, id do dossie e id retornado.

Gateway:

- Span: `mtr.dossie-produto.workflow.avancar`.
- `mtr.servico=simtr-dossie-produto`, `mtr.api=dossie-produto-v1`, `http.request.method=POST`.
- Path observado: `/simtr/dossie-produto/v1/dossie-produto/{id}/workflow`.

## Testes previstos

1. `ResourceEndpointTest`: `POST /arvore-documento/v1/dossie-produto/{id}/workflow` retorna `200` e body com `id`.
2. `ResourceEndpointTest`: `id=0` retorna `400` com erro padronizado `ARVDOCP0001`.
3. `DossieProdutoServiceTest`: com simulador habilitado usa `DossieProdutoMockFactory`.
4. `DossieProdutoServiceTest`: com simulador desabilitado usa `DossieProdutoGateway`.
5. `GatewayTest`: gateway encaminha `id` para client e retorna response.
6. `GatewayTest`: gateway propaga falha do client.
7. `MockFactoryTest`: mock de workflow e lido corretamente e retorna o `id` do path quando informado.
8. `DossieProdutoMapperTest`: manter ou reforcar round-trip de `DossieProdutoCriadoDto -> DossieProdutoCriadoVo -> DossieProdutoCriadoDto`.
9. Confirmar que endpoints existentes de criacao, formulario e documento continuam passando.

## Riscos e pontos de atencao

- O endpoint nao tem body. Incluir um DTO de request ou exigir `Content-Type` desnecessario pode restringir consumidores sem necessidade.
- O status de sucesso deve ser `200 OK`; copiar o comportamento de criacao/formulario/documento e retornar `201` seria divergente do contrato.
- A resposta `{ "id": ... }` parece reutilizar o schema de criacao no OpenAPI. Reaproveitar `DossieProdutoCriadoDto/Vo` reduz duplicacao, mas a nomenclatura pode ficar menos especifica em logs/testes. Mitigar com nomes de metodos e operacoes claros para workflow.
- A descricao `403` no OpenAPI menciona "criar Documentos em Dossie de Produto", provavelmente texto reaproveitado. Nao modelar regra local especifica a partir dessa descricao.
- Como o workflow publica mensagem assincrona no MTR, o Hub deve atuar apenas como proxy e nao tentar reproduzir a sinalizacao SINDA ou fila localmente.

## Pendencias antes da implementacao

- Planejamento revisado e aprovado pelo usuario em 2026-07-10.
- Usuario confirmou que a copia documental do mock em `doc/mock/dossie-produto/workflow-dossie-produto.md` deve ser criada junto com o mock runtime.
- Usuario confirmou que o retorno do Hub deve seguir o OpenAPI: `200 OK` com corpo `{ "id": ... }`.

## Criterios de aceite

- Endpoint do Hub disponivel em `POST /arvore-documento/v1/dossie-produto/{id}/workflow`.
- Chamada MTR montada como `/simtr/dossie-produto/v1/dossie-produto/{id}/workflow` via configuracao atual.
- Endpoint nao exige request body.
- Response retorna `200 OK` com `{ "id": ... }`.
- Simulador retorna mock quando habilitado.
- Gateway e usado quando simulador estiver desabilitado.
- Erros seguem o padrao atual.
- `mvn -q test` passa.
- Espaco colaborativo e documentacao consolidada sao atualizados ao final da implementacao aprovada.

## Ponto de aprovacao

Implementacao aprovada pelo usuario e concluida em 2026-07-10. Validacao executada com `mvn -q test`.
