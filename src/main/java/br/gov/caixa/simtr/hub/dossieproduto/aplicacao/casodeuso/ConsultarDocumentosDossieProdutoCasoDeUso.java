package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.ObterDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.CriteriosConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DocumentoDossieProdutoConsultado;
import io.smallrye.mutiny.Uni;

import java.util.List;

public final class ConsultarDocumentosDossieProdutoCasoDeUso
        implements ConsultarDocumentosDossieProduto {

    private final ObterDocumentosDossieProduto portaSaida;

    public ConsultarDocumentosDossieProdutoCasoDeUso(
            ObterDocumentosDossieProduto portaSaida
    ) {
        this.portaSaida = portaSaida;
    }

    @Override
    public Uni<List<DocumentoDossieProdutoConsultado>> executar(
            CriteriosConsultaDocumentosDossieProduto criterios
    ) {
        return portaSaida.obter(criterios);
    }
}
