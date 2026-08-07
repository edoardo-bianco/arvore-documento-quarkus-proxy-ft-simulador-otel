package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.documento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.PersistirCloudEventAnaliseConformidadePublisher;
import br.gov.caixa.simtr.hub.conformidade.suporte.CouchDbQuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.serverlessworkflow.impl.WorkflowApplication;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(
        value = CouchDbQuarkusTestResource.class,
        restrictToAnnotatedClass = true)
class FeedNativoFlowQuarkusTest {

    @Inject
    WorkflowApplication workflowApplication;

    @Test
    void registraSomenteConsumerNativoEPublisherDocumentalNoRuntime() {
        assertInstanceOf(
                FeedNativoEventConsumer.class,
                workflowApplication.eventConsumer());
        long publishersDocumentais = workflowApplication.eventPublishers().stream()
                .filter(PersistirCloudEventAnaliseConformidadePublisher.class::isInstance)
                .count();
        assertEquals(1L, publishersDocumentais);
        assertEquals(1, workflowApplication.eventPublishers().size());
    }
}
