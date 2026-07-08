package br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.checklist;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChecklistDto(
        String nome,
        @JsonProperty("identificador_negocial") Long identificadorNegocial,
        Integer versao,
        @JsonProperty("data_hora_criacao") String dataHoraCriacao,
        @JsonProperty("data_hora_ultima_alteracao") String dataHoraUltimaAlteracao,
        @JsonProperty("verificacao_previa") Boolean verificacaoPrevia,
        @JsonProperty("orientacao_operador") String orientacaoOperador,
        List<ChecklistApontamentoDto> apontamentos
) {
}
