package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarRegistroValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoRegistroValidacaoNegocialDossieProduto;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class ValidacaoNegocialDossieProdutoSelecaoSimuladorQuarkusTest {

    @Inject
    SolicitarRegistroValidacaoNegocialDossieProduto portaSaida;

    @Test
    void selecionaSimuladorSemAmbiguidadeCdiELeFixturePropria() {
        var comando = new ComandoRegistroValidacaoNegocialDossieProduto(
                123L, null, null);

        Void resultado = portaSaida.registrar(comando).await().indefinitely();

        assertNull(resultado);
    }
}
