# Planejamento - Reorganizacao por dominio e package root SIMTR Hub

Data: 2026-07-10

Status: revisado, aprovado e implementado em 2026-07-10, com validacao incremental por checkpoint.

## Objetivo

Reorganizar o projeto de uma estrutura por camada tecnica para uma estrutura por dominio de negocio, usando o root Java:

```text
br.gov.caixa.simtr.hub
```

A estrutura alvo deve aproximar cada dominio de um futuro microservico independente, mantendo no pacote `arquitetura` apenas o que for compartilhado, transversal ou tecnico.

Dominios de negocio alvo:

- `parametrizacao`
- `dossieproduto`
- `gestaodocumento`

Pacote transversal alvo:

- `arquitetura`

## Resultado implementado

Em 2026-07-10, a reorganizacao foi executada nos seguintes checkpoints:

1. Baseline com `mvn -q test` antes das movimentacoes.
2. Migracao de identidade tecnica para `simtr-hub` em `pom.xml`, `quarkus.application.name`, propriedades `simtr-hub.*`, spans/eventos e path externo `/hub/...`.
3. Verticalizacao de `parametrizacao` com `ParametrizacaoFachada`.
4. Verticalizacao de `gestaodocumento` com `GestaoDocumentoFachada`.
5. Verticalizacao de `dossieproduto` com `DossieProdutoFachada`.
6. Consolidacao de compartilhados em `arquitetura.excecao`, `arquitetura.observabilidade`, `arquitetura.seguranca` e `arquitetura.configuracao.mock`.
7. Ajuste do arquivo de log para `target/logs/simtr-hub.json`.

Validacoes executadas:

```powershell
mvn -q test
rg -n "br\.gov\.caixa\.simtr\.hub\.(api|application|domain|infrastructure|mapper|shared)" src/main/java src/test/java
rg -n "<padroes-legados-de-identidade>" pom.xml README.md doc src
```

Resultado:

- `mvn -q test` passou em todos os checkpoints.
- A varredura Java nao encontrou imports ou packages sob os pacotes antigos de layer.
- A varredura de identidade antiga nao encontrou referencias aos padroes legados nos arquivos principais.

## Contexto original

Antes desta migracao, o projeto usava identidade tecnica e package root ligados ao nome anterior e estava organizado principalmente por camada:

```text
api
application
domain
infrastructure
mapper
shared
```

Esse desenho funcionou para consolidar os primeiros endpoints, mas dificultava a extracao futura de cada modulo como microservico porque as classes de um mesmo dominio ficavam espalhadas em pacotes de camada.

O documento colaborativo registrava que a migracao de root/package deveria ocorrer em passo separado, com regressao completa e sem misturar com a migracao dos endpoints restantes. Aprovado o plano, a migracao foi executada nesta rodada.

## Fontes consultadas

- `doc/espaco-colaborativo-de-desenvolvimento.md`
- `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`
- `README.md`
- `pom.xml`
- `src/main/resources/application.properties`
- `src/test/resources/application.properties`
- Inventario de arquivos em `src/main/java/br/gov/caixa/simtr/hub`
- Inventario de testes em `src/test/java/br/gov/caixa/simtr/hub`
- Implementacao atual de `DossieProdutoService`, que mostra que services ainda misturam VO, DTO, mapper, mock e gateway em alguns pontos

## Estrutura alvo

Estrutura conceitual desejada:

```text
br.gov.caixa.simtr.hub
|-- parametrizacao
|   |-- fachada
|   |-- recurso
|   |   `-- rest
|   |       `-- v1
|   |-- servico
|   |-- dominio
|   |-- repositorio
|   |-- integracao
|   |-- mapeamento
|   `-- excecao
|-- dossieproduto
|   |-- fachada
|   |-- recurso
|   |   `-- rest
|   |       `-- v1
|   |-- servico
|   |-- dominio
|   |-- repositorio
|   |-- integracao
|   |-- mapeamento
|   `-- excecao
|-- gestaodocumento
|   |-- fachada
|   |-- recurso
|   |   `-- rest
|   |       `-- v1
|   |-- servico
|   |-- dominio
|   |-- repositorio
|   |-- integracao
|   |-- mapeamento
|   `-- excecao
`-- arquitetura
    |-- configuracao
    |-- seguranca
    |-- observabilidade
    |-- resiliencia
    |-- mensageria
    `-- excecao
```

Observacao: pacotes sem classes reais nao devem ser criados apenas para reservar lugar, porque o Git nao versiona diretorios vazios. O plano deve registrar os pacotes alvo, mas a implementacao deve criar apenas diretorios que contenham classes, `package-info.java` aprovado ou arquivos reais.

## Papel de cada pacote

`fachada`:
- porta interna do dominio para casos de uso expostos ao mundo externo ou a outros dominios;
- recurso REST chama fachada, nao service diretamente;
- ajuda a substituir futuramente uma chamada local por chamada HTTP/mensageria quando o dominio virar microservico.

`recurso.rest.v1`:
- resources JAX-RS e DTOs do contrato REST versionado do Hub;
- DTOs devem ficar perto do recurso e da versao do contrato, nao no pacote transversal.

`servico`:
- regras de aplicacao do dominio;
- decisao entre simulador e integracao real quando essa decisao ainda pertencer ao dominio;
- orquestracao de VOs e chamadas de integracao.

`dominio`:
- VOs, objetos de valor e modelos internos do dominio;
- nao deve depender de resource, DTO REST, client REST ou Quarkus quando evitavel.

`repositorio`:
- persistencia do dominio, quando existir;
- hoje nao ha classes de repositorio e o pacote deve permanecer ausente ate haver persistencia real.

`integracao`:
- REST Clients, gateways, exception mappers de client e mock factories especificos do dominio;
- subpacotes podem ser usados quando ajudarem a separar `mtr`, `mock` ou outro provedor.

`mapeamento`:
- MapStruct mappers do dominio;
- mapeia DTO REST, DTO de integracao e VO do dominio.

`excecao`:
- excecoes especificas do dominio, quando existirem;
- excecoes genericas, padrao de erro e exception mappers globais ficam em `arquitetura.excecao`.

`arquitetura`:
- configuracao, seguranca, observabilidade, resiliencia, mensageria e excecoes compartilhadas;
- nao deve receber classes que pertencam a um unico dominio.

## Mapeamento proposto

### Root e identidade tecnica

Root final:

```text
br.gov.caixa.simtr.hub
```

Itens aplicados na migracao:

- `quarkus.application.name=simtr-hub`
- `pom.xml` com `artifactId` e `name` iguais a `simtr-hub`
- paths externos `/hub/...`
- propriedades `simtr-hub.*`
- spans, eventos e atributos com prefixo `simtr-hub` ou `simtr_hub`
- arquivo de log `target/logs/simtr-hub.json`
- README e documentacao consolidada.

Decisao aplicada:

1. Migrar package root, nome da aplicacao, artifactId/name, propriedades internas, logs e spans para `hub`/`simtr-hub`.
2. Alterar paths externos para `/hub/...` nesta migracao, conforme aprovacao explicita do usuario.

### Parametrizacao

Atual:

```text
api.parametrizacao.ProcessoResource
api.parametrizacao.ChecklistResource
api.dto.parametrizacao.processo.*
api.dto.parametrizacao.checklist.*
application.parametrizacao.ProcessoService
application.parametrizacao.ChecklistService
domain.parametrizacao.processo.*
domain.parametrizacao.checklist.*
infrastructure.client.parametrizacao.*
infrastructure.client.parametrizacao.mock.*
mapper.parametrizacao.*
```

Alvo:

```text
hub.parametrizacao.fachada.ParametrizacaoFachada
hub.parametrizacao.recurso.rest.v1.ProcessoResource
hub.parametrizacao.recurso.rest.v1.ChecklistResource
hub.parametrizacao.recurso.rest.v1.dto.processo.*
hub.parametrizacao.recurso.rest.v1.dto.checklist.*
hub.parametrizacao.servico.ProcessoService
hub.parametrizacao.servico.ChecklistService
hub.parametrizacao.dominio.processo.*
hub.parametrizacao.dominio.checklist.*
hub.parametrizacao.integracao.ParametrizacaoProcessoClient
hub.parametrizacao.integracao.ParametrizacaoChecklistClient
hub.parametrizacao.integracao.ParametrizacaoProcessoGateway
hub.parametrizacao.integracao.ParametrizacaoChecklistGateway
hub.parametrizacao.integracao.ParametrizacaoClientExceptionMapper
hub.parametrizacao.integracao.mock.ProcessoMockFactory
hub.parametrizacao.integracao.mock.ChecklistMockFactory
hub.parametrizacao.mapeamento.ProcessoMapper
hub.parametrizacao.mapeamento.ChecklistMapper
```

### Dossie Produto

Atual:

```text
api.dossieproduto.DossieProdutoResource
api.dto.dossieproduto.*
application.dossieproduto.DossieProdutoService
domain.dossieproduto.*
infrastructure.client.dossieproduto.*
infrastructure.client.dossieproduto.mock.*
mapper.dossieproduto.DossieProdutoMapper
```

Alvo:

```text
hub.dossieproduto.fachada.DossieProdutoFachada
hub.dossieproduto.recurso.rest.v1.DossieProdutoResource
hub.dossieproduto.recurso.rest.v1.dto.*
hub.dossieproduto.servico.DossieProdutoService
hub.dossieproduto.dominio.*
hub.dossieproduto.integracao.DossieProdutoClient
hub.dossieproduto.integracao.DossieProdutoGateway
hub.dossieproduto.integracao.DossieProdutoClientExceptionMapper
hub.dossieproduto.integracao.mock.DossieProdutoMockFactory
hub.dossieproduto.mapeamento.DossieProdutoMapper
```

### Gestao Documento

Atual:

```text
api.gestaodocumento.GestaoDocumentoResource
api.dto.gestaodocumento.*
application.gestaodocumento.GestaoDocumentoService
domain.gestaodocumento.*
infrastructure.client.gestaodocumento.*
infrastructure.client.gestaodocumento.mock.*
mapper.gestaodocumento.GestaoDocumentoMapper
```

Alvo:

```text
hub.gestaodocumento.fachada.GestaoDocumentoFachada
hub.gestaodocumento.recurso.rest.v1.GestaoDocumentoResource
hub.gestaodocumento.recurso.rest.v1.dto.*
hub.gestaodocumento.servico.GestaoDocumentoService
hub.gestaodocumento.dominio.*
hub.gestaodocumento.integracao.GestaoDocumentoClient
hub.gestaodocumento.integracao.GestaoDocumentoGateway
hub.gestaodocumento.integracao.GestaoDocumentoClientExceptionMapper
hub.gestaodocumento.integracao.mock.GestaoDocumentoMockFactory
hub.gestaodocumento.mapeamento.GestaoDocumentoMapper
```

### Arquitetura

Atual:

```text
api.dto.erro.*
api.exception.*
shared.exception.*
shared.observability.ObservabilityLog
infrastructure.client.ClientErrorBodyReader
infrastructure.client.RequestHeaderFactory
infrastructure.client.RestClientObservabilityFilter
infrastructure.client.mock.MarkdownJsonMockReader
```

Alvo:

```text
hub.arquitetura.excecao.dto.ErroPadraoDto
hub.arquitetura.excecao.dto.ErroMensagemDto
hub.arquitetura.excecao.MtrRestClientException
hub.arquitetura.excecao.MtrBusinessErrorException
hub.arquitetura.excecao.MtrClientErrorException
hub.arquitetura.excecao.MtrClientTechnicalException
hub.arquitetura.excecao.MtrServerErrorException
hub.arquitetura.excecao.MtrErrorType
hub.arquitetura.excecao.ClientErrorBodyReader
hub.arquitetura.excecao.MtrRestClientExceptionMapper
hub.arquitetura.excecao.GenericExceptionMapper
hub.arquitetura.excecao.ConstraintViolationExceptionMapper
hub.arquitetura.seguranca.RequestHeaderFactory
hub.arquitetura.observabilidade.ObservabilityLog
hub.arquitetura.observabilidade.RestClientObservabilityFilter
hub.arquitetura.configuracao.mock.MarkdownJsonMockReader
```

Observacao: `resiliencia` ainda nao tem classe propria porque resiliencia hoje esta aplicada diretamente nos REST Clients por anotacoes. Criar classe em `arquitetura.resiliencia` somente se houver configuracao, interceptor ou politica compartilhada real.

Observacao: `mensageria` ainda nao tem classe propria. Deve permanecer sem arquivos ate existir uso real.

## Dependencias alvo

Direcao desejada dentro de cada dominio:

```text
recurso.rest.v1 -> fachada -> servico -> integracao
recurso.rest.v1 -> mapeamento
fachada/servico -> dominio
mapeamento -> recurso.rest.v1.dto + dominio + integracao.dto quando existir
integracao -> arquitetura
dominio -> sem dependencia tecnica de Quarkus/REST sempre que possivel
```

Dependencias permitidas para `arquitetura`:

```text
dominio/* -> arquitetura.observabilidade, arquitetura.excecao, arquitetura.seguranca, arquitetura.configuracao.mock quando necessario
arquitetura -> nao deve depender de parametrizacao, dossieproduto ou gestaodocumento
```

Regra importante:

- `arquitetura` nao deve importar classes de dominio de negocio.
- Um dominio nao deve importar classes internas de outro dominio nesta migracao.
- Se uma dependencia entre dominios aparecer, ela deve ser explicitada como contrato de fachada, DTO de integracao, evento ou decisao pendente.

## Plano por etapas

### Etapa 0 - Baseline e inventario

1. Garantir arvore Git conhecida com `git diff`.
2. Executar a suite atual:

```powershell
mvn -q test
```

3. Registrar quantidade de classes por pacote e pontos com `simtr-hub`:

```powershell
rg -n "hub|simtr-hub|simtr_hub" pom.xml README.md doc src
```

4. Confirmar que a suite passa antes de qualquer movimentacao.

Criterio de aceite:

- baseline verde;
- lista de referencias legadas conhecida.

### Etapa 1 - Criar a identidade tecnica `hub`

Objetivo: remover `hub` do root Java e preparar o projeto para o nome funcional `SIMTR Hub`.

Alteracoes previstas:

- mover `src/main/java/br/gov/caixa/simtr/hub` para `src/main/java/br/gov/caixa/simtr/hub`;
- mover `src/test/java/br/gov/caixa/simtr/hub` para `src/test/java/br/gov/caixa/simtr/hub`;
- atualizar `package` e `import`;
- atualizar testes, fixtures e referencias ao pacote antigo;
- avaliar alteracao de `pom.xml` para `artifactId`/`name` `simtr-hub`;
- atualizar `quarkus.application.name` para `simtr-hub`;
- atualizar `quarkus.jacoco.title`;
- confirmar path de log em `target/logs/simtr-hub.json`.

Ponto de decisao:

- aprovar se os endpoints externos e paths OpenAPI/Swagger passam de `/hub/...` para `/hub/...` nesta etapa ou se ficam como compatibilidade temporaria.

Validacao:

```powershell
mvn -q test
rg -n "br\\.gov\\.caixa\\.simtr\\.hub" src
```

Criterio de aceite:

- nenhum package/import Java antigo;
- suite passa;
- decisao sobre path externo registrada.

### Etapa 2 - Verticalizar `parametrizacao`

Objetivo: migrar Parametrizacao para a estrutura por dominio.

Alteracoes previstas:

- mover resources e DTOs para `hub.parametrizacao.recurso.rest.v1`;
- mover services para `hub.parametrizacao.servico`;
- mover VOs para `hub.parametrizacao.dominio`;
- mover clients, gateways, client exception mapper e mocks para `hub.parametrizacao.integracao`;
- mover mappers para `hub.parametrizacao.mapeamento`;
- criar `ParametrizacaoFachada` para concentrar os casos de uso expostos;
- ajustar recursos para dependerem da fachada;
- preservar comportamento dos endpoints e simulador.

Validacao:

```powershell
mvn -q test
rg -n "hub\\.(api|application|domain|infrastructure|mapper|shared)" src
```

Testes que devem continuar cobrindo a etapa:

- `ParametrizacaoServiceTest`
- `ParametrizacaoMapperTest`
- testes HTTP de processo e checklist em `ResourceEndpointTest`
- `GatewayTest` para gateways de Parametrizacao
- `MockFactoryTest` para mocks de Parametrizacao.

Criterio de aceite:

- endpoints de processo e checklist continuam passando;
- nenhum recurso de Parametrizacao depende diretamente de gateway ou mock;
- nao ha import de pacote antigo.

### Etapa 3 - Verticalizar `gestaodocumento`

Objetivo: migrar Gestao Documento para a estrutura por dominio.

Alteracoes previstas:

- mover resource e DTO para `hub.gestaodocumento.recurso.rest.v1`;
- mover service para `hub.gestaodocumento.servico`;
- mover VO para `hub.gestaodocumento.dominio`;
- mover client, gateway, client exception mapper e mock para `hub.gestaodocumento.integracao`;
- mover mapper para `hub.gestaodocumento.mapeamento`;
- criar `GestaoDocumentoFachada`;
- preservar cuidado de nao logar `sas`.

Validacao:

```powershell
mvn -q test
```

Testes que devem continuar cobrindo a etapa:

- `GestaoDocumentoServiceTest`
- `GestaoDocumentoMapperTest`
- teste HTTP de credencial de container;
- teste de mock factory;
- testes de mascaramento de payload sensivel.

Criterio de aceite:

- endpoint de credencial continua aceitando chamada sem body e sem `Content-Type`;
- `sas` segue mascarado;
- suite passa.

### Etapa 4 - Verticalizar `dossieproduto`

Objetivo: migrar Dossie Produto para a estrutura por dominio.

Alteracoes previstas:

- mover resource e DTOs para `hub.dossieproduto.recurso.rest.v1`;
- mover service para `hub.dossieproduto.servico`;
- mover VOs para `hub.dossieproduto.dominio`;
- mover client, gateway, client exception mapper e mock para `hub.dossieproduto.integracao`;
- mover mapper para `hub.dossieproduto.mapeamento`;
- criar `DossieProdutoFachada`;
- preservar todos os endpoints ja implementados.

Ponto de atencao:

- `DossieProdutoService` hoje ainda converte VO para DTO e usa mapper para chamar gateway ou mock. Isso pode ficar temporariamente preservado para reduzir risco da movimentacao. Uma etapa posterior pode limpar a fronteira para deixar a fachada ou integracao responsavel por adaptacao de contrato.

Validacao:

```powershell
mvn -q test
```

Testes que devem continuar cobrindo a etapa:

- `DossieProdutoServiceTest`
- `DossieProdutoMapperTest`
- testes HTTP de criacao, formulario, documento, workflow e validacao negocial;
- `GatewayTest` para Dossie Produto;
- `MockFactoryTest` para Dossie Produto.

Criterio de aceite:

- todos os endpoints implementados de Dossie Produto continuam passando;
- regras ja consolidadas permanecem: `objeto` opcional em atributos/propriedades de documento e `previo` opcional em validacao negocial;
- suite passa.

### Etapa 5 - Migrar arquitetura compartilhada

Objetivo: remover os pacotes tecnicos antigos e consolidar compartilhados sob `hub.arquitetura`.

Alteracoes previstas:

- mover exceptions globais e DTOs de erro para `hub.arquitetura.excecao`;
- mover exception mappers globais para `hub.arquitetura.excecao`;
- mover `ClientErrorBodyReader` para `hub.arquitetura.excecao`;
- mover `RequestHeaderFactory` para `hub.arquitetura.seguranca`;
- mover `ObservabilityLog` e `RestClientObservabilityFilter` para `hub.arquitetura.observabilidade`;
- mover `MarkdownJsonMockReader` para `hub.arquitetura.configuracao.mock`;
- conferir que `arquitetura` nao importa nenhum dominio.

Validacao:

```powershell
mvn -q test
rg -n "hub\\.arquitetura\\..*hub\\.(parametrizacao|dossieproduto|gestaodocumento)" src/main/java
```

Criterio de aceite:

- `arquitetura` fica independente dos dominios;
- todos os dominios usam apenas utilitarios transversais aprovados;
- suite passa.

### Etapa 6 - Limpeza de referencias legadas e documentacao

Objetivo: remover referencias residuais a `simtr-hub`, quando aprovadas como parte da mudanca de identidade.

Alteracoes previstas:

- atualizar `README.md`;
- atualizar `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`;
- atualizar `doc/espaco-colaborativo-de-desenvolvimento.md`;
- atualizar copias documentais em `doc/properties`;
- atualizar exemplos de curl;
- atualizar nomes de spans/eventos/documentacao de logs;
- atualizar OpenAPI/Swagger paths se a decisao for migrar paths externos para `/hub`.

Validacao:

```powershell
mvn -q test
rg -n "hub|simtr-hub|simtr_hub" README.md doc pom.xml src
```

Criterio de aceite:

- referencias legadas restantes, se existirem, estao justificadas como compatibilidade temporaria;
- documentacao reflete a nova estrutura.

### Etapa 7 - Revisao arquitetural final

Objetivo: confirmar que o projeto realmente ficou preparado para extracao futura por microservico.

Checklist:

- cada dominio compila conceitualmente como um modulo vertical;
- resources de um dominio nao importam services/gateways de outro dominio;
- DTOs REST estao no pacote de recurso versionado;
- VOs estao no pacote de dominio;
- mappers estao no pacote de mapeamento do dominio;
- integracoes MTR estao no pacote de integracao do dominio;
- shared tecnico esta apenas em `arquitetura`;
- docs e testes usam o novo root `hub`.

Validacao final:

```powershell
mvn -q test
```

Opcional, se houver tempo:

```powershell
mvn -q package
```

## Testes previstos

Suite completa obrigatoria a cada etapa:

```powershell
mvn -q test
```

Grupos de teste que precisam ser revisados/ajustados:

- testes HTTP em `ResourceEndpointTest`;
- cobertura de resources em `ResourceBeanCoverageTest`;
- testes de services por dominio;
- testes de gateways;
- testes de mock factories;
- testes de mappers MapStruct;
- testes de exception mappers;
- testes de observabilidade e mascaramento.

Novos testes recomendados:

- teste simples de fachada por dominio, quando a fachada tiver logica alem de delegacao;
- teste de arquitetura via busca/import, se o time aceitar um teste de regra arquitetural leve para impedir `arquitetura -> dominio`;
- teste de endpoint OpenAPI/Swagger no novo path, se o path externo mudar para `/hub`.

Cobertura:

- manter cobertura minima de 80% de linhas para o novo desenvolvimento;
- nao reduzir a cobertura global abaixo da meta vigente;
- manter relatorio em `target/jacoco-report/index.html`.

## Riscos

- Alterar package root toca quase todos os arquivos Java e testes.
- Alterar paths externos para `/hub` pode quebrar consumidores, colecoes Postman, documentacao e API Manager.
- Alterar propriedades de `simtr-hub.*` para `hub.*` pode quebrar configuracoes de ambiente se nao houver plano de transicao.
- MapStruct gerado em `target/generated-sources` pode ficar com referencias antigas se o build nao limpar corretamente.
- Quarkus CDI pode falhar se algum bean for movido mas injecoes/imports ficarem inconsistentes.
- Mover exception mappers globais exige validar que as respostas de erro continuam iguais.
- Logs, spans e metricas podem mudar de nome; dashboards externos precisam ser avisados se existirem.

## Decisoes aprovadas

1. Seguir com a estrutura por dominio proposta.
2. Introduzir fachadas por dominio como fronteira interna para futura extracao em microservicos.
3. Migrar a identidade tecnica para `simtr-hub` em `pom.xml`, `quarkus.application.name`, logs, spans e propriedades.
4. Alterar os paths externos para `/hub/...` nesta migracao.
5. Executar a implementacao em etapas com checkpoints e validacao incremental.

## Criterios de aceite finais

- Root Java passa a ser `br.gov.caixa.simtr.hub`.
- Classes ficam organizadas por dominio de negocio, nao por layer global.
- `arquitetura` contem apenas classes compartilhadas/transversais.
- Pacotes antigos `api`, `application`, `domain`, `infrastructure`, `mapper` e `shared` deixam de existir sob o root.
- Nenhum import Java referencia os pacotes antigos `br.gov.caixa.simtr.hub.api`, `br.gov.caixa.simtr.hub.application`, `br.gov.caixa.simtr.hub.domain`, `br.gov.caixa.simtr.hub.infrastructure`, `br.gov.caixa.simtr.hub.mapper` ou `br.gov.caixa.simtr.hub.shared`.
- Endpoints implementados continuam com o mesmo comportamento aprovado, salvo mudanca externa explicitamente aprovada.
- Documentacao principal, README e espaco colaborativo refletem a nova estrutura.
- `mvn -q test` passa ao final de cada etapa.

## Ponto de aprovacao

Planejamento revisado e aprovado pelo usuario em 2026-07-10.

Respostas registradas:

1. Estrutura por dominio proposta: aprovada.
2. Fachadas por dominio: aprovadas.
3. Identidade tecnica `simtr-hub` em `pom.xml`, `quarkus.application.name`, logs, spans e propriedades: aprovada.
4. Paths externos: usar `/hub/...` a partir desta migracao.
