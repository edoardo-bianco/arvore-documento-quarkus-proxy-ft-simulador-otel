package br.gov.caixa.simtr.hub.gestaodocumento.integracao;

import br.gov.caixa.simtr.hub.gestaodocumento.aplicacao.porta.saida.SolicitarCredencialContainer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@QuarkusTest
class GestaoDocumentoSelecaoSimuladorQuarkusTest {

    @Inject
    SolicitarCredencialContainer portaSaida;

    @Inject
    InMemorySpanExporter spanExporter;

    @Inject
    OpenTelemetry openTelemetry;

    private final CapturingHandler handler = new CapturingHandler();
    private Logger rootLogger;

    @BeforeEach
    void prepararTelemetria() {
        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        spanExporter.reset();
        rootLogger = Logger.getLogger("");
        handler.setLevel(Level.ALL);
        rootLogger.addHandler(handler);
    }

    @AfterEach
    void removerCapturaDeLogs() {
        rootLogger.removeHandler(handler);
        handler.close();
    }

    @Test
    void selecionaSimuladorSemAmbiguidadePreservandoFixtureLogEOrigemNoSpan() {
        var span = openTelemetry.getTracer("teste-gestao-documento-simulador")
                .spanBuilder("teste.gestao-documento.simulador")
                .startSpan();
        try (Scope ignored = span.makeCurrent()) {
            var credencial = portaSaida.obter().await().indefinitely();

            assertEquals(
                    "sv=mock&ss=b&srt=o&sp=rw&se=2026-07-10T18:00:00Z&sig=mock",
                    credencial.sas()
            );
            assertEquals("10/07/2026 18:00:00", credencial.validade());
            assertEquals(
                    "https://dossiedigitaldes.blob.core.windows.net",
                    credencial.urlStorage()
            );
            assertEquals("pre-validacao", credencial.nomeContainer());
        } finally {
            span.end();
        }

        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        var spanObservado = spanExporter.getFinishedSpanItems().stream()
                .filter(item -> "teste.gestao-documento.simulador".equals(item.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(
                "mock",
                spanObservado.getAttributes().get(
                        AttributeKey.stringKey("simtr_hub.origem_dados")
                )
        );

        var log = log("simtr-hub.gestao-documento.credencial-container.simulador.usado");
        assertEquals("application", log.mdc().get("camada"));
        assertEquals("GestaoDocumentoService", log.mdc().get("componente"));
        assertEquals("gerar-credencial-container", log.mdc().get("operacao"));
        assertEquals("mock", log.mdc().get("origem"));
        assertFalse(handler.logs().stream().anyMatch(item ->
                "mtr.gestao-documento.credencial-container.chamada.iniciada".equals(
                        item.evento()
                )));
    }

    private LogObservado log(String evento) {
        return handler.logs().stream()
                .filter(log -> evento.equals(log.evento()))
                .findFirst()
                .orElseThrow();
    }

    private static final class CapturingHandler extends Handler {

        private final List<LogObservado> logs = new CopyOnWriteArrayList<>();

        @Override
        public void publish(LogRecord record) {
            if (record instanceof ExtLogRecord extLogRecord && record.getMessage() != null) {
                logs.add(new LogObservado(record.getMessage(), extLogRecord.getMdcCopy()));
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
            logs.clear();
        }

        List<LogObservado> logs() {
            return new ArrayList<>(logs);
        }
    }

    private record LogObservado(String evento, Map<String, String> mdc) {
    }
}
