package br.gov.caixa.simtr.hub.dossieproduto.dominio.erro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

class FalhasDossieProdutoTest {

    private static final Integer STATUS = 422;
    private static final String RECURSO = "/dossies/123";
    private static final String ID_ERRO = "erro-123";
    private static final String CODIGO_ERRO = "MTR-123";
    private static final String MENSAGEM = "mensagem devolvida pela dependencia";
    private static final String DETALHE = "detalhe externo";
    private static final String STACKTRACE_EXTERNO = "stacktrace externo";
    private static final String MENSAGEM_CAUSA = "mensagem da causa";

    @Test
    void caracterizaFalhaDeCriacao() {
        var causa = new IllegalStateException(MENSAGEM_CAUSA);
        var mensagens = List.of(MENSAGEM);
        var falha = falhaCriacao(mensagens, causa);

        assertEquals(FalhaCriacaoDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(STATUS, falha.status());
        assertEquals(RECURSO, falha.recurso());
        assertEquals(ID_ERRO, falha.idErro());
        assertEquals(CODIGO_ERRO, falha.codigoErro());
        assertSame(mensagens, falha.mensagens());
        assertEquals(DETALHE, falha.detalhe());
        assertEquals(STACKTRACE_EXTERNO, falha.stacktraceExterno());
        assertSame(causa, falha.getCause());
        assertEquals(MENSAGEM, falha.getMessage());
        assertMensagensDefensivas(
                FalhasDossieProdutoTest::falhaCriacao,
                "Falha ao criar dossie produto");
    }

    @Test
    void caracterizaFalhaDeAtualizacaoDeFormulario() {
        var causa = new IllegalStateException(MENSAGEM_CAUSA);
        var mensagens = List.of(MENSAGEM);
        var falha = falhaAtualizacaoFormulario(mensagens, causa);

        assertEquals(FalhaAtualizacaoFormularioDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(STATUS, falha.status());
        assertEquals(RECURSO, falha.recurso());
        assertEquals(ID_ERRO, falha.idErro());
        assertEquals(CODIGO_ERRO, falha.codigoErro());
        assertSame(mensagens, falha.mensagens());
        assertEquals(DETALHE, falha.detalhe());
        assertEquals(STACKTRACE_EXTERNO, falha.stacktraceExterno());
        assertSame(causa, falha.getCause());
        assertEquals(MENSAGEM, falha.getMessage());
        assertMensagensDefensivas(
                FalhasDossieProdutoTest::falhaAtualizacaoFormulario,
                "Falha ao atualizar formulario do dossie produto");
    }

    @Test
    void caracterizaFalhaDeInclusaoDeDocumento() {
        var causa = new IllegalStateException(MENSAGEM_CAUSA);
        var mensagens = List.of(MENSAGEM);
        var falha = falhaInclusaoDocumento(mensagens, causa);

        assertEquals(FalhaInclusaoDocumentoDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(STATUS, falha.status());
        assertEquals(RECURSO, falha.recurso());
        assertEquals(ID_ERRO, falha.idErro());
        assertEquals(CODIGO_ERRO, falha.codigoErro());
        assertSame(mensagens, falha.mensagens());
        assertEquals(DETALHE, falha.detalhe());
        assertEquals(STACKTRACE_EXTERNO, falha.stacktraceExterno());
        assertSame(causa, falha.getCause());
        assertEquals(MENSAGEM, falha.getMessage());
        assertMensagensDefensivas(
                FalhasDossieProdutoTest::falhaInclusaoDocumento,
                "Falha ao incluir documento no dossie produto");
    }

    @Test
    void caracterizaFalhaDeRegistroDeValidacaoNegocial() {
        var causa = new IllegalStateException(MENSAGEM_CAUSA);
        var mensagens = List.of(MENSAGEM);
        var falha = falhaRegistroValidacaoNegocial(mensagens, causa);

        assertEquals(FalhaRegistroValidacaoNegocialDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(STATUS, falha.status());
        assertEquals(RECURSO, falha.recurso());
        assertEquals(ID_ERRO, falha.idErro());
        assertEquals(CODIGO_ERRO, falha.codigoErro());
        assertSame(mensagens, falha.mensagens());
        assertEquals(DETALHE, falha.detalhe());
        assertEquals(STACKTRACE_EXTERNO, falha.stacktraceExterno());
        assertSame(causa, falha.getCause());
        assertEquals(MENSAGEM, falha.getMessage());
        assertMensagensDefensivas(
                FalhasDossieProdutoTest::falhaRegistroValidacaoNegocial,
                "Falha ao registrar validacao negocial no dossie produto");
    }

    @Test
    void caracterizaFalhaDeWorkflow() {
        var causa = new IllegalStateException(MENSAGEM_CAUSA);
        var mensagens = List.of(MENSAGEM);
        var falha = falhaWorkflow(mensagens, causa);

        assertEquals(FalhaWorkflowDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(STATUS, falha.status());
        assertEquals(RECURSO, falha.recurso());
        assertEquals(ID_ERRO, falha.idErro());
        assertEquals(CODIGO_ERRO, falha.codigoErro());
        assertSame(mensagens, falha.mensagens());
        assertEquals(DETALHE, falha.detalhe());
        assertEquals(STACKTRACE_EXTERNO, falha.stacktraceExterno());
        assertSame(causa, falha.getCause());
        assertEquals(MENSAGEM, falha.getMessage());
        assertMensagensDefensivas(
                FalhasDossieProdutoTest::falhaWorkflow,
                "Falha ao avancar workflow do dossie produto");
    }

    private static void assertMensagensDefensivas(
            BiFunction<List<String>, Throwable, RuntimeException> fabrica,
            String fallback
    ) {
        var causa = new IllegalStateException(MENSAGEM_CAUSA);

        assertEquals(MENSAGEM_CAUSA, fabrica.apply(null, causa).getMessage());
        assertEquals(MENSAGEM_CAUSA, fabrica.apply(List.of(), causa).getMessage());
        assertEquals(
                MENSAGEM_CAUSA,
                fabrica.apply(Collections.singletonList(null), causa).getMessage());
        assertEquals(fallback, fabrica.apply(null, null).getMessage());
    }

    private static FalhaCriacaoDossieProduto falhaCriacao(
            List<String> mensagens,
            Throwable causa
    ) {
        return new FalhaCriacaoDossieProduto(
                FalhaCriacaoDossieProduto.Tipo.NEGOCIO,
                STATUS,
                RECURSO,
                ID_ERRO,
                CODIGO_ERRO,
                mensagens,
                DETALHE,
                STACKTRACE_EXTERNO,
                causa);
    }

    private static FalhaAtualizacaoFormularioDossieProduto falhaAtualizacaoFormulario(
            List<String> mensagens,
            Throwable causa
    ) {
        return new FalhaAtualizacaoFormularioDossieProduto(
                FalhaAtualizacaoFormularioDossieProduto.Tipo.NEGOCIO,
                STATUS,
                RECURSO,
                ID_ERRO,
                CODIGO_ERRO,
                mensagens,
                DETALHE,
                STACKTRACE_EXTERNO,
                causa);
    }

    private static FalhaInclusaoDocumentoDossieProduto falhaInclusaoDocumento(
            List<String> mensagens,
            Throwable causa
    ) {
        return new FalhaInclusaoDocumentoDossieProduto(
                FalhaInclusaoDocumentoDossieProduto.Tipo.NEGOCIO,
                STATUS,
                RECURSO,
                ID_ERRO,
                CODIGO_ERRO,
                mensagens,
                DETALHE,
                STACKTRACE_EXTERNO,
                causa);
    }

    private static FalhaRegistroValidacaoNegocialDossieProduto falhaRegistroValidacaoNegocial(
            List<String> mensagens,
            Throwable causa
    ) {
        return new FalhaRegistroValidacaoNegocialDossieProduto(
                FalhaRegistroValidacaoNegocialDossieProduto.Tipo.NEGOCIO,
                STATUS,
                RECURSO,
                ID_ERRO,
                CODIGO_ERRO,
                mensagens,
                DETALHE,
                STACKTRACE_EXTERNO,
                causa);
    }

    private static FalhaWorkflowDossieProduto falhaWorkflow(
            List<String> mensagens,
            Throwable causa
    ) {
        return new FalhaWorkflowDossieProduto(
                FalhaWorkflowDossieProduto.Tipo.NEGOCIO,
                STATUS,
                RECURSO,
                ID_ERRO,
                CODIGO_ERRO,
                mensagens,
                DETALHE,
                STACKTRACE_EXTERNO,
                causa);
    }
}
