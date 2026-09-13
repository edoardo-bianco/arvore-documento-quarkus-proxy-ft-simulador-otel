# Execução de 10.1-B1.2 — HTTP e iniciação

## Estado — 2026-09-11

Pedido humano **"10.1-B1.2"**, usando o GO **"go"** já registrado para o
[desenho e CP-B1](desenho-10-1-b1.md). Somente esta subfatia foi implementada.
**B1.2 concluída tecnicamente**, com testes/build aprovados e Sonar COMPLIANT.
Nenhum aceite excepcional ou encerramento humano inferido.

## Baseline e limites

Baseline LOCAL_SONAR / READY, 217 issues originais, preservado e comparado integralmente
com o snapshot COMPLIANT da B1.1 antes da primeira alteração executável.
Fingerprint inicial:
`d5462fde3863dfe04e3e3214122b076b6be7bed03cc6dc5cf8925dfa75596b05`.
Credencial somente herdada do processo; `sonar/` ausente. Sem reinicialização de baseline.

Cinco arquivos executáveis: Resource REST, caso de uso, seus dois testes e
`SuporteTelemetriaMonitoramento`. Não altera publisher, contrato REST/JSON, portas,
dependências, configuração operacional ou providers globais. C0.4/C9.1-L preservados;
PRODUCER/W3C/logs de publicação continuam B1.3/B1.4.

## Implementação

- Reutiliza o SERVER automático, renomeado para
  `simtr-hub.api.monitoramento-dossie.iniciar` quando o método REST executa (202/500).
  Mantém rota, parent remoto e lifecycle do Quarkus. Os 400 anteriores ao método
  mantêm nome e eventos automáticos existentes.
- Cria um INTERNAL `orquestrador.service.monitoramento-dossie.iniciar`, filho do
  Context capturado na invocação. A criação é lazy e fica antes da memoização.
- Prepara parâmetros/IDs e chama/assina a porta sob Scope lexical do INTERNAL.
  Um future local observa o resultado sem bloquear: o Scope da assinatura fecha
  antes de registrar/encaminhar a conclusão, inclusive se ela já foi síncrona.
  O callback abre seu próprio Scope, marca status/encerra e fecha antes de notificar
  o emitter. Preserva o contexto Mutiny do assinante.
- Sucesso UNSET, sem atributos/eventos/links novos. Falha ERROR com somente
  `error.type=FALHA_INICIO`, preservando a mesma exceção no fluxo, sem exportar
  mensagem, causa, suppressed ou stacktrace.
- Uma operação/span/publicação por invocação, mesmo com assinantes simultaneamente
  pendentes, cancelamento de um/todos e repetição tardia. Cancelar observadores
  não cancela o upstream; seu término real encerra o span.

## Diagnósticos e refinamentos locais

### Nome HTTP sobrescrito no envio da resposta

RED isolado: quatro falhas de nome em 202/500. `Span.updateName` isolado no método
continuou falhando. O fonte Quarkus 3.33.2.1 mostra que
`HttpInstrumenterVertxTracer.sendResponse` chama `HttpServerRoute.update` com
fonte SERVER_FILTER antes de encerrar o span.

A solução local informa a mesma rota estática com fonte CONTROLLER, pela API pública
OpenTelemetry instrumentation 2.23.0, e então renomeia o span. A precedência evita a
sobrescrita tardia. Os testes conferem nome final e `http.route` preservado.
Não houve filtro global, segundo SERVER ou encerramento manual.

Uma asserção inicial incorreta exigia eventos vazios também nos 400 anteriores ao
método. A caracterização mostrou um evento automático `exception` preexistente;
a asserção foi corrigida antes do RED isolado. Sanitização global desses eventos
não pertence a esta mudança.

### Restauração e decoração automática de callbacks

O `OpenTelemetryMpContextPropagationProvider` instalado anexa o Context capturado,
descarta o Scope retornado e só restaura o anterior quando seu span está gravando.
Uma reprodução temporária com Uni puro, sem caso de uso ou span manual, confirmou
a falha. O primeiro checkpoint completo parou no Maven por essas falhas e não
gerou nova análise Sonar. O defeito não foi tratado como aprovação ou reprovação humana.

Scopes internos ao callback não bastam para corrigir um decorator externo.
O fonte SmallRye 2.3.0 confirmou que wrappers `ThreadContext` instalam/restauram
sua política local durante a execução, e o interceptor Mutiny preserva callbacks
já contextualizados. A aplicação usa somente a API pública MicroProfile já instalada:
um ThreadContext por bean propaga ALL_REMAINING, deixa OpenTelemetry unchanged e não
limpa outros contextos. Seu `contextualSupplier(...).get()` envolve a construção
inteira do Uni, incluindo `memoize().indefinitely()`. Isso é necessário porque
`UniMemoize.until` também decora o predicado interno de invalidação.

O processamento segue lazy: esse supplier apenas constrói o Uni. Os Scopes manuais
controlam OTel durante preparação, assinatura e término. A escolha refina o mecanismo
local para cumprir o GO; não muda configuração global nem reduz o critério de restauração.
A revisão independente inicial foi corrigida após a leitura de SlowActiveContextState
e do predicado de memoização; o parecer final não apresentou achado obrigatório.

A reprodução temporária sem instrumentação foi removida da suíte do produto após
registrar sua evidência. Os dois testes estritos do caso de uso, com assinante gravável
e não gravável, permaneceram ativos e passaram. Uma prova adicional de erro usa
UniAssertSubscriber diretamente: `subscribeAsCompletionStage` criado pelo consumidor
fora da política local também pode acionar o defeito global do provider. Esse efeito
externo permanece uma limitação do runtime; a mudança não promete consertar callbacks
arbitrários criados pelo chamador. As provas com futures sob outro Context continuam
no teste principal e nas regressões de cancelamento.

Em B1.3, manter captura explícita e Scopes próprios do PRODUCER dentro dessa política;
não depender de propagação OTel automática nos callbacks do publisher.

## Fontes verificadas

- [Quarkus Azure Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/index.html)
  e [Service Bus Extension](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html):
  extensão efetiva 1.2.5, SDK 7.17.12; nenhuma mudança de dependência.
- [Quarkus OpenTelemetry 3.33](https://quarkus.io/version/3.33/guides/opentelemetry-tracing/)
  e [contrato de Context](https://opentelemetry.io/docs/specs/otel/context/).
- Fontes locais Quarkus 3.33.2.1: HttpInstrumenterVertxTracer,
  OpenTelemetryMpContextPropagationProvider e QuarkusContextStorage.
- Fontes locais OpenTelemetry instrumentation 2.23.0: HttpServerRoute/Source.
- Fontes locais Mutiny 3.1.1: UniMemoize, UniMemoizeOp, UniCreate,
  UniSubscribe e DefaultContextPropagationInterceptor.
- Fontes locais SmallRye Context Propagation 2.3.0: wrappers contextualizados e
  SlowActiveContextState. A eficácia da política local depende dessa implementação;
  os testes estritos protegem futuras atualizações.
- [Quarkus #52170](https://github.com/quarkusio/quarkus/issues/52170) registra um
  defeito semelhante em QuarkusContextStorage, corrigido anteriormente. Não é prova
  de correção do provider MP inspecionado nesta versão; não foi usado para prometer
  uma versão atualizada que resolveria este caso.


## Validação final

- Focados: **17 HTTP + 15 caso de uso + 9 publisher = 41 testes**, sem falhas,
  erros ou ignorados. A última inclusão foi o cancelamento efetivo do upstream,
  verificado junto com os outros 14 testes do caso de uso; produção já estabilizada.
- HTTP: parent remoto, nome exportado, rota, 202/500 e 400 anterior à porta.
- Caso de uso: ausência de efeitos antes da assinatura; assinantes pendentes/tardios,
  cancelamento de um/todos, sucesso e falha em outra thread, upstream cancelado como
  falha, exceção preservada, atributos exatos, contextos restaurados e invocações
  intercaladas com pais/identidades independentes. Contexto Mutiny também preservado.
- A2 final: **2 testes**, sem falhas/erros/ignorados; terminal 29,04s, fixture 27,40s.
- Suíte padrão no checkpoint: **1.358 testes em 192 classes**, sem falhas/erros/ignorados;
  build completo aprovado, incluindo contratos e ArchUnit.
- Revisões local e independente concluídas, sem achados obrigatórios na solução final.
  O revisor não executou testes; as execuções e inventários foram conferidos pelo agente principal.
- Diff sem erros de whitespace; hashes de A1, `.env` e
  `.codex-doc-alignment.patch` preservados. Demais mudanças anteriores mantidas.
- Suíte opt-in completa não repetida nesta subfatia; foram repetidos os dois cenários A2
  afetados. A prova completa POST→broker com PRODUCER/carrier permanece B1.5.

### Inventário A2 final, antes do clean do checkpoint

Ambos os inventários estavam VALIDADO após todas as asserções, incluindo resultado
correto e filas vazias. Captura CDI com controle positivo e forceFlush.

| Cenário | Spans / logs | Relação inicial e efeito |
|---|---|---|
| Terminal | 2 / 2 | HTTP → INTERNAL; CONCLUSIVO / CONFORME, uma tentativa. |
| Fixture Hub real | 5 / 15 | HTTP → INTERNAL + três consultas Hub ainda raízes; QUARENTENA / MAXIMO_TENTATIVAS, três tentativas. |

- Terminal: trace `7d7651a1417c438f909fb834e00dcbba`,
  HTTP `e2835c9b8910c02e`, INTERNAL `113109b2f4c47a5c`.
- Fixture: trace `e3be9e1df35f46f8a62003075789a1ef`,
  HTTP `b518c29d6e106140`, INTERNAL `5aec06e3dcce2064`.
- Parent remoto em ambos: `2222222222222222`. INTERNAL sem atributos/eventos/links,
  UNSET, duração contida no SERVER.
- Logs de settlement mantêm o par do HTTP; decisões e log final permanecem sem
  contexto. As três consultas Hub mantêm traces próprios e seus logs correlacionados.
  Nenhuma correção do consumo foi antecipada.
- Carrier vazio na tentativa inicial terminal e nas tentativas 2/3 observadas na
  fixture; ambos os reagendamentos vistos como SCHEDULED.

### Checkpoint final

Executado `./validar-checkpoint-sonarqube.ps1` em
`2026-09-11T14:54:49.2383466-03:00`.

| Evidência | Resultado |
|---|---|
| Situação técnica / decisão | COMPLIANT / NOT_REQUIRED |
| Issues abertas / baseline | 213 / 217 |
| Novas / HIGH, BLOCKER, CRITICAL | 0 / 0 |
| Cobertura / duplicação | 88,1% / 4,4% |
| Analysis key | `2980566c-0c37-4f26-8d59-e7b62010a82a` |
| Compute Engine | `1dd48ac8-d45c-41d5-bf6e-ca066e17d2d5` |
| Fingerprint final | `4d2504a12063a58bcfa25d77af573501b942c2e248b6c0a30dd1a447f6016dd3` |

Baseline e assessment comparados integralmente com o snapshot B1.1, sem divergência.
Snapshot final em `.codex/.state/session-after-10-1-b1-2-compliant-20260911.json`.
Markdown posterior ao checkpoint não altera o fingerprint executável.

**B1.2 concluída tecnicamente. Próxima: B1.3.** C3, encerramento humano da feature e
demais subfatias permanecem pendentes. Nenhum staging, commit ou push.
