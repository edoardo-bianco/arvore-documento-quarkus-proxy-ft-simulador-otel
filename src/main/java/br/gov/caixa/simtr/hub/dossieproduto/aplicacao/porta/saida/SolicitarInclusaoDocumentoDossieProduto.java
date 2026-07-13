package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoInclusaoDocumentoDossieProduto;
import io.smallrye.mutiny.Uni;

public interface SolicitarInclusaoDocumentoDossieProduto {

    Uni<ResultadoInclusaoDocumentoDossieProduto> incluir(
            ComandoInclusaoDocumentoDossieProduto comando);
}
