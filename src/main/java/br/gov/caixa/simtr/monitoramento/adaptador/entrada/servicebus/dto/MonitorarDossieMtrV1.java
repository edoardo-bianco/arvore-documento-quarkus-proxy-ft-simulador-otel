package br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebus.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

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
