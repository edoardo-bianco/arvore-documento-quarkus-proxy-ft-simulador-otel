# simtr-hub

Microsservico Quarkus que funciona como camada anticorrupcao entre consumidores internos e APIs
MTR de parametrizacao, dossie produto e gestao documental.

O codigo esta organizado como monolito modular DDD. Cada capacidade possui nucleo proprio,
portas de aplicacao e adapters independentes para REST publico, MTR e simulador. A refatoracao das
oito capacidades existentes e seu aceite humano final foram concluidos no checkpoint C6 de
`tasks/todo.md`.

## Capacidades e endpoints

| Dominio | Capacidade | Endpoint publico preservado |
|---|---|---|
| `arvoredocumento` | `ConsultarProcessoParametrizado` | `GET /simtr-hub/v1/processo/identificador-negocial/{identificador}` |
| `conformidade` | `ConsultarChecklist` | `GET /simtr-hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}` |
| `dossieproduto` | `CriarDossieProduto` | `POST /simtr-hub/v1/dossie-produto` |
| `dossieproduto` | `AtualizarFormularioDossieProduto` | `PATCH /simtr-hub/v1/dossie-produto/{id}/formulario` |
| `dossieproduto` | `IncluirDocumentoDossieProduto` | `POST /simtr-hub/v1/dossie-produto/{id}/documento` |
| `dossieproduto` | `RegistrarValidacaoNegocialDossieProduto` | `PATCH /simtr-hub/v1/dossie-produto/{id}/validacao-negocial` |
| `dossieproduto` | `IniciarOuAvancarWorkflowDossieProduto` | `POST /simtr-hub/v1/dossie-produto/{id}/workflow` |
| `gestaodocumento` | `ObterCredencialContainer` | `POST /simtr-hub/v1/storage/container/credencial` |

Nao foram adicionados endpoints nem orquestradores. Em especial, `gestaodocumento` apenas obtem e
devolve a credencial opaca fornecida pelo MTR: o Hub nao envia arquivos ao Azure, nao interpreta a
validade, nao reutiliza ou renova SAS e nao mantem cache.

## Endpoints da especificacao que nao existem no Hub

A especificacao funcional `doc/api-integracao-mtr-pre-validacao-v1.md` descreve APIs do MTR, nao
somente as operacoes expostas por este Hub. As oito operacoes usadas pelo fluxo principal estao
implementadas e aparecem na tabela anterior. Os cinco endpoints MTR abaixo estao documentados na
especificacao, mas **NAO ESTAO IMPLEMENTADOS NESTA SOLUCAO**:

| Endpoint MTR de referencia | Situacao no Hub |
|---|---|
| `PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/garantia` | Nao existe endpoint publico, capacidade, REST Client, adapter ou simulador |
| `PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/produto` | Nao existe endpoint publico, capacidade, REST Client, adapter ou simulador |
| `POST /simtr-dossie-produto/v1/dossie-produto/{id}/capturar` | Nao existe endpoint publico, capacidade, REST Client, adapter ou simulador |
| `POST /simtr-dossie-produto/v1/dossie-produto/{id}/cancelar` | Nao existe endpoint publico, capacidade, REST Client, adapter ou simulador |
| `GET /simtr-dossie-produto/v2/dossie-produto/{id}` | Nao existe endpoint publico, capacidade, REST Client, adapter ou simulador |

"Nao implementado no Hub" nao significa que a API upstream nao exista no MTR. Esses cinco
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

As dependencias fluem para dentro:

- `dominio` usa Java e tipos do proprio dominio;
- `aplicacao` usa tipos internos, portas e Mutiny `Uni`;
- o adapter REST converte DTO publico para tipos internos e chama somente a porta de entrada;
- os adapters MTR e simulador implementam a mesma porta de saida, com DTOs e mappers exclusivos;
- qualifiers e producers CDI selecionam MTR ou simulador pelas properties existentes;
- falhas de protocolo sao traduzidas depois da politica de fault tolerance e nao atravessam as
  portas;
- imports entre dominios sao reservados a futuros adapters locais ACL, limitados a API publica de
  entrada do dominio fornecedor.

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
|   |-- adaptador
|   `-- recurso/rest/v1
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

Dois desvios internos foram preservados para nao misturar renomes amplos com a migracao funcional:

- a borda REST de `dossieproduto` permanece fisicamente em `recurso/rest/v1`, mas e tratada pelos
  guardrails como adapter de entrada;
- o DTO tecnico compartilhado de erro REST permanece em `arquitetura.excecao.dto`, com excecao
  formal e uso proibido no dominio, na aplicacao e nos adapters de saida.

## Integracoes MTR

```http
GET /simtr/parametrizacao/v2/patriarca/processo/identificador-negocial/{identificador}
GET /simtr/parametrizacao/v1/cadastro/checklist/identificador-negocial/{identificador}/versao/{versao}
POST /simtr/dossie-produto/v1/dossie-produto
PATCH /simtr/dossie-produto/v1/dossie-produto/{id}/formulario
POST /simtr/dossie-produto/v2/dossie-produto/{id}/documento
PATCH /simtr/dossie-produto/v1/dossie-produto/{id}/validacao-negocial
POST /simtr/dossie-produto/v1/dossie-produto/{id}/workflow
POST /simtr/gestao-documento/v1/storage/container/credencial
```

Os REST Clients permanecem na borda `adaptador.saida.mtr.client` e conservam timeout, retry,
circuit breaker, headers, OIDC, propagacao de trace e classificacao de erros observados no
baseline. Operacoes mutaveis com retry continuam exigindo validacao de idempotencia antes de
qualquer workflow futuro.

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

Logs estruturados e spans mantem os nomes e atributos inventariados antes da migracao. Payloads de
REST Client sao mascarados para campos sensiveis; a obtencao de credencial nao registra SAS nem
validade. Por padrao, o projeto grava logs JSON e nao exporta OpenTelemetry para fora.

Consulte `tasks/inventario-observabilidade.md` para os sinais contratuais completos.

## Execucao local

```bash
mvn quarkus:dev -Ddebug=false
```

- Swagger UI: `http://localhost:8080/simtr-hub/doc`
- OpenAPI gerado pelo Quarkus: `http://localhost:8080/simtr-hub/openapi`

O OpenAPI nao possui arquivo estatico, filtro ou teste de documento nesta refatoracao.

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
- especificacao funcional de referencia: `doc/api-integracao-mtr-pre-validacao-v1.md`;
- verificacao final por capacidade: `tasks/equivalencia-final.md`;
- plano e checkpoint: `tasks/plan.md` e `tasks/todo.md`;
- observabilidade e operacao: `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`.

Quarkus Flow, novos workflows, persistencia de orquestracao, os cinco endpoints ausentes listados
acima, quaisquer outros endpoints novos, upload e lifecycle de SAS permanecem fora deste escopo.
