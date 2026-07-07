package br.gov.caixa.arvoredocumento.api.dto.parametrizacao.processo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChecklistReferenciaDto(
        @JsonProperty("identificador_checklist") Long identificadorChecklist,
        @JsonProperty("versao_checklist") Integer versaoChecklist
) {
}
