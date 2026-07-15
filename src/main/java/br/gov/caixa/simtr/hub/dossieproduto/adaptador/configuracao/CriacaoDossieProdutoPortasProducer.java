package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.CriacaoMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.CriacaoSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarCriacaoDossieProduto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class CriacaoDossieProdutoPortasProducer {

    @Produces
    @ApplicationScoped
    SolicitarCriacaoDossieProduto portaSaida(
            @CriacaoMtr SolicitarCriacaoDossieProduto mtr,
            @CriacaoSimulador SolicitarCriacaoDossieProduto simulador,
            @ConfigProperty(name = "simtr-hub.simulador.dossie-produto.habilitado")
            boolean simuladorHabilitado
    ) {
        return simuladorHabilitado ? simulador : mtr;
    }
}
