package br.gov.caixa.simtr.hub.contrato;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class GestaoDocumentoApiContractTest {

    private static final String MOCK = "mock/gestaodocumento/credencial-container.md";

    @Inject
    MarkdownJsonMockReader mockReader;

    @Test
    void preservaRespostaJsonCompletaDaCredencialDeContainer() {
        JsonNode esperado = mockReader.readFirstJsonObject(MOCK, JsonNode.class);

        JsonNode resposta = given()
                .accept(ContentType.JSON)
                .when()
                .post("/simtr-hub/v1/storage/container/credencial")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(esperado, resposta);
    }
}
