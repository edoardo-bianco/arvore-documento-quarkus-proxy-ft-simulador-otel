package br.gov.caixa.simtr.monitoramento.aplicacao.porta.entrada;

import br.gov.caixa.simtr.monitoramento.dominio.modelo.ParametrosMonitoramento;
import java.time.Instant;

/**
 * Calcula parâmetros iniciais com a política do monitoramento.
 *
 * <p><strong>Estado:</strong> porta declarada, sem implementação conectada.
 * Completar no item 6.1 do checklist; modelos ainda vazios permanecem pendentes.
 *
 * <p><strong>Implementação prevista:</strong> Receber o instante do orquestrador por sua ACL e calcular limite/versão com a política já configurada. Esta preparação local não consulta fontes, não gera IDs e não substitui o processamento pela fila de entrada.
 *
 * <p><strong>Verificação prevista:</strong> Provar cálculo síncrono em memória e versão retornada, sem efeitos externos.
 *
 * Consultar {@code doc/guias/guia-service-bus-amqp-dossie.md} e
 * {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 */
public interface PrepararMonitoramento {

    /**
     * Calcula parâmetros iniciais com a política do monitoramento.
     *
     * <p>Comportamento a implementar no item 6.1: Receber o instante do orquestrador por sua ACL e calcular limite/versão com a política já configurada. Esta preparação local não consulta fontes, não gera IDs e não substitui o processamento pela fila de entrada.
     *
     * @param iniciadoEm instante inicial informado pelo consumidor, sem geração de IDs nesta operação
     * @return limite calculado e versão da política do próprio monitoramento
     */
    ParametrosMonitoramento executar(Instant iniciadoEm);
}
