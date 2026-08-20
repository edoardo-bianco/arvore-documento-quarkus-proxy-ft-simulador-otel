# Checklist: consultar documentos vinculados ao dossiê de produto

## Estado

- **Branch:** `feature/consultar-documentos-dossie-produto`
- **Escopo:** feature concluída, integralmente verificada e aceita no checkpoint humano final
- **Próximo item:** encerrado — nenhuma pendência no plano desta feature

## Checklist

- [x] 0.1 Confirmar intenção, contrato fonte e critérios de sucesso;
- [x] 0.2 Ler arquitetura, índice e ADRs 0001, 0002, 0004, 0005 e 0006;
- [x] 0.3 Inspecionar Swagger v4, exemplo preservado, código, configuração e testes análogos;
- [x] 0.4 Registrar plano, divergências, riscos, verificações e checkpoints;
- [x] C0 Registrar GO humano para executar a feature;
- [x] C1 Aprovar contrato, arquitetura, segurança, observabilidade, IP, simulador e fault tolerance;
- [x] 1.1 Verificar `sonar/`, obter escolha humana da fonte quando aplicável e inicializar baseline;
- [x] 2.1 Congelar contrato público em RED;
- [x] 3.1 Criar critérios, modelo de documentos e falha interna;
- [x] 4.1 Criar portas e caso de uso;
- [x] 5.1 Entregar borda REST pública e deixar testes de contrato GREEN;
- [x] CA Revisar núcleo/REST e registrar evidências focadas;
- [x] 6.1 Criar DTO e mapper MTR v4;
- [x] 7.1 Criar query/client/erro/provider MTR seguros;
- [x] 8.1 Implementar e testar adapter MTR;
- [x] 9.1 Provar wire, erros, FT, correlação e ausência de vazamento ponta a ponta;
- [x] CB Executar checkpoint Sonar da fatia MTR e tratar eventual `NON_COMPLIANT`;
- [x] 10.1 Criar DTO, mapper e fixture sanitizada do simulador a partir do exemplo filtrado;
- [x] 11.1 Implementar adapter e qualifiers do simulador;
- [x] 12.1 Integrar producer, observabilidade, seleção CDI e bootstrap Quarkus;
- [x] 13.1 Atualizar somente documentação fonte;
- [x] 14.1 Executar revisão integral, suíte, checkpoint final e runtime;
- [x] CF Registrar aceitação e encerramento humanos.

## Decisões humanas

| Checkpoint | Status | Data | Evidência | Aprovador |
|---|---|---|---|---|
| C0 | APROVADO | 18/08/2026 | GO explícito do usuário para executar a feature | usuário |
| C1 | APROVADO | 18/08/2026 | Propostas recomendadas e detalhamento do simulador aprovados explicitamente | usuário |
| CB | CONCLUÍDO | 19/08/2026 | `ContinuarAjustes` registrado; análise final `b07bddd8-35d3-49a3-8bf1-dc1ba28ab27a` `COMPLIANT` | usuário |
| CF | APROVADO | 19/08/2026 | Aceitação e encerramento explícitos após revisão integral, suíte, Sonar e runtime | usuário |

## Evidências do planejamento

- branch criada a partir de `main` limpo;
- escopo desta sessão classificado como exclusivamente documental;
- nenhuma inspeção de `sonar/`, execução Maven/Sonar ou alteração de produção;
- Swagger confirma endpoint v4, 12 query params opcionais além do `id`, `200` array, `204` sem
  conteúdo e `400/401/403/404/500`;
- corpo `200` do exemplo filtrado do id `4081899` selecionado como base sanitizada do futuro
  simulador, com `inclui-armazenamento`, `inclui-assinaturas`, `inclui-atributos`,
  `inclui-conformidade`, `inclui-outsourcing` e `inclui-propriedades` iguais a `true`;
- título da fixture do simulador definido como `# 4081899`;
- risco de query string sensível no filtro compartilhado identificado e controlado no plano;
- diretório `sonar/` ausente; baseline inicializado exclusivamente no SonarQube Docker local;
- baseline `READY`/`LOCAL_SONAR` e avaliação `COMPLIANT`: 213 issues existentes, zero novas,
  zero high/blocker, cobertura de 88% e duplicação de 3,3%;
- contrato RED adicionado para rota pública, 12 query params, GET sem corpo/`Content-Type`, lista
  `200` com 14 itens e primeiro item sintético exato, `204` sem corpo, validação de `id` e
  annotations Java/OpenAPI;
- comando focado executado com compilação e bootstrap Quarkus válidos: 56 testes, 50 aprovados e
  somente os 6 novos cenários em RED, todos pela capacidade ausente (`404` ou método inexistente),
  sem erros e sem testes ignorados;
- checkpoint Sonar executado após a mudança em `src/test`; Maven falhou pelos mesmos testes RED
  intencionais antes da publicação da análise, portanto o baseline anterior permanece como última
  avaliação disponível e não há novo resultado Sonar a declarar;
- nenhum arquivo de produção foi alterado na Task 2.1;
- nenhum formato derivado foi autorizado.
- em 19/08/2026, o comando focado da Task 2 foi repetido integralmente: 56 testes, 50 aprovados e
  somente as mesmas 6 falhas RED pela rota/método ainda ausente, sem erros ou testes ignorados;
- o estado Sonar da nova sessão não reconheceu o baseline de 18/08; como `HEAD` e `main` apontavam
  para `54110b9a0f6f8d2c45bdc94c3257c71ffe1bbf72`, uma exportação temporária e limpa de `HEAD`
  inicializou o baseline da sessão sem tocar no working tree e foi removida após o uso;
- o novo baseline `READY`/`LOCAL_SONAR` ficou `COMPLIANT/NOT_REQUIRED`, análise
  `8d63a4b1-c1c4-4c21-be16-721692198ede`, com 213 issues existentes, zero novas ou severas,
  cobertura de 88,0% e duplicação de 3,3%;
- a Task 3.1 seguiu dois microciclos RED -> GREEN: critérios/falha falharam inicialmente pela
  ausência dos tipos e depois passaram; o modelo profundo falhou inicialmente pela classe ausente
  e depois passou;
- os 6 testes unitários da Task 3.1 estão verdes e cobrem os 12 filtros sem defaults, distinção
  entre `null` e `false`, schema v3 completo, datas opacas, ordem/nulos das listas e falha interna
  lossless sem tipos HTTP ou DTO MTR;
- o checkpoint Sonar do fingerprint da Task 3.1 compilou 306 fontes de produção e 153 de teste,
  mas o Maven encerrou com as mesmas 6 falhas RED da Task 2 antes do SonarScanner; os relatórios
  não registraram nenhuma outra falha e `lastCheckpoint` permaneceu nulo, sem nova conformidade a
  declarar;
- `git diff --check` não encontrou erro de whitespace; as advertências de conversão LF/CRLF nos
  três testes da Task 2 foram preservadas sem reformatá-los.
- a Task 4.1 seguiu RED -> GREEN: o teste do caso de uso falhou primeiro pela ausência da porta de
  saída e passou depois da criação das duas portas e da delegação direta;
- os 3 testes do novo caso de uso estão verdes e provam lista preenchida, lista vazia e falha
  propagadas sem transformação, com uma única delegação e a mesma instância dos critérios;
- a verificação focada conjunta das Tasks 3 e 4 com `ArchUnitProgressivoTest` passou, confirmando
  a direção de dependência sem imports de adapters na aplicação;
- o checkpoint Sonar da Task 4.1 compilou 309 fontes de produção e 154 de teste; somente as mesmas
  6 falhas RED da Task 2 apareceram, distribuídas entre os três testes de contrato já conhecidos;
  o Maven encerrou antes do SonarScanner e `lastCheckpoint` permaneceu nulo, sem nova conformidade
  a declarar;
- `git diff --check` continuou sem erro de whitespace; as advertências LF/CRLF preexistentes nos
  três testes da Task 2 foram preservadas.
- a Task 5.1 seguiu microciclos RED -> GREEN para query params, DTO de resposta, mapper e endpoint
  HTTP: as falhas evoluíram de tipos ausentes para rota `404` e então todos os contratos ficaram
  verdes;
- a borda REST publica a rota e os 12 filtros aprovados, responde com array `200`, vazio `204` e
  traduz a falha interna para o contrato de erro existente, sem expor DTO MTR ou do simulador;
- os filtros potencialmente sensíveis não são registrados em logs ou atributos de span; a
  observabilidade da borda limita-se a rota templated, versão da API, identificador, quantidade e
  tipo de erro;
- um bean exclusivamente de teste mantém os contratos Quarkus executáveis até a integração do
  producer na Task 12; em produção, `Instance<ConsultarDocumentosDossieProduto>` preserva o
  empacotamento intermediário sem antecipar adapter, seleção CDI ou producer;
- a verificação focada conjunta das Tasks 2 a 5, incluindo mapper REST, endpoint HTTP, caso de uso
  e `ArchUnitProgressivoTest`, passou após o refactor final;
- o primeiro checkpoint Sonar da Task 5.1 resultou em `NON_COMPLIANT` por duas issues novas:
  duplicação do literal `erro.tipo` e um import de teste não usado; o usuário escolheu
  `ContinuarAjustes`, decisão registrada pelo script, e ambas foram corrigidas sem alteração de
  comportamento;
- o checkpoint Sonar final ficou `COMPLIANT/NOT_REQUIRED`, análise
  `406a22bb-78d3-41d1-9226-9c148e2b3ac3`, com 213 issues de baseline, zero novas ou severas,
  cobertura de 85,5% e duplicação de 3,1%; a suíte completa registrou 595 testes aprovados, zero
  falhas, erros ou testes ignorados.
- o CA revisou testes antes da implementação e avaliou correção, simplicidade, arquitetura,
  segurança, desempenho e escopo; o veredito técnico foi `APROVAR`, sem achados `Critical` ou
  `Required`;
- a revisão confirmou rota, 12 filtros opcionais sem defaults, distinção entre ausência e `false`,
  array `200`, vazio `204`, validação do identificador, JSON explícito, nulos/listas/ordem, datas
  opacas e tradução de todos os tipos de falha;
- domínio e aplicação não importam adapters, REST ou DTOs; a borda REST não importa MTR ou
  simulador; o caso de uso faz uma única delegação `Uni` e o mapper percorre a árvore uma vez, sem
  bloqueio, threads, I/O adicional ou N+1;
- CPF, CNPJ, IP, tipologia e URL aparecem somente em critérios/modelos/DTOs e no mapeamento; a
  borda registra apenas rota templated, versão, identificador, quantidade e tipo de erro, e os
  dados usados nos testes são sintéticos;
- a tradução pública mantém o `ErroPadraoDto` e o campo `stacktrace` do contrato fonte conforme a
  tradução REST vigente aprovada em C1; nenhum erro externo textual é registrado como atributo ou
  campo de log nesta fatia;
- as dependências pendentes permaneceram explícitas: `Instance<ConsultarDocumentosDossieProduto>`
  é apenas a ponte de empacotamento até producer/qualifiers/bootstrap da Task 12, e a refatoração
  do `DossieProdutoResource` grande continua fora do escopo aprovado;
- três comandos focados do CA passaram: 56 testes de contrato HTTP/JSON/validação, 19 testes de
  núcleo/mapper/binding REST e 45 testes de ArchUnit/cobertura de beans/resources/spans; no total,
  120 testes aprovados, zero falhas, erros ou ignorados;
- o CA não alterou o fingerprint executável depois do checkpoint Sonar `COMPLIANT` da Task 5.1;
  portanto não houve nova análise, dependência, configuração ou formato derivado.
- a Task 6.1 seguiu dois passos RED -> GREEN: primeiro o teste falhou pela ausência do pacote DTO
  v4 e, após sua criação, falhou somente pelo mapper ausente; os quatro cenários ficaram verdes
  depois da transformação explícita campo a campo;
- o DTO MTR próprio tipa todo o schema `v3.dossie-produto.InstanciaDocumentoDTO1` referenciado
  pelo endpoint v4, declara todos os nomes `snake_case` e mantém datas como `String`; o mapper
  preserva nulos, listas, elementos nulos e ordem, convertendo resposta `null` ou vazia em lista
  vazia conforme C1, sem importar DTO REST;
- a verificação acumulada das Tasks 3 a 6 e `ArchUnitProgressivoTest` registrou 112 testes em 11
  suítes, sem falhas, erros ou ignorados;
- o primeiro checkpoint da Task 6.1 executou 599 testes verdes e publicou a análise
  `46b9f608-03bd-4923-9eb7-1cea9c8932d6`, mas ficou `NON_COMPLIANT` exclusivamente por cobertura
  de 82,3%; havia 213 issues iguais ao baseline, zero novas ou severas e duplicação de 3,0%;
- o usuário escolheu `ContinuarAjustes`, decisão registrada pelo script oficial. O JaCoCo mostrou
  0/191 linhas do mapper porque o teste unitário puro não passava pelo classloader instrumentado e
  o bean ainda não integra o runtime antes das Tasks 8/12;
- o ajuste ficou restrito ao teste: `@QuarkusTest` e injeção do bean fizeram os mesmos quatro
  cenários cobrirem 177/191 linhas do mapper e todos os 19 records do DTO, sem alterar produção,
  contrato ou comportamento;
- o checkpoint repetido ficou `COMPLIANT/NOT_REQUIRED`, análise
  `13a06bc8-30a6-4822-9332-249dd00ceaad`, com 213 issues iguais ao baseline, zero novas ou
  severas, cobertura de 85,8% e duplicação de 3,0%; a suíte completa manteve 599 testes aprovados,
  sem falhas, erros ou ignorados;
- a revisão da fatia não encontrou acoplamento MTR -> REST, logging ou segredos nos arquivos novos,
  alteração de dependências/configuração, implementação antecipada de client/adapter/FT ou formato
  derivado; `git diff --check` permaneceu limpo além dos avisos LF/CRLF preexistentes.
- a Task 7.1 iniciou em RED: o teste focado falhou na compilação exclusivamente pela ausência da
  query e da exceção de protocolo novas, antes de qualquer implementação de produção;
- a query MTR própria declara exatamente os 12 filtros aprovados com tipos anuláveis e nomes de
  wire hifenizados; o client usa `GET /v4/dossie-produto/{id}/documentos`, `@BeanParam`, API key,
  correlação, OIDC e retorno `Uni<List<ConsultaDocumentosDossieProdutoMtrResponse>>`, sem corpo ou
  `Content-Type` de requisição;
- o provider local desabilita o span HTTP automático que publicaria a URL com query string e
  reinjeta o contexto OpenTelemetry nos headers; o filtro compartilhado de observabilidade não é
  registrado e nenhuma das quatro peças adiciona log, atributo de span ou dependência lateral;
- `204` e todos os status menores que 400 não geram exceção; `400/404` são negócio, os demais 4xx
  são falha técnica de cliente e 5xx são servidor. Somente servidor, `ProcessingException` e
  timeout sofrem retry/circuit breaker; falhas de negócio e técnicas de cliente abortam;
- o payload de erro MTR é preservado para a futura tradução do adapter, enquanto a mensagem da
  exceção permanece genérica; corpo ausente, nulo ou malformado recebe fallback local sem registrar
  conteúdo externo;
- o teste focado ficou GREEN com 8 cenários, e a verificação conjunta com o mapper MTR e
  `ArchUnitProgressivoTest` passou sem falhas;
- o primeiro checkpoint da Task 7.1 publicou a análise
  `4ab7a4ed-371d-4a99-bcd8-bda6bbc78f92` e ficou `NON_COMPLIANT`: 215 issues contra 213 do
  baseline, duas novas `MINOR` no teste (`java:S1612` e `java:S1130`), zero severas, cobertura de
  84,5% e duplicação de 4,0%;
- o usuário escolheu `ContinuarAjustes`, decisão registrada pelo script oficial. As duas issues
  foram corrigidas e o teste passou a executar com `@QuarkusTest` para que as oito provas já
  existentes fossem coletadas pelo agente JaCoCo, sem mudança de produção ou comportamento;
- o checkpoint repetido ficou `COMPLIANT/NOT_REQUIRED`, análise
  `c757df5e-3a39-4665-b750-5dcc67249261`, com 213 issues iguais ao baseline, zero novas ou
  severas, cobertura de 85,9% e duplicação de 4,0%; a suíte completa registrou 607 testes
  aprovados, zero falhas, erros ou testes ignorados;
- a revisão final da fatia confirmou o escopo restrito a query/client/erro/provider MTR e seu teste:
  adapter, integração wire, simulador, producer, configuração, documentação derivada e dependências
  permanecem fora deste incremento.
- a Task 8.1 foi conduzida em microciclos RED -> GREEN para estrutura e delegação, preservação dos
  12 filtros, normalização de resposta nula/vazia, tradução lossless dos erros MTR, classificação de
  timeout/transientes e sinais seguros de sucesso/falha; o adapter ficou GREEN com 9 cenários;
- o adapter implementa a porta de saída, chama o client v4, usa o mapper próprio da borda e traduz
  falhas somente depois dos interceptors do client, preservando status, recurso, identificadores,
  mensagens, detalhe, stacktrace externo e causa quando fornecidos pelo protocolo;
- logs e span CLIENT registram somente rota parametrizada, versão, identificador, contagem,
  resultado e classificação técnica; CPF, CNPJ, IP, tipologia, URL, nomes, matrícula, paths, query
  string, payload e texto do erro externo permanecem fora dos sinais;
- a verificação acumulada de client, mapper, adapter e `ArchUnitProgressivoTest` registrou 54 testes
  aprovados (8 + 4 + 9 + 33), sem falhas, erros ou ignorados; `git diff --check` não encontrou erro
  de whitespace, apenas os avisos LF/CRLF já preservados nos arquivos rastreados;
- o checkpoint Sonar da Task 8.1 ficou `COMPLIANT/NOT_REQUIRED`, análise
  `d8d563f2-8351-4cbc-9b80-8d1e90e335aa`, com 213 issues iguais ao baseline, zero novas ou
  severas, cobertura de 86,2% e duplicação de 3,9%; a suíte completa registrou 616 testes
  aprovados, zero falhas, erros ou testes ignorados;
- a revisão da fatia não encontrou acoplamento REST/simulador, exposição de dados sensíveis,
  bloqueio, I/O adicional, dependência ou configuração nova. Qualifiers, producer, integração wire,
  simulador e documentação continuam pendentes nas Tasks 9 a 13, sem antecipação neste incremento.
- a Task 9.1 iniciou em RED de compilação pela ausência do contexto v4 e da captura da query no
  stub; depois da extensão mínima do stub, a rota pública passou por mapper REST, adapter, client
  real e servidor MTR local, com método, path, 12 filtros, headers, ausência de corpo e ausência de
  `Content-Type` verificados no wire;
- dez provas ponta a ponta cobrem `200`, `204`, `400/401/403/404` sem retry, recuperação após `500`,
  exaustão de três retries com quatro chamadas, timeout seguido de retry, payload de erro lossless,
  um único span CLIENT próprio e correlação exata do `traceparent` com esse span;
- o ciclo RED de segurança revelou três vazamentos/erros de correlação reais: o span HTTP de entrada
  mantinha `url.query`, a mensagem observável herdava texto externo e o OIDC injetava o contexto
  antes do provider local. O ajuste ficou restrito à rota: filtro REST limpa `url.query`, a falha
  expõe mensagem genérica e o provider roda imediatamente antes da prioridade de autenticação;
- a resposta HTTP continua preservando integralmente o erro MTR aprovado, enquanto spans e logs do
  mesmo trace foram verificados sem CPF, CNPJ, IP, tipologia, URL, matrícula, payload de sucesso,
  texto/detalhe/stacktrace externos, API key, bearer token ou valor do `traceparent`;
- o cenário que esgota retries recebeu perfil Quarkus próprio para isolar o estado do circuit
  breaker sem alterar thresholds ou adicionar espera; as duas classes ponta a ponta passaram com
  10 testes, sem falhas, erros ou ignorados;
- a verificação focada acumulada passou com 41 testes de contrato, client, adapter, mappers e falha;
  os guardrails passaram separadamente com 33 testes ArchUnit e 2 contratos de spans, todos verdes;
- `git diff --check` permaneceu sem erro de whitespace, apenas com os avisos LF/CRLF já conhecidos;
  não houve alteração de dependência, configuração, simulador, qualifier, producer ou formato
  derivado;
- o checkpoint Sonar do fingerprint da Task 9.1 não foi executado: `CB` permanece como o próximo
  item autorizado e a análise `d8d563f2-8351-4cbc-9b80-8d1e90e335aa` cobre somente o estado até a
  Task 8.1, não o fingerprint atual.
- o primeiro checkpoint `CB` executou `mvn clean verify` e SonarScanner completos; a suíte registrou
  626 testes aprovados, sem falhas, erros ou ignorados, e o Compute Engine concluiu a análise
  `6b0eec9e-ed92-446f-bfd1-56e5c79c386a`;
- o resultado técnico foi `NON_COMPLIANT`: 214 issues abertas contra 213 do baseline, uma única
  issue nova `MAJOR`/`java:S5961`, zero `HIGH`, `BLOCKER` ou `CRITICAL`, cobertura de 86,3% e
  duplicação de 3,9%; os dois limites quantitativos permanecem atendidos;
- a issue nova está no teste ponta a ponta
  `ConsultaDocumentosDossieProdutoMtrContractTest.java:291` e solicita reduzir de 26 para menos de
  25 assertions no método que prova correlação e ausência de vazamento; não foi feito ajuste nem
  registrada decisão por inferência, portanto `CB` permanece pendente de escolha humana.
- o usuário escolheu `ContinuarAjustes`, e a decisão foi registrada pelo script oficial antes de
  qualquer edição; o ajuste ficou restrito ao teste e extraiu para um helper nomeado as mesmas 14
  verificações de ausência de vazamento, sem remover sentinelas ou alterar produção;
- o contrato ponta a ponta passou isoladamente com 9 testes e o checkpoint completo foi repetido
  sobre o novo fingerprint; a suíte manteve 626 testes aprovados, sem falhas, erros ou ignorados;
- o `CB` final ficou `COMPLIANT/NOT_REQUIRED`, análise
  `b07bddd8-35d3-49a3-8bf1-dc1ba28ab27a`, com 213 issues iguais ao baseline, zero novas ou
  severas, cobertura de 86,3% e duplicação de 3,9%; `git diff --check` permaneceu limpo além dos
  avisos LF/CRLF conhecidos.
- a Task 10.1 iniciou em RED de compilação exclusivamente pela ausência do DTO e do mapper do
  simulador; após a implementação, os cinco cenários focados ficaram verdes;
- o contrato próprio do simulador encapsula a lista somente no wrapper interno `documentos` e o
  mapper entrega a lista do modelo da aplicação sem importar DTO ou mapper MTR; resposta ou lista
  ausente vira lista vazia, enquanto nulos, listas, elementos nulos e ordem são preservados;
- a fixture `# 4081899` mantém os 14 documentos e a chamada filtrada aprovada, com CPF, CNPJ,
  nomes, matrícula, códigos GED, identificadores negociais de vínculo, object store e paths
  substituídos por valores explicitamente sintéticos; a busca negativa não encontrou nenhum dos
  literais sensíveis originais selecionados;
- a verificação focada passou com 5 testes do mapper/fixture e 33 guardrails ArchUnit, sem falhas,
  erros ou ignorados; `git diff --check` continuou limpo além dos avisos LF/CRLF conhecidos;
- o checkpoint completo publicou a análise `321968db-56a4-406c-8abf-57ac04e94077` e ficou
  `NON_COMPLIANT` somente por cobertura de 83,3% abaixo do mínimo de 85%; há 213 issues iguais ao
  baseline, zero novas ou severas e duplicação de 4,0%; a decisão humana permanece pendente.
- o usuário escolheu `ContinuarAjustes`, decisão registrada pelo script oficial antes da edição;
  o ajuste ficou restrito ao teste, que passou a usar `@QuarkusTest` e injeção CDI do mesmo mapper
  para que os cinco cenários existentes fossem coletados pelo agente JaCoCo;
- o teste focado passou novamente com 5 testes, sem falhas, erros ou ignorados, e o checkpoint
  completo confirmou a suíte verde sem alteração do comportamento de produção;
- o checkpoint repetido ficou `COMPLIANT/NOT_REQUIRED`, análise
  `f2e984b6-7664-4f1f-8560-9e78bbbf875b`, com 213 issues iguais ao baseline, zero novas ou
  severas, cobertura de 86,5% e duplicação de 4,0%; a Task 10.1 está concluída.
- a Task 11.1 iniciou em RED de compilação pela ausência dos dois qualifiers e do adapter do
  simulador; após a criação dos qualifiers, o RED restante confirmou somente o adapter ausente;
- o adapter simulado implementa `ObterDocumentosDossieProduto`, resolve a fixture pelo
  identificador e depende exclusivamente de `MarkdownJsonMockReader` e do mapper próprio da
  borda; o cenário `4081899` entrega os 14 documentos determinísticos sem rede e sem reproduzir os
  filtros/projeções do MTR;
- fixture ausente, critérios ausentes ou identificador ausente produzem a falha de negócio simulada
  `404`, com recurso `simtr-dossie-produto`, código `DOSSIE_PRODUTO_NAO_ENCONTRADO` e sem tentar
  mapear uma resposta inexistente;
- os adapters MTR e simulador receberam qualifiers exclusivos com retenção runtime e targets de
  tipo/campo/parâmetro/método; a regressão alinhou os contratos MTR ao padrão consolidado de
  injeção da porta qualificada, preservando `400/401/403/404` sem retry e a exaustão de `500`;
- a inspeção negativa confirmou que o adapter simulado não importa client REST, fault tolerance,
  span ou logger e não lê CPF, CNPJ, IP ou tipologia; observabilidade, producer e seleção CDI
  permanecem reservados à Task 12.1;
- a verificação focada conjunta registrou 49 testes aprovados: 6 do adapter simulado, 9 do contrato
  MTR, 1 do contrato FT e 33 guardrails ArchUnit, sem falhas, erros ou ignorados;
- `git diff --check` permaneceu sem erro de whitespace, além dos avisos LF/CRLF já conhecidos; não
  houve dependência, configuração, formato derivado, producer ou observabilidade adicionados;
- o checkpoint completo executou 637 testes aprovados, sem falhas, erros ou ignorados, e ficou
  `COMPLIANT/NOT_REQUIRED` na análise `a26effba-5604-45b6-82e5-3e41f4300214`: 213 issues iguais ao
  baseline, zero novas ou severas, cobertura de 86,6% e duplicação de 4,0%; a Task 11.1 está
  concluída.
- a Task 12.1 iniciou em RED de compilação exclusivamente pela ausência do producer e do decorator
  observável; após a implementação mínima, os 7 testes focados dessas classes ficaram verdes;
- o producer seleciona exatamente um `ObterDocumentosDossieProduto` pelos qualifiers MTR/simulador
  e pela propriedade já existente; o Resource passou da ponte temporária `Instance#get()` para a
  porta de entrada direta, e o bean exclusivamente de teste foi removido;
- o decorator constrói o caso de uso atômico com a porta selecionada e publica o span INTERNAL
  `simtr-hub.service.dossie-produto.documentos.consultar`, origem `mock`/`mtr`, flag do simulador,
  identificador e quantidade; os eventos estruturados usam o prefixo aprovado e não incluem query,
  CPF, CNPJ, IP, tipologia, URL, nomes, matrícula, paths, payload ou texto externo de erro;
- os contratos MTR deixaram de desviar pelo bean provisório e passaram a provar a árvore real
  SERVER -> INTERNAL -> CLIENT com o simulador desabilitado; o teste Quarkus do simulador confirmou
  14 documentos, zero spans CLIENT próprios e zero requisições ao stub MTR;
- os dois comandos de aceitação da Task 12.1 passaram: 10 testes dos modos MTR/simulador e 45
  testes de ArchUnit, cobertura de beans/resources e contrato de spans; a regressão diretamente
  afetada também passou com 11 testes;
- a suíte completa inicialmente expôs três expectativas ainda acopladas aos dados do bean
  provisório; os contratos REST passaram a controlar explicitamente sua porta e o smoke integrado
  foi alinhado à fixture real, após o que a suíte completa ficou verde;
- o primeiro checkpoint publicou a análise `2c42049a-45d4-4a56-9b00-4f1aa9a9d240` e ficou
  `NON_COMPLIANT` por uma única issue nova `MINOR`/`java:S1481` no teste de observabilidade; havia
  zero issues severas, cobertura de 86,7% e duplicação de 4,0%;
- o usuário escolheu `ContinuarAjustes`, decisão registrada pelo script oficial; a variável local
  não usada foi substituída pelo identificador sem nome do Java, e o teste focado passou novamente;
- o checkpoint repetido ficou `COMPLIANT/NOT_REQUIRED`, análise
  `576ec862-82b3-4bd0-8e0f-9b2e60615229`, com 213 issues iguais ao baseline, zero novas ou
  severas, cobertura de 86,7% e duplicação de 4,0%; o `mvn clean verify` completo manteve 645 testes
  aprovados, sem falhas, erros ou ignorados, e a Task 12.1 está concluída.
- a Task 13.1 atualizou o README, o consolidado arquitetural e a documentação operacional para 12
  capacidades totais, nove em `dossieproduto`, incluindo as rotas pública e MTR v4 de consulta de
  documentos, os 12 filtros opcionais, fault tolerance, seleção MTR/simulador e sua limitação
  determinística;
- o ADR-0002 e seu índice agora refletem a nona capacidade do domínio sem criar decisão nova, pois
  a implementação aplica os limites e contratos por borda já aceitos;
- o catálogo registra os nomes e o parentage dos spans SERVER/INTERNAL/CLIENT, eventos, atributos
  seguros, supressão de `url.query` e do span HTTP automático, propagação correlacionada e ausência
  de CLIENT/rede no simulador;
- a verificação ficou restrita ao diff e a buscas nos Markdown fonte: nenhum formato derivado ou
  contrato externo foi alterado. Conforme a regra de escopo exclusivamente documental, não houve
  Maven, baseline, API ou checkpoint Sonar e o diretório `sonar/` não foi inspecionado nesta task.
- a revisão integral da Task 14.1 avaliou correção, simplicidade, arquitetura, segurança,
  desempenho, testes e escopo; o veredito técnico foi `APROVAR`, sem achados `Critical` ou
  `Required`, e manteve como dívida conhecida fora do escopo apenas a dimensão preexistente do
  `DossieProdutoResource`;
- a inspeção confirmou a direção domínio/aplicação -> portas -> adapters, separação dos DTOs e
  mappers REST/MTR/simulador, transformação linear sem bloqueio ou N+1, ausência de valores
  sensíveis em logs/spans e ausência de dependência, configuração executável ou formato derivado
  novo; `git diff --check` não encontrou erro de whitespace além dos avisos LF/CRLF conhecidos;
- `mvn -q clean test` terminou com código zero; o checkpoint final repetiu `mvn clean verify` e
  registrou 645 testes aprovados, sem falhas, erros ou ignorados;
- o checkpoint Sonar final ficou `COMPLIANT/NOT_REQUIRED` no fingerprint
  `e82fe7dc3de739ee63bee8bf19235a3c33f01ca77b673f56ba69bce642016c6c`, análise
  `ff5252e4-5f8d-4dd5-a842-efebbd30732d`: 213 issues iguais ao baseline, zero novas ou severas,
  cobertura de 86,7% e duplicação de 4,0%;
- no runtime empacotado com simulador, a rota pública respondeu `200` com os 14 documentos da
  fixture e primeiro identificador `1132220`, além de `400` estruturado para identificador
  inválido;
- no runtime empacotado em modo MTR contra stub exclusivamente local, a rota respondeu `200` com
  o documento sintético `9000001`, normalizou a resposta externa vazia para `204` sem corpo e
  traduziu o `400` lossless sem publicar `detalhe` ou `stacktrace`; aplicação e stub foram
  encerrados, as portas locais foram liberadas e o script temporário foi removido;
- a atualização deste checklist após o checkpoint é exclusivamente documental e não altera o
  fingerprint executável. Em 19/08/2026, o usuário aprovou explicitamente o checkpoint `CF` e
  encerrou a feature; nenhum commit foi criado.
