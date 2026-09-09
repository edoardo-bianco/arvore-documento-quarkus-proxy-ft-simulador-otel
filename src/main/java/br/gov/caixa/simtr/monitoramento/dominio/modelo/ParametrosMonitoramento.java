package br.gov.caixa.simtr.monitoramento.dominio.modelo;

import java.time.Instant;

/**
 * Declara limite e versão dos parâmetros iniciais pertencentes ao monitoramento.
 *
 * <p>O cálculo pertence à política do monitoramento; a ACL traduz para o tipo do orquestrador.
 * A colaboração está implementada em 6.1. Não compartilhar este tipo entre componentes.
 * O limite é calculado a partir do instante fornecido e deve permanecer estável no reagendamento.
 *
 * @param limiteEm instante limite calculado pela política do monitoramento, preservado na tradução
 * @param politicaMonitoramentoVersao versão da política usada no cálculo, sem substituição silenciosa
 * @see br.gov.caixa.simtr.monitoramento.aplicacao.porta.entrada.PrepararMonitoramento
 */
public record ParametrosMonitoramento(Instant limiteEm, String politicaMonitoramentoVersao) {
}
