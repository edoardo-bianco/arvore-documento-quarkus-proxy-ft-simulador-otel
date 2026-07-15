package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.mapper;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.dto.v1.checklist.ChecklistMtrResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChecklistMtrMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void desserializaSnakeCaseEMapeiaTodosOsCamposParaODominio() throws Exception {
        String json = """
                {
                  "nome": "Checklist MTR",
                  "identificador_negocial": 1000012583,
                  "versao": 7,
                  "data_hora_criacao": null,
                  "data_hora_ultima_alteracao": "14/07/2026 12:00:00",
                  "verificacao_previa": false,
                  "orientacao_operador": null,
                  "apontamentos": [null, {
                    "identificador_negocial": 2001,
                    "nome": null,
                    "descricao": "Descricao preservada",
                    "orientacao_operador": null,
                    "indicador_reanalise": false,
                    "sequencia_apresentacao": 1
                  }]
                }
                """;

        var resposta = OBJECT_MAPPER.readValue(json, ChecklistMtrResponse.class);
        var checklist = new ChecklistMtrMapper().paraDominio(resposta);

        assertEquals("Checklist MTR", checklist.nome());
        assertEquals(1000012583L, checklist.identificadorNegocial());
        assertEquals(7, checklist.versao());
        assertNull(checklist.dataHoraCriacao());
        assertEquals("14/07/2026 12:00:00", checklist.dataHoraUltimaAlteracao());
        assertFalse(checklist.verificacaoPrevia());
        assertNull(checklist.orientacaoOperador());
        assertNull(checklist.apontamentos().getFirst());
        assertEquals(2001L, checklist.apontamentos().get(1).identificadorNegocial());
        assertNull(checklist.apontamentos().get(1).nome());
        assertEquals("Descricao preservada", checklist.apontamentos().get(1).descricao());
        assertFalse(checklist.apontamentos().get(1).indicadorReanalise());
        assertEquals(1, checklist.apontamentos().get(1).sequenciaApresentacao());
    }

    @Test
    void preservaRespostaEListaNulas() {
        var mapper = new ChecklistMtrMapper();
        var resposta = new ChecklistMtrResponse(
                null, null, null, null, null, null, null, null);

        assertNull(mapper.paraDominio(null));
        assertNull(mapper.paraDominio(resposta).apontamentos());
    }

    @Test
    void preservaElementosNulosNaLista() {
        var apontamento = new ChecklistMtrResponse.Apontamento(
                2001L, null, null, null, false, 1);
        var resposta = new ChecklistMtrResponse(
                null, null, null, null, null, null, null,
                Arrays.asList(null, apontamento));

        var checklist = new ChecklistMtrMapper().paraDominio(resposta);

        assertNull(checklist.apontamentos().getFirst());
        assertEquals(2001L, checklist.apontamentos().get(1).identificadorNegocial());
    }
}
