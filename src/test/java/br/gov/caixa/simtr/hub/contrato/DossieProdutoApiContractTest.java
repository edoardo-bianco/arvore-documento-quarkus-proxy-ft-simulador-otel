package br.gov.caixa.simtr.hub.contrato;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1.DossieProdutoResource;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ConsultaDocumentosDossieProdutoSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.entrada.ConsultarDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.ObterDocumentosDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.CriteriosConsultaDocumentosDossieProduto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static br.gov.caixa.simtr.hub.contrato.JsonContractAssertions.assertJsonExato;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
    private static final String ROTA_DOCUMENTOS_DOSSIE_PRODUTO =
            "/simtr-hub/v1/dossie-produto/{id}/documentos";
    private static final String QUERY_DOCUMENTOS_COMPLETA =
            "?cnpj=00000000000000&cpf=00000000000&fase=5033"
                    + "&inclui-armazenamento=true&inclui-assinaturas=true"
                    + "&inclui-atributos=true&inclui-conformidade=true"
                    + "&inclui-outsourcing=true&inclui-propriedades=true"
                    + "&inclui-url=false&ip-usuario=192.0.2.10&tipologia=SIM-0001";

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

    private static final String PRIMEIRO_DOCUMENTO_CONSULTADO = """
            {
              "id_instancia_documento": 1132220,
              "id_documento": 1924727,
              "codigo_ged": "GED-SIMULADO-0001",
              "data_hora_captura": "08/07/2026 11:13:18",
              "data_hora_validade": null,
              "matricula_captura": "SIM0001",
              "tipo_documento": {
                "id": 3315,
                "nome": "Pesquisa Cadastral - Sipes",
                "codigo_tipologia": "0001000100069003",
                "ativo": false
              },
              "situacao_documento": "Criado",
              "vinculo_dossie": {
                "cliente": {
                  "cpf": "00000000000",
                  "nome": "CLIENTE SIMULADO 01",
                  "tipo_vinculo": "Vendedor PF",
                  "identificador_negocial_vinculo": 90000001,
                  "principal": false
                }
              },
              "atributos": [],
              "assinaturas_digitais": [],
              "conformidade": [],
              "propriedades": [],
              "outsourcing": [],
              "armazenamento": [{
                "id": 2098870,
                "data_hora_armazenamento": "08/07/2026 11:13:19",
                "tipo_armazenamento": "GED_RECEBIDO",
                "path_storage": null,
                "object_store_ged": "OS_SIMULADOR",
                "codigo_ged": "GED-SIMULADO-0001",
                "data_hora_previsao_exclusao": null,
                "data_hora_exclusao": null
              }]
            }
            """;

    @TestHTTPResource
    URI baseUri;

    @InjectMock
    ConsultarDocumentosDossieProduto consultarDocumentos;

    @Inject
    @ConsultaDocumentosDossieProdutoSimulador
    ObterDocumentosDossieProduto simuladorDocumentos;

    @BeforeEach
    void prepararPortaDaConsultaDeDocumentos() {
        when(consultarDocumentos.executar(
                any(CriteriosConsultaDocumentosDossieProduto.class)))
                .thenAnswer(invocacao -> {
                    CriteriosConsultaDocumentosDossieProduto criterios =
                            invocacao.getArgument(0);
                    if (criterios.identificador().valor() == 4_081_900L) {
                        return Uni.createFrom().item(List.of());
                    }
                    return simuladorDocumentos.obter(criterios);
                });
    }

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
    void preservaContratoDeConsultaDeDocumentosComTodosOsFiltros()
            throws IOException, InterruptedException {
        HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(baseUri.resolve(ROTA_DOCUMENTOS_DOSSIE_PRODUTO
                        .replace("{id}", "4081899") + QUERY_DOCUMENTOS_COMPLETA))
                .header("Accept", ContentType.JSON.toString())
                .GET()
                .build();

        assertTrue(requisicao.bodyPublisher().isEmpty());
        assertTrue(requisicao.headers().firstValue("Content-Type").isEmpty());

        HttpResponse<String> resposta = HTTP_CLIENT.send(
                requisicao, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resposta.statusCode());
        assertTrue(resposta.headers().firstValue("Content-Type")
                .orElseThrow().startsWith(ContentType.JSON.toString()));

        JsonNode documentos = JSON.readTree(resposta.body());
        assertTrue(documentos.isArray());
        assertEquals(14, documentos.size());
        assertJsonExato(PRIMEIRO_DOCUMENTO_CONSULTADO, documentos.get(0));
    }

    @Test
    void preservaContratoSemCorpoQuandoNaoExistemDocumentos()
            throws IOException, InterruptedException {
        HttpRequest requisicao = HttpRequest.newBuilder()
                .uri(baseUri.resolve(ROTA_DOCUMENTOS_DOSSIE_PRODUTO
                        .replace("{id}", "4081900")))
                .header("Accept", ContentType.JSON.toString())
                .GET()
                .build();

        HttpResponse<String> resposta = HTTP_CLIENT.send(
                requisicao, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resposta.statusCode());
        assertEquals("", resposta.body());
    }

    @Test
    void preservaAnnotationsDoContratoPublicoDeConsultaDeDocumentos() {
        Method metodo = Arrays.stream(DossieProdutoResource.class.getMethods())
                .filter(candidato -> candidato.getName()
                        .equals("consultarDocumentosDossieProduto"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "A capacidade consultarDocumentosDossieProduto deve existir"));

        assertNotNull(metodo.getAnnotation(GET.class));
        assertEquals("/{id}/documentos", metodo.getAnnotation(Path.class).value());
        assertEquals(MediaType.WILDCARD, metodo.getAnnotation(Consumes.class).value()[0]);

        var parametros = metodo.getParameters();
        assertEquals(2, parametros.length);
        assertEquals("id", parametros[0].getAnnotation(PathParam.class).value());
        assertNotNull(parametros[0].getAnnotation(NotNull.class));
        assertEquals(1L, parametros[0].getAnnotation(Min.class).value());
        assertNotNull(parametros[1].getAnnotation(BeanParam.class));
        assertEquals("ConsultaDocumentosDossieProdutoQueryParams",
                parametros[1].getType().getSimpleName());

        Set<String> queryParams = Arrays.stream(parametros[1].getType().getDeclaredFields())
                .map(campo -> campo.getAnnotation(QueryParam.class))
                .filter(Objects::nonNull)
                .map(QueryParam::value)
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                "cnpj", "cpf", "fase", "inclui-armazenamento", "inclui-assinaturas",
                "inclui-atributos", "inclui-conformidade", "inclui-outsourcing",
                "inclui-propriedades", "inclui-url", "ip-usuario", "tipologia"), queryParams);

        WithSpan span = metodo.getAnnotation(WithSpan.class);
        assertNotNull(span);
        assertEquals("simtr-hub.api.dossie-produto.documentos.consultar", span.value());
        assertEquals(SpanKind.SERVER, span.kind());

        APIResponse[] respostas = metodo.getAnnotationsByType(APIResponse.class);
        assertEquals(List.of("200", "204", "400", "401", "403", "404", "500"),
                Arrays.stream(respostas).map(APIResponse::responseCode).toList());
        assertEquals("ConsultaDocumentosDossieProdutoResponse",
                respostas[0].content()[0].schema().implementation().getSimpleName());
        assertEquals(SchemaType.ARRAY, respostas[0].content()[0].schema().type());
        assertEquals(0, respostas[1].content().length);
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
