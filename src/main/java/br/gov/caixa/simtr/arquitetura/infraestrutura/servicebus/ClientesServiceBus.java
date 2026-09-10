package br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus;

import com.azure.core.amqp.AmqpTransportType;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;
import java.util.ArrayDeque;
import java.util.Deque;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Cria os quatro clientes uma vez; somente esta fabrica configura o builder da extensao. */
@Startup
@ApplicationScoped
@IfBuildProperty(name = "quarkus.azure.servicebus.enabled", stringValue = "true", enableIfMissing = true)
public class ClientesServiceBus {

    private final Deque<Runnable> fechamentos = new ArrayDeque<>();
    private final ServiceBusSenderAsyncClient senderEntrada;
    private final ServiceBusSenderAsyncClient senderSaida;
    private final ServiceBusReceiverAsyncClient receiverEntrada;
    private final ServiceBusReceiverAsyncClient receiverSaida;

    @Inject
    public ClientesServiceBus(ServiceBusClientBuilder builder,
            @ConfigProperty(name = "monitoramento.service-bus.input-queue") String entrada,
            @ConfigProperty(name = "monitoramento.service-bus.output-queue") String saida,
            @ConfigProperty(name = "monitoramento.service-bus.transport-type") AmqpTransportType transporte,
            @ConfigProperty(name = "quarkus.azure.servicebus.connection-string") String connectionString) {
        if (connectionString == null || connectionString.isBlank()) {
            throw new IllegalStateException("Connection string Service Bus obrigatoria.");
        }
        try {
            builder.transportType(transporte);
            senderEntrada = builder.sender().queueName(entrada).buildAsyncClient();
            fechamentos.addFirst(senderEntrada::close);
            senderSaida = builder.sender().queueName(saida).buildAsyncClient();
            fechamentos.addFirst(senderSaida::close);
            receiverEntrada = builder.receiver().queueName(entrada)
                    .receiveMode(ServiceBusReceiveMode.PEEK_LOCK).disableAutoComplete()
                    .prefetchCount(0).buildAsyncClient();
            fechamentos.addFirst(receiverEntrada::close);
            receiverSaida = builder.receiver().queueName(saida)
                    .receiveMode(ServiceBusReceiveMode.PEEK_LOCK).disableAutoComplete().buildAsyncClient();
            fechamentos.addFirst(receiverSaida::close);
        } catch (RuntimeException _) {
            var falha = new IllegalStateException("Falha ao inicializar clientes Service Bus.");
            try {
                fechar();
            } catch (IllegalStateException fechamento) {
                falha.addSuppressed(fechamento);
            }
            throw falha;
        }
    }

    @Produces
    @Singleton
    @FilaEntrada
    public ServiceBusSenderAsyncClient senderEntrada() {
        return senderEntrada;
    }

    @Produces
    @Singleton
    @FilaSaida
    public ServiceBusSenderAsyncClient senderSaida() {
        return senderSaida;
    }

    @Produces
    @Singleton
    @FilaEntrada
    public ServiceBusReceiverAsyncClient receiverEntrada() {
        return receiverEntrada;
    }

    @Produces
    @Singleton
    @FilaSaida
    public ServiceBusReceiverAsyncClient receiverSaida() {
        return receiverSaida;
    }

    /** Listeners encerram suas assinaturas antes desta prioridade de shutdown. */
    void encerrar(@Observes @Priority(Interceptor.Priority.PLATFORM_AFTER) ShutdownEvent evento) {
        fechar();
    }

    /** Fecha todos os clientes, inclusive apos falha parcial; chamadas seguintes sao inofensivas. */
    @PreDestroy
    void fechar() {
        boolean falhou = false;
        while (!fechamentos.isEmpty()) {
            try {
                fechamentos.removeFirst().run();
            } catch (RuntimeException _) {
                falhou = true;
            }
        }
        if (falhou) {
            throw new IllegalStateException("Falha ao fechar clientes Service Bus.");
        }
    }
}
