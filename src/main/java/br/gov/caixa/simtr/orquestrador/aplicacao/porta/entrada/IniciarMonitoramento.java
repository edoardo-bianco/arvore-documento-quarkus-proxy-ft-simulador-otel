package br.gov.caixa.simtr.orquestrador.aplicacao.porta.entrada;

import br.gov.caixa.simtr.orquestrador.dominio.modelo.MonitoramentoIniciado;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.SolicitacaoMonitoramento;
import io.smallrye.mutiny.Uni;

/** Inicia a tentativa 1 com IDs e instante do servidor, parametros por ACL e publicacao pela porta. */
public interface IniciarMonitoramento {

    /** @param solicitacao identificadores validados pela borda REST
     * @return IDs gerados, emitidos somente apos confirmacao; a mesma invocacao preserva seu resultado */
    Uni<MonitoramentoIniciado> executar(SolicitacaoMonitoramento solicitacao);
}
