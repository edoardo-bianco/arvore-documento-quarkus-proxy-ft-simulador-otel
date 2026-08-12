# Checklist: consultar dossiê de produto por identificador

## Estado

- **Branch:** `feature/obter-dossie-produto`
- **Escopo:** feature aceita e encerrada pelo usuário
- **Próximo item:** nenhum — fluxo da feature concluído
- **Sonar nesta sessão:** checkpoint final `COMPLIANT`; zero issues novas ou severas e nenhuma decisão pendente
- **Alteração preexistente preservada:** `.tools/` não rastreado e fora do escopo

## Checklist

- [x] 0.1 Confirmar intenção e critérios de sucesso a partir do pedido;
- [x] 0.2 Ler arquitetura consolidada e índice de ADRs;
- [x] 0.3 Ler ADR-0002, ADR-0004, ADR-0005 e ADR-0006;
- [x] 0.4 Inspecionar Swagger 2.20.0.8 e especificação complementar;
- [x] 0.5 Inspecionar código, contratos e testes de GET, MTR v2 e simulador análogos;
- [x] 0.6 Registrar plano, divergências, riscos, verificações e checkpoints;
- [x] C0 Registrar GO humano antes da primeira alteração de produção ou teste executável;
- [x] C1 Aprovar contrato, arquitetura, segurança, observabilidade e fault tolerance propostos;
- [x] 1.1 Inicializar baseline SonarQube pelo modo escolhido pelo usuário;
- [x] 2.1 Congelar contrato público em RED;
- [x] 3.1 Criar modelo de leitura e falha interna;
- [x] 4.1 Criar portas e caso de uso atômico;
- [x] CA Verificar núcleo isolado e manter RED público explicado;
- [x] 5.1 Criar DTO e mapper MTR v2;
- [x] 6.1 Declarar REST Client e erro protocolar;
- [x] 7.1 Implementar adapter MTR e validação mínima da resposta;
- [x] CB Verificar borda MTR isolada e matriz de fault tolerance;
- [x] 8.1 Criar fixture, DTO e mapper próprios do simulador;
- [x] 9.1 Implementar adapter simulador e `404` para fixture ausente;
- [x] 10.1 Selecionar MTR/simulador por producer CDI;
- [x] 11.1 Criar DTO e mapper REST de resposta;
- [x] 12.1 Expor GET no Resource e instrumentar API/aplicação;
- [x] C2 Validar a fatia vertical, suíte completa, ArchUnit e checkpoint SonarQube;
- [x] 13.1 Provar wire MTR e seleção ponta a ponta;
- [x] 14.1 Congelar contratos transversais de logs e spans;
- [x] C3 Validar integração/observabilidade e checkpoint SonarQube;
- [x] 15.1 Atualizar README, arquitetura, ADR-0002 e índice;
- [x] 16.1 Atualizar catálogo de observabilidade; Postman será atualizado manualmente pelo responsável;
- [x] S1 Eliminar cinco issues LOW `java:S1185` sem alterar comportamento;
- [x] C4 Verificar documentação sem gerar derivados;
- [x] 17.1 Executar validação final e revisão integral do diff;
- [x] CF Registrar aceitação e encerramento humanos da feature.

## Critérios globais de conclusão

- [x] `GET /simtr-hub/v1/dossie-produto/{id}` responde o contrato público aprovado;
- [x] `id <= 0` responde `400` pelo contrato de validação existente;
- [x] modo MTR chama o wire v2 exato com headers e correlação;
- [x] modo simulador retorna fixture própria sem rede e sem PII real;
- [x] falhas `404`, `4xx`, `5xx` e timeout são classificadas e traduzidas sem perda;
- [x] nenhuma resposta externa nula ou com identificador divergente é publicada como sucesso;
- [x] DTOs e mappers das três bordas permanecem independentes;
- [x] logs/spans não contêm payload, CPF, CNPJ, nome, matrícula, token, API key ou URL interna;
- [x] testes focados, suíte completa e ArchUnit passam;
- [x] checkpoint SonarQube atual não possui decisão humana pendente;
- [x] README, arquitetura, ADR e catálogo refletem o estado implementado; Postman permanece fora
  do escopo automatizado por decisão do responsável;
- [x] nenhum formato derivado ou arquivo fora do escopo é alterado.

## Decisões humanas

| Checkpoint | Status | Data | Evidência/decisão necessária | Aprovador |
|---|---|---|---|---|
| C0 | APROVADO | 2026-08-12 | Usuário registrou `GO C0 e C1` | usuário |
| C1 | APROVADO | 2026-08-12 | Usuário aprovou path público, JSON/nulos/datas, segurança/fixture sintética, desenho arquitetural, telemetria e FT pelo GO conjunto | usuário |
| C2 | APROVADO | 2026-08-12 | Usuário registrou `GO C2`; suíte e checkpoint SonarQube concluíram conformes após `ContinuarAjustes` | usuário |
| C3 | APROVADO | 2026-08-12 | Usuário registrou `GO C3`; integração, observabilidade, suíte completa e SonarQube concluíram conformes | usuário |
| C4 | APROVADO | 2026-08-12 | Usuário autorizou com `c4`; documentação consistente, sem Postman ou formatos derivados alterados | usuário |
| CF | APROVADO | 2026-08-12 | Usuário registrou `cf` após validação técnica e manual completas | usuário |

## Evidências do planejamento

- 2026-08-12 — branch `feature/obter-dossie-produto` criada antes do registro documental;
- 2026-08-12 — arquitetura e índice de ADRs lidos; ADR-0002/0004/0005/0006 selecionados e lidos;
- 2026-08-12 — Swagger 2.20.0.8 confirma GET v2, `id int64`, sucesso `200`, resposta aninhada e
  erros `400/401/403/404/500`;
- 2026-08-12 — especificação complementar confirma datas textuais e o mesmo corpo lógico;
- 2026-08-12 — divergência `Calendar`/objeto versus data string registrada, sem correção silenciosa;
- 2026-08-12 — exemplo contém identidade possivelmente real; plano exige fixture sintética até
  autorização explícita de segurança;
- 2026-08-12 — padrões reais de `ConsultarChecklist`, `IncluirDocumentoDossieProduto`, testes de
  contrato, stub MTR, CDI, observabilidade e ArchUnit inspecionados;
- 2026-08-12 — `.tools/` identificado como conteúdo preexistente e mantido fora do escopo;
- 2026-08-12 — `sonar/`, Maven e SonarQube não foram inspecionados/executados porque esta sessão é
  exclusivamente documental.
- 2026-08-12 — usuário registrou explicitamente `GO C0 e C1`; contrato, arquitetura, segurança,
  observabilidade e fault tolerance propostos foram aprovados. Nenhuma alteração executável foi
  iniciada antes desse registro.
- 2026-08-12 — Task 1.1 concluída com baseline `LOCAL_SONAR` em estado `READY`; avaliação técnica
  `COMPLIANT`, 219 issues no baseline, cobertura de 87,8%, duplicação de 3,7% e nenhuma violação.
  O `clean verify`, o SonarScanner e o Compute Engine concluíram com sucesso; decisão humana
  adicional não foi exigida (`NOT_REQUIRED`).
- 2026-08-12 — Task 2.1 concluída em RED com
  `mvn -q "-Dtest=DossieProdutoApiContractTest,DossieProdutoErroApiContractTest,ResourceEndpointTest" test`:
  Quarkus iniciou normalmente e executou 46 testes; somente os quatro cenários novos falharam,
  todos com `404` por ausência de `GET /simtr-hub/v1/dossie-produto/{id}`. O sucesso e o smoke
  esperavam `200`; os identificadores `0` e `-1` esperavam `400`. Não houve erro de bootstrap,
  compilação ou acesso de rede. O contrato `200` preserva chaves nulas, listas vazias, datas como
  texto e usa `cpf = "00000000000"` e `nome = "CLIENTE SIMULADO"`.
- 2026-08-12 — Task 3.1 executada em dois ciclos RED -> GREEN. O primeiro RED falhou na
  compilação somente pela ausência de `FalhaConsultaDossieProduto`; após a implementação mínima,
  falha e ArchUnit ficaram verdes. O segundo RED falhou somente pela ausência de
  `DossieProdutoConsultado`; após criar o record e seus cinco tipos aninhados, o comando
  `mvn -q "-Dtest=FalhasDossieProdutoTest,ArchUnitProgressivoTest" test` concluiu com 42 testes
  verdes (9 de domínio e 33 de arquitetura), sem falhas, erros ou testes ignorados. O modelo usa
  apenas tipos Java/domínio, mantém datas como `String`, aceita nulos e preserva listas vazias e
  elementos nulos. A falha classifica `NEGOCIO`, `TECNICA_CLIENTE`,
  `DEPENDENCIA_INDISPONIVEL` e `TIMEOUT`. O contrato HTTP da Task 2 permanece RED pelo endpoint
  ausente; suíte completa e checkpoint SonarQube continuam previstos para C2.
- 2026-08-12 — Task 4.1 executada em RED -> GREEN. O RED falhou na compilação somente pela
  ausência das novas portas e do caso de uso. Foram adicionadas `ConsultarDossieProduto`,
  `ObterDossieProduto` e `ConsultarDossieProdutoCasoDeUso`, todos usando apenas
  `IdentificadorDossieProduto`, `DossieProdutoConsultado` e `Uni`. O comando
  `mvn -q "-Dtest=ConsultarDossieProdutoCasoDeUsoTest,ArchUnitProgressivoTest" test` concluiu com
  36 testes verdes (3 do caso de uso e 33 de arquitetura), sem falhas, erros ou testes ignorados.
  Os testes provam uma única delegação, identidade do identificador e propagação sem transformação
  de item, item nulo e falha. O caso de uso não bloqueia, valida, mapeia ou adiciona efeitos
  colaterais. O contrato HTTP continua RED e suíte completa/checkpoint SonarQube permanecem
  previstos para C2.
- 2026-08-12 — Checkpoint A concluído. O comando
  `mvn -q "-Dtest=FalhasDossieProdutoTest,ConsultarDossieProdutoCasoDeUsoTest,ArchUnitProgressivoTest" test`
  executou 45 testes verdes (9 de domínio, 3 do caso de uso e 33 de arquitetura), sem falhas,
  erros ou testes ignorados. A reexecução do contrato público executou 46 testes: os 42 cenários
  existentes passaram e somente os quatro novos falharam, todos com `404` pela ausência da rota
  ainda não implementada; o Quarkus iniciou normalmente e não houve falha de infraestrutura ou
  rede. A revisão de correção, simplicidade/legibilidade, arquitetura, segurança e desempenho foi
  aprovada sem apontamentos: o modelo é um record passivo de leitura, não um agregado nem contrato
  compartilhado; o núcleo depende somente de tipos internos e `Uni`, não bloqueia e não registra
  PII. O RED público permanece intencional; suíte completa e checkpoint SonarQube seguem previstos
  para C2.
- 2026-08-12 — Task 5.1 executada em RED -> GREEN. O RED falhou na compilação somente pela
  ausência do pacote e do DTO `ConsultaDossieProdutoMtrResponse`. Foram adicionados o DTO próprio
  da borda MTR v2 e `ConsultaDossieProdutoMtrMapper`, sem dependência das bordas REST ou simulador.
  Todos os componentes possuem `@JsonProperty` explícito; `data_criacao`, `processo.data`,
  `fase_atual.data` e `situacao_atual.data` permanecem `String`. O teste desserializa o contrato
  `snake_case` com identidade sintética, cobre `processo.data` ausente e prova a preservação de
  campos nulos, listas vazias, ordem, elementos nulos e todos os objetos aninhados. Resposta MTR
  nula continua sendo devolvida como `null` pelo mapper para validação posterior no adapter. O
  comando
  `mvn -q "-Dtest=ConsultaDossieProdutoMtrMapperTest,ArchUnitProgressivoTest" test` concluiu com
  37 testes verdes (4 do mapper e 33 de arquitetura), sem falhas, erros ou testes ignorados. REST
  Client, erro protocolar, adapter, suíte completa e checkpoint SonarQube permanecem nas etapas
  previstas; a borda pública não foi alterada e continua em RED. A revisão de segurança localizou
  um CPF literal preexistente em `TestFixtures.java`, fora do diff deste item; o dado não foi
  reutilizado nem alterado, e o risco foi registrado no plano para tratamento em demanda própria.
- 2026-08-12 — Task 6.1 executada incrementalmente em RED -> GREEN. O primeiro RED falhou na
  compilação somente pela ausência de `ConsultaDossieProdutoMtrException`; depois da hierarquia
  protocolar mínima, o segundo RED falhou somente pela ausência de
  `ConsultaDossieProdutoMtrClient`. O client usa o config key `dossie-produto`, base
  `/dossie-produto`, `GET /v2/dossie-produto/{id}`, `Accept: application/json`, retorno
  `Uni<ConsultaDossieProdutoMtrResponse>` e nenhum `@Consumes` ou parâmetro de corpo. Permanecem
  registrados `RequestHeaderFactory`, `OidcClientRequestReactiveFilter` e
  `RestClientObservabilityFilter`. A hierarquia preserva status e corpo MTR; classifica
  `400/404/409/422` como negócio, demais `4xx` como técnica cliente e `5xx` como servidor, além de
  normalizar somente campos protocolares ausentes e aplicar o fallback existente para corpo
  malformado. Timeout de 2.000 ms, até 3 retries com atraso de 300 ms/jitter de 100 ms e circuit
  breaker `10/0,5/10.000 ms/2` coincidem com C1. O comando focado
  `mvn -q "-Dtest=ConsultaDossieProdutoMtrClientTest" test` concluiu com 7 testes verdes; a
  execução separada de `ArchUnitProgressivoTest` concluiu com 33 testes verdes, sem falhas, erros
  ou testes ignorados. Adapter, validação mínima da resposta e wire HTTP real permanecem nas
  etapas 7.1 e 13.1; a borda pública não foi alterada e continua em RED. Suíte completa e
  checkpoint SonarQube seguem previstos para C2.
- 2026-08-12 — Task 7.1 executada em RED -> GREEN. O RED falhou na compilação somente pela
  ausência do qualifier `ConsultaDossieProdutoMtr` e de `ConsultaDossieProdutoMtrAdapter`. O
  adapter implementa `ObterDossieProduto`, possui qualifier MTR exclusivo, injeta o REST Client
  com `@RestClient` e declara span CLIENT `mtr.dossie-produto.consultar`. Respostas externas
  nulas, sem `id`, com identificador divergente ou solicitações sem identificador são rejeitadas
  antes do mapper como falha de dependência; resposta íntegra é convertida pelo mapper dedicado.
  Erros MTR de negócio, técnica cliente e servidor são traduzidos sem perda de status, recurso,
  identificadores, código, mensagens com elementos nulos, detalhe, stacktrace externo e causa;
  timeout e falha inesperada são classificados separadamente após a chamada do client. Os sinais
  registram somente identificador, contagens, resultado e tipo de erro, usando path templated e
  descrição estática; não registram payload, mensagem/stacktrace externo, CPF, CNPJ, nome ou
  matrícula. O comando
  `mvn -q "-Dtest=ConsultaDossieProdutoMtrAdapterTest,ArchUnitProgressivoTest" test` concluiu com
  41 testes verdes (8 do adapter e 33 de arquitetura), sem falhas, erros ou testes ignorados. A
  revisão de correção, simplicidade, arquitetura, segurança, desempenho e escopo não encontrou
  apontamentos; o wire HTTP real permanece para 13.1, a borda pública continua em RED e suíte
  completa/checkpoint SonarQube seguem previstos para C2.
- 2026-08-12 — Checkpoint B concluído. O comando
  `mvn -q "-Dtest=ConsultaDossieProdutoMtrMapperTest,ConsultaDossieProdutoMtrClientTest,ConsultaDossieProdutoMtrAdapterTest,FalhasDossieProdutoTest,ArchUnitProgressivoTest" test`
  executou 61 testes verdes (4 do mapper, 7 do client, 8 do adapter, 9 de falhas e 33 de
  arquitetura), sem falhas, erros ou testes ignorados. A inspeção reflexiva confirmou o wire GET,
  os providers existentes e a matriz C1 exata: timeout de 2.000 ms; até 3 retries com atraso de
  300 ms e jitter de 100 ms somente para erro servidor, `ProcessingException` e
  `TimeoutException`; abort para erro de negócio e técnica cliente; circuit breaker com volume
  10, razão 0,5, atraso de 10.000 ms e limiar de 2 sucessos, usando os mesmos grupos de
  falha/skip. A reexecução do contrato público executou 46 testes: 42 cenários existentes passaram
  e exatamente os quatro novos falharam, todos com `404` pela ausência deliberada do Resource e
  da seleção ainda não implementados; não houve erro de compilação, bootstrap ou rede. A revisão
  nos eixos de correção, simplicidade/legibilidade, arquitetura, segurança, desempenho e escopo
  foi aprovada sem apontamentos: DTO e mapper permanecem próprios da borda MTR, a tradução de
  falhas ocorre depois do client interceptado, a resposta externa é validada antes do mapper, os
  sinais não expõem payload/PII/segredos e não há bloqueio ou dependência nova. Suíte completa e
  checkpoint SonarQube permanecem previstos para C2.
- 2026-08-12 — Task 8.1 executada incrementalmente em RED -> GREEN. O primeiro RED falhou na
  compilação somente pela ausência de `ConsultaDossieProdutoSimuladorResponse`; após criar o DTO
  próprio, o segundo RED falhou somente pela ausência de `ConsultaDossieProdutoSimuladorMapper`.
  Foram adicionados o mapper dedicado e a fixture
  `mock/dossieproduto/4324680-v2-consulta-dossie-produto.md`, lida pelo
  `MarkdownJsonMockReader` real. O mock documenta
  `GET /simtr-dossie-produto/v2/dossie-produto/{id}`, preserva os campos, nulos, lista vazia e
  datas textuais aprovados, e substitui a identidade original por `cpf = "00000000000"` e
  `nome = "CLIENTE SIMULADO"`. O comando
  `mvn -q "-Dtest=ConsultaDossieProdutoSimuladorMapperTest,ArchUnitProgressivoTest" test`
  concluiu com 37 testes verdes (4 do mapper/fixture e 33 de arquitetura), sem falhas, erros ou
  testes ignorados. As buscas de isolamento não encontraram imports das bordas MTR/REST nem
  annotations de fault tolerance nos novos tipos; as buscas de segurança não encontraram a
  identidade original ou segredos. A revisão de correção, simplicidade, arquitetura, segurança,
  desempenho e escopo não encontrou apontamentos: o mapper é linear, preserva ordem e elementos
  nulos e converte somente `simulador -> modelo interno`. Adapter, qualifier e seleção CDI não
  foram criados; a borda pública não foi alterada e continua em RED. Suíte completa e checkpoint
  SonarQube permanecem previstos para C2.
- 2026-08-12 — Task 9.1 executada incrementalmente em RED -> GREEN. O primeiro RED falhou na
  compilação somente pela ausência do qualifier `ConsultaDossieProdutoSimulador`; depois de
  criá-lo, o segundo RED falhou somente pela ausência de
  `ConsultaDossieProdutoSimuladorAdapter`. O adapter implementa `ObterDossieProduto`, possui
  qualifier exclusivo e deriva o recurso
  `mock/dossieproduto/{id}-v2-consulta-dossie-produto.md` diretamente do identificador solicitado.
  `4324680` lê a fixture real e devolve a identidade sintética aprovada; identificador sem fixture
  produz `FalhaConsultaDossieProduto` de negócio com status `404`, sem consultar a fixture padrão,
  sem chamar o mapper e sem devolver dados de outro dossiê. O span ativo e o log registram somente
  identificador e `origem = mock`. O comando
  `mvn -q "-Dtest=ConsultaDossieProdutoSimuladorAdapterTest,ArchUnitProgressivoTest" test`
  concluiu com 38 testes verdes (5 do adapter e 33 de arquitetura), sem falhas, erros ou testes
  ignorados. As buscas de isolamento não encontraram imports de DTO/client/erro MTR ou REST nem
  annotations de fault tolerance nos artefatos do simulador; as buscas de segurança não
  encontraram PII ou segredos no qualifier/adapter. A revisão de correção, simplicidade,
  arquitetura, segurança, desempenho e escopo não encontrou apontamentos: a leitura é local,
  determinística e sem fallback; producer CDI e Resource não foram alterados. A borda pública
  continua em RED; suíte completa e checkpoint SonarQube permanecem previstos para C2.
- 2026-08-12 — Task 10.1 executada em RED -> GREEN. O RED falhou na compilação somente pela
  ausência de `ConsultaDossieProdutoPortasProducer`. O producer foi criado conforme o padrão do
  domínio: recebe `ObterDossieProduto` com os qualifiers MTR e simulador e produz uma única porta
  `@ApplicationScoped`; `true` devolve o simulador e `false` devolve o MTR. A seleção reutiliza
  exclusivamente `simtr-hub.simulador.dossie-produto.habilitado`; nenhum arquivo de configuração
  foi alterado, mantendo `false` em produção e `true` nos perfis dev/test já existentes. O comando
  `mvn -q "-Dtest=ConsultaDossieProdutoPortasProducerTest,ArchUnitProgressivoTest" test`
  concluiu com 36 testes verdes (3 do producer e 33 de arquitetura), sem falhas, erros ou testes
  ignorados. O Quarkus iniciou normalmente e o teste CDI confirmou uma única
  `ObterDossieProduto`, satisfeita e não ambígua, sem recursão ou dependência circular. A revisão
  de correção, simplicidade, arquitetura, segurança, desempenho e escopo não encontrou
  apontamentos: não há lógica adicional, dado sensível, dependência ou mudança de default. A
  borda pública não foi alterada e continua em RED; suíte completa e checkpoint SonarQube
  permanecem previstos para C2.
- 2026-08-12 — Task 11.1 executada incrementalmente em RED -> GREEN. O primeiro RED falhou na
  compilação somente pela ausência de `ConsultaDossieProdutoResponse`; após criar o DTO REST, o
  segundo RED falhou somente pela ausência de `ConsultaDossieProdutoRestMapper`. O DTO declara
  explicitamente todos os nomes `snake_case`, preserva os nulos aprovados e omite somente
  `processo.data` quando ausente, conforme o contrato congelado. O mapper converte exclusivamente
  o modelo interno para o DTO REST, preservando campos, ordem, listas vazias, listas nulas e
  elementos nulos; também traduz as quatro classificações internas para o mecanismo REST
  existente sem perder status, recurso, identificadores, código, mensagens, detalhe ou
  stacktrace externo. O teste estrutural compara o JSON efetivamente serializado e usa apenas a
  identidade sintética aprovada. O comando
  `mvn -q "-Dtest=ConsultaDossieProdutoRestMapperTest,ArchUnitProgressivoTest" test` concluiu com
  38 testes verdes (5 do mapper REST e 33 de arquitetura), sem falhas, erros ou testes ignorados.
  A reexecução do contrato público executou 46 testes: os 42 cenários existentes passaram e
  somente os quatro novos falharam, todos com `404` pela ausência deliberada do Resource. As
  revisões de correção, simplicidade, arquitetura, segurança, desempenho e escopo não encontraram
  apontamentos; não há dependência das bordas MTR/simulador nem PII ou segredo nos novos
  artefatos. Resource e instrumentação não foram alterados; suíte completa e checkpoint SonarQube
  permanecem previstos para C2.
- 2026-08-12 — Task 12.1 executada em RED -> GREEN. O RED falhou na compilação somente pela
  ausência de `ConsultaDossieProdutoObservabilidade` e do método
  `DossieProdutoResource.consultarDossieProduto(Long)`. O wrapper CDI de aplicação implementa
  somente `ConsultarDossieProduto`, constrói o caso de uso sobre a `ObterDossieProduto`
  selecionada e declara o span INTERNAL `simtr-hub.service.dossie-produto.consultar`. O Resource
  passou a expor `GET /simtr-hub/v1/dossie-produto/{id}` sem corpo, valida `id >= 1`, depende
  somente da porta de entrada, declara o span SERVER `simtr-hub.api.dossie-produto.consultar` e
  documenta `200/400/401/403/404/500` no OpenAPI. Sucesso retorna `200` com o DTO público; as
  quatro classificações internas são traduzidas pelo mapper REST e o teste Quarkus comprovou a
  preservação integral do erro `404`. API e aplicação registram rota templated, API v2, origem,
  flag do simulador, identificador, resultado e contagens; falhas registram apenas a classe do
  erro, sem `Throwable`, mensagem ou stacktrace externo. Sentinelas de CPF, CNPJ, nome e matrícula
  presentes na resposta de teste não apareceram nos novos spans ou logs. O comando focado
  `mvn -q "-Dtest=ConsultaDossieProdutoObservabilidadeTest,ConsultaDossieProdutoResourceQuarkusTest,DossieProdutoApiContractTest,DossieProdutoErroApiContractTest,ResourceEndpointTest,ResourceBeanCoverageTest,ConsultaDossieProdutoRestMapperTest,ArchUnitProgressivoTest" test`
  concluiu com 100 testes verdes (4 de observabilidade, 3 do Resource novo, 11 do contrato de
  sucesso, 18 do contrato de erros, 17 de endpoints, 9 de cobertura direta dos Resources, 5 do
  mapper REST e 33 de arquitetura), sem falhas, erros ou testes ignorados. Os quatro cenários
  públicos antes em RED agora estão verdes sem relaxar o contrato; a revisão de correção,
  simplicidade, arquitetura, segurança, desempenho e escopo não encontrou apontamentos. O
  `DossieProdutoResource` foi alterado somente no método, injeção e helpers necessários; nenhuma
  dependência ou configuração foi adicionada, `.tools/` permaneceu intacto, e suíte completa e
  checkpoint SonarQube continuam reservados para o C2.
- 2026-08-12 — usuário registrou `GO C2` e acrescentou o requisito de representar por constantes
  as strings dos testes unitários para prevenir duplicações apontadas pelo SonarQube. A aplicação
  dessa regra fica limitada aos testes novos ou alterados desta feature; testes preexistentes fora
  do escopo não serão refatorados silenciosamente.
- 2026-08-12 — C2 executado até o checkpoint de decisão. Strings repetidas nos testes novos ou
  alterados foram substituídas por constantes de teste ou constantes da API (`MediaType`), sem
  alterar comportamento; nenhuma issue `java:S1192` foi apontada nos testes. A bateria focada com
  contratos, domínio/aplicação, bordas REST/MTR/simulador, observabilidade e ArchUnit passou sem
  falhas. `mvn -q clean test` concluiu com 119 suites, 494 testes, zero falhas, zero erros e zero
  ignorados; `git diff --check` não encontrou erro de whitespace, apenas avisos de normalização
  LF/CRLF.
- 2026-08-12 — a primeira tentativa do checkpoint oficial foi recusada antes da análise porque
  `.codex/.state/session.json` havia sido substituído por um estado schema 1
  `NOT_REQUIRED_UNTIL_CODE_CHANGE`, apesar do baseline READY registrado na Task 1.1. O snapshot
  ainda presente no Sonar local foi consultado e coincidiu exatamente com a evidência registrada:
  mesma análise `bc25e485-140a-4a77-bd0c-6d8537ca5091`, 219 issues, cobertura 87,8% e duplicação
  3,7%. O estado operacional foi restaurado como schema 3 `LOCAL_SONAR/READY`, preservando o
  `sessionId` e o fingerprint inicial existentes, sem nova análise nem alteração no servidor.
- 2026-08-12 — `./validar-checkpoint-sonarqube.ps1` então concluiu build, 494 testes, scanner e
  Compute Engine com sucesso, mas registrou situação técnica `NON_COMPLIANT` e decisão humana
  `PENDING`: 242 issues abertas contra 219 no baseline, 23 issues novas, uma issue severa,
  cobertura 86,7% (mínimo 85%) e duplicação 3,8% (máximo 5%). A issue severa é `java:S1192` em
  `DossieProdutoResource`, pedindo reutilizar a constante já definida `DOSSIE_PRODUTO_API_V2`;
  as outras 22 são de manutenibilidade: 11 em produção e 11 em testes, abrangendo retorno nulo
  intencional de coleções contratuais, anotações/cast, variáveis ou pattern `ignored`, construtor
  com nove parâmetros, override redundante, lambdas de `assertThrows`, identificador restrito e
  métodos de teste com mais de 25 assertions. C2 permanece pendente até a decisão explícita do
  usuário: `Reprovar`, `AceitarExcepcionalmente` ou `ContinuarAjustes`.
- 2026-08-12 — usuário escolheu explicitamente `ContinuarAjustes`; a decisão foi registrada por
  `./validar-checkpoint-sonarqube.ps1 -HumanDecision ContinuarAjustes`. O C2 permanece em execução
  e as correções ficam limitadas a manutenibilidade sem mudança de contrato ou comportamento.
- 2026-08-12 — as correções de manutenibilidade eliminaram as 23 issues novas sem relaxar testes
  ou contratos. Foram reutilizadas constantes para strings repetidas, incluindo a constante da
  API v2 no Resource; lambdas, variáveis ignoradas, casts e métodos de teste foram simplificados;
  e os retornos nulos exigidos pela distinção contratual entre lista ausente e vazia receberam
  supressões locais justificadas. O checkpoint intermediário caiu para três issues novas, zero
  severas, cobertura de 86,7% e duplicação de 3,5%; as três sugestões remanescentes foram então
  corrigidas e a bateria focada de contratos, Resource, adapter MTR e ArchUnit permaneceu verde.
- 2026-08-12 — o checkpoint oficial final `./validar-checkpoint-sonarqube.ps1` concluiu novamente
  build, 494 testes, SonarScanner e Compute Engine com sucesso. O resultado foi `COMPLIANT`, com
  219 issues abertas, exatamente as 219 do baseline, zero issues novas, zero issues severas,
  cobertura de 86,7% (mínimo 85%) e duplicação de 3,5% (máximo 5%); a decisão ficou
  `NOT_REQUIRED`. `git diff --check` permaneceu verde, apresentando somente avisos de normalização
  LF/CRLF. O C2 está concluído e o item 13.1 não foi iniciado por depender de nova autorização.
- 2026-08-12 — usuário autorizou explicitamente o início da Task 13.1. O RED do comando planejado
  `mvn -q "-Dtest=ConsultaDossieProdutoMtrContractTest,ConsultaDossieProdutoSelecaoSimuladorQuarkusTest" test`
  falhou somente porque as duas provas ainda não existiam (`No tests matching pattern`).
- 2026-08-12 — Task 13.1 concluída com os dois testes de integração previstos, reutilizando o
  `DossieProdutoMtrStubTestResource` existente sem alterá-lo e sem mudança em código de produção.
  Com `simtr-hub.simulador.dossie-produto.habilitado=false`, três cenários percorreram
  `GET /simtr-hub/v1/dossie-produto/4324680` até o stub HTTP local: o sucesso preservou o JSON
  completo; o stub recebeu `GET /simtr/dossie-produto/v2/dossie-produto/4324680`, corpo vazio,
  ausência de `Content-Type`, `Accept: application/json`, API key de teste, bearer token obtido no
  endpoint OIDC local e `traceparent` válido; o `404` foi preservado integralmente com uma única
  chamada; e um `500` recuperável foi seguido por sucesso em duas requisições GET equivalentes.
  Com a property `true`, a API pública devolveu a fixture sintética `4324680`, incluindo nulos,
  lista vazia e produto nulo, enquanto o REST Client de teste permaneceu apontado para a porta
  local indisponível, provando a seleção do simulador sem acesso MTR.
- 2026-08-12 — a verificação GREEN executou os quatro testes novos sem falhas, erros ou testes
  ignorados. `ArchUnitProgressivoTest` passou separadamente com 33 testes. A revisão de correção,
  legibilidade/simplicidade, arquitetura, segurança e desempenho não encontrou apontamentos: os
  testes usam constantes para strings compartilhadas, somente identidade sintética, rede local e
  as bordas públicas reais; não adicionam dependência, abstração ou comportamento. Suíte completa,
  contratos transversais e checkpoint SonarQube permanecem reservados para C3, após a Task 14.1.
- 2026-08-12 — usuário autorizou explicitamente a Task 14.1 com `GO 14.1`. Os contratos
  transversais passaram a incluir a consulta de dossiê: no simulador, spans SERVER -> INTERNAL com
  origem `mock`; no MTR, spans SERVER -> INTERNAL -> CLIENT no mesmo trace, rota pública templated,
  API `dossie-produto-v2`, método e path MTR templated; em ambos, logs de sucesso/falha registram
  identificador, origem, resultado, classificação e, quando aplicável, contagens de clientes,
  unidades de tratamento e produtos contratados.
- 2026-08-12 — o ciclo RED de segurança encontrou `ERRO_SENTINELA_NAO_REGISTRAR` no evento
  transversal preexistente `mtr.erro.negocio.retornado`: o mapper compartilhado registrava
  `detalhe` e mensagens textuais do payload externo. O ajuste mínimo removeu somente esses dois
  campos da telemetria, preservando status, classificação e identificadores protocolares. A prova
  conjunta de `ConsultaDossieProdutoMtrContractTest` e `ExceptionMapperTest` executou 12 testes
  verdes e confirmou que o corpo HTTP de erro continua integralmente preservado.
- 2026-08-12 — o comando planejado
  `mvn -q "-Dtest=ObservabilidadeSpansContratoTest,ObservabilidadeLogsContratoTest,ConsultaDossieProdutoMtrContractTest" test`
  executou 9 testes verdes, sem falhas, erros ou testes ignorados. CPF, CNPJ, nome, matrícula,
  payload, mensagem de erro externa, token e API key sintéticos não apareceram nos logs, atributos
  ou eventos dos spans capturados. As strings compartilhadas dos testes foram mantidas em
  constantes para prevenir duplicação Sonar. A regressão de seleção do simulador e ArchUnit
  executou mais 34 testes verdes; `git diff --check` não encontrou erros, somente os avisos de
  normalização LF/CRLF já conhecidos. A suíte completa e o checkpoint SonarQube não foram
  executados porque pertencem ao próximo checkpoint C3.
- 2026-08-12 — usuário registrou explicitamente `GO C3`. A bateria focada
  `mvn -q "-Dtest=ConsultaDossieProdutoMtrContractTest,ConsultaDossieProdutoSelecaoSimuladorQuarkusTest,ObservabilidadeLogsContratoTest,ObservabilidadeSpansContratoTest" test`
  executou 10 testes verdes, cobrindo wire MTR, seleção sem rede, sucesso/falha de logs, hierarquia
  e atributos de spans e os dez endpoints observados. `mvn -q clean test` também concluiu com
  sucesso, sem falhas de suíte, provando a regressão completa antes da análise.
- 2026-08-12 — o checkpoint oficial `./validar-checkpoint-sonarqube.ps1` concluiu `clean verify`,
  SonarScanner e Compute Engine com sucesso. O resultado foi `COMPLIANT`: 218 issues abertas
  contra 219 no baseline, zero issues novas, zero issues severas, cobertura de 87,8% e duplicação
  de 3,5%; decisão humana `NOT_REQUIRED`. `git diff --check` permaneceu sem erros, apresentando
  somente os avisos de normalização LF/CRLF já conhecidos.
- 2026-08-12 — a revisão do C3 nos eixos de correção, legibilidade/simplicidade, arquitetura,
  segurança e desempenho não encontrou apontamentos obrigatórios: contratos e caminhos de erro
  estão cobertos; as bordas permanecem separadas e ArchUnit está verde na suíte; sinais não
  expõem payload, PII ou segredos; não houve dependência nova, bloqueio ou operação não limitada.
  O C3 está concluído e a Task 15.1 não foi iniciada por depender de nova autorização.
- 2026-08-12 — usuário autorizou explicitamente a Task 15.1 e reforçou a decisão do ADR-0006 de
  não criar testes que inspecionem ou mantenham snapshot do OpenAPI gerado. README e arquitetura
  consolidada agora registram dez capacidades no Hub, sete em `dossieproduto`, a capacidade
  `ConsultarDossieProduto`, o path público `GET /simtr-hub/v1/dossie-produto/{id}` e o consumo MTR
  `GET /simtr/dossie-produto/v2/dossie-produto/{id}`; a lista de operações ausentes foi reduzida
  de quatro para três.
- 2026-08-12 — o ADR-0002 permaneceu `Aceito` e recebeu somente a atualização factual do limite
  atual de `dossieproduto`; seu índice foi atualizado no mesmo incremento. O resumo do ADR-0006 no
  índice passou a explicitar que OpenAPI é gerado do código e não recebe snapshot estático, sem
  alterar a decisão já aceita nem criar ADR novo. Buscas de consistência não encontraram as
  contagens ou ausências antigas nos quatro documentos alterados; a única ocorrência restante de
  “quatro” no README descreve corretamente as quatro properties de simulador. Nenhum `.ppt`,
  `.pptx`, `.pdf` ou `.html` foi alterado. `git diff --check` não encontrou erros, somente avisos
  conhecidos de normalização LF/CRLF. Por ser incremento exclusivamente documental, Maven,
  SonarQube e inspeção de `sonar/` não foram executados, conforme `AGENTS.md`.
- 2026-08-12 — usuário autorizou a Task 16.1 com redução explícita de escopo: somente o catálogo
  de observabilidade seria atualizado; a coleção Postman ficou fora do fluxo automatizado e será
  atualizada manualmente pelo responsável. Plano, checklist, critérios de aceitação, verificações,
  arquivos prováveis e critério final foram ajustados antes da alteração do catálogo.
- 2026-08-12 — o catálogo passou a registrar a décima capacidade, os 20 spans manuais do caminho
  simulador, as dez declarações CLIENT MTR e os 50 eventos de sucesso. A consulta por identificador
  documenta os spans SERVER → INTERNAL e SERVER → INTERNAL → CLIENT, eventos REST/aplicação,
  simulador e MTR, rota pública, operação, origem, resultado, id, contagens, método/path MTR e
  classificação de falha.
- 2026-08-12 — as proibições operacionais da consulta ficaram explícitas: seus sinais específicos
  não registram payload, CPF, CNPJ, nome, matrícula, chave de correlação, token, API key,
  mensagem/detalhe externo ou URL interna completa; o comportamento preexistente do filtro
  compartilhado foi distinguido. Os nomes de testes catalogados existem nos contratos do C3 e os
  nomes/atributos conferem com Resource, wrapper de aplicação e adapters MTR/simulador. A busca não
  encontrou a identidade original nem segredos sintéticos nos documentos da task; Postman e
  formatos `.ppt`, `.pptx`, `.pdf` e `.html` permaneceram intocados. `git diff --check` não
  encontrou erros, somente o aviso conhecido de normalização LF/CRLF. Maven, SonarQube e `sonar/`
  não foram executados ou inspecionados porque o incremento continuou exclusivamente documental.
- 2026-08-12 — ajuste S1 autorizado pelo usuário para eliminar as cinco issues LOW `java:S1185`
  exibidas pelo SonarQube local no período de new code. A consulta da API identificou overrides
  redundantes de `mensagens()` em `FalhaCriacaoDossieProduto`,
  `FalhaAtualizacaoFormularioDossieProduto`, `FalhaInclusaoDocumentoDossieProduto`,
  `FalhaRegistroValidacaoNegocialDossieProduto` e `FalhaWorkflowDossieProduto`. Os mesmos cinco
  identificadores já constavam no baseline de 219 issues; ainda assim, foram removidos conforme
  solicitado, sem reclassificar ou aceitar excepcionalmente qualquer issue.
- 2026-08-12 — o comando
  `mvn -q "-Dtest=FalhasDossieProdutoTest,ArchUnitProgressivoTest" test` executou 42 testes verdes
  antes e depois da alteração. O diff das cinco classes remove somente os overrides que retornavam
  `super.mensagens()`; o método público herdado, os construtores, tipos, mensagens e causas foram
  preservados. O `clean verify` do checkpoint oficial também passou integralmente.
- 2026-08-12 — `./validar-checkpoint-sonarqube.ps1` concluiu build, testes, SonarScanner e Compute
  Engine com resultado `COMPLIANT`: 213 issues abertas contra 219 no baseline, zero issues novas,
  zero issues severas, cobertura de 87,8%, duplicação de 3,5%, nenhuma violação e decisão
  `NOT_REQUIRED`. A consulta posterior da API local confirmou `newCodeIssues=0`.
  `git diff --check` não encontrou erros, somente os avisos conhecidos de normalização LF/CRLF.
  O S1 está concluído; C4 permanece pendente e não foi iniciado.
- 2026-08-12 — usuário autorizou o checkpoint C4 com `c4`. README, arquitetura consolidada,
  ADR-0002, índice de ADRs e catálogo de observabilidade foram comparados com o Resource, REST
  Client MTR v2, configuração de seleção, spans e testes implementados. Permanecem consistentes a
  capacidade `ConsultarDossieProduto`, as sete capacidades de `dossieproduto`, a rota pública
  `GET /simtr-hub/v1/dossie-produto/{id}`, o wire MTR v2, a geração de OpenAPI sem snapshot e os
  contratos de logs/spans. Não houve decisão arquitetural nova nem necessidade de criar ADR.
- 2026-08-12 — o status Git confirmou `derivedChanges=0` e `postmanChanges=0`; os nomes de testes
  citados no catálogo existem no código. `git diff --check` não encontrou erros, somente os
  avisos conhecidos de normalização LF/CRLF. Nenhum `.ppt`, `.pptx`, `.pdf` ou `.html` foi gerado
  ou atualizado. Maven, SonarQube e `sonar/` não foram executados ou inspecionados, pois C4 foi
  exclusivamente documental. C4 está concluído; a Task 17.1 não foi iniciada.
- 2026-08-12 — usuário autorizou explicitamente a Task 17.1. A primeira execução de
  `mvn -q clean test` concluiu 121 suites e 500 testes, com zero falhas, zero erros e zero
  ignorados. A revisão integral começou pelos testes e confirmou cobertura de contrato público,
  validação de identificador, wire/headers/OIDC/trace MTR v2, seleção CDI, fixture sem rede,
  resposta ausente ou divergente, `404`, grupos `4xx`, `5xx`, timeout, retry e segurança dos
  sinais observáveis.
- 2026-08-12 — a revisão nos eixos de correção, legibilidade, arquitetura, segurança, desempenho
  e escopo encontrou um apontamento obrigatório: duas asserções negativas do teste da fixture
  ainda continham CPF e nome reais fornecidos no exemplo inicial. Os dois literais foram removidos
  e o teste foi renomeado para declarar apenas o contrato efetivamente provado — wire v2 e
  identidade sintética. A busca restrita à feature passou a retornar `featureIdentityMatches=0`;
  o teste focado do mapper/fixture permaneceu verde. Não restaram findings críticos ou
  obrigatórios.
- 2026-08-12 — as buscas finais retornaram zero imports do núcleo para adapters, zero imports
  cruzados entre REST, MTR e simulador, zero operações bloqueantes, zero testes de OpenAPI, zero
  mudanças em dependências, Postman ou formatos derivados e nenhum padrão de segredo novo. O
  Resource possui 979 linhas: foi inspecionado por proximidade do sinal estrutural de 1.000 linhas,
  mas permanece no padrão vigente de um Resource por domínio, sem acoplamento novo obrigatório a
  corrigir. `.tools/` continua preexistente, não rastreado e fora do escopo.
- 2026-08-12 — após a correção, `mvn -q clean test` repetiu 121 suites e 500 testes com zero
  falhas, erros ou ignorados. O checkpoint oficial `./validar-checkpoint-sonarqube.ps1` executou
  `clean verify`, SonarScanner e Compute Engine e terminou `COMPLIANT`: 213 issues abertas contra
  219 no baseline, zero issues novas, zero issues severas, cobertura de 87,8%, duplicação de 3,5%,
  nenhuma violação e decisão `NOT_REQUIRED`. `git diff --check` não encontrou erros, somente os
  avisos conhecidos de normalização LF/CRLF. A Task 17.1 está concluída; apenas o CF depende de
  aceitação e encerramento explícitos do usuário.
- 2026-08-12 — validação manual adicional executada em `quarkus:dev`, com o simulador habilitado
  pelo perfil dev. `GET /simtr-hub/v1/dossie-produto/4324680` respondeu `200` com o contrato
  completo, identidade sintética, nulos e listas preservados; o identificador `9999999` respondeu
  `404` com `DOSSIE_PRODUTO_NAO_ENCONTRADO`; e o identificador `0` respondeu `400` com a mensagem
  de validação aprovada. O processo Quarkus iniciado para a prova foi encerrado e a porta 8080
  ficou livre. A validação não foi interpretada como CF, que permanece pendente.
- 2026-08-12 — usuário registrou explicitamente `cf` após receber as evidências da suíte completa,
  checkpoint SonarQube, revisão integral e validação manual dos cenários `200`, `404` e `400`.
  O checkpoint final permanece `COMPLIANT`, sem decisão pendente. A feature foi aceita e encerrada;
  Postman continua reservado para atualização manual pelo responsável, conforme decisão de escopo.
