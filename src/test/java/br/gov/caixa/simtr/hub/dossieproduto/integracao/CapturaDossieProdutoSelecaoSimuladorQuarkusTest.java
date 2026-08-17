package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class CapturaDossieProdutoSelecaoSimuladorQuarkusTest {

    private static final long IDENTIFICADOR = 123L;
    private static final String CAMINHO_PUBLICO =
            "/simtr-hub/v1/dossie-produto/{id}/capturar";

    @ConfigProperty(name = "simtr-hub.simulador.dossie-produto.habilitado")
    boolean simuladorHabilitado;

    @ConfigProperty(name = "quarkus.rest-client.dossie-produto.url")
    String enderecoMtr;

    @Test
    void selecionaSimuladorPelaRotaPublicaComMtrIndisponivel() {
        JsonNode resposta = given()
                .accept(ContentType.JSON)
                .when()
                .post(CAMINHO_PUBLICO, IDENTIFICADOR)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertTrue(simuladorHabilitado);
        assertEquals("http://localhost:1", enderecoMtr);
        assertEquals(IDENTIFICADOR, resposta.path("id").longValue());
    }
}
