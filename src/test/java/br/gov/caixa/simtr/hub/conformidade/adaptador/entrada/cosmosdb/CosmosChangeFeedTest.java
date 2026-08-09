package br.gov.caixa.simtr.hub.conformidade.adaptador.entrada.cosmosdb;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper;
import br.gov.caixa.simtr.hub.conformidade.aplicacao.documento.ReferenciasDocumentoAnaliseConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ParecerConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.ResultadoApontamentoConformidade;
import br.gov.caixa.simtr.hub.conformidade.dominio.modelo.analise.RevisaoHumanaConformidade;
import com.azure.cosmos.ChangeFeedProcessor;
import com.azure.cosmos.ChangeFeedProcessorBuilder;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.models.ChangeFeedProcessorOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

@QuarkusTest
class CosmosChangeFeedTest {

    private static final String CORRELATION_ID =
            "7aa3ca4d-3c7e-4f61-a3a1-996571d3397a";
    private static final String INSTANCE_ID = "01J3FLOWTESTE00000000000000";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloudEventMapper mapper = new CloudEventMapper(objectMapper);

    @Inject
    Tracer tracer;

    @Inject
    InMemorySpanExporter exporter;

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    void configuraContainersELeasesPersistentesNoProcessor() {
        var feedContainer = mock(CosmosAsyncContainer.class);
        var leaseContainer = mock(CosmosAsyncContainer.class);
        var builder = mock(ChangeFeedProcessorBuilder.class);
        var processor = mock(ChangeFeedProcessor.class);
        when(builder.hostName(any())).thenReturn(builder);
        when(builder.feedContainer(any())).thenReturn(builder);
        when(builder.leaseContainer(any())).thenReturn(builder);
        when(builder.handleChanges(any())).thenReturn(builder);
        when(builder.options(any())).thenReturn(builder);
        when(builder.buildChangeFeedProcessor()).thenReturn(processor);

        new CosmosChangeFeed(
                feedContainer,
                leaseContainer,
                mapper,
                "simtr-hub-pod-1",
                builder);

        ArgumentCaptor<ChangeFeedProcessorOptions> opcoes =
                ArgumentCaptor.forClass(ChangeFeedProcessorOptions.class);
        verify(builder).hostName("simtr-hub-pod-1");
        verify(builder).feedContainer(feedContainer);
        verify(builder).leaseContainer(leaseContainer);
        verify(builder).options(opcoes.capture());
        assertEquals("simtr-conformidade-revisao-v1", opcoes.getValue().getLeasePrefix());
        assertTrue(opcoes.getValue().isStartFromBeginning());
    }

    @Test
    void ignoraDocumentoInvalidoEntregaOValidoEControlaCicloDeVida() {
        var processor = mock(ChangeFeedProcessor.class);
        when(processor.start()).thenReturn(Mono.empty());
        when(processor.stop()).thenReturn(Mono.empty());
        var feed = new CosmosChangeFeed(processor, mapper);
        var recebidos = new ArrayList<String>();

        feed.iniciar(evento -> recebidos.add(evento.getId()));
        feed.iniciar(evento -> recebidos.add(evento.getId()));
        feed.processarMudancas(List.of(
                documentoRevisao().put("hashConteudo", "0".repeat(64)),
                documentoRevisao()));
        feed.parar();
        feed.parar();

        assertEquals(
                List.of("conformidade:" + documentoRevisao().path("id").asText()),
                recebidos);
        verify(processor, times(1)).start();
        verify(processor, times(1)).stop();
    }

    @Test
    void registraConclusaoAssincronaDoInicioDoProcessor() {
        exporter.reset();
        var processor = mock(ChangeFeedProcessor.class);
        when(processor.start()).thenReturn(Mono.empty());
        var feed = new CosmosChangeFeed(processor, mapper, tracer);

        feed.iniciar(evento -> {
            // Consumidor sintético para ativar o feed.
        });

        ((OpenTelemetrySdk) openTelemetry)
                .getSdkTracerProvider()
                .forceFlush()
                .join(10, TimeUnit.SECONDS);

        SpanData span = exporter.getFinishedSpanItems().stream()
                .filter(item -> "simtr-hub.feed.conformidade.documento"
                        .equals(item.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals("iniciar", atributo(span, "conformidade.feed.operacao"));
        assertEquals("INICIADO", atributo(span, "conformidade.feed.resultado"));
    }

    @Test
    void propagaFalhaDaEntregaValidaParaORetryDoProcessor() {
        var processor = mock(ChangeFeedProcessor.class);
        when(processor.start()).thenReturn(Mono.empty());
        var feed = new CosmosChangeFeed(processor, mapper);
        feed.iniciar(evento -> {
            throw new IllegalStateException("falha simulada");
        });
        List<JsonNode> mudancas = List.of(documentoRevisao());

        assertThrows(
                IllegalStateException.class,
                () -> feed.processarMudancas(mudancas));
    }

    @Test
    void permiteNovaTentativaQuandoProcessorFalhaAoIniciar() {
        var processor = mock(ChangeFeedProcessor.class);
        when(processor.start())
                .thenReturn(Mono.error(new IllegalStateException("falha simulada")))
                .thenReturn(Mono.empty());
        var feed = new CosmosChangeFeed(processor, mapper);
        var recebidos = new ArrayList<String>();

        feed.iniciar(evento -> recebidos.add(evento.getId()));
        feed.iniciar(evento -> recebidos.add(evento.getId()));

        assertEquals(List.of(), recebidos);
        verify(processor, times(2)).start();
    }

    @Test
    void closeParaProcessorAtivoUmaUnicaVez() {
        var processor = mock(ChangeFeedProcessor.class);
        when(processor.start()).thenReturn(Mono.empty());
        when(processor.stop()).thenReturn(Mono.empty());
        var feed = new CosmosChangeFeed(processor, mapper);
        var recebidos = new ArrayList<String>();
        feed.iniciar(evento -> recebidos.add(evento.getId()));

        feed.close();
        feed.close();

        assertEquals(List.of(), recebidos);
        verify(processor, times(1)).stop();
    }

    @Test
    void ignoraLoteAusenteEItensQueNaoRepresentamRevisao() {
        var processor = mock(ChangeFeedProcessor.class);
        when(processor.start()).thenReturn(Mono.empty());
        var feed = new CosmosChangeFeed(processor, mapper);
        var recebidos = new ArrayList<String>();
        feed.iniciar(evento -> recebidos.add(evento.getId()));
        var documentos = new ArrayList<JsonNode>();
        documentos.add(null);
        documentos.add(objectMapper.createObjectNode().put("tipo", "resultado-final"));

        feed.processarMudancas(null);
        feed.processarMudancas(documentos);

        assertEquals(List.of(), recebidos);
    }

    @Test
    void rejeitaHostNameAusenteVazioOuAcimaDoLimite() {
        var feedContainer = mock(CosmosAsyncContainer.class);
        var leaseContainer = mock(CosmosAsyncContainer.class);
        var builder = mock(ChangeFeedProcessorBuilder.class);
        String hostAcimaDoLimite = "h".repeat(256);

        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new CosmosChangeFeed(
                                feedContainer,
                                leaseContainer,
                                mapper,
                                null,
                                builder)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new CosmosChangeFeed(
                                feedContainer,
                                leaseContainer,
                                mapper,
                                " ",
                                builder)),
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> new CosmosChangeFeed(
                                feedContainer,
                                leaseContainer,
                                mapper,
                                hostAcimaDoLimite,
                                builder)));
    }

    private com.fasterxml.jackson.databind.node.ObjectNode documentoRevisao() {
        var revisao = new RevisaoHumanaConformidade(
                "Aprovado pelo revisor",
                List.of(new ResultadoApontamentoConformidade(
                        10L,
                        "Documento identificado",
                        ParecerConformidade.CONFORME,
                        "Confirmado",
                        "Trecho",
                        0.9d)));
        var referencia = new ReferenciasDocumentoAnaliseConformidade(objectMapper)
                .revisao(CORRELATION_ID, revisao);
        return objectMapper.createObjectNode()
                .put("id", referencia.documentoRef())
                .put("tipo", "revisao-humana")
                .put("instanceId", INSTANCE_ID)
                .put("correlationId", CORRELATION_ID)
                .put("versaoSchema", referencia.versaoSchema())
                .put("hashConteudo", referencia.hashConteudo())
                .set("revisao", objectMapper.valueToTree(revisao));
    }

    private static Object atributo(SpanData span, String chave) {
        return span.getAttributes().asMap().entrySet().stream()
                .filter(entry -> entry.getKey().getKey().equals(chave))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
