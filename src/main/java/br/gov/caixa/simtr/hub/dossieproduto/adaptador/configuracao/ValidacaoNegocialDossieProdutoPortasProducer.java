package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ValidacaoNegocialMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ValidacaoNegocialSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarRegistroValidacaoNegocialDossieProduto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ValidacaoNegocialDossieProdutoPortasProducer {

    @Produces
    @ApplicationScoped
    SolicitarRegistroValidacaoNegocialDossieProduto portaSaida(
            @ValidacaoNegocialMtr SolicitarRegistroValidacaoNegocialDossieProduto mtr,
            @ValidacaoNegocialSimulador
            SolicitarRegistroValidacaoNegocialDossieProduto simulador,
            @ConfigProperty(name = "simtr-hub.simulador.dossie-produto.habilitado")
            boolean simuladorHabilitado
    ) {
        return simuladorHabilitado ? simulador : mtr;
    }
}
