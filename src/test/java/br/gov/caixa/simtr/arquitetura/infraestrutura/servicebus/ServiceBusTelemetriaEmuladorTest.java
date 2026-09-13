package br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.azure.core.util.TracingOptions;
import com.azure.core.util.tracing.TracerProvider;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.azure.messaging.servicebus.models.CompleteOptions;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import com.azure.messaging.servicebus.models.SubQueue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import reactor.core.publisher.Mono;

/** Caracterizacao do SDK instalado; ausencia de spans nao satisfaz a correlacao final de 10.1. */
@QuarkusTest
@Tag("servicebus-integration")
@Execution(ExecutionMode.SAME_THREAD)
@TestProfile(ServiceBusTelemetriaEmuladorTest.TelemetriaProfile.class)
class ServiceBusTelemetriaEmuladorTest {
    private static final Duration ESPERA = Duration.ofSeconds(40);
    private static final String CONTROLE = "teste.servicebus.telemetria.controle";

    @Inject Tracer tracer;
    @Inject OpenTelemetry openTelemetry;
    @Inject InMemorySpanExporter exporter;
    @Inject ServiceBusClientBuilder builder;
    @Inject @FilaEntrada ServiceBusSenderAsyncClient senderEntrada;
    @Inject @FilaEntrada ServiceBusReceiverAsyncClient receiverEntrada;
    @Inject @FilaSaida ServiceBusSenderAsyncClient senderSaida;
    @Inject @FilaSaida ServiceBusReceiverAsyncClient receiverSaida;
    private ServiceBusReceiverAsyncClient observadorEntrada;
    private ServiceBusReceiverAsyncClient observadorSaida;

    @BeforeEach
    void limparCaptura() {
        observadorEntrada = criarObservador(receiverEntrada.getEntityPath());
        observadorSaida = criarObservador(receiverSaida.getEntityPath());
        flush();
        exporter.reset();
    }

    @AfterEach
    void fecharObservadoresExclusivosDoTeste() {
        try (var _ = observadorEntrada; var _ = observadorSaida) {
            // Fecha tambem sob falha parcial do BeforeEach, sem fechar os receivers CDI.
        }
    }

    private ServiceBusReceiverAsyncClient criarObservador(String fila) {
        return builder.receiver().queueName(fila).receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                .disableAutoComplete().prefetchCount(0).buildAsyncClient();
    }

    @Test
    void deveCapturarControlePositivoDoTracerCdi() {
        observar(() -> "controle");
    }

    @Test
    void deveCaracterizarProviderAzureAusenteNoRuntimeAtual() {
        assertEquals(0L, ServiceLoader.load(TracerProvider.class).stream().count());
        assertEquals(0L, ServiceLoader.load(com.azure.core.util.tracing.Tracer.class).stream().count());
        var azureTracer = TracerProvider.getDefaultProvider()
                .createTracer("azure-messaging-servicebus", "7.17.12", "Microsoft.ServiceBus",
                        new TracingOptions());
        assertFalse(azureTracer.isEnabled(), "Reavaliar a caracterizacao se um provider for instalado.");
        observar(azureTracer::isEnabled);
    }

    @ParameterizedTest
    @CsvSource({"ENTRADA, COMPLETE", "ENTRADA, ABANDON", "ENTRADA, DEAD_LETTER",
            "SAIDA, COMPLETE", "SAIDA, ABANDON", "SAIDA, DEAD_LETTER"})
    void deveCaracterizarEnvioRecebimentoESettlement(Fila fila, Settlement settlement) {
        var sender = sender(fila);
        var receiver = receiver(fila);
        var observador = observador(fila);
        exigirFilaVazia(observador);
        var enviada = mensagem("settlement");
        observar(() -> sender.sendMessage(enviada).block(ESPERA));
        assertTrue(enviada.getApplicationProperties().isEmpty(),
                "O SDK atual nao injeta carrier no objeto enviado.");
        var recebida = observar(() -> receiver.receiveMessages()
                .concatMap(mensagem -> {
                    verificarEntrega(mensagem, enviada);
                    return switch (settlement) {
                        case COMPLETE -> receiver.complete(mensagem).thenReturn(mensagem);
                        case ABANDON -> receiver.abandon(mensagem).thenReturn(mensagem);
                        case DEAD_LETTER -> receiver.deadLetter(mensagem).thenReturn(mensagem);
                    };
                }, 0).next().block(ESPERA));
        assertNotNull(recebida);

        switch (settlement) {
            case COMPLETE -> exigirFilaVazia(observador);
            case ABANDON -> {
                var reentregue = receberECompletar(receiver, enviada, ESPERA);
                assertEquals(recebida.getSequenceNumber(), reentregue.getSequenceNumber());
                assertNotEquals(recebida.getLockToken(), reentregue.getLockToken());
                assertTrue(reentregue.getDeliveryCount() > recebida.getDeliveryCount());
                exigirFilaVazia(observador);
            }
            case DEAD_LETTER -> verificarDlq(receiver, observador, enviada);
        }
    }

    @Test
    void deveCaracterizarScheduleSemTransacao() {
        exigirFilaVazia(observadorEntrada);
        var enviada = mensagem("schedule");
        var horario = horarioAgendamento();
        var sequencia = observar(() -> senderEntrada.scheduleMessage(enviada, horario).block(ESPERA));
        assertNotNull(sequencia);
        var agendada = observarFila(observadorEntrada);
        assertEquals(1, agendada.size());
        assertEquals(sequencia.longValue(), agendada.getFirst().getSequenceNumber());
        assertEquals(enviada.getMessageId(), agendada.getFirst().getMessageId());
        var recebida = receberECompletar(receiverEntrada, enviada, ESPERA);
        assertFalse(Instant.now().isBefore(horario.toInstant()));
        assertTrue(recebida.getSequenceNumber() > sequencia);
        exigirFilaVazia(observadorEntrada);
    }

    @ParameterizedTest
    @EnumSource(Transacao.class)
    void deveCaracterizarScheduleCompleteEConfirmacaoTransacional(Transacao desfecho) {
        exigirFilaVazia(observadorEntrada);
        var atual = mensagem("atual");
        var proxima = mensagem("proxima");
        observar(() -> senderEntrada.sendMessage(atual).block(ESPERA));
        var prova = observar(() -> receiverEntrada.receiveMessages()
                .concatMap(recebida -> {
                    verificarEntrega(recebida, atual);
                    var horario = horarioAgendamento();
                    return receiverEntrada.createTransaction().flatMap(tx ->
                            senderEntrada.scheduleMessage(proxima, horario, tx).flatMap(sequencia ->
                                    receiverEntrada.complete(recebida, new CompleteOptions().setTransactionContext(tx))
                                            .then(Mono.defer(() -> desfecho == Transacao.COMMIT
                                                    ? receiverEntrada.commitTransaction(tx)
                                                    : receiverEntrada.rollbackTransaction(tx)))
                                            .then(receiverEntrada.peekMessages(10, 0).collectList())
                                            .map(fila -> {
                                                // ACK de Complete sozinho nao comprova o commit.
                                                assertEquals(1, fila.size());
                                                var esperada = desfecho == Transacao.COMMIT ? proxima : atual;
                                                assertEquals(esperada.getMessageId(), fila.getFirst().getMessageId());
                                                assertEquals(desfecho == Transacao.COMMIT ? sequencia.longValue()
                                                        : recebida.getSequenceNumber(), fila.getFirst().getSequenceNumber());
                                                return new Prova(recebida, sequencia, horario.toInstant());
                                            })));
                }, 0).next().block(ESPERA));
        assertNotNull(prova);
        if (desfecho == Transacao.COMMIT) {
            var recebida = receberECompletar(receiverEntrada, proxima, ESPERA);
            assertFalse(Instant.now().isBefore(prova.horario()));
            assertTrue(recebida.getSequenceNumber() > prova.sequenciaAgendada());
        } else {
            var recebida = receberECompletar(receiverEntrada, atual, Duration.ofSeconds(75));
            assertEquals(prova.atual().getSequenceNumber(), recebida.getSequenceNumber());
            assertNotEquals(prova.atual().getLockToken(), recebida.getLockToken());
            var duracao = Duration.between(Instant.now(), prova.horario().plusSeconds(1));
            var observacao = duracao.isNegative() ? Duration.ofSeconds(1) : duracao;
            var posteriores = observar(() -> receiverEntrada.receiveMessages()
                    .takeUntilOther(Mono.delay(observacao).then(verificarFilaVazia(receiverEntrada)))
                    .collectList().block(ESPERA));
            assertNotNull(posteriores);
            assertTrue(posteriores.isEmpty(), "O schedule desfeito nao pode reaparecer.");
        }
    }

    private ServiceBusReceivedMessage receberECompletar(ServiceBusReceiverAsyncClient receiver,
            ServiceBusMessage esperada, Duration espera) {
        var recebida = observar(() -> receiver.receiveMessages().concatMap(mensagem -> {
            verificarEntrega(mensagem, esperada);
            return receiver.complete(mensagem).then(verificarFilaVazia(receiver)).thenReturn(mensagem);
        }, 0).next().block(espera));
        assertNotNull(recebida);
        return recebida;
    }

    /** O peek termina antes de next/takeUntilOther cancelar o receive link associado. */
    private Mono<Void> verificarFilaVazia(ServiceBusReceiverAsyncClient receiver) {
        return Mono.defer(() -> receiver.peekMessages(10, 0).collectList())
                .doOnNext(mensagens -> assertTrue(mensagens.isEmpty(), "Nao descartar residuos de outros cenarios."))
                .then();
    }

    private void verificarDlq(ServiceBusReceiverAsyncClient principal,
            ServiceBusReceiverAsyncClient observadorPrincipal, ServiceBusMessage esperada) {
        exigirFilaVazia(observadorPrincipal);
        // Cliente auxiliar apenas da subfila: os clientes principais continuam sendo os da fabrica.
        try (var dlq = builder.receiver().queueName(principal.getEntityPath())
                .subQueue(SubQueue.DEAD_LETTER_QUEUE).receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                .disableAutoComplete().prefetchCount(0).buildAsyncClient();
                var observadorDlq = builder.receiver().queueName(principal.getEntityPath())
                        .subQueue(SubQueue.DEAD_LETTER_QUEUE).receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
                        .disableAutoComplete().prefetchCount(0).buildAsyncClient()) {
            var mensagens = observarFila(observadorDlq);
            assertEquals(1, mensagens.size(), "DLQ isolada deve conter somente a mensagem deste cenario.");
            assertEquals(esperada.getMessageId(), mensagens.getFirst().getMessageId());
            receberECompletar(dlq, esperada, ESPERA);
            exigirFilaVazia(observadorDlq);
        }
    }

    private void verificarEntrega(ServiceBusReceivedMessage recebida, ServiceBusMessage esperada) {
        assertEquals(esperada.getMessageId(), recebida.getMessageId());
        assertEquals(esperada.getBody().toString(), recebida.getBody().toString());
        assertTrue(recebida.getApplicationProperties().isEmpty(),
                "O carrier recebido deve ser observado inteiro, sem filtrar chaves.");
        assertFalse(Span.current().getSpanContext().isValid(),
                "Callback atual nao deve ganhar contexto artificialmente pelo teste.");
    }

    private void exigirFilaVazia(ServiceBusReceiverAsyncClient receiver) {
        assertTrue(observarFila(receiver).isEmpty(), "Nao descartar residuos de outros cenarios.");
    }

    private List<ServiceBusReceivedMessage> observarFila(ServiceBusReceiverAsyncClient receiver) {
        var mensagens = observar(() -> receiver.peekMessages(10, 0).collectList().block(ESPERA));
        assertNotNull(mensagens);
        return mensagens;
    }

    /** Ativa somente o chamador; nao injeta carrier nem abre scope nos callbacks do SDK. */
    private <T> T observar(Supplier<T> operacao) {
        flush();
        assertTrue(exporter.getFinishedSpanItems().isEmpty(), "Captura anterior deve estar encerrada.");
        var controle = tracer.spanBuilder(CONTROLE).setNoParent().setSpanKind(SpanKind.INTERNAL).startSpan();
        assertTrue(controle.isRecording());
        assertTrue(controle.getSpanContext().isSampled());
        T resultado;
        try (var _ = controle.makeCurrent()) {
            resultado = operacao.get();
        } finally {
            controle.end();
        }
        flush();
        // Sem filtro: qualquer span SDK, atributo, evento ou link adicional invalida esta caracterizacao.
        var spans = exporter.getFinishedSpanItems();
        assertEquals(1, spans.size(), "Capturar controle positivo e zero spans adicionais do SDK atual.");
        var capturado = spans.getFirst();
        assertEquals(controle.getSpanContext(), capturado.getSpanContext());
        assertEquals(CONTROLE, capturado.getName());
        assertEquals(SpanKind.INTERNAL, capturado.getKind());
        assertFalse(capturado.getParentSpanContext().isValid());
        assertTrue(capturado.getAttributes().isEmpty());
        assertTrue(capturado.getEvents().isEmpty());
        assertTrue(capturado.getLinks().isEmpty());
        exporter.reset();
        return resultado;
    }

    private void flush() {
        var resultado = ((OpenTelemetrySdk) openTelemetry).getSdkTracerProvider().forceFlush();
        resultado.join(10, TimeUnit.SECONDS);
        assertTrue(resultado.isSuccess(), "Flush de spans deve concluir com sucesso.");
    }

    private ServiceBusSenderAsyncClient sender(Fila fila) {
        return fila == Fila.ENTRADA ? senderEntrada : senderSaida;
    }

    private ServiceBusReceiverAsyncClient receiver(Fila fila) {
        return fila == Fila.ENTRADA ? receiverEntrada : receiverSaida;
    }

    private ServiceBusReceiverAsyncClient observador(Fila fila) {
        return fila == Fila.ENTRADA ? observadorEntrada : observadorSaida;
    }

    private static ServiceBusMessage mensagem(String etapa) {
        return new ServiceBusMessage("telemetria-sintetica-" + etapa).setMessageId(UUID.randomUUID().toString());
    }

    private static OffsetDateTime horarioAgendamento() {
        return OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).plusSeconds(10);
    }

    enum Fila { ENTRADA, SAIDA }
    enum Settlement { COMPLETE, ABANDON, DEAD_LETTER }
    enum Transacao { COMMIT, ROLLBACK }

    private record Prova(ServiceBusReceivedMessage atual, long sequenciaAgendada, Instant horario) {
    }

    public static class TelemetriaProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return new ServiceBusEmuladorTestProfile().getConfigProfile();
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            var config = new HashMap<>(new ServiceBusEmuladorTestProfile().getConfigOverrides());
            config.put("monitoramento.service-bus.entrada.consumo-habilitado", "false");
            config.put("monitoramento.service-bus.saida.consumo-habilitado", "false");
            return config;
        }
    }
}
