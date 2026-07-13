package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v2.documento;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentoDossieProdutoMtrResponse(
        @JsonProperty("id_documento") Long idDocumento,
        @JsonProperty("id_instancia_documento") Long idInstanciaDocumento
) {
}
