package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client;

import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.formulario.FormularioDossieProdutoMtrRequest;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.FormularioDossieProdutoMtrException;
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

class FormularioDossieProdutoMtrClientTest {

    @Test
    void classificaErroDeNegocioEPreservaCorpoMtr() {
        FormularioDossieProdutoMtrException.Erro erro =
                new FormularioDossieProdutoMtrException.Erro(
                        400, "simtr-dossie-produto", "formulario-400", "MTR-FORM-400",
                        List.of(new FormularioDossieProdutoMtrException.Mensagem(
                                "formulario nao permitido")),
                        "negocio", "stack-remota");
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(400);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(FormularioDossieProdutoMtrException.Erro.class))
                .thenReturn(erro);

        FormularioDossieProdutoMtrException.Negocio falha = assertInstanceOf(
                FormularioDossieProdutoMtrException.Negocio.class,
                FormularioDossieProdutoMtrClient.toException(response));

        assertEquals(400, falha.status());
        assertEquals(erro, falha.erro());
    }

    @Test
    void usaFallbackQuandoCorpoMtrForInvalido() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(FormularioDossieProdutoMtrException.Erro.class))
                .thenThrow(new ProcessingException("json invalido"));

        FormularioDossieProdutoMtrException.Servidor falha = assertInstanceOf(
                FormularioDossieProdutoMtrException.Servidor.class,
                FormularioDossieProdutoMtrClient.toException(response));

        assertEquals(500, falha.erro().codigoHttp());
        assertEquals("simtr-dossie-produto", falha.erro().recurso());
        assertEquals("ARVDOCP0002", falha.erro().codigoErro());
    }

    @Test
    void preservaMatrizDeFaultToleranceNoNovoClient() throws NoSuchMethodException {
        Method metodo = FormularioDossieProdutoMtrClient.class.getMethod(
                "atualizar", Long.class, List.class);

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
                FormularioDossieProdutoMtrException.Servidor.class,
                ProcessingException.class,
                TimeoutException.class
        }, retry.retryOn());
        assertArrayEquals(new Class<?>[]{
                FormularioDossieProdutoMtrException.Negocio.class,
                FormularioDossieProdutoMtrException.TecnicaCliente.class
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
