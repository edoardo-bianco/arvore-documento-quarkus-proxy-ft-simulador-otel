package br.gov.caixa.simtr.hub.arquitetura.excecao;

import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;

public class MtrBusinessErrorException extends MtrClientErrorException {

    public MtrBusinessErrorException(int status, ErroPadraoDto erro) {
        super(status, erro, MtrErrorType.NEGOCIO);
    }
}
