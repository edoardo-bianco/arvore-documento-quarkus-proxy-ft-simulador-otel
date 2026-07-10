package br.gov.caixa.simtr.arvoredocumento.mapper.dossieproduto;

import br.gov.caixa.simtr.arvoredocumento.TestFixtures;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriacaoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoFormularioDto;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoCriacaoVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoCriadoVo;
import br.gov.caixa.simtr.arvoredocumento.domain.dossieproduto.DossieProdutoFormularioVo;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class DossieProdutoMapperTest {

    @Inject
    DossieProdutoMapper mapper;

    @Test
    void devePreservarClienteRelacionadoNaCriacao() {
        DossieProdutoCriacaoDto dto = TestFixtures.dossieCriacaoDto();

        DossieProdutoCriacaoVo vo = mapper.toVo(dto);
        DossieProdutoCriacaoDto dtoFinal = mapper.toDto(vo);

        assertEquals(dto.processo(), dtoFinal.processo());
        assertEquals(dto.chaveCorrelacaoCanal(), dtoFinal.chaveCorrelacaoCanal());
        assertEquals(dto.numeroNegocio(), dtoFinal.numeroNegocio());
        assertEquals(dto.clientes().getFirst().cpf(), dtoFinal.clientes().getFirst().cpf());
        assertEquals(dto.clientes().getFirst().clienteRelacionado().cpf(),
                dtoFinal.clientes().getFirst().clienteRelacionado().cpf());
    }

    @Test
    void devePreservarVinculoCompletoDoFormulario() {
        List<DossieProdutoFormularioDto> dto = TestFixtures.formularioDto();

        List<DossieProdutoFormularioVo> vo = mapper.toFormularioVo(dto);
        List<DossieProdutoFormularioDto> dtoFinal = mapper.toFormularioDto(vo);

        DossieProdutoFormularioDto primeiro = dtoFinal.getFirst();
        assertEquals(10L, primeiro.vinculoDossie().fase());
        assertEquals("12345678901", primeiro.vinculoDossie().cliente().cpf());
        assertEquals(100, primeiro.vinculoDossie().produto().codigoOperacao());
        assertEquals(300, primeiro.vinculoDossie().garantia().codigoBacen());
        assertEquals("12345678000190", primeiro.vinculoDossie().garantia().clientesAvalistas().getFirst().cnpj());
        assertEquals(600L, primeiro.vinculoDossie().respostasFormulario().getFirst().campoFormulario());
        assertEquals(List.of("opcao-1"), primeiro.vinculoDossie().respostasFormulario().getFirst().opcoesSelecionadas());
        assertTrue(primeiro.vinculoDossie().respostasFormulario().getFirst().excluir());
    }

    @Test
    void deveRetornarNullParaContratosNulos() {
        assertNull(mapper.toVo((DossieProdutoCriacaoDto) null));
        assertNull(mapper.toDto((DossieProdutoCriacaoVo) null));
        assertNull(mapper.toVo((DossieProdutoCriadoDto) null));
        assertNull(mapper.toDto((DossieProdutoCriadoVo) null));
        assertNull(mapper.toFormularioVo(null));
        assertNull(mapper.toFormularioDto(null));
    }

    @Test
    void devePreservarItensNulosEmListasQuandoContratoPermitir() {
        assertNull(mapper.toFormularioVo(Collections.singletonList(null)).getFirst());
        assertNull(mapper.toFormularioDto(Collections.singletonList(null)).getFirst());

        DossieProdutoCriacaoDto dto = new DossieProdutoCriacaoDto(
                1L,
                2L,
                3L,
                Collections.singletonList(null)
        );

        DossieProdutoCriacaoDto dtoFinal = mapper.toDto(mapper.toVo(dto));

        assertEquals(1, dtoFinal.clientes().size());
        assertNull(dtoFinal.clientes().getFirst());
    }
}
