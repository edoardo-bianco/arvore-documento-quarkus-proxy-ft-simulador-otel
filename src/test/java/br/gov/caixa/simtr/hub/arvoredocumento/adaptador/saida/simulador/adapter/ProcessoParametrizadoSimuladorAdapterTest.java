package br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.simulador.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.arvoredocumento.adaptador.saida.simulador.mapper.ProcessoParametrizadoSimuladorMapper;
import br.gov.caixa.simtr.hub.arvoredocumento.dominio.modelo.IdentificadorNegocialProcesso;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ProcessoParametrizadoSimuladorAdapterTest {

    private final ProcessoParametrizadoSimuladorAdapter adapter =
            new ProcessoParametrizadoSimuladorAdapter(
                    new MarkdownJsonMockReader(new ObjectMapper()),
                    new ProcessoParametrizadoSimuladorMapper()
            );

    @Test
    void leFixtureEspecificaComDtoProprioEMapeiaAArvoreReal() {
        var processo = adapter.obter(new IdentificadorNegocialProcesso(1000009990L))
                .await().indefinitely();

        assertEquals(1000009990L, processo.identificadorNegocial());
        assertEquals("Utilização FGTS - 1º Uso", processo.nome());
        assertEquals(20210706L, processo.macroprocesso().identificadorNegocial());
        assertFalse(processo.relacionamentos().isEmpty());
        assertNotNull(processo.relacionamentos().getFirst().documentos());
        assertFalse(processo.fases().isEmpty());
        assertNull(processo.checklist());
    }

    @Test
    void preservaFallbackSilenciosoParaFixturePadraoQuandoIdNaoExisteOuEValorNulo() {
        var processoFallback = adapter.obter(new IdentificadorNegocialProcesso(999L))
                .await().indefinitely();
        var processoSemId = adapter.obter(new IdentificadorNegocialProcesso(null))
                .await().indefinitely();

        assertEquals(1000016487L, processoFallback.identificadorNegocial());
        assertEquals("Concessão Habitacional", processoFallback.nome());
        assertEquals(1000016487L, processoSemId.identificadorNegocial());
        assertEquals("Concessão Habitacional", processoSemId.nome());
    }
}
