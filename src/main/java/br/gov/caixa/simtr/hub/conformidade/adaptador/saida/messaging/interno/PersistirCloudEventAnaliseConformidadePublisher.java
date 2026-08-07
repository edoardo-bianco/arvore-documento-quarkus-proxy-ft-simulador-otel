package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

import br.gov.caixa.simtr.hub.conformidade.aplicacao.porta.saida.ArmazenarEstadoAnaliseConformidade;
import io.cloudevents.CloudEvent;
import io.serverlessworkflow.impl.events.EventPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class PersistirCloudEventAnaliseConformidadePublisher implements EventPublisher {

    private final ArmazenarEstadoAnaliseConformidade estados;
    private final CloudEventMapper mapper;

    @Inject
    PersistirCloudEventAnaliseConformidadePublisher(
            ArmazenarEstadoAnaliseConformidade estados,
            CloudEventMapper mapper) {
        this.estados = estados;
        this.mapper = mapper;
    }

    @Override
    public CompletableFuture<Void> publish(CloudEvent evento) {
        if (!mapper.ehEmissaoAnaliseConformidade(evento)) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            return estados.registrarEmissao(mapper.lerEmissaoReferencial(evento))
                    .subscribeAsCompletionStage()
                    .toCompletableFuture();
        } catch (RuntimeException falha) {
            return CompletableFuture.failedFuture(falha);
        }
    }

    @Override
    public void close() {
        // Sem recurso próprio: o ciclo de vida do store é administrado pelo CDI.
    }
}
