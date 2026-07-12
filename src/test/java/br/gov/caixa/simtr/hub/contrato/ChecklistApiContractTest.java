package br.gov.caixa.simtr.hub.contrato;

import br.gov.caixa.simtr.hub.arquitetura.configuracao.mock.MarkdownJsonMockReader;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ChecklistApiContractTest {

    private static final String PATH =
            "/simtr-hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}";
    private static final String MOCK =
            "mock/parametrizacao/1000012583-v1-checklist-parametrizacao-versao-1.md";

    @Inject
    MarkdownJsonMockReader mockReader;

    @Test
    void preservaRespostaJsonCompletaDaConsultaDeChecklist() {
        JsonNode esperado = removerCamposNulos(mockReader.readFirstJsonObject(MOCK, JsonNode.class));

        JsonNode resposta = given()
                .accept(ContentType.JSON)
                .when()
                .get(PATH, 1000012583L, 1)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(JsonNode.class);

        assertEquals(esperado, resposta);
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

        assertEquals(400, erro.path("codigo_http").asInt());
        assertEquals("simtr-hub", erro.path("recurso").asText());
        assertEquals("ARVDOCP0001", erro.path("codigo_erro").asText());
        assertEquals(mensagem, erro.path("erros").path(0).path("mensagem").asText());
        assertTrue(uuidValido(erro.path("id_erro").asText()));
        assertFalse(erro.has("detalhe"));
        assertFalse(erro.has("stacktrace"));
    }

    private static boolean uuidValido(String valor) {
        try {
            UUID.fromString(valor);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static JsonNode removerCamposNulos(JsonNode node) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> campos = node.fields();
            while (campos.hasNext()) {
                Map.Entry<String, JsonNode> campo = campos.next();
                if (campo.getValue().isNull()) {
                    campos.remove();
                } else {
                    removerCamposNulos(campo.getValue());
                }
            }
        } else if (node.isArray()) {
            node.forEach(ChecklistApiContractTest::removerCamposNulos);
        }
        return node;
    }
}
