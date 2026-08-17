package br.gov.caixa.simtr.hub.contrato;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static br.gov.caixa.simtr.hub.contrato.JsonContractAssertions.assertJsonExato;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class DossieProdutoApiContractTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private static final String ROTA_CONSULTA_DOSSIE_PRODUTO =
            "/simtr-hub/v1/dossie-produto/{id}";
    private static final String ROTA_CAPTURA_DOSSIE_PRODUTO =
            "/simtr-hub/v1/dossie-produto/{id}/capturar";
    private static final String ROTA_PRODUTO_DOSSIE_PRODUTO =
            "/simtr-hub/v1/dossie-produto/{id}/produto";
    private static final String ROTA_VALIDACAO_NEGOCIAL_DOSSIE_PRODUTO =
            "/simtr-hub/v1/dossie-produto/{id}/validacao-negocial";

    private static final String REQUEST_CRIACAO = """
            {
              "processo": 100,
              "chave_correlacao_canal": 200,
              "numero_negocio": 300,
              "clientes": [{
                "cpf": "12345678901",
                "cnpj": "12345678000190",
                "tipo_vinculo": 1,
                "cliente_relacionado": {"cpf": "98765432100"},
                "sequencia_titularidade": 1
              }]
            }
            """;

    private static final String REQUEST_FORMULARIO = """
            [{
              "vinculo_dossie": {
                "fase": 10,
                "cliente": {"cpf": "12345678901", "tipo_vinculo": 1},
                "produto": {"codigo_operacao": 100, "codigo_modalidade": 200},
                "garantia": {
                  "codigo_bacen": 300,
                  "produto_operacao": 400,
                  "produto_modalidade": 500,
                  "clientes_avalistas": [{"cnpj": "12345678000190"}]
                },
                "respostas_formulario": [{
                  "campo_formulario": 600,
                  "resposta": "resposta",
                  "opcoes_selecionadas": ["opcao-1"],
                  "excluir": true
                }]
              }
            }]
            """;

    private static final String REQUEST_DOCUMENTO = """
            {
              "path_storage": "container/documento.pdf",
              "codigo_ged": "GED123",
              "object_store_ged": "OBJECT_STORE",
              "tipo_documento": "RG",
              "vinculo_dossie": {
                "cliente": {"cpf": "12345678901", "tipo_vinculo": 1},
                "elemento_conteudo": 700,
                "garantia": {
                  "codigo_bacen": 300,
                  "produto_operacao": 400,
                  "produto_modalidade": 500,
                  "cliente_avalista": [{"cnpj": "12345678000190"}]
                }
              },
              "atributos": [{
                "chave": "numero",
                "valor": "12345",
                "objeto": "documento",
                "opcoes_selecionadas": ["opcao-1"]
              }],
              "propriedades": [{
                "chave": "origem",
                "valor": "pre-validacao",
                "objeto": "documento"
              }]
            }
            """;

    private static final String REQUEST_VALIDACAO = """
            {
              "verificacoes": [{
                "identificador_instancia_documento": 1122928,
                "identificador_checklist": 6592,
                "versao_checklist": 2,
                "analise_realizada": true,
                "parecer_apontamentos": [
                  {
                    "identificador_apontamento": 1000012877,
                    "resultado": "APROVADO",
                    "comentario": "apontamento aprovado",
                    "necessidade_reanalise": false,
                    "indice_ia": 1.0
                  },
                  {
                    "identificador_apontamento": 1000011696,
                    "resultado": "APROVADO",
                    "necessidade_reanalise": true,
                    "indice_ia": 0.7
                  },
                  {
                    "identificador_apontamento": 1000011695,
                    "resultado": "INCONCLUSIVO",
                    "comentario": "necessita revisao",
                    "necessidade_reanalise": true,
                    "indice_ia": 0.3
                  }
                ],
                "garantia": {
                  "codigo_bacen": 300,
                  "clientes_avalistas": [{"cpf": "12345678901"}]
                },
                "produto": {"codigo_operacao": 100, "codigo_modalidade": 200},
                "previo": true
              }],
              "respostas_formulario": [
                {"campo_formulario": 1000011689, "resposta": "teste"},
                {"campo_formulario": 1000011699, "opcoes_selecionadas": ["2"]}
              ]
            }
            """;

    private static final String REQUEST_PRODUTOS = """
            [
              {
                "codigo_operacao": 100,
                "codigo_modalidade": 200,
                "excluir": false
              },
              {
                "codigo_operacao": 300,
                "codigo_modalidade": 400,
                "excluir": true
              }
            ]
            """;

    private static final String RESPOSTA_CONSULTA = """
            {
              "id": 4324680,
              "chave_correlacao_canal": 1000012592,
              "instancia_jbpm": null,
              "numero_negocio": null,
              "canal_criacao": "SIMTRAPI",
              "unidade_criacao": 5402,
              "data_criacao": null,
              "clientes": [
                {
                  "cpf": "00000000000",
                  "cnpj": null,
                  "nome": "CLIENTE SIMULADO",
                  "razao_social": null,
                  "tipo_vinculo": "Proponente",
                  "identificador_negocial_vinculo": 40610702,
                  "principal": true
                }
              ],
              "processo": {
                "id": 5032,
                "nome": "Concessão Habitacional",
                "identificador_negocial": 1000016487,
                "macroprocesso": "HABITAÇÃO",
                "tratamento_seletivo": true,
                "complementacao_seletiva": true
              },
              "fase_atual": {
                "id": 5033,
                "nome": "Recepção de dados e documentos",
                "identificador_negocial": 1000016488,
                "data": "23/07/2026 10:24:00"
              },
              "situacao_atual": {
                "id": 1,
                "nome": "Rascunho",
                "data": "23/07/2026 10:24:00",
                "matricula": "SIMTRAPI"
              },
              "unidades_tratamento": [],
              "produtos_contratados": [
                {
                  "id": null,
                  "codigo_operacao": null,
                  "codigo_modalidade": null,
                  "nome": null
                }
              ]
            }
            """;

    @TestHTTPResource
    URI baseUri;

    @Test
    void preservaContratoDeConsultaPorIdentificador() {
        JsonNode resposta = given()
                .accept(ContentType.JSON)
                .when()
                .get(ROTA_CONSULTA_DOSSIE_PRODUTO, 4324680L)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertJsonExato(RESPOSTA_CONSULTA, resposta);
    }

    @Test
    void preservaContratoDeCriacao() {
        JsonNode resposta = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(REQUEST_CRIACAO)
                .when()
                .post("/simtr-hub/v1/dossie-produto")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertJsonExato("{\"id\":1}", resposta);
    }

    @Test
    void preservaContratoDeAtualizacaoDeFormulario() {
        JsonNode resposta = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(REQUEST_FORMULARIO)
                .when()
                .patch("/simtr-hub/v1/dossie-produto/{id}/formulario", 123L)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertJsonExato("{\"id\":123}", resposta);
    }

    @Test
    void preservaContratoDeAlteracaoDeProdutosSemCorpo() {
        String resposta = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(REQUEST_PRODUTOS)
                .when()
                .patch(ROTA_PRODUTO_DOSSIE_PRODUTO, 123L)
                .then()
                .statusCode(204)
                .extract().asString();

        assertEquals("", resposta);
    }

    @Test
    void preservaExclusaoOpcionalNaAlteracaoDeProdutos() {
        String resposta = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("[{\"codigo_operacao\":100,\"codigo_modalidade\":200}]")
                .when()
                .patch(ROTA_PRODUTO_DOSSIE_PRODUTO, 123L)
                .then()
                .statusCode(204)
                .extract().asString();

        assertEquals("", resposta);
    }

    @Test
    void preservaListaVaziaAceitaNaAlteracaoDeProdutos() {
        String resposta = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("[]")
                .when()
                .patch(ROTA_PRODUTO_DOSSIE_PRODUTO, 123L)
                .then()
                .statusCode(204)
                .extract().asString();

        assertEquals("", resposta);
    }

    @Test
    void preservaContratoDeInclusaoDeDocumento() {
        JsonNode resposta = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(REQUEST_DOCUMENTO)
                .when()
                .post("/simtr-hub/v1/dossie-produto/{id}/documento", 123L)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertJsonExato("{\"id_documento\":456,\"id_instancia_documento\":789}", resposta);
    }

    @Test
    void preservaContratoDeValidacaoNegocialSemCorpo() {
        String resposta = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(REQUEST_VALIDACAO)
                .when()
                .patch(ROTA_VALIDACAO_NEGOCIAL_DOSSIE_PRODUTO, 123L)
                .then()
                .statusCode(200)
                .extract().asString();

        assertEquals("", resposta);
    }

    @Test
    void preservaListasDeTopoNulasAceitasNaValidacaoNegocial() {
        String resposta = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "verificacoes": null,
                          "respostas_formulario": null
                        }
                        """)
                .when()
                .patch(ROTA_VALIDACAO_NEGOCIAL_DOSSIE_PRODUTO, 123L)
                .then()
                .statusCode(200)
                .extract().asString();

        assertEquals("", resposta);
    }

    @Test
    void preservaElementosObjetosECamposNulosAceitosNaValidacaoNegocial() {
        String resposta = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                          "verificacoes": [
                            null,
                            {
                              "identificador_instancia_documento": null,
                              "identificador_checklist": 6592,
                              "versao_checklist": 2,
                              "analise_realizada": true,
                              "parecer_apontamentos": [null],
                              "garantia": {
                                "codigo_bacen": null,
                                "clientes_avalistas": [null]
                              },
                              "produto": null,
                              "previo": null
                            }
                          ],
                          "respostas_formulario": [
                            null,
                            {
                              "campo_formulario": 1000011689,
                              "resposta": null,
                              "opcoes_selecionadas": [null]
                            }
                          ]
                        }
                        """)
                .when()
                .patch(ROTA_VALIDACAO_NEGOCIAL_DOSSIE_PRODUTO, 123L)
                .then()
                .statusCode(200)
                .extract().asString();

        assertEquals("", resposta);
    }

    @Test
    void preservaContratoDeAvancoDeWorkflow() {
        JsonNode resposta = given()
                .accept(ContentType.JSON)
                .when()
                .post("/simtr-hub/v1/dossie-produto/{id}/workflow", 123L)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertJsonExato("{\"id\":123}", resposta);
    }

    @Test
    void preservaContratoDeCapturaSemCorpoEContentType() throws IOException, InterruptedException {
        HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(baseUri.resolve(ROTA_CAPTURA_DOSSIE_PRODUTO.replace("{id}", "123")))
                .header("Accept", ContentType.JSON.toString())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        assertTrue(requisicao.headers().firstValue("Content-Type").isEmpty());

        HttpResponse<String> resposta = HTTP_CLIENT.send(
                requisicao, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resposta.statusCode());
        assertTrue(resposta.headers().firstValue("Content-Type")
                .orElseThrow().startsWith(ContentType.JSON.toString()));
        assertJsonExato("{\"id\":123}", JSON.readTree(resposta.body()));
    }
}
