package br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo;

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
