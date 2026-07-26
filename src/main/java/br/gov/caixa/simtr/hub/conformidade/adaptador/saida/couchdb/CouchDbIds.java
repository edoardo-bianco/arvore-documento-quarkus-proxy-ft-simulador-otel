package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.couchdb;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento.IdsDocumentoAnaliseConformidade;

final class CouchDbIds {

    private CouchDbIds() {
    }

    static String entrada(String correlationId) {
        return IdsDocumentoAnaliseConformidade.entrada(correlationId);
    }

    static String projecao(String instanceId) {
        return IdsDocumentoAnaliseConformidade.projecao(instanceId);
    }

    static String checklist(String correlationId) {
        return IdsDocumentoAnaliseConformidade.checklist(correlationId);
    }
}
