package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.FormularioMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.FormularioSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.FormularioDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.formulario.FormularioDossieProdutoMtrRequest;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.formulario.FormularioDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.FormularioDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoAtualizacaoFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.FormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.RespostaFormularioDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.VinculoFormularioDossieProduto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(
        value = DossieProdutoMtrStubTestResource.class,
        restrictToAnnotatedClass = true
)
class FormularioDossieProdutoMtrContractTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String MEDIA_TYPE_JSON = "application/json";
    private static final String CAMINHO_FORMULARIO =
            "/simtr/dossie-produto/v1/dossie-produto/123/formulario";
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
    private static final String REQUEST_FORMULARIO_COM_NULOS = """
            [
              null,
              {},
              {"vinculo_dossie": null},
              {"vinculo_dossie": {
                "fase": null,
                "cliente": null,
                "produto": null,
                "garantia": null,
                "respostas_formulario": [null, {
                  "campo_formulario": null,
                  "resposta": null,
                  "opcoes_selecionadas": null,
                  "excluir": null
                }]
              }}
            ]
            """;
    private static final String WIRE_FORMULARIO_COM_NULOS = """
            [
              null,
              {},
              {},
              {"vinculo_dossie": {"respostas_formulario": [null, {}]}}
            ]
            """;

    @Inject
    @RestClient
    FormularioDossieProdutoMtrClient novoClient;

    @Inject
    @FormularioMtr
    SolicitarAtualizacaoFormularioDossieProduto portaMtr;

    @Inject
    @FormularioSimulador
    SolicitarAtualizacaoFormularioDossieProduto portaSimulador;

    @Inject
    SolicitarAtualizacaoFormularioDossieProduto portaSelecionada;

    @BeforeEach
    void resetStub() {
        DossieProdutoMtrStubTestResource.reset();
    }

    @Test
    void selecionaMtrQuandoSimuladorEstaDesligadoEMantemSimuladorQualificado() {
        DossieProdutoMtrStubTestResource.responder(201, "{\"id\":654}");

        var resultadoFixture = portaSimulador.atualizar(
                new ComandoAtualizacaoFormularioDossieProduto(null, List.of()))
                .await().indefinitely();
        var resultadoSelecionado = portaSelecionada.atualizar(
                new ComandoAtualizacaoFormularioDossieProduto(321L, List.of()))
                .await().indefinitely();

        assertEquals(1L, resultadoFixture.identificadorDossieProduto());
        assertEquals(654L, resultadoSelecionado.identificadorDossieProduto());
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void preservaWireHeadersERespostaDoFormularioComSimuladorDesabilitado()
            throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(201, "{\"id\":987}");

        JsonNode response = patchFormulario(REQUEST_FORMULARIO).then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree("{\"id\":987}"), response);

        List<DossieProdutoMtrStubTestResource.CapturedRequest> requests =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(1, requests.size());

        DossieProdutoMtrStubTestResource.CapturedRequest request = requests.getFirst();
        assertEquals("PATCH", request.method());
        assertEquals(CAMINHO_FORMULARIO, request.path());
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_FORMULARIO), OBJECT_MAPPER.readTree(request.body()));
        assertTrue(request.contentType().startsWith(MEDIA_TYPE_JSON));
        assertTrue(request.accept().contains(MEDIA_TYPE_JSON));
        assertEquals("test-apikey", request.apikey());
        assertEquals("Bearer stub-access-token", request.authorization());
        assertNotNull(request.traceparent());
        assertTrue(request.traceparent().matches("00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}"));
    }

    @Test
    void preservaListasElementosECamposNulosNoWireDoFormulario() throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(201, "{\"id\":123}");

        patchFormulario(REQUEST_FORMULARIO_COM_NULOS).then()
                .statusCode(201)
                .contentType(ContentType.JSON);

        String body = DossieProdutoMtrStubTestResource.requisicoes().getFirst().body();
        assertEquals(OBJECT_MAPPER.readTree(WIRE_FORMULARIO_COM_NULOS), OBJECT_MAPPER.readTree(body));
    }

    @Test
    void preservaErroDeNegocioCompletoDoFormularioSemRetry() throws JsonProcessingException {
        String erroMtr = """
                {
                  "codigo_http": 400,
                  "recurso": "simtr-dossie-produto",
                  "id_erro": "stub-formulario-400",
                  "codigo_erro": "MTR-DOS-FORMULARIO-400",
                  "erros": [{"mensagem": "formulario nao permitido"}],
                  "detalhe": "falha de negocio controlada"
                }
                """;
        DossieProdutoMtrStubTestResource.responder(400, erroMtr);

        JsonNode response = patchFormulario(REQUEST_FORMULARIO).then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree(erroMtr), response);
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void retryDoFormularioRepeteOMesmoWireAposErroRecuperavel() throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(500, """
                {"codigo_http":500,"recurso":"simtr-dossie-produto","codigo_erro":"MTR-FORMULARIO-500"}
                """);
        DossieProdutoMtrStubTestResource.responder(201, "{\"id\":988}");

        JsonNode response = patchFormulario(REQUEST_FORMULARIO).then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree("{\"id\":988}"), response);
        List<DossieProdutoMtrStubTestResource.CapturedRequest> requests =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(2, requests.size());
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_FORMULARIO), OBJECT_MAPPER.readTree(requests.get(0).body()));
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_FORMULARIO), OBJECT_MAPPER.readTree(requests.get(1).body()));
    }

    @Test
    void congelaMatrizDeFaultToleranceDeclaradaNoFormulario() throws NoSuchMethodException {
        Method method = FormularioDossieProdutoMtrClient.class.getMethod(
                "atualizar",
                Long.class,
                List.class
        );

        Timeout timeout = method.getAnnotation(Timeout.class);
        assertEquals(2_000, timeout.value());
        assertEquals(ChronoUnit.MILLIS, timeout.unit());

        Retry retry = method.getAnnotation(Retry.class);
        assertEquals(3, retry.maxRetries());
        assertEquals(300, retry.delay());
        assertEquals(ChronoUnit.MILLIS, retry.delayUnit());
        assertEquals(100, retry.jitter());
        assertEquals(ChronoUnit.MILLIS, retry.jitterDelayUnit());
        assertArrayEquals(new Class<?>[]{
                FormularioDossieProdutoMtrException.Servidor.class,
                ProcessingException.class,
                TimeoutException.class
        }, retry.retryOn());
        assertArrayEquals(new Class<?>[]{
                FormularioDossieProdutoMtrException.Negocio.class,
                FormularioDossieProdutoMtrException.TecnicaCliente.class
        }, retry.abortOn());

        CircuitBreaker circuitBreaker = method.getAnnotation(CircuitBreaker.class);
        assertEquals(10, circuitBreaker.requestVolumeThreshold());
        assertEquals(0.5, circuitBreaker.failureRatio());
        assertEquals(10_000, circuitBreaker.delay());
        assertEquals(ChronoUnit.MILLIS, circuitBreaker.delayUnit());
        assertEquals(2, circuitBreaker.successThreshold());
        assertArrayEquals(retry.retryOn(), circuitBreaker.failOn());
        assertArrayEquals(retry.abortOn(), circuitBreaker.skipOn());
    }

    @Test
    void novoClientPreservaWireHeadersOidcERespostaComDtosExclusivos() throws Exception {
        DossieProdutoMtrStubTestResource.responder(201, "{\"id\":4321}");

        FormularioDossieProdutoMtrResponse resposta = novoClient.atualizar(
                123L, requestMtr()).await().indefinitely();

        assertEquals(4321L, resposta.id());
        List<DossieProdutoMtrStubTestResource.CapturedRequest> requests =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(1, requests.size());
        DossieProdutoMtrStubTestResource.CapturedRequest request = requests.getFirst();
        assertEquals("PATCH", request.method());
        assertEquals(CAMINHO_FORMULARIO, request.path());
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_FORMULARIO),
                OBJECT_MAPPER.readTree(request.body()));
        assertTrue(request.contentType().startsWith(MEDIA_TYPE_JSON));
        assertTrue(request.accept().contains(MEDIA_TYPE_JSON));
        assertEquals("test-apikey", request.apikey());
        assertEquals("Bearer stub-access-token", request.authorization());
        assertNotNull(request.traceparent());
    }

    @Test
    void novoAdapterPreservaListasElementosECamposNulosNoWire() throws Exception {
        DossieProdutoMtrStubTestResource.responder(201, "{\"id\":4322}");

        var resultado = portaMtr.atualizar(comandoComNulos()).await().indefinitely();

        assertEquals(4322L, resultado.identificadorDossieProduto());
        String body = DossieProdutoMtrStubTestResource.requisicoes().getFirst().body();
        assertEquals(OBJECT_MAPPER.readTree(WIRE_FORMULARIO_COM_NULOS),
                OBJECT_MAPPER.readTree(body));
    }

    @Test
    void novoClientClassificaErroDeNegocioSemRetryEPreservaPayload() {
        DossieProdutoMtrStubTestResource.responder(400, """
                {"codigo_http":400,"recurso":"simtr-dossie-produto",
                 "id_erro":"formulario-400","codigo_erro":"MTR-FORM-400",
                 "erros":[{"mensagem":"formulario nao permitido"}],"detalhe":"negocio"}
                """);

        FormularioDossieProdutoMtrException.Negocio falha = assertThrows(
                FormularioDossieProdutoMtrException.Negocio.class,
                () -> novoClient.atualizar(123L, requestMtr()).await().indefinitely());

        assertEquals(400, falha.status());
        assertEquals("formulario-400", falha.erro().idErro());
        assertEquals("formulario nao permitido", falha.erro().erros().getFirst().mensagem());
        assertEquals("negocio", falha.erro().detalhe());
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void novoClientRepeteMesmoWireAposErroRecuperavel() throws Exception {
        DossieProdutoMtrStubTestResource.responder(500,
                "{\"codigo_http\":500,\"recurso\":\"simtr-dossie-produto\"}");
        DossieProdutoMtrStubTestResource.responder(201, "{\"id\":4323}");

        FormularioDossieProdutoMtrResponse resposta = novoClient.atualizar(
                123L, requestMtr()).await().indefinitely();

        assertEquals(4323L, resposta.id());
        List<DossieProdutoMtrStubTestResource.CapturedRequest> requests =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(2, requests.size());
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_FORMULARIO),
                OBJECT_MAPPER.readTree(requests.get(0).body()));
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_FORMULARIO),
                OBJECT_MAPPER.readTree(requests.get(1).body()));
    }

    private static Response patchFormulario(String body) {
        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(body)
                .when()
                .patch("/simtr-hub/v1/dossie-produto/{id}/formulario", 123L);
    }

    private static List<FormularioDossieProdutoMtrRequest> requestMtr() {
        return List.of(new FormularioDossieProdutoMtrRequest(
                new FormularioDossieProdutoMtrRequest.VinculoDossie(
                        10L,
                        new FormularioDossieProdutoMtrRequest.Cliente(
                                "12345678901", null, 1L),
                        new FormularioDossieProdutoMtrRequest.Produto(100, 200),
                        new FormularioDossieProdutoMtrRequest.Garantia(
                                300, 400, 500,
                                List.of(new FormularioDossieProdutoMtrRequest.ClienteAvalista(
                                        null, "12345678000190"))),
                        List.of(new FormularioDossieProdutoMtrRequest.Resposta(
                                600L, "resposta", List.of("opcao-1"), true)))));
    }

    private static ComandoAtualizacaoFormularioDossieProduto comandoComNulos() {
        List<RespostaFormularioDossieProduto> respostas = new ArrayList<>();
        respostas.add(null);
        respostas.add(new RespostaFormularioDossieProduto(null, null, null, null));

        List<FormularioDossieProduto> formularios = new ArrayList<>();
        formularios.add(null);
        formularios.add(new FormularioDossieProduto(null));
        formularios.add(new FormularioDossieProduto(null));
        formularios.add(new FormularioDossieProduto(
                new VinculoFormularioDossieProduto(
                        null, null, null, null, respostas)));
        return new ComandoAtualizacaoFormularioDossieProduto(123L, formularios);
    }
}
