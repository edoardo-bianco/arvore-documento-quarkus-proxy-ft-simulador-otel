package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.couchdb;

import static java.net.http.HttpResponse.BodyHandlers.ofString;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Uni;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

final class CouchDbChangesHttpClient implements CouchDbChangesClient {

    private static final String CURSOR_ID = "_local/simtr-flow-revisao-v1";
    private static final String CURSOR_INICIAL = "0";
    private static final String CURSOR_INVALIDO = "Cursor CouchDB inválido";
    private static final int MAX_TENTATIVAS_CURSOR = 3;

    private final URI databaseUri;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String authorization;
    private final int longPollTimeoutMillis;

    CouchDbChangesHttpClient(
            URI baseUri,
            ObjectMapper objectMapper,
            String username,
            String password,
            String database) {
        this(baseUri, objectMapper, username, password, database, 25_000);
    }

    CouchDbChangesHttpClient(
            URI baseUri,
            ObjectMapper objectMapper,
            String username,
            String password,
            String database,
            int longPollTimeoutMillis) {
        this.databaseUri = databaseUri(baseUri, database);
        this.objectMapper = java.util.Objects.requireNonNull(objectMapper, "objectMapper");
        this.httpClient = HttpClient.newHttpClient();
        this.authorization = authorization(username, password);
        if (longPollTimeoutMillis < 100 || longPollTimeoutMillis > 60_000) {
            throw new IllegalArgumentException("Timeout do feed CouchDB inválido");
        }
        this.longPollTimeoutMillis = longPollTimeoutMillis;
    }

    @Override
    public Uni<String> carregarCursor() {
        return carregarEstadoCursor().map(EstadoCursor::sequencia);
    }

    @Override
    public Uni<List<CouchDbMudanca>> lerMudancas(String desde) {
        String cursor = validarTexto(desde, CURSOR_INVALIDO);
        String query = "_changes?feed=longpoll&include_docs=true&timeout="
                + longPollTimeoutMillis
                + "&since="
                + URLEncoder.encode(cursor, StandardCharsets.UTF_8);
        return enviar(request(databaseUri.resolve(query)).GET().build())
                .map(resposta -> {
                    if (resposta.status() != 200
                            || resposta.body() == null
                            || !resposta.body().path("results").isArray()) {
                        throw new IllegalStateException("Resposta _changes inválida");
                    }
                    List<CouchDbMudanca> mudancas = new ArrayList<>();
                    for (JsonNode item : resposta.body().path("results")) {
                        if (!item.isObject()) {
                            throw new IllegalStateException("Resposta _changes inválida");
                        }
                        JsonNode documento = item.path("doc");
                        mudancas.add(new CouchDbMudanca(
                                sequencia(item.path("seq")),
                                documento.isObject() ? documento : null));
                    }
                    return List.copyOf(mudancas);
                });
    }

    @Override
    public Uni<Void> salvarCursor(String sequencia) {
        return salvarCursor(
                validarTexto(sequencia, CURSOR_INVALIDO),
                1);
    }

    private Uni<Void> salvarCursor(String sequencia, int tentativa) {
        return carregarEstadoCursor().chain(atual -> {
            if (sequencia.equals(atual.sequencia())) {
                return Uni.createFrom().voidItem();
            }
            ObjectNode documento = objectMapper.createObjectNode()
                    .put("_id", CURSOR_ID)
                    .put("tipo", "cursor-feed-revisao")
                    .put("versaoSchema", 1)
                    .put("lastSeq", sequencia);
            if (atual.revisao() != null) {
                documento.put("_rev", atual.revisao());
            }
            return gravarCursor(documento).chain(resposta -> {
                if (resposta.status() == 201 || resposta.status() == 202) {
                    return Uni.createFrom().voidItem();
                }
                if (resposta.status() == 409 && tentativa < MAX_TENTATIVAS_CURSOR) {
                    return salvarCursor(sequencia, tentativa + 1);
                }
                return Uni.createFrom().failure(
                        new IllegalStateException("Não foi possível persistir cursor CouchDB"));
            });
        });
    }

    private Uni<EstadoCursor> carregarEstadoCursor() {
        return enviar(request(databaseUri.resolve(CURSOR_ID)).GET().build())
                .map(resposta -> {
                    if (resposta.status() == 404) {
                        return new EstadoCursor(CURSOR_INICIAL, null);
                    }
                    if (resposta.status() != 200
                            || resposta.body() == null
                            || !resposta.body().isObject()) {
                        throw new IllegalStateException(CURSOR_INVALIDO);
                    }
                    return new EstadoCursor(
                            validarTexto(
                                    resposta.body().path("lastSeq").textValue(),
                                    CURSOR_INVALIDO),
                            validarTexto(
                                    resposta.body().path("_rev").textValue(),
                                    CURSOR_INVALIDO));
                });
    }

    private Uni<Resposta> gravarCursor(ObjectNode documento) {
        try {
            var request = request(databaseUri.resolve(CURSOR_ID))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(documento)))
                    .build();
            return enviar(request);
        } catch (IOException falha) {
            return Uni.createFrom().failure(
                    new IllegalStateException("Falha ao serializar cursor CouchDB", falha));
        }
    }

    private Uni<Resposta> enviar(HttpRequest request) {
        return Uni.createFrom()
                .completionStage(() -> httpClient.sendAsync(
                        request,
                        ofString(StandardCharsets.UTF_8)))
                .map(response -> resposta(response.statusCode(), response.body()))
                .onFailure()
                .transform(falha -> new IllegalStateException(
                        "Falha na chamada ao feed CouchDB",
                        falha));
    }

    private Resposta resposta(int status, String body) {
        try {
            return new Resposta(
                    status,
                    body == null || body.isBlank() ? null : objectMapper.readTree(body));
        } catch (IOException falha) {
            throw new IllegalStateException("Resposta do feed CouchDB inválida", falha);
        }
    }

    private HttpRequest.Builder request(URI uri) {
        var builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(longPollTimeoutMillis + 10_000L));
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return builder;
    }

    private static URI databaseUri(URI baseUri, String database) {
        if (baseUri == null
                || baseUri.getScheme() == null
                || baseUri.getHost() == null
                || (!"http".equals(baseUri.getScheme())
                        && !"https".equals(baseUri.getScheme()))
                || baseUri.getUserInfo() != null
                || baseUri.getQuery() != null
                || baseUri.getFragment() != null
                || !(baseUri.getPath().isEmpty() || "/".equals(baseUri.getPath()))) {
            throw new IllegalArgumentException("URL CouchDB inválida");
        }
        if (database == null || !database.matches("[a-z][a-z0-9_$()+-]{0,237}")) {
            throw new IllegalArgumentException("Nome de database CouchDB inválido");
        }
        String raiz = baseUri.toString();
        if (!raiz.endsWith("/")) {
            raiz += "/";
        }
        return URI.create(raiz + database + "/");
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
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));
    }

    private static String sequencia(JsonNode valor) {
        if (valor == null || (!valor.isTextual() && !valor.isNumber())) {
            throw new IllegalStateException("Resposta _changes inválida");
        }
        return validarTexto(valor.asText(), "Resposta _changes inválida");
    }

    private static String validarTexto(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
        return valor;
    }

    private record EstadoCursor(String sequencia, String revisao) {
    }

    private record Resposta(int status, JsonNode body) {
    }
}
