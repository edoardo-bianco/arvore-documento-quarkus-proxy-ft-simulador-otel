package br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DossieProdutoVinculoClienteDto(
        String cpf,
        String cnpj,
        @JsonProperty("tipo_vinculo") Long tipoVinculo
) {
}
