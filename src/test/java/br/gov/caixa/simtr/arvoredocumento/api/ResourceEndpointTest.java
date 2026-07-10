package br.gov.caixa.simtr.arvoredocumento.api;

import br.gov.caixa.simtr.arvoredocumento.TestFixtures;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class ResourceEndpointTest {

    @Test
    void processoEndpointRetorna200ComMockDoQuarkus() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/arvore-documento/v1/processo/identificador-negocial/{identificador}", 1000016487L)
                .then()
                .statusCode(200)
                .body("identificador_negocial", equalTo(1000016487))
                .body("nome", notNullValue());
    }

    @Test
    void checklistEndpointRetorna200ComMockDoQuarkus() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/arvore-documento/v1/checklist/identificador-negocial/{identificador}/versao/{versao}",
                        1000012583L,
                        1)
                .then()
                .statusCode(200)
                .body("identificador_negocial", equalTo(1000012583))
                .body("versao", equalTo(1))
                .body("nome", notNullValue());
    }

    @Test
    void openApiEndpointRetorna200NoPathConvencional() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/arvore-documento/openapi")
                .then()
                .statusCode(200);
    }

    @Test
    void dossieProdutoPostRetorna201ComMockDoQuarkus() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(TestFixtures.dossieCriacaoDto())
                .when()
                .post("/arvore-documento/v1/dossie-produto")
                .then()
                .statusCode(201)
                .body("id", equalTo(1));
    }

    @Test
    void dossieProdutoPatchFormularioRetorna201ComIdDoPath() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(TestFixtures.formularioDto())
                .when()
                .patch("/arvore-documento/v1/dossie-produto/{id}/formulario", 123L)
                .then()
                .statusCode(201)
                .body("id", equalTo(123));
    }

    @Test
    void dossieProdutoPostDocumentoRetorna201ComIdsDoMock() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(TestFixtures.documentoInclusaoDto())
                .when()
                .post("/arvore-documento/v1/dossie-produto/{id}/documento", 123L)
                .then()
                .statusCode(201)
                .body("id_documento", equalTo(456))
                .body("id_instancia_documento", equalTo(789));
    }

    @Test
    void dossieProdutoPostDocumentoAceitaAtributoSemObjeto() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(TestFixtures.documentoInclusaoSemObjetoAtributoDto())
                .when()
                .post("/arvore-documento/v1/dossie-produto/{id}/documento", 123L)
                .then()
                .statusCode(201)
                .body("id_documento", equalTo(456))
                .body("id_instancia_documento", equalTo(789));
    }

    @Test
    void dossieProdutoPatchValidacaoNegocialRetorna200SemCorpo() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(TestFixtures.validacaoNegocialDto())
                .when()
                .patch("/arvore-documento/v1/dossie-produto/{id}/validacao-negocial", 123L)
                .then()
                .statusCode(200);
    }

    @Test
    void dossieProdutoPatchValidacaoNegocialAceitaVerificacaoSemPrevio() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "verificacoes": [
                            {
                              "identificador_checklist": 5000005493,
                              "versao_checklist": 1,
                              "analise_realizada": true,
                              "parecer_apontamentos": [
                                {
                                  "identificador_apontamento": 5000005494,
                                  "resultado": "APROVADO",
                                  "indice_ia": 1,
                                  "necessidade_reanalise": true,
                                  "comentario": "teste"
                                }
                              ]
                            }
                          ],
                          "respostas_formulario": [
                            {
                              "campo_formulario": 1000011176,
                              "resposta": null,
                              "opcoes_selecionadas": [
                                "S"
                              ]
                            }
                          ]
                        }
                        """)
                .when()
                .patch("/arvore-documento/v1/dossie-produto/{id}/validacao-negocial", 123L)
                .then()
                .statusCode(200);
    }

    @Test
    void dossieProdutoPostWorkflowRetorna200ComIdDoPath() {
        given()
                .accept(ContentType.JSON)
                .when()
                .post("/arvore-documento/v1/dossie-produto/{id}/workflow", 123L)
                .then()
                .statusCode(200)
                .body("id", equalTo(123));
    }

    @Test
    void gestaoDocumentoPostCredencialContainerRetorna200SemBodyEContentType() {
        given()
                .accept(ContentType.JSON)
                .when()
                .post("/arvore-documento/v1/storage/container/credencial")
                .then()
                .statusCode(200)
                .body("sas", notNullValue())
                .body("validade", equalTo("10/07/2026 18:00:00"))
                .body("url_storage", equalTo("https://dossiedigitaldes.blob.core.windows.net"))
                .body("nome_container", equalTo("pre-validacao"));
    }

    @Test
    void processoEndpointRetorna400ParaIdentificadorInvalido() {
        given()
                .accept(ContentType.JSON)
                .when()
                .get("/arvore-documento/v1/processo/identificador-negocial/{identificador}", 0)
                .then()
                .statusCode(400)
                .body("codigo_erro", equalTo("ARVDOCP0001"));
    }

    @Test
    void dossieProdutoPatchFormularioRetorna400ParaIdInvalido() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(TestFixtures.formularioDto())
                .when()
                .patch("/arvore-documento/v1/dossie-produto/{id}/formulario", 0)
                .then()
                .statusCode(400)
                .body("codigo_erro", equalTo("ARVDOCP0001"));
    }

    @Test
    void dossieProdutoPostDocumentoRetorna400ParaIdInvalido() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(TestFixtures.documentoInclusaoDto())
                .when()
                .post("/arvore-documento/v1/dossie-produto/{id}/documento", 0)
                .then()
                .statusCode(400)
                .body("codigo_erro", equalTo("ARVDOCP0001"));
    }

    @Test
    void dossieProdutoPatchValidacaoNegocialRetorna400ParaIdInvalido() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(TestFixtures.validacaoNegocialDto())
                .when()
                .patch("/arvore-documento/v1/dossie-produto/{id}/validacao-negocial", 0)
                .then()
                .statusCode(400)
                .body("codigo_erro", equalTo("ARVDOCP0001"));
    }

    @Test
    void dossieProdutoPatchValidacaoNegocialRetorna400ParaCorpoAusente() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .patch("/arvore-documento/v1/dossie-produto/{id}/validacao-negocial", 123L)
                .then()
                .statusCode(400)
                .body("codigo_erro", equalTo("ARVDOCP0001"));
    }

    @Test
    void dossieProdutoPostWorkflowRetorna400ParaIdInvalido() {
        given()
                .accept(ContentType.JSON)
                .when()
                .post("/arvore-documento/v1/dossie-produto/{id}/workflow", 0)
                .then()
                .statusCode(400)
                .body("codigo_erro", equalTo("ARVDOCP0001"));
    }
}
