package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ProdutoContratadoDossieProduto;

class ProdutoDossieProdutoMtrMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void preservaOrdemValoresENomesSnakeCaseSemSerializarExcluirAusente() throws Exception {
        var produtos = List.of(
                new ProdutoContratadoDossieProduto(101, 201, true),
                new ProdutoContratadoDossieProduto(102, 202, null));
        var comando = new ComandoAlteracaoProdutosContratadosDossieProduto(123L, produtos);

        var requisicao = new ProdutoDossieProdutoMtrMapper().paraMtr(comando);

        var jsonEsperado = OBJECT_MAPPER.readTree("""
                [
                  {
                    "codigo_operacao": 101,
                    "codigo_modalidade": 201,
                    "excluir": true
                  },
                  {
                    "codigo_operacao": 102,
                    "codigo_modalidade": 202
                  }
                ]
                """);
        assertEquals(jsonEsperado, OBJECT_MAPPER.valueToTree(requisicao));
    }

    @Test
    void preservaListaVazia() {
        var comando = new ComandoAlteracaoProdutosContratadosDossieProduto(123L, List.of());

        var requisicao = new ProdutoDossieProdutoMtrMapper().paraMtr(comando);

        assertEquals(List.of(), requisicao);
        assertEquals("[]", OBJECT_MAPPER.valueToTree(requisicao).toString());
    }
}
