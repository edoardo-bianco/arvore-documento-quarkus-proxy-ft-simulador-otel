package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.CriacaoDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.criacao.CriacaoDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.CriacaoDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper.CriacaoDossieProdutoMtrMapper;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteRelacionadoCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoCriacaoDossieProduto;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CriacaoDossieProdutoMtrAdapterTest {

    private static final String SERVICO_MTR = "simtr-dossie-produto";

    private final CriacaoDossieProdutoMtrClient client = mock(CriacaoDossieProdutoMtrClient.class);
    private final CriacaoDossieProdutoMtrAdapter adapter = new CriacaoDossieProdutoMtrAdapter(
            client, new CriacaoDossieProdutoMtrMapper());

    @Test
    void mapeiaComandoEConverteRespostaParaResultadoInterno() {
        when(client.criar(argThat(request ->
                request.processo().equals(100L)
                        && request.chaveCorrelacaoCanal().equals(200L)
                        && request.clientes().getFirst().clienteRelacionado().cpf()
                                .equals("98765432100"))))
                .thenReturn(Uni.createFrom().item(new CriacaoDossieProdutoMtrResponse(4321L)));

        var resultado = adapter.criar(comando()).await().indefinitely();

        assertEquals(4321L, resultado.identificadorDossieProduto());
    }

    @Test
    void traduzErroMtrParaFalhaInternaLossless() {
        var erro = new CriacaoDossieProdutoMtrException.Erro(
                400, SERVICO_MTR, "criacao-400", "MTR-CRIACAO-400",
                List.of(new CriacaoDossieProdutoMtrException.Mensagem("criacao nao permitida")),
                "negocio", "stack-remota");
        when(client.criar(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Uni.createFrom().failure(
                        new CriacaoDossieProdutoMtrException.Negocio(400, erro)));

        FalhaCriacaoDossieProduto falha = assertThrows(
                FalhaCriacaoDossieProduto.class,
                () -> adapter.criar(comando()).await().indefinitely());

        assertEquals(FalhaCriacaoDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(400, falha.status());
        assertEquals(SERVICO_MTR, falha.recurso());
        assertEquals("criacao-400", falha.idErro());
        assertEquals("MTR-CRIACAO-400", falha.codigoErro());
        assertEquals(List.of("criacao nao permitida"), falha.mensagens());
        assertEquals("negocio", falha.detalhe());
        assertEquals("stack-remota", falha.stacktraceExterno());
    }

    @Test
    void classificaTimeoutDepoisDaPoliticaDoClient() {
        when(client.criar(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Uni.createFrom().failure(new TimeoutException()));

        FalhaCriacaoDossieProduto falha = assertThrows(
                FalhaCriacaoDossieProduto.class,
                () -> adapter.criar(comando()).await().indefinitely());

        assertEquals(FalhaCriacaoDossieProduto.Tipo.TIMEOUT, falha.tipo());
        assertEquals(SERVICO_MTR, falha.recurso());
    }

    private static ComandoCriacaoDossieProduto comando() {
        return new ComandoCriacaoDossieProduto(
                100L,
                200L,
                300L,
                List.of(new ClienteCriacaoDossieProduto(
                        "12345678901",
                        "12345678000190",
                        1L,
                        new ClienteRelacionadoCriacaoDossieProduto("98765432100", null),
                        1)));
    }
}
