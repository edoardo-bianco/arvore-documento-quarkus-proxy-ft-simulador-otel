package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.AvancarWorkflowDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoWorkflowDossieProduto;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IniciarOuAvancarWorkflowDossieProdutoCasoDeUsoTest {

    @Test
    void delegaParaPortaDeSaidaERetornaResultadoInterno() {
        IdentificadorDossieProduto identificador = new IdentificadorDossieProduto(123L);
        ResultadoWorkflowDossieProduto esperado = new ResultadoWorkflowDossieProduto(987L);
        FakePortaSaida portaSaida = new FakePortaSaida(Uni.createFrom().item(esperado));
        IniciarOuAvancarWorkflowDossieProdutoCasoDeUso casoDeUso =
                new IniciarOuAvancarWorkflowDossieProdutoCasoDeUso(portaSaida);

        ResultadoWorkflowDossieProduto resultado = casoDeUso.executar(identificador)
                .await().indefinitely();

        assertSame(identificador, portaSaida.identificadorRecebido);
        assertSame(esperado, resultado);
    }

    @Test
    void preservaFalhaDaPortaDeSaida() {
        IllegalStateException falha = new IllegalStateException("falha interna");
        IniciarOuAvancarWorkflowDossieProdutoCasoDeUso casoDeUso =
                new IniciarOuAvancarWorkflowDossieProdutoCasoDeUso(
                        new FakePortaSaida(Uni.createFrom().failure(falha)));

        IllegalStateException observada = assertThrows(
                IllegalStateException.class,
                () -> casoDeUso.executar(new IdentificadorDossieProduto(123L)).await().indefinitely()
        );

        assertEquals("falha interna", observada.getMessage());
    }

    private static final class FakePortaSaida implements AvancarWorkflowDossieProduto {

        private final Uni<ResultadoWorkflowDossieProduto> resultado;
        private IdentificadorDossieProduto identificadorRecebido;

        private FakePortaSaida(Uni<ResultadoWorkflowDossieProduto> resultado) {
            this.resultado = resultado;
        }

        @Override
        public Uni<ResultadoWorkflowDossieProduto> avancar(IdentificadorDossieProduto identificador) {
            identificadorRecebido = identificador;
            return resultado;
        }
    }
}
