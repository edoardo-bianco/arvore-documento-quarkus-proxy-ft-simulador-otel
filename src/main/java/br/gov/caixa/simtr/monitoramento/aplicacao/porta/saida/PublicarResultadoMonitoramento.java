package br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida;

import br.gov.caixa.simtr.monitoramento.dominio.modelo.ResultadoMonitoramento;
import io.smallrye.mutiny.Uni;

/**
 * Publica o resultado terminal ou a quarentena na fila de saída.
 *
 * <p><strong>Estado:</strong> porta declarada, sem implementação conectada.
 * Completar no item 7.1 do checklist; modelos ainda vazios permanecem pendentes.
 *
 * <p><strong>Implementação prevista:</strong> Implementar no MonitoramentoResultadoPublisher com o sender de q.prevalidacao.monitoramento-mtr.out e DTO/mapper próprios. Preservar situação original do MTR e situação calculada de pré-validação; não persistir transição neste recorte.
 *
 * <p><strong>Verificação prevista:</strong> Provar contrato v1, identidade, confirmação e falha. A entrada só pode receber Complete após confirmação desta publicação; a operação não representa transação entre as duas filas.
 *
 * Consultar {@code doc/guias/guia-service-bus-amqp-dossie.md} e
 * {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 */
public interface PublicarResultadoMonitoramento {

    /**
     * Publica o resultado terminal ou a quarentena na fila de saída.
     *
     * <p>Comportamento a implementar no item 7.1: Implementar no MonitoramentoResultadoPublisher com o sender de q.prevalidacao.monitoramento-mtr.out e DTO/mapper próprios. Preservar situação original do MTR e situação calculada de pré-validação; não persistir transição neste recorte.
     *
     * @param resultado resultado próprio do monitoramento; campos e invariantes serão completados em 4.1
     * @return conclusão sem item de dados após confirmação do broker; falha se o envio não concluir
     */
    Uni<Void> executar(ResultadoMonitoramento resultado);
}
