package br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada;

import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.SolicitacaoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.VisaoAnaliseConformidade;

public interface IniciarAnaliseConformidade {

    VisaoAnaliseConformidade executar(SolicitacaoAnaliseConformidade solicitacao);
}
