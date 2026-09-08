package br.gov.caixa.simtr.monitoramento.adaptador.configuracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.azure.core.amqp.AmqpTransportType;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(ServiceBusClientBuilderInjectionTest.ExternalAzureProfile.class)
class ServiceBusClientBuilderInjectionTest {

    @Inject
    ServiceBusClientBuilder clientBuilder;

    @ConfigProperty(name = "quarkus.azure.servicebus.devservices.enabled")
    boolean devServicesEnabled;

    @ConfigProperty(name = "monitoramento.service-bus.input-queue")
    String inputQueue;

    @ConfigProperty(name = "monitoramento.service-bus.output-queue")
    String outputQueue;

    @ConfigProperty(name = "monitoramento.service-bus.transport-type")
    AmqpTransportType transportType;

    @Test
    void deveSelecionarConfiguracaoAzureExternaSemDevServices() {
        assertNotNull(clientBuilder);
        assertFalse(devServicesEnabled);
        assertEquals("q.prevalidacao.monitoramento-mtr.in", inputQueue);
        assertEquals("q.prevalidacao.monitoramento-mtr.out", outputQueue);
        assertEquals(AmqpTransportType.AMQP_WEB_SOCKETS, transportType);
    }

    public static final class ExternalAzureProfile implements QuarkusTestProfile {

        @Override
        public String getConfigProfile() {
            return "azure";
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.azure.servicebus.enabled", "true",
                    "quarkus.azure.servicebus.connection-string",
                    "Endpoint=sb://servicebus.invalid/;SharedAccessKeyName=synthetic-test;SharedAccessKey=dGVzdA==");
        }
    }
}
