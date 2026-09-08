package br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida;

import br.gov.caixa.simtr.monitoramento.dominio.modelo.ReagendamentoMonitoramento;
import io.smallrye.mutiny.Uni;

/**
 * Reagenda na fila de entrada a próxima tentativa decidida pelo monitoramento.
 *
 * <p><strong>Estado:</strong> porta declarada, sem implementação conectada.
 * Completar no item 8.1 do checklist; modelos ainda vazios permanecem pendentes.
 *
 * <p><strong>Implementação prevista:</strong> Implementar no MonitoramentoReagendamentoAdapter o envio agendado para q.prevalidacao.monitoramento-mtr.in e a conclusão da entrega atual na transação de entidade única. Preservar IDs, início, limite e versão; a política define contador e intervalo.
 *
 * <p><strong>Verificação prevista:</strong> Provar API/emulador, commit, rollback e resultado incerto antes de alegar atomicidade. Falha técnica/redelivery não incrementa tentativa funcional; contexto Azure não atravessa esta porta.
 *
 * Consultar {@code doc/guias/guia-service-bus-amqp-dossie.md} e
 * {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 */
public interface ReagendarTentativaMonitoramento {

    /**
     * Reagenda na fila de entrada a próxima tentativa decidida pelo monitoramento.
     *
     * <p>Comportamento a implementar no item 8.1: Implementar no MonitoramentoReagendamentoAdapter o envio agendado para q.prevalidacao.monitoramento-mtr.in e a conclusão da entrega atual na transação de entidade única. Preservar IDs, início, limite e versão; a política define contador e intervalo.
     *
     * @param reagendamento intenção semântica com dados a detalhar no item 8.1, sem contexto Azure
     * @return conclusão sem item de dados após o efeito transacional confirmado; falha se houver rollback ou ausência de confirmação
     */
    Uni<Void> executar(ReagendamentoMonitoramento reagendamento);
}
