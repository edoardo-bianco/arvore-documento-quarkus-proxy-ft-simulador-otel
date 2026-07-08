package br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DossieProdutoClienteDto(
        String cpf,
        String cnpj,
        @JsonProperty("tipo_vinculo") Long tipoVinculo,
        @JsonProperty("cliente_relacionado") @Valid DossieProdutoClienteRelacionadoDto clienteRelacionado,
        @JsonProperty("sequencia_titularidade") Integer sequenciaTitularidade
) {
}
