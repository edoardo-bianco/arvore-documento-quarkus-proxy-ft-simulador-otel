package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.RestClientObservabilityFilter;
import br.gov.caixa.simtr.hub.arquitetura.seguranca.RequestHeaderFactory;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v4.documentos.ConsultaDocumentosDossieProdutoMtrQuery;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v4.documentos.ConsultaDocumentosDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.ConsultaDocumentosDossieProdutoMtrException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.tracing.TracingPolicy;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
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

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class ConsultaDocumentosDossieProdutoMtrClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SERVICO_MTR = "simtr-dossie-produto";
    private static final String CODIGO_FALLBACK = "ARVDOCP0002";
    private static final String MENSAGEM_FALLBACK =
            "Erro retornado pelo serviço MTR fora do contrato esperado.";
    private static final String MENSAGEM_OBSERVAVEL = "Erro retornado pelo servico MTR";

    @Test
    void declaraGetComQueryPropriaCredenciaisEProviderLocalSeguro() throws Exception {
        var tipo = ConsultaDocumentosDossieProdutoMtrClient.class;
        var query = ConsultaDocumentosDossieProdutoMtrQuery.class;
        var metodo = tipo.getMethod("consultar", Long.class, query);
        var tracingProvider = ConsultaDocumentosDossieProdutoMtrTracingProvider.class;

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
        assertArrayEquals(new String[]{MediaType.APPLICATION_JSON},
                tipo.getAnnotation(Produces.class).value());
        assertNull(tipo.getAnnotation(Consumes.class));

        assertNotNull(metodo.getAnnotation(GET.class));
        assertEquals("/v4/dossie-produto/{id}/documentos",
                metodo.getAnnotation(Path.class).value());
        assertNull(metodo.getAnnotation(Consumes.class));
        assertEquals("id", metodo.getParameters()[0].getAnnotation(PathParam.class).value());
        assertNotNull(metodo.getParameters()[1].getAnnotation(BeanParam.class));
        assertEquals(Uni.class, metodo.getReturnType());
        var retorno = assertInstanceOf(ParameterizedType.class, metodo.getGenericReturnType());
        assertEquals(Uni.class, retorno.getRawType());
        var lista = assertInstanceOf(ParameterizedType.class, retorno.getActualTypeArguments()[0]);
        assertEquals(List.class, lista.getRawType());
        assertEquals(ConsultaDocumentosDossieProdutoMtrResponse.class,
                lista.getActualTypeArguments()[0]);
    }

    @Test
    void representaSomenteOsDozeFiltrosAprovadosEOmiteAusentes() {
        Map<String, String> filtros = Arrays.stream(
                        ConsultaDocumentosDossieProdutoMtrQuery.class.getDeclaredFields())
                .filter(campo -> campo.isAnnotationPresent(QueryParam.class))
                .collect(Collectors.toMap(
                        Field::getName,
                        campo -> campo.getAnnotation(QueryParam.class).value()));

        assertEquals(Map.ofEntries(
                Map.entry("cnpj", "cnpj"),
                Map.entry("cpf", "cpf"),
                Map.entry("fase", "fase"),
                Map.entry("incluiArmazenamento", "inclui-armazenamento"),
                Map.entry("incluiAssinaturas", "inclui-assinaturas"),
                Map.entry("incluiAtributos", "inclui-atributos"),
                Map.entry("incluiConformidade", "inclui-conformidade"),
                Map.entry("incluiOutsourcing", "inclui-outsourcing"),
                Map.entry("incluiPropriedades", "inclui-propriedades"),
                Map.entry("incluiUrl", "inclui-url"),
                Map.entry("ipUsuario", "ip-usuario"),
                Map.entry("tipologia", "tipologia")), filtros);
        assertEquals(12, ConsultaDocumentosDossieProdutoMtrQuery.class.getDeclaredFields().length);
        assertTrue(Arrays.stream(ConsultaDocumentosDossieProdutoMtrQuery.class.getDeclaredFields())
                .noneMatch(campo -> campo.getType().isPrimitive()));

        var vazia = new ConsultaDocumentosDossieProdutoMtrQuery();
        assertNull(vazia.cnpj());
        assertNull(vazia.fase());
        assertNull(vazia.incluiUrl());

        var completa = new ConsultaDocumentosDossieProdutoMtrQuery(
                "123", "456", 7L, true, false, true, false, true, false, true,
                "192.0.2.10", "CONTRATO");
        assertEquals("123", completa.cnpj());
        assertEquals("456", completa.cpf());
        assertEquals(7L, completa.fase());
        assertEquals(true, completa.incluiArmazenamento());
        assertEquals(false, completa.incluiAssinaturas());
        assertEquals(true, completa.incluiAtributos());
        assertEquals(false, completa.incluiConformidade());
        assertEquals(true, completa.incluiOutsourcing());
        assertEquals(false, completa.incluiPropriedades());
        assertEquals(true, completa.incluiUrl());
        assertEquals("192.0.2.10", completa.ipUsuario());
        assertEquals("CONTRATO", completa.tipologia());
    }

    @Test
    void ignoraSpanHttpAutomaticoEMantemPropagacaoDoContexto() {
        OpenTelemetry openTelemetry = mock(OpenTelemetry.class);
        TextMapPropagator propagator = mock(TextMapPropagator.class);
        when(openTelemetry.getPropagators()).thenReturn(ContextPropagators.create(propagator));
        var provider = new ConsultaDocumentosDossieProdutoMtrTracingProvider(openTelemetry);

        Priority prioridade = ConsultaDocumentosDossieProdutoMtrTracingProvider.class
                .getAnnotation(Priority.class);
        assertNotNull(prioridade);
        assertEquals(Priorities.AUTHENTICATION - 1, prioridade.value());

        HttpClientOptions options = provider.getContext(HttpClientOptions.class);
        assertNotNull(options);
        assertEquals(TracingPolicy.IGNORE, options.getTracingPolicy());
        assertNull(provider.getContext(String.class));

        ClientRequestContext request = mock(ClientRequestContext.class);
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(request.getHeaders()).thenReturn(headers);

        provider.filter(request);

        verify(propagator).inject(eq(Context.current()), same(headers), any());
    }

    @Test
    void classificaStatusSemTratarNoContentComoErro() {
        for (int status : new int[]{200, 204, 299, 399}) {
            assertNull(ConsultaDocumentosDossieProdutoMtrClient.toException(
                    responseSemPayload(status)), "status " + status);
        }
        for (int status : new int[]{400, 404}) {
            assertInstanceOf(
                    ConsultaDocumentosDossieProdutoMtrException.Negocio.class,
                    ConsultaDocumentosDossieProdutoMtrClient.toException(
                            responseSemPayload(status)),
                    "status " + status);
        }
        for (int status : new int[]{401, 403, 405, 409, 422, 429, 499}) {
            assertInstanceOf(
                    ConsultaDocumentosDossieProdutoMtrException.TecnicaCliente.class,
                    ConsultaDocumentosDossieProdutoMtrClient.toException(
                            responseSemPayload(status)),
                    "status " + status);
        }
        for (int status : new int[]{500, 502, 503}) {
            assertInstanceOf(
                    ConsultaDocumentosDossieProdutoMtrException.Servidor.class,
                    ConsultaDocumentosDossieProdutoMtrClient.toException(
                            responseSemPayload(status)),
                    "status " + status);
        }
    }

    @Test
    void preservaPayloadDeErroValidoSemExpoLoNaMensagemDaExcecao() throws Exception {
        var erro = OBJECT_MAPPER.readValue("""
                {
                  "codigo_http": 400,
                  "recurso": "mtr-externo",
                  "id_erro": "externo-400",
                  "codigo_erro": "MTR-400",
                  "erros": [{"mensagem": "cpf sigiloso em texto externo"}],
                  "detalhe": "detalhe externo",
                  "stacktrace": "stacktrace externo"
                }
                """, ConsultaDocumentosDossieProdutoMtrException.Erro.class);
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(400);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(ConsultaDocumentosDossieProdutoMtrException.Erro.class))
                .thenReturn(erro);

        var falha = assertInstanceOf(
                ConsultaDocumentosDossieProdutoMtrException.Negocio.class,
                ConsultaDocumentosDossieProdutoMtrClient.toException(response));

        assertEquals(400, falha.status());
        assertSame(erro, falha.erro());
        assertEquals(MENSAGEM_OBSERVAVEL, falha.getMessage());
        assertFalse(falha.getMessage().contains("sigiloso"));
        assertEquals("stacktrace externo", falha.erro().stacktrace());
    }

    @Test
    void normalizaSomenteIdentidadeAusenteDoErroExterno() {
        var erroIncompleto = new ConsultaDocumentosDossieProdutoMtrException.Erro(
                null, null, null, null, null, "detalhe preservado", "stack preservado");
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(404);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(ConsultaDocumentosDossieProdutoMtrException.Erro.class))
                .thenReturn(erroIncompleto);

        var falha = assertInstanceOf(
                ConsultaDocumentosDossieProdutoMtrException.Negocio.class,
                ConsultaDocumentosDossieProdutoMtrClient.toException(response));

        assertEquals(404, falha.erro().codigoHttp());
        assertEquals(SERVICO_MTR, falha.erro().recurso());
        assertFalse(falha.erro().idErro().isBlank());
        assertNull(falha.erro().codigoErro());
        assertNull(falha.erro().erros());
        assertEquals("detalhe preservado", falha.erro().detalhe());
        assertEquals("stack preservado", falha.erro().stacktrace());
    }

    @Test
    void usaFallbackSeguroParaErroAusenteOuMalformado() {
        var semPayload = assertInstanceOf(
                ConsultaDocumentosDossieProdutoMtrException.Servidor.class,
                ConsultaDocumentosDossieProdutoMtrClient.toException(responseSemPayload(500)));
        assertFallbackSeguro(semPayload, 500);

        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(502);
        when(response.hasEntity()).thenReturn(true);
        when(response.readEntity(ConsultaDocumentosDossieProdutoMtrException.Erro.class))
                .thenThrow(new ProcessingException("conteudo externo invalido"));
        var malformado = assertInstanceOf(
                ConsultaDocumentosDossieProdutoMtrException.Servidor.class,
                ConsultaDocumentosDossieProdutoMtrClient.toException(response));
        assertFallbackSeguro(malformado, 502);
    }

    @Test
    void aplicaRetrySomenteAErrosTransitoriosComTimeoutECircuitBreakerAprovados()
            throws Exception {
        var metodo = ConsultaDocumentosDossieProdutoMtrClient.class.getMethod(
                "consultar", Long.class, ConsultaDocumentosDossieProdutoMtrQuery.class);

        Timeout timeout = metodo.getAnnotation(Timeout.class);
        assertNotNull(timeout);
        assertEquals(2_000, timeout.value());
        assertEquals(ChronoUnit.MILLIS, timeout.unit());

        Retry retry = metodo.getAnnotation(Retry.class);
        assertNotNull(retry);
        assertEquals(3, retry.maxRetries());
        assertEquals(300, retry.delay());
        assertEquals(ChronoUnit.MILLIS, retry.delayUnit());
        assertEquals(100, retry.jitter());
        assertEquals(ChronoUnit.MILLIS, retry.jitterDelayUnit());
        assertArrayEquals(new Class<?>[]{
                ConsultaDocumentosDossieProdutoMtrException.Servidor.class,
                ProcessingException.class,
                TimeoutException.class
        }, retry.retryOn());
        assertArrayEquals(new Class<?>[]{
                ConsultaDocumentosDossieProdutoMtrException.Negocio.class,
                ConsultaDocumentosDossieProdutoMtrException.TecnicaCliente.class
        }, retry.abortOn());

        CircuitBreaker circuitBreaker = metodo.getAnnotation(CircuitBreaker.class);
        assertNotNull(circuitBreaker);
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

    private static void assertFallbackSeguro(
            ConsultaDocumentosDossieProdutoMtrException falha,
            int status
    ) {
        assertEquals(status, falha.erro().codigoHttp());
        assertEquals(SERVICO_MTR, falha.erro().recurso());
        assertFalse(falha.erro().idErro().isBlank());
        assertEquals(CODIGO_FALLBACK, falha.erro().codigoErro());
        assertEquals(MENSAGEM_FALLBACK, falha.erro().erros().getFirst().mensagem());
        assertNull(falha.erro().detalhe());
        assertNull(falha.erro().stacktrace());
        assertEquals(MENSAGEM_OBSERVAVEL, falha.getMessage());
    }
}
