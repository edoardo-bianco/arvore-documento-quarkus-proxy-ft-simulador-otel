package br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida;

import br.gov.caixa.simtr.orquestrador.dominio.modelo.ResultadoMonitoramento;
import io.smallrye.mutiny.Uni;

/**
 * Registra o resultado consumido da saída e encerra o demonstrador.
 *
 * <p><strong>Estado:</strong> porta declarada, sem implementação conectada.
 * Completar no item 9.1 do checklist; modelos ainda vazios permanecem pendentes.
 *
 * <p><strong>Implementação prevista:</strong> Implementar no ResultadoMonitoramentoLogAdapter o evento aprovado orquestrador.monitoramento-dossie.resultado.registrado, correlacionando monitoramentoId/orquestracaoId. O registro é o efeito final deste recorte, sem persistência ou avanço de workflow.
 *
 * <p><strong>Verificação prevista:</strong> Provar campos e correlação no log real, ausência de payload/segredos e propagação de falha. Preservar logs/erros do Hub e manter settlement no listener.
 *
 * Consultar {@code doc/guias/guia-service-bus-amqp-dossie.md} e
 * {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 */
public interface RegistrarResultadoMonitoramento {

    /**
     * Registra o resultado consumido da saída e encerra o demonstrador.
     *
     * <p>Comportamento a implementar no item 9.1: Implementar no ResultadoMonitoramentoLogAdapter o evento aprovado orquestrador.monitoramento-dossie.resultado.registrado, correlacionando monitoramentoId/orquestracaoId. O registro é o efeito final deste recorte, sem persistência ou avanço de workflow.
     *
     * @param resultado resultado semântico próprio, independente do DTO consumido
     * @return conclusão sem item de dados após o registro; falha se a operação não concluir
     */
    Uni<Void> executar(ResultadoMonitoramento resultado);
}
