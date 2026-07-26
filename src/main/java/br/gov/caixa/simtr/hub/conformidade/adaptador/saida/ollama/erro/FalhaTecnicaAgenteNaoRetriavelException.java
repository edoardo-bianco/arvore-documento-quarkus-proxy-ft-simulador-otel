package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.ollama.erro;

public final class FalhaTecnicaAgenteNaoRetriavelException extends RuntimeException {

    public FalhaTecnicaAgenteNaoRetriavelException(Throwable causa) {
        super("Falha técnica não transitória na análise automática", causa);
    }
}
