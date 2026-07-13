package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.IncluirDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoInclusaoDocumentoDossieProduto;
import io.smallrye.mutiny.Uni;

public final class IncluirDocumentoDossieProdutoCasoDeUso
        implements IncluirDocumentoDossieProduto {

    private final SolicitarInclusaoDocumentoDossieProduto portaSaida;

    public IncluirDocumentoDossieProdutoCasoDeUso(
            SolicitarInclusaoDocumentoDossieProduto portaSaida) {
        this.portaSaida = portaSaida;
    }

    @Override
    public Uni<ResultadoInclusaoDocumentoDossieProduto> executar(
            ComandoInclusaoDocumentoDossieProduto comando) {
        return portaSaida.incluir(comando);
    }
}
