# Planejamento - Modulo Dossie Produto - Inclusao de Documento v2

Data: 2026-07-10

Status: revisado, aprovado pelo usuario e implementado em 2026-07-10.

## Objetivo

Implementar no SIMTR Hub o proxy do endpoint de inclusao de documento no Dossie Produto:

```http
POST /hub/v1/dossie-produto/{id}/documento
```

Esse endpoint deve chamar o MTR:

```http
POST /simtr-dossie-produto/v2/dossie-produto/{id}/documento
```

No roteamento atual via API Manager, a chamada real deve continuar usando a base configurada em `application.properties`:

```properties
quarkus.rest-client.dossie-produto.url=https://api.des.caixa:8443/simtr
```

Portanto, o REST Client deve montar o path relativo:

```http
/dossie-produto/v2/dossie-produto/{id}/documento
```

Nao deve duplicar `/simtr` e nao deve usar diretamente o prefixo bruto `/simtr-dossie-produto` no client.

## Fontes consultadas

- `doc/espaco-colaborativo-de-desenvolvimento.md`
- `doc/api-integracao-mtr-pre-validacao-v1.md`
- `doc/swagger-mtr/simtr-dossie-produto-openapi- 2.20.0.8`
- `src/main/java/br/gov/caixa/simtr/hub/api/dossieproduto/DossieProdutoResource.java`
- `src/main/java/br/gov/caixa/simtr/hub/application/dossieproduto/DossieProdutoService.java`
- `src/main/java/br/gov/caixa/simtr/hub/infrastructure/client/dossieproduto/DossieProdutoClient.java`
- `src/main/java/br/gov/caixa/simtr/hub/infrastructure/client/dossieproduto/DossieProdutoGateway.java`
- `src/main/java/br/gov/caixa/simtr/hub/infrastructure/client/dossieproduto/mock/DossieProdutoMockFactory.java`
- `src/main/java/br/gov/caixa/simtr/hub/mapper/dossieproduto/DossieProdutoMapper.java`
- Testes existentes de resource, service, gateway, mock factory e mapper.

## Contexto funcional

No fluxo de pre-validacao, apos a criacao do dossie e a atualizacao do formulario, a Pre-validacao vincula cada documento validado ao Dossie Produto. A resposta v2 precisa devolver dois identificadores:

- `id_documento`
- `id_instancia_documento`

O `id_instancia_documento` sera usado depois pelo endpoint de validacao negocial.

Este endpoint nao deve gerar credencial SAS nem fazer upload no Storage. Essa parte pertence ao Modulo Gestao Documento e ocorre antes. O endpoint recebe o `path_storage` e demais metadados do documento ja enviado.

## Contrato externo

### Request

Path param:

- `id`: `integer int64`, obrigatorio, identificador do Dossie Produto.

Body conforme OpenAPI `v1.dossieprodiuto.criacao.documento.DocumentoInclusaoDossieDTO`. O nome do schema tem typo em `dossieprodiuto`, mas as classes do projeto devem usar `dossieproduto`.

Campos:

```json
{
  "id": 0,
  "path_storage": "{{nome_container}}/{{nome_arquivo}}.{{extensao}}",
  "codigo_ged": "string",
  "object_store_ged": "string",
  "tipo_documento": "string",
  "vinculo_dossie": {
    "cliente": {
      "cpf": "string",
      "cnpj": "string",
      "tipo_vinculo": 0
    },
    "elemento_conteudo": 0,
    "garantia": {
      "codigo_bacen": 0,
      "produto_operacao": 0,
      "produto_modalidade": 0,
      "cliente_avalista": [
        {
          "cpf": "string",
          "cnpj": "string"
        }
      ]
    }
  },
  "atributos": [
    {
      "chave": "string",
      "valor": "string",
      "objeto": "string",
      "opcoes_selecionadas": [
        "string"
      ]
    }
  ],
  "propriedades": [
    {
      "chave": "string",
      "valor": "string",
      "objeto": "string"
    }
  ]
}
```

Observacoes:

- O OpenAPI nao marca os campos top-level do request como `required`.
- `atributos[].objeto` e `propriedades[].objeto` aparecem como obrigatorios no schema bruto, mas o contrato real aceito pelo fluxo de documento permite omitir esses campos. O SIMTR Hub nao deve bloquea-los localmente.
- `atributos[].chave`, `atributos[].valor`, `propriedades[].chave` e `propriedades[].valor` seguem validados localmente.
- Validacoes locais devem seguir o contrato sem restringir indevidamente o que o MTR aceita.

### Response

HTTP `201 Created`.

```json
{
  "id_documento": 0,
  "id_instancia_documento": 0
}
```

Ambos os campos sao obrigatorios no schema v2 `v2.dossieproduto.criacao.documento.RespostaCriacaoDocumentoDTO`.

### Erros

Propagar o padrao ja existente para erros vindos do MTR por meio de `DossieProdutoClientExceptionMapper` e `MtrRestClientExceptionMapper`.

Status relevantes no OpenAPI:

- `400`: parametros incorretos.
- `401`: nao autorizado.
- `403`: canal ou usuario sem permissao para criar documentos em dossie.
- `404`: parametro nao localizado para criacao de documento.
- `409`: conflito ao processar a requisicao.
- `500`: falha nao mapeada.

## Decisoes tecnicas propostas

1. Manter o endpoint externo do Hub em `DossieProdutoResource`, com base atual:

```java
@Path("/hub/v1/dossie-produto")
```

2. Adicionar o metodo:

```http
POST /{id}/documento
```

3. Criar DTOs especificos para documento, sem reaproveitar os DTOs de formulario, porque o contrato de `vinculo_dossie` e diferente.

4. Criar VOs equivalentes aos DTOs para preservar a fronteira arquitetural `DTO -> VO -> DTO`.

5. Adicionar metodos no `DossieProdutoMapper` para request e response de documento.

6. Refatorar o `DossieProdutoClient` para suportar chamadas v1 e v2 no mesmo REST Client. Proposta:

```java
@Path("/dossie-produto")
```

E mover as versoes para os metodos:

```java
@POST
@Path("/v1/dossie-produto")

@PATCH
@Path("/v1/dossie-produto/{id}/formulario")

@POST
@Path("/v2/dossie-produto/{id}/documento")
```

Essa mudanca evita criar outro client para o mesmo servico e permite chamar v2 sem duplicar `/simtr`.

7. Adicionar metodo no `DossieProdutoGateway` com span/logs de dependencia e path:

```text
/simtr/dossie-produto/v2/dossie-produto/{id}/documento
```

8. Adicionar metodo no `DossieProdutoService` seguindo a decisao simulador vs MTR.

9. Adicionar mock Markdown em:

```text
src/main/resources/mock/dossieproduto/documento-dossie-produto.md
```

10. Adicionar metodo no `DossieProdutoMockFactory` lendo esse mock. O mock pode retornar ids fixos, sem depender do path param.

## Arquivos previstos

### Criar DTOs

Em `src/main/java/br/gov/caixa/simtr/hub/api/dto/dossieproduto`:

- `DossieProdutoDocumentoInclusaoDto.java`
- `DossieProdutoDocumentoCriadoDto.java`
- `DossieProdutoDocumentoVinculoDossieDto.java`
- `DossieProdutoDocumentoClienteDto.java`
- `DossieProdutoDocumentoGarantiaDto.java`
- `DossieProdutoDocumentoAtributoDto.java`
- `DossieProdutoDocumentoPropriedadeDto.java`

### Criar VOs

Em `src/main/java/br/gov/caixa/simtr/hub/domain/dossieproduto`:

- `DossieProdutoDocumentoInclusaoVo.java`
- `DossieProdutoDocumentoCriadoVo.java`
- `DossieProdutoDocumentoVinculoDossieVo.java`
- `DossieProdutoDocumentoClienteVo.java`
- `DossieProdutoDocumentoGarantiaVo.java`
- `DossieProdutoDocumentoAtributoVo.java`
- `DossieProdutoDocumentoPropriedadeVo.java`

### Alterar classes existentes

- `DossieProdutoResource.java`
- `DossieProdutoService.java`
- `DossieProdutoClient.java`
- `DossieProdutoGateway.java`
- `DossieProdutoMockFactory.java`
- `DossieProdutoMapper.java`
- `TestFixtures.java`
- `ResourceEndpointTest.java`
- `DossieProdutoServiceTest.java`
- `GatewayTest.java`
- `MockFactoryTest.java`
- `DossieProdutoMapperTest.java`
- `ResourceBeanCoverageTest.java`, se a cobertura/instanciacao exigir.

### Criar mock

- `src/main/resources/mock/dossieproduto/documento-dossie-produto.md`

## Plano de implementacao apos aprovacao

1. Criar DTOs e VOs de documento com `@JsonInclude(JsonInclude.Include.NON_NULL)`, `@JsonProperty` para campos snake_case e `@Valid` em objetos/listas aninhadas.
2. Atualizar `DossieProdutoMapper` com metodos:

```java
DossieProdutoDocumentoInclusaoVo toVo(DossieProdutoDocumentoInclusaoDto dto);
DossieProdutoDocumentoInclusaoDto toDto(DossieProdutoDocumentoInclusaoVo vo);
DossieProdutoDocumentoCriadoVo toVo(DossieProdutoDocumentoCriadoDto dto);
DossieProdutoDocumentoCriadoDto toDto(DossieProdutoDocumentoCriadoVo vo);
```

3. Ajustar `DossieProdutoClient` para base versionavel e adicionar metodo v2 com `@Timeout`, `@Retry`, `@CircuitBreaker` e o mesmo `@ClientExceptionMapper`.
4. Atualizar `DossieProdutoGateway` para encaminhar `id` e request ao client, registrar sucesso/falha e propagar excecoes sem mascarar.
5. Atualizar `DossieProdutoMockFactory` e criar mock Markdown de resposta.
6. Atualizar `DossieProdutoService` para converter VO -> DTO, escolher mock ou gateway, e converter DTO -> VO.
7. Atualizar `DossieProdutoResource` para expor `POST /{id}/documento`, validar `id > 0`, validar body nao nulo, mapear request/response e retornar `201`.
8. Atualizar fixtures e testes.
9. Rodar `mvn -q test`.
10. Atualizar `doc/espaco-colaborativo-de-desenvolvimento.md` e, se a refatoracao do client for consolidada, atualizar `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`.

## Observabilidade prevista

Resource:

- Span: `simtr-hub.api.dossie-produto.documento.incluir`.
- `http.route`: `/hub/v1/dossie-produto/{id}/documento`.
- Atributos: `dossie_produto.id`, `dossie_produto.documento.id`, `dossie_produto.documento.instancia.id`, quantidades de atributos/propriedades quando disponiveis.
- Logs de recebimento, sucesso e falha com `camada=api`.

Service:

- Span: `simtr-hub.service.dossie-produto.documento.incluir`.
- Registrar `simulador_habilitado`, origem `mock` ou `mtr`, id do dossie e ids retornados.

Gateway:

- Span: `mtr.dossie-produto.documento.incluir`.
- `mtr.servico=simtr-dossie-produto`, `mtr.api=dossie-produto-v2`, `http.request.method=POST`.
- Path observado: `/simtr/dossie-produto/v2/dossie-produto/{id}/documento`.

## Testes previstos

1. `ResourceEndpointTest`: `POST /hub/v1/dossie-produto/{id}/documento` retorna `201` e body com `id_documento` e `id_instancia_documento`.
2. `ResourceEndpointTest`: `id=0` retorna `400` com erro padronizado.
3. `DossieProdutoServiceTest`: com simulador habilitado usa `DossieProdutoMockFactory`.
4. `DossieProdutoServiceTest`: com simulador desabilitado usa `DossieProdutoGateway`.
5. `GatewayTest`: gateway encaminha `id` e request para client e retorna response.
6. `GatewayTest`: gateway propaga falha do client.
7. `MockFactoryTest`: mock de documento e lido corretamente.
8. `DossieProdutoMapperTest`: round-trip DTO -> VO -> DTO do request de documento preserva campos aninhados.
9. `DossieProdutoMapperTest`: round-trip da response preserva `id_documento` e `id_instancia_documento`.
10. `DossieProdutoMapperTest`: listas nulas, itens nulos e objetos aninhados nulos continuam aceitos quando o contrato permitir.
11. Confirmar que endpoints existentes de criacao e formulario continuam passando apos a mudanca de base do REST Client.

## Riscos e pontos de atencao

- A mudanca de `@Path` no `DossieProdutoClient` pode afetar os endpoints v1 existentes se algum metodo nao receber o path completo corretamente.
- O OpenAPI usa o nome de schema `dossieprodiuto`; isso deve ser tratado apenas como typo de documentacao.
- O campo `cliente_avalista` do contrato de documento difere de `clientes_avalistas` usado em outros DTOs. Usar DTO proprio.
- O endpoint v2 reutiliza request v1, mas tem response diferente. Nao reaproveitar `DossieProdutoCriadoDto`, porque ele representa somente `id`.
- A integracao com Gestao Documento nao deve ser embutida neste endpoint sem nova decisao, porque este endpoint so vincula ao dossie o documento ja armazenado.

## Criterios de aceite

- Endpoint do Hub disponivel em `POST /hub/v1/dossie-produto/{id}/documento`.
- Chamada MTR montada como `/simtr/dossie-produto/v2/dossie-produto/{id}/documento` via configuracao atual.
- Request serializa/deserializa campos snake_case do contrato.
- Response retorna `201` com `id_documento` e `id_instancia_documento`.
- Simulador retorna mock quando habilitado.
- Gateway e usado quando simulador estiver desabilitado.
- Erros seguem o padrao atual.
- `mvn -q test` passa.
- Espaco colaborativo e documentacao consolidada sao atualizados ao final da implementacao aprovada.

## Ponto de aprovacao

Implementacao aprovada pelo usuario e concluida em 2026-07-10. Validacao executada com `mvn -q test`.
