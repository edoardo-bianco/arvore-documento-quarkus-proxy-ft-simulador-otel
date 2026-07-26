package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.couchdb;

import static java.net.http.HttpResponse.BodyHandlers.ofString;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class CouchDbClient {

    private final URI baseUri;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String authorization;

    CouchDbClient(
            URI baseUri,
            ObjectMapper objectMapper,
            String username,
            String password) {
        this.baseUri = validarBaseUri(baseUri);
        this.objectMapper = java.util.Objects.requireNonNull(objectMapper, "objectMapper");
        this.httpClient = HttpClient.newHttpClient();
        this.authorization = authorization(username, password);
    }

    Uni<Resposta> gravar(String database, String documentId, JsonNode document) {
        return Uni.createFrom().deferred(() -> {
            try {
                var request = request(database, documentId)
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(
                                objectMapper.writeValueAsString(document)))
                        .build();
                return enviar(request);
            } catch (IOException falha) {
                return Uni.createFrom().failure(
                        new IllegalStateException("Falha ao serializar documento CouchDB", falha));
            }
        });
    }

    Uni<Resposta> consultar(String database, String documentId) {
        return enviar(request(database, documentId).GET().build());
    }

    private Uni<Resposta> enviar(HttpRequest request) {
        return Uni.createFrom()
                .completionStage(() -> httpClient.sendAsync(
                        request,
                        ofString(StandardCharsets.UTF_8)))
                .map(response -> resposta(
                        response.statusCode(),
                        response.body()))
                .onFailure()
                .transform(falha ->
                        new IllegalStateException("Falha na chamada CouchDB", falha));
    }

    private Resposta resposta(int status, String body) {
        try {
            JsonNode conteudo = body.isBlank()
                    ? null
                    : objectMapper.readTree(body);
            return new Resposta(status, conteudo);
        } catch (IOException falha) {
            throw new IllegalStateException("Resposta CouchDB inválida", falha);
        }
    }

    private HttpRequest.Builder request(String database, String documentId) {
        var builder = HttpRequest.newBuilder(baseUri.resolve(
                "/" + database + "/" + documentId));
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return builder;
    }

    private static URI validarBaseUri(URI baseUri) {
        if (baseUri == null
                || baseUri.getScheme() == null
                || baseUri.getHost() == null
                || (!"http".equals(baseUri.getScheme())
                        && !"https".equals(baseUri.getScheme()))) {
            throw new IllegalArgumentException("URL CouchDB inválida");
        }
        return baseUri;
    }

    private static String authorization(String username, String password) {
        boolean semUsuario = username == null || username.isBlank();
        boolean semSenha = password == null || password.isBlank();
        if (semUsuario && semSenha) {
            return null;
        }
        if (semUsuario || semSenha) {
            throw new IllegalArgumentException("Credencial CouchDB incompleta");
        }
        var credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));
    }

    record Resposta(int status, JsonNode body) {
    }
}
