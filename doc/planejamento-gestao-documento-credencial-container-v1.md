# Planejamento - Gestao de Documentos - Credencial de container v1

Data: 2026-07-10

Status: revisado, aprovado e implementado em 2026-07-10.

## Objetivo

Implementar no `SIMTR Hub` o fluxo vertical do Modulo Gestao de Documentos para obter uma credencial SAS de acesso ao container de documentos do MTR.

Endpoint MTR de referencia:

```http
POST /simtr-gestao-documento/v1/storage/container/credencial
```

Endpoint externo proposto para o `SIMTR Hub`, sujeito a aprovacao neste planejamento:

```http
POST /hub/v1/storage/container/credencial
```

A proposta mantem a convencao atual do Hub: o consumidor chama paths sob `/hub/v1/...`, e o detalhe interno do MTR fica encapsulado pelo REST Client.

## Fontes consultadas

- `doc/espaco-colaborativo-de-desenvolvimento.md`
- `doc/api-integracao-mtr-pre-validacao-v1.md`, secao `Modulo Gestao Documento`
- `doc/swagger-mtr/simtr-gestao-documento-openapi-2.23.1.0`
- `src/main/resources/application.properties`
- Padroes existentes de `DossieProdutoResource`, `DossieProdutoService`, `DossieProdutoClient`, `DossieProdutoGateway`, `DossieProdutoMockFactory`, `DossieProdutoMapper` e testes relacionados

Fonte tecnica obrigatoria para este endpoint:

```text
doc/swagger-mtr/simtr-gestao-documento-openapi-2.23.1.0
```

Nao usar o OpenAPI de Dossie Produto para modelar contrato, status codes ou DTOs deste endpoint.

## Contrato tecnico do OpenAPI

Operacao:

```http
POST /simtr-gestao-documento/v1/storage/container/credencial
```

Caracteristicas:

- Tags: `Negocio - Storage`
- Descricao: gera uma nova credencial de acesso compartilhado (SAS) para um container de armazenamento de documentos no Storage.
- Sem parametros de path.
- Sem parametros de query.
- Sem corpo de requisicao.
- Seguranca declarada: `apikey`.

Resposta de sucesso:

```http
200 OK
Content-Type: application/json
```

Schema de resposta do OpenAPI:

```json
{
  "sas": "string",
  "validade": "dd/MM/yyyy HH:mm:ss",
  "url_storage": "string",
  "nome_container": "string"
}
```

Campos requeridos pelo OpenAPI:

- `sas`
- `validade`
- `url_storage`

Campo opcional:

- `nome_container`

Status codes documentados para o endpoint:

- `200`: credencial SAS gerada com sucesso.
- `401`: usuario ou canal nao autenticado.
- `403`: usuario ou canal nao autorizado a utilizar o servico.
- `500`: falha nao mapeada.
- `503`: servico de terceiros indisponivel no momento.

Observacao sobre `validade`:

- O endpoint referencia o schema `Calendar`, que no OpenAPI aparece como objeto Java serializado.
- A documentacao funcional mostra `validade` como string no formato `dd/MM/yyyy HH:mm:ss`.
- A implementacao deve evitar parsing prematuro que quebre payload real aceito pelo MTR.
- Proposta tecnica inicial: modelar `validade` de forma flexivel, preservando o payload recebido do MTR, e ajustar para `String` somente se houver evidencia real de que a API sempre retorna string.

## Escopo funcional

O endpoint sera usado no fluxo de upload documental da pre-validacao:

1. A pre-validacao identifica que precisa armazenar documento no Storage do MTR.
2. Quando nao houver SAS valido em cache/controlador do consumidor, chama o `SIMTR Hub`.
3. O `SIMTR Hub` solicita a credencial ao `simtr-gestao-documento`.
4. O consumidor usa `sas`, `url_storage` e `nome_container` para enviar o binario ao Azure Blob Storage do MTR.
5. O path do documento armazenado sera usado posteriormente na inclusao de documento no Dossie Produto.

## Fora de escopo

- Fazer upload do binario para o Azure Blob Storage.
- Criar, listar ou excluir containers.
- Obter URL de acesso de documento por id.
- Vincular documento ao Dossie Produto.
- Alterar endpoints ja implementados de Parametrizacao ou Dossie Produto.
- Renomear packages, paths tecnicos ou o root `simtr-hub`.

## Decisoes tecnicas propostas

1. Criar um fluxo vertical proprio de Gestao de Documentos, separado de Dossie Produto.
2. Criar resource proprio em `api.gestaodocumento`, porque o endpoint pertence a outro modulo MTR.
3. Criar DTOs em `api.dto.gestaodocumento`.
4. Criar VOs em `domain.gestaodocumento`.
5. Criar mapper MapStruct em `mapper.gestaodocumento` com `@Mapper(componentModel = "jakarta-cdi")`.
6. Criar service em `application.gestaodocumento` com escolha entre simulador e MTR real.
7. Criar REST Client e Gateway em `infrastructure.client.gestaodocumento`.
8. Criar exception mapper especifico com recurso `simtr-gestao-documento`, reutilizando `ClientErrorBodyReader` e excecoes compartilhadas.
9. Criar mock Markdown em `src/main/resources/mock/gestaodocumento`.
10. Criar copia documental do mock em `doc/mock/gestao-documento`.
11. Usar `@Consumes(MediaType.WILDCARD)` no endpoint do Hub e no metodo do client, para aceitar chamada sem body e sem `Content-Type`.
12. Configurar o REST Client com `configKey = "gestao-documento"` e base `@Path("/gestao-documento")`.
13. Chamar o MTR real via API Manager como:

```text
base: https://api.des.caixa:8443/simtr
client path: /gestao-documento
method path: /v1/storage/container/credencial
resultado: https://api.des.caixa:8443/simtr/gestao-documento/v1/storage/container/credencial
```

14. Nao duplicar `/simtr` e nao copiar cegamente o prefixo bruto `/simtr-gestao-documento` para o REST Client.
15. Manter `@Timeout`, `@Retry` e `@CircuitBreaker` no REST Client, seguindo o padrao existente.
16. Nao aplicar validacao local de campos de request, porque o endpoint nao recebe request body.
17. Retornar `200 OK` com corpo de credencial.

## Arquivos previstos

Arquivos novos de codigo:

```text
src/main/java/br/gov/caixa/simtr/hub/api/gestaodocumento/GestaoDocumentoResource.java
src/main/java/br/gov/caixa/simtr/hub/api/dto/gestaodocumento/GestaoDocumentoCredencialContainerDto.java
src/main/java/br/gov/caixa/simtr/hub/application/gestaodocumento/GestaoDocumentoService.java
src/main/java/br/gov/caixa/simtr/hub/domain/gestaodocumento/GestaoDocumentoCredencialContainerVo.java
src/main/java/br/gov/caixa/simtr/hub/infrastructure/client/gestaodocumento/GestaoDocumentoClient.java
src/main/java/br/gov/caixa/simtr/hub/infrastructure/client/gestaodocumento/GestaoDocumentoGateway.java
src/main/java/br/gov/caixa/simtr/hub/infrastructure/client/gestaodocumento/GestaoDocumentoClientExceptionMapper.java
src/main/java/br/gov/caixa/simtr/hub/infrastructure/client/gestaodocumento/mock/GestaoDocumentoMockFactory.java
src/main/java/br/gov/caixa/simtr/hub/mapper/gestaodocumento/GestaoDocumentoMapper.java
```

Arquivos novos de mock:

```text
src/main/resources/mock/gestaodocumento/credencial-container.md
doc/mock/gestao-documento/credencial-container.md
```

Arquivos de configuracao a alterar:

```text
src/main/resources/application.properties
src/test/resources/application.properties
doc/properties/application.properties
doc/properties/application-simtr-hub.properties
doc/properties/application-dev-simtr-hub.properties
doc/properties/application-simtr-hub.yml
doc/properties/application-dev-simtr-hub.yml
```

Propriedades propostas:

```properties
quarkus.rest-client.gestao-documento.url=https://api.des.caixa:8443/simtr
quarkus.rest-client.gestao-documento.connect-timeout=3000
quarkus.rest-client.gestao-documento.read-timeout=10000

simtr-hub.simulador.gestao-documento.habilitado=false
%dev.simtr-hub.simulador.gestao-documento.habilitado=true
```

Arquivos de teste previstos:

```text
src/test/java/br/gov/caixa/simtr/hub/TestFixtures.java
src/test/java/br/gov/caixa/simtr/hub/api/ResourceEndpointTest.java
src/test/java/br/gov/caixa/simtr/hub/api/ResourceBeanCoverageTest.java
src/test/java/br/gov/caixa/simtr/hub/application/gestaodocumento/GestaoDocumentoServiceTest.java
src/test/java/br/gov/caixa/simtr/hub/infrastructure/client/GatewayTest.java
src/test/java/br/gov/caixa/simtr/hub/infrastructure/client/mock/MockFactoryTest.java
src/test/java/br/gov/caixa/simtr/hub/mapper/gestaodocumento/GestaoDocumentoMapperTest.java
```

Arquivos de documentacao a atualizar apos implementacao:

```text
doc/espaco-colaborativo-de-desenvolvimento.md
doc/documentacao-simtr-hub-arquitetura-observabilidade.md
README.md
```

## Testes previstos

Resource:

- `POST /hub/v1/storage/container/credencial` retorna `200 OK` com `sas`, `validade`, `url_storage` e `nome_container` quando simulador esta habilitado.
- Chamada sem corpo e sem `Content-Type` nao retorna `415`.
- Falha propagada pelo service e convertida pelo mapper global conforme padrao existente.

Service:

- Quando `simtr-hub.simulador.gestao-documento.habilitado=true`, usa `GestaoDocumentoMockFactory`.
- Quando `false`, usa `GestaoDocumentoGateway`.
- Propaga falhas de mock/gateway.

Gateway:

- Encaminha chamada sem request body para `GestaoDocumentoClient`.
- Registra spans/logs com dependencia `simtr-gestao-documento`.
- Propaga falhas do client.

Client exception mapper:

- `500` e `503` viram `MtrServerErrorException`.
- `401` e `403` viram `MtrClientTechnicalException`.
- Se aparecer `400`, `404`, `409` ou `422`, manter classificacao de erro de negocio por consistencia com os demais mappers.

Mock factory:

- Le arquivo Markdown `credencial-container.md`.
- Falha claramente quando o mock nao existir ou estiver invalido.

Mapper:

- Preserva todos os campos no ciclo DTO -> VO -> DTO.
- Preserva `validade` sem parsing destrutivo.

Suite:

```powershell
mvn -q test
```

## Observabilidade proposta

Spans:

```text
simtr-hub.api.gestao-documento.credencial-container.gerar
simtr-hub.service.gestao-documento.credencial-container.gerar
mtr.gestao-documento.credencial-container.gerar
```

Eventos de log sugeridos:

```text
simtr-hub.gestao-documento.credencial-container.requisicao.recebida
simtr-hub.gestao-documento.credencial-container.resposta.enviada
simtr-hub.gestao-documento.credencial-container.requisicao.falhou
simtr-hub.gestao-documento.credencial-container.service.iniciado
simtr-hub.gestao-documento.credencial-container.service.concluido
simtr-hub.gestao-documento.credencial-container.service.falhou
mtr.gestao-documento.credencial-container.chamada.iniciada
mtr.gestao-documento.credencial-container.chamada.concluida
mtr.gestao-documento.credencial-container.chamada.falhou
```

Campos importantes:

- `dependencia=simtr-gestao-documento`
- `operacao=gerar-credencial-container-v1`
- `nome_container`, quando retornado e seguro para log
- `resultado`
- `erro_tipo`

Nao registrar o valor de `sas` em logs de aplicacao por ser credencial sensivel. A implementacao atual mascara campos sensiveis no `RestClientObservabilityFilter` antes de registrar payloads de request/response em logs ou spans.

## Riscos e pontos de atencao

- `validade` diverge entre OpenAPI (`Calendar` objeto) e documentacao funcional (string `dd/MM/yyyy HH:mm:ss`).
- O valor `sas` e segredo operacional; risco mitigado pelo mascaramento de campos sensiveis no `RestClientObservabilityFilter`.
- O roteamento real do API Manager para `simtr-gestao-documento` foi inferido pelo padrao atual (`/simtr/gestao-documento/...`) e deve ser confirmado em teste integrado ou evidencia de ambiente.
- O endpoint nao declara `400` no OpenAPI, mas o mapper deve continuar robusto para status funcionais retornados pelo MTR.
- Como o endpoint nao recebe corpo, qualquer `@Consumes(MediaType.APPLICATION_JSON)` indevido pode causar `415` em chamadas sem `Content-Type`.

## Criterios de aceite

- Planejamento revisado e aprovado pelo usuario antes da implementacao.
- Endpoint externo aprovado ou ajustado no plano.
- Implementacao vertical completa com resource, service, gateway, client, mapper, DTO/VO e simulador.
- `POST /hub/v1/storage/container/credencial` retorna `200 OK` no simulador.
- Chamada sem body e sem `Content-Type` funciona.
- MTR real e chamado em modo nao simulado pelo caminho versionado correto, sem duplicar `/simtr`.
- Campo `sas` nao fica exposto em logs de aplicacao.
- Testes unitarios/cobertura adicionados para resource, service, gateway, mock e mapper.
- `mvn -q test` passa.
- `doc/espaco-colaborativo-de-desenvolvimento.md` e `doc/documentacao-simtr-hub-arquitetura-observabilidade.md` atualizados ao final da implementacao.

## Resultado da implementacao

Decisoes aprovadas pelo usuario em 2026-07-10:

1. Expor no Hub o path `POST /hub/v1/storage/container/credencial`.
2. Criar modulo tecnico separado `gestaodocumento`.
3. Tratar `validade` de forma flexivel para acomodar string e objeto `Calendar`.
4. Nao registrar `sas` em logs de payload.

Implementacao realizada:

- Criado fluxo vertical `GestaoDocumentoResource` -> `GestaoDocumentoService` -> `GestaoDocumentoGateway`/`GestaoDocumentoMockFactory` -> `GestaoDocumentoClient`.
- Criados DTO, VO e mapper MapStruct especificos para credencial de container.
- Criado REST Client `gestao-documento` com base `@Path("/gestao-documento")` e metodo `POST /v1/storage/container/credencial`.
- Criado simulador Markdown em `src/main/resources/mock/gestaodocumento/credencial-container.md` e copia documental em `doc/mock/gestao-documento/credencial-container.md`.
- Adicionadas propriedades `quarkus.rest-client.gestao-documento.*` e `simtr-hub.simulador.gestao-documento.habilitado`.
- `RestClientObservabilityFilter` passou a mascarar campos sensiveis como `sas`, `token`, `client_secret`, `apikey` e `password` antes de registrar payloads.
- Testes adicionados para endpoint HTTP sem body/sem `Content-Type`, resource CDI, service, gateway, mock factory, mapper e mascaramento de payload.

Validacao executada:

```powershell
mvn -q test
```

Resultado: suite passou.

## Perguntas respondidas na revisao

1. Aprova expor no Hub o path `POST /hub/v1/storage/container/credencial`?
2. Aprova criar modulo tecnico separado `gestaodocumento`, em vez de reaproveitar classes de Dossie Produto?
3. Aprova tratar `validade` de forma flexivel inicialmente para acomodar tanto string quanto objeto `Calendar`?
4. Aprova incluir no escopo da implementacao o cuidado para nao registrar `sas` em logs de payload?

Resposta do usuario: tudo aprovado.
