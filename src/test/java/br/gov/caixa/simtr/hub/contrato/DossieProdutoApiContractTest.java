package br.gov.caixa.simtr.hub.contrato;

import br.gov.caixa.simtr.hub.TestFixtures;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class DossieProdutoApiContractTest {

    @Inject
    ObjectMapper objectMapper;

    @Test
    void preservaContratoDeCriacao() throws JsonProcessingException {
        JsonNode resposta = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(TestFixtures.dossieCriacaoDto())
                .when()
                .post("/simtr-hub/v1/dossie-produto")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(json("{\"id\":1}"), resposta);
    }

    @Test
    void preservaContratoDeAtualizacaoDeFormulario() throws JsonProcessingException {
        JsonNode resposta = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(TestFixtures.formularioDto())
                .when()
                .patch("/simtr-hub/v1/dossie-produto/{id}/formulario", 123L)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(json("{\"id\":123}"), resposta);
    }

    @Test
    void preservaContratoDeInclusaoDeDocumento() throws JsonProcessingException {
        JsonNode resposta = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(TestFixtures.documentoInclusaoDto())
                .when()
                .post("/simtr-hub/v1/dossie-produto/{id}/documento", 123L)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(json("{\"id_documento\":456,\"id_instancia_documento\":789}"), resposta);
    }

    @Test
    void preservaContratoDeValidacaoNegocialSemCorpo() {
        String resposta = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(TestFixtures.validacaoNegocialDto())
                .when()
                .patch("/simtr-hub/v1/dossie-produto/{id}/validacao-negocial", 123L)
                .then()
                .statusCode(200)
                .extract().asString();

        assertEquals("", resposta);
    }

    @Test
    void preservaContratoDeAvancoDeWorkflow() throws JsonProcessingException {
        JsonNode resposta = given()
                .accept(ContentType.JSON)
                .when()
                .post("/simtr-hub/v1/dossie-produto/{id}/workflow", 123L)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(json("{\"id\":123}"), resposta);
    }

    private JsonNode json(String valor) throws JsonProcessingException {
        return objectMapper.readTree(valor);
    }
}
