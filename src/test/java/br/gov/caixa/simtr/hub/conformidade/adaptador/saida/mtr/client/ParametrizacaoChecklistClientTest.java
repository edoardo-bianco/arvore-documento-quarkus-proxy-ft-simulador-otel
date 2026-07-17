package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.mtr.erro.ChecklistMtrException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;

class ParametrizacaoChecklistClientTest {

    private static final String SERVICO_MTR = "simtr-parametrizacao";
    private static final String PREFIXO_STATUS = "status ";

    @Test
    void preservaPayloadDoErroDeNegocio() {
        var erro = new ChecklistMtrException.Erro(
                404,
                SERVICO_MTR,
                "checklist-404",
                "MTR-CHECKLIST-404",
                List.of(new ChecklistMtrException.Mensagem("checklist nao localizado")),
                "negocio",
                "stacktrace externo"
        );
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(404);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(ChecklistMtrException.Erro.class)).thenReturn(erro);

        var falha = assertInstanceOf(
                ChecklistMtrException.Negocio.class,
                ParametrizacaoChecklistClient.toException(response)
        );

        assertEquals(404, falha.status());
        assertEquals(erro, falha.erro());
    }

    @Test
    void normalizaSomenteCamposAusentesDoPayloadDeErro() {
        var erroIncompleto = new ChecklistMtrException.Erro(
                null, null, null, null, null, null, null
        );
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(404);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(ChecklistMtrException.Erro.class)).thenReturn(erroIncompleto);

        var falha = assertInstanceOf(
                ChecklistMtrException.Negocio.class,
                ParametrizacaoChecklistClient.toException(response)
        );

        assertEquals(404, falha.erro().codigoHttp());
        assertEquals(SERVICO_MTR, falha.erro().recurso());
        assertNotNull(falha.erro().idErro());
        assertNull(falha.erro().codigoErro());
        assertNull(falha.erro().erros());
        assertNull(falha.erro().detalhe());
        assertNull(falha.erro().stacktrace());
    }

    @Test
    void preservaClassificacaoDeStatusDoClienteLegado() {
        assertNull(ParametrizacaoChecklistClient.toException(responseSemPayload(399)));

        for (int status : new int[]{400, 404, 409, 422}) {
            assertInstanceOf(ChecklistMtrException.Negocio.class,
                    ParametrizacaoChecklistClient.toException(responseSemPayload(status)),
                    PREFIXO_STATUS + status);
        }
        for (int status : new int[]{401, 403, 405, 429}) {
            assertInstanceOf(ChecklistMtrException.TecnicaCliente.class,
                    ParametrizacaoChecklistClient.toException(responseSemPayload(status)),
                    PREFIXO_STATUS + status);
        }
        for (int status : new int[]{500, 502, 503}) {
            assertInstanceOf(ChecklistMtrException.Servidor.class,
                    ParametrizacaoChecklistClient.toException(responseSemPayload(status)),
                    PREFIXO_STATUS + status);
        }
    }

    @Test
    void usaFallbackContratualQuandoPayloadDeErroForInvalido() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(ChecklistMtrException.Erro.class))
                .thenThrow(new ProcessingException("json invalido"));

        var falha = assertInstanceOf(
                ChecklistMtrException.Servidor.class,
                ParametrizacaoChecklistClient.toException(response)
        );

        assertEquals(500, falha.erro().codigoHttp());
        assertEquals(SERVICO_MTR, falha.erro().recurso());
        assertEquals("ARVDOCP0002", falha.erro().codigoErro());
        assertEquals("Erro retornado pelo serviço MTR fora do contrato esperado.",
                falha.erro().erros().getFirst().mensagem());
    }

    @Test
    void preservaMatrizDeFaultToleranceNoNovoClient() throws NoSuchMethodException {
        Method method = ParametrizacaoChecklistClient.class.getMethod(
                "consultarPorIdentificadorNegocialEVersao", Long.class, Integer.class
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
                ChecklistMtrException.Servidor.class,
                ProcessingException.class,
                TimeoutException.class
        }, retry.retryOn());
        assertArrayEquals(new Class<?>[]{
                ChecklistMtrException.Negocio.class,
                ChecklistMtrException.TecnicaCliente.class
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
