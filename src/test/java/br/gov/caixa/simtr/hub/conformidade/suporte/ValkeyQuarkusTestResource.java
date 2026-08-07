package br.gov.caixa.simtr.hub.conformidade.suporte;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public final class ValkeyQuarkusTestResource
        implements QuarkusTestResourceLifecycleManager {

    static final String RESTART_PROOF_ENABLED = "restart.proof.enabled";

    private static final DockerImageName VALKEY_IMAGE =
            DockerImageName.parse("valkey/valkey:7.2-alpine");

    private GenericContainer<?> valkey;

    @Override
    public Map<String, String> start() {
        if (!deveGerenciarValkey(System.getProperty(RESTART_PROOF_ENABLED))) {
            return Map.of();
        }
        valkey = new GenericContainer<>(VALKEY_IMAGE).withExposedPorts(6379);
        valkey.start();
        return Map.of(
                "quarkus.redis.hosts",
                "redis://" + valkey.getHost() + ":" + valkey.getMappedPort(6379));
    }

    @Override
    public void stop() {
        if (valkey != null) {
            valkey.stop();
        }
    }

    static boolean deveGerenciarValkey(String restartProofEnabled) {
        return !Boolean.parseBoolean(restartProofEnabled);
    }
}
