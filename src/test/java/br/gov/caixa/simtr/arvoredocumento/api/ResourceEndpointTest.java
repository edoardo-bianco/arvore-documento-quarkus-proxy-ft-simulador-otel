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
}
