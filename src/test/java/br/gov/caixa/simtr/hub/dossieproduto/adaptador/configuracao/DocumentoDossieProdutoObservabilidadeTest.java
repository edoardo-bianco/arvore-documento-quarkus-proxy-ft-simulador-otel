package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoInclusaoDocumentoDossieProduto;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentoDossieProdutoObservabilidadeTest {

    @Test
    void delegaComandoERetornaResultadoSemAlteracao() {
        FakePortaSaida portaSaida = new FakePortaSaida();
        var observabilidade = new DocumentoDossieProdutoObservabilidade(portaSaida, false);
        var comando = comando(123L, "RG");

        var resultado = observabilidade.executar(comando).await().indefinitely();

        assertSame(comando, portaSaida.comandoRecebido);
        assertEquals(456L, resultado.identificadorDocumento());
        assertEquals(789L, resultado.identificadorInstanciaDocumento());
    }

    @Test
    void propagaMesmaFalhaDaPortaDeSaida() {
        FakePortaSaida portaSaida = new FakePortaSaida();
        portaSaida.falha = new IllegalStateException("falha documento");
        var observabilidade = new DocumentoDossieProdutoObservabilidade(portaSaida, true);
        var comando = comando(null, null);

        IllegalStateException falha = assertThrows(
                IllegalStateException.class,
                () -> observabilidade.executar(comando).await().indefinitely());

        assertSame(portaSaida.falha, falha);
        assertSame(comando, portaSaida.comandoRecebido);
    }

    private static ComandoInclusaoDocumentoDossieProduto comando(Long id, String tipo) {
        return new ComandoInclusaoDocumentoDossieProduto(
                id, null, null, null, null, tipo, null, null, null);
    }

    private static final class FakePortaSaida
            implements SolicitarInclusaoDocumentoDossieProduto {

        private ComandoInclusaoDocumentoDossieProduto comandoRecebido;
        private RuntimeException falha;

        @Override
        public Uni<ResultadoInclusaoDocumentoDossieProduto> incluir(
                ComandoInclusaoDocumentoDossieProduto comando) {
            comandoRecebido = comando;
            if (falha != null) {
                return Uni.createFrom().failure(falha);
            }
            return Uni.createFrom().item(
                    new ResultadoInclusaoDocumentoDossieProduto(456L, 789L));
        }
    }
}
