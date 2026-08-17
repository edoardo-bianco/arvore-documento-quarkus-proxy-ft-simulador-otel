package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.tracing.TracingPolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.ContextResolver;

@ApplicationScoped
public class CapturaDossieProdutoMtrTracingProvider
        implements ContextResolver<HttpClientOptions>, ClientRequestFilter {

    private static final TextMapSetter<MultivaluedMap<String, Object>> HEADER_SETTER =
            MultivaluedMap::putSingle;

    private final OpenTelemetry openTelemetry;

    @Inject
    public CapturaDossieProdutoMtrTracingProvider(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public HttpClientOptions getContext(Class<?> type) {
        if (HttpClientOptions.class.equals(type)) {
            return new HttpClientOptions().setTracingPolicy(TracingPolicy.IGNORE);
        }
        return null;
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        openTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(Context.current(), requestContext.getHeaders(), HEADER_SETTER);
    }
}
