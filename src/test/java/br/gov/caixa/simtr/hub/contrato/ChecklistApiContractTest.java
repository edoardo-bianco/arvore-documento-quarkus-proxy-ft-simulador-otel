package br.gov.caixa.simtr.hub.contrato;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static br.gov.caixa.simtr.hub.contrato.JsonContractAssertions.assertErroValidacaoExato;
import static br.gov.caixa.simtr.hub.contrato.JsonContractAssertions.assertFingerprint;
import static io.restassured.RestAssured.given;

@QuarkusTest
class ChecklistApiContractTest {

    private static final String PATH =
            "/simtr-hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}";
    private static final String FINGERPRINT_RESPOSTA =
            "fa497a3a7f7feea380be4312bb5ad8231c385c7a5aa10043b5092971fdb816aa";

    @Test
    void preservaRespostaJsonCompletaDaConsultaDeChecklist() {
        JsonNode resposta = given()
                .accept(ContentType.JSON)
                .when()
                .get(PATH, 1000012583L, 1)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(JsonNode.class);

        assertFingerprint(FINGERPRINT_RESPOSTA, resposta);
    }

    @Test
    void preservaErroPublicoParaIdentificadorDeChecklistInvalido() {
        validarErro(0, 1, "O identificador negocial deve ser maior que zero.");
    }

    @Test
    void preservaErroPublicoParaVersaoDeChecklistInvalida() {
        validarErro(1000012583L, 0, "A versão do checklist deve ser maior que zero.");
    }

    private static void validarErro(long identificador, int versao, String mensagem) {
        JsonNode erro = given()
                .accept(ContentType.JSON)
                .when()
                .get(PATH, identificador, versao)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract()
                .as(JsonNode.class);

        assertErroValidacaoExato(erro, mensagem);
    }
}
