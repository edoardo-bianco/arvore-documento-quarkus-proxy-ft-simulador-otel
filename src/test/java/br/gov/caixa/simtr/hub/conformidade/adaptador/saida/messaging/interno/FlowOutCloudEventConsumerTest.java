package br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno;

import static br.gov.caixa.simtr.hub.conformidade.adaptador.saida.messaging.interno.CloudEventMapper.EVENTO_REVISAO_SOLICITADA;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Test;

class FlowOutCloudEventConsumerTest {

    private final CloudEventMapper mapper = new CloudEventMapper(new ObjectMapper());

    @Test
    void confirmaSomenteDepoisDeRegistrarEventoValido() {
        List<String> ordem = new ArrayList<>();
        RegistrarEventoFlowOut registrar = evento ->
                Uni.createFrom().voidItem().invoke(() -> ordem.add("processar"));
        FlowOutCloudEventConsumer consumer = new FlowOutCloudEventConsumer(mapper, registrar);
        byte[] envelope = CloudEventMapperTest.eventoEstruturado(
                EVENTO_REVISAO_SOLICITADA,
                "instancia-ack",
                null,
                "{\"estado\":\"AGUARDANDO_REVISAO\"}");
        Message<byte[]> mensagem = Message.of(
                envelope,
                () -> {
                    ordem.add("ack");
                    return completedFuture(null);
                },
                falha -> {
                    ordem.add("nack");
                    return completedFuture(null);
                });

        consumer.consumir(mensagem).await().indefinitely();

        assertEquals(List.of("processar", "ack"), ordem);
    }

    @Test
    void rejeitaMensagemInvalidaSemConfirmar() {
        AtomicBoolean processou = new AtomicBoolean();
        AtomicBoolean confirmou = new AtomicBoolean();
        AtomicReference<Throwable> rejeicao = new AtomicReference<>();
        RegistrarEventoFlowOut registrar = evento ->
                Uni.createFrom().voidItem().invoke(() -> processou.set(true));
        FlowOutCloudEventConsumer consumer = new FlowOutCloudEventConsumer(mapper, registrar);
        Message<byte[]> mensagem = Message.of(
                "{invalido".getBytes(StandardCharsets.UTF_8),
                () -> {
                    confirmou.set(true);
                    return completedFuture(null);
                },
                falha -> {
                    rejeicao.set(falha);
                    return completedFuture(null);
                });

        consumer.consumir(mensagem).await().indefinitely();

        assertFalse(processou.get());
        assertFalse(confirmou.get());
        assertNotNull(rejeicao.get());
        assertInstanceOf(CloudEventInvalidoException.class, rejeicao.get());
    }

    @Test
    void rejeitaQuandoRegistroFalhaSemConfirmar() {
        AtomicBoolean confirmou = new AtomicBoolean();
        AtomicReference<Throwable> rejeicao = new AtomicReference<>();
        RegistrarEventoFlowOut registrar = evento -> Uni.createFrom().failure(
                new IllegalStateException("store indisponível"));
        FlowOutCloudEventConsumer consumer = new FlowOutCloudEventConsumer(mapper, registrar);
        byte[] envelope = CloudEventMapperTest.eventoEstruturado(
                EVENTO_REVISAO_SOLICITADA,
                "instancia-store",
                null,
                "{\"estado\":\"AGUARDANDO_REVISAO\"}");
        Message<byte[]> mensagem = Message.of(
                envelope,
                () -> {
                    confirmou.set(true);
                    return completedFuture(null);
                },
                falha -> {
                    rejeicao.set(falha);
                    return completedFuture(null);
                });

        consumer.consumir(mensagem).await().indefinitely();

        assertFalse(confirmou.get());
        assertInstanceOf(IllegalStateException.class, rejeicao.get());
    }
}
