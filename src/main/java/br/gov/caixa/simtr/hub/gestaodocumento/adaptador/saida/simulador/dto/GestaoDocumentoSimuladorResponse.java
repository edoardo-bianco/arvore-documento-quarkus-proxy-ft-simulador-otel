package br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.simulador.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GestaoDocumentoSimuladorResponse(
        String sas,
        Object validade,
        @JsonProperty("url_storage") String urlStorage,
        @JsonProperty("nome_container") String nomeContainer
) {
}
