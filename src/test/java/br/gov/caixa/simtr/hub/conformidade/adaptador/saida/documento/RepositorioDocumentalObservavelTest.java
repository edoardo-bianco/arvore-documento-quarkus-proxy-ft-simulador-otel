package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
class RepositorioDocumentalObservavelTest {

    private static final String CORRELATION_ID =
            "290ee116-a8c6-4d18-8ca6-c35a770a621f";
    private static final String INSTANCE_ID = "01KZOBSERVABILIDADE00000000";
    private static final String DOCUMENT_ID = "entrada:" + CORRELATION_ID;
    private static final String CONTEUDO_SENSIVEL =
            "TEXTO_PROMPT_REVISAO_EVIDENCIA_CREDENCIAL_DOCUMENTO";
    private static final Set<String> CAMPOS_MDC_PERMITIDOS = Set.of(
            "evento",
            "traceId",
            "spanId",
            "parentId",
            "sampled",
            "traceSampled",
            "conformidade.persistencia.backend",
            "conformidade.persistencia.operacao",
            "conformidade.persistencia.resultado",
            "conformidade.analise.correlation_id",
            "conformidade.analise.instance_id",
            "conformidade.analise.documento_id");
    private static final Set<String> CAMPOS_MDC_OBRIGATORIOS = Set.of(
            "evento",
            "traceId",
            "spanId",
            "traceSampled",
            "conformidade.persistencia.backend",
            "conformidade.persistencia.operacao",
            "conformidade.persistencia.resultado",
            "conformidade.analise.correlation_id",
            "conformidade.analise.instance_id",
            "conformidade.analise.documento_id");

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
    void registraSucessoSomenteComCamposPermitidosEContextoDeTrace() {
        var delegate = new RepositorioFalso();
        var repositorio = new RepositorioDocumentalObservavel(
                delegate,
                tracer,
                "couchdb");

        var resultado = repositorio.criar(DOCUMENT_ID, documento())
                .await()
                .indefinitely();

        assertEquals(RepositorioDocumental.ResultadoGravacao.GRAVADO, resultado);
        SpanData span = spanPersistencia();
        assertEquals("couchdb", atributo(span, "conformidade.persistencia.backend"));
        assertEquals("criar", atributo(span, "conformidade.persistencia.operacao"));
        assertEquals("GRAVADO", atributo(span, "conformidade.persistencia.resultado"));
        assertEquals(CORRELATION_ID, atributo(span, "conformidade.analise.correlation_id"));
        assertEquals(INSTANCE_ID, atributo(span, "conformidade.analise.instance_id"));
        assertEquals(DOCUMENT_ID, atributo(span, "conformidade.analise.documento_id"));
        assertTrue(span.getEvents().isEmpty());
        assertFalse(spanSerializado(span).contains(CONTEUDO_SENSIVEL));

        LogObservado log = logPersistencia();
        assertEquals("conformidade.persistencia.operacao.concluida", log.mensagem());
        assertCamposMdcPermitidosEObrigatorios(log);
        assertEquals(span.getTraceId(), log.mdc().get("traceId"));
        assertEquals(span.getSpanId(), log.mdc().get("spanId"));
        assertNull(log.falha());
        assertFalse(log.toString().contains(CONTEUDO_SENSIVEL));
    }

    @Test
    void repropragaFalhaSemRegistrarExcecaoOuConteudoSensivel() {
        var falha = new IllegalStateException(CONTEUDO_SENSIVEL);
        var delegate = new RepositorioFalso();
        delegate.falha = falha;
        var repositorio = new RepositorioDocumentalObservavel(
                delegate,
                tracer,
                "cosmosdb");

        var tentativa = repositorio.criar(DOCUMENT_ID, documento()).await();
        IllegalStateException observada = assertThrows(
                IllegalStateException.class,
                tentativa::indefinitely);

        assertSame(falha, observada);
        SpanData span = spanPersistencia();
        assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode());
        assertEquals("cosmosdb", atributo(span, "conformidade.persistencia.backend"));
        assertEquals("FALHA", atributo(span, "conformidade.persistencia.resultado"));
        assertTrue(span.getEvents().isEmpty());
        assertFalse(spanSerializado(span).contains(CONTEUDO_SENSIVEL));

        LogObservado log = logPersistencia();
        assertEquals("conformidade.persistencia.operacao.falhou", log.mensagem());
        assertCamposMdcPermitidosEObrigatorios(log);
        assertNull(log.falha());
        assertFalse(log.toString().contains(CONTEUDO_SENSIVEL));
    }

    private static void assertCamposMdcPermitidosEObrigatorios(LogObservado log) {
        assertTrue(CAMPOS_MDC_PERMITIDOS.containsAll(log.mdc().keySet()));
        assertTrue(log.mdc().keySet().containsAll(CAMPOS_MDC_OBRIGATORIOS));
    }

    private ObjectNode documento() {
        return new ObjectMapper().createObjectNode()
                .put("id", DOCUMENT_ID)
                .put("correlationId", CORRELATION_ID)
                .put("instanceId", INSTANCE_ID)
                .put("texto", CONTEUDO_SENSIVEL)
                .put("prompt", CONTEUDO_SENSIVEL)
                .put("resposta", CONTEUDO_SENSIVEL)
                .put("revisao", CONTEUDO_SENSIVEL)
                .put("evidencia", CONTEUDO_SENSIVEL)
                .put("credencial", CONTEUDO_SENSIVEL);
    }

    private SpanData spanPersistencia() {
        ((OpenTelemetrySdk) openTelemetry)
                .getSdkTracerProvider()
                .forceFlush()
                .join(10, TimeUnit.SECONDS);
        return exporter.getFinishedSpanItems().stream()
                .filter(span -> span.getName().equals(
                        "simtr-hub.persistencia.conformidade.documento"))
                .findFirst()
                .orElseThrow();
    }

    private LogObservado logPersistencia() {
        return handler.logs().stream()
                .filter(log -> log.mensagem().startsWith(
                        "conformidade.persistencia.operacao."))
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

    private static final class RepositorioFalso implements RepositorioDocumental {

        private RuntimeException falha;

        @Override
        public Uni<ResultadoGravacao> criar(String documentId, ObjectNode documento) {
            return falha == null
                    ? Uni.createFrom().item(ResultadoGravacao.GRAVADO)
                    : Uni.createFrom().failure(falha);
        }

        @Override
        public Uni<ResultadoGravacao> substituir(
                String documentId,
                String versaoEsperada,
                ObjectNode documento) {
            return criar(documentId, documento);
        }

        @Override
        public Uni<Optional<DocumentoPersistido>> consultar(
                String documentId,
                String correlationId) {
            return Uni.createFrom().item(Optional.empty());
        }
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
