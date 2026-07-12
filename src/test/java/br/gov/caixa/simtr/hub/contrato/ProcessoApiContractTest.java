package br.gov.caixa.simtr.hub.contrato;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ProcessoApiContractTest {

    private static final String PATH =
            "/simtr-hub/v1/processo/identificador-negocial/{identificador}";
    private static final String MOCK =
            "mock/parametrizacao/1000016487-consulta-processo-parametrizacao-v2-identificador-negocial.md";

    @Inject
    MarkdownJsonMockReader mockReader;

    @Test
    void preservaRespostaJsonCompletaDaConsultaDeProcesso() {
        JsonNode esperado = mockReader.readFirstJsonObject(MOCK, JsonNode.class);

        JsonNode resposta = given()
                .accept(ContentType.JSON)
                .when()
                .get(PATH, 1000016487L)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(JsonNode.class);

        assertEquals(esperado, resposta);
    }

    @Test
    void preservaErroPublicoParaIdentificadorDeProcessoInvalido() {
        JsonNode erro = given()
                .accept(ContentType.JSON)
                .when()
                .get(PATH, 0)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract()
                .as(JsonNode.class);

        assertEquals(400, erro.path("codigo_http").asInt());
        assertEquals("simtr-hub", erro.path("recurso").asText());
        assertEquals("ARVDOCP0001", erro.path("codigo_erro").asText());
        assertEquals("O identificador negocial deve ser maior que zero.",
                erro.path("erros").path(0).path("mensagem").asText());
        assertTrue(uuidValido(erro.path("id_erro").asText()));
        assertFalse(erro.has("detalhe"));
        assertFalse(erro.has("stacktrace"));
    }

    private static boolean uuidValido(String valor) {
        try {
            UUID.fromString(valor);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
