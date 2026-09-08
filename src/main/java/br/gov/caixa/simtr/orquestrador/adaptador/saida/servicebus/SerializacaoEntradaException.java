package br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus;

import java.io.Serial;

public final class SerializacaoEntradaException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String idErro;
    private final String codigoErro;

    SerializacaoEntradaException(String idErro, String codigoErro) {
        super("Falha ao serializar a mensagem de entrada.");
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
