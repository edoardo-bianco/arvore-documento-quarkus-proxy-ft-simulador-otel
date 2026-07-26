package br.gov.caixa.simtr.hub.contrato;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.ConsultarAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.IniciarAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.entrada.RevisarAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.erro.FalhaAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.VisaoAnaliseConformidade;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.smallrye.mutiny.Uni;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static br.gov.caixa.simtr.hub.contrato.JsonContractAssertions.assertErroValidacaoExato;
import static br.gov.caixa.simtr.hub.contrato.JsonContractAssertions.assertJsonExato;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@QuarkusTest
class AnaliseConformidadeApiContractTest {

    private static final String BASE_PATH = "/simtr-hub/v1/conformidade/analises";
    private static final String CORRELATION_ID = "7aa3ca4d-3c7e-4f61-a3a1-996571d3397a";
    private static final String IDENTIFICADOR_DOCUMENTO = "DOC-2026-000123";
    private static final String REVISAO_VALIDA = """
            {
              "observacao": "Revisão concluída",
              "apontamentos": [{
                "identificadorApontamento": 10,
                "nomeApontamento": "Documento identificado",
                "parecer": "CONFORME",
                "justificativa": "Confirmado",
                "evidencia": null,
                "confianca": 0.5
              }]
            }
            """;

    @InjectMock
    IniciarAnaliseConformidade iniciar;

    @InjectMock
    ConsultarAnaliseConformidade consultar;

    @InjectMock
    RevisarAnaliseConformidade revisar;

    @Test
    void preservaContratoJsonELocationDoInicio() {
        when(iniciar.executar(any()))
                .thenReturn(Uni.createFrom().item(visaoEmProcessamento()));

        JsonNode resposta = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "identificadorDocumento": "DOC-2026-000123",
                          "texto": "Texto para análise",
                          "identificadorChecklist": 1000012583,
                          "versaoChecklist": 1
                        }
                        """)
                .when()
                .post(BASE_PATH)
                .then()
                .statusCode(202)
                .header("Location", BASE_PATH + "/instancia-contrato")
                .contentType(ContentType.JSON)
                .extract()
                .as(JsonNode.class);

        assertJsonExato("""
                {
                  "correlationId": "7aa3ca4d-3c7e-4f61-a3a1-996571d3397a",
                  "instanceId": "instancia-contrato",
                  "identificadorDocumento": "DOC-2026-000123",
                  "identificadorChecklist": 1000012583,
                  "versaoChecklist": 1,
                  "status": "EM_PROCESSAMENTO"
                }
                """, resposta);
    }

    @Test
    void preservaContratoJsonDaConsultaEmProcessamento() {
        when(consultar.executar("instancia-contrato"))
                .thenReturn(Uni.createFrom().item(visaoEmProcessamento()));

        JsonNode resposta = given()
                .accept(ContentType.JSON)
                .when()
                .get(BASE_PATH + "/{instanceId}", "instancia-contrato")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(JsonNode.class);

        assertJsonExato("""
                {
                  "correlationId": "7aa3ca4d-3c7e-4f61-a3a1-996571d3397a",
                  "instanceId": "instancia-contrato",
                  "identificadorDocumento": "DOC-2026-000123",
                  "identificadorChecklist": 1000012583,
                  "versaoChecklist": 1,
                  "status": "EM_PROCESSAMENTO",
                  "resultadoPreliminar": null,
                  "resultadoFinal": null,
                  "mensagemErro": null
                }
                """, resposta);
    }

    @Test
    void preservaValidacoesDaSolicitacao() {
        validarErro400(
                given().contentType(ContentType.JSON).accept(ContentType.JSON)
                        .body("{}").post(BASE_PATH),
                "O identificador do documento deve ser informado.",
                "O texto deve ser informado.",
                "O identificador do checklist deve ser informado.",
                "A versão do checklist deve ser informada.");

        validarErro400(
                given().contentType(ContentType.JSON).accept(ContentType.JSON)
                        .body("""
                                {
                                  "identificadorDocumento": "DOC-2026-000123",
                                  "texto": "%s",
                                  "identificadorChecklist": 1000012583,
                                  "versaoChecklist": 1
                                }
                                """.formatted("x".repeat(20_001)))
                        .post(BASE_PATH),
                "O texto deve possuir no máximo 20000 caracteres.");

        validarErro400(
                given().contentType(ContentType.JSON).accept(ContentType.JSON)
                        .body("""
                                {
                                  "identificadorDocumento": "DOC-2026-000123",
                                  "texto": "Texto para análise",
                                  "identificadorChecklist": 0,
                                  "versaoChecklist": 0
                                }
                                """)
                        .post(BASE_PATH),
                "O identificador do checklist deve ser maior que zero.",
                "A versão do checklist deve ser maior que zero.");
    }

    @Test
    void preservaValidacoesDaRevisao() {
        validarErro400(
                given().contentType(ContentType.JSON).accept(ContentType.JSON)
                        .body("{}").put(BASE_PATH + "/{instanceId}/revisao", "instancia-contrato"),
                "Os apontamentos da revisão devem ser informados.");

        validarErro400(
                given().contentType(ContentType.JSON).accept(ContentType.JSON)
                        .body("{\"apontamentos\":[{}]}")
                        .put(BASE_PATH + "/{instanceId}/revisao", "instancia-contrato"),
                "O identificador do apontamento deve ser informado.",
                "O nome do apontamento deve ser informado.",
                "O parecer do apontamento deve ser informado.",
                "A justificativa do apontamento deve ser informada.",
                "A confiança do apontamento deve ser informada.");

        validarErro400(
                given().contentType(ContentType.JSON).accept(ContentType.JSON)
                        .body("{\"apontamentos\":[null]}")
                        .put(BASE_PATH + "/{instanceId}/revisao", "instancia-contrato"),
                "O apontamento da revisão deve ser informado.");

        validarErro400(
                given().contentType(ContentType.JSON).accept(ContentType.JSON)
                        .body("""
                                {
                                  "apontamentos": [{
                                    "identificadorApontamento": 10,
                                    "nomeApontamento": "Documento identificado",
                                    "parecer": "CONFORME",
                                    "justificativa": "Confirmado",
                                    "confianca": 1.1
                                  }]
                                }
                                """)
                        .put(BASE_PATH + "/{instanceId}/revisao", "instancia-contrato"),
                "A confiança do apontamento deve estar entre 0 e 1.");
    }

    @Test
    void preserva404ParaInstanciaDesconhecida() {
        when(consultar.executar("desconhecida"))
                .thenReturn(Uni.createFrom().failure(
                        FalhaAnaliseConformidade.instanciaNaoEncontrada()));

        validarErroDominio(
                given().accept(ContentType.JSON)
                        .get(BASE_PATH + "/{instanceId}", "desconhecida"),
                404,
                "ARVDOCP1002",
                "Análise de conformidade não localizada");
    }

    @Test
    void preserva409ParaTransicaoInvalida() {
        doThrow(FalhaAnaliseConformidade.transicaoInvalida())
                .when(revisar).executar(any(), any());

        validarErroDominio(
                given().contentType(ContentType.JSON).accept(ContentType.JSON)
                        .body(REVISAO_VALIDA)
                        .put(BASE_PATH + "/{instanceId}/revisao", "instancia-contrato"),
                409,
                "ARVDOCP1003",
                "Transição inválida para o estado atual da análise");
    }

    @Test
    void preserva422ParaRevisaoInconsistente() {
        doThrow(FalhaAnaliseConformidade.revisaoInconsistente(
                "A confiança do apontamento é somente leitura"))
                .when(revisar).executar(any(), any());

        validarErroDominio(
                given().contentType(ContentType.JSON).accept(ContentType.JSON)
                        .body(REVISAO_VALIDA)
                        .put(BASE_PATH + "/{instanceId}/revisao", "instancia-contrato"),
                422,
                "ARVDOCP1004",
                "A confiança do apontamento é somente leitura");
    }

    @Test
    void preserva503SanitizadoParaIndisponibilidadeTecnica() {
        when(iniciar.executar(any()))
                .thenReturn(Uni.createFrom().failure(
                        FalhaAnaliseConformidade.indisponibilidadeTecnica()));

        validarErroDominio(
                given().contentType(ContentType.JSON).accept(ContentType.JSON)
                        .body("""
                                {
                                  "identificadorDocumento": "DOC-2026-000123",
                                  "texto": "Texto para análise",
                                  "identificadorChecklist": 1000012583,
                                  "versaoChecklist": 1
                                }
                                """)
                        .post(BASE_PATH),
                503,
                "ARVDOCP1005",
                "Serviço de análise temporariamente indisponível");
    }

    @Test
    void documentaIdentidadesNoOpenApi() {
        JsonNode openApi = given()
                .accept(ContentType.JSON)
                .when()
                .get("/simtr-hub/openapi")
                .then()
                .statusCode(200)
                .extract()
                .as(JsonNode.class);

        JsonNode schemas = openApi.path("components").path("schemas");
        JsonNode request = schemas.path("IniciarAnaliseConformidadeRequest");
        JsonNode inicio = schemas.path("IniciarAnaliseConformidadeResponse");
        JsonNode visao = schemas.path("VisaoAnaliseConformidadeResponse");
        JsonNode revisao = schemas.path("RevisaoAnaliseConformidadeRequest");

        assertTrue(request.path("required").toString().contains("identificadorDocumento"));
        assertTrue(request.path("properties").has("identificadorDocumento"));
        assertPossuiCincoIdentidades(inicio);
        assertPossuiCincoIdentidades(visao);
        assertFalse(revisao.path("properties").has("correlationId"));
        assertFalse(revisao.path("properties").has("instanceId"));
        assertFalse(revisao.path("properties").has("identificadorDocumento"));
    }

    private static void validarErro400(Response response, String... mensagens) {
        JsonNode erro = response.then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract()
                .as(JsonNode.class);
        assertErroValidacaoExato(erro, mensagens);
    }

    private static void validarErroDominio(
            Response response,
            int status,
            String codigo,
            String mensagem) {
        JsonNode erro = response.then()
                .statusCode(status)
                .contentType(ContentType.JSON)
                .extract()
                .as(JsonNode.class);

        assertEquals(status, erro.path("codigo_http").asInt());
        assertEquals("simtr-hub", erro.path("recurso").asText());
        assertDoesNotExposeDetalhesInternos(erro);
        assertEquals(codigo, erro.path("codigo_erro").asText());
        assertEquals(mensagem, erro.path("erros").get(0).path("mensagem").asText());
        assertEquals(5, erro.size());
    }

    private static void assertDoesNotExposeDetalhesInternos(JsonNode erro) {
        assertTrue(uuidValido(erro.path("id_erro").asText()));
        assertFalse(erro.has("detalhe"));
        assertFalse(erro.has("stacktrace"));
    }

    private static boolean uuidValido(String valor) {
        try {
            UUID.fromString(valor);
            return true;
        } catch (IllegalArgumentException _) {
            return false;
        }
    }

    private static void assertPossuiCincoIdentidades(JsonNode schema) {
        JsonNode propriedades = schema.path("properties");
        assertTrue(propriedades.has("correlationId"));
        assertTrue(propriedades.has("instanceId"));
        assertTrue(propriedades.has("identificadorDocumento"));
        assertTrue(propriedades.has("identificadorChecklist"));
        assertTrue(propriedades.has("versaoChecklist"));
    }

    private static VisaoAnaliseConformidade visaoEmProcessamento() {
        return VisaoAnaliseConformidade.emProcessamento(
                CORRELATION_ID,
                "instancia-contrato",
                IDENTIFICADOR_DOCUMENTO,
                1000012583L,
                1);
    }
}
