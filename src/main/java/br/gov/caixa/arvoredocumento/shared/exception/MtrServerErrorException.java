package br.gov.caixa.arvoredocumento.shared.exception;

import br.gov.caixa.arvoredocumento.api.dto.erro.ErroPadraoDto;

public class MtrServerErrorException extends MtrRestClientException {

    public MtrServerErrorException(int status, ErroPadraoDto erro) {
        super(status, erro);
    }
}
