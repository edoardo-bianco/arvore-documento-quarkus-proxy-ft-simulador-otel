package br.gov.caixa.simtr.monitoramento.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.FilaEntrada;
import br.gov.caixa.simtr.arquitetura.infraestrutura.servicebus.ServiceBusEmuladorTestProfile;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.azure.messaging.servicebus.models.CompleteOptions;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import reactor.core.publisher.Mono;

/** Prova obrigatoria do SDK/broker, anterior a implementacao do adapter de reagendamento. */
@QuarkusTest
@Tag("servicebus-integration")
@Execution(ExecutionMode.SAME_THREAD)
@TestProfile(ServiceBusEmuladorTestProfile.class)
class ReagendamentoTransacionalEmuladorTest {
    private static final Duration ESPERA = Duration.ofSeconds(40);

    @Inject
    @FilaEntrada
    ServiceBusSenderAsyncClient sender;
    @Inject
    @FilaEntrada
    ServiceBusReceiverAsyncClient receiver;

    @BeforeEach
    void exigirFilaIsoladaSemDescartarResiduos() {
        assertTrue(observarFila().isEmpty(), "A prova exige fila de entrada vazia.");
    }

    @ParameterizedTest
    @EnumSource(Desfecho.class)
    void deveConfirmarOuDesfazerAgendamentoECompleteJuntos(Desfecho desfecho) {
        String identidade = UUID.randomUUID().toString();
        var atual = new ServiceBusMessage("tentativa-funcional-3").setMessageId(identidade);
        var proxima = new ServiceBusMessage("tentativa-funcional-4").setMessageId(identidade + "-proxima");
        sender.sendMessage(atual).block(ESPERA);
        var antes = observarFila();
        assertEquals(1, antes.size());
        long sequenciaOriginal = antes.getFirst().getSequenceNumber();

        var prova = receiver.receiveMessages()
                .concatMap(recebida -> transacionar(recebida, proxima, desfecho)
                        .flatMap(transacionada -> verificarEstadoTransacionado(transacionada, proxima, desfecho)), 0)
                .next().block(ESPERA);
        assertNotNull(prova);
        assertEquals(sequenciaOriginal, prova.atual().getSequenceNumber());
        assertEquals(identidade, prova.atual().getMessageId());
        assertEquals(desfecho == Desfecho.CONFIRMACAO_LOCAL_PERDIDA, prova.confirmacaoPerdida());

        if (desfecho == Desfecho.ROLLBACK) {
            verificarRollback(prova, identidade, sequenciaOriginal);
        } else {
            verificarCommit(prova, proxima);
        }
        assertTrue(observarFila().isEmpty(), "A prova deve consumir somente sua mensagem final.");
    }

    private Mono<Prova> transacionar(ServiceBusReceivedMessage atual, ServiceBusMessage proxima,
            Desfecho desfecho) {
        OffsetDateTime agendadoEm = OffsetDateTime.now(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.SECONDS).plusSeconds(10);
        return receiver.createTransaction().flatMap(tx ->
                sender.scheduleMessage(proxima, agendadoEm, tx).flatMap(sequenciaAgendada -> {
                    var prova = new Prova(atual, sequenciaAgendada, agendadoEm.toInstant(), false);
                    return receiver.complete(atual, new CompleteOptions().setTransactionContext(tx))
                            .then(Mono.defer(() -> desfecho == Desfecho.ROLLBACK
                                    ? receiver.rollbackTransaction(tx)
                                    : receiver.commitTransaction(tx)))
                            .thenReturn(prova)
                            .flatMap(confirmada -> desfecho == Desfecho.CONFIRMACAO_LOCAL_PERDIDA
                                    ? Mono.error(new ConfirmacaoLocalPerdida(confirmada))
                                    : Mono.just(confirmada));
                }))
                // Somente a falha sintetica apos commit real; erros de SDK/broker falham o teste.
                .onErrorResume(ConfirmacaoLocalPerdida.class, falha -> Mono.just(
                        new Prova(falha.prova.atual(), falha.prova.sequenciaAgendada(),
                                falha.prova.agendadoEm(), true)));
    }

    private Mono<Prova> verificarEstadoTransacionado(Prova prova, ServiceBusMessage proxima, Desfecho desfecho) {
        // Observar antes de cancelar a assinatura que recebeu a entrega transacionada.
        return receiver.peekMessages(10, 0).collectList().map(fila -> {
            assertEquals(1, fila.size());
            if (desfecho == Desfecho.ROLLBACK) {
                assertEquals(prova.atual().getSequenceNumber(), fila.getFirst().getSequenceNumber());
                assertFalse(fila.stream().anyMatch(m -> m.getSequenceNumber() == prova.sequenciaAgendada()));
            } else {
                assertEquals(prova.sequenciaAgendada(), fila.getFirst().getSequenceNumber());
                assertEquals(proxima.getMessageId(), fila.getFirst().getMessageId());
                assertFalse(fila.stream().anyMatch(m -> m.getSequenceNumber() == prova.atual().getSequenceNumber()));
            }
            return prova;
        });
    }

    private void verificarCommit(Prova prova, ServiceBusMessage proxima) {
        var entregue = receiver.receiveMessages()
                .concatMap(mensagem -> {
                    var recebida = new Recebida(mensagem, Instant.now());
                    return receiver.complete(mensagem).thenReturn(recebida);
                }, 0)
                .next().block(ESPERA);
        assertNotNull(entregue);
        assertEquals(proxima.getMessageId(), entregue.mensagem().getMessageId());
        assertEquals(proxima.getBody().toString(), entregue.mensagem().getBody().toString());
        // Ao ativar o agendamento, o broker atribui uma nova sequencia.
        assertTrue(entregue.mensagem().getSequenceNumber() > prova.sequenciaAgendada());
        assertFalse(entregue.recebidaEm().isBefore(prova.agendadoEm()),
                "A proxima tentativa nao pode ser entregue antes do horario agendado.");
    }

    private void verificarRollback(Prova prova, String identidade, long sequenciaOriginal) {
        var reentregue = receiver.receiveMessages()
                .concatMap(mensagem -> receiver.complete(mensagem).thenReturn(mensagem), 0)
                .next().block(Duration.ofSeconds(75));
        assertNotNull(reentregue);
        assertEquals(sequenciaOriginal, reentregue.getSequenceNumber());
        assertEquals(identidade, reentregue.getMessageId());
        assertEquals("tentativa-funcional-3", reentregue.getBody().toString());
        assertNotSame(prova.atual(), reentregue);
        assertNotEquals(prova.atual().getLockToken(), reentregue.getLockToken());
        // Rollback nao promete incrementar DeliveryCount: nova entrega preserva o corpo funcional.
        Duration observacao = Duration.between(Instant.now(), prova.agendadoEm().plusSeconds(1));
        if (observacao.isNegative()) observacao = Duration.ofSeconds(1);
        var posteriores = receiver.receiveMessages().take(observacao).collectList().block(ESPERA);
        assertNotNull(posteriores);
        assertTrue(posteriores.isEmpty(), "O agendamento desfeito nao pode ser ativado mais tarde.");
    }

    private List<ServiceBusReceivedMessage> observarFila() {
        var mensagens = receiver.peekMessages(10, 0).collectList().block(ESPERA);
        assertNotNull(mensagens);
        return mensagens;
    }

    enum Desfecho {
        COMMIT, ROLLBACK, CONFIRMACAO_LOCAL_PERDIDA
    }

    private record Prova(ServiceBusReceivedMessage atual, long sequenciaAgendada,
            Instant agendadoEm, boolean confirmacaoPerdida) {
    }

    private record Recebida(ServiceBusReceivedMessage mensagem, Instant recebidaEm) {
    }

    private static final class ConfirmacaoLocalPerdida extends RuntimeException {
        private final Prova prova;

        private ConfirmacaoLocalPerdida(Prova prova) {
            super("Confirmacao local descartada deliberadamente apos commit real.");
            this.prova = prova;
        }
    }
}
