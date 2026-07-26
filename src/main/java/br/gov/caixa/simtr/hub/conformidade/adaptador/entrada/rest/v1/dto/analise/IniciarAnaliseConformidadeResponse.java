package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1.dto.analise;

public record IniciarAnaliseConformidadeResponse(
        String instanceId,
        StatusAnaliseConformidadeDto status) {
}
