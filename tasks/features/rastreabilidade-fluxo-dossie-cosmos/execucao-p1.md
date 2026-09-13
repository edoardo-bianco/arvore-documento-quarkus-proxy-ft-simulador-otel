# P1: compatibilidade Cosmos e Dev Services

## Autorização e estado inicial — 2026-09-12

CP-COSMOS humano: "GO para o desenho e implementação incremental", reiterado como
"GO ao ADR-0013". ADR-0013 Aceito. Executar P1 antes do modelo/persistência operacional.
Branch feature/orquestrador-monitoramento-service-bus, HEAD 1f32466; preservar alterações
locais, sem staging, commit ou push.

Baseline READY/LOCAL_SONAR original preservado, sem pacotes sonar/. Fingerprint inicial
c6cb108f9ab162e4154e140c3cb4f2229193be14e84f1ccb8c67d1d5f17da488 igual ao checkpoint
COMPLIANT de 2026-09-12T15:31:47.8342667-03:00. Nenhuma reinicialização de baseline.

## Recorte

Até quatro arquivos executáveis: pom.xml, application.properties principal/teste e
CosmosDevServicesTest com perfil opt-in próprio. Adicionar a extensão pelo BOM 1.2.5,
mantendo Quarkus 3.33.2.1/JDK25; padrão sem rede/Docker, local por Dev Services, DES
configurável por variáveis externas. O bootstrap e os documentos sintéticos deste gate
pertencem ao teste; o bootstrap operacional integra o adapter posterior.

Primeiro provar ausência da extensão em RED; depois validar cliente CDI, endpoint local,
gateway, DB simtr-hub/container doctree e partição /idDossiePreValidacao. Ampliar a prova
para isolamento de partição, corpo exato, ETag obsoleto e batch atômico, inclusive rollback.
Recriar handle do banco confirma leitura independente do objeto Java anterior; não afirma
reinício de aplicação nem durabilidade após remoção do emulador. Não fechar cliente injetado
sem confirmar ownership.

Caracterizar lifecycle pela implementação efetiva da extensão e registrar limitações.
Nenhum envio Service Bus, acesso Cosmos externo, endpoint REST ou regra de negócio novo.
P1 não implementa rejeição operacional de TLS inseguro nem persistência do fluxo: essas
verificações pertencem à configuração/adapter antes de sua ativação em DES.

## Verificações

- [x] RED de disponibilidade: 1 falha esperada por ClassNotFoundException do producer, sem erro de compilação; GREEN depois da dependência: 1 teste aprovado.
- [x] GREEN local: bootstrap idempotente, CRUD, chave Pré-Valida original, corpo exato e 404 na partição incorreta.
- [x] Quatro testes finais aprovados: ETag 412 isolado, batch confirmado e rollback integral por 409/412 (3,387 s após bootstrap).
- [x] Dependências efetivas, lifecycle e imagem caracterizados; limitações abaixo.
- [ ] Testes padrão sem Docker; regressão Service Bus se dependências comuns mudarem.
- [ ] Revisão, build/checkpoint Sonar e atualização da arquitetura/retomada.

Fontes: guia Quarkus Azure Services e Cosmos, producer e DevServicesCosmosProcessor
na tag 1.2.5, referenciados no plano e ADR-0013. A guia atual e as APIs devem ser
confrontadas com os artefatos efetivamente resolvidos; não assumir equivalência de versões.
## Evidência da extensão, imagem e isolamento

Stack efetivamente resolvido: Quarkus 3.33.2.1/JDK25, extensão 1.2.5, Azure Cosmos SDK
4.73.1, Azure Core 1.55.5, Reactor Core 3.4.41, Reactor Netty 1.0.48 e Netty 4.1.135.Final.
O SDK 4.81.0 também existia no cache local, mas não foi o selecionado pelo Maven. Não
confundir artefato disponível com versão executada. Árvore preservada em
.codex/.state/cosmos-p1-dependency-tree.txt; nenhuma versão foi elevada silenciosamente.

O producer do artefato 1.2.5 tem escopo Dependent, comprovado pelo BeanManager durante o
teste, e não oferece disposer. O gate usa uma instância exclusiva da classe de teste e
fecha depois de todos os testes; logs confirmam encerramento do cliente. O adapter futuro
deve possuir uma única instância durável e fechar apenas seu recurso exclusivo, sem
fechar bean compartilhado de outro componente. Não há injeção async/builder inventada.

Dev Services iniciou HTTPS/gateway em porta local dinâmica e criou a imagem:

- mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:vnext-preview.
- Digest sha256:2db1f9e74c506bcf6fc347aa937aea1c00fa756061296a5a9efba530ce86ec02.
- Criação da imagem: 2026-09-08T18:45:16.657741934Z.

O teste confirmou rollback integral com evento anterior à operação que falha, incluindo
ETag obsoleto dentro do batch. Relatos anteriores no repositório oficial do emulador não
foram reproduzidos nesses casos nesta imagem; isso não equivale a prova de todas as
condições concorrentes nem do Cosmos gerenciado. Testes concorrentes do adapter ficam em P3.

A primeira prova Cosmos iniciou também SQL/Service Bus porque habilitar Dev Services global
não desabilita os demais processors. O perfil foi corrigido para desabilitar explicitamente
quarkus.azure.servicebus.devservices.enabled. Nenhum cliente operacional foi alterado.

Prova negativa com endpoint fictício https://cosmos-invalid.example: Maven exit 1 e mensagem
fixa esperada de recusa de endpoint/chave externos, antes de iniciar DocumentClient ou
emulador. O wrapper da verificação retornou 0 somente após conferir esses três fatos.
Não houve acesso ao ambiente DES. O teste também recusa chave externa pela mesma guarda.

Logs das provas em .codex/.state/cosmos-p1-{red,extensao-green,bootstrap,batch,endpoint-negativo}.log.
Revisão independente somente leitura: nenhum achado Critical/Required nos quatro arquivos.
Regressão conjunta Cosmos/Service Bus em execução; checkpoint final ainda pendente.
## Complemento de regressão descoberto no checkpoint

O primeiro checkpoint P1 executou 1.403 testes padrão com uma falha, antes do scanner:
MonitoramentoDossieResourceTest.deveRejeitarJsonMalformadoAntesDaPorta recebeu HTTP 400,
mas verificarSpanHttp capturou 0 spans em vez de 1. O helper fazia forceFlush seguido de
leitura imediata: forceFlush não aguarda o encerramento de spans ainda abertos. O retorno
HTTP e o callback que encerra SERVER podem ocorrer em momentos distintos.

Recorte complementar registrado antes da edição: quinto arquivo executável,
MonitoramentoDossieResourceTest. Aguardar o span do trace esperado com janela de consulta de 3 segundos (cada forceFlush conserva seu limite existente de 10 segundos),
com consulta periódica do exporter; manter inventário completo e todas as asserções de
quantidade/nome/parent/atributos, sem filtrar spans inesperados nem repetir requisição.
O cenário de limite MTR também deve aguardar/verificar seu SERVER antes do próximo reset.
Nenhuma alteração de produção, timeout/retry operacional, dependência ou contrato HTTP.

RED é a falha real 0/1 acima. Verificar a correção na classe completa e em repetições do
cenário JSON, restaurando a anotação permanente antes do checkpoint. Esse complemento
não muda o resultado dos 45 testes opt-in já aprovados; exige novo checkpoint padrão.
Fonte: https://opentelemetry.io/docs/specs/otel/trace/sdk/#forceflush-1.
A correção focada passou nos 17 testes da classe HTTP. O cenário JSON malformado passou
em 20 repetições (3,096 s); anotação temporária restaurada em finally e ausência de
RepeatedTest conferida. A lista devolvida pela espera continua sendo o inventário inteiro:
um span de outro trace não satisfaz a barreira e ainda faz a contagem exata falhar.
O teste do limite MTR aparecia por último na execução que falhou; sua falta de barreira
não é atribuída como causa daquela ocorrência, somente como risco de isolamento corrigido.

Na revisão final das asserções Cosmos, preparação de PartitionKey/documento/opções foi
movida para fora de assertThrows: somente a chamada SDK cuja falha é esperada fica na lambda.
Reexecutar os quatro testes Cosmos após essa alteração; nenhum comportamento operacional mudou.
O recorte inicial de quatro arquivos foi ampliado para cinco pelo complemento HTTP acima.
## Isolamento antes da descoberta JUnit

Checkpoint após correção HTTP: COMPLIANT em 2026-09-12T16:43:28.2433509-03:00,
fingerprint 2f410c274563c07081e7fefc72e5fdfe74bc055dc2ddd3fdf26d18656488821d,
1.403 testes padrão aprovados, 213 issues/0 novas/graves, cobertura 88,3%, duplicação 4,4%.
CE cc103a6d-a59c-4d2a-bd60-2c04a744b8f1; análise d473d277-4e67-47e8-8b9f-5b83b87c3f9b.
Snapshot intermediário preservado; ainda não atribuir esse checkpoint à alteração abaixo.

A execução mostrou descoberta de Dev Services antes dos testes filtrados. A API antiga
usada pela extensão inicia em discovery desde Quarkus 3.22; filtro JUnit por tag sozinho
não garante isolamento. Completar o critério P1 de opt-in no mesmo pom.xml já em escopo:
Surefire deve excluir as classes de emulador antes da descoberta no perfil padrão; perfis
Service Bus/Cosmos incluem apenas a respectiva família; perfil azure-integration habilita
ambas para a regressão conjunta. Preservar exclusão padrão das classes internas e os tags.
Nenhum arquivo executável adicional, versão, teste suprimido da sua suíte ou runtime alterado.

Verificar 1.403 testes padrão com ausência de startup de emuladores no log; 45 testes no
perfil combinado; seleção de classes por família; novo checkpoint do fingerprint final.
Fontes: https://quarkus.io/blog/new-dev-services-api/ e documentação oficial Surefire
sobre includes/excludes, com exclusões aplicadas antes da seleção JUnit por tags.
## Verificação do isolamento e revisão final

Depois da exclusão de classes no Surefire, mvn -q clean test terminou com exit 0:
1.403 testes em 194 classes, zero falhas/erros/ignorados. O log não contém início de
container, descoberta do Docker nem startup de Azure Dev Services. Evidência:
.codex/.state/cosmos-p1-padrao-isolado.log. A suíte padrão conservou sua cardinalidade.

A revisão independente final, somente leitura, não encontrou achado Critical/Required
nos cinco arquivos executáveis. Confirmou a seleção das famílias, preservação da exclusão
padrão de classes internas e inventário integral de spans na prova HTTP. Não há @Nested
no repositório. A correlação documentada Jaeger/log/EVENTO usa a identidade da operação;
o span CLIENT de persistência não a substitui. A revisão não substitui o teste conjunto.

Comandos canônicos atuais, executados sequencialmente:

- mvn -q clean test: suíte padrão, sem emulador.
- mvn -q -Pcosmos-integration clean test: somente integração Cosmos.
- mvn -q -Pservicebus-integration clean test: somente integração Service Bus.
- mvn -q -Pazure-integration clean test: integração conjunta das duas famílias.

Para foco em uma classe, acrescentar -Dtest=NomeDaClasse ao perfil correspondente.
Alterar apenas test.groups não retira as exclusões anteriores à descoberta; usar os perfis.
A regressão conjunta do novo perfil e o checkpoint final ainda estão em andamento.
## P1-R1: correção delimitada do harness terminal

A execução do perfil azure-integration terminou com 45 testes/12 classes, zero falhas,
um erro e zero ignorados (Maven exit 1). Cosmos passou. O erro ocorreu em
MonitoramentoTerminalEmuladorTest.deveEncerrarPrazoOriginalMesmoComVersaoRemovida,
no BeforeEach: assertFilaVazia(entrada), linha 95, peek com timeout de 30 segundos.
O corpo desse cenário não chegou a executar. O cenário imediatamente anterior havia
concluído redelivery/resultado e destruído a assinatura do listener; o mesmo receiver
CDI de entrada era reutilizado tanto pelo listener quanto pelos peeks do teste.
Isso localiza uma disputa de lifecycle no harness; não prova a causa interna do broker.

Recorte corretivo separado, antes da edição: somente MonitoramentoTerminalEmuladorTest.java.
Manter os receivers CDI para consumo/Complete e criar dois clientes observadores exclusivos
do teste, um por fila, para todos os peeks. Esses observadores nunca assinam receive e
são fechados no AfterEach, mesmo se a preparação falhar. O cenário DLQ tem observador
próprio no try-with-resources. Preservar identidade, entrega, redelivery, prazo, todas as
asserções de resíduos, ausência inicial, remoção e motivos; não descartar mensagens,
não repetir operações falhas nem alterar timeout/cliente/configuração de produção.
Essa separação evita acoplar a consulta de prova ao cancelamento do consumidor.

RED: a falha real da regressão acima. GREEN: executar os 12 cenários terminais, incluindo
a sequência completa de abertura/fechamento, e depois repetir a regressão conjunta.
Revisar o único arquivo adicional desta subfatia e executar um checkpoint final sobre o
conjunto coerente P1 + P1-R1. Não iniciar modelo/persistência operacional durante a correção.
O P1 original continua com cinco arquivos; P1-R1 é uma subfatia corretiva de um arquivo.
P1-R1 GREEN focado: 12 testes terminais aprovados, zero falhas/erros/ignorados, 59,93 s,
Maven exit 0. Relatório preservado em .codex/.state/cosmos-p1-evidencias/terminal-r1-green.txt;
RED em terminal-r1-red.txt. A revisão independente desse arquivo não encontrou achados
Critical/Required: confirmou propriedade dos clientes, fechamento sob falha parcial do
BeforeEach/teardown e preservação das asserções de redelivery, remoção, resíduos e DLQ.
Não atribuir a causa interna ao emulador: a correção retira o acoplamento de lifecycle
observado no harness. Regressão conjunta posterior em .codex/.state/cosmos-p1-conjunto-r1-final.log.
## P1-R2: observação externa no harness de telemetria Service Bus

Na regressão posterior ao R1, os 12 cenários terminais passaram novamente (54,01 s),
assim como Cosmos. Surgiu um timeout em ServiceBusTelemetriaEmuladorTest,
deveCaracterizarScheduleSemTransacao, linha 140: peek final depois de receberECompletar.
A mensagem foi agendada, recebida e concluída; o helper também confirmou fila vazia antes
do next cancelar receive. O timeout ocorreu na consulta externa adicional depois desse
cancelamento, não no schedule nem no Complete. A prova reporta 40 segundos de bloqueio.

Recorte corretivo P1-R2, um arquivo: ServiceBusTelemetriaEmuladorTest.java, preservando
suas alterações F0 anteriores. Criar observadores próprios por fila/teste para os peeks
externos ao receive, inclusive DLQ, com fechamento garantido. Preservar os peeks internos
que já integram a caracterização do receiver CDI enquanto receive permanece ativo.
Todas as asserções de identidade, carrier, spans, resíduos, horário, commit/rollback e
redelivery permanecem; não remover a consulta que falhou nem acrescentar retry/timeout.
O builder/configuração SDK é o mesmo; nenhum provider de tracing é adicionado/desabilitado.

RED: falha real acima, a preservar antes do clean. GREEN: 11 testes da caracterização,
revisão independente e regressão conjunta posterior. O checkpoint final abrange P1,
P1-R1 e P1-R2; modelo e persistência operacional continuam fora deste gate.
R2 GREEN focado: 11 testes aprovados, zero falhas/erros/ignorados, Maven exit 0.
Revisão independente sem achados Critical/Required. Relatório telemetria-r2-green.txt.

## P1-R3: observação da DLQ no harness do resultado

A regressão anterior ao R2 terminou com 45 testes/12 classes, zero falhas e dois erros,
zero ignorados. Além do A1 descrito acima, o último cenário de
MonitoramentoResultadoEmuladorTest.deveMoverContratoInvalidoDaSaidaParaDlqSemLogFinal
teve timeout de 30 segundos na linha 174: assertFilaVazia(dlq) após Complete/next/cancel.
Identidade, corpo e motivos da DLQ foram verificados antes; falhou a consulta final.
Esse segundo erro só ficou disponível ao término das classes finais; não omitir da evidência.

Recorte P1-R3, um arquivo: MonitoramentoResultadoEmuladorTest.java. Acrescentar apenas
observador DLQ exclusivo no try-with-resources existente; redirecionar os peeks inicial,
de localização e final para ele. O receiver DLQ continua recebendo/Complete, sem mudar
asserções de log, identidade, corpo, motivo ou retomada de um fluxo válido depois.
Os listeners principais ficam ativos durante essa classe e não são cancelados entre
cenários; não alterar seus clientes nem outras consultas sem evidência de necessidade.
RED é a falha acima, preservada em resultado-r3-red.txt. GREEN: quatro cenários da classe,
revisão e regressão conjunta posterior, seguida do checkpoint de todos os recortes do gate.
R3 GREEN focado: quatro cenários aprovados, zero falhas/erros/ignorados, 28,51 s,
Maven exit 0; resultado-r3-green.txt preservado. Revisão independente sem achados
Critical/Required: propriedade dos dois clientes DLQ, fechamento mesmo se a criação do
segundo falhar, preservação de todas as asserções e do fluxo válido subsequente.

Disponibilidade Jaeger local reconferida: HTTP 200 em http://localhost:16686/.
Isso não comprova o trace completo nem navegação Cosmos: ambos seguem pendentes em P3/P5-P10.
Os peeks do observador do produto serão definidos em P9, separadamente destes testes.

Revisão de alcance: P1 base possui cinco executáveis; R1, R2 e R3 têm um arquivo cada.
As três subfatias corrigem falhas reais da regressão, com RED/recorte registrados antes
das edições, sem mudar production runtime, timeout ou retry nesses complementos.
O conjunto final é executado sequencialmente, seguido do checkpoint somente se os
45 testes/12 classes tiverem zero falhas, erros e ignorados. Relatórios são preservados
antes do clean do checkpoint. Baseline original não será reinicializado.
## Decisão Sonar e preparação do commit — 2026-09-12

Regressão final R3 aprovada: 45 testes/12 classes, zero falhas/erros/ignorados. O checkpoint
seguinte executou 1.403 testes padrão/194 classes, Maven/build/scanner/CE com sucesso,
mas classificou NON_COMPLIANT/PENDING por quatro issues MINOR java:S1481: aliases
consultaEntrada/consultaSaida nos try-with-resources de A1 e do teste terminal.
Nenhuma issue HIGH/BLOCKER/CRITICAL, cobertura 88,3%, duplicação 4,4%, 217 issues frente
ao baseline original de 217 (quatro novas; totais iguais não significam ausência de novas).
CE db169e3e-0930-4caa-99a8-51b1d2fb8ad2; análise 927e01cb-6736-4741-ae63-47b1fe2d84a3;
fingerprint ee9506d1a64070b1ef0ac0fa36f7974000222446df54433f872c612b6b72f7df.

O usuário escolheu explicitamente ContinuarAjustes, reiterando a decisão; registrada por
./validar-checkpoint-sonarqube.ps1 -HumanDecision ContinuarAjustes. Correção delimitada:
usar variáveis sem nome suportadas pelo JDK25 nos mesmos recursos dos dois testes.
Fechamento automático, ordem e tratamento de exceções permanecem iguais. Nenhuma supressão
de regra nem mudança de comportamento. O novo checkpoint compila todos os testes e roda
a suíte padrão; as 45 integrações já aprovadas não mudam semanticamente com essa renomeação.

O usuário também solicitou commit local estável do decidido/codificado e atualização do
guia para implementar o restante. Essa autorização supera a restrição anterior de commit
para este marco; push continua fora do pedido. Não equivale a aceite final da feature.
O guia novo é guia-desenvolvimento.md, com links nos guias Service Bus existentes.
## Ponto seguro para retomada — 2026-09-13

P1, P1-R1, P1-R2 e P1-R3 estão concluídos tecnicamente. Resultado final:
- `mvn -q clean test`: 1.403 testes/194 classes, zero falhas, erros ou ignorados.
- `mvn -q -Pazure-integration clean test`: 45 testes/12 classes, zero falhas, erros ou ignorados.
- P1-R1: 12 cenários terminais; P1-R2: 11 cenários de telemetria; P1-R3: 4 cenários de resultado/DLQ; todos aprovados nas provas focadas.
- Checkpoint Sonar final `COMPLIANT`, sem issues novas ou graves, cobertura 88,3%, duplicação 4,4%; fingerprint `a0a2990acf0a2ca080ff667f25d04a54cfa5d4b0d4b7b901c555d68866fbdccf`.
- Jaeger local respondeu HTTP 200 em `http://localhost:16686`; a visualização do trace completo ainda não foi provada.

O commit deste ponto contém o gate Cosmos/Dev Services, a instrumentação B1 já existente,
as correções de isolamento dos testes, ADR-0013, documentação arquitetural e o guia de
continuidade. Não inclui `.codex/.state`, `target`, `.env`, patch local ou formatos derivados.

## O que falta

P2: modelos e redução do acompanhamento, sem alterar regras do monitoramento. P3: adapter
Cosmos operacional, documentos EXECUCAO/EVENTO, batch/ETag/idempotência, lifecycle e spans
CLIENT. P4: portas/ACLs. P5: diário durável da intenção do POST e confirmação após ACK.
P6: entrada, Pré-Valida/MTR/Hub, decisão, resultado e Complete com contexto causal. P7:
reagendamento e commit transacional. P8: leitura da saída, log e settlement. P9: observação
não destrutiva de filas/DLQs. P10: prova integrada real Cosmos/Service Bus/Jaeger, reinício,
falhas ambíguas, navegação trace/span ↔ evento/execução e aceite humano final.

A aplicação ainda não grava o fluxo no Cosmos e não há endpoint público de consulta. O
container local criado pelo teste é descartável; DES real ainda não foi acessado. A mesma
estratégia homogênea deve ser mantida: `traceId`/`spanId` da operação em Jaeger, logs e
EVENTO Cosmos; I/O Cosmos como span CLIENT filho; payload e IDs de negócio permanecem no
Cosmos, não nos sinais. Retomar por P2 e atualizar o checklist antes do próximo RED.