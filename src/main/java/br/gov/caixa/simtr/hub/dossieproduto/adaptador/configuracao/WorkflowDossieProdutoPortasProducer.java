package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.WorkflowMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.WorkflowSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.AvancarWorkflowDossieProduto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class WorkflowDossieProdutoPortasProducer {

    @Produces
    @ApplicationScoped
    AvancarWorkflowDossieProduto portaSaida(
            @WorkflowMtr AvancarWorkflowDossieProduto mtr,
            @WorkflowSimulador AvancarWorkflowDossieProduto simulador,
            @ConfigProperty(name = "simtr-hub.simulador.dossie-produto.habilitado")
            boolean simuladorHabilitado
    ) {
        return simuladorHabilitado ? simulador : mtr;
    }
}
