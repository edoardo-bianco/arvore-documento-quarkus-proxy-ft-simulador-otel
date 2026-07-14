package br.gov.caixa.simtr.hub.parametrizacao.mapeamento;

import br.gov.caixa.simtr.hub.TestFixtures;
import br.gov.caixa.simtr.hub.parametrizacao.dominio.checklist.ChecklistVo;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.checklist.ChecklistDto;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Collections;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class ParametrizacaoMapperTest {

    @Inject
    ChecklistMapper checklistMapper;

    @Test
    void deveInjetarMapperGeradoPeloCdi() {
        assertNotNull(checklistMapper);
    }

    @Test
    void devePreservarApontamentosDoChecklist() {
        ChecklistDto dto = TestFixtures.checklistDto();

        ChecklistDto dtoFinal = checklistMapper.toDto(checklistMapper.toVo(dto));

        assertEquals(dto.identificadorNegocial(), dtoFinal.identificadorNegocial());
        assertEquals(dto.versao(), dtoFinal.versao());
        assertEquals(dto.nome(), dtoFinal.nome());
        assertEquals(dto.dataHoraCriacao(), dtoFinal.dataHoraCriacao());
        assertEquals(dto.dataHoraUltimaAlteracao(), dtoFinal.dataHoraUltimaAlteracao());
        assertEquals(dto.verificacaoPrevia(), dtoFinal.verificacaoPrevia());
        assertEquals(dto.orientacaoOperador(), dtoFinal.orientacaoOperador());
        assertEquals(dto.apontamentos().getFirst().identificadorNegocial(),
                dtoFinal.apontamentos().getFirst().identificadorNegocial());
        assertEquals(dto.apontamentos().getFirst().nome(),
                dtoFinal.apontamentos().getFirst().nome());
        assertEquals(dto.apontamentos().getFirst().descricao(),
                dtoFinal.apontamentos().getFirst().descricao());
        assertEquals(dto.apontamentos().getFirst().orientacaoOperador(),
                dtoFinal.apontamentos().getFirst().orientacaoOperador());
        assertEquals(dto.apontamentos().getFirst().indicadorReanalise(),
                dtoFinal.apontamentos().getFirst().indicadorReanalise());
        assertEquals(dto.apontamentos().getFirst().sequenciaApresentacao(),
                dtoFinal.apontamentos().getFirst().sequenciaApresentacao());
    }

    @Test
    void deveRetornarNullParaContratosNulos() {
        assertNull(checklistMapper.toVo((ChecklistDto) null));
        assertNull(checklistMapper.toDto((ChecklistVo) null));
    }

    @Test
    void devePreservarNullsDoChecklistComMapStruct() {
        ChecklistDto checklistComListaNula = new ChecklistDto(
                "Checklist minimo",
                1L,
                1,
                null,
                null,
                false,
                null,
                null
        );
        ChecklistDto checklistComItemNulo = new ChecklistDto(
                "Checklist com item nulo",
                2L,
                1,
                null,
                null,
                true,
                null,
                Collections.singletonList(null)
        );

        ChecklistDto dtoComListaNulaFinal = checklistMapper.toDto(checklistMapper.toVo(checklistComListaNula));
        ChecklistDto dtoComItemNuloFinal = checklistMapper.toDto(checklistMapper.toVo(checklistComItemNulo));
        ChecklistVo voComListaNula = checklistMapper.toVo(checklistComListaNula);

        assertNull(voComListaNula.apontamentos());
        assertNull(dtoComListaNulaFinal.apontamentos());
        assertNull(dtoComItemNuloFinal.apontamentos().getFirst());
    }
}
