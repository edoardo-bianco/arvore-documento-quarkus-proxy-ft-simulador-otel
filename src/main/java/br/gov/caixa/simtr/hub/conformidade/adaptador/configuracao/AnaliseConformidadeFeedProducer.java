package br.gov.caixa.simtr.hub.conformidade.adaptador.configuracao;

import static br.gov.caixa.simtr.hub.conformidade.adaptador.configuracao.AnaliseConformidadePersistenciaProducer.endpointCouchDb;
import static br.gov.caixa.simtr.hub.conformidade.adaptador.configuracao.AnaliseConformidadePersistenciaProducer.obrigatorio;
import static br.gov.caixa.simtr.hub.conformidade.adaptador.configuracao.AnaliseConformidadePersistenciaProducer.opcional;
import static br.gov.caixa.simtr.hub.conformidade.adaptador.configuracao.AnaliseConformidadePersistenciaProducer.porta;
import static br.gov.caixa.simtr.hub.conformidade.adaptador.configuracao.AnaliseConformidadePersistenciaProducer.rejeitarSegredosCosmos;

import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.cosmosdb.CosmosChangeFeedFactory;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.couchdb.CouchDbChangesFeed;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.documento.FeedEventosAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.cosmosdb.CosmosDbClienteFactory;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper;
import com.azure.cosmos.CosmosAsyncClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.util.Locale;
import org.eclipse.microprofile.config.Config;

@ApplicationScoped
public class AnaliseConformidadeFeedProducer {

    private static final String PREFIXO = "conformidade.";
    private CosmosAsyncClient cosmosClient;

    @Produces
    @ApplicationScoped
    FeedEventosAnaliseConformidade feed(
            Config config,
            ObjectMapper objectMapper,
            CloudEventMapper mapper,
            CosmosDbClienteFactory cosmosFactory,
            CosmosChangeFeedFactory feedFactory) {
        String backend = obrigatorio(config, PREFIXO + "persistencia.backend")
                .toLowerCase(Locale.ROOT);
        return switch (backend) {
            case "couchdb" -> couchDb(config, objectMapper, mapper);
            case "cosmosdb" -> cosmosDb(config, mapper, cosmosFactory, feedFactory);
            default -> throw new IllegalStateException(
                    "Backend do feed da conformidade inválido");
        };
    }

    private static FeedEventosAnaliseConformidade couchDb(
            Config config,
            ObjectMapper objectMapper,
            CloudEventMapper mapper) {
        return new CouchDbChangesFeed(
                endpointCouchDb(
                        obrigatorio(config, PREFIXO + "couchdb.host"),
                        porta(config, PREFIXO + "couchdb.port")),
                objectMapper,
                opcional(config, PREFIXO + "couchdb.username"),
                opcional(config, PREFIXO + "couchdb.password"),
                obrigatorio(config, PREFIXO + "couchdb.database"),
                mapper);
    }

    private FeedEventosAnaliseConformidade cosmosDb(
            Config config,
            CloudEventMapper mapper,
            CosmosDbClienteFactory cosmosFactory,
            CosmosChangeFeedFactory feedFactory) {
        rejeitarSegredosCosmos(config);
        CosmosAsyncClient client = cosmosFactory.criar(
                obrigatorio(config, PREFIXO + "cosmos.endpoint"));
        try {
            var database = client.getDatabase(
                    obrigatorio(config, PREFIXO + "cosmos.database"));
            var feedContainer = database.getContainer(
                    obrigatorio(config, PREFIXO + "cosmos.container"));
            var leaseContainer = database.getContainer(
                    obrigatorio(config, PREFIXO + "cosmos.lease-container"));
            var feed = feedFactory.criar(
                    feedContainer,
                    leaseContainer,
                    mapper,
                    hostName(config));
            cosmosClient = client;
            return feed;
        } catch (RuntimeException falha) {
            client.close();
            throw falha;
        }
    }

    private static String hostName(Config config) {
        String aplicacao = obrigatorio(config, "quarkus.application.name");
        String host = System.getenv("HOSTNAME");
        if (host == null || host.isBlank()) {
            host = "processo-" + ProcessHandle.current().pid();
        }
        return (aplicacao + "-" + host).replaceAll("[^A-Za-z0-9._-]", "-");
    }

    @PreDestroy
    void encerrar() {
        if (cosmosClient != null) {
            cosmosClient.close();
            cosmosClient = null;
        }
    }
}
