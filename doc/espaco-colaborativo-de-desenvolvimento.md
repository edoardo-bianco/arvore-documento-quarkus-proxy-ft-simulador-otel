# Espaco colaborativo de desenvolvimento: SIMTR Hub

Data inicial: 2026-07-10.

## Objetivo do documento

Este arquivo e o espaco de trabalho compartilhado dos agentes que continuarem a evolucao do SIMTR Hub.

Ele deve registrar:

- decisoes arquiteturais consolidadas;
- escopo funcional geral da solucao;
- endpoints ja implementados;
- endpoints restantes;
- rotina obrigatoria de trabalho;
- testes executados;
- cobertura esperada;
- pendencias e decisoes por endpoint migrado.

Todo agente que implementar ou revisar um endpoint deve atualizar este arquivo ao final do trabalho, informando o que foi feito, o que ficou pendente, quais arquivos foram alterados e quais testes foram executados.

Quando a rodada consolidar uma decisao arquitetural, operacional, de contrato, observabilidade, testes ou padrao de implementacao, o agente tambem deve atualizar:

```text
doc/documentacao-simtr-hub-arquitetura-observabilidade.md
```

Este arquivo colaborativo registra o andamento e as pendencias. A documentacao principal registra o estado consolidado que outras pessoas devem usar para entender a solucao.

## Escopo funcional geral

O `SIMTR Hub` deve permitir integracao com os seguintes modulos MTR:

- Modulo Parametrizacao;
- Modulo Dossie Produto;
- Modulo Gestao de Documentos.

## Endpoints MTR contemplados na visao da solucao

### Modulo Parametrizacao

```http
GET /simtr-parametrizacao/v2/patriarca/processo/identificador-negocial/{identificador}
GET /simtr-parametrizacao/v1/cadastro/checklist/identificador-negocial/{identificador}/versao/{versao}
```

### Modulo Dossie Produto

```http
POST /simtr-dossie-produto/v1/dossie-produto
PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/formulario
PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/garantia
PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/produto
POST /simtr-dossie-produto/v1/dossie-produto/{id}/capturar
POST /simtr-dossie-produto/v1/dossie-produto/{id}/workflow
POST /simtr-dossie-produto/v1/dossie-produto/{id}/cancelar
GET /simtr-dossie-produto/v2/dossie-produto/{id}
POST /simtr-dossie-produto/v2/dossie-produto/{id}/documento
PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/validacao-negocial
```

### Modulo Gestao de Documentos

```http
POST /simtr-gestao-documento/v1/storage/container/credencial
```

## Escopo implementado nesta entrega

Nesta etapa, estao implementados e consolidados no `SIMTR Hub`:

```http
GET /hub/v1/processo/identificador-negocial/{identificador}
GET /hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}
POST /hub/v1/dossie-produto
PATCH /hub/v1/dossie-produto/{id}/formulario
POST /hub/v1/dossie-produto/{id}/documento
POST /hub/v1/dossie-produto/{id}/workflow
PATCH /hub/v1/dossie-produto/{id}/validacao-negocial
POST /hub/v1/storage/container/credencial
```

Esses endpoints cobrem:

- consulta da parametrizacao do processo;
- consulta de checklist por identificador e versao;
- criacao basica de Dossie Produto em modo rascunho;
- inclusao ou edicao de respostas de formulario no Dossie Produto.
- inclusao de documento no Dossie Produto com retorno de `id_documento` e `id_instancia_documento`.
- inicio ou avanco do workflow do Dossie Produto com retorno do `id` do dossie.
- registro de validacao negocial dos checklists analisados no Dossie Produto, seguindo o contrato tecnico do OpenAPI com `verificacoes` e `respostas_formulario`.
- geracao de credencial SAS para upload documental no Storage do MTR, sem expor `sas` em logs.

Tambem foi consolidada a migracao dos mappers para MapStruct:

```text
src/main/java/br/gov/caixa/simtr/hub/parametrizacao/mapeamento/ChecklistMapper.java
src/main/java/br/gov/caixa/simtr/hub/parametrizacao/mapeamento/ProcessoMapper.java
src/main/java/br/gov/caixa/simtr/hub/dossieproduto/mapeamento/DossieProdutoMapper.java
src/main/java/br/gov/caixa/simtr/hub/gestaodocumento/mapeamento/GestaoDocumentoMapper.java
```

Implementacoes MapStruct geradas e validadas:

```text
target/generated-sources/annotations/.../ChecklistMapperImpl.java
target/generated-sources/annotations/.../ProcessoMapperImpl.java
target/generated-sources/annotations/.../DossieProdutoMapperImpl.java
target/generated-sources/annotations/.../GestaoDocumentoMapperImpl.java
```

Validacao executada:

```powershell
mvn -q test
```

Resultado: suite passou apos a migracao de `DossieProdutoMapper`, passou novamente apos a migracao de `ProcessoMapper` e passou apos a implementacao de Gestao de Documentos.

## Matriz de endpoints da solucao

| Modulo | Endpoint MTR | Endpoint SIMTR Hub | Status | Observacao |
| --- | --- | --- | --- | --- |
| Parametrizacao | `GET /simtr-parametrizacao/v2/patriarca/processo/identificador-negocial/{identificador}` | `GET /hub/v1/processo/identificador-negocial/{identificador}` | Implementado | Preservar fluxo atual. |
| Parametrizacao | `GET /simtr-parametrizacao/v1/cadastro/checklist/identificador-negocial/{identificador}/versao/{versao}` | `GET /hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}` | Implementado | Preservar fluxo atual. |
| Dossie Produto | `POST /simtr-dossie-produto/v1/dossie-produto` | `POST /hub/v1/dossie-produto` | Implementado | Criacao basica em modo rascunho. |
| Dossie Produto | `PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/formulario` | `PATCH /hub/v1/dossie-produto/{id}/formulario` | Implementado | Ja existe no codigo; deve ser preservado. |
| Dossie Produto | `PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/garantia` | `PATCH /hub/v1/dossie-produto/{id}/garantia` | Pendente | Proximo endpoint candidato. |
| Dossie Produto | `PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/produto` | `PATCH /hub/v1/dossie-produto/{id}/produto` | Pendente | Implementar em passo separado. |
| Dossie Produto | `POST /simtr-dossie-produto/v1/dossie-produto/{id}/capturar` | `POST /hub/v1/dossie-produto/{id}/capturar` | Pendente | Implementar em passo separado. |
| Dossie Produto | `POST /simtr-dossie-produto/v1/dossie-produto/{id}/workflow` | `POST /hub/v1/dossie-produto/{id}/workflow` | Implementado | Sem corpo de requisicao; retorna `200 OK` com `{ "id": ... }`. |
| Dossie Produto | `POST /simtr-dossie-produto/v1/dossie-produto/{id}/cancelar` | `POST /hub/v1/dossie-produto/{id}/cancelar` | Pendente | Implementar em passo separado. |
| Dossie Produto | `GET /simtr-dossie-produto/v2/dossie-produto/{id}` | `GET /hub/v1/dossie-produto/{id}` | Pendente | Confirmar contrato externo do hub antes de implementar. |
| Dossie Produto | `POST /simtr-dossie-produto/v2/dossie-produto/{id}/documento` | `POST /hub/v1/dossie-produto/{id}/documento` | Implementado | Vincula documento ja armazenado ao Dossie Produto e retorna `id_documento` e `id_instancia_documento`. |
| Dossie Produto | `PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/validacao-negocial` | `PATCH /hub/v1/dossie-produto/{id}/validacao-negocial` | Implementado | Retorna `200 OK` sem corpo; contrato tecnico segue OpenAPI com `verificacoes` e `respostas_formulario`. |
| Gestao de Documentos | `POST /simtr-gestao-documento/v1/storage/container/credencial` | `POST /hub/v1/storage/container/credencial` | Implementado | Sem body e sem `Content-Type`; retorna `200 OK` com `sas`, `validade`, `url_storage` e `nome_container`; usa obrigatoriamente `doc/swagger-mtr/simtr-gestao-documento-openapi-2.23.1.0` e mascara `sas` em payloads logados. |

## Fontes obrigatorias para novos endpoints

Antes de implementar qualquer endpoint pendente, ler a documentacao funcional e o OpenAPI tecnico do modulo na secao exata do endpoint:

1. Documentacao funcional e de integracao:

```text
doc/api-integracao-mtr-pre-validacao-v1.md
```

Usar esse arquivo para entender:

- intencao de negocio;
- momento do fluxo de pre-validacao;
- comportamento esperado;
- relacao com workflow, documentos, validacao negocial, Dossie Produto e Gestao de Documentos.

2. Contrato OpenAPI tecnico de Dossie Produto:

```text
doc/swagger-mtr/simtr-dossie-produto-openapi- 2.20.0.8
```

Esse arquivo representa a familia `simtr-dossie-produto-openapi- 2.20.0.x` disponivel localmente. Usar o path correspondente para validar:

- metodo HTTP;
- path;
- parametros de path/query;
- request body;
- response body;
- status codes;
- schemas;
- campos obrigatorios/opcionais;
- versao do endpoint (`v1`, `v2`, `v3`, `v4`).

3. Contrato OpenAPI tecnico de Gestao de Documentos:

```text
doc/swagger-mtr/simtr-gestao-documento-openapi-2.23.1.0
```

Esse arquivo representa o contrato tecnico local do `simtr-gestao-documento`. Para o endpoint de credencial de container, usar obrigatoriamente:

```http
POST /simtr-gestao-documento/v1/storage/container/credencial
```

Usar esse OpenAPI para validar:

- metodo HTTP;
- path;
- ausencia ou presenca de parametros;
- ausencia ou presenca de request body;
- response body;
- status codes;
- schemas;
- campos obrigatorios/opcionais;
- seguranca declarada.

Para Gestao de Documentos, nao usar o OpenAPI de Dossie Produto como fonte tecnica de contrato.

Se a documentacao funcional e o OpenAPI parecerem divergentes:

- usar `api-integracao-mtr-pre-validacao-v1.md` para entender o comportamento de negocio;
- usar o OpenAPI para modelar contrato tecnico, DTOs e status codes;
- preservar a convencao de roteamento ja implementada no API Manager;
- registrar a decisao neste documento antes de seguir para o proximo endpoint.

Se payloads reais, evidencias de homologacao ou comportamento observado no MTR mostrarem que o OpenAPI bruto esta mais restritivo que o contrato aceito:

- nao aplicar validacao local mais restritiva que o MTR;
- preferir aceitar o payload real no SIMTR Hub e deixar validacoes negociais especificas para o MTR, salvo regra explicita do Hub;
- remover `@NotNull`/validacoes locais de campos opcionais ou divergentes;
- criar teste de regressao com o payload real ou com o menor payload valido conhecido;
- registrar a divergencia e a decisao neste documento e na documentacao consolidada.

## Decisoes arquiteturais consolidadas

### Nomenclatura, package root e organizacao por dominio

O nome funcional consolidado da solucao e `SIMTR Hub`.

O root Java consolidado e:

```text
br.gov.caixa.simtr.hub
```

A identidade tecnica consolidada usa:

```text
artifactId/name: simtr-hub
quarkus.application.name: simtr-hub
paths externos: /hub/...
propriedades: simtr-hub.*
logs locais: target/logs/simtr-hub.json
spans/eventos: simtr-hub.*
```

A organizacao atual e por dominio de negocio:

```text
br.gov.caixa.simtr.hub
|-- parametrizacao
|   |-- fachada
|   |-- recurso/rest/v1
|   |-- servico
|   |-- dominio
|   |-- integracao
|   `-- mapeamento
|-- dossieproduto
|   |-- fachada
|   |-- recurso/rest/v1
|   |-- servico
|   |-- dominio
|   |-- integracao
|   `-- mapeamento
|-- gestaodocumento
|   |-- fachada
|   |-- recurso/rest/v1
|   |-- servico
|   |-- dominio
|   |-- integracao
|   `-- mapeamento
`-- arquitetura
    |-- configuracao/mock
    |-- seguranca
    |-- observabilidade
    `-- excecao
```

Pacotes vazios como `repositorio`, `resiliencia`, `mensageria` e `excecao` de dominio nao devem ser criados ate haver classe real ou `package-info.java` aprovado.

### Fluxo por dominio

Fluxo padrao:

```text
Resource
  -> Mapper DTO/VO
  -> Fachada
      -> Service
          -> se simulador=true
              -> MockFactory
          -> se simulador=false
              -> Gateway
                  -> REST Client MTR
          -> Mapper.toVo(...)
  -> Mapper.toDto(...)
```

Para Dossie Produto, o fluxo padrao e:

```text
DossieProdutoResource
  -> DossieProdutoMapper.toVo(...)
  -> DossieProdutoFachada
  -> DossieProdutoService
      -> se simulador=true
          -> DossieProdutoMockFactory
      -> se simulador=false
          -> DossieProdutoGateway
              -> DossieProdutoClient
                  -> API Manager / simtr-dossie-produto
      -> DossieProdutoMapper.toVo(...)
  -> DossieProdutoMapper.toDto(...)
```

Nao remover a passagem por VO mesmo quando DTO e VO forem estruturalmente parecidos.

### MapStruct

Padrao consolidado:

```java
@Mapper(componentModel = "jakarta-cdi")
public interface NomeMapper {
}
```

Regras:

- CDI cria e injeta os mappers.
- Nao instanciar mapper com `new NomeMapper()`.
- Nao usar `Mappers.getMapper(...)`.
- Nao usar `ObjectMapper.convertValue(...)` para substituir fronteira DTO/VO.
- DTO -> VO -> DTO continua sendo fronteira arquitetural deliberada.
- Inconsistencias de mapeamento devem falhar em build/teste via MapStruct sempre que possivel.

### Dossie Produto

Todos os proximos endpoints de Dossie Produto devem continuar no mesmo resource:

```text
src/main/java/br/gov/caixa/simtr/hub/dossieproduto/recurso/rest/v1/DossieProdutoResource.java
```

Path externo base do proxy:

```java
@Path("/hub/v1/dossie-produto")
```

Nao criar um resource separado por endpoint sem decisao explicita.

Manter o REST Client existente:

```text
src/main/java/br/gov/caixa/simtr/hub/dossieproduto/integracao/DossieProdutoClient.java
```

Configuracao atual:

```java
@RegisterRestClient(configKey = "dossie-produto")
@Path("/dossie-produto")
```

As versoes do servico ficam nos paths dos metodos do client:

```text
POST /v1/dossie-produto
PATCH /v1/dossie-produto/{id}/formulario
POST /v2/dossie-produto/{id}/documento
POST /v1/dossie-produto/{id}/workflow
```

No `application.properties`, a URL ja inclui `/simtr`:

```properties
quarkus.rest-client.dossie-produto.url=https://api.des.caixa:8443/simtr
```

Resultado real via API Manager:

```text
https://api.des.caixa:8443/simtr/dossie-produto/v1/dossie-produto...
```

Importante:

- A documentacao bruta usa paths como `/simtr-dossie-produto/v1/dossie-produto`.
- A implementacao atual chama via API Manager com base `/simtr` e client path `/dossie-produto/...`.
- Nao duplicar `/simtr` no `@Path`.
- Nao copiar cegamente o prefixo `/simtr-dossie-produto` para o REST Client sem validar o roteamento do API Manager.

### Simulador

O simulador de Dossie Produto e controlado por:

```properties
simtr-hub.simulador.dossie-produto.habilitado=false
%dev.simtr-hub.simulador.dossie-produto.habilitado=true
```

Quando `true`, o service usa `DossieProdutoMockFactory` e nao chama o REST Client real.

Quando `false`, o service usa `DossieProdutoGateway` e chama `DossieProdutoClient`.

Mocks devem permanecer em:

```text
src/main/resources/mock/dossieproduto
```

Nao trocar mocks Markdown por implementacao hardcoded sem decisao explicita.

### Contrato de inclusao de documento v2

Para o endpoint:

```http
POST /hub/v1/dossie-produto/{id}/documento
```

Foi validado com payload real que:

- `atributos` pode conter itens com apenas `chave` e `valor`;
- `atributos[].objeto` e opcional no contrato aceito pelo fluxo real;
- `propriedades` e opcional no corpo da requisicao;
- quando `propriedades` vier informado, `propriedades[].objeto` tambem deve ser tratado como opcional pelo Hub;
- o Hub nao deve retornar `ARVDOCP0001` por ausencia de `objeto` em atributos ou propriedades de documento.

A experiencia que motivou esta regra:

- a primeira implementacao seguiu o `required` do OpenAPI bruto e colocou `@NotNull` em `DossieProdutoDocumentoAtributoDto.objeto`;
- payload real com dezenas de atributos sem `objeto` foi rejeitado localmente antes de chamar o MTR;
- o erro retornado repetia `O objeto do atributo do documento deve ser informado.`;
- a correcao foi remover a obrigatoriedade local de `objeto` em atributos e propriedades e adicionar teste de regressao com atributo sem `objeto`.

Se essa mensagem voltar a aparecer, conferir primeiro:

```text
src/main/java/br/gov/caixa/simtr/hub/dossieproduto/recurso/rest/v1/dto/DossieProdutoDocumentoAtributoDto.java
src/main/java/br/gov/caixa/simtr/hub/dossieproduto/recurso/rest/v1/dto/DossieProdutoDocumentoPropriedadeDto.java
```

Tambem confirmar se a instancia em execucao foi reiniciada ou rebuildada. Se o codigo local nao tiver `@NotNull` em `objeto`, mas a API ainda retornar essa mensagem, a chamada provavelmente esta chegando em artefato antigo.

## Rotina obrigatoria para o proximo agente

Para cada endpoint ou incremento funcional:

1. Ler este documento antes de editar codigo.
2. Escolher apenas um endpoint ou incremento por ciclo de trabalho.
3. Ler `doc/api-integracao-mtr-pre-validacao-v1.md` na secao exata do endpoint.
4. Ler `doc/swagger-mtr/simtr-dossie-produto-openapi- 2.20.0.8` no path exato do endpoint quando for Dossie Produto.
5. Ler `doc/swagger-mtr/simtr-gestao-documento-openapi-2.23.1.0` no path exato do endpoint quando for Gestao de Documentos.
6. Conferir a implementacao existente antes de criar qualquer classe nova.
7. Antes de implementar, criar um arquivo Markdown de planejamento em `doc/`, com nome descritivo do endpoint ou incremento.
8. O planejamento deve conter objetivo, fontes consultadas, contrato externo, endpoint exposto pelo SIMTR Hub, decisoes tecnicas propostas, arquivos previstos, testes previstos, riscos, pendencias e criterios de aceite.
9. Depois de criar o planejamento, perguntar explicitamente ao usuario se ele revisou e aprova seguir para implementacao.
10. Nao iniciar alteracoes de codigo de implementacao antes da aprovacao do usuario.
11. Implementar o fluxo vertical completo, respeitando o padrao ja desenvolvido, somente apos aprovacao do planejamento.
12. Criar ou ajustar testes unitarios para o novo desenvolvimento.
13. Manter cobertura minima de 80% de linhas para o novo desenvolvimento e nao reduzir a cobertura global abaixo da meta vigente.
14. Rodar a suite a cada implementacao:

```powershell
mvn -q test
```

15. Se a suite falhar, corrigir antes de iniciar outro endpoint.
16. Atualizar este documento com:

```text
data
endpoint
objetivo
planejamento revisado/aprovado
arquivos alterados
testes criados/ajustados
resultado dos testes
cobertura quando aplicavel
decisoes tomadas
pendencias restantes
```

17. Atualizar `doc/documentacao-simtr-hub-arquitetura-observabilidade.md` sempre que houver decisao consolidada ou mudanca de padrao que outras pessoas precisem conhecer.

Nao deixar implementacao sem planejamento aprovado, sem teste, sem atualizacao deste espaco colaborativo e sem consolidar na documentacao principal as decisoes que deixaram de ser apenas contexto de trabalho.

## Planejamentos de implementacao

Antes de qualquer nova implementacao, registrar o plano em um Markdown proprio e aguardar revisao do usuario.

Regra reforcada em 2026-07-10:

- Todo novo endpoint ou incremento funcional deve ter um `doc/planejamento-*.md` criado ou atualizado antes de qualquer alteracao de codigo de implementacao.
- O planejamento deve ser registrado nesta secao com status claro.
- O agente deve perguntar explicitamente ao usuario se o plano foi revisado e aprovado.
- O agente deve parar no ponto de aprovacao e nao iniciar implementacao, testes de implementacao ou alteracoes de codigo antes da aprovacao explicita do usuario.
- Se o usuario pedir para implementar diretamente, mas ainda nao houver planejamento aprovado, criar o planejamento primeiro e pedir revisao.

Regra operacional obrigatoria:

1. Criar ou atualizar um arquivo `doc/planejamento-*.md` antes de qualquer alteracao de codigo de implementacao.
2. Registrar o planejamento nesta secao com status claro.
3. Perguntar explicitamente ao usuario se o plano foi revisado e aprovado.
4. Somente iniciar alteracoes de codigo apos aprovacao explicita do usuario.
5. Se o plano mudar durante a implementacao, atualizar o Markdown e registrar a decisao neste espaco colaborativo.

Planejamentos registrados:

- `doc/planejamento-reorganizacao-packages-dominio-v1.md` - reorganizacao do root Java para `br.gov.caixa.simtr.hub` e migracao de estrutura por camada para estrutura por dominio de negocio (`parametrizacao`, `dossieproduto`, `gestaodocumento` e `arquitetura`). Revisado e aprovado pelo usuario em 2026-07-10; implementacao autorizada em etapas com checkpoints.
- `doc/planejamento-gestao-documento-credencial-container-v1.md` - `POST /simtr-gestao-documento/v1/storage/container/credencial` para gerar credencial SAS de container no Modulo Gestao de Documentos. Revisado, aprovado e implementado em 2026-07-10.
- `doc/planejamento-dossie-produto-validacao-negocial-v1.md` - `PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/validacao-negocial` para registrar resultados de validacao negocial dos checklists analisados no Dossie Produto. Revisado, aprovado e implementado em 2026-07-10.
- `doc/planejamento-dossie-produto-documento-v2.md` - `POST /simtr-dossie-produto/v2/dossie-produto/{id}/documento` para vincular documento ao Dossie Produto e retornar `id_documento` e `id_instancia_documento`. Revisado, aprovado e implementado em 2026-07-10.
- `doc/planejamento-dossie-produto-workflow-v1.md` - `POST /simtr-dossie-produto/v1/dossie-produto/{id}/workflow` para iniciar ou avancar o fluxo de um Dossie Produto. Revisado, aprovado e implementado em 2026-07-10.
- `doc/planejamento-ajuste-mockito-java-agent.md` - ajuste do build de testes para configurar Mockito como Java agent no Surefire e evitar auto-anexo dinamico do Byte Buddy. Revisado, aprovado e implementado em 2026-07-10.

## Padrao para adicionar novo endpoint de Dossie Produto

Para cada novo endpoint:

1. Criar DTOs em:

```text
src/main/java/br/gov/caixa/simtr/hub/dossieproduto/recurso/rest/v1/dto
```

2. Criar VOs equivalentes em:

```text
src/main/java/br/gov/caixa/simtr/hub/dossieproduto/dominio
```

3. Adicionar metodos no `DossieProdutoMapper`, mantendo interface MapStruct e CDI.
4. Modelar validacoes locais de DTO com cautela:

```text
validar path params obrigatorios do Hub
validar body obrigatorio quando o endpoint exigir corpo
validar campos internos somente quando houver regra confirmada do Hub ou contrato real inequívoco
nao transformar campos divergentes/opcionais do MTR em @NotNull apenas porque o OpenAPI bruto marcou required
```

5. Adicionar metodo no `DossieProdutoClient` com:

```text
metodo HTTP correto
@Path correto relativo ao @Path base do client
@PathParam e/ou @QueryParam quando aplicavel
@Timeout
@Retry
@CircuitBreaker
mesmo @ClientExceptionMapper ja existente
```

6. Adicionar metodo no `DossieProdutoGateway`, mantendo logs de dependencia, span e propagacao de erro.
7. Adicionar metodo no `DossieProdutoMockFactory`.
8. Criar mock em `src/main/resources/mock/dossieproduto`.
9. Adicionar metodo no `DossieProdutoService`, mantendo decisao simulador vs MTR.
10. Adicionar metodo na `DossieProdutoFachada`.
11. Adicionar endpoint no `DossieProdutoResource`.
12. Adicionar ou ajustar testes.
13. Rodar `mvn -q test`.
14. Atualizar este documento.

## Padrao para adicionar novo endpoint de Gestao de Documentos

Para cada novo endpoint de Gestao de Documentos:

1. Criar ou atualizar primeiro o planejamento Markdown em `doc/planejamento-*.md`.
2. Registrar o planejamento em `Planejamentos registrados` com status claro.
3. Perguntar explicitamente ao usuario se o plano foi revisado e aprovado.
4. Aguardar aprovacao explicita antes de alterar codigo de implementacao.
5. Usar como fonte tecnica obrigatoria:

```text
doc/swagger-mtr/simtr-gestao-documento-openapi-2.23.1.0
```

6. Criar DTOs em:

```text
src/main/java/br/gov/caixa/simtr/hub/gestaodocumento/recurso/rest/v1/dto
```

7. Criar VOs equivalentes em:

```text
src/main/java/br/gov/caixa/simtr/hub/gestaodocumento/dominio
```

8. Criar mapper MapStruct em:

```text
src/main/java/br/gov/caixa/simtr/hub/gestaodocumento/mapeamento/GestaoDocumentoMapper.java
```

9. Criar resource e service proprios do modulo:

```text
src/main/java/br/gov/caixa/simtr/hub/gestaodocumento/recurso/rest/v1
src/main/java/br/gov/caixa/simtr/hub/gestaodocumento/fachada
src/main/java/br/gov/caixa/simtr/hub/gestaodocumento/servico
```

10. Criar REST Client, Gateway, exception mapper e mock factory proprios:

```text
src/main/java/br/gov/caixa/simtr/hub/gestaodocumento/integracao
```

11. Manter DTO -> VO -> DTO como fronteira arquitetural.
12. Usar `@RegisterRestClient(configKey = "gestao-documento")`.
13. Usar base do REST Client coerente com o API Manager, prevista como `@Path("/gestao-documento")`, com URL base `https://api.des.caixa:8443/simtr`.
14. Nao duplicar `/simtr` no REST Client.
15. Nao copiar cegamente o prefixo bruto `/simtr-gestao-documento` para o REST Client sem validar o roteamento do API Manager.
16. Para endpoint sem request body, configurar consumo de forma que chamada sem corpo e sem `Content-Type` nao gere `415`.
17. Criar mocks Markdown em:

```text
src/main/resources/mock/gestaodocumento
doc/mock/gestao-documento
```

18. Tratar credenciais e tokens, como `sas`, como dados sensiveis e evitar exposicao em logs.
19. Criar testes de resource, service, gateway, mock factory e mapper.
20. Rodar `mvn -q test` apos implementacao.

## Observabilidade obrigatoria para novos endpoints

No resource:

- usar `@WithSpan(..., kind = SpanKind.SERVER)`;
- definir `http.route`;
- definir atributos de negocio relevantes no span;
- registrar recebimento com `ObservabilityLog.info`;
- registrar sucesso com `ObservabilityLog.info`;
- usar `.onFailure().invoke(...)`;
- em falha:
  - `span.recordException(erro)`;
  - `span.setStatus(StatusCode.ERROR, ...)`;
  - `ObservabilityLog.error(...)`.

No service:

- usar `@WithSpan`;
- registrar se simulador esta habilitado;
- registrar origem `mock` ou `mtr`;
- registrar ids/contadores relevantes;
- registrar sucesso;
- registrar falha com tipo da excecao.

No gateway:

- registrar dependencia;
- registrar metodo/operacao;
- registrar ids de negocio relevantes;
- propagar falhas do client sem mascarar o erro padronizado.

Campos minimos de log:

```text
camada
componente
operacao
resultado
erro_tipo quando houver falha
ids/contadores de negocio aplicaveis
```

## Testes esperados para novos endpoints

Sempre cobrir, conforme aplicavel:

- endpoint retorna status esperado;
- validacao de path/body retorna erro padronizado;
- service escolhe mock quando `simulador=true`;
- service escolhe gateway quando `simulador=false`;
- gateway repassa parametros para client;
- gateway propaga falha do client;
- mock funciona com o arquivo em `src/main/resources/mock`;
- mapper preserva DTO -> VO -> DTO;
- listas nulas e objetos aninhados nulos quando o contrato permitir;
- payload real ou payload minimo valido com campos opcionais omitidos, especialmente quando o OpenAPI bruto tiver indicado obrigatoriedade duvidosa;
- exception mapper continua padronizando erros;
- endpoints existentes continuam passando.

Sempre executar:

```powershell
mvn -q test
```

## Testes e cobertura

O projeto usa somente `quarkus-jacoco`, sem `jacoco-maven-plugin`.

Relatorio:

```text
target/jacoco-report/index.html
```

Configuracao em `src/test/resources/application.properties`:

```properties
quarkus.http.test-port=8082
quarkus.jacoco.report=true
quarkus.jacoco.report-location=target/jacoco-report
quarkus.jacoco.title=simtr-hub
```

O profile de testes deve usar a porta HTTP `8082`, evitando a porta padrao `8081` do Quarkus em execucoes locais e de pipeline.

Meta vigente:

```text
LINE coverage >= 80%
```

Nao reintroduzir `jacoco-maven-plugin` sem decisao explicita.

## Historico de trabalho dos agentes

### 2026-07-10 - Codex - Porta HTTP do profile test

Objetivo:
- Fixar a porta HTTP dos testes Quarkus em `8082`.

Feito:
- Adicionado `quarkus.http.test-port=8082` em `src/test/resources/application.properties`.
- Atualizados templates documentais `doc/properties/application-simtr-hub.properties` e `doc/properties/application-simtr-hub.yml`, substituindo `8083` por `8082`.
- Atualizadas a documentacao consolidada e a secao de testes deste espaco colaborativo.

Comandos executados:
- `mvn -q test`

Resultado dos testes:
- Suite passou.

Decisoes:
- O profile de testes deve usar a porta HTTP `8082`, evitando depender da porta padrao `8081` do Quarkus.

Pendencias:
- Nenhuma.

### 2026-07-10 - Codex - POST credencial container Gestao de Documentos v1

Objetivo:
- Implementar `POST /hub/v1/storage/container/credencial` como proxy do `POST /simtr-gestao-documento/v1/storage/container/credencial`.

Planejamento:
- `doc/planejamento-gestao-documento-credencial-container-v1.md` criado, revisado e aprovado pelo usuario antes da implementacao.
- Usuario aprovou expor o path do Hub, criar modulo tecnico separado `gestaodocumento`, tratar `validade` de forma flexivel e impedir exposicao de `sas` em logs de payload.

Feito:
- Criados DTO, VO e mapper MapStruct especificos para credencial de container.
- Criado `GestaoDocumentoResource` com endpoint sem body e `@Consumes(MediaType.WILDCARD)` para aceitar chamada sem `Content-Type`.
- Criado `GestaoDocumentoService` com escolha entre simulador e MTR real por `simtr-hub.simulador.gestao-documento.habilitado`.
- Criados `GestaoDocumentoClient`, `GestaoDocumentoGateway` e `GestaoDocumentoClientExceptionMapper`.
- Criado mock runtime e copia documental para `credencial-container.md`.
- Adicionadas propriedades `quarkus.rest-client.gestao-documento.*` e simulador de Gestao de Documentos nas configuracoes runtime, teste e documentais.
- Atualizado `RestClientObservabilityFilter` para mascarar campos sensiveis, incluindo `sas`, antes de registrar payloads em logs e spans.
- Atualizados README, documentacao consolidada e este espaco colaborativo.

Arquivos alterados:
- `src/main/java/br/gov/caixa/simtr/hub/api/gestaodocumento/GestaoDocumentoResource.java`
- `src/main/java/br/gov/caixa/simtr/hub/api/dto/gestaodocumento/GestaoDocumentoCredencialContainerDto.java`
- `src/main/java/br/gov/caixa/simtr/hub/application/gestaodocumento/GestaoDocumentoService.java`
- `src/main/java/br/gov/caixa/simtr/hub/domain/gestaodocumento/GestaoDocumentoCredencialContainerVo.java`
- `src/main/java/br/gov/caixa/simtr/hub/infrastructure/client/gestaodocumento/*`
- `src/main/java/br/gov/caixa/simtr/hub/infrastructure/client/gestaodocumento/mock/GestaoDocumentoMockFactory.java`
- `src/main/java/br/gov/caixa/simtr/hub/mapper/gestaodocumento/GestaoDocumentoMapper.java`
- `src/main/java/br/gov/caixa/simtr/hub/infrastructure/client/RestClientObservabilityFilter.java`
- `src/main/resources/mock/gestaodocumento/credencial-container.md`
- `doc/mock/gestao-documento/credencial-container.md`
- `src/main/resources/application.properties`
- `src/test/resources/application.properties`
- `doc/properties/application*.properties`
- `doc/properties/application*.yml`
- `src/test/java/br/gov/caixa/simtr/hub/**/*GestaoDocumento*Test.java`
- `src/test/java/br/gov/caixa/simtr/hub/api/ResourceEndpointTest.java`
- `src/test/java/br/gov/caixa/simtr/hub/api/ResourceBeanCoverageTest.java`
- `src/test/java/br/gov/caixa/simtr/hub/infrastructure/client/GatewayTest.java`
- `src/test/java/br/gov/caixa/simtr/hub/infrastructure/client/mock/MockFactoryTest.java`
- `src/test/java/br/gov/caixa/simtr/hub/infrastructure/client/RestClientObservabilityFilterTest.java`
- `doc/planejamento-gestao-documento-credencial-container-v1.md`
- `doc/espaco-colaborativo-de-desenvolvimento.md`
- `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`
- `README.md`

Testes criados/ajustados:
- Endpoint HTTP 200 para credencial de container sem body e sem `Content-Type`.
- Resource CDI cobrindo sucesso e falha.
- Service selecionando mock quando simulador esta habilitado e gateway quando desabilitado.
- Gateway encaminhando chamada sem request body e propagando falhas.
- Mock factory lendo `credencial-container.md`.
- Mapper preservando DTO -> VO -> DTO e `validade` flexivel.
- Filtro de observabilidade mascarando `sas` e outros campos sensiveis antes de logar payload.

Comandos executados:
- `mvn -q test`

Resultado dos testes:
- Suite passou.

Decisoes:
- O endpoint externo oficial do Hub e `POST /hub/v1/storage/container/credencial`.
- Gestao de Documentos usa modulo tecnico proprio `gestaodocumento`.
- O REST Client usa `@Path("/gestao-documento")` e URL base `https://api.des.caixa:8443/simtr`, resultando em `/simtr/gestao-documento/v1/storage/container/credencial` no API Manager, sem duplicar `/simtr`.
- `validade` permanece como `Object` no DTO/VO para preservar string ou objeto `Calendar` retornado pelo MTR.
- `sas` e tratado como segredo e nao deve aparecer em logs de aplicacao ou payloads logados pelo REST Client.

Pendencias:
- Adicionar stub/WireMock para respostas `200`, `401`, `403`, `500`, `503` e timeout do `simtr-gestao-documento`.

### 2026-07-10 - Consolidacao MapStruct e Dossie Produto

Feito:

- `ChecklistMapper`, `ProcessoMapper` e `DossieProdutoMapper` consolidados como interfaces MapStruct com CDI.
- Testes de mapper reforcados para round-trip DTO -> VO -> DTO, listas nulas, itens nulos e objetos aninhados nulos.
- Testes de service ajustados para usar mappers injetados pelo CDI.
- Confirmado que `DossieProdutoMapperImpl.java` e `ProcessoMapperImpl.java` sao gerados em `target/generated-sources/annotations`.
- Documentacao principal renomeada para `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`.
- Documentos antigos `doc/cenarios-testes-unitarios-endpoints.md` e `doc/relatorio-ajustes-maquina-dossie-produto.md` removidos por conterem informacoes antigas.

Testes:

```powershell
mvn -q test
```

Resultado:

- Suite passou apos a migracao de `DossieProdutoMapper`.
- Suite passou novamente apos a migracao de `ProcessoMapper`.

Pendencias:

- Migrar endpoints restantes da matriz de endpoints, um por vez.
- Atualizar este historico a cada endpoint implementado.

### 2026-07-10 - Codex - POST documento Dossie Produto v2

Objetivo:
- Implementar `POST /hub/v1/dossie-produto/{id}/documento` como proxy do `POST /simtr-dossie-produto/v2/dossie-produto/{id}/documento`.

Planejamento:
- `doc/planejamento-dossie-produto-documento-v2.md` criado, revisado e aprovado pelo usuario antes da implementacao.

Feito:
- Criados DTOs e VOs especificos para inclusao de documento e resposta v2.
- `DossieProdutoMapper` atualizado para mapear request/response de documento.
- `DossieProdutoClient` ajustado para base versionavel `@Path("/dossie-produto")`, mantendo endpoints v1 e adicionando endpoint v2 no mesmo REST Client.
- `DossieProdutoGateway`, `DossieProdutoService` e `DossieProdutoResource` atualizados com fluxo vertical completo.
- Mock runtime e copia documental adicionados para o endpoint de documento.
- Testes de resource, service, gateway, mock factory e mapper adicionados/ajustados.
- Corrigida validacao local indevida que rejeitava `atributos[].objeto` ausente no payload real de documento.

Arquivos alterados:
- `src/main/java/br/gov/caixa/simtr/hub/api/dossieproduto/DossieProdutoResource.java`
- `src/main/java/br/gov/caixa/simtr/hub/application/dossieproduto/DossieProdutoService.java`
- `src/main/java/br/gov/caixa/simtr/hub/infrastructure/client/dossieproduto/DossieProdutoClient.java`
- `src/main/java/br/gov/caixa/simtr/hub/infrastructure/client/dossieproduto/DossieProdutoGateway.java`
- `src/main/java/br/gov/caixa/simtr/hub/infrastructure/client/dossieproduto/mock/DossieProdutoMockFactory.java`
- `src/main/java/br/gov/caixa/simtr/hub/mapper/dossieproduto/DossieProdutoMapper.java`
- `src/main/java/br/gov/caixa/simtr/hub/api/dto/dossieproduto/*Documento*.java`
- `src/main/java/br/gov/caixa/simtr/hub/domain/dossieproduto/*Documento*.java`
- `src/main/resources/mock/dossieproduto/documento-dossie-produto.md`
- `doc/mock/dossie-produto/documento-dossie-produto.md`
- `src/test/java/br/gov/caixa/simtr/hub/TestFixtures.java`
- `src/test/java/br/gov/caixa/simtr/hub/api/ResourceEndpointTest.java`
- `src/test/java/br/gov/caixa/simtr/hub/api/ResourceBeanCoverageTest.java`
- `src/test/java/br/gov/caixa/simtr/hub/application/dossieproduto/DossieProdutoServiceTest.java`
- `src/test/java/br/gov/caixa/simtr/hub/infrastructure/client/GatewayTest.java`
- `src/test/java/br/gov/caixa/simtr/hub/infrastructure/client/mock/MockFactoryTest.java`
- `src/test/java/br/gov/caixa/simtr/hub/mapper/dossieproduto/DossieProdutoMapperTest.java`
- `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`
- `doc/planejamento-dossie-produto-documento-v2.md`

Testes criados/ajustados:
- Endpoint HTTP 201 para documento e 400 para id invalido.
- Endpoint HTTP 201 para documento com atributos sem `objeto`.
- Service selecionando mock ou gateway conforme simulador.
- Gateway encaminhando id/request e propagando falhas.
- Mock factory lendo `documento-dossie-produto.md`.
- Mapper preservando DTO -> VO -> DTO para documento e resposta v2.

Comandos executados:
- `mvn -q test`

Resultado dos testes:
- Suite passou.

Cobertura:
- Relatorio gerado pelo `quarkus-jacoco` em `target/jacoco-report/index.html`.

Decisoes:
- O endpoint de documento usa DTOs/VOs proprios porque o `vinculo_dossie` difere do contrato de formulario.
- O REST Client de Dossie Produto fica com base versionavel e paths versionados por metodo para suportar v1 e v2 no mesmo servico.
- A integracao com Gestao Documento nao foi embutida neste endpoint; o endpoint apenas vincula o documento ja armazenado.
- Em 2026-07-10, apos teste com payload real, `objeto` em `atributos` e `propriedades` de documento deixou de ser validado como obrigatorio pelo SIMTR Hub, apesar da indicacao do OpenAPI bruto. O Hub deve evitar validacao local mais restritiva que o MTR.
- Se a mensagem `O objeto do atributo do documento deve ser informado.` reaparecer, a primeira suspeita deve ser DTO antigo em execucao ou `@NotNull` reintroduzido em `DossieProdutoDocumentoAtributoDto.objeto`.

Pendencias:
- Implementar os demais endpoints pendentes da matriz em ciclos separados.

### 2026-07-10 - Codex - POST workflow Dossie Produto v1

Objetivo:
- Implementar `POST /hub/v1/dossie-produto/{id}/workflow` como proxy do `POST /simtr-dossie-produto/v1/dossie-produto/{id}/workflow`.

Planejamento:
- `doc/planejamento-dossie-produto-workflow-v1.md` criado, revisado e aprovado pelo usuario antes da implementacao.
- Usuario confirmou a criacao da copia documental do mock em `doc/mock/dossie-produto/workflow-dossie-produto.md`.
- Usuario confirmou retorno `200 OK` com corpo `{ "id": ... }`, seguindo o OpenAPI.

Feito:
- Adicionado endpoint HTTP `POST /hub/v1/dossie-produto/{id}/workflow`.
- Adicionado metodo v1 no `DossieProdutoClient` para chamar `/dossie-produto/v1/dossie-produto/{id}/workflow`.
- `DossieProdutoGateway`, `DossieProdutoService` e `DossieProdutoResource` atualizados com fluxo vertical completo.
- `DossieProdutoMockFactory` atualizado para retornar workflow mockado.
- Mock runtime e copia documental adicionados para workflow.
- Endpoint configurado com `@Consumes(MediaType.WILDCARD)` para aceitar chamada sem corpo e sem `Content-Type`.
- Testes de resource, service, gateway, mock factory e bean coverage adicionados/ajustados.

Arquivos alterados:
- `src/main/java/br/gov/caixa/simtr/hub/api/dossieproduto/DossieProdutoResource.java`
- `src/main/java/br/gov/caixa/simtr/hub/application/dossieproduto/DossieProdutoService.java`
- `src/main/java/br/gov/caixa/simtr/hub/infrastructure/client/dossieproduto/DossieProdutoClient.java`
- `src/main/java/br/gov/caixa/simtr/hub/infrastructure/client/dossieproduto/DossieProdutoGateway.java`
- `src/main/java/br/gov/caixa/simtr/hub/infrastructure/client/dossieproduto/mock/DossieProdutoMockFactory.java`
- `src/main/resources/mock/dossieproduto/workflow-dossie-produto.md`
- `doc/mock/dossie-produto/workflow-dossie-produto.md`
- `src/test/java/br/gov/caixa/simtr/hub/api/ResourceEndpointTest.java`
- `src/test/java/br/gov/caixa/simtr/hub/api/ResourceBeanCoverageTest.java`
- `src/test/java/br/gov/caixa/simtr/hub/application/dossieproduto/DossieProdutoServiceTest.java`
- `src/test/java/br/gov/caixa/simtr/hub/infrastructure/client/GatewayTest.java`
- `src/test/java/br/gov/caixa/simtr/hub/infrastructure/client/mock/MockFactoryTest.java`
- `doc/planejamento-dossie-produto-workflow-v1.md`
- `doc/espaco-colaborativo-de-desenvolvimento.md`
- `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`

Testes criados/ajustados:
- Endpoint HTTP 200 para workflow sem corpo e sem `Content-Type`.
- Endpoint HTTP 400 para `id` invalido.
- Service selecionando mock ou gateway conforme simulador.
- Gateway encaminhando `id` para client e propagando falhas.
- Mock factory lendo `workflow-dossie-produto.md`.
- Bean coverage cobrindo sucesso/falha do resource.

Comandos executados:
- `mvn -q test`

Resultado dos testes:
- Primeira execucao falhou com HTTP 415 no workflow sem `Content-Type`.
- Ajustado `@Consumes(MediaType.WILDCARD)` no metodo de workflow.
- Segunda execucao passou.

Cobertura:
- Relatorio gerado pelo `quarkus-jacoco` em `target/jacoco-report/index.html`.

Decisoes:
- O endpoint de workflow nao cria DTO/VO novo de request porque nao recebe corpo.
- O retorno reutiliza `DossieProdutoCriadoDto` e `DossieProdutoCriadoVo`, pois o OpenAPI define resposta simples `{ "id": ... }`.
- O status de sucesso do Hub e `200 OK`, nao `201 Created`.
- O metodo do resource aceita chamada sem corpo e sem `Content-Type` por ser endpoint sem request body.

Pendencias:
- Implementar os demais endpoints pendentes da matriz em ciclos separados.

### 2026-07-10 - Codex - Ajuste Mockito Java Agent nos testes

Objetivo:
- Resolver os warnings de auto-anexo dinamico do Mockito/Byte Buddy durante `mvn -q test` e preparar a suite para JDKs futuros.

Planejamento:
- `doc/planejamento-ajuste-mockito-java-agent.md` criado, revisado e aprovado pelo usuario antes da implementacao.
- Usuario confirmou seguir com a solucao proposta baseada em configurar Mockito como Java agent no Surefire.

Feito:
- Adicionada propriedade vazia `<argLine></argLine>` no `pom.xml`.
- Adicionado `maven-dependency-plugin` para copiar `org.mockito:mockito-core` resolvido pelo Maven para `target/test-agents/mockito-core.jar`.
- Atualizado `maven-surefire-plugin` para iniciar a JVM de testes com `@{argLine} -javaagent:${project.build.directory}/test-agents/mockito-core.jar`.
- Preservada a configuracao existente de `java.util.logging.manager` no Surefire.
- Documentacao consolidada atualizada com a motivacao e o troubleshooting do ajuste.

Arquivos alterados:
- `pom.xml`
- `doc/planejamento-ajuste-mockito-java-agent.md`
- `doc/espaco-colaborativo-de-desenvolvimento.md`
- `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`

Comandos executados:
- `mvn -q test`

Resultado dos testes:
- Suite passou.
- Os warnings `Mockito is currently self-attaching...`, `A Java agent has been loaded dynamically` e `Dynamic loading of agents will be disallowed...` nao apareceram na execucao validada.
- O aviso `OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended` ainda pode aparecer; ele e diferente do auto-anexo dinamico e pode ocorrer com agente Java carregado no startup.

Cobertura:
- Relatorio gerado pelo `quarkus-jacoco` em `target/jacoco-report/index.html`.

Decisoes:
- A regra "nao adicionar `argLine` no Surefire sem decisao explicita" foi satisfeita pela aprovacao do usuario neste planejamento.
- Nao usar `-XX:+EnableDynamicAgentLoading`, porque isso apenas esconderia parte do aviso e manteria o auto-anexo dinamico.
- Nao hardcodar caminho do jar no repositorio Maven local; o build copia o jar resolvido para `target/test-agents`.

Pendencias:
- Nenhuma pendencia neste ajuste. Se o warning de auto-anexo voltar, revisar `pom.xml`, a copia de `mockito-core.jar` e a configuracao de `argLine` do Surefire.

### 2026-07-10 - Codex - PATCH validacao negocial Dossie Produto v1

Objetivo:
- Implementar `PATCH /hub/v1/dossie-produto/{id}/validacao-negocial` como proxy do `PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/validacao-negocial`.

Planejamento:
- `doc/planejamento-dossie-produto-validacao-negocial-v1.md` criado, revisado e aprovado pelo usuario antes da implementacao.
- Usuario confirmou seguir considerando o OpenAPI como contrato tecnico verdadeiro do MTR, usando `verificacoes` e `respostas_formulario`.

Feito:
- Criados DTOs e VOs especificos para validacao negocial.
- `DossieProdutoMapper` atualizado para mapear request DTO -> VO -> DTO.
- `DossieProdutoClient`, `DossieProdutoGateway`, `DossieProdutoService` e `DossieProdutoResource` atualizados com fluxo vertical completo.
- Endpoint retorna `200 OK` sem corpo e usa `Uni<Void>` nas camadas internas.
- Simulador atualizado com mock Markdown de sucesso sem corpo.
- Testes de resource, service, gateway, mock factory e mapper adicionados/ajustados.

Arquivos alterados:
- `src/main/java/br/gov/caixa/simtr/hub/api/dossieproduto/DossieProdutoResource.java`
- `src/main/java/br/gov/caixa/simtr/hub/application/dossieproduto/DossieProdutoService.java`
- `src/main/java/br/gov/caixa/simtr/hub/infrastructure/client/dossieproduto/DossieProdutoClient.java`
- `src/main/java/br/gov/caixa/simtr/hub/infrastructure/client/dossieproduto/DossieProdutoGateway.java`
- `src/main/java/br/gov/caixa/simtr/hub/infrastructure/client/dossieproduto/mock/DossieProdutoMockFactory.java`
- `src/main/java/br/gov/caixa/simtr/hub/mapper/dossieproduto/DossieProdutoMapper.java`
- `src/main/java/br/gov/caixa/simtr/hub/api/dto/dossieproduto/*ValidacaoNegocial*.java`
- `src/main/java/br/gov/caixa/simtr/hub/domain/dossieproduto/*ValidacaoNegocial*.java`
- `src/main/resources/mock/dossieproduto/validacao-negocial-dossie-produto.md`
- `doc/mock/dossie-produto/validacao-negocial-dossie-produto.md`
- `src/test/java/br/gov/caixa/simtr/hub/TestFixtures.java`
- `src/test/java/br/gov/caixa/simtr/hub/api/ResourceEndpointTest.java`
- `src/test/java/br/gov/caixa/simtr/hub/api/ResourceBeanCoverageTest.java`
- `src/test/java/br/gov/caixa/simtr/hub/application/dossieproduto/DossieProdutoServiceTest.java`
- `src/test/java/br/gov/caixa/simtr/hub/infrastructure/client/GatewayTest.java`
- `src/test/java/br/gov/caixa/simtr/hub/infrastructure/client/mock/MockFactoryTest.java`
- `src/test/java/br/gov/caixa/simtr/hub/mapper/dossieproduto/DossieProdutoMapperTest.java`
- `doc/planejamento-dossie-produto-validacao-negocial-v1.md`
- `doc/espaco-colaborativo-de-desenvolvimento.md`
- `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`

Testes criados/ajustados:
- Endpoint HTTP 200 para validacao negocial com payload canonico do OpenAPI.
- Endpoint HTTP 400 para `id` invalido e corpo ausente.
- Service selecionando mock ou gateway conforme simulador.
- Gateway encaminhando `id` e request ao client e propagando falhas.
- Mock factory lendo `validacao-negocial-dossie-produto.md`.
- Mapper preservando DTO -> VO -> DTO para verificacoes, pareceres, garantia, produto e respostas de formulario.

Comandos executados:
- `mvn -q test`

Resultado dos testes:
- Suite passou.

Cobertura:
- Relatorio gerado pelo `quarkus-jacoco` em `target/jacoco-report/index.html`.

Decisoes:
- O OpenAPI e a fonte de verdade do contrato tecnico implementado no MTR para este endpoint.
- O Hub modela e encaminha `verificacoes` e `respostas_formulario`.
- Os nomes do exemplo funcional divergente, `verificacoes_realizadas` e `resposta_formulario`, nao foram modelados.
- O campo `identificador_negocial`, presente apenas no exemplo funcional, nao foi modelado nem encaminhado ao MTR.
- A resposta de sucesso e `200 OK` sem corpo.
- Validacoes locais foram mantidas cautelosas: path/body obrigatorios e campos required do OpenAPI nos objetos informados, exceto `previo`, sem validar enum de `resultado` nem range de `indice_ia`.

Pendencias:
- Nenhuma pendencia neste endpoint.

### 2026-07-10 - Codex - Ajuste path OpenAPI

Objetivo:
- Alterar o path do documento OpenAPI gerado pelo Quarkus de `/hub/openai` para `/hub/openapi`.

Feito:
- Atualizada a configuracao `quarkus.smallrye-openapi.path` e `%dev.quarkus.smallrye-openapi.path`.
- Atualizadas a documentacao principal, o README e a copia documental de properties.
- Adicionado teste HTTP para validar `GET /hub/openapi`.

Arquivos alterados:
- `src/main/resources/application.properties`
- `doc/properties/application.properties`
- `README.md`
- `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`
- `src/test/java/br/gov/caixa/simtr/hub/api/ResourceEndpointTest.java`
- `doc/espaco-colaborativo-de-desenvolvimento.md`

Testes criados/ajustados:
- `ResourceEndpointTest.openApiEndpointRetorna200NoPathConvencional`.

Comandos executados:
- `mvn -q test`

Resultado dos testes:
- Suite passou.

Cobertura:
- Relatorio gerado pelo `quarkus-jacoco` em `target/jacoco-report/index.html`.

Decisoes:
- O path oficial do OpenAPI do SIMTR Hub e `/hub/openapi`.
- O path antigo `/hub/openai` deixa de ser documentado como endpoint valido.

Pendencias:
- Nenhuma.

### 2026-07-10 - Codex - Correcao validacao local `previo`

Objetivo:
- Permitir payload de validacao negocial sem `previo` em cada verificacao, pois o MTR implementado aceita esse campo como opcional.

Feito:
- Removida a obrigatoriedade local de `previo` em `DossieProdutoValidacaoNegocialVerificacaoDto`.
- Adicionado teste HTTP com JSON sem `previo` para `PATCH /hub/v1/dossie-produto/{id}/validacao-negocial`.
- Ajustado teste de mapper para preservar `previo` nulo.
- Atualizados planejamento e documentacao consolidada com a decisao.

Arquivos alterados:
- `src/main/java/br/gov/caixa/simtr/hub/api/dto/dossieproduto/DossieProdutoValidacaoNegocialVerificacaoDto.java`
- `src/test/java/br/gov/caixa/simtr/hub/api/ResourceEndpointTest.java`
- `src/test/java/br/gov/caixa/simtr/hub/mapper/dossieproduto/DossieProdutoMapperTest.java`
- `doc/planejamento-dossie-produto-validacao-negocial-v1.md`
- `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`
- `doc/espaco-colaborativo-de-desenvolvimento.md`

Testes criados/ajustados:
- `ResourceEndpointTest.dossieProdutoPatchValidacaoNegocialAceitaVerificacaoSemPrevio`.
- `DossieProdutoMapperTest.devePreservarObjetosAninhadosNulos`.

Comandos executados:
- mvn -q test

Resultado dos testes:
- Suite passou.

Cobertura:
- Teste HTTP cobre a ausencia real do campo `previo` na entrada.
- Teste de mapper cobre preservacao de `previo` nulo no ciclo DTO -> VO -> DTO.

Decisoes:
- `previo` permanece no contrato para ser encaminhado quando informado.
- Ausencia de `previo` nao deve gerar `400` no Hub.

Pendencias:
- Nenhuma.

### 2026-07-10 - Codex - Reorganizacao por dominio SIMTR Hub

Objetivo:
- Reorganizar o projeto sob `br.gov.caixa.simtr.hub` por dominio de negocio, remover os pacotes globais por layer e preparar cada dominio para futura extracao como microservico.

Planejamento:
- `doc/planejamento-reorganizacao-packages-dominio-v1.md` revisado e aprovado pelo usuario.
- Usuario aprovou estrutura por dominio, fachadas por dominio, identidade tecnica `simtr-hub` e mudanca dos paths externos para `/hub/...`.

Feito:
- Migrada a identidade tecnica para `simtr-hub` em `pom.xml`, `quarkus.application.name`, propriedades, spans/eventos e logs.
- Ajustado o arquivo de log local para `target/logs/simtr-hub.json`.
- Verticalizados os dominios `parametrizacao`, `dossieproduto` e `gestaodocumento`.
- Criadas `ParametrizacaoFachada`, `DossieProdutoFachada` e `GestaoDocumentoFachada`.
- Resources passaram a depender das fachadas, nao diretamente dos services.
- DTOs REST ficaram em `recurso/rest/v1/dto`, VOs em `dominio`, services em `servico`, clients/gateways/mocks em `integracao` e mappers em `mapeamento`.
- Classes compartilhadas foram consolidadas em `arquitetura.excecao`, `arquitetura.observabilidade`, `arquitetura.seguranca` e `arquitetura.configuracao.mock`.
- Testes foram movidos para pacotes coerentes com a nova organizacao.

Arquivos/pacotes principais alterados:
- `src/main/java/br/gov/caixa/simtr/hub/parametrizacao`
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto`
- `src/main/java/br/gov/caixa/simtr/hub/gestaodocumento`
- `src/main/java/br/gov/caixa/simtr/hub/arquitetura`
- `src/test/java/br/gov/caixa/simtr/hub`
- `pom.xml`
- `src/main/resources/application.properties`
- `src/test/resources/application.properties`
- `README.md`
- `doc/planejamento-reorganizacao-packages-dominio-v1.md`
- `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`

Comandos executados:
- `mvn -q test`
- `rg -n "br\\.gov\\.caixa\\.simtr\\.hub\\.(api|application|domain|infrastructure|mapper|shared)" src/main/java src/test/java`
- `rg -n "<padroes-legados-de-identidade>" pom.xml README.md doc src`

Resultado dos testes:
- Suite passou apos a migracao de identidade.
- Suite passou apos a verticalizacao de `parametrizacao`.
- Suite passou apos a verticalizacao de `gestaodocumento`.
- Suite passou apos a verticalizacao de `dossieproduto`.
- Suite passou apos a consolidacao de `arquitetura`.

Decisoes:
- Pacotes vazios nao foram criados apenas para reservar estrutura.
- `arquitetura` fica restrita a codigo transversal e nao deve importar dominios.
- Fachadas sao a fronteira interna oficial dos dominios para futuras extracoes.
- Os endpoints externos oficiais ficam sob `/hub/...`.

Pendencias:
- Os planejamentos historicos de endpoints anteriores ainda podem citar caminhos antigos porque registram o contexto da epoca. Para novo desenvolvimento, usar os caminhos deste registro e da secao de arquivos de leitura atualizada.

### Template para proximos registros

```text
### AAAA-MM-DD - <agente> - <endpoint ou incremento>

Objetivo:
- 

Feito:
- 

Arquivos alterados:
- 

Testes criados/ajustados:
- 

Comandos executados:
- mvn -q test

Resultado dos testes:
- 

Cobertura:
- 

Decisoes:
- 

Pendencias:
- 
```

## Arquivos para ler no inicio de cada sessao

Sempre ler primeiro:

```text
doc/espaco-colaborativo-de-desenvolvimento.md
doc/api-integracao-mtr-pre-validacao-v1.md
doc/documentacao-simtr-hub-arquitetura-observabilidade.md
pom.xml
src/main/resources/application.properties
src/test/resources/application.properties
```

Para Dossie Produto, ler tambem:

```text
doc/swagger-mtr/simtr-dossie-produto-openapi- 2.20.0.8
src/main/java/br/gov/caixa/simtr/hub/dossieproduto/fachada/DossieProdutoFachada.java
src/main/java/br/gov/caixa/simtr/hub/dossieproduto/recurso/rest/v1/DossieProdutoResource.java
src/main/java/br/gov/caixa/simtr/hub/dossieproduto/servico/DossieProdutoService.java
src/main/java/br/gov/caixa/simtr/hub/dossieproduto/integracao/DossieProdutoClient.java
src/main/java/br/gov/caixa/simtr/hub/dossieproduto/integracao/DossieProdutoGateway.java
src/main/java/br/gov/caixa/simtr/hub/dossieproduto/integracao/DossieProdutoClientExceptionMapper.java
src/main/java/br/gov/caixa/simtr/hub/dossieproduto/integracao/mock/DossieProdutoMockFactory.java
src/main/java/br/gov/caixa/simtr/hub/dossieproduto/mapeamento/DossieProdutoMapper.java
src/test/java/br/gov/caixa/simtr/hub/dossieproduto/mapeamento/DossieProdutoMapperTest.java
src/test/java/br/gov/caixa/simtr/hub/dossieproduto/servico/DossieProdutoServiceTest.java
src/test/java/br/gov/caixa/simtr/hub/recurso/ResourceEndpointTest.java
src/test/java/br/gov/caixa/simtr/hub/recurso/ResourceBeanCoverageTest.java
```

Para Parametrizacao, ler tambem:

```text
src/main/java/br/gov/caixa/simtr/hub/parametrizacao/fachada/ParametrizacaoFachada.java
src/main/java/br/gov/caixa/simtr/hub/parametrizacao/recurso/rest/v1/ProcessoResource.java
src/main/java/br/gov/caixa/simtr/hub/parametrizacao/recurso/rest/v1/ChecklistResource.java
src/main/java/br/gov/caixa/simtr/hub/parametrizacao/servico/ProcessoService.java
src/main/java/br/gov/caixa/simtr/hub/parametrizacao/servico/ChecklistService.java
src/main/java/br/gov/caixa/simtr/hub/parametrizacao/integracao
src/main/java/br/gov/caixa/simtr/hub/parametrizacao/mapeamento
src/test/java/br/gov/caixa/simtr/hub/parametrizacao
```

Para Gestao de Documentos, ler tambem:

```text
doc/swagger-mtr/simtr-gestao-documento-openapi-2.23.1.0
doc/planejamento-gestao-documento-credencial-container-v1.md
src/main/java/br/gov/caixa/simtr/hub/gestaodocumento/fachada/GestaoDocumentoFachada.java
src/main/java/br/gov/caixa/simtr/hub/gestaodocumento/recurso/rest/v1/GestaoDocumentoResource.java
src/main/java/br/gov/caixa/simtr/hub/gestaodocumento/servico/GestaoDocumentoService.java
src/main/java/br/gov/caixa/simtr/hub/gestaodocumento/integracao/GestaoDocumentoClient.java
src/main/java/br/gov/caixa/simtr/hub/gestaodocumento/integracao/GestaoDocumentoGateway.java
src/main/java/br/gov/caixa/simtr/hub/gestaodocumento/integracao/GestaoDocumentoClientExceptionMapper.java
src/main/java/br/gov/caixa/simtr/hub/gestaodocumento/integracao/mock/GestaoDocumentoMockFactory.java
src/main/java/br/gov/caixa/simtr/hub/gestaodocumento/mapeamento/GestaoDocumentoMapper.java
```

## Regras de continuidade

- Nao reverter alteracoes nao relacionadas.
- Respeitar o que ja foi desenvolvido.
- Manter Quarkus + CDI.
- Manter MapStruct para mappers.
- Manter DTO -> VO -> DTO.
- Nao criar novo REST client de Dossie Produto se o endpoint pertence ao mesmo servico.
- Nao criar novo resource de Dossie Produto sem necessidade real.
- Para Gestao de Documentos, usar modulo tecnico proprio e o OpenAPI `simtr-gestao-documento-openapi-2.23.1.0`.
- Nao duplicar `/simtr` no REST Client.
- Nao trocar mocks Markdown por implementacao hardcoded sem decisao explicita.
- Nao reintroduzir `jacoco-maven-plugin` sem decisao explicita.
- Nao adicionar `argLine` no Surefire sem decisao explicita.
- Depois de cada endpoint ou migracao relevante, rodar `mvn -q test`.
- Depois de cada rodada, atualizar tambem `doc/documentacao-simtr-hub-arquitetura-observabilidade.md` com as decisoes consolidadas.

## Warnings conhecidos

O warning abaixo foi tratado em 2026-07-10 configurando Mockito como Java agent no Surefire:

```text
Mockito is currently self-attaching...
```

Ele nao esta relacionado a encoding, MapStruct ou JaCoCo. Se voltar a aparecer, verificar:

- `pom.xml`, principalmente `maven-dependency-plugin` copiando `mockito-core.jar`;
- `maven-surefire-plugin` com `@{argLine} -javaagent:${project.build.directory}/test-agents/mockito-core.jar`;
- existencia de `target/test-agents/mockito-core.jar` apos a fase `process-test-classes`.

O aviso abaixo ainda pode aparecer com agente Java carregado no startup e nao indica auto-anexo dinamico do Mockito:

```text
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```

Se texto com acento aparecer quebrado no console, validar primeiro o arquivo:

```text
target/logs/simtr-hub.json
```

Nao alterar encoding do build sem evidencia no arquivo JSON.
