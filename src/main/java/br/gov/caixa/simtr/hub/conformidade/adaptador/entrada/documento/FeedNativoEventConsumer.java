package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.documento;

import io.cloudevents.CloudEvent;
import io.serverlessworkflow.impl.events.AbstractTypeConsumer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@ApplicationScoped
public class FeedNativoEventConsumer extends AbstractTypeConsumer {

    private final FeedEventosAnaliseConformidade feed;
    private final Map<String, Consumer<CloudEvent>> consumidoresPorTipo =
            new ConcurrentHashMap<>();
    private final AtomicReference<Consumer<CloudEvent>> consumidorTodos =
            new AtomicReference<>();
    private boolean ativo;

    @Inject
    public FeedNativoEventConsumer(FeedEventosAnaliseConformidade feed) {
        this.feed = java.util.Objects.requireNonNull(feed, "feed");
    }

    @Override
    protected synchronized void registerToAll(Consumer<CloudEvent> consumidor) {
        consumidorTodos.set(consumidor);
        ativarSeNecessario();
    }

    @Override
    protected synchronized void unregisterFromAll() {
        consumidorTodos.set(null);
        pararSeSemConsumidor();
    }

    @Override
    protected synchronized void register(
            String tipo,
            Consumer<CloudEvent> consumidor) {
        consumidoresPorTipo.put(tipo, consumidor);
        ativarSeNecessario();
    }

    @Override
    protected synchronized void unregister(String tipo) {
        consumidoresPorTipo.remove(tipo);
        pararSeSemConsumidor();
    }

    @Override
    public synchronized void close() {
        if (ativo) {
            feed.parar();
            ativo = false;
        }
        consumidoresPorTipo.clear();
        consumidorTodos.set(null);
        feed.close();
    }

    private void entregar(CloudEvent evento) {
        Consumer<CloudEvent> todos = consumidorTodos.get();
        if (todos != null) {
            todos.accept(evento);
        }
        Consumer<CloudEvent> consumidor = consumidoresPorTipo.get(evento.getType());
        if (consumidor != null) {
            consumidor.accept(evento);
        }
    }

    private void ativarSeNecessario() {
        if (!ativo) {
            feed.iniciar(this::entregar);
            ativo = true;
        }
    }

    private void pararSeSemConsumidor() {
        if (ativo
                && consumidorTodos.get() == null
                && consumidoresPorTipo.isEmpty()) {
            feed.parar();
            ativo = false;
        }
    }
}
