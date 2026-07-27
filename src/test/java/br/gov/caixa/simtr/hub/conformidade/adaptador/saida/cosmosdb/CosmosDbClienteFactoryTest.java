package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.cosmosdb;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.azure.identity.DefaultAzureCredential;
import org.junit.jupiter.api.Test;

class CosmosDbClienteFactoryTest {

    private final CosmosDbClienteFactory factory = new CosmosDbClienteFactory();

    @Test
    void usaDefaultAzureCredential() {
        assertInstanceOf(DefaultAzureCredential.class, factory.credencial());
    }

    @Test
    void rejeitaEndpointSemHttps() {
        assertThrows(
                IllegalArgumentException.class,
                () -> factory.criar("http://conta.documents.azure.com"));
    }
}
