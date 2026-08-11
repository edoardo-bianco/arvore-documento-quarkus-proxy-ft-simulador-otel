package br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.dto.AlteracaoProdutoDossieProdutoRequest;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaAlteracaoProdutosContratadosDossieProduto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProdutoDossieProdutoRestMapperTest {

    @Test
    void converteContratoRestParaComandoInternoSemAcoplarAoContratoMtr() {
        var request = List.of(
                new AlteracaoProdutoDossieProdutoRequest(100, 200, false),
                new AlteracaoProdutoDossieProdutoRequest(300, 400, true));

        var comando = ProdutoDossieProdutoRestMapper.paraComando(123L, request);

        assertEquals(123L, comando.identificadorDossieProduto());
        assertEquals(2, comando.produtos().size());
        assertEquals(100, comando.produtos().getFirst().codigoOperacao());
        assertEquals(200, comando.produtos().getFirst().codigoModalidade());
        assertEquals(false, comando.produtos().getFirst().excluir());
        assertEquals(300, comando.produtos().get(1).codigoOperacao());
        assertEquals(400, comando.produtos().get(1).codigoModalidade());
        assertEquals(true, comando.produtos().get(1).excluir());
    }

    @Test
    void preservaListaVaziaECamposOpcionais() {
        var vazio = ProdutoDossieProdutoRestMapper.paraComando(123L, List.of());
        var comExcluirAusente = ProdutoDossieProdutoRestMapper.paraComando(
                123L,
                List.of(new AlteracaoProdutoDossieProdutoRequest(100, 200, null)));

        assertEquals(List.of(), vazio.produtos());
        assertNull(comExcluirAusente.produtos().getFirst().excluir());
    }

    @Test
    void preservaNulosParaQueAValidacaoDaBordaContinueResponsavelPelaEntrada() {
        List<AlteracaoProdutoDossieProdutoRequest> request = new ArrayList<>();
        request.add(null);

        var comando = ProdutoDossieProdutoRestMapper.paraComando(null, request);
        var comandoSemCorpo = ProdutoDossieProdutoRestMapper.paraComando(1L, null);

        assertNull(comando.identificadorDossieProduto());
        assertNull(comando.produtos().getFirst());
        assertEquals(1L, comandoSemCorpo.identificadorDossieProduto());
        assertNull(comandoSemCorpo.produtos());
    }

    @Test
    void traduzFalhasInternasSemPerderPayload() {
        var negocio = falha(
                FalhaAlteracaoProdutosContratadosDossieProduto.Tipo.NEGOCIO, 409);
        var tecnica = falha(
                FalhaAlteracaoProdutosContratadosDossieProduto.Tipo.TECNICA_CLIENTE, 422);
        var dependencia = falha(
                FalhaAlteracaoProdutosContratadosDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                503);
        var timeout = falha(
                FalhaAlteracaoProdutosContratadosDossieProduto.Tipo.TIMEOUT, null);

        MtrBusinessErrorException erroNegocio = assertInstanceOf(
                MtrBusinessErrorException.class,
                ProdutoDossieProdutoRestMapper.paraExcecaoRest(negocio));
        assertEquals(409, erroNegocio.status());
        assertEquals("produto-erro", erroNegocio.erro().idErro());
        assertEquals("mensagem externa", erroNegocio.erro().erros().getFirst().mensagem());
        assertEquals("detalhe", erroNegocio.erro().detalhe());
        assertEquals("stacktrace", erroNegocio.erro().stacktrace());

        assertEquals(422, assertInstanceOf(
                MtrClientTechnicalException.class,
                ProdutoDossieProdutoRestMapper.paraExcecaoRest(tecnica)).status());
        assertEquals(503, assertInstanceOf(
                MtrServerErrorException.class,
                ProdutoDossieProdutoRestMapper.paraExcecaoRest(dependencia)).status());
        assertEquals(500, assertInstanceOf(
                MtrServerErrorException.class,
                ProdutoDossieProdutoRestMapper.paraExcecaoRest(timeout)).status());
    }

    private static FalhaAlteracaoProdutosContratadosDossieProduto falha(
            FalhaAlteracaoProdutosContratadosDossieProduto.Tipo tipo,
            Integer status
    ) {
        return new FalhaAlteracaoProdutosContratadosDossieProduto(
                tipo,
                status,
                "simtr-dossie-produto",
                "produto-erro",
                "MTR-PROD-001",
                List.of("mensagem externa"),
                "detalhe",
                "stacktrace",
                new IllegalStateException("causa"));
    }
}
