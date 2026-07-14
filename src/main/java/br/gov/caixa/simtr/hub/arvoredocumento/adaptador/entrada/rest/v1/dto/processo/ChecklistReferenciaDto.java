package br.gov.caixa.simtr.hub.arvoredocumento.adaptador.entrada.rest.v1.dto.processo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChecklistReferenciaDto(
        @JsonProperty("identificador_checklist") Long identificadorChecklist,
        @JsonProperty("versao_checklist") Integer versaoChecklist
) {
}
