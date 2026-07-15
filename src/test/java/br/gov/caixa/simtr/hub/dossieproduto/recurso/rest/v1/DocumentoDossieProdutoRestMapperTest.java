package br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1;

import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ResultadoInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoAtributoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoClienteDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoGarantiaDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoPropriedadeDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoDocumentoVinculoDossieDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.InclusaoDocumentoDossieProdutoRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class DocumentoDossieProdutoRestMapperTest {

    @Test
    void converteContratoRestCompletoParaComandoInterno() {
        var request = new InclusaoDocumentoDossieProdutoRequest(
                10L,
                "container/documento.pdf",
                "GED123",
                "OBJECT_STORE",
                "RG",
                new DossieProdutoDocumentoVinculoDossieDto(
                        new DossieProdutoDocumentoClienteDto(
                                "12345678901", null, 1L),
                        700L,
                        new DossieProdutoDocumentoGarantiaDto(
                                300,
                                400,
                                500,
                                List.of(new DossieProdutoDocumentoClienteDto(
                                        null, "12345678000190", 2L)))),
                List.of(new DossieProdutoDocumentoAtributoDto(
                        "numero", "12345", "documento", List.of("opcao-1"))),
                List.of(new DossieProdutoDocumentoPropriedadeDto(
                        "origem", "pre-validacao", "documento")));

        var comando = DocumentoDossieProdutoRestMapper.paraComando(123L, request);

        assertEquals(123L, comando.identificadorDossieProduto());
        assertEquals(10L, comando.identificadorDocumento());
        assertEquals("container/documento.pdf", comando.caminhoArmazenamento());
        assertEquals("GED123", comando.codigoGed());
        assertEquals("OBJECT_STORE", comando.repositorioGed());
        assertEquals("RG", comando.tipoDocumento());
        assertEquals("12345678901", comando.vinculoDossie().cliente().cpf());
        assertEquals(1L, comando.vinculoDossie().cliente().tipoVinculo());
        assertEquals(700L, comando.vinculoDossie().elementoConteudo());
        assertEquals(300, comando.vinculoDossie().garantia().codigoBacen());
        assertEquals(400, comando.vinculoDossie().garantia().produtoOperacao());
        assertEquals(500, comando.vinculoDossie().garantia().produtoModalidade());
        assertEquals("12345678000190",
                comando.vinculoDossie().garantia().clientesAvalistas().getFirst().cnpj());
        assertEquals("numero", comando.atributos().getFirst().chave());
        assertEquals(List.of("opcao-1"),
                comando.atributos().getFirst().opcoesSelecionadas());
        assertEquals("origem", comando.propriedades().getFirst().chave());
    }

    @Test
    void preservaListasElementosObjetosECamposNulos() {
        List<DossieProdutoDocumentoClienteDto> avalistas = new ArrayList<>();
        avalistas.add(null);
        List<DossieProdutoDocumentoAtributoDto> atributos = new ArrayList<>();
        atributos.add(null);
        atributos.add(new DossieProdutoDocumentoAtributoDto(null, null, null, null));
        List<DossieProdutoDocumentoPropriedadeDto> propriedades = new ArrayList<>();
        propriedades.add(null);
        propriedades.add(new DossieProdutoDocumentoPropriedadeDto(null, null, null));
        var request = new InclusaoDocumentoDossieProdutoRequest(
                null, null, null, null, null,
                new DossieProdutoDocumentoVinculoDossieDto(
                        null, null,
                        new DossieProdutoDocumentoGarantiaDto(
                                null, null, null, avalistas)),
                atributos,
                propriedades);

        var comando = DocumentoDossieProdutoRestMapper.paraComando(null, request);

        assertNull(comando.identificadorDossieProduto());
        assertNull(comando.identificadorDocumento());
        assertNull(comando.caminhoArmazenamento());
        assertNull(comando.vinculoDossie().cliente());
        assertNull(comando.vinculoDossie().elementoConteudo());
        assertNull(comando.vinculoDossie().garantia().clientesAvalistas().getFirst());
        assertNull(comando.atributos().getFirst());
        assertNull(comando.atributos().get(1).chave());
        assertNull(comando.atributos().get(1).opcoesSelecionadas());
        assertNull(comando.propriedades().getFirst());
        assertNull(comando.propriedades().get(1).valor());

        var comandoSemRequest = DocumentoDossieProdutoRestMapper.paraComando(1L, null);
        assertEquals(1L, comandoSemRequest.identificadorDossieProduto());
        assertNull(comandoSemRequest.vinculoDossie());
        assertNull(comandoSemRequest.atributos());
        assertNull(comandoSemRequest.propriedades());
    }

    @Test
    void converteResultadoInclusiveNuloParaRespostaPublica() {
        var resposta = DocumentoDossieProdutoRestMapper.paraResposta(
                new ResultadoInclusaoDocumentoDossieProduto(456L, 789L));

        assertEquals(456L, resposta.idDocumento());
        assertEquals(789L, resposta.idInstanciaDocumento());
        assertNull(DocumentoDossieProdutoRestMapper.paraResposta(null));
    }

    @Test
    void traduzFalhasInternasSemPerderPayload() {
        var negocio = falha(FalhaInclusaoDocumentoDossieProduto.Tipo.NEGOCIO, 409);
        var tecnica = falha(FalhaInclusaoDocumentoDossieProduto.Tipo.TECNICA_CLIENTE, 422);
        var dependencia = falha(
                FalhaInclusaoDocumentoDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL, 503);
        var timeout = falha(FalhaInclusaoDocumentoDossieProduto.Tipo.TIMEOUT, null);

        MtrBusinessErrorException erroNegocio = assertInstanceOf(
                MtrBusinessErrorException.class,
                DocumentoDossieProdutoRestMapper.paraExcecaoRest(negocio));
        assertEquals(409, erroNegocio.status());
        assertEquals("documento-erro", erroNegocio.erro().idErro());
        assertEquals("mensagem externa", erroNegocio.erro().erros().getFirst().mensagem());
        assertEquals("detalhe", erroNegocio.erro().detalhe());
        assertEquals("stacktrace", erroNegocio.erro().stacktrace());

        assertEquals(422, assertInstanceOf(MtrClientTechnicalException.class,
                DocumentoDossieProdutoRestMapper.paraExcecaoRest(tecnica)).status());
        assertEquals(503, assertInstanceOf(MtrServerErrorException.class,
                DocumentoDossieProdutoRestMapper.paraExcecaoRest(dependencia)).status());
        assertEquals(500, assertInstanceOf(MtrServerErrorException.class,
                DocumentoDossieProdutoRestMapper.paraExcecaoRest(timeout)).status());
    }

    private static FalhaInclusaoDocumentoDossieProduto falha(
            FalhaInclusaoDocumentoDossieProduto.Tipo tipo,
            Integer status
    ) {
        return new FalhaInclusaoDocumentoDossieProduto(
                tipo,
                status,
                "simtr-dossie-produto",
                "documento-erro",
                "MTR-DOC-001",
                List.of("mensagem externa"),
                "detalhe",
                "stacktrace",
                new IllegalStateException("causa"));
    }
}
