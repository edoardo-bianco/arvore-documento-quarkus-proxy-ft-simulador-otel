package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoInclusaoDocumentoDossieProduto;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class DocumentoDossieProdutoSelecaoSimuladorQuarkusTest {

    @Inject
    SolicitarInclusaoDocumentoDossieProduto portaSaida;

    @Test
    void selecionaSimuladorSemAmbiguidadeCdiELêFixturePropria() {
        var comando = new ComandoInclusaoDocumentoDossieProduto(
                123L, null, null, null, null, "RG", null, null, null);

        var resultado = portaSaida.incluir(comando).await().indefinitely();

        assertEquals(456L, resultado.identificadorDocumento());
        assertEquals(789L, resultado.identificadorInstanciaDocumento());
    }
}
