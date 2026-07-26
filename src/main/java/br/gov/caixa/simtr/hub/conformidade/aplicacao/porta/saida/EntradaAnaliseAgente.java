package br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida;

import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;

public record EntradaAnaliseAgente(String texto, Checklist checklist) {

    public EntradaAnaliseAgente {
        if (texto == null || texto.isBlank()) {
            throw FalhaAnaliseConformidade.solicitacaoInvalida(
                    "O texto da análise é obrigatório");
        }
        if (checklist == null) {
            throw FalhaAnaliseConformidade.checklistInvalido(
                    "O checklist da análise é obrigatório");
        }
    }
}
