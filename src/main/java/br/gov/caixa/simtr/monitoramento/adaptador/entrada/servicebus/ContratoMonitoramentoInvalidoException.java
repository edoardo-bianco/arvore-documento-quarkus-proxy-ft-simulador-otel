package br.gov.caixa.simtr.monitoramento.adaptador.entrada.servicebus;

import java.io.Serial;

public final class ContratoMonitoramentoInvalidoException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String idErro;
    private final String codigoErro;

    public ContratoMonitoramentoInvalidoException(String idErro, String codigoErro) {
        super("Contrato de monitoramento invalido.");
        this.idErro = idErro;
        this.codigoErro = codigoErro;
    }

    public String idErro() {
        return idErro;
    }

    public String codigoErro() {
        return codigoErro;
    }
}
