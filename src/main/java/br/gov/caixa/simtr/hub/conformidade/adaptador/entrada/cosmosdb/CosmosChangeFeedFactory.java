package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.cosmosdb;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper;
import com.azure.cosmos.CosmosAsyncContainer;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CosmosChangeFeedFactory {

    public CosmosChangeFeed criar(
            CosmosAsyncContainer feedContainer,
            CosmosAsyncContainer leaseContainer,
            CloudEventMapper mapper,
            String hostName) {
        return new CosmosChangeFeed(
                feedContainer,
                leaseContainer,
                mapper,
                hostName);
    }
}
