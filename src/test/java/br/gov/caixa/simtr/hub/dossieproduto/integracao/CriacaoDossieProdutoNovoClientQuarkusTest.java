package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client.CriacaoDossieProdutoMtrClient;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.criacao.CriacaoDossieProdutoMtrRequest;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.criacao.CriacaoDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.CriacaoDossieProdutoMtrException;
import br.gov.caixa.simtr.hub.dossieproduto.aplicacao.porta.saida.SolicitarCriacaoDossieProduto;
import br.gov.caixa.simtr.hub.dossieproduto.dominio.modelo.ComandoCriacaoDossieProduto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(value = DossieProdutoMtrStubTestResource.class, restrictToAnnotatedClass = true)
class CriacaoDossieProdutoNovoClientQuarkusTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String REQUEST_JSON = """
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

    @Inject
    @RestClient
    CriacaoDossieProdutoMtrClient client;

    @Inject
    SolicitarCriacaoDossieProduto portaSaida;

    @BeforeEach
    void resetStub() {
        DossieProdutoMtrStubTestResource.reset();
    }

    @Test
    void enviaWireHeadersOidcERetornaRespostaComDTOsExclusivos() throws Exception {
        DossieProdutoMtrStubTestResource.responder(201, "{\"id\":4321}");

        CriacaoDossieProdutoMtrResponse resposta = client.criar(request()).await().indefinitely();

        assertEquals(4321L, resposta.id());
        List<DossieProdutoMtrStubTestResource.CapturedRequest> requisicoes =
                DossieProdutoMtrStubTestResource.requisicoes();
        assertEquals(1, requisicoes.size());
        DossieProdutoMtrStubTestResource.CapturedRequest requisicao = requisicoes.getFirst();
        assertEquals("POST", requisicao.method());
        assertEquals(DossieProdutoMtrStubTestResource.CAMINHO_CRIACAO, requisicao.path());
        assertEquals(OBJECT_MAPPER.readTree(REQUEST_JSON), OBJECT_MAPPER.readTree(requisicao.body()));
        assertTrue(requisicao.contentType().startsWith("application/json"));
        assertTrue(requisicao.accept().contains("application/json"));
        assertEquals("test-apikey", requisicao.apikey());
        assertEquals("Bearer stub-access-token", requisicao.authorization());
        assertNotNull(requisicao.traceparent());
    }

    @Test
    void classificaErroDeNegocioSemRetryEPreservaPayload() {
        DossieProdutoMtrStubTestResource.responder(400, """
                {"codigo_http":400,"recurso":"simtr-dossie-produto",
                 "id_erro":"criacao-400","codigo_erro":"MTR-CRIACAO-400",
                 "erros":[{"mensagem":"criacao nao permitida"}],"detalhe":"negocio"}
                """);

        CriacaoDossieProdutoMtrException.Negocio falha = assertThrows(
                CriacaoDossieProdutoMtrException.Negocio.class,
                () -> client.criar(request()).await().indefinitely());

        assertEquals(400, falha.status());
        assertEquals("criacao-400", falha.erro().idErro());
        assertEquals("criacao nao permitida", falha.erro().erros().getFirst().mensagem());
        assertEquals("negocio", falha.erro().detalhe());
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void executaRetryAposErroRecuperavel() {
        DossieProdutoMtrStubTestResource.responder(500,
                "{\"codigo_http\":500,\"recurso\":\"simtr-dossie-produto\"}");
        DossieProdutoMtrStubTestResource.responder(201, "{\"id\":4322}");

        CriacaoDossieProdutoMtrResponse resposta = client.criar(request()).await().indefinitely();

        assertEquals(4322L, resposta.id());
        assertEquals(2, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void selecionaAdapterMtrQuandoSimuladorEstaDesabilitado() {
        DossieProdutoMtrStubTestResource.responder(201, "{\"id\":4323}");

        var resultado = portaSaida.criar(new ComandoCriacaoDossieProduto(
                100L, 200L, 300L, List.of())).await().indefinitely();

        assertEquals(4323L, resultado.identificadorDossieProduto());
        assertEquals(1, DossieProdutoMtrStubTestResource.requisicoes().size());
    }

    @Test
    void preservaMatrizDeFaultToleranceDaCriacao() throws NoSuchMethodException {
        Method metodo = CriacaoDossieProdutoMtrClient.class.getMethod(
                "criar", CriacaoDossieProdutoMtrRequest.class);

        Timeout timeout = metodo.getAnnotation(Timeout.class);
        assertEquals(2_000, timeout.value());
        assertEquals(ChronoUnit.MILLIS, timeout.unit());

        Retry retry = metodo.getAnnotation(Retry.class);
        assertEquals(3, retry.maxRetries());
        assertEquals(300, retry.delay());
        assertEquals(ChronoUnit.MILLIS, retry.delayUnit());
        assertEquals(100, retry.jitter());
        assertEquals(ChronoUnit.MILLIS, retry.jitterDelayUnit());
        assertArrayEquals(new Class<?>[]{
                CriacaoDossieProdutoMtrException.Servidor.class,
                ProcessingException.class,
                TimeoutException.class
        }, retry.retryOn());
        assertArrayEquals(new Class<?>[]{
                CriacaoDossieProdutoMtrException.Negocio.class,
                CriacaoDossieProdutoMtrException.TecnicaCliente.class
        }, retry.abortOn());

        CircuitBreaker circuitBreaker = metodo.getAnnotation(CircuitBreaker.class);
        assertEquals(10, circuitBreaker.requestVolumeThreshold());
        assertEquals(0.5, circuitBreaker.failureRatio());
        assertEquals(10_000, circuitBreaker.delay());
        assertEquals(ChronoUnit.MILLIS, circuitBreaker.delayUnit());
        assertEquals(2, circuitBreaker.successThreshold());
        assertArrayEquals(retry.retryOn(), circuitBreaker.failOn());
        assertArrayEquals(retry.abortOn(), circuitBreaker.skipOn());
    }

    private static CriacaoDossieProdutoMtrRequest request() {
        return new CriacaoDossieProdutoMtrRequest(
                100L,
                200L,
                300L,
                List.of(new CriacaoDossieProdutoMtrRequest.Cliente(
                        "12345678901",
                        "12345678000190",
                        1L,
                        new CriacaoDossieProdutoMtrRequest.ClienteRelacionado(
                                "98765432100", null),
                        1)));
    }
}
