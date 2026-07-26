package br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise;

import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;

public record ResultadoApontamentoConformidade(
        Long identificadorApontamento,
        String nomeApontamento,
        ParecerConformidade parecer,
        String justificativa,
        String evidencia,
        Double confianca) {

    public ResultadoApontamentoConformidade {
        if (identificadorApontamento == null || identificadorApontamento <= 0) {
            throw FalhaAnaliseConformidade.resultadoInvalido(
                    "O identificador do apontamento deve ser positivo");
        }
        if (nomeApontamento == null || nomeApontamento.isBlank()) {
            throw FalhaAnaliseConformidade.resultadoInvalido(
                    "O nome do apontamento é obrigatório");
        }
        if (parecer == null) {
            throw FalhaAnaliseConformidade.resultadoInvalido(
                    "O parecer do apontamento é obrigatório");
        }
        if (justificativa == null || justificativa.isBlank()) {
            throw FalhaAnaliseConformidade.resultadoInvalido(
                    "A justificativa do apontamento é obrigatória");
        }
        if (confianca == null
                || !Double.isFinite(confianca)
                || confianca < 0.0d
                || confianca > 1.0d) {
            throw FalhaAnaliseConformidade.resultadoInvalido(
                    "A confiança deve estar entre 0 e 1");
        }
    }
}
