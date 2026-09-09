package br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida;

import br.gov.caixa.simtr.orquestrador.dominio.modelo.TentativaMonitoramento;
import io.smallrye.mutiny.Uni;

/** Publica a tentativa inicial pelo sender da fila de entrada, sem consumir mensagens. */
public interface PublicarTentativaMonitoramento {

    /** @param tentativa identidade e parametros ja definidos pelo orquestrador
     * @return conclusao apos confirmacao do broker, compartilhada por invocacao; falha sem falso sucesso */
    Uni<Void> executar(TentativaMonitoramento tentativa);
}
