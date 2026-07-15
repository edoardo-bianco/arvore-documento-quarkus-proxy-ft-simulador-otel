package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.DocumentoDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v2.documento.DocumentoDossieProdutoMtrRequest;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.DocumentoDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoInclusaoDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.GarantiaDocumentoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.VinculoDocumentoDossieProduto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(
        value = DossieProdutoMtrStubTestResource.class,
        restrictToAnnotatedClass = true
)
class DocumentoDossieProdutoMtrContractTest {

    @Inject
    @RestClient
    DocumentoDossieProdutoMtrClient novoClient;

    @Inject
    SolicitarInclusaoDocumentoDossieProduto portaSaida;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CAMINHO_DOCUMENTO =
            "/simtr/dossie-produto/v2/dossie-produto/123/documento";
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
    private static final String REQUEST_DOCUMENTO_COM_NULOS = """
            {
              "id": null,
              "path_storage": null,
              "codigo_ged": null,
              "object_store_ged": null,
              "tipo_documento": null,
              "vinculo_dossie": {
                "cliente": null,
                "elemento_conteudo": null,
                "garantia": {
                  "codigo_bacen": null,
                  "produto_operacao": null,
                  "produto_modalidade": null,
                  "cliente_avalista": [null]
                }
              },
              "atributos": [null],
              "propriedades": [null]
            }
            """;
    private static final String WIRE_DOCUMENTO_COM_NULOS = """
            {
              "vinculo_dossie": {
                "garantia": {"cliente_avalista": [null]}
              },
              "atributos": [null],
              "propriedades": [null]
            }
            """;

    @BeforeEach
    void resetStub() {
        DossieProdutoMtrStubTestResource.reset();
    }

    @Test
    void preservaWireV2HeadersERespostaDoDocumentoComSimuladorDesabilitado()
            throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(201,
                "{\"id_documento\":456,\"id_instancia_documento\":789}");

        JsonNode response = postDocumento(REQUEST_DOCUMENTO).then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree(
                "{\"id_documento\":456,\"id_instancia_documento\":789}"), response);

        List<DossieProdutoMtrStubTestResource.CapturedRequest> requests =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(1, requests.size());

        DossieProdutoMtrStubTestResource.CapturedRequest request = requests.getFirst();
        assertEquals("POST", request.method());
        assertEquals(CAMINHO_DOCUMENTO, request.path());
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_DOCUMENTO), OBJECT_MAPPER.readTree(request.body()));
        assertTrue(request.contentType().startsWith("application/json"));
        assertTrue(request.accept().contains("application/json"));
        assertEquals("test-apikey", request.apikey());
        assertEquals("Bearer stub-access-token", request.authorization());
        assertNotNull(request.traceparent());
        assertTrue(request.traceparent().matches("00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}"));
    }

    @Test
    void preservaListasElementosECamposNulosAceitosNoWireDoDocumento()
            throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(201,
                "{\"id_documento\":456,\"id_instancia_documento\":789}");

        postDocumento(REQUEST_DOCUMENTO_COM_NULOS).then()
                .statusCode(201)
                .contentType(ContentType.JSON);

        String body = DossieProdutoMtrStubTestResource.requisicoes().getFirst().body();
        assertEquals(OBJECT_MAPPER.readTree(WIRE_DOCUMENTO_COM_NULOS), OBJECT_MAPPER.readTree(body));
    }

    @Test
    void preservaErroDeNegocioCompletoDoDocumentoSemRetry() throws JsonProcessingException {
        String erroMtr = """
                {
                  "codigo_http": 400,
                  "recurso": "simtr-dossie-produto",
                  "id_erro": "stub-documento-400",
                  "codigo_erro": "MTR-DOS-DOCUMENTO-400",
                  "erros": [{"mensagem": "documento nao permitido"}],
                  "detalhe": "falha de negocio controlada"
                }
                """;
        DossieProdutoMtrStubTestResource.responder(400, erroMtr);

        JsonNode response = postDocumento(REQUEST_DOCUMENTO).then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree(erroMtr), response);
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void retryDoDocumentoRepeteOMesmoWireAposErroRecuperavel() throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(500, """
                {"codigo_http":500,"recurso":"simtr-dossie-produto","codigo_erro":"MTR-DOCUMENTO-500"}
                """);
        DossieProdutoMtrStubTestResource.responder(201,
                "{\"id_documento\":457,\"id_instancia_documento\":790}");

        JsonNode response = postDocumento(REQUEST_DOCUMENTO).then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree(
                "{\"id_documento\":457,\"id_instancia_documento\":790}"), response);
        List<DossieProdutoMtrStubTestResource.CapturedRequest> requests =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(2, requests.size());
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_DOCUMENTO), OBJECT_MAPPER.readTree(requests.get(0).body()));
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_DOCUMENTO), OBJECT_MAPPER.readTree(requests.get(1).body()));
    }

    @Test
    void novoClientPreservaWireV2HeadersERespostaDoDocumento()
            throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(201,
                "{\"id_documento\":456,\"id_instancia_documento\":789}");

        var resposta = novoClient.incluir(123L, requestMtr()).await().indefinitely();

        assertEquals(456L, resposta.idDocumento());
        assertEquals(789L, resposta.idInstanciaDocumento());
        List<DossieProdutoMtrStubTestResource.CapturedRequest> requests =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(1, requests.size());
        DossieProdutoMtrStubTestResource.CapturedRequest request = requests.getFirst();
        assertEquals("POST", request.method());
        assertEquals(CAMINHO_DOCUMENTO, request.path());
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_DOCUMENTO),
                OBJECT_MAPPER.readTree(request.body()));
        assertTrue(request.contentType().startsWith("application/json"));
        assertTrue(request.accept().contains("application/json"));
        assertEquals("test-apikey", request.apikey());
        assertEquals("Bearer stub-access-token", request.authorization());
        assertNotNull(request.traceparent());
    }

    @Test
    void selecionaMtrQuandoSimuladorDesabilitadoEPreservaNulosNoWire()
            throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(201,
                "{\"id_documento\":456,\"id_instancia_documento\":789}");

        var resposta = portaSaida.incluir(comandoComNulos()).await().indefinitely();

        assertEquals(456L, resposta.identificadorDocumento());
        assertEquals(789L, resposta.identificadorInstanciaDocumento());
        String body = DossieProdutoMtrStubTestResource.requisicoes().getFirst().body();
        assertEquals(OBJECT_MAPPER.readTree(WIRE_DOCUMENTO_COM_NULOS),
                OBJECT_MAPPER.readTree(body));
    }

    @Test
    void novoClientPreservaErroDeNegocioCompletoSemRetry() {
        String erroMtr = """
                {
                  "codigo_http": 400,
                  "recurso": "simtr-dossie-produto",
                  "id_erro": "stub-documento-400",
                  "codigo_erro": "MTR-DOS-DOCUMENTO-400",
                  "erros": [{"mensagem": "documento nao permitido"}],
                  "detalhe": "falha de negocio controlada",
                  "stacktrace": "stack-remota"
                }
                """;
        DossieProdutoMtrStubTestResource.responder(400, erroMtr);

        DocumentoDossieProdutoMtrException.Negocio falha = assertThrows(
                DocumentoDossieProdutoMtrException.Negocio.class,
                () -> novoClient.incluir(123L, requestMtr()).await().indefinitely());

        assertEquals(400, falha.status());
        assertEquals("simtr-dossie-produto", falha.erro().recurso());
        assertEquals("stub-documento-400", falha.erro().idErro());
        assertEquals("MTR-DOS-DOCUMENTO-400", falha.erro().codigoErro());
        assertEquals("documento nao permitido",
                falha.erro().erros().getFirst().mensagem());
        assertEquals("falha de negocio controlada", falha.erro().detalhe());
        assertEquals("stack-remota", falha.erro().stacktrace());
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void novoClientRepeteOMesmoWireAposErroRecuperavel()
            throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(500, """
                {"codigo_http":500,"recurso":"simtr-dossie-produto","codigo_erro":"MTR-DOCUMENTO-500"}
                """);
        DossieProdutoMtrStubTestResource.responder(201,
                "{\"id_documento\":457,\"id_instancia_documento\":790}");

        var resposta = novoClient.incluir(123L, requestMtr()).await().indefinitely();

        assertEquals(457L, resposta.idDocumento());
        assertEquals(790L, resposta.idInstanciaDocumento());
        List<DossieProdutoMtrStubTestResource.CapturedRequest> requests =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(2, requests.size());
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_DOCUMENTO),
                OBJECT_MAPPER.readTree(requests.get(0).body()));
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_DOCUMENTO),
                OBJECT_MAPPER.readTree(requests.get(1).body()));
    }

    private static DocumentoDossieProdutoMtrRequest requestMtr() {
        return new DocumentoDossieProdutoMtrRequest(
                null,
                "container/documento.pdf",
                "GED123",
                "OBJECT_STORE",
                "RG",
                new DocumentoDossieProdutoMtrRequest.VinculoDossie(
                        new DocumentoDossieProdutoMtrRequest.Cliente(
                                "12345678901", null, 1L),
                        700L,
                        new DocumentoDossieProdutoMtrRequest.Garantia(
                                300,
                                400,
                                500,
                                List.of(new DocumentoDossieProdutoMtrRequest.Cliente(
                                        null, "12345678000190", null)))),
                List.of(new DocumentoDossieProdutoMtrRequest.Atributo(
                        "numero", "12345", "documento", List.of("opcao-1"))),
                List.of(new DocumentoDossieProdutoMtrRequest.Propriedade(
                        "origem", "pre-validacao", "documento")));
    }

    private static ComandoInclusaoDocumentoDossieProduto comandoComNulos() {
        return new ComandoInclusaoDocumentoDossieProduto(
                123L,
                null,
                null,
                null,
                null,
                null,
                new VinculoDocumentoDossieProduto(
                        null,
                        null,
                        new GarantiaDocumentoDossieProduto(
                                null, null, null, Collections.singletonList(null))),
                Collections.singletonList(null),
                Collections.singletonList(null));
    }

    private static Response postDocumento(String body) {
        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(body)
                .when()
                .post("/simtr-hub/v1/dossie-produto/{id}/documento", 123L);
    }
}
