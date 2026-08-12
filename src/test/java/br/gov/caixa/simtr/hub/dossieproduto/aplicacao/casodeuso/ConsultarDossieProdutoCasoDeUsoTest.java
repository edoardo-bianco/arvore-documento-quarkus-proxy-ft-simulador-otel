package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.ObterDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaConsultaDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.DossieProdutoConsultado;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.IdentificadorDossieProduto;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultarDossieProdutoCasoDeUsoTest {

    @Test
    void delegaUmaVezComOMesmoIdentificadorEPropagaOItem() {
        var identificador = new IdentificadorDossieProduto(4324680L);
        var esperado = dossieConsultado();
        var portaSaida = new FakePortaSaida(Uni.createFrom().item(esperado));
        var casoDeUso = new ConsultarDossieProdutoCasoDeUso(portaSaida);

        var resultado = casoDeUso.executar(identificador).await().indefinitely();

        assertEquals(1, portaSaida.invocacoes);
        assertSame(identificador, portaSaida.identificadorRecebido);
        assertSame(esperado, resultado);
    }

    @Test
    void propagaItemNuloSemNovaDelegacao() {
        var identificador = new IdentificadorDossieProduto(4324680L);
        var portaSaida = new FakePortaSaida(Uni.createFrom().nullItem());
        var casoDeUso = new ConsultarDossieProdutoCasoDeUso(portaSaida);

        var resultado = casoDeUso.executar(identificador).await().indefinitely();

        assertEquals(1, portaSaida.invocacoes);
        assertSame(identificador, portaSaida.identificadorRecebido);
        assertNull(resultado);
    }

    @Test
    void propagaAMesmaFalhaSemNovaDelegacao() {
        var identificador = new IdentificadorDossieProduto(4324680L);
        var falha = new FalhaConsultaDossieProduto(
                FalhaConsultaDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                503,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        var portaSaida = new FakePortaSaida(Uni.createFrom().failure(falha));
        var casoDeUso = new ConsultarDossieProdutoCasoDeUso(portaSaida);

        var espera = casoDeUso.executar(identificador).await();
        var observada = assertThrows(
                FalhaConsultaDossieProduto.class, espera::indefinitely);

        assertEquals(1, portaSaida.invocacoes);
        assertSame(identificador, portaSaida.identificadorRecebido);
        assertSame(falha, observada);
    }

    private static DossieProdutoConsultado dossieConsultado() {
        return new DossieProdutoConsultado(
                4324680L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private static final class FakePortaSaida implements ObterDossieProduto {

        private final Uni<DossieProdutoConsultado> resultado;
        private IdentificadorDossieProduto identificadorRecebido;
        private int invocacoes;

        private FakePortaSaida(Uni<DossieProdutoConsultado> resultado) {
            this.resultado = resultado;
        }

        @Override
        public Uni<DossieProdutoConsultado> obter(IdentificadorDossieProduto identificador) {
            invocacoes++;
            identificadorRecebido = identificador;
            return resultado;
        }
    }
}
