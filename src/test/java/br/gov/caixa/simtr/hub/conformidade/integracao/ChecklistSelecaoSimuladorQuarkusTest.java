package br.gov.caixa.simtr.hub.conformidade.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ObterChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ComandoConsultaChecklist;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
class ChecklistSelecaoSimuladorQuarkusTest {

    @Inject
    ObterChecklist portaSaida;

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
        var span = openTelemetry.getTracer("teste-checklist-simulador")
                .spanBuilder("teste.checklist.simulador")
                .startSpan();
        try (Scope ignored = span.makeCurrent()) {
            var checklist = portaSaida.obter(new ComandoConsultaChecklist(1000012583L, 1))
                    .await().indefinitely();

            assertEquals(1000012583L, checklist.identificadorNegocial());
            assertEquals(1, checklist.versao());
            assertEquals("Utilização FGTS - Certidão de Casamento", checklist.nome());
            assertEquals(6, checklist.apontamentos().size());
        } finally {
            span.end();
        }

        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        var spanObservado = spanExporter.getFinishedSpanItems().stream()
                .filter(item -> "teste.checklist.simulador".equals(item.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(
                "mock",
                spanObservado.getAttributes().get(
                        AttributeKey.stringKey("simtr_hub.origem_dados")
                )
        );

        var log = log("simtr-hub.checklist.simulador.usado");
        assertEquals("application", log.mdc().get("camada"));
        assertEquals("ChecklistService", log.mdc().get("componente"));
        assertEquals("consultar-checklist", log.mdc().get("operacao"));
        assertEquals("1000012583", log.mdc().get("identificador_negocial"));
        assertEquals("1", log.mdc().get("versao"));
        assertEquals("mock", log.mdc().get("origem"));
        assertFalse(handler.logs().stream().anyMatch(item ->
                "mtr.parametrizacao.checklist.chamada.iniciada".equals(item.evento())));
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
            // No-op deliberado: o appender de teste não mantém estado pendente.
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
