package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoInclusaoDocumentoDossieProduto;
import io.smallrye.mutiny.Uni;

public interface IncluirDocumentoDossieProduto {

    Uni<ResultadoInclusaoDocumentoDossieProduto> executar(
            ComandoInclusaoDocumentoDossieProduto comando);
}
