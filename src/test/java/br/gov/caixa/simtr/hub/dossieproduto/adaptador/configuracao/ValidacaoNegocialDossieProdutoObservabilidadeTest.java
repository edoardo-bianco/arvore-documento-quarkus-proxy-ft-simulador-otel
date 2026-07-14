package br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarRegistroValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoRegistroValidacaoNegocialDossieProduto;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidacaoNegocialDossieProdutoObservabilidadeTest {

    @Test
    void delegaMesmoComandoEPreservaResultadoVoid() {
        FakePortaSaida portaSaida = new FakePortaSaida();
        var observabilidade = new ValidacaoNegocialDossieProdutoObservabilidade(
                portaSaida, false);
        var comando = new ComandoRegistroValidacaoNegocialDossieProduto(
                123L, null, null);

        Void resultado = observabilidade.executar(comando).await().indefinitely();

        assertSame(comando, portaSaida.comandoRecebido);
        assertNull(resultado);
    }

    @Test
    void propagaMesmaFalhaDaPortaDeSaida() {
        FakePortaSaida portaSaida = new FakePortaSaida();
        portaSaida.falha = new IllegalStateException("falha validacao");
        var observabilidade = new ValidacaoNegocialDossieProdutoObservabilidade(
                portaSaida, true);
        var comando = new ComandoRegistroValidacaoNegocialDossieProduto(
                null, null, null);

        IllegalStateException falha = assertThrows(
                IllegalStateException.class,
                () -> observabilidade.executar(comando).await().indefinitely());

        assertSame(portaSaida.falha, falha);
        assertSame(comando, portaSaida.comandoRecebido);
    }

    private static final class FakePortaSaida
            implements SolicitarRegistroValidacaoNegocialDossieProduto {

        private ComandoRegistroValidacaoNegocialDossieProduto comandoRecebido;
        private RuntimeException falha;

        @Override
        public Uni<Void> registrar(ComandoRegistroValidacaoNegocialDossieProduto comando) {
            comandoRecebido = comando;
            if (falha != null) {
                return Uni.createFrom().failure(falha);
            }
            return Uni.createFrom().voidItem();
        }
    }
}
