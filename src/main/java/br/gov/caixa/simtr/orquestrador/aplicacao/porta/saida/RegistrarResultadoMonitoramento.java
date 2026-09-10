package br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida;

import br.gov.caixa.simtr.orquestrador.dominio.modelo.ResultadoMonitoramento;
import io.smallrye.mutiny.Uni;

/**
 * Submete o evento final de resultado ao logging padrao, sem persistencia ou workflow.
 * O retorno nao confirma escrita no destino: falhas/filtros internos dos handlers podem ser silenciosos.
 */
public interface RegistrarResultadoMonitoramento {
    /**
     * @param resultado modelo proprio do orquestrador, sem dependencia do DTO de transporte
     * @return conclusao da submissao ao pipeline de logging, ou falha anterior a submissao
     */
    Uni<Void> executar(ResultadoMonitoramento resultado);
}
