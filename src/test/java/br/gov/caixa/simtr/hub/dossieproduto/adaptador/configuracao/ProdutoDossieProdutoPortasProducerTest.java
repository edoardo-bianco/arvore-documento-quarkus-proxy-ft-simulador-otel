package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAlteracaoProdutosContratadosDossieProduto;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@QuarkusTest
class ProdutoDossieProdutoPortasProducerTest {

    @Inject
    Instance<SolicitarAlteracaoProdutosContratadosDossieProduto> portaSelecionada;

    @Test
    void selecionaSimuladorQuandoLigadoEMtrQuandoDesligado() {
        SolicitarAlteracaoProdutosContratadosDossieProduto mtr = comando ->
                Uni.createFrom().voidItem();
        SolicitarAlteracaoProdutosContratadosDossieProduto simulador = comando ->
                Uni.createFrom().voidItem();
        var producer = new ProdutoDossieProdutoPortasProducer();

        assertSame(simulador, producer.portaSaida(mtr, simulador, true));
        assertSame(mtr, producer.portaSaida(mtr, simulador, false));
    }

    @Test
    void resolveUmaUnicaPortaNoBootstrapQuarkus() {
        assertFalse(portaSelecionada.isUnsatisfied());
        assertFalse(portaSelecionada.isAmbiguous());
        assertNotNull(portaSelecionada.get());
    }
}
