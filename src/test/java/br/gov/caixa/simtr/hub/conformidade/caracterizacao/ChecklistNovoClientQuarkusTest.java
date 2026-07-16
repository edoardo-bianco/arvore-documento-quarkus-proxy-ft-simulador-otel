package br.gov.caixa.simtr.hub.conformidade.caracterizacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ObterChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaConsultaChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ComandoConsultaChecklist;
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
@QuarkusTestResource(
        value = ChecklistMtrStubTestResource.class,
        restrictToAnnotatedClass = true
)
class ChecklistNovoClientQuarkusTest {

    private static final long IDENTIFICADOR = 1000012583L;
    private static final int VERSAO = 7;
    private static final String RESPOSTA_MTR = """
            {
              "nome": "Checklist no novo client",
              "identificador_negocial": 1000012583,
              "versao": 7,
              "data_hora_criacao": null,
              "data_hora_ultima_alteracao": "14/07/2026 12:00:00",
              "verificacao_previa": false,
              "orientacao_operador": null,
              "apontamentos": [null, {
                "identificador_negocial": 2001,
                "nome": null,
                "descricao": "Descricao preservada",
                "orientacao_operador": null,
                "indicador_reanalise": false,
                "sequencia_apresentacao": 1
              }]
            }
            """;

    @Inject
    ObterChecklist portaSaida;

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
        ChecklistMtrStubTestResource.reset();
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
        ChecklistMtrStubTestResource.responder(200, RESPOSTA_MTR);

        var checklist = portaSaida.obter(new ComandoConsultaChecklist(IDENTIFICADOR, VERSAO))
                .await().indefinitely();

        assertEquals(IDENTIFICADOR, checklist.identificadorNegocial());
        assertEquals("Checklist no novo client", checklist.nome());
        assertNull(checklist.dataHoraCriacao());
        assertNull(checklist.orientacaoOperador());
        assertNull(checklist.apontamentos().getFirst());
        assertNull(checklist.apontamentos().get(1).nome());
        assertEquals(false, checklist.apontamentos().get(1).indicadorReanalise());

        List<ChecklistMtrStubTestResource.CapturedRequest> requisicoes =
                ChecklistMtrStubTestResource.requisicoes();
        assertEquals(1, requisicoes.size());
        var requisicao = requisicoes.getFirst();
        assertEquals("GET", requisicao.method());
        assertEquals(
                ChecklistMtrStubTestResource.CAMINHO_CHECKLIST
                        + IDENTIFICADOR + "/versao/" + VERSAO,
                requisicao.path()
        );
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
        SpanData adapter = span("mtr.parametrizacao.checklist.consultar");
        assertEquals(SpanKind.CLIENT, adapter.getKind());
        assertEquals("simtr-parametrizacao",
                adapter.getAttributes().get(AttributeKey.stringKey("mtr.servico")));
        assertEquals("cadastro-checklist-v1",
                adapter.getAttributes().get(AttributeKey.stringKey("mtr.api")));
        assertEquals("GET",
                adapter.getAttributes().get(AttributeKey.stringKey("http.request.method")));
        assertEquals(
                "/simtr-parametrizacao/v1/cadastro/checklist/identificador-negocial/"
                        + "{identificador}/versao/{versao}",
                adapter.getAttributes().get(AttributeKey.stringKey("url.path"))
        );
        assertEquals(IDENTIFICADOR, adapter.getAttributes().get(AttributeKey.longKey(
                "mtr.parametrizacao.checklist.identificador_negocial"
        )));
        assertEquals((long) VERSAO, adapter.getAttributes().get(AttributeKey.longKey(
                "mtr.parametrizacao.checklist.versao"
        )));
        assertEquals(true,
                adapter.getAttributes().get(AttributeKey.booleanKey("mtr.resposta.sucesso")));
        assertEquals("Checklist no novo client",
                adapter.getAttributes().get(AttributeKey.stringKey("checklist.nome")));
        assertEquals(2L, adapter.getAttributes().get(AttributeKey.longKey(
                "checklist.apontamentos.quantidade"
        )));

        var inicio = log("mtr.parametrizacao.checklist.chamada.iniciada");
        assertEquals("infrastructure", inicio.mdc().get("camada"));
        assertEquals("ParametrizacaoChecklistGateway", inicio.mdc().get("componente"));
        assertEquals("simtr-parametrizacao", inicio.mdc().get("dependencia"));
        assertEquals("consultar-checklist-parametrizacao-v1", inicio.mdc().get("operacao"));
        assertEquals(String.valueOf(IDENTIFICADOR), inicio.mdc().get("identificador_negocial"));
        assertEquals(String.valueOf(VERSAO), inicio.mdc().get("versao"));

        var provider = log("mtr.rest-client.request.enviada");
        assertEquals("ParametrizacaoChecklistClient", provider.mdc().get("rest_client"));
        assertEquals("consultarPorIdentificadorNegocialEVersao",
                provider.mdc().get("operacao"));
        assertEquals("GET", provider.mdc().get("http_method"));
        assertEquals(requisicao.path(), provider.mdc().get("url_path"));
    }

    @Test
    void traduzErroDeNegocioCompletoSemRetry() {
        ChecklistMtrStubTestResource.responder(404, """
                {
                  "codigo_http": 404,
                  "recurso": "simtr-parametrizacao",
                  "id_erro": "checklist-novo-404",
                  "codigo_erro": "MTR-CHECKLIST-404",
                  "erros": [{"mensagem": "checklist nao localizado"}],
                  "detalhe": "falha de negocio controlada"
                }
                """);

        var falha = assertThrows(
                FalhaConsultaChecklist.class,
                () -> portaSaida.obter(new ComandoConsultaChecklist(IDENTIFICADOR, VERSAO))
                        .await().indefinitely()
        );

        assertEquals(FalhaConsultaChecklist.Tipo.NEGOCIO, falha.tipo());
        assertEquals(404, falha.status());
        assertEquals("simtr-parametrizacao", falha.recurso());
        assertEquals("checklist-novo-404", falha.idErro());
        assertEquals("MTR-CHECKLIST-404", falha.codigoErro());
        assertEquals(List.of("checklist nao localizado"), falha.mensagens());
        assertEquals("falha de negocio controlada", falha.detalhe());
        assertEquals(1, ChecklistMtrStubTestResource.requisicoes().size());

        var logFalha = log("mtr.parametrizacao.checklist.chamada.falhou");
        assertEquals("erro", logFalha.mdc().get("resultado"));
        assertEquals("MtrBusinessErrorException", logFalha.mdc().get("erro_tipo"));

        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        SpanData adapter = span("mtr.parametrizacao.checklist.consultar");
        assertEquals(false,
                adapter.getAttributes().get(AttributeKey.booleanKey("mtr.resposta.sucesso")));
        assertEquals(
                "br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException",
                adapter.getAttributes().get(AttributeKey.stringKey("erro.tipo"))
        );
    }

    @Test
    void retryRecuperaErroServidorComDuasRequisicoesGetIdenticas() {
        ChecklistMtrStubTestResource.responder(
                500,
                "{\"codigo_http\":500,\"recurso\":\"simtr-parametrizacao\"}"
        );
        ChecklistMtrStubTestResource.responder(200, RESPOSTA_MTR);

        var checklist = portaSaida.obter(new ComandoConsultaChecklist(IDENTIFICADOR, VERSAO))
                .await().indefinitely();

        assertEquals(IDENTIFICADOR, checklist.identificadorNegocial());
        List<ChecklistMtrStubTestResource.CapturedRequest> requisicoes =
                ChecklistMtrStubTestResource.requisicoes();
        assertEquals(2, requisicoes.size());
        assertTrue(requisicoes.stream().allMatch(requisicao -> "GET".equals(requisicao.method())));
        assertTrue(requisicoes.stream().allMatch(requisicao -> requisicao.path().equals(
                ChecklistMtrStubTestResource.CAMINHO_CHECKLIST
                        + IDENTIFICADOR + "/versao/" + VERSAO
        )));
        assertTrue(requisicoes.stream().allMatch(requisicao -> requisicao.body().isEmpty()));
    }

    private SpanData span(String nome) {
        return spanExporter.getFinishedSpanItems().stream()
                .filter(span -> nome.equals(span.getName()))
                .findFirst()
                .orElseThrow();
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
