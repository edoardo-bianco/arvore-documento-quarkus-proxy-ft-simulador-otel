package br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus;

import java.io.Serial;

/** Falha local de mapeamento com id/codigo do log, sem payload ou causa original. */
public final class MapeamentoReagendamentoException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String idErro;
    private final String codigoErro;

    MapeamentoReagendamentoException(String idErro, String codigoErro, String mensagem) {
        super(mensagem);
        this.idErro = idErro;
        this.codigoErro = codigoErro;
    }

    /** @return identificador unico da ocorrencia registrada */
    public String idErro() {
        return idErro;
    }

    /** @return classificacao estavel da falha nesta borda */
    public String codigoErro() {
        return codigoErro;
    }
}
