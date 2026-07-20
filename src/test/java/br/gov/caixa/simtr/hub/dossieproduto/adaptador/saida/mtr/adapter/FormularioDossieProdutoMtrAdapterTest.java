package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.FormularioDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.formulario.FormularioDossieProdutoMtrRequest;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.formulario.FormularioDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.FormularioDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper.FormularioDossieProdutoMtrMapper;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteAvalistaFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.FormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.GarantiaFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ProdutoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.RespostaFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.VinculoFormularioDossieProduto;
import io.smallrye.mutiny.Uni;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class FormularioDossieProdutoMtrAdapterTest {

    private static final String SERVICO_MTR = "simtr-dossie-produto";

    private final FormularioDossieProdutoMtrClient client =
            mock(FormularioDossieProdutoMtrClient.class);
    private final FormularioDossieProdutoMtrAdapter adapter =
            new FormularioDossieProdutoMtrAdapter(
                    client, new FormularioDossieProdutoMtrMapper());

    @Test
    void mapeiaComandoCompletoPreservandoNulosEConverteResposta() {
        when(client.atualizar(eq(123L), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Uni.createFrom().item(
                        new FormularioDossieProdutoMtrResponse(4321L)));

        var resultado = adapter.atualizar(comando()).await().indefinitely();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FormularioDossieProdutoMtrRequest>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(client).atualizar(eq(123L), captor.capture());
        List<FormularioDossieProdutoMtrRequest> request = captor.getValue();
        assertEquals("12345678901", request.getFirst().vinculoDossie().cliente().cpf());
        assertEquals(300, request.getFirst().vinculoDossie().garantia().codigoBacen());
        assertNull(request.getFirst().vinculoDossie().garantia().clientesAvalistas().get(1));
        assertNull(request.getFirst().vinculoDossie().respostasFormulario().get(1));
        assertNull(request.get(1));
        assertEquals(4321L, resultado.identificadorDossieProduto());
    }

    @Test
    void traduzErroMtrParaFalhaInternaLossless() {
        var erro = new FormularioDossieProdutoMtrException.Erro(
                400, SERVICO_MTR, "formulario-400", "MTR-FORM-400",
                List.of(new FormularioDossieProdutoMtrException.Mensagem(
                        "formulario nao permitido")),
                "negocio", "stack-remota");
        when(client.atualizar(eq(123L), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Uni.createFrom().failure(
                        new FormularioDossieProdutoMtrException.Negocio(400, erro)));

        FalhaAtualizacaoFormularioDossieProduto falha = assertThrows(
                FalhaAtualizacaoFormularioDossieProduto.class,
                () -> adapter.atualizar(comando()).await().indefinitely());

        assertEquals(FalhaAtualizacaoFormularioDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(400, falha.status());
        assertEquals(SERVICO_MTR, falha.recurso());
        assertEquals("formulario-400", falha.idErro());
        assertEquals("MTR-FORM-400", falha.codigoErro());
        assertEquals(List.of("formulario nao permitido"), falha.mensagens());
        assertEquals("negocio", falha.detalhe());
        assertEquals("stack-remota", falha.stacktraceExterno());
    }

    @Test
    void classificaTimeoutDepoisDaPoliticaDoClient() {
        when(client.atualizar(eq(123L), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Uni.createFrom().failure(new TimeoutException()));

        FalhaAtualizacaoFormularioDossieProduto falha = assertThrows(
                FalhaAtualizacaoFormularioDossieProduto.class,
                () -> adapter.atualizar(comando()).await().indefinitely());

        assertEquals(FalhaAtualizacaoFormularioDossieProduto.Tipo.TIMEOUT, falha.tipo());
        assertEquals(SERVICO_MTR, falha.recurso());
    }

    @Test
    void preservaAusenciaDeComandoEResposta() {
        when(client.atualizar(isNull(), isNull()))
                .thenReturn(Uni.createFrom().nullItem());

        var resultado = adapter.atualizar(null).await().indefinitely();

        assertNull(resultado);
        verify(client).atualizar(isNull(), isNull());
    }

    @Test
    void preservaListaDeFormulariosAusenteEConverteRespostaParcial() {
        var comando = new ComandoAtualizacaoFormularioDossieProduto(null, null);
        when(client.atualizar(isNull(), isNull()))
                .thenReturn(Uni.createFrom().item(
                        new FormularioDossieProdutoMtrResponse(null)));

        var resultado = adapter.atualizar(comando).await().indefinitely();

        assertNull(resultado.identificadorDossieProduto());
        verify(client).atualizar(isNull(), isNull());
    }

    @Test
    void contabilizaVinculosSemRespostaEPreservaCamposOpcionaisNulos() {
        var formularios = List.of(
                new FormularioDossieProduto(null),
                new FormularioDossieProduto(new VinculoFormularioDossieProduto(
                        null, null, null, null, null)));
        var comando = new ComandoAtualizacaoFormularioDossieProduto(123L, formularios);
        when(client.atualizar(eq(123L), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Uni.createFrom().item(
                        new FormularioDossieProdutoMtrResponse(null)));

        var resultado = adapter.atualizar(comando).await().indefinitely();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FormularioDossieProdutoMtrRequest>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(client).atualizar(eq(123L), captor.capture());
        var request = captor.getValue();
        assertNull(request.getFirst().vinculoDossie());
        assertNull(request.get(1).vinculoDossie().fase());
        assertNull(request.get(1).vinculoDossie().cliente());
        assertNull(request.get(1).vinculoDossie().produto());
        assertNull(request.get(1).vinculoDossie().garantia());
        assertNull(request.get(1).vinculoDossie().respostasFormulario());
        assertNull(resultado.identificadorDossieProduto());
    }

    @Test
    void classificaErroTecnicoSemCorpo() {
        var erroMtr = new FormularioDossieProdutoMtrException.TecnicaCliente(422, null);
        when(client.atualizar(eq(123L), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Uni.createFrom().failure(erroMtr));

        var espera = adapter.atualizar(comando()).await();
        FalhaAtualizacaoFormularioDossieProduto falha = assertThrows(
                FalhaAtualizacaoFormularioDossieProduto.class,
                espera::indefinitely);

        assertEquals(FalhaAtualizacaoFormularioDossieProduto.Tipo.TECNICA_CLIENTE, falha.tipo());
        assertEquals(422, falha.status());
        assertNull(falha.recurso());
        assertNull(falha.idErro());
        assertNull(falha.codigoErro());
        assertNull(falha.mensagens());
        assertNull(falha.detalhe());
        assertNull(falha.stacktraceExterno());
        assertSame(erroMtr, falha.getCause());
    }

    @Test
    void classificaErroDeServidorComListaDeMensagensAusente() {
        var erro = new FormularioDossieProdutoMtrException.Erro(
                503, SERVICO_MTR, "formulario-503", "MTR-FORM-503",
                null, "indisponivel", null);
        var erroMtr = new FormularioDossieProdutoMtrException.Servidor(503, erro);
        when(client.atualizar(eq(123L), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Uni.createFrom().failure(erroMtr));

        var espera = adapter.atualizar(comando()).await();
        FalhaAtualizacaoFormularioDossieProduto falha = assertThrows(
                FalhaAtualizacaoFormularioDossieProduto.class,
                espera::indefinitely);

        assertEquals(
                FalhaAtualizacaoFormularioDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                falha.tipo());
        assertEquals(503, falha.status());
        assertEquals(SERVICO_MTR, falha.recurso());
        assertNull(falha.mensagens());
        assertSame(erroMtr, falha.getCause());
    }

    @Test
    void classificaSubtipoMtrDesconhecidoEPreservaMensagemNula() {
        var erro = new FormularioDossieProdutoMtrException.Erro(
                500, SERVICO_MTR, "formulario-500", "MTR-FORM-500",
                java.util.Collections.singletonList(null), null, null);
        var erroMtr = mock(FormularioDossieProdutoMtrException.class);
        when(erroMtr.status()).thenReturn(500);
        when(erroMtr.erro()).thenReturn(erro);
        when(client.atualizar(eq(123L), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Uni.createFrom().failure(erroMtr));

        var espera = adapter.atualizar(comando()).await();
        FalhaAtualizacaoFormularioDossieProduto falha = assertThrows(
                FalhaAtualizacaoFormularioDossieProduto.class,
                espera::indefinitely);

        assertEquals(
                FalhaAtualizacaoFormularioDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                falha.tipo());
        assertEquals(java.util.Collections.singletonList(null), falha.mensagens());
        assertSame(erroMtr, falha.getCause());
    }

    @Test
    void classificaFalhaInesperadaComoDependenciaIndisponivel() {
        var erro = new IllegalStateException("falha inesperada");
        when(client.atualizar(eq(123L), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Uni.createFrom().failure(erro));

        var espera = adapter.atualizar(comando()).await();
        FalhaAtualizacaoFormularioDossieProduto falha = assertThrows(
                FalhaAtualizacaoFormularioDossieProduto.class,
                espera::indefinitely);

        assertEquals(
                FalhaAtualizacaoFormularioDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                falha.tipo());
        assertEquals(SERVICO_MTR, falha.recurso());
        assertSame(erro, falha.getCause());
    }

    private static ComandoAtualizacaoFormularioDossieProduto comando() {
        List<ClienteAvalistaFormularioDossieProduto> avalistas = new ArrayList<>();
        avalistas.add(new ClienteAvalistaFormularioDossieProduto(
                "98765432100", "12345678000190"));
        avalistas.add(null);

        List<RespostaFormularioDossieProduto> respostas = new ArrayList<>();
        respostas.add(new RespostaFormularioDossieProduto(
                600L, "resposta", List.of("opcao-1"), true));
        respostas.add(null);

        List<FormularioDossieProduto> formularios = new ArrayList<>();
        formularios.add(new FormularioDossieProduto(
                new VinculoFormularioDossieProduto(
                        10L,
                        new ClienteFormularioDossieProduto("12345678901", null, 1L),
                        new ProdutoFormularioDossieProduto(100, 200),
                        new GarantiaFormularioDossieProduto(300, 400, 500, avalistas),
                        respostas)));
        formularios.add(null);
        return new ComandoAtualizacaoFormularioDossieProduto(123L, formularios);
    }
}
