package br.gov.caixa.simtr.orquestrador.aplicacao.casodeuso;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.ObterParametrosMonitoramento;
import br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.PublicarTentativaMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.MonitoramentoIniciado;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.ParametrosMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.SolicitacaoMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.TentativaMonitoramento;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
class IniciarMonitoramentoUseCaseTest {

    private static final Instant INICIO = Instant.parse("2026-09-08T12:00:00.123456789Z");
    private static final Duration ESPERA = Duration.ofSeconds(3);
    private static final SolicitacaoMonitoramento SOLICITACAO = new SolicitacaoMonitoramento("pre-em-analise", "0007");
    private static final Clock RELOGIO = Clock.fixed(INICIO, ZoneOffset.UTC);

    @Inject Tracer tracer;
    @Inject OpenTelemetry openTelemetry;
    @Inject InMemorySpanExporter exporter;
    private static final Context PAI = contexto("11111111111111111111111111111111", "aaaaaaaaaaaaaaaa");
    private static final Context OUTRO = contexto("22222222222222222222222222222222", "bbbbbbbbbbbbbbbb");
    private Span assinanteGravavel;
    private Scope contextoDoTeste;

    @AfterEach
    void encerrarControleAssinante() {
        if (assinanteGravavel != null) {
            assinanteGravavel.end();
        }
        contextoDoTeste.close();
    }

    @BeforeEach
    void prepararCaptura() {
        contextoDoTeste = Context.root().makeCurrent();
        spans();
        exporter.reset();
        var controle = tracer.spanBuilder("teste.iniciacao.controle").setNoParent().startSpan();
        assertTrue(controle.isRecording());
        controle.end();
        assertEquals(1, spans().size());
        exporter.reset();
    }

    private List<SpanData> spans() {
        var flush = ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider().forceFlush();
        flush.join(10, TimeUnit.SECONDS);
        assertTrue(flush.isSuccess());
        return exporter.getFinishedSpanItems();
    }

    private SpanData verificarSpan(Context pai, StatusCode status) {
        var capturados = spans();
        assertEquals(1, capturados.size());
        var span = capturados.getFirst();
        assertEquals("orquestrador.service.monitoramento-dossie.iniciar", span.getName());
        assertEquals(SpanKind.INTERNAL, span.getKind());
        assertEquals(Span.fromContext(pai).getSpanContext(), span.getParentSpanContext());
        assertEquals(status, span.getStatus().getStatusCode());
        assertEquals("", span.getStatus().getDescription());
        assertTrue(span.getEvents().isEmpty());
        assertTrue(span.getLinks().isEmpty());
        var esperados = status == StatusCode.ERROR
                ? Attributes.of(AttributeKey.stringKey("error.type"), "FALHA_INICIO") : Attributes.empty();
        assertEquals(esperados.asMap(), span.getAttributes().asMap());
        return span;
    }

    private static Context contexto(String traceId, String spanId) {
        return Context.root().with(Span.wrap(SpanContext.create(traceId, spanId,
                TraceFlags.getSampled(), TraceState.getDefault())));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deveRastrearOperacaoLazyUnicaComPaiDaInvocacaoEAckEmOutraThread(boolean gravavel) throws Exception {
        var assinante = OUTRO;
        if (gravavel) {
            assinanteGravavel = tracer.spanBuilder("teste.assinante").setParent(OUTRO).startSpan();
            assinante = Context.root().with(assinanteGravavel);
        }
        final var contextoAssinante = assinante;
        var confirmacao = new CompletableFuture<Void>();
        var capturas = new ArrayList<Span>();
        var caso = new IniciarMonitoramentoUseCase(instante -> {
            capturas.add(Span.current());
            return parametros().executar(instante);
        }, _ -> {
            capturas.add(Span.current());
            return Uni.createFrom().deferred(() -> {
                capturas.add(Span.current());
                return Uni.createFrom().completionStage(confirmacao);
            });
        }, RELOGIO, tracer);
        var anterior = Span.current().getSpanContext();
        Uni<MonitoramentoIniciado> inicio;
        try (var _ = PAI.makeCurrent()) {
            inicio = caso.executar(SOLICITACAO);
        }
        assertEquals(anterior, Span.current().getSpanContext());
        assertTrue(capturas.isEmpty());
        assertTrue(spans().isEmpty());
        CompletableFuture<MonitoramentoIniciado> primeiro;
        CompletableFuture<MonitoramentoIniciado> segundo;
        SpanContext depoisDaAssinatura;
        try (var _ = contextoAssinante.makeCurrent()) {
            primeiro = inicio.subscribeAsCompletionStage().toCompletableFuture();
            segundo = inicio.subscribeAsCompletionStage().toCompletableFuture();
            depoisDaAssinatura = Span.current().getSpanContext();
        }
        assertEquals(3, capturas.size());
        for (var captura : capturas) {
            assertTrue(captura.isRecording());
            assertEquals(capturas.getFirst().getSpanContext(), captura.getSpanContext());
        }
        assertFalse(primeiro.isDone());
        assertFalse(segundo.isDone());
        assertTrue(spans().isEmpty());
        List<SpanContext> contextosDaThread;
        try (var executor = Executors.newSingleThreadExecutor()) {
            contextosDaThread = executor.submit(() -> {
                var contextoAnterior = Span.current().getSpanContext();
                SpanContext observado;
                try (var _ = contextoAssinante.makeCurrent()) {
                    assertTrue(confirmacao.complete(null));
                    observado = Span.current().getSpanContext();
                }
                return List.of(observado, contextoAnterior, Span.current().getSpanContext());
            }).get(3, TimeUnit.SECONDS);
        }
        var resultado = primeiro.get(3, TimeUnit.SECONDS);
        assertSame(resultado, segundo.get(3, TimeUnit.SECONDS));
        assertSame(resultado, inicio.await().atMost(ESPERA));
        var span = verificarSpan(PAI, StatusCode.UNSET);
        assertEquals(capturas.getFirst().getSpanContext(), span.getSpanContext());
        assertFalse(capturas.getFirst().isRecording());
        var depoisDaReassinatura = Span.current().getSpanContext();
        var esperado = Span.fromContext(contextoAssinante).getSpanContext();
        assertAll(() -> assertEquals(esperado, depoisDaAssinatura, "Restauracao depois da assinatura"),
                () -> assertEquals(esperado, contextosDaThread.getFirst(), "Restauracao depois do callback"),
                () -> assertEquals(contextosDaThread.get(1), contextosDaThread.getLast(), "Contexto original da thread"),
                () -> assertEquals(anterior, depoisDaReassinatura, "Restauracao depois da assinatura tardia"));
    }

    @Test
    void devePrepararPublicarEAguardarConfirmacaoPreservandoDados() throws Exception {
        var enviados = new ArrayList<TentativaMonitoramento>();
        var ordem = new ArrayList<String>();
        var confirmacao = new CompletableFuture<Void>();
        var caso = new IniciarMonitoramentoUseCase(instante -> {
            ordem.add("parametros");
            assertEquals(INICIO, instante);
            return new ParametrosMonitoramento(INICIO.plusSeconds(731), "politica-configurada");
        }, tentativa -> {
            ordem.add("publicacao");
            enviados.add(tentativa);
            return Uni.createFrom().completionStage(confirmacao);
        }, RELOGIO, tracer);

        var inicio = caso.executar(SOLICITACAO);
        assertEquals(0, ordem.size());
        var primeiro = inicio.subscribeAsCompletionStage().toCompletableFuture();
        var segundo = inicio.subscribeAsCompletionStage().toCompletableFuture();
        assertFalse(primeiro.isDone());
        assertFalse(segundo.isDone());
        confirmacao.complete(null);
        var resultado = primeiro.get(3, TimeUnit.SECONDS);
        assertEquals(resultado, segundo.get(3, TimeUnit.SECONDS));
        assertEquals(resultado, inicio.await().atMost(ESPERA));
        assertEquals(java.util.List.of("parametros", "publicacao"), ordem);
        assertEquals(1, enviados.size());
        var tentativa = enviados.getFirst();
        assertEquals(new MonitoramentoIniciado(tentativa.monitoramentoId(), tentativa.orquestracaoId()), resultado);
        assertNotNull(UUID.fromString(resultado.monitoramentoId()));
        assertNotNull(UUID.fromString(resultado.orquestracaoId()));
        assertNotEquals(resultado.monitoramentoId(), resultado.orquestracaoId());
        assertEquals("pre-em-analise", tentativa.idDossiePreValidacao());
        assertEquals("0007", tentativa.idDossieMtr());
        assertEquals(1, tentativa.tentativaAtual());
        assertEquals(INICIO, tentativa.iniciadoEm());
        assertEquals(INICIO.plusSeconds(731), tentativa.limiteEm());
        assertEquals("politica-configurada", tentativa.politicaMonitoramentoVersao());
    }

    @Test
    void deveGerarIdentidadesIndependentesParaSolicitacoesDistintas() {
        var enviados = new ArrayList<TentativaMonitoramento>();
        var confirmacoes = new ArrayList<CompletableFuture<Void>>();
        var caso = new IniciarMonitoramentoUseCase(parametros(), tentativa -> {
            enviados.add(tentativa);
            var confirmacao = new CompletableFuture<Void>();
            confirmacoes.add(confirmacao);
            return Uni.createFrom().completionStage(confirmacao);
        }, RELOGIO, tracer);
        Uni<MonitoramentoIniciado> inicioPrimeiro;
        Uni<MonitoramentoIniciado> inicioSegundo;
        try (var _ = PAI.makeCurrent()) {
            inicioPrimeiro = caso.executar(SOLICITACAO);
        }
        try (var _ = OUTRO.makeCurrent()) {
            inicioSegundo = caso.executar(SOLICITACAO);
        }
        var aguardandoPrimeiro = inicioPrimeiro.subscribeAsCompletionStage().toCompletableFuture();
        inicioSegundo.subscribeAsCompletionStage();
        assertEquals(2, enviados.size());
        confirmacoes.getLast().complete(null);
        var segundo = inicioSegundo.await().atMost(ESPERA);
        assertFalse(aguardandoPrimeiro.isDone());
        confirmacoes.getFirst().complete(null);
        var primeiro = inicioPrimeiro.await().atMost(ESPERA);
        assertEquals(enviados.getFirst().monitoramentoId(), primeiro.monitoramentoId());
        assertEquals(enviados.getLast().monitoramentoId(), segundo.monitoramentoId());
        assertNotEquals(primeiro.monitoramentoId(), segundo.monitoramentoId());
        assertNotEquals(primeiro.orquestracaoId(), segundo.orquestracaoId());
        assertEquals(2, enviados.size());
        var encerrados = spans();
        assertEquals(2, encerrados.size());
        assertEquals(Span.fromContext(OUTRO).getSpanContext(), encerrados.getFirst().getParentSpanContext());
        assertEquals(Span.fromContext(PAI).getSpanContext(), encerrados.getLast().getParentSpanContext());
        assertNotEquals(encerrados.getFirst().getSpanId(), encerrados.getLast().getSpanId());
        assertNotEquals(encerrados.getFirst().getTraceId(), encerrados.getLast().getTraceId());
        assertEquals(SpanContext.getInvalid(), Span.current().getSpanContext());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deveConfirmarPublicacaoUnicaAposCancelarAssinantes(boolean cancelarTodos) throws Exception {
        var enviados = new ArrayList<TentativaMonitoramento>();
        var confirmacao = new CompletableFuture<Void>();
        var cancelamentos = new AtomicInteger();
        var caso = new IniciarMonitoramentoUseCase(parametros(), tentativa -> {
            enviados.add(tentativa);
            return Uni.createFrom().completionStage(confirmacao)
                    .onCancellation().invoke(cancelamentos::incrementAndGet);
        }, RELOGIO, tracer);
        var inicio = caso.executar(SOLICITACAO);
        var primeiro = inicio.subscribeAsCompletionStage().toCompletableFuture();
        var segundo = inicio.subscribeAsCompletionStage().toCompletableFuture();
        assertTrue(primeiro.cancel(true));
        if (cancelarTodos) {
            assertTrue(segundo.cancel(true));
        }
        assertTrue(primeiro.isCancelled());
        assertEquals(cancelarTodos, segundo.isDone());
        assertFalse(confirmacao.isDone());
        assertEquals(0, cancelamentos.get());
        assertTrue(spans().isEmpty(), "Cancelar observadores nao encerra a operacao upstream.");
        assertTrue(confirmacao.complete(null));
        var resultado = inicio.await().atMost(ESPERA);
        if (!cancelarTodos) {
            assertSame(resultado, segundo.get(3, TimeUnit.SECONDS));
        }
        assertEquals(1, enviados.size());
        var enviada = enviados.getFirst();
        assertEquals(new MonitoramentoIniciado(enviada.monitoramentoId(), enviada.orquestracaoId()), resultado);
        assertEquals(0, cancelamentos.get());
        verificarSpan(Context.root(), StatusCode.UNSET);
    }

    @Test
    void deveMemorizarFalhaAposCancelamentoDeTodosSemRepublicar() {
        var confirmacao = new CompletableFuture<Void>();
        var chamadas = new AtomicInteger();
        var cancelamentos = new AtomicInteger();
        var caso = new IniciarMonitoramentoUseCase(parametros(), _ -> {
            chamadas.incrementAndGet();
            return Uni.createFrom().completionStage(confirmacao)
                    .onCancellation().invoke(cancelamentos::incrementAndGet);
        }, RELOGIO, tracer);
        var inicio = caso.executar(SOLICITACAO);
        var primeiro = inicio.subscribeAsCompletionStage().toCompletableFuture();
        var segundo = inicio.subscribeAsCompletionStage().toCompletableFuture();
        assertTrue(primeiro.cancel(true));
        assertTrue(segundo.cancel(true));
        assertFalse(confirmacao.isDone());
        assertTrue(spans().isEmpty());
        var falha = new IllegalStateException("Falha sintetica apos cancelamento dos observadores.");
        assertTrue(confirmacao.completeExceptionally(falha));
        var aguardando = inicio.await();
        assertSame(falha, assertThrows(IllegalStateException.class, () -> aguardando.atMost(ESPERA)));
        assertSame(falha, assertThrows(IllegalStateException.class, () -> aguardando.atMost(ESPERA)));
        assertEquals(1, chamadas.get());
        assertEquals(0, cancelamentos.get());
        verificarSpan(Context.root(), StatusCode.ERROR);
    }

    @Test
    void devePropagarFalhaDePreparacaoSemPublicar() {
        var falha = new IllegalStateException("Falha sintetica de preparacao.");
        var chamadas = new AtomicInteger();
        var caso = new IniciarMonitoramentoUseCase(_ -> { throw falha; },
                _ -> { chamadas.incrementAndGet(); return Uni.createFrom().voidItem(); }, RELOGIO, tracer);
        var aguardando = caso.executar(SOLICITACAO).await();
        assertSame(falha, assertThrows(IllegalStateException.class, () -> aguardando.atMost(ESPERA)));
        assertEquals(0, chamadas.get());
        verificarSpan(Context.root(), StatusCode.ERROR);
    }

    @Test
    void devePropagarFalhaAssincronaSemRetornoDeSucessoOuRetry() {
        var falha = new IllegalStateException("Falha sintetica de publicacao.");
        var chamadas = new AtomicInteger();
        var caso = new IniciarMonitoramentoUseCase(parametros(), _ -> {
            chamadas.incrementAndGet();
            return Uni.createFrom().failure(falha);
        }, RELOGIO, tracer);
        var aguardando = caso.executar(SOLICITACAO).await();
        assertSame(falha, assertThrows(IllegalStateException.class, () -> aguardando.atMost(ESPERA)));
        assertEquals(1, chamadas.get());
        verificarSpan(Context.root(), StatusCode.ERROR);
    }

    @Test
    void deveEmitirFalhaSincronaDaPortaNoFluxo() {
        var falha = new IllegalStateException("Falha sintetica sincrona.");
        PublicarTentativaMonitoramento publicar = _ -> { throw falha; };
        var caso = new IniciarMonitoramentoUseCase(parametros(), publicar, RELOGIO, tracer);
        var aguardando = caso.executar(SOLICITACAO).await();
        assertSame(falha, assertThrows(IllegalStateException.class, () -> aguardando.atMost(ESPERA)));
        verificarSpan(Context.root(), StatusCode.ERROR);
    }

    @Test
    void deveRejeitarSolicitacaoAusenteAntesDePrepararOuPublicar() {
        var chamadas = new AtomicInteger();
        var caso = new IniciarMonitoramentoUseCase(_ -> {
            chamadas.incrementAndGet();
            return new ParametrosMonitoramento(INICIO.plusSeconds(60), "v1");
        }, _ -> { chamadas.incrementAndGet(); return Uni.createFrom().voidItem(); }, RELOGIO, tracer);
        var aguardando = caso.executar(null).await();
        assertThrows(NullPointerException.class, () -> aguardando.atMost(ESPERA));
        assertEquals(0, chamadas.get());
        verificarSpan(Context.root(), StatusCode.ERROR);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deveSanitizarFalhaEmOutraThreadERestaurarContextoAposCancelamento(boolean cancelar) throws Exception {
        var confirmacao = new CompletableFuture<Void>();
        var falha = new IllegalStateException("sentinela-mensagem", new RuntimeException("sentinela-causa"));
        falha.addSuppressed(new RuntimeException("sentinela-suppressed"));
        var caso = new IniciarMonitoramentoUseCase(parametros(),
                _ -> Uni.createFrom().completionStage(confirmacao), RELOGIO, tracer);
        Uni<MonitoramentoIniciado> inicio;
        try (var _ = PAI.makeCurrent()) {
            inicio = caso.executar(SOLICITACAO);
        }
        var assinante = inicio.subscribe().withSubscriber(UniAssertSubscriber.<MonitoramentoIniciado>create());
        if (cancelar) {
            assinante.cancel();
        }
        assertTrue(spans().isEmpty());
        try (var executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                try (var _ = OUTRO.makeCurrent()) {
                    assertTrue(confirmacao.completeExceptionally(falha));
                    assertEquals(Span.fromContext(OUTRO).getSpanContext(), Span.current().getSpanContext());
                }
                assertEquals(SpanContext.getInvalid(), Span.current().getSpanContext());
            }).get(3, TimeUnit.SECONDS);
        }
        var aguardando = inicio.await();
        assertSame(falha, assertThrows(IllegalStateException.class, () -> aguardando.atMost(ESPERA)));
        assertSame(falha, assertThrows(IllegalStateException.class, () -> aguardando.atMost(ESPERA)));
        if (cancelar) {
            assinante.assertNotTerminated();
        } else {
            assertSame(falha, assinante.getFailure());
        }
        verificarSpan(PAI, StatusCode.ERROR);
        assertEquals(SpanContext.getInvalid(), Span.current().getSpanContext());
    }

    @Test
    void deveTratarCancelamentoEfetivoDoUpstreamComoFalha() {
        var confirmacao = new CompletableFuture<Void>();
        var caso = new IniciarMonitoramentoUseCase(parametros(),
                _ -> Uni.createFrom().completionStage(confirmacao), RELOGIO, tracer);
        var inicio = caso.executar(SOLICITACAO);
        inicio.subscribe().withSubscriber(UniAssertSubscriber.create());
        assertTrue(confirmacao.cancel(true));
        var aguardando = inicio.await();
        var falha = assertThrows(CancellationException.class, () -> aguardando.atMost(ESPERA));
        assertSame(falha, assertThrows(CancellationException.class, () -> aguardando.atMost(ESPERA)));
        verificarSpan(Context.root(), StatusCode.ERROR);
        assertEquals(SpanContext.getInvalid(), Span.current().getSpanContext());
    }

    @Test
    void devePreservarContextoMutinyDoAssinanteNaPorta() throws Exception {
        var caso = new IniciarMonitoramentoUseCase(parametros(),
                _ -> Uni.createFrom().context(contexto -> {
                    assertEquals("teste", contexto.get("marcador"));
                    return Uni.createFrom().voidItem();
                }), RELOGIO, tracer);
        var inicio = caso.executar(SOLICITACAO);
        var resultado = inicio.subscribe().asCompletionStage(io.smallrye.mutiny.Context.of("marcador", "teste"))
                .get(3, TimeUnit.SECONDS);
        assertNotNull(resultado);
        verificarSpan(Context.root(), StatusCode.UNSET);
    }

    private static ObterParametrosMonitoramento parametros() {
        return iniciadoEm -> new ParametrosMonitoramento(iniciadoEm.plusSeconds(60), "v1");
    }
}
