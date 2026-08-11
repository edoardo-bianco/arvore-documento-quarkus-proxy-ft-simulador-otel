package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ProdutoContratadoDossieProduto;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class ProdutoDossieProdutoSelecaoSimuladorQuarkusTest {

    @Inject
    SolicitarAlteracaoProdutosContratadosDossieProduto portaSaida;

    @Test
    void selecionaSimuladorSemAmbiguidadeCdiELeFixturePropriaSemRedeExterna() {
        var comando = new ComandoAlteracaoProdutosContratadosDossieProduto(
                123L,
                List.of(new ProdutoContratadoDossieProduto(100, 200, false)));

        assertNull(portaSaida.alterar(comando).await().indefinitely());
    }
}
