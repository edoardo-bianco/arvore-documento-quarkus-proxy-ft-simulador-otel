package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ConsultaDocumentosDossieProdutoMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ConsultaDocumentosDossieProdutoSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.ObterDocumentosDossieProduto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ConsultaDocumentosDossieProdutoPortasProducer {

    @Produces
    @ApplicationScoped
    ObterDocumentosDossieProduto portaSaida(
            @ConsultaDocumentosDossieProdutoMtr
            ObterDocumentosDossieProduto mtr,
            @ConsultaDocumentosDossieProdutoSimulador
            ObterDocumentosDossieProduto simulador,
            @ConfigProperty(name = "simtr-hub.simulador.dossie-produto.habilitado")
            boolean simuladorHabilitado
    ) {
        return simuladorHabilitado ? simulador : mtr;
    }
}
