package br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida;

import br.gov.caixa.simtr.orquestrador.dominio.modelo.TentativaMonitoramento;
import io.smallrye.mutiny.Uni;

/**
 * Publica a tentativa inicial do orquestrador na fila de entrada.
 *
 * <p><strong>Estado:</strong> porta declarada, sem implementação conectada.
 * Completar no item 6.1 do checklist; modelos ainda vazios permanecem pendentes.
 *
 * <p><strong>Implementação prevista:</strong> Implementar no MonitoramentoEntradaPublisher com o sender da fila q.prevalidacao.monitoramento-mtr.in. Usar o mapper próprio já implementado e compor a confirmação assíncrona do envio, sem gerar novos IDs.
 *
 * <p><strong>Verificação prevista:</strong> Provar corpo v1 e envelope AMQP, envio único por invocação, confirmação e propagação de falha; não consumir a fila de entrada.
 *
 * Consultar {@code doc/guias/guia-service-bus-amqp-dossie.md} e
 * {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 */
public interface PublicarTentativaMonitoramento {

    /**
     * Publica a tentativa inicial do orquestrador na fila de entrada.
     *
     * <p>Comportamento a implementar no item 6.1: Implementar no MonitoramentoEntradaPublisher com o sender da fila q.prevalidacao.monitoramento-mtr.in. Usar o mapper próprio já implementado e compor a confirmação assíncrona do envio, sem gerar novos IDs.
     *
     * @param tentativa tentativa semântica do orquestrador com parâmetros e identidade já definidos
     * @return conclusão sem item de dados após confirmação do broker; falha se o envio não concluir
     */
    Uni<Void> executar(TentativaMonitoramento tentativa);
}
