package br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

/**
 * Contrato v1 independente da borda de resultado do monitoramento.
 *
 * <p>Situacao da pre-validacao calculada, sem representar persistencia. Campos e valores
 * recebidos sao preservados; o envelope AMQP e a coerencia temporal pertencem ao mapper.
 *
 * @param schemaVersion versao fixa do contrato
 * @param monitoramentoId identidade do monitoramento
 * @param orquestracaoId identidade da orquestracao
 * @param idDossiePreValidacao identificador externo da pre-validacao
 * @param idDossieMtr identificador decimal positivo compativel com Long, sem normalizacao
 * @param resultadoMonitoramento classificacao do resultado
 * @param situacaoMtr situacao original consultada, quando disponivel
 * @param situacaoPreValidacao situacao calculada da pre-validacao
 * @param motivo motivo do resultado
 * @param tentativasRealizadas contador nao negativo; zero permitido na quarentena sem consulta
 * @param iniciadoEm inicio original do monitoramento
 * @param concluidoEm conclusao do monitoramento
 * @param inputSequenceNumber sequencia tecnica da entrega original, preservada como long
 */
public record ResultadoMonitoramentoDossieMtrV1(
        @Min(1) @Max(1) int schemaVersion,
        @NotBlank String monitoramentoId,
        @NotBlank String orquestracaoId,
        @NotBlank String idDossiePreValidacao,
        @NotBlank @Pattern(regexp = "\\d+") @DecimalMin("1") @DecimalMax("9223372036854775807")
        String idDossieMtr,
        @NotBlank String resultadoMonitoramento,
        String situacaoMtr,
        @NotBlank String situacaoPreValidacao,
        @NotBlank String motivo,
        @Min(0) int tentativasRealizadas,
        @NotNull Instant iniciadoEm,
        @NotNull Instant concluidoEm,
        long inputSequenceNumber) {

    /** Verifica a evidencia de consulta conclusiva sem consultar MTR ou recalcular estados. */
    @AssertTrue
    @JsonIgnore
    public boolean isConsultaConclusivaValida() {
        return !"CONCLUSIVO".equals(resultadoMonitoramento)
                || tentativasRealizadas >= 1 && switch (situacaoMtr) {
                    case "CONFORME", "NAO_CONFORME", "PENDENTE_INFORMACAO",
                            "FINALIZADO_CONFORME", "FINALIZADO_INCONFORME", "PENDENTE_INFORMACA" -> true;
                    case null, default -> false;
                };
    }
}
