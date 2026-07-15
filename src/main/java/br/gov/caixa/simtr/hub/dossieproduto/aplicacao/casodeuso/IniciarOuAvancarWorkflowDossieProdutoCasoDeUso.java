package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.IniciarOuAvancarWorkflowDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.AvancarWorkflowDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoWorkflowDossieProduto;
import io.smallrye.mutiny.Uni;

public final class IniciarOuAvancarWorkflowDossieProdutoCasoDeUso
        implements IniciarOuAvancarWorkflowDossieProduto {

    private final AvancarWorkflowDossieProduto portaSaida;

    public IniciarOuAvancarWorkflowDossieProdutoCasoDeUso(
            AvancarWorkflowDossieProduto portaSaida
    ) {
        this.portaSaida = portaSaida;
    }

    @Override
    public Uni<ResultadoWorkflowDossieProduto> executar(IdentificadorDossieProduto identificador) {
        return portaSaida.avancar(identificador);
    }
}
