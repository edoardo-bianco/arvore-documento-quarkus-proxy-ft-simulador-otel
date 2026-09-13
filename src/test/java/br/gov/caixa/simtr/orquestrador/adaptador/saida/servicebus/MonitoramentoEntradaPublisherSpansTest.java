package br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.gov.caixa.simtr.orquestrador.dominio.modelo.TentativaMonitoramento;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonGenerationException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.time.Instant;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Mono;

/** Provas da telemetria emitida e do carrier; somente a fronteira SDK e controlada. */
@QuarkusTest
@TestProfile(OrquestradorEntradaLogTest.LogJsonProfile.class)
class MonitoramentoEntradaPublisherSpansTest {
    private static final String FILA = "q.prevalidacao.monitoramento-mtr.in";
    private static final TentativaMonitoramento TENTATIVA = new TentativaMonitoramento(
            "MON-1", "ORQ-1", "pre-restrita", "0007", 1,
            Instant.parse("2026-09-08T12:00:00Z"), Instant.parse("2026-09-09T12:00:00Z"), "v1");
    private static final Context PAI = Context.root().with(Span.wrap(SpanContext.create(
            "11111111111111111111111111111111", "aaaaaaaaaaaaaaaa",
            TraceFlags.getSampled(), TraceState.builder().put("teste", "origem").build())));
    private static final Context OUTRO = Context.root().with(Span.wrap(SpanContext.create(
            "22222222222222222222222222222222", "bbbbbbbbbbbbbbbb",
            TraceFlags.getDefault(), TraceState.getDefault())));
    @Inject Tracer tracer;
    @Inject OpenTelemetry openTelemetry;
    @Inject InMemorySpanExporter exporter;
    @Inject ObjectMapper json;
    private Scope raiz;
    private Span assinanteGravavel;
    private int primeiraLinha;
    private int logsEsperados;
    private static final Path LOG = Path.of("target/logs/orquestrador-entrada-erros-test.json");

    @BeforeEach
    void prepararCaptura() throws IOException {
        primeiraLinha = Files.readAllLines(LOG).size();
        logsEsperados = 1;
        raiz = Context.root().makeCurrent();
        spans();
        exporter.reset();
        var controle = tracer.spanBuilder("teste.publicacao.controle").setNoParent().startSpan();
        assertTrue(controle.isRecording());
        controle.end();
        assertEquals(1, spans().size());
        exporter.reset();
    }

    @AfterEach
    void restaurarContextoDoTeste() throws IOException {
        try {
            assertEquals(logsEsperados, registros().size(), "Uma operacao emite exatamente um evento terminal.");
        } finally {
            if (assinanteGravavel != null) {
                assinanteGravavel.end();
            }
            raiz.close();
        }
    }

    private List<JsonNode> registros() throws IOException {
        var linhas = Files.readAllLines(LOG);
        var capturados = new ArrayList<JsonNode>();
        for (var linha : linhas.subList(primeiraLinha, linhas.size())) {
            var registro = json.readTree(linha);
            if (registro.path("message").asText().startsWith("orquestrador.")) {
                capturados.add(registro);
            }
        }
        return capturados;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deveAbrirProducerAntesDoEnvioEPropagarSeuContextoAteOAck(boolean assinanteGrava) {
        if (assinanteGrava) {
            assinanteGravavel = tracer.spanBuilder("teste.assinante.gravavel").setNoParent().startSpan();
        }
        var contextoAssinante = assinanteGrava ? Context.root().with(assinanteGravavel) : OUTRO;
        var sender = mock(ServiceBusSenderAsyncClient.class);
        var confirmacao = new CompletableFuture<Void>();
        var contextos = new ArrayList<Span>();
        var mensagens = new ArrayList<ServiceBusMessage>();
        when(sender.sendMessage(any(ServiceBusMessage.class))).thenAnswer(chamada -> {
            contextos.add(Span.current());
            mensagens.add(chamada.getArgument(0));
            return Mono.defer(() -> {
                contextos.add(Span.current());
                return Mono.fromFuture(confirmacao);
            });
        });
        Uni<Void> publicacao;
        try (var _ = Baggage.builder().put("segredo", "SEGREDO_BAGGAGE").build().storeInContext(PAI).makeCurrent()) {
            publicacao = new MonitoramentoEntradaPublisher(referencia(sender), json, tracer, FILA).executar(TENTATIVA);
        }
        assertTrue(spans().isEmpty());
        verifyNoInteractions(sender);
        try (var _ = contextoAssinante.makeCurrent()) {
            var primeiro = publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create());
            var segundo = publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create());
            primeiro.assertNotTerminated();
            segundo.assertNotTerminated();
            assertTrue(spans().isEmpty(), "O span permanece aberto ate o ACK.");
            assertEquals(2, contextos.size());
            assertTrue(contextos.getFirst().isRecording());
            assertSame(contextos.getFirst(), contextos.getLast());
            assertEquals(Span.fromContext(contextoAssinante).getSpanContext(), Span.current().getSpanContext());
            assertTrue(confirmacao.complete(null));
            primeiro.assertCompleted().assertItem(null);
            segundo.assertCompleted().assertItem(null);
            publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).assertCompleted();
            assertEquals(Span.fromContext(contextoAssinante).getSpanContext(), Span.current().getSpanContext());
        }
        assertEquals(SpanContext.getInvalid(), Span.current().getSpanContext());
        var span = verificarSpan(PAI, null);
        assertEquals(Map.of("traceparent", "00-" + span.getTraceId() + "-" + span.getSpanId() + "-01",
                "tracestate", "teste=origem"), mensagens.getFirst().getApplicationProperties());
        verify(sender).sendMessage(any(ServiceBusMessage.class));
    }


    @ParameterizedTest
    @CsvSource({"0,false", "1,false", "2,false", "0,true", "2,true"})
    void deveEncerrarSomenteAposTerminoEmOutraThreadEMemorizar(int cancelados, boolean falhar) throws Exception {
        var sender = mock(ServiceBusSenderAsyncClient.class);
        var confirmacao = new CompletableFuture<Void>();
        var corrente = new AtomicReference<Span>();
        when(sender.sendMessage(any(ServiceBusMessage.class))).thenAnswer(_ -> {
            corrente.set(Span.current());
            return Mono.fromFuture(confirmacao);
        });
        Uni<Void> publicacao;
        try (var _ = PAI.makeCurrent()) {
            publicacao = new MonitoramentoEntradaPublisher(referencia(sender), json, tracer, FILA).executar(TENTATIVA);
        }
        var primeiro = publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create());
        var segundo = publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create());
        if (cancelados > 0) {
            primeiro.cancel();
        }
        if (cancelados == 2) {
            segundo.cancel();
        }
        assertTrue(corrente.get().isRecording());
        assertTrue(spans().isEmpty());
        assertFalse(confirmacao.isDone());
        assertEquals(SpanContext.getInvalid(), Span.current().getSpanContext());
        var original = new IllegalStateException("SEGREDO_BROKER", new RuntimeException("SEGREDO_CAUSA"));
        original.addSuppressed(new RuntimeException("SEGREDO_SUPPRESSED"));
        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                try (var _ = OUTRO.makeCurrent()) {
                    if (falhar) {
                        assertTrue(confirmacao.completeExceptionally(original));
                    } else {
                        assertTrue(confirmacao.complete(null));
                    }
                    assertEquals(Span.fromContext(OUTRO).getSpanContext(), Span.current().getSpanContext());
                }
                assertEquals(SpanContext.getInvalid(), Span.current().getSpanContext());
            }).get(3, TimeUnit.SECONDS);
        }
        assertFalse(corrente.get().isRecording());
        var tardio = publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create());
        if (falhar) {
            var falha = tardio.assertFailedWith(IllegalStateException.class).getFailure();
            assertEquals("Falha ao publicar tentativa de monitoramento.", falha.getMessage());
            assertNull(falha.getCause());
            assertEquals(0, falha.getSuppressed().length);
            assertSame(falha, publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).getFailure());
            if (cancelados == 0) {
                assertSame(falha, primeiro.getFailure());
                assertSame(falha, segundo.getFailure());
            }
        } else {
            tardio.assertCompleted().assertItem(null);
            if (cancelados == 0) {
                primeiro.assertCompleted();
            }
            if (cancelados < 2) {
                segundo.assertCompleted();
            }
        }
        verificarSpan(PAI, falhar ? "FALHA_PUBLICACAO" : null);
        verify(sender).sendMessage(any(ServiceBusMessage.class));
        assertEquals(SpanContext.getInvalid(), Span.current().getSpanContext());
    }

    @ParameterizedTest
    @ValueSource(strings = {"sender", "sincrona", "assinatura", "cancelamento"})
    void deveSanitizarFalhasDoSdkSemExporThrowable(String etapa) {
        var sender = mock(ServiceBusSenderAsyncClient.class);
        var instancia = referencia(sender);
        var original = new IllegalArgumentException("SEGREDO_SDK", new RuntimeException("SEGREDO_CAUSA"));
        original.addSuppressed(new RuntimeException("SEGREDO_SUPPRESSED"));
        var confirmacao = new CompletableFuture<Void>();
        switch (etapa) {
            case "sender" -> when(instancia.get()).thenThrow(original);
            case "sincrona" -> when(sender.sendMessage(any(ServiceBusMessage.class))).thenThrow(original);
            case "assinatura" -> when(sender.sendMessage(any(ServiceBusMessage.class)))
                    .thenReturn(Mono.defer(() -> { throw original; }));
            case "cancelamento" -> when(sender.sendMessage(any(ServiceBusMessage.class)))
                    .thenReturn(Mono.fromFuture(confirmacao));
            default -> throw new AssertionError(etapa);
        }
        var publicacao = new MonitoramentoEntradaPublisher(instancia, json, tracer, FILA).executar(TENTATIVA);
        var assinante = publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create());
        if ("cancelamento".equals(etapa)) {
            assertTrue(spans().isEmpty());
            assertTrue(confirmacao.cancel(true));
        }
        var falha = assinante.assertFailedWith(IllegalStateException.class).getFailure();
        assertEquals("Falha ao publicar tentativa de monitoramento.", falha.getMessage());
        assertNull(falha.getCause());
        assertEquals(0, falha.getSuppressed().length);
        assertSame(falha, publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).getFailure());
        verificarSpan(Context.root(), "FALHA_PUBLICACAO");
        verify(instancia).get();
        assertEquals(SpanContext.getInvalid(), Span.current().getSpanContext());
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void deveInstrumentarPreparacaoEPreservarSeuErroELog(boolean serializacao) throws Exception {
        var sender = mock(ServiceBusSenderAsyncClient.class);
        var instancia = referencia(sender);
        var serializer = mock(ObjectMapper.class);
        var original = serializacao
                ? new JsonGenerationException("SEGREDO_SERIALIZACAO", new IllegalStateException("SEGREDO_CAUSA"), null)
                : new IllegalStateException("SEGREDO_PREPARACAO");
        original.addSuppressed(new IllegalStateException("SEGREDO_SUPPRESSED"));
        var corrente = new AtomicReference<Span>();
        when(serializer.writeValueAsString(any())).thenAnswer(_ -> {
            corrente.set(Span.current());
            assertTrue(corrente.get().isRecording(), "PRODUCER deve iniciar antes da serializacao.");
            throw original;
        });
        Uni<Void> publicacao;
        try (var _ = PAI.makeCurrent()) {
            publicacao = new MonitoramentoEntradaPublisher(instancia, serializer, tracer, FILA).executar(TENTATIVA);
        }
        Throwable falha;
        try (var _ = OUTRO.makeCurrent()) {
            falha = publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).getFailure();
            assertNotNull(falha);
            assertSame(falha, publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).getFailure());
            assertEquals(Span.fromContext(OUTRO).getSpanContext(), Span.current().getSpanContext());
        }
        var span = verificarSpan(PAI, serializacao ? "SERIALIZACAO_ENTRADA" : "FALHA_PREPARACAO_PUBLICACAO");
        assertEquals(corrente.get().getSpanContext(), span.getSpanContext());
        assertFalse(corrente.get().isRecording());
        verifyNoInteractions(instancia, sender);
        verify(serializer).writeValueAsString(any());
        if (serializacao) {
            var tipada = assertInstanceOf(SerializacaoEntradaException.class, falha);
            assertNull(tipada.getCause());
            assertEquals(0, tipada.getSuppressed().length);
            var logs = registros();
            assertEquals(1, logs.size());
            var registro = logs.getFirst();
            assertEquals("orquestrador.servicebus.entrada.falhou", registro.path("evento").asText());
            assertEquals("ORQUESTRADOR_ENTRADA_SERIALIZACAO_FALHOU", tipada.codigoErro());
            assertEquals(tipada.codigoErro(), registro.path("codigo_erro").asText());
            assertEquals(tipada.idErro(), registro.path("id_erro").asText());
            assertEquals(span.getTraceId(), registro.path("traceId").asText());
            assertEquals(span.getSpanId(), registro.path("spanId").asText());
            assertEquals("ERROR", registro.path("level").asText());
            assertFalse(registro.has("exception"));
            assertFalse(registro.toString().contains("SEGREDO_"));
        } else {
            assertSame(original, falha);
            assertEquals("orquestrador.monitoramento-dossie.publicacao.falhou", registros().getFirst().path("evento").asText());
        }
        assertEquals(SpanContext.getInvalid(), Span.current().getSpanContext());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void devePropagarProducerValidoNaoGravavelSemDependerDeRecording(boolean comTracestate) {
        var sender = mock(ServiceBusSenderAsyncClient.class);
        var enviado = new AtomicReference<ServiceBusMessage>();
        var corrente = new AtomicReference<Span>();
        when(sender.sendMessage(any(ServiceBusMessage.class))).thenAnswer(chamada -> {
            enviado.set(chamada.getArgument(0));
            corrente.set(Span.current());
            return Mono.empty();
        });
        var pai = comTracestate ? PAI : OUTRO;
        try (var provider = SdkTracerProvider.builder().setSampler(Sampler.alwaysOff()).build()) {
            var semGravacao = provider.get("teste.publicacao.nao-gravavel");
            Uni<Void> publicacao;
            try (var _ = Baggage.builder().put("segredo", "SEGREDO_BAGGAGE").build().storeInContext(pai).makeCurrent()) {
                publicacao = new MonitoramentoEntradaPublisher(referencia(sender), json, semGravacao, FILA)
                        .executar(TENTATIVA);
            }
            publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).assertCompleted();
            var contexto = corrente.get().getSpanContext();
            assertTrue(contexto.isValid());
            assertFalse(corrente.get().isRecording());
            assertFalse(contexto.isSampled());
            assertEquals(Span.fromContext(pai).getSpanContext().getTraceId(), contexto.getTraceId());
            assertNotEquals(Span.fromContext(pai).getSpanContext().getSpanId(), contexto.getSpanId());
            var esperado = new java.util.HashMap<String, Object>();
            esperado.put("traceparent", "00-" + contexto.getTraceId() + "-" + contexto.getSpanId() + "-00");
            if (comTracestate) {
                esperado.put("tracestate", "teste=origem");
            }
            assertEquals(esperado, enviado.get().getApplicationProperties());
            assertTrue(spans().isEmpty());
            assertEquals(SpanContext.getInvalid(), Span.current().getSpanContext());
        }
    }

    @Test
    void contextoInvalidoNaoDeveInventarCarrier() {
        var sender = mock(ServiceBusSenderAsyncClient.class);
        var enviada = org.mockito.ArgumentCaptor.forClass(ServiceBusMessage.class);
        when(sender.sendMessage(any(ServiceBusMessage.class))).thenAnswer(_ -> {
            assertEquals(SpanContext.getInvalid(), Span.current().getSpanContext());
            return Mono.empty();
        });
        var publicacao = new MonitoramentoEntradaPublisher(referencia(sender), json,
                OpenTelemetry.noop().getTracer("teste.invalido"), FILA).executar(TENTATIVA);
        try (var _ = PAI.makeCurrent()) {
            publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).assertCompleted();
            assertEquals(Span.fromContext(PAI).getSpanContext(), Span.current().getSpanContext());
        }
        verify(sender).sendMessage(enviada.capture());
        assertTrue(enviada.getValue().getApplicationProperties().isEmpty());
        assertTrue(spans().isEmpty());
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void deveUsarFlagsDoSamplerAtualEOmitirTracestateVazio(boolean comPai) {
        var sender = mock(ServiceBusSenderAsyncClient.class);
        var enviada = org.mockito.ArgumentCaptor.forClass(ServiceBusMessage.class);
        when(sender.sendMessage(any(ServiceBusMessage.class))).thenReturn(Mono.empty());
        var pai = comPai ? OUTRO : Context.root();
        Uni<Void> publicacao;
        try (var _ = pai.makeCurrent()) {
            publicacao = new MonitoramentoEntradaPublisher(referencia(sender), json, tracer, FILA).executar(TENTATIVA);
        }
        try (var _ = PAI.makeCurrent()) {
            publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).assertCompleted();
            assertEquals(Span.fromContext(PAI).getSpanContext(), Span.current().getSpanContext());
        }
        var span = verificarSpan(pai, null);
        assertTrue(span.getSpanContext().isSampled(), "O sampler CDI continua always_on, inclusive com parent 00.");
        verify(sender).sendMessage(enviada.capture());
        assertEquals(Map.of("traceparent", "00-" + span.getTraceId() + "-" + span.getSpanId() + "-01"),
                enviada.getValue().getApplicationProperties());
    }

    @Test
    void deveIsolarParentsCarriersEConfirmacoesDeInvocacoesIntercaladas() throws IOException {
        logsEsperados = 2;
        var sender = mock(ServiceBusSenderAsyncClient.class);
        var primeiraConfirmacao = new CompletableFuture<Void>();
        var segundaConfirmacao = new CompletableFuture<Void>();
        when(sender.sendMessage(any(ServiceBusMessage.class)))
                .thenReturn(Mono.fromFuture(primeiraConfirmacao), Mono.fromFuture(segundaConfirmacao));
        var publisher = new MonitoramentoEntradaPublisher(referencia(sender), json, tracer, FILA);
        Uni<Void> primeira;
        Uni<Void> segunda;
        try (var _ = PAI.makeCurrent()) {
            primeira = publisher.executar(TENTATIVA);
        }
        try (var _ = OUTRO.makeCurrent()) {
            segunda = publisher.executar(TENTATIVA);
        }
        var primeiro = primeira.subscribe().withSubscriber(UniAssertSubscriber.<Void>create());
        var segundo = segunda.subscribe().withSubscriber(UniAssertSubscriber.<Void>create());
        assertTrue(spans().isEmpty());
        assertTrue(segundaConfirmacao.complete(null));
        segundo.assertCompleted();
        primeiro.assertNotTerminated();
        verificarSpan(OUTRO, null);
        assertTrue(primeiraConfirmacao.complete(null));
        primeiro.assertCompleted();
        var capturados = spans();
        var logs = registros();
        assertEquals(2, logs.size());
        assertEquals(2, capturados.size());
        assertEquals(Span.fromContext(PAI).getSpanContext(), capturados.getLast().getParentSpanContext());
        assertNotEquals(capturados.getFirst().getTraceId(), capturados.getLast().getTraceId());
        assertNotEquals(capturados.getFirst().getSpanId(), capturados.getLast().getSpanId());
        var enviadas = org.mockito.ArgumentCaptor.forClass(ServiceBusMessage.class);
        verify(sender, times(2)).sendMessage(enviadas.capture());
        for (int indice = 0; indice < 2; indice++) {
            var span = capturados.get(1 - indice);
            assertEquals(span.getTraceId(), logs.get(1 - indice).path("traceId").asText());
            assertEquals(span.getSpanId(), logs.get(1 - indice).path("spanId").asText());
            var carrier = enviadas.getAllValues().get(indice).getApplicationProperties();
            var esperado = new java.util.HashMap<String, Object>();
            esperado.put("traceparent", "00-" + span.getTraceId() + "-" + span.getSpanId() + "-01");
            if (indice == 0) {
                esperado.put("tracestate", "teste=origem");
            }
            assertEquals(esperado, carrier);
        }
        primeira.subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).assertCompleted();
        segunda.subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).assertCompleted();
        assertEquals(2, spans().size());
        assertEquals(SpanContext.getInvalid(), Span.current().getSpanContext());
    }

    private SpanData verificarSpan(Context pai, String erro) {
        var capturados = spans();
        assertEquals(1, capturados.size());
        var span = capturados.getFirst();
        assertEquals("send " + FILA, span.getName());
        assertEquals(SpanKind.PRODUCER, span.getKind());
        assertEquals(Span.fromContext(pai).getSpanContext(), span.getParentSpanContext());
        assertEquals(erro == null ? StatusCode.UNSET : StatusCode.ERROR, span.getStatus().getStatusCode());
        assertEquals("", span.getStatus().getDescription());
        assertTrue(span.getEvents().isEmpty());
        assertTrue(span.getLinks().isEmpty());
        var atributos = Attributes.builder().put("messaging.system", "servicebus")
                .put("messaging.destination.name", FILA).put("messaging.operation.name", "send")
                .put("messaging.operation.type", "send");
        if (erro != null) {
            atributos.put("error.type", erro);
        }
        assertEquals(atributos.build().asMap(), span.getAttributes().asMap());
        return span;
    }

    private List<SpanData> spans() {
        var flush = ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider().forceFlush();
        flush.join(10, TimeUnit.SECONDS);
        assertTrue(flush.isSuccess());
        return exporter.getFinishedSpanItems();
    }

    @SuppressWarnings("unchecked")
    private static Instance<ServiceBusSenderAsyncClient> referencia(ServiceBusSenderAsyncClient sender) {
        Instance<ServiceBusSenderAsyncClient> referencia = mock(Instance.class);
        when(referencia.get()).thenReturn(sender);
        return referencia;
    }
}
