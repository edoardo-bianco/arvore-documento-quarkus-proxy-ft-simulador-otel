package br.gov.caixa.simtr.hub.conformidade.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import io.cloudevents.CloudEvent;
import io.serverlessworkflow.api.types.EventFilter;
import io.serverlessworkflow.impl.WorkflowApplication;
import io.serverlessworkflow.impl.events.EventConsumer;
import io.serverlessworkflow.impl.events.EventPublisher;

class FlowPersistenciaSpiCompatibilidadeTest {

    @Test
    void fixaExtensoesNoFlowSelecionadoPeloProjeto() throws Exception {
        assertEquals("0.10.2", artifactVersion("quarkus-flow-redis"));
        assertEquals("0.10.2", artifactVersion("quarkus-flow-durable-kubernetes"));
    }

    @Test
    void confirmaAssinaturasDoSpiDeEventosSemImplementarFallback() throws Exception {
        var listen = EventConsumer.class.getMethod(
                "listen",
                EventFilter.class,
                WorkflowApplication.class);
        var publish = EventPublisher.class.getMethod("publish", CloudEvent.class);
        var withId = WorkflowApplication.Builder.class.getMethod("withId", String.class);

        assertNotNull(listen);
        assertEquals(CompletableFuture.class, publish.getReturnType());
        assertEquals(WorkflowApplication.Builder.class, withId.getReturnType());
    }

    private static String artifactVersion(String artifactId) throws Exception {
        var resource = "META-INF/maven/io.quarkiverse.flow/" + artifactId + "/pom.properties";
        try (var stream = FlowPersistenciaSpiCompatibilidadeTest.class
                .getClassLoader()
                .getResourceAsStream(resource)) {
            assertNotNull(stream, resource);
            var properties = new Properties();
            properties.load(stream);
            return properties.getProperty("version");
        }
    }
}
