package br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DossieProdutoValidacaoNegocialDto(
        List<@Valid DossieProdutoValidacaoNegocialVerificacaoDto> verificacoes,
        @JsonProperty("respostas_formulario") List<@Valid DossieProdutoValidacaoNegocialRespostaFormularioDto> respostasFormulario
) {
}
