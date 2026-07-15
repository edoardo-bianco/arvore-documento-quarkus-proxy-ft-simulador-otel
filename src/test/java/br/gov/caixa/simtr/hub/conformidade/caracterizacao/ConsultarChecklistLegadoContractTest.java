package br.gov.caixa.simtr.hub.conformidade.caracterizacao;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.client.ParametrizacaoChecklistClient;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.erro.ChecklistMtrException;
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(
        value = ChecklistMtrStubTestResource.class,
        restrictToAnnotatedClass = true
)
class ConsultarChecklistLegadoContractTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String PATH_PUBLICO =
            "/simtr-hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}";
    private static final long IDENTIFICADOR = 1000012583L;
    private static final int VERSAO = 7;
    private static final String RESPOSTA_MTR_COM_NULOS = """
            {
              "nome": "Checklist caracterizado no stub",
              "identificador_negocial": 1000012583,
              "versao": 7,
              "data_hora_criacao": null,
              "data_hora_ultima_alteracao": "14/07/2026 12:00:00",
              "verificacao_previa": false,
              "orientacao_operador": null,
              "apontamentos": [null, {
                "identificador_negocial": 2001,
                "nome": null,
                "descricao": "Descricao preservada",
                "orientacao_operador": null,
                "indicador_reanalise": false,
                "sequencia_apresentacao": 1
              }]
            }
            """;
    private static final String RESPOSTA_PUBLICA_COM_NULOS_OMITIDOS = """
            {
              "nome": "Checklist caracterizado no stub",
              "identificador_negocial": 1000012583,
              "versao": 7,
              "data_hora_ultima_alteracao": "14/07/2026 12:00:00",
              "verificacao_previa": false,
              "apontamentos": [null, {
                "identificador_negocial": 2001,
                "descricao": "Descricao preservada",
                "indicador_reanalise": false,
                "sequencia_apresentacao": 1
              }]
            }
            """;

    @Inject
    InMemorySpanExporter spanExporter;

    @Inject
    OpenTelemetry openTelemetry;

    @BeforeEach
    void resetarStub() {
        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        spanExporter.reset();
        ChecklistMtrStubTestResource.reset();
    }

    @Test
    void preservaHttpJsonIdentificadorVersaoNulabilidadeEWireDoLegado() throws Exception {
        ChecklistMtrStubTestResource.responder(200, RESPOSTA_MTR_COM_NULOS);

        JsonNode response = consultar(IDENTIFICADOR, VERSAO).then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree(RESPOSTA_PUBLICA_COM_NULOS_OMITIDOS), response);
        List<ChecklistMtrStubTestResource.CapturedRequest> requests =
                ChecklistMtrStubTestResource.requisicoes();
        assertEquals(1, requests.size());

        ChecklistMtrStubTestResource.CapturedRequest request = requests.getFirst();
        assertEquals("GET", request.method());
        assertEquals(
                ChecklistMtrStubTestResource.CAMINHO_CHECKLIST + IDENTIFICADOR + "/versao/" + VERSAO,
                request.path());
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
                .filter(span -> "mtr.parametrizacao.checklist.consultar".equals(span.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(SpanKind.CLIENT, gateway.getKind());
        assertEquals("simtr-parametrizacao",
                gateway.getAttributes().get(AttributeKey.stringKey("mtr.servico")));
        assertEquals("cadastro-checklist-v1",
                gateway.getAttributes().get(AttributeKey.stringKey("mtr.api")));
        assertEquals("GET",
                gateway.getAttributes().get(AttributeKey.stringKey("http.request.method")));
        assertEquals(
                "/simtr-parametrizacao/v1/cadastro/checklist/identificador-negocial/{identificador}/versao/{versao}",
                gateway.getAttributes().get(AttributeKey.stringKey("url.path")));
        assertEquals(true,
                gateway.getAttributes().get(AttributeKey.booleanKey("mtr.resposta.sucesso")));

        SpanData restClient = spanExporter.getFinishedSpanItems().stream()
                .filter(span -> "ParametrizacaoChecklistClient".equals(
                        span.getAttributes().get(AttributeKey.stringKey("rest_client.class"))))
                .findFirst()
                .orElseThrow();
        assertEquals("consultarPorIdentificadorNegocialEVersao",
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
    void preservaValidacaoDoIdentificadorSemChamarMtr() {
        JsonNode erro = consultar(0, 1).then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertErroValidacao(erro, "O identificador negocial deve ser maior que zero.");
        assertTrue(ChecklistMtrStubTestResource.requisicoes().isEmpty());
    }

    @Test
    void preservaValidacaoDaVersaoSemChamarMtr() {
        JsonNode erro = consultar(IDENTIFICADOR, 0).then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertErroValidacao(erro, "A versão do checklist deve ser maior que zero.");
        assertTrue(ChecklistMtrStubTestResource.requisicoes().isEmpty());
    }

    @Test
    void preservaRespostaSemConteudoDoMtr() {
        ChecklistMtrStubTestResource.responder(204, "");

        consultar(IDENTIFICADOR, VERSAO).then()
                .statusCode(204);

        assertEquals(1, ChecklistMtrStubTestResource.requisicoes().size());
    }

    @Test
    void preservaErroDeNegocioCompletoSemRetry() throws Exception {
        String erroMtr = """
                {
                  "codigo_http": 404,
                  "recurso": "simtr-parametrizacao",
                  "id_erro": "checklist-stub-404",
                  "codigo_erro": "MTR-CHECKLIST-404",
                  "erros": [{"mensagem": "checklist nao localizado"}],
                  "detalhe": "falha de negocio controlada"
                }
                """;
        ChecklistMtrStubTestResource.responder(404, erroMtr);

        JsonNode response = consultar(IDENTIFICADOR, VERSAO).then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree(erroMtr), response);
        assertEquals(1, ChecklistMtrStubTestResource.requisicoes().size());
    }

    @Test
    void retryRecuperaErroServidorSemAlterarOGet() throws Exception {
        ChecklistMtrStubTestResource.responder(500,
                "{\"codigo_http\":500,\"recurso\":\"simtr-parametrizacao\"}");
        ChecklistMtrStubTestResource.responder(200, RESPOSTA_MTR_COM_NULOS);

        JsonNode response = consultar(IDENTIFICADOR, VERSAO).then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree(RESPOSTA_PUBLICA_COM_NULOS_OMITIDOS), response);
        List<ChecklistMtrStubTestResource.CapturedRequest> requests =
                ChecklistMtrStubTestResource.requisicoes();
        assertEquals(2, requests.size());
        assertTrue(requests.stream().allMatch(request -> "GET".equals(request.method())));
        assertTrue(requests.stream().allMatch(request -> request.path().equals(
                ChecklistMtrStubTestResource.CAMINHO_CHECKLIST
                        + IDENTIFICADOR + "/versao/" + VERSAO)));
        assertTrue(requests.stream().allMatch(request -> request.body().isEmpty()));
    }

    @Test
    void preservaMatrizDeFaultToleranceDeclaradaNoClientAtivo() throws Exception {
        Method method = ParametrizacaoChecklistClient.class.getMethod(
                "consultarPorIdentificadorNegocialEVersao", Long.class, Integer.class);

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
                ChecklistMtrException.Servidor.class,
                ProcessingException.class,
                TimeoutException.class
        }, retry.retryOn());
        assertArrayEquals(new Class<?>[]{
                ChecklistMtrException.Negocio.class,
                ChecklistMtrException.TecnicaCliente.class
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

    private static void assertErroValidacao(JsonNode erro, String mensagem) {
        assertEquals(400, erro.path("codigo_http").asInt());
        assertEquals("simtr-hub", erro.path("recurso").asText());
        assertEquals("ARVDOCP0001", erro.path("codigo_erro").asText());
        assertDoesNotThrow(() -> UUID.fromString(erro.path("id_erro").asText()));
        assertEquals(1, erro.path("erros").size());
        assertEquals(mensagem, erro.path("erros").get(0).path("mensagem").asText());
    }

    private static io.restassured.response.Response consultar(long identificador, int versao) {
        return given()
                .accept(ContentType.JSON)
                .when()
                .get(PATH_PUBLICO, identificador, versao);
    }
}
