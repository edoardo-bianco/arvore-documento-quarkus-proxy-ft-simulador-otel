package br.gov.caixa.simtr.hub.contrato;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static br.gov.caixa.simtr.hub.contrato.JsonContractAssertions.assertJsonExato;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class DossieProdutoApiContractTest {

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
                .patch("/simtr-hub/v1/dossie-produto/{id}/validacao-negocial", 123L)
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
}
