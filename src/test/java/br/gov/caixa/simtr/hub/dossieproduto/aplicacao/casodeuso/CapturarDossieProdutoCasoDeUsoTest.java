package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarCapturaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaCapturaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCapturaDossieProduto;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CapturarDossieProdutoCasoDeUsoTest {

    @Test
    void delegaUmaVezComOMesmoIdentificadorEPropagaOItem() {
        var identificador = new IdentificadorDossieProduto(123L);
        var esperado = new ResultadoCapturaDossieProduto(123L);
        var portaSaida = new FakePortaSaida(Uni.createFrom().item(esperado));
        var casoDeUso = new CapturarDossieProdutoCasoDeUso(portaSaida);

        var resultado = casoDeUso.executar(identificador).await().indefinitely();

        assertEquals(1, portaSaida.invocacoes);
        assertSame(identificador, portaSaida.identificadorRecebido);
        assertSame(esperado, resultado);
    }

    @Test
    void propagaItemNuloSemNovaDelegacao() {
        var identificador = new IdentificadorDossieProduto(123L);
        var portaSaida = new FakePortaSaida(Uni.createFrom().nullItem());
        var casoDeUso = new CapturarDossieProdutoCasoDeUso(portaSaida);

        var resultado = casoDeUso.executar(identificador).await().indefinitely();

        assertEquals(1, portaSaida.invocacoes);
        assertSame(identificador, portaSaida.identificadorRecebido);
        assertNull(resultado);
    }

    @Test
    void propagaAMesmaFalhaSemNovaDelegacao() {
        var identificador = new IdentificadorDossieProduto(123L);
        var falha = new FalhaCapturaDossieProduto(
                FalhaCapturaDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                503,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        var portaSaida = new FakePortaSaida(Uni.createFrom().failure(falha));
        var casoDeUso = new CapturarDossieProdutoCasoDeUso(portaSaida);

        var espera = casoDeUso.executar(identificador).await();
        var observada = assertThrows(
                FalhaCapturaDossieProduto.class, espera::indefinitely);

        assertEquals(1, portaSaida.invocacoes);
        assertSame(identificador, portaSaida.identificadorRecebido);
        assertSame(falha, observada);
    }

    private static final class FakePortaSaida implements SolicitarCapturaDossieProduto {

        private final Uni<ResultadoCapturaDossieProduto> resultado;
        private IdentificadorDossieProduto identificadorRecebido;
        private int invocacoes;

        private FakePortaSaida(Uni<ResultadoCapturaDossieProduto> resultado) {
            this.resultado = resultado;
        }

        @Override
        public Uni<ResultadoCapturaDossieProduto> capturar(
                IdentificadorDossieProduto identificador
        ) {
            invocacoes++;
            identificadorRecebido = identificador;
            return resultado;
        }
    }
}
