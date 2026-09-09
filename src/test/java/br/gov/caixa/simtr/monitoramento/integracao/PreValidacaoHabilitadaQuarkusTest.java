package br.gov.caixa.simtr.monitoramento.integracao;

import br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida.ConsultarPreValidacao;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(PreValidacaoHabilitadaQuarkusTest.Habilitada.class)
class PreValidacaoHabilitadaQuarkusTest {

    private static final Duration ESPERA = Duration.ofSeconds(5);

    @Inject
    ConsultarPreValidacao preValidacao;

    @ParameterizedTest
    @CsvSource({
            "pre-em-analise, EM_ANALISE_ENVIO_MTR",
            "pre-conforme, CONFORME",
            "pre-nao-conforme, NAO_CONFORME"
    })
    void profileHabilitaConsultaPelaPortaComMapperReal(String id, String situacao) {
        var resultado = preValidacao.executar(id).await().atMost(ESPERA);

        assertEquals(situacao, resultado.situacao());
        assertTrue(resultado.simulada());
    }

    @Test
    void consultaAusenteContinuaFalhaComMockHabilitado() {
        var aguardando = preValidacao.executar("ausente").await();
        var falha = assertThrows(NoSuchElementException.class, () -> aguardando.atMost(ESPERA));

        assertEquals("Pre-validacao nao encontrada no simulador.", falha.getMessage());
    }

    public static class Habilitada implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("monitoramento.simulador.prevalidacao.habilitado", "true");
        }
    }
}
