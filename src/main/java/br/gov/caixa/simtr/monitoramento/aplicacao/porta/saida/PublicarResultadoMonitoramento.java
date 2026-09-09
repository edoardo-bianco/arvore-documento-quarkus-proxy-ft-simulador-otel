package br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida;

import br.gov.caixa.simtr.monitoramento.dominio.modelo.ResultadoMonitoramento;
import io.smallrye.mutiny.Uni;

/**
 * Publica resultado terminal ou quarentena pela borda Service Bus do monitoramento.
 * A situacao original do MTR permanece separada da pre-validacao calculada.
 * Nao persiste transicao nem representa transacao entre as duas filas.
 */
public interface PublicarResultadoMonitoramento {

    /**
     * Publica pelo sender compartilhado e mapper proprios da fila de saida.
     *
     * @param resultado resultado proprio com identidade e campos preservados
     * @return conclusao apos confirmacao do broker; falha se a publicacao nao concluir
     */
    Uni<Void> executar(ResultadoMonitoramento resultado);
}
