package br.gov.caixa.simtr.hub.arquitetura.observabilidade;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class RestClientObservabilityFilterTest {

    private static final String INVOKED_METHOD_PROPERTY = "org.eclipse.microprofile.rest.client.invokedMethod";
    private static final String PAYLOAD_ENABLED_PROPERTY =
            "simtr-hub.observabilidade.rest-client.payload.habilitado";
    private static final String INPUT_MAX_LENGTH_PROPERTY =
            "simtr-hub.observabilidade.rest-client.payload.input.max-length";
    private static final String OUTPUT_MAX_LENGTH_PROPERTY =
            "simtr-hub.observabilidade.rest-client.payload.output.max-length";

    @AfterEach
    void limparProperties() {
        System.clearProperty(PAYLOAD_ENABLED_PROPERTY);
        System.clearProperty(INPUT_MAX_LENGTH_PROPERTY);
        System.clearProperty(OUTPUT_MAX_LENGTH_PROPERTY);
    }

    @Test
    void registraRequestEResponseComPayloadTruncado() throws Exception {
        System.setProperty(PAYLOAD_ENABLED_PROPERTY, "true");
        System.setProperty(INPUT_MAX_LENGTH_PROPERTY, "5");
        System.setProperty(OUTPUT_MAX_LENGTH_PROPERTY, "4");

        Map<String, Object> properties = new HashMap<>();
        properties.put(INVOKED_METHOD_PROPERTY, metodoRestClientFake());
        ClientRequestContext request = requestContext(properties, "POST", "http://localhost/simtr/recurso", "payload-maior");
        ClientResponseContext response = responseContext(
                201,
                "{\"ok\":true}",
                new MediaType("application", "json", Map.of("charset", "charset-invalido"))
        );
        RestClientObservabilityFilter filter = new RestClientObservabilityFilter();

        filter.filter(request);
        filter.filter(request, response);

        verify(request).setProperty(anyString(), any());
        verify(response).setEntityStream(any(ByteArrayInputStream.class));
    }

    @Test
    void registraPayloadDesabilitadoEInvocacaoDesconhecida() {
        System.setProperty(PAYLOAD_ENABLED_PROPERTY, "false");

        Map<String, Object> properties = new HashMap<>();
        ClientRequestContext request = requestContext(properties, "GET", "http://localhost/simtr/sem-payload", null);
        ClientResponseContext response = responseContext(204, null, null);
        RestClientObservabilityFilter filter = new RestClientObservabilityFilter();

        filter.filter(request);
        filter.filter(request, response);

        verify(request).setProperty(anyString(), any());
    }

    @Test
    void registraErroQuandoStreamDaRespostaNaoPodeSerLida() throws Exception {
        System.setProperty(PAYLOAD_ENABLED_PROPERTY, "true");
        System.setProperty(OUTPUT_MAX_LENGTH_PROPERTY, "100");

        Map<String, Object> properties = new HashMap<>();
        properties.put(INVOKED_METHOD_PROPERTY, metodoRestClientFake());
        ClientRequestContext request = requestContext(properties, "GET", "http://localhost/simtr/erro-stream", null);
        ClientResponseContext response = mock(ClientResponseContext.class);
        when(response.getStatus()).thenReturn(500);
        when(response.hasEntity()).thenReturn(true);
        when(response.getEntityStream()).thenReturn(new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("falha de leitura");
            }
        });
        RestClientObservabilityFilter filter = new RestClientObservabilityFilter();

        filter.filter(request);
        filter.filter(request, response);

        verify(request).setProperty(anyString(), any());
    }

    @Test
    void serializaTiposDePayloadDeRequestSuportados() {
        System.setProperty(PAYLOAD_ENABLED_PROPERTY, "true");

        RestClientObservabilityFilter filter = new RestClientObservabilityFilter();
        filter.filter(requestContext(new HashMap<>(), "POST", "http://localhost/simtr/string", "texto"));
        filter.filter(requestContext(new HashMap<>(), "POST", "http://localhost/simtr/bytes", "texto".getBytes(StandardCharsets.UTF_8)));
        filter.filter(requestContext(new HashMap<>(), "POST", "http://localhost/simtr/stream", new ByteArrayInputStream(new byte[]{1, 2, 3})));
        filter.filter(requestContext(new HashMap<>(), "POST", "http://localhost/simtr/objeto", new PayloadSemJson()));
    }

    @Test
    void mascaraCamposSensiveisNoPayloadAntesDeLogar() {
        String payload = """
                {
                  "sas": "segredo-sas",
                  "url_storage": "https://storage.example",
                  "aninhado": {
                    "token": "segredo-token"
                  }
                }
                """;

        String sanitized = RestClientObservabilityFilter.sanitizePayload(payload);

        assertFalse(sanitized.contains("segredo-sas"));
        assertFalse(sanitized.contains("segredo-token"));
        assertTrue(sanitized.contains("\"sas\":\"***\""));
        assertTrue(sanitized.contains("\"token\":\"***\""));
    }

    private static ClientRequestContext requestContext(
            Map<String, Object> properties,
            String method,
            String uri,
            Object entity
    ) {
        ClientRequestContext request = mock(ClientRequestContext.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getUri()).thenReturn(URI.create(uri));
        when(request.hasEntity()).thenReturn(entity != null);
        when(request.getEntity()).thenReturn(entity);
        when(request.getProperty(anyString())).thenAnswer(invocation -> properties.get(invocation.getArgument(0, String.class)));
        doAnswer(invocation -> {
            properties.put(invocation.getArgument(0, String.class), invocation.getArgument(1));
            return null;
        }).when(request).setProperty(anyString(), any());
        return request;
    }

    private static ClientResponseContext responseContext(int status, String body, MediaType mediaType) {
        ClientResponseContext response = mock(ClientResponseContext.class);
        when(response.getStatus()).thenReturn(status);
        when(response.hasEntity()).thenReturn(body != null);
        when(response.getEntityStream()).thenReturn(body != null
                ? new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))
                : null);
        when(response.getMediaType()).thenReturn(mediaType);
        return response;
    }

    private static Method metodoRestClientFake() throws NoSuchMethodException {
        return RestClientFake.class.getMethod("executar");
    }

    public interface RestClientFake {
        void executar();
    }

    private static class PayloadSemJson {
        @Override
        public String toString() {
            return "payload-sem-json";
        }
    }
}
