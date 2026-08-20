# Guia de implementação manual de uma nova capacidade integrada ao MTR

## Objetivo

Este guia descreve a ordem recomendada para implementar uma capacidade atômica do `simtr-hub`
que chama um novo endpoint externo do MTR. O cenário completo considera uma rota REST pública,
uma porta de entrada, um caso de uso, uma porta de saída, adapters MTR e simulador, fixture de mock,
seleção CDI, observabilidade e testes.

Se a feature não precisar de uma nova rota pública, omita somente os passos da borda REST. Não
elimine as portas nem faça um consumidor interno chamar REST Client ou adapter diretamente.

Este documento é um roteiro reutilizável. Ele não autoriza nenhum endpoint específico e não
substitui o plano e o checklist obrigatórios em `tasks/features/<nome>/`.

## Fluxo que deve resultar da implementação

```text
cliente HTTP
    -> Resource e mapper REST
        -> porta de entrada
            -> caso de uso atômico
                -> porta de saída
                    -> adapter selecionado pelo producer CDI
                        |-- adapter MTR -> mapper MTR -> REST Client MTR
                        `-- adapter simulador -> mapper do simulador -> fixture Markdown
```

As dependências apontam para os tipos internos. REST, MTR e simulador são bordas independentes e
não compartilham DTOs ou mapeiam diretamente uma borda para outra.

### Papel de cada componente

| Componente | Papel | Pode conhecer |
|---|---|---|
| Adapter REST de entrada | Traduz HTTP/JSON para a aplicação | DTO REST, validação e porta de entrada |
| Porta de entrada | Expõe a capacidade em linguagem de negócio | Tipos internos e `Uni` |
| Caso de uso | Executa ou coordena a operação atômica | Porta de entrada, porta de saída e tipos internos |
| Porta de saída | Descreve a necessidade externa do consumidor | Tipos internos e `Uni` |
| Adapter MTR de saída | Traduz a necessidade para o fornecedor | Porta de saída, DTO/mapper/client MTR e erro protocolar |
| Adapter simulador de saída | Executa a mesma necessidade sem rede | Porta de saída, DTO/mapper do simulador e fixture |

A porta é uma interface voltada ao negócio; o adapter é a implementação de uma borda técnica. O
caso de uso implementa a porta de entrada e usa a porta de saída, sem selecionar qual adapter será
executado.

## Antes de alterar código

### 1. Abrir a feature e registrar o plano

1. Leia `doc/arquitetura-ddd-integracoes-atomicas.md` e `doc/adr/README.md`.
2. Leia integralmente os ADRs aplicáveis. Para este tipo de feature, normalmente são aplicáveis:
   ADR-0001, ADR-0002, ADR-0004, ADR-0005 e ADR-0006. Consulte também o ADR-0007 quando a operação
   for mutável, repetível, composta por workflow ou sujeita a retry.
3. Confirme o contrato MTR em fonte versionada e inspecione uma capacidade semelhante no código e
   nos testes. Documente qualquer divergência entre Swagger, especificação e comportamento real.
4. Crie uma branch curta, por exemplo `feature/<nome-da-capacidade>`.
5. Copie `tasks/templates/plan.md` e `tasks/templates/todo.md` para
   `tasks/features/<nome-da-capacidade>/`.
6. Preencha intenção, escopo, fora de escopo, critérios de aceitação, riscos, dependências,
   arquivos prováveis, verificações e checkpoints.
7. Registre no `todo.md` que o plano aguarda GO. Somente o usuário pode registrar o GO.

### 2. Congelar as decisões antes da implementação

O plano deve responder, sem deixar decisões implícitas:

| Dimensão | Decisão que deve estar registrada |
|---|---|
| Ownership | Domínio responsável e nome da capacidade em linguagem de negócio |
| Contrato público | Verbo, path, parâmetros, validações, JSON, status de sucesso e erros |
| Contrato MTR | Serviço, versão, verbo, path, headers, query, corpo, resposta e erros |
| Semântica interna | Comando/consulta, resultado e classificações de falha necessárias |
| Assincronicidade | Assinatura `Uni<T>` e ausência de bloqueio no caso de uso |
| Fault tolerance | Timeout, retry, circuit breaker, `retryOn`, `abortOn`, `failOn` e `skipOn` |
| Idempotência | Evidência de que uma repetição é segura ou decisão explícita de não repetir |
| Simulador | Property, cenários determinísticos, identificadores e resposta da fixture |
| Segurança | API key, OIDC, propagação, dados sensíveis e superfície de entrada |
| Observabilidade | Nomes de spans/eventos e allowlist de atributos seguros |

Uma nova capacidade normalmente exige checkpoints humanos adicionais de contrato, arquitetura e
comportamento observável. Segurança também exige checkpoint quando a feature altera autenticação,
autorização, credenciais, dados sensíveis ou superfície de entrada.

### 3. Inicializar o baseline SonarQube

Este passo acontece depois do GO e antes da primeira alteração de código, teste ou tooling.

1. Verifique se existem pacotes em `sonar/`.
2. Se existirem, pergunte ao usuário se o baseline usará somente o SonarQube Docker local, o
   servidor local mais um pacote offline específico ou exclusivamente um pacote offline.
3. Não solicite nem aceite token no chat. O processo deve herdar `SONAR_TOKEN` de uma sessão
   iniciada por `./iniciar-codex-com-sonar.ps1`.
4. Para baseline local, execute:

   ```powershell
   ./validar-checkpoint-sonarqube.ps1 -InitializeBaseline
   ```

5. Acrescente `-OfflineReportPath "./sonar/<pacote>"` somente quando essa fonte tiver sido
   autorizada. Para baseline exclusivamente offline, use também `-OfflineOnlyBaseline`.

Um baseline exclusivamente offline permanece `UNVERIFIED`; ele não comprova cobertura, duplicação,
issues atuais nem Quality Gate do servidor.

## Ordem de implementação

Em cada passo que altera comportamento, aplique RED -> GREEN -> REFACTOR: primeiro escreva o teste
que falha pelo motivo esperado, implemente apenas o necessário para fazê-lo passar e simplifique
sem mudar o comportamento.

### Passo 1 — Congelar o contrato público em RED

Quando houver nova rota pública, crie testes que descrevam antes da produção:

- verbo e path parametrizado;
- parâmetros de path/query e corpo JSON;
- validação Jakarta e mensagens aprovadas;
- status e corpo de sucesso, inclusive a diferença entre `200`, `201` e `204`;
- tradução dos erros internos para o contrato REST;
- annotations que alimentam o OpenAPI gerado pelo Quarkus.

O teste deve falhar porque a rota ainda não existe. Não crie snapshot estático do OpenAPI.

**Checkpoint:** obtenha aprovação humana do contrato antes de implementar a rota ou seus DTOs.

### Passo 2 — Criar os tipos semânticos internos

Crie no domínio somente os tipos exigidos pela capacidade:

- comando ou critérios de consulta;
- resultado de negócio, quando houver retorno;
- objetos internos aninhados realmente necessários;
- falha interna com classificações suficientes para o caso de uso e a borda pública.

Não coloque annotations, DTOs ou exceções de protocolo MTR nesses tipos. Preserve de forma
explícita a diferença entre `null`, coleção vazia e campo ausente quando isso fizer parte do
contrato.

**Teste:** prove construção, invariantes, valores opcionais, coleções, ordem e mensagem observável
segura da falha, quando aplicável.

### Passo 3 — Criar as portas e o caso de uso

Implemente nesta ordem, porque cada item depende do anterior:

1. porta de saída com o nome da necessidade do domínio, não do fornecedor;
2. porta de entrada com o nome da capacidade;
3. caso de uso que implementa a porta de entrada e depende somente da porta de saída.

Exemplo de responsabilidades:

```text
<Capacidade>                         porta de entrada
<Capacidade>CasoDeUso                implementação da porta de entrada
<NecessidadeDoDominio>               porta de saída
```

O caso de uso recebe tipos internos, devolve `Uni<T>`, não conhece Resource, REST Client, URL,
DTO MTR, fixture, property do simulador ou qualifier CDI. Não use `await`, não bloqueie o event loop
e não crie um `Service` genérico para concentrar operações independentes.

**Teste com porta falsa ou mock:** confirme que o mesmo comando/critério chega à porta de saída,
que o resultado é preservado e que a mesma falha é propagada. Esse é o primeiro “mock” da feature:
um test double da dependência do caso de uso.

### Checkpoint A — Núcleo isolado

- tipos internos representam somente a nova capacidade;
- porta de entrada e porta de saída usam linguagem de negócio;
- caso de uso passa nos testes sem Quarkus HTTP, MTR ou fixture;
- aplicação e domínio não importam tipos de nenhuma borda.

### Passo 4 — Criar DTO e mapper da borda MTR

Dentro de `adaptador/saida/mtr`, crie DTOs próprios para request, response e query. Separe-os por
versão e operação quando o contrato puder evoluir independentemente, por exemplo:

```text
adaptador/saida/mtr/dto/v<versao>/<operacao>/
```

Crie o mapper entre os tipos internos e os DTOs MTR. Nunca faça mapper direto de DTO REST para DTO
MTR. Use nomes JSON explícitos quando o wire exigir `snake_case` ou outro nome diferente do Java.

**Teste:** serialize e desserialize JSON representativo; prove nomes de campos, omissão de nulos,
listas vazias, ordem, query params opcionais e mapeamento completo da resposta.

### Passo 5 — Declarar REST Client, erro protocolar e fault tolerance

Crie ou estenda um REST Client somente dentro da borda MTR. Confira a composição entre a URL base
configurada, o `@Path` da interface e o `@Path` do método. Preserve os providers aprovados:

- `RequestHeaderFactory` para headers técnicos;
- filtro OIDC da borda;
- filtro/provider de observabilidade aplicável ao client;
- mapper de exceção do client para classificar o erro protocolar MTR.

Timeout, retry e circuit breaker pertencem à chamada interceptada do client. O erro deve continuar
protocolar durante essa política; a tradução para falha interna ocorre somente no adapter.

Não copie a política de outra operação sem justificar. Para uma operação mutável, a ausência de
evidência de idempotência deve impedir retry automático. Se for necessário suprimir um span HTTP
automático que exponha URL ou query sensível, use provider registrado somente no client afetado e
preserve a propagação do contexto; não altere o tracing global por conveniência.

**Teste do client:** prove verbo, path, tipos, annotations, providers, classificação dos status,
fallback para erro ausente/malformado e matriz completa de fault tolerance.

### Passo 6 — Implementar o adapter MTR

O adapter MTR deve:

1. implementar a porta de saída;
2. receber o REST Client e o mapper MTR;
3. mapear tipos internos para o wire;
4. chamar o client sem bloquear;
5. mapear a resposta para o resultado interno;
6. traduzir erro MTR, timeout e falhas transitórias para a falha interna depois da política do
   client;
7. registrar somente logs e atributos aprovados, sem payload, token, API key, URL interna,
   stacktrace externo ou dados sensíveis.

**Teste com client mockado:** capture os argumentos enviados ao client, prove o resultado e cubra
erro de negócio, erro técnico do cliente, indisponibilidade, timeout e falha inesperada. Preserve
lossless os dados necessários à resposta pública, mas não publique o DTO MTR.

### Checkpoint B — Borda MTR isolada

- DTO e mapper MTR são exclusivos da operação/versão;
- REST Client representa exatamente o contrato externo aprovado;
- política de retry está apoiada por idempotência ou está desabilitada;
- adapter implementa a porta e traduz erros somente depois do client;
- testes unitários da borda MTR estão verdes.

### Passo 7 — Criar o contrato e a fixture do simulador

O simulador implementa a mesma porta de saída, mas tem contrato próprio:

```text
src/main/java/.../<dominio>/adaptador/saida/simulador/dto/
src/main/java/.../<dominio>/adaptador/saida/simulador/mapper/
src/main/resources/mock/<dominio>/<operacao>.md
```

A fixture Markdown lida por `MarkdownJsonMockReader` deve conter um objeto JSON após o título
exato:

```markdown
## dados do mock corpo do retorno json
```

Use um DTO do simulador, inclusive quando a resposta for `{}`. Não reutilize DTO MTR ou REST. Se o
cenário feliz depender de um identificador, torne o valor descobrível no título ou em uma seção de
referência, sem substituir `{id}` na documentação do endpoint.

Defina explicitamente os cenários determinísticos: sucesso, identificador divergente, fixture
ausente, JSON inválido e campos obrigatórios ausentes. O simulador não recebe annotations de fault
tolerance e não faz chamada de rede.

**Teste:** leia a fixture real, prove o DTO e o mapper próprios, cubra os cenários de erro e confirme
que o adapter não declara timeout, retry, circuit breaker nem span CLIENT.

### Passo 8 — Selecionar MTR ou simulador por CDI

Crie qualifiers exclusivos da capacidade, por exemplo `<Operacao>Mtr` e
`<Operacao>Simulador`. Anote cada adapter com seu qualifier e crie um producer que exponha uma
única implementação não qualificada da porta de saída conforme a property aprovada.

Reutilize uma property existente somente quando ela tiver a semântica desejada. Não faça um
roteador injetar ou selecionar a si próprio.

**Testes:**

- unitário do producer com simulador ligado e desligado;
- bootstrap Quarkus que prove uma única porta resolvida, sem ambiguidade CDI;
- seleção do simulador com o MTR indisponível e zero requisições de rede.

### Passo 9 — Implementar a borda REST e a observabilidade de aplicação

Quando houver rota pública:

1. crie DTOs REST próprios com validações e nomes JSON do contrato público;
2. crie mapper REST -> tipo interno e resultado interno -> resposta REST;
3. traduza falha interna para as exceções/status públicos aprovados;
4. faça o Resource depender somente da porta de entrada;
5. declare verbo, path, `Consumes`, `Produces`, validações e respostas OpenAPI;
6. devolva o status e o corpo exatos, inclusive ausência de corpo em `204`;
7. registre o span SERVER e os eventos permitidos.

Siga o padrão local de uma classe de observabilidade que implementa a porta de entrada, envolve o
caso de uso e cria o span INTERNAL. Ela pode conhecer a flag do simulador para registrar a origem,
mas o caso de uso não pode conhecê-la.

**Testes:** mapper REST, validação, Resource Quarkus e caminho de erro. Confirme que a borda chama a
porta de entrada, não a porta de saída, o adapter MTR ou um endpoint REST local.

### Checkpoint C — Primeira fatia vertical

- a entrada aprovada -> porta de entrada -> caso de uso -> porta de saída funciona;
- MTR e simulador implementam a mesma porta e são selecionados sem ambiguidade;
- contratos REST, MTR e simulador permanecem independentes;
- testes focados do núcleo e das três bordas estão verdes;
- execute o checkpoint SonarQube se o incremento alterou o fingerprint executável.

### Passo 10 — Provar o wire MTR com stub HTTP local

Mockar o REST Client no teste do adapter não prova o contrato HTTP. Estenda o
`DossieProdutoMtrStubTestResource` ou o stub do domínio responsável e crie um teste Quarkus que
percorra a rota pública até o servidor local. Se não houver rota pública, acione a entrada de
aplicação aprovada e preserve o restante da prova de integração.

Capture e valide:

- método e path completos, incluindo a base `/simtr` configurada nos testes;
- query string e ausência de parâmetros não informados;
- `Content-Type`, `Accept`, API key, bearer e `traceparent`;
- corpo JSON exato ou corpo vazio;
- status e corpo devolvidos pelo Hub;
- número de chamadas para sucesso, erro não recuperável, retry, timeout e circuit breaker.

O teste deve usar somente localhost e não pode depender do MTR real, Docker ou Dev Services.

### Passo 11 — Provar o simulador ponta a ponta

Com a property do simulador habilitada e o endereço MTR indisponível, percorra a rota pública ou a
entrada de aplicação aprovada e prove:

- leitura da fixture própria;
- resposta pública esperada para sucesso e cenários determinísticos de falha;
- zero chamadas ao stub MTR;
- hierarquia de spans SERVER -> INTERNAL, sem CLIENT;
- origem `mock` sem payload ou dado sensível nos sinais.

Esse teste é diferente do stub HTTP: a fixture prova o modo simulado; o stub prova o wire MTR.

### Passo 12 — Atualizar contratos transversais

Revise e ajuste somente o necessário em:

- `ResourceBeanCoverageTest` para a nova operação pública;
- `ArchUnitProgressivoTest` para direção de dependências e separação das bordas;
- `ObservabilidadeSpansContratoTest` para nomes, hierarquia e atributos dos spans;
- `ObservabilidadeLogsContratoTest` para eventos e ausência de dados proibidos;
- properties e profiles, se a feature realmente exigir nova configuração.

Não altere nomes observáveis existentes junto com a feature. Qualquer mudança de log, span,
atributo, configuração, timeout, retry ou circuit breaker precisa do checkpoint humano de
comportamento observável.

### Passo 13 — Atualizar documentação fonte e verificar tudo

Depois de o comportamento estar estável:

1. atualize `README.md` e `doc/arquitetura-ddd-integracoes-atomicas.md` com a capacidade entregue;
2. atualize o ADR aplicável e seu índice somente quando o estado ou a decisão tiver mudado;
3. crie ADR novo como `Proposto` apenas se houver decisão arquitetural nova;
4. atualize o catálogo de observabilidade quando houver novos sinais;
5. atualize coleção de uso ou exemplo apenas se estiver no escopo aprovado;
6. não gere nem altere `.ppt`, `.pptx`, `.pdf` ou `.html` derivados de Markdown.

Execute testes focados após cada fatia, depois a suíte completa:

```powershell
mvn -q -Dtest="<CasoDeUsoTest>,<RestMapperTest>,<MtrMapperTest>" test
mvn -q -Dtest="<MtrAdapterTest>,<SimuladorAdapterTest>,<PortasProducerTest>" test
mvn -q -Dtest="<MtrContractTest>,<SelecaoSimuladorQuarkusTest>" test
mvn -q clean test
./validar-checkpoint-sonarqube.ps1
git diff --check
git status --short
```

Não execute análise completa a cada edição isolada; use-a depois de um incremento coerente. Se o
baseline for exclusivamente offline, execute os testes locais e informe que o checkpoint Sonar
atual permanece indisponível.

Se o checkpoint resultar em `NON_COMPLIANT`, apresente toda a evidência e aguarde o usuário escolher
`Reprovar`, `AceitarExcepcionalmente` ou `ContinuarAjustes`. Registre a decisão com o script oficial.

## Os quatro tipos de test double usados neste fluxo

| Tipo | Onde usar | O que prova | O que não prova |
|---|---|---|---|
| Porta falsa/mockada | Teste do caso de uso | Delegação, resultado e propagação de falha | HTTP, CDI ou JSON |
| REST Client mockado | Teste do adapter MTR | Mapeamento e tradução para falha interna | Wire, headers ou interceptors reais |
| Fixture Markdown | Adapter simulador | Cenário local determinístico sem rede | Compatibilidade com o MTR |
| Stub HTTP local | Teste de contrato Quarkus | Wire real, headers, FT e integração das camadas | Disponibilidade do ambiente MTR real |

Nenhum desses testes substitui os demais.

## Estrutura provável de arquivos

Adapte os nomes ao domínio e à operação; não crie arquivos sem responsabilidade real.

```text
src/main/java/.../<dominio>/
|-- dominio/
|   |-- modelo/<ComandoOuCriterios>.java
|   |-- modelo/<Resultado>.java
|   `-- erro/<FalhaDaCapacidade>.java
|-- aplicacao/
|   |-- porta/entrada/<Capacidade>.java
|   |-- porta/saida/<NecessidadeDoDominio>.java
|   `-- casodeuso/<Capacidade>CasoDeUso.java
`-- adaptador/
    |-- entrada/rest/v1/
    |   |-- dto/<RequestOuResponse>.java
    |   |-- <Operacao>RestMapper.java
    |   `-- <Dominio>Resource.java
    |-- configuracao/
    |   |-- qualificador/<Operacao>Mtr.java
    |   |-- qualificador/<Operacao>Simulador.java
    |   |-- <Operacao>PortasProducer.java
    |   `-- <Operacao>Observabilidade.java
    `-- saida/
        |-- mtr/
        |   |-- adapter/<Operacao>MtrAdapter.java
        |   |-- client/<Operacao>MtrClient.java
        |   |-- dto/v<versao>/<operacao>/...
        |   |-- erro/<Operacao>MtrException.java
        |   `-- mapper/<Operacao>MtrMapper.java
        `-- simulador/
            |-- adapter/<Operacao>SimuladorAdapter.java
            |-- dto/<Operacao>SimuladorResponse.java
            `-- mapper/<Operacao>SimuladorMapper.java

src/main/resources/mock/<dominio>/<operacao>.md
src/test/java/.../<dominio>/... testes correspondentes
tasks/features/<nome>/plan.md
tasks/features/<nome>/todo.md
```

## Referências concretas no repositório

Use os fluxos existentes como referência de estrutura, não como fonte automática de decisões:

- `AlterarProdutosContratadosDossieProduto`: operação com corpo, retorno vazio, DTOs independentes,
  producer CDI, fixture `{}` e teste de wire;
- `CapturarDossieProduto`: operação mutável sem corpo e sem retry automático por falta de prova de
  idempotência, com provider de tracing local;
- `ConsultarDocumentosDossieProduto`: consulta idempotente com query params opcionais, retry de
  falhas transitórias, resposta profunda, fixture determinística e provider local para proteger a
  query string.

Copie o formato, não a política. Verbo, status, retry, timeout, DTOs, validações, sinais e cenários
do simulador devem vir do contrato e do plano da nova feature.

## Checklist de conclusão

- [ ] Plano e checklist da feature existem e possuem GO humano registrado.
- [ ] Checkpoints de contrato, arquitetura, segurança e observabilidade aplicáveis foram aprovados.
- [ ] Contrato público e contrato MTR foram verificados separadamente.
- [ ] Tipos internos não importam DTOs ou exceções das bordas.
- [ ] Porta de entrada, caso de uso e porta de saída usam linguagem de negócio.
- [ ] Adapters MTR e simulador implementam a mesma porta de saída.
- [ ] DTOs e mappers REST, MTR e simulador são independentes.
- [ ] Política de fault tolerance possui testes e decisão explícita de idempotência.
- [ ] Simulador funciona sem rede e sua fixture é determinística e descobrível.
- [ ] Stub HTTP prova método, path, headers, payload/query, resposta e número de chamadas.
- [ ] Erros são traduzidos depois da política do client e preservam somente os dados necessários.
- [ ] Logs e traces não contêm credencial, token, payload, URL interna ou dado sensível.
- [ ] Testes focados, suíte completa, ArchUnit e verificações transversais passaram.
- [ ] Checkpoint SonarQube foi executado conforme o baseline disponível.
- [ ] Documentação fonte foi atualizada sem alterar formatos derivados.
- [ ] Diff final contém somente o escopo aprovado e aguarda encerramento humano.
