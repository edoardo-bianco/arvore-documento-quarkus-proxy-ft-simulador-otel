# simtr-hub

Microsservico Quarkus que funciona como camada anticorrupcao entre consumidores internos e APIs
MTR de parametrizacao, dossie produto e gestao documental.

O codigo esta organizado como monolito modular DDD. Cada capacidade possui nucleo proprio,
portas de aplicacao e adapters independentes para REST publico, MTR e simulador. A visão atual e
as decisões vigentes estão em `doc/arquitetura-ddd-integracoes-atomicas.md` e `doc/adr/README.md`.

## Capacidades e endpoints

| Dominio | Capacidade | Endpoint publico preservado |
|---|---|---|
| `arvoredocumento` | `ConsultarProcessoParametrizado` | `GET /simtr-hub/v1/processo/identificador-negocial/{identificador}` |
| `conformidade` | `ConsultarChecklist` | `GET /simtr-hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}` |
| `dossieproduto` | `ConsultarDossieProduto` | `GET /simtr-hub/v1/dossie-produto/{id}` |
| `dossieproduto` | `ConsultarDocumentosDossieProduto` | `GET /simtr-hub/v1/dossie-produto/{id}/documentos` |
| `dossieproduto` | `CriarDossieProduto` | `POST /simtr-hub/v1/dossie-produto` |
| `dossieproduto` | `AtualizarFormularioDossieProduto` | `PATCH /simtr-hub/v1/dossie-produto/{id}/formulario` |
| `dossieproduto` | `IncluirDocumentoDossieProduto` | `POST /simtr-hub/v1/dossie-produto/{id}/documento` |
| `dossieproduto` | `RegistrarValidacaoNegocialDossieProduto` | `PATCH /simtr-hub/v1/dossie-produto/{id}/validacao-negocial` |
| `dossieproduto` | `AlterarProdutosContratadosDossieProduto` | `PATCH /simtr-hub/v1/dossie-produto/{id}/produto` |
| `dossieproduto` | `CapturarDossieProduto` | `POST /simtr-hub/v1/dossie-produto/{id}/capturar` |
| `dossieproduto` | `IniciarOuAvancarWorkflowDossieProduto` | `POST /simtr-hub/v1/dossie-produto/{id}/workflow` |
| `gestaodocumento` | `ObterCredencialContainer` | `POST /simtr-hub/v1/storage/container/credencial` |

Nao existe endpoint unico de pre-validacao nem orquestrador local. Em especial,
`gestaodocumento` apenas obtem e devolve a credencial opaca fornecida pelo MTR: o Hub nao envia
arquivos ao Azure, nao interpreta a validade, nao reutiliza ou renova SAS e nao mantem cache.

## Endpoints da especificacao que nao existem no Hub

A especificacao funcional `doc/api-integracao-mtr-pre-validacao-v1.md` descreve APIs do MTR, nao
somente as operacoes expostas por este Hub. As doze operacoes expostas estao implementadas e
aparecem na tabela anterior. Os dois endpoints MTR abaixo estao documentados na
especificacao, mas **NAO ESTAO IMPLEMENTADOS NESTA SOLUCAO**:

| Endpoint MTR de referencia | Situacao no Hub |
|---|---|
| `PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/garantia` | Nao existe endpoint publico, capacidade, REST Client, adapter ou simulador |
| `POST /simtr-dossie-produto/v1/dossie-produto/{id}/cancelar` | Nao existe endpoint publico, capacidade, REST Client, adapter ou simulador |

"Nao implementado no Hub" nao significa que a API upstream nao exista no MTR. Esses dois
endpoints aparecem na especificacao como operacoes do ciclo de vida do dossie a serem mantidas,
mas nao sao chamados pelo diagrama de sequencia principal e nao ganharam rota proxy nesta
solucao. Tambem nao existe endpoint unico de pre-validacao nem orquestrador local. Uma eventual
implementacao exige nova decisao de escopo, contrato, testes, fase e branch; esta documentacao nao
representa compromisso de entrega.

A especificacao identifica os servicos pelos prefixos `/simtr-parametrizacao`,
`/simtr-dossie-produto` e `/simtr-gestao-documento`. O ambiente atualmente configurado usa uma
base de gateway terminada em `/simtr`, somada aos paths `/parametrizacao`, `/dossie-produto` e
`/gestao-documento` declarados nos REST Clients.

## Arquitetura

```text
REST atual --------------------------+
                                     |
orquestrador futuro do dominio ------+--> porta de entrada
                                           -> caso de uso atomico
                                           -> porta de saida do consumidor
                                           -> adapter selecionado
                                              |-- MTR
                                              `-- simulador
```

As dependencias fluem para dentro. Essa direcao e aplicada de forma pragmatica: **Quarkus pode ser
usado em qualquer componente, inclusive dominio, aplicacao, portas e casos de uso, e nao e
bloqueado por camada**. Os guardrails protegem fronteiras e responsabilidades; eles nao exigem
pureza de framework.

- `dominio` concentra tipos e regras do proprio dominio e nao depende das bordas;
- `aplicacao` coordena tipos internos e portas e usa atualmente Mutiny `Uni`;
- o adapter REST converte DTO publico para tipos internos e chama somente a porta de entrada;
- os adapters MTR e simulador implementam a mesma porta de saida, com DTOs e mappers exclusivos;
- qualifiers e producers CDI selecionam MTR ou simulador pelas properties existentes;
- falhas de protocolo sao traduzidas depois da politica de fault tolerance e nao atravessam as
  portas;
- imports entre dominios sao reservados a futuros adapters locais ACL, limitados a API publica de
  entrada do dominio fornecedor.

Uma API especifica ainda pode ser confinada por seu papel — REST Clients, por exemplo, pertencem
ao adapter MTR. Isso restringe a responsabilidade do componente, nao o uso do Quarkus como
framework.

O dominio interno `parametrizacao` foi removido. O nome continua presente somente onde representa
o sistema externo: paths MTR, config keys, fixtures e sinais de telemetria contratuais.

### Organizacao de packages

```text
br.gov.caixa.simtr.hub
|-- arvoredocumento
|   |-- dominio
|   |-- aplicacao
|   `-- adaptador
|-- conformidade
|   |-- dominio
|   |-- aplicacao
|   `-- adaptador
|-- dossieproduto
|   |-- dominio
|   |-- aplicacao
|   `-- adaptador
|       `-- entrada/rest/v1
|-- gestaodocumento
|   |-- dominio
|   |-- aplicacao
|   `-- adaptador
`-- arquitetura
    |-- configuracao/mock
    |-- excecao
    |-- observabilidade
    `-- seguranca
```

A borda REST de `dossieproduto` reside no package canonico `adaptador/entrada/rest/v1`. O DTO
tecnico compartilhado de erro REST permanece em `arquitetura.excecao.dto` como unico desvio
interno documentado, com excecao formal e uso proibido no dominio, na aplicacao e nos adapters de
saida.

## Integracoes MTR

```http
GET /simtr/parametrizacao/v2/patriarca/processo/identificador-negocial/{identificador}
GET /simtr/parametrizacao/v1/cadastro/checklist/identificador-negocial/{identificador}/versao/{versao}
GET /simtr/dossie-produto/v2/dossie-produto/{id}
GET /simtr/dossie-produto/v4/dossie-produto/{id}/documentos
POST /simtr/dossie-produto/v1/dossie-produto
PATCH /simtr/dossie-produto/v1/dossie-produto/{id}/formulario
POST /simtr/dossie-produto/v2/dossie-produto/{id}/documento
PATCH /simtr/dossie-produto/v1/dossie-produto/{id}/validacao-negocial
PATCH /simtr/dossie-produto/v1/dossie-produto/{id}/produto
POST /simtr/dossie-produto/v1/dossie-produto/{id}/capturar
POST /simtr/dossie-produto/v1/dossie-produto/{id}/workflow
POST /simtr/gestao-documento/v1/storage/container/credencial
```

Os REST Clients permanecem na borda `adaptador.saida.mtr.client` e conservam as politicas
especificas de timeout, retry, circuit breaker, headers, OIDC, propagacao de trace e classificacao
de erros protegidas por testes. A captura envia um POST sem corpo, com timeout e circuit breaker,
mas sem retry automatico porque o contrato MTR nao comprova idempotencia. Seu provider de tracing
e registrado somente nesse client para preservar a correlacao e impedir que a URL interna completa
seja publicada pelo span HTTP automatico.

A consulta de documentos usa o GET v4 idempotente e encaminha somente os query params opcionais
explicitamente recebidos. Ela aplica timeout, retry apenas para falhas transitorias e circuit
breaker. Um provider exclusivo desse client suprime o span HTTP automatico, que poderia publicar a
query string, e reinjeta o contexto OpenTelemetry; o caminho MTR conserva exatamente um span
CLIENT proprio.

`CapturarDossieProduto` e `ConsultarDocumentosDossieProduto` usam portas de saida intercambiaveis
para MTR e simulador. Producers CDI selecionam os adapters pela property existente
`simtr-hub.simulador.dossie-produto.habilitado`; no modo simulador, cada capacidade usa DTO, mapper
e fixture proprios, sem chamada de rede. A consulta de documentos entrega o cenario deterministico
do identificador `4081899` e nao reproduz a engine de filtros/projecoes do MTR.

## Configuracao

As URLs podem ser sobrescritas pelas variaveis abaixo:

```text
QUARKUS_REST_CLIENT_PARAMETRIZACAO_PROCESSO_URL
QUARKUS_REST_CLIENT_PARAMETRIZACAO_CHECKLIST_URL
QUARKUS_REST_CLIENT_DOSSIE_PRODUTO_URL
QUARKUS_REST_CLIENT_GESTAO_DOCUMENTO_URL
```

Credenciais sao recebidas somente por ambiente:

```text
SIMTR_API_KEY
SIMTR_OIDC_CLIENT_SECRET
SIMTR_OIDC_INTERNET_CLIENT_SECRET
```

### Simulador

```properties
simtr-hub.simulador.parametrizacao-processo.habilitado=false
simtr-hub.simulador.parametrizacao-checklist.habilitado=false
simtr-hub.simulador.dossie-produto.habilitado=false
simtr-hub.simulador.gestao-documento.habilitado=false
```

O profile `dev` habilita os quatro simuladores pelas properties correspondentes. O profile de
teste nao depende de Docker nem de Dev Services.

## Erros, fault tolerance e observabilidade

O fluxo de falha e:

```text
REST Client MTR
  -> erro de protocolo classificado dentro da chamada interceptada
  -> timeout/retry/circuit breaker
  -> adapter MTR traduz para falha interna lossless
  -> adapter REST traduz para status e JSON publicos preservados
```

Logs estruturados e spans possuem nomes e atributos protegidos por testes. Payloads de
REST Client sao mascarados para campos sensiveis; a obtencao de credencial nao registra SAS nem
validade. Por padrao, o projeto grava logs JSON e nao exporta OpenTelemetry para fora.

Consulte `doc/catalogo-observabilidade.md` para os sinais contratuais completos.

## Execucao local

```bash
mvn quarkus:dev -Ddebug=false
```

- Swagger UI: `http://localhost:8080/simtr-hub/doc`
- OpenAPI gerado pelo Quarkus: `http://localhost:8080/simtr-hub/openapi`

O OpenAPI nao possui arquivo estatico nem filtro. Os testes protegem o comportamento HTTP e os
contratos Java que alimentam a geracao, sem inspecionar o documento gerado pelo Quarkus.

## Testes e cobertura

```bash
mvn -q clean test
```

A suite usa stubs HTTP locais para exercitar o caminho MTR sem rede externa. A evidencia
quantitativa de cobertura fica exclusivamente em:

```text
target/jacoco-report/index.html
```

## Documentacao

- decisao arquitetural canonica: `doc/arquitetura-ddd-integracoes-atomicas.md`;
- indice de decisoes arquiteturais: `doc/adr/README.md`;
- especificacao funcional de referencia: `doc/api-integracao-mtr-pre-validacao-v1.md`;
- planejamento de novas features: `tasks/README.md`;
- observabilidade e operacao: `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`;
- catalogo de sinais: `doc/catalogo-observabilidade.md`.

Quarkus Flow, novos workflows, persistencia de orquestracao, os dois endpoints ausentes listados
acima, quaisquer outros endpoints novos, upload e lifecycle de SAS nao estao implementados e
exigem feature, plano e GO proprios.
