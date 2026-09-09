package br.gov.caixa.simtr.monitoramento.aplicacao.porta.entrada;

import br.gov.caixa.simtr.monitoramento.dominio.modelo.ParametrosMonitoramento;
import java.time.Instant;

/** Calcula parametros iniciais em memoria pela politica CDI do monitoramento. */
public interface PrepararMonitoramento {

    /** @param iniciadoEm instante gerado pelo consumidor; nao gera IDs nem consulta fontes
     * @return limite calculado e versao da politica, preservados no reagendamento */
    ParametrosMonitoramento executar(Instant iniciadoEm);
}
