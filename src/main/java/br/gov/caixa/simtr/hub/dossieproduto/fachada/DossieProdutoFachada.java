package br.gov.caixa.simtr.hub.dossieproduto.fachada;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoDocumentoCriadoVo;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoDocumentoInclusaoVo;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoValidacaoNegocialVo;
import br.gov.caixa.simtr.hub.dossieproduto.servico.DossieProdutoService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DossieProdutoFachada {

    private final DossieProdutoService dossieProdutoService;

    @Inject
    public DossieProdutoFachada(DossieProdutoService dossieProdutoService) {
        this.dossieProdutoService = dossieProdutoService;
    }

    public Uni<DossieProdutoDocumentoCriadoVo> incluirDocumentoDossieProduto(
            Long id,
            DossieProdutoDocumentoInclusaoVo requisicao
    ) {
        return dossieProdutoService.incluirDocumentoDossieProduto(id, requisicao);
    }

    public Uni<Void> registrarValidacaoNegocialDossieProduto(
            Long id,
            DossieProdutoValidacaoNegocialVo requisicao
    ) {
        return dossieProdutoService.registrarValidacaoNegocialDossieProduto(id, requisicao);
    }
}
