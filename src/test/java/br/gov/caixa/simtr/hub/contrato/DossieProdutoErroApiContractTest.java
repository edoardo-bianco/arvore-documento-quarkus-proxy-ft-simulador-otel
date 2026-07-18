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

    private static final String ROTA_DOSSIE_PRODUTO = "/simtr-hub/v1/dossie-produto";
    private static final String ROTA_DOCUMENTO_DOSSIE_PRODUTO =
            "/simtr-hub/v1/dossie-produto/{id}/documento";
    private static final String ROTA_VALIDACAO_NEGOCIAL_DOSSIE_PRODUTO =
            "/simtr-hub/v1/dossie-produto/{id}/validacao-negocial";
    private static final String MENSAGEM_CORPO_OBRIGATORIO =
            "O corpo da requisicao deve ser informado.";
    private static final String MENSAGEM_ID =
            "O identificador do dossie produto deve ser maior que zero.";

    @Test
    void preservaErroParaCorpoDeCriacaoAusente() {
        validarErro(given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post(ROTA_DOSSIE_PRODUTO),
                MENSAGEM_CORPO_OBRIGATORIO);
    }

    @Test
    void preservaErrosCompletosParaCamposObrigatoriosDaCriacao() {
        validarErro(given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .body("{}")
                        .post(ROTA_DOSSIE_PRODUTO),
                "A chave de correlacao do canal deve ser informada.",
                "O processo deve ser informado.");
    }

    @Test
    void preservaErroParaCorpoDeFormularioAusente() {
        validarErro(given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .patch("/simtr-hub/v1/dossie-produto/{id}/formulario", 123L),
                MENSAGEM_CORPO_OBRIGATORIO);
    }

    @Test
    void preservaErroParaCorpoDeDocumentoAusente() {
        validarErro(given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .post(ROTA_DOCUMENTO_DOSSIE_PRODUTO, 123L),
                MENSAGEM_CORPO_OBRIGATORIO);
    }

    @Test
    void preservaErroParaCorpoDeValidacaoNegocialAusente() {
        validarErro(given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .patch(ROTA_VALIDACAO_NEGOCIAL_DOSSIE_PRODUTO, 123L),
                MENSAGEM_CORPO_OBRIGATORIO);
    }

    @Test
    void preservaTodasAsValidacoesObrigatoriasDeAtributosEPropriedadesDoDocumento() {
        validarErro(given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .body("{\"atributos\":[{}],\"propriedades\":[{}]}")
                        .post(ROTA_DOCUMENTO_DOSSIE_PRODUTO, 123L),
                "A chave do atributo do documento deve ser informada.",
                "O valor do atributo do documento deve ser informado.",
                "A chave da propriedade do documento deve ser informada.",
                "O valor da propriedade do documento deve ser informado.");
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
                        .patch(ROTA_VALIDACAO_NEGOCIAL_DOSSIE_PRODUTO, 123L),
                "O identificador do checklist deve ser informado.");
    }

    @Test
    void preservaTodasAsValidacoesObrigatoriasDaValidacaoNegocial() {
        validarErro(given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .body("""
                                {
                                  "verificacoes": [
                                    {
                                      "identificador_checklist": 6592,
                                      "versao_checklist": 2,
                                      "analise_realizada": true,
                                      "parecer_apontamentos": null
                                    },
                                    {
                                      "parecer_apontamentos": [{}],
                                      "produto": {}
                                    }
                                  ],
                                  "respostas_formulario": [{}]
                                }
                                """)
                        .patch(ROTA_VALIDACAO_NEGOCIAL_DOSSIE_PRODUTO, 123L),
                "Os pareceres dos apontamentos devem ser informados.",
                "O identificador do checklist deve ser informado.",
                "A versao do checklist deve ser informada.",
                "O indicador de analise realizada deve ser informado.",
                "O identificador do apontamento deve ser informado.",
                "O resultado do apontamento deve ser informado.",
                "A necessidade de reanalise do apontamento deve ser informada.",
                "O codigo da operacao do produto deve ser informado.",
                "O codigo da modalidade do produto deve ser informado.",
                "O campo do formulario deve ser informado.");
    }

    @Test
    void preservaErroSemCorpoParaJsonMalformado() {
        String resposta = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("{")
                .post(ROTA_DOSSIE_PRODUTO)
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
                .post(ROTA_DOCUMENTO_DOSSIE_PRODUTO, 0), MENSAGEM_ID);
    }

    @Test
    void preservaErroParaIdInvalidoNaValidacaoNegocial() {
        validarErro(given().contentType(ContentType.JSON).accept(ContentType.JSON)
                .body("{}")
                .patch(ROTA_VALIDACAO_NEGOCIAL_DOSSIE_PRODUTO, 0), MENSAGEM_ID);
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
