package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.WorkflowDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.WorkflowDossieProdutoMtrException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.ws.rs.ProcessingException;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(
        value = DossieProdutoMtrStubTestResource.class,
        restrictToAnnotatedClass = true
)
class WorkflowDossieProdutoMtrContractTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CAMINHO_WORKFLOW =
            "/simtr/dossie-produto/v1/dossie-produto/123/workflow";

    @BeforeEach
    void resetStub() {
        DossieProdutoMtrStubTestResource.reset();
    }

    @Test
    void preservaWireHeadersERespostaDoWorkflowComSimuladorDesabilitado()
            throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(200, "{\"id\":987}");

        JsonNode response = postWorkflow().then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree("{\"id\":987}"), response);

        List<DossieProdutoMtrStubTestResource.CapturedRequest> requests =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(1, requests.size());

        DossieProdutoMtrStubTestResource.CapturedRequest request = requests.getFirst();
        assertEquals("POST", request.method());
        assertEquals(CAMINHO_WORKFLOW, request.path());
        assertEquals("", request.body());
        assertTrue(request.accept().contains("application/json"));
        assertEquals("test-apikey", request.apikey());
        assertEquals("Bearer stub-access-token", request.authorization());
        assertNotNull(request.traceparent());
        assertTrue(request.traceparent().matches("00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}"));
    }

    @Test
    void preservaErroDeNegocioCompletoDoWorkflowSemRetry() throws JsonProcessingException {
        String erroMtr = """
                {
                  "codigo_http": 400,
                  "recurso": "simtr-dossie-produto",
                  "id_erro": "stub-workflow-400",
                  "codigo_erro": "MTR-DOS-WORKFLOW-400",
                  "erros": [{"mensagem": "workflow nao permitido"}],
                  "detalhe": "falha de negocio controlada"
                }
                """;
        DossieProdutoMtrStubTestResource.responder(400, erroMtr);

        JsonNode response = postWorkflow().then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree(erroMtr), response);
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void retryDoWorkflowRepeteAChamadaAposErroRecuperavel() throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(500, """
                {"codigo_http":500,"recurso":"simtr-dossie-produto","codigo_erro":"MTR-WORKFLOW-500"}
                """);
        DossieProdutoMtrStubTestResource.responder(200, "{\"id\":988}");

        JsonNode response = postWorkflow().then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree("{\"id\":988}"), response);
        assertEquals(2, DossieProdutoMtrStubTestResource.requisicoes().size());
        assertTrue(DossieProdutoMtrStubTestResource.requisicoes().stream()
                .allMatch(request -> CAMINHO_WORKFLOW.equals(request.path())));
    }

    @Test
    void timeoutDoWorkflowCancelaTentativaLentaEExecutaRetry() throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responderComAtraso(
                Duration.ofMillis(2_100),
                200,
                "{\"id\":999}"
        );
        DossieProdutoMtrStubTestResource.responder(200, "{\"id\":1000}");

        JsonNode response = postWorkflow().then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree("{\"id\":1000}"), response);
        assertEquals(2, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void congelaMatrizDeFaultToleranceDeclaradaNoWorkflow() throws NoSuchMethodException {
        Method method = WorkflowDossieProdutoMtrClient.class.getMethod(
                "iniciarOuAvancar",
                Long.class
        );

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
                WorkflowDossieProdutoMtrException.Servidor.class,
                ProcessingException.class,
                TimeoutException.class
        }, retry.retryOn());
        assertArrayEquals(new Class<?>[]{
                WorkflowDossieProdutoMtrException.Negocio.class,
                WorkflowDossieProdutoMtrException.TecnicaCliente.class
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

    private static Response postWorkflow() {
        return given()
                .accept(ContentType.JSON)
                .when()
                .post("/simtr-hub/v1/dossie-produto/{id}/workflow", 123L);
    }

}
