package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAlteracaoProdutosContratadosDossieProduto;
import io.smallrye.mutiny.Uni;

public interface SolicitarAlteracaoProdutosContratadosDossieProduto {

    Uni<Void> alterar(ComandoAlteracaoProdutosContratadosDossieProduto comando);
}
