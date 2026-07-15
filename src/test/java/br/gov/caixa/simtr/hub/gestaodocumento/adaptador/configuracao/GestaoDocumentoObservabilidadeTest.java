package br.gov.caixa.simtr.hub.gestaodocumento.adaptador.configuracao;

import br.gov.caixa.simtr.hub.gestaodocumento.aplicacao.porta.saida.SolicitarCredencialContainer;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.modelo.CredencialContainer;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GestaoDocumentoObservabilidadeTest {

    @Test
    void delegaParaOCasoDeUsoEPreservaResultado() {
        FakePortaSaida portaSaida = new FakePortaSaida();
        CredencialContainer esperado = new CredencialContainer(
                "sas-opaca", "validade-opaca", "url", "container"
        );
        portaSaida.resultado = esperado;
        var observabilidade = new GestaoDocumentoObservabilidade(portaSaida, false);

        CredencialContainer resultado = observabilidade.executar().await().indefinitely();

        assertSame(esperado, resultado);
        assertSame(esperado, portaSaida.resultado);
    }

    @Test
    void propagaMesmaFalhaDaPortaDeSaida() {
        FakePortaSaida portaSaida = new FakePortaSaida();
        portaSaida.falha = new IllegalStateException("falha controlada");
        var observabilidade = new GestaoDocumentoObservabilidade(portaSaida, true);

        IllegalStateException falha = assertThrows(
                IllegalStateException.class,
                () -> observabilidade.executar().await().indefinitely()
        );

        assertSame(portaSaida.falha, falha);
    }

    private static final class FakePortaSaida implements SolicitarCredencialContainer {

        private CredencialContainer resultado;
        private RuntimeException falha;

        @Override
        public Uni<CredencialContainer> obter() {
            if (falha != null) {
                return Uni.createFrom().failure(falha);
            }
            return Uni.createFrom().item(resultado);
        }
    }
}
