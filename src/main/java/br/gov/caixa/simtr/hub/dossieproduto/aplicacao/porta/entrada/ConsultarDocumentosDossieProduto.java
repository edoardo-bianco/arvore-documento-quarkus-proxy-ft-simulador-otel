package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.CriteriosConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DocumentoDossieProdutoConsultado;
import io.smallrye.mutiny.Uni;

import java.util.List;

public interface ConsultarDocumentosDossieProduto {

    Uni<List<DocumentoDossieProdutoConsultado>> executar(
            CriteriosConsultaDocumentosDossieProduto criterios);
}
