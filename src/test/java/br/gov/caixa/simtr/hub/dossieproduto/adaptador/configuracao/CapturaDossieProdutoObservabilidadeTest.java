package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.CapturarDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarCapturaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCapturaDossieProduto;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.Test;

class CapturaDossieProdutoObservabilidadeTest {

    private static final long IDENTIFICADOR = 123L;
    private static final String EVENTO_PREFIXO =
            "simtr-hub.dossie-produto.captura.processamento.";
    private static final String SEGREDO_SENTINELA = "API_KEY_EXTERNA_NAO_LOGAR";

    @Test
    void implementaPortaDeEntradaDelegaIdentificadorEPreservaResposta() {
        var portaSaida = new FakePortaSaida();
        var resposta = new ResultadoCapturaDossieProduto(IDENTIFICADOR);
        portaSaida.resposta = resposta;
        var observabilidade = new CapturaDossieProdutoObservabilidade(portaSaida, true);
        var identificador = new IdentificadorDossieProduto(IDENTIFICADOR);

        var resultado = observabilidade.executar(identificador).await().indefinitely();

        assertSame(identificador, portaSaida.identificadorRecebido);
        assertSame(resposta, resultado);
        assertEquals(CapturarDossieProduto.class,
                CapturaDossieProdutoObservabilidade.class.getInterfaces()[0]);
        assertNotNull(CapturaDossieProdutoObservabilidade.class
                .getAnnotation(ApplicationScoped.class));
    }

    @Test
    void declaraSpanInternoAprovado() throws NoSuchMethodException {
        var metodo = CapturaDossieProdutoObservabilidade.class.getMethod(
                "executar", IdentificadorDossieProduto.class);
        WithSpan span = metodo.getAnnotation(WithSpan.class);

        assertNotNull(span);
        assertEquals("simtr-hub.service.dossie-produto.capturar", span.value());
        assertEquals(SpanKind.INTERNAL, span.kind());
    }

    @Test
    void registraOrigemIdentificadorEEventosDeSucessoAprovados() {
        var portaSaida = new FakePortaSaida();
        portaSaida.resposta = new ResultadoCapturaDossieProduto(IDENTIFICADOR);
        var observabilidade = new CapturaDossieProdutoObservabilidade(portaSaida, true);
        var telemetria = iniciarTelemetria();

        try {
            executarEmSpan(telemetria, () -> observabilidade.executar(
                    new IdentificadorDossieProduto(IDENTIFICADOR)).await().indefinitely());

            var span = telemetria.exporter().getFinishedSpanItems().getFirst();
            assertEquals(true, span.getAttributes().get(AttributeKey.booleanKey(
                    "simtr_hub.simulador_dossie_produto_habilitado")));
            assertEquals("mock", span.getAttributes().get(AttributeKey.stringKey(
                    "simtr_hub.origem_dados")));
            assertEquals(IDENTIFICADOR, span.getAttributes().get(AttributeKey.longKey(
                    "dossie_produto.id")));
            assertEquals("sucesso", span.getAttributes().get(AttributeKey.stringKey(
                    "resultado")));

            var logs = telemetria.handler().logsDoEvento();
            assertEquals(List.of(
                    EVENTO_PREFIXO + "iniciada",
                    EVENTO_PREFIXO + "concluida"),
                    logs.stream().map(LogObservado::evento).toList());
            assertEquals("mock", logs.getFirst().mdc().get("origem"));
            assertEquals("123", logs.getFirst().mdc().get("dossie_produto_id"));
        } finally {
            telemetria.close();
        }
    }

    @Test
    void propagaMesmaFalhaSemRegistrarMensagemOuThrowableExterno() {
        var portaSaida = new FakePortaSaida();
        portaSaida.falha = new IllegalStateException(SEGREDO_SENTINELA);
        var observabilidade = new CapturaDossieProdutoObservabilidade(portaSaida, false);
        var telemetria = iniciarTelemetria();

        try {
            var falha = assertThrows(IllegalStateException.class,
                    () -> executarEmSpan(telemetria, () -> observabilidade.executar(
                            new IdentificadorDossieProduto(IDENTIFICADOR))
                            .await().indefinitely()));

            assertSame(portaSaida.falha, falha);
            var span = telemetria.exporter().getFinishedSpanItems().getFirst();
            assertEquals("IllegalStateException", span.getAttributes().get(
                    AttributeKey.stringKey("erro.tipo")));
            assertEquals("erro", span.getAttributes().get(
                    AttributeKey.stringKey("resultado")));
            assertEquals(0, span.getEvents().size());

            var logs = telemetria.handler().logsDoEvento();
            assertEquals(List.of(
                    EVENTO_PREFIXO + "iniciada",
                    EVENTO_PREFIXO + "falhou"),
                    logs.stream().map(LogObservado::evento).toList());
            String sinais = span.getAttributes() + logs.toString();
            assertFalse(sinais.contains(SEGREDO_SENTINELA));
        } finally {
            telemetria.close();
        }
    }

    private static Telemetria iniciarTelemetria() {
        var exporter = InMemorySpanExporter.create();
        var provider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        var handler = new CapturingHandler();
        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(handler);
        return new Telemetria(exporter, provider, handler, rootLogger);
    }

    private static void executarEmSpan(Telemetria telemetria, Runnable acao) {
        var openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(telemetria.provider())
                .build();
        var span = openTelemetry.getTracer("teste-captura-dossie-produto")
                .spanBuilder("teste.captura-dossie-produto.service")
                .startSpan();
        try (var _ = span.makeCurrent()) {
            acao.run();
        } finally {
            span.end();
        }
        telemetria.provider().forceFlush().join(10, TimeUnit.SECONDS);
    }

    private static final class FakePortaSaida implements SolicitarCapturaDossieProduto {

        private IdentificadorDossieProduto identificadorRecebido;
        private ResultadoCapturaDossieProduto resposta;
        private RuntimeException falha;

        @Override
        public Uni<ResultadoCapturaDossieProduto> capturar(
                IdentificadorDossieProduto identificador) {
            identificadorRecebido = identificador;
            if (falha != null) {
                return Uni.createFrom().failure(falha);
            }
            return Uni.createFrom().item(resposta);
        }
    }

    private static final class CapturingHandler extends Handler {

        private final List<LogObservado> logs = new ArrayList<>();

        @Override
        public void publish(LogRecord logRecord) {
            if (logRecord instanceof ExtLogRecord extLogRecord
                    && logRecord.getMessage() != null) {
                logs.add(new LogObservado(
                        logRecord.getMessage(), extLogRecord.getMdcCopy()));
            }
        }

        @Override
        public void flush() {
            // No-op: não há buffer no handler de teste.
        }

        @Override
        public void close() {
            logs.clear();
        }

        List<LogObservado> logsDoEvento() {
            return logs.stream()
                    .filter(log -> log.evento().startsWith(EVENTO_PREFIXO))
                    .toList();
        }
    }

    private record LogObservado(String evento, Map<String, String> mdc) {
    }

    private record Telemetria(
            InMemorySpanExporter exporter,
            SdkTracerProvider provider,
            CapturingHandler handler,
            Logger rootLogger) implements AutoCloseable {

        @Override
        public void close() {
            rootLogger.removeHandler(handler);
            provider.close();
        }
    }
}
