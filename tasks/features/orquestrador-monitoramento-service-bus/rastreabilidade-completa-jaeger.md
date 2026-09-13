# Rastreabilidade completa e operação com Jaeger — 2026-09-12

## CP-COSMOS recebido; P1 em validação — 2026-09-12

GO humano: "GO para o desenho e implementação incremental", reiterado "GO ao ADR-0013".
ADR-0013 Aceito; o registro anterior de espera está superado. P1 adicionou a extensão,
perfis local/DES e teste opt-in; quatro provas Cosmos aprovadas, incluindo ETag/batch/rollback.
Regressão conjunta e checkpoint em andamento. Ainda não existe persistência operacional
nem rastreamento completo. [Execução P1](../rastreabilidade-fluxo-dossie-cosmos/execucao-p1.md).
B2–B5 continuam coordenados com P6–P8. Sem staging, commit ou push.

## Ampliação humana: Cosmos DB, situações, mensagens e DLQ — 2026-09-12

O usuário confirmou estado durável em Cosmos DB, extensão Quarkus, Dev Services local e
configuração posterior do Cosmos real de DES. Container novo doctree; chave de negócio
idDossiePreValidacao. Incluir situação/etapa, mudanças, mensagens enviadas, filas, agendamento,
situações Pré-Valida/MTR, quarentena e DLQs com motivos.
[Plano específico](../rastreabilidade-fluxo-dossie-cosmos/plan.md),
[dados/transições](../rastreabilidade-fluxo-dossie-cosmos/especificacao.md) e ADR-0013 Aceito após o GO humano.
A alternativa inicial sem banco foi superada pelo pedido humano. O GO posterior aprovou
arquitetura e consistência; a extensão e o gate local foram implementados em P1.
Persistência operacional e trace completo continuam pendentes no checklist Cosmos.

Complemento B1.5: DevServicesTest e A1 ajustados para preservar lifecycle de receive/peek;
10 repetições DevServices e 13 focados aprovados. Regressão final: 41 testes/11 classes,
zero falhas/erros/ignorados. Checkpoint final COMPLIANT: 1.403 testes padrão/194 classes,
213 issues (0 novas/graves), cobertura 88,3% e duplicação 4,4%; baseline original preservado.
B1.5 e B1 consolidados tecnicamente. [Evidência final](execucao-10-1-b1-5.md#complemento-final-da-regressão-e-consolidação-técnica--2026-09-12).
A revisão do desenho Cosmos terminou sem bloqueadores e o CP-COSMOS humano foi recebido; seguir o checklist Cosmos vigente.
Os blocos posteriores preservam marcos históricos e suas pendências à época; este estado prevalece.
B2–B5 permanecem pendentes e serão coordenados com P6–P8 do novo plano. Sem commit/push.


## Pedido e autorização existente

Pedido humano: habilitar o Jaeger e fazer o necessário para rastrear o dossiê, identificar
as filas pelas quais passou e investigar encaminhamento à Dead Letter.
C0.4 e CP-B1 já autorizam os sinais documentados. A execução continua incremental,
sem commit/push, preservando alterações locais e baseline Sonar original.
Detalhes novos de contrato público, capacidade transversal ou segurança exigem desenho
concreto e checkpoint antes da implementação afetada, conforme AGENTS.md.

## Perguntas operacionais e aceitação

1. Qual POST iniciou este monitoramento e qual foi o último passo confirmado?
   Trace único atravessa POST, envio, entrega, processamento, Hub, decisão, saída e log.
2. Houve reagendamento ou reentrega? Quando a próxima tentativa foi agendada?
   Spans distintos por entrega/agendamento, tentativa funcional e delivery count separados;
   confirmação transacional só depois do commit, sem span aberto durante espera na fila.
3. A mensagem foi completada, abandonada ou enviada à DLQ? Por qual motivo controlado?
   Spans de settlement refletem confirmação/erro/cancelamento. Quarentena funcional não
   se confunde com DLQ. Movimento automático pelo broker não recebe span fictício da aplicação.
4. Em qual fila ou DLQ a mensagem foi observada agora?
   Consulta de leitura limitada às duas filas e respectivas DLQs, com instante e limites
   da varredura. Peek é snapshot: não prova lock nem processamento; ausência não prova conclusão.

Jaeger já responde em http://localhost:16686 e existe profile jaeger com exportação OTLP.
É necessário validar exportação real e a árvore completa, além dos testes em memória.
Não recriar nem atualizar containers existentes para realizar essa validação.

## Ordem de execução e checkpoints

- B1.5 complementar: corrigir somente ServiceBusDevServicesTest para validar identidade e
  aguardar Complete dentro de concatMap, antes de next cancelar o receive. RED já evidenciado
  na suíte opt-in de 2026-09-12; validar repetição focada, suíte opt-in e checkpoint.
- B2: desenhar e implementar contexto por entrega da entrada, avaliação e pré-validação;
  manter nomes C0.4 e fazer o wrapper existente do Hub pertencer à cadeia.
- B3: publicar resultado com W3C e medir o Complete da entrada; confirmação/erro reais.
- B4: propagar pelo agendamento transacional e próxima entrega; preservar commit, rollback
  e cancelamento. Dividir em subfatias se necessário.
- B5: extrair na saída, registrar resultado/log correlacionados e observar settlement.
- C: testes integrados dos dois fluxos, no-op, falhas, reentrega e DLQ nas duas bordas;
  isolamento entre mensagens, sentinelas e inventário integral da captura.
- J: executar aplicação com profile jaeger, gerar casos sintéticos e comprovar trace
  exportado pela API e interface do Jaeger. Documentar comandos e busca por IDs técnicos.
- Q: detalhar diagnóstico de localização e DLQ somente leitura, com estado durável Cosmos conforme o novo plano;
  respeitar a escolha humana por Cosmos e submeter qualquer novo contrato antes de expô-lo.

Cada incremento tem até cinco arquivos executáveis incluindo testes; desenho antes do RED,
revisão e checkpoint Sonar ao fechar. Não atribuir resultado antigo a fingerprint novo.
O fechamento técnico não registra encerramento/aceitação humana C3.

## Limites, dependências e arquivos prováveis

Extensão/persistência Cosmos passam ao plano específico. Sem novo retry, timeout de produção ou alteração de contratos funcionais,
consumo destrutivo ou reprocessamento de DLQ. Identificadores de negócio, payload, credenciais,
namespace, Throwable externo e baggage não entram nos sinais novos. Identidades técnicas
permitem correlacionar monitoramento, mensagem e trace. Não criar helper transversal antecipado.

Arquivos B2–B5: listeners de entrada/saída; ProcessarTentativaMonitoramentoUseCase;
PreValidacaoSimuladaAdapter; MonitoramentoResultadoPublisher; MonitoramentoReagendamentoAdapter;
ReceberResultadoMonitoramentoUseCase; adaptadores de log e testes diretamente relacionados.
Perfis/roteiro Jaeger e diagnóstico Q recebem recorte concreto antes de qualquer edição executável.

Dependências: emulador/SQL por Dev Services, Jaeger local existente, token Sonar herdado,
baseline original READY e checkpoint B1.5 COMPLIANT conferidos antes desta continuidade.

## Riscos e fontes oficiais

Jaeger registra história observada, não é cadastro de estado atual do broker. A retenção
configurada limita consultas históricas. DLQ automática por TTL/MaxDeliveryCount precisa de
observação do broker; a aplicação pode não executar no instante da movimentação.
Peek pagina por sequência e pode mostrar mensagens agendadas, bloqueadas ou expiradas;
resultado parcial/timeouts devem ser explícitos, sem declarar ausência definitiva.

- [Quarkus Azure Services](https://docs.quarkiverse.io/quarkus-azure-services/dev/index.html)
  e [Service Bus](https://docs.quarkiverse.io/quarkus-azure-services/dev/quarkus-azure-servicebus.html).
- [Quarkus OpenTelemetry 3.33](https://quarkus.io/version/3.33/guides/opentelemetry-tracing/).
- [Peek e seus limites](https://learn.microsoft.com/en-us/azure/service-bus-messaging/message-browsing).
- [Dead Letter e motivos do broker](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-dead-letter-queues).

## Execução

Desenho registrado antes do ajuste complementar B1.5. B2–B5/J/Q ainda não implementados.

### Estabilização complementar A1 — 2026-09-12

Após a correção de ServiceBusDevServicesTest, dez repetições passaram. A suíte ampla
executou 41 cenários: 40 passaram e um erro ocorreu em ServiceBusTelemetriaEmuladorTest,
ramo ROLLBACK, no peek final (linha 187 no estado anterior). Rollback/ACK já haviam
terminado; receive.take(observacao) havia cancelado o link antes do management peek.
A mensagem de fechamento por 300000 ms é relato do broker, não causa interna comprovada.

Recorte adicional necessário à base de validação: somente esse arquivo A1. Mover a
prova de fila vazia de receberECompletar para depois do ACK e antes de next; no ramo
rollback, o companion de takeUntilOther espera a janela, executa peek/assert e somente
então cancela o recebimento. Remover peeks finais redundantes depois desse cancelamento.
Manter identidade antes de Complete, os clientes reais, timeouts, janela e ausência de
mensagens posteriores. Não criar cliente, retry ou sleep compensatório. Revisão independente
confirmou o local e propôs essa ordenação. Peeks de início entre cenários permanecem um
limite conhecido a observar; não afirmar teardown assíncrono comprovado.

O complemento B1.5 agora abrange dois arquivos de teste (DevServices e A1). Produção
permanece intacta. B2 só começa após regressão e checkpoint deste recorte coerente.
