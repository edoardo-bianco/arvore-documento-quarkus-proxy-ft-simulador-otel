package br.gov.caixa.simtr.monitoramento.dominio.modelo;

/**
 * Situacao minima consultada da pre-validacao, com origem explicita.
 *
 * @param situacao texto original nao vazio; a elegibilidade pertence ao processamento
 * @param simulada indica se a consulta usou dados simulados
 */
public record PreValidacaoConsultada(String situacao, boolean simulada) {

    public PreValidacaoConsultada {
        if (situacao == null || situacao.isBlank()) {
            throw new IllegalArgumentException("Situacao da pre-validacao obrigatoria.");
        }
    }
}
