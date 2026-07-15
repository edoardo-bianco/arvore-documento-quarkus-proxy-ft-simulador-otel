package br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.simulador.adapter;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.simulador.mapper.GestaoDocumentoSimuladorMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GestaoDocumentoSimuladorAdapterTest {

    @Test
    void leFixtureRealComDtoProprioSemInterpretarSasOuValidade() {
        var adapter = new GestaoDocumentoSimuladorAdapter(
                new MarkdownJsonMockReader(new ObjectMapper()),
                new GestaoDocumentoSimuladorMapper()
        );

        var credencial = adapter.obter().await().indefinitely();

        assertEquals(
                "sv=mock&ss=b&srt=o&sp=rw&se=2026-07-10T18:00:00Z&sig=mock",
                credencial.sas()
        );
        assertEquals("10/07/2026 18:00:00", credencial.validade());
        assertEquals(
                "https://dossiedigitaldes.blob.core.windows.net",
                credencial.urlStorage()
        );
        assertEquals("pre-validacao", credencial.nomeContainer());
    }

    @Test
    void falhaExplicitamenteQuandoFixtureNaoForneceObjetoJson() {
        var reader = mock(MarkdownJsonMockReader.class);
        when(reader.readFirstJsonObject(anyString(), any())).thenReturn(null);
        var adapter = new GestaoDocumentoSimuladorAdapter(
                reader,
                new GestaoDocumentoSimuladorMapper()
        );

        var falha = assertThrows(IllegalStateException.class, adapter::obter);

        assertEquals(
                "Arquivo de mock nao encontrado no classpath: "
                        + "mock/gestaodocumento/credencial-container.md",
                falha.getMessage()
        );
    }
}
