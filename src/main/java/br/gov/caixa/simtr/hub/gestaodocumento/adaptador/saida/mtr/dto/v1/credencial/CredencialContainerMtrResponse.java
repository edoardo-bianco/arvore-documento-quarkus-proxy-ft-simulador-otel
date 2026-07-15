package br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.mtr.dto.v1.credencial;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CredencialContainerMtrResponse(
        String sas,
        Object validade,
        @JsonProperty("url_storage") String urlStorage,
        @JsonProperty("nome_container") String nomeContainer
) {
}
