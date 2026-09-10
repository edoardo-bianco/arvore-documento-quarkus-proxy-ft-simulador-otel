package br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida;

import br.gov.caixa.simtr.monitoramento.dominio.modelo.ReagendamentoMonitoramento;
import io.smallrye.mutiny.Uni;

/** Agenda a proxima tentativa e conclui a atual; a associacao da entrega pertence a borda. */
public interface ReagendarTentativaMonitoramento {
    /**
     * Preserva IDs, inicio, limite e versao na transacao da fila de entrada.
     * @param reagendamento proxima tentativa e horario decididos, sem contexto Azure
     * @return conclusao apos commit confirmado; rollback ou resultado incerto propagam falha
     */
    Uni<Void> executar(ReagendamentoMonitoramento reagendamento);
}
