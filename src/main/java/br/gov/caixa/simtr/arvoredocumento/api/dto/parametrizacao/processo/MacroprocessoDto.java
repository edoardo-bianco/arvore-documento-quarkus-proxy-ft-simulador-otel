package br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MacroprocessoDto(
        @JsonProperty("identificador_negocial") Long identificadorNegocial,
        String nome,
        Boolean ativo,
        @JsonProperty("ultima_alteracao") String ultimaAlteracao
) {
}
