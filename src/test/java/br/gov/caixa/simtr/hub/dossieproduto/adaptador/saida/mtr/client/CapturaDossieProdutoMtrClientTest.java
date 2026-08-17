package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.RestClientObservabilityFilter;
import br.gov.caixa.simtr.hub.arquitetura.seguranca.RequestHeaderFactory;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.captura.CapturaDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.CapturaDossieProdutoMtrException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CapturaDossieProdutoMtrClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SERVICO_MTR = "simtr-dossie-produto";
    private static final String CODIGO_FALLBACK = "ARVDOCP0002";
    private static final String MENSAGEM_FALLBACK =
            "Erro retornado pelo serviço MTR fora do contrato esperado.";
    private static final String MENSAGEM_SEM_ERRO = "Erro retornado pelo servico MTR";

    @Test
    void declaraPostWireSemCorpoComCredenciaisESemProviderQueRegistraPayload() throws Exception {
        var tipo = CapturaDossieProdutoMtrClient.class;
        var metodo = tipo.getMethod("capturar", Long.class);
        var tracingProvider = CapturaDossieProdutoMtrTracingProvider.class;

        assertEquals("dossie-produto", tipo.getAnnotation(RegisterRestClient.class).configKey());
        assertEquals("/dossie-produto", tipo.getAnnotation(Path.class).value());
        assertEquals(RequestHeaderFactory.class,
                tipo.getAnnotation(RegisterClientHeaders.class).value());
        Set<Class<?>> providers = Arrays.stream(tipo.getAnnotationsByType(RegisterProvider.class))
                .map(RegisterProvider::value)
                .collect(Collectors.toSet());
        assertEquals(Set.of(OidcClientRequestReactiveFilter.class, tracingProvider), providers);
        assertFalse(providers.contains(RestClientObservabilityFilter.class));
        assertTrue(ClientRequestFilter.class.isAssignableFrom(tracingProvider));
        assertTrue(ContextResolver.class.isAssignableFrom(tracingProvider));
        assertNull(tracingProvider.getAnnotation(Provider.class));
        assertNull(RestClientObservabilityFilter.class.getAnnotation(Provider.class));
        assertArrayEquals(new String[]{MediaType.APPLICATION_JSON},
                tipo.getAnnotation(Produces.class).value());
        assertNull(tipo.getAnnotation(Consumes.class));

        assertNotNull(metodo.getAnnotation(POST.class));
        assertEquals("/v1/dossie-produto/{id}/capturar",
                metodo.getAnnotation(Path.class).value());
        assertNull(metodo.getAnnotation(Consumes.class));
        assertEquals(1, metodo.getParameterCount());
        assertEquals("id", metodo.getParameters()[0].getAnnotation(PathParam.class).value());
        assertEquals(Uni.class, metodo.getReturnType());
        var retorno = assertInstanceOf(ParameterizedType.class, metodo.getGenericReturnType());
        assertEquals(CapturaDossieProdutoMtrResponse.class,
                retorno.getActualTypeArguments()[0]);
    }

    @Test
    void desserializaEClassificaErroDeNegocioSemPerderPayload() throws Exception {
        var erro = OBJECT_MAPPER.readValue("""
                {
                  "codigo_http": 409,
                  "recurso": "simtr-dossie-produto",
                  "id_erro": "captura-409",
                  "codigo_erro": "MTR-CAPTURA-409",
                  "erros": [{"mensagem": "dossie em estado incompatível"}],
                  "detalhe": "negocio",
                  "stacktrace": "stacktrace externo"
                }
                """, CapturaDossieProdutoMtrException.Erro.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(409);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(CapturaDossieProdutoMtrException.Erro.class)).thenReturn(erro);

        var falha = assertInstanceOf(
                CapturaDossieProdutoMtrException.Negocio.class,
                CapturaDossieProdutoMtrClient.toException(response));

        assertEquals(409, falha.status());
        assertEquals(erro, falha.erro());
        assertEquals(MENSAGEM_SEM_ERRO, falha.getMessage());
        assertEquals("captura-409", falha.erro().idErro());
        assertEquals("MTR-CAPTURA-409", falha.erro().codigoErro());
        assertEquals("stacktrace externo", falha.erro().stacktrace());
    }

    @Test
    void classificaTodosOsGruposDeStatusAprovados() {
        assertNull(CapturaDossieProdutoMtrClient.toException(responseSemPayload(200)));
        assertNull(CapturaDossieProdutoMtrClient.toException(responseSemPayload(399)));

        for (int status : new int[]{400, 404, 409, 422}) {
            assertInstanceOf(
                    CapturaDossieProdutoMtrException.Negocio.class,
                    CapturaDossieProdutoMtrClient.toException(responseSemPayload(status)),
                    "status " + status);
        }
        for (int status : new int[]{401, 403, 405, 429, 499}) {
            assertInstanceOf(
                    CapturaDossieProdutoMtrException.TecnicaCliente.class,
                    CapturaDossieProdutoMtrClient.toException(responseSemPayload(status)),
                    "status " + status);
        }
        for (int status : new int[]{500, 502, 503}) {
            assertInstanceOf(
                    CapturaDossieProdutoMtrException.Servidor.class,
                    CapturaDossieProdutoMtrClient.toException(responseSemPayload(status)),
                    "status " + status);
        }
    }

    @Test
    void normalizaSomenteCamposObrigatoriosAusentesDoPayloadDeErro() {
        var erroIncompleto = new CapturaDossieProdutoMtrException.Erro(
                null, null, null, null, null, null, null);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(404);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(CapturaDossieProdutoMtrException.Erro.class))
                .thenReturn(erroIncompleto);

        var falha = assertInstanceOf(
                CapturaDossieProdutoMtrException.Negocio.class,
                CapturaDossieProdutoMtrClient.toException(response));

        assertEquals(404, falha.erro().codigoHttp());
        assertEquals(SERVICO_MTR, falha.erro().recurso());
        assertFalse(falha.erro().idErro().isBlank());
        assertNull(falha.erro().codigoErro());
        assertNull(falha.erro().erros());
        assertNull(falha.erro().detalhe());
        assertNull(falha.erro().stacktrace());
    }

    @Test
    void usaFallbackSeguroQuandoCorpoDeErroEstaAusente() {
        var falha = assertInstanceOf(
                CapturaDossieProdutoMtrException.Servidor.class,
                CapturaDossieProdutoMtrClient.toException(responseSemPayload(500)));

        assertFallbackSeguro(falha, 500);
    }

    @Test
    void usaFallbackSeguroQuandoCorpoDeErroEstaMalformado() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(CapturaDossieProdutoMtrException.Erro.class))
                .thenThrow(new ProcessingException("json invalido"));

        var falha = assertInstanceOf(
                CapturaDossieProdutoMtrException.Servidor.class,
                CapturaDossieProdutoMtrClient.toException(response));

        assertFallbackSeguro(falha, 500);
    }

    @Test
    void usaMensagemSeguraQuandoErroNaoPossuiMensagem() {
        var falhaSemErro = new CapturaDossieProdutoMtrException.Servidor(500, null);
        var falhaSemMensagens = new CapturaDossieProdutoMtrException.Servidor(
                500,
                new CapturaDossieProdutoMtrException.Erro(
                        500, SERVICO_MTR, "captura-500", null, List.of(), null, null));

        assertEquals(MENSAGEM_SEM_ERRO, falhaSemErro.getMessage());
        assertEquals(MENSAGEM_SEM_ERRO, falhaSemMensagens.getMessage());
    }

    @Test
    void preservaTimeoutECircuitBreakerSemRetry() throws Exception {
        var metodo = CapturaDossieProdutoMtrClient.class.getMethod("capturar", Long.class);

        Timeout timeout = metodo.getAnnotation(Timeout.class);
        assertNotNull(timeout);
        assertEquals(2_000, timeout.value());
        assertEquals(ChronoUnit.MILLIS, timeout.unit());
        assertNull(metodo.getAnnotation(Retry.class));

        CircuitBreaker circuitBreaker = metodo.getAnnotation(CircuitBreaker.class);
        assertNotNull(circuitBreaker);
        assertEquals(10, circuitBreaker.requestVolumeThreshold());
        assertEquals(0.5, circuitBreaker.failureRatio());
        assertEquals(10_000, circuitBreaker.delay());
        assertEquals(ChronoUnit.MILLIS, circuitBreaker.delayUnit());
        assertEquals(2, circuitBreaker.successThreshold());
        assertArrayEquals(new Class<?>[]{
                CapturaDossieProdutoMtrException.Servidor.class,
                ProcessingException.class,
                TimeoutException.class
        }, circuitBreaker.failOn());
        assertArrayEquals(new Class<?>[]{
                CapturaDossieProdutoMtrException.Negocio.class,
                CapturaDossieProdutoMtrException.TecnicaCliente.class
        }, circuitBreaker.skipOn());
    }

    private static Response responseSemPayload(int status) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);
        when(response.hasEntity()).thenReturn(false);
        return response;
    }

    private static void assertFallbackSeguro(
            CapturaDossieProdutoMtrException falha,
            int status
    ) {
        assertEquals(status, falha.erro().codigoHttp());
        assertEquals(SERVICO_MTR, falha.erro().recurso());
        assertFalse(falha.erro().idErro().isBlank());
        assertEquals(CODIGO_FALLBACK, falha.erro().codigoErro());
        assertEquals(MENSAGEM_FALLBACK, falha.erro().erros().getFirst().mensagem());
        assertNull(falha.erro().detalhe());
        assertNull(falha.erro().stacktrace());
    }
}
