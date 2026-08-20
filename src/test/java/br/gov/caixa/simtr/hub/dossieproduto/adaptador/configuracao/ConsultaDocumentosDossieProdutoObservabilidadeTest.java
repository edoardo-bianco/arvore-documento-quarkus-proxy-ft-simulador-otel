package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.ObterDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.CriteriosConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DocumentoDossieProdutoConsultado;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultaDocumentosDossieProdutoObservabilidadeTest {

    private static final long IDENTIFICADOR = 4_081_899L;
    private static final String EVENTO_PREFIXO =
            "simtr-hub.dossie-produto.documentos.consulta.service.";
    private static final String CPF_SENTINELA = "CPF_SENTINELA_00000000000";
    private static final String CNPJ_SENTINELA = "CNPJ_SENTINELA_00000000000000";
    private static final String IP_SENTINELA = "IP_SENTINELA_192.0.2.10";
    private static final String TIPOLOGIA_SENTINELA = "TIPOLOGIA_SENTINELA";
    private static final String GED_SENTINELA = "GED_SENTINELA";
    private static final String NOME_SENTINELA = "NOME_SENTINELA";
    private static final String MATRICULA_SENTINELA = "MATRICULA_SENTINELA";
    private static final String URL_SENTINELA = "https://documento.invalid/sentinela";
    private static final String PATH_SENTINELA = "container/path/sentinela";

    @Test
    void implementaPortaDeEntradaDelegaCriteriosEPreservaResposta() {
        var portaSaida = new FakePortaSaida();
        var observabilidade = new ConsultaDocumentosDossieProdutoObservabilidade(
                portaSaida, true);
        var criterios = criterios();
        var resposta = List.of(documento());
        portaSaida.resposta = resposta;

        var resultado = observabilidade.executar(criterios).await().indefinitely();

        assertSame(criterios, portaSaida.criteriosRecebidos);
        assertSame(resposta, resultado);
        assertEquals(ConsultarDocumentosDossieProduto.class,
                ConsultaDocumentosDossieProdutoObservabilidade.class.getInterfaces()[0]);
        assertNotNull(ConsultaDocumentosDossieProdutoObservabilidade.class
                .getAnnotation(ApplicationScoped.class));
    }

    @Test
    void propagaMesmaFalhaSemExporMensagemExterna() {
        var portaSaida = new FakePortaSaida();
        portaSaida.falha = new IllegalStateException(CPF_SENTINELA);
        var observabilidade = new ConsultaDocumentosDossieProdutoObservabilidade(
                portaSaida, false);
        var criterios = criterios();
        var handler = new CapturingHandler();
        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(handler);

        try {
            var espera = observabilidade.executar(criterios).await();
            var falha = assertThrows(IllegalStateException.class, espera::indefinitely);

            assertSame(portaSaida.falha, falha);
            assertSame(criterios, portaSaida.criteriosRecebidos);
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
        var metodo = ConsultaDocumentosDossieProdutoObservabilidade.class.getMethod(
                "executar", CriteriosConsultaDocumentosDossieProduto.class);
        var span = metodo.getAnnotation(WithSpan.class);

        assertNotNull(span);
        assertEquals("simtr-hub.service.dossie-produto.documentos.consultar", span.value());
        assertEquals(SpanKind.INTERNAL, span.kind());
    }

    @Test
    void registraOrigemIdentificadorEQuantidadeSemDadosSensiveis() {
        var exporter = InMemorySpanExporter.create();
        var tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        var handler = new CapturingHandler();
        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(handler);
        try {
            var openTelemetry = OpenTelemetrySdk.builder()
                    .setTracerProvider(tracerProvider)
                    .build();
            var portaSaida = new FakePortaSaida();
            portaSaida.resposta = List.of(documento(), documento());
            var observabilidade = new ConsultaDocumentosDossieProdutoObservabilidade(
                    portaSaida, true);
            var span = openTelemetry.getTracer("teste-consulta-documentos-dossie-produto")
                    .spanBuilder("teste.consulta-documentos-dossie-produto.service")
                    .startSpan();

            try (var _ = span.makeCurrent()) {
                observabilidade.executar(criterios()).await().indefinitely();
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
                    "dossie_produto.documentos.quantidade")));

            var logs = handler.logs().stream()
                    .filter(log -> log.evento().startsWith(EVENTO_PREFIXO))
                    .toList();
            assertEquals(List.of(
                    EVENTO_PREFIXO + "iniciada",
                    EVENTO_PREFIXO + "concluida"
            ), logs.stream().map(LogObservado::evento).toList());
            assertEquals("2", logs.getLast().mdc().get("documentos_quantidade"));

            String sinais = observado.getAttributes() + logs.toString();
            assertNaoContemDadosSensiveis(sinais);
        } finally {
            rootLogger.removeHandler(handler);
            tracerProvider.close();
        }
    }

    private static CriteriosConsultaDocumentosDossieProduto criterios() {
        return new CriteriosConsultaDocumentosDossieProduto(
                new IdentificadorDossieProduto(IDENTIFICADOR),
                CNPJ_SENTINELA,
                CPF_SENTINELA,
                7L,
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                IP_SENTINELA,
                TIPOLOGIA_SENTINELA);
    }

    private static DocumentoDossieProdutoConsultado documento() {
        return new DocumentoDossieProdutoConsultado(
                9_000_001L,
                9_000_002L,
                GED_SENTINELA,
                "01/01/2030 10:00:00",
                null,
                MATRICULA_SENTINELA,
                new DocumentoDossieProdutoConsultado.TipoDocumento(
                        9_001L, NOME_SENTINELA, TIPOLOGIA_SENTINELA, true),
                "Criado",
                new DocumentoDossieProdutoConsultado.VinculoDossie(
                        new DocumentoDossieProdutoConsultado.Cliente(
                                CPF_SENTINELA,
                                CNPJ_SENTINELA,
                                NOME_SENTINELA,
                                null,
                                "Proponente",
                                9_000_003L,
                                true),
                        null,
                        null,
                        null,
                        null),
                URL_SENTINELA,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new DocumentoDossieProdutoConsultado.Armazenamento(
                        9_000_004L,
                        "01/01/2030 10:00:01",
                        "GED_RECEBIDO",
                        PATH_SENTINELA,
                        "OBJECT_STORE_SENTINELA",
                        GED_SENTINELA,
                        null,
                        null)));
    }

    private static void assertNaoContemDadosSensiveis(String sinais) {
        assertFalse(sinais.contains(CPF_SENTINELA), sinais);
        assertFalse(sinais.contains(CNPJ_SENTINELA), sinais);
        assertFalse(sinais.contains(IP_SENTINELA), sinais);
        assertFalse(sinais.contains(TIPOLOGIA_SENTINELA), sinais);
        assertFalse(sinais.contains(GED_SENTINELA), sinais);
        assertFalse(sinais.contains(NOME_SENTINELA), sinais);
        assertFalse(sinais.contains(MATRICULA_SENTINELA), sinais);
        assertFalse(sinais.contains(URL_SENTINELA), sinais);
        assertFalse(sinais.contains(PATH_SENTINELA), sinais);
    }

    private static final class FakePortaSaida implements ObterDocumentosDossieProduto {

        private CriteriosConsultaDocumentosDossieProduto criteriosRecebidos;
        private List<DocumentoDossieProdutoConsultado> resposta;
        private RuntimeException falha;

        @Override
        public Uni<List<DocumentoDossieProdutoConsultado>> obter(
                CriteriosConsultaDocumentosDossieProduto criterios
        ) {
            criteriosRecebidos = criterios;
            if (falha != null) {
                return Uni.createFrom().failure(falha);
            }
            return Uni.createFrom().item(resposta);
        }
    }

    private static final class CapturingHandler extends Handler {

        private final List<LogObservado> logs = new ArrayList<>();

        @Override
        public void publish(LogRecord registro) {
            if (registro instanceof ExtLogRecord extLogRecord
                    && registro.getMessage() != null) {
                String throwable = "";
                if (registro.getThrown() != null) {
                    var stacktrace = new StringWriter();
                    registro.getThrown().printStackTrace(new PrintWriter(stacktrace));
                    throwable = stacktrace.toString();
                }
                logs.add(new LogObservado(
                        registro.getMessage(), extLogRecord.getMdcCopy(), throwable));
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

    private record LogObservado(String evento, Map<String, String> mdc, String throwable) {
    }
}
