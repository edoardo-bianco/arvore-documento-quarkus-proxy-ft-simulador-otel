package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAtualizacaoFormularioDossieProduto;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class FormularioDossieProdutoSelecaoSimuladorQuarkusTest {

    @Inject
    SolicitarAtualizacaoFormularioDossieProduto portaSaida;

    @Test
    void selecionaSimuladorSemAmbiguidadeCdiEPreservaFixtureEIdInformado() {
        var resultadoFixture = portaSaida.atualizar(
                new ComandoAtualizacaoFormularioDossieProduto(null, List.of()))
                .await().indefinitely();
        var resultadoComId = portaSaida.atualizar(
                new ComandoAtualizacaoFormularioDossieProduto(321L, List.of()))
                .await().indefinitely();

        assertEquals(1L, resultadoFixture.identificadorDossieProduto());
        assertEquals(321L, resultadoComId.identificadorDossieProduto());
    }
}
