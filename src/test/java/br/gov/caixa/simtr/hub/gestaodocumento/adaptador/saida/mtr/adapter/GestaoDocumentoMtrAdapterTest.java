package br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.mtr.adapter;

import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.mtr.client.GestaoDocumentoClient;
import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.mtr.dto.v1.credencial.CredencialContainerMtrResponse;
import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.mtr.erro.GestaoDocumentoMtrException;
import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.mtr.mapper.CredencialContainerMtrMapper;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.erro.FalhaObtencaoCredencialContainer;
import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GestaoDocumentoMtrAdapterTest {

    @Test
    void obtemCredencialEMapeiaRespostaParaODominioSemInterpretarValidade() {
        var validade = Map.<String, Object>of(
                "expira_em", "31/12/2099 23:59:47",
                "origem", "contrato-mtr"
        );
        var resposta = new CredencialContainerMtrResponse(
                "sv=teste&sp=rw&sig=valor-opaco",
                validade,
                "https://storage.example.test",
                "container-teste"
        );
        var client = mock(GestaoDocumentoClient.class);
        when(client.gerarCredencialContainer()).thenReturn(Uni.createFrom().item(resposta));
        var adapter = new GestaoDocumentoMtrAdapter(client, new CredencialContainerMtrMapper());

        var credencial = adapter.obter().await().indefinitely();

        assertEquals("sv=teste&sp=rw&sig=valor-opaco", credencial.sas());
        assertSame(validade, credencial.validade());
        assertEquals("https://storage.example.test", credencial.urlStorage());
        assertEquals("container-teste", credencial.nomeContainer());
    }

    @Test
    void traduzErroDeNegocioSemPerderCamposOuElementosNulos() {
        var erro = new GestaoDocumentoMtrException.Erro(
                404,
                "simtr-gestao-documento",
                "credencial-404",
                "MTR-CREDENCIAL-404",
                Arrays.asList(
                        new GestaoDocumentoMtrException.Mensagem("container nao localizado"),
                        null
                ),
                "falha de negocio",
                "stacktrace externo"
        );
        var origem = new GestaoDocumentoMtrException.Negocio(404, erro);

        var falha = assertThrows(
                FalhaObtencaoCredencialContainer.class,
                () -> adapterComFalha(origem).obter().await().indefinitely()
        );

        assertEquals(FalhaObtencaoCredencialContainer.Tipo.NEGOCIO, falha.tipo());
        assertEquals(404, falha.status());
        assertEquals("simtr-gestao-documento", falha.recurso());
        assertEquals("credencial-404", falha.idErro());
        assertEquals("MTR-CREDENCIAL-404", falha.codigoErro());
        assertEquals(Arrays.asList("container nao localizado", null), falha.mensagens());
        assertEquals("falha de negocio", falha.detalhe());
        assertEquals("stacktrace externo", falha.stacktraceExterno());
        assertSame(origem, falha.getCause());
    }

    @Test
    void traduzFalhasTecnicaClienteEServidorParaClassificacoesInternas() {
        var erro = new GestaoDocumentoMtrException.Erro(
                429, null, null, null, null, null, null
        );

        var tecnica = assertThrows(
                FalhaObtencaoCredencialContainer.class,
                () -> adapterComFalha(
                        new GestaoDocumentoMtrException.TecnicaCliente(429, erro)
                ).obter().await().indefinitely()
        );
        var servidor = assertThrows(
                FalhaObtencaoCredencialContainer.class,
                () -> adapterComFalha(
                        new GestaoDocumentoMtrException.Servidor(503, erro)
                ).obter().await().indefinitely()
        );

        assertEquals(FalhaObtencaoCredencialContainer.Tipo.TECNICA_CLIENTE, tecnica.tipo());
        assertEquals(
                FalhaObtencaoCredencialContainer.Tipo.DEPENDENCIA_INDISPONIVEL,
                servidor.tipo()
        );
    }

    @Test
    void traduzTimeoutSomenteDepoisDaFalhaDoClient() {
        var origem = new TimeoutException("tempo esgotado");

        var falha = assertThrows(
                FalhaObtencaoCredencialContainer.class,
                () -> adapterComFalha(origem).obter().await().indefinitely()
        );

        assertEquals(FalhaObtencaoCredencialContainer.Tipo.TIMEOUT, falha.tipo());
        assertSame(origem, falha.getCause());
    }

    private static GestaoDocumentoMtrAdapter adapterComFalha(Throwable falha) {
        var client = mock(GestaoDocumentoClient.class);
        when(client.gerarCredencialContainer()).thenReturn(Uni.createFrom().failure(falha));
        return new GestaoDocumentoMtrAdapter(client, new CredencialContainerMtrMapper());
    }
}
