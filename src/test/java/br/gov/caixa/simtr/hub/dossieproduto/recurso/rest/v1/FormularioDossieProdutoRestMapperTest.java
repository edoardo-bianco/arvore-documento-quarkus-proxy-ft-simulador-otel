package br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1;

import br.gov.caixa.simtr.hub.TestFixtures;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoFormularioDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoRespostaFormularioDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoVinculoDossieDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormularioDossieProdutoRestMapperTest {

    @Test
    void converteContratoRestCompletoParaComandoInterno() {
        var comando = FormularioDossieProdutoRestMapper.paraComando(
                123L, TestFixtures.formularioDto());

        assertEquals(123L, comando.identificadorDossieProduto());
        var vinculo = comando.formularios().getFirst().vinculoDossie();
        assertEquals(10L, vinculo.fase());
        assertEquals("12345678901", vinculo.cliente().cpf());
        assertEquals(100, vinculo.produto().codigoOperacao());
        assertEquals(300, vinculo.garantia().codigoBacen());
        assertEquals("12345678000190",
                vinculo.garantia().clientesAvalistas().getFirst().cnpj());
        assertEquals(600L, vinculo.respostasFormulario().getFirst().campoFormulario());
        assertEquals(List.of("opcao-1"),
                vinculo.respostasFormulario().getFirst().opcoesSelecionadas());
        assertTrue(vinculo.respostasFormulario().getFirst().excluir());
    }

    @Test
    void preservaListasElementosObjetosECamposNulos() {
        List<DossieProdutoRespostaFormularioDto> respostas = new ArrayList<>();
        respostas.add(null);
        respostas.add(new DossieProdutoRespostaFormularioDto(null, null, null, null));
        List<DossieProdutoFormularioDto> formularios = new ArrayList<>();
        formularios.add(null);
        formularios.add(new DossieProdutoFormularioDto(null));
        formularios.add(new DossieProdutoFormularioDto(new DossieProdutoVinculoDossieDto(
                null, null, null, null, respostas)));

        var comando = FormularioDossieProdutoRestMapper.paraComando(null, formularios);

        assertNull(comando.identificadorDossieProduto());
        assertNull(comando.formularios().get(0));
        assertNull(comando.formularios().get(1).vinculoDossie());
        var vinculo = comando.formularios().get(2).vinculoDossie();
        assertNull(vinculo.fase());
        assertNull(vinculo.cliente());
        assertNull(vinculo.produto());
        assertNull(vinculo.garantia());
        assertNull(vinculo.respostasFormulario().get(0));
        assertNull(vinculo.respostasFormulario().get(1).campoFormulario());
        assertNull(vinculo.respostasFormulario().get(1).opcoesSelecionadas());
        assertNull(FormularioDossieProdutoRestMapper.paraComando(1L, null).formularios());
    }

    @Test
    void converteResultadoInclusiveNuloParaRespostaPublica() {
        assertEquals(456L, FormularioDossieProdutoRestMapper.paraResposta(
                new ResultadoAtualizacaoFormularioDossieProduto(456L)).id());
        assertNull(FormularioDossieProdutoRestMapper.paraResposta(null));
    }

    @Test
    void traduzFalhasInternasSemPerderPayload() {
        var negocio = falha(FalhaAtualizacaoFormularioDossieProduto.Tipo.NEGOCIO, 409);
        var tecnica = falha(FalhaAtualizacaoFormularioDossieProduto.Tipo.TECNICA_CLIENTE, 422);
        var timeout = falha(FalhaAtualizacaoFormularioDossieProduto.Tipo.TIMEOUT, null);

        MtrBusinessErrorException erroNegocio = assertInstanceOf(
                MtrBusinessErrorException.class,
                FormularioDossieProdutoRestMapper.paraExcecaoRest(negocio));
        assertEquals(409, erroNegocio.status());
        assertEquals("formulario-erro", erroNegocio.erro().idErro());
        assertEquals("mensagem externa", erroNegocio.erro().erros().getFirst().mensagem());
        assertEquals("detalhe", erroNegocio.erro().detalhe());
        assertEquals("stacktrace", erroNegocio.erro().stacktrace());

        assertEquals(422, assertInstanceOf(MtrClientTechnicalException.class,
                FormularioDossieProdutoRestMapper.paraExcecaoRest(tecnica)).status());
        assertEquals(500, assertInstanceOf(MtrServerErrorException.class,
                FormularioDossieProdutoRestMapper.paraExcecaoRest(timeout)).status());
    }

    private static FalhaAtualizacaoFormularioDossieProduto falha(
            FalhaAtualizacaoFormularioDossieProduto.Tipo tipo,
            Integer status
    ) {
        return new FalhaAtualizacaoFormularioDossieProduto(
                tipo,
                status,
                "simtr-dossie-produto",
                "formulario-erro",
                "MTR-FORM-001",
                List.of("mensagem externa"),
                "detalhe",
                "stacktrace",
                new IllegalStateException("causa"));
    }
}
