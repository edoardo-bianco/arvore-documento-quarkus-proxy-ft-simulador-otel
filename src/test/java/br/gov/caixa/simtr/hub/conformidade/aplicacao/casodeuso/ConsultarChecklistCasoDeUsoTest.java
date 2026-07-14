package br.gov.caixa.simtr.hub.conformidade.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ObterChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ApontamentoChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ComandoConsultaChecklist;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultarChecklistCasoDeUsoTest {

    @Test
    void delegaComandoParaPortaDeSaidaERetornaAgregadoDeLeitura() {
        var comando = new ComandoConsultaChecklist(1000012583L, 7);
        var apontamento = new ApontamentoChecklist(
                2001L, "Apontamento", null, null, false, 1);
        var esperado = new Checklist(
                "Checklist", 1000012583L, 7, null, "14/07/2026 12:00:00",
                false, null, List.of(apontamento));
        var portaSaida = new FakePortaSaida(Uni.createFrom().item(esperado));
        var casoDeUso = new ConsultarChecklistCasoDeUso(portaSaida);

        var resultado = casoDeUso.executar(comando).await().indefinitely();

        assertSame(comando, portaSaida.comandoRecebido);
        assertSame(esperado, resultado);
    }

    @Test
    void preservaFalhaDaPortaDeSaida() {
        var falha = new IllegalStateException("falha interna");
        var casoDeUso = new ConsultarChecklistCasoDeUso(
                new FakePortaSaida(Uni.createFrom().failure(falha)));

        var observada = assertThrows(
                IllegalStateException.class,
                () -> casoDeUso.executar(new ComandoConsultaChecklist(1000012583L, 7))
                        .await().indefinitely());

        assertEquals("falha interna", observada.getMessage());
    }

    private static final class FakePortaSaida implements ObterChecklist {

        private final Uni<Checklist> resultado;
        private ComandoConsultaChecklist comandoRecebido;

        private FakePortaSaida(Uni<Checklist> resultado) {
            this.resultado = resultado;
        }

        @Override
        public Uni<Checklist> obter(ComandoConsultaChecklist comando) {
            comandoRecebido = comando;
            return resultado;
        }
    }
}
