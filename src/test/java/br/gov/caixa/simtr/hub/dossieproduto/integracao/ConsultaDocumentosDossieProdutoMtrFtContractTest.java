package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(ConsultaDocumentosDossieProdutoMtrFtContractTest.FtIsoladoProfile.class)
@QuarkusTestResource(
        value = DossieProdutoMtrStubTestResource.class,
        restrictToAnnotatedClass = true
)
class ConsultaDocumentosDossieProdutoMtrFtContractTest {

    private static final long IDENTIFICADOR = 4_081_899L;
    private static final String CAMINHO_PUBLICO =
            "/simtr-hub/v1/dossie-produto/{id}/documentos";

    @BeforeEach
    void limparStubMtr() {
        DossieProdutoMtrStubTestResource.reset();
    }

    @Test
    void erro500ExaureTresRetriesEPreservaUltimoCorpo() {
        DossieProdutoMtrStubTestResource.responderRepetidamente(
                4,
                500,
                erroMtr500());

        JsonNode erro = given()
                .accept(ContentType.JSON)
                .when()
                .get(CAMINHO_PUBLICO, IDENTIFICADOR)
                .then()
                .statusCode(500)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(500, erro.path("codigo_http").intValue());
        assertEquals("MTR-DOCUMENTOS-500", erro.path("codigo_erro").textValue());
        assertEquals("ERRO_EXTERNO_500",
                erro.path("erros").get(0).path("mensagem").textValue());
        assertEquals(4, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    private static String erroMtr500() {
        return """
                {
                  "codigo_http": 500,
                  "recurso": "simtr-dossie-produto",
                  "id_erro": "documentos-500",
                  "codigo_erro": "MTR-DOCUMENTOS-500",
                  "erros": [{"mensagem": "ERRO_EXTERNO_500"}],
                  "detalhe": "DETALHE_EXTERNO_500",
                  "stacktrace": "STACKTRACE_EXTERNO_500"
                }
                """;
    }

    public static final class FtIsoladoProfile implements QuarkusTestProfile {
    }
}
