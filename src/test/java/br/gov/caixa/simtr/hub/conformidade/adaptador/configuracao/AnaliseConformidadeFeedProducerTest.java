package br.gov.caixa.simtr.hub.conformidade.adaptador.configuracao;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.cosmosdb.CosmosChangeFeed;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.cosmosdb.CosmosChangeFeedFactory;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.couchdb.CouchDbChangesFeed;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.cosmosdb.CosmosDbClienteFactory;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AnaliseConformidadeFeedProducerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloudEventMapper mapper = new CloudEventMapper(objectMapper);
    private final CosmosDbClienteFactory cosmosFactory = mock(CosmosDbClienteFactory.class);
    private final CosmosChangeFeedFactory feedFactory = mock(CosmosChangeFeedFactory.class);
    private final AnaliseConformidadeFeedProducer producer =
            new AnaliseConformidadeFeedProducer();

    @Test
    void selecionaChangesDoCouchDbPeloMesmoBackendDocumental() {
        var feed = producer.feed(
                config(Map.of(
                        "conformidade.persistencia.backend", "couchdb",
                        "conformidade.couchdb.host", "localhost",
                        "conformidade.couchdb.port", "5984",
                        "conformidade.couchdb.database", "conformidade")),
                objectMapper,
                mapper,
                cosmosFactory,
                feedFactory);

        assertInstanceOf(CouchDbChangesFeed.class, feed);
        feed.close();
    }

    @Test
    void selecionaChangeFeedComContainerDeLeasesEFechaCliente() {
        CosmosAsyncClient client = mock(CosmosAsyncClient.class);
        CosmosAsyncDatabase database = mock(CosmosAsyncDatabase.class);
        CosmosAsyncContainer items = mock(CosmosAsyncContainer.class);
        CosmosAsyncContainer leases = mock(CosmosAsyncContainer.class);
        when(cosmosFactory.criar("https://conta.documents.azure.com"))
                .thenReturn(client);
        when(client.getDatabase("conformidade")).thenReturn(database);
        when(database.getContainer("analises")).thenReturn(items);
        when(database.getContainer("analises-leases")).thenReturn(leases);
        CosmosChangeFeed cosmosFeed = mock(CosmosChangeFeed.class);
        when(feedFactory.criar(eq(items), eq(leases), eq(mapper), anyString()))
                .thenReturn(cosmosFeed);

        var feed = producer.feed(
                config(Map.of(
                        "quarkus.application.name", "simtr-hub",
                        "conformidade.persistencia.backend", "cosmosdb",
                        "conformidade.cosmos.endpoint", "https://conta.documents.azure.com",
                        "conformidade.cosmos.database", "conformidade",
                        "conformidade.cosmos.container", "analises",
                        "conformidade.cosmos.lease-container", "analises-leases")),
                objectMapper,
                mapper,
                cosmosFactory,
                feedFactory);
        producer.encerrar();

        assertInstanceOf(CosmosChangeFeed.class, feed);
        verify(database).getContainer("analises");
        verify(database).getContainer("analises-leases");
        verify(client).close();
    }

    @Test
    void rejeitaBackendDesconhecido() {
        Config config = config(Map.of(
                "conformidade.persistencia.backend",
                "mongodb"));

        assertThrows(
                IllegalStateException.class,
                () -> producer.feed(
                        config,
                        objectMapper,
                        mapper,
                        cosmosFactory,
                        feedFactory));
    }

    private static Config config(Map<String, String> valores) {
        Config config = mock(Config.class);
        when(config.getOptionalValue(anyString(), eq(String.class)))
                .thenAnswer(invocacao -> Optional.ofNullable(
                        valores.get(invocacao.getArgument(0, String.class))));
        return config;
    }
}
