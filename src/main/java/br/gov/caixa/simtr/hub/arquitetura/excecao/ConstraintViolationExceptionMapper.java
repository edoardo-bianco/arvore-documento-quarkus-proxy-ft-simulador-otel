package br.gov.caixa.simtr.hub.arquitetura.excecao;

import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroMensagemDto;
import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import java.util.UUID;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<ErroMensagemDto> erros = exception.getConstraintViolations()
                .stream()
                .map(violacao -> new ErroMensagemDto(violacao.getMessage()))
                .toList();

        ErroPadraoDto erro = new ErroPadraoDto(
                Response.Status.BAD_REQUEST.getStatusCode(),
                "simtr-hub",
                UUID.randomUUID().toString(),
                "ARVDOCP0001",
                erros,
                null,
                null
        );

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(erro)
                .build();
    }
}
