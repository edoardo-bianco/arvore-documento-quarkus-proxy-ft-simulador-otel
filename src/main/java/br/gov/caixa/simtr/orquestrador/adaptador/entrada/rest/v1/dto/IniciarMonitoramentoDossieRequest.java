package br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record IniciarMonitoramentoDossieRequest(
        @NotBlank(message = "O identificador do dossie de pre-validacao deve ser informado.")
        String idDossiePreValidacao,
        @NotBlank(message = "O identificador do dossie MTR deve ser informado.")
        @Pattern(regexp = "\\d+", message = "O identificador do dossie MTR deve conter somente digitos decimais.")
        @DecimalMin(value = "1", message = "O identificador do dossie MTR deve ser positivo.")
        @DecimalMax(value = "9223372036854775807", message = "O identificador do dossie MTR deve ser menor ou igual a 9223372036854775807.")
        String idDossieMtr
) {
}
