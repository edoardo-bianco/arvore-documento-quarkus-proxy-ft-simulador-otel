package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v2.documento.DocumentoDossieProdutoMtrRequest;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.DocumentoDossieProdutoMtrException;
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

class DocumentoDossieProdutoMtrClientTest {

    @Test
    void classificaErroDeNegocioEPreservaCorpoMtr() {
        DocumentoDossieProdutoMtrException.Erro erro =
                new DocumentoDossieProdutoMtrException.Erro(
                        400, "simtr-dossie-produto", "documento-400", "MTR-DOC-400",
                        List.of(new DocumentoDossieProdutoMtrException.Mensagem(
                                "documento nao permitido")),
                        "negocio", "stack-remota");
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(400);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(DocumentoDossieProdutoMtrException.Erro.class))
                .thenReturn(erro);

        DocumentoDossieProdutoMtrException.Negocio falha = assertInstanceOf(
                DocumentoDossieProdutoMtrException.Negocio.class,
                DocumentoDossieProdutoMtrClient.toException(response));

        assertEquals(400, falha.status());
        assertEquals(erro, falha.erro());
    }

    @Test
    void usaFallbackQuandoCorpoMtrForInvalido() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(DocumentoDossieProdutoMtrException.Erro.class))
                .thenThrow(new ProcessingException("json invalido"));

        DocumentoDossieProdutoMtrException.Servidor falha = assertInstanceOf(
                DocumentoDossieProdutoMtrException.Servidor.class,
                DocumentoDossieProdutoMtrClient.toException(response));

        assertEquals(500, falha.erro().codigoHttp());
        assertEquals("simtr-dossie-produto", falha.erro().recurso());
        assertEquals("ARVDOCP0002", falha.erro().codigoErro());
    }

    @Test
    void preservaMatrizDeFaultToleranceNoNovoClientV2() throws NoSuchMethodException {
        Method metodo = DocumentoDossieProdutoMtrClient.class.getMethod(
                "incluir", Long.class, DocumentoDossieProdutoMtrRequest.class);

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
                DocumentoDossieProdutoMtrException.Servidor.class,
                ProcessingException.class,
                TimeoutException.class
        }, retry.retryOn());
        assertArrayEquals(new Class<?>[]{
                DocumentoDossieProdutoMtrException.Negocio.class,
                DocumentoDossieProdutoMtrException.TecnicaCliente.class
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
}
