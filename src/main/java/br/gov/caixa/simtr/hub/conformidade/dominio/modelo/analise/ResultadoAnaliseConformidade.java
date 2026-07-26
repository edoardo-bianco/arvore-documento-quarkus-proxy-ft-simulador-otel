package br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise;

import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import java.util.List;

public record ResultadoAnaliseConformidade(
        Long identificadorChecklist,
        Integer versaoChecklist,
        String nomeChecklist,
        String resumo,
        List<ResultadoApontamentoConformidade> apontamentos,
        OrigemResultado origem) {

    public ResultadoAnaliseConformidade {
        if (identificadorChecklist == null || identificadorChecklist <= 0) {
            throw FalhaAnaliseConformidade.resultadoInvalido(
                    "O identificador do checklist deve ser positivo");
        }
        if (versaoChecklist == null || versaoChecklist <= 0) {
            throw FalhaAnaliseConformidade.resultadoInvalido(
                    "A versão do checklist deve ser positiva");
        }
        if (nomeChecklist == null || nomeChecklist.isBlank()) {
            throw FalhaAnaliseConformidade.resultadoInvalido(
                    "O nome do checklist é obrigatório");
        }
        if (resumo == null || resumo.isBlank()) {
            throw FalhaAnaliseConformidade.resultadoInvalido(
                    "O resumo da análise é obrigatório");
        }
        if (apontamentos == null
                || apontamentos.isEmpty()
                || apontamentos.stream().anyMatch(java.util.Objects::isNull)) {
            throw FalhaAnaliseConformidade.resultadoInvalido(
                    "A análise deve conter apontamentos");
        }
        if (origem == null) {
            throw FalhaAnaliseConformidade.resultadoInvalido(
                    "A origem do resultado é obrigatória");
        }
        apontamentos = List.copyOf(apontamentos);
    }
}
