package br.gov.caixa.simtr.hub.conformidade.aplicacao.workflow;

import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoAnaliseConformidade;

public record ContextoAnaliseConformidadeFlow(
        String texto,
        Checklist checklist,
        ResultadoAnaliseConformidade resultado) {
}
