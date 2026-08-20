package br.gov.caixa.simtr.hub.dossieproduto.dominio.erro;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class FalhaConsultaDocumentosDossieProdutoTest {

    private static final String MENSAGEM_PADRAO =
            "Falha ao consultar documentos do dossie produto";

    @Test
    void preservaClassificacaoEDadosInternosDaFalha() {
        var causa = new IllegalStateException("falha sintetica");
        var mensagens = List.of("documentos indisponiveis");

        var falha = novaFalha(mensagens, causa);

        assertEquals(FalhaConsultaDocumentosDossieProduto.Tipo.NEGOCIO, falha.tipo());
        assertEquals(404, falha.status());
        assertEquals("/dossies/4081899/documentos", falha.recurso());
        assertEquals("erro-sintetico", falha.idErro());
        assertEquals("MTRPRD0002", falha.codigoErro());
        assertSame(mensagens, falha.mensagens());
        assertEquals("detalhe sintetico", falha.detalhe());
        assertEquals("stacktrace externo sintetico", falha.stacktraceExterno());
        assertSame(causa, falha.getCause());
        assertEquals(MENSAGEM_PADRAO, falha.getMessage());
    }

    @Test
    void classificaTodosOsTiposDeFalha() {
        assertArrayEquals(
                new FalhaConsultaDocumentosDossieProduto.Tipo[]{
                        FalhaConsultaDocumentosDossieProduto.Tipo.NEGOCIO,
                        FalhaConsultaDocumentosDossieProduto.Tipo.TECNICA_CLIENTE,
                        FalhaConsultaDocumentosDossieProduto.Tipo.DEPENDENCIA_INDISPONIVEL,
                        FalhaConsultaDocumentosDossieProduto.Tipo.TIMEOUT
                },
                FalhaConsultaDocumentosDossieProduto.Tipo.values());
    }

    @Test
    void usaMensagemObservavelSeguraIndependentementeDoPayloadOuDaCausa() {
        var causa = new IllegalStateException("falha sintetica");

        assertEquals(MENSAGEM_PADRAO, novaFalha(null, causa).getMessage());
        assertEquals(MENSAGEM_PADRAO, novaFalha(List.of(), causa).getMessage());
        assertEquals(MENSAGEM_PADRAO,
                novaFalha(Collections.singletonList(null), causa).getMessage());
        assertEquals(MENSAGEM_PADRAO, novaFalha(null, null).getMessage());
    }

    private static FalhaConsultaDocumentosDossieProduto novaFalha(
            List<String> mensagens,
            Throwable causa
    ) {
        return new FalhaConsultaDocumentosDossieProduto(
                FalhaConsultaDocumentosDossieProduto.Tipo.NEGOCIO,
                404,
                "/dossies/4081899/documentos",
                "erro-sintetico",
                "MTRPRD0002",
                mensagens,
                "detalhe sintetico",
                "stacktrace externo sintetico",
                causa);
    }
}
