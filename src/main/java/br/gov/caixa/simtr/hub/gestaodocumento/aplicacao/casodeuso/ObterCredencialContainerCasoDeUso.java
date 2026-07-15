package br.gov.caixa.simtr.hub.gestaodocumento.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.gestaodocumento.aplicacao.porta.entrada.ObterCredencialContainer;
import br.gov.caixa.simtr.hub.gestaodocumento.aplicacao.porta.saida.SolicitarCredencialContainer;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.modelo.CredencialContainer;
import io.smallrye.mutiny.Uni;

public final class ObterCredencialContainerCasoDeUso implements ObterCredencialContainer {

    private final SolicitarCredencialContainer portaSaida;

    public ObterCredencialContainerCasoDeUso(SolicitarCredencialContainer portaSaida) {
        this.portaSaida = portaSaida;
    }

    @Override
    public Uni<CredencialContainer> executar() {
        return portaSaida.obter();
    }
}
