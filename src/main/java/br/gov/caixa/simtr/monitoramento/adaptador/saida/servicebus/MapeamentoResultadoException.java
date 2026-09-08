package br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus;

import java.io.Serial;

/** Falha local do resultado com id/codigo do log, sem transportar payload ou causa original. */
public final class MapeamentoResultadoException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String idErro;
    private final String codigoErro;

    MapeamentoResultadoException(String idErro, String codigoErro, String mensagem) {
        super(mensagem);
        this.idErro = idErro;
        this.codigoErro = codigoErro;
    }

    /** @return identidade da ocorrencia registrada nesta borda */
    public String idErro() {
        return idErro;
    }

    /** @return codigo estavel da classificacao local */
    public String codigoErro() {
        return codigoErro;
    }
}
