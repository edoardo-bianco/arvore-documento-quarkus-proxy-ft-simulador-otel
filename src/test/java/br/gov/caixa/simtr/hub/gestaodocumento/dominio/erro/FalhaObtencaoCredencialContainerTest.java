package br.gov.caixa.simtr.hub.gestaodocumento.dominio.erro;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class FalhaObtencaoCredencialContainerTest {

    @Test
    void preservaClassificacaoEPayloadDeErroSemPerdas() {
        var causa = new IllegalStateException("origem externa");
        var mensagens = Arrays.asList("container nao localizado", null);

        var falha = new FalhaObtencaoCredencialContainer(
                FalhaObtencaoCredencialContainer.Tipo.NEGOCIO,
                404,
                "simtr-gestao-documento",
                "credencial-404",
                "MTR-CREDENCIAL-404",
                mensagens,
                "falha de negocio",
                "stacktrace externo",
                causa
        );

        assertEquals(FalhaObtencaoCredencialContainer.Tipo.NEGOCIO, falha.tipo());
        assertEquals(404, falha.status());
        assertEquals("simtr-gestao-documento", falha.recurso());
        assertEquals("credencial-404", falha.idErro());
        assertEquals("MTR-CREDENCIAL-404", falha.codigoErro());
        assertSame(mensagens, falha.mensagens());
        assertEquals("falha de negocio", falha.detalhe());
        assertEquals("stacktrace externo", falha.stacktraceExterno());
        assertEquals("container nao localizado", falha.getMessage());
        assertSame(causa, falha.getCause());
    }

    @Test
    void usaMensagemDaCausaQuandoPayloadNaoPossuiMensagem() {
        var causa = new IllegalStateException("dependencia indisponivel");

        var falha = new FalhaObtencaoCredencialContainer(
                FalhaObtencaoCredencialContainer.Tipo.DEPENDENCIA_INDISPONIVEL,
                null, null, null, null, null, null, null, causa
        );

        assertEquals("dependencia indisponivel", falha.getMessage());
    }
}
