package br.gov.caixa.simtr.monitoramento.integracao;

import br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida.ConsultarPreValidacao;
import br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida.ConsultarSituacaoDossie;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.SituacaoDossieConsultada;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class ConsultasMonitoramentoQuarkusTest {

    private static final Duration ESPERA = Duration.ofSeconds(5);

    @Inject
    ConsultarPreValidacao preValidacao;

    @Inject
    ConsultarSituacaoDossie situacaoDossie;

    @ConfigProperty(name = "monitoramento.simulador.prevalidacao.habilitado")
    boolean simuladorHabilitado;

    @Test
    void configuracaoPadraoMantemMockDesabilitadoSemImpedirBootstrap() {
        assertFalse(simuladorHabilitado);

        var aguardando = preValidacao.executar("pre-em-analise").await();
        var falha = assertThrows(IllegalStateException.class, () -> aguardando.atMost(ESPERA));

        assertEquals("Simulador de pre-validacao desabilitado.", falha.getMessage());
    }

    @Test
    void aclInjetadaConsultaHubRealComSimuladorExistente() {
        var resultado = situacaoDossie.executar("0004324680").await().atMost(ESPERA);

        assertEquals(new SituacaoDossieConsultada(1, "Rascunho"), resultado);
    }

    @Test
    void ausenciaNoHubNaoProduzSituacaoFicticia() {
        var aguardando = situacaoDossie.executar("4324681").await();
        var falha = assertThrows(RuntimeException.class, () -> aguardando.atMost(ESPERA));

        assertEquals("Dossie produto 4324681 nao encontrado no simulador.", falha.getMessage());
    }
}
