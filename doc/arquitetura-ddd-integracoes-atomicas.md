# Arquitetura DDD para integracoes atomicas do simtr-hub

## Status e autoridade

- **Status:** aceito
- **Data:** 2026-07-11
- **Estado verificado:** 2026-07-15, depois da conclusao da refatoracao e do inventario de endpoints
- **Escopo:** refatoracao do comportamento atualmente implementado
- **Plano executavel:** `../tasks/plan.md`
- **Checklist de retomada:** `../tasks/todo.md`

Este e o documento arquitetural canonico para a refatoracao. Agentes e pessoas devem le-lo antes
de alterar packages, portas, DTOs, adapters, tratamento de erros ou testes relacionados.

## Visao da solucao

O `simtr-hub` e um monolito modular organizado por dominios de negocio. A solucao implementada
oferece oito capacidades atomicas por endpoints REST publicos e integra cada capacidade ao MTR ou
ao simulador por adapters de saida independentes. Cada dominio concentra seus casos de uso,
modelos internos e contratos de aplicacao; detalhes de HTTP, serializacao, CDI, fault tolerance e
telemetria permanecem nas bordas.

O fluxo executado atualmente pode ser lido assim:

```text
cliente HTTP
    -> adapter REST de entrada
        -> porta de entrada
            -> caso de uso atomico
                -> porta de saida
                    -> adapter selecionado por configuracao
                        |-- MTR
                        `-- simulador
```

Nao existe nesta solucao um endpoint unico de pre-validacao, orquestrador local, motor de workflow
ou comunicacao distribuida entre os dominios. A composicao por orquestradores e a extracao para
microsservicos sao possibilidades futuras, explicitamente separadas do estado implementado. A
secao de cobertura frente a especificacao identifica os oito endpoints existentes e os cinco que
ainda nao existem no Hub.

## Fundamentos de organizacao

### Organizacao por dominio

Ha duas formas comuns de agrupar o codigo: por tecnologia, reunindo Resources, services, clients e
mappers de toda a solucao, ou por dominio, reunindo os artefatos que realizam uma capacidade de
negocio. O `simtr-hub` adota **package by domain**.

Assim, `arvoredocumento`, `conformidade`, `dossieproduto` e `gestaodocumento` possuem fronteiras
proprias. Os nomes dos sistemas externos e das tecnologias aparecem nas bordas quando fazem parte
do contrato, mas nao definem um dominio interno compartilhado. Essa organizacao reduz o
acoplamento entre capacidades e torna explicito quem e responsavel por cada operacao.

### Aplicacao e infraestrutura

O codigo de aplicacao expressa as capacidades e coordena os casos de uso com seus modelos
internos. O codigo de infraestrutura implementa os mecanismos necessarios para executa-los, como
endpoints REST, chamadas HTTP ao MTR, leitura de fixtures do simulador, serializacao,
autenticacao, resiliencia e observabilidade.

Separar essas responsabilidades permite testar o nucleo sem acionar infraestrutura, trocar o
mecanismo de integracao com impacto concentrado e diagnosticar problemas na fronteira correta. Na
solucao atual, essa separacao nao elimina Quarkus das bordas; ela impede que detalhes do framework
e dos contratos externos se tornem dependencias do dominio.

## Portas e Adaptadores no `simtr-hub`

A Arquitetura Hexagonal, ou Portas e Adaptadores, e o padrao tatico usado para proteger o interior
de cada dominio. Portas sao contratos orientados a capacidades e nao dependem de tecnologia:

- uma **porta de entrada** expressa uma capacidade oferecida pela aplicacao e e acionada pelo
  adapter REST atual;
- uma **porta de saida** expressa uma necessidade externa do caso de uso, sem expor protocolo,
  fornecedor ou DTO de integracao;
- um **adapter de entrada** traduz o contrato REST publico para os tipos internos;
- um **adapter de saida** implementa a necessidade externa e traduz os tipos internos para o
  contrato do MTR ou do simulador.

As dependencias apontam das bordas para as portas e os modelos internos. O caso de uso nao conhece
Resource, REST Client, URL, DTO MTR, fixture ou mecanismo de selecao CDI; adapters de tipos
diferentes tambem nao dependem diretamente uns dos outros.

Esse padrao organiza o interior do Hub, mas nao define por si so a arquitetura distribuida do
sistema. O DDD estabelece responsabilidades e limites entre dominios; REST, mensageria, futuros
orquestradores ou uma eventual extracao em microsservicos exigem decisoes arquiteturais proprias.
A aplicacao do padrao e pragmatica: nao se criam portas, adapters ou pastas sem uma capacidade e um
consumidor reais.

## Intencao confirmada

Transformar o `simtr-hub` em um monolito modular DDD, composto por capacidades atomicas e
desacopladas. Os endpoints REST atuais e os futuros orquestradores devem ser entradas diferentes
para os mesmos casos de uso. O nucleo nao dependera de Quarkus Flow e nao pressupora que ele sera
adotado.

A refatoracao deve preparar a futura extracao dos modulos em microsservicos sem implementar agora
novos endpoints, workflows ou integracoes funcionais.

## Resultado esperado

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

- REST e orquestradores nao conhecem REST Client, DTO MTR, URL ou politica HTTP.
- Um orquestrador do mesmo dominio pode compor capacidades atomicas desse dominio.
- Uma chamada entre dominios usa uma porta de saida do consumidor e uma camada anticorrupcao.
- Enquanto os dominios estiverem no mesmo processo, a camada anticorrupcao pode usar um adapter
  local. Depois da extracao, somente esse adapter muda para HTTP ou mensageria.

## Dominios e capacidades atuais

### `arvoredocumento`

E dono da capacidade atomica de consultar o processo parametrizado usado no calculo futuro da
arvore documental.

Capacidade existente:

- `ConsultarProcessoParametrizado`

O path REST atual deve ser preservado. O package interno `parametrizacao` deixa de representar um
dominio. O nome `parametrizacao` pode continuar aparecendo somente na borda de integracao com o
sistema MTR correspondente.

O calculo da arvore, os servicos de IA e a orquestracao da arvore nao fazem parte desta etapa.

### `conformidade`

E dono da capacidade atomica de consultar um checklist por identificador e versao.

Capacidade existente:

- `ConsultarChecklist`

Essa capacidade nao compartilha um grande modelo de parametrizacao com `arvoredocumento`. Cada
dominio possui seu contrato de aplicacao, modelos internos e mapeamento do contrato externo.

A analise de documentos e a orquestracao de conformidade nao fazem parte desta etapa.

### `dossieproduto`

E dono das capacidades atomicas necessarias ao ciclo de vida do dossie no MTR. Nesta etapa nao
existem aggregate, maquina de estados ou sequenciamento inventados.

Capacidades existentes:

- `CriarDossieProduto`
- `AtualizarFormularioDossieProduto`
- `IncluirDocumentoDossieProduto`
- `RegistrarValidacaoNegocialDossieProduto`
- `IniciarOuAvancarWorkflowDossieProduto`

Endpoints adicionais descritos em documentos funcionais, mas nao implementados, nao entraram na
refatoracao. A secao seguinte identifica cada um explicitamente; sua eventual implementacao exige
decisao, contrato, fase e capacidade atomica proprios.

### `gestaodocumento`

E uma capacidade transversal que obtem do MTR a credencial de acesso ao container documental.

Capacidade existente:

- `ObterCredencialContainer`

O `simtr-hub` nao envia arquivos ao Azure, nao reutiliza a SAS, nao mantem cache da credencial e
nao fixa sua duracao no codigo. O microsservico consumidor e responsavel por reutilizar a
credencial ate a expiracao informada pelo contrato e solicitar outra quando necessario.

### `prevalidacao` futura

O futuro dominio de pre-validacao sera dono do processo associado ao espelho da proposta do
usuario. Ele coordenara outros dominios por suas portas, mas esta fora do escopo inicial.

## Cobertura frente a especificacao de pre-validacao

A fonte funcional de comparacao e `api-integracao-mtr-pre-validacao-v1.md`. Os paths da coluna
"Endpoint MTR da especificacao" identificam APIs do sistema upstream; eles nao sao rotas publicas
do Hub. Neste inventario:

- **IMPLEMENTADO** significa que existem no Hub endpoint publico, porta/caso de uso, adapter MTR e
  adapter de simulador para a operacao;
- **NAO IMPLEMENTADO** significa que o Hub nao possui Resource, rota publica, capacidade, REST
  Client, adapter nem simulador para a operacao. Isso nao afirma que a API upstream do MTR nao
  exista.

| Endpoint MTR da especificacao | Estado no Hub | Endpoint publico do Hub |
|---|---|---|
| `GET /simtr-parametrizacao/v2/patriarca/processo/identificador-negocial/{identificador}` | **IMPLEMENTADO** | `GET /simtr-hub/v1/processo/identificador-negocial/{identificador}` |
| `GET /simtr-parametrizacao/v1/cadastro/checklist/identificador-negocial/{identificador}/versao/{versao}` | **IMPLEMENTADO** | `GET /simtr-hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}` |
| `POST /simtr-dossie-produto/v1/dossie-produto` | **IMPLEMENTADO** | `POST /simtr-hub/v1/dossie-produto` |
| `PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/formulario` | **IMPLEMENTADO** | `PATCH /simtr-hub/v1/dossie-produto/{id}/formulario` |
| `POST /simtr-dossie-produto/v2/dossie-produto/{id}/documento` | **IMPLEMENTADO** | `POST /simtr-hub/v1/dossie-produto/{id}/documento` |
| `PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/validacao-negocial` | **IMPLEMENTADO** | `PATCH /simtr-hub/v1/dossie-produto/{id}/validacao-negocial` |
| `POST /simtr-dossie-produto/v1/dossie-produto/{id}/workflow` | **IMPLEMENTADO** | `POST /simtr-hub/v1/dossie-produto/{id}/workflow` |
| `POST /simtr-gestao-documento/v1/storage/container/credencial` | **IMPLEMENTADO** | `POST /simtr-hub/v1/storage/container/credencial` |
| `PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/garantia` | **NAO IMPLEMENTADO** | Nao existe |
| `PATCH /simtr-dossie-produto/v1/dossie-produto/{id}/produto` | **NAO IMPLEMENTADO** | Nao existe |
| `POST /simtr-dossie-produto/v1/dossie-produto/{id}/capturar` | **NAO IMPLEMENTADO** | Nao existe |
| `POST /simtr-dossie-produto/v1/dossie-produto/{id}/cancelar` | **NAO IMPLEMENTADO** | Nao existe |
| `GET /simtr-dossie-produto/v2/dossie-produto/{id}` | **NAO IMPLEMENTADO** | Nao existe |

Os cinco endpoints ausentes aparecem na especificacao como operacoes existentes do ciclo de vida
do dossie que deveriam ser mantidas inalteradas; eles nao aparecem nas duas sequencias principais
descritas. As oito operacoes dessas sequencias existem como capacidades atomicas, sem
orquestracao: nao existe endpoint unico de pre-validacao nem orquestrador local nesta solucao.

A especificacao usa os nomes de servico `/simtr-parametrizacao`, `/simtr-dossie-produto` e
`/simtr-gestao-documento`. Na configuracao atual do Hub, o gateway fornece uma base terminada em
`/simtr` e os REST Clients acrescentam, respectivamente, `/parametrizacao`, `/dossie-produto` e
`/gestao-documento`. Essa diferenca de montagem nao muda a operacao nem sua versao funcional.

## Decisao sobre `parametrizacao`

`parametrizacao` nao permanece como dominio interno porque descreve o sistema upstream, e nao uma
responsabilidade de negocio do Hub.

- A consulta de processo pertence a `arvoredocumento`.
- A consulta de checklist pertence a `conformidade`.
- Os dois podem integrar com APIs do MTR chamadas parametrizacao.
- DTOs, mapeadores e modelos nao sao compartilhados automaticamente entre os dois consumidores.
- Duplicacao pequena na borda e preferivel a um acoplamento que impeca a extracao futura.

## Camadas por dominio

Estrutura alvo, adaptada apenas quando uma capacidade realmente precisar de cada pasta:

```text
<dominio>/
|-- dominio/
|   |-- modelo/
|   `-- erro/
|-- aplicacao/
|   |-- porta/
|   |   |-- entrada/
|   |   `-- saida/
|   `-- casodeuso/
`-- adaptador/
    |-- entrada/
    |   `-- rest/v1/
    |       |-- dto/
    |       |-- mapper/
    |       `-- resource/
    `-- saida/
        |-- mtr/
        |   |-- dto/<versao>/<operacao>/
        |   |-- mapper/
        |   |-- client/
        |   |-- erro/
        |   `-- adapter/
        `-- simulador/
            |-- dto/
            |-- mapper/
            `-- adapter/
```

Nao se criam pastas vazias nem abstracoes sem consumidor real.

## Regras de dependencia

1. `dominio` usa somente Java e outros tipos do proprio dominio.
2. `dominio` nao importa Quarkus, Mutiny, CDI, Jackson, Jakarta, REST, OpenTelemetry ou adapters.
3. `aplicacao` pode usar Mutiny `Uni`, tipos do proprio dominio e suas portas.
4. `aplicacao` nao importa `adaptador`, DTO, REST Client ou outro dominio.
5. Adapters de entrada dependem de portas de entrada e mapeiam DTOs para tipos internos.
6. Adapters de saida implementam portas de saida e mapeiam tipos internos para seus contratos.
7. DTO publico, DTO MTR e DTO do simulador sao contratos independentes.
8. Nenhum mapper converte diretamente DTO publico em DTO MTR.
9. `arquitetura` nao importa dominios e nao possui regra, modelo ou erro especifico de negocio.
10. Um adapter local entre dominios fica na borda do consumidor; ele pode importar a API de
    aplicacao publica do fornecedor e realiza a traducao entre modelos.

A **API publica de aplicacao** de um dominio e formada exclusivamente por suas portas de entrada e
pelos tipos semanticos de comando/resultado referenciados por elas. Casos de uso concretos, portas
de saida e demais tipos internos nao fazem parte dessa API. A aplicacao de um dominio consumidor
nao importa diretamente nem mesmo essa API: somente seu adapter local anticorrupcao pode importa-la.

Essas regras estao codificadas no build por ArchUnit para todas as capacidades migradas. A
ativacao ocorreu progressivamente durante a refatoracao para evitar um big bang.

## Portas e granularidade

- Uma porta de entrada por capacidade atomica.
- Casos de uso expressam verbos de negocio e retornam tipos internos.
- Portas de saida representam a necessidade do consumidor, nao a API generica do fornecedor.
- Um mesmo adapter MTR pode implementar varias portas pequenas do mesmo dominio.
- Nao existe `Service` generico que exponha todas as operacoes de todos os contextos.

Essa granularidade permite que o futuro orquestrador componha somente as capacidades necessarias e
impede que um consumer dependa de uma interface ampla por conveniencia.

## Modelos internos e nomes

- Records podem ser usados, mas um record nao e automaticamente um value object imutavel.
- Tipos internos recebem nomes semanticos: comandos, resultados, identificadores e conceitos do
  negocio.
- O sufixo generico `Vo` nao e obrigatorio e sera removido durante a migracao de cada capacidade.
- Comandos podem usar o sufixo `Command`; respostas internas devem representar o resultado, nao o
  protocolo que as transportou.
- Colecoes continuam com a semantica atual nesta refatoracao. Imutabilidade profunda e tratamento
  de elementos nulos so mudam mediante decisao funcional e testes proprios.

## Contratos das tres bordas

### REST publico

- DTOs de negocio ficam exclusivamente no adapter REST de entrada do proprio dominio.
- DTOs de topo usam nomes explicitos de request e response.
- Tipos aninhados usam nomes semanticos dentro do package da operacao.
- Paths, verbos, status, JSON e validacoes observaveis permanecem iguais.
- O OpenAPI e gerado exclusivamente pelo Quarkus a partir do codigo; nao existe arquivo estatico,
  filtro, complemento nem teste do documento gerado.
- A unica excecao compartilhada permitida e o contrato tecnico de erro REST, localizado em
  `arquitetura.excecao.dto`. Ele e proibido no dominio, na aplicacao e nos adapters MTR/simulador.

### MTR

- DTOs ficam exclusivamente no adapter MTR.
- Sao separados por versao e operacao quando o contrato externo puder evoluir de modo independente.
- REST Clients e annotations de fault tolerance ficam nessa borda.

### Simulador

- Implementa as mesmas portas de saida implementadas pelo adapter MTR.
- Trabalha com tipos internos nas portas.
- Quando le JSON, usa `JSON -> DTO do simulador -> modelo interno`.
- Nao reutiliza DTO publico nem DTO MTR.
- A selecao MTR/simulador usa qualifiers ou producer CDI explicitos; nao pode haver injecao
  ambigua nem roteador que se injete recursivamente.
- As propriedades atuais de habilitacao sao preservadas.

## Erros

- Excecoes e tipos HTTP nao atravessam portas.
- Cada dominio classifica falhas relevantes para seus casos de uso, como rejeicao de negocio,
  dependencia indisponivel, timeout e resposta externa invalida.
- O adapter MTR traduz erros externos para falhas internas da aplicacao.
- A classificacao protocolar usada por retry/circuit breaker ocorre dentro da chamada interceptada
  do REST Client; a traducao para falha interna ocorre somente depois que a politica de fault
  tolerance termina. Essa ordem preserva `retryOn`, `abortOn`, `failOn` e `skipOn` atuais.
- A traducao preserva, sem perdas, todos os dados necessarios para reproduzir o contrato publico
  atual: status, recurso, identificador, codigo, mensagens, detalhe e demais campos observaveis.
- O DTO de erro recebido do MTR e diferente do DTO de erro devolvido pela API publica.
- O adapter REST traduz falhas internas para o status e corpo atuais.
- Erros de validacao e desserializacao, que podem ocorrer antes do metodo Resource, continuam sob
  mappers tecnicos do adapter REST.

O contrato tecnico de erro REST compartilhado e a excecao formal a regra geral de DTO por dominio.
ArchUnit deve codificar o package permitido e proibir seu uso nas demais bordas, evitando allowlist
informal.

## Reatividade e chamadas bloqueantes

- `Uni` e permitido na camada de aplicacao para representar uma operacao assincrona com zero ou um
  resultado.
- Nenhum caso de uso chama `await`, bloqueia event loop ou cria thread por conta propria.
- Adapters bloqueantes devem deslocar a execucao para worker thread sem expor esse detalhe ao
  dominio.

## Fault tolerance e idempotencia

As politicas atuais de timeout, retry e circuit breaker devem ser preservadas durante a
refatoracao, inclusive sua classificacao de excecoes. Elas pertencem ao adapter MTR e nao sao
aplicadas automaticamente ao simulador.

Ha um risco conhecido: criacao de dossie, inclusao de documento e avanco de workflow sao operacoes
mutaveis com retry. Um timeout depois de sucesso remoto pode duplicar efeitos.

**Checkpoint bloqueante para workflows futuros:** antes de um orquestrador usar essas operacoes,
deve existir evidencia de idempotencia do MTR ou uma chave/estrategia idempotente. Essa correcao
comportamental nao sera misturada a refatoracao estrutural atual.

## Observabilidade

Nomes de spans, eventos de log e atributos atuais sao contrato de compatibilidade nesta etapa.

- Um inventario e testes de caracterizacao precedem renomes estruturais.
- A mudanca de nomes Java nao pode alterar silenciosamente atributos obtidos por reflexao do REST
  Client.
- Cada capacidade preserva os sinais atuais nas bordas REST, caso de uso e adapter MTR.
- Reducao de logs duplicados e uma mudanca posterior, com decisao e testes separados.

## Estrategia de testes

Foi usado TDD leve:

1. caracterizar o comportamento observavel que ainda nao possui teste;
2. provar que o teste protege a fronteira ou falha arquitetural pretendida;
3. migrar a menor fatia vertical possivel;
4. executar os testes focados;
5. executar a suite completa no checkpoint.

Os testes devem cobrir, conforme a capacidade:

- path, verbo, status e JSON publico;
- mensagens e caminhos de Jakarta Validation;
- mapeamento REST -> interno -> MTR e caminho inverso;
- JSON realmente enviado e recebido pelo REST Client;
- headers e providers relevantes;
- simulador habilitado e desabilitado;
- traducao de erros ponta a ponta;
- matriz de retry, circuit breaker e timeout sem alterar os valores atuais;
- nomes e atributos de observabilidade considerados contratuais;
- annotations e contratos Java que alimentam o OpenAPI gerado, sem testar ou manipular o artefato;
- propriedades, defaults e profiles de configuracao usados pela capacidade;
- regras ArchUnit das capacidades ja migradas.

O baseline anterior foi preservado por manifestos e contratos executaveis por capacidade. A
evidencia quantitativa de cobertura permanece exclusivamente no relatorio JaCoCo. Nenhum teste
preexistente foi removido, desabilitado ou substituido sem justificativa e GO humano.

## Migracao progressiva concluida

A unidade de migracao e uma capacidade vertical, nao um package inteiro.

Ordem executada:

1. guardrails e caracterizacao;
2. piloto `IniciarOuAvancarWorkflowDossieProduto`, por possuir contrato pequeno;
3. demais capacidades de `dossieproduto`, uma por vez;
4. `ConsultarProcessoParametrizado` para `arvoredocumento`;
5. `ConsultarChecklist` para `conformidade`;
6. `ObterCredencialContainer` em `gestaodocumento`;
7. remocao segura do antigo package `parametrizacao` e consolidacao final.

Cada checkpoint exigiu suite verde, build valido, regras arquiteturais da fatia ativas e revisao
do diff antes da continuidade.

## Fora de escopo

- os cinco endpoints marcados como **NAO IMPLEMENTADO** acima e quaisquer outros endpoints novos;
- implementacao de qualquer workflow ou orquestrador;
- escolha definitiva ou dependencia de Quarkus Flow;
- Redis, Cosmos DB ou persistencia de estado de workflow;
- calculo da arvore documental e servicos inteligentes de IA;
- analise de conformidade;
- upload para Azure Blob Storage;
- cache ou renovacao de SAS no Hub;
- mudanca de contratos publicos;
- correcao de idempotencia das operacoes MTR;
- redesenho funcional de nulabilidade ou imutabilidade profunda.

## Alternativas rejeitadas

### Manter `parametrizacao` como dominio compartilhado

Rejeitada porque acopla `arvoredocumento` e `conformidade` a um modelo grande definido pelo nome do
sistema externo, dificultando evolucao e extracao independentes.

### Orquestradores chamarem endpoints REST locais

Rejeitada porque adiciona protocolo, serializacao e falhas de rede dentro do mesmo processo. REST e
orquestradores sao entradas para os mesmos casos de uso.

### Reutilizar os mesmos DTOs em todas as bordas

Rejeitada porque transforma mudancas do MTR em mudancas da API publica e impede o nucleo de evoluir
independentemente.

### Criar desde ja aggregates e maquinas de estado

Rejeitada porque as regras completas dos fluxos nao fazem parte do escopo. Modelagem inventada
criaria falsa complexidade de dominio.

### Adicionar Quarkus Flow agora

Rejeitada nesta etapa. O nucleo deve permitir uma avaliacao futura, mas a decisao depende de spike,
criterios de operacao e maturidade dos providers de persistencia.

## Riscos conhecidos e controles

| Risco | Controle |
|---|---|
| Retry de operacao mutavel duplicar efeito | Preservar agora; bloquear workflow futuro ate validar idempotencia |
| Alterar erro publico ao separar DTOs | Modelo interno lossless e testes ponta a ponta |
| Quebrar simulador ao remover Jackson dos modelos | DTO e mapper exclusivos do simulador |
| Injecao CDI ambigua entre MTR, simulador e roteador | Qualifiers/producer explicitos e teste de bootstrap |
| Renome quebrar dashboards | Inventario e testes de spans, eventos e atributos |
| Testes validarem apenas simulador | Stub MTR com simulador desabilitado |
| Big bang de packages | Uma capacidade vertical por etapa e checkpoints GO/NO-GO |

## Fontes oficiais de framework

- Mutiny e `Uni`: https://quarkus.io/guides/mutiny-primer
- Quarkus REST reativo: https://quarkus.io/guides/rest
- SmallRye Fault Tolerance: https://quarkus.io/guides/smallrye-fault-tolerance
- Quarkus Flow: https://quarkus.io/extensions/io.quarkiverse.flow/quarkus-flow/
- Persistencia do Quarkus Flow: https://docs.quarkiverse.io/quarkus-flow/dev/persistence.html

Quarkus Flow `0.12.0` foi publicado para Quarkus `3.33.2.1`, versao atual do projeto, mas a propria
documentacao informa que os providers de persistencia ainda passam por testes e benchmarks de alto
throughput. Isso reforca que a adocao futura exige spike e nao faz parte desta refatoracao.
