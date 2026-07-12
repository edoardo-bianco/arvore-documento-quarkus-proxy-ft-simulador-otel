package br.gov.caixa.simtr.hub.contrato;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static br.gov.caixa.simtr.hub.contrato.JsonContractAssertions.assertFingerprint;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@QuarkusTest
class OpenApiContractTest {

    private static final Set<String> METODOS_HTTP = Set.of("get", "post", "put", "patch", "delete");
    private static final String RESPOSTAS_ERRO_DOSSIE =
            "400=application/json:#/components/schemas/ErroPadraoDto;"
                    + "401=application/json:#/components/schemas/ErroPadraoDto;"
                    + "403=application/json:#/components/schemas/ErroPadraoDto;"
                    + "404=application/json:#/components/schemas/ErroPadraoDto;"
                    + "409=application/json:#/components/schemas/ErroPadraoDto;"
                    + "500=application/json:#/components/schemas/ErroPadraoDto";
    private static final Map<String, String> OPERACOES_ESPERADAS = Map.ofEntries(
            Map.entry("GET /simtr-hub/v1/checklist/identificador-negocial/{identificador}/versao/{versao}",
                    "tags=Parametrização - Checklist|operationId=<absent>|security=<absent>"
                            + "|parameters=identificador:path:required=true:integer:int64:minimum=1;"
                            + "versao:path:required=true:integer:int32:minimum=1|request=<absent>"
                            + "|responses=200=application/json:#/components/schemas/ChecklistDto;204=<absent>;"
                            + "400=application/json:#/components/schemas/ErroPadraoDto;"
                            + "500=application/json:#/components/schemas/ErroPadraoDto"),
            Map.entry("GET /simtr-hub/v1/processo/identificador-negocial/{identificador}",
                    "tags=Parametrização - Processo|operationId=<absent>|security=<absent>"
                            + "|parameters=identificador:path:required=true:integer:int64:minimum=1|request=<absent>"
                            + "|responses=200=application/json:#/components/schemas/ProcessoDto;"
                            + "400=application/json:#/components/schemas/ErroPadraoDto;"
                            + "404=application/json:#/components/schemas/ErroPadraoDto;"
                            + "500=application/json:#/components/schemas/ErroPadraoDto"),
            Map.entry("PATCH /simtr-hub/v1/dossie-produto/{id}/formulario",
                    "tags=Negócio - Dossiê Produto|operationId=<absent>|security=<absent>"
                            + "|parameters=id:path:required=true:integer:int64:minimum=1"
                            + "|request=required=true,content=application/json:array<"
                            + "#/components/schemas/DossieProdutoFormularioDto>"
                            + "|responses=201=application/json:#/components/schemas/DossieProdutoCriadoDto;"
                            + RESPOSTAS_ERRO_DOSSIE),
            Map.entry("PATCH /simtr-hub/v1/dossie-produto/{id}/validacao-negocial",
                    "tags=Negócio - Dossiê Produto|operationId=<absent>|security=<absent>"
                            + "|parameters=id:path:required=true:integer:int64:minimum=1"
                            + "|request=required=true,content=application/json:"
                            + "#/components/schemas/DossieProdutoValidacaoNegocialDto"
                            + "|responses=200=application/json:{};" + RESPOSTAS_ERRO_DOSSIE
                            + ";503=application/json:#/components/schemas/ErroPadraoDto"),
            Map.entry("POST /simtr-hub/v1/dossie-produto",
                    "tags=Negócio - Dossiê Produto|operationId=<absent>|security=<absent>|parameters="
                            + "|request=required=true,content=application/json:"
                            + "#/components/schemas/DossieProdutoCriacaoDto"
                            + "|responses=201=application/json:#/components/schemas/DossieProdutoCriadoDto;"
                            + RESPOSTAS_ERRO_DOSSIE),
            Map.entry("POST /simtr-hub/v1/dossie-produto/{id}/documento",
                    "tags=Negócio - Dossiê Produto|operationId=<absent>|security=<absent>"
                            + "|parameters=id:path:required=true:integer:int64:minimum=1"
                            + "|request=required=true,content=application/json:"
                            + "#/components/schemas/DossieProdutoDocumentoInclusaoDto"
                            + "|responses=201=application/json:#/components/schemas/DossieProdutoDocumentoCriadoDto;"
                            + RESPOSTAS_ERRO_DOSSIE),
            Map.entry("POST /simtr-hub/v1/dossie-produto/{id}/workflow",
                    "tags=Negócio - Dossiê Produto|operationId=<absent>|security=<absent>"
                            + "|parameters=id:path:required=true:integer:int64:minimum=1|request=<absent>"
                            + "|responses=200=application/json:#/components/schemas/DossieProdutoCriadoDto;"
                            + RESPOSTAS_ERRO_DOSSIE),
            Map.entry("POST /simtr-hub/v1/storage/container/credencial",
                    "tags=Negocio - Storage|operationId=<absent>|security=<absent>|parameters=|request=<absent>"
                            + "|responses=200=application/json:#/components/schemas/"
                            + "GestaoDocumentoCredencialContainerDto;"
                            + "401=application/json:#/components/schemas/ErroPadraoDto;"
                            + "403=application/json:#/components/schemas/ErroPadraoDto;"
                            + "500=application/json:#/components/schemas/ErroPadraoDto;"
                            + "503=application/json:#/components/schemas/ErroPadraoDto")
    );
    private static final String FINGERPRINT_OPENAPI =
            "85c9bab0170fec09ec0d76b483b468410b5d819ebf54827ddc9f2a5fde874e88";

    @Test
    void preservaSemanticaDasOitoOperacoesPublicas() {
        JsonNode openApi = openApi();

        assertEquals("3.1.0", openApi.path("openapi").asText());
        assertEquals("simtr-hub-test API", openApi.path("info").path("title").asText());
        assertEquals("1.0.0-SNAPSHOT", openApi.path("info").path("version").asText());
        assertFalse(openApi.has("security"));
        assertEquals(42, openApi.path("components").path("schemas").size());
        assertEquals(OPERACOES_ESPERADAS, extrairOperacoes(openApi));
    }

    @Test
    void preservaDocumentoOpenApiCompletoSemDependerDaOrdemDasPropriedades() {
        assertFingerprint(FINGERPRINT_OPENAPI, openApi());
    }

    private static JsonNode openApi() {
        return given()
                .accept(ContentType.JSON)
                .when()
                .get("/simtr-hub/openapi?format=json")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);
    }

    private static Map<String, String> extrairOperacoes(JsonNode openApi) {
        Map<String, String> operacoes = new TreeMap<>();
        Iterator<Map.Entry<String, JsonNode>> paths = openApi.path("paths").fields();

        while (paths.hasNext()) {
            Map.Entry<String, JsonNode> path = paths.next();
            if (!path.getKey().startsWith("/simtr-hub/v1/")) {
                continue;
            }
            Iterator<Map.Entry<String, JsonNode>> metodos = path.getValue().fields();
            while (metodos.hasNext()) {
                Map.Entry<String, JsonNode> metodo = metodos.next();
                if (METODOS_HTTP.contains(metodo.getKey())) {
                    String chave = metodo.getKey().toUpperCase() + " " + path.getKey();
                    operacoes.put(chave, descreverOperacao(metodo.getValue()));
                }
            }
        }

        return operacoes;
    }

    private static String descreverOperacao(JsonNode operacao) {
        List<String> tags = textos(operacao.path("tags"));
        List<String> parametros = new ArrayList<>();
        operacao.path("parameters").forEach(parametro -> parametros.add(
                parametro.path("name").asText()
                        + ":" + parametro.path("in").asText()
                        + ":required=" + parametro.path("required").asBoolean()
                        + ":" + descreverSchema(parametro.path("schema"))));
        parametros.sort(String::compareTo);

        String request = operacao.has("requestBody")
                ? "required=" + operacao.path("requestBody").path("required").asBoolean()
                    + ",content=" + descreverContent(operacao.path("requestBody").path("content"))
                : "<absent>";

        List<String> respostas = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> responseFields = operacao.path("responses").fields();
        while (responseFields.hasNext()) {
            Map.Entry<String, JsonNode> resposta = responseFields.next();
            respostas.add(resposta.getKey() + "=" + descreverContent(resposta.getValue().path("content")));
        }
        respostas.sort(String::compareTo);

        return "tags=" + String.join(",", tags)
                + "|operationId=" + textoOuAusente(operacao, "operationId")
                + "|security=" + (operacao.has("security") ? operacao.path("security") : "<absent>")
                + "|parameters=" + String.join(";", parametros)
                + "|request=" + request
                + "|responses=" + String.join(";", respostas);
    }

    private static String descreverContent(JsonNode content) {
        if (!content.isObject() || content.isEmpty()) {
            return "<absent>";
        }

        List<String> descricoes = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> mediaTypes = content.fields();
        while (mediaTypes.hasNext()) {
            Map.Entry<String, JsonNode> mediaType = mediaTypes.next();
            descricoes.add(mediaType.getKey() + ":" + descreverSchema(mediaType.getValue().path("schema")));
        }
        descricoes.sort(String::compareTo);
        return String.join(",", descricoes);
    }

    private static String descreverSchema(JsonNode schema) {
        if (!schema.isObject() || schema.isEmpty()) {
            return "{}";
        }
        if (schema.has("$ref")) {
            return schema.path("$ref").asText();
        }
        if (schema.path("type").asText().equals("array")) {
            return "array<" + descreverSchema(schema.path("items")) + ">";
        }

        String descricao = schema.path("type").asText("<sem-tipo>");
        if (schema.has("format")) {
            descricao += ":" + schema.path("format").asText();
        }
        if (schema.has("minimum")) {
            descricao += ":minimum=" + schema.path("minimum").asText();
        }
        return descricao;
    }

    private static List<String> textos(JsonNode array) {
        List<String> valores = new ArrayList<>();
        array.forEach(valor -> valores.add(valor.asText()));
        valores.sort(String::compareTo);
        return valores;
    }

    private static String textoOuAusente(JsonNode node, String campo) {
        return node.has(campo) ? node.path(campo).asText() : "<absent>";
    }
}
