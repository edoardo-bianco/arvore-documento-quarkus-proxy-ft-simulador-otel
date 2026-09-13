# Desenho 10.1-B2 — contexto da entrada e avaliação

## Escopo e autorização

Continuidade do pedido humano de rastreabilidade completa, usando nomes/limites C0.4.
Não introduz dependência, contrato público, carrier alternativo ou helper transversal.
B1.5 complementar deve fechar tecnicamente antes do RED de B2.1.

## B2.1 — contexto por entrega

Até cinco arquivos: MonitoramentoEntradaListener, MonitoramentoEntradaListenerTest,
MonitoramentoEntradaLogTest e novo teste dedicado de tracing se necessário.
A regressão A2 recebe atualização explícita em B2.3, pois suas contagens/carência de
contexto caracterizam o estado anterior. Não enfraquecer essas asserções silenciosamente.

CONSUMER `process {fila}` começa ao assinar o tratamento de cada entrega e termina depois
do settlement/commit, erro ou cancelamento dessa entrega. Não abrange o tempo aguardando
ativação futura na fila. CLIENTs de settlement serão acrescentados em B3/B4.

Extrair apenas strings traceparent/tracestate das application properties, usando
W3CTraceContextPropagator e Context.root(). Propriedade ausente, não textual ou inválida
não herda contexto da thread/pump e não altera decisão funcional. Não usar Diagnostic-Id,
correlationId, baggage ou outros valores como pai. Scopes lexicais nos callbacks que
mapeiam, assinam a porta, decidem e executam/liquidam; nenhum Scope atravessa uma thread.

Atributos iniciais: messaging.system=servicebus; messaging.destination.name=fila;
messaging.operation.name=process; messaging.operation.type=process; delivery_count e
sequence_number numéricos do broker. Após mapeamento válido: tentativa_atual numérica.
Decisão e settlement usam somente enum local. Não exportar corpo/IDs de negócio,
propriedades arbitrárias, Throwable externo ou sua mensagem. Identidades técnicas para
busca serão tratadas no desenho específico, sem copiar texto de mensagem não validado.

Status UNSET para decisões funcionais confirmadas; ERROR/error.type controlado para
CONTRATO_ENTRADA_INVALIDO, FALHA_PROCESSAMENTO, FALHA_SETTLEMENT ou CANCELAMENTO.
Falha de processamento continua escolhendo abandon; falha ao liquidar não gera segundo
settlement. Cancelamento encerra span e preserva a propagação de cancelamento já existente.
Não modificar fábrica/startup/flags, assinaturas únicas ou política transacional.

RED/GREEN: carrier válido, tracestate, inválido/ausente/não textual, dois traces intercalados,
callback em outra thread, processamento e ACK pendentes, cancelamento, falha técnica,
contrato inválido e ausência de dados proibidos. Um span por entrega, sem vazamento no
chamador e sem segundo processamento/settlement. Regressões de listener/log e checkpoint.

## B2.2 — avaliação e pré-validação

Até cinco arquivos: ProcessarTentativaMonitoramentoUseCase, PreValidacaoSimuladaAdapter,
respectivos testes e suporte local de teste se necessário.
INTERNAL `doctree.service.monitoramento-mtr.avaliar` por invocação memoizada; abrange
validação, pré-validação, eventual Hub, decisão/publicação executada pela porta.
INTERNAL `doctree.service.prevalidacao.dossie.consultar` lazy por assinatura do adapter.
Contexto capturado por invocação e reaplicado explicitamente na construção/assinatura e
nos callbacks assíncronos. Política local ThreadContext preserva demais contextos e deixa
OpenTelemetry sob Scopes explícitos, conforme B1. Sem annotation que capture Throwable.
Falhas mantêm identidade/contrato original; somente error.type controlado nos spans.
Reassinatura, cancelamento e memorização conservam efeitos existentes. O Hub recebe o
contexto pela porta atual; seu wrapper e sinais próprios permanecem iguais.

## B2.3 — prova integrada da entrada

Até três arquivos: SuporteTelemetriaMonitoramento e as duas provas A2 terminal/Hub real.
Exigir contagens/parentage exatos do estado B2. B3–B5 ainda não existem: resultado,
reagendamento e saída continuam declarados como lacunas até seus incrementos.
Comprovar primeira entrega ligada ao POST e Hub filho da avaliação; futuras entregas sem
carrier continuam raízes próprias até B4, sem criar correlação fictícia no teste.
Executar suíte opt-in, revisão, checkpoint e registrar inventário sanitizado.

## Revisão e limites

Revisão independente somente leitura recomendou separar borda e avaliação. A sugestão
de terminar CONSUMER antes do settlement não foi adotada: a entrega permanece em execução
até confirmação/erro/cancelamento, e spans CLIENT filhos medirão os settlements em B3/B4.
A espera até a próxima ativação pertence ao broker e fica fora do CONSUMER.

Não declarar B2 completa nem Jaeger fim a fim antes de B2.1–B2.3 e etapas posteriores.
