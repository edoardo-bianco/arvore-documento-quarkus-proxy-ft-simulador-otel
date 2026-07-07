package br.gov.caixa.simtr.arvoredocumento.api.exception;

import br.gov.caixa.simtr.arvoredocumento.api.dto.erro.ErroMensagemDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.erro.ErroPadraoDto;
import br.gov.caixa.simtr.arvoredocumento.shared.observability.ObservabilityLog;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class);
    private static final String CODIGO_ERRO_INTERNO = "ARVDOCP9999";

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException webApplicationException) {
            return webApplicationException.getResponse();
        }

        String idErro = UUID.randomUUID().toString();

        ObservabilityLog.error(
                LOG,
                "arvore-documento.erro.nao-tratado",
                exception,
                ObservabilityLog.fields(
                        "camada", "api",
                        "componente", "GenericExceptionMapper",
                        "id_erro", idErro,
                        "codigo_erro", CODIGO_ERRO_INTERNO,
                        "erro_tipo", exception.getClass().getName(),
                        "erro_mensagem", exception.getMessage(),
                        "resultado", "erro"
                )
        );

        ErroPadraoDto erro = new ErroPadraoDto(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "arvore-documento",
                idErro,
                CODIGO_ERRO_INTERNO,
                List.of(new ErroMensagemDto("Erro interno ao processar a requisição.")),
                null,
                null
        );

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(erro)
                .build();
    }
}
