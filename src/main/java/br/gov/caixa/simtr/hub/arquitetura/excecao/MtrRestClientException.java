package br.gov.caixa.simtr.hub.arquitetura.excecao;

import br.gov.caixa.simtr.hub.arquitetura.excecao.dto.ErroPadraoDto;

public abstract class MtrRestClientException extends RuntimeException {

    private final int status;
    private final ErroPadraoDto erro;
    private final MtrErrorType tipoErro;

    protected MtrRestClientException(int status, ErroPadraoDto erro) {
        this(status, erro, MtrErrorType.TECNICO_CLIENTE);
    }

    protected MtrRestClientException(int status, ErroPadraoDto erro, MtrErrorType tipoErro) {
        super(resolveMessage(erro));
        this.status = status;
        this.erro = erro;
        this.tipoErro = tipoErro != null ? tipoErro : MtrErrorType.TECNICO_CLIENTE;
    }

    public int status() {
        return status;
    }

    public ErroPadraoDto erro() {
        return erro;
    }

    public MtrErrorType tipoErro() {
        return tipoErro;
    }

    private static String resolveMessage(ErroPadraoDto erro) {
        if (erro == null || erro.erros() == null || erro.erros().isEmpty()) {
            return "Erro retornado pelo serviço MTR";
        }
        return erro.erros().get(0).mensagem();
    }
}
