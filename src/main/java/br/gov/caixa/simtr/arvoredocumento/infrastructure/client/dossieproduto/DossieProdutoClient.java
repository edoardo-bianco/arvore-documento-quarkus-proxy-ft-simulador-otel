package br.gov.caixa.simtr.arvoredocumento.infrastructure.client.dossieproduto;

import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriacaoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoCriadoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoDocumentoCriadoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoDocumentoInclusaoDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoFormularioDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.dossieproduto.DossieProdutoValidacaoNegocialDto;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.RequestHeaderFactory;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.RestClientObservabilityFilter;
import br.gov.caixa.simtr.arvoredocumento.shared.exception.MtrBusinessErrorException;
import br.gov.caixa.simtr.arvoredocumento.shared.exception.MtrClientTechnicalException;
import br.gov.caixa.simtr.arvoredocumento.shared.exception.MtrServerErrorException;
import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
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

@RegisterRestClient(configKey = "dossie-produto")
@Path("/dossie-produto")
@RegisterClientHeaders(RequestHeaderFactory.class)
@RegisterProvider(OidcClientRequestReactiveFilter.class)
@RegisterProvider(RestClientObservabilityFilter.class)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface DossieProdutoClient {

    @POST
    @Path("/v1/dossie-produto")
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
    Uni<DossieProdutoCriadoDto> criarDossieProduto(DossieProdutoCriacaoDto requisicao);

    @PATCH
    @Path("/v1/dossie-produto/{id}/formulario")
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
    Uni<DossieProdutoCriadoDto> atualizarFormularioDossieProduto(
            @PathParam("id") Long id,
            List<DossieProdutoFormularioDto> requisicao
    );

    @POST
    @Path("/v2/dossie-produto/{id}/documento")
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
    Uni<DossieProdutoDocumentoCriadoDto> incluirDocumentoDossieProduto(
            @PathParam("id") Long id,
            DossieProdutoDocumentoInclusaoDto requisicao
    );

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

    @POST
    @Path("/v1/dossie-produto/{id}/workflow")
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
    Uni<DossieProdutoCriadoDto> iniciarOuAvancarWorkflowDossieProduto(@PathParam("id") Long id);

    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        return DossieProdutoClientExceptionMapper.toException(response);
    }
}
