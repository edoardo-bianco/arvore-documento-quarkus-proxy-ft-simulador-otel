package br.gov.caixa.simtr.monitoramento.dominio.politica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.gov.caixa.simtr.monitoramento.dominio.politica.PoliticaMonitoramento.Decisao.Encerrar;
import br.gov.caixa.simtr.monitoramento.dominio.politica.PoliticaMonitoramento.Decisao.Reagendar;
import br.gov.caixa.simtr.monitoramento.dominio.politica.PoliticaMonitoramento.MotivoEncerramento;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class PoliticaMonitoramentoProgressivaTest {

    private static final Instant INICIO = Instant.parse("2026-09-05T12:00:00Z");
    private static final Instant LIMITE = INICIO.plus(Duration.ofHours(24));

    @Test
    void reutilizaIntervaloUnicoEmTodasAsTentativasSemLimiteDeQuantidade() {
        var politica = novaPolitica(List.of(Duration.ofMinutes(30)), OptionalInt.empty());

        var primeira = assertInstanceOf(
                Reagendar.class,
                politica.avaliarTentativaNaoConclusiva(1, INICIO, LIMITE));
        var centesima = assertInstanceOf(
                Reagendar.class,
                politica.avaliarTentativaNaoConclusiva(100, INICIO, LIMITE));

        assertEquals(2, primeira.proximaTentativa());
        assertEquals(Duration.ofMinutes(30), primeira.intervalo());
        assertEquals(101, centesima.proximaTentativa());
        assertEquals(Duration.ofMinutes(30), centesima.intervalo());
    }

    @Test
    void aplicaProgressaoERepeteUltimoIntervaloQuandoAListaTermina() {
        var politica = novaPolitica(
                List.of(
                        Duration.ofMinutes(30),
                        Duration.ofHours(3),
                        Duration.ofHours(4),
                        Duration.ofHours(6)),
                OptionalInt.empty());

        assertEquals(
                Duration.ofMinutes(30),
                assertInstanceOf(
                                Reagendar.class,
                                politica.avaliarTentativaNaoConclusiva(1, INICIO, LIMITE))
                        .intervalo());
        assertEquals(
                Duration.ofHours(6),
                assertInstanceOf(
                                Reagendar.class,
                                politica.avaliarTentativaNaoConclusiva(4, INICIO, LIMITE))
                        .intervalo());
        assertEquals(
                Duration.ofHours(6),
                assertInstanceOf(
                                Reagendar.class,
                                politica.avaliarTentativaNaoConclusiva(8, INICIO, LIMITE))
                        .intervalo());
    }

    @Test
    void encerraPeloMaximoDeTentativasSomenteQuandoConfigurado() {
        var politicaComMaximo = novaPolitica(
                List.of(Duration.ofMinutes(30)),
                OptionalInt.of(5));
        var politicaSemMaximo = novaPolitica(
                List.of(Duration.ofMinutes(30)),
                OptionalInt.empty());

        var encerrada = assertInstanceOf(
                Encerrar.class,
                politicaComMaximo.avaliarTentativaNaoConclusiva(5, INICIO, LIMITE));
        var reagendada = assertInstanceOf(
                Reagendar.class,
                politicaSemMaximo.avaliarTentativaNaoConclusiva(5, INICIO, LIMITE));

        assertEquals(MotivoEncerramento.MAXIMO_TENTATIVAS, encerrada.motivo());
        assertEquals(6, reagendada.proximaTentativa());
    }

    @Test
    void encerraQuandoOInstanteAtingeOPrazoMaximo() {
        var politica = novaPolitica(
                List.of(Duration.ofMinutes(30)),
                OptionalInt.empty());

        var decisao = assertInstanceOf(
                Encerrar.class,
                politica.avaliarTentativaNaoConclusiva(1, LIMITE, LIMITE));

        assertEquals(MotivoEncerramento.PRAZO_MAXIMO, decisao.motivo());
    }

    @Test
    void calculaLimiteAPartirDaDuracaoConfigurada() {
        var politica = novaPolitica(
                List.of(Duration.ofMinutes(30)),
                OptionalInt.empty());

        assertEquals("v1", politica.versao());
        assertEquals(LIMITE, politica.calcularLimite(INICIO));
    }

    @Test
    void rejeitaConfiguracaoInvalida() {
        List<Duration> semIntervalos = List.of();
        var intervaloZero = List.of(Duration.ZERO);
        var intervaloValido = List.of(Duration.ofMinutes(30));
        var semMaximo = OptionalInt.empty();
        var maximoZero = OptionalInt.of(0);

        assertThrows(
                IllegalArgumentException.class,
                () -> novaPolitica(semIntervalos, semMaximo));
        assertThrows(
                IllegalArgumentException.class,
                () -> novaPolitica(intervaloZero, semMaximo));
        assertThrows(
                IllegalArgumentException.class,
                () -> novaPolitica(intervaloValido, maximoZero));
        assertThrows(
                IllegalArgumentException.class,
                () -> new PoliticaMonitoramentoProgressiva(
                        "v1",
                        intervaloValido,
                        semMaximo,
                        Duration.ZERO));
    }

    @Test
    void avaliaLimitesComZeroTentativasSemConsumirAPrimeiraConsulta() {
        var politica = novaPolitica(List.of(Duration.ofMinutes(30)), OptionalInt.of(1));
        assertEquals(java.util.Optional.empty(), politica.motivoEncerramento(0, INICIO, LIMITE));
        assertEquals(java.util.Optional.of(MotivoEncerramento.MAXIMO_TENTATIVAS),
                politica.motivoEncerramento(1, INICIO, LIMITE));
        assertEquals(java.util.Optional.of(MotivoEncerramento.PRAZO_MAXIMO),
                politica.motivoEncerramento(1, LIMITE, LIMITE));
    }

    @Test
    void rejeitaContagemNegativaEInstantesAusentesNaConsultaDeLimites() {
        var politica = novaPolitica(List.of(Duration.ofMinutes(30)), OptionalInt.empty());
        assertThrows(IllegalArgumentException.class, () -> politica.motivoEncerramento(-1, INICIO, LIMITE));
        assertThrows(NullPointerException.class, () -> politica.motivoEncerramento(0, null, LIMITE));
        assertThrows(NullPointerException.class, () -> politica.motivoEncerramento(0, INICIO, null));
    }

    @Test
    void encerraNoLimiteRepresentavelAntesDeIncrementarMesmoSemMaximoConfigurado() {
        var politica = novaPolitica(List.of(Duration.ofMinutes(30)), OptionalInt.empty());
        var ultimaAgendada = assertInstanceOf(Reagendar.class,
                politica.avaliarTentativaNaoConclusiva(Integer.MAX_VALUE - 1, INICIO, LIMITE));
        assertEquals(Integer.MAX_VALUE, ultimaAgendada.proximaTentativa());
        var encerrada = assertInstanceOf(Encerrar.class,
                politica.avaliarTentativaNaoConclusiva(Integer.MAX_VALUE, INICIO, LIMITE));
        assertEquals(MotivoEncerramento.MAXIMO_TENTATIVAS, encerrada.motivo());
        var expirada = assertInstanceOf(Encerrar.class,
                politica.avaliarTentativaNaoConclusiva(Integer.MAX_VALUE, LIMITE, LIMITE));
        assertEquals(MotivoEncerramento.PRAZO_MAXIMO, expirada.motivo());
    }

    private static PoliticaMonitoramentoProgressiva novaPolitica(
            List<Duration> intervalos,
            OptionalInt maximoTentativas) {
        return new PoliticaMonitoramentoProgressiva(
                "v1",
                intervalos,
                maximoTentativas,
                Duration.ofHours(24));
    }
}
