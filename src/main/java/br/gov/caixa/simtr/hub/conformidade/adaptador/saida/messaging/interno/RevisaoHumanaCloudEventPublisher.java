package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.PublicarRevisaoNoWorkflow;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;

@ApplicationScoped
public class RevisaoHumanaCloudEventPublisher implements PublicarRevisaoNoWorkflow {

    private static final String CANAL_FLOW_IN = "flow-in";

    private final MutinyEmitter<byte[]> emitter;
    private final CloudEventMapper mapper;
    private final ObjectMapper objectMapper;

    @Inject
    public RevisaoHumanaCloudEventPublisher(
            @Channel(CANAL_FLOW_IN) MutinyEmitter<byte[]> emitter,
            CloudEventMapper mapper,
            ObjectMapper objectMapper) {
        this.emitter = emitter;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Uni<Void> publicar(
            String instanceId,
            RevisaoHumanaConformidade revisao) {
        byte[] envelope = mapper.serializarRevisaoConcluida(
                instanceId,
                objectMapper.valueToTree(revisao));
        return emitter.sendMessage(Message.of(envelope));
    }
}
