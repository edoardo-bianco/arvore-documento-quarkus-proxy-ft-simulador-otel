package br.gov.caixa.simtr.hub.gestaodocumento.caracterizacao;

import br.gov.caixa.simtr.hub.gestaodocumento.aplicacao.porta.saida.SolicitarCredencialContainer;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.erro.FalhaObtencaoCredencialContainer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.common.QuarkusTestResource;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(
        value = GestaoDocumentoMtrStubTestResource.class,
        restrictToAnnotatedClass = true
)
class GestaoDocumentoNovoClientQuarkusTest {

    private static final String NOME_CONTAINER = "container-novo-client";
    private static final String SERVICO_MTR = "simtr-gestao-documento";
    private static final String RESPOSTA_MTR = """
            {
              "sas": "sv=novo-client&sp=rw&sig=valor-opaco",
              "validade": {
                "expira_em": "31/12/2099 23:59:47",
                "origem": "contrato-mtr"
              },
              "url_storage": "https://novo-client.blob.core.windows.net",
              "nome_container": "container-novo-client"
            }
            """;

    @Inject
    SolicitarCredencialContainer portaSaida;

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
        GestaoDocumentoMtrStubTestResource.reset();
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
    void percorreNovaPortaAteOStubPreservandoWireValidadeHeadersETelemetria() {
        GestaoDocumentoMtrStubTestResource.responder(200, RESPOSTA_MTR);

        var credencial = portaSaida.obter().await().indefinitely();

        assertEquals("sv=novo-client&sp=rw&sig=valor-opaco", credencial.sas());
        assertEquals(
                "31/12/2099 23:59:47",
                ((Map<?, ?>) credencial.validade()).get("expira_em")
        );
        assertEquals(
                "https://novo-client.blob.core.windows.net",
                credencial.urlStorage()
        );
        assertEquals(NOME_CONTAINER, credencial.nomeContainer());

        List<GestaoDocumentoMtrStubTestResource.CapturedRequest> requisicoes =
                GestaoDocumentoMtrStubTestResource.requisicoes();
        assertEquals(1, requisicoes.size());
        var requisicao = requisicoes.getFirst();
        assertEquals("POST", requisicao.method());
        assertEquals(GestaoDocumentoMtrStubTestResource.CAMINHO_CREDENCIAL, requisicao.path());
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
        SpanData adapter = span("mtr.gestao-documento.credencial-container.gerar");
        assertEquals(SpanKind.CLIENT, adapter.getKind());
        assertEquals(SERVICO_MTR,
                adapter.getAttributes().get(AttributeKey.stringKey("mtr.servico")));
        assertEquals("gestao-documento-v1",
                adapter.getAttributes().get(AttributeKey.stringKey("mtr.api")));
        assertEquals("POST",
                adapter.getAttributes().get(AttributeKey.stringKey("http.request.method")));
        assertEquals(GestaoDocumentoMtrStubTestResource.CAMINHO_CREDENCIAL,
                adapter.getAttributes().get(AttributeKey.stringKey("url.path")));
        assertEquals(true,
                adapter.getAttributes().get(AttributeKey.booleanKey("mtr.resposta.sucesso")));
        assertEquals(NOME_CONTAINER, adapter.getAttributes().get(
                AttributeKey.stringKey("gestao_documento.container.nome")
        ));

        var inicio = log("mtr.gestao-documento.credencial-container.chamada.iniciada");
        assertEquals("infrastructure", inicio.mdc().get("camada"));
        assertEquals("GestaoDocumentoGateway", inicio.mdc().get("componente"));
        assertEquals(SERVICO_MTR, inicio.mdc().get("dependencia"));
        assertEquals("gerar-credencial-container-v1", inicio.mdc().get("operacao"));

        assertEquals(
                "mtr.rest-client.request.enviada",
                log("mtr.rest-client.request.enviada").evento()
        );
    }

    @Test
    void traduzErroDeNegocioCompletoSemRetry() {
        GestaoDocumentoMtrStubTestResource.responder(404, """
                {
                  "codigo_http": 404,
                  "recurso": "simtr-gestao-documento",
                  "id_erro": "credencial-novo-404",
                  "codigo_erro": "MTR-CREDENCIAL-404",
                  "erros": [{"mensagem": "container nao localizado"}],
                  "detalhe": "falha de negocio controlada"
                }
                """);

        var falha = assertThrows(
                FalhaObtencaoCredencialContainer.class,
                () -> portaSaida.obter().await().indefinitely()
        );

        assertEquals(FalhaObtencaoCredencialContainer.Tipo.NEGOCIO, falha.tipo());
        assertEquals(404, falha.status());
        assertEquals(SERVICO_MTR, falha.recurso());
        assertEquals("credencial-novo-404", falha.idErro());
        assertEquals("MTR-CREDENCIAL-404", falha.codigoErro());
        assertEquals(List.of("container nao localizado"), falha.mensagens());
        assertEquals("falha de negocio controlada", falha.detalhe());
        assertEquals(1, GestaoDocumentoMtrStubTestResource.requisicoes().size());

        var logFalha = log("mtr.gestao-documento.credencial-container.chamada.falhou");
        assertEquals("erro", logFalha.mdc().get("resultado"));
        assertEquals("MtrBusinessErrorException", logFalha.mdc().get("erro_tipo"));

        ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider()
                .forceFlush().join(10, TimeUnit.SECONDS);
        SpanData adapter = span("mtr.gestao-documento.credencial-container.gerar");
        assertEquals(false,
                adapter.getAttributes().get(AttributeKey.booleanKey("mtr.resposta.sucesso")));
        assertEquals(
                "br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException",
                adapter.getAttributes().get(AttributeKey.stringKey("erro.tipo"))
        );
    }

    @Test
    void retryRecuperaErroServidorComDuasRequisicoesPostIdenticas() {
        GestaoDocumentoMtrStubTestResource.responder(
                500,
                "{\"codigo_http\":500,\"recurso\":\"simtr-gestao-documento\"}"
        );
        GestaoDocumentoMtrStubTestResource.responder(200, RESPOSTA_MTR);

        var credencial = portaSaida.obter().await().indefinitely();

        assertEquals(NOME_CONTAINER, credencial.nomeContainer());
        List<GestaoDocumentoMtrStubTestResource.CapturedRequest> requisicoes =
                GestaoDocumentoMtrStubTestResource.requisicoes();
        assertEquals(2, requisicoes.size());
        assertTrue(requisicoes.stream().allMatch(requisicao ->
                "POST".equals(requisicao.method())));
        assertTrue(requisicoes.stream().allMatch(requisicao ->
                GestaoDocumentoMtrStubTestResource.CAMINHO_CREDENCIAL.equals(
                        requisicao.path()
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
