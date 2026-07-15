package br.gov.caixa.simtr.hub.conformidade.adaptador.configuracao;

import br.gov.caixa.simtr.hub.conformidade.adaptador.configuracao.qualificador.ChecklistMtr;
import br.gov.caixa.simtr.hub.conformidade.adaptador.configuracao.qualificador.ChecklistSimulador;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ObterChecklist;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ChecklistPortasProducer {

    @Produces
    @ApplicationScoped
    ObterChecklist portaSaida(
            @ChecklistMtr ObterChecklist mtr,
            @ChecklistSimulador ObterChecklist simulador,
            @ConfigProperty(
                    name = "simtr-hub.simulador.parametrizacao-checklist.habilitado",
                    defaultValue = "false"
            ) boolean simuladorHabilitado
    ) {
        return simuladorHabilitado ? simulador : mtr;
    }
}
