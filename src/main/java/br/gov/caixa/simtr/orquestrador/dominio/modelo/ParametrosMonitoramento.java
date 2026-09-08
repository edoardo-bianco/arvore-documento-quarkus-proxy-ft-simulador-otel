package br.gov.caixa.simtr.orquestrador.dominio.modelo;

import java.time.Instant;

/**
 * Declara limite e versão dos parâmetros iniciais pertencentes ao orquestrador.
 *
 * <p><strong>Estado:</strong> os dados do record estão declarados; cálculo e tradução por ACL
 * permanecem no item 6.1. Não duplicar a política nem compartilhar este tipo entre componentes.
 * O limite é calculado a partir do instante fornecido e deve permanecer estável no reagendamento.
 *
 * @param limiteEm instante limite calculado pela política do monitoramento, preservado na tradução
 * @param politicaMonitoramentoVersao versão da política usada no cálculo, sem substituição silenciosa
 * @see br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.ObterParametrosMonitoramento
 */
public record ParametrosMonitoramento(Instant limiteEm, String politicaMonitoramentoVersao) {
}
