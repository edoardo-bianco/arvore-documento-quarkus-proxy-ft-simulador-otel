package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.DocumentoMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.DocumentoSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarInclusaoDocumentoDossieProduto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class DocumentoDossieProdutoPortasProducer {

    @Produces
    @ApplicationScoped
    SolicitarInclusaoDocumentoDossieProduto portaSaida(
            @DocumentoMtr SolicitarInclusaoDocumentoDossieProduto mtr,
            @DocumentoSimulador SolicitarInclusaoDocumentoDossieProduto simulador,
            @ConfigProperty(name = "simtr-hub.simulador.dossie-produto.habilitado")
            boolean simuladorHabilitado
    ) {
        return simuladorHabilitado ? simulador : mtr;
    }
}
