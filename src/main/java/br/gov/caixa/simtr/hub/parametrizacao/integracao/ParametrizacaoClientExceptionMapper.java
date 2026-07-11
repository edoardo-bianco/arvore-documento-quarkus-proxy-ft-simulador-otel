package br.gov.caixa.simtr.hub.parametrizacao.integracao;

import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;
import br.gov.caixa.simtr.hub.arquitetura.excecao.ClientErrorBodyReader;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrBusinessErrorException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrClientTechnicalException;
import br.gov.caixa.simtr.hub.arquitetura.excecao.MtrServerErrorException;
import jakarta.ws.rs.core.Response;

final class ParametrizacaoClientExceptionMapper {

    private static final String RECURSO = "simtr-parametrizacao";

    private ParametrizacaoClientExceptionMapper() {
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
