package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RevisaoAnaliseConformidadeRequest(
        String observacao,
        @NotEmpty(message = "Os apontamentos da revisão devem ser informados.")
        List<
                @NotNull(message = "O apontamento da revisão deve ser informado.")
                @Valid RevisaoApontamentoConformidadeRequest> apontamentos) {
}
