package br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

/** Verifica fontes externas antes do bootstrap dos testes explicitos de emulador. */
public final class ServiceBusEmuladorTestProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return LaunchMode.TEST.getDefaultProfile();
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        final boolean conexaoExterna;
        try {
            // Config novo por chamada: nao reutiliza o estado de outra aplicacao/teste.
            var config = ConfigUtils.configBuilder().withProfile(getConfigProfile()).build();
            // Verifica presenca sem expandir expressoes ou acessar seus segredos.
            conexaoExterna = config.isPropertyPresent("quarkus.azure.servicebus.connection-string")
                    || config.isPropertyPresent("quarkus.azure.servicebus.namespace");
        } catch (RuntimeException _) {
            throw new IllegalStateException("Nao foi possivel verificar a configuracao do teste de emulador.");
        }
        if (conexaoExterna) {
            throw new IllegalStateException(
                    "O teste de emulador exige ausencia de configuracao externa do Azure Service Bus.");
        }
        return Map.of(
                "quarkus.devservices.enabled", "true",
                "quarkus.azure.servicebus.enabled", "true",
                "quarkus.azure.servicebus.devservices.enabled", "true");
    }
}
