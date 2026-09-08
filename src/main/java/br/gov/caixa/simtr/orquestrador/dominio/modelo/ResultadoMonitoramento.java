package br.gov.caixa.simtr.orquestrador.dominio.modelo;

import java.time.Instant;

/**
 * Resultado proprio do orquestrador, separado do contrato de transporte.
 *
 * <p>A situacao da pre-validacao representa um valor calculado, sem comprovar persistencia.
 * Este tipo transporta a decisao; nao consulta fontes, publica mensagens ou conclui entregas.
 *
 * @param monitoramentoId identidade tecnica do monitoramento
 * @param orquestracaoId identidade tecnica da orquestracao
 * @param idDossiePreValidacao identificador externo da pre-validacao
 * @param idDossieMtr identificador decimal preservado como string
 * @param resultadoMonitoramento classificacao do resultado recebido
 * @param situacaoMtr situacao original consultada no MTR, quando disponivel
 * @param situacaoPreValidacao situacao calculada da pre-validacao
 * @param motivo motivo informado pelo processamento
 * @param tentativasRealizadas quantidade funcional de tentativas realizadas
 * @param iniciadoEm instante original de inicio
 * @param concluidoEm instante de conclusao do monitoramento
 * @param inputSequenceNumber sequencia tecnica da entrega de origem, independente de tentativas
 */
public record ResultadoMonitoramento(
        String monitoramentoId,
        String orquestracaoId,
        String idDossiePreValidacao,
        String idDossieMtr,
        String resultadoMonitoramento,
        String situacaoMtr,
        String situacaoPreValidacao,
        String motivo,
        int tentativasRealizadas,
        Instant iniciadoEm,
        Instant concluidoEm,
        long inputSequenceNumber) {
}
