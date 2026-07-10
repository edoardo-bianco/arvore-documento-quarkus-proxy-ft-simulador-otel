package br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DossieProdutoValidacaoNegocialRespostaFormularioDto(
        @JsonProperty("campo_formulario")
        @NotNull(message = "O campo do formulario deve ser informado.") Long campoFormulario,
        String resposta,
        @JsonProperty("opcoes_selecionadas") List<String> opcoesSelecionadas
) {
}
