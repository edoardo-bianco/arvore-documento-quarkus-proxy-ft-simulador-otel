package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.captura;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CapturaDossieProdutoMtrResponse(
        @JsonProperty("id") Long id
) {
}
