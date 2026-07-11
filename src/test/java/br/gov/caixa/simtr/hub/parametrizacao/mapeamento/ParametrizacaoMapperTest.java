package br.gov.caixa.simtr.hub.parametrizacao.mapeamento;

import br.gov.caixa.simtr.hub.TestFixtures;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.checklist.ChecklistDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.CampoFormularioDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.DocumentoDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.FaseDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.FuncaoDocumentalDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.GarantiaDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.ProcessoDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.ProdutoDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.RelacionamentoDto;
import br.gov.caixa.simtr.hub.parametrizacao.recurso.rest.v1.dto.processo.TipoDocumentoDto;
import br.gov.caixa.simtr.hub.parametrizacao.dominio.checklist.ChecklistVo;
import br.gov.caixa.simtr.hub.parametrizacao.dominio.processo.ProcessoVo;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
class ParametrizacaoMapperTest {

    @Inject
    ProcessoMapper processoMapper;

    @Inject
    ChecklistMapper checklistMapper;

    @Test
    void deveInjetarMappersGeradosPeloCdi() {
        assertNotNull(processoMapper);
        assertNotNull(checklistMapper);
    }

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
        assertEquals(dto.relacionamentos().getFirst().documentos().getFirst().obrigatorio(),
                dtoFinal.relacionamentos().getFirst().documentos().getFirst().obrigatorio());
        assertEquals(dto.produtos().getFirst().camposFormulario().getFirst().opcoesDisponiveis().getFirst().valorOpcao(),
                dtoFinal.produtos().getFirst().camposFormulario().getFirst().opcoesDisponiveis().getFirst().valorOpcao());
        assertEquals(dto.produtos().getFirst().documentos().getFirst().tipoDocumento().nome(),
                dtoFinal.produtos().getFirst().documentos().getFirst().tipoDocumento().nome());
        assertEquals(dto.produtos().getFirst().garantias().getFirst().codigoBacen(),
                dtoFinal.produtos().getFirst().garantias().getFirst().codigoBacen());
        assertEquals(dto.produtos().getFirst().checklist().versaoChecklist(),
                dtoFinal.produtos().getFirst().checklist().versaoChecklist());
        assertEquals(dto.fases().getFirst().produtos().getFirst().codigoOperacao(),
                dtoFinal.fases().getFirst().produtos().getFirst().codigoOperacao());
        assertEquals(dto.fases().getFirst().garantias().getFirst().nomeGarantia(),
                dtoFinal.fases().getFirst().garantias().getFirst().nomeGarantia());
        assertEquals(dto.fases().getFirst().camposFormulario().getFirst().tipo(),
                dtoFinal.fases().getFirst().camposFormulario().getFirst().tipo());
        assertEquals(dto.fases().getFirst().documentos().getFirst().funcaoDocumental().nome(),
                dtoFinal.fases().getFirst().documentos().getFirst().funcaoDocumental().nome());
        assertEquals(dto.fases().getFirst().checklist().getFirst().identificadorChecklist(),
                dtoFinal.fases().getFirst().checklist().getFirst().identificadorChecklist());
        assertEquals(dto.documentos().getFirst().funcaoDocumental().tiposDocumento().getFirst().codigoTipologia(),
                dtoFinal.documentos().getFirst().funcaoDocumental().tiposDocumento().getFirst().codigoTipologia());
        assertEquals(dto.documentos().getFirst().tipoDocumento().checklist().identificadorChecklist(),
                dtoFinal.documentos().getFirst().tipoDocumento().checklist().identificadorChecklist());
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
    void devePreservarListasEObjetosAninhadosNulosDoProcesso() {
        CampoFormularioDto campo = new CampoFormularioDto(
                10L,
                "Campo minimo",
                true,
                true,
                null,
                null,
                null,
                "TEXTO",
                null,
                null,
                null,
                null,
                null,
                false,
                null
        );
        DocumentoDto documento = new DocumentoDto(
                new FuncaoDocumentalDto("Funcao", null, null),
                new TipoDocumentoDto("RG", "Registro Geral", true, false, true, null),
                null
        );
        GarantiaDto garantia = new GarantiaDto(1L, "Garantia", false, null, null, null);
        ProdutoDto produto = new ProdutoDto(2L, 3L, "Produto", null, null, List.of(garantia), null);
        RelacionamentoDto relacionamento = new RelacionamentoDto(
                4L,
                "Relacionamento",
                "PF",
                true,
                true,
                false,
                false,
                List.of(campo),
                List.of(documento)
        );
        FaseDto fase = new FaseDto(
                5L,
                "Fase",
                true,
                "01/01/2026",
                1,
                "Orientacao",
                List.of(produto),
                List.of(garantia),
                List.of(campo),
                List.of(documento),
                null
        );
        ProcessoDto dto = new ProcessoDto(
                6L,
                "Processo com nulos internos",
                true,
                "01/01/2026",
                false,
                null,
                List.of(relacionamento),
                List.of(produto),
                List.of(fase),
                List.of(documento),
                null
        );

        ProcessoDto dtoFinal = processoMapper.toDto(processoMapper.toVo(dto));

        assertNull(dtoFinal.macroprocesso());
        assertNull(dtoFinal.checklist());
        assertNull(dtoFinal.relacionamentos().getFirst().camposFormulario().getFirst().opcoesDisponiveis());
        assertNull(dtoFinal.documentos().getFirst().funcaoDocumental().tiposDocumento());
        assertNull(dtoFinal.documentos().getFirst().funcaoDocumental().checklist());
        assertNull(dtoFinal.documentos().getFirst().tipoDocumento().checklist());
        assertNull(dtoFinal.produtos().getFirst().camposFormulario());
        assertNull(dtoFinal.produtos().getFirst().documentos());
        assertNull(dtoFinal.produtos().getFirst().checklist());
        assertNull(dtoFinal.produtos().getFirst().garantias().getFirst().camposFormulario());
        assertNull(dtoFinal.produtos().getFirst().garantias().getFirst().documentos());
        assertNull(dtoFinal.produtos().getFirst().garantias().getFirst().checklist());
        assertNull(dtoFinal.fases().getFirst().checklist());
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
