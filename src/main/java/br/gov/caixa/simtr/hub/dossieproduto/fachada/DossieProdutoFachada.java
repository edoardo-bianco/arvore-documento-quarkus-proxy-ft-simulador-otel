package br.gov.caixa.simtr.hub.dossieproduto.fachada;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoCriadoVo;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoDocumentoCriadoVo;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoDocumentoInclusaoVo;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoFormularioVo;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoValidacaoNegocialVo;
import br.gov.caixa.simtr.hub.dossieproduto.servico.DossieProdutoService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class DossieProdutoFachada {

    private final DossieProdutoService dossieProdutoService;

    @Inject
    public DossieProdutoFachada(DossieProdutoService dossieProdutoService) {
        this.dossieProdutoService = dossieProdutoService;
    }

    public Uni<DossieProdutoCriadoVo> atualizarFormularioDossieProduto(
            Long id,
            List<DossieProdutoFormularioVo> requisicao
    ) {
        return dossieProdutoService.atualizarFormularioDossieProduto(id, requisicao);
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
