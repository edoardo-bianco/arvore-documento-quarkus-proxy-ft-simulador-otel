package br.gov.caixa.simtr.arvoredocumento.mapper.parametrizacao;

import br.gov.caixa.simtr.arvoredocumento.TestFixtures;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.checklist.ChecklistDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.processo.ProcessoDto;
import br.gov.caixa.simtr.arvoredocumento.domain.parametrizacao.checklist.ChecklistVo;
import br.gov.caixa.simtr.arvoredocumento.domain.parametrizacao.processo.ProcessoVo;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class ParametrizacaoMapperTest {

    @Inject
    ProcessoMapper processoMapper;

    @Inject
    ChecklistMapper checklistMapper;

    @Test
    void devePreservarCamposPrincipaisDoProcesso() {
        ProcessoDto dto = TestFixtures.processoDto();

        ProcessoDto dtoFinal = processoMapper.toDto(processoMapper.toVo(dto));

        assertEquals(dto.identificadorNegocial(), dtoFinal.identificadorNegocial());
        assertEquals(dto.nome(), dtoFinal.nome());
        assertEquals(dto.indicadorProdutoObrigatorio(), dtoFinal.indicadorProdutoObrigatorio());
        assertEquals(dto.fases().size(), dtoFinal.fases().size());
        assertEquals(dto.produtos().size(), dtoFinal.produtos().size());
        assertEquals(dto.relacionamentos().size(), dtoFinal.relacionamentos().size());
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
    void devePreservarEstruturaCompletaDoProcesso() {
        ProcessoDto dto = TestFixtures.processoDto();

        ProcessoDto dtoFinal = processoMapper.toDto(processoMapper.toVo(dto));

        assertEquals(dto.macroprocesso().nome(), dtoFinal.macroprocesso().nome());
        assertEquals(dto.relacionamentos().getFirst().camposFormulario().getFirst().label(),
                dtoFinal.relacionamentos().getFirst().camposFormulario().getFirst().label());
        assertEquals(dto.produtos().getFirst().garantias().getFirst().codigoBacen(),
                dtoFinal.produtos().getFirst().garantias().getFirst().codigoBacen());
        assertEquals(dto.fases().getFirst().produtos().getFirst().codigoOperacao(),
                dtoFinal.fases().getFirst().produtos().getFirst().codigoOperacao());
        assertEquals(dto.documentos().getFirst().funcaoDocumental().tiposDocumento().getFirst().codigoTipologia(),
                dtoFinal.documentos().getFirst().funcaoDocumental().tiposDocumento().getFirst().codigoTipologia());
        assertEquals(dto.checklist().identificadorChecklist(), dtoFinal.checklist().identificadorChecklist());
    }

    @Test
    void deveRetornarNullParaContratosNulos() {
        assertNull(processoMapper.toVo((ProcessoDto) null));
        assertNull(processoMapper.toDto((ProcessoVo) null));
        assertNull(checklistMapper.toVo((ChecklistDto) null));
        assertNull(checklistMapper.toDto((ChecklistVo) null));
    }

    @Test
    void devePreservarItensNulosEmListasQuandoContratoPermitir() {
        ProcessoDto dto = new ProcessoDto(
                1L,
                "Processo minimo",
                true,
                "01/01/2026",
                false,
                null,
                Collections.singletonList(null),
                Collections.singletonList(null),
                Collections.singletonList(null),
                Collections.singletonList(null),
                null
        );

        ProcessoDto dtoFinal = processoMapper.toDto(processoMapper.toVo(dto));

        assertNull(dtoFinal.macroprocesso());
        assertNull(dtoFinal.relacionamentos().getFirst());
        assertNull(dtoFinal.produtos().getFirst());
        assertNull(dtoFinal.fases().getFirst());
        assertNull(dtoFinal.documentos().getFirst());
        assertNull(dtoFinal.checklist());
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
