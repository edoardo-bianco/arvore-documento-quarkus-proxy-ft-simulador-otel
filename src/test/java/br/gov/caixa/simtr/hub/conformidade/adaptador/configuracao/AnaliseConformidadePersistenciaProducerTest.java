package br.gov.caixa.simtr.hub.conformidade.adaptador.configuracao;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.cosmosdb.CosmosDbAnaliseConformidadeStore;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.cosmosdb.CosmosDbClienteFactory;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.couchdb.CouchDbAnaliseConformidadeStore;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.quarkus.test.junit.QuarkusTest;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AnaliseConformidadePersistenciaProducerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CosmosDbClienteFactory cosmosFactory = mock(CosmosDbClienteFactory.class);
    private final Tracer tracer = OpenTelemetry.noop()
            .getTracer("simtr-hub-conformidade");
    private final AnaliseConformidadePersistenciaProducer producer =
            new AnaliseConformidadePersistenciaProducer();

    @Test
    void selecionaCouchDbExplicitamente() {
        var porta = producer.portaSaida(
                config(Map.of(
                        "conformidade.persistencia.backend", "couchdb",
                        "conformidade.couchdb.host", "localhost",
                        "conformidade.couchdb.port", "5984",
                        "conformidade.couchdb.database", "conformidade")),
                objectMapper,
                cosmosFactory,
                tracer);

        assertInstanceOf(CouchDbAnaliseConformidadeStore.class, porta);
    }

    @Test
    void selecionaCosmosComClienteAssincronoEFechaClienteNoShutdown() {
        CosmosAsyncClient client = mock(CosmosAsyncClient.class);
        CosmosAsyncDatabase database = mock(CosmosAsyncDatabase.class);
        CosmosAsyncContainer container = mock(CosmosAsyncContainer.class);
        when(cosmosFactory.criar("https://conta.documents.azure.com"))
                .thenReturn(client);
        when(client.getDatabase("conformidade")).thenReturn(database);
        when(database.getContainer("analises")).thenReturn(container);

        var porta = producer.portaSaida(
                config(Map.of(
                        "conformidade.persistencia.backend", "cosmosdb",
                        "conformidade.cosmos.endpoint", "https://conta.documents.azure.com",
                        "conformidade.cosmos.database", "conformidade",
                        "conformidade.cosmos.container", "analises")),
                objectMapper,
                cosmosFactory,
                tracer);
        producer.encerrar();

        assertInstanceOf(CosmosDbAnaliseConformidadeStore.class, porta);
        verify(client).close();
    }

    @Test
    void falhaSemBackendOuComBackendDesconhecido() {
        assertThrows(
                IllegalStateException.class,
                () -> producer.portaSaida(
                        config(Map.of()),
                        objectMapper,
                        cosmosFactory,
                        tracer));
        assertThrows(
                IllegalStateException.class,
                () -> producer.portaSaida(
                        config(Map.of("conformidade.persistencia.backend", "mongodb")),
                        objectMapper,
                        cosmosFactory,
                        tracer));
    }

    @Test
    void rejeitaChaveOuConnectionStringNoBackendCosmos() {
        Map<String, String> base = Map.of(
                "conformidade.persistencia.backend", "cosmosdb",
                "conformidade.cosmos.endpoint", "https://conta.documents.azure.com",
                "conformidade.cosmos.database", "conformidade",
                "conformidade.cosmos.container", "analises");

        assertThrows(
                IllegalStateException.class,
                () -> producer.portaSaida(
                        config(comChave(base, "conformidade.cosmos.key", "segredo")),
                        objectMapper,
                        cosmosFactory,
                        tracer));
        assertThrows(
                IllegalStateException.class,
                () -> producer.portaSaida(
                        config(comChave(
                                base,
                                "conformidade.cosmos.connection-string",
                                "segredo")),
                        objectMapper,
                        cosmosFactory,
                        tracer));
    }

    private static Config config(Map<String, String> valores) {
        Config config = mock(Config.class);
        when(config.getOptionalValue(anyString(), eq(String.class)))
                .thenAnswer(invocacao -> Optional.ofNullable(
                        valores.get(invocacao.getArgument(0, String.class))));
        return config;
    }

    private static Map<String, String> comChave(
            Map<String, String> base,
            String chave,
            String valor) {
        var valores = new java.util.HashMap<>(base);
        valores.put(chave, valor);
        return valores;
    }
}
