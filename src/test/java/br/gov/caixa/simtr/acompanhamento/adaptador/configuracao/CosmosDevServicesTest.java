package br.gov.caixa.simtr.acompanhamento.adaptador.configuracao;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosBatch;
import com.azure.cosmos.models.CosmosBatchItemRequestOptions;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;

@QuarkusTest
@Tag("cosmos-integration")
@TestProfile(CosmosDevServicesTest.EmuladorProfile.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(120)
class CosmosDevServicesTest {

    @Inject CosmosClient client;
    @Inject BeanManager beanManager;
    @ConfigProperty(name = "quarkus.azure.cosmos.endpoint") URI endpoint;
    @ConfigProperty(name = "quarkus.azure.cosmos.default-gateway-mode") boolean gateway;
    @ConfigProperty(name = "acompanhamento.cosmos.database") String database;
    @ConfigProperty(name = "acompanhamento.cosmos.container") String containerName;
    private CosmosContainer container;

    @BeforeAll
    void criarBancoLocal() {
        assertTrue(List.of("localhost", "127.0.0.1", "::1", "[::1]").contains(endpoint.getHost()));
        assertEquals("https", endpoint.getScheme());
        assertTrue(gateway);
        assertEquals(Dependent.class, beanManager.resolve(beanManager.getBeans(CosmosClient.class)).getScope());
        client.createDatabaseIfNotExists(database);
        var db = client.getDatabase(database);
        db.createContainerIfNotExists(containerName, "/idDossiePreValidacao");
        db.createContainerIfNotExists(containerName, "/idDossiePreValidacao");
        container = db.getContainer(containerName);
        assertEquals(List.of("/idDossiePreValidacao"), container.read().getProperties().getPartitionKeyDefinition().getPaths());
    }

    @AfterAll
    void fecharClienteExclusivoDoTeste() {
        // Producer 1.2.5 fornece instancia Dependent sem disposer; este teste e seu unico dono.
        if (client != null) client.close();
    }

    @Test
    void devePreservarChaveCorpoEIsolamentoDaParticao() {
        String id = "evento:" + UUID.randomUUID();
        String dossie = "0000123/Pré Valida?#" + UUID.randomUUID();
        String corpo = "{\n  \"situacao\": \"AGUARDANDO\", \"texto\": \"ação\"\n}";
        var pk = new PartitionKey(dossie);
        assertEquals(201, container.createItem(Map.of("id", id, "idDossiePreValidacao", dossie, "corpo", corpo)).getStatusCode());
        var reaberto = client.getDatabase(database).getContainer(containerName);
        var lido = reaberto.readItem(id, pk, ObjectNode.class).getItem();
        assertEquals(dossie, lido.path("idDossiePreValidacao").asText());
        assertEquals(corpo, lido.path("corpo").asText());
        var outraParticao = new PartitionKey("outro-" + dossie);
        assertEquals(404, assertThrows(CosmosException.class,
                () -> reaberto.readItem(id, outraParticao, ObjectNode.class)).getStatusCode());
        assertEquals(204, container.deleteItem(id, pk, null).getStatusCode());
        assertEquals(404, assertThrows(CosmosException.class,
                () -> reaberto.readItem(id, pk, ObjectNode.class)).getStatusCode());
    }

    @Test
    void deveRejeitarAtualizacaoComETagObsoleto() {
        String dossie = UUID.randomUUID().toString();
        var pk = new PartitionKey(dossie);
        var original = container.createItem(documento("execucao", dossie, 1));
        var atual = container.replaceItem(documento("execucao", dossie, 2), "execucao", pk,
                new CosmosItemRequestOptions().setIfMatchETag(original.getETag()));
        assertNotEquals(original.getETag(), atual.getETag());
        var alteracaoObsoleta = documento("execucao", dossie, 3);
        var condicaoObsoleta = new CosmosItemRequestOptions().setIfMatchETag(original.getETag());
        assertEquals(412, assertThrows(CosmosException.class,
                () -> container.replaceItem(alteracaoObsoleta, "execucao", pk, condicaoObsoleta)).getStatusCode());
        assertEquals(2, container.readItem("execucao", pk, ObjectNode.class).getItem().path("versao").asInt());
    }

    @Test
    void deveConfirmarEventoEProjecaoEReverterTodoBatchEmConflito() {
        String dossie = UUID.randomUUID().toString();
        var pk = new PartitionKey(dossie);
        var inicio = CosmosBatch.createCosmosBatch(pk);
        inicio.createItemOperation(documento("execucao", dossie, 1));
        inicio.createItemOperation(documento("evento:1", dossie, 1));
        assertTrue(container.executeCosmosBatch(inicio).isSuccessStatusCode());
        String etag = container.readItem("execucao", pk, ObjectNode.class).getETag();
        var avancar = CosmosBatch.createCosmosBatch(pk);
        avancar.createItemOperation(documento("evento:2", dossie, 2));
        avancar.replaceItemOperation("execucao", documento("execucao", dossie, 2),
                new CosmosBatchItemRequestOptions().setIfMatchETag(etag));
        assertTrue(container.executeCosmosBatch(avancar).isSuccessStatusCode());
        assertEquals(2, container.readItem("evento:2", pk, ObjectNode.class).getItem().path("versao").asInt());
        String etagAtual = container.readItem("execucao", pk, ObjectNode.class).getETag();
        var conflito = CosmosBatch.createCosmosBatch(pk);
        conflito.createItemOperation(documento("evento:3", dossie, 3));
        conflito.replaceItemOperation("execucao", documento("execucao", dossie, 3),
                new CosmosBatchItemRequestOptions().setIfMatchETag(etagAtual));
        conflito.createItemOperation(documento("evento:2", dossie, 3));
        var resultado = container.executeCosmosBatch(conflito);
        assertAll(
                () -> assertFalse(resultado.isSuccessStatusCode()),
                () -> assertEquals(409, resultado.getStatusCode()),
                () -> assertEquals(2, container.readItem("execucao", pk, ObjectNode.class).getItem().path("versao").asInt()),
                () -> assertEquals(etagAtual, container.readItem("execucao", pk, ObjectNode.class).getETag()),
                () -> assertEquals(404, assertThrows(CosmosException.class,
                        () -> container.readItem("evento:3", pk, ObjectNode.class)).getStatusCode()));
    }

    @Test
    void deveRejeitarETagObsoletoDentroDoBatchSemPersistirEventoAnterior() {
        String dossie = UUID.randomUUID().toString();
        var pk = new PartitionKey(dossie);
        var original = container.createItem(documento("execucao", dossie, 1));
        var atual = container.replaceItem(documento("execucao", dossie, 2), "execucao", pk,
                new CosmosItemRequestOptions().setIfMatchETag(original.getETag()));
        var obsoleto = CosmosBatch.createCosmosBatch(pk);
        obsoleto.createItemOperation(documento("evento:obsoleto", dossie, 3));
        obsoleto.replaceItemOperation("execucao", documento("execucao", dossie, 3),
                new CosmosBatchItemRequestOptions().setIfMatchETag(original.getETag()));
        var resultado = container.executeCosmosBatch(obsoleto);
        assertAll(
                () -> assertEquals(412, resultado.getStatusCode()),
                () -> assertEquals(atual.getETag(), container.readItem("execucao", pk, ObjectNode.class).getETag()),
                () -> assertEquals(2, container.readItem("execucao", pk, ObjectNode.class).getItem().path("versao").asInt()),
                () -> assertEquals(404, assertThrows(CosmosException.class,
                        () -> container.readItem("evento:obsoleto", pk, ObjectNode.class)).getStatusCode()));
    }

    private static Map<String, Object> documento(String id, String dossie, int versao) {
        return Map.of("id", id, "idDossiePreValidacao", dossie, "versao", versao);
    }

    public static final class EmuladorProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            final boolean configuracaoExterna;
            try {
                var config = ConfigUtils.configBuilder().withProfile("test").build();
                configuracaoExterna = config.isPropertyPresent("quarkus.azure.cosmos.endpoint")
                        || config.isPropertyPresent("quarkus.azure.cosmos.key");
            } catch (RuntimeException _) {
                throw new IllegalStateException("Nao foi possivel verificar a configuracao do teste Cosmos.");
            }
            if (configuracaoExterna) {
                throw new IllegalStateException("O teste Cosmos exige ausencia de endpoint/chave externos.");
            }
            return Map.of("quarkus.devservices.enabled", "true",
                    "quarkus.azure.servicebus.devservices.enabled", "false",
                    "quarkus.azure.cosmos.enabled", "true",
                    "quarkus.azure.cosmos.devservices.enabled", "true",
                    "quarkus.azure.cosmos.devservices.shared", "false");
        }
    }
}