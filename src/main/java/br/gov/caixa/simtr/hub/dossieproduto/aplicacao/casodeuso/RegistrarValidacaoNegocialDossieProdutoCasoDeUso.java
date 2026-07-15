package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.RegistrarValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarRegistroValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoRegistroValidacaoNegocialDossieProduto;
import io.smallrye.mutiny.Uni;

public final class RegistrarValidacaoNegocialDossieProdutoCasoDeUso
        implements RegistrarValidacaoNegocialDossieProduto {

    private final SolicitarRegistroValidacaoNegocialDossieProduto portaSaida;

    public RegistrarValidacaoNegocialDossieProdutoCasoDeUso(
            SolicitarRegistroValidacaoNegocialDossieProduto portaSaida) {
        this.portaSaida = portaSaida;
    }

    @Override
    public Uni<Void> executar(ComandoRegistroValidacaoNegocialDossieProduto comando) {
        return portaSaida.registrar(comando);
    }
}
