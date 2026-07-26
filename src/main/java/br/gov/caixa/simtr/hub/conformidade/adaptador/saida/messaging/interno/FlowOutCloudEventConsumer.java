package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.CompletionStage;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

@ApplicationScoped
public class FlowOutCloudEventConsumer {

    private static final String CANAL_FLOW_OUT = "flow-out";

    private final CloudEventMapper mapper;
    private final RegistrarEventoFlowOut registrar;

    @Inject
    FlowOutCloudEventConsumer(CloudEventMapper mapper, RegistrarEventoFlowOut registrar) {
        this.mapper = mapper;
        this.registrar = registrar;
    }

    @Incoming(CANAL_FLOW_OUT)
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    public CompletionStage<Void> consumir(Message<byte[]> mensagem) {
        try {
            EventoFlowOutRecebido evento = mapper.lerEventoSaida(mensagem.getPayload());
            registrar.registrar(evento);
        } catch (RuntimeException falha) {
            return mensagem.nack(falha);
        }
        return mensagem.ack();
    }
}
