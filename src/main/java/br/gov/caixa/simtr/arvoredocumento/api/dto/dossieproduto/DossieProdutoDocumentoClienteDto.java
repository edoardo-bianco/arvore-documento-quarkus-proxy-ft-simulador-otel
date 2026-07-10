package br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DossieProdutoDocumentoClienteDto(
        String cpf,
        String cnpj,
        @JsonProperty("tipo_vinculo") Long tipoVinculo
) {
}
