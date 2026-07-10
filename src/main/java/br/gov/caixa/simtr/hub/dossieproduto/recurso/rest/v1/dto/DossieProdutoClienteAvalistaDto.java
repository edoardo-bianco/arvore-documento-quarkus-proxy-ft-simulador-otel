package br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DossieProdutoClienteAvalistaDto(
        String cpf,
        String cnpj
) {
}
