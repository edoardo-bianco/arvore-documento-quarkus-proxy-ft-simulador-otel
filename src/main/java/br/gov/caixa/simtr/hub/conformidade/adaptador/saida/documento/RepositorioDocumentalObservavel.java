package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.documento;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.smallrye.mutiny.Uni;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jboss.logging.Logger;

public final class RepositorioDocumentalObservavel
        implements RepositorioDocumental {

    private static final Logger LOG =
            Logger.getLogger(RepositorioDocumentalObservavel.class);
    private static final String NOME_SPAN =
            "simtr-hub.persistencia.conformidade.documento";
    private static final String EVENTO_CONCLUIDO =
            "conformidade.persistencia.operacao.concluida";
    private static final String EVENTO_FALHOU =
            "conformidade.persistencia.operacao.falhou";
    private static final String BACKEND = "conformidade.persistencia.backend";
    private static final String OPERACAO = "conformidade.persistencia.operacao";
    private static final String RESULTADO = "conformidade.persistencia.resultado";
    private static final String CORRELATION_ID =
            "conformidade.analise.correlation_id";
    private static final String INSTANCE_ID =
            "conformidade.analise.instance_id";
    private static final String DOCUMENTO_ID =
            "conformidade.analise.documento_id";

    private final RepositorioDocumental delegate;
    private final Tracer tracer;
    private final String backendDocumental;

    public RepositorioDocumentalObservavel(
            RepositorioDocumental delegate,
            Tracer tracer,
            String backend) {
        this.delegate = java.util.Objects.requireNonNull(delegate, "delegate");
        this.tracer = java.util.Objects.requireNonNull(tracer, "tracer");
        this.backendDocumental = validarBackend(backend);
    }

    @Override
    public Uni<ResultadoGravacao> criar(
            String documentId,
            ObjectNode documento) {
        return observar(
                "criar",
                identidades(documentId, documento, null),
                () -> delegate.criar(documentId, documento),
                ResultadoGravacao::name);
    }

    @Override
    public Uni<ResultadoGravacao> substituir(
            String documentId,
            String versaoEsperada,
            ObjectNode documento) {
        return observar(
                "substituir",
                identidades(documentId, documento, null),
                () -> delegate.substituir(documentId, versaoEsperada, documento),
                ResultadoGravacao::name);
    }

    @Override
    public Uni<Optional<DocumentoPersistido>> consultar(
            String documentId,
            String correlationId) {
        return observar(
                "consultar",
                identidades(documentId, null, correlationId),
                () -> delegate.consultar(documentId, correlationId),
                resultado -> resultado.isPresent()
                        ? "ENCONTRADO"
                        : "NAO_ENCONTRADO");
    }

    private <T> Uni<T> observar(
            String operacao,
            Identidades identidades,
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
                            identidades,
                            desfecho,
                            resultadoDaOperacao));

            return protegido
                    .onTermination()
                    .invoke(() -> encerrar(span, scope))
                    .chain(desfecho -> desfecho.falha() == null
                            ? Uni.createFrom().item(desfecho.item())
                            : Uni.createFrom().failure(desfecho.falha()));
        });
    }

    private <T> void registrar(
            Span span,
            String operacao,
            Identidades identidades,
            Desfecho<T> desfecho,
            Function<T, String> resultadoDaOperacao) {
        String resultado;
        if (desfecho.falha() == null) {
            resultado = resultadoDaOperacao.apply(desfecho.item());
        } else {
            resultado = "FALHA";
            span.setStatus(StatusCode.ERROR);
        }

        definirAtributo(span, BACKEND, backendDocumental);
        definirAtributo(span, OPERACAO, operacao);
        definirAtributo(span, RESULTADO, resultado);
        definirAtributo(span, CORRELATION_ID, identidades.correlationId());
        definirAtributo(span, INSTANCE_ID, identidades.instanceId());
        definirAtributo(span, DOCUMENTO_ID, identidades.documentId());

        Map<String, Object> campos = ObservabilityLog.fields(
                BACKEND, backendDocumental,
                OPERACAO, operacao,
                RESULTADO, resultado,
                CORRELATION_ID, identidades.correlationId(),
                INSTANCE_ID, identidades.instanceId(),
                DOCUMENTO_ID, identidades.documentId());
        if (desfecho.falha() == null) {
            ObservabilityLog.info(LOG, EVENTO_CONCLUIDO, campos);
        } else {
            ObservabilityLog.error(LOG, EVENTO_FALHOU, campos);
        }
    }

    private static Identidades identidades(
            String documentId,
            JsonNode documento,
            String correlationIdInformado) {
        String correlationId = correlationIdInformado == null
                ? texto(documento, "correlationId")
                : correlationIdInformado;
        return new Identidades(
                documentId,
                correlationId,
                texto(documento, "instanceId"));
    }

    private static String texto(JsonNode documento, String campo) {
        if (documento == null) {
            return null;
        }
        JsonNode valor = documento.path(campo);
        return valor.isTextual() && !valor.textValue().isBlank()
                ? valor.textValue()
                : null;
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

    private record Identidades(
            String documentId,
            String correlationId,
            String instanceId) {
    }

    private record Desfecho<T>(T item, Throwable falha) {
    }
}
