package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class CapturaDossieProdutoSelecaoSimuladorQuarkusTest {

    private static final long IDENTIFICADOR = 123L;
    private static final long IDENTIFICADOR_NAO_DISPONIVEL = 13L;
    private static final String CAMINHO_PUBLICO =
            "/simtr-hub/v1/dossie-produto/{id}/capturar";

    @ConfigProperty(name = "simtr-hub.simulador.dossie-produto.habilitado")
    boolean simuladorHabilitado;

    @ConfigProperty(name = "quarkus.rest-client.dossie-produto.url")
    String enderecoMtr;

    @Test
    void selecionaSimuladorPelaRotaPublicaComMtrIndisponivel() {
        JsonNode resposta = given()
                .accept(ContentType.JSON)
                .when()
                .post(CAMINHO_PUBLICO, IDENTIFICADOR)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertTrue(simuladorHabilitado);
        assertEquals("http://localhost:1", enderecoMtr);
        assertEquals(IDENTIFICADOR, resposta.path("id").longValue());
    }

    @Test
    void informaIdentificadoresSolicitadoEDisponivelNo404Simulado() {
        JsonNode resposta = given()
                .accept(ContentType.JSON)
                .when()
                .post(CAMINHO_PUBLICO, IDENTIFICADOR_NAO_DISPONIVEL)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertTrue(simuladorHabilitado);
        assertEquals(404, resposta.path("codigo_http").intValue());
        assertEquals("simtr-dossie-produto", resposta.path("recurso").textValue());
        assertEquals("mock-captura-dossie-produto-13", resposta.path("id_erro").textValue());
        assertEquals("DOSSIE_PRODUTO_NAO_ENCONTRADO", resposta.path("codigo_erro").textValue());
        assertEquals(1, resposta.path("erros").size());
        assertEquals(
                "Dossie produto 13 nao encontrado no simulador. "
                        + "O unico identificador disponivel no simulador e 123.",
                resposta.path("erros").path(0).path("mensagem").textValue());
    }
}
