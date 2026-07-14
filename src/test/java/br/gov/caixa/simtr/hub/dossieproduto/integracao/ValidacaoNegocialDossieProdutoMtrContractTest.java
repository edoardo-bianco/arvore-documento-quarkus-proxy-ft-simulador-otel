package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ValidacaoNegocialMtr;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.configuracao.qualificador.ValidacaoNegocialSimulador;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.ValidacaoNegocialDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.validacaonegocial.ValidacaoNegocialDossieProdutoMtrRequest;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.ValidacaoNegocialDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarRegistroValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ClienteAvalistaValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoRegistroValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.GarantiaValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ParecerApontamentoValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.RespostaFormularioValidacaoNegocialDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.VerificacaoValidacaoNegocialDossieProduto;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(
        value = DossieProdutoMtrStubTestResource.class,
        restrictToAnnotatedClass = true
)
class ValidacaoNegocialDossieProdutoMtrContractTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CAMINHO_VALIDACAO_NEGOCIAL =
            "/simtr/dossie-produto/v1/dossie-produto/123/validacao-negocial";
    private static final String REQUEST_VALIDACAO_NEGOCIAL = """
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
    private static final String REQUEST_VALIDACAO_NEGOCIAL_COM_NULOS = """
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
            """;
    private static final String WIRE_VALIDACAO_NEGOCIAL_COM_NULOS = """
            {
              "verificacoes": [
                null,
                {
                  "identificador_checklist": 6592,
                  "versao_checklist": 2,
                  "analise_realizada": true,
                  "parecer_apontamentos": [null],
                  "garantia": {"clientes_avalistas": [null]}
                }
              ],
              "respostas_formulario": [
                null,
                {
                  "campo_formulario": 1000011689,
                  "opcoes_selecionadas": [null]
                }
              ]
            }
            """;

    @Inject
    @RestClient
    ValidacaoNegocialDossieProdutoMtrClient novoClient;

    @Inject
    @ValidacaoNegocialMtr
    SolicitarRegistroValidacaoNegocialDossieProduto portaMtr;

    @Inject
    @ValidacaoNegocialSimulador
    SolicitarRegistroValidacaoNegocialDossieProduto portaSimulador;

    @Inject
    SolicitarRegistroValidacaoNegocialDossieProduto portaSelecionada;

    @BeforeEach
    void resetStub() {
        DossieProdutoMtrStubTestResource.reset();
    }

    @Test
    void selecionaMtrQuandoSimuladorEstaDesligadoEMantemSimuladorQualificado() {
        DossieProdutoMtrStubTestResource.responder(200, "");
        var comando = new ComandoRegistroValidacaoNegocialDossieProduto(
                123L, null, null);

        assertNull(portaSimulador.registrar(comando).await().indefinitely());
        assertEquals(0, DossieProdutoMtrStubTestResource.requisicoes().size());

        assertNull(portaSelecionada.registrar(comando).await().indefinitely());
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void preservaWireHeadersOidcERespostaSemCorpoComSimuladorDesabilitado()
            throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(200, "");

        String response = patchValidacaoNegocial(REQUEST_VALIDACAO_NEGOCIAL).then()
                .statusCode(200)
                .extract().asString();

        assertEquals("", response);
        List<DossieProdutoMtrStubTestResource.CapturedRequest> requests =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(1, requests.size());

        DossieProdutoMtrStubTestResource.CapturedRequest request = requests.getFirst();
        assertEquals("PATCH", request.method());
        assertEquals(CAMINHO_VALIDACAO_NEGOCIAL, request.path());
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_VALIDACAO_NEGOCIAL),
                OBJECT_MAPPER.readTree(request.body()));
        assertTrue(request.contentType().startsWith("application/json"));
        assertTrue(request.accept().contains("application/json"));
        assertEquals("test-apikey", request.apikey());
        assertEquals("Bearer stub-access-token", request.authorization());
        assertNotNull(request.traceparent());
        assertTrue(request.traceparent().matches("00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}"));
    }

    @Test
    void preservaListasElementosObjetosECamposNulosNoWireDaValidacaoNegocial()
            throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(200, "");

        patchValidacaoNegocial(REQUEST_VALIDACAO_NEGOCIAL_COM_NULOS).then()
                .statusCode(200);

        String body = DossieProdutoMtrStubTestResource.requisicoes().getFirst().body();
        assertEquals(OBJECT_MAPPER.readTree(WIRE_VALIDACAO_NEGOCIAL_COM_NULOS),
                OBJECT_MAPPER.readTree(body));
    }

    @Test
    void preservaErroDeNegocioCompletoDaValidacaoNegocialSemRetry()
            throws JsonProcessingException {
        String erroMtr = """
                {
                  "codigo_http": 400,
                  "recurso": "simtr-dossie-produto",
                  "id_erro": "stub-validacao-400",
                  "codigo_erro": "MTR-DOS-VALIDACAO-400",
                  "erros": [{"mensagem": "validacao negocial nao permitida"}],
                  "detalhe": "falha de negocio controlada"
                }
                """;
        DossieProdutoMtrStubTestResource.responder(400, erroMtr);

        JsonNode response = patchValidacaoNegocial(REQUEST_VALIDACAO_NEGOCIAL).then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract().as(JsonNode.class);

        assertEquals(OBJECT_MAPPER.readTree(erroMtr), response);
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void retryDaValidacaoNegocialRepeteOMesmoWireAposErroRecuperavel()
            throws JsonProcessingException {
        DossieProdutoMtrStubTestResource.responder(500, """
                {"codigo_http":500,"recurso":"simtr-dossie-produto","codigo_erro":"MTR-VALIDACAO-500"}
                """);
        DossieProdutoMtrStubTestResource.responder(200, "");

        patchValidacaoNegocial(REQUEST_VALIDACAO_NEGOCIAL).then()
                .statusCode(200);

        List<DossieProdutoMtrStubTestResource.CapturedRequest> requests =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(2, requests.size());
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_VALIDACAO_NEGOCIAL),
                OBJECT_MAPPER.readTree(requests.get(0).body()));
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_VALIDACAO_NEGOCIAL),
                OBJECT_MAPPER.readTree(requests.get(1).body()));
    }

    @Test
    void congelaMatrizDeFaultToleranceDeclaradaNaValidacaoNegocial()
            throws NoSuchMethodException {
        Method method = ValidacaoNegocialDossieProdutoMtrClient.class.getMethod(
                "registrar",
                Long.class,
                ValidacaoNegocialDossieProdutoMtrRequest.class
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
                ValidacaoNegocialDossieProdutoMtrException.Servidor.class,
                ProcessingException.class,
                TimeoutException.class
        }, retry.retryOn());
        assertArrayEquals(new Class<?>[]{
                ValidacaoNegocialDossieProdutoMtrException.Negocio.class,
                ValidacaoNegocialDossieProdutoMtrException.TecnicaCliente.class
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
    void novoClientPreservaWireHeadersOidcTraceERespostaSemCorpo() throws Exception {
        DossieProdutoMtrStubTestResource.responder(200, "");

        Void resposta = novoClient.registrar(123L, requestMtr()).await().indefinitely();

        assertNull(resposta);
        List<DossieProdutoMtrStubTestResource.CapturedRequest> requests =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(1, requests.size());
        DossieProdutoMtrStubTestResource.CapturedRequest request = requests.getFirst();
        assertEquals("PATCH", request.method());
        assertEquals(CAMINHO_VALIDACAO_NEGOCIAL, request.path());
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_VALIDACAO_NEGOCIAL),
                OBJECT_MAPPER.readTree(request.body()));
        assertTrue(request.contentType().startsWith("application/json"));
        assertTrue(request.accept().contains("application/json"));
        assertEquals("test-apikey", request.apikey());
        assertEquals("Bearer stub-access-token", request.authorization());
        assertNotNull(request.traceparent());
    }

    @Test
    void novoAdapterPreservaListasElementosObjetosECamposNulosNoWire()
            throws Exception {
        DossieProdutoMtrStubTestResource.responder(200, "");

        Void resposta = portaMtr.registrar(comandoComNulos()).await().indefinitely();

        assertNull(resposta);
        String body = DossieProdutoMtrStubTestResource.requisicoes().getFirst().body();
        assertEquals(OBJECT_MAPPER.readTree(WIRE_VALIDACAO_NEGOCIAL_COM_NULOS),
                OBJECT_MAPPER.readTree(body));
    }

    @Test
    void novoClientClassificaErroDeNegocioSemRetryEPreservaPayload() {
        DossieProdutoMtrStubTestResource.responder(400, """
                {"codigo_http":400,"recurso":"simtr-dossie-produto",
                 "id_erro":"validacao-400","codigo_erro":"MTR-VALIDACAO-400",
                 "erros":[{"mensagem":"validacao negocial nao permitida"}],
                 "detalhe":"negocio"}
                """);

        ValidacaoNegocialDossieProdutoMtrException.Negocio falha = assertThrows(
                ValidacaoNegocialDossieProdutoMtrException.Negocio.class,
                () -> novoClient.registrar(123L, requestMtr()).await().indefinitely());

        assertEquals(400, falha.status());
        assertEquals("validacao-400", falha.erro().idErro());
        assertEquals("validacao negocial nao permitida",
                falha.erro().erros().getFirst().mensagem());
        assertEquals("negocio", falha.erro().detalhe());
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void novoClientRepeteMesmoWireAposErroRecuperavel() throws Exception {
        DossieProdutoMtrStubTestResource.responder(500,
                "{\"codigo_http\":500,\"recurso\":\"simtr-dossie-produto\"}");
        DossieProdutoMtrStubTestResource.responder(200, "");

        Void resposta = novoClient.registrar(123L, requestMtr()).await().indefinitely();

        assertNull(resposta);
        List<DossieProdutoMtrStubTestResource.CapturedRequest> requests =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(2, requests.size());
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_VALIDACAO_NEGOCIAL),
                OBJECT_MAPPER.readTree(requests.get(0).body()));
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_VALIDACAO_NEGOCIAL),
                OBJECT_MAPPER.readTree(requests.get(1).body()));
    }

    private static Response patchValidacaoNegocial(String body) {
        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(body)
                .when()
                .patch("/simtr-hub/v1/dossie-produto/{id}/validacao-negocial", 123L);
    }

    private static ValidacaoNegocialDossieProdutoMtrRequest requestMtr() {
        return new ValidacaoNegocialDossieProdutoMtrRequest(
                List.of(new ValidacaoNegocialDossieProdutoMtrRequest.Verificacao(
                        1122928L,
                        6592L,
                        2,
                        true,
                        List.of(
                                new ValidacaoNegocialDossieProdutoMtrRequest.ParecerApontamento(
                                        1000012877L, "APROVADO", "apontamento aprovado",
                                        false, 1.0),
                                new ValidacaoNegocialDossieProdutoMtrRequest.ParecerApontamento(
                                        1000011696L, "APROVADO", null, true, 0.7),
                                new ValidacaoNegocialDossieProdutoMtrRequest.ParecerApontamento(
                                        1000011695L, "INCONCLUSIVO", "necessita revisao",
                                        true, 0.3)),
                        new ValidacaoNegocialDossieProdutoMtrRequest.Garantia(
                                300,
                                List.of(new ValidacaoNegocialDossieProdutoMtrRequest
                                        .ClienteAvalista("12345678901", null))),
                        new ValidacaoNegocialDossieProdutoMtrRequest.Produto(100, 200),
                        true)),
                List.of(
                        new ValidacaoNegocialDossieProdutoMtrRequest.RespostaFormulario(
                                1000011689L, "teste", null),
                        new ValidacaoNegocialDossieProdutoMtrRequest.RespostaFormulario(
                                1000011699L, null, List.of("2"))));
    }

    private static ComandoRegistroValidacaoNegocialDossieProduto comandoComNulos() {
        List<VerificacaoValidacaoNegocialDossieProduto> verificacoes = new ArrayList<>();
        verificacoes.add(null);
        List<ParecerApontamentoValidacaoNegocialDossieProduto> pareceres =
                new ArrayList<>();
        pareceres.add(null);
        List<ClienteAvalistaValidacaoNegocialDossieProduto> avalistas =
                new ArrayList<>();
        avalistas.add(null);
        verificacoes.add(new VerificacaoValidacaoNegocialDossieProduto(
                null,
                6592L,
                2,
                true,
                pareceres,
                new GarantiaValidacaoNegocialDossieProduto(null, avalistas),
                null,
                null));

        List<RespostaFormularioValidacaoNegocialDossieProduto> respostas = new ArrayList<>();
        respostas.add(null);
        List<String> opcoes = new ArrayList<>();
        opcoes.add(null);
        respostas.add(new RespostaFormularioValidacaoNegocialDossieProduto(
                1000011689L, null, opcoes));
        return new ComandoRegistroValidacaoNegocialDossieProduto(
                123L, verificacoes, respostas);
    }
}
