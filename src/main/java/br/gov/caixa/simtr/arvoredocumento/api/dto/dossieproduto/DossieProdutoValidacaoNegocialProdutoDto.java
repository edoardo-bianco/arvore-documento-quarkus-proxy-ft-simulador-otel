package br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DossieProdutoValidacaoNegocialProdutoDto(
        @JsonProperty("codigo_operacao")
        @NotNull(message = "O codigo da operacao do produto deve ser informado.") Integer codigoOperacao,
        @JsonProperty("codigo_modalidade")
        @NotNull(message = "O codigo da modalidade do produto deve ser informado.") Integer codigoModalidade
) {
}
