package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RevisaoApontamentoConformidadeRequest(
        @NotNull(message = "O identificador do apontamento deve ser informado.")
        @Positive(message = "O identificador do apontamento deve ser maior que zero.")
        Long identificadorApontamento,
        @NotBlank(message = "O nome do apontamento deve ser informado.")
        String nomeApontamento,
        @NotNull(message = "O parecer do apontamento deve ser informado.")
        ParecerConformidadeDto parecer,
        @NotBlank(message = "A justificativa do apontamento deve ser informada.")
        String justificativa,
        String evidencia,
        @NotNull(message = "A confiança do apontamento deve ser informada.")
        @DecimalMin(
                value = "0.0",
                message = "A confiança do apontamento deve estar entre 0 e 1.")
        @DecimalMax(
                value = "1.0",
                message = "A confiança do apontamento deve estar entre 0 e 1.")
        Double confianca) {
}
