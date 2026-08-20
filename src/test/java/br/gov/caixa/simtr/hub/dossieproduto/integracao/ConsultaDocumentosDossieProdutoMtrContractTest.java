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
import java.util.ArrayList;
import java.util.List;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(
        value = DossieProdutoMtrStubTestResource.class,
        restrictToAnnotatedClass = true
)
class ConsultaDocumentosDossieProdutoMtrContractTest {

    private static final long IDENTIFICADOR = 4_081_899L;
    private static final String CAMINHO_PUBLICO =
            "/simtr-hub/v1/dossie-produto/{id}/documentos";
    private static final String CNPJ = "00000000000191";
    private static final String CPF = "00000000191";
    private static final String IP = "192.0.2.10";
    private static final String TIPOLOGIA = "TIPOLOGIA-WIRE-01";
    private static final String SPAN_API =
            "simtr-hub.api.dossie-produto.documentos.consultar";
    private static final String SPAN_SERVICE =
            "simtr-hub.service.dossie-produto.documentos.consultar";
    private static final String SPAN_CLIENT = "mtr.dossie-produto.documentos.consultar";
    private static final String RESPOSTA_MTR = """
            [{
              "id_instancia_documento": 9000001,
              "id_documento": 9000002,
              "codigo_ged": "GED-WIRE-0001",
              "data_hora_captura": "01/01/2030 10:00:00",
              "data_hora_validade": null,
              "matricula_captura": "MATRICULA-WIRE-0001",
              "tipo_documento": {
                "id": 9001,
                "nome": "DOCUMENTO WIRE",
                "codigo_tipologia": "TIPO-WIRE-01",
                "ativo": true
              },
              "situacao_documento": "Criado",
              "vinculo_dossie": {
                "cliente": {
                  "cpf": "00000000191",
                  "cnpj": "00000000000191",
                  "nome": "CLIENTE WIRE",
                  "razao_social": null,
                  "tipo_vinculo": "Proponente",
                  "identificador_negocial_vinculo": 9000003,
                  "principal": true
                },
                "produto": null,
                "garantia": null,
                "fase": null,
                "processo": null
              },
              "url": "https://documento.invalid/wire-0001",
              "atributos": [],
              "assinaturas_digitais": [],
              "conformidade": [],
              "propriedades": [],
              "outsourcing": [],
              "armazenamento": []
            }]
            """;

    @Inject
    InMemorySpanExporter spanExporter;

    @Inject
    OpenTelemetry openTelemetry;

    private final CapturingHandler logHandler = new CapturingHandler();
    private Logger rootLogger;

    @BeforeEach
    void prepararEvidencias() {
        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        spanExporter.reset();
        DossieProdutoMtrStubTestResource.reset();
        rootLogger = Logger.getLogger("");
        rootLogger.addHandler(logHandler);
    }

    @AfterEach
    void removerCapturaDeLogs() {
        rootLogger.removeHandler(logHandler);
        logHandler.close();
    }

    @Test
    void percorreRotaPublicaAteWireMtrComFiltrosHeadersESemCorpo() {
        DossieProdutoMtrStubTestResource.responder(200, RESPOSTA_MTR);

        JsonNode resposta = given()
                .accept(ContentType.JSON)
                .queryParam("cnpj", CNPJ)
                .queryParam("cpf", CPF)
                .queryParam("fase", 7)
                .queryParam("inclui-armazenamento", true)
                .queryParam("inclui-assinaturas", false)
                .queryParam("inclui-atributos", true)
                .queryParam("inclui-conformidade", false)
                .queryParam("inclui-outsourcing", true)
                .queryParam("inclui-propriedades", false)
                .queryParam("inclui-url", true)
                .queryParam("ip-usuario", IP)
                .queryParam("tipologia", TIPOLOGIA)
                .when()
                .get(CAMINHO_PUBLICO, IDENTIFICADOR)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(1, resposta.size());
        assertEquals(9_000_001L, resposta.get(0).path("id_instancia_documento").longValue());
        assertEquals(CPF, resposta.get(0).path("vinculo_dossie").path("cliente")
                .path("cpf").textValue());

        List<DossieProdutoMtrStubTestResource.CapturedRequest> requisicoes =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(1, requisicoes.size());
        var requisicao = requisicoes.getFirst();
        assertEquals("GET", requisicao.method());
        assertEquals(
                DossieProdutoMtrStubTestResource.CAMINHO_DOCUMENTOS
                        + "/" + IDENTIFICADOR + "/documentos",
                requisicao.path());
        assertEquals(Set.of(
                "cnpj=" + CNPJ,
                "cpf=" + CPF,
                "fase=7",
                "inclui-armazenamento=true",
                "inclui-assinaturas=false",
                "inclui-atributos=true",
                "inclui-conformidade=false",
                "inclui-outsourcing=true",
                "inclui-propriedades=false",
                "inclui-url=true",
                "ip-usuario=" + IP,
                "tipologia=" + TIPOLOGIA),
                Set.of(requisicao.query().split("&")));
        assertEquals("", requisicao.body());
        assertNull(requisicao.contentType());
        assertTrue(requisicao.accept().contains("application/json"));
        assertEquals("test-apikey", requisicao.apikey());
        assertEquals("Bearer stub-access-token", requisicao.authorization());
        assertNotNull(requisicao.traceparent());
        assertTrue(requisicao.traceparent()
                .matches("00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}"));
    }

    @Test
    void omiteFiltrosNulosENormalizaRespostaMtr204() {
        DossieProdutoMtrStubTestResource.responder(204, null);

        given()
                .accept(ContentType.JSON)
                .when()
                .get(CAMINHO_PUBLICO, IDENTIFICADOR)
                .then()
                .statusCode(204)
                .body(org.hamcrest.Matchers.emptyOrNullString());

        List<DossieProdutoMtrStubTestResource.CapturedRequest> requisicoes =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(1, requisicoes.size());
        assertNull(requisicoes.getFirst().query());
        assertEquals("", requisicoes.getFirst().body());
        assertNull(requisicoes.getFirst().contentType());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404})
    void preservaErroMtrCompletoSemRetry(int status) {
        String corpo = erroMtr(status);
        DossieProdutoMtrStubTestResource.responder(status, corpo);

        JsonNode erro = given()
                .accept(ContentType.JSON)
                .when()
                .get(CAMINHO_PUBLICO, IDENTIFICADOR)
                .then()
                .statusCode(status)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(status, erro.path("codigo_http").intValue());
        assertEquals("simtr-dossie-produto", erro.path("recurso").textValue());
        assertEquals("documentos-" + status, erro.path("id_erro").textValue());
        assertEquals("MTR-DOCUMENTOS-" + status, erro.path("codigo_erro").textValue());
        assertEquals("ERRO_EXTERNO_" + status,
                erro.path("erros").get(0).path("mensagem").textValue());
        assertEquals("DETALHE_EXTERNO_" + status, erro.path("detalhe").textValue());
        assertEquals("STACKTRACE_EXTERNO_" + status, erro.path("stacktrace").textValue());
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void retryRecuperaErro500RepetindoGetIdentico() {
        DossieProdutoMtrStubTestResource.responder(500, erroMtr(500));
        DossieProdutoMtrStubTestResource.responder(200, RESPOSTA_MTR);

        given()
                .accept(ContentType.JSON)
                .queryParam("inclui-url", false)
                .when()
                .get(CAMINHO_PUBLICO, IDENTIFICADOR)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);

        List<DossieProdutoMtrStubTestResource.CapturedRequest> requisicoes =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(2, requisicoes.size());
        requisicoes.forEach(requisicao -> {
            assertEquals("GET", requisicao.method());
            assertEquals("inclui-url=false", requisicao.query());
            assertEquals("", requisicao.body());
            assertNull(requisicao.contentType());
        });
    }

    @Test
    void timeoutCancelaTentativaLentaEExecutaRetry() {
        DossieProdutoMtrStubTestResource.responderComAtraso(
                Duration.ofMillis(2_100),
                200,
                RESPOSTA_MTR);
        DossieProdutoMtrStubTestResource.responder(200, RESPOSTA_MTR);

        given()
                .accept(ContentType.JSON)
                .when()
                .get(CAMINHO_PUBLICO, IDENTIFICADOR)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);

        assertEquals(2, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void propagaContextoEmUmUnicoClientSemVazarQueryCredenciaisOuPayload() {
        DossieProdutoMtrStubTestResource.responder(401, erroMtr(401));

        given()
                .accept(ContentType.JSON)
                .queryParam("cnpj", CNPJ)
                .queryParam("cpf", CPF)
                .queryParam("ip-usuario", IP)
                .queryParam("tipologia", TIPOLOGIA)
                .when()
                .get(CAMINHO_PUBLICO, IDENTIFICADOR)
                .then()
                .statusCode(401);

        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        SpanData api = span(SPAN_API);
        SpanData service = span(SPAN_SERVICE);
        List<SpanData> spansDoTrace = spanExporter.getFinishedSpanItems().stream()
                .filter(span -> api.getTraceId().equals(span.getTraceId()))
                .toList();
        List<SpanData> clientsProprios = spansDoTrace.stream()
                .filter(span -> SpanKind.CLIENT.equals(span.getKind()))
                .filter(span -> SPAN_CLIENT.equals(span.getName()))
                .toList();

        String descricaoClients = spansDoTrace.stream()
                .filter(span -> SpanKind.CLIENT.equals(span.getKind()))
                .map(span -> span.getName() + " " + span.getAttributes())
                .collect(Collectors.joining("\n"));
        assertEquals(1, clientsProprios.size(), descricaoClients);
        SpanData client = clientsProprios.getFirst();
        assertEquals(SPAN_CLIENT, client.getName());
        assertEquals(SpanKind.SERVER, api.getKind());
        assertEquals(SpanKind.INTERNAL, service.getKind());
        assertEquals(api.getTraceId(), service.getTraceId());
        assertEquals(api.getSpanId(), service.getParentSpanId());
        assertEquals(api.getTraceId(), client.getTraceId());
        assertEquals(service.getSpanId(), client.getParentSpanId());
        assertEquals("mtr", service.getAttributes().get(
                AttributeKey.stringKey("simtr_hub.origem_dados")));
        assertEquals(false, service.getAttributes().get(
                AttributeKey.booleanKey(
                        "simtr_hub.simulador_dossie_produto_habilitado")));
        assertEquals(
                "/simtr/dossie-produto/v4/dossie-produto/{id}/documentos",
                client.getAttributes().get(AttributeKey.stringKey("url.path")));
        assertNull(client.getAttributes().get(AttributeKey.stringKey("url.full")));

        String traceparent = DossieProdutoMtrStubTestResource.requisicoes()
                .getFirst().traceparent();
        assertNotNull(traceparent);
        String[] camposTraceparent = traceparent.split("-");
        assertEquals(4, camposTraceparent.length);
        assertEquals(client.getTraceId(), camposTraceparent[1]);
        String descricaoCorrelacao = spansDoTrace.stream()
                .map(span -> span.getName()
                        + " span=" + span.getSpanId()
                        + " parent=" + span.getParentSpanId())
                .collect(Collectors.joining("\n"));
        assertEquals(client.getSpanId(), camposTraceparent[2], descricaoCorrelacao);

        String sinais = spansDoTrace.stream()
                .map(span -> span.getName()
                        + span.getAttributes()
                        + span.getEvents()
                        + span.getStatus())
                .collect(Collectors.joining("\n"))
                + "\n"
                + logHandler.sinais();
        assertSinaisNaoExpoemDadosDaConsulta(sinais, traceparent);
    }

    private static void assertSinaisNaoExpoemDadosDaConsulta(
            String sinais,
            String traceparent
    ) {
        assertNaoContem(sinais, CNPJ);
        assertNaoContem(sinais, CPF);
        assertNaoContem(sinais, IP);
        assertNaoContem(sinais, TIPOLOGIA);
        assertNaoContem(sinais, "GED-WIRE-0001");
        assertNaoContem(sinais, "MATRICULA-WIRE-0001");
        assertNaoContem(sinais, "CLIENTE WIRE");
        assertNaoContem(sinais, "https://documento.invalid/wire-0001");
        assertNaoContem(sinais, "ERRO_EXTERNO_401");
        assertNaoContem(sinais, "DETALHE_EXTERNO_401");
        assertNaoContem(sinais, "STACKTRACE_EXTERNO_401");
        assertNaoContem(sinais, "test-apikey");
        assertNaoContem(sinais, "stub-access-token");
        assertNaoContem(sinais, traceparent);
    }

    private SpanData span(String nome) {
        return spanExporter.getFinishedSpanItems().stream()
                .filter(item -> nome.equals(item.getName()))
                .findFirst()
                .orElseThrow();
    }

    private static void assertNaoContem(String sinais, String valor) {
        assertFalse(sinais.contains(valor), sinais);
    }

    private static String erroMtr(int status) {
        return """
                {
                  "codigo_http": %d,
                  "recurso": "simtr-dossie-produto",
                  "id_erro": "documentos-%d",
                  "codigo_erro": "MTR-DOCUMENTOS-%d",
                  "erros": [{"mensagem": "ERRO_EXTERNO_%d"}],
                  "detalhe": "DETALHE_EXTERNO_%d",
                  "stacktrace": "STACKTRACE_EXTERNO_%d"
                }
                """.formatted(status, status, status, status, status, status);
    }

    private static final class CapturingHandler extends Handler {

        private final List<String> sinais = new CopyOnWriteArrayList<>();

        @Override
        public void publish(LogRecord registro) {
            if (registro.getMessage() != null && pertenceAConsulta(registro.getMessage())) {
                sinais.add(serializar(registro));
            }
        }

        private static boolean pertenceAConsulta(String evento) {
            return evento.startsWith(
                    "simtr-hub.dossie-produto.documentos.consulta.service.")
                    || evento.startsWith("mtr.dossie-produto.documentos.consulta.")
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

        String sinais() {
            return String.join("\n", new ArrayList<>(sinais));
        }

        @Override
        public void flush() {
            // No-op: os registros são adicionados diretamente à coleção em memória.
        }

        @Override
        public void close() {
            sinais.clear();
        }
    }
}
