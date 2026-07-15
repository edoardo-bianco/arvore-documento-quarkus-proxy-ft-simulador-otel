package br.gov.caixa.simtr.hub.dossieproduto.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteRelacionadoCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoCriacaoDossieProduto;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class CriarDossieProdutoCasoDeUsoTest {

    @Test
    void delegaComandoCompletoParaPortaDeSaidaERetornaResultadoInterno() {
        ComandoCriacaoDossieProduto comando = comando();
        ResultadoCriacaoDossieProduto esperado = new ResultadoCriacaoDossieProduto(987L);
        FakePortaSaida portaSaida = new FakePortaSaida(Uni.createFrom().item(esperado));
        CriarDossieProdutoCasoDeUso casoDeUso = new CriarDossieProdutoCasoDeUso(portaSaida);

        ResultadoCriacaoDossieProduto resultado = casoDeUso.executar(comando)
                .await().indefinitely();

        assertSame(comando, portaSaida.comandoRecebido);
        assertSame(esperado, resultado);
    }

    @Test
    void preservaFalhaDaPortaDeSaida() {
        IllegalStateException falha = new IllegalStateException("falha interna");
        CriarDossieProdutoCasoDeUso casoDeUso = new CriarDossieProdutoCasoDeUso(
                new FakePortaSaida(Uni.createFrom().failure(falha)));

        IllegalStateException observada = assertThrows(
                IllegalStateException.class,
                () -> casoDeUso.executar(comando()).await().indefinitely()
        );

        assertEquals("falha interna", observada.getMessage());
    }

    private static ComandoCriacaoDossieProduto comando() {
        return new ComandoCriacaoDossieProduto(
                100L,
                200L,
                300L,
                List.of(new ClienteCriacaoDossieProduto(
                        "12345678901",
                        "12345678000190",
                        1L,
                        new ClienteRelacionadoCriacaoDossieProduto("98765432100", null),
                        1
                ))
        );
    }

    private static final class FakePortaSaida implements SolicitarCriacaoDossieProduto {

        private final Uni<ResultadoCriacaoDossieProduto> resultado;
        private ComandoCriacaoDossieProduto comandoRecebido;

        private FakePortaSaida(Uni<ResultadoCriacaoDossieProduto> resultado) {
            this.resultado = resultado;
        }

        @Override
        public Uni<ResultadoCriacaoDossieProduto> criar(ComandoCriacaoDossieProduto comando) {
            comandoRecebido = comando;
            return resultado;
        }
    }
}
