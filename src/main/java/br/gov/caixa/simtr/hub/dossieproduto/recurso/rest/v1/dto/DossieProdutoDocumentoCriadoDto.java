package br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DossieProdutoDocumentoCriadoDto(
        @JsonProperty("id_documento") Long idDocumento,
        @JsonProperty("id_instancia_documento") Long idInstanciaDocumento
) {
}
