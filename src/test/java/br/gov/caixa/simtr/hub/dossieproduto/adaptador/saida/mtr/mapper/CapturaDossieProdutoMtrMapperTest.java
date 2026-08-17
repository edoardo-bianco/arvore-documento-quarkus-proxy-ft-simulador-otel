package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.captura.CapturaDossieProdutoMtrResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CapturaDossieProdutoMtrMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void desserializaIdInt64EMapeiaParaResultadoInterno() throws Exception {
        var resposta = OBJECT_MAPPER.readValue(
                """
                        { "id": 9223372036854775807 }
                        """,
                CapturaDossieProdutoMtrResponse.class);

        var resultado = new CapturaDossieProdutoMtrMapper().paraResultado(resposta);

        assertEquals(Long.MAX_VALUE, resposta.id());
        assertEquals(Long.MAX_VALUE, resultado.identificadorDossieProduto());
    }

    @Test
    void declaraNomeJsonExplicitoNoContratoMtr() throws Exception {
        var jsonProperty = CapturaDossieProdutoMtrResponse.class
                .getDeclaredMethod("id")
                .getAnnotation(JsonProperty.class);

        assertNotNull(jsonProperty);
        assertEquals("id", jsonProperty.value());
    }

    @Test
    void mantemRespostaEIdNulosDetectaveisPeloAdapter() {
        var mapper = new CapturaDossieProdutoMtrMapper();

        var resultadoComIdNulo = mapper.paraResultado(
                new CapturaDossieProdutoMtrResponse(null));

        assertNull(mapper.paraResultado(null));
        assertNotNull(resultadoComIdNulo);
        assertNull(resultadoComIdNulo.identificadorDossieProduto());
    }

    @Test
    void mantemIdDivergenteDetectavelPeloAdapter() {
        var idSolicitado = 123L;
        var idRecebido = 456L;

        var resultado = new CapturaDossieProdutoMtrMapper().paraResultado(
                new CapturaDossieProdutoMtrResponse(idRecebido));

        assertEquals(idRecebido, resultado.identificadorDossieProduto());
        assertNotEquals(idSolicitado, resultado.identificadorDossieProduto());
    }
}
