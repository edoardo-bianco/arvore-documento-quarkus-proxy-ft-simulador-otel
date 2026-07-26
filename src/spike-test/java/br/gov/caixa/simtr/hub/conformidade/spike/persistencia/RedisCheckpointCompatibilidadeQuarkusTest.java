package br.gov.caixa.simtr.hub.conformidade.spike.persistencia;

import jakarta.inject.Inject;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.serverlessworkflow.impl.persistence.PersistenceInstanceHandlers;
import io.serverlessworkflow.impl.persistence.test.AbstractHandlerPersistenceTest;

@QuarkusTest
@QuarkusTestResource(ValkeySpikeResource.class)
class RedisCheckpointCompatibilidadeQuarkusTest extends AbstractHandlerPersistenceTest {

    @Inject
    PersistenceInstanceHandlers persistenceHandlers;

    @Override
    protected PersistenceInstanceHandlers getPersistenceHandlers() {
        return persistenceHandlers;
    }
}
