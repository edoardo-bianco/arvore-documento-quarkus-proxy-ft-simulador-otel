# Observabilidade homogênea: Jaeger, logs e Cosmos

## Requisito humano e alcance

Em 2026-09-12, após o GO do ADR-0013, o usuário pediu estratégia homogênea com spans
rastreáveis no Jaeger e referência ao Cosmos para consultar o detalhe do que acontece.
Este documento torna obrigatória a mesma correlação em P3 e P5–P10. Não cria endpoint,
console ou novo barramento. P1 continua sendo a validação da extensão e do emulador.

## Uma correlação em todas as bordas

O contexto W3C traceparent/tracestate percorre HTTP, entrada, processamento/Hub, agendamento,
saída e registro final. Cada etapa cria seu span no contexto causal correto e registra o
mesmo par traceId/spanId em seus logs e nos fatos correspondentes do Cosmos. Um evento pode
referenciar a operação de negócio, enquanto a escrita Cosmos tem seu span CLIENT filho:
a latência da persistência não deve substituir a identidade da operação acompanhada.

traceId identifica o trace; spanId identifica sua operação; monitoramentoId identifica
a execução durável; idDossiePreValidacao identifica o dossiê/partição do Cosmos. Não usar
uma dessas identidades como substituta da outra. O mesmo dossiê pode ter várias execuções
e uma execução pode ter traces adicionais de redelivery sem carrier ou observação posterior.

O par traceId/spanId já apresentado pelo Jaeger é a referência de busca dos eventos no
container doctree. Não é necessário colocar o ID de negócio ou a partition key no span.
Nos logs, manter os nomes já adotados: traceId, spanId, monitoramento_id, orquestracao_id,
message_id e tentativa_atual, quando conhecidos e válidos. No Cosmos, os campos existentes
traceId/spanId e monitoramentoId fazem essa ligação sem renomear o contrato dos logs.

Nomes/kinds e tipos de erro do fluxo seguem C0.4/B1–B5. Nenhum componente inventa outro
carrier, usa MDC como fonte de parentage ou abre uma raiz por conveniência. Spans de operações
Cosmos seguem a convenção de banco e são definidos/validados no adapter P3, com inventário
para impedir duplicação entre SDK e instrumentação da aplicação. Os domínios não importam
SDK OpenTelemetry: metadados técnicos atravessam somente contratos próprios das bordas.

## Matriz obrigatória

| Etapa | Span no Jaeger | Detalhe relacionado no Cosmos |
|---|---|---|
| POST aceito e iniciação | SERVER e INTERNAL existentes | Execução, início e identidade do dossiê |
| Publicação de entrada | PRODUCER de send | Mensagem exata preparada, destino e ACK/pendência |
| Leitura da entrada | CONSUMER por entrega | Fila, tentativa, delivery count e sequência |
| Pré-Valida e MTR/Hub | INTERNAL/CLIENT conforme a borda | Situações consultadas, origem e horário |
| Decisão funcional | INTERNAL da avaliação | Resultado, situação calculada, quarentena e motivo |
| Reagendamento | PRODUCER/CLIENT de schedule e transação | Mensagem, data/seq e confirmação somente após commit |
| Publicação da saída | PRODUCER de send | Mensagem exata, destino e ACK/pendência |
| Leitura da saída e registro | CONSUMER e operação de registro/settlement | Resultado, log submetido e Complete distinguíveis |
| Persistência ou consulta Cosmos | CLIENT filho da operação solicitante | Evidência técnica da gravação/consulta, sem reproduzir corpo no span |
| Observação de fila/DLQ | Operação de observação e CLIENT de peek | Localização vista, fonte, horário e motivo da DLQ |

Um movimento automático para DLQ só pode ser registrado quando observado. Seu span é o
da observação atual; não fabricar um span retrospectivo no instante desconhecido da movimentação.
O observador conserva a relação com a execução encontrada, sem fingir contexto ativo da entrega.

## Navegação Jaeger → Cosmos → Jaeger

Partindo de um trace Jaeger, consultar os eventos pelo traceId; para uma etapa exata,
acrescentar spanId. Exemplo parametrizado, a executar pelo mecanismo de consulta do Cosmos:

```sql
SELECT c.id, c.idDossiePreValidacao, c.monitoramentoId, c.traceId, c.spanId,
       c.tipo, c.fase, c.ocorridoEm, c.registradoEm
FROM c
WHERE c.tipoDocumento = 'EVENTO' AND c.traceId = @traceId
```

Essa busca inicial pode cruzar partições. O resultado fornece a chave de negócio e as
execuções; não escolher silenciosamente uma execução se houver mais de uma. A leitura do
snapshot usa partition key idDossiePreValidacao e id execucao:<monitoramentoId>. A partir daí,
consultar histórico/mensagens dentro dessa partição e execução, com paginação. Para o filtro
da etapa, usar AND c.spanId = @spanId. A interface pública REST permanece fora do escopo.

No caminho inverso, cada evento com contexto válido permite abrir /trace/<traceId> na URL
configurada do Jaeger (local: http://localhost:16686) e localizar o span pelo spanId. Não
persistir URL fixa de um ambiente no documento nem inventar link do Azure Portal sem dados.
As consultas acima são o contrato de navegação planejado; serão verificadas após P3/P5.

## Falhas, espera e comprovação

- Sem carrier válido, criar nova raiz real e conservar monitoramentoId para a continuidade
  durável. Não fabricar traceId/spanId a partir do Cosmos ou de identificadores de negócio.
- Durante espera por reagendamento, o Cosmos mostra AGUARDANDO e agendadaPara; o span da
  entrega anterior fica encerrado. Não manter span artificialmente aberto durante horas.
- Se a confirmação Cosmos falhar depois de ACK, o span da gravação mostra falha controlada,
  o resultado funcional confirmado é preservado e a intenção continua pendente no banco.
- Corpo, envelope permitido, situações e motivos detalhados permanecem no Cosmos. Logs/spans
  conservam somente os campos autorizados, sem credencial, payload ou texto bruto de erro.
- Persistência de eventos não depende de amostragem ou disponibilidade do Jaeger. Um contexto
  válido pode não ter span exportado; não declarar o link disponível sem conferir o backend.
- Prova local usa amostragem integral e compara trace exportado, log e evento pelo mesmo par.
  Cada caminho aplicável deve navegar nos dois sentidos, inclusive falha, reagendamento e DLQ.

## Critérios adicionados aos incrementos

P3 valida spans de I/O e consulta por referências; P5–P8 provam o par do span da operação
nos fatos/logs correspondentes; P9 preserva identidade e tempo da observação. P10 verifica
Jaeger real e consulta dos documentos: não basta exporter em memória, log isolado ou um
trace sem seus detalhes duráveis. Nenhuma referência nova fica apenas decorativa.

Fontes de nomes/kinds: desenhos B1/B2 e C0.4 do repositório;
[OpenTelemetry Messaging](https://opentelemetry.io/docs/specs/semconv/messaging/messaging-spans/)
e [Cosmos DB](https://opentelemetry.io/docs/specs/semconv/db/cosmosdb/). Convenções Cosmos
estão em Development: o adapter deverá fixar o recorte aplicável ao SDK efetivo e verificar
seus sinais, sem mudar silenciosamente os sinais anteriores do Hub.