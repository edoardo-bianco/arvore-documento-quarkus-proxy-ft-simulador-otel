package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

import io.quarkiverse.flow.messaging.FlowMessagingConsumer;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.CompletionStage;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

/**
 * Shim temporário para registrar a entrada da ponte do Flow 0.10.2 sem connector.
 */
@ApplicationScoped
public class FlowMessagingConsumerSemConnector extends FlowMessagingConsumer {

    @Override
    @Incoming("flow-in")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    public CompletionStage<Void> onIncoming(Message<byte[]> mensagem) {
        return super.onIncoming(mensagem);
    }
}
