package br.gov.caixa.simtr.arvoredocumento.shared.exception;

import br.gov.caixa.simtr.arvoredocumento.api.dto.erro.ErroPadraoDto;

public class MtrClientErrorException extends MtrRestClientException {

    public MtrClientErrorException(int status, ErroPadraoDto erro) {
        super(status, erro);
    }
}
