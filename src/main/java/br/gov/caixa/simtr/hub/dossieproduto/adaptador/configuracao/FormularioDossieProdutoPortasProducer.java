package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.FormularioMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.FormularioSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAtualizacaoFormularioDossieProduto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class FormularioDossieProdutoPortasProducer {

    @Produces
    @ApplicationScoped
    SolicitarAtualizacaoFormularioDossieProduto portaSaida(
            @FormularioMtr SolicitarAtualizacaoFormularioDossieProduto mtr,
            @FormularioSimulador SolicitarAtualizacaoFormularioDossieProduto simulador,
            @ConfigProperty(name = "simtr-hub.simulador.dossie-produto.habilitado")
            boolean simuladorHabilitado
    ) {
        return simuladorHabilitado ? simulador : mtr;
    }
}
