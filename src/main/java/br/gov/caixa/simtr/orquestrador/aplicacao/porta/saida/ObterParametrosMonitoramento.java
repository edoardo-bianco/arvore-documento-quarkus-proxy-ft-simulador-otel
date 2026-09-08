package br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida;

import br.gov.caixa.simtr.orquestrador.dominio.modelo.ParametrosMonitoramento;
import java.time.Instant;

/**
 * Obtém limite e versão antes da publicação inicial do orquestrador.
 *
 * <p><strong>Estado:</strong> porta declarada, sem implementação conectada.
 * Completar no item 6.1 do checklist; modelos ainda vazios permanecem pendentes.
 *
 * <p><strong>Implementação prevista:</strong> Implementar pela ACL local que chama PrepararMonitoramento, porta pública do monitoramento. A operação é síncrona e somente calcula parâmetros em memória; não envia mensagens nem processa tentativas.
 *
 * <p><strong>Verificação prevista:</strong> Provar tradução independente e preservação do instante, limite e versão, sem acesso direto à política/configuração do fornecedor.
 *
 * Consultar {@code doc/guias/guia-service-bus-amqp-dossie.md} e
 * {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 */
public interface ObterParametrosMonitoramento {

    /**
     * Obtém limite e versão antes da publicação inicial do orquestrador.
     *
     * <p>Comportamento a implementar no item 6.1: Implementar pela ACL local que chama PrepararMonitoramento, porta pública do monitoramento. A operação é síncrona e somente calcula parâmetros em memória; não envia mensagens nem processa tentativas.
     *
     * @param iniciadoEm instante inicial gerado pelo servidor, preservado na primeira mensagem
     * @return limite e versão traduzidos para o tipo próprio do orquestrador
     */
    ParametrosMonitoramento executar(Instant iniciadoEm);
}
