package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.couchdb;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento.DocumentoAnaliseConformidadeStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;

public class CouchDbAnaliseConformidadeStore
        extends DocumentoAnaliseConformidadeStore {

    static final Short VERSAO_SCHEMA =
            DocumentoAnaliseConformidadeStore.VERSAO_SCHEMA;

    public CouchDbAnaliseConformidadeStore(
            URI endpoint,
            ObjectMapper objectMapper,
            String username,
            String password,
            String database) {
        this(
                new CouchDbClient(endpoint, objectMapper, username, password),
                objectMapper,
                database);
    }

    CouchDbAnaliseConformidadeStore(
            CouchDbClient client,
            ObjectMapper objectMapper,
            String database) {
        super(
                new CouchDbRepositorioDocumental(client, database),
                objectMapper);
    }
}
