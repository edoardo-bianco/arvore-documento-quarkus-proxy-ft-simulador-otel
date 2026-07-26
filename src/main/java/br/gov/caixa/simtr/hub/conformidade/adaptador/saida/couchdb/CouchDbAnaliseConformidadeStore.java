package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.couchdb;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento.DocumentoAnaliseConformidadeStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CouchDbAnaliseConformidadeStore
        extends DocumentoAnaliseConformidadeStore {

    static final Short VERSAO_SCHEMA =
            DocumentoAnaliseConformidadeStore.VERSAO_SCHEMA;

    CouchDbAnaliseConformidadeStore(
            CouchDbClient client,
            ObjectMapper objectMapper,
            String database) {
        super(
                new CouchDbRepositorioDocumental(client, database),
                objectMapper);
    }
}
