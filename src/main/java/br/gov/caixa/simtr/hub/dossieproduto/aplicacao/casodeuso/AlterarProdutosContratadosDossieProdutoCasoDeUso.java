package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.AlterarProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAlteracaoProdutosContratadosDossieProduto;
import io.smallrye.mutiny.Uni;

public final class AlterarProdutosContratadosDossieProdutoCasoDeUso
        implements AlterarProdutosContratadosDossieProduto {

    private final SolicitarAlteracaoProdutosContratadosDossieProduto portaSaida;

    public AlterarProdutosContratadosDossieProdutoCasoDeUso(
            SolicitarAlteracaoProdutosContratadosDossieProduto portaSaida
    ) {
        this.portaSaida = portaSaida;
    }

    @Override
    public Uni<Void> executar(ComandoAlteracaoProdutosContratadosDossieProduto comando) {
        return portaSaida.alterar(comando);
    }
}
