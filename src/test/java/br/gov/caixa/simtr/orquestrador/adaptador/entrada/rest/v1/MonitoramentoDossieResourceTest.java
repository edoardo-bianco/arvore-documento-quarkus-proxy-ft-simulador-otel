package br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1.dto.IniciarMonitoramentoDossieRequest;
import br.gov.caixa.simtr.orquestrador.adaptador.entrada.rest.v1.dto.IniciarMonitoramentoDossieResponse;
import br.gov.caixa.simtr.orquestrador.aplicacao.porta.entrada.IniciarMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.MonitoramentoIniciado;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.SolicitacaoMonitoramento;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Contrato HTTP local com porta simulada e Service Bus desabilitado. */
@QuarkusTest
class MonitoramentoDossieResourceTest {

    private static final String NOME_HTTP = "simtr-hub.api.monitoramento-dossie.iniciar";
    private static final String PAI_REMOTO = "2222222222222222";
    private static final String PATH = "/simtr-hub/v1/monitoramentos-dossie";
    private static final String VALIDO = "{\"idDossiePreValidacao\":\"pre-em-analise\",\"idDossieMtr\":\"0007\"}";
    private static final MonitoramentoIniciado RESULTADO = new MonitoramentoIniciado("MON-SERVIDOR", "ORQ-SERVIDOR");

    @InjectMock
    IniciarMonitoramento iniciar;

    @Inject
    MonitoramentoDossieResource resource;

    @Inject Tracer tracer;
    @Inject OpenTelemetry openTelemetry;
    @Inject InMemorySpanExporter exporter;
    private String tracePost;

    @BeforeEach
    void prepararPorta() {
        flush();
        exporter.reset();
        var controle = tracer.spanBuilder("teste.monitoramento.http.controle").setNoParent().startSpan();
        assertTrue(controle.isRecording());
        controle.end();
        flush();
        assertEquals(1, exporter.getFinishedSpanItems().size());
        assertEquals(controle.getSpanContext(), exporter.getFinishedSpanItems().getFirst().getSpanContext());
        exporter.reset();
        tracePost = UUID.randomUUID().toString().replace("-", "");
        reset(iniciar);
        when(iniciar.executar(any())).thenReturn(Uni.createFrom().item(RESULTADO));
    }

    @Test
    void deveResponder202ComIdsDoServidorEPreservarIdentificadores() {
        requisicao().body(VALIDO)
                .when().post(PATH).then().statusCode(202).contentType(ContentType.JSON)
                .body("monitoramentoId", equalTo("MON-SERVIDOR"),
                        "orquestracaoId", equalTo("ORQ-SERVIDOR"), "size()", equalTo(2));
        verify(iniciar).executar(new SolicitacaoMonitoramento("pre-em-analise", "0007"));
        verificarSpanHttp(NOME_HTTP);
    }

    @Test
    void deveIgnorarCamposDesconhecidosSemAceitarIdsGeradosPeloCliente() {
        requisicao()
                .body(VALIDO.replace("}", ",\"monitoramentoId\":\"id-cliente\",\"tentativaAtual\":99}"))
                .when().post(PATH).then().statusCode(202).body("monitoramentoId", equalTo("MON-SERVIDOR"));
        verify(iniciar).executar(new SolicitacaoMonitoramento("pre-em-analise", "0007"));
        verificarSpanHttp(NOME_HTTP);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "null", "{}", "{\"idDossieMtr\":\"7\"}",
            "{\"idDossiePreValidacao\":\"pre\"}",
            "{\"idDossiePreValidacao\":\" \",\"idDossieMtr\":\"7\"}",
            "{\"idDossiePreValidacao\":\"pre\",\"idDossieMtr\":\"0\"}",
            "{\"idDossiePreValidacao\":\"pre\",\"idDossieMtr\":\"-1\"}",
            "{\"idDossiePreValidacao\":\"pre\",\"idDossieMtr\":\"+7\"}",
            "{\"idDossiePreValidacao\":\"pre\",\"idDossieMtr\":\"7.1\"}",
            "{\"idDossiePreValidacao\":\"pre\",\"idDossieMtr\":\"9223372036854775808\"}"
    })
    void deveRejeitarRequestInvalidoAntesDaPorta(String corpo) {
        requisicao().body(corpo)
                .when().post(PATH).then().statusCode(400)
                .body("codigo_http", equalTo(400), "codigo_erro", equalTo("ARVDOCP0001"));
        verifyNoInteractions(iniciar);
        verificarSpanHttp("POST " + PATH);
    }

    @Test
    void deveRejeitarJsonMalformadoAntesDaPorta() {
        requisicao().body("{")
                .when().post(PATH).then().statusCode(400);
        verifyNoInteractions(iniciar);
        verificarSpanHttp("POST " + PATH);
    }

    @Test
    void devePreservarLimiteMaximoMtrEmString() {
        var corpo = "{\"idDossiePreValidacao\":\"pre\",\"idDossieMtr\":\"09223372036854775807\"}";
        requisicao().body(corpo)
                .when().post(PATH).then().statusCode(202);
        verify(iniciar).executar(new SolicitacaoMonitoramento("pre", "09223372036854775807"));
        verificarSpanHttp(NOME_HTTP);
    }

    @Test
    void deveResponderErroPadraoSemDetalhesDaFalhaAssincrona() {
        when(iniciar.executar(any())).thenReturn(Uni.createFrom().failure(new IllegalStateException("dado-restrito")));
        var corpo = requisicao().body(VALIDO)
                .when().post(PATH).then().statusCode(500).contentType(ContentType.JSON)
                .body("codigo_http", equalTo(500), "recurso", equalTo("simtr-hub"),
                        "codigo_erro", equalTo("ARVDOCP9999"), "id_erro", notNullValue(),
                        "erros[0].mensagem", equalTo("Erro interno ao processar a requisição."),
                        "$", not(hasKey("stacktrace")), "$", not(hasKey("detalhe")))
                .extract().asString();
        assertFalse(corpo.contains("dado-restrito"));
        verificarSpanHttp(NOME_HTTP);
    }

    @Test
    void deveSanitizarTambemFalhaSincronaDaPorta() {
        when(iniciar.executar(any())).thenThrow(new IllegalStateException("dado-restrito"));
        requisicao().body(VALIDO)
                .when().post(PATH).then().statusCode(500)
                .body("codigo_erro", equalTo("ARVDOCP9999"),
                        "erros[0].mensagem", equalTo("Erro interno ao processar a requisição."));
        verificarSpanHttp(NOME_HTTP);
    }

    @Test
    void deveProduzirResponseSomenteDepoisDaConclusaoDaPorta() throws Exception {
        var confirmacao = new CompletableFuture<MonitoramentoIniciado>();
        when(iniciar.executar(any())).thenReturn(Uni.createFrom().completionStage(confirmacao));
        var resposta = resource.iniciar(new IniciarMonitoramentoDossieRequest("pre", "7"))
                .subscribeAsCompletionStage().toCompletableFuture();
        assertFalse(resposta.isDone());
        confirmacao.complete(RESULTADO);
        try (var concluida = resposta.get(3, TimeUnit.SECONDS)) {
            assertEquals(202, concluida.getStatus());
            assertEquals(new IniciarMonitoramentoDossieResponse("MON-SERVIDOR", "ORQ-SERVIDOR"), concluida.getEntity());
        }
    }

    private RequestSpecification requisicao() {
        return given().contentType(ContentType.JSON)
                .header("traceparent", "00-" + tracePost + "-" + PAI_REMOTO + "-01");
    }

    private void verificarSpanHttp(String nome) {
        var spans = aguardarSpanDoPost();
        assertEquals(1, spans.size(), "A porta simulada deixa somente o SERVER automatico.");
        var http = spans.getFirst();
        assertEquals(SpanKind.SERVER, http.getKind());
        assertEquals(nome, http.getName());
        assertEquals(PATH, http.getAttributes().get(AttributeKey.stringKey("http.route")));
        assertEquals(tracePost, http.getTraceId());
        assertEquals(PAI_REMOTO, http.getParentSpanId());
        assertTrue(http.getParentSpanContext().isRemote());
        assertTrue(http.getLinks().isEmpty());
        if (NOME_HTTP.equals(nome)) {
            assertTrue(http.getEvents().isEmpty());
        } else {
            assertEquals(List.of("exception"), http.getEvents().stream().map(EventData::getName).toList());
        }
        assertFalse(http.getAttributes().toString().contains("dado-restrito"));
        assertFalse(http.getStatus().getDescription().contains("dado-restrito"));
    }

    private List<SpanData> aguardarSpanDoPost() {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (true) {
            // Flush exporta spans terminados; o callback HTTP ainda pode estar encerrando o SERVER.
            flush();
            var spans = exporter.getFinishedSpanItems();
            if (spans.stream().anyMatch(span -> tracePost.equals(span.getTraceId()))
                    || System.nanoTime() >= limite) {
                return spans; // Inventario completo: spans inesperados continuam fazendo a prova falhar.
            }
            if (Thread.currentThread().isInterrupted()) {
                throw new AssertionError("Espera do span HTTP interrompida.");
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
        }
    }

    private void flush() {
        var resultado = ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider().forceFlush();
        resultado.join(10, TimeUnit.SECONDS);
        assertTrue(resultado.isSuccess());
    }
}
