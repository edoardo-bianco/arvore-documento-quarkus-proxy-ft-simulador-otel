package br.gov.caixa.simtr.monitoramento.aplicacao.casodeuso;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.gov.caixa.simtr.monitoramento.aplicacao.porta.entrada.PrepararMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.ParametrosMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.politica.PoliticaMonitoramentoProgressiva;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Calculo local; QuarkusTest habilita a instrumentacao existente, sem broker. */
@QuarkusTest
class PrepararMonitoramentoUseCaseTest {

    @Inject
    PrepararMonitoramento preparar;

    @ParameterizedTest
    @ValueSource(strings = {"1970-01-01T00:00:00Z", "2026-09-08T12:00:00.123456789Z"})
    void deveUsarPoliticaSelecionadaNoCdi(String inicio) {
        var iniciadoEm = Instant.parse(inicio);
        assertEquals(new ParametrosMonitoramento(iniciadoEm.plus(Duration.ofHours(24)), "v1"),
                preparar.executar(iniciadoEm));
    }

    @Test
    void deveDelegarDuracaoEVersaoSemFixarPadraoNoCasoDeUso() {
        var politica = new PoliticaMonitoramentoProgressiva("outra-versao",
                List.of(Duration.ofMinutes(1)), OptionalInt.empty(), Duration.ofHours(7));
        var caso = new PrepararMonitoramentoUseCase(politica);
        assertEquals(new ParametrosMonitoramento(Instant.EPOCH.plus(Duration.ofHours(7)), "outra-versao"),
                caso.executar(Instant.EPOCH));
    }

    @Test
    void devePropagarInstanteAusente() {
        assertThrows(NullPointerException.class, () -> preparar.executar(null));
    }

    @Test
    void devePropagarOverflowDoLimiteSemRetornoFicticio() {
        assertThrows(DateTimeException.class, () -> preparar.executar(Instant.MAX));
    }
}
