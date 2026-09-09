package br.gov.caixa.simtr.orquestrador.adaptador.saida.acl.monitoramento;

import br.gov.caixa.simtr.monitoramento.aplicacao.porta.entrada.PrepararMonitoramento;
import br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.ObterParametrosMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.ParametrosMonitoramento;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;

/** Traduz a preparacao pela API publica do monitoramento, sem duplicar sua politica. */
@ApplicationScoped
public class ParametrosMonitoramentoAcl implements ObterParametrosMonitoramento {

    private final PrepararMonitoramento monitoramento;

    @Inject
    public ParametrosMonitoramentoAcl(PrepararMonitoramento monitoramento) {
        this.monitoramento = monitoramento;
    }

    /** Mantem a chamada local/sincrona e preserva limite e versao no tipo proprio. */
    @Override
    public ParametrosMonitoramento executar(Instant iniciadoEm) {
        var parametros = monitoramento.executar(iniciadoEm);
        return new ParametrosMonitoramento(parametros.limiteEm(), parametros.politicaMonitoramentoVersao());
    }
}
