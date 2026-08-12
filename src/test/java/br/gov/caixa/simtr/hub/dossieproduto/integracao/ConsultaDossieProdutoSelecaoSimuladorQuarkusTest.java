package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ConsultaDossieProdutoSelecaoSimuladorQuarkusTest {

    private static final long IDENTIFICADOR = 4_324_680L;
    private static final String CAMINHO_PUBLICO = "/simtr-hub/v1/dossie-produto/{id}";
    private static final String CONFIG_SIMULADOR =
            "simtr-hub.simulador.dossie-produto.habilitado";
    private static final String CPF_SIMULADO = "00000000000";
    private static final String NOME_SIMULADO = "CLIENTE SIMULADO";

    @ConfigProperty(name = CONFIG_SIMULADOR)
    boolean simuladorHabilitado;

    @Test
    void selecionaSimuladorPelaApiPublicaELêFixtureSemAcessoMtr() {
        JsonNode resposta = given()
                .accept(ContentType.JSON)
                .when()
                .get(CAMINHO_PUBLICO, IDENTIFICADOR)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertTrue(simuladorHabilitado);
        assertEquals(IDENTIFICADOR, resposta.path("id").longValue());
        assertTrue(resposta.path("instancia_jbpm").isNull());
        assertTrue(resposta.path("numero_negocio").isNull());
        assertEquals(CPF_SIMULADO, resposta.path("clientes").get(0).path("cpf").textValue());
        assertEquals(NOME_SIMULADO, resposta.path("clientes").get(0).path("nome").textValue());
        assertTrue(resposta.path("unidades_tratamento").isEmpty());
        assertTrue(resposta.path("produtos_contratados").get(0).path("id").isNull());
    }
}
