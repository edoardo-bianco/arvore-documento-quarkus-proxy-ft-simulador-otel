package br.gov.caixa.simtr.monitoramento.adaptador.saida.simulador.prevalidacao;

import java.time.Duration;
import java.util.NoSuchElementException;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Executa os cenarios no classloader instrumentado pelo Quarkus/JaCoCo do projeto. */
@QuarkusTest
class PreValidacaoSimuladaAdapterTest {

    private static final Duration ESPERA = Duration.ofSeconds(2);
    private final PreValidacaoSimuladaAdapter adapter =
            new PreValidacaoSimuladaAdapter(true, new PreValidacaoSimuladaMapper());

    @ParameterizedTest
    @CsvSource({
            "pre-em-analise, EM_ANALISE_ENVIO_MTR",
            "pre-conforme, CONFORME",
            "pre-nao-conforme, NAO_CONFORME"
    })
    void consultaCenarioDeterministicoComOrigemSimulada(String id, String situacao) {
        var consulta = adapter.executar(id);
        var primeira = consulta.await().atMost(ESPERA);
        var segunda = consulta.await().atMost(ESPERA);

        assertEquals(situacao, primeira.situacao());
        assertTrue(primeira.simulada());
        assertEquals(primeira, segunda);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void identificadorAusenteFalhaNoUniSemExporValor(String id) {
        var consulta = adapter.executar(id);

        var aguardando = consulta.await();
        var falha = assertThrows(IllegalArgumentException.class, () -> aguardando.atMost(ESPERA));

        assertEquals("Identificador da pre-validacao obrigatorio.", falha.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"nao-cadastrada", " pre-em-analise ", "segredo-sintetico"})
    void ausenciaNaoRetornaEstadoNaoElegivelNemFallback(String id) {
        var consulta = adapter.executar(id);

        var aguardando = consulta.await();
        var falha = assertThrows(NoSuchElementException.class, () -> aguardando.atMost(ESPERA));

        assertEquals("Pre-validacao nao encontrada no simulador.", falha.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"pre-em-analise", "nao-cadastrada"})
    void desabilitadoNuncaRetornaDadosSimulados(String id) {
        var desabilitado = new PreValidacaoSimuladaAdapter(false, new PreValidacaoSimuladaMapper());
        var consulta = desabilitado.executar(id);

        var aguardando = consulta.await();
        var falha = assertThrows(IllegalStateException.class, () -> aguardando.atMost(ESPERA));

        assertEquals("Simulador de pre-validacao desabilitado.", falha.getMessage());
    }

    @Test
    void falhaDeConsultaNaoMudaOsDemaisCenarios() {
        var aguardando = adapter.executar("ausente").await();
        assertThrows(NoSuchElementException.class, () -> aguardando.atMost(ESPERA));

        var resultado = adapter.executar("pre-conforme").await().atMost(ESPERA);

        assertEquals("CONFORME", resultado.situacao());
    }
}
