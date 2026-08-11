package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.AlterarProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ProdutoContratadoDossieProduto;
import io.smallrye.mutiny.Uni;

class AlterarProdutosContratadosDossieProdutoCasoDeUsoTest {

    @Test
    void delegaOMesmoComandoParaPortaDeSaidaERetornaResultadoVazio() {
        var produtos = List.of(new ProdutoContratadoDossieProduto(1, 2, false));
        var comando = new ComandoAlteracaoProdutosContratadosDossieProduto(123L, produtos);
        var portaSaida = new FakePortaSaida(Uni.createFrom().voidItem());
        AlterarProdutosContratadosDossieProduto casoDeUso =
                new AlterarProdutosContratadosDossieProdutoCasoDeUso(portaSaida);

        var resultado = casoDeUso.executar(comando).await().indefinitely();

        assertNull(resultado);
        assertSame(comando, portaSaida.comandoRecebido);
        assertSame(produtos, portaSaida.comandoRecebido.produtos());
    }

    @Test
    void preservaFalhaDaPortaDeSaida() {
        var falha = new IllegalStateException("falha interna");
        AlterarProdutosContratadosDossieProduto casoDeUso =
                new AlterarProdutosContratadosDossieProdutoCasoDeUso(
                        new FakePortaSaida(Uni.createFrom().failure(falha)));
        var comando = new ComandoAlteracaoProdutosContratadosDossieProduto(123L, List.of());

        var espera = casoDeUso.executar(comando).await();
        var observada = assertThrows(
                IllegalStateException.class,
                espera::indefinitely);

        assertSame(falha, observada);
    }

    private static final class FakePortaSaida
            implements SolicitarAlteracaoProdutosContratadosDossieProduto {

        private final Uni<Void> resultado;
        private ComandoAlteracaoProdutosContratadosDossieProduto comandoRecebido;

        private FakePortaSaida(Uni<Void> resultado) {
            this.resultado = resultado;
        }

        @Override
        public Uni<Void> alterar(ComandoAlteracaoProdutosContratadosDossieProduto comando) {
            comandoRecebido = comando;
            return resultado;
        }
    }
}
