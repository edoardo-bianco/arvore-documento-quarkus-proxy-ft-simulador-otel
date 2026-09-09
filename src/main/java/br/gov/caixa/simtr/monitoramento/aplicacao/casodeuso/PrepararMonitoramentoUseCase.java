package br.gov.caixa.simtr.monitoramento.aplicacao.casodeuso;

import br.gov.caixa.simtr.monitoramento.aplicacao.porta.entrada.PrepararMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.ParametrosMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.politica.PoliticaMonitoramento;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;

/** Calcula os parametros iniciais em memoria pela politica validada do monitoramento. */
@ApplicationScoped
public class PrepararMonitoramentoUseCase implements PrepararMonitoramento {

    private final PoliticaMonitoramento politica;

    @Inject
    public PrepararMonitoramentoUseCase(PoliticaMonitoramento politica) {
        this.politica = politica;
    }

    /** Preserva o instante recebido; nao consulta fontes, gera IDs ou publica mensagens. */
    @Override
    public ParametrosMonitoramento executar(Instant iniciadoEm) {
        return new ParametrosMonitoramento(politica.calcularLimite(iniciadoEm), politica.versao());
    }
}
