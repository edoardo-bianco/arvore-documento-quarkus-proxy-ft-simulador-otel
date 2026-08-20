package br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.client;

import br.gov.caixa.simtr.hub.arquitetura.seguranca.RequestHeaderFactory;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v4.documentos.ConsultaDocumentosDossieProdutoMtrQuery;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.dto.v4.documentos.ConsultaDocumentosDossieProdutoMtrResponse;
import br.gov.caixa.simtr.hub.dossieproduto.adaptador.saida.mtr.erro.ConsultaDocumentosDossieProdutoMtrException;
import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.BeanParam;
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
import java.util.List;
import java.util.UUID;

@RegisterRestClient(configKey = "dossie-produto")
@Path("/dossie-produto")
@RegisterClientHeaders(RequestHeaderFactory.class)
@RegisterProvider(OidcClientRequestReactiveFilter.class)
@RegisterProvider(ConsultaDocumentosDossieProdutoMtrTracingProvider.class)
@Produces(MediaType.APPLICATION_JSON)
public interface ConsultaDocumentosDossieProdutoMtrClient {

    @GET
    @Path("/v4/dossie-produto/{id}/documentos")
    @Timeout(value = 2_000, unit = ChronoUnit.MILLIS)
    @Retry(
            maxRetries = 3,
            delay = 300,
            delayUnit = ChronoUnit.MILLIS,
            jitter = 100,
            jitterDelayUnit = ChronoUnit.MILLIS,
            retryOn = {
                    ConsultaDocumentosDossieProdutoMtrException.Servidor.class,
                    ProcessingException.class,
                    TimeoutException.class
            },
            abortOn = {
                    ConsultaDocumentosDossieProdutoMtrException.Negocio.class,
                    ConsultaDocumentosDossieProdutoMtrException.TecnicaCliente.class
            }
    )
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 10_000,
            delayUnit = ChronoUnit.MILLIS,
            successThreshold = 2,
            failOn = {
                    ConsultaDocumentosDossieProdutoMtrException.Servidor.class,
                    ProcessingException.class,
                    TimeoutException.class
            },
            skipOn = {
                    ConsultaDocumentosDossieProdutoMtrException.Negocio.class,
                    ConsultaDocumentosDossieProdutoMtrException.TecnicaCliente.class
            }
    )
    Uni<List<ConsultaDocumentosDossieProdutoMtrResponse>> consultar(
            @PathParam("id") Long identificador,
            @BeanParam ConsultaDocumentosDossieProdutoMtrQuery query);

    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        if (response.getStatus() < 400) {
            return null;
        }

        int status = response.getStatus();
        ConsultaDocumentosDossieProdutoMtrException.Erro erro = lerErro(response);
        if (status >= 500) {
            return new ConsultaDocumentosDossieProdutoMtrException.Servidor(status, erro);
        }
        if (status == 400 || status == 404) {
            return new ConsultaDocumentosDossieProdutoMtrException.Negocio(status, erro);
        }
        return new ConsultaDocumentosDossieProdutoMtrException.TecnicaCliente(status, erro);
    }

    private static ConsultaDocumentosDossieProdutoMtrException.Erro lerErro(Response response) {
        try {
            if (response.hasEntity()) {
                ConsultaDocumentosDossieProdutoMtrException.Erro erro = response.readEntity(
                        ConsultaDocumentosDossieProdutoMtrException.Erro.class);
                if (erro != null) {
                    return normalizarIdentidadeErro(response.getStatus(), erro);
                }
            }
        } catch (RuntimeException _) {
            // Resposta externa fora do contrato: usar somente o fallback seguro.
        }

        return new ConsultaDocumentosDossieProdutoMtrException.Erro(
                response.getStatus(),
                "simtr-dossie-produto",
                UUID.randomUUID().toString(),
                "ARVDOCP0002",
                List.of(new ConsultaDocumentosDossieProdutoMtrException.Mensagem(
                        "Erro retornado pelo serviço MTR fora do contrato esperado.")),
                null,
                null);
    }

    private static ConsultaDocumentosDossieProdutoMtrException.Erro normalizarIdentidadeErro(
            int status,
            ConsultaDocumentosDossieProdutoMtrException.Erro erro
    ) {
        if (erro.codigoHttp() != null && erro.recurso() != null && erro.idErro() != null) {
            return erro;
        }
        return new ConsultaDocumentosDossieProdutoMtrException.Erro(
                erro.codigoHttp() != null ? erro.codigoHttp() : status,
                erro.recurso() != null ? erro.recurso() : "simtr-dossie-produto",
                erro.idErro() != null ? erro.idErro() : UUID.randomUUID().toString(),
                erro.codigoErro(),
                erro.erros(),
                erro.detalhe(),
                erro.stacktrace());
    }
}
