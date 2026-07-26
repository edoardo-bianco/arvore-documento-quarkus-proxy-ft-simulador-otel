package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.cosmosdb;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento.DocumentoAnaliseConformidadeStore;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento.RepositorioDocumental;
import com.azure.cosmos.CosmosAsyncContainer;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class CosmosDbAnaliseConformidadeStore
        extends DocumentoAnaliseConformidadeStore {

    public CosmosDbAnaliseConformidadeStore(
            CosmosAsyncContainer container,
            ObjectMapper objectMapper) {
        super(new CosmosDbRepositorioDocumental(container), objectMapper);
    }

    CosmosDbAnaliseConformidadeStore(
            RepositorioDocumental repositorio,
            ObjectMapper objectMapper) {
        super(repositorio, objectMapper);
    }
}
