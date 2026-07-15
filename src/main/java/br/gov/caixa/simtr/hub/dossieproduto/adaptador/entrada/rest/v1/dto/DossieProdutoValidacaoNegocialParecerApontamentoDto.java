package br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DossieProdutoValidacaoNegocialParecerApontamentoDto(
        @JsonProperty("identificador_apontamento")
        @NotNull(message = "O identificador do apontamento deve ser informado.") Long identificadorApontamento,
        @NotNull(message = "O resultado do apontamento deve ser informado.") String resultado,
        String comentario,
        @JsonProperty("necessidade_reanalise")
        @NotNull(message = "A necessidade de reanalise do apontamento deve ser informada.") Boolean necessidadeReanalise,
        @JsonProperty("indice_ia") Double indiceIa
) {
}
