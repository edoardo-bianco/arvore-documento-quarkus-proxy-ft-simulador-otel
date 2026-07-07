package br.gov.caixa.arvoredocumento.infrastructure.client.parametrizacao;

import br.gov.caixa.arvoredocumento.api.dto.erro.ErroPadraoDto;
import br.gov.caixa.arvoredocumento.api.dto.parametrizacao.processo.ProcessoDto;
import br.gov.caixa.arvoredocumento.shared.exception.MtrClientErrorException;
import br.gov.caixa.arvoredocumento.shared.exception.MtrServerErrorException;
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
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;

import java.time.temporal.ChronoUnit;

@RegisterRestClient(configKey = "parametrizacao-processo")
@Path("/simtr-parametrizacao/v2/patriarca/processo")
@Produces(MediaType.APPLICATION_JSON)
public interface ParametrizacaoProcessoClient {

    @GET
    @Path("/identificador-negocial/{identificador}")
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
                    MtrClientErrorException.class
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
                    MtrClientErrorException.class
            }
    )
    Uni<ProcessoDto> consultarPorIdentificadorNegocial(@PathParam("identificador") Long identificador);

    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        if (response.getStatus() < 400) {
            return null;
        }

        ErroPadraoDto erro = ClientErrorBodyReader.read(response, "simtr-parametrizacao");

        if (response.getStatus() >= 500) {
            return new MtrServerErrorException(response.getStatus(), erro);
        }

        return new MtrClientErrorException(response.getStatus(), erro);
    }
}
