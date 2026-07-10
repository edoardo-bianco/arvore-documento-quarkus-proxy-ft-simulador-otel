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
GET /arvore-documento/v1/processo/identificador-negocial/{identificador}
GET /arvore-documento/v1/checklist/identificador-negocial/{identificador}/versao/{versao}
POST /arvore-documento/v1/dossie-produto
PATCH /arvore-documento/v1/dossie-produto/{id}/formulario
```

Esses endpoints cobrem:

- consulta da parametrizacao do processo;
- consulta de checklist por identificador e versao;
- criacao basica de Dossie Produto em modo rascunho;
- inclusao ou edicao de respostas de formulario no Dossie Produto.

Tambem foi consolidada a migracao dos mappers para MapStruct:

```text
src/main/java/br/gov/caixa/simtr/arvoredocumento/mapper/parametrizacao/ChecklistMapper.java
src/main/java/br/gov/caixa/simtr/arvoredocumento/mapper/parametrizacao/ProcessoMapper.java
src/main/java/br/gov/caixa/simtr/arvoredocumento/mapper/dossieproduto/DossieProdutoMapper.java
```

Implementacoes MapStruct geradas e validadas:

```text
target/generated-sources/annotations/.../ChecklistMapperImpl.java
target/generated-sources/annotations/.../ProcessoMapperImpl.java
target/generated-sources/annotations/.../DossieProdutoMapperImpl.java
```

Validacao executada:

```powershell
mvn -q test
```

Resultado: suite passou apos a migracao de `DossieProdutoMapper` e passou novamente apos a migracao de `ProcessoMapper`.

## Matriz de endpoints da solucao

| Modulo | Endpoint MTR | Endpoint SIMTR Hub | Status | Observacao |
| --- | --- | --- | --- | --- |
| Parametrizacao | `GET /simtr-parametrizacao/v2/patriarca/processo/identificador-negocial/{identificador}` | `GET /arvore-documento/v1/processo/identificador-negocial/{identificador}` | Implementado | Preservar fluxo atual. |
| Parametrizacao | `GET /simtr-parametrizacao/v1/cadastro/checklist/identificador-negocial/{identificador}/versao/{versao}` | `GET /arvore-documento/v1/checklist/identificador-negocial/{identificador}/versao/{versao}` | Implementado | Preservar fluxo atual. |
| Dossie Produto | `POST /simtr-dossie-produto/v1/dossie-produto` | `POST /arvore-documento/v1/dossie-produto` | Implementado | Criacao basica em modo rascunho. |
| Dossie Produto | `PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/formulario` | `PATCH /arvore-documento/v1/dossie-produto/{id}/formulario` | Implementado | Ja existe no codigo; deve ser preservado. |
| Dossie Produto | `PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/garantia` | `PATCH /arvore-documento/v1/dossie-produto/{id}/garantia` | Pendente | Proximo endpoint candidato. |
| Dossie Produto | `PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/produto` | `PATCH /arvore-documento/v1/dossie-produto/{id}/produto` | Pendente | Implementar em passo separado. |
| Dossie Produto | `POST /simtr-dossie-produto/v1/dossie-produto/{id}/capturar` | `POST /arvore-documento/v1/dossie-produto/{id}/capturar` | Pendente | Implementar em passo separado. |
| Dossie Produto | `POST /simtr-dossie-produto/v1/dossie-produto/{id}/workflow` | `POST /arvore-documento/v1/dossie-produto/{id}/workflow` | Pendente | Implementar em passo separado. |
| Dossie Produto | `POST /simtr-dossie-produto/v1/dossie-produto/{id}/cancelar` | `POST /arvore-documento/v1/dossie-produto/{id}/cancelar` | Pendente | Implementar em passo separado. |
| Dossie Produto | `GET /simtr-dossie-produto/v2/dossie-produto/{id}` | `GET /arvore-documento/v1/dossie-produto/{id}` | Pendente | Confirmar contrato externo do hub antes de implementar. |
| Dossie Produto | `POST /simtr-dossie-produto/v2/dossie-produto/{id}/documento` | `POST /arvore-documento/v1/dossie-produto/{id}/documento` | Pendente | Confirmar integracao com Gestao de Documentos e payload. |
| Dossie Produto | `PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/validacao-negocial` | `PATCH /arvore-documento/v1/dossie-produto/{id}/validacao-negocial` | Pendente | Ler secao funcional antes de modelar DTOs. |
| Gestao de Documentos | `POST /simtr-gestao-documento/v1/storage/container/credencial` | A definir | Pendente | Definir resource/client mantendo padrao do projeto. |

## Fontes obrigatorias para novos endpoints

Antes de implementar qualquer endpoint pendente, ler as duas fontes abaixo na secao exata do endpoint:

1. Documentacao funcional e de integracao:

```text
doc/api-integracao-mtr-pre-validacao-v1.md
```

Usar esse arquivo para entender:

- intencao de negocio;
- momento do fluxo de pre-validacao;
- comportamento esperado;
- relacao com workflow, documentos, validacao negocial e Dossie Produto.

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

Se a documentacao funcional e o OpenAPI parecerem divergentes:

- usar `api-integracao-mtr-pre-validacao-v1.md` para entender o comportamento de negocio;
- usar o OpenAPI para modelar contrato tecnico, DTOs e status codes;
- preservar a convencao de roteamento ja implementada no API Manager;
- registrar a decisao neste documento antes de seguir para o proximo endpoint.

## Decisoes arquiteturais consolidadas

### Nomenclatura e package root futuro

O nome funcional consolidado da solucao e `SIMTR Hub`.

Ainda existem identificadores tecnicos legados com `arvore-documento`, incluindo paths externos atuais, propriedades, nomes de span/log e o package root Java:

```text
br.gov.caixa.simtr.arvoredocumento
```

A estrutura de packages devera ser alterada no futuro para um root coerente com `simtr-hub`, mas essa migracao sera feita no seu tempo, em passo separado, com regressao completa e sem misturar com a migracao dos endpoints restantes.

Enquanto essa migracao nao ocorrer:

- preservar os packages atuais;
- nao renomear classes, packages ou paths tecnicos incidentalmente;
- registrar novas decisoes usando o nome funcional `SIMTR Hub`;
- manter compatibilidade com os endpoints ja implementados.

### Camadas

Manter a separacao atual:

```text
api
  -> application
      -> infrastructure/client ou mock
  -> mapper DTO/VO
domain
```

Para Dossie Produto, o fluxo padrao e:

```text
DossieProdutoResource
  -> DossieProdutoMapper.toVo(...)
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
src/main/java/br/gov/caixa/simtr/arvoredocumento/api/dossieproduto/DossieProdutoResource.java
```

Path externo base do proxy:

```java
@Path("/arvore-documento/v1/dossie-produto")
```

Nao criar um resource separado por endpoint sem decisao explicita.

Manter o REST Client existente:

```text
src/main/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/dossieproduto/DossieProdutoClient.java
```

Configuracao atual:

```java
@RegisterRestClient(configKey = "dossie-produto")
@Path("/dossie-produto/v1/dossie-produto")
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
arvore-documento.simulador.dossie-produto.habilitado=false
%dev.arvore-documento.simulador.dossie-produto.habilitado=true
```

Quando `true`, o service usa `DossieProdutoMockFactory` e nao chama o REST Client real.

Quando `false`, o service usa `DossieProdutoGateway` e chama `DossieProdutoClient`.

Mocks devem permanecer em:

```text
src/main/resources/mock/dossieproduto
```

Nao trocar mocks Markdown por implementacao hardcoded sem decisao explicita.

## Rotina obrigatoria para o proximo agente

Para cada endpoint ou incremento funcional:

1. Ler este documento antes de editar codigo.
2. Escolher apenas um endpoint ou incremento por ciclo de trabalho.
3. Ler `doc/api-integracao-mtr-pre-validacao-v1.md` na secao exata do endpoint.
4. Ler `doc/swagger-mtr/simtr-dossie-produto-openapi- 2.20.0.8` no path exato do endpoint quando for Dossie Produto.
5. Conferir a implementacao existente antes de criar qualquer classe nova.
6. Implementar o fluxo vertical completo, respeitando o padrao ja desenvolvido.
7. Criar ou ajustar testes unitarios para o novo desenvolvimento.
8. Manter cobertura minima de 80% de linhas para o novo desenvolvimento e nao reduzir a cobertura global abaixo da meta vigente.
9. Rodar a suite a cada implementacao:

```powershell
mvn -q test
```

10. Se a suite falhar, corrigir antes de iniciar outro endpoint.
11. Atualizar este documento com:

```text
data
endpoint
objetivo
arquivos alterados
testes criados/ajustados
resultado dos testes
cobertura quando aplicavel
decisoes tomadas
pendencias restantes
```

12. Atualizar `doc/documentacao-simtr-hub-arquitetura-observabilidade.md` sempre que houver decisao consolidada ou mudanca de padrao que outras pessoas precisem conhecer.

Nao deixar implementacao sem teste, sem atualizacao deste espaco colaborativo e sem consolidar na documentacao principal as decisoes que deixaram de ser apenas contexto de trabalho.

## Padrao para adicionar novo endpoint de Dossie Produto

Para cada novo endpoint:

1. Criar DTOs em:

```text
src/main/java/br/gov/caixa/simtr/arvoredocumento/api/dto/dossieproduto
```

2. Criar VOs equivalentes em:

```text
src/main/java/br/gov/caixa/simtr/arvoredocumento/domain/dossieproduto
```

3. Adicionar metodos no `DossieProdutoMapper`, mantendo interface MapStruct e CDI.
4. Adicionar metodo no `DossieProdutoClient` com:

```text
metodo HTTP correto
@Path correto relativo ao @Path base do client
@PathParam e/ou @QueryParam quando aplicavel
@Timeout
@Retry
@CircuitBreaker
mesmo @ClientExceptionMapper ja existente
```

5. Adicionar metodo no `DossieProdutoGateway`, mantendo logs de dependencia, span e propagacao de erro.
6. Adicionar metodo no `DossieProdutoMockFactory`.
7. Criar mock em `src/main/resources/mock/dossieproduto`.
8. Adicionar metodo no `DossieProdutoService`, mantendo decisao simulador vs MTR.
9. Adicionar endpoint no `DossieProdutoResource`.
10. Adicionar ou ajustar testes.
11. Rodar `mvn -q test`.
12. Atualizar este documento.

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
quarkus.jacoco.report=true
quarkus.jacoco.report-location=target/jacoco-report
quarkus.jacoco.title=arvore-documento
```

Meta vigente:

```text
LINE coverage >= 80%
```

Nao reintroduzir `jacoco-maven-plugin` sem decisao explicita.

## Historico de trabalho dos agentes

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
src/main/java/br/gov/caixa/simtr/arvoredocumento/api/dossieproduto/DossieProdutoResource.java
src/main/java/br/gov/caixa/simtr/arvoredocumento/application/dossieproduto/DossieProdutoService.java
src/main/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/dossieproduto/DossieProdutoClient.java
src/main/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/dossieproduto/DossieProdutoGateway.java
src/main/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/dossieproduto/DossieProdutoClientExceptionMapper.java
src/main/java/br/gov/caixa/simtr/arvoredocumento/infrastructure/client/dossieproduto/mock/DossieProdutoMockFactory.java
src/main/java/br/gov/caixa/simtr/arvoredocumento/mapper/dossieproduto/DossieProdutoMapper.java
src/test/java/br/gov/caixa/simtr/arvoredocumento/mapper/dossieproduto/DossieProdutoMapperTest.java
src/test/java/br/gov/caixa/simtr/arvoredocumento/application/dossieproduto/DossieProdutoServiceTest.java
src/test/java/br/gov/caixa/simtr/arvoredocumento/api/ResourceEndpointTest.java
src/test/java/br/gov/caixa/simtr/arvoredocumento/api/ResourceBeanCoverageTest.java
```

## Regras de continuidade

- Nao reverter alteracoes nao relacionadas.
- Respeitar o que ja foi desenvolvido.
- Manter Quarkus + CDI.
- Manter MapStruct para mappers.
- Manter DTO -> VO -> DTO.
- Nao criar novo REST client de Dossie Produto se o endpoint pertence ao mesmo servico.
- Nao criar novo resource de Dossie Produto sem necessidade real.
- Nao duplicar `/simtr` no REST Client.
- Nao trocar mocks Markdown por implementacao hardcoded sem decisao explicita.
- Nao reintroduzir `jacoco-maven-plugin` sem decisao explicita.
- Nao adicionar `argLine` no Surefire sem decisao explicita.
- Depois de cada endpoint ou migracao relevante, rodar `mvn -q test`.
- Depois de cada rodada, atualizar tambem `doc/documentacao-simtr-hub-arquitetura-observabilidade.md` com as decisoes consolidadas.

## Warnings conhecidos

O warning abaixo pode aparecer durante testes:

```text
Mockito is currently self-attaching...
```

Ele nao esta relacionado a encoding, MapStruct ou JaCoCo.

Se texto com acento aparecer quebrado no console, validar primeiro o arquivo:

```text
target/logs/arvore-documento.json
```

Nao alterar encoding do build sem evidencia no arquivo JSON.
