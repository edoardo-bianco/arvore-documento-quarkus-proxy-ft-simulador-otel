package br.gov.caixa.simtr.orquestrador.integracao;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.FilaEntrada;
import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.FilaSaida;
import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.ServiceBusEmuladorTestProfile;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.time.Instant;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentelemetry.api.common.AttributeKey;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@QuarkusTest
@Tag("servicebus-integration")
@Execution(ExecutionMode.SAME_THREAD)
@TestProfile(MonitoramentoInicioTelemetriaEmuladorTest.InicioProfile.class)
class MonitoramentoInicioTelemetriaEmuladorTest {
    private static final Duration ESPERA = Duration.ofSeconds(40);
    private static final String CAMINHO = "/simtr-hub/v1/monitoramentos-dossie";
    private static final String EVENTO = "orquestrador.monitoramento-dossie.publicacao.confirmada";
    private static final String PAI = "2222222222222222";
    private static final Path LOG = Path.of("target/logs/telemetria-inicio.json");
    private static final Path INVENTARIO = Path.of("target/provas-10-1-b1-5/inicio.json");
    @Inject Tracer tracer;
    @Inject OpenTelemetry openTelemetry;
    @Inject InMemorySpanExporter exporter;
    @Inject @ConfigProperty(name = "monitoramento.service-bus.input-queue") String fila;
    @Inject @FilaEntrada ServiceBusReceiverAsyncClient entrada;
    @Inject @FilaSaida ServiceBusReceiverAsyncClient saida;
    @Inject ObjectMapper json;

    @Test
    void deveConfirmarPostComMensagemPropriaNoBroker() throws Exception {
        salvarInventario(json.createObjectNode().put("estado", "INICIADO"));
        exigirFilaVazia(entrada);
        exigirFilaVazia(saida);
        var preValidacao = "SEGREDO_PRE_" + UUID.randomUUID();
        var trace = UUID.randomUUID().toString().replace("-", "");
        validarCaptura();
        int primeiraLinha = Files.readAllLines(LOG).size();
        var antesPost = Instant.now();
        var resposta = given().contentType(ContentType.JSON)
                .header("traceparent", "00-" + trace + "-" + PAI + "-01")
                .header("tracestate", "teste=origem")
                .header("baggage", "sentinela=SEGREDO_BAGGAGE")
                .body(Map.of("idDossiePreValidacao", preValidacao, "idDossieMtr", "0007"))
                .when().post(CAMINHO).then().statusCode(202).contentType(ContentType.JSON)
                .extract().asString();
        var depoisPost = Instant.now();
        var ids = json.readTree(resposta);
        assertEquals(Set.of("monitoramentoId", "orquestracaoId"), chaves(ids));
        var monitoramento = texto(ids, "monitoramentoId");
        var orquestracao = texto(ids, "orquestracaoId");
        assertEquals(monitoramento, UUID.fromString(monitoramento).toString());
        assertEquals(orquestracao, UUID.fromString(orquestracao).toString());
        var observadas = entrada.peekMessages(10, 0L).collectList().block(ESPERA);
        assertNotNull(observadas);
        assertEquals(1, observadas.size());
        var observada = observadas.getFirst();
        conferirIdentidade(observada, monitoramento, orquestracao);
        var recebida = entrada.receiveMessages().concatMap(mensagem -> {
            conferirIdentidade(mensagem, monitoramento, orquestracao);
            conferirMesmaMensagem(observada, mensagem);
            // O peek usa o receive link associado: conclui antes de next() cancelar esse link.
            return entrada.complete(mensagem).then(verificarFilaVazia(entrada))
                    .then(verificarFilaVazia(saida)).thenReturn(mensagem);
        }, 0).next().block(ESPERA);
        assertNotNull(recebida);
        var spans = capturarSpans();
        var logs = capturarLogs(primeiraLinha);
        var inventario = inventariar(spans, logs, recebida);
        verificarContrato(recebida, preValidacao, monitoramento, orquestracao, antesPost, depoisPost);
        var produtor = verificarSpans(spans, trace);
        assertEquals(Map.of("traceparent", "00-" + produtor.getTraceId() + "-" + produtor.getSpanId() + "-01",
                "tracestate", "teste=origem"), recebida.getApplicationProperties());
        verificarLogs(logs, produtor, monitoramento, orquestracao);
        salvarInventario(inventario.put("estado", "VALIDADO"));
    }

    private static void conferirMesmaMensagem(ServiceBusReceivedMessage observada, ServiceBusReceivedMessage recebida) {
        assertEquals(observada.getSequenceNumber(), recebida.getSequenceNumber());
        assertEquals(observada.getBody().toString(), recebida.getBody().toString());
        assertEquals(observada.getSubject(), recebida.getSubject());
        assertEquals(observada.getContentType(), recebida.getContentType());
        assertEquals(observada.getApplicationProperties(), recebida.getApplicationProperties());
    }

    private void verificarContrato(ServiceBusReceivedMessage mensagem, String preValidacao, String monitoramento,
            String orquestracao, Instant antesPost, Instant depoisPost) throws IOException {
        assertEquals("MONITORAR_DOSSIE_MTR", mensagem.getSubject());
        assertEquals("application/json", mensagem.getContentType());
        var corpo = json.readTree(mensagem.getBody().toString());
        assertEquals(Set.of("schemaVersion", "monitoramentoId", "orquestracaoId", "idDossiePreValidacao",
                "idDossieMtr", "tentativaAtual", "iniciadoEm", "limiteEm", "politicaMonitoramentoVersao"), chaves(corpo));
        assertTrue(corpo.path("schemaVersion").isInt());
        assertEquals(1, corpo.path("schemaVersion").intValue());
        assertEquals(monitoramento, texto(corpo, "monitoramentoId"));
        assertEquals(orquestracao, texto(corpo, "orquestracaoId"));
        assertEquals(preValidacao, texto(corpo, "idDossiePreValidacao"));
        assertEquals("0007", texto(corpo, "idDossieMtr"));
        assertTrue(corpo.path("tentativaAtual").isInt());
        assertEquals(1, corpo.path("tentativaAtual").intValue());
        assertEquals("v1", texto(corpo, "politicaMonitoramentoVersao"));
        var iniciado = Instant.parse(texto(corpo, "iniciadoEm"));
        var limite = Instant.parse(texto(corpo, "limiteEm"));
        assertFalse(iniciado.isBefore(antesPost));
        assertFalse(iniciado.isAfter(depoisPost));
        assertEquals(Duration.ofHours(24), Duration.between(iniciado, limite));
    }

    private static String texto(JsonNode objeto, String campo) {
        assertTrue(objeto.path(campo).isTextual(), () -> "Campo deve ser string: " + campo);
        return objeto.path(campo).textValue();
    }

    private static Set<String> chaves(JsonNode objeto) {
        assertTrue(objeto.isObject());
        var nomes = new HashSet<String>();
        objeto.fieldNames().forEachRemaining(nomes::add);
        return Set.copyOf(nomes);
    }

    private void validarCaptura() {
        capturarSpans();
        exporter.reset();
        var controle = tracer.spanBuilder("teste.inicio.controle").setNoParent().startSpan();
        assertTrue(controle.isRecording());
        controle.end();
        var capturados = capturarSpans();
        assertEquals(1, capturados.size(), "Captura CDI exige controle positivo exportado.");
        assertEquals(controle.getSpanContext(), capturados.getFirst().getSpanContext());
        exporter.reset();
    }

    private List<SpanData> capturarSpans() {
        var flush = ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider().forceFlush();
        flush.join(10, TimeUnit.SECONDS);
        assertTrue(flush.isSuccess());
        return List.copyOf(exporter.getFinishedSpanItems());
    }

    private List<JsonNode> capturarLogs(int primeiraLinha) throws IOException {
        var linhas = Files.readAllLines(LOG);
        assertTrue(linhas.size() >= primeiraLinha);
        var registros = new ArrayList<JsonNode>();
        for (var linha : linhas.subList(primeiraLinha, linhas.size())) {
            registros.add(json.readTree(linha));
        }
        return List.copyOf(registros);
    }

    private SpanData verificarSpans(List<SpanData> spans, String trace) {
        assertEquals(3, spans.size(), "Inventario completo inclui receive/Complete/peeks; nenhum span oculto.");
        var http = unico(spans, SpanKind.SERVER, "simtr-hub.api.monitoramento-dossie.iniciar");
        var inicio = unico(spans, SpanKind.INTERNAL, "orquestrador.service.monitoramento-dossie.iniciar");
        var produtor = unico(spans, SpanKind.PRODUCER, "send " + fila);
        assertEquals(trace, http.getParentSpanContext().getTraceId());
        assertEquals(PAI, http.getParentSpanId());
        assertEquals(CAMINHO, http.getAttributes().get(AttributeKey.stringKey("http.route")));
        assertEquals(CAMINHO, http.getAttributes().get(AttributeKey.stringKey("url.path")));
        assertEquals("POST", http.getAttributes().get(AttributeKey.stringKey("http.request.method")));
        assertEquals(202L, http.getAttributes().get(AttributeKey.longKey("http.response.status_code")));
        assertTrue(http.getParentSpanContext().isRemote());
        assertEquals(http.getSpanContext(), inicio.getParentSpanContext());
        assertEquals(inicio.getSpanContext(), produtor.getParentSpanContext());
        assertEquals(3, Set.of(http.getSpanId(), inicio.getSpanId(), produtor.getSpanId()).size());
        for (var span : spans) {
            assertEquals(trace, span.getTraceId());
            assertTrue(span.getSpanContext().isSampled());
            assertFalse(span.getName().contains("SEGREDO"));
            assertEquals(StatusCode.UNSET, span.getStatus().getStatusCode());
            assertTrue(span.getStatus().getDescription().isEmpty());
            assertTrue(span.getEvents().isEmpty());
            assertTrue(span.getLinks().isEmpty());
            assertEquals(TraceState.builder().put("teste", "origem").build(), span.getSpanContext().getTraceState());
            assertTrue(span.getStartEpochNanos() <= span.getEndEpochNanos());
            assertFalse(span.getAttributes().toString().contains("SEGREDO"));
        }
        assertTrue(http.getStartEpochNanos() <= inicio.getStartEpochNanos());
        assertTrue(inicio.getStartEpochNanos() <= produtor.getStartEpochNanos());
        assertTrue(produtor.getEndEpochNanos() <= inicio.getEndEpochNanos());
        assertTrue(inicio.getEndEpochNanos() <= http.getEndEpochNanos());
        assertTrue(inicio.getAttributes().isEmpty());
        assertEquals(Attributes.builder().put("messaging.system", "servicebus")
                .put("messaging.destination.name", fila).put("messaging.operation.name", "send")
                .put("messaging.operation.type", "send").build().asMap(), produtor.getAttributes().asMap());
        return produtor;
    }

    private static SpanData unico(List<SpanData> spans, SpanKind kind, String nome) {
        var selecionados = spans.stream().filter(span -> span.getKind() == kind).toList();
        assertEquals(1, selecionados.size());
        assertEquals(nome, selecionados.getFirst().getName());
        return selecionados.getFirst();
    }

    private static void verificarLogs(List<JsonNode> logs, SpanData produtor, String monitoramento, String orquestracao) {
        var negocio = logs.stream().filter(log -> eventoDoFluxo(log.path("message").asText())
                || eventoDoFluxo(log.path("evento").asText())).toList();
        assertEquals(1, negocio.size(), "Somente a confirmacao inicial, sem consumo ou segundo evento.");
        var log = negocio.getFirst();
        assertEquals(Set.of("timestamp", "sequence", "loggerClassName", "loggerName", "level", "message",
                "threadName", "threadId", "mdc", "ndc", "hostName", "processName", "processId", "service.name",
                "evento", "camada", "componente", "operacao", "monitoramento_id", "orquestracao_id",
                "tentativa_atual", "message_id", "traceId", "spanId"), chaves(log));
        assertEquals(EVENTO, texto(log, "message"));
        assertEquals(EVENTO, texto(log, "evento"));
        assertEquals("INFO", texto(log, "level"));
        assertEquals("adaptador", texto(log, "camada"));
        assertEquals("MonitoramentoEntradaPublisher", texto(log, "componente"));
        assertEquals("publicar", texto(log, "operacao"));
        assertEquals("br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus.MonitoramentoEntradaPublisher",
                texto(log, "loggerName"));
        assertEquals(produtor.getTraceId(), texto(log, "traceId"));
        assertEquals(produtor.getSpanId(), texto(log, "spanId"));
        assertEquals(monitoramento, texto(log, "monitoramento_id"));
        assertEquals(orquestracao, texto(log, "orquestracao_id"));
        assertEquals(monitoramento + ":tentativa:1", texto(log, "message_id"));
        assertTrue(log.path("tentativa_atual").isInt());
        assertEquals(1, log.path("tentativa_atual").intValue());
        assertTrue(log.path("mdc").isObject());
        assertTrue(log.path("mdc").isEmpty());
        assertEquals("", texto(log, "ndc"));
        assertFalse(logs.toString().contains("SEGREDO"));
    }

    private static boolean eventoDoFluxo(String evento) {
        return evento.startsWith("orquestrador.") || evento.startsWith("monitoramento.")
                || evento.startsWith("doctree.") || evento.startsWith("simtr-hub.");
    }

    private ObjectNode inventariar(List<SpanData> spans, List<JsonNode> logs, ServiceBusReceivedMessage mensagem)
            throws IOException {
        var inventario = json.createObjectNode().put("estado", "CAPTURADO").put("limpeza_concluida", true);
        var spansJson = inventario.putArray("spans");
        for (var span : spans) {
            var item = spansJson.addObject().put("nome", span.getName()).put("kind", span.getKind().name())
                    .put("trace_id", span.getTraceId()).put("span_id", span.getSpanId())
                    .put("parent_id", span.getParentSpanId()).put("status", span.getStatus().getStatusCode().name());
            var atributos = item.putArray("atributos_chaves");
            span.getAttributes().asMap().keySet().forEach(chave -> atributos.add(chave.getKey()));
            item.put("eventos", span.getEvents().size()).put("links", span.getLinks().size());
        }
        var logsJson = inventario.putArray("logs");
        for (var log : logs) {
            var item = logsJson.addObject().put("logger", log.path("loggerName").asText())
                    .put("confirmacao", EVENTO.equals(log.path("message").asText()));
            var chaves = item.putArray("chaves");
            log.fieldNames().forEachRemaining(chaves::add);
        }
        inventario.put("sequencia_recebida", mensagem.getSequenceNumber());
        inventario.set("carrier_chaves", json.valueToTree(mensagem.getApplicationProperties().keySet()));
        inventario.put("quantidade_spans", spans.size()).put("quantidade_logs", logs.size());
        salvarInventario(inventario);
        return inventario;
    }

    private void salvarInventario(ObjectNode inventario) throws IOException {
        Files.createDirectories(INVENTARIO.getParent());
        Files.writeString(INVENTARIO, json.writerWithDefaultPrettyPrinter().writeValueAsString(inventario));
    }

    private static void conferirIdentidade(ServiceBusReceivedMessage mensagem, String monitoramento, String orquestracao) {
        assertEquals(monitoramento + ":tentativa:1", mensagem.getMessageId());
        assertEquals(orquestracao, mensagem.getCorrelationId());
    }

    private static void exigirFilaVazia(ServiceBusReceiverAsyncClient receiver) {
        verificarFilaVazia(receiver).block(ESPERA);
    }

    private static Mono<Void> verificarFilaVazia(ServiceBusReceiverAsyncClient receiver) {
        return Mono.defer(() -> receiver.peekMessages(1, 0L).collectList())
                .doOnNext(mensagens -> assertTrue(mensagens.isEmpty(), "Nao descartar mensagem alheia."))
                .then();
    }

    public static class InicioProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return new ServiceBusEmuladorTestProfile().getConfigProfile();
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            var config = new HashMap<>(new ServiceBusEmuladorTestProfile().getConfigOverrides());
            config.put("monitoramento.service-bus.entrada.consumo-habilitado", "false");
            config.put("monitoramento.service-bus.saida.consumo-habilitado", "false");
            config.put("quarkus.log.file.enabled", "true");
            config.put("quarkus.log.file.path", LOG.toString());
            config.put("quarkus.log.file.json.enabled", "true");
            config.put("quarkus.log.console.json.enabled", "true");
            return Map.copyOf(config);
        }
    }
}
