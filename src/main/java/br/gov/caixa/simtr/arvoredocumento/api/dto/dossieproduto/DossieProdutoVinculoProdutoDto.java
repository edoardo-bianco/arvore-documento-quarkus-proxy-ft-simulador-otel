package br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DossieProdutoVinculoProdutoDto(
        @JsonProperty("codigo_operacao") Integer codigoOperacao,
        @JsonProperty("codigo_modalidade") Integer codigoModalidade
) {
}
