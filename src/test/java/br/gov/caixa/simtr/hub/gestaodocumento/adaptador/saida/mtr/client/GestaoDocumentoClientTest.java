package br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.mtr.client;

import br.gov.caixa.simtr.hub.gestaodocumento.adaptador.saida.mtr.erro.GestaoDocumentoMtrException;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GestaoDocumentoClientTest {

    private static final String RECURSO_GESTAO_DOCUMENTO = "simtr-gestao-documento";
    private static final String PREFIXO_STATUS = "status ";

    @Test
    void preservaPayloadDoErroDeNegocio() {
        var erro = new GestaoDocumentoMtrException.Erro(
                404,
                RECURSO_GESTAO_DOCUMENTO,
                "credencial-404",
                "MTR-CREDENCIAL-404",
                List.of(new GestaoDocumentoMtrException.Mensagem("container nao localizado")),
                "negocio",
                "stacktrace externo"
        );
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(404);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(GestaoDocumentoMtrException.Erro.class)).thenReturn(erro);

        var falha = assertInstanceOf(
                GestaoDocumentoMtrException.Negocio.class,
                GestaoDocumentoClient.toException(response)
        );

        assertEquals(404, falha.status());
        assertEquals(erro, falha.erro());
    }

    @Test
    void normalizaSomenteCamposAusentesDoPayloadDeErro() {
        var erroIncompleto = new GestaoDocumentoMtrException.Erro(
                null, null, null, null, null, null, null
        );
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(404);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(GestaoDocumentoMtrException.Erro.class))
                .thenReturn(erroIncompleto);

        var falha = assertInstanceOf(
                GestaoDocumentoMtrException.Negocio.class,
                GestaoDocumentoClient.toException(response)
        );

        assertEquals(404, falha.erro().codigoHttp());
        assertEquals(RECURSO_GESTAO_DOCUMENTO, falha.erro().recurso());
        assertNotNull(falha.erro().idErro());
        assertNull(falha.erro().codigoErro());
        assertNull(falha.erro().erros());
        assertNull(falha.erro().detalhe());
        assertNull(falha.erro().stacktrace());
    }

    @Test
    void preservaClassificacaoDeStatusDoClientLegado() {
        assertNull(GestaoDocumentoClient.toException(responseSemPayload(399)));

        for (int status : new int[]{400, 404, 409, 422}) {
            assertInstanceOf(
                    GestaoDocumentoMtrException.Negocio.class,
                    GestaoDocumentoClient.toException(responseSemPayload(status)),
                    PREFIXO_STATUS + status
            );
        }
        for (int status : new int[]{401, 403, 405, 429}) {
            assertInstanceOf(
                    GestaoDocumentoMtrException.TecnicaCliente.class,
                    GestaoDocumentoClient.toException(responseSemPayload(status)),
                    PREFIXO_STATUS + status
            );
        }
        for (int status : new int[]{500, 502, 503}) {
            assertInstanceOf(
                    GestaoDocumentoMtrException.Servidor.class,
                    GestaoDocumentoClient.toException(responseSemPayload(status)),
                    PREFIXO_STATUS + status
            );
        }
    }

    @Test
    void usaFallbackContratualQuandoPayloadDeErroForInvalido() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(GestaoDocumentoMtrException.Erro.class))
                .thenThrow(new ProcessingException("json invalido"));

        var falha = assertInstanceOf(
                GestaoDocumentoMtrException.Servidor.class,
                GestaoDocumentoClient.toException(response)
        );

        assertEquals(500, falha.erro().codigoHttp());
        assertEquals(RECURSO_GESTAO_DOCUMENTO, falha.erro().recurso());
        assertEquals("ARVDOCP0002", falha.erro().codigoErro());
        assertEquals(
                "Erro retornado pelo serviço MTR fora do contrato esperado.",
                falha.erro().erros().getFirst().mensagem()
        );
    }

    @Test
    void preservaMatrizDeFaultToleranceNoNovoClient() throws NoSuchMethodException {
        Method method = GestaoDocumentoClient.class.getMethod("gerarCredencialContainer");

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
                GestaoDocumentoMtrException.Servidor.class,
                ProcessingException.class,
                TimeoutException.class
        }, retry.retryOn());
        assertArrayEquals(new Class<?>[]{
                GestaoDocumentoMtrException.Negocio.class,
                GestaoDocumentoMtrException.TecnicaCliente.class
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

    private static Response responseSemPayload(int status) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);
        when(response.hasEntity()).thenReturn(false);
        return response;
    }
}
