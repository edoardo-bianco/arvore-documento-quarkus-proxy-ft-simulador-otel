package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.RestClientObservabilityFilter;
import br.gov.caixa.simtr.hub.arquitetura.seguranca.RequestHeaderFactory;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.produto.ProdutoDossieProdutoMtrRequest;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.ProdutoDossieProdutoMtrException;
import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@QuarkusTest
class ProdutoDossieProdutoMtrClientTest {

    @Test
    void declaraContratoHttpEProvidersCompartilhados() throws NoSuchMethodException {
        var tipo = ProdutoDossieProdutoMtrClient.class;

        assertEquals("dossie-produto", tipo.getAnnotation(RegisterRestClient.class).configKey());
        assertEquals("/dossie-produto", tipo.getAnnotation(Path.class).value());
        assertArrayEquals(
                new String[]{MediaType.APPLICATION_JSON},
                tipo.getAnnotation(Consumes.class).value());
        assertArrayEquals(
                new String[]{MediaType.APPLICATION_JSON},
                tipo.getAnnotation(Produces.class).value());
        assertEquals(
                RequestHeaderFactory.class,
                tipo.getAnnotation(RegisterClientHeaders.class).value());

        var providers = Arrays.stream(tipo.getAnnotationsByType(RegisterProvider.class))
                .map(RegisterProvider::value)
                .toList();
        assertEquals(2, providers.size());
        assertTrue(providers.contains(OidcClientRequestReactiveFilter.class));
        assertTrue(providers.contains(RestClientObservabilityFilter.class));

        Method metodo = tipo.getMethod("alterar", Long.class, List.class);
        assertNotNull(metodo.getAnnotation(PATCH.class));
        assertEquals(
                "/v1/dossie-produto/{id}/produto",
                metodo.getAnnotation(Path.class).value());
        assertEquals(
                "id",
                metodo.getParameters()[0].getAnnotation(PathParam.class).value());
        assertEquals(
                "io.smallrye.mutiny.Uni<java.lang.Void>",
                metodo.getGenericReturnType().getTypeName());
        assertEquals(
                "java.util.List<"
                        + ProdutoDossieProdutoMtrRequest.class.getName()
                        + ">",
                metodo.getGenericParameterTypes()[1].getTypeName());
    }

    @Test
    void classificaStatusDeNegocio() {
        for (int status : new int[]{400, 404, 409, 422}) {
            assertInstanceOf(
                    ProdutoDossieProdutoMtrException.Negocio.class,
                    ProdutoDossieProdutoMtrClient.toException(responseSemCorpo(status)));
        }
    }

    @Test
    void classificaDemaisStatusDeErroENaoConverteSucesso() {
        assertInstanceOf(
                ProdutoDossieProdutoMtrException.TecnicaCliente.class,
                ProdutoDossieProdutoMtrClient.toException(responseSemCorpo(403)));
        assertInstanceOf(
                ProdutoDossieProdutoMtrException.Servidor.class,
                ProdutoDossieProdutoMtrClient.toException(responseSemCorpo(500)));
        assertNull(ProdutoDossieProdutoMtrClient.toException(responseSemCorpo(200)));
    }

    @Test
    void preservaCorpoDeErroMtrValido() {
        var erro = new ProdutoDossieProdutoMtrException.Erro(
                409,
                "simtr-dossie-produto",
                "produto-409",
                "MTR-PRODUTO-409",
                List.of(new ProdutoDossieProdutoMtrException.Mensagem(
                        "alteracao de produto em conflito")),
                "negocio",
                "stack-remota");
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(409);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(ProdutoDossieProdutoMtrException.Erro.class)).thenReturn(erro);

        var falha = assertInstanceOf(
                ProdutoDossieProdutoMtrException.Negocio.class,
                ProdutoDossieProdutoMtrClient.toException(response));

        assertEquals(409, falha.status());
        assertEquals(erro, falha.erro());
        assertEquals("alteracao de produto em conflito", falha.getMessage());
    }

    @Test
    void usaFallbackPublicoQuandoCorpoMtrForMalformado() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(ProdutoDossieProdutoMtrException.Erro.class))
                .thenThrow(new ProcessingException("json interno invalido"));

        var falha = assertInstanceOf(
                ProdutoDossieProdutoMtrException.Servidor.class,
                ProdutoDossieProdutoMtrClient.toException(response));

        assertEquals(500, falha.erro().codigoHttp());
        assertEquals("simtr-dossie-produto", falha.erro().recurso());
        assertEquals("ARVDOCP0002", falha.erro().codigoErro());
        assertFalse(falha.erro().idErro().isBlank());
        assertEquals(
                "Erro retornado pelo serviço MTR fora do contrato esperado.",
                falha.erro().erros().getFirst().mensagem());
        assertNull(falha.erro().detalhe());
        assertNull(falha.erro().stacktrace());
        assertFalse(falha.getMessage().contains("json interno"));
    }

    @Test
    void aplicaMatrizDeFaultToleranceAprovada() throws NoSuchMethodException {
        Method metodo = ProdutoDossieProdutoMtrClient.class.getMethod(
                "alterar", Long.class, List.class);

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
                ProdutoDossieProdutoMtrException.Servidor.class,
                ProcessingException.class,
                TimeoutException.class
        }, retry.retryOn());
        assertArrayEquals(new Class<?>[]{
                ProdutoDossieProdutoMtrException.Negocio.class,
                ProdutoDossieProdutoMtrException.TecnicaCliente.class
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

    private static Response responseSemCorpo(int status) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);
        return response;
    }
}
