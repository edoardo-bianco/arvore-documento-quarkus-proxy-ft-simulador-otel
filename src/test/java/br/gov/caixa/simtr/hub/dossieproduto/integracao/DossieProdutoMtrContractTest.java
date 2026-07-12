package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(DossieProdutoMtrContractTest.MtrProfile.class)
@QuarkusTestResource(
        value = DossieProdutoMtrStubTestResource.class,
        restrictToAnnotatedClass = true
)
class DossieProdutoMtrContractTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String REQUEST_CRIACAO = """
            {
              "processo": 100,
              "chave_correlacao_canal": 200,
              "numero_negocio": 300,
              "clientes": [{
                "cpf": "12345678901",
                "cnpj": "12345678000190",
                "tipo_vinculo": 1,
                "cliente_relacionado": {"cpf": "98765432100"},
                "sequencia_titularidade": 1
              }]
            }
            """;

    @Inject
    InMemorySpanExporter spanExporter;

    @Inject
    OpenTelemetry openTelemetry;

    @BeforeEach
    void resetStub() {
        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider().forceFlush().join(10, TimeUnit.SECONDS);
        spanExporter.reset();
        DossieProdutoMtrStubTestResource.reset();
    }

    @Test
    void percorreResourceAteStubMtrPreservandoWireHeadersEResposta() throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(201, "{\"id\":987}");

        JsonNode response = postCriacao().then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree("{\"id\":987}"), response);

        List<DossieProdutoMtrStubTestResource.CapturedRequest> requests =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(1, requests.size());

        DossieProdutoMtrStubTestResource.CapturedRequest request = requests.getFirst();
        assertEquals("POST", request.method());
        assertEquals(DossieProdutoMtrStubTestResource.CAMINHO_CRIACAO, request.path());
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_CRIACAO), OBJECT_MAPPER.readTree(request.body()));
        assertTrue(request.contentType().startsWith("application/json"));
        assertTrue(request.accept().contains("application/json"));
        assertEquals("test-apikey", request.apikey());
        assertEquals("Bearer stub-access-token", request.authorization());
        assertNotNull(request.traceparent());
        assertTrue(request.traceparent().matches("00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}"));

        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider().forceFlush().join(10, TimeUnit.SECONDS);
        SpanData observed = spanExporter.getFinishedSpanItems().stream()
                .filter(span -> "DossieProdutoClient".equals(
                        span.getAttributes().get(AttributeKey.stringKey("rest_client.class"))))
                .findFirst()
                .orElseThrow();
        assertEquals("criarDossieProduto",
                observed.getAttributes().get(AttributeKey.stringKey("rest_client.operation")));
        assertEquals(201L,
                observed.getAttributes().get(AttributeKey.longKey("rest_client.response.status_code")));
        Set<String> eventNames = observed.getEvents().stream()
                .map(event -> event.getName())
                .collect(Collectors.toSet());
        assertTrue(eventNames.contains("mtr.rest-client.request.enviada"));
        assertTrue(eventNames.contains("mtr.rest-client.response.recebida"));
    }

    @Test
    void preservaErroDeNegocioCompletoSemRetry() throws JsonProcessingException {
        String erroMtr = """
                {
                  "codigo_http": 400,
                  "recurso": "simtr-dossie-produto",
                  "id_erro": "stub-negocio-400",
                  "codigo_erro": "MTR-DOS-400",
                  "erros": [{"mensagem": "processo nao permitido"}],
                  "detalhe": "falha de negocio controlada"
                }
                """;
        DossieProdutoMtrStubTestResource.responder(400, erroMtr);

        JsonNode response = postCriacao().then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree(erroMtr), response);
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void retryReenviaMesmoWireAposErroMtrRecuperavel() throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(500, """
                {"codigo_http":500,"recurso":"simtr-dossie-produto","codigo_erro":"MTR-DOS-500"}
                """);
        DossieProdutoMtrStubTestResource.responder(201, "{\"id\":988}");

        JsonNode response = postCriacao().then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree("{\"id\":988}"), response);
        List<DossieProdutoMtrStubTestResource.CapturedRequest> requests =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(2, requests.size());
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_CRIACAO), OBJECT_MAPPER.readTree(requests.get(0).body()));
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_CRIACAO), OBJECT_MAPPER.readTree(requests.get(1).body()));
    }

    @Test
    void timeoutCancelaTentativaLentaEExecutaRetry() throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responderComAtraso(
                Duration.ofMillis(2_100),
                201,
                "{\"id\":999}"
        );
        DossieProdutoMtrStubTestResource.responder(201, "{\"id\":1000}");

        JsonNode response = postCriacao().then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree("{\"id\":1000}"), response);
        assertEquals(2, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void circuitBreakerAbertoInterrompeNovasChamadasAoStub() {
        DossieProdutoMtrStubTestResource.responderRepetidamente(
                50,
                500,
                "{\"codigo_http\":500,\"recurso\":\"simtr-dossie-produto\"}"
        );

        for (int i = 0; i < 4; i++) {
            postWorkflow().then().statusCode(500);
        }
        int requestsAfterOpening = DossieProdutoMtrStubTestResource.requisicoes().size();

        postWorkflow().then().statusCode(500);

        List<DossieProdutoMtrStubTestResource.CapturedRequest> requests =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(requestsAfterOpening, requests.size());
        assertEquals(10, requestsAfterOpening);
        assertTrue(requests.stream().allMatch(request -> request.path().equals(
                "/simtr/dossie-produto/v1/dossie-produto/123/workflow"
        )));
    }

    private static Response postCriacao() {
        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(REQUEST_CRIACAO)
                .when()
                .post("/simtr-hub/v1/dossie-produto");
    }

    private static Response postWorkflow() {
        return given()
                .accept(ContentType.JSON)
                .when()
                .post("/simtr-hub/v1/dossie-produto/{id}/workflow", 123L);
    }

    public static class MtrProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "simtr-hub.simulador.dossie-produto.habilitado", "false",
                    "quarkus.oidc-client.enabled", "true",
                    "quarkus.oidc-client.connection-retry-count", "1"
            );
        }
    }
}
