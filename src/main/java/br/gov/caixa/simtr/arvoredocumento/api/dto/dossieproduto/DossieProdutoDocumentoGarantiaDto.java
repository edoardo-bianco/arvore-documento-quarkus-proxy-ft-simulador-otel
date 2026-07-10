package br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DossieProdutoDocumentoGarantiaDto(
        @JsonProperty("codigo_bacen") Integer codigoBacen,
        @JsonProperty("produto_operacao") Integer produtoOperacao,
        @JsonProperty("produto_modalidade") Integer produtoModalidade,
        @JsonProperty("cliente_avalista") @Valid List<DossieProdutoDocumentoClienteDto> clienteAvalista
) {
}
