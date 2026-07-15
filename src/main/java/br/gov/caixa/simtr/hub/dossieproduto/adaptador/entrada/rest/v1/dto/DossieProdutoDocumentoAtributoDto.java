package br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DossieProdutoDocumentoAtributoDto(
        @NotNull(message = "A chave do atributo do documento deve ser informada.") String chave,
        @NotNull(message = "O valor do atributo do documento deve ser informado.") String valor,
        String objeto,
        @JsonProperty("opcoes_selecionadas") List<String> opcoesSelecionadas
) {
}
