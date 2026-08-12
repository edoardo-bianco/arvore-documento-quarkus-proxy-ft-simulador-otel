package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.RestClientObservabilityFilter;
import br.gov.caixa.simtr.hub.arquitetura.seguranca.RequestHeaderFactory;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v2.consulta.ConsultaDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.ConsultaDossieProdutoMtrException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.ParameterizedType;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.junit.jupiter.api.Test;

class ConsultaDossieProdutoMtrClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SERVICO_MTR = "simtr-dossie-produto";
    private static final String CODIGO_FALLBACK = "ARVDOCP0002";
    private static final String METODO_CONSULTAR = "consultar";
    private static final String PREFIXO_STATUS = "status ";
    private static final String MENSAGEM_SEM_ERRO = "Erro retornado pelo servico MTR";

    @Test
    void declaraGetWireSemCorpoComAcceptJsonEProvidersExistentes() throws Exception {
        var tipo = ConsultaDossieProdutoMtrClient.class;
        var metodo = tipo.getMethod(METODO_CONSULTAR, Long.class);

        assertEquals("dossie-produto", tipo.getAnnotation(RegisterRestClient.class).configKey());
        assertEquals("/dossie-produto", tipo.getAnnotation(Path.class).value());
        assertEquals(RequestHeaderFactory.class,
                tipo.getAnnotation(RegisterClientHeaders.class).value());
        Set<Class<?>> providers = Arrays.stream(tipo.getAnnotationsByType(RegisterProvider.class))
                .map(RegisterProvider::value)
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                OidcClientRequestReactiveFilter.class,
                RestClientObservabilityFilter.class
        ), providers);
        assertArrayEquals(new String[]{MediaType.APPLICATION_JSON},
                tipo.getAnnotation(Produces.class).value());
        assertNull(tipo.getAnnotation(Consumes.class));

        assertNotNull(metodo.getAnnotation(GET.class));
        assertEquals("/v2/dossie-produto/{id}", metodo.getAnnotation(Path.class).value());
        assertNull(metodo.getAnnotation(Consumes.class));
        assertEquals("id", metodo.getParameters()[0].getAnnotation(PathParam.class).value());
        assertEquals(Uni.class, metodo.getReturnType());
        var retorno = assertInstanceOf(ParameterizedType.class, metodo.getGenericReturnType());
        assertEquals(ConsultaDossieProdutoMtrResponse.class,
                retorno.getActualTypeArguments()[0]);
    }

    @Test
    void desserializaEClassificaErroDeNegocioSemPerderPayload() throws Exception {
        var erro = OBJECT_MAPPER.readValue("""
                {
                  "codigo_http": 404,
                  "recurso": "simtr-dossie-produto",
                  "id_erro": "dossie-404",
                  "codigo_erro": "MTR-DOSSIE-404",
                  "erros": [{"mensagem": "dossie nao localizado"}],
                  "detalhe": "negocio",
                  "stacktrace": "stacktrace externo"
                }
                """, ConsultaDossieProdutoMtrException.Erro.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(404);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(ConsultaDossieProdutoMtrException.Erro.class)).thenReturn(erro);

        var falha = assertInstanceOf(
                ConsultaDossieProdutoMtrException.Negocio.class,
                ConsultaDossieProdutoMtrClient.toException(response)
        );

        assertEquals(404, falha.status());
        assertEquals(erro, falha.erro());
        assertEquals("dossie nao localizado", falha.getMessage());
        assertEquals("dossie-404", falha.erro().idErro());
        assertEquals("MTR-DOSSIE-404", falha.erro().codigoErro());
        assertEquals("stacktrace externo", falha.erro().stacktrace());
    }

    @Test
    void classificaTodosOsGruposDeStatusAprovados() {
        assertNull(ConsultaDossieProdutoMtrClient.toException(responseSemPayload(200)));
        assertNull(ConsultaDossieProdutoMtrClient.toException(responseSemPayload(399)));

        for (int status : new int[]{400, 404, 409, 422}) {
            assertInstanceOf(
                    ConsultaDossieProdutoMtrException.Negocio.class,
                    ConsultaDossieProdutoMtrClient.toException(responseSemPayload(status)),
                    PREFIXO_STATUS + status
            );
        }
        for (int status : new int[]{401, 403, 405, 429, 499}) {
            assertInstanceOf(
                    ConsultaDossieProdutoMtrException.TecnicaCliente.class,
                    ConsultaDossieProdutoMtrClient.toException(responseSemPayload(status)),
                    PREFIXO_STATUS + status
            );
        }
        for (int status : new int[]{500, 502, 503}) {
            assertInstanceOf(
                    ConsultaDossieProdutoMtrException.Servidor.class,
                    ConsultaDossieProdutoMtrClient.toException(responseSemPayload(status)),
                    PREFIXO_STATUS + status
            );
        }
    }

    @Test
    void normalizaSomenteCamposObrigatoriosAusentesDoPayloadDeErro() {
        var erroIncompleto = new ConsultaDossieProdutoMtrException.Erro(
                null, null, null, null, null, null, null
        );
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(404);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(ConsultaDossieProdutoMtrException.Erro.class))
                .thenReturn(erroIncompleto);

        var falha = assertInstanceOf(
                ConsultaDossieProdutoMtrException.Negocio.class,
                ConsultaDossieProdutoMtrClient.toException(response)
        );

        assertEquals(404, falha.erro().codigoHttp());
        assertEquals(SERVICO_MTR, falha.erro().recurso());
        assertFalse(falha.erro().idErro().isBlank());
        assertNull(falha.erro().codigoErro());
        assertNull(falha.erro().erros());
        assertNull(falha.erro().detalhe());
        assertNull(falha.erro().stacktrace());
    }

    @Test
    void usaFallbackContratualQuandoPayloadDeErroForMalformado() {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(ConsultaDossieProdutoMtrException.Erro.class))
                .thenThrow(new ProcessingException("json invalido"));

        var falha = assertInstanceOf(
                ConsultaDossieProdutoMtrException.Servidor.class,
                ConsultaDossieProdutoMtrClient.toException(response)
        );

        assertEquals(500, falha.erro().codigoHttp());
        assertEquals(SERVICO_MTR, falha.erro().recurso());
        assertFalse(falha.erro().idErro().isBlank());
        assertEquals(CODIGO_FALLBACK, falha.erro().codigoErro());
        assertEquals("Erro retornado pelo serviço MTR fora do contrato esperado.",
                falha.erro().erros().getFirst().mensagem());
        assertNull(falha.erro().detalhe());
        assertNull(falha.erro().stacktrace());
    }

    @Test
    void usaMensagemSeguraQuandoErroNaoPossuiMensagem() {
        var falhaSemErro = new ConsultaDossieProdutoMtrException.Servidor(500, null);
        var falhaSemMensagens = new ConsultaDossieProdutoMtrException.Servidor(
                500,
                new ConsultaDossieProdutoMtrException.Erro(
                        500, SERVICO_MTR, "dossie-500", null, List.of(), null, null
                )
        );

        assertEquals(MENSAGEM_SEM_ERRO, falhaSemErro.getMessage());
        assertEquals(MENSAGEM_SEM_ERRO, falhaSemMensagens.getMessage());
    }

    @Test
    void preservaMatrizDeFaultToleranceAprovada() throws NoSuchMethodException {
        var metodo = ConsultaDossieProdutoMtrClient.class.getMethod(
                METODO_CONSULTAR, Long.class
        );

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
                ConsultaDossieProdutoMtrException.Servidor.class,
                ProcessingException.class,
                TimeoutException.class
        }, retry.retryOn());
        assertArrayEquals(new Class<?>[]{
                ConsultaDossieProdutoMtrException.Negocio.class,
                ConsultaDossieProdutoMtrException.TecnicaCliente.class
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

    private static Response responseSemPayload(int status) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);
        when(response.hasEntity()).thenReturn(false);
        return response;
    }
}
