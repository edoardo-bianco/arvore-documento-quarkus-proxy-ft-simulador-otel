package br.gov.caixa.simtr.monitoramento.aplicacao.porta.entrada;

import br.gov.caixa.simtr.monitoramento.dominio.modelo.DecisaoProcessamento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.TentativaMonitoramento;
import io.smallrye.mutiny.Uni;

/**
 * Processa a tentativa recebida pelo listener da fila de entrada.
 *
 * <p><strong>Estado:</strong> porta declarada, sem implementação conectada.
 * Completar no item 7.1/8.1 do checklist; modelos ainda vazios permanecem pendentes.
 *
 * <p><strong>Implementação prevista:</strong> Coordenar consulta de pré-validação; fora de EM_ANALISE_ENVIO_MTR, decidir no-op. Verificar limites para quarentena; nos demais casos consultar o Hub, produzir resultado conclusivo ou calcular reagendamento pela política. Coordenar os efeitos pelas portas de saída do monitoramento.
 *
 * <p><strong>Verificação prevista:</strong> Provar cada caminho e sua ordem, versões e falhas técnicas. Detalhar a associação entre decisão, efeitos confirmados e settlement da mesma entrega antes de implementar a transação; SDK/handles permanecem nas bordas.
 *
 * Consultar {@code doc/guias/guia-service-bus-amqp-dossie.md} e
 * {@code tasks/features/orquestrador-monitoramento-service-bus/guia-desenvolvimento.md}.
 */
public interface ProcessarTentativaMonitoramento {

    /**
     * Processa a tentativa recebida pelo listener da fila de entrada.
     *
     * <p>Comportamento a implementar no item 7.1/8.1: Coordenar consulta de pré-validação; fora de EM_ANALISE_ENVIO_MTR, decidir no-op. Verificar limites para quarentena; nos demais casos consultar o Hub, produzir resultado conclusivo ou calcular reagendamento pela política. Coordenar os efeitos pelas portas de saída do monitoramento.
     *
     * @param tentativa tentativa validada pela borda, com instante, limite e versão preservados
     * @return decisão semântica a detalhar em 7.1/8.1, distinguindo no-op, resultado e reagendamento; falha se o processamento não concluir
     */
    Uni<DecisaoProcessamento> executar(TentativaMonitoramento tentativa);
}
