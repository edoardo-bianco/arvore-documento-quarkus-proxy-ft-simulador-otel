package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record VisaoAnaliseConformidadeResponse(
        String correlationId,
        String instanceId,
        String identificadorDocumento,
        Long identificadorChecklist,
        Integer versaoChecklist,
        StatusAnaliseConformidadeDto status,
        ResultadoAnaliseConformidadeResponse resultadoPreliminar,
        ResultadoAnaliseConformidadeResponse resultadoFinal,
        String mensagemErro) {
}
