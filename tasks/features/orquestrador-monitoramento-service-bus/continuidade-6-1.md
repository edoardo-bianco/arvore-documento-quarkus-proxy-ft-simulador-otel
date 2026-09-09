# Continuidade 6.1 — publicação inicial e escolha do ambiente

## GO e orientação humana

O usuário respondeu “go” após a conclusão de 5.1, cujo próximo item declarado era 6.1.
Antes da produção, confirmou: testes unitários não devem usar emulador; no desenvolvimento
local, o dev escolhe entre emulador e filas configuradas no Azure. Esta orientação atualiza
o escopo de testes/build de 6.1 e prevalece sobre a execução automática anterior do emulador.

## Intenção e decisões existentes

Concluir somente 6.1: POST /simtr-hub/v1/monitoramentos-dossie -> porta de iniciação ->
caso de uso -> porta de publicação -> adapter/sender da entrada. Responder 202 somente
após confirmação. Reutilizar DTOs/mappers v1 e política já implementados.
ADRs 0003/0004/0010/0011/0012 preservados. Hub e dossie não serão alterados.

A preparação local recebe iniciadoEm, calcula limite/versão no monitoramento e traduz pela
ACL para o orquestrador. IDs UUID e instante inicial são gerados no servidor; tentativa inicial 1.
Somente a fábrica técnica configura o builder da extensão 1.2.5, cria os clientes duradouros
das duas filas com qualifiers e fecha os clientes após as assinaturas dos futuros listeners.
Receptores PEEK_LOCK com auto-complete desabilitado; 6.1 não inicia consumo.

## Isolamento dos testes e ambiente local

- Maven test padrão executa testes unitários/contratos locais sem emulador nem fila real.
  Manter quarkus.devservices.enabled=false e quarkus.azure.servicebus.enabled=false nos
  testes comuns. Mocks/stubs verificam confirmação, falha, transporte e lifecycle.
- Marcar integração com broker com @Tag("servicebus-integration"). Profile Maven
  servicebus-integration seleciona explicitamente somente esse grupo. Não excluir produção
  da cobertura, alterar limiares ou introduzir dependências. Adaptar o teste existente.
- Testes de integração com emulador rejeitam configuração externa para não acessar Azure
  por acidente. A execução explícita continua necessária para comprovar REST -> fila.
- Dev com emulador: profile dev, sem connection string/namespace externo; Dev Services
  fornece o emulador e sua configuração atual.
- Dev com filas Azure: profiles dev,azure, connection string SAS externa e nomes das filas;
  Dev Services desabilitado e AMQP sobre WebSockets, conforme configuração já aprovada.
  Não gravar credenciais nem acrescentar fallback de transporte/autenticação.
- Os componentes que dependem do broker acompanham a flag existente da extensão; não
  inventar uma flag adicional nem ativar conexão nos testes comuns.

## Subfatias e arquivos prováveis

1. 6.1a — pom.xml e ServiceBusDevServicesTest: separar execução padrão e integração explícita.
2. 6.1b — PrepararMonitoramentoUseCase, ParametrosMonitoramentoAcl, testes de ambas e
   EstruturaPlanejada (até cinco arquivos). Verificar política/instante, tradução e falhas.
3. 6.1c — ClientesServiceBus, qualifiers entrada/saída, testes e inventário. Construção única,
   transporte por profile, fechamento completo/idempotente e falha parcial sem vazamento.
4. 6.1d — MonitoramentoEntradaPublisher, testes, tratamento de falha próprio e inventário.
   Mapper atual, envio assíncrono único, confirmação antes da conclusão, sem retry novo.
5. 6.1e — IniciarMonitoramentoUseCase, testes e inventário. Ordem, IDs/instantes estáveis,
   preservação dos dados e falha sem falso sucesso. Sem SDK no núcleo.
6. 6.1f — MonitoramentoDossieResource, erro REST, testes e inventário. Validação/JSON/202
   já aprovados, erro REST compatível e seguro. Não importar implementações/erros do Hub.
7. 6.1g — testes explícitos de emulador, guardrails finais, Javadocs/portas e documentação.

Cada subfatia é verificada antes da seguinte. Implementação e cobertura seguem a orientação
humana sem exigir uma ordem rígida de TDD. Checkpoint Sonar no incremento coerente.

## Critérios de aceitação e verificações

- Testes padrão passam sem iniciar Dev Services ou usar um broker.
- Preparação usa política CDI existente e preserva limite/versão na ACL.
- Clientes corretos por qualifier, identidade estável e lifecycle gerenciado.
- REST válido publica exatamente uma mensagem v1 na entrada; 202 só após confirmação.
- Validação e falha preservam o padrão REST, sem broker, namespace, credencial ou payload.
- ArchUnit: builder exclusivo da fábrica; núcleo sem SDK/infra; ACL só usa API pública.
- Integração explícita comprova mensagem/envelope da solicitação REST no emulador.
- Sonar: nenhuma nova issue/severa, cobertura >=85%, duplicação <=5%. Baseline preservado;
  NON_COMPLIANT exige decisão humana conforme AGENTS.md.
- Atualizar guias, checklist, retomada e consolidado; não gerar derivados.

## Riscos e limites

O teste ServiceBusDevServicesTest executava automaticamente no Maven test; essa divergência
foi corrigida pela orientação humana acima. A ativação da fábrica precisa respeitar a extensão
desabilitada nos testes. Falhas do broker não podem chegar ao mapper genérico com causa sensível.
Preservar os logs existentes e definir o tratamento local necessário antes da borda correspondente.
O contrato de erro REST compartilhado está atualmente em hub.arquitetura.excecao.dto; os guardrails
dos componentes proíbem importar o Hub fora da ACL. Manter o formato por DTO próprio da borda
REST, sem mover o tipo existente nem relaxar a fronteira.

Fora de escopo: listeners funcionais, consulta/classificação terminal (7.1), reagendamento e
settlement (8.1), consumo do resultado (9.1), nova telemetria manual (10.1), persistência e deploy.
C2 será apresentado com a evidência desta fatia; somente o usuário registra aceitação/encerramento.

## Baseline antes da edição executável

READY / LOCAL_SONAR, integralmente idêntico à referência original; 217 issues.
Último checkpoint 5.1: COMPLIANT, análise 76257c69-5787-46cd-95e9-0d01c2549c0c,
1113 testes, cobertura 87,2%, duplicação 4,4%. Token presente somente em memória.
Não executar InitializeBaseline.

## Referências verificadas

- https://docs.quarkiverse.io/quarkus-azure-services/dev/index.html
- https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html
- https://quarkus.io/guides/cdi-reference/
- https://maven.apache.org/surefire/maven-surefire-plugin/examples/junit-platform.html#filtering-by-tags

Confirmar a API nos artefatos efetivos: Quarkus 3.33.2.1, Azure Services 1.2.5,
Service Bus 7.17.12 e Surefire 3.5.3; não atualizar versões nesta fatia.

## Detalhes da borda REST e ativação

Resource/caso de uso permanecem disponíveis com a extensão desabilitada. O publisher injeta
Instance do cliente qualificado e resolve o singleton apenas ao publicar; indisponibilidade
falha explicitamente. Somente a fábrica acompanha IfBuildProperty da extensão.
Erros na iniciação retornam 500 no formato REST existente (ARVDOCP9999 e mensagem genérica),
por DTO próprio da borda, sem importar Hub nem expor causa. A validação continua no mapper
global existente com 400/ARVDOCP0001. Não acrescentar log manual duplicado ou span nesta etapa.
O Uni de cada iniciação/publicação compartilha o resultado entre assinantes, preservando IDs
sem reenviar na mesma invocação; isso não representa idempotência durável entre requisições.

## Ajuste Sonar autorizado — S1710

O checkpoint de 2026-09-08T18:12:47.3532624-03:00 concluiu 1155 testes padrão sem falhas,
com cobertura 87,4%, duplicação 4,3% e uma issue nova LOW/MINOR java:S1710.
O usuário decidiu explicitamente ContinuarAjustes; decisão registrada pelo script.
Ajuste restrito ao Resource: remover import/agrupador APIResponses e manter os três
APIResponse com os mesmos códigos, descrições e schemas. Sem alteração do contrato ou testes.
Verificar regressão REST e checkpoint completo com o baseline original preservado.

## Estado final de 6.1

Implementação e verificações concluídas, inclusive o ajuste autorizado de S1710.
Checkpoint COMPLIANT, 1155 testes padrão sem broker, cobertura 87,4% e duplicação 4,3%.
Quatro testes de integração explícita com emulador passaram separadamente.
Ver [evidência final](todo.md#61--conclusão-técnica-e-ajuste-sonar-em-2026-09-08).
C2 permanece pendente de decisão humana; 7.1 não foi iniciado.
Não houve novo commit/push. Baseline e capacidades existentes preservados.

## Correção posterior C2-R1 — 2026-09-09

O profile de integração fixa `test` e reconstrói as fontes de configuração do Quarkus
antes de cada bootstrap. Rejeita connection string/namespace efetivos, inclusive por arquivo
ou `%test`, sem expandir expressões; falha de leitura produz mensagem fixa sem causa externa.
A suíte padrão continua sem broker e a escolha emulador/Azure no desenvolvimento permanece igual.

11 regressões sem broker e quatro integrações explícitas passaram. Checkpoint COMPLIANT:
1.166 testes padrão, cobertura 87,4%, duplicação 4,3%; baseline original preservado.
A [revisão C2](revisao-c2.md) registra a correção e os limites. C2 aguarda aceite humano;
7.1 permanece não iniciado.
