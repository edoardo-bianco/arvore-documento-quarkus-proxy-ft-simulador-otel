package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
    public Uni<Void> consumir(Message<byte[]> mensagem) {
        return Uni.createFrom().deferred(() -> {
            EventoFlowOutRecebido evento = mapper.lerEventoSaida(mensagem.getPayload());
            return registrar.registrar(evento)
                    .chain(() -> Uni.createFrom().completionStage(mensagem::ack));
        }).onFailure().recoverWithUni(falha ->
                Uni.createFrom().completionStage(() -> mensagem.nack(falha)));
    }
}
