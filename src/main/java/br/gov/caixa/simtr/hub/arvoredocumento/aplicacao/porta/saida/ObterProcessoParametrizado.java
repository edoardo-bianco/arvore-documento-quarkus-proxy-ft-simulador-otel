package br.gov.caixa.simtr.hub.arvoredocumento.aplicacao.porta.saida;

import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.IdentificadorNegocialProcesso;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.ProcessoParametrizado;
import io.smallrye.mutiny.Uni;

public interface ObterProcessoParametrizado {

    Uni<ProcessoParametrizado> obter(IdentificadorNegocialProcesso identificador);
}
