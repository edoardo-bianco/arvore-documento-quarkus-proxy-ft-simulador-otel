package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.adapter;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.dto.ValidacaoNegocialDossieProdutoSimuladorResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.simulador.mapper.ValidacaoNegocialDossieProdutoSimuladorMapper;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoRegistroValidacaoNegocialDossieProduto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidacaoNegocialDossieProdutoSimuladorAdapterTest {

    @Test
    void leFixtureVaziaPorDtoEMapperPropriosEPreservaResultadoVoid() {
        var reader = new MarkdownJsonMockReader(new ObjectMapper());
        var mapper = new ValidacaoNegocialDossieProdutoSimuladorMapper();
        var adapter = new ValidacaoNegocialDossieProdutoSimuladorAdapter(reader, mapper);
        var comando = new ComandoRegistroValidacaoNegocialDossieProduto(
                123L, null, null);

        Void resultado = adapter.registrar(comando).await().indefinitely();

        assertNull(resultado);
    }

    @Test
    void falhaExplicitamenteQuandoFixtureNaoExiste() {
        MarkdownJsonMockReader reader = mock(MarkdownJsonMockReader.class);
        when(reader.readFirstJsonObject(anyString(),
                eq(ValidacaoNegocialDossieProdutoSimuladorResponse.class))).thenReturn(null);
        var mapper = new ValidacaoNegocialDossieProdutoSimuladorMapper();
        var adapter = new ValidacaoNegocialDossieProdutoSimuladorAdapter(reader, mapper);

        IllegalStateException falha = assertThrows(
                IllegalStateException.class,
                () -> adapter.registrar(null));

        assertEquals("Arquivo de mock nao encontrado no classpath: "
                        + "mock/dossieproduto/validacao-negocial-dossie-produto.md",
                falha.getMessage());
    }
}
