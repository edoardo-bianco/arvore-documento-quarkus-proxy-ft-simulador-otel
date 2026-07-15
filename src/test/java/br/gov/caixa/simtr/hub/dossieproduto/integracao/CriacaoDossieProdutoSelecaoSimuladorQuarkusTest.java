package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoCriacaoDossieProduto;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class CriacaoDossieProdutoSelecaoSimuladorQuarkusTest {

    @Inject
    SolicitarCriacaoDossieProduto portaSaida;

    @Test
    void selecionaSimuladorSemAmbiguidadeCdiELêFixturePropria() {
        var comando = new ComandoCriacaoDossieProduto(100L, 200L, 300L, List.of());

        assertEquals(1L, portaSaida.criar(comando)
                .await().indefinitely().identificadorDossieProduto());
    }
}
