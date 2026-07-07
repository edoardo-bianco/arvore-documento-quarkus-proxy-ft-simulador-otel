package br.gov.caixa.simtr.arvoredocumento.shared.exception;

import br.gov.caixa.simtr.arvoredocumento.api.dto.erro.ErroPadraoDto;

public abstract class MtrRestClientException extends RuntimeException {

    private final int status;
    private final ErroPadraoDto erro;

    protected MtrRestClientException(int status, ErroPadraoDto erro) {
        super(resolveMessage(erro));
        this.status = status;
        this.erro = erro;
    }

    public int status() {
        return status;
    }

    public ErroPadraoDto erro() {
        return erro;
    }

    private static String resolveMessage(ErroPadraoDto erro) {
        if (erro == null || erro.erros() == null || erro.erros().isEmpty()) {
            return "Erro retornado pelo serviço MTR";
        }
        return erro.erros().get(0).mensagem();
    }
}
