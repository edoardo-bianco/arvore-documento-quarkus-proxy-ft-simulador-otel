package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(
        value = DossieProdutoMtrStubTestResource.class,
        restrictToAnnotatedClass = true
)
class CapturaDossieProdutoMtrContractTest {

    private static final String CAMINHO_CAPTURA =
            "/simtr/dossie-produto/v1/dossie-produto/123/capturar";
    private static final String CAMINHO_PUBLICO =
            "/simtr-hub/v1/dossie-produto/{id}/capturar";
    private static final String SPAN_API = "simtr-hub.api.dossie-produto.capturar";
    private static final String SPAN_SERVICO = "simtr-hub.service.dossie-produto.capturar";
    private static final String SPAN_CLIENTE = "mtr.dossie-produto.capturar";
    private static final String SERVICO_MTR = "simtr-dossie-produto";
    private static final String MENSAGEM_EXTERNA = "MENSAGEM_EXTERNA_SENTINELA";
    private static final String DETALHE_EXTERNO = "DETALHE_EXTERNO_SENTINELA";
    private static final String STACKTRACE_EXTERNO = "STACKTRACE_EXTERNO_SENTINELA";
    private static final String API_KEY_TESTE = "test-apikey";
    private static final String CORPO_SUCESSO = "{\"id\":123}";
    private static final Set<String> SPANS_CAPTURA = Set.of(
            SPAN_API,
            SPAN_SERVICO,
            SPAN_CLIENTE);

    @Inject
    InMemorySpanExporter spanExporter;

    @Inject
    OpenTelemetry openTelemetry;

    private final CapturingHandler logHandler = new CapturingHandler();
    private Logger rootLogger;

    @BeforeEach
    void resetStub() {
        rootLogger = Logger.getLogger("");
        rootLogger.addHandler(logHandler);
        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        spanExporter.reset();
        DossieProdutoMtrStubTestResource.reset();
    }

    @AfterEach
    void removerCapturaDeLogs() {
        rootLogger.removeHandler(logHandler);
        logHandler.close();
    }

    @Test
    void preservaHierarquiaPathsTemplatedESigiloNosSpansELogsMtr() {
        DossieProdutoMtrStubTestResource.responder(500, """
                {
                  "codigo_http": 500,
                  "erros": [{"mensagem": "%s"}],
                  "detalhe": "%s",
                  "stacktrace": "%s"
                }
                """.formatted(MENSAGEM_EXTERNA, DETALHE_EXTERNO, STACKTRACE_EXTERNO));

        postCaptura().then().statusCode(500);

        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        List<SpanData> finalizados = spanExporter.getFinishedSpanItems();
        Map<String, SpanData> spans = finalizados.stream()
                .filter(span -> SPANS_CAPTURA.contains(span.getName()))
                .collect(Collectors.toMap(SpanData::getName, span -> span));

        assertEquals(SPANS_CAPTURA, spans.keySet());
        SpanData api = spans.get(SPAN_API);
        SpanData servico = spans.get(SPAN_SERVICO);
        SpanData cliente = spans.get(SPAN_CLIENTE);
        List<SpanData> spansDoTrace = finalizados.stream()
                .filter(span -> api.getTraceId().equals(span.getTraceId()))
                .toList();
        String sinaisDosSpans = spansDoTrace.stream()
                .map(span -> span.getName()
                        + span.getAttributes()
                        + span.getEvents()
                        + span.getStatus())
                .collect(Collectors.joining("\n"));

        assertEquals(SpanKind.SERVER, api.getKind());
        assertEquals(SpanKind.INTERNAL, servico.getKind());
        assertEquals(SpanKind.CLIENT, cliente.getKind());
        assertEquals(api.getTraceId(), servico.getTraceId());
        assertEquals(servico.getTraceId(), cliente.getTraceId());
        assertEquals(api.getSpanId(), servico.getParentSpanId());
        assertEquals(servico.getSpanId(), cliente.getParentSpanId());
        assertEquals(CAMINHO_PUBLICO, atributo(api, "http.route"));
        assertEquals(
                "/simtr/dossie-produto/v1/dossie-produto/{id}/capturar",
                atributo(cliente, "url.path"));
        List<SpanData> spansClient = spansDoTrace.stream()
                .filter(span -> SpanKind.CLIENT.equals(span.getKind()))
                .toList();
        assertEquals(1, spansClient.size(), sinaisDosSpans);
        assertEquals(SPAN_CLIENTE, spansClient.getFirst().getName());

        String sinaisContratuais = sinaisDosSpans
                + "\n"
                + logHandler.sinaisDaCaptura();

        assertFalse(sinaisContratuais.contains(MENSAGEM_EXTERNA), sinaisContratuais);
        assertFalse(sinaisContratuais.contains(DETALHE_EXTERNO), sinaisContratuais);
        assertFalse(sinaisContratuais.contains(STACKTRACE_EXTERNO), sinaisContratuais);
        assertFalse(sinaisContratuais.contains(API_KEY_TESTE), sinaisContratuais);
        assertFalse(sinaisContratuais.contains("stub-access-token"), sinaisContratuais);
        assertFalse(sinaisContratuais.contains("url.full"), sinaisContratuais);
        assertFalse(sinaisContratuais.contains("http://127.0.0.1"), sinaisContratuais);
        String traceparent = DossieProdutoMtrStubTestResource.requisicoes()
                .getFirst()
                .traceparent();
        assertNotNull(traceparent);
        String[] camposTraceparent = traceparent.split("-");
        assertEquals(4, camposTraceparent.length);
        assertEquals(cliente.getTraceId(), camposTraceparent[1]);
        assertEquals(cliente.getSpanId(), camposTraceparent[2]);
        assertFalse(sinaisContratuais.contains(traceparent), sinaisContratuais);
    }

    @Test
    void percorreRotaPublicaAteWireMtrComCorpoVazioEHeadersObrigatorios() {
        DossieProdutoMtrStubTestResource.responder(200, CORPO_SUCESSO);

        JsonNode resposta = postCaptura().then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(123L, resposta.path("id").longValue());
        List<DossieProdutoMtrStubTestResource.CapturedRequest> requisicoes =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(1, requisicoes.size());

        DossieProdutoMtrStubTestResource.CapturedRequest requisicao = requisicoes.getFirst();
        assertEquals("POST", requisicao.method());
        assertEquals(CAMINHO_CAPTURA, requisicao.path());
        assertEquals("", requisicao.body());
        assertTrue(requisicao.accept().contains("application/json"));
        assertEquals(API_KEY_TESTE, requisicao.apikey());
        assertEquals("Bearer stub-access-token", requisicao.authorization());
        assertNotNull(requisicao.traceparent());
        assertTrue(requisicao.traceparent()
                .matches("00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}"));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 409, 500})
    void preservaStatusECorpoDosErrosMtrAprovadosSemRetry(int status) {
        String corpo = """
                {
                  "codigo_http": %d,
                  "recurso": "%s",
                  "id_erro": "captura-%d",
                  "codigo_erro": "MTR-CAPTURA-%d",
                  "erros": [{"mensagem": "captura recusada"}],
                  "detalhe": "erro controlado"
                }
                """.formatted(status, SERVICO_MTR, status, status);
        DossieProdutoMtrStubTestResource.responder(status, corpo);

        JsonNode erro = postCaptura().then()
                .statusCode(status)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(status, erro.path("codigo_http").intValue());
        assertEquals(SERVICO_MTR, erro.path("recurso").textValue());
        assertEquals("captura-" + status, erro.path("id_erro").textValue());
        assertEquals("MTR-CAPTURA-" + status, erro.path("codigo_erro").textValue());
        assertEquals("captura recusada", erro.path("erros").get(0).path("mensagem").textValue());
        assertEquals("erro controlado", erro.path("detalhe").textValue());
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void normalizaCorpoDeErroMtrInvalidoSemRetry() {
        DossieProdutoMtrStubTestResource.responder(500, "{");

        JsonNode erro = postCaptura().then()
                .statusCode(500)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(500, erro.path("codigo_http").intValue());
        assertEquals(SERVICO_MTR, erro.path("recurso").textValue());
        assertEquals("ARVDOCP0002", erro.path("codigo_erro").textValue());
        assertEquals(
                "Erro retornado pelo serviço MTR fora do contrato esperado.",
                erro.path("erros").get(0).path("mensagem").textValue());
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void timeoutProduzErroSeguroComUmaUnicaChamadaMtr() {
        DossieProdutoMtrStubTestResource.responderComAtraso(
                Duration.ofMillis(2_100),
                200,
                CORPO_SUCESSO
        );

        JsonNode erro = postCaptura().then()
                .statusCode(500)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(500, erro.path("codigo_http").intValue());
        assertEquals("simtr-hub", erro.path("recurso").textValue());
        assertEquals("ARVDOCP9999", erro.path("codigo_erro").textValue());
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    private static Response postCaptura() {
        return given()
                .accept(ContentType.JSON)
                .when()
                .post(CAMINHO_PUBLICO, 123L);
    }

    private static Object atributo(SpanData span, String nome) {
        return span.getAttributes().get(AttributeKey.stringKey(nome));
    }

    private static final class CapturingHandler extends Handler {

        private final List<String> sinais = new CopyOnWriteArrayList<>();

        @Override
        public void publish(LogRecord registro) {
            if (registro.getMessage() != null && pertenceACaptura(registro)) {
                sinais.add(serializar(registro));
            }
        }

        String sinaisDaCaptura() {
            return String.join("\n", sinais);
        }

        private static boolean pertenceACaptura(LogRecord registro) {
            String evento = registro.getMessage();
            return evento.startsWith("simtr-hub.dossie-produto.captura.")
                    || evento.startsWith("mtr.dossie-produto.captura.")
                    || evento.startsWith("mtr.erro.");
        }

        private static String serializar(LogRecord registro) {
            StringBuilder sinal = new StringBuilder(registro.getMessage());
            if (registro instanceof ExtLogRecord extLogRecord) {
                sinal.append(extLogRecord.getMdcCopy());
            }
            if (registro.getThrown() != null) {
                StringWriter stacktrace = new StringWriter();
                registro.getThrown().printStackTrace(new PrintWriter(stacktrace));
                sinal.append(stacktrace);
            }
            return sinal.toString();
        }

        @Override
        public void flush() {
            // No-op: o handler mantém apenas registros já publicados em memória.
        }

        @Override
        public void close() {
            sinais.clear();
        }
    }
}
