package br.gov.caixa.simtr.monitoramento.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida.PublicarResultadoMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.ResultadoMonitoramento;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Resolve a porta e o mapper reais com a extensao desabilitada, sem broker. */
@QuarkusTest
class PublicarResultadoMonitoramentoQuarkusTest {

    @Inject
    PublicarResultadoMonitoramento publicacao;

    @Test
    void deveResolverPortaERecusarEnvioSemClienteConfigurado() {
        var instante = Instant.parse("2026-09-09T12:00:00Z");
        var resultado = new ResultadoMonitoramento("MON-1", "ORQ-1", "pre-1", "0007",
                "QUARENTENA", null, "QUARENTENA", "PRAZO_MAXIMO", 0, instante, instante, 42L);
        var aguardando = publicacao.executar(resultado).await();
        var espera = Duration.ofSeconds(3);

        var falha = assertThrows(IllegalStateException.class, () -> aguardando.atMost(espera));
        assertEquals("Falha ao publicar resultado de monitoramento.", falha.getMessage());
        assertNull(falha.getCause());
    }
}
