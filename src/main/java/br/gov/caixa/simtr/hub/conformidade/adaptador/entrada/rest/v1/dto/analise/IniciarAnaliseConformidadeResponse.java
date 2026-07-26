package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise;

public record IniciarAnaliseConformidadeResponse(
        String correlationId,
        String instanceId,
        String identificadorDocumento,
        Long identificadorChecklist,
        Integer versaoChecklist,
        StatusAnaliseConformidadeDto status) {
}
