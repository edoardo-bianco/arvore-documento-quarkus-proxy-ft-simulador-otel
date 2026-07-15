package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.simulador.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.simulador.mapper.ChecklistSimuladorMapper;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.ComandoConsultaChecklist;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ChecklistSimuladorAdapterTest {

    private final ChecklistSimuladorAdapter adapter = new ChecklistSimuladorAdapter(
            new MarkdownJsonMockReader(new ObjectMapper()),
            new ChecklistSimuladorMapper()
    );

    @Test
    void leFixtureRealComDtoProprioEMapeiaChecklistCompleto() {
        var checklist = adapter.obter(new ComandoConsultaChecklist(1000012583L, 1))
                .await().indefinitely();

        assertEquals(1000012583L, checklist.identificadorNegocial());
        assertEquals(1, checklist.versao());
        assertEquals("Utilização FGTS - Certidão de Casamento", checklist.nome());
        assertEquals("25/03/2026 15:36:04", checklist.dataHoraCriacao());
        assertEquals("13/05/2026 16:27:41", checklist.dataHoraUltimaAlteracao());
        assertFalse(checklist.verificacaoPrevia());
        assertNull(checklist.orientacaoOperador());
        assertEquals(6, checklist.apontamentos().size());
        assertEquals(1000012577L,
                checklist.apontamentos().getFirst().identificadorNegocial());
        assertEquals(1, checklist.apontamentos().getFirst().sequenciaApresentacao());
    }

    @Test
    void preservaFallbackSilenciosoParaFixturePadraoQuandoArquivoNaoExisteOuParametrosSaoNulos() {
        var checklistFallback = adapter.obter(new ComandoConsultaChecklist(999L, 99))
                .await().indefinitely();
        var checklistSemParametros = adapter.obter(new ComandoConsultaChecklist(null, null))
                .await().indefinitely();

        assertEquals(1000012583L, checklistFallback.identificadorNegocial());
        assertEquals(1, checklistFallback.versao());
        assertEquals("Utilização FGTS - Certidão de Casamento", checklistFallback.nome());
        assertEquals(1000012583L, checklistSemParametros.identificadorNegocial());
        assertEquals(1, checklistSemParametros.versao());
    }
}
