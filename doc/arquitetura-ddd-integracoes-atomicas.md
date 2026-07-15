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

## Fundamentos de organizacao

### Organizacao por dominio

Ha duas formas comuns de agrupar o codigo: por tecnologia, reunindo Resources, services, clients e
mappers de toda a solucao, ou por dominio, reunindo os artefatos que realizam uma capacidade de
negocio. O `simtr-hub` adota **package by domain**.

Assim, `arvoredocumento`, `conformidade`, `dossieproduto` e `gestaodocumento` possuem fronteiras
proprias. Os nomes dos sistemas externos e das tecnologias aparecem nas bordas quando fazem parte
do contrato, mas nao definem um dominio interno compartilhado. Essa organizacao reduz o
acoplamento entre capacidades e torna explicito quem e responsavel por cada operacao.

### Codigo da aplicacao e codigo de infraestrutura

O **codigo da aplicacao** representa a logica de negocio e as funcionalidades centrais do
software. Ele expressa capacidades, coordena casos de uso e trabalha com portas e modelos internos.

O **codigo de infraestrutura** implementa as tecnologias que apoiam essas funcionalidades. Em
termos gerais, pode incluir consultas SQL, requisicoes HTTP, chamadas AMQP, persistencia,
mensageria, armazenamento e integracoes externas. No `simtr-hub` atual, inclui principalmente os
endpoints REST, as chamadas HTTP ao MTR, a leitura de fixtures do simulador, serializacao,
autenticacao, fault tolerance e observabilidade.

Essa separacao aplica o principio de separacao de responsabilidades e contribui para:

- simplificar os testes;
- reduzir o acoplamento com tecnologias especificas;
- reutilizar a logica da aplicacao por entradas diferentes;
- substituir mecanismos de integracao com menor impacto;
- facilitar a manutencao e a identificacao de problemas.

Na solucao atual, essa separacao nao elimina Quarkus das bordas. Ela impede que detalhes do
framework e dos contratos externos se tornem dependencias do dominio e da logica de aplicacao.

## Arquitetura Hexagonal

A **Arquitetura Hexagonal**, tambem conhecida como **Portas e Adaptadores**, promove a separacao
entre o nucleo da aplicacao e os detalhes de infraestrutura.

```text
Adapter de entrada
        |
        v
Porta de entrada
        |
        v
Aplicacao / Dominio
        |
        v
Porta de saida
        |
        v
Adapter de saida
```

A regra central e que o nucleo conheca contratos orientados a capacidades, sem depender
diretamente de REST, banco de dados, mensageria, armazenamento, SDKs externos, simuladores ou
outras tecnologias. As dependencias de codigo apontam das bordas para as portas e os modelos do
nucleo.

### Portas

Uma **porta** e um contrato do codigo que nao depende de tecnologia.

- A **porta de entrada** representa uma capacidade oferecida pela aplicacao. Ela permite que uma
  requisicao HTTP, uma mensagem, uma tarefa agendada ou uma futura etapa de orquestracao acione um
  caso de uso.
- A **porta de saida** representa uma necessidade externa da aplicacao. Ela permite consultar ou
  persistir dados, chamar outro sistema ou publicar uma mensagem sem acoplar o caso de uso ao
  mecanismo utilizado.

As portas expressam intencoes e capacidades, nao o protocolo ou o fornecedor que as implementa.

### Adaptadores

Um **adapter** conecta uma porta a um mecanismo especifico. Adapters de entrada traduzem uma
interacao externa para uma porta de entrada. Adapters de saida implementam portas de saida e
traduzem os tipos internos para os contratos exigidos pela infraestrutura.

Adapters dependem das portas. O nucleo nao depende dos adapters, e adapters de tipos diferentes
nao dependem diretamente uns dos outros.

### Direcao da dependencia e papel do orquestrador

O papel arquitetural de um orquestrador depende da direcao da interacao; o nome
"orquestrador" sozinho nao determina sua camada.

Quando um **orquestrador externo inicia uma operacao**, ele e um ator de entrada. O protocolo e
tratado por um adapter de entrada, que aciona a porta da aplicacao:

```text
Orquestrador externo
        -> adapter de entrada (REST ou mensagem)
            -> porta de entrada
                -> aplicacao
```

Quando existe um **orquestrador local que coordena casos de uso**, essa coordenacao pertence a
camada de aplicacao. Ele permanece atras de sua propria porta de entrada, compoe capacidades do
mesmo dominio por seus contratos de entrada e atravessa outro dominio somente por uma porta de
saida do consumidor e uma camada anticorrupcao. Ele nao conhece Resources, REST Clients ou
adapters:

```text
Adapter de entrada
        -> porta de entrada do fluxo
            -> orquestrador da aplicacao (futuro)
                |-> porta de entrada de capacidade atomica do mesmo dominio
                `-> porta de saida do consumidor
                    -> adapter / camada anticorrupcao
                        -> outro dominio
```

Quando a aplicacao precisa acionar um **orquestrador externo como dependencia**, a interacao
pertence ao lado de saida:

```text
Aplicacao
    -> porta de saida
        -> adapter de saida
            -> orquestrador externo
```

Esses tres desenhos explicam possibilidades arquiteturais, nao componentes existentes. O
`simtr-hub` atual nao possui orquestrador local, motor de workflow nem adapter dedicado a um
orquestrador externo.

### Arquitetura Hexagonal no mundo dos microsservicos

Embora tenha "arquitetura" no nome, a Arquitetura Hexagonal e adotada neste documento como um
**padrao tatico para estruturar o codigo e os componentes dentro de um servico especifico**.

Ela nao define a visao de alto nivel, nao determina como servicos devem se comunicar e nao
estabelece a estrutura de um sistema composto por multiplos servicos. Comunicacao por REST,
mensageria ou outro mecanismo pertence ao desenho arquitetural do sistema.

No `simtr-hub`, o padrao hexagonal organiza o interior dos dominios e das capacidades. O DDD define
os limites de negocio e as formas de colaboracao; uma eventual extracao para microsservicos exige
decisoes proprias de contratos, comunicacao, consistencia e operacao.

### Uso pragmatico

A adocao deve considerar os beneficios obtidos e o custo das abstracoes. O padrao nao e uma
solucao universal e nao justifica componentes sem uso real. Portas, adapters e pastas so devem
existir quando houver uma capacidade e um consumidor concretos.

## Visao da solucao

O `simtr-hub` e um monolito modular organizado por dominios de negocio. A solucao implementada
oferece oito capacidades atomicas por endpoints REST publicos e integra cada capacidade ao MTR ou
ao simulador por adapters de saida independentes. Cada dominio concentra seus casos de uso,
modelos internos e contratos de aplicacao; os detalhes tecnologicos permanecem nas bordas.

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

No fluxo atual, a entrada e REST e as duas saidas alternativas sao MTR e simulador. O caso de uso
nao conhece Resource, REST Client, URL, DTO MTR, fixture nem o mecanismo de selecao CDI.

Nao existe nesta solucao um endpoint unico de pre-validacao, orquestrador local, motor de workflow
ou comunicacao distribuida entre os dominios. A composicao por orquestradores e a extracao para
microsservicos sao possibilidades futuras, explicitamente separadas do estado implementado. A
secao de cobertura frente a especificacao identifica os oito endpoints existentes e os cinco que
ainda nao existem no Hub.

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
