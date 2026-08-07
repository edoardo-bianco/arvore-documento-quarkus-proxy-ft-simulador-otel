package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.documento;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.serverlessworkflow.impl.events.TypeEventRegistrationBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FeedNativoEventConsumerTest {

    @Test
    void conectaFeedAoRegistroDoFlowEDespachaSomenteOTipoAssinado() {
        var feed = new FeedFalso();
        var eventConsumer = new FeedNativoEventConsumer(feed);
        var recebidos = new ArrayList<CloudEvent>();
        var builder = new TypeEventRegistrationBuilder(
                "evento.esperado",
                (evento, workflow, task) -> true);

        var registro = eventConsumer.register(builder, recebidos::add);
        feed.entregar(evento("evento.ignorado", "1"));
        feed.entregar(evento("evento.esperado", "2"));

        assertEquals(List.of("2"), recebidos.stream().map(CloudEvent::getId).toList());
        assertEquals(1, feed.inicios);

        eventConsumer.unregister(registro);
        assertEquals(1, feed.paradas);

        eventConsumer.close();
        assertEquals(1, feed.fechamentos);
    }

    private static CloudEvent evento(String tipo, String id) {
        return CloudEventBuilder.v1()
                .withId(id)
                .withSource(URI.create("urn:teste"))
                .withType(tipo)
                .build();
    }

    private static final class FeedFalso implements FeedEventosAnaliseConformidade {

        private Consumer<CloudEvent> consumidor;
        private int inicios;
        private int paradas;
        private int fechamentos;

        @Override
        public void iniciar(Consumer<CloudEvent> consumidor) {
            this.consumidor = consumidor;
            inicios++;
        }

        @Override
        public void parar() {
            paradas++;
        }

        @Override
        public void close() {
            fechamentos++;
        }

        void entregar(CloudEvent evento) {
            consumidor.accept(evento);
        }
    }
}
