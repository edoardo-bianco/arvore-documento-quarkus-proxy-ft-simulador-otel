package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.documento;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import io.cloudevents.CloudEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.smallrye.mutiny.Uni;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jboss.logging.Logger;

public final class ObservabilidadeFeedDocumental {

    private static final Logger LOG =
            Logger.getLogger(ObservabilidadeFeedDocumental.class);
    private static final String NOME_SPAN =
            "simtr-hub.feed.conformidade.documento";
    private static final String EVENTO_CONCLUIDO =
            "conformidade.feed.operacao.concluida";
    private static final String EVENTO_FALHOU =
            "conformidade.feed.operacao.falhou";
    private static final String BACKEND = "conformidade.feed.backend";
    private static final String OPERACAO = "conformidade.feed.operacao";
    private static final String RESULTADO = "conformidade.feed.resultado";
    private static final String REPLAY = "conformidade.feed.replay";
    private static final String CURSOR_LEASE_ID =
            "conformidade.feed.cursor_lease_id";
    private static final String EVENTO_ID = "conformidade.feed.evento_id";
    private static final String CORRELATION_ID =
            "conformidade.analise.correlation_id";
    private static final String INSTANCE_ID =
            "conformidade.analise.instance_id";
    private static final String RESULTADO_FALHA = "FALHA";

    private final Tracer tracer;
    private final String backendDocumental;

    public ObservabilidadeFeedDocumental(Tracer tracer, String backend) {
        this.tracer = java.util.Objects.requireNonNull(tracer, "tracer");
        this.backendDocumental = validarBackend(backend);
    }

    public <T> Uni<T> observar(
            String operacao,
            String cursorLeaseId,
            String replay,
            CloudEvent evento,
            Supplier<Uni<T>> operacaoReativa,
            Function<T, String> resultadoDaOperacao) {
        return Uni.createFrom().deferred(() -> {
            Span span = tracer.spanBuilder(NOME_SPAN)
                    .setParent(Context.current())
                    .startSpan();
            Scope scope = span.makeCurrent();
            Uni<T> original;
            try {
                original = java.util.Objects.requireNonNull(
                        operacaoReativa.get(),
                        "resultado reativo");
            } catch (Exception falha) {
                original = Uni.createFrom().failure(falha);
            }

            Uni<Desfecho<T>> protegido = original
                    .onItemOrFailure()
                    .transform(Desfecho::new)
                    .invoke(desfecho -> registrar(
                            span,
                            operacao,
                            cursorLeaseId,
                            replay,
                            evento,
                            desfecho.falha() == null
                                    ? resultadoDaOperacao.apply(desfecho.item())
                                    : RESULTADO_FALHA));

            return protegido
                    .onTermination()
                    .invoke(() -> encerrar(span, scope))
                    .chain(desfecho -> desfecho.falha() == null
                            ? Uni.createFrom().item(desfecho.item())
                            : Uni.createFrom().failure(desfecho.falha()));
        });
    }

    public void executar(
            String operacao,
            String cursorLeaseId,
            String replay,
            CloudEvent evento,
            Runnable acao) {
        Span span = tracer.spanBuilder(NOME_SPAN)
                .setParent(Context.current())
                .startSpan();
        try (var _ = span.makeCurrent()) {
            try {
                acao.run();
                registrar(
                        span,
                        operacao,
                        cursorLeaseId,
                        replay,
                        evento,
                        "CONCLUIDO");
            } catch (RuntimeException falha) {
                registrar(
                        span,
                        operacao,
                        cursorLeaseId,
                        replay,
                        evento,
                        RESULTADO_FALHA);
                throw falha;
            }
        } finally {
            span.end();
        }
    }

    public void registrar(
            String operacao,
            String cursorLeaseId,
            String replay,
            CloudEvent evento,
            String resultado) {
        Span span = tracer.spanBuilder(NOME_SPAN)
                .setParent(Context.current())
                .startSpan();
        try (var _ = span.makeCurrent()) {
            registrar(
                    span,
                    operacao,
                    cursorLeaseId,
                    replay,
                    evento,
                    resultado);
        } finally {
            span.end();
        }
    }

    private void registrar(
            Span span,
            String operacao,
            String cursorLeaseId,
            String replay,
            CloudEvent evento,
            String resultado) {
        String eventoId = evento == null ? null : evento.getId();
        String correlationId = extensao(evento, "correlationid");
        String instanceId = extensao(evento, "flowinstanceid");
        if (RESULTADO_FALHA.equals(resultado)) {
            span.setStatus(StatusCode.ERROR);
        }
        definirAtributo(span, BACKEND, backendDocumental);
        definirAtributo(span, OPERACAO, operacao);
        definirAtributo(span, RESULTADO, resultado);
        definirAtributo(span, REPLAY, replay);
        definirAtributo(span, CURSOR_LEASE_ID, cursorLeaseId);
        definirAtributo(span, EVENTO_ID, eventoId);
        definirAtributo(span, CORRELATION_ID, correlationId);
        definirAtributo(span, INSTANCE_ID, instanceId);

        Map<String, Object> campos = ObservabilityLog.fields(
                BACKEND, backendDocumental,
                OPERACAO, operacao,
                RESULTADO, resultado,
                REPLAY, replay,
                CURSOR_LEASE_ID, cursorLeaseId,
                EVENTO_ID, eventoId,
                CORRELATION_ID, correlationId,
                INSTANCE_ID, instanceId);
        if (RESULTADO_FALHA.equals(resultado)) {
            ObservabilityLog.error(LOG, EVENTO_FALHOU, campos);
        } else {
            ObservabilityLog.info(LOG, EVENTO_CONCLUIDO, campos);
        }
    }

    private static String extensao(CloudEvent evento, String nome) {
        if (evento == null) {
            return null;
        }
        Object valor = evento.getExtension(nome);
        return valor == null || String.valueOf(valor).isBlank()
                ? null
                : String.valueOf(valor);
    }

    private static void definirAtributo(Span span, String chave, String valor) {
        if (valor != null && !valor.isBlank()) {
            span.setAttribute(chave, valor);
        }
    }

    private static void encerrar(Span span, Scope scope) {
        try {
            span.end();
        } finally {
            scope.close();
        }
    }

    private static String validarBackend(String backend) {
        if (backend == null || !backend.matches("[a-z][a-z0-9-]*")) {
            throw new IllegalArgumentException("Backend documental inválido");
        }
        return backend;
    }

    private record Desfecho<T>(T item, Throwable falha) {
    }
}
