package br.gov.caixa.simtr.orquestrador.adaptador.saida.servicebus;

import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.FilaEntrada;
import br.gov.caixa.simtr.orquestrador.aplicacao.porta.saida.PublicarTentativaMonitoramento;
import br.gov.caixa.simtr.orquestrador.dominio.modelo.TentativaMonitoramento;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/** Publica pelo cliente da fabrica e so conclui depois da confirmacao do broker. */
@ApplicationScoped
public class MonitoramentoEntradaPublisher implements PublicarTentativaMonitoramento {

    private final Instance<ServiceBusSenderAsyncClient> sender;
    private final ObjectMapper json;

    @Inject
    public MonitoramentoEntradaPublisher(@FilaEntrada Instance<ServiceBusSenderAsyncClient> sender, ObjectMapper json) {
        this.sender = sender;
        this.json = json;
    }

    /** Uma invocacao compartilha a confirmacao; nao adiciona retry nem cria clientes. */
    @Override
    public Uni<Void> executar(TentativaMonitoramento tentativa) {
        return Uni.createFrom().item(() -> MonitoramentoEntradaServiceBusMapper.paraMensagem(tentativa, json))
                .onItem().transformToUni(mensagem -> Uni.createFrom().completionStage(
                        () -> sender.get().sendMessage(mensagem).toFuture())
                        .onFailure().transform(_ ->
                                new IllegalStateException("Falha ao publicar tentativa de monitoramento.")))
                .memoize().indefinitely();
    }
}
