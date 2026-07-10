package br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DossieProdutoRespostaFormularioDto(
        @JsonProperty("campo_formulario") Long campoFormulario,
        String resposta,
        @JsonProperty("opcoes_selecionadas") List<String> opcoesSelecionadas,
        Boolean excluir
) {
}
