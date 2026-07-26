package br.gov.caixa.simtr.hub.conformidade.spike.persistencia;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.ObjectMapper;

class CouchDbChangesIntegrationTest {

    private static final DockerImageName COUCHDB_IMAGE = DockerImageName.parse("couchdb:3.5.2");
    private static final String USER = "spike";
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final String DOCUMENT = """
            {
              "_id": "revisao:analise-123",
              "tipo": "revisao-humana",
              "referencia": "analise-123",
              "hash": "sha256:abc123",
              "parecer": "conteudo-que-nao-deve-ser-publicado"
            }
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void documentoGeraCloudEventReferencialRepetivelPeloChanges() throws Exception {
        try (var couchDb = new GenericContainer<>(COUCHDB_IMAGE)
                .withEnv("COUCHDB_USER", USER)
                .withEnv("COUCHDB_PASSWORD", PASSWORD)
                .withExposedPorts(5984)) {
            couchDb.start();
            var baseUri = URI.create("http://" + couchDb.getHost() + ":" + couchDb.getMappedPort(5984));

            send(baseUri.resolve("/conformidade"), "PUT", null, 201);
            send(baseUri.resolve("/conformidade/revisao%3Aanalise-123"), "PUT", DOCUMENT, 201);

            var firstFeed = changes(baseUri);
            var repeatedFeed = changes(baseUri);
            var mapper = new CouchDbChangeEventMapper();

            var firstEvent = mapper.map(firstFeed.path("results").get(0));
            var repeatedEvent = mapper.map(repeatedFeed.path("results").get(0));

            assertEquals(firstEvent.getId(), repeatedEvent.getId());
            assertEquals("analise-123", firstEvent.getExtension("referencia"));
            assertEquals("sha256:abc123", firstEvent.getExtension("hashdocumento"));
            assertNull(firstEvent.getData(), "o parecer não pode atravessar o CloudEvent referencial");
        }
    }

    private com.fasterxml.jackson.databind.JsonNode changes(URI baseUri) throws Exception {
        var response = send(
                baseUri.resolve("/conformidade/_changes?since=0&include_docs=true&feed=normal"),
                "GET",
                null,
                200);
        return objectMapper.readTree(response);
    }

    private String send(URI uri, String method, String body, int expectedStatus) throws Exception {
        var bodyPublisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        var request = HttpRequest.newBuilder(uri)
                .header("Authorization", basicAuthorization())
                .header("Content-Type", "application/json")
                .method(method, bodyPublisher)
                .build();
        var response = httpClient.send(request, ofString());
        assertEquals(expectedStatus, response.statusCode(), response.body());
        return response.body();
    }

    private static String basicAuthorization() {
        var credentials = USER + ":" + PASSWORD;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
