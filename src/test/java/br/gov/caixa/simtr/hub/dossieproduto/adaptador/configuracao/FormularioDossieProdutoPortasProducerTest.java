package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoAtualizacaoFormularioDossieProduto;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class FormularioDossieProdutoPortasProducerTest {

    @Test
    void selecionaSimuladorQuandoLigadoEMtrQuandoDesligado() {
        SolicitarAtualizacaoFormularioDossieProduto mtr = comando ->
                Uni.createFrom().item(new ResultadoAtualizacaoFormularioDossieProduto(10L));
        SolicitarAtualizacaoFormularioDossieProduto simulador = comando ->
                Uni.createFrom().item(new ResultadoAtualizacaoFormularioDossieProduto(20L));
        var producer = new FormularioDossieProdutoPortasProducer();

        assertSame(simulador, producer.portaSaida(mtr, simulador, true));
        assertSame(mtr, producer.portaSaida(mtr, simulador, false));
    }
}
