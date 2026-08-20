package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.CriteriosConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DocumentoDossieProdutoConsultado;
import io.smallrye.mutiny.Uni;

import java.util.List;

public interface ObterDocumentosDossieProduto {

    Uni<List<DocumentoDossieProdutoConsultado>> obter(
            CriteriosConsultaDocumentosDossieProduto criterios);
}
