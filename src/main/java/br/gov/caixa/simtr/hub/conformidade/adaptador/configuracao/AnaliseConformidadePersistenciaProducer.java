package br.gov.caixa.simtr.hub.conformidade.adaptador.configuracao;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.cosmosdb.CosmosDbAnaliseConformidadeStore;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.cosmosdb.CosmosDbClienteFactory;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.couchdb.CouchDbAnaliseConformidadeStore;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import com.azure.cosmos.CosmosAsyncClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Tracer;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import org.eclipse.microprofile.config.Config;

@ApplicationScoped
public class AnaliseConformidadePersistenciaProducer {

    private static final String PREFIXO = "conformidade.";
    private CosmosAsyncClient cosmosClient;

    @Produces
    @ApplicationScoped
    ArmazenarEstadoAnaliseConformidade portaSaida(
            Config config,
            ObjectMapper objectMapper,
            CosmosDbClienteFactory cosmosFactory,
            Tracer tracer) {
        String backend = obrigatorio(config, PREFIXO + "persistencia.backend")
                .toLowerCase(Locale.ROOT);
        return switch (backend) {
            case "couchdb" -> couchDb(config, objectMapper, tracer);
            case "cosmosdb" -> cosmosDb(config, objectMapper, cosmosFactory, tracer);
            default -> throw new IllegalStateException(
                    "Backend de persistência da conformidade inválido");
        };
    }

    private static ArmazenarEstadoAnaliseConformidade couchDb(
            Config config,
            ObjectMapper objectMapper,
            Tracer tracer) {
        URI endpoint = endpointCouchDb(
                obrigatorio(config, PREFIXO + "couchdb.host"),
                porta(config, PREFIXO + "couchdb.port"));
        return new CouchDbAnaliseConformidadeStore(
                endpoint,
                objectMapper,
                opcional(config, PREFIXO + "couchdb.username"),
                opcional(config, PREFIXO + "couchdb.password"),
                obrigatorio(config, PREFIXO + "couchdb.database"),
                tracer);
    }

    private ArmazenarEstadoAnaliseConformidade cosmosDb(
            Config config,
            ObjectMapper objectMapper,
            CosmosDbClienteFactory cosmosFactory,
            Tracer tracer) {
        rejeitarSegredosCosmos(config);
        CosmosAsyncClient client = cosmosFactory.criar(
                obrigatorio(config, PREFIXO + "cosmos.endpoint"));
        try {
            var container = client
                    .getDatabase(obrigatorio(config, PREFIXO + "cosmos.database"))
                    .getContainer(obrigatorio(config, PREFIXO + "cosmos.container"));
            cosmosClient = client;
            return new CosmosDbAnaliseConformidadeStore(
                    container,
                    objectMapper,
                    tracer);
        } catch (RuntimeException falha) {
            client.close();
            throw falha;
        }
    }

    static void rejeitarSegredosCosmos(Config config) {
        boolean segredoConfigurado =
                configurado(config, PREFIXO + "cosmos.key")
                        || configurado(config, PREFIXO + "cosmos.connection-string")
                        || ambienteConfigurado("COSMOS_KEY")
                        || ambienteConfigurado("AZURE_COSMOS_KEY")
                        || ambienteConfigurado("COSMOS_CONNECTION_STRING")
                        || ambienteConfigurado("AZURE_COSMOS_CONNECTION_STRING");
        if (segredoConfigurado) {
            throw new IllegalStateException(
                    "Chave e connection string do Cosmos não são permitidas");
        }
    }

    static URI endpointCouchDb(String host, int port) {
        try {
            URI endpoint = new URI("http", null, host, port, null, null, null);
            if (endpoint.getHost() == null) {
                throw new IllegalStateException("Endpoint CouchDB inválido");
            }
            return endpoint;
        } catch (URISyntaxException falha) {
            throw new IllegalStateException("Endpoint CouchDB inválido", falha);
        }
    }

    static int porta(Config config, String nome) {
        try {
            int valor = Integer.parseInt(obrigatorio(config, nome));
            if (valor < 1 || valor > 65_535) {
                throw new IllegalStateException("Porta CouchDB inválida");
            }
            return valor;
        } catch (NumberFormatException falha) {
            throw new IllegalStateException("Porta CouchDB inválida", falha);
        }
    }

    static String obrigatorio(Config config, String nome) {
        return config.getOptionalValue(nome, String.class)
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                .orElseThrow(() -> new IllegalStateException(
                        "Configuração obrigatória da persistência ausente"));
    }

    static String opcional(Config config, String nome) {
        return config.getOptionalValue(nome, String.class)
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                .orElse(null);
    }

    private static boolean configurado(Config config, String nome) {
        return opcional(config, nome) != null;
    }

    private static boolean ambienteConfigurado(String nome) {
        String valor = System.getenv(nome);
        return valor != null && !valor.isBlank();
    }

    @PreDestroy
    void encerrar() {
        if (cosmosClient != null) {
            cosmosClient.close();
            cosmosClient = null;
        }
    }
}
