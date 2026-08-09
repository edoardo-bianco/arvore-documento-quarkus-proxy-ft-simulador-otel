package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.documento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ObservabilidadeFeedDocumentalTest {

    private static final String CORRELATION_ID =
            "f81fc760-d9c6-459b-bc8e-b99e70959965";
    private static final String INSTANCE_ID = "01KZFEEDOBSERVABILIDADE00000";
    private static final String EVENTO_ID = "conformidade:revisao:" + CORRELATION_ID;
    private static final String CONTEUDO_SENSIVEL =
            "TEXTO_PROMPT_RESPOSTA_REVISAO_EVIDENCIA_CREDENCIAL_DOCUMENTO";
    private static final Set<String> CAMPOS_MDC_PERMITIDOS = Set.of(
            "evento",
            "traceId",
            "spanId",
            "parentId",
            "sampled",
            "traceSampled",
            "conformidade.feed.backend",
            "conformidade.feed.operacao",
            "conformidade.feed.resultado",
            "conformidade.feed.replay",
            "conformidade.feed.cursor_lease_id",
            "conformidade.feed.evento_id",
            "conformidade.analise.correlation_id",
            "conformidade.analise.instance_id");

    @Inject
    Tracer tracer;

    @Inject
    InMemorySpanExporter exporter;

    @Inject
    OpenTelemetry openTelemetry;

    private final CapturingHandler handler = new CapturingHandler();
    private Logger rootLogger;

    @BeforeEach
    void prepararCaptura() {
        ((OpenTelemetrySdk) openTelemetry)
                .getSdkTracerProvider()
                .forceFlush()
                .join(10, TimeUnit.SECONDS);
        exporter.reset();
        rootLogger = Logger.getLogger("");
        handler.setLevel(Level.ALL);
        rootLogger.addHandler(handler);
    }

    @AfterEach
    void encerrarCaptura() {
        rootLogger.removeHandler(handler);
        handler.close();
    }

    @Test
    void registraEntregaComIdsCursorEReplaySemCorpoCloudEvent() {
        var observabilidade = new ObservabilidadeFeedDocumental(
                tracer,
                "couchdb");

        observabilidade.executar(
                "entregar-revisao",
                "cursor-42",
                "CURSOR_PERSISTIDO",
                evento(),
                () -> {
                    // Entrega sintética concluída.
                });

        SpanData span = spanFeed();
        assertEquals("couchdb", atributo(span, "conformidade.feed.backend"));
        assertEquals("entregar-revisao", atributo(span, "conformidade.feed.operacao"));
        assertEquals("CONCLUIDO", atributo(span, "conformidade.feed.resultado"));
        assertEquals("CURSOR_PERSISTIDO", atributo(span, "conformidade.feed.replay"));
        assertEquals("cursor-42", atributo(span, "conformidade.feed.cursor_lease_id"));
        assertEquals(EVENTO_ID, atributo(span, "conformidade.feed.evento_id"));
        assertEquals(CORRELATION_ID, atributo(span, "conformidade.analise.correlation_id"));
        assertEquals(INSTANCE_ID, atributo(span, "conformidade.analise.instance_id"));
        assertTrue(span.getEvents().isEmpty());
        assertFalse(spanSerializado(span).contains(CONTEUDO_SENSIVEL));

        LogObservado log = logFeed();
        assertEquals("conformidade.feed.operacao.concluida", log.mensagem());
        assertEquals(CAMPOS_MDC_PERMITIDOS, log.mdc().keySet());
        assertNull(log.falha());
        assertFalse(log.toString().contains(CONTEUDO_SENSIVEL));
    }

    @Test
    void repropragaFalhaReativaSemRegistrarExcecaoOuConteudoSensivel() {
        var falha = new IllegalStateException(CONTEUDO_SENSIVEL);
        var observabilidade = new ObservabilidadeFeedDocumental(
                tracer,
                "cosmosdb");

        var tentativa = observabilidade.observar(
                "processar-lote",
                "simtr-conformidade-revisao-v1",
                "DESDE_INICIO_SEM_LEASE",
                null,
                () -> Uni.createFrom().failure(falha),
                ignorado -> "CONCLUIDO").await();
        IllegalStateException observada = assertThrows(
                IllegalStateException.class,
                tentativa::indefinitely);

        assertSame(falha, observada);
        SpanData span = spanFeed();
        assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode());
        assertEquals("FALHA", atributo(span, "conformidade.feed.resultado"));
        assertTrue(span.getEvents().isEmpty());
        assertFalse(spanSerializado(span).contains(CONTEUDO_SENSIVEL));

        LogObservado log = logFeed();
        assertEquals("conformidade.feed.operacao.falhou", log.mensagem());
        assertNull(log.falha());
        assertFalse(log.toString().contains(CONTEUDO_SENSIVEL));
    }

    @Test
    void preservaContextoDeTraceQuandoOperacaoConcluiEmOutraThread() {
        var observabilidade = new ObservabilidadeFeedDocumental(
                tracer,
                "couchdb");

        String resultado = observabilidade.observar(
                        "carregar-cursor",
                        null,
                        "INDETERMINADO",
                        null,
                        () -> Uni.createFrom().completionStage(
                                CompletableFuture.supplyAsync(() -> "cursor-42")),
                        ignorado -> "CARREGADO")
                .await()
                .indefinitely();

        assertEquals("cursor-42", resultado);
        SpanData span = spanFeed();
        LogObservado log = logFeed();
        assertEquals(span.getTraceId(), log.mdc().get("traceId"));
        assertEquals(span.getSpanId(), log.mdc().get("spanId"));
    }

    private CloudEvent evento() {
        return CloudEventBuilder.v1()
                .withId(EVENTO_ID)
                .withType("br.gov.caixa.simtr.conformidade.revisao.recebida.v1")
                .withSource(URI.create("urn:simtr-hub:conformidade"))
                .withExtension("correlationid", CORRELATION_ID)
                .withExtension("flowinstanceid", INSTANCE_ID)
                .withData(
                        "application/json",
                        CONTEUDO_SENSIVEL.getBytes(StandardCharsets.UTF_8))
                .build();
    }

    private SpanData spanFeed() {
        ((OpenTelemetrySdk) openTelemetry)
                .getSdkTracerProvider()
                .forceFlush()
                .join(10, TimeUnit.SECONDS);
        return exporter.getFinishedSpanItems().stream()
                .filter(span -> span.getName().equals(
                        "simtr-hub.feed.conformidade.documento"))
                .findFirst()
                .orElseThrow();
    }

    private LogObservado logFeed() {
        return handler.logs().stream()
                .filter(log -> log.mensagem().startsWith(
                        "conformidade.feed.operacao."))
                .findFirst()
                .orElseThrow();
    }

    private static Object atributo(SpanData span, String chave) {
        return span.getAttributes().asMap().entrySet().stream()
                .filter(entry -> entry.getKey().getKey().equals(chave))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static String spanSerializado(SpanData span) {
        return span.getAttributes().toString() + span.getEvents();
    }

    private static final class CapturingHandler extends Handler {

        private final List<LogObservado> logs = new CopyOnWriteArrayList<>();

        @Override
        public void publish(LogRecord logRecord) {
            if (logRecord instanceof ExtLogRecord extLogRecord
                    && logRecord.getMessage() != null) {
                logs.add(new LogObservado(
                        logRecord.getMessage(),
                        extLogRecord.getMdcCopy(),
                        logRecord.getThrown()));
            }
        }

        @Override
        public void flush() {
            // No-op: o handler de teste não mantém estado pendente.
        }

        @Override
        public void close() {
            logs.clear();
        }

        List<LogObservado> logs() {
            return new ArrayList<>(logs);
        }
    }

    private record LogObservado(
            String mensagem,
            Map<String, String> mdc,
            Throwable falha) {
    }
}
