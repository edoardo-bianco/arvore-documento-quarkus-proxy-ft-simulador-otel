# Planejamento - Modulo Dossie Produto - Validacao Negocial v1

Data: 2026-07-10

Status: revisado, aprovado pelo usuario e implementado em 2026-07-10. Implementacao feita usando o OpenAPI como contrato tecnico verdadeiro do MTR.

## Objetivo

Implementar no SIMTR Hub o proxy do endpoint de validacao negocial do Dossie Produto:

```http
PATCH /arvore-documento/v1/dossie-produto/{id}/validacao-negocial
```

Esse endpoint deve chamar o MTR:

```http
PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/validacao-negocial
```

No roteamento atual via API Manager, a chamada real deve continuar usando a base configurada em `application.properties`:

```properties
quarkus.rest-client.dossie-produto.url=https://api.des.caixa:8443/simtr
```

Portanto, o REST Client deve montar o path relativo:

```http
/dossie-produto/v1/dossie-produto/{id}/validacao-negocial
```

Nao deve duplicar `/simtr` e nao deve usar diretamente o prefixo bruto `/simtr-dossie-produto` no client.

## Fontes consultadas

- `doc/espaco-colaborativo-de-desenvolvimento.md`
- `doc/api-integracao-mtr-pre-validacao-v1.md`
- `doc/swagger-mtr/simtr-dossie-produto-openapi- 2.20.0.8`
- `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`
- `pom.xml`
- `src/main/resources/application.properties`
- `src/test/resources/application.properties`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/api/dossieproduto/DossieProdutoResource.java`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/application/dossieproduto/DossieProdutoService.java`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/dossieproduto/DossieProdutoClient.java`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/dossieproduto/DossieProdutoGateway.java`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/dossieproduto/mock/DossieProdutoMockFactory.java`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/mapper/dossieproduto/DossieProdutoMapper.java`
- Testes existentes de resource, service, gateway, mock factory, mapper e fixtures.

## Contexto funcional

No fluxo de pre-validacao, a validacao negocial e registrada depois que:

1. o Dossie Produto foi criado;
2. o formulario foi preenchido;
3. cada documento validado foi vinculado ao dossie por `POST /simtr-dossie-produto/v2/dossie-produto/{id}/documento`;
4. o retorno do endpoint de documento forneceu `id_instancia_documento`.

Depois da validacao negocial, o fluxo chama o workflow do dossie.

O endpoint recebe o resultado dos checklists analisados para um dossie em situacao `RASCUNHO` ou `EM_ALIMENTACAO`. Cada verificacao pode referenciar:

- instancia documental analisada;
- checklist e versao;
- parecer dos apontamentos;
- garantia;
- produto;
- indicador de checklist previo;
- respostas de formulario complementares.

## Contrato externo

### Request

Path param:

- `id`: `integer int64`, obrigatorio, identificador do Dossie Produto encaminhado para validacao negocial.

Body conforme OpenAPI `v1.dossieproduto.validacaonegocial.SolicitacaoValidacaoNegocialDTO`:

```json
{
  "verificacoes": [
    {
      "identificador_instancia_documento": 0,
      "identificador_checklist": 0,
      "versao_checklist": 0,
      "analise_realizada": true,
      "parecer_apontamentos": [
        {
          "identificador_apontamento": 0,
          "resultado": "APROVADO",
          "comentario": "string",
          "necessidade_reanalise": true,
          "indice_ia": 0.99
        }
      ],
      "garantia": {
        "codigo_bacen": 0,
        "clientes_avalistas": [
          {
            "cpf": "string",
            "cnpj": "string"
          }
        ]
      },
      "produto": {
        "codigo_operacao": 0,
        "codigo_modalidade": 0
      },
      "previo": true
    }
  ],
  "respostas_formulario": [
    {
      "campo_formulario": 29848450,
      "resposta": "string",
      "opcoes_selecionadas": [
        "string"
      ]
    }
  ]
}
```

Campos do OpenAPI:

- `verificacoes`: lista de `VerificacaoDTO`.
- `respostas_formulario`: lista de `RespostaFormularioDTO`.
- Em cada verificacao, o OpenAPI 2.20.0.8 marca como obrigatorios `identificador_checklist`, `versao_checklist`, `analise_realizada`, `parecer_apontamentos` e `previo`; porem a integracao real do MTR aceita payload sem `previo`, entao o Hub nao deve bloquear localmente a ausencia desse campo.
- Em cada parecer, o OpenAPI marca como obrigatorios `identificador_apontamento`, `resultado` e `necessidade_reanalise`.
- `resultado` e string; valores documentados: `SEM_ANALISE`, `APROVADO`, `REPROVADO`, `INCONCLUSIVO`.
- `indice_ia` e opcional e representa confianca entre `0.0` e `1.0`, mas o OpenAPI nao define `minimum`/`maximum`.
- Em `produto`, quando informado, `codigo_operacao` e `codigo_modalidade` sao obrigatorios no schema.
- Em `respostas_formulario`, quando o item for informado, `campo_formulario` e obrigatorio no schema.

### Divergencia funcional x OpenAPI

A documentacao funcional tambem traz um exemplo com nomes diferentes:

```json
{
  "verificacoes_realizadas": [],
  "resposta_formulario": []
}
```

O OpenAPI tecnico define:

```json
{
  "verificacoes": [],
  "respostas_formulario": []
}
```

Decisao aprovada:

1. Modelar e serializar o contrato canonico do OpenAPI: `verificacoes` e `respostas_formulario`.
2. Nao aceitar aliases especificos para `verificacoes_realizadas` ou `resposta_formulario`, porque o MTR foi implementado conforme o OpenAPI.
3. Nao modelar `identificador_negocial` dentro da verificacao, pois ele aparece apenas no exemplo funcional e nao no schema tecnico do endpoint de validacao negocial. Com `quarkus.jackson.fail-on-unknown-properties=false`, o Hub tende a aceitar esse campo na entrada e ignora-lo, mas nao deve encaminha-lo ao MTR nesta implementacao.

### Response

HTTP `200 OK`.

A documentacao funcional informa:

```text
sem Corpo da reposta para Http 200
```

O OpenAPI tambem descreve `200` sem schema de resposta.

Decisao implementada:

- Resource retorna `Response.ok().build()`.
- Client, gateway e service usam `Uni<Void>` para representar sucesso sem corpo.
- Nao criar DTO/VO de resposta.

### Erros

Propagar o padrao ja existente para erros vindos do MTR por meio de `DossieProdutoClientExceptionMapper` e `MtrRestClientExceptionMapper`.

Status relevantes no OpenAPI:

- `400`: falha nas validacoes da requisicao.
- `401`: usuario ou canal nao autenticado.
- `403`: usuario ou canal nao autorizado.
- `404`: recurso nao localizado para os parametros informados.
- `409`: conflito.
- `500`: falha nao mapeada.
- `503`: servico de terceiros indisponivel.

## Endpoint exposto pelo SIMTR Hub

Manter o padrao externo atual do Hub:

```http
PATCH /arvore-documento/v1/dossie-produto/{id}/validacao-negocial
```

Retornar `200 OK` sem corpo.

## Decisoes tecnicas propostas

1. Manter o endpoint no resource existente:

```text
src/main/java/br/gov/caixa/simtr/arvoredocumento/api/dossieproduto/DossieProdutoResource.java
```

2. Criar DTOs especificos para validacao negocial, sem reaproveitar DTOs de formulario/documento, porque o contrato tem estrutura propria e resposta sem corpo.

3. Criar VOs equivalentes aos DTOs para preservar a fronteira arquitetural `DTO -> VO -> DTO`.

4. Adicionar metodos no `DossieProdutoMapper` para request de validacao negocial:

```java
DossieProdutoValidacaoNegocialVo toVo(DossieProdutoValidacaoNegocialDto dto);
DossieProdutoValidacaoNegocialDto toDto(DossieProdutoValidacaoNegocialVo vo);
```

5. Adicionar metodo no `DossieProdutoClient`:

```java
@PATCH
@Path("/v1/dossie-produto/{id}/validacao-negocial")
Uni<Void> registrarValidacaoNegocialDossieProduto(
        @PathParam("id") Long id,
        DossieProdutoValidacaoNegocialDto requisicao
);
```

O metodo deve manter `@Timeout`, `@Retry`, `@CircuitBreaker` e o `@ClientExceptionMapper` ja existente.

6. Adicionar metodo no `DossieProdutoGateway` com span/logs de dependencia e path observado:

```text
/simtr/dossie-produto/v1/dossie-produto/{id}/validacao-negocial
```

7. Adicionar metodo no `DossieProdutoService` seguindo a decisao simulador vs MTR.

8. Adicionar metodo no `DossieProdutoMockFactory`. Mesmo sem corpo de resposta, o simulador deve ler um mock Markdown para manter o padrao do projeto. O arquivo pode conter `{}` como JSON de sucesso.

9. Nao alterar o roteamento base do `DossieProdutoClient`, que ja esta consolidado como:

```java
@Path("/dossie-produto")
```

10. Nao implementar neste ciclo os endpoints de garantia, produto, captura, cancelamento ou consulta.

## Validacoes locais propostas

Validar localmente:

- `id` do path obrigatorio e maior que zero;
- body obrigatorio.

Validar com `@NotNull` somente campos marcados como obrigatorios no OpenAPI e que tambem fazem sentido no contrato de negocio:

- `identificador_checklist`;
- `versao_checklist`;
- `analise_realizada`;
- `parecer_apontamentos`;
- `identificador_apontamento`;
- `resultado`;
- `necessidade_reanalise`;
- `codigo_operacao` e `codigo_modalidade` quando `produto` for informado;
- `campo_formulario` quando houver item em `respostas_formulario`.

Nao validar localmente nesta primeira implementacao:

- enum de `resultado`, para nao bloquear valor novo aceito pelo MTR sem evidencia;
- range de `indice_ia`, porque o OpenAPI nao define `minimum`/`maximum`;
- obrigatoriedade de `verificacoes` e `respostas_formulario` no top-level, porque o schema top-level nao possui lista `required`;
- obrigatoriedade de `garantia`, `produto` ou `identificador_instancia_documento`, porque esses campos variam conforme o tipo de checklist/documento.
- obrigatoriedade de `previo`, porque payload real usado na integracao de pre-validacao nao envia esse campo e o MTR implementado nao deve ser bloqueado pelo Hub nesse caso.

Se o MTR rejeitar algum desses casos em homologacao, ajustar com evidencia e registrar a decisao.

## Arquivos previstos

### Criar DTOs

Em `src/main/java/br/gov/caixa/simtr/arvoredocumento/api/dto/dossieproduto`:

- `DossieProdutoValidacaoNegocialDto.java`
- `DossieProdutoValidacaoNegocialVerificacaoDto.java`
- `DossieProdutoValidacaoNegocialParecerApontamentoDto.java`
- `DossieProdutoValidacaoNegocialGarantiaDto.java`
- `DossieProdutoValidacaoNegocialClienteAvalistaDto.java`
- `DossieProdutoValidacaoNegocialProdutoDto.java`
- `DossieProdutoValidacaoNegocialRespostaFormularioDto.java`

### Criar VOs

Em `src/main/java/br/gov/caixa/simtr/arvoredocumento/domain/dossieproduto`:

- `DossieProdutoValidacaoNegocialVo.java`
- `DossieProdutoValidacaoNegocialVerificacaoVo.java`
- `DossieProdutoValidacaoNegocialParecerApontamentoVo.java`
- `DossieProdutoValidacaoNegocialGarantiaVo.java`
- `DossieProdutoValidacaoNegocialClienteAvalistaVo.java`
- `DossieProdutoValidacaoNegocialProdutoVo.java`
- `DossieProdutoValidacaoNegocialRespostaFormularioVo.java`

### Alterar classes existentes

- `src/main/java/br/gov/caixa/simtr/arvoredocumento/api/dossieproduto/DossieProdutoResource.java`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/application/dossieproduto/DossieProdutoService.java`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/dossieproduto/DossieProdutoClient.java`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/dossieproduto/DossieProdutoGateway.java`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/dossieproduto/mock/DossieProdutoMockFactory.java`
- `src/main/java/br/gov/caixa/simtr/arvoredocumento/mapper/dossieproduto/DossieProdutoMapper.java`
- `src/test/java/br/gov/caixa/simtr/arvoredocumento/TestFixtures.java`
- `src/test/java/br/gov/caixa/simtr/arvoredocumento/api/ResourceEndpointTest.java`
- `src/test/java/br/gov/caixa/simtr/arvoredocumento/api/ResourceBeanCoverageTest.java`, se a cobertura/instanciacao exigir.
- `src/test/java/br/gov/caixa/simtr/arvoredocumento/application/dossieproduto/DossieProdutoServiceTest.java`
- `src/test/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/GatewayTest.java`
- `src/test/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/mock/MockFactoryTest.java`
- `src/test/java/br/gov/caixa/simtr/arvoredocumento/mapper/dossieproduto/DossieProdutoMapperTest.java`
- `doc/espaco-colaborativo-de-desenvolvimento.md`
- `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`, se a implementacao consolidar informacao de contrato ou observabilidade que deva entrar na documentacao principal.

### Criar mock

- `src/main/resources/mock/dossieproduto/validacao-negocial-dossie-produto.md`
- `doc/mock/dossie-produto/validacao-negocial-dossie-produto.md`, como copia documental do mock runtime.

## Plano de implementacao apos aprovacao

1. Criar DTOs de validacao negocial com `@JsonInclude(JsonInclude.Include.NON_NULL)`, `@JsonProperty` para campos snake_case canonicos do OpenAPI e `@Valid` em objetos/listas aninhadas.
2. Criar VOs equivalentes.
3. Atualizar `DossieProdutoMapper` com metodos DTO -> VO -> DTO para o request.
4. Criar fixture `validacaoNegocialDto()` com pelo menos uma verificacao, tres pareceres, produto, garantia e uma resposta de formulario.
5. Atualizar `DossieProdutoClient` com `PATCH /v1/dossie-produto/{id}/validacao-negocial`, retorno `Uni<Void>` e resiliencia igual aos demais metodos.
6. Atualizar `DossieProdutoGateway` para encaminhar `id` e request ao client, registrar sucesso/falha e propagar excecoes sem mascarar.
7. Atualizar `DossieProdutoMockFactory` para ler `validacao-negocial-dossie-produto.md` e representar sucesso sem corpo.
8. Atualizar `DossieProdutoService` para converter VO -> DTO, escolher mock ou gateway e retornar `Uni<Void>`.
9. Atualizar `DossieProdutoResource` para expor `PATCH /{id}/validacao-negocial`, validar `id > 0`, validar body nao nulo, chamar service e retornar `200 OK` sem entity.
10. Atualizar testes de resource, service, gateway, mock factory e mapper.
11. Rodar:

```powershell
mvn -q test
```

12. Atualizar `doc/espaco-colaborativo-de-desenvolvimento.md` com arquivos alterados, testes executados, resultado e pendencias.
13. Atualizar `doc/documentacao-simtr-hub-arquitetura-observabilidade.md` se houver consolidacao de contrato ou spans/logs novos.

## Observabilidade prevista

Resource:

- Span: `arvore-documento.api.dossie-produto.validacao-negocial.registrar`.
- `http.route`: `/arvore-documento/v1/dossie-produto/{id}/validacao-negocial`.
- Atributos: `dossie_produto.id`, `dossie_produto.validacao.verificacoes.quantidade`, `dossie_produto.validacao.respostas_formulario.quantidade`.
- Logs de recebimento, sucesso e falha com `camada=api`.

Service:

- Span: `arvore-documento.service.dossie-produto.validacao-negocial.registrar`.
- Registrar `simulador_habilitado`, origem `mock` ou `mtr`, id do dossie, quantidade de verificacoes e quantidade de respostas de formulario.

Gateway:

- Span: `mtr.dossie-produto.validacao-negocial.registrar`.
- `mtr.servico=simtr-dossie-produto`, `mtr.api=dossie-produto-v1`, `http.request.method=PATCH`.
- Path observado: `/simtr/dossie-produto/v1/dossie-produto/{id}/validacao-negocial`.

## Testes previstos

1. `ResourceEndpointTest`: `PATCH /arvore-documento/v1/dossie-produto/{id}/validacao-negocial` retorna `200` sem corpo relevante.
2. `ResourceEndpointTest`: `id=0` retorna `400` com erro padronizado `ARVDOCP0001`.
3. `ResourceEndpointTest`: body ausente retorna `400` com erro padronizado.
4. `DossieProdutoServiceTest`: com simulador habilitado usa `DossieProdutoMockFactory`.
5. `DossieProdutoServiceTest`: com simulador desabilitado usa `DossieProdutoGateway`.
6. `GatewayTest`: gateway encaminha `id` e request para client.
7. `GatewayTest`: gateway propaga falha do client.
8. `MockFactoryTest`: mock de validacao negocial e lido corretamente.
9. `DossieProdutoMapperTest`: round-trip DTO -> VO -> DTO preserva verificacoes, pareceres, garantia, produto e respostas de formulario.
10. `DossieProdutoMapperTest`: listas nulas, itens nulos e objetos aninhados nulos continuam aceitos quando o contrato permitir.
11. Confirmar que endpoints existentes de criacao, formulario, documento e workflow continuam passando.

## Riscos e pontos de atencao

- A divergencia `verificacoes_realizadas`/`resposta_formulario` versus `verificacoes`/`respostas_formulario` foi decidida em favor do OpenAPI, pois ele representa o contrato implementado no MTR.
- O exemplo funcional inclui `identificador_negocial` dentro da verificacao, mas o OpenAPI nao define esse campo. A primeira implementacao nao deve encaminhar esse campo sem confirmacao, para evitar contrato tecnico divergente no MTR.
- Retornar body em sucesso seria divergente do contrato. O correto e `200 OK` sem entity.
- Validar enum/range localmente pode bloquear payload aceito pelo MTR. A proposta deixa essas regras para o MTR ate existir evidencia de regra propria do Hub.
- O `Uni<Void>` precisa ser testado no resource, service, gateway e mock para evitar resposta `204` acidental ou falha de serializacao.
- Como o mock nao possui corpo de resposta, o arquivo Markdown deve existir para manter a disciplina de mocks versionados.

## Pendencias antes da implementacao

- Nenhuma. Usuario aprovou seguir considerando o OpenAPI como verdade, usando `verificacoes` e `respostas_formulario`.

## Criterios de aceite

- Endpoint do Hub disponivel em `PATCH /arvore-documento/v1/dossie-produto/{id}/validacao-negocial`.
- Chamada MTR montada como `/simtr/dossie-produto/v1/dossie-produto/{id}/validacao-negocial` via configuracao atual.
- Request serializa/deserializa campos snake_case do contrato canonico.
- Aliases documentais `verificacoes_realizadas` e `resposta_formulario` nao sao modelados; o contrato aceito segue o OpenAPI implementado no MTR.
- Response retorna `200 OK` sem corpo.
- Simulador retorna sucesso quando habilitado.
- Gateway e usado quando simulador estiver desabilitado.
- Erros seguem o padrao atual.
- `mvn -q test` passa.
- Espaco colaborativo e documentacao consolidada sao atualizados ao final da implementacao aprovada.

## Resultado da implementacao

Implementacao concluida em 2026-07-10.

Comando executado:

```powershell
mvn -q test
```

Resultado:

- Suite passou.
- Endpoint implementado com `200 OK` sem corpo.
- Contrato canonico do OpenAPI preservado: `verificacoes` e `respostas_formulario`.
- Aliases documentais divergentes nao foram modelados.
- Ajuste posterior em 2026-07-10: `previo` foi mantido no DTO/VO, mas deixou de ser obrigatorio na validacao local do Hub.

## Ponto de aprovacao

Aprovado pelo usuario em 2026-07-10 para implementar usando o OpenAPI como fonte de verdade do contrato tecnico.
