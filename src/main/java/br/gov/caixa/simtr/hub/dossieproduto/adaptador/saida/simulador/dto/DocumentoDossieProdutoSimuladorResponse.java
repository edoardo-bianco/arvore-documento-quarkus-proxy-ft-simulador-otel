package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DocumentoDossieProdutoSimuladorResponse(
        @JsonProperty("id_documento") Long idDocumento,
        @JsonProperty("id_instancia_documento") Long idInstanciaDocumento
) {
}
