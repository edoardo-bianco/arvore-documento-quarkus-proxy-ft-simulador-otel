package br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DossieProdutoValidacaoNegocialGarantiaDto(
        @JsonProperty("codigo_bacen") Integer codigoBacen,
        @JsonProperty("clientes_avalistas") List<@Valid DossieProdutoValidacaoNegocialClienteAvalistaDto> clientesAvalistas
) {
}
