package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CapturaDossieProdutoSimuladorResponse(
        @JsonProperty("id") Long id
) {
}
