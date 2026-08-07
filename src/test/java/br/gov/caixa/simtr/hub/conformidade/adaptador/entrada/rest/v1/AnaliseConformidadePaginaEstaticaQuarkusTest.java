package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.rest.v1;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AnaliseConformidadePaginaEstaticaQuarkusTest {

    private static final String DIRETORIO_RECURSOS =
            "META-INF/resources/poc-conformidade/";

    @Test
    void deveServirPaginaEstaticaNoCaminhoDaPoc() {
        given()
                .when()
                .get("/poc-conformidade/")
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"))
                .body(containsString("Análise de conformidade"));
    }

    @Test
    void deveExporFormularioAcessivelEIdentidadesSomenteLeitura() throws IOException {
        String html = carregarRecurso("index.html");

        assertAll(
                () -> assertTrue(html.contains("lang=\"pt-BR\"")),
                () -> assertTrue(html.contains("name=\"viewport\"")),
                () -> assertTrue(html.contains("<main")),
                () -> assertTrue(html.contains("<h1")),
                () -> assertRotulo(html, "identificador-documento"),
                () -> assertRotulo(html, "identificador-checklist"),
                () -> assertRotulo(html, "versao-checklist"),
                () -> assertRotulo(html, "texto-documento"),
                () -> assertTrue(html.contains("aria-live=\"polite\"")),
                () -> assertTrue(html.contains("role=\"alert\"")),
                () -> assertIdentidadeSomenteLeitura(html, "correlation-id"),
                () -> assertIdentidadeSomenteLeitura(html, "instance-id"),
                () -> assertIdentidadeSomenteLeitura(html, "identificador-documento-atual"),
                () -> assertIdentidadeSomenteLeitura(html, "identificador-checklist-atual"),
                () -> assertIdentidadeSomenteLeitura(html, "versao-checklist-atual"));
    }

    @Test
    void deveIntegrarApiComPollingRevisaoESanitizacaoDeErros() throws IOException {
        String javascript = carregarRecurso("app.js");

        assertAll(
                () -> assertTrue(javascript.contains(
                        "/simtr-hub/v1/conformidade/analises")),
                () -> assertTrue(javascript.contains("INTERVALO_POLLING_MS = 1500")),
                () -> assertTrue(javascript.contains("method: \"POST\"")),
                () -> assertTrue(javascript.contains("method: \"GET\"")),
                () -> assertTrue(javascript.contains("method: \"PUT\"")),
                () -> assertTrue(javascript.contains("EM_PROCESSAMENTO")),
                () -> assertTrue(javascript.contains("AGUARDANDO_REVISAO")),
                () -> assertTrue(javascript.contains("CONCLUIDA")),
                () -> assertTrue(javascript.contains("FALHOU")),
                () -> assertTrue(javascript.contains("setTimeout")),
                () -> assertTrue(javascript.contains("clearTimeout")),
                () -> assertTrue(javascript.contains("400")),
                () -> assertTrue(javascript.contains("404")),
                () -> assertTrue(javascript.contains("409")),
                () -> assertTrue(javascript.contains("422")),
                () -> assertTrue(javascript.contains("503")),
                () -> assertFalse(javascript.contains("stacktrace")));
    }

    @Test
    void naoDevePersistirDadosNemCarregarDependenciaExterna() throws IOException {
        String recursos = String.join(
                        "\n",
                        carregarRecurso("index.html"),
                        carregarRecurso("styles.css"),
                        carregarRecurso("app.js"))
                .toLowerCase();

        assertAll(
                () -> assertFalse(recursos.contains("localstorage")),
                () -> assertFalse(recursos.contains("sessionstorage")),
                () -> assertFalse(recursos.contains("https://")),
                () -> assertFalse(recursos.contains("http://")),
                () -> assertTrue(recursos.contains("backend documental")),
                () -> assertTrue(recursos.contains("redis/valkey")),
                () -> assertTrue(recursos.contains("múltiplos pods")));
    }

    private static void assertRotulo(String html, String id) {
        assertTrue(html.contains("for=\"" + id + "\""));
        assertTrue(html.contains("id=\"" + id + "\""));
    }

    private static void assertIdentidadeSomenteLeitura(String html, String id) {
        assertTrue(html.contains("<dd id=\"" + id + "\""));
    }

    private static String carregarRecurso(String nome) throws IOException {
        String caminho = DIRETORIO_RECURSOS + nome;
        try (InputStream recurso = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(caminho)) {
            assertNotNull(recurso, "Recurso estático ausente: " + caminho);
            return new String(recurso.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
