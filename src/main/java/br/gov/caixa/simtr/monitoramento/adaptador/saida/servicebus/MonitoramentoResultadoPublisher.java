package br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus;

import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.FilaSaida;
import br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida.PublicarResultadoMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.ResultadoMonitoramento;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/** Publica o resultado pelo cliente compartilhado; conclusao significa confirmacao do broker. */
@ApplicationScoped
public class MonitoramentoResultadoPublisher implements PublicarResultadoMonitoramento {

    private final Instance<ServiceBusSenderAsyncClient> sender;
    private final MonitoramentoResultadoServiceBusMapper mapper;

    @Inject
    public MonitoramentoResultadoPublisher(@FilaSaida Instance<ServiceBusSenderAsyncClient> sender,
            MonitoramentoResultadoServiceBusMapper mapper) {
        this.sender = sender;
        this.mapper = mapper;
    }

    /** Compartilha o envio da invocacao, sem retry nem transacao entre as filas. */
    @Override
    public Uni<Void> executar(ResultadoMonitoramento resultado) {
        return Uni.createFrom().item(() -> mapper.paraMensagem(resultado))
                .onItem().transformToUni(mensagem -> Uni.createFrom().completionStage(
                        () -> sender.get().sendMessage(mensagem).toFuture())
                        .onFailure().transform(_ ->
                                new IllegalStateException("Falha ao publicar resultado de monitoramento.")))
                .memoize().indefinitely();
    }
}
