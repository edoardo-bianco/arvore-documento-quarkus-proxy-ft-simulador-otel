package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoAtualizacaoFormularioDossieProduto;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormularioDossieProdutoObservabilidadeTest {

    @Test
    void delegaComandoERetornaResultadoSemAlteracao() {
        FakePortaSaida portaSaida = new FakePortaSaida();
        var observabilidade = new FormularioDossieProdutoObservabilidade(portaSaida, false);
        var comando = new ComandoAtualizacaoFormularioDossieProduto(123L, List.of());

        var resultado = observabilidade.executar(comando).await().indefinitely();

        assertSame(comando, portaSaida.comandoRecebido);
        assertEquals(123L, resultado.identificadorDossieProduto());
    }

    @Test
    void propagaMesmaFalhaDaPortaDeSaida() {
        FakePortaSaida portaSaida = new FakePortaSaida();
        portaSaida.falha = new IllegalStateException("falha formulario");
        var observabilidade = new FormularioDossieProdutoObservabilidade(portaSaida, true);
        var comando = new ComandoAtualizacaoFormularioDossieProduto(null, null);

        IllegalStateException falha = assertThrows(
                IllegalStateException.class,
                () -> observabilidade.executar(comando).await().indefinitely());

        assertSame(portaSaida.falha, falha);
        assertSame(comando, portaSaida.comandoRecebido);
    }

    private static final class FakePortaSaida
            implements SolicitarAtualizacaoFormularioDossieProduto {

        private ComandoAtualizacaoFormularioDossieProduto comandoRecebido;
        private RuntimeException falha;

        @Override
        public Uni<ResultadoAtualizacaoFormularioDossieProduto> atualizar(
                ComandoAtualizacaoFormularioDossieProduto comando) {
            comandoRecebido = comando;
            if (falha != null) {
                return Uni.createFrom().failure(falha);
            }
            return Uni.createFrom().item(new ResultadoAtualizacaoFormularioDossieProduto(
                    comando.identificadorDossieProduto()));
        }
    }
}
