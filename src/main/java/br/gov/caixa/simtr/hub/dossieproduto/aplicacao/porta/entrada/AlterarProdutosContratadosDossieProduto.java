package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAlteracaoProdutosContratadosDossieProduto;
import io.smallrye.mutiny.Uni;

public interface AlterarProdutosContratadosDossieProduto {

    Uni<Void> executar(ComandoAlteracaoProdutosContratadosDossieProduto comando);
}
