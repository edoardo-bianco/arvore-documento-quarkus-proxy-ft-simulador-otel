# Especificação: estado, histórico e mensagens do fluxo

## Identidade e documentos

Chave de negócio e partition key: idDossiePreValidacao, preservada como string original.
O container doctree pertence ao database configurado (local: simtr-hub).
monitoramentoId identifica uma execução e orquestracaoId mantém a correlação existente.
Mais de um POST do mesmo dossiê continua possível: consultar por dossiê retorna execuções,
sem escolher silenciosamente uma delas nem sobrescrever a anterior.

Dois formatos físicos no mesmo container/partição:

- EXECUCAO: id = execucao:<monitoramentoId>; snapshot limitado aos valores atuais e referências.
- EVENTO: id = evento:<identidade determinística da operação e fase>; registro imutável.
  Preparação/publicação, consumo, consulta, decisão, agendamento, settlement e observação
  de DLQ são tipos de evento, sem coleções especializadas nem array crescente no snapshot.

O id técnico Cosmos não é a chave de negócio: o par partition key + id identifica o item.
Isso preserva IDs de negócio com caracteres não aceitos no id físico Cosmos. Não normalizar
zeros, espaços internos, maiúsculas ou trocar pelo ID MTR. Limites de tamanho devem ser caracterizados
no adapter P3 antes da ativação; nenhum truncamento ou conversão silenciosa de chave é permitido.

## Campos do snapshot por execução

| Campo/dimensão | Conteúdo e semântica |
|---|---|
| schemaVersion, tipoDocumento | Versão explícita e EXECUCAO |
| idDossiePreValidacao | Chave principal de negócio/partição |
| monitoramentoId, orquestracaoId, idDossieMtr | Identidades da execução e referência MTR, sem confundir suas funções |
| situacaoFluxo | Resumo projetado: EM_ANDAMENTO, AGUARDANDO, CONCLUIDO, QUARENTENA ou INTERROMPIDO; não substitui as situações externas |
| etapaFluxo | Última etapa causal conhecida: PUBLICACAO_ENTRADA, AGUARDANDO_ENTRADA, PROCESSAMENTO_ENTRADA, CONSULTA_PREVALIDACAO, CONSULTA_MTR, REAGENDAMENTO, AGUARDANDO_REAGENDAMENTO, PUBLICACAO_SAIDA, AGUARDANDO_SAIDA, REGISTRO_RESULTADO ou ENCERRAMENTO |
| iniciadoEm, atualizadoEm, versao | Datas e revisão de projeção; relógio sozinho não ordena transições concorrentes |
| tentativaAtual, proximaTentativa, tentativasRealizadas | Separadas de delivery count; reagendamento não se confunde com redelivery |
| preValidacao.situacaoObservada, consultadaEm | Valor efetivamente devolvido pela consulta ao Pré-Valida |
| preValidacao.situacaoCalculada | Resultado calculado pelo monitoramento; não confirma alteração no sistema externo |
| mtr.situacaoObservada, consultadaEm | Situação efetivamente consultada no MTR/Hub, incluindo sua origem simulada/real |
| quarentena.motivo, definidaEm | Código funcional de motivo da quarentena; separado de erro técnico/DLQ |
| entrada, reagendamento, saida | Dimensões independentes com mensagem, tentativa, sequência conhecida, fase, fila e confirmação |
| resultado | Tipo, motivo e situação resultante já decididos pelo monitoramento; acompanhamento não recalcula a política |
| pendenciasConfirmacao | Resumo limitado: quantidade e indicador de pendência, com última referência aplicável em cada dimensão fixa; lista completa consultada nos EVENTO |
| ultimoEventoRegistradoId, ultimoEventoAplicadoId, traceId, spanId | Registro e avanço da projeção são distintos; evento atrasado pode ser registrado sem avançar o fluxo; referências não reconstruem parentage |

Cada dimensão de mensagem guarda: messageId, fila lógica, nome da entidade, localização
observada, observadaEm, fonte, deliveryCount/sequenceNumber quando conhecidos e referência
ao evento que contém o corpo. As dimensões e suas referências têm quantidade fixa; histórico,
mensagens anteriores e lista de pendências ficam nos EVENTO, sem arrays crescentes no snapshot.
Agendamento guarda agendadaPara e sequência agendada, sem inventar sequência para send normal,
cujo ACK não a retorna.

Localizações possíveis por mensagem: ENTRADA, SAIDA, ENTRADA_DLQ, SAIDA_DLQ ou NAO_CONFIRMADA.
Estado da mensagem é separado: PREPARADA, PUBLICADA, EM_PROCESSAMENTO, AGENDADA, COMPLETADA,
ABANDONADA, EM_DLQ ou CONFIRMACAO_PENDENTE. Mensagem completada permanece no histórico,
mas não é anunciada como presente na fila. Mais de uma localização pode coexistir na execução.

## Evento e mensagem persistida

Evento contém: tipo/fase, operationId, identidades, tentativa, sequência/delivery count,
fila, situação/etapa anterior e nova quando aplicáveis, resultado/motivo, ocorridoEm,
registradoEm, fonte e traceId/spanId. Eventos de entrega diferentes permanecem distintos.
A idempotência do registro não promete deduplicação de todos os efeitos funcionais.

O evento de preparação armazena uma cópia exata da mensagem gerada para envio:

- corpo serializado como string, exatamente o submetido ao SDK, e SHA-256 para confronto;
- messageId, correlationId, subject, contentType, fila destino e horário de agendamento;
- somente application properties W3C que o próprio fluxo inclui (traceparent/tracestate).

A confirmação referencia a preparação, evitando replicar payload a cada mudança. Mesmo
operationId/fase com conteúdo diferente é conflito, nunca upsert silencioso. Não persistir
credencial, connection string, lock token, objeto SDK, Throwable ou dump arbitrário de headers.
A permissão humana de guardar a mensagem no Cosmos não amplia os campos permitidos em logs
ou spans: corpos e novos IDs de negócio não são exportados para Jaeger/log.

## Fatos que autorizam cada mudança

| Fato observado | Registro permitido |
|---|---|
| Intenção Cosmos confirmada, envio ainda não confirmado | PUBLICACAO_ENTRADA/SAIDA; mensagem PREPARADA; localização NAO_CONFIRMADA |
| ACK de send | Mensagem PUBLICADA na fila destino, com fonte ACK_BROKER e instante |
| Entrega PEEK_LOCK recebida | EM_PROCESSAMENTO naquela fila; delivery count/sequence e nova entrega |
| Consulta Pré-Valida/Hub concluída | Situação consultada e instante; cenário/origem real ou simulada |
| Decisão funcional | Ignorar, resultado conclusivo, reagendar ou quarentena/motivo, sem repetir regras no acompanhamento |
| ACK de schedule e Complete em transação, sem commit | Operação ainda pendente; não afirmar próximo item AGENDADO nem entrada removida |
| Commit confirmado | Entrada completada e próxima mensagem agendada, com horário/seq conhecidos |
| Rollback confirmado | Transação desfeita; não confirmar agendamento/remoção da entrada |
| Commit/settlement com resposta ambígua | CONFIRMACAO_PENDENTE; não reenviar, reagendar ou liquidar novamente por dedução |
| Complete confirmado | Aquela entrega concluída; histórico conserva fila/identidade anteriores |
| DeadLetter explícito confirmado | ENTRADA_DLQ ou SAIDA_DLQ e motivo controlado da aplicação |
| Peek real encontra mensagem em DLQ | Localização DLQ observada e motivo do broker; ocorridoEm do movimento só quando disponível, observadaEm sempre |
| Saída processada e log submetido | REGISTRO_RESULTADO; não declarar garantia de gravação pelo handler de log (C9.1-L) |
| Confirmações finais conhecidas | ENCERRAMENTO do acompanhamento daquela execução; nenhuma pendência operacional ocultada |

A entrada pode aguardar Complete enquanto a saída já está publicada/consumida. Dimensões
independentes preservam esse intervalo. Uma atualização atrasada da entrada não regride
uma etapa de saída já conhecida. Atualizações de outra execução não alteram este snapshot.
Encerramento exige somente as confirmações aplicáveis à decisão: uma entrada ignorada pode
encerrar após seu Complete, sem inventar publicação/consumo de saída ou log de resultado.

## Concorrência e consistência

Evento imutável create-only e nova projeção são gravados em batch na mesma partição Cosmos.
ETag protege a projeção; conflito exige releitura/redução condicionada, com tentativas
limitadas definidas no recorte de implementação, sem loop infinito ou last-write-wins.
Idempotência compara identidade e conteúdo. O avanço considera execução, tentativa,
identidade/sequência da mensagem e fase dentro de cada dimensão; não compara cegamente
sequências de filas diferentes nem usa apenas timestamps. Evento atrasado ainda integra o histórico.

Não existe transação Cosmos+Service Bus. Diário da intenção confirmado antes do efeito;
confirmação do efeito gravada depois do ACK/commit. Se o registro anterior falhar, não executar
o efeito protegido. Se o registro posterior falhar, preservar o resultado do ACK e não
acionar segundo settlement/envio. A preparação durável continua apontando a confirmação
pendente. Falha do Cosmos não produz fallback automático para memória.

Consequências obrigatórias para os chamadores quando somente a confirmação Cosmos falha:

- POST mantém HTTP 202 após ACK de publicação; não converte esse sucesso em HTTP 500.
- Publicação de saída confirmada mantém seu sucesso; a falha posterior do Cosmos não provoca
  Abandon da entrada, nova publicação ou mudança da decisão funcional.
- Complete ou commit confirmado mantém sucesso, sem segundo settlement, nova transação ou
  sinalização de que o efeito confirmado falhou apenas porque a confirmação não foi gravada.
- Um diagnóstico seguro registra a falha de confirmação; a intenção durável permanece
  consultável como pendente. Não declarar que essa confirmação ficou persistida.

Falha de intenção antes do efeito pode propagar como falha da operação e impede o efeito.
Essas regras não suprimem falhas reais do broker nem confundem ACK conhecido com resposta ambígua.

O registro de intenção não é um outbox com dispatcher. Reinício identifica pendências;
reconciliação só atualiza por evidência positiva do broker, da execução ou evidência operacional.
Ausência na fila não prova que Complete aconteceu. Não prometer reconstituição perfeita de
um ACK cuja confirmação foi perdida por crash. Reenvio/reprocessamento exigem ação própria.

## DLQ e consulta de localização

Observar entrada, saída e respectivas subfilas por peek, sem receive/Complete/abandon ou
republicação. Paginar por sequência com limite e horário; registrar varredura parcial/falha.
Peek não comprova lock e pode incluir mensagens expiradas ainda não removidas. Consulta
não afirma ausência definitiva nem altera fila apenas porque não encontrou a mensagem.

DLQ explícita e automática (por exemplo MaxDeliveryCountExceeded ou TTLExpiredException)
são reconhecidas por mensagem. Persistir motivo do broker como dado, com limite/truncamento
explícito de descrição quando aplicável; não executar ou ecoar esse texto em log/trace.
Quarentena guarda o motivo funcional e não implica presença em DLQ.

Mensagem sem identidade confiável não pode ser vinculada arbitrariamente a um dossiê.
Tentar associação por messageId da mensagem já registrada; se não for possível, expor
limitação de correlação no diagnóstico, sem fabricar ID de negócio ou inserir payload
inválido de terceiros no histórico de outro dossiê.

## Local e DES

Local: Dev Services da extensão, bootstrap idempotente de simtr-hub/doctree, partição
/idDossiePreValidacao, dados sintéticos e testes reais de CRUD/batch/ETag. O emulador criado
pela extensão é descartável; recriar o componente com o banco ativo comprova que estado
não depende da memória do processo. Remover o emulador não é cenário de durabilidade do banco.

DES: endpoint/database/credencial externos, container doctree preexistente, Dev Services
Cosmos desabilitado, TLS válido e ausência do bypass local. Sem provisionar conta/recursos
reais ou copiar dados para Azure durante o desenvolvimento desta feature.

## Correlação homogênea com Jaeger e logs

[Contrato transversal da feature](observabilidade-homogenea.md): eventos guardam o par
traceId/spanId da operação acompanhada; o Jaeger fornece esse mesmo par para consultar
seus detalhes no Cosmos. Escritas/consultas Cosmos têm spans CLIENT filhos, sem substituir
a referência da operação de negócio. Logs mantêm os nomes técnicos atuais. A consulta por
trace pode localizar várias execuções; a leitura seguinte usa partição e monitoramentoId.
A implementação integrada e a navegação real em P10 são obrigatórias para considerar a
rastreabilidade concluída. Esta seção não afirma que a correlação já está implementada.