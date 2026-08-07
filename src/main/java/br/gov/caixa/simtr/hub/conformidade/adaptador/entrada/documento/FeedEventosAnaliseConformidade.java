package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.documento;

import io.cloudevents.CloudEvent;
import java.util.function.Consumer;

public interface FeedEventosAnaliseConformidade extends AutoCloseable {

    void iniciar(Consumer<CloudEvent> consumidor);

    void parar();

    @Override
    void close();
}
