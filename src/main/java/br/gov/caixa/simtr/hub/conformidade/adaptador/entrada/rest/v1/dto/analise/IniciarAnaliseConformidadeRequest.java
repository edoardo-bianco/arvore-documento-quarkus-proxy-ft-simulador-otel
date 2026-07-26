package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record IniciarAnaliseConformidadeRequest(
        @NotBlank(message = "O identificador do documento deve ser informado.")
        String identificadorDocumento,
        @NotBlank(message = "O texto deve ser informado.")
        @Size(
                max = 20_000,
                message = "O texto deve possuir no máximo 20000 caracteres.")
        String texto,
        @NotNull(message = "O identificador do checklist deve ser informado.")
        @Positive(message = "O identificador do checklist deve ser maior que zero.")
        Long identificadorChecklist,
        @NotNull(message = "A versão do checklist deve ser informada.")
        @Positive(message = "A versão do checklist deve ser maior que zero.")
        Integer versaoChecklist) {
}
