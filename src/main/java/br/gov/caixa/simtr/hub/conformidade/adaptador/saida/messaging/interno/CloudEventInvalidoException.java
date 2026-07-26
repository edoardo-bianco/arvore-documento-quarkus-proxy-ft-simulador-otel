package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

/**
 * Sinaliza falha controlada no envelope interno sem incorporar o payload à mensagem.
 */
final class CloudEventInvalidoException extends IllegalArgumentException {

    CloudEventInvalidoException(String mensagem) {
        super(mensagem);
    }

    CloudEventInvalidoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
