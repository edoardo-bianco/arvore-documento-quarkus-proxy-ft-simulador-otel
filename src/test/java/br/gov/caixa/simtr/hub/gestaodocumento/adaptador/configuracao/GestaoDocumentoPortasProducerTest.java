package br.gov.caixa.simtr.hub.gestaodocumento.adaptador.configuracao;

import br.gov.caixa.simtr.hub.gestaodocumento.aplicacao.porta.saida.SolicitarCredencialContainer;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class GestaoDocumentoPortasProducerTest {

    @Test
    void selecionaSimuladorQuandoLigadoEMtrQuandoDesligado() {
        SolicitarCredencialContainer mtr = () -> Uni.createFrom().nullItem();
        SolicitarCredencialContainer simulador = () -> Uni.createFrom().nullItem();
        var producer = new GestaoDocumentoPortasProducer();

        assertSame(simulador, producer.portaSaida(mtr, simulador, true));
        assertSame(mtr, producer.portaSaida(mtr, simulador, false));
    }
}
