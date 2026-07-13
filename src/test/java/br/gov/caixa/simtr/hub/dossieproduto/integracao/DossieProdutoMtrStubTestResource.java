package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DossieProdutoMtrStubTestResource implements QuarkusTestResourceLifecycleManager {

    static final String CAMINHO_CRIACAO = "/simtr/dossie-produto/v1/dossie-produto";

    private static final ConcurrentLinkedQueue<StubResponse> RESPOSTAS = new ConcurrentLinkedQueue<>();
    private static final CopyOnWriteArrayList<CapturedRequest> REQUISICOES = new CopyOnWriteArrayList<>();

    private HttpServer server;
    private ExecutorService executor;

    @Override
    public Map<String, String> start() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException("Nao foi possivel iniciar o stub MTR local", e);
        }

        executor = Executors.newFixedThreadPool(4);
        server.setExecutor(executor);
        server.createContext("/oidc/token", this::handleToken);
        server.createContext(CAMINHO_CRIACAO, this::handleDossieProduto);
        server.start();

        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        return Map.of(
                "quarkus.rest-client.dossie-produto.url", baseUrl + "/simtr",
                "quarkus.rest-client.dossie-produto.read-timeout", "5000",
                "quarkus.oidc-client.auth-server-url", baseUrl,
                "quarkus.oidc-client.token-path", "/oidc/token",
                "quarkus.oidc-client.connection-retry-count", "1",
                "simtr-hub.simulador.dossie-produto.habilitado", "false"
        );
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
        reset();
    }

    static void reset() {
        RESPOSTAS.clear();
        REQUISICOES.clear();
    }

    static void responder(int status, String body) {
        responderComAtraso(Duration.ZERO, status, body);
    }

    static void responderComAtraso(Duration atraso, int status, String body) {
        RESPOSTAS.add(new StubResponse(status, body, atraso));
    }

    static void responderRepetidamente(int quantidade, int status, String body) {
        for (int i = 0; i < quantidade; i++) {
            responder(status, body);
        }
    }

    static List<CapturedRequest> requisicoes() {
        return List.copyOf(REQUISICOES);
    }

    private void handleToken(HttpExchange exchange) throws IOException {
        writeJson(exchange, 200, """
                {"access_token":"stub-access-token","token_type":"Bearer","expires_in":3600}
                """);
    }

    private void handleDossieProduto(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        REQUISICOES.add(new CapturedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                body,
                exchange.getRequestHeaders().getFirst("Content-Type"),
                exchange.getRequestHeaders().getFirst("Accept"),
                exchange.getRequestHeaders().getFirst("apikey"),
                exchange.getRequestHeaders().getFirst("Authorization"),
                exchange.getRequestHeaders().getFirst("traceparent")
        ));

        StubResponse response = RESPOSTAS.poll();
        if (response == null) {
            writeJson(exchange, 500, "{\"detalhe\":\"resposta nao configurada no stub\"}");
            return;
        }

        delay(response.atraso());
        writeJson(exchange, response.status(), response.body());
    }

    private static void delay(Duration atraso) {
        if (atraso.isZero() || atraso.isNegative()) {
            return;
        }
        try {
            Thread.sleep(atraso);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void writeJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    record CapturedRequest(
            String method,
            String path,
            String body,
            String contentType,
            String accept,
            String apikey,
            String authorization,
            String traceparent
    ) {
    }

    private record StubResponse(int status, String body, Duration atraso) {
    }
}
