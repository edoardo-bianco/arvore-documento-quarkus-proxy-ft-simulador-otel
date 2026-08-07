package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.cosmosdb;

import br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.documento.FeedEventosAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventInvalidoException;
import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper;
import com.azure.cosmos.ChangeFeedProcessor;
import com.azure.cosmos.ChangeFeedProcessorBuilder;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.ChangeFeedProcessorOptions;
import com.fasterxml.jackson.databind.JsonNode;
import io.cloudevents.CloudEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.jboss.logging.Logger;

public final class CosmosChangeFeed implements FeedEventosAnaliseConformidade {

    static final String PREFIXO_LEASE = "simtr-conformidade-revisao-v1";
    private static final Logger LOG = Logger.getLogger(CosmosChangeFeed.class);

    private final ChangeFeedProcessor processor;
    private final CloudEventMapper mapper;
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
                new ChangeFeedProcessorBuilder());
    }

    CosmosChangeFeed(
            CosmosAsyncContainer feedContainer,
            CosmosAsyncContainer leaseContainer,
            CloudEventMapper mapper,
            String hostName,
            ChangeFeedProcessorBuilder builder) {
        this.mapper = java.util.Objects.requireNonNull(mapper, "mapper");
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
        this.processor = java.util.Objects.requireNonNull(processor, "processor");
        this.mapper = java.util.Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public void iniciar(Consumer<CloudEvent> consumidor) {
        this.consumidor.set(java.util.Objects.requireNonNull(consumidor, "consumidor"));
        if (ativo.compareAndSet(false, true)) {
            processor.start().subscribe(
                    ignorado -> {
                        // Mono<Void>: conclusão indica processor iniciado.
                    },
                    falha -> {
                        ativo.set(false);
                        LOG.warn("Change Feed Cosmos não pôde ser iniciado");
                    });
        }
    }

    @Override
    public void parar() {
        if (ativo.compareAndSet(true, false)) {
            consumidor.set(null);
            processor.stop().subscribe(
                    ignorado -> {
                        // Mono<Void>: conclusão indica processor parado.
                    },
                    falha -> LOG.warn("Change Feed Cosmos não pôde ser parado"));
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
        for (JsonNode documento : documentos) {
            if (!ehRevisao(documento)) {
                continue;
            }
            try {
                destino.accept(mapper.mapearRevisaoPersistida(documento));
            } catch (CloudEventInvalidoException _) {
                LOG.warn("Change Feed Cosmos ignorou documento de revisão inválido");
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
