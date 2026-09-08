# Checklist: orquestração de monitoramento com duas filas do Service Bus

**Continuidade atual:** item 4.1 tecnicamente concluído, incluindo política/configuração,
contratos, mappers, logs tipados, validação confirmada de resultado e guardrails.
Checkpoint COMPLIANT; evidências completas no checklist. Nenhum avanço para 5.1,
listener, publicação, settlement ou encerramento humano da feature.

## Estado

O usuário confirmou QUARENTENA com situacaoMtr=null e zero tentativas quando ainda não
houve consulta; CONCLUSIVO exige situação conclusiva e pelo menos uma tentativa.
A regra foi implementada e verificada nas duas bordas independentes. Contadores negativos
são rejeitados. O JSON v1 e as situações recebidas são preservados, sem recálculo ou efeito remoto.

**4.1 tecnicamente concluído.** A regressão focada passou em 342 testes, incluindo 25 casos
novos; a suíte completa passou em **1046 testes em 171 classes**, sem falhas, erros ou ignorados.
Checkpoint de `2026-09-08T11:12:19.3296159-03:00`: **COMPLIANT**, cobertura **87,0%**,
duplicação **4,4%**, 213 issues abertas, nenhuma nova ou HIGH/BLOCKER/CRITICAL.
A duplicação não aumentou neste incremento. A validação nova tem 100% das linhas e condições
cobertas em ambas as bordas. Análise `f3b6720c-0b18-4713-8a16-22a030278153`.
Os checkpoints de 859, 900 e 1021 testes continuam como evidências históricas.

Baseline original de 217 issues integralmente preservado, análise
`f6183a72-a2ea-44bc-9374-b2b064bdad55`, READY; nenhuma reinicialização.
Branch `feature/orquestrador-monitoramento-service-bus`; commit e push autorizados pelo usuário para revisão com os desenvolvedores.
Hub, dossiê, configurações e extensão/emulador preservados. Restam 19 esqueletos inativos,
correspondentes aos incrementos futuros. O próximo item é 5.1, ainda não iniciado.
O encerramento humano da feature não foi inferido; detalhes no [checklist](todo.md).

## Checklist

- [x] 0.1 Confirmar a intenção do recorte com endpoint, duas filas, consulta Hub, mock e log;
- [x] 0.2 Ler arquitetura consolidada, índice e ADRs 0001, 0002, 0003, 0004, 0005, 0006, 0007 e 0009;
- [x] 0.3 Ler integralmente o documento técnico de duas filas e os templates de tasks;
- [x] 0.4 Inspecionar `pom.xml`, configuração, consulta de dossiê, simulador, telemetria e ArchUnit;
- [x] 0.5 Verificar em fontes oficiais extensão, Quarkus LTS, Dev Services, emulador e telemetria;
- [x] 0.6 Criar branch específica e registrar plano, escopo, riscos, verificações e checkpoints;
- [x] C0.1 Aprovar arquitetura: packages, ownership do processor, ADR sucessor e limites do recorte;
- [x] C0.2 Aprovar contrato: REST, mensagens v1, validação, status e OpenAPI;
- [x] C0.3 Aprovar segurança: SAS/connection string, menor privilégio, mock e EULAs;
- [x] C0.4 Aprovar observabilidade: spans, atributos, logs e propagação;
- [x] E0.1 Confirmar escopo de plataforma: manter Quarkus `3.33.2.1` e deixar o upgrade fora da feature;
- [x] GO Registrar autorização humana antes da primeira alteração executável;
- [x] B0.1 Verificar pacotes em `sonar/` e obter escolha humana da fonte do baseline;
- [x] B0.2 Inicializar o baseline SonarQube autorizado;
- [x] 1.1 Propor ADR sucessor e atualizar o índice sem apagar o ADR-0009;
- [x] 1.2 Registrar aceite do ADR ou interromper a implementação;
- [x] 2.1 Adicionar e provar a extensão `quarkus-azure-servicebus:1.2.5` no stack atual;
- [x] C1 Revisar dependency tree, JDK 25, augmentation e compatibilidade efetiva;
- [x] 3.1 Configurar Dev Services, imagens fixadas, duas filas e profiles de transporte;
- [x] 4.1-E1 Antecipar estrutura Java inativa dos componentes/classes, verificar arquitetura e CDI;
- [x] 4.1-E2 Criar guia de continuidade e preparar manifesto do pacote de commit para revisão;
- [x] 4.1 Completar contratos e política em `monitoramento`, com migração, configuração CDI, implementação e cobertura conforme direção humana atual;
- [ ] 5.1 Implementar em `monitoramento` consulta simulada da pré-validação e ACL local para o Hub;
- [ ] 6.1 Implementar em `orquestrador` a fatia POST REST -> fila de entrada, através das portas;
- [ ] C2 Revisar contrato, segurança, telemetria e checkpoint Sonar da primeira fatia;
- [ ] 7.1 Implementar em `monitoramento` processamento terminal da entrada -> fila de saída;
- [ ] 8.1 Implementar em `monitoramento` reagendamento transacional da situação não conclusiva;
- [ ] 9.1 Implementar em `orquestrador` listener da saída -> porta/caso de uso -> log estruturado;
- [ ] 10.1 Fechar e testar correlação OpenTelemetry ponta a ponta;
- [ ] C3 Executar fluxo com emulador e revisar Complete, Abandon, DLQ, retry e sinais;
- [ ] 11.1 Executar suíte completa e checkpoint SonarQube final;
- [ ] 11.2 Atualizar arquitetura, ADRs e tasks conforme estado implementado;
- [ ] 11.3 Revisar correção, simplicidade, arquitetura, segurança, desempenho, testes e escopo;
- [ ] CF Registrar encerramento decidido pelo usuário.

## Subfatias do item 4.1

O item 4.1 está tecnicamente concluído. Os itens 5.1 em diante permanecem pendentes.

- [x] Executar RED e GREEN da política pura; teste focado aprovado, conforme estado de retomada
  informado pelo usuário e arquivos existentes;
- [x] Migrar as duas classes da política para `src/main/java/br/gov/caixa/simtr/monitoramento/dominio/politica/`
  e os três testes existentes para a estrutura espelhada em `src/test/java/br/gov/caixa/simtr/monitoramento/`;
- [x] Verificar imports/packages e regressão da migração, sem alterar o `config.json` do emulador;
- [x] Criar e executar RED da configuração tipada e do producer CDI;
- [x] Fazer GREEN em `monitoramento.adaptador.configuracao`, com política ativa e definições
  integralmente em `application.properties` e rejeição de configuração inválida no bootstrap;
- [x] Completar os contratos e mappers mínimos das bordas de cada componente;
  - [x] Request REST -> tipo semântico próprio, validação aprovada e contrato JSON;
  - [x] Resultado interno -> response REST, sem Resource nem publicação nesta subfatia;
  - [x] Contratos/mappers independentes de entrada e saída Service Bus e compatibilidade JSON;
    - [x] Produtor da entrada: modelo/DTO/mapper JSON e AMQP, sem publicação;
    - [x] Consumidor da entrada: DTO próprio, validação, mapeamento e compatibilidade;
    - [x] Produtor de reagendamento: DTO/mapper próprio, sem agendamento;
    - [x] Resultado: produtor/consumidor independentes, compatibilidade e validação confirmada de quarentena/conclusivo implementados e verificados;
- [x] Detalhar e submeter ao checkpoint arquitetural a obtenção dos parâmetros iniciais pelo
  orquestrador e a localização/acesso da fábrica CDI única, antes de implementar essa colaboração;
  - [x] Detalhar proposta no ADR-0011 e no plano, sem implementar a colaboração ou a fábrica;
  - [x] Registrar direção humana: `arquitetura.infraestrutura.servicebus` somente técnico,
    regras assíncronas em seus componentes, Quarkus reativo permitido no domínio e Hub inalterado;
  - [x] C4.1 Obter decisão humana sobre porta/ACL e detalhes de composição do ADR-0011:
    usuário declarou “ADR-0011 aprovado” em 2026-09-06; ADR, índice, arquitetura e plano atualizados;
- [x] C4.1 Obter separadamente decisão humana sobre limite positivo compatível com `Long`
  no contrato MTR: usuário aprovou e deu GO em 2026-09-06;
- [x] Aplicar limite MTR aprovado na borda REST por RED/GREEN, preservando string e zeros à
  esquerda; provar limites inclusivos e rejeição de overflow;
- [x] Executar checkpoint Sonar da subfatia do limite MTR: 715 testes, build e análise concluídos;
  `NON_COMPLIANT` por `java:S4144`, sem reprovação automática;
- [x] Obter e registrar decisão humana Sonar sobre S4144: usuário decidiu `ContinuarAjustes`,
  registrado com `./validar-checkpoint-sonarqube.ps1 -HumanDecision ContinuarAjustes`;
- [x] Consolidar os cenários de rejeição MTR em um teste parametrizado, preservando casos e
  asserções; executar regressão focada sem mudar código de produção;
- [x] Concluir novo checkpoint Sonar após o ajuste autorizado de S4144: 715 testes e
  `COMPLIANT`; S4144 confirmada como `FIXED`, sem supressão ou aceitação excepcional;
- [x] Verificar contratos da entrada: 161 testes focados e 779 completos aprovados;
  checkpoint `NON_COMPLIANT` por uma issue nova `java:S7467`;
- [x] Obter e registrar decisão humana sobre S7467: `ContinuarAjustes`, já registrado em
  `2026-09-06T20:45:43-03:00`, conforme confirmação do usuário;
- [x] Atualizar plan.md e todo.md com as evidências da entrada que ficaram pendentes na pausa;
- [x] Recuperar a referência original do baseline perdida no estado de sessão atual, sem
  reinicializar ou substituir o baseline;
- [x] Aplicar somente `catch (JsonProcessingException _)` no mapper consumidor, preservando
  corpo e testes; executar regressão focada e novo checkpoint com o baseline original;
- [x] Registrar o requisito humano de log de erro com JSON padronizado como no REST,
  localização da falha e informação utilizável para ação posterior; propor C4.2 no plano;
- [x] C4.2 Confirmar destino do registro e aprovar contrato de log: campos/tipos JSON,
  códigos/eventos, nível ERROR, diagnóstico sanitizado, correlação e escopo das bordas;
- [x] Caracterizar o formatter efetivo: MDC converte array/número em texto; prova isolada de
  decorator emite JSON tipado e delega registro existente sem alteração;
- [x] C4.3 Usuário autorizou a adaptação somente para o novo desenvolvimento, preservando
  código, logs e erros de `simtr.hub`; ADR-0012 aceito com essa restrição, sem repetir C4.2;
- [x] Após C4.3, implementar e verificar a composição técnica, com regressão e Sonar;
  ajustes S6878/S5786 verificados e provas do console/observer CDI concluídas;
- [x] Executar RED/GREEN do log real do consumidor da entrada, cobrindo parsing,
  envelope e validação, propagação da falha e ausência de payload/segredos; preservar baseline;
- [x] Revisar e verificar a fatia do consumidor com regressão e checkpoint Sonar;
- [x] Aplicar e verificar o padrão ao produtor da entrada, com RuntimeException e log JSON;
  S110, S5961 e S1130 corrigidas, regressão e checkpoint `COMPLIANT`;
- [x] Aplicar o mesmo formato de erro às bordas de reagendamento/resultado dentro de 4.1,
  com DTOs próprios e sem antecipar listeners ou 5.1;
- [x] Proteger as fronteiras introduzidas com ArchUnit, provas negativas de acoplamento proibido
  e provas positivas de uso permitido de Quarkus reativo/Mutiny e CDI no domínio e na aplicação;
- [x] Executar checkpoint completo da subfatia migração/configuração e registrar resultado técnico
  `NON_COMPLIANT`; isso não representa aprovação ou reprovação humana;
- [x] Obter e registrar decisão humana sobre a issue `java:S8911` desta subfatia: `ContinuarAjustes`;
- [x] Ajustar inicialização CDI, comprovar bootstrap inválido e obter novo checkpoint `COMPLIANT`;
- [x] Executar checkpoint da subfatia REST: 706 testes, build e Compute Engine concluídos;
- [x] Obter decisão humana sobre `java:S6353` da subfatia REST: usuário decidiu
  `ContinuarAjustes`, registrado com `-HumanDecision ContinuarAjustes`;
- [x] Simplificar a regex de S6353, preservar os testes e executar novo checkpoint `COMPLIANT`;
- [x] Executar verificações e checkpoint Sonar após as subfatias restantes de contratos/ArchUnit,
  sem avançar para 5.1.

## C4.4 — estrutura e continuidade

- [x] Registrar direção humana de 2026-09-07: estrutura de componentes e classes no código antes
  da lógica, seguida de guia para implementação e acompanhamento pelos desenvolvedores;
- [x] Atualizar plano e ordem do checklist antes de alterar código;
- [x] Conferir baseline original: READY, integralmente idêntico à referência recuperada;
  fingerprint inicial coincide com o checkpoint COMPLIANT de S1130; credencial disponível em memória;
- [x] Criar os arquivos estruturais e contratos de portas, preservando implementação existente;
- [x] Verificar compilação, fronteiras arquiteturais e ausência dos esqueletos no CDI;
- [x] Executar regressão e checkpoint Sonar do incremento, sem reinicializar o baseline;
- [x] Criar e conferir guia, inventário, links, roteiro de desenvolvimento e manifesto de commit;
- [x] Registrar as evidências e o próximo passo sem concluir as pendências funcionais de 4.1.

## Decisões humanas

| Checkpoint | Status | Data | Evidência | Aprovador |
|---|---|---|---|---|
| C0.1 Arquitetura | APROVADO | 2026-09-04 | Packages irmãos `orquestrador` e `doctree.monitoramentomtr`, ACL local, ADR sucessor e limites de demonstrador aprovados | Usuário |
| C0.2 Contrato | APROVADO | 2026-09-04 | `POST /simtr-hub/v1/monitoramentos-dossie`, validação, `202` após confirmação do broker, mensagens v1 independentes, erro REST e OpenAPI aprovados | Usuário |
| C0.3 Segurança | APROVADO | 2026-09-04 | Connection string/SAS, emulador, ausência de Entra ID, menor privilégio, autorização vigente, mock restrito e EULAs aprovados | Usuário |
| C0.4 Observabilidade | APROVADO | 2026-09-04 | Nomes/kinds de spans, eventos estruturados, atributos permitidos, propagação W3C e regra contra duplicidade aprovados | Usuário |
| E0.1 Escopo de plataforma | APROVADO | 2026-09-04 | Manter Quarkus `3.33.2.1`; upgrade para `3.33.3.2` permanece fora da feature | Usuário |
| GO | APROVADO | 2026-09-04 | Execução da feature autorizada conforme plano e checkpoints registrados | Usuário |
| 1.2 ADR-0010 | APROVADO | 2026-09-04 | ADR-0010 aceito; ADR-0009 substituído e preservado como histórico | Usuário |
| C1 Compatibilidade | APROVADO | 2026-09-05 | Matriz efetiva aceita com o risco residual registrado; comunicação AMQP real permanece para as Tasks 3 e C3 | Usuário |
| 4.1 Política configurável | APROVADO | 2026-09-05 | Usuário definiu política injetável selecionada por objeto de configuração; intervalos e duração são configuráveis, máximo de tentativas é opcional e um único intervalo deve ser reutilizado | Usuário |
| C0.1 Revisão dos packages | DEFINIDO PELO USUÁRIO | 2026-09-06 | `orquestrador` escreve na entrada e lê a saída; `monitoramento` lê a entrada, processa/aplica políticas e escreve na saída. Eliminar os intermediários `doctree`/`monitoramentomtr`; política em `monitoramento/dominio/politica`. Ambos seguem DDD/hexagonal com domínio, aplicação, portas e adapters; manter extensão Quarkus Service Bus e simuladores | Usuário |
| C0.1 Hexagonal pragmática | DEFINIDO PELO USUÁRIO | 2026-09-06 | Aplicar hexagonal não estrita, permitindo Quarkus no domínio. Preservar a orientação do ADR-0001: framework pode apoiar domínio e aplicação; responsabilidades, contratos e direção de dependências continuam protegidos | Usuário |
| 4.1 Retomada após revisão | AUTORIZADO | 2026-09-06 | Usuário: “prosseguir assim”, após revisão dos packages e direcionamento de hexagonal não estrita. Continuar somente 4.1, preservando workspace e alterações | Usuário |
| 4.1 Sonar da configuração | CONTINUAR AJUSTES | 2026-09-06 | Usuário decidiu explicitamente `ContinuarAjustes`; registrado com `./validar-checkpoint-sonarqube.ps1 -HumanDecision ContinuarAjustes`. Não houve aceitação excepcional. O ajuste foi verificado em novo checkpoint técnico `COMPLIANT`, registrado nas evidências abaixo | Usuário |
| C2 Primeira fatia | PENDENTE | — | Depende das Tasks 2–6 | — |
| C4.1 Localização e limites transversais | DEFINIDO PELO USUÁRIO | 2026-09-06 | `br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus` somente para capacidade técnica transversal, não todo código assíncrono; regra permanece em seu componente, Quarkus reativo pode ser usado no domínio e `simtr.hub`/`hub.arquitetura` ficam como estão. Não equivale ao aceite das demais propostas | Usuário |
| C4.1 ADR-0011 | APROVADO | 2026-09-06 | Usuário declarou “ADR-0011 aprovado”. Aceite inclui porta/ACL de parâmetros iniciais e composição CDI técnica, preservando limites transversais, Quarkus reativo no núcleo e Hub. Não conclui 4.1 nem antecipa 5.1/6.1 | Usuário |
| C4.1 Limite MTR | APROVADO | 2026-09-06 | Usuário: “Long do contrato MTR ok, aprovado vamos prosseguir go”. Aceitar `1..9223372036854775807`, manter JSON string e zeros à esquerda, rejeitar overflow antes da publicação. GO restrito à continuidade de 4.1 | Usuário |
| 4.1 Sonar do limite MTR | CONTINUAR AJUSTES | 2026-09-06 | Usuário decidiu explicitamente `ContinuarAjustes` para S4144; decisão registrada pelo script em `2026-09-06T17:46:40.762221-03:00`. Sem aceitação excepcional ou reprovação; ajuste e novo checkpoint descritos abaixo | Usuário |
| 4.1 Sonar do contrato REST | CONTINUAR AJUSTES | 2026-09-06 | Usuário decidiu explicitamente `ContinuarAjustes` para S6353; decisão registrada pelo script. Não aprova ADR-0011 nem o limite superior MTR | Usuário |
| 4.1 Sonar da fila de entrada | CONTINUAR AJUSTES | 2026-09-06 | Usuário confirmou na retomada que `ContinuarAjustes` já foi registrado em `2026-09-06T20:45:43-03:00` para S7467. Sem aceitação excepcional ou reprovação | Usuário |
| 4.1 Retomada de S7467 | AUTORIZADO | 2026-09-07 | Atualizar tasks, trocar somente o parâmetro `ignored` por `_` no catch da linha 60 do mapper consumidor, preservar corpo/testes e executar regressão focada/checkpoint com baseline preservado. Preservar todos os arquivos e alterações; não reiniciar itens nem avançar para 5.1 | Usuário |
| 4.1 Requisito de log JSON | SOLICITADO PELO USUÁRIO | 2026-09-07 | Usuário requer log do erro para localizar a falha e mensagens de erro na estrutura JSON padrão do REST, com registro utilizável para ação posterior. Não constitui aceite dos códigos, detalhes técnicos ou ampliação global propostos pelo agente | Usuário |
| C4.2 Log de erro padronizado | APROVADO | 2026-09-07 | Usuário respondeu “GO” à proposta concreta: bordas Service Bus de 4.1, inicialmente somente logs, começando pelo consumidor. Campos/tipos do REST, diagnóstico seguro, eventos/códigos e nível ERROR aprovados; sem persistência, ação automática, listener ou mudança global do Hub | Usuário |
| C3 Ponta a ponta | PENDENTE | — | Depende das Tasks 7–10 | — |
| CF | PENDENTE | — | Feature em andamento; encerramento não solicitado | — |

## Evidências de planejamento

| Item | Data | Evidência |
|---|---|---|
| 0.2 | 2026-09-04 | Arquitetura e ADRs aplicáveis lidos; ADR-0009 conflita com extensão + connection string |
| 0.3 | 2026-09-04 | Documento de duas filas lido integralmente; proposta inclui contratos, retry, settlement e código ilustrativo |
| 0.4 | 2026-09-04 | Projeto usa Quarkus 3.33.2.1/JDK 25, OTel e simulador de dossiê; não possui Service Bus/orquestrador |
| 0.5 | 2026-09-04 | Extensão 1.2.5 é `preview`, construída com Quarkus 3.37.4/Java 17; Dev Services usa emulador |
| 0.5 | 2026-09-04 | Emulador suporta apenas AMQP/TCP; WebSockets não são suportados |
| 0.6 | 2026-09-04 | Branch criada sem modificar o documento-base não rastreado |
| C0.3 (parcial) | 2026-09-04 | Usuário decidiu por connection string externa em ambiente real e emulador em dev/test, sem Entra ID; EULAs e demais controles seguem pendentes |
| C0.1 | 2026-09-04 | Usuário aprovou packages, ownership do processor, ADR sucessor e limites de demonstrador não produtivo |
| C0.2 | 2026-09-04 | Usuário aprovou o contrato REST e os contratos v1 das filas nos termos registrados no plano |
| C0.3 | 2026-09-04 | Usuário aprovou os controles de segurança e aceitou explicitamente as EULAs do Service Bus Emulator e do SQL Server |
| C0.4 | 2026-09-04 | Usuário aprovou nomes de spans e eventos, atributos permitidos, propagação e caracterização antes de instrumentação manual |
| E0.1 | 2026-09-04 | Usuário confirmou Quarkus `3.33.2.1` e manteve o upgrade fora do escopo |
| GO | 2026-09-04 | Usuário autorizou a execução da feature; primeira alteração executável permanece condicionada ao baseline SonarQube |
| B0.1 | 2026-09-04 | Diretório `sonar/` ausente; não há pacote offline disponível e o baseline aplicável é somente o SonarQube Docker local |
| B0.2 | 2026-09-04 | Baseline local registrado como tecnicamente conforme: 652 testes, 213 issues abertas no baseline, 0 novas, 0 HIGH/BLOCKER/CRITICAL, cobertura 86,6% e duplicação 4% |
| 1.1 | 2026-09-04 | ADR-0010 mantido como `Proposto` e incluído no índice com extensão, autenticação, emulador, risco de compatibilidade e aplicabilidade; ADR-0009 preservado como `Aceito` sem alterações |
| 1.2 | 2026-09-04 | Usuário aceitou explicitamente o ADR-0010; status alterado para `Aceito`, ADR-0009 marcado como `Substituído pelo ADR-0010` e mantido integralmente no histórico |
| B0.2 (retomada) | 2026-09-05 | Estado de sessão estava `NOT_REQUIRED_UNTIL_CODE_CHANGE`; as alterações do item 2.1 foram temporariamente removidas, o baseline local pré-incremento foi reinicializado e ficou conforme: 652 testes, 213 issues, 0 novas, 0 HIGH/BLOCKER/CRITICAL, cobertura 86,6% e duplicação 4% |
| 2.1 RED | 2026-09-05 | O smoke test de injeção falhou em `testCompile` exclusivamente pela ausência de `com.azure.messaging.servicebus.ServiceBusClientBuilder`; após a dependência, o bootstrap ainda caracterizou a exigência de `connection-string` ou `namespace` quando o producer está habilitado |
| 2.1 GREEN | 2026-09-05 | BOM `quarkus-azure-services-bom:1.2.5` e extensão sem versão individual adicionados; profile isolado habilitou o producer com configuração sintética e sem rede; teste focado iniciou Quarkus 3.33.2.1/JDK 25, registrou `azure-servicebus` e injetou o builder |
| 2.1 correção de package | 2026-09-05 | Smoke test movido de `orquestrador` para `br.gov.caixa.simtr.doctree.monitoramentomtr.adaptador.configuracao`, alinhando a prova de infraestrutura do Service Bus ao bounded context de monitoramento definido no plano e no documento-base |
| 2.1 dependency tree | 2026-09-05 | Extensão efetiva `1.2.5`, Azure Service Bus SDK `7.17.12`, Azure Core `1.55.5`, Vert.x `4.5.28`, Reactor `3.4.41`, Netty `4.1.135.Final`, Jackson `2.21.2` e OpenTelemetry `1.57.0`; a árvore mostrou versões gerenciadas/duplicatas, sem dependência omitida por conflito; `azure-identity:1.16.2` é transitiva da extensão e não foi configurada nem usada |
| 2.1 verificação | 2026-09-05 | Maven 3.9.16/Java 25.0.3; 653 testes, 0 falhas, 0 erros e 0 ignorados; augmentation concluída; checkpoint Sonar conforme com 213 issues (baseline 213), 0 novas, 0 HIGH/BLOCKER/CRITICAL, cobertura 86,6% e duplicação 4% |
| C1 revisão técnica | 2026-09-05 | Árvore efetiva sem artefato omitido por conflito: extensão `1.2.5`, Service Bus SDK `7.17.12`, Azure Core `1.55.5`, Vert.x `4.5.28`, Reactor `3.4.41`, Netty `4.1.135.Final`, tcnative `2.0.77.Final`, Jackson `2.21.x` e OpenTelemetry `1.57.0`. Alinhamentos gerenciados foram explicados e o stack passou em JDK `25.0.3`, CDI, 653 testes e augmentation. Risco residual: a matriz oficial da extensão cobre `1.2.5` com Quarkus `3.37.4`/Java `17`, não a combinação atual `3.33.2.1`/Java `25`; conexão AMQP real permanece para as Tasks 3 e C3. Recomendação técnica: aceitar C1 mantendo esse risco explícito |
| C1 decisão humana | 2026-09-05 | Usuário aprovou explicitamente o C1 com a matriz efetiva e o risco residual apresentados; próximo item liberado: 3.1 |
| 3.1 RED | 2026-09-05 | Com Docker disponível e sem conexão externa, o novo teste iniciou o bootstrap do Dev Services e falhou pela ausência de aceite explícito das EULAs; nenhum container do Service Bus foi iniciado nesse RED |
| 3.1 GREEN | 2026-09-05 | Configuração alinhada à guia oficial da Quarkus Azure Service Bus Extension: EULA aceita somente em dev/test, imagens fixadas nas versões verificadas pela extensão, config.json no caminho oficial e Dev Services desabilitado em prod/azure |
| 3.1 topologia e transporte | 2026-09-05 | Emulador e SQL iniciaram; mensagens foram enviadas, recebidas e concluídas nas filas q.prevalidacao.monitoramento-mtr.in e .out por AMQP/TCP. O profile azure, com connection string sintética e sem rede, iniciou sem Dev Services e selecionou AMQP sobre WebSockets/443 |
| 3.1 segurança e limites | 2026-09-05 | Nenhuma credencial real foi adicionada; configuração externa continua via QUARKUS_AZURE_SERVICEBUS_CONNECTION_STRING. O teste do emulador rejeita connection string/namespace externos sem registrar valores. TTL local fixado em PT1H, máximo oficial do emulador; eventual TTL maior do serviço Azure não é validável localmente |
| 3.1 verificação | 2026-09-05 | Testes focados e suíte completa aprovados; 654 testes, 0 falhas, 0 erros e 0 ignorados. Checkpoint Sonar conforme: 213 issues (baseline 213), 0 novas, 0 HIGH/BLOCKER/CRITICAL, cobertura 86,6% e duplicação 4% |
| 4.1 decisão humana adicional | 2026-09-05 | Política ativa e definições serão objetos tipados de application.properties. A progressão repete o último intervalo; lista unitária gera intervalo fixo. Máximo de tentativas ausente desativa somente esse critério, preservando duração máxima e reagendamento |
| Revisão de planejamento | 2026-09-06 | Plano e checklist alinhados às raízes `orquestrador` e `monitoramento`; migração dos cinco arquivos Java existentes permanece pendente em 4.1. Estrutura hexagonal detalhada, extensão/simuladores preservados e referências de dependência corrigidas para C0.1–C0.4, mantendo C2 após 6.1 |
| Lacunas arquiteturais | 2026-09-06 | Registradas necessidade de ampliar ArchUnit para os novos packages, definição da obtenção de parâmetros iniciais pelo orquestrador e localização da fábrica CDI única. Não foi implementado acesso entre componentes nem package técnico compartilhado |
| 4.1 continuidade Sonar | 2026-09-06 | Na leitura da retomada anterior, o estado de sessão estava `NOT_REQUIRED_UNTIL_CODE_CHANGE`, com `baseline=null`, apesar dos checkpoints registrados. A continuidade deve ser resolvida ao retomar código, preservando o workspace; esta revisão documental não executa Sonar |
| B0.2 continuidade preservada | 2026-09-06 | `sonar/` ausente. Nova referência exclusivamente local inicializada antes da migração/configuração, sem remover alterações nem reiniciar itens: 660 testes, 217 issues, 0 HIGH/BLOCKER/CRITICAL, cobertura 85,8% e duplicação 3,9%. A referência já inclui a política pura existente; não prova ausência de regressão desde o checkpoint histórico de 213 issues |
| 4.1 migração | 2026-09-06 | Duas classes de política e três testes migrados para `monitoramento`; 8 testes focados aprovados, incluindo injeção da extensão e envio/recebimento/Complete nas duas filas do emulador. Nenhuma referência Java a `doctree`/`monitoramentomtr`; `config.json` preservado |
| 4.1 configuração RED | 2026-09-06 | `mvn -q "-Dtest=PoliticasMonitoramentoConfigTest,PoliticaMonitoramentoProducerTest" test` falhou em `testCompile` pela ausência de `PoliticasMonitoramentoConfig`, antes da implementação |
| 4.1 configuração GREEN | 2026-09-06 | `PoliticasMonitoramentoConfig` mapeia política ativa e mapa de definições; producer CDI `@Singleton @Startup` valida todas e produz a selecionada. 21 casos de configuração, 1 teste CDI e 6 testes da política passaram. Valores operacionais somente em `application.properties`, sem defaults compilados |
| 4.1 bootstrap inválido | 2026-09-06 | `mvn -q "-Dtest=ServiceBusClientBuilderInjectionTest" "-Dmonitoramento.politicas.ativa=inexistente" test` falhou como esperado antes do teste, com `IllegalArgumentException` do producer durante `StartupEvent`. O teste não injeta política; a prova não depende de criação tardia. Override existiu somente nessa execução |
| 4.1 revisão Sonar | 2026-09-06 | API local identificou as quatro issues adicionais do baseline como `java:S5778`, todas nas lambdas de `assertThrows` do teste da política. Preparação de argumentos movida para fora das lambdas, preservando os cenários; as quatro issues não constam mais entre as abertas após o checkpoint |
| 4.1 verificação da subfatia | 2026-09-06 | `./validar-checkpoint-sonarqube.ps1` concluiu `clean verify`, scanner e Compute Engine. 682 testes, 0 falhas, 0 erros, 0 ignorados; build e ArchUnit existente aprovados. API local: 214 issues (baseline 217), 1 nova, 1 CRITICAL/HIGH, cobertura 86,3%, duplicação 3,9%; situação técnica `NON_COMPLIANT`, decisão humana `PENDING`. CE task `c124f962-dda0-4244-b0b8-07b05164a9fe` |
| 4.1 divergência S8911 | 2026-09-06 | Issue `7d1bbce0-46a7-4c5f-b649-5f225f3f6116`, `java:S8911`, CRITICAL / RELIABILITY HIGH, linha 21 de `monitoramento/adaptador/configuracao/PoliticaMonitoramentoProducer.java`: a regra questiona `@Startup` em producer. A guia oficial Quarkus admite producer anotado e o runtime 3.33.2.1 confirmou inicialização pelo observer sintético de StartupEvent. Registrar a divergência não suprime a issue nem autoriza exceção. Recomendação técnica: `ContinuarAjustes` para adequar a inicialização preservando o producer e o fail-fast, seguida de nova prova negativa e checkpoint |

## Ajuste autorizado de S8911 — evidências

- Decisão humana registrada pelo script em 2026-09-06, preservando o baseline de 217 issues;
- RED: `mvn -q "-Dtest=PoliticasMonitoramentoConfigTest" test` falhou em `testCompile` pela
  ausência do construtor que recebe `PoliticasMonitoramentoConfig`;
- GREEN: `@Startup` passou para a classe produtora; construtor `@Inject` valida todas as
  definições e guarda a política selecionada em campo final. O método `@Produces @Singleton`
  não recebe parâmetros e entrega a instância já validada;
- 29 testes focados passaram: 22 de configuração, 1 de injeção CDI e 6 da política. O novo teste
  comprova rejeição de configuração inválida na construção, antes de chamar o método produtor;
- Bootstrap negativo repetido: `mvn -q "-Dtest=ServiceBusClientBuilderInjectionTest"
  "-Dmonitoramento.politicas.ativa=inexistente" test` falhou como esperado no construtor do
  producer durante `StartupEvent`, antes do teste e sem injeção de política pelo consumidor.
  O override ficou restrito à execução; as properties do projeto não foram modificadas;
- Checkpoint `./validar-checkpoint-sonarqube.ps1` concluiu Maven `clean verify`, scanner e Compute
  Engine: 683 testes, 0 falhas, 0 erros e 0 ignorados, incluindo as duas provas da extensão/emulador
  e os 36 testes ArchUnit existentes. Situação técnica `COMPLIANT`, decisão Sonar `NOT_REQUIRED`;
- 213 issues abertas (baseline 217), 0 novas, 0 HIGH/BLOCKER/CRITICAL; cobertura 86,3% e duplicação
  3,9%. A nova análise não mantém o apontamento S8911 aberto; nenhuma supressão ou exceção foi usada;
- CE task `1449d4bc-ccd2-4235-846b-48f31bc7b4be`, análise `329c8342-5a74-4266-8d22-92a60a61ff9f`,
  concluída em 2026-09-06T17:17:58Z;
- Revisão do ajuste: preservados os critérios da política, a validação das definições inativas,
  a instância singleton e a direção das dependências. Sem nova dependência, rede, credencial,
  supressão de regra, mudança de properties ou alteração do caminho do emulador.

## 4.1 Contratos REST — evidências

- Request RED: `mvn -q "-Dtest=MonitoramentoDossieRestMapperTest" test` falhou em `testCompile`
  pela ausência do DTO e do modelo semântico; GREEN com request, `SolicitacaoMonitoramento` e
  mapper aprovou 22 casos;
- Response RED: o mesmo comando falhou em `testCompile` pela ausência de `MonitoramentoIniciado`;
  GREEN acrescentou o response e sua conversão, sem Resource, publicação ou geração de IDs;
- `mvn -q "-Dtest=MonitoramentoDossieRestMapperTest,ArchUnitProgressivoTest" test` aprovou 23 casos
  do contrato e 36 guardrails existentes. Os testes usam ObjectMapper e Validator reais via CDI;
- Provas: JSON camelCase, strings preservadas com zeros à esquerda, ausência de restrição UUID à
  pré-validação, campos obrigatórios, MTR decimal positivo, tolerância a campos desconhecidos e
  impossibilidade de incorporar no DTO parâmetros controlados pelo servidor;
- Revisão: cinco arquivos de produção e um teste, em duas subfatias de até quatro arquivos cada.
  Modelos próprios sem dependência no Hub/monitoramento; mapper restrito à borda REST; sem novas
  dependências, propriedades, endpoints, logs, spans, rede ou credenciais. O caminho do emulador
  permanece intacto e a busca Java não encontrou `doctree`/`monitoramentomtr`;
- Arquitetura: ADR-0011 registrado como `Proposto`, índice e consolidado atualizados. Porta/ACL,
  fábrica técnica e limite superior `Long` ainda não estavam implementados nem aceitos nessa etapa;
- `./validar-checkpoint-sonarqube.ps1` concluiu `clean verify`, scanner e Compute Engine:
  **706 testes, 0 falhas, 0 erros e 0 ignorados**, build aprovado;
- Na emissão desse checkpoint: situação técnica **NON_COMPLIANT**, decisão humana **PENDING**:
  214 issues abertas (baseline 217),
  1 nova, 0 HIGH/BLOCKER/CRITICAL; cobertura 86,3%, duplicação 3,9%;
- Issue `375d15b5-d673-499d-96e4-9de2e870b090`, regra `java:S6353`, severidade MINOR,
  impacto MAINTAINABILITY LOW, linha 11 de
  `src/main/java/br/gov/caixa/simtr/orquestrador/adaptador/entrada/rest/v1/dto/IniciarMonitoramentoDossieRequest.java`:
  o Sonar pede a sintaxe concisa de dígitos no lugar de `[0-9]`;
- Recomendação técnica: `ContinuarAjustes` para trocar o literal Java por `"\\d+"`, mantendo a
  ausência de flags Unicode e a regressão já existente que rejeita dígito não ASCII. A
  [documentação Java 25](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/regex/Pattern.html)
  confirma a equivalência sem `UNICODE_CHARACTER_CLASS`. Naquela entrega, nenhum ajuste ou
  supressão foi aplicado após o checkpoint; a decisão humana permanecia pendente;
- CE task `875cb75a-dbd0-48fe-9b0a-3ebb8e395666`, análise `aa349a9f-7902-4b97-90c0-b67d5048bebb`,
  checkpoint em 2026-09-06T17:58:26Z, fingerprint
  `4b51dd0137d3be9177c8c1741d18a20da4caff18876491d8ca71f0cc0218bae5`.

## Ajuste autorizado de S6353 — evidências

- Usuário decidiu `ContinuarAjustes`; `./validar-checkpoint-sonarqube.ps1 -HumanDecision
  ContinuarAjustes` registrou a decisão em 2026-09-06T18:22:04Z sobre o checkpoint REST;
- REFACTOR do RED/GREEN já concluído: substituição de `[0-9]+` pelo literal Java `"\\d+"`
  na linha 11 do request, sem habilitar flags Unicode. Nenhum teste, mensagem de validação,
  campo, mapper ou regra de positividade/obrigatoriedade foi alterado;
- `mvn -q "-Dtest=MonitoramentoDossieRestMapperTest" test`: 23 testes aprovados, incluindo
  rejeição de dígito não ASCII e preservação de zeros à esquerda;
- `./validar-checkpoint-sonarqube.ps1` concluiu `clean verify`, scanner e Compute Engine:
  706 testes, 0 falhas, 0 erros e 0 ignorados; build aprovado, incluindo emulador e 36 ArchUnit;
- Situação técnica `COMPLIANT`, decisão Sonar `NOT_REQUIRED`: 213 issues abertas (baseline 217),
  0 novas, 0 HIGH/BLOCKER/CRITICAL, cobertura 86,3% e duplicação 3,9%. S6353 não permanece entre
  as issues abertas; nenhuma supressão nem aceitação excepcional foi utilizada;
- CE task `19eb96ab-d095-4673-821d-b97dc630e121`, análise `188b8435-dbed-427c-ab38-3fb362044afe`,
  checkpoint em 2026-09-06T18:35:50Z, fingerprint
  `7f98724d28316d57b7d5eb91c996e0d8bc6b41625b41fbf16f19f00de80a70e6`;
- Revisão: uma única linha executável alterada, sem mudança de semântica, dependências,
  arquitetura, autenticação, configuração ou sinais observáveis. Registros atualizados somente
  na pasta da feature. ADR-0011 e limite superior MTR permaneciam pendentes de decisão humana
  nessa etapa.

## Revisão documental dos limites transversais — 2026-09-06

- Orientação humana explicitada no ADR-0011, índice, consolidado arquitetural, plano e checklist:
  infraestrutura Service Bus em `br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus`,
  exclusivamente técnica; execução assíncrona não transfere regras de negócio para esse package;
- Quarkus reativo/Mutiny e CDI permitidos no domínio e na aplicação. Corrigida no plano a restrição
  contraditória de Mutiny somente às bordas; SDK, transporte e bloqueio do event loop continuam
  sujeitos às fronteiras e restrições registradas;
- `br.gov.caixa.simtr.hub` e suas capacidades em `hub.arquitetura` preservados. Nenhuma classe
  movida, fábrica implementada ou package técnico criado; extensão, simuladores e config do emulador
  permanecem inalterados;
- Apenas Markdown alterado, sem Maven ou Sonar nesta revisão. Os resultados executáveis acima
  são do incremento anterior, não uma nova análise. Naquela revisão, o ADR-0011 permanecia
  `Proposto` nos pontos restantes; ainda não havia aceite da porta/ACL, dos detalhes CDI ou do
  limite superior MTR.

## Aceite humano do ADR-0011 — 2026-09-06

- Usuário declarou explicitamente “ADR-0011 aprovado”; ADR alterado de `Proposto` para `Aceito`,
  com índice, arquitetura, plano e checklist sincronizados;
- Aceite abrange a colaboração local por porta/ACL e a composição técnica CDI descritas no ADR.
  Mantém infraestrutura em `br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus`, regras
  assíncronas nos componentes, Quarkus reativo permitido no domínio e Hub preservado;
- Naquela etapa, o limite positivo compatível com `Long` no contrato MTR permanecia pendente
  de decisão própria; o aceite subsequente está registrado abaixo;
- Atualização somente documental, sem alterações de código, Maven, Sonar, commit ou avanço de
  item. Porta/ACL e fábrica continuam sem implementação; os resultados executáveis anteriores
  não representam nova análise nesta atualização.

## GO do limite MTR — 2026-09-06

- Aprovação contratual explícita e GO registrados antes de editar código;
- Baseline local existente `READY`, último checkpoint `COMPLIANT` e `NOT_REQUIRED`; fingerprint
  executável confirmado igual a `7f98724d28316d57b7d5eb91c996e0d8bc6b41625b41fbf16f19f00de80a70e6`;
- Sonar local `UP`, token herdado disponível sem exposição do valor e nenhum pacote em `sonar/`.
  Baseline preservado, sem reinicialização. Esta evidência precede o incremento, não o aprova;
- RED executado com `mvn -q "-Dtest=MonitoramentoDossieRestMapperTest" test`: 32 testes,
  exatamente 4 falhas nos valores acima do teto; nenhum erro ou teste ignorado;
- GREEN: adicionado somente `@DecimalMax` ao request, preservando `@NotBlank`, `@Pattern`,
  `@DecimalMin`, strings, mapper e JSON. O mesmo comando passou os 32 testes sem alterar o RED;
- Novos cenários: mínimo, máximo inclusivo, vizinho inferior, preservação de zeros à esquerda
  inclusive com mais de 19 caracteres, primeiro valor acima do teto e valores maiores que `Long`;
- `./validar-checkpoint-sonarqube.ps1` concluiu `clean verify`, scanner e Compute Engine:
  715 testes, 0 falhas, 0 erros e 0 ignorados; build aprovado;
- Situação técnica `NON_COMPLIANT`, decisão humana `PENDING`: 214 issues abertas (baseline 217),
  1 nova, 0 HIGH/BLOCKER/CRITICAL, cobertura 86,3% e duplicação 3,9%;
- Issue nova `a8bf0691-bd18-48c5-b70f-d8a2738a1ade`, regra `java:S4144`, severidade `MAJOR`,
  impacto de manutenibilidade `MEDIUM`, em `MonitoramentoDossieRestMapperTest.java:97`: corpo
  idêntico ao teste `deveRejeitarMtrAusenteOuDiferenteDeInteiroDecimalPositivo`, linha 68;
- Recomendação submetida à decisão humana: `ContinuarAjustes` para consolidar os cenários de
  rejeição em um teste parametrizado, preservando casos e cobertura. Nenhum ajuste adicional,
  supressão ou aceitação excepcional foi aplicado após o checkpoint;
- CE task `9bb95244-7812-4a37-9acd-e3e3a7921d68`, análise `6f747f79-0990-45a5-8360-f1f90e58c5c6`,
  checkpoint em `2026-09-06T17:16:20.8928001-03:00`, fingerprint
  `9b7e58ff571e2b73c2df532635545edc3f8cedabdc13bec11ca10e277e097d3f`;
- Revisão: mudança executável restrita ao request REST e ao teste; sem novos tipos, dependências,
  propriedades, endpoint, rede ou lógica de negócio. Preservados Hub, extensão, simuladores e
  `src/main/azure/servicebus-emulator/config.json`. Simplicidade e validação de borda mantidas;
  duplicação de corpo de teste apontada pelo Sonar exigia a decisão posteriormente registrada abaixo.

Referências conferidas contra Quarkus `3.33.2.1`, Jakarta Validation API `3.1.1` e Hibernate
Validator `9.1.0.Final` do build efetivo:

- [Jakarta Validation 3.1 — DecimalMax, suporte a CharSequence e limite inclusivo](https://jakarta.ee/specifications/bean-validation/3.1/apidocs/jakarta/validation/constraints/decimalmax);
- [Quarkus — validação com Hibernate Validator](https://quarkus.io/guides/validation/).

## Ajuste autorizado de S4144 — evidências

- Usuário decidiu `ContinuarAjustes`; registro feito pelo script em
  `2026-09-06T17:46:40.762221-03:00`, antes da edição do teste;
- Baseline existente `READY`/`LOCAL_SONAR` preservado; fingerprint inicial igual ao checkpoint
  de S4144, token herdado disponível sem exposição e nenhum pacote offline em `sonar/`;
- REFACTOR do RED/GREEN já concluído: os quatro valores de overflow foram transferidos para o
  teste parametrizado de rejeição, renomeado para `deveRejeitarMtrAusenteOuForaDoContratoLongPositivo`.
  Removido somente o corpo redundante; todos os dados e asserções foram preservados;
- `mvn -q "-Dtest=MonitoramentoDossieRestMapperTest" test` aprovado após a consolidação;
  os mesmos 32 cenários também passaram na suíte completa, sem falhas, erros ou ignorados;
- Nenhuma alteração no request, nas constraints, no mapper, nos modelos, nos contratos JSON,
  na arquitetura ou na extensão/simuladores. Não houve supressão Sonar nem redução de cenários;
- `./validar-checkpoint-sonarqube.ps1` concluiu `clean verify`, scanner e Compute Engine:
  715 testes, 0 falhas, 0 erros e 0 ignorados; build aprovado;
- Situação técnica `COMPLIANT`, decisão Sonar `NOT_REQUIRED`: 213 issues abertas (baseline 217),
  0 novas, 0 HIGH/BLOCKER/CRITICAL, cobertura 86,3% e duplicação 3,9%;
- Consulta da ocorrência `a8bf0691-bd18-48c5-b70f-d8a2738a1ade` confirmou `java:S4144` como
  `CLOSED`/`FIXED`, sem supressão nem aceitação excepcional;
- CE task `27d21534-bd09-46e1-be12-efb0d73007ea`, análise `ae87fcb7-e8e4-46a2-824a-bd0949037dab`,
  checkpoint em `2026-09-06T18:08:17.1932246-03:00`, fingerprint
  `5dfa2bf6024227e7b0291a4488dfe8bec8f46bcd76b8f23b04f4684e1ff393e5`;
- Revisão: dados e asserções dos dois testes de rejeição reunidos em um único teste parametrizado,
  mantendo entradas nulas/vazias, formato inválido, zero/negativos e os quatro casos de overflow.
  Testes positivos e casos de JSON/mapeamento intactos. Mudança executável em um único arquivo
  de teste, sem alterar produção, dependências, segurança, desempenho ou sinais observáveis.

## Ponto de retomada após o ajuste de S4144 — histórico

Workspace e branch preservados, sem commit e sem comandos em execução. Configuração/producer e ajustes S8911/S6353
permanecem verificados. ADR-0011 e limite MTR aprovados; RED/GREEN do teto REST concluído.
`ContinuarAjustes` de S4144 registrado; consolidação do teste verificada com 32 testes focados,
715 completos e novo checkpoint `COMPLIANT`. Nenhuma decisão Sonar pendente. Retomar
contratos/mappers Service Bus e guardrails restantes de 4.1 conforme arquitetura aprovada.
Não reapresentar os aceites arquitetural/contratual como pendentes nem reiniciar a validação.
Não reiniciar política/configuração, refazer a migração nem avançar para 5.1. O item 4.1 e a feature
continuam em andamento; nenhum endpoint de monitoramento foi exposto.

## 4.1 Contrato da fila de entrada — evidências recuperadas em 2026-09-07

- Produtor: modelo próprio em `orquestrador.dominio.modelo`, DTO v1 e mapper em
  `orquestrador.adaptador.saida.servicebus`. JSON com nove campos e propriedades AMQP
  determinísticas; sem publicação ou agendamento. Três testes cobrem corpo, propriedades e
  compatibilidade JSON com o consumidor independente.
- Consumidor: modelo próprio em `monitoramento.dominio.modelo`, DTO v1 e mapper em
  `monitoramento.adaptador.entrada.servicebus`. Validação de envelope, tipos JSON, campos
  obrigatórios, schema, tentativa positiva, limite MTR e datas. Os 61 casos incluem JSON
  inválido, ausência/nulidade, overflow, envelope divergente, conteúdo após o objeto e campos
  desconhecidos. Falha tipada sem payload ou causa do parser; sem SDK no modelo interno.
- Preservados IDs, zeros à esquerda, prazo original e versão da política. `DeliveryCount`
  não é tentativa funcional. A compatibilidade usa JSON, sem DTO Java compartilhado.
- 161 testes focados informados pelo usuário, correspondentes a contrato de entrada (61),
  produtor/compatibilidade (3), REST (32), configuração (22), producer CDI (1), política (6)
  e ArchUnit existente (36). Relatórios Surefire preservados confirmam esses totais e os
  779 testes completos: zero falhas, erros ou ignorados.
- Último checkpoint informado pelo usuário: `NON_COMPLIANT`, uma issue nova `java:S7467`,
  cobertura 86,5%, duplicação 3,9%, nenhuma HIGH/BLOCKER/CRITICAL. O arquivo `report-task.txt`
  preserva a CE task `659df2e9-2046-41f3-a99d-548da50cb64d`, de 2026-09-06.
- `ContinuarAjustes` já registrado em `2026-09-06T20:45:43-03:00`, conforme confirmação humana.
  O ajuste ainda não estava aplicado: linha 60 contém `catch (JsonProcessingException ignored)`.

## Retomada de S7467 — 2026-09-07

- Branch e alterações rastreadas/não rastreadas preservadas. Nenhum commit ou troca de branch.
- Divergência confirmada antes da edição: `.codex/.state/session.json`, atualizado em
  `2026-09-07T07:55:17-03:00`, contém `NOT_REQUIRED_UNTIL_CODE_CHANGE`, baseline e checkpoint
  nulos. O hook `sonar-session-start.ps1` sobrescreve esses campos no início da sessão.
- Token presente no processo, verificado somente como booleano; `sonar/` ausente. A ausência
  do snapshot original não será tratada como nova autorização para inicializar baseline.
- Referência original recuperada dos resultados completos das ferramentas da sessão anterior:
  baseline capturado em `2026-09-06T11:34:28.1558818-03:00`, análise
  `f6183a72-a2ea-44bc-9374-b2b064bdad55`, 217 issues, cobertura 85,8% e duplicação 3,9%.
  Comparação integral do objeto restaurado com o snapshot histórico aprovada; SHA-256 do JSON
  canônico: `5011E6E15B808AC8FCBB633B7174A5E9AF11B51C759FEC91B1B232F4313066D5`.
- Último checkpoint e decisão também recuperados: análise
  `4c8a9c91-8dba-4a5a-8f22-7fb52c52c666`, fingerprint
  `d597630d43c431598a346463a6eb11c88463854cc3174fb49f41e57b98610195`,
  `CONTINUE_ADJUSTMENTS` em `2026-09-06T20:45:43.39731-03:00`. Fingerprint atual conferido
  antes da edição. Cópias `session-before-s7467-recovery-20260907.json` e
  `session-restored-s7467-20260907.json` preservadas em `.codex/.state/`.
- API local confirmou a S7467 `55eabafd-7bf6-4e13-a4ba-159ce83247d0`, MINOR/LOW,
  aberta na linha 60; 214 issues, cobertura 86,5% e duplicação 3,9%.
  Nenhum baseline reinicializado, hook alterado ou decisão humana repetida.
- Após a correção e seu checkpoint, permanecem DTO/mapper de reagendamento, contratos de
  resultado e novos guardrails ArchUnit no item 4.1. O item 5.1 permanece pendente.

## Ajuste autorizado de S7467 — evidências

- Aplicado somente `ignored` -> `_` no catch da linha 60 do mapper consumidor. Corpo,
  comentário, imports e todos os testes preservados. A falha continua sendo traduzida para
  `ContratoMonitoramentoInvalidoException`, sem payload ou causa do parser; sem log adicional.
- A dúvida sobre log foi avaliada contra o tratamento existente e o plano: o mapper classifica
  a falha de contrato; o futuro listener dispõe do contexto da mensagem e executará o tratamento
  operacional/DLQ. Registrar a exceção original poderia expor trechos do payload.
- Regressão executada com o mesmo conjunto de 161 casos:

  ```powershell
  mvn -q "-Dtest=MonitoramentoEntradaContratoTest,MonitoramentoEntradaServiceBusMapperTest,MonitoramentoDossieRestMapperTest,PoliticasMonitoramentoConfigTest,PoliticaMonitoramentoProducerTest,PoliticaMonitoramentoProgressivaTest,ArchUnitProgressivoTest" test
  ```

  Sete classes e 161 testes aprovados, zero falhas, erros ou ignorados. Relatórios atualizados
  em `2026-09-07T08:27:32-03:00` a `2026-09-07T08:27:36-03:00`.
- `./validar-checkpoint-sonarqube.ps1` concluiu `clean verify`, scanner e Compute Engine.
  779 testes, zero falhas, erros ou ignorados; build e ArchUnit existente aprovados.
- Situação técnica `COMPLIANT`, decisão Sonar `NOT_REQUIRED`: 213 issues abertas (baseline 217),
  nenhuma nova, nenhuma HIGH/BLOCKER/CRITICAL, cobertura 86,5% e duplicação 3,9%.
- Ocorrência `55eabafd-7bf6-4e13-a4ba-159ce83247d0` confirmada pela API local como
  `CLOSED`/`FIXED`, sem supressão ou aceitação excepcional.
- CE task `d3f2549e-4a21-4236-9b76-6ed86536f06f`, análise
  `14d98817-619a-4693-954e-fdfb2dbc9583`, checkpoint em
  `2026-09-07T08:32:07.7908983-03:00`, fingerprint
  `e75c03d957d3658f645d7659c8de8cb60da9dff6baab96848e51248832132c52`.
- Baseline comparado integralmente com a cópia recuperada depois do checkpoint: idêntico.
  Fingerprint do código igual ao analisado. Nenhuma reinicialização nem nova decisão humana.
- Revisão de correção, simplicidade, arquitetura, segurança, desempenho, testes e escopo:
  comparação dos hashes de 627 arquivos inventariados encontrou mudanças somente no mapper,
  plan.md e todo.md. A inversão da única substituição reproduz o hash original do mapper;
  nenhum teste alterado ou arquivo inventariado removido. `git diff --check` aprovado.
- A tradução e o comportamento permanecem iguais, sem dependência, log, contrato, configuração
  ou custo de processamento adicional. Extensão, simuladores, Hub e
  `src/main/azure/servicebus-emulator/config.json` preservados. Sem commit ou troca de branch.
- Ponto de retomada: reagendamento, contratos de resultado e novos guardrails ArchUnit em 4.1.
  O item 4.1 e a feature continuam em andamento; 5.1 permanece pendente.

## Proposta C4.2 — log de erro JSON — 2026-09-07

- Leitura do DTO de erro REST, mappers de exceção, helper de logs, configuração JSON e testes
  existentes. Constatado que a resposta REST e o registro de log atual têm estruturas diferentes:
  o helper converte valores em strings, e não inclui integralmente o DTO da resposta.
- Proposta adicionada ao plano com intenção, escopo, exemplo JSON, códigos/eventos, nível,
  diagnóstico sanitizado, critérios/testes, riscos e sequência em subfatias de 4.1.
- A estrutura JSON de erro mantém os nomes/tipos do REST; DTO/helper do Hub não serão importados
  pelas bordas Service Bus. O campo codigo_http será omitido em AMQP. Stack técnico contém tipo
  e frames, sem mensagem original ou causas; erros de validação têm mensagens controladas.
- Pergunta de esclarecimento apresentada ao usuário sobre somente log ou armazenamento para
  processamento posterior. Log é a hipótese de trabalho, não uma decisão humana inferida.
- Nenhum código, configuração, hook ou dependência alterado nesta revisão; sem Maven/Sonar,
  reinicialização de baseline ou atualização de formatos derivados. S7467 e seu checkpoint
  COMPLIANT permanecem como evidência do incremento anterior, não como aprovação de C4.2.
- Histórico da proposta: antes da resposta humana, C4.2 estava pendente. O usuário respondeu
  “GO” em 2026-09-07 ao formato apresentado, começando pelas bordas Service Bus de 4.1,
  inicialmente somente em logs. Esse aceite está registrado na tabela de decisões acima.
- Retomada após GO: baseline original READY e último checkpoint COMPLIANT preservados, sem
  reinicialização. Primeiro passo técnico: caracterizar o formatter instalado para garantir
  `erros` como array JSON real antes da implementação do consumidor.

## C4.3 autorizado com preservação do Hub — 2026-09-07

- Usuário: “vamos fazzer essa alteração apenas para o novo desenvololvimento deixando o código
  e log e erro do simtr.hub inaleterado”. Autorização condicionada registrada sem ampliar o Hub.
- Seleção explícita por categoria e marcador no formatter; Hub sempre excluído. Testar delegação
  byte a byte, inclusive com marcador em categoria Hub, e conferir hashes de código/testes.
- Permanecem somente logs inicialmente, baseline preservado e limite no item 4.1.

## C4.2 aprovado e prova do formatter — 2026-09-07

- GO humano registrado para o formato apresentado e destino inicialmente somente logs.
- Prova temporária em `%TEMP%/simtr-c42-formatter-20260907/`, com
  `ProvaFormatoErroC42.java` e `resultado.txt`; executada por Java source-file com classpath
  das bibliotecas de `target/quarkus-app/lib/{main,boot}`, sem download ou alteração de build.
- O formatter instalado converte array e número MDC em texto. O protótipo de decorator passou
  seis verificações de JSON tipado e uma de delegação idêntica byte a byte para registro de Hub.
  Limites explícitos: sem instalação em handlers, lifecycle, concorrência ou integração do mapper.
- Proposta técnica C4.3 registrada no plano e ADR-0012 como Proposto, com índice atualizado.
  Ela alcança handlers compartilhados e nova capacidade transversal; depende do checkpoint
  exigido por C4.2 e ADR-0011. O GO de C4.2 permanece válido e não precisa ser repetido.
- Nenhuma alteração executável no repositório nesta subfatia; nenhum novo RED/GREEN, Maven
  ou checkpoint Sonar. Não confundir a prova isolada com implementação do requisito.
  Baseline e último checkpoint do incremento S7467 permanecem preservados.

## Composição técnica C4.3 — evidências de 2026-09-07

- Três classes novas em `arquitetura.infraestrutura.observabilidade`: marcador técnico
  `CamposLogJson`, decorator `FormatoLogJsonServiceBus` e instalação/restauração CDI
  `ConfiguracaoLogJsonServiceBus`. Dois arquivos de teste novos; fatia de cinco arquivos Java.
- A seleção exige marcador específico e categoria de adapter Service Bus de monitoramento ou
  orquestrador. Hub e demais categorias são delegados sem alteração, inclusive com marcador.
  DTOs, códigos, mensagens e sanitização de erros continuam responsabilidade das bordas.
- RED: testes falharam em `testCompile` pela ausência de `FormatoLogJsonServiceBus` e
  `CamposLogJson`, antes da implementação. Log: `%TEMP%/simtr-c43-red-20260907.log`.
- Primeira execução após implementação: 19 casos passaram e a prova de arquivo falhou porque
  `src/test/resources/application.properties` define `quarkus.log.file.enabled=false`.
  Corrigida somente a fixture com profile exclusivo, sem alterar configuração existente.
- GREEN/regressão: 184 testes, zero falhas, erros ou ignorados: 20 novos, 161 do conjunto anterior
  e 3 de `ObservabilidadeLogsContratoTest`. Log: `%TEMP%/simtr-c43-focused-20260907.log`.
- Provas cobrem array/objeto/número JSON, metadados, escaping/delimitador, categorias excluídas,
  Throwable proibido, colisão de metadados, 40 ocorrências concorrentes, instalação idempotente,
  handlers intermediários, restauração condicional e emissão no arquivo real do profile exclusivo.
- Revisão independente, somente leitura, não identificou bug concreto no estado/configuração
  atuais. Indicou completar asserts do console real JSON/textual e do lifecycle de shutdown.
  A existência de console JSON no output não substitui o teste desse destino.
- Checkpoint completo: 799 testes, zero falhas, erros ou ignorados; análise
  `aa112515-6c37-44ae-a2c3-6cbbc06e35bf`, CE
  `837f2a64-07f2-48d8-a4e5-9c1451925f42`, em
  `2026-09-07T09:34:54.5352269-03:00`; fingerprint
  `3fd717ba8ebda4571369d5a31d5f3ac1ee83d11a540a8c2f02bf8340f16106f8`.
- Situação `NON_COMPLIANT`, decisão `PENDING`: 215 issues abertas, duas novas, nenhuma
  HIGH/BLOCKER/CRITICAL; cobertura 86,5%, duplicação 3,9%. Baseline original comparado integralmente
  após o checkpoint: idêntico, sem reinicialização.
- Issues consultadas como dados na API local:
  - `87a1c2cd-bf0b-404e-a153-5be8ee0584c7`, `java:S6878`, MAJOR/MEDIUM,
    `FormatoLogJsonServiceBus.java:31`: usar record pattern;
  - `ffefd8f1-59a2-46a1-bed5-39ce309f0ccf`, `java:S5786`, INFO,
    `ConfiguracaoLogJsonServiceBusTest.java:21`: remover `public` da classe de teste.
- Decisão humana solicitada conforme AGENTS.md. Ajustes propostos: record pattern, visibilidade
  do teste e conclusão das provas de console/lifecycle; aguardar resposta antes de editar código.
- Inventário anterior ao incremento em `%TEMP%/simtr-c43-audit-20260907.json`;
  hashes de 493 arquivos do Hub conferidos sem mudança. `git diff --check` aprovado.
  Sem commit, troca de branch, reset, clean, dependência nova ou alteração de configuração existente.
- C4.3 técnico ainda em andamento. O mapper consumidor ainda não emite o novo log; permanecem
  consumidor, produtor e demais contratos/guardrails de 4.1. Não avançar para 5.1.

## Continuidade autorizada de C4.3 — 2026-09-07

- Usuário respondeu `ContinuarAjustes`; script registrou `CONTINUE_ADJUSTMENTS` em
  `2026-09-07T09:53:45.1657041-03:00`, sobre o checkpoint de S6878/S5786. Sem aceite excepcional.
- Ajustar somente record pattern e visibilidade do teste, completar captura do console real
  JSON/textual e notificação do observer CDI de encerramento/restauração dos handlers reais.
  Não disparar os demais observers de shutdown nem encerrar parcialmente a aplicação no teste.
- Executar caracterização/regressão e novo checkpoint com baseline original e Hub preservados.

## Ajustes C4.3 concluídos — 2026-09-07

- S6878 corrigida com record pattern; verificação do marcador precede o stream de categorias,
  preservando caminho direto ao delegate para logs existentes. S5786 corrigida removendo
  `public` somente da classe de teste.
- Captura do ConsoleHandler real nos testes: flush, troca temporária para memória e restauração
  do target definido pela configuração. Console JSON e arquivo real emitem exatamente um registro
  de cada evento, com comparação de seus objetos JSON; console textual real mantém a saída original.
- Observer CDI de ShutdownEvent resolvido/notificado seletivamente: restaura os dois handlers
  reais; observer StartupEvent é notificado no finally para reinstalar a composição. Prova local
  de CDI/lifecycle, sem disparar outros observers ou afirmar shutdown completo do processo.
- Caracterização: 22 testes de logging passaram antes das correções sintáticas. Depois dos
  ajustes, 186 focados e 801 completos passaram, zero falhas/erros/ignorados.
- Revisão independente dos ajustes não encontrou bug ou risco material e confirmou que a lacuna
  do console foi coberta. Sem alteração de configuração, dependência, destino ou código do Hub.
- Checkpoint `COMPLIANT` em `2026-09-07T10:17:38.3543906-03:00`: 213 issues, zero novas,
  zero HIGH/BLOCKER/CRITICAL, cobertura 86,5%, duplicação 3,9%, decisão `NOT_REQUIRED`.
  S6878 e S5786 confirmadas pela API como `CLOSED/FIXED`.
- Análise `602d2c0a-8150-4192-bcbf-34db3245be81`, CE
  `d373f598-f9fe-423a-ac2b-c668cf412847`, fingerprint
  `90a565ae12f7799d7aaf99f2ff5e852534b651e3155ed16fa7e2507f3154f504`.
  Baseline integralmente idêntico ao original e fingerprint conferido antes da próxima fatia.
- Inventário de 627 arquivos preservado sem ausências; entre os arquivos inventariados somente
  plan/todo/consolidado arquitetural mudaram. 493 arquivos do Hub sem alteração de hash.
  Os arquivos novos de logging são adicionais; `git diff --check` aprovado. Sem commit.
- C4.3 concluído. Próxima subfatia já autorizada em C4.2: consumidor registra erro JSON e propaga
  classificação/identidade da ocorrência; preservar aceitação/rejeição dos 61 casos existentes.
  Nenhum avanço para 5.1.

## Log JSON do consumidor concluído — 2026-09-07

- RED comportamental: dois testes executados antes da alteração de produção; sucesso passou,
  parsing falhou por esperar um registro e receber zero. Log:
  `%TEMP%/simtr-consumidor-log-red-20260907.log`.
- GREEN inicial: 66 testes; ampliação para 18 cenários de log e regressão de 204 testes focados,
  todos aprovados. Após centralizar os nomes das operações, 82 testes do consumidor/compatibilidade
  passaram. Logs `simtr-consumidor-log-green-20260907.log`,
  `simtr-consumidor-log-focused-20260907.log` e `simtr-consumidor-log-refactor-20260907.log` em `%TEMP%`.
- Mapper classifica parsing, envelope AMQP e contrato v1, registra um ERROR com campos tipados
  do padrão REST e propaga `ContratoMonitoramentoInvalidoException` com o mesmo id/código.
  Aceitação/rejeição dos contratos preservada; teste observa o arquivo JSON real.
- Diagnóstico técnico contém somente classe e frames, linha/coluna numéricas positivas quando
  disponíveis e trace válido quando presente. Mensagem original, payload, valores rejeitados,
  sourceRef, causas e suppressed não são registrados. Falha propagada mantém mensagem controlada
  e causa nula; não houve listener, settlement ou ação automática.
- Revisão independente dos cinco arquivos não identificou bug, risco material ou lacuna relevante.
- Checkpoint completo: 819 testes, zero falhas/erros/ignorados; `COMPLIANT` em
  `2026-09-07T10:44:11.0098197-03:00`, 213 issues, nenhuma nova, nenhuma HIGH/BLOCKER/CRITICAL,
  cobertura 86,6%, duplicação 3,8%, decisão `NOT_REQUIRED`.
- Análise `f952ab1c-25da-4a56-89e4-114a71fafe3b`, CE
  `7b8263c8-db6d-478c-9f6e-3741ca30c2a1`, fingerprint
  `fba8ccd56a3ba7fee3301b1d220a5d71730b3f5aaab45c227e9f0cab9375db9b`.
  Baseline comparado integralmente: idêntico. Fingerprint atual confere.
- Próxima subfatia C4.2 já aprovada: falha de serialização no produtor, com DTO/registro próprios,
  preservando o contrato checked `JsonProcessingException` por subtipo local com id/código,
  sem causa original. A fila continua obtida da configuração existente. RED antes de produção,
  depois regressão e checkpoint. Reagendamento/resultado/guardrails seguem pendentes; 5.1 não iniciado.

## Log JSON do produtor — 2026-09-07

- RED comportamental executado antes da produção: dois testes, uma falha esperada
  (um registro esperado, zero recebido), sucesso aprovado. Log
  `%TEMP%/simtr-produtor-log-red-20260907.log`.
- GREEN inicial com o contrato/compatibilidade existente aprovado; ampliação para cinco casos
  de log e regressão focada de 209 testes em 13 classes, zero falhas/erros/ignorados.
- Mapper preserva JSON e AMQP de sucesso, captura somente `JsonProcessingException`, registra
  um ERROR com evento `orquestrador.servicebus.entrada.falhou` e código
  `ORQUESTRADOR_ENTRADA_SERIALIZACAO_FALHOU`. DTO/helper independentes na borda do orquestrador.
  Recurso obtido da configuração da fila existente somente no caminho de erro.
- `SerializacaoEntradaException` preserva o contrato checked por subtipo e propaga o mesmo
  id/código do registro, com mensagem controlada e sem causa/location/processor originais.
  Testes observam o arquivo JSON real, array `erros`, UUID, metadados e ausência de `codigo_http`.
- Serializador Jackson real de teste contém sentinelas na mensagem, causa, suppressed e processor;
  nenhuma aparece no registro ou na exceção devolvida. Stack mantém o frame do mapper.
  Trace válido aparece somente na ocorrência corrente; ids não se repetem. Falha inesperada
  propaga por identidade, sem reclassificação. Sucesso não gera log de erro.
- Revisão independente dos quatro arquivos de produção e do teste não encontrou bug ou risco
  concreto e confirmou cobertura das lacunas apontadas. Sem mudança adicional de produção.
- Inventário de 627 arquivos sem ausências; somente plan/todo/consolidado, os dois mappers de
  entrada e a exceção do consumidor mudaram entre os arquivos inventariados. Novos arquivos são
  adicionais. Todos os 493 arquivos do Hub mantêm o hash anterior; `git diff --check` aprovado.
  Sem commit, reset, clean ou troca de branch. Extensão, configurações e emulador preservados.
- Checkpoint completo: 824 testes, zero falhas/erros/ignorados, build e análise concluídos;
  `NON_COMPLIANT`, decisão `PENDING`, em `2026-09-07T14:26:27.2559322-03:00`.
  215 issues, duas novas MAJOR/MEDIUM, nenhuma HIGH/BLOCKER/CRITICAL;
  cobertura 86,7%, duplicação 3,8%.
- Análise `cf324b27-f972-405b-b659-01890966f166`, CE
  `2fac3810-7bb6-4564-b3fe-442e66216779`, fingerprint
  `b9b07f9bbc4bedb7492e3466e308885d060937771c392966c7cd2cf6eee9765f`.
  Baseline comparado integralmente: idêntico. Fingerprint atual confere.
- Issues consultadas na API local:
  - `ee7fec89-5053-4239-a761-01577316b071`, `java:S110`, MAJOR/MEDIUM,
    `SerializacaoEntradaException.java:6`: seis ancestrais, máximo cinco;
  - `38b7346f-bcbe-48f1-9e67-1e13e1cb0e61`, `java:S5961`, MAJOR/MEDIUM,
    `OrquestradorEntradaLogTest.java:54`: 29 asserções, exige menos de 25.
- Proposta concreta para decisão humana: manter falha checked e id/código com exceção local
  derivada de `IOException`; o mapper de visibilidade package-private declara
  `throws SerializacaoEntradaException`. Isso altera o tipo técnico anteriormente baseado
  em Jackson neste mapper novo; preservar todos os campos JSON/AMQP, sanitização e Hub.
  Dividir o primeiro teste entre contrato JSON e identidade/propagação, preservando asserções.
  Não suprimir regras, alterar baseline nem transportar id por campos artificiais do Jackson.
- Revisão independente da proposta confirmou ausência de callers de produção atuais; somente
  testes do mesmo package chamam o mapper. A mudança deixa de ser capturável por
  `catch (JsonProcessingException)` e deve ser autorizada explicitamente na decisão humana.
  Divisão do teste por responsabilidades preserva cobertura, sem contornar a regra.
- Aguardar `Reprovar`, `AceitarExcepcionalmente` ou `ContinuarAjustes` conforme AGENTS.md;
  registrar apenas a resposta humana efetiva. Os ajustes acima ainda não foram aplicados.
  Nenhum comando permanece em execução; nenhum commit. 4.1 não concluído; 5.1 não iniciado.

## Continuidade parcial: S5961 e esclarecimento de S110 — 2026-09-07

- Usuário pediu esclarecimento sobre S110 e afirmou querer RuntimeException e tratamento correto.
  Autorizou expressamente: “para S5961 pode continuar o ajuste”.
- `ContinuarAjustes` registrado pelo script em `2026-09-07T14:38:52.9539656-03:00`,
  exclusivamente para a divisão do teste S5961. Essa decisão não autoriza a proposta anterior
  de IOException nem representa aceite excepcional de S110.
- S110 está na exceção do produtor, atualmente checked por herdar JsonProcessingException.
  A exceção do consumidor já herda RuntimeException. Cadeia do produtor tem seis ancestrais;
  o limite da regra é cinco. O diagnóstico não afirma que seja necessário usar IOException.
- Proposta alinhada à preferência humana: produtor passa a `extends RuntimeException`,
  preservando captura específica do Jackson, log JSON sanitizado único e propagação tipada
  de id/código. Remover a declaração checked do mapper e verificar propagação unchecked.
  A exceção original continua fora da causa propagada, conforme o contrato sanitizado aprovado.
  Isso substitui a proposta de IOException, mas não será aplicado neste incremento.
- Antes de implementação de S5961: baseline integral idêntico e fingerprint conferido
  `b9b07f9bbc4bedb7492e3466e308885d060937771c392966c7cd2cf6eee9765f`.
- Alterar somente OrquestradorEntradaLogTest: dividir o caso de 29 asserções em contrato JSON
  e identidade/propagação, cada um provocando sua ocorrência e verificando emissão única.
  Preservar todas as verificações, os demais testes e todo código de produção.
- Executar regressão focada e checkpoint com baseline original. S110 deverá continuar aberta
  enquanto a hierarquia não mudar; não inferir conformidade completa ou decisão sobre ela.

## S5961 concluída; S110 permanece em discussão — 2026-09-07

- Um teste foi dividido em contrato JSON e identidade/propagação, com ocorrência e captura
  próprias. Todas as 29 verificações foram preservadas; cada teste exige uma única emissão.
  Produção e os demais testes não foram alterados.
- Regressão focada: seis testes de log e três de contrato/compatibilidade, nove aprovados,
  zero falhas/erros/ignorados. Revisão independente contra cópia anterior confirmou
  preservação das verificações e responsabilidades distintas, sem bug ou risco material.
- Checkpoint completo: 825 testes, zero falhas/erros/ignorados; build/análise concluídos.
  `NON_COMPLIANT`, decisão `PENDING`, em `2026-09-07T14:52:05.5299633-03:00`.
  214 issues, uma nova MAJOR/MEDIUM (S110), nenhuma HIGH/BLOCKER/CRITICAL;
  cobertura 86,7%, duplicação 3,8%.
- API local confirma S5961 `38b7346f-bcbe-48f1-9e67-1e13e1cb0e61` como `CLOSED/FIXED`.
  S110 `ee7fec89-5053-4239-a761-01577316b071` continua `OPEN` em
  `SerializacaoEntradaException.java:6`; nenhum ajuste da hierarquia foi aplicado.
- Análise `d58b8d22-e962-4f38-a480-c46e744ff5a2`, CE
  `f75d120f-6a0a-4620-b28a-3d085bd36c37`, fingerprint
  `d875348a293025c42004a97ddd0db29d754c213e386b972e4d3c50b773a56551`.
  Baseline integral idêntico e fingerprint atual conferido após o checkpoint.
- Inventário `%TEMP%/simtr-s5961-audit-20260907.json`: 641 arquivos, nenhuma ausência;
  antes desta atualização documental somente OrquestradorEntradaLogTest tinha hash diferente.
  Todo código de produção, Hub, extensão, configurações e emulador preservados.
  `git diff --check` aprovado. Sem commit, troca de branch, reset ou clean.
- Explicação S110 conferida no bytecode Jackson 2.21.2: o produtor herda
  JsonProcessingException -> JacksonException -> IOException -> Exception -> Throwable -> Object
  (seis ancestrais); com RuntimeException direto seriam quatro. A escolha unchecked remove
  a obrigação do compilador, mas preserva a possibilidade de catch tipado e propagação.
  Referência: [RuntimeException — Java 25](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/RuntimeException.html).
- Próxima decisão permanece restrita a S110: proposta RuntimeException própria com id/código,
  captura específica do Jackson, log sanitizado único e interrupção da montagem da mensagem.
  Não traduzir falhas inesperadas por captura genérica; não encadear a causa original com payload
  no contrato sanitizado vigente. Retry/settlement/listeners não fazem parte deste incremento.
- Aguardar decisão humana sobre S110 conforme AGENTS.md. Nenhum comando em execução;
  reagendamento, resultado e novos guardrails de 4.1 continuam pendentes; 5.1 não iniciado.

## S110: RuntimeException autorizada — 2026-09-07

- Usuário decidiu explicitamente: “ContinuarAjustes com RuntimeException”.
  A decisão abrange o novo tipo técnico unchecked, substitui a proposta IOException e autoriza
  a retirada de `throws JsonProcessingException` da assinatura do mapper produtor.
- Decisão Sonar registrada com `-HumanDecision ContinuarAjustes` em
  `2026-09-07T14:59:49.8565705-03:00`; baseline original preservado.
  Antes da edição, baseline integral idêntico e fingerprint
  `d875348a293025c42004a97ddd0db29d754c213e386b972e4d3c50b773a56551` conferido.
- Alterações previstas somente no produtor: SerializacaoEntradaException, mapper e teste de log;
  na prova de compatibilidade existente, retirar `throws Exception` somente do teste que monta
  propriedades AMQP, para provar que um chamador não precisa declarar/capturar falha checked.
- RED comportamental antes de produção: testes exigem RuntimeException em vez de checked Jackson.
  Atualizar verificações específicas de metadata Jackson para o contrato local: causa nula,
  ausência de suppressed e mensagem sanitizada; manter tipo/id/código, log único, contexto,
  stack e sentinelas. Nenhuma captura genérica, descarte silencioso ou ação automática.
- GREEN: herança direta RuntimeException e assinatura sem checked; preservar todo corpo da
  exceção, catch específico do Jackson, helper/DTO de log e JSON/AMQP de sucesso.
- Executar regressão focada, revisão e checkpoint completo; atualizar consolidado arquitetural
  após a mudança implementada. Preservar Hub, baseline, extensão/configuração/emulador e demais
  arquivos. Não iniciar reagendamento/resultado/guardrails nesta correção nem avançar para 5.1.

## S110: RuntimeException implementada — 2026-09-07

- RED comportamental antes de produção: seis testes, três falhas esperadas porque a exceção
  local ainda era checked. Asserções exigiam RuntimeException; sucesso, trace e falha inesperada
  continuaram passando.
- GREEN: SerializacaoEntradaException herda diretamente RuntimeException; mensagem, campos,
  serialVersionUID e causa nula preservados. Mapper remove apenas a declaração checked,
  mantendo catch de JsonProcessingException, log único e propagação tipada.
- Testes do log agora exigem RuntimeException e preservam vínculo do id/código, erro JSON real,
  sentinelas, tipo/frames, contexto e emissão única. Verificações da API específica do Jackson
  foram substituídas por causa nula, suppressed vazio e mensagem/toString sem conteúdo sensível.
- O teste de propriedades AMQP compila e executa sem declarar throws, comprovando chamada
  sem obrigação checked. Nenhuma asserção desse teste foi alterada.
- Regressão focada: nove testes (seis de log e três de contrato/compatibilidade), todos aprovados,
  sem falhas/erros/ignorados. Revisão independente dos quatro arquivos não identificou bug ou risco
  material. Helper/DTO de log, consumidor e todos os fontes do Hub permanecem iguais.
- Checkpoint completo: 825 testes, zero falhas/erros/ignorados; build e análise concluídos.
  `NON_COMPLIANT`, decisão `PENDING`, em `2026-09-07T15:20:31.1038299-03:00`.
  214 issues, uma nova MINOR/LOW (S1130), nenhuma HIGH/BLOCKER/CRITICAL;
  cobertura 86,7%, duplicação 3,8%.
- API confirma S110 `ee7fec89-5053-4239-a761-01577316b071` como `CLOSED/FIXED`;
  S5961 `38b7346f-bcbe-48f1-9e67-1e13e1cb0e61` permanece `CLOSED/FIXED`.
- Issue nova `7d05a48f-c784-4383-90e1-fc166cb61d82`, `java:S1130`, MINOR/LOW,
  em `MonitoramentoEntradaServiceBusMapperTest.java:53`: o teste de compatibilidade ainda
  declara `throws Exception`, embora seu corpo não lance mais exceção checked.
  Essa declaração residual deveria ter sido removida junto da adaptação dos chamadores.
- Proposta de ajuste: retirar somente `throws Exception` de
  `deveSerCompativelComContratoIndependenteDoConsumidor()`, preservando corpo/asserções e todo
  código de produção. O teste que chama `json.readTree` continua com declaração checked necessária.
  Executar regressão focada e novo checkpoint, preservando o baseline, após decisão humana.
- Análise `ee9e08bd-a9a4-41d9-aa5f-11ec695af524`, CE
  `fce57622-04f5-4285-a08c-1f3c90d11470`, fingerprint
  `bf0ba694a95bc5060d35050d05417f3d4ad5b69f17e0a4292e8fc5819e080b6d`.
  Baseline integral idêntico e fingerprint atual conferido após o checkpoint.
- Inventário `%TEMP%/simtr-s110-runtime-audit-20260907.json`: 641 arquivos, nenhuma ausência,
  alterações executáveis restritas aos quatro arquivos previstos. Os 493 arquivos do Hub
  preservam os hashes; extensão, configuração e caminho do emulador permanecem iguais.
  `git diff --check` aprovado; plan/todo e consolidado atualizados. Sem commit ou troca de branch.
- Aguardar decisão humana sobre S1130 conforme AGENTS.md: `ContinuarAjustes`,
  `AceitarExcepcionalmente` ou `Reprovar`. O ajuste residual ainda não foi aplicado.
  Nenhum comando em execução; demais subfatias de 4.1 e 5.1 não iniciadas.

## Continuidade autorizada de S1130 — 2026-09-07

- Usuário decidiu `ContinuarAjustes`; decisão registrada em `2026-09-07T15:32:28.5995936-03:00`
  pelo comando `./validar-checkpoint-sonarqube.ps1 -HumanDecision ContinuarAjustes`.
- Baseline integralmente idêntico antes da edição e fingerprint conferido:
  `bf0ba694a95bc5060d35050d05417f3d4ad5b69f17e0a4292e8fc5819e080b6d`.
- Aplicar somente a remoção de `throws Exception` em
  `MonitoramentoEntradaServiceBusMapperTest.deveSerCompativelComContratoIndependenteDoConsumidor`.
  Preservar corpo, asserções, demais testes, produção e todo o workspace.
- Regressão focada de log/contrato/compatibilidade e novo checkpoint Sonar; sem novos testes
  ou mudança de comportamento, e sem reinicialização do baseline. Não avançar nas outras
  subfatias de 4.1 nem para 5.1.

## S1130 corrigida; entrada com log JSON verificada — 2026-09-07

- Removida somente a declaração `throws Exception` de
  `deveSerCompativelComContratoIndependenteDoConsumidor()`. Comparação integral com a cópia
  anterior confirmou que corpo, asserções e restante do arquivo estão idênticos.
- Regressão focada: nove testes (seis de log e três de contrato/compatibilidade), zero
  falhas/erros/ignorados. Revisão independente confirmou o diff estritamente autorizado.
- Checkpoint completo: 825 testes, zero falhas/erros/ignorados; build, scanner e Compute Engine
  concluídos. `COMPLIANT` em `2026-09-07T16:06:58.5792654-03:00`, decisão `NOT_REQUIRED`.
  213 issues, nenhuma nova, nenhuma HIGH/BLOCKER/CRITICAL; cobertura 86,7%, duplicação 3,8%.
- API local confirmou como `CLOSED/FIXED`:
  S1130 `7d05a48f-c784-4383-90e1-fc166cb61d82`,
  S110 `ee7fec89-5053-4239-a761-01577316b071` e
  S5961 `38b7346f-bcbe-48f1-9e67-1e13e1cb0e61`.
- Análise `7bfaf0af-f0df-404f-a570-9bd4965c1b84`, CE
  `2d7c4580-b09f-4e37-a7e2-f4b61a58179d`, fingerprint
  `ea979f13bf089415bf407f6ab9540cf2e1614d881f7aeda88c01d6ec1d9bd92f`.
  Baseline comparado integralmente: idêntico. Fingerprint atual coincide com o checkpoint.
- Inventário `%TEMP%/simtr-s1130-audit-20260907.json`: 641 arquivos, nenhuma ausência; antes
  desta atualização documental somente o teste autorizado tinha hash diferente. Comparação
  exata confirmou apenas a remoção prevista. Os 493 arquivos do Hub preservam seus hashes;
  produção, extensão, configuração e `src/main/azure/servicebus-emulator/config.json` inalterados.
- Logs da entrada em consumidor/produtor concluídos e verificados, com DTOs independentes,
  código estável, id de ocorrência, JSON tipado e diagnóstico sanitizado. O produtor mantém
  RuntimeException própria e propagação após o log. Sem ação automática ou persistência.
- Plan/todo atualizados; arquitetura consolidada já descreve a implementação RuntimeException.
  Sem commit, reset, clean, troca de branch ou comando em execução. Restam reagendamento,
  resultado e novos guardrails em 4.1; não houve avanço para 5.1 nem encerramento humano da feature.

## Referências da configuração implementada

- [Quarkus Config Mapping — mapa de definições](https://quarkus.io/guides/config-mappings/#maps)
- [Quarkus lifecycle — producer inicializado com Startup](https://quarkus.io/guides/lifecycle/#using-startup-to-initialize-a-cdi-bean-at-application-startup)

Padrões conferidos contra o runtime efetivo Quarkus `3.33.2.1`, com testes positivos e prova
negativa de bootstrap. Extensão Azure Service Bus `1.2.5`, simuladores e transporte permanecem
inalterados nesta subfatia.

## Regra de retomada

Depois do GO, executar somente o próximo item pendente. Mudança de escopo atualiza primeiro este
checklist e o plano. Nenhuma decisão pendente é inferida pelo agente.

## C4.4 — estrutura e guia entregues — 2026-09-07

- Direção humana atendida: antecipar componentes/classes no código e criar guia para os
  desenvolvedores antes da implementação efetiva. Não se repetiu GO já concedido.
- 40 arquivos de produção adicionados: 11 interfaces de portas, dois records independentes
  de parâmetros iniciais e 27 classes com `@Vetoed`, Javadoc e responsabilidade vinculada
  aos itens funcionais. Casos de uso/adapters ainda não implementam as interfaces; dependências
  indicadas por `@see` são previstas, sem injeção ou execução.
- Tipos vazios de resultado, consulta, decisão e reagendamento não têm campos/invariantes
  implementados. DTOs/mappers vazios permanecem pendentes em 4.1. Nenhum retorno fictício,
  endpoint, observer, publisher, client ou listener novo foi ativado.
- Prova inicial: corrigida uma expressão do teste para a API ArchUnit 1.4.2 instalada;
  em seguida, `EsqueletosMonitoramentoCdiTest` executou 27 casos com `ClassNotFoundException`
  esperada pela ausência das classes planejadas, antes de criar os arquivos de produção.
- Os oito grupos de até cinco arquivos de produção passaram por `mvn -q compile`.
  Uma tentativa inicial com property Maven sem aspas foi rejeitada pelo parsing do shell;
  o comando de compilação sem essa property foi executado com sucesso.
- GREEN: 27 casos CDI provam presença de `@Vetoed`, ausência de `@Path` e ausência do bean
  para as classes inventariadas. Sete testes ArchUnit protegem núcleo/borda, colaboração fora
  de ACL, infraestrutura, SDK e interfaces, com fixture negativa de SDK no núcleo e positiva
  de Quarkus/Mutiny/CDI.
- Regressão focada: 190 testes em oito classes, sem falhas/erros/ignorados. Incluiu novos testes,
  36 guardrails existentes do Hub, REST, contrato de entrada e logs das duas bordas.
- `./validar-checkpoint-sonarqube.ps1` concluiu Maven `clean verify`, scanner e Compute Engine:
  **859 testes, zero falhas, erros ou ignorados; build aprovado**.
- Checkpoint **COMPLIANT** em `2026-09-07T17:13:24.4462907-03:00`: 213 issues abertas,
  nenhuma nova, nenhuma HIGH/BLOCKER/CRITICAL, cobertura **86,4%**, duplicação **3,7%**;
  decisão humana `NOT_REQUIRED`, sem supressão, exceção ou novo baseline.
- Análise `43ed862c-5df3-4572-9e2c-587aa1db4a16`; CE
  `3b6df1c7-0cc6-478c-a520-2bd678e69e01`; fingerprint
  `553c8f4f746c06182649ac65ed15403bf6a611e09937aa807404e5b1dac89053`.
  Baseline integral comparado com a referência recuperada: idêntico; fingerprint conferido antes
  do complemento exclusivamente documental de Javadoc.
- [Guia](guia-desenvolvimento.md) com diagrama, inventário dos 40 arquivos, responsabilidades,
  limites, procedimento para completar/habilitar classes, verificações e pontos para discussão.
  [Manifesto](pacote-commit.md) propõe duas partes de revisão; nenhum staging ou commit.
- 153 links locais do guia/manifesto conferidos, nenhum ausente. `git diff --check` aprovado;
  somente avisos preexistentes de conversão LF/CRLF, sem normalização de arquivos.
- Inventário inicial de 641 arquivos comparado: nenhum removido. Somente plan.md, todo.md e
  consolidado arquitetural mudaram entre os arquivos anteriores; todo código/teste preexistente,
  Hub, extensão, simuladores e config do emulador conservam o hash inicial.
- Revisão de correção, simplicidade, arquitetura, segurança, desempenho e escopo: novas classes
  ficam inativas, contratos permanecem próprios, não há I/O ou lógica adicionada ao fluxo.
  Limites explícitos: faltam campos/mappers funcionais e provas completas das ACLs/DTOs; a
  associação entre reagendamento e settlement será detalhada antes de 8.1.
- Próximo passo humano: discutir a estrutura e revisar o pacote com os desenvolvedores. A próxima
  implementação funcional continua nas pendências de 4.1; **5.1 não iniciado**, sem encerramento
  humano da feature. Nenhum comando em execução ao registrar o resultado técnico.

## Complemento documental — Javadoc solicitado

- Usuário solicitou comentários Javadoc nas classes e métodos cuja lógica ainda falta implementar.
- Escopo registrado no plano: somente comentários dos 40 tipos estruturais, sem alterar contratos,
  assinaturas, anotações ou corpos; orientação ligada ao guia e aos itens funcionais.
- Verificação documental concluída: DocLint sem erros, referências resolvidas e comparação do
  conteúdo fora dos comentários preservada; detalhes abaixo.
- O checkpoint anterior descreve a estrutura validada antes deste complemento de documentação.

## Complemento Javadoc — verificação concluída

- 40 comentários de tipos complementados: 27 classes inativas, 11 portas e dois records.
  Os 11 métodos declarados das portas receberam Javadoc próprio, com `@param` e `@return`.
  Os quatro componentes dos dois records também possuem `@param`.
- Comentários descrevem estado pendente, responsabilidade, implementação/verificação previstas,
  dependências e item funcional. Referências usam `@see`/`{@link ...}` e caminhos do guia.
  Nenhum método, construtor ou `@throws` fictício foi introduzido.
- `javac -proc:none -Xdoclint:all` validou os 40 fontes com as dependências locais e saída
  temporária: zero erros; 27 avisos de construtores implícitos sem comentário, preservados por
  não serem declarações existentes no fonte. Nenhum HTML ou derivado foi gerado.
- A atualização aplicou guardas por arquivo e comparou conteúdo sem Javadoc antes/depois:
  assinaturas, imports, anotações, campos e corpos preservados. Quatro cópias temporárias
  dos comentários/fontes anteriores permitem conferir essa comparação.
- Guia atualizado com leitura/manutenção do Javadoc e exemplos navegáveis. Este recorte foi
  exclusivamente documental: não executou Maven, inspeção de token, baseline, API ou checkpoint Sonar.
- O checkpoint técnico de 859 testes permanece evidência do incremento estrutural anterior;
  o fingerprint dos arquivos-fonte muda com comentários e não foi declarado novamente conforme.
- Estrutura/guia continuam entregues para discussão com os desenvolvedores. Reagendamento,
  resultado e guardrails funcionais restantes continuam em 4.1; sem implementação de 5.1,
  commit, alteração do Hub ou descarte de arquivos.

## Retomada do plano — reagendamento em 4.1 (2026-09-07)

- [x] Registrar solicitação humana de continuidade após estrutura/guia/Javadoc, preservando escopo e GOs.
- [x] Conferir o baseline original e o último checkpoint; diferença de fingerprint documentada pelo complemento Javadoc.
- [x] RED/GREEN do DTO/mapper próprio de reagendamento, compatibilidade JSON/AMQP e validação.
- [x] RED/GREEN do log JSON da borda, RuntimeException própria, diagnóstico sanitizado e correlação.
- [x] Atualizar Javadoc e retirar apenas os dois tipos implementados do inventário de esqueletos.
- [x] Executar regressão focada e checkpoint completo, preservando baseline.
- [x] Atualizar guia, manifesto, arquitetura e evidências; manter resultado/guardrails pendentes em 4.1 e 5.1 não iniciado.

## Revisão do guia indicado pelo usuário (2026-09-07)

- [x] Identificar o alvo: `doc/guias/guia-service-bus-amqp-dossie.md`, ainda baseado no ADR-0009.
- [x] Confirmar decisões vigentes: duas filas, orquestrador/monitoramento, connection string/SAS, extensão e Dev Services; `dossie` e Hub preservados.
- [x] Preservar os três arquivos de teste novos de reagendamento em RED; nenhum GREEN/novo checkpoint concluído.
- [x] Reescrever o guia e alinhar referências, plano, checklist e ADRs inconsistentes.
- [x] Corrigir Javadocs incompletos e orientar a implementação do fluxo sem alterar lógica/assinaturas.
- [x] Verificar links, DocLint e equivalência fora dos comentários; registrar pendências reais e continuidade.

## Verificação da revisão documental

213 links em 12 documentos: nenhum ausente. Javadoc de 20 tipos, incluindo 11 métodos das portas:
DocLint exit 0, sem erros; 9 avisos somente dos construtores implícitos preservados. Comparação
confirmou código fora de Javadoc idêntico. Os 11 trechos `undefined` foram removidos.
Nenhum arquivo original perdido; apenas esses comentários mudaram nos fontes preexistentes.
`dossie`, Hub, configurações, simuladores e `src/main/azure/servicebus-emulator/config.json`
mantidos. `git diff --check` passou com avisos LF/CRLF preexistentes.

Revisão exclusivamente documental, sem Maven/Sonar e sem derivados HTML/PDF/PPT.
Os testes novos de reagendamento permanecem em RED de compilação por API ausente.
A evidência anterior de 859 testes não é uma aprovação do workspace atual.
Próximo incremento funcional: concluir essa subfatia de 4.1 com regressão e checkpoint,
preservando o baseline existente. Nenhum commit ou avanço para 5.1.

## Pausa ao final do dia — 2026-09-07

Usuário solicitou parar e retomar amanhã em situação segura. Branch e arquivos preservados,
sem commit, staging, reset, clean ou troca de branch. Comandos do agente encerrados e nenhum
agente auxiliar trabalhando. Criado roteiro de retomada com decisões, arquivos do RED, próxima
subfatia de 4.1 e referência ao baseline existente. Não executar trabalho funcional durante a pausa.

## Reagendamento em 4.1 — conclusão técnica em 2026-09-08

- Autorização humana: retomar somente 4.1, completando o mapper de reagendamento,
  preservando baseline, dossiê, Hub e configurações. Não houve avanço para resultado,
  guardrails novos ou 5.1; nenhum staging/commit.
- RED reproduzido antes da produção com
  `mvn -q "-Dtest=MonitoramentoReagendamentoContratoTest,MonitoramentoReagendamentoLogTest" test`:
  falha esperada de compilação por ausência de método, construtor e exceção.
- GREEN: os mesmos dois testes e a fixture foram preservados integralmente.
  37 casos de contrato e 6 de log passaram. JSON v1/envelope determinístico,
  limites inclusivos, obrigatoriedade, datas e compatibilidade real com o consumidor;
  validação antes da serialização, erro JSON tipado, diagnóstico sem payload/causa/processor
  e correlação somente com trace válido.
- Produção: DTO/mapper de saída implementados; adicionados `LogErroReagendamento`,
  `ErroReagendamentoDto` e `MapeamentoReagendamentoException extends RuntimeException`.
  Sem publicação, agendamento, incremento de tentativa ou recálculo de prazo/versão.
- Somente os dois tipos concluídos foram retirados de `EstruturaPlanejada`; 25 esqueletos
  continuam inativos. Regressão focada de 12 classes: **221 testes, 0 falhas/erros/ignorados**.
  Comando: `mvn -q "-Dtest=MonitoramentoReagendamentoContratoTest,MonitoramentoReagendamentoLogTest,MonitoramentoEntradaContratoTest,MonitoramentoEntradaServiceBusMapperTest,MonitoramentoEntradaLogTest,OrquestradorEntradaLogTest,FormatoLogJsonServiceBusTest,ConfiguracaoLogJsonServiceBusTest,ConsoleTextualServiceBusTest,EstruturaMonitoramentoArchUnitTest,EsqueletosMonitoramentoCdiTest,ArchUnitProgressivoTest" test`.
- Checkpoint `./validar-checkpoint-sonarqube.ps1` concluído em
  `2026-09-08T08:59:53.1413894-03:00`: **900 testes em 168 classes**, sem falhas, erros
  ou ignorados; build, scanner e Compute Engine concluídos.
  **COMPLIANT**, 213 issues abertas, 0 novas, 0 HIGH/BLOCKER/CRITICAL,
  cobertura **86,5%**, duplicação **3,6%**; decisão humana `NOT_REQUIRED`.
  CE `a304909a-5d6e-49ba-b02c-acb88f93becc`;
  análise `d59e50b3-116d-4473-9872-87094b8bee96`;
  fingerprint `f5c889b7c9abc4106be13e94c525e60f331d18a46961867de768db690d71e86e`.
  A diferença em relação aos 859 testes históricos é +43 casos novos e -2 entradas
  do teste parametrizado de inatividade, correspondentes aos tipos agora implementados.
- Baseline original de 217 issues restaurado da referência indicada, sem reinicialização.
  O estado inicial vazio foi copiado em
  `.codex/.state/session-before-reagendamento-recovery-20260908.json`.
  O baseline foi comparado integralmente; nenhum hook foi alterado. A evidência do checkpoint
  atual substitui a ausência operacional de checkpoint na abertura desta sessão.
- Revisão de correção, simplicidade, arquitetura, segurança, desempenho, testes e escopo:
  validação local antes de Jackson; captura específica de `JsonProcessingException`;
  sem dependência lateral entre DTOs, sem cliente/efeito remoto ou abstração transversal nova;
  tipo/frames sanitizados e classificação mantidos na própria borda conforme ADR-0012.
- Preservação por SHA-256: 599 arquivos preexistentes de `src/`, `pom.xml` e `AGENTS.md`
  permaneceram idênticos. Apenas os dois esqueletos de produção e o inventário mudaram;
  três arquivos próprios de produção foram adicionados. Nenhum arquivo removido.
  Isso inclui preservação dos testes novos, Hub, dossiê, configuração e emulador.
- Javadocs, guias, manifesto, arquitetura e retomada alinhados. Links locais verificados;
  `git diff --check` sem erros. Nenhum formato derivado gerado.
- **Esta subfatia está tecnicamente concluída.** 4.1 permanece aberto para contratos/mappers
  de resultado, logs correspondentes e guardrails restantes. O encerramento humano da feature
  não foi inferido.

## Continuidade de 4.1 — implementação e cobertura de resultado (2026-09-08)

Direção humana: "vamos fazer uma implementação vamos focar na implemntação e na cobertura
sem seguir exatamente o TDD". Para esta continuidade, implementar e verificar comportamento,
regressão e cobertura sem exigir a sequência RED anterior à produção. O histórico TDD das
subfatias concluídas permanece preservado.

Escopo: modelos próprios de resultado, DTOs/mappers produtor e consumidor independentes, JSON
v1 e envelope aprovados, logs locais tipados com diagnóstico seguro, testes reais de contrato,
cobertura e compatibilidade. Completar os guardrails previstos de isolamento por borda e acesso
das ACLs à API pública. Permanecer em 4.1, sem listeners, publicação, settlement ou 5.1.
Baseline original READY e checkpoint de 900 testes COMPLIANT conferidos; sem reinicialização.

Implementar em subfatias locais: modelos/DTOs; mapper/erro do produtor; mapper/erro do consumidor;
testes de contrato e logs; guardrails e regressão; checkpoint completo único do incremento.
A compatibilidade das duas bordas constitui a unidade de verificação; a orientação atual de
foco na implementação/cobertura substitui a ordem rígida de RED e o limite histórico de cinco
arquivos, sem autorizar compartilhamento de DTOs, mudança transversal ou ampliação funcional.

Contrato a confirmar: o fluxo aprovado verifica limites antes de consultar MTR; o texto v1 não
define nulidade de situacaoMtr nem contador zero na quarentena anterior à primeira consulta.
Proposta submetida: permitir null/zero nesse caso e exigir situação conclusiva e pelo menos
uma tentativa no resultado CONCLUSIVO. Não aplicar essa validação antes da resposta humana.
Os demais campos/tipos e propriedades seguem o contrato aprovado; motivo textual é preservado.
inputSequenceNumber conserva o long recebido, sem interpretar seus bits ou usá-lo como contador.
concluidoEm pode coincidir com iniciadoEm, mas não precedê-lo.

Arquivos: os dois modelos e quatro DTOs/mappers estruturais existentes; helpers, DTOs de erro e
exceções próprios de cada borda; testes espelhados, inventário de esqueletos e guardrails com
fixtures negativas/positivas. Atualização documental restrita ao estado e às evidências.

Verificações: ObjectMapper e Validator reais no Quarkus; JSON exato e envelope determinístico;
campos ausentes/nulos/tipos inválidos/overflow/datas; preservação de IDs e zeros; compatibilidade,
campos desconhecidos e rejeição de conteúdo extra; um erro sanitizado por falha reconhecida,
id/código propagados, trace válido e erro inesperado não reclassificado. Regra nova de arquitetura
deve rejeitar fixtures inválidas e aceitar as portas/modelos públicos e o framework já permitido.

## Ajuste Sonar do resultado — autorizado em 2026-09-08

O incremento de resultado e guardrails passou em 317 testes focados e no checkpoint completo
com 1021 testes em 171 classes, sem falhas, erros ou ignorados. Cobertura 87,0%, duplicação 4,4%.
O checkpoint de 2026-09-08T10:10:55.4993743-03:00 ficou NON_COMPLIANT: 215 issues abertas,
duas novas CRITICAL/HIGH de java:S1192 no mapper consumidor de resultado. São os literais
"paraResultado" (três ocorrências) e "validarTipos" (cinco ocorrências), sem falha funcional.
Issues: 1dfde59a-ce66-490f-97d7-7f227d974d46 e 1f4b5927-0402-44d5-b3fd-283dd75c5b7e.
Análise: ddc71af5-f271-48a7-8512-2ee6f7c61c7e; CE: c3b09788-5b67-4f89-b732-fa1d871fff40.
Fingerprint: f78cac3cee1c3f3ca65b857e9be8be403d14a3733809b2eed42b7facd267e975.

Direção humana: "vamos resolver os problemas sonar". Decisão ContinuarAjustes registrada
pelo script do checkpoint. Escopo do ajuste: extrair duas constantes no mapper consumidor,
preservar os textos emitidos e os testes existentes, executar regressão focada e novo checkpoint
com o baseline original. Sem aceitação excepcional, reinicialização de baseline ou avanço para 5.1.
A definição de nulidade/contador da quarentena anterior à primeira consulta MTR continua
aguardando resposta à pergunta de contrato; esta autorização trata das duas issues Sonar.

## Resultado e guardrails — evidência final do ajuste Sonar (2026-09-08)

- Modelos/DTOs independentes e mappers das duas bordas de resultado implementados, com JSON
  v1, envelope determinístico, validação estrutural/temporal e erros locais tipados e sanitizados.
  Sem envio, listener, settlement ou persistência. Validação especial da quarentena pendente.
- 98 testes de contrato de resultado, 11 de logs reais e 18 de fronteiras ArchUnit aprovados.
  Guardrails exercitam DTOs por borda, domínio/portas e acesso das ACLs à API pública, com
  fixtures positivas e negativas. Seis tipos implementados retirados do inventário: restam 19.
- Regressão após S1192: 317 testes em 11 classes, zero falhas/erros/ignorados.
  Comando: `mvn -q "-Dtest=ResultadoMonitoramentoContratoTest,ResultadoServiceBusLogTest,FronteirasMonitoramentoArchUnitTest,EstruturaMonitoramentoArchUnitTest,EsqueletosMonitoramentoCdiTest,ArchUnitProgressivoTest,MonitoramentoReagendamentoContratoTest,MonitoramentoReagendamentoLogTest,MonitoramentoEntradaContratoTest,MonitoramentoEntradaLogTest,OrquestradorEntradaLogTest" test`.
- O ajuste de produção extraiu somente OPERACAO_MAPEAMENTO e OPERACAO_VALIDACAO_TIPOS no
  mapper consumidor de resultado. Textos dos logs e comportamento preservados; comparação
  reversa da extração idêntica ao arquivo anterior. Nenhum teste foi alterado para esse ajuste.
- `./validar-checkpoint-sonarqube.ps1` concluiu build, scanner e Compute Engine em
  `2026-09-08T10:30:53.8267228-03:00`: **1021 testes em 171 classes**, sem falhas/erros/ignorados.
  **COMPLIANT**, decisão técnica `NOT_REQUIRED`, 213 issues (baseline 217), nenhuma nova
  ou HIGH/BLOCKER/CRITICAL, cobertura **87,0%**, duplicação **4,4%**.
- API local confirmou ambas as issues S1192 como CLOSED/FIXED, sem supressão ou exceção.
  CE: `22a6d6fe-92da-4f60-87cf-c1a4f2807310`; análise: `c86b89ae-ac8d-49d2-b540-439d06446b38`.
  Fingerprint: `13df928607bf0c276fecfbba4395ada0195fbc41f0ecf06c47fd6e4095f389d9`, idêntico ao código atual.
- JaCoCo: 157/157 linhas cobertas nos dois mappers, dois helpers de log e duas exceções de
  resultado. Essa cobertura não conclui a definição contratual de quarentena ainda pendente.
- Baseline comparado integralmente com a referência original, sem reinicialização.
  Auditoria SHA-256 do incremento de resultado: 598 arquivos preexistentes preservados,
  seis tipos de produção e o inventário alterados, seis novos arquivos de produção e nove
  de testes/fixtures, nenhum removido. Inclui preservação de Hub, dossiê, reagendamento,
  configurações, pom.xml e AGENTS.md no incremento.
- Revisão final: correção, simplicidade, arquitetura, segurança, desempenho, testes e escopo
  conferidos; extração local sem nova dependência ou efeito externo. Documentação de continuidade
  alinhada. Nenhum staging, commit, avanço para 5.1 ou encerramento humano da feature.


## Acompanhamento de duplicação — 2026-09-08

O usuário perguntou se a duplicação exige preocupação. O checkpoint atual mede 4,4%, abaixo
do limite de 5%, com margem de 0,6 ponto percentual; o incremento anterior media 3,6%.
Consulta aos blocos reais do Sonar local: os dois LogErroResultado têm 62 e 61 linhas marcadas
como duplicadas, incluindo montagem/emissão do JSON e diagnóstico; as duas
MapeamentoResultadoException têm 22 linhas cada. Há também blocos nos helpers anteriores.
Os DTOs novos não aparecem entre os arquivos com duplicação positiva nessa consulta.

Risco registrado: repetição técnica pode aumentar custo de manutenção e consumir a margem.
Priorizar avaliação da montagem/emissão técnica dos logs, preservando DTOs, exceções,
classificação e sanitização locais conforme ADR-0012. Uma extração transversal exige proposta
concreta e checkpoint arquitetural antes da implementação. Esta avaliação não alterou código,
exclusões Sonar, limiar, baseline ou Hub e não representa autorização para mudar a arquitetura.

## Preparação da continuidade de 4.1 — 2026-09-08

Usuário: "ok prosseguimos". Conferidos o estado atual, fluxo aprovado, DTOs/mappers, teste de
contrato, logs e limites do ADR-0012. A proposta de nulidade/contador da quarentena foi apresentada
novamente para resposta; não houve decisão inferida. A [matriz de validação preparada](preparacao-validacao-resultado.md)
identifica os casos e arquivos do próximo incremento. A avaliação da duplicação confirmou que
centralizar emissão exige proposta e checkpoint arquitetural; não houve extração transversal.

Neste recorte preparatório foram alterados somente documentos da feature. Código, testes,
baseline e último checkpoint permanecem preservados; sem Maven, API Sonar, commit ou 5.1.

## Validação final de resultado — confirmação humana em 2026-09-08

Usuário respondeu "confirmo" à proposta: QUARENTENA com situacaoMtr=null e zero tentativas
quando ainda não houve consulta; CONCLUSIVO com situação conclusiva e pelo menos uma tentativa.
Essa confirmação autoriza a validação local nas duas bordas de resultado em 4.1.

Implementação: manter DTOs independentes, rejeitar contador negativo e validar que CONCLUSIVO
possui tentativasRealizadas >= 1 e situacaoMtr em CONFORME, NAO_CONFORME ou PENDENTE_INFORMACAO.
QUARENTENA aceita null/zero e preserva o contador não negativo recebido; ausência de situação
não permite reconstruir consultas anteriores. Não recalcular situações, motivo ou contador.
A validação auxiliar não deve acrescentar propriedade ao JSON v1.

Verificações: produtor e consumidor reais, JSON exato com situação nula na quarentena,
rejeição de conclusivo sem situação/consulta, três situações conclusivas no limite mínimo,
contador negativo e compatibilidade dos limites numéricos; log real sanitizado da nova rejeição,
regressão e checkpoint completo. Preservar formato de erros, configurações, Hub e baseline.
A implementação segue o foco autorizado em cobertura sem ordem rígida de TDD.
O item 4.1 será marcado tecnicamente concluído somente após essas verificações; 5.1 não iniciado.
Duplicação permanece acompanhada; esta confirmação não altera a responsabilidade de logging.

## Conclusão técnica de 4.1 — validação de resultado em 2026-09-08

- Direção humana: "confirmo" à regra apresentada de quarentena null/zero antes de consulta
  e CONCLUSIVO com situação conclusiva e pelo menos uma tentativa. Confirmação registrada
  antes da produção; não foi inferida da mensagem anterior "ok prosseguimos".
- Os dois DTOs próprios agora rejeitam contador negativo e validam a evidência de consulta
  em CONCLUSIVO: CONFORME, NAO_CONFORME ou PENDENTE_INFORMACAO e contador >= 1.
  QUARENTENA preserva situação MTR nula e contador não negativo recebido; ausência de estado
  não é preenchida com consulta, valor inventado ou contador da sequência técnica.
- Jakarta Validation executa a condição local; JsonIgnore impede propriedade auxiliar no JSON.
  Mappers, helpers de log, exceções e contratos de outras bordas permaneceram idênticos.
  Não houve extração transversal ou mudança de logging para reduzir artificialmente duplicação.
- 25 casos novos: quarentena null/zero e limites, ausência de situação no consumidor, contador
  negativo nas duas bordas, conclusivo sem situação/consulta, tipos inválidos e logs reais.
  Os três estados conclusivos foram verificados no mínimo de uma tentativa.
  ResultadoMonitoramentoContratoTest: 120 casos; ResultadoServiceBusLogTest: 14 casos.
- Regressão focada: **342 testes em 11 classes**, zero falhas/erros/ignorados. Comando:
  `mvn -q "-Dtest=ResultadoMonitoramentoContratoTest,ResultadoServiceBusLogTest,FronteirasMonitoramentoArchUnitTest,EstruturaMonitoramentoArchUnitTest,EsqueletosMonitoramentoCdiTest,ArchUnitProgressivoTest,MonitoramentoReagendamentoContratoTest,MonitoramentoReagendamentoLogTest,MonitoramentoEntradaContratoTest,MonitoramentoEntradaLogTest,OrquestradorEntradaLogTest" test`.
- `./validar-checkpoint-sonarqube.ps1` concluiu build, scanner e Compute Engine em
  `2026-09-08T11:12:19.3296159-03:00`: **1046 testes em 171 classes**, sem falhas, erros ou ignorados.
  **COMPLIANT**, decisão `NOT_REQUIRED`, 213 issues (baseline 217), nenhuma nova/severa,
  cobertura **87,0%**, duplicação **4,4%**. A duplicação não aumentou.
  CE `c189234e-e9d0-4a30-a4e1-38f845c48484`; análise `f3b6720c-0b18-4713-8a16-22a030278153`.
  Fingerprint `13c39b71061152a7498dd7c83022afee3342e8eea8fe4f8fa6bbbe936bcf86fe`, conferido com o código atual.
- JaCoCo da nova validação: 4/4 linhas e 6/6 condições em cada DTO, sem trechos descobertos.
  Comparação de SHA-256: somente dois DTOs e três testes/fixture alterados; 615 arquivos
  preexistentes preservados, nenhum adicionado ou removido em src/. Baseline integralmente
  idêntico à referência original; token permaneceu somente em memória.
- Revisão de correção, simplicidade, arquitetura, segurança, desempenho, testes e escopo:
  condição pura e local, sem nova dependência, dados transportados ou operação externa;
  JSON exato, diagnóstico sanitizado e fronteiras arquiteturais preservados.
- Plano/checklist, retomada, guias, manifesto, preparação e consolidado alinhados.
  **4.1 está tecnicamente concluído**, com seus contratos e guardrails. 5.1 permanece não iniciado;
  nenhum staging, commit ou encerramento humano da feature.

Referências de validação consultadas e confirmadas no runtime efetivo:
[Quarkus Validation](https://quarkus.io/guides/validation/) e
[Jakarta AssertTrue](https://jakarta.ee/specifications/bean-validation/3.1/apidocs/jakarta/validation/constraints/asserttrue).


## Complemento do roteiro para continuidade por desenvolvedores — 2026-09-08

- [x] Corrigir pendências antigas de 4.1 no guia de desenvolvimento e identificar notas históricas.
- [x] Explicitar no guia principal como começar 5.1, com arquivos e critérios já planejados.
- [x] Verificar referências e diff documental do complemento.

Conferidos 96 links locais dos dois guias, sem destino ausente. Portas/Javadocs de 5.1,
validação dos DTOs de resultado e testes de fronteira confirmados no código. O diff contém
somente os dois guias e plan/todo; `git diff --check` sem erros. Não houve alteração executável,
Maven, baseline ou Sonar. O complemento está preparado para commit e push na mesma branch,
conforme autorização da revisão documental; a publicação será identificável no histórico Git.
4.1 continua concluído tecnicamente e 5.1 não foi iniciado.

## Preparação de commit e push para revisão com desenvolvedores — 2026-09-08

O usuário autorizou organizar commit e push na branch atual, depois de revisar o guia para
explicitar arquitetura, decisões adotadas e o que está pronto ou pendente. A revisão altera
somente Markdown; o código permanece no estado verificado por 1046 testes e checkpoint
COMPLIANT (87,0% de cobertura e 4,4% de duplicação). Nenhum novo incremento funcional.

Escopo: guia Service Bus como leitura principal, guia de desenvolvimento como inventário,
manifesto do pacote e notas de continuidade. Explicar no próprio guia as decisões aceitas dos
ADRs 0001/0003/0004/0006/0010/0011/0012 e as definições de contrato/política das tasks, além
de manter seus links. Corrigir referências desatualizadas à fila de saída ainda pendente.

Revisar e incluir os fontes/configurações/testes existentes da feature, ADRs, documentação e
tasks relacionadas. A orientação Azure de AGENTS.md e o documento de origem serão incluídos
após revisão: o primeiro orienta a continuidade e o segundo é destino de links dos guias.
O patch temporário .codex-doc-alignment.patch permanece local. Não publicar estado de sessão,
credenciais, relatórios Sonar, saídas de build ou formatos derivados. Fazer push normal para
origin na mesma branch, sem force, merge ou encerramento da feature.

## Revisão do guia e pacote para desenvolvedores — 2026-09-08

O guia principal passou a abrir com o que está pronto e explicitar arquitetura do mesmo runtime,
responsabilidade de cada componente, hexagonal pragmática, contratos independentes, ACLs, factory,
SAS/Dev Services, logging tipado e telemetria planejada. A seção desatualizada da fila de saída
foi substituída pelo contrato implementado, JSON v1 e regras confirmadas. Adicionado roteiro 5.1–CF.

Os nomes de telemetria planejados com prefixo doctree foram preservados como contrato histórico,
com nota explícita de que a revisão dos packages não os renomeou. Essa diferença está documentada
para caracterização/checkpoint da Task 10; nenhum sinal ou código foi alterado nesta revisão.

Guia de desenvolvimento e manifesto foram alinhados. A seleção agora inclui AGENTS.md e o
documento amplo de origem após revisão, preservando as referências para os devs. O patch
temporário continua local. O usuário já autorizou commit e push nesta branch; não repetir
pedido de autorização para essas duas operações.

## Verificação do pacote antes do commit — 2026-09-08

Revisados escopo, comportamento já entregue, fronteiras, diagnóstico seguro, configuração,
testes e estruturas futuras. Os 620 arquivos conferidos de src/, pom.xml e AGENTS.md ficaram
idênticos durante esta revisão documental; a evidência executável anterior permanece aplicável.

Verificados 17 documentos e 146 referências locais, sem destino ausente; os dois exemplos JSON
do guia são sintaticamente válidos e os dois diagramas distinguem arquitetura e fluxo planejado.
Conferidos os valores contra DTOs, política/configuração, factory inativa e infraestrutura de logs.
A seleção contém 125 arquivos; a busca por formatos de credenciais privadas/Sonar/GitHub/SAS
não encontrou candidatos, sem expor valores. O diff documental passou após ajuste de linha final.

A branch remota ainda não existia na consulta de origem. Publicação autorizada: commit único
do estado acumulado até 4.1 e push com upstream para a mesma branch. Nenhum merge ou force.
O único arquivo de trabalho preservado fora do pacote é .codex-doc-alignment.patch.
