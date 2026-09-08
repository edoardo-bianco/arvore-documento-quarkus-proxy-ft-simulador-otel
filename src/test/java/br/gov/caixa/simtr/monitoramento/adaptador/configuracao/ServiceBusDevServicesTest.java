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
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(ServiceBusDevServicesTest.DevServicesProfile.class)
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

            var received = receiver.receiveMessages().next().block(TIMEOUT);
            assertNotNull(received);
            assertEquals(messageId, received.getMessageId());
            receiver.complete(received).block(TIMEOUT);
        }
    }

    public static final class DevServicesProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            rejectExternalServiceBusConfiguration();
            return Map.of(
                    "quarkus.devservices.enabled", "true",
                    "quarkus.azure.servicebus.enabled", "true",
                    "quarkus.azure.servicebus.devservices.enabled", "true");
        }

        private static void rejectExternalServiceBusConfiguration() {
            if (isConfigured("QUARKUS_AZURE_SERVICEBUS_CONNECTION_STRING")
                    || isConfigured("QUARKUS_AZURE_SERVICEBUS_NAMESPACE")
                    || isSystemPropertyConfigured("quarkus.azure.servicebus.connection-string")
                    || isSystemPropertyConfigured("quarkus.azure.servicebus.namespace")) {
                throw new IllegalStateException(
                        "O teste de Dev Services exige ausência de configuração externa do Azure Service Bus");
            }
        }

        private static boolean isConfigured(String name) {
            String value = System.getenv(name);
            return value != null && !value.isBlank();
        }

        private static boolean isSystemPropertyConfigured(String name) {
            String value = System.getProperty(name);
            return value != null && !value.isBlank();
        }
    }
}
