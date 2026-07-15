package br.gov.caixa.simtr.hub.arvoredocumento.aplicacao.porta.entrada;

import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.IdentificadorNegocialProcesso;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.ProcessoParametrizado;
import io.smallrye.mutiny.Uni;

public interface ConsultarProcessoParametrizado {

    Uni<ProcessoParametrizado> executar(IdentificadorNegocialProcesso identificador);
}
