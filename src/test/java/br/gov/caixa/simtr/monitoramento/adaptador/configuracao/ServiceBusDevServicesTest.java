package br.gov.caixa.simtr.monitoramento.adaptador.configuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.azure.core.amqp.AmqpTransportType;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import io.quarkus.test.junit.QuarkusTest;
import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.ServiceBusEmuladorTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

@QuarkusTest
@Tag("servicebus-integration")
@TestProfile(ServiceBusEmuladorTestProfile.class)
class ServiceBusDevServicesTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Inject
    ServiceBusClientBuilder clientBuilder;

    @ConfigProperty(name = "monitoramento.service-bus.input-queue")
    String inputQueue;

    @ConfigProperty(name = "monitoramento.service-bus.output-queue")
    String outputQueue;

    @ConfigProperty(name = "monitoramento.service-bus.transport-type")
    AmqpTransportType transportType;

    @Test
    void deveDisponibilizarAsDuasFilasNoEmuladorComAmqpTcp() {
        assertEquals(AmqpTransportType.AMQP, transportType);
        assertFilaDisponivel(inputQueue);
        assertFilaDisponivel(outputQueue);
    }

    private void assertFilaDisponivel(String queueName) {
        String messageId = UUID.randomUUID().toString();
        ServiceBusMessage message = new ServiceBusMessage("dev-services-probe");
        message.setMessageId(messageId);

        try (ServiceBusSenderAsyncClient sender = clientBuilder
                .transportType(transportType)
                .sender()
                .queueName(queueName)
                .buildAsyncClient();
                ServiceBusReceiverAsyncClient receiver = clientBuilder
                        .transportType(transportType)
                        .receiver()
                        .queueName(queueName)
                        .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                        .disableAutoComplete()
                        .buildAsyncClient()) {
            sender.sendMessage(message).block(TIMEOUT);

            var received = receiver.receiveMessages().concatMap(entrega -> {
                assertEquals(messageId, entrega.getMessageId());
                return receiver.complete(entrega).thenReturn(entrega);
            }, 0).next().block(TIMEOUT);
            assertNotNull(received);
        }
    }
}
