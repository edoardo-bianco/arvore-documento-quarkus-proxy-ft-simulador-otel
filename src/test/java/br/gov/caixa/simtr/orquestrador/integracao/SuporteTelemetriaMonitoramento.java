package br.gov.caixa.simtr.orquestrador.integracao;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.FilaEntrada;
import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.FilaSaida;
import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.ServiceBusEmuladorTestProfile;
import br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.RegistrarResultadoMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.ResultadoMonitoramento;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Captura do estado atual: nao propaga contexto nem altera o processamento da aplicacao. */
abstract class SuporteTelemetriaMonitoramento {
    protected static final Duration ESPERA = Duration.ofSeconds(45);
    protected static final String EVENTO_FINAL = "orquestrador.monitoramento-dossie.resultado.registrado";
    protected static final String EVENTO_SETTLEMENT = "doctree.monitoramento-mtr.settlement.executado";
    protected static final String SPAN_HUB = "simtr-hub.service.dossie-produto.consultar";
    private static final String CAMINHO = "/simtr-hub/v1/monitoramentos-dossie";
    private static final String PAI_REMOTO = "2222222222222222";
    private static final String EVENTO_PUBLICACAO = "orquestrador.monitoramento-dossie.publicacao.confirmada";
    private static final String TRACE_ID = "trace_id";
    private static final String SPAN_ID = "span_id";

    @Inject @FilaEntrada ServiceBusReceiverAsyncClient entrada;
    @Inject @FilaSaida ServiceBusReceiverAsyncClient saida;
    @Inject ObjectMapper json;
    @Inject Tracer tracer;
    @Inject OpenTelemetry openTelemetry;
    @Inject InMemorySpanExporter exporter;
    @Inject @ConfigProperty(name = "monitoramento.service-bus.input-queue") String filaEntrada;
    @InjectSpy RegistrarResultadoMonitoramento registrar;

    protected String tracePost;
    protected String monitoramentoId;
    protected String orquestracaoId;
    protected final List<MensagemObservada> observadas = new ArrayList<>();
    private int primeiraLinha;

    protected abstract String cenario();

    private Path arquivoLog() {
        return Path.of("target/logs/telemetria-" + cenario() + ".json");
    }

    private Path arquivoInventario() {
        return Path.of("target/provas-10-1-a2", cenario() + ".json");
    }

    @BeforeEach
    void validarCapturaEIsolamento() throws IOException {
        Files.deleteIfExists(arquivoInventario());
        assertFilaVazia(entrada);
        assertFilaVazia(saida);
        primeiraLinha = linhasCompletas().size();
        tracePost = UUID.randomUUID().toString().replace("-", "");
        observadas.clear();
        flush();
        exporter.reset();
        var controle = tracer.spanBuilder("teste.monitoramento.telemetria.controle")
                .setNoParent().startSpan();
        assertTrue(controle.isRecording());
        controle.end();
        flush();
        assertEquals(1, exporter.getFinishedSpanItems().size(), "Captura CDI exige controle positivo exportado.");
        assertEquals(controle.getSpanContext(), exporter.getFinishedSpanItems().getFirst().getSpanContext());
        exporter.reset();
    }

    protected void iniciarPost(String idDossie) {
        var resposta = given().contentType(ContentType.JSON)
                .header("traceparent", "00-" + tracePost + "-" + PAI_REMOTO + "-01")
                .body(Map.of("idDossiePreValidacao", "pre-em-analise", "idDossieMtr", idDossie))
                .when().post(CAMINHO).then().statusCode(202).extract().jsonPath();
        monitoramentoId = resposta.getString("monitoramentoId");
        orquestracaoId = resposta.getString("orquestracaoId");
        assertNotNull(monitoramentoId);
        assertNotNull(orquestracaoId);
    }

    protected void observarEntrada() throws IOException {
        var mensagens = entrada.peekMessages(10, 0L).collectList().block(ESPERA);
        assertNotNull(mensagens);
        for (var mensagem : mensagens) {
            registrarMensagem(mensagem);
        }
    }

    private void registrarMensagem(ServiceBusReceivedMessage mensagem) throws IOException {
        var corpo = json.readTree(mensagem.getBody().toString());
        int tentativa = corpo.path("tentativaAtual").asInt();
        assertEquals(monitoramentoId + ":tentativa:" + tentativa, mensagem.getMessageId());
        assertEquals(orquestracaoId, mensagem.getCorrelationId());
        if (tentativa == 1) {
            var produtor = producer(spans());
            assertEquals(Map.of("traceparent", "00-" + produtor.getTraceId() + "-" + produtor.getSpanId()
                    + "-" + produtor.getSpanContext().getTraceFlags().asHex()), mensagem.getApplicationProperties());
        } else {
            assertTrue(mensagem.getApplicationProperties().isEmpty(), "Reagendamentos continuam sem carrier ate B4.");
        }
        var observada = new MensagemObservada(mensagem.getSequenceNumber(), tentativa,
                mensagem.getState().toString(), Set.copyOf(mensagem.getApplicationProperties().keySet()),
                (String) mensagem.getApplicationProperties().get("traceparent"));
        if (!observadas.contains(observada)) {
            observadas.add(observada);
        }
    }

    protected void aguardarFluxo(int tentativas) {
        var concluido = Mono.fromCallable(() -> {
            observarEntrada();
            var logs = registros();
            return contar(logs, EVENTO_FINAL) == 1 && contar(logs, EVENTO_SETTLEMENT) == tentativas
                    && Boolean.FALSE.equals(entrada.peekMessages(1, 0L).hasElements().block(ESPERA))
                    && Boolean.FALSE.equals(saida.peekMessages(1, 0L).hasElements().block(ESPERA));
        }).subscribeOn(Schedulers.boundedElastic())
                .repeatWhen(repeticoes -> repeticoes.delayElements(Duration.ofMillis(100)))
                .filter(Boolean::booleanValue).next().block(ESPERA);
        assertEquals(Boolean.TRUE, concluido);
        assertFilaVazia(entrada);
        assertFilaVazia(saida);
    }

    protected List<SpanData> spans() {
        flush();
        return exporter.getFinishedSpanItems();
    }

    protected SpanData verificarCadeiaInicial(List<SpanData> spans, int consultasHub) {
        assertEquals(3 + consultasHub, spans.size(), "B1.3 exige HTTP, iniciacao e PRODUCER, alem das consultas Hub.");
        assertEquals(consultasHub, spans.stream().filter(span -> SPAN_HUB.equals(span.getName())).count());
        var servidores = spans.stream().filter(span -> span.getKind() == SpanKind.SERVER).toList();
        assertEquals(1, servidores.size(), "Um unico span HTTP automatico deve existir.");
        var http = servidores.getFirst();
        assertEquals(tracePost, http.getTraceId());
        assertEquals(PAI_REMOTO, http.getParentSpanId());
        assertTrue(http.getParentSpanContext().isRemote());
        assertEquals("simtr-hub.api.monitoramento-dossie.iniciar", http.getName());
        assertTrue(http.getLinks().isEmpty());
        var iniciacoes = spans.stream()
                .filter(span -> "orquestrador.service.monitoramento-dossie.iniciar".equals(span.getName())).toList();
        assertEquals(1, iniciacoes.size());
        var iniciacao = iniciacoes.getFirst();
        assertEquals(SpanKind.INTERNAL, iniciacao.getKind());
        assertEquals(http.getSpanContext(), iniciacao.getParentSpanContext());
        assertEquals(http.getTraceId(), iniciacao.getTraceId());
        assertEquals(StatusCode.UNSET, iniciacao.getStatus().getStatusCode());
        assertTrue(iniciacao.getStatus().getDescription().isEmpty());
        assertTrue(iniciacao.getAttributes().isEmpty());
        assertTrue(iniciacao.getEvents().isEmpty());
        assertTrue(iniciacao.getLinks().isEmpty());
        assertTrue(iniciacao.getStartEpochNanos() >= http.getStartEpochNanos());
        assertTrue(iniciacao.getEndEpochNanos() <= http.getEndEpochNanos());
        var envio = producer(spans);
        assertEquals(iniciacao.getSpanContext(), envio.getParentSpanContext());
        assertEquals(iniciacao.getTraceId(), envio.getTraceId());
        assertEquals(StatusCode.UNSET, envio.getStatus().getStatusCode());
        assertTrue(envio.getStatus().getDescription().isEmpty());
        assertTrue(envio.getEvents().isEmpty());
        assertTrue(envio.getLinks().isEmpty());
        assertEquals(Attributes.builder().put("messaging.system", "servicebus")
                .put("messaging.destination.name", filaEntrada).put("messaging.operation.name", "send")
                .put("messaging.operation.type", "send").build().asMap(), envio.getAttributes().asMap());
        assertTrue(envio.getStartEpochNanos() >= iniciacao.getStartEpochNanos());
        assertTrue(envio.getEndEpochNanos() <= iniciacao.getEndEpochNanos());
        return http;
    }

    private SpanData producer(List<SpanData> spans) {
        var produtores = spans.stream().filter(span -> span.getKind() == SpanKind.PRODUCER).toList();
        assertEquals(1, produtores.size());
        assertEquals("send " + filaEntrada, produtores.getFirst().getName());
        return produtores.getFirst();
    }

    protected void verificarLogsPublicacaoInicial(List<JsonNode> logs, int demaisRegistros) {
        assertEquals(1, contar(logs, EVENTO_PUBLICACAO));
        assertEquals(0, contar(logs, "orquestrador.monitoramento-dossie.publicacao.falhou"));
        assertEquals(demaisRegistros + 1, logs.size(), "B1.4 acrescenta exatamente o evento de confirmacao.");
        var registro = logs.stream().filter(log -> EVENTO_PUBLICACAO.equals(evento(log))).findFirst().orElseThrow();
        var produtor = producer(spans());
        assertEquals(produtor.getTraceId(), registro.path("traceId").asText());
        assertEquals(produtor.getSpanId(), registro.path("spanId").asText());
        assertEquals("INFO", registro.path("level").asText());
        assertEquals(EVENTO_PUBLICACAO, registro.path("evento").asText());
        assertEquals("adaptador", registro.path("camada").asText());
        assertEquals("MonitoramentoEntradaPublisher", registro.path("componente").asText());
        assertEquals("publicar", registro.path("operacao").asText());
        assertEquals(monitoramentoId, registro.path("monitoramento_id").asText());
        assertEquals(orquestracaoId, registro.path("orquestracao_id").asText());
        assertEquals(monitoramentoId + ":tentativa:1", registro.path("message_id").asText());
        assertTrue(registro.path("tentativa_atual").isIntegralNumber());
        assertEquals(1, registro.path("tentativa_atual").asInt());
        assertTrue(registro.path("mdc").isEmpty());
        assertFalse(registro.has("error_type"));
        assertFalse(registro.has("exception"));
        assertFalse(registro.has("stacktrace"));
    }

    protected ResultadoMonitoramento verificarResultado(String tipo, String motivo, int tentativas) {
        var captura = ArgumentCaptor.forClass(ResultadoMonitoramento.class);
        verify(registrar).executar(captura.capture());
        var resultado = captura.getValue();
        assertEquals(monitoramentoId, resultado.monitoramentoId());
        assertEquals(orquestracaoId, resultado.orquestracaoId());
        assertEquals(tipo, resultado.resultadoMonitoramento());
        assertEquals(motivo, resultado.motivo());
        assertEquals(tentativas, resultado.tentativasRealizadas());
        return resultado;
    }

    protected void verificarLogsListener(SpanData http, int reagendamentos) throws IOException {
        var logs = registros();
        var decisoes = logs.stream()
                .filter(log -> "doctree.monitoramento-mtr.decisao.tomada".equals(evento(log))).toList();
        assertEquals(reagendamentos, decisoes.size());
        for (var decisao : decisoes) {
            assertEquals("REAGENDAR", decisao.path("decisao").asText());
            verificarSemContexto(decisao);
        }
        var settlements = logs.stream().filter(log -> EVENTO_SETTLEMENT.equals(evento(log))).toList();
        assertEquals(reagendamentos + 1, settlements.size());
        for (int i = 0; i < settlements.size(); i++) {
            var log = settlements.get(i);
            assertEquals(i < reagendamentos ? "complete_transacional" : "complete", log.path("settlement").asText());
            assertEquals(http.getTraceId(), traceLog(log));
            assertEquals(http.getSpanId(), log.path("mdc").path("spanId").asText());
        }
    }

    protected static void verificarSemContexto(JsonNode log) {
        assertEquals("", traceLog(log));
        assertEquals("", log.path("mdc").path("spanId").asText());
    }

    protected JsonNode verificarLogFinal() throws IOException {
        var finais = registros().stream().filter(log -> EVENTO_FINAL.equals(evento(log))).toList();
        assertEquals(1, finais.size());
        var registro = finais.getFirst();
        assertEquals(monitoramentoId, registro.path("mdc").path("monitoramento_id").asText());
        assertEquals(orquestracaoId, registro.path("mdc").path("orquestracao_id").asText());
        assertEquals("INFO", registro.path("level").asText());
        assertFalse(registro.has("exception"));
        assertFalse(registro.has("stacktrace"));
        return registro;
    }

    protected List<JsonNode> registros() throws IOException {
        var linhas = linhasCompletas();
        assertTrue(linhas.size() >= primeiraLinha, "Arquivo de captura nao pode ser truncado durante o cenario.");
        var registros = new ArrayList<JsonNode>();
        for (String linha : linhas.subList(primeiraLinha, linhas.size())) {
            var registro = json.readTree(linha);
            var nome = evento(registro);
            if (nome.startsWith("doctree.") || nome.startsWith("orquestrador.")
                    || nome.startsWith("simtr-hub.dossie-produto.")) {
                registros.add(registro);
            }
        }
        return registros;
    }

    protected static long contar(List<JsonNode> registros, String nome) {
        return registros.stream().filter(log -> nome.equals(evento(log))).count();
    }

    protected static String evento(JsonNode log) {
        return log.path("message").asText();
    }

    protected static String traceLog(JsonNode log) {
        return EVENTO_PUBLICACAO.equals(evento(log))
                ? log.path("traceId").asText() : log.path("mdc").path("traceId").asText();
    }

    private List<String> linhasCompletas() throws IOException {
        var conteudo = Files.readString(arquivoLog());
        return conteudo.substring(0, conteudo.lastIndexOf('\n') + 1).lines().toList();
    }

    private static void assertFilaVazia(ServiceBusReceiverAsyncClient receiver) {
        assertEquals(Boolean.FALSE, receiver.peekMessages(1, 0L).hasElements().block(ESPERA),
                "Nao descartar residuos de outros cenarios.");
    }

    private void flush() {
        var resultado = ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider().forceFlush();
        resultado.join(10, TimeUnit.SECONDS);
        assertTrue(resultado.isSuccess(), "Flush deve concluir com sucesso.");
    }

    protected void registrarInventarioSanitizado() throws IOException {
        var inventario = json.createObjectNode();
        inventario.put("estado", "VALIDADO").put("cenario", cenario())
                .put("trace_post", tracePost).put("parent_remoto", PAI_REMOTO);
        var spansJson = inventario.putArray("spans");
        for (var span : spans()) {
            var item = spansJson.addObject();
            item.put("nome", span.getName()).put("kind", span.getKind().name())
                    .put("scope", span.getInstrumentationScopeInfo().getName())
                    .put("scope_versao", span.getInstrumentationScopeInfo().getVersion())
                    .put(TRACE_ID, span.getTraceId()).put(SPAN_ID, span.getSpanId())
                    .put("parent_id", span.getParentSpanId()).put("parent_valido", span.getParentSpanContext().isValid())
                    .put("status", span.getStatus().getStatusCode().name())
                    .put("status_descricao_presente", !span.getStatus().getDescription().isEmpty());
            var atributos = item.putArray("atributos_chaves");
            span.getAttributes().asMap().keySet().forEach(chave -> atributos.add(chave.getKey()));
            var resource = item.putArray("resource_chaves");
            span.getResource().getAttributes().asMap().keySet().forEach(chave -> resource.add(chave.getKey()));
            var links = item.putArray("links");
            span.getLinks().forEach(link -> links.addObject()
                    .put(TRACE_ID, link.getSpanContext().getTraceId()).put(SPAN_ID, link.getSpanContext().getSpanId()));
            var eventos = item.putArray("eventos_nomes");
            span.getEvents().forEach(evento -> eventos.add(evento.getName()));
        }
        var logs = inventario.putArray("logs_negocio_e_hub");
        for (var log : registros()) {
            var item = logs.addObject();
            item.put("evento", evento(log)).put("logger", log.path("loggerName").asText())
                    .put(TRACE_ID, traceLog(log)).put(SPAN_ID, EVENTO_PUBLICACAO.equals(evento(log))
                            ? log.path("spanId").asText() : log.path("mdc").path("spanId").asText())
                    .put("settlement", log.path("settlement").asText())
                    .put("decisao", log.path("decisao").asText());
        }
        inventario.set("mensagens_observadas", json.valueToTree(observadas));
        var destino = arquivoInventario();
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, json.writerWithDefaultPrettyPrinter().writeValueAsString(inventario));
    }

    protected record MensagemObservada(long sequencia, int tentativa, String estado,
            Set<String> carrierChaves, String traceparent) { }

    public abstract static class Perfil implements QuarkusTestProfile {
        protected abstract String cenario();

        @Override
        public String getConfigProfile() {
            return new ServiceBusEmuladorTestProfile().getConfigProfile();
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            var config = new HashMap<>(new ServiceBusEmuladorTestProfile().getConfigOverrides());
            config.put("monitoramento.service-bus.entrada.consumo-habilitado", "true");
            config.put("monitoramento.service-bus.saida.consumo-habilitado", "true");
            config.put("monitoramento.simulador.prevalidacao.habilitado", "true");
            config.put("simtr-hub.simulador.dossie-produto.habilitado", "true");
            config.put("monitoramento.politicas.definicoes.padrao.intervalos", "PT2S");
            config.put("monitoramento.politicas.definicoes.padrao.duracao-maxima", "PT1M");
            config.put("monitoramento.politicas.definicoes.padrao.max-tentativas", "3");
            config.put("quarkus.log.file.enabled", "true");
            config.put("quarkus.log.file.path", "target/logs/telemetria-" + cenario() + ".json");
            config.put("quarkus.log.file.json.enabled", "true");
            config.put("quarkus.log.console.json.enabled", "true");
            return Map.copyOf(config);
        }
    }
}
