# Plano: consultar documentos vinculados ao dossiê de produto

## Intenção

Expor no `simtr-hub` uma nova capacidade atômica do domínio `dossieproduto` para consultar os
documentos vinculados a um dossiê, consumindo o contrato upstream
`GET /simtr-dossie-produto/v4/dossie-produto/{id}/documentos` do MTR e oferecendo a mesma porta de
saída por um simulador próprio.

## Resultado esperado

- o consumidor chama `GET /simtr-hub/v1/dossie-produto/{id}/documentos`;
- o Hub aceita o `id` e os 12 query params opcionais do contrato v4, sem corpo de requisição;
- em modo MTR, um REST Client próprio executa o `GET` v4 com API key, OIDC, correlação, timeout,
  retry e circuit breaker adequados a uma leitura idempotente;
- em modo simulador, a mesma porta de saída lê uma fixture própria do identificador `4081899`,
  derivada do exemplo `200` com os seis filtros `inclui-*` solicitados como `true` em
  `doc/api-integracao-mtr-pre-validacao-v1.md` e sem rede;
- DTOs e mappers REST, MTR e simulador permanecem independentes;
- `200` preserva a lista e o JSON `snake_case`; ausência de documentos produz `204` sem corpo;
- CPF, CNPJ, IP, nome, matrícula, URL, paths, payload, credenciais e query string não aparecem em
  logs ou traces.

## Premissas aprovadas em C1

C0 e C1 foram aprovados explicitamente pelo usuário em 18/08/2026, incluindo as propostas
recomendadas abaixo e o detalhamento adicional da fixture do simulador.

1. O path público será `GET /simtr-hub/v1/dossie-produto/{id}/documentos`. O path informado no
   pedido, com prefixo `/simtr-dossie-produto/v4`, é o contrato upstream do MTR e não será exposto
   diretamente pelo Hub.
2. `id` será `Long`, obrigatório e maior que zero, reutilizando `IdentificadorDossieProduto`.
3. Os query params públicos terão os mesmos nomes, tipos e opcionalidade do Swagger:
   `cnpj`, `cpf`, `fase`, `inclui-armazenamento`, `inclui-assinaturas`, `inclui-atributos`,
   `inclui-conformidade`, `inclui-outsourcing`, `inclui-propriedades`, `inclui-url`, `ip-usuario`
   e `tipologia`.
4. Query params ausentes permanecerão nulos no modelo de borda e serão omitidos do wire MTR;
   valores booleanos explicitamente enviados, inclusive `false`, serão encaminhados. O Hub não
   inventará validação de CPF, CNPJ, IP, tipologia ou `fase` que não exista no contrato fonte.
5. O corpo `200` seguirá a estrutura do schema
   `v3.dossie-produto.InstanciaDocumentoDTO1`, referenciado pelo endpoint v4, preservando nomes,
   ordem, nulos e listas conforme o wire aprovado.
6. Datas serão transportadas como `String` opaca no formato recebido, sem parse, timezone ou
   reformatação, porque o Swagger declara `Calendar` enquanto seus exemplos usam
   `dd/MM/yyyy HH:mm:ss`.
7. Uma resposta MTR `204`, `null` ou uma coleção vazia será representada internamente por lista
   vazia e traduzida pelo adapter REST para `204` sem corpo. Uma coleção não vazia retorna `200`.
8. `ip-usuario` será encaminhado somente quando explicitamente informado. O Hub não confiará
   silenciosamente em `Forwarded`/`X-Forwarded-For`; quando `inclui-url=true` sem `ip-usuario`, o
   comportamento de fallback do MTR poderá usar o IP de saída do Hub. Qualquer inferência de IP
   exige ampliação de escopo e decisão de segurança própria.
9. O simulador usará uma fixture Markdown/JSON identificada por `4081899`, cujo primeiro título
   será exatamente `# 4081899`. Como o leitor atual aceita objeto JSON, o contrato exclusivo do
   simulador poderá encapsular a lista em `{"documentos": [...]}`; esse wrapper nunca aparecerá
   no REST público nem no DTO MTR.
10. O simulador não reimplementará as regras internas de filtragem e projeção do MTR. Ele aceitará
    os critérios pela mesma porta e entregará o cenário determinístico da fixture por id; o wire e
    a propagação exata de todos os filtros serão provados no caminho MTR.
11. A fixture partirá do corpo da resposta `200` do exemplo filtrado abaixo, preservará sua
    estrutura, listas, nulos e campos, mas substituirá CPF, CNPJ, nome, matrícula, código GED, URL
    e paths possivelmente reais por valores claramente sintéticos:

    `GET {{dossie-produto-BaseUrl}}/simtr-dossie-produto/v4/dossie-produto/4081899/documentos?inclui-armazenamento=true&inclui-assinaturas=true&inclui-atributos=true&inclui-conformidade=true&inclui-outsourcing=true&inclui-propriedades=true`
12. A chamada MTR não registrará o `RestClientObservabilityFilter`, pois ele publica a URL completa
    e vazaria filtros. Um provider local, no padrão da captura de dossiê, suprimirá o span HTTP
    automático e reinjetará o contexto no outbound.
13. A matriz inicial de fault tolerance será a da consulta GET já existente: timeout de 2.000 ms,
    até três retries com atraso de 300 ms e jitter de 100 ms, e circuit breaker 10/0,5/10 s/2;
    apenas falhas transitórias serão repetidas.

## Critérios de sucesso

- método, rota, parâmetros, status, JSON, nulabilidade e OpenAPI públicos estão protegidos por
  testes executáveis;
- o wire MTR usa exatamente `/simtr/dossie-produto/v4/dossie-produto/{id}/documentos`, sem corpo,
  com query params presentes/ausentes conforme a chamada e headers obrigatórios;
- `200`, `204`, `400`, `401`, `403`, `404`, `500`, timeout, retry e circuit breaker têm comportamento
  caracterizado;
- MTR e simulador implementam a mesma porta de saída e a seleção CDI funciona nos dois modos;
- a fixture do id `4081899` funciona sem rede e contém somente dados sintéticos;
- existe exatamente um span CLIENT próprio no modo MTR e nenhum span CLIENT no simulador;
- nenhum sinal contém query string, CPF, CNPJ, IP, nome, matrícula, URL de documento, path de
  storage, API key, bearer token ou payload de resposta;
- ArchUnit, testes focados, suíte completa, build e checkpoint Sonar aplicável passam ou têm decisão
  humana registrada conforme `AGENTS.md`;
- README, arquitetura consolidada, ADR-0002, índice de ADRs e catálogo de observabilidade refletem
  somente o estado efetivamente implementado.

## Escopo

- criar `ConsultarDocumentosDossieProduto` como nona capacidade atômica de `dossieproduto`;
- criar critérios e modelo interno de leitura, porta de entrada, caso de uso, porta de saída e
  falha específica;
- adicionar o endpoint público v1, DTOs de query/resposta e mapper REST próprios;
- criar REST Client, query DTO, response DTO, mapper, adapter, erro protocolar e provider de tracing
  próprios para o contrato MTR v4;
- reutilizar o config key `dossie-produto`, os providers de API key/OIDC e a property
  `simtr-hub.simulador.dossie-produto.habilitado`;
- criar adapter, DTO, mapper, qualifiers, producer, observabilidade e fixture próprios do simulador;
- preservar a seção “Manter inalterado o endpoint GET de Consulta uma lista de documentos...” e o
  Swagger local como fontes somente leitura;
- cobrir contrato HTTP/JSON, query params, validação, mapeamentos, wire, erros, fault tolerance,
  simulador, CDI, observabilidade, segurança e regras ArchUnit;
- atualizar documentação fonte depois da implementação aprovada.

## Fora de escopo

- expor `/simtr-dossie-produto/v4` como namespace público do Hub;
- alterar o Swagger MTR ou `doc/api-integracao-mtr-pre-validacao-v1.md`;
- implementar alteração de garantia, cancelamento, workflow, orquestração ou outro endpoint;
- baixar, armazenar, fazer proxy do binário ou validar a URL da imagem do documento;
- interpretar datas, resultados de conformidade, outsourcing ou conteúdo dos atributos;
- reproduzir no simulador a engine de filtros/projeções do MTR;
- inferir IP do usuário a partir de headers não confiáveis;
- compartilhar DTO ou mapper entre REST, MTR e simulador;
- alterar o filtro REST Client compartilhado ou os clients existentes;
- adicionar dependência, config key, property de seleção, cache, persistência ou paginação;
- refatorar o `DossieProdutoResource` ou outras capacidades por tamanho/estilo;
- criar snapshot ou arquivo OpenAPI estático;
- atualizar Postman ou formatos derivados `.html`, `.pdf`, `.ppt` e `.pptx`.

## Contexto verificado

- arquitetura lida em `doc/arquitetura-ddd-integracoes-atomicas.md`;
- índice e ADRs aplicáveis lidos: ADR-0001, ADR-0002, ADR-0004, ADR-0005 e ADR-0006;
- contrato fonte inspecionado em
  `doc/swagger-mtr/simtr-dossie-produto-openapi- 2.20.0.8`, versão `2.20.0.8`;
- exemplo filtrado do id `4081899`, seu corpo `200`, filtros, `204` e erros inspecionados na seção
  indicada de `doc/api-integracao-mtr-pre-validacao-v1.md`;
- ausência atual da capacidade confirmada na arquitetura, API pública, código, testes e fixtures;
- padrão comparado com `ConsultarDossieProduto`, incluindo Resource, portas, caso de uso, REST
  Client, adapter MTR, simulador, producer, observabilidade e testes com stub;
- provider seguro comparado com `CapturarDossieProdutoMtrTracingProvider` e seus contratos;
- testes transversais inspecionados: `DossieProdutoApiContractTest`,
  `DossieProdutoErroApiContractTest`, `DossieProdutoValidacaoJakartaContractTest`,
  `ResourceBeanCoverageTest`, `ObservabilidadeSpansContratoTest` e `ArchUnitProgressivoTest`;
- configuração confirmada: base MTR `/simtr`, client `/dossie-produto`, property de simulador comum;
- o `MarkdownJsonMockReader` atual extrai somente objeto JSON, não array de topo;
- a `definition-of-done.md` referenciada pelas skills não está instalada; `AGENTS.md`, os critérios
  por task e as verificações executáveis deste plano formam a Definition of Done aplicável.

## Divergências e riscos de contrato

| Fonte | Divergência/risco | Tratamento proposto |
|---|---|---|
| Swagger 2.20.0.8 | Datas são `Calendar`/objeto, exemplos são strings | String opaca; congelar wire e decidir em C1 |
| Swagger 2.20.0.8 | Campos requeridos como datas, URL e listas aparecem nulos ou ausentes nos exemplos | Preservar nulabilidade/ausência observada sem inventar validação |
| Swagger x gateway | Swagger publica `/simtr-dossie-produto`; configuração usa `/simtr` + `/dossie-produto` | Testar o wire efetivo sem mudar configuração comum |
| Exemplo documental | Contém identidade, matrícula e identificadores possivelmente reais | Sanitizar fixture e proibir literais em sinais/testes |
| Filtro compartilhado | `RestClientObservabilityFilter` registra URL completa, inclusive query string | Provider local e teste negativo de vazamento |
| `inclui-url` | Fallback de IP da chamada direta muda quando existe o Hub como proxy | Encaminhar apenas `ip-usuario` explícito; decisão em C1 |
| Simulador | Fonte oferece um corpo `200` com seis filtros `inclui-*` ativos, mas não define algoritmo simulável para todas as combinações | Usar esse corpo como cenário determinístico do id; não duplicar regras do MTR |
| Leitor de fixture | Aceita objeto, enquanto resposta pública é array | Wrapper exclusivo do simulador, removido pelo mapper |
| Resource atual | Já concentra oito capacidades e está próximo de 1.000 linhas | Adicionar só o método necessário; refatoração permanece fora de escopo |
| Arquitetura consolidada | Registra 11 capacidades totais e oito em `dossieproduto` | Após implementação, atualizar para 12 e nove, respectivamente |

## Contrato público proposto

| Elemento | Proposta |
|---|---|
| Método/path | `GET /simtr-hub/v1/dossie-produto/{id}/documentos` |
| Path param | `id`: `Long`, obrigatório, maior que zero |
| Query params | os 12 nomes listados na premissa 3, todos opcionais |
| Corpo de request | nenhum; não enviar `Content-Type` |
| Sucesso com itens | `200 OK`, `application/json`, array tipado |
| Sem itens | `204 No Content`, sem corpo |
| Erros | `400`, `401`, `403`, `404` e `500`, conforme tradução REST vigente |
| OpenAPI | annotations do Resource/DTOs; nenhum snapshot estático |

O DTO de query REST será uma borda própria e o mapper produzirá um critério semântico interno.
Flags ausentes e `false` explícito permanecerão distinguíveis até o client para evitar adicionar
query params não enviados pelo consumidor.

### Estrutura do item `200`

- campos base: `id_instancia_documento`, `id_documento`, `codigo_ged`, `data_hora_captura`,
  `data_hora_validade`, `matricula_captura`, `tipo_documento`, `situacao_documento` e `url`;
- vínculo: `cliente`, `produto`, `garantia` com avalistas, `fase` e `processo`;
- coleções opcionais: `atributos`, `assinaturas_digitais`, `conformidade`, `propriedades`,
  `outsourcing` e `armazenamento`;
- os subtipos e campos internos seguirão o schema v3 referenciado pelo endpoint v4 e o exemplo da
  seção preservada; records aninhados serão preferidos para manter coesão e limitar arquivos.

## Contrato MTR proposto

| Elemento | Proposta |
|---|---|
| REST Client | `ConsultaDocumentosDossieProdutoMtrClient`, config key `dossie-produto` |
| Base | `@Path("/dossie-produto")` |
| Operação | `@GET @Path("/v4/dossie-produto/{id}/documentos")` |
| Wire | `GET /simtr/dossie-produto/v4/dossie-produto/{id}/documentos` |
| Query | DTO MTR próprio com nomes hifenizados exatos e omissão de nulos |
| Headers | API key, OIDC, `Accept: application/json` e `traceparent`; sem corpo/Content-Type |
| Provider | provider local de tracing; não registrar `RestClientObservabilityFilter` |
| Sucesso | lista de DTO MTR v4 convertida pelo mapper; vazio normalizado semanticamente |
| Erros | `400/404` negócio; demais `4xx` técnica cliente; `5xx` servidor |
| Retry | somente servidor, conexão/processamento e timeout; nunca `400/401/403/404` |

## Desenho arquitetural proposto

```text
GET /simtr-hub/v1/dossie-produto/{id}/documentos?...
    -> DossieProdutoResource
        -> ConsultarDocumentosDossieProduto
            -> ConsultarDocumentosDossieProdutoCasoDeUso
                -> ObterDocumentosDossieProduto
                    |-- ConsultaDocumentosDossieProdutoMtrAdapter
                    |     -> ConsultaDocumentosDossieProdutoMtrClient v4
                    `-- ConsultaDocumentosDossieProdutoSimuladorAdapter
                          -> fixture 4081899-v4-consulta-documentos-dossie-produto.md
```

- `CriteriosConsultaDocumentosDossieProduto` conterá o identificador e os filtros sem annotations
  de borda;
- `DocumentoDossieProdutoConsultado` será modelo interno de leitura, não aggregate preventivo;
- a porta de saída expressará “obter documentos do dossiê”, não uma API genérica do MTR;
- nenhuma conversão será REST -> MTR ou MTR -> REST;
- a nova capacidade aplica os ADRs existentes e não exige ADR novo.

## Estilo, estrutura e limites

```java
public interface ConsultarDocumentosDossieProduto {

    Uni<List<DocumentoDossieProdutoConsultado>> executar(
            CriteriosConsultaDocumentosDossieProduto criterios);
}
```

- produção em `src/main/java/br/gov/caixa/simtr/hub/dossieproduto` por camada/borda existente;
- testes espelham os packages em `src/test/java` e integração usa o stub MTR local;
- fixture em `src/main/resources/mock/dossieproduto`;
- nomes Java em linguagem do negócio e nomes JSON/query explícitos nas bordas;
- `Uni` ponta a ponta, sem `await`, `subscribe`, threads ou bloqueio;
- sempre: contrato primeiro, RED -> GREEN -> REFACTOR, dados sintéticos e verificação focada;
- perguntar antes: contrato, arquitetura, segurança, observabilidade, nova dependência/configuração;
- nunca: DTO compartilhado, query/payload em logs, segredo em arquivo, mudança silenciosa de fonte
  documental ou formato derivado.

## Observabilidade e segurança propostas

| Camada | Sinal |
|---|---|
| API | span SERVER `simtr-hub.api.dossie-produto.documentos.consultar` |
| Aplicação | span INTERNAL `simtr-hub.service.dossie-produto.documentos.consultar` |
| MTR | span CLIENT `mtr.dossie-produto.documentos.consultar` |
| Eventos Hub | prefixo `simtr-hub.dossie-produto.documentos.consulta` |
| Eventos MTR | prefixo `mtr.dossie-produto.documentos.consulta` |
| Atributos permitidos | rota templated, API v4, origem, flag do simulador, id e quantidade |

- `url.path` será templated e nunca conterá query string;
- não registrar valores ou presença individual de CPF, CNPJ, IP, tipologia ou URL;
- não registrar item, payload, erro externo textual, `traceparent`, API key ou bearer token;
- o modo MTR terá exatamente um CLIENT próprio e propagará seu contexto ao stub/upstream;
- o modo simulador não terá span CLIENT nem chamada de rede.

## Decisões e impactos

| Dimensão | Situação | Checkpoint humano adicional? |
|---|---|---|
| Contrato | Novo path, query params, status, JSON, validação e OpenAPI públicos | sim — C1 |
| Arquitetura | Nona capacidade, portas e adapters em `dossieproduto`; sem novo ADR | sim — C1 |
| Segurança | CPF/CNPJ/IP/URL na superfície e provider local para impedir vazamento | sim — C1 |
| Observabilidade | Novos spans/eventos/atributos e exclusão do filtro compartilhado nesse client | sim — C1 |
| Fault tolerance | Matriz de leitura GET idempotente | sim — C1 |
| Dependências/configuração | Reuso de biblioteca, config key e property existentes | não |

## Estratégia de testes

- RED HTTP para rota, query params, `200`, `204`, erro de id e contrato JSON exato;
- testes de DTO/mapper REST para nulos, listas, strings de data e wrapper inexistente no público;
- caso de uso e porta provando repasse integral dos critérios;
- DTO/mapper MTR para todos os subtipos, listas ausentes/vazias e elementos nulos;
- reflexão do REST Client para path, providers, query DTO e matriz de fault tolerance;
- adapter MTR para `200`, `204`, `400/401/403/404/500`, corpo inválido, timeout e retry;
- integração com stub para método, path, query exata, ausência de corpo/Content-Type, headers e
  `traceparent`;
- fixture/mapper/adapter do simulador, id presente/ausente, seleção CDI e ausência de rede;
- parentage SERVER -> INTERNAL -> CLIENT no MTR e SERVER -> INTERNAL no simulador;
- testes negativos para todos os valores sensíveis nos sinais;
- ArchUnit e cobertura de beans/resources para fronteiras e bootstrap CDI.

## Comandos de verificação

```powershell
mvn -q "-Dtest=DossieProdutoApiContractTest,DossieProdutoErroApiContractTest,ResourceEndpointTest" test
mvn -q "-Dtest=ConsultarDocumentosDossieProdutoCasoDeUsoTest,ConsultaDocumentosDossieProdutoRestMapperTest" test
mvn -q "-Dtest=ConsultaDocumentosDossieProdutoMtrClientTest,ConsultaDocumentosDossieProdutoMtrMapperTest,ConsultaDocumentosDossieProdutoMtrAdapterTest" test
mvn -q "-Dtest=ConsultaDocumentosDossieProdutoMtrContractTest,ConsultaDocumentosDossieProdutoSelecaoSimuladorQuarkusTest" test
mvn -q "-Dtest=ArchUnitProgressivoTest,ResourceBeanCoverageTest,ObservabilidadeSpansContratoTest" test
mvn -q clean test
./validar-checkpoint-sonarqube.ps1
git diff --check
```

O comando Sonar só será executado após baseline autorizado conforme a seção seguinte.

## Tarefas

### Task 1 — Inicializar baseline técnico

**Descrição:** depois de C0/C1, verificar se existem pacotes em `sonar/`, pedir ao usuário a fonte
do baseline quando exigido e inicializar o baseline antes da primeira alteração em `src/`.

**Critérios de aceitação:** fonte escolhida pelo usuário; baseline registrado ou indisponibilidade
evidenciada sem declarar aprovação.

**Verificação:** comando oficial `./validar-checkpoint-sonarqube.ps1 -InitializeBaseline` com os
parâmetros autorizados.

**Dependências:** C0 e C1. **Arquivos prováveis:** nenhum arquivo fonte.

### Task 2 — Congelar o contrato público em RED

**Descrição:** adicionar testes que exijam a rota, parâmetros, `200`, `204`, ausência de corpo e
validação de `id` antes de criar produção.

**Critérios de aceitação:** RED falha pela capacidade ausente; JSON esperado usa somente dados
sintéticos; annotations OpenAPI são verificadas pelo contrato Java.

**Verificação:** primeiro comando focado da seção de comandos.

**Dependências:** Task 1.

**Arquivos prováveis:**

- `src/test/java/br/gov/caixa/simtr/hub/contrato/DossieProdutoApiContractTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/contrato/DossieProdutoErroApiContractTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/recurso/ResourceEndpointTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/contrato/DossieProdutoValidacaoJakartaContractTest.java`.

### Task 3 — Modelar critérios, documentos e falha interna

**Descrição:** criar os tipos semânticos do núcleo sem annotations ou DTOs de borda.

**Critérios de aceitação:** todos os filtros são preservados; modelo cobre o schema v3; falha não
transporta exceção HTTP/DTO MTR; listas/nulos têm semântica testada.

**Verificação:** testes unitários dos modelos e falha.

**Dependências:** Task 2.

**Arquivos prováveis:**

- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/dominio/modelo/CriteriosConsultaDocumentosDossieProduto.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/dominio/modelo/DocumentoDossieProdutoConsultado.java`;
- `src/main/java/br/gov/caixa/simtr/hub/dossieproduto/dominio/erro/FalhaConsultaDocumentosDossieProduto.java`;
- testes correspondentes em `src/test/java/.../dossieproduto/dominio/`.

### Task 4 — Criar portas e caso de uso

**Descrição:** conectar a porta de entrada à necessidade de saída, mantendo o caso de uso atômico.

**Critérios de aceitação:** `Uni<List<...>>` sem bloqueio; critérios chegam sem perda à porta de
saída; aplicação não importa adapters.

**Verificação:** segundo comando focado, limitado inicialmente ao caso de uso.

**Dependências:** Task 3.

**Arquivos prováveis:**

- `src/main/java/.../aplicacao/porta/entrada/ConsultarDocumentosDossieProduto.java`;
- `src/main/java/.../aplicacao/porta/saida/ObterDocumentosDossieProduto.java`;
- `src/main/java/.../aplicacao/casodeuso/ConsultarDocumentosDossieProdutoCasoDeUso.java`;
- `src/test/java/.../aplicacao/casodeuso/ConsultarDocumentosDossieProdutoCasoDeUsoTest.java`.

### Task 5 — Entregar a borda REST pública

**Descrição:** criar query/response DTOs e mapper próprios e adicionar somente o método necessário
ao `DossieProdutoResource`.

**Critérios de aceitação:** rota/params exatos; array `200`; vazio `204`; erro interno traduzido;
nenhum wrapper do simulador ou DTO MTR aparece no contrato público.

**Verificação:** testes RED da Task 2 ficam GREEN e mapper REST passa isoladamente.

**Dependências:** Task 4.

**Arquivos prováveis:**

- `src/main/java/.../adaptador/entrada/rest/v1/DossieProdutoResource.java`;
- `src/main/java/.../adaptador/entrada/rest/v1/dto/ConsultaDocumentosDossieProdutoQueryParams.java`;
- `src/main/java/.../adaptador/entrada/rest/v1/dto/ConsultaDocumentosDossieProdutoResponse.java`;
- `src/main/java/.../adaptador/entrada/rest/v1/ConsultaDocumentosDossieProdutoRestMapper.java`;
- `src/test/java/.../adaptador/entrada/rest/v1/ConsultaDocumentosDossieProdutoRestMapperTest.java`.

### Checkpoint técnico A — Núcleo e REST

- testes focados das Tasks 2 a 5 verdes;
- diff revisado quanto a contrato, simplicidade, segurança e direção de dependência;
- nenhuma alteração fora da nova capacidade.

### Task 6 — Criar DTO e mapper MTR v4

**Descrição:** modelar a lista upstream em DTO próprio e convertê-la para o modelo interno.

**Critérios de aceitação:** todos os campos do schema referenciado são tipados; datas permanecem
strings; nulos/listas/ordem são preservados; resposta vazia vira lista vazia.

**Verificação:** teste focado do mapper MTR.

**Dependências:** Checkpoint A.

**Arquivos prováveis:**

- `src/main/java/.../adaptador/saida/mtr/dto/v4/documentos/ConsultaDocumentosDossieProdutoMtrResponse.java`;
- `src/main/java/.../adaptador/saida/mtr/mapper/ConsultaDocumentosDossieProdutoMtrMapper.java`;
- `src/test/java/.../adaptador/saida/mtr/mapper/ConsultaDocumentosDossieProdutoMtrMapperTest.java`.

### Task 7 — Definir client MTR e propagação segura

**Descrição:** criar query DTO, client, erro protocolar e provider local que não publica a URL
com filtros.

**Critérios de aceitação:** path/providers/query/fault tolerance exatos; `204` não vira erro;
`400/404` não têm retry; filtro compartilhado ausente; contexto reinjetado.

**Verificação:** teste focado e reflexivo do REST Client.

**Dependências:** Task 6.

**Arquivos prováveis:**

- `src/main/java/.../adaptador/saida/mtr/dto/v4/documentos/ConsultaDocumentosDossieProdutoMtrQuery.java`;
- `src/main/java/.../adaptador/saida/mtr/client/ConsultaDocumentosDossieProdutoMtrClient.java`;
- `src/main/java/.../adaptador/saida/mtr/client/ConsultaDocumentosDossieProdutoMtrTracingProvider.java`;
- `src/main/java/.../adaptador/saida/mtr/erro/ConsultaDocumentosDossieProdutoMtrException.java`;
- `src/test/java/.../adaptador/saida/mtr/client/ConsultaDocumentosDossieProdutoMtrClientTest.java`.

### Task 8 — Implementar o adapter MTR

**Descrição:** chamar o client, validar/mapear a resposta, registrar somente sinais permitidos e
traduzir erros depois dos interceptors.

**Critérios de aceitação:** filtros preservados; contagem registrada sem conteúdo; erro lossless
no núcleo; timeout/transientes classificados; sem query/payload em sinais.

**Verificação:** terceiro comando focado da seção de comandos.

**Dependências:** Task 7.

**Arquivos prováveis:**

- `src/main/java/.../adaptador/saida/mtr/adapter/ConsultaDocumentosDossieProdutoMtrAdapter.java`;
- `src/test/java/.../adaptador/saida/mtr/adapter/ConsultaDocumentosDossieProdutoMtrAdapterTest.java`.

### Task 9 — Provar o wire MTR ponta a ponta

**Descrição:** estender o stub e executar a rota pública até o client real em teste.

**Critérios de aceitação:** método/path/query/headers/corpo exatos; `200/204/erros/retry` provados;
um CLIENT próprio; `traceparent` correto; valores sensíveis ausentes dos sinais.

**Verificação:** quarto comando focado, limitado ao contrato MTR.

**Dependências:** Task 8.

**Arquivos prováveis:**

- `src/test/java/.../integracao/DossieProdutoMtrStubTestResource.java`;
- `src/test/java/.../integracao/ConsultaDocumentosDossieProdutoMtrContractTest.java`;
- `src/test/java/.../arquitetura/observabilidade/ObservabilidadeSpansContratoTest.java`.

### Checkpoint Sonar B — Fim da fatia MTR

- testes focados e guardrails verdes;
- executar `./validar-checkpoint-sonarqube.ps1` porque o fingerprint executável mudou;
- se `NON_COMPLIANT`, apresentar toda a evidência e aguardar decisão humana
  `Reprovar`, `AceitarExcepcionalmente` ou `ContinuarAjustes`.

### Task 10 — Criar contrato e fixture do simulador

**Descrição:** adicionar wrapper/DTO e mapper exclusivos do simulador e a fixture sanitizada do id
`4081899`, derivada do corpo `200` do exemplo com `inclui-armazenamento`, `inclui-assinaturas`,
`inclui-atributos`, `inclui-conformidade`, `inclui-outsourcing` e `inclui-propriedades` iguais a
`true`.

**Critérios de aceitação:** JSON interno contém `documentos`; resposta pública não; dados pessoais
e caminhos são sintéticos; lista/nulos/ordem são preservados; a primeira linha Markdown é
exatamente `# 4081899`.

**Verificação:** teste focado do mapper/fixture e busca negativa pelos literais originais.

**Dependências:** Checkpoint B conforme decisão registrada.

**Arquivos prováveis:**

- `src/main/java/.../adaptador/saida/simulador/dto/ConsultaDocumentosDossieProdutoSimuladorResponse.java`;
- `src/main/java/.../adaptador/saida/simulador/mapper/ConsultaDocumentosDossieProdutoSimuladorMapper.java`;
- `src/main/resources/mock/dossieproduto/4081899-v4-consulta-documentos-dossie-produto.md`;
- `src/test/java/.../adaptador/saida/simulador/mapper/ConsultaDocumentosDossieProdutoSimuladorMapperTest.java`.

### Task 11 — Implementar adapter e qualifiers do simulador

**Descrição:** criar o adapter do simulador e os dois qualifiers da nova porta de saída.

**Critérios de aceitação:** fixture existente retorna itens sem rede; ausência de fixture tem erro
simulado aprovado; adapters ficam identificados sem ambiguidade; simulador não recebe FT.

**Verificação:** teste unitário do adapter e inspeção dos qualifiers.

**Dependências:** Task 10.

**Arquivos prováveis:**

- `src/main/java/.../adaptador/saida/simulador/adapter/ConsultaDocumentosDossieProdutoSimuladorAdapter.java`;
- `src/main/java/.../adaptador/configuracao/qualificador/ConsultaDocumentosDossieProdutoMtr.java`;
- `src/main/java/.../adaptador/configuracao/qualificador/ConsultaDocumentosDossieProdutoSimulador.java`;
- `src/test/java/.../adaptador/saida/simulador/adapter/ConsultaDocumentosDossieProdutoSimuladorAdapterTest.java`.

### Task 12 — Integrar observabilidade e bootstrap CDI

**Descrição:** criar o producer explícito, adicionar o decorator observável da aplicação e provar
os dois modos em Quarkus.

**Critérios de aceitação:** parentage e nomes aprovados; origem/quantidade sem dados sensíveis;
simulador não chama stub; todos os beans resolvem no bootstrap.

**Verificação:** quarto e quinto comandos focados da seção de comandos.

**Dependências:** Task 11.

**Arquivos prováveis:**

- `src/main/java/.../adaptador/configuracao/ConsultaDocumentosDossieProdutoPortasProducer.java`;
- `src/main/java/.../adaptador/configuracao/ConsultaDocumentosDossieProdutoObservabilidade.java`;
- `src/test/java/.../adaptador/configuracao/ConsultaDocumentosDossieProdutoObservabilidadeTest.java`;
- `src/test/java/.../integracao/ConsultaDocumentosDossieProdutoSelecaoSimuladorQuarkusTest.java`;
- `src/test/java/br/gov/caixa/simtr/hub/recurso/ResourceBeanCoverageTest.java`.

### Task 13 — Atualizar documentação fonte

**Descrição:** registrar somente o estado efetivamente implementado, sem alterar fontes externas
ou derivados.

**Critérios de aceitação:** nova rota/capacidade e sinais documentados; ADR-0002 e índice coerentes;
nenhum `.html/.pdf/.ppt/.pptx` alterado.

**Verificação:** inspeção do diff documental e busca pelos números/rota/sinais.

**Dependências:** Task 12.

**Arquivos prováveis:**

- `README.md`;
- `doc/arquitetura-ddd-integracoes-atomicas.md`;
- `doc/adr/0002-limites-por-dominio-e-capacidade.md`;
- `doc/adr/README.md`;
- `doc/catalogo-observabilidade.md`.

### Task 14 — Revisar e verificar a feature completa

**Descrição:** revisar correção, simplicidade, arquitetura, segurança, desempenho, testes e escopo;
executar suíte, checkpoint e validação runtime nos modos simulador e MTR/stub.

**Critérios de aceitação:** critérios de sucesso satisfeitos; nenhum segredo/dado sensível novo;
nenhuma mudança fora de escopo; evidências registradas no `todo.md`.

**Verificação:** `mvn -q clean test`, checkpoint Sonar, `git diff --check`, inspeção do diff e chamadas
manuais `200`, `204` e `400` com a aplicação em execução.

**Dependências:** Task 13.

**Arquivos prováveis:** somente ajustes diretamente exigidos por findings da revisão.

### Checkpoint final — CF

- suíte/build/checkpoint e runtime evidenciados;
- documentação fonte coerente e formatos derivados intactos;
- diff integral apresentado;
- somente o usuário registra aceitação e encerramento.

## SonarQube

- até o registro de C0/C1, esta sessão alterou exclusivamente documentação: `sonar/` não havia sido
  inspecionado e nenhum baseline, Maven, API Sonar ou checkpoint havia sido executado;
- após C0/C1, foi confirmada a ausência do diretório `sonar/`; não havia pacote offline nem escolha
  adicional a solicitar;
- baseline `LOCAL_SONAR` inicializado em 18/08/2026, antes de qualquer alteração em `src/`, com
  status `READY` e avaliação técnica `COMPLIANT`: 213 issues no baseline, nenhuma issue nova,
  nenhuma issue high/blocker, cobertura de 88% e duplicação de 3,3%;
- checkpoint esperado ao fim da fatia MTR e novamente no fechamento se o fingerprint mudar;
- baseline offline permanece `UNVERIFIED` e nunca autoriza declarar métricas atuais aprovadas.

## Riscos e controles

| Risco | Impacto | Controle |
|---|---|---|
| Vazamento de filtros/PII pela URL observada | alto | provider local, rota templated e testes negativos |
| URL vinculada ao IP do Hub | alto | só encaminhar IP explícito; decisão de segurança em C1 |
| Divergência Calendar/string | alto | String opaca e contrato wire/JSON congelado |
| DTO grande e profundo | médio | records aninhados por borda, sem abstração genérica |
| Alterar nulos/listas/ordem | alto | comparação JSON exata e testes de mapper |
| Retry indevido em falha funcional | alto | matriz reflexiva e integração 400/404/500 |
| Simulador confundido com engine MTR | médio | limitação explícita e cenário determinístico por id |
| `DossieProdutoResource` crescer além de 1.000 linhas | médio | mudança cirúrgica; refatoração separada se aprovada |
| Fixture com dados reais | alto | anonimização, revisão e buscas negativas |
| Bootstrap CDI ambíguo | alto | qualifiers/producer e `ResourceBeanCoverageTest` |
| Regressão arquitetural | alto | ArchUnit e revisão de imports entre bordas |

## Dependências

- GO humano C0 e aprovação C1, registrados em 18/08/2026;
- escolha humana da fonte do baseline Sonar quando houver pacote offline;
- contrato local MTR 2.20.0.8 e exemplo documental já versionados;
- infraestrutura existente de REST Client, OIDC, API key, OpenTelemetry, Mutiny, fixture e stub;
- nenhuma nova biblioteca ou serviço.

## Decisões aprovadas em C1

1. Path público no namespace `/simtr-hub/v1` aprovado.
2. `204` para lista vazia aprovado, inclusive quando o MTR responder `200 []`.
3. Ausência de inferência de `ip-usuario` quando o parâmetro não for informado aprovada.
4. Simulador determinístico por id, sem reproduzir filtros/projeções do MTR, aprovado.
5. Datas como strings opacas e matriz de fault tolerance proposta aprovadas.
6. Para o id `4081899`, fixture com título `# 4081899` e dados derivados do corpo `200` do exemplo
   com os seis filtros `inclui-*` indicados como `true`, preservando a sanitização, aprovada.

## GO registrado

C0 e C1 foram registrados em `todo.md` em 18/08/2026. A próxima etapa é escolher e inicializar o
baseline SonarQube antes da primeira alteração de produção.
