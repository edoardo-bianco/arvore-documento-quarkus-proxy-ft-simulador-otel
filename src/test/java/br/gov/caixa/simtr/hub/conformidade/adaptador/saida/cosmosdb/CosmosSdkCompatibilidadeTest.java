package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.cosmosdb;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class CosmosSdkCompatibilidadeTest {

    @Test
    void sdkAssincronoECredencialEntraEstaoDisponiveis() {
        assertDoesNotThrow(() -> Class.forName(
                "com.azure.cosmos.CosmosAsyncClient"));
        assertDoesNotThrow(() -> Class.forName(
                "com.azure.identity.DefaultAzureCredential"));
    }
}
