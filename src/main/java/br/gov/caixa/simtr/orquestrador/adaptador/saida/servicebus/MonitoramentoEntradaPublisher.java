package br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus;

import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.FilaEntrada;
import br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.PublicarTentativaMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.TentativaMonitoramento;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ThreadContext;

/** Publica pelo cliente da fabrica e so conclui depois da confirmacao do broker. */
@ApplicationScoped
public class MonitoramentoEntradaPublisher implements PublicarTentativaMonitoramento {

    private final Instance<ServiceBusSenderAsyncClient> sender;
    private final ObjectMapper json;
    private final Tracer tracer;
    private final String fila;
    private final LogPublicacaoEntrada log;
    private final ThreadContext callbacks = ThreadContext.builder()
            .propagated(ThreadContext.ALL_REMAINING).unchanged("OpenTelemetry").cleared().build();

    @Inject
    public MonitoramentoEntradaPublisher(@FilaEntrada Instance<ServiceBusSenderAsyncClient> sender,
            ObjectMapper json, Tracer tracer,
            @ConfigProperty(name = "monitoramento.service-bus.input-queue") String fila) {
        this(sender, json, tracer, fila, new LogPublicacaoEntrada());
    }

    MonitoramentoEntradaPublisher(Instance<ServiceBusSenderAsyncClient> sender,
            ObjectMapper json, Tracer tracer, String fila, LogPublicacaoEntrada log) {
        this.log = log;
        this.sender = sender;
        this.json = json;
        this.tracer = tracer;
        this.fila = fila;
    }

    /** Uma invocacao compartilha a confirmacao; nao adiciona retry nem cria clientes. */
    @Override
    public Uni<Void> executar(TentativaMonitoramento tentativa) {
        var pai = Context.current();
        // Politica local: OTel usa Scopes explicitos; os demais contextos seguem propagados.
        return callbacks.contextualSupplier(() -> Uni.createFrom().<Void>emitter(emissor -> {
            var span = tracer.spanBuilder("send " + fila).setSpanKind(SpanKind.PRODUCER).setParent(pai)
                    .setAttribute("messaging.system", "servicebus")
                    .setAttribute("messaging.destination.name", fila)
                    .setAttribute("messaging.operation.name", "send")
                    .setAttribute("messaging.operation.type", "send").startSpan();
            var contexto = pai.with(span);
            var tipoFalha = new AtomicReference<String>();
            CompletableFuture<Void> confirmacao;
            try (var _ = contexto.makeCurrent()) {
                confirmacao = prepararEEnviar(tentativa, contexto, tipoFalha)
                        .subscribe().asCompletionStage(emissor.context());
            }
            // Registrar depois de fechar o Scope tambem protege o ACK sincrono.
            confirmacao.handle((resultado, falha) -> finalizarPublicacao(tentativa, contexto, tipoFalha.get(), falha))
                    .whenComplete((falhaPublicacao, falhaObservacao) -> {
                        var falha = falhaObservacao == null ? falhaPublicacao : falhaObservacao;
                        if (falha == null) {
                            emissor.complete(null);
                        } else {
                            emissor.fail(falha);
                        }
                    });
        }).memoize().indefinitely()).get();
    }

    private Throwable finalizarPublicacao(TentativaMonitoramento tentativa, Context contexto,
            String tipoFalha, Throwable falha) {
        var span = Span.fromContext(contexto);
        try (var _ = contexto.makeCurrent()) {
            if (falha != null) {
                span.setStatus(StatusCode.ERROR).setAttribute("error.type", tipoFalha);
            }
            if (!(falha instanceof SerializacaoEntradaException)) {
                registrarPublicacao(tentativa, span, tipoFalha);
            }
        } finally {
            span.end();
        }
        return falha; // Preserva identidade, sem wrapper de falha ao encadear o callback.
    }

    private void registrarPublicacao(TentativaMonitoramento tentativa, Span span, String tipoFalha) {
        try {
            log.registrar(tentativa, span.getSpanContext(), tipoFalha);
        } catch (RuntimeException _) {
            // CP-B1: falha isolada do novo log preserva ACK/erro seguro, sem outro evento ou envio.
        }
    }

    private Uni<Void> prepararEEnviar(TentativaMonitoramento tentativa, Context contexto,
            AtomicReference<String> tipoFalha) {
        return Uni.createFrom().item(() -> {
            var mensagem = MonitoramentoEntradaServiceBusMapper.paraMensagem(tentativa, json);
            W3CTraceContextPropagator.getInstance().inject(contexto, mensagem.getApplicationProperties(), Map::put);
            return mensagem;
        }).onFailure().invoke(falha -> tipoFalha.set(
                falha instanceof SerializacaoEntradaException
                        ? "SERIALIZACAO_ENTRADA" : "FALHA_PREPARACAO_PUBLICACAO"))
                .onItem().transformToUni(mensagem -> Uni.createFrom().completionStage(
                        () -> sender.get().sendMessage(mensagem).toFuture())
                        .onFailure().transform(_ -> {
                            tipoFalha.set("FALHA_PUBLICACAO");
                            return new IllegalStateException("Falha ao publicar tentativa de monitoramento.");
                        }));
    }
}
