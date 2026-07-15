package br.gov.caixa.simtr.hub.contrato;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static br.gov.caixa.simtr.hub.contrato.JsonContractAssertions.assertJsonExato;
import static io.restassured.RestAssured.given;

@QuarkusTest
class GestaoDocumentoApiContractTest {

    @Test
    void preservaRespostaJsonCompletaDaCredencialDeContainer() {
        JsonNode resposta = given()
                .accept(ContentType.JSON)
                .when()
                .post("/simtr-hub/v1/storage/container/credencial")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertJsonExato("""
                {
                  "sas": "sv=mock&ss=b&srt=o&sp=rw&se=2026-07-10T18:00:00Z&sig=mock",
                  "validade": "10/07/2026 18:00:00",
                  "url_storage": "https://dossiedigitaldes.blob.core.windows.net",
                  "nome_container": "pre-validacao"
                }
                """, resposta);
    }
}
