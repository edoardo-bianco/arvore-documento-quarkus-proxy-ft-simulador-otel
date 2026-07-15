package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.adapter;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.ValidacaoNegocialDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.validacaonegocial.ValidacaoNegocialDossieProdutoMtrRequest;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.ValidacaoNegocialDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.mapper.ValidacaoNegocialDossieProdutoMtrMapper;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.erro.FalhaRegistroValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteAvalistaValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoRegistroValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.GarantiaValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ParecerApontamentoValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ProdutoValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.RespostaFormularioValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.VerificacaoValidacaoNegocialDossieProduto;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ValidacaoNegocialDossieProdutoMtrAdapterTest {

    private final ValidacaoNegocialDossieProdutoMtrClient client =
            mock(ValidacaoNegocialDossieProdutoMtrClient.class);
    private final ValidacaoNegocialDossieProdutoMtrAdapter adapter =
            new ValidacaoNegocialDossieProdutoMtrAdapter(
                    client, new ValidacaoNegocialDossieProdutoMtrMapper());

    @Test
    void mapeiaComandoCompletoPreservandoListasElementosECamposNulos() {
        when(client.registrar(eq(123L), any()))
                .thenReturn(Uni.createFrom().voidItem());

        Void resultado = adapter.registrar(comando()).await().indefinitely();

        ArgumentCaptor<ValidacaoNegocialDossieProdutoMtrRequest> captor =
                ArgumentCaptor.forClass(ValidacaoNegocialDossieProdutoMtrRequest.class);
        verify(client).registrar(eq(123L), captor.capture());
        ValidacaoNegocialDossieProdutoMtrRequest request = captor.getValue();
        assertEquals(1122928L,
                request.verificacoes().getFirst().identificadorInstanciaDocumento());
        assertEquals("APROVADO", request.verificacoes().getFirst()
                .parecerApontamentos().getFirst().resultado());
        assertNull(request.verificacoes().getFirst().parecerApontamentos().get(1));
        assertEquals(300, request.verificacoes().getFirst().garantia().codigoBacen());
        assertNull(request.verificacoes().getFirst().garantia().clientesAvalistas().get(1));
        assertNull(request.verificacoes().get(1));
        assertEquals(List.of("2"),
                request.respostasFormulario().getFirst().opcoesSelecionadas());
        assertNull(request.respostasFormulario().get(1));
        assertNull(resultado);
    }

    @Test
    void traduzErroMtrParaFalhaInternaLossless() {
        var erro = new ValidacaoNegocialDossieProdutoMtrException.Erro(
                400, "simtr-dossie-produto", "validacao-400", "MTR-VALIDACAO-400",
                List.of(new ValidacaoNegocialDossieProdutoMtrException.Mensagem(
                        "validacao negocial nao permitida")),
                "negocio", "stack-remota");
        when(client.registrar(eq(123L), any()))
                .thenReturn(Uni.createFrom().failure(
                        new ValidacaoNegocialDossieProdutoMtrException.Negocio(400, erro)));

        FalhaRegistroValidacaoNegocialDossieProduto falha = assertThrows(
                FalhaRegistroValidacaoNegocialDossieProduto.class,
                () -> adapter.registrar(comando()).await().indefinitely());

        assertEquals(FalhaRegistroValidacaoNegocialDossieProduto.Tipo.NEGOCIO,
                falha.tipo());
        assertEquals(400, falha.status());
        assertEquals("simtr-dossie-produto", falha.recurso());
        assertEquals("validacao-400", falha.idErro());
        assertEquals("MTR-VALIDACAO-400", falha.codigoErro());
        assertEquals(List.of("validacao negocial nao permitida"), falha.mensagens());
        assertEquals("negocio", falha.detalhe());
        assertEquals("stack-remota", falha.stacktraceExterno());
    }

    @Test
    void classificaTimeoutDepoisDaPoliticaDoClient() {
        when(client.registrar(eq(123L), any()))
                .thenReturn(Uni.createFrom().failure(new TimeoutException()));

        FalhaRegistroValidacaoNegocialDossieProduto falha = assertThrows(
                FalhaRegistroValidacaoNegocialDossieProduto.class,
                () -> adapter.registrar(comando()).await().indefinitely());

        assertEquals(FalhaRegistroValidacaoNegocialDossieProduto.Tipo.TIMEOUT,
                falha.tipo());
        assertEquals("simtr-dossie-produto", falha.recurso());
    }

    private static ComandoRegistroValidacaoNegocialDossieProduto comando() {
        List<ClienteAvalistaValidacaoNegocialDossieProduto> avalistas = new ArrayList<>();
        avalistas.add(new ClienteAvalistaValidacaoNegocialDossieProduto(
                "12345678901", null));
        avalistas.add(null);

        List<ParecerApontamentoValidacaoNegocialDossieProduto> pareceres =
                new ArrayList<>();
        pareceres.add(new ParecerApontamentoValidacaoNegocialDossieProduto(
                1000012877L, "APROVADO", "apontamento aprovado", false, 1.0));
        pareceres.add(null);

        List<VerificacaoValidacaoNegocialDossieProduto> verificacoes = new ArrayList<>();
        verificacoes.add(new VerificacaoValidacaoNegocialDossieProduto(
                1122928L,
                6592L,
                2,
                true,
                pareceres,
                new GarantiaValidacaoNegocialDossieProduto(300, avalistas),
                new ProdutoValidacaoNegocialDossieProduto(100, 200),
                null));
        verificacoes.add(null);

        List<RespostaFormularioValidacaoNegocialDossieProduto> respostas =
                new ArrayList<>();
        respostas.add(new RespostaFormularioValidacaoNegocialDossieProduto(
                1000011699L, null, List.of("2")));
        respostas.add(null);

        return new ComandoRegistroValidacaoNegocialDossieProduto(
                123L, verificacoes, respostas);
    }
}
