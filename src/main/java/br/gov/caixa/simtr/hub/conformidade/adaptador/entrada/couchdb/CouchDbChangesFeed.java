package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.couchdb;

import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.documento.FeedEventosAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.documento.ObservabilidadeFeedDocumental;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventInvalidoException;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper;
import io.cloudevents.CloudEvent;
import io.smallrye.mutiny.Uni;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class CouchDbChangesFeed implements FeedEventosAnaliseConformidade {

    private static final long ATRASO_RECONEXAO_SEGUNDOS = 1L;
    private static final String CURSOR_INICIAL = "CURSOR_INICIAL";
    private static final String CURSOR_PERSISTIDO = "CURSOR_PERSISTIDO";

    private final CouchDbChangesClient client;
    private final CloudEventMapper mapper;
    private final ScheduledExecutorService executor;
    private final ObservabilidadeFeedDocumental observabilidade;
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
                endpoint,
                objectMapper,
                username,
                password,
                database,
                mapper,
                OpenTelemetry.noop().getTracer("simtr-hub-conformidade"));
    }

    public CouchDbChangesFeed(
            URI endpoint,
            ObjectMapper objectMapper,
            String username,
            String password,
            String database,
            CloudEventMapper mapper,
            Tracer tracer) {
        this(
                new CouchDbChangesHttpClient(
                        endpoint,
                        objectMapper,
                        username,
                        password,
                        database),
                mapper,
                tracer);
    }

    CouchDbChangesFeed(
            CouchDbChangesClient client,
            CloudEventMapper mapper) {
        this(
                client,
                mapper,
                OpenTelemetry.noop().getTracer("simtr-hub-conformidade"));
    }

    CouchDbChangesFeed(
            CouchDbChangesClient client,
            CloudEventMapper mapper,
            Tracer tracer) {
        this.client = java.util.Objects.requireNonNull(client, "client");
        this.mapper = java.util.Objects.requireNonNull(mapper, "mapper");
        this.observabilidade = new ObservabilidadeFeedDocumental(
                tracer,
                "couchdb");
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
        return observabilidade.observar(
                        "carregar-cursor",
                        null,
                        "INDETERMINADO",
                        null,
                        client::carregarCursor,
                        ignorado -> "CARREGADO")
                .chain(cursor -> observabilidade.observar(
                        "processar-mudancas",
                        cursor,
                        replay(cursor),
                        null,
                        () -> client.lerMudancas(cursor)
                                .chain(mudancas -> processar(
                                        mudancas,
                                        destino,
                                        replay(cursor))),
                        ignorado -> "CONCLUIDO"));
    }

    private Uni<Void> processar(
            List<CouchDbMudanca> mudancas,
            Consumer<CloudEvent> destino,
            String replay) {
        Uni<Void> processamento = Uni.createFrom().voidItem();
        for (CouchDbMudanca mudanca : mudancas) {
            processamento = processamento.chain(() -> processar(
                    mudanca,
                    destino,
                    replay));
        }
        return processamento;
    }

    private Uni<Void> processar(
            CouchDbMudanca mudanca,
            Consumer<CloudEvent> destino,
            String replay) {
        return Uni.createFrom().deferred(() -> {
            if (!ehRevisao(mudanca.documento())) {
                return salvarCursor(mudanca.sequencia(), replay);
            }
            try {
                CloudEvent evento = mapper.mapearRevisaoPersistida(mudanca.documento());
                observabilidade.executar(
                        "entregar-revisao",
                        mudanca.sequencia(),
                        replay,
                        evento,
                        () -> destino.accept(evento));
                return salvarCursor(mudanca.sequencia(), replay);
            } catch (CloudEventInvalidoException _) {
                observabilidade.registrar(
                        "validar-revisao",
                        mudanca.sequencia(),
                        replay,
                        null,
                        "IGNORADO");
                return salvarCursor(mudanca.sequencia(), replay);
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
                falha -> agendar(ATRASO_RECONEXAO_SEGUNDOS));
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

    private static String replay(String cursor) {
        return "0".equals(cursor) ? CURSOR_INICIAL : CURSOR_PERSISTIDO;
    }

    private Uni<Void> salvarCursor(String cursor, String replay) {
        return observabilidade.observar(
                "salvar-cursor",
                cursor,
                replay,
                null,
                () -> client.salvarCursor(cursor),
                ignorado -> "SALVO");
    }
}
