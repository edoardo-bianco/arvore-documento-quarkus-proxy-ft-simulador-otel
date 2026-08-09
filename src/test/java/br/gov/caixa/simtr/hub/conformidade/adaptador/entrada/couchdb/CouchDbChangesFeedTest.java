package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.couchdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.documento.ReferenciasDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ParecerConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoApontamentoConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CouchDbChangesFeedTest {

    private static final String CORRELATION_ID =
            "7aa3ca4d-3c7e-4f61-a3a1-996571d3397a";
    private static final String INSTANCE_ID = "01J3FLOWTESTE00000000000000";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloudEventMapper mapper = new CloudEventMapper(objectMapper);

    @Inject
    Tracer tracer;

    @Inject
    InMemorySpanExporter exporter;

    @Inject
    OpenTelemetry openTelemetry;

    @BeforeEach
    void limparSpans() {
        flush();
        exporter.reset();
    }

    @Test
    void avancaCursorAposDocumentoIgnoradoOuInvalidoEEntregaOValido() {
        var client = new ClientFalso(List.of(
                mudanca("1", objectMapper.createObjectNode()
                        .put("id", "projecao:1")
                        .put("tipo", "projecao-analise")),
                mudanca("2", documentoRevisao().put("hashConteudo", "0".repeat(64))),
                mudanca("3", documentoRevisao())));
        var feed = new CouchDbChangesFeed(client, mapper, tracer);
        var idsRecebidos = new ArrayList<String>();

        feed.processarUmaVez(evento -> idsRecebidos.add(evento.getId()))
                .await()
                .indefinitely();

        assertEquals(List.of("1", "2", "3"), client.cursoresSalvos);
        assertEquals(
                List.of("conformidade:" + documentoRevisao().path("id").asText()),
                idsRecebidos);
        assertEquals(3L, spansDaOperacao("salvar-cursor").size());
    }

    @Test
    void naoConfirmaCursorQuandoEntregaAoFlowFalha() {
        var client = new ClientFalso(List.of(mudanca("1", documentoRevisao())));
        var feed = new CouchDbChangesFeed(client, mapper);
        var falhaNaEntrega = feed.processarUmaVez(evento -> {
            throw new IllegalStateException("falha simulada");
        }).await();

        assertThrows(
                IllegalStateException.class,
                falhaNaEntrega::indefinitely);
        assertEquals(List.of(), client.cursoresSalvos);
    }

    @Test
    void registraFalhaAoCarregarCursorSemExporMensagem() {
        var falha = new IllegalStateException(
                "TEXTO_PROMPT_REVISAO_EVIDENCIA_CREDENCIAL_DOCUMENTO");
        CouchDbChangesClient client = new CouchDbChangesClient() {
            @Override
            public Uni<String> carregarCursor() {
                return Uni.createFrom().failure(falha);
            }

            @Override
            public Uni<List<CouchDbMudanca>> lerMudancas(String desde) {
                return Uni.createFrom().item(List.of());
            }

            @Override
            public Uni<Void> salvarCursor(String sequencia) {
                return Uni.createFrom().voidItem();
            }
        };
        var feed = new CouchDbChangesFeed(client, mapper, tracer);

        IllegalStateException observada = assertThrows(
                IllegalStateException.class,
                feed.processarUmaVez(evento -> {
                    // A carga do cursor falha antes de qualquer entrega.
                }).await()::indefinitely);

        assertSame(falha, observada);
        SpanData span = spansDaOperacao("carregar-cursor").getFirst();
        assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode());
        assertEquals("FALHA", atributo(span, "conformidade.feed.resultado"));
        assertFalse(span.toString().contains(falha.getMessage()));
    }

    private List<SpanData> spansDaOperacao(String operacao) {
        flush();
        return exporter.getFinishedSpanItems().stream()
                .filter(span -> "simtr-hub.feed.conformidade.documento"
                        .equals(span.getName()))
                .filter(span -> operacao.equals(atributo(
                        span,
                        "conformidade.feed.operacao")))
                .toList();
    }

    private void flush() {
        ((OpenTelemetrySdk) openTelemetry)
                .getSdkTracerProvider()
                .forceFlush()
                .join(10, TimeUnit.SECONDS);
    }

    private static String atributo(SpanData span, String chave) {
        return span.getAttributes().get(AttributeKey.stringKey(chave));
    }

    private CouchDbMudanca mudanca(
            String sequencia,
            com.fasterxml.jackson.databind.JsonNode documento) {
        return new CouchDbMudanca(sequencia, documento);
    }

    private com.fasterxml.jackson.databind.node.ObjectNode documentoRevisao() {
        var revisao = new RevisaoHumanaConformidade(
                "Aprovado pelo revisor",
                List.of(new ResultadoApontamentoConformidade(
                        10L,
                        "Documento identificado",
                        ParecerConformidade.CONFORME,
                        "Confirmado",
                        "Trecho",
                        0.9d)));
        var referencia = new ReferenciasDocumentoAnaliseConformidade(objectMapper)
                .revisao(CORRELATION_ID, revisao);
        return objectMapper.createObjectNode()
                .put("id", referencia.documentoRef())
                .put("tipo", "revisao-humana")
                .put("instanceId", INSTANCE_ID)
                .put("correlationId", CORRELATION_ID)
                .put("versaoSchema", referencia.versaoSchema())
                .put("hashConteudo", referencia.hashConteudo())
                .set("revisao", objectMapper.valueToTree(revisao));
    }

    private static final class ClientFalso implements CouchDbChangesClient {

        private final List<CouchDbMudanca> mudancas;
        private final List<String> cursoresSalvos = new ArrayList<>();

        private ClientFalso(List<CouchDbMudanca> mudancas) {
            this.mudancas = mudancas;
        }

        @Override
        public Uni<String> carregarCursor() {
            return Uni.createFrom().item("0");
        }

        @Override
        public Uni<List<CouchDbMudanca>> lerMudancas(String desde) {
            return Uni.createFrom().item(mudancas);
        }

        @Override
        public Uni<Void> salvarCursor(String sequencia) {
            cursoresSalvos.add(sequencia);
            return Uni.createFrom().voidItem();
        }
    }
}
