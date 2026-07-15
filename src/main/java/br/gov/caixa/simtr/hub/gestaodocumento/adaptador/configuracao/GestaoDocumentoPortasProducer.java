package br.gov.caixa.simtr.hub.gestaodocumento.adaptador.configuracao;

import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.configuracao.qualificador.GestaoDocumentoMtr;
import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.configuracao.qualificador.GestaoDocumentoSimulador;
import br.gov.caixa.simtr.hub.gestaodocumento.aplicacao.porta.saida.SolicitarCredencialContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class GestaoDocumentoPortasProducer {

    @Produces
    @ApplicationScoped
    SolicitarCredencialContainer portaSaida(
            @GestaoDocumentoMtr SolicitarCredencialContainer mtr,
            @GestaoDocumentoSimulador SolicitarCredencialContainer simulador,
            @ConfigProperty(
                    name = "simtr-hub.simulador.gestao-documento.habilitado",
                    defaultValue = "false"
            ) boolean simuladorHabilitado
    ) {
        return simuladorHabilitado ? simulador : mtr;
    }
}
