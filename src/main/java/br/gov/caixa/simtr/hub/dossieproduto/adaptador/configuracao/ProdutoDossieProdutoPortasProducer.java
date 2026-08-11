package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ProdutoMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ProdutoSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAlteracaoProdutosContratadosDossieProduto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class ProdutoDossieProdutoPortasProducer {

    @Produces
    @ApplicationScoped
    SolicitarAlteracaoProdutosContratadosDossieProduto portaSaida(
            @ProdutoMtr SolicitarAlteracaoProdutosContratadosDossieProduto mtr,
            @ProdutoSimulador SolicitarAlteracaoProdutosContratadosDossieProduto simulador,
            @ConfigProperty(name = "simtr-hub.simulador.dossie-produto.habilitado")
            boolean simuladorHabilitado
    ) {
        return simuladorHabilitado ? simulador : mtr;
    }
}
