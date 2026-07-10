package br.gov.caixa.simtr.hub.dossieproduto.mapeamento;

import br.gov.caixa.simtr.hub.TestFixtures;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoClienteDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoCriacaoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoCriadoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoInclusaoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoFormularioDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoVinculoDossieDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoVinculoGarantiaDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialVerificacaoDto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoCriacaoVo;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoCriadoVo;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoDocumentoCriadoVo;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoDocumentoInclusaoVo;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoFormularioVo;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.DossieProdutoValidacaoNegocialVo;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class DossieProdutoMapperTest {

    @Inject
    DossieProdutoMapper mapper;

    @Test
    void deveInjetarMapperGeradoPeloCdi() {
        assertNotNull(mapper);
    }

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
    void devePreservarDossieProdutoCriado() {
        DossieProdutoCriadoDto dto = new DossieProdutoCriadoDto(123L);

        DossieProdutoCriadoVo vo = mapper.toVo(dto);
        DossieProdutoCriadoDto dtoFinal = mapper.toDto(vo);

        assertEquals(dto.id(), dtoFinal.id());
    }

    @Test
    void devePreservarDocumentoCriado() {
        DossieProdutoDocumentoCriadoDto dto = new DossieProdutoDocumentoCriadoDto(456L, 789L);

        DossieProdutoDocumentoCriadoVo vo = mapper.toVo(dto);
        DossieProdutoDocumentoCriadoDto dtoFinal = mapper.toDto(vo);

        assertEquals(dto.idDocumento(), dtoFinal.idDocumento());
        assertEquals(dto.idInstanciaDocumento(), dtoFinal.idInstanciaDocumento());
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
    void devePreservarDocumentoInclusaoCompleto() {
        DossieProdutoDocumentoInclusaoDto dto = TestFixtures.documentoInclusaoDto();

        DossieProdutoDocumentoInclusaoVo vo = mapper.toVo(dto);
        DossieProdutoDocumentoInclusaoDto dtoFinal = mapper.toDto(vo);

        assertEquals(dto.pathStorage(), dtoFinal.pathStorage());
        assertEquals(dto.codigoGed(), dtoFinal.codigoGed());
        assertEquals(dto.objectStoreGed(), dtoFinal.objectStoreGed());
        assertEquals(dto.tipoDocumento(), dtoFinal.tipoDocumento());
        assertEquals(700L, dtoFinal.vinculoDossie().elementoConteudo());
        assertEquals("12345678901", dtoFinal.vinculoDossie().cliente().cpf());
        assertEquals(300, dtoFinal.vinculoDossie().garantia().codigoBacen());
        assertEquals("12345678000190", dtoFinal.vinculoDossie().garantia().clienteAvalista().getFirst().cnpj());
        assertEquals("numero", dtoFinal.atributos().getFirst().chave());
        assertEquals(List.of("opcao-1"), dtoFinal.atributos().getFirst().opcoesSelecionadas());
        assertEquals("origem", dtoFinal.propriedades().getFirst().chave());
    }

    @Test
    void devePreservarValidacaoNegocialCompleta() {
        DossieProdutoValidacaoNegocialDto dto = TestFixtures.validacaoNegocialDto();

        DossieProdutoValidacaoNegocialVo vo = mapper.toVo(dto);
        DossieProdutoValidacaoNegocialDto dtoFinal = mapper.toDto(vo);

        assertEquals(1122928L, dtoFinal.verificacoes().getFirst().identificadorInstanciaDocumento());
        assertEquals(6592L, dtoFinal.verificacoes().getFirst().identificadorChecklist());
        assertEquals(2, dtoFinal.verificacoes().getFirst().versaoChecklist());
        assertTrue(dtoFinal.verificacoes().getFirst().analiseRealizada());
        assertEquals(3, dtoFinal.verificacoes().getFirst().parecerApontamentos().size());
        assertEquals(1000012877L,
                dtoFinal.verificacoes().getFirst().parecerApontamentos().getFirst().identificadorApontamento());
        assertEquals("APROVADO", dtoFinal.verificacoes().getFirst().parecerApontamentos().getFirst().resultado());
        assertEquals(1.0, dtoFinal.verificacoes().getFirst().parecerApontamentos().getFirst().indiceIa());
        assertEquals(300, dtoFinal.verificacoes().getFirst().garantia().codigoBacen());
        assertEquals("12345678901",
                dtoFinal.verificacoes().getFirst().garantia().clientesAvalistas().getFirst().cpf());
        assertEquals(100, dtoFinal.verificacoes().getFirst().produto().codigoOperacao());
        assertTrue(dtoFinal.verificacoes().getFirst().previo());
        assertEquals(2, dtoFinal.respostasFormulario().size());
        assertEquals(1000011689L, dtoFinal.respostasFormulario().getFirst().campoFormulario());
        assertEquals(List.of("2"), dtoFinal.respostasFormulario().get(1).opcoesSelecionadas());
    }

    @Test
    void deveRetornarNullParaContratosNulos() {
        assertNull(mapper.toVo((DossieProdutoCriacaoDto) null));
        assertNull(mapper.toDto((DossieProdutoCriacaoVo) null));
        assertNull(mapper.toVo((DossieProdutoCriadoDto) null));
        assertNull(mapper.toDto((DossieProdutoCriadoVo) null));
        assertNull(mapper.toFormularioVo(null));
        assertNull(mapper.toFormularioDto(null));
        assertNull(mapper.toVo((DossieProdutoDocumentoInclusaoDto) null));
        assertNull(mapper.toDto((DossieProdutoDocumentoInclusaoVo) null));
        assertNull(mapper.toVo((DossieProdutoDocumentoCriadoDto) null));
        assertNull(mapper.toDto((DossieProdutoDocumentoCriadoVo) null));
        assertNull(mapper.toVo((DossieProdutoValidacaoNegocialDto) null));
        assertNull(mapper.toDto((DossieProdutoValidacaoNegocialVo) null));
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
        DossieProdutoDocumentoInclusaoDto documentoFinal = mapper.toDto(mapper.toVo(
                new DossieProdutoDocumentoInclusaoDto(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Collections.singletonList(null),
                        Collections.singletonList(null)
                )
        ));

        assertEquals(1, dtoFinal.clientes().size());
        assertNull(dtoFinal.clientes().getFirst());
        assertEquals(1, documentoFinal.atributos().size());
        assertNull(documentoFinal.atributos().getFirst());
        assertEquals(1, documentoFinal.propriedades().size());
        assertNull(documentoFinal.propriedades().getFirst());

        DossieProdutoValidacaoNegocialDto validacaoFinal = mapper.toDto(mapper.toVo(
                new DossieProdutoValidacaoNegocialDto(
                        Collections.singletonList(null),
                        Collections.singletonList(null)
                )
        ));

        assertEquals(1, validacaoFinal.verificacoes().size());
        assertNull(validacaoFinal.verificacoes().getFirst());
        assertEquals(1, validacaoFinal.respostasFormulario().size());
        assertNull(validacaoFinal.respostasFormulario().getFirst());
    }

    @Test
    void devePreservarObjetosAninhadosNulos() {
        DossieProdutoCriacaoDto criacao = new DossieProdutoCriacaoDto(
                1L,
                2L,
                3L,
                List.of(new DossieProdutoClienteDto("12345678901", null, 1L, null, 1))
        );
        List<DossieProdutoFormularioDto> formulario = List.of(
                new DossieProdutoFormularioDto(new DossieProdutoVinculoDossieDto(
                        10L,
                        null,
                        null,
                        null,
                        null
                )),
                new DossieProdutoFormularioDto(new DossieProdutoVinculoDossieDto(
                        11L,
                        null,
                        null,
                        new DossieProdutoVinculoGarantiaDto(1, 2, 3, null),
                        null
                ))
        );

        DossieProdutoCriacaoDto criacaoFinal = mapper.toDto(mapper.toVo(criacao));
        List<DossieProdutoFormularioDto> formularioFinal = mapper.toFormularioDto(mapper.toFormularioVo(formulario));

        assertNull(criacaoFinal.clientes().getFirst().clienteRelacionado());
        assertNull(formularioFinal.getFirst().vinculoDossie().cliente());
        assertNull(formularioFinal.getFirst().vinculoDossie().produto());
        assertNull(formularioFinal.getFirst().vinculoDossie().garantia());
        assertNull(formularioFinal.getFirst().vinculoDossie().respostasFormulario());
        assertNull(formularioFinal.get(1).vinculoDossie().garantia().clientesAvalistas());

        DossieProdutoDocumentoInclusaoDto documento = new DossieProdutoDocumentoInclusaoDto(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        DossieProdutoDocumentoInclusaoDto documentoFinal = mapper.toDto(mapper.toVo(documento));

        assertNull(documentoFinal.vinculoDossie());
        assertNull(documentoFinal.atributos());
        assertNull(documentoFinal.propriedades());

        DossieProdutoValidacaoNegocialDto validacao = new DossieProdutoValidacaoNegocialDto(
                List.of(new DossieProdutoValidacaoNegocialVerificacaoDto(
                        null,
                        1L,
                        1,
                        true,
                        Collections.singletonList(null),
                        null,
                        null,
                        null
                )),
                null
        );
        DossieProdutoValidacaoNegocialDto validacaoFinal = mapper.toDto(mapper.toVo(validacao));

        assertNull(validacaoFinal.verificacoes().getFirst().identificadorInstanciaDocumento());
        assertEquals(1, validacaoFinal.verificacoes().getFirst().parecerApontamentos().size());
        assertNull(validacaoFinal.verificacoes().getFirst().parecerApontamentos().getFirst());
        assertNull(validacaoFinal.verificacoes().getFirst().garantia());
        assertNull(validacaoFinal.verificacoes().getFirst().produto());
        assertNull(validacaoFinal.verificacoes().getFirst().previo());
        assertNull(validacaoFinal.respostasFormulario());
    }
}
