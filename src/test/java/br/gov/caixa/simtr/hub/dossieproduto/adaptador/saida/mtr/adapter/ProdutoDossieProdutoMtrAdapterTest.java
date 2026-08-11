package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.ProdutoDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.produto.ProdutoDossieProdutoMtrRequest;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.ProdutoDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper.ProdutoDossieProdutoMtrMapper;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAlteracaoProdutosContratadosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ProdutoContratadoDossieProduto;
import io.smallrye.mutiny.Uni;

@QuarkusTest
class ProdutoDossieProdutoMtrAdapterTest {

    private static final String SERVICO_MTR = "simtr-dossie-produto";

    private final ProdutoDossieProdutoMtrClient client =
            mock(ProdutoDossieProdutoMtrClient.class);
    private final ProdutoDossieProdutoMtrAdapter adapter =
            new ProdutoDossieProdutoMtrAdapter(client, new ProdutoDossieProdutoMtrMapper());

    @Test
    void mapeiaComandoChamaClientERetornaVoid() {
        when(client.alterar(eq(123L), anyList())).thenReturn(Uni.createFrom().voidItem());
        var comando = comando();

        var resultado = adapter.alterar(comando).await().indefinitely();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProdutoDossieProdutoMtrRequest>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(client).alterar(eq(123L), captor.capture());
        var requisicao = captor.getValue();
        assertEquals(2, requisicao.size());
        assertEquals(101, requisicao.getFirst().codigoOperacao());
        assertEquals(201, requisicao.getFirst().codigoModalidade());
        assertEquals(true, requisicao.getFirst().excluir());
        assertEquals(102, requisicao.get(1).codigoOperacao());
        assertNull(requisicao.get(1).excluir());
        assertNull(resultado);
    }

    @Test
    void traduzErroDeNegocioLosslessDepoisDoClient() {
        var mensagensMtr = new ArrayList<ProdutoDossieProdutoMtrException.Mensagem>();
        mensagensMtr.add(new ProdutoDossieProdutoMtrException.Mensagem(
                "alteracao de produto nao permitida"));
        mensagensMtr.add(null);
        var erro = new ProdutoDossieProdutoMtrException.Erro(
                409, SERVICO_MTR, "produto-409", "MTR-PRODUTO-409",
                mensagensMtr, "negocio", "stack-remota");
        var erroMtr = new ProdutoDossieProdutoMtrException.Negocio(409, erro);
        when(client.alterar(eq(123L), anyList()))
                .thenReturn(Uni.createFrom().failure(erroMtr));

        var espera = adapter.alterar(comando()).await();
        var falha = assertThrows(
                FalhaAlteracaoProdutosContratadosDossieProduto.class,
                espera::indefinitely);

        assertEquals(
                FalhaAlteracaoProdutosContratadosDossieProduto.Tipo.NEGOCIO,
                falha.tipo());
        assertEquals(409, falha.status());
        assertEquals(SERVICO_MTR, falha.recurso());
        assertEquals("produto-409", falha.idErro());
        assertEquals("MTR-PRODUTO-409", falha.codigoErro());
        assertEquals(
                java.util.Arrays.asList("alteracao de produto nao permitida", null),
                falha.mensagens());
        assertEquals("negocio", falha.detalhe());
        assertEquals("stack-remota", falha.stacktraceExterno());
        assertSame(erroMtr, falha.getCause());
    }

    @Test
    void traduzErroTecnicoSemCorpo() {
        var erroMtr = new ProdutoDossieProdutoMtrException.TecnicaCliente(403, null);
        when(client.alterar(eq(123L), anyList()))
                .thenReturn(Uni.createFrom().failure(erroMtr));

        var espera = adapter.alterar(comando()).await();
        var falha = assertThrows(
                FalhaAlteracaoProdutosContratadosDossieProduto.class,
                espera::indefinitely);

        assertEquals(
                FalhaAlteracaoProdutosContratadosDossieProduto.Tipo.TECNICA_CLIENTE,
                falha.tipo());
        assertEquals(403, falha.status());
        assertNull(falha.recurso());
        assertNull(falha.mensagens());
        assertSame(erroMtr, falha.getCause());
    }

    @Test
    void traduzErroDeServidorComoDependenciaIndisponivel() {
        var erro = new ProdutoDossieProdutoMtrException.Erro(
                503, SERVICO_MTR, "produto-503", "MTR-PRODUTO-503",
                null, "indisponivel", null);
        var erroMtr = new ProdutoDossieProdutoMtrException.Servidor(503, erro);
        when(client.alterar(eq(123L), anyList()))
                .thenReturn(Uni.createFrom().failure(erroMtr));

        var espera = adapter.alterar(comando()).await();
        var falha = assertThrows(
                FalhaAlteracaoProdutosContratadosDossieProduto.class,
                espera::indefinitely);

        assertEquals(
                FalhaAlteracaoProdutosContratadosDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                falha.tipo());
        assertEquals(503, falha.status());
        assertEquals("indisponivel", falha.detalhe());
        assertSame(erroMtr, falha.getCause());
    }

    @Test
    void classificaTimeoutDepoisDaPoliticaDoClient() {
        var timeout = new TimeoutException();
        when(client.alterar(eq(123L), anyList()))
                .thenReturn(Uni.createFrom().failure(timeout));

        var espera = adapter.alterar(comando()).await();
        var falha = assertThrows(
                FalhaAlteracaoProdutosContratadosDossieProduto.class,
                espera::indefinitely);

        assertEquals(
                FalhaAlteracaoProdutosContratadosDossieProduto.Tipo.TIMEOUT,
                falha.tipo());
        assertEquals(SERVICO_MTR, falha.recurso());
        assertSame(timeout, falha.getCause());
    }

    @Test
    void classificaFalhaInesperadaComoDependenciaIndisponivel() {
        var erro = new IllegalStateException("falha inesperada");
        when(client.alterar(eq(123L), anyList()))
                .thenReturn(Uni.createFrom().failure(erro));

        var espera = adapter.alterar(comando()).await();
        var falha = assertThrows(
                FalhaAlteracaoProdutosContratadosDossieProduto.class,
                espera::indefinitely);

        assertEquals(
                FalhaAlteracaoProdutosContratadosDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                falha.tipo());
        assertEquals(SERVICO_MTR, falha.recurso());
        assertSame(erro, falha.getCause());
    }

    private static ComandoAlteracaoProdutosContratadosDossieProduto comando() {
        return new ComandoAlteracaoProdutosContratadosDossieProduto(
                123L,
                List.of(
                        new ProdutoContratadoDossieProduto(101, 201, true),
                        new ProdutoContratadoDossieProduto(102, 202, null)));
    }
}
