package br.gov.caixa.simtr.hub.dossieproduto.integracao;

import br.gov.caixa.simtr.hub.dossieproduto.recurso.rest.v1.dto.DossieProdutoValidacaoNegocialDto;
import br.gov.caixa.simtr.hub.arquitetura.seguranca.RequestHeaderFactory;
import br.gov.caixa.simtr.hub.arquitetura.observabilidade.RestClientObservabilityFilter;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
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

@RegisterRestClient(configKey = "dossie-produto")
@Path("/dossie-produto")
@RegisterClientHeaders(RequestHeaderFactory.class)
@RegisterProvider(OidcClientRequestReactiveFilter.class)
@RegisterProvider(RestClientObservabilityFilter.class)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface DossieProdutoClient {

    @PATCH
    @Path("/v1/dossie-produto/{id}/validacao-negocial")
    @Timeout(value = 2_000, unit = ChronoUnit.MILLIS)
    @Retry(
            maxRetries = 3,
            delay = 300,
            delayUnit = ChronoUnit.MILLIS,
            jitter = 100,
            jitterDelayUnit = ChronoUnit.MILLIS,
            retryOn = {
                    MtrServerErrorException.class,
                    ProcessingException.class,
                    TimeoutException.class
            },
            abortOn = {
                    MtrBusinessErrorException.class,
                    MtrClientTechnicalException.class
            }
    )
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 10_000,
            delayUnit = ChronoUnit.MILLIS,
            successThreshold = 2,
            failOn = {
                    MtrServerErrorException.class,
                    ProcessingException.class,
                    TimeoutException.class
            },
            skipOn = {
                    MtrBusinessErrorException.class,
                    MtrClientTechnicalException.class
            }
    )
    Uni<Void> registrarValidacaoNegocialDossieProduto(
            @PathParam("id") Long id,
            DossieProdutoValidacaoNegocialDto requisicao
    );

    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        return DossieProdutoClientExceptionMapper.toException(response);
    }
}
