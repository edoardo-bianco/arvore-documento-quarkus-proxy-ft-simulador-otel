package br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.gov.caixa.simtr.orquestrador.dominio.modelo.TentativaMonitoramento;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import com.fasterxml.jackson.core.JsonGenerationException;
import java.util.concurrent.Executors;
import org.jboss.logmanager.MDC;
import org.jboss.logmanager.NDC;
import reactor.core.publisher.Mono;

@QuarkusTest
@TestProfile(OrquestradorEntradaLogTest.LogJsonProfile.class)
class LogPublicacaoEntradaTest {
    private static final String CONFIRMADA = "orquestrador.monitoramento-dossie.publicacao.confirmada";
    private static final String FALHOU = "orquestrador.monitoramento-dossie.publicacao.falhou";
    private static final String MON = "aa111111-1111-4111-8111-111111111111";
    private static final String ORQ = "bb222222-2222-4222-8222-222222222222";
    private static final String FILA = "q.prevalidacao.monitoramento-mtr.in";
    private static final Path ARQUIVO = Path.of("target/logs/orquestrador-entrada-erros-test.json");
    @Inject ObjectMapper json;
    @Inject Tracer tracer;
    @Inject OpenTelemetry openTelemetry;
    @Inject InMemorySpanExporter exporter;
    private int primeiraLinha;
    private Scope raiz;

    @BeforeEach
    void preparar() throws IOException {
        primeiraLinha = Files.readAllLines(ARQUIVO).size();
        raiz = Context.root().makeCurrent();
        spans();
        exporter.reset();
        var controle = tracer.spanBuilder("teste.log.publicacao.controle").setNoParent().startSpan();
        assertTrue(controle.isRecording());
        controle.end();
        assertEquals(1, spans().size());
        exporter.reset();
    }

    @AfterEach
    void restaurar() {
        raiz.close();
    }

    @Test
    void deveRegistrarSomenteDepoisDoAckUmaVezComParDoProducer() throws IOException {
        var sender = mock(ServiceBusSenderAsyncClient.class);
        var ack = new CompletableFuture<Void>();
        when(sender.sendMessage(any(ServiceBusMessage.class))).thenReturn(Mono.fromFuture(ack));
        var publicacao = new MonitoramentoEntradaPublisher(referencia(sender), json, tracer, FILA)
                .executar(tentativa(MON, ORQ, 1));
        verifyNoInteractions(sender);
        assertTrue(registros().isEmpty());
        var primeiro = publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create());
        var segundo = publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create());
        primeiro.assertNotTerminated();
        assertTrue(registros().isEmpty());
        assertTrue(spans().isEmpty());
        assertTrue(ack.complete(null));
        primeiro.assertCompleted();
        segundo.assertCompleted();
        publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).assertCompleted();
        verify(sender).sendMessage(any(ServiceBusMessage.class));
        assertEquals(1, registros().size());
        verificarRegistro(registros().getFirst(), CONFIRMADA, null);
        assertEquals(1, spans().size());
        assertEquals(StatusCode.UNSET, spans().getFirst().getStatus().getStatusCode());
        assertEquals(spans().getFirst().getTraceId(), registros().getFirst().path("traceId").asText());
        assertEquals(spans().getFirst().getSpanId(), registros().getFirst().path("spanId").asText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"sender", "sincrona", "assincrona"})
    void deveRegistrarUmaFalhaSeguraDoSdk(String etapa) throws IOException {
        var sender = mock(ServiceBusSenderAsyncClient.class);
        var instancia = referencia(sender);
        var original = new IllegalStateException("SEGREDO_SDK", new RuntimeException("SEGREDO_CAUSA"));
        original.addSuppressed(new RuntimeException("SEGREDO_SUPPRESSED"));
        switch (etapa) {
            case "sender" -> when(instancia.get()).thenThrow(original);
            case "sincrona" -> when(sender.sendMessage(any(ServiceBusMessage.class))).thenThrow(original);
            case "assincrona" -> when(sender.sendMessage(any(ServiceBusMessage.class))).thenReturn(Mono.error(original));
            default -> throw new AssertionError(etapa);
        }
        var publicacao = new MonitoramentoEntradaPublisher(instancia, json, tracer, FILA)
                .executar(tentativa(MON, ORQ, 1));
        var falha = publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create())
                .assertFailedWith(IllegalStateException.class).getFailure();
        assertEquals("Falha ao publicar tentativa de monitoramento.", falha.getMessage());
        assertNull(falha.getCause());
        assertEquals(0, falha.getSuppressed().length);
        assertSame(falha, publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).getFailure());
        assertEquals(1, registros().size());
        verificarRegistro(registros().getFirst(), FALHOU, "FALHA_PUBLICACAO");
        verificarCorrelacao(registros().getFirst(), spans().getFirst());
        verify(instancia).get();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void devePreservarFalhaDePreparacaoESerializacaoSemDuplicar(boolean serializacao) throws Exception {
        var sender = mock(ServiceBusSenderAsyncClient.class);
        var serializer = mock(ObjectMapper.class);
        var original = serializacao
                ? new JsonGenerationException("SEGREDO_SERIAL", (com.fasterxml.jackson.core.JsonGenerator) null)
                : new IllegalArgumentException("SEGREDO_PREPARACAO");
        when(serializer.writeValueAsString(any())).thenThrow(original);
        var publicacao = new MonitoramentoEntradaPublisher(referencia(sender), serializer, tracer, FILA)
                .executar(tentativa(MON, ORQ, 1));
        var falha = publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).getFailure();
        assertSame(falha, publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).getFailure());
        verifyNoInteractions(sender);
        assertEquals(1, registros().size());
        var registro = registros().getFirst();
        if (serializacao) {
            assertInstanceOf(SerializacaoEntradaException.class, falha);
            assertEquals("orquestrador.servicebus.entrada.falhou", registro.path("evento").asText());
        } else {
            assertSame(original, falha);
            verificarRegistro(registro, FALHOU, "FALHA_PREPARACAO_PUBLICACAO");
        }
        verificarCorrelacao(registro, spans().getFirst());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void falhaPropagadaDoHelperNaoDeveAlterarAckNemErroSeguro(boolean falhar) throws IOException {
        var sender = mock(ServiceBusSenderAsyncClient.class);
        when(sender.sendMessage(any(ServiceBusMessage.class))).thenReturn(falhar
                ? Mono.error(new IllegalStateException("SEGREDO_SDK")) : Mono.empty());
        var log = mock(LogPublicacaoEntrada.class);
        doAnswer(_ -> {
            assertTrue(Span.current().isRecording(), "Log acontece antes do termino do PRODUCER.");
            throw new IllegalStateException("SEGREDO_LOG");
        }).when(log).registrar(any(), any(), nullable(String.class));
        var publicacao = new MonitoramentoEntradaPublisher(referencia(sender), json, tracer, FILA, log)
                .executar(tentativa(MON, ORQ, 1));
        var assinante = publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create());
        if (falhar) {
            var erro = assinante.assertFailedWith(IllegalStateException.class).getFailure();
            assertEquals("Falha ao publicar tentativa de monitoramento.", erro.getMessage());
            assertNull(erro.getCause());
            assertEquals(0, erro.getSuppressed().length);
            assertSame(erro, publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).getFailure());
        } else {
            assinante.assertCompleted();
            publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).assertCompleted();
        }
        verify(sender).sendMessage(any(ServiceBusMessage.class));
        verify(log).registrar(any(), any(), nullable(String.class));
        assertTrue(registros().isEmpty());
        assertEquals(1, spans().size());
        assertEquals(falhar ? StatusCode.ERROR : StatusCode.UNSET, spans().getFirst().getStatus().getStatusCode());
        assertTrue(spans().getFirst().getEvents().isEmpty());
        assertFalse(spans().getFirst().getAttributes().toString().contains("SEGREDO"));
    }

    @Test
    void errorDoHelperNaoDeveSerAbsorvidoNemDeixarSpanOuUniPendentes() {
        var sender = mock(ServiceBusSenderAsyncClient.class);
        when(sender.sendMessage(any(ServiceBusMessage.class))).thenReturn(Mono.empty());
        var log = mock(LogPublicacaoEntrada.class);
        var original = new AssertionError("Falha simulada do logger");
        doThrow(original).when(log).registrar(any(), any(), nullable(String.class));
        var publicacao = new MonitoramentoEntradaPublisher(referencia(sender), json, tracer, FILA, log)
                .executar(tentativa(MON, ORQ, 1));
        var falha = publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create())
                .assertFailedWith(java.util.concurrent.CompletionException.class).getFailure();
        assertSame(original, falha.getCause());
        assertSame(falha, publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).getFailure());
        verify(sender).sendMessage(any(ServiceBusMessage.class));
        assertEquals(1, spans().size());
        assertEquals(StatusCode.UNSET, spans().getFirst().getStatus().getStatusCode());
    }

    @ParameterizedTest
    @CsvSource({"handler,false", "handler,true", "filtro,false", "filtro,true"})
    void defeitoRealDeLoggingNaoDeveAlterarPublicacao(String ponto, boolean falhar) throws IOException {
        var logger = org.jboss.logmanager.Logger.getLogger(MonitoramentoEntradaPublisher.class.getName());
        var erros = new java.util.concurrent.atomic.AtomicInteger();
        var invocacoes = new java.util.concurrent.atomic.AtomicInteger();
        var handler = new org.jboss.logmanager.ExtHandler() {
            @Override
            protected void doPublish(org.jboss.logmanager.ExtLogRecord registro) {
                invocacoes.incrementAndGet();
                throw new IllegalStateException("Falha simulada do handler");
            }
        };
        handler.setLevel(org.jboss.logmanager.Level.ALL);
        handler.setErrorManager(new java.util.logging.ErrorManager() {
            @Override
            public void error(String mensagem, Exception falha, int codigo) {
                erros.incrementAndGet();
            }
        });
        handler.setFilter(registro -> {
            if (!MonitoramentoEntradaPublisher.class.getName().equals(registro.getLoggerName())
                    || !(CONFIRMADA.equals(registro.getMessage()) || FALHOU.equals(registro.getMessage()))
                    || !(registro instanceof org.jboss.logmanager.ExtLogRecord detalhado)
                    || !(detalhado.getMarker() instanceof br.gov.caixa.simtr.arquitetura.infraestrutura.observabilidade.CamposLogJson campos)
                    || !MON.equals(campos.campos().getString("monitoramento_id", ""))) {
                return false;
            }
            if ("filtro".equals(ponto)) {
                invocacoes.incrementAndGet();
                throw new IllegalStateException("Falha simulada do filtro");
            }
            return true;
        });
        logger.addHandler(handler);
        try {
            var sender = mock(ServiceBusSenderAsyncClient.class);
            when(sender.sendMessage(any(ServiceBusMessage.class))).thenReturn(falhar
                    ? Mono.error(new IllegalArgumentException("SEGREDO_SDK")) : Mono.empty());
            var publicacao = new MonitoramentoEntradaPublisher(referencia(sender), json, tracer, FILA)
                    .executar(tentativa(MON, ORQ, 1));
            var assinante = publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create());
            if (falhar) {
                var falha = assinante.assertFailedWith(IllegalStateException.class).getFailure();
                assertEquals("Falha ao publicar tentativa de monitoramento.", falha.getMessage());
                assertNull(falha.getCause());
                assertSame(falha, publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).getFailure());
            } else {
                assinante.assertCompleted();
                publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).assertCompleted();
            }
            verify(sender).sendMessage(any(ServiceBusMessage.class));
            assertEquals(1, invocacoes.get());
            assertEquals(1, erros.get());
            assertEquals(1, registros().size(), "O handler pai continua escrevendo o JSON real.");
            verificarRegistro(registros().getFirst(), falhar ? FALHOU : CONFIRMADA, falhar ? "FALHA_PUBLICACAO" : null);
            assertEquals(1, spans().size());
            assertEquals(falhar ? StatusCode.ERROR : StatusCode.UNSET, spans().getFirst().getStatus().getStatusCode());
        } finally {
            logger.removeHandler(handler);
            handler.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deveRegistrarComContextoProprioAposCancelamentoEmOutraThreadSemCopiarMdc(boolean falhar) throws Exception {
        var sender = mock(ServiceBusSenderAsyncClient.class);
        var ack = new CompletableFuture<Void>();
        when(sender.sendMessage(any(ServiceBusMessage.class))).thenReturn(Mono.fromFuture(ack));
        var pai = Context.root().with(Span.wrap(SpanContext.create(
                "11111111111111111111111111111111", "aaaaaaaaaaaaaaaa",
                TraceFlags.getSampled(), TraceState.getDefault())));
        io.smallrye.mutiny.Uni<Void> publicacao;
        MDC.put("sentinela", "SEGREDO_MDC_INVOCACAO");
        try (var _ = Baggage.builder().put("segredo", "SEGREDO_BAGGAGE").build().storeInContext(pai).makeCurrent()) {
            publicacao = new MonitoramentoEntradaPublisher(referencia(sender), json, tracer, FILA)
                    .executar(tentativa(MON, ORQ, 1));
        } finally {
            MDC.remove("sentinela");
        }
        var primeiro = publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create());
        var segundo = publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create());
        primeiro.cancel();
        segundo.cancel();
        assertTrue(registros().isEmpty());
        assertTrue(spans().isEmpty());
        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                MDC.put("sentinela", "SEGREDO_MDC_CALLBACK");
                NDC.push("SEGREDO_NDC");
                try (var _ = Context.root().makeCurrent()) {
                    if (falhar) {
                        ack.completeExceptionally(new IllegalStateException("SEGREDO_SDK"));
                    } else {
                        ack.complete(null);
                    }
                    assertEquals("SEGREDO_MDC_CALLBACK", MDC.get("sentinela"));
                    assertEquals("SEGREDO_NDC", NDC.get());
                    assertEquals(SpanContext.getInvalid(), Span.current().getSpanContext());
                } finally {
                    MDC.remove("sentinela");
                    NDC.pop();
                }
            }).get(3, TimeUnit.SECONDS);
        }
        var tardio = publicacao.subscribe().withSubscriber(UniAssertSubscriber.<Void>create());
        if (falhar) {
            tardio.assertFailedWith(IllegalStateException.class);
        } else {
            tardio.assertCompleted();
        }
        assertEquals(1, registros().size());
        var registro = registros().getFirst();
        verificarRegistro(registro, falhar ? FALHOU : CONFIRMADA, falhar ? "FALHA_PUBLICACAO" : null);
        assertEquals(1, spans().size());
        verificarCorrelacao(registro, spans().getFirst());
        assertEquals(Span.fromContext(pai).getSpanContext(), spans().getFirst().getParentSpanContext());
        verify(sender).sendMessage(any(ServiceBusMessage.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void logDeveUsarContextoValidoNaoGravavelEOmitirContextoInvalido(boolean valido) throws IOException {
        var sender = mock(ServiceBusSenderAsyncClient.class);
        var enviado = new java.util.concurrent.atomic.AtomicReference<SpanContext>();
        when(sender.sendMessage(any(ServiceBusMessage.class))).thenAnswer(_ -> {
            enviado.set(Span.current().getSpanContext());
            return Mono.empty();
        });
        try (var provider = SdkTracerProvider.builder().setSampler(Sampler.alwaysOff()).build()) {
            var origem = valido ? provider.get("teste.nao-gravavel") : OpenTelemetry.noop().getTracer("teste.invalido");
            new MonitoramentoEntradaPublisher(referencia(sender), json, origem, FILA)
                    .executar(tentativa(MON, ORQ, 1))
                    .subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).assertCompleted();
        }
        assertTrue(spans().isEmpty());
        assertEquals(1, registros().size());
        var registro = registros().getFirst();
        if (valido) {
            assertTrue(enviado.get().isValid());
            assertFalse(enviado.get().isSampled());
            assertEquals(enviado.get().getTraceId(), registro.path("traceId").asText());
            assertEquals(enviado.get().getSpanId(), registro.path("spanId").asText());
        } else {
            assertFalse(registro.has("traceId"));
            assertFalse(registro.has("spanId"));
        }
        verificarRegistro(registro, CONFIRMADA, null);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "null|valido|1|false|true|true",
        "valido|null|1|true|false|true",
        "1-1-1-1-1|valido|1|false|true|true",
        "espacos|valido|1|false|true|true",
        "SEGREDO_ID|SEGREDO_ID|1|false|false|true",
        "maiusculo|maiusculo|2|true|true|true",
        "valido|valido|0|true|true|false",
        "valido|valido|-1|true|true|false"
    }, delimiter = '|', nullValues = "null")
    void deveCopiarSomenteIdsCanonicosETentativaPositiva(String mon, String orq, int tentativa,
            boolean monPresente, boolean orqPresente, boolean numeroPresente) throws IOException {
        var monitoramento = id(mon, MON);
        var orquestracao = id(orq, ORQ);
        new LogPublicacaoEntrada().registrar(tentativa(monitoramento, orquestracao, tentativa),
                SpanContext.getInvalid(), null);
        assertEquals(1, registros().size());
        var registro = registros().getFirst();
        assertEquals(monPresente, registro.has("monitoramento_id"));
        assertEquals(orqPresente, registro.has("orquestracao_id"));
        assertEquals(numeroPresente, registro.has("tentativa_atual"));
        assertEquals(monPresente && orqPresente && numeroPresente, registro.has("message_id"));
        if (monPresente) {
            assertEquals(monitoramento, registro.path("monitoramento_id").asText());
        }
        if (orqPresente) {
            assertEquals(orquestracao, registro.path("orquestracao_id").asText());
        }
        if (numeroPresente) {
            assertTrue(registro.path("tentativa_atual").isIntegralNumber());
            assertEquals(tentativa, registro.path("tentativa_atual").asInt());
        }
        if (registro.has("message_id")) {
            assertEquals(monitoramento + ":tentativa:" + tentativa, registro.path("message_id").asText());
        }
        assertFalse(registro.toString().contains("SEGREDO"));
    }

    @Test
    void preparacaoSemModeloDeveFalharSemInventarIds() throws IOException {
        var sender = mock(ServiceBusSenderAsyncClient.class);
        new MonitoramentoEntradaPublisher(referencia(sender), json, tracer, FILA).executar(null)
                .subscribe().withSubscriber(UniAssertSubscriber.<Void>create()).assertFailedWith(NullPointerException.class);
        verifyNoInteractions(sender);
        assertEquals(1, registros().size());
        var registro = registros().getFirst();
        assertEquals(FALHOU, registro.path("evento").asText());
        assertEquals("FALHA_PREPARACAO_PUBLICACAO", registro.path("error_type").asText());
        for (var campo : List.of("monitoramento_id", "orquestracao_id", "message_id", "tentativa_atual")) {
            assertFalse(registro.has(campo));
        }
        verificarCorrelacao(registro, spans().getFirst());
    }

    private static String id(String valor, String valido) {
        if (valor == null) {
            return null;
        }
        return switch (valor) {
            case "valido" -> valido;
            case "maiusculo" -> valido.toUpperCase(java.util.Locale.ROOT);
            case "espacos" -> " " + valido + " ";
            default -> valor;
        };
    }

    private static void verificarCorrelacao(JsonNode registro, SpanData span) {
        assertEquals(span.getTraceId(), registro.path("traceId").asText());
        assertEquals(span.getSpanId(), registro.path("spanId").asText());
    }

    private void verificarRegistro(JsonNode registro, String evento, String erro) {
        assertEquals(evento, registro.path("message").asText());
        assertEquals(evento, registro.path("evento").asText());
        assertEquals(erro == null ? "INFO" : "ERROR", registro.path("level").asText());
        assertEquals("adaptador", registro.path("camada").asText());
        assertEquals("MonitoramentoEntradaPublisher", registro.path("componente").asText());
        assertEquals("publicar", registro.path("operacao").asText());
        assertEquals(MON, registro.path("monitoramento_id").asText());
        assertEquals(ORQ, registro.path("orquestracao_id").asText());
        assertEquals(MON + ":tentativa:1", registro.path("message_id").asText());
        assertTrue(registro.path("tentativa_atual").isIntegralNumber());
        assertEquals(1, registro.path("tentativa_atual").asInt());
        if (erro == null) {
            assertFalse(registro.has("error_type"));
        } else {
            assertEquals(erro, registro.path("error_type").asText());
        }
        assertTrue(registro.path("mdc").isEmpty());
        assertEquals("", registro.path("ndc").asText());
        assertFalse(registro.has("exception"));
        assertFalse(registro.has("stacktrace"));
        assertFalse(registro.toString().contains("SEGREDO"));
        var esperados = new java.util.HashSet<>(java.util.Set.of("timestamp", "sequence", "loggerClassName",
                "loggerName", "level", "message", "threadName", "threadId", "mdc", "ndc", "hostName",
                "processName", "processId", "service.name", "evento", "camada", "componente", "operacao",
                "monitoramento_id", "orquestracao_id", "message_id", "tentativa_atual"));
        if (registro.has("traceId")) {
            esperados.addAll(List.of("traceId", "spanId"));
        }
        if (erro != null) {
            esperados.add("error_type");
        }
        var presentes = new java.util.HashSet<String>();
        registro.fieldNames().forEachRemaining(presentes::add);
        assertEquals(esperados, presentes, "JSON completo permite somente metadados padrao e campos aprovados.");
    }

    private List<JsonNode> registros() throws IOException {
        var linhas = Files.readAllLines(ARQUIVO);
        var registros = new ArrayList<JsonNode>();
        for (var linha : linhas.subList(primeiraLinha, linhas.size())) {
            var registro = json.readTree(linha);
            if (registro.path("message").asText().startsWith("orquestrador.")) {
                registros.add(registro);
            }
        }
        return registros;
    }

    private List<SpanData> spans() {
        var flush = ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider().forceFlush();
        flush.join(10, TimeUnit.SECONDS);
        assertTrue(flush.isSuccess());
        return exporter.getFinishedSpanItems();
    }

    @SuppressWarnings("unchecked")
    private static Instance<ServiceBusSenderAsyncClient> referencia(ServiceBusSenderAsyncClient sender) {
        var instancia = (Instance<ServiceBusSenderAsyncClient>) mock(Instance.class);
        when(instancia.get()).thenReturn(sender);
        return instancia;
    }

    private static TentativaMonitoramento tentativa(String mon, String orq, int numero) {
        return new TentativaMonitoramento(mon, orq, "SEGREDO_PRE", "SEGREDO_MTR", numero,
                Instant.parse("2026-09-11T12:00:00Z"), Instant.parse("2026-09-12T12:00:00Z"), "v1");
    }
}
