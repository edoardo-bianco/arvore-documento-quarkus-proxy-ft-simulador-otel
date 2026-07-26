package br.gov.caixa.simtr.hub.conformidade.spike.persistencia;

import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public final class ValkeySpikeResource implements QuarkusTestResourceLifecycleManager {

    private static final DockerImageName VALKEY_IMAGE = DockerImageName.parse("valkey/valkey:7.2-alpine");

    private GenericContainer<?> valkey;

    @Override
    public Map<String, String> start() {
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
}
