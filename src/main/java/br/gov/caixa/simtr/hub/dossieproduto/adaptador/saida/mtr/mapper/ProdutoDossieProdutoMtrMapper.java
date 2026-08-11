package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper;

import java.util.List;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.produto.ProdutoDossieProdutoMtrRequest;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ProdutoContratadoDossieProduto;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProdutoDossieProdutoMtrMapper {

    @SuppressWarnings("java:S1168") // Null preserva comando/corpo ausente; lista vazia é válida.
    public List<ProdutoDossieProdutoMtrRequest> paraMtr(
            ComandoAlteracaoProdutosContratadosDossieProduto comando
    ) {
        if (comando == null || comando.produtos() == null) {
            return null;
        }
        return comando.produtos().stream()
                .map(ProdutoDossieProdutoMtrMapper::produto)
                .toList();
    }

    private static ProdutoDossieProdutoMtrRequest produto(
            ProdutoContratadoDossieProduto produto
    ) {
        if (produto == null) {
            return null;
        }
        return new ProdutoDossieProdutoMtrRequest(
                produto.codigoOperacao(),
                produto.codigoModalidade(),
                produto.excluir());
    }
}
