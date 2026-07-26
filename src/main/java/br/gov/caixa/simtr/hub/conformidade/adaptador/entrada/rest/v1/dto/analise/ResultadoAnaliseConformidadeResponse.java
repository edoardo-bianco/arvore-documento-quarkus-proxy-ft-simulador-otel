package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ResultadoAnaliseConformidadeResponse(
        Long identificadorChecklist,
        Integer versaoChecklist,
        String nomeChecklist,
        String resumo,
        List<ResultadoApontamentoConformidadeResponse> apontamentos) {
}
