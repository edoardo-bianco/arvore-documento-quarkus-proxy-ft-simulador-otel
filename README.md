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
| `conformidade` | `IniciarAnaliseConformidade` | `POST /simtr-hub/v1/conformidade/analises` |
| `conformidade` | `ConsultarAnaliseConformidade` | `GET /simtr-hub/v1/conformidade/analises/{instanceId}` |
| `conformidade` | `RevisarAnaliseConformidade` | `PUT /simtr-hub/v1/conformidade/analises/{instanceId}/revisao` |
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

Nao existe endpoint unico de pre-validacao nem orquestrador local que componha as capacidades MTR.
A análise de conformidade é uma PoC orquestrada por Quarkus Flow e não altera as doze capacidades
atômicas de integração com o MTR. Em especial, `gestaodocumento` apenas obtém e devolve a
credencial opaca fornecida pelo MTR: o Hub não envia arquivos ao Azure, não interpreta a validade,
não reutiliza ou renova SAS e não mantém cache.

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
solucao. Tambem nao existe endpoint único de pré-validação nem orquestrador local que componha
essas operações MTR do dossiê. Uma eventual implementação exige nova decisão de escopo, contrato,
testes, fase e branch; esta documentação não representa compromisso de entrega.

A especificacao identifica os servicos pelos prefixos `/simtr-parametrizacao`,
`/simtr-dossie-produto` e `/simtr-gestao-documento`. O ambiente atualmente configurado usa uma
base de gateway terminada em `/simtr`, somada aos paths `/parametrizacao`, `/dossie-produto` e
`/gestao-documento` declarados nos REST Clients.

## Arquitetura

```text
REST atomico ---------------------------> porta de entrada
                                            -> caso de uso atomico
                                            -> porta de saida do consumidor
                                            -> adapter selecionado
                                               |-- MTR
                                               `-- simulador

REST da PoC -> casos de uso de conformidade -> Quarkus Flow
                |                               |-- checkpoint Redis/Valkey
                |                               `-- agente Ollama
                `-- porta documental neutra
                    |-- CouchDB em DES/local
                    `-- Cosmos DB for NoSQL em PRD
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

O profile `dev` habilita os quatro simuladores pelas properties correspondentes. A suíte padrão
não usa Compose Dev Services nem Ollama/Cosmos reais, mas testes de persistência iniciam containers
efêmeros e isolados de CouchDB/Valkey quando necessário.

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

## PoC de conformidade durável

A página da PoC fica em `http://localhost:8080/poc-conformidade/`. Ela usa os três endpoints de
conformidade, consulta o estado a cada 1.500 ms e não grava `localStorage` nem `sessionStorage`.
Durante todo o fluxo exibe como somente leitura `correlationId`, `instanceId`,
`identificadorDocumento`, `identificadorChecklist` e `versaoChecklist`.

Os dados de negócio ficam no backend documental selecionado: CouchDB no ambiente local e Azure
Cosmos DB for NoSQL em PRD. Redis/Valkey guarda somente checkpoints técnicos do Flow. Solicitação
de revisão e conclusão usam CloudEvents referenciais e feed nativo do backend, sem Kafka ou outro
broker. A entrega é pelo menos uma vez; IDs determinísticos, documentos imutáveis e validação da
correlação tornam o reprocessamento idempotente.

### Desenvolvimento com Quarkus

O `quarkus:dev` inicia o CouchDB `3.5.2` definido em `compose-devservices.yml` e preserva seu
volume entre reinícios. Defina `COUCHDB_USERNAME`, `COUCHDB_PASSWORD` e `SIMTR_API_KEY` somente no
ambiente, mantenha um Redis/Valkey acessível por `QUARKUS_REDIS_HOSTS` e inicie previamente o
Ollama com o modelo configurado, por padrão `llama3.2:3b`.

```powershell
mvn quarkus:dev -Ddebug=false
```

### Uma réplica em containers

Copie o exemplo para um arquivo ignorado pelo Git, preencha as três credenciais e confirme que o
modelo configurado já existe no Ollama do host:

```powershell
Copy-Item poc-containers.env.example .env.poc
docker compose --env-file .env.poc -f compose-poc.yml up --build -d
docker compose --env-file .env.poc -f compose-poc.yml ps
```

O Compose publica aplicação, CouchDB e Valkey somente em `127.0.0.1`. O preflight
`ollama-check` impede o início da aplicação quando o Ollama do host ou o modelo não está
disponível. Para provar que uma instância sobrevive ao restart apenas da aplicação, mantenha
CouchDB e Valkey ativos:

```powershell
docker compose --env-file .env.poc -f compose-poc.yml restart simtr-hub
```

O encerramento comum preserva o volume nomeado do CouchDB:

```powershell
docker compose --env-file .env.poc -f compose-poc.yml down
```

Não use `down -v` se precisar recuperar os documentos. O Valkey deste Compose não possui volume
nem persistência própria; recriá-lo perde checkpoints e fica fora da prova de restart da PoC.

A prova automatizada equivalente, com duas JVMs sequenciais e containers efêmeros, é:

```powershell
./validar-restart-conformidade.ps1
```

### Duas réplicas no kind

São pré-requisitos Docker, Maven, `kubectl`, `kind` no `PATH` ou em `.tools/kind.exe`, além do
Ollama/modelo no host. Prepare o Secret local e execute o roteiro de identidade durável:

```powershell
Copy-Item poc-kubernetes.env.example .env.poc-kubernetes
./validar-poc-kubernetes.ps1
./validar-failover-poc-kubernetes.ps1
```

O primeiro script cria ou reutiliza o cluster `simtr-hub-poc`, empacota e carrega a imagem, aplica
`k8s/poc`, valida duas Leases de membro, readiness e rolling restart. O segundo direciona
POST/PUT/GET a pods diferentes, substitui o owner antes e depois da revisão e exige conclusão
única. Os roteiros deixam o cluster ativo para inspeção; removê-lo com
`kind delete cluster --name simtr-hub-poc` também elimina o PVC e todos os dados locais do cluster.

Os manifests reservam 100 mCPU/256 MiB para cada pod da aplicação, 50 mCPU/128 MiB para CouchDB e
25 mCPU/32 MiB para Valkey; os limites de memória são, respectivamente, 768 MiB, 512 MiB e 128 MiB.
O PVC do CouchDB possui 1 GiB.

### Replay e limites operacionais

- o feed CouchDB mantém o cursor no documento local `_local/simtr-flow-revisao-v1`; sem cursor,
  inicia em `0`, e com cursor retoma da sequência persistida;
- o Change Feed do Cosmos usa container de leases e prefixo próprio; sem lease, lê desde o início;
- cursores e leases são estado técnico interno: não há comando público de reset e eles não devem
  ser editados manualmente; duplicatas de replay são esperadas e tratadas por idempotência;
- a página aceita texto de até 20.000 caracteres, enquanto o modelo local usa contexto
  `num-ctx=2048`; não há truncamento ou particionamento silencioso;
- timeout/retry/circuit breaker do adapter Ollama podem levar a chamada a aproximadamente 181 s
  antes do fallback técnico completo para revisão humana;
- CouchDB é uma única réplica e Valkey é compartilhado e não persistente nos ambientes da PoC;
  perda, restart e alta disponibilidade desses backends não foram comprovados;
- o acesso cross-pod e o failover do owner foram comprovados somente com CouchDB, Valkey e Ollama
  continuamente disponíveis;
- o adapter Cosmos passou em contrato determinístico com SDK mockado; integração real contra
  Emulator ou conta não produtiva continua gate obrigatório antes de PRD;
- o profile `%poc` desabilita OIDC somente nos ambientes locais empacotados e habilita logs
  completos do LangChain4j; use exclusivamente dados sintéticos e nunca credenciais ou documentos
  reais nesse profile.

## Testes e cobertura

```bash
mvn -q clean test
```

A suíte usa stubs HTTP locais para exercitar o caminho MTR sem rede externa e containers efêmeros
para os contratos documentais necessários. Ollama real, Cosmos real e provas de restart entre
processos permanecem gates opt-in. A evidência quantitativa de cobertura fica exclusivamente em:

```text
target/jacoco-report/index.html
```

## Documentacao

- decisao arquitetural canonica: `doc/arquitetura-ddd-integracoes-atomicas.md`;
- indice de decisoes arquiteturais: `doc/adr/README.md`;
- guia para implementar nova capacidade integrada ao MTR:
  `doc/guias/guia-implementacao-nova-capacidade-mtr.md`;
- especificacao funcional de referencia: `doc/api-integracao-mtr-pre-validacao-v1.md`;
- planejamento de novas features: `tasks/README.md`;
- observabilidade e operacao: `doc/documentacao-simtr-hub-arquitetura-observabilidade.md`;
- catalogo de sinais: `doc/catalogo-observabilidade.md`.

O workflow de conformidade descrito acima está implementado. Os dois endpoints ausentes listados,
quaisquer outros endpoints ou workflows, upload e lifecycle de SAS continuam fora do escopo e
exigem feature, plano e GO próprios.
