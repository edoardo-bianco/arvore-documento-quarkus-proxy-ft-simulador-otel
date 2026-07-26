package br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise;

import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import java.util.List;

public record RevisaoHumanaConformidade(
        String observacao,
        List<ResultadoApontamentoConformidade> apontamentos) {

    public RevisaoHumanaConformidade {
        if (apontamentos == null
                || apontamentos.isEmpty()
                || apontamentos.stream().anyMatch(java.util.Objects::isNull)) {
            throw FalhaAnaliseConformidade.revisaoInconsistente(
                    "A revisão deve conter todos os apontamentos");
        }
        apontamentos = List.copyOf(apontamentos);
    }
}
