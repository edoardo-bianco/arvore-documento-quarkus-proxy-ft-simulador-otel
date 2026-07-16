package br.gov.caixa.simtr.hub.arvoredocumento.caracterizacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.gov.caixa.simtr.hub.arvoredocumento.aplicacao.porta.saida.ObterProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.erro.FalhaConsultaProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.IdentificadorNegocialProcesso;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.jboss.logmanager.ExtLogRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(
        value = ProcessoParametrizadoMtrStubTestResource.class,
        restrictToAnnotatedClass = true
)
class ProcessoParametrizadoNovoClientQuarkusTest {

    private static final long IDENTIFICADOR = 1000016487L;
    private static final String RESPOSTA_MTR = """
            {
              "identificador_negocial": 1000016487,
              "nome": "Processo no novo client",
              "ativo": true,
              "ultima_alteracao": null,
              "indicador_produto_obrigatorio": false,
              "macroprocesso": null,
              "relacionamentos": [null],
              "produtos": null,
              "fases": [],
              "documentos": [{
                "funcao_documental": null,
                "tipo_documento": null,
                "obrigatorio": false
              }],
              "checklist": [{
                "identificador_checklist": "1234",
                "versao_checklist": "2"
              }]
            }
            """;

    @Inject
    ObterProcessoParametrizado portaSaida;

    @Inject
    InMemorySpanExporter spanExporter;

    @Inject
    OpenTelemetry openTelemetry;

    private final CapturingHandler handler = new CapturingHandler();
    private Logger rootLogger;

    @BeforeEach
    void resetarStubETelemetria() {
        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        spanExporter.reset();
        ProcessoParametrizadoMtrStubTestResource.reset();
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
    void percorreNovaPortaAteOStubPreservandoWireNulosHeadersETelemetria() {
        ProcessoParametrizadoMtrStubTestResource.responder(200, RESPOSTA_MTR);

        var processo = portaSaida.obter(new IdentificadorNegocialProcesso(IDENTIFICADOR))
                .await().indefinitely();

        assertEquals(IDENTIFICADOR, processo.identificadorNegocial());
        assertEquals("Processo no novo client", processo.nome());
        assertNull(processo.ultimaAlteracao());
        assertNull(processo.macroprocesso());
        assertNull(processo.relacionamentos().getFirst());
        assertNull(processo.produtos());
        assertTrue(processo.fases().isEmpty());
        assertEquals(false, processo.documentos().getFirst().obrigatorio());
        assertEquals(1234L, processo.checklist().identificadorChecklist());
        assertEquals(2, processo.checklist().versaoChecklist());

        List<ProcessoParametrizadoMtrStubTestResource.CapturedRequest> requisicoes =
                ProcessoParametrizadoMtrStubTestResource.requisicoes();
        assertEquals(1, requisicoes.size());
        var requisicao = requisicoes.getFirst();
        assertEquals("GET", requisicao.method());
        assertEquals(ProcessoParametrizadoMtrStubTestResource.CAMINHO_PROCESSO + IDENTIFICADOR,
                requisicao.path());
        assertEquals("", requisicao.body());
        assertNull(requisicao.contentType());
        assertTrue(requisicao.accept().contains("application/json"));
        assertEquals("test-apikey", requisicao.apikey());
        assertEquals("Bearer stub-access-token", requisicao.authorization());
        assertNotNull(requisicao.traceparent());
        assertTrue(requisicao.traceparent().matches(
                "00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}"
        ));

        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        SpanData adapter = spanExporter.getFinishedSpanItems().stream()
                .filter(span -> "mtr.parametrizacao.processo.consultar".equals(span.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(SpanKind.CLIENT, adapter.getKind());
        assertEquals("simtr-parametrizacao",
                adapter.getAttributes().get(AttributeKey.stringKey("mtr.servico")));
        assertEquals("patriarca-processo-v2",
                adapter.getAttributes().get(AttributeKey.stringKey("mtr.api")));
        assertEquals("GET",
                adapter.getAttributes().get(AttributeKey.stringKey("http.request.method")));
        assertEquals(
                "/simtr-parametrizacao/v2/patriarca/processo/identificador-negocial/{identificador}",
                adapter.getAttributes().get(AttributeKey.stringKey("url.path"))
        );
        assertEquals(IDENTIFICADOR, adapter.getAttributes().get(AttributeKey.longKey(
                "mtr.parametrizacao.processo.identificador_negocial"
        )));
        assertEquals(true,
                adapter.getAttributes().get(AttributeKey.booleanKey("mtr.resposta.sucesso")));
        assertEquals("Processo no novo client",
                adapter.getAttributes().get(AttributeKey.stringKey("processo.nome")));

        var inicio = log("mtr.parametrizacao.processo.chamada.iniciada");
        assertEquals("infrastructure", inicio.mdc().get("camada"));
        assertEquals("ParametrizacaoProcessoGateway", inicio.mdc().get("componente"));
        assertEquals("simtr-parametrizacao", inicio.mdc().get("dependencia"));
        assertEquals("consultar-processo-parametrizacao-v2", inicio.mdc().get("operacao"));
        assertEquals(String.valueOf(IDENTIFICADOR), inicio.mdc().get("identificador_negocial"));

        var provider = log("mtr.rest-client.request.enviada");
        assertEquals("ParametrizacaoProcessoClient", provider.mdc().get("rest_client"));
        assertEquals("consultarPorIdentificadorNegocial", provider.mdc().get("operacao"));
        assertEquals("GET", provider.mdc().get("http_method"));
        assertEquals(ProcessoParametrizadoMtrStubTestResource.CAMINHO_PROCESSO + IDENTIFICADOR,
                provider.mdc().get("url_path"));
    }

    @Test
    void traduzErroDeNegocioCompletoSemRetry() {
        ProcessoParametrizadoMtrStubTestResource.responder(404, """
                {
                  "codigo_http": 404,
                  "recurso": "simtr-parametrizacao",
                  "id_erro": "processo-novo-404",
                  "codigo_erro": "MTR-PROCESSO-404",
                  "erros": [{"mensagem": "processo nao localizado"}],
                  "detalhe": "falha de negocio controlada"
                }
                """);

        var falha = assertThrows(
                FalhaConsultaProcessoParametrizado.class,
                () -> portaSaida.obter(new IdentificadorNegocialProcesso(IDENTIFICADOR))
                        .await().indefinitely()
        );

        assertEquals(FalhaConsultaProcessoParametrizado.Tipo.NEGOCIO, falha.tipo());
        assertEquals(404, falha.status());
        assertEquals("simtr-parametrizacao", falha.recurso());
        assertEquals("processo-novo-404", falha.idErro());
        assertEquals("MTR-PROCESSO-404", falha.codigoErro());
        assertEquals(List.of("processo nao localizado"), falha.mensagens());
        assertEquals("falha de negocio controlada", falha.detalhe());
        assertEquals(1, ProcessoParametrizadoMtrStubTestResource.requisicoes().size());
        var logFalha = log("mtr.parametrizacao.processo.chamada.falhou");
        assertEquals("erro", logFalha.mdc().get("resultado"));
        assertEquals("MtrBusinessErrorException", logFalha.mdc().get("erro_tipo"));

        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        SpanData adapter = spanExporter.getFinishedSpanItems().stream()
                .filter(span -> "mtr.parametrizacao.processo.consultar".equals(span.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(false,
                adapter.getAttributes().get(AttributeKey.booleanKey("mtr.resposta.sucesso")));
        assertEquals(
                "br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException",
                adapter.getAttributes().get(AttributeKey.stringKey("erro.tipo"))
        );
    }

    @Test
    void retryRecuperaErroServidorComDuasRequisicoesGetIdenticas() {
        ProcessoParametrizadoMtrStubTestResource.responder(
                500,
                "{\"codigo_http\":500,\"recurso\":\"simtr-parametrizacao\"}"
        );
        ProcessoParametrizadoMtrStubTestResource.responder(200, RESPOSTA_MTR);

        var processo = portaSaida.obter(new IdentificadorNegocialProcesso(IDENTIFICADOR))
                .await().indefinitely();

        assertEquals(IDENTIFICADOR, processo.identificadorNegocial());
        List<ProcessoParametrizadoMtrStubTestResource.CapturedRequest> requisicoes =
                ProcessoParametrizadoMtrStubTestResource.requisicoes();
        assertEquals(2, requisicoes.size());
        assertTrue(requisicoes.stream().allMatch(requisicao -> "GET".equals(requisicao.method())));
        assertTrue(requisicoes.stream().allMatch(requisicao -> requisicao.path().equals(
                ProcessoParametrizadoMtrStubTestResource.CAMINHO_PROCESSO + IDENTIFICADOR
        )));
        assertTrue(requisicoes.stream().allMatch(requisicao -> requisicao.body().isEmpty()));
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
            // No-op deliberado: appender de teste sem estado pendente.
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
