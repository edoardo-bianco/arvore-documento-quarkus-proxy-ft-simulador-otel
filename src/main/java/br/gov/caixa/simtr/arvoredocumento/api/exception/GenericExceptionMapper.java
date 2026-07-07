package br.gov.caixa.simtr.arvoredocumento.api.exception;

import br.gov.caixa.simtr.arvoredocumento.api.dto.erro.ErroMensagemDto;
import br.gov.caixa.simtr.arvoredocumento.api.dto.erro.ErroPadraoDto;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import java.util.UUID;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException webApplicationException) {
            return webApplicationException.getResponse();
        }

        ErroPadraoDto erro = new ErroPadraoDto(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "arvore-documento",
                UUID.randomUUID().toString(),
                "ARVDOCP9999",
                List.of(new ErroMensagemDto("Erro interno ao processar a requisição.")),
                null,
                null
        );

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(erro)
                .build();
    }
}
