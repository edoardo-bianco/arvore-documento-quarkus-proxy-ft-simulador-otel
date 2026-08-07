package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.couchdb;

import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.documento.FeedEventosAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventInvalidoException;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper;
import io.cloudevents.CloudEvent;
import io.smallrye.mutiny.Uni;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.jboss.logging.Logger;

public final class CouchDbChangesFeed implements FeedEventosAnaliseConformidade {

    private static final Logger LOG = Logger.getLogger(CouchDbChangesFeed.class);
    private static final long ATRASO_RECONEXAO_SEGUNDOS = 1L;

    private final CouchDbChangesClient client;
    private final CloudEventMapper mapper;
    private final ScheduledExecutorService executor;
    private final AtomicBoolean ativo = new AtomicBoolean();
    private final AtomicReference<Consumer<CloudEvent>> consumidor = new AtomicReference<>();

    public CouchDbChangesFeed(
            URI endpoint,
            ObjectMapper objectMapper,
            String username,
            String password,
            String database,
            CloudEventMapper mapper) {
        this(
                new CouchDbChangesHttpClient(
                        endpoint,
                        objectMapper,
                        username,
                        password,
                        database),
                mapper);
    }

    CouchDbChangesFeed(
            CouchDbChangesClient client,
            CloudEventMapper mapper) {
        this.client = java.util.Objects.requireNonNull(client, "client");
        this.mapper = java.util.Objects.requireNonNull(mapper, "mapper");
        this.executor = Executors.newSingleThreadScheduledExecutor(tarefa -> {
            Thread thread = new Thread(tarefa, "couchdb-changes-conformidade");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void iniciar(Consumer<CloudEvent> consumidor) {
        this.consumidor.set(java.util.Objects.requireNonNull(consumidor, "consumidor"));
        if (ativo.compareAndSet(false, true)) {
            agendar(0L);
        }
    }

    @Override
    public void parar() {
        ativo.set(false);
        consumidor.set(null);
    }

    @Override
    public void close() {
        parar();
        executor.shutdownNow();
    }

    Uni<Void> processarUmaVez(Consumer<CloudEvent> destino) {
        return client.carregarCursor()
                .chain(client::lerMudancas)
                .chain(mudancas -> processar(mudancas, destino));
    }

    private Uni<Void> processar(
            List<CouchDbMudanca> mudancas,
            Consumer<CloudEvent> destino) {
        Uni<Void> processamento = Uni.createFrom().voidItem();
        for (CouchDbMudanca mudanca : mudancas) {
            processamento = processamento.chain(() -> processar(mudanca, destino));
        }
        return processamento;
    }

    private Uni<Void> processar(
            CouchDbMudanca mudanca,
            Consumer<CloudEvent> destino) {
        return Uni.createFrom().deferred(() -> {
            if (!ehRevisao(mudanca.documento())) {
                return client.salvarCursor(mudanca.sequencia());
            }
            try {
                destino.accept(mapper.mapearRevisaoPersistida(mudanca.documento()));
                return client.salvarCursor(mudanca.sequencia());
            } catch (CloudEventInvalidoException _) {
                LOG.warn("Feed CouchDB ignorou documento de revisão inválido");
                return client.salvarCursor(mudanca.sequencia());
            } catch (RuntimeException falha) {
                return Uni.createFrom().failure(falha);
            }
        });
    }

    private void executarCiclo() {
        Consumer<CloudEvent> destino = consumidor.get();
        if (!ativo.get() || destino == null) {
            return;
        }
        processarUmaVez(destino).subscribe().with(
                ignorado -> agendar(0L),
                falha -> {
                    LOG.warn("Feed CouchDB indisponível; nova tentativa será realizada");
                    agendar(ATRASO_RECONEXAO_SEGUNDOS);
                });
    }

    private void agendar(long atrasoSegundos) {
        if (ativo.get()) {
            executor.schedule(
                    this::executarCiclo,
                    atrasoSegundos,
                    TimeUnit.SECONDS);
        }
    }

    private static boolean ehRevisao(com.fasterxml.jackson.databind.JsonNode documento) {
        return documento != null
                && documento.isObject()
                && "revisao-humana".equals(documento.path("tipo").textValue());
    }
}
