package br.gov.caixa.simtr.arvoredocumento.infrastructure.client.gestaodocumento;

import br.gov.caixa.simtr.arvoredocumento.api.dto.gestaodocumento.GestaoDocumentoCredencialContainerDto;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.RequestHeaderFactory;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.RestClientObservabilityFilter;
import br.gov.caixa.simtr.arvoredocumento.shared.exception.MtrBusinessErrorException;
import br.gov.caixa.simtr.arvoredocumento.shared.exception.MtrClientTechnicalException;
import br.gov.caixa.simtr.arvoredocumento.shared.exception.MtrServerErrorException;
import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
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

@RegisterRestClient(configKey = "gestao-documento")
@Path("/gestao-documento")
@RegisterClientHeaders(RequestHeaderFactory.class)
@RegisterProvider(OidcClientRequestReactiveFilter.class)
@RegisterProvider(RestClientObservabilityFilter.class)
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.APPLICATION_JSON)
public interface GestaoDocumentoClient {

    @POST
    @Path("/v1/storage/container/credencial")
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
    Uni<GestaoDocumentoCredencialContainerDto> gerarCredencialContainer();

    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        return GestaoDocumentoClientExceptionMapper.toException(response);
    }
}
