package br.gov.caixa.simtr.hub.arvoredocumento.adaptador.configuracao;

import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.configuracao.qualificador.ProcessoMtr;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.configuracao.qualificador.ProcessoSimulador;
import br.gov.caixa.simtr.hub.arvoredocumento.aplicacao.porta.saida.ObterProcessoParametrizado;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ProcessoParametrizadoPortasProducer {

    @Produces
    @ApplicationScoped
    ObterProcessoParametrizado portaSaida(
            @ProcessoMtr ObterProcessoParametrizado mtr,
            @ProcessoSimulador ObterProcessoParametrizado simulador,
            @ConfigProperty(
                    name = "simtr-hub.simulador.parametrizacao-processo.habilitado",
                    defaultValue = "false"
            ) boolean simuladorHabilitado
    ) {
        return simuladorHabilitado ? simulador : mtr;
    }
}
