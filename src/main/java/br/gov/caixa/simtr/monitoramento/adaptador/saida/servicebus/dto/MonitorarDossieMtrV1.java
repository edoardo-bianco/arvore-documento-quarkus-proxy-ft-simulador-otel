package br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

/**
 * Contrato v1 proprio da saida de reagendamento, compativel por JSON com a fila de entrada.
 *
 * <p>Os parametros originais e a tentativa ja decidida sao preservados. A coerencia temporal
 * e validada pelo mapper apos a obrigatoriedade dos campos, antes de serializar.
 *
 * @param schemaVersion versao do contrato, fixada em 1
 * @param monitoramentoId identificador tecnico do monitoramento
 * @param orquestracaoId identificador tecnico da orquestracao
 * @param idDossiePreValidacao identificador externo da pre-validacao
 * @param idDossieMtr decimal positivo compativel com Long, preservado como string e com zeros
 * @param tentativaAtual tentativa funcional positiva, independente do DeliveryCount
 * @param iniciadoEm instante original de inicio
 * @param limiteEm limite original, posterior ao inicio
 * @param politicaMonitoramentoVersao versao original da politica
 */
public record MonitorarDossieMtrV1(
        @Min(1) @Max(1) int schemaVersion,
        @NotBlank String monitoramentoId,
        @NotBlank String orquestracaoId,
        @NotBlank String idDossiePreValidacao,
        @NotBlank @Pattern(regexp = "\\d+") @DecimalMin("1") @DecimalMax("9223372036854775807")
        String idDossieMtr,
        @Positive int tentativaAtual,
        @NotNull Instant iniciadoEm,
        @NotNull Instant limiteEm,
        @NotBlank String politicaMonitoramentoVersao) {
}
