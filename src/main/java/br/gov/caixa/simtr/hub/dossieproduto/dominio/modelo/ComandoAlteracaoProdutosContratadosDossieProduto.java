package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

import java.util.List;

public record ComandoAlteracaoProdutosContratadosDossieProduto(
        Long identificadorDossieProduto,
        List<ProdutoContratadoDossieProduto> produtos
) {
}
