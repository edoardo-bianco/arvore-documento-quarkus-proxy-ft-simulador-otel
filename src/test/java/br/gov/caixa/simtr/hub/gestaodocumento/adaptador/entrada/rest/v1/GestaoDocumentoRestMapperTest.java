package br.gov.caixa.simtr.hub.gestaodocumento.adaptador.entrada.rest.v1;

import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.erro.FalhaObtencaoCredencialContainer;
import br.gov.caixa.simtr.hub.gestaodocumento.dominio.modelo.CredencialContainer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestaoDocumentoRestMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void mapeiaRespostaPreservandoJsonEValidadeOpaca() {
        Map<String, Object> validade = Map.of(
                "instante", "2099-12-31T23:59:47Z",
                "origem", "mtr"
        );
        CredencialContainer credencial = new CredencialContainer(
                "sv=caracterizacao&sig=valor-opaco",
                validade,
                "https://storage.invalid",
                "container-documentos"
        );

        var resposta = GestaoDocumentoRestMapper.paraResposta(credencial);

        assertEquals(credencial.sas(), resposta.sas());
        assertSame(validade, resposta.validade());
        assertEquals(credencial.urlStorage(), resposta.urlStorage());
        assertEquals(credencial.nomeContainer(), resposta.nomeContainer());

        JsonNode json = OBJECT_MAPPER.valueToTree(resposta);
        assertEquals("https://storage.invalid", json.path("url_storage").asText());
        assertEquals("container-documentos", json.path("nome_container").asText());
        assertEquals("2099-12-31T23:59:47Z", json.path("validade").path("instante").asText());
    }

    @Test
    void preservaRespostaNulaECamposNulosPresentesNoJson() {
        assertNull(GestaoDocumentoRestMapper.paraResposta(null));

        JsonNode json = OBJECT_MAPPER.valueToTree(GestaoDocumentoRestMapper.paraResposta(
                new CredencialContainer(null, null, null, null)
        ));

        assertEquals(4, json.size());
        assertTrue(json.path("sas").isNull());
        assertTrue(json.path("validade").isNull());
        assertTrue(json.path("url_storage").isNull());
        assertTrue(json.path("nome_container").isNull());
    }

    @Test
    void traduzFalhasInternasParaAsExcecoesPublicasSemPerderPayload() {
        MtrBusinessErrorException negocio = assertInstanceOf(
                MtrBusinessErrorException.class,
                GestaoDocumentoRestMapper.paraExcecaoRest(falha(
                        FalhaObtencaoCredencialContainer.Tipo.NEGOCIO,
                        404,
                        Arrays.asList(null, "container nao localizado")
                ))
        );
        assertEquals(404, negocio.status());
        assertEquals(404, negocio.erro().codigoHttp());
        assertEquals("simtr-gestao-documento", negocio.erro().recurso());
        assertEquals("credencial-404", negocio.erro().idErro());
        assertEquals("MTR-CREDENCIAL-404", negocio.erro().codigoErro());
        assertNull(negocio.erro().erros().getFirst());
        assertEquals("container nao localizado", negocio.erro().erros().get(1).mensagem());
        assertEquals("detalhe externo", negocio.erro().detalhe());
        assertEquals("stack externo", negocio.erro().stacktrace());

        assertEquals(422, assertInstanceOf(
                MtrClientTechnicalException.class,
                GestaoDocumentoRestMapper.paraExcecaoRest(falha(
                        FalhaObtencaoCredencialContainer.Tipo.TECNICA_CLIENTE,
                        422,
                        List.of("falha tecnica")
                ))
        ).status());
        assertEquals(503, assertInstanceOf(
                MtrServerErrorException.class,
                GestaoDocumentoRestMapper.paraExcecaoRest(falha(
                        FalhaObtencaoCredencialContainer.Tipo.DEPENDENCIA_INDISPONIVEL,
                        503,
                        List.of("dependencia indisponivel")
                ))
        ).status());

        MtrServerErrorException timeout = assertInstanceOf(
                MtrServerErrorException.class,
                GestaoDocumentoRestMapper.paraExcecaoRest(falha(
                        FalhaObtencaoCredencialContainer.Tipo.TIMEOUT,
                        null,
                        null
                ))
        );
        assertEquals(500, timeout.status());
        assertNull(timeout.erro().codigoHttp());
        assertNull(timeout.erro().erros());
    }

    private static FalhaObtencaoCredencialContainer falha(
            FalhaObtencaoCredencialContainer.Tipo tipo,
            Integer status,
            List<String> mensagens
    ) {
        return new FalhaObtencaoCredencialContainer(
                tipo,
                status,
                "simtr-gestao-documento",
                "credencial-404",
                "MTR-CREDENCIAL-404",
                mensagens,
                "detalhe externo",
                "stack externo",
                new IllegalStateException("causa")
        );
    }
}
