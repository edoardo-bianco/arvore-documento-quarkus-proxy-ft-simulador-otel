package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.ObterDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DossieProdutoConsultado;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.Test;

class ConsultaDossieProdutoObservabilidadeTest {

    private static final long IDENTIFICADOR = 4324680L;
    private static final String LOGGER_RAIZ = "";
    private static final String EVENTO_PREFIXO =
            "simtr-hub.dossie-produto.consulta.service.";
    private static final String CPF_SENTINELA = "CPF_SENTINELA_00000000000";
    private static final String CNPJ_SENTINELA = "CNPJ_SENTINELA_00000000000000";
    private static final String NOME_SENTINELA = "NOME_SENTINELA_CLIENTE";
    private static final String MATRICULA_SENTINELA = "MATRICULA_SENTINELA";

    @Test
    void implementaPortaDeEntradaDelegaIdentificadorEPreservaResposta() {
        var portaSaida = new FakePortaSaida();
        var observabilidade = new ConsultaDossieProdutoObservabilidade(portaSaida, true);
        var identificador = new IdentificadorDossieProduto(IDENTIFICADOR);
        var resposta = dossie(IDENTIFICADOR);
        portaSaida.resposta = resposta;

        var resultado = observabilidade.executar(identificador).await().indefinitely();

        assertSame(identificador, portaSaida.identificadorRecebido);
        assertSame(resposta, resultado);
        assertEquals(ConsultarDossieProduto.class,
                ConsultaDossieProdutoObservabilidade.class.getInterfaces()[0]);
        assertNotNull(ConsultaDossieProdutoObservabilidade.class
                .getAnnotation(ApplicationScoped.class));
    }

    @Test
    void propagaMesmaFalhaDaPortaDeSaida() {
        var portaSaida = new FakePortaSaida();
        portaSaida.falha = new IllegalStateException(CPF_SENTINELA);
        var observabilidade = new ConsultaDossieProdutoObservabilidade(portaSaida, false);
        var identificador = new IdentificadorDossieProduto(IDENTIFICADOR);
        var handler = new CapturingHandler();
        Logger rootLogger = Logger.getLogger(LOGGER_RAIZ);
        rootLogger.addHandler(handler);

        try {
            var espera = observabilidade.executar(identificador).await();
            var falha = assertThrows(IllegalStateException.class,
                    espera::indefinitely);

            assertSame(portaSaida.falha, falha);
            assertSame(identificador, portaSaida.identificadorRecebido);
            var logs = handler.logs().stream()
                    .filter(log -> log.evento().startsWith(EVENTO_PREFIXO))
                    .toList();
            assertEquals("IllegalStateException", logs.getLast().mdc().get("erro_tipo"));
            assertFalse(logs.toString().contains(CPF_SENTINELA));
        } finally {
            rootLogger.removeHandler(handler);
        }
    }

    @Test
    void declaraSpanInternoAprovado() throws NoSuchMethodException {
        var metodo = ConsultaDossieProdutoObservabilidade.class.getMethod(
                "executar", IdentificadorDossieProduto.class
        );
        var span = metodo.getAnnotation(WithSpan.class);

        assertNotNull(span);
        assertEquals("simtr-hub.service.dossie-produto.consultar", span.value());
        assertEquals(SpanKind.INTERNAL, span.kind());
    }

    @Test
    void registraOrigemIdentificadorEContagensSemDadosPessoais() {
        var exporter = InMemorySpanExporter.create();
        var tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        var handler = new CapturingHandler();
        Logger rootLogger = Logger.getLogger(LOGGER_RAIZ);
        rootLogger.addHandler(handler);
        try {
            var openTelemetry = OpenTelemetrySdk.builder()
                    .setTracerProvider(tracerProvider)
                    .build();
            var portaSaida = new FakePortaSaida();
            portaSaida.resposta = dossieComDadosPessoais();
            var observabilidade = new ConsultaDossieProdutoObservabilidade(portaSaida, true);
            var span = openTelemetry.getTracer("teste-consulta-dossie-produto")
                    .spanBuilder("teste.consulta-dossie-produto.service")
                    .startSpan();

            try (var _ = span.makeCurrent()) {
                observabilidade.executar(new IdentificadorDossieProduto(IDENTIFICADOR))
                        .await().indefinitely();
            } finally {
                span.end();
            }
            tracerProvider.forceFlush().join(10, TimeUnit.SECONDS);

            var observado = exporter.getFinishedSpanItems().getFirst();
            assertEquals(true, observado.getAttributes().get(AttributeKey.booleanKey(
                    "simtr_hub.simulador_dossie_produto_habilitado")));
            assertEquals("mock", observado.getAttributes().get(AttributeKey.stringKey(
                    "simtr_hub.origem_dados")));
            assertEquals(IDENTIFICADOR, observado.getAttributes().get(AttributeKey.longKey(
                    "dossie_produto.id")));
            assertEquals(2L, observado.getAttributes().get(AttributeKey.longKey(
                    "dossie_produto.clientes.quantidade")));
            assertEquals(1L, observado.getAttributes().get(AttributeKey.longKey(
                    "dossie_produto.unidades_tratamento.quantidade")));
            assertEquals(1L, observado.getAttributes().get(AttributeKey.longKey(
                    "dossie_produto.produtos_contratados.quantidade")));

            var logs = handler.logs().stream()
                    .filter(log -> log.evento().startsWith(EVENTO_PREFIXO))
                    .toList();
            assertEquals(List.of(
                    EVENTO_PREFIXO + "iniciada",
                    EVENTO_PREFIXO + "concluida"
            ), logs.stream().map(LogObservado::evento).toList());
            assertEquals("2", logs.get(1).mdc().get("clientes_quantidade"));
            assertEquals("1", logs.get(1).mdc().get("unidades_tratamento_quantidade"));
            assertEquals("1", logs.get(1).mdc().get("produtos_contratados_quantidade"));

            String sinais = observado.getAttributes() + logs.toString();
            assertFalse(sinais.contains(CPF_SENTINELA));
            assertFalse(sinais.contains(CNPJ_SENTINELA));
            assertFalse(sinais.contains(NOME_SENTINELA));
            assertFalse(sinais.contains(MATRICULA_SENTINELA));
        } finally {
            rootLogger.removeHandler(handler);
            tracerProvider.close();
        }
    }

    private static DossieProdutoConsultado dossie(Long id) {
        return new DossieProdutoConsultado(
                id, null, null, null, null, null, null,
                List.of(), null, null, null, List.of(), List.of()
        );
    }

    private static DossieProdutoConsultado dossieComDadosPessoais() {
        return new DossieProdutoConsultado(
                IDENTIFICADOR, null, null, null, "SIMTRAPI", 5402, null,
                Arrays.asList(
                        new DossieProdutoConsultado.Cliente(
                                CPF_SENTINELA, CNPJ_SENTINELA, NOME_SENTINELA,
                                null, "Proponente", 1L, true
                        ),
                        null
                ),
                null,
                null,
                new DossieProdutoConsultado.Situacao(
                        1, "Rascunho", null, MATRICULA_SENTINELA
                ),
                List.of(1),
                List.of(new DossieProdutoConsultado.ProdutoContratado(
                        null, null, null, null
                ))
        );
    }

    private static final class FakePortaSaida implements ObterDossieProduto {

        private IdentificadorDossieProduto identificadorRecebido;
        private DossieProdutoConsultado resposta;
        private RuntimeException falha;

        @Override
        public Uni<DossieProdutoConsultado> obter(IdentificadorDossieProduto identificador) {
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
                        logRecord.getMessage(), extLogRecord.getMdcCopy()
                ));
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

        List<LogObservado> logs() {
            return new ArrayList<>(logs);
        }
    }

    private record LogObservado(String evento, Map<String, String> mdc) {
    }
}
