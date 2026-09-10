package br.gov.caixa.simtr.monitoramento.dominio.modelo;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@io.quarkus.test.junit.QuarkusTest
class ReagendamentoMonitoramentoTest {
    private static final Instant INICIO = Instant.parse("2026-09-09T12:00:00Z");
    private static final Instant LIMITE = INICIO.plus(Duration.ofHours(24));
    private static final TentativaMonitoramento ATUAL = new TentativaMonitoramento(
            "MON", "ORQ", "pre", "0007", 3, INICIO, LIMITE, "v-removida");
    private static final DecisaoProcessamento.PoliticaAplicada POLITICA =
            new DecisaoProcessamento.PoliticaAplicada("v-removida", "v1", true);

    @ParameterizedTest
    @CsvSource({"60,30,30", "30,30,30", "5,30,5"})
    void preservaJanelaVersaoEIdsAoLimitarProximoHorario(long minutosRestantes,
            long intervaloMinutos, long esperaMinutos) {
        Instant processado = LIMITE.minus(Duration.ofMinutes(minutosRestantes));
        var reagendamento = ReagendamentoMonitoramento.aPartirDe(
                decisao(4, Duration.ofMinutes(intervaloMinutos), processado));
        assertEquals(processado.plus(Duration.ofMinutes(esperaMinutos)), reagendamento.agendadoEm());
        assertEquals(new TentativaMonitoramento("MON", "ORQ", "pre", "0007", 4,
                INICIO, LIMITE, "v-removida"), reagendamento.proximaTentativa());
    }

    @Test
    void intervaloGiganteUsaPrazoOriginalSemOverflowDeInstant() {
        var reagendamento = ReagendamentoMonitoramento.aPartirDe(
                decisao(4, Duration.ofSeconds(Long.MAX_VALUE), INICIO));
        assertEquals(LIMITE, reagendamento.agendadoEm());
    }

    @Test
    void recusaDecisaoExpiradaOuSemAvancoDaTentativa() {
        var intervalo = Duration.ofMinutes(30);
        var expirada = decisao(4, intervalo, LIMITE);
        var semAvanco = decisao(3, intervalo, INICIO);
        var salto = decisao(5, intervalo, INICIO);
        var zero = decisao(4, Duration.ZERO, INICIO);
        var negativo = decisao(4, Duration.ofMinutes(-1), INICIO);
        assertThrows(IllegalArgumentException.class, () -> ReagendamentoMonitoramento.aPartirDe(expirada));
        assertThrows(IllegalArgumentException.class, () -> ReagendamentoMonitoramento.aPartirDe(semAvanco));
        assertThrows(IllegalArgumentException.class, () -> ReagendamentoMonitoramento.aPartirDe(salto));
        assertThrows(IllegalArgumentException.class, () -> ReagendamentoMonitoramento.aPartirDe(zero));
        assertThrows(IllegalArgumentException.class, () -> ReagendamentoMonitoramento.aPartirDe(negativo));
    }

    @Test
    void modeloNaoAceitaHorarioForaDaJanelaOriginal() {
        Instant antes = INICIO.minusSeconds(1);
        Instant depois = LIMITE.plusSeconds(1);
        assertThrows(IllegalArgumentException.class, () -> new ReagendamentoMonitoramento(ATUAL, antes));
        assertThrows(IllegalArgumentException.class, () -> new ReagendamentoMonitoramento(ATUAL, depois));
        assertThrows(NullPointerException.class, () -> new ReagendamentoMonitoramento(null, INICIO));
        assertThrows(NullPointerException.class, () -> new ReagendamentoMonitoramento(ATUAL, null));
    }

    private static DecisaoProcessamento.ReagendamentoPendente decisao(int proxima,
            Duration intervalo, Instant processado) {
        return new DecisaoProcessamento.ReagendamentoPendente(ATUAL, proxima, intervalo, processado, POLITICA);
    }
}
