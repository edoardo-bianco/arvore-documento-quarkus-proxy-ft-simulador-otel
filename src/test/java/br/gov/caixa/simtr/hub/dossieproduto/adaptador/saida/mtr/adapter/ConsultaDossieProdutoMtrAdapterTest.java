package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter;

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

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ConsultaDossieProdutoMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.ConsultaDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v2.consulta.ConsultaDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.ConsultaDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper.ConsultaDossieProdutoMtrMapper;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.ObterDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaConsultaDossieProduto;
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
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ConsultaDossieProdutoMtrAdapterTest {

    private static final long IDENTIFICADOR = 4324680L;
    private static final String SERVICO_MTR = "simtr-dossie-produto";
    private static final String TRACER_TESTE = "teste-consulta-dossie-produto";
    private static final String SPAN_SUCESSO = "teste.consulta-dossie-produto.mtr";
    private static final String SPAN_FALHA = "teste.consulta-dossie-produto.mtr-falha";
    private static final String ATRIBUTO_RESPOSTA_SUCESSO = "mtr.resposta.sucesso";
    private static final String ID_ERRO_404 = "dossie-404";
    private static final String CODIGO_ERRO_404 = "MTR-DOSSIE-404";
    private static final String TIPO_ERRO_NEGOCIO = "Negocio";
    private static final String DETALHE_INDISPONIVEL = "indisponivel";
    private static final String DATA_RESPOSTA = "23/07/2026 10:24:00";
    private static final String CPF_SENTINELA = "CPF_SENTINELA_00000000000";
    private static final String CNPJ_SENTINELA = "CNPJ_SENTINELA_00000000000000";
    private static final String NOME_SENTINELA = "NOME_SENTINELA_CLIENTE";
    private static final String MATRICULA_SENTINELA = "MATRICULA_SENTINELA";

    @Inject
    InMemorySpanExporter spanExporter;

    @Inject
    OpenTelemetry openTelemetry;

    private final CapturingHandler handler = new CapturingHandler();
    private Logger rootLogger;
    private ConsultaDossieProdutoMtrClient client;
    private ConsultaDossieProdutoMtrAdapter adapter;

    @BeforeEach
    void prepararTeste() {
        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        spanExporter.reset();
        rootLogger = Logger.getLogger("");
        handler.setLevel(Level.ALL);
        rootLogger.addHandler(handler);
        client = mock(ConsultaDossieProdutoMtrClient.class);
        adapter = new ConsultaDossieProdutoMtrAdapter(
                client, new ConsultaDossieProdutoMtrMapper()
        );
    }

    @AfterEach
    void limparTeste() {
        rootLogger.removeHandler(handler);
        handler.close();
    }

    @Test
    void implementaPortaComQualifierExclusivoEDeclaraSpanClient() throws Exception {
        assertTrue(ObterDossieProduto.class.isAssignableFrom(
                ConsultaDossieProdutoMtrAdapter.class));
        assertNotNull(ConsultaDossieProdutoMtrAdapter.class
                .getAnnotation(ApplicationScoped.class));
        assertNotNull(ConsultaDossieProdutoMtrAdapter.class
                .getAnnotation(ConsultaDossieProdutoMtr.class));
        assertNotNull(ConsultaDossieProdutoMtr.class.getAnnotation(Qualifier.class));

        var construtor = ConsultaDossieProdutoMtrAdapter.class.getConstructor(
                ConsultaDossieProdutoMtrClient.class,
                ConsultaDossieProdutoMtrMapper.class
        );
        assertNotNull(construtor.getParameters()[0].getAnnotation(RestClient.class));

        var metodo = ConsultaDossieProdutoMtrAdapter.class.getMethod(
                "obter", IdentificadorDossieProduto.class
        );
        var span = metodo.getAnnotation(WithSpan.class);
        assertEquals("mtr.dossie-produto.consultar", span.value());
        assertEquals(SpanKind.CLIENT, span.kind());
    }

    @Test
    void consultaMapeiaRespostaValidaERegistraSomenteIdentificadorEContagens() {
        when(client.consultar(IDENTIFICADOR)).thenReturn(Uni.createFrom().item(respostaValida()));
        var span = openTelemetry.getTracer(TRACER_TESTE)
                .spanBuilder(SPAN_SUCESSO)
                .startSpan();

        try (var _ = span.makeCurrent()) {
            var resultado = adapter.obter(new IdentificadorDossieProduto(IDENTIFICADOR))
                    .await().indefinitely();

            assertEquals(IDENTIFICADOR, resultado.id());
            assertEquals(CPF_SENTINELA, resultado.clientes().getFirst().cpf());
            assertNull(resultado.clientes().get(1));
            assertEquals(2, resultado.unidadesTratamento().size());
            assertEquals(1, resultado.produtosContratados().size());
        } finally {
            span.end();
        }

        verify(client).consultar(IDENTIFICADOR);
        var spanObservado = span(SPAN_SUCESSO);
        assertEquals(SERVICO_MTR,
                spanObservado.getAttributes().get(AttributeKey.stringKey("mtr.servico")));
        assertEquals("dossie-produto-v2",
                spanObservado.getAttributes().get(AttributeKey.stringKey("mtr.api")));
        assertEquals("GET",
                spanObservado.getAttributes().get(AttributeKey.stringKey("http.request.method")));
        assertEquals("/simtr/dossie-produto/v2/dossie-produto/{id}",
                spanObservado.getAttributes().get(AttributeKey.stringKey("url.path")));
        assertEquals(IDENTIFICADOR,
                spanObservado.getAttributes().get(AttributeKey.longKey("dossie_produto.id")));
        assertEquals(2L, spanObservado.getAttributes().get(
                AttributeKey.longKey("dossie_produto.clientes.quantidade")));
        assertEquals(2L, spanObservado.getAttributes().get(
                AttributeKey.longKey("dossie_produto.unidades_tratamento.quantidade")));
        assertEquals(1L, spanObservado.getAttributes().get(
                AttributeKey.longKey("dossie_produto.produtos_contratados.quantidade")));
        assertEquals(true, spanObservado.getAttributes().get(
                AttributeKey.booleanKey(ATRIBUTO_RESPOSTA_SUCESSO)));

        var concluida = log("mtr.dossie-produto.consulta.chamada.concluida");
        assertEquals(String.valueOf(IDENTIFICADOR),
                concluida.mdc().get("dossie_produto_id"));
        assertEquals("2", concluida.mdc().get("clientes_quantidade"));
        assertEquals("2", concluida.mdc().get("unidades_tratamento_quantidade"));
        assertEquals("1", concluida.mdc().get("produtos_contratados_quantidade"));
        assertEquals("sucesso", concluida.mdc().get("resultado"));
        assertSemDadosSensiveis(spanObservado.getAttributes().toString(), logsDaConsulta());
    }

    @Test
    void rejeitaRespostaNulaAntesDoMapper() {
        assertRespostaInvalida(Uni.createFrom().nullItem());
    }

    @Test
    void rejeitaRespostaSemIdentificadorAntesDoMapper() {
        assertRespostaInvalida(Uni.createFrom().item(resposta(null)));
    }

    @Test
    void rejeitaRespostaDeOutroIdentificadorAntesDoMapper() {
        assertRespostaInvalida(Uni.createFrom().item(resposta(9999999L)));
    }

    @Test
    void traduzErroMtrDeNegocioSemPerdaESemVazarDetalhesNaTelemetria() {
        var mensagens = new ArrayList<ConsultaDossieProdutoMtrException.Mensagem>();
        mensagens.add(new ConsultaDossieProdutoMtrException.Mensagem(CPF_SENTINELA));
        mensagens.add(null);
        var erro = new ConsultaDossieProdutoMtrException.Erro(
                404,
                SERVICO_MTR,
                ID_ERRO_404,
                CODIGO_ERRO_404,
                mensagens,
                CNPJ_SENTINELA,
                NOME_SENTINELA
        );
        var origem = new ConsultaDossieProdutoMtrException.Negocio(404, erro);
        when(client.consultar(IDENTIFICADOR)).thenReturn(Uni.createFrom().failure(origem));
        var span = openTelemetry.getTracer(TRACER_TESTE)
                .spanBuilder(SPAN_FALHA)
                .startSpan();

        FalhaConsultaDossieProduto falha;
        try (var _ = span.makeCurrent()) {
            var espera = adapter.obter(new IdentificadorDossieProduto(IDENTIFICADOR)).await();
            falha = assertThrows(FalhaConsultaDossieProduto.class, espera::indefinitely);
        } finally {
            span.end();
        }

        assertEquals(FalhaConsultaDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(404, falha.status());
        assertEquals(SERVICO_MTR, falha.recurso());
        assertEquals(ID_ERRO_404, falha.idErro());
        assertEquals(CODIGO_ERRO_404, falha.codigoErro());
        assertEquals(Arrays.asList(CPF_SENTINELA, null), falha.mensagens());
        assertEquals(CNPJ_SENTINELA, falha.detalhe());
        assertEquals(NOME_SENTINELA, falha.stacktraceExterno());
        assertSame(origem, falha.getCause());

        var spanObservado = span(SPAN_FALHA);
        assertEquals(StatusCode.ERROR, spanObservado.getStatus().getStatusCode());
        assertEquals(false, spanObservado.getAttributes().get(
                AttributeKey.booleanKey(ATRIBUTO_RESPOSTA_SUCESSO)));
        assertEquals(TIPO_ERRO_NEGOCIO, spanObservado.getAttributes().get(
                AttributeKey.stringKey("erro.tipo")));
        assertTrue(spanObservado.getEvents().isEmpty());
        var logFalha = log("mtr.dossie-produto.consulta.chamada.falhou");
        assertEquals(TIPO_ERRO_NEGOCIO, logFalha.mdc().get("erro_tipo"));
        assertEquals("erro", logFalha.mdc().get("resultado"));
        assertNull(logFalha.throwable());
        assertSemDadosSensiveis(spanObservado.getAttributes().toString(), logsDaConsulta());
    }

    @Test
    void traduzErrosTecnicoEServidorParaAsClassificacoesInternas() {
        var tecnicaOrigem = new ConsultaDossieProdutoMtrException.TecnicaCliente(403, null);
        when(client.consultar(IDENTIFICADOR))
                .thenReturn(Uni.createFrom().failure(tecnicaOrigem));

        var tecnica = executarFalha();

        assertEquals(FalhaConsultaDossieProduto.Tipo.TECNICA_CLIENTE, tecnica.tipo());
        assertEquals(403, tecnica.status());
        assertNull(tecnica.recurso());
        assertNull(tecnica.mensagens());
        assertSame(tecnicaOrigem, tecnica.getCause());

        var erro = new ConsultaDossieProdutoMtrException.Erro(
                503, SERVICO_MTR, "dossie-503", "MTR-DOSSIE-503",
                null, DETALHE_INDISPONIVEL, null
        );
        var servidorOrigem = new ConsultaDossieProdutoMtrException.Servidor(503, erro);
        when(client.consultar(IDENTIFICADOR))
                .thenReturn(Uni.createFrom().failure(servidorOrigem));

        var servidor = executarFalha();

        assertEquals(FalhaConsultaDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                servidor.tipo());
        assertEquals(503, servidor.status());
        assertEquals(DETALHE_INDISPONIVEL, servidor.detalhe());
        assertSame(servidorOrigem, servidor.getCause());
    }

    @Test
    void traduzTimeoutEFalhaInesperadaDepoisDoClient() {
        var timeoutOrigem = new TimeoutException("tempo esgotado");
        when(client.consultar(IDENTIFICADOR))
                .thenReturn(Uni.createFrom().failure(timeoutOrigem));

        var timeout = executarFalha();

        assertEquals(FalhaConsultaDossieProduto.Tipo.TIMEOUT, timeout.tipo());
        assertEquals(SERVICO_MTR, timeout.recurso());
        assertSame(timeoutOrigem, timeout.getCause());

        var inesperadaOrigem = new IllegalStateException("falha inesperada");
        when(client.consultar(IDENTIFICADOR))
                .thenReturn(Uni.createFrom().failure(inesperadaOrigem));

        var inesperada = executarFalha();

        assertEquals(FalhaConsultaDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                inesperada.tipo());
        assertEquals(SERVICO_MTR, inesperada.recurso());
        assertSame(inesperadaOrigem, inesperada.getCause());
    }

    private void assertRespostaInvalida(Uni<ConsultaDossieProdutoMtrResponse> resposta) {
        var mapper = mock(ConsultaDossieProdutoMtrMapper.class);
        var adapterIsolado = new ConsultaDossieProdutoMtrAdapter(client, mapper);
        when(client.consultar(IDENTIFICADOR)).thenReturn(resposta);

        var espera = adapterIsolado.obter(new IdentificadorDossieProduto(IDENTIFICADOR)).await();
        var falha = assertThrows(FalhaConsultaDossieProduto.class, espera::indefinitely);

        assertEquals(FalhaConsultaDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                falha.tipo());
        assertEquals(SERVICO_MTR, falha.recurso());
        assertTrue(falha.getCause() instanceof IllegalStateException);
        assertEquals("Resposta MTR invalida para consulta de dossie produto",
                falha.getCause().getMessage());
        verifyNoInteractions(mapper);
    }

    private FalhaConsultaDossieProduto executarFalha() {
        var espera = adapter.obter(new IdentificadorDossieProduto(IDENTIFICADOR)).await();
        return assertThrows(FalhaConsultaDossieProduto.class, espera::indefinitely);
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
        return logsDaConsulta().stream()
                .filter(item -> evento.equals(item.evento()))
                .findFirst()
                .orElseThrow();
    }

    private List<LogObservado> logsDaConsulta() {
        return handler.logs().stream()
                .filter(item -> item.evento().startsWith("mtr.dossie-produto.consulta."))
                .toList();
    }

    private static void assertSemDadosSensiveis(
            String atributosSpan,
            List<LogObservado> logs
    ) {
        String observado = atributosSpan + logs;
        assertFalse(observado.contains(CPF_SENTINELA));
        assertFalse(observado.contains(CNPJ_SENTINELA));
        assertFalse(observado.contains(NOME_SENTINELA));
        assertFalse(observado.contains(MATRICULA_SENTINELA));
    }

    private static ConsultaDossieProdutoMtrResponse respostaValida() {
        return new ConsultaDossieProdutoMtrResponse(
                IDENTIFICADOR,
                1000012592L,
                null,
                null,
                "SIMTRAPI",
                5402,
                DATA_RESPOSTA,
                Arrays.asList(
                        new ConsultaDossieProdutoMtrResponse.Cliente(
                                CPF_SENTINELA,
                                CNPJ_SENTINELA,
                                NOME_SENTINELA,
                                null,
                                "Proponente",
                                40610702L,
                                true
                        ),
                        null
                ),
                null,
                null,
                new ConsultaDossieProdutoMtrResponse.Situacao(
                        1, "Rascunho", DATA_RESPOSTA, MATRICULA_SENTINELA
                ),
                List.of(5402, 5403),
                List.of(new ConsultaDossieProdutoMtrResponse.ProdutoContratado(
                        12, 101, 202, "PRODUTO_SENTINELA"
                ))
        );
    }

    private static ConsultaDossieProdutoMtrResponse resposta(Long identificador) {
        return new ConsultaDossieProdutoMtrResponse(
                identificador,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static final class CapturingHandler extends Handler {

        private final List<LogObservado> logs = new CopyOnWriteArrayList<>();

        @Override
        public void publish(LogRecord logRecord) {
            if (logRecord instanceof ExtLogRecord extLogRecord
                    && logRecord.getMessage() != null) {
                logs.add(new LogObservado(
                        logRecord.getMessage(), extLogRecord.getMdcCopy(), logRecord.getThrown()
                ));
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

    private record LogObservado(
            String evento,
            Map<String, String> mdc,
            Throwable throwable
    ) {
    }
}
