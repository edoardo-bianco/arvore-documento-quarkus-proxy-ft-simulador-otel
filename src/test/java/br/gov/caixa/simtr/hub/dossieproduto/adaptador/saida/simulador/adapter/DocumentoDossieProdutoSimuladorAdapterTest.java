package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.adapter;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.DocumentoDossieProdutoSimuladorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentoDossieProdutoSimuladorAdapterTest {

    @Test
    void leFixtureSnakeCasePorDtoProprioEConverteParaResultadoInterno() {
        var reader = new MarkdownJsonMockReader(new ObjectMapper());
        var adapter = new DocumentoDossieProdutoSimuladorAdapter(reader);

        var resultado = adapter.incluir(null).await().indefinitely();

        assertEquals(456L, resultado.identificadorDocumento());
        assertEquals(789L, resultado.identificadorInstanciaDocumento());
    }

    @Test
    void falhaExplicitamenteQuandoFixtureNaoExiste() {
        MarkdownJsonMockReader reader = mock(MarkdownJsonMockReader.class);
        when(reader.readFirstJsonObject(anyString(),
                eq(DocumentoDossieProdutoSimuladorResponse.class))).thenReturn(null);
        var adapter = new DocumentoDossieProdutoSimuladorAdapter(reader);

        IllegalStateException falha = assertThrows(
                IllegalStateException.class, () -> adapter.incluir(null));

        assertEquals("Arquivo de mock nao encontrado no classpath: "
                        + "mock/dossieproduto/documento-dossie-produto.md",
                falha.getMessage());
    }
}
