package br.gov.caixa.simtr.hub.arvoredocumento.adaptador.configuracao;

import static org.junit.jupiter.api.Assertions.assertSame;

import br.gov.caixa.simtr.hub.arvoredocumento.aplicacao.porta.saida.ObterProcessoParametrizado;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

class ProcessoParametrizadoPortasProducerTest {

    @Test
    void selecionaSimuladorQuandoLigadoEMtrQuandoDesligado() {
        ObterProcessoParametrizado mtr = identificador -> Uni.createFrom().nullItem();
        ObterProcessoParametrizado simulador = identificador -> Uni.createFrom().nullItem();
        var producer = new ProcessoParametrizadoPortasProducer();

        assertSame(simulador, producer.portaSaida(mtr, simulador, true));
        assertSame(mtr, producer.portaSaida(mtr, simulador, false));
    }
}
