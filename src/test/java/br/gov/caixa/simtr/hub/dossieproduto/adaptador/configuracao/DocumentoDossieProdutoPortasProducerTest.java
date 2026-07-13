package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoInclusaoDocumentoDossieProduto;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class DocumentoDossieProdutoPortasProducerTest {

    @Test
    void selecionaSimuladorQuandoLigadoEMtrQuandoDesligado() {
        SolicitarInclusaoDocumentoDossieProduto mtr = comando ->
                Uni.createFrom().item(new ResultadoInclusaoDocumentoDossieProduto(10L, 11L));
        SolicitarInclusaoDocumentoDossieProduto simulador = comando ->
                Uni.createFrom().item(new ResultadoInclusaoDocumentoDossieProduto(20L, 21L));
        var producer = new DocumentoDossieProdutoPortasProducer();

        assertSame(simulador, producer.portaSaida(mtr, simulador, true));
        assertSame(mtr, producer.portaSaida(mtr, simulador, false));
    }
}
