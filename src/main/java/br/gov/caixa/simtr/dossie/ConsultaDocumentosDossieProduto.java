package br.gov.caixa.simtr.dossie;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.CriteriosConsultaDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DocumentoDossieProdutoConsultado;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class ConsultaDocumentosDossieProduto {

    private final ConsultarDocumentosDossieProduto porta;

    @Inject
    public ConsultaDocumentosDossieProduto(ConsultarDocumentosDossieProduto porta) {
        this.porta = porta;
    }

    public Uni<List<DocumentoDossieProdutoConsultado>> consultar(
            CriteriosConsultaDocumentosDossieProduto criterios
    ) {
        return porta.executar(criterios);
    }
}
