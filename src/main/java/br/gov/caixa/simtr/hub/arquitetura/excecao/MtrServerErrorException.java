package br.gov.caixa.simtr.hub.arquitetura.excecao;

import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;

public class MtrServerErrorException extends MtrRestClientException {

    public MtrServerErrorException(int status, ErroPadraoDto erro) {
        super(status, erro, MtrErrorType.TECNICO_SERVIDOR);
    }
}
