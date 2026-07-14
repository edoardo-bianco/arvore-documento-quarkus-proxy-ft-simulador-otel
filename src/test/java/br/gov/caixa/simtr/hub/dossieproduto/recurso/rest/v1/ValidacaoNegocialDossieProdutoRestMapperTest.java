package br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1;

import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaRegistroValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialClienteAvalistaDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialGarantiaDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialParecerApontamentoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialProdutoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialRespostaFormularioDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialVerificacaoDto;
import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.ValidacaoNegocialDossieProdutoRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class ValidacaoNegocialDossieProdutoRestMapperTest {

    @Test
    void converteContratoRestCompletoParaComandoInterno() {
        var request = new ValidacaoNegocialDossieProdutoRequest(
                List.of(new DossieProdutoValidacaoNegocialVerificacaoDto(
                        1122928L,
                        6592L,
                        2,
                        true,
                        List.of(new DossieProdutoValidacaoNegocialParecerApontamentoDto(
                                1000012877L, "APROVADO", "comentario", false, 1.0)),
                        new DossieProdutoValidacaoNegocialGarantiaDto(
                                300,
                                List.of(new DossieProdutoValidacaoNegocialClienteAvalistaDto(
                                        "12345678901", null))),
                        new DossieProdutoValidacaoNegocialProdutoDto(100, 200),
                        true)),
                List.of(new DossieProdutoValidacaoNegocialRespostaFormularioDto(
                        1000011689L, "resposta", List.of("2"))));

        var comando = ValidacaoNegocialDossieProdutoRestMapper.paraComando(123L, request);

        assertEquals(123L, comando.identificadorDossieProduto());
        var verificacao = comando.verificacoes().getFirst();
        assertEquals(1122928L, verificacao.identificadorInstanciaDocumento());
        assertEquals(6592L, verificacao.identificadorChecklist());
        assertEquals(2, verificacao.versaoChecklist());
        assertEquals(true, verificacao.analiseRealizada());
        assertEquals(1000012877L,
                verificacao.parecerApontamentos().getFirst().identificadorApontamento());
        assertEquals("comentario",
                verificacao.parecerApontamentos().getFirst().comentario());
        assertEquals(300, verificacao.garantia().codigoBacen());
        assertEquals("12345678901",
                verificacao.garantia().clientesAvalistas().getFirst().cpf());
        assertEquals(100, verificacao.produto().codigoOperacao());
        assertEquals(true, verificacao.previo());
        assertEquals(1000011689L,
                comando.respostasFormulario().getFirst().campoFormulario());
        assertEquals(List.of("2"),
                comando.respostasFormulario().getFirst().opcoesSelecionadas());
    }

    @Test
    void preservaListasElementosObjetosECamposNulos() {
        List<DossieProdutoValidacaoNegocialParecerApontamentoDto> pareceres =
                new ArrayList<>();
        pareceres.add(null);
        List<DossieProdutoValidacaoNegocialClienteAvalistaDto> avalistas =
                new ArrayList<>();
        avalistas.add(null);
        List<DossieProdutoValidacaoNegocialVerificacaoDto> verificacoes =
                new ArrayList<>();
        verificacoes.add(null);
        verificacoes.add(new DossieProdutoValidacaoNegocialVerificacaoDto(
                null, 6592L, 2, true, pareceres,
                new DossieProdutoValidacaoNegocialGarantiaDto(null, avalistas),
                null, null));
        List<DossieProdutoValidacaoNegocialRespostaFormularioDto> respostas =
                new ArrayList<>();
        respostas.add(null);
        respostas.add(new DossieProdutoValidacaoNegocialRespostaFormularioDto(
                1000011689L, null, null));
        var request = new ValidacaoNegocialDossieProdutoRequest(verificacoes, respostas);

        var comando = ValidacaoNegocialDossieProdutoRestMapper.paraComando(null, request);

        assertNull(comando.identificadorDossieProduto());
        assertNull(comando.verificacoes().getFirst());
        assertNull(comando.verificacoes().get(1).identificadorInstanciaDocumento());
        assertNull(comando.verificacoes().get(1).parecerApontamentos().getFirst());
        assertNull(comando.verificacoes().get(1).garantia().codigoBacen());
        assertNull(comando.verificacoes().get(1).garantia().clientesAvalistas().getFirst());
        assertNull(comando.verificacoes().get(1).produto());
        assertNull(comando.verificacoes().get(1).previo());
        assertNull(comando.respostasFormulario().getFirst());
        assertNull(comando.respostasFormulario().get(1).resposta());
        assertNull(comando.respostasFormulario().get(1).opcoesSelecionadas());

        var comandoSemRequest =
                ValidacaoNegocialDossieProdutoRestMapper.paraComando(1L, null);
        assertEquals(1L, comandoSemRequest.identificadorDossieProduto());
        assertNull(comandoSemRequest.verificacoes());
        assertNull(comandoSemRequest.respostasFormulario());
    }

    @Test
    void traduzFalhasInternasSemPerderPayload() {
        var negocio = falha(FalhaRegistroValidacaoNegocialDossieProduto.Tipo.NEGOCIO, 409);
        var tecnica = falha(
                FalhaRegistroValidacaoNegocialDossieProduto.Tipo.TECNICA_CLIENTE, 422);
        var dependencia = falha(
                FalhaRegistroValidacaoNegocialDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                503);
        var timeout = falha(FalhaRegistroValidacaoNegocialDossieProduto.Tipo.TIMEOUT, null);

        MtrBusinessErrorException erroNegocio = assertInstanceOf(
                MtrBusinessErrorException.class,
                ValidacaoNegocialDossieProdutoRestMapper.paraExcecaoRest(negocio));
        assertEquals(409, erroNegocio.status());
        assertEquals("validacao-erro", erroNegocio.erro().idErro());
        assertEquals("mensagem externa",
                erroNegocio.erro().erros().getFirst().mensagem());
        assertEquals("detalhe", erroNegocio.erro().detalhe());
        assertEquals("stacktrace", erroNegocio.erro().stacktrace());

        assertEquals(422, assertInstanceOf(MtrClientTechnicalException.class,
                ValidacaoNegocialDossieProdutoRestMapper.paraExcecaoRest(tecnica)).status());
        assertEquals(503, assertInstanceOf(MtrServerErrorException.class,
                ValidacaoNegocialDossieProdutoRestMapper.paraExcecaoRest(dependencia)).status());
        assertEquals(500, assertInstanceOf(MtrServerErrorException.class,
                ValidacaoNegocialDossieProdutoRestMapper.paraExcecaoRest(timeout)).status());
    }

    private static FalhaRegistroValidacaoNegocialDossieProduto falha(
            FalhaRegistroValidacaoNegocialDossieProduto.Tipo tipo,
            Integer status
    ) {
        return new FalhaRegistroValidacaoNegocialDossieProduto(
                tipo,
                status,
                "simtr-dossie-produto",
                "validacao-erro",
                "MTR-VAL-001",
                List.of("mensagem externa"),
                "detalhe",
                "stacktrace",
                new IllegalStateException("causa"));
    }
}
