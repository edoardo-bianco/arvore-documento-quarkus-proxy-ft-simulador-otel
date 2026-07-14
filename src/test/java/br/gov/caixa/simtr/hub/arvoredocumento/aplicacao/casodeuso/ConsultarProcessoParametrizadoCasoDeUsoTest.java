package br.gov.caixa.simtr.hub.arvoredocumento.aplicacao.casodeuso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.gov.caixa.simtr.hub.arvoredocumento.aplicacao.porta.saida.ObterProcessoParametrizado;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.IdentificadorNegocialProcesso;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.ProcessoParametrizado;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

class ConsultarProcessoParametrizadoCasoDeUsoTest {

    @Test
    void delegaParaPortaDeSaidaERetornaAgregadoDeLeitura() {
        var identificador = new IdentificadorNegocialProcesso(123L);
        var esperado = new ProcessoParametrizado(
                123L, null, null, null, null, null, null, null, null, null, null
        );
        var portaSaida = new FakePortaSaida(Uni.createFrom().item(esperado));
        var casoDeUso = new ConsultarProcessoParametrizadoCasoDeUso(portaSaida);

        var resultado = casoDeUso.executar(identificador).await().indefinitely();

        assertSame(identificador, portaSaida.identificadorRecebido);
        assertSame(esperado, resultado);
    }

    @Test
    void preservaFalhaDaPortaDeSaida() {
        var falha = new IllegalStateException("falha interna");
        var casoDeUso = new ConsultarProcessoParametrizadoCasoDeUso(
                new FakePortaSaida(Uni.createFrom().failure(falha))
        );

        var observada = assertThrows(
                IllegalStateException.class,
                () -> casoDeUso.executar(new IdentificadorNegocialProcesso(123L))
                        .await().indefinitely()
        );

        assertEquals("falha interna", observada.getMessage());
    }

    private static final class FakePortaSaida implements ObterProcessoParametrizado {

        private final Uni<ProcessoParametrizado> resultado;
        private IdentificadorNegocialProcesso identificadorRecebido;

        private FakePortaSaida(Uni<ProcessoParametrizado> resultado) {
            this.resultado = resultado;
        }

        @Override
        public Uni<ProcessoParametrizado> obter(IdentificadorNegocialProcesso identificador) {
            identificadorRecebido = identificador;
            return resultado;
        }
    }
}
