package br.gov.caixa.simtr.monitoramento.adaptador.saida.servicebus;

import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.FilaEntrada;
import br.gov.caixa.simtr.monitoramento.aplicacao.porta.saida.ReagendarTentativaMonitoramento;
import br.gov.caixa.simtr.monitoramento.dominio.modelo.ReagendamentoMonitoramento;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.azure.messaging.servicebus.models.CompleteOptions;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import reactor.core.publisher.Mono;

/** Associa cada entrega a sua transacao sem guardar contexto mutavel no bean. */
@ApplicationScoped
public class MonitoramentoReagendamentoAdapter {
    private final Instance<ServiceBusSenderAsyncClient> clientes;
    private final MonitoramentoReagendamentoServiceBusMapper mapper;

    @Inject
    public MonitoramentoReagendamentoAdapter(@FilaEntrada Instance<ServiceBusSenderAsyncClient> clientes,
            MonitoramentoReagendamentoServiceBusMapper mapper) {
        this.clientes = clientes;
        this.mapper = mapper;
    }

    /** Handles permanecem capturados na borda; a porta recebe somente a intencao de negocio. */
    public ReagendarTentativaMonitoramento associar(ServiceBusReceiverAsyncClient receiver,
            ServiceBusReceivedMessage atual) {
        Objects.requireNonNull(receiver, "receiver");
        Objects.requireNonNull(atual, "atual");
        return pedido -> Uni.createFrom().item(() -> transacionar(receiver, atual, pedido).toFuture())
                // Compartilhar o futuro, e nao a espera: cancelamento deve alcancar o SDK.
                .memoize().indefinitely()
                .onItem().transformToUni(futuro -> Uni.createFrom().completionStage(futuro));
    }

    private Mono<Void> transacionar(ServiceBusReceiverAsyncClient receiver,
            ServiceBusReceivedMessage atual, ReagendamentoMonitoramento pedido) {
        return Mono.defer(() -> {
            var proxima = mapper.paraMensagem(pedido.proximaTentativa());
            var horario = OffsetDateTime.ofInstant(pedido.agendadoEm(), ZoneOffset.UTC);
            var sender = clientes.get();
            return receiver.createTransaction().flatMap(tx ->
                    Mono.defer(() -> sender.scheduleMessage(proxima, horario, tx))
                            .flatMap(_ -> Mono.defer(() -> receiver.complete(atual,
                                    new CompleteOptions().setTransactionContext(tx))))
                            .onErrorResume(_ -> Mono.defer(() -> receiver.rollbackTransaction(tx))
                                    .then(Mono.error(falhaNaoConfirmada())))
                            // Commit fica fora do recovery: falha aqui pode significar efeito remoto confirmado.
                            .then(Mono.defer(() -> receiver.commitTransaction(tx))));
        }).onErrorMap(_ -> falhaNaoConfirmada());
    }

    private static IllegalStateException falhaNaoConfirmada() {
        return new IllegalStateException("Reagendamento transacional nao confirmado.");
    }
}
