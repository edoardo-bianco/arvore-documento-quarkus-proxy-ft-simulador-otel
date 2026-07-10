package br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DossieProdutoDocumentoCriadoDto(
        @JsonProperty("id_documento") Long idDocumento,
        @JsonProperty("id_instancia_documento") Long idInstanciaDocumento
) {
}
