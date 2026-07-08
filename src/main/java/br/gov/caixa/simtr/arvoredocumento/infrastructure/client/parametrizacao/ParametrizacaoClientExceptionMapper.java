package br.gov.caixa.simtr.arvoredocumento.infrastructure.client.parametrizacao;

import br.gov.caixa.simtr.arvoredocumento.api.dto.erro.ErroPadraoDto;
import br.gov.caixa.simtr.arvoredocumento.shared.exception.MtrClientErrorException;
import br.gov.caixa.simtr.arvoredocumento.shared.exception.MtrServerErrorException;
import jakarta.ws.rs.core.Response;

final class ParametrizacaoClientExceptionMapper {

    private static final String RECURSO = "simtr-parametrizacao";

    private ParametrizacaoClientExceptionMapper() {
    }

    static RuntimeException toException(Response response) {
        if (response.getStatus() < 400) {
            return null;
        }

        ErroPadraoDto erro = ClientErrorBodyReader.read(response, RECURSO);

        if (response.getStatus() >= 500) {
            return new MtrServerErrorException(response.getStatus(), erro);
        }

        return new MtrClientErrorException(response.getStatus(), erro);
    }
}
