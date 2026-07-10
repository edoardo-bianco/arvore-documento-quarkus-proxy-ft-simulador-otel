package br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class ChecklistReferenciaDtoDeserializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void desserializaObjetoComNumeros() throws Exception {
        ChecklistReferenciaDto dto = objectMapper.readValue(
                """
                {
                  "identificador_checklist": 10,
                  "versao_checklist": 2
                }
                """,
                ChecklistReferenciaDto.class
        );

        assertEquals(10L, dto.identificadorChecklist());
        assertEquals(2, dto.versaoChecklist());
    }

    @Test
    void desserializaArrayComNumerosEmTexto() throws Exception {
        ChecklistReferenciaDto dto = objectMapper.readValue(
                """
                [
                  {
                    "identificador_checklist": "11",
                    "versao_checklist": "3"
                  }
                ]
                """,
                ChecklistReferenciaDto.class
        );

        assertEquals(11L, dto.identificadorChecklist());
        assertEquals(3, dto.versaoChecklist());
    }

    @Test
    void retornaNullParaArrayVazioOuValorNaoObjeto() throws Exception {
        assertNull(objectMapper.readValue("[]", ChecklistReferenciaDto.class));
        assertNull(objectMapper.readValue("\"valor\"", ChecklistReferenciaDto.class));
        assertNull(objectMapper.readValue("null", ChecklistReferenciaDto.class));
    }
}
