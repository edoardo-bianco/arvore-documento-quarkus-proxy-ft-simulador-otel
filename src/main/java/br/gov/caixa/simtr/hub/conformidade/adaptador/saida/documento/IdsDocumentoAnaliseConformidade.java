package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento;

public final class IdsDocumentoAnaliseConformidade {

    private IdsDocumentoAnaliseConformidade() {
    }

    public static String entrada(String correlationId) {
        return br.gov.caixa.simtr.hub.conformidade.aplicacao.documento
                .IdsDocumentoAnaliseConformidade.entrada(correlationId);
    }

    public static String projecao(String instanceId) {
        return br.gov.caixa.simtr.hub.conformidade.aplicacao.documento
                .IdsDocumentoAnaliseConformidade.projecao(instanceId);
    }

    public static String checklist(String correlationId) {
        return br.gov.caixa.simtr.hub.conformidade.aplicacao.documento
                .IdsDocumentoAnaliseConformidade.checklist(correlationId);
    }

    public static String resultadoPreliminar(String correlationId) {
        return br.gov.caixa.simtr.hub.conformidade.aplicacao.documento
                .IdsDocumentoAnaliseConformidade.resultadoPreliminar(correlationId);
    }

    public static String revisao(String correlationId) {
        return br.gov.caixa.simtr.hub.conformidade.aplicacao.documento
                .IdsDocumentoAnaliseConformidade.revisao(correlationId);
    }

    public static String resultadoFinal(String correlationId) {
        return br.gov.caixa.simtr.hub.conformidade.aplicacao.documento
                .IdsDocumentoAnaliseConformidade.resultadoFinal(correlationId);
    }

    public static String falha(String correlationId) {
        return br.gov.caixa.simtr.hub.conformidade.aplicacao.documento
                .IdsDocumentoAnaliseConformidade.falha(correlationId);
    }

    public static String emissao(String eventoId) {
        return br.gov.caixa.simtr.hub.conformidade.aplicacao.documento
                .IdsDocumentoAnaliseConformidade.emissao(eventoId);
    }
}
