package br.gov.caixa.simtr.hub.conformidade.integracao;

import br.gov.caixa.simtr.hub.conformidade.suporte.ValkeyQuarkusTestResource;
import jakarta.inject.Inject;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.serverlessworkflow.impl.persistence.PersistenceInstanceHandlers;
import io.serverlessworkflow.impl.persistence.test.AbstractHandlerPersistenceTest;

@QuarkusTest
@QuarkusTestResource(ValkeyQuarkusTestResource.class)
class RedisCheckpointCompatibilidadeQuarkusTest extends AbstractHandlerPersistenceTest {

    @Inject
    PersistenceInstanceHandlers persistenceHandlers;

    @Override
    protected PersistenceInstanceHandlers getPersistenceHandlers() {
        return persistenceHandlers;
    }
}
