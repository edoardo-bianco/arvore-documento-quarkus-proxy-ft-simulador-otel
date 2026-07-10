package br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TipoDocumentoDto(
        @JsonProperty("codigo_tipologia") String codigoTipologia,
        String nome,
        @JsonProperty("permite_reuso") Boolean permiteReuso,
        @JsonProperty("permite_multiplo") Boolean permiteMultiplo,
        Boolean ativo,
        ChecklistReferenciaDto checklist
) {
}
