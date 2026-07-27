package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.cosmosdb;

import com.azure.core.credential.TokenCredential;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;

@ApplicationScoped
public class CosmosDbClienteFactory {

    public CosmosAsyncClient criar(String endpoint) {
        return new CosmosClientBuilder()
                .endpoint(validarEndpoint(endpoint).toString())
                .credential(credencial())
                .buildAsyncClient();
    }

    TokenCredential credencial() {
        return new DefaultAzureCredentialBuilder().build();
    }

    private static URI validarEndpoint(String endpoint) {
        try {
            URI uri = URI.create(endpoint);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null) {
                throw new IllegalArgumentException("Endpoint Cosmos inválido");
            }
            return uri;
        } catch (IllegalArgumentException falha) {
            throw new IllegalArgumentException("Endpoint Cosmos inválido", falha);
        }
    }
}
