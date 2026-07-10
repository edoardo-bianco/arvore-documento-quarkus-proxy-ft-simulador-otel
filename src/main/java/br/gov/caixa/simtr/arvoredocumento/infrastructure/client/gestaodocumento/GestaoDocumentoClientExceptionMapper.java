package br.gov.caixa.simtr.arvoredocumento.infrastructure.client.gestaodocumento;

import br.gov.caixa.simtr.arvoredocumento.api.dto.erro.ErroPadraoDto;
import br.gov.caixa.simtr.arvoredocumento.infrastructure.client.ClientErrorBodyReader;
import br.gov.caixa.simtr.arvoredocumento.shared.exception.MtrBusinessErrorException;
import br.gov.caixa.simtr.arvoredocumento.shared.exception.MtrClientTechnicalException;
import br.gov.caixa.simtr.arvoredocumento.shared.exception.MtrServerErrorException;
import jakarta.ws.rs.core.Response;

final class GestaoDocumentoClientExceptionMapper {

    private static final String RECURSO = "simtr-gestao-documento";

    private GestaoDocumentoClientExceptionMapper() {
    }

    static RuntimeException toException(Response response) {
        if (response.getStatus() < 400) {
            return null;
        }

        int status = response.getStatus();
        ErroPadraoDto erro = ClientErrorBodyReader.read(response, RECURSO);

        if (status >= 500) {
            return new MtrServerErrorException(status, erro);
        }

        if (isBusinessError(status)) {
            return new MtrBusinessErrorException(status, erro);
        }

        return new MtrClientTechnicalException(status, erro);
    }

    private static boolean isBusinessError(int status) {
        return status == Response.Status.BAD_REQUEST.getStatusCode()
                || status == Response.Status.NOT_FOUND.getStatusCode()
                || status == Response.Status.CONFLICT.getStatusCode()
                || status == 422;
    }
}
