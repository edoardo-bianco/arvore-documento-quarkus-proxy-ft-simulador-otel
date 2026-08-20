package br.gov.caixa.simtr.hub.dossieproduto.adaptador.entrada.rest.v1;

import io.opentelemetry.api.trace.Span;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import java.util.regex.Pattern;

public class ConsultaDocumentosDossieProdutoTracingFilter {

    private static final Pattern ROTA_DOCUMENTOS = Pattern.compile(
            "^/?simtr-hub/v1/dossie-produto/[^/]+/documentos/?$");

    @ServerRequestFilter
    public void removerQueryDoSpanHttp(ContainerRequestContext requestContext) {
        if (HttpMethod.GET.equals(requestContext.getMethod())
                && ROTA_DOCUMENTOS.matcher(requestContext.getUriInfo().getPath()).matches()) {
            Span.current().setAttribute("url.query", "");
        }
    }
}
