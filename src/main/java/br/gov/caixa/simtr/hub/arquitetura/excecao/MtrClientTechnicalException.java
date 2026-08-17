package br.gov.caixa.simtr.hub.arquitetura.excecao;

import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;

public class MtrClientTechnicalException extends MtrClientErrorException {

    public MtrClientTechnicalException(int status, ErroPadraoDto erro) {
        super(status, erro, MtrErrorType.TECNICO_CLIENTE);
    }

    public MtrClientTechnicalException(int status, ErroPadraoDto erro, String mensagemObservavel) {
        super(status, erro, MtrErrorType.TECNICO_CLIENTE, mensagemObservavel);
    }
}
