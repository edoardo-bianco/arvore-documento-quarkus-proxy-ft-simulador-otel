package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.criacao.CriacaoDossieProdutoMtrRequest;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.CriacaoDossieProdutoMtrException;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class CriacaoDossieProdutoMtrClientTest {

    @Test
    void classificaErroDeNegocioEPreservaCorpoMtr() {
        CriacaoDossieProdutoMtrException.Erro erro = new CriacaoDossieProdutoMtrException.Erro(
                400, "simtr-dossie-produto", "id-400", "MTR-400",
                List.of(new CriacaoDossieProdutoMtrException.Mensagem("negocio")),
                "detalhe", "stacktrace");
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(400);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(CriacaoDossieProdutoMtrException.Erro.class)).thenReturn(erro);

        CriacaoDossieProdutoMtrException.Negocio falha = assertInstanceOf(
                CriacaoDossieProdutoMtrException.Negocio.class,
                CriacaoDossieProdutoMtrClient.toException(response));

        assertEquals(400, falha.status());
        assertEquals(erro, falha.erro());
    }

    @Test
    void usaFallbackQuandoCorpoMtrForInvalido() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(CriacaoDossieProdutoMtrException.Erro.class))
                .thenThrow(new ProcessingException("json invalido"));

        CriacaoDossieProdutoMtrException.Servidor falha = assertInstanceOf(
                CriacaoDossieProdutoMtrException.Servidor.class,
                CriacaoDossieProdutoMtrClient.toException(response));

        assertEquals(500, falha.erro().codigoHttp());
        assertEquals("simtr-dossie-produto", falha.erro().recurso());
        assertEquals("ARVDOCP0002", falha.erro().codigoErro());
    }

    @Test
    void preservaMatrizDeFaultToleranceNoNovoClient() throws NoSuchMethodException {
        Method method = CriacaoDossieProdutoMtrClient.class.getMethod(
                "criar", CriacaoDossieProdutoMtrRequest.class);

        Timeout timeout = method.getAnnotation(Timeout.class);
        assertEquals(2_000, timeout.value());
        assertEquals(ChronoUnit.MILLIS, timeout.unit());

        Retry retry = method.getAnnotation(Retry.class);
        assertEquals(3, retry.maxRetries());
        assertEquals(300, retry.delay());
        assertEquals(100, retry.jitter());
        assertArrayEquals(new Class<?>[]{
                CriacaoDossieProdutoMtrException.Servidor.class,
                ProcessingException.class,
                TimeoutException.class
        }, retry.retryOn());
        assertArrayEquals(new Class<?>[]{
                CriacaoDossieProdutoMtrException.Negocio.class,
                CriacaoDossieProdutoMtrException.TecnicaCliente.class
        }, retry.abortOn());

        CircuitBreaker circuitBreaker = method.getAnnotation(CircuitBreaker.class);
        assertEquals(10, circuitBreaker.requestVolumeThreshold());
        assertEquals(0.5, circuitBreaker.failureRatio());
        assertEquals(10_000, circuitBreaker.delay());
        assertEquals(2, circuitBreaker.successThreshold());
        assertArrayEquals(retry.retryOn(), circuitBreaker.failOn());
        assertArrayEquals(retry.abortOn(), circuitBreaker.skipOn());
    }
}
