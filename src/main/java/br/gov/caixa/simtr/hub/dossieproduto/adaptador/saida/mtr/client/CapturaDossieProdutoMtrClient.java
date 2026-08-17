package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client;

import br.gov.caixa.simtr.hub.arquitetura.seguranca.RequestHeaderFactory;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.captura.CapturaDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.CapturaDossieProdutoMtrException;
import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RegisterRestClient(configKey = "dossie-produto")
@Path("/dossie-produto")
@RegisterClientHeaders(RequestHeaderFactory.class)
@RegisterProvider(OidcClientRequestReactiveFilter.class)
@RegisterProvider(CapturaDossieProdutoMtrTracingProvider.class)
@Produces(MediaType.APPLICATION_JSON)
public interface CapturaDossieProdutoMtrClient {

    @POST
    @Path("/v1/dossie-produto/{id}/capturar")
    @Timeout(value = 2_000, unit = ChronoUnit.MILLIS)
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 10_000,
            delayUnit = ChronoUnit.MILLIS,
            successThreshold = 2,
            failOn = {
                    CapturaDossieProdutoMtrException.Servidor.class,
                    ProcessingException.class,
                    TimeoutException.class
            },
            skipOn = {
                    CapturaDossieProdutoMtrException.Negocio.class,
                    CapturaDossieProdutoMtrException.TecnicaCliente.class
            }
    )
    Uni<CapturaDossieProdutoMtrResponse> capturar(@PathParam("id") Long identificador);

    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        if (response.getStatus() < 400) {
            return null;
        }

        int status = response.getStatus();
        CapturaDossieProdutoMtrException.Erro erro = lerErro(response);
        if (status >= 500) {
            return new CapturaDossieProdutoMtrException.Servidor(status, erro);
        }
        if (status == 400 || status == 404 || status == 409 || status == 422) {
            return new CapturaDossieProdutoMtrException.Negocio(status, erro);
        }
        return new CapturaDossieProdutoMtrException.TecnicaCliente(status, erro);
    }

    private static CapturaDossieProdutoMtrException.Erro lerErro(Response response) {
        try {
            if (response.hasEntity()) {
                CapturaDossieProdutoMtrException.Erro erro =
                        response.readEntity(CapturaDossieProdutoMtrException.Erro.class);
                if (erro != null) {
                    return new CapturaDossieProdutoMtrException.Erro(
                            erro.codigoHttp() != null ? erro.codigoHttp() : response.getStatus(),
                            erro.recurso() != null ? erro.recurso() : "simtr-dossie-produto",
                            erro.idErro() != null ? erro.idErro() : UUID.randomUUID().toString(),
                            erro.codigoErro(),
                            erro.erros(),
                            erro.detalhe(),
                            erro.stacktrace());
                }
            }
        } catch (RuntimeException _) {
            // Resposta externa fora do contrato: usar somente o fallback seguro.
        }

        return new CapturaDossieProdutoMtrException.Erro(
                response.getStatus(),
                "simtr-dossie-produto",
                UUID.randomUUID().toString(),
                "ARVDOCP0002",
                List.of(new CapturaDossieProdutoMtrException.Mensagem(
                        "Erro retornado pelo serviço MTR fora do contrato esperado.")),
                null,
                null);
    }
}
