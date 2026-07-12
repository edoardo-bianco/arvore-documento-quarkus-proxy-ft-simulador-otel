package br.gov.caixa.simtr.hub.contrato;

import br.gov.caixa.simtr.hub.TestFixtures;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class DossieProdutoErroApiContractTest {

    private static final String MENSAGEM_ID =
            "O identificador do dossie produto deve ser maior que zero.";

    @Test
    void preservaErroParaCorpoDeCriacaoAusente() {
        validarErro(given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("/simtr-hub/v1/dossie-produto"),
                "O corpo da requisicao deve ser informado.");
    }

    @Test
    void preservaErroParaIdInvalidoNoFormulario() {
        validarErro(given().contentType(ContentType.JSON).accept(ContentType.JSON)
                .body(TestFixtures.formularioDto())
                .patch("/simtr-hub/v1/dossie-produto/{id}/formulario", 0), MENSAGEM_ID);
    }

    @Test
    void preservaErroParaIdInvalidoNaInclusaoDeDocumento() {
        validarErro(given().contentType(ContentType.JSON).accept(ContentType.JSON)
                .body(TestFixtures.documentoInclusaoDto())
                .post("/simtr-hub/v1/dossie-produto/{id}/documento", 0), MENSAGEM_ID);
    }

    @Test
    void preservaErroParaIdInvalidoNaValidacaoNegocial() {
        validarErro(given().contentType(ContentType.JSON).accept(ContentType.JSON)
                .body(TestFixtures.validacaoNegocialDto())
                .patch("/simtr-hub/v1/dossie-produto/{id}/validacao-negocial", 0), MENSAGEM_ID);
    }

    @Test
    void preservaErroParaIdInvalidoNoWorkflow() {
        validarErro(given().accept(ContentType.JSON)
                .post("/simtr-hub/v1/dossie-produto/{id}/workflow", 0), MENSAGEM_ID);
    }

    private static void validarErro(Response response, String mensagem) {
        JsonNode erro = response.then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(400, erro.path("codigo_http").asInt());
        assertEquals("simtr-hub", erro.path("recurso").asText());
        assertEquals("ARVDOCP0001", erro.path("codigo_erro").asText());
        assertEquals(mensagem, erro.path("erros").path(0).path("mensagem").asText());
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
