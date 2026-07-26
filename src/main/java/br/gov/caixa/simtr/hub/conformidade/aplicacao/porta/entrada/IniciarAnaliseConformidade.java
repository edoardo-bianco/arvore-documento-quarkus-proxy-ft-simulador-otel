package br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada;

import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.SolicitacaoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.VisaoAnaliseConformidade;
import io.smallrye.mutiny.Uni;

public interface IniciarAnaliseConformidade {

    Uni<VisaoAnaliseConformidade> executar(SolicitacaoAnaliseConformidade solicitacao);
}
