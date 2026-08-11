package br.gov.caixa.simtr.hub.contrato;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class DossieProdutoOpenApiContractTest {

    private static final String OPERACAO_PRODUTO =
            "/paths/~1simtr-hub~1v1~1dossie-produto~1{id}~1produto/patch";
    private static final String SCHEMA_PRODUTO =
            "/components/schemas/AlteracaoProdutoDossieProdutoRequest";

    @Test
    void descreveContratoPublicoDeAlteracaoDeProdutos() {
        JsonNode openApi = given()
                .accept(ContentType.JSON)
                .queryParam("format", "JSON")
                .when()
                .get("/simtr-hub/openapi")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        JsonNode operacao = openApi.at(OPERACAO_PRODUTO);
        assertFalse(operacao.isMissingNode(), "OpenAPI deve expor o PATCH de produto");

        JsonNode requestBody = operacao.path("requestBody");
        assertTrue(requestBody.path("required").asBoolean(),
                "corpo da alteracao de produtos deve ser obrigatorio");

        JsonNode schemaLista = requestBody.at("/content/application~1json/schema");
        assertEquals("array", schemaLista.path("type").asText());
        assertEquals("#/components/schemas/AlteracaoProdutoDossieProdutoRequest",
                schemaLista.at("/items/$ref").asText());

        JsonNode schemaProduto = openApi.at(SCHEMA_PRODUTO);
        assertFalse(schemaProduto.isMissingNode(), "OpenAPI deve declarar o item de produto");
        assertEquals(Set.of("codigo_operacao", "codigo_modalidade"),
                nomes(schemaProduto.path("required")));
        assertEquals("integer", schemaProduto.at("/properties/codigo_operacao/type").asText());
        assertEquals("int32", schemaProduto.at("/properties/codigo_operacao/format").asText());
        assertEquals("integer", schemaProduto.at("/properties/codigo_modalidade/type").asText());
        assertEquals("int32", schemaProduto.at("/properties/codigo_modalidade/format").asText());
        assertEquals("boolean", schemaProduto.at("/properties/excluir/type").asText());

        JsonNode resposta200 = operacao.at("/responses/200");
        assertFalse(resposta200.isMissingNode(), "OpenAPI deve declarar sucesso 200");
        assertTrue(resposta200.get("content") == null || resposta200.path("content").isEmpty(),
                "resposta 200 nao deve declarar corpo");
    }

    private static Set<String> nomes(JsonNode valores) {
        Set<String> nomes = new HashSet<>();
        valores.forEach(valor -> nomes.add(valor.asText()));
        return nomes;
    }
}
