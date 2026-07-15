package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client;

import br.gov.caixa.simtr.hub.arquitetura.observabilidade.RestClientObservabilityFilter;
import br.gov.caixa.simtr.hub.arquitetura.seguranca.RequestHeaderFactory;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v1.validacaonegocial.ValidacaoNegocialDossieProdutoMtrRequest;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.ValidacaoNegocialDossieProdutoMtrException;
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
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
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
@RegisterProvider(RestClientObservabilityFilter.class)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface ValidacaoNegocialDossieProdutoMtrClient {

    @PATCH
    @Path("/v1/dossie-produto/{id}/validacao-negocial")
    @Timeout(value = 2_000, unit = ChronoUnit.MILLIS)
    @Retry(maxRetries = 3, delay = 300, delayUnit = ChronoUnit.MILLIS,
            jitter = 100, jitterDelayUnit = ChronoUnit.MILLIS,
            retryOn = {ValidacaoNegocialDossieProdutoMtrException.Servidor.class,
                    ProcessingException.class, TimeoutException.class},
            abortOn = {ValidacaoNegocialDossieProdutoMtrException.Negocio.class,
                    ValidacaoNegocialDossieProdutoMtrException.TecnicaCliente.class})
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5,
            delay = 10_000, delayUnit = ChronoUnit.MILLIS, successThreshold = 2,
            failOn = {ValidacaoNegocialDossieProdutoMtrException.Servidor.class,
                    ProcessingException.class, TimeoutException.class},
            skipOn = {ValidacaoNegocialDossieProdutoMtrException.Negocio.class,
                    ValidacaoNegocialDossieProdutoMtrException.TecnicaCliente.class})
    Uni<Void> registrar(
            @PathParam("id") Long identificadorDossieProduto,
            ValidacaoNegocialDossieProdutoMtrRequest requisicao);

    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        if (response.getStatus() < 400) {
            return null;
        }
        int status = response.getStatus();
        ValidacaoNegocialDossieProdutoMtrException.Erro erro = lerErro(response);
        if (status >= 500) {
            return new ValidacaoNegocialDossieProdutoMtrException.Servidor(status, erro);
        }
        if (status == 400 || status == 404 || status == 409 || status == 422) {
            return new ValidacaoNegocialDossieProdutoMtrException.Negocio(status, erro);
        }
        return new ValidacaoNegocialDossieProdutoMtrException.TecnicaCliente(status, erro);
    }

    private static ValidacaoNegocialDossieProdutoMtrException.Erro lerErro(Response response) {
        try {
            if (response.hasEntity()) {
                ValidacaoNegocialDossieProdutoMtrException.Erro erro = response.readEntity(
                        ValidacaoNegocialDossieProdutoMtrException.Erro.class);
                if (erro != null) {
                    return new ValidacaoNegocialDossieProdutoMtrException.Erro(
                            erro.codigoHttp() != null ? erro.codigoHttp() : response.getStatus(),
                            erro.recurso() != null ? erro.recurso() : "simtr-dossie-produto",
                            erro.idErro() != null ? erro.idErro() : UUID.randomUUID().toString(),
                            erro.codigoErro(), erro.erros(), erro.detalhe(), erro.stacktrace());
                }
            }
        } catch (RuntimeException ignored) {
            // Resposta externa fora do contrato: preservar o fallback publico existente.
        }
        return new ValidacaoNegocialDossieProdutoMtrException.Erro(
                response.getStatus(), "simtr-dossie-produto", UUID.randomUUID().toString(),
                "ARVDOCP0002",
                List.of(new ValidacaoNegocialDossieProdutoMtrException.Mensagem(
                        "Erro retornado pelo serviço MTR fora do contrato esperado.")),
                null, null);
    }
}
