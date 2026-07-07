package br.gov.caixa.simtr.arvoredocumento.api.exception;

import br.gov.caixa.simtr.arvoredocumento.shared.exception.MtrRestClientException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class MtrRestClientExceptionMapper implements ExceptionMapper<MtrRestClientException> {

    @Override
    public Response toResponse(MtrRestClientException exception) {
        return Response.status(exception.status())
                .entity(exception.erro())
                .build();
    }
}
