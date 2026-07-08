package br.gov.caixa.simtr.arvoredocumento.infrastructure.client.parametrizacao;

import br.gov.caixa.simtr.arvoredocumento.api.dto.parametrizacao.checklist.ChecklistDto;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.RequestHeaderFactory;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.RestClientObservabilityFilter;
import br.gov.caixa.simtr.arvoredocumento.shared.exception.MtrBusinessErrorException;
import br.gov.caixa.simtr.arvoredocumento.shared.exception.MtrClientTechnicalException;
import br.gov.caixa.simtr.arvoredocumento.shared.exception.MtrServerErrorException;
import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
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

@RegisterRestClient(configKey = "parametrizacao-checklist")
@Path("/parametrizacao/v1/cadastro/checklist")
@RegisterClientHeaders(RequestHeaderFactory.class)
@RegisterProvider(OidcClientRequestReactiveFilter.class)
@RegisterProvider(RestClientObservabilityFilter.class)
@Produces(MediaType.APPLICATION_JSON)
public interface ParametrizacaoChecklistClient {

    @GET
    @Path("/identificador-negocial/{identificador}/versao/{versao}")
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
    Uni<ChecklistDto> consultarPorIdentificadorNegocialEVersao(
            @PathParam("identificador") Long identificador,
            @PathParam("versao") Integer versao);

    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        return ParametrizacaoClientExceptionMapper.toException(response);
    }
}
