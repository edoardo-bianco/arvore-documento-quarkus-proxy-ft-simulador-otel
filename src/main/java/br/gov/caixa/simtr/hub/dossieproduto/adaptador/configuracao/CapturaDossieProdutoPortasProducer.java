package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.CapturaMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.CapturaSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarCapturaDossieProduto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class CapturaDossieProdutoPortasProducer {

    @Produces
    @ApplicationScoped
    SolicitarCapturaDossieProduto portaSaida(
            @CapturaMtr SolicitarCapturaDossieProduto mtr,
            @CapturaSimulador SolicitarCapturaDossieProduto simulador,
            @ConfigProperty(name = "simtr-hub.simulador.dossie-produto.habilitado")
            boolean simuladorHabilitado
    ) {
        return simuladorHabilitado ? simulador : mtr;
    }
}
