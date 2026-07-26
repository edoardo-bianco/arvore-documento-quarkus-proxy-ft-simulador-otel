package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.erro;

public final class FalhaTecnicaAgenteTransitoriaException extends RuntimeException {

    public FalhaTecnicaAgenteTransitoriaException(Throwable causa) {
        super("Falha técnica transitória na análise automática", causa);
    }
}
