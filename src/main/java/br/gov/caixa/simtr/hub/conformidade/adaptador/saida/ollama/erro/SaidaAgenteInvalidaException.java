package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.erro;

public final class SaidaAgenteInvalidaException extends RuntimeException {

    public SaidaAgenteInvalidaException() {
        super("Saída estruturada do agente inválida");
    }

    public SaidaAgenteInvalidaException(Throwable causa) {
        super("Saída estruturada do agente inválida", causa);
    }
}
