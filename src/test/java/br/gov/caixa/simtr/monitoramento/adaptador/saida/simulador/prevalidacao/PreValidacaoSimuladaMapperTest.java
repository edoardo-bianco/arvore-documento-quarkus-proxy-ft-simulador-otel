package br.gov.caixa.simtr.monitoramento.adaptador.saida.simulador.prevalidacao;

import br.gov.caixa.simtr.monitoramento.adaptador.saida.simulador.prevalidacao.dto.PreValidacaoSimuladaDto;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.PreValidacaoConsultada;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Executa os cenarios no classloader instrumentado pelo Quarkus/JaCoCo do projeto. */
@QuarkusTest
class PreValidacaoSimuladaMapperTest {

    private final PreValidacaoSimuladaMapper mapper = new PreValidacaoSimuladaMapper();

    @ParameterizedTest
    @ValueSource(strings = {"EM_ANALISE_ENVIO_MTR", "CONFORME", "NAO_CONFORME", "Outra situacao"})
    void preservaSituacaoOriginalEIdentificaOrigemSimulada(String situacao) {
        var resultado = mapper.paraModelo(new PreValidacaoSimuladaDto(situacao));

        assertEquals(situacao, resultado.situacao());
        assertTrue(resultado.simulada());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void rejeitaSituacaoAusenteSemInventarEstado(String situacao) {
        var dto = new PreValidacaoSimuladaDto(situacao);

        var falha = assertThrows(IllegalArgumentException.class, () -> mapper.paraModelo(dto));

        assertEquals("Situacao da pre-validacao obrigatoria.", falha.getMessage());
    }

    @Test
    void rejeitaRespostaSimuladaAusente() {
        var falha = assertThrows(IllegalArgumentException.class, () -> mapper.paraModelo(null));

        assertEquals("Resposta simulada de pre-validacao ausente.", falha.getMessage());
    }

    @Test
    void modeloPreservaTextoEOrigemRecebidos() {
        var consultada = new PreValidacaoConsultada(" situacao original ", false);

        assertEquals(new PreValidacaoConsultada(" situacao original ", false), consultada);
    }
}
