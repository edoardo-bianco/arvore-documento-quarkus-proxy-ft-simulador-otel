package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ProdutoSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.ProdutoDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.ProdutoDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ProdutoContratadoDossieProduto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
class ProdutoDossieProdutoMtrContractTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String MEDIA_TYPE_JSON = "application/json";
    private static final String CAMINHO_PRODUTO =
            "/simtr/dossie-produto/v1/dossie-produto/123/produto";
    private static final String REQUEST_PRODUTOS = """
            [
              {
                "codigo_operacao": 100,
                "codigo_modalidade": 200,
                "excluir": false
              },
              {
                "codigo_operacao": 300,
                "codigo_modalidade": 400,
                "excluir": true
              }
            ]
            """;

    @Inject
    @ProdutoSimulador
    SolicitarAlteracaoProdutosContratadosDossieProduto portaSimulador;

    @Inject
    SolicitarAlteracaoProdutosContratadosDossieProduto portaSelecionada;

    @Inject
    InMemorySpanExporter exporter;

    @Inject
    OpenTelemetry openTelemetry;

    @BeforeEach
    void resetStub() {
        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        exporter.reset();
        DossieProdutoMtrStubTestResource.reset();
    }

    @Test
    void selecionaMtrQuandoSimuladorEstaDesligadoEMantemSimuladorQualificado() {
        DossieProdutoMtrStubTestResource.responder(200, "");

        assertNull(portaSimulador.alterar(comando()).await().indefinitely());
        assertEquals(0, DossieProdutoMtrStubTestResource.requisicoes().size());

        assertNull(portaSelecionada.alterar(comando()).await().indefinitely());
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void preservaWireHeadersOidcTraceERespostaSemCorpoComSimuladorDesabilitado()
            throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(200, "");

        String response = patchProdutos().then()
                .statusCode(200)
                .extract().asString();

        assertEquals("", response);
        List<DossieProdutoMtrStubTestResource.CapturedRequest> requests =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(1, requests.size());

        DossieProdutoMtrStubTestResource.CapturedRequest request = requests.getFirst();
        assertEquals("PATCH", request.method());
        assertEquals(CAMINHO_PRODUTO, request.path());
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_PRODUTOS),
                OBJECT_MAPPER.readTree(request.body()));
        assertTrue(request.contentType().startsWith(MEDIA_TYPE_JSON));
        assertTrue(request.accept().contains(MEDIA_TYPE_JSON));
        assertEquals("test-apikey", request.apikey());
        assertEquals("Bearer stub-access-token", request.authorization());
        assertNotNull(request.traceparent());
        assertTrue(request.traceparent().matches(
                "00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}"));
    }

    @Test
    void preservaErroDeNegocioCompletoSemRetry() throws JsonProcessingException {
        String erroMtr = """
                {
                  "codigo_http": 400,
                  "recurso": "simtr-dossie-produto",
                  "id_erro": "stub-produto-400",
                  "codigo_erro": "MTR-DOS-PRODUTO-400",
                  "erros": [{"mensagem": "alteracao de produto nao permitida"}],
                  "detalhe": "falha de negocio controlada"
                }
                """;
        DossieProdutoMtrStubTestResource.responder(400, erroMtr);

        JsonNode response = patchProdutos().then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree(erroMtr), response);
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void retryRepeteOMesmoWireAposErroRecuperavel() throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(500, """
                {"codigo_http":500,"recurso":"simtr-dossie-produto","codigo_erro":"MTR-PRODUTO-500"}
                """);
        DossieProdutoMtrStubTestResource.responder(200, "");

        patchProdutos().then().statusCode(200);

        List<DossieProdutoMtrStubTestResource.CapturedRequest> requests =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(2, requests.size());
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_PRODUTOS),
                OBJECT_MAPPER.readTree(requests.get(0).body()));
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_PRODUTOS),
                OBJECT_MAPPER.readTree(requests.get(1).body()));
    }

    @Test
    void congelaMatrizDeFaultToleranceDeclarada() throws NoSuchMethodException {
        Method method = ProdutoDossieProdutoMtrClient.class.getMethod(
                "alterar", Long.class, List.class);

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
                ProdutoDossieProdutoMtrException.Servidor.class,
                ProcessingException.class,
                TimeoutException.class
        }, retry.retryOn());
        assertArrayEquals(new Class<?>[]{
                ProdutoDossieProdutoMtrException.Negocio.class,
                ProdutoDossieProdutoMtrException.TecnicaCliente.class
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

    @Test
    void propagaContextoEntreSpansApiAplicacaoEClientSemDadosSensiveis() {
        DossieProdutoMtrStubTestResource.responder(200, "");

        patchProdutos().then().statusCode(200);
        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);

        Set<String> nomes = Set.of(
                "simtr-hub.api.dossie-produto.produto.alterar",
                "simtr-hub.service.dossie-produto.produto.alterar",
                "mtr.dossie-produto.produto.alterar");
        Map<String, SpanData> spans = exporter.getFinishedSpanItems().stream()
                .filter(span -> nomes.contains(span.getName()))
                .collect(Collectors.toMap(SpanData::getName, span -> span));
        assertEquals(nomes, spans.keySet());

        SpanData api = spans.get("simtr-hub.api.dossie-produto.produto.alterar");
        SpanData aplicacao = spans.get("simtr-hub.service.dossie-produto.produto.alterar");
        SpanData client = spans.get("mtr.dossie-produto.produto.alterar");
        assertEquals(SpanKind.SERVER, api.getKind());
        assertEquals(SpanKind.INTERNAL, aplicacao.getKind());
        assertEquals(SpanKind.CLIENT, client.getKind());
        assertEquals(api.getTraceId(), aplicacao.getTraceId());
        assertEquals(api.getTraceId(), client.getTraceId());
        assertEquals(api.getSpanId(), aplicacao.getParentSpanId());
        assertEquals(aplicacao.getSpanId(), client.getParentSpanId());
        assertEquals("/simtr-hub/v1/dossie-produto/{id}/produto",
                atributo(api, "http.route"));
        assertEquals("PATCH", atributo(client, "http.request.method"));
        assertEquals(123L, atributo(api, "dossie_produto.id"));
        assertEquals(2L, atributo(aplicacao, "dossie_produto.produtos.quantidade"));
        assertEquals(123L, atributo(client, "dossie_produto.id"));

        assertFalse((Boolean) atributo(client, "rest_client.payload.enabled"));
        assertNull(atributo(client, "rest_client.request.body"));

        String atributosDaCapacidade = spans.values().stream()
                .flatMap(span -> span.getAttributes().asMap().entrySet().stream())
                .filter(entry -> !entry.getKey().getKey().startsWith("rest_client."))
                .map(entry -> entry.getKey().getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(" "));
        assertFalse(atributosDaCapacidade.contains("codigo_operacao"));
        assertFalse(atributosDaCapacidade.contains("test-apikey"));
        assertFalse(atributosDaCapacidade.contains("stub-access-token"));
        assertFalse(atributosDaCapacidade.contains("127.0.0.1"));
        assertFalse(atributosDaCapacidade.contains("localhost"));
    }

    private static Response patchProdutos() {
        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(REQUEST_PRODUTOS)
                .when()
                .patch("/simtr-hub/v1/dossie-produto/{id}/produto", 123L);
    }

    private static ComandoAlteracaoProdutosContratadosDossieProduto comando() {
        return new ComandoAlteracaoProdutosContratadosDossieProduto(
                123L,
                List.of(
                        new ProdutoContratadoDossieProduto(100, 200, false),
                        new ProdutoContratadoDossieProduto(300, 400, true)));
    }

    private static Object atributo(SpanData span, String nome) {
        return span.getAttributes().asMap().entrySet().stream()
                .filter(entry -> entry.getKey().getKey().equals(nome))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
