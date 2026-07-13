package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.IniciarOuAvancarWorkflowDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.AvancarWorkflowDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class WorkflowDossieProdutoSelecaoSimuladorQuarkusTest {

    @Inject
    AvancarWorkflowDossieProduto portaSaida;

    @Inject
    IniciarOuAvancarWorkflowDossieProduto portaEntrada;

    @Test
    void selecionaSimuladorEProduzCasoDeUsoSemAmbiguidadeCdi() {
        assertEquals(123L, portaSaida.avancar(new IdentificadorDossieProduto(123L))
                .await().indefinitely().identificadorDossieProduto());
        assertEquals(456L, portaEntrada.executar(new IdentificadorDossieProduto(456L))
                .await().indefinitely().identificadorDossieProduto());
    }

}
