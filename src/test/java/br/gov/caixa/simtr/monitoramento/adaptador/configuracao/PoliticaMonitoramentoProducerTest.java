package br.gov.caixa.simtr.monitoramento.adaptador.configuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import br.gov.caixa.simtr.monitoramento.dominio.politica.PoliticaMonitoramento;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@QuarkusTest
class PoliticaMonitoramentoProducerTest {

    @Inject
    Instance<PoliticaMonitoramento> politicas;

    @Inject
    PoliticasMonitoramentoConfig config;

    @Test
    void injetaUmaUnicaPoliticaComValoresDeApplicationProperties() {
        assertTrue(politicas.isResolvable());
        var politica = politicas.get();
        assertSame(politica, politicas.get());
        assertEquals("padrao", config.ativa());
        assertEquals("v1", politica.versao());
        var inicio = Instant.parse("2026-09-06T12:00:00Z");
        var limite = politica.calcularLimite(inicio);
        assertEquals(inicio.plus(Duration.ofHours(24)), limite);
        assertEquals(new PoliticaMonitoramento.Decisao.Reagendar(101, Duration.ofHours(6)),
                politica.avaliarTentativaNaoConclusiva(100, inicio, limite));
    }
}
