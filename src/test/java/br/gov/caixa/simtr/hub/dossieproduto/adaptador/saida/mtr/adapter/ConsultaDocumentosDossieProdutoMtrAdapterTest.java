package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.ConsultaDocumentosDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v4.documentos.ConsultaDocumentosDossieProdutoMtrQuery;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v4.documentos.ConsultaDocumentosDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.ConsultaDocumentosDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper.ConsultaDocumentosDossieProdutoMtrMapper;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.ObterDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.CriteriosConsultaDocumentosDossieProduto;
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
import jakarta.ws.rs.ProcessingException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class ConsultaDocumentosDossieProdutoMtrAdapterTest {

    private static final long IDENTIFICADOR = 4_081_899L;
    private static final String CNPJ = "CNPJ_SENTINELA_00000000000000";
    private static final String CPF = "CPF_SENTINELA_00000000000";
    private static final String IP = "IP_SENTINELA_192_0_2_10";
    private static final String TIPOLOGIA = "TIPOLOGIA_SENTINELA";
    private static final String URL = "URL_SENTINELA_DOCUMENTO";
    private static final String GED = "GED_SENTINELA";
    private static final String MATRICULA = "MATRICULA_SENTINELA";
    private static final String NOME = "NOME_SENTINELA";
    private static final String PATH_STORAGE = "PATH_SENTINELA_STORAGE";
    private static final String SERVICO_MTR = "simtr-dossie-produto";
    private static final String ID_ERRO_404 = "documentos-404";
    private static final String CODIGO_ERRO_404 = "MTR-DOCUMENTOS-404";
    private static final String TRACER_TESTE = "teste-consulta-documentos";
    private static final String SPAN_SUCESSO = "teste.documentos.mtr.sucesso";
    private static final String SPAN_FALHA = "teste.documentos.mtr.falha";

    @Inject
    InMemorySpanExporter spanExporter;

    @Inject
    OpenTelemetry openTelemetry;

    private final CapturingHandler handler = new CapturingHandler();
    private Logger rootLogger;
    private ConsultaDocumentosDossieProdutoMtrClient client;
    private ConsultaDocumentosDossieProdutoMtrAdapter adapter;

    @BeforeEach
    void prepararTeste() {
        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        spanExporter.reset();
        rootLogger = Logger.getLogger("");
        handler.setLevel(Level.ALL);
        rootLogger.addHandler(handler);
        client = mock(ConsultaDocumentosDossieProdutoMtrClient.class);
        adapter = new ConsultaDocumentosDossieProdutoMtrAdapter(
                client, new ConsultaDocumentosDossieProdutoMtrMapper());
    }

    @AfterEach
    void limparTeste() {
        rootLogger.removeHandler(handler);
        handler.close();
    }

    @Test
    void implementaPortaEDeclaraClientRestESpanAprovado() throws NoSuchMethodException {
        assertTrue(ObterDocumentosDossieProduto.class.isAssignableFrom(
                ConsultaDocumentosDossieProdutoMtrAdapter.class));
        assertNotNull(ConsultaDocumentosDossieProdutoMtrAdapter.class
                .getAnnotation(ApplicationScoped.class));

        var construtor = ConsultaDocumentosDossieProdutoMtrAdapter.class.getConstructor(
                ConsultaDocumentosDossieProdutoMtrClient.class,
                ConsultaDocumentosDossieProdutoMtrMapper.class);
        assertNotNull(construtor.getParameters()[0].getAnnotation(RestClient.class));

        var metodo = ConsultaDocumentosDossieProdutoMtrAdapter.class.getMethod(
                "obter", CriteriosConsultaDocumentosDossieProduto.class);
        WithSpan span = metodo.getAnnotation(WithSpan.class);
        assertNotNull(span);
        assertEquals("mtr.dossie-produto.documentos.consultar", span.value());
        assertEquals(SpanKind.CLIENT, span.kind());
    }

    @Test
    void preservaOsDozeFiltrosEMapeiaAListaDeResposta() {
        when(client.consultar(eq(IDENTIFICADOR), any(ConsultaDocumentosDossieProdutoMtrQuery.class)))
                .thenReturn(Uni.createFrom().item(List.of(resposta())));

        var documentos = adapter.obter(criterios()).await().indefinitely();

        ArgumentCaptor<ConsultaDocumentosDossieProdutoMtrQuery> queryCaptor =
                ArgumentCaptor.forClass(ConsultaDocumentosDossieProdutoMtrQuery.class);
        verify(client).consultar(eq(IDENTIFICADOR), queryCaptor.capture());
        var query = queryCaptor.getValue();
        assertEquals(CNPJ, query.cnpj());
        assertEquals(CPF, query.cpf());
        assertEquals(7L, query.fase());
        assertEquals(true, query.incluiArmazenamento());
        assertEquals(false, query.incluiAssinaturas());
        assertEquals(true, query.incluiAtributos());
        assertEquals(false, query.incluiConformidade());
        assertEquals(true, query.incluiOutsourcing());
        assertEquals(false, query.incluiPropriedades());
        assertEquals(true, query.incluiUrl());
        assertEquals(IP, query.ipUsuario());
        assertEquals(TIPOLOGIA, query.tipologia());

        assertEquals(1, documentos.size());
        assertEquals(90_000_001L, documentos.getFirst().idInstanciaDocumento());
        assertEquals(CPF, documentos.getFirst().vinculoDossie().cliente().cpf());
        assertEquals(URL, documentos.getFirst().url());
    }

    @Test
    void normalizaRespostaNulaOuVaziaComoListaVazia() {
        when(client.consultar(eq(IDENTIFICADOR), any(ConsultaDocumentosDossieProdutoMtrQuery.class)))
                .thenReturn(Uni.createFrom().nullItem())
                .thenReturn(Uni.createFrom().item(List.of()));

        var respostaNula = adapter.obter(criterios()).await().indefinitely();
        var respostaVazia = adapter.obter(criterios()).await().indefinitely();

        assertNotNull(respostaNula);
        assertTrue(respostaNula.isEmpty());
        assertNotNull(respostaVazia);
        assertTrue(respostaVazia.isEmpty());
    }

    @Test
    void registraSomenteRotaTemplatedIdentificadorEContagemNoSpanDeSucesso() {
        when(client.consultar(eq(IDENTIFICADOR), any(ConsultaDocumentosDossieProdutoMtrQuery.class)))
                .thenReturn(Uni.createFrom().item(List.of(resposta())));
        var span = openTelemetry.getTracer(TRACER_TESTE)
                .spanBuilder(SPAN_SUCESSO)
                .startSpan();

        try (var _ = span.makeCurrent()) {
            adapter.obter(criterios()).await().indefinitely();
        } finally {
            span.end();
        }

        var observado = span(SPAN_SUCESSO);
        assertEquals(SERVICO_MTR,
                observado.getAttributes().get(AttributeKey.stringKey("mtr.servico")));
        assertEquals("dossie-produto-v4",
                observado.getAttributes().get(AttributeKey.stringKey("mtr.api")));
        assertEquals("GET",
                observado.getAttributes().get(AttributeKey.stringKey("http.request.method")));
        assertEquals("/simtr/dossie-produto/v4/dossie-produto/{id}/documentos",
                observado.getAttributes().get(AttributeKey.stringKey("url.path")));
        assertEquals(IDENTIFICADOR,
                observado.getAttributes().get(AttributeKey.longKey("dossie_produto.id")));
        assertEquals(1L, observado.getAttributes().get(
                AttributeKey.longKey("dossie_produto.documentos.quantidade")));
        assertEquals(true, observado.getAttributes().get(
                AttributeKey.booleanKey("mtr.resposta.sucesso")));
        assertSemDadosSensiveis(observado.getAttributes().toString());
    }

    @Test
    void traduzErroMtrDeNegocioParaFalhaInternaSemPerderPayload() {
        var mensagens = new ArrayList<ConsultaDocumentosDossieProdutoMtrException.Mensagem>();
        mensagens.add(new ConsultaDocumentosDossieProdutoMtrException.Mensagem(
                "MENSAGEM_EXTERNA_SENTINELA"));
        mensagens.add(null);
        var erro = new ConsultaDocumentosDossieProdutoMtrException.Erro(
                404,
                SERVICO_MTR,
                ID_ERRO_404,
                CODIGO_ERRO_404,
                mensagens,
                "DETALHE_EXTERNO_SENTINELA",
                "STACKTRACE_EXTERNO_SENTINELA");
        var origem = new ConsultaDocumentosDossieProdutoMtrException.Negocio(404, erro);
        when(client.consultar(eq(IDENTIFICADOR), any(ConsultaDocumentosDossieProdutoMtrQuery.class)))
                .thenReturn(Uni.createFrom().failure(origem));

        var espera = adapter.obter(criterios()).await();
        FalhaConsultaDocumentosDossieProduto falha = assertThrows(
                FalhaConsultaDocumentosDossieProduto.class,
                espera::indefinitely);

        assertEquals(FalhaConsultaDocumentosDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(404, falha.status());
        assertEquals(SERVICO_MTR, falha.recurso());
        assertEquals(ID_ERRO_404, falha.idErro());
        assertEquals(CODIGO_ERRO_404, falha.codigoErro());
        assertEquals(Arrays.asList("MENSAGEM_EXTERNA_SENTINELA", null), falha.mensagens());
        assertEquals("DETALHE_EXTERNO_SENTINELA", falha.detalhe());
        assertEquals("STACKTRACE_EXTERNO_SENTINELA", falha.stacktraceExterno());
        assertSame(origem, falha.getCause());
    }

    @Test
    void distingueErroTecnicoDeClienteEIndisponibilidadeDoServidor() {
        var tecnicaOrigem =
                new ConsultaDocumentosDossieProdutoMtrException.TecnicaCliente(403, null);
        when(client.consultar(eq(IDENTIFICADOR), any(ConsultaDocumentosDossieProdutoMtrQuery.class)))
                .thenReturn(Uni.createFrom().failure(tecnicaOrigem));

        var tecnica = executarFalha();

        assertEquals(FalhaConsultaDocumentosDossieProduto.Tipo.TECNICA_CLIENTE,
                tecnica.tipo());
        assertEquals(403, tecnica.status());
        assertNull(tecnica.recurso());
        assertNull(tecnica.mensagens());
        assertSame(tecnicaOrigem, tecnica.getCause());

        var erro = new ConsultaDocumentosDossieProdutoMtrException.Erro(
                503,
                SERVICO_MTR,
                "documentos-503",
                "MTR-DOCUMENTOS-503",
                null,
                "indisponivel",
                null);
        var servidorOrigem =
                new ConsultaDocumentosDossieProdutoMtrException.Servidor(503, erro);
        when(client.consultar(eq(IDENTIFICADOR), any(ConsultaDocumentosDossieProdutoMtrQuery.class)))
                .thenReturn(Uni.createFrom().failure(servidorOrigem));

        var servidor = executarFalha();

        assertEquals(FalhaConsultaDocumentosDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                servidor.tipo());
        assertEquals(503, servidor.status());
        assertEquals("indisponivel", servidor.detalhe());
        assertSame(servidorOrigem, servidor.getCause());
    }

    @Test
    void classificaTimeoutETransientesDepoisDaPoliticaDoClient() {
        var timeoutOrigem = new TimeoutException("tempo esgotado");
        when(client.consultar(eq(IDENTIFICADOR), any(ConsultaDocumentosDossieProdutoMtrQuery.class)))
                .thenReturn(Uni.createFrom().failure(timeoutOrigem));

        var timeout = executarFalha();

        assertEquals(FalhaConsultaDocumentosDossieProduto.Tipo.TIMEOUT, timeout.tipo());
        assertEquals(SERVICO_MTR, timeout.recurso());
        assertSame(timeoutOrigem, timeout.getCause());

        var processamentoOrigem = new ProcessingException("conexao interrompida");
        when(client.consultar(eq(IDENTIFICADOR), any(ConsultaDocumentosDossieProdutoMtrQuery.class)))
                .thenReturn(Uni.createFrom().failure(processamentoOrigem));

        var processamento = executarFalha();

        assertEquals(FalhaConsultaDocumentosDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                processamento.tipo());
        assertEquals(SERVICO_MTR, processamento.recurso());
        assertSame(processamentoOrigem, processamento.getCause());

        var inesperadaOrigem = new IllegalStateException("falha inesperada");
        when(client.consultar(eq(IDENTIFICADOR), any(ConsultaDocumentosDossieProdutoMtrQuery.class)))
                .thenReturn(Uni.createFrom().failure(inesperadaOrigem));

        var inesperada = executarFalha();

        assertEquals(FalhaConsultaDocumentosDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                inesperada.tipo());
        assertEquals(SERVICO_MTR, inesperada.recurso());
        assertSame(inesperadaOrigem, inesperada.getCause());
    }

    @Test
    void registraSomenteClassificacaoSeguraNoSpanDeFalha() {
        var erro = new ConsultaDocumentosDossieProdutoMtrException.Erro(
                404,
                SERVICO_MTR,
                ID_ERRO_404,
                CODIGO_ERRO_404,
                List.of(new ConsultaDocumentosDossieProdutoMtrException.Mensagem(CPF)),
                CNPJ,
                IP);
        var origem = new ConsultaDocumentosDossieProdutoMtrException.Negocio(404, erro);
        when(client.consultar(eq(IDENTIFICADOR), any(ConsultaDocumentosDossieProdutoMtrQuery.class)))
                .thenReturn(Uni.createFrom().failure(origem));
        var span = openTelemetry.getTracer(TRACER_TESTE)
                .spanBuilder(SPAN_FALHA)
                .startSpan();

        try (var _ = span.makeCurrent()) {
            var espera = adapter.obter(criterios()).await();
            assertThrows(FalhaConsultaDocumentosDossieProduto.class, espera::indefinitely);
        } finally {
            span.end();
        }

        var observado = span(SPAN_FALHA);
        assertEquals(StatusCode.ERROR, observado.getStatus().getStatusCode());
        assertEquals(false, observado.getAttributes().get(
                AttributeKey.booleanKey("mtr.resposta.sucesso")));
        assertEquals("Negocio",
                observado.getAttributes().get(AttributeKey.stringKey("erro.tipo")));
        assertTrue(observado.getEvents().isEmpty());
        assertSemDadosSensiveis(observado.getAttributes().toString());
    }

    @Test
    void registraLogsComSomenteIdentificadorContagemResultadoEClassificacao() {
        when(client.consultar(eq(IDENTIFICADOR), any(ConsultaDocumentosDossieProdutoMtrQuery.class)))
                .thenReturn(Uni.createFrom().item(List.of(resposta())));

        adapter.obter(criterios()).await().indefinitely();

        var iniciada = log("mtr.dossie-produto.documentos.consulta.chamada.iniciada");
        assertEquals(String.valueOf(IDENTIFICADOR),
                iniciada.mdc().get("dossie_produto_id"));
        assertNull(iniciada.throwable());
        var concluida = log("mtr.dossie-produto.documentos.consulta.chamada.concluida");
        assertEquals("1", concluida.mdc().get("documentos_quantidade"));
        assertEquals("sucesso", concluida.mdc().get("resultado"));
        assertNull(concluida.throwable());

        var erro = new ConsultaDocumentosDossieProdutoMtrException.Erro(
                404,
                SERVICO_MTR,
                ID_ERRO_404,
                CODIGO_ERRO_404,
                List.of(new ConsultaDocumentosDossieProdutoMtrException.Mensagem(CPF)),
                CNPJ,
                IP);
        when(client.consultar(eq(IDENTIFICADOR), any(ConsultaDocumentosDossieProdutoMtrQuery.class)))
                .thenReturn(Uni.createFrom().failure(
                        new ConsultaDocumentosDossieProdutoMtrException.Negocio(404, erro)));

        var espera = adapter.obter(criterios()).await();
        assertThrows(FalhaConsultaDocumentosDossieProduto.class, espera::indefinitely);

        var falhou = log("mtr.dossie-produto.documentos.consulta.chamada.falhou");
        assertEquals("Negocio", falhou.mdc().get("erro_tipo"));
        assertEquals("erro", falhou.mdc().get("resultado"));
        assertNull(falhou.throwable());
        assertSemDadosSensiveis(logsDaConsulta().toString());
    }

    private FalhaConsultaDocumentosDossieProduto executarFalha() {
        var espera = adapter.obter(criterios()).await();
        return assertThrows(FalhaConsultaDocumentosDossieProduto.class, espera::indefinitely);
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
                .filter(item -> item.evento().startsWith(
                        "mtr.dossie-produto.documentos.consulta."))
                .toList();
    }

    private static void assertSemDadosSensiveis(String observado) {
        assertFalse(observado.contains(CNPJ));
        assertFalse(observado.contains(CPF));
        assertFalse(observado.contains(IP));
        assertFalse(observado.contains(TIPOLOGIA));
        assertFalse(observado.contains(URL));
        assertFalse(observado.contains(GED));
        assertFalse(observado.contains(MATRICULA));
        assertFalse(observado.contains(NOME));
        assertFalse(observado.contains(PATH_STORAGE));
    }

    private static CriteriosConsultaDocumentosDossieProduto criterios() {
        return new CriteriosConsultaDocumentosDossieProduto(
                new IdentificadorDossieProduto(IDENTIFICADOR),
                CNPJ,
                CPF,
                7L,
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                IP,
                TIPOLOGIA);
    }

    private static ConsultaDocumentosDossieProdutoMtrResponse resposta() {
        return new ConsultaDocumentosDossieProdutoMtrResponse(
                90_000_001L,
                90_000_002L,
                GED,
                "01/01/2030 10:00:00",
                null,
                MATRICULA,
                new ConsultaDocumentosDossieProdutoMtrResponse.TipoDocumento(
                        9_001L, "DOCUMENTO SENTINELA", "TIPO-01", true),
                "Criado",
                new ConsultaDocumentosDossieProdutoMtrResponse.VinculoDossie(
                        new ConsultaDocumentosDossieProdutoMtrResponse.Cliente(
                                CPF, CNPJ, NOME, null,
                                "Proponente", 90_000_003L, true),
                        null,
                        null,
                        null,
                        null),
                URL,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new ConsultaDocumentosDossieProdutoMtrResponse.Armazenamento(
                        90_000_004L,
                        "01/01/2030 10:00:01",
                        "GED_RECEBIDO",
                        PATH_STORAGE,
                        "OBJECT_STORE_SENTINELA",
                        GED,
                        null,
                        null)));
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
