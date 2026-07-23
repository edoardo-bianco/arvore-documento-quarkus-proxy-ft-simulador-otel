package br.gov.caixa.simtr.hub.arvoredocumento.caracterizacao;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(
        value = ProcessoParametrizadoMtrStubTestResource.class,
        restrictToAnnotatedClass = true
)
class ConsultarProcessoParametrizadoLegadoContractTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String PATH_PUBLICO =
            "/simtr-hub/v1/processo/identificador-negocial/{identificador}";
    private static final long IDENTIFICADOR = 1000016487L;
    private static final long TIMEOUT_SPAN_SEGUNDOS = 5L;
    private static final String RESPOSTA_MTR_COM_NULOS = """
            {
              "identificador_negocial": 1000016487,
              "nome": "Processo caracterizado no stub",
              "ativo": true,
              "ultima_alteracao": null,
              "indicador_produto_obrigatorio": false,
              "macroprocesso": null,
              "relacionamentos": [null],
              "produtos": null,
              "fases": [],
              "documentos": [{
                "funcao_documental": null,
                "tipo_documento": null,
                "obrigatorio": false
              }],
              "checklist": null
            }
            """;
    private static final String RESPOSTA_PUBLICA_COM_NULOS_OMITIDOS = """
            {
              "identificador_negocial": 1000016487,
              "nome": "Processo caracterizado no stub",
              "ativo": true,
              "indicador_produto_obrigatorio": false,
              "relacionamentos": [null],
              "fases": [],
              "documentos": [{"obrigatorio": false}]
            }
            """;

    @Inject
    InMemorySpanExporter spanExporter;

    @Inject
    OpenTelemetry openTelemetry;

    @BeforeEach
    void resetarCaracterizacao() {
        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        spanExporter.reset();
        ProcessoParametrizadoMtrStubTestResource.reset();
    }

    @Test
    void preservaWireHeadersRespostaNulabilidadeEObservabilidadeDoLegado() throws Exception {
        ProcessoParametrizadoMtrStubTestResource.responder(200, RESPOSTA_MTR_COM_NULOS);

        JsonNode response = consultar(IDENTIFICADOR).then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree(RESPOSTA_PUBLICA_COM_NULOS_OMITIDOS), response);
        List<ProcessoParametrizadoMtrStubTestResource.CapturedRequest> requests =
                ProcessoParametrizadoMtrStubTestResource.requisicoes();
        assertEquals(1, requests.size());

        ProcessoParametrizadoMtrStubTestResource.CapturedRequest request = requests.getFirst();
        assertEquals("GET", request.method());
        assertEquals(ProcessoParametrizadoMtrStubTestResource.CAMINHO_PROCESSO + IDENTIFICADOR,
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
                .filter(span -> "mtr.parametrizacao.processo.consultar".equals(span.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(SpanKind.CLIENT, gateway.getKind());
        assertEquals("simtr-parametrizacao",
                gateway.getAttributes().get(AttributeKey.stringKey("mtr.servico")));
        assertEquals("patriarca-processo-v2",
                gateway.getAttributes().get(AttributeKey.stringKey("mtr.api")));
        assertEquals("GET",
                gateway.getAttributes().get(AttributeKey.stringKey("http.request.method")));
        assertEquals(
                "/simtr-parametrizacao/v2/patriarca/processo/identificador-negocial/{identificador}",
                gateway.getAttributes().get(AttributeKey.stringKey("url.path")));
        assertEquals(true,
                gateway.getAttributes().get(AttributeKey.booleanKey("mtr.resposta.sucesso")));

        SpanData restClient = aguardarSpanRestClient();
        assertEquals("consultarPorIdentificadorNegocial",
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
    void preservaErroDeNegocioCompletoSemRetry() throws Exception {
        String erroMtr = """
                {
                  "codigo_http": 404,
                  "recurso": "simtr-parametrizacao",
                  "id_erro": "processo-stub-404",
                  "codigo_erro": "MTR-PROCESSO-404",
                  "erros": [{"mensagem": "processo nao localizado"}],
                  "detalhe": "falha de negocio controlada"
                }
                """;
        ProcessoParametrizadoMtrStubTestResource.responder(404, erroMtr);

        JsonNode response = consultar(IDENTIFICADOR).then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree(erroMtr), response);
        assertEquals(1, ProcessoParametrizadoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void retryRecuperaErroServidorSemAlterarOGet() throws Exception {
        ProcessoParametrizadoMtrStubTestResource.responder(500,
                "{\"codigo_http\":500,\"recurso\":\"simtr-parametrizacao\"}");
        ProcessoParametrizadoMtrStubTestResource.responder(200, RESPOSTA_MTR_COM_NULOS);

        JsonNode response = consultar(IDENTIFICADOR).then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree(RESPOSTA_PUBLICA_COM_NULOS_OMITIDOS), response);
        List<ProcessoParametrizadoMtrStubTestResource.CapturedRequest> requests =
                ProcessoParametrizadoMtrStubTestResource.requisicoes();
        assertEquals(2, requests.size());
        assertTrue(requests.stream().allMatch(request -> "GET".equals(request.method())));
        assertTrue(requests.stream().allMatch(request -> request.path().equals(
                ProcessoParametrizadoMtrStubTestResource.CAMINHO_PROCESSO + IDENTIFICADOR)));
        assertTrue(requests.stream().allMatch(request -> request.body().isEmpty()));
    }

    private static io.restassured.response.Response consultar(long identificador) {
        return given()
                .accept(ContentType.JSON)
                .when()
                .get(PATH_PUBLICO, identificador);
    }

    private SpanData aguardarSpanRestClient() throws Exception {
        Optional<SpanData> spanAtual = localizarSpanRestClient();
        if (spanAtual.isPresent()) {
            return spanAtual.get();
        }

        try (var executor = Executors.newSingleThreadScheduledExecutor()) {
            var spanEncontrado = new CompletableFuture<SpanData>();
            executor.scheduleWithFixedDelay(
                    () -> localizarSpanRestClient().ifPresent(spanEncontrado::complete),
                    0L,
                    10L,
                    TimeUnit.MILLISECONDS
            );
            try {
                return spanEncontrado.get(TIMEOUT_SPAN_SEGUNDOS, TimeUnit.SECONDS);
            } catch (TimeoutException timeout) {
                throw timeoutSpanRestClient(timeout);
            }
        }
    }

    private Optional<SpanData> localizarSpanRestClient() {
        return spanExporter.getFinishedSpanItems().stream()
                .filter(item -> "ParametrizacaoProcessoClient".equals(
                        item.getAttributes().get(AttributeKey.stringKey("rest_client.class"))))
                .findFirst();
    }

    private AssertionError timeoutSpanRestClient(TimeoutException causa) {
        List<String> spansRecebidos = spanExporter.getFinishedSpanItems().stream()
                .map(SpanData::getName)
                .toList();
        return new AssertionError(
                "Span do ParametrizacaoProcessoClient não foi exportado em até "
                        + TIMEOUT_SPAN_SEGUNDOS + " segundos. Spans recebidos: " + spansRecebidos,
                causa
        );
    }

}
