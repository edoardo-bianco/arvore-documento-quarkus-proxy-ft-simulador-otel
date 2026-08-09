package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.couchdb;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento.DocumentoAnaliseConformidadeStore;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento.RepositorioDocumentalObservavel;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
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
                endpoint,
                objectMapper,
                username,
                password,
                database,
                OpenTelemetry.noop().getTracer("simtr-hub-conformidade"));
    }

    public CouchDbAnaliseConformidadeStore(
            URI endpoint,
            ObjectMapper objectMapper,
            String username,
            String password,
            String database,
            Tracer tracer) {
        this(
                new CouchDbClient(endpoint, objectMapper, username, password),
                objectMapper,
                database,
                tracer);
    }

    CouchDbAnaliseConformidadeStore(
            CouchDbClient client,
            ObjectMapper objectMapper,
            String database) {
        this(
                client,
                objectMapper,
                database,
                OpenTelemetry.noop().getTracer("simtr-hub-conformidade"));
    }

    CouchDbAnaliseConformidadeStore(
            CouchDbClient client,
            ObjectMapper objectMapper,
            String database,
            Tracer tracer) {
        super(
                new RepositorioDocumentalObservavel(
                        new CouchDbRepositorioDocumental(client, database),
                        tracer,
                        "couchdb"),
                objectMapper);
    }
}
