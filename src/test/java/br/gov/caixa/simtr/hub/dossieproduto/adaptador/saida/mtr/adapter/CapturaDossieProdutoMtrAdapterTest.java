package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.CapturaMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.CapturaDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.captura.CapturaDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.CapturaDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper.CapturaDossieProdutoMtrMapper;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarCapturaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaCapturaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
class CapturaDossieProdutoMtrAdapterTest {

    private static final long IDENTIFICADOR = 123L;
    private static final String SERVICO_MTR = "simtr-dossie-produto";
    private static final String TRACER_TESTE = "teste-captura-dossie-produto";
    private static final String SPAN_SUCESSO = "teste.captura-dossie-produto.mtr";
    private static final String SPAN_FALHA = "teste.captura-dossie-produto.mtr-falha";
    private static final String ATRIBUTO_RESPOSTA_SUCESSO = "mtr.resposta.sucesso";
    private static final String MENSAGEM_EXTERNA_SENTINELA = "TOKEN_SENTINELA_BEARER";
    private static final String DETALHE_EXTERNO_SENTINELA = "API_KEY_SENTINELA";
    private static final String STACKTRACE_EXTERNO_SENTINELA = "STACKTRACE_EXTERNO_SENTINELA";

    @Inject
    InMemorySpanExporter spanExporter;

    @Inject
    OpenTelemetry openTelemetry;

    private final CapturingHandler handler = new CapturingHandler();
    private Logger rootLogger;
    private CapturaDossieProdutoMtrClient client;
    private CapturaDossieProdutoMtrAdapter adapter;

    @BeforeEach
    void prepararTeste() {
        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        spanExporter.reset();
        rootLogger = Logger.getLogger("");
        handler.setLevel(Level.ALL);
        rootLogger.addHandler(handler);
        client = mock(CapturaDossieProdutoMtrClient.class);
        adapter = new CapturaDossieProdutoMtrAdapter(
                client, new CapturaDossieProdutoMtrMapper());
    }

    @AfterEach
    void limparTeste() {
        rootLogger.removeHandler(handler);
        handler.close();
    }

    @Test
    void implementaPortaComQualifierExclusivoEDeclaraSpanClient() throws Exception {
        assertTrue(SolicitarCapturaDossieProduto.class.isAssignableFrom(
                CapturaDossieProdutoMtrAdapter.class));
        assertNotNull(CapturaDossieProdutoMtrAdapter.class
                .getAnnotation(ApplicationScoped.class));
        assertNotNull(CapturaDossieProdutoMtrAdapter.class.getAnnotation(CapturaMtr.class));
        assertNotNull(CapturaMtr.class.getAnnotation(Qualifier.class));

        var construtor = CapturaDossieProdutoMtrAdapter.class.getConstructor(
                CapturaDossieProdutoMtrClient.class,
                CapturaDossieProdutoMtrMapper.class);
        assertNotNull(construtor.getParameters()[0].getAnnotation(RestClient.class));

        var metodo = CapturaDossieProdutoMtrAdapter.class.getMethod(
                "capturar", IdentificadorDossieProduto.class);
        var span = metodo.getAnnotation(WithSpan.class);
        assertEquals("mtr.dossie-produto.capturar", span.value());
        assertEquals(SpanKind.CLIENT, span.kind());
    }

    @Test
    void capturaMapeiaRespostaValidaERegistraSomenteDadosPermitidos() {
        when(client.capturar(IDENTIFICADOR)).thenReturn(
                Uni.createFrom().item(new CapturaDossieProdutoMtrResponse(IDENTIFICADOR)));
        var span = openTelemetry.getTracer(TRACER_TESTE)
                .spanBuilder(SPAN_SUCESSO)
                .startSpan();

        try (var _ = span.makeCurrent()) {
            var resultado = adapter.capturar(new IdentificadorDossieProduto(IDENTIFICADOR))
                    .await().indefinitely();

            assertEquals(IDENTIFICADOR, resultado.identificadorDossieProduto());
        } finally {
            span.end();
        }

        verify(client).capturar(IDENTIFICADOR);
        var spanObservado = span(SPAN_SUCESSO);
        assertEquals(SERVICO_MTR,
                spanObservado.getAttributes().get(AttributeKey.stringKey("mtr.servico")));
        assertEquals("dossie-produto-v1",
                spanObservado.getAttributes().get(AttributeKey.stringKey("mtr.api")));
        assertEquals("POST", spanObservado.getAttributes().get(
                AttributeKey.stringKey("http.request.method")));
        assertEquals("/simtr/dossie-produto/v1/dossie-produto/{id}/capturar",
                spanObservado.getAttributes().get(AttributeKey.stringKey("url.path")));
        assertEquals(IDENTIFICADOR, spanObservado.getAttributes().get(
                AttributeKey.longKey("dossie_produto.id")));
        assertEquals(true, spanObservado.getAttributes().get(
                AttributeKey.booleanKey(ATRIBUTO_RESPOSTA_SUCESSO)));

        var concluida = log("mtr.dossie-produto.captura.chamada.concluida");
        assertEquals(String.valueOf(IDENTIFICADOR),
                concluida.mdc().get("dossie_produto_id"));
        assertEquals("sucesso", concluida.mdc().get("resultado"));
        assertSemDadosSensiveis(spanObservado.getAttributes().toString(), logsDaCaptura());
    }

    @Test
    void rejeitaRespostaNulaAntesDoMapper() {
        assertRespostaInvalida(Uni.createFrom().nullItem());
    }

    @Test
    void rejeitaRespostaSemIdentificadorAntesDoMapper() {
        assertRespostaInvalida(Uni.createFrom().item(
                new CapturaDossieProdutoMtrResponse(null)));
    }

    @Test
    void rejeitaRespostaDeOutroIdentificadorAntesDoMapper() {
        assertRespostaInvalida(Uni.createFrom().item(
                new CapturaDossieProdutoMtrResponse(456L)));
    }

    @Test
    void traduzErroMtrDeNegocioSemPerdaESemVazarConteudoExterno() {
        var mensagens = new ArrayList<CapturaDossieProdutoMtrException.Mensagem>();
        mensagens.add(new CapturaDossieProdutoMtrException.Mensagem(
                MENSAGEM_EXTERNA_SENTINELA));
        mensagens.add(null);
        var erro = new CapturaDossieProdutoMtrException.Erro(
                409,
                SERVICO_MTR,
                "captura-409",
                "MTR-CAPTURA-409",
                mensagens,
                DETALHE_EXTERNO_SENTINELA,
                STACKTRACE_EXTERNO_SENTINELA);
        var origem = new CapturaDossieProdutoMtrException.Negocio(409, erro);
        when(client.capturar(IDENTIFICADOR)).thenReturn(Uni.createFrom().failure(origem));
        var span = openTelemetry.getTracer(TRACER_TESTE)
                .spanBuilder(SPAN_FALHA)
                .startSpan();

        FalhaCapturaDossieProduto falha;
        try (var _ = span.makeCurrent()) {
            var espera = adapter.capturar(new IdentificadorDossieProduto(IDENTIFICADOR)).await();
            falha = assertThrows(FalhaCapturaDossieProduto.class, espera::indefinitely);
        } finally {
            span.end();
        }

        assertEquals(FalhaCapturaDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(409, falha.status());
        assertEquals(SERVICO_MTR, falha.recurso());
        assertEquals("captura-409", falha.idErro());
        assertEquals("MTR-CAPTURA-409", falha.codigoErro());
        assertEquals(Arrays.asList(MENSAGEM_EXTERNA_SENTINELA, null), falha.mensagens());
        assertEquals(DETALHE_EXTERNO_SENTINELA, falha.detalhe());
        assertEquals(STACKTRACE_EXTERNO_SENTINELA, falha.stacktraceExterno());
        assertSame(origem, falha.getCause());

        var spanObservado = span(SPAN_FALHA);
        assertEquals(StatusCode.ERROR, spanObservado.getStatus().getStatusCode());
        assertEquals("falha na captura MTR de dossie produto",
                spanObservado.getStatus().getDescription());
        assertEquals(false, spanObservado.getAttributes().get(
                AttributeKey.booleanKey(ATRIBUTO_RESPOSTA_SUCESSO)));
        assertEquals("Negocio", spanObservado.getAttributes().get(
                AttributeKey.stringKey("erro.tipo")));
        assertTrue(spanObservado.getEvents().isEmpty());
        var logFalha = log("mtr.dossie-produto.captura.chamada.falhou");
        assertEquals("Negocio", logFalha.mdc().get("erro_tipo"));
        assertEquals("erro", logFalha.mdc().get("resultado"));
        assertNull(logFalha.throwable());
        assertSemDadosSensiveis(spanObservado.getAttributes().toString(), logsDaCaptura());
    }

    @Test
    void traduzErrosTecnicoEServidorParaAsClassificacoesInternas() {
        var tecnicaOrigem = new CapturaDossieProdutoMtrException.TecnicaCliente(403, null);
        when(client.capturar(IDENTIFICADOR)).thenReturn(
                Uni.createFrom().failure(tecnicaOrigem));

        var tecnica = executarFalha();

        assertEquals(FalhaCapturaDossieProduto.Tipo.TECNICA_CLIENTE, tecnica.tipo());
        assertEquals(403, tecnica.status());
        assertNull(tecnica.recurso());
        assertNull(tecnica.mensagens());
        assertSame(tecnicaOrigem, tecnica.getCause());

        var erro = new CapturaDossieProdutoMtrException.Erro(
                503, SERVICO_MTR, "captura-503", "MTR-CAPTURA-503",
                null, "indisponivel", null);
        var servidorOrigem = new CapturaDossieProdutoMtrException.Servidor(503, erro);
        when(client.capturar(IDENTIFICADOR)).thenReturn(
                Uni.createFrom().failure(servidorOrigem));

        var servidor = executarFalha();

        assertEquals(FalhaCapturaDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                servidor.tipo());
        assertEquals(503, servidor.status());
        assertEquals("indisponivel", servidor.detalhe());
        assertSame(servidorOrigem, servidor.getCause());
    }

    @Test
    void traduzTimeoutEFalhaInesperadaDepoisDoClient() {
        var timeoutOrigem = new TimeoutException("tempo esgotado");
        when(client.capturar(IDENTIFICADOR)).thenReturn(
                Uni.createFrom().failure(timeoutOrigem));

        var timeout = executarFalha();

        assertEquals(FalhaCapturaDossieProduto.Tipo.TIMEOUT, timeout.tipo());
        assertEquals(SERVICO_MTR, timeout.recurso());
        assertSame(timeoutOrigem, timeout.getCause());

        var inesperadaOrigem = new IllegalStateException("falha inesperada");
        when(client.capturar(IDENTIFICADOR)).thenReturn(
                Uni.createFrom().failure(inesperadaOrigem));

        var inesperada = executarFalha();

        assertEquals(FalhaCapturaDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                inesperada.tipo());
        assertEquals(SERVICO_MTR, inesperada.recurso());
        assertSame(inesperadaOrigem, inesperada.getCause());
    }

    private void assertRespostaInvalida(Uni<CapturaDossieProdutoMtrResponse> resposta) {
        var mapper = mock(CapturaDossieProdutoMtrMapper.class);
        var adapterIsolado = new CapturaDossieProdutoMtrAdapter(client, mapper);
        when(client.capturar(IDENTIFICADOR)).thenReturn(resposta);

        var espera = adapterIsolado.capturar(new IdentificadorDossieProduto(IDENTIFICADOR))
                .await();
        var falha = assertThrows(FalhaCapturaDossieProduto.class, espera::indefinitely);

        assertEquals(FalhaCapturaDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                falha.tipo());
        assertEquals(SERVICO_MTR, falha.recurso());
        assertTrue(falha.getCause() instanceof IllegalStateException);
        assertEquals("Resposta MTR invalida para captura de dossie produto",
                falha.getCause().getMessage());
        verifyNoInteractions(mapper);
    }

    private FalhaCapturaDossieProduto executarFalha() {
        var espera = adapter.capturar(new IdentificadorDossieProduto(IDENTIFICADOR)).await();
        return assertThrows(FalhaCapturaDossieProduto.class, espera::indefinitely);
    }

    private io.opentelemetry.sdk.trace.data.SpanData span(String nome) {
        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        return spanExporter.getFinishedSpanItems().stream()
                .filter(item -> nome.equals(item.getName()))
                .findFirst()
                .orElseThrow();
    }

    private LogObservado log(String evento) {
        return logsDaCaptura().stream()
                .filter(item -> evento.equals(item.evento()))
                .findFirst()
                .orElseThrow();
    }

    private List<LogObservado> logsDaCaptura() {
        return handler.logs().stream()
                .filter(item -> item.evento().startsWith("mtr.dossie-produto.captura."))
                .toList();
    }

    private static void assertSemDadosSensiveis(
            String atributosSpan,
            List<LogObservado> logs
    ) {
        String observado = atributosSpan + logs;
        assertFalse(observado.contains(MENSAGEM_EXTERNA_SENTINELA));
        assertFalse(observado.contains(DETALHE_EXTERNO_SENTINELA));
        assertFalse(observado.contains(STACKTRACE_EXTERNO_SENTINELA));
    }

    private static final class CapturingHandler extends Handler {

        private final List<LogObservado> logs = new CopyOnWriteArrayList<>();

        @Override
        public void publish(LogRecord logRecord) {
            if (logRecord instanceof ExtLogRecord extLogRecord
                    && logRecord.getMessage() != null) {
                logs.add(new LogObservado(
                        logRecord.getMessage(), extLogRecord.getMdcCopy(), logRecord.getThrown()));
            }
        }

        @Override
        public void flush() {
            // No-op deliberado: o appender de teste nao mantem estado pendente.
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
            String evento,
            Map<String, String> mdc,
            Throwable throwable
    ) {
    }
}
