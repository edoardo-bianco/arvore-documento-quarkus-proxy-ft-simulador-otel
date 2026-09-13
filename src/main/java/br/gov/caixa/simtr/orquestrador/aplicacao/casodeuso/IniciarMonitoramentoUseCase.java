package br.gov.caixa.simtr.orquestrador.aplicacao.casodeuso;

import br.gov.caixa.simtr.orquestrador.aplicacao.porta.entrada.IniciarMonitoramento;
import br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.ObterParametrosMonitoramento;
import br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.PublicarTentativaMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.MonitoramentoIniciado;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.SolicitacaoMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.TentativaMonitoramento;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.eclipse.microprofile.context.ThreadContext;

/** Gera a identidade inicial e publica pela porta; a politica permanece no monitoramento. */
@ApplicationScoped
public class IniciarMonitoramentoUseCase implements IniciarMonitoramento {

    private final ObterParametrosMonitoramento parametros;
    private final PublicarTentativaMonitoramento publicar;
    private final Clock relogio;
    private final Tracer tracer;
    private final ThreadContext callbacks = ThreadContext.builder()
            .propagated(ThreadContext.ALL_REMAINING).unchanged("OpenTelemetry").cleared().build();

    @Inject
    public IniciarMonitoramentoUseCase(ObterParametrosMonitoramento parametros,
            PublicarTentativaMonitoramento publicar, Tracer tracer) {
        this(parametros, publicar, Clock.systemUTC(), tracer);
    }

    IniciarMonitoramentoUseCase(ObterParametrosMonitoramento parametros,
            PublicarTentativaMonitoramento publicar, Clock relogio, Tracer tracer) {
        this.parametros = parametros;
        this.publicar = publicar;
        this.relogio = relogio;
        this.tracer = tracer;
    }

    /** Cada invocacao mantem IDs e parametros estaveis e conclui somente apos a publicacao. */
    @Override
    public Uni<MonitoramentoIniciado> executar(SolicitacaoMonitoramento solicitacao) {
        var pai = Context.current();
        // O contexto OTel e manual; os demais contextos continuam propagados pelo runtime.
        return callbacks.contextualSupplier(() -> Uni.createFrom().<MonitoramentoIniciado>emitter(emissor -> {
            var span = tracer.spanBuilder("orquestrador.service.monitoramento-dossie.iniciar")
                    .setSpanKind(SpanKind.INTERNAL).setParent(pai).startSpan();
            var contexto = pai.with(span);
            CompletableFuture<MonitoramentoIniciado> confirmacao;
            try (var _ = contexto.makeCurrent()) {
                // A assinatura da porta tambem precisa ocorrer no contexto da operacao.
                confirmacao = Uni.createFrom().deferred(() -> preparar(solicitacao))
                        .subscribe().asCompletionStage(emissor.context());
            }
            confirmacao.whenComplete((resultado, falha) -> {
                try (var _ = contexto.makeCurrent()) {
                    if (falha != null) {
                        span.setAttribute("error.type", "FALHA_INICIO");
                        span.setStatus(StatusCode.ERROR);
                    }
                    span.end();
                }
                if (falha == null) {
                    emissor.complete(resultado);
                } else {
                    emissor.fail(falha);
                }
            });
        }).memoize().indefinitely()).get();
    }

    private Uni<MonitoramentoIniciado> preparar(SolicitacaoMonitoramento solicitacao) {
        Objects.requireNonNull(solicitacao, "Solicitacao de monitoramento obrigatoria.");
        var iniciadoEm = relogio.instant();
        var configurados = parametros.executar(iniciadoEm);
        var monitoramentoId = UUID.randomUUID().toString();
        var orquestracaoId = UUID.randomUUID().toString();
        var tentativa = new TentativaMonitoramento(monitoramentoId, orquestracaoId,
                solicitacao.idDossiePreValidacao(), solicitacao.idDossieMtr(), 1,
                iniciadoEm, configurados.limiteEm(), configurados.politicaMonitoramentoVersao());
        return publicar.executar(tentativa)
                .replaceWith(new MonitoramentoIniciado(monitoramentoId, orquestracaoId));
    }
}
