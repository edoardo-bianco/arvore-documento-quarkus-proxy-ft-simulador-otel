package br.gov.caixa.simtr.hub.contrato;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static br.gov.caixa.simtr.hub.contrato.JsonContractAssertions.assertErroValidacaoExato;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void preservaErrosCompletosParaCamposObrigatoriosDaCriacao() {
        validarErro(given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .body("{}")
                        .post("/simtr-hub/v1/dossie-produto"),
                "A chave de correlacao do canal deve ser informada.",
                "O processo deve ser informado.");
    }

    @Test
    void preservaErroDeValidacaoCascataDoAtributoDeDocumento() {
        validarErro(given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .body("{\"atributos\":[{\"valor\":\"123\"}]}")
                        .post("/simtr-hub/v1/dossie-produto/{id}/documento", 123L),
                "A chave do atributo do documento deve ser informada.");
    }

    @Test
    void preservaErroDeValidacaoCascataDaVerificacaoNegocial() {
        validarErro(given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .body("""
                                {
                                  "verificacoes": [{
                                    "versao_checklist": 1,
                                    "analise_realizada": true,
                                    "parecer_apontamentos": []
                                  }]
                                }
                                """)
                        .patch("/simtr-hub/v1/dossie-produto/{id}/validacao-negocial", 123L),
                "O identificador do checklist deve ser informado.");
    }

    @Test
    void preservaErroSemCorpoParaJsonMalformado() {
        String resposta = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("{")
                .post("/simtr-hub/v1/dossie-produto")
                .then()
                .statusCode(400)
                .extract().asString();

        assertEquals("", resposta);
    }

    @Test
    void preservaErroParaIdInvalidoNoFormulario() {
        validarErro(given().contentType(ContentType.JSON).accept(ContentType.JSON)
                .body("[]")
                .patch("/simtr-hub/v1/dossie-produto/{id}/formulario", 0), MENSAGEM_ID);
    }

    @Test
    void preservaErroParaIdInvalidoNaInclusaoDeDocumento() {
        validarErro(given().contentType(ContentType.JSON).accept(ContentType.JSON)
                .body("{}")
                .post("/simtr-hub/v1/dossie-produto/{id}/documento", 0), MENSAGEM_ID);
    }

    @Test
    void preservaErroParaIdInvalidoNaValidacaoNegocial() {
        validarErro(given().contentType(ContentType.JSON).accept(ContentType.JSON)
                .body("{}")
                .patch("/simtr-hub/v1/dossie-produto/{id}/validacao-negocial", 0), MENSAGEM_ID);
    }

    @Test
    void preservaErroParaIdInvalidoNoWorkflow() {
        validarErro(given().accept(ContentType.JSON)
                .post("/simtr-hub/v1/dossie-produto/{id}/workflow", 0), MENSAGEM_ID);
    }

    private static void validarErro(Response response, String... mensagens) {
        JsonNode erro = response.then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertErroValidacaoExato(erro, mensagens);
    }
}
