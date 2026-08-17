# Checklist: capturar dossiê de produto

## Estado

- **Branch:** `feature/capturar-dossie-produto`
- **Escopo:** adendos documental e da mensagem simulada aprovados e encerrados
- **Próximo item:** nenhum — C18 aprovado pelo usuário
- **Último Sonar:** análise `98a5379f-ab23-40cd-b7c4-7b45c0289ecb`, fingerprint
  `4373b4294828bbe04aeaf683c13b80e554fb4887bfc0299d382303f29af9a0ba`, `COMPLIANT` e
  `NOT_REQUIRED`: zero issue nova, cobertura 88,0% e duplicação 3,3%
- **Alteração preexistente preservada:** `.tools/` não rastreado e fora do escopo

## Checklist

- [x] 0.1 Confirmar intenção e critérios de sucesso a partir do pedido;
- [x] 0.2 Ler arquitetura consolidada e índice de ADRs;
- [x] 0.3 Ler ADR-0002, ADR-0004, ADR-0005, ADR-0006 e ADR-0007;
- [x] 0.4 Inspecionar Swagger 2.20.0.8 e especificação complementar;
- [x] 0.5 Inspecionar código, configuração e testes de capacidades análogas;
- [x] 0.6 Registrar contrato, arquitetura, riscos, verificações e checkpoints;
- [x] C0 Registrar GO humano antes da primeira alteração de produção, teste ou tooling;
- [x] C1 Aprovar contrato, arquitetura, segurança, observabilidade e política sem retry;
- [x] 1.1 Inicializar baseline SonarQube pelo modo escolhido pelo usuário;
- [x] 2.1 Congelar contrato público em RED;
- [x] 3.1 Criar resultado e falha internos;
- [x] 4.1 Criar portas e caso de uso atômico;
- [x] CA Verificar núcleo isolado e manter RED público explicado;
- [x] 5.1 Criar DTO e mapper MTR da captura;
- [x] 6.1 Declarar REST Client, erro protocolar e fault tolerance aprovada;
- [x] 7.1 Implementar adapter MTR e validar integridade da resposta;
- [x] CB Verificar borda MTR e matriz sem retry;
- [x] 8.1 Criar fixture, DTO e mapper próprios do simulador;
- [x] 9.1 Implementar adapter simulador e `404` para fixture ausente;
- [x] 10.1 Selecionar MTR/simulador por producer CDI;
- [x] 11.1 Criar DTO e mapper REST da captura;
- [x] 12.1 Expor POST no Resource e instrumentar API/aplicação;
- [x] C2 Validar fatia vertical, suíte completa, ArchUnit e checkpoint SonarQube;
- [x] 13.1 Provar wire MTR, headers, zero retry e seleção sem rede;
- [x] 14.1 Congelar contratos transversais de endpoints, logs e spans;
  - [x] Reproduzir em runtime o contrato MTR de hierarquia, paths templated e sigilo;
  - [x] C14-OBS Autorizar ajuste de observabilidade estrito à captura;
  - [x] Tornar verdes os contratos MTR/simulador, logs, spans, Resource e ArchUnit;
- [x] RP-C3 Replanejar a eliminação de `url.full` após a reprovação do C3;
- [x] C3-TRACE Aprovar supressão local do span HTTP automático e propagação explícita do contexto;
- [x] 14.2 Eliminar `url.full` do trace da captura sem afetar outros REST Clients;
- [x] C3 Revalidar integração/observabilidade e checkpoint SonarQube após a Task 14.2;
- [x] 15.1 Atualizar README, arquitetura, ADR-0002 e índice;
- [x] 16.1 Atualizar documentação fonte de observabilidade;
- [x] C4 Validar documentação sem Postman ou formatos derivados;
- [x] 17.1 Executar validação final e revisão integral do diff;
  - [x] 17.1-A Extrair constantes locais nos dois testes autorizados;
  - [x] 17.1-R Revalidar testes, SonarQube, diff e revisão após o ajuste;
- [x] CF Registrar aceitação e encerramento humanos da feature.
- [x] C18-GO Registrar GO humano para o adendo documental 18.1;
- [x] 18.1 Explicitar o identificador `123` no título e nas referências do mock;
- [x] C18-CONTRATO Aprovar a mensagem pública do `404` simulado e registrar `GO 18.2`;
- [x] 18.2 Aplicar RED → GREEN, regressão completa e checkpoint SonarQube;
- [x] C18 Revisar e encerrar os adendos.

## Critérios globais de conclusão

- [x] `POST /simtr-hub/v1/dossie-produto/{id}/capturar` responde o contrato público aprovado;
- [x] requisição sem corpo e sem `Content-Type` é aceita;
- [x] `id <= 0` responde `400` pelo contrato de validação existente;
- [x] modo MTR chama exatamente
  `POST /simtr/dossie-produto/v1/dossie-produto/{id}/capturar`, com corpo vazio;
- [x] API key, bearer token e `traceparent` são propagados sem aparecer nos sinais próprios da
  captura;
- [x] modo simulador retorna fixture própria sem rede e sem PII real;
- [x] `404` simulado e erros MTR `400/401/403/404/409/500`, timeout e corpo inválido são cobertos;
- [x] `500` e timeout produzem uma única chamada enquanto não houver idempotência aprovada;
- [x] nenhuma resposta externa nula, sem identificador ou com id divergente é publicada como
  sucesso;
- [x] DTOs e mappers das três bordas permanecem independentes;
- [x] logs/spans não contêm payload, PII, token, API key, mensagem/stacktrace externo ou URL
  interna completa;
- [x] testes focados e suíte completa passam; 50 contratos públicos e 33 regras ArchUnit do novo
  fingerprint estão verdes;
- [x] checkpoint SonarQube do novo fingerprint executável está `COMPLIANT/NOT_REQUIRED`, sem issue
  nova e dentro dos limites de cobertura e duplicação;
- [x] README, arquitetura, ADR e catálogo refletem a capacidade implementada;
- [x] Postman, formatos derivados, dependências e arquivos fora do escopo permanecem intactos.
- [x] o identificador `123` está visível como dado da fixture, enquanto o endpoint público continua
  documentado com o parâmetro `{id}`, sem alteração do JSON ou do contrato parametrizado.
- [x] o `404` simulado informa dinamicamente os identificadores solicitado e disponível quando a
  fixture possui ID divergente, sem alterar os demais campos ou anunciar ID quando ele não existe.

## Decisões humanas

| Checkpoint | Status | Data | Evidência/decisão necessária | Aprovador |
|---|---|---|---|---|
| C0 | APROVADO | 2026-08-13 | Usuário registrou `GO C0 e C1` | usuário |
| C1 | APROVADO | 2026-08-13 | Usuário aprovou rota/JSON/status, capacidade atômica, segurança, sinais, timeout/circuit breaker e ausência de retry pelo GO conjunto | usuário |
| CA | APROVADO | 2026-08-13 | Usuário registrou `GO CA`; núcleo isolado verde e RED público explicado | usuário |
| CB | APROVADO | 2026-08-13 | Usuário registrou `GO CB` e `GO ajuste CB`; borda MTR verde após retirar somente do novo client o provider que registrava payload/URL completa | usuário |
| C2 | APROVADO | 2026-08-13 | Usuário registrou `GO C2`; fatia vertical revisada sem achado crítico ou obrigatório, com suíte, ArchUnit e SonarQube verdes | usuário |
| C14-OBS | APROVADO | 2026-08-14 | Usuário registrou `GO ajuste 14.1`, restrito ao código estritamente necessário para a nova captura | usuário |
| C3 | REPROVADO | 2026-08-14 | Usuário registrou `Reprovar C3` diante da permanência do `url.full` no span HTTP automático | usuário |
| RP-C3 | APROVADO | 2026-08-14 | Usuário registrou `GO replanejar C3 para eliminar url.full`; autorização exclusivamente documental | usuário |
| C3-TRACE | APROVADO | 2026-08-14 | Usuário registrou `GO C3-TRACE`; provider exclusivo da captura com `TracingPolicy.IGNORE` e propagação explícita pelo OpenTelemetry configurado | usuário |
| C3-R2 | APROVADO | 2026-08-14 | Usuário registrou `Aprovar C3` após o ajuste MINOR, 569 testes verdes e SonarQube `COMPLIANT/NOT_REQUIRED` | usuário |
| C4 | APROVADO | 2026-08-14 | Usuário registrou `GO C4`; documentação consistente e Postman/derivados reservados à atualização manual | usuário |
| CF | APROVADO | 2026-08-15 | Usuário registrou explicitamente `CF`; evidências finais aceitas e feature encerrada | usuário |
| C18-GO | APROVADO | 2026-08-17 | Usuário registrou explicitamente `GO 18.1`; escopo restrito ao título e às referências humanas do mock | usuário |
| C18-CONTRATO | APROVADO | 2026-08-17 | Usuário registrou `GO C18-CONTRATO e 18.2`; IDs solicitado e disponível permanecem dinâmicos, vindos respectivamente da rota e da fixture | usuário |
| C18 | APROVADO | 2026-08-17 | Usuário registrou explicitamente `Aprovar C18` após receber as evidências da implementação, dos testes e do checkpoint SonarQube | usuário |

## Evidências do planejamento

- 2026-08-17 — o usuário registrou explicitamente `Aprovar C18`, aceitou as evidências da Task
  18.2 e encerrou os adendos. Nenhuma execução Maven ou SonarQube foi repetida, pois este registro
  é exclusivamente documental e não houve alteração executável após o checkpoint final
  `COMPLIANT/NOT_REQUIRED`.
- 2026-08-17 — a Task 18.2 foi implementada após o GO. O primeiro baseline foi impedido pelo
  processo de desenvolvimento que mantinha `target/simtr-hub-dev.jar` aberto; após identificar e
  encerrar somente o processo Java da porta `8080`, o script oficial concluiu o baseline local
  `READY`, análise `0ddbed8b-2bcc-4046-946f-eaea349d77b5`, com 213 issues, cobertura de 88,0%,
  duplicação de 3,3% e `COMPLIANT/NOT_REQUIRED`. A aplicação local permaneceu parada para que o
  usuário decida quando reiniciá-la.
- 2026-08-17 — o RED focado executou nove testes e falhou exatamente nos dois novos contratos: o
  teste unitário e o teste Quarkus pela rota pública ainda receberam a mensagem anterior, sem o ID
  disponível. A implementação mínima passou a obter o ID solicitado do parâmetro da rota e o ID
  disponível da resposta da fixture, sem fixar `13` ou `123` no código. Fixture ausente ou sem ID
  preserva a mensagem anterior.
- 2026-08-17 — o GREEN focado executou os mesmos nove testes sem falhas. `mvn -q clean test`
  concluiu sem falhas, erros ou ignorados, e o checkpoint oficial publicou a análise
  `98a5379f-ab23-40cd-b7c4-7b45c0289ecb`, fingerprint
  `4373b4294828bbe04aeaf683c13b80e554fb4887bfc0299d382303f29af9a0ba`. O resultado ficou
  `COMPLIANT/NOT_REQUIRED`: 213 issues atuais e 213 no baseline, zero issue nova ou severa,
  cobertura de 88,0%, duplicação de 3,3% e nenhuma violação.
- 2026-08-17 — a revisão de contrato, correção, simplicidade, arquitetura, segurança, desempenho,
  testes e escopo não encontrou achado crítico ou obrigatório. Status, formato JSON, `recurso`,
  `id_erro`, `codigo_erro`, sucesso com `123`, falhas sem ID, mensagens MTR e outros simuladores
  permanecem inalterados. Configuração, dependências, Postman, formatos derivados e `.tools/`
  ficaram fora do incremento; `git diff --check` terminou sem erros. A Task 18.2 está concluída,
  mas o checkpoint C18 permanece pendente de decisão humana explícita.
- 2026-08-17 — usuário registrou explicitamente `GO C18-CONTRATO e 18.2`. O contrato aprovado não
  fixa o ID solicitado: a mensagem usa o parâmetro recebido em
  `POST /simtr-hub/v1/dossie-produto/{id}/capturar` e informa o identificador disponível lido da
  fixture. O próximo passo autorizado é inicializar o baseline local antes do teste RED.
- 2026-08-17 — usuário solicitou implementar no `404` do simulador a informação de que a fixture
  possui somente o ID `123`. A inspeção localizou a mensagem em
  `CapturaDossieProdutoSimuladorAdapter.naoEncontrado` e sua asserção unitária; o plano acrescenta
  também um contrato Quarkus pela rota pública. A proposta deriva o ID disponível da resposta da
  fixture, preserva a mensagem atual quando não houver ID válido e não altera erros MTR ou outros
  simuladores. Como passa a haver código/teste, baseline, Maven e checkpoint SonarQube voltam a ser
  obrigatórios após o GO e antes/nos momentos definidos na Task 18.2. A verificação prévia
  confirmou que `sonar/` permanece ausente, portanto não há pacote offline a escolher e o baseline
  previsto usa somente o SonarQube Docker local.
- 2026-08-17 — durante a revisão C18, o usuário esclareceu que o endpoint público deve permanecer
  parametrizado por um identificador de dossiê. A referência foi corrigida para distinguir o
  contrato `/{id}/capturar` do valor `123`, que pertence somente ao cenário feliz da fixture; não
  houve alteração do JSON, do endpoint ou do comportamento.
- 2026-08-17 — após `GO 18.1`, o título e a nova referência humana passaram a tornar o identificador
  sintético descobrível. O endpoint templated, o cabeçalho técnico e o JSON da fixture permaneceram
  inalterados. Por ser ajuste exclusivamente documental, Maven, baseline e checkpoint SonarQube
  não foram executados; C18 aguarda revisão e encerramento humanos.
- 2026-08-17 — usuário relatou que o identificador disponível não está explícito no título nem em
  uma referência do mock e solicitou torná-lo visível. A inspeção confirmou que a fixture contém
  somente o id sintético `123` no JSON e que `MarkdownJsonMockReader` depende literalmente do
  cabeçalho `## dados do mock corpo do retorno json`; o adendo preservará ambos e alterará somente
  o texto humano após `GO 18.1`.
- 2026-08-13 — usuário registrou explicitamente `GO 12.1`; o escopo autorizado ficou limitado à
  ligação da rota pública ao caso de uso, wrapper de observabilidade da aplicação e testes dessa
  fatia, sem avançar para provas de wire MTR, contratos transversais, documentação ou C2.
- 2026-08-13 — usuário registrou explicitamente `GO 11.1`; o escopo autorizado ficou limitado ao
  DTO de resposta REST, mapper de resultado/falhas e teste unitário da captura, sem criar rota,
  alterar Resource, instrumentar spans ou modificar mappers e DTOs existentes.
- 2026-08-13 — usuário registrou explicitamente `GO 10.1`; o escopo autorizado ficou limitado ao
  producer CDI da porta de saída da captura e seu teste, reutilizando a property existente, sem
  alteração de adapters, REST, Resource, observabilidade ou configuração compartilhada.
- 2026-08-13 — usuário registrou explicitamente `GO 9.1`; o escopo autorizado ficou limitado ao
  qualifier e adapter simulador, seu teste unitário e ArchUnit, sem producer CDI, REST, Resource
  ou alteração da borda MTR.
- 2026-08-13 — branch `feature/capturar-dossie-produto` criada a partir de `main` em `d40999f`
  antes do registro documental;
- 2026-08-13 — arquitetura e índice lidos; ADR-0002/0004/0005/0006/0007 selecionados e lidos;
- 2026-08-13 — Swagger confirma POST v1, `id int64`, ausência de body, sucesso `200 {id}` e erros
  `400/401/403/404/409/500`;
- 2026-08-13 — especificação complementar confirma a transição
  `PENDENTE_INFORMACAO -> EM_ALIMENTACAO`;
- 2026-08-13 — composição local confirmada como base `/simtr` mais client `/dossie-produto`,
  produzindo o wire `/simtr/dossie-produto/v1/dossie-produto/{id}/capturar`;
- 2026-08-13 — padrões reais de Resource, workflow, consulta, MTR, simulador, CDI, segurança,
  observabilidade, contratos e ArchUnit foram inspecionados;
- 2026-08-13 — divergência dos mappers em linha do workflow registrada; a captura terá DTOs e
  mappers independentes sem refatorar o legado;
- 2026-08-13 — ausência de garantia de idempotência registrada; a política proposta usa timeout e
  circuit breaker, sem retry automático;
- 2026-08-13 — `.tools/` identificado como conteúdo preexistente e mantido fora do escopo;
- 2026-08-13 — `sonar/`, Maven e SonarQube não foram inspecionados/executados porque esta sessão é
  exclusivamente documental;
- 2026-08-13 — referência geral de Definition of Done das skills não encontrada; foram usados
  `AGENTS.md`, `tasks/README.md` e os templates do repositório como critérios aplicáveis.
- 2026-08-13 — usuário registrou explicitamente `GO C0 e C1`; contrato público, desenho
  arquitetural, segurança, observabilidade e política de fault tolerance sem retry foram
  aprovados. Nenhuma alteração executável havia sido iniciada antes desse registro.
- 2026-08-13 — Task 1.1 confirmou que não existe diretório `sonar/`; portanto, sem pacote offline
  a escolher, a fonte aplicável foi somente o SonarQube Docker local. A primeira tentativa do
  baseline não iniciou análise porque `localhost:9000` recusou a conexão; o usuário ativou o
  Docker/Sonar, e a verificação seguinte confirmou container `sonarqube-simtr-local` ativo,
  servidor `UP` e `SONAR_TOKEN` presente sem exposição.
- 2026-08-13 — `./validar-checkpoint-sonarqube.ps1 -InitializeBaseline` concluiu `clean verify`,
  SonarScanner e Compute Engine com sucesso: 121 suítes, 500 testes, zero falhas, zero erros e zero
  ignorados. O baseline `LOCAL_SONAR` ficou `READY` na revisão `d40999f`, análise
  `9056ed2e-9910-4ed7-b6b6-33371ca35e64`, com 213 issues, cobertura de 87,8% e duplicação de 3,5%.
  A avaliação técnica foi `COMPLIANT` e a decisão ficou `NOT_REQUIRED`. Nenhuma alteração de
  produção ou teste foi iniciada; o próximo item é 2.1.
- 2026-08-13 — usuário autorizou explicitamente a Task 2.1 com `GO 2.1`. Foram acrescentados
  quatro cenários públicos: sucesso `200 {"id":123}` sem body/`Content-Type`, identificadores `0`
  e `-1` com erro `400` exato e smoke test `200` do endpoint. A requisição de sucesso usa o
  `HttpClient` da JDK e verifica antes do envio que `Content-Type` está ausente, porque a primeira
  execução revelou que o REST Assured acrescenta automaticamente
  `application/x-www-form-urlencoded` em POST sem body.
- 2026-08-13 — o RED final executou
  `mvn -q "-Dtest=DossieProdutoApiContractTest,DossieProdutoErroApiContractTest,ResourceEndpointTest" test`:
  Quarkus iniciou normalmente e executou 50 testes; 46 cenários existentes passaram e exatamente
  os quatro novos falharam, todos com `404` pela ausência deliberada de
  `POST /simtr-hub/v1/dossie-produto/{id}/capturar`. Os sucessos esperavam `200`; os ids inválidos
  esperavam `400`. Não houve erro de compilação, bootstrap ou infraestrutura. Nenhum arquivo de
  produção foi alterado; suíte completa e checkpoint SonarQube permanecem previstos para C2.
- 2026-08-13 — usuário autorizou explicitamente a Task 3.1 com `GO 3.1`. O teste foi escrito antes
  da implementação e o RED
  `mvn -q "-Dtest=FalhasDossieProdutoTest,ArchUnitProgressivoTest" test` falhou na compilação
  somente pela ausência de `ResultadoCapturaDossieProduto` e `FalhaCapturaDossieProduto`.
- 2026-08-13 — a implementação mínima criou um record com apenas
  `Long identificadorDossieProduto` e uma falha final com as classificações `NEGOCIO`,
  `TECNICA_CLIENTE`, `DEPENDENCIA_INDISPONIVEL` e `TIMEOUT`. A falha preserva status, recurso,
  identificadores, mensagens, detalhe, stacktrace externo e causa, além dos fallbacks do domínio.
  Nenhum dos tipos importa adapter, Jakarta, Quarkus ou MicroProfile.
- 2026-08-13 — o GREEN do mesmo comando concluiu 45 testes: 12 de domínio e 33 de arquitetura,
  todos verdes, sem falhas, erros ou ignorados. O contrato HTTP da Task 2.1 permanece RED pela
  rota ausente; portas, caso de uso, suíte completa e checkpoint SonarQube permanecem nas etapas
  planejadas.
- 2026-08-13 — usuário autorizou a Task 4.1 ao solicitar explicitamente
  `Próximo item: 4.1 — criar portas e caso de uso atômico.` O teste foi escrito antes da
  implementação e o RED
  `mvn -q "-Dtest=CapturarDossieProdutoCasoDeUsoTest,ArchUnitProgressivoTest" test` falhou na
  compilação somente pela ausência da nova porta de saída.
- 2026-08-13 — a implementação mínima criou as portas `CapturarDossieProduto` e
  `SolicitarCapturaDossieProduto` e o `CapturarDossieProdutoCasoDeUso`. O caso de uso delega
  exatamente uma vez com a mesma instância de `IdentificadorDossieProduto` e devolve diretamente
  o `Uni` da porta, preservando item, item nulo e a mesma falha, sem CDI, adapter, bloqueio ou
  transformação.
- 2026-08-13 — o GREEN do mesmo comando concluiu 36 testes: 3 do novo caso de uso e 33 de
  arquitetura, todos verdes, sem falhas, erros ou ignorados. `git diff --check` não encontrou
  erros, e as novas classes de aplicação não importam adapter, Jakarta ou Quarkus nem usam espera
  bloqueante. O contrato HTTP permanece deliberadamente RED pela rota ausente; CA é o próximo
  item e depende de nova autorização.
- 2026-08-13 — usuário registrou explicitamente `GO CA`. O checkpoint executou
  `mvn -q "-Dtest=FalhasDossieProdutoTest,CapturarDossieProdutoCasoDeUsoTest,ArchUnitProgressivoTest" test`:
  48 testes verdes, sendo 12 de domínio, 3 de aplicação e 33 de arquitetura, sem falhas, erros ou
  ignorados.
- 2026-08-13 — o contrato público foi reexecutado com
  `mvn -q "-Dtest=DossieProdutoApiContractTest,DossieProdutoErroApiContractTest,ResourceEndpointTest" test`:
  50 testes executados, 46 existentes verdes e exatamente os quatro novos cenários falhando com
  `404`. Os dois sucessos esperavam `200`, e os identificadores `0` e `-1` esperavam `400`; a
  causa única permanece a ausência deliberada da rota REST. Não houve erro de compilação,
  bootstrap ou infraestrutura.
- 2026-08-13 — revisão CA nos eixos de correção, legibilidade, arquitetura, segurança e desempenho
  não encontrou achados críticos ou obrigatórios. O núcleo contém somente resultado/falha,
  portas e delegação reativa direta; não possui adapter, CDI, Jakarta, Quarkus, MicroProfile,
  espera bloqueante, log, segredo, entrada externa, dependência nova ou comportamento futuro.
  `git diff --check` permaneceu sem erros, o `pom.xml` não foi alterado e `.tools/` foi preservado
  fora do escopo. Checkpoint CA concluído; a Task 5.1 depende de nova autorização.
- 2026-08-13 — usuário registrou explicitamente `GO 5.1`. O Swagger foi reconfirmado com sucesso
  `200 application/json`, propriedade obrigatória `id` e formato `int64`. O teste foi escrito
  antes da implementação, e o RED
  `mvn -q "-Dtest=CapturaDossieProdutoMtrMapperTest,ArchUnitProgressivoTest" test` falhou na
  compilação somente porque o pacote do DTO de captura ainda não existia.
- 2026-08-13 — a implementação mínima criou `CapturaDossieProdutoMtrResponse` com
  `@JsonProperty("id") Long id` e `CapturaDossieProdutoMtrMapper`, CDI e exclusivo da borda MTR.
  O mapper traduz diretamente para `ResultadoCapturaDossieProduto`, preservando resposta nula,
  identificador nulo e identificador divergente para validação posterior pelo adapter. A primeira
  tentativa de GREEN revelou somente uso incorreto da API de reflexão no próprio teste; após a
  correção para `getDeclaredMethod`, nenhuma mudança de produção adicional foi necessária.
- 2026-08-13 — o GREEN do mesmo comando concluiu 37 testes: 4 de desserialização/mapeamento e 33
  de arquitetura, todos verdes, sem falhas, erros ou ignorados. O `Long.MAX_VALUE` foi
  desserializado e mapeado sem perda, o nome JSON explícito foi verificado e não há dependência de
  REST, simulador nem espera bloqueante. `git diff --check` não encontrou erros; suíte completa e
  checkpoint SonarQube permanecem previstos para C2, pois o contrato público continua
  deliberadamente RED até a implementação da rota.
- 2026-08-13 — usuário registrou explicitamente `GO 6.1`. O teste foi escrito antes da
  implementação, e o RED
  `mvn -q "-Dtest=CapturaDossieProdutoMtrClientTest,ArchUnitProgressivoTest" test` falhou na
  compilação somente pela ausência do novo erro protocolar/client. O checklist complementar
  `security-checklist.md` indicado pela skill não estava instalado; foram aplicadas as regras
  principais da skill, do `AGENTS.md`, do plano e do C1 aprovado.
- 2026-08-13 — a implementação mínima criou `CapturaDossieProdutoMtrClient` com config key e
  providers existentes, `POST /v1/dossie-produto/{id}/capturar`, somente path param, sem body ou
  `@Consumes`, `Accept: application/json` e retorno reativo. A política contém timeout de 2.000 ms
  e circuit breaker `10/0,5/10.000 ms/2`; falhas de servidor, processamento e timeout contam,
  enquanto negócio e técnica cliente são ignoradas. Não existe `@Retry`.
- 2026-08-13 — `CapturaDossieProdutoMtrException` preserva corpo válido e classifica `400`, `404`,
  `409` e `422` como negócio, demais `4xx` como técnica cliente e `5xx` como servidor. Corpo
  ausente, nulo ou malformado recebe fallback sem detalhe ou stacktrace. O GREEN do mesmo comando
  concluiu 41 testes: 8 do client/erro e 33 de arquitetura, todos verdes, sem falhas, erros ou
  ignorados. A revisão confirmou URL fixo, providers de API key/OIDC/trace, ausência de segredo,
  log, body, `@Consumes` e `@Retry`, além de `pom.xml` intacto e `git diff --check` sem erros.
- 2026-08-13 — usuário registrou explicitamente `GO 7.1`. O teste foi escrito antes da
  implementação, e o RED
  `mvn -q "-Dtest=CapturaDossieProdutoMtrAdapterTest,ArchUnitProgressivoTest" test` falhou na
  compilação somente pela ausência de `CapturaMtr` e `CapturaDossieProdutoMtrAdapter`. A referência
  complementar `observability-checklist.md` indicada pela skill não estava instalada; foram
  aplicados o checklist principal da skill, os contratos locais, o plano e o C1 aprovado.
- 2026-08-13 — o adapter MTR implementa `SolicitarCapturaDossieProduto`, injeta client e mapper,
  valida resposta nula, identificador nulo e identificador divergente antes do mapper e traduz
  negócio, técnica cliente, servidor, timeout e falha inesperada para as categorias internas,
  preservando status, campos protocolares e causa. O qualifier exclusivo `CapturaMtr` foi criado.
- 2026-08-13 — a telemetria usa span CLIENT `mtr.dossie-produto.capturar`, rota templated e eventos
  estáveis `mtr.dossie-produto.captura.chamada.*`; registra somente serviço, API, método,
  identificador, resultado e tipo da falha. O teste confirmou que mensagem, detalhe, stacktrace e
  sentinelas de token/API key externos não aparecem em logs, atributos, eventos ou status do span,
  e que nenhum throwable externo é anexado ao log.
- 2026-08-13 — o GREEN do mesmo comando concluiu 41 testes: 8 do adapter/telemetria e 33 de
  arquitetura, todos verdes, sem falhas, erros ou ignorados. A revisão confirmou ausência de URL
  concreta, `recordException`, mensagem externa, log de throwable, segredo e retry no adapter;
  `pom.xml` permaneceu intacto e `git diff --check` não encontrou erros. O checkpoint CB depende
  de nova autorização.
- 2026-08-13 — usuário registrou explicitamente `GO CB`. O checkpoint executou
  `mvn -q "-Dtest=CapturaDossieProdutoMtrMapperTest,CapturaDossieProdutoMtrClientTest,CapturaDossieProdutoMtrAdapterTest,RequestHeaderFactoryTest,RestClientObservabilityFilterTest,ArchUnitProgressivoTest" test`:
  60 testes verdes, sendo 4 de DTO/mapper, 8 de client/erro, 8 de adapter, 1 de API key, 6 do
  filtro de observabilidade e 33 de arquitetura, sem falhas, erros ou ignorados.
- 2026-08-13 — a matriz foi confirmada: `400/404/409/422` são negócio, demais `4xx` são técnica
  cliente e `5xx` são dependência indisponível; resposta nula, id ausente ou divergente não chega
  ao mapper. Timeout de 2.000 ms e circuit breaker `10/0,5/10.000 ms/2` coincidem com C1. Busca
  restrita ao client e adapter da captura confirmou ausência de import ou annotation `Retry`.
- 2026-08-13 — API key e bearer permanecem ligados somente ao client MTR por
  `RequestHeaderFactory` e `OidcClientRequestReactiveFilter`; os testes existentes do mesmo padrão
  de REST Client comprovam também a propagação automática de `traceparent`. A prova de wire
  específica da captura, inclusive número de chamadas, permanece corretamente reservada à Task
  13.1.
- 2026-08-13 — a auditoria CB encontrou um achado obrigatório de observabilidade: o novo client
  registra `RestClientObservabilityFilter`, enquanto a configuração principal habilita payload
  (`simtr-hub.observabilidade.rest-client.payload.habilitado=true`). Esse filtro acrescenta URL
  concreta e corpos de request/response aos spans e logs. Em erro MTR, isso pode registrar
  `mensagem`, `detalhe` e `stacktrace` externos, contrariando a segurança e a lista de atributos
  permitidos aprovadas em C1. O teste do adapter não detectava a divergência porque usa client
  mockado e, portanto, não executa o provider.
- 2026-08-13 — revisão nos cinco eixos não encontrou outro achado crítico ou obrigatório. DTO,
  mapper, erro protocolar, client, adapter e qualifier permanecem pequenos, reativos, isolados e
  sem dependência nova. `git diff --check` ficou limpo, `pom.xml` permaneceu intacto e `.tools/`
  foi preservado. CB fica `EM AJUSTE`, sem alteração de produção/teste/tooling neste checkpoint;
  a correção recomendada é retirar o filtro genérico somente do client de captura, manter
  API key/OIDC/trace e a telemetria segura do adapter e criar uma regressão que proíba o provider.
- 2026-08-13 — usuário registrou explicitamente `GO ajuste CB` e reforçou que código de produção
  não relacionado não deve ser alterado. O ajuste foi autorizado com escopo estrito aos arquivos
  do novo client de captura e de seu teste; provider compartilhado, configuração global e demais
  clients permanecem fora do escopo.
- 2026-08-13 — o RED do ajuste executou
  `mvn -q "-Dtest=CapturaDossieProdutoMtrClientTest" test`: 8 testes executados, 7 verdes e
  exatamente 1 falha porque o client ainda registrava `RestClientObservabilityFilter`. A correção
  mínima removeu somente o import e `@RegisterProvider` desse filtro no client de captura; API
  key por `RequestHeaderFactory`, bearer por `OidcClientRequestReactiveFilter`, propagação de
  contexto OpenTelemetry e a telemetria segura do adapter foram preservados.
- 2026-08-13 — o GREEN focado concluiu os 8 testes do client sem falhas, erros ou ignorados. A
  repetição completa de CB concluiu 60 testes verdes: 4 de DTO/mapper, 8 de client/erro, 8 de
  adapter, 1 de API key, 6 do filtro compartilhado e 33 de arquitetura. A regressão exige que o
  novo client possua somente o provider OIDC e proíbe explicitamente o filtro que registra
  payload; busca restrita ao client/adapter confirmou também ausência de `Retry`.
- 2026-08-13 — o escopo final do ajuste contém somente o novo client de captura, seu teste e este
  registro. `RestClientObservabilityFilter`, configurações, `pom.xml`, demais clients e código de
  produção não relacionado permaneceram intactos. A revisão de correção, simplicidade,
  arquitetura, segurança, desempenho, testes e escopo não encontrou achado crítico ou obrigatório
  remanescente.
- 2026-08-13 — `./validar-checkpoint-sonarqube.ps1` confirmou SonarQube local `UP`, mas não chegou
  ao scanner: o `mvn clean verify` executou 530 testes e parou com exatamente os quatro contratos
  públicos deliberadamente RED da Task 2.1 (`404` enquanto a rota ainda não existe), sem erros ou
  ignorados. Nenhuma análise Sonar nova foi publicada e não existe situação técnica
  `NON_COMPLIANT` nem decisão humana Sonar pendente; o checkpoint efetivo permanece em C2, quando
  a fatia vertical tornará esses contratos verdes. CB concluído; a Task 8.1 depende de novo GO.
- 2026-08-13 — usuário registrou explicitamente `GO 8.1`. O escopo autorizado contém somente a
  fixture sintética, o DTO, o mapper e seu teste usando o reader real; adapter, qualifier,
  producer, Resource e código compartilhado permanecem fora do escopo. Antes da alteração
  executável, o plano foi alinhado ao ajuste CB já aprovado: o novo client preserva API key,
  OIDC e contexto OpenTelemetry sem registrar o filtro genérico que publicava payload/URL.
- 2026-08-13 — o teste da Task 8.1 foi escrito antes da implementação. O RED
  `mvn -q "-Dtest=CapturaDossieProdutoSimuladorMapperTest" test` falhou na compilação somente pela
  ausência de `CapturaDossieProdutoSimuladorResponse`, comprovando que o novo contrato ainda não
  existia.
- 2026-08-13 — a implementação mínima criou a fixture
  `mock/dossieproduto/captura-dossie-produto.md`, contendo apenas o id sintético `123` e o endpoint
  MTR simulado, além de DTO e mapper exclusivos da borda simulador. O teste usa o
  `MarkdownJsonMockReader` real, verifica desserialização, `@JsonProperty("id")`, ausência de PII,
  credenciais e stacktrace e confirma que resposta/id nulos ou id divergente permanecem
  detectáveis para rejeição pelo adapter da Task 9.1. O mapper não importa REST/MTR e não recebe
  timeout, retry ou circuit breaker.
- 2026-08-13 — o GREEN
  `mvn -q "-Dtest=CapturaDossieProdutoSimuladorMapperTest,ArchUnitProgressivoTest" test` concluiu
  38 testes, sendo 5 da nova fixture/DTO/mapper e 33 de arquitetura, todos verdes, sem falhas,
  erros ou ignorados. A revisão de correção, simplicidade, arquitetura, segurança, desempenho,
  testes e escopo não encontrou achado crítico ou obrigatório; adapter, qualifier, producer,
  Resource, configuração, `pom.xml` e código compartilhado permaneceram intactos.
- 2026-08-13 — a tentativa obrigatória de checkpoint
  `./validar-checkpoint-sonarqube.ps1` confirmou SonarQube local `UP`, compilou 296 fontes e 143
  testes e executou 535 testes. O `verify` parou com exatamente os quatro contratos públicos RED
  da Task 2.1, ainda respondendo `404` porque a rota será ligada apenas nas Tasks 10 a 12; os 531
  demais testes passaram, sem erros ou ignorados. O scanner não foi alcançado, nenhuma análise
  Sonar nova foi publicada e não há decisão humana Sonar pendente. Task 8.1 concluída; a Task 9.1
  depende de novo GO.
- 2026-08-13 — o teste da Task 9.1 foi escrito antes da implementação. O RED
  `mvn -q "-Dtest=CapturaDossieProdutoSimuladorAdapterTest,ArchUnitProgressivoTest" test` falhou
  na compilação somente pela ausência de `CapturaSimulador` e
  `CapturaDossieProdutoSimuladorAdapter`, sem falha em código existente.
- 2026-08-13 — a implementação mínima criou o qualifier exclusivo `CapturaSimulador` e o adapter
  que implementa `SolicitarCapturaDossieProduto`, lê somente a fixture própria e valida resposta
  nula, id ausente ou divergente antes do mapper. Identificador não encontrado retorna
  `FalhaCapturaDossieProduto` de negócio com status `404`, código
  `DOSSIE_PRODUTO_NAO_ENCONTRADO` e mensagem controlada; o caminho de sucesso devolve o id `123`
  sem rede.
- 2026-08-13 — a telemetria do simulador registra somente o evento estável
  `simtr-hub.dossie-produto.captura.simulador.usado`, origem `mock` e identificador. O teste
  comprovou que nenhum span CLIENT é criado, e a busca restrita confirmou ausência de imports MTR,
  REST Client, `WithSpan`, timeout, retry ou circuit breaker nas duas novas classes de produção.
- 2026-08-13 — o GREEN focado concluiu 40 testes, sendo 7 do adapter e 33 de arquitetura. A
  verificação ampliada concluiu 45 testes verdes, acrescentando os 5 testes da fixture/DTO/mapper,
  sem falhas, erros ou ignorados. A revisão de correção, simplicidade, arquitetura, segurança,
  desempenho, testes e escopo não encontrou achado crítico ou obrigatório; `pom.xml`, producer,
  Resource, configuração, borda MTR e código de produção não relacionado permaneceram intactos.
- 2026-08-13 — o checkpoint obrigatório `./validar-checkpoint-sonarqube.ps1` confirmou o servidor
  local `UP`, compilou 298 fontes e 144 testes e executou 542 testes. O `verify` parou com os mesmos
  quatro contratos públicos deliberadamente RED da Task 2.1, ainda respondendo `404`; os outros
  538 testes passaram, sem erros ou ignorados. O scanner não foi alcançado, nenhuma análise Sonar
  nova foi publicada e não há decisão humana Sonar pendente. Task 9.1 concluída; a Task 10.1
  depende de novo GO.
- 2026-08-13 — o teste da Task 10.1 foi escrito antes da implementação. O RED
  `mvn -q "-Dtest=CapturaDossieProdutoPortasProducerTest,ArchUnitProgressivoTest" test` falhou na
  compilação somente porque `CapturaDossieProdutoPortasProducer` ainda não existia, sem falha em
  código existente.
- 2026-08-13 — a implementação mínima criou um producer CDI próprio para
  `SolicitarCapturaDossieProduto`. O método `@Produces @ApplicationScoped` recebe exclusivamente
  as portas qualificadas por `CapturaMtr` e `CapturaSimulador` e reutiliza
  `simtr-hub.simulador.dossie-produto.habilitado`: `true` devolve exatamente o simulador e `false`
  devolve exatamente o MTR, sem executar qualquer adapter durante a seleção.
- 2026-08-13 — o GREEN focado concluiu 36 testes, sendo 3 do producer e 33 de arquitetura. A
  verificação ampliada concluiu 51 testes verdes, acrescentando os 8 do adapter MTR e os 7 do
  adapter simulador, sem falhas, erros ou ignorados. O bootstrap Quarkus confirmou uma única porta
  default, satisfeita e não ambígua.
- 2026-08-13 — a revisão de correção, simplicidade, arquitetura, segurança, desempenho, testes e
  escopo não encontrou achado crítico ou obrigatório. Não foi criada nova property, dependência,
  rede, telemetria, segredo ou configuração; adapters, REST, Resource, `pom.xml` e código de
  produção não relacionado permaneceram intactos.
- 2026-08-13 — o checkpoint obrigatório `./validar-checkpoint-sonarqube.ps1` confirmou SonarQube
  local `UP`, compilou 299 fontes e 145 testes e executou 545 testes. O `verify` parou com os mesmos
  quatro contratos públicos deliberadamente RED da Task 2.1; os outros 541 testes passaram, sem
  erros ou ignorados. O scanner não foi alcançado, nenhuma análise Sonar nova foi publicada e não
  há decisão humana Sonar pendente. Task 10.1 concluída; a Task 11.1 depende de novo GO.
- 2026-08-13 — o teste da Task 11.1 foi escrito antes da implementação. O RED
  `mvn -q "-Dtest=CapturaDossieProdutoRestMapperTest,ArchUnitProgressivoTest" test` falhou na
  compilação somente pela ausência de `CapturaDossieProdutoResponse`, comprovando que o novo
  contrato REST ainda não existia.
- 2026-08-13 — a implementação mínima criou `CapturaDossieProdutoResponse`, com exatamente o
  campo JSON explícito `id`, e `CapturaDossieProdutoRestMapper`, exclusivo da borda REST. O mapper
  converte sucesso sem reutilizar DTO MTR/simulador, preserva status e corpo MTR válido nas classes
  públicas existentes e converte qualquer falha sem status em erro `500` controlado, sem detalhe,
  stacktrace, causa ou conteúdo externo sensível.
- 2026-08-13 — o GREEN focado concluiu 38 testes, sendo 5 do novo DTO/mapper e 33 de arquitetura.
  A verificação ampliada, incluindo os cinco mappers REST análogos existentes, concluiu 58 testes
  verdes, sem falhas, erros ou ignorados. A revisão de correção, simplicidade, arquitetura,
  segurança, desempenho, testes e escopo não encontrou achado crítico ou obrigatório; Resource,
  rota, observabilidade, bordas MTR/simulador, mappers existentes e `pom.xml` permaneceram
  intactos.
- 2026-08-13 — o checkpoint obrigatório `./validar-checkpoint-sonarqube.ps1` confirmou SonarQube
  local `UP`, compilou 301 fontes e 146 testes e executou 550 testes. O `verify` parou com os mesmos
  quatro contratos públicos deliberadamente RED da Task 2.1; os outros 546 testes passaram, sem
  erros ou ignorados, inclusive os 5 testes do mapper REST da captura. O scanner não foi
  alcançado, nenhuma análise Sonar nova foi publicada e não há decisão humana Sonar pendente.
  Task 11.1 concluída; a Task 12.1 depende de novo GO.
- 2026-08-13 — o RED inicial de 12.1 reexecutou os 50 contratos públicos: 46 cenários existentes
  permaneceram verdes e exatamente os quatro cenários da captura falharam com `404`. O RED
  específico seguinte falhou na compilação somente pela ausência de
  `CapturaDossieProdutoObservabilidade`, antes de qualquer alteração de produção da task.
- 2026-08-13 — a implementação ligou `POST /simtr-hub/v1/dossie-produto/{id}/capturar` à porta de
  entrada, com `@Consumes(MediaType.WILDCARD)`, validação de id, resposta `200 {"id":123}` e matriz
  OpenAPI aprovada. O wrapper de aplicação usa span INTERNAL, origem `mtr|mock` e eventos de
  processamento estáveis; API e aplicação não registram payload, mensagem ou throwable externo.
- 2026-08-13 — o GREEN focado concluiu 40 testes, os 50 contratos públicos ficaram verdes e a
  bateria ampliada da capacidade concluiu 145 testes verdes. A revisão multi-eixo não encontrou
  achado crítico ou obrigatório no comportamento; código compartilhado, adapters existentes,
  configuração, dependências e `pom.xml` permaneceram intactos.
- 2026-08-13 — `./validar-checkpoint-sonarqube.ps1` confirmou SonarQube `UP`, compilou 302 fontes
  e 148 testes, executou 557 testes verdes e publicou a análise
  `5af51d30-20af-4938-af35-3e50b9a68fca`. Cobertura de 86,9% e duplicação de 3,7% atendem aos
  limites, porém a avaliação ficou `NON_COMPLIANT`: 217 issues abertas contra 213 no baseline,
  com quatro novas e uma HIGH. As issues são `S1192` HIGH no wrapper novo (literal `resultado`),
  `S1710` e `S1905` MINOR na nova rota e `S7467` MINOR no client novo da captura criado na Task
  6.1 e analisado pela primeira vez. A decisão humana permanece `PENDING`; nenhuma correção foi
  aplicada após essa avaliação.
- 2026-08-13 — o usuário escolheu `ContinuarAjustes`, e a decisão foi registrada por
  `./validar-checkpoint-sonarqube.ps1 -HumanDecision ContinuarAjustes` antes de qualquer correção.
  Os ajustes ficaram restritos às quatro issues da captura: constante para a chave `resultado`,
  anotações OpenAPI repetíveis sem o contêiner redundante, remoção do cast redundante no mapper e
  unnamed pattern no `catch` do novo client. O teste de reflexão foi adaptado para ler as mesmas
  sete respostas OpenAPI sem alterar o contrato.
- 2026-08-13 — a bateria focada de Resource, observabilidade, client MTR e ArchUnit ficou verde.
  O checkpoint completo seguinte compilou 302 fontes e 148 testes, executou 131 suítes e 557 testes
  sem falhas, erros ou ignorados e publicou a análise
  `d5a1a719-1304-44db-a4bb-cd7b0bf918ca`. O estado técnico ficou `COMPLIANT`, com 213 issues atuais
  e 213 no baseline, zero issue nova, zero issue HIGH/BLOCKER nova, cobertura de 86,9%, duplicação
  de 3,7% e decisão `NOT_REQUIRED`. Task 12.1 concluída; C2 depende de novo GO.
- 2026-08-13 — o usuário registrou `GO C2`. O fingerprint executável atual
  `a9b42faea37d4bea803b7bc1faf5eba829ba591539b68b5c5b7064f3f92eb483` é idêntico ao da análise
  `COMPLIANT`; por isso, o mesmo Maven/Sonar não foi repetido sem alteração de código. Os relatórios
  preservados confirmam 131 suítes e 557 testes, sem falhas, erros ou ignorados, incluindo 50
  contratos públicos e 33 regras ArchUnit verdes.
- 2026-08-13 — a revisão C2 nos eixos de correção, simplicidade, arquitetura, segurança e
  desempenho não encontrou achado crítico ou obrigatório. A rota aceita POST sem corpo, valida o
  id, preserva o JSON e os status aprovados, usa fluxo reativo sem espera bloqueante ou retry,
  mantém DTOs/mappers independentes e não registra credenciais, payload ou conteúdo externo nos
  sinais específicos da captura. O crescimento do `DossieProdutoResource` permanece como risco
  já registrado no plano; uma extração maior exige plano/GO próprios para não alterar código não
  relacionado. C2 concluído; a Task 13.1 depende de `GO 13.1`.
- 2026-08-14 — o usuário registrou explicitamente `GO 13.1`. O escopo executável ficou restrito a
  duas provas Quarkus de integração: rota pública até o stub MTR e seleção do simulador com o MTR
  deliberadamente indisponível em `http://localhost:1`. Produção, configuração, stub compartilhado,
  dependências e `.tools/` permaneceram intactos.
- 2026-08-14 — `CapturaDossieProdutoMtrContractTest` comprovou `POST` no wire exato, corpo vazio,
  `Accept`, API key, bearer e `traceparent`; preservação ponta a ponta de `400/401/403/404/409/500`;
  fallback seguro para corpo inválido; e exatamente uma chamada tanto em `500` quanto em timeout.
  `CapturaDossieProdutoSelecaoSimuladorQuarkusTest` comprovou sucesso `200 {"id":123}` pela rota
  pública mesmo com o endpoint MTR inacessível. A bateria focada executou 10 testes verdes.
- 2026-08-14 — a execução real revelou divergência em relação à conclusão registrada no ajuste
  CB: `RestClientObservabilityFilter` continua sendo executado para a captura porque é um
  `@Provider` global, embora não esteja registrado no novo client. O profile de teste desabilita
  payload, mas a configuração principal o habilita e o filtro registra URL e corpos. A conformidade
  de sigilo dos sinais, portanto, permanece não comprovada e deve ser caracterizada na Task 14.1;
  qualquer correção compartilhada de observabilidade exige checkpoint e GO próprios.
- 2026-08-14 — como o baseline pertencia à sessão anterior e não existe diretório `sonar/`, o
  baseline somente local foi reinicializado sobre o fingerprint de C2, retirando temporariamente
  apenas os dois testes novos e restaurando-os sem alteração antes do checkpoint. A análise do
  incremento `6af6e3b3-45cc-40d6-a83e-ce77909a62b6`, fingerprint
  `af2ea18a722a95f7e90cc6fbdf055ab4890cf3e4ff4862ea9b9cb1a8d4938e38`, executou 133 suítes e 567
  testes sem falhas, erros ou ignorados. O estado ficou `COMPLIANT`: 213 issues atuais e 213 no
  baseline, nenhuma issue nova ou HIGH/BLOCKER nova, cobertura de 87,9%, duplicação de 3,7% e
  decisão `NOT_REQUIRED`.
- 2026-08-14 — a revisão de correção, simplicidade, arquitetura, segurança, desempenho, testes e
  escopo não encontrou problema nos dois testes adicionados. Task 13.1 concluída; a Task 14.1,
  incluindo a caracterização do provider global observada em runtime, depende de `GO 14.1`.
- 2026-08-14 — o usuário registrou explicitamente `GO 14.1`. A primeira fatia TDD habilitou no
  stub somente o mesmo comportamento de payload ativo da configuração principal e acrescentou um
  contrato de trace MTR com erro sintético. A execução isolada concluiu 10 testes, com os 9
  contratos anteriores verdes e exatamente o novo contrato RED. Antes da cláusula de sigilo, ele
  confirmou a cadeia `SERVER -> INTERNAL -> CLIENT`, trace contínuo, `http.route` público e
  `url.path` MTR templated.
- 2026-08-14 — o RED comprovou que o provider global registra no span CLIENT a URL interna
  concreta e o corpo de resposta, incluindo as sentinelas de mensagem, detalhe e stacktrace. A
  instrumentação automática também registrou a mensagem externa em `exception.message` e na
  cadeia de `exception.stacktrace` dos spans CLIENT, INTERNAL e SERVER. API key e bearer não foram
  encontrados no trace. Como a correção muda comportamento observável e alcança um filtro
  compartilhado, nenhuma produção foi alterada; o checkpoint `C14-OBS` depende de
  `GO ajuste 14.1`.
- 2026-08-14 — o usuário registrou `GO ajuste 14.1` e limitou expressamente a correção ao código
  estritamente necessário para a nova funcionalidade. O ajuste autorizado remove somente a
  autodiscovery global do filtro que todos os clients anteriores já registram explicitamente,
  mantém a captura sem esse provider e saneia apenas as mensagens observáveis das exceções da
  captura, preservando status e campos do JSON público. Configuração global, demais clients,
  segurança, fault tolerance e dependências permanecem fora do escopo.
- 2026-08-14 — o GREEN removeu `@Provider` de `RestClientObservabilityFilter`; os dez REST Clients
  anteriores continuam registrando esse filtro explicitamente e sem mudança de comportamento,
  enquanto o client novo da captura mantém somente API key, OIDC e propagação OpenTelemetry. Não
  foi criada configuração, dependência, abstração, provider ou política global. A busca restrita
  ao client/adapter da captura continuou sem `Retry`.
- 2026-08-14 — as exceções compartilhadas ganharam overloads explícitos de mensagem observável,
  mantendo os construtores e o comportamento anteriores. Somente o mapper REST da captura usa os
  overloads seguros; `CapturaDossieProdutoMtrException` e `FalhaCapturaDossieProduto` também
  expõem mensagem genérica na cadeia observável. Status e todos os campos externos aprovados
  permanecem no `ErroPadraoDto` devolvido ao consumidor, sem entrar em mensagem ou stacktrace de
  log/span.
- 2026-08-14 — os contratos globais agora cobrem onze capacidades. No simulador, a captura possui
  árvore `SERVER -> INTERNAL`, origem `mock`, cinco eventos estruturados e nenhum span CLIENT. No
  MTR, o teste runtime comprova `SERVER -> INTERNAL -> CLIENT`, trace contínuo, `http.route` e
  `url.path` templated. O handler de regressão serializa mensagem, MDC e cadeia completa de
  throwable e não encontrou payload, PII, API key, bearer, `traceparent`, mensagem/detalhe/
  stacktrace externos nem URL `127.0.0.1` nos sinais próprios da captura.
- 2026-08-14 — a bateria focada de nove classes executou 89 testes verdes, incluindo 10 contratos
  runtime MTR, 3 contratos globais de logs, 2 de spans, 10 do bean Resource, 33 regras ArchUnit e
  31 regressões unitárias de erro/client/mapper/filtro. Após acrescentar a asserção negativa do
  `traceparent`, os 10 contratos MTR foram reexecutados e permaneceram verdes. `git diff --check`
  não encontrou erro.
- 2026-08-14 — limitação mantida explícita para o C3: além dos três spans contratuais próprios da
  captura, o Quarkus cria um span HTTP Client automático com o atributo semântico padrão
  `url.full`. A versão instalada oferece desativação da instrumentação REST Client somente em
  escopo global; alterá-la afetaria clients existentes e violaria o limite autorizado. Por isso,
  o critério global de ausência de URL interna completa permanece aberto para revisão humana no
  C3, sem mascarar o sinal nem ampliar o código da feature. Task 14.1 concluída; C3 depende de
  `GO C3`.
- 2026-08-14 — o usuário registrou explicitamente `GO C3`. O checkpoint fica autorizado somente
  para reexecutar a bateria focada, a suíte completa pelo script oficial, o SonarQube e a revisão
  de wire, seleção sem rede e sigilo; eventual correção executável exige nova evidência e decisão.
- 2026-08-14 — a bateria focada do C3, com wire MTR, seleção do simulador sem rede, contratos
  públicos, logs, spans, Resource e 33 regras ArchUnit, ficou integralmente verde. O script oficial
  seguinte confirmou SonarQube local `UP`, compilou 302 fontes e 150 testes, mas o `clean verify`
  parou antes do scanner com três falhas em 569 testes distribuídos por 133 suítes; nenhuma análise
  Sonar nova foi publicada.
- 2026-08-14 — a triagem localizou uma única causa: a caracterização RED de 14.1 acrescentou
  `simtr-hub.observabilidade.rest-client.payload.habilitado=true` ao
  `DossieProdutoMtrStubTestResource`, compartilhado com as operações de consulta e produto. Isso
  sobrescreveu o profile de teste que mantém payload desabilitado e fez dois contratos de consulta
  reencontrarem CPF/erro externo e um contrato de produto observar
  `rest_client.payload.enabled=true`. A reprodução isolada executou 10 testes e repetiu exatamente
  as mesmas três falhas.
- 2026-08-14 — a correção de raiz proposta remove somente essa entrada de configuração do stub.
  Ela não é mais necessária após a captura deixar de autodetectar o filtro compartilhado, e sua
  remoção restaura o isolamento dos contratos antigos sem tocar produção, configuração principal,
  client, segurança ou comportamento da captura. C3 permanece `EM AJUSTE` até
  `GO ajuste C3`.
- 2026-08-14 — o usuário registrou explicitamente `GO ajuste C3`. O escopo autorizado contém
  somente a remoção do override de payload no stub compartilhado, a reexecução das provas afetadas
  e a retomada das verificações do C3; produção e configuração principal permanecem intactas.
- 2026-08-14 — o ajuste removeu somente
  `simtr-hub.observabilidade.rest-client.payload.habilitado=true` do stub compartilhado, sem delta
  líquido adicional nesse arquivo em relação ao estado anterior à caracterização. O GREEN isolado
  reexecutou os 10 contratos de consulta e produto que haviam reproduzido a regressão, todos sem
  falhas, erros ou ignorados. A bateria ampliada de 12 classes, cobrindo wire MTR, seleção do
  simulador sem rede, contratos públicos, logs, spans, Resource e ArchUnit, também ficou verde.
- 2026-08-14 — `./validar-checkpoint-sonarqube.ps1` confirmou SonarQube local `UP`, compilou 302
  fontes e 150 testes, executou 133 suítes e 569 testes, todos verdes, e publicou a análise
  `03f3527a-be4c-4647-8c57-e4c784eec7a8`. Cobertura de 88,0% e duplicação de 3,3% atendem aos
  limites, mas o estado técnico ficou `NON_COMPLIANT`: 218 issues atuais contra 213 no baseline,
  cinco novas e uma HIGH. A decisão humana permanece `PENDING`.
- 2026-08-14 — as cinco issues foram lidas como dados no Sonar local: `S1192` HIGH no novo
  `FalhaCapturaDossieProduto`, por repetir a mensagem já definida em constante; três `S6213` nos
  parâmetros `record` do novo handler de teste MTR; e `S5778` na lambda de `assertThrows` que cobre
  a nova rota em `ResourceBeanCoverageTest`. Os ajustes candidatos são locais e mecânicos, mas não
  foram aplicados sem a decisão humana obrigatória `Reprovar`, `AceitarExcepcionalmente` ou
  `ContinuarAjustes`.
- 2026-08-14 — o usuário escolheu `ContinuarAjustes`, e a decisão foi registrada por
  `./validar-checkpoint-sonarqube.ps1 -HumanDecision ContinuarAjustes` antes de qualquer correção.
  O ajuste reutilizou `MENSAGEM_OBSERVAVEL` no construtor da nova falha, renomeou somente os três
  parâmetros `record` do handler novo para `registro` e separou o `await()` da nova asserção de
  falha seguindo o padrão já existente no mesmo teste. Nenhum contrato, comportamento, produção
  compartilhada, configuração ou dependência foi acrescentado.
- 2026-08-14 — o GREEN focado das três classes afetadas encerrou com código zero. O checkpoint
  oficial seguinte recompilou 302 fontes e 150 testes, executou 133 suítes e 569 testes sem falhas,
  erros ou ignorados e publicou a análise `e53a5942-6786-4746-afbe-c1a61fde9fb0`, fingerprint
  `45446f10c2411169f43a49e4cada47a39ce09a0c1b299d3837f63d9483e54449`. O estado ficou
  `COMPLIANT`: 213 issues atuais e 213 no baseline, zero issue nova ou severa, cobertura de 88,0%,
  duplicação de 3,3% e decisão `NOT_REQUIRED`.
- 2026-08-14 — a revisão C3 de correção, simplicidade, arquitetura, segurança e desempenho não
  encontrou achado crítico ou obrigatório no ajuste. O client mantém o wire aprovado, timeout e
  circuit breaker sem `Retry`; os testes preservam headers, uma chamada em falha/timeout, seleção
  do simulador com MTR indisponível e sigilo dos sinais próprios. `pom.xml`, `.tools/` e código não
  relacionado permaneceram intactos. Resta somente a decisão humana já prevista sobre aceitar o
  `url.full` criado pela instrumentação HTTP automática ou autorizar uma mudança global que afeta
  os demais REST Clients; C3 não foi aprovado por inferência.
- 2026-08-14 — o usuário registrou explicitamente `Reprovar C3`. A decisão reprova o checkpoint de
  integração e sigilo por causa do `url.full` no span HTTP Client automático; ela não altera o
  estado SonarQube atual, que permanece `COMPLIANT/NOT_REQUIRED`, e por isso não foi executado
  `-HumanDecision Reprovar`. Nenhuma correção adicional, mudança global, documentação de 15.1 ou
  item posterior foi iniciada. Uma retomada exige nova definição humana de escopo, atualização do
  plano/checklist e os checkpoints aplicáveis antes de qualquer alteração executável.
- 2026-08-14 — o usuário registrou `GO replanejar C3 para eliminar url.full`. O GO autorizou
  somente a atualização de `plan.md` e `todo.md`; nenhum código, teste executável, tooling,
  configuração, Maven ou SonarQube foi alterado/executado neste replanejamento.
- 2026-08-14 — a investigação confirmou nas fontes instaladas do Quarkus 3.33.2.1 e Vert.x 4.5.28
  que o span automático obtém `url.full` da URI absoluta e que o REST Client aceita
  `HttpClientOptions` fornecido por provider registrado por client. `TracingPolicy.IGNORE` impede
  o span automático, mas também a propagação do Vert.x; por isso, a solução proposta combina essa
  política local com injeção do contexto corrente pelo `TextMapPropagator` configurado. Esse
  achado substitui a conclusão preliminar registrada em 14.1 de que a única alternativa seria uma
  mudança global, sem apagar o histórico que fundamentou a reprovação do C3.
- 2026-08-14 — a Task 14.2 proposta toca no máximo o client da captura, um novo provider local e
  seus dois testes. O contrato runtime passará a inspecionar todos os spans do mesmo trace, exigir
  ausência de `url.full`/URL interna, exatamente um CLIENT próprio e `traceparent` correspondente
  ao trace/span id desse CLIENT. API key, OIDC, wire, corpo vazio, matriz de status, timeout,
  circuit breaker, zero retry e seleção sem rede permanecem congelados.
- 2026-08-14 — foram descartados do escopo: desabilitar instrumentação global, alterar outros REST
  Clients, ocultar `url.full` no exporter, manter `IGNORE` sem reposição de propagação, adicionar
  dependência ou remover o span CLIENT próprio. O desenho aplica o ADR-0006 já aceito e não cria
  decisão arquitetural nova. A implementação depende de `GO C3-TRACE`; a documentação de 15.1
  continua bloqueada pelo C3 revalidado.
- 2026-08-14 — o usuário registrou explicitamente `GO C3-TRACE`. Ficou aprovada a política de
  observabilidade exclusiva do REST Client MTR da captura: `HttpClientOptions` com
  `TracingPolicy.IGNORE` para suprimir o span HTTP automático e injeção explícita do contexto
  corrente pelo propagador OpenTelemetry configurado para preservar o `traceparent` originado no
  span CLIENT próprio. Configuração global, outros REST Clients, API key, OIDC, fault tolerance,
  wire, contrato público e dependências permanecem fora da mudança.
- 2026-08-14 — o `GO C3-TRACE` registra a decisão humana, mas não antecipa a autorização da Task
  14.2. Nenhum código, teste executável, tooling, Maven ou SonarQube foi alterado/executado neste
  checkpoint. O próximo item depende de `GO 14.2`.
- 2026-08-14 — o usuário registrou explicitamente `GO 14.2`. O incremento autorizado fica
  limitado a fortalecer os dois testes da captura, registrar no novo client um único provider
  local com `TracingPolicy.IGNORE` e reinjetar o contexto corrente pelo propagador OpenTelemetry
  configurado. Configuração global, outros clients, dependências, segurança, fault tolerance,
  wire, contrato público e itens posteriores permanecem fora do escopo.
- 2026-08-14 — antes da primeira alteração executável, a branch foi reconfirmada como
  `feature/capturar-dossie-produto`, o worktree preexistente foi preservado e `sonar/` continuou
  inexistente. O baseline local válido e o checkpoint `COMPLIANT/NOT_REQUIRED` já registrados
  permanecem a referência; a próxima análise completa será feita no C3 após o incremento coerente.
- 2026-08-14 — o contrato runtime foi fortalecido antes da produção para selecionar todos os
  spans do mesmo trace, exigir exatamente um CLIENT próprio, proibir explicitamente `url.full` e
  URL interna completa e comparar trace id/parent id do `traceparent` recebido no stub com o span
  `mtr.dossie-produto.capturar`. O RED executou 10 testes: nove passaram e somente esse contrato
  falhou, mostrando dois CLIENT e o automático
  `POST /dossie-produto/v1/dossie-produto/{id}/capturar` com
  `url.full=http://127.0.0.1:<porta>/simtr/dossie-produto/v1/dossie-produto/123/capturar`.
- 2026-08-14 — a implementação mínima criou
  `CapturaDossieProdutoMtrTracingProvider`, bean CDI sem `@Provider`, registrado exclusivamente em
  `CapturaDossieProdutoMtrClient`. Como `ContextResolver<HttpClientOptions>`, ele aplica
  `TracingPolicy.IGNORE`; como `ClientRequestFilter`, usa o `TextMapPropagator` do OpenTelemetry
  injetado para propagar `Context.current()` nos headers. Nenhum header é montado manualmente e
  nenhum sinal, segredo ou URL é registrado.
- 2026-08-14 — o primeiro GREEN executou os 10 contratos runtime e os oito contratos reflexivos do
  client sem falhas. A bateria ampliada seguinte executou 124 testes em 12 suítes, cobrindo wire,
  API key, bearer, correlação, status, timeout, uma chamada sem retry, seleção do simulador sem
  rede, contrato público, logs, spans, Resources e 33 regras ArchUnit; todos passaram, sem falhas,
  erros ou ignorados. Após um refactor apenas no teste, sua suíte de oito casos permaneceu verde.
- 2026-08-14 — a revisão da Task 14.2 em correção, simplicidade, arquitetura, segurança e
  desempenho não encontrou achado crítico ou obrigatório. A mudança executável ficou nos quatro
  arquivos planejados; configuração global, demais REST Clients, `pom.xml`, dependências, API
  key/OIDC, fault tolerance, contrato público e `.tools/` permaneceram intactos. Não há `@Retry`
  na captura nem `@Provider` no novo bean, e as checagens de whitespace ficaram limpas.
- 2026-08-14 — Task 14.2 concluída. Suíte completa e SonarQube não foram antecipados porque formam
  o próximo checkpoint C3 previsto no plano; portanto, nenhuma conformidade foi inferida para o
  novo fingerprint. A retomada depende de `GO C3`.
- 2026-08-14 — o usuário registrou explicitamente `GO C3`, autorizando revalidar a integração e a
  observabilidade da Task 14.2, executar a suíte completa, publicar a análise SonarQube do novo
  fingerprint e revisar correção, simplicidade, arquitetura, segurança, desempenho e escopo. A
  Task 15.1 permaneceu bloqueada.
- 2026-08-14 — a primeira invocação do checkpoint excedeu o limite inicial do executor e uma
  repetição imediata encontrou `target/test-agents/mockito-core.jar` transitoriamente bloqueado no
  `mvn clean`; ambas terminaram antes de compilar ou publicar análise. Não havia processo Maven ou
  Java órfão — somente o IntelliJ do usuário, que permaneceu intacto — e nenhum arquivo foi
  removido ou alterado para liberar o lock.
- 2026-08-14 — a repetição bem-sucedida de `./validar-checkpoint-sonarqube.ps1` compilou 303 fontes
  e 150 testes e executou 133 suítes com 569 testes, sem falhas, erros ou ignorados. Permaneceram
  verdes 50 contratos públicos e 33 regras ArchUnit. Os contratos runtime da captura comprovam
  exatamente um span CLIENT próprio, `traceparent` associado ao trace/span desse CLIENT e ausência
  de `url.full`, URL interna completa, payload e segredos nos spans e logs inspecionados.
- 2026-08-14 — o SonarQube publicou a análise `2a1d41c1-054c-4c1f-951e-520749b867a6` para o
  fingerprint `80398c64581a8a3e548728042dfd7ef6bcfac8ae80bb6d8c1806bbeb9fc4ce1d`. Cobertura de
  88,0% e duplicação de 3,3% atendem aos limites, e não há issue nova HIGH, BLOCKER ou CRITICAL;
  porém, o estado técnico ficou `NON_COMPLIANT`, com 214 issues atuais contra 213 no baseline e
  uma issue MINOR nova. A decisão humana permanece `PENDING`.
- 2026-08-14 — a issue nova `java:S1612` está na linha 20 de
  `CapturaDossieProdutoMtrTracingProvider`: o Sonar sugere substituir a lambda que chama
  `headers.putSingle(key, value)` pela referência `MultivaluedMap::putSingle`. O ajuste candidato
  é local, mecânico e preserva a propagação configurada, mas não foi aplicado sem a decisão humana
  obrigatória `Reprovar`, `AceitarExcepcionalmente` ou `ContinuarAjustes`.
- 2026-08-14 — a revisão C3 de correção, simplicidade, arquitetura, segurança e desempenho não
  encontrou outro achado crítico ou obrigatório. A política permanece exclusiva do client da
  captura, sem `@Provider` global e sem alterar outros REST Clients; OIDC, API key, wire, timeout,
  circuit breaker, zero retry e seleção do simulador sem rede continuam cobertos. `pom.xml`,
  dependências, configuração global, `.tools/` e código não relacionado permaneceram intactos.
- 2026-08-14 — o usuário escolheu `ContinuarAjustes`, e a decisão foi registrada por
  `./validar-checkpoint-sonarqube.ps1 -HumanDecision ContinuarAjustes` antes da correção. O único
  ajuste substituiu a lambda de `HEADER_SETTER` por `MultivaluedMap::putSingle` no provider
  exclusivo da captura. Trata-se da referência de método semanticamente equivalente indicada
  pelo Sonar; contrato, propagação, configuração e demais clients não foram alterados.
- 2026-08-14 — o GREEN focado executou oito contratos estruturais do client e dez contratos
  runtime MTR, totalizando 18 testes sem falhas. Continuaram comprovados o único span CLIENT
  próprio, a correlação exata do `traceparent`, a ausência de `url.full`/URL interna e a política
  sem retry.
- 2026-08-14 — o checkpoint oficial final recompilou 303 fontes e 150 testes, executou 133 suítes
  e 569 testes sem falhas, erros ou ignorados e publicou a análise
  `6cb78450-d645-4eef-85f9-1971f3f7a76b`, fingerprint
  `bad7346ef5ad5da42049640ef8c856982a656b5af962a0a006d8b62bbc97015f`. O estado voltou a
  `COMPLIANT/NOT_REQUIRED`: 213 issues atuais e 213 no baseline, zero issue nova ou severa,
  cobertura de 88,0% e duplicação de 3,3%.
- 2026-08-14 — a revisão final não encontrou achado crítico ou obrigatório. A checagem de
  whitespace terminou sem erro além dos avisos preexistentes de conversão LF/CRLF; `pom.xml`,
  dependências, configuração global, outros REST Clients, `.tools/` e código não relacionado
  permaneceram intactos. C3 não foi aprovado por inferência: aguarda decisão humana explícita,
  e a Task 15.1 não foi iniciada.
- 2026-08-14 — o usuário registrou explicitamente `Aprovar C3`. A revalidação C3-R2 ficou
  aprovada com 569 testes verdes em 133 suítes, 50 contratos públicos, 33 regras ArchUnit e
  SonarQube `COMPLIANT/NOT_REQUIRED` no fingerprint
  `bad7346ef5ad5da42049640ef8c856982a656b5af962a0a006d8b62bbc97015f`. A decisão não exigiu
  `-HumanDecision`, pois o checkpoint técnico final não possui violação pendente. Nenhuma nova
  execução Maven/Sonar ou alteração executável foi realizada; a Task 15.1 depende de `GO 15.1`.
- 2026-08-14 — o usuário registrou explicitamente `GO 15.1`. O incremento autorizado é
  exclusivamente documental e se limita a `README.md`, arquitetura consolidada, ADR-0002, índice
  de ADRs e este registro de execução. A Task 16.1, catálogo de observabilidade, documentação de
  observabilidade, Postman e formatos derivados permanecem fora do escopo; Maven, SonarQube e
  baseline não serão executados enquanto o incremento continuar somente em Markdown fonte.
- 2026-08-14 — `README.md` e a arquitetura consolidada passaram a registrar a décima primeira
  capacidade do Hub e a oitava de `dossieproduto`, a rota pública
  `POST /simtr-hub/v1/dossie-produto/{id}/capturar`, o wire MTR sem corpo, a política sem retry e
  a seleção MTR/simulador pela property existente. A lista de operações MTR ainda ausentes ficou
  limitada a alterar garantia e cancelar dossiê.
- 2026-08-14 — ADR-0002 e seu índice receberam somente a atualização factual de sete para oito
  capacidades de `dossieproduto`, incluindo captura para edição; o ADR permaneceu `Aceito` e
  nenhum ADR novo foi criado.
- 2026-08-14 — buscas de consistência não encontraram as contagens ou afirmações substituídas nas
  quatro fontes. `git diff --check` encerrou sem erro, apenas com avisos preexistentes de conversão
  LF/CRLF. Nenhum Postman, `.ppt`, `.pptx`, `.pdf` ou `.html` foi alterado. Como o incremento foi
  exclusivamente documental, Maven, SonarQube e baseline não foram executados. Task 15.1
  concluída; a retomada depende de `GO 16.1`.
- 2026-08-14 — o usuário registrou explicitamente `GO 16.1`. O incremento permanece
  exclusivamente documental e se limita a `doc/catalogo-observabilidade.md`,
  `doc/documentacao-simtr-hub-arquitetura-observabilidade.md` e este registro. C4, código,
  configuração, testes, Postman e formatos derivados permanecem fora do escopo; Maven, SonarQube
  e baseline não serão executados.
- 2026-08-14 — a leitura dos contratos executáveis confirmou 11 capacidades, 22 spans manuais no
  caminho simulador, 11 declarações CLIENT e 55 eventos de sucesso. A documentação operacional
  ainda contava nove capacidades e marcava captura e consulta de dossiê como ausentes; essa
  divergência diretamente ligada à matriz atual será corrigida em 16.1 para restarem somente
  alterar garantia e cancelar dossiê como operações não implementadas.
- 2026-08-14 — o catálogo passou a registrar a captura na matriz de capacidades e na borda MTR,
  com os três spans, parentage, cinco eventos do simulador, três eventos MTR, eventos de falha,
  campos de log e atributos de span efetivamente protegidos pelos contratos. Também registra a
  exceção local ao span HTTP automático, o `traceparent` vinculado ao CLIENT próprio e a proibição
  de `url.full`, URL interna, payload, credenciais e conteúdo externo nos sinais da captura.
- 2026-08-14 — a documentação operacional passou a listar as 11 rotas/capacidades, incluindo
  consulta e captura de dossiê, e somente garantia/cancelamento como ausentes. Foram registradas a
  seleção MTR/simulador pela property existente, a política de timeout/circuit breaker sem retry,
  uma chamada em `500`/timeout e a política de tracing exclusiva da captura sem afetar outros
  REST Clients.
- 2026-08-14 — buscas de consistência confirmaram 22 spans, 11 CLIENT, 55 eventos e os nomes da
  captura; as únicas linhas `Nao implementado` restantes correspondem a garantia e cancelamento.
  `git diff --check` encerrou sem erro, apenas com avisos preexistentes de LF/CRLF, e a checagem
  explícita de trailing whitespace ficou limpa. Nenhum Postman, `.ppt`, `.pptx`, `.pdf` ou `.html`
  foi alterado. Como o incremento permaneceu exclusivamente documental, Maven, SonarQube e
  baseline não foram executados. Task 16.1 concluída; a retomada depende de `GO C4`.
- 2026-08-14 — o usuário registrou explicitamente `GO C4` e confirmou que a coleção Postman será
  atualizada manualmente, permanecendo excluída desta feature. O checkpoint ficou restrito à
  validação das fontes Markdown e da lista de arquivos alterados.
- 2026-08-14 — README e arquitetura enumeram 11 capacidades/rotas públicas; somente alteração de
  garantia e cancelamento permanecem ausentes. Rota pública, wire MTR, três spans e eventos da
  captura coincidem com código e contratos executáveis. `git diff --check` das seis fontes
  Markdown ficou limpo, e nenhum arquivo Postman, `.ppt`, `.pptx`, `.pdf` ou `.html` aparece entre
  mudanças rastreadas ou novos arquivos fora de `.tools/`. `pom.xml`, dependências e arquivos não
  autorizados permanecem intactos. Maven, SonarQube e baseline não foram executados porque C4 é
  exclusivamente documental. Checkpoint C4 aprovado; a Task 17.1 depende de `GO 17.1`.
- 2026-08-14 — o usuário registrou explicitamente `GO 17.1`. `mvn -q clean test` recompilou o
  projeto e executou 569 testes em 133 suítes, sem falhas, erros ou ignorados. Os relatórios
  Surefire confirmaram os testes focados da captura, os 50 contratos públicos do recorte
  dossiê/endpoints e as 33 regras ArchUnit verdes.
- 2026-08-14 — o checkpoint oficial executou Maven, SonarScanner e Compute Engine completos e
  publicou a análise `64a26a46-1830-4d5a-b63d-672fffdebc00`, fingerprint
  `bad7346ef5ad5da42049640ef8c856982a656b5af962a0a006d8b62bbc97015f`. O resultado ficou
  `COMPLIANT/NOT_REQUIRED`: 213 issues atuais e 213 no baseline, zero issue nova ou severa,
  cobertura de 88,0% e duplicação de 3,3%, sem decisão humana Sonar pendente.
- 2026-08-14 — a revisão integral de correção, legibilidade/simplicidade, arquitetura, segurança,
  desempenho, testes e escopo não encontrou achado crítico ou obrigatório. O fluxo permanece
  reativo e sem espera bloqueante ou retry; timeout, circuit breaker, chamada MTR única, seleção do
  simulador sem rede, validação de entrada/resposta externa e sigilo dos sinais estão cobertos. As
  alterações compartilhadas das exceções e do filtro correspondem ao `GO ajuste 14.1` e preservam
  os consumidores anteriores. A única observação não bloqueante é o tamanho já registrado do
  `DossieProdutoResource`; extraí-lo exigiria plano e GO próprios.
- 2026-08-14 — foram revisados 55 arquivos substantivos, todos previstos ou previamente
  autorizados. `git diff --check` e a checagem de trailing whitespace dos arquivos novos ficaram
  limpos; `pom.xml`, dependências, tooling, configuração global, Postman, formatos derivados,
  `.tools/` e código não relacionado permaneceram intactos. Task 17.1 concluída; somente o
  checkpoint humano CF permanece pendente.
- 2026-08-14 — o usuário registrou `GO ajuste 17.1` após a auditoria de strings repetidas. O
  escopo foi reaberto exclusivamente para constantes privadas locais em
  `CapturaDossieProdutoMtrContractTest` e `CapturaDossieProdutoRestMapperTest`. Chaves JSON ficam
  literais para documentar o contrato; não serão criados helpers compartilhados nem alterados
  produção, configuração, dependências, Postman ou formatos derivados. Como testes em `src/`
  alteram o fingerprint, o ajuste exige testes focados, suíte completa e checkpoint SonarQube
  antes de devolver a feature ao CF.
- 2026-08-15 — a retomada confirmou as constantes locais já aplicadas no contrato MTR e o relatório
  Surefire com 10 testes verdes, sem falhas, erros ou ignorados. O mapper REST ainda estava
  inalterado no início deste subpasso.
- 2026-08-15 — `CapturaDossieProdutoRestMapperTest` passou a usar quatro constantes privadas locais
  para a mensagem observável e as três sentinelas internas. Cenários, dados, asserts, chaves JSON
  explícitas e comportamento executável foram preservados; nenhum helper compartilhado ou código
  de produção foi alterado.
- 2026-08-15 — o teste focado do mapper REST executou 5 testes sem falhas, erros ou ignorados. A
  revisão de correção, simplicidade, arquitetura, segurança, desempenho e escopo não encontrou
  achado crítico ou obrigatório; a checagem de trailing whitespace do arquivo ficou limpa. Task
  17.1-A concluída; suíte completa, checkpoint SonarQube e revisão final permanecem pendentes em
  17.1-R, e CF continua pendente de decisão humana.
- 2026-08-15 — o usuário registrou explicitamente `GO 17.1-R`. O checkpoint autorizado fica
  restrito à suíte completa, SonarQube, inspeção do diff/whitespace e revisão final; não autoriza
  nova correção executável, commit, publicação nem encerramento CF por inferência.
- 2026-08-15 — `mvn -q clean test` executou 133 suítes e 569 testes, sem falhas, erros ou
  ignorados. Os logs de exceções correspondem aos cenários negativos contratuais; o Maven encerrou
  com código zero e a contagem foi confirmada pelos relatórios Surefire.
- 2026-08-15 — a primeira tentativa do checkpoint oficial foi interrompida antes de Maven/Sonar
  porque o baseline da sessão atual ainda estava em `NOT_REQUIRED_UNTIL_CODE_CHANGE`. Para seguir
  o precedente seguro da feature, os bytes exatos dos dois testes foram preservados e somente as
  constantes do ajuste foram revertidas temporariamente antes de tentar reinicializar o baseline
  local; a diferença byte-level do fingerprint anterior não pôde ser reproduzida e permanece como
  limitação explícita, sem ampliar a alteração semântica usada para a comparação.
- 2026-08-15 — `./validar-checkpoint-sonarqube.ps1 -InitializeBaseline` parou na verificação de
  disponibilidade: `localhost:9000` recusou a conexão. Nenhuma análise, baseline, avaliação
  `COMPLIANT/NON_COMPLIANT` ou decisão humana foi produzida; o estado da sessão permanece sem
  baseline e sem `lastCheckpoint`.
- 2026-08-15 — os dois testes foram restaurados byte a byte, com hashes SHA-256 iguais aos backups,
  e todos os temporários foram removidos. O teste focado do mapper REST foi reexecutado após a
  restauração e manteve 5 testes verdes. `git diff --check` ficou sem erro, além dos avisos
  preexistentes de LF/CRLF, e a checagem explícita de trailing whitespace dos dois testes e deste
  checklist ficou limpa.
- 2026-08-15 — a revisão de correção, simplicidade, arquitetura, segurança, desempenho, testes e
  escopo não encontrou achado crítico ou obrigatório. Produção, configuração, dependências,
  Postman, formatos derivados e `.tools/` permaneceram intactos neste incremento. Task 17.1-R
  continua aberta exclusivamente pelo checkpoint SonarQube indisponível; CF permanece pendente e
  não foi inferido.
- 2026-08-15 — o usuário iniciou o Docker manualmente; o container `sonarqube-simtr-local` ficou
  disponível em `localhost:9000`. O baseline local foi então reinicializado sobre o estado
  semântico anterior às constantes, mantendo explícita a limitação byte-level já registrada. A
  análise `a4aa8552-89cb-457e-a98f-50cd0ed326a9`, fingerprint
  `de849bcfecf2c465e11c532b50ed6c90876a65464bae1899122d0cbd2dba0309`, executou 133 suítes e 569
  testes verdes e registrou 213 issues, cobertura 88,0%, duplicação 3,3% e
  `COMPLIANT/NOT_REQUIRED`.
- 2026-08-15 — os dois testes atuais foram restaurados byte a byte antes do checkpoint, com hashes
  iguais aos backups. O script oficial publicou a análise
  `0a51bf8a-180b-41a7-ad4a-f338f2f27a35`, fingerprint
  `478ede4feadf45af5cab9638fe36433d26a16f2f1a089f5e5b9c97af81f7ec6c`, e reexecutou 133 suítes e
  569 testes sem falhas, erros ou ignorados. O estado ficou `COMPLIANT/NOT_REQUIRED`: 213 issues
  atuais e 213 no baseline, zero issue nova ou severa, cobertura 88,0%, duplicação 3,3% e nenhuma
  violação ou decisão humana Sonar pendente.
- 2026-08-15 — os backups temporários foram removidos. A revisão final permaneceu sem achado
  crítico ou obrigatório, e o escopo continuou restrito aos dois testes e ao registro da feature;
  produção, configuração, dependências, Postman, formatos derivados e `.tools/` permaneceram
  intactos. Tasks 17.1-A, 17.1-R e 17.1 concluídas; somente CF aguarda decisão humana explícita.
- 2026-08-15 — o usuário registrou explicitamente `CF`, aceitou as evidências finais e encerrou a
  feature. Nenhuma execução Maven ou SonarQube foi repetida, pois este registro é exclusivamente
  documental e não houve alteração executável após o checkpoint final `COMPLIANT/NOT_REQUIRED`.
  Produção, configuração, dependências, Postman, formatos derivados e `.tools/` permaneceram
  intactos.
