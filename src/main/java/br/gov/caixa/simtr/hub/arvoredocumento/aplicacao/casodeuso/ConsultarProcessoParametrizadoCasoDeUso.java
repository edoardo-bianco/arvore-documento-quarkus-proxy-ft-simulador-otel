package br.gov.caixa.simtr.hub.arvoredocumento.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.arvoredocumento.aplicacao.porta.entrada.ConsultarProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.aplicacao.porta.saida.ObterProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.IdentificadorNegocialProcesso;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.ProcessoParametrizado;
import io.smallrye.mutiny.Uni;

public final class ConsultarProcessoParametrizadoCasoDeUso
        implements ConsultarProcessoParametrizado {

    private final ObterProcessoParametrizado portaSaida;

    public ConsultarProcessoParametrizadoCasoDeUso(ObterProcessoParametrizado portaSaida) {
        this.portaSaida = portaSaida;
    }

    @Override
    public Uni<ProcessoParametrizado> executar(IdentificadorNegocialProcesso identificador) {
        return portaSaida.obter(identificador);
    }
}
