package br.gov.caixa.simtr.arvoredocumento.shared.exception;

import br.gov.caixa.simtr.arvoredocumento.api.dto.erro.ErroPadraoDto;

public class MtrClientTechnicalException extends MtrClientErrorException {

    public MtrClientTechnicalException(int status, ErroPadraoDto erro) {
        super(status, erro, MtrErrorType.TECNICO_CLIENTE);
    }
}
