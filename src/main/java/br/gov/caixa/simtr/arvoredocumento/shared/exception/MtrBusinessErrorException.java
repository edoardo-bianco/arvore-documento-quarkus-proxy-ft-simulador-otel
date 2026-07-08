package br.gov.caixa.simtr.arvoredocumento.shared.exception;

import br.gov.caixa.simtr.arvoredocumento.api.dto.erro.ErroPadraoDto;

public class MtrBusinessErrorException extends MtrClientErrorException {

    public MtrBusinessErrorException(int status, ErroPadraoDto erro) {
        super(status, erro, MtrErrorType.NEGOCIO);
    }
}
