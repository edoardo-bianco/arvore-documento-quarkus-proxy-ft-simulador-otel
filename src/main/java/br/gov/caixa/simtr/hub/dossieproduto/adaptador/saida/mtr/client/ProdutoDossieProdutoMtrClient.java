package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.RestClientObservabilityFilter;
import br.gov.caixa.simtr.hub.arquitetura.seguranca.RequestHeaderFactory;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.produto.ProdutoDossieProdutoMtrRequest;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.ProdutoDossieProdutoMtrException;
import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@RegisterRestClient(configKey = "dossie-produto")
@Path("/dossie-produto")
@RegisterClientHeaders(RequestHeaderFactory.class)
@RegisterProvider(OidcClientRequestReactiveFilter.class)
@RegisterProvider(RestClientObservabilityFilter.class)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface ProdutoDossieProdutoMtrClient {

    @PATCH
    @Path("/v1/dossie-produto/{id}/produto")
    @Timeout(value = 2_000, unit = ChronoUnit.MILLIS)
    @Retry(maxRetries = 3, delay = 300, delayUnit = ChronoUnit.MILLIS,
            jitter = 100, jitterDelayUnit = ChronoUnit.MILLIS,
            retryOn = {ProdutoDossieProdutoMtrException.Servidor.class,
                    ProcessingException.class, TimeoutException.class},
            abortOn = {ProdutoDossieProdutoMtrException.Negocio.class,
                    ProdutoDossieProdutoMtrException.TecnicaCliente.class})
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5,
            delay = 10_000, delayUnit = ChronoUnit.MILLIS, successThreshold = 2,
            failOn = {ProdutoDossieProdutoMtrException.Servidor.class,
                    ProcessingException.class, TimeoutException.class},
            skipOn = {ProdutoDossieProdutoMtrException.Negocio.class,
                    ProdutoDossieProdutoMtrException.TecnicaCliente.class})
    Uni<Void> alterar(
            @PathParam("id") Long identificadorDossieProduto,
            List<ProdutoDossieProdutoMtrRequest> requisicao);

    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        if (response.getStatus() < 400) {
            return null;
        }
        int status = response.getStatus();
        ProdutoDossieProdutoMtrException.Erro erro = lerErro(response);
        if (status >= 500) {
            return new ProdutoDossieProdutoMtrException.Servidor(status, erro);
        }
        if (status == 400 || status == 404 || status == 409 || status == 422) {
            return new ProdutoDossieProdutoMtrException.Negocio(status, erro);
        }
        return new ProdutoDossieProdutoMtrException.TecnicaCliente(status, erro);
    }

    private static ProdutoDossieProdutoMtrException.Erro lerErro(Response response) {
        try {
            if (response.hasEntity()) {
                ProdutoDossieProdutoMtrException.Erro erro = response.readEntity(
                        ProdutoDossieProdutoMtrException.Erro.class);
                if (erro != null) {
                    return new ProdutoDossieProdutoMtrException.Erro(
                            erro.codigoHttp() != null ? erro.codigoHttp() : response.getStatus(),
                            erro.recurso() != null ? erro.recurso() : "simtr-dossie-produto",
                            erro.idErro() != null ? erro.idErro() : UUID.randomUUID().toString(),
                            erro.codigoErro(), erro.erros(), erro.detalhe(), erro.stacktrace());
                }
            }
        } catch (RuntimeException _) {
            // Resposta externa fora do contrato: preservar o fallback publico existente.
        }
        return new ProdutoDossieProdutoMtrException.Erro(
                response.getStatus(), "simtr-dossie-produto", UUID.randomUUID().toString(),
                "ARVDOCP0002",
                List.of(new ProdutoDossieProdutoMtrException.Mensagem(
                        "Erro retornado pelo serviço MTR fora do contrato esperado.")),
                null, null);
    }
}
