package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarRegistroValidacaoNegocialDossieProduto;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class ValidacaoNegocialDossieProdutoPortasProducerTest {

    @Test
    void selecionaSimuladorQuandoLigadoEMtrQuandoDesligado() {
        SolicitarRegistroValidacaoNegocialDossieProduto mtr = comando ->
                Uni.createFrom().voidItem();
        SolicitarRegistroValidacaoNegocialDossieProduto simulador = comando ->
                Uni.createFrom().voidItem();
        var producer = new ValidacaoNegocialDossieProdutoPortasProducer();

        assertSame(simulador, producer.portaSaida(mtr, simulador, true));
        assertSame(mtr, producer.portaSaida(mtr, simulador, false));
    }
}
