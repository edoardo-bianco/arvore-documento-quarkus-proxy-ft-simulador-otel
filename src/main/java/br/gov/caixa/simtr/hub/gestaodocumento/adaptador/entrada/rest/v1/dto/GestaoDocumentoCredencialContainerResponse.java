package br.gov.caixa.simtr.hub.gestaodocumento.adaptador.entrada.rest.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GestaoDocumentoCredencialContainerResponse(
        String sas,
        Object validade,
        @JsonProperty("url_storage") String urlStorage,
        @JsonProperty("nome_container") String nomeContainer
) {
}
