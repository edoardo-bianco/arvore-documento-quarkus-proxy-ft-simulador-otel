package br.gov.caixa.simtr.hub.conformidade.adaptador.configuracao;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ObterChecklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.Checklist;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ComandoConsultaChecklist;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChecklistObservabilidadeTest {

    @Test
    void delegaMesmoComandoEPreservaResultado() {
        FakePortaSaida portaSaida = new FakePortaSaida();
        Checklist esperado = new Checklist(
                "Checklist", 100L, 7, null, null, false, null, null);
        portaSaida.resultado = esperado;
        var observabilidade = new ChecklistObservabilidade(portaSaida, false);
        var comando = new ComandoConsultaChecklist(100L, 7);

        Checklist resultado = observabilidade.executar(comando).await().indefinitely();

        assertSame(comando, portaSaida.comandoRecebido);
        assertSame(esperado, resultado);
    }

    @Test
    void propagaMesmaFalhaDaPortaDeSaida() {
        FakePortaSaida portaSaida = new FakePortaSaida();
        portaSaida.falha = new IllegalStateException("falha checklist");
        var observabilidade = new ChecklistObservabilidade(portaSaida, true);
        var comando = new ComandoConsultaChecklist(null, null);

        IllegalStateException falha = assertThrows(
                IllegalStateException.class,
                () -> observabilidade.executar(comando).await().indefinitely());

        assertSame(portaSaida.falha, falha);
        assertSame(comando, portaSaida.comandoRecebido);
    }

    private static final class FakePortaSaida implements ObterChecklist {

        private ComandoConsultaChecklist comandoRecebido;
        private Checklist resultado;
        private RuntimeException falha;

        @Override
        public Uni<Checklist> obter(ComandoConsultaChecklist comando) {
            comandoRecebido = comando;
            if (falha != null) {
                return Uni.createFrom().failure(falha);
            }
            return Uni.createFrom().item(resultado);
        }
    }
}
