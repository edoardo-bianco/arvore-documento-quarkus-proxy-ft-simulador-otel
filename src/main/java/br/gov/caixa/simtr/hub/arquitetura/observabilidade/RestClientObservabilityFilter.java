package br.gov.caixa.simtr.hub.arquitetura.observabilidade;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.ObservabilityLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Provider
@Priority(Priorities.USER)
public class RestClientObservabilityFilter implements ClientRequestFilter, ClientResponseFilter {

    private static final Logger LOG = Logger.getLogger(RestClientObservabilityFilter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String INVOKED_METHOD_PROPERTY = "org.eclipse.microprofile.rest.client.invokedMethod";
    private static final String START_NANO_PROPERTY = RestClientObservabilityFilter.class.getName() + ".startNano";

    private static final String PAYLOAD_ENABLED_PROPERTY =
            "simtr-hub.observabilidade.rest-client.payload.habilitado";
    private static final String INPUT_MAX_LENGTH_PROPERTY =
            "simtr-hub.observabilidade.rest-client.payload.input.max-length";
    private static final String OUTPUT_MAX_LENGTH_PROPERTY =
            "simtr-hub.observabilidade.rest-client.payload.output.max-length";

    private static final boolean DEFAULT_PAYLOAD_ENABLED = true;
    private static final int DEFAULT_INPUT_MAX_LENGTH = 2_000;
    private static final int DEFAULT_OUTPUT_MAX_LENGTH = 4_000;
    private static final String MASKED_VALUE = "***";
    private static final Set<String> SENSITIVE_FIELD_NAMES = Set.of(
            "sas",
            "token",
            "access_token",
            "refresh_token",
            "client_secret",
            "secret",
            "apikey",
            "api_key",
            "password"
    );
    private static final Pattern SENSITIVE_JSON_STRING_FIELD_PATTERN = Pattern.compile(
            "(?i)(\"(?:sas|token|access_token|refresh_token|client_secret|secret|apikey|api_key|password)\"\\s*:\\s*)\"[^\"]*\""
    );

    @Override
    public void filter(ClientRequestContext requestContext) {
        requestContext.setProperty(START_NANO_PROPERTY, System.nanoTime());

        InvocationMetadata invocation = invocationMetadata(requestContext);
        URI uri = requestContext.getUri();
        boolean payloadEnabled = payloadEnabled();
        PayloadSnapshot requestPayload = payloadEnabled
                ? requestPayload(requestContext, inputMaxLength())
                : PayloadSnapshot.disabled();

        Span span = Span.current();
        span.setAttribute("rest_client.request.method", requestContext.getMethod());
        span.setAttribute("rest_client.url", uri.toString());
        span.setAttribute("rest_client.url.path", uri.getPath());
        span.setAttribute("rest_client.class", invocation.className());
        span.setAttribute("rest_client.operation", invocation.methodName());
        span.setAttribute("rest_client.payload.enabled", payloadEnabled);
        putPayloadAttributes(span, "rest_client.request", requestPayload);
        span.addEvent("mtr.rest-client.request.enviada", Attributes.builder()
                .put("http.request.method", requestContext.getMethod())
                .put("url.full", uri.toString())
                .put("rest_client.class", invocation.className())
                .put("rest_client.operation", invocation.methodName())
                .build());

        ObservabilityLog.info(
                LOG,
                "mtr.rest-client.request.enviada",
                ObservabilityLog.fields(
                        "camada", "infrastructure",
                        "componente", "RestClientObservabilityFilter",
                        "rest_client", invocation.className(),
                        "operacao", invocation.methodName(),
                        "http_method", requestContext.getMethod(),
                        "url", uri.toString(),
                        "url_path", uri.getPath(),
                        "payload_habilitado", payloadEnabled,
                        "request_body", requestPayload.body(),
                        "request_body_tamanho", requestPayload.originalLength(),
                        "request_body_truncado", requestPayload.truncated(),
                        "request_body_limite", requestPayload.limit()
                )
        );
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        InvocationMetadata invocation = invocationMetadata(requestContext);
        URI uri = requestContext.getUri();
        long durationMillis = durationMillis(requestContext);
        boolean payloadEnabled = payloadEnabled();
        PayloadSnapshot responsePayload = payloadEnabled
                ? responsePayload(responseContext, outputMaxLength())
                : PayloadSnapshot.disabled();

        Span span = Span.current();
        span.setAttribute("rest_client.response.status_code", responseContext.getStatus());
        span.setAttribute("rest_client.duration_ms", durationMillis);
        putPayloadAttributes(span, "rest_client.response", responsePayload);
        span.addEvent("mtr.rest-client.response.recebida", Attributes.builder()
                .put("http.response.status_code", responseContext.getStatus())
                .put("rest_client.duration_ms", durationMillis)
                .put("url.full", uri.toString())
                .put("rest_client.class", invocation.className())
                .put("rest_client.operation", invocation.methodName())
                .build());

        ObservabilityLog.info(
                LOG,
                "mtr.rest-client.response.recebida",
                ObservabilityLog.fields(
                        "camada", "infrastructure",
                        "componente", "RestClientObservabilityFilter",
                        "rest_client", invocation.className(),
                        "operacao", invocation.methodName(),
                        "http_method", requestContext.getMethod(),
                        "url", uri.toString(),
                        "url_path", uri.getPath(),
                        "status_http", responseContext.getStatus(),
                        "duracao_ms", durationMillis,
                        "payload_habilitado", payloadEnabled,
                        "response_body", responsePayload.body(),
                        "response_body_tamanho", responsePayload.originalLength(),
                        "response_body_truncado", responsePayload.truncated(),
                        "response_body_limite", responsePayload.limit()
                )
        );
    }

    private static void putPayloadAttributes(Span span, String prefix, PayloadSnapshot payload) {
        span.setAttribute(prefix + ".body.present", payload.present());
        span.setAttribute(prefix + ".body.truncated", payload.truncated());
        span.setAttribute(prefix + ".body.length", payload.originalLength());
        span.setAttribute(prefix + ".body.limit", payload.limit());

        if (payload.body() != null) {
            span.setAttribute(prefix + ".body", payload.body());
        }
    }

    private static PayloadSnapshot requestPayload(ClientRequestContext requestContext, int limit) {
        if (!requestContext.hasEntity()) {
            return PayloadSnapshot.empty(limit);
        }

        return truncate(serializeEntity(requestContext.getEntity()), limit);
    }

    private static String serializeEntity(Object entity) {
        if (entity == null) {
            return null;
        }
        if (entity instanceof String value) {
            return value;
        }
        if (entity instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (entity instanceof InputStream) {
            return "[stream nao logado]";
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            return String.valueOf(entity);
        }
    }

    private static PayloadSnapshot responsePayload(ClientResponseContext responseContext, int limit) {
        if (!responseContext.hasEntity() || responseContext.getEntityStream() == null) {
            return PayloadSnapshot.empty(limit);
        }

        try {
            byte[] bytes = responseContext.getEntityStream().readAllBytes();
            responseContext.setEntityStream(new ByteArrayInputStream(bytes));

            if (bytes.length == 0) {
                return PayloadSnapshot.empty(limit);
            }

            return truncate(new String(bytes, charset(responseContext.getMediaType())), limit);
        } catch (IOException e) {
            return truncate("[erro ao ler payload de resposta do REST Client: " + e.getMessage() + "]", limit);
        }
    }

    private static Charset charset(MediaType mediaType) {
        if (mediaType == null || mediaType.getParameters() == null) {
            return StandardCharsets.UTF_8;
        }

        String charset = mediaType.getParameters().get("charset");
        if (charset == null || charset.isBlank()) {
            return StandardCharsets.UTF_8;
        }

        try {
            return Charset.forName(charset);
        } catch (RuntimeException e) {
            return StandardCharsets.UTF_8;
        }
    }

    private static PayloadSnapshot truncate(String payload, int limit) {
        int normalizedLimit = Math.max(0, limit);

        if (payload == null) {
            return PayloadSnapshot.empty(normalizedLimit);
        }

        String sanitizedPayload = sanitizePayload(payload);
        int originalLength = sanitizedPayload.length();
        if (originalLength <= normalizedLimit) {
            return new PayloadSnapshot(sanitizedPayload, true, originalLength, false, normalizedLimit);
        }

        return new PayloadSnapshot(sanitizedPayload.substring(0, normalizedLimit), true, originalLength, true, normalizedLimit);
    }

    static String sanitizePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return payload;
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(payload);
            JsonNode sanitizedRoot = root.deepCopy();
            if (maskSensitiveFields(sanitizedRoot)) {
                return OBJECT_MAPPER.writeValueAsString(sanitizedRoot);
            }
            return payload;
        } catch (RuntimeException | JsonProcessingException e) {
            return SENSITIVE_JSON_STRING_FIELD_PATTERN.matcher(payload)
                    .replaceAll("$1\"" + MASKED_VALUE + "\"");
        }
    }

    private static boolean maskSensitiveFields(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            boolean masked = false;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitiveField(field.getKey())) {
                    objectNode.put(field.getKey(), MASKED_VALUE);
                    masked = true;
                } else {
                    masked |= maskSensitiveFields(field.getValue());
                }
            }
            return masked;
        }

        if (node instanceof ArrayNode arrayNode) {
            boolean masked = false;
            for (JsonNode item : arrayNode) {
                masked |= maskSensitiveFields(item);
            }
            return masked;
        }

        return false;
    }

    private static boolean isSensitiveField(String fieldName) {
        return fieldName != null && SENSITIVE_FIELD_NAMES.contains(fieldName.toLowerCase());
    }

    private static InvocationMetadata invocationMetadata(ClientRequestContext requestContext) {
        Object invokedMethod = requestContext.getProperty(INVOKED_METHOD_PROPERTY);
        if (invokedMethod instanceof Method method) {
            return new InvocationMetadata(method.getDeclaringClass().getSimpleName(), method.getName());
        }
        return new InvocationMetadata("desconhecido", "desconhecida");
    }

    private static long durationMillis(ClientRequestContext requestContext) {
        Object startNano = requestContext.getProperty(START_NANO_PROPERTY);
        if (startNano instanceof Long start) {
            return Math.max(0, (System.nanoTime() - start) / 1_000_000);
        }
        return 0;
    }

    private static boolean payloadEnabled() {
        try {
            return ConfigProvider.getConfig()
                    .getOptionalValue(PAYLOAD_ENABLED_PROPERTY, Boolean.class)
                    .orElse(DEFAULT_PAYLOAD_ENABLED);
        } catch (RuntimeException e) {
            return DEFAULT_PAYLOAD_ENABLED;
        }
    }

    private static int inputMaxLength() {
        return configuredLimit(INPUT_MAX_LENGTH_PROPERTY, DEFAULT_INPUT_MAX_LENGTH);
    }

    private static int outputMaxLength() {
        return configuredLimit(OUTPUT_MAX_LENGTH_PROPERTY, DEFAULT_OUTPUT_MAX_LENGTH);
    }

    private static int configuredLimit(String propertyName, int defaultValue) {
        try {
            return ConfigProvider.getConfig()
                    .getOptionalValue(propertyName, Integer.class)
                    .map(value -> Math.max(0, value))
                    .orElse(defaultValue);
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    private record InvocationMetadata(String className, String methodName) {
    }

    private record PayloadSnapshot(
            String body,
            boolean present,
            int originalLength,
            boolean truncated,
            int limit
    ) {

        private static PayloadSnapshot empty(int limit) {
            return new PayloadSnapshot(null, false, 0, false, Math.max(0, limit));
        }

        private static PayloadSnapshot disabled() {
            return new PayloadSnapshot(null, false, 0, false, 0);
        }
    }
}
