package br.gov.caixa.simtr.orquestrador.aplicacao.porta.entrada;

import br.gov.caixa.simtr.orquestrador.dominio.modelo.MonitoramentoIniciado;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.SolicitacaoMonitoramento;
import io.smallrye.mutiny.Uni;

/**
 * Inicia o monitoramento pela entrada REST do orquestrador.
 *
 * <p><strong>Estado:</strong> porta declarada, sem implementação conectada.
 * Completar no item 6.1 do checklist; modelos ainda vazios permanecem pendentes.
 *
 * <p><strong>Implementação prevista:</strong> Gerar os IDs e o instante inicial no servidor, obter limite/versão pela porta local de parâmetros e publicar a tentativa 1 pela porta de saída do orquestrador. O processamento seguirá pela fila de entrada.
 *
 * <p><strong>Verificação prevista:</strong> Provar IDs/parâmetros preservados e conclusão apenas após confirmação do broker; falha não pode produzir resposta de sucesso.
 *
 * Consultar {@code doc/guias/guia-service-bus-amqp-dossie.md} e
 * {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 */
public interface IniciarMonitoramento {

    /**
     * Inicia o monitoramento pela entrada REST do orquestrador.
     *
     * <p>Comportamento a implementar no item 6.1: Gerar os IDs e o instante inicial no servidor, obter limite/versão pela porta local de parâmetros e publicar a tentativa 1 pela porta de saída do orquestrador. O processamento seguirá pela fila de entrada.
     *
     * @param solicitacao identificadores recebidos pela borda REST e traduzidos para o tipo próprio
     * @return resultado com IDs gerados no servidor, emitido apenas após confirmação da publicação
     */
    Uni<MonitoramentoIniciado> executar(SolicitacaoMonitoramento solicitacao);
}
