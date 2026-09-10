package br.gov.caixa.simtr.orquestrador.aplicacao.porta.entrada;

import br.gov.caixa.simtr.orquestrador.dominio.modelo.ResultadoMonitoramento;
import io.smallrye.mutiny.Uni;

/** Recebe o resultado validado pela borda e solicita seu registro por porta de saida. */
public interface ReceberResultadoMonitoramento {
    /**
     * @param resultado modelo semantico proprio, ja mapeado e validado pela borda
     * @return conclusao da porta de registro, ou sua falha; nao confirma persistencia do log
     */
    Uni<Void> executar(ResultadoMonitoramento resultado);
}
