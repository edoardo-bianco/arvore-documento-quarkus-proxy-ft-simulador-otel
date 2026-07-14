package br.gov.caixa.simtr.hub.conformidade.adaptador.configuracao;

import static org.junit.jupiter.api.Assertions.assertSame;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ObterChecklist;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

class ChecklistPortasProducerTest {

    @Test
    void selecionaSimuladorQuandoLigadoEMtrQuandoDesligado() {
        ObterChecklist mtr = comando -> Uni.createFrom().nullItem();
        ObterChecklist simulador = comando -> Uni.createFrom().nullItem();
        var producer = new ChecklistPortasProducer();

        assertSame(simulador, producer.portaSaida(mtr, simulador, true));
        assertSame(mtr, producer.portaSaida(mtr, simulador, false));
    }
}
