package br.gov.caixa.simtr.hub.arquitetura.excecao;

import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;

public class MtrClientErrorException extends MtrRestClientException {

    public MtrClientErrorException(int status, ErroPadraoDto erro) {
        super(status, erro);
    }

    protected MtrClientErrorException(int status, ErroPadraoDto erro, MtrErrorType tipoErro) {
        super(status, erro, tipoErro);
    }
}
