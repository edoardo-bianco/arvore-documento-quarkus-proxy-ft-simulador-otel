package br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(using = ChecklistReferenciaDtoDeserializer.class)
public record ChecklistReferenciaDto(
        @JsonProperty("identificador_checklist") Long identificadorChecklist,
        @JsonProperty("versao_checklist") Integer versaoChecklist
) {
}
