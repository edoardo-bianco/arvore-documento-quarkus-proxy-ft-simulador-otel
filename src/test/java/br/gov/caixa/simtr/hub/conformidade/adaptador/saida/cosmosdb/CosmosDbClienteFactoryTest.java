package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.cosmosdb;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.azure.identity.DefaultAzureCredential;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
class CosmosDbClienteFactoryTest {

    private final CosmosDbClienteFactory factory = new CosmosDbClienteFactory();

    @Test
    void usaDefaultAzureCredential() {
        assertInstanceOf(DefaultAzureCredential.class, factory.credencial());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://conta.documents.azure.com",
            "https:///sem-host",
            "https://usuario@conta.documents.azure.com",
            "https://conta.documents.azure.com?consulta=1",
            "https://conta.documents.azure.com#fragmento",
            "https://[endpoint-invalido"
    })
    void rejeitaEndpointInseguroOuMalformado(String endpoint) {
        assertThrows(
                IllegalArgumentException.class,
                () -> factory.criar(endpoint));
    }
}
