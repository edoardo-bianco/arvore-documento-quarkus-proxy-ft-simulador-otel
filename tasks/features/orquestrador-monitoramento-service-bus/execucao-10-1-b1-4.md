# Execução de 10.1-B1.4 — logs da publicação inicial

## Estado final — 2026-09-11

**B1.4 concluída tecnicamente.** S3776 corrigida após ContinuarAjustes humano,
45 testes focados e novo checkpoint completo aprovados. Situação COMPLIANT /
NOT_REQUIRED. Próximo item: B1.5, sem encerrar B1/10.1/C3 ou a feature.

## Retomada após ContinuarAjustes

O usuário respondeu **ContinuarAjustes** após a apresentação da S3776.
Decisão registrada com ./validar-checkpoint-sonarqube.ps1 -HumanDecision ContinuarAjustes.
Aplicada somente a extração proposta de finalizarPublicacao no publisher,
preservando try/Scope, finally/end, identidade das falhas e o encaminhamento terminal.
Os cinco arquivos do incremento permanecem os mesmos; nenhum outro executável foi
alterado nesta correção. Os **45 focados** (26 logs + 19 spans) passaram após
extrair o método; revisão independente confirmou equivalência. Novo checkpoint
completo aprovado, conforme o fechamento abaixo. A2 foi executada antes dessa extração privada; suas relações
permanecem cobertas pelas regressões focadas, sem nova execução A2 nesta correção.
O primeiro checkpoint e a proposta abaixo permanecem como histórico.

## Início — 2026-09-11

Pedido humano: **10.1-B1.4**. Execução do próximo item sob o GO do desenho/CP-B1.
B1.3 permanece COMPLIANT, fingerprint ab2d6fa84161732355b713c3809bc5594e61d4fb91850e2e04a4790c566f9cca.
Baseline LOCAL_SONAR e assessment conferidos contra o snapshot B1.3, preservados
(217 issues); nenhuma pasta sonar/, credencial herdada disponível sem exposição.

## Recorte e verificações

Cinco arquivos executáveis conforme refinamento do desenho: publisher, helper local,
novo teste JSON, teste de spans B1.3 e suporte A2. Nenhuma alteração no mapper,
contratos, configuração, dependências ou provider. B1.5/B2–B5 continuam pendentes.

Concretizações locais da segurança já prevista:
- Só copiar IDs com forma UUID canônica (comparação sem trim, ignorando caixa).
  Cada ID inválido/ausente é omitido; tentativa numérica somente quando positiva.
  message_id derivado do modelo somente com ambos IDs e tentativa válidos.
  Não rejeitar nem alterar a publicação por metadado indisponível.
- MDC vazio no ExtLogRecord, traceId/spanId explícitos do PRODUCER, nenhum Throwable,
  mensagem externa, baggage, NDC, payload ou dado de dossiê nos novos eventos.
- Preservar construtor CDI; helper local substituível em construtor package-private
  para provar RuntimeException propagada. Captura restrita à chamada de logging;
  sem captura de Error, destino alternativo ou evento recursivo.
- Classificação da falha permanece definida na etapa conhecida (preparação ou SDK);
  não inferir etapa por texto/classe genérica da exceção segura.
- Encerrar span em finally após tentativa de log e observar a conclusão desse callback
  antes de encaminhar ao emissor, evitando Uni pendente se ocorrer Error no logging.

RED/GREEN: log somente após ACK, falhas SDK/preparação/serialização, memoização,
cancelamento, contextos inválidos/não graváveis, isolamento e JSON real com sentinelas.
Provar defeito real de handler/filtro e RuntimeException propagada pelo helper.
A2 exige exatamente 3/6 spans e 3/16 logs, mantendo demais relações e inventários.
Executar focados, duas A2, revisão independente e checkpoint completo.

## RED/GREEN e implementação

RED inicial: 1 teste, 1 falha por ausência de registro após ACK (esperado 1, obtido 0).
GREEN inicial: o mesmo teste passou com JSON real e contexto do PRODUCER.

A matriz inicial de 28 testes encontrou 14 falhas: encadear dois whenComplete
transformava falhas originais em CompletionException. Diagnóstico confirmado pelos
tipos/identidades nas regressões B1.3 e no novo teste. Correção local: handle devolve
a falha da publicação como valor; whenComplete observa esse valor ou uma falha da
própria observação. Todos os 28 passaram após a correção, sem relaxar asserções.
Error propagado pelo helper termina o Uni excepcionalmente, com span encerrado;
a captura restrita de RuntimeException mantém o ACK ou a exceção segura original.

Matriz ampliada: **92 testes em seis classes**, zero falhas/erros/ignorados:
26 LogPublicacaoEntrada, 19 PublisherSpans, 9 publisher funcional,
6 OrquestradorEntradaLog, 15 iniciação e 17 HTTP.
A comparação exata das chaves do JSON e a correlação das operações intercaladas
foram reforçadas depois dessa execução e serão verificadas novamente.

O helper reutiliza CamposLogJson/formatter existente. Não recebe Throwable.
AtomicReference local à operação guarda a classificação definida na etapa conhecida,
pois preparação e SDK podem produzir a mesma classe de exceção. Não há recuperação
nem estado entre invocações. O construtor CDI permanece inequívoco.

Revisão independente identificou duas melhorias na fixture: limitar o handler/filtro
por categoria, evento e monitoramento da prova; comparar o conjunto exato das chaves
JSON. Ambas incorporadas. Handler e filtro reais têm RuntimeException encaminhada
ao ErrorManager pelo LogManager; handler pai continua escrevendo JSON. Um mock do
helper prova separadamente RuntimeException propagada e Error fora do catch.
O handler temporário é removido/fechado em finally.

Fontes conferidas: [Azure Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/index.html),
[Service Bus](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html)
e [logging Quarkus 3.33](https://quarkus.io/version/3.33/guides/logging/#json-logging-format).
Versões preservadas: Quarkus 3.33.2.1, extensão 1.2.5, SDK 7.17.12.
Bytecode local JBoss LogManager 3.2.1.Final confirma encaminhamento de falha de
doPublish/filtro ao ErrorManager; não se infere escrita durável pelo retorno do logger.

## Verificações finais e integração A2

Depois dos reforços de whitelist e isolamento, **45 testes** passaram novamente:
26 LogPublicacaoEntrada (2,434s) + 19 PublisherSpans (0,144s), sem falhas/erros/ignorados.
Revisão independente final dos cinco arquivos: nenhum achado obrigatório pendente.
Revisão local conferiu correção, simplicidade, arquitetura, segurança, desempenho,
testes e escopo; git diff --check aprovado.

Comando: mvn -q -Pservicebus-integration
"-Dtest=MonitoramentoTelemetriaEmuladorTest,MonitoramentoTelemetriaHubEmuladorTest" test.
**2 testes, zero falhas/erros/ignorados**: terminal 28,69s e fixture Hub 27,14s.
Exporter CDI com controle positivo, forceFlush, inventários VALIDADO e filas vazias.

| Cenário | Spans / logs | Resultado |
|---|---|---|
| Terminal | 3 / 3 | CONCLUSIVO / CONFORME, uma tentativa |
| Fixture Hub real | 6 / 16 | QUARENTENA / MAXIMO_TENTATIVAS, três consultas |

Terminal: trace bfb3d584de614108a41fc79a89f8bf98;
SERVER 79f7c6249646947d → INTERNAL b3e7f4705c440319 → PRODUCER a07b901dfd650c67.
Log confirmado tem o par do PRODUCER. Mensagem inicial ACTIVE, sequência 1,
somente traceparent=00-bfb3d584de614108a41fc79a89f8bf98-a07b901dfd650c67-01.

Fixture: trace dc1eaeeea2ec4b80a26b4a98527bf7ff;
SERVER e87e50530ea21e6d → INTERNAL eb08c723e11ddbff → PRODUCER 9eb022fdcdc40848.
Log confirmado tem o par do PRODUCER. Parent remoto dos SERVER: 2222222222222222.
Spans manuais UNSET, sem descrição/eventos/links; PRODUCER tem os quatro atributos
aprovados e duração contida no INTERNAL.

Inventário completo dos eventos, sem contagem mínima nem aceitação de evento desconhecido:
- Terminal: 1 confirmação inicial + 1 resultado final + 1 settlement.
- Fixture: 1 confirmação inicial + 9 logs Hub (3 por consulta) + 2 decisões REAGENDAR
  + 1 resultado final + 3 settlements.
- Os três spans Hub seguem raízes, nos traces 7a8bef6ea228b74d8709d87efcd09658,
  09031abc3926d0f90059a7cd6b50469e e c87c0c43fda1eb7219d42945b9c8042c;
  seus três logs continuam correlacionados a cada consulta.
- Settlement mantém o par HTTP; decisões e resultado final continuam sem contexto.
  A2 não comprova a correlação distribuída após o consumo nem acesso ao MTR externo.
- Fixture observou tentativa 2 SCHEDULED (seq.2), tentativa 3 SCHEDULED (seq.4)
  e ACTIVE (seq.5), todas sem carrier. A primeira mensagem foi observada no cenário
  terminal; não foi capturada pelo peek da fixture.

Inventários target/provas-10-1-a2/terminal.json e hub-real.json lidos integralmente
antes do clean do checkpoint; resultados relevantes preservados acima.
A1 raw SDK permanece inalterada e validada na B1.3; suíte opt-in completa fica para B1.5.

## Primeiro checkpoint — histórico NON_COMPLIANT / PENDING

Comando ./validar-checkpoint-sonarqube.ps1 completou Maven clean verify, build,
SonarScanner e Compute Engine. **1.403 testes em 194 classes**, zero falhas,
erros ou ignorados; BUILD SUCCESS.

| Evidência | Resultado |
|---|---|
| checkedAt | 2026-09-11T20:56:17.7836302-03:00 |
| Situação / decisão | NON_COMPLIANT / PENDING |
| Issues abertas / baseline | 214 / 217 |
| Novas / HIGH, BLOCKER, CRITICAL | 1 / 1 (mesma issue) |
| Cobertura / duplicação | 88,2% / 4,4% |
| Analysis key | 4ec84c8d-e4bb-4c61-ade2-9f59800332f5 |
| Compute Engine | 64afc115-5f98-48c0-a085-606bdc842c74 |
| Fingerprint | 659c5eb3bf6ba7359b0c6a97a9f79c8b3d0ab6098f2ec3387582bf2269bd6369 |

Issue f7b33795-91d9-4038-ac93-83b46be4802f: **java:S3776**, CRITICAL,
maintainability HIGH, OPEN, MonitoramentoEntradaPublisher.java:54, método executar.
Complexidade cognitiva **17**, limite **15**. Fluxos apontados: if status (linha73,
+4), if serialização (76,+4), ternário de falha (84,+4), if emissor (85,+4), else (87,+1).
Cobertura e duplicação cumprem os limites; a situação decorre da única issue nova/grave.

### Ajuste apresentado no primeiro checkpoint

Extrair o corpo do handle para método privado na mesma classe:

```java
private Throwable finalizarPublicacao(TentativaMonitoramento tentativa,
        Context contexto, String tipoFalha, Throwable falha)
```

Esse método obtém Span.fromContext(contexto), mantém o mesmo try de Scope com
status/error.type e emissão condicional, encerra span em finally e retorna o mesmo
Throwable. A lambda chama finalizarPublicacao(tentativa, contexto, tipoFalha.get(), falha).
O segundo whenComplete e registrarPublicacao permanecem iguais. A extração separa
a conclusão observável da composição reativa, sem nova classe/contrato/configuração.
Complexidade estimada: executar 9 e helper 2, a confirmar pelo Sonar após a decisão.

Proposta revisada independentemente: preserva identidade, Scope, Error, serialização,
memorização e política CP-B1. O encaminhamento apresentado foi registrar a decisão
humana pelo script, aplicar somente essa extração e repetir 45 focados/checkpoint.
A execução ocorreu após a resposta ContinuarAjustes registrada no início deste documento,
sem ampliar para B1.5.

Baseline e assessment comparados integralmente ao snapshot B1.3 e preservados.
Fingerprint corrente igual ao checkpoint. Snapshot imutável:
.codex/.state/session-after-10-1-b1-4-noncompliant-pending-20260911.json.

Branch feature/orquestrador-monitoramento-service-bus, HEAD 1f32466, índice vazio.
Hashes de .env, .codex-doc-alignment.patch e A1 raw SDK iguais aos anteriores.
Nenhum arquivo derivado gerado, staging, commit ou push.

O primeiro checkpoint permaneceu pendente até o usuário responder **ContinuarAjustes**.
A resposta foi registrada pelo script; não houve aceite excepcional nem decisão inferida.

## Fechamento técnico após ContinuarAjustes

Aplicada somente a extração privada de finalizarPublicacao no publisher; os demais
executáveis ficaram inalterados. **45 focados** passaram novamente.
**1.403 testes padrão em 194 classes**, zero falhas/erros/ignorados e BUILD SUCCESS.
Revisão independente da extração: nenhum achado pendente; sem mudança semântica.

A evidência de 92 focados e 2 A2 anteriores permanece registrada com seus limites.
As integrações não foram repetidas após a extração; testes de logs/spans e suíte padrão
foram repetidos. A suíte opt-in completa está prevista para B1.5.

S3776 f7b33795-91d9-4038-ac93-83b46be4802f: **CLOSED / FIXED**,
consultada após o novo checkpoint, updateDate 2026-09-11T21:06:11-03:00.

| Evidência final | Resultado |
|---|---|
| checkedAt | 2026-09-11T21:06:43.0732816-03:00 |
| Situação / decisão | COMPLIANT / NOT_REQUIRED |
| Issues abertas / baseline | 213 / 217 |
| Novas / HIGH, BLOCKER, CRITICAL | 0 / 0 |
| Cobertura / duplicação | 88,3% / 4,4% |
| Analysis key | d8794447-6dca-468b-9fa2-c9dfc826bdb7 |
| Compute Engine | 76addbeb-a1dd-402a-88bf-1f56436a0e2a |
| Fingerprint | b77639406f1d5e434ce682712060a635b3a770e670c11f843fe88580d331939d |

Baseline e assessment originais comparados integralmente ao snapshot B1.3 e preservados.
Fingerprint corrente igual ao checkpoint; snapshot final:
.codex/.state/session-after-10-1-b1-4-compliant-20260911.json.
Markdown posterior não modifica o fingerprint executável. git diff --check aprovado,
índice vazio e arquivos locais protegidos preservados.

**B1.4 concluída tecnicamente; próximo item B1.5**, sob o GO do desenho/CP-B1.
Sem staging/commit/push; C3 e encerramento humano da feature permanecem pendentes.
