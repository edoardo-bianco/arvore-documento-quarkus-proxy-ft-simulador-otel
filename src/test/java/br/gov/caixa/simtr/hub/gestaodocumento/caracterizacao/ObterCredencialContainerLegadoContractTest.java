package br.gov.caixa.simtr.hub.gestaodocumento.caracterizacao;

import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.mtr.client.GestaoDocumentoClient;
import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.mtr.erro.GestaoDocumentoMtrException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(
        value = GestaoDocumentoMtrStubTestResource.class,
        restrictToAnnotatedClass = true
)
class ObterCredencialContainerLegadoContractTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String PATH_PUBLICO =
            "/simtr-hub/v1/storage/container/credencial";
    private static final String RESPOSTA_MTR = """
            {
              "sas": "sv=caracterizacao&sp=rw&sig=valor-opaco",
              "validade": "31/12/2099 23:59:47",
              "url_storage": "https://caracterizacao.blob.core.windows.net",
              "nome_container": "container-caracterizado"
            }
            """;
    private static final String RESPOSTA_COM_NULOS = """
            {
              "sas": null,
              "validade": null,
              "url_storage": null,
              "nome_container": null
            }
            """;

    @Inject
    InMemorySpanExporter spanExporter;

    @Inject
    OpenTelemetry openTelemetry;

    @BeforeEach
    void resetarStubETelemetria() {
        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        spanExporter.reset();
        GestaoDocumentoMtrStubTestResource.reset();
    }

    @Test
    void preservaHttpJsonWireEValidadeFornecidaPeloMtrSemFixarDuracao() throws Exception {
        GestaoDocumentoMtrStubTestResource.responder(200, RESPOSTA_MTR);

        JsonNode response = gerar().then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree(RESPOSTA_MTR), response);
        assertEquals("31/12/2099 23:59:47", response.path("validade").asText());

        List<GestaoDocumentoMtrStubTestResource.CapturedRequest> requests =
                GestaoDocumentoMtrStubTestResource.requisicoes();
        assertEquals(1, requests.size());

        GestaoDocumentoMtrStubTestResource.CapturedRequest request = requests.getFirst();
        assertEquals("POST", request.method());
        assertEquals(GestaoDocumentoMtrStubTestResource.CAMINHO_CREDENCIAL, request.path());
        assertEquals("", request.body());
        assertNull(request.contentType());
        assertTrue(request.accept().contains("application/json"));
        assertEquals("test-apikey", request.apikey());
        assertEquals("Bearer stub-access-token", request.authorization());
        assertNotNull(request.traceparent());
        assertTrue(request.traceparent().matches("00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}"));

        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        SpanData gateway = spanExporter.getFinishedSpanItems().stream()
                .filter(span -> "mtr.gestao-documento.credencial-container.gerar"
                        .equals(span.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(SpanKind.CLIENT, gateway.getKind());
        assertEquals("simtr-gestao-documento",
                gateway.getAttributes().get(AttributeKey.stringKey("mtr.servico")));
        assertEquals("gestao-documento-v1",
                gateway.getAttributes().get(AttributeKey.stringKey("mtr.api")));
        assertEquals("POST",
                gateway.getAttributes().get(AttributeKey.stringKey("http.request.method")));
        assertEquals(GestaoDocumentoMtrStubTestResource.CAMINHO_CREDENCIAL,
                gateway.getAttributes().get(AttributeKey.stringKey("url.path")));
        assertEquals(true,
                gateway.getAttributes().get(AttributeKey.booleanKey("mtr.resposta.sucesso")));
        assertEquals("container-caracterizado", gateway.getAttributes().get(
                AttributeKey.stringKey("gestao_documento.container.nome")));

        SpanData restClient = spanExporter.getFinishedSpanItems().stream()
                .filter(span -> "GestaoDocumentoClient".equals(
                        span.getAttributes().get(AttributeKey.stringKey("rest_client.class"))))
                .findFirst()
                .orElseThrow();
        assertEquals("gerarCredencialContainer",
                restClient.getAttributes().get(AttributeKey.stringKey("rest_client.operation")));
        assertTrue(spanExporter.getFinishedSpanItems().stream()
                .map(span -> span.getAttributes().get(
                        AttributeKey.longKey("rest_client.response.status_code")))
                .anyMatch(status -> Long.valueOf(200L).equals(status)));
        Set<String> eventos = spanExporter.getFinishedSpanItems().stream()
                .flatMap(span -> span.getEvents().stream())
                .map(event -> event.getName())
                .collect(Collectors.toSet());
        assertTrue(eventos.contains("mtr.rest-client.request.enviada"));
        assertTrue(eventos.contains("mtr.rest-client.response.recebida"));
    }

    @Test
    void preservaNulabilidadeDoJsonDoLegado() throws Exception {
        GestaoDocumentoMtrStubTestResource.responder(200, RESPOSTA_COM_NULOS);

        JsonNode response = gerar().then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree(RESPOSTA_COM_NULOS), response);
        assertEquals(1, GestaoDocumentoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void preservaErroDeNegocioCompletoSemRetry() throws Exception {
        String erroMtr = """
                {
                  "codigo_http": 404,
                  "recurso": "simtr-gestao-documento",
                  "id_erro": "credencial-stub-404",
                  "codigo_erro": "MTR-CREDENCIAL-404",
                  "erros": [{"mensagem": "container nao localizado"}],
                  "detalhe": "falha de negocio controlada"
                }
                """;
        GestaoDocumentoMtrStubTestResource.responder(404, erroMtr);

        JsonNode response = gerar().then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree(erroMtr), response);
        assertEquals(1, GestaoDocumentoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void retryRecuperaErroServidorSemAlterarOPost() throws Exception {
        GestaoDocumentoMtrStubTestResource.responder(
                500, "{\"codigo_http\":500,\"recurso\":\"simtr-gestao-documento\"}");
        GestaoDocumentoMtrStubTestResource.responder(200, RESPOSTA_MTR);

        JsonNode response = gerar().then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree(RESPOSTA_MTR), response);
        List<GestaoDocumentoMtrStubTestResource.CapturedRequest> requests =
                GestaoDocumentoMtrStubTestResource.requisicoes();
        assertEquals(2, requests.size());
        assertTrue(requests.stream().allMatch(request -> "POST".equals(request.method())));
        assertTrue(requests.stream().allMatch(request ->
                GestaoDocumentoMtrStubTestResource.CAMINHO_CREDENCIAL.equals(request.path())));
        assertTrue(requests.stream().allMatch(request -> request.body().isEmpty()));
    }

    @Test
    void preservaMatrizDeFaultToleranceDeclaradaNoClientMigrado() throws Exception {
        Method method = GestaoDocumentoClient.class.getMethod("gerarCredencialContainer");

        Timeout timeout = method.getAnnotation(Timeout.class);
        assertEquals(2_000, timeout.value());
        assertEquals(ChronoUnit.MILLIS, timeout.unit());

        Retry retry = method.getAnnotation(Retry.class);
        assertEquals(3, retry.maxRetries());
        assertEquals(300, retry.delay());
        assertEquals(ChronoUnit.MILLIS, retry.delayUnit());
        assertEquals(100, retry.jitter());
        assertEquals(ChronoUnit.MILLIS, retry.jitterDelayUnit());
        assertArrayEquals(new Class<?>[]{
                GestaoDocumentoMtrException.Servidor.class,
                ProcessingException.class,
                TimeoutException.class
        }, retry.retryOn());
        assertArrayEquals(new Class<?>[]{
                GestaoDocumentoMtrException.Negocio.class,
                GestaoDocumentoMtrException.TecnicaCliente.class
        }, retry.abortOn());

        CircuitBreaker circuitBreaker = method.getAnnotation(CircuitBreaker.class);
        assertEquals(10, circuitBreaker.requestVolumeThreshold());
        assertEquals(0.5, circuitBreaker.failureRatio());
        assertEquals(10_000, circuitBreaker.delay());
        assertEquals(ChronoUnit.MILLIS, circuitBreaker.delayUnit());
        assertEquals(2, circuitBreaker.successThreshold());
        assertArrayEquals(retry.retryOn(), circuitBreaker.failOn());
        assertArrayEquals(retry.abortOn(), circuitBreaker.skipOn());
    }

    private static io.restassured.response.Response gerar() {
        return given()
                .accept(ContentType.JSON)
                .when()
                .post(PATH_PUBLICO);
    }
}
