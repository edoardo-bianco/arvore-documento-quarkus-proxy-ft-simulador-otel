package br.gov.caixa.simtr.orquestrador.aplicacao.casodeuso;

import br.gov.caixa.simtr.orquestrador.aplicacao.porta.entrada.IniciarMonitoramento;
import br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.ObterParametrosMonitoramento;
import br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.PublicarTentativaMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.MonitoramentoIniciado;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.SolicitacaoMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.TentativaMonitoramento;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/** Gera a identidade inicial e publica pela porta; a politica permanece no monitoramento. */
@ApplicationScoped
public class IniciarMonitoramentoUseCase implements IniciarMonitoramento {

    private final ObterParametrosMonitoramento parametros;
    private final PublicarTentativaMonitoramento publicar;
    private final Clock relogio;

    @Inject
    public IniciarMonitoramentoUseCase(ObterParametrosMonitoramento parametros, PublicarTentativaMonitoramento publicar) {
        this(parametros, publicar, Clock.systemUTC());
    }

    IniciarMonitoramentoUseCase(ObterParametrosMonitoramento parametros,
            PublicarTentativaMonitoramento publicar, Clock relogio) {
        this.parametros = parametros;
        this.publicar = publicar;
        this.relogio = relogio;
    }

    /** Cada invocacao mantem IDs e parametros estaveis e conclui somente apos a publicacao. */
    @Override
    public Uni<MonitoramentoIniciado> executar(SolicitacaoMonitoramento solicitacao) {
        return Uni.createFrom().deferred(() -> {
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
        }).memoize().indefinitely();
    }
}
