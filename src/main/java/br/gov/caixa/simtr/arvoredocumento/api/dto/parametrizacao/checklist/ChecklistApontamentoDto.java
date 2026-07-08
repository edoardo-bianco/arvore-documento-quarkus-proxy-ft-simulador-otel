package br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.checklist;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChecklistApontamentoDto(
        @JsonProperty("identificador_negocial") Long identificadorNegocial,
        String nome,
        String descricao,
        @JsonProperty("orientacao_operador") String orientacaoOperador,
        @JsonProperty("indicador_reanalise") Boolean indicadorReanalise,
        @JsonProperty("sequencia_apresentacao") Integer sequenciaApresentacao
) {
}
