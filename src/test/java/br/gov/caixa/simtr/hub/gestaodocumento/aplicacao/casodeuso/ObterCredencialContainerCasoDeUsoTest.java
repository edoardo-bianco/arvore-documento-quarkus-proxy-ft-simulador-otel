package br.gov.caixa.simtr.hub.gestaodocumento.aplicacao.casodeuso;

import br.gov.caixa.simtr.hub.gestaodocumento.aplicacao.porta.saida.SolicitarCredencialContainer;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.modelo.CredencialContainer;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObterCredencialContainerCasoDeUsoTest {

    @Test
    void delegaParaPortaDeSaidaERetornaCredencialSemInterpretarValidade() {
        var validade = Map.<String, Object>of(
                "expira_em", "31/12/2099 23:59:47",
                "origem", "contrato-mtr"
        );
        var esperado = new CredencialContainer(
                "sv=teste&sp=rw&sig=valor-opaco",
                validade,
                "https://storage.example.test",
                "container-teste"
        );
        var portaSaida = new FakePortaSaida(Uni.createFrom().item(esperado));
        var casoDeUso = new ObterCredencialContainerCasoDeUso(portaSaida);

        var resultado = casoDeUso.executar().await().indefinitely();

        assertEquals(1, portaSaida.chamadas);
        assertSame(esperado, resultado);
        assertSame(validade, resultado.validade());
    }

    @Test
    void modeloPreservaNulabilidadeContratualSemCriarInvariantes() {
        var credencial = new CredencialContainer(null, null, null, null);

        assertNull(credencial.sas());
        assertNull(credencial.validade());
        assertNull(credencial.urlStorage());
        assertNull(credencial.nomeContainer());
    }

    @Test
    void preservaMesmaFalhaDaPortaDeSaida() {
        var falha = new IllegalStateException("falha interna");
        var casoDeUso = new ObterCredencialContainerCasoDeUso(
                new FakePortaSaida(Uni.createFrom().failure(falha))
        );

        var observada = assertThrows(
                IllegalStateException.class,
                () -> casoDeUso.executar().await().indefinitely()
        );

        assertSame(falha, observada);
    }

    private static final class FakePortaSaida implements SolicitarCredencialContainer {

        private final Uni<CredencialContainer> resultado;
        private int chamadas;

        private FakePortaSaida(Uni<CredencialContainer> resultado) {
            this.resultado = resultado;
        }

        @Override
        public Uni<CredencialContainer> obter() {
            chamadas++;
            return resultado;
        }
    }
}
