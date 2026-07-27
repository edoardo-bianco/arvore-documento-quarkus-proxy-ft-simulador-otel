package br.gov.caixa.simtr.hub.conformidade.suporte;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class CouchDbQuarkusTestResource
        implements QuarkusTestResourceLifecycleManager {

    private static final int PORTA_COUCHDB = 5984;
    private static final String USUARIO = "quarkus-test";
    private static final String DATABASE = "conformidade-test";
    private GenericContainer<?> couchDb;

    @Override
    public Map<String, String> start() {
        String password = UUID.randomUUID().toString();
        couchDb = new GenericContainer<>(DockerImageName.parse("couchdb:3.5.2"))
                .withEnv("COUCHDB_USER", USUARIO)
                .withEnv("COUCHDB_PASSWORD", password)
                .withExposedPorts(PORTA_COUCHDB)
                .waitingFor(Wait.forHttp("/_up")
                        .withBasicCredentials(USUARIO, password)
                        .forStatusCode(200));
        couchDb.start();
        try {
            criarDatabase(password);
        } catch (RuntimeException falha) {
            couchDb.stop();
            throw falha;
        }
        return Map.of(
                "conformidade.persistencia.backend", "couchdb",
                "conformidade.couchdb.host", couchDb.getHost(),
                "conformidade.couchdb.port", couchDb.getMappedPort(PORTA_COUCHDB).toString(),
                "conformidade.couchdb.database", DATABASE,
                "conformidade.couchdb.username", USUARIO,
                "conformidade.couchdb.password", password);
    }

    @Override
    public void stop() {
        if (couchDb != null) {
            couchDb.stop();
        }
    }

    private void criarDatabase(String password) {
        try {
            URI endpoint = URI.create("http://"
                    + couchDb.getHost()
                    + ":"
                    + couchDb.getMappedPort(PORTA_COUCHDB)
                    + "/"
                    + DATABASE);
            String credencial = Base64.getEncoder().encodeToString(
                    (USUARIO + ":" + password).getBytes(StandardCharsets.UTF_8));
            var requisicao = HttpRequest.newBuilder(endpoint)
                    .header("Authorization", "Basic " + credencial)
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();
            int status = HttpClient.newHttpClient()
                    .send(requisicao, HttpResponse.BodyHandlers.discarding())
                    .statusCode();
            if (status != 201 && status != 412) {
                throw new IllegalStateException(
                        "Não foi possível preparar o CouchDB de teste");
            }
        } catch (InterruptedException falha) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Preparação do CouchDB de teste interrompida",
                    falha);
        } catch (java.io.IOException falha) {
            throw new IllegalStateException(
                    "Não foi possível preparar o CouchDB de teste",
                    falha);
        }
    }
}
