package br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada;

import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.VisaoAnaliseConformidade;

public interface ConsultarAnaliseConformidade {

    VisaoAnaliseConformidade executar(String instanceId);
}
