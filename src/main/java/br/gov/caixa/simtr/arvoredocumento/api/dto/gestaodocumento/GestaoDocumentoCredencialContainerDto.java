package br.gov.caixa.simtr.arvoredocumento.api.dto.gestaodocumento;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GestaoDocumentoCredencialContainerDto(
        String sas,
        Object validade,
        @JsonProperty("url_storage") String urlStorage,
        @JsonProperty("nome_container") String nomeContainer
) {
}
