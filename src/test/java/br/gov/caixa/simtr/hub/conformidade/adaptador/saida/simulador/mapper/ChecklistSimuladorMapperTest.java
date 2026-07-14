package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.simulador.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.simulador.dto.ChecklistSimuladorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ChecklistSimuladorMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void leContratoSnakeCaseProprioEMapeiaNulosEApontamentos() throws Exception {
        String json = """
                {
                  "nome": "Checklist simulado",
                  "identificador_negocial": 1000012583,
                  "versao": 1,
                  "data_hora_criacao": null,
                  "data_hora_ultima_alteracao": "13/05/2026 16:27:41",
                  "verificacao_previa": false,
                  "orientacao_operador": null,
                  "apontamentos": [null, {
                    "identificador_negocial": 1000012577,
                    "nome": null,
                    "descricao": "Descricao preservada",
                    "orientacao_operador": null,
                    "indicador_reanalise": false,
                    "sequencia_apresentacao": 1
                  }]
                }
                """;

        var resposta = OBJECT_MAPPER.readValue(json, ChecklistSimuladorResponse.class);
        var checklist = new ChecklistSimuladorMapper().paraDominio(resposta);

        assertEquals("Checklist simulado", checklist.nome());
        assertEquals(1000012583L, checklist.identificadorNegocial());
        assertEquals(1, checklist.versao());
        assertNull(checklist.dataHoraCriacao());
        assertEquals("13/05/2026 16:27:41", checklist.dataHoraUltimaAlteracao());
        assertFalse(checklist.verificacaoPrevia());
        assertNull(checklist.orientacaoOperador());
        assertNull(checklist.apontamentos().getFirst());
        assertEquals(1000012577L,
                checklist.apontamentos().get(1).identificadorNegocial());
        assertNull(checklist.apontamentos().get(1).nome());
        assertEquals("Descricao preservada", checklist.apontamentos().get(1).descricao());
    }

    @Test
    void preservaRespostaListaEElementosNulos() {
        var mapper = new ChecklistSimuladorMapper();
        var semLista = new ChecklistSimuladorResponse(
                null, null, null, null, null, null, null, null
        );
        var comElementos = new ChecklistSimuladorResponse(
                null, null, null, null, null, null, null,
                Arrays.asList(null, new ChecklistSimuladorResponse.Apontamento(
                        2001L, null, null, null, false, 1
                ))
        );

        assertNull(mapper.paraDominio(null));
        assertNull(mapper.paraDominio(semLista).apontamentos());
        assertNull(mapper.paraDominio(comElementos).apontamentos().getFirst());
        assertEquals(2001L, mapper.paraDominio(comElementos).apontamentos().get(1)
                .identificadorNegocial());
    }
}
