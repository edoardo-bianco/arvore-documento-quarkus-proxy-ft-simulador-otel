package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.simulador.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChecklistSimuladorResponse(
        String nome,
        @JsonProperty("identificador_negocial") Long identificadorNegocial,
        Integer versao,
        @JsonProperty("data_hora_criacao") String dataHoraCriacao,
        @JsonProperty("data_hora_ultima_alteracao") String dataHoraUltimaAlteracao,
        @JsonProperty("verificacao_previa") Boolean verificacaoPrevia,
        @JsonProperty("orientacao_operador") String orientacaoOperador,
        List<Apontamento> apontamentos
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Apontamento(
            @JsonProperty("identificador_negocial") Long identificadorNegocial,
            String nome,
            String descricao,
            @JsonProperty("orientacao_operador") String orientacaoOperador,
            @JsonProperty("indicador_reanalise") Boolean indicadorReanalise,
            @JsonProperty("sequencia_apresentacao") Integer sequenciaApresentacao
    ) {
    }
}
