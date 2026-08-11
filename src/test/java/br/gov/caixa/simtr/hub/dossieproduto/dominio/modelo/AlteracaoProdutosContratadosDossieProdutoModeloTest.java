package br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

class AlteracaoProdutosContratadosDossieProdutoModeloTest {

    @Test
    void representaComandoComProdutosContratados() {
        var produto = new ProdutoContratadoDossieProduto(1, 2, true);
        var produtos = List.of(produto);
        var comando = new ComandoAlteracaoProdutosContratadosDossieProduto(123L, produtos);

        assertEquals(123L, comando.identificadorDossieProduto());
        assertEquals(produtos, comando.produtos());
        assertEquals(1, produto.codigoOperacao());
        assertEquals(2, produto.codigoModalidade());
        assertEquals(true, produto.excluir());
    }

    @Test
    void preservaExclusaoAusente() {
        var produto = new ProdutoContratadoDossieProduto(1, 2, null);

        assertNull(produto.excluir());
    }
}
