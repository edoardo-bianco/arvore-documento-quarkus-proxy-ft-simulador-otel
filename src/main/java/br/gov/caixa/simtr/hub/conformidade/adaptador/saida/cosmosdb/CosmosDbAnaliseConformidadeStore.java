package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.cosmosdb;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento.DocumentoAnaliseConformidadeStore;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento.RepositorioDocumental;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento.RepositorioDocumentalObservavel;
import com.azure.cosmos.CosmosAsyncContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

public final class CosmosDbAnaliseConformidadeStore
        extends DocumentoAnaliseConformidadeStore {

    public CosmosDbAnaliseConformidadeStore(
            CosmosAsyncContainer container,
            ObjectMapper objectMapper) {
        this(
                container,
                objectMapper,
                OpenTelemetry.noop().getTracer("simtr-hub-conformidade"));
    }

    public CosmosDbAnaliseConformidadeStore(
            CosmosAsyncContainer container,
            ObjectMapper objectMapper,
            Tracer tracer) {
        super(
                new RepositorioDocumentalObservavel(
                        new CosmosDbRepositorioDocumental(container),
                        tracer,
                        "cosmosdb"),
                objectMapper);
    }

    CosmosDbAnaliseConformidadeStore(
            RepositorioDocumental repositorio,
            ObjectMapper objectMapper) {
        super(repositorio, objectMapper);
    }
}
