package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.cosmosdb;

import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.documento.FeedEventosAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.documento.ObservabilidadeFeedDocumental;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventInvalidoException;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper;
import com.azure.cosmos.ChangeFeedProcessor;
import com.azure.cosmos.ChangeFeedProcessorBuilder;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.ChangeFeedProcessorOptions;
import com.fasterxml.jackson.databind.JsonNode;
import io.cloudevents.CloudEvent;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class CosmosChangeFeed implements FeedEventosAnaliseConformidade {

    private static final String INSTRUMENTACAO = "simtr-hub-conformidade";
    static final String PREFIXO_LEASE = "simtr-conformidade-revisao-v1";
    private static final String REPLAY_DESDE_INICIO_SEM_LEASE =
            "DESDE_INICIO_SEM_LEASE";

    private final ChangeFeedProcessor processor;
    private final CloudEventMapper mapper;
    private final ObservabilidadeFeedDocumental observabilidade;
    private final AtomicBoolean ativo = new AtomicBoolean();
    private final AtomicReference<Consumer<CloudEvent>> consumidor = new AtomicReference<>();

    public CosmosChangeFeed(
            CosmosAsyncContainer feedContainer,
            CosmosAsyncContainer leaseContainer,
            CloudEventMapper mapper,
            String hostName) {
        this(
                feedContainer,
                leaseContainer,
                mapper,
                hostName,
                OpenTelemetry.noop().getTracer(INSTRUMENTACAO));
    }

    public CosmosChangeFeed(
            CosmosAsyncContainer feedContainer,
            CosmosAsyncContainer leaseContainer,
            CloudEventMapper mapper,
            String hostName,
            Tracer tracer) {
        this(
                feedContainer,
                leaseContainer,
                mapper,
                hostName,
                new ChangeFeedProcessorBuilder(),
                tracer);
    }

    CosmosChangeFeed(
            CosmosAsyncContainer feedContainer,
            CosmosAsyncContainer leaseContainer,
            CloudEventMapper mapper,
            String hostName,
            ChangeFeedProcessorBuilder builder) {
        this(
                feedContainer,
                leaseContainer,
                mapper,
                hostName,
                builder,
                OpenTelemetry.noop().getTracer(INSTRUMENTACAO));
    }

    CosmosChangeFeed(
            CosmosAsyncContainer feedContainer,
            CosmosAsyncContainer leaseContainer,
            CloudEventMapper mapper,
            String hostName,
            ChangeFeedProcessorBuilder builder,
            Tracer tracer) {
        this.mapper = java.util.Objects.requireNonNull(mapper, "mapper");
        this.observabilidade = new ObservabilidadeFeedDocumental(
                tracer,
                "cosmosdb");
        String host = validarHostName(hostName);
        var opcoes = new ChangeFeedProcessorOptions()
                .setLeasePrefix(PREFIXO_LEASE)
                .setStartFromBeginning(true)
                .setLeaseVerificationEnabledOnRestart(true);
        this.processor = java.util.Objects.requireNonNull(builder, "builder")
                .hostName(host)
                .feedContainer(java.util.Objects.requireNonNull(
                        feedContainer,
                        "feedContainer"))
                .leaseContainer(java.util.Objects.requireNonNull(
                        leaseContainer,
                        "leaseContainer"))
                .handleChanges(this::processarMudancas)
                .options(opcoes)
                .buildChangeFeedProcessor();
    }

    CosmosChangeFeed(
            ChangeFeedProcessor processor,
            CloudEventMapper mapper) {
        this(
                processor,
                mapper,
                OpenTelemetry.noop().getTracer(INSTRUMENTACAO));
    }

    CosmosChangeFeed(
            ChangeFeedProcessor processor,
            CloudEventMapper mapper,
            Tracer tracer) {
        this.processor = java.util.Objects.requireNonNull(processor, "processor");
        this.mapper = java.util.Objects.requireNonNull(mapper, "mapper");
        this.observabilidade = new ObservabilidadeFeedDocumental(
                tracer,
                "cosmosdb");
    }

    @Override
    public void iniciar(Consumer<CloudEvent> consumidor) {
        this.consumidor.set(java.util.Objects.requireNonNull(consumidor, "consumidor"));
        if (ativo.compareAndSet(false, true)) {
            processor.start().subscribe(
                    ignorado -> {
                        // Mono<Void> não emite item.
                    },
                    falha -> {
                        ativo.set(false);
                        observabilidade.registrar(
                                "iniciar",
                                PREFIXO_LEASE,
                                REPLAY_DESDE_INICIO_SEM_LEASE,
                                null,
                                "FALHA");
                    },
                    () -> observabilidade.registrar(
                            "iniciar",
                            PREFIXO_LEASE,
                            REPLAY_DESDE_INICIO_SEM_LEASE,
                            null,
                            "INICIADO"));
        }
    }

    @Override
    public void parar() {
        if (ativo.compareAndSet(true, false)) {
            consumidor.set(null);
            processor.stop().subscribe(
                    ignorado -> {
                        // Mono<Void> não emite item.
                    },
                    falha -> observabilidade.registrar(
                            "parar",
                            PREFIXO_LEASE,
                            REPLAY_DESDE_INICIO_SEM_LEASE,
                            null,
                            "FALHA"),
                    () -> observabilidade.registrar(
                            "parar",
                            PREFIXO_LEASE,
                            REPLAY_DESDE_INICIO_SEM_LEASE,
                            null,
                            "PARADO"));
        }
    }

    @Override
    public void close() {
        parar();
    }

    void processarMudancas(List<JsonNode> documentos) {
        Consumer<CloudEvent> destino = consumidor.get();
        if (destino == null || documentos == null) {
            return;
        }
        observabilidade.executar(
                "processar-lote",
                PREFIXO_LEASE,
                REPLAY_DESDE_INICIO_SEM_LEASE,
                null,
                () -> processarMudancas(documentos, destino));
    }

    private void processarMudancas(
            List<JsonNode> documentos,
            Consumer<CloudEvent> destino) {
        for (JsonNode documento : documentos) {
            if (!ehRevisao(documento)) {
                continue;
            }
            try {
                CloudEvent evento = mapper.mapearRevisaoPersistida(documento);
                observabilidade.executar(
                        "entregar-revisao",
                        PREFIXO_LEASE,
                        REPLAY_DESDE_INICIO_SEM_LEASE,
                        evento,
                        () -> destino.accept(evento));
            } catch (CloudEventInvalidoException _) {
                observabilidade.registrar(
                        "validar-revisao",
                        PREFIXO_LEASE,
                        REPLAY_DESDE_INICIO_SEM_LEASE,
                        null,
                        "IGNORADO");
            }
        }
    }

    private static String validarHostName(String hostName) {
        if (hostName == null || hostName.isBlank() || hostName.length() > 255) {
            throw new IllegalArgumentException("Host do Change Feed Cosmos inválido");
        }
        return hostName;
    }

    private static boolean ehRevisao(JsonNode documento) {
        return documento != null
                && documento.isObject()
                && "revisao-humana".equals(documento.path("tipo").textValue());
    }
}
