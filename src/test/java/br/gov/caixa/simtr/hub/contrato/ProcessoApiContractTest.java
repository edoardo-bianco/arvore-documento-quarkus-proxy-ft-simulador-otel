package br.gov.caixa.simtr.hub.contrato;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static br.gov.caixa.simtr.hub.contrato.JsonContractAssertions.assertErroValidacaoExato;
import static br.gov.caixa.simtr.hub.contrato.JsonContractAssertions.assertFingerprint;
import static io.restassured.RestAssured.given;

@QuarkusTest
class ProcessoApiContractTest {

    private static final String PATH =
            "/simtr-hub/v1/processo/identificador-negocial/{identificador}";
    private static final String FINGERPRINT_RESPOSTA =
            "01d6ac0993410e4cb8edc9c338a333acd56fa93390054c31907351ca19d70d34";

    @Test
    void preservaRespostaJsonCompletaDaConsultaDeProcesso() {
        JsonNode resposta = given()
                .accept(ContentType.JSON)
                .when()
                .get(PATH, 1000016487L)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(JsonNode.class);

        assertFingerprint(FINGERPRINT_RESPOSTA, resposta);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void preservaErroPublicoParaIdentificadorDeProcessoInvalido(long identificador) {
        JsonNode erro = given()
                .accept(ContentType.JSON)
                .when()
                .get(PATH, identificador)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract()
                .as(JsonNode.class);

        assertErroValidacaoExato(erro, "O identificador negocial deve ser maior que zero.");
    }
}
